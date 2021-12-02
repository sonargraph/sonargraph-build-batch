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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import com.hello2morrow.sonargraph.batch.configuration.Props;
import com.hello2morrow.sonargraph.batch.shell.IShell;

public final class SonargraphCommand
{
    private SonargraphCommand()
    {
        super();
    }

    public static String createReport(final IShell shell, final String commit, final Pair<String, String> timestamps, final String tag,
            final File analysisDir, final String baselineReportPath, final Configuration configuration, final String configFile,
            final String sonargraphSystemPath) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'executeSonargraph' must not be null";
        assert commit != null : "Parameter 'commit' of method 'executeSonargraph' must not be null";
        assert timestamps != null : "Parameter 'timestamps' of method 'executeSonargraph' must not be null";
        assert configuration != null : "Parameter 'configuration' of method 'createReport' must not be null";
        assert configFile != null : "Parameter 'configFile' of method 'createReport' must not be null";
        assert sonargraphSystemPath != null : "Parameter 'sonargraphSystemPath' of method 'createReport' must not be null";

        final String activationCode = System.getProperty("sonargraph.activationCode");
        if (activationCode == null)
        {
            throw new RuntimeException(
                    "Missing Sonargraph activation code. Must be set as System property (-Dsonargraph.activationCode=xxxx-xxxx-xxxx-xxxx)!");
        }

        final String instDirectory = configuration.getString(Props.INST_DIRECTORY.getPropertyName());
        final String buildClientJar = SonargraphInstallationUtility.getSonargraphBuildClientJar(new File(instDirectory)).getAbsolutePath();
        final String osgiJar = SonargraphInstallationUtility.getOsgiJar(new File(instDirectory)).getAbsolutePath();

        final String systemName = configuration.getString(Props.NAME.getPropertyName());

        final String uploadHostUrl = configuration.getString(Props.UPLOAD_HOST_URL.getPropertyName());
        final String clientKey = configuration.getString(Props.CLIENT_KEY.getPropertyName());

        final String reportDirectory = new File(analysisDir, "reports").getAbsolutePath();
        final String reportIdentifier;
        if (timestamps.getKey().equals(commit))
        {
            reportIdentifier = timestamps.getKey() + "-" + tag;
        }
        else
        {
            reportIdentifier = timestamps.getKey() + "-" + commit;
        }

        final String reportFileName = systemName + "-" + reportIdentifier;

        final StringBuilder commandString = new StringBuilder();
        commandString.append("java -ea -cp ").append(buildClientJar).append(";").append(osgiJar)
                .append(" com.hello2morrow.sonargraph.build.client.SonargraphBuildRunner ");
        commandString.append(configFile);
        commandString.append(" activationCode=").append(activationCode);
        commandString.append(" installationDirectory=").append(instDirectory);

        commandString.append(" systemDirectory=").append(sonargraphSystemPath);
        commandString.append(" reportDirectory=").append(reportDirectory);
        commandString.append(" reportFileName=").append(reportFileName);
        commandString.append(" reportType=standard reportFormat=xml,html");

        commandString.append(" snapshotDirectory=").append(new File(analysisDir, "snapshots").getAbsolutePath());
        commandString.append(" snapshotFileName=").append(systemName).append("-").append(timestamps.getKey()).append("-").append(commit);
        if (baselineReportPath != null && baselineReportPath.trim().length() > 0)
        {
            commandString.append(" baselineReportPath=").append(baselineReportPath);
        }

        commandString.append(" logFile=").append(new File(new File(analysisDir, "logs"), "sg-build_" + commit + ".log").getAbsolutePath());
        commandString.append(" uploadHosturl=").append(uploadHostUrl).append(" clientKey=").append(clientKey).append(" commitId=").append(commit)
                .append(" timestamp=").append(timestamps.getValue());
        commandString.append(" version=").append(tag);

        shell.execute(commandString.toString(), analysisDir);

        return new File(reportDirectory, reportFileName).getAbsolutePath() + ".xml";
    }
}