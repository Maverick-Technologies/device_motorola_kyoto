# Experiment: Launcher3 QuickStep instead of Pixel Launcher

EvoX 10 ships Google's prebuilt Pixel Launcher (`NexusLauncherRelease`), which overrides
`Launcher3QuickStep` and has the Android 15 "Bubble" app shortcut compiled out. To get
"Bubble" in the app long-press menu (with `experiments/flags/bubble-anything`), kyoto builds
Launcher3 QuickStep (`TrebuchetQuickStep`) from source instead. Pixel Launcher features
(At a Glance, Google search bar, Google feed) are not available.

Patches, applied by `patches/apply.sh device/motorola/kyoto/experiments/launcher3`:
- `vendor/gms`: drop `NexusLauncherRelease` from the GMS package lists
- `vendor/pixel-style`: `config_recentsComponentName`, `config_secondaryHomePackage` and the
  setup wizard's gesture-onboarding action point at `com.android.launcher3`

Skip with `LAUNCHER3=off` (takes effect on the next `repo sync`, which drops applied patches).
