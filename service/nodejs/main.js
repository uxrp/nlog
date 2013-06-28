/**
 * @author 王集鹄(wangjihu,http://weibo.com/zswang)
 */
var http = require('http');
var event = require('./event');
var url = require('url');
var zlib = require('zlib');
var crypto = require('crypto');

require('./file-storage');

http.createServer(function(request, response) {
    if (request.method != 'POST') {
    	response.write("method error.");
    	response.end();
        return;
    }
    console.log(request);
    //response.pipe(zlib.createGunzip()).pipe(output);
    var gunzip = zlib.createGunzip();
    var text = "";
    gunzip.on('data', function(data){
        text += data.toString();
    });

    gunzip.on('end', function(){
    	var data = {
    		url: request.url,
    		id: crypto.createHash('md5').update(text).digest('hex'),
    		text: text
    	};
		event.emit('storage-save', 'nlog', data.id, data);
    	response.write(text);
    	response.end();
    });
	request.pipe(gunzip);

}).listen(process.argv[2] || "80");