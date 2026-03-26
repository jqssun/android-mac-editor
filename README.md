# MAC Editor for Android

[![Stars](https://img.shields.io/github/stars/jqssun/android-mac-editor)](https://github.com/jqssun/android-mac-editor/stargazers)
[![LSPosed](https://img.shields.io/github/downloads/Xposed-Modules-Repo/io.github.jqssun.maceditor/total?label=LSPosed&logo=Android&style=flat&labelColor=F48FB1&logoColor=ffffff)](https://github.com/Xposed-Modules-Repo/io.github.jqssun.maceditor/releases)
[![GitHub](https://img.shields.io/github/downloads/jqssun/android-mac-editor/total?label=GitHub&logo=GitHub)](https://github.com/jqssun/android-mac-editor/releases)
[![release](https://img.shields.io/github/v/release/jqssun/android-mac-editor)](https://github.com/jqssun/android-mac-editor/releases)
[![build](https://img.shields.io/github/actions/workflow/status/jqssun/android-mac-editor/apk.yml)](https://github.com/jqssun/android-mac-editor/actions/workflows/apk.yml)
[![license](https://img.shields.io/github/license/jqssun/android-mac-editor?color=green)](https://github.com/jqssun/android-mac-editor/blob/master/LICENSE)

A free and open-source module that gives you granular control over the Wi-Fi MAC address on Android devices. It supports manual MAC override and enables native MAC randomization support exposed by Android on supported hardware regardless of the OEM's implementation. You can use it, for example, to customize MAC behavior for privacy, or to assist devices with limited captive portal support to access the Internet on certain Wi-Fi networks.

## Compatibility

- Android 12+ (tested up to Android 16 QPR2)
- Rooted devices with LSPosed framework installed

## Implementation

On modern Android, [the Wi-Fi subsystem](https://android.googlesource.com/platform/packages/modules/Wifi/+/refs/heads/main/service/java/com/android/server/wifi/WifiNative.java) is capable of randomizing device MAC address per network or per connection. This module hooks the following system server methods to allow manual MAC assignment when randomization is enabled,

- `WifiVendorHal.setStaMacAddress()`
- `WifiVendorHal.setApMacAddress()`

For better compatibility, the module can also be used to force enable [MAC randomization](https://source.android.com/docs/core/connect/wifi-mac-randomization) by specifying the following resource booleans,
- `config_wifi_connected_mac_randomization_supported`: support for standard Wi-Fi
- `config_wifi_p2p_mac_randomization_supported`: support for Wi-Fi Direct or P2P
- `config_wifi_ap_mac_randomization_supported`: support for mobile hotspot

This is useful on devices where the hardware and chipset drivers do support MAC randomization, but the device vendor does not implement proper software support. This can also happen on some alternative Android builds where MAC randomization is not explicitly enabled. 

### Note for Qualcomm devices

Hardware support on certain chipsets can be checked by looking at `/vendor{/etc/wifi/kiwi_v2,firmware/wlan/qca_cld}/WCNSS_qcom_cfg.ini`. For legacy Qualcomm devices without MAC randomization support, consider editing `wlan_mac.bin` or `/sys/wifi/mac_addr` directly instead of using this module.

## Credits

- [David Berdik](https://f-droid.org/repo/com.berdik.macsposed_6_src.tar.gz) for the initial open-source system server hook implementation