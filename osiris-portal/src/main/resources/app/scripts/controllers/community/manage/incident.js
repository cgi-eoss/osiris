/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityManageDatabasketCtrl
 * @description
 * # CommunityManageDatabasketCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules', 'ol', 'moment'], function (osirismodules, ol, moment) {

    osirismodules.controller('CommunityManageIncidentCtrl', ['IncidentService', 'IncidentTypeService', 'AoiService', 'MapService', 'CommunityService', '$scope', '$mdDialog', function (IncidentService, IncidentTypeService, AoiService, MapService, CommunityService, $scope, $mdDialog) {

        /* Get stored Databaskets & Files details */
        $scope.incidentParams = IncidentService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "File";

        var request = IncidentTypeService.getIncidentTypes();

        $scope.incidentTypes = request;
        request.then(function(response) {
            $scope.incidentTypes = response.data;
        });

        $scope.$watch(function() {return $scope.incidentParams.selectedIncident;}, function() {

            var incident = $scope.incidentParams.selectedIncident;

            var incidentData = {
                id: incident.id,
                title: incident.title,
                description: incident.description,
                startDate: moment.utc(incident.startDate).toDate(),
                endDate: moment.utc(incident.endDate).toDate(),
                aoi: incident.aoi
            }

            if (incident.type) {
                incidentData.type = incident.type.id;
            }

            let aoi = incidentData.aoi;
            if (aoi) {
                try {
                    var polygon = new ol.format.WKT().readGeometry(aoi);
                    AoiService.setSearchAoi({
                        geometry: JSON.parse(new ol.format.GeoJSON().writeGeometry(polygon))
                    });

                    polygon.transform('EPSG:4326', 'EPSG:3857');
                    var extent = polygon.getExtent();
                    if (extent) {
                        MapService.fitExtent(extent);
                    }
                } catch(e) {
                    MapService.getMap().setView(new ol.View({
                        center: ol.proj.fromLonLat([0, 51.28]),
                        zoomLevel: 10
                    }));
                }
            } else {
                AoiService.setSearchAoi(null);
                MapService.fitExtent([-647452,1599348.255904,3071953.613876,10293520])
            }

            $scope.incidentData = incidentData;
        });


        $scope.itemSearch = {
            searchText: $scope.incidentParams.itemSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.filename && item.filename.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.shareQuickSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.incidentParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        var getIncidentParams = function(incident) {

            var params = {
                title: incident.title,
                aoi: incident.aoi,
                startDate: moment.utc(incident.startDate).format('YYYY-MM-DD[T00:00:00Z]'),
                endDate: moment.utc(incident.endDate).format('YYYY-MM-DD[T23:59:59Z]'),
                type: $scope.incidentTypes.find(function(type) {
                    return type.id === incident.type;
                })._links.self.href,
                description: incident.description
            }

            if (incident.id) {
                params.id = incident.id;
            }

            return params;
        }

        $scope.addOrUpdateIncident = function() {
            var incident = $scope.incidentData;

            var data = getIncidentParams(incident);

            if (!data.id) {
                IncidentService.createIncident(data).then(function(newIncident) {
                    IncidentService.refreshIncidents('community', 'Create', newIncident);
                });
            } else {
                IncidentService.updateIncident(data).then(function(updatedIncident) {
                    IncidentService.refreshIncidents('community');
                });;
            }
        }

    }]);
});
