<%@page import="java.util.Collections"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="javax.naming.Context"%>
<%@page import="org.slf4j.Logger"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="gov.usgs.cida.gcmrc.util.PropertiesLoader"%>
<%!private static final Logger log = LoggerFactory.getLogger("gcmrc_jsp");
	protected PropertiesLoader propertiesLoader = new PropertiesLoader();
	protected Context context = propertiesLoader.getContextProps();
	
	protected Map<String, Boolean> features = new HashMap<String, Boolean>();
	
	{
		features.put("CANYONLANDS", Boolean.parseBoolean(propertiesLoader.getProp(context, "gcmrc.features.canyonlands", "false")));
	}%>
<script type="text/javascript">
var GCMRC = {
	administrator : 'cida_gcmrc',
	version : '${version}'
};
</script>
<link rel="shortcut icon" type="image/ico" href="${relativePath}app/favicon.ico" />

<script type="text/javascript" src="${relativePath}app/jslResourceLoad.js"></script>
<script type="text/javascript">
GCMRC.Stations = {
	"00000000" : {
		"hidden" : true,
		"network" : "none",
		"nwisSite" : "00000000",
		"displayName" : "ERROR",
		"researchItemId" : "ERROR",
		"photoItemId" : "ERROR",
		"topoSurveyId" : "ERROR",
		"gisDataId" : "ERROR",
		"rainGaugeId" : "ERROR",
		"lat" : 43.0731,
		"lon" : -89.4011
	},
	"08374500" : {
		"moreData" : '<li>Find data for this site in the <a href="http://www.ibwc.gov/wad/DDQTERLI.htm">IBWC historical dataset</a> or the <a href="http://www.ibwc.gov/wad/374500_a.txt">IBWC realtime dataset</a></li>'
	},
	"08375000" : {
		"moreData" : '<li>Find data for this site in the <a href="http://www.ibwc.gov/wad/DDQJOHNS.htm">IBWC historical dataset</a> or the <a href="http://www.ibwc.gov/wad/375000_a.txt">IBWC realtime dataset</a></li>'
	}
};
GCMRC.StationLoad = JSL.ResourceLoad(function(el) {
	var name = "00000000";
			
	if (el.siteName) {
		name = el.siteName;
	}
	
	el.key = name;

	GCMRC.Stations[name] = Object.merge({}, GCMRC.Stations[name], true).merge(el, true);
});

GCMRC.Features = {
	active : {
		"CANYONLANDS" : <%= (null != features.get("CANYONLANDS") && Boolean.TRUE == features.get("CANYONLANDS"))?"true":"false" %>
	},
	checkFeature : function(obj) {
		var result = false;
		
		if (!obj.appFeatureId || GCMRC.Features.active[obj.appFeatureId]) {
			result = true;
		}
		
		return result;
	}
};
GCMRC.doShowCalcSandBedload = function(netName) {
	var CALC_SAND_BEDLOAD_NETWORKS = ['DINO'];
	return netName != null && CALC_SAND_BEDLOAD_NETWORKS.includes(netName.toUpperCase());
}

CONFIG.instColor = "#4DAF4A";
CONFIG.instHiColor = "#FF0033";
CONFIG.pumpColor = "#A6CEE3";
CONFIG.pumpHiColor = "#1F78B4";
CONFIG.sampColor = "#FF7F00";
CONFIG.sampHiColor = "#FF9900";

CONFIG.delims = {
	sampleMethod : "!"
};
</script>
<script type="text/javascript" src="${relativePath}app/networkdata.js"></script>
<script type="text/javascript" src="${relativePath}app/reachdata.js"></script>
<script type="text/javascript" src="${relativePath}app/mapping.js"></script>
<script type="text/javascript" src="${relativePath}app/orgs.js"></script>

<script type="text/javascript" src="${relativePath}app/onReady.js"></script>
