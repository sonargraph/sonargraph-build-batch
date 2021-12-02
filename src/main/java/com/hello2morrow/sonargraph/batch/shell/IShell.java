package com.hello2morrow.sonargraph.batch.shell;

import java.io.File;
import java.util.List;

public interface IShell
{
    enum OS
    {
        WINDOWS,
        UNIX
    }

    List<String> execute(final String cmd, final File workingDirectory) throws Exception;

    OS getOs();

}