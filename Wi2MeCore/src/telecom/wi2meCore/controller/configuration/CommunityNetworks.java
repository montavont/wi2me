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

package telecom.wi2meCore.controller.configuration;

/**Enumeration class containing the community networks.
 * To create a new community network, simply follow those 3 steps:
 * 1. Create a class implementing this interface, copy/paste the code from another community account and change ONLY the private parameters.
 * 2. Add the community network to the list of CommunityNetworks (telecom.wi2meCore.controller.configuration)
 * 3. Add the community network in the CommunityNetworkService (telecom.wi2meCore.controller.services.communityNetworks) simply by
 * adding it in the list of parameters and in the switch of the method getCommunityNetwork ONLY. 
 * @author Gilles Vidal
 *
 */
public enum CommunityNetworks {
	
	SALSA,
	NEUF,
	FREE,
	FON

}
