package wycliffeassociates.recordingapp.ProjectManager;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;

import java.util.ArrayList;
import java.util.List;

import wycliffeassociates.recordingapp.R;
import wycliffeassociates.recordingapp.widgets.FourStepImageView;
import wycliffeassociates.recordingapp.widgets.UnitCard;

/**
 * Created by leongv on 7/28/2016.
 */
public class UnitCardAdapter extends RecyclerView.Adapter<UnitCardAdapter.ViewHolder> {

    private AppCompatActivity mCtx;
    private Project mProject;
    private int mChapterNum;
    private List<UnitCard> mUnitCardList;
    private List<Integer> mExpandedCards = new ArrayList<>();
    private List<ViewHolder> mSelectedCards = new ArrayList<>();
    private MultiSelector mMultiSelector = new MultiSelector();
    private ActionMode mActionMode;


    // Constructor
    public UnitCardAdapter(AppCompatActivity context, Project project, int chapter, List<UnitCard> unitCardList) {
        mUnitCardList = unitCardList;
        mCtx = context;
        mProject = project;
        mChapterNum = chapter;
    }


    private ActionMode.Callback mMultiSelectMode = new ModalMultiSelectorCallback(mMultiSelector) {

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            mMultiSelector.clearSelections();
            mMultiSelector.setSelectable(true);
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            mCtx.getMenuInflater().inflate(R.menu.unit_menu, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.set_units_checking_level:
                    System.out.println("Placeholder: Set checking level for selected units");
                    break;
                default:
                    System.out.println("Default action");
                    break;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            mMultiSelector.setSelectable(false);
            mMultiSelector.clearSelections();
            for (ViewHolder vh : mSelectedCards) {
                vh.mUnitCard.drop(vh);
            }
            mSelectedCards.clear();
        }
    };


    public class ViewHolder extends SwappingHolder implements View.OnClickListener,
            View.OnLongClickListener {

        public RelativeLayout mCardHeader, mCardFooter;
        public SeekBar mSeekBar;
        public TextView mUnitTitle, mCurrentTake, mProgress, mDuration, mCurrentTakeTimeStamp;
        public LinearLayout mCardBody, mCardContainer, mUnitActions;
        public ImageView mUnitRecordBtn, mUnitExpandBtn, mPrevTakeBtn, mNextTakeBtn;
        public ImageButton mTakeDeleteBtn, mTakePlayPauseBtn, mTakeEditBtn, mTakeSelectBtn;
        public FourStepImageView mTakeRatingBtn;
        public UnitCard mUnitCard;
        public CardView mCardView;

        public ViewHolder(View view) {
            super(view, mMultiSelector);
            // Containers
            mCardView = (CardView) view.findViewById(R.id.unitCard);
            mCardContainer = (LinearLayout) view.findViewById(R.id.unitCardContainer);
            mCardHeader = (RelativeLayout) view.findViewById(R.id.cardHeader);
            mCardBody = (LinearLayout) view.findViewById(R.id.cardBody);
            mCardFooter = (RelativeLayout) view.findViewById(R.id.cardFooter);
            mUnitActions = (LinearLayout) view.findViewById(R.id.unitActions);

            // Views
            mUnitTitle = (TextView) view.findViewById(R.id.unitTitle);
            mCurrentTake = (TextView) view.findViewById(R.id.currentTakeView);
            mCurrentTakeTimeStamp = (TextView) view.findViewById(R.id.currentTakeTimeStamp);
            mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
            mProgress = (TextView) view.findViewById(R.id.timeElapsed);
            mDuration = (TextView) view.findViewById(R.id.timeDuration);
            mCurrentTakeTimeStamp = (TextView) view.findViewById(R.id.currentTakeTimeStamp);

            // Buttons
            mTakeRatingBtn = (FourStepImageView) view.findViewById(R.id.rateTakeBtn);
            mUnitRecordBtn = (ImageView) view.findViewById(R.id.unitRecordBtn);
            mUnitExpandBtn = (ImageView) view.findViewById(R.id.unitExpandBtn);
            mTakeDeleteBtn = (ImageButton) view.findViewById(R.id.deleteTakeBtn);
            mTakePlayPauseBtn = (ImageButton) view.findViewById(R.id.playTakeBtn);
            mTakeEditBtn = (ImageButton) view.findViewById(R.id.editTakeBtn);
            mTakeSelectBtn = (ImageButton) view.findViewById(R.id.selectTakeBtn);
            mPrevTakeBtn = (ImageView) view.findViewById(R.id.prevTakeBtn);
            mNextTakeBtn = (ImageView) view.findViewById(R.id.nextTakeBtn);

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            view.setLongClickable(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setSelectionModeStateListAnimator(null);
                setDefaultModeStateListAnimator(null);
            }
            setSelectionModeBackgroundDrawable(null);
            setDefaultModeBackgroundDrawable(null);
        }

        // Called on onBindViewHolder, when the view is visible on the screen
        public void  bindViewHolder(ViewHolder holder, int position, UnitCard unitCard) {
            // Capture the UnitCard object
            mUnitCard = unitCard;
            // Set card views based on the UnitCard object
            mUnitTitle.setText(unitCard.getTitle());
            // Expand card if it's already expanded before
            if (unitCard.isExpanded()) {
                unitCard.expand(holder);
            } else {
                unitCard.collapse(holder);
            }
            // Raise card, and show appropriate visual cue, if it's already selected
            if (mMultiSelector.isSelected(position, 0)) {
                mSelectedCards.add(this);
                unitCard.raise(holder);
            } else {
                mSelectedCards.remove(this);
                unitCard.drop(holder);
            }

            // Hide expand icon if it's empty
            if (unitCard.isEmpty()) {
                mUnitExpandBtn.setVisibility(View.INVISIBLE);
            } else {
                mUnitExpandBtn.setVisibility(View.VISIBLE);
            }

            setListeners(mUnitCard, this);
        }

        @Override
        public void onClick(View view) {
            // NOTE: There is no action that needs multi-select at unit level at this point
//            if (mUnitCard == null) {
//                return;
//            }
//
//            if(mMultiSelector.isSelectable() && !mUnitCard.isEmpty()) {
//
//                // Close card if it is expanded in multi-select mode
//                if(mUnitCard.isExpanded()){
//                    toggleExpansion(this, mExpandedCards);
//                }
//
//                // Select/de-select item
//                mMultiSelector.tapSelection(this);
//
//                // Raise/drop card
//                if (mMultiSelector.isSelected(this.getAdapterPosition(), 0)) {
//                    mSelectedCards.add(this);
//                    mUnitCard.raise(this);
//                } else {
//                    mSelectedCards.remove(this);
//                    mUnitCard.drop(this);
//                }
//
//                // Finish action mode if all cards are de-selected
//                if (mActionMode != null && mSelectedCards.size() <= 0) {
//                    mActionMode.finish();
//                }
//            }
        }

        @Override
        public boolean onLongClick(View view) {
            // NOTE: There is no action that needs multi-select at unit level at this point
//            if (!mUnitCard.isEmpty()) {
//                mActionMode = mCtx.startSupportActionMode(mMultiSelectMode);
//                mMultiSelector.setSelected(this, true);
//
//                // Close card if it is expanded on entering multi-select mode
//                if(mUnitCard.isExpanded()){
//                    toggleExpansion(this, mExpandedCards);
//                }
//
//                mSelectedCards.add(this);
//                mUnitCard.raise(this);
//            }
            return true;
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.unit_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UnitCard unitCard = mUnitCardList.get(position);
        holder.bindViewHolder(holder, position, unitCard);
    }

    @Override
    public int getItemCount() {
        return mUnitCardList.size();
    }


    public List<UnitCard> getSelectedCards() {
        List<UnitCard> cards = new ArrayList<>();
        for (int i = getItemCount(); i >= 0; i--) {
            if (mMultiSelector.isSelected(i, 0)) {
                cards.add(mUnitCardList.get(i));
            }
        }
        return cards;
    }

    private void setListeners(final UnitCard unitCard, final ViewHolder holder) {
        holder.mUnitRecordBtn.setOnClickListener(unitCard.getUnitRecordOnClick(holder));
        holder.mUnitExpandBtn.setOnClickListener(unitCard.getUnitExpandOnClick(holder, mExpandedCards));
        holder.mTakeDeleteBtn.setOnClickListener(unitCard.getTakeDeleteOnClick(holder, this));
        holder.mTakePlayPauseBtn.setOnClickListener(unitCard.getTakePlayPauseOnClick(holder));
        holder.mTakeEditBtn.setOnClickListener(unitCard.getTakeEditOnClickListener(holder));
        holder.mTakeRatingBtn.setOnClickListener(unitCard.getTakeRatingOnClick(holder));
        holder.mTakeSelectBtn.setOnClickListener(unitCard.getTakeSelectOnClick(holder));
        holder.mNextTakeBtn.setOnClickListener(unitCard.getTakeIncrementOnClick(holder));
        holder.mPrevTakeBtn.setOnClickListener(unitCard.getTakeDecrementOnClick(holder));
    }

    public void exitCleanUp() {
        for (UnitCard uc : mUnitCardList) {
            if (uc.isExpanded()) {
                uc.destroyAudioPlayer();
            }
        }
    }

}
