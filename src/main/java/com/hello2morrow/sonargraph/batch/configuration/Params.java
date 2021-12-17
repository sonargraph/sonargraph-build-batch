package com.hello2morrow.sonargraph.batch.configuration;

public enum Params
{
    /** Startup parameters */
    ACTIVATION_CODE("activationCode", "code"),
    LICENSE_FILE("licenseFile", "path"),
    INSTALLATION_DIRECTORY("installationDirectory", "path"),
    LANGUAGES("languages", "Java|CPlusPlus|CSharp"),
    LOG_FILE("logFile", "path/filename"),
    LOG_LEVEL("logLevel", "off|error|warn|info|debug|trace|all", "info"),
    PROXY_HOST("proxyHost", "hostname"),
    PROXY_PORT("proxyPort", "port"),
    PROXY_USERNAME("proxyUsername", "name"),
    PROXY_PASSWORD("proxyPassword", "pwd"),
    LICENSE_SERVER_HOST("licenseServerHost", "hostname"),
    LICENSE_SERVER_PORT("licenseServerPort", "port", "8080"),
    PROGRESS_INFO(
                  "progressInfo",
                  "none, basic (shows progress as '.') or detailed (shows progress details and assumes support for backspace character)",
                  "none"),
    WAIT_FOR_LICENSE(
                     "waitForLicense",
                     "Number of tries that a license ticket is requested with a wait period of 1 minute between tries: -1 (never wait), 0 (wait indefinitely), positive number > 0",
                     "-1"),

    /** Installation specific parameters */
    /** CPlusPlus */
    COMPILER_DEFINITION_PATH("compilerDefinitionPath", "path to the C++ compiler definition to be used"),

    /** Python */
    PYTHON_INTERPRETER_PATH("pythonInterpreterPath", "path to the Python 3 interpreter to be used"),

    /** System open() + refresh() parameters */
    SYSTEM_DIRECTORY("systemDirectory", "path"),
    VIRTUAL_MODEL("virtualModel", "Modifiable.vm", "Modifiable.vm"),
    WORKSPACE_PROFILE("workspaceProfile", "Profile.xml"),
    QUALITY_MODEL_FILE("qualityModelFile", "QualityModel.sgqm"),
    SNAPSHOT_DIRECTORY("snapshotDirectory", "path"),
    SNAPSHOT_FILE_NAME("snapshotFileName", "fileName"),

    /** report() parameters */
    REPORT_DIRECTORY("reportDirectory", "path"),
    REPORT_FILE_NAME("reportFileName", "fileName"),
    REPORT_TYPE("reportType", "standard|full", "standard"),
    REPORT_FORMAT("reportFormat", "xml|html", "html"),
    ELEMENT_COUNT_TO_SPLIT_HTML_REPORT(
                                       "elementCountToSplitHtmlReport",
                                       "-1 (never split), 0 (use default value), 1 (always split), positive number > 1 (threshold for split)",
                                       "1000"),
    MAX_ELEMENT_COUNT_FOR_HTML_DETAILS_PAGE(
                                            "maxElementCountForHtmlDetailsPage",
                                            "-1 (no limit), 0 (use default value), positive number (controls the upper bound of elements shown in the table)",
                                            "2000"),
    SPLIT_BY_MODULE("splitByModule", "Controls if individual html files are generated per module", "false"),
    CONTEXT_INFO("contextInfo", "Allows specifying some additional info that helps to identify later the context of the report creation."),

    /** system diff parameters */
    BASELINE_REPORT_PATH("baselineReportPath", "Path of the baseline XML report that the current system is compared against"),

    /** failSet parameters */
    FAILSET("failSet", "failSet"),
    INCLUDE_ISSUE_TYPE("include", "include issue type"),
    EXCLUDE_ISSUE_TYPE("exclude", "exclude issue type"),

    RESOLUTION("resolution", "", "none"),
    SEVERITY("severity", "", "any"),
    ISSUE_TYPE("issueType", "", "any"),

    FAIL_ON_EMPTY_WORKSPACE("failOnEmptyWorkspace", "Fail if workspace is empty"),
    FAIL_ON_ISSUES("failOnIssues", "Fail if issues exist"),

    /** Dynamic system creation parameters */
    SYSTEM_BASE_DIRECTORY("systemBaseDirectory", "Base directory of the created software system"),
    SYSTEM_ID("systemId", "Id of the dynamically created software system"),
    USE_GROUP_ID_IN_MODULE_NAME("useGroupIdInModuleName", "Controls the module id creation"),
    INCLUDE_TEST_CODE("includeTestCode", "Controls if test code is included in the created software system"),

    /** Controls how the existing system info is updated */
    REPLACE_EXISTING_SYSTEM("replaceExisting", "Replaces the existing system completely", "false"),

    /** SonargraphEnterprise parameters */
    UPLOAD_HOST_URL(
                    "uploadHostUrl",
                    "The host and port of the Sonargraph Enterprise server. If this parameter is defined Sonargraph-Build will upload the report to this server. "
                            + "If the upload fails for some reason the report will be copied to a configurable directory (see parameter 'failedUploadDirectory') that collects all failed uploads. "
                            + "This directory is used by another task named 'resendFailedUploads' that should be invoked on a regular base. "
                            + "It is assumed that the server is internal, so as of now proxy settings are ignored. Must start with \"http://\""),
    CLIENT_KEY(
               "clientKey",
               "The client key for the Sonargraph-Enterprise server. Uploading reports only works with the right client "
                       + "key, which can be found on the settings page of Sonargaph-Enterprise. The settings page is only visible "
                       + "in administrator mode. Mandatory only if 'uploadHostUrl' is set."),
    FAILED_UPLOAD_DIRECTORY(
                            "failedUploadDirectory",
                            "If the upload to the server configured in the parameter 'uploadHostUrl' fails for some reason "
                                    + "the report that failed to upload is copied to this directory for later pickup by "
                                    + "'resendFailedUploads'. If you have a distributed build, that directory should ideally point to "
                                    + "a shared network storage drive."),
    BRANCH(
           "branch",
           "If reports are uploaded to the Sonargraph Enterprise server (see parameter 'uploadHostUrl'), it is useful to associate the report with the "
                   + "version control system's branch name to avoid mixing data of different branches. If the branch name is not given we assume 'default'. "
                   + "If you are only uploading data of the same branch you do not need to pass the branch name, otherwise it is highly recommended."),
    COMMIT_ID(
              "commitId",
              "This parameter is only used in conjunction with 'uploadHostUrl' and should be used if the uploaded report should be associated "
                      + "with a specific version control commit id."),
    VERSION(
            "version",
            "This parameter is only used in conjunction with 'uploadHostUrl' and should be used if the uploaded report should be associated with a specific software version. "
                    + "If you are using git flow, you would want to use this parameter for every commit of the master branch, since each commit is associated with a software release."),
    TIMESTAMP(
              "timestamp",
              "If reports are created for past system states, this parameter allows to specify the timestamp in ISO-8601 extended offset date-time format, e.g. '2011-12-03T10:15:30+01:00'");

    private final String m_parameterName;
    private final String m_valueInfo;
    private String m_defaultValue;

    private Params(final String parameterName, final String valueInfo)
    {
        this(parameterName, valueInfo, "");
    }

    private Params(final String parameterName, final String valueInfo, final String defaultValue)
    {
        assert parameterName != null && parameterName.length() > 0 : "Parameter 'parameterName' of method 'Params' must not be empty";
        assert valueInfo != null : "Parameter 'valueInfo' of method 'Params' must not be null";
        assert defaultValue != null : "Parameter 'defaultValue' of method 'Params' must not be null";
        m_parameterName = parameterName;
        m_valueInfo = valueInfo;
        m_defaultValue = defaultValue;
    }

    @Override
    public String toString()
    {
        return String.format("%s=<%s>", m_parameterName, m_valueInfo);
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

    public static Params fromParameterName(final String paramName)
    {
        assert paramName != null : "Parameter 'paramName' of method 'fromParameterName' must not be null";
        for (final Params param : Params.values())
        {
            if (param.getParameterName().equalsIgnoreCase(paramName))
            {
                return param;
            }
        }

        throw new IllegalArgumentException("Unsupported parameter '" + paramName + "'");
    }
}