//
//  AppDelegate.m
//  nlogdemo
//
//  Created by Miller on 13-7-16.
//  Copyright (c) 2013年 baidu. All rights reserved.
//

#import "AppDelegate.h"

#import "FirstViewController.h"

#import "SecondViewController.h"

#import <nlog/NLog.h>


@implementation AppDelegate

- (void)dealloc
{
    [_window release];
    [_tabBarController release];
    [super dealloc];
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]] autorelease];
    // Override point for customization after application launch.
    UIViewController *viewController1, *viewController2;
    if ([[UIDevice currentDevice] userInterfaceIdiom] == UIUserInterfaceIdiomPhone) {
        viewController1 = [[[FirstViewController alloc] init] autorelease];
        viewController2 = [[[SecondViewController alloc] initWithNibName:@"SecondViewController_iPhone" bundle:nil] autorelease];
    } else {
        viewController1 = [[[FirstViewController alloc] init] autorelease];
        viewController2 = [[[SecondViewController alloc] initWithNibName:@"SecondViewController_iPad" bundle:nil] autorelease];
    }
    self.tabBarController = [[[UITabBarController alloc] init] autorelease];
    self.tabBarController.viewControllers = @[viewController1, viewController2];
    self.window.rootViewController = self.tabBarController;
    [self.window makeKeyAndVisible];
    
    [NLog startWithAppId:@"9"];
    
//    [[NLog getTracker:@"wenku"] setSampleRate:0.05];
    
    [NLog set:@"protocolParameter"
          val:[NSDictionary dictionaryWithObjectsAndKeys:
               @"protocolkey", @"originalkey",
               @"ea",@"eventAction"
               , nil]];
    
    [NLog set:@"paid" val:@"11111111" isMutable:YES];
    /*
    [NLog set:@"c" val:@"TODO"];
    
    [NLog set:@"av" val:@"TODO"];
    
    [NLog set:@"i" val:@"TODO"];
    */
    
//    [NLog set:@"test" val: [NSNumber numberWithInt:1]];
    
//    [NLog set:@"test中文" val: @"中文"];
    
    [NLog send:@"millertype"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"miller-framework-value",@"nlogframework",
                nil]];
    
    [NLog send:@"test protocol"
        params:[NSDictionary dictionaryWithObjectsAndKeys:
                @"originalval",@"originalkey",
                @"中文VAL",@"中文KEY",
                nil]];
    
    /*
    id var = 1;
    
    NSLog(@"%@", [var class]);
    */
    NSString* str1 = @"1.7.5";
    NSString* str2 = @"0.8.1";
    
    NSComparisonResult res = [str1 compare:str2];
    
    switch (res) {
        case NSOrderedAscending:
            // going up
            NSLog(@"NSOrderedAscending");
            break;
        case NSOrderedSame:
            // even steven
            NSLog(@"NSOrderedSame");
            break;
        case NSOrderedDescending:
            NSLog(@"NSOrderedDescending");
            // down i go
            break;
        default:
            break;
    }
    
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

/*
// Optional UITabBarControllerDelegate method.
- (void)tabBarController:(UITabBarController *)tabBarController didSelectViewController:(UIViewController *)viewController
{
}
*/

/*
// Optional UITabBarControllerDelegate method.
- (void)tabBarController:(UITabBarController *)tabBarController didEndCustomizingViewControllers:(NSArray *)viewControllers changed:(BOOL)changed
{
}
*/

@end
