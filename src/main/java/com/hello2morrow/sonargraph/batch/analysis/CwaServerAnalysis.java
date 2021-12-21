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
package com.hello2morrow.sonargraph.batch.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hello2morrow.sonargraph.batch.commands.GitCommands;
import com.hello2morrow.sonargraph.batch.commands.MavenCommands;
import com.hello2morrow.sonargraph.batch.commands.SonargraphCommand;
import com.hello2morrow.sonargraph.batch.configuration.ConfigurationReader;
import com.hello2morrow.sonargraph.batch.configuration.Props;
import com.hello2morrow.sonargraph.batch.shell.IShell;
import com.hello2morrow.sonargraph.batch.shell.ShellFactory;

/**
 * This class executes the analysis for the German Corona-Warn-App server, available at https://github.com/corona-warn-app/cwa-server.
 *
 * It checks out the Git repo, pulls the latest changes and extracts the existing tagged commits. <br>
 * For each tag, the commit is checked out, Maven is called to compile the code and SonargraphBuild is started. <br>
 * The XML report and snapshot is pushed to a local Sonargraph-Enterprise server.
 */
public class CwaServerAnalysis
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CwaServerAnalysis.class);

    private final Configuration m_configuration;
    private final Charset m_charset;

    private final String m_activationCode;

    private CwaServerAnalysis(final Configuration configuration, final String activationCode)
    {
        assert configuration != null : "Parameter 'configuration' of method 'Execution' must not be null";
        assert activationCode != null && activationCode.length() > 0 : "Parameter 'activationCode' of method 'CwaServerAnalysis' must not be empty";

        m_configuration = configuration;
        m_activationCode = activationCode;
        final String charsetName = m_configuration.getString(Props.SHELL_CHARSET.getPropertyName());
        m_charset = charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    public static void main(final String[] args)
    {
        final String propertyFileName = args.length > 0 ? args[0] : null;
        //FIXME [IK] Should be solved like in the AnalyzeMavenArtifact class
        final String activationCode = args[1];
        final Configuration props = ConfigurationReader.read(propertyFileName);
        if (props != null)
        {
            final CwaServerAnalysis execution = new CwaServerAnalysis(props, activationCode);
            execution.run();
        }
    }

    private void run()
    {
        final String repoPath = m_configuration.getString(Props.REPO_DIRECTORY.getPropertyName());
        final File repoDir = new File(repoPath);
        if (!repoDir.exists() || !repoDir.isDirectory())
        {
            throw new RuntimeException("Repository directory '" + repoDir.getAbsolutePath() + "' does not exist.");
        }

        final IShell shell = ShellFactory.create(m_charset);
        final String branchName = m_configuration.getString(Props.BRANCH_NAME.getPropertyName());
        if (!GitCommands.checkoutBranchAndGetLatest(shell, repoDir, branchName))
        {
            throw new RuntimeException("Failed to checkout branch '" + branchName + "'");
        }

        final String analysisPath = m_configuration.getString(Props.ANALYSIS_DIRECTORY.getPropertyName());
        final File analysisDir = new File(analysisPath);
        if (!analysisDir.exists() || !analysisDir.isDirectory())
        {
            analysisDir.mkdirs();
        }

        final File commitsAndTagsFile = new File(analysisDir, "commits_and_tags.txt");
        final List<Pair<String, String>> commitsAndTags;
        if (m_configuration.getBoolean(Props.WRITE_TAGS_FILE.getPropertyName(), true))
        {
            final List<String> excludedTags = m_configuration.getList(String.class, Props.EXCLUDED_TAG_PARTS.getPropertyName());
            final Set<String> excludedTagParts = new HashSet<>(excludedTags);
            commitsAndTags = GitCommands.createListOfTags(shell, repoDir, commitsAndTagsFile, excludedTagParts);
            if (commitsAndTags == null)
            {
                throw new RuntimeException("Failed to create list of commits and tags for repository at: " + repoPath);
            }
            else if (commitsAndTags.isEmpty())
            {
                throw new RuntimeException("No tags found for repository at: " + repoPath);
            }
            LOGGER.info("{} commits with tags for repo '" + repoPath + "' written to file {}", commitsAndTags.size(),
                    commitsAndTagsFile.getAbsolutePath());
        }
        else
        {
            //If commits and tags have been manually created, they are simply read from file
            commitsAndTags = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(commitsAndTagsFile)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    final String[] parts = line.split(" ");
                    if (parts.length == 2)
                    {
                        commitsAndTags.add(new ImmutablePair<>(parts[0], parts[1]));
                    }
                }
            }
            catch (final IOException ex)
            {
                LOGGER.error("Failed to read commits and tags from file for repo: " + repoPath);
                throw new RuntimeException(ex);
            }
        }

        String baselineReportPath = "";
        //Execute analysis for all detected tagged commits
        for (int i = 0; i < commitsAndTags.size(); i++)
        {
            final Pair<String, String> next = commitsAndTags.get(i);
            final String commit = next.getKey();
            final String tag = next.getValue();
            try
            {
                final long start = System.currentTimeMillis();
                LOGGER.info("[{} of {}] Analysis of tag {}", i + 1, commitsAndTags.size(), tag);
                baselineReportPath = analyseCommit(shell, commit, tag, repoDir, analysisDir, baselineReportPath);
                LOGGER.info("Finished after {} ms.\n", (System.currentTimeMillis() - start));
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to run analysis for tag '" + tag + "', '" + commit + "'");
                logExceptionToFile(analysisDir, commit, tag, e);
            }
            finally
            {
                try
                {
                    GitCommands.reset(shell, repoDir);
                }
                catch (final Exception e)
                {
                    LOGGER.error("Failed to reset repo after analysis of tag '{}'", tag);
                    throw new RuntimeException(e);
                }
            }
            LOGGER.info("----------------------");
        }
    }

    private void logExceptionToFile(final File analysisDir, final String commit, final String tag, final Exception exception)
    {
        assert analysisDir != null : "Parameter 'analysisDir' of method 'logExceptionToFile' must not be null";
        assert commit != null && commit.length() > 0 : "Parameter 'commit' of method 'logExceptionToFile' must not be empty";
        assert tag != null && tag.length() > 0 : "Parameter 'tag' of method 'logExceptionToFile' must not be empty";
        assert exception != null : "Parameter 'exception' of method 'logExceptionToFile' must not be null";

        final File logDir = new File(analysisDir + "/logs");
        if (!logDir.exists())
        {
            logDir.mkdir();
        }
        final File logFile = new File(logDir, System.currentTimeMillis() + "_" + tag + "_" + commit + ".log");
        try (Writer writer = new FileWriter(logFile))
        {
            writer.append(exception.getMessage());
            writer.flush();
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to write exception to log file", e);
            LOGGER.error("Exception that should have been logged: ", e);
        }
    }

    private String analyseCommit(final IShell shell, final String commit, final String tag, final File repoDir, final File analysisDir,
            final String baselineReportPath) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'runAnalysisForCommit' must not be null";
        assert commit != null : "Parameter 'commit' of method 'runAnalysisForCommit' must not be null";
        assert tag != null : "Parameter 'tag' of method 'runAnalysisForCommit' must not be null";
        assert repoDir != null : "Parameter 'repoDir' of method 'runAnalysisForCommit' must not be null";
        assert analysisDir != null : "Parameter 'analysisDir' of method 'runAnalysisForCommit' must not be null";
        assert baselineReportPath != null : "Parameter 'baselineReportPath' of method 'runAnalysisForCommit' must not be null";

        try
        {
            GitCommands.checkoutCommit(shell, commit, repoDir);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to checkout commit for tag '" + tag + "'", e);
            throw e;
        }

        final Pair<String, String> timestamps;
        try
        {
            timestamps = GitCommands.getTimestampOfCommit(shell, commit, repoDir);
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to get timestamps for commit for tag '{}'", tag);
            throw e;
        }

        try
        {
            final String javaHomeForMvn = m_configuration.getString(Props.JAVA_HOME_FOR_MVN.getPropertyName());
            MavenCommands.executeMvn(shell, repoDir, javaHomeForMvn, "mvn clean compile test-compile -Dcheckstyle.skip=true");
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to run Maven build for tag '{}'", tag);
            throw e;
        }

        final String config = new File(m_configuration.getString(Props.CONFIG_FILE.getPropertyName())).getAbsolutePath();
        final String systemDirectory = new File(m_configuration.getString(Props.SONARGRAPH_SYSTEM_DIRECTORY.getPropertyName())).getAbsolutePath();

        final String systemName = m_configuration.getString(Props.NAME.getPropertyName());
        return SonargraphCommand.createReport(shell, systemName, commit, timestamps, tag, analysisDir, baselineReportPath, m_activationCode,
                m_configuration, config, systemDirectory);
    }
}