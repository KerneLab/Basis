/**
 * Segar is a jQuery plugin to display a progress bar with serial segments.<br />
 * D3 framework is also needed.<br />
 * The default css styles:<br />
 * .segar-box { border: 1px solid #6666dd; float: left; height: 20px; width:
 * 200px; overflow: hidden; position: relative; float: left; }
 * 
 * .segar-bar { background-color: #9999ee; float: left; height: 20px; overflow:
 * hidden; position: relative; text-align: center; }
 * 
 * .segar-txt-hint,.segar-txt-light { height: 20px; line-height: 20px; position:
 * absolute; text-align: center; width: 200px; }
 * 
 * .segar-txt-hint { float: left; position: absolute; z-index: 100; top: 0px;
 * left: 0px; }
 * 
 * .segar-bar { float: left; }
 * 
 * .segar-txt-hint { color: #333333; }
 * 
 * .segar-txt-light { color: #ffffff; }
 */
(function($)
{
	$.fn.segar = function(options)
	{
		var self = this;

		var opts = $.extend({}, $.fn.segar.defaults, options);

		var box = this;

		this.rates = self.data("segar.ratio") != null ? self.data("segar.ratio") : opts["init.rates"];

		this.sum = function()
		{
			var data = Tools.get(arguments, 0, self.rates);
			var sum = 0;
			for ( var i in data)
			{
				sum += data[i]["value"];
			}
			return sum;
		};

		var rate = self.sum(self.rates);

		box.empty();

		if (!box.is("div"))
		{
			box = $("<div></div>");
			this.append(box);
		}

		box.addClass(opts["box.class"]);

		var darkText = $("<div></div>").addClass(opts["txt.hint.class"]).text(opts["txt.formatter"](rate));
		box.append(darkText);

		this.bars = d3.select(self.get(0)).selectAll("div." + opts["bar.class"]) //
		.data(self.rates, function(d, i)
		{
			return d["id"] == null ? i : d["id"];
		}).enter().append("div") //
		.classed(opts["bar.class"], true) //
		.style("background-color", function(d, i)
		{
			return d["color"] == null ? null : d["color"];
		})//
		.attr("title", function(d, i)
		{
			var r = opts["txt.formatter"](d["value"]);
			if (d["id"] != null)
			{
				r = d["id"] + "(" + r + ")";
			}
			return r;
		}) //
		.text(function(d, i)
		{
			return opts["txt.formatter"](d["value"]);
		}) //
		.style("width", function(d, i)
		{
			return (d["value"] * 100) + "%";
		});

		this.to = function(rates)
		{
			if ($.type(rates) == "number")
			{
				rates = [ {
					"value": rates
				} ];
			}

			var rat = rate;
			var node = self.get(0);
			rate = self.sum(rates);
			var duration = Math.abs(rate - rat) * opts["duration.total"];

			d3.select(self.get(0)).selectAll("div." + opts["bar.class"]) //
			.data(rates, function(d, i)
			{
				return d["id"] == null ? i : d["id"];
			}) //
			.style("background-color", function(d, i)
			{
				return d["color"] == null ? null : d["color"];
			}) //
			.attr("title", function(d, i)
			{
				var r = opts["txt.formatter"](d["value"]);
				if (d["id"] != null)
				{
					r = d["id"] + "(" + r + ")";
				}
				return r;
			}) //
			.transition().duration(duration) //
			.tween("text", function(d, i, a)
			{
				var s = a == null || a == "" ? "0%" : a;
				var p = d3.interpolate(parseFloat(s.substr(0, s.length - 1)) / 100.0, d["value"]);

				return function(t)
				{
					this.textContent = opts["txt.formatter"](p(t));
				};
			}) //
			.styleTween("width", function(d, i, a)
			{
				var p = d3.interpolate(parseFloat(a.substr(0, a.length - 2)) / self.width(), d["value"]);

				return function(t)
				{
					return (p(t) * 100).toFixed(2) + "%"
				}
			});

			d3.select(node).selectAll("." + opts["txt.hint.class"]).transition().duration(duration).tween("text",
					function()
					{
						var p = d3.interpolate(rat, rate);
						return function(t)
						{
							this.textContent = opts["txt.formatter"](p(t));
						};
					});

			self.data("segar.ratio", rates);

			self.rates = rates;

			return self;
		};

		return this;
	};

	$.fn.segar.defaults = {
		"init.rates": [ {
			"value": 0
		} ],
		"duration.total": 2000,
		"box.class": "segar-box",
		"bar.class": "segar-bar",
		"txt.hint.class": "segar-txt-hint",
		"txt.formatter": function(n)
		{
			return (n * 100).toFixed(0) + "%";
		}
	};

})(jQuery);