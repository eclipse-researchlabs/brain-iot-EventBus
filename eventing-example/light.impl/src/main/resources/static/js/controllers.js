'use strict';

/* Controllers */

function LightUICtl($scope, $http, $timeout) {

	$scope.start = function() {
		if ($scope.eventSource) {
			$scope.eventSource.close();
		}

		$scope.eventSource = new EventSource("../light/stream");

		$scope.eventSource.onmessage = function(event) {
			var data = {};
			if(event.name === "error") {
				data.error = event.data;
				data.imageUrl = "img/lightBroken.png"
				data.status = "N/A";
				data.brightness = "N/A";
			} else {
				data = JSON.parse(event.data);
				data.imageUrl = data.status ? "img/light" + data.brightness + ".png" : "img/light0.png";
				data.status = data.status ? "on" : "off";
			}
			$scope.bulb = data;
			
			$scope.$apply();
		};
		
		$scope.eventSource.onerror = function(event) {
			var e = {};
			e.name = "error";
			e.data = "Connection failed: " + event.data;
			$scope.eventSource.onmessage(e);
			$scope.start();
		};
	};

	$scope.start();
}
