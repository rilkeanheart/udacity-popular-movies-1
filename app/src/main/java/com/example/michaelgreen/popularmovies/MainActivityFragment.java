package com.example.michaelgreen.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

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
public class MainActivityFragment extends Fragment {

    private ImageAdapter mMoviePosterGridAdapter;
    private final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    private String mLastSortByStr;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(savedInstanceState == null) {
            mMoviePosterGridAdapter =
                    new ImageAdapter(
                            getActivity().getApplicationContext(), // The current context (this activity)
                            R.layout.grid_item_movie_poster, // The name of the layout ID.
                            R.id.grid_item_movie_imageview, // The ID of the view item to populate.
                            new ArrayList<JSONObject>());
        } else {
            ArrayList<String> stringifiedMoviesList = savedInstanceState.getStringArrayList("key");
            ArrayList<JSONObject> jsonMoviesList =
                    new ArrayList<JSONObject>(stringifiedMoviesList.size());
            for(String movieString : stringifiedMoviesList) {
                try {
                    jsonMoviesList.add(new JSONObject(movieString));
                } catch(JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }
            }

            mMoviePosterGridAdapter =
                    new ImageAdapter(
                            getActivity().getApplicationContext(), // The current context (this activity)
                            R.layout.grid_item_movie_poster, // The name of the layout ID.
                            R.id.grid_item_movie_imageview, // The ID of the view item to populate.
                            jsonMoviesList);
        }

        //Picasso.with(context).load("http://i.imgur.com/DvpvklR.png").into(imageView);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        GridView gridview = (GridView) rootView.findViewById(R.id.gridview_posters);

        // Grab more real estate if in landscape mode
        if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            gridview.setNumColumns(5);
        }

        gridview.setAdapter(mMoviePosterGridAdapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                JSONObject selectedMovie = mMoviePosterGridAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), MovieDetails.class)
                        .putExtra(Intent.EXTRA_TEXT, selectedMovie.toString());
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int sizeList = mMoviePosterGridAdapter.getCount();
        ArrayList<String> stringifiedMoviesList = new ArrayList<String>(sizeList);
        for(int i = 0; i < sizeList; i++) {
            JSONObject thisMovie = mMoviePosterGridAdapter.getItem(i);
            stringifiedMoviesList.add(thisMovie.toString());
        }
        outState.putStringArrayList("key", stringifiedMoviesList);
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currentSortByPref = sharedPrefs.getString(
                getString(R.string.pref_sort_order_key),
                getString(R.string.pref_sort_by_release_date));
        if(mMoviePosterGridAdapter.getCount() == 0 ||
                !currentSortByPref.equals(mLastSortByStr)) {
            updateMovies();
        }
    }

    private void updateMovies(){
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        mLastSortByStr = sharedPrefs.getString(
                getString(R.string.pref_sort_order_key),
                getString(R.string.pref_sort_by_release_date));
        // Fetch Movies using AsyncTask
        FetchMoviesTask moviesTask = new FetchMoviesTask();
        moviesTask.execute();
    }

    public class ImageAdapter extends ArrayAdapter<JSONObject> {

        private final String LOG_TAG = ImageAdapter.class.getSimpleName();

        // http://api.themoviedb.org/3/discover/movie?primary_release_date.gte=2014-09-15&primary_release_date.lte=2014-10-2&api_key=dd55e50640613eeec0d43fb98621c1c9
        // http://developer.android.com/guide/topics/ui/layout/gridview.html
        // http://www.stealthcopter.com/blog/2010/09/android-creating-a-custom-adapter-for-gridview-buttonadapter/
        // http://stackoverflow.com/questions/12213987/how-to-pass-json-object-to-new-activity

        public ImageAdapter(Context context,
                            int resource,
                            int imageViewResourceId,
                            ArrayList<JSONObject> target)
        {
            super(context, resource, imageViewResourceId, target);
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(getContext());
                imageView.setAdjustViewBounds(true);
                //imageView.setPadding(0, 0, 0, 0);
            } else {
                imageView = (ImageView) convertView;
            }

            JSONObject thisMovie = getItem(position);
            String imageURLPath = getString(R.string.image_url_base);
            try {
                imageURLPath += thisMovie.getString(getString(R.string.movie_api_json_poster_path));
                Picasso.with(getContext())
                        .load(imageURLPath)
                        .into(imageView);
            } catch(JSONException e){
                Log.e(LOG_TAG, "Error ", e);
                imageView = null;
            }

            return imageView;
        }
    }

    public class FetchMoviesTask extends AsyncTask<Void, Void, JSONObject[]> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private JSONObject[] getMoviesDataFromJson(String discoverMoviesJsonStr)
                throws JSONException {

            JSONObject moviesJson = new JSONObject(discoverMoviesJsonStr);
            JSONArray movieResultsArray = moviesJson.getJSONArray(getString(R.string.movie_api_json_results));

            JSONObject[] results = new JSONObject[movieResultsArray.length()];

            try {
                for(int i = 0; i < movieResultsArray.length(); i++) {
                    results[i] = movieResultsArray.getJSONObject(i);
                }
            } catch(JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }

            return results;
        }
        @Override
        protected JSONObject[] doInBackground(Void... params) {

            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            String sortBy = sharedPrefs.getString(
                    getString(R.string.pref_sort_order_key),
                    getString(R.string.pref_sort_by_release_date));
            String sortByURLValue;
            if(sortBy.equals(getString(R.string.pref_sort_by_release_date))) {
                sortByURLValue = getString(R.string.movie_api_sort_by_release_url_param);
            } else if (sortBy.equals(getString(R.string.pref_sort_by_popularity))){
                sortByURLValue = getString(R.string.movie_api_sort_by_popularity_url_param);
            } else {
                sortByURLValue = getString(R.string.movie_api_sort_by_rating_url_param);
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr = null;

            Date today =  Calendar.getInstance().getTime();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -3600);
            Date ninetyDaysAgo = cal.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            // http://api.themoviedb.org/3/discover/movie?primary_release_date.gte=2014-09-15&primary_release_date.lte=2015-08-10&api_key=dd55e50640613eeec0d43fb98621c1c9
            try {
                final String DISCOVERY_API_BASE_URL =
                        getString(R.string.movie_api_discover_base_url);
                final String SORT_BY_PARAM = "sort_by";
                final String API_KEY_PARAM = "api_key";
                final String LESS_THAN_DATE = "primary_release_date.lte";
                final String GREATER_THAN_DATE = "primary_release_date.gte";

                Uri builtUri = Uri.parse(DISCOVERY_API_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_BY_PARAM, sortByURLValue)
                        .appendQueryParameter(LESS_THAN_DATE, sdf.format(today))
                        .appendQueryParameter(GREATER_THAN_DATE, sdf.format(ninetyDaysAgo))
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

                moviesJsonStr = buffer.toString();
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
                return getMoviesDataFromJson(moviesJsonStr);
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
                mMoviePosterGridAdapter.clear();
                for(JSONObject movieObject : result) {
                    mMoviePosterGridAdapter.add(movieObject);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }
}
