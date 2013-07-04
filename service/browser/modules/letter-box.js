/**
 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
 */
AceCore.addModule("LetterBox", function(sandbox){
	/**
	 * 事件集合
	 */
	var events = sandbox.getConfig("Events");
	/**
	 * 类库
	 */
	var lib = sandbox.getLib();
	/**
	 * 登录信息
	 */
	var passportInfo = {};
	/**
	 * 聊天室api
	 */
	var chatApi = sandbox.getExtension("ChatApi") || 
		sandbox.getExtension("ZhouziApi");
	/**
	 * 私信列表
	 */
	var letterTree;
	
	/**
	 * 获取房间当前状态成功
	 * @param {Object} data
	 */
	function pickSuccess(data){
		lib.each(data, function(item){
			switch (item.type) {
				case "passport":
					passportInfo = item.info;
					break;
				case "letterAll":
					letterTree.loadChilds(item.messages);
					letterTree.each(function(node){
						node.setStatus("old", node.data.time < item.lastView);
					});
					updateNewLetter();
					scrollBottom();
					break;
				case "letterAdd":
					letterTree.appendChilds(item.messages);
					scrollBottom();
					updateNewLetter();
					break;
			}
		});
	}
	
	/**
	 * 滚动到底部
	 */
	function scrollBottom(){
		var parent = letterTree.parent.parentNode;
		parent.scrollTop = parent.scrollHeight;
	}

	var newLetterNumber = 0;
	/**
	 * 更新新消息数
	 */
	function updateNewLetter(){
		newLetterNumber = 0;
		letterTree.each(function(node){
			if (!node.getStatus("old")) newLetterNumber++;
		});
		lib.g("newLetterNumber").innerHTML = newLetterNumber ? "(<b>" + newLetterNumber + "</b>)" : "";
	}
	/**
	 * 格式化时间
	 * @param {Date} time
	 */
	function formatTime(time){
		time = new Date(time);
		var timeStr = lib.date.format(time, "HH:mm:ss");
		var dateStr = lib.date.format(time, "yyyy:MM:dd");
		return lib.date.format(new Date, "yyyy:MM:dd") == dateStr ? timeStr : [dateStr, timeStr].join(" ");
	}
	
	/**
	 * 处理多行文本
	 * @param {String} text 文本
	 */
	function mutiline(text){
		return lib.encodeHTML(text).replace(/\n/g, "<br/>");
	}
	
	/**
	 * 发送私信对话框
	 */
	function letterDialog(data){
		sandbox.fire(events.showDialog, {
			type: "letter",
			nick: data.nick,
			to: data.to,
			oncommand: function(command, data){
				if (command != "ok") return;
				var letter = lib.g("inputLetter").value;
				var error = ChannelCommon.checkLetter(letter);
				if (error) {
					sandbox.fire(events.showDialog, {
						type: "error",
						message: error
					});
					return true;
				}
				chatApi.command({
					command: "letter",
					to: data.to,
					text: letter
				});
				
			},
			onshow: function(data){
				var input = lib.g("inputLetter");
				input.setSelectionRange(0, input.value.length);
				input.focus();
			}
		});
	}
	
	function viewLetter(){
		lib.removeClass('letterBox', 'hidden');
		if (!newLetterNumber) return;
		chatApi.command({
			command: "viewLetter"
		});
	}
	
	return {
		init: function(){
		
			letterTree = AceTree.create({
				parent: lib.g("letterListTemplate").parentNode,
				oninit: function(tree){
					tree.eventHandler = AceEvent.on(tree.parent, function(command, element, e){
						var node = tree.node4target(element);
						node && tree.oncommand(command, node, e);
					});
				},
				onreader: function(node){
					return AceTemplate.format('letterListTemplate', node.data, {
						node: node,
						formatTime: formatTime,
						mutiline: mutiline
					});
				},
				oncommand: function(command, node, e){
					switch (command) {
						case "reply":
							sandbox.fire(events.letterDialog, {
								nick: node.data.nick,
								to: node.data.from
							});
							break;
					}
				},
				statusClasses: /^(focus|hover|select|expand|old|self)$/,
				oncreated: function(node) {
					node.setStatus("self", node.data.from == passportInfo.id, true);
				}
			});
			
			sandbox.on(events.letterDialog, letterDialog);
			sandbox.on(events.pickSuccess, pickSuccess);
			sandbox.on(events.viewLetter, viewLetter);
			
			AceEvent.on('letterBox', function(command){
				switch (command) {
					case "ok":
					case "cancel":
						lib.addClass('letterBox', 'hidden');
						if (!newLetterNumber) return;
						letterTree.each(function(node){
							node.setStatus("old", true);
						});
						updateNewLetter();
						break;
				}
			});
		}
	};
});
