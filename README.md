nlog
====

Native 统计框架

# 概要

nlog是一套管理多套统计模块的框架，借鉴了alog的设计思路，将每个统计模块的字段统一管理起来

# API

## 模块

### NLog

#### getInstance

```java
/**
 * 获取NLog实例
 * @param context 设备上下文
 */
public static NLog getInstance(Context context)
```

#### getStartTime
```java
/**
 * 采集模块启动的时间
 */
public Long getStartTime()
```

#### buildPost
```java
/**
 * 获得post的数据，建值进行url编码
 * @param map 参数集合
 * @return 返回url参数字符串
 */
public static String buildPost(Map<String, Object> map
```

#### follow

```java
/**
 * Activity生命周期发生改变 需要在每个Activity的onResume()和onPause()方法中调用，用于监听session变化
 */
public void follow()
public static void follow(Context context)
```

#### getTracker

```java
/**
 * 获取追踪器
 * @param name 追踪器名称
 */
public NTracker getTracker(String name)
```

#### setSessionTimeout

```java
/**
 * 设置session超时时间
 * @param value 超时时间，单位：秒，默认30
 */
public void setSessionTimeout(Integer value)
```

### setOnlywifi

```java
/**
 * 是否只在wifi环境下发送
 * @param value 是否压缩
 */
public void setOnlywifi(Boolean value)
```

### setSendInterval

```java
/**
 * 发送周期，单位秒
 * @param value 是否压缩
 */
public void setSendInterval(Integer value)
```

### NTracker

#### set
```java
/**
 * 设置字段值
 * @param params 参数集合 (key1, value1, key2, value2...) 会将key后面的“=”或“：”移除
 */
public void set(Object... params) {
```

#### get
```java
/**
 * 获取字段值
 * @param key 键值名
 * @return 返回键值对应的数据
 */
public Object get(String key)
```
#### start

```java
/**
 * 开始采集
 * @param params 起始参数，参考 set
 */
public void start(Object... params)
```

#### stop

```java
/**
 * 停止采集
 */
public void stop()
```

#### send
```java
/**
 * 发送数据
 * @param hitType 发送类型，'appview'、'event'、'timing'、'exception'
 * @param params 参数集合
 */
public void send(String hitType, Object... params)
```

## 配置字段

### postUrl

上报数据的路径

### protocolParameter

协议字段，用来做简写和标记不用上报的字段

```javascript
// 默认值
[
    postUrl: null,
    protocolParameter: null
]
```
