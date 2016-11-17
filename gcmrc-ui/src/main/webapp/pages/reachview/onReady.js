$(document).ready(function onReady() {
	$("span.network-name").html(GCMRC.Networks[CONFIG.networkName].displayName);
	var upstreamStation = GCMRC.Stations[CONFIG.upstreamStationName];
	var downstreamStation = GCMRC.Stations[CONFIG.downstreamStationName];
	var upstreamSecondaryStation = GCMRC.Stations[GCMRC.Page.reach.upstreamSecondaryStation];

	$('#station-title').html(GCMRC.Page.reach.reachName);

	$('#loading').hide();
	$('#parameterLoading').hide();

	GCMRC.Page.addCredits();

	GCMRC.Page.createMiniMap({
		upstreamStationName: (!upstreamStation) ? null : upstreamStation.siteName,
		downstreamStationName: (!downstreamStation) ? null : downstreamStation.siteName,
		upstreamSecondaryStation: (!upstreamSecondaryStation) ? null : upstreamSecondaryStation.siteName
	});
	
	GCMRC.Page.buildPORView($('#porContainer'), GCMRC.Page.earliestPositionISO, GCMRC.Page.latestPositionISO);
        
        if ("DINO" === CONFIG.networkName) {
	
        $('#bedLoadSlider').children().first().text('Sand Bedload Included in Sand Budget');
        GCMRC.Page.buildRadioInfo($('#bedLoadList'));
        
        } else {
	var bedLoadList = [GCMRC.Page.sliderConfig.bedLoad];
	
	GCMRC.Page.createParameterList($('#bedLoadList'), bedLoadList);
        }
	GCMRC.Page.createDateList($('#lastSedDates'), GCMRC.Page.reach);
	
	var getParamList = function() {
		var result = [
			
		];
		
		var addToResult = {
			sandRiverLoad : false,
			finesRiverLoad : false,
			sandMajor : false,
			finesMajor : false,
			sandMinor : false,
			finesMinor : false
		}
		
		GCMRC.Page.reachDetail.each(function(el) {
			if (el.reachGroup === "S Sand Cumul Load") {
				addToResult["sandRiverLoad"] = true;
				if (el.majorStation) {
					addToResult["sandMajor"] = true;
				}
				if (el.minorStation) {
					addToResult["sandMinor"] = true;
				}
			}
			if (el.reachGroup === "S Fines Cumul Load") {
				addToResult["finesRiverLoad"] = true;
				if (el.majorStation) {
					addToResult["finesMajor"] = true;
				}
				if (el.minorStation) {
					addToResult["finesMinor"] = true;
				}
			}
		});
		
		if (addToResult["sandRiverLoad"]) {
			result.push(GCMRC.Page.sliderConfig.riverLoad);
		}
		if (addToResult["finesRiverLoad"]) {
			result.push(GCMRC.Page.sliderConfig.riverFinesLoad);
		}
		if (addToResult["sandMajor"]) {
			result.push(GCMRC.Page.sliderConfig.majorTribLoad);
		}
		if (addToResult["finesMajor"]) {
			result.push(GCMRC.Page.sliderConfig.majorTribFinesLoad);
		}
		if (addToResult["sandMinor"]) {
			result.push(GCMRC.Page.sliderConfig.minorTribLoad);
		}
		if (addToResult["finesMinor"]) {
			result.push(GCMRC.Page.sliderConfig.minorTribFinesLoad);
		}
		
		return result;
	}
	
	
	GCMRC.Page.createParameterList($('#parameterList'), getParamList());
	
	$('#buildGraph').click(GCMRC.Page.buildGraphClicked);
	$('#downloadData').click(GCMRC.Page.downloadDataClicked);

	$('#beginDatePicker').attr('value', new Date(GCMRC.Page.latestPosition).utc().addMonths(-1).format('{yyyy}-{MM}-{dd}'));
	$('#beginDatePicker').datepicker({
		format: 'yyyy-mm-dd',
		startDate: new Date(GCMRC.Page.earliestPosition),
		endDate: new Date(GCMRC.Page.latestPosition),
		startView: 0
	});
	$('#endDatePicker').attr('value', new Date(GCMRC.Page.latestPosition).utc().format('{yyyy}-{MM}-{dd}'));
	$('#endDatePicker').datepicker({
		format: 'yyyy-mm-dd',
		startDate: new Date(GCMRC.Page.earliestPosition),
		endDate: new Date(GCMRC.Page.latestPosition),
		startView: 0
	});

	$('#resetDefaults').click(GCMRC.Page.resetSliders);

	$('#loading').ajaxStart(function() {
		$(this).show();
	}).ajaxStop(function() {
		$(this).hide();
	});
});
