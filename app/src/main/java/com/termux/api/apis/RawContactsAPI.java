package com.termux.api.apis;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.JsonWriter;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.util.ResultReturner;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * API for reading and writing raw contacts via ContactsContract.
 * Fix for issue #766.
 * Requires android.permission.READ_CONTACTS and WRITE_CONTACTS.
 */
public class RawContactsAPI {

    private static final String LOG_TAG = "RawContactsAPI";

    public static void onReceive(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        Logger.logDebug(LOG_TAG, "onReceive");

        final String action = intent.getStringExtra("action");
        if (action == null) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'action' extra"));
            return;
        }

        switch (action) {
            case "list":
                listContacts(apiReceiver, context, intent);
                break;
            case "get":
                getContact(apiReceiver, context, intent);
                break;
            case "add":
                addContact(apiReceiver, context, intent);
                break;
            case "delete":
                deleteContact(apiReceiver, context, intent);
                break;
            default:
                ResultReturner.returnData(apiReceiver, intent, out ->
                        out.println("ERROR: Unknown action: " + action));
        }
    }

    /**
     * List all raw contacts with basic info.
     */
    private static void listContacts(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                ContentResolver cr = context.getContentResolver();
                out.beginArray();

                try (Cursor c = cr.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        new String[]{
                                ContactsContract.RawContacts._ID,
                                ContactsContract.RawContacts.CONTACT_ID,
                                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                                ContactsContract.RawContacts.ACCOUNT_TYPE,
                                ContactsContract.RawContacts.ACCOUNT_NAME
                        },
                        null, null, ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " ASC")) {

                    if (c != null) {
                        while (c.moveToNext()) {
                            out.beginObject();
                            out.name("_id").value(c.getLong(0));
                            out.name("contact_id").value(c.getLong(1));
                            out.name("display_name").value(c.getString(2));
                            out.name("account_type").value(c.getString(3));
                            out.name("account_name").value(c.getString(4));
                            out.endObject();
                        }
                    }
                } catch (SecurityException e) {
                    out.beginObject().name("error").value("Permission denied: " + e.getMessage()).endObject();
                }
                out.endArray();
            }
        });
    }

    /**
     * Get detailed info for a specific contact including phone numbers and emails.
     * Required extra: _id (the raw contact ID)
     */
    private static void getContact(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final long contactId = intent.getLongExtra("_id", -1);
        if (contactId == -1) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing '_id' extra"));
            return;
        }

        ResultReturner.returnData(apiReceiver, intent, new ResultReturner.ResultJsonWriter() {
            @Override
            public void writeJson(JsonWriter out) throws Exception {
                ContentResolver cr = context.getContentResolver();
                out.beginObject();

                // Raw contact info
                try (Cursor c = cr.query(
                        ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, contactId),
                        new String[]{
                                ContactsContract.RawContacts._ID,
                                ContactsContract.RawContacts.CONTACT_ID,
                                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                                ContactsContract.RawContacts.ACCOUNT_TYPE,
                                ContactsContract.RawContacts.ACCOUNT_NAME
                        }, null, null, null)) {

                    if (c != null && c.moveToFirst()) {
                        out.name("_id").value(c.getLong(0));
                        out.name("contact_id").value(c.getLong(1));
                        out.name("display_name").value(c.getString(2));
                        out.name("account_type").value(c.getString(3));
                        out.name("account_name").value(c.getString(4));
                    }
                }

                // Phone numbers
                out.name("phones");
                out.beginArray();
                try (Cursor phones = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.LABEL
                        },
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                        new String[]{String.valueOf(contactId)}, null)) {

                    if (phones != null) {
                        while (phones.moveToNext()) {
                            out.beginObject();
                            out.name("number").value(phones.getString(0));
                            out.name("type").value(phones.getInt(1));
                            out.name("label").value(phones.getString(2));
                            out.endObject();
                        }
                    }
                }
                out.endArray();

                // Emails
                out.name("emails");
                out.beginArray();
                try (Cursor emails = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                ContactsContract.CommonDataKinds.Email.TYPE
                        },
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                        new String[]{String.valueOf(contactId)}, null)) {

                    if (emails != null) {
                        while (emails.moveToNext()) {
                            out.beginObject();
                            out.name("address").value(emails.getString(0));
                            out.name("type").value(emails.getInt(1));
                            out.endObject();
                        }
                    }
                }
                out.endArray();

                out.endObject();
            }
        });
    }

    /**
     * Add a new raw contact.
     * Required extras: display_name
     * Optional extras: phone, email
     */
    private static void addContact(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final String displayName = intent.getStringExtra("display_name");
        if (displayName == null || displayName.isEmpty()) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing 'display_name' extra"));
            return;
        }

        final String phone = intent.getStringExtra("phone");
        final String email = intent.getStringExtra("email");

        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                // Raw contact
                ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build());

                // Display name
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                        .build());

                // Phone
                if (phone != null && !phone.isEmpty()) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                            .build());
                }

                // Email
                if (email != null && !email.isEmpty()) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE,
                                    ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                            .build());
                }

                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                out.println("Contact added: " + displayName);
            } catch (SecurityException e) {
                out.println("ERROR: Permission denied: " + e.getMessage());
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }

    /**
     * Delete a raw contact by ID.
     * Required extra: _id
     */
    private static void deleteContact(TermuxApiReceiver apiReceiver, final Context context, final Intent intent) {
        final long contactId = intent.getLongExtra("_id", -1);
        if (contactId == -1) {
            ResultReturner.returnData(apiReceiver, intent, out ->
                    out.println("ERROR: Missing '_id' extra"));
            return;
        }

        ResultReturner.returnData(apiReceiver, intent, out -> {
            try {
                int deleted = context.getContentResolver().delete(
                        ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, contactId),
                        null, null);
                if (deleted > 0) {
                    out.println("Contact deleted: _id=" + contactId);
                } else {
                    out.println("ERROR: No contact found with _id=" + contactId);
                }
            } catch (SecurityException e) {
                out.println("ERROR: Permission denied: " + e.getMessage());
            } catch (Exception e) {
                out.println("ERROR: " + e.getMessage());
            }
        });
    }
}
