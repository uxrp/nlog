/**
 * 事件处理中心
 */
var events = require('events');
var emitter = new events.EventEmitter();
console.log('emitter');
for (var p in emitter){
    if (typeof emitter[p] == 'function'){
        exports[p] = (function(name){
            return function(){
                emitter[name].apply(emitter, arguments);
            }
        })(p);
    }
}