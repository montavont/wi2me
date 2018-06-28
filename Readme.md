# wi2me

This project contains the source of the wireless network android discovery application wi2me, as well as the tools used to analyse its results. Two versions of thie application are available :
* Wi2Me Research : an highly customizable application for scientific experiment
* Wi2Me User : an user friendly application automatically scanning, associating and authentifying the phone to wifi community networks.

Both versions rely on a common implementation base, stored as an android library in the Wi2MeCore folder.

## Getting Started

### Installing

This android application is build in the standard manner, using gradle.

```
./gradlew build
```

This will cause the wi2me core library, and both version of the application to be build, each case in two android versions (pre and post SDK version 25).
The apks can be located in Wi2Me[Recherche, User]/build/outputs/apk/

## License

This project is licensed under the GNU GENERAL PUBLIC LICENSE Version 3 - see the [LICENSE.md](LICENSE.md) file for details

## Publication

The initial description of the wi2me application is available in the following [paper](https://ieeexplore.ieee.org/document/6258143/). If using wi2me in your own scientific publication, please cite this paper as reference.
