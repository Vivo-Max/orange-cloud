fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android listings

```sh
[bundle exec] fastlane android listings
```

只推商店文案 + 图标/特征图/截图。不碰版本说明、不碰轨道、不传二进制（validate:true 可干跑）

### android release_notes

```sh
[bundle exec] fastlane android release_notes
```

把 changelogs/<versionCode>.txt 挂到指定轨道的 release。默认 production + draft（不会自动上线）

### android metadata_check

```sh
[bundle exec] fastlane android metadata_check
```

干跑：商店文案 + 版本说明全量送 Play 校验但不落库（validate_only）

### android deploy

```sh
[bundle exec] fastlane android deploy
```

上传 AAB + 元数据到指定轨道。track: internal(默认)/alpha(封闭测试)/beta/production；images:true 才连图一起传

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
