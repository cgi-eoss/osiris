/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityManageDatabasketCtrl
 * @description
 * # CommunityManageDatabasketCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules', 'ol', 'moment'], function (osirismodules, ol, moment) {

    osirismodules.controller('CommunityManageIncidentCtrl', ['$q', 'IncidentService', 'IncidentTypeService', 'IncidentProcessingService', 'ProductService', 'CollectionService', 'SystematicService', 'TabService', 'AoiService', 'MapService', 'CommunityService', '$scope', '$mdDialog', function ($q, IncidentService, IncidentTypeService, IncidentProcessingService, ProductService, CollectionService, SystematicService, TabService, AoiService, MapService, CommunityService, $scope, $mdDialog) {

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


            if (incident.type && incident.incidentProcessings) {
                $scope.onIncidentTypeChange(incident.type.id, incident.incidentProcessings);
            }

        });

        $scope.onIncidentTypeChange = function(type, incidentProcessings) {

            if (incidentProcessings) {

                var processingTemplates = [];

                incidentProcessings.forEach(function(processing) {
                    var processingTemplate = processing.template;
                    processingTemplate.instance = {
                        active: true,
                        meta: processing
                    }
                    processingTemplates.push(processingTemplate);
                });

                $scope.incidentData.processingTemplates = processingTemplates;

            } else {
                IncidentTypeService.getProcessingTemplatesForType(type).then(function(processingTemplates) {

                    processingTemplates.forEach(function(template) {
                        template.instance = {
                            active: true,
                            searchParameters: {},
                            inputs: {}
                        }
                    })

                    $scope.incidentData.processingTemplates = processingTemplates;
                });
            }
        }

        $scope.editProcessingTemplateDialog = function($event, processingTemplate) {

            var readOnly = !!$scope.incidentData.id;

            function EditProcessingTemplateController($scope, $mdDialog) {

                $scope.processingTemplateToEdit = processingTemplate;
                $scope.processingInstance = processingTemplate.instance;

                $scope.readOnly = readOnly || false;

                $scope.closeDialog = function() {
                    $mdDialog.hide();
                };
            }
            EditProcessingTemplateController.$inject = ['$scope', '$mdDialog'];

            if (processingTemplate.instance.meta && !processingTemplate.instance.searchParameters) {
                IncidentProcessingService.getIncidentProcessing({id: processingTemplate.instance.meta.id}).then(function(data) {
                    processingTemplate.instance.searchParameters = data.searchParameters;
                    processingTemplate.instance.inputs = data.inputs;
                    delete processingTemplate.instance.id;

                    $mdDialog.show({
                        controller: EditProcessingTemplateController,
                        templateUrl: 'views/community/manage/incidentprocessing.html',
                        parent: angular.element(document.body),
                        targetEvent: $event,
                        clickOutsideToClose: false
                    });

                })
            } else {
                $mdDialog.show({
                    controller: EditProcessingTemplateController,
                    templateUrl: 'views/community/manage/incidentprocessing.html',
                    parent: angular.element(document.body),
                    targetEvent: $event,
                    clickOutsideToClose: false
                });
            }
        }

        $scope.goToProcessingCollection = function(collection) {

            CollectionService.params.community.searchParams.ownership = CollectionService.dbOwnershipFilters.ALL_COLLECTIONS;
            CollectionService.params.community.searchParams.searchText = collection.name;
            CollectionService.params.community.searchParams.fileType = 'OUTPUT_PRODUCT';
            TabService.navInfo.community.activeSideNav = TabService.getCommunityNavTabs().COLLECTIONS;
            CollectionService.params.community.selectedCollection = collection;
            CollectionService.refreshCollections('community');

        };

        $scope.goToSystematicProcessing = function(systematicProcessing) {
            SystematicService.params.community.selectedOwnershipFilter = SystematicService.ownershipFilters.ALL_PROCESSINGS;
            TabService.navInfo.community.activeSideNav = TabService.getCommunityNavTabs().SYSTEMATICPROCS;
            SystematicService.params.community.selectedSystematicProcessing = systematicProcessing;
            SystematicService.refreshSelectedSystematicProcessing('community');
        }

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

        var getProcessingInstances = function(incident, processingTemplates) {
            return processingTemplates.filter(function(template) {
                return template.instance.active
            }).map(function(template) {
                var instanceConfig = {
                    inputs: template.instance.inputs,
                    searchParameters: template.instance.searchParameters,
                    template: template._links.self.href,
                    incident: incident
                }
                if (template.instance.cronExpression) {
                    instanceConfig.cronExpression = template.instance.cronExpression;
                }
                return instanceConfig;
            });
        }

        var populateImplicitInputs = function(incidentParams) {
            let requests = [];

            $scope.incidentData.processingTemplates.forEach(function(template) {
                if (template.instance.active && template.cronExpression) {
                    requests.push(ProductService.getService(template.service).then(function(detailedService) {
                        detailedService.serviceDescriptor.dataInputs.forEach(function(input) {
                            if (input.id === 'aoi') {
                                template.instance.inputs['aoi'] = [incidentParams.aoi];
                            }
                            if (input.id === 'startDate') {
                                template.instance.inputs['startDate'] = [incidentParams.startDate];
                            }
                            if (input.id === 'endDate') {
                                template.instance.inputs['endDate'] = [incidentParams.endDate];
                            }
                        });
                    }));
                }
            })

            return $q.all(requests);
        }

        $scope.addOrUpdateIncident = function() {


            var incident = $scope.incidentData;

            var data = getIncidentParams(incident);

            if (!data.id) {
                IncidentService.createIncident(data).then(function(newIncident) {
                    populateImplicitInputs(data).then(function() {
                        var processingInstances = getProcessingInstances(newIncident._links.self.href, $scope.incidentData.processingTemplates);
                        IncidentProcessingService.createIncidentProcessings(processingInstances).then(function(response) {
                            IncidentService.startIncidentProcessing(response[0]._embedded.incident).then(function() {
                                IncidentService.refreshIncidents('community', 'Create', newIncident)
                            })
                        });
                    });
                });
            } else {
                IncidentService.updateIncident(data).then(function(updatedIncident) {
                    IncidentService.refreshIncidents('community');
                });;
            }
        }

    }]);
});
