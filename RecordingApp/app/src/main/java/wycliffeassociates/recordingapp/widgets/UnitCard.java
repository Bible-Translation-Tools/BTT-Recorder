package wycliffeassociates.recordingapp.widgets;

import android.content.Context;
import android.os.Build;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

import wycliffeassociates.recordingapp.ProjectManager.Project;
import wycliffeassociates.recordingapp.ProjectManager.UnitCardAdapter;
import wycliffeassociates.recordingapp.R;
import wycliffeassociates.recordingapp.Recording.RecordingScreen;

/**
 * Created by leongv on 7/28/2016.
 */
public class UnitCard {

    private final String mFirstVerse;

    public interface OnClickListener extends View.OnClickListener{
        void onClick(View v, UnitCardAdapter.ViewHolder vh, List<Integer> expandedCards, int position);
    }

    // State
    private boolean mIsExpanded = false;

    // Attributes
    private String mTitle;
    private Context mCtx;



    // Constructors
    public UnitCard(Context ctx, String mode, String firstVerse) {
        mTitle = mode + " " + firstVerse;
        mFirstVerse = firstVerse;
        mCtx = ctx;
    }



    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public void expand(UnitCardAdapter.ViewHolder vh) {
        vh.mCardBody.setVisibility(View.VISIBLE);
        vh.mCardFooter.setVisibility(View.VISIBLE);
        vh.mUnitPlayBtn.setVisibility(View.GONE);
        mIsExpanded = true;
    }

    public void collapse(UnitCardAdapter.ViewHolder vh) {
        vh.mCardBody.setVisibility(View.GONE);
        vh.mCardFooter.setVisibility(View.GONE);
        vh.mUnitPlayBtn.setVisibility(View.VISIBLE);
        mIsExpanded = false;
    }

    public void raise(UnitCardAdapter.ViewHolder vh) {
        vh.mCardView.setCardElevation(8f);
        vh.mCardContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.accent));
        vh.mUnitTitle.setTextColor(mCtx.getResources().getColor(R.color.text_light));

        // Different implementations that are based on API version. Kind off ridiculous.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vh.mUnitRecordBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_record, mCtx.getTheme())
            );
            vh.mUnitPlayBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_play_white, mCtx.getTheme())
            );
        } else {
            vh.mUnitRecordBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_record)
            );
            vh.mUnitPlayBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_play_white)
            );
        }
    }

    public void drop(UnitCardAdapter.ViewHolder vh) {
        vh.mCardView.setCardElevation(2f);
        vh.mCardContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.card_bg));
        vh.mUnitTitle.setTextColor(
            mCtx.getResources().getColor(R.color.primary_text_default_material_light)
        );

        // Different implementations that are based on API version. Possibly worse than browser
        // compatibility.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vh.mUnitRecordBtn.setImageDrawable(
                mCtx.getResources().getDrawable(
                    R.drawable.ic_microphone_grey600_36dp, mCtx.getTheme()
                )
            );
            vh.mUnitPlayBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_play_blue, mCtx.getTheme())
            );
        } else {
            vh.mUnitRecordBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_microphone_grey600_36dp)
            );
            vh.mUnitPlayBtn.setImageDrawable(
                mCtx.getResources().getDrawable(R.drawable.ic_play_blue)
            );
        }
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public View.OnClickListener getUnitRecordOnClick(final Project project, final int chapter) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Project.loadProjectIntoPreferences(view.getContext(), project);
                int startVerse = Integer.parseInt(mFirstVerse);
                view.getContext().startActivity(RecordingScreen.getNewRecordingIntent(view.getContext(), project, chapter, startVerse));
            }
        };
    }

}