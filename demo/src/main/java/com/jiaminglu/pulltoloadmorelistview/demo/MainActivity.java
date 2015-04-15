package com.jiaminglu.pulltoloadmorelistview.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import com.jiaminglu.pulltoloadmorelistview.PullToLoadMoreListView;


public class MainActivity extends ActionBarActivity {

    ArrayList<String> items = new ArrayList<>();

    void loadItems(int count) {
        for (int i = 0; i < count; i++) {
            items.add(0, String.format("Item #%d", items.size()));
        }
        if (-- limitPullTimes == 0)
            listView.setPullEnabled(false);
    }

    BaseAdapter adapter;
    PullToLoadMoreListView listView;
    View loadView;

    int limitPullTimes = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (PullToLoadMoreListView) findViewById(R.id.list);
        loadItems(3);
        listView.setPullEnabled(true);
        listView.setPullView(loadView = getLayoutInflater().inflate(R.layout.load_view, null));
        listView.setAdapter(adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public String getItem(int i) {
                return items.get(i);
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null)
                    view = getLayoutInflater().inflate(R.layout.list_item, viewGroup, false);
                TextView textView = (TextView) view.findViewById(R.id.content);
                textView.setText(getItem(i));
                return view;
            }
        });
        listView.setOnPullListener(new PullToLoadMoreListView.OnPullListener() {
            @Override
            public void onPull(int distance, int total) {
                ((TextView)loadView.findViewById(R.id.text)).setText(distance < total ? "Pull to load more" : "Release to load more");
            }

            @Override
            public void onStartLoading() {
                loadView.findViewById(R.id.text).setVisibility(View.GONE);
                loadView.findViewById(R.id.progress).setVisibility(View.VISIBLE);
                listView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadView.findViewById(R.id.text).setVisibility(View.VISIBLE);
                        loadView.findViewById(R.id.progress).setVisibility(View.GONE);
                        loadItems(5);
                        adapter.notifyDataSetChanged();
                        listView.loaded(5);
                    }
                }, 1000);
            }
        });
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
