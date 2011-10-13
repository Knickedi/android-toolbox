package de.viktorreiser.toolbox.os;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;

/**
 * Put objects in a pool and access the in a shared way.<br>
 * <br>
 * {@link #put(int, Object)} an object in the pool and {@link #get(int)} it later.<br>
 * Pooled object will be available until {@link #put(int, Object)} is called with same key or the
 * garbage collector decided to free the pooled object.<br>
 * You can {@link #clear()} the pool any time.<br>
 * <br>
 * As key you could use a hash value (e.g. {@code "identifier".hashCode()}). But be careful with
 * this approach since {@code hashCode()} doesn't guarantee to be unique. An incrementing integer
 * key is the safest approach.<br>
 * <br>
 * You can use a typed pool by defining the generic type or you use {@code Object} and pool any
 * object.<br>
 * <br>
 * <b>Note</b>: This pool is general since extending class have to override
 * {@link #getReference(Object, ReferenceQueue)}.
 * 
 * @see SynchronizedGeneralPool
 * @see SoftPool
 * @see WeakPool
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
abstract class GeneralPool<T> {
	
	// PRIVATE ====================================================================================
	
	/**
	 * Value of {@link #mReqeustCount} until old (garbage collected) references will be cleared from
	 * pool.
	 */
	private static final int CLEAN_REQUEST_COUNT = 100;
	
	
	/** Count of requests to {@link #put(int, Object)} and {@link #get(int)}. */
	private int mReqeustCount = 0;
	
	/** Pooled objects. */
	private Map<Integer, Reference<T>> mObjectPool = new HashMap<Integer, Reference<T>>();
	
	/** Inverted pool which contains keys for references. */
	private Map<Reference<T>, Integer> mReferencePool = new HashMap<Reference<T>, Integer>();
	
	/** Garbage collected objects. */
	private ReferenceQueue<T> mQueue = new ReferenceQueue<T>();
	
	// ABSTRACT ===================================================================================
	
	/**
	 * Type of reference used for pool creation.<br>
	 * <br>
	 * Here you have only two choices:<br>
	 * <br>
	 * {@code return new SoftReference<T>(object, queue)}<br>
	 * Pooled objects will last until garbage collector reaches critical memory level and decides to
	 * free objects which are not strong referenced.<br>
	 * <br>
	 * {@code return new WeakReference<T>(object, queue)}<br>
	 * Pooled objects will last as long they are strong referenced otherwise garbage collector will
	 * free them.
	 * 
	 * @param object
	 *            object to create reference for
	 * @param queue
	 *            reference queue to put the reference in
	 * 
	 * @return created reference
	 */
	protected abstract Reference<T> getReference(T object, ReferenceQueue<T> queue);
	
	// PUBLIC =====================================================================================
	
	/**
	 * Put object into pool.
	 * 
	 * @param key
	 *            integer key to access pooled object with {@link #get(int)} - if key is already
	 *            available it will replace already pooled object
	 * @param object
	 *            object to pool ({@code null} will be ignored)
	 */
	public void put(int key, T object) {
		if (object == null) {
			return;
		}
		
		clean();
		
		Reference<T> cached = getReference(object, mQueue);
		Reference<T> oldCache = mObjectPool.remove(key);
		
		if (oldCache != null) {
			mReferencePool.remove(oldCache);
		}
		
		mObjectPool.put(key, cached);
		mReferencePool.put(cached, key);
	}
	
	/**
	 * Get pooled object.
	 * 
	 * @param key
	 *            key of pooled object
	 * 
	 * @return pooled object or {@code null} if there is no object with this key in pool or the
	 *         object was garbage collected
	 * 
	 * @see #put(int, Object)
	 */
	public T get(int key) {
		clean();
		
		Reference<T> ref = mObjectPool.get(key);
		
		if (ref == null) {
			return null;
		}
		
		return ref.get();
	}
	
	/**
	 * Clear pool.<br>
	 * <br>
	 * This will release all references so it will free all pooled objects (delete the references).<br>
	 * After that the pool object will be like fresh created.
	 */
	public void clear() {
		mReqeustCount = 0;
		mQueue = new ReferenceQueue<T>();
		mObjectPool.clear();
		mReferencePool.clear();
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * Clean pool from all dead references from reference queue.
	 */
	private void clean() {
		mReqeustCount++;
		
		if (mReqeustCount == CLEAN_REQUEST_COUNT) {
			Reference<?> ref = null;
			
			while ((ref = mQueue.poll()) != null) {
				mObjectPool.remove(mReferencePool.remove(ref));
			}
		}
	}
}
