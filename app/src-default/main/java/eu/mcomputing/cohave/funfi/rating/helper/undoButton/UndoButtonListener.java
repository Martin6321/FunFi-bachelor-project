package eu.mcomputing.cohave.funfi.rating.helper.undoButton;


import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.rating.adapter.WifiRatingAdapter;

/**
 * Listener for undo action button
 */
public interface UndoButtonListener {


    /**
     * To shoiw Undo Button
     */
    void show(WifiModel item, int position, WifiRatingAdapter adapter);

    /**
     * To hide Undo Button
     */
    void hide();
}
