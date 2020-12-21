//
//  FLTWKConfigurationManager.m
//  webview_flutter
//
//  Created by Tomasz Rejdych on 21/12/2020.
//

#import "FLTWKConfigurationManager.h"
#import <WebKit/WebKit.h>

@implementation FLTWKConfigurationManager
+ (FLTWKConfigurationManager *)shared {
    static FLTWKConfigurationManager *manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[self alloc] init];
    });
    return manager;
}

- (FLTWKConfigurationManager *)init {
    if (self = [super init]) {
        self.processPool = [[WKProcessPool alloc] init];
    }
    return self;
  }

@end
