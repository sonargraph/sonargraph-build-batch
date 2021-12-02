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

public enum Props
{
    NAME("name"),
    SONARGRAPH_SYSTEM_DIRECTORY("sonargraphSystemDirectory"),
    SHELL_CHARSET("shellCharset"),
    REPO_DIRECTORY("repoDirectory"),
    BRANCH_NAME("branchName"),
    EXCLUDED_TAG_PARTS("excludedTagParts"),
    ANALYSIS_DIRECTORY("analysisDirectory"),

    CONFIG_FILE("configFile"),

    INST_DIRECTORY("instDirectory"),

    UPLOAD_HOST_URL("uploadHostUrl"),
    CLIENT_KEY("clientKey"),
    WRITE_TAGS_FILE("writeTagsFile"),
    JAVA_HOME_FOR_MVN("javaHomeForMvn"),

    MAVEN_REPO_HOME("mavenRepoHome"),
    MAVEN_VERSIONS_URL("mavenVersionsUrl"),
    MAVEN_GROUP_ID("mavenGroupId"),
    MAVEN_ARTIFACT_ID("mavenArtifactId"),

    ;

    private String m_propName;

    Props(final String propName)
    {
        m_propName = propName;
    }

    public String getPropertyName()
    {
        return m_propName;
    }
}