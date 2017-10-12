all: buildgradle

clean:
	./gradlew clean

buildgradle:
	./gradlew build

upload.debug:
	adb install -r  ./Wi2MeRecherche/build/outputs/apk/Wi2MeRecherche-debug.apk

dbg: buildgradle upload.debug
