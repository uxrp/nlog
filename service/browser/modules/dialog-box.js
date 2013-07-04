/**
 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
 */
AceCore.addModule("DialogBox", function(sandbox){
	/**
	 * 事件集合
	 */
	var events = sandbox.getConfig("Events");
	/**
	 * 类库
	 */
	var lib = sandbox.getLib();
	/**
	 * 对话框标识
	 */
	var handler = 0;

	/**
	 * 显示对话框
	 * @param {Object} data 对话框参数
	 * 	{string} type  对话框类型
	 * 	{function} onshow 显示时触发
	 * 	{function} oncommand 点击按钮时触发，返回true表示不关闭对话框
	 * 	{function} onclose 关闭后触发
	 */
	function showDialog(data) {
		data._handler = handler++;
		dialogTree.appendChild(data);
		data.onshow && data.onshow(data);
	}
	
	/**
	 * 关闭对话框 
	 * @param {Object} data 对话框参数
	 */
	function closeDialog(data) {
		dialogTree.removeNode(data);
		data.onclose && data.onclose(data);
	}

	return {
		init: function(){
			dialogTree = AceTree.create({
				fieldIdentifier: "_handler",
				parent: lib.g("dialogTemplate").parentNode,
				oninit: function(tree){
					tree.eventHandler = AceEvent.on(tree.parent, function(command, element, e){
						var node = tree.node4target(element);
						node && tree.oncommand(command, node, e);
					});
				},
				onreader: function(node){
					return AceTemplate.format('dialogTemplate', node.data);
				},
				oncommand: function(command, node, e){
					var result = node.data.oncommand && node.data.oncommand(command, node.data);
					if (!result) closeDialog(node.data);
				}
			});

			sandbox.on(events.showDialog, showDialog);
		}
	};
});
