/**
 * jQuery extension for JSAN table.<br />
 * Based on Tools.js<br />
 * Kernelab.org
 */
(function($) {
	$.fn.jable = function(options) {
		var self = this;

		var opts = $.extend({}, $.fn.jable.defaults, options);

		this.param = function() {
			switch (arguments.length) {
			case 0:
				return opts.param;
				break;
			case 1:
				switch ($.type(arguments[0])) {
				case "string":
					return opts.param[arguments[0]];
					break;
				case "object":
					opts.param = arguments[0];
					return self;
					break;
				}
				break;
			case 2:
				opts.param[arguments[0]] = arguments[1];
				return self;
				break;
			}
		};

		this.renderData = function(jsan, index) {
			if (index >= 0 && index < jsan.length) {
				self.append(Tools.FillTemplate(opts.templateBody, jsan[index]));
				row = self.find("tr:last").hide().data(jsan[index]);
				if ($.type(opts.onRenderRow) == "function") {
					opts.onRenderRow(self.find("tr:last").hide(), index + 1);
					opts.onShowingRow(row, index + 1, function() {
						self.renderData(jsan, index + 1);
					});
				}
			} else {
				if ($.type(opts.onRefreshed) == "function") {
					opts.onRefreshed(jsan);
				}
			}
		};

		this.refresh = function() {
			for ( var i in arguments) {
				var arg = arguments[i];
				switch ($.type(arg)) {
				case "string":
					opts.url = arg;
					break;
				case "object":
					self.param(arg);
					break;
				case "function":
					opts.onRefreshed = arg;
					break;
				}
			}

			$.post(opts.url, self.param(), function(jsan) {
				self.empty();

				self.append(opts.templateHead);
				var row = self.find("tr:last").hide();
				if ($.type(opts.onRenderRow) == "function") {
					opts.onRenderRow(row, 0);
				}
				opts.onShowingRow(row, 0, function() {
					self.renderData(jsan, 0);
				});

			}, "json");
		};

		return this;
	};

	$.fn.jable.defaults = {
		"templateHead" : "",
		"templateBody" : "",
		"url" : "",
		"param" : {},
		"onRefreshed" : function(jsan) {
		},
		"onRenderRow" : function(row, index) {
		},
		"onShowingRow" : function(row, index, next) {
			row.show(next);
		}
	};

})(jQuery);