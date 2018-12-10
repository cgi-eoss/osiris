/**
 * @ngdoc function
 * @name osirisApp.controller:CommunityManageFileCtrl
 * @description
 * # CommunityManageFileCtrl
 * Controller of the osirisApp
 */

'use strict';

define(['../../../osirismodules'], function (osirismodules) {

    osirismodules.controller('CommunityManageFileCtrl', ['CommunityService', 'FileService', '$scope', function (CommunityService, FileService, $scope) {

        /* Get stored File details */
        $scope.fileParams = FileService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "File";

         /* Filters */
        $scope.toggleSharingFilters = function () {
            $scope.fileParams.sharedGroupsDisplayFilters = !$scope.fileParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.fileParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.fileTags = ['File', 'Reference', 'testfile'];

        $scope.refreshFile = function() {
            FileService.refreshSelectedOsirisFile('community');
        };

        /* Patch file and update file list */
        $scope.saveFile = function() {
            FileService.updateOsirisFile($scope.fileParams.selectedFile).then(function (data) {
                FileService.refreshOsirisFiles("community");
            });
        };

        $scope.getGeometryStr = function(geoJson){
            return JSON.stringify(geoJson);
        };

    }]);
});
