/**
 * 部署在BAE环境
 * @see http://developer.baidu.com/
 */

var event = require('./event');
var mysql = require('mysql');
var util = require('util');

var common = {
    safeParam: function(param){
        return String(param || '').replace(/\\/g, "\\\\").replace(/'/g, "''");
    }
};
var client = mysql.createConnection({
    host: process.env.BAE_ENV_ADDR_SQL_IP,
    port: process.env.BAE_ENV_ADDR_SQL_PORT,
    user: process.env.BAE_ENV_AK,
    password: process.env.BAE_ENV_SK,
    database: 'LXQSWLhwYQbGTFZwuCNr'
});    

client.connect();
client.query("\
CREATE TABLE IF NOT EXISTS storage(\n\
    id BIGINT UNSIGNED NULL AUTO_INCREMENT COMMENT '记录id',\n\
    `table` VARCHAR(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL COMMENT '表名',\n\
    `key` VARCHAR(100) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL COMMENT '键值',\n\
    `value` LONGTEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL COMMENT '内容',\n\
    modified TIMESTAMP NOT NULL COMMENT '更新时间',\n\
\n\
    PRIMARY KEY (id),\n\
    UNIQUE (`table`, `key`)\n\
)\n\
");

event.on('storage-save', function(table, key, value, callback){
    client.query(util.format("REPLACE INTO storage(`table`, `key`, `value`) VALUES('%s', '%s', '%s')", 
        common.safeParam(table), common.safeParam(key), 
            common.safeParam(JSON.stringify(value))
        ),
        function(err, result){
            callback && callback(err, result);
        }
    );
});

event.on('storage-load', function(table, key, callback){
    if (!callback) return;
    client.query(util.format("SELECT `value` FROM storage WHERE `table` = '%s' AND `key` = '%s'",
        common.safeParam(table), common.safeParam(key)),
        function(err, result){
            if (err){
                callback(err, null);
            } if (!result.length){
                callback('not found.');
            } else {
                //[{"value":"{\"id\":\"ddd\",\"nick\":\"ddd\"}"}]
                var data;
                try{
                    data = JSON.parse(result[0].value);
                }catch(ex){
                    callback(ex.message);
                    return;
                }
                callback(null, data);
            }
        });
});