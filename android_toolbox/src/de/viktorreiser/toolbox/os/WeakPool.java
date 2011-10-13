package de.viktorreiser.toolbox.os;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import android.os.AsyncTask;

/**
 * Pool for weak referenced objects.<br>
 * <br>
 * It's a {@link GeneralPool} with a {@link WeakReference} implementation.<br>
 * Pooled objects will last as long they are strong referenced otherwise garbage collector will free
 * them.<br>
 * <br>
 * This pool is special. It's not made for caching (see {@link SoftPool} instead).<br>
 * This is good for reusing immutable objects which are still in use outside the pool.<br>
 * It could also be used to lookup objects which are sill out there but can be deleted any time
 * (e.g. a {@link AsyncTask}).<br>
 * <br>
 * <i>Depends on</i>: {@link GeneralPool}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class WeakPool<T> extends GeneralPool<T> {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	protected Reference<T> getReference(T object, ReferenceQueue<T> queue) {
		return new WeakReference<T>(object, queue);
	}
	
}
