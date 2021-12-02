package com.hello2morrow.sonargraph.batch.maven;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.hello2morrow.sonargraph.batch.shell.IShell;

public class MavenRepo
{
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    //Example line of maven repo:
    //<a href="3.3.0.CR1/" title="3.3.0.CR1/">3.3.0.CR1/</a>                                        2008-09-26 20:39         -
    private static final Pattern PATTERN = Pattern.compile(".*title=\"(.*)\".*</a>[\\s]+(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}).*");

    public static List<Pair<String, Date>> getVersions(final IShell shell, final String url, final Set<String> skipVersionParts) throws Exception
    {
        assert shell != null : "Parameter 'shell' of method 'getVersions' must not be null";
        assert url != null : "Parameter 'url' of method 'getVersions' must not be null";
        assert skipVersionParts != null : "Parameter 'skipVersionParts' of method 'getVersions' must not be null";

        final List<String> lines = shell.execute("curl " + url, new File("."));
        final List<Pair<String, Date>> versions = new ArrayList<>();
        for (final String next : lines)
        {
            final String trimmed = next.trim();
            if (trimmed.startsWith("<a href=\""))
            {
                final Pair<String, Date> versionAndDate = processVersionLine(trimmed, skipVersionParts);
                if (versionAndDate != null)
                {
                    versions.add(versionAndDate);
                }
            }
        }

        versions.sort((p1, p2) -> p1.getRight().compareTo(p2.getRight()));
        return versions;
    }

    //Package private to allow access in JUnit test
    static Pair<String, Date> processVersionLine(final String line, final Set<String> skipVersionParts) throws ParseException
    {
        assert line != null : "Parameter 'line' of method 'processVersionLine' must not be null";
        assert skipVersionParts != null : "Parameter 'skipVersionParts' of method 'processVersionLine' must not be null";

        final Matcher matcher = PATTERN.matcher(line);
        if (matcher.matches())
        {
            final String rawVersion = matcher.group(1);
            final String version;
            if (rawVersion.endsWith("/"))
            {
                version = rawVersion.substring(0, rawVersion.length() - 1);
            }
            else
            {
                version = rawVersion;
            }

            if (version.startsWith("maven-metadata.xml"))
            {
                return null;
            }

            if (skipVersionParts.stream().anyMatch(skip -> version.contains(skip)))
            {
                return null;
            }

            final String dateTime = matcher.group(2);
            final Date date = DATE_FORMAT.parse(dateTime);
            return new ImmutablePair<>(version, date);
        }

        return null;
    }
}