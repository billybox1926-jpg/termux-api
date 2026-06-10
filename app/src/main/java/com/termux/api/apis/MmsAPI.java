package com.termux.api.apis;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.util.JsonWriter;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * API for sending MMS messages.
 * Fix for issue #240.
 * Uses SmsManager for MMS on supported devices.
 */
public class MmsAPI {

    private static final String LOG_TAG = "MmsAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "send":
                sendMms(apiReceiver, context, intent);
                break;
            case "info":
                mmsInfo(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    @RequiresPermission(allOf = {android.Manifest.permission.SEND_SMS, android.Manifest.permission.READ_PHONE_STATE})
    private static void sendMms(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.WithStringInput() {
            @Override
            public void writeResult(PrintWriter out) {
                try {
                    String[] recipients = intent.getStringArrayExtra("recipients");
                    if (recipients == null) {
                        String recipient = intent.getStringExtra("recipient");
                        if (recipient != null) recipients = new String[]{recipient};
                    }
                    if (recipients == null || recipients.length == 0) {
                        out.println("ERROR: No recipients specified");
                        return;
                    }

                    String subject = intent.getStringExtra("subject");
                    String text = inputString;
                    String attachment = intent.getStringExtra("attachment");

                    SmsManager smsManager = getSmsManager(context, intent);

                    for (String recipient : recipients) {
                        if (attachment != null && !attachment.isEmpty()) {
                            // Send MMS with attachment
                            android.net.Uri attachmentUri = Uri.parse(attachment);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                smsManager.sendMultimediaMessage(context, attachmentUri, null, null, null);
                            } else {
                                // Fallback: send as Intent
                                Intent mmsIntent = new Intent(Intent.ACTION_SEND);
                                mmsIntent.putExtra("address", recipient);
                                mmsIntent.putExtra("sms_body", text);
                                mmsIntent.putExtra(Intent.EXTRA_STREAM, attachmentUri);
                                mmsIntent.setType("image/*");
                                mmsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(mmsIntent);
                            }
                        } else if (text != null && !text.isEmpty()) {
                            // Send as SMS if no attachment
                            ArrayList<String> parts = smsManager.divideMessage(text);
                            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null);
                        }
                    }
                    out.println("MMS sent to " + recipients.length + " recipient(s)");
                } catch (Exception e) {
                    out.println("ERROR: " + e.getMessage());
                }
            }
        });
    }

    private static SmsManager getSmsManager(Context context, Intent intent) {
        int subId = intent.getIntExtra("subscription_id", -1);
        if (subId >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return SmsManager.getSmsManagerForSubscriptionId(subId);
        }
        return SmsManager.getDefault();
    }

    private static void mmsInfo(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(android.util.JsonWriter out) throws Exception {
                out.beginObject();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    if (sm != null) {
                        out.name("subscriptions");
                        out.beginArray();
                        for (SubscriptionInfo info : sm.getActiveSubscriptionInfoList()) {
                            out.beginObject();
                            out.name("id").value(info.getSubscriptionId());
                            out.name("carrier").value(info.getCarrierName());
                            out.name("number").value(info.getNumber());
                            out.endObject();
                        }
                        out.endArray();
                    }
                }
                out.endObject();
            }
        });
    }
}
