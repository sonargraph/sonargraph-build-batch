package com.hello2morrow.sonargraph.batch.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hello2morrow.sonargraph.batch.shell.IShell;

public final class GitCommands
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GitCommands.class);

    private GitCommands()
    {
        super();
    }

    public static boolean checkoutBranchAndGetLatest(final IShell shell, final File repoDir, final String branchName)
    {
        assert shell != null : "Parameter 'shell' of method 'checkout' must not be null";
        assert repoDir != null : "Parameter 'repoDir' of method 'checkout' must not be null";
        assert branchName != null && branchName.length() > 0 : "Parameter 'branchName' of method 'checkout' must not be empty";
        try
        {
            shell.execute("git checkout " + branchName, repoDir);
            shell.execute("git pull origin " + branchName, repoDir);
            return true;
        }
        catch (final Exception e)
        {
            LOGGER.error("FAILURE: {}", e.getMessage());
            return false;
        }
    }

    public static List<Pair<String, String>> createListOfTags(final IShell shell, final File repoDir, final File commitsAndTagsFile,
            final Set<String> excludedTagParts)
    {
        assert shell != null : "Parameter 'shell' of method 'writeListOfTags' must not be null";
        assert repoDir != null : "Parameter 'repoDir' of method 'writeListOfTags' must not be null";
        assert commitsAndTagsFile != null : "Parameter 'commitsAndTags' of method 'writeListOfTags' must not be null";

        final List<Pair<String, String>> result = new ArrayList<>();
        try
        {
            final List<String> lines = shell.execute("git log --tags --simplify-by-decoration --pretty=\"format:%H %d\" --reverse", repoDir);
            for (final String next : lines)
            {
                // Example line:
                // 70fa2fc5585896115fccf46728170835c3466ab9  (tag: activemq-4.1.0)
                final String[] parts = next.split("  ");
                if (parts.length != 2)
                {
                    LOGGER.warn("Line '{}' contains unexpected number of parts: {}. Skipping...", next, parts.length);
                    continue;
                }
                final String commitId = parts[0].trim();
                final String rawTag = parts[1].trim();

                final String[] tagParts = rawTag.split("tag: ");
                if (tagParts.length != 2)
                {
                    LOGGER.warn("Line '{}' contains unexpected format for tag : {}. Skipping...", next, tagParts.length);
                    continue;
                }
                final String tag = tagParts[1].substring(0, tagParts[1].length() - 1); //removing last ')'
                final String[] tagAndOrigin = tag.split(", ");
                final String finalTag;
                if (tagAndOrigin.length == 2)
                {
                    finalTag = tagAndOrigin[0];
                }
                else
                {
                    finalTag = tag;
                }
                LOGGER.debug("Tag '{}'", finalTag);

                if (excludedTagParts.stream().anyMatch(excl -> finalTag.contains(excl)))
                {
                    LOGGER.debug("Skipping tag '{}'", finalTag);
                    continue;
                }

                result.add(new ImmutablePair<>(commitId, finalTag));
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to extract commits + tags", e);
            return null;
        }

        try (Writer writer = new BufferedWriter(new FileWriter(commitsAndTagsFile)))
        {
            for (final Pair<String, String> next : result)
            {
                writer.append(next.getKey());
                writer.append(" ");
                writer.append(next.getValue());
                writer.append("\n");
                writer.flush();
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to write file containing commits and tags", e);
        }

        return result;
    }

    public static Pair<String, String> getTimestampOfCommit(final IShell shell, final String commit, final File repoDir) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'getTimestampOfCommit' must not be null";
        assert commit != null && commit.length() > 0 : "Parameter 'commit' of method 'getTimestampOfCommit' must not be empty";
        assert repoDir != null : "Parameter 'repoDir' of method 'getTimestampOfCommit' must not be null";

        final List<String> lines1 = shell.execute("git log -1 --date=format:%Y-%m-%d_%H-%M-%S --pretty=format:%ad", repoDir);
        final String fileNameTimestamp = lines1.get(0);

        final List<String> lines2 = shell.execute("git log -1 --format=%aI " + commit, repoDir);
        final String isoTimestamp = lines2.get(0);

        return new ImmutablePair<>(fileNameTimestamp, isoTimestamp);
    }

    public static void reset(final IShell shell, final File repoDir) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'reset' must not be null";
        assert repoDir != null : "Parameter 'repoDir' of method 'reset' must not be null";

        shell.execute("git reset --hard", repoDir);
    }

    public static void checkoutCommit(final IShell shell, final String commit, final File repoDir) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'checkoutCommit' must not be null";
        assert commit != null && commit.length() > 0 : "Parameter 'commit' of method 'checkoutCommit' must not be empty";
        assert repoDir != null : "Parameter 'repoDir' of method 'checkoutCommit' must not be null";

        shell.execute("git checkout " + commit, repoDir);
    }
}
