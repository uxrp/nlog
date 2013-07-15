NLog(iOS)统计框架使用指南
====

## 概要

NLog是一套管理多个统计模块的Native实现框架，借鉴了[alog](https://github.com/uxrp/alog)的设计思路，将每个统计模块的字段统一管理起来。

## 集成指南

### 下载SDK

进入[https://github.com/uxrp/nlog/tree/master/ios/dist](https://github.com/uxrp/nlog/tree/master/ios/dist)，下载`libNLog.a`和`NLog.h`两个文件到本地。

### 加入XCode工程

* 导入SDK
    * __导入插件__
    请在你的工程目录结构中，右键选择`Add->Existing Files…`，选择这两个文件。或者将这两个文件拖入XCode工程目录结构中，在弹出的界面中勾选`Copy items into destination group's folder(if needed)`, 并确保`Add To Targets`勾选相应的target。
    * __添加依赖__
    `TARGETS-->Build Phases-->Link Binary With Libraries--> + -->libz.dylib`
* 嵌入代码
    打开`*AppDelegate.m`(*代表你的工程名字），在
    
    ```Objective-C
    - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
    ```
    方法内添加`[NLog startWithAppId:@"xxxxx"];`，其中`xxxxx`是产品ID，具体值需提前约定。

至此，就完成了NLog SDK嵌入工程的工作。

### 发送日志

首先，我们可以通过`set`方法来设置一些公共的字段，例如用户ID、渠道信息、应用版本等：

```Objective-C
[NLog set:@"uid" val: @"ad31ds134"];
[NLog set:@"channel" val: @"app_fromxxx"];
[NLog set:@"appversion" val: @"1.2.1"];
```

之后，在需要记录日志信息的位置通过`sendXX`系列方法来记录日志：

```Objective-C
NSDictionary* data = [NSDictionary dictionaryWithObjectsAndKeys:
    @"dataval",@"datakey"
    ,nil];
[NLog send:@"logCategory" params:data];

[NLog sendEvent:@"ui"
         action:@"click" 
          label:@"button.trigger" 
          value:[NSNumber numberWithInt:1]];
```
调用这些方法时并不会立即发送网络请求，而是先存储到本地，然后根据具体的发送策略来发送日志。

如果需要立即发送当前记录的日志则可以调用如下方法：

```Objective-C
[NLog send:@"immediatelytype"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"dataobject",@"datakey"
                , nil]
   immediately:YES];
```

即在`send`方法中再传入`immediately:YES`参数，调用后框架会立即尝试发送，如遇日志发送失败则会在下次发送时机再次尝试。关于发送策略可以参考__发送策略说明__。

## 日志API说明

### send

```Objective-C
/**
 * 记录日志
 * hitType 日志类型(event/appview/timing/exception)
 * params 日志数据
 * immediately 是否立即发送到服务器
 */
+ (void)send: (NSString*)hitType params: (NSDictionary *) params immediately: (BOOL) now;
```
该方法是通用的日志发送方法，发送时提供日志的类型和具体的数据即可。
其中日志类型默认的分类包括

* event: 用户交互事件相关日志，例如按钮点击信息；
* appview: UI视图相关相关日志，例如视图切换信息；
* timing: 时间相关日志，例如性能相关的时间信息；
* exception: 异常日志信息；
* 自定义：你也可以加入自定义的类型；

以上类型信息会以ht为字段发送到服务器端，因此如果需要日志类型起作用的话需要日志处理系统能够解析该字段。
如果不需要立即发送日志数据，则其中的`immediately`字段可以省略，否则传入`YES`。

### sendView

```Objective-C
/**
 * 记录视图类日志
 * viewName 视图名称
 * params   数据
 */
+ (void)sendView: (NSString *)viewName params: (NSDictionary *)params;
```
后续这几个`sendX`方法都是基于`send`方法进行的封装。
该方法用于发送视图相关的日志，例如：

```Objective-C
[NLog sendView:@"home"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"out",@"action",
                duration,@"duration"
                nil]];
```

### sendEvent

```Objective-C
/**
 * 记录事件类日志
 * category 事件类别
 * action   事件动作
 * label    事件标签
 * value    事件关联数据
 */
+ (void)sendEvent: (NSString *)category
           action: (NSString *)action
            label: (NSString *)label
            value: (NSNumber *)value;
```
通过`sendEvent`方法可以发送用户交互事件信息，例如：

```Objective-C
[NLog sendEvent:@"ui"
         action:@"click"
          label:@"button"
          value:[NSNumber numberWithInt:1]];
```
### sendTiming

```Objective-C
/**
 * 记录时间类日志
 * category 事件类别
 * interval 时长(单位：秒)
 * name     事件名称
 * label    事件标签
 */
+ (void)sendTiming: (NSString *)category
          interval: (NSTimeInterval *)interval
              name: (NSString *)name
             label: (NSString *)label;
```
发送时间类日志，例如

```Objective-C
[NLog sendTiming:@"performance"
        interval:5
            name:@"home-done"
           label:nil];
```
### sendException

```Objective-C
/**
 * 记录时间类日志
 * category 事件类别
 * interval 时长(单位：秒)
 * name     事件名称
 * label    事件标签
 */
+ (void)sendException: (NSString *)description
              isFatal: (Boolean) isFatal
               params: (NSDictionary *)params;
```
### set

```Objective-C
/**
 * 设置公共数据
 */
+ (void)set: (NSString *)key val:(id) val;
```
用于设置公共字段，每个请求都会携带这些字段。

## 发送策略说明

目前日志数据发送的几个时间点：

 * APP启动时；
 * APP切换到后台时；
 * 周期性发送：按一定周期（可配置）循环发送；
 * 调用`send`接口，并制定`immediately`为`YES`：调用后立即尝试发送数据；

同时按照上述4种策略发送日志。

## 配置介绍

配置包括本地配置和云端配置，云端配置优先。

__本地配置__

本地配置在SDK的`NLog.h`文件中，以宏定义的方式存在，如需修改按照其中的说明自行调整即可；

__云端配置__

NLog支持云端配置，配置格式为JSON形式，具体定义如下：

```javascript
{
    /**
     * 策略文件自身描述
     * ================================
     */
    /**
     * 策略ID
     */
    "id": "1001",
    /**
     * 应用ID
     */
    "apid": "android.wenku",
    /**
     * 版本号
     */
    "apver": "2.7.5",
    /**
     * 挂起多长时间算会话超时，单位：秒，范围：30-120
     */
    "sessionTimeout": 30,
    /**
     * 存储相关
     * ================================
     */
    /**
     * 数据过期时间，单位：天，范围：2-30
     */
    "storageExpires": 10, // 10天
    /**
     * 发送文件最大长度，单位：KB，范围：2-500
     */
    "sendMaxLength": 200, // 200K
    /**
     * 离线数据重试发送周期，单位：秒，范围：30-600
     */
    "sendInterval": 120, // 120秒
    /**
     * 在wifi环境下的发送周期，单位：秒，范围：30-600
     */
    "sendIntervalWifi": 60, // 60秒
    /**
     * 在wifi环境下的发送周期，单位：秒，范围：30-600
     */
    "onlywifi": false, // 只在wifi环境下上报
    /**
     * 抽样率
     */
    "sampleRate": {
        /**
         * 默认抽样率，1为全样本，0为不抽取，范围：0-1
         */
        "default": 1,
        /**
         * 追踪器名称 tracker name
         */
        "wenku": 0.5, // 50%
        "speed": 1 // 全样本
    }
}
```

## 多统计模块

在NLog中是有Tracker的概念的，一个Tracker就是一个独立的日志收集器，不仅如此，日志在本地的存储以及发送都是独立的，而且可以单独的停止某个tracker。此外，每个tracker可以设置不同的服务器接收地址。

上述文档中的方法实际上是初始化了一个默认的tracker，并使用该tracker来收集和发送数据。这在大多数情况下已经足够了，但是如果你确实有多个tracker的需求的话可以通过如下方式实现：

```Objective-C
// 获取新tracker
id<NTracker> tracker = [NLog getTracker:@"ue-data"];

// 为tracker设置日志收取地址（如使用默认的可不设置）
[tracker set:@"receiverUrl" val:@"http://xxx"];

// 设置其他公共字段
[tracker set:@"common-ue-field" val:@"common-ue-data"];

// 记录日志
[tracker send:@"appview" params:nil];

// 停止日志收集
[tracker stop];

// 开始收集日志
[tracker start];

```
## 日志发送格式

日志的发送分为GET和POST方法，其中公共字段会以GET方式（即URL参数形式）发送，而通过`send`类方法设置的字段会以POST方式发送。举个例子：

```Objective-C
[NLog set:@"uid" val: @"ad31ds134"];
[NLog set:@"channel" val: @"app_fromxxx"];
[NLog set:@"appversion" val: @"1.2.1"];
```

假如有上述代码，而且服务器地址为`http://www.x.com/log.php`，则发送日志时会向以下地址POST记录的日志数据：
`http://www.x.com/log.php?uid=ad31ds134&channel=app_fromxxx&&appversion=1.2.1`


