var closureLocation = "../../webjars/closure-library/${version.closure}/goog";
importScripts(
        closureLocation + "/base.js",
        closureLocation + "/deps.js",
        closureLocation + "/debug/error.js",
        closureLocation + "/string/string.js",
        closureLocation + "/asserts/asserts.js",
        closureLocation + "/array/array.js",
        closureLocation + "/math/math.js",
        closureLocation + "/math/size.js",
        closureLocation + "/math/matrix.js"
        );

importScripts("../../webjars/sugar/${version.sugarjs}/sugar-full.min.js");
importScripts("../../js/js-utils/binary-search.js");

importScripts("../../app/graphing/MatrixWorker.js");

var matrixStuff = new MatrixWorker(MatrixWorker.SAND);

self.addEventListener("message", function(msg) {
	var config = msg.data;
	
	if ("transformArray" === config.messageType) {
		var result = matrixStuff.transformArray(config);
		self.postMessage(result);
	} else if ("setDataArray" === config.messageType) {
		matrixStuff.setDataArray(config);
	} else if ("addBedloadToDataArray" === config.messageType) {
		matrixStuff.addBedloadToDataArray(config);
	} else {
		//damn
	}
});