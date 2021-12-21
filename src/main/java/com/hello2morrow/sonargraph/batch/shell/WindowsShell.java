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
import java.util.ArrayList;
import java.util.List;

final class WindowsShell extends AbstractShell implements IShell
{
    public WindowsShell(final Charset charset)
    {
        super(OS.WINDOWS, charset);
    }

    @Override
    protected List<String> createCommand(final List<String> cmd)
    {
        assert cmd != null : "Parameter 'cmd' of method 'createCommand' must not be null";

        final List<String> result = new ArrayList<>(cmd.size() + 2);
        result.add("cmd.exe");
        result.add("/c");
        result.addAll(cmd);
        return result;
    }
}