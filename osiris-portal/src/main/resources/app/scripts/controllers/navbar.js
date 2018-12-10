/**
 * @ngdoc function
 * @name osirisApp.controller:NavbarCtrl
 * @description
 * # NavbarCtrl
 * Controller of the navbar
 */
define(['../osirismodules'], function (osirismodules) {
    'use strict';

    osirismodules.controller('NavbarCtrl', ['osirisProperties', '$scope', '$location', 'UserService', 'UserEndpointService', '$window', function (osirisProperties, $scope, $location, UserService, UserEndpointService, $window) {

        $scope.user = undefined;
        $scope.ssoUrl = osirisProperties.SSO_URL;
        $scope.osirisUrl = osirisProperties.OSIRIS_URL;
        $scope.analystpUrl = osirisProperties.ANALYST_URL;
        $scope.userEndpoints = [];

        $scope.$watch( function() {
            return UserEndpointService.endpoints;
         }, function( endpoints ) {
            $scope.userEndpoints = endpoints;
         });

        $scope.isActive = function (route) {
            return route === $location.path();
        };

        $scope.user = UserService.params.activeUser;

        $scope.$on('active.user', function(event, user) {
            $scope.user = UserService.params.activeUser;
            /*
            UserEndpointService.getUserEndpoints().then(function(endpoints) {
                $scope.userEndpoints = endpoints;
            });
            */
        });

        $scope.$on('no.user', function() {
            $scope.user = UserService.params.activeUser;
            $scope.userEndpoints = [];
        });

    }]);
});
