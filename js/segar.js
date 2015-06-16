/**
 * Segar is a jQuery plugin to display a progress bar with serial segments.<br />
 * D3 framework is also needed.<br />
 * The default css styles:<br />
 * .segar-box { border: 1px solid #999999; float: left; height: 20px; width:
 * 200px; overflow: hidden; position: relative; }
 * 
 * .segar-bar { background-color: #9999ee; color: #FFFFFF; float: left; height:
 * 20px; line-height: 20px; overflow: hidden; position: relative; text-align:
 * center; }
 */
(function($)
{
	$.fn.segar = function(options)
	{
		var self = this;

		var opts = $.extend({}, $.fn.segar.defaults, options);

		var box = this;

		this.rates = self.data("segar.ratio") != null ? self
				.data("segar.ratio") : opts["init.rates"];

		this.trace = [];

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

		this.traceRates = function(rates)
		{
			self.trace = [];
			for ( var i in rates)
			{
				self.trace.push(rates[i]);
			}
		};

		box.empty();

		if (!box.is("div"))
		{
			box = $("<div></div>");
			this.append(box);
		}

		box.addClass(opts["box.class"]);

		this.bars = d3.select(self.get(0))
				.selectAll("div." + opts["bar.class"]) //
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

			self.traceRates(self.rates);

			var rat = rate;
			var node = self.get(0);
			rate = self.sum(rates);
			var duration = Math.max(Math.abs(rate - rat)
					* opts["duration.total"], opts["duration.least"]);

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
			.tween("text", function(d, i)
			{
				var a = self.trace[i];
				var p = d3.interpolate(a == null ? 0 : a["value"], d["value"]);

				return function(t)
				{
					this.textContent = opts["txt.formatter"](p(t));
				};
			}) //
			.styleTween(
					"width",
					function(d, i, a)
					{
						var p = d3.interpolate(parseFloat(a.substr(0,
								a.length - 2))
								/ self.width(), d["value"]);

						return function(t)
						{
							return (p(t) * 100).toFixed(2) + "%";
						}
					});

			self.data("segar.ratio", rates);

			self.rates = rates;

			return self;
		};

		self.data("segar.ratio", self.rates);

		return this;
	};

	$.fn.segar.defaults = {
		"init.rates": [ {
			"value": 0
		} ],
		"duration.total": 2000,
		"duration.least": 500,
		"box.class": "segar-box",
		"bar.class": "segar-bar",
		"txt.formatter": function(n)
		{
			return (n * 100).toFixed(0) + "%";
		}
	};

})(jQuery);