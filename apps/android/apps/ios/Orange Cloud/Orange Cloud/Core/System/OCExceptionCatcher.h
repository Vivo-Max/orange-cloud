//
//  OCExceptionCatcher.h
//  Orange Cloud
//
//  ObjC 异常捕获垫片。SwiftData/CoreData 在个别设备（TF 崩溃点 D8tiH4pqdctLgx_nCLGnZ，
//  iOS 17.0 单机，疑本机缓存库损坏）会在 fetch 时抛 ObjC NSException——Swift 的
//  try?/do-catch 只接 Swift Error，接不住 NSException，直接 SIGABRT。
//  唯一可靠的拦截点是 ObjC 层 @try/@catch，故有此文件（经 bridging header 暴露给 Swift）。
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// 同步执行 block；若抛 ObjC 异常则捕获并返回该异常，正常完成返回 nil。
FOUNDATION_EXPORT NSException * _Nullable OCCatchException(void (NS_NOESCAPE ^block)(void));

NS_ASSUME_NONNULL_END
