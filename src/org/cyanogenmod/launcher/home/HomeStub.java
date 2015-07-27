/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.launcher.home;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import com.android.launcher.home.Home;
import com.android.launcher3.R;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.listener.dismiss.DefaultDismissableManager;
import org.cyanogenmod.launcher.cardprovider.CmHomeApiCardProvider;
import org.cyanogenmod.launcher.cardprovider.DashClockExtensionCardProvider;
import org.cyanogenmod.launcher.cardprovider.ICardProvider;
import org.cyanogenmod.launcher.cardprovider.ICardProvider.CardProviderUpdateResult;
import org.cyanogenmod.launcher.cards.CmCard;
import org.cyanogenmod.launcher.cards.SimpleMessageCard;

import java.util.ArrayList;
import java.util.List;

public class HomeStub implements Home {

    private static final String TAG = "HomeStub";
    private static final String NO_EXTENSIONS_CARD_ID = "noExtensions";
    private static final String BACKGROUND_THREAD_NAME = "CMHomeBackgroundThread";
    private HomeLayout mHomeLayout;
    private RecyclerView mRecyclerView;
    private Context mHostActivityContext;
    private Context mCMHomeContext;
    private boolean mShowContent = false;
    private SimpleMessageCard mNoExtensionsCard;
    private List<ICardProvider> mCardProviders = new ArrayList<ICardProvider>();
    private List<CMHomeCard> mCards;
    private CMHomeCardArrayAdapter mCardArrayAdapter;
    private LinearLayoutManager mLayoutManager;

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private Handler mUiThreadHandler;

    private final AccelerateInterpolator mAlphaInterpolator;

    private final ICardProvider.CardProviderUpdateListener mCardProviderUpdateListener =
            new ICardProvider.CardProviderUpdateListener() {
                @Override
                public boolean onCardProviderUpdate(String cardId, boolean wasPending) {
                    return refreshCard(cardId);
                }

                @Override
                public void onCardDelete(String cardId) {

                }
            };

    private final Runnable mLoadAllCardsRunnable = new Runnable() {
        @Override
        public void run() {
            loadAllCards();
        }
    };

    public HomeStub() {
        super();
        mAlphaInterpolator = new AccelerateInterpolator();
    }

    @Override
    public void setHostActivityContext(Context context) {
        mHostActivityContext = context;
        mUiThreadHandler = new Handler(mHostActivityContext.getMainLooper());
    }

    @Override
    public void onStart(Context context) {
        mCMHomeContext = context;

        // Start up a background thread to handle updating.
        mBackgroundHandlerThread = new HandlerThread(BACKGROUND_THREAD_NAME);
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());

        if(mShowContent) {
            // Add any providers we wish to include, if we should show content
            initProvidersIfNeeded(context);
        }
    }

    @Override
    public void setShowContent(Context context, boolean showContent) {
        mShowContent = showContent;
        if(mShowContent) {
            // Add any providers we wish to include, if we should show content
            initProvidersIfNeeded(context);
            if(mHomeLayout != null) {
                loadCardsFromProviders();
            }
        } else {
            for(ICardProvider cardProvider : mCardProviders) {
                cardProvider.onHide(context);
            }
            mCardProviders.clear();
            if(mHomeLayout != null) {
                removeAllCards(context);
                // Make sure that the Undo Bar is hidden if no content is to be shown.
                hideUndoBar();
            }
        }
    }

    @Override
    public void onDestroy(Context context) {
        mHomeLayout = null;
    }

    @Override
    public void onResume(Context context) {
    }

    @Override
    public void onPause(Context context) {
    }

    @Override
    public void onShow(Context context) {
        if (mHomeLayout != null) {
            mHomeLayout.setAlpha(1.0f);

            if(mShowContent) {
                for(ICardProvider cardProvider : mCardProviders) {
                    cardProvider.onShow();
                    cardProvider.requestRefresh();
                }
            } else {
                hideUndoBar();
            }
        }
    }

    @Override
    public void onScrollProgressChanged(Context context, float progress) {
        if (mHomeLayout != null) {
            mHomeLayout.setAlpha(mAlphaInterpolator.getInterpolation(progress));
        }
    }

    @Override
    public void onHide(Context context) {
        if (mHomeLayout != null) {
            mHomeLayout.setAlpha(0.0f);
        }
        for(ICardProvider cardProvider : mCardProviders) {
            cardProvider.onHide(context);
        }
    }

    @Override
    public void onInvalidate(Context context) {
        if (mHomeLayout != null) {
            mHomeLayout.removeAllViews();
        }
    }

    @Override
    public void onRequestSearch(Context context, int mode) {

    }

    @Override
    public View createCustomView(Context context) {
        if(mHomeLayout == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mHomeLayout = (HomeLayout) inflater.inflate(R.layout.home_layout, null);
        }
        hideUndoBar();

        mRecyclerView = (RecyclerView) mHomeLayout.findViewById(R.id.main_recycler_view);

        initData();

        CMHomeAdapter adapter = new CMHomeAdapter(context, mCards);
        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(adapter);

        return mHomeLayout;
    }

    private void initData(){
        Resources resources = mCMHomeContext.getResources();
        TypedArray typedArray = resources.obtainTypedArray(R.array.spoof_data); ;
        int n = typedArray.length();
        String[][] dataArray = new String[n][];

        for(int i = 0; i < n; i++){
            int id = typedArray.getResourceId(i,0);
            if(id > 0){
                dataArray[i] = resources.getStringArray(id);
            } else {
                //something is wrong
            }
        }

        typedArray.recycle();

        mCards = new ArrayList<>();

        mCards.add(new CMHomeContact(
                dataArray[0][0],
                dataArray[0][1],
                dataArray[0][2],
                dataArray[0][3]
        ));

        mCards.add(new CMHomeCalendar(
                Long.parseLong(dataArray[1][0]),
                Long.parseLong(dataArray[1][1]),
                dataArray[1][2],
                dataArray[1][3]
        ));

        for(int i = 2; i < n; i++){
            mCards.add(new CMHomeNews(
                    dataArray[i][0],
                    dataArray[i][1],
                    dataArray[i][2],
                    Long.parseLong(dataArray[i][3]),
                    dataArray[i][4]
            ));
        }

        Log.w("HAX", "Making data");
    }

    @Override
    public String getName(Context context) {
        return "HomeStub";
    }

    @Override
    public int getNotificationFlags() {
        return Home.FLAG_NOTIFY_ALL;
    }

    @Override
    public int getOperationFlags() {
        return Home.FLAG_OP_MASK;
    }

    private void hideUndoBar() {
        View undoLayout = mHomeLayout.findViewById(R.id.list_card_undobar);
        if (undoLayout != null) {
            undoLayout.setVisibility(View.GONE);
        }
    }

    public void initProvidersIfNeeded(Context context) {
        if (mCardProviders.size() == 0) {
            mCardProviders.add(new DashClockExtensionCardProvider(context, mHostActivityContext));
            mCardProviders.add(new CmHomeApiCardProvider(context, mHostActivityContext,
                                                         mBackgroundHandler));

            for (ICardProvider cardProvider : mCardProviders) {
                cardProvider.addOnUpdateListener(mCardProviderUpdateListener);
            }
        }
    }

    /*
     * Gets a list of all cards provided by each provider,
     * and updates the UI to show them.
     */
    private void loadCardsFromProviders() {
        // If cards have been initialized already, just update them
        if(mCardArrayAdapter != null
           && mCardArrayAdapter.getCards().size() > 0
           && mHomeLayout != null) {
            mBackgroundHandler.post(new RefreshAllCardsRunnable(true));
        } else {
            mBackgroundHandler.post(mLoadAllCardsRunnable);
        }
    }

    /**
     * Creates a card with a message to inform the user they have no extensions
     * installed to publish content.
     */
    private Card getNoExtensionsCard(final Context context) {
        if (mNoExtensionsCard == null) {
            mNoExtensionsCard = new SimpleMessageCard(context);
            mNoExtensionsCard.setTitle(context.getResources().getString(R.string.no_extensions_card_title));
            mNoExtensionsCard.setBody(context.getResources().getString(R.string.no_extensions_card_body));
            mNoExtensionsCard.setId(NO_EXTENSIONS_CARD_ID);
        }

        return mNoExtensionsCard;
    }

    public boolean refreshCard(String cardId) {
        boolean cardIsNew = false;
        if (mCardArrayAdapter != null) {
            CmCard card = mCardArrayAdapter.getCardWithId(cardId);

            // The card already exists in the list
            if (card != null) {
                // Allow each provider to update the card (if necessary)
                for (ICardProvider cardProvider : mCardProviders) {
                    cardProvider.updateCard(card);
                }
            } else {
                // The card is brand new, add it
                CmCard newCard = null;
                for (ICardProvider cardProvider : mCardProviders) {
                    newCard = cardProvider.createCardForId(cardId);
                    if (newCard != null) break;
                }

                if (newCard != null) {
                    card = newCard;
                    cardIsNew = true;
                }
            }

            final boolean runnableCardIsNew = cardIsNew;
            final CmCard runnableCard = card;
            mUiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (runnableCard != null) {
                        if (runnableCardIsNew) {
                            mCardArrayAdapter.add(runnableCard);
                            // Remove the "no cards" card, if it's there.
                            mCardArrayAdapter.remove(getNoExtensionsCard(mCMHomeContext));
                            mCardArrayAdapter.notifyDataSetChanged();
                        } else {
                            mCardArrayAdapter.updateCardViewIfVisible(runnableCard);
                        }
                    }
                }
            });
        }
        return cardIsNew;
    }

    private void removeAllCards(Context context) {

    }

    private void loadAllCards() {
        final List<Card> cards = new ArrayList<Card>();
        for (ICardProvider provider : mCardProviders) {
            for (Card card : provider.getCards()) {
                cards.add(card);
            }
        }

        // If there aren't any cards, show the user a message about how to fix that!
        if (cards.size() == 0) {
            cards.add(getNoExtensionsCard(mCMHomeContext));
        }

        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    /**
     * Refresh all cards by asking the providers to update them.
     * @param addNew If providers have new cards that have not
     * been displayed yet, should they be added?
     */
    private void refreshCards(final boolean addNew) {
        boolean noExtensionsCardExists;
        List<CmCard> originalCards = mCardArrayAdapter.getCards();
        int finalCardCount = 0;

        final CardProviderUpdateResult updateResult =
                    new CardProviderUpdateResult(new ArrayList<CmCard>(),
                                                 new ArrayList<CmCard>());
        // Allow each provider to update it's cards
        for (ICardProvider cardProvider : mCardProviders) {
            CardProviderUpdateResult tempResult;
            tempResult = cardProvider.updateAndAddCards(originalCards);
            updateResult.getCardsToAdd().addAll(tempResult.getCardsToAdd());
            updateResult.getCardsToRemove().addAll(tempResult.getCardsToRemove());
        }

        noExtensionsCardExists = originalCards.contains(mNoExtensionsCard);

        if (updateResult != null) {
            finalCardCount += updateResult.getCardsToAdd().size();
            finalCardCount -= updateResult.getCardsToRemove().size();
        }

        final boolean runnableNoExtensionCard = noExtensionsCardExists;
        final int runnableFinalCardCount = finalCardCount;
        mUiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (updateResult != null) {
                    if (addNew) {
                        mCardArrayAdapter.addAll(updateResult.getCardsToAdd());
                    }
                    for (Card card : updateResult.getCardsToRemove()) {
                        mCardArrayAdapter.remove(card);
                    }
                }

                if (runnableNoExtensionCard && runnableFinalCardCount > 1) {
                    mCardArrayAdapter.remove(mNoExtensionsCard);
                }
            }
        });

    }

    public class CMHomeCardArrayAdapter extends CardArrayAdapter {

        public CMHomeCardArrayAdapter(Context context, List<Card> cards) {
            super(context, cards);
        }

        public List<CmCard> getCards() {
            List<CmCard> cardsToReturn = new ArrayList<CmCard>();
            for(int i = 0; i < getCount(); i++) {
                cardsToReturn.add((CmCard)getItem(i));
            }
            return cardsToReturn;
        }

        public CmCard getCardWithId(String id) {
            CmCard theCard = null;
            for(int i = 0; i < getCount(); i++) {
                CmCard card = (CmCard) getItem(i);
                if (card.getId().equals(id)) {
                    theCard = card;
                    break;
                }
            }
            return theCard;
        }

        /**
         * Find the CardView displaying the card that has changed
         * and update it, if it is currently on screen. Otherwise,
         * do nothing.
         * @param card The card object to re-draw onscreen.
         */
        public void updateCardViewIfVisible(Card card) {
            CardListView listView = getCardListView();
            int start = listView.getFirstVisiblePosition();
            int last = listView.getLastVisiblePosition();
            for (int i = start; i <= last; i++) {
                if (card == listView.getItemAtPosition(i)) {
                    View cardView = listView.getChildAt(i - start);
                    getView(i, cardView, listView);
                    break;
                }
            }
        }
    }

    private class RefreshAllCardsRunnable implements Runnable {
        private boolean mAddNew = false;

        private RefreshAllCardsRunnable(boolean addNew) {
            mAddNew = addNew;
        }

        @Override
        public void run() {
            refreshCards(mAddNew);
        }
    }

    /**
     * A DismissableManager implementation that only allows cards to be swiped to the right.
     */
    private class RightDismissableManager extends DefaultDismissableManager {
        @Override
        public SwipeDirection getSwipeDirectionAllowed() {
            return SwipeDirection.RIGHT;
        }
    }
}
