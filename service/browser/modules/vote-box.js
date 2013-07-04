/**
 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
 */
AceCore.addModule("VoteBox", function(sandbox){
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
	var voteTree;
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
				case "voteAll":
					voteTree.loadChilds(item.votes);
					break;
				case "voteUpdate":
					lib.each(item.votes, function(item) {
						var node = voteTree.updateData(item);
					});
					break;
			}
		});
	}
	
	return {
		init: function() {
			voteTree = AceTree.create({
				parent: lib.g("voteListTemplate").parentNode,
				oninit: function(tree){
					tree.eventHandler = AceEvent.on(tree.parent, function(command, target, e){
						var node = tree.node4target(target);
						node && tree.oncommand(command, node, e);
					});
				},
				onreader: function(node){
					return AceTemplate.format('voteListTemplate', node.data, {
						node: node
					});
				},
				oncommand: function(command, node, e){
					switch (command) {
						case "vote":
							if (node.data.voted) return;
							node.tree.each(function(item){
								item.data.voted = true;
							});
							node.tree.refresh();
							sandbox.fire(events.vote, node.data.id);
							break;
					}
				},
				statusClasses: /^(focus|hover|select|expand|self)$/
			});
			sandbox.on(events.pickSuccess, pickSuccess);
		}
	};
});