package com.hello2morrow.sonargraph.batch.analysis;

import java.io.File;
import java.io.IOException;
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

import com.hello2morrow.sonargraph.batch.commands.MavenCommands;
import com.hello2morrow.sonargraph.batch.commands.SonargraphCommand;
import com.hello2morrow.sonargraph.batch.configuration.ConfigurationReader;
import com.hello2morrow.sonargraph.batch.configuration.Props;
import com.hello2morrow.sonargraph.batch.maven.MavenRepo;
import com.hello2morrow.sonargraph.batch.shell.IShell;
import com.hello2morrow.sonargraph.batch.shell.ShellFactory;

/**
 * This class executes the analysis for the Hibernate-Core module, which is part of the Hibernate project, available at
 * https://github.com/hibernate/hibernate-orm.
 *
 * The available releases are queried from Maven repo, and a small dummy project is used to download the JAR and sources-JAR. These JARs are analyzed
 * by Sonargraph-Build and the resulting report and snapshot are uploaded to a local instance of Sonargraph-Enterprise.
 */
public final class HibernateCoreAnalysis
{
    private static final String VERSION_TIME_SEPARATOR = " -- ";

    private static final Logger LOGGER = LoggerFactory.getLogger(CwaServerAnalysis.class);

    private final Configuration m_configuration;
    private final Charset m_charset;

    private HibernateCoreAnalysis(final Configuration configuration)
    {
        assert configuration != null : "Parameter 'configuration' of method 'Execution' must not be null";

        m_configuration = configuration;
        final String charsetName = m_configuration.getString(Props.SHELL_CHARSET.getPropertyName());
        m_charset = charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    public static void main(final String[] args)
    {
        final String propertyFileName = args.length > 0 ? args[0] : null;
        final Configuration props = ConfigurationReader.read(propertyFileName);
        if (props != null)
        {
            final HibernateCoreAnalysis execution = new HibernateCoreAnalysis(props);
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

    private void run() throws IOException
    {
        final IShell shell = ShellFactory.create(m_charset);
        final String analysisPath = m_configuration.getString(Props.ANALYSIS_DIRECTORY.getPropertyName());
        if (analysisPath == null || analysisPath.trim().isEmpty())
        {
            throw new RuntimeException("Missing configuration for '" + Props.ANALYSIS_DIRECTORY.getPropertyName() + "'");
        }
        final File analysisDirectory = new File(analysisPath);
        if (!analysisDirectory.exists() || !analysisDirectory.isDirectory())
        {
            analysisDirectory.mkdirs();
        }

        final List<Pair<String, Date>> versionsAndDates = processVersions(shell, analysisDirectory);
        LOGGER.info("Processing {} versions", versionsAndDates.size());
        LOGGER.debug("Versions: \n{}", versionsAndDates.stream().map(v -> v.getLeft()).collect(Collectors.joining("\n")));

        final File samplesDirectory = new File(analysisDirectory, "sample_projects");
        if (!samplesDirectory.exists() || !samplesDirectory.isDirectory())
        {
            samplesDirectory.mkdir();
        }

        final File sample = new File(analysisDirectory, "sample");

        final String group = m_configuration.getString(Props.MAVEN_GROUP_ID.getPropertyName());
        final String artifactId = m_configuration.getString(Props.MAVEN_ARTIFACT_ID.getPropertyName());
        final String mavenRepoHome = m_configuration.getString(Props.MAVEN_REPO_HOME.getPropertyName());
        if (mavenRepoHome == null)
        {
            throw new RuntimeException("Missing configuration property '" + Props.MAVEN_REPO_HOME.getPropertyName() + "'");
        }
        final File directoryInMavenHome = getArtifactDirectory(mavenRepoHome, group, artifactId);

        final String sonargraphSystemPath = m_configuration.getString(Props.SONARGRAPH_SYSTEM_DIRECTORY.getPropertyName());
        if (sonargraphSystemPath == null)
        {
            throw new RuntimeException("Missing configuration property '" + Props.SONARGRAPH_SYSTEM_DIRECTORY.getPropertyName() + "'");
        }

        final File sonargraphSystemDir = new File(sonargraphSystemPath);
        if (!sonargraphSystemDir.exists() || !sonargraphSystemDir.isDirectory())
        {
            throw new RuntimeException("Not a Sonargraph system directory: " + sonargraphSystemDir.getAbsolutePath());
        }

        final String startupXmlPath = m_configuration.getString(Props.CONFIG_FILE.getPropertyName());
        if (startupXmlPath == null)
        {
            throw new RuntimeException("Missing configuration property '" + Props.CONFIG_FILE.getPropertyName() + "'");
        }
        final File startupXml = new File(startupXmlPath);
        if (!startupXml.exists())
        {
            throw new RuntimeException("startup.xml does not exist at: " + startupXmlPath);
        }

        String baselineReportPath = "";
        int i = 1;
        for (final Pair<String, Date> next : versionsAndDates)
        {
            LOGGER.info("\n ---- Processing {} of {} ---", i++, versionsAndDates.size());
            final String version = next.getLeft();
            final Date date = next.getRight();

            //create directory matching version
            final File projectDir = new File(samplesDirectory, version);
            if (!projectDir.exists())
            {
                projectDir.mkdir();
            }

            //copy sample pom.xml
            final File sourcePom = new File(sample, "pom.xml");
            final File targetPom = new File(projectDir, "pom.xml");
            copyToDir(sourcePom, projectDir);

            //replace version
            if (!replaceLineInFile(targetPom, "<hibernate.version>5.5.2.Final</hibernate.version>",
                    "<hibernate.version>" + version + "</hibernate.version>"))
            {
                LOGGER.error("Failed to replace version information in file {}", targetPom);
                continue;
            }

            try
            {
                MavenCommands.executeMvn(shell, projectDir, null, "mvn compile dependency:sources");
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute Maven for version '" + version + "'", e);
                continue;
            }

            final File versionDirInJavaHome = new File(directoryInMavenHome, version);
            if (!versionDirInJavaHome.exists() || !versionDirInJavaHome.isDirectory())
            {
                LOGGER.error("Missing directory in Maven home for version '{}'", version);
                continue;
            }

            //copy jar and sources-jar from maven repo to sample directory and rename them to default
            final File jar = new File(versionDirInJavaHome, artifactId + "-" + version + ".jar");
            final File targetJar = new File(projectDir, artifactId + ".jar");
            copyToFile(jar, targetJar);
            final File sourcesJar = new File(versionDirInJavaHome, artifactId + "-" + version + "-sources.jar");
            final File targetSourcesJar = new File(projectDir, artifactId + "-sources.jar");
            copyToFile(sourcesJar, targetSourcesJar);

            //copy Sonargraph system to sample directory
            final File systemDirectory = new File(projectDir, sonargraphSystemDir.getName());
            systemDirectory.mkdir();
            final File sonargraphFile = new File(sonargraphSystemDir, "system.sonargraph");
            copyToDir(sonargraphFile, systemDirectory);

            //copy startup config
            copyToDir(startupXml, projectDir);
            final File targetStartupXml = new File(projectDir, startupXml.getName());

            final Pair<String, String> timestamps = createTimestamps(date);
            //execute Sonargraph
            try
            {
                baselineReportPath = SonargraphCommand.createReport(shell, timestamps.getLeft(), timestamps, version, analysisDirectory,
                        baselineReportPath, m_configuration, targetStartupXml.getAbsolutePath(), systemDirectory.getAbsolutePath());
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute Sonargraph for version " + version, e);
            }
        }

    }

    private List<Pair<String, Date>> processVersions(final IShell shell, final File analysisDirectory)
    {
        assert shell != null : "Parameter 'shell' of method 'processVersions' must not be null";
        assert analysisDirectory != null : "Parameter 'analysisDirectory' of method 'processVersions' must not be null";

        final Path versionsFile = Paths.get(analysisDirectory.getAbsolutePath(), "versionsAndTimes.txt");
        final List<Pair<String, Date>> versionsAndDates;
        if (m_configuration.getBoolean(Props.WRITE_TAGS_FILE.getPropertyName(), true))
        {
            try
            {
                final String versionUrl = m_configuration.getString(Props.MAVEN_VERSIONS_URL.getPropertyName());
                final List<String> excludedVersionParts = m_configuration.getList(String.class, Props.EXCLUDED_TAG_PARTS.getPropertyName());
                final Set<String> excludedTagParts = new HashSet<>(excludedVersionParts);
                versionsAndDates = MavenRepo.getVersions(shell, versionUrl, excludedTagParts);
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

    private void copyToFile(final File source, final File target) throws IOException
    {
        assert source != null : "Parameter 'source' of method 'copyToFile' must not be null";
        assert target != null : "Parameter 'target' of method 'copyToFile' must not be null";

        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyToDir(final File source, final File targetDir) throws IOException
    {
        assert source != null : "Parameter 'source' of method 'copyToDir' must not be null";
        assert targetDir != null : "Parameter 'targetDir' of method 'copyToDir' must not be null";

        final File target = new File(targetDir, source.getName());
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private File getArtifactDirectory(final String mavenHome, final String group, final String artifactId)
    {
        //C:\Users\Ingmar\.m2\repository\org\hibernate\hibernate-core\5.5.2.Final
        final List<String> parts = new ArrayList<>();
        parts.add("repository");
        for (final String next : group.split("\\."))
        {
            parts.add(next);
        }
        parts.add(artifactId);
        return Paths.get(mavenHome, parts.toArray(new String[] {})).toFile();
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