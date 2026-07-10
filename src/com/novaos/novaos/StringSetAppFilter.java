package com.novaos.novaos;

import android.content.ComponentName;
import android.content.Context;

public class StringSetAppFilter implements AppFilter {
    @Override
    public boolean shouldShowApp(ComponentName app, Context context) {
        return true;
    }
}
