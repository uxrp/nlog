//
//  NLogConfig.m
//  NLog
//
//  Created by Miller on 13-7-11.
//
//

#import "NLogConfig.h"

#define NLOG_POLICY_KEY  @"nlog_policy"

static NLogConfig * _sharedInstance = nil;

@implementation NLogConfig

+ (NLogConfig *)sharedInstance{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NLogConfig alloc] init];
        
        // 每次初始化时检查更新
        [_sharedInstance checkUpdate];
        
    });
    return _sharedInstance;
}

+ (NLogConfig *)sharedInstanceWith:(NSDictionary*)configs{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[NLogConfig alloc] init];
        
        [_sharedInstance setConfigs:configs];
        
        // 每次初始化时检查更新
        [_sharedInstance checkUpdate];
        
    });
    return _sharedInstance;
}

- (id)init{
    self = [super init];
    
    if (self) {
        defaultConfig = [[NSMutableDictionary dictionaryWithObjectsAndKeys:
                          RECEIVER_URL, @"receiverUrl",
                          REMOTE_RULE_URL, @"remoteRuleUrl",
                          [NSNumber numberWithInt:REMOTE_RULE_EXPIRES], @"remoteRuleExpires",
                          [NSNumber numberWithBool:ONLY_SEND_WITH_WIFI], @"onlyWifi",
                          [NSNumber numberWithInt:TIMER_INTERVAL], @"sendInterval",
                          [NSNumber numberWithInt:TIMER_INTERVAL_WITH_WIFI], @"sendIntervalWifi",
                          [NSNumber numberWithInt:SESSION_TIMEOUT], @"sessionTimeout",
                          [NSNumber numberWithInt:STORAGE_EXPIRES], @"storageExpires",
                          [NSNumber numberWithInt:MAX_SEND_SIZE], @"sendMaxLength",
                          [NSNumber numberWithInt:SAMPLE_RATE], @"sampleRate",
                          SDK_VERSION, @"sdkVersion",
                          ENCRYPT_TOKEN,@"encryptToken"
                          
                          , nil] retain];
        
    }
    
    return self;
}

- (void)setConfigs:(NSDictionary *)configs{
    if (configs == nil) {
        return;
    }
    
    for( id key in configs ) {
        [defaultConfig setValue:[configs objectForKey:key] forKey:key];
    }
}

- (id)get:(NSString *)key{
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary* policy = [defaults objectForKey:NLOG_POLICY_KEY];
    id policyItem = nil;
    
    if (policy != nil) {
        policyItem = [policy objectForKey:key];
    }
    
    if (policyItem == nil) {
        policyItem = [self getDefault:key];
    }
    
    return policyItem;
}

- (id)get:(NSString *)key subkey:(NSString *)subkey{
    
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary* policy = [defaults objectForKey:NLOG_POLICY_KEY];
    id policyItem = nil;
    
    if (policy != nil) {
        policyItem = [policy objectForKey:key];
    }
    
    if ([policyItem isKindOfClass:[NSDictionary class]] ) {
        policyItem = [policyItem objectForKey:subkey];
        
        if (!policyItem && [key isEqualToString:@"sampleRate"]) {
            policyItem = [policyItem objectForKey:@"default"];
        }
    }
    
    if (policyItem == nil) {
        policyItem = [self getDefault:key];
    }

    return policyItem;
}

- (id)getDefault:(NSString *)key{
    
    /*
    if ([key isEqualToString:@"sessionTimeout"]) {
        return [NSNumber numberWithInt:SESSION_TIMEOUT];
    }
    else if ([key isEqualToString:@"storageExpires"]) {
        return [NSNumber numberWithInt:STORAGE_EXPIRES];
    }
    else if ([key isEqualToString:@"sendMaxLength"]) {
        return [NSNumber numberWithInt:MAX_SEND_SIZE];
    }
    else if ([key isEqualToString:@"sendInterval"]) {
        return [NSNumber numberWithInt:TIMER_INTERVAL];
    }
    else if ([key isEqualToString:@"sendIntervalWifi"]) {
        return [NSNumber numberWithInt:TIMER_INTERVAL_WITH_WIFI];
    }
    else if ([key isEqualToString:@"onlyWifi"]) {
        return [NSNumber numberWithBool:ONLY_SEND_WITH_WIFI];
    }
    else if ([key isEqualToString:@"ruleExpires"]) {
        return [NSNumber numberWithInt:REMOTE_RULE_EXPIRES];
    }
    else if ([key isEqualToString:@"sampleRate"]) {
        return [NSNumber numberWithFloat:SAMPLE_RATE];
    }
    else{
        return nil;
    }
    */
    return [defaultConfig objectForKey:key];
}

- (void) checkUpdate{
    Boolean needUpdate = NO;
    NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
    NSDictionary* policy = [defaults objectForKey:NLOG_POLICY_KEY];
    
    if (policy != nil) {
        long long lastUpdateTime = [[policy objectForKey:@"lastUpdate"] longLongValue];
        long long current = CurrentTimeMillis;
        long long expires = [[self get:@"ruleExpires"] longLongValue];
        
        if (current - lastUpdateTime > expires * 3600 * 24 * 1000 ) {
            needUpdate = YES;
        }
        
    }
    else {
        needUpdate = YES;
    }
    
    if (needUpdate) {
        [self updatePolicy];
    }
    
}

- (void) updatePolicy{
    [self performSelectorInBackground:@selector(downloadPolicyFile:) withObject:nil];
}

/**
 * 下载策略文件更新到NSUserDefaults
 */
- (void) downloadPolicyFile:(id)arg{
    NSAutoreleasePool* autoreleasePool = [[NSAutoreleasePool alloc] init];
    
    @try {
        NPrintLog(@"Downloading policy file...");
        NSString *urlString = [self getDefault:@"remoteRuleUrl"];
        NSMutableURLRequest *request = [[[NSMutableURLRequest alloc] init] autorelease];
        [request setURL:[NSURL URLWithString:urlString]];
        [request setHTTPMethod:@"GET"];
        
        //set headers
        NSString *contentType = [NSString stringWithFormat:@"application/json"];
        [request addValue:contentType forHTTPHeaderField: @"Content-Type"];
        
        //get response
        NSHTTPURLResponse* urlResponse = nil;
        NSError *error = nil;
        NSData *responseData = [NSURLConnection sendSynchronousRequest:request
                                                     returningResponse:&urlResponse error:&error];
        
        NSString *result = [[NSString alloc] initWithData:responseData
                                                 encoding:NSUTF8StringEncoding];
        
        NPrintLog(@"Policy file response code: %d", [urlResponse statusCode]);
        
        if ([urlResponse statusCode] >= 200 && [urlResponse statusCode] < 300) {
            //NSLog(@"Response: %@", result);
            
            NSMutableDictionary *jsonResult = [result mutableObjectFromJSONString];
            
            [jsonResult setObject: [NSNumber numberWithLongLong:CurrentTimeMillis]
                           forKey:@"lastUpdate"];
            
            NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
            [defaults setObject:jsonResult forKey:NLOG_POLICY_KEY];
            
            [defaults synchronize];
            
        }
        
        [result release];
    }
    @catch (NSException *exception) {
        NPrintLog(@"Download policy file exception: %@",[exception reason]);
    }
    @finally {
        
    }
    
    [autoreleasePool drain];
}


+ (id)get:(NSString *)key{
    return [_sharedInstance get:key];
}

+ (id)get:(NSString *)key subkey:(NSString *)subkey{
    return [_sharedInstance get:key subkey:subkey];
}
@end