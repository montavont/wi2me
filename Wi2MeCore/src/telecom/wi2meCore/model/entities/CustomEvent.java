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

import telecom.wi2meCore.model.entities.Trace.TraceType;


public class CustomEvent extends Trace{

	public static final String TABLE_NAME = "CustomEvent";
	public static final String EVENT = "event";

	private String event;

	protected CustomEvent(Trace trace, String event){
		Trace.copy(trace, this);
		this.event = event;
	}

	public String getEvent() {
		return event;
	}

	public static CustomEvent getNewCustomEvent(Trace trace, String event){
		return new CustomEvent(trace, event);
	}

	public String toString(){
		return super.toString() + "CUSTOM_EVENT:" + event;
	}

	@Override
	public TraceType getStoringType() {
		return TraceType.CUSTOM_EVENT;
	}
}
