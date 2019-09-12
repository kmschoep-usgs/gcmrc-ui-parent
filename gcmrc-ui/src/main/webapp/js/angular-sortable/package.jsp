<%@page import="gov.usgs.cida.gcmrc.util.PathUtil"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="javax.naming.Context"%>
<%@page import="java.util.Properties"%>
<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%!
	private static final Logger log = LoggerFactory.getLogger("package_jsp");
	protected PropertiesLoader propertiesLoader = new PropertiesLoader();
	protected Properties properties = propertiesLoader.getProperties();
	protected Context context = propertiesLoader.getContextProps();
	
	protected boolean development = Boolean.parseBoolean(propertiesLoader.getProp(context, "all.development")) || Boolean.parseBoolean(propertiesLoader.getProp(context, "${project.artifactId}.development"));

%>
<%
        String vAngularJs = propertiesLoader.getProp(properties, "version.angularjs");
        String vAngularUiSortable = propertiesLoader.getProp(properties, "version.angular_ui_sortable");
        String vAngularUiBootstrap = propertiesLoader.getProp(properties, "version.angular_ui_bootstrap");
        String relPath = request.getParameter("relativePath");
%>
<script type="text/javascript" src="<%= relPath %>webjars/angularjs/<%=vAngularJs%>/angular<%= development ? "" : ".min"%>.js"></script>
<script type="text/javascript" src="<%= relPath %>webjars/angular-ui-sortable/<%=vAngularUiSortable%>/sortable<%= development ? "" : ".min"%>.js"></script>
<script type="text/javascript" src="<%= relPath %>webjars/angular-ui-bootstrap/<%=vAngularUiBootstrap%>/ui-bootstrap-tpls<%= development ? "" : ".min"%>.js"></script>