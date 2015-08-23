package com.example.michaelgreen.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailsFragment extends Fragment {

    private static final String LOG_TAG = MovieDetailsFragment.class.getSimpleName();
    private JSONObject mMovieDetailsJSONObject;

    public MovieDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_movie_details, container, false);

        // The detail activity called via Intent. Inspect the intent for a movie JSONObject string
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {

            try {
                mMovieDetailsJSONObject = new JSONObject(intent.getStringExtra(Intent.EXTRA_TEXT));

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

        return rootView;
    }
}
