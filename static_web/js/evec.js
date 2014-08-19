EVEC = {};

var urlParams = {};
(function () {
    var match,
    pl     = /\+/g,  // Regex for replacing addition symbol with a space
    search = /([^&=]+)=?([^&]*)/g,
    decode = function (s) { return decodeURIComponent(s.replace(pl, " ")); },
    query  = window.location.search.substring(1);

    while (match = search.exec(query))
	urlParams[decode(match[1])] = decode(match[2]);
})();


(function() {
    

    Handlebars.registerHelper("formatPrice", function(price) {
	price = parseFloat(price.toString()); // make sure we have a string

	if (price < 100000) { // 100k
	    return new Handlebars.SafeString(price.toFixed(2).replace(/(^\d{1,3}|\d{3})(?=(?:\d{3})+(?:$|\.))/g, '$1,'));
	} else if (price < 1000000000) { // 1b
	    price = price / 1000000;
	    return new Handlebars.SafeString(price.toFixed(2) + " M");
	} else if (price < 1000000000000) { // 1t
	    price = price / 1000000000;
	    return new Handlebars.SafeString(price.toFixed(3) + " B");
	}
    });
    
})();

(function(ec) {

    ec.types = {
	34 : "Tritanium",
	35 : "Pyerite",
	36 : "Mexallon",
	37 : "Isogen",
	38 : "Nocxium",
	39 : "Zydrine",
	40 : "Megacyte",
	29668 : "PLEX",
	16274 : "Helium Isotopes",
	17889 : "Hydrogen Isotopes",
	17888 : "Nitrogen Isotopes",
	17887 : "Oxygen Isotopes"
    };

    ec.systems = {
	"30000142": "Jita",
	"30002659": "Dodixie",
	"30002510": "Rens",
	"30002187": "Amarr",
	"30002053": "Hek"
    };


    ec.indexSystem = "30000142";
    ec.indexQuery = function() { 
	var types = _.keys(ec.types)
	return {"typeid" : types.join(","), "usesystem" : ec.indexSystem };  
    }
    ec.statsModel = Backbone.Model.extend({
	url:'/api/marketstat/json'
    });

    ec.statsView = Backbone.View.extend({
	initialize:function () {
            this.model.bind("change", this.render, this);
	},
	render: function (event) { 
	    var source   = $("#statsTemplate").html();
	    var template = Handlebars.compile(source);
	    this.$el.html(template(this.model.toJSON()[0]));
	    return this;
	}
    });

    ec.statsScrollView = Backbone.View.extend({
	initialize: function() {
            this.model.bind("change", this.render, this); 
	},
	render: function(event) {
	    var template = Handlebars.compile($("#statsTemplate").html());
	    var data = this.model.toJSON();

	    this.$el.empty();
	    var model = this;
	    $.each(data, function() {
		model.$el.append(template({ typeid : this.sell.forQuery.types[0], typename : ec.types[this.sell.forQuery.types[0]], values: this }));
	    });

	    return this;
	}
    });

    ec.statsNameView = Backbone.View.extend({
	initialize: function() { 
	    this.model.bind("change", this.render, this); 
	},
	render: function(event) {
	    var el = this.$el;
	    var model = this.model;
	    el.empty();

	    _.each(_.keys(ec.systems), function(system) {
		if (system == ec.indexSystem) {
		    el.append("<span class=picked>" + ec.systems[system] + "</span>");
		} else {
		    var n = $("<span class=topick>" + ec.systems[system] + "</span>");
		    var a = el.append(n);
		    n.one("click", "", system, 
			 function(t) { 
			     ec.indexSystem = t.data;
			     model.fetch({data: ec.indexQuery()});
			 }
			);

		}
	    });

	}
    });

    // ----------------------------------------
    // Page level handlers
    // ----------------------------------------

    ec.indexpage = function() {
	var stats = new ec.statsModel();
	var statsView = new ec.statsScrollView( { model : stats, el : $("#statsHolder")});
	var statsNameView = new ec.statsNameView( { model: stats, el : $("#statsName")});
	stats.fetch({data: ec.indexQuery()});
	window.setInterval(function() {
	    stats.fetch({data: ec.indexQuery()});
	}, 600000);
	
    };

    ec.quicklook = function (regionlist) {
	var stats = new ec.statsModel();
	urlParams["regionlimit"] = new Array();
	var regionstr = "";
	$.each(regionlist, function (region) {
	    regionstr = regionstr + this.toString() + ",";
	});
	urlParams["regionlimit"] = regionstr;

	this.statsView = new ec.statsView({ model: stats, el: $("#statsHolder") } );
	stats.fetch({data:urlParams});

	window.setInterval(function() {
	    stats.fetch({data: urlParams});
	}, 600000);

    };

    ec.tradetool = function() {
	var desc = Handlebars.compile('<p>{{name}} - {{type}}</p>');

	$("#system1s").typeahead({
	    name: 'locations',
	    remote: '/json_tools/system_search?name=%QUERY',
	    valueKey: 'name',
	    template: desc,
	}).bind("typeahead:selected", function(evt, item) {
	    $('#system1').val(item.id);
	});
	$("#system2s").typeahead({
	    name: 'locations',
	    remote: '/json_tools/system_search?name=%QUERY',
	    valueKey: 'id',
	    template: desc
	}).bind("typeahead:selected", function(evt, item) {
	    $('#system2').val(item.id);
	});

	

    };


})(window.ec = window.ec || {});