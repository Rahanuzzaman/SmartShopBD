# Smart Shop BD V5 - Mobile APK Build Package

This package is prepared for building the Android APK with a cloud CI runner, so a PC is not required for the user.

## Build result
The GitHub Actions workflow builds `app-debug.apk` and publishes it as a downloadable workflow artifact.

## Project
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Jetpack Compose
- Room 2.6.1
- compileSdk 35 / targetSdk 35 / minSdk 24

## Important
This is a debug build workflow. A production release should use a private signing key and a signed release configuration. Do not put keystore passwords or private keys in source control.
