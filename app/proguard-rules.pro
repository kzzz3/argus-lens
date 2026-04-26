# Preserve enough metadata for Room, Retrofit, Gson, Hilt, and coroutine stack traces
# while still allowing R8 to shrink and optimize release builds.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit service interfaces are invoked reflectively through generated proxies.
-keep interface com.kzzz3.argus.lens.data.**.*ApiService { *; }

# Keep model field names used by Gson at app/backend JSON boundaries.
-keepclassmembers class com.kzzz3.argus.lens.data.**.*Api* { <fields>; }
-keepclassmembers class com.kzzz3.argus.lens.data.**.*Request* { <fields>; }
-keepclassmembers class com.kzzz3.argus.lens.data.**.*Response* { <fields>; }
-keepclassmembers class com.kzzz3.argus.lens.data.**.*Dto* { <fields>; }
-keepclassmembers class com.kzzz3.argus.lens.data.**.*Body { <fields>; }

# Room entities and relation containers are schema boundaries.
-keep class com.kzzz3.argus.lens.data.local.*Entity { *; }
-keep class com.kzzz3.argus.lens.data.local.*With* { *; }
