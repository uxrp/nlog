//
//  NStorage.h
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-10.
//
//

#import <Foundation/Foundation.h>

@interface NStorage : NSObject

- (void) cacheLogData:(NSNotification *)notification;

+ (id)sharedInstance;

@end
