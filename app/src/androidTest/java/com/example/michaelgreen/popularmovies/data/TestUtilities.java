package com.example.michaelgreen.popularmovies.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Set;

/**
 * Created by michaelgreen on 11/7/15.
 */
public class TestUtilities extends AndroidTestCase {
    static final String TEST_LOCATION = "99705";
    static final long TEST_DATE = 1419033600L;  // December 20th, 2014

    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }

    /*
        Students: Use this to create some default weather values for your database tests.
     */
    static ContentValues createMovieValues() {
        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieContract.MovieEntry.COLUMN_ID, 166424);
        movieValues.put(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH, "/3Kgu3ys6W6UZWWFty7rlTWgST63.jpg");
        movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, "Fantastic Four");
        movieValues.put(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE, "Fantastic Four");
        movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, "Four young outsiders teleport to a dangerous universe, which alters their physical form in shocking ways. Their lives irrevocably upended, the team must learn to harness their daunting new abilities and work together to save Earth from a former friend turned enemy.");
        movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, "2015-08-07");
        movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, "/g23cs30dCMiG4ldaoVNP1ucjs6.jpg");
        movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY, 6.9);
        movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, 4.6);
        movieValues.put(MovieContract.MovieEntry.COLUMN_TRAILERS, "Poof");
        movieValues.put(MovieContract.MovieEntry.COLUMN_REVIEWS, "Reviews");
        movieValues.put(MovieContract.MovieEntry.COLUMN_IS_FAVORITE, "TRUE");

        return movieValues;
    }
}
