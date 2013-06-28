package com.baidu.nlog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import android.content.Context;

public final class NLog {
    /**
     * nlog
     * @description Native统计框架
     * @author 王集鹄(WangJihu,http://weibo.com/zswang),彭正山(PengZhengshan)
     * @version 1.0
     * @copyright www.baidu.com
     */
	
	/**
	 * 设备上下文
	 */
	private Context context;
	/**
	 * 获取设备上下文
	 */
	public Context getContext() {
		return context;
	}
	
	/**
	 * 采集模块启动的时间
	 */
	private Long startTime = System.currentTimeMillis();
	public Long getStartTime() {
		return startTime;
	}
	
	/**
	 * 获取时间戳 
	 * @param now 当前时间
	 * @return 返回差值
	 */
	public Long timestamp(Long now) {
		return System.currentTimeMillis() - now;
	}
	
	/**
	 * session超时时间，单位：秒
	 */
	private Integer sessionTimeout = 30;
	
	/**
	 * 获取session超时时间
	 * @param context 上下文
	 * @return 返回session超时时间
	 */
	public static Integer getSessionTimeout(Context context) {
		NLog instance = getInstance(context);
		return instance.sessionTimeout;
	}
	/**
	 * 获取session超时时间
	 * @return 返回session超时时间，单位：秒
	 */
	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	/**
	 * 设置session超时时间
	 * @param context 上下文
	 * @param value 超时时间，单位：秒，默认30
	 */
	public static void setSessionTimeout(Context context, Integer value) {
		NLog instance = getInstance(context);
		instance.setSessionTimeout(value);
	}
	/**
	 * 设置session超时时间
	 */
	public void setSessionTimeout(Integer value) {
		if (sessionTimeout == value) {
			return;
		}
		sessionTimeout = value;
		fire("sessionTimeoutChange", "value=", value);
	}
	
	/**
	 * 启动新会话
	 */
	private void createSession() {
		buildSessionId();
		startTime = System.currentTimeMillis();
		fire("createSession", "sessionId=", sessionId);
	}
	
	/**
	 * 获取时间戳 
	 * @return 返回差值
	 */
	public Long timestamp() {
		return System.currentTimeMillis() - startTime;
	}
	
	/**
	 * 获取时间戳 
	 * @param context 上下文
	 * @param now 当前时间
	 * @return 返回差值
	 */
	public static Long timestamp(Context context, Long now) {
		NLog instance = getInstance(context);
		return instance.timestamp(now);
	}
	
	/**
	 * 会话id, 当前时间毫秒36进制+随机数
	 */
	private String sessionId;
	public String getSessionId() {
		return sessionId;
	}
	
	/**
	 * 获取时间戳
	 * @param context 上下文 
	 * @return 返回差值
	 */
	public static Long timestamp(Context context) {
		NLog instance = getInstance(context);
		return instance.timestamp();
	}

	/**
	 * 是否处于debug状态
	 */
	private Boolean debug = false;
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	public static void setDebug(Context context, Boolean debug) {
		NLog instance = getInstance(context);
		instance.setDebug(debug);
	}
	public Boolean getDebug() {
		return debug;
	}
	public static Boolean getDebug(Context context) {
		NLog instance = getInstance(context);
		return instance.getDebug();
	}
	
	/**
	 * 固定随机数，用于计算采样率
	 */
	private static Double randomSeed = Math.random();
	public static Double getRandomSeed() {
		return randomSeed;
	}

	/**
	 * 命令字符串解析，如："wenku.set" -> ["wenku", "set"] "set" -> [null, "set"]
	 */
	private static Pattern cmdPattern = Pattern.compile("^(?:([\\w$_]+)\\.)?(\\w+)$");

	/**
	 * 实例集合，以context为下标
	 */
	private static Map<Context, NLog> instances = new HashMap<Context, NLog>();

    /**
     * 将参数数组转换成字典，为了简化调用方式
     * @param params 参数列表
     * @param offset 起始位置
     * @return 返回key-value集合
     */
    public static Map<String, Object> buildMap(Object[] params, Integer offset) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Integer i = offset; i + 1 < params.length; i += 2) {
            String key = (String)params[i];
            key = key.replaceFirst("[:=]$", ""); // "a=", 3, "b:", 4 -> "a", 3, "b", 4
            Object value = params[i + 1];
            result.put(key, value);
        }
        return result;
    }
    
    /**
     * 合并两个map
     * @param a map1
     * @param b map2
     * @return 返回合并后的map
     */
    public static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
    	Map<String, Object> result = new HashMap<String, Object>();
    	result.putAll(a);
    	result.putAll(b);
    	return result;
    }

	/**
	 * 将参数数组转换成字典
	 * @param params 参数列表
	 * @return 返回key-value集合
	 */
    public static Map<String, Object> buildMap(Object... params) {
	    return buildMap(params, 0);
	}

    /**
	 * 获取NLog实例
	 * @param context 设备上下文
	 */
	public static NLog getInstance(Context context) {
		// 只能传递Application，对于android程序只会有一个application实例
		Context app = context.getApplicationContext();
		NLog result = instances.get(app);
		if (result == null) {
			result = new NLog(app);
			instances.put(app, result);
		}
		
		/**
		NLog result = instances.get(null);
		if (result == null) {
			result = new NLog(context);
			instances.put(null, result);
		} else {
			result.context = context;
		}
		*/
		
		/* debug start */
        System.out.println(String.format("NLog.getInstance(%s) => %s", context, result));
		/* debug end */
		return result;
	}
	
	/**
	 * 追踪器集合，以name为下标
	 */
	private Map<String, NTracker> trackers = new HashMap<String, NTracker>();
	
	/**
	 * 获取追踪器
	 * @param name 追踪器名称
	 */
	public NTracker getTracker(String name) {
		if (name == null) {
			name = "default";
		}
		NTracker result = trackers.get(name);
		if (result == null) {
			result = new NTracker(name, this);
			trackers.put(name, result);
		}
		return result;
	}
	
	/**
	 * 获取追踪器
	 * @param context 上下文
	 * @param name 追踪器名称
	 */
	public static NTracker getTracker(Context context, String name) {
		NLog instance = getInstance(context);
		return instance.getTracker(name);
	}
	
	/**
	 * 生成新的sessionId
	 */
	private void buildSessionId() {
		sessionId = Long.toString(System.currentTimeMillis(), 36) + 
				Long.toString((long)(36 * 36 * 36 * 36 * Math.random()), 36);
	}
	
    /**
	 * 构造函数
	 * @param context 追踪器名称
	 */
	private NLog(Context context) {
		this.context = context;
		this.nstorage = new NStorage(context);
		buildSessionId();
	}
	
	/**
	 * 是否只在wifi网络情况下上报数据
	 */
	public void setOnlywifi(Boolean value) {
		nstorage.setOnlywifi(value);
	}
	public static void setOnlywifi(Context context, Boolean value) {
		NLog instance = getInstance(context);
		instance.setOnlywifi(value);
	}
	
	/**
	 * 重发数据的时间间隔
	 */
	public void setSendInterval(Integer value) {
		nstorage.setSendInterval(value);
		fire("sendInterval", value);
	}
	public static void setSendInterval(Context context, Integer value) {
		NLog instance = getInstance(context);
		instance.setSendInterval(value);
	}
	
	
	/**
	 * 执行命令
	 * @param cmd 命令，"<追踪器名>.<方法名>"
	 * @param params 参数列表
	 * @return 返回get命令结果
	 */
	public Object cmd(String cmd, Object... params) {
		/* debug start */
        System.out.println(String.format("%s.command('%s', [length:%s])", this, cmd, params.length));
		/* debug end */

        // 分解 "name.method" 为 ["name", "method"]
		Matcher matcher = cmdPattern.matcher(cmd);

		if (!matcher.find()) {
			/* TODO : 记录异常 */
			return null;
		}

		String trackerName = matcher.group(1);
		String method = matcher.group(2);
		NTracker tracker = getTracker(trackerName);
		return tracker.command(method, params);
	}
	
	/**
	 * 执行命令
	 * @param context 设备上下文
	 * @param cmd 命令，"<追踪器名>.<方法名>"
	 * @param params 参数列表
	 * @return 返回get命令结果
	 */
	public static Object cmd(Context context, String cmd, Object... params) {
		NLog instance = getInstance(context);
		return instance.cmd(cmd, params);
	}
	
	/**
	 * 监听器集合
	 */
	private Map<String, ArrayList<EventListener>> listeners = new HashMap<String, ArrayList<EventListener>>();
	
	/**
	 * 绑定事件
	 * @param eventName 事件名
	 * @param callback 回调函数类
	 */
	public void on(String eventName, EventListener callback) {
		/* debug start */
        System.out.println(String.format("%s.on('%s', %s)", this, eventName, callback));
		/* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
		if (list == null) {
			list = new ArrayList<EventListener>();
			listeners.put(eventName, list);
		}
		list.add(list.size(), callback); // 向后添加
	}
	
	/**
	 * 绑定事件
	 * @param context 上下文
	 * @param eventName 事件名
	 * @param callback 回调函数类
	 */
	public static void on(Context context, String eventName, EventListener callback) {
		NLog instance = getInstance(context);
		instance.on(eventName, callback);
	} 
	
	/**
	 * 注销事件绑定
	 * @param eventName 事件名
	 * @param callback 回调函数类
	 */
	public void un(String eventName, EventListener callback) {
		/* debug start */
        System.out.println(String.format("%s.un('%s', %s)", this, eventName, callback));
		/* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
		if (list != null) {
			list.remove(callback);
		}
	}

	/**
	 * 注销事件绑定
	 * @param context 上下文
	 * @param eventName 事件名
	 * @param callback 回调函数类
	 */
	public static void un(Context context, String eventName, EventListener callback) {
		NLog instance = getInstance(context);
		instance.un(eventName, callback);
	} 

	/**
	 * 派发事件
	 * @param eventName 事件名
	 * @param params 参数列表
	 */
	public void fire(String eventName, Object... params) {
		/* debug start */
        System.out.println(String.format("%s.fire('%s', [length:%s])", this, eventName, params.length));
		/* debug end */
        fire(eventName, buildMap(params));
	}
	
	/**
	 * 派发事件
	 * @param context 上下文
	 * @param eventName 事件名
	 * @param params 参数列表
	 */
	public static void fire(Context context, String eventName, Object... params) {
		NLog instance = getInstance(context);
		instance.fire(eventName, params);
	}

	/**
	 * 派发事件
	 * @param eventName 事件名
	 * @param map 参数列表
	 */
	public void fire(String eventName, Map<String, Object> map) {
		/* debug start */
        System.out.println(String.format("%s.fire('%s', %s)", this, eventName, map));
		/* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
		if (list == null) {
			return;
		}
		for (EventListener callback : list) {
			callback.onHandler(map);
		}
	}
	
	/**
	 * 事件监听类
	 */
	public static abstract class EventListener {
		/**
		 * 处理事件
		 * @param params 参数列表
		 */
		public abstract void onHandler(Map<String, Object> map);
	}
	
	/**
	 * Activity生命周期发生改变
	 * @param context
	 */
	public static void follow(Context context) {
		/* debug start */
        System.out.println(String.format("NLog.follow(%s)", context));
		/* debug end */

        NLog instance = getInstance(context);
		instance.follow();
	}
	
	/**
	 * Activity生命周期发生改变 需要在每个Activity的onResume()和onPause()方法中调用，监听session变化
	 */
	public void follow() {
		String methodName = null;
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			String name = element.getMethodName();
			if ("".equals(name.replaceFirst("^(onCreate|onStart|onResume|onPause|onStop|onDestroy|onRestart)$", ""))) {
				methodName = element.getMethodName();
				break;
			}
		}
        
		/* debug start */
        System.out.println(String.format("%s.follow() methodName => %s", this, methodName));
		/* debug end */
		if (methodName == null) {
			return;
		}

		if ("onResume".equals(methodName)) {
			if (System.currentTimeMillis() - pauseTime > sessionTimeout * 1000) { // session超时
				createSession();
			}
		} else if ("onPause".equals(methodName)) {
			pauseTime = System.currentTimeMillis();
		}
		
		fire(context, methodName);
	}
	
	/**
	 * 最后一次暂停的时间
	 */
	private Long pauseTime = 0L;
	
	/**
	 * 获得post的数据，建值进行url编码
	 * @param map 参数集合
	 * @return 返回url参数字符串
	 */
	public static String buildPost(Map<String, Object> map) {
		StringBuilder sb = new StringBuilder();
		for (String key : map.keySet()) {
			try {
				Object value = map.get(key);
				if (value == null) {
					continue;
				}
				sb.append(String.format("&%s=%s", key, URLEncoder.encode(value.toString(), "utf-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (sb.length() > 0) sb.deleteCharAt(0);
		return sb.toString();
	}
	
	/**
	 * 存储和发送数据
	 */
	private NStorage nstorage;
	
	/**
	 * 上报数据
	 * @param trackerName 追踪器名称
	 * @param fields 公共字段
	 * @param data 上报数据
	 */
    public void report(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
		/* debug start */
        System.out.println(String.format("%s.report(%s, %s)", this, fields, data));
		/* debug end */
        
        fire("report", buildMap("name=", trackerName, "fields=", fields, "data=", data));
        nstorage.report(trackerName, fields, data);
    }
}