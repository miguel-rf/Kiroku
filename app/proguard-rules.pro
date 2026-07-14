# Retrofit reflects on service interfaces and kotlinx.serialization generates
# serializers referenced directly by those interfaces. Their consumer rules
# cover the required metadata. Keep only Kiroku's navigation keys explicitly.
-keepclassmembers class com.kiroku.app.** implements androidx.navigation3.runtime.NavKey {
    <fields>;
}

