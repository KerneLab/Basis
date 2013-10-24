/**
 * Basic Tools Written with JavaScript. <br />
 * KerneLab.org
 */
function Tools() {

}

Tools.add = function(vector1, vector2) {
	return [ vector1[0] + vector2[0], vector1[1] + vector2[1] ];
};

Tools.cast = function(src, obj) {
	var keys = [];
	if (arguments.length == 2) {
		Tools.keys(src, keys);
	} else if (arguments.length > 2) {
		for ( var i = 2; i < arguments.length; i++) {
			var k = arguments[i];
			if ($.type(k) == "array") {
				for ( var j in k) {
					var key = k[j];
					if (src.hasOwnProperty(key)) {
						keys.push(key);
					}
				}
			} else {
				if (src.hasOwnProperty(k)) {
					keys.push(k);
				}
			}
		}
	}
	for ( var i in keys) {
		var key = keys[i];
		obj[key] = src[key];
	}
	return obj;
};

Tools.clean = function(obj) {
	for ( var i in obj) {
		if (obj.hasOwnProperty(i)) {
			delete obj[i];
		}
	}
};

Tools.escapeRegex = function(str) {
	return str.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
};

Tools.escapeText = function(text) {
	text = "" + text;
	var buffer = [];
	for ( var i = 0; i < text.length; i++) {
		buffer.push("&#" + text.charCodeAt(i) + ";");
	}
	return buffer.join("");
};

Tools.fillTemplate = function(template, data) {
	if (data != null) {
		for ( var k in data) {
			template = template.replace(new RegExp(Tools.escapeRegex("?" + k
					+ "?"), "g"), Tools.escapeText(Tools.nullEmpty(data[k])));
		}
	}
	return template;
};

Tools.find = function(contain, object) {
	var index = -1;
	var equal = Tools.get(arguments, 2, function(a, b) {
		return a == b;
	});
	for ( var i in contain) {
		if (contain.hasOwnProperty(i) && equal(object, contain[i])) {
			index = i;
			break;
		}
	}
	return index;
};

Tools.get = function(obj, key) {
	var val = obj[key];
	if (val == null && arguments.length > 2) {
		if ($.type(arguments[2]) == "function") {
			val = arguments[2](key, obj);
		} else {
			val = arguments[2];
		}
		obj[key] = val;
	}
	return val;
};

Tools.keys = function(object) {
	return Tools.reduce(object, function(res, val, key) {
		res.push(key);
		return res;
	}, Tools.get(arguments, 1, []));
};

Tools.map = function(contain, mapper) {
	var result = Tools.get(arguments, 2, []);
	for ( var i in contain) {
		if (contain.hasOwnProperty(i)) {
			result.push(mapper(contain[i], i));
		}
	}
	return result;
};

Tools.multiple = function(factor, vector) {
	return [ vector[0] * factor, vector[1] * factor ];
};

Tools.negative = function(vector) {
	return [ -vector[0], -vector[1] ];
};

Tools.nullEmpty = function(string) {
	var def = arguments.length > 1 ? def = arguments[1] : "";
	return string == null ? def : string;
};

Tools.orth = function(vector) {
	var orth = [ vector[1], -vector[0] ];
	return orth;
};

Tools.reduce = function(contain, reducer, result) {
	for ( var i in contain) {
		if (contain.hasOwnProperty(i)) {
			result = reducer(result, contain[i], i);
		}
	}
	return result;
};

Tools.size = function(obj) {
	var s = 0;
	for ( var i in obj) {
		if (obj.hasOwnProperty(i)) {
			s++;
		}
	}
	return s;
};

Tools.unit = function(vector) {
	var length = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]);
	return [ vector[0] / length, vector[1] / length ];
};

Tools.values = function(object) {
	return Tools.reduce(object, function(res, val) {
		res.push(val);
		return res;
	}, Tools.get(arguments, 1, []));
};

Tools.vector = function(from, to) {
	return [ to[0] - from[0], to[1] - from[1] ];
};
