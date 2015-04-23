package com.esri.runtime.android.materialbasemaps;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.esri.runtime.android.materialbasemaps.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withText;



/**
 * Tests to verify that the behavior of {@link MainActivity} is correct.
 * <p>
 * Note that in order to scroll the list you shouldn't use {@link android.support.test.espresso.ViewAction#scrollTo()} as
 * {@link android.support.test.espresso.Espresso#onData(org.hamcrest.Matcher)} handles scrolling.</p>
 *
 * @see #onRow(String)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest{

    protected static final String TEXT_ITEM_1 = "Imagery";
    protected static final String LAST_ITEM_ID = "USGS National Map";

    /**
     * A JUnit {@link org.junit.Rule @Rule} to launch your activity under test. This is a replacement
     * for {@link ActivityInstrumentationTestCase2}.
     * <p>
     * Rules are interceptors which are executed for each test method and will run before
     * any of your setup code in the {@link Before @Before} method.
     * <p>
     * {@link android.support.test.rule.ActivityTestRule} will create and launch of the activity for you and also expose
     * the activity under test. To get a reference to the activity you can use
     * the {@link android.support.test.rule.ActivityTestRule#getActivity()} method.
     */
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    /**
     * Test that the list is long enough for this sample, the last item shouldn't appear.
     */
    @Test
    public void lastItem_NotDisplayed() {
        // Last item should not exist if the list wasn't scrolled down.
        onView(withText(LAST_ITEM_ID)).check(ViewAssertions.doesNotExist());
    }

//    /**
//     * Check that the item is created. onData() takes care of scrolling.
//     */
//    @Test
//    public void list_Scrolls() {
//        onRow(LAST_ITEM_ID).check(matches(isCompletelyDisplayed()));
//    }


}
