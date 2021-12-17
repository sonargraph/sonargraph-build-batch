package com.hello2morrow.sonargraph.batch.shell;

import java.nio.charset.Charset;

final class UnixShell extends AbstractShell
{
    public UnixShell(final Charset charset)
    {
        super(OS.UNIX, charset);
    }
}