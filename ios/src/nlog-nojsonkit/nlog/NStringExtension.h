//
//  NSStringExtension.h
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-15.
//
//

#import <Foundation/Foundation.h>

@interface NStringExtension : NSObject

+ (NSString *) md5:(NSString *)sourceStr;
+ (NSString *)encrypt:(NSString *)sourceStr;
+ (NSString *)unencrypt:(NSString *)sourceStr;
+ (NSString*) urlEncoding:(NSString *)sourceStr;
+ (NSString *)urlParametersStringFromDictionary:(NSDictionary *)info;
+ (NSString *) getMacAddress;




@end
