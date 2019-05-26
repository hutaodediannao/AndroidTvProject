package com.jbh.tvapp.customDesign;

import android.content.Context;
import android.support.v17.leanback.widget.TitleViewAdapter;
import android.view.View;

public class MyTitleView extends View implements TitleViewAdapter.Provider{

    public MyTitleView(Context context) {
        super(context);
    }

    @Override
    public TitleViewAdapter getTitleViewAdapter() {
        return new TitleViewAdapter() {
            @Override
            public View getSearchAffordanceView() {
                return null;
            }
        };
    }
}
