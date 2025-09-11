# 16KB Page Size Compatibility Fix for Android 16+

## Overview
This document summarizes the comprehensive changes made to ensure QRScanner app compatibility with 16KB page sizes required for Android 16+ devices.

## Problem
The app was showing compatibility errors on Pixel 8a running Android 16:
- `libimage_processing_util_jni.so` not aligned at 16KB boundaries
- Other ML Kit native libraries causing alignment issues
- Google Play Store requirement for 16KB support starting November 2025

## Solution Summary

### 1. Build Configuration (app/build.gradle)
```gradle
packagingOptions {
    jniLibs {
        useLegacyPackaging = false  // Changed from true
        keepDebugSymbols += "**/*.so"
    }
    doNotStrip "**/*.so"
    pickFirst "**/*.so"
}

splits {
    abi {
        enable false
    }
}
```

### 2. Android Manifest (AndroidManifest.xml)
```xml
android:extractNativeLibs="false"  // Changed from true
```

### 3. Gradle Properties (gradle.properties)
```properties
android.experimental.enable16KPageSizes=true
android.enableR8.fullMode=false
android.useDeprecatedNdk=false
```
Note: `android.bundle.enableUncompressedNativeLibs` was removed as it's deprecated in AGP 8.1+

### 4. ProGuard Rules (proguard-rules.pro)
Added rules to preserve ML Kit native methods:
```
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.mlkit.common.** { *; }
```

### 5. SDK and Dependency Updates
- Compile/Target SDK: 35
- Android Gradle Plugin: 8.13.0 (updated from 8.7.2)
- ML Kit Barcode Scanning: 17.3.0
- ML Kit Text Recognition: 16.0.1
- CameraX: 1.3.4

## Key Changes Explained

1. **extractNativeLibs=false**: This ensures native libraries remain uncompressed in the APK, allowing proper 16KB alignment.

2. **useLegacyPackaging=false**: Forces modern packaging that respects alignment requirements.

3. **pickFirst "**/*.so"**: Handles duplicate native libraries by picking the first occurrence.

4. **doNotStrip**: Prevents stripping of native libraries which can affect alignment.

5. **enable16KPageSizes=true**: Explicitly enables 16KB page size support.

## Version Information
- Version: 2.4.6 (Build 29)
- Tested on: Pixel 8a with Android 16

## Testing Instructions
1. Clean and rebuild the project
2. Generate a debug APK
3. Install on Android 16 device
4. Verify no 16KB compatibility warnings

## References
- [Android 16KB Page Size Documentation](https://developer.android.com/16kb-page-size)
- [ML Kit Migration Guide](https://developers.google.com/ml-kit/migration)
