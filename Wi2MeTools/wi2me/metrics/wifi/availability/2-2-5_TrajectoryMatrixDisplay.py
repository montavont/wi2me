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
from PIL import Image
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
from statsmodels.distributions.empirical_distribution import ECDF


from settings import TEMP_DIR
from wi2me.utils import MatrixHelper
from wi2me.model.APManager import APManager

METRIC_NAME = "Matrix bitmap representation - Coverage path selection"
METRIC_CODE = "2.2.5"

DEFAULT_CONFIG = {
		'SINGLE':True,
		'VERBOSE':False,
		'MATRIX_BACKGROUND_COLOR':(255, 255, 255),
		'MATRIX_AP_COLOR':(100, 100, 100),
		'MATRIX_TRANSITION_COLOR':(200, 200, 200),
		'MATRIX_PLOT_TRANSITIONS':False,
		}

BMP_MATRIX_BASENAME = "coverage_matrix"
MATRIX_PIC_EXT = '.bmp'
TEXT_DUMP_EXT = '.txt'
FIGURE_FORMAT = '.svg'
OVERLAPPING_SUFFIX = "_overlapping_cdf"
MINCONTRIB_SUFFIX =  "_minimal_contribution_impact_"
MATRIX_SELECTED_AP_COLOR =  (0, 150, 0)

def plotSelectionOnMatrix(path, coveragePath, matrix):
	im = MatrixHelper.saveBlackNWhiteBitmap(matrix)
	columns = sorted([ap for ap in set(zip(*coveragePath)[0])])
	for ap in columns:
		for line in np.nonzero(matrix.T[ap])[0]:
			#for col in coveragePath[:ap]:
			im.putpixel((ap, line), MATRIX_SELECTED_AP_COLOR)
	im.save(path + MATRIX_PIC_EXT)


def plotOverlapping(matrix, coveragePath, path = ""):
	over = []
	columns = sorted([ap for ap in set(zip(*coveragePath)[0])])

	for line in range(matrix.shape[0]):
		over.append(len(np.nonzero(matrix[line])[0]))

	mean = np.mean(over)

	if len(path) > 0:
			
		fig = plt.figure()

		ecdf = ECDF(over)

		plt.step(ecdf.x, ecdf.y)
		plt.ylabel("CDF")
		plt.xlabel("Coverage Overlapping")

		fig.tight_layout()

		fig.savefig(path)
		plt.close(fig)

	return mean

def DumpCovDataToFile(path, coveragePath, matrix, APs = [], uncovered = 0, algo = ""):
	NumLines, NumCols = matrix.shape
	columns = sorted([ap for ap in set(zip(*coveragePath)[0])])

	out = open(path, 'w')
	out.write("Coverage Algorithm : " + algo)
	out.write("Coverage(%)	" + str((NumLines - uncovered) * 100.0 / NumLines)  + "\n")
	out.write("TotalLines	" + str(NumLines) + "\n")
	out.write("SkippedLines	" + str(uncovered) + "\n")
	out.write("TotalAPs	" + str(NumCols) + "\n")
	out.write("UsedColumns	" + str(len(columns)) + "\n")
	out.write("UsedColumns(%)	" + str(len(columns) * 100.0 / NumCols) + "\n")
	out.write("MeanOverlapping	" + str(plotOverlapping(matrix, coveragePath)) + "\n")

	out.write("\n")	
	out.write("APLIST\n")	
	for ap in APs:
		for nw in ap.networks:
			out.write(nw.bssid + "\n")	

	out.close
	

def contributionCDF(mat, coveragePath):
	contribs = [float(cont) / mat.shape[0] * 100 for cont in zip(*coveragePath)[1]]
	ecdf = ECDF(contribs)
	return [ecdf.x, ecdf.y]

def coverageForMinContribution(mat, coveragePath, uncovered):
	contribs = sorted([float(cont) / mat.shape[0] * 100 for cont in zip(*coveragePath)[1]])
	coverage = 100.0 - float(uncovered) / mat.shape[0]
	steps = [coverage]

	for cont in contribs:
		coverage -= cont
		steps.append(coverage)

	return [range(len(contribs) + 1), steps]

def plotXYFig(data, path, xLabel, yLabel, ylim=None):

	legends = []
	legendNames = []	

	fig = plt.figure()
	for X, Y, leg in data:
		plotted, = plt.step(X, Y)
		legendNames.append(leg)
		legends.append(plotted)

	if ylim != None:
		plt.ylim(ylim)
	plt.ylabel(yLabel)
	plt.xlabel(xLabel)

	plt.legend(legends, legendNames)

	fig.tight_layout()

	fig.savefig(path)




def CountHandovers(mat, coveragePath):

	#If only one ap is necessary, 0 handover is needed. Thus start counting at -1 
	handoverCount = -1

	columns = sorted([ap[0] for ap in coveragePath[0]])

	line = 0
	while line < mat.shape[0]:
		#how much more we can stay on each of the other columns	without interruptions
		zeros = [np.where(mat.T[col, line:] == 0)[0] for col in columns]
		potentials = [0]
		for z in zeros:
			#If no zero, fully covered line till the end
			if len(z) == 0:
				potentials.append(mat.shape[0] - line)
			else:
				potentials.append(z[0])

		#Select the one that takes us the longest way
		successor = max(potentials)
		line += successor

		#Got an empty line, skip to the next, but do not count it as a handover (will be counted once out of the empty zone)
		if successor == 0:
			line += 1
		else:
			handoverCount += 1

	return handoverCount

def ComputeOverlapping(matrix, coveragePath):
	over = []

	columns = sorted([ap[0] for ap in coveragePath[0]])

	for line in range(matrix.shape[0]):
		over.append(len(np.nonzero(matrix[line][columns])[0]))

	mean = np.mean(over)

	return mean


def ComputeIntraAPOverlapping(matrix):
	over = []

	for col in matrix.T:
		aps = col[np.nonzero(col)]
		if len(aps) == 0:
			continue

		available = max([len(line) for line in col[np.nonzero(col)]])
		colpres = []
		for ap in aps:
			colpres.append(float(len(ap)) / available )

		over.append(np.mean(colpres))
		
	mean = np.mean(over)

	return mean




def plot(config):

	dMatrixes = []

	
	apMgr = APManager(config['data'])	
	bssMatrixes = apMgr.getBssidMatrixes()

	#Plot for separated sources
	for matInd, mat in enumerate(bssMatrixes):
		source = config['data'][matInd]
		contribCDFs = []
		contribDecons = []
		outPath = config['outdir'] + METRIC_CODE + "_BSSID_" + source.name + "_" + BMP_MATRIX_BASENAME
		
		#Simple matrix with no path
		for coverage in [MatrixHelper.coverageContinuous, MatrixHelper.coverageGreedy]:
			coveragePath, uncovered = coverage(mat)
			plotOverlapping(mat, coveragePath, config['outdir'] + METRIC_CODE + "_" + source.name + "_with_" + coverage.__name__  + OVERLAPPING_SUFFIX + FIGURE_FORMAT)
			X, Y = contributionCDF(mat, coveragePath)
			contribCDFs.append([X, Y, coverage.__name__])
			X, Y = coverageForMinContribution(mat, coveragePath, uncovered)
			contribDecons.append([X, Y, coverage.__name__])

		path = config['outdir'] + METRIC_CODE + "_" + source.name + "_"
		plotXYFig(contribCDFs, path + MINCONTRIB_SUFFIX + "CDF" + FIGURE_FORMAT, "AP Contribution (%)", "CDF")
		plotXYFig(contribDecons, path + MINCONTRIB_SUFFIX + "DECONSTRUCTION" + FIGURE_FORMAT, "APs", "Remaining Coverage", ylim=[0, 100])

	for matInd, mat in enumerate(apMgr.getAPMatrixes()):
		mat = MatrixHelper.orderMatrix(mat)
		source = config['data'][matInd]
		contribCDFs = []
		contribDecons = []
		outPath = config['outdir'] + METRIC_CODE + "_AP_" + source.name + "_" + BMP_MATRIX_BASENAME
		MatrixHelper.plotAnimatedMatrix(mat, outPath + ".html")
		MatrixHelper.saveBlackNWhiteBitmap(mat, outPath +  ".bmp")

		distanceMatrix = MatrixHelper.convertToDistanceMatrix(mat)
		dMatrixes.append(distanceMatrix)
		
		for coverage in [MatrixHelper.coverageContinuous, MatrixHelper.coverageGreedy]:
			coveragePath, uncovered = coverage(distanceMatrix)
			path = config['outdir'] + METRIC_CODE + "_" + source.name + "_" + BMP_MATRIX_BASENAME + "_with_" + coverage.__name__
			plotSelectionOnMatrix(path + MATRIX_PIC_EXT, coveragePath, distanceMatrix)
			DumpCovDataToFile(path + TEXT_DUMP_EXT, coveragePath, distanceMatrix, uncovered =  uncovered)

	#Plot Merged Matrixes
	#Merge by columns using distance Matrices 
	for mergeFunc in [MatrixHelper.COLU_LII_MergeColorize]:
		#for splitFunc in [MatrixHelper.bssidTimeSplitting, MatrixHelper.bssidSsidSplitting]:
		outPath = config['outdir'] + METRIC_CODE + "_mergedby_" + mergeFunc.__name__ 

		traceMat = MatrixHelper.mergeMatrixesByColumns(dMatrixes, mergeFunc, MatrixHelper.MERGE_COLOR_FIRST)
		traceMat = MatrixHelper.orderMatrix(traceMat)

		for cov in [MatrixHelper.coverageGreedy, MatrixHelper.coverageContinuous]:
			matPath = outPath + "_" + cov.__name__
			coveragePath, uncovered = cov(traceMat)

			apSelected = {}
			for apCol, _ in coveragePath:
				for pos in traceMat.T[apCol]:
					if pos != 0:
						for det in pos:
							if det.nw.ap.id not in apSelected:
								apSelected[det.nw.ap.id] = det.nw.ap
				

			DumpCovDataToFile(matPath + TEXT_DUMP_EXT, coveragePath, traceMat, uncovered =  uncovered, algo = cov.__name__, APs = apSelected.values())
			plotSelectionOnMatrix(matPath + MATRIX_PIC_EXT, coveragePath, traceMat)
	
		MatrixHelper.saveBlackNWhiteBitmap(traceMat, outPath + ".bmp")
		MatrixHelper.plotAnimatedMatrix(traceMat, outPath + ".html")

