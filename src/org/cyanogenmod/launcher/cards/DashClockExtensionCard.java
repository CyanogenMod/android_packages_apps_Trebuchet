package org.cyanogenmod.launcher.cards;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.launcher3.R;
import com.google.android.apps.dashclock.api.ExtensionData;

import it.gmariotti.cardslib.library.internal.Card;
import org.cyanogenmod.launcher.dashclock.ExtensionManager;

import java.io.FileNotFoundException;

import it.gmariotti.cardslib.library.internal.CardExpand;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.ViewToClickToExpand;

/**
 * This class provides a card that will represent a DashClock Extension
 */
public class DashClockExtensionCard extends CmCard {
    private final static String TAG = "DashClockExtensionCard";
    private ExtensionManager.ExtensionWithData mExtensionWithData;
    private Context mHostActivityContext;
    private String mFlattenedComponentNameString = "";

    public DashClockExtensionCard(Context context,
                                  ExtensionManager.ExtensionWithData extensionWithData,
                                  Context hostActivityContext) {
        this(context, extensionWithData,
             R.layout.dashclock_card_inner_content, hostActivityContext);
    }

    public DashClockExtensionCard(Context context,
                                  ExtensionManager.ExtensionWithData extensionWithData,
                                  int innerLayout,
                                  Context hostActivityContext) {
        super(context, innerLayout);
        mExtensionWithData = extensionWithData;
        mHostActivityContext = hostActivityContext;
        init();
    }

    private void init() {
        // Track the ComponentName of the extension driving this card
        mFlattenedComponentNameString
                = mExtensionWithData.listing.componentName.flattenToString();

        //Add Header
        CardHeader header = new CardHeader(getContext());
        header.setTitle(getHeaderTitleFromExtension());
        addCardHeader(header);

        addCardIcon();

        DashClockCardExpand cardExpand = new DashClockCardExpand(getContext());
        cardExpand.onExtensionUpdate();
        addCardExpand(cardExpand);

        setSwipeable(true);

        setId(mFlattenedComponentNameString);
    }

    @Override
    public void onUndoSwipe(Card card, boolean timedOut) {
        // TODO Store the ID of the card that was swiped, so we can not bring it back unless
        // we want to for some new reason
    }

    public void updateFromExtensionWithData(ExtensionManager.ExtensionWithData extensionWithData) {
        if(TextUtils.isEmpty(extensionWithData.latestData.expandedBody())
            && TextUtils.isEmpty(extensionWithData.latestData.status())
            && TextUtils.isEmpty(extensionWithData.latestData.expandedTitle())) {
            // Empty update, don't continue.
            return;
        }
        mExtensionWithData = extensionWithData;

        init();
    }

    private void addCardIcon() {
        ExtensionData data = mExtensionWithData.latestData;
        if(getCardThumbnail() == null
           && (data.iconUri() != null
                || data.icon() > 0)) {
            CardThumbnail thumbnail = new DashClockThumbnail(mContext);
            thumbnail.setCustomSource(new DashClockIconCardThumbnailSource(mContext, mExtensionWithData.listing.componentName, data));
            addCardThumbnail(thumbnail);
        } else if (data.iconUri() != null || data.icon() > 0) {
            CardThumbnail thumbnail = getCardThumbnail();
            DashClockIconCardThumbnailSource thumbnailSource =
                    (DashClockIconCardThumbnailSource) thumbnail.getCustomSource();
            thumbnailSource.setExtensionData(data);
            boolean shouldUpdate = thumbnailSource.shouldUpdateThumbnail();
            if (shouldUpdate) {
                addCardThumbnail(thumbnail);
            }
        }
    }

    public String getFlattenedComponentNameString() {
        return mFlattenedComponentNameString;
    }

    private String getHeaderTitleFromExtension() {
        ExtensionData data = mExtensionWithData.latestData;
        String title = "";

        if(!TextUtils.isEmpty(mExtensionWithData.listing.title)) {
            title = mExtensionWithData.listing.title;
        } else if(!TextUtils.isEmpty(data.expandedTitle())) {
            title = data.expandedTitle();
        } else if(!TextUtils.isEmpty(data.status())) {
            title = data.status();
        }
        return title;
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        setupInnerView(view);
    }

    public void setupInnerView(View view) {
        TextView titleTextView = (TextView) view.findViewById(R.id.dashclock_card_inner_title_text);
        TextView statusTextView = (TextView) view.findViewById(R.id.dashclock_card_inner_status_text);
        TextView bodyTextView = (TextView) view.findViewById(R.id.dashclock_card_inner_body_text);

        String title = mExtensionWithData.latestData.expandedTitle();
        String status = mExtensionWithData.latestData.status();
        String body = mExtensionWithData.latestData.expandedBody();

        if(TextUtils.isEmpty(title) && !TextUtils.isEmpty(status)) {
            titleTextView.setText(status);
            statusTextView.setVisibility(View.GONE);
        } else {
            titleTextView.setText(title);
            statusTextView.setText(status);
            statusTextView.setVisibility(View.VISIBLE);
        }

        bodyTextView.setText(body);

        // Clicking the card expands it, if one of the buttons is enabled
        DashClockCardExpand cardExpand = (DashClockCardExpand)getCardExpand();
        if (!cardExpand.getActionEnabled() && !cardExpand.getSettingsEnabled()) {
            setViewToClickToExpand(null);
        } else {
            ViewToClickToExpand viewToClickToExpand =
                    ViewToClickToExpand.builder().setupView(getCardView());
            setViewToClickToExpand(viewToClickToExpand);
        }
    }

    private static class DashClockThumbnail extends CardThumbnail {
        private static int[] sIconBackgroundColors;

        private static int sCurrentIconColorIndex = 0;
        private int mIconColorIndex = -1;

        public DashClockThumbnail(Context context) {
            super(context);
            sIconBackgroundColors =
                    context.getResources().getIntArray(R.array.icon_background_colors);

            // Assign this card a color, incrementing the static ongoing color index
            if(mIconColorIndex == -1) {
                mIconColorIndex = sCurrentIconColorIndex++ % sIconBackgroundColors.length;
            }
        }

        @Override
        public void setupInnerViewElements(ViewGroup parent, View viewImage) {
            ImageView image= (ImageView) viewImage;

            // Pick the next background color for the icon.
            // Choose the color in the order they appear in ICON_BACKGROUND_COLORS.
            int color = sIconBackgroundColors[mIconColorIndex];
            image.setBackgroundColor(color);
        }
    }

    private static class DashClockIconCardThumbnailSource implements CardThumbnail.CustomSource {
        private final static float[] WHITE_COLOR_MATRIX = new float[] {
                                1f, 1f, 1f, 0, 0,
                                1f, 1f, 1f, 0, 0,
                                1f, 1f, 1f, 0, 0,
                                0, 0, 0, 1f, 0
        };

        Context mContext;
        ComponentName mComponentName;
        ExtensionData mExtensionData;
        // A String representing the source of this image, specific to dashclock extensions
        // This String will represent either a URI or a resource id int.
        private String mImageSource;

        public DashClockIconCardThumbnailSource(Context context,
                                                ComponentName componentName,
                                                ExtensionData extensionData) {
            mContext = context;
            mComponentName = componentName;
            mExtensionData = extensionData;
        }

        @Override
        public String getTag() {
            return mComponentName.flattenToShortString() + getImageSource();
        }

        @Override
        public Bitmap getBitmap() {
            Bitmap bitmapToReturn;
            // As per the DashClock documentation, prefer the iconUri resource.
            if(mExtensionData.iconUri() != null) {
                bitmapToReturn = getBitmapFromUri(mExtensionData.iconUri());
                mImageSource = mExtensionData.iconUri().toString();
            } else {
                bitmapToReturn = getIconFromResId(mExtensionData.icon());
                mImageSource = Integer.toString(mExtensionData.icon());
            }
            // Return an all white (leaving alpha alone) version of the icon.
            return applyWhiteColorFilter(bitmapToReturn);
        }

        private void updateImageSource() {
            // As per the DashClock documentation, prefer the iconUri resource.
            if(mExtensionData.iconUri() != null) {
                mImageSource = mExtensionData.iconUri().toString();
            } else {
                mImageSource = Integer.toString(mExtensionData.icon());
            }
        }

        public String getImageSource() {
            updateImageSource();
            return mImageSource;
        }

        public void setExtensionData(ExtensionData extensionData) {
            mExtensionData = extensionData;
        }

        public boolean shouldUpdateThumbnail() {
            boolean hasNewUri = (mExtensionData.iconUri() != null
                                 && !mExtensionData.iconUri().toString().equals(getImageSource()));

            boolean hasNewResId = !hasNewUri
                                  && mExtensionData.icon() > 0
                                  && !Integer.toString(mExtensionData.icon()).
                                                        equals(getImageSource());

            return hasNewUri || hasNewResId;
        }

        private Bitmap getIconFromResId(int resId) {
            String packageName = mComponentName.getPackageName();
            try {
                Context packageContext = mContext.createPackageContext(packageName, 0);
                Resources packagesRes = packageContext.getResources();
                return BitmapFactory.decodeResource(packagesRes, resId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "DashClock icon could not be loaded from package: " + packageName);
            }
            return null;
        }

        private Bitmap getBitmapFromUri(Uri uri) {
            ParcelFileDescriptor iconFd;
            Bitmap icon = null;
            try {
                iconFd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                icon = BitmapFactory.decodeFileDescriptor(iconFd.getFileDescriptor());
            } catch (FileNotFoundException e) {
                Log.w(TAG, "DashClock icon could not be loaded: " + uri);
            }
            return icon;
        }

        /**
         * The DashClock extension docs say that icons should be all white
         * with a transparent background, but I have found that many do not
         * respect this. This method corrects that by changing any non-transparent pixel to
         * white, leaving alpha values alone.
         * @param bitmap The input bitmap to color.
         * @return A copy of the original bitmap, colored to white.
         */
        private Bitmap applyWhiteColorFilter(Bitmap bitmap) {
            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Paint paint = new Paint();
            ColorMatrixColorFilter matrixColorFilter = new ColorMatrixColorFilter(WHITE_COLOR_MATRIX);
            paint.setColorFilter(matrixColorFilter);
            Canvas canvas = new Canvas(mutableBitmap);
            canvas.drawBitmap(mutableBitmap, 0, 0, paint);
            return mutableBitmap;
        }
    }

    private class DashClockCardExpand extends CardExpand {
        Context mContext;
        boolean mSettingsEnabled = false;
        boolean mActionEnabled = false;

        public DashClockCardExpand(Context context) {
            super(context, R.layout.dashclock_card_expand_inner_content);
            mContext = context;
        }

        private Intent getClickIntent() {
            Intent clickIntent = null;
            if(mExtensionWithData.latestData.clickIntent() != null) {
                clickIntent = mExtensionWithData.latestData.clickIntent();
                clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            return clickIntent;
        }

        private Intent getSettingsIntent() {
            Intent settingsIntent = null;
            if (mExtensionWithData.listing.settingsActivity != null) {
                settingsIntent = new Intent();
                settingsIntent.setComponent(mExtensionWithData.listing.settingsActivity);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            return settingsIntent;
        }

        public boolean getSettingsEnabled() {
            return mSettingsEnabled;
        }

        public boolean getActionEnabled() {
            return mActionEnabled;
        }

        public void onExtensionUpdate() {
            mActionEnabled = isIntentSupported(mContext, getClickIntent());
            mSettingsEnabled = isIntentSupported(mContext, getSettingsIntent());
        }

        private void showNoActivityFoundToast() {
            String message =
                    mContext.getResources().
                                 getString(R.string.dashclock_activity_not_found_toast_message);
            Toast.makeText(mHostActivityContext,
                           message,
                           Toast.LENGTH_SHORT).show();
        }

        @Override
        public void setupInnerViewElements(ViewGroup parent, View view) {
            if (view == null) return;

            Button clickButton =
                    (Button) view.findViewById(R.id.dashclock_card_expand_action_button);

            if (clickButton != null) {
                if(!isIntentSupported(mContext, getClickIntent())) {
                    mActionEnabled = false;
                    clickButton.setVisibility(View.GONE);
                } else {
                    mActionEnabled = true;
                    clickButton.setVisibility(View.VISIBLE);
                    clickButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent clickIntent = getClickIntent();
                            if (clickIntent != null) {
                                startIntentIfSupported(clickIntent);
                            }
                        }
                    });
                }
            }

            Button settingsButton =
                    (Button) view.findViewById(R.id.dashclock_card_expand_settings_button);

            if (settingsButton != null) {
                if(!isIntentSupported(mContext, getSettingsIntent())) {
                    mSettingsEnabled = false;
                    settingsButton.setVisibility(View.GONE);
                } else {
                    mSettingsEnabled = true;
                    settingsButton.setVisibility(View.VISIBLE);
                    settingsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent settingsIntent = getSettingsIntent();
                            if (settingsIntent != null) {
                                startIntentIfSupported(settingsIntent);
                            }
                        }
                    });
                }
            }
        }

        private void startIntentIfSupported(Intent intent) {
            try {
                if (isIntentSupported(mContext, intent)) {
                    mContext.startActivity(intent);
                } else {
                    showNoActivityFoundToast();
                }
            } catch (ActivityNotFoundException e) {
                showNoActivityFoundToast();
            } catch (SecurityException e) {
                // The extension linked to an Activity that
                // we don't have permission to launch
                showNoActivityFoundToast();
            }
        }

        private boolean isIntentSupported(Context context, Intent intent) {
            if(intent == null || context == null) {
                return false;
            }

            PackageManager pm = context.getPackageManager();
            return pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null;
        }
    }
}
