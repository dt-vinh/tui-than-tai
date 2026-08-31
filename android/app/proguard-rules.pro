# JNI entry points are resolved by their Java names at runtime.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# DJL loads these bridge classes through its native tokenizer runtime.
-keep class ai.djl.huggingface.tokenizers.jni.** { *; }
