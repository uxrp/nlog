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

@implementation NStringExtension

+ (NSString *) md5:(NSString *)sourceStr
{
    const char* str = [sourceStr UTF8String];
    unsigned char result[16];
    CC_MD5(str, strlen(str), result);
    return [NSString stringWithFormat:@"%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",            result[0], result[1], result[2], result[3], result[4], result[5], result[6], result[7],            result[8], result[9], result[10], result[11], result[12], result[13], result[14], result[15]            ];
}

+ (NSString *)encrypt:(NSString *)sourceStr
{
    NSString * token = [NStringExtension md5:@"5D97EEF8-3127-4859-2222-82E6C8FABD8A"];
    const char * tokenBytes = [token UTF8String];
    const char * targetBytes = [sourceStr UTF8String];
    int targetLength = strlen(targetBytes);
    unsigned char *resultBuffer = malloc(targetLength);
    
    for (int i = 0; i < targetLength; i++) {
        resultBuffer[i] = targetBytes[i] ^ tokenBytes[ i % strlen(tokenBytes)];
    }
    
    //    NSString * resultString = [NSString stringWithCString:resultBuffer encoding:UTF]
    
    //    NSData * data = [NSData dataWithBytesNoCopy:resultBuffer length:targetLength];
    //    NSString * result = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    
    //    return [NSString stringWithUTF8String:(const char *)resultBuffer];
    return [[NSString alloc] initWithBytesNoCopy:resultBuffer length:targetLength encoding:NSASCIIStringEncoding freeWhenDone:NO];
}


+ (NSString *)unencrypt:(NSString *)sourceStr
{
    return [NStringExtension encrypt:sourceStr];
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