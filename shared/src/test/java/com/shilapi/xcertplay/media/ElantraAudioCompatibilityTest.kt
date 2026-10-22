package com.shilapi.xcertplay.media

import android.media.AudioAttributes
import android.os.Build
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ElantraAudioCompatibilityTest {
    private val manufacturer = Build.MANUFACTURER
    private val model = Build.MODEL
    @Before fun setDevice() {
        ShadowBuild.setManufacturer("humaxdigital")
        ShadowBuild.setModel("BHCN-T3103C")
    }
    @After fun cleanup() {
        ShadowBuild.setManufacturer(manufacturer)
        ShadowBuild.setModel(model)
    }

    @Test fun assistantUsesAudibleNavigationUsageWhileMusicAndGuidanceKeepTheirAttributes() {
        for ((usage, expected) in listOf(
            AudioAttributes.USAGE_MEDIA to AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE to AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
            AudioAttributes.USAGE_ASSISTANT to AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
        )) {
            val actual = ElantraAudioCompatibility.attributes(AudioAttributes.Builder()
                .setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            assertEquals(expected, actual.usage)
            assertEquals(AudioAttributes.CONTENT_TYPE_MUSIC, actual.contentType)
            assertTrue((AudioAttributes::class.java.getMethod("getTags").invoke(actual) as Set<*>).isEmpty())
        }
    }

    @Test fun phoneAndOtherHeadUnitsKeepTheirExistingAttributes() {
        val phone = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build()
        assertSame(phone, ElantraAudioCompatibility.attributes(phone))
        ShadowBuild.setModel("BHCN-T3105C")
        val media = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
        assertSame(media, ElantraAudioCompatibility.attributes(media))
    }
}
