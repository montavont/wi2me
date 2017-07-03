#!/usr/bin/python
import os
import sys
import signal
#import subprocess

import time
import unittest
from selenium import webdriver
from selenium.webdriver.common.action_chains import ActionChains;


from TestUtils import *

class Wi2MeTest(unittest.TestCase):

    def setUp(self):

        desired_capabilities = {'aut': 'telecom.wi2meRecherche:6.2'}

        self.driver = webdriver.Remote(
            desired_capabilities=desired_capabilities
        )
        self.driver.implicitly_wait(30)

    def tearDown(self):
        self.driver.quit()


class Wi2MeTestMenuPages(Wi2MeTest):
	
	def _test_menu(self):
		NUM_TEST=100
		

		#get main activity
		self.driver.get('and-activity://telecom.wi2meRecherche.Wi2MeRecherche')
		self.assertTrue("and-activity://Wi2MeRecherche" in self.driver.current_url)
		
		for i in range(NUM_TEST * 2):
			chain = ActionChains(self.driver);
			chain.send_keys(u'\ue102').perform();
		
		self.assertTrue("and-activity://Wi2MeRecherche" in self.driver.current_url)

	def _test_menu_running(self):
		NUM_TEST=100 * 2 # Even number to open then close the menu 
		

		#get main activity
		self.driver.get('and-activity://telecom.wi2meRecherche.Wi2MeRecherche')
		self.assertTrue("and-activity://Wi2MeRecherche" in self.driver.current_url)
		
		#open Menu
		chain = ActionChains(self.driver);
		chain.send_keys(u'\ue102').perform();

		#Start the application
		self.driver.find_elements_by_link_text("Start")[0].click()

		time.sleep(6)

		for i in range(NUM_TEST):
			chain2 = ActionChains(self.driver);
			chain2.send_keys(u'\ue102').perform();

		self.assertTrue("and-activity://Wi2MeRecherche" in self.driver.current_url)

	def test_ui_line_running(self):
		NUM_TEST=50
		
		chain = ActionChains(self.driver);

		#get main activity
		self.driver.get('and-activity://telecom.wi2meRecherche.Wi2MeRecherche')
		self.assertTrue("and-activity://Wi2MeRecherche" in self.driver.current_url)
		
		#open Menu
		chain.send_keys(u'\ue102')
		chain.perform()


		#Start the application
		self.driver.find_elements_by_link_text("Start")[0].click()

		while True:
			print self.driver.current_url
			time.sleep(1)
		
		time.sleep(6)
		self.driver.get('and-activity://telecom.wi2meRecherche.Wi2MeRecherche')

		for i in range(1, NUM_TEST):
			print i
			line = self.driver.find_elements_by_link_text("Latitude")[0]
			line.click()
	
	def _test_start_stop_running(self): #NOFONC 
		
		chain = ActionChains(self.driver);
		chain2 = ActionChains(self.driver);

		#get main activity
		self.driver.get('and-activity://telecom.wi2meRecherche.Wi2MeRecherche')
		self.assertTrue("and-activity://Wi2MeRecherche" in self.driver.current_url)
		
		#open Menu
		chain.send_keys(u'\ue102')
		chain.perform()

		#Start the application
		self.driver.find_elements_by_link_text("Start")[0].click()
		
		chain2.send_keys(u'\ue102')
		chain2.perform()


if __name__ == '__main__':

	startServer = True
	pro = None
	
	if startServer:

		pro = startSelendroidProcess()

	try:
		unittest.main()
	finally:
		if startServer:
			os.killpg(pro.pid, signal.SIGTERM)
