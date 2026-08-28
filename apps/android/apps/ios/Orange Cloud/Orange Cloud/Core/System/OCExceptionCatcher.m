//
//  OCExceptionCatcher.m
//  Orange Cloud
//

#import "OCExceptionCatcher.h"

NSException * _Nullable OCCatchException(void (NS_NOESCAPE ^block)(void)) {
    @try {
        block();
        return nil;
    } @catch (NSException *exception) {
        return exception;
    }
}
