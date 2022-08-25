package rx.android.view;

import android.view.View;

@javax.annotation.Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_OnClickEvent extends OnClickEvent {
  private final View view;

  AutoValue_OnClickEvent(
      View view) {
    if (view == null) {
      throw new NullPointerException("Null view");
    }
    this.view = view;
  }

  @Override
  public View view() {
    return view;
  }

  @Override
  public String toString() {
    return "OnClickEvent{"
        + "view=" + view
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof OnClickEvent) {
      OnClickEvent that = (OnClickEvent) o;
      return (this.view.equals(that.view()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= view.hashCode();
    return h;
  }
}
