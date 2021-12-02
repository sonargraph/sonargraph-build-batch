package com.hello2morrow.sonargraph.batch.shell;

import java.nio.charset.Charset;

public final class ShellFactory
{
    public static IShell create(final Charset charset)
    {
        final String osName = System.getProperty("os.name", "unknown").trim().toLowerCase();
        if (osName.indexOf("windows") >= 0)
        {
            return new WindowsShell(charset);
        }

        throw new RuntimeException("Unsupported operating system " + osName);
    }
}