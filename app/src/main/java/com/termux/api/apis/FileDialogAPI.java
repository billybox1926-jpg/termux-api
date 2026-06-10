package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

/** File dialog, directory picker, and intent result launcher. Fix for #566 and #567. */
public class FileDialogAPI {
    private static final String LOG_TAG = "FileDialogAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out -> out.println("Missing 'action' extra"));
            return;
        }
        switch (action) {
            case "save": saveDialog(context, intent); break;
            case "pickdir": pickDirectory(context, intent); break;
            case "launch": launchForResult(apiReceiver, context, intent); break;
            default: ResultReturner.returnData(apiReceiver, intent, out -> out.println("Unknown: " + action));
        }
    }

    static void saveDialog(Context ctx, Intent intent) {
        Intent i = new Intent(ctx, SaveDialogActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResultReturner.copyIntentExtras(intent, i);
        ctx.startActivity(i);
    }

    static void pickDirectory(Context ctx, Intent intent) {
        Intent i = new Intent(ctx, DirPickerActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResultReturner.copyIntentExtras(intent, i);
        ctx.startActivity(i);
    }

    static void launchForResult(TermuxApiReceiver rec, Context ctx, Intent intent) {
        Intent i = new Intent(ctx, IntentResultActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResultReturner.copyIntentExtras(intent, i);
        ctx.startActivity(i);
        ResultReturner.returnData(rec, intent, out -> out.println("Intent launched"));
    }

    public static class SaveDialogActivity extends AppCompatActivity {
        boolean done = false;
        @Override protected void onCreate(Bundle s) {
            super.onCreate(savedInstanceState);
            String mime = getIntent().getStringExtra("mimetype"); if (mime == null) mime = "*/*";
            String fn = getIntent().getStringExtra("filename"); if (fn == null) fn = "file";
            try { startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE, fn), 3001); }
            catch (Exception e) { done=true; ResultReturner.returnData(this, getIntent(), o->o.println("ERROR: "+e)); finishAndRemoveTask(); }
        }
        @Override protected void onActivityResult(int rc, int res, Intent d) {
            done=true;
            if (d != null && d.getData() != null) {
                try { getContentResolver().takePersistableUriPermission(d.getData(), d.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch (Exception ignored) {}
                ResultReturner.returnData(this, getIntent(), o->o.println(d.getDataString()));
            } else ResultReturner.returnData(this, getIntent(), o->o.println(""));
            finishAndRemoveTask();
        }
        @Override protected void onDestroy() { super.onDestroy(); if (!done) { done=true; try { ResultReturner.returnData(this,getIntent(),o->o.println("")); } catch (Exception ignored) {} } }
    }

    public static class DirPickerActivity extends AppCompatActivity {
        boolean done = false;
        @Override protected void onCreate(Bundle s) {
            super.onCreate(savedInstanceState);
            try { startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), 3002); }
            catch (Exception e) { done=true; ResultReturner.returnData(this,getIntent(),o->o.println("ERROR: "+e)); finishAndRemoveTask(); }
        }
        @Override protected void onActivityResult(int rc, int res, Intent d) {
            done=true;
            if (d != null && d.getData() != null) {
                try { getContentResolver().takePersistableUriPermission(d.getData(), d.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch (Exception ignored) {}
                ResultReturner.returnData(this, getIntent(), o->o.println(d.getDataString()));
            } else ResultReturner.returnData(this, getIntent(), o->o.println(""));
            finishAndRemoveTask();
        }
        @Override protected void onDestroy() { super.onDestroy(); if (!done) { done=true; try { ResultReturner.returnData(this,getIntent(),o->o.println("")); } catch (Exception ignored) {} } }
    }

    public static class IntentResultActivity extends AppCompatActivity {
        boolean done = false;
        @Override protected void onCreate(Bundle s) {
            super.onCreate(savedInstanceState);
            String a = getIntent().getStringExtra("intent_action");
            if (a == null) { done=true; ResultReturner.returnData(this,getIntent(),o->o.println("ERROR: no intent_action")); finishAndRemoveTask(); return; }
            Intent li = new Intent(a);
            String d = getIntent().getStringExtra("intent_data"); if (d != null) li.setData(Uri.parse(d));
            String t = getIntent().getStringExtra("intent_type"); if (t != null) li.setType(t);
            try { startActivityForResult(li, 3003); }
            catch (Exception e) { done=true; ResultReturner.returnData(this,getIntent(),o->o.println("ERROR: "+e)); finishAndRemoveTask(); }
        }
        @Override protected void onActivityResult(int rc, int res, Intent d) {
            done=true;
            ResultReturner.returnData(this, getIntent(), o->{
                StringBuilder sb = new StringBuilder();
                sb.append("{\"result_code\":").append(res);
                if (d != null) {
                    sb.append(",\"data\":\"").append(d.getDataString()!=null?d.getDataString():"").append("\"");
                    Bundle ex = d.getExtras();
                    if (ex != null) { sb.append(",\"extras\":{"); boolean f=true;
                        for (String k : ex.keySet()) { if (!f) sb.append(",");
                            Object v = ex.get(k); sb.append("\"").append(k).append("\":\"").append(v!=null?v.toString():"").append("\""); f=false;
                        } sb.append("}");
                    }
                } sb.append("}"); o.println(sb.toString());
            });
            finishAndRemoveTask();
        }
        @Override protected void onDestroy() { super.onDestroy(); if (!done) { done=true; try { ResultReturner.returnData(this,getIntent(),o->o.println("")); } catch (Exception ignored) {} } }
    }
}
