package com.hello2morrow.sonargraph.batch.shell;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractShell implements IShell
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractShell.class);
    private final OS m_os;
    private final Charset m_charset;

    protected AbstractShell(final OS os, final Charset charset)
    {
        assert os != null : "Parameter 'os' of method 'AbstractShell' must not be null";
        assert charset != null : "Parameter 'charset' of method 'AbstractShell' must not be null";

        m_os = os;
        m_charset = charset;
    }

    @Override
    public final List<String> execute(final String cmd, final File workingDirectory) throws Exception
    {
        assert cmd != null && !cmd.isEmpty() : "Parameter 'cmd' of method 'executeCommand' must not be empty";
        assert workingDirectory != null : "Parameter 'workingDirectory' of method 'executeCommand' must not be null";

        final List<String> output = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        final String command = createCommand(cmd);

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

    protected String createCommand(final String cmd)
    {
        return cmd;
    }

    @Override
    public final OS getOs()
    {
        return m_os;
    }
}