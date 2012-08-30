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
	    return new Handlebars.SafeString(price.toFixed(2));
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
	    $("#statsHolder").empty().append(template(this.model.toJSON()[0]));
	}
    });

    ec.quicklook = function (regionlist) {
	var stats = new ec.statsModel();
	urlParams["regionlimit"] = new Array();
	var regionstr = "";
	$.each(regionlist, function (region) {
	    regionstr = regionstr + this.toString() + ",";
	});
	urlParams["regionlimit"] = regionstr;
	stats.fetch({data:urlParams});
	this.statsView = new ec.statsView({ model: stats } );
	window.setInterval(function() {
	    stats.fetch({data: urlParams});
	}, 60000);

    };


})(window.ec = window.ec || {});