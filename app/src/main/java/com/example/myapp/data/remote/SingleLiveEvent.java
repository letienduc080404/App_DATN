package com.example.myapp.data.remote;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LiveData chỉ phát sự kiện một lần duy nhất (Single-event).
 * Dùng cho các sự kiện như Navigation, Show Snackbar/Toast, Logout mà không bị phát lại khi xoay màn hình hoặc tạo mới Observer.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private static final String TAG = "SingleLiveEvent";
    private final AtomicBoolean mPending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Nhiều observer đang đăng ký nhưng chỉ có 1 observer nhận được thông báo.");
        }

        super.observe(owner, t -> {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        mPending.set(true);
        super.setValue(t);
    }

    @Override
    public void postValue(@Nullable T t) {
        mPending.set(true);
        super.postValue(t);
    }

    public void reset() {
        mPending.set(false);
    }
}
