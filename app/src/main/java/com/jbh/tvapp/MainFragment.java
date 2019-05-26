/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jbh.tvapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jbh.tvapp.customDesign.MyTitleView;
import com.jbh.tvapp.model.Image;
import com.jbh.tvapp.model.Movie;
import com.jbh.tvapp.player.VideoPlayerPageActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;

public class MainFragment extends BrowseFragment {
    private static final String TAG = "MainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;

    private final Handler mHandler = new Handler();
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        prepareBackgroundManager();
        setupUIElements();
        loadRows();
        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {
        movieList = new ArrayList<>();
        imageList = new ArrayList<>();
        queryNetData();
    }

    private List<Movie> movieList;
    private List<Image> imageList;

    /**
     * 请求视频数据源
     */
    private void queryNetData() {
        BmobQuery<Movie> query = new BmobQuery<>();
        query.setLimit(500);
        query.order("createdAt");
        //v3.5.0版本提供`findObjectsByTable`方法查询自定义表名的数据
        query.findObjects(new FindListener<Movie>() {
            @Override
            public void done(List<Movie> list, BmobException e) {
                if (list != null) {
                    movieList.addAll(list);
                    queryImageData();
                }
            }
        });
    }

    /**
     * 请求图片数据源
     */
    private void queryImageData() {
        BmobQuery<Image> query = new BmobQuery<>();
        query.setLimit(500);
        query.order("createdAt");
        //v3.5.0版本提供`findObjectsByTable`方法查询自定义表名的数据
        query.findObjects(new FindListener<Image>() {
            @Override
            public void done(List<Image> list, BmobException e) {
                if (list != null) imageList.addAll(list);
                setAdapterData();
            }
        });
    }

    private void setAdapterData() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        HeaderItem gridHeader = new HeaderItem(0, "会员专区");
        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.addAll(0, imageList);
        rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        HeaderItem gridHeader2 = new HeaderItem(0, "企业风采");
        GridItemPresenter mGridPresenter2 = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter2 = new ArrayObjectAdapter(mGridPresenter2);
        gridRowAdapter2.addAll(0, movieList);
        rowsAdapter.add(new ListRow(gridHeader2, gridRowAdapter2));

        setAdapter(rowsAdapter);
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        mDefaultBackground = ContextCompat.getDrawable(getActivity(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.lb_preference_item_category_text_color));
        // set search icon color
//        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));
        //手动重写TitleViewAdapter可以自定义头部搜索栏样式UI，此处隐藏了
        setTitleView(new MyTitleView(getActivity()));
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            try {
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    Log.d(TAG, "Item: " + item.toString());
                    Intent intent = new Intent(getActivity(), VideoPlayerPageActivity.class);
                    intent.putExtra(VideoPlayerPageActivity.MOVIE, movie.getVideoFile().getFileUrl());
                    startActivity(intent);
                } else if (item instanceof Image) {
                    Image image = (Image) item;
                    ArrayList<String> imgs = new ArrayList<>();
                    imgs.add(image.getImgFile().getFileUrl());
                    ImageBrowserActivity.startImageBrowserActivity(imgs, 0, getActivity());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {
            try {
                if (item instanceof Movie) {
                    mBackgroundUri = ((Movie) item).getVideoFile().getFileUrl();
                    startBackgroundTimer();
                } else if (item instanceof Image) {
                    mBackgroundUri = ((Image) item).getImgFile().getFileUrl();
                    startBackgroundTimer();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateBackground(mBackgroundUri);
                }
            });
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_background_lay, parent, false);
            rootView.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            rootView.setFocusable(true);
            rootView.setFocusableInTouchMode(true);
            rootView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.default_background));
            return new ViewHolder(rootView);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            try {
                View rootView = viewHolder.view;
                ImageView iv = rootView.findViewById(R.id.iv);
                TextView tv = rootView.findViewById(R.id.tv);
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    Glide.with(getActivity()).load(movie.getImageFile().getFileUrl()).into(iv);
                    tv.setText(movie.getTitle());
                } else if (item instanceof Image) {
                    Image image = (Image) item;
                    Glide.with(getActivity()).load(image.getImgFile().getFileUrl()).into(iv);
                    tv.setText(image.getTitle());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
