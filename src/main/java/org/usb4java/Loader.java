/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.md for licensing information.
 */

package org.usb4java;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

/**
 * Utility class to load native libraries from classpath.
 *
 * @author Klaus Reimer (k@ailis.de)
 */
public final class Loader
{
    /** The temporary directory for native libraries. */
    private static File tmp;

    /** If library is already loaded. */
    private static boolean loaded = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private Loader()
    {
        // Nothing to do here
    }

    /**
     * Returns the operating system name. This could be "linux", "windows" or
     * "macos" or (for any other non-supported platform) the value of the
     * "os.name" property converted to lower case and with removed space
     * characters.
     *
     * @return The operating system name.
     */
    private static String getOS()
    {
        final String os = System.getProperty("os.name").toLowerCase()
            .replace(" ", "");
        if (os.contains("windows"))
        {
            return "win32";
        }
        if (os.equals("macosx") || os.equals("macos"))
        {
            return "darwin";
        }
        return os;
    }

    /**
     * Returns the CPU architecture. This will be "x86" or "x86_64" (Platform
     * names i386 und amd64 are converted accordingly) or (when platform is
     * unsupported) the value of os.arch converted to lower-case and with
     * removed space characters.
     *
     * @return The CPU architecture
     */
    private static String getArch()
    {
        final String arch = System.getProperty("os.arch").toLowerCase()
            .replace(" ", "");
        if (arch.equals("i386"))
        {
            return "x86";
        }
        if (arch.equals("amd64") || arch.equals("x86_64"))
        {
            return "x86-64";
        }
        if (arch.equals("arm64"))
        {
            return "aarch64";
        }
        if (arch.equals("armhf") || arch.equals("aarch32") || arch.equals("armv7l"))
        {
            return "arm";
        }
        return arch;
    }

    /**
     * Returns the shared library extension name.
     *
     * @return The shared library extension name.
     */
    private static String getExt()
    {
        final String os = getOS();
        final String key = "usb4java.libext." + getOS();
        final String ext = System.getProperty(key);
        if (ext != null)
        {
            return ext;
        }
        if (os.equals("linux") || os.equals("freebsd") || os.equals("sunos"))
        {
            return "so";
        }
        if (os.equals("win32"))
        {
            return "dll";
        }
        if (os.equals("darwin"))
        {
            return "dylib";
        }
        throw new LoaderException("Unable to determine the shared library "
            + "file extension for operating system '" + os
            + "'. Please specify Java parameter -D" + key
            + "=<FILE-EXTENSION>");
    }

    /**
     * Creates the temporary directory used for unpacking the native libraries.
     * This directory is marked for deletion on exit.
     *
     * @return The temporary directory for native libraries.
     */
    private static File createTempDirectory()
    {
        // Return cached tmp directory when already created
        if (tmp != null)
        {
            return tmp;
        }

        try
        {
            tmp = Files.createTempDirectory("usb4java").toFile();
            tmp.deleteOnExit();
            return tmp;
        }
        catch (final IOException e)
        {
            throw new LoaderException("Unable to create temporary directory "
                + "for usb4java natives: " + e, e);
        }
    }

    /**
     * Returns the platform name. This could be for example "linux-x86" or
     * "windows-x86_64".
     *
     * @return The architecture name. Never null.
     */
    private static String getPlatform()
    {
        return getOS() + "-" + getArch();
    }

    /**
     * Returns the name of the usb4java native library. This could be
     * "libusb4java.dll" for example.
     *
     * @return The usb4java native library name. Never null.
     */
    private static String getLibName()
    {
        return "libusb4java." + getExt();
    }

    /**
     * Returns the name of the libusb native library. This could be
     * "libusb0.dll" for example or null if this library is not needed on the
     * current platform (Because it is provided by the operating system or
     * statically linked into the libusb4java library).
     *
     * @return The libusb native library name or null if not needed.
     */
    private static String getExtraLibName()
    {
        return null;
    }

    /**
     * Extracts a single library.
     *
     * @param platform
     *            The platform name (For example "linux-x86")
     * @param lib
     *            The library name to extract (For example "libusb0.dll")
     * @return The absolute path to the extracted library.
     */
    private static String extractLibrary(final String platform,
        final String lib)
    {
        // Extract the usb4java library
        final String source = '/'
            + Loader.class.getPackage().getName().replace('.', '/') + '/'
            + platform + "/" + lib;

        // Check if native library is present
        final URL url = Loader.class.getResource(source);
        if (url == null)
        {
            throw new LoaderException("Native library not found in classpath: "
                + source);
        }

        // If native library was found in an already extracted form then
        // return this one without extracting it
        if ("file".equals(url.getProtocol()))
        {
            try
            {
                return new File(url.toURI()).getAbsolutePath();
            }
            catch (final URISyntaxException e)
            {
                // Can't happen because we are not constructing the URI
                // manually. But even when it happens then we fall back to
                // extracting the library.
                throw new LoaderException(e.toString(), e);
            }
        }

        // Extract the library and return the path to the extracted file.
        final File dest = new File(createTempDirectory(), lib);
        try
        {
            final InputStream stream = Loader.class.getResourceAsStream(source);
            if (stream == null)
            {
                throw new LoaderException("Unable to find " + source
                    + " in the classpath");
            }
            try
            {
                Files.copy(stream, dest.toPath());
            }
            finally
            {
                stream.close();
            }
        }
        catch (final IOException e)
        {
            throw new LoaderException("Unable to extract native library "
                + source + " to " + dest + ": " + e, e);
        }

        // Mark usb4java library for deletion
        dest.deleteOnExit();

        return dest.getAbsolutePath();
    }

    /**
     * Loads the libusb native wrapper library. Can be safely called multiple
     * times. Duplicate calls are ignored. This method is automatically called
     * when the {@link LibUsb} class is loaded. When you need to do it earlier
     * (To catch exceptions for example) then simply call this method manually.
     *
     * @throws LoaderException
     *             When loading the native wrapper libraries failed.
     */
    public static synchronized void load()
    {
        // Do nothing if already loaded (or still loading)
        if (loaded)
        {
            return;
        }

        loaded = true;
        final String platform = getPlatform();
        final String lib = getLibName();
        final String extraLib = getExtraLibName();
        if (extraLib != null)
        {
            System.load(extractLibrary(platform, extraLib));
        }
        System.load(extractLibrary(platform, lib));
    }
}
