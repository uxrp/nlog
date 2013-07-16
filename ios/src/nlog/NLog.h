//
//  NLog.h
//  NLog
//
//  Created by Miller( https://github.com/miller ) on 13-7-8.
//  Copyright (c) 2013年 Baidu. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "NTracker.h"


@interface NLog : NSObject

/**
 * 初始化统计框架
 * appid    当前APP的标识
 */
+ (void)startWithAppId:(NSString *) appid;

/**
 * 初始化统计框架
 * appid    当前APP的标识 
 * configs  默认配置
 */
+ (void)startWithAppId:(NSString *) appid configs:(NSDictionary *)configs;

+ (id)sharedInstance;

/**
 * 获取tracker实例
 */
+ (NTracker *)getTracker:(NSString *) trackerId;

/**
 * 记录日志
 * hitType 日志类型(event/appview/timing/exception)
 * params 日志数据
 */
+ (void)send: (NSString*)hitType params: (NSDictionary *) params;

/**
 * 记录日志
 * hitType 日志类型(event/appview/timing/exception)
 * params 日志数据
 * immediately 是否立即发送到服务器
 */
+ (void)send: (NSString*)hitType params: (NSDictionary *) params immediately: (BOOL) now;

/**
 * 记录视图类日志
 * viewName 视图名称
 * params   数据
 */
+ (void)sendView: (NSString *)viewName params: (NSDictionary *)params;

/**
 * 记录事件类日志
 * category 事件类别
 * action   事件动作
 * label    事件标签
 * value    事件关联数据
 */
+ (void)sendEvent: (NSString *)category
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
+ (void)sendTiming: (NSString *)category
          interval: (NSTimeInterval *)interval
              name: (NSString *)name
             label: (NSString *)label;

/**
 * 记录时间类日志
 * category 事件类别
 * interval 时长(单位：秒)
 * name     事件名称
 * label    事件标签
 */
+ (void)sendException: (NSString *)description
              isFatal: (Boolean) isFatal
               params: (NSDictionary *)params;

/**
 * 设置公共数据
 */
+ (void)set: (NSString *)key val:(id) val;

@end
