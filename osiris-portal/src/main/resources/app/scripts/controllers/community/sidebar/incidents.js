/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules'], function (osirismodules) {

    osirismodules.controller('CommunityIncidentsCtrl', ['IncidentService', 'IncidentTypeService', 'CommonService', 'PublishingService', '$scope', '$mdDialog', function (IncidentService, IncidentTypeService, CommonService, PublishingService, $scope, $mdDialog) {

        $scope.incidentParams = IncidentService.params.community;
        $scope.incidentOwnershipFilters = IncidentService.dbOwnershipFilters;
        $scope.item = "Incident";


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

        IncidentService.refreshIncidents("community");

        $scope.$on('poll.incidents', function (event, data) {
            $scope.incidentParams.incidents = data;
        });

        /* Stop polling */
        $scope.$on("$destroy", function() {
            IncidentService.stopPolling();
        });

        $scope.getPage = function(url){
            IncidentService.getIncidentsPage('community', url);
        };

        $scope.filter = function(){
            IncidentService.getIncidentsByFilter('community');
        };

        $scope.selectIncident = function (item) {
            $scope.incidentParams.selectedIncident = item;
            IncidentService.refreshSelectedIncident("community");
        };

        $scope.createIncident = function($event) {
            $scope.incidentParams.selectedIncident = {};
        };

        $scope.deleteIncident = function(event, incident) {
            CommonService.confirm(event, 'Are you sure you want to delete this incident: "' + incident.title + '"?').then(function (confirmed){
                if(confirmed === false){
                    return;
                }
                IncidentService.removeIncident(incident).then(function(){
                    IncidentService.refreshIncidents('community', 'Remove', incident);
                });
            });
        }

    }]);
});
