package de.viktorreiser.toolbox.os;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * Like {@link SoftPool} but thread-safe.<br>
 * <br>
 * <i>Depends on</i>: {@link SynchronizedGeneralPool}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SynchronizedSoftPool<T> extends SynchronizedGeneralPool<T> {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	protected Reference<T> getReference(T object, ReferenceQueue<T> queue) {
		return new SoftReference<T>(object, queue);
	}
	
}
