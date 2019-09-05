<%@page import="java.io.File"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Properties"%>
<%@page import="javax.naming.Context"%>
<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%@page import="gov.usgs.cida.gcmrc.util.PathUtil"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!private static final Logger log = LoggerFactory.getLogger("index_jsp");
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
%>

<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html lang="en" class="no-js"> <!--<![endif]-->
    <head>
        <jsp:include page="template/GCMRCHead.jsp">
			<jsp:param name="relPath" value="" />
			<jsp:param name="shortName" value="GCMRC" />
			<jsp:param name="title" value="" />
			<jsp:param name="description" value="" />
			<jsp:param name="author" value="" />
			<jsp:param name="keywords" value="test" />
			<jsp:param name="publisher" value="" />
			<jsp:param name="revisedDate" value="" />
			<jsp:param name="nextReview" value="" />
			<jsp:param name="expires" value="never" />
			<jsp:param name="development" value="${development}" />
		</jsp:include>
		<jsp:include page="app/libs.jsp"></jsp:include>

		<jsp:include page="js/openlayers/openlayers.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="debug-qualifier" value="<%= development%>" />
		</jsp:include>
		<script src="${relativePath}js/openlayers/extension/Renderer/DeclusterCanvas.js" type="text/javascript"></script>
		<script type="text/javascript">
			var CONFIG = {};

			CONFIG.development = <%= development%>;
			CONFIG.relativePath = '<%= relativePath%>';
		</script>

		<jsp:include page="app/gcmrc.jsp"></jsp:include>
		<jsp:include page="pages/page.jsp">
			<jsp:param name="relPath" value="${relativePath}" />
			<jsp:param name="pageName" value="${pageName}" />
		</jsp:include>

    </head>
    <body>
		<div class="container-fluid">
			<jsp:include page="template/GCMRCHeader.jsp">
				<jsp:param name="relPath" value="${relativePath}" />
				<jsp:param name="header-class" value="" />
			</jsp:include>
				<!--[if lt IE 7]>
				<p class="chromeframe">You are using an outdated browser. <a href="http://browsehappy.com/">Upgrade your browser today</a> or <a href="http://www.google.com/chromeframe/?redirect=true">install Google Chrome Frame</a> to better experience this site.</p>
				<![endif]-->
				<div class="application-body">
					<h2>Grand Canyon Monitoring and Research Center</h2>
					<h3>Discharge, Sediment, and Water Quality Monitoring</h3>
					<% if (!warningMessage.isEmpty()) { %>
						<div class="alert alert-danger site-alert"><b>NOTICE:&nbsp;</b><%= warningMessage%></div>
					<% } %>
					<div id="breadcrumbs" class="row-fluid">
						<span>
							<span><a href="https://www.gcmrc.gov/gcmrc.aspx">Home</a></span>
							<span> &gt; </span>
							<span>Discharge, Sediment and Water Quality</span>
						</span>
					</div>
					<div id="content" class="row-fluid">
						<div class="span9">
							<div class="well">
								<div id="openlayers-map"></div>
							</div>
						</div>
						<div id="networkListHolder" class="span3">
							<div class="sectionWideTitle">Networks</div>
							<div id="networkList" class="well"></div>
						</div>
					</div>
					<div id="credits" class="row-fluid">
						<div class="span4">
							<div class="sectionWideTitle">For Additional Information</div>
							<div class="well full">
								<span>For additional information, please contact</span>
								<div class="media">
									<div class="media-body">
										<h5 class="media-heading">Project Chief</h5>
										<ul class="unstyled">
											<li>David Topping
												<ul>
													<li>USGS Southwest Biological Science Center</li>
													<li>Grand Canyon Monitoring Research Center</li>
													<li>Contact <a href="mailto:dtopping@usgs.gov">dtopping@usgs.gov</a> or (928)556-7396</li>
												</ul>
											</li>
										</ul>
									</div>
								</div>
								<div class="media">
									<div class="media-body">
										<h5 class="media-heading">Website Design and Programming</h5>
										<ul class="unstyled">
											<li>
												<a href="https://www.usgs.gov/science/mission-areas/water">USGS Water Mission Area</a>
												<ul>
													<li>Contact <a href="mailto:cida_gcmrc@usgs.gov">cida_gcmrc@usgs.gov</a></li>
												</ul>
											</li>
										</ul>
									</div>
								</div>
							</div>
						</div>
						<div class="span4">
							<div class="sectionWideTitle">Cooperating Agencies and Academic Institutions</div>
							<div class="well half">
								<div class="media">
									<a href="https://www.usbr.gov/">
										<div class="media-object pull-left" style="width:106px; text-align: center;"><img src="./template/images/source-usbr.gif"></div>
										<div class="media-body">
											<h4 class="media-heading">Bureau of Reclamation</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://www.nps.gov/">
										<div class="media-object pull-left" style="width:106px; text-align: center;"><img src="./template/images/source-nps.gif"></div>
										<div class="media-body">
											<h4 class="media-heading">National Park Service</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="http://www.usace.army.mil/">
										<div class="media-object pull-left" style="width:106px; text-align: center;"><img src="./template/images/source-ace.jpg"></div>
										<div class="media-body">
											<h4 class="media-heading">US Army Corps of Engineers</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://www.blm.gov/">
										<div class="media-object pull-left" style="width:106px; text-align: center;"><img src="./template/images/source-blm.jpg"></div>
										<div class="media-body">
											<h4 class="media-heading">Bureau of Land Management</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="http://www.cec.org/">
										<div class="media-object pull-left" style="width:106px; text-align: center;"><img src="./template/images/source-cec.gif"></div>
										<div class="media-body">
											<h4 class="media-heading">Commission for Environmental Cooperation</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="http://www.usu.edu/">
										<div class="media-object pull-left" style="width:106px; text-align: center;"><img src="./template/images/source-usu.gif"></div>
										<div class="media-body">
											<h4 class="media-heading">Utah State University</h4>
										</div>
									</a>
								</div>
							</div>
							<div class="sectionWideTitle">Cooperating USGS Water Science Centers</div>
							<div class="well half">
								<div class="media">
									<a href="https://az.water.usgs.gov/">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">Arizona Water Science Center</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://co.water.usgs.gov/">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">Colorado Water Science Center</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://mn.water.usgs.gov/">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">Minnesota Water Science Center</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://nm.water.usgs.gov/">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">New Mexico Water Science Center</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://ut.water.usgs.gov/">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">Utah Water Science Center</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://tx.usgs.gov/">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">Texas Water Science Center</h4>
										</div>
									</a>
								</div>
								<div class="media">
									<a href="https://www.usgs.gov/centers/wisconsin-water-science-center">
										<!--<img class="media-object pull-left" src="app/sources/source-usgs.gif">-->
										<div class="media-body">
											<h4 class="media-heading">Wisconsin Water Science Center</h4>
										</div>
									</a>
								</div>
							</div>
						</div>
						<div class="span4">
							<div class="sectionWideTitle">Terms of Use</div>
							<div class="well full">
								<p>
									The data presented in this website are collected 
									and processed using standard USGS protocols 
									and other established peer-reviewed methods, 
									and subject to rigorous quality control.  
									Nevertheless, minor edits of these data are 
									possible.  
								</p>
								<p>
									The data are released on the condition that 
									neither the USGS nor the U.S. Government may 
									be held liable for any damages resulting from 
									its authorized or unauthorized use.
								</p>
							</div>
						</div>
					</div>
					<div id="networkPopup" class="modal hide">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
							<h3><span class="network-name"></span> Network</h3>
						</div>
						<div class="modal-body">
							<ul>
								<li><a href="#" id="networkStation">Monitoring Stations</a></li>
								<li><a href="#" id="networkReach">Sediment Budget Reaches</a></li>
								<li><a href="#" id="networkResearchFolder">Ancillary Sediment Data</a></li>
								<li><a href="#" id="networkTopoSurveyFolder">Topographic Surveys</a></li>
								<li><a href="#" id="networkGisFolder">GIS Data</a></li>
								<li><a href="#" id="networkPhotoFolder">Historical Photographs</a></li>
								<li><a href="#" id="networkRainGaugeFolder">Rain Gauge Network</a></li>
							</ul>
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
