package com.amazon.avod.test.runners;

import static androidx.test.InstrumentationRegistry.getContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import org.junit.runner.Description;
import org.junit.runner.Result;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import androidx.test.InstrumentationRegistry;
import androidx.test.internal.runner.listener.InstrumentationRunListener;
import com.google.common.io.ByteStreams;

/**
 * Extract an embedded remote routines into a temporary file. Implemented as an {@link InstrumentationRunListener}
 * so that the final path can be sent via the result bundle.
 */
public class RemoteRoutinesRunnerExtractor extends InstrumentationRunListener {
    private static final String TAG = "RemoteRoutinesExtractor";
    private static final String REMOTE_ROUTINES_RUNNER_PATH_KEY = "remote_routines_runner_path";

    public static final String OUTPUT_DIRECTORY = "/sdcard/Download"; // The Environment.getExternalStorageDirectory does not work
    private static final String REMOTE_ROUTINES_RUNNER_FILE = "ATVAndroidRemoteRoutinesRunner-all-1.0.jar";
    private static final String REMOTE_ROUTINES_RUNNER_TITLE = "ATVAndroidRemoteRoutinesRunner-all-1.0";
    private Future<String> remoteRoutinesRunnerPathFuture = null;

    @Override
    public void testRunStarted(final Description description) throws Exception {
        super.testRunStarted(description);

        remoteRoutinesRunnerPathFuture = Executors.newSingleThreadExecutor().submit(
                this::extractRemoteRoutinesRunner);
    }

    @Override
    public void instrumentationRunFinished(
            final PrintStream streamResult,
            final Bundle resultBundle,
            final Result junitResults
    ) {
        super.instrumentationRunFinished(streamResult, resultBundle, junitResults);

        try {
            final String remoteRoutinesRunnerPath = remoteRoutinesRunnerPathFuture.get();
            if (remoteRoutinesRunnerPath != null) {
                resultBundle.putString(REMOTE_ROUTINES_RUNNER_PATH_KEY, remoteRoutinesRunnerPath);
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractRemoteRoutinesRunner() {
        try {
            Log.i(TAG, "Extracting remote routines");
            final InputStream source = getContext().getAssets()
                    .open(REMOTE_ROUTINES_RUNNER_FILE);
            final File tempFile = writeToTemporaryFile(source);
            return tempFile.getAbsolutePath();
        } catch (final IOException e) {
            Log.e(TAG, "Unable to copy remote routines: " + e);
            // No remote routines embedded in APK
            return null;
        }
    }

    private File writeToTemporaryFile(final InputStream source) {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return writeToTemporaryFileViaMediaStore(source);
        }

        final File outputFile = new File(OUTPUT_DIRECTORY + File.separator + REMOTE_ROUTINES_RUNNER_FILE);

        Log.i(TAG, "copying remote routines to " + outputFile.getAbsolutePath());
        try {
            ByteStreams.copy(source, new FileOutputStream(outputFile));
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy data", e);
        }

        return outputFile;
    }

    private File writeToTemporaryFileViaMediaStore(final InputStream source) {
        // Removing orphaned file technique from `MediaStoreFileUtil` does not work.
        // So this is what I got...
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("content delete --uri "
                + MediaStore.Files.getContentUri("external")
                + " --where title='" + REMOTE_ROUTINES_RUNNER_TITLE + "'");

        final ContentResolver resolver = getContext().getContentResolver();

        final Uri uri = getNewContentUri(REMOTE_ROUTINES_RUNNER_FILE, resolver);
        if (uri == null) {
            throw new RuntimeException("unable to create writable file");
        }

        final String actualFilePath = getActualFilePath(uri, resolver);

        Log.i(TAG, "copying remote routines to " + actualFilePath);
        try {
            ByteStreams.copy(source, resolver.openOutputStream(uri));
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy data", e);
        }

        return new File(actualFilePath);
    }

    private Uri getNewContentUri(@Nonnull final String fileName, @Nonnull final ContentResolver resolver) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);

        final Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
        if (uri == null) {
            throw new RuntimeException("unable to create mediastore record");
        }

        return uri;
    }

    private String getActualFilePath(@Nonnull final Uri uri, final ContentResolver resolver) {
        // Because <del>timing issue<del> android is a pain, the file name may not be
        // exactly the same as requested
        final Cursor cursor = resolver.query(uri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(0);
        }

        throw new RuntimeException("Unable to resolve mediastore record path");
    }
}
