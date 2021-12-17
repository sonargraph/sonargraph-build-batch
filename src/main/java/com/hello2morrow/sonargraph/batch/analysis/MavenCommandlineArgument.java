package com.hello2morrow.sonargraph.batch.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MavenCommandlineArgument
{
    GROUP_ID("groupId", "Maven Group Id", false),
    ARTIFACT_ID("artifactId", "Maven Artifact Id", false),
    PROPERTY_FILE_NAME("propertyFileName", "Path to the .properties file containing further analysis configuration", false),
    WRITE_VERSIONS_FILE(
                        "writeVersionsFile",
                        "If 'true' available versions will be written to file. If 'false', versions will be read from file.",
                        true,
                        "true",
                        Type.BOOLEAN),
    NUMBER_OF_MOST_RECENT_VERSIONS(
                                   "numberOfMostRecentVersions",
                                   "Number of most recent versions to anaylze (-1 means all).",
                                   true,
                                   "-1",
                                   Type.INTEGER);

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenCommandlineArgument.class);

    private final String m_parameterName;
    private final String m_valueInfo;
    private boolean m_isOptional;
    private String m_defaultValue;
    private Type m_type;

    MavenCommandlineArgument(final String parameterName, final String valueInfo, final boolean isOptional)
    {
        this(parameterName, valueInfo, isOptional, "");
    }

    MavenCommandlineArgument(final String parameterName, final String valueInfo, final boolean isOptional, final String defaultValue)
    {
        this(parameterName, valueInfo, isOptional, defaultValue, Type.STRING);
    }

    MavenCommandlineArgument(final String parameterName, final String valueInfo, final boolean isOptional, final String defaultValue, final Type type)
    {
        assert parameterName != null && parameterName.length() > 0 : "Parameter 'parameterName' of method 'Params' must not be empty";
        assert valueInfo != null : "Parameter 'valueInfo' of method 'Params' must not be null";
        assert defaultValue != null : "Parameter 'defaultValue' of method 'Params' must not be null";
        assert type != null : "Parameter 'type' of method 'MavenCommandlineArgument' must not be null";

        m_parameterName = parameterName;
        m_valueInfo = valueInfo;
        m_isOptional = isOptional;
        m_defaultValue = defaultValue;
        m_type = type;
    }

    @Override
    public String toString()
    {
        if (!m_isOptional)
        {
            return String.format("%s=<%s> (default: %s), type=%s", m_parameterName, m_valueInfo, m_defaultValue, m_type.name());
        }

        return String.format("%s=<%s> [optional] (default: %s), type=%s", m_parameterName, m_valueInfo, m_defaultValue, m_type.name());
    }

    public String getValueInfo()
    {
        return m_valueInfo;
    }

    public String getDefaultValue()
    {
        return m_defaultValue;
    }

    public String getParameterName()
    {
        return m_parameterName;
    }

    public static MavenCommandlineArgument fromParameterName(final String paramName)
    {
        assert paramName != null : "Parameter 'paramName' of method 'fromParameterName' must not be null";
        for (final MavenCommandlineArgument param : MavenCommandlineArgument.values())
        {
            if (param.getParameterName().equalsIgnoreCase(paramName))
            {
                return param;
            }
        }

        throw new IllegalArgumentException("Unsupported parameter '" + paramName + "'");
    }

    static Map<MavenCommandlineArgument, String> parseArgs(final String[] args)
    {
        if (args.length == 0)
        {
            return Collections.emptyMap();
        }

        final Map<MavenCommandlineArgument, String> argsMap = new HashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            final String next = args[i];
            final String[] line = next.split("=");
            if (line.length != 2)
            {
                LOGGER.info("Ignoring argument " + next);
                continue;
            }

            final String paramName = line[0].trim();
            try
            {
                final MavenCommandlineArgument param = MavenCommandlineArgument.fromParameterName(paramName);
                final String value = line[1].trim();
                argsMap.put(param, value);
            }
            catch (final IllegalArgumentException ex)
            {
                LOGGER.error("Unsupported parameter '" + paramName + "'");
            }
        }
        return argsMap;
    }

    static String getArgument(final Map<MavenCommandlineArgument, String> argsMap, final MavenCommandlineArgument arg)
    {
        assert argsMap != null : "Parameter 'argsMap' of method 'getArgument' must not be null";
        assert arg != null : "Parameter 'arg' of method 'getArgument' must not be null";

        String value = argsMap.get(arg);
        if (value == null)
        {
            if (!arg.m_isOptional)
            {
                throw new RuntimeException("Missing argument for: " + arg.toString());
            }

            value = arg.getDefaultValue();
        }

        switch (arg.m_type)
        {
        case STRING:
            return value;
        case BOOLEAN:
            return value;
        case INTEGER:
            try
            {
                Integer.parseInt(value);
            }
            catch (final NumberFormatException ex)
            {
                throw new RuntimeException("Invalid value for: " + arg.toString());
            }
            return value;
        default:
            assert false : "Unsupported argument type for: " + arg.toString();
        }
        return null;
    }

    private enum Type
    {
        STRING,
        BOOLEAN,
        INTEGER
    }
}