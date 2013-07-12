package com.baidu.nlog;

import java.util.*;

import android.util.Log;

public final class NTracker {
    /**
     * nlog
     * @description Native统计框架，追踪器实现
     * @author 王集鹄(WangJihu,http://weibo.com/zswang),彭正山(PengZhengshan)
     * @see https://github.com/uxrp/nlog/wiki/design
     * @version 1.0
     * @copyright www.baidu.com
     */
    /**
     *  日志TAG
     */
    private static String LOGTAG = "NTracker";

    /**
     * 追踪器集合，以name为下标
     */
    private Map<String, Object> fields = new HashMap<String, Object>();
    
    /**
     * 命令参数类
     */
    private class Args {
        /**
         * 方法名
         */
        public String method;
        /**
         * 参数
         */
        public Object[] params;
        public Args(String method, Object[] params) {
            this.method = method;
            this.params = params;
        }
    }
    
    /**
     * 命令参数缓存，当追踪器没有启动的时候
     */
    private ArrayList<Args> argsList = new ArrayList<Args>();
    
    /**
     * 是否在运行中 
     */
    private Boolean running = false;
    public Boolean getRunning() {
        return running;
    }
    public void setRunning(Boolean value) {
        if (value) {
            start();
        } else {
            stop();
        }
    }
    
    /**
     * 开始采集
     * @param params 起始参数
     */
    public void start(Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.start([length:%s])", this, params.length));
        /* debug end */
        
        start(NLog.buildMap(params));
    }
    
    /**
     * 开始采集
     * @param map 起始参数，key-value形式
     */
    public void start(Map<String, Object> map) {
        if (running) {
            return;
        }
        running = true;
        
        /* debug start */
        Log.d(LOGTAG, String.format("%s.start(%s)", this, map));
        /* debug end */

        set(map);
        // 清理之前的参数
        for (Args args : argsList) {
            command(args.method, args.params);
        }
        argsList.clear();
        fire("start");
    }

    /**
     * 停止采集
     */
    public void stop() {
        if (!running) return;
        running = true;
        
        /* debug start */
        Log.d(LOGTAG, String.format("%s.stop()", this));
        /* debug end */
        
        fire("stop");
    }
    
    /**
     * 追踪器名称
     */
    private String name;
    public String getName() {
        return name;
    }
    
    /**
     * 固定的配置字段
     */
    private static Map<String, Object> configFields = NLog.buildMap(
        "postUrl=", null, // 上报路径
        "protocolParameter=", null // 字段缩写字典
    );

    /**
     * 设置字段值
     * @param map 参数集合，key-value形式
     */
    @SuppressWarnings("unchecked")
    public void set(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.set(%s)", this, map));
        /* debug end */
        
        Iterator<String> iterator = map.keySet().iterator();    
        while (iterator.hasNext()) {    
            String key = iterator.next();
            
            Object value = map.get(key);
            if ("protocolParameter".equals(key)) {
                if (!(value instanceof Map)) continue;
                value = NLog.mergeMap(
                    configFields, 
                    (Map<String, Object>)value
                );
            }
            fields.put(key, value);
        }
    }

    /**
     * 设置字段值
     * @param params 参数集合
     */
    public void set(Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.set(%s)", this, params));
        /* debug end */
        set(NLog.buildMap(params));
    }
    
    /**
     * 获取字段值
     * @param key 键值名
     * @return 返回键值对应的数据
     */
    public Object get(String key) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.get('%s') => %s", this, key, fields.get(key)));
        /* debug end */
        
        return fields.get(key);
    }
    
    /**
     * 构造函数
     * @param name 追踪器名称
     * @param nlog NLog对象
     */
    public NTracker(String name) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s::NTracker('%s')", this, name));
        /* debug end */

        this.name = name;
        fields.put("protocolParameter", configFields);
    }
    
    /**
     * 发送数据
     * @param hitType 发送类型，appview、event、timing、exception
     * @param map 参数集合
     */
    public void send(String hitType, Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.send('%s', %s)", this, hitType, map));
        /* debug end */
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = NLog.mergeMap(
            NLog.buildMap(
                "sid=", NLog.getSessionId(), // 会话id
                "seq=", NLog.getSessionSeq(), // 会话顺序
                "time=", System.currentTimeMillis(), // 事件发生的时间
                "ts=", Long.toString(NLog.timestamp(), 36), // 36进制的时间戳
                "ht=", hitType
            ), map);
        fire("send", data);
        NLog.report(name, fields, data);
    }

    /**
     * 发送数据
     * @param hitType 发送类型，appview、event、timing、exception
     * @param map 参数集合
     */
    public void send(String hitType, Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.send('%s', %s)", this, hitType, params));
        /* debug end */
        
        send(hitType, NLog.buildMap(params));
    }
    
    /**
     * 发送appview
     * @param map 参数集合
     */
    public void sendView(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendView(%s)", this, map));
        /* debug end */
        
        send("appview", map);
    }
    
    /**
     * 发送appview
     * @param appScreen 屏幕场景名称
     */
    public void sendView(String appScreen) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendView('%s')", this, appScreen));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appScreen", appScreen);
        send("appview", map);
    }
        
    /**
     * 发送事件
     * @param map 参数集合
     */
    public void sendEvent(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendEvent('%s')", this, map));
        /* debug end */
        
        send("event", map);
    }
    
    /**
     * 发送事件
     * @param category 事件分类，如：button
     * @param action 动作，如：click
     * @param label 标签，e.g：save
     * @param value 执行次数
     */
    public void sendEvent(String category, String action, String label, Long value) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendEvent('%s', '%s', '%s', %s)", this, category, action, label, value));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("eventCategory", category);
        map.put("eventAction", action);
        map.put("eventLabel", label);
        map.put("eventValue", value);
        send("event", map);
    }

    /**
     * 发送异常
     * @param map 参数集合
     */
    public void sendException(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendException('%s')", this, map));
        /* debug end */
        
        send("exception", map);
    }
    
    /**
     * 发送异常
     * @param description 异常描述
     * @param fatal 是否导致崩溃
     */
    public void sendException(String description, Boolean fatal) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendException('%s', %s)", this, description, fatal));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("exDescription", description);
        map.put("exFatal", fatal);
        
        send("exception", map);
    }
    
    
    /**
     * 发送异常
     * @param threadName 线程名
     * @param description 异常描述
     * @param fatal 是否导致崩溃
     */
    public void sendException(String threadName, String description, Boolean fatal) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendException('%s', '%s', %s)", this, threadName, description, fatal));
        /* debug end */
        
        send("exception", NLog.buildMap(
            "exThread=", threadName,
            "exDescription=", description,
             "exFatal=", fatal
        ));
    }

    /**
     * 发送时间统计
     * @param map 参数集合
     */
    public void sendTiming(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendTiming('%s')", this, map));
        /* debug end */
        
        send("timing", map);
    }
    
    /**
     * 发送时间统计
     * @param category 类别
     * @param intervalInMilliseconds 耗时
     * @param name 名称
     * @param label 标签
     */
    public void sendTiming(String category, String var, Long value, String label) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendTiming('%s')", this, category, var, value, label));
        /* debug end */
        
        send("timing", NLog.buildMap(
            "timingCategory=", category,
            "timingVar=", var,
            "timingValue=", value,
            "timingLabel=", label
        ));
    }
    
    /**
     * 绑定事件
     * @param eventName 事件名
     * @param callback 回调函数类
     */
    public void on(String eventName, NLog.EventListener callback) {
        NLog.on(name + "." + eventName, callback);
    }
    
    /**
     * 注销事件绑定
     * @param eventName 事件名
     * @param callback 回调函数类
     */
    public void un(String eventName, NLog.EventListener callback) {
        NLog.un(name + "." + eventName, callback);
    }
    
    /**
     * 派发事件
     * @param eventName 事件名
     * @param params 参数列表
     */
    public void fire(String eventName, Object... params) {
        NLog.fire(name + "." + eventName, params);
    }
    
    /**
     * 派发事件
     * @param eventName 事件名
     * @param map 参数列表
     */
    public void fire(String eventName, Map<String, Object> map) {
        NLog.fire(name + "." + eventName, map);
    }
    
    /**
     * 执行命令
     * @param method 方法名 set、get、send、start、stop
     * @param params
     * @return 返回命令执行的结果，主要用于get方法
     */
    public Object command(String method, Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.command('%s', [length:%s])", this, method, params.length));
        /* debug end */
        
        if (!running && "".equals(method.replaceAll("^(fire|send)$", ""))) {
            argsList.add(new Args(method, params));
            return null;
        }
        
        if (method.equals("set")) {
            set(NLog.buildMap(params));
        } else if (method.equals("get")) {
            return get((String)params[0]);
        } else if (method.equals("send")) {
            if (params.length >= 1) { // send方法必须存在hitType
                String hitType = (String)params[0];
                send(hitType, NLog.buildMapOffset(params, 1));
            }
        } else if (method.equals("start")) {
            start(NLog.buildMap(params));
        } else if (method.equals("stop")) {
            stop();
        } else if (method.equals("on") || method.equals("un")) {
            if (params.length >= 2 && params[1] instanceof NLog.EventListener) {
                String eventName = (String)params[0]; 
                NLog.EventListener callback = (NLog.EventListener)params[1]; 
                if (method.equals("on")) {
                    on(eventName, callback);
                } else {
                    un(eventName, callback);
                }
            }
        } else if (method.equals("fire")) {
            if (params.length >= 1) {
                String eventName = (String)params[0];
                fire(eventName, NLog.buildMapOffset(params, 1));
            }
        }
        
        return null;
    }
}