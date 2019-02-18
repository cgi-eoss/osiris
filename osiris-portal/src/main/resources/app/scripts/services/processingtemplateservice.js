/**
 * @ngdoc service
 * @name osirisApp.ProcessingTemplateService
 * @description
 * # ProcessingTemplateService
 * Service in the osirisApp.
 */
'use strict';

define(['../osirismodules', 'traversonHal'], function (osirismodules, TraversonJsonHalAdapter) {


    osirismodules.service('ProcessingTemplateService', ['$rootScope', 'traverson', 'UserService', 'MessageService', 'osirisProperties', '$q', function($rootScope, traverson, UserService, MessageService, osirisProperties, $q) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = osirisProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        var baseUrl = rootUri + '/incidentProcessingTemplates';

        var processingServicesRequest = null;

        var retrieveProcessingServices = function() {

            let url = rootUri + '/services/search/findByFilterOnly';

            url += '?sort=name&size=100'

            processingServicesRequest = halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    return document._embedded.services.filter(function(service) {
                        return service.type !== 'APPLICATION';
                    })
                }, function (error) {
                    MessageService.addError('Could not get services', error);
                });

        }

        this.getProcessingServices = function(expr) {
            if (!processingServicesRequest) {
                retrieveProcessingServices();
            }
            expr = expr || '';
            expr = expr.toLowerCase();
            return processingServicesRequest.then(function(services) {
                return services.filter(function(service) {
                    return service.name.toLowerCase().indexOf(expr) !== -1;
                });
            });
        }

        this.createProcessingTemplate = function(data){

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
                            MessageService.addInfo('Processing template added', 'New Template ' + data.title + ' added.');
                            resolve(JSON.parse(document.data));
                        } else {
                            MessageService.addError('Could not create Template ' + data.title, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not add Template ' + data.title, error);
                        reject();
                    }
                );
            });
        };

        this.updateProcessingTemplate = function(id, data) {
            return $q(function(resolve, reject) {
                halAPI.from(baseUrl + '/' + id)
                    .newRequest()
                    .patch(data)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Processing template successfully updated', 'Processing template ' + data.title + ' updated.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not update processing template ' + data.title, error);
                            reject();
                        });
            });
        };

        this.removeProcessingTemplate = function(processingTemplate) {
            return $q(function(resolve, reject) {
                deleteAPI.from(baseUrl + '/' + processingTemplate.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function(document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('Processing template deleted', 'Processing template ' + processingTemplate.title + ' deleted.');
                                resolve(processingTemplate);
                            } else {
                                MessageService.addError('Could not remove processing template ' + processingTemplate.title, document);
                                reject();
                            }
                        }, function(error) {
                            MessageService.addError('Could not remove processing template ' + processingTemplate.title, error);
                            reject();
                        });
            });
        };


        this.getProcessingTemplate = function(processingTemplate) {
            var uri;
            if (processingTemplate.id) {
                uri = rootUri + '/incidentProcessingTemplates/' + processingTemplate.id + '?projection=detailedIncidentProcessingTemplate';
            } else {
                uri = processingTemplate._links.self.href + '?projection=detailedIncidentProcessingTemplate';
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
                        MessageService.addError('Could not get processing template: ' + processingTemplate.title, error);
                        deferred.reject();
                    });
            return deferred.promise;
        }

    }])
});
