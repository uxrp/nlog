var AceTree = AceTree || {};

void function(exports){
	/**
	 * Ace Engine Tree
	 * 一套基于模板和事件代理的通用树形控件
	 * @see http://code.google.com/p/ace-engine/wiki/AceTree
	 * @author 王集鹄(wangjihu，http://weibo.com/zswang)
	 * @version 1.0
	 * @copyright www.baidu.com
	 */
	/* Debug Start */
	var logger = {
		log: function(text){
			/*
			 var dom = document.getElementById("log");
			 if (dom) {
			 dom.value += text + "\n";
			 }
			 */
			window.console && console.log(text)
		}
	};
	/* Debug End */
	
	var lib = {
		/**
		 * 通过id获得DOM对象
		 * @param {String} id
		 */
		g: function(id){
			if (typeof id != "string") return id;
			return document.getElementById(id);
		},
		/**
		 * 移除当前DOM对象的HTML
		 * @param {Element|String} element
		 */
		remove: function(element){
			element = this.g(element);
			if (!element) return;
			element.parentNode.removeChild(element);
		},
		addClass: function(element, className){
			addAttr(element, "class", className);
		},
		removeClass: function(element, className){
			removeAttr(element, "class", className);
		},
		/**
		 * 变量数组
		 * @param {Array} source
		 * @param {Function} iterator
		 */
		each: function(source, iterator){
			if (!source || !iterator) return;
			for (var i = 0; i < source.length; i++) {
				if (iterator(source[i], i) === false) break;
			}
		},
		/**
		 * 获得对象的键值列表，测试用
		 */
		getKeys: function(obj){
			var result = [];
			for (var p in obj) {
				result.push(p);
			}
			return result;
		}
	};
	
	/**
	 * 检查是否包含关系,包括是否相同
	 * @param {Element|String} element
	 * @param {Element|String} parent
	 */
	function isAncestor(element, parent){
		element = lib.g(element);
		parent = lib.g(parent);
		if (element == parent) return true;
		var result;
		scanParent(parent, function(item){
			if (element == item) {
				result = false;
				return false;
			}
		});
		return result;
	}
	
	/**
	 * 替换当前DOM对象的HTML
	 * @param {Element|String} element
	 * @param {String} html
	 */
	function replaceWith(element, html){
		element = lib.g(element);
		if (!element) return;
		element.removeAttribute("id");
		element.parentNode.insertBefore(html2Fragment(html), element);
		element.parentNode.removeChild(element);
	}
	
	/**
	 * 添加当前DOM对象的HTML
	 * @param {Element|String} element
	 * @param {String} html
	 */
	function appendWith(element, html){
		element = lib.g(element);
		if (!element) return;
		element.appendChild(html2Fragment(html));
	}
	
	/**
	 * 添加一个属性值
	 * @param {Element|String} element
	 * @param {String} attrName
	 * @param {String} attrValue
	 */
	function addAttr(element, attrName, attrValue){
		element = lib.g(element);
		if (!element || !element.getAttribute || !element.setAttribute) return;
		var newValue = element.getAttribute(attrName);
		if (newValue) {
			var i = newValue.indexOf(attrValue);
			while (i >= 0) {
				if (/^\s*$/.test(newValue.charAt(i - 1) + newValue.charAt(i + attrValue.length))) return; // 出现相同的属性
				i = newValue.indexOf(attrValue, i + 1);
			}
			newValue += " " + attrValue;
		} else {
			newValue = attrValue;
		}
		element.setAttribute(attrName, newValue);
	}
	
	/**
	 * 移除一个属性值
	 * @param {Element|String} element
	 * @param {String} attrName
	 * @param {String} attrValue
	 */
	function removeAttr(element, attrName, attrValue){
		element = lib.g(element);
		if (!element || !element.getAttribute || !element.setAttribute) return;
		var newValue = element.getAttribute(attrName);
		if (!newValue) return;
		var i = newValue.indexOf(attrValue);
		while (i >= 0) {
			if (/^\s*$/.test(newValue.charAt(i - 1) + newValue.charAt(i + attrValue.length))) {
				element.setAttribute(attrName, newValue.substring(0, i) + newValue.substring(i + attrValue.length));
				return; // 出现被移除的属性
			}
			i = newValue.indexOf(attrValue, i + 1);
		}
	}
	
	/**
	 * 扫描容器
	 * @param {Element} element DOM对象
	 * @param {Function} scaner 扫描方法，返回true则停止扫描
	 */
	function scanParent(element, scaner){
		element = lib.g(element);
		if (!element || !scaner) return;
		while (element && !/^html$/i.test(element.tagName)) {
			if (scaner(element) == false) return;
			element = element.parentNode;
		}
	}
	
	/**
	 * 处理HTML碎片，保证innerHTML能够赋值
	 */
	var wrapMap = {
		option: [1, "<select multiple='multiple'>", "</select>"],
		legend: [1, "<fieldset>", "</fieldset>"],
		thead: [1, "<table>", "</table>"],
		tr: [2, "<table><tbody>", "</tbody></table>"],
		td: [3, "<table><tbody><tr>", "</tr></tbody></table>"],
		col: [2, "<table><tbody></tbody><colgroup>", "</colgroup></table>"],
		area: [1, "<map>", "</map>"],
		_default: [0, "", ""]
	};
	wrapMap.optgroup = wrapMap.option;
	wrapMap.tbody = wrapMap.tfoot = wrapMap.colgroup = wrapMap.caption = wrapMap.thead;
	wrapMap.th = wrapMap.td;
	/**
	 * 将HTML处理成DOM碎片
	 * @param {String} html HTML代码
	 */
	function html2Fragment(html){
		var fragment = document.createDocumentFragment();
		if (!html) return fragment;
		// "<div/>" -> "<div></div>"
		html = html.replace(/<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:]+)[^>]*)\/>/ig, "<$1></$2>");
		var tag = (html.match(/<([\w:]+)/) || ["", ""])[1].toLowerCase(), wrap = wrapMap[tag] || wrapMap._default, i = wrap[0], div = document.createElement("div");
		div.innerHTML = [wrap[1], html, wrap[2]].join("");
		while (i--) {
			div = div.lastChild;
		}
		while (div.childNodes.length) 
			fragment.appendChild(div.childNodes[0]);
		return fragment;
	}
	
	/**
	 * 节点字典
	 */
	var nodeDict = {};
	
	/**
	 * 句柄种子，为每一个实例分配一个句柄
	 */
	var handler = 0;
	
	/**
	 * 节点类
	 * @param {Node} parentNode 父节点
	 * @param {Object} data 数据
	 */
	function TemplateNode(parentNode, data){
		this.parentNode = parentNode;
		this.tree = parentNode.tree;
		this.layer = parentNode.layer + 1; // 层次
		this.id = data[this.tree.fieldIdentifier];
		this.did = [this.tree.didPrefix, this.tree.handler, this.id].join("-");
		this.cid = [this.tree.didPrefix, this.tree.handler, this.id, "childs"].join("-");
		nodeDict[this.did] = this;
		this.data = data;
		this.status = {};
		this.childNodes = [];
		if (this.tree.oncreated) {
			this.tree.oncreated(this);
		}
	}
	
	/**
	 * 移除自己
	 * @param {Boolean} pending 标志正在处理中，为true时则不更新dom节点
	 */
	TemplateNode.prototype.remove = function(pending){
		if (this.tree.focusDid == this.did) {
			this.tree.focusDid = "";
		}
		this.removeAll(true);
		delete nodeDict[this.did];
		if (pending) return;
		this.parentNode.childNodes = [].concat(this.parentNode.childNodes.slice(0, this.index), this.parentNode.childNodes.slice(this.index + 1)); // 移除容器的节点
		for (var i = this.index; i < this.parentNode.childNodes.length; i++) { // 更新序号
			this.parentNode.childNodes[i].index = i;
		}
		if (this.tree.onsort) {
			this.parentNode.refresh();
		} else {
			!isAncestor(this.did, this.cid) && lib.remove(this.cid); // 如果子节点容器是嵌套形式出现
			lib.remove(this.did);
		}
	};
	
	/**
	 * 添加子节点
	 * @param {Object} data 数据
	 * @param {Boolean} depth 是否深度添加，默认为true
	 */
	TemplateNode.prototype.appendChild = function(data, depth){
		if (!data) return;
		if (typeof depth == "undefined") depth = true;
		var node = new TemplateNode(this, data);
		node.index = this.childNodes.length;
		this.childNodes.push(node);
		if (depth) node.loadChilds(data[this.tree.fieldChilds], true);
		if (this.tree.onsort) {
			this.childNodes.sort(this.tree.onsort);
			replaceWith(this.did, this.reader());
		} else {
			appendWith(this.cid, node.reader());
		}
		return node;
	};
	
	/**
	 * 批量添加子节点
	 * @param {Array} datas 数据列表
	 * @param {Boolean} depth 是否深度添加，默认为true
	 */
	TemplateNode.prototype.appendChilds = function(datas, depth){
		if (!datas) return;
		if (typeof depth == "undefined") depth = true;
		var self = this;
		var nodes = [];
		lib.each(datas, function(data){
			var node = new TemplateNode(self, data);
			nodes.push(node);
			node.index = self.childNodes.length;
			self.childNodes.push(node);
			if (depth) node.loadChilds(data[self.tree.fieldChilds], true);
		});
		if (this.tree.onsort) {
			this.tree.onsort && this.childNodes.sort(this.tree.onsort);
			this.refresh();
		} else {
			var html = [];
			lib.each(nodes, function(data) {
				html.push(data.reader());
			});
			appendWith(this.cid, html.join(""));
		}
		return nodes;
	};
	
	/**
	 * 排序
	 * @param {Boolean} depth 是否深度排序
	 * @param {Boolean} pending 标志正在处理中，为true时则不更新dom节点
	 */
	TemplateNode.prototype.sort = function(depth, pending){
		if (!this.tree.onsort) return;
		this.childNodes.sort(this.tree.onsort);
		var result;
		lib.each(this.childNodes, function(node, index){
			if (node.index != index) {
				node.index = index; // 校对序号
				result = true;
			}
			if (depth && node.sort(true, true)) result = true;
		});
		if (result && !pending) {
			this.refresh(); // 重新渲染子节点
		}
		return result;
	};
	
	/**
	 * 更新数据
	 * @param {Object} data 数据项
	 * @param {Boolean} depth 是否深度更新，默认为false
	 */
	TemplateNode.prototype.updateData = function(data, depth){
		if (!data) return;
		var changed = false;
		for (var p in data) {
			if (this.data[p] !== data[p]) {
				this.data[p] = data[p];
				changed = true;
			}
		}
		if (depth) { // 深度更新则重新加载子节点
			this.loadChilds(data[this.tree.fieldChilds], true);
		}
		if (changed) this.refresh();
		/*
		if (changed || depth) {
			this.tree.onsort && this.parentNode.childNodes.sort(this.tree.onsort);
			this.parentNode.refresh();
		}
		*/
	};
	
	/**
	 * 获取当前节点的html字符串
	 * @return 返回渲染后的html字符串
	 */
	TemplateNode.prototype.reader = function(){
		if (!this.tree.onreader) return "";
		var childLines = [];
		lib.each(this.childNodes, function(node){
			childLines.push(node.reader());
		});
		if (this == this.tree) return childLines.join("");
		var self = this;
		return this.tree.onreader(this).replace(/(^\s*<\w+)([^>]*>)([\s\S]*$)/, function(all, $1, $2, $3){
			$3 = $3.replace(/(<\w+\b[^>]*)(\bdata-type="childs"[^>]*>)(\s*<\/\w+>)/, function(all, $1, $2, $3){
				if (!childLines.length) return "";
				return [$1, ' id="', self.cid, '"', $2, childLines.join(""), $3].join("");
			});
			return [$1, ' id="', self.did, '"', $2, $3].join("");
		});
	};
	
	/**
	 * 移除全部子节点
	 * @param {Boolean} pending 标志正在处理中，为true时则不更新dom节点
	 */
	TemplateNode.prototype.removeAll = function(pending){
		lib.each(this.childNodes, function(node){
			node.remove(true);
		});
		this.childNodes = [];
		if (pending) return;
		this.refresh();
	};
	
	/**
	 * 重新加载子节点
	 * @param {Array} datas 节点数据
	 * @param {Boolean} pending 标志正在处理中，为true时则不更新dom节点
	 */
	TemplateNode.prototype.loadChilds = function(datas, pending){
		if (this.childNodes.length == 0 && datas && datas.length == 0) return;
		this.removeAll(true);
		if (!datas) return;
		var self = this;
		lib.each(datas, function(data){
			var node = new TemplateNode(self, data);
			self.childNodes.push(node);
			node.loadChilds(data[self.tree.fieldChilds], true);
		});
		this.tree.onsort && this.childNodes.sort(this.tree.onsort);
		lib.each(this.childNodes, function(node, index){
			node.index = index;
		});
		!pending && this.refresh();
	};
	
	/**
	 * 遍历子节点
	 * @param {Function} iterator
	 */
	TemplateNode.prototype.each = function(iterator){
		var returnValue;
		iterator &&
		lib.each(this.childNodes, function(node){
			result = iterator(node) || node.each(iterator);
			if (returnValue === false) return returnValue;
		});
		return returnValue;
	};
	
	/**
	 * 改变状态值，同时渲染dom节点
	 * @param {String} name 状态名
	 * @param {Boolean} value 状态值
	 * @param {Boolean} pending 标志正在处理中，为true时则不更新dom节点
	 */
	TemplateNode.prototype.setStatus = function(name, value, pending){
		if (!name) return;
		// 状态为改变
		if (this.status[name] == value) return;
		this.status[name] = value;
		if (pending) return;
		if (this.tree.statusClasses instanceof RegExp) {
			if (this.tree.statusClasses.test(name)) {
				if (value) {
					lib.addClass(this.did, name);
					this.tree.childsClasses.test(name) && lib.addClass(this.cid, name);
				} else {
					lib.removeClass(this.did, name);
					this.tree.childsClasses.test(name) && lib.removeClass(this.cid, name);
				}
				return;
			}
		} if (typeof this.tree.statusClasses == "object") {
			var statusClasse = this.tree.statusClasses[name];
			if (statusClasse) {
				if (statusClasse instanceof Array) {
					if (value) {
						lib.addClass(this.did, statusClasse[0]);
						lib.removeClass(this.did, statusClasse[1]);
					} else {
						lib.addClass(this.did, statusClasse[1]);
						lib.removeClass(this.did, statusClasse[0]);
					}
				} else {
					if (value) {
						lib.addClass(this.did, statusClasse);
					} else {
						lib.removeClass(this.did, statusClasse);
					}
				}
				return;
			}
		}
		this.refresh();
	};
	
	TemplateNode.prototype.getStatus = function(name){
		return this.status[name];
	};
	
	/**
	 * 获取节点的类名
	 * @param {Boolean} isChilds 只是子节点容器相关类名
	 */
	TemplateNode.prototype.getClasses = function(isChilds){
		var result = [];
		for (var p in this.status) {
			if (!this.tree.statusClasses.test(p)) continue; // 状态和类名没有关系
			if (this.status[p] && (!isChilds || this.tree.childsClasses.test(p))) {
				result.push(p);
			}
		}
		return result.join(" ");
	};
	
	/**
	 * 重新渲染整个节点
	 */
	TemplateNode.prototype.refresh = function(){
		if (this == this.tree) {
			this.parent.innerHTML = TemplateNode.prototype.reader.call(this);
			return;
		}
		!isAncestor(this.did, this.cid) && lib.remove(this.cid); // 如果子节点容器是嵌套形式出现
		replaceWith(this.did, this.reader());
	};
	
	/**
	 *
	 */
	TemplateNode.prototype.expand = function(){
		this.setStatus("expand", true);
	};
	
	TemplateNode.prototype.collapse = function(){
		this.setStatus("expand", false);
	};
	
	TemplateNode.prototype.toggle = function(){
		this.setStatus("expand", !this.getStatus("expand"));
	};
	
	TemplateNode.prototype.nextNode = function(){
		if (this.childNodes.length && this.getStatus("expand")) {
			return this.childNodes[0];
		}
		return this.parentNode.childNodes[this.index + 1];
	};
	
	TemplateNode.prototype.previousNode = function(){
		if (!this.index) {
			if (this.parentNode == this.tree) return;
			return this.parentNode;
		}
		return this.parentNode.childNodes[this.index - 1];
	};
	
	/**
	 * 节点聚焦
	 * @param {Integer} scroll 0 -- 不滚动 1 -- 靠上滚动 2 -- 靠下滚动
	 */
	TemplateNode.prototype.focus = function(scroll){
		this.tree.setFocused(this, scroll);
	};
	
	/**
	 * 默认配置项
	 */
	var defaultOptions = {
		/**
		 * Dom id前缀
		 */
		didPrefix: "node",
		/**
		 * id字段名
		 */
		fieldIdentifier: "id",
		/**
		 * 子节点字段名
		 */
		fieldChilds: "nodes",
		/**
		 * 只需要改变类名的状态，其他则需要渲染
		 */
		statusClasses: /^(focus|hover|select|expand)$/,
		/**
		 * 涉及到子节点容器的类名
		 */
		childsClasses: /^(expand)$/
	};
	
	//=======================================================================================
	/**
	 * 模板树形控件
	 * @param {Object} options
	 *      {Element} parent 容器
	 *      {Function} onreader 渲染事件
	 *      {Function} onsort 排序事件
	 *      {Function} oninit 初始化事件
	 *      {Function} oncreated 节点创建事件
	 *      {Function} onfocus 聚焦事件
	 *      {Function} onselect 选中事件
	 *      {Function} onexpand 节点展开事件
	 */
	function TemplateTree(options){
		options = options || {};
		for (var p in defaultOptions) {
			!(p in options) && (options[p] = defaultOptions[p]);
		}
		for (var p in options) {
			this[p] = options[p];
		}
		this.tree = this;
		this.layer = 0;
		this.index = 0;
		this.handler = handler++;
		this.childNodes = [];
		this.focusDid = "";
		this.cid = this.parent;
		addAttr(this.parent, "data-handler", this.handler);
		if (this.oninit) this.oninit(this);
	}
	
	/**
	 * 加载子节点
	 * @param {Array} datas 子节点数据，采用深度加载
	 * @param {Boolean} reserves 是否保留节点状态
	 */
	TemplateTree.prototype.loadChilds = function(datas, reserves){
		if (this.childNodes.length == 0 && datas && datas.length == 0) return;
		reserves && this.saveStatus();
		TemplateNode.prototype.loadChilds.call(this, datas, true);
		reserves && this.restoreStatus();
		this.refresh();
	};
	
	/**
	 * 添加子节点
	 * @param {Object} data 数据
	 * @param {Boolean} depth 是否深度添加，默认为true
	 */
	TemplateTree.prototype.appendChild = function(data, depth){
		return TemplateNode.prototype.appendChild.call(this, data, depth);
	};
	
	/**
	 * 批量添加子节点
	 * @param {Array} datas 数据列表
	 * @param {Boolean} depth 是否深度添加，默认为true
	 */
	TemplateTree.prototype.appendChilds = function(datas, depth){
		return TemplateNode.prototype.appendChilds.call(this, datas, depth);
	};
	
	/**
	 * 更新数据
	 * @param {Object} data 数据
	 * @param {Boolean} depth 是否深度更新
	 */
	TemplateTree.prototype.updateData = function(data, depth){
		if (!data) return;
		var node = this.node4data(data);
		node && node.updateData(data, depth);
		return node;
	};
	
	/**
	 * 排序
	 * @param {Boolean} depth 是否深度更新，默认为true
	 */
	TemplateTree.prototype.sort = function(depth){
		TemplateNode.prototype.sort.call(this, true, depth || typeof depth == "undefiend");
	};
	
	/**
	 * 重新渲染整个节点
	 */
	TemplateTree.prototype.refresh = function(){
		TemplateNode.prototype.refresh.call(this);
	};
	
	/**
	 * 移除所有子节点
	 */
	TemplateTree.prototype.removeAll = function(){
		if (!this.childNodes.length) { // 如果节点未加载则不需要清空，也避免第一次将模板元素删除
			return;
		}
		TemplateNode.prototype.removeAll.call(this);
		this.refresh();
	};
	
	/**
	 * 移除子节点
	 * @param {Object|String} data 数据或者是ID字符串
	 */
	TemplateTree.prototype.removeNode = function(data){
		if (!data) return;
		var node = this.node4data(data);
		node && node.remove();
	};
	
	/**
	 * 移除子节点
	 * @param {Object|String|TemplateNode} data 数据或者是ID字符串
	 * @param {Integer} scroll 0 -- 不滚动 1 -- 靠上滚动 2 -- 靠下滚动
	 */
	TemplateTree.prototype.setFocused = function(data, scroll){
		if (!data) return;
		var node = this.node4data(data);
		if (!node || node.did == this.focusDid) return;
		var focused = nodeDict[this.focusDid];
		focused && focused.setStatus("focus", false);
		node && node.setStatus("focus", true);
		if (scroll) {
			var dom = lib.g(node.did);
			dom && dom.scrollIntoView(scroll == 1);
		}
		this.focusDid = node.did;
		if (this.onfocus) this.onfocus(node);
	};
	
	TemplateTree.prototype.getFocused = function(){
		return nodeDict[this.focusDid];
	};
	
	/**
	 * 通过数据查找节点
	 * @param {Object|String|TemplateNode} data 数据或者是ID字符串
	 */
	TemplateTree.prototype.node4data = function(data){
		if (!data) return;
		if (data instanceof TemplateNode && data.tree == this) return data;
		var id = typeof data == "string" ? data : data[this.fieldIdentifier];
		var did = [this.didPrefix, this.handler, id].join("-");
		if (nodeDict[did]) {
			return nodeDict[did];
		}
	};
	
	/**
	 * 通过Dom对象获取节点
	 * @param {String|Element} target Dom节点或者它的id
	 * @return {TemplateNode} 返回对象的模板节点对象，如果没有找到则返回undefined
	 */
	TemplateTree.prototype.node4target = function(target){
		var node = AceTree.node4target(target);
		if (!node || node.tree != this) return;
		return node;
	};
	
	/**
	 * 遍历子节点
	 * @param{Function} iterator 遍历方法
	 */
	TemplateTree.prototype.each = function(iterator){
		return TemplateNode.prototype.each.call(this, iterator);
	};
	
	/**
	 * 保存所有节点的状态
	 */
	TemplateTree.prototype.saveStatus = function(){
		var status = {};
		this.status = status;
		this.each(function(node){
			status[node.did] = node.status;
		});
	};
	
	/**
	 * 恢复之前节点的状态
	 */
	TemplateTree.prototype.restoreStatus = function(){
		if (!this.status) return;
		var status = this.status;
		this.each(function(node){
			if (node.did in status) node.status = status[node.did];
		});
		this.status = null;
	};
	//=======================================================================================
	/**
	 * 创建一个基于模板的树形控件
	 * @param {Object} options
	 */
	exports.create = function(options){
		return new TemplateTree(options);
	};
	
	/**
	 * 通过Dom对象获取节点
	 * @param {String|Element} target Dom节点或者它的id
	 * @return {TemplateNode} 返回对象的模板节点对象，如果没有找到则返回undefined
	 */
	exports.node4target = function(target){
		var result;
		target = lib.g(target);
		scanParent(target, function(element){
			var node = nodeDict[element.id];
			if (node) {
				result = node;
				return false;
			}
		});
		return result;
	};
}(AceTree);
