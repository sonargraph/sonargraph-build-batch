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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigurationReader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationReader.class);

    private ConfigurationReader()
    {
        super();
    }

    public static Configuration read(final String propertyFileName)
    {
        final PropertiesConfiguration props = new PropertiesConfiguration();
        props.setListDelimiterHandler(new DefaultListDelimiterHandler(','));

        if (propertyFileName != null)
        {
            try (final Reader reader = new InputStreamReader(new FileInputStream(new File(propertyFileName))))
            {
                props.read(reader);
            }
            catch (final IOException | ConfigurationException ex)
            {
                LOGGER.error("Failed to load properties: ", ex);
                return null;
            }
        }
        else
        {
            try (InputStream inStream = Configuration.class.getClassLoader().getResourceAsStream("config.properties");
                    Reader reader = new InputStreamReader(inStream))
            {
                props.read(reader);
            }
            catch (final IOException | ConfigurationException ex)
            {
                LOGGER.error("Failed to load properties: ", ex);
                return null;
            }
        }
        return props;
    }
}