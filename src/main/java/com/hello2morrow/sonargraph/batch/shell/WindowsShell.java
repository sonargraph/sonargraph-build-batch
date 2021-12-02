/*
 * Sonargraph Integration Access
 * Copyright (C) 2016-2021 hello2morrow GmbH
 * mailto: support AT hello2morrow DOT com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hello2morrow.sonargraph.batch.shell;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WindowsShell implements IShell
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsShell.class);

    private final Charset m_charset;

    public WindowsShell(final Charset charset)
    {
        assert charset != null : "Parameter 'charset' of method 'Command' must not be null";
        m_charset = charset;
    }

    @Override
    public List<String> execute(final String cmd, final File workingDirectory) throws Exception
    {
        assert cmd != null && !cmd.isEmpty() : "Parameter 'cmd' of method 'executeCommand' must not be empty";
        assert workingDirectory != null : "Parameter 'workingDirectory' of method 'executeCommand' must not be null";

        final List<String> output = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        //Execute on Windows shell
        final String command = "cmd.exe /c " + cmd;

        final ProcessBuilder builder = new ProcessBuilder(command.split(" "));

        if (workingDirectory != null)
        {
            builder.directory(workingDirectory);
        }

        Process process = null;
        LOGGER.info("Executing: {}", command);
        process = builder.start();

        final ProcessStream outputStream = new ProcessStream("STANDARD OUT", process.getInputStream(), m_charset);
        final ProcessStream errorStream = new ProcessStream("STANDARD ERR", process.getErrorStream(), m_charset);
        outputStream.start();
        errorStream.start();
        // process.waitFor() returns immediately with exit code 0 for 'wrapper' executables that just spawn another executable
        process.waitFor();
        // wait until the streams are closed, which surprisingly works for 'wrapper' executables
        outputStream.join();
        errorStream.join();

        // now we maybe get the 'real' exit code, or not...
        final int exitValue = process.exitValue();
        output.addAll(outputStream.getOutput());
        errors.addAll(errorStream.getOutput());

        if (exitValue != 0)
        {
            if (errors.size() > 0)
            { //On Windows, processes like MSBuild report errors on standard out
                output.addAll(errors);
            }
            throw new IOException(output.stream().collect(Collectors.joining("\n")));
        }

        LOGGER.debug("Output: {}", output.stream().collect(Collectors.joining("\n")));
        return output;
    }

    public Charset getCharset()
    {
        return m_charset;
    }

    @Override
    public OS getOs()
    {
        return OS.WINDOWS;
    }
}