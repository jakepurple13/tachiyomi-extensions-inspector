package android.view;

import android.graphics.drawable.Drawable;

public class View implements Drawable.Callback {

    @Override
    public void invalidateDrawable(Drawable who) {

    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {

    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {

    }
}
