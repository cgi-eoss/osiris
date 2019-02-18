/**
 * @ngdoc function
 * @name osirisApp.controller:IncidentProcessingCtrl
 * @description
 * # IncidentProcessingCtrl
 * Controller of the osirisApp
 */
'use strict';

define(['../../../osirismodules'], function (osirismodules) {

    osirismodules.controller('IncidentProcessingCtrl', [ '$scope', 'ProductService', 'SearchService', 'ProcessingTemplateService', function ($scope, ProductService, SearchService, ProcessingTemplateService) {

        $scope.processingTemplate = {};
        $scope.serviceSearchExpression = '';
        $scope.isServiceDescriptionLoading = false;
        $scope.isSearchParametersLoading = true;

        $scope.searchForm = {
            config: {},
            api: {},
            data: {}
        }

        if ($scope.processingTemplateToEdit) {
            ProcessingTemplateService.getProcessingTemplate($scope.processingTemplateToEdit).then(function(processingTemplate) {
                $scope.processingTemplateToEdit = processingTemplate;

                $scope.processingTemplate.title = $scope.processingTemplateToEdit.title;
                $scope.processingTemplate.description = $scope.processingTemplateToEdit.description;

                var formData = {};

                for (var key in processingTemplate.searchParameters) {
                    formData[key] = processingTemplate.searchParameters[key][0];
                }

                if ($scope.processingInstance) {
                    for (var key in $scope.processingInstance.searchParameters) {
                        formData[key] = $scope.processingInstance.searchParameters[key][0];
                    }
                }

                $scope.searchForm.data = formData;

                $scope.processingTemplate.systematicInput = processingTemplate.systematicInput;
                $scope.processingTemplate.service = processingTemplate.service;

            });

        }

        SearchService.getSearchParameters().then(function(data){

            delete data.productDate;
            delete data.aoi;

            data.catalogue.type = 'select';
            data.catalogue.defaultValue = data.catalogue.allowed.values[0].value;

            for (var key in data) {
                if (data[key].type === 'daterange') {
                    if ($scope.searchForm.data[key + 'Start'] || $scope.searchForm.data[key + 'End']) {
                        $scope.searchForm.data[key] = {
                            start: $scope.searchForm.data[key + 'Start'] ? new Date($scope.searchForm.data[key + 'Start']) : null,
                            end:  $scope.searchForm.data[key + 'End'] ? new Date($scope.searchForm.data[key + 'End']) : null
                        }
                        delete $scope.searchForm.data[key + 'Start'];
                        delete $scope.searchForm.data[key + 'End'];
                    }
                } else {
                    if ($scope.searchForm.data[key]) {
                        if (data[key].type === 'int') {
                            $scope.searchForm.data[key] = parseInt($scope.searchForm.data[key]);
                        }
                    }
                }
            }

            $scope.searchForm.config = data;

            $scope.isSearchParametersLoading = false;

        });

        $scope.searchServices = function(expression) {
            return ProcessingTemplateService.getProcessingServices(expression);
        }

        $scope.onSelectedServiceChange = function(service) {

            $scope.processingTemplate.serviceDetails = null;
            $scope.processingTemplate.serviceParams = {};
            $scope.processingTemplate.systematicInput = null;

            if (service) {
                $scope.isServiceDescriptionLoading = true;
                ProductService.getService(service).then(function(detailedService){

                    $scope.processingTemplate.serviceDetails = detailedService;
                    detailedService.serviceDescriptor.dataInputs = detailedService.serviceDescriptor.dataInputs.filter(function(input) {
                        return input.id !== 'aoi';
                    });

                    $scope.isServiceDescriptionLoading = false;

                    var serviceParams = {}
                    if ($scope.processingTemplateToEdit && service === $scope.processingTemplateToEdit.service) {
                        for (var key in $scope.processingTemplateToEdit.fixedInputs) {
                            serviceParams[key] = $scope.processingTemplateToEdit.fixedInputs[key][0];
                        }
                        $scope.processingTemplate.systematicInput = $scope.processingTemplateToEdit.systematicInput;
                    }
                    if ($scope.processingInstance) {
                        for (var key in $scope.processingInstance.inputs) {
                            serviceParams[key] = $scope.processingInstance.inputs[key][0];
                        }
                    }

                    $scope.processingTemplate.serviceParams = serviceParams;
                });
            }
        }

        $scope.getDefaultValue = function(fieldDesc){
            return $scope.processingTemplate.serviceParams[fieldDesc.id] ? $scope.processingTemplate.serviceParams[fieldDesc.id] : fieldDesc.defaultAttrs.value;
        };

        $scope.updateIncidentProcessing = function($event) {

            if (!$scope.processingInstance) {
                delete  $scope.processingTemplate.serviceParams[$scope.processingTemplate.systematicInput];

                var searchParams = $scope.searchForm.api.getFormData();
                for (var key in searchParams) {
                    searchParams[key] = [searchParams[key]];
                }

                var serviceParams = {}
                for (var key in $scope.processingTemplate.serviceParams) {
                    serviceParams[key] = [$scope.processingTemplate.serviceParams[key]]
                }

                var serviceData  = {
                    title: $scope.processingTemplate.title,
                    description: $scope.processingTemplate.description,
                    systematicInput: $scope.processingTemplate.systematicInput,
                    fixedInputs: serviceParams,
                    searchParameters: searchParams,
                    service: $scope.processingTemplate.service._links.self.href,
                    incidentType: $scope.incidentType._links.self.href
                };

                if ($scope.processingTemplateToEdit) {
                    ProcessingTemplateService.updateProcessingTemplate($scope.processingTemplateToEdit.id, serviceData).then(function() {
                        $scope.closeDialog(true);
                    })
                } else {
                    ProcessingTemplateService.createProcessingTemplate(serviceData).then(function() {
                        $scope.closeDialog(true);
                    });
                }
            } else {

                $scope.processingInstance.searchParameters = {};

                var formSearchParams = $scope.searchForm.api.getFormData();
                var originalSearchParams = $scope.processingTemplateToEdit.searchParameters;
                for (var key in formSearchParams) {
                    if (!originalSearchParams[key] || formSearchParams[key] !== originalSearchParams[key][0]) {
                        $scope.processingInstance.searchParameters[key] = [formSearchParams[key]];
                    }
                }

                $scope.processingInstance.inputs = {}
                var formServiceParams = $scope.processingTemplate.serviceParams;
                var originalServiceParams = $scope.processingTemplateToEdit.fixedInputs;
                for (var key in formServiceParams) {
                    if (!originalServiceParams[key] || formServiceParams[key] !== originalServiceParams[key][0]) {
                        $scope.processingInstance.inputs[key] = [formServiceParams[key]];
                    }
                }

                $scope.closeDialog(true);
            }

        };

    }]);
});
