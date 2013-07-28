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

#define ENCRYPT_TOKEN @"5D97EEF8-3127-4859-2222-82E6C8FABD8A"

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
        
        NSString * token = [NStringExtension md5:ENCRYPT_TOKEN];
        const char * tokenBytes = [token UTF8String];
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
    
    return [NSString stringWithUTF8String:(const char *)resultBuffer];
}

+ (NSString *) getMacAddress
{
    int                 mgmtInfoBase[6];
    char                *msgBuffer = NULL;
    size_t              length;
    unsigned char       macAddress[6];
    struct if_msghdr    *interfaceMsgStruct;
    struct sockaddr_dl  *socketStruct;
    NSString            *errorFlag = NULL;
    
    // Setup the management Information Base (mib)
    mgmtInfoBase[0] = CTL_NET;        // Request network subsystem
    mgmtInfoBase[1] = AF_ROUTE;       // Routing table info
    mgmtInfoBase[2] = 0;
    mgmtInfoBase[3] = AF_LINK;        // Request link layer information
    mgmtInfoBase[4] = NET_RT_IFLIST;  // Request all configured interfaces
    
    // With all configured interfaces requested, get handle index
    if ((mgmtInfoBase[5] = if_nametoindex("en0")) == 0)
        errorFlag = @"if_nametoindex failure";
    else
    {
        // Get the size of the data available (store in len)
        if (sysctl(mgmtInfoBase, 6, NULL, &length, NULL, 0) < 0)
            errorFlag = @"sysctl mgmtInfoBase failure";
        else
        {
            // Alloc memory based on above call
            if ((msgBuffer = malloc(length)) == NULL)
                errorFlag = @"buffer allocation failure";
            else
            {
                // Get system information, store in buffer
                if (sysctl(mgmtInfoBase, 6, msgBuffer, &length, NULL, 0) < 0)
                    errorFlag = @"sysctl msgBuffer failure";
            }
        }
    }
    
    // Befor going any further...
    if (errorFlag != NULL)
    {
        //        NSLog(@"Error: %@", errorFlag);
        return errorFlag;
    }
    
    // Map msgbuffer to interface message structure
    interfaceMsgStruct = (struct if_msghdr *) msgBuffer;
    
    // Map to link-level socket structure
    socketStruct = (struct sockaddr_dl *) (interfaceMsgStruct + 1);
    
    // Copy link layer address data in socket structure to an array
    memcpy(&macAddress, socketStruct->sdl_data + socketStruct->sdl_nlen, 6);
    
    // Read from char array into a string object, into traditional Mac address format
    NSString *macAddressString = [NSString stringWithFormat:@"%x:%x:%x:%x:%x:%x",
                                  macAddress[0], macAddress[1], macAddress[2],
                                  macAddress[3], macAddress[4], macAddress[5]];
    //    NSLog(@"Mac Address: %@", macAddressString);
    
    // Release the buffer memory
    free(msgBuffer);
    
    return macAddressString;
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
