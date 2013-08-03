var event = require('./event');

var url = require('url');
var zlib = require('zlib');
var crypto = require('crypto');
var querystring = require('querystring');

void function(){
    /**
     * @author 王集鹄(wangjihu，http://weibo.com/zswang)
     */
    function NLogPlugin(channel, options){
        this.channel = channel;
        this.lastItems = [];
        options = options || {};
    }

    NLogPlugin.prototype.command = function(fields, passport, query, request){
        if (!fields || !passport || !query || !request){
            return;
        }

        switch (query.command) {
            case 'nlog-post':
                if (!/^post$/i.test(request.method)) return;
                //if (request.headers['content-type'] != 'gzip') return;

                var me = this;
                var text = "";
                request.pipe(zlib.createGunzip()).on('data', function(data){
                    text += data;
                }).on('end', function(){
                    var data = {
                        url: request.url,
                        id: crypto.createHash('md5').update(text).digest('hex'),
                        text: text
                    };

                    event.emit('storage-load', 'nlog', data.id, function(err, value){
                        console.log(err, value);
                        if (!err){
                            console.log(data.id + ' exists');
                            return;
                        }
                        event.emit('storage-save', 'nlog', data.id, data);
                        var addItems = [];
                        text.split(/\n/).forEach(function(item, index){
                            if (!item) return;
                            try{
                                var params = querystring.parse(item);
                            }catch(ex){
                                console.log(ex.message);
                                return;
                            }
                            var nlogItem = {
                                head: query,
                                params: params
                            };

                            me.lastItems.push(nlogItem);
                            addItems.push(nlogItem);
                        });

                        var fields = [];
                        var messages = [];
                        addItems.forEach(function(item){
                            messages.push(buildMessage(item));
                        });
                        fields.push({
                            type: "messageAdd",
                            messages: messages
                        });  
                        me.channel.fire(fields);

                        while (me.lastItems.length >= 30) {
                            me.lastItems.shift();
                        }
                    });
                });
                break;
        }
    };

    function jsonFormat(json) {
        var result = '';
        for (var key in json) {
            result += "[red]" + key + "[/red][blue]: [/blue] " + json[key] + '\n';
        }
        return result;
    }
    function buildMessage(item){
        return {
            id: 'nlog_' + (+new Date).toString(36),
            from: 100000001,
            nick: "-----nlog-----",
            weibo: "",
            time: new Date(+item.params.time || +item.params.t || +new Date),
            format: 'ubb',
            message: 
                "[b]head:[/b]\n" + jsonFormat(item.head) +
                "[b]params:[/b]\n" + jsonFormat(item.params)

        };
    }
    NLogPlugin.prototype.all = function(fields, passport, query){
        if (!this.lastItems.length) {
            return;
        }

        var messages = [];
        this.lastItems.forEach(function(item){
            messages.push(buildMessage(item));
        });

        fields.push({
            type: "messageAdd",
            messages: messages
        });  
    };

    exports.create = function(channel, options){
        return new NLogPlugin(channel, options);
    };
    
    event.emit('plugin-register', 'nlog', NLogPlugin);
}();
