all: R U

userUninstall:
	adb uninstall telecom.wi2meUser

researchUninstall:
	adb uninstall telecom.wi2meRecherche

userAPK:
	cd ../../Wi2MeUser/ && ant debug > /dev/null

researchAPK:
	cd ../../Wi2MeRecherche/ && ant debug > /dev/null

U: userAPK userUninstall
	./Wi2MeUser.py

R: researchAPK researchUninstall
	./Wi2MeRecherche.py


