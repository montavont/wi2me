/**
 * Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
 *
 * This file is part of Wi2Me.
 *
 * Wi2Me is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wi2Me is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
 */

package telecom.wi2meUser;


import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;


/** The Network Manager screen of Wi2MeUser.
 * Includes three sub-screen : Network Preference; Personal Network Manager; Community Network Manager
 * @author Xin CHEN*/





public class Wi2MeNetworkManagerActivity extends TabActivity{
	private TabHost tabHost;
	private TabSpec tabSpec;
	private boolean tabChanged;
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.network_manager);

		
        /* Get TableHost object */
        tabHost = getTabHost();   

        
        /* Create network rating tab */
        Intent networkPreferece= new Intent(this, Wi2MeAPPreferenceActivity.class);
        tabSpec = tabHost.newTabSpec("Network Preference")
        		.setIndicator("Preference",getResources().getDrawable(R.drawable.etoile)).setContent(networkPreferece);
        tabHost.addTab(tabSpec);
        
        
        /* Create Private Network Manager tab */
        Intent privateNetworkManager = new Intent(Settings.ACTION_WIFI_SETTINGS);
        privateNetworkManager.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        tabSpec = tabHost.newTabSpec("Personal Network Manager")
        		.setIndicator("Personal Network",getResources().getDrawable(R.drawable.private_network)).setContent(privateNetworkManager);

        tabHost.addTab(tabSpec);
	
	// Starting in Android 4.0, it is no longer possible to embed an external activity. This will open the actual network manager instead.
	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	{
		getTabWidget().getChildAt(Wi2MeNetworkManagerActivity.this.getTabWidget().getTabCount()-1).setOnClickListener(new OnClickListener()
		{

		        @Override
		        public void onClick(View v) {

			Intent privateNetworkManager = new Intent(Settings.ACTION_WIFI_SETTINGS);
			privateNetworkManager.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(privateNetworkManager);
        		}
		});
	}
        
        /* Create Community Network Manager tab */
        Intent cnNetworkManager = new Intent(this, Wi2MeAccountManagerActivity.class);        
        tabSpec = tabHost.newTabSpec("Community Network Manager")
        		.setIndicator("Community Network",getResources().getDrawable(R.drawable.community_network)).setContent(cnNetworkManager);
        tabHost.addTab(tabSpec);

        for (int i=0; i < tabHost.getTabWidget().getChildCount();i++) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title); //Unselected Tabs
            if (tv != null) {
                tv.setPadding(5, 0, 5, 0);
            }
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
        	tabHost.setCurrentTab(b.getInt("ID"));
        }
        else{
        	tabHost.setCurrentTab(0);
        }
       
        
    }
	
}
