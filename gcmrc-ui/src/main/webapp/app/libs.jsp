<%@page import="java.io.File"%>
<%@page import="java.io.FileReader" %>
<%@page import="java.util.Properties"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%@page import="gov.usgs.cida.gcmrc.util.PathUtil"%>
<%!
    private static final Logger log = LoggerFactory.getLogger("libs_jsp");
    protected PropertiesLoader propertiesLoader = new PropertiesLoader();
    protected Properties properties = propertiesLoader.getProperties();
%>
<%
    String vJquery = propertiesLoader.getProp(properties, "version.jquery");
    String vLog4JavaScript = propertiesLoader.getProp(properties, "version.log4javascript");
    String vOpenLayers = propertiesLoader.getProp(properties, "version.openlayers");
    String vModernizr = propertiesLoader.getProp(properties, "version.modernizr");
    String vBootstrap = propertiesLoader.getProp(properties, "version.bootstrap");
    String vJqueryUi = propertiesLoader.getProp(properties, "version.jqueryui");
    String vSugarJs = propertiesLoader.getProp(properties, "version.sugarjs");
    String vClosure = propertiesLoader.getProp(properties, "version.closure");
    String relPath = request.getParameter("relativePath");
    boolean development = Boolean.parseBoolean(request.getParameter("development"));
%>

<%-- Log4JavaScript --%>
<script type="text/javascript" src="<%= relPath%>webjars/log4javascript/<%= vLog4JavaScript%>/log4javascript<%= development ? "_uncompressed" : ""%>.js"></script>
<%-- The code below was originally part of the initialization convenience method in
    the CIDA LIBS log4javascript package but since we've switched to webjars, it 
    should be included here --%>
<script type="text/javascript">
    var LOG;
    /**
     * Initializes the log4javascript framework
     * 
     * @see http://log4javascript.org/
     * @param {type} params
     * @returns {undefined}
     */
    function initializeLogging(params) {
        if (!params) {
            params = {};
        }
        LOG = log4javascript.getLogger();

        var LOG4JS_PATTERN_LAYOUT = params.LOG4JS_PATTERN_LAYOUT || '' || "%rms - %-5p - %m%n";
        var LOG4JS_LOG_THRESHOLD = params.LOG4JS_LOG_THRESHOLD || 'debug' || "info"; // info will be default

        var appender = new log4javascript.BrowserConsoleAppender();

        appender.setLayout(new log4javascript.PatternLayout(LOG4JS_PATTERN_LAYOUT));
        var logLevel;
        switch (LOG4JS_LOG_THRESHOLD) {
            case "trace" :
                logLevel = log4javascript.Level.TRACE;
                break;
            case "debug" :
                logLevel = log4javascript.Level.DEBUG;
                break;
            case "info" :
                logLevel = log4javascript.Level.INFO;
                break;
            case "warn" :
                logLevel = log4javascript.Level.WARN;
                break;
            case "error" :
                logLevel = log4javascript.Level.ERROR;
                break;
            case "fatal" :
                logLevel = log4javascript.Level.FATAL;
                break;
            case "off" :
                logLevel = log4javascript.Level.OFF;
                break;
            default:
                logLevel = log4javascript.Level.INFO;
        }
        appender.setThreshold(logLevel);

        LOG.addAppender(appender);

        LOG.info('Log4javascript v.' + log4javascript.version + ' initialized.');
        LOG.info('Logging Appender Pattern Set to: ' + LOG4JS_PATTERN_LAYOUT);
        LOG.info('Logging Threshold Set To: ' + logLevel);
    }
</script>

<%-- Modernizr --%>
<script type="text/javascript" src="<%= relPath%>webjars/modernizr/<%=vModernizr%>/modernizr<%= development ? "" : ".min"%>.js"></script>

<%-- JQuery --%>
<script type="text/javascript" src="<%= relPath%>webjars/jquery/<%=vJquery%>/jquery<%= development ? "" : ".min"%>.js"></script>

<%-- Bootstrap --%>
<link rel="stylesheet" href="<%= relPath%>webjars/bootstrap/<%=vBootstrap%>/css/bootstrap<%= development ? "" : ".min"%>.css"/>
<link rel="stylesheet" href="<%= relPath%>webjars/bootstrap/<%=vBootstrap%>/css/bootstrap-responsive<%= development ? "" : ".min"%>.css"/>
<script type="text/javascript" src="<%= relPath%>webjars/bootstrap/<%=vBootstrap%>/js/bootstrap<%= development ? "" : ".min"%>.js"></script>

<%-- JqueryUI --%>
<link rel="stylesheet" href="<%= relPath%>webjars/jquery-ui/<%=vJqueryUi%>/themes/base/<%= development ? "/" : "minified/"%>jquery-ui<%= development ? "" : ".min"%>.css"/>
<script type="text/javascript" src="<%= relPath%>webjars/jquery-ui/<%=vJqueryUi%>/ui/<%= development ? "/" : "minified/"%>jquery-ui<%= development ? "" : ".min"%>.js"></script>

<%-- SugarJS --%>
<script type="text/javascript" src="<%= relPath%>webjars/sugar/<%=vSugarJs%>/sugar-full<%= development ? ".development" : ".min"%>.js"></script>

<script type="text/javascript">
    Object.extend();
</script>

<%-- Closure --%>
<script type="text/javascript" src="<%= relPath%>webjars/closure-library/<%=vClosure%>/goog/base.js"></script>

<%-- OpenLayers --%>
<script type="text/javascript" src="<%= relPath%>webjars/openlayers/<%=vOpenLayers%>/OpenLayers<%= development ? ".debug" : ""%>.js"></script>