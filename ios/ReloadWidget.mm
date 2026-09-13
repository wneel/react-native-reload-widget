// The interface lives here rather than in a public header on purpose.
//
// This pod is mixed Swift + Objective-C++, so CocoaPods builds a clang module for it and
// generates the umbrella header from the pod's *public* headers. That umbrella is parsed as
// Objective-C, while the codegen spec header below pulls in the C++ standard library, so any
// public header reaching the spec fails the module build with "'utility' file not found".
// Keeping the interface in the .mm, and marking ios/generated private in the podspec, keeps
// every C++ include out of the umbrella.

#ifdef RCT_NEW_ARCH_ENABLED
#import "RNReloadWidgetSpec.h"
#else
#import <React/RCTBridgeModule.h>
#endif

// CocoaPods derives the Swift module name from the pod name, so the dashes become underscores.
// The bracketed form is the one that resolves under use_frameworks!, the quoted form the one
// that resolves without it.
#if __has_include(<react_native_reload_widget/react_native_reload_widget-Swift.h>)
#import <react_native_reload_widget/react_native_reload_widget-Swift.h>
#else
#import "react_native_reload_widget-Swift.h"
#endif

#ifdef RCT_NEW_ARCH_ENABLED
@interface ReloadWidget : NSObject <NativeReloadWidgetSpec>
#else
@interface ReloadWidget : NSObject <RCTBridgeModule>
#endif
@end

@implementation ReloadWidget {
	ReloadWidgetImpl *_impl;
}

RCT_EXPORT_MODULE()

- (instancetype)init
{
	if (self = [super init]) {
		_impl = [ReloadWidgetImpl new];
	}
	return (self);
}

+ (BOOL)requiresMainQueueSetup
{
	return (NO);
}

RCT_EXPORT_METHOD(reloadAllWidgets:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
	resolve(@([_impl reloadAllWidgets]));
}

RCT_EXPORT_METHOD(reloadWidget:(NSString *)kind
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
	resolve(@([_impl reloadWidget:kind]));
}

RCT_EXPORT_METHOD(getInstalledWidgets:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
	[_impl getInstalledWidgets:^(NSArray<NSDictionary<NSString *, id> *> *widgets) {
		resolve(widgets);
	}];
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
	(const facebook::react::ObjCTurboModule::InitParams &)params
{
	return std::make_shared<facebook::react::NativeReloadWidgetSpecJSI>(params);
}
#endif

@end
