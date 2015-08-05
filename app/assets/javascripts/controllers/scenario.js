define(["angular"], function (angular) {
    "use strict";
    return {
        ScenarioCtrl: function ($rootScope, $scope, playRoutes, ngProgress, ClientService) {
            $scope.newRow = {};
            $scope.scenario_types = ["swing", "service", "web"];
            $scope.selectedType = "";
            $scope.importModes = ["prepend", "append"];
            $scope.scenarii = [];
            $scope.regexList = [];
            $scope.regexMap = [];
            $scope.scenario = undefined;
            $scope.stepType = "";

            $scope.add = add;
            $scope.addRow = addRow;
            $scope.save = save;
            $scope.deleteRow = deleteRow;
            $scope.importScenario = importScenario;
            $scope.onPatternValueChange = onPatternValueChange;
            $scope.convertToTemplate = convertToTemplate;
            $scope.editScenario = editScenario;
            $scope.deleteScenarii = deleteScenarii;
            $scope.swaptToSwingRow = swaptToSwingRow;
            $scope.swaptToWebRow = swaptToWebRow;
            $scope.swaptToServiceRow = swaptToServiceRow;
            $scope.addRowBefore = addRowBefore;

            __init__();
            
            function editScenario(scenario){
                $scope.scenario = scenario; 
                swaptToDefaultRow();
            }

            ////////// trigger the right event !! //////////////
            function swaptToSwingRow(){
                $scope.stepType = "swing";
                $scope.regexList = $scope.regexMap[$scope.stepType];
            }

            function swaptToServiceRow(){
                $scope.stepType = "service";
                $scope.regexList = $scope.regexMap[$scope.stepType];
            }

            function swaptToWebRow(){
                $scope.stepType = "web";
                $scope.regexList = $scope.regexMap[$scope.stepType];
            }

            function swaptToDefaultRow(){
                $scope.stepType = $scope.scenario.type;   
                $scope.regexList = $scope.regexMap[$scope.stepType];
            }
            //////////////////////////////////////////////////

            function add() {
                playRoutes.controllers.ScenarioController.loadScenarioCtx($scope.selectedType).get().then(function (response) {
                    var scenarioDescriptor = response.data;
                    var newScenario = {
                        type: $scope.selectedType.type,
                        driver: $scope.selectedType.name, //related service
                        columns: scenarioDescriptor,
                        rows: []
                    }
                    $scope.scenarii.push(newScenario);
                    $scope.scenario = newScenario;
                    $scope.stepType = $scope.selectedType;
                });
            };

            function addRow(newRow) {
                newRow.kind = $scope.stepType;
                $scope.scenario.rows.push(newRow);
                $scope.newRow = {};
            };


            function addRowBefore(scenario, newRow, currentRow) {
            
            };

            function deleteRow(scenario, row) {
                //ajax call directly, if not new !
                scenario.rows.splice(scenario.rows.indexOf(row), 1);
            };

            function save() {
                var scenarioCopy = angular.copy($scope.scenario);
                scenarioCopy.rows = JSON.stringify(scenarioCopy.rows);
                delete scenarioCopy.columns;
                playRoutes.controllers.ScenarioController.saveScenarii().post(scenarioCopy).then(function () {
                    __init__();
                });
            };

            function saveScenarii(scenarii){
                var scenarioCopy = angular.copy(scenarii);
                scenarioCopy.rows = JSON.stringify(scenarioCopy.rows);
                delete scenarioCopy.columns;
                playRoutes.controllers.ScenarioController.saveScenarii().post(scenarioCopy).then(function () {
                    __init__();
                });

            }

            function deleteScenarii(scenario){
                playRoutes.controllers.ScenarioController.deleteScenarii().post(angular.toJson(scenario.id)).then(function () {
                    __init__();
                });
            }

            function importScenario(scenario) {
                var mode = scenario.selectedImportMode;
                var toImport = scenario.imp;
                if (mode == "prepend") {
                    scenario.rows = angular.copy(toImport.rows).concat(scenario.rows);
                } else if (mode == "append") {
                    scenario.rows = scenario.rows.concat(angular.copy(toImport.rows));
                }
                delete scenario.imp;
                delete scenario.selectedImportMode;
            };

            function convertToTemplate(scenario){
                var newScenarioTemplate = scenario;
                for(var i = 0 ; i < newScenarioTemplate.rows.length ; i++){
                    var actionType = getActionType(newScenarioTemplate, newScenarioTemplate.rows[i]) || 'swing';
                    newScenarioTemplate.rows[i].kind = actionType;
                    var regexList = $scope.regexMap[actionType]; 
                    var sentence = removeHeadAnnotation(newScenarioTemplate.rows[i].patterns);
                    for(var j=0; j < regexList.length; j++){
                        var replacedSentence = ClientService.convertToRegexSentence(regexList[j].typed_sentence);
                        var regex = new RegExp(replacedSentence, 'i');
                        if(regex.test(sentence)){
                            var typeSentence = regexList[j].typed_sentence;
                            var pattern = ClientService.convertToPatternSentence(typeSentence);
                            var scenarioRow = newScenarioTemplate.rows[i];
                            setMappingForScenarioRow(scenarioRow, pattern, typeSentence);
                            break;
                        }
                    } 
                }
                newScenarioTemplate.template = false;
                saveScenarii(newScenarioTemplate);
            }

            function removeHeadAnnotation(sentence){
                var regex = /(swing|web|service|driverLess):?([\w]*)? ([\w\W]+)/
                var tail;
                if(tail = regex.exec(sentence)){
                    return tail[3];
                }
                return sentence;
            }

            function getActionType(scenario, row){
                if(row.patterns.startsWith("@service")){
                    return "service";
                }
                else if (row.patterns.startsWith("@web")){
                    return "service";
                }
                else if (row.patterns.startsWith("@swing")){
                    return "swing";
                }
                else {
                    return scenario.type;
                }
            }

            function setMappingForScenarioRow(scenarioRow, pattern, typeSentence){
                var scenarioSentenceWithValues = scenarioRow.patterns;
                scenarioRow.patterns = typeSentence;
                var patternValue = pattern;
                var tag = getRegexTag(patternValue);
                var tags = [];
                var tagPosition = 0;
                while (tag != null) {
                    tags.push(tag);
                    var tagName = tags[tagPosition][0];
                    var varType = tags[tagPosition][3];
                    var mappingValue = scenarioSentenceWithValues.split(" ")[getIndex(pattern.split(" "), tagName)];
                    patternValue = replaceIndex(patternValue, tagName,  tags[tagPosition].index , mappingValue);
                    mappingValue = mappingValue.replace(/\*/g, '');
                    onPatternValueChange(scenarioRow, tagPosition, varType == "component" ? varType : tagPosition.toString(), mappingValue);
                    tagPosition = tagPosition + 1;
                    tag = getRegexTag(patternValue);
                }
            }

            //performs model update
            function onPatternValueChange(row, position, identifier, value) {
                var newVal = {id: identifier, pos: position, val: value};
                if (angular.isUndefined(row.mappings)) {
                    row.mappings = [];
                    row.mappings.push(newVal);
                } else {
                    var found = false;
                    for (var i = 0; i < row.mappings.length; i++) {
                        if (row.mappings[i].pos == position) {
                            row.mappings[i] = newVal;
                            found = true;
                        }
                    }
                    if (!found) {
                        row.mappings.push(newVal);
                    }
                }
            }



            /** util functions */
            function getRegexTag(sentence){
                var tagRegex = /(@)\[\[(\d+):([\w\s@\.,-\/#!$%\^&\*;:{}=\-_`~()]+):([\w\s@\.,-\/#!$%\^&\*;:{}=\-_`~()]+)\]\]/gi
                var tag = tagRegex.exec(sentence);
                return tag;
            }

            function getIndex(array, word){
                for(var i = 0 ; i< array.length; i++){
                    if(array[i] == word){
                        return i;
                    }
                }
            }

            function replaceIndex(string, regex, at, repl) {
               return string.replace(regex, function(match, i) {
                    if( i === at ) return repl;
                    return match;
                });
            }                    

            function __init__() {
                for(var i =0 ; i < $scope.scenario_types.length; i++){
                    var scenariiKind = $scope.scenario_types[i];
                    ClientService.loadRegexList(scenariiKind, function(scenariiKind, list){
                         $scope.regexList = $scope.regexList.concat(list || []);
                         $scope.regexMap[scenariiKind] = list;
                    });
                }   

                playRoutes.controllers.ScenarioController.loadScenarii().get().then(function (response) {
                    var data = response.data || [];
                    data.map(function (scenario) {
                        try{
                            scenario.rows = angular.isObject(scenario.rows) ? scenario.rows : JSON.parse(scenario.rows);
                            var isTemplate = true;
                            for(var i = 0 ; i < scenario.rows.length ; i++){
                                if(angular.isDefined(scenario.rows[i].mappings) && scenario.rows[i].mappings.length > 0){
                                    isTemplate = false;
                                    break;
                                }
                            }
                            scenario.template = isTemplate;
                        }catch(e){
                            if(!angular.isObject(scenario.rows)){
                                //convert it into rows
                                var lines = scenario.rows.split( "\n" );
                                scenario.template = true;
                                scenario.rows = [];
                                for(var i = 0; i< lines.length; i++){
                                    scenario.rows.push({"patterns" : lines[i]});
                                }
                            }
                        }
                        return scenario;
                    });
                    $scope.scenarii = data;
                });
            }
            
        }
    };
});