/*
 * Play JavaScript routing as an AngularJS module.
 * Wraps Play's routes to use Angular's $http.
 * Example:
 * {{{
 * // For `POST /login controller.Application.login()` Play generates:
 * jsRoutes.controllers.Application.login()
 * // With playRoutes, this can be used like this:
 * playRoutes.controllers.Application.login().post({user:"username", password:"secret"}).then(function(response) {
 *   ...
 * )};
 * }}}
 * @author Marius Soutier, 2013
 */
define(["angular", "jsRoutes", "../libs/ngProgress.min"], function (angular, jsRoutes) {
  "use strict";

  // The module - will be referenced by other modules
  var module = angular.module("play.routing", ["ngProgress"]);

  // The service - will be used by controllers or other services, filters, etc.
  module.service("playRoutes", ["$http", "ngProgress", "$location", function ($http, ngProgress, $location) {

    /**
     * Wrap a Play JS function with a new function that adds the appropriate $http method.
     * Note that the url has been already applied to the $http method so you only have to pass in
     * the data (if any).
     * Note: This is not only easier on the eyes, but must be called in a separate function with its own
     * set of arguments, because otherwise JavaScript's function scope will bite us.
     * @param playFunction The function from Play's jsRouter to be wrapped
     */
    var wrapHttp = function (playFunction) {
      return function (/*arguments*/) {
        var routeObject = playFunction.apply(this, arguments);
        var httpMethod = routeObject.method.toLowerCase();
        var url = routeObject.url;
        console.log("url",url);
        var res = {
          method: httpMethod,
          url: url,
          absoluteUrl: routeObject.absoluteURL,
          webSocketUrl: routeObject.webSocketURL
        };
        res[httpMethod] = function (obj) {
          ngProgress.start();
          var promise = $http[httpMethod](url, obj);
          promise.then(function(){
            ngProgress.complete();
            return arguments;
          }, function(reponse){
            ngProgress.complete();
            console.log(reponse.status);
            $location.path("/error");
          })
          return promise;
        };
        return res;
      };
    };

    // Add package object, in most cases 'controllers'
    var addPackageObject = function (packageName, service) {
      if (!(packageName in service)) {
        service[packageName] = {};
      }
    };

    // Add controller object, e.g. Application
    var addControllerObject = function (controllerKey, service) {
      addPackageObject(controllerKey, service);
    };

    var playRoutes = {};

    // checks if the controllerKey starts with a lower case letter
    var isControllerKey = function (controllerKey) {
      return (/^[A-Z].*/.test(controllerKey));
    };

    var addRoutes = function (playRoutesObject, jsRoutesObject) {
      for (var key in jsRoutesObject) {
        if (isControllerKey(key)) {
          var controller = jsRoutesObject[key];
          addControllerObject(key, playRoutesObject);
          for (var methodKey in controller) {
            playRoutesObject[key][methodKey] = wrapHttp(controller[methodKey]);
          }
        } else {
          addPackageObject(key, playRoutesObject);
          addRoutes(playRoutesObject[key], jsRoutesObject[key]);
        }
      }
    };

    addRoutes(playRoutes, jsRoutes);
    return playRoutes;
  }]);

  return module;
});