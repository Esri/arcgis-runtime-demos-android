package com.esri.runtime.android.materialbasemaps;

import android.support.annotation.IdRes;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.esri.runtime.android.materialbasemaps.ui.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;




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

    /**
     * Check that the item is created. onData() takes care of scrolling.
     */
    @Test
    public void list_Scrolls() {
        ViewMatchers.onRecyclerItemView(R.id.basemapName, withText("USGS National Map"),  withId(R.id.list))
                .matches(check(withText("Test Content")));
    }

    public class ViewMatchers {
        @SuppressWarnings("unchecked")
        public static Matcher<View> withRecyclerView(@IdRes int viewId) {
            return allOf(isAssignableFrom(RecyclerView.class), withId(viewId));
        }

        @SuppressWarnings("unchecked")
        public static ViewInteraction onRecyclerItemView(@IdRes int identifyingView, Matcher<View> identifyingMatcher, Matcher<View> childMatcher) {
            Matcher<View> itemView = allOf(withParent(withRecyclerView(R.id.list_item)),
                    withChild(allOf(withId(identifyingView), identifyingMatcher)));
            return Espresso.onView(allOf(isDescendantOfA(itemView), childMatcher));
        }
    }


}
