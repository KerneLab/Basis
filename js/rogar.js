(function($)
{
	$.fn.rogar = function(options)
	{
		var self = this;

		var opts = $.extend({}, $.fn.rogar.defaults, options);

		var rate = opts["init.rate"];

		this.children().remove();

		this.addClass(opts["box.class"]);

		var darkText = $("<div></div>").addClass(opts["txt.dark.class"]).text(opts["txt.formatter"](rate));

		var lightText = $("<div></div>").addClass(opts["txt.light.class"]).text(opts["txt.formatter"](rate));

		var bar = $("<div></div>").addClass(opts["bar.class"]).css("width", (rate * 100) + "%");

		this.append(darkText);

		bar.append(lightText);

		this.append(bar);

		this.to = function(r)
		{
			d3.select(self.selector).selectAll("." + opts["bar.class"]).transition().duration(750).style("width",
					(r * 100) + "%");

			d3.select(self.selector).selectAll("." + opts["txt.dark.class"]).transition().duration(750).tween("text",
					function()
					{
						var i = d3.interpolate(rate, r);
						return function(t)
						{
							this.textContent = opts["txt.formatter"](i(t));
						};
					});

			d3.select(self.selector).selectAll("." + opts["txt.light.class"]).transition().duration(750).tween("text",
					function()
					{
						var i = d3.interpolate(rate, r);
						return function(t)
						{
							this.textContent = opts["txt.formatter"](i(t));
						};
					});

			return self;
		};

		return this;
	};

	$.fn.rogar.defaults = {
		"init.rate": 0,
		"box.class": "rogar-box",
		"bar.class": "rogar-bar",
		"txt.dark.class": "rogar-txt-dark",
		"txt.light.class": "rogar-txt-light",
		"txt.formatter": function(n)
		{
			return (n * 100).toFixed(2) + "%";
		}
	};

})(jQuery);