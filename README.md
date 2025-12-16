# NanoSonic

**A minimalist, offline-first Android music player with accessible professional-grade parametric equalization**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Material You](https://img.shields.io/badge/Design-Material%20You-blue.svg)](https://m3.material.io/)
[![Offline](https://img.shields.io/badge/Mode-Fully%20Offline-orange.svg)]()

---

## Overview

NanoSonic is a lightweight, privacy-focused Android music player that prioritizes audio quality and accessible EQ integration. NanoSonic offers professional-grade parametric equalization and provides device-specific profiles sourced from the [AutoEQ](https://github.com/jaakkopasanen/AutoEq) project.

This local music player is ideal for users who want studio-quality EQ without the learning curve.

---

## ✨ Key Features

### 🎚️ Professional Parametric Equalization
- **Full parametric EQ control** with per-band frequency, gain, and Q (bandwidth) support
- Real-time audio processing with minimal latency
- Support for peaking, low-shelf, high-shelf, lowpass, and highpass filters

### 🎧 AutoEQ Profile Support
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
- Music stored in the default Android music folder: `/storage/emulated/0/Music`

---

## ⚠️ Known Limitations

### Current Limitations
These limitations exist in the current version and may be addressed in future updates:

- **24-bit audio not supported** - The parametric equalizer currently only processes 16-bit PCM audio. 24-bit files will play but may experience quality degradation through the EQ chain.
- **Fixed music directories** - The app only scans the default Android music directories. Custom directory selection is not currently supported.

### Design Philosophy: Features I Probably Won't Add

NanoSonic is intentionally minimal and offline-focused. The following features are **not planned** and will likely never be implemented:

❌ **Android Auto**

❌ **Playlist Creation** 

❌ **Internet-Dependent Features** 

❌ **Social Features** 


---

## 🛠️ Technical Details

### Audio Processing
- **Engine**: Custom implementation using MWEngine DSP library
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
NanoSonic's parametric EQ profiles are sourced from the [AutoEQ project](https://github.com/jaakkopasanen/AutoEq) by Jaakko Pasanen. AutoEQ provides scientifically measured frequency response corrections for thousands of headphones, bringing them closer to a neutral reference sound.

**AutoEQ License**: MIT License  
**Database**: Includes measurements from:
- oratory1990 (Reddit)
- crinacle
- Rtings
- Innerfidelity
- And many other contributors

### Audio DSP
Built with components from:
- **MWEngine** - Low-latency audio engine for Android
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
