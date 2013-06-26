nlog
====

Native 统计框架


## API

### 模块

### NLog

### getInstance

```java
/**
 * 获取NLog实例
 * @param context 设备上下文
 */
public static NLog getInstance(Context context)
```

#### getTracker

```java
/**
 * 获取追踪器
 * @param name 追踪器名称
 */
public NTracker getTracker(String name)
```

### NTracker
#### start
#### set
```java
/**
 * 设置字段值，支持批量设置
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

#### send
```java
/**
 * 发送数据
 * @param hitType 发送类型，'appview'、'event'、'timing'、'exception'
 * @param params 参数集合 (key1, value1, key2, value2...) 会将key后面的“=”或“：”移除
 */
public void send(String hitType, Object... params)
```
