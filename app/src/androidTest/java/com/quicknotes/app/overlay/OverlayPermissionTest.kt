package com.quicknotes.app.overlay

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayPermissionTest {
    @Test
    fun requestIntentTargetsOverlaySettingsForThisApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = OverlayPermission.requestIntent(context)
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }

    @Test
    fun isGrantedReflectsSystemSetting() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(Settings.canDrawOverlays(context), OverlayPermission.isGranted(context))
    }
}
