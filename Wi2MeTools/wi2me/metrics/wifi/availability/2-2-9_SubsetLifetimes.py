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

import os
import math
import random
from PIL import Image

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
from statsmodels.distributions.empirical_distribution import ECDF

from wi2me.utils import MapHelper
from wi2me.utils import SVGHelper

from wi2me.model import APManager

from settings import TEMP_DIR

METRIC_NAME = "Measure subset evolution in time"
METRIC_CODE = "2.2.9"

DEFAULT_CONFIG = {
                    "INTERPOLATION_MAX_DIST":20, #Maximal distance between two consecutive scans in order to populate cell with their intersections
                    "MIN_EXPLORATION_LEVEL":1, # Minimal number of trace having explored a cell in order to consider given cell
		}

colors = {}

def dist(a, b):
    return math.sqrt(math.pow(a[0] - b[0], 2) + math.pow(a[1] - b[1], 2))


def streetsToCells(streets):
    retval = []

    for start, end in streets:
        dx = end[0] - start[0]
        dy = end[1] - start[1]
        d = dist(start, end)
        for step in range(int(d) + 1):
            x = start[0] + step / d * dx
            y = start[1] + step / d * dy
            if (x, y) not in retval:
                retval.append((x, y))

    return retval

def plotCellMap(cells, svg_path, hlpr):

    w, h = hlpr.getDimensions()
    background_path = hlpr.getBackgroundImage()
    second_pass_path = TEMP_DIR + "./layers.svg"

    artist = SVGHelper.SVGHelper(second_pass_path, w, h, hlpr,)

    color = "red"
    artist.plotPoints(cells, 1, color, stroke = color, convert = False)

    artist.finish()

    SVGHelper.addOverlay(background_path, artist.svgFile.tostring() , svg_path)

    fig = plt.figure()

    plt.scatter(*zip(*cells))

    fig.savefig(svg_path + "matpl.svg", bbox_inches='tight')
			
    plt.close(fig)

def randomColor():
    cStart = 64
    cEnd = 196
    retval = []
    
    for _ in range(3):
        retval.append(random.randint(cStart, cEnd))
    
    return tuple(retval)

            
def colorToString(color):
    return "rgb(" + str(color[0]) + "," + str(color[1]) + "," + str(color[2]) + ")"

def getApColor(apId):
    if apId not in colors:
        colors[apId] = randomColor()
    return colors[apId]

def plotSubsetMap(cells, svg_path, hlpr):

    w, h = hlpr.getDimensions()
    background_path = hlpr.getBackgroundImage()
    second_pass_path = TEMP_DIR + "./layers.svg"

    artist = SVGHelper.SVGHelper(second_pass_path, w, h, hlpr,)
    for apId in set(cells.values()):
        color = colorToString(getApColor(apId))
        artist.plotPoints([c for c in cells if cells[c] == apId], 1, color, stroke = color, convert = False)

    artist.finish()

    SVGHelper.addOverlay(background_path, artist.svgFile.tostring() , svg_path)


def getGreedySubset(cells, minRssi = -200):
    retval = {}
    retaps = []

    #List the aps available in these cells
    apIds = set([bssidToId(u.bssid) for content in cells.values() for u in content])

    #List the cells to cover
    cellsToCover = [c for c in cells if max([u.detections[0].rssi for u in cells[c]]) >= minRssi]

    while len(cellsToCover) > 0:
        #List the cells covered by each aps
        apCoverages = {i:[c for c in cells if c in cellsToCover if i in [bssidToId(u.bssid) for u in cells[c] if u.detections[0].rssi >= minRssi]] for i in apIds}
        
        #Pick the AP that covers the most cells
        bestAp = max(apCoverages, key = lambda x : len(apCoverages[x]))

        for c in apCoverages[bestAp]:
            retval[c] = bestAp
        retaps.append(bestAp)

        #Update the list of cells remaining to cover
        cellsToCover = [c for c in cellsToCover if c not in apCoverages[bestAp]]

    return retval, retaps

def bssidToId(bssid):
    return bssid[3:14]

def saveSubsetBitmap(data, path, relativeX = True):
    if len(data) > 0:

        endTs = max ([ts for c in data for ts in c])
        startTs = min ([ts for c in data for ts in c])
        timestamps = sorted(set([ts for c in data for ts in c]))

        height = len(data)
        width = height

        im = Image.new("RGB", (width + 1, height), "black")

        data = sorted(data, key = lambda x : ''.join([x[u] for u in sorted(x)]))

        for line, d in enumerate(data):
            lastCol = -1
            lastAp = None

            for ts in sorted(d):
                ap = d[ts]

                #Place the columns with abcisses relative to time, or regularily
                if relativeX:
                    col = int(float(ts - startTs) / (endTs - startTs) * width)
                else:
                    col = int(float(timestamps.index(ts)) / (len(timestamps) - 1) * width)
                    

                im.putpixel((col, line), getApColor(ap))

                if lastAp == ap:
                    for col2 in range(lastCol + 1, col):
                        im.putpixel((col2, line), getApColor(ap))
                elif lastAp is not None:
                    for col2 in range(lastCol + 1, col - 1):
                        im.putpixel((col2, line), (255,255,255))
                
                im.putpixel((col, line), (255,0,0))

                lastCol = col
                lastAp = ap

        im.save(path)

def plotContribCDF(data, path):
    aps = set([v for u in data for v in u.values()])
    
    contribs = []

    for ap in aps:
        contrib = 0
        for cell in data:
            if ap in cell.values():
                contrib += 1
        
        contribs.append(contrib)

    if len(contribs) > 0:

        fig = plt.figure()
        cdf = ECDF(contribs)

        plt.step(cdf.x, cdf.y)

        plt.ylabel("CDF")
        plt.xlabel("Cell coverage")
			
        fig.savefig(path, bbox_inches='tight')
			
        plt.close(fig)

def plotLifetimeCDF(data, path):
    durations = []
    singlePointCells = 0  #Cells with a single datapoint
    switchingCells = 0 #Cells without a durable AP

    for cellCov in data:
        lastAp = None
        lastTs = -1
        dur = 0
        cellDurations = []

        if len(cellCov) == 1:
            singlePointCells += 1
        else:
            for ts in cellCov:
                ap = cellCov[ts]
                if ap == lastAp:
                    dur += ts - lastTs
                else:
                    if dur > 0:
                        cellDurations.append(dur)                
                        dur = 0

                lastTs = ts
                lastAp = ap

            if dur > 0:
                cellDurations.append(dur)

            if len(cellDurations) == 0:
                switchingCells += 1
            else:
                durations += cellDurations

    outF = open(path + '.txt', 'w')
    outF.write("cell count " + str(len(data)) + '\n')
    outF.write("Single Point data " + str(singlePointCells) + '\n')
    outF.write("Switching cells " + str(switchingCells) + '\n')

    outF.close()

    if len(durations) > 0:

        fig = plt.figure()
        cdf = ECDF(durations)

        plt.step(cdf.x, cdf.y)

        plt.ylabel("CDF")
        plt.xlabel("Duration")

        Day = 1000 * 3600 * 24
        Week = Day * 24
        Month = Day * 30
        Year = Day * 365
        Ticks = [Month, 3 * Month, 6 * Month, Year]
        TickLabels = [
            "1 Month",
            "3 Month",
            "6 Month",
            "1 Year",
        ]
        plt.xticks(Ticks, TickLabels, rotation='vertical')

			
        fig.savefig(path, bbox_inches='tight')
			
        plt.close(fig)

#Transaction journal specifi implem, ditch after
def generateLifetimeRssITable(data, apCellRssis, path):
    durations = []
    singlePointCells = 0  #Cells with a single datapoint
    switchingCells = 0 #Cells without a durable AP
    durationRssis = {}

    print apCellRssis

    for cell in data:
        cellCov = data[cell]
        lastAp = None
        lastTs = -1
        dur = 0
        cellDurations = []

        if len(cellCov) == 1:
            singlePointCells += 1
        else:
            for ts in cellCov:
                ap = cellCov[ts]
                if ap == lastAp:
                    dur += ts - lastTs
                else:
                    if dur > 0:
                        cellDurations.append(dur)
                        if cell in apCellRssis[lastAp]:
                            if dur not in durationRssis:
                                durationRssis[dur] = []
                            durationRssis[dur].append(apCellRssis[lastAp][cell])
                        dur = 0

                lastTs = ts
                lastAp = ap

            if dur > 0:
                cellDurations.append(dur)
                if cell in apCellRssis[ap]:
                    if dur not in durationRssis:
                        durationRssis[dur] = []
                    durationRssis[dur].append(apCellRssis[ap][cell])

            if len(cellDurations) == 0:
                switchingCells += 1
            else:
                durations += cellDurations

    Day = 1000 * 3600 * 24
    Week = Day * 24
    Month = Day * 30
    Year = Day * 365

    DurationLineLimits = {
            "0" : 0,
            "1 Week": Day * 7,
            "1 Month": Month,
            "3 Month": Month * 2,
            "1 Year": Year,
    }
    DurationLines = {}
    RssiLines = {}

    for duration in durations:
        closestlimit = max([l for l in DurationLineLimits if DurationLineLimits[l] <= duration], key = lambda x : DurationLineLimits[x])
        if closestlimit not in DurationLines:
            DurationLines[closestlimit] = 0
            RssiLines[closestlimit] = []
        DurationLines[closestlimit] += 1
        
        if duration in durationRssis and len(durationRssis[duration]) > 0:
            RssiLines[closestlimit].append(durationRssis[duration].pop())

    print DurationLines
    print {k:np.average(RssiLines[k]) for k in RssiLines}
    

def plotEvolution(additionnal_cells, renewed_cells, unexplored_cells, confirmed_cells, path):
    #This plots four curves filled inbetween, assuming all four parameter dicts have exactly the same keys
  
    #No warranties regarding sorting.... 
    X = sorted(additionnal_cells.keys())
    additionnal_cells = [additionnal_cells[x] for x in X]
    renewed_cells = [renewed_cells[x] for x in X]
    unexplored_cells = [unexplored_cells[x] for x in X]
    confirmed_cells = [confirmed_cells[x] for x in X]
    
    fig = plt.figure()

    plt.ylabel("Cell count")
    plt.xlabel("Time")
    
    Y0 = [0 for _ in additionnal_cells]
    for _list, color, label in [
                (unexplored_cells, "darkgreen", "Non visited cells"),
                (confirmed_cells, "green", "Subset availability confirmed"),
                (renewed_cells, "red", "Subset failure"),
                (additionnal_cells, "yellow", "New cells")
            ]:
        Y = []
        for x, val in enumerate(_list):
            Y.append(Y0[x] + val)

        plt.fill_between(X, Y, Y0, color = color, label = label)

        Y0 = Y

    plt.legend(loc='lower center', bbox_to_anchor=(0.5, 1.05), ncol=2,)
			
    fig.savefig(path, bbox_inches='tight')
			
    plt.close(fig)


def getExploredCells(cells, sources, explorationLevel, interpolationMaxDist, mapHelper, outpath = None):
    explorations = {}

    for srcInd, source in enumerate(sources):
        lastCell = None
        for gps_pt in source.getPoints():
            xy_pt = mapHelper.convertPointToMeters(gps_pt)
            cell = min(cells, key = lambda x : dist(xy_pt, x))
            if cell not in explorations:
                explorations[cell] = []
            explorations[cell].append(srcInd)

            if lastCell is not None:
                distance = dist(lastCell, cell)
                if distance > 0 and distance <= interpolationMaxDist:
                    center = ((cell[0] + lastCell[0]) / 2, (cell[1] + lastCell[1]) / 2)
                    radius = dist(cell, lastCell) / 2 
                    for c in cells:
                        if dist(center, c) <= radius and c not in (cell, lastCell):
                            if c not in explorations:
                                explorations[c] = []
                            explorations[c].append(srcInd)
            
            lastCell = cell
    
    retval = [c for c in explorations if len(set(explorations[c])) >= explorationLevel]

    if outpath is not None:
        #Exploration CDF
        fig = plt.figure()
        cdf = ECDF([len(set(v)) for v in explorations.values()])

        plt.step(cdf.x, cdf.y)

        plt.ylabel("CDF")
        plt.xlabel("ExplorationLevels")
			
        fig.savefig(outpath, bbox_inches='tight')
			
        plt.close(fig)
    
        #Exploration Map
        w, h = mapHelper.getDimensions()
        background_path = mapHelper.getBackgroundImage()
        second_pass_path = TEMP_DIR + "./layers.svg"

        artist = SVGHelper.SVGHelper(second_pass_path, w, h, mapHelper,)

        cellByExplorationLevel = {}
        for c in explorations:
            lvl = len(set(explorations[c]))
            if lvl not in cellByExplorationLevel:
                cellByExplorationLevel[lvl] = []
            cellByExplorationLevel[lvl].append(c)

        
        for lvl in range(1, max(cellByExplorationLevel.keys()) + 1):
            color = "green"
            if lvl == 1:
                color = "red"
            if lvl == 2:
                color = "orange"
            if lvl == 3:
                color = "yellow"
        
            if lvl in cellByExplorationLevel:
                artist.plotPoints(cellByExplorationLevel[lvl], 1, color, stroke = color, convert = False)

        artist.finish()

        SVGHelper.addOverlay(background_path, artist.svgFile.tostring() , outpath + "map.svg")

    return retval

def plot(config):
    config['data'] = sorted(config['data'], key = lambda x : x.getStartTime())
    starTime = config['data'][0].getStartTime()
    stopTime = config['data'][-1].getEndTime()
        
    mapHelper = MapHelper.MapHelper(config['data'])
    roads = []
    for rd in mapHelper.getRoadsFromOSM():
        points = mapHelper.convertPointsToMeters(rd)
        for ptInd in range(1, len(points)):
            roads.append((points[ptInd - 1], points[ptInd]))
    
    print "roads loaded"

    cells = streetsToCells(roads)

    print "cells generated"

    plotCellMap(cells, config["outdir"] + METRIC_CODE + "_cellMap.svg", mapHelper)

    #Delete unused cells
    cells = getExploredCells(cells, config['data'], 1, config['INTERPOLATION_MAX_DIST'], mapHelper)

    print "Cleaned up cells"

    exploredCells = getExploredCells(cells, config['data'], config['MIN_EXPLORATION_LEVEL'], config['INTERPOLATION_MAX_DIST'], mapHelper, outpath = config["outdir"] + METRIC_CODE + "_explorationCDF.svg")

    print "exploredCells selected"

    if len(config['data']) <= 1:
        print "Need at least two sources to compute subset evolution"
    else:
        for minRssi in [-90]:

            ap_subset = []
            covered_cells = {}

            additionnal_cells = {}
            renewed_cells = {}
            unexplored_cells = {}
            confirmed_cells = {}
            cellCount = 0

            ap_cell_rssis = {}

            for srcInd, source in enumerate(config['data']):
                print str(srcInd) + "/" + str(len(config['data']))
                sourceTime = source.getStartTime()
                for _dict in [additionnal_cells, renewed_cells, unexplored_cells, confirmed_cells]:
                    _dict[sourceTime] = 0

                source_results = {}
                cells_for_subset_computation = {}

                lastScan = []
                lastCell = None
                for scan in source.getScanResults():
                    #Place scan results in closest cell
                    cell = min(cells, key = lambda x : dist(mapHelper.convertPointToMeters(scan[0].detections[0].GPS), x))
                    if cell in exploredCells:
                        source_results[cell] = scan
                        for res in scan:
                            apId = bssidToId(res.bssid) 
                            if apId not in ap_cell_rssis:
                                ap_cell_rssis[apId] = {cell:[res.detections[0].rssi]}
                            elif cell not in ap_cell_rssis[apId]:
                                ap_cell_rssis[apId][cell] = [res.detections[0].rssi]
                            else:
                                ap_cell_rssis[apId][cell].append(res.detections[0].rssi)

                    #Interpolate with last cell
                    bssids = [u.bssid for u in scan]
                    intersection = [n for n in lastScan if n.bssid in bssids]

                    if len(intersection) > 0:
                        distance = MapHelper.meterDist(scan[0].detections[0].GPS, lastScan[0].detections[0].GPS)
                        if distance <= config['INTERPOLATION_MAX_DIST']:
                            #Fill all the cells that are inside the circle of diameter [currentCell:lastCell]
                            center = ((cell[0] + lastCell[0]) / 2, (cell[1] + lastCell[1]) / 2)
                            radius = dist(cell, lastCell) / 2 
                            for c in cells:
                                if dist(center, c) <= radius and c not in [cell, lastCell]:
                                    if c in exploredCells:
                                        source_results[c] = intersection

                    lastCell = cell
                    lastScan = scan

                for c in source_results:
                    if c not in covered_cells:#never explored before cell
                        subset_members = [u for u in source_results[c] if bssidToId(u.bssid) in ap_subset]
                        if len(subset_members) > 0: #Covered by previous subset members
                            covered_cells[c] = {sourceTime:bssidToId(subset_members[0].bssid)}
                        else: #Not covered by any subset member
                            cells_for_subset_computation[c] = source_results[c]
                        additionnal_cells[sourceTime] += 1
                    else: #Cell was already explored
                        if covered_cells[c].values()[-1] in [bssidToId(u.bssid) for u in source_results[c]]: #Coverage still available, store the timestamp
                            covered_cells[c][sourceTime] = covered_cells[c].values()[-1]
                            confirmed_cells[sourceTime] += 1
                        else: # Previous coverage set no longer works
                            cells_for_subset_computation[c] = source_results[c]
                            renewed_cells[sourceTime] += 1
                
                unexplored_cells[sourceTime] = cellCount - renewed_cells[sourceTime] - confirmed_cells[sourceTime]
                cellCount += additionnal_cells[sourceTime]

                src_covered_cells, src_ap_subset = getGreedySubset(cells_for_subset_computation, minRssi = minRssi)

                ap_subset += src_ap_subset
                for c in src_covered_cells:
                    if c in covered_cells:
                        covered_cells[c][sourceTime]  = src_covered_cells[c]
                    else:
                        covered_cells[c] = {sourceTime : src_covered_cells[c]}

                #plotSubsetMap({u:covered_cells[u].values()[-1] for u in covered_cells}, config["outdir"] + METRIC_CODE + "_subsetMap_" + str(srcInd) + ".svg", mapHelper)

            basename = config["outdir"] + METRIC_CODE + "_rssi=" + str(minRssi)

            saveSubsetBitmap(covered_cells.values(), basename + "_subsetMatrix_abs.bmp", relativeX = True)
            saveSubsetBitmap(covered_cells.values(), basename + "_subsetMatrix_seq.bmp", relativeX = False)
            saveSubsetBitmap([u for u in covered_cells.values() if len(u) > 1], basename + "_subsetMatrix_seq_min1.bmp", relativeX = True)
            saveSubsetBitmap([u for u in covered_cells.values() if len(u) > 1], basename + "_subsetMatrix_abs_min1.bmp", relativeX = False)
            plotContribCDF(covered_cells.values(), basename + "_contribCDF.svg")
            plotEvolution(additionnal_cells, renewed_cells, unexplored_cells, confirmed_cells, basename + "_evolution.svg")
            plotLifetimeCDF([u for u in covered_cells.values() if len(u)> 0], basename + "_lifetimeCDF.svg")

            #Lifetime in cells that were visited in a timespan superior to a year
            plotLifetimeCDF([u for u in covered_cells.values() if max(u) - min(u) >= 31104000000], basename + "_lifetimeCDF_overAyear.svg")
            generateLifetimeRssITable(covered_cells, ap_cell_rssis, basename + "_lifetimeCDF_overAyear.svg")

            outF = open(basename + "_stats.txt", 'w')
            outF.write("replaced cells : " + str(sum(renewed_cells.values())) + "\n")
            outF.write("explored cells : " + str(len(exploredCells))+ "\n")

            outF.write("duration : " + str(stopTime - starTime)+ " ms\n")

            outF.close()
