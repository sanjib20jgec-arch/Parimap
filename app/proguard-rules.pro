# Add project specific ProGuard rules here.
# Keep ARCore and SceneView classes
-keep class com.google.ar.** { *; }
-keep class io.github.sceneview.** { *; }
-keep class org.opencv.** { *; }

# Keep Gson classes for MeasurementData serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.armeasure.app.MeasurementData { *; }
