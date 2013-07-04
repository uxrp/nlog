/**
 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
 */
AceCore.addModule("PlayerBox", function(sandbox){
	/**
	 * 事件集合
	 */
	var events = sandbox.getConfig("Events");
	/**
	 * 类库
	 */
	var lib = sandbox.getLib();
	/**
	 * 用户列表
	 */
	var playerTree;
	/**
	 * 登录信息
	 */
	var passportInfo = {};
	/**
	 * 获取房间当前状态成功
	 * @param {Object} data
	 */
	function pickSuccess(data) {
		lib.each(data, function(item) {
			switch(item.type) {
				case "passport":
					passportInfo = item.info;
					break;
				case "playerAll":
					playerTree.loadChilds(item.players);
					break;
				case "playerAdd":
					playerTree.appendChilds(item.players);
					break;
				case "playerUpdate":
					lib.each(item.players, function(player) {
						var node = playerTree.updateData(player);
						if (passportInfo.id == player.id) {
							passportInfo.nick = node.data.nick;
						}
					});
					break;
				case "playerRemove":
					lib.each(item.players, function(player) {
						playerTree.removeNode(player);
					});
					break;
			}
		});
	}
	
	return {
		init: function() {
			playerTree = AceTree.create({
				parent: lib.g("playerListTemplate").parentNode,
				/*
				onsort: function(a, b){
					return (a.data.commandTime || 0) - (b.data.commandTime || 0);
				},
				*/
				oninit: function(tree){
					tree.eventHandler = AceEvent.on(tree.parent, function(command, target, e){
						var node = tree.node4target(target);
						node && tree.oncommand(command, node, e);
					});
				},
				onreader: function(node){
					return AceTemplate.format('playerListTemplate', node.data, {
						node: node
					});
				},
				oncommand: function(command, node, e){
					switch (command) {
						case "focus":
							node.focus();
							sandbox.fire(events.playerFocus, {
								info: node.data
							});
							break;
						case "letter":
							sandbox.fire(events.letterDialog, {
								nick: node.data.nick,
								to: node.data.id
							});
							break;
					}
				},
				statusClasses: /^(focus|hover|select|expand|self)$/,
				oncreated: function(node) {
					node.setStatus("self", node.data.id == passportInfo.id, true);
				}
			});
			
			sandbox.on(events.pickSuccess, pickSuccess);
			AceEvent.on('playerTools', function(command) {
				switch (command) {
					case 'nick':
						sandbox.fire(events.showDialog, {
							type: 'nick',
							maxNick: ChannelCommon.maxNick,
							nick: passportInfo.nick,
							oncommand: function(command, data) {
								if (command != "ok") return;
								var nick = lib.g("inputNick").value;
								var error = ChannelCommon.checkNick(nick);
								if (error) {
									sandbox.fire(events.showDialog, {
										type: "error",
										message: error
									});
									return true;
								}
								if (nick != data.nick) {
									sandbox.fire(events.nick, nick);
								}
							},
							onshow: function(data) {
								var input = lib.g("inputNick");
								input.setSelectionRange(0, input.value.length);
								input.focus();
							}
						});
						break;
					case 'weibo':
						sandbox.fire(events.showDialog, {
							type: 'weibo',
							maxWeibo: ChannelCommon.maxWeibo,
							weibo: passportInfo.weibo || 'http://weibo.com/',
							oncommand: function(command, data) {
								if (command != "ok") return;
								var weibo = lib.g("inputWeibo").value;
								var error = ChannelCommon.checkWeibo(weibo);
								if (error) {
									sandbox.fire(events.showDialog, {
										type: "error",
										message: error
									});
									return true;
								}
								if (weibo != data.weibo) {
									sandbox.fire(events.weibo, weibo);
								}
							},
							onshow: function(data) {
								var input = lib.g("inputWeibo");
								var length = input.value.length;
								input.setSelectionRange &&
									input.setSelectionRange(length, length);
								input.focus();
							}
						});
						break;
					case 'viewLetter':
						sandbox.fire(events.viewLetter);
						break;
				}
			});
		}
	};
});