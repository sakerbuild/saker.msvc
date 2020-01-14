# saker.msvc

![Build status](https://img.shields.io/azure-devops/build/sakerbuild/89658990-239d-4856-973f-f880318c937c/9/master) [![Latest version](https://mirror.nest.saker.build/badges/saker.msvc/version.svg)](https://nest.saker.build/package/saker.msvc "saker.msvc | saker.nest")

Build tasks for using the MSVC toolchain with the [saker.build system](https://saker.build). The project implements C/C++ compilation and linking support for the build system. It supports proper incremental and distributed compilation of the source files.

See the [documentation](https://saker.build/saker.msvc/doc/) for more information.

## Build instructions

The library uses the [saker.build system](https://saker.build) for building. Use the following command to build the project:

```
java -jar path/to/saker.build.jar -bd build compile saker.build
```

## License

The source code for the project is licensed under *GNU General Public License v3.0 only*.

Short identifier: [`GPL-3.0-only`](https://spdx.org/licenses/GPL-3.0-only.html).

Official releases of the project (and parts of it) may be licensed under different terms. See the particular releases for more information.
