package com.project.movies.popular.popularmovies1;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.project.movies.popular.popularmovies1.utilities.MovieJSONUtils;
import com.project.movies.popular.popularmovies1.utilities.NetworkUtils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MovieListAdapter.MovieListAdapterOnClickHandler {

    private static final String TAG = MainActivity.class.getSimpleName();

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

        GridLayoutManager layoutManager = new GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false);
        mMovieListRecyclerView.setLayoutManager(layoutManager);

        mMovieListRecyclerView.setHasFixedSize(true);

        movieListAdapter = new MovieListAdapter(this);
        mMovieListRecyclerView.setAdapter(movieListAdapter);

        mErrorTextView = (TextView) findViewById(R.id.tv_error_main);

        new FetchMovieTask().execute(MovieOrderType.POPULAR);

    }

    @Override
    public void onClick(Movie movie) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra("movieId", movie.getId());
        intent.putExtra("movieTitle", movie.getTitle());
        intent.putExtra("moviePoster", movie.getPosterPath());
        startActivity(intent);
    }

    public class FetchMovieTask extends AsyncTask<MovieOrderType, Void, List<Movie>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            hideErrorMessage();
            showLoadingIndicator();
        }

        @Override
        protected List<Movie> doInBackground(MovieOrderType... params) {

            MovieOrderType option = MovieOrderType.POPULAR;
            if (params != null) {
                option = params[0];
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

            List<Movie> movieList = new ArrayList<>();
            try {
                movieList.addAll(MovieJSONUtils.getMoviesFromJson(response));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, "There is a problem parsing the JSON");
                showErrorMessage();
            }

            return movieList;
        }

        @Override
        protected void onPostExecute(List<Movie> result) {
            super.onPostExecute(result);
            hideLoadingIndicator();
            movieListAdapter.setMovieList(result);
        }

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
            new FetchMovieTask().execute(MovieOrderType.POPULAR);
            return true;
        } else if (idSelected == R.id.action_order_top_rated) {
            new FetchMovieTask().execute(MovieOrderType.TOP_RATED);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

}
