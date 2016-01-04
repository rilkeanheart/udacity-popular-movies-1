package com.example.michaelgreen.popularmovies;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.michaelgreen.popularmovies.data.MovieContract;
import com.example.michaelgreen.popularmovies.data.MovieDbHelper;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailsFragment extends Fragment {

    private static final String LOG_TAG = MovieDetailsFragment.class.getSimpleName();
    static final String MOVIE_STRING = "MOVIE_STRING";

    private JSONObject mMovieDetailsJSONObject;
    private ToggleButton mFavoritesButton;
    private TrailerAdapter mTrailerAdapter;
    private ReviewAdapter mReviewAdapter;
    private ListView mTrailersListView;
    private ListView mReviewsListView;

    public MovieDetailsFragment() {
    }

    /**** Method for Setting the Height of the ListView dynamically.
     **** Hack to fix the issue of not showing all the items of the ListView
     **** when placed inside a ScrollView  ****/
    //http://stackoverflow.com/questions/18367522/android-list-view-inside-a-scroll-view
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_movie_details, container, false);
        mFavoritesButton = (ToggleButton) rootView.findViewById(R.id.like_toggle);

        // The detail activity called via Intent. Inspect the intent for a movie JSONObject string
        //Intent intent = getActivity().getIntent();
        Bundle arguments = getArguments();
        if (arguments != null) {

            try {
                mMovieDetailsJSONObject = new JSONObject(arguments.getString(MovieDetailsFragment.MOVIE_STRING));

                String movieTitle = getString(R.string.movie_api_json_original_title);
                ((TextView) rootView.findViewById(R.id.movieTitle))
                        .setText(mMovieDetailsJSONObject.getString(movieTitle));

                String releaseDate = getString(R.string.movie_api_json_release_date);
                ((TextView) rootView.findViewById(R.id.releaseDate))
                        .setText(mMovieDetailsJSONObject.getString(releaseDate));

                String userRating = getString(R.string.movie_api_json_vote_average);
                ((TextView) rootView.findViewById(R.id.userRating))
                    .setText(mMovieDetailsJSONObject.getString(userRating));

                String plotSynopsis = getString(R.string.movie_api_json_overview);
                ((TextView) rootView.findViewById(R.id.plotSynopsis))
                        .setText(mMovieDetailsJSONObject.getString(plotSynopsis));

                String isFavoriteKey = getString(R.string.movie_json_is_favorite);
                String isFavorite = mMovieDetailsJSONObject.optString(isFavoriteKey,
                        getString(R.string.movie_json_is_favorite_unknown));
                if(isFavorite.equals(getString(R.string.movie_json_is_favorite_unknown))) {
                    // Will fetch the state from the database and update the UI with results
                    setIsFavoriteToggleButton();
                } else {
                    // If value is set, we can update the UI based on the JSONObject alone
                    if(isFavorite.equals("TRUE")) {
                        mFavoritesButton.setChecked(true);
                    } else {
                        mFavoritesButton.setChecked(false);
                    }
                }

                String imageURLPath = getString(R.string.detail_image_url_base);
                String posterPathString = getString(R.string.movie_api_json_poster_path);
                imageURLPath += mMovieDetailsJSONObject.getString(posterPathString);
                Log.d(LOG_TAG, "Image URL String:  " + imageURLPath);
                ImageView imageView = (ImageView) rootView.findViewById(R.id.moviePoster);
                Picasso.with(getActivity())
                        .load(imageURLPath)
                        .into(imageView);

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }

        }

        // Set up the Movie Trailer or Review lists
        ArrayList<JSONObject> movieTrailers = new ArrayList<JSONObject>();
        ArrayList<JSONObject> movieReviews = new ArrayList<JSONObject>();
        boolean updateTrailers = false;
        boolean updateReviews = false;

        if(mMovieDetailsJSONObject != null) {
            try{
                if(mMovieDetailsJSONObject.has(getString(R.string.movie_json_trailers))) {
                    String trailersString = mMovieDetailsJSONObject.getString(getString(R.string.movie_json_trailers));
                    JSONArray trailers = new JSONArray(trailersString);
                    for(int i = 0; i < trailers.length(); i++) {
                        movieTrailers.add(trailers.getJSONObject(i));
                    }
                } else {
                    updateTrailers = true;
                }

                if(mMovieDetailsJSONObject.has(getString(R.string.movie_json_reviews))) {
                    String reviewsString = mMovieDetailsJSONObject.getString(getString(R.string.movie_json_reviews));
                    JSONArray reviews = new JSONArray(reviewsString);
                    for(int i = 0; i < reviews.length(); i++) {
                        movieReviews.add(reviews.getJSONObject(i));
                    }
                } else {
                    updateReviews = true;
                }
            } catch(JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }
        }
        mTrailerAdapter = new TrailerAdapter(getActivity(), movieTrailers);
        mTrailersListView = (ListView) rootView.findViewById(R.id.listView_trailers);
        MovieDetailsFragment.setListViewHeightBasedOnChildren(mTrailersListView);
        mTrailersListView.setAdapter(mTrailerAdapter);
        mTrailersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                JSONObject selectedMovie = mTrailerAdapter.getItem(position);
                try {
                    String trailerSite = selectedMovie.getString(getString(R.string.movie_api_json_trailer_site));
                    if(trailerSite.equalsIgnoreCase(getString(R.string.youtube_video_site_name))){
                        // By default, use native You Tube app
                        String youTubeNativeAppPath = getString(R.string.youtube_native_app_uri_base);
                        youTubeNativeAppPath += selectedMovie.getString(getString(R.string.movie_api_json_trailer_key));
                        Uri builtUri = Uri.parse(youTubeNativeAppPath);
                        Intent intent = new Intent(Intent.ACTION_VIEW, builtUri);
                        PackageManager packageManager = getActivity().getPackageManager();
                        if(intent.resolveActivity(packageManager) == null) {
                            String youTubeWebAppPath = getString(R.string.youtube_url_base);
                            youTubeWebAppPath += selectedMovie.getString(getString(R.string.movie_api_json_trailer_key));
                            builtUri = Uri.parse(youTubeWebAppPath);
                            intent = new Intent(Intent.ACTION_VIEW, builtUri);
                        }
                        startActivity(intent);
                    }

                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }
            }
        });
        if(updateTrailers) {
            updateMovieTrailers();
        } else {
            MovieDetailsFragment.setListViewHeightBasedOnChildren(mTrailersListView);
        }

        // Set up the Movie Reviews list
        mReviewAdapter = new ReviewAdapter(getActivity(), movieReviews);
        mReviewsListView = (ListView) rootView.findViewById(R.id.listView_reviews);
        MovieDetailsFragment.setListViewHeightBasedOnChildren(mReviewsListView);
        mReviewsListView.setAdapter(mReviewAdapter);
        mReviewsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                JSONObject selectedReview = mReviewAdapter.getItem(position);
                try {
                    String reviewURL = selectedReview.getString(getString(R.string.movie_json_review_url));
                    Uri builtUri = Uri.parse(reviewURL);
                    Intent intent = new Intent(Intent.ACTION_VIEW, builtUri);
                    startActivity(intent);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }
            }
        });
        if(updateReviews) {
            updateMovieReviews();
        } else {
            MovieDetailsFragment.setListViewHeightBasedOnChildren(mReviewsListView);
        }

        mFavoritesButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                    // Update the is favorite of the JSON
                    try {
                        mMovieDetailsJSONObject.put(getString(R.string.movie_json_is_favorite),
                                "TRUE");
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error", e);
                    }

                    // Fetch Movies using DB Based AsyncTask
                    UpdateMoviesTask moviesTask = new UpdateMoviesTask(getActivity());
                    moviesTask.execute(mMovieDetailsJSONObject);

                    /*Context context = getActivity().getApplicationContext();
                    CharSequence text = "You just liked this movie!";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();*/
                } else {
                    // The toggle is disabled

                    // Update the is favorite of the JSON
                    try {
                        mMovieDetailsJSONObject.put(getString(R.string.movie_json_is_favorite),
                                "FALSE");
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error", e);
                    }

                    // Fetch Movies using DB Based AsyncTask
                    UpdateMoviesTask moviesTask = new UpdateMoviesTask(getActivity());
                    moviesTask.execute(mMovieDetailsJSONObject);

                    /*Context context = getActivity().getApplicationContext();
                    CharSequence text = "You no longer like this movie!";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();*/
                }
            }
        });

        ListView lvTrailers = (ListView) rootView.findViewById(R.id.listView_trailers);
        lvTrailers.setOnTouchListener(new OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        ListView lvReviews = (ListView) rootView.findViewById(R.id.listView_reviews);
        lvReviews.setOnTouchListener(new OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        return rootView;
    }

    private void updateMovieTrailers(){
        FetchTrailersTask trailersTask = new FetchTrailersTask(getActivity(), mTrailersListView);
        trailersTask.execute();
    }

    private void updateMovieReviews(){
        FetchReviewsTask reviewsTask = new FetchReviewsTask(getActivity(), mReviewsListView);
        reviewsTask.execute();
    }

    private void setIsFavoriteToggleButton() {
        // TODO: Create and execute SetFavoriteToggleStateTask
        SetFavoriteToggleStateTask checkFavoritesTask = new SetFavoriteToggleStateTask(getActivity(),
                mFavoritesButton);
        checkFavoritesTask.execute(mMovieDetailsJSONObject);
    }

    public class TrailerAdapter extends ArrayAdapter<JSONObject> {

        private final String LOG_TAG = TrailerAdapter.class.getSimpleName();

        public TrailerAdapter(Context context,
                            ArrayList<JSONObject> target)
        {
            super(context,-1, target);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View trailerItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_trailer,
                    parent, false);

            TextView trailerNameTextView = (TextView) trailerItemView.findViewById(R.id.list_item_trailer_name);
            JSONObject thisTrailer = getItem(position);

            try {
                String trailerName = thisTrailer.
                        getString(getString(R.string.movie_api_json_trailer_name));
                trailerNameTextView.setText(trailerName);

            } catch(JSONException e){
                Log.e(LOG_TAG, "Error ", e);
            }

            return trailerItemView;
        }
    }

    public class ReviewAdapter extends ArrayAdapter<JSONObject> {

        private final String LOG_TAG = ReviewAdapter.class.getSimpleName();

        public ReviewAdapter(Context context,
                              ArrayList<JSONObject> target)
        {
            super(context,-1, target);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View reviewItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_review,
                    parent, false);

            /*TextView reviewContentTextView = (TextView) reviewItemView.
                    findViewById(R.id.list_item_review_content);*/
            TextView reviewAuthorTextView = (TextView) reviewItemView.
                    findViewById(R.id.list_item_review_author);
            JSONObject thisReview = getItem(position);
            //String youTubeVideoURLPath = getString(R.string.youtube_url_base);
            //String youTubeVideoParam = getString(R.string.youtube_video_param_name);
            /*Uri builtUri = Uri.parse(youTubeVideoURLPath).buildUpon()
                    .appendQueryParameter(youTubeVideoParam, getString(R.string.movie_api_key))
                    .build();
            URL url = new URL(builtUri.toString());*/
            try {
               // reviewContentTextView.setText(thisReview.getString(getString(R.string.movie_json_review_content)));
                reviewAuthorTextView.setText(thisReview.getString(getString(R.string.movie_json_review_author)));
            } catch(JSONException e){
                Log.e(LOG_TAG, "Error ", e);
            }

            return reviewItemView;
        }
    }

    public class UpdateMoviesTask extends AsyncTask<JSONObject, Void, Boolean> {

        private Context mContext;

        private final String LOG_TAG = UpdateMoviesTask.class.getSimpleName();

        public UpdateMoviesTask(Context context){
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(JSONObject... params) {

            JSONObject movie = params[0];
            String strIsFavorite = "FALSE";
            // Create movie values based on the JSON Object
            ContentValues movieValues = new ContentValues();
            String strMovieTitle = "";
            String strMovieReleaseDate = "";
            try {
                strMovieTitle = movie.getString(getString(R.string.movie_api_json_title));
                strMovieReleaseDate = movie.getString(getString(R.string.movie_api_json_release_date));

                movieValues.put(MovieContract.MovieEntry.COLUMN_ID,
                        movie.getInt(getString(R.string.movie_api_json_id)));

                movieValues.put(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH,
                        movie.getString(getString(R.string.movie_api_json_backdrop_path)));

                movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, strMovieTitle);

                movieValues.put(MovieContract.MovieEntry.COLUMN_ORIGINAL_TITLE,
                        movie.getString(getString(R.string.movie_api_json_original_title)));

                movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW,
                        movie.getString(getString(R.string.movie_api_json_overview)));

                movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE,strMovieReleaseDate);

                movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH,
                        movie.getString(getString(R.string.movie_api_json_poster_path)));

                movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY,
                        movie.getDouble(getString(R.string.movie_api_json_popularity)));

                movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE,
                        movie.getDouble(getString(R.string.movie_api_json_vote_average)));

                JSONArray trailers = movie.getJSONArray(getString(R.string.movie_json_trailers));
                movieValues.put(MovieContract.MovieEntry.COLUMN_TRAILERS,
                        trailers.toString());

                JSONArray reviews = movie.getJSONArray(getString(R.string.movie_json_reviews));
                movieValues.put(MovieContract.MovieEntry.COLUMN_REVIEWS,
                        reviews.toString());

                strIsFavorite = movie.getString(getString(R.string.movie_json_is_favorite));
                movieValues.put(MovieContract.MovieEntry.COLUMN_IS_FAVORITE,
                        strIsFavorite);

            } catch(JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }

            // Get reference to writable database
            // If there's an error in those massive SQL table creation Strings,
            // errors will be thrown here when you try to get a writable database.
            MovieDbHelper dbHelper = new MovieDbHelper(mContext);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // Look for a matching movie entry based on name and release date
            String sByMovieTitleAndReleaseDateSelection =
                    MovieContract.MovieEntry.TABLE_NAME+
                            "." + MovieContract.MovieEntry.COLUMN_TITLE + " = ? ";
            sByMovieTitleAndReleaseDateSelection +=
                    "AND " + MovieContract.MovieEntry.TABLE_NAME+
                            "." + MovieContract.MovieEntry.COLUMN_RELEASE_DATE + " = ? ";

            Cursor existingMovieCursor = mContext.getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    new String[]{MovieContract.MovieEntry._ID},
                    sByMovieTitleAndReleaseDateSelection,
                    new String[]{strMovieTitle, strMovieReleaseDate},
                    null
            );

            if(existingMovieCursor != null) {
                if(existingMovieCursor.moveToFirst()) {
                    // We have an existing movie, update it
                    long locationRowId = existingMovieCursor.getLong(0);
                    int count = mContext.getContentResolver().update(
                            MovieContract.MovieEntry.CONTENT_URI, movieValues, MovieContract.MovieEntry._ID + "= ?",
                            new String[] { Long.toString(locationRowId)});
                } else {
                    // This is a new movie, Insert ContentValues into database and get a row ID back
                    long movieRowId = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, movieValues);
                }
            }

            return (strIsFavorite.equals("TRUE")) ? true : false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == true) {
                CharSequence text = "You liked this movie!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(mContext, text, duration);
                toast.show();
            } else {
                CharSequence text = "You no longer like this movie!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(mContext, text, duration);
                toast.show();
            }
        }
    }

    public class SetFavoriteToggleStateTask extends AsyncTask<JSONObject, Void, Boolean> {

        private Context mContext;
        private ToggleButton mFavoriteToggleButton;

        private final String LOG_TAG = SetFavoriteToggleStateTask.class.getSimpleName();

        public SetFavoriteToggleStateTask(Context context, ToggleButton button){
            mContext = context;
            mFavoriteToggleButton = button;
        }

        @Override
        protected Boolean doInBackground(JSONObject... params) {

            Boolean shouldCheckFavorite = false;
            JSONObject thisMovie = params[0];
            try{
                String strMovieTitle =
                        thisMovie.getString(getString(R.string.movie_api_json_title));
                String strMovieReleaseDate =
                        thisMovie.getString(getString(R.string.movie_api_json_release_date));
                String sByMovieTitleAndReleaseDateSelection =
                        MovieContract.MovieEntry.TABLE_NAME+
                                "." + MovieContract.MovieEntry.COLUMN_TITLE + " = ? ";
                sByMovieTitleAndReleaseDateSelection +=
                        "AND " + MovieContract.MovieEntry.TABLE_NAME+
                                "." + MovieContract.MovieEntry.COLUMN_RELEASE_DATE + " = ? ";

                // Test the basic content provider query
                Cursor movieCursor = mContext.getContentResolver().query(
                        MovieContract.MovieEntry.CONTENT_URI,
                        new String[]{MovieContract.MovieEntry.COLUMN_IS_FAVORITE},
                        sByMovieTitleAndReleaseDateSelection,
                        new String[]{strMovieTitle, strMovieReleaseDate},
                        null
                );

                if(movieCursor != null) {
                    if(movieCursor.moveToFirst()) {
                        String isFavorite = movieCursor.getString(0);
                        if(isFavorite.equals("TRUE")) {
                            shouldCheckFavorite = true;
                        }
                    }
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }

            return shouldCheckFavorite;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // AND set is favorite on JSONObject to false
            // Otherwise set isFavorite to true and check the box
            if (result) {
                mFavoriteToggleButton.setChecked(true);
            } else {
                mFavoriteToggleButton.setChecked(false);
            }
        }
    }

    public class FetchReviewsTask extends AsyncTask<Void, Void, JSONObject[]> {

        private Context mContext;
        private ListView mReviewsListView;

        private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();

        public FetchReviewsTask(Context context, ListView reviewsListView){
            mContext = context;
            mReviewsListView = reviewsListView;
        }

        /**
         * Take the String representing the list of reviews in JSON Format and
         * pull out the data we need to construct the Strings needed.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private JSONObject[] getReviewsDataFromJson(String movieReviewsJsonStr)
                throws JSONException {

            JSONObject reviewsJson = new JSONObject(movieReviewsJsonStr);
            JSONArray reviewResultsArray = reviewsJson.getJSONArray(getString(R.string.movie_api_json_results));

            JSONObject[] results = new JSONObject[reviewResultsArray.length()];

            try {
                for(int i = 0; i < reviewResultsArray.length(); i++) {
                    results[i] = reviewResultsArray.getJSONObject(i);
                }
            } catch(JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }

            return results;
        }

        @Override
        protected JSONObject[] doInBackground(Void... params) {

            JSONObject movie = mMovieDetailsJSONObject;

            //http://api.themoviedb.org/3/movie/135397/reviews?api_key=dd55e50640613eeec0d43fb98621c1c9
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieReviewsJsonStr = null;

            /*Date today =  Calendar.getInstance().getTime();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -3600);
            Date ninetyDaysAgo = cal.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");*/

            // http://api.themoviedb.org/3/discover/movie?primary_release_date.gte=2014-09-15&primary_release_date.lte=2015-08-10&api_key=dd55e50640613eeec0d43fb98621c1c9
            try {
                String movieID = String.valueOf(movie.getInt(getString(R.string.movie_api_json_id)));
                final String MOVIE_API_REVIEWS_BASE_URL = String.format(getString(R.string.movie_api_reviews_base_url), movieID);
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIE_API_REVIEWS_BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY_PARAM, getString(R.string.movie_api_key))
                        .build();

                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "Query String: " + url);

                // Create the request to Movie API, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                movieReviewsJsonStr = buffer.toString();
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getReviewsDataFromJson(movieReviewsJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject[] result) {
            if (result != null) {
                // Add as JSONArray to mMovieDetailsJSONObject
                try {
                    JSONArray resultsArray = new JSONArray();

                    mReviewAdapter.clear();
                    for(JSONObject reviewObject : result) {
                        mReviewAdapter.add(reviewObject);
                        resultsArray.put(reviewObject);
                    }

                    mMovieDetailsJSONObject.put(getString(R.string.movie_json_reviews), resultsArray);
                } catch(JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }

                MovieDetailsFragment.setListViewHeightBasedOnChildren(mReviewsListView);
            }
        }
    }

    public class FetchTrailersTask extends AsyncTask<Void, Void, JSONObject[]> {

        private Context mContext;
        private ListView mTrailersListView;

        private final String LOG_TAG = UpdateMoviesTask.class.getSimpleName();

        public FetchTrailersTask(Context context, ListView trailersListView){
            mContext = context;
            mTrailersListView = trailersListView;
        }

        /**
         * Take the String representing the list of reviews in JSON Format and
         * pull out the data we need to construct the Strings needed.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private JSONObject[] getTrailersDataFromJson(String movieTrailersJsonStr)
                throws JSONException {

            JSONObject trailersJson = new JSONObject(movieTrailersJsonStr);
            JSONArray reviewResultsArray = trailersJson.getJSONArray(getString(R.string.movie_api_json_results));

            JSONObject[] results = new JSONObject[reviewResultsArray.length()];

            try {
                for(int i = 0; i < reviewResultsArray.length(); i++) {
                    results[i] = reviewResultsArray.getJSONObject(i);
                }
            } catch(JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }

            return results;
        }

        @Override
        protected JSONObject[] doInBackground(Void... params) {

            JSONObject movie = mMovieDetailsJSONObject;

            //http://api.themoviedb.org/3/movie/135397/reviews?api_key=dd55e50640613eeec0d43fb98621c1c9
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieTrailersJsonStr = null;

            /*Date today =  Calendar.getInstance().getTime();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -3600);
            Date ninetyDaysAgo = cal.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");*/

            // http://api.themoviedb.org/3/discover/movie?primary_release_date.gte=2014-09-15&primary_release_date.lte=2015-08-10&api_key=dd55e50640613eeec0d43fb98621c1c9
            try {
                String movieID = String.valueOf(movie.getInt(getString(R.string.movie_api_json_id)));
                final String MOVIE_API_REVIEWS_BASE_URL = String.format(getString(R.string.movie_api_trailers_base_url), movieID);
                final String API_KEY_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIE_API_REVIEWS_BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY_PARAM, getString(R.string.movie_api_key))
                        .build();

                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "Query String: " + url);

                // Create the request to Movie API, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                movieTrailersJsonStr = buffer.toString();
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getTrailersDataFromJson(movieTrailersJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject[] result) {
            if (result != null) {

                // Add as JSONArray to mMovieDetailsJSONObject
                try {
                    JSONArray resultsArray = new JSONArray();

                    mTrailerAdapter.clear();
                    for(JSONObject reviewObject : result) {
                        mTrailerAdapter.add(reviewObject);
                        resultsArray.put(reviewObject);
                    }

                    mMovieDetailsJSONObject.put(getString(R.string.movie_json_trailers), resultsArray);
                } catch(JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }

                MovieDetailsFragment.setListViewHeightBasedOnChildren(mTrailersListView);
            }
        }
    }

}
