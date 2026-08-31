# DJL Tokenizer Native 16 KB Build

`tokenizer-native-0.33.0-16k.aar` is a locally rebuilt copy of DJL's Android
tokenizer native package. It keeps the Java/JNI contract from DJL `v0.33.0`
while rebuilding every Android ABI with 16 KB ELF load alignment.

- Upstream: https://github.com/deepjavalibrary/djl
- Source tag: `v0.33.0`
- Source commit: `39f5fa8b2e4e362613379caf8e6715a08ea93cac`
- Native toolchain: Android NDK `28.2.13676358` (`r28c`)
- AAR SHA-256: `225fcfcf5463388e99182256cdd0b2fa6a6be6eeb4d217051655179f16e469f0`
- Rust targets: `aarch64-linux-android`, `armv7-linux-androideabi`,
  `x86_64-linux-android`, `i686-linux-android`
- License: Apache License 2.0, matching the upstream DJL project

Regenerate the AAR with `scripts/build-djl-tokenizer-16k.ps1`. The script
checks every `PT_LOAD` alignment before it copies the artifact into this
directory.
