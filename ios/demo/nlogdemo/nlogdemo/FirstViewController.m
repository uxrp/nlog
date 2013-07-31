//
//  FirstViewController.m
//  nlogdemo
//
//  Created by Miller on 13-7-16.
//  Copyright (c) 2013年 baidu. All rights reserved.
//

#import "FirstViewController.h"
#import <nlog/NLog.h>
#define CurrentTimeMillis  (long long)round ([[NSDate date] timeIntervalSince1970] * (double)1000)

@interface FirstViewController ()

@end

@implementation FirstViewController

//- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
//{
//    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
//    if (self) {
//        self.title = NSLocalizedString(@"First", @"First");
//        self.tabBarItem.image = [UIImage imageNamed:@"first"];
//    }
//    return self;
//}

- (id)init
{
    if (self = [super init])
    {
        self.title = NSLocalizedString(@"First", @"First");
        self.tabBarItem.image = [UIImage imageNamed:@"first"];

    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor colorWithRed:225.0 green:225.0 blue:225.0 alpha:0.9];
    
    UIButton* button = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [button setTitle:@"记录点击事件" forState:UIControlStateNormal];
    button.frame = CGRectMake((self.view.bounds.size.width-200) / 2.0, 200.0, 200, 50);
    [button addTarget:self action:@selector(button1clicked) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:button];
    
    UIButton* button2 = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [button2 setTitle:@"记录点击事件并立即发送" forState:UIControlStateNormal];
    button2.frame = CGRectMake((self.view.bounds.size.width-200) / 2.0, 300.0, 200, 50);
    [button2 addTarget:self action:@selector(button2clicked) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:button2];
    
    
	// Do any additional setup after loading the view, typically from a nib.
}

- (void)button1clicked
{
    [NLog sendEvent:@"ui" action:@"click" label:@"button1" value:nil];
}

- (void)button2clicked
{
    [NLog send:@"2"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"ui", @"category",
                @"click", @"click",
                @"button2", @"label"
                , nil]
   immediately:YES];
    
    // 性能测试：NSUserDefaults单次读写时间在 0.6ms左右，批量的读写还是比较耗时间的，可以考虑做缓存
    /*
    int i = 500 * 8;
    double start = CurrentTimeMillis;
    while (i>0) {
        NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
        
        NSString* test = [defaults objectForKey:@"test"];
        [defaults setObject:[NSString stringWithFormat:@"%@\ntest",test] forKey:@"test"];
        [defaults synchronize];
        [NLog sendEvent:@"ui" action:@"click" label:@"button1" value:nil];
        [NLog sendException:@"hankException" isFatal:YES
                     params:[NSDictionary dictionaryWithObjectsAndKeys:
                     @"hank_ex_a",@"aaaa",
                     @"hank_ex_b",@"bbbb",
                     @"hank_ex_c",@"cccc",
                     nil]];
        
        [NLog sendException:@"hankException2"
                    isFatal:NO
                     params:[NSDictionary dictionaryWithObjectsAndKeys:
                                            @"hank_ex_2",@"2222",
                                            nil]];
        
        [NLog send:@"2"
            params:[NSDictionary dictionaryWithObjectsAndKeys:
                    @"ui", @"category",
                    @"click", @"click",
                    @"button2", @"label"
                    , nil]
       immediately:NO];
        i--;
    }
    
    double end = CurrentTimeMillis;
    
    NSLog(@"duration:%f", end - start);
    */
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    [NLog logDurationStart:@"firstview"];
}

- (void)viewDidDisappear:(BOOL)animated{
    [super viewDidDisappear:animated];
    [NLog logDurationEnd:@"firstview"];
}

@end
