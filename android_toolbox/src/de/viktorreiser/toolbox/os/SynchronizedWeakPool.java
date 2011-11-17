package de.viktorreiser.toolbox.os;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Like {@link WeakPool} but thread-safe (<b>Beta</b>).<br>
 * <br>
 * <i>Depends on</i>: {@link SynchronizedGeneralPool}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SynchronizedWeakPool<T> extends SynchronizedGeneralPool<T> {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	protected Reference<T> getReference(T object, ReferenceQueue<T> queue) {
		return new WeakReference<T>(object, queue);
	}
	
}
