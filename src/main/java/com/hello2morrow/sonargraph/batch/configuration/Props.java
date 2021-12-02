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