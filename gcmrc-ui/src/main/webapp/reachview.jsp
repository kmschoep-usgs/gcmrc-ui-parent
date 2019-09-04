<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%@page import="gov.usgs.cida.path.PathUtil"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.Map"%>
<%@page import="javax.naming.Context"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!
	private static final Logger log = LoggerFactory.getLogger("reachview_jsp");
	protected PropertiesLoader propertiesLoader = new PropertiesLoader();
	protected Context context = propertiesLoader.getContextProps();
	
	boolean development = Boolean.parseBoolean(propertiesLoader.getProp(context, "all.development")) || Boolean.parseBoolean(propertiesLoader.getProp(context, "${project.artifactId}.development"));
	protected String warningMessage = propertiesLoader.getProp(context, "gcmrc.site.warning.message", "");
%>

<%
	request.setAttribute("development", development);
	request.setAttribute("logLevel", development ? "debug" : "info");

	String pageName = page.getClass().getSimpleName().substring(0, page.getClass().getSimpleName().indexOf("_jsp"));
	request.setAttribute("pageName", pageName);

	String relativePath = PathUtil.calculateRelativePath(request.getRequestURI(), request.getContextPath());
	request.setAttribute("relativePath", relativePath);

	String basePath = "reach";
	Map<String, String> restOfPath = PathUtil.calculateRestOfURI(request.getRequestURI(), basePath, "networkName", "upstreamStationName", "downstreamStationName");
	String networkName = restOfPath.get("networkName");
	if (StringUtils.isEmpty(networkName)) {
		networkName = "Unknown";
	}
	request.setAttribute("networkName", networkName);
	
	String networkHoursOffset = "7";
	if ( ("BIBE".equals(networkName)) || ("CHIP".equals(networkName)) ) {
		networkHoursOffset = "6";
	}
	request.setAttribute("networkHoursOffset", networkHoursOffset);
	
	String upstreamStationName = restOfPath.get("upstreamStationName");
	if (StringUtils.isEmpty(upstreamStationName)) {
		upstreamStationName = "00000000";
	}
	request.setAttribute("upstreamStationName", upstreamStationName);
	
	String downstreamStationName = restOfPath.get("downstreamStationName");
	if (StringUtils.isEmpty(downstreamStationName)) {
		downstreamStationName = "00000000";
	}
	request.setAttribute("downstreamStationName", downstreamStationName);
%>

<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html lang="en" class="no-js"> <!--<![endif]-->
    <head>
        <jsp:include page="template/GCMRCHead.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="shortName" value="GCMRC" />
			<jsp:param name="title" value="" />
			<jsp:param name="description" value="" />
			<jsp:param name="author" value="" />
			<jsp:param name="keywords" value="" />
			<jsp:param name="publisher" value="" />
			<jsp:param name="revisedDate" value="" />
			<jsp:param name="nextReview" value="" />
			<jsp:param name="expires" value="never" />
			<jsp:param name="development" value="${development}" />
		</jsp:include>

		<jsp:include page="app/libs.jsp"></jsp:include>

		<jsp:include page="js/bootstrap-datepicker/package.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="<%= development%>" />
		</jsp:include>

		<script src="${relativePath}js/proj4js/proj4js.js" type="text/javascript"></script>
		<script type="text/javascript">
			Proj4js.defs["EPSG:26949"] = "+proj=tmerc +lat_0=31 +lon_0=-111.9166666666667 +k=0.9999 +x_0=213360 +y_0=0 +ellps=GRS80 +datum=NAD83 +units=m +no_defs";
		</script>
		<jsp:include page="js/openlayers/openlayers.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>
		<script src="${relativePath}js/openlayers/extension/Renderer/DeclusterCanvas.js" type="text/javascript"></script>
			
		<jsp:include page="js/dygraphs/dygraphs.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>

		<script type="text/javascript">
			var CONFIG = {};

			CONFIG.development = ${development};
			CONFIG.relativePath = '${relativePath}';
			CONFIG.networkName = '${networkName}';
			CONFIG.upstreamStationName = '${upstreamStationName}';
			CONFIG.downstreamStationName = '${downstreamStationName}';
			CONFIG.networkHoursOffset = ${networkHoursOffset};

		</script>

		<jsp:include page="app/gcmrc.jsp"></jsp:include>
		<jsp:include page="js/angular-sortable/package.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>
		<script src="${relativePath}services/rest/station/allsite/${networkName}?jsonp_callback=GCMRC.StationLoad"></script>
		<jsp:include page="app/graphing/package.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>
		<jsp:include page="pages/page.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="pageName" value="${pageName}" />
		</jsp:include>
		<script src="${relativePath}services/rest/reach/${networkName}/${upstreamStationName}/${downstreamStationName}?jsonp_callback=GCMRC.Page.reachLoad"></script>
		<script src="${relativePath}services/rest/reach/detail/${networkName}/${upstreamStationName}/${downstreamStationName}?jsonp_callback=GCMRC.Page.reachDetailLoad"></script>
		<script src="${relativePath}services/rest/reach/por/${upstreamStationName}?jsonp_callback=GCMRC.Page.reachPORLoad"></script>
		<script src="${relativePath}services/rest/reach/credit/${upstreamStationName}/${downstreamStationName}?jsonp_callback=GCMRC.Page.creditLoad"></script>

		<style type="text/css">
			.buildButton {
				-moz-box-shadow:inset 0px 1px 0px 0px #ffffff;
				-webkit-box-shadow:inset 0px 1px 0px 0px #ffffff;
				box-shadow:inset 0px 1px 0px 0px #ffffff;
				background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #f9f9f9), color-stop(1, #e9e9e9) );
				background:-moz-linear-gradient( center top, #f9f9f9 5%, #e9e9e9 100% );
				filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#f9f9f9', endColorstr='#e9e9e9');
				background-color:#f9f9f9;
				-moz-border-radius:8px;
				-webkit-border-radius:8px;
				border-radius:8px;
				border:1px solid #dcdcdc;
				display:inline-block;
				color:#666666;
				font-family:arial;
				font-size:18px;
				font-weight:bold;
				padding:8px 24px;
				margin: 0px 5px;
				text-decoration:none;
				text-shadow:1px 1px 0px #ffffff;
			}.buildButton:hover {
				background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #e9e9e9), color-stop(1, #f9f9f9) );
				background:-moz-linear-gradient( center top, #e9e9e9 5%, #f9f9f9 100% );
				filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#e9e9e9', endColorstr='#f9f9f9');
				background-color:#e9e9e9;
			}.buildButton:active {
				position:relative;
				top:1px;
			}
			/* This imageless css button was generated by CSSButtonGenerator.com */
		</style>
    </head>
    <body>
		<div class="container-fluid">
			<jsp:include page="template/GCMRCHeader.jsp">
				<jsp:param name="relPath" value="${relativePath}" />
				<jsp:param name="header-class" value="" />
			</jsp:include>
			<div>
				<!--				<div class="clear">-->
				<!--				</div>-->
				<h2>Grand Canyon Monitoring and Research Center</h2>
				<h3><span id="station-title"></span> <small id="station-subtitle"></small></h3>
				<% if (!warningMessage.isEmpty()) { %>
					<div class="alert alert-danger site-alert"><b>NOTICE:&nbsp;</b><%= warningMessage%></div>
				<% } %>
				<div id="breadcrumbs">
					<span>
						<span><a href="https://www.gcmrc.gov/gcmrc.aspx">Home</a></span>
						<span> &gt; </span>
						<span><a href="${relativePath}index.jsp">Discharge, Sediment and Water Quality</a></span>
						<span> &gt; </span>
						<span><a href="${relativePath}reaches/${networkName}/"><span class="network-name"></span> Reaches</a></span>
						<span> &gt; </span>
						<span>${upstreamStationName} to ${downstreamStationName}</span>
					</span>
				</div>
				<div id="content" class="row-fluid">
					<div class="span3">
						<div id="bedloadSlider" class="innerRightSidebar">
							<div class="sectionTitle">Adjustable Bedload</div>
							<div id="bedloadList" class="rightSidebarText"></div>
						</div>
						<div class="innerRightSidebar">
							<div class="sectionTitle">Adjustable Uncertainty</div>
							<div id="parameterLoading" class="ajaxLoading"><div class="alert alert-info">Loading...</div><div style="margin-bottom: 20px;"><img src="${relativePath}app/ajax-loader-trsp.gif" alt="Loading..." height="32px" width="32px"></div></div>
							<div id="parameterList" class="rightSidebarText"></div>
							<div id="riverBedloadSlider" class="rightSidebarText"></div>
						</div>
						<div id="resetParametersDiv" style="text-align:center;">
							<a href="#" class="buildButton" id="resetDefaults">Restore Defaults</a>
						</div>
					</div>
					<div class="span6" id="options-dygraph">
						<div class="innerRightSidebar">
							<div class="sectionTitle">Date Range</div>
							<form class="form-horizontal">
								<div id="porContainer"></div>
								<div class="control-group">
									<label class="control-label" for="beginDatePicker">Start</label>
									<div class="controls">
										<div class="input-prepend">
											<span class="add-on"><i class="icon-calendar"></i> </span>
											<input type="text" id="beginDatePicker" name="beginPosition" placeholder="Start Date">
										</div>
									</div>
								</div>
								<div class="control-group">
									<label class="control-label" for="endDatePicker">End</label>
									<div class="controls">
										<div class="input-prepend">
											<span class="add-on"><i class="icon-calendar"></i> </span>
											<input type="text" id="endDatePicker" name="endPosition" placeholder="End Date">
										</div>
									</div>
								</div>
								<div id="build-div" style="text-align:center;"><a href="#" class="buildButton" id="buildGraph">Build Graph</a><!--<a href="#" class="buildButton" id="downloadData">Download</a>--></div>
							</form>
							<form id='exportPost' action="${relativePath}export" method="POST">
								<input type="hidden" name='filename' value="gcmrcSedimentBudget.tsv">
								<input type='hidden' name='type' value='text/tab-separated-values'>
								<input type='hidden' name='data' value='Hello World'>
							</form>
						</div>
						<div id="viz" class="innerRightSidebar">
							<div class="sectionTitle">Data</div>
							<div class="rightSidebarText">
								<div id="specialMsg"></div>
								<div id="errorMsg"></div>
								<div id="infoMsg">
									To visualize data in a graph, please select parameters from the left as well as a date range from above.
								</div>
								<div id="loading" class="ajaxLoading">
									<div class="alert alert-info">Your graph is being constructed...</div>
									<div style="margin-bottom: 20px;">
										<img src="${relativePath}app/ajax-loader-trsp.gif" alt="Loading..." height="32px" width="32px">
									</div>
								</div>
								<div id="reachSummaryContainer">
									<div id="reach-fines-summary"></div>
									<div id="reach-sand-summary"></div>
								</div>
								<div id="legend-dygraph"></div>
								<div id="data-dygraph"></div>
							</div>
						</div>
					</div>

					<div class="span3">
						<div class="innerRightSidebar">
							<div class="sectionTitle">Location</div>
							<div id="locationArea">
								<div id="openlayers-map"></div>
							</div>
						</div>
						<div id="addlSources" class="innerRightSidebar">
							<div class="sectionTitle">Additional Information</div>
							<ul id="addlSourcesBody">
							</ul>
						</div>
						<div id="lastSedDates"></div> <%-- placeholder for date stuff --%>
					</div>
				</div>
			</div>
			<jsp:include page="template/GCMRCFooter.jsp">
				<jsp:param name="relPath" value="${relativePath}" />
				<jsp:param name="header-class" value="" />
				<jsp:param name="contact-info" value="" />
			</jsp:include>
		</div>
	</body>
</html>
