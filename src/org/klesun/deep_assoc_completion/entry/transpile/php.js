
/**
 * this file provides implementations for built-in
 * php functions that work more or less same way
 * (be careful with the functions that write to a passed
 * var, like preg_match, they will 100% work differently)
 *
 * keep in mind, there may be bugs in some of these
 * functions, I did not test the whole code much
 */

let util = require('util');
let {safe} = require('../Utils/Misc.js');

let php = {};

let empty = (value) =>
	!value || (
	(typeof value === 'object')
		? Object.keys(value).length === 0
		: +value === 0);

let strval = (value) => value === null || value === false || value === undefined ? '' : value + '';

php.STR_PAD_LEFT = 0;
php.STR_PAD_RIGHT = 1;
php.PREG_PATTERN_ORDER = 1;
php.PREG_SET_ORDER = 2;

php.empty = empty;
php.get_class = (value) => value ? (value.constructor || {}).name || null : null;
php.is_null = (value) => value === null || value === undefined;
php.is_string = (value) => typeof value === 'string';
php.intval = (value) => +value;
php.boolval = (value) => empty(value) ? true : false;
php.abs = (value) => Math.abs(value);
php.isset = (value) => value !== null && value !== undefined;
php.is_array = val => Array.isArray(val) || isPlainObject(val);
php.is_integer = str => {
	let n = Math.floor(Number(str));
	return n !== Infinity && String(n) === str;
};
// partial implementation
let equals = (a, b, strict) => {
	let occurrences = new Set();
	let equalsImpl = (a, b) => {
		if (strict && a === b) {
			return true;
		} else if (!strict && a == b) {
			// there are probably some discrepancies
			// between js == and php == - should check one day
			return true;
		} else if (occurrences.has(a) || occurrences.has(b)) {
			// circular reference, it probably could happen in js
			return false;
		} else if (Array.isArray(a) && Array.isArray(b)) {
			if (a.length !== b.length) {
				return false;
			} else {
				occurrences.add(a);
				occurrences.add(b);
				for (let i = 0; i < a.length; ++i) {
					if (!equalsImpl(a[i], b[i])) {
						return false;
					}
				}
				return true;
			}
		} else {
			return false;
		}
	};
	return equalsImpl(a, b);
};
php.equals = equals;
php.is_numeric = str => +str + '' === str;
php.floor = (num) => Math.floor(num);
php.max = (...args) => {
	if (args.length === 1 && Array.isArray(args[0])) {
		return Math.max(...args[0]);
	} else {
		return Math.max(...args);
	}
};
php.min = (...args) => {
	if (args.length === 1 && Array.isArray(args[0])) {
		return Math.min(...args[0]);
	} else {
		return Math.min(...args);
	}
};

php.call_user_func = (func, arg) => normFunc(func)(arg);
php.json_encode = (str) => JSON.stringify(str);
php.json_decode = (str) => str ? JSON.parse(str) : null;

// --------------------------------------
//  datetime functions follow
// --------------------------------------

php.strtotime = (dtStr) => {
	if (dtStr === 'now') {
		return Date.now() / 1000;
	} else if (dtStr.match(/^\d{4}-\d{2}-\d{2}( \d{2}:\d{2}:\d{2})?$/)) {
		return Date.parse(dtStr + ' Z') / 1000;
	} else if (dtStr.match(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d*|)Z$/)) {
		return Date.parse(dtStr) / 1000;
	} else if (dtStr.match(/^\d{2}:\d{2} [AP]M$/)) {
		return Date.parse('2016-01-01 ' + dtStr + ' Z') / 1000;
	} else {
		throw new Error('Unsupported date str format - ' + JSON.stringify(dtStr));
	}
};
php.date = (format, epoch) => {
	let dtObj;
	if (epoch === undefined) {
		dtObj = new Date();
	} else {
		dtObj = new Date(epoch * 1000);
	}
	if (format === 'Y-m-d H:i:s') {
		return safe(() => dtObj.toISOString().slice(0, '2018-12-05T22:13:41'.length).replace('T', ' '));
	} else if (format === 'Y-m-d') {
		return safe(() => dtObj.toISOString().slice(0, '2018-12-05'.length));
	} else if (format === 'm-d') {
		return safe(() => dtObj.toISOString().slice('2018-'.length, '2018-12-05'.length));
	} else if (format === 'H:i') {
		return safe(() => dtObj.toISOString().slice('2018-12-05T'.length, '2018-12-05T22:13'.length));
	} else if (format === 'y') {
		return safe(() => dtObj.toISOString().slice('20'.length, '2018'.length));
	} else if (format === 'my') {
		return safe(() => dtObj.toISOString().slice('2018-'.length, '2018-12'.length)
						+ dtObj.toISOString().slice('20'.length, '2018'.length));
	} else if (format === 'Y') {
		return safe(() => dtObj.toISOString().slice(0, '2018'.length));
	} else if (format === 'dM') {
		return safe(() => {
        let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
		return ('00' + dtObj.getUTCDate()).slice(-2) + months[dtObj.getUTCMonth()];
		});
	} else {
		throw new Error('Unsupported date format - ' + format);
	}
};

// ------------------
//  string functions follow
// ------------------

php.ltrim = (str, chars = ' \n\t') => {
	str = strval(str);
	let startAt = 0;
	for (let i = startAt; i <= str.length; ++i) {
		if (chars.includes(str[i])) {
			startAt = i + 1;
		} else {
			break;
		}
	}
	return str.slice(startAt);
	//return str.replace(/^\s+/, '');
};
php.rtrim = (str, chars = ' \n\t') => {
	str = strval(str);
	let endAt = str.length;
	for (let i = endAt; i > 0; --i) {
		if (chars.includes(str[i - 1])) {
			endAt = i - 1;
		} else {
			break;
		}
	}
	return str.slice(0, endAt);
	//return str.replace(/\s+$/, '');
};
php.trim = (value, chars = ' \n\t') => php.ltrim(php.rtrim(value, chars), chars);
php.strval = strval;
php.floatval = num => +num;
php.strtoupper = (value) => strval(value).toUpperCase();
php.strtolower = (value) => strval(value).toLowerCase();
php.substr = (str, from, length) => strval(str).slice(from, length !== undefined ? from + length : undefined);
php.mb_substr = php.substr; // simple substr() behaves a bit differently with unicode, but nah
php.str_pad = ($input, $pad_length, $pad_string = " ", $pad_type = php.STR_PAD_RIGHT) => {
	if ($pad_type == php.STR_PAD_RIGHT) {
		return strval($input).padEnd($pad_length, $pad_string);
	} else if ($pad_type == php.STR_PAD_LEFT) {
		return strval($input).padStart($pad_length, $pad_string);
	} else {
		throw new Error('Unsupported padding type - ' + $pad_type);
	}
};
php.str_repeat = (str, n) => strval(str).repeat(n);

php.implode = (delim, values) => values.join(delim);
php.explode = (delim, str) => strval(str).split(delim);

php.ucfirst = str => str.slice(0, 1).toUpperCase() + str.slice(1);
php.strlen = str => (str + "").length;
php.mb_strlen = php.strlen; // mb_*() behaves a bit differently with unicode, but nah

php.substr_replace = (str, replace, start, length = null) => {
	if (length === null) {
		length = str.length;
	}
	let end = length >= 0 ? start + length : length;
	return str.slice(0, start) + replace + str.slice(end);
};

php.strcasecmp = (a, b) =>
	a.toLowerCase() > b.toLowerCase() ? 1 :
	a.toLowerCase() < b.toLowerCase() ? -1 : 0;

/** be careful, it does not support '%.02f' format */
php.sprintf = (template, ...values) => util.format(template, ...values);
php.strpos = (str, substr) => {
	let index = str.indexOf(substr);
	return index > -1 ? index : false;
};
let escapeRegex = (str) => str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
php.str_replace = (search, replace, str) => str.replace(new RegExp(escapeRegex(search), 'g'), replace);
php.str_split = (str, size = 1) => {
	if (size < 1) {
		throw new Error('Invalid chunk size - ' + size + ', it must be >= 1');
	}
	let chunks = [];
	for (let i = 0; i < str.length; i += size) {
		chunks.push(str.slice(i, i + size));
	}
	return chunks;
};

// --------------------------
//  preg_* functions follow
// --------------------------

let normReg = (pattern) => {
	if (typeof pattern === 'string') {
		let match = pattern.match(/^\/(.*)\/([a-z]*)$/);
		if (match) {
			let [_, content, flags] = match;
			// php takes content and flags in one string,
			// but js takes them as separate arguments
			pattern = new RegExp(content, flags);
		}
	}
	return pattern;
};
php.PREG_SPLIT_NO_EMPTY = 1;
php.PREG_SPLIT_DELIM_CAPTURE = 2;
php.PREG_SPLIT_OFFSET_CAPTURE = 2;
php.preg_split = (regex, str, limit = -1, flags = 0) => {
	if (limit !== -1) {
		throw new Error('Unsupported preg_split parameter - limit ' + limit);
	} else if (flags != php.PREG_SPLIT_DELIM_CAPTURE) {
		// Because in js str.split(...) always includes captures. I guess I could implement a
		// workaround here, but I'm too lazy, - it's easier to just change (...) to (?:...) everywhere
		throw new Error('preg_split is only supported with PREG_SPLIT_DELIM_CAPTURE flag');
	}
	return str.split(regex);
};
php.preg_replace = (pattern, replace, str) => {
	let reg = normReg(pattern);
	if (!reg.flags.includes('g')) {
		reg = new RegExp(reg.source, reg.flags + 'g');
	}
	return str.replace(reg, replace);
};
php.preg_replace_callback = (pattern, callback, str) => {
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
let normMatch = match => {
	if (match) {
		Object.assign(match, match.groups);
	}
	return match;
};
php.preg_match = (pattern, str, dest = [], phpFlags = null) => {
	pattern = normReg(pattern);
	str = strval(str);
	if (phpFlags) {
		throw new Error('Fourth preg_match argument, php flags, is not supported - ' + phpFlags);
	} else {
		let matches = normMatch(str.match(pattern));
		if (matches) {
			Object.assign(dest, matches);
		}
		delete(dest.groups);
		return matches;
	}
};
php.preg_match_all = (pattern, str, dest, bitMask) => {
	let regex = new RegExp(normReg(pattern));
	if (regex.flags.indexOf('g') < 0) {
		regex = new RegExp(regex.source, regex.flags + 'g');
	}
	let inSetOrder = [];
	let match;
	while ((match = regex.exec(str)) !== null) {
		inSetOrder.push(normMatch(match));
	}
	if (inSetOrder.length === 0) {
		return null;
	} else if (bitMask & php.PREG_SET_ORDER) {
		Object.assign(dest, inSetOrder);
		return inSetOrder;
	} else {
		let result = {};
		for (let match of inSetOrder) {
			for (let [name, value] of Object.entries(match)) {
				result[name] = result[name] || [];
				result[name].push(value);
			}
		}
		Object.assign(dest, result);
		return result;
	}
};

// ----------------------
//  array functions follow
// ----------------------

php.array_keys = (obj) => Object.keys(obj);
php.array_values = (obj) => Object.values(obj);
php.in_array = (value, arr) => Object.values(arr).indexOf(value) > -1;
/** @param {Array} arr */
php.array_shift = (arr) => arr.shift();
php.array_push = (arr, el) => arr.push(el);
/** @param {Array} arr */
php.array_pop = (arr) => arr.pop();
/** @param {Array} arr */
php.array_unshift = (arr, value) => arr.unshift(value);

php.array_key_exists = (key, obj) => key in obj;

php.array_merge = (...arrays) => {
	let result = arrays.every(arr => Array.isArray(arr)) ? [] : {};
	for (let arr of arrays) {
		if (Array.isArray(result)) {
			// php drops numeric indexes on array_merge()
			result.push(...arr.filter(a => a !== undefined));
		} else {
			for (let [k,v] of Object.entries(arr)) {
				result[k] = v;
			}
		}
	}
	return result;
};
php.array_intersect_key = (source, whitelist) => {
	let newObj = {};
	for (let [key, val] of Object.entries(source)) {
		if (key in whitelist) {
			newObj[key] = val;
		}
	}
	return newObj;
};
php.array_intersect = (arr1, arr2) => {
	let set2 = new Set(arr2);
	return Object.values(arr1)
		.filter(el => set2.has(el));
};
php.array_diff = (arr1, arr2) => {
	let set2 = new Set(arr2);
	return Object.values(arr1)
		.filter(el => !set2.has(el));
};
php.array_diff_key = (minuend, subtrahend) => {
	let difference = {};
	for (let k in minuend) {
		if (!(k in subtrahend)) {
			difference[k] = minuend[k];
		}
	}
	return difference;
};
php.array_flip = (obj) => {
	let newObj = {};
	for (let [key, val] of Object.entries(obj)) {
		newObj[val] = key;
	}
	return newObj;
};
php.ksort = (obj) => {
	for (let k of Object.keys(obj).sort()) {
		let value = obj[k];
		delete obj[k];
		obj[k] = value;
	}
};
php.range = (start, end, step = 1) => {
	start = +start;
	end = +end;
	step = +step;
	if (!step) {
		throw Error('Step arg must not be 0');
	}
	let arr = [];
	if (start <= end) {
		for (let i = start; i <= end; i += Math.abs(step)) {
			arr.push(i);
		}
	} else {
		// I hate it so much for this behaviour...
		for (let i = start; i >= end; i -= Math.abs(step)) {
			arr.push(i);
		}
	}
	return arr;
};
php.array_unique = (arr) => {
	arr = Array.isArray(arr) ? [...arr] : {...arr};
	let occurrences = new Set();
	for (let k in arr) {
		if (occurrences.has(arr[k])) {
			delete arr[k];
		} else {
			occurrences.add(arr[k]);
		}
	}
	return arr;
};
php.array_reverse = (arr) => Object.values(arr).reverse();
php.array_pad = (array, size, value) => {
	array = Object.values(array);
	let absLen = Math.abs(size);
	let restVals = Array(absLen).fill(value);
	if (size > 0) {
		return array.concat(restVals).slice(0, absLen);
	} else if (size < 0) {
		return restVals.concat(array).slice(-absLen);
	} else {
		throw new Error('Invalid size value for array_pad - ' + size);
	}
};
php.array_splice = (arr, start, length = undefined) => {
	if (Array.isArray(arr)) {
		throw new Error('Tried to splice a non-array - ' + arr);
	}
	arr = Object.values(arr);
	length = length === undefined ? arr.length : length;
	return arr.splice(start, start + length);
};
php.array_slice = (arr, start, length = undefined) => {
	arr = Object.values(arr);
	length = length === undefined ? arr.length : length;
	return arr.slice(start, start + length);
};
php.array_sum = (arr) => {
	let result = 0;
	for (let value of Object.values(arr)) {
		result += +value;
	}
	return result;
};
php.array_column = (arr, key) => {
	return Object.values(arr).map(el => el[key]);
};
php.array_combine = (keys, values) => {
	keys = Object.values(keys);
	values = Object.values(values);
	if (keys.length !== values.length) {
		throw new Error('array_combine passed key count ' + keys.length +
			' does not match value count ' + values.length);
	}
	let result = {};
	for (let i = 0; i < keys.length; ++i) {
		result[keys[i]] = values[i];
	}
	return result;
};

// ------------------
//  functional built-ins follow
// ------------------

let normFunc = (func) => {
	if (typeof func === 'string') {
		if (func in php) {
			func = php[func];
		} else {
			throw Error('Unsupported built-in function - ' + func);
		}
	}
	return func;
};
php.array_map = (func, obj, additionalValues = []) => {
	func = normFunc(func);
	let newObj = Array.isArray(obj) ? [] : {};
	for (let [key, val] of Object.entries(obj)) {
		newObj[key] = func(val, additionalValues[key]);
	}
	return newObj;
};
php.array_filter = (obj, func, flags = null) => {
	func = normFunc(func) || ((v) => !empty(v));
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

let isPlainObject = (val) => val && val.constructor && val.constructor.name === 'Object';
php.count = val => Object.values(val).length;

//php.PREG_OFFSET_CAPTURE = 256;

php.PHP_EOL = '\n';

module.exports = php;
