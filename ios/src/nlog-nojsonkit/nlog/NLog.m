//
//  NLog.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-8.
//  TODO:异常捕获（入口参数类型检查，尤其是ID类型）
//  TODO:UnitTest
//  TODO:输出日志优化（输出开关、内容）
//  TODO: 默认日志数据
//  +TODO: ht字段压缩：用数字代替
//  +TODO:时长统计接口
//  +TODO:session id 与 adroid版一致性
//  +TODO:Tracker加多线程锁
//  +TODO:iOS7 mac address 换openuuid
//  +TODO:加密
//  +TODO:抽样率
//  +TODO:field protocol
//  +TODO:多tracker
//  +TODO:线上地址POST调通
//  +TODO:程序调通
//  +TODO:启动发送时应该将当前启动时收集的数据发送出去
//  +TODO:CUID [NLog set]
//  +TODO:currentTime...函数提取出来，不放在NSession
//  +TODO:挂起前发送数据
//  +TODO:文档
//  +TODO:lib包（framework)
//  +TODO:JSONKit外置版本
//  +TODO:真机测试
//  +TODO:旧Log在立即发送接口调用后不发送(在存在多个item时出现，不过请求均已发出，有可能是测试后端处理问题)
//  +TODO:本地存储协议版本
//  +TODO:网络异常测试
//  +TODO:数据过期测试
//

#import "NLog.h"
#import "NTracker.h"
#import "NSession.h"
#import "NStorage.h"
#import "NLogConfig.h"
#import "NSender.h"
#import "NStringExtension.h"
#import <UIKit/UIDevice.h>
#import <UIKit/UIApplication.h>

#define kNLogAppVersion    @"nlog_av"
//////////////////////////////////////////////////////

static NLog * _sharedInstance = nil;

@interface NLog ()
{
    NSString* _appId;
    NSession* _session;
    NStorage* _storage;
    NLogConfig* _logConfig;
    NSender* _sender;
}

//- (NSString *)getAppId;


//- (void)setOptions:(NSDictionary *)options;
//
//- (NSDictionary *)getOptions;

@property (nonatomic,retain)NSString * appId;
@end

@implementation NLog

@synthesize appId = _appId;

- (id)initWithAppId:(NSString *)appId {
    if (self = [super init]){
        _appId = [appId retain];
        
        _session = [[NSession sharedInstance] retain];
        
        _storage = [[NStorage sharedInstance] retain];
        
        _logConfig = [[NLogConfig sharedInstance] retain];
        
        _sender = [[NSender sharedInstance] retain];
        
        [[NTracker getTracker:_appId] set:@"aid" value:_appId];
        
    }
    return self;
}

- (id)initWithAppId:(NSString *)appId configs:(NSDictionary *)configs {
    if (self = [super init]){
        _appId = [appId retain];
        
        _session = [[NSession sharedInstance] retain];
        
        _storage = [[NStorage sharedInstance] retain];
        
        _logConfig = [[NLogConfig sharedInstanceWith:configs] retain];
        
        _sender = [[NSender sharedInstance] retain];
        
        [[NTracker getTracker:_appId] set:@"aid" value:_appId];
    }
    return self;
}

- (void)dealloc
{
    [_appId release];
    [_session release];
    [_storage release];
    [_logConfig release];
    [_sender release];
    
    [super dealloc];
}

//- (NSString *)getAppId{
//    return _appId;
//}

- (void)enteredBackground:(NSNotification *) notification{
    [_session pause];
}

- (void)enteredForeground:(NSNotification *) notification{
    [_session resume];
}

- (void)_sendSessionStart{
    [NLog send:@"session" params:@{@"act": @"start"}];
    NPrintLog(@"Send session start log.");
}

- (void)_sendSessionEnd:(NSNotification*)notification{
    NSMutableDictionary* params = [[NSMutableDictionary alloc] init];
    
    long long duration = [[[notification userInfo] objectForKey:@"duration"] longLongValue];
    // 通过[NSession recovery]触发的消息会携带旧sid，需要记录
    NSString* sid = [[notification userInfo] objectForKey:@"sid"];
    
    [params setObject:@"shutdown" forKey:@"act"];
    [params setObject:[NSNumber numberWithLongLong:duration] forKey:@"duration"];
    
    if (sid) {
        [params setObject:sid forKey:@"sid"];
    }
    
    [NLog send:@"session"
        params:params];
    
    [params release];
    
    NPrintLog(@"Send session end log.");
}

// 启动后需要发送的日志，延迟发送（待主线程中的通用数据设置完毕）
- (void) lazyActionAfterInit{
    // 检查是否有旧Session日志需要发送
    [NSession recovery];
    
    [self _sendSessionStart];
    
    // 检查版本升级
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSString* previousAV = [defaults objectForKey:kNLogAppVersion];
    NSString* currentAV = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    
    if (!previousAV || ![currentAV isEqualToString:previousAV]) {
        if (!previousAV) {
            previousAV = @"";
        }
        [defaults setObject:currentAV forKey:kNLogAppVersion];
        [NLog send:@"app" params:@{@"act": @"upgrade", @"pav":previousAV}];
        NPrintLog(@"Send app upgrade log.");
        
    }
    
}

+ (id)sharedInstanceWithAppId:(NSString *)appId {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NLog alloc] initWithAppId:appId];
        
        [NLog actionAfterInit];

    });
    return _sharedInstance;
}

+ (id)sharedInstanceWithAppId:(NSString *)appId configs:(NSDictionary *)configs {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NLog alloc] initWithAppId:appId configs: configs];
        
        [NLog actionAfterInit];
    });
    return _sharedInstance;
}

+ (void) actionAfterInit{
    [NLog bindEvents];
    
    // 发送APP启动时日志
    [NSTimer scheduledTimerWithTimeInterval:1
                                     target:_sharedInstance
                                   selector:@selector(lazyActionAfterInit)
                                   userInfo:nil
                                    repeats:NO];
    
}

+ (void)bindEvents{
    
    NSString *reqSysVer = @"4.0";
    NSString *currSysVer = [[UIDevice currentDevice] systemVersion];
    
    // 监听APP前后台切换事件
    if ([currSysVer compare:reqSysVer options:NSNumericSearch] != NSOrderedAscending){
        [[NSNotificationCenter defaultCenter] addObserver: _sharedInstance
                                                 selector: @selector(enteredBackground:)
                                                     name: UIApplicationDidEnterBackgroundNotification
                                                   object: nil];
        [[NSNotificationCenter defaultCenter] addObserver: _sharedInstance
                                                 selector: @selector(enteredForeground:)
                                                     name: UIApplicationWillEnterForegroundNotification
                                                   object: nil];
    }
    
    // 监听session周期
    [[NSNotificationCenter defaultCenter] addObserver: _sharedInstance
                                             selector: @selector(_sendSessionStart)
                                                 name: @"NLOG_SESSION_START"
                                               object: nil];

    [[NSNotificationCenter defaultCenter] addObserver: _sharedInstance
                                             selector: @selector(_sendSessionEnd:)
                                                 name: @"NLOG_SESSION_END"
                                               object: nil];}

+ (id)sharedInstance{
    return _sharedInstance;
}

+ (void) startWithAppId:(NSString *)appid {
//    NSLog(@"NLog started with appid: %@", appid);
    [NLog sharedInstanceWithAppId:appid];
    NPrintLog(@"started with appid:%@",appid);
}

+ (void) startWithAppId:(NSString *)appid configs:(NSDictionary *)configs {
    //    NSLog(@"NLog started with appid: %@", appid);
    [NLog sharedInstanceWithAppId:appid configs:configs];
    NPrintLog(@"started with appid:%@ and configs:%@",appid,configs);
}

+ (NSString *)getAppId{
    return [_sharedInstance appId];
}

+ (NTracker *)getTracker:(NSString *)trackerId{
    return [NTracker getTracker:trackerId];
}

+ (NTracker *)getDefaultTracker{
    return [NTracker getTracker:[NLog getAppId]];
}

+ (void)send: (NSString*)hitType params: (NSDictionary *) params{
    [[NLog getDefaultTracker] send:hitType params:params];
}

+ (void)send: (NSString*)hitType params: (NSDictionary *) params immediately:(BOOL)now{
    [[NLog getDefaultTracker] send:hitType params:params immediately:now];
}

+ (void)sendView: (NSString *)viewName params: (NSDictionary *)params{
    [[NLog getDefaultTracker] sendView:viewName params:params];
}

+ (void)sendEvent: (NSString *)category
           action: (NSString *)action
            label: (NSString *)label
            value: (NSNumber *)value{
    [[NLog getDefaultTracker] sendEvent:category action:action label:label value:value];
}

+ (void)sendTiming: (NSString *)category
          interval: (NSTimeInterval)interval
              name: (NSString *)name
             label: (NSString *)label{
    [[NLog getDefaultTracker] sendTiming:category interval:interval name:name label:label];
}

+ (void)sendException: (NSString *)description
              isFatal: (Boolean) isFatal
               params: (NSDictionary *)params{
    [[NLog getDefaultTracker] sendException:description isFatal:isFatal params:params];
}

+ (void)logDurationStart:(NSString *)name{
    [[NLog getDefaultTracker] logDurationStart:name];
}

+ (void)logDurationEnd:(NSString *)name{
    [[NLog getDefaultTracker] logDurationEnd:name];
}

+ (void)set:(NSString *)key val:(id)val{
    [[NLog getDefaultTracker] set:key value:val];
}

+ (void)set:(NSString *)key val:(id)val isMutable:(Boolean)isMutable{
    [[NLog getDefaultTracker] set:key value:val isMutable:isMutable];
}

@end

