'use strict';

/* Controllers */

function SensorUICtl($scope, $http, $timeout) {

	$scope.trigger = function() {
		
		$http.post("../sensor", null);

	};
}
