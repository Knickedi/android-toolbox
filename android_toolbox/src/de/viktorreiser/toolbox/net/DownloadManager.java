package de.viktorreiser.toolbox.net;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * <b>This class is not ready to be used!</b>
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class DownloadManager {
	
	// PRIVTE ====================================================================================
	
	private DBHelper mDbHelper;
	private SQLiteDatabase mDb;
	private List<DownloadData> mDownloadsInProgress = new ArrayList<DownloadData>();
	
	private long mDiscardDownloadByAge = 60 * 60 * 1000;
	private long mMaxSizeForMobileDownload = 1024 * 1024;
	private volatile long mUpdateRate = 500;
	private volatile int mMaxHttpDownloads = 2;
	private volatile int mCurrentHttpDownloads = 0;
	
	private volatile boolean mDestroy = false;
	
	// PUBLIC =====================================================================================
	
	public DownloadManager(Context context) {
		this(context, null);
	}
	
	public DownloadManager(Context context, String databaseName) {
		mDbHelper = new DBHelper(context, databaseName);
		mDb = mDbHelper.getWritableDatabase();
	}
	
	
	public long startDownload(DownloadRequest download) {
		String scheme = download.uri.getScheme();
		
		if (!scheme.equals("http") && !scheme.equals("https")) {
			return -1;
		}
		
		if (download.persistPath != null) {
			File downloadPath = new File(download.persistPath);
			
			if (downloadPath.isDirectory()) {
				if (downloadPath.canWrite()) {
					String name = download.uri.getLastPathSegment();
						download.persistPath = downloadPath.getAbsolutePath()
								+ (name == null || name.trim().equals("")
								? download.uri.getHost() : name.trim());
				} else {
					return -1;
				}
			}
			
			String currentPath = download.persistPath;
			File filePath;
			
			while ((filePath = new File(currentPath)).exists()) {
				
			}
			
			download.persistPath = currentPath;
			
			try {
				if (!filePath.createNewFile()) {
					return -1;
				}
			} catch (IOException e) {
				return -1;
			}
		}
		
		DownloadData data = new DownloadData(download);
		data.id = mDb.insert(DB.TABLE_NAME, null, DBHelper.createContentValues(data));
		
		processDownload();
		
		return data.id;
	}
	
	public void close() {
		if (!mDestroy) {
			mDestroy = true;
			mDbHelper.close();
		}
	}
	
	
	public void setMaximumHttpDownloads(int maxDownloads) {
		if (maxDownloads >= 0) {
			mMaxHttpDownloads = maxDownloads;
		}
	}
	
	public void setMaxSizeForMobileDownload(int maxSize) {
		if (maxSize >= 0) {
			mMaxSizeForMobileDownload = maxSize;
		}
	}
	
	public void setDiscardAge(int age) {
		if (age > 0) {
			mDiscardDownloadByAge = age * 1000;
		}
	}
	
	public void setUpdateRate(long rate) {
		if (rate >= 50) {
			mUpdateRate = rate;
		}
	}
	
	
	public static class DownloadStatus {
		
		public static final int SUCCEED = 1;
		public static final int PENDING = 2;
		public static final int FAILED = 3;
		public static final int CANCELED = 4;
		public static final int DOWNLOADING = 5;
		public static final int DISCARDED = 6;
		public static final int STOPPED = 7;
		
		
		public static boolean isFinished(int status) {
			return status == SUCCEED || status == FAILED || status == CANCELED;
		}
	}
	
	public static class DownloadPrioriy {
		public static final int LOW = 1;
		public static final int NORMAL = 2;
		public static final int HIGH = 3;
	}
	
	public static class DownloadRequest {
		
		public Uri uri = null;
		public String persistPath = null;
		public int priority = DownloadPrioriy.NORMAL;
		public boolean deleteAfterPersist = true;
		public boolean bypassMobileSizeRestriction = false;
		public int networkChangeAttempts = 5;
		public boolean restrictToWifi;
		
		
		public DownloadRequest(Uri uri) {
			this.uri = uri;
		}
		
		
		public DownloadRequest setPersistPath(String persistPath) {
			this.persistPath = persistPath;
			return this;
		}
		
		public DownloadRequest setDeleteAfterPersist(boolean delete) {
			deleteAfterPersist = delete;
			return this;
		}
		
		public DownloadRequest setBypassMobileSizeRestriction(boolean bypass) {
			bypassMobileSizeRestriction = bypass;
			return this;
		}
		
		public DownloadRequest setNetworkChangeAttempts(int attempts) {
			networkChangeAttempts = Math.max(attempts, 0);
			return this;
		}
		
		public DownloadRequest setPriority(int priority) {
			if (priority == DownloadPrioriy.NORMAL || priority == DownloadPrioriy.HIGH
					|| priority == DownloadPrioriy.LOW) {
				this.priority = priority;
			}
			
			return this;
		}
		
		public DownloadRequest setRestrictToWifi(boolean restrict) {
			restrictToWifi = restrict;
			return this;
		}
	}
	
	public static class DB {
		
		private static final String TABLE_NAME = "downloads";
		
		public static final String ID = "_id";
		public static final String URI = "uri";
		public static final String SIZE = "size";
		public static final String MIME = "mime";
		public static final String STATUS = "status";
		public static final String RATE = "rate";
		public static final String PRIORITY = "priority";
		public static final String BYPASS_MOBILE_RESTRICTION = "bypass";
		public static final String PERSIT_PATH = "persistPath";
		public static final String DELETE_AFTER_PERSIST = "persistDelete";
		public static final String ADD_TIME = "addTime";
		public static final String FINISH_TIME = "finishTime";
		public static final String DOWNLOAD_START_TIME = "downloadStartTime";
		public static final String CHANGE_ATTEMPTS = "changeAttempts";
		public static final String CHANGE_ATTEMPT_COUNT = "changeAttemptCount";
		public static final String RESTRICT_TO_WIFI = "restrictToWifi";
	}
	
	// OVERRRIDDEN ================================================================================
	
	@Override
	protected void finalize() {
		mDestroy = true;
	}
	
	// PRIVATE ====================================================================================
	
	private void processDownload() {
		// discard old pending and stopped downloads
		ContentValues v = new ContentValues();
		v.put(DB.STATUS, DownloadStatus.DISCARDED);
		mDb.update(DB.TABLE_NAME, v, DB.ADD_TIME + "<" + mDiscardDownloadByAge
				+ " AND " + DB.STATUS + " IN ("
				+ DownloadStatus.PENDING + "," + DownloadStatus.STOPPED + ")", null);
		
		// Cursor pending = mDb.query(DB.TABLE_NAME, DBHelper.ALL_COLUMNS, selection, selectionArgs,
		// groupBy, having, orderBy)
	}
	
	private static class DownloadData implements Cloneable {
		
		public long id;
		public DownloadTask task = null;
		public Uri uri = null;
		public String persistPath = null;
		public int size = 0;
		public String mime = null;
		public long addTime = 0;
		public long downloadStartTime = 0;
		public long finishTime = 0;
		public int status = DownloadStatus.PENDING;
		public int priority;
		public boolean deleteAfterPersist = true;
		public boolean bypassMobileSizeRestriction = false;
		public int downloadRate = 0;
		public int networkChangeAttempts = 0;
		public int networkChangeAttemptCount = 0;
		public boolean restrictToWifi;
		
		
		private DownloadData(DownloadRequest download) {
			uri = download.uri;
			persistPath = download.persistPath;
			deleteAfterPersist = download.deleteAfterPersist;
			bypassMobileSizeRestriction = download.bypassMobileSizeRestriction;
			networkChangeAttempts = download.networkChangeAttempts;
			priority = download.priority;
			restrictToWifi = download.restrictToWifi;
			addTime = new Date().getTime();
		}
		
		private DownloadData() {
			
		}
	}
	
	private class DownloadTask extends AsyncTask<Object, Void, Void> {
		
		private volatile boolean mmCancel = false;
		private int mmDataIndex = 0;
		private DownloadData mmDownloadData;
		
		
		@Override
		protected Void doInBackground(Object... params) {
			mmDownloadData = (DownloadData) params[0];
			
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			
		}
	}
	
	private static class DBHelper extends SQLiteOpenHelper {
		
		private static final String DB_NAME = "downloadmanager.db";
		private static final int DB_VERSION = 1;
		
		public static final String [] ALL_COLUMNS = new String [] {
				DB.ID, DB.URI, DB.SIZE, DB.MIME, DB.STATUS, DB.RATE, DB.PRIORITY,
				DB.BYPASS_MOBILE_RESTRICTION, DB.PERSIT_PATH, DB.DELETE_AFTER_PERSIST, DB.ADD_TIME,
				DB.FINISH_TIME, DB.DOWNLOAD_START_TIME, DB.CHANGE_ATTEMPTS,
				DB.CHANGE_ATTEMPT_COUNT, DB.RESTRICT_TO_WIFI
		};
		
		
		public DBHelper(Context context, String dbName) {
			super(context, dbName == null ? DB_NAME : dbName, null, DB_VERSION);
		}
		
		public static ContentValues createContentValues(DownloadData data) {
			ContentValues v = new ContentValues();
			
			v.put(DB.URI, data.uri.toString());
			v.put(DB.STATUS, data.status);
			v.put(DB.PRIORITY, data.priority);
			v.put(DB.BYPASS_MOBILE_RESTRICTION, data.bypassMobileSizeRestriction);
			v.put(DB.DELETE_AFTER_PERSIST, data.deleteAfterPersist);
			v.put(DB.PERSIT_PATH, data.persistPath);
			v.put(DB.SIZE, data.size);
			v.put(DB.MIME, data.mime);
			v.put(DB.RATE, data.downloadRate);
			v.put(DB.ADD_TIME, data.addTime);
			v.put(DB.DOWNLOAD_START_TIME, data.downloadStartTime);
			v.put(DB.FINISH_TIME, data.finishTime);
			v.put(DB.CHANGE_ATTEMPTS, data.networkChangeAttempts);
			v.put(DB.CHANGE_ATTEMPT_COUNT, data.networkChangeAttemptCount);
			v.put(DB.RESTRICT_TO_WIFI, data.restrictToWifi);
			
			return v;
		}
		
		public static DownloadData createFromCursor(Cursor c) {
			DownloadData data = new DownloadData();
			
			data.id = c.getLong(0);
			data.uri = Uri.parse(c.getString(1));
			data.size = c.getInt(2);
			data.mime = c.getString(3);
			data.status = c.getInt(4);
			data.downloadRate = c.getInt(5);
			data.priority = c.getInt(6);
			data.bypassMobileSizeRestriction = c.getInt(7) != 0;
			data.persistPath = c.getString(8);
			data.deleteAfterPersist = c.getInt(9) != 0;
			data.addTime = c.getLong(11);
			data.finishTime = c.getLong(12);
			data.downloadStartTime = c.getLong(13);
			data.networkChangeAttempts = c.getInt(14);
			data.networkChangeAttemptCount = c.getInt(15);
			data.restrictToWifi = c.getInt(16) != 0;
			
			return data;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.rawQuery("CREATE TABLE " + DB.TABLE_NAME + " (\n"
					+ DB.ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,\n"
					+ DB.URI + " TEXT NOT NULL,\n"
					+ DB.SIZE + " INTEGER NOT NULL,\n"
					+ DB.MIME + " TEXT DEFUALT NULL,\n"
					+ DB.STATUS + " INTEGER NOT NULL,\n"
					+ DB.RATE + " INTEGER NOT NULL,\n"
					+ DB.PRIORITY + " INTEGER NOT NULL,\n"
					+ DB.BYPASS_MOBILE_RESTRICTION + " INTEGER NOT NULL,\n"
					+ DB.PERSIT_PATH + " TEXT,\n"
					+ DB.DELETE_AFTER_PERSIST + " INTEGER NOT NULL,\n"
					+ DB.ADD_TIME + " INTEGER NOT NULL,\n"
					+ DB.DOWNLOAD_START_TIME + " INTEGER NOT NULL,\n"
					+ DB.FINISH_TIME + " INTEGER NOT NULL,\n"
					+ DB.CHANGE_ATTEMPTS + " INTEGER NOT NULL,\n"
					+ DB.CHANGE_ATTEMPT_COUNT + " INTEGER NOT NULL,\n"
					+ DB.RESTRICT_TO_WIFI + " INTEGER NOT NULL\n"
					+ ");", null);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}
	}
}
