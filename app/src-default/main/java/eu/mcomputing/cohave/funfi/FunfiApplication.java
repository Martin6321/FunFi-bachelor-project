package eu.mcomputing.cohave.funfi;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import eu.mcomputing.cohave.CohaveApplication;
import eu.mcomputing.cohave.funfi.helper.config.Config;


public class FunfiApplication extends CohaveApplication {
    /**
     * The Analytics singleton. The field is set in onCreate method override when the application
     * class is initially created.
     */
    private static GoogleAnalytics analytics;

    /**
     * The default app tracker. The field is from onCreate callback when the application is
     * initially created.
     */
    private static Tracker tracker;

    /**
     * Access to the global Analytics singleton. If this method returns null you forgot to either
     * set android:name="&lt;this.class.name&gt;" attribute on your application element in
     * AndroidManifest.xml or you are not setting this.analytics field in onCreate method override.
     */
    public static GoogleAnalytics analytics() {
        return analytics;
    }

    /**
     * The default app tracker. If this method returns null you forgot to either set
     * android:name="&lt;this.class.name&gt;" attribute on your application element in
     * AndroidManifest.xml or you are not setting this.tracker field in onCreate method override.
     */
    public static Tracker tracker() {
        return tracker;
    }

    @Override
    public void onCreate() {
        super.onCreate();

//        analytics = GoogleAnalytics.getInstance(this);
//        tracker = analytics.newTracker(Config.googleKey);
//        tracker.enableExceptionReporting(true);
//        tracker.enableAdvertisingIdCollection(true);
//        tracker.enableAutoActivityTracking(true);

    }
}

