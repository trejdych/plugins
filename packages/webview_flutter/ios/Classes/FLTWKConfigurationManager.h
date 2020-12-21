//
//  FLTWKConfigurationManager.h
//  webview_flutter
//
//  Created by Tomasz Rejdych on 21/12/2020.
//

#import <Foundation/Foundation.h>
#import <WebKit/WebKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface FLTWKConfigurationManager : NSObject
+ (FLTWKConfigurationManager *)shared;

@property (nonatomic, strong) WKProcessPool *processPool;
@end

NS_ASSUME_NONNULL_END
