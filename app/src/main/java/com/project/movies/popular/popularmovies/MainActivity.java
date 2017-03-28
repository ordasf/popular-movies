package com.project.movies.popular.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.project.movies.popular.popularmovies.beans.Movie;
import com.project.movies.popular.popularmovies.utilities.MovieJSONUtils;
import com.project.movies.popular.popularmovies.utilities.NetworkUtils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        MovieListAdapter.MovieListAdapterOnClickHandler, LoaderManager.LoaderCallbacks<List<Movie>> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String INSTANCE_STATE_BUNDLE_ORDER_KEY = "order_key";

    private static final int LOADER_MOVIE_LIST_ID = 1000;
    private static final String LOADER_ORDER_TYPE_KEY = "loader_order_type_key";

    private MovieOrderType movieOrderType = MovieOrderType.POPULAR;

    private ProgressBar mLoadingMain;

    private RecyclerView mMovieListRecyclerView;

    private MovieListAdapter movieListAdapter;

    private TextView mErrorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLoadingMain = (ProgressBar)  findViewById(R.id.pb_loading_main);

        mMovieListRecyclerView = (RecyclerView) findViewById(R.id.rv_movie_list);

        GridLayoutManager layoutManager = new GridLayoutManager(this, calculateNoOfColumns(getBaseContext()), GridLayoutManager.VERTICAL, false);
        mMovieListRecyclerView.setLayoutManager(layoutManager);

        mMovieListRecyclerView.setHasFixedSize(true);

        movieListAdapter = new MovieListAdapter(this);
        mMovieListRecyclerView.setAdapter(movieListAdapter);

        mErrorTextView = (TextView) findViewById(R.id.tv_error_main);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(INSTANCE_STATE_BUNDLE_ORDER_KEY)) {
                String orderTypeString = savedInstanceState.getString(INSTANCE_STATE_BUNDLE_ORDER_KEY);
                movieOrderType = MovieOrderType.getFromString(orderTypeString);
            }
        }

        startLoader();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(INSTANCE_STATE_BUNDLE_ORDER_KEY, movieOrderType.getValue());
    }

    @Override
    public void onClick(Movie movie) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra("MOVIE", movie);
        startActivity(intent);
    }

    private void startLoader() {

        Bundle loaderBundle = new Bundle();
        loaderBundle.putString(LOADER_ORDER_TYPE_KEY, movieOrderType.getValue());

        LoaderManager loaderManager = getSupportLoaderManager();

        Loader<List<Movie>> movieListLoader = loaderManager.getLoader(LOADER_MOVIE_LIST_ID);
        if (movieListLoader == null) {
            loaderManager.initLoader(LOADER_MOVIE_LIST_ID, loaderBundle, this);
        } else {
            loaderManager.restartLoader(LOADER_MOVIE_LIST_ID, loaderBundle, this);
        }

    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<Movie>>(this) {

            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                if (args == null) {
                    return;
                }
                hideErrorMessage();
                showLoadingIndicator();
                forceLoad();
            }

            @Override
            public List<Movie> loadInBackground() {

                MovieOrderType option = MovieOrderType.getFromString(args.getString(LOADER_ORDER_TYPE_KEY));

                List<Movie> movieList = new ArrayList<>();
                if (!isOnline()) {
                    Log.d(TAG, "No connectivity");
                    showErrorMessage();
                    return movieList;
                }

                URL movieUrl = NetworkUtils.buildUrl(option);

                String response = null;
                try {
                    response = NetworkUtils.getResponseFromHttpUrl(movieUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "There is a problem getting the response");
                    showErrorMessage();
                }


                try {
                    movieList.addAll(MovieJSONUtils.getMoviesFromJson(response));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "There is a problem parsing the JSON");
                    showErrorMessage();
                }

                return movieList;
            }

        };
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> result) {
        hideLoadingIndicator();
        movieListAdapter.setMovieList(result);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int idSelected = item.getItemId();
        if (idSelected == R.id.action_order_popular) {
            movieOrderType = MovieOrderType.POPULAR;
            startLoader();
            return true;
        } else if (idSelected == R.id.action_order_top_rated) {
            movieOrderType = MovieOrderType.TOP_RATED;
            startLoader();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    private void showLoadingIndicator() {
        mMovieListRecyclerView.setVisibility(View.INVISIBLE);
        mLoadingMain.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        mLoadingMain.setVisibility(View.INVISIBLE);
        mMovieListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        mMovieListRecyclerView.setVisibility(View.INVISIBLE);
        mErrorTextView.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        mErrorTextView.setVisibility(View.INVISIBLE);
        mMovieListRecyclerView.setVisibility(View.VISIBLE);
    }

    public int calculateNoOfColumns(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (dpWidth / 180);
        return noOfColumns;
    }
}
