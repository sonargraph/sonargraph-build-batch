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
package com.hello2morrow.sonargraph.batch.configuration;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version>, Serializable
{
    private static final long serialVersionUID = 6387606412181055570L;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+).*");

    final int m_major;
    final int m_minor;
    final int m_micro;
    final int m_build;

    private final String m_versionString;

    private Version(final String versionString, final int major, final int minor, final int micro, final int build)
    {
        assert versionString != null : "Parameter 'versionString' of method 'Version' must not be null";

        m_versionString = versionString;
        m_major = major;
        m_minor = minor;
        m_micro = micro;
        m_build = build;
    }

    @Override
    public int compareTo(final Version other)
    {
        int diff = m_major - other.m_major;

        if (diff != 0)
        {
            return diff;
        }
        diff = m_minor - other.m_minor;
        if (diff != 0)
        {
            return diff;
        }
        diff = m_micro - other.m_micro;
        if (diff != 0)
        {
            return diff;
        }
        diff = m_build - other.m_build;
        return diff;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final Version version = (Version) o;
        return version.m_versionString.equals(m_versionString);
    }

    @Override
    public int hashCode()
    {
        return m_versionString.hashCode();
    }

    @Override
    public String toString()
    {
        return m_versionString;
    }

    public int getMajor()
    {
        return m_major;
    }

    public int getMinor()
    {
        return m_minor;
    }

    public int getMicro()
    {
        return m_micro;
    }

    public int getBuild()
    {
        return m_build;
    }

    public String getVersionWithoutBuildNumber()
    {
        return String.format("%d.%d.%d", m_major, m_minor, m_micro);
    }

    public static Version fromString(final String versionString)
    {
        assert versionString != null : "Parameter 'versionString' of method 'fromString' must not be null";

        try
        {
            final String[] parts = versionString.split("\\.");
            final int major = Integer.parseInt(parts[0]);

            final int minor = parts.length > 1 ? extractNumber(parts[1]) : 0;
            final int micro = parts.length > 2 ? extractNumber(parts[2]) : 0;
            int build;
            if (parts.length > 3)
            { //4th part is optional

                build = extractNumber(parts[3]);
            }
            else
            {
                build = 0;
            }

            return new Version(versionString, major, minor, micro, build);
        }
        catch (final NumberFormatException ex)
        {
            throw new IllegalArgumentException("Version '" + versionString + "' does not match the expected format a.b.c", ex);
        }
    }

    private static int extractNumber(final String part)
    {
        final Matcher matcher = NUMBER_PATTERN.matcher(part);
        if (matcher.matches())
        {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
