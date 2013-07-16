//
//  NSender.h
//  NLog
//
//  Created by Miller on 13-7-11.
//
//

#import <Foundation/Foundation.h>

@interface NSender : NSObject{
    NSTimer* sendTimer;
}

+ (id)sharedInstance;

@end
