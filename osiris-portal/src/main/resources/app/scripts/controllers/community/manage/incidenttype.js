/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityManageDatabasketCtrl
 * @description
 * # CommunityManageDatabasketCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules'], function (osirismodules) {

    osirismodules.controller('CommunityManageIncidentTypeCtrl', ['IncidentTypeService', 'ProcessingTemplateService', 'CommunityService', '$scope', '$mdDialog', function (IncidentTypeService, ProcessingTemplateService, CommunityService, $scope, $mdDialog) {

        /* Get stored Databaskets & Files details */
        $scope.incidentTypeParams = IncidentTypeService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "File";


        $scope.itemSearch = {
            searchText: $scope.incidentTypeParams.itemSearchText
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
                $scope.incidentTypeParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };


       $scope.addProcessingTemplateDialog = function($event) {

            var incidentType = $scope.incidentTypeParams.selectedIncidentType;
            function AddProcessingTemplateController($scope, $mdDialog) {

                $scope.incidentType = incidentType;

                $scope.closeDialog = function(created) {
                    if (created) {
                        IncidentTypeService.refreshSelectedIncidentType('community');
                    }
                    $mdDialog.hide();
                };
            }
            AddProcessingTemplateController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: AddProcessingTemplateController,
                templateUrl: 'views/community/manage/incidentprocessing.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: false
           });
        };

        $scope.editProcessingTemplateDialog = function($event, processingTemplate) {
            var incidentType = $scope.incidentTypeParams.selectedIncidentType;
            function EditrocessingTemplateController($scope, $mdDialog) {

                $scope.incidentType = incidentType;
                $scope.processingTemplateToEdit = processingTemplate;

                $scope.closeDialog = function(updated) {
                    if (updated) {
                        IncidentTypeService.refreshSelectedIncidentType('community');
                    }
                    $mdDialog.hide();
                };
            }
            EditrocessingTemplateController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: EditrocessingTemplateController,
                templateUrl: 'views/community/manage/incidentprocessing.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: false
           });
        }

        $scope.removeProcessingTemplate = function(processingTemplate) {
            ProcessingTemplateService.removeProcessingTemplate(processingTemplate).then((function() {
                IncidentTypeService.refreshSelectedIncidentType('community');
            }))
        }

        $scope.refreshIncidentType = function() {
            IncidentTypeService.refreshSelectedIncidentType('community');
        };

    }]);
});
