NLog(iOS)统计框架使用指南
====

* [概要](#%E6%A6%82%E8%A6%81)
* [集成指南](#%E9%9B%86%E6%88%90%E6%8C%87%E5%8D%97)
    * [下载SDK](#%E4%B8%8B%E8%BD%BDsdk)
    * [加入XCode工程](#%E5%8A%A0%E5%85%A5xcode%E5%B7%A5%E7%A8%8B)
    * [发送日志](#%E5%8F%91%E9%80%81%E6%97%A5%E5%BF%97)
* [日志API说明](#%E6%97%A5%E5%BF%97api%E8%AF%B4%E6%98%8E)
    * [send](#send)
    * [sendView](#sendview)
    * [sendEvent](#sendevent)
    * [sendTiming](#sendtiming)
    * [sendException](#sendexception)
    * [set](#set)
* [发送策略说明](#%E5%8F%91%E9%80%81%E7%AD%96%E7%95%A5%E8%AF%B4%E6%98%8E)
* [配置介绍](#%E9%85%8D%E7%BD%AE%E4%BB%8B%E7%BB%8D)
* [多统计模块](#%E5%A4%9A%E7%BB%9F%E8%AE%A1%E6%A8%A1%E5%9D%97)
* [日志发送格式](#%E6%97%A5%E5%BF%97%E5%8F%91%E9%80%81%E6%A0%BC%E5%BC%8F)
* [修改数据发送字段](#%E4%BF%AE%E6%94%B9%E6%95%B0%E6%8D%AE%E5%8F%91%E9%80%81%E5%AD%97%E6%AE%B5)
* [设置抽样率](#%E8%AE%BE%E7%BD%AE%E6%8A%BD%E6%A0%B7%E7%8E%87)

## 概要

NLog是一套管理多个统计模块的Native实现框架，借鉴了[alog](https://github.com/uxrp/alog)的设计思路，将每个统计模块的字段统一管理起来。

## 集成指南

### 下载SDK

进入[https://github.com/uxrp/nlog/tree/master/ios/dist](https://github.com/uxrp/nlog/tree/master/ios/dist)，下载`nlog.framework.zip`到本地，解压缩后得到`nlog.framework`文件夹。

__注__：nlog实现时使用了[JSONKit](https://github.com/johnezang/JSONKitl)，如果你的项目中也有使用的话下载时请选择`nlog.framework-nojsonkit.zip`。

### 加入XCode工程

* 导入SDK
    * __导入SDK__
    打开需要加入SDK的工程，将`nlog.framework`文件夹拖入XCode工程目录结构中，在弹出的界面中勾选`Copy items into destination group's folder(if needed)`, 并确保`Add To Targets`勾选相应的target。
    * __添加依赖__
    `TARGETS-->Build Phases-->Link Binary With Libraries--> + -->libz.dylib`
    `TARGETS-->Build Phases-->Link Binary With Libraries--> + -->SystemConfiguration.framework`
* 嵌入代码
    打开`*AppDelegate.m`(*代表你的工程名字），先在头部引入SDK：

    ```Objective-C
    #import <nlog/NLog.h>

    ```
    然后在

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

即在`send`方法中再传入`immediately:YES`参数，调用后框架会立即尝试发送，如遇日志发送失败则会在下次发送时机再次尝试。关于发送策略可以参考 __发送策略说明__。

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

可以在统计启动前修改默认的配置项，需要注意的是本地配置项优先级低于云端配置，但不是所有的配置项都是由云端来控制的。

通过以下方法来设置默认配置项：

```Objective-C
/**
 * 初始化统计框架
 * appid    当前APP的标识 
 * configs  默认配置
 */
+ (void)startWithAppId:(NSString *) appid configs:(NSDictionary *)configs;
```

例如：

```Objective-C
[NLog startWithAppId:@"NlogFrameworkDemoID"
                 configs:[NSDictionary dictionaryWithObjectsAndKeys:
                          @"http://hunter.duapp.com/command/?command=nlog-post&channel=miller4", @"receiverUrl"
                          , nil]];
```

除此之外其他的配置项如以下类别所示：

<table>
	<tr>
		<th>名称</th><th>含义</th><th>默认值</th><th>是否云端可控</th>
	</tr>
	<tr>
		<td>receiverUrl</td>
		<td>日志服务器地址</td>
		<td>http://kstj.baidu.com/ctj/88/</td>
		<td>否</td>
	</tr>

        <tr>
                <td>remoteRuleUrl</td>
                <td>云端策略文件地址</td>
                <td>待定</td>
                <td>否</td>
        </tr>
	<tr>
                <td>remoteRuleExpires</td>
                <td>云端策略文件过期时间</td>
                <td>1（天）</td>
                <td>否</td>
        </tr>
        <tr>
                <td>onlyWifi</td>
                <td>是否只在wifi下发送日志</td>
                <td>NO</td>
                <td>是</td>
        </tr>
        <tr>
                <td>sendInterval</td>
                <td>循环发送周期</td>
                <td>120（秒）</td>
                <td>是</td>
        </tr>
        <tr>
		<td>sendIntervalWifi</td>
                <td>Wifi下循环发送周期</td>
                <td>60（秒）</td>
                <td>是</td>
        </tr>
        <tr>
                <td>sessionTimeout</td>
                <td>session过期时间</td>
                <td>30（秒）</td>
                <td>是</td>
        </tr>
        <tr>
                <td>storageExpires</td>
                <td>本地日志缓存有效期</td>
                <td>10（天）</td>
                <td>是</td>
        </tr>
        <tr>
                <td>sendMaxLength</td>
                <td>日志最大发送大小</td>
                <td>200（KB）</td>
                <td>是</td>
        </tr>
        <tr>
                <td>sampleRate</td>
                <td>抽样率</td>
                <td>1（100%）</td>
                <td>是</td>
        </tr>
</table>

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

## 修改数据发送字段

为了减小发送到服务器的无效数据，NLog支持字段写和发送分离，即记录日志时可以使用易读的字段，发送时可以使用简单的字段，减少不必要的浪费。

使用时可以继续使用`set`接口，只不过字段名应该设为`protocolParameter`，参数为`NSDictionary`类型。

具体例子如下：

```Objective-C
[NLog set:@"protocolParameter"
          val:[NSDictionary dictionaryWithObjectsAndKeys:
               @"ok", @"originalkey",
               , nil]];

// 在未使用protocolParameter情况下，发送到服务器的数据将是
// originalkey=originalval，使用后变为ok=originalval
[NLog send:@"test protocol"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"originalval",@"originalkey",
                nil]];
```
## 设置抽样率

NLog的抽样率值为0 - 1，默认抽样率为1，即100%。
设置抽样率有两种方式，云端和运行时。

__云端设置__

首先我们看下云端策略文件抽样率的格式：

```javascript
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
```
其中的`sampleRate`是一个map结构，key是tracker的name（NLog中默认tracker的name为appid），也可以为所有tracker设置一个默认值；

__运行时设置__

通过以下方法设置即可：

```javascript
[[NLog getTracker:@"wenku"] setSampleRate:0.5];
```



