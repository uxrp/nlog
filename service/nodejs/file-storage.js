var event = require('./event');
var fs = require('fs');
var path = require('path');
var root = process.env['storage-root'] || 'storage';
 
event.on('storage-save', function(table, key, value, callback){
    var dir = root;
    if (!fs.existsSync(dir)){
        fs.mkdirSync(dir, 0777);
        console.log('mk', dir);
    }
    var dir = path.join(dir, table);
    if (!fs.existsSync(dir)){
        fs.mkdirSync(dir, 0777);
        console.log('mk', dir);
    }
    fs.writeFile(path.join(dir, key), JSON.stringify(value), function(err){
        if (callback) callback(err);
    });
});

event.on('storage-load', function(table, key, callback){
    if (!callback) return;
    fs.readFile(path.join(root, table, key), function(err, data){
        if (err){
            callback(err, null);
        } else {
            callback(err, JSON.parse(data));
        }
    });
});