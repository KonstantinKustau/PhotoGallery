package com.example.ibanez_xiphos.photogallery.other;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class MemoryCache {

    private LruCache<Integer, Bitmap> mMemoryCache;

    public MemoryCache() {
       initCache();
    }

    private void initCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory());
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer position, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
    }

    public void addBitmapToMemoryCache(Integer key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(Integer key) {
        return mMemoryCache.get(key);
    }

    public void clearCache() {
        for(int i = 0; i < mMemoryCache.size(); i++) {
            mMemoryCache.remove(i);
        }
    }
}
