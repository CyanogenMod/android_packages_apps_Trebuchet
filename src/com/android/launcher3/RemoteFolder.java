package com.android.launcher3;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by tmiller on 11/24/15.
 */
public class RemoteFolder extends Folder {

    public static final String TAG = "RemoteFolder";
    public static final String REMOTE_FOLDER_ENABLED = "remote_folder_enabled";
    private ScrollView mContentScrollView;
    private ImageView mFolderInfo;
    private TextView mFolderHelpText;
    private Button mCloseInfoButton;
    private View mFolderInfoContainer;

    private int mFolderInfoContainerHeight;
    private int mHelpTextHeight;
    private int mHelpTextWidth;
    private int mButtonHeight;
    private int mFolderInfoIconHeight;
    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs   The attribtues set containing the Workspace's customization values.
     */
    public RemoteFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates a new UserFolder, inflated from R.layout.remote_folder.
     *
     * @param context The application's context.
     *
     * @return A new UserFolder.
     */
    static RemoteFolder fromXml(Context context) {
        return (RemoteFolder) LayoutInflater.from(context).inflate(R.layout.remote_folder, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int measureSpec = MeasureSpec.UNSPECIFIED;

        mContentScrollView = (ScrollView) findViewById(R.id.scroll_view);

        mFolderInfoContainer = findViewById(R.id.folder_info_container);
        mFolderInfoContainer.measure(measureSpec, measureSpec);
        mFolderInfoContainerHeight = mFolderInfoContainer.getMeasuredHeight();

        mFolderInfo = (ImageView) findViewById(R.id.folder_info);
        mFolderInfo.measure(measureSpec, measureSpec);
        mFolderInfoIconHeight = mFolderInfo.getMeasuredHeight();
        mFolderInfo.setOnClickListener(this);

        mFolderHelpText = (TextView) findViewById(R.id.help_text_view);
        mFolderHelpText.setText(getResources().getString(R.string.recommendations_help_text));
        mFolderHelpText.measure(measureSpec, measureSpec);
        mHelpTextHeight = (mFolderHelpText.getLineHeight() * mFolderHelpText.getLineCount()) +
                mFolderHelpText.getPaddingTop() + mFolderHelpText.getPaddingBottom();
        mHelpTextWidth = mFolderHelpText.getMeasuredWidth();
        mFolderHelpText.setVisibility(GONE);

        mCloseInfoButton = (Button) findViewById(R.id.close_info_button);
        mCloseInfoButton.setText(getResources().getString(R.string.close));
        mCloseInfoButton.measure(measureSpec, measureSpec);
        mButtonHeight = mCloseInfoButton.getMeasuredHeight();
        mCloseInfoButton.setOnClickListener(this);
    }

    protected int getFolderHeight() {
        if (mFolderHelpText.getVisibility() == VISIBLE) {
            mHelpTextHeight = (mFolderHelpText.getLineHeight() * mFolderHelpText.getLineCount()) +
                    mFolderHelpText.getPaddingTop() + mFolderHelpText.getPaddingBottom();

            int height = getPaddingTop() + getPaddingBottom() + mFolderInfoIconHeight
                    + mHelpTextHeight + mButtonHeight;
            return height;
        } else {
            int height = getPaddingTop() + getPaddingBottom() + mFolderInfoIconHeight
                    + getContentAreaHeight();
            return height;

        }
    }

    private int getFolderWidth() {
        if (mFolderHelpText.getVisibility() == VISIBLE) {
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int width = Math.min(mHelpTextWidth, screenWidth - getPaddingLeft() - getPaddingRight());
            return width;
        } else {
            int width = getPaddingLeft() + getPaddingRight() + mContent.getDesiredWidth();
            return width;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO (Tyson): clean this up before merging into main CM branch
        int width = getFolderWidth();
        int height = getFolderHeight();
        int contentAreaWidthSpec = MeasureSpec.makeMeasureSpec(getContentAreaWidth(),
                MeasureSpec.EXACTLY);
        int contentAreaHeightSpec = MeasureSpec.makeMeasureSpec(getContentAreaHeight(),
                MeasureSpec.EXACTLY);

        if (LauncherAppState.isDisableAllApps()) {
            // Don't cap the height of the content to allow scrolling.
            mContent.setFixedSize(getContentAreaWidth(), mContent.getDesiredHeight());
        } else {
            mContent.setFixedSize(getContentAreaWidth(), getContentAreaHeight());
        }
        mContentScrollView.measure(contentAreaWidthSpec, contentAreaHeightSpec);

        if (mFolderHelpText.getVisibility() == VISIBLE) {
            mHelpTextHeight = (mFolderHelpText.getLineHeight() * mFolderHelpText.getLineCount()) +
                    mFolderHelpText.getPaddingTop() + mFolderHelpText.getPaddingBottom();

            mFolderHelpText.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(
                    mHelpTextHeight, MeasureSpec.EXACTLY));
            mFolderInfoContainer.measure(contentAreaWidthSpec,
                    MeasureSpec.makeMeasureSpec(
                            mFolderInfoIconHeight + mHelpTextHeight + mButtonHeight, MeasureSpec.EXACTLY));
            mCloseInfoButton.measure(contentAreaWidthSpec,
                    MeasureSpec.makeMeasureSpec(mButtonHeight, MeasureSpec.EXACTLY));
        } else {
            mHelpTextHeight = 0;
            mFolderHelpText.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(
                    mHelpTextHeight, MeasureSpec.EXACTLY));
            mFolderInfoContainer.measure(contentAreaWidthSpec,
                    MeasureSpec.makeMeasureSpec(mFolderInfoIconHeight, MeasureSpec.EXACTLY));
            mCloseInfoButton.measure(contentAreaWidthSpec,
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY));
        }

        Log.e(TAG, "onMeasure(), width: " + width +  ", height:" + height);

        setMeasuredDimension(width, height);
    }

    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            mLauncher.onClick(v);
        }

        switch (v.getId()) {
            case R.id.folder_info:
                toggleInfoPane();
                break;
            case R.id.close_info_button:
                mLauncher.closeFolder();
                break;
            default:
                break;
        }
    }

    private void toggleInfoPane() {
        if (mFolderHelpText.getVisibility() == VISIBLE) {
            // info ImageView becomes a close "X" when the help text is showing, handle accordingly
            mContentScrollView.setVisibility(VISIBLE);
            mContent.setVisibility(VISIBLE);

            mFolderHelpText.setVisibility(GONE);

            mCloseInfoButton.setVisibility(GONE);

            mFolderInfo.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_info_normal_holo));

        } else {
            // show the info to the user about remote folders, including the option to disable it
            mContentScrollView.setVisibility(GONE);
            mContent.setVisibility(GONE);

            mFolderHelpText.setVisibility(VISIBLE);

            mCloseInfoButton.setVisibility(VISIBLE);

            mFolderInfo.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_clear_normal_holo));
        }
        this.invalidate();
    }

    @Override
    public void animateClosed(boolean animate) {
        super.animateClosed(animate);
        mFolderHelpText.setVisibility(GONE);
        mCloseInfoButton.setVisibility(GONE);
        mContentScrollView.setVisibility(VISIBLE);
        mFolderInfo.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_info_normal_holo));
    }

    @Override
    public void animateOpen(Workspace workspace, int[] folderTouch) {
        super.animateOpen(workspace, folderTouch);

        mFolderHelpText.setText(getResources().getString(R.string.recommendations_help_text));
        mFolderHelpText.setVisibility(GONE);
        mCloseInfoButton.setVisibility(GONE);
        mContentScrollView.setVisibility(VISIBLE);
        mContent.setVisibility(VISIBLE);
        mFolderInfo.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher_info_normal_holo));
        this.invalidate();
    }
}
