# Android-PullToLoadMoreListView

Usage:
```java
        listView = (PullToLoadMoreListView) findViewById(...);
        listView.setPullView(loadView);
        listView.setAdapter(...);
        listView.setOnPullListener(new PullToLoadMoreListView.OnPullListener() {
            @Override
            public void onPull(int distance, int total) {
                // Callback for the touch event
            }

            @Override
            public void onStartLoading() {
                // Load new data
                
                // After loading data, notify the ListView how many items have been loaded,
                // then scroll to right position.
                listView.loaded(5);
            }
        });
```
