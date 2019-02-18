/**
 * @ngdoc service
 * @name osirisApp.IncidentService
 * @description
 * # IncidentService
 * Service in the osirisApp.
 */
'use strict';

define(['../osirismodules', 'traversonHal'], function(osirismodules, TraversonJsonHalAdapter) {

    osirismodules.service('IncidentService', ['$rootScope', '$http', 'osirisProperties', '$q', '$timeout', 'IncidentTypeService', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'FileService', 'traverson', function($rootScope, $http, osirisProperties, $q, $timeout, IncidentTypeService, MessageService, UserService, TabService, CommunityService, FileService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = osirisProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.dbOwnershipFilters = {
            ALL_INCIDENTS: {id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
            MY_INCIDENTS: {id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner'},
            SHARED_INCIDENTS: {id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner'}
        };

        var typeFilterAll = {
            id: 'all',
            title: 'All incident types'
        };

        this.getIncidentTypeFilters = function() {

            return IncidentTypeService.getIncidentTypes().then(function(response) {
                var types = response.data.map(function(type) {
                    return {
                        title: type.title,
                        id: type._links.self.href
                    };
                });
                types.unshift(typeFilterAll);

                return types;
            });
        }

        this.params = {
            community: {
                pollingUrl: rootUri + '/incidents/?sort=name',
                pagingData: {},
                incidents: undefined,
                items: undefined,
                selectedIncident: undefined,
                searchText: '',
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                selectedOwnershipFilter: self.dbOwnershipFilters.ALL_INCIDENTS,
                selectedTypeFilter: 'all'
            }
        };

        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollIncidents = function(page) {
            pollingTimer = $timeout(function() {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function(document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.incidents', document._embedded.incidents);
                        pollIncidents(page);
                    }, function(error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Incidents', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollIncidents(page);
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

        var getIncidents = function(page) {
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {
                    if (startPolling) {
                        pollIncidents(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.incidents);
                }, function(error) {
                    MessageService.addError('Could not get Incidents', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.createIncident = function(data) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/incidents/')
                    .newRequest()
                    .post(data)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Incident created', 'New Incident ' + data.title + ' created.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not create Incident ' + data.title, error);
                            reject();
                        });
            });
        };

        this.startIncidentProcessing = function(incident) {
            return halAPI.from(rootUri + '/incidents/' + incident.id + '/process')
                .newRequest()
                .post()
                .result
                .then(
                    function(document) {
                        MessageService.addInfo('Incident processing start', 'Incident processing for ' + incident.title + ' started.');
                        return document.data;
                    }, function(error) {
                        MessageService.addError('Could not start incident processing for incident ' + incident.title, error);
                    });
        }

        this.removeIncident = function(incident) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/incidents/' + incident.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function(document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('Incident deleted', 'Incident ' + incident.name + ' deleted.');
                                resolve(incident);
                            } else {
                                MessageService.addError('Could not remove Incident ' + incident.name, document);
                                reject();
                            }
                        }, function(error) {
                            MessageService.addError('Could not remove Incident ' + incident.name, error);
                            reject();
                        });
            });
        };

        this.updateIncident = function(data) {
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/incidents/' + data.id)
                    .newRequest()
                    .patch(data)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Incident successfully updated', 'Incident ' + data.id + ' updated.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not update Incident ' + data.id, error);
                            reject();
                        });
            });
        };

        var getIncident = function(incident) {
            var deferred = $q.defer();
            var uri;
            if (incident.id) {
                uri = rootUri + '/incidents/' + incident.id + '?projection=detailedIncident';
            } else {
                uri = incident._links.self.href + '?projection=detailedIncident';
            }

            halAPI.from(uri)
                .newRequest()
                .getResource()
                .result
                .then(
                    function(document) {
                        deferred.resolve(document);
                    }, function(error) {
                        MessageService.addError('Could not get Incident: ' + incident.name, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.refreshIncidents = function(page, action, incident) {
            if (self.params[page]) {
                /* Get incident list */
                return getIncidents(page).then(function(data) {

                    self.params[page].incidents = data;

                    /* Select last incident if created */
                    if (action === "Create") {
                        self.params[page].selectedIncident = incident;
                    }

                    /* Clear incident if deleted */
                    if (action === "Remove") {
                        if (incident && self.params[page].selectedIncident && incident.id === self.params[page].selectedIncident.id) {
                            self.params[page].selectedIncident = undefined;
                            self.params[page].items = [];
                        }
                    }

                    /* Update the selected incident */
                    return self.refreshSelectedIncident(page);
                });
            }
        };

        /* Fetch a new page */
        this.getIncidentsPage = function(page, url) {
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getIncidents(page).then(function(data) {
                    self.params[page].incidents = data;
                });
            }
        };

        this.getIncidentsByFilter = function(page) {
            if (self.params[page]) {
                var url = rootUri + '/incidents/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if (self.params[page].selectedOwnershipFilter !== self.dbOwnershipFilters.ALL_INCIDENTS) {
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }

                if (self.params[page].selectedTypeFilter !== typeFilterAll.id) {
                    url += '&incidentType=' + self.params[page].selectedTypeFilter;
                }
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getIncidents(page).then(function(data) {
                    self.params[page].incidents = data;
                });
            }
        };


        this.refreshSelectedIncident = function(page) {
            if (self.params[page]) {
                /* Get incident contents if selected */
                if (self.params[page].selectedIncident &&
                        (self.params[page].selectedIncident.id || self.params[page].selectedIncident._links)) {

                    return getIncident(self.params[page].selectedIncident).then(function(incident) {
                        self.params[page].selectedIncident = incident;
                    });
                }
            }
        };

        return this;
    }]);
});
