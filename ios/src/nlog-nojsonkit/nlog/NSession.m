//
//  NSession.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-9.
//
//

#import "NLogConfig.h"
#import "NSession.h"

static NSession * _sharedInstance = nil;

@implementation NSession

@synthesize sessionId = _sessionId,
            seq = _seq;

- (id)init{
    _seq = -1;
    [self reset];
    return self;
}

- (void)pause{
    _end = CurrentTimeMillis;
}

- (void)reset{
    _start = CurrentTimeMillis;
    _end = -1;
    _seq++;
    [_sessionId release],_sessionId = nil;
    _sessionId = [[self generateId] copy];
}

/**
 * 时间+随机数
 */
- (NSString *)generateId{
    NSString* date = [NSString stringWithFormat:@"%llx", CurrentTimeMillis];
    int random = arc4random() % 10;
    
    return [NSString stringWithFormat:@"%@%d",date,random];
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

+ (void)reset{
    [[NSession sharedInstance] reset];
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
