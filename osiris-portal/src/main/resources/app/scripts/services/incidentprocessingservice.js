/**
 * @ngdoc service
 * @name osirisApp.ProcessingTemplateService
 * @description
 * # ProcessingTemplateService
 * Service in the osirisApp.
 */
'use strict';

define(['../osirismodules', 'traversonHal'], function (osirismodules, TraversonJsonHalAdapter) {


    osirismodules.service('IncidentProcessingService', ['$rootScope', 'traverson', 'UserService', 'MessageService', 'osirisProperties', '$q', function($rootScope, traverson, UserService, MessageService, osirisProperties, $q) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = osirisProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        var baseUrl = rootUri + '/incidentProcessings';


        this.createIncidentProcessing = function(data){

            var postData = Object.assign({}, data, {
                owner: UserService.params.activeUser._links.self.href
            });

            return $q(function(resolve, reject) {
                  halAPI.from(baseUrl + '/')
                           .newRequest()
                           .post(postData)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            resolve(JSON.parse(document.data));
                        } else {
                            MessageService.addError('Could not create incident processing', document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not create incident processing', error);
                        reject();
                    }
                );
            });
        };

        this.createIncidentProcessings = function(processings) {
            let requests = processings.map(function(processing) {
                return self.createIncidentProcessing(processing);
            })
            return $q.all(requests);
        }

        this.updateIncidentProcessing = function(id, data) {
            return $q(function(resolve, reject) {
                halAPI.from(baseUrl + '/' + id)
                    .newRequest()
                    .patch(data)
                    .result
                    .then(
                        function(document) {
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not update incident processing', error);
                            reject();
                        });
            });
        };

        this.removeIncidentProcessing = function(incidentProcessing) {
            return $q(function(resolve, reject) {
                deleteAPI.from(baseUrl + '/' + incidentProcessing.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function(document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('Incident processing deleted', 'Incident processing deleted.');
                                resolve(processingTemplate);
                            } else {
                                MessageService.addError('Could not remove incident processing', document);
                                reject();
                            }
                        }, function(error) {
                            MessageService.addError('Could not remove incident processing', error);
                            reject();
                        });
            });
        };


        this.getIncidentProcessing = function(incidentProcessing) {
            var uri;
            if (incidentProcessing.id) {
                uri = rootUri + '/incidentProcessings/' + incidentProcessing.id + '?projection=detailedIncidentProcessing';
            } else {
                uri = incidentProcessing._links.self.href + '?projection=detailedIncidentProcessing';
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
                        MessageService.addError('Could not get incident processing', error);
                        deferred.reject();
                    });
            return deferred.promise;
        }

    }])
});
