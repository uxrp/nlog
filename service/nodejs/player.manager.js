var common = require('../common/channel.common.js');
var event = require('./event');
var querystring = require('querystring');

void function(){
    /**
     * 玩家集合
     */
    var playerDict = {};
    /**
     * 可更新的字段列表
     */
    var updateFields = ['nick', 'state', 'weibo', 'face'];
    /**
     * 存储目录
     */
    var dir = "players";
    /**
     * 获取用户身份验证码
     * @param {String} id 用户id
     * @param {String} visa 密码
     */
    function getPlayerMask(id, visa){
        return (common.passportKey ^ parseInt(visa, 36) ^ parseInt(id, 36)).toString(36);
    }

    /**
     * 用户信息
     * @param {String} id
     * @param {Boolean} value 是否从文件中读取
     */
    function Player(id, value){
        if (value) {
            if (value.id == id) {
                this.id = id;
                this.visa = value.visa;
                this.mask = value.mask;
                this.nick = value.nick;
                this.state = value.state;
                this.weibo = value.weibo;
                this.createTime = new Date(value.createTime);
                this.accessTime = new Date(value.accessTime);
                this.modifyTime = new Date(value.modifyTime);
                this.passportTime = new Date(value.passportTime);
                this.commandTime = new Date(value.commandTime);
                playerDict[this.id] = this;
                return;
            }
            return;
        }
        this.id = id || (+new Date - new Date('2011/8/16')).toString(36);
        this.visa = parseInt(Math.random() * 99999999).toString(36);
        this.mask = getPlayerMask(this.id, this.visa);
        /**
         * 昵称
         */
        this.nick = this.id;
        /**
         * 在线状态 online-在线 offline-离线 busy-忙碌
         */
        this.state = "online";
        var now = new Date;
        /**
         * 创建时间
         */
        this.createTime = now;
        /**
         * 访问时间
         */
        this.accessTime = now;
        /**
         * 修改时间
         */
        this.modifyTime = now;
        /**
         * 验证时间，用来判断是否离线
         */
        this.passportTime = now;
        /**
         * 最后发送命令的时间，用来验证是否活跃
         */
        this.commandTime = now;
        
        playerDict[this.id] = this;
        event.emit('storage-save', 'player', this.id, this);
    }
    /**
     * 更新用户信息
     * @param {Object} data 更新的数据
     */
    Player.prototype.update = function(data){
        var self = this;
        var changed = false;
        common.forEach(updateFields, function(field){
            if (field in data){
                if (self[field] != data[field]){
                    changed = true;
                    self[field] = data[field];
                    self.modifyTime = new Date;
                }
            }
        });
        changed && event.emit('storage-save', 'player', this.id, this);
    };

    function buildPlayer(id, callback){
        if (!callback) return; // 无回调
        
        if (!id){ // 新帐号
            player = new Player();
            playerDict[player.id] = player;
            callback(null, player);
            return;
        }
        
        var player = playerDict[id];
        if (player){
            player.accessTime = new Date;
            callback(null, player);
            return;
        }
        
        event.emit('storage-load', 'player', id, function(err, value){
            if (err){
                callback(err);
            } else {
                player = playerDict[id] = new Player(id, value);
                callback(null, player);
            }
        });
    }
    
    var badPassport = {};
    
    function buildPassport(request, respose, query, callback){
        var passportKey = query.passport;
        var headers = request.headers;
        // http://localhost:2012/command?passport=id%3Dp14ksl9%26visa%3D955ja%26mask%3D-mnbsgs
        // console.log('passportKey', passportKey);
        if (!passportKey){
            var cookie = headers['cookie'] || "";
            var m = cookie.match(/\bpassport=([^;]+)/);
            passportKey = m && m[1];
        }
        var passport = passportKey && querystring.parse(passportKey);
        
        var address = String(passportKey).substring(0, 30) + ',' + // 识别同一个用户
            + request.connection.remoteAddress
            + String(headers['x-real-ip'] || headers['x-forwarded-for']).replace(/,.*/, '')
            + String(headers['user-agent']).substring(0, 200);
        
        buildPlayer(passport && passport.id, function(err, player){
            if (!err && passport && player.id == passport.id && player.visa == passport.visa &&
                player.mask == getPlayerMask(passport.id, passport.visa)){
                callback(null, player);
                return;
            }
            if (badPassport[address] && new Date - badPassport[address].accessTime < 300000){
                player = badPassport[address].player;
                badPassport[address].accessTime = new Date;
            } else {
                player = new Player();
                playerDict[player.id] = player;
                if (passportKey){
                    badPassport[address] = {
                        player: player,
                        accessTime: new Date
                    };
                }
            }
            respose.setHeader("Set-Cookie", [common.format("passport=id=#{id}&visa=#{visa}&mask=#{mask}; expires=Mon, 31 Dec 2998 16:00:00 GMT; path=/;", player)]);
            callback(null, player);
        });
    }
    
    exports.buildPassport = buildPassport;
    exports.buildPlayer = buildPlayer;
    
}();