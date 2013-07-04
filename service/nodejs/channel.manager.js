var common = require('../common/channel.common.js');
var playerManager = require('./player.manager.js');
var event = require('./event');
var url = require('url');

void function(){
    /**
     * @author 王集鹄(wangjihu，http://weibo.com/zswang)
     * 频道管理器
     */
    /**
     * 频道集合
     */
    var channelDict = {};
    
    var pluginInfos = {};
    
    /**
     * 频道实例
     * @param{String} id 频道id
     * @param{Array Of Object} plugins 插件列表
     */
    function Channel(id){
        /**
         * 频道id
         */
        this.id = id;
        this.title = id;
        /**
         * pick缓存
         */
        this.pickDict = {};
        this.pickKey = 0;
        this.seqFields = [];
        this.currSeq = 1;
        this.minSeq = 0;
        this.plugins = {};
        var now = new Date;
        this.createTime = now;
        this.accessTime = now;
        this.modifyTime = now;
        /**
         * 清理过期数据
         */
        this.patrolTime = now;
    }
    /**
     * 执行回调
     * @param{Object} response 应答对象
     * @param{String} callback 回调函数名
     */
    Channel.prototype.callback = function(response, callback, json){
        response.writeHead(200, {
            'Content-Type': 'text/javascript'
        });
        response.end([callback, "(", JSON.stringify(json), ");"].join(""));
    };
    /**
     * 执行命令请求
     * @param{Object} passport 请求对象
     * @param{Object} query 请求参数
     * @param{Object} response 应答对象
     * @param{Object} request 请求对象
     */
    Channel.prototype.command = function(passport, query, response, request){
        var fields = [];
        var error;
        common.forEach(this.plugins, function(plugin){
            if (error) return;
            error = plugin.command && plugin.command(fields, passport, query, request);
        });
        this.callback(response, query.callback, {
            result: "ok",
            channel: this.id,
            error: error
        });
        this.fire(fields);
    };
    /**
     * 触发pick返回
     * @param{Array Of Object} 变化列表
     */
    Channel.prototype.fire = function(fields){
        if (!fields || !fields.length) return;
        this.seqFields.push({
            startSeq: this.currSeq,
            fields: fields
        });
        while (this.seqFields.length > common.maxFireCount) {
            this.minSeq = this.seqFields.shift().startSeq;
        }
        this.currSeq++;
        for (var key in this.pickDict) {
            var pickItem = this.pickDict[key];
            if (!pickItem) continue;
            clearTimeout(pickItem.timer);
            var data = {
                result: "ok",
                channel: this.id,
                currSeq: pickItem.query.seq,
                nextSeq: this.currSeq,
                fields: []
            };
            var response = pickItem.response;
            var query = pickItem.query;
            common.forEach(this.seqFields, function(item){
                if (data.currSeq > item.startSeq) return;
                item.fields.forEach(function(field){
                    // 处理黑名单
                    if (field.blackList &&
                    ['', field.blackList, ''].join().indexOf(['', pickItem.passport.id, ''].join()) >=
                    0) return;
                    // 处理白名单
                    if (field.whiteList &&
                    ['', field.whiteList, ''].join().indexOf(['', pickItem.passport.id, ''].join()) <
                    0) return;
                    data.fields.push(field);
                });
            });
            this.callback(response, query.callback, data);
        }
        this.pickDict = {};
    };
    /**
     * 处理用户pick请求
     * @param {Object} passport
     * @param {Object} query
     * @param {Object} response
     */
    Channel.prototype.pick = function(passport, query, response){
        if (query.seq <= this.minSeq) { // 首次访问或完整数据
            var fields = [{
                type: "passport",
                info: {
                    id: passport.id,
                    nick: passport.nick,
                    weibo: passport.weibo,
                    visa: passport.visa,
                    mask: passport.mask
                }
            }, {
                type: "channel",
                info: {
                    id: this.id,
                    title: this.title
                }
            }];
            common.forEach(this.plugins, function(plugin){
                plugin.all && plugin.all(fields, passport, query);
            });
            this.callback(response, query.callback, {
                result: "ok",
                channel: this.id,
                currSeq: query.seq,
                nextSeq: this.currSeq,
                fields: fields
            });
            return;
        }
        var key = this.pickKey++;
        var self = this;
        this.pickDict[key] = {
            passport: passport,
            query: query,
            key: key,
            response: response,
            timer: setTimeout(function(){
                self.pickDict[key] = null;
                self.callback(response, query.callback, {
                    channel: self.id,
                    result: "overtime"
                });
            }, common.pickWait)
        };
        
        var fields = [];
        this.patrol(fields); // 处理过期数据
        if (fields.length) {
            this.fire(fields);
        }
    };
    /**
     * 处理过期数据
     * @param {Array of Object} fields 返回动作列表
     * @param {Object} passport pick 触发者
     */
    Channel.prototype.patrol = function(fields){
        var now = new Date;
        if (now - this.patrolTime < common.maxPatrolTime) return;
        common.forEach(this.plugins, function(plugin){
            plugin.patrol && plugin.patrol(fields);
        });
        this.patrolTime = now;
    };

    event.on('request', function(request, response){
        try{
            var reqUrl = url.parse(request.url, true);
        }catch(ex){
            response.writeHead(200, {
                'Content-Type': 'text/html'
            });
            response.end("/* url is invalid. */");
            return;
        }
        var query = reqUrl.query;
        if (/[^\w_$]/.test(query.callback)) { // 错误的callback参数
            response.writeHead(200, {
                'Content-Type': 'text/html'
            });
            response.end("/* callback is invalid. */");
            return;
        }
        
        switch (reqUrl.pathname) {
            case "/command/":
            case "/command":
            case "/pick/":
            case "/pick":
                break;
            default:
                response.writeHead(200, {
                    'Content-Type': 'text/html'
                });
                response.end("/* pathname is invalid. */");
                return;
        }
        var name = query.channel || 'home';
        var channel = channelDict[name];
        if (!channel){
            channelDict[name] = channel = new Channel(name);
            
            var plugins = {};
            for (var key in pluginInfos) {
                var pluginInfo = pluginInfos[key];
                plugins[key] = new pluginInfo(channel);
            }
            channel.plugins = plugins;
        }
        
        playerManager.buildPassport(request, response, query, function(err, passport){
            passport.passportTime = new Date;
            switch (reqUrl.pathname) {
                case "/command/":
                case "/command":
                    channel.command(passport, query, response, request);
                    break;
                case "/pick/":
                case "/pick":
                    channel.pick(passport, query, response, request);
                    break;
            }        
        });
    });
    
    // plugins
    event.on('plugin-register', function(name, pluginCreator){
        pluginInfos[name] = pluginCreator;
    });

    event.on('channel-fire', function(name, fields){
        var channel = channelDict[name];
        if (!channel) return;
        channel.fire(fields);
    });
}();