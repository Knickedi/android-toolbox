package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.bugsense.trace.BugSenseHandler;

/**
 * Showcase for android toolbox features.<br>
 * <br>
 * This activity will list the main categories of the demonstrations. So it will start other
 * activities in this package which will list demonstrations for a certain category.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class AndroidToolboxShowcaseActivity extends Activity {
	
	// PRIVATE ====================================================================================
	
	private static final Object [][] mShowcases = new Object [] [] {
			new Object [] {
					null,
					"Information",
					"Some general information about this app"
			},
			new Object [] {
					SwipeableShowcaseActivity.class,
					"Swipeable (list)",
					"Demonstrates how to use a swipeable (list) item and it's implementations"
			},
			new Object [] {
					DrawableShowcaseActivity.class,
					"Custom drawables",
					"Demonstrates the features of custom drawable implementations"
			},
			new Object [] {
					WidgetShowcaseActivity.class,
					"Custom widgets",
					"Demonstrates custom widgets and how to use them"
			}
	};
	
	// PUBLIC =====================================================================================
	
	public static String getShowcaseTitle(Class<?> activityClass) {
		return Showcase.getTitle(activityClass, mShowcases);
	}
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		BugSenseHandler.setup(this, "00ef9ee6");
		
		Showcase.Adapter adapter = new Showcase.Adapter(this, mShowcases) {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int p, long id) {
				if (p == 0) {
					showInfo();
				} else {
					super.onItemClick(a, v, p, id);
				}
			}
		};
		
		ListView list = new ListView(this);
		list.setOnItemClickListener(adapter);
		list.setAdapter(adapter);
		
		setContentView(list);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		showInfo();
		
		return false;
	}
	
	// PRIVATE ====================================================================================
	
	private void showInfo() {
		String info = "<h1>Android Toolbox</h1>"
				+ "First of all: This application is made for developers. You won't find "
				+ "something useful here as a ordinary user.<br>"
				+ "<br>"
				+ "It's a <a href='https://github.com/Knickedi/android-toolbox'>project hosted "
				+ "on GitHub</a> which provides some useful tools for android developers.<br>"
				+ "<br>"
				+ "<a href='https://github.com/Knickedi/android-toolbox/tree/master/showcase'>"
				+ "This showcase application</a> is demonstrating how to use those tools by "
				+ "providing a working source and it's visual results. Please report bugs if the "
				+ "provided examples don't work right way on your device.<br>"
				+ "<br>"
				+ "The showcase app will be extended when there's something new to present or on "
				+ "request when there's somthing unclear about a certain tool or feature.<br>"
				+ "<br>"
				+ "<b>Press the menu button in the showcase activities for some additional information.</b>";
		InformationActivity.showInfo(this, info);
	}
}