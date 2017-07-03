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

package telecom.wi2meRecherche.model.parameters;

import android.util.Log;
import telecom.wi2meCore.model.parameters.*;

public class ParameterManager implements IParameterManager{
		
	private ParameterManager next = null;
	private Parameter type;
	private Object parameter = null;

	private ParameterManager(Parameter type){
		this.type = type;
	}

	@Override
	public synchronized Object getParameter(Parameter type) {
		if (type == this.type){ //If we can handle this parameter, we return its value
			return valueOrDefault();
		}
		/*
		if (next == null) //If we cannot handle it, and the next is null, throw an exception
			throw new InvalidParameterException(type.name());
			*/
		if (next == null) //If we cannot handle it, and there is no next handler, we create it
			next = getNewParameterManager(type);
		//We let the next handle it
		return next.getParameter(type);
	}
	
	@Override
	public synchronized String buildConfigFile(){
		String ret="";
		for (Parameter conf : Parameter.values()){
			if(conf.isInConfFile()){
				ret=ret+conf+"="+getParameter((Parameter) conf)+"\n";
			}
		}
		return ret;
	}

	private Object valueOrDefault() {
		if (parameter == null){
			Object ret = ParameterDefaultValues.getDefaultValue(type);
			Log.w(this.getClass().getSimpleName(), "++ "+String.format(ParameterErrorMessages.ERROR_MESSAGE, type.name(), ret));
			return ret;
		}else{
			return parameter;
		}
	}

	@Override
	public synchronized void setParameter(Parameter type, Object parameter) {
		if (type == this.type){ //If we can handle this parameter, we keep its value
			this.parameter = parameter;
		}else{
			/*
			if (next == null) //If we cannot handle it, and the next is null, throw an exception
				throw new InvalidParameterException(type.name());
				*/
			if (next == null) //If we cannot handle it, and there is no next handler, we create it
				next = getNewParameterManager(type);
			//If it is not null let the next handle it
			next.setParameter(type, parameter);			
		}
	}
	
	public void setNext(ParameterManager next){
		if (this.next == null)//If I am the last one in the chain, set it as my next, if not, send it forward
			this.next = next;
		else
			this.next.setNext(next);
	}
	
	public static ParameterManager getNewParameterManager(Parameter type){
		return new ParameterManager(type);
	}
}


