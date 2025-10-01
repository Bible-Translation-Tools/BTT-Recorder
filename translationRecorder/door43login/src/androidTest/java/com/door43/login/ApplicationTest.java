package com.door43.login;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    private Application application;

    @Before
    public void setUp() {
        application = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testApplicationNotNull() {
        assertNotNull(application);
    }
}