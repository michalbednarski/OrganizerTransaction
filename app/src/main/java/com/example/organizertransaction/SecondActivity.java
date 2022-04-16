package com.example.organizertransaction;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.SurfaceControl;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint("PrivateApi")
public class SecondActivity extends Activity {

    public static final String[] ACTION_NAMES = {
            "IntentsLab",
            "Shutdown"
    };
    private SurfaceControl mLeash;
    private Class<?> mOrganizerClass;
    private Object mOrganizer;
    private IBinder mActivityToken;
    private Binder mFragmentToken;
    private Spinner mSpinner;
    private CheckBox mZoomAndScale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        mSpinner = findViewById(R.id.spinner);
        mSpinner.setAdapter(new ArrayAdapter<>(mSpinner.getContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.destinations)));
        mZoomAndScale = findViewById(R.id.zoom_and_set_alpha);
        mZoomAndScale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    buttonView.setEnabled(false);
                    if (mLeash != null) {
                        doZoomAndScale();
                    }
                }
            }
        });
    }

    public void doStuff2(View view) throws Exception {
        if (mOrganizer == null) {
            initOrganizerAndFragment();
        }
        Intent intent = null;
        switch (mSpinner.getSelectedItemPosition()) {
            case 0:
                intent = new Intent(Settings.ACTION_SETTINGS);
                break;
            case 1:
                intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
                break;
        }
        startActivityInOrganizer(intent);
    }

    private void doZoomAndScale() {
        try {
            moveAndScale(mLeash, 0.5f, 1000, 0, 0.5f, 0.5f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void onTaskAppeared() {
        if (mZoomAndScale.isChecked()) {
            doZoomAndScale();
        }
    }

    void initOrganizerAndFragment() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        mActivityToken = (IBinder) Activity.class.getMethod("getActivityToken").invoke(this);
        mFragmentToken = new Binder("FragmentToken");

        mOrganizerClass = Class.forName("android.window.TaskFragmentOrganizer");
        mOrganizer = mOrganizerClass.getConstructor(Executor.class)
                .newInstance((Executor) this::executorFn);

        Object transactionBuilder = Class.forName("android.window.TaskFragmentCreationParams$Builder")
                .getConstructor(
                        Class.forName("android.window.TaskFragmentOrganizerToken"),
                        IBinder.class,
                        IBinder.class
                )
                .newInstance(
                        mOrganizerClass.getMethod("getOrganizerToken").invoke(mOrganizer), // organizer
                        mFragmentToken, // fragmentToken
                        mActivityToken // ownerToken
                );

        Class<?> transactionClass = Class.forName("android.window.WindowContainerTransaction");
        Object transaction = transactionClass.newInstance();

        transactionClass
                .getMethod("createTaskFragment", Class.forName("android.window.TaskFragmentCreationParams"))
                .invoke(transaction, transactionBuilder.getClass().getMethod("build").invoke(transactionBuilder));


        mOrganizerClass.getMethod("registerOrganizer").invoke(mOrganizer);

        mOrganizerClass.getMethod("applyTransaction", transactionClass).invoke(mOrganizer, transaction);
    }

    void startActivityInOrganizer(Intent intent) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> transactionClass = Class.forName("android.window.WindowContainerTransaction");
        Object transaction = transactionClass.newInstance();

        transactionClass
                .getMethod(
                        "startActivityInTaskFragment",
                        IBinder.class, // container
                        IBinder.class, // reparentContainer
                        Intent.class, // activityIntent
                        Bundle.class // launchOptions
                )
                .invoke(
                        transaction,
                        mFragmentToken,
                        mActivityToken,
                        intent,
                        null
                );

        mOrganizerClass.getMethod("applyTransaction", transactionClass).invoke(mOrganizer, transaction);
    }

    void executorFn(Runnable r) {
        r.run();

        Class<? extends Runnable> rClass = r.getClass();
        Field field = null;
        try {
            field = rClass.getDeclaredField("f$1");
        } catch (NoSuchFieldException e) {
        }
        String fieldTypeName = null;
        if (field != null) {
            fieldTypeName = field.getType().getName();
        }
        if ("android.window.TaskFragmentAppearedInfo".equals(fieldTypeName)) {
            try {
                field.setAccessible(true);
                Object info = field.get(r);
                mLeash = (SurfaceControl) info.getClass().getMethod("getLeash").invoke(info);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            runOnUiThread(this::onTaskAppeared);
        }
    }

    private void moveAndScale(SurfaceControl sc, float alpha, float positionX, float positionY, float scaleX, float scaleY) throws Exception {
        SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
        transaction.setAlpha(sc, alpha);
        transaction.getClass()
                .getMethod(
                        "setMatrix",
                        SurfaceControl.class,
                        float.class,
                        float.class,
                        float.class,
                        float.class
                )
                .invoke(
                        transaction,
                        sc,
                        scaleX,
                        0.0f,
                        0.0f,
                        scaleY
                );
//        transaction.getClass()
//                .getMethod(
//                        "setPosition",
//                        SurfaceControl.class,
//                        float.class,
//                        float.class
//                )
//                .invoke(
//                        transaction,
//                        sc,
//                        positionX,
//                        positionY
//                );
        transaction.apply();
    }
}