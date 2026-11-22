package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and full name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val fullName = context.getString(R.string.app_full_name)
        assertEquals("Narendra Weight & Money Calc", appName)
        assertEquals("Narendra Weight & Money Calculator", fullName)
        assertEquals("com.narendra.weightmoneycalculator", context.packageName)
    }
}
