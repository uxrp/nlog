var ChannelCommon = typeof exports == "undefined" ? ChannelCommon || {} : exports;

void function(exports){

    /**
     * @author 王集鹄(wangjihu，http://weibo.com/zswang)
     */
    /**
     * 模板处理
     * @param {String} template 模板字符 #{key}
     * @param {Object} json 数据
     */
    function format(template, json){
        if (!json) return template;
        return template.replace(/\#\{(.+?)\}/g, function(){
            return json[arguments[1]];
        });
    }
    /**
     * 遍历数组或对象
     * @param {Object|Array} arr
     * @param {Object} callback 回调
     *     @param {Object} item 子项
     *     @param {Number|String} key 下标和键值
     */
    function forEach(arr, callback){
        if (arr instanceof Array) {
            for (var i = 0; i < arr.length; i++) {
                if (callback(arr[i], i) === false) return false;
            }
        } else if (typeof arr == "object") {
            for (var p in arr) {
                if (callback(arr[p], p) === false) return false;
            }
        }
    }
    
    function safeParam(param){
        return String(param || '').replace(/\\/g, "\\\\").replace(/'/g, "''");
    }
    
    /**
     * 公用部分
     */
    var ext = {
        /* Debug Start */
        // 服务器配置参数
        /**
         * 验证私钥
         */
        passportKey: 20110815,
        /**
         * 等待时间
         */
        pickWait: 30 * 1000,
        /**
         * 最大缓存的变化数
         */
        maxFireCount: 15,
        /**
         * 清理用户掉线的时间
         */
        maxPatrolTime: 45 * 1000,
        /**
         * 离线的时间差
         */
        offineTime: 75 * 1000,
        
        /**
         * 忙碌的时间差
         */
        busyTime: 100 * 1000,
        /* Debug End */
        
        // 前后端共用
        /**
         * 昵称最大长度
         */
        maxNick: 20,
        /**
         * 微博最大长度
         */
        maxWeibo: 150,
        /**
         * 私信最大长度
         */
        maxLetter: 4000,
        /*
         * 对话内容最大长度
         */
        maxTalk: 2000,
        /**
         * 验证昵称是否合法
         * @param {Object} nick
         */
        checkNick: function(nick){
            if (!nick || /^\s+$/.test(nick)) {
                return "昵称不能为空";
            }
            if (nick.length > this.maxNick) {
                return this.format("昵称长度不能超过#{0}", [this.maxNick]);
            }
            if (/@/.test(nick)) {
                return "昵称不能带@";
            }
        },
        /**
         * 验证微博是否合法
         * @param {String} weibo
         */
        checkWeibo: function(weibo){
            if (/^\s+$/.test(weibo)) {
                return "微博不能为空";
            }
            if (!weibo) return;
            if (weibo.length > this.maxWeibo) {
                return this.format("昵称长度不能超过#{0}", [this.maxWeibo]);
            }
            if (!(/^http:\/\/(weibo\.com|t\.qq\.com|t\.163\.com)\/\w+$/.test(weibo))) {
                return "微博格式不正确，仅只支持：http://weibo.com|http://t.qq.com|http://t.163.com";
            }
        },
        /**
         * 验证私信是否合法
         * @param {Object} letter
         */
        checkLetter: function(letter){
            if (!letter || /^\s+$/.test(letter)){
                return "私信不能为空";
            }
            if (letter.length > this.maxLetter){
                return this.format("私信长度不能超过#{0}", [this.maxLetter]);
            }
        },
        /**
         * 验证对话是否合法
         * @param {Object} talk
         */
        checkTalk: function(talk){
            if (!talk || /^\s+$/.test(talk)){
                return "对话内容不能为空";
            }
            if (talk.length > this.maxTalk){
                return this.format("对话内容长度不能超过#{0}", [this.maxTalk]);
            }
        },
        format: format,
        forEach: forEach,
        safeParam: safeParam
    };
    
    forEach(ext, function(value, key){
        exports[key] = value;
    });
    
}(ChannelCommon);
