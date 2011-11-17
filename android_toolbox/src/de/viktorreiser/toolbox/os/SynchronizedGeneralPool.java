package de.viktorreiser.toolbox.os;

/**
 * Like {@link GeneralPool} but thread-safe (<b>Beta</b>).<br>
 * <br>
 * <i>Depends on</i>: {@link GeneralPool}
 * 
 * @see SynchronizedSoftPool
 * @see SynchronizedWeakPool
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
abstract class SynchronizedGeneralPool<T> extends GeneralPool<T> {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public synchronized void put(int key, T object) {
		super.put(key, object);
	}
	
	@Override
	public synchronized T get(int key) {
		return super.get(key);
	}
	
	@Override
	public synchronized void clear() {
		super.clear();
	}
}
