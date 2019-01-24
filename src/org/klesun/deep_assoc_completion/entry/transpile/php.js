
/**
 * this file provides implementations for built-in
 * php functions that work more or less same way
 * (be careful with the functions that write to a passed vars, like preg_match)
 */

let util = require('util');

let empty = (value) => !value || +value === 0 ||
	(typeof value === 'object') && Object.keys(value).length === 0;

let strval = (value) => value === null || value === false || value === undefined ? '' : value + '';

let STR_PAD_LEFT = 0;
let STR_PAD_RIGHT = 1;

exports.empty = empty;
exports.intval = (value) => +value;
exports.boolval = (value) => empty(value) ? true : false;
exports.trim = (value) => strval(value).trim();
exports.strval = strval;
exports.strtoupper = (value) => strval(value).toUpperCase();
exports.substr = (str, from, length) => str.slice(from, from + length);
exports.str_pad = ($input, $pad_length, $pad_string = " ", $pad_type = STR_PAD_RIGHT) => {
	if ($pad_type == STR_PAD_RIGHT) {
		return strval($input).padEnd($pad_length, $pad_string);
	} else if ($pad_type == STR_PAD_LEFT) {
		return strval($input).padStart($pad_length, $pad_string);
	} else {
		throw new Error('Unsupported padding type - ' + $pad_type);
	}
};

exports.isset = (value) => value !== null && value !== undefined;
exports.strtotime = (dtStr) => {
	if (dtStr === 'now') {
		return Date.now() / 1000;
	} else if (dtStr.match(/^\d{4}-\d{2}-\d{2}(\d{2}:\d{2}:\d{2})?$/)) {
		return Date.parse(dtStr) / 1000;
	} else {
		throw new Error('Unsupported date str format - ' + dtStr);
	}
};
exports.date = (format, epoch) => {
	let dtObj;
	if (epoch === undefined) {
		dtObj = new Date();
	} else {
		dtObj = new Date(1970, 0, 1); // Epoch
		dtObj.setSeconds(epoch);
	}
	if (format === 'Y-m-d H:i:s') {
		return dtObj.toISOString().slice(0, '2018-12-05T22:13:41'.length).replace('T', ' ');
	} else if (format === 'Y-m-d') {
		return dtObj.toISOString().slice(0, '2018-12-05'.length);
	} else if (format === 'm-d') {
		return dtObj.toISOString().slice('2018-'.length, '2018-12-05'.length);
	} else if (format === 'H:i') {
		return dtObj.toISOString().slice('2018-12-05T'.length, '2018-12-05T22:13'.length);
	} else {
		throw new Error('Unsupported date format - ' + format);
	}
};

exports.array_keys = (obj) => Object.keys(obj);
exports.array_values = (obj) => Object.values(obj);
exports.in_array = (value, arr) => Object.values(arr).indexOf(value) > -1;
/** @param {Array} arr */
exports.array_shift = (arr) => arr.shift();
/** @param {Array} arr */
exports.array_unshift = (arr, value) => arr.unshift(value);
exports.implode = (delim, values) => values.join(delim);
exports.explode = (delim, str) => str.split(delim);

exports.json_encode = (str) => JSON.stringify(str);
exports.json_decode = (str) => str ? JSON.parse(str) : null;
exports.ucfirst = str => str.slice(0, 1).toUpperCase() + str.slice(1);
exports.ltrim = str => str.replace(/^\s+/, '');
exports.rtrim = str => str.replace(/\s+$/, '');
exports.strlen = str => (str + "").length;
exports.array_key_exists = (key, obj) => key in obj;
exports.substr_replace = (str, replace, start, length = null) => {
	if (length === null) {
		length = str.length;
	}
	let end = length >= 0 ? start + length : length;
	return str.slice(0, start) + replace + str.slice(end);
};

exports.strcasecmp = (a, b) =>
	a.toLowerCase() > b.toLowerCase() ? 1 :
	a.toLowerCase() < b.toLowerCase() ? -1 : 0;

exports.sprintf = (template, ...values) => util.format(template, ...values);
exports.str_replace = (search, replace, str) => str.replace(search, replace);
exports.preg_replace = (pattern, replace, str) => {
	let reg = new RegExp(pattern);
	if (!reg.flags.includes('g')) {
		reg = new RegExp(reg.source, reg.flags + 'g');
	}
	return str.replace(reg, replace);
};
exports.preg_replace_callback = (pattern, callback, str) => {
	let reg = new RegExp(pattern);
	if (!reg.flags.includes('g')) {
		reg = new RegExp(reg.source, reg.flags + 'g');
	}
	return str.replace(reg, (args) => {
		let fullStr = args.pop();
		let offset = args.pop();
		let matches = args;
		return callback(matches);
	});
};
exports.preg_match = (pattern, str, matches = null, phpFlags = null) => {
	if (phpFlags) {
		throw new Error('Fourth preg_match argument, php flags, is not supported - ' + phpFlags);
	} else {
		let matches = str.match(pattern);
		if (matches) {
			Object.assign(matches, matches.groups);
		}
		return matches;
	}
};
// exports.preg_match_all = (pattern, str, dest, bitMask) => {
// 	let inSaneFormat = matchAll(pattern, str);
// };
exports.array_merge = (...arrays) => {
	let result = arrays.every(arr => Array.isArray(arr)) ? [] : {};
	for (let arr of arrays) {
		if (Array.isArray(arr)) {
			result.push(...arr);
		} else {
			for (let [k,v] of Object.entries(arr)) {
				result[k] = v;
			}
		}
	}
	return result;
};
exports.array_intersect_key = (source, whitelist) => {
	let newObj = {};
	for (let [key, val] of Object.entries(source)) {
		if (key in whitelist) {
			newObj[key] = val;
		}
	}
	return newObj;
};
exports.array_flip = (obj) => {
	let newObj = {};
	for (let [key, val] of Object.entries(obj)) {
		newObj[val] = key;
	}
	return newObj;
};
exports.array_map = (func, obj, additionalValues = []) => {
	let newObj = Array.isArray(obj) ? [] : {};
	for (let [key, val] of Object.entries(obj)) {
		newObj[key] = func(val, additionalValues[key]);
	}
	return newObj;
};
exports.array_filter = (obj, func, flags = null) => {
	if (flags) {
		throw new Error('array_filter php flags are not supported');
	}
	let newObj = Array.isArray(obj) ? [] : {};
	for (let [key, val] of Object.entries(obj)) {
		if (func(val)) {
			newObj[key] = val;
		}
	}
	return newObj;
};
exports.str_split = (str, size = 1) => {
	if (size < 1) {
		throw new Error('Invalid chunk size - ' + size + ', it must be >= 1');
	}
	let chunks = [];
	for (let i = 0; i < str.length; i += size) {
		chunks.push(str.slice(i, size));
	}
	return chunks;
};

//exports.PREG_OFFSET_CAPTURE = 256;

exports.PHP_EOL = '\n';
exports.STR_PAD_LEFT = STR_PAD_LEFT;
exports.STR_PAD_RIGHT = STR_PAD_RIGHT;
