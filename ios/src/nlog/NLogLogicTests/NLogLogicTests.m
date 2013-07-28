//
//  NLogLogicTests.m
//  NLogLogicTests
//
//  Created by Miller on 13-7-22.
//  Copyright (c) 2013年 baidu. All rights reserved.
//

#import "NLogLogicTests.h"
#import <nlog/NLog.h>
#import <nlog/NTracker.h>
#import <nlog/NStringExtension.h>

@implementation NLogLogicTests

- (void)setUp
{
    [super setUp];
    
    // Set-up code here.
}

- (void)tearDown
{
    // Tear-down code here.
    
    [super tearDown];
}

- (void)testEncrypt{
//    STFail(@"Unit tests are not implemented yet in NLogLogicTests");
    
    // 可见单字节字符
    
    NSString* astr1 = @"aA~!@#$%^&*()_+`1-=[]{}\\|,.<>/?;:'\"`~\t\n\r";
    NSString* bstr1 = [NStringExtension encrypt:astr1];
    NSString* cstr1 = [NStringExtension unencrypt:bstr1];
    
    STAssertTrue(![astr1 isEqualToString:bstr1], @"Encypt failed.");
    STAssertTrue([astr1 isEqualToString:cstr1], @"The string is not equal after encypt and unencrypt.");
    
    // 可见双字节字符
    NSString* astr2 = @"中文，。、？《》‘“”’；：【】￥·";
    
    // NSString* astr2 = @"】￥";
    NSString* bstr2 = [NStringExtension encrypt:astr2];
    NSString* cstr2 = [NStringExtension unencrypt:bstr2];
    
    STAssertTrue(![astr2 isEqualToString:bstr2], @"Encypt failed.");
    STAssertTrue([astr2 isEqualToString:cstr2], @"The string is not equal after encypt and unencrypt.");
    
    // 混合
    NSString* astr3 = @"中文，。、？《》aA~!@#$%^&*()_+`1-=[]{}\\|,.<>/?;:'\"`~\t\n\r‘“”’；：【】￥·";
    
    NSString* bstr3 = [NStringExtension encrypt:astr3];
    NSString* cstr3 = [NStringExtension unencrypt:bstr3];
    
    STAssertTrue(![astr3 isEqualToString:bstr3], @"Encypt failed.");
    STAssertTrue([astr3 isEqualToString:cstr3], @"The string is not equal after encypt and unencrypt.");
    
}

@end
