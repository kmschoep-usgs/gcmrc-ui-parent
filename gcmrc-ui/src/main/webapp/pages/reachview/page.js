CONFIG.everyPeriod = "P1M";

GCMRC.Page = {
	createMiniMap: function(config) {
		if (!config)
			config = {};

		var upstreamStationName = config['upstreamStationName'];
		var downstreamStationName = config['downstreamStationName'];
		var upstreamSecondaryStation = config['upstreamSecondaryStation'];
		var divId = config['divId'] || 'openlayers-map';

		var options = {};
		GCMRC.Mapping.maps[divId] = new OpenLayers.Map(divId, options);

		var layersToAdd = [];
		layersToAdd.push(GCMRC.Mapping.layers.esri.esriTopo);
		if (GCMRC.Mapping.layers.flowlines["zone" + GCMRC.Page.reach.displayOrder]) {
			layersToAdd.push(GCMRC.Mapping.layers.flowlines["zone" + GCMRC.Page.reach.displayOrder]);
		}
		layersToAdd.push(GCMRC.Mapping.layers.markers);

		GCMRC.Mapping.maps[divId].addLayers(layersToAdd);
		var makeIcon = function() {
			var size = new OpenLayers.Size(21, 25);
			var offset = new OpenLayers.Pixel(-(size.w / 2), -size.h);
			var icon = new OpenLayers.Icon(CONFIG.relativePath + 'app/marker.png', size, offset);

			return icon;
		};

		if (GCMRC.Stations[upstreamStationName]) {
			GCMRC.Mapping.layers.markers.addMarker(
					new OpenLayers.Marker(new OpenLayers.LonLat(GCMRC.Stations[upstreamStationName].lon, GCMRC.Stations[upstreamStationName].lat).transform(
					new OpenLayers.Projection("EPSG:4326"),
					GCMRC.Mapping.maps[divId].getProjectionObject()
					), makeIcon()));
		}

		if (GCMRC.Stations[downstreamStationName]) {
			GCMRC.Mapping.layers.markers.addMarker(new OpenLayers.Marker(
					new OpenLayers.LonLat(GCMRC.Stations[downstreamStationName].lon, GCMRC.Stations[downstreamStationName].lat).transform(
					new OpenLayers.Projection("EPSG:4326"),
					GCMRC.Mapping.maps[divId].getProjectionObject()
					), makeIcon()));
		}

		if (GCMRC.Stations[upstreamSecondaryStation]) {
			GCMRC.Mapping.layers.markers.addMarker(new OpenLayers.Marker(
					new OpenLayers.LonLat(GCMRC.Stations[upstreamSecondaryStation].lon, GCMRC.Stations[upstreamSecondaryStation].lat).transform(
					new OpenLayers.Projection("EPSG:4326"),
					GCMRC.Mapping.maps[divId].getProjectionObject()
					), makeIcon()));
		}

		GCMRC.Mapping.maps[divId].zoomToExtent(GCMRC.Mapping.layers.markers.getDataExtent());
	},
	buildPORView : function(div, earliest, latest) {
		var result = [
			'<div>',
			'Records exist from ',
			'<a href="#" class="fromdate">',
			earliest.split("T")[0],
			'</a>',
			' through ',
			'<a href="#" class="todate">',
			latest.split("T")[0],
			'</a>',
			'</div>'
		];
		div.append(result.join(''));
	},
	buildSliderInfo : function(div, dateStr, multiplier, loadDivKey, dataType) {
		div.append('<div>Uncertainty for ' + (("BIBE" === CONFIG.networkName)?"Tornillo Creek":"Major Tributary") + ' ' + dataType + ' Loads <span class="' + loadDivKey + '_qual' + multiplier + '"></span>% after ' + dateStr + '</div>');
	},
        buildRadioInfo : function(div) {
		div.append('<div class="form-inline"><label class="radio bedRadio"><input type="radio" name="bedLoadToggle" value="yes">Yes</label><label class="radio bedRadio"><input type="radio" name="bedLoadToggle" value="no">No</label></div>');
	},
	buildGraphClicked: function() {
		var NETWORK_DINO = "DINO";
		var begin = $("input[name='beginPosition']").val();
		var end = $("input[name='endPosition']").val();

		var endStaticRecMillis = new Date(GCMRC.Page.reach.endStaticRec).getTime() + (CONFIG.networkHoursOffset * 60 * 60 * 1000);
		var newestSuspSedMillis = new Date(GCMRC.Page.reach.newestSuspSed).getTime() + (CONFIG.networkHoursOffset * 60 * 60 * 1000);

		var beginMillis = Date.create(begin).getUTCTime() + (CONFIG.networkHoursOffset * 60 * 60 * 1000);
		var endMillis = Date.create(end).getUTCTime() + (CONFIG.networkHoursOffset * 60 * 60 * 1000);
		if (endMillis >= beginMillis) {
			var expectedGraphColumns = GCMRC.Page.getExpectedGraphColumns();
			var chosenParameters = ["time!UTCMillis"];
			expectedGraphColumns.forEach(function(el) {
				[].push.apply(this, el.columns.map(function(col) {
					return col;
				}).unique());
			}, chosenParameters);

			expectedGraphColumns.forEach(function(el) {
				var cols = [];
				[].push.apply(cols, el.columns.map(function(col) {
					var result = col;
					var colSplit = col.split("!");
					if (3 < colSplit.length) {
						result = colSplit[0] + "!" + colSplit[1] + "!" + colSplit[2] + "-" + colSplit[3];
					}

					return result;
				}));

				el.columns = cols;
			});

			var expectedStations = [];
			if (CONFIG.upstreamStationName) {
				expectedStations.push(CONFIG.upstreamStationName);
			}
			if (CONFIG.downstreamStationName) {
				expectedStations.push(CONFIG.downstreamStationName);
			}
			if (GCMRC.Page.reachDetail.majorStation) {
				expectedStations.push(GCMRC.Page.reachDetail.majorStation);
			}
			if (GCMRC.Page.reachDetail.minorStation) {
				expectedStations.push(GCMRC.Page.reachDetail.minorStation);
			}
			if (GCMRC.Page.reach.downstreamDischargeStation) {
				expectedStations.push(GCMRC.Page.reach.downstreamDischargeStation);
			}

			var serviceOptions = {
				beginPosition: begin,
				endPosition: end,
				column: chosenParameters,
				tz: '-' + CONFIG.networkHoursOffset,
				cutoffBefore: GCMRC.Page.earliestPositionISO,
				cutoffAfter: GCMRC.Page.latestPositionISO,
				every: CONFIG.everyPeriod,
				noDataFilter: 'true',
				useLagged: 'true'
			};

			var bedLoadServiceOptions = {
				beginPosition: begin,
				endPosition: end,
				column: 'Calc Inst Sand Bedload',
				tz: '-' + CONFIG.networkHoursOffset,
				cutoffBefore: GCMRC.Page.earliestPositionISO,
				cutoffAfter: GCMRC.Page.latestPositionISO,
				every: CONFIG.everyPeriod,
				noDataFilter: 'true',
				useLagged: 'true'
			};
                        
			var aggTime = GCMRC.Page.checkIfAgg(serviceOptions);

			if (aggTime) {
				serviceOptions['downscale'] = aggTime;
			}

			var addUncertaintyInformation = function(loadTypes) {
				if (GCMRC.Page.reach.endStaticRec && GCMRC.Page.reach.newestSuspSed) {
					loadTypes.keys(function(loadDivKey, loadType) {
						var thingthing = $('div.' + loadDivKey + '_qual');
						thingthing.empty();
						if ((endStaticRecMillis && endStaticRecMillis < endMillis) &&
								(!newestSuspSedMillis || (endStaticRecMillis < newestSuspSedMillis && newestSuspSedMillis > beginMillis))) {
							GCMRC.Page.buildSliderInfo(thingthing, GCMRC.Page.reach.endStaticRec.split("T")[0], 2, loadDivKey, loadType);
							$('.' + loadDivKey + '_qual2').html(parseFloat($('span[name=' + loadDivKey + '_val]').html()) * 2);
						}
						if (newestSuspSedMillis && newestSuspSedMillis < endMillis) {
							GCMRC.Page.buildSliderInfo(thingthing, GCMRC.Page.reach.newestSuspSed.split("T")[0], 4, loadDivKey, loadType);
							$('.' + loadDivKey + '_qual4').html(parseFloat($('span[name=' + loadDivKey + '_val]').html()) * 4);
						}
					});
				}
			};

			addUncertaintyInformation({'f' : 'Silt and Clay', 'c' : 'Sand'});
			
			GCMRC.Graphing.createDataGraph(
					'agg',
					{
						divId: 'data-dygraph',
						labelDivId: 'legend-dygraph',
						graphsToMake: expectedGraphColumns,
						endStaticRec : endStaticRecMillis,
						newestSuspSed : newestSuspSedMillis,
						dateWindow : [beginMillis, endMillis]
					},
			serviceOptions);

			if (CONFIG.networkName === NETWORK_DINO) {
			    GCMRC.Page.getBedLoadData(
				'agg',
				{bedLoad: 'Calc Inst Sand Bedload'},
			bedLoadServiceOptions);
			}
                        
			if (CONFIG.networkName === NETWORK_DINO) {
				GCMRC.Graphing.showInfoMsg("#infoMsg",'The sediment supplies from ungaged small tributaries are not included in these sediment budgets.  These tributaries transport relatively small amounts of silt and clay and negligible amounts of sand.  Not including the sediment supplied from these small tributaries therefore does not measurably affect the sand budgets, but does result in small negative step changes in the silt and clay budgets that are not the result of erosion.');
			}
			
		} else {
			GCMRC.Graphing.clearErrorMsg();
			GCMRC.Graphing.showErrorMsg("Please choose an End that is after Start");
		}
	},
	downloadDataClicked : function() {
		if (GCMRC.Graphing.graphs['data-dygraph']) {
			var form = $('#exportPost');
			var tabbedData = "Time" +"\t"+
					"Sand Mass Lower Uncertainty Bound"+"\t"+
					"Sand Mass Zero Bias Value"+"\t"+
					"Sand Mass Upper Uncertainty Bound"+"\n";
			GCMRC.Graphing.graphs['data-dygraph']["sandbudget"].file_.forEach(function(el) {
				tabbedData += el[0] + '\t' + el[1][0] + '\t' + el[1][1] + '\t' + el[1][2] + '\n';
			});

			form.find('[name=data]').val(tabbedData);
			form.submit();
		} else {
			LOG.debug("We don't have data to pull from!");
			GCMRC.Graphing.clearErrorMsg();
			GCMRC.Graphing.showErrorMsg("Please graph the data before downloading");
		}
	},
	fromDateClicked: function() {
		$('#beginDatePicker').val($(this).html());
	},
	toDateClicked: function() {
		var date = $(this).html();
		if ("Now" === date) {
			date = new Date();

			var formatted = {
				d: date.getUTCDate(),
				m: date.getUTCMonth() + 1,
				yyyy: date.getUTCFullYear()
			};

			formatted.dd = (formatted.d < 10 ? '0' : '') + formatted.d;
			formatted.mm = (formatted.m < 10 ? '0' : '') + formatted.m;

			date = [formatted['yyyy'], formatted['mm'], formatted['dd']].join('-');
		}
		$('#endDatePicker').val(date);
	},
	addCreditToList: function(e) {
		var org = GCMRC.Organizations[e];

		if (org) {
			var listing = [
				'<li><div><a href="',
				org.url,
				'"><img src="',
				org.logo.url,
				'" height="',
				org.logo.height,
				'px" width="',
				org.logo.width,
				'px">',
				org.displayName,
				'</a></div></li>'
			];

			$('#addlSourcesBody').append(listing.join(''));
		} else {
			LOG.info("Unknown Organizational Name, or no Additional Info associated with '" + e + "'");
		}
	},
	addCredits: function() {
		$('#addlSourcesBody').append('<b>Data provided by:</b>');
		GCMRC.Page.credits.forEach(GCMRC.Page.addCreditToList);
	},
	checkIfAgg: function(config) {
		var result = null;

		var conf = config || {};
		var begin = new Date(conf['beginPosition']).getTime();
		var end = new Date(conf['endPosition']).getTime();
		var cols = conf['column'] || [];

		var month = 1/*month*/ * 31/*days*/ * 24/*hours*/ * 60/*minutes*/ * 60/*seconds*/ * 1000/*millis*/;
		var year = 12 * month;
		var maxDifference = 6 * month;
		var minDifference = 2 * month;
		var interval = 2 * month;

		var diff = maxDifference;
		var i = (cols.length) - 1;
		diff = diff - (interval * i);
		if (diff < minDifference) {
			diff = minDifference;
		}

		var realDifference = end - begin;
		if (realDifference > diff) {
			if (realDifference > year) {
				result = 'PT24H';
			} else if (realDifference > maxDifference) {
				result = 'PT12H';
			} else {
				result = 'PT6H';
			}
		}

		return result;
	},
	sandworker: new Worker(CONFIG.relativePath + "app/graphing/matrixSandWorker.js"),
	isSandWorkerFed: false,
	finesworker: new Worker(CONFIG.relativePath + "app/graphing/matrixFinesWorker.js"),
	isFinesWorkerFed: false,
	latestSandReqId: 1,
	latestFinesReqId: 1,
	updateSandSummary: function(config) {
		if (config) {
			var upper = config.upper;
			var mid = config.mid;
			var lower = config.lower;

			var result = [
				'<div>',
				"<h3>Change in Sand Mass</h3>",
				"<ul><li>Zero Bias Value: <strong>",
				new Number(mid.toPrecision(2)).format(0, ",", "."),
				" Metric Tons</strong></li><li>Upper Uncertainty Bound: ",
				new Number(upper.toPrecision(2)).format(0, ",", "."),
				" Metric Tons</li><li>Lower Uncertainty Bound: ",
				new Number(lower.toPrecision(2)).format(0, ",", "."),
				" Metric Tons</li></ul>",
				"</div>"
			];

			var summaryDiv = $("#reach-sand-summary");
			summaryDiv.parent().addClass("well");
			summaryDiv.empty();
			summaryDiv.append(result.join(""));
		}
	},
	updateFinesSummary: function(config) {
		if (config) {
			var upper = config.upper;
			var mid = config.mid;
			var lower = config.lower;

			var result = [
				'<div>',
				"<h3>Change in Silt and Clay Mass</h3>",
				"<ul><li>Zero Bias Value: <strong>",
				new Number(mid.toPrecision(2)).format(0, ",", "."),
				" Metric Tons</strong></li><li>Upper Uncertainty Bound: ",
				new Number(upper.toPrecision(2)).format(0, ",", "."),
				" Metric Tons</li><li>Lower Uncertainty Bound: ",
				new Number(lower.toPrecision(2)).format(0, ",", "."),
				" Metric Tons</li></ul>",
				"</div>"
			];

			var summaryDiv = $("#reach-fines-summary");
			summaryDiv.parent().addClass("well");
			summaryDiv.empty();
			summaryDiv.append(result.join(""));
		}
	},
	drawBudget: function(config) {
		if (GCMRC.Page.isSandWorkerFed) {
			var msg = config.clone();
			msg.messageType = "transformArray";
			msg.reqId = ++GCMRC.Page.latestSandReqId;
			delete msg.graphsToMake;

			GCMRC.Page.sandworker.postMessage(msg);
		}
		if (GCMRC.Page.isFinesWorkerFed) {
			var msg = config.clone();
			msg.messageType = "transformArray";
			msg.reqId = ++GCMRC.Page.latestFinesReqId;
			delete msg.graphsToMake;

			GCMRC.Page.finesworker.postMessage(msg);
		}
	},
	getExpectedGraphColumns: function() {
		var reach = GCMRC.Page.reach;
		var budgetColumns = {};
		var responseColumns = {};
		//TODO Build Columns
		GCMRC.Page.reachDetail.each(function(elContainer) {
			var el = elContainer.reachGroup;
			budgetColumns[el] = [];
			budgetColumns[el].push("inst!" + el + "!" + reach.upstreamStation);
			budgetColumns[el].push("inst!" + el + "!" + reach.downstreamStation);			
			if (elContainer.majorStation) {
				budgetColumns[el].push("inst!" + elContainer.majorGroup + "!" + elContainer.majorStation);
			}
			if (elContainer.minorStation) {
				budgetColumns[el].push("inst!" + elContainer.minorGroup + "!" + elContainer.minorStation);
			}
			//check for secondary upstream sediment stations
			if (reach.upstreamSecondaryStation) {
				budgetColumns[el].push("inst!" + el + "!" +reach.upstreamSecondaryStation);
			}
			
			responseColumns[el] = [];
			responseColumns[el].push("inst!" + el + "-" + reach.upstreamStation);
			responseColumns[el].push("inst!" + elContainer.majorGroup + "-" + elContainer.majorStation);
			responseColumns[el].push("inst!" + elContainer.minorGroup + "-" + elContainer.minorStation);
			responseColumns[el].push("inst!" + el + "-" + reach.downstreamStation);
			//check for secondary upstream sediment station
			if (reach.upstreamSecondaryStation) {
				responseColumns[el].push("inst!" + el + "-" + reach.upstreamSecondaryStation);
			}
		});

		var result = [
			{
				groupId : 2,
				groupName: "Discharge",
				columns: ["inst!" + "Discharge" + "!" + GCMRC.Page.reach.downstreamDischargeStation],
				responseColumns : ["inst!" + "Discharge" + "-" + GCMRC.Page.reach.downstreamDischargeStation],
				yAxisLabel: "Discharge (cfs) at " + GCMRC.Page.reach.downstreamDischargeName
			}
		];

		var Budget = function(config) {
			this.config = config;
			this.groupId = config.budgetType;
			this.columns = config.budgetColumns;
			this.yAxisLabel = config.yAxisLabel;
			this.dealWithResponse = function(graphToMake, data, config, buildGraph) {
				var self = this;
				var datas = [];
				var times = [];

				var getValue = function(row, colName) {
					var result = 0.0;
					if (row[colName]) {
						result = parseFloat(row[colName]);
					}
					return result;
				};

				// add additional sediment station value for both DINO networks
				data.success.data.each(function(el) {
					var combinedValue = self.config.responseColumns.length > 4 ?
							getValue(el, self.config.responseColumns[0]) + 
							getValue(el, self.config.responseColumns[4]):
							getValue(el, self.config.responseColumns[0]);
					datas.push([combinedValue,
						getValue(el, self.config.responseColumns[1]),
						getValue(el, self.config.responseColumns[2]),
						getValue(el, self.config.responseColumns[3])]);
					times.push(getValue(el, "time"));
				});

				GCMRC.Page[self.config.workerName].postMessage({
					messageType: "setDataArray",
					divId: config.divId,
					data: datas,
					time: times,
					endStaticRec: config.endStaticRec,
					newestSuspSed: config.newestSuspSed
				});

				config.a = parseFloat($('span[name=a_val]').html()) / 100.0; //Consider not using these crappy alphabet vars?
				config.b = parseFloat($('span[name=b_val]').html()) / 100.0;
				config.c = parseFloat($('span[name=c_val]').html()) / 100.0;
				config.d = parseFloat($('span[name=d_val]').html()) / 100.0;
				config.e = parseFloat($('span[name=e_val]').html()) / 100.0;
				config.f = parseFloat($('span[name=f_val]').html()) / 100.0;
				config.g = parseFloat($('span[name=g_val]').html()) / 100.0;

				var createCallback = function(response) {
					var conf = config.clone();
					conf.data = response.data.dataArray;
					conf['yAxisLabel'] = graphToMake.yAxisLabel;
					conf["labels"] = ["Time", self.config.seriesName, "High", "Low"];
					conf['dataformatter'] = GCMRC.Dygraphs.DataFormatter(0);
					conf['decimalPlaces'] = 0;
					conf["parameterName"] = graphToMake.groupId;
					conf["div"] = $('#' + conf.divId + ' div.p' + graphToMake.groupId).get(0);
					conf["labelDiv"] = $('#' + conf.labelDivId + ' div.p' + graphToMake.groupId).get(0);
					conf["colors"] = [CONFIG.instColor, CONFIG.instColor, CONFIG.instColor];
					conf["highlightColor"] = {
						"High": CONFIG.instHiColor,
						"Low": CONFIG.instHiColor
					};
					conf["highlightColor"][self.config.seriesName] = CONFIG.instHiColor;
					conf["series"] = {
						"High": {
							strokeWidth: 0.0,
							drawPoints: false,
							highlightCircleSize: 3
						},
						"Low": {
							strokeWidth: 0.0,
							drawPoints: false,
							highlightCircleSize: 3
						}
					};
					buildGraph(conf);
				};

				GCMRC.Page[self.config.workerName].onmessage = function(response) {
					if (response.data && GCMRC.Page[self.config.reqIdName] === response.data.reqId) {
						if (GCMRC.Graphing.graphs[config.divId][self.config.budgetType]) {
							LOG.trace("update");
							GCMRC.Graphing.graphs[config.divId][self.config.budgetType].updateOptions({"file": response.data.dataArray});
						} else {
							LOG.trace("create");
							createCallback(response);
						}
						GCMRC.Page[self.config.updateFnName](response.data.config);
					}
				};
				GCMRC.Page[self.config.workerFedName] = true;

				GCMRC.Page.drawBudget(config);
			};
		};

		if (GCMRC.Page.reachDetail.some(function(el){return el.reachGroup === "S Sand Cumul Load"})) {
			result.push(new Budget({
				budgetType : "sandbudget",
				budgetColumns : budgetColumns["S Sand Cumul Load"],
				responseColumns : responseColumns["S Sand Cumul Load"],
				yAxisLabel : "Change in Sand Stored in Reach (Metric Tons)",
				seriesName : "Sand Storage Change",
				reqIdName : "latestSandReqId",
				updateFnName : "updateSandSummary",
				workerFedName : "isSandWorkerFed",
				workerName : "sandworker"
			}));
		}

		if (GCMRC.Page.reachDetail.some(function(el){return el.reachGroup === "S Fines Cumul Load"})) {
			result.push(new Budget({
				budgetType : "finesbudget",
				budgetColumns : budgetColumns["S Fines Cumul Load"],
				responseColumns : responseColumns["S Fines Cumul Load"],
				yAxisLabel : "Change in Silt and Clay Stored in Reach (Metric Tons)",
				seriesName : "Silt and Clay Storage Change",
				reqIdName : "latestFinesReqId",
				updateFnName : "updateFinesSummary",
				workerFedName : "isFinesWorkerFed",
				workerName : "finesworker"
			}));
		}

		return result;
	},
	createDateList: function(container, dates) {
		var result = [];

		if (dates && dates.endStaticRec && dates.newestSuspSed) {
			var endStatic = dates.endStaticRec;
			var newest = dates.newestSuspSed;
			var endStaticSplit = dates.endStaticRec.split('T');
			var newestSplit = dates.newestSuspSed.split('T');
			if (1 < endStaticSplit.length && 1 < newestSplit.length) {
				endStatic = endStaticSplit[0];
				newest = newestSplit[0];
			}

			var majorTribName = "Major Tributary";
			if (GCMRC.Page.reach.majorTribRiver) {
				majorTribName = GCMRC.Page.reach.majorTribRiver;
			}

			[].push.apply(result, [
				'<div class="innerRightSidebar">',
				'<div class="sectionTitle">Data Status</div>',
				'<div><ul>',
				'<li>',
				"End of complete lab-processed suspended-sediment record for " + majorTribName + ": ",
				'<a href="#" class="todate">',
				endStatic,
				'</a>',
				'</li>',
				'<li>',
				"Most recent suspended-sediment sample from " + majorTribName + " used in calculations: ",
				'<a href="#" class="todate">',
				newest,
				'</a>',
				'</li>',
				'</ul></div>',
				'</div>']);
		}

		container.append(result.join(""));
	},
	createParameterList: function(container, params) {
		var html = [];

		var addParamToList = function(param) {
			var listing = ['<div class="parameterListing">',
				'<h5>',
				param["displayName"],
				'</h5>',
				'<ul class="unstyled"><li>',
				'<div class="pull-left">',
				param["adjustMin"],
				'%</div><div class="pull-right">',
				param["adjustMax"],
				'%</div>',
				'<div name="',
				param["name"],
				'"></div>',
				'</li>',
				'<li><div style="text-align:center;"><span name="',
				param["name"],
				'_val"></span>%</div></li>',
				'<li>',
				'<div class="',
				param["name"],
				'_qual"></div>',
				'</li>',
				'</ul>',
				'</div>'];
			html.push(listing.join(''));
		};

		params.forEach(addParamToList);

		container.html(html.join('\n'));

		var slidechange = function(paramName) {
			return function(event, ui) {
				$('span[name=' + paramName + '_val]').html(ui.value);
				var a = parseFloat($('span[name=a_val]').html()) / 100.0;
				var b = parseFloat($('span[name=b_val]').html()) / 100.0;
				var c = parseFloat($('span[name=c_val]').html()) / 100.0;
				var d = parseFloat($('span[name=d_val]').html()) / 100.0;
				var e = parseFloat($('span[name=e_val]').html()) / 100.0;
				var f = parseFloat($('span[name=f_val]').html()) / 100.0;
				var g = parseFloat($('span[name=g_val]').html()) / 100.0;

				GCMRC.Page.drawBudget({
					divId: 'data-dygraph',
					labelDivId: 'legend-dygraph',
					a: a,
					b: b,
					c: c,
					d: d,
					e: e,
					f: f,
					g: g
				});
			};
		};
		var slidequal = function(changeThis, multiplier) {
			return function(event, ui) {
				$(changeThis).html(ui.value * multiplier);
			};
		};
		params.forEach(function(param) {
			var slider = $('div[name=' + param["name"] + ']').slider({
				min: param['adjustMin'],
				max: param['adjustMax'],
				value: param["adjustDefault"],
				slide: slidechange(param["name"]),
				change: slidechange(param["name"])
			});

			slider.on("slidechange", slidequal('span.' + param["name"] + '_qual2', 2));
			slider.on("slide", slidequal('span.' + param["name"] + '_qual2', 2));
			slider.on("slidechange", slidequal('span.' + param["name"] + '_qual4', 4));
			slider.on("slide", slidequal('span.' + param["name"] + '_qual4', 4));

			var defaultLocation = $('div[name=' + param["name"] + '] a').attr("style");
			$('div[name=' + param["name"] + ']').append('<a class="ui-corner-all ui-slider-default-position" style="' + defaultLocation + '" href="#"></a>');
			$('span[name=' + param["name"] + '_val]').html(param["adjustDefault"]);
		});

		$('.fromdate').click(GCMRC.Page.fromDateClicked);
		$('.todate').click(GCMRC.Page.toDateClicked);
	},
	earliestPosition: null,
	earliestPositionISO: null,
	latestPosition: null,
	latestPositionISO: null,
	reachPORLoad : JSL.ResourceLoad(function(el) {
		var streams = ["upstream", "upstreamSecondary", "downstream", "majorTrib", "minorTrib"];

		streams.each(function(stream) {
			if (el[stream + "BeginPosition"]) {
				var beginPosition = new Date(el[stream + "BeginPosition"]).getTime();
				if (!GCMRC.Page.earliestPosition || beginPosition > GCMRC.Page.earliestPosition) { //LATEST EARLIEST POSITION!
					GCMRC.Page.earliestPosition = beginPosition;
					GCMRC.Page.earliestPositionISO = el[stream + "BeginPosition"];
				}
			}
			if (el[stream + "EndPosition"]) {
				var endPosition = new Date(el[stream + "EndPosition"]).getTime();
				if (!GCMRC.Page.latestPosition || endPosition < GCMRC.Page.latestPosition) { ///EARLIEST LATEST POSITION!
					GCMRC.Page.latestPosition = endPosition;
					GCMRC.Page.latestPositionISO = el[stream + "EndPosition"];
				}
			}
		});
	}, function() {
		GCMRC.Page.params["finesbudget"] = {
			inst: {
				decimalPlaces: "0",
				displayName: "Silt and Clay Storage Change",
				displayOrder: "10",
				endPosition: GCMRC.Page.latestPositionISO.split("T")[0],
				groupId: "finesbudget",
				sampleMethod: "inst",
				beginPosition: GCMRC.Page.earliestPositionISO.split("T")[0],
				units: "Metric Tons",
				unitsShort: "Metric Tons",
				color: CONFIG.instColor,
				highlightColor: CONFIG.instHiColor
			},
			description : {
				groupId: "finesbudget",
				displayName: "Silt and Clay Storage Change",
				displayOrder: "10",
				units: "Metric Tons",
				unitsShort: "Metric Tons"
			}
		};
		GCMRC.Page.params["sandbudget"] = {
			inst: {
				decimalPlaces: "0",
				displayName: "Sand Storage Change",
				displayOrder: "20",
				endPosition: GCMRC.Page.latestPositionISO.split("T")[0],
				groupId: "sandbudget",
				sampleMethod: "inst",
				beginPosition: GCMRC.Page.earliestPositionISO.split("T")[0],
				units: "Metric Tons",
				unitsShort: "Metric Tons",
				color: CONFIG.instColor,
				highlightColor: CONFIG.instHiColor
			},
			description : {
				groupId: "sandbudget",
				displayName: "Sand Storage Change",
				displayOrder: "20",
				units: "Metric Tons",
				unitsShort: "Metric Tons"
			}
		};
		GCMRC.Page.params["2"] = {
			inst: {
				decimalPlaces: "0",
				displayName: "Discharge",
				displayOrder: "200",
				endPosition: GCMRC.Page.latestPositionISO.split("T")[0],
				groupId: "2",
				groupName: "Discharge",
				sampleMethod: "inst",
				beginPosition: GCMRC.Page.earliestPositionISO.split("T")[0],
				units: "Cubic feet per second",
				unitsShort: "cfs",
				color: CONFIG.instColor,
				highlightColor: CONFIG.instHiColor
			},
			description : {
				groupId: "2",
				groupName: "Discharge",
				displayName: "Discharge",
				displayOrder: "200",
				units: "Cubic feet per second",
				unitsShort: "cfs"
			}
		}
	}),
	params: {},
	credits: ["USGSGCMR"],
	creditLoad: JSL.ResourceLoad(function(el) {
		if (GCMRC.Page.credits.none(el.orgCode)) {
			GCMRC.Page.credits.push(el.orgCode);
		} else {
			// LOG.info("Credits already contain " + el.orgCode);
		}
	}),
	reach: {},
	reachLoad: JSL.ResourceLoad(function(el) {
		GCMRC.Page.reach = el;
	}),
	reachDetail : [],
	reachDetailLoad : JSL.ResourceLoad(null, function(data) {
		GCMRC.Page.reachDetail = data;
	}),
	sliderConfig: GCMRC.Reaches.sliderConfig,
	resetSliders: function() {
		GCMRC.Page.sliderConfig.values(function(el) {
			$('div[name=' + el.name + ']').slider("option", "value", el.adjustDefault);
		});
	},
	dealWithBedLoadResponse: function(bedLoad, bedLoadData, config) {
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
		conf.bedLoadData = bedLoadData.success.data.map(function(el) {
			var result;

			result = [parseInt(el[timeColumn])];
			
			var columns = bedLoad.responseColumns || bedLoad.columns;
			var dataColumns = columns.filter(function(n){return !n.startsWith(timeColumn)}).map(function(col) {
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
            },
	    getBedLoadData: function(param, config, urlParams) {
			$('#infoMsg').empty();
			$("#errorMsg").empty();
			var urls = {
			    agg: 'services/agg/'
			};
			urlParams["output"] = "json";
			$.ajax({
				type: 'GET',
				dataType: 'json',
				url: CONFIG.relativePath + urls[param],
				data: urlParams,
				timeout: 1200000, /* 20 minutes allowed, from start to data complete */
				success: function(bedLoadData, textStatus, jqXHR) {
					if (!bedLoadData || (!bedLoadData.contentType && bedLoadData.contentType === "text/xml")) {
						GCMRC.Graphing.clearErrorMsg();
						GCMRC.Graphing.showErrorMsg("An error has occurred.  Please contact <a href='mailto:" + GCMRC.administrator + "@usgs.gov'>" + GCMRC.administrator + "@usgs.gov</a>");
					} else {
						if (bedLoadData.data && !bedLoadData.data.ERROR && bedLoadData.data.time) {
							data = {
								success : {
									"@rowCount" : "-1",
									data : [
										bedLoadData.data
									]
								}
							};
						}
						//success
						if (bedLoadData.success && bedLoadData.success.data && $.isArray(bedLoadData.success.data)) {
							config.bedLoad.forEach(function(bedLoad) {
								if (bedLoad.dealWithBedLoadResponse) {
								    dealWithBedLoadResponse(bedLoad, bedLoadData, config);
								}
							});
						} else if (bedLoadData.data && bedLoadData.data.ERROR) {
							GCMRC.Graphing.clearErrorMsg();
							GCMRC.Graphing.showErrorMsg("Please select a parameter to graph!");
						} else {
							LOG.error("what the heck just happened?");
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