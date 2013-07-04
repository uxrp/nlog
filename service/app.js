/**
 * @author 王集鹄(wangjihu,http://weibo.com/zswang)
 * bae环境
 */
var http = require('http');
var event = require('./service/event');

require('./service/channel.manager.js');
require('./service/player.manager.js');
require('./service/chat.plugin.js');
require('./service/nlog.plugin.js');
require('./service/player.plugin.js');

//require('./file-storage');
require('./service/mysql-storage');

http.createServer(function(request, response){
    event.emit('request', request, response);

}).listen(process.env.APP_PORT);