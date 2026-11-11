package com.google.android.gms.tasks;
public class Task {
    private static OnSuccessListener last;
    public Task addOnSuccessListener(OnSuccessListener listener) { last = listener; return this; }
    public static void fireSuccess() { OnSuccessListener listener = last; last = null; listener.onSuccess(new Object()); }
}
