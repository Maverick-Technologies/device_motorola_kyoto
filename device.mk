#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

LOCAL_PATH := device/motorola/kyoto

# Launch / API levels (kyoto shipped Android 11)
PRODUCT_SHIPPING_API_LEVEL := 30
PRODUCT_TARGET_VNDK_VERSION := 31
PRODUCT_EXTRA_VNDK_VERSIONS := 31

# Dynamic partitions
PRODUCT_USE_DYNAMIC_PARTITIONS := true
PRODUCT_BUILD_SUPER_PARTITION := false

# A/B
AB_OTA_POSTINSTALL_CONFIG += \
    RUN_POSTINSTALL_system=true \
    POSTINSTALL_PATH_system=system/bin/otapreopt_script \
    FILESYSTEM_TYPE_system=ext4 \
    POSTINSTALL_OPTIONAL_system=true

PRODUCT_PACKAGES += \
    checkpoint_gc \
    otapreopt_script \
    update_engine \
    update_engine_sideload \
    update_verifier

# fastbootd (needed to flash logical partitions)
PRODUCT_PACKAGES += \
    android.hardware.fastboot@1.1-impl-mock \
    fastbootd

# First stage mount
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/fstab.mt6853:$(TARGET_COPY_OUT_RAMDISK)/fstab.mt6853

# Init
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/rootdir/etc/init.kyoto.rc:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/init/init.kyoto.rc

# Off-mode charging: AOSP charger with the stock vendor health HAL (verified on device).
# Stock kpoc_charger is not shipped; it needs libshowlogo/libsuspend from stock system.

# Overlays (runtime resource overlays in rro_overlays/)
PRODUCT_PACKAGES += \
    FrameworksResKyoto \
    SettingsResKyoto \
    SystemUIResKyoto

# Moto gestures: chop chop flashlight, peek wake, double tap to wake
PRODUCT_PACKAGES += \
    KyotoActions

# Refresh rate setting (Settings > Display > Refresh rate: Adaptive / 60 Hz / 90 Hz)
PRODUCT_PACKAGES += \
    KyotoRefreshRate

# Display
TARGET_SCREEN_HEIGHT := 2400
TARGET_SCREEN_WIDTH := 1080
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := 420dpi

# Characteristics
PRODUCT_CHARACTERISTICS := nosdcard

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)

# Moto Camera 4 (stock MotCamera4 9.1.18.92 + Motorola framework libraries)
TARGET_USES_MOTCAMERA4 := true
$(call inherit-product-if-exists, vendor/motorola/MotCamera4-kyoto/motcamera4.mk)

# Motorola apps: FM Radio, Moto Audio, per-app volume, recorder, aptX, Gametime, Spaces,
# screenshot editor, Time & Weather, live wallpaper, Face Unlock. KyotoMotoSettings stands in
# for Motorola's settings provider (android.uid.system on stock), which these apps need.
$(call inherit-product-if-exists, vendor/motorola/motoapps-kyoto/motoapps.mk)
PRODUCT_PACKAGES += \
    KyotoMotoSettings

# Prebuilt vendor tree
$(call inherit-product, vendor/motorola/kyoto/kyoto-vendor.mk)
