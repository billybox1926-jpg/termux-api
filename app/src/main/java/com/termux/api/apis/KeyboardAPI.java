package com.termux.api.apis;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.PrintWriter;

/**
 * API for requesting soft-keyboard show/hide actions and querying keyboard visibility.
 */
public class KeyboardAPI {

    private static final String LOG_TAG = "KeyboardAPI";
    private static final String EXTRA_KEYBOARD_ACTION = "keyboard_action";

    private static final String ACTION_SHOW = "show";
    private static final String ACTION_HIDE = "hide";
    private static final String ACTION_VISIBLE = "visible";

    public static void onReceiveShow(final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveShow");
        startKeyboardActivity(context, intent, ACTION_SHOW);
    }

    public static void onReceiveHide(final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveHide");
        startKeyboardActivity(context, intent, ACTION_HIDE);
    }

    public static void onReceiveVisible(final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceiveVisible");
        startKeyboardActivity(context, intent, ACTION_VISIBLE);
    }

    private static void startKeyboardActivity(final Context context, final Intent intent, final String action) {
        Intent activityIntent = new Intent(context, KeyboardActivity.class);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            activityIntent.putExtras(extras);
        }
        activityIntent.putExtra(EXTRA_KEYBOARD_ACTION, action);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(activityIntent);
    }

    public static class KeyboardActivity extends Activity {

        private static final long RESULT_DELAY_MILLIS = 150L;
        private boolean resultReturned = false;
        private FrameLayout rootView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Logger.logDebug(LOG_TAG, "KeyboardActivity.onCreate");
            super.onCreate(savedInstanceState);

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

            rootView = new FrameLayout(this);
            rootView.setFocusableInTouchMode(true);
            setContentView(rootView);

            final String action = getIntent().getStringExtra(EXTRA_KEYBOARD_ACTION);
            rootView.post(() -> handleAction(action == null ? ACTION_VISIBLE : action));
        }

        @Override
        protected void onDestroy() {
            Logger.logDebug(LOG_TAG, "KeyboardActivity.onDestroy");
            if (!resultReturned) {
                returnDone();
            }
            super.onDestroy();
        }

        private void handleAction(final String action) {
            switch (action) {
                case ACTION_SHOW:
                    showKeyboard();
                    break;
                case ACTION_HIDE:
                    hideKeyboard();
                    break;
                case ACTION_VISIBLE:
                    returnVisibleAfterInsetsSettle();
                    break;
                default:
                    Logger.logError(LOG_TAG, "Unknown keyboard action: " + action);
                    returnDone();
            }
        }

        private void showKeyboard() {
            EditText editText = new EditText(this);
            editText.setSingleLine(true);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setAlpha(0.01f);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
            rootView.addView(editText, params);

            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }

            new Handler(Looper.getMainLooper()).postDelayed(this::returnDone, RESULT_DELAY_MILLIS);
        }

        private void hideKeyboard() {
            rootView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(rootView.getWindowToken(), 0);
            }

            new Handler(Looper.getMainLooper()).postDelayed(this::returnDone, RESULT_DELAY_MILLIS);
        }

        private void returnVisibleAfterInsetsSettle() {
            new Handler(Looper.getMainLooper()).postDelayed(() -> returnVisible(isKeyboardVisible(rootView)), RESULT_DELAY_MILLIS);
        }

        private boolean isKeyboardVisible(final View view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsets insets = view.getRootWindowInsets();
                if (insets != null) {
                    return insets.isVisible(WindowInsets.Type.ime());
                }
            }

            Rect visibleFrame = new Rect();
            view.getWindowVisibleDisplayFrame(visibleFrame);
            int rootHeight = view.getRootView().getHeight();
            int hiddenHeight = rootHeight - visibleFrame.bottom;

            return rootHeight > 0 && hiddenHeight > rootHeight * 0.15f;
        }

        private synchronized void returnVisible(final boolean visible) {
            if (resultReturned) {
                return;
            }
            resultReturned = true;

            ResultReturner.returnData(this, getIntent(), new ResultReturner.ResultWriter() {
                @Override
                public void writeResult(PrintWriter out) {
                    out.println(visible ? "true" : "false");
                }
            });
        }

        private synchronized void returnDone() {
            if (resultReturned) {
                return;
            }
            resultReturned = true;
            ResultReturner.returnData(this, getIntent(), null);
        }
    }
}
