package com.hello2morrow.sonargraph.batch.commands;

import java.io.File;
import java.io.IOException;

final class SonargraphInstallationUtility
{
    private static final String OSGI_JAR_PREFIX = "org.eclipse.osgi_";
    private static final String CLIENT_JAR_PREFIX = "com.hello2morrow.sonargraph.build.client_";
    private static final String JAR_POSTFIX = ".jar";

    private SonargraphInstallationUtility()
    {
        super();
    }

    static File getSonargraphBuildClientJar(final File installationDirectory) throws IOException
    {
        assert installationDirectory != null : "Parameter 'installationDirectory' of method 'detectSonargraphVersion' must not be null";

        if (!installationDirectory.exists() || !installationDirectory.isDirectory())
        {
            throw new IOException("Sonargraph installation directory does not exist.");
        }

        final File clientDir = new File(installationDirectory, "client");
        if (!clientDir.exists() || !clientDir.isDirectory())
        {
            throw new IOException("Not a Sonargraph installation directory. Sub directory 'client' missing");
        }

        final File[] clientJars = clientDir.listFiles((dir, name) -> name.startsWith(CLIENT_JAR_PREFIX) && name.endsWith(JAR_POSTFIX));
        if (clientJars.length != 1)
        {
            throw new IOException("Not a valid Sonargraph installation directory. Expected 1 file starting with '" + CLIENT_JAR_PREFIX
                    + "', but found " + clientJars.length);
        }

        return clientJars[0];
    }

    static File getOsgiJar(final File installationDirectory) throws IOException
    {
        assert installationDirectory != null : "Parameter 'installationDirectory' of method 'getOsgiJar' must not be null";

        if (!installationDirectory.exists() || !installationDirectory.isDirectory())
        {
            throw new IOException("Sonargraph installation directory does not exist.");
        }

        final File pluginsDir = new File(installationDirectory, "plugins");
        if (!pluginsDir.exists() || !pluginsDir.isDirectory())
        {
            throw new IOException("Not a Sonargraph installation directory. Sub directory 'plugins' missing");
        }

        final File[] osgiJars = pluginsDir.listFiles((dir, name) -> name.startsWith(OSGI_JAR_PREFIX) && name.endsWith(JAR_POSTFIX));
        if (osgiJars.length != 1)
        {
            throw new IOException("Not a valid Sonargraph installation directory. Expected 1 file starting with '" + CLIENT_JAR_PREFIX
                    + "', but found " + osgiJars.length);
        }

        return osgiJars[0];
    }
}