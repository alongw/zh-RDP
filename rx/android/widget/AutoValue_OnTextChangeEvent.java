package rx.android.widget;

import android.widget.TextView;

@javax.annotation.Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_OnTextChangeEvent extends OnTextChangeEvent {
  private final TextView view;
  private final CharSequence text;

  AutoValue_OnTextChangeEvent(
      TextView view,
      CharSequence text) {
    if (view == null) {
      throw new NullPointerException("Null view");
    }
    this.view = view;
    if (text == null) {
      throw new NullPointerException("Null text");
    }
    this.text = text;
  }

  @Override
  public TextView view() {
    return view;
  }

  @Override
  public CharSequence text() {
    return text;
  }

  @Override
  public String toString() {
    return "OnTextChangeEvent{"
        + "view=" + view
        + ", text=" + text
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof OnTextChangeEvent) {
      OnTextChangeEvent that = (OnTextChangeEvent) o;
      return (this.view.equals(that.view()))
          && (this.text.equals(that.text()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= view.hashCode();
    h *= 1000003;
    h ^= text.hashCode();
    return h;
  }
}
