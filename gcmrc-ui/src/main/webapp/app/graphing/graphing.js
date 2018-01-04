GCMRC.Dygraphs.overrides(CONFIG.networkHoursOffset);

GCMRC.Graphing = function(hoursOffset) {
	var blockRedraw = false;
	var blockHighlight = false;

	var graphs = {};
	
	var durationCurves = {
	};
	
	var durationCurveConfiguration = {};

	var isResizeListenerAttached = false;

	var urls = {
		agg: 'services/agg/',
		durationCurve: 'services/rest/durationcurve/'
	};
	
	// Don't show duration curve plot option for bed sediment or any cumulative timeseries
	var NO_DURATION_CURVE_IDS = ["3", "4", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "58", "61", "115", "117"];

	var showInfoMessage = function(locator, msg) {
		$(locator).append('<div class="alert alert-info"><button type="button" class="close" data-dismiss="alert">×</button>' + msg + '</div>');
	};
	
	var showWarningMessage = function(msg) {
		$("#errorMsg").append('<div class="alert"><button type="button" class="close" data-dismiss="alert">×</button>' + msg + '</div>');
	};
	var clearWarningMessage = function(msg) {
		$("#errorMsg").empty();
	};

	var showErrorMessage = function(msg) {
		$("#errorMsg").append('<div class="alert alert-error"><button type="button" class="close" data-dismiss="alert">×</button>' + msg + '</div>');
	};
	var clearErrorMessage = function(msg) {
		$("#errorMsg").empty();
	};
	
	var isNaN = function(x) {
		return x !== x;
	};
	
	var isNullUndefinedOrNaN = function(x, undefined) {
		return (x === null ||
				x === undefined ||
				isNaN(x));
	};
	
	var dealWithDurationCurveResponse = function(graphToMake, relevantData, config, buildGraph) {
		var conf = $.extend({}, config);		
		var identifier = graphToMake;
		var parameterMetadata = GCMRC.Page.params[identifier].description;
		var graphName = parameterMetadata['displayName'];
		var hasData = false;
		var displayData = new Array();
		var logDataExcluded = false;
		
		if(Array.isArray(relevantData.points) && relevantData.points.length > 0){
			hasData = true;
		}
		
		if (hasData) {
			var minY = parseFloat(relevantData.points[0].lowBound);
			var maxY = parseFloat(relevantData.points[0].lowBound);
			
			relevantData.points.forEach(function(point){
				var xVal = parseFloat(point.cumulativeBinPerc);
				var yVal = parseFloat(point.lowBound);
				
				//Logarithmic Plots should not show bins that are negative or 0
				if(relevantData.binType.toLowerCase() === "lin" || yVal > 0){
					if(yVal > maxY){
						maxY = yVal;
					} else if(yVal < minY){
						minY = yVal;
					}


					displayData.push([xVal, [yVal, yVal, yVal]]);
				} else {
					logDataExcluded = true;
				}
			});
			
			if(displayData.length > 0) {
				displayData.reverse();
			
				conf.labels = ["Percentage", "Value"];

				conf['yAxisLabel'] = graphToMake.yAxisLabel || graphName + " (" + parameterMetadata['unitsShort'] + ")";
				
				//Need a minimum of 1 decimal place for Duration Curves
				conf['decimalPlaces'] = parameterMetadata['decimalPlaces'] > 1 ? parameterMetadata['decimalPlaces'] : 1;
				conf['dataformatter'] = GCMRC.Dygraphs.DataFormatter(conf['decimalPlaces']);
				
				conf["parameterName"] = identifier;
				conf["labelDiv"] = $('#' + conf.labelDivId + ' div.duration-plot-' + identifier).get(0);
				conf["colors"] = durationCurveConfiguration[identifier].colors;
				conf["highlightColor"] = durationCurveConfiguration[identifier].highlightColor.values()[0];
				conf["data"] = displayData;
				conf["minY"] = minY;
				conf["maxY"] = maxY;

				//Logarithmic vs Linear Plots
				if(relevantData.binType.toLowerCase() === "log"){
					conf["div"] = $('#' + conf.divId + ' div.duration-plot-' + identifier + '[id=log]').get(0);
					buildGraph(conf, true);
				} else {
					conf["div"] = $('#' + conf.divId + ' div.duration-plot-' + identifier + '[id=lin]').get(0);
					buildGraph(conf, false);
				}
			}
			
			if(logDataExcluded){
				var div = '#' + conf.divId + " .plot-container-" + identifier;
				showInfoMessage(div, "Logarithmic duration curves only account for data points with a value greater than or equal to 0.1." + 
						" In order to see a full duration curve for this parameter over this time period please check the linear view.");
			}
		}
	};
	
	var dealWithResponse = function(graphToMake, data, config, buildGraph) {
		var parseColData = function(str) {
			var result = null;
			var low = null, med = null, high = null;

			if (str) {
				var strSplit = str.split(';');
				if (2 < strSplit.length) {
					low = parseFloat(strSplit[0]);
					med = parseFloat(strSplit[1]);
					high = parseFloat(strSplit[2]);
				} else {
					med = parseFloat(str);
				}
			}

			if (!isNullUndefinedOrNaN(med)) {
				if (isNullUndefinedOrNaN(low)) {
					low = med;
				}
				if (isNullUndefinedOrNaN(high)) {
					high = med;
				}
			}
			result = [low, med, high];

			return result;
		};
		var timeColumn = "time";
		var conf = $.extend({}, config);
		var hasData = false;
		var bedloadData;
		if (graphToMake.groupId === 'sandbudget') {
			    bedloadData = GCMRC.Page.bedloadCoeffData;
			}
		conf.data = data.success.data.map(function(el) {
			var result;
			
			result = [parseInt(el[timeColumn])];
			
			var columns = graphToMake.responseColumns || graphToMake.columns;
			var dataColumns = columns.filter(function(n){return !n.startsWith(timeColumn);}).map(function(col) {
				return parseColData(el[col]);
			});

			[].push.apply(result, dataColumns);


			if (!hasData && dataColumns.reduce(function(a, b) {
				return a || b.some(function(e){
					return e === 0 || !!e;
				});
			}, false)) {
				hasData = true;
			}

			return result;
		});
		
		var identifier = graphToMake.groupId;
		var parameterMetadata = GCMRC.Page.params[identifier].description;
		var graphName = parameterMetadata['displayName'];
		
		if (hasData) {
			conf.labels = [timeColumn];
			[].push.apply(conf.labels, graphToMake.columns.filter(function(n){return !n.startsWith(timeColumn)}).map(function(el) {
				var param = GCMRC.Page.params[identifier][el.split("!")[0]];
				var result = ("inst" === param.sampleMethod) ? param.displayName : param.sampleMethod;
				return result;
			}));
			conf['yAxisLabel'] = graphToMake.yAxisLabel || graphName + " (" + parameterMetadata['unitsShort'] + ")";
			conf['dataformatter'] = GCMRC.Dygraphs.DataFormatter(parameterMetadata['decimalPlaces']);
			conf['decimalPlaces'] = parameterMetadata['decimalPlaces'];
			conf["parameterName"] = identifier;
			conf["div"] = $('#' + conf.divId + ' div.timeseries-plot-' + identifier).get(0);
			conf["labelDiv"] = $('#' + conf.labelDivId + ' div.timeseries-plot-' + identifier).get(0);
			conf["colors"] = [];
			conf["highlightColor"] = {};
			[].push.apply(conf.colors, graphToMake.columns.filter(function(n){return !n.startsWith(timeColumn)}).map(function(el) {
				var param = GCMRC.Page.params[identifier][el.split("!")[0]];
				var idiot = ("inst" === param.sampleMethod) ? param.displayName : param.sampleMethod;
				this[idiot] = param['highlightColor'];
				return param['color'];
			}, conf.highlightColor));

			conf["series"] = {};
			graphToMake.columns.filter(function(n){return !n.startsWith(timeColumn)}).map(function(el) {
				var param = GCMRC.Page.params[identifier][el.split("!")[0]];
				var idiot = ("inst" === param.sampleMethod) ? param.displayName : param.sampleMethod;
				this[idiot] = param.series;
			}, conf["series"]);
			
			//Save Configuration so Duration Curve Plots can access it
			durationCurveConfiguration[identifier] = conf;

			buildGraph(conf);
		} else {
			showInfoMessage("#" + conf.divId + ' div.timeseries-plot-' + identifier, "There were no data during this period for " + graphName);
		}

	};
	
	var buildGraph = function(config) {
		if (!config)
			config = {};

		var containerId = config.divId || "unknown";
		var parameterName = config.parameterName || "unknown";
		var div = config.div;
//		var labelDiv = config.labelDiv;
		
		var labels = config['labels'];
		
		var dateWindow = config['dateWindow'].clone();
		dateWindow[1] = dateWindow[1] + (24 * 60 * 60 * 1000) - 1;
		
		div.style.display = "inline-block";
		div.style.margin = "4px";

		var data = config['data'];

		var title = config['graphTitle'] || '';

		var yAxisLabel = config['yAxisLabel'] || 'Data';

		var dataformatter = config['dataformatter'] || GCMRC.Dygraphs.DataFormatter(0);
		var decimalPlaces = config['decimalPlaces'] || 0;
		
		var confColors = config['colors'] || [CONFIG.instColor, CONFIG.pumpColor, CONFIG.sampColor, CONFIG.sampColor];
		var highlightColor = config['highlightColor'];

		var axes = {
			y: {
				axisLabelFormatter: dataformatter,
				valueFormatter: dataformatter
			},
			x: {
				ticker: Dygraph.dateTicker,
				axisLabelFormatter: GCMRC.Dygraphs.timeFormatter(hoursOffset),
				valueFormatter: GCMRC.Dygraphs.timeLabelFormatter(hoursOffset),
				pixelsPerLabel: 85
			}
		};

		var lighterColorHighlightPoint = function(g, name, ctx, canvasx, canvasy, color, radius) {
			var lighterColor = highlightColor[name] || color;
			ctx.beginPath();
			ctx.fillStyle = lighterColor;
			ctx.arc(canvasx, canvasy, radius, 0, 2 * Math.PI, false);
			ctx.fill();
		};
				
		var opts = {
			title: title,
			labelsDivStyles: {
				'textAlign': 'right'
			},
			//width: graphWidth,
			height: 420,
			xlabel: 'Time',
			ylabel: yAxisLabel,
			yAxisLabelWidth: 85,
			xAxisHeight: 50,
			xAxisLabelWidth: 85,
			axes: axes,
			yRangePad: 5,
//			includeZero: true,
			labels: labels,
			dateWindow : dateWindow,
			originalDateWindow : dateWindow,
			//To be used in "fixed" legend
//			labelsDiv: labelDiv,
//			labelsSeparateLines: true,
//			legend: 'always',
			labelsDivWidth: 300,
			showRangeSelector: true,
			connectSeparatedPoints: false,
			highlightCircleSize: 4,
			strokeWidth: 2,
			pixelsPerTimeLabel : 85,
			ticker: GCMRC.Dygraphs.ScaledTicker(decimalPlaces),
			drawHighlightPointCallback: lighterColorHighlightPoint,
			customBars: true,
			drawPoints: false,
			stackedGraph: false,
			colors: confColors,
			drawCallback: function(me, initial) {
				if (blockRedraw || initial)
					return;
				blockRedraw = true;
				var range = me.xAxisRange();
				$.each(graphs[containerId], function(key, val) {
					if (val !== me) {
						val.updateOptions({
							dateWindow: range
						});
					}
				});
				blockRedraw = false;
			},
			highlightCallback: function(event, x, points, row, seriesName) {
				if (blockHighlight) {
					return;
				}
				blockHighlight = true;
				$.each(graphs[containerId], function(key, graph) {
					graph.setSelection(row);
					var canvasx;
					if (graph && graph.selPoints_.length > 0 && graph.selPoints_[0]) {
						canvasx = graph.selPoints_[0].canvasx;
					} else {
						//if no selected point?  TODO, is this needed now that we interpolate?
						if (points && points.length > 0 && points[0]) { 
							canvasx = points[0].canvasx;
						} else {
							var canvasCoords = graph.eventToDomCoords(event);
							canvasx = canvasCoords[0];
						}
					}
					var ctx = graph.canvas_ctx_;
					ctx.fillStyle = '#FF0000';
					ctx.fillRect(canvasx - 0.5, 0, 1, graph.height_);
				});
				blockHighlight = false;
			},
			unhighlightCallback: function(event, x, points, row, seriesName) {
				if (blockHighlight) {
					return;
				}
				blockHighlight = true;
				$.each(graphs[containerId], function(key, val) {
					val.clearSelection();
				});
				blockHighlight = false;
			}
		};
		
		if (config.series) {
			opts.series = config.series.clone();
		}
		
		graphs[containerId][parameterName] = new Dygraph(
				div,
				data,
				opts
				);
	};

	var buildDurationCurve = function(config, logScale) {
		if (!config)
			config = {};

		var containerId = config.divId || "unknown";
		var parameterName = config.parameterName || "unknown";
		var div = config.div;
//		var labelDiv = config.labelDiv;
		
		var labels = config['labels'];
		var minY = config['minY'];
		var maxY = config['maxY'];
		
		var logScaleParam = logScale ? "log" : "lin";
		
		if(!durationCurves[containerId][logScaleParam]){
			durationCurves[containerId][logScaleParam] = {};
		}
		
		//Determine if data should actually be shown on a logarithmic plot
		//Needed because Dygraphs has issues zooming on log graphs that don't have large enough data ranges
		if((Math.abs(Math.floor(Math.log10(maxY)) - Math.floor(Math.log10(minY))) < 1) || (Math.abs(maxY - minY) < 10)){
			logScale = false;
		}
		
		div.style.display = "inline-block";
		div.style.margin = "4px";

		var data = config['data'];

		var title = config['graphTitle'] || '';

		var yAxisLabel = config['yAxisLabel'] || 'Data';

		var dataformatter = config['dataformatter'] || GCMRC.Dygraphs.DataFormatter(1);
		var decimalPlaces = config['decimalPlaces'] || 1;
		
		var confColors = config['colors'] || [CONFIG.instColor, CONFIG.pumpColor, CONFIG.sampColor, CONFIG.sampColor];
		var highlightColor = config['highlightColor'];

		var axes = {
			y: {
				axisLabelFormatter: dataformatter,
				valueFormatter: GCMRC.Dygraphs.DataFormatter(2),
				includeZero: false
			},
			x: {
				axisLabelFormatter: GCMRC.Dygraphs.DataFormatter(config.data[config.data.length-1][0] <= 10 ? 1 : 0),
				valueFormatter: GCMRC.Dygraphs.DataFormatter(2),
				pixelsPerLabel: 50,
				logscale: false
			}
		};

		var lighterColorHighlightPoint = function(g, name, ctx, canvasx, canvasy, color, radius) {
			var lighterColor = highlightColor;
			ctx.beginPath();
			ctx.fillStyle = lighterColor;
			ctx.arc(canvasx, canvasy, radius, 0, 2 * Math.PI, false);
			ctx.fill();
		};
				
		var opts = {
			title: title,
			labelsDivStyles: {
				'textAlign': 'right'
			},
			//width: graphWidth,
			logscale: logScale,
			height: 420,
			xlabel: 'Percentage of Time Equaled or Exceeded',
			ylabel: yAxisLabel,
			yAxisLabelWidth: 85,
			xAxisHeight: 50,
			axes: axes,
			yRangePad: 5,
			pointSize: 2,
			xRangePad: 2,
			includeZero: true,
			labels: labels,
			//To be used in "fixed" legend
//			labelsDiv: labelDiv,
//			labelsSeparateLines: true,
//			legend: 'always',
			labelsDivWidth: 200,
			dateWindow: [0, config.data[config.data.length-1][0] + 0.25],
			originalDateWindow: [0,config.data[config.data.length-1][0] + 0.25],
			showRangeSelector: true,
			connectSeparatedPoints: false,
			highlightCircleSize: 4,
			strokeWidth: 2,
			pixelsPerTimeLabel : 95,
			ticker: GCMRC.Dygraphs.ScaledTicker(decimalPlaces),
			drawHighlightPointCallback: lighterColorHighlightPoint,
			customBars: true,
			drawPoints: false,
			stackedGraph: false,
			colors: confColors,
			highlightCallback: function(event, x, points, row, seriesName) {
				if (blockHighlight) {
					return;
				}
								
				blockHighlight = true;
				var graph = durationCurves[containerId][logScaleParam][parameterName];
				
				if(!graph){
					return;
				}
										
				graph.setSelection(row);
				var canvasx;
				if (graph && graph.selPoints_.length > 0 && graph.selPoints_[0]) {
					canvasx = graph.selPoints_[0].canvasx;
					var ctx = graph.canvas_ctx_;
					ctx.fillStyle = '#FF0000';
					ctx.fillRect(canvasx - 0.5, 0, 1, graph.height_);
				} else {
					//if no selected point?  TODO, is this needed now that we interpolate?
					if (points && points.length > 0 && points[0]) { 
						canvasx = points[0].canvasx;
					} else {
						var canvasCoords = graph.eventToDomCoords(event);
						canvasx = canvasCoords[0];
					}
				}
				
				blockHighlight = false;
			},
			unhighlightCallback: function(event, x, points, row, seriesName) {
				if (blockHighlight) {
					return;
				}
				blockHighlight = true;
				var graph = durationCurves[containerId][logScaleParam][parameterName];
				graph.clearSelection();
				blockHighlight = false;
			}
		};
		
		if (config.series) {
			opts.series = config.series.clone();
		}
		
		durationCurves[containerId][logScaleParam][parameterName] = new Dygraph(
				div,
				data,
				opts
				);
	};
	
	var createDurationCurvePlot = function(param, config, urlParams) {
		
		//Filter out requested groupids that will never have duration curves (bed sediment params)
		var validIds = new Array();
		
		config.graphsToMake.forEach(function(elem) {
			if(NO_DURATION_CURVE_IDS.indexOf(elem) === -1){
				validIds.add(elem);
			}
		});
		
		urlParams.groupId = validIds;
		
		$.ajax({
			jsonp: "jsonp_callback",
			dataType: 'jsonp',
			url: CONFIG.relativePath + urls[param],
			data: urlParams,
			timeout: 1200000, /* 20 minutes allowed, from start to data complete */
			success: function(data, textStatus, jqXHR) {
				if (!data || (!data.contentType && data.contentType === "text/xml")) {
					clearErrorMessage();
					showErrorMessage("An error has occurred.  Please contact <a href='mailto:" + GCMRC.administrator + "@usgs.gov'>" + GCMRC.administrator + "@usgs.gov</a>");
				} else {
					if (data.data && !data.data.ERROR && data.data.points && data.data.groupId) {
						data = {
							success : {
								"@rowCount" : "-1",
								data : [
									data.data
								]
							}
						};
					}	
					//has valid data
					if (data.success && data.success.data && $.isArray(data.success.data)) {
					    var gapMinutes;
					    var consecutiveGapMinutes;
						//Build plots
						data.success.data.forEach(function(graph) {
							dealWithDurationCurveResponse(graph.groupId, graph, config, buildDurationCurve);
							gapMinutes = graph.gapMinutesPercent;
							consecutiveGapMinutes = graph.consecutiveGap;
						});
						
						//Add proper UI elements based on which duration curves were built
						urlParams.groupId.forEach(function(id) {
							var hasLin = false, hasLog = false;
							var div = $(".plot-container-"+id);
							
							if(div){
								if(GCMRC.Graphing.durationCurves[config.divId].lin[id]){
									hasLin = true;
									div.children('#lin').addClass("selected-duration-scale");
								}

								if(GCMRC.Graphing.durationCurves[config.divId].log[id]){
									hasLog = true;
									div.children('#log').addClass("selected-duration-scale");
									div.children('#lin').removeClass("selected-duration-scale");
								}
								
								if(hasLin || hasLog){
									$(createDurationCurveToggles(id, hasLin, hasLog)).prependTo(div);
									if (gapMinutes >= 60) {
									    $(createDurationCurveGapMinutesMessage(gapMinutes, consecutiveGapMinutes)).appendTo(div);
									}
								} else {
									clearErrorMessage();
									showErrorMessage("Duration curves could not be calculated for some of the selected parameters for the selected time period.");
								}
							}
						});
						
						//Hide Duration Curve Plots after building because TS Plots are the default
						$('div[class^="duration-plot"]').hide();

						//Show the Time Series Plots after everything is done building because they're on by default
						$('div[class^="timeseries-plot"]').show();
						
						//Force-Redraw Time Series Plots just in case the window was resized while they were hidden
						graphs[config.divId].values().forEach(function(graph){
							graph.updateOptions({});
							graph.resize();
						});
					} else {
						clearErrorMessage();
						showErrorMessage("An error occured while fetching duration curve data. Error: " + data.failure.error);
						//Show the time series plots
						$('div[class^="timeseries-plot"]').show();
					}
				}
			},
			error: function(jqXHR, textStatus, errorThrown) {
				clearErrorMessage();
				var msg = "";
				switch(textStatus) {
					case 'timeout':
						msg = "The browser timed out waiting for a response from the server.";
					break;
					case 'abort':
						msg = "The request was aborted.";
					break;
					case 'parsererror':
						msg = "A response was received, but it was unreadable.";
					break;
					case 'error':
					//break; fall thru
				default:
					msg = "Some type of server or network error occured.";
				}
				showErrorMessage("Your request could not be completed.  Reason: '" + msg + "'  If you repeatedly receive this message, contact <a href='mailto:" + GCMRC.administrator + "@usgs.gov'>" + GCMRC.administrator + "@usgs.gov</a>");
			}
		});
	};

	var createDurationCurveGapMinutesMessage = function(gapMinutes, consecutiveGap){
	    var consecutiveGapResult = '';
	    var consecGapMessage = "";
	    
	    consecutiveGapResult = consecutiveGap.gapTime + ' ' + consecutiveGap.gapUnit
	    

	    if (consecutiveGap) {
	        consecGapMessage = ' Longest consecutive period(s) of missing data: <br>' + consecutiveGapResult;
	    }
	    var gapMinutesMessage = '<div class="alert alert-info durationCurveMessage" style="display: none;"><button type="button" class="close" data-dismiss="alert">×</button>There are ' + gapMinutes + '% of minutes with no measured data in the selected time period. ' + consecGapMessage + '</div>';
	    
	    return gapMinutesMessage;
	};
	var createDurationCurveToggle = function(chartId) {
		return '<div onselectstart="return false" class="curveSelectButton toggle-switch-' + chartId + '" style="display: inline-block;">' +
					'<input class="curve-select" type="radio" id="chart-view-input-' + chartId + '" name="toggle-curve-' + chartId + '" checked="checked" value="chart">' +
					'<label for="chart-view-input-' + chartId + '" class="chart-view-label">Time Series Plot</label>' +
					'<input class="curve-select" type="radio" id="curve-view-input-' + chartId + '" name="toggle-curve-' + chartId + '" value="curve">' +
					'<label for="curve-view-input-' + chartId + '" class="curve-view-label">Duration Curve Plot</label>' +
				'</div>';
	};
	
	var createDurationCurveScaleToggle = function(chartId, hasLin, hasLog, gapMinutes, consecutiveGapMinutes) {
		var toReturn = "";		
		
		if(hasLin && hasLog) {
			toReturn = '<div onselectstart="return false" class="scaleSelectButton toggle-switch-' + chartId + '" style="display: none;">' +
							'<input class="scale-select" type="radio" id="log-view-input-' + chartId + '" name="toggle-scale-' + chartId + '" checked="checked" value="log">' +
							'<label for="log-view-input-' + chartId + '" class="log-scale-label">Logarithmic</label>' +
							'<input class="scale-select" type="radio" id="lin-view-input-' + chartId + '" name="toggle-scale-' + chartId + '" value="lin">' +
							'<label for="lin-view-input-' + chartId + '" class="lin-scale-label">Linear</label>' +
						'</div>';
				 
		} else if(hasLin) {
			toReturn = '<div onselectstart="return false" class="scaleSelectButton toggle-switch-' + chartId + '" style="display: none;">' +
							'<input class="scale-select" type="radio" id="lin-view-input-' + chartId + '" name="toggle-scale-' + chartId + '" checked="checked" value="lin">' +
							'<label for="lin-view-input-' + chartId + '" class="one-scale-only-label">Linear</label>' +
						'</div>';
		} else if(hasLog) {
			toReturn = '<div onselectstart="return false" class="scaleSelectButton toggle-switch-' + chartId + '" style="display: none;">' +
							'<input class="scale-select" type="radio" id="log-view-input-' + chartId + '" name="toggle-scale-' + chartId + '" checked="checked" value="log">' +
							'<label for="log-view-input-' + chartId + '" class="one-scale-label">Logarithmic</label>' +
						'</div>';
		}
		
		return toReturn;
	};
	
	var createDurationCurveToggles = function(chartId, hasLin, hasLog, gapMinutes, consecutiveGapMinutes){
		return createDurationCurveToggle(chartId) + createDurationCurveScaleToggle(chartId, hasLin, hasLog, gapMinutes, consecutiveGapMinutes);
	};
	
	return {
		graphs: graphs,
		durationCurves: durationCurves,
		urls: urls,
		isResizeListenerAttached : isResizeListenerAttached,
		showInfoMsg : showInfoMessage,
		showWarningMsg : showWarningMessage,
		clearWarningMsg : clearWarningMessage,
		showErrorMsg : showErrorMessage,
		clearErrorMsg : clearErrorMessage,
		NO_DURATION_CURVE_IDS : NO_DURATION_CURVE_IDS,
		createDataGraph: function(param, config, urlParams) {
			$('#infoMsg').empty();
			if (CONFIG.stationName === GCMRC.Page.oldDinosaurSite) {
			    showInfoMessage($('#infoMsg'), "The cumulative silt and clay load, cumulative suspended-sand load, and cumulative calculated sand bedload are calculated using suspended-sediment measurements from the Green River above Jensen, UT, station after April 12, 2016.");
			}
			$("#errorMsg").empty();

			if (urlParams['downscale']) {
				clearWarningMessage();
				showWarningMessage('Due to the length of your request, some timesteps may be filtered from the graph. You may still download the unfiltered dataset, or shorten the date range of your request.');
			}
			
			var containerDiv = $("#" + config.divId);
			var labelDiv = $("#" + config.labelDivId);

			graphs[config.divId] = {};
			durationCurves[config.divId] = {
				log: {},
				lin: {}
			};
			durationCurveConfiguration = {};
						
			/*
			 * Clean out and repopulated the container/graph divs
			 * for the correct display order
			 */
			containerDiv.empty();
			labelDiv.empty();		

			urlParams["output"] = "json";
			$.ajax({
				type: 'GET',
				dataType: 'json',
				url: CONFIG.relativePath + urls[param],
				data: urlParams,
				timeout: 2400000, /* 40 minutes allowed, from start to data complete */
				success: function(data, textStatus, jqXHR) {
					if (!data || (!data.contentType && data.contentType === "text/xml")) {
						clearErrorMessage();
						showErrorMessage("An error has occurred.  Please contact <a href='mailto:" + GCMRC.administrator + "@usgs.gov'>" + GCMRC.administrator + "@usgs.gov</a>");
					} else {
						if (data.data && !data.data.ERROR && data.data.time) {
							data = {
								success : {
									"@rowCount" : "-1",
									data : [
										data.data
									]
								}
							};
						}
						//success
						if (data.success && data.success.data && $.isArray(data.success.data)) {
							//Store individual chart divs for later populating with duration curve toggle
							GCMRC.Page.params.values().sortBy(function(n) {
								return parseInt(n.description.displayOrder || 9999999);
							}).map(function(n) {
								return n.description.groupId;
							}).forEach(function(el) {
								var plotDiv = $('<div class="plot-container-' + el + '"><div class="timeseries-plot-' + el + 
										'"></div><div id="log" class="duration-plot-' + el + 
										'"></div><div id="lin" class="duration-plot-' + el + 
										'"></div></div>');
								plotDiv.appendTo(containerDiv);
								labelDiv.append($('<div class="timeseries-plot-' + el +'"></div><div class="duration-plot-' + el +'"></div>'));
							});
														
							config.graphsToMake.forEach(function(graphToMake) {							
								//Build Plot
								if (graphToMake.dealWithResponse) {
									graphToMake.dealWithResponse(graphToMake, data, config, buildGraph);
								} else {
									dealWithResponse(graphToMake, data, config, buildGraph);
								}
							});
							
							//Only run this if duration curves should be generating
							if(config.durationCurveConf){
								//Hide TS plots after they've built until the duration curve plots are finished building.
								$('div[class^="timeseries-plot"]').hide();

								//Build List of Graphs that Populated and thus should have duration curves
								var durationCurveIds = graphs[config.divId].keys();

								config.durationCurveConf.graphsToMake = durationCurveIds;
								config.durationCurveConf.divId = config.divId;

								//Build Duration Curve Plots. Note: Non-blocking function because of AJAX call.
								createDurationCurvePlot('durationCurve', config.durationCurveConf, config.durationCurveParams);
							} else {
								//Force-Redraw Time Series Plots just in case the window was resized while they were hidden
								graphs[config.divId].values().forEach(function(graph){
									graph.updateOptions({});
									graph.resize();
								});
							}
							if (GCMRC.isDinoNetwork(CONFIG.networkName) && config.graphsToMake.some({groupId:'sandbudget'})) {
							    GCMRC.Page.bedloadToggleChange(Boolean(parseFloat($("input[name=bedloadToggle]:checked").val())));
							}
						} else if (data.data && data.data.ERROR) {
							clearErrorMessage();
							showErrorMessage("Please select a parameter to graph!");
						} else {
							LOG.error("An unknown error occured when fetching the data.");
							clearErrorMessage();
							showErrorMessage("An unknown error occured when fetching the data.");
						}
					}
				},
				error: function(jqXHR, textStatus, errorThrown) {
					clearErrorMessage();
					var msg = "";
					switch(textStatus) {
						case 'timeout':
							msg = "The browser timed out waiting for a response from the server.";
						break;
						case 'abort':
							msg = "The request was aborted.";
						break;
						case 'parsererror':
							msg = "A response was received, but it was unreadable.";
						break;
						case 'error':
						//break; fall thru
					default:
						msg = "Some type of server or network error occured.";
					}
					showErrorMessage("Your request could not be completed.  Reason: '" + msg + "'  If you repeatedly receive this message, contact <a href='mailto:" + GCMRC.administrator + "@usgs.gov'>" + GCMRC.administrator + "@usgs.gov</a>");
				}
			});
		}
	};
}(CONFIG.networkHoursOffset);