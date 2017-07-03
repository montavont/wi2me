import sys
import os
import signal 
import subprocess
import time

KEY_ALT_LEFT=u'\ue00A'
KEY_DEL=u'\ue017'
KEY_DPAD_DOWN=u'\ue017'
KEY_DPAD_LEFT=u'\ue012'
KEY_DPAD_RIGHT=u'\ue014'
KEY_DPAD_UP=u'\ue013'
KEY_ENTER=u'\ue007'
KEY_SHIFT_LEFT=u'\ue008'
KEY_BACK=u'\ue100'
KEY_ANDROID_HOME=u'\ue101'
KEY_MENU=u'\ue102'
KEY_SEARCH=u'\ue103'
KEY_SYM=u'\ue104'
KEY_ALT_RIGHT=u'\ue105'
KEY_SHIFT_RIGHT=u'\ue106'

SELENDROID_JAR = "selendroid-standalone-0.16.0-with-dependencies.jar"
APKS = ["../../Wi2MeUser/bin/Wi2MeUserActivity-debug.apk", "../../Wi2MeRecherche/bin/Wi2MeRecherche-debug.apk"]

def startSelendroidProcess(extraSleep=5, verbose=False):

	#Gracious closing
	def handler(signum, stack):
		os.killpg(pro.pid, signal.SIGTERM)
		sys.exit(0)

        signal.signal(signal.SIGINT, handler)
 	signal.signal(signal.SIGTERM, handler)

	selenCommandLine = "java -jar "
	selenCommandLine += SELENDROID_JAR
	for apk in APKS:
		selenCommandLine += " -aut "
		selenCommandLine += apk

	selenCommandLine += " -logLevel VERBOSE -forceReinstall"

	pro = subprocess.Popen(selenCommandLine, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, shell=True, preexec_fn=os.setsid)

	runningLine = "INFO: Selendroid standalone server has been started on port: 4444"
	line = ""
	while runningLine not in line:
		line = pro.stdout.readline()
		if verbose:
			print line

	time.sleep(extraSleep)

	return pro
