package me.drakeet.meizhi;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PictureActivity extends ToolbarActivity {

    public static final String EXTRA_IMAGE_URL = "image_url";
    public static final String EXTRA_IMAGE_TITLE = "image_title";
    public static final String TRANSIT_PIC = "picture";

    private ImageView mImageView;
    private PhotoViewAttacher mPhotoViewAttacher;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_picture;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageView = (ImageView) findViewById(R.id.picture);
        mPhotoViewAttacher = new PhotoViewAttacher(mImageView);
        Picasso.with(this).load(getIntent().getStringExtra(EXTRA_IMAGE_URL)).into(mImageView);

        setAppBarAlpha(0.7f);
        setTitle(getIntent().getStringExtra(EXTRA_IMAGE_TITLE));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewCompat.setTransitionName(mImageView, TRANSIT_PIC);

        mPhotoViewAttacher.setOnViewTapListener(
                new PhotoViewAttacher.OnViewTapListener() {
                    @Override
                    public void onViewTap(View view, float v, float v1) {
                        hideOrShowToolbar();
                    }
                }
        );
        //解决一开始在图片在左上角的问题
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mPhotoViewAttacher.update();
            }
        }, 300);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_picture, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
