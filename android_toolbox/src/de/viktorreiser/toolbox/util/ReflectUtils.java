package de.viktorreiser.toolbox.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Static helper utilities for reflection operations.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public final class ReflectUtils {
	
	// PRIVATE ====================================================================================
	
	/** Set of primitive classes, their wrapper classes and {@code String}. */
	private static final Set<Class<?>> PRIMITIVES =
			Collections.synchronizedSet(new HashSet<Class<?>>() {
				private static final long serialVersionUID = 1L;
				{
					add(boolean.class);
					add(char.class);
					add(byte.class);
					add(short.class);
					add(int.class);
					add(long.class);
					add(float.class);
					add(double.class);
					add(String.class);
					add(Boolean.class);
					add(Character.class);
					add(Byte.class);
					add(Short.class);
					add(Integer.class);
					add(Float.class);
					add(Double.class);
				}
			});
	
	// PUBLIC =====================================================================================
	
	/**
	 * Print all object fields which {@link #isPrimitive(Class)} and not final.<br>
	 * <br>
	 * Following field names will be corrected in output:<br>
	 * E.g. {@code mMyVar} and {@code mmMyVar} become {@code myVar}
	 * 
	 * @param obj
	 *            object to print
	 * 
	 * @return String with format {@code "field1=value field2=value"}
	 */
	public static String printPrimitiveFields(Object obj) {
		StringBuilder s = new StringBuilder();
		
		Field [] fields = obj.getClass().getFields();
		
		for (int i = 0; i < fields.length; i++) {
			try {
				Field field = fields[i];
				
				if (isPrimitive(field.getType())) {
					field.setAccessible(true);
					
					if ((field.getModifiers() & Modifier.FINAL) != 0) {
						continue;
					}
					
					Object value = field.get(obj);
					String name = field.getName();
					
					try {
						// check for special condition: mVar or mmVar
						if (name.charAt(0) == 'm' && Character.isUpperCase(name.charAt(1))) {
							s.append(Character.toLowerCase(name.charAt(1)));
							s.append(name.substring(2));
						} else if (name.charAt(0) == 'm' && name.charAt(1) == 'm'
								&& Character.isUpperCase(name.charAt(2))) {
							s.append(Character.toLowerCase(name.charAt(2)));
							s.append(name.substring(3));
						} else {
							s.append(name);
						}
					} catch (IndexOutOfBoundsException e) {
						// field name had only one ore two characters
						s.append(name);
					}
					
					s.append("=");
					s.append(String.valueOf(value));
					s.append(" ");
				}
			} catch (IllegalArgumentException e) {
				// field is from objects class so should never happen
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				// no access to field then skip it
			}
		}
		
		// delete trailing white space
		int length = s.length();
		
		if (length != 0) {
			s.replace(length - 1, length, "");
		}
		
		return s.toString();
	}
	
	/**
	 * Is class representing a primitive type.<br>
	 * <br>
	 * Primitives are {@code boolean}, {@code byte}, {@code short}, {@code int}, {@code long},
	 * {@code float}, {@code double}, their wrapper classes, {@code String} or an instance of
	 * {@code enum}.
	 * 
	 * @param cls
	 *            class to check
	 * 
	 * @return {@code true} if class represents a primitive type
	 */
	public static boolean isPrimitive(Class<?> cls) {
		return PRIMITIVES.contains(cls) || cls.isEnum();
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * No constructor for a static class.
	 */
	private ReflectUtils() {
		
	}
}
