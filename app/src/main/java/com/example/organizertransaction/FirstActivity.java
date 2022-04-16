package com.example.organizertransaction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

public class FirstActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
    }

    public void doStuff(View view) {
        // Intent that will be launched by ChooserActivity
        // (at first ChooserActivity will launch ResolverActivity,
        // then ResolverActivity will launch SecondActivity)
        Intent targetIntent = new Intent("com.example.organizertransaction.SECOND_ACTIVITY");
        targetIntent.setClassName("android", "com.android.internal.app.ResolverActivity");

        // Intent that will launch ChooserActivity
        // Only this Intent of all in chain has FLAG_ACTIVITY_NEW_TASK
        Intent chooserIntent = Intent.createChooser(
                new Intent("com.example.organizertransaction.NO_SUCH_ACTIVITY"), null
        );
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[] { targetIntent });
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(chooserIntent);
    }

}