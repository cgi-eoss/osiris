/**
 * @ngdoc service
 * @name osirisApp.CollectionService
 * @description
 * # CollectionService
 * Service in the osirisApp.
 */
'use strict';

define(['../osirismodules', 'traversonHal', 'moment'], function(osirismodules, TraversonJsonHalAdapter, moment) {

    osirismodules.service('CollectionService', ['$rootScope', '$http', 'osirisProperties', '$q', '$timeout', '$mdDialog', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'FileService', 'traverson', function($rootScope, $http, osirisProperties, $q, $timeout, $mdDialog, MessageService, UserService, TabService, CommunityService, FileService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = osirisProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.dbOwnershipFilters = {
            ALL_COLLECTIONS: {id: 0, name: 'All'},
            MY_COLLECTIONS: {id: 1, name: 'Mine'},
            SHARED_COLLECTIONS: {id: 2, name: 'Shared'}
        };

        this.fileTypes = [
            {name: 'Output product', value: 'OUTPUT_PRODUCT'},
            {name: 'Reference data', value: 'REFERENCE_DATA'}
        ]

        var uploadFileTypes = [
            { name: "GeoTIFF", value: "GEOTIFF"},
            { name: "Shapefile", value: "SHAPEFILE"},
            { name: "Other", value: "OTHER"}
        ];

        this.fileTypeFilters = this.fileTypes.slice();
        this.fileTypeFilters.unshift({
            name: 'All', value: ''
        });

        this.params = {
            community: {
                pagingData: {},
                collections: undefined,
                selectedCollection: undefined,
                searchParams: {
                    ownership: self.dbOwnershipFilters.ALL_COLLECTIONS,
                    fileType: null,
                    searchText: null
                },
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
            }
        };

        var polling = {
            frequency: 20 * 1000,
            remainingAttempts: 3,
            pollTimeout: null
        }

        var buildSearchUrlFromParams = function(params) {

            var url = rootUri + '/collections/search/parametricFind?sort=name';

            if (params.ownership === self.dbOwnershipFilters.MY_COLLECTIONS) {
                url += '&owner=' + UserService.params.activeUser._links.self.href
            } else if (params.ownership === self.dbOwnershipFilters.SHARED_COLLECTIONS) {
                url += '&notOwner=' + UserService.params.activeUser._links.self.href
            }

            if (params.fileType) {
                url += '&fileType=' + params.fileType;
            }

            if (params.searchText) {
                url += '&filter=' + params.searchText;
            }

            return url;
        }

        var updateCollectionsForState = function(state) {
            var deferred = $q.defer();
            halAPI.from(state.pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {

                    state.pagingData = {
                        _links: document._links,
                        page: document.page
                    }
                    state.collections = document._embedded.collections;

                    deferred.resolve(document);
                }, function(error) {
                    MessageService.addError('Could not get Collections', error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        var setPollingTimeout = function(state) {
            polling.pollTimeout = $timeout(function() {
                updateCollectionsForState(state).then(function() {
                    setPollingTimeout(state);
                }, function(error) {
                    if (polling.remainingAttempts) {
                        polling.remainingAttempts--;
                        setPollingTimeout(state);
                    }
                });
            }, polling.frequency);
        }

        this.refreshCollections = function(page, action, collection) {

            var state = self.params[page];

            if (state) {

                self.stopPolling();

                state.pollingUrl = buildSearchUrlFromParams(state.searchParams);

                /* Get collection list */
                updateCollectionsForState(state).then(function(data) {

                    /* Select last collection if created */
                    if (action === "Create") {
                        self.params[page].selectedCollection = collection;
                    }

                    /* Clear collection if deleted */
                    if (action === "Remove") {
                        if (collection && self.params[page].selectedCollection && collection.id === self.params[page].selectedCollection.id) {
                            self.params[page].selectedCollection = undefined;
                            self.params[page].items = [];
                        }
                    }

                    /* Update the selected collection */
                    self.refreshSelectedCollection(page);

                }).finally(function() {
                    setPollingTimeout(state);
                });

            }
        }

        this.refreshSelectedCollection = function(page) {

            var state = self.params[page];

            if (state) {
                /* Get collection contents if selected */
                if (state.selectedCollection) {

                    getCollection(state.selectedCollection).then(function(collection) {
                        state.selectedCollection = collection;

                        if (page === 'community') {
                            CommunityService.getObjectGroups(collection, 'collection').then(function(data) {
                                state.sharedGroups = data;
                            });
                        }
                    });
                }
            }
        };

        this.refreshCollectionsFromUrl = function(page, url) {

            var state = self.params[page];

            if (state) {
                state.pollingUrl = url;
                updateCollectionsForState(state);
            }
        };


        this.stopPolling = function() {
            if (polling.pollTimeout) {
                $timeout.cancel(polling.pollTimeout);
                delete polling.pollTimeout;
            }
        };

        this.createCollection = function(data) {
            return $q(function(resolve, reject) {
                var collection = {
                    name: data.name,
                    description: (data.description ? data.description : ''),
                    fileType: data.fileType,
                    productsType: data.productsType
                };
                halAPI.from(rootUri + '/collections/')
                    .newRequest()
                    .post(collection)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Collection created', 'New Collection ' + name + ' created.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not create Collection ' + name, error);
                            reject();
                        });
            });
        };

        this.removeCollection = function(collection) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/collections/' + collection.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function(document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('Collection deleted', 'Collection ' + collection.name + ' deleted.');
                                resolve(collection);
                            } else {
                                MessageService.addError('Could not remove Collection ' + collection.name, document);
                                reject();
                            }
                        }, function(error) {
                            MessageService.addError('Could not remove Collection ' + collection.name, error);
                            reject();
                        });
            });
        };

        this.updateCollection = function(collection) {
            var newcollection = {name: collection.name, description: collection.description, productsType: collection.productsType};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/collections/' + collection.id)
                    .newRequest()
                    .patch(newcollection)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Collection successfully updated', 'Collection ' + collection.name + ' updated.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not update Collection ' + collection.name, error);
                            reject();
                        });
            });
        };

        var getCollection = function(collection) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/collections/' + collection.id + '?projection=detailedCollection')
                .newRequest()
                .getResource()
                .result
                .then(
                    function(document) {
                        deferred.resolve(document);
                    }, function(error) {
                        MessageService.addError('Could not get Collection: ' + collection.name, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.findCollections = function(params) {
            var url = buildSearchUrlFromParams(params);

            return halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {
                    return document._embedded.collections;
                });
        }

        this.addReferenceFileToCollection = function ($event, collection, userProperties) {
            function AddReferenceFileDialog($scope, $mdDialog, FileService) {

                $scope.item = "File";
                $scope.fileTypes = uploadFileTypes;
                $scope.fileParams = FileService.params.community;
                $scope.newReference = {
                    userProperties: userProperties || {},
                    collection: collection
                };
                $scope.validation = "Valid";
                $scope.rangeFieldEnabled = userProperties ? (userProperties.startTime || userProperties.endTime ? true : false) : false

                $scope.validateFile = function (file) {
                    if(!file) {
                        $scope.validation = "No file selected";
                    } else if (file.name.indexOf(' ') >= 0) {
                        $scope.validation = "Filename cannot contain white space";
                    } else if (file.size >= (1024*1024*1024*2)) {
                        $scope.validation = "Filesize cannot exceed 2GB";
                    } else {
                        $scope.validation = "Valid";
                    }
                };

                $scope.searchCollection = function() {
                    return self.findCollections({
                        searchText: $scope.collectionSearchString,
                        fileType: 'REFERENCE_DATA'
                    }).then(function(collections) {
                        return collections.map(function(collection) {
                            return {
                                id: collection.id,
                                identifier: collection.identifier,
                                name: collection.name
                            };
                        })
                    });
                }

                $scope.updateFieldsForFileType = function() {
                    $scope.geometryFieldEnabled = false;
                    $scope.showGeometryField = $scope.newReference.fileType === 'OTHER';
                }

                $scope.onStartDateChange = function() {
                    var data = $scope.newReference.userProperties;
                    if (!data.endTime || data.endTime < data.startTime) {
                        data.endTime = data.startTime;
                    }
                }

                $scope.onEndDateChange = function() {
                    var data = $scope.newReference.userProperties;
                    if (!data.startTime || data.startTime > data.endTime) {
                        data.startTime = data.endTime;
                    }
                }
                /* Upload the file */
                $scope.addReferenceFile = function () {

                    var userProperties = Object.assign({}, $scope.newReference.userProperties);
                    if ($scope.newReference.fileType === 'OTHER' && !userProperties.geometry) {
                        userProperties.geometry = 'POINT(0 100)';
                    }

                    userProperties.startTime = userProperties.endTime || userProperties.startTime;
                    userProperties.endTime = userProperties.endTime || userProperties.startTime;

                    if (userProperties.startTime) {
                        userProperties.startTime = moment(userProperties.startTime).format('YYYY-MM-DD[T00:00:00Z]');
                        userProperties.endTime = moment(userProperties.endTime).format('YYYY-MM-DD[T23:59:59Z]');
                    }

                    FileService.uploadFile("community", {
                        file: $scope.newReference.file,
                        fileType: $scope.newReference.fileType,
                        collection: $scope.newReference.collection.identifier,
                        userProperties: userProperties
                    }).then(function (response) {
                        /* Get updated list of reference data */
                        FileService.refreshOsirisFiles("community");
                    });
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };

            }
            AddReferenceFileDialog.$inject = ['$scope', '$mdDialog', 'FileService'];
            $mdDialog.show({
                controller: AddReferenceFileDialog,
                templateUrl: 'views/community/templates/addreferencedata.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

        return this;
    }]);
});
