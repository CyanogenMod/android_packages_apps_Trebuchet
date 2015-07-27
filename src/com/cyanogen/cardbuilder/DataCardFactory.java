package com.cyanogen.cardbuilder;

import android.content.Context;
import android.text.TextUtils;
import it.gmariotti.cardslib.library.internal.Card;
import org.cyanogenmod.launcher.cards.StatusCard;
import org.cyanogenmod.launcher.home.api.cards.CardData;

public class DataCardFactory {
    public static Card createCard(Context context, CardData cardData) {
        Card card = null;
        if (cardDataCanDisplayAsStatusCard(cardData)) {
            card = new StatusCard(context, cardData);
        }
        return card;
    }

    private static boolean cardDataCanDisplayAsStatusCard(CardData cardData) {
        return !TextUtils.isEmpty(cardData.getTitle()) &&
               !TextUtils.isEmpty(cardData.getBodyText());
    }
}
