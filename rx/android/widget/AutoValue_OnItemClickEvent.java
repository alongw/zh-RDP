package rx.android.widget;

import android.view.View;
import android.widget.AdapterView;

@javax.annotation.Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_OnItemClickEvent extends OnItemClickEvent {
  private final AdapterView<?> parent;
  private final View view;
  private final int position;
  private final long id;

  AutoValue_OnItemClickEvent(
      AdapterView<?> parent,
      View view,
      int position,
      long id) {
    if (parent == null) {
      throw new NullPointerException("Null parent");
    }
    this.parent = parent;
    if (view == null) {
      throw new NullPointerException("Null view");
    }
    this.view = view;
    this.position = position;
    this.id = id;
  }

  @Override
  public AdapterView<?> parent() {
    return parent;
  }

  @Override
  public View view() {
    return view;
  }

  @Override
  public int position() {
    return position;
  }

  @Override
  public long id() {
    return id;
  }

  @Override
  public String toString() {
    return "OnItemClickEvent{"
        + "parent=" + parent
        + ", view=" + view
        + ", position=" + position
        + ", id=" + id
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof OnItemClickEvent) {
      OnItemClickEvent that = (OnItemClickEvent) o;
      return (this.parent.equals(that.parent()))
          && (this.view.equals(that.view()))
          && (this.position == that.position())
          && (this.id == that.id());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= parent.hashCode();
    h *= 1000003;
    h ^= view.hashCode();
    h *= 1000003;
    h ^= position;
    h *= 1000003;
    h ^= (id >>> 32) ^ id;
    return h;
  }
}
