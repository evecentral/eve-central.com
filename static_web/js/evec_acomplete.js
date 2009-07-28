EVEC.SystemSearch = function(name){
	// Instantiate an XHR DataSource and define schema as an array:
	//     ["Multi-depth.object.notation.to.find.a.single.result.item",
	//     "Query Key",
	//     "Additional Param Name 1",
	//     ...
	//     "Additional Param Name n"]


	this.oACDS = new YAHOO.widget.DS_XHR("/json_tools/system_search", ["results","name", "id", "type"]);
	this.oACDS.queryMatchContains = true;
	this.oACDS.responseType = YAHOO.widget.DS_XHR.TYPE_JSON;
	this.oACDS.scriptQueryParam = 'name';


	// Instantiate AutoComplete
	this.oAutoComp = new YAHOO.widget.AutoComplete(name + "s",name + "ysearchcontainer", this.oACDS);
	this.oAutoComp.useShadow = true;
	this.oAutoComp.animSpeed = 0.15;
	this.oAutoComp.maxResultsDisplayed = 15;
	this.oAutoComp.typeAhead = true;

	this.oAutoComp.minQueryLength = 2;
	this.oAutoComp.forceSelection = true;

	this.updateId = function (e, args) {
		YAHOO.util.Dom.get(name).value = args[2][1];
	}




	this.oAutoComp.formatResult = function(oResultItem, sQuery) {

		return oResultItem[0] + " --- <i>" + oResultItem[2]+ "</i>";
	};

	//this.oAutoComp.typeAhead = true;

	this.oAutoComp.itemSelectEvent.subscribe(this.updateId);

	// Stub for form validation
	this.validateForm = function() {
		var ret=  false;
		if (YAHOO.util.Dom.get(name).value == "-1") {
			alert("No system/region selected");
			return false;
		}

		return true;
	};
};

var ac1 = new EVEC.SystemSearch("system1");
var ac2 = new EVEC.SystemSearch("system2");