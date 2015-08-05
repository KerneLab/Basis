/**
 * Basic Tools Written in JavaScript.<br />
 * KerneLab.org
 */
function Tools()
{
}

Tools.add = function(vector1, vector2)
{
	return [ vector1[0] + vector2[0], vector1[1] + vector2[1] ];
};

Tools.clean = function(obj)
{
	for ( var i in obj)
	{
		if (obj.hasOwnProperty(i))
		{
			delete obj[i];
		}
	}
	return obj;
};

Tools.clone = function(source)
{
	var type = $.type(source);

	if (type != "array" && type != "object")
	{
		return source;
	}

	var target = Tools.get(arguments, 1, null);
	var keys = Tools.get(arguments, 2, Tools.keys(source));

	if (target == null)
	{
		if (type == "array")
		{
			target = [];
		}
		else
		{
			target = {};
		}
	}

	for ( var k in keys)
	{
		var key = keys[k];
		if (source.hasOwnProperty(key))
		{
			target[key] = Tools.clone(source[key]);
		}
	}

	return target;
};

Tools.dualMatch = function(str, a, b, from)
{
	var pos = null;
	if ($.type(str) == "string")
	{
		pos = -1;
		if (from == null)
		{
			from = 0;
		}
		var quotes = {};
		if ($.type(arguments[4]) == "array")
		{
			var quote = arguments[4];
			for ( var k in quote)
			{
				quotes[quote[k]] = k;
			}
		}
		var length = str.length;
		var count = 0;
		var quoting = null;
		for (var i = Math.max(from, 0); i < length; i++)
		{
			var c = str.charAt(i);
			var q = quotes[c];
			if (q != null)
			{
				if (q == quoting)
				{
					quoting = null;
					continue;
				}
				else if (quoting == null)
				{
					quoting = q;
					continue;
				}
			}
			if (quoting == null)
			{
				if (c == a)
				{
					count--;
				}
				else if (c == b)
				{
					count++;
				}
				if (count == 0)
				{
					pos = i;
					break;
				}
			}
		}
	}
	return pos;
};

Tools.dualCount = function(str, a, b, from)
{
	var count = null;
	if ($.type(str) == "string")
	{
		count = 0;
		if (from == null)
		{
			from = 0;
		}
		var quotes = {};
		if ($.type(arguments[4]) == "array")
		{
			var quote = arguments[4];
			for ( var k in quote)
			{
				quotes[quote[k]] = k;
			}
		}
		var length = str.length;
		var quoting = null;
		for (var i = Math.max(from, 0); i < length; i++)
		{
			var c = str.charAt(i);
			var q = quotes[c];
			if (q != null)
			{
				if (q == quoting)
				{
					quoting = null;
					continue;
				}
				else if (quoting == null)
				{
					quoting = q;
					continue;
				}
			}
			if (quoting == null)
			{
				if (c == a)
				{
					count--;
				}
				else if (c == b)
				{
					count++;
				}
			}
		}
	}
	return count;
};

Tools.escapeRegex = function(str)
{
	return str.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
};

Tools.escapeText = function(text)
{
	text = "" + text;
	var buffer = [];
	for (var i = 0; i < text.length; i++)
	{
		buffer.push("&#" + text.charCodeAt(i) + ";");
	}
	return buffer.join("");
};

Tools.fillTemplate = function(template, data)
{
	if (data != null)
	{
		for ( var k in data)
		{
			template = template.replace(new RegExp(Tools.escapeRegex("?" + k
					+ "?"), "g"), Tools.escapeText(Tools.nullEmpty(data[k])));
		}
	}
	return template;
};

Tools.find = function(contain, object)
{
	var index = -1;
	var equal = Tools.get(arguments, 2, function(a, b)
	{
		return a == b;
	});
	for ( var i in contain)
	{
		if (contain.hasOwnProperty(i) && equal(contain[i], object))
		{
			index = i;
			break;
		}
	}
	return index;
};

Tools.get = function(obj, key)
{
	var val = null;
	if (obj != null)
	{
		val = obj[key];
	}
	if (val == null && arguments.length > 2)
	{
		val = arguments[2];
	}
	return val;
};

Tools.keys = function(object)
{
	return Tools.reduce(object, function(res, val, key)
	{
		res.push(key);
		return res;
	}, Tools.get(arguments, 1, []));
};

Tools.map = function(contain, mapper)
{
	var result = Tools.get(arguments, 2, []);
	for ( var i in contain)
	{
		if (contain.hasOwnProperty(i))
		{
			result.push(mapper(contain[i], i));
		}
	}
	return result;
};

Tools.merge = function(into, from, equal)
{
	var value = Tools.get(arguments, 3);
	for ( var i in from)
	{
		var src = from[i];
		if (src != null)
		{
			var val = $.type(value) == "function" ? value(src) : src;
			var tar = Tools.get(into, Tools.find(into, src, equal));
			if (tar != null)
			{
				Tools.clone(val, tar);
			}
			else
			{
				into.push(val);
			}
		}
	}
	return into;
};

Tools.multiple = function(factor, vector)
{
	return [ vector[0] * factor, vector[1] * factor ];
};

Tools.negative = function(vector)
{
	return [ -vector[0], -vector[1] ];
};

Tools.nullEmpty = function(string)
{
	var def = arguments.length > 1 ? def = arguments[1] : "";
	return string == null ? def : string;
};

Tools.orth = function(vector)
{
	var orth = [ vector[1], -vector[0] ];
	return orth;
};

Tools.reduce = function(contain, reducer, result)
{
	var temp = undefined;
	for ( var i in contain)
	{
		if (contain.hasOwnProperty(i))
		{
			temp = reducer(result, contain[i], i);
			if (typeof (temp) != "undefined")
			{
				result = temp;
			}
		}
	}
	return result;
};

Tools.size = function(obj)
{
	var s = 0;
	for ( var i in obj)
	{
		if (obj.hasOwnProperty(i))
		{
			s++;
		}
	}
	return s;
};

Tools.trim = function(str)
{
	return str == null ? str : str.replace(new RegExp("^\\s+|\\s+$", "g"), "");
};

Tools.unit = function(vector)
{
	var length = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
	return [ vector[0] / length, vector[1] / length ];
};

Tools.values = function(object)
{
	return Tools.reduce(object, function(res, val)
	{
		res.push(val);
		return res;
	}, Tools.get(arguments, 1, []));
};

Tools.vector = function(from, to)
{
	return [ to[0] - from[0], to[1] - from[1] ];
};
