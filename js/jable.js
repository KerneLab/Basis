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

		this.renderData = function(jsan, index, templateBody) {
			if (index >= 0 && index < jsan.length) {
				if (templateBody == null) {
					var columns = Tools.keys(jsan[index]);
					var buffer = [ "<tr>" ];
					for ( var i = 0; i < columns.length; i++) {
						buffer.push("<td>");
						buffer.push("?" + columns[i] + "?");
						buffer.push("</td>");
					}
					buffer.push("</tr>");
					templateBody = buffer.join("");
				}
				self.append(Tools.fillTemplate(templateBody, jsan[index]));
				row = self.find("tr:last").hide().data(jsan[index]);
				if ($.type(opts.onRenderRow) == "function") {
					opts.onRenderRow(self.find("tr:last").hide(), index + 1);
					opts.onShowingRow(row, index + 1, function() {
						self.renderData(jsan, index + 1, templateBody);
					});
				}
			}
		};

		this.refresh = function() {
			var url = opts.url;
			var param = self.param();
			var onRefreshed = opts.onRefreshed;
			var templateHead = opts.templateHead;
			var templateBody = opts.templateBody;

			for ( var i in arguments) {
				var arg = arguments[i];
				switch ($.type(arg)) {
				case "string":
					url = arg;
					break;
				case "object":
					param = arg;
					break;
				case "function":
					onRefreshed = arg;
					break;
				}
			}

			$.post(url, param, function(jsan) {
				self.empty();

				if (templateHead == null && jsan.length > 0) {
					var columns = Tools.keys(jsan[0]);
					var buffer = [ "<tr>" ];
					for ( var i = 0; i < columns.length; i++) {
						buffer.push("<th>");
						buffer.push(Tools.escapeText(columns[i]));
						buffer.push("</th>");
					}
					buffer.push("</tr>");
					templateHead = buffer.join("");
				}

				templateHead = templateHead == null ? "" : templateHead;

				self.append(templateHead);

				var row = self.find("tr:last").hide();
				if ($.type(opts.onRenderRow) == "function") {
					opts.onRenderRow(row, 0);
				}
				opts.onShowingRow(row, 0, function() {
					self.renderData(jsan, 0, templateBody);
				});

				if ($.type(onRefreshed) == "function") {
					onRefreshed(jsan);
				}

			}, "json");
		};

		return this;
	};

	$.fn.jable.defaults = {
		"templateHead" : null,
		"templateBody" : null,
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