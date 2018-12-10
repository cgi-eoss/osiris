/**
 * @ngdoc service
 * @name osirisApp.BasketService
 * @description
 * # BasketService
 * Service in the osirisApp.
 */
'use strict';

define(['../osirismodules'], function(osirismodules) {

    osirismodules.service('AoiService', ['$rootScope', '$http', 'osirisProperties', '$q', '$timeout', 'UserPrefsService', function($rootScope, $http, osirisProperties, $q, $timeout, userPrefsService) {

        var self = this;

        var searchAoi = null;

        this.getSavedAois = function() {
            return userPrefsService.getPreferences('aoi');
        };

        this.getSavedAoiGeometry = function(aoi) {
            return userPrefsService.getPreference(aoi.id).then(function(data) {
                return JSON.parse(data.preference);
            });
        };

        this.saveAoi = function(name, geometry) {
            return userPrefsService.setPreference('aoi', name, JSON.stringify(geometry));
        };

        this.updateAoi = function(name, geometry) {
            return userPrefsService.updatePreferenceWithName('aoi', name, JSON.stringify(geometry));
        };

        this.deleteAoi = function(id) {
            return userPrefsService.deletePreference(id);
        };

        this.setSearchAoi = function(aoi) {
            searchAoi = aoi;
        };

        this.getSearchAoi = function() {
            return searchAoi;
        };

        return this;
    }]);
});
