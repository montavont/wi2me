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

package telecom.wi2meCore.model.entities;

import telecom.wi2meCore.controller.configuration.CommunityNetworks;
import telecom.wi2meCore.controller.services.communityNetworks.ICommunityNetwork;

public class User {
	public static final String COM_NET_REFERENCE = ICommunityNetwork.TABLE_NAME + "Id";
	
	public static final String TABLE_NAME = "User";
	public static final String NAME = "name";
	public static final String PASS = "password";
	
	private String name = "";
	private String password = "";
	private CommunityNetworks communityNetwork;
	
	public CommunityNetworks getCommunityNetwork() {
		return communityNetwork;
	}
	public void setCommunityNetwork(CommunityNetworks communityNetwork) {
		this.communityNetwork = communityNetwork;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public User(String name, String password, CommunityNetworks communityNetwork){
		if (name != null)
			this.name = name;
		if (password != null)
			this.password = password;
		this.communityNetwork = communityNetwork;
	}
	
	public static final User getNewUser(String name, String password, CommunityNetworks communityNetwork){
		return new User(name, password, communityNetwork);
	}
	
	public String toString(){
		return "username:" + name + "-password:" + password + "-(CN=" + communityNetwork + ")";
	}
}
