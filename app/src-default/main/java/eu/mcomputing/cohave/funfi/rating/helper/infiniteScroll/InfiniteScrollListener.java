package eu.mcomputing.cohave.funfi.rating.helper.infiniteScroll;


import eu.mcomputing.cohave.funfi.rating.adapter.WifiRatingAdapter;

/**
 * Listener for creating infinite scroll
 */
public interface InfiniteScrollListener {

    int LoadingRunning = 1;
    int LoadingIdle = 0;
    int LoadingError = 2;

    /**
     * Check if new data are needed to load
     */
    void checkDataToAdd(WifiRatingAdapter adapter);

    /**
     * Loading started
     */
    void setLoadingStart(String text);

    /**
     * Loaded finished success
     */
    void setLoadingEnd();

    /**
     * Loading error
     */
    void setLoadingError(String text);

    void setEmptyViewIfNoData();

    int getStatus();
}
