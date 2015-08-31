package io.github.xwz.abciview.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Handler;
import android.support.v17.leanback.widget.ImageCardView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.MediaController;
import android.widget.TextView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.DebugTextViewHelper;
import com.google.android.exoplayer.util.Util;

import java.util.Arrays;
import java.util.List;

import io.github.xwz.abciview.R;
import io.github.xwz.abciview.models.EpisodeModel;
import io.github.xwz.abciview.player.VideoPlayer;

/**
 * Created by wei on 30/08/15.
 */
public class VideoPlayerView {
    private static final String TAG = "VideoPlayerView";

    private View shutterView;
    private AspectRatioFrameLayout videoFrame;
    private SurfaceView surfaceView;
    private TextView debugTextView;
    private TextView playerStateTextView;
    private TextView statusTextView;
    private SubtitleLayout subtitleLayout;
    private EpisodeCardView nextEpisode;
    private View nextEpisodeCard;
    private View episodeDetails;
    private TextView episodeTitle;
    private TextView seriesTitle;
    private TextView duration;

    private DebugTextViewHelper debugViewHelper;

    private final Context mContext;

    private MediaController mediaController;
    private MediaController.MediaPlayerControl mPlayer;

    private static final List<Integer> PLAY_PAUSE_EVENTS = Arrays.asList(
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_DPAD_CENTER
    );

    public VideoPlayerView(Context context, MediaController controller, View root) {
        mContext = context;
        mediaController = controller;
        shutterView = root.findViewById(R.id.shutter);

        videoFrame = (AspectRatioFrameLayout) root.findViewById(R.id.video_frame);
        surfaceView = (SurfaceView) root.findViewById(R.id.surface_view);
        debugTextView = (TextView) root.findViewById(R.id.debug_text_view);
        statusTextView = (TextView) root.findViewById(R.id.status);

        playerStateTextView = (TextView) root.findViewById(R.id.player_state_view);
        subtitleLayout = (SubtitleLayout) root.findViewById(R.id.subtitles);
        ImageCardView card = (ImageCardView) root.findViewById(R.id.next_episode);
        nextEpisodeCard = root.findViewById(R.id.next_episode_card);
        episodeDetails = root.findViewById(R.id.episode_details);
        episodeTitle = (TextView) root.findViewById(R.id.episode_title);
        seriesTitle = (TextView) root.findViewById(R.id.series_title);
        duration = (TextView) root.findViewById(R.id.duration);

        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setInfoVisibility(View.VISIBLE);
        card.setExtraVisibility(View.VISIBLE);
        nextEpisode = new EpisodeCardView(context, card);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return handleTouchEvents(view, motionEvent);
            }
        });
        root.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return handleKeyEvents(v, keyCode, event);
            }
        });
    }

    private boolean handleKeyEvents(View v, int keyCode, KeyEvent event) {
        Log.d(TAG, "keyCode:" + keyCode + ", event:" + event);
        final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
        if (PLAY_PAUSE_EVENTS.contains(keyCode) && uniqueDown) {
            doPauseResume();
            return true;
        }
        return false;
    }

    private boolean handleTouchEvents(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            toggleControlsVisibility();
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            view.performClick();
        }
        return true;
    }

    private void doPauseResume() {
        Log.d(TAG, "doPauseResume:" + mPlayer);
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                showControls();
            } else {
                mPlayer.start();
                hideControlsDelayed();
            }
        }
    }

    public void setEpisode(EpisodeModel episode) {
        episodeTitle.setText(episode.getTitle());
        seriesTitle.setText(episode.getSeriesTitle());
        duration.setText(episode.getDurationText());
        showShutter(true);
        showEpisodeDetails();
    }

    public void setMediaPlayer(MediaController.MediaPlayerControl player) {
        mPlayer = player;
    }

    public void resetView() {
        nextEpisodeCard.setVisibility(View.GONE);
    }

    public void setVideoFrameAspectRatio(float ratio) {
        videoFrame.setAspectRatio(ratio);
    }

    public void showShutter(boolean show) {
        shutterView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }

    public void startDebugView(VideoPlayer player) {
        debugViewHelper = new DebugTextViewHelper(player, debugTextView);
        debugViewHelper.start();
    }

    public void stopDebugView() {
        debugViewHelper.stop();
        debugViewHelper = null;
    }

    private void toggleControlsVisibility() {
        if (mediaController.isShowing()) {
            hideControls();
        } else {
            showControls();
        }
    }

    public void showControls() {
        mediaController.show(0);
        showEpisodeDetails();
    }

    public void showEpisodeDetails() {
        episodeDetails.setVisibility(View.VISIBLE);
    }

    public void hideControls() {
        mediaController.hide();
        episodeDetails.setVisibility(View.GONE);
    }

    public void configureSubtitleView() {
        CaptionStyleCompat captionStyle;
        float captionFontScale;
        if (Util.SDK_INT >= 19) {
            captionStyle = getUserCaptionStyleV19();
            captionFontScale = getUserCaptionFontScaleV19();
        } else {
            captionStyle = CaptionStyleCompat.DEFAULT;
            captionFontScale = 1.0f;
        }
        subtitleLayout.setStyle(captionStyle);
        subtitleLayout.setFontScale(captionFontScale);
    }

    public Surface getVideoSurface() {
        return surfaceView.getHolder().getSurface();
    }

    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            showControls();
        }
        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                showStatusText(R.string.buffering);
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                showStatusText(R.string.loading);
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                hideStatusText();
                showShutter(false);
                if (mPlayer != null && mPlayer.isPlaying()) {
                    hideControlsDelayed();
                }
                break;
            default:
                text += "unknown";
                break;
        }
        playerStateTextView.setText(text);
    }

    private void hideControlsDelayed() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideControls();
            }
        }, 3000);
    }

    public void suggestNextEpisode(EpisodeModel episode) {
        nextEpisode.setEpisode(episode);
        nextEpisodeCard.setVisibility(View.VISIBLE);
    }

    public void showStatusText(int resId) {
        showStatusText(mContext.getResources().getString(resId));
    }

    public void hideStatusText() {
        statusTextView.setVisibility(View.GONE);
    }

    public void showStatusText(String text) {
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(text);
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }
}
