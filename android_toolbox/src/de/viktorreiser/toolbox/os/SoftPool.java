package de.viktorreiser.toolbox.os;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * Pool for soft referenced objects.<br>
 * <br>
 * It's a {@link GeneralPool} with a {@link SoftReference} implementation.<br>
 * Pooled objects will last until garbage collector reaches critical memory level and decides to
 * free objects which are not strong referenced.<br>
 * <br>
 * This pool is great for caching data of any type.<br>
 * <br>
 * <i>Depends on</i>: {@link GeneralPool}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SoftPool<T> extends GeneralPool<T> {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	protected Reference<T> getReference(T object, ReferenceQueue<T> queue) {
		return new SoftReference<T>(object, queue);
	}
	
}
