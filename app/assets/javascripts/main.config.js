(function() {
  'use strict';

  angular.module("app").config(ConfigConfig);

  ConfigConfig.$inject = ["toastrConfig"];

   function ConfigConfig(toastrConfig){
       angular.extend(toastrConfig, {
        autoDismiss: false,
        timeOut: 2000,
        extendedTimeOut: 300,
        containerId: 'toast-container',
        maxOpened: 0,
        newestOnTop: true,
        positionClass: 'toast-top-center',
        preventDuplicates: false,
        preventOpenDuplicates: false,
        target: 'body'
      });
    }

})();