package com.hello2morrow.sonargraph.batch.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class MavenDownloadExecutionTest
{
    @Test
    public void processVersionLineTest() throws ParseException
    {
        final String line = "<a href=\"3.3.0.CR1/\" title=\"3.3.0.CR1/\">3.3.0.CR1/</a>                                        2008-09-26 20:39         -";
        final Pair<String, Date> versionAndDate = MavenRepo.processVersionLine(line, Collections.emptySet());
        assertNotNull(versionAndDate);
        assertEquals("Wrong version", "3.3.0.CR1", versionAndDate.getLeft());
        assertEquals("Wrong date", "2008-09-26 20:39", MavenRepo.DATE_FORMAT.format(versionAndDate.getRight()));
    }
}