package eu.mcomputing.cohave.funfi.contentprovider;


import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import org.acra.ACRA;

import eu.mcomputing.cohave.funfi.contentprovider.dao.LocationModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.PlaceVoteModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiLocationModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiNetworkModel;
import eu.mcomputing.cohave.funfi.contentprovider.dao.WifiStaticModel;
import eu.mcomputing.cohave.funfi.helper.config.Config;
import eu.mcomputing.cohave.funfi.helper.log.MyLog;

/*
 The MIT License (MIT)

 Copyright (c) 2015 Maros Cavojsky (www.mpage.sk), mComputing (www.mcomputig.eu)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
public class WifiContentProvider extends ContentProvider {

    private SQLiteDatabase db;
    private static final String DATABASE_NAME = Config.Database.DATABASE_NAME;
    private static final int DATABASE_VERSION = Config.Database.DATABASE_VERSION;

    public static final String AUTHORITY = "eu.mcomputing.cohave.funfi.provider";

    public static final Uri BATCH_INSERT_URI = Uri.parse("content://" + AUTHORITY + "/batchInsert");

    //--- WIFI ---- //
    public static final String WIFI_PATH = WifiModel.Table.TABLE_NAME;
    public static final String WIFI_URL = "content://" + AUTHORITY + "/" + WIFI_PATH;
    public static final Uri WIFI_CONTENT_URI = Uri.parse(WIFI_URL);

    // 1 - 99
    private static final int WIFI_DIR_ID = 1;
    private static final int WIFI_ITEM_ID = 2;
    private static final int WIFI_DIR_LIMIT_ID = 3;
    private static final int WIFI_DIR_LIMIT_OFFSET_ID = 4;

    //--- LOCATION ---- //
    public static final String LOCATION_PATH = LocationModel.Table.TABLE_NAME;
    public static final String LOCATION_URL = "content://" + AUTHORITY + "/" + LOCATION_PATH;
    public static final Uri LOCATION_CONTENT_URI = Uri.parse(LOCATION_URL);

    // 100 - 199
    private static final int LOCATION_DIR_ID = 101;
    private static final int LOCATION_ITEM_ID = 102;
    private static final int LOCATION_DIR_LIMIT_ID = 103;
    private static final int LOCATION_DIR_LIMIT_OFFSET_ID = 104;

    //--- WIFI NETWORK ---- //
    public static final String WIFI_NETWORK_PATH = WifiNetworkModel.Table.TABLE_NAME;
    public static final String WIFI_NETWORK_URL = "content://" + AUTHORITY + "/" + WIFI_NETWORK_PATH;
    public static final Uri WIFI_NETWORK_CONTENT_URI = Uri.parse(WIFI_NETWORK_URL);

    // 200 - 299
    private static final int WIFI_NETWORK_DIR_ID = 201;
    private static final int WIFI_NETWORK_ITEM_ID = 202;
    private static final int WIFI_NETWORK_DIR_LIMIT_ID = 203;
    private static final int WIFI_NETWORK_DIR_LIMIT_OFFSET_ID = 204;

    //--- WIFI Location ---- //
    public static final String WIFI_LOCATION_PATH = WifiLocationModel.Table.TABLE_NAME;
    public static final String WIFI_LOCATION_URL = "content://" + AUTHORITY + "/" + WIFI_LOCATION_PATH;
    public static final Uri WIFI_LOCATION_CONTENT_URI = Uri.parse(WIFI_LOCATION_URL);

    // 300 - 399
    private static final int WIFI_LOCATION_DIR_ID = 301;
    private static final int WIFI_LOCATION_ITEM_ID = 302;
    private static final int WIFI_LOCATION_DIR_LIMIT_ID = 303;
    private static final int WIFI_LOCATION_DIR_LIMIT_OFFSET_ID = 304;

    //--- WIFI STATIC NETWORK ---- //
    public static final String WIFI_STATIC_PATH = WifiStaticModel.Table.TABLE_NAME;
    public static final String WIFI_STATIC_URL = "content://" + AUTHORITY + "/" + WIFI_STATIC_PATH;
    public static final Uri WIFI_STATIC_CONTENT_URI = Uri.parse(WIFI_STATIC_URL);

    // 400 - 499
    private static final int WIFI_STATIC_DIR_ID = 401;
    private static final int WIFI_STATIC_ITEM_ID = 402;
    private static final int WIFI_STATIC_DIR_LIMIT_ID = 403;
    private static final int WIFI_STATIC_DIR_LIMIT_OFFSET_ID = 404;

    //--- PLACE VOTE ---- //
    public static final String PLACE_VOTE_PATH = PlaceVoteModel.Table.TABLE_NAME;
    public static final String PLACE_VOTE_URL = "content://" + AUTHORITY + "/" + PLACE_VOTE_PATH;
    public static final Uri PLACE_VOTE_CONTENT_URI = Uri.parse(PLACE_VOTE_URL);

    // 100 - 199
    private static final int PLACE_VOTE_DIR_ID = 501;
    private static final int PLACE_VOTE_ITEM_ID = 502;
    private static final int PLACE_VOTE_DIR_LIMIT_ID = 503;
    private static final int PLACE_VOTE_DIR_LIMIT_OFFSET_ID = 504;

    // -- GENERAL-- //
    public static final Uri[] URIS = {
            WIFI_CONTENT_URI, LOCATION_CONTENT_URI, WIFI_NETWORK_CONTENT_URI, WIFI_LOCATION_CONTENT_URI, WIFI_STATIC_CONTENT_URI, PLACE_VOTE_CONTENT_URI};
    public static final String[] MASKS = {
            WifiModel.Table.TABLE_NAME, LocationModel.Table.TABLE_NAME,
            WifiNetworkModel.Table.TABLE_NAME, WifiLocationModel.Table.TABLE_NAME, WifiStaticModel.Table.TABLE_NAME, PlaceVoteModel.Table.TABLE_NAME};
    private static final int DATA_DIR = 0;
    public static final int DATA_ITEM = 1;
    private static final int DATA_DIR_LIMIT = 2;
    private static final int DATA_DIR_LIMIT_OFFSET = 4;


    public static final String LIMIT = "LIMIT";
    public static final String OFFSET = "OFFSET";

    public static final Uri getLimitUri(String url, long limit, long offset) {
        String uri = url + "/" + LIMIT + "/" + limit;
        if (offset > 0) {
            uri += "/" + OFFSET + "/" + offset;
        }
        return Uri.parse(uri);
    }

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, WIFI_PATH, WIFI_DIR_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_PATH + "/#", WIFI_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_PATH + "/" + LIMIT + "/#", WIFI_DIR_LIMIT_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_PATH + "/" + LIMIT + "/#/" + OFFSET + "/#", WIFI_DIR_LIMIT_OFFSET_ID);

        uriMatcher.addURI(AUTHORITY, LOCATION_PATH, LOCATION_DIR_ID);
        uriMatcher.addURI(AUTHORITY, LOCATION_PATH + "/#", LOCATION_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, LOCATION_PATH + "/" + LIMIT + "/#", LOCATION_DIR_LIMIT_ID);
        uriMatcher.addURI(AUTHORITY, LOCATION_PATH + "/" + LIMIT + "/#/" + OFFSET + "/#", LOCATION_DIR_LIMIT_OFFSET_ID);

        uriMatcher.addURI(AUTHORITY, WIFI_NETWORK_PATH, WIFI_NETWORK_DIR_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_NETWORK_PATH + "/#", WIFI_NETWORK_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_NETWORK_PATH + "/" + LIMIT + "/#", WIFI_NETWORK_DIR_LIMIT_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_NETWORK_PATH + "/" + LIMIT + "/#/" + OFFSET + "/#", WIFI_NETWORK_DIR_LIMIT_OFFSET_ID);

        uriMatcher.addURI(AUTHORITY, WIFI_LOCATION_PATH, WIFI_LOCATION_DIR_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_LOCATION_PATH + "/#", WIFI_LOCATION_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_LOCATION_PATH + "/" + LIMIT + "/#", WIFI_LOCATION_DIR_LIMIT_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_LOCATION_PATH + "/" + LIMIT + "/#/" + OFFSET + "/#", WIFI_LOCATION_DIR_LIMIT_OFFSET_ID);

        uriMatcher.addURI(AUTHORITY, WIFI_STATIC_PATH, WIFI_STATIC_DIR_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_STATIC_PATH + "/#", WIFI_STATIC_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_STATIC_PATH + "/" + LIMIT + "/#", WIFI_STATIC_DIR_LIMIT_ID);
        uriMatcher.addURI(AUTHORITY, WIFI_STATIC_PATH + "/" + LIMIT + "/#/" + OFFSET + "/#", WIFI_STATIC_DIR_LIMIT_OFFSET_ID);

        uriMatcher.addURI(AUTHORITY, PLACE_VOTE_PATH, PLACE_VOTE_DIR_ID);
        uriMatcher.addURI(AUTHORITY, PLACE_VOTE_PATH + "/#", PLACE_VOTE_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, PLACE_VOTE_PATH + "/" + LIMIT + "/#", PLACE_VOTE_DIR_LIMIT_ID);
        uriMatcher.addURI(AUTHORITY, PLACE_VOTE_PATH + "/" + LIMIT + "/#/" + OFFSET + "/#", PLACE_VOTE_DIR_LIMIT_OFFSET_ID);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(WifiModel.Table.CREATE_SQL);
            sqLiteDatabase.execSQL(LocationModel.Table.CREATE_SQL);
            sqLiteDatabase.execSQL(WifiNetworkModel.Table.CREATE_SQL);
            sqLiteDatabase.execSQL(WifiLocationModel.Table.CREATE_SQL);
            sqLiteDatabase.execSQL(WifiStaticModel.Table.CREATE_SQL);
            sqLiteDatabase.execSQL(PlaceVoteModel.Table.CREATE_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            MyLog.log(getClass(), "DB upgraded");

            sqLiteDatabase.execSQL("drop table if exists " + WifiModel.Table.TABLE_NAME);
            sqLiteDatabase.execSQL("drop table if exists " + LocationModel.Table.TABLE_NAME);
            sqLiteDatabase.execSQL("drop table if exists " + WifiNetworkModel.Table.TABLE_NAME);
            sqLiteDatabase.execSQL("drop table if exists " + WifiLocationModel.Table.TABLE_NAME);
            sqLiteDatabase.execSQL("drop table if exists " + WifiStaticModel.Table.TABLE_NAME);
            sqLiteDatabase.execSQL("drop table if exists " + PlaceVoteModel.Table.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();

        return db == null ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriMask = uriMatcher.match(uri) / 100;

        queryBuilder.setTables(MASKS[uriMask]); //set right table

        Cursor c;
        String limit = null;
        switch (uriMatcher.match(uri)) {
            case WIFI_DIR_LIMIT_OFFSET_ID:
            case LOCATION_DIR_LIMIT_OFFSET_ID:
            case WIFI_NETWORK_DIR_LIMIT_OFFSET_ID:
            case WIFI_LOCATION_DIR_LIMIT_OFFSET_ID:
            case WIFI_STATIC_DIR_LIMIT_OFFSET_ID:
            case PLACE_VOTE_DIR_LIMIT_OFFSET_ID:
                limit = uri.getPathSegments().get(DATA_DIR_LIMIT_OFFSET);

            case WIFI_DIR_LIMIT_ID:
            case LOCATION_DIR_LIMIT_ID:
            case WIFI_NETWORK_DIR_LIMIT_ID:
            case WIFI_LOCATION_DIR_LIMIT_ID:
            case WIFI_STATIC_DIR_LIMIT_ID:
            case PLACE_VOTE_DIR_LIMIT_ID:
                limit = limit == null ? "0," + uri.getPathSegments().get(DATA_DIR_LIMIT) : limit + "," + uri.getPathSegments().get(DATA_DIR_LIMIT);

            case WIFI_DIR_ID:
            case LOCATION_DIR_ID:
            case WIFI_NETWORK_DIR_ID:
            case WIFI_LOCATION_DIR_ID:
            case WIFI_STATIC_DIR_ID:
            case PLACE_VOTE_DIR_ID:
                c = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
                break;

            case WIFI_ITEM_ID:
            case LOCATION_ITEM_ID:
            case WIFI_NETWORK_ITEM_ID:
            case WIFI_LOCATION_ITEM_ID:
            case WIFI_STATIC_ITEM_ID:
            case PLACE_VOTE_ITEM_ID:
                queryBuilder.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(DATA_ITEM));
                c = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public String getType(Uri uri) {
        int uriMask = uriMatcher.match(uri) / 100;

        switch (uriMatcher.match(uri)) {
            case WIFI_DIR_ID:
            case LOCATION_DIR_ID:
            case WIFI_NETWORK_DIR_ID:
            case WIFI_LOCATION_DIR_ID:
            case WIFI_STATIC_DIR_ID:
            case PLACE_VOTE_DIR_ID:
                return "vnd.android.cursor.dir/vnd.funfi." + MASKS[uriMask];
            case WIFI_ITEM_ID:
            case LOCATION_ITEM_ID:
            case WIFI_NETWORK_ITEM_ID:
            case WIFI_LOCATION_ITEM_ID:
            case WIFI_STATIC_ITEM_ID:
            case PLACE_VOTE_ITEM_ID:
                return "vnd.android.cursor.item/vnd.funfi." + MASKS[uriMask];
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int uriMask = uriMatcher.match(uri) / 100;

        long rowID = db.insertWithOnConflict(MASKS[uriMask], null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);

        Uri id_uri = null;

        if (rowID > 0) {
            id_uri = ContentUris.withAppendedId(URIS[uriMask], rowID);
            getContext().getContentResolver().notifyChange(id_uri, null,false);
        }

        return id_uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        int uriMask = uriMatcher.match(uri) / 100;

        switch (uriMatcher.match(uri)) {
            case WIFI_DIR_ID:
            case LOCATION_DIR_ID:
            case WIFI_NETWORK_DIR_ID:
            case WIFI_LOCATION_DIR_ID:
            case WIFI_STATIC_DIR_ID:
            case PLACE_VOTE_DIR_ID:
                count = db.delete(MASKS[uriMask], selection, selectionArgs);
                break;
            case WIFI_ITEM_ID:
            case LOCATION_ITEM_ID:
            case WIFI_NETWORK_ITEM_ID:
            case WIFI_LOCATION_ITEM_ID:
            case WIFI_STATIC_ITEM_ID:
            case PLACE_VOTE_ITEM_ID:
                long wid = Long.valueOf(uri.getPathSegments().get(DATA_ITEM));

                count = db.delete(MASKS[uriMask], BaseColumns._ID + " = " + wid
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "")
                        , selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int count = 0;

        int uriMask = uriMatcher.match(uri) / 100;

        switch (uriMatcher.match(uri)) {
            case WIFI_DIR_ID:
            case LOCATION_DIR_ID:
            case WIFI_NETWORK_DIR_ID:
            case WIFI_LOCATION_DIR_ID:
            case WIFI_STATIC_DIR_ID:
            case PLACE_VOTE_DIR_ID:
                count = db.update(MASKS[uriMask], contentValues, selection, selectionArgs);
                break;
            case WIFI_ITEM_ID:
            case LOCATION_ITEM_ID:
            case WIFI_NETWORK_ITEM_ID:
            case WIFI_LOCATION_ITEM_ID:
            case WIFI_STATIC_ITEM_ID:
            case PLACE_VOTE_ITEM_ID:
                long wid = Long.valueOf(uri.getPathSegments().get(DATA_ITEM));

                count = db.update(MASKS[uriMask], contentValues, BaseColumns._ID + " = " + wid
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "")
                        , selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int uriMask = uriMatcher.match(uri) / 100;
        String table = MASKS[uriMask];
        int numInserted = 0;

        db.beginTransaction();
        try {
            for ( ContentValues cv : values ) {
                long rowID = db.insertWithOnConflict(table, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                if ( rowID > 0 ) {
                    getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(BATCH_INSERT_URI,rowID), null, false);
                }
            }
            db.setTransactionSuccessful();
            getContext().getContentResolver().notifyChange(uri, null, false);
            numInserted = values.length;
        }catch ( Exception e ){
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        } finally {
            db.endTransaction();
        }

        return numInserted;
    }

}
