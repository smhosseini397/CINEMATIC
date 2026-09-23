import JSZip from 'jszip';
import { ANDROID_FILES } from '../data/androidFiles';

export async function generateAndroidProjectZip(): Promise<Blob> {
  const zip = new JSZip();

  // Root wrapper files
  const gradlewBash = `#!/bin/sh
# Gradle start up script for POSIX systems
exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
`;
  const gradlewBat = `@rem Gradle start up script for Windows
@rem Execute gradle-wrapper.jar
java -jar gradle\\wrapper\\gradle-wrapper.jar %*
`;

  const wrapperProps = `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
`;

  const rootSettings = `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "CinematicPhotoAnimator"
include(":app")
`;

  const rootBuild = `plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
`;

  const gradleProps = `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
`;

  zip.file('gradlew', gradlewBash, { unixPermissions: '755' });
  zip.file('gradlew.bat', gradlewBat);
  zip.file('settings.gradle.kts', rootSettings);
  zip.file('build.gradle.kts', rootBuild);
  zip.file('gradle.properties', gradleProps);

  zip.file('gradle/wrapper/gradle-wrapper.properties', wrapperProps);

  // Fetch real binary jar
  try {
    const jarResponse = await fetch('/gradle-wrapper.jar');
    if (jarResponse.ok) {
      const jarBlob = await jarResponse.blob();
      zip.file('gradle/wrapper/gradle-wrapper.jar', jarBlob);
    }
  } catch (err) {
    console.warn('Could not fetch wrapper jar, falling back', err);
  }

  // App files
  for (const file of ANDROID_FILES) {
    // Strip "android/" prefix for standalone project root
    const cleanPath = file.path.replace(/^android\//, '');
    zip.file(cleanPath, file.content);
  }

  // Add standard resource files
  zip.file('app/src/main/res/values/strings.xml', `<resources>
    <string name="app_name">Cinematic Photo Animator</string>
    <string name="persian_carpet_mode">Persian Carpet Luxury Showcase</string>
</resources>`);

  zip.file('app/src/main/res/values/colors.xml', `<resources>
    <color name="obsidian_black">#0D0E12</color>
    <color name="luxury_gold">#D4AF37</color>
</resources>`);

  zip.file('app/src/main/res/xml/file_paths.xml', `<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-files-path name="cinematic_videos" path="CinematicVideos/" />
</paths>`);

  zip.file('README.md', `# Cinematic Photo Animator (Native Android)

A complete native Android application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **MediaCodec hardware video encoding**.

## Requirements
- Android Studio Iguana / Jellyfish or newer
- JDK 17
- Minimum Android 8.0 (API 26)
- Target Android 14 (API 34)

## Building the APK
To assemble the debug APK:
\`\`\`bash
./gradlew assembleDebug
\`\`\`

The generated APK will be in:
\`app/build/outputs/apk/debug/app-debug.apk\`

## Core Architecture
- **Clean Architecture**: MVVM, Repository pattern, Kotlin Coroutines, StateFlow
- **Encoder**: MediaCodec H.264 High Profile Surface input at 60 FPS
- **Motion Engine**: Ken Burns, 3D Parallax, Cinematic Zooms, Handheld Drift
- **Persian Carpet Showcase**: Macro waypoints (Medallion, Spandrel, Border, Pile, Fringe) with fiber lighting sweep and 100% color-true preservation
`);

  return zip.generateAsync({ type: 'blob' });
}
