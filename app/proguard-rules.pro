# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file names for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# NewPipeExtractor reflection & internal parsers
-keep class org.schabi.newpipe.extractor.** { *; }
-keep interface org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Rhino JavaScript engine used by NewPipeExtractor
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# JSON & HTML parsers
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# PureTube Models (prevent field stripping for JSON/Room/Extractor)
-keep class com.dr.tech.puretube.core.data.model.** { *; }
-keep class com.dr.tech.puretube.core.database.entity.** { *; }
-keep class com.dr.tech.puretube.core.extractor.pack.** { *; }