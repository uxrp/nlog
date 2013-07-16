//
//  NTracker.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-8.
//
//
#import "NTracker.h"
#import "NSession.h"
#import "NLogConfig.h"

static NSMutableDictionary * trackers = nil;

@implementation NTracker

@synthesize name;

- (id)init{
    [super init];
    [self start];
    
    /**
     * 默认采样率:100%
     */
    sampleRate = [NLogConfig get:@"sampleRate"];
    
    fields = [[NSMutableDictionary alloc] init];
    
    // 设置默认服务器路径
    [fields setValue:[NLogConfig get:@"receiverUrl"] forKey:@"receiverUrl"];
    
    return self;
}

- (void)start{
    running = YES;
}

- (void)stop{
    running = NO;
}

- (void) send:(NSString *)hitType params:(NSDictionary *)params{
    [self send:hitType params:params immediately:NO];
}

- (void) send:(NSString *)hitType params:(NSDictionary *)params immediately:(BOOL)now{
    if (running == NO) {
        NSLog(@"NLog is stopped, so no data sent.");
        return;
    }
        
    NSMutableDictionary* mutableParams = [NSMutableDictionary dictionaryWithDictionary:params];
    
    // 增加保留字段
    [mutableParams setValue:[NSession getId] forKey:@"sid"];
    [mutableParams setValue:[NSNumber numberWithInt:[NSession getSeq]] forKey:@"seq"];
    [mutableParams setValue:[NSNumber numberWithLongLong:CurrentTimeMillis] forKey:@"time"];
    [mutableParams setValue:hitType forKey:@"ht"];
    
    [[NSNotificationCenter defaultCenter]
     postNotificationName:@"NLOG_TRACKER_SEND"
     object:nil
     userInfo:[NSDictionary dictionaryWithObjectsAndKeys:
            mutableParams,@"params",
            fields,@"fields",
            name,@"name",
            [NSNumber numberWithBool:now],@"sendNow",
                nil]];

}

- (void) sendEvent:(NSString *)category
            action:(NSString *)action
             label:(NSString *)label
             value:(NSNumber *)value{
    
    NSString *hitType = @"event";
    NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:
                            category,   @"eventCategory",
                            action,     @"eventAction",
                            label,      @"eventLabel",
                            value,      @"eventValue",
                            nil];
    
    [self send:hitType params:params];
}

- (void) sendException:(NSString *)description
               isFatal:(Boolean)isFatal
                params:(NSDictionary *)params{
    
    NSString *hitType = @"exception";
    
    if (params == nil) {
        params = [[NSMutableDictionary alloc] init];
    }
    else {
        params = [NSMutableDictionary dictionaryWithDictionary:params];
    }
    
    [params setValue:description forKey:@"exDescription"];
    [params setValue:[NSNumber numberWithBool:isFatal] forKey:@"exFatal"];

    [self send:hitType params:params];
}

- (void) sendTiming:(NSString *)category
          interval:(NSTimeInterval *)interval
              name:(NSString *)logName
             label:(NSString *)label{
    
    NSString *hitType = @"timing";
    NSDictionary* params = [NSDictionary dictionaryWithObjectsAndKeys:
                            category,   @"tmCategory",
                            [NSNumber numberWithInt:(int)interval],   @"tmInterval",
                            logName,       @"tmName", 
                            label,      @"tmLabel", 
                            nil];
    
    [self send:hitType params:params];
}

- (void) sendView:(NSString *)viewName params:(NSDictionary *)params{
    
    NSString *hitType = @"appview";
    
    if (params == nil) {
        params = [[NSMutableDictionary alloc] init];
    }
    else {
        params = [NSMutableDictionary dictionaryWithDictionary:params];
    }
    
    [params setValue:viewName forKey:@"appScreen"];
    
    [self send:hitType params:params];

}

- (void) setSampleRate:(NSNumber *)rate{
    sampleRate = [rate retain];
}

- (void) set:(NSDictionary *)params{
    if (params == nil) {
        return;
    }
    
    for(id key in params){
        [fields setValue:[params objectForKey:key] forKey:key];
    }
}

- (void) set:(NSString *)key value:(id)val{
    [fields setValue:val forKey:key];
}

+ (NTracker *) getTracker:(NSString *)trackerId{
    NTracker* tracker = nil;
    
    if (trackers != nil) {
        tracker = [trackers objectForKey:trackerId];
    }
    else {
        trackers = [[NSMutableDictionary alloc] init];
    }
    
    if (tracker == nil) {
        tracker = [[NTracker alloc] init];
        tracker.name = [trackerId retain];
        [trackers setValue:tracker forKey:trackerId];
    }
    
    return tracker;
}

@end
