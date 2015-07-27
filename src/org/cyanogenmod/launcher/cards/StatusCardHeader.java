package org.cyanogenmod.launcher.cards;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.launcher3.R;
import it.gmariotti.cardslib.library.internal.CardHeader;
import org.cyanogenmod.launcher.home.api.cards.CardData;

import java.util.Date;

/**
 * The header for a status card, backed by a CM Home API CardData.
 */
public class StatusCardHeader extends CardHeader {
    private CardData mCardData;

    public StatusCardHeader(Context context, CardData cardData) {
        super(context, R.layout.card_status_header_inner);
        setCardData(cardData);
    }

    private void setCardData(CardData cardData) {
        mCardData = cardData;
    }

    private CardData getCardData() {
        return mCardData;
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        if (view != null && getCardData() != null) {
            TextView titleTv = (TextView) view.findViewById(R.id.status_card_title);
            String title = getCardData().getTitle();
            if (titleTv != null && !TextUtils.isEmpty(title)) {
                titleTv.setText(title);
            }

            TextView dateTv = (TextView) view.findViewById(R.id.status_card_date);
            Date contentCreatedDate = getCardData().getContentCreatedDate();
            String dateString = DateUtils.getRelativeTimeSpanString(contentCreatedDate.getTime(),
                                                            System.currentTimeMillis(),
                                                            DateUtils.SECOND_IN_MILLIS).toString();
            if (dateTv != null && !TextUtils.isEmpty(dateString)) {
                dateTv.setText(dateString);
            }
        }
    }
}
