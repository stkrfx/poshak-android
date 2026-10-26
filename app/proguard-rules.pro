# WorkManager creates its generated Room database through reflection during
# AndroidX Startup initialization. Keep the no-argument constructor explicitly:
# R8 full mode may otherwise remove it and crash the app before MainActivity opens.
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }

# WorkManager instantiates this default merger reflectively for worker inputs.
-keep class androidx.work.OverwritingInputMerger { <init>(); }
