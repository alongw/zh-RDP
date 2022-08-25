package rx.android.view;

import android.widget.CompoundButton;

@javax.annotation.Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_OnCheckedChangeEvent extends OnCheckedChangeEvent {
  private final CompoundButton view;
  private final boolean value;

  AutoValue_OnCheckedChangeEvent(
      CompoundButton view,
      boolean value) {
    if (view == null) {
      throw new NullPointerException("Null view");
    }
    this.view = view;
    this.value = value;
  }

  @Override
  public CompoundButton view() {
    return view;
  }

  @Override
  public boolean value() {
    return value;
  }

  @Override
  public String toString() {
    return "OnCheckedChangeEvent{"
        + "view=" + view
        + ", value=" + value
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof OnCheckedChangeEvent) {
      OnCheckedChangeEvent that = (OnCheckedChangeEvent) o;
      return (this.view.equals(that.view()))
          && (this.value == that.value());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= view.hashCode();
    h *= 1000003;
    h ^= value ? 1231 : 1237;
    return h;
  }
}
