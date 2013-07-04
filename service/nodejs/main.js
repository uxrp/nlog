/**
 * @author 王集鹄(wangjihu,http://weibo.com/zswang)
 * 本机调试
 */
var http = require('http');
var event = require('./event');

require('./channel.manager.js');
require('./player.manager.js');
require('./chat.plugin.js');
require('./nlog.plugin.js');
require('./player.plugin.js');
require('./file-storage');

require('./file-storage');
//require('./mysql-storage');

http.createServer(function(request, response){
    event.emit('request', request, response);

}).listen(process.argv[2] || "80");