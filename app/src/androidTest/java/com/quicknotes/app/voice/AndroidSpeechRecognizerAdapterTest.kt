package com.quicknotes.app.voice

import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSpeechRecognizerAdapterTest {
    @Test
    fun canBeConstructedWithoutCrashing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // SpeechRecognizer enforces main-thread-only construction/teardown since API 30.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val adapter = AndroidSpeechRecognizerAdapter(context)
            assertNotNull(adapter)
            adapter.destroy()
        }
    }

    @Test
    fun reportsRecognitionAvailability() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // This assertion documents the spec's risk: availability depends on the device.
        // It must not throw regardless of the result.
        SpeechRecognizer.isRecognitionAvailable(context)
    }
}
