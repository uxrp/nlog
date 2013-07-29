package com.example.nlogdemo;

import com.baidu.nlog.NLog;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

public class MainActivity extends Activity {
	void logContentView(View parent, String indent) {
	    Log.i("NLOG", indent + parent.getClass().getName());
	    if (parent instanceof ViewGroup) {
	        ViewGroup group = (ViewGroup)parent;
	        for (int i = 0; i < group.getChildCount(); i++)
	            logContentView(group.getChildAt(i), indent + " ");
	    }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		NLog.follow(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		NLog.follow(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);

		ScrollView scrollView = new ScrollView(this);
		scrollView.addView(linearLayout);

		RelativeLayout relativeLayout = (RelativeLayout)findViewById(R.id.box1);
		relativeLayout.addView(scrollView);
		
		@SuppressWarnings("deprecation")
		LayoutParams layoutParams = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(20, 20, 0, 0);
		
		for (Integer i = 0; i < 20; i++) {
			Button button = new Button(this);
			Integer id = 2000 + i;
			button.setId(id);
			button.setText("#" + id);
			
			button.setLayoutParams(layoutParams);
			OnClickListener buttonClickListener = new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Button self = (Button)arg0;
					NLog.cmd("pv.send", "event",
							"act=", "click",
							"target=", self.getId(),
							"text=", self.getText());
					
				}
			}; 
			button.setOnClickListener(buttonClickListener);
			linearLayout.addView(button);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
