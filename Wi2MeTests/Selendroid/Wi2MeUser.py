#!/usr/bin/python
import os
import sys
import signal

import time
import unittest
from selenium import webdriver
from selenium.webdriver.common.action_chains import ActionChains;

from TestUtils import *



ACT_URL_MAIN = 'and-activity://Wi2MeUserActivity'
ACT_URL_PREF = 'and-activity://Wi2MePreferenceActivity'
ACT_URL_NETW = 'and-activity://Wi2MeNetworkManagerActivity'

class Wi2MeTest(unittest.TestCase):

    def setUp(self):
        desired_capabilities = {'aut': 'telecom.wi2meUser:6.2'}

        self.driver = webdriver.Remote(
            desired_capabilities=desired_capabilities
        )
        self.driver.implicitly_wait(30)

    def tearDown(self):
        self.driver.quit()


class Wi2MeTestMenuPages(Wi2MeTest):


	def waitForUrl(self, url, timeout = 10, additionnalTime = 1):
		match = False
		startT = time.time()
		time.sleep(additionnalTime)
		while not match:
			if time.time() - startT > timeout:
				print "timout waiting for url : " + url
				self.assertTrue(False)
				break
			match = url ==  self.driver.current_url
		self.assertTrue(match)

	
	def test_menu(self): #DISBALED _ NOFONC

		NUM_TEST=10
				
		#get main activity
		self.driver.get('and-activity://telecom.wi2meUser.Wi2MeUserActivity')
		self.assertTrue("and-activity://Wi2MeUserActivity" in self.driver.current_url)

		self.driver.find_element_by_id('btnstart')

		#Menu Menu combination
		for i in range(NUM_TEST):
			chain = ActionChains(self.driver)

			chain.send_keys(KEY_MENU)
			chain.send_keys(KEY_MENU)

			chain.perform();


	def test_menu_buttons(self):
		NUM_TEST=10
				
		#get main activity
		self.driver.get('and-activity://telecom.wi2meUser.Wi2MeUserActivity')
		self.assertTrue("and-activity://Wi2MeUserActivity" in self.driver.current_url)

		menuElems = [["Preferences", ACT_URL_PREF], ["Network Management", ACT_URL_NETW]]

		for pref, actUrl in menuElems:
			
			for i in range(NUM_TEST):
				chain = ActionChains(self.driver);

				chain.send_keys(KEY_MENU).perform();
				self.driver.find_elements_by_link_text(pref)[0].click()
				self.waitForUrl(actUrl)
			
				chain = ActionChains(self.driver);

				chain.send_keys(KEY_BACK).perform();
				self.waitForUrl(ACT_URL_MAIN)
				self.assertEquals(self.driver.current_url, "and-activity://Wi2MeUserActivity");

	def test_upload(self):
		NUM_TEST=10
				
		#get main activity
		self.driver.get('and-activity://telecom.wi2meUser.Wi2MeUserActivity')
		self.assertTrue("and-activity://Wi2MeUserActivity" in self.driver.current_url)

		for i in range(NUM_TEST):
			chain = ActionChains(self.driver);
			chain.send_keys(KEY_MENU).perform();
			self.driver.find_elements_by_link_text("Upload")[0].click()

			self.assertEquals(self.driver.current_url, "and-activity://Wi2MeUserActivity");

	def test_network_management(self):
		NUM_TEST=10
				
		#get main activity
		self.driver.get('and-activity://telecom.wi2meUser.Wi2MeUserActivity')
		self.assertTrue("and-activity://Wi2MeUserActivity" in self.driver.current_url)

		self.driver.find_element_by_id('btnstart')

		name = "Network Management"
		#tabs = ["Preference", "Personal Network", "Community Network"]
		tabs = ["Preference", "Community Network"]
			
		for i in range(NUM_TEST):
		

			for tab in tabs: 
				ActionChains(self.driver).send_keys(KEY_MENU).perform();
				self.driver.find_elements_by_link_text(name)[0].click()
				self.waitForUrl(ACT_URL_NETW)
				self.driver.find_elements_by_link_text(tab)[0].click()
				ActionChains(self.driver).send_keys(KEY_BACK).perform();
		
				self.waitForUrl(ACT_URL_MAIN)

	def test_startStop(self):
		NUM_TEST=10
				
		#get main activity
		self.driver.get('and-activity://telecom.wi2meUser.Wi2MeUserActivity')
		self.assertTrue("and-activity://Wi2MeUserActivity" in self.driver.current_url)

		button = self.driver.find_element_by_id('btnstart')
		
		self.assertTrue(button.text, "Start");

		for i in range(NUM_TEST):
			button.click()
			#Repick the button to get the updated object
			button = self.driver.find_element_by_id('btnstop')
			
			self.assertTrue(button.text, "Stop");

			button.click()
			#Repick the button to get the updated object
			button = self.driver.find_element_by_id('btnstart')
			self.assertTrue(button.text, "Start");


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
	unittest.main()
