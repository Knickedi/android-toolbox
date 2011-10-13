package de.viktorreiser.toolbox.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * Allow an enumeration value to be persisted as integer value.<br>
 * <br>
 * Persisting an enumeration value is not easy.<br>
 * Using its name is heavy and dangerous because it could be changed later. Same with its ordinal
 * value.<br>
 * So this interface allows an implementation of a persistent enumeration.<br>
 * <br>
 * <b>Example</b>:
 * <pre>
 * public enum MyEnum implements EnumPersistor&lt;MyEnum&gt; {
 * 
 * 	FIRST(1),
 * 	SECOND(39);
 * 
 * 	private static final EnumPersistor.ReverseMap&lt;MyEnum&gt; map = 
 * 			new EnumPersistor.ReverseMap&lt;MyEnum&gt;(MyEnum.class);
 * 
 * 	private final int id;
 * 
 * 	MyEnum(int id) {
 * 		this.id = id;
 * 	}
 * 
 * 	public int getPersistId() {
 * 		return id;
 *	}
 *  
 * 	public MyEnum fromPersistId(int id) {
 *		return map.get(id);
 * 	}
 * }</pre>
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public interface EnumPersistor<T extends Enum<T>> {
	
	// PUBLIC =====================================================================================
	
	public int getPersistId();
	
	
	public class ReverseMap<T extends Enum<T> & EnumPersistor<T>> {
		
		// PRIVATE --------------------------------------------------------------------------------
		
		private Map<Integer, T> mMap = new HashMap<Integer, T>();
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		public ReverseMap(Class<T> cls) {
			for (T v : cls.getEnumConstants()) {
				mMap.put(v.getPersistId(), v);
			}
		}
		
		
		public T get(int persistId) {
			return mMap.get(persistId);
		}
	}
}
