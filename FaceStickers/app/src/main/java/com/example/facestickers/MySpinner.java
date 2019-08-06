package com.example.facestickers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;
import androidx.appcompat.widget.AppCompatSpinner;

public class MySpinner extends AppCompatSpinner {
    OnItemSelectedListener listener;

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        if (listener != null)
            listener.onItemSelected(null, null, position, 0);
    }

    public void setOnItemSelectedEvenIfUnchangedListener(
            OnItemSelectedListener listener) {
        this.listener = listener;
    }
}
