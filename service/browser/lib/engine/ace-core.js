var AceCore = typeof exports == "undefined" ? AceCore || {} : exports;

void function(exports){

	/**
	 * @author 王集鹄(wangjihu，http://weibo.com/zswang) 李学健(lixuejian，http://weibo.com/poppyrr)
	 * @fileoverview 内核 注册模块、事件管理、类库引用
	 * @version 1.0.2
	 * @copyright www.baidu.com
	 */
	/**
	 * 监听事件集合
	 */
	var listeners = {};
	/**
	 * 配置项
	 */
	var configs = {};
	/**
	 * 程序是否启动
	 */
	var active = false;
	/**
	 * 事件缓存
	 */
	var eventCaches = [];
	/**
	 * 类库
	 */
	var lib = {
		/**
		 * 格式化函数
		 * @param {String} template 模板
		 * @param {Object} json 数据项
		 */
		format: function(template, json){
			return template.replace(/#\{(.*?)\}/g, function(all, key){
				return json && (key in json) ? json[key] : "";
			});
		}
	};
	
	/**
	 * 模块集合
	 */
	var modules = {};
	
	/**
	 * 扩展集合
	 */
	var extensions = {};
	
	/**
	 * 日志记录
	 */
	var logger = {
		log: function(message){
			/* Debug Start */
			typeof console == "object" && console.log(message);
			/* Debug End */
		}
	};
	
	/**
	 * 注册监听
	 * @param {String|Array} event 事件名
	 * @param {Function} handler 处理方法
	 */
	function on(event, handler){
		if (typeof handler != "function") return;
		
		if (event instanceof Array) {
			var i = event.length;
			while (i--) {
				on(event[i], handler);
			}
			return;
		}
		
		listeners[event] = listeners[event] || [];
		listeners[event].unshift(handler); // 向前添加
	}
	
	/**
	 * 注销监听
	 * @param {String|Array} event 事件名
	 * @param {Object} handler 处理方法
	 */
	function un(event, handler){
		if (typeof handler != "function") return;
		
		if (event instanceof Array) {
			var i = event.length;
			while (i--) {
				un(event[i], handler);
			}
			return;
		}
		
		var listener = listeners[event];
		if (!listener) return;
		var i = listener.length;
		while (i--) {
			if (listener[i] === handler) {
				listener.splice(i, 1);
			}
		}
	}
	
	/**
	 * 触发事件
	 * @param {String} event 取消事件
	 * @param {Object} data 事件参数
	 */
	function fire(event, data){
		if (!active) { // 程序未启动，先做缓存
			eventCaches.push([event, data]);
			return;
		}
		
		var listener = listeners[event];
		if (!listener) return;
		var i = listener.length;
		while (i--) {
			try {
				listener[i](data, event);
			} catch(ex) {
				logger.log([event, ex.message].join(" : "));
			}
		}
	}
	
	/**
	 * 获取扩展
	 * @param {String} id 扩展标识
	 */
	function getExtension(id){
		var extension = extensions[id];
		return extension && extension.instance;
	}
	
	/**
	 * 获取模块的配置信息
	 * @param {String} id 模块标识
	 */
	function getConfig(id){
		return configs[id] || {};
	}
	
	/**
	 * 启动模块
	 * @param {String} id 模块ID
	 */
	function startModule(id){
		var module = modules[id];
		if (!module || module.instance || !module.creator) return;
		module.instance = module.creator({
			on: on,
			un: un,
			fire: fire,
			getConfig: function(name){
				return function(_name){
					return getConfig(_name || name);
				}
			}(id),
			getExtension: getExtension,
			getLib: getLib,
			log: function(name){
				return function(message){
					logger.log && logger.log([name, message].join(" : "));
				}
			}(id)
		});
		if (module.instance.init) module.instance.init();
	}
	
	/**
	 * 启动扩展
	 * @param {String} id 扩展ID
	 */
	function startExtension(id){
		var extension = extensions[id];
		if (!extension || extension.instance || !extension.creator) return;
		extension.instance = extension.creator({
			getConfig: function(name){
				return function(_name){
					return getConfig(_name || name);
				}
			}(id),
			getExtension: getExtension,
			getLib: getLib,
			log: function(name){
				return function(message){
					logger.log && logger.log([name, message].join(" : "));
				}
			}(id)
		});
		if (extension.instance.init) extension.instance.init();
	}

	/**
	 * 停用模块
	 * @param {String} id 模块ID
	 */
	function stopModule(id){
		var module = modules[id];
		if (!module || !module.instance) return;
		if (module.instance.destroy) module.instance.destroy();
		module.instance = null;
	}
	
	/**
	 * 停用扩展
	 * @param {String} id 扩展ID
	 */
	function stopExtension(id){
		var extension = extensions[id];
		if (!extension || !extension.instance) return;
		if (extension.instance.destroy) extension.instance.destroy();
		extension.instance = null;
	}
	
	/**
	 * 注册配置文件
	 * @param {String} id 配置文件标识，如果为空表示根级修改
	 * @param {Object} options 配置项
	 */
	function addConfig(id, options){
		if (!options) return;
		var config = configs[id] || {};
		for (var p in options) {
			config[p] = options[p];
		}
		configs[id] = config;
	}
	
	/**
	 * 注册模块
	 * @param {String} id 模块标识
	 * @param {Function} creator 构造器
	 */
	function addModule(id, creator){
		var module = modules[id];
		if (module) return;
		modules[id] = {
			creator: creator
		};
		active && startModule(id);
	}
	
	/**
	 * 注册模块
	 * @param {String} id 模块标识
	 * @param {Function} creator 构造器
	 */
	function addExtension(id, creator){
		var extension = extensions[id];
		if (extension) return;
		extensions[id] = {
			creator: creator
		};
		active && startExtension(id);
	}
	
	/**
	 * 启动内核
	 * @param {Object} options 配置项
	 * 	{Object} lib 类库
	 * 	{Object} logger 日志器
	 */
	function start(options){
		if (active) return;
		options = options || {};
		lib = options.lib || lib;
		logger = options.logger || logger;
		for (var id in extensions) {
			startExtension(id);
		}
		for (var id in modules) {
			startModule(id);
		}
		active = true;
		
		//处理缓存的事件
		var item;
		while (item = eventCaches.shift()) {
			fire(item[0], item[1]);
		}
		logger.log && logger.log("core start.");
	}
	
	/**
	 * 停止内核
	 */
	function stop(){
		if (!active) return;
		for (var id in modules) {
			stopModule(id);
		}
		for (var id in extensions) {
			stopExtension(id);
		}
		active = false;
		logger.log && logger.log("core stop.");
	}
	
	/**
	 * 返回类库
	 */
	function getLib() {
		return lib;
	}
	
	exports.addConfig = addConfig;
	exports.addModule = addModule;
	exports.addExtension = addExtension;
	exports.start = start;
	exports.stop = stop;
	
}(AceCore);
