<%@page import="gov.usgs.cida.gcmrc.util.PathUtil"%>
<%@page import="javax.naming.Context"%>
<%@page import="java.util.Properties"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%!
	private static final Logger log = LoggerFactory.getLogger("package_jsp");
	protected PropertiesLoader propertiesLoader = new PropertiesLoader();
	protected Properties properties = propertiesLoader.getProperties();
	protected Context context = propertiesLoader.getContextProps();

	protected boolean development = Boolean.parseBoolean(propertiesLoader.getProp(context, "all.development")) || Boolean.parseBoolean(propertiesLoader.getProp(context, "${project.artifactId}.development"));
%>
<%
    String vPro4JS = propertiesLoader.getProp(properties, "version.proj4js");
    String relPath = request.getParameter("relativePath");
%>
<script src="<%= relPath%>webjars/proj4js/<%= vPro4JS%>/proj4.js" type="text/javascript"></script>
<script type="text/javascript">
    proj4.defs["EPSG:26949"] = "+proj=tmerc +lat_0=31 +lon_0=-111.9166666666667 +k=0.9999 +x_0=213360 +y_0=0 +ellps=GRS80 +datum=NAD83 +units=m +no_defs";
</script>