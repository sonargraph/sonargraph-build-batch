package com.hello2morrow.sonargraph.batch.configuration;

public class Platform
{
    private Platform()
    {
        super();
    }

    public static boolean isWindows()
    {
        final String osName = System.getProperty("os.name", "unknown").trim().toLowerCase();
        return osName.indexOf("windows") >= 0;
    }
}