package de.viktorreiser.androidtoolbox.showcase;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Helper for a clean showcase navigation implementation.<br>
 * <br>
 * Because there are some activities which are using the same pattern to provide showcases. All you
 * need a data object which contains showcase information. This object is process by this helper.
 * 
 * <pre>
 * Object [][] showcases new Object [][] {
 *     new Object [] {
 *         TheShowcaseActivity.class,
 *         "Title of showcase",
 *         "A description what the showcase demonstrates"
 *     }
 *     [ ... further showcases ... ]
 * }
 * </pre>
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class Showcase {
	
	// expressive constants to access showcase data
	public static final int SHOWCASE_CLASS = 0;
	public static final int SHOWCASE_TITLE = 1;
	public static final int SHOWCASE_DESCRIPTION = 2;
	
	
	/**
	 * Get activity title for a certain showcase.
	 * 
	 * @param activityClass
	 *            class of activity to get the title for
	 * @param showcases
	 *            showcases data
	 * 
	 * @return {@code title} or {@code null} if given class is not a category activity
	 */
	public static String getTitle(Class<?> activityClass, Object [][] showcases) {
		for (int i = 0; i < showcases.length; i++) {
			if (activityClass.equals(showcases[i][Showcase.SHOWCASE_CLASS])) {
				return (String) showcases[i][Showcase.SHOWCASE_TITLE];
			}
		}
		
		return null;
	}
	
	/**
	 * Launch a showcase at a certain position.
	 * 
	 * @param context
	 * @param showcases
	 *            showcases data
	 * @param position
	 *            position of showcase to launch
	 */
	public static void launch(Context context, Object [][] showcases, int position) {
		context.startActivity(new Intent(context, (Class<?>) showcases[position][SHOWCASE_CLASS]));
	}
	
	/**
	 * Adapter which displays the showcases in a list.<br>
	 * <br>
	 * Use this class as item click listener to launch the showcase activities on click.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static class Adapter extends BaseAdapter implements OnItemClickListener {
		
		private Context mmContext;
		private Object [][] mmShowcases;
		
		public Adapter(Context context, Object [][] showcases) {
			mmContext = context;
			mmShowcases = showcases;
		}
		
		@Override
		public int getCount() {
			return mmShowcases.length;
		}
		
		@Override
		public Object getItem(int arg0) {
			return null;
		}
		
		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = ((LayoutInflater) mmContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(android.R.layout.simple_list_item_2, null);
			}
			
			((TextView) convertView.findViewById(android.R.id.text1)).setText(
					String.valueOf(mmShowcases[position][SHOWCASE_TITLE]));
			((TextView) convertView.findViewById(android.R.id.text2)).setText(
					String.valueOf(mmShowcases[position][SHOWCASE_DESCRIPTION]));
			
			return convertView;
		}
		
		@Override
		public void onItemClick(AdapterView<?> a, View v, int p, long id) {
			launch(mmContext, mmShowcases, p);
		}
	}
}
