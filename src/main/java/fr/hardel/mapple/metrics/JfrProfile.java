package fr.hardel.mapple.metrics;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

public final class JfrProfile {
    private static final String CONFIGURATION = "profile";

    private final Recording recording;

    private JfrProfile(Recording recording) {
        this.recording = recording;
    }

    public static JfrProfile start(Path destination) {
        try {
            Recording recording = new Recording(Configuration.getConfiguration(CONFIGURATION));
            recording.setDestination(destination);
            recording.start();
            return new JfrProfile(recording);
        } catch (IOException | ParseException exception) {
            throw new IllegalStateException("Failed to start the flight recording " + destination, exception);
        }
    }

    public void stop() {
        this.recording.stop();
        this.recording.close();
    }
}
