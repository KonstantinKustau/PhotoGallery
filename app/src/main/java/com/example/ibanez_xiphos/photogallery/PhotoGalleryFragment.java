package com.example.ibanez_xiphos.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    public static int page = 1;
    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter adapter;
    private List<GalleryItem.Photos.Photo> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private LruCache<Integer, Bitmap> mMemoryCache;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        initCache();
        new FetchItemsTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {

                    @Override
                    public void onThumbnailDownloaded(final PhotoHolder holder, final Bitmap thumbnail) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (getBitmapFromMemCache(holder.getAdapterPosition()) == null) {
                                    Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                                    holder.bindDrawable(drawable);
                                }
                                addBitmapToMemoryCache(holder.getAdapterPosition(), thumbnail);
                            }
                        });
                    }
                }
        );


        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!ViewCompat.canScrollVertically(recyclerView, 1)) {
                    Log.i(TAG, "onScrolled");
                    loadMoreData();
                }
            }
        });

        return v;
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


    private void setupAdapter() {
        if (isAdded()) {
            adapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(adapter);
        }
    }

    private void loadMoreData() {
        new FetchItemsTask().execute();
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem.Photos.Photo>> {

        @Override
        protected List<GalleryItem.Photos.Photo> doInBackground(Void... voids) {
            return new FlickrFetchr().fetchItems(page++);
        }

        @Override
        protected void onPostExecute(List<GalleryItem.Photos.Photo> items) {
            mItems.addAll(items);
            adapter.notifyDataSetChanged();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

//        public void bindGalleryItem(GalleryItem.Photos.Photo galleryItem) {
//            Picasso.with(getActivity())
//                    .load(galleryItem.getUrl_s())
//                    .placeholder(R.mipmap.bill_up_close)
//                    .into(mItemImageView);
//        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem.Photos.Photo> mGalleryItems;

        public PhotoAdapter(List<GalleryItem.Photos.Photo> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            Drawable placeholder;
            GalleryItem.Photos.Photo galleryItem = mGalleryItems.get(position);

            if(getBitmapFromMemCache(holder.getAdapterPosition()) == null) {
                placeholder = getResources().getDrawable(R.mipmap.bill_up_close);
            } else {
                placeholder = new BitmapDrawable(getResources(), getBitmapFromMemCache(holder.getAdapterPosition()));
            }
            holder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl_s());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        page = 0;
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }
}
