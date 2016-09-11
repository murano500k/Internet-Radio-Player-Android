package com.android.murano500k.newradio;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Created by artem on 9/9/16.
 */
@RunWith(AndroidJUnit4.class)
public class TestBase extends Assert {
	Instrumentation instrumentation;
	Context context;
	public TestBase() {
		instrumentation= InstrumentationRegistry.getInstrumentation();
		context=instrumentation.getTargetContext();
	}
}
