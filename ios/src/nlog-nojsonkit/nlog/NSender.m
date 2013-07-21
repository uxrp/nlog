//
//  NSender.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-11.
//
//

#import "NSender.h"
#import <zlib.h>
#import <stdlib.h>
#import <UIKit/UIDevice.h>
#import <UIKit/UIApplication.h>
#import "NLogConfig.h"
#import "NReachability.h"
#import "NStringExtension.h"



@implementation NSender

static NSender* _sharedInstance = nil;

+ (id)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NSender alloc] init];
    });
    return _sharedInstance;
}

- (id) init{
    [super init];
        
    // 启动立即发送数据
    // [self performSelectorInBackground:@selector(_sendAll) withObject:nil];
    // 为了将启动数据尽快发送出去采取异步处理
    [NSTimer scheduledTimerWithTimeInterval:1
                                     target:self
                                   selector:@selector(_sendAll)
                                   userInfo:nil
                                    repeats:NO];
    
    // 定期发送数据
    [self startSendTimer];
    
    // 监听消息响应特殊需求
    [[NSNotificationCenter defaultCenter] addObserver: self
                                             selector: @selector(_sendAll)
                                                 name: @"NLOG_SEND_NOW"
                                               object: nil];
    
    // APP进入后台前发送日志（可用时间为5s）
    NSString *reqSysVer = @"4.0";
    NSString *currSysVer = [[UIDevice currentDevice] systemVersion];
    
    // 监听APP前后台切换事件
    if ([currSysVer compare:reqSysVer options:NSNumericSearch] != NSOrderedAscending){
        [[NSNotificationCenter defaultCenter] addObserver: self
                                                 selector: @selector(enteredBackground:)
                                                     name: UIApplicationDidEnterBackgroundNotification
                                                   object: nil];    }

    
    return self;
}

- (void) startSendTimer{
    NSString* intervalFields = @"sendInterval";
    
    if ([self isWifi]) {
        intervalFields = @"sendIntervalWifi";
    }
    
    int interval = [[NLogConfig get:intervalFields] intValue];
    
    sendTimer = [NSTimer scheduledTimerWithTimeInterval:interval
                                                 target:self
                                               selector:@selector(_sendAll)
                                               userInfo:nil
                                                repeats:YES];
    
    [sendTimer retain];
}

- (void)enteredBackground:(NSNotification *) notification{
    [self performSelectorInBackground:@selector(_sendAll) withObject:nil];
}

/**
 * TODO: nlog cache为空时会挂掉
 */
- (void) _sendAll{
    
    @synchronized(self){
//        return;
        if (![self canISend]) {
            return;
        }
        
        NSAutoreleasePool* autoreleasePool = [[NSAutoreleasePool alloc] init];
        
        NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
        NSDictionary* nlogCache = [defaults objectForKey:NLOG_CACHE_KEY];
        
        
        // NOTICE:必须要使用alloc initWithDictionary方法，否则会挂
        NSMutableDictionary* mutableCache = [[NSMutableDictionary alloc] initWithDictionary:nlogCache];

        // 当天日期
        
        NSDateFormatter* dateFormat = [[NSDateFormatter alloc] init];
        [dateFormat setDateFormat:@"yyyy-MM-dd"];
        NSString *currentDateStr = [dateFormat stringFromDate:[NSDate date]];
        
        double todayTimestamp = round ([[NSDate date] timeIntervalSince1970] * (double)1000);
        
        /*
         'af0f1795be01562fb8ef5d9f954fcb9e' : {
            'date': '2013-7-11',
            'head': 'http://www.baidu.com?a=1',
            'logs': [
                'ht=appview&a=1'
            ],
            'lockedLogs': [
                'ht=appview&a=1'            
            ]
         }
         */
        // 因为无法修改遍历对象，因此这里对nlogCache遍历key，然后在mutableCache上操作
        
        for( id key in nlogCache ) {
            // 检查有效性
            // 1. 如果存入日期不是当天且日志数据为空则删除，因为每天的数据都会独立存储；
            // 2. 数据是否超过设定的时间；
            NSDictionary* logItem = [mutableCache objectForKey:key];
            NSMutableDictionary* mutableLogItem = [[NSMutableDictionary alloc] initWithDictionary:logItem];
            NSString* logDate = [logItem objectForKey:@"date"];
            
            // 删除过期格式日志
            // 日志格式版本
            NSNumber* version = [logItem objectForKey:@"version"];
            
            if ([version intValue] != LOG_FORMAT_VERSION) {
                [mutableCache removeObjectForKey:key];
            }
            else if (![logDate isEqualToString:currentDateStr]) {
                // 日志是否为空
                int logsCount = [[logItem objectForKey:@"logs"] count];
                int lockedLogsCount = [[logItem objectForKey:@"lockedLogs"] count];
                
                // 获取有效期校验数据
                NSDateFormatter *dateFormat = [[NSDateFormatter alloc] init];
                [dateFormat setDateFormat:@"yyyy-MM-dd"];
                NSDate *date = [dateFormat dateFromString: logDate];
                double dateTimestamp = round ([date timeIntervalSince1970] * (double)1000);
                
                // 有效期设置（单位：天）
                double expires = [[NLogConfig get:@"storageExpires"] doubleValue] * 3600 * 24 * 1000 ;
                
                if (
                    // 无效空存储单元
                    logsCount + lockedLogsCount == 0
                    // 过期数据
                    || todayTimestamp - dateTimestamp > expires
                    ) {
                    [mutableCache removeObjectForKey:key];
                }
            }
            else{
                // 正式发送前先将数据从logs搬运到lockedLogs，防止发送失败后再次写入数据
                // 确保发送失败的数据再重试时发送的数据和之前是一致的（后台可以校验除重）
                NSArray* logs = [logItem objectForKey:@"logs"];
                NSMutableArray* mutableLogs = [[NSMutableArray alloc] initWithArray:logs];
                
                NSArray* lockedLogs = [logItem objectForKey:@"lockedLogs"];
                NSMutableArray* mutableLockedLogs = [[NSMutableArray alloc] initWithArray:lockedLogs];
                
                // logs -> lockedLogs
                for (int i = 0; i < [logs count] ;i++ ) {
                    [mutableLockedLogs addObject:[mutableLogs objectAtIndex:i]];
                }
                
                // 清空logs
                [mutableLogs removeAllObjects];
                
                NSString* head = [logItem objectForKey:@"head"];
                
                if (!IS_DEBUG) {
                    head = [NStringExtension unencrypt:head];
                }
                
                // 发送lockedLogs中的数据
                for (int i = [mutableLockedLogs count] - 1; i >= 0; i--) {
                    NSString* logData = [mutableLockedLogs objectAtIndex:i];
                    
                    if (!IS_DEBUG) {
                        logData = [NStringExtension unencrypt:logData];
                    }
                    
                    BOOL successed = [self _send:
                                      [NSDictionary dictionaryWithObjectsAndKeys:
                                       head, @"head",
                                       logData, @"data"
                                       , nil]];
                    
                    // 如果发送成功则从lockedLogs中删除日志
                    if (successed == YES) {
                        [mutableLockedLogs removeObjectAtIndex:i];
                    }
                    // 如果发送失败则终止发送，以免在某些极端囤积日志很多的情况下导致网络繁忙
                    else{
                        break;
                    }
                }
                
                [mutableLogItem setObject:mutableLogs forKey:@"logs"];
                [mutableLogItem setObject:mutableLockedLogs forKey:@"lockedLogs"];
                [mutableLogs release];
                [mutableLockedLogs release];
                
                [mutableCache setObject:mutableLogItem forKey:key];
                
                [mutableLogItem release];
            }
        }
        
        [defaults setObject:mutableCache forKey:NLOG_CACHE_KEY];
        [defaults synchronize];
        
        [mutableCache release];
        [autoreleasePool drain];
    }
}

- (BOOL) _send:(NSDictionary *) log{
    NSAutoreleasePool* autoreleasePool = [[NSAutoreleasePool alloc] init];
    
    BOOL successed = NO;
    
    NSString *url = [log objectForKey:@"head"];
    NSString *data = [log objectForKey:@"data"];
    
    // 正式环境发送前需要对本地日志解密
//    if (!IS_DEBUG) {
//        data = [data unencrypt];
//    }
    
    NSData *logData = [data dataUsingEncoding:NSUTF8StringEncoding];
    
    NSMutableURLRequest* req = [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:url]];
    NSURLResponse* response = nil;
    
    NSData* gziped = [self gzipDeflate:logData];
    [req setHTTPMethod: @"POST"];
    [req setHTTPBody:gziped];
    [req setTimeoutInterval:10];
    [req addValue:@"gzip" forHTTPHeaderField:@"Content-Type"];
    
    // 配合日志服务器校验，需要增加额外HTTP Header
    int rawLength = data.length;
    int gzipLength = gziped.length;
    
    NSString* combineStr = [NSString stringWithFormat:@"%d%%%d", rawLength, gzipLength];
    NSData* combineStrData = [combineStr dataUsingEncoding:NSUTF8StringEncoding];
    
    unsigned long result = crc32(0, combineStrData.bytes, combineStrData.length);
    
    [req addValue:[NSString stringWithFormat:@"%d",rawLength] forHTTPHeaderField:@"length"];
    [req addValue:[NSString stringWithFormat:@"%d",gzipLength] forHTTPHeaderField:@"Content-Length"];
    [req addValue:[NSString stringWithFormat:@"%lu",result] forHTTPHeaderField:@"md5"];
    
    [NSURLConnection sendSynchronousRequest:req returningResponse:&response error:nil];
    NSHTTPURLResponse* httpResponse = (NSHTTPURLResponse*)response;
    
    // 发送成功
    if ([httpResponse statusCode] == 200){
        
        successed = YES;
    }
    
    [req release];
    
    [autoreleasePool drain];
    
    return successed;
}

- (NSData *)gzipDeflate:(NSData*) src
{
	if ([src length] == 0) return src;
	
	z_stream strm;
	
	strm.zalloc = Z_NULL;
	strm.zfree = Z_NULL;
	strm.opaque = Z_NULL;
	strm.total_out = 0;
	strm.next_in=(Bytef *)[src bytes];
	strm.avail_in = [src length];
	
	// Compresssion Levels:
	//   Z_NO_COMPRESSION
	//   Z_BEST_SPEED
	//   Z_BEST_COMPRESSION
	//   Z_DEFAULT_COMPRESSION
	
	if (deflateInit2(&strm, Z_DEFAULT_COMPRESSION, Z_DEFLATED, (15+16), 8, Z_DEFAULT_STRATEGY) != Z_OK) return nil;
	
	NSMutableData *compressed = [NSMutableData dataWithLength:16384];  // 16K chunks for expansion
	do {
		
		if (strm.total_out >= [compressed length])
			[compressed increaseLengthBy: 16384];
		
		strm.next_out = [compressed mutableBytes] + strm.total_out;
		strm.avail_out = [compressed length] - strm.total_out;
		
		deflate(&strm, Z_FINISH);
		
	} while (strm.avail_out == 0);
	deflateEnd(&strm);
	[compressed setLength: strm.total_out];
	return [NSData dataWithData:compressed];
}

- (BOOL) isWifi{
    return [[NReachability reachabilityForInternetConnection]
            currentReachabilityStatus] == NReachableViaWiFi;
}

- (BOOL) disconnected{
    return [[NReachability reachabilityForInternetConnection]
            currentReachabilityStatus] == NNotReachable;}

- (BOOL) canISend{
    BOOL onlyWifi = [[NLogConfig get:@"onlyWifi"] boolValue];
    
    if ((onlyWifi && ![self isWifi]) || [self disconnected]) {
        return NO;
    }
    
    return YES;
}

- (void) dealloc{
    [sendTimer release];
    
    [super dealloc];
}

@end
