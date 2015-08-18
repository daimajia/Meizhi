package me.drakeet.meizhi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import me.drakeet.meizhi.model.Meizhi;
import me.drakeet.meizhi.util.DateUtils;
import me.drakeet.meizhi.util.HttpUtils;
import me.drakeet.meizhi.util.TaskUtils;
import me.drakeet.meizhi.util.ToastUtils;

public class MainActivity extends SwipeRefreshBaseActivity {

    RecyclerView mRecyclerView;
    Handler mHandler;
    MeizhiListAdapter mMeizhiListAdapter;
    List<Meizhi> mMeizhiList;
    boolean mIsDbInited, mIsFirstTimeTouchBottom = true;
    int mOffset = 0;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mMeizhiList = new ArrayList<>();
        setUpRecyclerView();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        getData();
                    }
                }, 358
        );
    }

    private void setUpRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_meizhi);
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(
                2, StaggeredGridLayoutManager.VERTICAL
        );
        mRecyclerView.setLayoutManager(layoutManager);
        mMeizhiListAdapter = new MeizhiListAdapter(this, mMeizhiList);
        mRecyclerView.setAdapter(mMeizhiListAdapter);
        mRecyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView rv, int dx, int dy) {
                        if (!mSwipeRefreshLayout.isRefreshing() && layoutManager.findLastCompletelyVisibleItemPositions(
                                new int[2]
                        )[1] >= mMeizhiListAdapter.getItemCount() - 2) {
                            if (!mIsFirstTimeTouchBottom) {
                                mSwipeRefreshLayout.setRefreshing(true);
                                mOffset += 20;
                                getData();
                            } else {
                                mIsFirstTimeTouchBottom = false;
                            }
                        }
                    }
                }
        );
    }

    private void getData(final boolean addFromDb) {
        setRefreshing(true);
        TaskUtils.executeAsyncTask(
                new AsyncTask<Object, Object, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Object... params) {
                        if (!mIsDbInited) {
                            mIsDbInited = OldMeizhi.init();
                            //测试数据在测试时去掉注释，因为最近没有数据; from XellosRyan
                            //mMeizhiList.addAll(DataSupport.findAll(Meizhi.class));
                        }
                        HttpUtils httpUtils = new HttpUtils();
                        Calendar calendar = Calendar.getInstance();
                        Date today = new Date();
                        calendar.set(2015, 5, 21);
                        Date thatDay = calendar.getTime();

                        int oLength = mMeizhiList.size();
                        while (thatDay.compareTo(today) <= 0) {
                            String dateString = DateUtils.toDate(thatDay);
                            thatDay = DateUtils.getNextdayDate(thatDay);
                            List<Meizhi> qList = DataSupport.where("mid = ?", dateString).find(Meizhi.class);
                            if (qList.size() > 0) {
                                continue;
                            }
                            publishProgress(dateString);

                            Meizhi meizhi = new Meizhi();
                            meizhi.setMid(dateString);

                            String httpContent = httpUtils.download(getString(R.string.api) + dateString);
                            int s0 = httpContent.indexOf("<img");
                            if (s0 == -1) {
                                meizhi.setUrl(getString(R.string.no_data_the_day));
                                if (!DateUtils.toDate(DateUtils.getLastdayDate(thatDay)).equals(DateUtils.toDate(today)))
                                    meizhi.save();
                                continue;
                            }

                            int s1 = httpContent.indexOf("src=\"", s0) + "src=\"".length();
                            int e1 = httpContent.indexOf("\"", s1);
                            meizhi.setUrl(httpContent.substring(s1, e1));

                            int s2 = httpContent.indexOf("height:", e1) + "height:".length();
                            int e2 = httpContent.indexOf("px", s2);
                            meizhi.setThumbHeight(Integer.valueOf(httpContent.substring(s2, e2)));

                            int s3 = httpContent.indexOf("width:", e1) + "width:".length();
                            int e3 = httpContent.indexOf("px", s3);
                            meizhi.setThumbWidth(Integer.valueOf(httpContent.substring(s3, e3)));

                            meizhi.save();
                            if (!addFromDb) {
                                mMeizhiList.add(meizhi);
                            }
                        }

                        if (addFromDb) {
                            List<Meizhi> meizhiList = DataSupport.offset(mOffset)
                                                                 .limit(20)
                                                                 .order("id desc")
                                                                 .find(Meizhi.class);
                            for (Meizhi meizhi : meizhiList) {
                                if (!meizhi.getUrl().equals(getString(R.string.no_data_the_day))) {
                                    mMeizhiList.add(meizhi);
                                }
                            }
                        }
                        return mMeizhiList.size() > oLength;
                    }

                    @Override
                    protected void onProgressUpdate(Object... values) {
                        super.onProgressUpdate(values);
                        ToastUtils.showShort(getString(R.string.loading_num_of) + values[0]);
                    }

                    @Override
                    protected void onPostExecute(Boolean o) {
                        super.onPostExecute(o);
                        if (o)
                            mMeizhiListAdapter.notifyDataSetChanged();
                        setRefreshing(false);
                    }
                }
        );

    }

    private void getData() {
        getData(true);
    }

    @Override
    public void onToolbarClick() {
                super.onToolbarClick();
        mRecyclerView.smoothScrollToPosition(0);
    }

    public void onFab(View v) {
        mRecyclerView.smoothScrollToPosition(0);
        requestDataRefresh();
    }

    @Override
    public void requestDataRefresh() {
        super.requestDataRefresh();
        getData(/* add from db */ false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
