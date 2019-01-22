/**
 * @ngdoc service
 * @name osirisApp.IncidentTypeService
 * @description
 * # IncidentTypeService
 * Service in the osirisApp.
 */
'use strict';

define(['../osirismodules', 'traversonHal'], function(osirismodules, TraversonJsonHalAdapter) {

    osirismodules.service('IncidentTypeService', ['$rootScope', '$http', 'osirisProperties', '$q', '$timeout', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'FileService', 'traverson', function($rootScope, $http, osirisProperties, $q, $timeout, MessageService, UserService, TabService, CommunityService, FileService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = osirisProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.dbOwnershipFilters = {
            ALL_INCIDENT_TYPES: {id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
            MY_INCIDENT_TYPES: {id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner'},
            SHARED_INCIDENT_TYPES: {id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner'}
        };

        this.params = {
            community: {
                pollingUrl: rootUri + '/incidentTypes/?sort=name',
                pagingData: {},
                incidentTypes: undefined,
                items: undefined,
                selectedIncidentType: undefined,
                searchText: '',
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                selectedOwnershipFilter: self.dbOwnershipFilters.ALL_INCIDENT_TYPES
            }
        };

        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollIncidentTypes = function(page) {
            pollingTimer = $timeout(function() {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function(document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.incidentTypes', document._embedded.incidentTypes);
                        pollIncidentTypes(page);
                    }, function(error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll IncidentTypes', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollIncidentTypes(page);
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function() {
            if (pollingTimer) {
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        var getIncidentTypes = function(page) {
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {
                    if (startPolling) {
                        pollIncidentTypes(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.incidentTypes);
                }, function(error) {
                    MessageService.addError('Could not get IncidentTypes', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.createIncidentType = function(data) {
            return $q(function(resolve, reject) {
                var incidentType = {title: data.title, description: (data.description ? data.description : ''), iconId: data.iconId};
                halAPI.from(rootUri + '/incidentTypes/')
                    .newRequest()
                    .post(incidentType)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Incident type created', 'New Incident type ' + name + ' created.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not create IncidentType ' + name, error);
                            reject();
                        });
            });
        };

        this.removeIncidentType = function(incidentType) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/incidentTypes/' + incidentType.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function(document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('IncidentType deleted', 'IncidentType ' + incidentType.name + ' deleted.');
                                resolve(incidentType);
                            } else {
                                MessageService.addError('Could not remove IncidentType ' + incidentType.name, document);
                                reject();
                            }
                        }, function(error) {
                            MessageService.addError('Could not remove IncidentType ' + incidentType.name, error);
                            reject();
                        });
            });
        };

        this.updateIncidentType = function(incidentType) {
            var newincidentType = {title: incidentType.title, description: incidentType.description, iconId: incidentType.iconId};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/incidentTypes/' + incidentType.id)
                    .newRequest()
                    .patch(newincidentType)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Incident type successfully updated', 'Incident type ' + incidentType.name + ' updated.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not update Incident type ' + incidentType.name, error);
                            reject();
                        });
            });
        };

        var getIncidentType = function(incidentType) {

            var uri;
            if (incidentType.id) {
                uri = rootUri + '/incidentTypes/' + incidentType.id + '?projection=detailedIncidentType';
            } else {
                uri = incidentType._links.self.href + '?projection=detailedIncidentType';
            }

            var deferred = $q.defer();
            halAPI.from(uri)
                .newRequest()
                .getResource()
                .result
                .then(
                    function(document) {
                        deferred.resolve(document);
                    }, function(error) {
                        MessageService.addError('Could not get Incident type: ' + incidentType.name, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.refreshIncidentTypes = function(page, action, incidentType) {
            if (self.params[page]) {
                /* Get incidentType list */
                getIncidentTypes(page).then(function(data) {

                    self.params[page].incidentTypes = data;

                    /* Select last incidentType if created */
                    if (action === "Create") {
                        self.params[page].selectedIncidentType = incidentType;
                    }

                    /* Clear incidentType if deleted */
                    if (action === "Remove") {
                        if (incidentType && self.params[page].selectedIncidentType && incidentType.id === self.params[page].selectedIncidentType.id) {
                            self.params[page].selectedIncidentType = undefined;
                            self.params[page].items = [];
                        }
                    }

                    /* Update the selected incidentType */
                    self.refreshSelectedIncidentType(page);
                });
            }
        };

        /* Fetch a new page */
        this.getIncidentTypesPage = function(page, url) {
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getIncidentTypes(page).then(function(data) {
                    self.params[page].incidentTypes = data;
                });
            }
        };

        this.getIncidentTypesByFilter = function(page) {
            if (self.params[page]) {
                var url = rootUri + '/incidentTypes/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if (self.params[page].selectedOwnershipFilter !== self.dbOwnershipFilters.ALL_INCIDENT_TYPES) {
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getIncidentTypes(page).then(function(data) {
                    self.params[page].incidentTypes = data;
                });
            }
        };


        this.refreshSelectedIncidentType = function(page) {
            if (self.params[page]) {
                /* Get incidentType contents if selected */
                if (self.params[page].selectedIncidentType) {

                    getIncidentType(self.params[page].selectedIncidentType).then(function(incidentType) {
                        self.params[page].selectedIncidentType = incidentType;
                    });
                }
            }
        };

        this.getIncidentTypes = function() {

            var url = rootUri + '/incidentTypes';

            url += '?sort=name&size=100';

            return halAPI.from(url)
            .newRequest()
            .getResource()
            .result
            .then(function (document) {
                return {
                    data: document._embedded.incidentTypes
                };
            }, function (error) {
                MessageService.addError('Could not get incident yypes', error);
            });
        };

        var iconLibrary = null;
        this.getIconsLibrary = function() {

            if (!iconLibrary) {
                let iconsRoot = 'images/incident-icon-library/';

                return $http.get(iconsRoot + 'icons.json').then(function(response) {
                    let iconLibrary = {};
                    for (var id in response.data) {
                        iconLibrary[id] = {
                            id: id,
                            title: response.data[id].title,
                            path: iconsRoot + response.data[id].path
                        };
                    }

                    return iconLibrary;
                });
            } else {
                return Promise.resolve(iconLibrary);
            }
        };

        return this;
    }]);
});
