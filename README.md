# NanoSonic

**A minimalist, offline-first Android music player with accessible and powerful parametric EQ**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Material You](https://img.shields.io/badge/Design-Material%20You-blue.svg)](https://m3.material.io/)
[![Offline](https://img.shields.io/badge/Mode-Fully%20Offline-orange.svg)]()

---

## Overview

NanoSonic is a lightweight, privacy-focused local music player for Android. NanoSonic offers professional-grade parametric EQ and provides device-specific profiles sourced from the [AutoEQ](https://github.com/jaakkopasanen/AutoEq) project. These profiles are locally stored and can be imported through our user-friendly setup wizard at any time, regardless of internet connection or the user's level of technical proficiency.

NanoSonic is primary designed as a FOSS alternative for users who want studio-quality EQ without the cost of proprietary solutions like [Poweramp (20$)](https://play.google.com/store/apps/details?id=com.maxmpz.audioplayer.unlock&pcampaignid=web_share) or [Neutron ($13)](https://play.google.com/store/apps/details?id=com.neutroncode.mp&pcampaignid=web_share). Unlike external EQ apps such as [Wavelet](https://play.google.com/store/apps/details?id=com.pittvandewitt.wavelet&hl=en_US) that require additional configuration, NanoSonic provides integrated, powerful equalization that works seamlessly with your music library out of the box.

---

## ✨ Key Features

### 🎚️ Studio-grade Parametric Equalization
- Full parametric EQ support with per-band frequency, gain, and Q (bandwidth) support
- Support for peaking, low-shelf, high-shelf, lowpass, and highpass filters
- Real-time audio processing with minimal latency


### 🎧 Integrated AutoEQ Profiles
- **Integrated AutoEQ database** for offline access of profiles supporting thousands of headphones and IEMs
- **User-friendly import wizard** for quick EQ profile setup
- **Custom profile import** supporting industry-standard parametric EQ profile format

### 🎵 Gapless Playback
- Seamless transitions between tracks
- Configurable gapless modes:
    - **Enabled for Albums** (default)
    - **Enabled** - Gapless playback everywhere
    - **Disabled** - Traditional playback with gaps

---

### Requirements
- Android 13 (API 33) or higher
- Storage permission for accessing music files
- Music stored in the default Android music folder: `/storage/emulated/0/Music` or `/storage/emulated/0/Downloads`

### Current Limitations
These limitations exist in the current version and may be addressed in future updates:

- **24-bit audio not supported** - The equalizer will only process 16-bit PCM audio. 24-bit files will play but will down-sample to 16-bit through the EQ chain.
- **Fixed music directories** - The app only scans the default Android 'music' and 'downloads' directories. Custom directory selection is not currently supported.

### Design Philosophy: Features I Probably Won't Add

NanoSonic is intentionally minimal and offline-focused. The following features are **not planned** and will likely never be implemented:

❌ **Android Auto**

❌ **Playlist Creation/Management** 

❌ **Internet-Dependent Features**

---

## 🛠️ Technical Details

### Audio Processing
- **Sample Rate**: Adaptive (matches device/file sample rate)
- **Bit Depth**: 16-bit PCM processing
- **Latency**: Optimized for real-time playback with minimal delay

### EQ Implementation
- **Filter Type**: Biquad IIR filters
- **Coefficient Calculation**: Based on Audio EQ Cookbook formulas
- **Processing**: Serial cascade of filters in the audio processing chain
- **Precision**: Double-precision coefficient calculation for accuracy

---

## 🙏 Acknowledgments

### AutoEQ Project
NanoSonic's parametric EQ profiles are sourced from the [AutoEQ project](https://github.com/jaakkopasanen/AutoEq) by Jaakko Pasanen. AutoEQ provides scientifically measured frequency response corrections for thousands of headphones, bringing them closer to a more widely-preferred Harman sound.

**AutoEQ License**: MIT License  
**Database**: Includes measurements from:
- oratory1990 (Reddit)
- Crinacle
- RTINGS.com
- Super Review
- And many other contributors

### Audio DSP
Built with components from:
- **Audio EQ Cookbook** - Robert Bristow-Johnson's biquad filter formulas
- **ExoPlayer** - Google's media player library for Android

---

## License

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

NanoSonic is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
