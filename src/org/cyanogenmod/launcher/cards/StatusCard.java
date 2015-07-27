package org.cyanogenmod.launcher.cards;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.launcher3.R;
import it.gmariotti.cardslib.library.internal.Card;
import org.cyanogenmod.launcher.home.api.cards.CardData;

/**
 * A card for a text based message, such as a social media status.
 */
public class StatusCard extends ApiCard {
    private String mStatus = "";

    public StatusCard(Context context, CardData cardData) {
        this(context, R.layout.status_card_inner_content, cardData);
        StatusCardHeader header = new StatusCardHeader(context, cardData);
        addCardHeader(header);
        setStatus(cardData.getBodyText());
    }

    public StatusCard(final Context context, int innerLayout, CardData cardData) {
        super(context, innerLayout, cardData);
        setSwipeable(true);
    }

    @Override
    public void onUndoSwipe(Card card, boolean timedOut) {
        // TODO implement undo handling
    }

    private void setStatus(String status) {
        mStatus = status;
    }

    public String getStatus() {
        return mStatus;
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        TextView status = (TextView)view.findViewById(R.id.status_card_status_text);

        if (status != null) {
            status.setText(Html.fromHtml(getStatus()));
        }
    }
}
