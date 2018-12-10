/**
* @ngdoc function
* @name osirisApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the osirisApp
*/
define(['../osirismodules'], function (osirismodules) {
    'use strict';

    osirismodules.controller('IndexCtrl', ['osirisProperties', '$scope', '$location', '$window', 'UserService', function (osirisProperties, $scope, $location, $window, UserService) {

        $scope.osirisUrl = osirisProperties.OSIRIS_URL;
        $scope.sessionEnded = false;
        $scope.timeoutDismissed = false;

        $scope.$on('no.user', function() {
            $scope.sessionEnded = true;
        });

        $scope.hideTimeout = function() {
            $scope.sessionEnded = false;
            $scope.timeoutDismissed = true;
        };

        $scope.reloadRoute = function() {
            $window.location.reload();
        };

        $scope.goTo = function ( path ) {
            $location.path( path );
        };

        $scope.version = document.getElementById("version").content;

        // Trigger a user check to ensure controllers load correctly
        UserService.checkLoginStatus();
    }]);
});
