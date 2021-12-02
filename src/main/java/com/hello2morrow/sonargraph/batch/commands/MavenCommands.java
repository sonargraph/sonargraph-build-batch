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
package com.hello2morrow.sonargraph.batch.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import com.hello2morrow.sonargraph.batch.shell.IShell;
import com.hello2morrow.sonargraph.batch.shell.IShell.OS;

public final class MavenCommands
{
    private MavenCommands()
    {
        super();
    }

    //We need to configure Java home differently for the maven execution.
    public static void executeMvn(final IShell shell, final File repoDir, final String javaHomeForMvn, final String commandLine) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'executeMvn' must not be null";
        assert repoDir != null : "Parameter 'repoDir' of method 'executeMvn' must not be null";
        assert commandLine != null : "Parameter 'commandLine' of method 'executeMvn' must not be null";

        if (javaHomeForMvn != null && javaHomeForMvn.trim().length() > 0)
        {
            final File batFile = new File(repoDir, "executeAnalysis.bat");
            try (PrintWriter writer = new PrintWriter(new FileWriter(batFile)))
            {
                writer.println("set JAVA_HOME=" + javaHomeForMvn);
                if (shell.getOs() == OS.WINDOWS)
                {
                    writer.println("set PATH=%JAVA_HOME%\\bin;%PATH%");
                }
                else
                {
                    writer.println("set PATH=$JAVA_HOME/bin:$PATH");
                }
                writer.println(commandLine);
                writer.flush();
            }
            try
            {
                shell.execute(batFile.getName(), repoDir);
            }
            finally
            {
                batFile.delete();
            }
        }
        else
        {
            shell.execute(commandLine, repoDir);
        }
    }
}