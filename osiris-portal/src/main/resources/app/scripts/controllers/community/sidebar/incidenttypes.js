/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules'], function (osirismodules) {

    osirismodules.controller('CommunityIncidentTypesCtrl', ['IncidentTypeService', 'CommonService', 'PublishingService', '$scope', '$mdDialog', function (IncidentTypeService, CommonService, PublishingService, $scope, $mdDialog) {

        $scope.incidentTypeParams = IncidentTypeService.params.community;
        $scope.incidentTypeOwnershipFilters = IncidentTypeService.dbOwnershipFilters;
        $scope.item = "IncidentType";

        $scope.iconsLibrary = IncidentTypeService.getIconsLibrary().then(function(library) {
            $scope.iconsLibrary = library;
        });

        $scope.getIconPath = function(iconId) {
            try {
                return $scope.iconsLibrary[iconId].path;
            } catch(e) {
                return null;
            }
        };

        IncidentTypeService.refreshIncidentTypes("community");

        $scope.$on('poll.incidentTypes', function (event, data) {
            $scope.incidentTypeParams.incidentTypes = data;
        });

        /* Stop polling */
        $scope.$on("$destroy", function() {
            IncidentTypeService.stopPolling();
        });

        $scope.getPage = function(url){
            IncidentTypeService.getIncidentTypesPage('community', url);
        };

        $scope.filter = function(){
            IncidentTypeService.getIncidentTypesByFilter('community');
        };

        $scope.selectIncidentType = function (item) {
            $scope.incidentTypeParams.selectedIncidentType = item;
            IncidentTypeService.refreshSelectedIncidentType("community");
        };


        var scope = $scope;

        var showEditDialog = function($event, item) {

            function CreateIncidentTypeController($scope, $mdDialog) {

                $scope.newItem = item || {};
                $scope.iconsLibrary = scope.iconsLibrary;

                $scope.addOrUpdateIncident = function() {
                    if (!$scope.newItem.id) {
                        IncidentTypeService.createIncidentType($scope.newItem).then(function (newIncidentType) {
                            IncidentTypeService.refreshIncidentTypes('community', 'Create', newIncidentType);
                        });
                    } else {
                        IncidentTypeService.updateIncidentType($scope.newItem).then(function (updatedIncidentType) {
                            IncidentTypeService.refreshIncidentTypes('community');
                        });
                    }
                    $mdDialog.hide();
                };

                $scope.deleteIncident = function($event, incidentType) {
                    var confirmDialog = $mdDialog.confirm()
                        .title('Confirmation needed')
                        .targetEvent($event)
                        .htmlContent('Are you sure you want to delete incident type ' + incidentType.title + '?')
                        .ok('Confirm')
                        .cancel('Cancel');


                    $mdDialog.show(confirmDialog).then(function () {
                        IncidentTypeService.removeIncidentType(incidentType).then(function() {
                            IncidentTypeService.refreshIncidentTypes('community', 'Remove', incidentType);
                        });
                        $mdDialog.hide();
                    })
                }

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            CreateIncidentTypeController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: CreateIncidentTypeController,
                templateUrl: 'views/common/templates/createincidenttype.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });

        }

        $scope.createIncidentType = function($event) {
            showEditDialog($event);
        };


        $scope.editIncidentType = function ($event, item) {
            showEditDialog($event, item);
        };


    }]);
});
