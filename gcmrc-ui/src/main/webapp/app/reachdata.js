GCMRC.Reaches = function () {

	var slider_defaults = {
		bedLoad: {
			name: "a",
			displayName: "Bedload Coefficient for River Sand Loads",
			adjustMin: 0,
			adjustMax: 10,
			adjustDefault: 5
		},
		riverFinesLoad: {
			name: "e",
			displayName: "Magnitude of Possible Persistent Bias in Measured River Silt and Clay Loads",
			adjustMin: 0,
			adjustMax: 25,
			adjustDefault: 5
		},
		riverLoad: {
			name: "b",
			displayName: "Magnitude of Possible Persistent Bias in Measured River Sand Loads",
			adjustMin: 0,
			adjustMax: 25,
			adjustDefault: 5
		},
		majorTribFinesLoad: {
			name: "f",
			displayName: "Magnitude of Possible Persistent Bias in Measured Major Tributary Silt and Clay Loads",
			adjustMin: 0,
			adjustMax: 25,
			adjustDefault: 10
		},
		majorTribLoad: {
			name: "c",
			displayName: "Magnitude of Possible Persistent Bias in Measured Major Tributary Sand Loads",
			adjustMin: 0,
			adjustMax: 25,
			adjustDefault: 10
		},
		minorTribFinesLoad: {
			name: "g",
			displayName: "Magnitude of Possible Persistent Bias in Lesser Tributary Silt and Clay Loads",
			adjustMin: 0,
			adjustMax: 50,
			adjustDefault: 50
		},
		minorTribLoad: {
			name: "d",
			displayName: "Magnitude of Possible Persistent Bias in Lesser Tributary Sand Loads",
			adjustMin: 0,
			adjustMax: 50,
			adjustDefault: 50
		}
	};

	var slider = slider_defaults;

	// Network specific overrides

	if ("BIBE" === CONFIG.networkName) {
		var RIVER_NAME = "Rio Grande";
		var MAJOR_TRIB_NAME = "Tornillo Creek";
		var LESSOR_TRIB_NAME = "Other Tributary";

		slider.bedLoad.displayName = slider.bedLoad.displayName.replace("River", RIVER_NAME);

		slider.riverFinesLoad.displayName = slider.riverFinesLoad.displayName.replace("River", RIVER_NAME);
		slider.riverFinesLoad.adjustDefault = 10;

		slider.riverLoad.displayName = slider.riverLoad.displayName.replace("River", RIVER_NAME);
		slider.riverLoad.adjustDefault = 10;

		slider.majorTribFinesLoad.displayName = slider.majorTribFinesLoad.displayName.replace("Major Tributary", MAJOR_TRIB_NAME);
		slider.majorTribFinesLoad.adjustDefault = 20;

		slider.majorTribLoad.displayName = slider.majorTribLoad.displayName.replace("Major Tributary", MAJOR_TRIB_NAME);
		slider.majorTribLoad.adjustDefault = 20;

		slider.minorTribFinesLoad.displayName = slider.minorTribFinesLoad.displayName.replace("Lesser Tributary", LESSOR_TRIB_NAME);
		slider.minorTribFinesLoad.adjustMax = 100;

		slider.minorTribLoad.displayName = slider.minorTribLoad.displayName.replace("Lesser Tributary", LESSOR_TRIB_NAME);
		slider.minorTribLoad.adjustMax = 100;
	}
	else if ("DINO" === CONFIG.networkName) {
		slider.bedLoad.adjustMax = 0;
		slider.bedLoad.adjustDefault = 0;

		slider.riverFinesLoad.adjustMax = 50;
		slider.riverFinesLoad.adjustDefault = 10;

		slider.riverLoad.adjustMax = 50;
		slider.riverLoad.adjustDefault = 10;
	}
	else if ("CRD" === CONFIG.networkName) {
		slider.bedLoad.adjustMax = 200;
		slider.bedLoad.adjustDefault = 50;

		slider.riverFinesLoad.adjustMax = 50;
		slider.riverFinesLoad.adjustDefault = 10;

		slider.riverLoad.adjustMax = 50;
		slider.riverLoad.adjustDefault = 10;

	}

	return {
		sliderConfig: slider
	};
}();

