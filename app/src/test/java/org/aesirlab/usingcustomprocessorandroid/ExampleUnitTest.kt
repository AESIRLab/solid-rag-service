package org.aesirlab.usingcustomprocessorandroid

import android.app.Application
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.aesirlab.usingcustomprocessorandroid.rag.RagPipeline
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
//@RunWith(Robo)
class ExampleUnitTest {


    private lateinit var mockApplication: Application;

    @Before
    fun setUp() {
        mockApplication = mock(Application::class.java)
    }
//    @Test
//    fun canEvaluateRagPipeline() {
//        val ragPipeline = RagPipeline(mockApplication)
//        assertEquals(2, 2)
//    }
}