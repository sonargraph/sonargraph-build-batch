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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionTest
{
    @Test
    public void test()
    {
        final String versionString = "1.2.4-groovyless";
        final Version version = Version.fromString(versionString);
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(4, version.getMicro());
        assertEquals(0, version.getBuild());
        assertEquals(versionString, version.toString());
    }
}