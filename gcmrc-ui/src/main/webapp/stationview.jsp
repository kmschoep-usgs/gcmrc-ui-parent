<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%@page import="gov.usgs.cida.gcmrc.util.PathUtil"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.util.Map"%>
<%@page import="javax.naming.Context"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%!private static final Logger log = LoggerFactory.getLogger("stationview_jsp");
	protected PropertiesLoader propertiesLoader = new PropertiesLoader();
	protected Context context = propertiesLoader.getContextProps();
	
	boolean development = Boolean.parseBoolean(propertiesLoader.getProp(context, "all.development")) || Boolean.parseBoolean(propertiesLoader.getProp(context, "${project.artifactId}.development"));
	protected String warningMessage = propertiesLoader.getProp(context, "gcmrc.site.warning.message", "");%>

<%
	request.setAttribute("development", development);
	request.setAttribute("logLevel", development ? "debug" : "info");

	String pageName = page.getClass().getSimpleName().substring(0, page.getClass().getSimpleName().indexOf("_jsp"));
	request.setAttribute("pageName", pageName);

	String relativePath = PathUtil.calculateRelativePath(request.getRequestURI(), request.getContextPath());
	request.setAttribute("relativePath", relativePath);

	String basePath = "station";
	Map<String, String> restOfPath = PathUtil.calculateRestOfURI(request.getRequestURI(), basePath, "networkName", "stationName");
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

	String stationName = restOfPath.get("stationName");
	if (StringUtils.isEmpty(stationName)) {
		stationName = "00000000";
	}
	request.setAttribute("stationName", stationName);
%>

<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html lang="en" class="no-js"> <!--<![endif]-->
    <head>
        <jsp:include page="template/GCMRCHead.jsp">
			<jsp:param name="relativePath" value="${relativePath}" />
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

		<jsp:include page="app/libs.jsp">
                    <jsp:param name="relativePath" value="${relativePath}" />
                </jsp:include>

		<jsp:include page="js/bootstrap-datepicker/package.jsp">
			<jsp:param name="relativePath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="<%= development%>" />
		</jsp:include>

		<jsp:include page="js/proj4js/package.jsp">
                    <jsp:param name="relativePath" value="${relativePath}" />
                </jsp:include>
                
		<script src="${relativePath}app/DeclusterCanvas.js" type="text/javascript"></script>

		<jsp:include page="js/dygraphs/dygraphs.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>

		<script type="text/javascript">
			var CONFIG = {};

			CONFIG.development = ${development};
			CONFIG.relativePath = '${relativePath}';
			CONFIG.networkName = '${networkName}';
			CONFIG.stationName = '${stationName}';
			CONFIG.networkHoursOffset = ${networkHoursOffset};

		</script>

		<jsp:include page="app/gcmrc.jsp"></jsp:include>
		<jsp:include page="js/angular-sortable/package.jsp">
			<jsp:param name="relativePath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>
		<script type="text/javascript">
			var gcmrcModule = angular.module('gcmrc', ['ui.sortable', "ui.bootstrap"]);
		</script>
		<script src="${relativePath}services/rest/station/allsite/${networkName}?jsonp_callback=GCMRC.StationLoad"></script>
		<jsp:include page="app/graphing/package.jsp">
			<jsp:param name="relativePath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="${development}" />
		</jsp:include>
		<jsp:include page="pages/page.jsp">
			<jsp:param name="relativePath" value="${relativePath}" />
			<jsp:param name="pageName" value="${pageName}" />
		</jsp:include>
		<script src="${relativePath}services/rest/reach/trib/${stationName}?jsonp_callback=GCMRC.Page.reachLoad"></script>
		<script src="${relativePath}services/rest/lookup/ancillary?jsonp_callback=GCMRC.Page.ancillaryLoad"></script>
		<script src="${relativePath}services/rest/station/param/${stationName}?jsonp_callback=GCMRC.Page.paramsLoad"></script>
		<script src="${relativePath}services/rest/param/bs/${stationName}?jsonp_callback=GCMRC.Page.bsLoad"></script>
		<script src="${relativePath}services/rest/param/qw/${stationName}?jsonp_callback=GCMRC.Page.qwAndDiscMeasurementLoad"></script>
		<script src="${relativePath}services/rest/param/dischargeError/${stationName}?jsonp_callback=GCMRC.Page.qwAndDiscMeasurementLoad"></script>
		<script src="${relativePath}services/rest/station/credit/${stationName}?jsonp_callback=GCMRC.Page.creditLoad"></script>
		<script src="${relativePath}services/rest/station/pubs/${stationName}?jsonp_callback=GCMRC.Page.pubsLoad"></script>
		
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
    <body ng-app="gcmrc">
		<div class="container-fluid">
			<jsp:include page="template/GCMRCHeader.jsp">
				<jsp:param name="relativePath" value="${relativePath}" />
				<jsp:param name="header-class" value="" />
			</jsp:include>
			<div>
				<h2>Grand Canyon Monitoring and Research Center</h2>				
				<h3><span id="station-title"></span> <small>${stationName}</small></h3>
				<% if (!warningMessage.isEmpty()) { %>
					<div class="alert alert-danger site-alert"><b>NOTICE:&nbsp;</b><%= warningMessage%></div>
				<% } %>
				<div id="breadcrumbs">
					<span>
						<span><a href="https://www.gcmrc.gov/gcmrc.aspx">Home</a></span>
						<span> &gt; </span>
						<span><a href="${relativePath}index.jsp">Discharge, Sediment and Water Quality</a></span>
						<span> &gt; </span>
						<span><a href="${relativePath}stations/${networkName}/"><span class="network-name"></span> Stations</a></span>
						<span> &gt; </span>
						<span>${stationName}</span>
					</span>
				</div>
				<div id="content" class="row-fluid">
					<div class="span3">
						<div class="innerRightSidebar">
							<div class="sectionTitle">Parameter Availability</div>
							<div id="parameterLoading" class="ajaxLoading"><div class="alert alert-info">Loading...</div><div style="margin-bottom: 20px;"><img src="${relativePath}app/ajax-loader-trsp.gif" alt="Loading..." height="32px" width="32px"></div></div>
							<div id="parameterList" class="rightSidebarText"></div>
						</div>
					</div>
					<div class="span6" id="options-dygraph">
						<div class="innerRightSidebar">
							<div class="sectionTitle">Date Range</div>
							<form class="form-horizontal">
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
								<div id="build-div" style="text-align:center;"><a href="#" class="buildButton" name="endPosition" id="buildGraph">Build Graph</a><a href="#" class="buildButton" name="endPosition" id="downloadData">Download</a></div>
							</form>
						</div>
						<div id="viz" class="innerRightSidebar">
							<div class="sectionTitle">Data</div>
							<div class="dataChartView">
								<div id="errorMsg"></div>
								<div id="infoMsg">
									To visualize data in a graph, please select parameters from the left as well as a date range from above.
								</div>
								<div id="loading" class="ajaxLoading"><div class="alert alert-info">Your graphs are being constructed...</div><div style="margin-bottom: 20px;"><img src="${relativePath}app/ajax-loader-trsp.gif" alt="Loading..." height="32px" width="32px"></div></div>
								<div id="legend-dygraph"></div>
								<div id="data-dygraph"></div>
								<div id="legend-duration-curve"></div>
								<div id="data-duration-curve"></div>
							</div>
						</div>
					</div>

					<div class="span3">
						<div class="innerRightSidebar">
							<div class="sectionTitle">Location</div>
							<div id="locationArea">
								<div id="station-photo">
									<a href="${relativePath}photo/${networkName}/${stationName}/${stationName}_01.jpg"><img src="${relativePath}photo/${networkName}/${stationName}/${stationName}_01sm.jpg" width="100%"/></a>
								</div>
								<div id="openlayers-map"></div>
							</div>
						</div>
						<div id="addlSources" class="innerRightSidebar">
							<div class="sectionTitle">Additional Information</div>
							<ul id="addlSourcesBody">
							</ul>
							<ul id="addlPubsBody">
							</ul>
						</div>
						<div id="lastSedDates"></div>
					</div>
				</div>
				<div id="downloadPopup" ng:controller="downloadPopupController" class="modal hide">
					<div class="modal-header">
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
						<h3>Download Data</h3>
					</div>
					<div class="modal-body container-fluid">
						<tabset>
							<tab active="downloadTypes.continuousActive" disabled="downloadTypes.isOnlyPhysical">
								<tab-heading>Continuous Data</tab-heading>
								<div class="row-fluid">
									<div class="span6">
										<h5>Ordering</h5>
										<ul ui:sortable="{items:'li.reorderable'}" ng:model="columnOrdering" id="downloadColumnOrdering">
											<li ng:repeat="el in columnOrdering" ng:click="liClicked()" ng-class="{selected:isSelected(), reorderable:el.reorderable}" class="ui-state-default">
												<button type="button" class="close removeColumn" ng:click="removeColumn()" aria-hidden="true" title="Remove Column">&times;</button>
												<button type="button" class="close addColumn" ng:click="addColumn()" aria-hidden="true" title="Copy Column">+</button>
												<span class="ui-icon ui-icon-arrowthick-2-n-s" ng-show='el.reorderable'></span>
												{{el.name}}
												<div ng-hide="el.nameConfig.useDefault" class="ui-state-disabled">({{el.nameConfig.customName}})</div>
											</li>
										</ul>
									</div>
									<div id="downloadColumnDetail" class="span6">
										<div ng-show="columnSelected">
											<h5>{{columnSelected.name}} options</h5>
											<div ng-show="columnSelected.nameConfig">
												<hr>
												<h6>Naming</h6>
												<div><input type="checkbox" ng:model="columnSelected.nameConfig.useDefault">Use default name</div>
												<div><input type="checkbox" ng:model="columnSelected.nameConfig.useDefault" inverted><input type="text" ng:model="columnSelected.nameConfig.customName" ng-disabled="columnSelected.nameConfig.useDefault" placeholder="Specify custom name..."></div>
											</div>
											<div ng-show="columnSelected.formatConfig">
												<hr>
												<h6>Format</h6>
												<input type="text" help-tooltip ng:model="columnSelected.formatConfig.format">
											</div>
										</div>
									</div>
								</div>
								<div class="row-fluid">
									<a href="#" id="stupidlyBigDownloadButton" class='buildButton'>Download Data</a>
								</div>
							</tab>
							<tab active="downloadTypes.physicalActive" disabled="downloadTypes.isOnlyContinuous">
								<tab-heading>Physical Samples</tab-heading>
								<div class="row-fluid">
									<a href="#" id="physicalDownloadButton" class='buildButton'>Download Data</a>
								</div>
							</tab>
							<tab>
								<tab-heading>Bed Sediment</tab-heading>
								<div class="row-fluid">
									<a href="#" id="bedSedimentDownloadButton" class='buildButton'>Download Data</a>
								</div>
							</tab>
							<tab>
								<tab-heading>Duration Curves</tab-heading>
								<div class="row-fluid">
									<div class="datatype-select">
										<label for="bintype">Data Type</label>
										<input type="radio" name="bintype" value="log" checked="checked">Logarithmic</input>
										<input type="radio" name="bintype" value="lin">Linear</input>
										<input type="radio" name="bintype" value="both">Both</input>
									</div>
									<a href="#" id="durationCurveDownloadButton" class='buildButton'>Download Data</a>
								</div>
							</tab>
						</tabset>
					</div>
				</div>
			</div>
			<jsp:include page="template/GCMRCFooter.jsp">
				<jsp:param name="header-class" value="" />
				<jsp:param name="contact-info" value="" />
			</jsp:include>
		</div>
	</body>
</html>
