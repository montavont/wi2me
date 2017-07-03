# Copyright (c) 2012 Institut Mines-Telecom / Telecom Bretagne. All rights reserved.
#
# This file is part of Wi2Me.
#
# Wi2Me is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Wi2Me is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Wi2Me.  If not, see <http://www.gnu.org/licenses/>.
#
		
from math import radians, degrees, cos, sin, asin, sqrt

from wi2me.utils import MapHelper
from wi2me.utils import SVGHelper

from settings import TEMP_DIR
from wi2me.model.ConnectionEvent import TYPE_CONNECTION_START, TYPE_ASSOCIATED, TYPE_ASSOCIATING, TYPE_DHCP, TYPE_CONNECTED, TYPE_PASSED_PORTAL, TYPE_DISCONNECTED

METRIC_NAME = "Wifi Trajectory, Event and Dataflow display on map"
METRIC_CODE = "2.4.1"

DEFAULT_CONFIG = {'GRANULARITY':200,
		'AP_MIN_LEVEL':-101, # Must be at least -100
		'PLOT_AP':False,
		'PLOT_ALL_AP_POINTS':False,
		'PLOT_AP_CIRCLE':False,
		'ONLY_USED_APS':False,
		'PLOT_EVENTS':True,
		'PLOT_ASSOCIATION_BYTES':False, # Plot the last two bytes of the bssid we are associating to
		'PLOT_BYTES':False,
		'TRAJECTORY_COLOR':"red",
		'BSSID_FILTER':[], 
		'PLACEMENT_MIN_POINTS':3,
		'PLOT_TRAJECTORY':True,
		'LINE_SIZE':4,
		'DATA_LINE_SIZE':8,
		'COLOR_ASSOCIATING' :  "orange",
		'COLOR_DHCP' :  "yellow",
		'COLOR_PORTAL' : "blue",
		'COLOR_CONNECTED' : "green",
		'SCALE':True,
		'SCALE_METERS':100,
		'SCALE_CAPTION_UNIT':"m",
		'SCALE_COLOR':"black",
		'SCALE_POS_X':0.05,
		'SCALE_POS_Y':0.05,
		}

SEARCHING_START = 1
SEARCHING_ASSOCIATED = 2
SEARCHING_DHCP = 3
SEARCHING_PASSED_PORTAL = 4
SEARCHING_CONNECTED = 5
SEARCHING_DISCONNECTED = 6


def plot(config):


	tracePoints = []
	
	mapHelper = MapHelper.MapHelper(config['data'])
	
	box_lat_max, box_lat_min, box_long_max, box_long_min = mapHelper.getMapBox()
	image_width, image_height = mapHelper.getDimensions()
	if image_width * image_height > 0:

		Association_Points = [] #Between CONNECTION_START and OBTAINING_IPADDR
		Association_Decisions = []
		DHCP_Points = [] #Between OBTAINING_IPADDR and CONNECTED
		Portal_Points = [] #Between CONNECTED and CN's CONNECTED
		Connection_Points = [] #Between CONNECTED and DISCONNECTED
		
		DataPoints = [] #Positions where data transfer was done
		UsedAPs = []

		GPSPoints = []


		APBlobs = {}
		APCenters = {}

		for source in config['data']:

			#Connection Data Event
			if config['PLOT_EVENTS']:

				CaptivePortalPassings = source.getCNEvents([TYPE_PASSED_PORTAL])
				ConnectionStarts = source.getEvents([TYPE_CONNECTION_START, TYPE_ASSOCIATING, TYPE_ASSOCIATED, TYPE_DHCP, TYPE_CONNECTED, TYPE_DISCONNECTED])
				Events = sorted(ConnectionStarts + CaptivePortalPassings, key = lambda x : x.timestamp)
		
				if len(Events) > 0:
					state = SEARCHING_START
					lastTs = Events[0].timestamp

					for event in Events:
						if event.state == TYPE_ASSOCIATING:
							as_pts = source.getPoints([event.timestamp, event.timestamp])
							if len(as_pts) > 0:
								as_lat, as_lon = as_pts[0]
								as_rssi = [det.rssi for det in event.network.detections if det.timestamp == event.timestamp][0]
								as_text = event.network.bssid[12:]
								Association_Decisions.append([as_rssi, as_text, as_lat, as_lon])

						if event.state == TYPE_CONNECTION_START:
							lastTs = event.timestamp
							state = SEARCHING_ASSOCIATED
							UsedAPs.append(event.network)
						elif event.state == TYPE_ASSOCIATED:
							Association_Points.append(source.getPoints([lastTs, event.timestamp]))
							lastTs = event.timestamp
							state = SEARCHING_DHCP
						elif event.state == TYPE_DHCP:
							lastDHCPTs = event.timestamp
							state = SEARCHING_CONNECTED
						elif event.state == TYPE_CONNECTED:
							if lastDHCPTs > 0: #DHCP will not always happen
								DHCP_Points.append(source.getPoints([lastDHCPTs, event.timestamp]))
								lastDHCPTs = 0
							lastTs = event.timestamp
							state = SEARCHING_PASSED_PORTAL
						elif event.state == TYPE_PASSED_PORTAL:
							Portal_Points.append(source.getPoints([lastTs, event.timestamp]))
							lastTs = event.timestamp
							state = SEARCHING_DISCONNECTED
						elif event.state == TYPE_DISCONNECTED:
							Connection_Points.append(source.getPoints([lastTs, event.timestamp]))
							lastTs = event.timestamp
							state = SEARCHING_START


				#Doing it this way only filters trace wise. If only trace2 used AP2, detection points for AP2 in trace1 will not be displayed
				if config['ONLY_USED_APS']:
					config['BSSID_FILTER'] = [ u.bssid for u in UsedAPs]

			
			#Connection Data Points
			#Regroup them by download occurence (append to a new array each time the progress goes back to Zero)
			LastBytes = 0
			DataTransfer = []
			for dataTrans in source.getTransferredData():
				if dataTrans.progress <= LastBytes:
					if len(DataTransfer) > 0:
						DataPoints.append(DataTransfer)
						DataTransfer = []
						LastBytes = 0
				else:
					DataTransfer.append(dataTrans)

				LastBytes = dataTrans.progress

			if len(DataTransfer) > 0:
				DataPoints.append(DataTransfer)

			# GPS Data	
			if config['PLOT_TRAJECTORY']:
				GPSPoints.append(source.getPoints())

			##################################################################################################
			#		Access Point Data
			##################################################################################################
			if (config['PLOT_AP'] == True or config['PLOT_AP_CIRCLE'] == True):

				for nw in source.getApList():

					if len(config['BSSID_FILTER']) > 0 and ap.bssid not in config['BSSID_FILTER']:
						continue


					APBlobs[nw.bssid] = []
					APCenters[nw.bssid] = []
					for det in nw.detections:

                                                if det.rssi < config['AP_MIN_LEVEL']:
                                                    continue
						added = -1
						for i, blob in enumerate(APBlobs[nw.bssid]):
							if MapHelper.meterDist(APCenters[nw.bssid][i], det.GPS) < config['GRANULARITY']:
								added = 1
								APBlobs[nw.bssid][i].append(det)
								APCenters[nw.bssid][i] = MapHelper.locateCenter([[d.rssi, d.GPS[0], d.GPS[1]] for d in APBlobs[nw.bssid][i]])
								break

						if added < 1:
							APBlobs[nw.bssid].append([det])
							APCenters[nw.bssid].append(det.GPS)




		finalFile = config['outdir'] + "_" + METRIC_CODE + "_TrajectoryWithWifiAP.svg"

		background_path = mapHelper.getBackgroundImage()

		second_pass_path = TEMP_DIR + "./layers.svg"

		svgHelper = SVGHelper.SVGHelper(second_pass_path, image_width, image_height, mapHelper)

		#AP Area of effect
		if config['PLOT_AP_CIRCLE']:
			for bssid in APCenters:
				for i, center in enumerate(APCenters[bssid]):
					if len(APBlobs[bssid][i])< config['PLACEMENT_MIN_POINTS']:
						continue
					size = MapHelper.MaxNormalDistanceFrom(mapHelper.convertPoint(center), mapHelper.convertPoints([u.GPS for u in APBlobs[bssid][i]]))
					svgHelper.plotPoints([center], size, "purple", "black", 0.2)



		#Main trajectory
		if config['PLOT_TRAJECTORY']:
			for points in GPSPoints:
				svgHelper.plotLineFromPoints(points, config['LINE_SIZE'], config['TRAJECTORY_COLOR'])


		if config['PLOT_EVENTS']:
			#Moments when associating
			for points in Association_Points:
				svgHelper.plotLineFromPoints(points, config['LINE_SIZE'], config['COLOR_ASSOCIATING'])
			
			#Moments when obatining an ip addresss
			for points in DHCP_Points:
				svgHelper.plotLineFromPoints(points, config['LINE_SIZE'], config['COLOR_DHCP'])
			
			#Moments when authenticating to captive portal
			for points in Portal_Points:
				svgHelper.plotLineFromPoints(points, config['LINE_SIZE'], config['COLOR_PORTAL'])

			#Moments when connected
			for points in Connection_Points:
				svgHelper.plotLineFromPoints(points, config['LINE_SIZE'], config['COLOR_CONNECTED'])

			#Moments when transferring data
			for points in DataPoints:
				if len(points) > 0:
					svgHelper.plotLineFromPoints([p.network.detections[0].GPS for p in points], config['DATA_LINE_SIZE'], "green")
					if config['PLOT_BYTES']:
						LastPos = points.pop()
						progress = LastPos.progress / LastPos.total * 100
						progress_str = str(progress) + " %"
						svgHelper.addText(str(LastPos.progress), LastPos.network.detections[0].GPS, 10, "Arial")

		#Access points
		if config['PLOT_AP']:
			apToPlot = []
			for bssid in APCenters:
				for i, center in enumerate(APCenters[bssid]):
					if len(APBlobs[bssid][i])< config['PLACEMENT_MIN_POINTS']:
						continue
					apToPlot.append(center)
			svgHelper.plotPoints(apToPlot, 5, "blue", "blue")

		#Access points
		if config['PLOT_ALL_AP_POINTS']:
			svgHelper.plotPoints([w.GPS for v in APBlobs.values() for u in v for w in u], 2, "blue", "blue")
	

		# Plot the last two bytes of the bssid and the RSSI of the AP we are associating
		if config['PLOT_ASSOCIATION_BYTES']:

			#Dirty Hack for emulated coverage, where no actual association was made, use the DATA points with 
			if len(DataPoints) > 0 and len(Association_Decisions) == 0:
				for points in DataPoints:
					assoNW = points[0].network
					assoPt = assoNW.detections[0]
					Association_Decisions.append([assoPt.rssi, assoNW.bssid[12:], assoPt.GPS[0], assoPt.GPS[1]])



			for rssi, text, lat, lon in Association_Decisions:
				svgHelper.addText(text + "@" + str(rssi), (lat, lon), 12, "Arial", color="black")
		

		#Plot an horizontal line of config['SCALE_METERS'] length
		if config['SCALE']:
			Startp_lat = box_lat_min + config['SCALE_POS_Y'] * (box_lat_max - box_lat_min) 
			Startp_long = box_long_min + config['SCALE_POS_X'] * (box_long_max - box_long_min) 

			Stopp_lat = Startp_lat
			#This formula comes from the inverted meterdist maphelper function
			Stopp_long = Startp_long + degrees(2 * asin(sin(float(config['SCALE_METERS']) / (2 * MapHelper.RADIUS_EARTH)) / cos(radians(Stopp_lat))))

			svgHelper.plotLineFromPoints([(Startp_lat, Startp_long), (Stopp_lat, Stopp_long)], 4, config['SCALE_COLOR'])

			svgHelper.addText(str(config['SCALE_METERS']) + " " + config['SCALE_CAPTION_UNIT'], (Startp_lat + 0.01 * (box_lat_max - box_lat_min), Startp_long) , 10, "Arial")

		
		###Merge both Files !!!
		finalFile = config['outdir'] + METRIC_CODE + "_WifiTrajectoryMapping.svg"
		SVGHelper.addOverlay(background_path, svgHelper.svgFile.tostring() , finalFile)
