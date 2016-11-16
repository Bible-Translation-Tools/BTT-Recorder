package wycliffeassociates.recordingapp.Playback.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import wycliffeassociates.recordingapp.Playback.interfaces.MediaController;
import wycliffeassociates.recordingapp.Playback.interfaces.VerseMarkerModeToggler;
import wycliffeassociates.recordingapp.R;
import wycliffeassociates.recordingapp.Utils;
import wycliffeassociates.recordingapp.widgets.PlaybackTimer;

/**
 * Created by sarabiaj on 11/15/2016.
 */

public class MarkerToolbarFragment extends Fragment {

    private ImageView mPlaceMarker;

    public interface OnMarkerPlacedListener {
        void onMarkerPlaced();
    }

    private OnMarkerPlacedListener mOnMarkerPlacedListener;
    private VerseMarkerModeToggler mModeToggleCallback;
    private MediaController mMediaController;
    private ImageButton mPlayBtn;
    private ImageButton mPauseBtn;
    private ImageButton mSkipBackBtn;
    private ImageButton mSkipForwardBtn;
    private TextView mPlaybackElapsed;
    private TextView mPlaybackDuration;
    private PlaybackTimer mTimer;


    public static MarkerToolbarFragment newInstance(){
        MarkerToolbarFragment f = new MarkerToolbarFragment();
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mModeToggleCallback = (VerseMarkerModeToggler) activity;
        mMediaController = (MediaController) activity;
        mOnMarkerPlacedListener = (OnMarkerPlacedListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_marker_toolbar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews();
        initViews();
        initTimer(mPlaybackElapsed, mPlaybackDuration);
    }

    private void findViews(){
        View view = getView();
        mPlayBtn = (ImageButton) view.findViewById(R.id.btn_play);
        mPauseBtn = (ImageButton) view.findViewById(R.id.btn_pause);
        mSkipBackBtn = (ImageButton) view.findViewById(R.id.btn_skip_back);
        mSkipForwardBtn = (ImageButton) view.findViewById(R.id.btn_skip_forward);

        mPlaybackElapsed = (TextView) view.findViewById(R.id.playback_elapsed);
        mPlaybackDuration = (TextView) view.findViewById(R.id.playback_duration);

        mPlaceMarker = (ImageView) view.findViewById(R.id.btn_drop_verse_marker);
    }

    private void initViews(){
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.swapViews(new View[]{mPauseBtn}, new View[]{mPlayBtn});
                mMediaController.onMediaPlay();
            }
        });

        mPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.swapViews(new View[]{mPlayBtn}, new View[]{mPauseBtn});
                mMediaController.onMediaPause();
            }
        });

        mSkipBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMediaController.onSeekBackward();
            }
        });

        mSkipForwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMediaController.onSeekForward();
            }
        });

        mPlaceMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnMarkerPlacedListener.onMarkerPlaced();
            }
        });
    }

    private void initTimer(final TextView elapsed, final TextView duration) {
        mTimer = new PlaybackTimer(elapsed, duration);
        mTimer.setElapsed(0);
        mTimer.setDuration(mMediaController.getDuration());
    }

    public void onLocationUpdated(int ms){
        mTimer.setElapsed(ms);
    }

    public void onDurationUpdated(int ms){
        mTimer.setDuration(ms);
    }
}
