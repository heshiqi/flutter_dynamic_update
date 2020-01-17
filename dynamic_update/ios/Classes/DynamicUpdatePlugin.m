#import "DynamicUpdatePlugin.h"
#if __has_include(<dynamic_update/dynamic_update-Swift.h>)
#import <dynamic_update/dynamic_update-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "dynamic_update-Swift.h"
#endif

@implementation DynamicUpdatePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftDynamicUpdatePlugin registerWithRegistrar:registrar];
}
@end
