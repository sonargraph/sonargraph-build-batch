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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
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
            LOGGER.error("FAILURE checking out branch " + branchName + " of repo " + repoDir.getAbsolutePath(), e);
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
            final List<String> cmd = Arrays.asList("git", "log", "--tags", "--simplify-by-decoration", "--pretty='%H %d'", "--reverse");
            final List<String> lines = shell.execute(cmd, repoDir);
            for (final String next : lines)
            {
                final Pair<String, String> pair = processTaggedCommit(next, excludedTagParts);
                if (pair != null)
                {
                    result.add(pair);
                }
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

    // Example line:
    // 70fa2fc5585896115fccf46728170835c3466ab9  (tag: activemq-4.1.0)
    static Pair<String, String> processTaggedCommit(final String line, final Set<String> excludedTagParts)
    {
        assert line != null : "Parameter 'line' of method 'processTaggedCommit' must not be null";
        assert excludedTagParts != null : "Parameter 'excludedTagParts' of method 'processTaggedCommit' must not be null";

        final String[] parts = line.split("\\s+");
        if (parts.length < 2)
        {
            LOGGER.warn("Line '{}' contains unexpected number of parts: {}. Skipping...", line, parts.length);
            return null;
        }
        String commitId = parts[0].trim();
        if (commitId.startsWith("'"))
        {
            commitId = commitId.substring(1, commitId.length());
        }

        final String rawTag;
        if (parts[1].contains("tag:"))
        {
            rawTag = parts[2].trim();
        }
        else
        {
            rawTag = parts[1].trim();
        }

        final int index = rawTag.indexOf(')');
        final String tag;
        if (index > 0)
        {
            tag = rawTag.substring(0, index); //removing last ')'
        }
        else
        {
            tag = rawTag;
        }
        final String finalTag;
        if (tag.endsWith(","))
        {
            finalTag = tag.substring(0, tag.length() - 1);
        }
        else
        {
            finalTag = tag;
        }

        if (finalTag.length() < 2 || excludedTagParts.stream().anyMatch(excl -> finalTag.contains(excl)))
        {
            LOGGER.debug("Skipping tag '{}'", finalTag);
            return null;
        }

        return new ImmutablePair<>(commitId, finalTag);
    }

    public static Pair<String, String> getTimestampOfCommit(final IShell shell, final String commit, final File repoDir) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'getTimestampOfCommit' must not be null";
        assert commit != null && commit.length() > 0 : "Parameter 'commit' of method 'getTimestampOfCommit' must not be empty";
        assert repoDir != null : "Parameter 'repoDir' of method 'getTimestampOfCommit' must not be null";

        final List<String> lines1 = shell.execute("git log -1 --date=format:'%Y-%m-%d_%H-%M-%S' --pretty=%ad", repoDir);
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
