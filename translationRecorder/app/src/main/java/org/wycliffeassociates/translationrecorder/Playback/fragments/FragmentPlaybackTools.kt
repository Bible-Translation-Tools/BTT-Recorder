package org.wycliffeassociates.translationrecorder.Playback.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wycliffeassociates.translationrecorder.Playback.interfaces.AudioEditDelegator;
import org.wycliffeassociates.translationrecorder.Playback.interfaces.EditStateInformer;
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaController;
import org.wycliffeassociates.translationrecorder.Utils;

import org.wycliffeassociates.translationrecorder.databinding.FragmentPlayerToolbarBinding;
import org.wycliffeassociates.translationrecorder.widgets.PlaybackTimer;

/**
 * Created by sarabiaj on 11/4/2016.
 */

public class FragmentPlaybackTools extends Fragment {

    private FragmentPlayerToolbarBinding binding;

    MediaController mMediaController;
    AudioEditDelegator mAudioEditDelegator;

    private PlaybackTimer mTimer;
    private boolean mUndoVisible = false;

    public static FragmentPlaybackTools newInstance() {
        return new FragmentPlaybackTools();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mMediaController = (MediaController) activity;
            mMediaController.setOnCompleteListner(new Runnable() {
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.swapViews(
                                    new View[]{binding.btnPlay},
                                    new View[]{binding.btnPause}
                            );
                        }
                    });
                }
            });
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaController");
        }
        try {
            mAudioEditDelegator = (AudioEditDelegator) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement AudioEditDelegator");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaController = null;
        mAudioEditDelegator = null;
        //hold the visibility of the undo button for switching contexts to verse marker mode and back
        mUndoVisible = binding.btnUndo.getVisibility() == View.VISIBLE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlayerToolbarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        attachListeners();
        initTimer(binding.playbackElapsed, binding.playbackDuration);
        Utils.swapViews(new View[]{binding.btnPlay}, new View[]{binding.btnPause});
        if (mMediaController.isPlaying()) {
            showPauseButton();
        } else {
            showPlayButton();
        }
        //restore undo button visibility from detach
        binding.btnUndo.setVisibility((mUndoVisible) ? View.VISIBLE : View.INVISIBLE);
    }

    private void initTimer(final TextView elapsed, final TextView duration) {
        mTimer = new PlaybackTimer(elapsed, duration);
        mTimer.setElapsed(mMediaController.getLocationMs());
        mTimer.setDuration(mMediaController.getDurationMs());
    }

    public void onLocationUpdated(int ms) {
        mTimer.setElapsed(ms);
    }

    public void onDurationUpdated(int ms) {
        mTimer.setDuration(ms);
    }

    private void attachListeners() {
        attachMediaControllerListeners();
    }

    public void showPauseButton() {
        Utils.swapViews(new View[]{binding.btnPause}, new View[]{binding.btnPlay});
    }

    public void showPlayButton() {
        Utils.swapViews(new View[]{binding.btnPlay}, new View[]{binding.btnPause});
    }

    public void viewOnSetStartMarker() {
        Utils.swapViews(new View[]{binding.btnEndMark, binding.btnClear}, new View[]{binding.btnStartMark});
    }

    public void viewOnSetEndMarker() {
        Utils.swapViews(new View[]{binding.btnCut}, new View[]{binding.btnEndMark, binding.btnStartMark});
    }

    public void viewOnSetBothMarkers() {
        Utils.swapViews(new View[]{binding.btnCut}, new View[]{binding.btnEndMark, binding.btnStartMark});
    }

    public void viewOnCut() {
        Utils.swapViews(new View[]{binding.btnStartMark, binding.btnUndo}, new View[]{binding.btnCut, binding.btnClear});
    }

    public void viewOnUndo() {
        View[] toHide = {};
        if (!((EditStateInformer) mAudioEditDelegator).hasEdits()) {
            toHide = new View[]{binding.btnUndo};
        }
        Utils.swapViews(new View[]{}, toHide);
    }

    public void viewOnClearMarkers() {
        Utils.swapViews(new View[]{binding.btnStartMark}, new View[]{binding.btnClear, binding.btnCut, binding.btnEndMark});
    }

    private void attachMediaControllerListeners() {
        binding.btnPlay.setOnClickListener(v -> {
            showPauseButton();
            mMediaController.onMediaPlay();
        });

        binding.btnPause.setOnClickListener(v -> {
            showPlayButton();
            mMediaController.onMediaPause();
        });

        binding.btnSkipBack.setOnClickListener(v -> mMediaController.onSeekBackward());

        binding.btnSkipForward.setOnClickListener(v -> mMediaController.onSeekForward());

        binding.btnStartMark.setOnClickListener(v -> {
            viewOnSetStartMarker();
            mAudioEditDelegator.onDropStartMarker();
        });

        binding.btnEndMark.setOnClickListener(v -> {
            viewOnSetEndMarker();
            mAudioEditDelegator.onDropEndMarker();
        });

        binding.btnDropVerseMarker.setOnClickListener(v -> mAudioEditDelegator.onDropVerseMarker());

        binding.btnCut.setOnClickListener(v -> {
            viewOnCut();
            mAudioEditDelegator.onCut();
        });

        binding.btnUndo.setOnClickListener(v -> {
            mAudioEditDelegator.onUndo();
            viewOnUndo();
        });

        binding.btnClear.setOnClickListener(v -> {
            viewOnClearMarkers();
            mAudioEditDelegator.onClearMarkers();
        });

        binding.btnSave.setOnClickListener(v -> mAudioEditDelegator.onSave());
    }

    public void onPlayerPaused() {
        Utils.swapViews(new View[]{binding.btnPlay}, new View[]{binding.btnPause});
    }

    public void invalidate(int ms) {

    }
}
