goog.require("goog.math.Matrix");

var MatrixWorker = function(finesOrSand) {
	this.dataArrays = {};
	this.times = {};
	this.originalDataArrays = {};
	this.bedloadData = {};
	this.xformMatrices = {
		"sign": new goog.math.Matrix([
			[1, 1, 1],
			[1, 1, 1],
			[1, 1, 1],
			[-1, -1, -1]
		]),
		"bedload": new goog.math.Matrix([
			[1, 1, 1],
			[0, 0, 0],
			[0, 0, 0],
			[-1, -1, -1]
		]),
		"river": new goog.math.Matrix([
			[-1, 0, 1],
			[0, 0, 0],
			[0, 0, 0],
			[-1, -0, 1]
		]),
		"riverBedload": new goog.math.Matrix([
			[-1, 0, 1],
			[0, 0, 0],
			[0, 0, 0],
			[-1, 0, 1]
		]),
		"minor": new goog.math.Matrix([
			[0, 0, 0],
			[0, 0, 0],
			[-1, -0, 1],
			[0, 0, 0]
		]),
		"major": new goog.math.Matrix([
			[0, 0, 0],
			[-1, -0, 1],
			[0, 0, 0],
			[0, 0, 0]
		])
	};
	this.endStaticRec = null;
	this.newestSuspSed = null;
	
	this.isFines = finesOrSand;
};

MatrixWorker.FINES = true;
MatrixWorker.SAND = false;

MatrixWorker.getOffset = function(xform, doubleXform, offset) {
	var offsetMatrix = new goog.math.Matrix(offset).multiply(xform);
	var doubleOffsetMatrix = new goog.math.Matrix(offset).multiply(doubleXform);
	var minused = offsetMatrix.subtract(doubleOffsetMatrix);

	return minused.getValueAt(0, 0);
};

MatrixWorker.prototype.setDataArray = function(config) {
	this.dataArrays[config.divId] = config.data;
	this.originalDataArrays[config.divId] = config.data;
	this.times[config.divId] = config.time;
	this.endStaticRec = config.endStaticRec;
	this.newestSuspSed = config.newestSuspSed;
};

MatrixWorker.prototype.addBedloadToDataArray = function(config) {
	
	if(config.useBedload){
		this.bedloadData = config.data;
	}
	
	this.dataArrays[config.divId] = this.originalDataArrays[config.divId].clone();
};

MatrixWorker.addBedloads = function(dataArray, bedloadData, xformMatrices, riverBedloadPercentage) {
	//Create the matrix that computes the edges for cumulative river bedload
	var riverBedloadTransformMatrix = (xformMatrices["sign"].multiply(1)).
		add(xformMatrices["riverBedload"].multiply(riverBedloadPercentage));
		
	//Do the multiplication/error calculations.
	var bedloadMatrix = null;
	bedloadMatrix = new goog.math.Matrix(bedloadData).multiply(riverBedloadTransformMatrix);
	
	//Convert the bedload matrix to an array so that it can be added to the data array.
	var bedloadArray = bedloadMatrix.toArray();
	for(var i = 0; i < bedloadArray.length; i++){
		//Start at [i][1] because [i][0] is the times variable.
		//lower, middle, upper
		dataArray[i][1][0] += bedloadArray[i][0];
		dataArray[i][1][1] += bedloadArray[i][1];
		dataArray[i][1][2] += bedloadArray[i][2];
		//All upper
		dataArray[i][2][0] += bedloadArray[i][2];
		dataArray[i][2][1] += bedloadArray[i][2];
		dataArray[i][2][2] += bedloadArray[i][2];
		//All lower
		dataArray[i][3][0] += bedloadArray[i][0];
		dataArray[i][3][1] += bedloadArray[i][0];
		dataArray[i][3][2] += bedloadArray[i][0];
	}
	
	return dataArray;
};

MatrixWorker.prototype.transformArray = function(config) {
	var percentages = {};
	if (MatrixWorker.FINES === this.isFines) {
		percentages["bedload"] = 0;
		percentages["river"] = config.e || 0;
		percentages["major"] = config.f || 0;
		percentages["minor"] = config.g || 0;
	} else {
		percentages["bedload"] = config.a || 0;
		percentages["river"] = config.b || 0;
		percentages["major"] = config.c || 0;
		percentages["minor"] = config.d || 0;
		percentages["riverBedload"] = config.riverBedload || 0;
	}
	

	var transformMatrix = (this.xformMatrices["sign"].multiply(1)).
			add(this.xformMatrices["bedload"].multiply(percentages["bedload"])).
			add(this.xformMatrices["river"].multiply(percentages["river"] + percentages["river"] * percentages["bedload"])).
			add(this.xformMatrices["minor"].multiply(percentages["minor"])).
			add(this.xformMatrices["major"].multiply(percentages["major"]));
	
	var doubleTransformMatrix = (this.xformMatrices["sign"].multiply(1)).
			add(this.xformMatrices["bedload"].multiply(percentages["bedload"])).
			add(this.xformMatrices["river"].multiply(percentages["river"] + percentages["river"] * percentages["bedload"])).
			add(this.xformMatrices["minor"].multiply(percentages["minor"])).
			add(this.xformMatrices["major"].multiply(percentages["major"] * 2));
	
	var quadTransformMatrix = (this.xformMatrices["sign"].multiply(1)).
		add(this.xformMatrices["bedload"].multiply(percentages["bedload"])).
		add(this.xformMatrices["river"].multiply(percentages["river"] + percentages["river"] * percentages["bedload"])).
		add(this.xformMatrices["minor"].multiply(percentages["minor"])).
		add(this.xformMatrices["major"].multiply(percentages["major"] * 4));
	
	var singleTimes = this.times[config.divId];
	var singleArray = this.dataArrays[config.divId];
	var offsetArray = [];
	var doubleTimes = [];
	var doubleArray = [];
	var doubleOffsetArray = [];
	var quadTimes = [];
	var quadArray = [];
	if (this.endStaticRec) {
		var index = Math.abs(binaryIndexOf.call(this.times[config.divId], this.endStaticRec));
		var endIndex = this.times[config.divId].length;
		if (this.newestSuspSed) {
			endIndex = Math.abs(binaryIndexOf.call(this.times[config.divId], this.newestSuspSed));
		}
		
		singleTimes = this.times[config.divId].slice(0,index);
		singleArray = this.dataArrays[config.divId].slice(0,index);
		if (0 < index) {
			offsetArray = this.dataArrays[config.divId].slice(index - 1, index);
		}
		doubleTimes = this.times[config.divId].slice(index, endIndex);
		doubleArray = this.dataArrays[config.divId].slice(index, endIndex);
		if (0 < endIndex && index < endIndex) {
			doubleOffsetArray = this.dataArrays[config.divId].slice(endIndex - 1, endIndex);
		}
		quadTimes = this.times[config.divId].slice(endIndex, this.times[config.divId].length);
		quadArray = this.dataArrays[config.divId].slice(endIndex, this.times[config.divId].length);
	}

	var dataMatrix = null;
	if (0 < singleTimes.length) {
		dataMatrix = new goog.math.Matrix(singleArray).multiply(transformMatrix);
	}

	var doubleOffset = 0;
	var doubleDataMatrix = null;
	if (0 < doubleTimes.length) {
		doubleDataMatrix = new goog.math.Matrix(doubleArray).multiply(doubleTransformMatrix);
		if (0 < offsetArray.length) {
			doubleOffset = MatrixWorker.getOffset(transformMatrix, doubleTransformMatrix, offsetArray);
		}
	}
	
	var quadOffset = 0;
	var quadDataMatrix = null;
	if (0 < quadTimes.length) {
		quadDataMatrix = new goog.math.Matrix(quadArray).multiply(quadTransformMatrix);
		if (0 < doubleOffsetArray.length) {
			quadOffset = MatrixWorker.getOffset(doubleTransformMatrix, quadTransformMatrix, doubleOffsetArray);
		} else if (0 < offsetArray.length) {
			quadOffset = MatrixWorker.getOffset(transformMatrix, quadTransformMatrix, offsetArray);
		}
	}

	var dataArray = [];
	if (dataMatrix) {
		dataMatrix.toArray().forEach(function(el, i, arr) {
			dataArray.push([singleTimes[i], [el[0], el[1], el[2]], [el[2], el[2], el[2]], [el[0], el[0], el[0]]]);
		}, this);
	}
	if (doubleDataMatrix) {
		doubleDataMatrix.toArray().forEach(function(el, i, arr) {
			dataArray.push([doubleTimes[i], 
				[el[0]+doubleOffset, el[1], el[2]-doubleOffset], 
				[el[2]-doubleOffset, el[2]-doubleOffset, el[2]-doubleOffset], 
				[el[0]+doubleOffset, el[0]+doubleOffset, el[0]+doubleOffset]]);
		}, this);
	}
	if (quadDataMatrix) {
		quadDataMatrix.toArray().forEach(function(el, i, arr) {
			dataArray.push([quadTimes[i], 
				[el[0]+quadOffset, el[1], el[2]-quadOffset], 
				[el[2]-quadOffset, el[2]-quadOffset, el[2]-quadOffset], 
				[el[0]+quadOffset, el[0]+quadOffset, el[0]+quadOffset]]);
		}, this);
	}
	
	if(config.useBedload && !(this.bedloadData.isEmpty())){
		dataArray = MatrixWorker.addBedloads(dataArray, this.bedloadData, this.xformMatrices, percentages["riverBedload"]);
	}

	var lastIndex = dataArray[dataArray.length - 1][1];
	var summary = {
		upper: lastIndex[2],
		mid: lastIndex[1],
		lower: lastIndex[0]
	};

	return {
		reqId: config.reqId,
		dataArray: dataArray,
		config: summary
	};
};