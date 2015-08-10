/**
 * jQuery extension for dragging.<br />
 * Kernelab.org
 */
(function($)
{
	$.fn.dragger = function(options)
	{
		var self = this;

		var opts = $.extend({}, $.fn.dragger.defaults, options);

		var handler = opts["handler"] == null ? self : opts["handler"];

		var dragging = false;
		var srcX = null, srcY = null;
		var draggin = opts["draggin"], dragged = opts["dragged"];

		handler.mousedown(function(e)
		{
			dragging = true;
			srcX = e.pageX - parseInt(self.css("left"));
			srcY = e.pageY - parseInt(self.css("top"));
			if ($.type(draggin) == "function")
			{
				draggin(self, handler);
			}
		}).mousemove(function(e)
		{
			if (dragging)
			{
				var x = e.pageX - srcX;
				var y = e.pageY - srcY;
				self.css({
					"left": x,
					"top": y
				});
			}
		}).mouseup(function(e)
		{
			dragging = false;
			srcX = null;
			srcY = null;
			if ($.type(dragged) == "function")
			{
				dragged(self, handler);
			}
		});
	};

	$.fn.dragger.defaults = {
		"handler": null,
		"draggin": function(self, handler)
		{
			handler.css("cursor", "move");
			self.fadeTo(200, 0.85);
		},
		"dragged": function(self, handler)
		{
			handler.css("cursor", "default");
			self.fadeTo(200, 1);
		}
	};

})(jQuery);