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
package com.hello2morrow.sonargraph.batch.shell;

import java.nio.charset.Charset;

public final class ShellFactory
{
    public static IShell create(final Charset charset)
    {
        final String osName = System.getProperty("os.name", "unknown").trim().toLowerCase();
        if (osName.indexOf("windows") >= 0)
        {
            return new WindowsShell(charset);
        }

        throw new RuntimeException("Unsupported operating system " + osName);
    }
}