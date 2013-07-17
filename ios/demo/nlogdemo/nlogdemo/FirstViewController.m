//
//  FirstViewController.m
//  nlogdemo
//
//  Created by Miller on 13-7-16.
//  Copyright (c) 2013年 baidu. All rights reserved.
//

#import "FirstViewController.h"
#import <nlog/NLog.h>

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
    [NLog send:@"event"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"ui", @"category",
                @"click", @"click",
                @"button2", @"label"
                , nil]
   immediately:YES];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
