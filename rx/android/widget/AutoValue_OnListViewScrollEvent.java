package rx.android.widget;

import android.widget.AbsListView;

@javax.annotation.Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_OnListViewScrollEvent extends OnListViewScrollEvent {
  private final AbsListView listView;
  private final int scrollState;
  private final int firstVisibleItem;
  private final int visibleItemCount;
  private final int totalItemCount;

  AutoValue_OnListViewScrollEvent(
      AbsListView listView,
      int scrollState,
      int firstVisibleItem,
      int visibleItemCount,
      int totalItemCount) {
    if (listView == null) {
      throw new NullPointerException("Null listView");
    }
    this.listView = listView;
    this.scrollState = scrollState;
    this.firstVisibleItem = firstVisibleItem;
    this.visibleItemCount = visibleItemCount;
    this.totalItemCount = totalItemCount;
  }

  @Override
  public AbsListView listView() {
    return listView;
  }

  @Override
  public int scrollState() {
    return scrollState;
  }

  @Override
  public int firstVisibleItem() {
    return firstVisibleItem;
  }

  @Override
  public int visibleItemCount() {
    return visibleItemCount;
  }

  @Override
  public int totalItemCount() {
    return totalItemCount;
  }

  @Override
  public String toString() {
    return "OnListViewScrollEvent{"
        + "listView=" + listView
        + ", scrollState=" + scrollState
        + ", firstVisibleItem=" + firstVisibleItem
        + ", visibleItemCount=" + visibleItemCount
        + ", totalItemCount=" + totalItemCount
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof OnListViewScrollEvent) {
      OnListViewScrollEvent that = (OnListViewScrollEvent) o;
      return (this.listView.equals(that.listView()))
          && (this.scrollState == that.scrollState())
          && (this.firstVisibleItem == that.firstVisibleItem())
          && (this.visibleItemCount == that.visibleItemCount())
          && (this.totalItemCount == that.totalItemCount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= listView.hashCode();
    h *= 1000003;
    h ^= scrollState;
    h *= 1000003;
    h ^= firstVisibleItem;
    h *= 1000003;
    h ^= visibleItemCount;
    h *= 1000003;
    h ^= totalItemCount;
    return h;
  }
}
