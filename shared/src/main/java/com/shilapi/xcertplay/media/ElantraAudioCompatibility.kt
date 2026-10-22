package com.shilapi.xcertplay.media

import android.media.AudioAttributes
import android.os.Build

/** Public Android routing for the owner's Humax unit, refined using real playback results. */
object ElantraAudioCompatibility {
    fun supported(manufacturer: String = Build.MANUFACTURER.orEmpty(), model: String = Build.MODEL.orEmpty()): Boolean =
        manufacturer.equals("humaxdigital", ignoreCase = true) && model.equals("BHCN-T3103C", ignoreCase = true)

    /** Navigation is audible over native Bluetooth; test10 assistant media usage was silent. */
    fun attributes(original: AudioAttributes): AudioAttributes {
        if (!supported()) return original
        val usage = when (original.usage) {
            AudioAttributes.USAGE_ASSISTANT -> AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
            AudioAttributes.USAGE_MEDIA, AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> original.usage
            else -> return original // Keep CarPlay's separate telephone channel intact.
        }
        return AudioAttributes.Builder().setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    }
}
