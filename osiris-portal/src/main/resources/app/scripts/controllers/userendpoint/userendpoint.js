/**
 * @ngdoc function
 * @name osirisApp.controller:UserEndpointCtrl
 * @description
 * # HelpdeskCtrl
 * Controller of the osirisApp
 */
'use strict';
define(['../../osirismodules'], function (osirismodules) {

    osirismodules.controller('UserEndpointCtrl', ['osirisProperties', '$scope', '$routeParams', '$sce', 'UserEndpointService', function (osirisProperties, $scope, $routeParams, $sce, UserEndpointService) {

        $scope.endpoint = null;

        UserEndpointService.getUserEndpoint($routeParams.app).then(function(endpoint) {
            endpoint.url = $sce.trustAsResourceUrl(endpoint.url);
            $scope.endpoint = endpoint;
        });
    }]);
});

