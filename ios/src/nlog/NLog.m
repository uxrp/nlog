//
//  NLog.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-8.
//  +TODO:程序调通
//  +TODO:启动发送时应该将当前启动时收集的数据发送出去
//  +TODO:CUID [NLog set]
//  +TODO:currentTime...函数提取出来，不放在NSession
//  +TODO:挂起前发送数据
//  +TODO:文档
//  +TODO:lib包（framework)
//  +TODO:JSONKit外置版本
//  +TODO:真机测试
//  TODO: 旧Log在立即发送接口调用后不发送
//  *TODO:field protocol
//  *TODO:多tracker
//  TODO:加密
//  TODO:抽样率
//  +TODO:本地存储协议版本
//  +TODO:网络异常测试
//  +TODO:数据过期测试
//  TODO:输出日志优化（输出开关、内容）
//  TODO:异常捕获
//  TODO:UnitTest
//

#import "NLog.h"
#import "NTracker.h"
#import "NSession.h"
#import "NStorage.h"
#import "NLogConfig.h"
#import "NSender.h"
#import <UIKit/UIDevice.h>
#import <UIKit/UIApplication.h>




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
    
    NSDictionary* sessionData = [_session dataInDict];
    
    long long currentTime = CurrentTimeMillis;
    long long sessionPausedTime = [[sessionData objectForKey:@"end"] longLongValue];
    int sessionTimeout = [[NLogConfig get:@"sessionTimeout"] integerValue];
    
    if ((currentTime - sessionPausedTime) > (sessionTimeout * 1000 )) {
        [_session reset];
    }
}

+ (id)sharedInstanceWithAppId:(NSString *)appId {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NLog alloc] initWithAppId:appId];
        
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

    });
    return _sharedInstance;
}

+ (id)sharedInstanceWithAppId:(NSString *)appId configs:(NSDictionary *)configs {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NLog alloc] initWithAppId:appId configs: configs];
        
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
        
    });
    return _sharedInstance;
}

+ (id)sharedInstance{
    return _sharedInstance;
}

+ (void) startWithAppId:(NSString *)appid {
//    NSLog(@"NLog started with appid: %@", appid);
    [NLog sharedInstanceWithAppId:appid];
}

+ (void) startWithAppId:(NSString *)appid configs:(NSDictionary *)configs {
    //    NSLog(@"NLog started with appid: %@", appid);
    [NLog sharedInstanceWithAppId:appid configs:configs];
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
          interval: (NSTimeInterval *)interval
              name: (NSString *)name
             label: (NSString *)label{
    [[NLog getDefaultTracker] sendTiming:category interval:interval name:name label:label];
}

+ (void)sendException: (NSString *)description
              isFatal: (Boolean) isFatal
               params: (NSDictionary *)params{
    [[NLog getDefaultTracker] sendException:description isFatal:isFatal params:params];
}

+ (void)set:(NSString *)key val:(id)val{
    [[NLog getDefaultTracker] set:key value:val];
}

@end

