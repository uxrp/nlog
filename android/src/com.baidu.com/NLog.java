package com.baidu.nlog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

public final class NLog {
    /**
     * nlog
     * @description Native统计框架
     * @author 王集鹄(WangJihu,http://weibo.com/zswang),彭正山(PengZhengshan)
     * @see https://github.com/uxrp/nlog/wiki/design
     * @version 1.0
     * @copyright www.baidu.com
     */
    // 日志TAG
    private final static String LOG_TAG = "NLog";
    /**
     * 当前应用包名
     */
	private static String currPackageName = "";
	/**
	 * activity全部挂起之后，最近一个包名
	 */
	private static String pauseAfterPackageName = "";
    /**
     * 追踪器集合，以name为下标
     */
    private static Map<String, Object> fields;
    
    /**
     * 获取字段值
     * @param key 键值名
     * @return 返回键值对应的数据
     */
    public static Object get(String key) {
        
        
        return fields.get(key);
    }
    /**
     * 获取整数字段值
     * @param key 键值名
     * @return 返回键值对应的数据
     */
    public static Integer getInteger(String key) {
        
        Object configField = configFields.get(key);
        if (configField == null) {
            return null;
        } 
        return safeInteger(fields.get(key), ((ConfigField)configField).defaultValue);
    }
    /**
     * 获取逻辑值
     * @param key 键值名
     * @return 返回键值对应的数据
     */
    public static Boolean getBoolean(String key) {
        return safeBoolean(fields.get(key), false);
    }
    /**
     * 获取字符串
     * @param key 键值名
     * @return 返回键值对应的数据
     */
    public static String getString(String key, String defaultValue) {
        return safeString(fields.get(key), defaultValue);
    }
    public static String getString(String key) {
        return safeString(fields.get(key), "");
    }
    /**
     * 是否已经初始化
     */
    private static Boolean initCompleted = false;
    public static Boolean getInitCompleted() {
        return initCompleted;
    }
    
    /**
     * 安全获取整数数值
     * @param value 可以是字符串、浮点
     * @param defaultValue 默认值
     * @return
     */
    @SuppressLint("UseValueOf")
    public static Integer safeInteger(Object value, Integer defaultValue) {
        Integer result = defaultValue;
        if (value != null) {
            if (value instanceof Integer) {
                result = (Integer)value;
            } else {
                try {
                    result = new Integer(value.toString());
                } catch(NumberFormatException e) {
                }
            }
        }
        return result;
    }
    /**
     * 安全获取字符串
     * @param value 可以是字符串
     * @param defaultValue 默认值
     * @return
     */
    @SuppressLint("UseValueOf")
    public static String safeString(Object value, String defaultValue) {
        String result = defaultValue;
        if (value != null) {
            if (value instanceof String) {
                result = (String)value;
            } else {
                try {
                    result = value.toString();
                } catch(NumberFormatException e) {
                }
            }
        }
        return result;
    }
    /**
     * 安全获取整数数值
     * @param value 可以是字符串、浮点
     * @param defaultValue 默认值
     * @return
     */
    @SuppressLint("UseValueOf")
    public static Double safeDouble(Object value, Double defaultValue) {
        Double result = defaultValue;
        if (value != null) {
            if (value instanceof Double) {
                result = (Double)value;
            } else {
                try {
                    result = new Double(value.toString());
                } catch(NumberFormatException e) {
                }
            }
        }
        return result;
    }
    /**
     * 安全获取逻辑值
     * @param value 可以是字符串
     * @param defaultValue 默认值
     * @return
     */
    @SuppressLint("UseValueOf")
    public static Boolean safeBoolean(Object value, Boolean defaultValue) {
        Boolean result = defaultValue;
        if (value != null) {
            if (value instanceof Boolean) {
                result = (Boolean)value;
            } else {
                result = new Boolean(value.toString());
            }
        }
        return result;
    }
    /**
     * 配置字段
     */
    private static class ConfigField {
        Integer defaultValue;
        Integer minValue;
        Integer maxValue;
        ConfigField(Integer defaultValue, Integer minValue, Integer maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.defaultValue = defaultValue;
        }
    }
   /*
    | 参数             | 说明                | 单位  | 默认值 |取值范围|
    | --------------- | -------------------| ------|------:|-------|
     | ruleUrl         | 云端策略存放的路径     |       |null   |       |
     | ruleExpires     | 策略文件过期时间       |天     | 2     |2-30   |
    | onlywifi        | 只在wifi环境下发送    |bool   | false |       |
    | sendMaxLength   | 单次发送最大的包长度   |KB     | 200   |2-500 |
    | sendInterval    | 重发数据周期          |秒     | 300   |1-600 |
    | sendIntervalWifi| 在wifi环境下的重发周期 |秒     | 150   |1-600 |
    | sessionTimeout  | 会话超时时间          |秒     | 30    |30-120 |
    | storageExpires  | 离线数据过期时间       |天     | 10    |2-30  |
    | sampleRate      | 各个Tracker的抽样率   |浮点数  |[1...] |0-1    |
    */
    private static Map<String, Object> configFields;
    /**
     * 内部初始化
     */
    static {
        configFields = buildMap(
            "ruleExpires=", new ConfigField(5, 2, 30),
            "sendMaxLength", new ConfigField(2, 500, 200),
            "sendInterval", new ConfigField(300, 1, 600),
            "sendIntervalWifi", new ConfigField(150, 1, 600),
            "sessionTimeout", new ConfigField(30, 30, 120),
            "storageExpires", new ConfigField(10, 2, 30)
        );
    }

    /**
     * 初始化
     * @param context 上下文
     * @param params 初始化参数
     */
    @SuppressLint({ "UseValueOf", "DefaultLocale" })
    @SuppressWarnings({ "unchecked", "deprecation" })
    public static void init(Context context, Object... params) {
        if (initCompleted) {
            Log.w(LOG_TAG, "init() Can't repeat initialization.");
            return;
        }

        if (context == null) {
            Log.w(LOG_TAG, "init() Context can't for empty.");
            return;
        }
        initCompleted = true;
        Context app = context.getApplicationContext();
        currPackageName = app.getPackageName();

        fields = mergeMap(buildMap(
                "ruleUrl=", null,
                "ruleExpires=", 2
        ), buildMap(params));
        fields.put("applicationContext", app);
        
        // 配置事件 onXdddd -> xdddd
        for (String key : fields.keySet()) {
            Object listener = fields.get(key);
            if (!(listener instanceof EventListener)) {
                continue;
            }
            Matcher matcher = eventPattern.matcher(key);
            if (matcher.find()) {
                on(key.substring(2, 3).toLowerCase() + key.substring(3), (EventListener)listener);    
            }
            
        }
        
        // 将数值调整到合理范围
        for (String key : configFields.keySet()) {
            ConfigField configField = (ConfigField)configFields.get(key); 
            fields.put(key, Math.min(
                    Math.max(safeInteger(fields.get(key), configField.defaultValue), configField.minValue),
                    configField.maxValue
            ));
        }
        
        // 设置抽样率
        Object items = fields.get("sampleRate");
        if (items != null && items instanceof Map) {
            Map<String, ?> map = (Map<String, ?>)items;
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                sampleRate.put(key.toString(),
                        Math.max(Math.min(safeDouble(value, 1.0), 1), 0));
            }
        }

        // 通用字段
        fields.put("systemVersion", Build.VERSION.RELEASE);
        fields.put("model", Build.MODEL);
        // 网络运营商
        try {
        	TelephonyManager telManager = (TelephonyManager)app.getSystemService(Context.TELEPHONY_SERVICE);
        	String operator = telManager.getNetworkOperator();
        	fields.put("networkOperator", operator == null || "".equals(operator) ? "0" : operator); // 获取为空则返回0
		} catch (Exception e) {
        	fields.put("networkOperator", "0");
			e.printStackTrace();
        }
        
        // 应用程序版本
    	PackageInfo packageInfo = null;
		try {
			PackageManager pm = app.getPackageManager();
			packageInfo = pm.getPackageInfo(app.getPackageName(), 0);
			fields.put("applicationVersion", packageInfo.versionName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 分辨率
    	try {
	    	WindowManager wm = (WindowManager)app.getSystemService(Context.WINDOW_SERVICE);
	    	if (wm != null) {
				fields.put("screenResolution", wm.getDefaultDisplay().getWidth() + "*" + wm.getDefaultDisplay().getHeight());
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        
        NStorage.init();
        
        // 处理未初始化前的命令
        for (CmdParamItem item : cmdParamList) {
        	NTracker tracker = getTracker(item.trackerName);
            tracker.command(item.method, item.params);
        }
        cmdParamList.clear();
        
        
    }
    
    /**
     * 获取设备上下文
     */
    public static Context getContext() {
        return (Context)fields.get("applicationContext");
    }
    
    /**
     * 采集模块启动的时间
     */
    private static Long startTime = System.currentTimeMillis();
    public static Long getStartTime() {
        return startTime;
    }
    
    /**
     * 获取时间戳 
     * @param now 当前时间
     * @return 返回差值
     */
    public static Long timestamp(Long now) {
        return System.currentTimeMillis() - now;
    }
    
    /**
     * 启动新会话
     */
    private static void createSession() {
    	if (sessionId != null) {
            fire("destorySession", buildMap(
            		"sessionId=", sessionId,
            		"duration=", timestamp(pauseTime), // 流量耗时
            		"time=", pauseTime 
    		));
    	}
    	Long now = System.currentTimeMillis();
        pauseTime = now;
        sessionSeq++;
        sessionId = Long.toString(now, 36) + Long.toString((long)(36 * 36 * 36 * 36 * Math.random()), 36);
        startTime = now;
        fire("createSession", "sessionId=", sessionId);
    }
    
    /**
     * 获取时间戳 
     * @return 返回差值
     */
    public static Long timestamp() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 会话id, 当前时间毫秒36进制+随机数
     */
    private static String sessionId = null;
    public static String getSessionId() {
        return sessionId;
    }
    
    /**
     * 当前第几次会话
     */
    private static Integer sessionSeq = 0;
    public static Integer getSessionSeq() {
        return sessionSeq;
    }
    
    /**
     * 固定随机数，用于计算采样率
     */
    private static Double randomSeed = Math.random();

    /**
     * 命令字符串解析，如："wenku.set" -> ["wenku", "set"] "set" -> [null, "set"]
     */
    private static Pattern cmdPattern = Pattern.compile("^(?:([\\w$_]+)\\.)?(\\w+)$");

    /**
     * 事件字符串解析，如："onCreate" -> ["Create"]
     */
    private static Pattern eventPattern = Pattern.compile("^on([A-Z]\\w*)$");
    /**
     * 将参数数组转换成字典，为了简化调用方式
     * @param params 参数列表
     * @param offset 起始位置
     * @return 返回key-value集合
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> buildMapOffset(Object[] params, Integer offset) {
        Map<String, Object> result = new HashMap<String, Object>();
        if (params.length - 1 == offset && offset >= 0) {
            if (params[offset] instanceof Map) {
                result.putAll((Map<String, Object>)params[offset]);
            }
            return result;
        }
        for (Integer i = offset; i + 1 < params.length; i += 2) {
            String key = (String)params[i];
            key = key.replaceFirst("[:=]$", ""); // "a=", 3, "b:", 4 -> "a", 3, "b", 4
            Object value = params[i + 1];
            result.put(key, value);
        }
        return result;
    }
    
    /**
     * 将参数数组转换成字典，为了简化调用方式
     * @param params 参数列表
     * @return 返回key-value集合
     */
    public static Map<String, Object> buildMap(Object... params) {
        return buildMapOffset(params, 0);
    }
    
    /**
     * 合并多个map
     * @param maps 多个Map
     * @return 返回合并后的map
     */
    public static Map<String, Object> mergeMap(Map<String, Object>... maps) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map<String, Object> map : maps) {
            result.putAll(map);
        }
        return result;
    }
    private static Map<String, NTracker> trackers = new HashMap<String, NTracker>();
    /**
     * 获取追踪器
     * @param name 追踪器名称
     */
    private static NTracker getTracker(String name) {
        if (name == null) {
            name = "default";
        }
        NTracker result = trackers.get(name);
        if (result == null) {
            result = new NTracker(name);
            trackers.put(name, result);
        }
        return result;
    }
    
    private static class CmdParamItem {
        public String trackerName;
        public String method;
        public Object[] params;
        CmdParamItem(String trackerName, String method, Object[] params) {
            this.trackerName = trackerName;
            this.method = method;
            this.params = params;
        }
    }
    private static ArrayList<CmdParamItem> cmdParamList = new ArrayList<CmdParamItem>();
    /**
     * 执行命令
     * @param cmd 命令，"<追踪器名>.<方法名>"
     * @param params 参数列表
     * @return 返回get命令结果
     */
    public static Object cmd(String cmd, Object... params) {
        
        // 分解 "name.method" 为 ["name", "method"]
        Matcher matcher = cmdPattern.matcher(cmd);

        if (!matcher.find()) {
            
            return null;
        }

        String trackerName = matcher.group(1);
        String method = matcher.group(2);
        if (initCompleted) {
            NTracker tracker = getTracker(trackerName);
            return tracker.command(method, params);    
        } else {
            cmdParamList.add(new CmdParamItem(trackerName, method, params));
            return null;
        }
    }

    /**
     * 监听器集合
     */
    private static Map<String, ArrayList<EventListener>> listeners = new HashMap<String, ArrayList<EventListener>>();
    
    /**
     * 绑定事件
     * @param eventName 事件名
     * @param callback 回调函数类
     */
    public static void on(String eventName, EventListener callback) {
        

        ArrayList<EventListener> list = listeners.get(eventName);
        if (list == null) {
            list = new ArrayList<EventListener>();
            listeners.put(eventName, list);
        }
        list.add(list.size(), callback); // 向后添加
    }
    
    /**
     * 注销事件绑定
     * @param eventName 事件名
     * @param callback 回调函数类
     */
    public static void un(String eventName, EventListener callback) {
        

        ArrayList<EventListener> list = listeners.get(eventName);
        if (list != null) {
            list.remove(callback);
        }
    }

    /**
     * 派发事件
     * @param eventName 事件名
     * @param params 参数列表
     */
    public static void fire(String eventName, Object... params) {
        
        fire(eventName, buildMap(params));
    }

    /**
     * 派发事件
     * @param eventName 事件名
     * @param map 参数列表
     */
    public static void fire(String eventName, Map<String, Object> map) {
        

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
     * activity生命周期跟踪
     */
    public static class FollowInfo {
    	public Long time;
    	public Object context;
    	public String name;
    	FollowInfo(Long time, Object context, String name) {
    		this.time = time;
    		this.context = context;
    		this.name = name;
    	}
    }
    /**
     * 用户浏览的顺序
     */
    private static ArrayList<FollowInfo> followPath = new ArrayList<FollowInfo>();
    /**
     * 加速查询
     */
    private static Map<Object, FollowInfo> followMap = new HashMap<Object, FollowInfo>(); 
    
    /**
     * Activity生命周期发生改变 需要在每个Activity|Fragment的onResume()和onPause()方法中调用，监听session变化
     */
    public static void follow(Object context) {
    	follow(context, null);
    }
    /**
     * 为了实现自定义生命周期
     * @param context
     * @param name
     */
    public static void onResume(Object context, String name) {
    	follow(context, name, "onResume");
    }
    public static void onResume(Object context) {
    	follow(context, "", "onResume");
    }
    /**
     * 为了实现自定义生命周期
     * @param context
     * @param name
     */
    public static void onPause(Object context, String name) {
    	follow(context, name, "onPause");
    }
    public static void onPause(Object context) {
    	follow(context, "", "onPause");
    }
    /**
     * 计时器，用于判断焦点是否在子应用中
     */
    private static Timer timer;
    /**
     * 清除计时器
     */
    private static void cancelTimer() {
    	if (timer != null) {
    		if (!"".equals(pauseAfterPackageName)) {
    			onPause(pauseAfterPackageName);
    			pauseAfterPackageName = "";
    		}
    		timer.cancel();
    		timer = null;
    	}
    }
    /**
     * 启动计时器
     */
    private static void startTimer() {
    	cancelTimer();
    	if ("".equals(pauseAfterPackageName)) {
    		return;
    	}
    	onResume(pauseAfterPackageName);
    	timer = new Timer();
    	timer.schedule(new TimerTask() {

			@Override
			public void run() {
				PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
				if (pm.isScreenOn() && pauseAfterPackageName.equals(topPackageName())) { // 焦点还是停留子打开的子应用
					pauseTime = System.currentTimeMillis();
				} else if (System.currentTimeMillis() - pauseTime > getInteger("sessionTimeout") * 1000) { // session超时
					cancelTimer();
				}
			}
			
		}, 5000, 5000);
    }
    
    public static void follow(Object context, String name) {
        String methodName = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String t = element.getMethodName();
            if ("".equals(t.replaceFirst("^(onCreate|onStart|onResume|onPause|onStop|onDestroy|onRestart)$", ""))) {
                methodName = element.getMethodName();
                break;
            }
        }
        follow(context, name, methodName);
    }
    /**
     * Activity生命周期发生改变 需要在每个Activity|Fragment的onResume()和onPause()方法中调用，监听session变化
     * @param context
     * @param name 需要跟踪的name
     */
    public static void follow(Object context, String name, String methodName) {
        if (methodName == null) {
            Log.w(LOG_TAG, String.format("follow() Not in the right place."));
            return;
        }
        
        if (NLog.getBoolean("debug")) {
        	Log.d(LOG_TAG, String.format("follow('%s') context=%s name='%s'", methodName, context, name));
        }

        if ("onResume".equals(methodName)) { // 重新激活
            cancelTimer(); // 清理计时器
            if (System.currentTimeMillis() - pauseTime > getInteger("sessionTimeout") * 1000) { // session超时
                createSession();
            }
            FollowInfo info = followMap.get(context);
            if (followPath.contains(info)) {
                Log.w(LOG_TAG, String.format("follow('%s') Does not match the context onPause and onResume. context=%s", methodName, context));
            } else {
            	info = new FollowInfo(System.currentTimeMillis(), context, name);
            	followMap.put(context, info);
                followPath.add(info);
            }
            
        } else if ("onPause".equals(methodName)) { 
        	if (!(context instanceof String)) { // 非子应用的情况，子应用的pause时间由时间器处理
        		pauseTime = System.currentTimeMillis();
        	}
            FollowInfo info = followMap.get(context);
            if (followPath.contains(info)) {
            	
            	fire("viewClose", buildMap(
            			"target=", context,
            			"name=", info.name,
            			"duration=", System.currentTimeMillis() - info.time
    			));
            	
                followMap.remove(context);
                followPath.remove(info);
                
                if (followPath.size() <= 0 && !(context instanceof String)) { // activity全部挂起//context是String类型为childPackage的情况
                	String childPackages = NLog.getString("childPackages", "");
                	if (!"".equals(childPackages)) { // 存在子应用
                		String packageName = topPackageName();
                		if (currPackageName.equals(packageName)) { // activity全部挂起时，package没有改变。。。
                			return;
                		}
                		if (("," + childPackages + ",").indexOf("," + packageName + ",") >= 0) { // 开启了子应用
                			cancelTimer();
                			pauseAfterPackageName = packageName;
                			startTimer();
                		}
                		
                	}
                	
                }
            } else {
                Log.w(LOG_TAG, String.format("follow('%s') Does not match the context onPause and onResume. context=%s", methodName, context));
            }
            
        }

        fire("follow", buildMap(
                "method=", methodName,
                "target=", context,
                "path=", followPath,
                "name=", name
        ));
    }
    /**
     * 获取顶部包的数据
     * 需要拥有GET_TASKS权限
     * @return
     */
    public static String topPackageName() {
    	String result = "";
		try {
			ActivityManager am = (ActivityManager)getContext().getSystemService(Service.ACTIVITY_SERVICE);
			ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
			result = cn.getPackageName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
    }
    /**
     * 进程退出，终止化
     */
    public static void exit() {
    	if (!initCompleted) { // 未初始化
    		return;
    	}
    	if (sessionId != null) {
	        fire("destorySession", buildMap(
	        		"sessionId=", sessionId,
	        		"duration=", timestamp(pauseTime), // 流量耗时
	        		"time=", System.currentTimeMillis()
			));
    	}
        pauseTime = 0L;
    	initCompleted = false;
    }
    
    /**
     * 最后一次暂停的时间
     */
    private static Long pauseTime = 0L;
    
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
     * 上报数据
     * @param trackerName 追踪器名称
     * @param fields 公共字段
     * @param data 上报数据
     */
    public static void report(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
        
        if (!initCompleted) {
            return;
        }
        if (!NLog.getBoolean("debug") && !isSampled(trackerName)) { // 判断抽样，debug模式则为全样本
            
            return;
        }
        fire("report", buildMap("name=", trackerName, "fields=", fields, "data=", data));
        NStorage.report(trackerName, fields, data);
    }
    
    /**
     * 判断是否追踪器是否被抽样
     * @param trackerName 
     * @return 是否被抽中
     */
    public static Boolean isSampled(String trackerName) {
        Boolean result = true;
        Double trackerSampleRate = sampleRate.get(trackerName);
        if (trackerSampleRate != null && trackerSampleRate < randomSeed) {
            result = false;
        }
        return result;
    }
    
    /**
     * 当前应用是否处于激活状态
     * @return 返回应用是否激活
     */
    /* 暂不开放
    public static Boolean isFocus() {
		PowerManager pm = (PowerManager)getContext().getSystemService(Context.POWER_SERVICE);
		String topPackage = topPackageName();
		return (currPackageName.equals(topPackage) || pauseAfterPackageName.equals(topPackage)) && pm.isScreenOn();
    }
    */
    
    /**
     * 抽样率
     */
    public static Map<String, Double> sampleRate = new HashMap<String, Double>();
    /**
     * 更新规则
     * @param jsonText
     */
    public static void updateRule(String jsonText) {
        
        try {
            JSONObject json = new JSONObject(jsonText);

            // 将数值调整到合理范围
            for (String key : configFields.keySet()) {
                ConfigField configField = (ConfigField)configFields.get(key); 
                if (json.has(key)) {
                    // 将数值调整到合理范围
                    fields.put(key, Math.min(
                            Math.max(safeInteger(json.get(key), configField.defaultValue), configField.minValue),
                            configField.maxValue
                    ));
                }
            }

            if (json.has("sampleRate")) {
                JSONObject items = json.getJSONObject("sampleRate");
                @SuppressWarnings("unchecked")
                Iterator<String> keys = items.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    sampleRate.put(key, Math.max(Math.min(1, safeDouble(items.get(key), 1.0)), 0));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * 版本升级
     * @param currVersion 当前版本
     * @param oldVersion 升级后的版本
     */
    public static void updateVersion(String currVersion, String oldVersion) {
    	fire("upgrade", NLog.buildMap(
    			"newVersion=", currVersion,
    			"oldVersion=", oldVersion
		));
    }
    
}
