//
//  NSStringExtension.m
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-15.
//
//

#import "NStringExtension.h"

#import <CommonCrypto/CommonDigest.h>
#include <sys/socket.h>
#include <sys/sysctl.h>
#include <net/if.h>
#include <net/if_dl.h>
#include "NLogConfig.h"

@implementation NStringExtension

+ (NSString *) md5:(NSString *)sourceStr
{
    const char* str = [sourceStr UTF8String];
    unsigned char result[16];
    CC_MD5(str, strlen(str), result);
    return [NSString stringWithFormat:@"%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",            result[0], result[1], result[2], result[3], result[4], result[5], result[6], result[7],            result[8], result[9], result[10], result[11], result[12], result[13], result[14], result[15]            ];
}

/**
 * 按位异或会导致加密后可能出现不存在的字符，因此返回的内容是加密后的16进制数据串，而不是应该显示的字符串；
 * 如果加密后的到的字符编码不存在并使用stringWithUTF8String时会得到空字符串；
 */
+ (NSString *)encrypt:(NSString *)sourceStr
{
    NSMutableString* hexResult = [[NSMutableString alloc] init];
    
    @try{
        NSString* token = [NLogConfig get:@"encryptToken"];
        
        NPrintLog(@"encrypt token:%@",token);
        
        NSString * tokenMD5 = [NStringExtension md5:token];
        const char * tokenBytes = [tokenMD5 UTF8String];
        const char * targetBytes = [sourceStr UTF8String];
        unsigned long targetLength = strlen(targetBytes);
        
        for (int i = 0; i < targetLength; i++) {
            unsigned char tmp = targetBytes[i] ^ tokenBytes[ i % strlen(tokenBytes) ];
            [hexResult appendFormat:@"%x",tmp];
            
            if (i < targetLength - 1 ) {
                [hexResult appendString:@"x"];
            }
        }
    }
    @catch(NSException* ex){        
        NSLog(@"At NStringExtension encrypt, reason: %@", [ex reason]);
    }
    
    return hexResult;
    
}


+ (NSString *)unencrypt:(NSString *)sourceStr
{
    // d0xe2xae
    
    NSArray * components = [sourceStr componentsSeparatedByString:@"x"];
    unsigned char * targetBytes = (unsigned char *) malloc([components count]);
    int i = 0;
    
    for ( NSString * component in components ) {
        int value = 0;
        sscanf([component cStringUsingEncoding:NSUTF8StringEncoding], "%x", &value);
        targetBytes[i++] = (unsigned char)value;
    }
    
    NSString * token = [NStringExtension md5:ENCRYPT_TOKEN];
    const char * tokenBytes = [token UTF8String];
    unsigned long targetLength = [components count];
    unsigned char * resultBuffer = (unsigned char *) malloc(targetLength+1);
    
    for (int i = 0; i < targetLength; i++) {
        resultBuffer[i] = targetBytes[i] ^ tokenBytes[ i % strlen(tokenBytes) ];
    }
    
    resultBuffer[targetLength] = '\0';
    
    NSString* result = [NSString stringWithUTF8String:(const char *)resultBuffer];
    
    free(targetBytes);
    free(resultBuffer);
    
    return result;
}

+ (NSString *)urlParametersStringFromDictionary:(NSDictionary *)info
{
    __block NSMutableString * body = [NSMutableString stringWithCapacity:100];
    [info enumerateKeysAndObjectsUsingBlock:^(NSString * key, id obj, BOOL *stop)
     {
         @autoreleasepool
         {
             NSString * value = [NSString stringWithFormat:@"%@",[info objectForKey:key]];
             if ([body length] > 0) {
                 [body appendString:@"&"];
             }
             [body appendFormat:@"%@=%@",[NStringExtension urlEncoding:key],[NStringExtension urlEncoding:value]];
         }
     }];
    
	return body;
}





+ (NSString*) urlEncoding:(NSString *)sourceStr
{
    NSString *result = (NSString *) CFURLCreateStringByAddingPercentEscapes(kCFAllocatorDefault, (CFStringRef)sourceStr, NULL, CFSTR(":/?#[]@!$&’()*+,;="), kCFStringEncodingUTF8);
    return [result autorelease];
}

@end
