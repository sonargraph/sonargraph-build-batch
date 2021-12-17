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
package com.hello2morrow.sonargraph.batch.maven;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.hello2morrow.sonargraph.batch.configuration.Version;
import com.hello2morrow.sonargraph.batch.shell.IShell;

public class MavenRepo
{
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    //Example line of maven repo:
    //<a href="3.3.0.CR1/" title="3.3.0.CR1/">3.3.0.CR1/</a>                                        2008-09-26 20:39         -
    private static final Pattern PATTERN = Pattern.compile(".*title=\"(.*)\".*</a>[\\s]+(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}).*");

    public static List<Pair<Version, Date>> getVersions(final IShell shell, final String url, final Set<String> skipVersionParts) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'getVersions' must not be null";
        assert url != null : "Parameter 'url' of method 'getVersions' must not be null";
        assert skipVersionParts != null : "Parameter 'skipVersionParts' of method 'getVersions' must not be null";

        final List<String> lines = shell.execute("curl -L " + url, new File("."));
        final List<Pair<Version, Date>> versions = new ArrayList<>();
        for (final String next : lines)
        {
            final String trimmed = next.trim();
            if (trimmed.startsWith("<a href=\""))
            {
                final Pair<Version, Date> versionAndDate = processVersionLine(trimmed, skipVersionParts);
                if (versionAndDate != null)
                {
                    versions.add(versionAndDate);
                }
            }
        }

        versions.sort((p1, p2) -> p1.getRight().compareTo(p2.getRight()));

        Version previous = null;
        for (final Iterator<Pair<Version, Date>> iter = versions.iterator(); iter.hasNext();)
        {
            final Pair<Version, Date> next = iter.next();
            if (previous == null)
            {
                previous = next.getLeft();
                continue;
            }

            if (previous.compareTo(next.getLeft()) > 0)
            {
                //previous was of higher version than next and needs to be discarded
                iter.remove();
            }
            else
            {
                previous = next.getLeft();
            }
        }

        return versions;
    }

    //Package private to allow access in JUnit test
    static Pair<Version, Date> processVersionLine(final String line, final Set<String> skipVersionParts) throws ParseException
    {
        assert line != null : "Parameter 'line' of method 'processVersionLine' must not be null";
        assert skipVersionParts != null : "Parameter 'skipVersionParts' of method 'processVersionLine' must not be null";

        final Matcher matcher = PATTERN.matcher(line);
        if (matcher.matches())
        {
            final String rawVersion = matcher.group(1);
            final String versionString;
            if (rawVersion.endsWith("/"))
            {
                versionString = rawVersion.substring(0, rawVersion.length() - 1);
            }
            else
            {
                versionString = rawVersion;
            }

            if (versionString.startsWith("maven-metadata.xml"))
            {
                return null;
            }

            if (skipVersionParts.stream().anyMatch(skip -> versionString.contains(skip)))
            {
                return null;
            }

            final Version version = Version.fromString(versionString);
            final String dateTime = matcher.group(2);
            final Date date = DATE_FORMAT.parse(dateTime);
            return new ImmutablePair<>(version, date);
        }

        return null;
    }
}