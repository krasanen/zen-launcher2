package fi.zmengames.zlauncher.searcher;

import fi.zmengames.zlauncher.ui.ListPopup;

public interface QueryInterface {
    void launchOccurred();

    void registerPopup(ListPopup popup);
}
