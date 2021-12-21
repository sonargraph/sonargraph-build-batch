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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.hello2morrow.sonargraph.batch.configuration.Version;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeMavenArtifact.class);

    private static final String LAST_VERSION_ANALYZED_FILE_NAME = "lastVersionAnalyzed.txt";
    private static final String VERSION_TIME_SEPARATOR = " -- ";

    private final Charset m_charset;

    private final String m_artifactId;
    private final String m_groupId;
    private final Configuration m_configuration;
    private final String m_activationCode;
    private boolean m_writeVersionsFile = true;
    private final int m_versionsToAnalyze;

    private AnalyzeMavenArtifact(final String groupId, final String artifactId, final Configuration configuration, final String activationCode,
            final boolean writeVersionsFile, final int versionsToAnalyze)
    {
        assert groupId != null && groupId.length() > 0 : "Parameter 'groupId' of method 'RunAnalysisForMavenBundle' must not be empty";
        assert artifactId != null && artifactId.length() > 0 : "Parameter 'artifactId' of method 'RunAnalysisForMavenBundle' must not be empty";
        assert configuration != null : "Parameter 'configuration' of method 'Execution' must not be null";
        assert activationCode != null
                && activationCode.length() > 0 : "Parameter 'activationCode' of method 'AnalyzeMavenArtifact' must not be empty";

        m_groupId = groupId;
        m_artifactId = artifactId;
        m_configuration = configuration;
        m_activationCode = activationCode;
        m_writeVersionsFile = writeVersionsFile;
        m_versionsToAnalyze = versionsToAnalyze;

        final String charsetName = m_configuration.getString(Props.SHELL_CHARSET.getPropertyName());
        m_charset = charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    /**
     * Expected arguments:
     * <ol>
     * <li>groupId: Maven group id</li>
     * <li>artifactId: Maven artifact id</li>
     * <li>propertiesFile: Properties file containing further configuration properties.</li>
     * <li>activationCode: Activation code for Sonargraph-Build
     * <li>writeVersionsFile (optional): If provided, the versions are retrieved and written to disk. Leave it out, if you need to tweak the versions
     * manually.
     * </ol>
     *
     *
     * Specify the activationCode as VM property: -Dsonargraph.activationCode=XXXX-XXXX-XXXX-XXXX
     */
    public static void main(final String[] args)
    {
        if (args.length < 4)
        {
            throw new IllegalArgumentException(
                    "Expected arguments: groupId=<groupId> artifactId=<artifactId> propertiesFilePath=<propertiesFilePath> activationCode=<activation-code> [writeVersionsFile=<true|false>] [numberOfMostRecentVersions=<n>]");
        }

        final long overallStart = System.currentTimeMillis();

        final Map<MavenCommandlineArgument, String> argsMap = MavenCommandlineArgument.parseArgs(args);
        final String groupId = MavenCommandlineArgument.getArgument(argsMap, MavenCommandlineArgument.GROUP_ID);
        final String artifactId = MavenCommandlineArgument.getArgument(argsMap, MavenCommandlineArgument.ARTIFACT_ID);
        final String propertyFileName = MavenCommandlineArgument.getArgument(argsMap, MavenCommandlineArgument.PROPERTY_FILE_NAME);
        final String activationCode = MavenCommandlineArgument.getArgument(argsMap, MavenCommandlineArgument.ACTIVATIONCODE);
        final boolean writeVersionsFile = Boolean
                .parseBoolean(MavenCommandlineArgument.getArgument(argsMap, MavenCommandlineArgument.WRITE_VERSIONS_FILE));
        final int versionsToAnalyze = Integer
                .parseInt(MavenCommandlineArgument.getArgument(argsMap, MavenCommandlineArgument.NUMBER_OF_MOST_RECENT_VERSIONS));

        final Configuration props = ConfigurationReader.read(propertyFileName);
        if (props == null)
        {
            LOGGER.error("Failed to load configuration properties file from " + propertyFileName);
            System.exit(-1);
        }

        try
        {
            final AnalyzeMavenArtifact execution = new AnalyzeMavenArtifact(groupId, artifactId, props, activationCode, writeVersionsFile,
                    versionsToAnalyze);
            execution.run();
        }
        catch (final IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
        LOGGER.info("\n----- Finished analysis after {} seconds", Math.round(System.currentTimeMillis() - overallStart) / 1000.0);

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

        final Pair<Version, Date> lastAnalyzedVersion = determineLastAnalyzedVersion(projectDir);
        final List<Pair<Version, Date>> versionsAndDates = processVersions(shell, projectDir, lastAnalyzedVersion, m_versionsToAnalyze);
        if (versionsAndDates.size() == 0)
        {
            if (lastAnalyzedVersion != null)
            {
                LOGGER.info("Nothing to do. Last analyzed version '{}' is the most recent one for {}.", lastAnalyzedVersion.getLeft().toString(),
                        m_artifactId);
            }
            else
            {
                LOGGER.info("Nothing to do. No versions found for {}.", m_artifactId);
            }
            return;
        }
        LOGGER.info("Processing {} versions", versionsAndDates.size());
        LOGGER.debug("Versions: \n{}", versionsAndDates.stream().map(v -> v.getLeft().toString()).collect(Collectors.joining("\n")));

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

        //FIXME [IK] The sample system and all the necessary files should be loaded from the classpath, so that the analysis can be more easily
        //setup on a build server.
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
        for (final Pair<Version, Date> next : versionsAndDates)
        {
            final long start = System.currentTimeMillis();
            LOGGER.info("\n ---- Processing {} of {} ---", i, versionsAndDates.size());
            final String version = next.getLeft().toString();
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
                        baselineReportPath, m_activationCode, m_configuration, targetStartupXml.getAbsolutePath(), systemDirectory.getAbsolutePath());
                Files.writeString(new File(projectDir, LAST_VERSION_ANALYZED_FILE_NAME).toPath(), createVersionAndDateLine(next));
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute Sonargraph for version " + version, e);
            }

            LOGGER.info("Finished processing {} of {} in {} ms", i++, versionsAndDates.size(), System.currentTimeMillis() - start);
        }
    }

    private Pair<Version, Date> determineLastAnalyzedVersion(final File projectDir)
    {
        assert projectDir != null : "Parameter 'projectDir' of method 'determineLastAanlyzedVersion' must not be null";

        final File lastVersionAnalyzed = new File(projectDir, LAST_VERSION_ANALYZED_FILE_NAME);
        if (!lastVersionAnalyzed.exists())
        {
            return null;
        }

        try
        {
            for (final String next : Files.readAllLines(lastVersionAnalyzed.toPath()))
            {
                return extractVersionAndDateFromLine(next);
            }
        }
        catch (final IOException ex)
        {
            LOGGER.error("Failed to extract last analyzed version from file.", ex);
        }

        return null;
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

    private List<Pair<Version, Date>> processVersions(final IShell shell, final File analysisDirectory, final Pair<Version, Date> lastAnalyzedVersion,
            final int versionsToAnalyze)
    {
        assert shell != null : "Parameter 'shell' of method 'processVersions' must not be null";
        assert analysisDirectory != null : "Parameter 'analysisDirectory' of method 'processVersions' must not be null";

        final Path versionsFile = Paths.get(analysisDirectory.getAbsolutePath(), "versionsAndTimes.txt");
        final List<Pair<Version, Date>> versionsAndDates;
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
                for (final Pair<Version, Date> next : versionsAndDates)
                {
                    lines.add(createVersionAndDateLine(next));
                }
                Files.write(versionsFile, lines);
                LOGGER.debug("Versions written to {}", versionsFile.toString());
            }
            catch (final IOException ex)
            {
                throw new RuntimeException("Failed to write versions to file", ex);
            }
        }
        else
        {
            versionsAndDates = new ArrayList<>();
            if (versionsFile.toFile().exists())
            {
                try
                {
                    for (final String next : Files.readAllLines(versionsFile))
                    {
                        versionsAndDates.add(extractVersionAndDateFromLine(next));
                    }
                }
                catch (final IOException ex)
                {
                    throw new RuntimeException("Failed to read versions file", ex);
                }
            }
            else
            {
                throw new RuntimeException("Expected file does not exist: " + versionsFile.toAbsolutePath());
            }
        }

        if (lastAnalyzedVersion == null)
        {
            if (versionsToAnalyze == -1 //
                    || versionsToAnalyze >= versionsAndDates.size())
            {
                return versionsAndDates;
            }

            return versionsAndDates.subList(versionsAndDates.size() - versionsToAnalyze, versionsAndDates.size());
        }

        final List<Pair<Version, Date>> result = new ArrayList<>();
        for (int i = versionsAndDates.size() - 1; i >= 0; i--)
        {
            final Pair<Version, Date> next = versionsAndDates.get(i);
            if (lastAnalyzedVersion.getLeft().compareTo(next.getLeft()) < 0)
            {
                result.add(next);
            }
            else
            {
                break;
            }
        }
        Collections.reverse(result);
        return result;
    }

    private Pair<Version, Date> extractVersionAndDateFromLine(final String next)
    {
        final String[] parts = next.split(VERSION_TIME_SEPARATOR);
        assert parts.length == 2 : "Unexpected number of parts in line '" + next + "': " + parts.length;
        final Version version = Version.fromString(parts[0]);
        final Date date = new Date(Long.parseLong(parts[1]));
        final Pair<Version, Date> pair = new ImmutablePair<>(version, date);
        return pair;
    }

    private String createVersionAndDateLine(final Pair<Version, Date> versionAndDate)
    {
        assert versionAndDate != null : "Parameter 'versionAndDate' of method 'createVersionAndDateLine' must not be null";
        return String.format("%s%s%s", versionAndDate.getLeft().toString(), VERSION_TIME_SEPARATOR, versionAndDate.getRight().getTime());
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

    private boolean replaceLineInFile(final File file, final String search, final String replace) throws IOException
    {
        assert file != null : "Parameter 'file' of method 'replaceLineInFile' must not be null";
        assert search != null && search.length() > 0 : "Parameter 'search' of method 'replaceLineInFile' must not be empty";
        assert replace != null && replace.length() > 0 : "Parameter 'replace' of method 'replaceLineInFile' must not be empty";

        boolean anyReplaced = false;
        final List<String> content = new ArrayList<>();
        for (final String next : Files.readAllLines(file.toPath()))
        {
            final String replaced = next.replace(search, replace);
            if (!next.equals(replaced))
            {
                anyReplaced = true;
            }
            content.add(replaced);
        }

        Files.write(file.toPath(), content);
        return anyReplaced;
    }
}