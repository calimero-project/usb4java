usb4java: USB library for Java based on libusb 1.0
========
_Fork of usb4java/usb4java, using Maven groupId `io.calimero`_.

#### Notable changes
* Add support for Windows and macOS on AArch64 / Apple Silicon
* Add support for resource loading on module path
* Require Java 8 or later
* Improve security for libusb4java temporary directory creation
* Remove the commons-lang3 dependency
* Automatic-Module-Name: org.usb4java
-----

Copyright (C) 2011 Klaus Reimer, k@ailis.de  
Copyright (C) 2013 Luca Longinotti, l@longi.li

See LICENSE.md for licensing information.

This library can be used to access USB devices in Java. It is based on the 
native libusb 1.0 shared library and reflects this API as complete as 
possible. usb4java also implements the javax-usb (JSR80) API. So you can choose 
if you want to access the USB devices via the low-level libusb 1.0 API or 
via the high level javax-usb API.

The native libraries are included in pre-compiled form for Linux, Windows and
Mac OS X in 32 and 64 bit versions. The libraries are automatically extracted
and loaded so you don't need to care about them. 

For more detailed information please visit the [usb4java website](http://usb4java.org/).

