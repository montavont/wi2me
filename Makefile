all: buildgradle

clean:
	./gradlew clean

buildgradle:
	./gradlew build

upload.debug.25:
	adb install -r Wi2MeRecherche/build/outputs/apk/Wi2MeRecherche-minApi25-debug.apk

dbg: buildgradle upload.debug.25
