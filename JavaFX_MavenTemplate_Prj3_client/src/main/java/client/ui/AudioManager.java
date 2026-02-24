package client.ui;

import java.net.URL;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;

public class AudioManager {

    private static final double DEFAULT_VOLUME = 0.5;
    private static AudioManager instance;

    private MediaPlayer bgPlayer;
    private AudioClip clickClip;
    private AudioClip revealClip;
    private double musicVolume = DEFAULT_VOLUME;
    private double sfxVolume = DEFAULT_VOLUME;

    private AudioManager() {
        try {
            URL bgUrl = getClass().getResource("/client/audio/bg.mp3");
            if (bgUrl == null) {
                System.out.println("Background music not found at /client/audio/bg.mp3");
                return;
            }

            Media media = new Media(bgUrl.toExternalForm());
            bgPlayer = new MediaPlayer(media);
            bgPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgPlayer.setVolume(musicVolume);
            bgPlayer.setOnError(() -> System.out.println("Audio error: " + bgPlayer.getError()));

            URL clickUrl = getClass().getResource("/client/audio/click.wav");
            if (clickUrl != null) {
                clickClip = new AudioClip(clickUrl.toExternalForm());
                clickClip.setVolume(sfxVolume);
            } else {
                System.out.println("Click sound not found at /client/audio/click.wav");
            }

            URL revealUrl = getClass().getResource("/client/audio/reveal.wav");
            if (revealUrl != null) {
                revealClip = new AudioClip(revealUrl.toExternalForm());
                revealClip.setVolume(sfxVolume);
            } else {
                System.out.println("Reveal sound not found at /client/audio/reveal.wav");
            }
        } catch (MediaException e) {
            System.out.println("Unable to load background music: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected audio error: " + e.getMessage());
        }
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playBackground() {
        if (bgPlayer != null) {
            bgPlayer.play();
        }
    }

    public void stopBackground() {
        if (bgPlayer != null) {
            bgPlayer.stop();
        }
    }

    public void setMusicVolume(double volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        if (bgPlayer != null) {
            bgPlayer.setVolume(this.musicVolume);
        }
    }

    public void setVolume(double volume) {
        setMusicVolume(volume);
    }

    public void setSfxVolume(double volume) {
        this.sfxVolume = Math.max(0, Math.min(1, volume));
        if (clickClip != null) {
            clickClip.setVolume(this.sfxVolume);
        }
        if (revealClip != null) {
            revealClip.setVolume(this.sfxVolume);
        }
    }

    public double getVolume() {
        return musicVolume;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public void playClick() {
        if (clickClip != null) {
            clickClip.play();
        }
    }

    public void playReveal() {
        if (revealClip != null) {
            revealClip.play();
        }
    }

    public void dispose() {
        if (bgPlayer != null) {
            bgPlayer.stop();
            bgPlayer.dispose();
            bgPlayer = null;
        }
        clickClip = null;
        revealClip = null;
    }
}
