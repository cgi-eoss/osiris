/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityManageDatabasketCtrl
 * @description
 * # CommunityManageDatabasketCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules'], function (osirismodules) {

    osirismodules.controller('CommunityManageIncidentTypeCtrl', ['IncidentTypeService', 'FileService', 'CommunityService', '$scope', '$mdDialog', function (IncidentTypeService, FileService, CommunityService, $scope, $mdDialog) {

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


        $scope.refreshIncidentType = function() {
            IncidentTypeService.refreshSelectedIncidentType('community');
        };

    }]);
});
