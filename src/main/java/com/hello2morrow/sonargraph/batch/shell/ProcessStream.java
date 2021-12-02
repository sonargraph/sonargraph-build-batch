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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

final class ProcessStream extends Thread
{
    private final InputStream m_is;
    private final List<String> m_output;
    private volatile boolean m_completed;
    private final Charset m_charset;

    public ProcessStream(final String name, final InputStream is, final Charset charset)
    {
        super(name);
        assert is != null : "Parameter 'is' of method 'ProcessStream' must not be null";
        assert charset != null : "Parameter 'charSet' of method 'ProcessStream' must not be null";

        m_is = is;
        m_output = new ArrayList<>();
        m_charset = charset;
    }

    @Override
    public void run()
    {
        m_completed = false;
        final InputStreamReader isr = new InputStreamReader(m_is, m_charset);
        final BufferedReader br = new BufferedReader(isr);
        String line;
        try
        {
            while ((line = br.readLine()) != null)
            {
                final String next = line;
                m_output.add(next);
            }
        }
        catch (final IOException ex)
        {
            throw new RuntimeException(ex);
        }
        m_completed = true;
    }

    public List<String> getOutput()
    {
        return m_output;
    }

    public boolean isCompleted()
    {
        return m_completed;
    }
}