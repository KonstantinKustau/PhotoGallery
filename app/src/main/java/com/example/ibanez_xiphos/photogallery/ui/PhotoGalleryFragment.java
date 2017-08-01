package com.example.ibanez_xiphos.photogallery.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.ibanez_xiphos.photogallery.R;
import com.example.ibanez_xiphos.photogallery.other.FlickrFetchr;
import com.example.ibanez_xiphos.photogallery.other.GalleryItem;
import com.example.ibanez_xiphos.photogallery.other.MemoryCache;
import com.example.ibanez_xiphos.photogallery.other.QueryPreferences;
import com.example.ibanez_xiphos.photogallery.other.ThumbnailDownloader;
import com.example.ibanez_xiphos.photogallery.service.PollService;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";

    public static int page = 1;
    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter adapter;
    private List<GalleryItem.Photos.Photo> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private MemoryCache memoryCache;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        memoryCache = new MemoryCache();
        updateItems();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {

                    @Override
                    public void onThumbnailDownloaded(final PhotoHolder holder, final Bitmap thumbnail) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (memoryCache.getBitmapFromMemCache(holder.getAdapterPosition()) == null) {
                                    Log.d("TAG1", "Uploaded");
                                    Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                                    holder.bindDrawable(drawable);
                                }
                                memoryCache.addBitmapToMemoryCache(holder.getAdapterPosition(), thumbnail);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            adapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(adapter);
        }
    }

    private void loadMoreData() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        memoryCache.clearCache();
        new FetchItemsTask(query).execute();
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem.Photos.Photo>> {

        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem.Photos.Photo> doInBackground(Void ...voids) {
            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(page++);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, page++);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem.Photos.Photo> items) {
            mItems.addAll(items);
            adapter.notifyDataSetChanged();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener   {

        private ImageView mItemImageView;
        private GalleryItem.Photos.Photo mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem.Photos.Photo galleryItem) {
            mGalleryItem = galleryItem;
        }

//        public void bindGalleryItem(GalleryItem.Photos.Photo galleryItem) {
//            Picasso.with(getActivity())
//                    .load(galleryItem.getUrl_s())
//                    .placeholder(R.mipmap.bill_up_close)
//                    .into(mItemImageView);
//        }

        @Override
        public void onClick(View view) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), getPhotoPageUri());
            startActivity(i);
        }

        private Uri getPhotoPageUri() {
            return Uri.parse("http://www.flickr.com/photos/")
                    .buildUpon()
                    .appendPath(mGalleryItem.getOwner())
                    .appendPath(mGalleryItem.getId())
                    .build();
        }
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
            holder.bindGalleryItem(galleryItem);

            if (memoryCache.getBitmapFromMemCache(holder.getAdapterPosition()) == null) {
                placeholder = getResources().getDrawable(R.mipmap.bill_up_close);
            } else {
                Log.d("TAG1", "PhotoGalleryFragment: placeholder second");
                placeholder = new BitmapDrawable(getResources(), memoryCache.getBitmapFromMemCache(holder.getAdapterPosition()));
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
