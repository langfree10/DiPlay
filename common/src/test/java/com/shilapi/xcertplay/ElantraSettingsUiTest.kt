package com.shilapi.xcertplay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.shilapi.xcertplay.host.R
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ElantraSettingsUiTest {
    private val manufacturer = Build.MANUFACTURER
    private val model = Build.MODEL
    private val context get() = RuntimeEnvironment.getApplication()
    @Before fun setup() {
        ShadowBuild.setManufacturer("humaxdigital"); ShadowBuild.setModel("BHCN-T3103C")
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("diplay_prompt_audio_diagnostics", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun cleanup() {
        ShadowBuild.setManufacturer(manufacturer); ShadowBuild.setModel(model)
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun render(activity: DiPlayActivity, sections: Set<SettingsSection>): LinearLayout {
        DiPlayActivity::class.java.getDeclaredField("page").apply { isAccessible = true }.set(activity, "settings")
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        DiPlayActivity::class.java.getDeclaredMethod("renderSections", LinearLayout::class.java, Set::class.java)
            .apply { isAccessible = true }.invoke(activity, column, sections)
        return column
    }
    private fun texts(root: View): List<String> = views(root).filterIsInstance<TextView>().map { it.text.toString() }.toList()
    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(views(view.getChildAt(i)))
    }
    @Test fun wiredSetupHasNoWirelessChoicesOrBydCards() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        val root = render(activity, setOf(SettingsSection.USB_CONNECTION, SettingsSection.BYD_ADB,
            SettingsSection.BYD_NAVIGATION, SettingsSection.CLUSTER_MAP, SettingsSection.AMBIENT_LIGHTING))
        val copy = texts(root)
        assertTrue(copy.any { it.startsWith(activity.getString(R.string.settings_usb_charging)) })
        assertFalse(copy.any { it.contains("Wi-Fi") || it.contains("BYD") })
        val setup = LinearLayout(activity)
        DiPlayActivity::class.java.getDeclaredMethod("connectionSetup", LinearLayout::class.java)
            .apply { isAccessible = true }.invoke(activity, setup)
        assertTrue(texts(setup).any { it == activity.getString(R.string.connect_with_usb) })
        assertFalse(texts(setup).any { it.contains("Wi-Fi") })
        activity.finish()
    }
    @Test fun chargingMenuIsAscendingAndRetainsTheSavedZeroSelection() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        AirPlayPersistence.saveUsbChargingCurrent(activity, UsbChargingCurrent.DATA_ONLY)
        val root = render(activity, setOf(SettingsSection.USB_CONNECTION))
        views(root).filterIsInstance<TextView>().first {
            it.text.toString().startsWith(activity.getString(R.string.settings_usb_charging) + " ·")
        }.performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals(listOf(R.string.settings_usb_charging_data_only, R.string.settings_usb_charging_low,
            R.string.settings_usb_charging_reduced, R.string.settings_usb_charging_normal).map(activity::getString),
            (0 until dialog.listView.adapter.count).map { dialog.listView.adapter.getItem(it).toString() })
        assertEquals(0, dialog.listView.checkedItemPosition)
        dialog.dismiss(); activity.finish()
    }

    @Test fun videoChoicesKeepSavedRatesAndSeparateReceivedCodecFromThePreference() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        AirPlayPersistence.saveFps(activity, 35)
        AirPlayPersistence.saveCarBluetoothAudio(activity, true)
        AirPlayPersistence.saveUsbChargingCurrent(activity, UsbChargingCurrent.DATA_ONLY)
        var root = render(activity, setOf(SettingsSection.DISPLAY_AND_PERFORMANCE, SettingsSection.ADVANCED_MEDIA))
        views(root).filterIsInstance<TextView>().first {
            it.text.toString().startsWith(activity.getString(R.string.frame_rate) + " ·")
        }.performClick()
        var dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals(listOf("30 fps", "35 fps", "40 fps", "45 fps", "50 fps", "55 fps", "60 fps"),
            (0 until dialog.listView.adapter.count).map { dialog.listView.adapter.getItem(it).toString() })
        assertEquals(1, dialog.listView.checkedItemPosition)
        org.robolectric.Shadows.shadowOf(dialog).clickOnItem(3)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(45, AirPlayPersistence.loadFps(activity))
        views(root).filterIsInstance<TextView>().first {
            it.text.toString().startsWith(activity.getString(R.string.settings_video_codec_experimental) + " ·")
        }.performClick()
        dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals(0, dialog.listView.checkedItemPosition)
        assertEquals(listOf(activity.getString(R.string.settings_video_h264), activity.getString(R.string.settings_video_hevc)),
            (0 until dialog.listView.adapter.count).map { dialog.listView.adapter.getItem(it).toString() })
        org.robolectric.Shadows.shadowOf(dialog).clickOnItem(1)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(AirPlayPersistence.loadHevcEnabled(activity))
        val attempt = DisplayDiagnosticSnapshot.begin(activity, "request", "capability", "effective")
        DisplayDiagnosticSnapshot.recordReceivedCodec(activity, attempt, com.shilapi.xcertplay.airplay.VideoCodec.H264)
        root = render(activity, setOf(SettingsSection.DISPLAY_AND_PERFORMANCE, SettingsSection.ADVANCED_MEDIA))
        assertTrue(texts(root).contains(activity.getString(R.string.settings_video_received, "H.264")))
        assertTrue(texts(root).any { it.endsWith(" · 45 fps") })
        assertTrue(AirPlayPersistence.loadCarBluetoothAudio(activity))
        assertEquals(UsbChargingCurrent.DATA_ONLY, AirPlayPersistence.loadUsbChargingCurrent(activity))
        activity.finish()
    }

    @Test fun promptQualityChoicesPreservePreferenceAndShowReceivedRateSeparately() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        AirPlayPersistence.saveCarBluetoothAudio(activity, false)
        val attempt = PromptAudioDiagnosticSnapshot.begin(activity)
        PromptAudioDiagnosticSnapshot.record(activity, attempt, com.shilapi.xcertplay.airplay.AudioFormat(
            com.shilapi.xcertplay.airplay.AudioCodecKind.LPCM, 8000, 2, 101, "default"))
        var root = render(activity, setOf(SettingsSection.ADVANCED_MEDIA))
        views(root).filterIsInstance<TextView>().first {
            it.text.toString().startsWith(activity.getString(R.string.settings_usb_prompt_quality_experimental) + " ·")
        }.performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals(listOf(activity.getString(R.string.settings_usb_prompt_compatible),
            activity.getString(R.string.settings_usb_prompt_clear), activity.getString(R.string.settings_usb_prompt_pcm_48)),
            (0 until dialog.listView.adapter.count).map { dialog.listView.adapter.getItem(it).toString() })
        assertEquals(2, dialog.listView.checkedItemPosition)
        org.robolectric.Shadows.shadowOf(dialog).clickOnItem(1)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(AirPlayPersistence.loadSpeechWithout8And16Khz(activity))
        root = render(activity, setOf(SettingsSection.ADVANCED_MEDIA))
        assertTrue(texts(root).contains(activity.getString(R.string.settings_usb_prompt_received, "8", "PCM", 2)))
        assertFalse(AirPlayPersistence.loadCarBluetoothAudio(activity))
        activity.finish()
    }

    @Test fun selecting48KhzPersistsChoiceButDoesNotFakeAReceived48KhzFormat() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        val attempt = PromptAudioDiagnosticSnapshot.begin(activity)
        PromptAudioDiagnosticSnapshot.record(activity, attempt, com.shilapi.xcertplay.airplay.AudioFormat(
            com.shilapi.xcertplay.airplay.AudioCodecKind.LPCM, 24000, 2, 101, "default"))
        val root = render(activity, setOf(SettingsSection.ADVANCED_MEDIA))
        views(root).filterIsInstance<TextView>().first {
            it.text.toString().startsWith(activity.getString(R.string.settings_usb_prompt_quality_experimental) + " ·")
        }.performClick()
        var dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals(3, dialog.listView.adapter.count)
        org.robolectric.Shadows.shadowOf(dialog).clickOnItem(2)
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(UsbPromptQuality.PCM_48_KHZ, AirPlayPersistence.loadUsbPromptQuality(activity))
        assertEquals(24000, PromptAudioDiagnosticSnapshot.latest(activity)!!.sampleRate)
        val updated = render(activity, setOf(SettingsSection.ADVANCED_MEDIA))
        assertTrue(texts(updated).contains(activity.getString(R.string.settings_usb_prompt_received, "24", "PCM", 2)))
        views(updated).filterIsInstance<TextView>().first {
            it.text.toString().startsWith(activity.getString(R.string.settings_usb_prompt_quality_experimental) + " ·")
        }.performClick()
        dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertEquals(2, dialog.listView.checkedItemPosition)
        dialog.dismiss(); activity.finish()
    }

    @Test fun promptSummaryRefreshesInPlaceAndKeepsLastRateAfterReconnect() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        val root = render(activity, setOf(SettingsSection.ADVANCED_MEDIA))
        assertTrue(texts(root).contains(activity.getString(R.string.settings_usb_prompt_received_unknown)))
        val first = PromptAudioDiagnosticSnapshot.begin(activity)
        PromptAudioDiagnosticSnapshot.record(activity, first, com.shilapi.xcertplay.airplay.AudioFormat(
            com.shilapi.xcertplay.airplay.AudioCodecKind.LPCM, 24000, 2, 101, "default"))
        fun refresh() = DiPlayActivity::class.java.getDeclaredMethod("refreshPromptAudioSummary")
            .apply { isAccessible = true }.invoke(activity)
        refresh()
        assertTrue(texts(root).contains(activity.getString(R.string.settings_usb_prompt_received, "24", "PCM", 2)))
        PromptAudioDiagnosticSnapshot.begin(activity)
        refresh()
        val summary = texts(root).single { it.contains("24") && it.contains("PCM") }
        assertTrue(summary.contains("\n"))
        assertFalse(texts(root).contains(activity.getString(R.string.settings_usb_prompt_received_unknown)))
        activity.finish()
    }

    @Test fun largeFontUsbAndCarButtonScreenshotsAndSquareIcon() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        AirPlayPersistence.saveUsbChargingCurrent(activity, UsbChargingCurrent.DATA_ONLY)
        val attempt = PromptAudioDiagnosticSnapshot.begin(activity)
        PromptAudioDiagnosticSnapshot.record(activity, attempt, com.shilapi.xcertplay.airplay.AudioFormat(
            com.shilapi.xcertplay.airplay.AudioCodecKind.LPCM, 24000, 2, 101, "default"))
        PromptAudioDiagnosticSnapshot.begin(activity)
        AirPlayPersistence.saveUsbPromptQuality(activity, UsbPromptQuality.PCM_48_KHZ)
        for ((name, widthDp, language) in listOf(Triple("compact-zh", 320, "zh-CN"),
            Triple("full-zh", 960, "zh-CN"), Triple("compact-ar-rtl", 320, "ar-SA"))) {
            val locale = Locale.forLanguageTag(language)
            val config = Configuration(activity.resources.configuration).apply {
                fontScale = 1.5f; setLocale(locale); setLayoutDirection(locale)
            }
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
            val column = render(activity, setOf(SettingsSection.USB_CONNECTION, SettingsSection.CAR_BUTTON, SettingsSection.ADVANCED_MEDIA, SettingsSection.DIAGNOSTICS, SettingsSection.DISPLAY_AND_PERFORMANCE))
            val copy = texts(column)
            assertTrue(copy.any { it.contains("伊兰特") })
            assertTrue(copy.contains(activity.getString(R.string.settings_report_to_usb)))
            assertFalse(copy.contains(activity.getString(R.string.settings_report_on_phone)))
            assertTrue(copy.contains(activity.getString(R.string.settings_siri_usb_reply_experimental)))
            assertTrue(copy.any { it.startsWith(activity.getString(R.string.settings_video_codec_experimental) + " ·") })
            assertFalse(copy.contains(activity.getString(R.string.efficient_video)))
            val width = (widthDp * activity.resources.displayMetrics.density).toInt()
            column.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            column.layout(0, 0, width, column.measuredHeight)
            val export = views(column).filterIsInstance<TextView>().single {
                it.text.toString() == activity.getString(R.string.settings_report_to_usb)
            }
            assertTrue("USB export text clipped at $name", export.layout.getLineBottom(export.layout.lineCount - 1) <=
                export.height - export.compoundPaddingTop - export.compoundPaddingBottom)
            val bitmap = Bitmap.createBitmap(width, column.measuredHeight, Bitmap.Config.ARGB_8888)
            val directory = File("build/reports/elantra-test12-ui").apply { mkdirs() }
            try {
                column.draw(Canvas(bitmap))
                File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
        }
        val encoded = ElantraCarIcon.defaultBytes(activity)
        val icon = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
        assertEquals(192, icon.width); assertEquals(192, icon.height)
        File("build/reports/elantra-test12-ui/hyundai-carplay-icon.png").writeBytes(encoded)
        icon.recycle()
        activity.finish()
    }
}
