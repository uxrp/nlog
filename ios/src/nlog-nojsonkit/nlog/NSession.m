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
//    NSLog(@"sid:%@", _sessionId);
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
    
    return result;
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
