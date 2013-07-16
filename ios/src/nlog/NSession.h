//
//  NSession.h
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-9.
//
//

#import <Foundation/Foundation.h>

@interface NSession : NSObject{
    long long   _start;
    long long   _end;
    NSString*   _sessionId;
    int         _seq;
}

+ (id)sharedInstance;

/**
 * 调用pause方法只将当前时间记录到_end，用于判断session是否过期
 * 如果过期则应该调用reset方法更换id和_start，如果未过期则无需其他操作
 */

@property (nonatomic,copy,readonly)   NSString * sessionId;
@property (nonatomic,assign,readonly) int        seq;

- (void)pause;

- (void)reset;

- (NSDictionary *)dataInDict;

//- (NSString *)getId;
//
//- (int)getSeq;

- (NSString *)generateId;

+ (void)pause;

+ (void)reset;

+ (NSDictionary *)dataInDict;

+ (NSString *)getId;

+ (int)getSeq;

@end
