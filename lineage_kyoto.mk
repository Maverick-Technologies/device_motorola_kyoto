#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from device
$(call inherit-product, device/motorola/kyoto/device.mk)

# Inherit some common LineageOS / Evolution X stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Boot animation
TARGET_BOOT_ANIMATION_RES := 1080

# Device identifier. This must come after all inclusions.
PRODUCT_NAME := lineage_kyoto
PRODUCT_DEVICE := kyoto
PRODUCT_BRAND := motorola
PRODUCT_MODEL := motorola edge 20 fusion
PRODUCT_MANUFACTURER := motorola

PRODUCT_GMS_CLIENTID_BASE := android-motorola

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="kyoto_retail-user 13 T2RK33.146-14-1 20a30-3236ff release-keys" \
    BuildFingerprint=motorola/kyoto_retail/kyoto:13/T2RK33.146-14-1/20a30-3236ff:user/release-keys \
    DeviceName=kyoto \
    DeviceProduct=kyoto_retail
