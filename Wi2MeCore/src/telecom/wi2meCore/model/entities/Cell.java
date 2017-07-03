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

import telecom.wi2meCore.controller.services.cell.CellInfo;

public class Cell {
	
	public static final String CELL_REFERENCE = Cell.TABLE_NAME + "Id";
	
	public static final String TABLE_NAME = "Cell";
	public static final String OPERATOR_NAME = "operatorName";
	public static final String OPERATOR = "operator";
	public static final String NET_TYPE = "networkType";
	public static final String PHONE_TYPE = "phoneType";
	public static final String CID = "cid";
	public static final String LAC = "lac";
	public static final String LEVEL_DBM = "leveldBm";
	public static final String CURRENT = "current";
	
	private String operatorName;
	private String operator;
	private String networkType;
	private String phoneType;
	private int cid;
	private int lac;
	private int leveldBm;
	private boolean current;
	
	public Cell(String operatorName, String operator, String networkType, String phoneType, 
				int cid, int lac, int leveldBm, boolean current){
		this.operatorName = operatorName;
		this.operator = operator;
		this.networkType = networkType;
		this.phoneType = phoneType;
		this.cid = cid;
		this.lac = lac;
		this.leveldBm = leveldBm;
		this.current = current;
	}
	
	public String getOperatorName() {
		return operatorName;
	}
	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public String getNetworkType() {
		return networkType;
	}
	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}
	public String getPhoneType() {
		return phoneType;
	}
	public void setPhoneType(String phoneType) {
		this.phoneType = phoneType;
	}
	public int getCid() {
		return cid;
	}
	public void setCid(int cid) {
		this.cid = cid;
	}
	public int getLac() {
		return lac;
	}
	public void setLac(int lac) {
		this.lac = lac;
	}
	public int getLeveldBm() {
		return leveldBm;
	}
	public void setLeveldBm(int leveldBm) {
		this.leveldBm = leveldBm;
	}
	public boolean isCurrent() {
		return current;
	}
	public void setCurrent(boolean current) {
		this.current = current;
	}
	
	public static Cell getNewCell(String operatorName, String operator, String networkType, String phoneType, 
			int cid, int lac, int leveldBm, boolean current){
		return new Cell (operatorName, operator, networkType, phoneType, cid, lac, leveldBm, current);
	}
	
	/**
	 * Returns a Cell object out of a CellInfo object. This method will be used to convert the info obtained from scanning to a Cell object
	 * @param info The info of the cell we want to use to fill the Cell object
	 * @return The Cell object with the information of the cell info passed (with the current attribute in true)
	 */
	public static Cell getNewCellFromCellInfo(CellInfo info){
		return new Cell (info.operatorName, info.operator, info.networkType, info.phoneType, info.cid, info.lac, info.getLeveldBm(), true);
	}
	
	private static final String ATRIBUTE_SEPARATOR = ",";
	
	public String toString(){
		return operatorName + ATRIBUTE_SEPARATOR + operator + ATRIBUTE_SEPARATOR + networkType + ATRIBUTE_SEPARATOR
		        + phoneType + ATRIBUTE_SEPARATOR + cid + ATRIBUTE_SEPARATOR + lac + ATRIBUTE_SEPARATOR 
		        + leveldBm + ATRIBUTE_SEPARATOR + current;
	}

}
