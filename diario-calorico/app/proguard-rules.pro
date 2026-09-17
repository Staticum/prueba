# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.staticum.diariocalorico.network.** { *; }
-keep class com.staticum.diariocalorico.update.** { *; }

# Tink (usado por androidx.security-crypto) referencia anotaciones de
# error-prone que son solo para compilación, no están en runtime ni se
# necesitan: se le indica a R8 que las ignore en vez de fallar por ausencia.
-dontwarn com.google.errorprone.annotations.**
