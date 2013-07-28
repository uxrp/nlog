//
//  NTracker.h
//  BaiduMobStatSample
//
//  Created by Miller on 13-7-8.
//
//

#import <Foundation/Foundation.h>

@interface NTracker : NSObject{
    NSString* name;
    Boolean running;
    Boolean isHit;
    Boolean needCalc;
    double sampleRate;
    NSMutableDictionary* fields;
    NSMutableDictionary* durations;
    NSDictionary* fieldsProtocol;
}

@property (nonatomic,retain) NSString* name;

+ (NTracker*)getTracker:(NSString *)trackerId;

/**
 * 开始记录日志（新建tracker会自动开始）
 */
- (void)start;

/**
 * 停止记录日志
 */
- (void)stop;

/**
 * 记录日志
 * hitType 日志类型(event/appview/timing/exception)
 */
- (void)send: (NSString *)hitType params: (NSDictionary *) params;

/**
 * 记录日志
 * hitType 日志类型(event/appview/timing/exception)
 * params 日志数据
 * immediately 是否立即发送到服务器
 */
- (void)send: (NSString*)hitType
      params: (NSDictionary *) params
 immediately: (BOOL) now;

/**
 * 记录视图类日志
 * viewName 视图名称
 * params   数据
 */
- (void)sendView: (NSString *)viewName params: (NSDictionary *)params;

/**
 * 记录事件类日志
 * category 事件类别
 * action   事件动作
 * label    事件标签
 * value    事件关联数据
 */
- (void)sendEvent: (NSString *)category
           action: (NSString *)action
            label: (NSString *)label
            value: (NSNumber *)value;

/**
 * 记录时间类日志
 * category 事件类别
 * interval 时长(单位：秒)
 * name     事件名称
 * label    事件标签
 */
- (void)sendTiming: (NSString *)category
          interval: (NSTimeInterval)interval
              name: (NSString *)name
             label: (NSString *)label;

/**
 * 记录时间类日志
 * category 事件类别
 * interval 时长(单位：秒)
 * name     事件名称
 * label    事件标签
 */
- (void)sendException: (NSString *)description
              isFatal: (Boolean) isFatal
               params: (NSDictionary *)params;

/**
 * 设置采样率
 * rate 采样率(0 - 1)
 */
- (void)setSampleRate: (double)rate;

/**
 * 设置公共数据(每个日志请求都将携带该数据)
 */
- (void)set:(NSDictionary *)params;

- (void)set:(NSString *)key value:(id)val;

/**
 * 记录时长
 */
- (void)logDurationStart:(NSString *)label;
- (void)logDurationEnd:(NSString *)label;

@end
