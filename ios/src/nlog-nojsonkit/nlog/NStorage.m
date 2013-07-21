//
//  NStorage.m
//  NLog
//
//  Created by Miller on 13-7-10.
//
//

#import "NStorage.h"
#import "NSession.h"
#import "NLogConfig.h"
#import "NStringExtension.h"

static NStorage * _sharedInstance = nil;

@implementation NStorage

+ (id)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NStorage alloc] init];
        
        [[NSNotificationCenter defaultCenter] addObserver: _sharedInstance
                                                 selector: @selector(cacheLogData:)
                                                     name: @"NLOG_TRACKER_SEND"
                                                   object: nil];
    });
    return _sharedInstance;
}

- (id) init{
    [super init];
    
    [self initUserDefaults];
    
    return self;
}

- (void) initUserDefaults{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary *cache = [defaults objectForKey:NLOG_CACHE_KEY];
    
    if (cache == nil) {
        [defaults setValue:[[NSDictionary alloc] init] forKey:NLOG_CACHE_KEY];
        [defaults synchronize];
    }
}

/**
 * 为tracker生成用于在NSUserDefaults中存储数据的key
 * key的生成为MD5(trackername + fieldsstring + date + version)
 * 同一tracker，同fields数据以及同一天的数据会存储在同一个key下
 * 同一天的数据存储在一起易于清除过期数据
 */
- (NSString *) genKeyForTrackerData:(NSString *)trackerName
                             fields:(NSDictionary *)trackerFields{
    NSString *fieldsStr = [NStringExtension urlParametersStringFromDictionary:trackerFields];
    
    NSDateFormatter* dateFormat = [[NSDateFormatter alloc] init];    
    [dateFormat setDateFormat:@"yyyy-MM-dd"];
    
    NSString *currentDateStr = [dateFormat stringFromDate:[NSDate date]];
    
    NSString *keyStr = [NSString stringWithFormat:@"%@%@%@%d",trackerName,fieldsStr,currentDateStr,LOG_FORMAT_VERSION];
    
//    NSLog(@"genKeyForTrackerData key str:%@",keyStr);
    
    return [NStringExtension md5:keyStr];
}

- (NSDictionary *)createLogWith:(NSDictionary *)fields{
    NSString *receiverUrl = [fields objectForKey:@"receiverUrl"];
    NSMutableDictionary* log = [[NSMutableDictionary alloc] init] ;
    
    NSMutableDictionary* mutableFields = [NSMutableDictionary dictionaryWithDictionary:fields];
    [mutableFields removeObjectForKey:@"receiverUrl"];
    
    NSString *head = [NSString stringWithFormat:@"%@&%@",
                      receiverUrl,
                      [NStringExtension urlParametersStringFromDictionary:mutableFields]];
    
    if (!IS_DEBUG) {
        head = [NStringExtension encrypt:head];
    }
    
    [log setValue:head forKey:@"head"];
    [log setValue:[NSArray array] forKey:@"logs"];
    [log setValue:[NSArray array] forKey:@"lockedLogs"];
    
    [log setValue:[NSNumber numberWithInt:LOG_FORMAT_VERSION] forKey:@"version"];
    
    NSDateFormatter* dateFormat = [[NSDateFormatter alloc] init];
    [dateFormat setDateFormat:@"yyyy-MM-dd"];
    
    NSString *currentDateStr = [dateFormat stringFromDate:[NSDate date]];
    
    [log setValue:currentDateStr forKey:@"date"];
    
    return [log autorelease];
}

- (void) cacheLogData:(NSNotification *)notification{
    [self performSelectorInBackground:@selector(_cacheLogData:) withObject:notification];
}

- (void) _cacheLogData:(NSNotification *)notification{
    
    @synchronized(self){
        NSAutoreleasePool* autoreleasePool = [[NSAutoreleasePool alloc] init];
        /*
         NLOG_CACHE_KEY: {
            'abcdefmd5str' : {
                'head': 'http://abc.com?a=1&b=2',
                'logs': [
                    'a=1&b=2',
                    'c=3&d=4'
                ],
                'lockedLogs': []
            }
         }
         */
    
//      NSLog(@"%@", [NSString urlParametersStringFromDictionary:[[notification userInfo] objectForKey:@"params"]]);
        NSDictionary *fields = [[notification userInfo] objectForKey:@"fields"];
        NSDictionary *params = [[notification userInfo] objectForKey:@"params"];
        NSString *name = [[notification userInfo] objectForKey:@"name"];
        
        // 是否在存储后立即发送
        BOOL sendNow = [[[notification userInfo] objectForKey:@"sendNow"] boolValue];
    
        NSString *paramsStr = [NStringExtension urlParametersStringFromDictionary:params];
    
        NSString *key = [self genKeyForTrackerData:name fields:fields];
    
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        NSDictionary *cache = [defaults objectForKey:NLOG_CACHE_KEY];
    
        NSMutableDictionary *mutableCache = [[NSMutableDictionary alloc] initWithDictionary:cache];
    
        NSDictionary *logItem = [mutableCache objectForKey:key];
    
        if (logItem == nil) {
        
            logItem = [self createLogWith:fields];
        }
    
        NSMutableDictionary *mutableLogItem = [[NSMutableDictionary alloc] initWithDictionary:logItem];
        NSMutableArray *logs = [[NSMutableArray alloc] initWithArray:[mutableLogItem objectForKey:@"logs"]];
    
        // 尝试放入最后一个单元
        NSString *lastItem = [logs lastObject];
    
        // 如果超出限制则新建一个单元
        if ( lastItem == nil || [lastItem length] + [paramsStr length] > [[NLogConfig get:@"sendMaxLength"] integerValue] * 1024 ) {
            
//            NSLog(@"before encrypt length:%d", [paramsStr length]);
            
            if (!IS_DEBUG) {
                paramsStr = [NStringExtension encrypt:paramsStr];
            }
            
//            NSLog(@"after encrypt length:%d", [paramsStr length]);
            
//            NSLog(@"after unencrypt:%@", [paramsStr unencrypt]);
            
            [logs addObject:paramsStr];
        }
        else {
            
            if (!IS_DEBUG) {
                lastItem = [NStringExtension unencrypt:lastItem];
            }
            
            // NSLog(@"lastItem:%@",lastItem);
            
            lastItem = [NSString stringWithFormat:@"%@\n%@",lastItem,paramsStr];
            
            if (!IS_DEBUG) {
                lastItem = [NStringExtension encrypt:lastItem];
            }
            
            [logs
             setObject: lastItem
             atIndexedSubscript:[logs count] - 1];
        }
    
        [mutableLogItem setValue:logs forKey:@"logs"];
        [mutableCache setValue:mutableLogItem forKey:key];
        [defaults setObject:mutableCache forKey:NLOG_CACHE_KEY];
    
        [defaults synchronize];
        [mutableCache release];
        [mutableLogItem release];
        [logs release];
    
        if (sendNow) {
            [[NSNotificationCenter defaultCenter] postNotificationName:@"NLOG_SEND_NOW" object:nil];
        }
    
        [autoreleasePool drain];
    }
}

@end
