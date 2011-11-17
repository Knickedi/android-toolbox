package de.viktorreiser.toolbox.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.ListView;
import de.viktorreiser.toolbox.os.SynchronizedSoftPool;
import de.viktorreiser.toolbox.util.L;

/**
 * Fetch favicons from Internet and cache them on SD card (<b>Beta</b>).<br>
 * <br>
 * This class allows you to fetch favicons of websites and cache them on your SD card so you can
 * load them immediately on next request. A good practice is to keep an global instance in your own
 * {@link Application} implementation.<br>
 * <br>
 * Create an instance of this class and request a favicon with {@link #getFavicon(URL)}.
 * {@link #addOnFaviconLoadListener(OnFaviconLoad)} before request to be informed when favicon was
 * loaded from Internet. Allow and forbid Internet communication with
 * {@link #setLoadEnabled(boolean)}.<br>
 * {@link FaviconHelper} can give you some support loading favicons into {@link ImageView}s. This is
 * a great way to load favicons dynamically in {@link ListView}s.<br>
 * <br>
 * Class is thread-safe but should be created on UI thread because listener will be informed on the
 * thread which created the cache object.<br>
 * <br>
 * <i>Depends on</i>: {@link L}, {@link SynchronizedSoftPool}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class FaviconCache {
	
	// PRIVATE ====================================================================================
	
	/** Value used for {@link URLConnection#setConnectTimeout(int)} of every connection. */
	private static final int CONNECT_TIMEOUT = 5000;
	
	/** Value used for {@link URLConnection#setReadTimeout(int)} of every connection. */
	private static final int READ_TIMEOUT = 10000;
	
	
	/** Folder path to cache folder. */
	private String mCacheFolder = null;
	
	/** Queue for favicon requests to load on thread. */
	private final BlockingQueue<URL> mFaviconQueue = new LinkedBlockingQueue<URL>(QUEUE_LIMIT);
	
	/** Block favicon thread if load is disabled. */
	private final ReentrantLock mLoadLock = new ReentrantLock();
	
	/** Listeners which will be called when favicon was loaded and is available now. */
	private final Map<OnFaviconLoad, OnFaviconLoad> mListeners =
			new WeakHashMap<OnFaviconLoad, OnFaviconLoad>();
	
	/** Cached favicons. */
	private final SynchronizedSoftPool<Bitmap> mCache = new SynchronizedSoftPool<Bitmap>();
	
	/** Thread which loads favicons from Internet. */
	private final FaviconThread mFaviconThread = new FaviconThread();
	
	/** Handler which allows to run code on UI thread. */
	private final Handler mUiThreadHandler = new Handler();
	
	/** Set of all available favicons on SD card. */
	private final Set<Integer> mAvailableOnSdCard = Collections
			.synchronizedSet(new TreeSet<Integer>());
	
	/** Persisted (or cached) favicon if {@link #mDefaultIconFromServer} matches. */
	private Bitmap mDefaultIcon = null;
	
	/**
	 * Left to right, top to bottom filled array with {@link Bitmap#getPixel(int, int)} values of
	 * default favicon bitmap which represents the default icon returned on Internet request when no
	 * favicon was found (it's really ugly ;-).
	 */
	private int [] mDefaultIconFromServer;
	
	/** Should default icon (if favicon not found) be persisted on SD card. */
	private boolean mPersistDefault;
	
	/** {@link #FETCH_LEVEL_WEAK}, {@link #FETCH_LEVEL_SOFT} or {@link #FETCH_LEVEL_STRONG}. */
	private FetchLevel [] mFetchLevel;
	
	/** {@link Bitmap#compress(CompressFormat, int, java.io.OutputStream)} */
	private CompressFormat mCompressFormat = null;
	
	/** {@link Bitmap#compress(CompressFormat, int, java.io.OutputStream)} */
	private int mCompressQuality = 100;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Fetch levels for facivons.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static enum FetchLevel {
		/**
		 * Favicon will be requested by {@code www.getfavicon.org} API.<br>
		 * <br>
		 * This will get a lot of favicons. And the advantage is that the returned favicons may be
		 * in better resolution than {@link #SOFT}.<br>
		 * <br>
		 * <b>Note</b>: If this fetch level is used then "defaultFaviconForCache" file is expected
		 * to be located in root directory of application assets.
		 */
		WEAK,
		
		/**
		 * An {@code URL/favicon.ico} request will be sent.<br>
		 * <br>
		 * This is the standard way to get a favicon.
		 */
		SOFT,
		
		/**
		 * HTML code from URL will be parsed and extract the favicon information.<br>
		 * <br>
		 * This is really intense. HTML will be fetched and parsed and a second request will fetch
		 * the favicon (if it {@code link} tag found in HTML). <br>
		 * Parser of HTML is optimized for {@code link} tag search in {@code head} section.
		 * {@code script} and {@code style} sections are skipped. If {@code head} section ends
		 * parsing will be canceled. Regular expressions won't be used, it's straight forward
		 * character comparison!
		 */
		STRONG
	}
	
	
	/** Prefix of cache filenames. */
	protected static final String CACHE_PREFIX = "favbm_";
	
	/** Name of file of default favicon returned by Internet request if no favicon was found. */
	protected static final String DEFAULT_FAVICON_FILENAME = "defaultFaviconForCache";
	
	/** Limit for pending favicon requests. */
	protected static final int QUEUE_LIMIT = 100;
	
	/**
	 * Interface to implement by a listener which want to be informed when a favicon is loaded.
	 * 
	 * @see FaviconCache#addOnFaviconLoadListener(OnFaviconLoad)
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static interface OnFaviconLoad {
		
		/**
		 * Favicon was loaded.
		 * 
		 * @param url
		 *            URL of requested favicon
		 * @param hash
		 *            hash value of cache favicon (calling {@link FaviconCache#getCacheHash(URL)} on
		 *            {@code url} would equal this value)
		 * @param favicon
		 *            loaded favicon
		 * 
		 * @see FaviconCache#getFavicon(URL)
		 */
		public void onFaviconLoad(URL url, int hash, Bitmap favicon);
	}
	
	
	/**
	 * Helper which dynamically loads favicons to image views.<br>
	 * <br>
	 * Here is how you use it:
	 * <ul>
	 * <li>First make sure to keep track of all used favicon image views by adding them with
	 * {@link #addShownImageView(ImageView)}. In adapters you do that when
	 * {@link Adapter#getView(int, View, ViewGroup)} is called and you create a new view because you
	 * don't get a recycled view.</li>
	 * <li>If you request a favicon with {@link FaviconCache#getFavicon(URL)} and get {@code null}
	 * set {@link FaviconCache#getCacheHash(URL)} of the request URL as tag of favicon image view.
	 * It will be updated automatically when favicon is loaded. Otherwise set {@code null} as tag
	 * and image view will stay untouched.</li>
	 * </ul>
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static class FaviconHelper implements OnFaviconLoad {
		
		// PRIVATE --------------------------------------------------------------------------------
		
		/** All (recycled) image views which display a favicon. */
		private List<WeakReference<ImageView>> mShownImageViews =
				new LinkedList<WeakReference<ImageView>>();
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		/**
		 * Setup helper.
		 * 
		 * @param cache
		 *            instance of favicon cache
		 */
		public FaviconHelper(FaviconCache cache) {
			cache.addOnFaviconLoadListener(this);
		}
		
		
		/**
		 * Add a fresh created image view to list of used favicon views.
		 * 
		 * @param imageView
		 *            created image view
		 */
		public final void addShownImageView(ImageView imageView) {
			if (imageView != null) {
				mShownImageViews.add(new WeakReference<ImageView>(imageView));
			}
		}
		
		// OVERRIDDEN -----------------------------------------------------------------------------
		
		/**
		 * <i>Overridden for internal use!</i>.<br>
		 * <br>
		 * Check if a image view has to be updated because it was waiting for that icon.
		 */
		@Override
		public void onFaviconLoad(URL url, int hash, Bitmap favicon) {
			for (WeakReference<ImageView> r : mShownImageViews) {
				ImageView iv = r.get();
				
				if (iv != null && iv.getTag() != null && iv.getTag() instanceof Integer
						&& hash == (Integer) iv.getTag()) {
					iv.setImageBitmap(favicon);
					iv.setTag(null);
				}
			}
		}
	}
	
	
	/**
	 * Used to identify a favicon request.<br>
	 * <br>
	 * Different URLs could lead to the same cached icon so the returned value of this method will
	 * be equal for two URLs when they both would lead to the same cached icon.
	 * 
	 * @param url
	 *            URL to hash
	 * 
	 * @return hash of request URL or {@code null} if {@code url} is {@code null}
	 * 
	 * @see #getFavicon(URL)
	 * @see OnFaviconLoad#onFaviconLoad(URL, int, Bitmap)
	 */
	public static Integer getCacheHash(URL url) {
		return url == null ? null : url.getHost().hashCode();
	}
	
	
	/**
	 * Create favicon cache.<br>
	 * <br>
	 * <b>Recommended</b> (so you have a clue what to use)<br>
	 * {@link Context#getCacheDir()} {@code .getAbsolutePath()} as cache folder (so it will use the
	 * application cache folder).<br>
	 * Persist default icon set to {@code true}.<br>
	 * {@link FetchLevel#WEAK}, {@link FetchLevel#SOFT}, {@link FetchLevel#STRONG} to be sure to
	 * fetch the favicon.<br>
	 * {@link CompressFormat#PNG} with quality {@code 100} for best quality (but this will also
	 * consume the most cache disc space).<br>
	 * <br>
	 * <b>Cache folder path</b><br>
	 * You can choose any writable folder, e.g. {@link Environment#getExternalStorageDirectory()}
	 * {@code .getAbsolutePath()} + {@code "/MyCacheOnSDCard/"}<br>
	 * This way you can use application cache {@link Context#getCacheDir()}
	 * {@code .getAbsolutePath()}<br>
	 * <i>Note</i>: cached files have always name format {@value #CACHE_PREFIX} {@code _INTEGER}, so
	 * it shouldn't conflict with other cached files.<br>
	 * If you give {@code null} as folder path, the fetched favicons will only remain in the cache
	 * and requested again if garbage collector decides to free the favicon. Nothing is persisted.
	 * This might be good for testing.<br>
	 * <br>
	 * <b>Persist default favicon</b><br>
	 * {@code true} will persist the given default favicon in cache folder if no favicon was found.<br>
	 * {@code false} won't do that but it will put it in the memory so next request will get the
	 * cached default icon (until it is garbage collected). The intend is that it could be available
	 * later because server could be online at a later time.<br>
	 * If request doesn't fail and there's no favicon default will be persisted anyway since there
	 * is almost no chance that the given site will set an favicon in the near feature.<br>
	 * <br>
	 * <b>Default favicon</b><br>
	 * Favicon which will be used if no favicon was found (see persist description above).<br>
	 * If you use {@code null} as default icon no favicon will be put in the cache or persisted to
	 * cache folder if no favicon is found. {@link #getFavicon(URL)} will return {@code null} and
	 * next request will trigger Internet lookup again because nothing is cached. This is a very
	 * special behavior which you may want to avoid.
	 * 
	 * @param cacheFolderPath
	 *            path to cache folder
	 * @param persistDefault
	 *            should default icon be persisted to cache folder when no favicon found
	 * @param assets
	 *            see {@link FetchLevel#WEAK}
	 * @param defaultIcon
	 *            default icon which will be used if no favicon found
	 * @param fetchLevel
	 *            array of {@link FetchLevel} to proceed - at least one fetch level, no fetch level
	 *            twice and order matters
	 * @param compressFormat
	 *            format used to persist favicon with
	 *            {@link Bitmap#compress(CompressFormat, int, java.io.OutputStream)}
	 * @param compressQuality
	 *            quality used to persist favicon with
	 *            {@link Bitmap#compress(CompressFormat, int, java.io.OutputStream)}
	 * 
	 * @throws IllegalArgumentException
	 *             if assets do no contain {@value #DEFAULT_FAVICON_FILENAME} in root folder or if
	 *             {@code cacheFolderPath} is not writable
	 */
	public FaviconCache(String cacheFolderPath, boolean persistDefault, AssetManager assets,
			Bitmap defaultIcon, FetchLevel [] fetchLevel, CompressFormat compressFormat,
			int compressQuality) {
		if (assets == null || compressFormat == null) {
			throw new NullPointerException();
		}
		
		if (compressQuality < 0 || compressQuality > 100) {
			throw new IllegalArgumentException("compress quality should be 0 to 100!");
		}
		
		mCacheFolder = cacheFolderPath;
		mCompressFormat = compressFormat;
		mCompressQuality = compressQuality;
		mDefaultIcon = defaultIcon;
		mDefaultIconFromServer = new int [1024];
		mPersistDefault = persistDefault;
		mFetchLevel = fetchLevel;
		
		if (mFetchLevel.length == 0) {
			throw new IllegalArgumentException("you have to define at least one fetch level");
		}
		
		Set<FetchLevel> alreadyIn = new HashSet<FetchLevel>();
		
		for (FetchLevel fl : fetchLevel) {
			if (fl == null) {
				throw new NullPointerException("fetchLevel contains null value");
			}
			
			if (alreadyIn.contains(fl)) {
				throw new IllegalArgumentException(FetchLevel.class.getSimpleName()
						+ "." + fl + " set twice!");
			}
			
			alreadyIn.add(fl);
		}
		
		if (alreadyIn.contains(FetchLevel.WEAK)) {
			// get default favicon returned from Internet if no favicon was found
			try {
				DataInputStream dis = new DataInputStream(
						assets.open(DEFAULT_FAVICON_FILENAME));
				
				for (int y = 0; y < 32; y++) {
					int h = y * 32;
					for (int x = 0; x < 32; x++) {
						mDefaultIconFromServer[h + x] = dis.readInt();
					}
				}
				
				dis.close();
			} catch (IOException e) {
				throw new IllegalArgumentException("Expected asset file "
						+ DEFAULT_FAVICON_FILENAME + " in root directory!", e);
			}
		}
		
		// run favicon thread which will run forever
		mFaviconThread.start();
		
		if (mCacheFolder != null) {
			// setup cache folder path
			File file = new File(mCacheFolder);
			
			if (file.exists()) {
				if (!file.isDirectory()) {
					throw new IllegalArgumentException(mCacheFolder + " is a file not a directory!");
				}
			} else {
				if (file.mkdirs()) {
					throw new IllegalArgumentException(
							mCacheFolder + " is not a directory or is not writable!");
				}
			}
			
			mCacheFolder = file.getAbsolutePath() + "/";
			
			// remember all already cached favicons
			File [] cached = file.listFiles();
			
			if (cached != null) {
				for (File f : cached) {
					String name = f.getName();
					
					if (name.startsWith(CACHE_PREFIX)) {
						try {
							mAvailableOnSdCard.add(Integer.parseInt(
									name.substring(CACHE_PREFIX.length())));
						} catch (NumberFormatException e) {
							// cached icons are always an integer number
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Add a listener which will be informed when a favicon is loaded.
	 * 
	 * @param listener
	 *            listener to add
	 */
	public void addOnFaviconLoadListener(OnFaviconLoad listener) {
		synchronized (mListeners) {
			mListeners.put(listener, listener);
		}
	}
	
	/**
	 * Remove listener to stop getting load information.
	 * 
	 * @param listener
	 *            listener to remove
	 */
	public void removeOnFaviconLoadListener(OnFaviconLoad listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}
	
	/**
	 * Get cached favicon.<br>
	 * <br>
	 * If icon is available in cache or SD card you will get the icon otherwise {@code null} will be
	 * returned.<br>
	 * The icon will be loaded from Internet if {@link #isLoadEnabled()}. When it was loaded all
	 * listeners added with {@link #addOnFaviconLoadListener(OnFaviconLoad)} will be informed.<br>
	 * <br>
	 * Favicon request queue for Internet lookup will take up to {@value #QUEUE_LIMIT} pending
	 * requests. Following requests will be simply ignored (especially when {@link #isLoadEnabled()}
	 * is {@code false}). This <b>doesn't</b> mean that you can't get already cached icons!
	 * 
	 * @param url
	 *            URL to site (see {@link #getCacheHash(URL)})
	 * 
	 * @return cached icon or {@code null} if not available in cache or when {@code url} is
	 *         {@code null}
	 */
	public Bitmap getFavicon(URL url) {
		if (url == null) {
			return null;
		}
		
		int hash = getCacheHash(url);
		Bitmap icon = mCache.get(hash);
		
		// icon is cached so return it
		if (icon != null) {
			return icon;
		}
		
		// try to load icon cache from SD card
		if (mAvailableOnSdCard.contains(hash) && mCacheFolder != null) {
			icon = BitmapFactory.decodeFile(mCacheFolder + CACHE_PREFIX + hash);
			mCache.put(hash, icon);
			return icon;
		}
		
		// request favicon from Internet
		mFaviconQueue.offer(url);
		
		return null;
	}
	
	/**
	 * Prevents loading of favicons from Internet.<br>
	 * <br>
	 * Use this to disable load if no Internet connection is available or request will be
	 * unnecessarily send and fail.
	 * 
	 * @param enabled
	 *            {@code true} to enable {@code false} to disable
	 * 
	 * @see #getFavicon(URL)
	 * @see #isLoadEnabled()
	 */
	public void setLoadEnabled(boolean enabled) {
		if (enabled && mLoadLock.isLocked()) {
			mLoadLock.unlock();
		} else if (!enabled && !mLoadLock.isLocked()) {
			mLoadLock.lock();
		}
	}
	
	/**
	 * Is loading from Internet permitted?
	 * 
	 * @return {@code true} if loading is enabled
	 * 
	 * @see #getFavicon(URL)
	 * @see #setLoadEnabled(boolean)
	 */
	public boolean isLoadEnabled() {
		return !mLoadLock.isLocked();
	}
	
	/**
	 * Delete all cached files from SD.
	 */
	public void clearCache() {
		if (mCacheFolder == null) {
			return;
		}
		
		File [] files = new File(mCacheFolder).listFiles();
		
		for (File file : files) {
			String name = file.getName();
			
			if (!name.startsWith(CACHE_PREFIX)) {
				continue;
			}
			
			name = name.substring(CACHE_PREFIX.length());
			
			try {
				mAvailableOnSdCard.remove(Integer.parseInt(name));
				file.delete();
			} catch (NumberFormatException e) {
				// we didn't cache that file
			}
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	protected void finalize() {
		mFaviconThread.mmShutdown = true;
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * Thread which loads favicons from Internet.<br>
	 * <br>
	 * Thread should be started once and it will stay idle as long
	 * {@link FaviconCache#mFaviconQueue} dosen't contain any favicon load requests.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private class FaviconThread extends Thread {
		
		// private final Pattern LINK_PATTERN = Pattern.compile(
		// "<(link|LINK)[^>]*?(rel|REL)=\"(shortcut |SHORTCUT )?(icon|ICON)?\"[^>]*?/?>");
		private volatile boolean mmShutdown = false;
		
		@Override
		public void run() {
			while (!mmShutdown) {
				processRequest();
			}
		}
		
		/**
		 * Fetch favicon from Internet.
		 */
		private void processRequest() {
			// This blocks until queue can poll so thread is idle if it has nothing to do
			URL u;
			
			try {
				u = mFaviconQueue.take();
			} catch (InterruptedException e1) {
				// whatever interrupted - try again
				return;
			}
			
			// wait for lock if load is disabled
			mLoadLock.lock();
			// free lock immediately because we don't want to block we just waited for it
			mLoadLock.unlock();
			
			// setup needed data
			final URL url = u;
			final String host = url.getHost();
			final String protocol = url.getProtocol();
			final int hash = getCacheHash(url);
			
			// skip requests for same favicon if its loaded already
			if (mCache.get(hash) != null) {
				return;
			}
			
			Bitmap icon = null;
			boolean offline = false;
			Object [] result = new Object [3];
			
			for (FetchLevel fetchLevel : mFetchLevel) {
				result[0] = null;
				result[1] = result[2] = false;
				
				switch (fetchLevel) {
				case WEAK:
					runFetchLevelWeak(result, host);
					break;
				
				case SOFT:
					runFetchLevelSoft(result, host, protocol);
					break;
				
				case STRONG:
					runFetchLevelStrong(result, url);
					break;
				}
				
				icon = (Bitmap) result[0];
				offline = (Boolean) result[1];
				
				if ((Boolean) result[2]) {
					return;
				} else if (icon != null || offline) {
					break;
				}
			}
			
			if (icon == null) {
				icon = mDefaultIcon;
				
				if (icon == null) {
					// no icon - no default - skip it
					return;
				}
			}
			
			// put favicon in cache
			mCache.put(hash, icon);
			
			// persist icon to SD card
			if ((icon != mDefaultIcon || mPersistDefault || !offline) && mCacheFolder != null) {
				try {
					icon.compress(mCompressFormat, mCompressQuality,
							new FileOutputStream(mCacheFolder + CACHE_PREFIX + hash));
					mAvailableOnSdCard.add(hash);
				} catch (FileNotFoundException e) {
					// should not happen
				}
			}
			
			if (L.isD()) {
				L.d("Favicon [cached, " + ((icon != mDefaultIcon || mPersistDefault || !offline)
						&& mCacheFolder != null ? "" : "not ") + "persitsted"
						+ (icon == mDefaultIcon ? ", default" : "") + "] " + host);
			}
			
			// get thread safe listeners ...
			final OnFaviconLoad [] listener;
			
			synchronized (mListeners) {
				listener = mListeners.keySet().toArray(new OnFaviconLoad [0]);
			}
			
			final Bitmap loadedFavicon = icon;
			
			// ... and inform them on UI thread
			mUiThreadHandler.post(new Runnable() {
				@Override
				public void run() {
					for (OnFaviconLoad l : listener) {
						l.onFaviconLoad(url, hash, loadedFavicon);
					}
				}
			});
		}
		
		private void runFetchLevelWeak(Object [] result, String host) {
			// try to load favicon from icon cache API
			try {
				HttpURLConnection con = (HttpURLConnection)
						new URL("http://www.getfavicon.org/?url=" + host + "/.32.png")
								.openConnection();
				con.setConnectTimeout(CONNECT_TIMEOUT);
				con.setReadTimeout(READ_TIMEOUT);
				con.connect();
				
				if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
					result[0] = BitmapFactory.decodeStream(bis);
					bis.close();
				} else {
					result[1] = mFetchLevel[mFetchLevel.length - 1] == FetchLevel.WEAK;
				}
			} catch (MalformedURLException e) {
				// should never happen
				throw new RuntimeException(e);
			} catch (SocketTimeoutException e) {
				result[1] = mFetchLevel[mFetchLevel.length - 1] == FetchLevel.WEAK;
			} catch (IOException e) {
				// bad luck - connection failed
				result[2] = true;
				return;
			}
			
			// if a default favicon is given check retrieved favicon from equality and
			// replace icon with default if it matches
			if (result[0] != null) {
				boolean same = true;
				final Bitmap icon = (Bitmap) result[0];
				
				// check 1 / 4 of image for equality (should be enough)
				for (int y = 0; y < 32 && same; y += 2) {
					int h = y * 32;
					for (int x = 0; x < 32 && same; x += 2) {
						if (icon.getPixel(x, y) != mDefaultIconFromServer[h + x]) {
							same = false;
						}
					}
				}
				
				if (same) {
					result[0] = null;
				}
			}
		}
		
		private void runFetchLevelSoft(Object [] result, String host, String protocol) {
			try {
				HttpURLConnection con = (HttpURLConnection) new URL(
						protocol + "://" + host + "/favicon.ico").openConnection();
				con.setConnectTimeout(CONNECT_TIMEOUT);
				con.setReadTimeout(READ_TIMEOUT);
				con.connect();
				
				if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedInputStream bis = new BufferedInputStream(
							con.getInputStream());
					result[0] = BitmapFactory.decodeStream(bis);
					bis.close();
				}
			} catch (MalformedURLException e) {
				// should never happen
				throw new RuntimeException(e);
			} catch (SocketTimeoutException e) {
				result[1] = true;
			} catch (SocketException e) {
				result[1] = true;
			} catch (IOException e) {
				// bad luck - connection failed
				result[2] = true;
			}
		}
		
		private void runFetchLevelStrong(Object [] result, URL url) {
			try {
				HttpURLConnection con = (HttpURLConnection) new URL(
						url.getProtocol() + "://" + url.getHost()).openConnection();
				con.setConnectTimeout(CONNECT_TIMEOUT);
				con.setReadTimeout(READ_TIMEOUT);
				con.connect();
				
				String faviconHref = null;
				
				if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedReader br = new BufferedReader(new InputStreamReader(
							con.getInputStream()));
					faviconHref = parseLinkFromHtml(br);
					br.close();
				}
				
				if (faviconHref != null) {
					URL faviconUrl = null;
					
					try {
						faviconUrl = new URL(faviconHref);
					} catch (MalformedURLException e) {
						String path = url.getPath();
						path = path.equals("") ? "/" : path
								.substring(path.lastIndexOf('/'));
						
						faviconUrl = new URL(url.getProtocol() + "://" + url.getHost()
								+ path
								+ faviconHref);
					}
					
					con = (HttpURLConnection) faviconUrl.openConnection();
					con.connect();
					
					if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
						BufferedInputStream bis = new BufferedInputStream(
								con.getInputStream());
						result[0] = BitmapFactory.decodeStream(bis);
						bis.close();
					}
				}
			} catch (MalformedURLException e) {
				// should never happen
				throw new RuntimeException(e);
			} catch (SocketTimeoutException e) {
				result[1] = true;
			} catch (SocketException e) {
				result[1] = true;
			} catch (IOException e) {
				// bad luck - connection failed
				result[2] = true;
			}
		}
		
		/**
		 * Get {@code href} value of {@code <link rel="icon" .../>} from HTML.
		 * 
		 * @param br
		 *            input stream of HTML response
		 * 
		 * @return value of {@code href} or {@code null} when no {@code link} tag is given
		 * 
		 * @throws IOException
		 *             error during read of input stream
		 */
		private String parseLinkFromHtml(BufferedReader br) throws IOException {
			String line = null;
			boolean isStyle = false;
			boolean isScript = false;
			
			while ((line = br.readLine()) != null) {
				try {
					int i = 0;
					
					while (true) {
						while (line.charAt(i++) != '<') {
						}
						
						if (isScript) {
							if (line.startsWith("/script>", i) || line.startsWith("/SCRIPT>", i)) {
								isScript = false;
								i += 8;
							}
						} else if (isStyle) {
							if (line.startsWith("/style>", i) || line.startsWith("/STYLE>", i)) {
								isStyle = false;
								i += 7;
							}
						} else {
							if (line.startsWith("script", i) || line.startsWith("SCRIPT", i)) {
								isScript = true;
								i += 6;
							} else if (line.startsWith("style", i) || line.startsWith("STYLE", i)) {
								isStyle = true;
								i += 5;
							} else if (line.startsWith("link", i) || line.startsWith("REL", i)) {
								i += 4;
								Object [] res = getHrefIfIconLink(line, i);
								String href = (String) res[0];
								i = (Integer) res[1];
								
								if (href != null) {
									return href;
								}
							} else if (line.startsWith("/head>", i) || line.startsWith("/HEAD>", i)) {
								return null;
							}
						}
					}
				} catch (IndexOutOfBoundsException e) {
				}
			}
			
			return null;
		}
		
		/**
		 * Extract {@code href} from found {@code link} tag.<br>
		 * <br>
		 * Returned {@code href} is not {@code null} if {@code link} tag contained a favicon
		 * definition. If it is {@code null} then {@code i} contains the new cursor position for
		 * parse process.
		 * 
		 * @param line
		 *            HTML line where {@code link} tag found
		 * @param i
		 *            position of read cursor (at whitespace after {@code <link})
		 * 
		 * @return two dimensional array - first is the {@code href} tag - second is the new
		 *         position {@code i} (after closing bracket {@code <link ..>} when {@code href} not
		 *         found)
		 */
		private Object [] getHrefIfIconLink(String line, int i) {
			Object [] res = new Object [2];
			StringBuilder rel = null;
			StringBuilder href = null;
			
			while (true) {
				if (line.startsWith("rel=\"", i) || line.startsWith("REL=\"", i)) {
					i += 5;
					
					if (rel != null) {
						continue;
					}
					
					rel = new StringBuilder();
					char ct;
					
					while ((ct = line.charAt(i++)) != '"') {
						rel.append(ct);
					}
				} else if (line.startsWith("href=\"", i) || line.startsWith("HREF=\"", i)) {
					i += 6;
					
					if (href != null) {
						continue;
					}
					
					href = new StringBuilder();
					char ct;
					
					while ((ct = line.charAt(i++)) != '"') {
						href.append(ct);
					}
				}
				
				if (line.charAt(i) == '>' || rel != null && href != null) {
					res[1] = ++i;
					
					if (rel == null || href == null) {
						return res;
					}
					
					String r = rel.toString().toLowerCase();
					
					if ((r.equals("icon") || r.equals("shortcut icon")
							|| r.equals("apple-touch-icon")
							|| r.equals("apple-touch-icon-precompressed"))) {
						res[0] = href.toString();
					}
					
					return res;
				}
				
				i++;
			}
		}
	}
	
	// used to write raw bitmap of default favicon of getfavicon.org API- needed once to get it
	
	// private void writeRawToSd(Bitmap icon, int hash) {
	// if (hash == -726121024) {
	// try {
	// DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(
	// Environment.getExternalStorageDirectory().getAbsolutePath() + "/RAW")));
	//
	// for (int i = 0; i < 32; i++) {
	// for (int j = 0; j < 32; j++) {
	// dos.writeInt(icon.getPixel(j, i));
	// }
	// }
	//
	// dos.flush();
	// } catch (IOException e) {
	// }
	// }
	// }
}
