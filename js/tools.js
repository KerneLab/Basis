/**
 * Basic Tools Written with JavaScript. <br />
 * KerneLab.org
 */
function Tools()
{

}

Tools.Add = function(vector1, vector2)
{
	return [ vector1[0] + vector2[0], vector1[1] + vector2[1] ];
};

Tools.Cast = function(src, obj)
{
	var keys = [];
	if (arguments.length == 2)
	{
		Tools.Keys(src, keys);
	}
	else if (arguments.length > 2)
	{
		for ( var i = 2; i < arguments.length; i++)
		{
			var k = arguments[i];
			if ($.type(k) == "array")
			{
				for ( var j in k)
				{
					var key = k[j];
					if (src.hasOwnProperty(key))
					{
						keys.push(key);
					}
				}
			}
			else
			{
				if (src.hasOwnProperty(k))
				{
					keys.push(k);
				}
			}
		}
	}
	for ( var i in keys)
	{
		var key = keys[i];
		obj[key] = src[key];
	}
	return obj;
};

Tools.Clean = function(obj)
{
	for ( var i in obj)
	{
		if (obj.hasOwnProperty(i))
		{
			delete obj[i];
		}
	}
};

Tools.EscapeRegex = function(str)
{
	return str.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
};

Tools.EscapeText = function(text)
{
	text = "" + text;
	var buffer = [];
	for ( var i = 0; i < text.length; i++)
	{
		buffer.push("&#" + text.charCodeAt(i) + ";");
	}
	return buffer.join("");
};

Tools.FillTemplate = function(template, data)
{
	if (data != null)
	{
		for ( var k in data)
		{
			template = template.replace(new RegExp(Tools.EscapeRegex("?" + k + "?"), "g"), Tools.EscapeText(Tools
					.NullEmpty(data[k])));
		}
	}
	return template;
};

Tools.Find = function(contain, object)
{
	var index = -1;
	var equal = Tools.Get(arguments, 2, function(a, b)
	{
		return a == b;
	});
	for ( var i in contain)
	{
		if (contain.hasOwnProperty(i) && equal(object, contain[i]))
		{
			index = i;
			break;
		}
	}
	return index;
};

Tools.Get = function(obj, key)
{
	var val = obj[key];
	if (val == null && arguments.length > 2 && $.type(arguments[2]) == "function")
	{
		val = arguments[2](key, obj);
		obj[key] = val;
	}
	return val;
};

Tools.Keys = function(object)
{
	return Tools.Reduce(object, function(res, val, key)
	{
		res.push(key);
		return res;
	}, Tools.Get(arguments, 1, []));
};

Tools.Map = function(contain, mapper)
{
	var result = Tools.Get(arguments, 2, []);
	for ( var i in contain)
	{
		if (contain.hasOwnProperty(i))
		{
			result.push(mapper(contain[i], i));
		}
	}
	return result;
};

Tools.Multiple = function(factor, vector)
{
	return [ vector[0] * factor, vector[1] * factor ];
};

Tools.Negative = function(vector)
{
	return [ -vector[0], -vector[1] ];
};

Tools.NullEmpty = function(string)
{
	return string == null ? "" : string;
};

Tools.Orth = function(vector)
{
	var orth = [ vector[1], -vector[0] ];
	return orth;
};

Tools.Reduce = function(contain, reducer, result)
{
	for ( var i in contain)
	{
		if (contain.hasOwnProperty(i))
		{
			result = reducer(result, contain[i], i);
		}
	}
	return result;
};

Tools.Size = function(obj)
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

Tools.Unit = function(vector)
{
	var length = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
	return [ vector[0] / length, vector[1] / length ];
};

Tools.Values = function(object)
{
	return Tools.Reduce(object, function(res, val)
	{
		res.push(val);
		return res;
	}, Tools.Get(arguments, 1, []));
};

Tools.Vector = function(from, to)
{
	return [ to[0] - from[0], to[1] - from[1] ];
};
