package com.baidu.nlog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public final class NLog {
    /**
     * nlog
     * @description Native统计框架
     * @author 王集鹄(WangJihu,http://weibo.com/zswang),彭正山(PengZhengshan)
     * @version 1.0
     * @copyright www.baidu.com
     */
    // 日志TAG
    private static String LOGTAG = (new Object() {
        public String getClassName() {
            String clazzName = this.getClass().getName();
            return clazzName.substring(0, clazzName.lastIndexOf('$'));
        }
    }).getClassName();
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
        /* debug start */
        Log.d(LOGTAG, String.format("get('%s') => %s", key, fields.get(key)));
        /* debug end */
        
        return fields.get(key);
    }
    /**
     * 获取整数字段值
     * @param key 键值名
     * @return 返回键值对应的数据
     */
    public static Integer getInteger(String key) {
        /* debug start */
        Log.d(LOGTAG, String.format("get('%s') => %s", key, fields.get(key)));
        /* debug end */
        Object configField = configFields.get(key);
        if (configField == null) {
            return null;
        } 
        return safeInteger(fields.get(key), ((ConfigField)configField).defaultValue);
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
    | sendInterval    | 重发数据周期          |秒     | 300   |30-600 |
    | sendIntervalWifi| 在wifi环境下的重发周期 |秒     | 150   |30-600 |
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
            "sendInterval", new ConfigField(300, 30, 600),
            "sendIntervalWifi", new ConfigField(150, 30, 600),
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
    @SuppressWarnings("unchecked")
    public static void init(Context context, Object... params) {
        if (initCompleted) {
            Log.w(LOGTAG, "init() Can't repeat initialization.");
            return;
        }

        if (context == null) {
            Log.w(LOGTAG, "init() Context can't for empty.");
            return;
        }
        pauseTime = System.currentTimeMillis();
        initCompleted = true;
        Context app = context.getApplicationContext();

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
                
        NStorage.init();
        createSession();
        
        // 处理未初始化前的命令
        for (CmdParamItem item : cmdParamList) {
            item.tracker.command(item.method, item.params);
        }
        cmdParamList.clear();
        
        /* debug start */
        Log.i(LOGTAG, String.format("NLog.init(%s, %s) fields => %s", context, buildMap(params), fields));
        /* debug end */
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
        sessionSeq++;
        buildSessionId();
        startTime = System.currentTimeMillis();
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
    private static String sessionId;
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
    
    /**
     * 生成新的sessionId
     */
    private static void buildSessionId() {
        sessionId = Long.toString(System.currentTimeMillis(), 36) + 
                Long.toString((long)(36 * 36 * 36 * 36 * Math.random()), 36);
    }
        
    private static class CmdParamItem {
        public NTracker tracker;
        public String method;
        public Object[] params;
        CmdParamItem(NTracker tracker, String method, Object[] params) {
            this.tracker = tracker;
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
        /* debug start */
        Log.d(LOGTAG, String.format("command('%s', [length:%s])", cmd, params.length));
        /* debug end */

        // 分解 "name.method" 为 ["name", "method"]
        Matcher matcher = cmdPattern.matcher(cmd);

        if (!matcher.find()) {
            /* debug start */
            Log.w(LOGTAG, String.format("'%s' Command format error.", cmd));
            /* debug end */
            return null;
        }

        String trackerName = matcher.group(1);
        String method = matcher.group(2);
        NTracker tracker = getTracker(trackerName);
        if (initCompleted) {
            return tracker.command(method, params); 
        } else {
            cmdParamList.add(new CmdParamItem(tracker, method, params));
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
        /* debug start */
        Log.d(LOGTAG, String.format("on('%s', %s)", eventName, callback));
        /* debug end */

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
        /* debug start */
        Log.d(LOGTAG, String.format("un('%s', %s)", eventName, callback));
        /* debug end */

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
        /* debug start */
        Log.d(LOGTAG, String.format("fire('%s', [length:%s])", eventName, params.length));
        /* debug end */
        fire(eventName, buildMap(params));
    }

    /**
     * 派发事件
     * @param eventName 事件名
     * @param map 参数列表
     */
    public static void fire(String eventName, Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("fire('%s', %s)", eventName, map));
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
     * 用户浏览的顺序
     */
    private static ArrayList<Context> followPath = new ArrayList<Context>();
    
    /**
     * Activity生命周期发生改变 需要在每个Activity的onResume()和onPause()方法中调用，监听session变化
     */
    public static void follow(Context context) {
        String methodName = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String name = element.getMethodName();
            if ("".equals(name.replaceFirst("^(onCreate|onStart|onResume|onPause|onStop|onDestroy|onRestart)$", ""))) {
                methodName = element.getMethodName();
                break;
            }
        }
        
        /* debug start */
        Log.d(LOGTAG, String.format("follow(%s) methodName => %s", context, methodName));
        /* debug end */
        if (methodName == null) {
            Log.w(LOGTAG, String.format("follow() Not in the right place."));
            return;
        }
       
        if ("onResume".equals(methodName)) { // 重新激活
            
            if (System.currentTimeMillis() - pauseTime > (Integer)fields.get("sessionTimeout") * 1000) { // session超时
                pauseTime = System.currentTimeMillis();
                createSession();
            }
            
            if (followPath.contains(context)) {
                Log.w(LOGTAG, String.format("follow('%s') Does not match the context onPause and onResume. context=%s", methodName, context));
            } else {
                followPath.add(context);
            }
            
        } else if ("onPause".equals(methodName)) { 
            
            pauseTime = System.currentTimeMillis();
            if (followPath.contains(context)) {
                followPath.remove(context);
            } else {
                Log.w(LOGTAG, String.format("follow('%s') Does not match the context onPause and onResume. context=%s", methodName, context));
            }
            
        }
        
        fire("follow", buildMap(
                "method", methodName,
                "path=", followPath
        ));
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
        /* debug start */
        Log.d(LOGTAG, String.format("report(%s, %s)", fields, data));
        /* debug end */
        if (!initCompleted) {
            return;
        }
        if (!isSampled(trackerName)) {
            /* debug start */
            Log.i(LOGTAG, String.format("Tracker '%s' Not sample.", trackerName));
            /* debug end */
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
     * 抽样率
     */
    public static Map<String, Double> sampleRate = new HashMap<String, Double>();
    /**
     * 更新规则
     * @param jsonText
     */
    public static void updateRule(String jsonText) {
        /* debug start */
        Log.d(LOGTAG, String.format("updateRule(%s)", jsonText));
        /* debug end */
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
}