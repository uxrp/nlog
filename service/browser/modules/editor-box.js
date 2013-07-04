/**
 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
 */
AceCore.addModule("EditorBox", function(sandbox){
    /**
     * 事件集合
     */
    var events = sandbox.getConfig("Events");
    /**
     * 类库
     */
    var lib = sandbox.getLib();

    return {
        init: function(){
            var eventHandler = AceEvent.on("editorTools", function(command, element, e){
                switch (command) {
                    case "send":
                        var text = lib.g("editor").value;
                        var formats = document.getElementsByName('format');
                        var format = '';
                        for (var i = formats.length - 1; i >= 0; i--){
                            if (formats[i].checked){
                                format = formats[i].value;
                                break;
                            }
                        }
                        var error = ChannelCommon.checkTalk(text);
                        if (error) {
                            sandbox.fire(events.showDialog, {
                                type: "error",
                                message: error
                            });
                            return true;
                        }
                        sandbox.fire(events.talk, {
                            text: text,
                            format: format
                        });
                        break;
                    case "focus":
                        lib.g('editor').focus();
                        break;
                }
            });
            
            lib.on('editor', 'keydown', function(e){
                switch (e.which || e.keyCode || e.charCode) {
                    case 13:
                        if (!(lib.g("ctrlEnter").checked ^ e.ctrlKey)) {
                            lib.event.stop(e);
                            AceEvent.fire(eventHandler, "send");
                        }
                        break;
                }
            });
        }
    };
});
