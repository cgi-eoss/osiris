/**
 * @ngdoc function
 * @name osirisApp.controller:HelpdeskCtrl
 * @description
 * # HelpdeskCtrl
 * Controller of the osirisApp
 */
'use strict';
define(['../../osirismodules'], function (osirismodules) {

    osirismodules.controller('HelpdeskCtrl', ['osirisProperties', '$scope', '$http', 'ProductService', 'TabService', 'MessageService', function (osirisProperties, $scope, $http, ProductService, TabService, MessageService) {

        /* Sidenav & Bottombar */
        $scope.navInfo = TabService.navInfo.admin;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };
        /* End Sidenav & Bottombar */

        $scope.applications = [];
        $scope.processors = [];

        ProductService.getDefaultServices().then(function (services) {
            if(services){
                for(var i=0; i<services.length; i++){
                    if(services[i].type === 'APPLICATION'){
                        $scope.applications.push(services[i].name);
                    }
                    else if(services[i].type === 'PROCESSOR' || services[i].type === 'BULK_PROCESSOR' || services[i].type === 'PARALLEL_PROCESSOR'){
                        $scope.processors.push(services[i].name);
                    }
                }
            }
        });

        $scope.osirisUrl = osirisProperties.OSIRIS_URL;
        $scope.osirisPortalUrl = osirisProperties.OSIRIS_PORTAL_URL;

        $scope.videos = [
                 {
                url: $scope.osirisUrl + '/manual/search_create_databasket.mp4',
                     description: 'Perform a Search and create/manage databasket',
                     image: 'images/helpdesk/search.jpg'
            }, {
                url: $scope.osirisUrl + '/manual/run_sentinel2_ndvi.mp4',
                     description: 'Run an NDVI service',
                     image: 'images/helpdesk/service_NDVI.jpg'
            }, {
                url: $scope.osirisUrl + '/manual/open_ndvi_snap.mp4',
                     description: 'Open a product using Sentinel2 Toolbox',
                     image: 'images/helpdesk/NDVI_product_toolbox.jpg'
            }, {
                url: $scope.osirisUrl + 'manual/vegindex_snap.mp4',
                     description: 'Create a VegetaionIndex and open result in Sentinel2 Toolbox',
                     image: 'images/helpdesk/vegind_product_toolbox.jpg'
                 }
        ];

        $scope.hideContent = true;
        var navbar, sidenav, tutorials;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'tutorials':
                    tutorials = true;
                    break;

            }

            if (navbar && sidenav && tutorials) {
                $scope.hideContent = false;
            }
        };

    }]);
});

