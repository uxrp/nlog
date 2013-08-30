//
//  NSession.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-9.
//
//

#import "NLogConfig.h"
#import "NSession.h"

#define RANDOM_MAX 0×100000000
#define kNLogSession    @"nlog_session"

static NSession * _sharedInstance = nil;

@implementation NSession

@synthesize sessionId = _sessionId,
            seq = _seq;

- (id)init{
    _seq = -1;
    _start = 0;
    _end = 0;
    
    // 尝试从缓存恢复
    // [self recovery]; // 此时recovery，因其他功能未初始化完成，无法发送日志，改为外部调用
    
    [self reset];
    
    return self;
}

- (void)pause{
    _end = CurrentTimeMillis;
    
    [self backup];
    
    NPrintLog(@"session paused at:%lld", _end);
}

- (void)resume{
    NPrintLog(@"session resumed.");
    [self clear];
    
    long long currentTime = CurrentTimeMillis;
    int sessionTimeout = [[NLogConfig get:@"sessionTimeout"] integerValue];
    long long pauseDuration = currentTime - _end;
    
    if (pauseDuration > (sessionTimeout * 1000 )) {        
        [self reset];
    }
    else{
        // 有效的session在恢复时需要重新调整开始时间，否则停留时间会计算错误
        _start += pauseDuration;
    }
}

- (void)reset{
    
    /**
     * session的结束有两种情形:
     * 1. 切到后台恢复后超过session冰冻时限；
     * 2. 进入APP后，缓存中有上一次session记录；（Modified:这种情形已修改，不会在初始化时进入if）
     * 两种状态都会进入reset。
     */
    if (_start && _end) {
        // 当前session持续时间
        long long sessionDuration = _end - _start;
        
        NPrintLog(@"session end: %@ with duration:%lld", _sessionId, sessionDuration);
        
        // 发送session结束消息
        [[NSNotificationCenter defaultCenter]
         postNotificationName:@"NLOG_SESSION_END"
         object:nil
         userInfo:[NSDictionary dictionaryWithObjectsAndKeys:
                   [NSNumber numberWithLongLong:sessionDuration],@"duration",
                   [NSNumber numberWithLongLong:_end],@"t",
                   nil]];
        
    }
    
    _start = CurrentTimeMillis;
    _end = -1;
    _seq++;
    [_sessionId release],_sessionId = nil;
    _sessionId = [[self generateId] copy];
    
    // 发送session开始消息
    [[NSNotificationCenter defaultCenter] postNotificationName:@"NLOG_SESSION_START"
                                                        object:nil];
    
    
    NPrintLog(@"new session id:%@", _sessionId);
}

/**
 * 时间+随机数
 */
- (NSString *)generateId{
    
    double random = (arc4random() % 10000 ) / 10000.0;
    
    long suffix = 36 * 36 * 36 * 36 * random;
    
//    NSLog(@"random:%f",random);
    
    return [NSString stringWithFormat:@"%@%@",
            [self get36RadixFromLong:CurrentTimeMillis],
            [self get36RadixFromLong:suffix]];
}

- (NSString *)get36RadixFromLong:(long long)number{
    NSMutableString* result = [[NSMutableString alloc] init];
    
    long long answer;
    long long remainder;
    NSMutableArray* remainders = [[NSMutableArray alloc] init];
    
//    NSLog(@"number:%ld", number);
    
    while ((answer = floor(number / 36))) {
//        NSLog(@"answer:%ld", answer);
        remainder = number % 36;
        number = answer;
//        NSLog(@"remainder:%ld", remainder);
        
        [remainders addObject:[NSNumber numberWithLongLong:remainder]];
    }
    
    remainder = number % 36;
    [remainders addObject:[NSNumber numberWithLong:remainder]];
//    NSLog(@"remainder:%ld", remainder);
    
    for (int i = [remainders count] - 1; i >= 0; i--) {
        long long item = [[remainders objectAtIndex:i] longLongValue];
                
        if (item < 10) {
            [result appendFormat:@"%lld", item];
        }
        else if (item < 36){
            [result appendFormat:@"%c", (char)('a' + item - 10)];
        }
        
    }
    
    [remainders release];
    
    return [result autorelease];
}

/**
 * 将session存入NSUserDefault
 */
- (void)backup{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    
    NSDictionary* data = @{
                           @"sid":_sessionId,
//                           @"seq":[NSNumber numberWithInt:_seq],
                           @"start": [NSNumber numberWithLongLong:_start],
                           @"end": [NSNumber numberWithLongLong:_end]
                           };
    
    [defaults setObject:data forKey:kNLogSession];
    
    [defaults synchronize];
}
/**
 * 从NSUserDefault中恢复session
 * backup和recovery都没有对seq进行操作，主要考虑到seq恢复后不好处理；
 */
- (void)recovery{
    
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    
    NSDictionary* data = [defaults objectForKey:kNLogSession];
        
    if (data) {
        NSString* sid = [data objectForKey:@"sid"];
        long long start = [[data objectForKey:@"start"] longLongValue];
        long long end = [[data objectForKey:@"end"] longLongValue];
        
        // 当前session持续时间
        long long sessionDuration = end - start;
        
        NPrintLog(@"session end: %@ with duration:%lld", sid, sessionDuration);
        
        // 发送session结束消息
        [[NSNotificationCenter defaultCenter]
         postNotificationName:@"NLOG_SESSION_END"
         object:nil
         userInfo:[NSDictionary dictionaryWithObjectsAndKeys:
                   [NSNumber numberWithLongLong:sessionDuration],@"duration",
                   sid,@"sid",
                   [NSNumber numberWithLongLong:end],@"t",
                   nil]];
    }
    
    [self clear];
}

/**
 * 清除NSUserDefault中的session
 */
- (void)clear{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    
    [defaults removeObjectForKey:kNLogSession];
    
    [defaults synchronize];
}



- (NSDictionary *)dataInDict{

    return [NSDictionary dictionaryWithObjectsAndKeys:
            [NSNumber numberWithLongLong:_start],     @"start",
            [NSNumber numberWithLongLong:_end],       @"end",
            _sessionId,                               @"id",
            [NSNumber numberWithInt:_seq],            @"seq",
            nil];
}

+ (id)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NSession alloc] init];
    });
    return _sharedInstance;
}

+ (void)pause{
    [[NSession sharedInstance] pause];
}

+ (void)resume{
    [[NSession sharedInstance] resume];
}

+ (void)reset{
    [[NSession sharedInstance] reset];
}

+ (void)recovery{
    [[NSession sharedInstance] recovery];
}

+ (NSString *)getId{
    return _sharedInstance.sessionId;
}

+ (int)getSeq{
    return _sharedInstance.seq;
}

+ (NSDictionary *) dataInDict{
    return [[NSession sharedInstance] dataInDict];
}

@end
