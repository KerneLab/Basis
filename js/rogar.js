/**
 * Rogar is a jQuery plugin to display a progress bar.<br />
 * D3 framework is also needed.<br />
 * The default css styles:<br />
 * .rogar-box { border: 1px solid #6666dd; float: left; height: 20px; width:
 * 200px; overflow: hidden; position: relative;}
 * 
 * .rogar-bar { background-color: #9999ee; height: 20px; overflow: hidden;
 * position: relative; text-align: center; }
 * 
 * .rogar-txt-dark,.rogar-txt-light { height: 20px; line-height: 20px; position:
 * absolute; text-align: center; width: 200px; }
 * 
 * .rogar-txt-dark { color: #333333; }
 * 
 * .rogar-txt-light { color: #ffffff; }
 */
(function($) {
	$.fn.rogar = function(options) {
		var self = this;

		var opts = $.extend({}, $.fn.rogar.defaults, options);

		var box = this;

		var rate = self.data("rogar.ratio") != null ? self.data("rogar.ratio")
				: opts["init.rate"];

		box.empty();

		if (!box.is("div")) {
			box = $("<div></div>");
			this.append(box);
		}

		box.addClass(opts["box.class"]);

		var darkText = $("<div></div>").addClass(opts["txt.dark.class"]).text(
				opts["txt.formatter"](rate));

		var lightText = $("<div></div>").addClass(opts["txt.light.class"])
				.text(opts["txt.formatter"](rate));

		var bar = $("<div></div>").addClass(opts["bar.class"]).css("width",
				(rate * 100) + "%");

		box.append(darkText);

		bar.append(lightText);

		box.append(bar);

		var findFillColor = function(rate) {
			rate *= 100;
			var map = opts["bar.fill.colors"];
			var found = null;
			var value = null;
			for ( var k in map) {
				var val = parseFloat(k);
				if (found == null) {
					found = k;
					value = val
				} else {
					if (val < rate && value < val) {
						found = k;
						value = val;
					}
				}
			}
			return found == null ? null : map[found];
		};

		this.to = function(r) {
			var rat = rate;
			var duration = Math.abs(r - rat) * opts["duration.total"];
			var node = self.get(0);
			d3.select(node).selectAll("." + opts["bar.class"]).transition()
					.duration(duration).tween("width", function() {
						var i = d3.interpolate(rat, r);
						return function(t) {
							var temp = i(t);
							rate = temp;
							bar.css({
								"width" : (temp * 100).toFixed(2) + "%",
								"background-color" : findFillColor(temp)
							});
						}
					});

			d3.select(node).selectAll("." + opts["txt.dark.class"])
					.transition().duration(duration).tween("text", function() {
						var i = d3.interpolate(rat, r);
						return function(t) {
							this.textContent = opts["txt.formatter"](i(t));
						};
					});

			d3.select(node).selectAll("." + opts["txt.light.class"])
					.transition().duration(duration).tween("text", function() {
						var i = d3.interpolate(rat, r);
						return function(t) {
							this.textContent = opts["txt.formatter"](i(t));
						};
					});

			self.data("rogar.ratio", r);

			return self;
		};

		return this;
	};

	$.fn.rogar.defaults = {
		"init.rate" : 0,
		"duration.total" : 2000,
		"box.class" : "rogar-box",
		"bar.class" : "rogar-bar",
		"txt.dark.class" : "rogar-txt-dark",
		"txt.light.class" : "rogar-txt-light",
		"txt.formatter" : function(n) {
			return (n * 100).toFixed(2) + "%";
		},
		"bar.fill.colors" : {
			"0" : "#1A0FE3",
			"30" : "#1ED51E",
			"65" : "#F9A800",
			"85" : "#EE2D2D"
		}
	};

})(jQuery);