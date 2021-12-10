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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hello2morrow.sonargraph.batch.commands.SonargraphCommand;
import com.hello2morrow.sonargraph.batch.configuration.ConfigurationReader;
import com.hello2morrow.sonargraph.batch.configuration.Props;
import com.hello2morrow.sonargraph.batch.maven.MavenRepo;
import com.hello2morrow.sonargraph.batch.shell.IShell;
import com.hello2morrow.sonargraph.batch.shell.ShellFactory;

/**
 * This class executes the analysis for a Maven artifact.
 *
 * The available releases are queried from Maven repo, and classes and sources jars are downloaded. These JARs are analyzed by Sonargraph-Build and
 * the resulting report and snapshot are uploaded to a local instance of Sonargraph-Enterprise.
 */
public final class AnalyzeMavenArtifact
{
    private static final String VERSION_TIME_SEPARATOR = " -- ";

    private static final Logger LOGGER = LoggerFactory.getLogger(CwaServerAnalysis.class);

    private final Charset m_charset;

    private final String m_artifactId;
    private final String m_groupId;
    private final Configuration m_configuration;
    private final boolean m_writeVersionsFile;

    private AnalyzeMavenArtifact(final String groupId, final String artifactId, final Configuration configuration,
            final boolean writeVersionsFile)
    {
        assert groupId != null && groupId.length() > 0 : "Parameter 'groupId' of method 'RunAnalysisForMavenBundle' must not be empty";
        assert artifactId != null && artifactId.length() > 0 : "Parameter 'artifactId' of method 'RunAnalysisForMavenBundle' must not be empty";
        assert configuration != null : "Parameter 'configuration' of method 'Execution' must not be null";

        m_groupId = groupId;
        m_artifactId = artifactId;
        m_configuration = configuration;
        m_writeVersionsFile = writeVersionsFile;

        final String charsetName = m_configuration.getString(Props.SHELL_CHARSET.getPropertyName());
        m_charset = charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    /**
     * Expected arguments:
     * <ol>
     * <li>groupId: Maven group id</li>
     * <li>artifactId: Maven artifact id</li>
     * <li>propertiesFile: Properties file containing further configuration properties.</li>
     * <li>writeVersionsFile: optional. If provided, the versions are retrieved and written to disk. Leave it out, if you need to tweak the versions
     * manually.
     * </ol>
     *
     * Specify the activationCode as VM property: -Dsonargraph.activationCode=XXXX-XXXX-XXXX-XXXX
     */
    public static void main(final String[] args)
    {
        if (args.length < 3)
        {
            throw new IllegalArgumentException("Expected arguments: <groupId> <artifactId> <propertiesFilePath> [writeVersionsFile]");
        }

        final String groupId = args[0];
        final String artifactId = args[1];
        final String propertyFileName = args[2];
        final boolean writeVersionsFile = args.length > 3 && args[3].equals("writeVersionsFile");

        final Configuration props = ConfigurationReader.read(propertyFileName);
        if (props != null)
        {
            final AnalyzeMavenArtifact execution = new AnalyzeMavenArtifact(groupId, artifactId, props, writeVersionsFile);
            try
            {
                execution.run();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes the analysis:
     * <ol>
     * <li>Does first some setup work like retrieving the available versions, setting up the sample directory, copying the default Sonargraph system
     * and change its id.</li>
     * <li>For each version, download jar + sources.jar and execute Sonargraph-Build.</li>
     * </ol>
     *
     * @throws IOException
     */
    private void run() throws IOException
    {
        final IShell shell = ShellFactory.create(m_charset);
        final String basePath = m_configuration.getString(Props.BASE_DIRECTORY.getPropertyName());
        if (basePath == null || basePath.trim().isEmpty())
        {
            throw new RuntimeException("Missing configuration for '" + Props.BASE_DIRECTORY.getPropertyName() + "'");
        }
        final File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory())
        {
            baseDir.mkdirs();
        }

        final File projectDir = new File(baseDir, m_artifactId);
        if (!projectDir.exists() || !projectDir.isDirectory())
        {
            projectDir.mkdirs();
        }

        final List<Pair<String, Date>> versionsAndDates = processVersions(shell, projectDir);
        LOGGER.info("Processing {} versions", versionsAndDates.size());
        LOGGER.debug("Versions: \n{}", versionsAndDates.stream().map(v -> v.getLeft()).collect(Collectors.joining("\n")));

        final File samplesProjectsDirectory = new File(projectDir, "sampleProjects");
        if (!samplesProjectsDirectory.exists() || !samplesProjectsDirectory.isDirectory())
        {
            samplesProjectsDirectory.mkdir();
        }

        final File sample = new File(projectDir, "sample");
        if (!sample.exists() || !sample.isDirectory())
        {
            sample.mkdir();
        }

        final File sourceDirectory = new File("./src/test/sample");
        copyDirectory(sourceDirectory.getAbsolutePath(), sample.getAbsolutePath());
        final String sonargraphSystemPath = adjustSample(sample, m_groupId, m_artifactId);

        final File sonargraphSystemDir = new File(sonargraphSystemPath);
        if (!sonargraphSystemDir.exists() || !sonargraphSystemDir.isDirectory())
        {
            throw new RuntimeException("Not a Sonargraph system directory: " + sonargraphSystemDir.getAbsolutePath());
        }

        final File startupXml = new File(sample, "startup.xml");
        if (!startupXml.exists())
        {
            throw new RuntimeException("startup.xml does not exist at: " + startupXml.getAbsolutePath());
        }

        String baselineReportPath = "";
        int i = 1;
        for (final Pair<String, Date> next : versionsAndDates)
        {
            LOGGER.info("\n ---- Processing {} of {} ---", i++, versionsAndDates.size());
            final String version = next.getLeft();
            final Date date = next.getRight();

            //create directory matching version
            final File projectVersionDir = new File(samplesProjectsDirectory, version);
            if (!projectVersionDir.exists())
            {
                projectVersionDir.mkdir();
            }

            try
            {
                downloadJarsFromMavenCentral(projectVersionDir, version);
            }
            catch (final IOException ex)
            {
                LOGGER.error("Failed to download files for version " + version, ex);
                continue;
            }

            //copy Sonargraph system to sample directory
            final File systemDirectory = new File(projectVersionDir, sonargraphSystemDir.getName());
            systemDirectory.mkdir();
            final File sonargraphFile = new File(sonargraphSystemDir, "system.sonargraph");
            copyToDir(sonargraphFile, systemDirectory);

            copyToDir(startupXml, projectVersionDir);
            final File targetStartupXml = new File(projectVersionDir, startupXml.getName());

            final Pair<String, String> timestamps = createTimestamps(date);
            try
            {
                baselineReportPath = SonargraphCommand.createReport(shell, m_artifactId, timestamps.getLeft(), timestamps, version, projectDir,
                        baselineReportPath, m_configuration, targetStartupXml.getAbsolutePath(), systemDirectory.getAbsolutePath());
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute Sonargraph for version " + version, e);
            }
        }
    }

    private void downloadJarsFromMavenCentral(final File projectVersionDir, final String version) throws IOException
    {
        assert projectVersionDir != null : "Parameter 'projectVersionDir' of method 'downloadJarsFromMavenCentral' must not be null";
        assert version != null && version.length() > 0 : "Parameter 'version' of method 'downloadJarsFromMavenCentral' must not be empty";

        final String repoUrl = m_configuration.getString(Props.MAVEN_REPO_URL.getPropertyName());
        if (repoUrl == null)
        {
            throw new RuntimeException("Missing configuration property '" + Props.MAVEN_REPO_URL.getPropertyName() + "'");
        }

        final StringBuilder url = new StringBuilder(repoUrl);
        url.append(m_groupId.replace(".", "/")).append("/");
        url.append(m_artifactId).append("/");
        url.append(version);

        url.append("/").append(m_artifactId).append("-").append(version);

        downloadFile(url.toString() + ".jar", new File(projectVersionDir, "classes.jar"));
        downloadFile(url.toString() + "-sources.jar", new File(projectVersionDir, "sources.jar"));
    }

    private void downloadFile(final String url, final File targetFile) throws IOException
    {
        try (final InputStream in = new URL(url).openStream();)
        {
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Successfully downloaded {}", url);
        }
    }

    private String adjustSample(final File sampleDir, final String groupId, final String artifactId) throws IOException
    {
        assert sampleDir != null : "Parameter 'sampleDir' of method 'adjustSample' must not be null";
        assert groupId != null : "Parameter 'groupId' of method 'adjustSample' must not be null";
        assert artifactId != null : "Parameter 'artifactId' of method 'adjustSample' must not be null";

        final String samplePath = sampleDir.getAbsolutePath();
        final Path sonargraphDir = Paths.get(samplePath, artifactId + ".sonargraph");
        if (!sonargraphDir.toFile().exists())
        {
            Files.move(Paths.get(samplePath, "Sonargraph-System.sonargraph"), sonargraphDir, StandardCopyOption.REPLACE_EXISTING);
        }
        final File sonargraphSystemFile = new File(sonargraphDir.toFile(), "system.sonargraph");
        replaceLineInFile(sonargraphSystemFile, "id=\"XXXX\"", "id=\"" + groupId + "." + artifactId + "\"");

        return sonargraphDir.toAbsolutePath().toString();
    }

    private static void copyDirectory(final String sourceDirectoryLocation, final String destinationDirectoryLocation) throws IOException
    {
        Files.walk(Paths.get(sourceDirectoryLocation)).forEach(source ->
        {
            final Path destination = Paths.get(destinationDirectoryLocation, source.toString().substring(sourceDirectoryLocation.length()));
            if (!destination.toFile().exists())
            {
                try
                {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (final IOException e)
                {
                    LOGGER.error("Failed to copy", e);
                }
            }
        });
    }

    private List<Pair<String, Date>> processVersions(final IShell shell, final File analysisDirectory)
    {
        assert shell != null : "Parameter 'shell' of method 'processVersions' must not be null";
        assert analysisDirectory != null : "Parameter 'analysisDirectory' of method 'processVersions' must not be null";

        final Path versionsFile = Paths.get(analysisDirectory.getAbsolutePath(), "versionsAndTimes.txt");
        final List<Pair<String, Date>> versionsAndDates;
        if (m_writeVersionsFile || m_configuration.getBoolean(Props.WRITE_TAGS_FILE.getPropertyName(), false))
        {
            try
            {
                final StringBuilder versionUrl = new StringBuilder(m_configuration.getString(Props.MAVEN_REPO_URL.getPropertyName()));
                versionUrl.append(m_groupId.replaceAll("\\.", "/"));
                versionUrl.append("/").append(m_artifactId).append("/");

                final List<String> excludedVersionParts = m_configuration.getList(String.class, Props.EXCLUDED_TAG_PARTS.getPropertyName());
                final Set<String> excludedTagParts = new HashSet<>(excludedVersionParts);
                versionsAndDates = MavenRepo.getVersions(shell, versionUrl.toString(), excludedTagParts);
            }
            catch (final Exception e)
            {
                throw new RuntimeException("Failed to determine versions", e);
            }
            try
            {
                final List<String> lines = new ArrayList<>();
                for (final Pair<String, Date> next : versionsAndDates)
                {
                    final String line = next.getKey() + VERSION_TIME_SEPARATOR + next.getRight().getTime();
                    lines.add(line);
                }
                Files.write(versionsFile, lines);
            }
            catch (final IOException ex)
            {
                throw new RuntimeException("Failed to write versions to file", ex);
            }
        }
        else
        {
            try
            {
                versionsAndDates = new ArrayList<>();
                for (final String next : Files.readAllLines(versionsFile))
                {
                    final String[] parts = next.split(VERSION_TIME_SEPARATOR);
                    assert parts.length == 2 : "Unexpected number of parts in line '" + next + "': " + parts.length;
                    final String version = parts[0];
                    final Date date = new Date(Long.parseLong(parts[1]));
                    versionsAndDates.add(new ImmutablePair<>(version, date));
                }
            }
            catch (final IOException ex)
            {
                throw new RuntimeException("Failed to read versions file", ex);
            }
        }
        return versionsAndDates;
    }

    /**
     * @param date
     * @return pair consisting of timestamp for file name and iso8601 timestamp.
     */
    private Pair<String, String> createTimestamps(final Date date)
    {
        final SimpleDateFormat fileTimestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        final String fileTimestamp = fileTimestampFormat.format(date);

        final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        final String timestamp = iso8601.format(date);
        return new ImmutablePair<>(fileTimestamp, timestamp);
    }

    private void copyToDir(final File source, final File targetDir) throws IOException
    {
        assert source != null : "Parameter 'source' of method 'copyToDir' must not be null";
        assert targetDir != null : "Parameter 'targetDir' of method 'copyToDir' must not be null";

        final File target = new File(targetDir, source.getName());
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean replaceLineInFile(final File targetPom, final String search, final String replace) throws IOException
    {
        assert targetPom != null : "Parameter 'targetPom' of method 'replaceLineInFile' must not be null";
        assert search != null && search.length() > 0 : "Parameter 'search' of method 'replaceLineInFile' must not be empty";
        assert replace != null && replace.length() > 0 : "Parameter 'replace' of method 'replaceLineInFile' must not be empty";

        boolean anyReplaced = false;
        final List<String> content = new ArrayList<>();
        for (final String next : Files.readAllLines(targetPom.toPath()))
        {
            final String replaced = next.replace(search, replace);
            if (!next.equals(replaced))
            {
                anyReplaced = true;
            }
            content.add(replaced);
        }

        Files.write(targetPom.toPath(), content);

        return anyReplaced;
    }
}