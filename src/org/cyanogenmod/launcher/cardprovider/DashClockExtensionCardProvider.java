package org.cyanogenmod.launcher.cardprovider;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.cyanogenmod.launcher.cards.CmCard;
import org.cyanogenmod.launcher.cards.DashClockExtensionCard;
import org.cyanogenmod.launcher.dashclock.ExtensionHost;
import org.cyanogenmod.launcher.dashclock.ExtensionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Manages fetching data from all installed DashClock extensions
 * and generates cards to be displayed.
 */
public class DashClockExtensionCardProvider implements ICardProvider, ExtensionManager.OnChangeListener {
    public static final String TAG = "DashClockExtensionCardProvider";
    public static final String EXTENSION_TIMEOUT_SHARED_PREF_FILE = "DashClockExtensionTimeouts";
    public static final int CARD_REAPPEAR_TIME_IN_MINUTES = 180; // three hours

    private ExtensionManager mExtensionManager;
    private ExtensionHost mExtensionHost;
    private Context mContext;
    private Context mHostActivityContext;
    private List<CardProviderUpdateListener> mUpdateListeners = new ArrayList<CardProviderUpdateListener>();

    public DashClockExtensionCardProvider(Context context, Context hostActivityContext) {
        mContext = context;
        mHostActivityContext = hostActivityContext;
        mExtensionManager = ExtensionManager.getInstance(context, hostActivityContext);
        mExtensionManager.addOnChangeListener(this);
        mExtensionHost = new ExtensionHost(context, hostActivityContext);

        trackAllExtensions();
    }

    @Override
    public void onShow() {
        mExtensionHost.init();
        mExtensionManager.addOnChangeListener(this);
        trackAllExtensions();
    }

    @Override
    public void onDestroy(Context context) {
        mExtensionManager.removeOnChangeListener(this);
        mExtensionHost.destroy();
        mExtensionManager.setActiveExtensions(new ArrayList<ComponentName>());
    }

    @Override
    public void onHide(Context context) {
        // Tear down the extension connections when the app is hidden,
        // so that we don't block other readers (i.e. actual dashclock).
        mExtensionManager.removeOnChangeListener(this);
        mExtensionHost.destroy();
        mExtensionManager.setActiveExtensions(new ArrayList<ComponentName>());
    }

    @Override
    public List<CmCard> getCards() {
        List<CmCard> cards = new ArrayList<CmCard>();

        for(ExtensionManager.ExtensionWithData extensionWithData :
                mExtensionManager.getActiveExtensionsWithData()) {
            if(extensionWithData.latestData != null
               && extensionWithData.latestData.visible()
               && shouldReappear(extensionWithData.listing.componentName.flattenToString())
               && !TextUtils.isEmpty(extensionWithData.latestData.status())) {
                DashClockExtensionCard card = new DashClockExtensionCard(mContext,
                                                       extensionWithData,
                                                       mHostActivityContext);
                setCardSwipeAndUndoListeners(card);
                cards.add(card);
            }
        }

        return cards;
    }

    @Override
    public void requestRefresh() {
        trackAllExtensions();
        mExtensionHost.requestAllManualUpdate();
    }

    @Override
    public CardProviderUpdateResult updateAndAddCards(List<CmCard> cards) {
        List<ExtensionManager.ExtensionWithData> extensions
                = mExtensionManager.getActiveExtensionsWithData();

        // A List of cards to return that must be removed
        List<CmCard> cardsToRemove = new ArrayList<CmCard>();

        // Create a map from ComponentName String -> extensionWithData
        HashMap<String, ExtensionManager.ExtensionWithData> map
                = new HashMap<String, ExtensionManager.ExtensionWithData>();
        for(ExtensionManager.ExtensionWithData extension : extensions) {
            map.put(extension.listing.componentName.flattenToString(), extension);
        }

        for(CmCard card : cards) {
            if(card instanceof DashClockExtensionCard) {
                DashClockExtensionCard dashClockExtensionCard
                        = (DashClockExtensionCard) card;
                if(map.containsKey(dashClockExtensionCard
                        .getFlattenedComponentNameString())) {
                    ExtensionManager.ExtensionWithData extensionWithData
                            = map.get(dashClockExtensionCard
                                      .getFlattenedComponentNameString());
                    if (extensionWithData.latestData.visible()
                        && shouldReappear(extensionWithData.
                            listing.componentName.flattenToString())) {
                        dashClockExtensionCard
                                .updateFromExtensionWithData(extensionWithData);
                    } else {
                        cardsToRemove.add(dashClockExtensionCard);
                    }
                    map.remove(dashClockExtensionCard.getFlattenedComponentNameString());
                }
            }
        }

        // A List of cards to return that must be added
        List<CmCard> cardsToAdd = new ArrayList<CmCard>();

        // Create new cards for extensions that were not represented
        for(Map.Entry<String, ExtensionManager.ExtensionWithData> entry : map.entrySet()) {
            ExtensionManager.ExtensionWithData extension = entry.getValue();

            if(extension.latestData != null && !TextUtils.isEmpty(extension.latestData.status())) {
                DashClockExtensionCard card =
                        new DashClockExtensionCard(mContext, extension, mHostActivityContext);
                if (extension.latestData.visible()
                    && shouldReappear(extension.listing.componentName.flattenToString())) {
                    setCardSwipeAndUndoListeners(card);
                    cardsToAdd.add(card);
                }
            }
        }

        return new CardProviderUpdateResult(cardsToAdd, cardsToRemove);
    }

    private void setCardSwipeAndUndoListeners(DashClockExtensionCard card) {
        card.setOnSwipeListener(new Card.OnSwipeListener() {
            @Override
            public void onSwipe(Card card) {
                storeReappearTime(card.getId(),
                                  getReappearTimeFromNow());
            }
        });

        card.setOnUndoSwipeListListener(new Card.OnUndoSwipeListListener() {
            @Override
            public void onUndoSwipe(Card card, boolean timedOut) {
                if (!timedOut) {
                    clearReappearTime(card.getId());
                }
            }
        });
    }

    @Override
    public void updateCard(CmCard card) {
        if (!(card instanceof DashClockExtensionCard)) {
            return;
        }

        List<ExtensionManager.ExtensionWithData> extensions
                = mExtensionManager.getActiveExtensionsWithData();

        for(ExtensionManager.ExtensionWithData extension : extensions) {
            if (extension.listing.componentName.flattenToString()
                    .equals(card.getId())) {
                ((DashClockExtensionCard) card)
                        .updateFromExtensionWithData(extension);
            }
        }
    }

    public CmCard createCardForId(String id) {
        List<ExtensionManager.ExtensionWithData> extensions
                = mExtensionManager.getActiveExtensionsWithData();

        for(ExtensionManager.ExtensionWithData extension : extensions) {
            if (extension.listing.componentName.flattenToString()
                    .equals(id)
                && extension.latestData.visible()
                && shouldReappear(extension.listing.componentName.flattenToString())) {
                DashClockExtensionCard card =
                        new DashClockExtensionCard(mContext, extension,
                                                   mHostActivityContext);
                setCardSwipeAndUndoListeners(card);
                return card;
            }
        }

        return null;
    }

    @Override
    public void onExtensionsChanged(ComponentName sourceExtension) {
        if (sourceExtension != null) {
            for (CardProviderUpdateListener listener : mUpdateListeners) {
                listener.onCardProviderUpdate(sourceExtension.flattenToString(), false);
            }
        }
    }

    /**
     * Retrieves a list of all available extensions installed on the device
     * and sets mExtensionManager to track them for updates.
     */
    private void trackAllExtensions() {
        List<ComponentName> availableComponents = new ArrayList<ComponentName>();
        for(ExtensionManager.ExtensionListing listing : mExtensionManager.getAvailableExtensions()) {
           availableComponents.add(listing.componentName);
        }
        mExtensionManager.setActiveExtensions(availableComponents);
    }

    /**
     * Adds a listener for any extension updates.
     * @param listener The listener to update
     */
    @Override
    public void addOnUpdateListener(CardProviderUpdateListener listener) {
        mUpdateListeners.add(listener);
    }

    /**
     * Gets the time in the future when a card
     * should reappear, if it has been dismissed now.
     * @return The time in millis when the card should be allowed to reappear
     */
    private long getReappearTimeFromNow() {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, CARD_REAPPEAR_TIME_IN_MINUTES);
        return now.getTimeInMillis();
    }

    /**
     * Gets the stored time at which the card should be allowed to reappear.
     * @param extensionKey The DashClock extension ComponentName String that will be the key
     * @return The time in millis at which the card can reappear OR zero if no time is stored.
     */
    private long getStoredReappearTime(String extensionKey) {
        SharedPreferences preferences =
                mHostActivityContext.getSharedPreferences(EXTENSION_TIMEOUT_SHARED_PREF_FILE,
                                                          Context.MODE_PRIVATE);
        return preferences.getLong(extensionKey, 0);
    }

    private void storeReappearTime(String extensionKey, long returnTime) {
        SharedPreferences preferences =
                mHostActivityContext.getSharedPreferences(EXTENSION_TIMEOUT_SHARED_PREF_FILE,
                                                          Context.MODE_PRIVATE);
        preferences.edit().putLong(extensionKey, returnTime).apply();
    }

    private void clearReappearTime(String extensionKey) {
        SharedPreferences preferences =
                mHostActivityContext.getSharedPreferences(EXTENSION_TIMEOUT_SHARED_PREF_FILE,
                                                          Context.MODE_PRIVATE);
        preferences.edit().remove(extensionKey).apply();
    }

    /**
     * Checks if the card representing the extensionKey parameter should be allowed to appear.
     * @param extensionKey The flattened ComponentName String representing an extension.
     * @return True if the current time is after the stored reappearance time OR
     *         if there is no stored time for this extension. False if the stored time
     *         is still in the future.
     */
    private boolean shouldReappear(String extensionKey) {
        Calendar now = Calendar.getInstance();
        long reappearTime = getStoredReappearTime(extensionKey);
        boolean shouldReappear = true;
        if (reappearTime != 0) {
            Calendar reappear = Calendar.getInstance();
            reappear.setTimeInMillis(reappearTime);
            shouldReappear = now.after(reappear);
        }
        return shouldReappear;
    }
}
