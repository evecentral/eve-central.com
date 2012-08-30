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


(function(ec) {

    ec.statsModel = Backbone.Model.extend({
	url:'/api/marketstat/json'
    });

    ec.statsView = Backbone.View.extend({
	render: function (event) { return this; }
    });

    ec.quicklook = function () {
	console.log("Building quicklook");
	console.log("Stats" + urlParams);
	window.stats = new ec.statsModel().fetch({data:urlParams});


    };

    ec.AppRouter = Backbone.Router.extend({
	routes : { 
	    "" : "home",
	    "home/quicklook.html" : "quicklook",

	},
	home: function() { console.log("Home" + urlParams); },
	initialize : function() { console.log("Init"); },
	quicklook : function (params) {
	}
    });
    //ec.app = new ec.AppRouter();
    //Backbone.history.start();

})(window.ec = window.ec || {});