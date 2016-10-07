package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.network.Transaction;
import com.mukera.sheket.client.network.TransactionResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by gamma on 3/3/16.
 */
public class STransaction extends UUIDSyncable implements Parcelable {
    private static final String LOG_TAG = STransaction.class.getSimpleName();

    public static final String JSON_TRANS_KEY_TRANS_ID = "trans_id";
    public static final String JSON_TRANS_KEY_BRANCH_ID = "branch_id";
    public static final String JSON_TRANS_KEY_DATE = "date";
    public static final String JSON_TRANS_KEY_TRANS_NOTE = "trans_note";
    public static final String JSON_TRANS_KEY_ITEMS = "items";
    public static final String JSON_TRANS_KEY_UUID = "client_uuid";

    static String _f(String s) {
        return TransactionEntry._full(s);
    }

    public static final String[] TRANSACTION_COLUMNS = {
            _f(TransactionEntry.COLUMN_COMPANY_ID),
            _f(TransactionEntry.COLUMN_TRANS_ID),
            _f(TransactionEntry.COLUMN_USER_ID),
            _f(TransactionEntry.COLUMN_BRANCH_ID),
            _f(TransactionEntry.COLUMN_DATE),
            _f(TransactionEntry.COLUMN_TRANS_NOTE),
            _f(COLUMN_CHANGE_INDICATOR),
            _f(COLUMN_UUID)
    };

    // columns of "STransaction" + "STransactionItem" + "SItem" combined!!!
    public static final String[] TRANSACTION_JOIN_ITEMS_COLUMNS;
    static {
        int trans_size = TRANSACTION_COLUMNS.length;
        int trans_items_size = STransactionItem.TRANS_ITEMS_COLUMNS.length;
        int item_size = SItem.ITEM_COLUMNS.length;

        int total_size = trans_size + trans_items_size + item_size;
        TRANSACTION_JOIN_ITEMS_COLUMNS = new String[total_size];

        System.arraycopy(TRANSACTION_COLUMNS, 0, TRANSACTION_JOIN_ITEMS_COLUMNS,
                0, trans_size);
        System.arraycopy(STransactionItem.TRANS_ITEMS_COLUMNS, 0, TRANSACTION_JOIN_ITEMS_COLUMNS,
                trans_size, trans_items_size);
        System.arraycopy(SItem.ITEM_COLUMNS, 0, TRANSACTION_JOIN_ITEMS_COLUMNS,
                trans_size + trans_items_size, item_size);
    }

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_TRANS_ID = 1;
    public static final int COL_USER_ID = 2;
    public static final int COL_BRANCH_ID = 3;
    public static final int COL_DATE = 4;
    public static final int COL_TRANS_NOTE = 5;
    public static final int COL_CHANGE = 6;
    public static final int COL_CLIENT_UUID = 7;

    // use this to retrieve next columns in a joined query
    public static final int COL_LAST = 8;

    public long company_id;
    public long transaction_id;
    public long user_id;
    public long branch_id;
    public long date;
    public String decodedDate;
    public String transactionNote;

    public List<STransactionItem> transactionItems;

    public STransaction() {
        transactionItems = new ArrayList<>();
    }

    public STransaction(TransactionResponse.SyncTransaction gRPC_Sync_Transaction, long t_company_id) {
        Transaction transaction = gRPC_Sync_Transaction.getTransaction();

        user_id = gRPC_Sync_Transaction.getUserId();

        company_id = t_company_id;
        transaction_id = transaction.getTransId();
        branch_id = transaction.getBranchId();
        date = transaction.getDateTime();
        transactionNote = transaction.getTransNote();
        client_uuid = transaction.getUUID();

        transactionItems = new ArrayList<>();

        for (Transaction.TransItem _item : transaction.getTransactionItemsList()) {
            STransactionItem transItem = new STransactionItem();
            transItem.company_id = company_id;

            // the transaction id isn't sent with the transaction items
            // but through the "outer" transaction
            transItem.trans_id = transaction_id;
            transItem.trans_type = (int)_item.getTransType();
            transItem.item_id = _item.getItemId();
            transItem.other_branch_id = _item.getOtherBranchId();
            transItem.quantity = _item.getQuantity();
            transItem.item_note = _item.getItemNote();

            transactionItems.add(transItem);
        }
    }

    public STransaction(Cursor cursor) {
        this(cursor, 0, false);
    }

    public STransaction(Cursor cursor, boolean fetch_affected) {
        this(cursor, 0, fetch_affected);
    }

    private static SimpleDateFormat sDateFormatter;
    static {
        sDateFormatter = new SimpleDateFormat("dd/MMM/yyyy");
    }

    public STransaction(Cursor cursor, int offset, boolean fetch_affected) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        transaction_id = cursor.getLong(COL_TRANS_ID + offset);
        user_id = cursor.getLong(COL_USER_ID + offset);
        branch_id = cursor.getLong(COL_BRANCH_ID + offset);
        date = cursor.getLong(COL_DATE + offset);

        Date d = SheketContract.getDateFromDb(date);
        decodedDate = sDateFormatter.format(d);
        transactionNote = cursor.getString(COL_TRANS_NOTE + offset);

        change_status = cursor.getInt(COL_CHANGE + offset);
        client_uuid = cursor.getString(COL_CLIENT_UUID + offset);

        transactionItems = new ArrayList<>();

        if (fetch_affected) {
            do {
                long curr_trans_id = cursor.getLong(COL_TRANS_ID + offset);
                if (curr_trans_id != transaction_id) {  // we've cross to the next transaction record, pull back
                    cursor.moveToPrevious();
                    break;
                }
                transactionItems.add(
                        new STransactionItem(cursor, offset + COL_LAST, true));
            } while (cursor.moveToNext());
        }
    }

    private STransaction(Parcel parcel) {
        company_id = parcel.readLong();
        transaction_id = parcel.readLong();
        user_id = parcel.readLong();
        branch_id = parcel.readLong();
        date = parcel.readLong();
        transactionNote = parcel.readString();
        change_status = parcel.readInt();
        transactionItems = parcel.readArrayList(STransactionItem.class.getClassLoader());
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_COMPANY_ID, company_id);
        values.put(TransactionEntry.COLUMN_TRANS_ID, transaction_id);
        values.put(TransactionEntry.COLUMN_USER_ID, user_id);
        values.put(TransactionEntry.COLUMN_BRANCH_ID, branch_id);
        values.put(TransactionEntry.COLUMN_DATE, date);
        values.put(TransactionEntry.COLUMN_TRANS_NOTE, transactionNote);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        values.put(COLUMN_UUID, client_uuid);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(JSON_TRANS_KEY_TRANS_ID, transaction_id);
        result.put(JSON_TRANS_KEY_BRANCH_ID, branch_id);
        result.put(JSON_TRANS_KEY_UUID, client_uuid);

        result.put(JSON_TRANS_KEY_DATE, date);
        result.put(JSON_TRANS_KEY_TRANS_NOTE,
                TextUtils.isEmpty(transactionNote) ? "" : transactionNote);

        JSONArray itemsArr = new JSONArray();
        for (STransactionItem transItem : transactionItems) {
            JSONArray json_item = new JSONArray();
            /**
             * This is the format the server expects
             *
             * [a(int), b(int), c(int), d(float), e(string)]
             * a: transaction type
             * b: item_id
             * c: other_branch_id
             * d: quantity
             * e: item note
             */
            json_item.put(transItem.trans_type);
            json_item.put(transItem.item_id);
            json_item.put(transItem.other_branch_id);
            json_item.put(transItem.quantity);
            json_item.put(TextUtils.isEmpty(transItem.item_note) ? "" :
                    transItem.item_note);

            itemsArr.put(json_item);
        }
        result.put(JSON_TRANS_KEY_ITEMS, itemsArr);
        return result;
    }

    public Transaction.Builder toGRPCBuilder() {
        Transaction.Builder builder = Transaction.newBuilder().
                setTransId(transaction_id).
                setBranchId(branch_id).
                setUUID(client_uuid).
                setDateTime(date).
                setTransNote(transactionNote);

        for (STransactionItem transItem : transactionItems) {
            builder.addTransactionItems(
                    Transaction.TransItem.newBuilder().
                            setTransType(transItem.trans_type).
                            setItemId(transItem.item_id).
                            setOtherBranchId(transItem.other_branch_id).
                            setQuantity(transItem.quantity).
                            setItemNote(transItem.item_note));
        }

        return builder;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(transaction_id);
        dest.writeLong(user_id);
        dest.writeLong(branch_id);
        dest.writeLong(date);
        dest.writeString(transactionNote);
        dest.writeInt(change_status);
        dest.writeList(transactionItems);
    }

    public static final Parcelable.Creator<STransaction> CREATOR = new
            Parcelable.Creator<STransaction>() {
                @Override
                public STransaction createFromParcel(Parcel source) {
                    return new STransaction(source);
                }

                @Override
                public STransaction[] newArray(int size) {
                    return new STransaction[size];
                }
            };

    public static final class STransactionItem extends ChangeTraceable implements Parcelable {
        static String _f(String s) { return TransItemEntry._full(s); }
        public static final String[] TRANS_ITEMS_COLUMNS = {
                _f(TransItemEntry.COLUMN_COMPANY_ID),
                _f(TransItemEntry.COLUMN_TRANSACTION_ID),
                _f(TransItemEntry.COLUMN_TRANSACTION_TYPE),
                _f(TransItemEntry.COLUMN_ITEM_ID),
                _f(TransItemEntry.COLUMN_OTHER_BRANCH_ID),
                _f(TransItemEntry.COLUMN_QTY),
                _f(TransItemEntry.COLUMN_ITEM_NOTE),
                _f(COLUMN_CHANGE_INDICATOR),
        };

        public static final int COL_COMPANY_ID = 0;
        public static final int COL_TRANS_ID = 1;
        public static final int COL_TRANS_TYPE = 2;
        public static final int COL_ITEM_ID = 3;
        public static final int COL_OTHER_BRANCH_ID = 4;
        public static final int COL_QTY = 5;
        public static final int COL_ITEM_NOTE = 6;
        public static final int COL_CHANGE = 7;

        public static final int COL_LAST = 8;

        public long company_id;
        public long trans_id;
        public int trans_type;
        public long item_id;
        public long other_branch_id;
        public double quantity;
        public String item_note;

        public SItem item;

        public STransactionItem() {
        }

        public STransactionItem(Cursor cursor, int offset, boolean fetch_item) {
            company_id = cursor.getLong(COL_COMPANY_ID + offset);
            trans_id = cursor.getLong(COL_TRANS_ID + offset);
            trans_type = cursor.getInt(COL_TRANS_TYPE + offset);
            item_id = cursor.getInt(COL_ITEM_ID + offset);
            other_branch_id = cursor.getLong(COL_OTHER_BRANCH_ID + offset);
            quantity = cursor.getDouble(COL_QTY + offset);
            item_note = cursor.getString(COL_ITEM_NOTE + offset);
            change_status = cursor.getInt(COL_CHANGE + offset);
            if (fetch_item) {
                item = new SItem(cursor, offset + COL_LAST);
            }
        }

        public ContentValues toContentValues() {
            ContentValues values = new ContentValues();
            values.put(TransItemEntry.COLUMN_COMPANY_ID, company_id);
            values.put(TransItemEntry.COLUMN_TRANSACTION_ID, trans_id);
            values.put(TransItemEntry.COLUMN_TRANSACTION_TYPE, trans_type);
            values.put(TransItemEntry.COLUMN_ITEM_ID, item_id);
            values.put(TransItemEntry.COLUMN_OTHER_BRANCH_ID, other_branch_id);
            values.put(TransItemEntry.COLUMN_QTY, quantity);
            values.put(TransItemEntry.COLUMN_ITEM_NOTE, item_note);
            values.put(COLUMN_CHANGE_INDICATOR, change_status);
            return values;
        }

        private STransactionItem(Parcel parcel) {
            company_id = parcel.readLong();
            trans_id = parcel.readLong();
            trans_type = parcel.readInt();
            item_id = parcel.readLong();
            other_branch_id = parcel.readLong();
            quantity = parcel.readDouble();
            item_note = parcel.readString();
            change_status = parcel.readInt();
        }

        @Override
        public int describeContents() {
            return hashCode();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(company_id);
            dest.writeLong(trans_id);
            dest.writeInt(trans_type);
            dest.writeLong(item_id);
            dest.writeLong(other_branch_id);
            dest.writeDouble(quantity);
            dest.writeString(item_note);
            dest.writeInt(change_status);
        }

        public static final Parcelable.Creator<STransactionItem> CREATOR = new
                Parcelable.Creator<STransactionItem>() {
                    @Override
                    public STransactionItem createFromParcel(Parcel source) {
                        return new STransactionItem(source);
                    }

                    @Override
                    public STransactionItem[] newArray(int size) {
                        return new STransactionItem[size];
                    }
                };
    }
}
