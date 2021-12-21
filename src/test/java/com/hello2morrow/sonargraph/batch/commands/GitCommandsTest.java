package com.hello2morrow.sonargraph.batch.commands;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class GitCommandsTest
{
    @Test
    public void processTaggedLineWindows()
    {
        final Pair<String, String> commitAndTag = GitCommands.processTaggedCommit("'ccbfe413378ec4b7d48c96840067e0923980d549 v1.0.8)",
                Collections.emptySet());
        assertEquals("Wrong commit", "ccbfe413378ec4b7d48c96840067e0923980d549", commitAndTag.getLeft());
        assertEquals("Wrong tag", "v1.0.8", commitAndTag.getRight());
    }

    @Test
    public void processTaggedLineUnix()
    {
        final Pair<String, String> commitAndTag = GitCommands.processTaggedCommit("'23b492fe7b9f0821f2a8c34516cb858efcfaa55b  (tag: v0.3)'",
                Collections.emptySet());
        assertEquals("Wrong commit", "23b492fe7b9f0821f2a8c34516cb858efcfaa55b", commitAndTag.getLeft());
        assertEquals("Wrong tag", "v0.3", commitAndTag.getRight());
    }
}