// ==ClosureCompiler==
// @compilation_level SIMPLE_OPTIMIZATIONS

/**
 * @license Highstock JS v4.2.5 (2016-05-06)
 *
 * (c) 2009-2016 Torstein Honsi
 *
 * License: www.highcharts.com/license
 */

(function (root, factory) {
    if (typeof module === 'object' && module.exports) {
        module.exports = root.document ?
            factory(root) : 
            factory;
    } else {
        root.Highcharts = factory(root);
    }
}(typeof window !== 'undefined' ? window : this, function (win) { // eslint-disable-line no-undef
// encapsulated variables
    var UNDEFINED,
        doc = win.document,
        math = Math,
        mathRound = math.round,
        mathFloor = math.floor,
        mathCeil = math.ceil,
        mathMax = math.max,
        mathMin = math.min,
        mathAbs = math.abs,
        mathCos = math.cos,
        mathSin = math.sin,
        mathPI = math.PI,
        deg2rad = mathPI * 2 / 360,


        // some variables
        userAgent = (win.navigator && win.navigator.userAgent) || '',
        isOpera = win.opera,
        isMS = /(msie|trident|edge)/i.test(userAgent) && !isOpera,
        docMode8 = doc && doc.documentMode === 8,
        isWebKit = !isMS && /AppleWebKit/.test(userAgent),
        isFirefox = /Firefox/.test(userAgent),
        isTouchDevice = /(Mobile|Android|Windows Phone)/.test(userAgent),
        SVG_NS = 'http://www.w3.org/2000/svg',
        hasSVG = doc && doc.createElementNS && !!doc.createElementNS(SVG_NS, 'svg').createSVGRect,
        hasBidiBug = isFirefox && parseInt(userAgent.split('Firefox/')[1], 10) < 4, // issue #38
        useCanVG = doc && !hasSVG && !isMS && !!doc.createElement('canvas').getContext,
        Renderer,
        hasTouch,
        symbolSizes = {},
        idCounter = 0,
        garbageBin,
        defaultOptions,
        dateFormat, // function
        pathAnim,
        timeUnits,
        noop = function () {},
        charts = [],
        chartCount = 0,
        PRODUCT = 'Highstock',
        VERSION = '4.2.5',

        // some constants for frequently used strings
        DIV = 'div',
        ABSOLUTE = 'absolute',
        RELATIVE = 'relative',
        HIDDEN = 'hidden',
        PREFIX = 'highcharts-',
        VISIBLE = 'visible',
        PX = 'px',
        NONE = 'none',
        M = 'M',
        L = 'L',
        numRegex = /^[0-9]+$/,
        NORMAL_STATE = '',
        HOVER_STATE = 'hover',
        SELECT_STATE = 'select',
        marginNames = ['plotTop', 'marginRight', 'marginBottom', 'plotLeft'],

        // Object for extending Axis
        AxisPlotLineOrBandExtension,

        // constants for attributes
        STROKE_WIDTH = 'stroke-width',

        // time methods, changed based on whether or not UTC is used
        Date,  // Allow using a different Date class
        makeTime,
        timezoneOffset,
        getTimezoneOffset,
        getMinutes,
        getHours,
        getDay,
        getDate,
        getMonth,
        getFullYear,
        setMilliseconds,
        setSeconds,
        setMinutes,
        setHours,
        setDate,
        setMonth,
        setFullYear,


        // lookup over the types and the associated classes
        seriesTypes = {},
        Highcharts;

    /**
     * Provide error messages for debugging, with links to online explanation
     */
    function error(code, stop) {
        var msg = 'Highcharts error #' + code + ': www.highcharts.com/errors/' + code;
        if (stop) {
            throw new Error(msg);
        }
        // else ...
        if (win.console) {
            console.log(msg); // eslint-disable-line no-console
        }
    }

    // The Highcharts namespace
    Highcharts = win.Highcharts ? error(16, true) : { win: win };

    Highcharts.seriesTypes = seriesTypes;
    var timers = [],
        getStyle,

        // Previous adapter functions
        inArray,
        each,
        grep,
        offset,
        map,
        addEvent,
        removeEvent,
        fireEvent,
        animate,
        stop;

    /**
     * An animator object. One instance applies to one property (attribute or style prop) 
     * on one element.
     * 
     * @param {object} elem    The element to animate. May be a DOM element or a Highcharts SVGElement wrapper.
     * @param {object} options Animation options, including duration, easing, step and complete.
     * @param {object} prop    The property to animate.
     */
    function Fx(elem, options, prop) {
        this.options = options;
        this.elem = elem;
        this.prop = prop;
    }
    Fx.prototype = {
    
        /**
         * Animating a path definition on SVGElement
         * @returns {undefined} 
         */
        dSetter: function () {
            var start = this.paths[0],
                end = this.paths[1],
                ret = [],
                now = this.now,
                i = start.length,
                startVal;

            if (now === 1) { // land on the final path without adjustment points appended in the ends
                ret = this.toD;

            } else if (i === end.length && now < 1) {
                while (i--) {
                    startVal = parseFloat(start[i]);
                    ret[i] =
                        isNaN(startVal) ? // a letter instruction like M or L
                                start[i] :
                                now * (parseFloat(end[i] - startVal)) + startVal;

                }
            } else { // if animation is finished or length not matching, land on right value
                ret = end;
            }
            this.elem.attr('d', ret);
        },

        /**
         * Update the element with the current animation step
         * @returns {undefined}
         */
        update: function () {
            var elem = this.elem,
                prop = this.prop, // if destroyed, it is null
                now = this.now,
                step = this.options.step;

            // Animation setter defined from outside
            if (this[prop + 'Setter']) {
                this[prop + 'Setter']();

            // Other animations on SVGElement
            } else if (elem.attr) {
                if (elem.element) {
                    elem.attr(prop, now);
                }

            // HTML styles, raw HTML content like container size
            } else {
                elem.style[prop] = now + this.unit;
            }
        
            if (step) {
                step.call(elem, now, this);
            }

        },

        /**
         * Run an animation
         */
        run: function (from, to, unit) {
            var self = this,
                timer = function (gotoEnd) {
                    return timer.stopped ? false : self.step(gotoEnd);
                },
                i;

            this.startTime = +new Date();
            this.start = from;
            this.end = to;
            this.unit = unit;
            this.now = this.start;
            this.pos = 0;

            timer.elem = this.elem;

            if (timer() && timers.push(timer) === 1) {
                timer.timerId = setInterval(function () {
                
                    for (i = 0; i < timers.length; i++) {
                        if (!timers[i]()) {
                            timers.splice(i--, 1);
                        }
                    }

                    if (!timers.length) {
                        clearInterval(timer.timerId);
                    }
                }, 13);
            }
        },
    
        /**
         * Run a single step in the animation
         * @param   {Boolean} gotoEnd Whether to go to then endpoint of the animation after abort
         * @returns {Boolean} True if animation continues
         */
        step: function (gotoEnd) {
            var t = +new Date(),
                ret,
                done,
                options = this.options,
                elem = this.elem,
                complete = options.complete,
                duration = options.duration,
                curAnim = options.curAnim,
                i;
        
            if (elem.attr && !elem.element) { // #2616, element including flag is destroyed
                ret = false;

            } else if (gotoEnd || t >= duration + this.startTime) {
                this.now = this.end;
                this.pos = 1;
                this.update();

                curAnim[this.prop] = true;

                done = true;
                for (i in curAnim) {
                    if (curAnim[i] !== true) {
                        done = false;
                    }
                }

                if (done && complete) {
                    complete.call(elem);
                }
                ret = false;

            } else {
                this.pos = options.easing((t - this.startTime) / duration);
                this.now = this.start + ((this.end - this.start) * this.pos);
                this.update();
                ret = true;
            }
            return ret;
        },

        /**
         * Prepare start and end values so that the path can be animated one to one
         */
        initPath: function (elem, fromD, toD) {
            fromD = fromD || '';
            var shift = elem.shift,
                bezier = fromD.indexOf('C') > -1,
                numParams = bezier ? 7 : 3,
                endLength,
                slice,
                i,
                start = fromD.split(' '),
                end = [].concat(toD), // copy
                isArea = elem.isArea,
                positionFactor = isArea ? 2 : 1,
                sixify = function (arr) { // in splines make move points have six parameters like bezier curves
                    i = arr.length;
                    while (i--) {
                        if (arr[i] === M || arr[i] === L) {
                            arr.splice(i + 1, 0, arr[i + 1], arr[i + 2], arr[i + 1], arr[i + 2]);
                        }
                    }
                };

            if (bezier) {
                sixify(start);
                sixify(end);
            }

            // If shifting points, prepend a dummy point to the end path. For areas,
            // prepend both at the beginning and end of the path.
            if (shift <= end.length / numParams && start.length === end.length) {
                while (shift--) {
                    end = end.slice(0, numParams).concat(end);
                    if (isArea) {
                        end = end.concat(end.slice(end.length - numParams));
                    }
                }
            }
            elem.shift = 0; // reset for following animations

        
            // Copy and append last point until the length matches the end length
            if (start.length) {
                endLength = end.length;
                while (start.length < endLength) {

                    // Pull out the slice that is going to be appended or inserted. In a line graph,
                    // the positionFactor is 1, and the last point is sliced out. In an area graph,
                    // the positionFactor is 2, causing the middle two points to be sliced out, since
                    // an area path starts at left, follows the upper path then turns and follows the
                    // bottom back. 
                    slice = start.slice().splice(
                        (start.length / positionFactor) - numParams, 
                        numParams * positionFactor
                    );
                
                    // Disable first control point
                    if (bezier) {
                        slice[numParams - 6] = slice[numParams - 2];
                        slice[numParams - 5] = slice[numParams - 1];
                    }
                
                    // Now insert the slice, either in the middle (for areas) or at the end (for lines)
                    [].splice.apply(
                        start, 
                        [(start.length / positionFactor), 0].concat(slice)
                    );

                }
            }

            return [start, end];
        }
    }; // End of Fx prototype


    /**
     * Extend an object with the members of another
     * @param {Object} a The object to be extended
     * @param {Object} b The object to add to the first one
     */
    var extend = Highcharts.extend = function (a, b) {
        var n;
        if (!a) {
            a = {};
        }
        for (n in b) {
            a[n] = b[n];
        }
        return a;
    };

    /**
     * Deep merge two or more objects and return a third object. If the first argument is
     * true, the contents of the second object is copied into the first object.
     * Previously this function redirected to jQuery.extend(true), but this had two limitations.
     * First, it deep merged arrays, which lead to workarounds in Highcharts. Second,
     * it copied properties from extended prototypes.
     */
    function merge() {
        var i,
            args = arguments,
            len,
            ret = {},
            doCopy = function (copy, original) {
                var value, key;

                // An object is replacing a primitive
                if (typeof copy !== 'object') {
                    copy = {};
                }

                for (key in original) {
                    if (original.hasOwnProperty(key)) {
                        value = original[key];

                        // Copy the contents of objects, but not arrays or DOM nodes
                        if (value && typeof value === 'object' && Object.prototype.toString.call(value) !== '[object Array]' &&
                                key !== 'renderTo' && typeof value.nodeType !== 'number') {
                            copy[key] = doCopy(copy[key] || {}, value);

                        // Primitives and arrays are copied over directly
                        } else {
                            copy[key] = original[key];
                        }
                    }
                }
                return copy;
            };

        // If first argument is true, copy into the existing object. Used in setOptions.
        if (args[0] === true) {
            ret = args[1];
            args = Array.prototype.slice.call(args, 2);
        }

        // For each argument, extend the return
        len = args.length;
        for (i = 0; i < len; i++) {
            ret = doCopy(ret, args[i]);
        }

        return ret;
    }

    /**
     * Shortcut for parseInt
     * @param {Object} s
     * @param {Number} mag Magnitude
     */
    function pInt(s, mag) {
        return parseInt(s, mag || 10);
    }

    /**
     * Check for string
     * @param {Object} s
     */
    function isString(s) {
        return typeof s === 'string';
    }

    /**
     * Check for object
     * @param {Object} obj
     */
    function isObject(obj) {
        return obj && typeof obj === 'object';
    }

    /**
     * Check for array
     * @param {Object} obj
     */
    function isArray(obj) {
        return Object.prototype.toString.call(obj) === '[object Array]';
    }

    /**
     * Check for number
     * @param {Object} n
     */
    var isNumber = Highcharts.isNumber = function isNumber(n) {
        return typeof n === 'number' && !isNaN(n);
    };

    /**
     * Remove last occurence of an item from an array
     * @param {Array} arr
     * @param {Mixed} item
     */
    function erase(arr, item) {
        var i = arr.length;
        while (i--) {
            if (arr[i] === item) {
                arr.splice(i, 1);
                break;
            }
        }
        //return arr;
    }

    /**
     * Returns true if the object is not null or undefined.
     * @param {Object} obj
     */
    function defined(obj) {
        return obj !== UNDEFINED && obj !== null;
    }

    /**
     * Set or get an attribute or an object of attributes. Can't use jQuery attr because
     * it attempts to set expando properties on the SVG element, which is not allowed.
     *
     * @param {Object} elem The DOM element to receive the attribute(s)
     * @param {String|Object} prop The property or an abject of key-value pairs
     * @param {String} value The value if a single property is set
     */
    function attr(elem, prop, value) {
        var key,
            ret;

        // if the prop is a string
        if (isString(prop)) {
            // set the value
            if (defined(value)) {
                elem.setAttribute(prop, value);

            // get the value
            } else if (elem && elem.getAttribute) { // elem not defined when printing pie demo...
                ret = elem.getAttribute(prop);
            }

        // else if prop is defined, it is a hash of key/value pairs
        } else if (defined(prop) && isObject(prop)) {
            for (key in prop) {
                elem.setAttribute(key, prop[key]);
            }
        }
        return ret;
    }
    /**
     * Check if an element is an array, and if not, make it into an array.
     */
    function splat(obj) {
        return isArray(obj) ? obj : [obj];
    }

    /**
     * Set a timeout if the delay is given, otherwise perform the function synchronously
     * @param   {Function} fn      The function to perform
     * @param   {Number}   delay   Delay in milliseconds
     * @param   {Ojbect}   context The context
     * @returns {Nubmer}           An identifier for the timeout
     */
    function syncTimeout(fn, delay, context) {
        if (delay) {
            return setTimeout(fn, delay, context);
        }
        fn.call(0, context);
    }


    /**
     * Return the first value that is defined.
     */
    var pick = Highcharts.pick = function () {
        var args = arguments,
            i,
            arg,
            length = args.length;
        for (i = 0; i < length; i++) {
            arg = args[i];
            if (arg !== UNDEFINED && arg !== null) {
                return arg;
            }
        }
    };

    /**
     * Set CSS on a given element
     * @param {Object} el
     * @param {Object} styles Style object with camel case property names
     */
    function css(el, styles) {
        if (isMS && !hasSVG) { // #2686
            if (styles && styles.opacity !== UNDEFINED) {
                styles.filter = 'alpha(opacity=' + (styles.opacity * 100) + ')';
            }
        }
        extend(el.style, styles);
    }

    /**
     * Utility function to create element with attributes and styles
     * @param {Object} tag
     * @param {Object} attribs
     * @param {Object} styles
     * @param {Object} parent
     * @param {Object} nopad
     */
    function createElement(tag, attribs, styles, parent, nopad) {
        var el = doc.createElement(tag);
        if (attribs) {
            extend(el, attribs);
        }
        if (nopad) {
            css(el, { padding: 0, border: 'none', margin: 0 });
        }
        if (styles) {
            css(el, styles);
        }
        if (parent) {
            parent.appendChild(el);
        }
        return el;
    }

    /**
     * Extend a prototyped class by new members
     * @param {Object} parent
     * @param {Object} members
     */
    function extendClass(Parent, members) {
        var object = function () {
        };
        object.prototype = new Parent();
        extend(object.prototype, members);
        return object;
    }

    /**
     * Pad a string to a given length by adding 0 to the beginning
     * @param {Number} number
     * @param {Number} length
     */
    function pad(number, length, padder) {
        return new Array((length || 2) + 1 - String(number).length).join(padder || 0) + number;
    }

    /**
     * Return a length based on either the integer value, or a percentage of a base.
     */
    function relativeLength(value, base) {
        return (/%$/).test(value) ? base * parseFloat(value) / 100 : parseFloat(value);
    }

    /**
     * Wrap a method with extended functionality, preserving the original function
     * @param {Object} obj The context object that the method belongs to
     * @param {String} method The name of the method to extend
     * @param {Function} func A wrapper function callback. This function is called with the same arguments
     * as the original function, except that the original function is unshifted and passed as the first
     * argument.
     */
    var wrap = Highcharts.wrap = function (obj, method, func) {
        var proceed = obj[method];
        obj[method] = function () {
            var args = Array.prototype.slice.call(arguments);
            args.unshift(proceed);
            return func.apply(this, args);
        };
    };


    function getTZOffset(timestamp) {
        return ((getTimezoneOffset && getTimezoneOffset(timestamp)) || timezoneOffset || 0) * 60000;
    }

    /**
     * Based on http://www.php.net/manual/en/function.strftime.php
     * @param {String} format
     * @param {Number} timestamp
     * @param {Boolean} capitalize
     */
    dateFormat = function (format, timestamp, capitalize) {
        if (!isNumber(timestamp)) {
            return defaultOptions.lang.invalidDate || '';
        }
        format = pick(format, '%Y-%m-%d %H:%M:%S');

        var date = new Date(timestamp - getTZOffset(timestamp)),
            key, // used in for constuct below
            // get the basic time values
            hours = date[getHours](),
            day = date[getDay](),
            dayOfMonth = date[getDate](),
            month = date[getMonth](),
            fullYear = date[getFullYear](),
            lang = defaultOptions.lang,
            langWeekdays = lang.weekdays,
            shortWeekdays = lang.shortWeekdays,

            // List all format keys. Custom formats can be added from the outside.
            replacements = extend({

                // Day
                'a': shortWeekdays ? shortWeekdays[day] : langWeekdays[day].substr(0, 3), // Short weekday, like 'Mon'
                'A': langWeekdays[day], // Long weekday, like 'Monday'
                'd': pad(dayOfMonth), // Two digit day of the month, 01 to 31
                'e': pad(dayOfMonth, 2, ' '), // Day of the month, 1 through 31
                'w': day,

                // Week (none implemented)
                //'W': weekNumber(),

                // Month
                'b': lang.shortMonths[month], // Short month, like 'Jan'
                'B': lang.months[month], // Long month, like 'January'
                'm': pad(month + 1), // Two digit month number, 01 through 12

                // Year
                'y': fullYear.toString().substr(2, 2), // Two digits year, like 09 for 2009
                'Y': fullYear, // Four digits year, like 2009

                // Time
                'H': pad(hours), // Two digits hours in 24h format, 00 through 23
                'k': hours, // Hours in 24h format, 0 through 23
                'I': pad((hours % 12) || 12), // Two digits hours in 12h format, 00 through 11
                'l': (hours % 12) || 12, // Hours in 12h format, 1 through 12
                'M': pad(date[getMinutes]()), // Two digits minutes, 00 through 59
                'p': hours < 12 ? 'AM' : 'PM', // Upper case AM or PM
                'P': hours < 12 ? 'am' : 'pm', // Lower case AM or PM
                'S': pad(date.getSeconds()), // Two digits seconds, 00 through  59
                'L': pad(mathRound(timestamp % 1000), 3) // Milliseconds (naming from Ruby)
            }, Highcharts.dateFormats);


        // do the replaces
        for (key in replacements) {
            while (format.indexOf('%' + key) !== -1) { // regex would do it in one line, but this is faster
                format = format.replace('%' + key, typeof replacements[key] === 'function' ? replacements[key](timestamp) : replacements[key]);
            }
        }

        // Optionally capitalize the string and return
        return capitalize ? format.substr(0, 1).toUpperCase() + format.substr(1) : format;
    };

    /**
     * Format a single variable. Similar to sprintf, without the % prefix.
     */
    function formatSingle(format, val) {
        var floatRegex = /f$/,
            decRegex = /\.([0-9])/,
            lang = defaultOptions.lang,
            decimals;

        if (floatRegex.test(format)) { // float
            decimals = format.match(decRegex);
            decimals = decimals ? decimals[1] : -1;
            if (val !== null) {
                val = Highcharts.numberFormat(
                    val,
                    decimals,
                    lang.decimalPoint,
                    format.indexOf(',') > -1 ? lang.thousandsSep : ''
                );
            }
        } else {
            val = dateFormat(format, val);
        }
        return val;
    }

    /**
     * Format a string according to a subset of the rules of Python's String.format method.
     */
    function format(str, ctx) {
        var splitter = '{',
            isInside = false,
            segment,
            valueAndFormat,
            path,
            i,
            len,
            ret = [],
            val,
            index;

        while ((index = str.indexOf(splitter)) !== -1) {

            segment = str.slice(0, index);
            if (isInside) { // we're on the closing bracket looking back

                valueAndFormat = segment.split(':');
                path = valueAndFormat.shift().split('.'); // get first and leave format
                len = path.length;
                val = ctx;

                // Assign deeper paths
                for (i = 0; i < len; i++) {
                    val = val[path[i]];
                }

                // Format the replacement
                if (valueAndFormat.length) {
                    val = formatSingle(valueAndFormat.join(':'), val);
                }

                // Push the result and advance the cursor
                ret.push(val);

            } else {
                ret.push(segment);

            }
            str = str.slice(index + 1); // the rest
            isInside = !isInside; // toggle
            splitter = isInside ? '}' : '{'; // now look for next matching bracket
        }
        ret.push(str);
        return ret.join('');
    }

    /**
     * Get the magnitude of a number
     */
    function getMagnitude(num) {
        return math.pow(10, mathFloor(math.log(num) / math.LN10));
    }

    /**
     * Take an interval and normalize it to multiples of 1, 2, 2.5 and 5
     * @param {Number} interval
     * @param {Array} multiples
     * @param {Number} magnitude
     * @param {Object} options
     */
    function normalizeTickInterval(interval, multiples, magnitude, allowDecimals, preventExceed) {
        var normalized,
            i,
            retInterval = interval;

        // round to a tenfold of 1, 2, 2.5 or 5
        magnitude = pick(magnitude, 1);
        normalized = interval / magnitude;

        // multiples for a linear scale
        if (!multiples) {
            multiples = [1, 2, 2.5, 5, 10];

            // the allowDecimals option
            if (allowDecimals === false) {
                if (magnitude === 1) {
                    multiples = [1, 2, 5, 10];
                } else if (magnitude <= 0.1) {
                    multiples = [1 / magnitude];
                }
            }
        }

        // normalize the interval to the nearest multiple
        for (i = 0; i < multiples.length; i++) {
            retInterval = multiples[i];
            if ((preventExceed && retInterval * magnitude >= interval) || // only allow tick amounts smaller than natural
                    (!preventExceed && (normalized <= (multiples[i] + (multiples[i + 1] || multiples[i])) / 2))) {
                break;
            }
        }

        // multiply back to the correct magnitude
        retInterval *= magnitude;

        return retInterval;
    }


    /**
     * Utility method that sorts an object array and keeping the order of equal items.
     * ECMA script standard does not specify the behaviour when items are equal.
     */
    function stableSort(arr, sortFunction) {
        var length = arr.length,
            sortValue,
            i;

        // Add index to each item
        for (i = 0; i < length; i++) {
            arr[i].safeI = i; // stable sort index
        }

        arr.sort(function (a, b) {
            sortValue = sortFunction(a, b);
            return sortValue === 0 ? a.safeI - b.safeI : sortValue;
        });

        // Remove index from items
        for (i = 0; i < length; i++) {
            delete arr[i].safeI; // stable sort index
        }
    }

    /**
     * Non-recursive method to find the lowest member of an array. Math.min raises a maximum
     * call stack size exceeded error in Chrome when trying to apply more than 150.000 points. This
     * method is slightly slower, but safe.
     */
    function arrayMin(data) {
        var i = data.length,
            min = data[0];

        while (i--) {
            if (data[i] < min) {
                min = data[i];
            }
        }
        return min;
    }

    /**
     * Non-recursive method to find the lowest member of an array. Math.min raises a maximum
     * call stack size exceeded error in Chrome when trying to apply more than 150.000 points. This
     * method is slightly slower, but safe.
     */
    function arrayMax(data) {
        var i = data.length,
            max = data[0];

        while (i--) {
            if (data[i] > max) {
                max = data[i];
            }
        }
        return max;
    }

    /**
     * Utility method that destroys any SVGElement or VMLElement that are properties on the given object.
     * It loops all properties and invokes destroy if there is a destroy method. The property is
     * then delete'ed.
     * @param {Object} The object to destroy properties on
     * @param {Object} Exception, do not destroy this property, only delete it.
     */
    function destroyObjectProperties(obj, except) {
        var n;
        for (n in obj) {
            // If the object is non-null and destroy is defined
            if (obj[n] && obj[n] !== except && obj[n].destroy) {
                // Invoke the destroy
                obj[n].destroy();
            }

            // Delete the property from the object.
            delete obj[n];
        }
    }


    /**
     * Discard an element by moving it to the bin and delete
     * @param {Object} The HTML node to discard
     */
    function discardElement(element) {
        // create a garbage bin element, not part of the DOM
        if (!garbageBin) {
            garbageBin = createElement(DIV);
        }

        // move the node and empty bin
        if (element) {
            garbageBin.appendChild(element);
        }
        garbageBin.innerHTML = '';
    }

    /**
     * Fix JS round off float errors
     * @param {Number} num
     */
    function correctFloat(num, prec) {
        return parseFloat(
            num.toPrecision(prec || 14)
        );
    }

    /**
     * Set the global animation to either a given value, or fall back to the
     * given chart's animation option
     * @param {Object} animation
     * @param {Object} chart
     */
    function setAnimation(animation, chart) {
        chart.renderer.globalAnimation = pick(animation, chart.animation);
    }

    /**
     * Get the animation in object form, where a disabled animation is always
     * returned with duration: 0
     */
    function animObject(animation) {
        return isObject(animation) ? merge(animation) : { duration: animation ? 500 : 0 };
    }

    /**
     * The time unit lookup
     */
    timeUnits = {
        millisecond: 1,
        second: 1000,
        minute: 60000,
        hour: 3600000,
        day: 24 * 3600000,
        week: 7 * 24 * 3600000,
        month: 28 * 24 * 3600000,
        year: 364 * 24 * 3600000
    };


    /**
     * Format a number and return a string based on input settings
     * @param {Number} number The input number to format
     * @param {Number} decimals The amount of decimals
     * @param {String} decimalPoint The decimal point, defaults to the one given in the lang options
     * @param {String} thousandsSep The thousands separator, defaults to the one given in the lang options
     */
    Highcharts.numberFormat = function (number, decimals, decimalPoint, thousandsSep) {

        number = +number || 0;
        decimals = +decimals;

        var lang = defaultOptions.lang,
            origDec = (number.toString().split('.')[1] || '').length,
            decimalComponent,
            strinteger,
            thousands,
            absNumber = Math.abs(number),
            ret;

        if (decimals === -1) {
            decimals = Math.min(origDec, 20); // Preserve decimals. Not huge numbers (#3793).
        } else if (!isNumber(decimals)) {
            decimals = 2;
        }

        // A string containing the positive integer component of the number
        strinteger = String(pInt(absNumber.toFixed(decimals)));

        // Leftover after grouping into thousands. Can be 0, 1 or 3.
        thousands = strinteger.length > 3 ? strinteger.length % 3 : 0;

        // Language
        decimalPoint = pick(decimalPoint, lang.decimalPoint);
        thousandsSep = pick(thousandsSep, lang.thousandsSep);

        // Start building the return
        ret = number < 0 ? '-' : '';

        // Add the leftover after grouping into thousands. For example, in the number 42 000 000,
        // this line adds 42.
        ret += thousands ? strinteger.substr(0, thousands) + thousandsSep : '';

        // Add the remaining thousands groups, joined by the thousands separator
        ret += strinteger.substr(thousands).replace(/(\d{3})(?=\d)/g, '$1' + thousandsSep);

        // Add the decimal point and the decimal component
        if (decimals) {
            // Get the decimal component, and add power to avoid rounding errors with float numbers (#4573)
            decimalComponent = Math.abs(absNumber - strinteger + Math.pow(10, -Math.max(decimals, origDec) - 1));
            ret += decimalPoint + decimalComponent.toFixed(decimals).slice(2);
        }

        return ret;
    };

    /**
     * Easing definition
     * @param   {Number} pos Current position, ranging from 0 to 1
     */
    Math.easeInOutSine = function (pos) {
        return -0.5 * (Math.cos(Math.PI * pos) - 1);
    };

    /**
     * Internal method to return CSS value for given element and property
     */
    getStyle = function (el, prop) {

        var style;

        // For width and height, return the actual inner pixel size (#4913)
        if (prop === 'width') {
            return Math.min(el.offsetWidth, el.scrollWidth) - getStyle(el, 'padding-left') - getStyle(el, 'padding-right');
        } else if (prop === 'height') {
            return Math.min(el.offsetHeight, el.scrollHeight) - getStyle(el, 'padding-top') - getStyle(el, 'padding-bottom');
        }

        // Otherwise, get the computed style
        style = win.getComputedStyle(el, undefined);
        return style && pInt(style.getPropertyValue(prop));
    };

    /**
     * Return the index of an item in an array, or -1 if not found
     */
    inArray = function (item, arr) {
        return arr.indexOf ? arr.indexOf(item) : [].indexOf.call(arr, item);
    };

    /**
     * Filter an array
     */
    grep = function (elements, callback) {
        return [].filter.call(elements, callback);
    };

    /**
     * Map an array
     */
    map = function (arr, fn) {
        var results = [],
            i = 0,
            len = arr.length;

        for (; i < len; i++) {
            results[i] = fn.call(arr[i], arr[i], i, arr);
        }

        return results;
    };

    /**
     * Get the element's offset position, corrected by overflow:auto.
     */
    offset = function (el) {
        var docElem = doc.documentElement,
            box = el.getBoundingClientRect();

        return {
            top: box.top  + (win.pageYOffset || docElem.scrollTop)  - (docElem.clientTop  || 0),
            left: box.left + (win.pageXOffset || docElem.scrollLeft) - (docElem.clientLeft || 0)
        };
    };

    /**
     * Stop running animation.
     * A possible extension to this would be to stop a single property, when
     * we want to continue animating others. Then assign the prop to the timer
     * in the Fx.run method, and check for the prop here. This would be an improvement
     * in all cases where we stop the animation from .attr. Instead of stopping
     * everything, we can just stop the actual attributes we're setting.
     */
    stop = function (el) {

        var i = timers.length;

        // Remove timers related to this element (#4519)
        while (i--) {
            if (timers[i].elem === el) {
                timers[i].stopped = true; // #4667
            }
        }
    };

    /**
     * Utility for iterating over an array.
     * @param {Array} arr
     * @param {Function} fn
     */
    each = function (arr, fn) { // modern browsers
        return Array.prototype.forEach.call(arr, fn);
    };

    /**
     * Add an event listener
     */
    addEvent = function (el, type, fn) {
    
        var events = el.hcEvents = el.hcEvents || {};

        function wrappedFn(e) {
            e.target = e.srcElement || win; // #2820
            fn.call(el, e);
        }

        // Handle DOM events in modern browsers
        if (el.addEventListener) {
            el.addEventListener(type, fn, false);

        // Handle old IE implementation
        } else if (el.attachEvent) {

            if (!el.hcEventsIE) {
                el.hcEventsIE = {};
            }

            // Link wrapped fn with original fn, so we can get this in removeEvent
            el.hcEventsIE[fn.toString()] = wrappedFn;

            el.attachEvent('on' + type, wrappedFn);
        }

        if (!events[type]) {
            events[type] = [];
        }

        events[type].push(fn);
    };

    /**
     * Remove event added with addEvent
     */
    removeEvent = function (el, type, fn) {
    
        var events,
            hcEvents = el.hcEvents,
            index;

        function removeOneEvent(type, fn) {
            if (el.removeEventListener) {
                el.removeEventListener(type, fn, false);
            } else if (el.attachEvent) {
                fn = el.hcEventsIE[fn.toString()];
                el.detachEvent('on' + type, fn);
            }
        }

        function removeAllEvents() {
            var types,
                len,
                n;

            if (!el.nodeName) {
                return; // break on non-DOM events
            }

            if (type) {
                types = {};
                types[type] = true;
            } else {
                types = hcEvents;
            }

            for (n in types) {
                if (hcEvents[n]) {
                    len = hcEvents[n].length;
                    while (len--) {
                        removeOneEvent(n, hcEvents[n][len]);
                    }
                }
            }
        }

        if (hcEvents) {
            if (type) {
                events = hcEvents[type] || [];
                if (fn) {
                    index = inArray(fn, events);
                    if (index > -1) {
                        events.splice(index, 1);
                        hcEvents[type] = events;
                    }
                    removeOneEvent(type, fn);

                } else {
                    removeAllEvents();
                    hcEvents[type] = [];
                }
            } else {
                removeAllEvents();
                el.hcEvents = {};
            }
        }
    };

    /**
     * Fire an event on a custom object
     */
    fireEvent = function (el, type, eventArguments, defaultFunction) {
        var e,
            hcEvents = el.hcEvents,
            events,
            len,
            i,
            fn;

        eventArguments = eventArguments || {};

        if (doc.createEvent && (el.dispatchEvent || el.fireEvent)) {
            e = doc.createEvent('Events');
            e.initEvent(type, true, true);
            e.target = el;

            extend(e, eventArguments);

            if (el.dispatchEvent) {
                el.dispatchEvent(e);
            } else {
                el.fireEvent(type, e);
            }

        } else if (hcEvents) {
        
            events = hcEvents[type] || [];
            len = events.length;

            // Attach a simple preventDefault function to skip default handler if called. 
            // The built-in defaultPrevented property is not overwritable (#5112)
            if (!eventArguments.preventDefault) {
                eventArguments.preventDefault = function () {
                    eventArguments.defaultPrevented = true;
                };
            }

            eventArguments.target = el;

            // If the type is not set, we're running a custom event (#2297). If it is set,
            // we're running a browser event, and setting it will cause en error in
            // IE8 (#2465).
            if (!eventArguments.type) {
                eventArguments.type = type;
            }
        
            for (i = 0; i < len; i++) {
                fn = events[i];

                // If the event handler return false, prevent the default handler from executing
                if (fn.call(el, eventArguments) === false) {
                    eventArguments.preventDefault();
                }
            }
        }
            
        // Run the default if not prevented
        if (defaultFunction && !eventArguments.defaultPrevented) {
            defaultFunction(eventArguments);
        }
    };

    /**
     * The global animate method, which uses Fx to create individual animators.
     */
    animate = function (el, params, opt) {
        var start,
            unit = '',
            end,
            fx,
            args,
            prop;

        if (!isObject(opt)) { // Number or undefined/null
            args = arguments;
            opt = {
                duration: args[2],
                easing: args[3],
                complete: args[4]
            };
        }
        if (!isNumber(opt.duration)) {
            opt.duration = 400;
        }
        opt.easing = typeof opt.easing === 'function' ? opt.easing : (Math[opt.easing] || Math.easeInOutSine);
        opt.curAnim = merge(params);

        for (prop in params) {
            fx = new Fx(el, opt, prop);
            end = null;

            if (prop === 'd') {
                fx.paths = fx.initPath(
                    el,
                    el.d,
                    params.d
                );
                fx.toD = params.d;
                start = 0;
                end = 1;
            } else if (el.attr) {
                start = el.attr(prop);
            } else {
                start = parseFloat(getStyle(el, prop)) || 0;
                if (prop !== 'opacity') {
                    unit = 'px';
                }
            }

            if (!end) {
                end = params[prop];
            }
            if (end.match && end.match('px')) {
                end = end.replace(/px/g, ''); // #4351
            }
            fx.run(start, end, unit);
        }
    };

    /**
     * Register Highcharts as a plugin in jQuery
     */
    if (win.jQuery) {
        win.jQuery.fn.highcharts = function () {
            var args = [].slice.call(arguments);

            if (this[0]) { // this[0] is the renderTo div

                // Create the chart
                if (args[0]) {
                    new Highcharts[ // eslint-disable-line no-new
                        isString(args[0]) ? args.shift() : 'Chart' // Constructor defaults to Chart
                    ](this[0], args[0], args[1]);
                    return this;
                }

                // When called without parameters or with the return argument, return an existing chart
                return charts[attr(this[0], 'data-highcharts-chart')];
            }
        };
    }


    /**
     * Compatibility section to add support for legacy IE. This can be removed if old IE 
     * support is not needed.
     */
    if (doc && !doc.defaultView) {
        getStyle = function (el, prop) {
            var val,
                alias = { width: 'clientWidth', height: 'clientHeight' }[prop];
            
            if (el.style[prop]) {
                return pInt(el.style[prop]);
            }
            if (prop === 'opacity') {
                prop = 'filter';
            }

            // Getting the rendered width and height
            if (alias) {
                el.style.zoom = 1;
                return Math.max(el[alias] - 2 * getStyle(el, 'padding'), 0);
            }
        
            val = el.currentStyle[prop.replace(/\-(\w)/g, function (a, b) {
                return b.toUpperCase();
            })];
            if (prop === 'filter') {
                val = val.replace(
                    /alpha\(opacity=([0-9]+)\)/, 
                    function (a, b) { 
                        return b / 100; 
                    }
                );
            }
        
            return val === '' ? 1 : pInt(val);
        };
    }

    if (!Array.prototype.forEach) {
        each = function (arr, fn) { // legacy
            var i = 0, 
                len = arr.length;
            for (; i < len; i++) {
                if (fn.call(arr[i], arr[i], i, arr) === false) {
                    return i;
                }
            }
        };
    }

    if (!Array.prototype.indexOf) {
        inArray = function (item, arr) {
            var len, 
                i = 0;

            if (arr) {
                len = arr.length;
            
                for (; i < len; i++) {
                    if (arr[i] === item) {
                        return i;
                    }
                }
            }

            return -1;
        };
    }

    if (!Array.prototype.filter) {
        grep = function (elements, fn) {
            var ret = [],
                i = 0,
                length = elements.length;

            for (; i < length; i++) {
                if (fn(elements[i], i)) {
                    ret.push(elements[i]);
                }
            }

            return ret;
        };
    }

    //--- End compatibility section ---

    // Expose utilities
    Highcharts.Fx = Fx;
    Highcharts.inArray = inArray;
    Highcharts.each = each;
    Highcharts.grep = grep;
    Highcharts.offset = offset;
    Highcharts.map = map;
    Highcharts.addEvent = addEvent;
    Highcharts.removeEvent = removeEvent;
    Highcharts.fireEvent = fireEvent;
    Highcharts.animate = animate;
    Highcharts.animObject = animObject;
    Highcharts.stop = stop;

    /* ****************************************************************************
     * Handle the options                                                         *
     *****************************************************************************/
    defaultOptions = {
        colors: ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c',
                '#8085e9', '#f15c80', '#e4d354', '#2b908f', '#f45b5b', '#91e8e1'],
        symbols: ['circle', 'diamond', 'square', 'triangle', 'triangle-down'],
        lang: {
            loading: 'Loading...',
            months: ['January', 'February', 'March', 'April', 'May', 'June', 'July',
                    'August', 'September', 'October', 'November', 'December'],
            shortMonths: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
            weekdays: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
            // invalidDate: '',
            decimalPoint: '.',
            numericSymbols: ['k', 'M', 'G', 'T', 'P', 'E'], // SI prefixes used in axis labels
            resetZoom: 'Reset zoom',
            resetZoomTitle: 'Reset zoom level 1:1',
            thousandsSep: ' '
        },
        global: {
            useUTC: true,
            //timezoneOffset: 0,
            canvasToolsURL: 'http://code.highcharts.com/modules/canvas-tools.js',
            VMLRadialGradientURL: 'http://code.highcharts.com/stock/4.2.5/gfx/vml-radial-gradient.png'
        },
        chart: {
            //animation: true,
            //alignTicks: false,
            //reflow: true,
            //className: null,
            //events: { load, selection },
            //margin: [null],
            //marginTop: null,
            //marginRight: null,
            //marginBottom: null,
            //marginLeft: null,
            borderColor: '#4572A7',
            //borderWidth: 0,
            borderRadius: 0,
            defaultSeriesType: 'line',
            ignoreHiddenSeries: true,
            //inverted: false,
            //shadow: false,
            spacing: [10, 10, 15, 10],
            //spacingTop: 10,
            //spacingRight: 10,
            //spacingBottom: 15,
            //spacingLeft: 10,
            //style: {
            //    fontFamily: '"Lucida Grande", "Lucida Sans Unicode", Verdana, Arial, Helvetica, sans-serif', // default font
            //    fontSize: '12px'
            //},
            backgroundColor: '#FFFFFF',
            //plotBackgroundColor: null,
            plotBorderColor: '#C0C0C0',
            //plotBorderWidth: 0,
            //plotShadow: false,
            //zoomType: ''
            resetZoomButton: {
                theme: {
                    zIndex: 20
                },
                position: {
                    align: 'right',
                    x: -10,
                    //verticalAlign: 'top',
                    y: 10
                }
                // relativeTo: 'plot'
            }
        },
        title: {
            text: 'Chart title',
            align: 'center',
            // floating: false,
            margin: 15,
            // x: 0,
            // verticalAlign: 'top',
            // y: null,
            style: {
                color: '#333333',
                fontSize: '18px'
            },
            widthAdjust: -44

        },
        subtitle: {
            text: '',
            align: 'center',
            // floating: false
            // x: 0,
            // verticalAlign: 'top',
            // y: null,
            style: {
                color: '#555555'
            },
            widthAdjust: -44
        },

        plotOptions: {
            line: { // base series options
                allowPointSelect: false,
                showCheckbox: false,
                animation: {
                    duration: 1000
                },
                //connectNulls: false,
                //cursor: 'default',
                //clip: true,
                //dashStyle: null,
                //enableMouseTracking: true,
                events: {},
                //legendIndex: 0,
                //linecap: 'round',
                lineWidth: 2,
                //shadow: false,
                // stacking: null,
                marker: {
                    //enabled: true,
                    //symbol: null,
                    lineWidth: 0,
                    radius: 4,
                    lineColor: '#FFFFFF',
                    //fillColor: null,
                    states: { // states for a single point
                        hover: {
                            enabled: true,
                            lineWidthPlus: 1,
                            radiusPlus: 2
                        },
                        select: {
                            fillColor: '#FFFFFF',
                            lineColor: '#000000',
                            lineWidth: 2
                        }
                    }
                },
                point: {
                    events: {}
                },
                dataLabels: {
                    align: 'center',
                    // defer: true,
                    // enabled: false,
                    formatter: function () {
                        return this.y === null ? '' : Highcharts.numberFormat(this.y, -1);
                    },
                    style: {
                        color: 'contrast',
                        fontSize: '11px',
                        fontWeight: 'bold',
                        textShadow: '0 0 6px contrast, 0 0 3px contrast'
                    },
                    verticalAlign: 'bottom', // above singular point
                    x: 0,
                    y: 0,
                    // backgroundColor: undefined,
                    // borderColor: undefined,
                    // borderRadius: undefined,
                    // borderWidth: undefined,
                    padding: 5
                    // shadow: false
                },
                cropThreshold: 300, // draw points outside the plot area when the number of points is less than this
                pointRange: 0,
                //pointStart: 0,
                //pointInterval: 1,
                //showInLegend: null, // auto: true for standalone series, false for linked series
                softThreshold: true,
                states: { // states for the entire series
                    hover: {
                        //enabled: false,
                        lineWidthPlus: 1,
                        marker: {
                            // lineWidth: base + 1,
                            // radius: base + 1
                        },
                        halo: {
                            size: 10,
                            opacity: 0.25
                        }
                    },
                    select: {
                        marker: {}
                    }
                },
                stickyTracking: true,
                //tooltip: {
                    //pointFormat: '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>{point.y}</b>'
                    //valueDecimals: null,
                    //xDateFormat: '%A, %b %e, %Y',
                    //valuePrefix: '',
                    //ySuffix: ''
                //}
                turboThreshold: 1000
                // zIndex: null
            }
        },
        labels: {
            //items: [],
            style: {
                //font: defaultFont,
                position: ABSOLUTE,
                color: '#3E576F'
            }
        },
        legend: {
            enabled: true,
            align: 'center',
            //floating: false,
            layout: 'horizontal',
            labelFormatter: function () {
                return this.name;
            },
            //borderWidth: 0,
            borderColor: '#909090',
            borderRadius: 0,
            navigation: {
                // animation: true,
                activeColor: '#274b6d',
                // arrowSize: 12
                inactiveColor: '#CCC'
                // style: {} // text styles
            },
            // margin: 20,
            // reversed: false,
            shadow: false,
            // backgroundColor: null,
            /*style: {
                padding: '5px'
            },*/
            itemStyle: {
                color: '#333333',
                fontSize: '12px',
                fontWeight: 'bold'
            },
            itemHoverStyle: {
                //cursor: 'pointer', removed as of #601
                color: '#000'
            },
            itemHiddenStyle: {
                color: '#CCC'
            },
            itemCheckboxStyle: {
                position: ABSOLUTE,
                width: '13px', // for IE precision
                height: '13px'
            },
            // itemWidth: undefined,
            // symbolRadius: 0,
            // symbolWidth: 16,
            symbolPadding: 5,
            verticalAlign: 'bottom',
            // width: undefined,
            x: 0,
            y: 0,
            title: {
                //text: null,
                style: {
                    fontWeight: 'bold'
                }
            }
        },

        loading: {
            // hideDuration: 100,
            labelStyle: {
                fontWeight: 'bold',
                position: RELATIVE,
                top: '45%'
            },
            // showDuration: 0,
            style: {
                position: ABSOLUTE,
                backgroundColor: 'white',
                opacity: 0.5,
                textAlign: 'center'
            }
        },

        tooltip: {
            enabled: true,
            animation: hasSVG,
            //crosshairs: null,
            backgroundColor: 'rgba(249, 249, 249, .85)',
            borderWidth: 1,
            borderRadius: 3,
            dateTimeLabelFormats: {
                millisecond: '%A, %b %e, %H:%M:%S.%L',
                second: '%A, %b %e, %H:%M:%S',
                minute: '%A, %b %e, %H:%M',
                hour: '%A, %b %e, %H:%M',
                day: '%A, %b %e, %Y',
                week: 'Week from %A, %b %e, %Y',
                month: '%B %Y',
                year: '%Y'
            },
            footerFormat: '',
            //formatter: defaultFormatter,
            headerFormat: '<span style="font-size: 10px">{point.key}</span><br/>',
            pointFormat: '<span style="color:{point.color}">\u25CF</span> {series.name}: <b>{point.y}</b><br/>',
            shadow: true,
            //shape: 'callout',
            //shared: false,
            snap: isTouchDevice ? 25 : 10,
            style: {
                color: '#333333',
                cursor: 'default',
                fontSize: '12px',
                padding: '8px',
                pointerEvents: 'none', // #1686 http://caniuse.com/#feat=pointer-events
                whiteSpace: 'nowrap'
            }
            //xDateFormat: '%A, %b %e, %Y',
            //valueDecimals: null,
            //valuePrefix: '',
            //valueSuffix: ''
        },

        credits: {
            enabled: true,
            text: 'Highcharts.com',
            href: 'http://www.highcharts.com',
            position: {
                align: 'right',
                x: -10,
                verticalAlign: 'bottom',
                y: -5
            },
            style: {
                cursor: 'pointer',
                color: '#909090',
                fontSize: '9px'
            }
        }
    };



    /**
     * Set the time methods globally based on the useUTC option. Time method can be either
     * local time or UTC (default).
     */
    function setTimeMethods() {
        var globalOptions = defaultOptions.global,
            useUTC = globalOptions.useUTC,
            GET = useUTC ? 'getUTC' : 'get',
            SET = useUTC ? 'setUTC' : 'set';


        Date = globalOptions.Date || win.Date;
        timezoneOffset = useUTC && globalOptions.timezoneOffset;
        getTimezoneOffset = useUTC && globalOptions.getTimezoneOffset;
        makeTime = function (year, month, date, hours, minutes, seconds) {
            var d;
            if (useUTC) {
                d = Date.UTC.apply(0, arguments);
                d += getTZOffset(d);
            } else {
                d = new Date(
                    year,
                    month,
                    pick(date, 1),
                    pick(hours, 0),
                    pick(minutes, 0),
                    pick(seconds, 0)
                ).getTime();
            }
            return d;
        };
        getMinutes =      GET + 'Minutes';
        getHours =        GET + 'Hours';
        getDay =          GET + 'Day';
        getDate =         GET + 'Date';
        getMonth =        GET + 'Month';
        getFullYear =     GET + 'FullYear';
        setMilliseconds = SET + 'Milliseconds';
        setSeconds =      SET + 'Seconds';
        setMinutes =      SET + 'Minutes';
        setHours =        SET + 'Hours';
        setDate =         SET + 'Date';
        setMonth =        SET + 'Month';
        setFullYear =     SET + 'FullYear';

    }

    /**
     * Merge the default options with custom options and return the new options structure
     * @param {Object} options The new custom options
     */
    function setOptions(options) {

        // Copy in the default options
        defaultOptions = merge(true, defaultOptions, options);

        // Apply UTC
        setTimeMethods();

        return defaultOptions;
    }

    /**
     * Get the updated default options. Until 3.0.7, merely exposing defaultOptions for outside modules
     * wasn't enough because the setOptions method created a new object.
     */
    function getOptions() {
        return defaultOptions;
    }






    // Series defaults
    var defaultPlotOptions = defaultOptions.plotOptions,
        defaultSeriesOptions = defaultPlotOptions.line;

    // set the default time methods
    setTimeMethods();


    /**
     * Handle color operations. The object methods are chainable.
     * @param {String} input The input color in either rbga or hex format
     */
    function Color(input) {
        // Backwards compatibility, allow instanciation without new
        if (!(this instanceof Color)) {
            return new Color(input);
        }
        // Initialize
        this.init(input);
    }
    Color.prototype = {

        // Collection of parsers. This can be extended from the outside by pushing parsers
        // to Highcharts.Colors.prototype.parsers.
        parsers: [{
            // RGBA color
            regex: /rgba\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]?(?:\.[0-9]+)?)\s*\)/,
            parse: function (result) {
                return [pInt(result[1]), pInt(result[2]), pInt(result[3]), parseFloat(result[4], 10)];
            }
        }, {
            // HEX color
            regex: /#([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})/,
            parse: function (result) {
                return [pInt(result[1], 16), pInt(result[2], 16), pInt(result[3], 16), 1];
            }
        }, {
            // RGB color
            regex: /rgb\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*\)/,
            parse: function (result) {
                return [pInt(result[1]), pInt(result[2]), pInt(result[3]), 1];
            }
        }],

        /**
         * Parse the input color to rgba array
         * @param {String} input
         */
        init: function (input) {
            var result,
                rgba,
                i,
                parser;

            this.input = input;

            // Gradients
            if (input && input.stops) {
                this.stops = map(input.stops, function (stop) {
                    return new Color(stop[1]);
                });

            // Solid colors
            } else {
                i = this.parsers.length;
                while (i-- && !rgba) {
                    parser = this.parsers[i];
                    result = parser.regex.exec(input);
                    if (result) {
                        rgba = parser.parse(result);
                    }
                }
            }
            this.rgba = rgba || [];
        },

        /**
         * Return the color a specified format
         * @param {String} format
         */
        get: function (format) {
            var input = this.input,
                rgba = this.rgba,
                ret;

            if (this.stops) {
                ret = merge(input);
                ret.stops = [].concat(ret.stops);
                each(this.stops, function (stop, i) {
                    ret.stops[i] = [ret.stops[i][0], stop.get(format)];
                });

            // it's NaN if gradient colors on a column chart
            } else if (rgba && isNumber(rgba[0])) {
                if (format === 'rgb' || (!format && rgba[3] === 1)) {
                    ret = 'rgb(' + rgba[0] + ',' + rgba[1] + ',' + rgba[2] + ')';
                } else if (format === 'a') {
                    ret = rgba[3];
                } else {
                    ret = 'rgba(' + rgba.join(',') + ')';
                }
            } else {
                ret = input;
            }
            return ret;
        },

        /**
         * Brighten the color
         * @param {Number} alpha
         */
        brighten: function (alpha) {
            var i, 
                rgba = this.rgba;

            if (this.stops) {
                each(this.stops, function (stop) {
                    stop.brighten(alpha);
                });

            } else if (isNumber(alpha) && alpha !== 0) {
                for (i = 0; i < 3; i++) {
                    rgba[i] += pInt(alpha * 255);

                    if (rgba[i] < 0) {
                        rgba[i] = 0;
                    }
                    if (rgba[i] > 255) {
                        rgba[i] = 255;
                    }
                }
            }
            return this;
        },

        /**
         * Set the color's opacity to a given alpha value
         * @param {Number} alpha
         */
        setOpacity: function (alpha) {
            this.rgba[3] = alpha;
            return this;
        }
    };


    /**
     * A wrapper object for SVG elements
     */
    function SVGElement() {}

    SVGElement.prototype = {

        // Default base for animation
        opacity: 1,
        // For labels, these CSS properties are applied to the <text> node directly
        textProps: ['direction', 'fontSize', 'fontWeight', 'fontFamily', 'fontStyle', 'color',
            'lineHeight', 'width', 'textDecoration', 'textOverflow', 'textShadow'],

        /**
         * Initialize the SVG renderer
         * @param {Object} renderer
         * @param {String} nodeName
         */
        init: function (renderer, nodeName) {
            var wrapper = this;
            wrapper.element = nodeName === 'span' ?
                    createElement(nodeName) :
                    doc.createElementNS(SVG_NS, nodeName);
            wrapper.renderer = renderer;
        },

        /**
         * Animate a given attribute
         * @param {Object} params
         * @param {Number} options Options include duration, easing, step and complete
         * @param {Function} complete Function to perform at the end of animation
         */
        animate: function (params, options, complete) {
            var animOptions = pick(options, this.renderer.globalAnimation, true);
            stop(this); // stop regardless of animation actually running, or reverting to .attr (#607)
            if (animOptions) {
                if (complete) { // allows using a callback with the global animation without overwriting it
                    animOptions.complete = complete;
                }
                animate(this, params, animOptions);
            } else {
                this.attr(params, null, complete);
            }
            return this;
        },

        /**
         * Build an SVG gradient out of a common JavaScript configuration object
         */
        colorGradient: function (color, prop, elem) {
            var renderer = this.renderer,
                colorObject,
                gradName,
                gradAttr,
                radAttr,
                gradients,
                gradientObject,
                stops,
                stopColor,
                stopOpacity,
                radialReference,
                n,
                id,
                key = [],
                value;

            // Apply linear or radial gradients
            if (color.linearGradient) {
                gradName = 'linearGradient';
            } else if (color.radialGradient) {
                gradName = 'radialGradient';
            }

            if (gradName) {
                gradAttr = color[gradName];
                gradients = renderer.gradients;
                stops = color.stops;
                radialReference = elem.radialReference;

                // Keep < 2.2 kompatibility
                if (isArray(gradAttr)) {
                    color[gradName] = gradAttr = {
                        x1: gradAttr[0],
                        y1: gradAttr[1],
                        x2: gradAttr[2],
                        y2: gradAttr[3],
                        gradientUnits: 'userSpaceOnUse'
                    };
                }

                // Correct the radial gradient for the radial reference system
                if (gradName === 'radialGradient' && radialReference && !defined(gradAttr.gradientUnits)) {
                    radAttr = gradAttr; // Save the radial attributes for updating
                    gradAttr = merge(gradAttr,
                        renderer.getRadialAttr(radialReference, radAttr),
                        { gradientUnits: 'userSpaceOnUse' }
                        );
                }

                // Build the unique key to detect whether we need to create a new element (#1282)
                for (n in gradAttr) {
                    if (n !== 'id') {
                        key.push(n, gradAttr[n]);
                    }
                }
                for (n in stops) {
                    key.push(stops[n]);
                }
                key = key.join(',');

                // Check if a gradient object with the same config object is created within this renderer
                if (gradients[key]) {
                    id = gradients[key].attr('id');

                } else {

                    // Set the id and create the element
                    gradAttr.id = id = PREFIX + idCounter++;
                    gradients[key] = gradientObject = renderer.createElement(gradName)
                        .attr(gradAttr)
                        .add(renderer.defs);

                    gradientObject.radAttr = radAttr;

                    // The gradient needs to keep a list of stops to be able to destroy them
                    gradientObject.stops = [];
                    each(stops, function (stop) {
                        var stopObject;
                        if (stop[1].indexOf('rgba') === 0) {
                            colorObject = Color(stop[1]);
                            stopColor = colorObject.get('rgb');
                            stopOpacity = colorObject.get('a');
                        } else {
                            stopColor = stop[1];
                            stopOpacity = 1;
                        }
                        stopObject = renderer.createElement('stop').attr({
                            offset: stop[0],
                            'stop-color': stopColor,
                            'stop-opacity': stopOpacity
                        }).add(gradientObject);

                        // Add the stop element to the gradient
                        gradientObject.stops.push(stopObject);
                    });
                }

                // Set the reference to the gradient object
                value = 'url(' + renderer.url + '#' + id + ')';
                elem.setAttribute(prop, value);
                elem.gradient = key;

                // Allow the color to be concatenated into tooltips formatters etc. (#2995)
                color.toString = function () {
                    return value;
                };
            }
        },

        /**
         * Apply a polyfill to the text-stroke CSS property, by copying the text element
         * and apply strokes to the copy.
         *
         * Contrast checks at http://jsfiddle.net/highcharts/43soe9m1/2/
         */
        applyTextShadow: function (textShadow) {
            var elem = this.element,
                tspans,
                hasContrast = textShadow.indexOf('contrast') !== -1,
                styles = {},
                forExport = this.renderer.forExport,
                // IE10 and IE11 report textShadow in elem.style even though it doesn't work. Check
                // this again with new IE release. In exports, the rendering is passed to PhantomJS.
                supports = forExport || (elem.style.textShadow !== UNDEFINED && !isMS);

            // When the text shadow is set to contrast, use dark stroke for light text and vice versa
            if (hasContrast) {
                styles.textShadow = textShadow = textShadow.replace(/contrast/g, this.renderer.getContrast(elem.style.fill));
            }

            // Safari with retina displays as well as PhantomJS bug (#3974). Firefox does not tolerate this,
            // it removes the text shadows.
            if (isWebKit || forExport) {
                styles.textRendering = 'geometricPrecision';
            }

            /* Selective side-by-side testing in supported browser (http://jsfiddle.net/highcharts/73L1ptrh/)
            if (elem.textContent.indexOf('2.') === 0) {
                elem.style['text-shadow'] = 'none';
                supports = false;
            }
            // */

            // No reason to polyfill, we've got native support
            if (supports) {
                this.css(styles); // Apply altered textShadow or textRendering workaround
            } else {

                this.fakeTS = true; // Fake text shadow

                // In order to get the right y position of the clones,
                // copy over the y setter
                this.ySetter = this.xSetter;

                tspans = [].slice.call(elem.getElementsByTagName('tspan'));
                each(textShadow.split(/\s?,\s?/g), function (textShadow) {
                    var firstChild = elem.firstChild,
                        color,
                        strokeWidth;

                    textShadow = textShadow.split(' ');
                    color = textShadow[textShadow.length - 1];

                    // Approximately tune the settings to the text-shadow behaviour
                    strokeWidth = textShadow[textShadow.length - 2];

                    if (strokeWidth) {
                        each(tspans, function (tspan, y) {
                            var clone;

                            // Let the first line start at the correct X position
                            if (y === 0) {
                                tspan.setAttribute('x', elem.getAttribute('x'));
                                y = elem.getAttribute('y');
                                tspan.setAttribute('y', y || 0);
                                if (y === null) {
                                    elem.setAttribute('y', 0);
                                }
                            }

                            // Create the clone and apply shadow properties
                            clone = tspan.cloneNode(1);
                            attr(clone, {
                                'class': PREFIX + 'text-shadow',
                                'fill': color,
                                'stroke': color,
                                'stroke-opacity': 1 / mathMax(pInt(strokeWidth), 3),
                                'stroke-width': strokeWidth,
                                'stroke-linejoin': 'round'
                            });
                            elem.insertBefore(clone, firstChild);
                        });
                    }
                });
            }
        },

        /**
         * Set or get a given attribute
         * @param {Object|String} hash
         * @param {Mixed|Undefined} val
         */
        attr: function (hash, val, complete) {
            var key,
                value,
                element = this.element,
                hasSetSymbolSize,
                ret = this,
                skipAttr,
                setter;

            // single key-value pair
            if (typeof hash === 'string' && val !== UNDEFINED) {
                key = hash;
                hash = {};
                hash[key] = val;
            }

            // used as a getter: first argument is a string, second is undefined
            if (typeof hash === 'string') {
                ret = (this[hash + 'Getter'] || this._defaultGetter).call(this, hash, element);

            // setter
            } else {

                for (key in hash) {
                    value = hash[key];
                    skipAttr = false;



                    if (this.symbolName && /^(x|y|width|height|r|start|end|innerR|anchorX|anchorY)/.test(key)) {
                        if (!hasSetSymbolSize) {
                            this.symbolAttr(hash);
                            hasSetSymbolSize = true;
                        }
                        skipAttr = true;
                    }

                    if (this.rotation && (key === 'x' || key === 'y')) {
                        this.doTransform = true;
                    }

                    if (!skipAttr) {
                        setter = this[key + 'Setter'] || this._defaultSetter;
                        setter.call(this, value, key, element);

                        // Let the shadow follow the main element
                        if (this.shadows && /^(width|height|visibility|x|y|d|transform|cx|cy|r)$/.test(key)) {
                            this.updateShadows(key, value, setter);
                        }
                    }
                }

                // Update transform. Do this outside the loop to prevent redundant updating for batch setting
                // of attributes.
                if (this.doTransform) {
                    this.updateTransform();
                    this.doTransform = false;
                }

            }

            // In accordance with animate, run a complete callback
            if (complete) {
                complete();
            }

            return ret;
        },

        /**
         * Update the shadow elements with new attributes
         * @param   {String}        key    The attribute name
         * @param   {String|Number} value  The value of the attribute
         * @param   {Function}      setter The setter function, inherited from the parent wrapper
         * @returns {undefined}
         */
        updateShadows: function (key, value, setter) {
            var shadows = this.shadows,
                i = shadows.length;

            while (i--) {
                setter.call(
                    shadows[i], 
                    key === 'height' ?
                        Math.max(value - (shadows[i].cutHeight || 0), 0) :
                        key === 'd' ? this.d : value, 
                    key, 
                    shadows[i]
                );
            }
        },

        /**
         * Add a class name to an element
         */
        addClass: function (className) {
            var element = this.element,
                currentClassName = attr(element, 'class') || '';

            if (currentClassName.indexOf(className) === -1) {
                attr(element, 'class', currentClassName + ' ' + className);
            }
            return this;
        },
        /* hasClass and removeClass are not (yet) needed
        hasClass: function (className) {
            return attr(this.element, 'class').indexOf(className) !== -1;
        },
        removeClass: function (className) {
            attr(this.element, 'class', attr(this.element, 'class').replace(className, ''));
            return this;
        },
        */

        /**
         * If one of the symbol size affecting parameters are changed,
         * check all the others only once for each call to an element's
         * .attr() method
         * @param {Object} hash
         */
        symbolAttr: function (hash) {
            var wrapper = this;

            each(['x', 'y', 'r', 'start', 'end', 'width', 'height', 'innerR', 'anchorX', 'anchorY'], function (key) {
                wrapper[key] = pick(hash[key], wrapper[key]);
            });

            wrapper.attr({
                d: wrapper.renderer.symbols[wrapper.symbolName](
                    wrapper.x,
                    wrapper.y,
                    wrapper.width,
                    wrapper.height,
                    wrapper
                )
            });
        },

        /**
         * Apply a clipping path to this object
         * @param {String} id
         */
        clip: function (clipRect) {
            return this.attr('clip-path', clipRect ? 'url(' + this.renderer.url + '#' + clipRect.id + ')' : NONE);
        },

        /**
         * Calculate the coordinates needed for drawing a rectangle crisply and return the
         * calculated attributes
         * @param {Number} strokeWidth
         * @param {Number} x
         * @param {Number} y
         * @param {Number} width
         * @param {Number} height
         */
        crisp: function (rect) {

            var wrapper = this,
                key,
                attribs = {},
                normalizer,
                strokeWidth = wrapper.strokeWidth || 0;

            normalizer = mathRound(strokeWidth) % 2 / 2; // mathRound because strokeWidth can sometimes have roundoff errors

            // normalize for crisp edges
            rect.x = mathFloor(rect.x || wrapper.x || 0) + normalizer;
            rect.y = mathFloor(rect.y || wrapper.y || 0) + normalizer;
            rect.width = mathFloor((rect.width || wrapper.width || 0) - 2 * normalizer);
            rect.height = mathFloor((rect.height || wrapper.height || 0) - 2 * normalizer);
            rect.strokeWidth = strokeWidth;

            for (key in rect) {
                if (wrapper[key] !== rect[key]) { // only set attribute if changed
                    wrapper[key] = attribs[key] = rect[key];
                }
            }

            return attribs;
        },

        /**
         * Set styles for the element
         * @param {Object} styles
         */
        css: function (styles) {
            var elemWrapper = this,
                oldStyles = elemWrapper.styles,
                newStyles = {},
                elem = elemWrapper.element,
                textWidth,
                n,
                serializedCss = '',
                hyphenate,
                hasNew = !oldStyles;

            // convert legacy
            if (styles && styles.color) {
                styles.fill = styles.color;
            }

            // Filter out existing styles to increase performance (#2640)
            if (oldStyles) {
                for (n in styles) {
                    if (styles[n] !== oldStyles[n]) {
                        newStyles[n] = styles[n];
                        hasNew = true;
                    }
                }
            }
            if (hasNew) {
                textWidth = elemWrapper.textWidth =
                    (styles && styles.width && elem.nodeName.toLowerCase() === 'text' && pInt(styles.width)) ||
                    elemWrapper.textWidth; // #3501

                // Merge the new styles with the old ones
                if (oldStyles) {
                    styles = extend(
                        oldStyles,
                        newStyles
                    );
                }

                // store object
                elemWrapper.styles = styles;

                if (textWidth && (useCanVG || (!hasSVG && elemWrapper.renderer.forExport))) {
                    delete styles.width;
                }

                // serialize and set style attribute
                if (isMS && !hasSVG) {
                    css(elemWrapper.element, styles);
                } else {
                    hyphenate = function (a, b) {
                        return '-' + b.toLowerCase();
                    };
                    for (n in styles) {
                        serializedCss += n.replace(/([A-Z])/g, hyphenate) + ':' + styles[n] + ';';
                    }
                    attr(elem, 'style', serializedCss); // #1881
                }


                // re-build text
                if (textWidth && elemWrapper.added) {
                    elemWrapper.renderer.buildText(elemWrapper);
                }
            }

            return elemWrapper;
        },

        /**
         * Add an event listener
         * @param {String} eventType
         * @param {Function} handler
         */
        on: function (eventType, handler) {
            var svgElement = this,
                element = svgElement.element;

            // touch
            if (hasTouch && eventType === 'click') {
                element.ontouchstart = function (e) {
                    svgElement.touchEventFired = Date.now();
                    e.preventDefault();
                    handler.call(element, e);
                };
                element.onclick = function (e) {
                    if (userAgent.indexOf('Android') === -1 || Date.now() - (svgElement.touchEventFired || 0) > 1100) { // #2269
                        handler.call(element, e);
                    }
                };
            } else {
                // simplest possible event model for internal use
                element['on' + eventType] = handler;
            }
            return this;
        },

        /**
         * Set the coordinates needed to draw a consistent radial gradient across
         * pie slices regardless of positioning inside the chart. The format is
         * [centerX, centerY, diameter] in pixels.
         */
        setRadialReference: function (coordinates) {
            var existingGradient = this.renderer.gradients[this.element.gradient];

            this.element.radialReference = coordinates;

            // On redrawing objects with an existing gradient, the gradient needs
            // to be repositioned (#3801)
            if (existingGradient && existingGradient.radAttr) {
                existingGradient.animate(
                    this.renderer.getRadialAttr(
                        coordinates,
                        existingGradient.radAttr
                    )
                );
            }

            return this;
        },

        /**
         * Move an object and its children by x and y values
         * @param {Number} x
         * @param {Number} y
         */
        translate: function (x, y) {
            return this.attr({
                translateX: x,
                translateY: y
            });
        },

        /**
         * Invert a group, rotate and flip
         */
        invert: function () {
            var wrapper = this;
            wrapper.inverted = true;
            wrapper.updateTransform();
            return wrapper;
        },

        /**
         * Private method to update the transform attribute based on internal
         * properties
         */
        updateTransform: function () {
            var wrapper = this,
                translateX = wrapper.translateX || 0,
                translateY = wrapper.translateY || 0,
                scaleX = wrapper.scaleX,
                scaleY = wrapper.scaleY,
                inverted = wrapper.inverted,
                rotation = wrapper.rotation,
                element = wrapper.element,
                transform;

            // flipping affects translate as adjustment for flipping around the group's axis
            if (inverted) {
                translateX += wrapper.attr('width');
                translateY += wrapper.attr('height');
            }

            // Apply translate. Nearly all transformed elements have translation, so instead
            // of checking for translate = 0, do it always (#1767, #1846).
            transform = ['translate(' + translateX + ',' + translateY + ')'];

            // apply rotation
            if (inverted) {
                transform.push('rotate(90) scale(-1,1)');
            } else if (rotation) { // text rotation
                transform.push('rotate(' + rotation + ' ' + (element.getAttribute('x') || 0) + ' ' + (element.getAttribute('y') || 0) + ')');

                // Delete bBox memo when the rotation changes
                //delete wrapper.bBox;
            }

            // apply scale
            if (defined(scaleX) || defined(scaleY)) {
                transform.push('scale(' + pick(scaleX, 1) + ' ' + pick(scaleY, 1) + ')');
            }

            if (transform.length) {
                element.setAttribute('transform', transform.join(' '));
            }
        },
        /**
         * Bring the element to the front
         */
        toFront: function () {
            var element = this.element;
            element.parentNode.appendChild(element);
            return this;
        },


        /**
         * Break down alignment options like align, verticalAlign, x and y
         * to x and y relative to the chart.
         *
         * @param {Object} alignOptions
         * @param {Boolean} alignByTranslate
         * @param {String[Object} box The box to align to, needs a width and height. When the
         *        box is a string, it refers to an object in the Renderer. For example, when
         *        box is 'spacingBox', it refers to Renderer.spacingBox which holds width, height
         *        x and y properties.
         *
         */
        align: function (alignOptions, alignByTranslate, box) {
            var align,
                vAlign,
                x,
                y,
                attribs = {},
                alignTo,
                renderer = this.renderer,
                alignedObjects = renderer.alignedObjects;

            // First call on instanciate
            if (alignOptions) {
                this.alignOptions = alignOptions;
                this.alignByTranslate = alignByTranslate;
                if (!box || isString(box)) { // boxes other than renderer handle this internally
                    this.alignTo = alignTo = box || 'renderer';
                    erase(alignedObjects, this); // prevent duplicates, like legendGroup after resize
                    alignedObjects.push(this);
                    box = null; // reassign it below
                }

            // When called on resize, no arguments are supplied
            } else {
                alignOptions = this.alignOptions;
                alignByTranslate = this.alignByTranslate;
                alignTo = this.alignTo;
            }

            box = pick(box, renderer[alignTo], renderer);

            // Assign variables
            align = alignOptions.align;
            vAlign = alignOptions.verticalAlign;
            x = (box.x || 0) + (alignOptions.x || 0); // default: left align
            y = (box.y || 0) + (alignOptions.y || 0); // default: top align

            // Align
            if (align === 'right' || align === 'center') {
                x += (box.width - (alignOptions.width || 0)) /
                        { right: 1, center: 2 }[align];
            }
            attribs[alignByTranslate ? 'translateX' : 'x'] = mathRound(x);


            // Vertical align
            if (vAlign === 'bottom' || vAlign === 'middle') {
                y += (box.height - (alignOptions.height || 0)) /
                        ({ bottom: 1, middle: 2 }[vAlign] || 1);

            }
            attribs[alignByTranslate ? 'translateY' : 'y'] = mathRound(y);

            // Animate only if already placed
            this[this.placed ? 'animate' : 'attr'](attribs);
            this.placed = true;
            this.alignAttr = attribs;

            return this;
        },

        /**
         * Get the bounding box (width, height, x and y) for the element
         */
        getBBox: function (reload, rot) {
            var wrapper = this,
                bBox, // = wrapper.bBox,
                renderer = wrapper.renderer,
                width,
                height,
                rotation,
                rad,
                element = wrapper.element,
                styles = wrapper.styles,
                textStr = wrapper.textStr,
                textShadow,
                elemStyle = element.style,
                toggleTextShadowShim,
                cache = renderer.cache,
                cacheKeys = renderer.cacheKeys,
                cacheKey;

            rotation = pick(rot, wrapper.rotation);
            rad = rotation * deg2rad;

            if (textStr !== UNDEFINED) {

                // Properties that affect bounding box
                cacheKey = ['', rotation || 0, styles && styles.fontSize, element.style.width].join(',');

                // Since numbers are monospaced, and numerical labels appear a lot in a chart,
                // we assume that a label of n characters has the same bounding box as others
                // of the same length.
                if (textStr === '' || numRegex.test(textStr)) {
                    cacheKey = 'num:' + textStr.toString().length + cacheKey;

                // Caching all strings reduces rendering time by 4-5%.
                } else {
                    cacheKey = textStr + cacheKey;
                }
            }

            if (cacheKey && !reload) {
                bBox = cache[cacheKey];
            }

            // No cache found
            if (!bBox) {

                // SVG elements
                if (element.namespaceURI === SVG_NS || renderer.forExport) {
                    try { // Fails in Firefox if the container has display: none.

                        // When the text shadow shim is used, we need to hide the fake shadows
                        // to get the correct bounding box (#3872)
                        toggleTextShadowShim = this.fakeTS && function (display) {
                            each(element.querySelectorAll('.' + PREFIX + 'text-shadow'), function (tspan) {
                                tspan.style.display = display;
                            });
                        };

                        // Workaround for #3842, Firefox reporting wrong bounding box for shadows
                        if (isFirefox && elemStyle.textShadow) {
                            textShadow = elemStyle.textShadow;
                            elemStyle.textShadow = '';
                        } else if (toggleTextShadowShim) {
                            toggleTextShadowShim(NONE);
                        }

                        bBox = element.getBBox ?
                            // SVG: use extend because IE9 is not allowed to change width and height in case
                            // of rotation (below)
                            extend({}, element.getBBox()) :
                            // Canvas renderer and legacy IE in export mode
                            {
                                width: element.offsetWidth,
                                height: element.offsetHeight
                            };

                        // #3842
                        if (textShadow) {
                            elemStyle.textShadow = textShadow;
                        } else if (toggleTextShadowShim) {
                            toggleTextShadowShim('');
                        }
                    } catch (e) {}

                    // If the bBox is not set, the try-catch block above failed. The other condition
                    // is for Opera that returns a width of -Infinity on hidden elements.
                    if (!bBox || bBox.width < 0) {
                        bBox = { width: 0, height: 0 };
                    }


                // VML Renderer or useHTML within SVG
                } else {

                    bBox = wrapper.htmlGetBBox();

                }

                // True SVG elements as well as HTML elements in modern browsers using the .useHTML option
                // need to compensated for rotation
                if (renderer.isSVG) {
                    width = bBox.width;
                    height = bBox.height;

                    // Workaround for wrong bounding box in IE9 and IE10 (#1101, #1505, #1669, #2568)
                    if (isMS && styles && styles.fontSize === '11px' && height.toPrecision(3) === '16.9') {
                        bBox.height = height = 14;
                    }

                    // Adjust for rotated text
                    if (rotation) {
                        bBox.width = mathAbs(height * mathSin(rad)) + mathAbs(width * mathCos(rad));
                        bBox.height = mathAbs(height * mathCos(rad)) + mathAbs(width * mathSin(rad));
                    }
                }

                // Cache it
                if (cacheKey) {

                    // Rotate (#4681)
                    while (cacheKeys.length > 250) {
                        delete cache[cacheKeys.shift()];
                    }

                    if (!cache[cacheKey]) {
                        cacheKeys.push(cacheKey);
                    }
                    cache[cacheKey] = bBox;
                }
            }
            return bBox;
        },

        /**
         * Show the element
         */
        show: function (inherit) {
            return this.attr({ visibility: inherit ? 'inherit' : VISIBLE });
        },

        /**
         * Hide the element
         */
        hide: function () {
            return this.attr({ visibility: HIDDEN });
        },

        fadeOut: function (duration) {
            var elemWrapper = this;
            elemWrapper.animate({
                opacity: 0
            }, {
                duration: duration || 150,
                complete: function () {
                    elemWrapper.attr({ y: -9999 }); // #3088, assuming we're only using this for tooltips
                }
            });
        },

        /**
         * Add the element
         * @param {Object|Undefined} parent Can be an element, an element wrapper or undefined
         *    to append the element to the renderer.box.
         */
        add: function (parent) {

            var renderer = this.renderer,
                element = this.element,
                inserted;

            if (parent) {
                this.parentGroup = parent;
            }

            // mark as inverted
            this.parentInverted = parent && parent.inverted;

            // build formatted text
            if (this.textStr !== undefined) {
                renderer.buildText(this);
            }

            // Mark as added
            this.added = true;

            // If we're adding to renderer root, or other elements in the group
            // have a z index, we need to handle it
            if (!parent || parent.handleZ || this.zIndex) {
                inserted = this.zIndexSetter();
            }

            // If zIndex is not handled, append at the end
            if (!inserted) {
                (parent ? parent.element : renderer.box).appendChild(element);
            }

            // fire an event for internal hooks
            if (this.onAdd) {
                this.onAdd();
            }

            return this;
        },

        /**
         * Removes a child either by removeChild or move to garbageBin.
         * Issue 490; in VML removeChild results in Orphaned nodes according to sIEve, discardElement does not.
         */
        safeRemoveChild: function (element) {
            var parentNode = element.parentNode;
            if (parentNode) {
                parentNode.removeChild(element);
            }
        },

        /**
         * Destroy the element and element wrapper
         */
        destroy: function () {
            var wrapper = this,
                element = wrapper.element || {},
                shadows = wrapper.shadows,
                parentToClean = wrapper.renderer.isSVG && element.nodeName === 'SPAN' && wrapper.parentGroup,
                grandParent,
                key,
                i;

            // remove events
            element.onclick = element.onmouseout = element.onmouseover = element.onmousemove = element.point = null;
            stop(wrapper); // stop running animations

            if (wrapper.clipPath) {
                wrapper.clipPath = wrapper.clipPath.destroy();
            }

            // Destroy stops in case this is a gradient object
            if (wrapper.stops) {
                for (i = 0; i < wrapper.stops.length; i++) {
                    wrapper.stops[i] = wrapper.stops[i].destroy();
                }
                wrapper.stops = null;
            }

            // remove element
            wrapper.safeRemoveChild(element);

            // destroy shadows
            if (shadows) {
                each(shadows, function (shadow) {
                    wrapper.safeRemoveChild(shadow);
                });
            }

            // In case of useHTML, clean up empty containers emulating SVG groups (#1960, #2393, #2697).
            while (parentToClean && parentToClean.div && parentToClean.div.childNodes.length === 0) {
                grandParent = parentToClean.parentGroup;
                wrapper.safeRemoveChild(parentToClean.div);
                delete parentToClean.div;
                parentToClean = grandParent;
            }

            // remove from alignObjects
            if (wrapper.alignTo) {
                erase(wrapper.renderer.alignedObjects, wrapper);
            }

            for (key in wrapper) {
                delete wrapper[key];
            }

            return null;
        },

        /**
         * Add a shadow to the element. Must be done after the element is added to the DOM
         * @param {Boolean|Object} shadowOptions
         */
        shadow: function (shadowOptions, group, cutOff) {
            var shadows = [],
                i,
                shadow,
                element = this.element,
                strokeWidth,
                shadowWidth,
                shadowElementOpacity,

                // compensate for inverted plot area
                transform;


            if (shadowOptions) {
                shadowWidth = pick(shadowOptions.width, 3);
                shadowElementOpacity = (shadowOptions.opacity || 0.15) / shadowWidth;
                transform = this.parentInverted ?
                        '(-1,-1)' :
                        '(' + pick(shadowOptions.offsetX, 1) + ', ' + pick(shadowOptions.offsetY, 1) + ')';
                for (i = 1; i <= shadowWidth; i++) {
                    shadow = element.cloneNode(0);
                    strokeWidth = (shadowWidth * 2) + 1 - (2 * i);
                    attr(shadow, {
                        'isShadow': 'true',
                        'stroke': shadowOptions.color || 'black',
                        'stroke-opacity': shadowElementOpacity * i,
                        'stroke-width': strokeWidth,
                        'transform': 'translate' + transform,
                        'fill': NONE
                    });
                    if (cutOff) {
                        attr(shadow, 'height', mathMax(attr(shadow, 'height') - strokeWidth, 0));
                        shadow.cutHeight = strokeWidth;
                    }

                    if (group) {
                        group.element.appendChild(shadow);
                    } else {
                        element.parentNode.insertBefore(shadow, element);
                    }

                    shadows.push(shadow);
                }

                this.shadows = shadows;
            }
            return this;

        },

        xGetter: function (key) {
            if (this.element.nodeName === 'circle') {
                key = { x: 'cx', y: 'cy' }[key] || key;
            }
            return this._defaultGetter(key);
        },

        /**
         * Get the current value of an attribute or pseudo attribute, used mainly
         * for animation.
         */
        _defaultGetter: function (key) {
            var ret = pick(this[key], this.element ? this.element.getAttribute(key) : null, 0);

            if (/^[\-0-9\.]+$/.test(ret)) { // is numerical
                ret = parseFloat(ret);
            }
            return ret;
        },


        dSetter: function (value, key, element) {
            if (value && value.join) { // join path
                value = value.join(' ');
            }
            if (/(NaN| {2}|^$)/.test(value)) {
                value = 'M 0 0';
            }
            element.setAttribute(key, value);

            this[key] = value;
        },
        dashstyleSetter: function (value) {
            var i,
                strokeWidth = this['stroke-width'];
        
            // If "inherit", like maps in IE, assume 1 (#4981). With HC5 and the new strokeWidth 
            // function, we should be able to use that instead.
            if (strokeWidth === 'inherit') {
                strokeWidth = 1;
            }
            value = value && value.toLowerCase();
            if (value) {
                value = value
                    .replace('shortdashdotdot', '3,1,1,1,1,1,')
                    .replace('shortdashdot', '3,1,1,1')
                    .replace('shortdot', '1,1,')
                    .replace('shortdash', '3,1,')
                    .replace('longdash', '8,3,')
                    .replace(/dot/g, '1,3,')
                    .replace('dash', '4,3,')
                    .replace(/,$/, '')
                    .split(','); // ending comma

                i = value.length;
                while (i--) {
                    value[i] = pInt(value[i]) * strokeWidth;
                }
                value = value.join(',')
                    .replace(/NaN/g, 'none'); // #3226
                this.element.setAttribute('stroke-dasharray', value);
            }
        },
        alignSetter: function (value) {
            this.element.setAttribute('text-anchor', { left: 'start', center: 'middle', right: 'end' }[value]);
        },
        opacitySetter: function (value, key, element) {
            this[key] = value;
            element.setAttribute(key, value);
        },
        titleSetter: function (value) {
            var titleNode = this.element.getElementsByTagName('title')[0];
            if (!titleNode) {
                titleNode = doc.createElementNS(SVG_NS, 'title');
                this.element.appendChild(titleNode);
            }

            // Remove text content if it exists
            if (titleNode.firstChild) {
                titleNode.removeChild(titleNode.firstChild);
            }

            titleNode.appendChild(
                doc.createTextNode(
                    (String(pick(value), '')).replace(/<[^>]*>/g, '') // #3276, #3895
                )
            );
        },
        textSetter: function (value) {
            if (value !== this.textStr) {
                // Delete bBox memo when the text changes
                delete this.bBox;

                this.textStr = value;
                if (this.added) {
                    this.renderer.buildText(this);
                }
            }
        },
        fillSetter: function (value, key, element) {
            if (typeof value === 'string') {
                element.setAttribute(key, value);
            } else if (value) {
                this.colorGradient(value, key, element);
            }
        },
        visibilitySetter: function (value, key, element) {
            // IE9-11 doesn't handle visibilty:inherit well, so we remove the attribute instead (#2881, #3909)
            if (value === 'inherit') {
                element.removeAttribute(key);
            } else {
                element.setAttribute(key, value);
            }
        },
        zIndexSetter: function (value, key) {
            var renderer = this.renderer,
                parentGroup = this.parentGroup,
                parentWrapper = parentGroup || renderer,
                parentNode = parentWrapper.element || renderer.box,
                childNodes,
                otherElement,
                otherZIndex,
                element = this.element,
                inserted,
                run = this.added,
                i;

            if (defined(value)) {
                element.zIndex = value; // So we can read it for other elements in the group
                value = +value;
                if (this[key] === value) { // Only update when needed (#3865)
                    run = false;
                }
                this[key] = value;
            }

            // Insert according to this and other elements' zIndex. Before .add() is called,
            // nothing is done. Then on add, or by later calls to zIndexSetter, the node
            // is placed on the right place in the DOM.
            if (run) {
                value = this.zIndex;

                if (value && parentGroup) {
                    parentGroup.handleZ = true;
                }

                childNodes = parentNode.childNodes;
                for (i = 0; i < childNodes.length && !inserted; i++) {
                    otherElement = childNodes[i];
                    otherZIndex = otherElement.zIndex;
                    if (otherElement !== element && (
                            // Insert before the first element with a higher zIndex
                            pInt(otherZIndex) > value ||
                            // If no zIndex given, insert before the first element with a zIndex
                            (!defined(value) && defined(otherZIndex))

                        )) {
                        parentNode.insertBefore(element, otherElement);
                        inserted = true;
                    }
                }
                if (!inserted) {
                    parentNode.appendChild(element);
                }
            }
            return inserted;
        },
        _defaultSetter: function (value, key, element) {
            element.setAttribute(key, value);
        }
    };

    // Some shared setters and getters
    SVGElement.prototype.yGetter = SVGElement.prototype.xGetter;
    SVGElement.prototype.translateXSetter = SVGElement.prototype.translateYSetter =
            SVGElement.prototype.rotationSetter = SVGElement.prototype.verticalAlignSetter =
            SVGElement.prototype.scaleXSetter = SVGElement.prototype.scaleYSetter = function (value, key) {
                this[key] = value;
                this.doTransform = true;
            };

    // WebKit and Batik have problems with a stroke-width of zero, so in this case we remove the
    // stroke attribute altogether. #1270, #1369, #3065, #3072.
    SVGElement.prototype['stroke-widthSetter'] = SVGElement.prototype.strokeSetter = function (value, key, element) {
        this[key] = value;
        // Only apply the stroke attribute if the stroke width is defined and larger than 0
        if (this.stroke && this['stroke-width']) {
            this.strokeWidth = this['stroke-width'];
            SVGElement.prototype.fillSetter.call(this, this.stroke, 'stroke', element); // use prototype as instance may be overridden
            element.setAttribute('stroke-width', this['stroke-width']);
            this.hasStroke = true;
        } else if (key === 'stroke-width' && value === 0 && this.hasStroke) {
            element.removeAttribute('stroke');
            this.hasStroke = false;
        }
    };


    /**
     * The default SVG renderer
     */
    var SVGRenderer = function () {
        this.init.apply(this, arguments);
    };
    SVGRenderer.prototype = {
        Element: SVGElement,

        /**
         * Initialize the SVGRenderer
         * @param {Object} container
         * @param {Number} width
         * @param {Number} height
         * @param {Boolean} forExport
         */
        init: function (container, width, height, style, forExport, allowHTML) {
            var renderer = this,
                boxWrapper,
                element,
                desc;

            boxWrapper = renderer.createElement('svg')
                .attr({
                    version: '1.1'
                })
                .css(this.getStyle(style));
            element = boxWrapper.element;
            container.appendChild(element);

            // For browsers other than IE, add the namespace attribute (#1978)
            if (container.innerHTML.indexOf('xmlns') === -1) {
                attr(element, 'xmlns', SVG_NS);
            }

            // object properties
            renderer.isSVG = true;
            renderer.box = element;
            renderer.boxWrapper = boxWrapper;
            renderer.alignedObjects = [];

            // Page url used for internal references. #24, #672, #1070
            renderer.url = (isFirefox || isWebKit) && doc.getElementsByTagName('base').length ?
                    win.location.href
                        .replace(/#.*?$/, '') // remove the hash
                        .replace(/([\('\)])/g, '\\$1') // escape parantheses and quotes
                        .replace(/ /g, '%20') : // replace spaces (needed for Safari only)
                    '';

            // Add description
            desc = this.createElement('desc').add();
            desc.element.appendChild(doc.createTextNode('Created with ' + PRODUCT + ' ' + VERSION));


            renderer.defs = this.createElement('defs').add();
            renderer.allowHTML = allowHTML;
            renderer.forExport = forExport;
            renderer.gradients = {}; // Object where gradient SvgElements are stored
            renderer.cache = {}; // Cache for numerical bounding boxes
            renderer.cacheKeys = [];
            renderer.imgCount = 0;

            renderer.setSize(width, height, false);



            // Issue 110 workaround:
            // In Firefox, if a div is positioned by percentage, its pixel position may land
            // between pixels. The container itself doesn't display this, but an SVG element
            // inside this container will be drawn at subpixel precision. In order to draw
            // sharp lines, this must be compensated for. This doesn't seem to work inside
            // iframes though (like in jsFiddle).
            var subPixelFix, rect;
            if (isFirefox && container.getBoundingClientRect) {
                renderer.subPixelFix = subPixelFix = function () {
                    css(container, { left: 0, top: 0 });
                    rect = container.getBoundingClientRect();
                    css(container, {
                        left: (mathCeil(rect.left) - rect.left) + PX,
                        top: (mathCeil(rect.top) - rect.top) + PX
                    });
                };

                // run the fix now
                subPixelFix();

                // run it on resize
                addEvent(win, 'resize', subPixelFix);
            }
        },

        getStyle: function (style) {
            this.style = extend({
                fontFamily: '"Lucida Grande", "Lucida Sans Unicode", Arial, Helvetica, sans-serif', // default font
                fontSize: '12px'
            }, style);
            return this.style;
        },

        /**
         * Detect whether the renderer is hidden. This happens when one of the parent elements
         * has display: none. #608.
         */
        isHidden: function () {
            return !this.boxWrapper.getBBox().width;
        },

        /**
         * Destroys the renderer and its allocated members.
         */
        destroy: function () {
            var renderer = this,
                rendererDefs = renderer.defs;
            renderer.box = null;
            renderer.boxWrapper = renderer.boxWrapper.destroy();

            // Call destroy on all gradient elements
            destroyObjectProperties(renderer.gradients || {});
            renderer.gradients = null;

            // Defs are null in VMLRenderer
            // Otherwise, destroy them here.
            if (rendererDefs) {
                renderer.defs = rendererDefs.destroy();
            }

            // Remove sub pixel fix handler
            // We need to check that there is a handler, otherwise all functions that are registered for event 'resize' are removed
            // See issue #982
            if (renderer.subPixelFix) {
                removeEvent(win, 'resize', renderer.subPixelFix);
            }

            renderer.alignedObjects = null;

            return null;
        },

        /**
         * Create a wrapper for an SVG element
         * @param {Object} nodeName
         */
        createElement: function (nodeName) {
            var wrapper = new this.Element();
            wrapper.init(this, nodeName);
            return wrapper;
        },

        /**
         * Dummy function for use in canvas renderer
         */
        draw: function () {},

        /**
         * Get converted radial gradient attributes
         */
        getRadialAttr: function (radialReference, gradAttr) {
            return {
                cx: (radialReference[0] - radialReference[2] / 2) + gradAttr.cx * radialReference[2],
                cy: (radialReference[1] - radialReference[2] / 2) + gradAttr.cy * radialReference[2],
                r: gradAttr.r * radialReference[2]
            };
        },

        /**
         * Parse a simple HTML string into SVG tspans
         *
         * @param {Object} textNode The parent text SVG node
         */
        buildText: function (wrapper) {
            var textNode = wrapper.element,
                renderer = this,
                forExport = renderer.forExport,
                textStr = pick(wrapper.textStr, '').toString(),
                hasMarkup = textStr.indexOf('<') !== -1,
                lines,
                childNodes = textNode.childNodes,
                styleRegex,
                hrefRegex,
                wasTooLong,
                parentX = attr(textNode, 'x'),
                textStyles = wrapper.styles,
                width = wrapper.textWidth,
                textLineHeight = textStyles && textStyles.lineHeight,
                textShadow = textStyles && textStyles.textShadow,
                ellipsis = textStyles && textStyles.textOverflow === 'ellipsis',
                i = childNodes.length,
                tempParent = width && !wrapper.added && this.box,
                getLineHeight = function (tspan) {
                    return textLineHeight ?
                            pInt(textLineHeight) :
                            renderer.fontMetrics(
                                /(px|em)$/.test(tspan && tspan.style.fontSize) ?
                                        tspan.style.fontSize :
                                        ((textStyles && textStyles.fontSize) || renderer.style.fontSize || 12),
                                tspan
                            ).h;
                },
                unescapeAngleBrackets = function (inputStr) {
                    return inputStr.replace(/&lt;/g, '<').replace(/&gt;/g, '>');
                };

            /// remove old text
            while (i--) {
                textNode.removeChild(childNodes[i]);
            }

            // Skip tspans, add text directly to text node. The forceTSpan is a hook
            // used in text outline hack.
            if (!hasMarkup && !textShadow && !ellipsis && textStr.indexOf(' ') === -1) {
                textNode.appendChild(doc.createTextNode(unescapeAngleBrackets(textStr)));

            // Complex strings, add more logic
            } else {

                styleRegex = /<.*style="([^"]+)".*>/;
                hrefRegex = /<.*href="(http[^"]+)".*>/;

                if (tempParent) {
                    tempParent.appendChild(textNode); // attach it to the DOM to read offset width
                }

                if (hasMarkup) {
                    lines = textStr
                        .replace(/<(b|strong)>/g, '<span style="font-weight:bold">')
                        .replace(/<(i|em)>/g, '<span style="font-style:italic">')
                        .replace(/<a/g, '<span')
                        .replace(/<\/(b|strong|i|em|a)>/g, '</span>')
                        .split(/<br.*?>/g);

                } else {
                    lines = [textStr];
                }


                // Trim empty lines (#5261)
                lines = grep(lines, function (line) {
                    return line !== '';
                });


                // build the lines
                each(lines, function buildTextLines(line, lineNo) {
                    var spans,
                        spanNo = 0;
                    line = line
                        .replace(/^\s+|\s+$/g, '') // Trim to prevent useless/costly process on the spaces (#5258)
                        .replace(/<span/g, '|||<span')
                        .replace(/<\/span>/g, '</span>|||');
                    spans = line.split('|||');

                    each(spans, function buildTextSpans(span) {
                        if (span !== '' || spans.length === 1) {
                            var attributes = {},
                                tspan = doc.createElementNS(SVG_NS, 'tspan'),
                                spanStyle; // #390
                            if (styleRegex.test(span)) {
                                spanStyle = span.match(styleRegex)[1].replace(/(;| |^)color([ :])/, '$1fill$2');
                                attr(tspan, 'style', spanStyle);
                            }
                            if (hrefRegex.test(span) && !forExport) { // Not for export - #1529
                                attr(tspan, 'onclick', 'location.href=\"' + span.match(hrefRegex)[1] + '\"');
                                css(tspan, { cursor: 'pointer' });
                            }

                            span = unescapeAngleBrackets(span.replace(/<(.|\n)*?>/g, '') || ' ');

                            // Nested tags aren't supported, and cause crash in Safari (#1596)
                            if (span !== ' ') {

                                // add the text node
                                tspan.appendChild(doc.createTextNode(span));

                                if (!spanNo) { // first span in a line, align it to the left
                                    if (lineNo && parentX !== null) {
                                        attributes.x = parentX;
                                    }
                                } else {
                                    attributes.dx = 0; // #16
                                }

                                // add attributes
                                attr(tspan, attributes);

                                // Append it
                                textNode.appendChild(tspan);

                                // first span on subsequent line, add the line height
                                if (!spanNo && lineNo) {

                                    // allow getting the right offset height in exporting in IE
                                    if (!hasSVG && forExport) {
                                        css(tspan, { display: 'block' });
                                    }

                                    // Set the line height based on the font size of either
                                    // the text element or the tspan element
                                    attr(
                                        tspan,
                                        'dy',
                                        getLineHeight(tspan)
                                    );
                                }

                                /*if (width) {
                                    renderer.breakText(wrapper, width);
                                }*/

                                // Check width and apply soft breaks or ellipsis
                                if (width) {
                                    var words = span.replace(/([^\^])-/g, '$1- ').split(' '), // #1273
                                        hasWhiteSpace = spans.length > 1 || lineNo || (words.length > 1 && textStyles.whiteSpace !== 'nowrap'),
                                        tooLong,
                                        actualWidth,
                                        rest = [],
                                        dy = getLineHeight(tspan),
                                        softLineNo = 1,
                                        rotation = wrapper.rotation,
                                        wordStr = span, // for ellipsis
                                        cursor = wordStr.length, // binary search cursor
                                        bBox;

                                    while ((hasWhiteSpace || ellipsis) && (words.length || rest.length)) {
                                        wrapper.rotation = 0; // discard rotation when computing box
                                        bBox = wrapper.getBBox(true);
                                        actualWidth = bBox.width;

                                        // Old IE cannot measure the actualWidth for SVG elements (#2314)
                                        if (!hasSVG && renderer.forExport) {
                                            actualWidth = renderer.measureSpanWidth(tspan.firstChild.data, wrapper.styles);
                                        }

                                        tooLong = actualWidth > width;

                                        // For ellipsis, do a binary search for the correct string length
                                        if (wasTooLong === undefined) {
                                            wasTooLong = tooLong; // First time
                                        }
                                        if (ellipsis && wasTooLong) {
                                            cursor /= 2;

                                            if (wordStr === '' || (!tooLong && cursor < 0.5)) {
                                                words = []; // All ok, break out
                                            } else {
                                                wordStr = span.substring(0, wordStr.length + (tooLong ? -1 : 1) * mathCeil(cursor));
                                                words = [wordStr + (width > 3 ? '\u2026' : '')];
                                                tspan.removeChild(tspan.firstChild);
                                            }

                                        // Looping down, this is the first word sequence that is not too long,
                                        // so we can move on to build the next line.
                                        } else if (!tooLong || words.length === 1) {
                                            words = rest;
                                            rest = [];

                                            if (words.length) {
                                                softLineNo++;

                                                tspan = doc.createElementNS(SVG_NS, 'tspan');
                                                attr(tspan, {
                                                    dy: dy,
                                                    x: parentX
                                                });
                                                if (spanStyle) { // #390
                                                    attr(tspan, 'style', spanStyle);
                                                }
                                                textNode.appendChild(tspan);
                                            }
                                            if (actualWidth > width) { // a single word is pressing it out
                                                width = actualWidth;
                                            }
                                        } else { // append to existing line tspan
                                            tspan.removeChild(tspan.firstChild);
                                            rest.unshift(words.pop());
                                        }
                                        if (words.length) {
                                            tspan.appendChild(doc.createTextNode(words.join(' ').replace(/- /g, '-')));
                                        }
                                    }
                                    wrapper.rotation = rotation;
                                }

                                spanNo++;
                            }
                        }
                    });
                });

                if (wasTooLong) {
                    wrapper.attr('title', wrapper.textStr);
                }
                if (tempParent) {
                    tempParent.removeChild(textNode); // attach it to the DOM to read offset width
                }

                // Apply the text shadow
                if (textShadow && wrapper.applyTextShadow) {
                    wrapper.applyTextShadow(textShadow);
                }
            }
        },



        /*
        breakText: function (wrapper, width) {
            var bBox = wrapper.getBBox(),
                node = wrapper.element,
                textLength = node.textContent.length,
                pos = mathRound(width * textLength / bBox.width), // try this position first, based on average character width
                increment = 0,
                finalPos;

            if (bBox.width > width) {
                while (finalPos === undefined) {
                    textLength = node.getSubStringLength(0, pos);

                    if (textLength <= width) {
                        if (increment === -1) {
                            finalPos = pos;
                        } else {
                            increment = 1;
                        }
                    } else {
                        if (increment === 1) {
                            finalPos = pos - 1;
                        } else {
                            increment = -1;
                        }
                    }
                    pos += increment;
                }
            }
            console.log('width', width, 'stringWidth', node.getSubStringLength(0, finalPos))
        },
        */

        /**
         * Returns white for dark colors and black for bright colors
         */
        getContrast: function (color) {
            color = Color(color).rgba;
            return color[0] + color[1] + color[2] > 384 ? '#000000' : '#FFFFFF';
        },

        /**
         * Create a button with preset states
         * @param {String} text
         * @param {Number} x
         * @param {Number} y
         * @param {Function} callback
         * @param {Object} normalState
         * @param {Object} hoverState
         * @param {Object} pressedState
         */
        button: function (text, x, y, callback, normalState, hoverState, pressedState, disabledState, shape) {
            var label = this.label(text, x, y, shape, null, null, null, null, 'button'),
                curState = 0,
                stateOptions,
                stateStyle,
                normalStyle,
                hoverStyle,
                pressedStyle,
                disabledStyle,
                verticalGradient = { x1: 0, y1: 0, x2: 0, y2: 1 };

            // Normal state - prepare the attributes
            normalState = merge({
                'stroke-width': 1,
                stroke: '#CCCCCC',
                fill: {
                    linearGradient: verticalGradient,
                    stops: [
                        [0, '#FEFEFE'],
                        [1, '#F6F6F6']
                    ]
                },
                r: 2,
                padding: 5,
                style: {
                    color: 'black'
                }
            }, normalState);
            normalStyle = normalState.style;
            delete normalState.style;

            // Hover state
            hoverState = merge(normalState, {
                stroke: '#68A',
                fill: {
                    linearGradient: verticalGradient,
                    stops: [
                        [0, '#FFF'],
                        [1, '#ACF']
                    ]
                }
            }, hoverState);
            hoverStyle = hoverState.style;
            delete hoverState.style;

            // Pressed state
            pressedState = merge(normalState, {
                stroke: '#68A',
                fill: {
                    linearGradient: verticalGradient,
                    stops: [
                        [0, '#9BD'],
                        [1, '#CDF']
                    ]
                }
            }, pressedState);
            pressedStyle = pressedState.style;
            delete pressedState.style;

            // Disabled state
            disabledState = merge(normalState, {
                style: {
                    color: '#CCC'
                }
            }, disabledState);
            disabledStyle = disabledState.style;
            delete disabledState.style;

            // Add the events. IE9 and IE10 need mouseover and mouseout to funciton (#667).
            addEvent(label.element, isMS ? 'mouseover' : 'mouseenter', function () {
                if (curState !== 3) {
                    label.attr(hoverState)
                        .css(hoverStyle);
                }
            });
            addEvent(label.element, isMS ? 'mouseout' : 'mouseleave', function () {
                if (curState !== 3) {
                    stateOptions = [normalState, hoverState, pressedState][curState];
                    stateStyle = [normalStyle, hoverStyle, pressedStyle][curState];
                    label.attr(stateOptions)
                        .css(stateStyle);
                }
            });

            label.setState = function (state) {
                label.state = curState = state;
                if (!state) {
                    label.attr(normalState)
                        .css(normalStyle);
                } else if (state === 2) {
                    label.attr(pressedState)
                        .css(pressedStyle);
                } else if (state === 3) {
                    label.attr(disabledState)
                        .css(disabledStyle);
                }
            };

            return label
                .on('click', function (e) {
                    if (curState !== 3) {
                        callback.call(label, e);
                    }
                })
                .attr(normalState)
                .css(extend({ cursor: 'default' }, normalStyle));
        },

        /**
         * Make a straight line crisper by not spilling out to neighbour pixels
         * @param {Array} points
         * @param {Number} width
         */
        crispLine: function (points, width) {
            // points format: [M, 0, 0, L, 100, 0]
            // normalize to a crisp line
            if (points[1] === points[4]) {
                // Substract due to #1129. Now bottom and left axis gridlines behave the same.
                points[1] = points[4] = mathRound(points[1]) - (width % 2 / 2);
            }
            if (points[2] === points[5]) {
                points[2] = points[5] = mathRound(points[2]) + (width % 2 / 2);
            }
            return points;
        },


        /**
         * Draw a path
         * @param {Array} path An SVG path in array form
         */
        path: function (path) {
            var attr = {
                fill: NONE
            };
            if (isArray(path)) {
                attr.d = path;
            } else if (isObject(path)) { // attributes
                extend(attr, path);
            }
            return this.createElement('path').attr(attr);
        },

        /**
         * Draw and return an SVG circle
         * @param {Number} x The x position
         * @param {Number} y The y position
         * @param {Number} r The radius
         */
        circle: function (x, y, r) {
            var attr = isObject(x) ? x : { x: x, y: y, r: r },
                wrapper = this.createElement('circle');

            // Setting x or y translates to cx and cy
            wrapper.xSetter = wrapper.ySetter = function (value, key, element) {
                element.setAttribute('c' + key, value);
            };

            return wrapper.attr(attr);
        },

        /**
         * Draw and return an arc
         * @param {Number} x X position
         * @param {Number} y Y position
         * @param {Number} r Radius
         * @param {Number} innerR Inner radius like used in donut charts
         * @param {Number} start Starting angle
         * @param {Number} end Ending angle
         */
        arc: function (x, y, r, innerR, start, end) {
            var arc;

            if (isObject(x)) {
                y = x.y;
                r = x.r;
                innerR = x.innerR;
                start = x.start;
                end = x.end;
                x = x.x;
            }

            // Arcs are defined as symbols for the ability to set
            // attributes in attr and animate
            arc = this.symbol('arc', x || 0, y || 0, r || 0, r || 0, {
                innerR: innerR || 0,
                start: start || 0,
                end: end || 0
            });
            arc.r = r; // #959
            return arc;
        },

        /**
         * Draw and return a rectangle
         * @param {Number} x Left position
         * @param {Number} y Top position
         * @param {Number} width
         * @param {Number} height
         * @param {Number} r Border corner radius
         * @param {Number} strokeWidth A stroke width can be supplied to allow crisp drawing
         */
        rect: function (x, y, width, height, r, strokeWidth) {

            r = isObject(x) ? x.r : r;

            var wrapper = this.createElement('rect'),
                attribs = isObject(x) ? x : x === UNDEFINED ? {} : {
                    x: x,
                    y: y,
                    width: mathMax(width, 0),
                    height: mathMax(height, 0)
                };

            if (strokeWidth !== UNDEFINED) {
                wrapper.strokeWidth = strokeWidth;
                attribs = wrapper.crisp(attribs);
            }

            if (r) {
                attribs.r = r;
            }

            wrapper.rSetter = function (value, key, element) {
                attr(element, {
                    rx: value,
                    ry: value
                });
            };

            return wrapper.attr(attribs);
        },

        /**
         * Resize the box and re-align all aligned elements
         * @param {Object} width
         * @param {Object} height
         * @param {Boolean} animate
         *
         */
        setSize: function (width, height, animate) {
            var renderer = this,
                alignedObjects = renderer.alignedObjects,
                i = alignedObjects.length;

            renderer.width = width;
            renderer.height = height;

            renderer.boxWrapper[pick(animate, true) ? 'animate' : 'attr']({
                width: width,
                height: height
            });

            while (i--) {
                alignedObjects[i].align();
            }
        },

        /**
         * Create a group
         * @param {String} name The group will be given a class name of 'highcharts-{name}'.
         *     This can be used for styling and scripting.
         */
        g: function (name) {
            var elem = this.createElement('g');
            return defined(name) ? elem.attr({ 'class': PREFIX + name }) : elem;
        },

        /**
         * Display an image
         * @param {String} src
         * @param {Number} x
         * @param {Number} y
         * @param {Number} width
         * @param {Number} height
         */
        image: function (src, x, y, width, height) {
            var attribs = {
                    preserveAspectRatio: NONE
                },
                elemWrapper;

            // optional properties
            if (arguments.length > 1) {
                extend(attribs, {
                    x: x,
                    y: y,
                    width: width,
                    height: height
                });
            }

            elemWrapper = this.createElement('image').attr(attribs);

            // set the href in the xlink namespace
            if (elemWrapper.element.setAttributeNS) {
                elemWrapper.element.setAttributeNS('http://www.w3.org/1999/xlink',
                    'href', src);
            } else {
                // could be exporting in IE
                // using href throws "not supported" in ie7 and under, requries regex shim to fix later
                elemWrapper.element.setAttribute('hc-svg-href', src);
            }
            return elemWrapper;
        },

        /**
         * Draw a symbol out of pre-defined shape paths from the namespace 'symbol' object.
         *
         * @param {Object} symbol
         * @param {Object} x
         * @param {Object} y
         * @param {Object} radius
         * @param {Object} options
         */
        symbol: function (symbol, x, y, width, height, options) {

            var ren = this,
                obj,

                // get the symbol definition function
                symbolFn = this.symbols[symbol],

                // check if there's a path defined for this symbol
                path = symbolFn && symbolFn(
                    mathRound(x),
                    mathRound(y),
                    width,
                    height,
                    options
                ),

                imageRegex = /^url\((.*?)\)$/,
                imageSrc,
                imageSize,
                centerImage;

            if (path) {

                obj = this.path(path);
                // expando properties for use in animate and attr
                extend(obj, {
                    symbolName: symbol,
                    x: x,
                    y: y,
                    width: width,
                    height: height
                });
                if (options) {
                    extend(obj, options);
                }


            // image symbols
            } else if (imageRegex.test(symbol)) {

                // On image load, set the size and position
                centerImage = function (img, size) {
                    if (img.element) { // it may be destroyed in the meantime (#1390)
                        img.attr({
                            width: size[0],
                            height: size[1]
                        });

                        if (!img.alignByTranslate) { // #185
                            img.translate(
                                mathRound((width - size[0]) / 2), // #1378
                                mathRound((height - size[1]) / 2)
                            );
                        }
                    }
                };

                imageSrc = symbol.match(imageRegex)[1];
                imageSize = symbolSizes[imageSrc] || (options && options.width && options.height && [options.width, options.height]);

                // Ireate the image synchronously, add attribs async
                obj = this.image(imageSrc)
                    .attr({
                        x: x,
                        y: y
                    });
                obj.isImg = true;

                if (imageSize) {
                    centerImage(obj, imageSize);
                } else {
                    // Initialize image to be 0 size so export will still function if there's no cached sizes.
                    obj.attr({ width: 0, height: 0 });

                    // Create a dummy JavaScript image to get the width and height. Due to a bug in IE < 8,
                    // the created element must be assigned to a variable in order to load (#292).
                    createElement('img', {
                        onload: function () {

                            // Special case for SVGs on IE11, the width is not accessible until the image is
                            // part of the DOM (#2854).
                            if (this.width === 0) {
                                css(this, {
                                    position: ABSOLUTE,
                                    top: '-999em'
                                });
                                doc.body.appendChild(this);
                            }

                            // Center the image
                            centerImage(obj, symbolSizes[imageSrc] = [this.width, this.height]);

                            // Clean up after #2854 workaround.
                            if (this.parentNode) {
                                this.parentNode.removeChild(this);
                            }

                            // Fire the load event when all external images are loaded
                            ren.imgCount--;
                            if (!ren.imgCount && charts[ren.chartIndex].onload) {
                                charts[ren.chartIndex].onload();
                            }
                        },
                        src: imageSrc
                    });
                    this.imgCount++;
                }
            }

            return obj;
        },

        /**
         * An extendable collection of functions for defining symbol paths.
         */
        symbols: {
            'circle': function (x, y, w, h) {
                var cpw = 0.166 * w;
                return [
                    M, x + w / 2, y,
                    'C', x + w + cpw, y, x + w + cpw, y + h, x + w / 2, y + h,
                    'C', x - cpw, y + h, x - cpw, y, x + w / 2, y,
                    'Z'
                ];
            },

            'square': function (x, y, w, h) {
                return [
                    M, x, y,
                    L, x + w, y,
                    x + w, y + h,
                    x, y + h,
                    'Z'
                ];
            },

            'triangle': function (x, y, w, h) {
                return [
                    M, x + w / 2, y,
                    L, x + w, y + h,
                    x, y + h,
                    'Z'
                ];
            },

            'triangle-down': function (x, y, w, h) {
                return [
                    M, x, y,
                    L, x + w, y,
                    x + w / 2, y + h,
                    'Z'
                ];
            },
            'diamond': function (x, y, w, h) {
                return [
                    M, x + w / 2, y,
                    L, x + w, y + h / 2,
                    x + w / 2, y + h,
                    x, y + h / 2,
                    'Z'
                ];
            },
            'arc': function (x, y, w, h, options) {
                var start = options.start,
                    radius = options.r || w || h,
                    end = options.end - 0.001, // to prevent cos and sin of start and end from becoming equal on 360 arcs (related: #1561)
                    innerRadius = options.innerR,
                    open = options.open,
                    cosStart = mathCos(start),
                    sinStart = mathSin(start),
                    cosEnd = mathCos(end),
                    sinEnd = mathSin(end),
                    longArc = options.end - start < mathPI ? 0 : 1;

                return [
                    M,
                    x + radius * cosStart,
                    y + radius * sinStart,
                    'A', // arcTo
                    radius, // x radius
                    radius, // y radius
                    0, // slanting
                    longArc, // long or short arc
                    1, // clockwise
                    x + radius * cosEnd,
                    y + radius * sinEnd,
                    open ? M : L,
                    x + innerRadius * cosEnd,
                    y + innerRadius * sinEnd,
                    'A', // arcTo
                    innerRadius, // x radius
                    innerRadius, // y radius
                    0, // slanting
                    longArc, // long or short arc
                    0, // clockwise
                    x + innerRadius * cosStart,
                    y + innerRadius * sinStart,

                    open ? '' : 'Z' // close
                ];
            },

            /**
             * Callout shape used for default tooltips, also used for rounded rectangles in VML
             */
            callout: function (x, y, w, h, options) {
                var arrowLength = 6,
                    halfDistance = 6,
                    r = mathMin((options && options.r) || 0, w, h),
                    safeDistance = r + halfDistance,
                    anchorX = options && options.anchorX,
                    anchorY = options && options.anchorY,
                    path;

                path = [
                    'M', x + r, y,
                    'L', x + w - r, y, // top side
                    'C', x + w, y, x + w, y, x + w, y + r, // top-right corner
                    'L', x + w, y + h - r, // right side
                    'C', x + w, y + h, x + w, y + h, x + w - r, y + h, // bottom-right corner
                    'L', x + r, y + h, // bottom side
                    'C', x, y + h, x, y + h, x, y + h - r, // bottom-left corner
                    'L', x, y + r, // left side
                    'C', x, y, x, y, x + r, y // top-right corner
                ];

                if (anchorX && anchorX > w && anchorY > y + safeDistance && anchorY < y + h - safeDistance) { // replace right side
                    path.splice(13, 3,
                        'L', x + w, anchorY - halfDistance,
                        x + w + arrowLength, anchorY,
                        x + w, anchorY + halfDistance,
                        x + w, y + h - r
                        );
                } else if (anchorX && anchorX < 0 && anchorY > y + safeDistance && anchorY < y + h - safeDistance) { // replace left side
                    path.splice(33, 3,
                        'L', x, anchorY + halfDistance,
                        x - arrowLength, anchorY,
                        x, anchorY - halfDistance,
                        x, y + r
                        );
                } else if (anchorY && anchorY > h && anchorX > x + safeDistance && anchorX < x + w - safeDistance) { // replace bottom
                    path.splice(23, 3,
                        'L', anchorX + halfDistance, y + h,
                        anchorX, y + h + arrowLength,
                        anchorX - halfDistance, y + h,
                        x + r, y + h
                        );
                } else if (anchorY && anchorY < 0 && anchorX > x + safeDistance && anchorX < x + w - safeDistance) { // replace top
                    path.splice(3, 3,
                        'L', anchorX - halfDistance, y,
                        anchorX, y - arrowLength,
                        anchorX + halfDistance, y,
                        w - r, y
                        );
                }
                return path;
            }
        },

        /**
         * Define a clipping rectangle
         * @param {String} id
         * @param {Number} x
         * @param {Number} y
         * @param {Number} width
         * @param {Number} height
         */
        clipRect: function (x, y, width, height) {
            var wrapper,
                id = PREFIX + idCounter++,

                clipPath = this.createElement('clipPath').attr({
                    id: id
                }).add(this.defs);

            wrapper = this.rect(x, y, width, height, 0).add(clipPath);
            wrapper.id = id;
            wrapper.clipPath = clipPath;
            wrapper.count = 0;

            return wrapper;
        },





        /**
         * Add text to the SVG object
         * @param {String} str
         * @param {Number} x Left position
         * @param {Number} y Top position
         * @param {Boolean} useHTML Use HTML to render the text
         */
        text: function (str, x, y, useHTML) {

            // declare variables
            var renderer = this,
                fakeSVG = useCanVG || (!hasSVG && renderer.forExport),
                wrapper,
                attr = {};

            if (useHTML && (renderer.allowHTML || !renderer.forExport)) {
                return renderer.html(str, x, y);
            }

            attr.x = Math.round(x || 0); // X is always needed for line-wrap logic
            if (y) {
                attr.y = Math.round(y);
            }
            if (str || str === 0) {
                attr.text = str;
            }

            wrapper = renderer.createElement('text')
                .attr(attr);

            // Prevent wrapping from creating false offsetWidths in export in legacy IE (#1079, #1063)
            if (fakeSVG) {
                wrapper.css({
                    position: ABSOLUTE
                });
            }

            if (!useHTML) {
                wrapper.xSetter = function (value, key, element) {
                    var tspans = element.getElementsByTagName('tspan'),
                        tspan,
                        parentVal = element.getAttribute(key),
                        i;
                    for (i = 0; i < tspans.length; i++) {
                        tspan = tspans[i];
                        // If the x values are equal, the tspan represents a linebreak
                        if (tspan.getAttribute(key) === parentVal) {
                            tspan.setAttribute(key, value);
                        }
                    }
                    element.setAttribute(key, value);
                };
            }

            return wrapper;
        },

        /**
         * Utility to return the baseline offset and total line height from the font size
         */
        fontMetrics: function (fontSize, elem) {
            var lineHeight,
                baseline,
                style;

            fontSize = fontSize || this.style.fontSize;
            if (!fontSize && elem && win.getComputedStyle) {
                elem = elem.element || elem; // SVGElement
                style = win.getComputedStyle(elem, '');
                fontSize = style && style.fontSize; // #4309, the style doesn't exist inside a hidden iframe in Firefox
            }
            fontSize = /px/.test(fontSize) ? pInt(fontSize) : /em/.test(fontSize) ? parseFloat(fontSize) * 12 : 12;

            // Empirical values found by comparing font size and bounding box height.
            // Applies to the default font family. http://jsfiddle.net/highcharts/7xvn7/
            lineHeight = fontSize < 24 ? fontSize + 3 : mathRound(fontSize * 1.2);
            baseline = mathRound(lineHeight * 0.8);

            return {
                h: lineHeight,
                b: baseline,
                f: fontSize
            };
        },

        /**
         * Correct X and Y positioning of a label for rotation (#1764)
         */
        rotCorr: function (baseline, rotation, alterY) {
            var y = baseline;
            if (rotation && alterY) {
                y = mathMax(y * mathCos(rotation * deg2rad), 4);
            }
            return {
                x: (-baseline / 3) * mathSin(rotation * deg2rad),
                y: y
            };
        },

        /**
         * Add a label, a text item that can hold a colored or gradient background
         * as well as a border and shadow.
         * @param {string} str
         * @param {Number} x
         * @param {Number} y
         * @param {String} shape
         * @param {Number} anchorX In case the shape has a pointer, like a flag, this is the
         *    coordinates it should be pinned to
         * @param {Number} anchorY
         * @param {Boolean} baseline Whether to position the label relative to the text baseline,
         *    like renderer.text, or to the upper border of the rectangle.
         * @param {String} className Class name for the group
         */
        label: function (str, x, y, shape, anchorX, anchorY, useHTML, baseline, className) {

            var renderer = this,
                wrapper = renderer.g(className),
                text = renderer.text('', 0, 0, useHTML)
                    .attr({
                        zIndex: 1
                    }),
                    //.add(wrapper),
                box,
                bBox,
                alignFactor = 0,
                padding = 3,
                paddingLeft = 0,
                width,
                height,
                wrapperX,
                wrapperY,
                crispAdjust = 0,
                deferredAttr = {},
                baselineOffset,
                needsBox,
                updateBoxSize,
                updateTextPadding,
                boxAttr;

            /**
             * This function runs after the label is added to the DOM (when the bounding box is
             * available), and after the text of the label is updated to detect the new bounding
             * box and reflect it in the border box.
             */
            updateBoxSize = function () {
                var boxX,
                    boxY,
                    style = text.element.style;

                bBox = (width === undefined || height === undefined || wrapper.styles.textAlign) && defined(text.textStr) &&
                    text.getBBox(); //#3295 && 3514 box failure when string equals 0
                wrapper.width = (width || bBox.width || 0) + 2 * padding + paddingLeft;
                wrapper.height = (height || bBox.height || 0) + 2 * padding;

                // update the label-scoped y offset
                baselineOffset = padding + renderer.fontMetrics(style && style.fontSize, text).b;


                if (needsBox) {

                    if (!box) {
                        // create the border box if it is not already present
                        boxX = crispAdjust;
                        boxY = (baseline ? -baselineOffset : 0) + crispAdjust;

                        wrapper.box = box = shape ?
                                renderer.symbol(shape, boxX, boxY, wrapper.width, wrapper.height, deferredAttr) :
                                renderer.rect(boxX, boxY, wrapper.width, wrapper.height, 0, deferredAttr[STROKE_WIDTH]);

                        if (!box.isImg) { // #4324, fill "none" causes it to be ignored by mouse events in IE
                            box.attr('fill', NONE);
                        }
                        box.add(wrapper);
                    }

                    // apply the box attributes
                    if (!box.isImg) { // #1630
                        box.attr(extend({
                            width: mathRound(wrapper.width),
                            height: mathRound(wrapper.height)
                        }, deferredAttr));
                    }
                    deferredAttr = null;
                }
            };

            /**
             * This function runs after setting text or padding, but only if padding is changed
             */
            updateTextPadding = function () {
                var styles = wrapper.styles,
                    textAlign = styles && styles.textAlign,
                    x = paddingLeft + padding,
                    y;

                // determin y based on the baseline
                y = baseline ? 0 : baselineOffset;

                // compensate for alignment
                if (defined(width) && bBox && (textAlign === 'center' || textAlign === 'right')) {
                    x += { center: 0.5, right: 1 }[textAlign] * (width - bBox.width);
                }

                // update if anything changed
                if (x !== text.x || y !== text.y) {
                    text.attr('x', x);
                    if (y !== UNDEFINED) {
                        text.attr('y', y);
                    }
                }

                // record current values
                text.x = x;
                text.y = y;
            };

            /**
             * Set a box attribute, or defer it if the box is not yet created
             * @param {Object} key
             * @param {Object} value
             */
            boxAttr = function (key, value) {
                if (box) {
                    box.attr(key, value);
                } else {
                    deferredAttr[key] = value;
                }
            };

            /**
             * After the text element is added, get the desired size of the border box
             * and add it before the text in the DOM.
             */
            wrapper.onAdd = function () {
                text.add(wrapper);
                wrapper.attr({
                    text: (str || str === 0) ? str : '', // alignment is available now // #3295: 0 not rendered if given as a value
                    x: x,
                    y: y
                });

                if (box && defined(anchorX)) {
                    wrapper.attr({
                        anchorX: anchorX,
                        anchorY: anchorY
                    });
                }
            };

            /*
             * Add specific attribute setters.
             */

            // only change local variables
            wrapper.widthSetter = function (value) {
                width = value;
            };
            wrapper.heightSetter = function (value) {
                height = value;
            };
            wrapper.paddingSetter =  function (value) {
                if (defined(value) && value !== padding) {
                    padding = wrapper.padding = value;
                    updateTextPadding();
                }
            };
            wrapper.paddingLeftSetter =  function (value) {
                if (defined(value) && value !== paddingLeft) {
                    paddingLeft = value;
                    updateTextPadding();
                }
            };


            // change local variable and prevent setting attribute on the group
            wrapper.alignSetter = function (value) {
                value = { left: 0, center: 0.5, right: 1 }[value];
                if (value !== alignFactor) {
                    alignFactor = value;
                    if (bBox) { // Bounding box exists, means we're dynamically changing
                        wrapper.attr({ x: wrapperX }); // #5134
                    }
                }
            };

            // apply these to the box and the text alike
            wrapper.textSetter = function (value) {
                if (value !== UNDEFINED) {
                    text.textSetter(value);
                }
                updateBoxSize();
                updateTextPadding();
            };

            // apply these to the box but not to the text
            wrapper['stroke-widthSetter'] = function (value, key) {
                if (value) {
                    needsBox = true;
                }
                crispAdjust = value % 2 / 2;
                boxAttr(key, value);
            };
            wrapper.strokeSetter = wrapper.fillSetter = wrapper.rSetter = function (value, key) {
                if (key === 'fill' && value) {
                    needsBox = true;
                }
                boxAttr(key, value);
            };
            wrapper.anchorXSetter = function (value, key) {
                anchorX = value;
                boxAttr(key, mathRound(value) - crispAdjust - wrapperX);
            };
            wrapper.anchorYSetter = function (value, key) {
                anchorY = value;
                boxAttr(key, value - wrapperY);
            };

            // rename attributes
            wrapper.xSetter = function (value) {
                wrapper.x = value; // for animation getter
                if (alignFactor) {
                    value -= alignFactor * ((width || bBox.width) + 2 * padding);
                }
                wrapperX = mathRound(value);
                wrapper.attr('translateX', wrapperX);
            };
            wrapper.ySetter = function (value) {
                wrapperY = wrapper.y = mathRound(value);
                wrapper.attr('translateY', wrapperY);
            };

            // Redirect certain methods to either the box or the text
            var baseCss = wrapper.css;
            return extend(wrapper, {
                /**
                 * Pick up some properties and apply them to the text instead of the wrapper
                 */
                css: function (styles) {
                    if (styles) {
                        var textStyles = {};
                        styles = merge(styles); // create a copy to avoid altering the original object (#537)
                        each(wrapper.textProps, function (prop) {
                            if (styles[prop] !== UNDEFINED) {
                                textStyles[prop] = styles[prop];
                                delete styles[prop];
                            }
                        });
                        text.css(textStyles);
                    }
                    return baseCss.call(wrapper, styles);
                },
                /**
                 * Return the bounding box of the box, not the group
                 */
                getBBox: function () {
                    return {
                        width: bBox.width + 2 * padding,
                        height: bBox.height + 2 * padding,
                        x: bBox.x - padding,
                        y: bBox.y - padding
                    };
                },
                /**
                 * Apply the shadow to the box
                 */
                shadow: function (b) {
                    if (box) {
                        box.shadow(b);
                    }
                    return wrapper;
                },
                /**
                 * Destroy and release memory.
                 */
                destroy: function () {

                    // Added by button implementation
                    removeEvent(wrapper.element, 'mouseenter');
                    removeEvent(wrapper.element, 'mouseleave');

                    if (text) {
                        text = text.destroy();
                    }
                    if (box) {
                        box = box.destroy();
                    }
                    // Call base implementation to destroy the rest
                    SVGElement.prototype.destroy.call(wrapper);

                    // Release local pointers (#1298)
                    wrapper = renderer = updateBoxSize = updateTextPadding = boxAttr = null;
                }
            });
        }
    }; // end SVGRenderer


    // general renderer
    Renderer = SVGRenderer;
    // extend SvgElement for useHTML option
    extend(SVGElement.prototype, {
        /**
         * Apply CSS to HTML elements. This is used in text within SVG rendering and
         * by the VML renderer
         */
        htmlCss: function (styles) {
            var wrapper = this,
                element = wrapper.element,
                textWidth = styles && element.tagName === 'SPAN' && styles.width;

            if (textWidth) {
                delete styles.width;
                wrapper.textWidth = textWidth;
                wrapper.updateTransform();
            }
            if (styles && styles.textOverflow === 'ellipsis') {
                styles.whiteSpace = 'nowrap';
                styles.overflow = 'hidden';
            }
            wrapper.styles = extend(wrapper.styles, styles);
            css(wrapper.element, styles);

            return wrapper;
        },

        /**
         * VML and useHTML method for calculating the bounding box based on offsets
         * @param {Boolean} refresh Whether to force a fresh value from the DOM or to
         * use the cached value
         *
         * @return {Object} A hash containing values for x, y, width and height
         */

        htmlGetBBox: function () {
            var wrapper = this,
                element = wrapper.element;

            // faking getBBox in exported SVG in legacy IE
            // faking getBBox in exported SVG in legacy IE (is this a duplicate of the fix for #1079?)
            if (element.nodeName === 'text') {
                element.style.position = ABSOLUTE;
            }

            return {
                x: element.offsetLeft,
                y: element.offsetTop,
                width: element.offsetWidth,
                height: element.offsetHeight
            };
        },

        /**
         * VML override private method to update elements based on internal
         * properties based on SVG transform
         */
        htmlUpdateTransform: function () {
            // aligning non added elements is expensive
            if (!this.added) {
                this.alignOnAdd = true;
                return;
            }

            var wrapper = this,
                renderer = wrapper.renderer,
                elem = wrapper.element,
                translateX = wrapper.translateX || 0,
                translateY = wrapper.translateY || 0,
                x = wrapper.x || 0,
                y = wrapper.y || 0,
                align = wrapper.textAlign || 'left',
                alignCorrection = { left: 0, center: 0.5, right: 1 }[align],
                shadows = wrapper.shadows,
                styles = wrapper.styles;

            // apply translate
            css(elem, {
                marginLeft: translateX,
                marginTop: translateY
            });
            if (shadows) { // used in labels/tooltip
                each(shadows, function (shadow) {
                    css(shadow, {
                        marginLeft: translateX + 1,
                        marginTop: translateY + 1
                    });
                });
            }

            // apply inversion
            if (wrapper.inverted) { // wrapper is a group
                each(elem.childNodes, function (child) {
                    renderer.invertChild(child, elem);
                });
            }

            if (elem.tagName === 'SPAN') {

                var rotation = wrapper.rotation,
                    baseline,
                    textWidth = pInt(wrapper.textWidth),
                    whiteSpace = styles && styles.whiteSpace,
                    currentTextTransform = [rotation, align, elem.innerHTML, wrapper.textWidth, wrapper.textAlign].join(',');

                if (currentTextTransform !== wrapper.cTT) { // do the calculations and DOM access only if properties changed


                    baseline = renderer.fontMetrics(elem.style.fontSize).b;

                    // Renderer specific handling of span rotation
                    if (defined(rotation)) {
                        wrapper.setSpanRotation(rotation, alignCorrection, baseline);
                    }

                    // Update textWidth
                    if (elem.offsetWidth > textWidth && /[ \-]/.test(elem.textContent || elem.innerText)) { // #983, #1254
                        css(elem, {
                            width: textWidth + PX,
                            display: 'block',
                            whiteSpace: whiteSpace || 'normal' // #3331
                        });
                        wrapper.hasTextWidth = true;
                    } else if (wrapper.hasTextWidth) { // #4928
                        css(elem, {
                            width: '',
                            display: '',
                            whiteSpace: whiteSpace || 'nowrap'
                        });
                        wrapper.hasTextWidth = false;
                    }

                    wrapper.getSpanCorrection(wrapper.hasTextWidth ? textWidth : elem.offsetWidth, baseline, alignCorrection, rotation, align);
                }

                // apply position with correction
                css(elem, {
                    left: (x + (wrapper.xCorr || 0)) + PX,
                    top: (y + (wrapper.yCorr || 0)) + PX
                });

                // force reflow in webkit to apply the left and top on useHTML element (#1249)
                if (isWebKit) {
                    baseline = elem.offsetHeight; // assigned to baseline for lint purpose
                }

                // record current text transform
                wrapper.cTT = currentTextTransform;
            }
        },

        /**
         * Set the rotation of an individual HTML span
         */
        setSpanRotation: function (rotation, alignCorrection, baseline) {
            var rotationStyle = {},
                cssTransformKey = isMS ? '-ms-transform' : isWebKit ? '-webkit-transform' : isFirefox ? 'MozTransform' : isOpera ? '-o-transform' : '';

            rotationStyle[cssTransformKey] = rotationStyle.transform = 'rotate(' + rotation + 'deg)';
            rotationStyle[cssTransformKey + (isFirefox ? 'Origin' : '-origin')] = rotationStyle.transformOrigin = (alignCorrection * 100) + '% ' + baseline + 'px';
            css(this.element, rotationStyle);
        },

        /**
         * Get the correction in X and Y positioning as the element is rotated.
         */
        getSpanCorrection: function (width, baseline, alignCorrection) {
            this.xCorr = -width * alignCorrection;
            this.yCorr = -baseline;
        }
    });

    // Extend SvgRenderer for useHTML option.
    extend(SVGRenderer.prototype, {
        /**
         * Create HTML text node. This is used by the VML renderer as well as the SVG
         * renderer through the useHTML option.
         *
         * @param {String} str
         * @param {Number} x
         * @param {Number} y
         */
        html: function (str, x, y) {
            var wrapper = this.createElement('span'),
                element = wrapper.element,
                renderer = wrapper.renderer,
                isSVG = renderer.isSVG,
                addSetters = function (element, style) {
                    // These properties are set as attributes on the SVG group, and as
                    // identical CSS properties on the div. (#3542)
                    each(['opacity', 'visibility'], function (prop) {
                        wrap(element, prop + 'Setter', function (proceed, value, key, elem) {
                            proceed.call(this, value, key, elem);
                            style[key] = value;
                        });
                    });            
                };

            // Text setter
            wrapper.textSetter = function (value) {
                if (value !== element.innerHTML) {
                    delete this.bBox;
                }
                element.innerHTML = this.textStr = value;
                wrapper.htmlUpdateTransform();
            };

            // Add setters for the element itself (#4938)
            if (isSVG) { // #4938, only for HTML within SVG
                addSetters(wrapper, wrapper.element.style);
            }

            // Various setters which rely on update transform
            wrapper.xSetter = wrapper.ySetter = wrapper.alignSetter = wrapper.rotationSetter = function (value, key) {
                if (key === 'align') {
                    key = 'textAlign'; // Do not overwrite the SVGElement.align method. Same as VML.
                }
                wrapper[key] = value;
                wrapper.htmlUpdateTransform();
            };

            // Set the default attributes
            wrapper
                .attr({
                    text: str,
                    x: mathRound(x),
                    y: mathRound(y)
                })
                .css({
                    position: ABSOLUTE,
                    fontFamily: this.style.fontFamily,
                    fontSize: this.style.fontSize
                });

            // Keep the whiteSpace style outside the wrapper.styles collection
            element.style.whiteSpace = 'nowrap';

            // Use the HTML specific .css method
            wrapper.css = wrapper.htmlCss;

            // This is specific for HTML within SVG
            if (isSVG) {
                wrapper.add = function (svgGroupWrapper) {

                    var htmlGroup,
                        container = renderer.box.parentNode,
                        parentGroup,
                        parents = [];

                    this.parentGroup = svgGroupWrapper;

                    // Create a mock group to hold the HTML elements
                    if (svgGroupWrapper) {
                        htmlGroup = svgGroupWrapper.div;
                        if (!htmlGroup) {

                            // Read the parent chain into an array and read from top down
                            parentGroup = svgGroupWrapper;
                            while (parentGroup) {

                                parents.push(parentGroup);

                                // Move up to the next parent group
                                parentGroup = parentGroup.parentGroup;
                            }

                            // Ensure dynamically updating position when any parent is translated
                            each(parents.reverse(), function (parentGroup) {
                                var htmlGroupStyle,
                                    cls = attr(parentGroup.element, 'class');

                                if (cls) {
                                    cls = { className: cls };
                                } // else null

                                // Create a HTML div and append it to the parent div to emulate
                                // the SVG group structure
                                htmlGroup = parentGroup.div = parentGroup.div || createElement(DIV, cls, {
                                    position: ABSOLUTE,
                                    left: (parentGroup.translateX || 0) + PX,
                                    top: (parentGroup.translateY || 0) + PX,
                                    opacity: parentGroup.opacity // #5075
                                }, htmlGroup || container); // the top group is appended to container

                                // Shortcut
                                htmlGroupStyle = htmlGroup.style;

                                // Set listeners to update the HTML div's position whenever the SVG group
                                // position is changed
                                extend(parentGroup, {
                                    translateXSetter: function (value, key) {
                                        htmlGroupStyle.left = value + PX;
                                        parentGroup[key] = value;
                                        parentGroup.doTransform = true;
                                    },
                                    translateYSetter: function (value, key) {
                                        htmlGroupStyle.top = value + PX;
                                        parentGroup[key] = value;
                                        parentGroup.doTransform = true;
                                    }
                                });
                                addSetters(parentGroup, htmlGroupStyle);
                            });

                        }
                    } else {
                        htmlGroup = container;
                    }

                    htmlGroup.appendChild(element);

                    // Shared with VML:
                    wrapper.added = true;
                    if (wrapper.alignOnAdd) {
                        wrapper.htmlUpdateTransform();
                    }

                    return wrapper;
                };
            }
            return wrapper;
        }
    });


    /* ****************************************************************************
     *                                                                            *
     * START OF INTERNET EXPLORER <= 8 SPECIFIC CODE                              *
     *                                                                            *
     * For applications and websites that don't need IE support, like platform    *
     * targeted mobile apps and web apps, this code can be removed.               *
     *                                                                            *
     *****************************************************************************/

    /**
     * @constructor
     */
    var VMLRenderer, VMLElement;
    if (!hasSVG && !useCanVG) {

    /**
     * The VML element wrapper.
     */
    VMLElement = {

        /**
         * Initialize a new VML element wrapper. It builds the markup as a string
         * to minimize DOM traffic.
         * @param {Object} renderer
         * @param {Object} nodeName
         */
        init: function (renderer, nodeName) {
            var wrapper = this,
                markup =  ['<', nodeName, ' filled="f" stroked="f"'],
                style = ['position: ', ABSOLUTE, ';'],
                isDiv = nodeName === DIV;

            // divs and shapes need size
            if (nodeName === 'shape' || isDiv) {
                style.push('left:0;top:0;width:1px;height:1px;');
            }
            style.push('visibility: ', isDiv ? HIDDEN : VISIBLE);

            markup.push(' style="', style.join(''), '"/>');

            // create element with default attributes and style
            if (nodeName) {
                markup = isDiv || nodeName === 'span' || nodeName === 'img' ?
                    markup.join('')    :
                    renderer.prepVML(markup);
                wrapper.element = createElement(markup);
            }

            wrapper.renderer = renderer;
        },

        /**
         * Add the node to the given parent
         * @param {Object} parent
         */
        add: function (parent) {
            var wrapper = this,
                renderer = wrapper.renderer,
                element = wrapper.element,
                box = renderer.box,
                inverted = parent && parent.inverted,

                // get the parent node
                parentNode = parent ?
                    parent.element || parent :
                    box;

            if (parent) {
                this.parentGroup = parent;
            }

            // if the parent group is inverted, apply inversion on all children
            if (inverted) { // only on groups
                renderer.invertChild(element, parentNode);
            }

            // append it
            parentNode.appendChild(element);

            // align text after adding to be able to read offset
            wrapper.added = true;
            if (wrapper.alignOnAdd && !wrapper.deferUpdateTransform) {
                wrapper.updateTransform();
            }

            // fire an event for internal hooks
            if (wrapper.onAdd) {
                wrapper.onAdd();
            }

            return wrapper;
        },

        /**
         * VML always uses htmlUpdateTransform
         */
        updateTransform: SVGElement.prototype.htmlUpdateTransform,

        /**
         * Set the rotation of a span with oldIE's filter
         */
        setSpanRotation: function () {
            // Adjust for alignment and rotation. Rotation of useHTML content is not yet implemented
            // but it can probably be implemented for Firefox 3.5+ on user request. FF3.5+
            // has support for CSS3 transform. The getBBox method also needs to be updated
            // to compensate for the rotation, like it currently does for SVG.
            // Test case: http://jsfiddle.net/highcharts/Ybt44/

            var rotation = this.rotation,
                costheta = mathCos(rotation * deg2rad),
                sintheta = mathSin(rotation * deg2rad);

            css(this.element, {
                filter: rotation ? ['progid:DXImageTransform.Microsoft.Matrix(M11=', costheta,
                    ', M12=', -sintheta, ', M21=', sintheta, ', M22=', costheta,
                    ', sizingMethod=\'auto expand\')'].join('') : NONE
            });
        },

        /**
         * Get the positioning correction for the span after rotating.
         */
        getSpanCorrection: function (width, baseline, alignCorrection, rotation, align) {

            var costheta = rotation ? mathCos(rotation * deg2rad) : 1,
                sintheta = rotation ? mathSin(rotation * deg2rad) : 0,
                height = pick(this.elemHeight, this.element.offsetHeight),
                quad,
                nonLeft = align && align !== 'left';

            // correct x and y
            this.xCorr = costheta < 0 && -width;
            this.yCorr = sintheta < 0 && -height;

            // correct for baseline and corners spilling out after rotation
            quad = costheta * sintheta < 0;
            this.xCorr += sintheta * baseline * (quad ? 1 - alignCorrection : alignCorrection);
            this.yCorr -= costheta * baseline * (rotation ? (quad ? alignCorrection : 1 - alignCorrection) : 1);
            // correct for the length/height of the text
            if (nonLeft) {
                this.xCorr -= width * alignCorrection * (costheta < 0 ? -1 : 1);
                if (rotation) {
                    this.yCorr -= height * alignCorrection * (sintheta < 0 ? -1 : 1);
                }
                css(this.element, {
                    textAlign: align
                });
            }
        },

        /**
         * Converts a subset of an SVG path definition to its VML counterpart. Takes an array
         * as the parameter and returns a string.
         */
        pathToVML: function (value) {
            // convert paths
            var i = value.length,
                path = [];

            while (i--) {

                // Multiply by 10 to allow subpixel precision.
                // Substracting half a pixel seems to make the coordinates
                // align with SVG, but this hasn't been tested thoroughly
                if (isNumber(value[i])) {
                    path[i] = mathRound(value[i] * 10) - 5;
                } else if (value[i] === 'Z') { // close the path
                    path[i] = 'x';
                } else {
                    path[i] = value[i];

                    // When the start X and end X coordinates of an arc are too close,
                    // they are rounded to the same value above. In this case, substract or
                    // add 1 from the end X and Y positions. #186, #760, #1371, #1410.
                    if (value.isArc && (value[i] === 'wa' || value[i] === 'at')) {
                        // Start and end X
                        if (path[i + 5] === path[i + 7]) {
                            path[i + 7] += value[i + 7] > value[i + 5] ? 1 : -1;
                        }
                        // Start and end Y
                        if (path[i + 6] === path[i + 8]) {
                            path[i + 8] += value[i + 8] > value[i + 6] ? 1 : -1;
                        }
                    }
                }
            }


            // Loop up again to handle path shortcuts (#2132)
            /*while (i++ < path.length) {
                if (path[i] === 'H') { // horizontal line to
                    path[i] = 'L';
                    path.splice(i + 2, 0, path[i - 1]);
                } else if (path[i] === 'V') { // vertical line to
                    path[i] = 'L';
                    path.splice(i + 1, 0, path[i - 2]);
                }
            }*/
            return path.join(' ') || 'x';
        },

        /**
         * Set the element's clipping to a predefined rectangle
         *
         * @param {String} id The id of the clip rectangle
         */
        clip: function (clipRect) {
            var wrapper = this,
                clipMembers,
                cssRet;

            if (clipRect) {
                clipMembers = clipRect.members;
                erase(clipMembers, wrapper); // Ensure unique list of elements (#1258)
                clipMembers.push(wrapper);
                wrapper.destroyClip = function () {
                    erase(clipMembers, wrapper);
                };
                cssRet = clipRect.getCSS(wrapper);

            } else {
                if (wrapper.destroyClip) {
                    wrapper.destroyClip();
                }
                cssRet = { clip: docMode8 ? 'inherit' : 'rect(auto)' }; // #1214
            }

            return wrapper.css(cssRet);

        },

        /**
         * Set styles for the element
         * @param {Object} styles
         */
        css: SVGElement.prototype.htmlCss,

        /**
         * Removes a child either by removeChild or move to garbageBin.
         * Issue 490; in VML removeChild results in Orphaned nodes according to sIEve, discardElement does not.
         */
        safeRemoveChild: function (element) {
            // discardElement will detach the node from its parent before attaching it
            // to the garbage bin. Therefore it is important that the node is attached and have parent.
            if (element.parentNode) {
                discardElement(element);
            }
        },

        /**
         * Extend element.destroy by removing it from the clip members array
         */
        destroy: function () {
            if (this.destroyClip) {
                this.destroyClip();
            }

            return SVGElement.prototype.destroy.apply(this);
        },

        /**
         * Add an event listener. VML override for normalizing event parameters.
         * @param {String} eventType
         * @param {Function} handler
         */
        on: function (eventType, handler) {
            // simplest possible event model for internal use
            this.element['on' + eventType] = function () {
                var evt = win.event;
                evt.target = evt.srcElement;
                handler(evt);
            };
            return this;
        },

        /**
         * In stacked columns, cut off the shadows so that they don't overlap
         */
        cutOffPath: function (path, length) {

            var len;

            path = path.split(/[ ,]/);
            len = path.length;

            if (len === 9 || len === 11) {
                path[len - 4] = path[len - 2] = pInt(path[len - 2]) - 10 * length;
            }
            return path.join(' ');
        },

        /**
         * Apply a drop shadow by copying elements and giving them different strokes
         * @param {Boolean|Object} shadowOptions
         */
        shadow: function (shadowOptions, group, cutOff) {
            var shadows = [],
                i,
                element = this.element,
                renderer = this.renderer,
                shadow,
                elemStyle = element.style,
                markup,
                path = element.path,
                strokeWidth,
                modifiedPath,
                shadowWidth,
                shadowElementOpacity;

            // some times empty paths are not strings
            if (path && typeof path.value !== 'string') {
                path = 'x';
            }
            modifiedPath = path;

            if (shadowOptions) {
                shadowWidth = pick(shadowOptions.width, 3);
                shadowElementOpacity = (shadowOptions.opacity || 0.15) / shadowWidth;
                for (i = 1; i <= 3; i++) {

                    strokeWidth = (shadowWidth * 2) + 1 - (2 * i);

                    // Cut off shadows for stacked column items
                    if (cutOff) {
                        modifiedPath = this.cutOffPath(path.value, strokeWidth + 0.5);
                    }

                    markup = ['<shape isShadow="true" strokeweight="', strokeWidth,
                        '" filled="false" path="', modifiedPath,
                        '" coordsize="10 10" style="', element.style.cssText, '" />'];

                    shadow = createElement(renderer.prepVML(markup),
                        null, {
                            left: pInt(elemStyle.left) + pick(shadowOptions.offsetX, 1),
                            top: pInt(elemStyle.top) + pick(shadowOptions.offsetY, 1)
                        }
                    );
                    if (cutOff) {
                        shadow.cutOff = strokeWidth + 1;
                    }

                    // apply the opacity
                    markup = ['<stroke color="', shadowOptions.color || 'black', '" opacity="', shadowElementOpacity * i, '"/>'];
                    createElement(renderer.prepVML(markup), null, null, shadow);


                    // insert it
                    if (group) {
                        group.element.appendChild(shadow);
                    } else {
                        element.parentNode.insertBefore(shadow, element);
                    }

                    // record it
                    shadows.push(shadow);

                }

                this.shadows = shadows;
            }
            return this;
        },
        updateShadows: noop, // Used in SVG only

        setAttr: function (key, value) {
            if (docMode8) { // IE8 setAttribute bug
                this.element[key] = value;
            } else {
                this.element.setAttribute(key, value);
            }
        },
        classSetter: function (value) {
            // IE8 Standards mode has problems retrieving the className unless set like this
            this.element.className = value;
        },
        dashstyleSetter: function (value, key, element) {
            var strokeElem = element.getElementsByTagName('stroke')[0] ||
                createElement(this.renderer.prepVML(['<stroke/>']), null, null, element);
            strokeElem[key] = value || 'solid';
            this[key] = value; /* because changing stroke-width will change the dash length
                and cause an epileptic effect */
        },
        dSetter: function (value, key, element) {
            var i,
                shadows = this.shadows;
            value = value || [];
            this.d = value.join && value.join(' '); // used in getter for animation

            element.path = value = this.pathToVML(value);

            // update shadows
            if (shadows) {
                i = shadows.length;
                while (i--) {
                    shadows[i].path = shadows[i].cutOff ? this.cutOffPath(value, shadows[i].cutOff) : value;
                }
            }
            this.setAttr(key, value);
        },
        fillSetter: function (value, key, element) {
            var nodeName = element.nodeName;
            if (nodeName === 'SPAN') { // text color
                element.style.color = value;
            } else if (nodeName !== 'IMG') { // #1336
                element.filled = value !== NONE;
                this.setAttr('fillcolor', this.renderer.color(value, element, key, this));
            }
        },
        'fill-opacitySetter': function (value, key, element) {
            createElement(
                this.renderer.prepVML(['<', key.split('-')[0], ' opacity="', value, '"/>']),
                null,
                null,
                element
            );
        },
        opacitySetter: noop, // Don't bother - animation is too slow and filters introduce artifacts
        rotationSetter: function (value, key, element) {
            var style = element.style;
            this[key] = style[key] = value; // style is for #1873

            // Correction for the 1x1 size of the shape container. Used in gauge needles.
            style.left = -mathRound(mathSin(value * deg2rad) + 1) + PX;
            style.top = mathRound(mathCos(value * deg2rad)) + PX;
        },
        strokeSetter: function (value, key, element) {
            this.setAttr('strokecolor', this.renderer.color(value, element, key, this));
        },
        'stroke-widthSetter': function (value, key, element) {
            element.stroked = !!value; // VML "stroked" attribute
            this[key] = value; // used in getter, issue #113
            if (isNumber(value)) {
                value += PX;
            }
            this.setAttr('strokeweight', value);
        },
        titleSetter: function (value, key) {
            this.setAttr(key, value);
        },
        visibilitySetter: function (value, key, element) {

            // Handle inherited visibility
            if (value === 'inherit') {
                value = VISIBLE;
            }

            // Let the shadow follow the main element
            if (this.shadows) {
                each(this.shadows, function (shadow) {
                    shadow.style[key] = value;
                });
            }

            // Instead of toggling the visibility CSS property, move the div out of the viewport.
            // This works around #61 and #586
            if (element.nodeName === 'DIV') {
                value = value === HIDDEN ? '-999em' : 0;

                // In order to redraw, IE7 needs the div to be visible when tucked away
                // outside the viewport. So the visibility is actually opposite of
                // the expected value. This applies to the tooltip only.
                if (!docMode8) {
                    element.style[key] = value ? VISIBLE : HIDDEN;
                }
                key = 'top';
            }
            element.style[key] = value;
        },
        xSetter: function (value, key, element) {
            this[key] = value; // used in getter

            if (key === 'x') {
                key = 'left';
            } else if (key === 'y') {
                key = 'top';
            }/* else {
                value = mathMax(0, value); // don't set width or height below zero (#311)
            }*/

            // clipping rectangle special
            if (this.updateClipping) {
                this[key] = value; // the key is now 'left' or 'top' for 'x' and 'y'
                this.updateClipping();
            } else {
                // normal
                element.style[key] = value;
            }
        },
        zIndexSetter: function (value, key, element) {
            element.style[key] = value;
        }
    };
    VMLElement['stroke-opacitySetter'] = VMLElement['fill-opacitySetter'];

    Highcharts.VMLElement = VMLElement = extendClass(SVGElement, VMLElement);

    // Some shared setters
    VMLElement.prototype.ySetter =
        VMLElement.prototype.widthSetter =
        VMLElement.prototype.heightSetter =
        VMLElement.prototype.xSetter;


    /**
     * The VML renderer
     */
    var VMLRendererExtension = { // inherit SVGRenderer

        Element: VMLElement,
        isIE8: userAgent.indexOf('MSIE 8.0') > -1,


        /**
         * Initialize the VMLRenderer
         * @param {Object} container
         * @param {Number} width
         * @param {Number} height
         */
        init: function (container, width, height, style) {
            var renderer = this,
                boxWrapper,
                box,
                css;

            renderer.alignedObjects = [];

            boxWrapper = renderer.createElement(DIV)
                .css(extend(this.getStyle(style), { position: 'relative' }));
            box = boxWrapper.element;
            container.appendChild(boxWrapper.element);


            // generate the containing box
            renderer.isVML = true;
            renderer.box = box;
            renderer.boxWrapper = boxWrapper;
            renderer.gradients = {};
            renderer.cache = {}; // Cache for numerical bounding boxes
            renderer.cacheKeys = [];
            renderer.imgCount = 0;


            renderer.setSize(width, height, false);

            // The only way to make IE6 and IE7 print is to use a global namespace. However,
            // with IE8 the only way to make the dynamic shapes visible in screen and print mode
            // seems to be to add the xmlns attribute and the behaviour style inline.
            if (!doc.namespaces.hcv) {

                doc.namespaces.add('hcv', 'urn:schemas-microsoft-com:vml');

                // Setup default CSS (#2153, #2368, #2384)
                css = 'hcv\\:fill, hcv\\:path, hcv\\:shape, hcv\\:stroke' +
                    '{ behavior:url(#default#VML); display: inline-block; } ';
                try {
                    doc.createStyleSheet().cssText = css;
                } catch (e) {
                    doc.styleSheets[0].cssText += css;
                }

            }
        },


        /**
         * Detect whether the renderer is hidden. This happens when one of the parent elements
         * has display: none
         */
        isHidden: function () {
            return !this.box.offsetWidth;
        },

        /**
         * Define a clipping rectangle. In VML it is accomplished by storing the values
         * for setting the CSS style to all associated members.
         *
         * @param {Number} x
         * @param {Number} y
         * @param {Number} width
         * @param {Number} height
         */
        clipRect: function (x, y, width, height) {

            // create a dummy element
            var clipRect = this.createElement(),
                isObj = isObject(x);

            // mimic a rectangle with its style object for automatic updating in attr
            return extend(clipRect, {
                members: [],
                count: 0,
                left: (isObj ? x.x : x) + 1,
                top: (isObj ? x.y : y) + 1,
                width: (isObj ? x.width : width) - 1,
                height: (isObj ? x.height : height) - 1,
                getCSS: function (wrapper) {
                    var element = wrapper.element,
                        nodeName = element.nodeName,
                        isShape = nodeName === 'shape',
                        inverted = wrapper.inverted,
                        rect = this,
                        top = rect.top - (isShape ? element.offsetTop : 0),
                        left = rect.left,
                        right = left + rect.width,
                        bottom = top + rect.height,
                        ret = {
                            clip: 'rect(' +
                                mathRound(inverted ? left : top) + 'px,' +
                                mathRound(inverted ? bottom : right) + 'px,' +
                                mathRound(inverted ? right : bottom) + 'px,' +
                                mathRound(inverted ? top : left) + 'px)'
                        };

                    // issue 74 workaround
                    if (!inverted && docMode8 && nodeName === 'DIV') {
                        extend(ret, {
                            width: right + PX,
                            height: bottom + PX
                        });
                    }
                    return ret;
                },

                // used in attr and animation to update the clipping of all members
                updateClipping: function () {
                    each(clipRect.members, function (member) {
                        if (member.element) { // Deleted series, like in stock/members/series-remove demo. Should be removed from members, but this will do.
                            member.css(clipRect.getCSS(member));
                        }
                    });
                }
            });

        },


        /**
         * Take a color and return it if it's a string, make it a gradient if it's a
         * gradient configuration object, and apply opacity.
         *
         * @param {Object} color The color or config object
         */
        color: function (color, elem, prop, wrapper) {
            var renderer = this,
                colorObject,
                regexRgba = /^rgba/,
                markup,
                fillType,
                ret = NONE;

            // Check for linear or radial gradient
            if (color && color.linearGradient) {
                fillType = 'gradient';
            } else if (color && color.radialGradient) {
                fillType = 'pattern';
            }


            if (fillType) {

                var stopColor,
                    stopOpacity,
                    gradient = color.linearGradient || color.radialGradient,
                    x1,
                    y1,
                    x2,
                    y2,
                    opacity1,
                    opacity2,
                    color1,
                    color2,
                    fillAttr = '',
                    stops = color.stops,
                    firstStop,
                    lastStop,
                    colors = [],
                    addFillNode = function () {
                        // Add the fill subnode. When colors attribute is used, the meanings of opacity and o:opacity2
                        // are reversed.
                        markup = ['<fill colors="' + colors.join(',') + '" opacity="', opacity2, '" o:opacity2="', opacity1,
                            '" type="', fillType, '" ', fillAttr, 'focus="100%" method="any" />'];
                        createElement(renderer.prepVML(markup), null, null, elem);
                    };

                // Extend from 0 to 1
                firstStop = stops[0];
                lastStop = stops[stops.length - 1];
                if (firstStop[0] > 0) {
                    stops.unshift([
                        0,
                        firstStop[1]
                    ]);
                }
                if (lastStop[0] < 1) {
                    stops.push([
                        1,
                        lastStop[1]
                    ]);
                }

                // Compute the stops
                each(stops, function (stop, i) {
                    if (regexRgba.test(stop[1])) {
                        colorObject = Color(stop[1]);
                        stopColor = colorObject.get('rgb');
                        stopOpacity = colorObject.get('a');
                    } else {
                        stopColor = stop[1];
                        stopOpacity = 1;
                    }

                    // Build the color attribute
                    colors.push((stop[0] * 100) + '% ' + stopColor);

                    // Only start and end opacities are allowed, so we use the first and the last
                    if (!i) {
                        opacity1 = stopOpacity;
                        color2 = stopColor;
                    } else {
                        opacity2 = stopOpacity;
                        color1 = stopColor;
                    }
                });

                // Apply the gradient to fills only.
                if (prop === 'fill') {

                    // Handle linear gradient angle
                    if (fillType === 'gradient') {
                        x1 = gradient.x1 || gradient[0] || 0;
                        y1 = gradient.y1 || gradient[1] || 0;
                        x2 = gradient.x2 || gradient[2] || 0;
                        y2 = gradient.y2 || gradient[3] || 0;
                        fillAttr = 'angle="' + (90  - math.atan(
                            (y2 - y1) / // y vector
                            (x2 - x1) // x vector
                            ) * 180 / mathPI) + '"';

                        addFillNode();

                    // Radial (circular) gradient
                    } else {

                        var r = gradient.r,
                            sizex = r * 2,
                            sizey = r * 2,
                            cx = gradient.cx,
                            cy = gradient.cy,
                            radialReference = elem.radialReference,
                            bBox,
                            applyRadialGradient = function () {
                                if (radialReference) {
                                    bBox = wrapper.getBBox();
                                    cx += (radialReference[0] - bBox.x) / bBox.width - 0.5;
                                    cy += (radialReference[1] - bBox.y) / bBox.height - 0.5;
                                    sizex *= radialReference[2] / bBox.width;
                                    sizey *= radialReference[2] / bBox.height;
                                }
                                fillAttr = 'src="' + defaultOptions.global.VMLRadialGradientURL + '" ' +
                                    'size="' + sizex + ',' + sizey + '" ' +
                                    'origin="0.5,0.5" ' +
                                    'position="' + cx + ',' + cy + '" ' +
                                    'color2="' + color2 + '" ';

                                addFillNode();
                            };

                        // Apply radial gradient
                        if (wrapper.added) {
                            applyRadialGradient();
                        } else {
                            // We need to know the bounding box to get the size and position right
                            wrapper.onAdd = applyRadialGradient;
                        }

                        // The fill element's color attribute is broken in IE8 standards mode, so we
                        // need to set the parent shape's fillcolor attribute instead.
                        ret = color1;
                    }

                // Gradients are not supported for VML stroke, return the first color. #722.
                } else {
                    ret = stopColor;
                }

            // If the color is an rgba color, split it and add a fill node
            // to hold the opacity component
            } else if (regexRgba.test(color) && elem.tagName !== 'IMG') {

                colorObject = Color(color);

                wrapper[prop + '-opacitySetter'](colorObject.get('a'), prop, elem);

                ret = colorObject.get('rgb');


            } else {
                var propNodes = elem.getElementsByTagName(prop); // 'stroke' or 'fill' node
                if (propNodes.length) {
                    propNodes[0].opacity = 1;
                    propNodes[0].type = 'solid';
                }
                ret = color;
            }

            return ret;
        },

        /**
         * Take a VML string and prepare it for either IE8 or IE6/IE7.
         * @param {Array} markup A string array of the VML markup to prepare
         */
        prepVML: function (markup) {
            var vmlStyle = 'display:inline-block;behavior:url(#default#VML);',
                isIE8 = this.isIE8;

            markup = markup.join('');

            if (isIE8) { // add xmlns and style inline
                markup = markup.replace('/>', ' xmlns="urn:schemas-microsoft-com:vml" />');
                if (markup.indexOf('style="') === -1) {
                    markup = markup.replace('/>', ' style="' + vmlStyle + '" />');
                } else {
                    markup = markup.replace('style="', 'style="' + vmlStyle);
                }

            } else { // add namespace
                markup = markup.replace('<', '<hcv:');
            }

            return markup;
        },

        /**
         * Create rotated and aligned text
         * @param {String} str
         * @param {Number} x
         * @param {Number} y
         */
        text: SVGRenderer.prototype.html,

        /**
         * Create and return a path element
         * @param {Array} path
         */
        path: function (path) {
            var attr = {
                // subpixel precision down to 0.1 (width and height = 1px)
                coordsize: '10 10'
            };
            if (isArray(path)) {
                attr.d = path;
            } else if (isObject(path)) { // attributes
                extend(attr, path);
            }
            // create the shape
            return this.createElement('shape').attr(attr);
        },

        /**
         * Create and return a circle element. In VML circles are implemented as
         * shapes, which is faster than v:oval
         * @param {Number} x
         * @param {Number} y
         * @param {Number} r
         */
        circle: function (x, y, r) {
            var circle = this.symbol('circle');
            if (isObject(x)) {
                r = x.r;
                y = x.y;
                x = x.x;
            }
            circle.isCircle = true; // Causes x and y to mean center (#1682)
            circle.r = r;
            return circle.attr({ x: x, y: y });
        },

        /**
         * Create a group using an outer div and an inner v:group to allow rotating
         * and flipping. A simple v:group would have problems with positioning
         * child HTML elements and CSS clip.
         *
         * @param {String} name The name of the group
         */
        g: function (name) {
            var wrapper,
                attribs;

            // set the class name
            if (name) {
                attribs = { 'className': PREFIX + name, 'class': PREFIX + name };
            }

            // the div to hold HTML and clipping
            wrapper = this.createElement(DIV).attr(attribs);

            return wrapper;
        },

        /**
         * VML override to create a regular HTML image
         * @param {String} src
         * @param {Number} x
         * @param {Number} y
         * @param {Number} width
         * @param {Number} height
         */
        image: function (src, x, y, width, height) {
            var obj = this.createElement('img')
                .attr({ src: src });

            if (arguments.length > 1) {
                obj.attr({
                    x: x,
                    y: y,
                    width: width,
                    height: height
                });
            }
            return obj;
        },

        /**
         * For rectangles, VML uses a shape for rect to overcome bugs and rotation problems
         */
        createElement: function (nodeName) {
            return nodeName === 'rect' ? this.symbol(nodeName) : SVGRenderer.prototype.createElement.call(this, nodeName);
        },

        /**
         * In the VML renderer, each child of an inverted div (group) is inverted
         * @param {Object} element
         * @param {Object} parentNode
         */
        invertChild: function (element, parentNode) {
            var ren = this,
                parentStyle = parentNode.style,
                imgStyle = element.tagName === 'IMG' && element.style; // #1111

            css(element, {
                flip: 'x',
                left: pInt(parentStyle.width) - (imgStyle ? pInt(imgStyle.top) : 1),
                top: pInt(parentStyle.height) - (imgStyle ? pInt(imgStyle.left) : 1),
                rotation: -90
            });

            // Recursively invert child elements, needed for nested composite shapes like box plots and error bars. #1680, #1806.
            each(element.childNodes, function (child) {
                ren.invertChild(child, element);
            });
        },

        /**
         * Symbol definitions that override the parent SVG renderer's symbols
         *
         */
        symbols: {
            // VML specific arc function
            arc: function (x, y, w, h, options) {
                var start = options.start,
                    end = options.end,
                    radius = options.r || w || h,
                    innerRadius = options.innerR,
                    cosStart = mathCos(start),
                    sinStart = mathSin(start),
                    cosEnd = mathCos(end),
                    sinEnd = mathSin(end),
                    ret;

                if (end - start === 0) { // no angle, don't show it.
                    return ['x'];
                }

                ret = [
                    'wa', // clockwise arc to
                    x - radius, // left
                    y - radius, // top
                    x + radius, // right
                    y + radius, // bottom
                    x + radius * cosStart, // start x
                    y + radius * sinStart, // start y
                    x + radius * cosEnd, // end x
                    y + radius * sinEnd  // end y
                ];

                if (options.open && !innerRadius) {
                    ret.push(
                        'e',
                        M,
                        x, // - innerRadius,
                        y// - innerRadius
                    );
                }

                ret.push(
                    'at', // anti clockwise arc to
                    x - innerRadius, // left
                    y - innerRadius, // top
                    x + innerRadius, // right
                    y + innerRadius, // bottom
                    x + innerRadius * cosEnd, // start x
                    y + innerRadius * sinEnd, // start y
                    x + innerRadius * cosStart, // end x
                    y + innerRadius * sinStart, // end y
                    'x', // finish path
                    'e' // close
                );

                ret.isArc = true;
                return ret;

            },
            // Add circle symbol path. This performs significantly faster than v:oval.
            circle: function (x, y, w, h, wrapper) {

                if (wrapper) {
                    w = h = 2 * wrapper.r;
                }

                // Center correction, #1682
                if (wrapper && wrapper.isCircle) {
                    x -= w / 2;
                    y -= h / 2;
                }

                // Return the path
                return [
                    'wa', // clockwisearcto
                    x, // left
                    y, // top
                    x + w, // right
                    y + h, // bottom
                    x + w, // start x
                    y + h / 2,     // start y
                    x + w, // end x
                    y + h / 2,     // end y
                    //'x', // finish path
                    'e' // close
                ];
            },
            /**
             * Add rectangle symbol path which eases rotation and omits arcsize problems
             * compared to the built-in VML roundrect shape. When borders are not rounded,
             * use the simpler square path, else use the callout path without the arrow.
             */
            rect: function (x, y, w, h, options) {
                return SVGRenderer.prototype.symbols[
                    !defined(options) || !options.r ? 'square' : 'callout'
                ].call(0, x, y, w, h, options);
            }
        }
    };
    Highcharts.VMLRenderer = VMLRenderer = function () {
        this.init.apply(this, arguments);
    };
    VMLRenderer.prototype = merge(SVGRenderer.prototype, VMLRendererExtension);

        // general renderer
        Renderer = VMLRenderer;
    }

    // This method is used with exporting in old IE, when emulating SVG (see #2314)
    SVGRenderer.prototype.measureSpanWidth = function (text, styles) {
        var measuringSpan = doc.createElement('span'),
            offsetWidth,
            textNode = doc.createTextNode(text);

        measuringSpan.appendChild(textNode);
        css(measuringSpan, styles);
        this.box.appendChild(measuringSpan);
        offsetWidth = measuringSpan.offsetWidth;
        discardElement(measuringSpan); // #2463
        return offsetWidth;
    };


    /* ****************************************************************************
     *                                                                            *
     * END OF INTERNET EXPLORER <= 8 SPECIFIC CODE                                *
     *                                                                            *
     *****************************************************************************/
    /* ****************************************************************************
     *                                                                            *
     * START OF ANDROID < 3 SPECIFIC CODE. THIS CAN BE REMOVED IF YOU'RE NOT      *
     * TARGETING THAT SYSTEM.                                                     *
     *                                                                            *
     *****************************************************************************/
    var CanVGRenderer,
        CanVGController;

    /**
     * Downloads a script and executes a callback when done.
     * @param {String} scriptLocation
     * @param {Function} callback
     */
    function getScript(scriptLocation, callback) {
        var head = doc.getElementsByTagName('head')[0],
            script = doc.createElement('script');

        script.type = 'text/javascript';
        script.src = scriptLocation;
        script.onload = callback;

        head.appendChild(script);
    }

    if (useCanVG) {
        /**
         * The CanVGRenderer is empty from start to keep the source footprint small.
         * When requested, the CanVGController downloads the rest of the source packaged
         * together with the canvg library.
         */
        Highcharts.CanVGRenderer = CanVGRenderer = function () {
            // Override the global SVG namespace to fake SVG/HTML that accepts CSS
            SVG_NS = 'http://www.w3.org/1999/xhtml';
        };

        /**
         * Start with an empty symbols object. This is needed when exporting is used (exporting.src.js will add a few symbols), but
         * the implementation from SvgRenderer will not be merged in until first render.
         */
        CanVGRenderer.prototype.symbols = {};

        /**
         * Handles on demand download of canvg rendering support.
         */
        CanVGController = (function () {
            // List of renderering calls
            var deferredRenderCalls = [];

            /**
             * When downloaded, we are ready to draw deferred charts.
             */
            function drawDeferred() {
                var callLength = deferredRenderCalls.length,
                    callIndex;

                // Draw all pending render calls
                for (callIndex = 0; callIndex < callLength; callIndex++) {
                    deferredRenderCalls[callIndex]();
                }
                // Clear the list
                deferredRenderCalls = [];
            }

            return {
                push: function (func, scriptLocation) {
                    // Only get the script once
                    if (deferredRenderCalls.length === 0) {
                        getScript(scriptLocation, drawDeferred);
                    }
                    // Register render call
                    deferredRenderCalls.push(func);
                }
            };
        }());

        Renderer = CanVGRenderer;
    } // end CanVGRenderer

    /* ****************************************************************************
     *                                                                            *
     * END OF ANDROID < 3 SPECIFIC CODE                                           *
     *                                                                            *
     *****************************************************************************/

    /**
     * The Tick class
     */
    function Tick(axis, pos, type, noLabel) {
        this.axis = axis;
        this.pos = pos;
        this.type = type || '';
        this.isNew = true;

        if (!type && !noLabel) {
            this.addLabel();
        }
    }

    Tick.prototype = {
        /**
         * Write the tick label
         */
        addLabel: function () {
            var tick = this,
                axis = tick.axis,
                options = axis.options,
                chart = axis.chart,
                categories = axis.categories,
                names = axis.names,
                pos = tick.pos,
                labelOptions = options.labels,
                str,
                tickPositions = axis.tickPositions,
                isFirst = pos === tickPositions[0],
                isLast = pos === tickPositions[tickPositions.length - 1],
                value = categories ?
                    pick(categories[pos], names[pos], pos) :
                    pos,
                label = tick.label,
                tickPositionInfo = tickPositions.info,
                dateTimeLabelFormat;

            // Set the datetime label format. If a higher rank is set for this position, use that. If not,
            // use the general format.
            if (axis.isDatetimeAxis && tickPositionInfo) {
                dateTimeLabelFormat = options.dateTimeLabelFormats[tickPositionInfo.higherRanks[pos] || tickPositionInfo.unitName];
            }
            // set properties for access in render method
            tick.isFirst = isFirst;
            tick.isLast = isLast;

            // get the string
            str = axis.labelFormatter.call({
                axis: axis,
                chart: chart,
                isFirst: isFirst,
                isLast: isLast,
                dateTimeLabelFormat: dateTimeLabelFormat,
                value: axis.isLog ? correctFloat(axis.lin2log(value)) : value
            });

            // prepare CSS
            //css = width && { width: mathMax(1, mathRound(width - 2 * (labelOptions.padding || 10))) + PX };

            // first call
            if (!defined(label)) {

                tick.label = label =
                    defined(str) && labelOptions.enabled ?
                        chart.renderer.text(
                                str,
                                0,
                                0,
                                labelOptions.useHTML
                            )
                            //.attr(attr)
                            // without position absolute, IE export sometimes is wrong
                            .css(merge(labelOptions.style))
                            .add(axis.labelGroup) :
                        null;
                tick.labelLength = label && label.getBBox().width; // Un-rotated length
                tick.rotation = 0; // Base value to detect change for new calls to getBBox

            // update
            } else if (label) {
                label.attr({ text: str });
            }
        },

        /**
         * Get the offset height or width of the label
         */
        getLabelSize: function () {
            return this.label ?
                this.label.getBBox()[this.axis.horiz ? 'height' : 'width'] :
                0;
        },

        /**
         * Handle the label overflow by adjusting the labels to the left and right edge, or
         * hide them if they collide into the neighbour label.
         */
        handleOverflow: function (xy) {
            var axis = this.axis,
                pxPos = xy.x,
                chartWidth = axis.chart.chartWidth,
                spacing = axis.chart.spacing,
                leftBound = pick(axis.labelLeft, mathMin(axis.pos, spacing[3])),
                rightBound = pick(axis.labelRight, mathMax(axis.pos + axis.len, chartWidth - spacing[1])),
                label = this.label,
                rotation = this.rotation,
                factor = { left: 0, center: 0.5, right: 1 }[axis.labelAlign],
                labelWidth = label.getBBox().width,
                slotWidth = axis.getSlotWidth(),
                modifiedSlotWidth = slotWidth,
                xCorrection = factor,
                goRight = 1,
                leftPos,
                rightPos,
                textWidth,
                css = {};

            // Check if the label overshoots the chart spacing box. If it does, move it.
            // If it now overshoots the slotWidth, add ellipsis.
            if (!rotation) {
                leftPos = pxPos - factor * labelWidth;
                rightPos = pxPos + (1 - factor) * labelWidth;

                if (leftPos < leftBound) {
                    modifiedSlotWidth = xy.x + modifiedSlotWidth * (1 - factor) - leftBound;
                } else if (rightPos > rightBound) {
                    modifiedSlotWidth = rightBound - xy.x + modifiedSlotWidth * factor;
                    goRight = -1;
                }

                modifiedSlotWidth = mathMin(slotWidth, modifiedSlotWidth); // #4177
                if (modifiedSlotWidth < slotWidth && axis.labelAlign === 'center') {
                    xy.x += goRight * (slotWidth - modifiedSlotWidth - xCorrection * (slotWidth - mathMin(labelWidth, modifiedSlotWidth)));
                }
                // If the label width exceeds the available space, set a text width to be
                // picked up below. Also, if a width has been set before, we need to set a new
                // one because the reported labelWidth will be limited by the box (#3938).
                if (labelWidth > modifiedSlotWidth || (axis.autoRotation && label.styles.width)) {
                    textWidth = modifiedSlotWidth;
                }

            // Add ellipsis to prevent rotated labels to be clipped against the edge of the chart
            } else if (rotation < 0 && pxPos - factor * labelWidth < leftBound) {
                textWidth = mathRound(pxPos / mathCos(rotation * deg2rad) - leftBound);
            } else if (rotation > 0 && pxPos + factor * labelWidth > rightBound) {
                textWidth = mathRound((chartWidth - pxPos) / mathCos(rotation * deg2rad));
            }

            if (textWidth) {
                css.width = textWidth;
                if (!axis.options.labels.style.textOverflow) {
                    css.textOverflow = 'ellipsis';
                }
                label.css(css);
            }
        },

        /**
         * Get the x and y position for ticks and labels
         */
        getPosition: function (horiz, pos, tickmarkOffset, old) {
            var axis = this.axis,
                chart = axis.chart,
                cHeight = (old && chart.oldChartHeight) || chart.chartHeight;

            return {
                x: horiz ?
                    axis.translate(pos + tickmarkOffset, null, null, old) + axis.transB :
                    axis.left + axis.offset + (axis.opposite ? ((old && chart.oldChartWidth) || chart.chartWidth) - axis.right - axis.left : 0),

                y: horiz ?
                    cHeight - axis.bottom + axis.offset - (axis.opposite ? axis.height : 0) :
                    cHeight - axis.translate(pos + tickmarkOffset, null, null, old) - axis.transB
            };

        },

        /**
         * Get the x, y position of the tick label
         */
        getLabelPosition: function (x, y, label, horiz, labelOptions, tickmarkOffset, index, step) {
            var axis = this.axis,
                transA = axis.transA,
                reversed = axis.reversed,
                staggerLines = axis.staggerLines,
                rotCorr = axis.tickRotCorr || { x: 0, y: 0 },
                yOffset = labelOptions.y,
                line;

            if (!defined(yOffset)) {
                if (axis.side === 0) {
                    yOffset = label.rotation ? -8 : -label.getBBox().height;
                } else if (axis.side === 2) {
                    yOffset = rotCorr.y + 8;
                } else {
                    // #3140, #3140
                    yOffset = mathCos(label.rotation * deg2rad) * (rotCorr.y - label.getBBox(false, 0).height / 2);
                }
            }

            x = x + labelOptions.x + rotCorr.x - (tickmarkOffset && horiz ?
                tickmarkOffset * transA * (reversed ? -1 : 1) : 0);
            y = y + yOffset - (tickmarkOffset && !horiz ?
                tickmarkOffset * transA * (reversed ? 1 : -1) : 0);

            // Correct for staggered labels
            if (staggerLines) {
                line = (index / (step || 1) % staggerLines);
                if (axis.opposite) {
                    line = staggerLines - line - 1;
                }
                y += line * (axis.labelOffset / staggerLines);
            }

            return {
                x: x,
                y: mathRound(y)
            };
        },

        /**
         * Extendible method to return the path of the marker
         */
        getMarkPath: function (x, y, tickLength, tickWidth, horiz, renderer) {
            return renderer.crispLine([
                M,
                x,
                y,
                L,
                x + (horiz ? 0 : -tickLength),
                y + (horiz ? tickLength : 0)
            ], tickWidth);
        },

        /**
         * Put everything in place
         *
         * @param index {Number}
         * @param old {Boolean} Use old coordinates to prepare an animation into new position
         */
        render: function (index, old, opacity) {
            var tick = this,
                axis = tick.axis,
                options = axis.options,
                chart = axis.chart,
                renderer = chart.renderer,
                horiz = axis.horiz,
                type = tick.type,
                label = tick.label,
                pos = tick.pos,
                labelOptions = options.labels,
                gridLine = tick.gridLine,
                gridPrefix = type ? type + 'Grid' : 'grid',
                tickPrefix = type ? type + 'Tick' : 'tick',
                gridLineWidth = options[gridPrefix + 'LineWidth'],
                gridLineColor = options[gridPrefix + 'LineColor'],
                dashStyle = options[gridPrefix + 'LineDashStyle'],
                tickSize = axis.tickSize(tickPrefix),
                tickColor = options[tickPrefix + 'Color'],
                gridLinePath,
                mark = tick.mark,
                markPath,
                step = /*axis.labelStep || */labelOptions.step,
                attribs,
                show = true,
                tickmarkOffset = axis.tickmarkOffset,
                xy = tick.getPosition(horiz, pos, tickmarkOffset, old),
                x = xy.x,
                y = xy.y,
                reverseCrisp = ((horiz && x === axis.pos + axis.len) || (!horiz && y === axis.pos)) ? -1 : 1; // #1480, #1687

            opacity = pick(opacity, 1);
            this.isActive = true;

            // create the grid line
            if (gridLineWidth) {
                gridLinePath = axis.getPlotLinePath(pos + tickmarkOffset, gridLineWidth * reverseCrisp, old, true);

                if (gridLine === UNDEFINED) {
                    attribs = {
                        stroke: gridLineColor,
                        'stroke-width': gridLineWidth
                    };
                    if (dashStyle) {
                        attribs.dashstyle = dashStyle;
                    }
                    if (!type) {
                        attribs.zIndex = 1;
                    }
                    if (old) {
                        attribs.opacity = 0;
                    }
                    tick.gridLine = gridLine =
                        gridLineWidth ?
                            renderer.path(gridLinePath)
                                .attr(attribs).add(axis.gridGroup) :
                            null;
                }

                // If the parameter 'old' is set, the current call will be followed
                // by another call, therefore do not do any animations this time
                if (!old && gridLine && gridLinePath) {
                    gridLine[tick.isNew ? 'attr' : 'animate']({
                        d: gridLinePath,
                        opacity: opacity
                    });
                }
            }

            // create the tick mark
            if (tickSize) {
                if (axis.opposite) {
                    tickSize[0] = -tickSize[0];
                }
                markPath = tick.getMarkPath(x, y, tickSize[0], tickSize[1] * reverseCrisp, horiz, renderer);
                if (mark) { // updating
                    mark.animate({
                        d: markPath,
                        opacity: opacity
                    });
                } else { // first time
                    tick.mark = renderer.path(
                        markPath
                    ).attr({
                        stroke: tickColor,
                        'stroke-width': tickSize[1],
                        opacity: opacity
                    }).add(axis.axisGroup);
                }
            }

            // the label is created on init - now move it into place
            if (label && isNumber(x)) {
                label.xy = xy = tick.getLabelPosition(x, y, label, horiz, labelOptions, tickmarkOffset, index, step);

                // Apply show first and show last. If the tick is both first and last, it is
                // a single centered tick, in which case we show the label anyway (#2100).
                if ((tick.isFirst && !tick.isLast && !pick(options.showFirstLabel, 1)) ||
                        (tick.isLast && !tick.isFirst && !pick(options.showLastLabel, 1))) {
                    show = false;

                // Handle label overflow and show or hide accordingly
                } else if (horiz && !axis.isRadial && !labelOptions.step && !labelOptions.rotation && !old && opacity !== 0) {
                    tick.handleOverflow(xy);
                }

                // apply step
                if (step && index % step) {
                    // show those indices dividable by step
                    show = false;
                }

                // Set the new position, and show or hide
                if (show && isNumber(xy.y)) {
                    xy.opacity = opacity;
                    label[tick.isNew ? 'attr' : 'animate'](xy);
                    tick.isNew = false;
                } else {
                    label.attr('y', -9999); // #1338
                }
            }
        },

        /**
         * Destructor for the tick prototype
         */
        destroy: function () {
            destroyObjectProperties(this, this.axis);
        }
    };

    /**
     * The object wrapper for plot lines and plot bands
     * @param {Object} options
     */
    Highcharts.PlotLineOrBand = function (axis, options) {
        this.axis = axis;

        if (options) {
            this.options = options;
            this.id = options.id;
        }
    };

    Highcharts.PlotLineOrBand.prototype = {

        /**
         * Render the plot line or plot band. If it is already existing,
         * move it.
         */
        render: function () {
            var plotLine = this,
                axis = plotLine.axis,
                horiz = axis.horiz,
                options = plotLine.options,
                optionsLabel = options.label,
                label = plotLine.label,
                width = options.width,
                to = options.to,
                from = options.from,
                isBand = defined(from) && defined(to),
                value = options.value,
                dashStyle = options.dashStyle,
                svgElem = plotLine.svgElem,
                path = [],
                addEvent,
                eventType,
                color = options.color,
                zIndex = pick(options.zIndex, 0),
                events = options.events,
                attribs = {},
                renderer = axis.chart.renderer,
                log2lin = axis.log2lin;

            // logarithmic conversion
            if (axis.isLog) {
                from = log2lin(from);
                to = log2lin(to);
                value = log2lin(value);
            }

            // plot line
            if (width) {
                path = axis.getPlotLinePath(value, width);
                attribs = {
                    stroke: color,
                    'stroke-width': width
                };
                if (dashStyle) {
                    attribs.dashstyle = dashStyle;
                }
            } else if (isBand) { // plot band

                path = axis.getPlotBandPath(from, to, options);
                if (color) {
                    attribs.fill = color;
                }
                if (options.borderWidth) {
                    attribs.stroke = options.borderColor;
                    attribs['stroke-width'] = options.borderWidth;
                }
            } else {
                return;
            }
            // zIndex
            attribs.zIndex = zIndex;

            // common for lines and bands
            if (svgElem) {
                if (path) {
                    svgElem.show();
                    svgElem.animate({ d: path });
                } else {
                    svgElem.hide();
                    if (label) {
                        plotLine.label = label = label.destroy();
                    }
                }
            } else if (path && path.length) {
                plotLine.svgElem = svgElem = renderer.path(path)
                    .attr(attribs).add();

                // events
                if (events) {
                    addEvent = function (eventType) {
                        svgElem.on(eventType, function (e) {
                            events[eventType].apply(plotLine, [e]);
                        });
                    };
                    for (eventType in events) {
                        addEvent(eventType);
                    }
                }
            }

            // the plot band/line label
            if (optionsLabel && defined(optionsLabel.text) && path && path.length && 
                    axis.width > 0 && axis.height > 0 && !path.flat) {
                // apply defaults
                optionsLabel = merge({
                    align: horiz && isBand && 'center',
                    x: horiz ? !isBand && 4 : 10,
                    verticalAlign: !horiz && isBand && 'middle',
                    y: horiz ? isBand ? 16 : 10 : isBand ? 6 : -4,
                    rotation: horiz && !isBand && 90
                }, optionsLabel);

                this.renderLabel(optionsLabel, path, isBand, zIndex);

            } else if (label) { // move out of sight
                label.hide();
            }

            // chainable
            return plotLine;
        },

        /**
         * Render and align label for plot line or band.
         */
        renderLabel: function (optionsLabel, path, isBand, zIndex) {
            var plotLine = this,
                label = plotLine.label,
                renderer = plotLine.axis.chart.renderer,
                attribs,
                xs,
                ys,
                x,
                y;

            // add the SVG element
            if (!label) {
                attribs = {
                    align: optionsLabel.textAlign || optionsLabel.align,
                    rotation: optionsLabel.rotation
                };
            
                attribs.zIndex = zIndex;
            
                plotLine.label = label = renderer.text(
                        optionsLabel.text,
                        0,
                        0,
                        optionsLabel.useHTML
                    )
                    .attr(attribs)
                    .css(optionsLabel.style)
                    .add();
            }

            // get the bounding box and align the label
            // #3000 changed to better handle choice between plotband or plotline
            xs = [path[1], path[4], (isBand ? path[6] : path[1])];
            ys = [path[2], path[5], (isBand ? path[7] : path[2])];
            x = arrayMin(xs);
            y = arrayMin(ys);

            label.align(optionsLabel, false, {
                x: x,
                y: y,
                width: arrayMax(xs) - x,
                height: arrayMax(ys) - y
            });
            label.show();
        },

        /**
         * Remove the plot line or band
         */
        destroy: function () {
            // remove it from the lookup
            erase(this.axis.plotLinesAndBands, this);

            delete this.axis;
            destroyObjectProperties(this);
        }
    };

    /**
     * Object with members for extending the Axis prototype
     */

    AxisPlotLineOrBandExtension = {

        /**
         * Create the path for a plot band
         */
        getPlotBandPath: function (from, to) {
            var toPath = this.getPlotLinePath(to, null, null, true),
                path = this.getPlotLinePath(from, null, null, true);

            if (path && toPath) {

                // Flat paths don't need labels (#3836)
                path.flat = path.toString() === toPath.toString();

                path.push(
                    toPath[4],
                    toPath[5],
                    toPath[1],
                    toPath[2]
                );
            } else { // outside the axis area
                path = null;
            }

            return path;
        },

        addPlotBand: function (options) {
            return this.addPlotBandOrLine(options, 'plotBands');
        },

        addPlotLine: function (options) {
            return this.addPlotBandOrLine(options, 'plotLines');
        },

        /**
         * Add a plot band or plot line after render time
         *
         * @param options {Object} The plotBand or plotLine configuration object
         */
        addPlotBandOrLine: function (options, coll) {
            var obj = new Highcharts.PlotLineOrBand(this, options).render(),
                userOptions = this.userOptions;

            if (obj) { // #2189
                // Add it to the user options for exporting and Axis.update
                if (coll) {
                    userOptions[coll] = userOptions[coll] || [];
                    userOptions[coll].push(options);
                }
                this.plotLinesAndBands.push(obj);
            }

            return obj;
        },

        /**
         * Remove a plot band or plot line from the chart by id
         * @param {Object} id
         */
        removePlotBandOrLine: function (id) {
            var plotLinesAndBands = this.plotLinesAndBands,
                options = this.options,
                userOptions = this.userOptions,
                i = plotLinesAndBands.length;
            while (i--) {
                if (plotLinesAndBands[i].id === id) {
                    plotLinesAndBands[i].destroy();
                }
            }
            each([options.plotLines || [], userOptions.plotLines || [], options.plotBands || [], userOptions.plotBands || []], function (arr) {
                i = arr.length;
                while (i--) {
                    if (arr[i].id === id) {
                        erase(arr, arr[i]);
                    }
                }
            });
        }
    };

    /**
     * Create a new axis object
     * @param {Object} chart
     * @param {Object} options
     */
    var Axis = Highcharts.Axis = function () {
        this.init.apply(this, arguments);
    };

    Axis.prototype = {

        /**
         * Default options for the X axis - the Y axis has extended defaults
         */
        defaultOptions: {
            // allowDecimals: null,
            // alternateGridColor: null,
            // categories: [],
            dateTimeLabelFormats: {
                millisecond: '%H:%M:%S.%L',
                second: '%H:%M:%S',
                minute: '%H:%M',
                hour: '%H:%M',
                day: '%e. %b',
                week: '%e. %b',
                month: '%b \'%y',
                year: '%Y'
            },
            endOnTick: false,
            gridLineColor: '#D8D8D8',
            // gridLineDashStyle: 'solid',
            // gridLineWidth: 0,
            // reversed: false,

            labels: {
                enabled: true,
                // rotation: 0,
                // align: 'center',
                // step: null,
                style: {
                    color: '#606060',
                    cursor: 'default',
                    fontSize: '11px'
                },
                x: 0
                //y: undefined
                /*formatter: function () {
                    return this.value;
                },*/
            },
            lineColor: '#C0D0E0',
            lineWidth: 1,
            //linkedTo: null,
            //max: undefined,
            //min: undefined,
            minPadding: 0.01,
            maxPadding: 0.01,
            //minRange: null,
            minorGridLineColor: '#E0E0E0',
            // minorGridLineDashStyle: null,
            minorGridLineWidth: 1,
            minorTickColor: '#A0A0A0',
            //minorTickInterval: null,
            minorTickLength: 2,
            minorTickPosition: 'outside', // inside or outside
            //minorTickWidth: 0,
            //opposite: false,
            //offset: 0,
            //plotBands: [{
            //    events: {},
            //    zIndex: 1,
            //    labels: { align, x, verticalAlign, y, style, rotation, textAlign }
            //}],
            //plotLines: [{
            //    events: {}
            //  dashStyle: {}
            //    zIndex:
            //    labels: { align, x, verticalAlign, y, style, rotation, textAlign }
            //}],
            //reversed: false,
            // showFirstLabel: true,
            // showLastLabel: true,
            startOfWeek: 1,
            startOnTick: false,
            tickColor: '#C0D0E0',
            //tickInterval: null,
            tickLength: 10,
            tickmarkPlacement: 'between', // on or between
            tickPixelInterval: 100,
            tickPosition: 'outside',
            //tickWidth: 1,
            title: {
                //text: null,
                align: 'middle', // low, middle or high
                //margin: 0 for horizontal, 10 for vertical axes,
                //rotation: 0,
                //side: 'outside',
                style: {
                    color: '#707070'
                }
                //x: 0,
                //y: 0
            },
            type: 'linear' // linear, logarithmic or datetime
            //visible: true
        },

        /**
         * This options set extends the defaultOptions for Y axes
         */
        defaultYAxisOptions: {
            endOnTick: true,
            gridLineWidth: 1,
            tickPixelInterval: 72,
            showLastLabel: true,
            labels: {
                x: -8
            },
            lineWidth: 0,
            maxPadding: 0.05,
            minPadding: 0.05,
            startOnTick: true,
            //tickWidth: 0,
            title: {
                rotation: 270,
                text: 'Values'
            },
            stackLabels: {
                enabled: false,
                //align: dynamic,
                //y: dynamic,
                //x: dynamic,
                //verticalAlign: dynamic,
                //textAlign: dynamic,
                //rotation: 0,
                formatter: function () {
                    return Highcharts.numberFormat(this.total, -1);
                },
                style: merge(defaultPlotOptions.line.dataLabels.style, { color: '#000000' })
            }
        },

        /**
         * These options extend the defaultOptions for left axes
         */
        defaultLeftAxisOptions: {
            labels: {
                x: -15
            },
            title: {
                rotation: 270
            }
        },

        /**
         * These options extend the defaultOptions for right axes
         */
        defaultRightAxisOptions: {
            labels: {
                x: 15
            },
            title: {
                rotation: 90
            }
        },

        /**
         * These options extend the defaultOptions for bottom axes
         */
        defaultBottomAxisOptions: {
            labels: {
                autoRotation: [-45],
                x: 0
                // overflow: undefined,
                // staggerLines: null
            },
            title: {
                rotation: 0
            }
        },
        /**
         * These options extend the defaultOptions for top axes
         */
        defaultTopAxisOptions: {
            labels: {
                autoRotation: [-45],
                x: 0
                // overflow: undefined
                // staggerLines: null
            },
            title: {
                rotation: 0
            }
        },

        /**
         * Initialize the axis
         */
        init: function (chart, userOptions) {


            var isXAxis = userOptions.isX,
                axis = this;

            axis.chart = chart;

            // Flag, is the axis horizontal
            axis.horiz = chart.inverted ? !isXAxis : isXAxis;

            // Flag, isXAxis
            axis.isXAxis = isXAxis;
            axis.coll = isXAxis ? 'xAxis' : 'yAxis';

            axis.opposite = userOptions.opposite; // needed in setOptions
            axis.side = userOptions.side || (axis.horiz ?
                    (axis.opposite ? 0 : 2) : // top : bottom
                    (axis.opposite ? 1 : 3));  // right : left

            axis.setOptions(userOptions);


            var options = this.options,
                type = options.type,
                isDatetimeAxis = type === 'datetime';

            axis.labelFormatter = options.labels.formatter || axis.defaultLabelFormatter; // can be overwritten by dynamic format


            // Flag, stagger lines or not
            axis.userOptions = userOptions;

            //axis.axisTitleMargin = UNDEFINED,// = options.title.margin,
            axis.minPixelPadding = 0;

            axis.reversed = options.reversed;
            axis.visible = options.visible !== false;
            axis.zoomEnabled = options.zoomEnabled !== false;

            // Initial categories
            axis.categories = options.categories || type === 'category';
            axis.names = axis.names || []; // Preserve on update (#3830)

            // Elements
            //axis.axisGroup = UNDEFINED;
            //axis.gridGroup = UNDEFINED;
            //axis.axisTitle = UNDEFINED;
            //axis.axisLine = UNDEFINED;

            // Shorthand types
            axis.isLog = type === 'logarithmic';
            axis.isDatetimeAxis = isDatetimeAxis;

            // Flag, if axis is linked to another axis
            axis.isLinked = defined(options.linkedTo);
            // Linked axis.
            //axis.linkedParent = UNDEFINED;

            // Tick positions
            //axis.tickPositions = UNDEFINED; // array containing predefined positions
            // Tick intervals
            //axis.tickInterval = UNDEFINED;
            //axis.minorTickInterval = UNDEFINED;


            // Major ticks
            axis.ticks = {};
            axis.labelEdge = [];
            // Minor ticks
            axis.minorTicks = {};

            // List of plotLines/Bands
            axis.plotLinesAndBands = [];

            // Alternate bands
            axis.alternateBands = {};

            // Axis metrics
            //axis.left = UNDEFINED;
            //axis.top = UNDEFINED;
            //axis.width = UNDEFINED;
            //axis.height = UNDEFINED;
            //axis.bottom = UNDEFINED;
            //axis.right = UNDEFINED;
            //axis.transA = UNDEFINED;
            //axis.transB = UNDEFINED;
            //axis.oldTransA = UNDEFINED;
            axis.len = 0;
            //axis.oldMin = UNDEFINED;
            //axis.oldMax = UNDEFINED;
            //axis.oldUserMin = UNDEFINED;
            //axis.oldUserMax = UNDEFINED;
            //axis.oldAxisLength = UNDEFINED;
            axis.minRange = axis.userMinRange = options.minRange || options.maxZoom;
            axis.range = options.range;
            axis.offset = options.offset || 0;


            // Dictionary for stacks
            axis.stacks = {};
            axis.oldStacks = {};
            axis.stacksTouched = 0;

            // Min and max in the data
            //axis.dataMin = UNDEFINED,
            //axis.dataMax = UNDEFINED,

            // The axis range
            axis.max = null;
            axis.min = null;

            // User set min and max
            //axis.userMin = UNDEFINED,
            //axis.userMax = UNDEFINED,

            // Crosshair options
            axis.crosshair = pick(options.crosshair, splat(chart.options.tooltip.crosshairs)[isXAxis ? 0 : 1], false);
            // Run Axis

            var eventType,
                events = axis.options.events;

            // Register
            if (inArray(axis, chart.axes) === -1) { // don't add it again on Axis.update()
                if (isXAxis && !this.isColorAxis) { // #2713
                    chart.axes.splice(chart.xAxis.length, 0, axis);
                } else {
                    chart.axes.push(axis);
                }

                chart[axis.coll].push(axis);
            }

            axis.series = axis.series || []; // populated by Series

            // inverted charts have reversed xAxes as default
            if (chart.inverted && isXAxis && axis.reversed === UNDEFINED) {
                axis.reversed = true;
            }

            axis.removePlotBand = axis.removePlotBandOrLine;
            axis.removePlotLine = axis.removePlotBandOrLine;


            // register event listeners
            for (eventType in events) {
                addEvent(axis, eventType, events[eventType]);
            }

            // extend logarithmic axis
            if (axis.isLog) {
                axis.val2lin = axis.log2lin;
                axis.lin2val = axis.lin2log;
            }
        },

        /**
         * Merge and set options
         */
        setOptions: function (userOptions) {
            this.options = merge(
                this.defaultOptions,
                this.isXAxis ? {} : this.defaultYAxisOptions,
                [this.defaultTopAxisOptions, this.defaultRightAxisOptions,
                    this.defaultBottomAxisOptions, this.defaultLeftAxisOptions][this.side],
                merge(
                    defaultOptions[this.coll], // if set in setOptions (#1053)
                    userOptions
                )
            );
        },

        /**
         * The default label formatter. The context is a special config object for the label.
         */
        defaultLabelFormatter: function () {
            var axis = this.axis,
                value = this.value,
                categories = axis.categories,
                dateTimeLabelFormat = this.dateTimeLabelFormat,
                numericSymbols = defaultOptions.lang.numericSymbols,
                i = numericSymbols && numericSymbols.length,
                multi,
                ret,
                formatOption = axis.options.labels.format,

                // make sure the same symbol is added for all labels on a linear axis
                numericSymbolDetector = axis.isLog ? value : axis.tickInterval;

            if (formatOption) {
                ret = format(formatOption, this);

            } else if (categories) {
                ret = value;

            } else if (dateTimeLabelFormat) { // datetime axis
                ret = dateFormat(dateTimeLabelFormat, value);

            } else if (i && numericSymbolDetector >= 1000) {
                // Decide whether we should add a numeric symbol like k (thousands) or M (millions).
                // If we are to enable this in tooltip or other places as well, we can move this
                // logic to the numberFormatter and enable it by a parameter.
                while (i-- && ret === UNDEFINED) {
                    multi = Math.pow(1000, i + 1);
                    if (numericSymbolDetector >= multi && (value * 10) % multi === 0 && numericSymbols[i] !== null) {
                        ret = Highcharts.numberFormat(value / multi, -1) + numericSymbols[i];
                    }
                }
            }

            if (ret === UNDEFINED) {
                if (mathAbs(value) >= 10000) { // add thousands separators
                    ret = Highcharts.numberFormat(value, -1);

                } else { // small numbers
                    ret = Highcharts.numberFormat(value, -1, UNDEFINED, ''); // #2466
                }
            }

            return ret;
        },

        /**
         * Get the minimum and maximum for the series of each axis
         */
        getSeriesExtremes: function () {
            var axis = this,
                chart = axis.chart;

            axis.hasVisibleSeries = false;

            // Reset properties in case we're redrawing (#3353)
            axis.dataMin = axis.dataMax = axis.threshold = null;
            axis.softThreshold = !axis.isXAxis;

            if (axis.buildStacks) {
                axis.buildStacks();
            }

            // loop through this axis' series
            each(axis.series, function (series) {

                if (series.visible || !chart.options.chart.ignoreHiddenSeries) {

                    var seriesOptions = series.options,
                        xData,
                        threshold = seriesOptions.threshold,
                        seriesDataMin,
                        seriesDataMax;

                    axis.hasVisibleSeries = true;

                    // Validate threshold in logarithmic axes
                    if (axis.isLog && threshold <= 0) {
                        threshold = null;
                    }

                    // Get dataMin and dataMax for X axes
                    if (axis.isXAxis) {
                        xData = series.xData;
                        if (xData.length) {
                            // If xData contains values which is not numbers, then filter them out.
                            // To prevent performance hit, we only do this after we have already
                            // found seriesDataMin because in most cases all data is valid. #5234.
                            seriesDataMin = arrayMin(xData);
                            if (!isNumber(seriesDataMin) && !(seriesDataMin instanceof Date)) { // Date for #5010
                                xData = grep(xData, function (x) {
                                    return isNumber(x);
                                });
                                seriesDataMin = arrayMin(xData); // Do it again with valid data
                            }

                            axis.dataMin = mathMin(pick(axis.dataMin, xData[0]), seriesDataMin);
                            axis.dataMax = mathMax(pick(axis.dataMax, xData[0]), arrayMax(xData));
                        
                        }

                    // Get dataMin and dataMax for Y axes, as well as handle stacking and processed data
                    } else {

                        // Get this particular series extremes
                        series.getExtremes();
                        seriesDataMax = series.dataMax;
                        seriesDataMin = series.dataMin;

                        // Get the dataMin and dataMax so far. If percentage is used, the min and max are
                        // always 0 and 100. If seriesDataMin and seriesDataMax is null, then series
                        // doesn't have active y data, we continue with nulls
                        if (defined(seriesDataMin) && defined(seriesDataMax)) {
                            axis.dataMin = mathMin(pick(axis.dataMin, seriesDataMin), seriesDataMin);
                            axis.dataMax = mathMax(pick(axis.dataMax, seriesDataMax), seriesDataMax);
                        }

                        // Adjust to threshold
                        if (defined(threshold)) {
                            axis.threshold = threshold;
                        }
                        // If any series has a hard threshold, it takes precedence
                        if (!seriesOptions.softThreshold || axis.isLog) {
                            axis.softThreshold = false;
                        }
                    }
                }
            });
        },

        /**
         * Translate from axis value to pixel position on the chart, or back
         *
         */
        translate: function (val, backwards, cvsCoord, old, handleLog, pointPlacement) {
            var axis = this.linkedParent || this, // #1417
                sign = 1,
                cvsOffset = 0,
                localA = old ? axis.oldTransA : axis.transA,
                localMin = old ? axis.oldMin : axis.min,
                returnValue,
                minPixelPadding = axis.minPixelPadding,
                doPostTranslate = (axis.isOrdinal || axis.isBroken || (axis.isLog && handleLog)) && axis.lin2val;

            if (!localA) {
                localA = axis.transA;
            }

            // In vertical axes, the canvas coordinates start from 0 at the top like in
            // SVG.
            if (cvsCoord) {
                sign *= -1; // canvas coordinates inverts the value
                cvsOffset = axis.len;
            }

            // Handle reversed axis
            if (axis.reversed) {
                sign *= -1;
                cvsOffset -= sign * (axis.sector || axis.len);
            }

            // From pixels to value
            if (backwards) { // reverse translation

                val = val * sign + cvsOffset;
                val -= minPixelPadding;
                returnValue = val / localA + localMin; // from chart pixel to value
                if (doPostTranslate) { // log and ordinal axes
                    returnValue = axis.lin2val(returnValue);
                }

            // From value to pixels
            } else {
                if (doPostTranslate) { // log and ordinal axes
                    val = axis.val2lin(val);
                }
                if (pointPlacement === 'between') {
                    pointPlacement = 0.5;
                }
                returnValue = sign * (val - localMin) * localA + cvsOffset + (sign * minPixelPadding) +
                    (isNumber(pointPlacement) ? localA * pointPlacement * axis.pointRange : 0);
            }

            return returnValue;
        },

        /**
         * Utility method to translate an axis value to pixel position.
         * @param {Number} value A value in terms of axis units
         * @param {Boolean} paneCoordinates Whether to return the pixel coordinate relative to the chart
         *        or just the axis/pane itself.
         */
        toPixels: function (value, paneCoordinates) {
            return this.translate(value, false, !this.horiz, null, true) + (paneCoordinates ? 0 : this.pos);
        },

        /*
         * Utility method to translate a pixel position in to an axis value
         * @param {Number} pixel The pixel value coordinate
         * @param {Boolean} paneCoordiantes Whether the input pixel is relative to the chart or just the
         *        axis/pane itself.
         */
        toValue: function (pixel, paneCoordinates) {
            return this.translate(pixel - (paneCoordinates ? 0 : this.pos), true, !this.horiz, null, true);
        },

        /**
         * Create the path for a plot line that goes from the given value on
         * this axis, across the plot to the opposite side
         * @param {Number} value
         * @param {Number} lineWidth Used for calculation crisp line
         * @param {Number] old Use old coordinates (for resizing and rescaling)
         */
        getPlotLinePath: function (value, lineWidth, old, force, translatedValue) {
            var axis = this,
                chart = axis.chart,
                axisLeft = axis.left,
                axisTop = axis.top,
                x1,
                y1,
                x2,
                y2,
                cHeight = (old && chart.oldChartHeight) || chart.chartHeight,
                cWidth = (old && chart.oldChartWidth) || chart.chartWidth,
                skip,
                transB = axis.transB,
                /**
                 * Check if x is between a and b. If not, either move to a/b or skip,
                 * depending on the force parameter.
                 */
                between = function (x, a, b) {
                    if (x < a || x > b) {
                        if (force) {
                            x = mathMin(mathMax(a, x), b);
                        } else {
                            skip = true;
                        }
                    }
                    return x;
                };

            translatedValue = pick(translatedValue, axis.translate(value, null, null, old));
            x1 = x2 = mathRound(translatedValue + transB);
            y1 = y2 = mathRound(cHeight - translatedValue - transB);
            if (!isNumber(translatedValue)) { // no min or max
                skip = true;

            } else if (axis.horiz) {
                y1 = axisTop;
                y2 = cHeight - axis.bottom;
                x1 = x2 = between(x1, axisLeft, axisLeft + axis.width);
            } else {
                x1 = axisLeft;
                x2 = cWidth - axis.right;
                y1 = y2 = between(y1, axisTop, axisTop + axis.height);
            }
            return skip && !force ?
                null :
                chart.renderer.crispLine([M, x1, y1, L, x2, y2], lineWidth || 1);
        },

        /**
         * Set the tick positions of a linear axis to round values like whole tens or every five.
         */
        getLinearTickPositions: function (tickInterval, min, max) {
            var pos,
                lastPos,
                roundedMin = correctFloat(mathFloor(min / tickInterval) * tickInterval),
                roundedMax = correctFloat(mathCeil(max / tickInterval) * tickInterval),
                tickPositions = [];

            // For single points, add a tick regardless of the relative position (#2662)
            if (min === max && isNumber(min)) {
                return [min];
            }

            // Populate the intermediate values
            pos = roundedMin;
            while (pos <= roundedMax) {

                // Place the tick on the rounded value
                tickPositions.push(pos);

                // Always add the raw tickInterval, not the corrected one.
                pos = correctFloat(pos + tickInterval);

                // If the interval is not big enough in the current min - max range to actually increase
                // the loop variable, we need to break out to prevent endless loop. Issue #619
                if (pos === lastPos) {
                    break;
                }

                // Record the last value
                lastPos = pos;
            }
            return tickPositions;
        },

        /**
         * Return the minor tick positions. For logarithmic axes, reuse the same logic
         * as for major ticks.
         */
        getMinorTickPositions: function () {
            var axis = this,
                options = axis.options,
                tickPositions = axis.tickPositions,
                minorTickInterval = axis.minorTickInterval,
                minorTickPositions = [],
                pos,
                i,
                pointRangePadding = axis.pointRangePadding || 0,
                min = axis.min - pointRangePadding, // #1498
                max = axis.max + pointRangePadding, // #1498
                range = max - min,
                len;

            // If minor ticks get too dense, they are hard to read, and may cause long running script. So we don't draw them.
            if (range && range / minorTickInterval < axis.len / 3) { // #3875

                if (axis.isLog) {
                    len = tickPositions.length;
                    for (i = 1; i < len; i++) {
                        minorTickPositions = minorTickPositions.concat(
                            axis.getLogTickPositions(minorTickInterval, tickPositions[i - 1], tickPositions[i], true)
                        );
                    }
                } else if (axis.isDatetimeAxis && options.minorTickInterval === 'auto') { // #1314
                    minorTickPositions = minorTickPositions.concat(
                        axis.getTimeTicks(
                            axis.normalizeTimeTickInterval(minorTickInterval),
                            min,
                            max,
                            options.startOfWeek
                        )
                    );
                } else {
                    for (pos = min + (tickPositions[0] - min) % minorTickInterval; pos <= max; pos += minorTickInterval) {
                        minorTickPositions.push(pos);
                    }
                }
            }

            if (minorTickPositions.length !== 0) { // don't change the extremes, when there is no minor ticks
                axis.trimTicks(minorTickPositions, options.startOnTick, options.endOnTick); // #3652 #3743 #1498
            }
            return minorTickPositions;
        },

        /**
         * Adjust the min and max for the minimum range. Keep in mind that the series data is
         * not yet processed, so we don't have information on data cropping and grouping, or
         * updated axis.pointRange or series.pointRange. The data can't be processed until
         * we have finally established min and max.
         */
        adjustForMinRange: function () {
            var axis = this,
                options = axis.options,
                min = axis.min,
                max = axis.max,
                zoomOffset,
                spaceAvailable = axis.dataMax - axis.dataMin >= axis.minRange,
                closestDataRange,
                i,
                distance,
                xData,
                loopLength,
                minArgs,
                maxArgs,
                minRange;

            // Set the automatic minimum range based on the closest point distance
            if (axis.isXAxis && axis.minRange === UNDEFINED && !axis.isLog) {

                if (defined(options.min) || defined(options.max)) {
                    axis.minRange = null; // don't do this again

                } else {

                    // Find the closest distance between raw data points, as opposed to
                    // closestPointRange that applies to processed points (cropped and grouped)
                    each(axis.series, function (series) {
                        xData = series.xData;
                        loopLength = series.xIncrement ? 1 : xData.length - 1;
                        for (i = loopLength; i > 0; i--) {
                            distance = xData[i] - xData[i - 1];
                            if (closestDataRange === UNDEFINED || distance < closestDataRange) {
                                closestDataRange = distance;
                            }
                        }
                    });
                    axis.minRange = mathMin(closestDataRange * 5, axis.dataMax - axis.dataMin);
                }
            }

            // if minRange is exceeded, adjust
            if (max - min < axis.minRange) {
                minRange = axis.minRange;
                zoomOffset = (minRange - max + min) / 2;

                // if min and max options have been set, don't go beyond it
                minArgs = [min - zoomOffset, pick(options.min, min - zoomOffset)];
                if (spaceAvailable) { // if space is available, stay within the data range
                    minArgs[2] = axis.dataMin;
                }
                min = arrayMax(minArgs);

                maxArgs = [min + minRange, pick(options.max, min + minRange)];
                if (spaceAvailable) { // if space is availabe, stay within the data range
                    maxArgs[2] = axis.dataMax;
                }

                max = arrayMin(maxArgs);

                // now if the max is adjusted, adjust the min back
                if (max - min < minRange) {
                    minArgs[0] = max - minRange;
                    minArgs[1] = pick(options.min, max - minRange);
                    min = arrayMax(minArgs);
                }
            }

            // Record modified extremes
            axis.min = min;
            axis.max = max;
        },

        /**
         * Find the closestPointRange across all series
         */
        getClosest: function () {
            var ret;
            each(this.series, function (series) {
                var seriesClosest = series.closestPointRange;
                if (!series.noSharedTooltip && defined(seriesClosest)) {
                    ret = defined(ret) ?
                        mathMin(ret, seriesClosest) :
                        seriesClosest;
                }
            });
            return ret;
        },

        /**
         * Update translation information
         */
        setAxisTranslation: function (saveOld) {
            var axis = this,
                range = axis.max - axis.min,
                pointRange = axis.axisPointRange || 0,
                closestPointRange,
                minPointOffset = 0,
                pointRangePadding = 0,
                linkedParent = axis.linkedParent,
                ordinalCorrection,
                hasCategories = !!axis.categories,
                transA = axis.transA,
                isXAxis = axis.isXAxis;

            // Adjust translation for padding. Y axis with categories need to go through the same (#1784).
            if (isXAxis || hasCategories || pointRange) {
                if (linkedParent) {
                    minPointOffset = linkedParent.minPointOffset;
                    pointRangePadding = linkedParent.pointRangePadding;

                } else {
                
                    // Get the closest points
                    closestPointRange = axis.getClosest();

                    each(axis.series, function (series) {
                        var seriesPointRange = hasCategories ? 
                            1 : 
                            (isXAxis ? 
                                pick(series.options.pointRange, closestPointRange, 0) : 
                                (axis.axisPointRange || 0)), // #2806
                            pointPlacement = series.options.pointPlacement;

                        pointRange = mathMax(pointRange, seriesPointRange);

                        if (!axis.single) {
                            // minPointOffset is the value padding to the left of the axis in order to make
                            // room for points with a pointRange, typically columns. When the pointPlacement option
                            // is 'between' or 'on', this padding does not apply.
                            minPointOffset = mathMax(
                                minPointOffset,
                                isString(pointPlacement) ? 0 : seriesPointRange / 2
                            );

                            // Determine the total padding needed to the length of the axis to make room for the
                            // pointRange. If the series' pointPlacement is 'on', no padding is added.
                            pointRangePadding = mathMax(
                                pointRangePadding,
                                pointPlacement === 'on' ? 0 : seriesPointRange
                            );
                        }
                    });
                }

                // Record minPointOffset and pointRangePadding
                ordinalCorrection = axis.ordinalSlope && closestPointRange ? axis.ordinalSlope / closestPointRange : 1; // #988, #1853
                axis.minPointOffset = minPointOffset = minPointOffset * ordinalCorrection;
                axis.pointRangePadding = pointRangePadding = pointRangePadding * ordinalCorrection;

                // pointRange means the width reserved for each point, like in a column chart
                axis.pointRange = mathMin(pointRange, range);

                // closestPointRange means the closest distance between points. In columns
                // it is mostly equal to pointRange, but in lines pointRange is 0 while closestPointRange
                // is some other value
                if (isXAxis) {
                    axis.closestPointRange = closestPointRange;
                }
            }

            // Secondary values
            if (saveOld) {
                axis.oldTransA = transA;
            }
            axis.translationSlope = axis.transA = transA = axis.len / ((range + pointRangePadding) || 1);
            axis.transB = axis.horiz ? axis.left : axis.bottom; // translation addend
            axis.minPixelPadding = transA * minPointOffset;
        },

        minFromRange: function () {
            return this.max - this.range;
        },

        /**
         * Set the tick positions to round values and optionally extend the extremes
         * to the nearest tick
         */
        setTickInterval: function (secondPass) {
            var axis = this,
                chart = axis.chart,
                options = axis.options,
                isLog = axis.isLog,
                log2lin = axis.log2lin,
                isDatetimeAxis = axis.isDatetimeAxis,
                isXAxis = axis.isXAxis,
                isLinked = axis.isLinked,
                maxPadding = options.maxPadding,
                minPadding = options.minPadding,
                length,
                linkedParentExtremes,
                tickIntervalOption = options.tickInterval,
                minTickInterval,
                tickPixelIntervalOption = options.tickPixelInterval,
                categories = axis.categories,
                threshold = axis.threshold,
                softThreshold = axis.softThreshold,
                thresholdMin,
                thresholdMax,
                hardMin,
                hardMax;

            if (!isDatetimeAxis && !categories && !isLinked) {
                this.getTickAmount();
            }

            // Min or max set either by zooming/setExtremes or initial options
            hardMin = pick(axis.userMin, options.min);
            hardMax = pick(axis.userMax, options.max);

            // Linked axis gets the extremes from the parent axis
            if (isLinked) {
                axis.linkedParent = chart[axis.coll][options.linkedTo];
                linkedParentExtremes = axis.linkedParent.getExtremes();
                axis.min = pick(linkedParentExtremes.min, linkedParentExtremes.dataMin);
                axis.max = pick(linkedParentExtremes.max, linkedParentExtremes.dataMax);
                if (options.type !== axis.linkedParent.options.type) {
                    error(11, 1); // Can't link axes of different type
                }

            // Initial min and max from the extreme data values
            } else {

                // Adjust to hard threshold
                if (!softThreshold && defined(threshold)) {
                    if (axis.dataMin >= threshold) {
                        thresholdMin = threshold;
                        minPadding = 0;
                    } else if (axis.dataMax <= threshold) {
                        thresholdMax = threshold;
                        maxPadding = 0;
                    }
                }

                axis.min = pick(hardMin, thresholdMin, axis.dataMin);
                axis.max = pick(hardMax, thresholdMax, axis.dataMax);

            }

            if (isLog) {
                if (!secondPass && mathMin(axis.min, pick(axis.dataMin, axis.min)) <= 0) { // #978
                    error(10, 1); // Can't plot negative values on log axis
                }
                // The correctFloat cures #934, float errors on full tens. But it
                // was too aggressive for #4360 because of conversion back to lin,
                // therefore use precision 15.
                axis.min = correctFloat(log2lin(axis.min), 15);
                axis.max = correctFloat(log2lin(axis.max), 15);
            }

            // handle zoomed range
            if (axis.range && defined(axis.max)) {
                axis.userMin = axis.min = hardMin = mathMax(axis.min, axis.minFromRange()); // #618
                axis.userMax = hardMax = axis.max;

                axis.range = null;  // don't use it when running setExtremes
            }

            // Hook for Highstock Scroller. Consider combining with beforePadding.
            fireEvent(axis, 'foundExtremes');

            // Hook for adjusting this.min and this.max. Used by bubble series.
            if (axis.beforePadding) {
                axis.beforePadding();
            }

            // adjust min and max for the minimum range
            axis.adjustForMinRange();

            // Pad the values to get clear of the chart's edges. To avoid tickInterval taking the padding
            // into account, we do this after computing tick interval (#1337).
            if (!categories && !axis.axisPointRange && !axis.usePercentage && !isLinked && defined(axis.min) && defined(axis.max)) {
                length = axis.max - axis.min;
                if (length) {
                    if (!defined(hardMin) && minPadding) {
                        axis.min -= length * minPadding;
                    }
                    if (!defined(hardMax)  && maxPadding) {
                        axis.max += length * maxPadding;
                    }
                }
            }

            // Stay within floor and ceiling
            if (isNumber(options.floor)) {
                axis.min = mathMax(axis.min, options.floor);
            }
            if (isNumber(options.ceiling)) {
                axis.max = mathMin(axis.max, options.ceiling);
            }

            // When the threshold is soft, adjust the extreme value only if
            // the data extreme and the padded extreme land on either side of the threshold. For example,
            // a series of [0, 1, 2, 3] would make the yAxis add a tick for -1 because of the
            // default minPadding and startOnTick options. This is prevented by the softThreshold
            // option.
            if (softThreshold && defined(axis.dataMin)) {
                threshold = threshold || 0;
                if (!defined(hardMin) && axis.min < threshold && axis.dataMin >= threshold) {
                    axis.min = threshold;
                } else if (!defined(hardMax) && axis.max > threshold && axis.dataMax <= threshold) {
                    axis.max = threshold;
                }
            }


            // get tickInterval
            if (axis.min === axis.max || axis.min === undefined || axis.max === undefined) {
                axis.tickInterval = 1;
            } else if (isLinked && !tickIntervalOption &&
                    tickPixelIntervalOption === axis.linkedParent.options.tickPixelInterval) {
                axis.tickInterval = tickIntervalOption = axis.linkedParent.tickInterval;
            } else {
                axis.tickInterval = pick(
                    tickIntervalOption,
                    this.tickAmount ? ((axis.max - axis.min) / mathMax(this.tickAmount - 1, 1)) : undefined,
                    categories ? // for categoried axis, 1 is default, for linear axis use tickPix
                        1 :
                        // don't let it be more than the data range
                        (axis.max - axis.min) * tickPixelIntervalOption / mathMax(axis.len, tickPixelIntervalOption)
                );
            }

            // Now we're finished detecting min and max, crop and group series data. This
            // is in turn needed in order to find tick positions in ordinal axes.
            if (isXAxis && !secondPass) {
                each(axis.series, function (series) {
                    series.processData(axis.min !== axis.oldMin || axis.max !== axis.oldMax);
                });
            }

            // set the translation factor used in translate function
            axis.setAxisTranslation(true);

            // hook for ordinal axes and radial axes
            if (axis.beforeSetTickPositions) {
                axis.beforeSetTickPositions();
            }

            // hook for extensions, used in Highstock ordinal axes
            if (axis.postProcessTickInterval) {
                axis.tickInterval = axis.postProcessTickInterval(axis.tickInterval);
            }

            // In column-like charts, don't cramp in more ticks than there are points (#1943, #4184)
            if (axis.pointRange && !tickIntervalOption) {
                axis.tickInterval = mathMax(axis.pointRange, axis.tickInterval);
            }

            // Before normalizing the tick interval, handle minimum tick interval. This applies only if tickInterval is not defined.
            minTickInterval = pick(options.minTickInterval, axis.isDatetimeAxis && axis.closestPointRange);
            if (!tickIntervalOption && axis.tickInterval < minTickInterval) {
                axis.tickInterval = minTickInterval;
            }

            // for linear axes, get magnitude and normalize the interval
            if (!isDatetimeAxis && !isLog && !tickIntervalOption) {
                axis.tickInterval = normalizeTickInterval(
                    axis.tickInterval,
                    null,
                    getMagnitude(axis.tickInterval),
                    // If the tick interval is between 0.5 and 5 and the axis max is in the order of
                    // thousands, chances are we are dealing with years. Don't allow decimals. #3363.
                    pick(options.allowDecimals, !(axis.tickInterval > 0.5 && axis.tickInterval < 5 && axis.max > 1000 && axis.max < 9999)),
                    !!this.tickAmount
                );
            }

            // Prevent ticks from getting so close that we can't draw the labels
            if (!this.tickAmount && this.len) { // Color axis with disabled legend has no length
                axis.tickInterval = axis.unsquish();
            }

            this.setTickPositions();
        },

        /**
         * Now we have computed the normalized tickInterval, get the tick positions
         */
        setTickPositions: function () {

            var options = this.options,
                tickPositions,
                tickPositionsOption = options.tickPositions,
                tickPositioner = options.tickPositioner,
                startOnTick = options.startOnTick,
                endOnTick = options.endOnTick,
                single;

            // Set the tickmarkOffset
            this.tickmarkOffset = (this.categories && options.tickmarkPlacement === 'between' &&
                this.tickInterval === 1) ? 0.5 : 0; // #3202


            // get minorTickInterval
            this.minorTickInterval = options.minorTickInterval === 'auto' && this.tickInterval ?
                this.tickInterval / 5 : options.minorTickInterval;

            // Find the tick positions
            this.tickPositions = tickPositions = tickPositionsOption && tickPositionsOption.slice(); // Work on a copy (#1565)
            if (!tickPositions) {

                if (this.isDatetimeAxis) {
                    tickPositions = this.getTimeTicks(
                        this.normalizeTimeTickInterval(this.tickInterval, options.units),
                        this.min,
                        this.max,
                        options.startOfWeek,
                        this.ordinalPositions,
                        this.closestPointRange,
                        true
                    );
                } else if (this.isLog) {
                    tickPositions = this.getLogTickPositions(this.tickInterval, this.min, this.max);
                } else {
                    tickPositions = this.getLinearTickPositions(this.tickInterval, this.min, this.max);
                }

                // Too dense ticks, keep only the first and last (#4477)
                if (tickPositions.length > this.len) {
                    tickPositions = [tickPositions[0], tickPositions.pop()];
                }

                this.tickPositions = tickPositions;

                // Run the tick positioner callback, that allows modifying auto tick positions.
                if (tickPositioner) {
                    tickPositioner = tickPositioner.apply(this, [this.min, this.max]);
                    if (tickPositioner) {
                        this.tickPositions = tickPositions = tickPositioner;
                    }
                }

            }

            if (!this.isLinked) {

                // reset min/max or remove extremes based on start/end on tick
                this.trimTicks(tickPositions, startOnTick, endOnTick);

                // When there is only one point, or all points have the same value on this axis, then min
                // and max are equal and tickPositions.length is 0 or 1. In this case, add some padding
                // in order to center the point, but leave it with one tick. #1337.
                if (this.min === this.max && defined(this.min) && !this.tickAmount) {
                    // Substract half a unit (#2619, #2846, #2515, #3390)
                    single = true;
                    this.min -= 0.5;
                    this.max += 0.5;
                }
                this.single = single;

                if (!tickPositionsOption && !tickPositioner) {
                    this.adjustTickAmount();
                }
            }
        },

        /**
         * Handle startOnTick and endOnTick by either adapting to padding min/max or rounded min/max
         */
        trimTicks: function (tickPositions, startOnTick, endOnTick) {
            var roundedMin = tickPositions[0],
                roundedMax = tickPositions[tickPositions.length - 1],
                minPointOffset = this.minPointOffset || 0;

            if (startOnTick) {
                this.min = roundedMin;
            } else {
                while (this.min - minPointOffset > tickPositions[0]) {
                    tickPositions.shift();
                }
            }

            if (endOnTick) {
                this.max = roundedMax;
            } else {
                while (this.max + minPointOffset < tickPositions[tickPositions.length - 1]) {
                    tickPositions.pop();
                }
            }

            // If no tick are left, set one tick in the middle (#3195)
            if (tickPositions.length === 0 && defined(roundedMin)) {
                tickPositions.push((roundedMax + roundedMin) / 2);
            }
        },

        /**
         * Check if there are multiple axes in the same pane
         * @returns {Boolean} There are other axes
         */
        alignToOthers: function () {
            var others = {}, // Whether there is another axis to pair with this one
                hasOther,
                options = this.options;

            if (this.chart.options.chart.alignTicks !== false && options.alignTicks !== false) {
                each(this.chart[this.coll], function (axis) {
                    var otherOptions = axis.options,
                        horiz = axis.horiz,
                        key = [
                            horiz ? otherOptions.left : otherOptions.top, 
                            otherOptions.width,
                            otherOptions.height, 
                            otherOptions.pane
                        ].join(',');


                    if (axis.series.length) { // #4442
                        if (others[key]) {
                            hasOther = true; // #4201
                        } else {
                            others[key] = 1;
                        }
                    }
                });
            }
            return hasOther;
        },

        /**
         * Set the max ticks of either the x and y axis collection
         */
        getTickAmount: function () {
            var options = this.options,
                tickAmount = options.tickAmount,
                tickPixelInterval = options.tickPixelInterval;

            if (!defined(options.tickInterval) && this.len < tickPixelInterval && !this.isRadial &&
                    !this.isLog && options.startOnTick && options.endOnTick) {
                tickAmount = 2;
            }

            if (!tickAmount && this.alignToOthers()) {
                // Add 1 because 4 tick intervals require 5 ticks (including first and last)
                tickAmount = mathCeil(this.len / tickPixelInterval) + 1;
            }

            // For tick amounts of 2 and 3, compute five ticks and remove the intermediate ones. This
            // prevents the axis from adding ticks that are too far away from the data extremes.
            if (tickAmount < 4) {
                this.finalTickAmt = tickAmount;
                tickAmount = 5;
            }

            this.tickAmount = tickAmount;
        },

        /**
         * When using multiple axes, adjust the number of ticks to match the highest
         * number of ticks in that group
         */
        adjustTickAmount: function () {
            var tickInterval = this.tickInterval,
                tickPositions = this.tickPositions,
                tickAmount = this.tickAmount,
                finalTickAmt = this.finalTickAmt,
                currentTickAmount = tickPositions && tickPositions.length,
                i,
                len;

            if (currentTickAmount < tickAmount) {
                while (tickPositions.length < tickAmount) {
                    tickPositions.push(correctFloat(
                        tickPositions[tickPositions.length - 1] + tickInterval
                    ));
                }
                this.transA *= (currentTickAmount - 1) / (tickAmount - 1);
                this.max = tickPositions[tickPositions.length - 1];

            // We have too many ticks, run second pass to try to reduce ticks
            } else if (currentTickAmount > tickAmount) {
                this.tickInterval *= 2;
                this.setTickPositions();
            }

            // The finalTickAmt property is set in getTickAmount
            if (defined(finalTickAmt)) {
                i = len = tickPositions.length;
                while (i--) {
                    if (
                        (finalTickAmt === 3 && i % 2 === 1) || // Remove every other tick
                        (finalTickAmt <= 2 && i > 0 && i < len - 1) // Remove all but first and last
                    ) {
                        tickPositions.splice(i, 1);
                    }
                }
                this.finalTickAmt = UNDEFINED;
            }
        },

        /**
         * Set the scale based on data min and max, user set min and max or options
         *
         */
        setScale: function () {
            var axis = this,
                isDirtyData,
                isDirtyAxisLength;

            axis.oldMin = axis.min;
            axis.oldMax = axis.max;
            axis.oldAxisLength = axis.len;

            // set the new axisLength
            axis.setAxisSize();
            //axisLength = horiz ? axisWidth : axisHeight;
            isDirtyAxisLength = axis.len !== axis.oldAxisLength;

            // is there new data?
            each(axis.series, function (series) {
                if (series.isDirtyData || series.isDirty ||
                        series.xAxis.isDirty) { // when x axis is dirty, we need new data extremes for y as well
                    isDirtyData = true;
                }
            });

            // do we really need to go through all this?
            if (isDirtyAxisLength || isDirtyData || axis.isLinked || axis.forceRedraw ||
                axis.userMin !== axis.oldUserMin || axis.userMax !== axis.oldUserMax || axis.alignToOthers()) {

                if (axis.resetStacks) {
                    axis.resetStacks();
                }

                axis.forceRedraw = false;

                // get data extremes if needed
                axis.getSeriesExtremes();

                // get fixed positions based on tickInterval
                axis.setTickInterval();

                // record old values to decide whether a rescale is necessary later on (#540)
                axis.oldUserMin = axis.userMin;
                axis.oldUserMax = axis.userMax;

                // Mark as dirty if it is not already set to dirty and extremes have changed. #595.
                if (!axis.isDirty) {
                    axis.isDirty = isDirtyAxisLength || axis.min !== axis.oldMin || axis.max !== axis.oldMax;
                }
            } else if (axis.cleanStacks) {
                axis.cleanStacks();
            }
        },

        /**
         * Set the extremes and optionally redraw
         * @param {Number} newMin
         * @param {Number} newMax
         * @param {Boolean} redraw
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         * @param {Object} eventArguments
         *
         */
        setExtremes: function (newMin, newMax, redraw, animation, eventArguments) {
            var axis = this,
                chart = axis.chart;

            redraw = pick(redraw, true); // defaults to true

            each(axis.series, function (serie) {
                delete serie.kdTree;
            });

            // Extend the arguments with min and max
            eventArguments = extend(eventArguments, {
                min: newMin,
                max: newMax
            });

            // Fire the event
            fireEvent(axis, 'setExtremes', eventArguments, function () { // the default event handler

                axis.userMin = newMin;
                axis.userMax = newMax;
                axis.eventArgs = eventArguments;

                if (redraw) {
                    chart.redraw(animation);
                }
            });
        },

        /**
         * Overridable method for zooming chart. Pulled out in a separate method to allow overriding
         * in stock charts.
         */
        zoom: function (newMin, newMax) {
            var dataMin = this.dataMin,
                dataMax = this.dataMax,
                options = this.options,
                min = mathMin(dataMin, pick(options.min, dataMin)),
                max = mathMax(dataMax, pick(options.max, dataMax));

            // Prevent pinch zooming out of range. Check for defined is for #1946. #1734.
            if (!this.allowZoomOutside) {
                if (defined(dataMin) && newMin <= min) {
                    newMin = min;
                }
                if (defined(dataMax) && newMax >= max) {
                    newMax = max;
                }
            }

            // In full view, displaying the reset zoom button is not required
            this.displayBtn = newMin !== UNDEFINED || newMax !== UNDEFINED;

            // Do it
            this.setExtremes(
                newMin,
                newMax,
                false,
                UNDEFINED,
                { trigger: 'zoom' }
            );
            return true;
        },

        /**
         * Update the axis metrics
         */
        setAxisSize: function () {
            var chart = this.chart,
                options = this.options,
                offsetLeft = options.offsetLeft || 0,
                offsetRight = options.offsetRight || 0,
                horiz = this.horiz,
                width = pick(options.width, chart.plotWidth - offsetLeft + offsetRight),
                height = pick(options.height, chart.plotHeight),
                top = pick(options.top, chart.plotTop),
                left = pick(options.left, chart.plotLeft + offsetLeft),
                percentRegex = /%$/;

            // Check for percentage based input values. Rounding fixes problems with
            // column overflow and plot line filtering (#4898, #4899)
            if (percentRegex.test(height)) {
                height = Math.round(parseFloat(height) / 100 * chart.plotHeight);
            }
            if (percentRegex.test(top)) {
                top = Math.round(parseFloat(top) / 100 * chart.plotHeight + chart.plotTop);
            }

            // Expose basic values to use in Series object and navigator
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
            this.bottom = chart.chartHeight - height - top;
            this.right = chart.chartWidth - width - left;

            // Direction agnostic properties
            this.len = mathMax(horiz ? width : height, 0); // mathMax fixes #905
            this.pos = horiz ? left : top; // distance from SVG origin
        },

        /**
         * Get the actual axis extremes
         */
        getExtremes: function () {
            var axis = this,
                isLog = axis.isLog,
                lin2log = axis.lin2log;

            return {
                min: isLog ? correctFloat(lin2log(axis.min)) : axis.min,
                max: isLog ? correctFloat(lin2log(axis.max)) : axis.max,
                dataMin: axis.dataMin,
                dataMax: axis.dataMax,
                userMin: axis.userMin,
                userMax: axis.userMax
            };
        },

        /**
         * Get the zero plane either based on zero or on the min or max value.
         * Used in bar and area plots
         */
        getThreshold: function (threshold) {
            var axis = this,
                isLog = axis.isLog,
                lin2log = axis.lin2log,
                realMin = isLog ? lin2log(axis.min) : axis.min,
                realMax = isLog ? lin2log(axis.max) : axis.max;

            // With a threshold of null, make the columns/areas rise from the top or bottom
            // depending on the value, assuming an actual threshold of 0 (#4233).
            if (threshold === null) {
                threshold = realMax < 0 ? realMax : realMin;
            } else if (realMin > threshold) {
                threshold = realMin;
            } else if (realMax < threshold) {
                threshold = realMax;
            }

            return axis.translate(threshold, 0, 1, 0, 1);
        },

        /**
         * Compute auto alignment for the axis label based on which side the axis is on
         * and the given rotation for the label
         */
        autoLabelAlign: function (rotation) {
            var ret,
                angle = (pick(rotation, 0) - (this.side * 90) + 720) % 360;

            if (angle > 15 && angle < 165) {
                ret = 'right';
            } else if (angle > 195 && angle < 345) {
                ret = 'left';
            } else {
                ret = 'center';
            }
            return ret;
        },

        /**
         * Get the tick length and width for the axis.
         * @param   {String} prefix 'tick' or 'minorTick'
         * @returns {Array}        An array of tickLength and tickWidth
         */
        tickSize: function (prefix) {
            var options = this.options,
                tickLength = options[prefix + 'Length'],
                tickWidth = pick(options[prefix + 'Width'], prefix === 'tick' && this.isXAxis ? 1 : 0); // X axis defaults to 1

            if (tickWidth && tickLength) {
                // Negate the length
                if (options[prefix + 'Position'] === 'inside') {
                    tickLength = -tickLength;
                }
                return [tickLength, tickWidth];
            }
            
        },

        /**
         * Return the size of the labels
         */
        labelMetrics: function () {
            return this.chart.renderer.fontMetrics(
                this.options.labels.style.fontSize, 
                this.ticks[0] && this.ticks[0].label
            );
        },

        /**
         * Prevent the ticks from getting so close we can't draw the labels. On a horizontal
         * axis, this is handled by rotating the labels, removing ticks and adding ellipsis.
         * On a vertical axis remove ticks and add ellipsis.
         */
        unsquish: function () {
            var labelOptions = this.options.labels,
                horiz = this.horiz,
                tickInterval = this.tickInterval,
                newTickInterval = tickInterval,
                slotSize = this.len / (((this.categories ? 1 : 0) + this.max - this.min) / tickInterval),
                rotation,
                rotationOption = labelOptions.rotation,
                labelMetrics = this.labelMetrics(),
                step,
                bestScore = Number.MAX_VALUE,
                autoRotation,
                // Return the multiple of tickInterval that is needed to avoid collision
                getStep = function (spaceNeeded) {
                    var step = spaceNeeded / (slotSize || 1);
                    step = step > 1 ? mathCeil(step) : 1;
                    return step * tickInterval;
                };

            if (horiz) {
                autoRotation = !labelOptions.staggerLines && !labelOptions.step && ( // #3971
                    defined(rotationOption) ?
                        [rotationOption] :
                        slotSize < pick(labelOptions.autoRotationLimit, 80) && labelOptions.autoRotation
                );

                if (autoRotation) {

                    // Loop over the given autoRotation options, and determine which gives the best score. The
                    // best score is that with the lowest number of steps and a rotation closest to horizontal.
                    each(autoRotation, function (rot) {
                        var score;

                        if (rot === rotationOption || (rot && rot >= -90 && rot <= 90)) { // #3891

                            step = getStep(mathAbs(labelMetrics.h / mathSin(deg2rad * rot)));

                            score = step + mathAbs(rot / 360);

                            if (score < bestScore) {
                                bestScore = score;
                                rotation = rot;
                                newTickInterval = step;
                            }
                        }
                    });
                }

            } else if (!labelOptions.step) { // #4411
                newTickInterval = getStep(labelMetrics.h);
            }

            this.autoRotation = autoRotation;
            this.labelRotation = pick(rotation, rotationOption);

            return newTickInterval;
        },

        /**
         * Get the general slot width for this axis. This may change between the pre-render (from Axis.getOffset) 
         * and the final tick rendering and placement (#5086).
         */
        getSlotWidth: function () {
            var chart = this.chart,
                horiz = this.horiz,
                labelOptions = this.options.labels,
                slotCount = Math.max(this.tickPositions.length - (this.categories ? 0 : 1), 1),
                marginLeft = chart.margin[3];

            return (horiz && (labelOptions.step || 0) < 2 && !labelOptions.rotation && // #4415
                ((this.staggerLines || 1) * chart.plotWidth) / slotCount) ||
                (!horiz && ((marginLeft && (marginLeft - chart.spacing[3])) || chart.chartWidth * 0.33)); // #1580, #1931

        },

        /**
         * Render the axis labels and determine whether ellipsis or rotation need to be applied
         */
        renderUnsquish: function () {
            var chart = this.chart,
                renderer = chart.renderer,
                tickPositions = this.tickPositions,
                ticks = this.ticks,
                labelOptions = this.options.labels,
                horiz = this.horiz,
                slotWidth = this.getSlotWidth(),
                innerWidth = mathMax(1, mathRound(slotWidth - 2 * (labelOptions.padding || 5))),
                attr = {},
                labelMetrics = this.labelMetrics(),
                textOverflowOption = labelOptions.style.textOverflow,
                css,
                labelLength = 0,
                label,
                i,
                pos;

            // Set rotation option unless it is "auto", like in gauges
            if (!isString(labelOptions.rotation)) {
                attr.rotation = labelOptions.rotation || 0; // #4443
            }

            // Handle auto rotation on horizontal axis
            if (this.autoRotation) {

                // Get the longest label length
                each(tickPositions, function (tick) {
                    tick = ticks[tick];
                    if (tick && tick.labelLength > labelLength) {
                        labelLength = tick.labelLength;
                    }
                });

                // Apply rotation only if the label is too wide for the slot, and
                // the label is wider than its height.
                if (labelLength > innerWidth && labelLength > labelMetrics.h) {
                    attr.rotation = this.labelRotation;
                } else {
                    this.labelRotation = 0;
                }

            // Handle word-wrap or ellipsis on vertical axis
            } else if (slotWidth) {
                // For word-wrap or ellipsis
                css = { width: innerWidth + PX };

                if (!textOverflowOption) {
                    css.textOverflow = 'clip';

                    // On vertical axis, only allow word wrap if there is room for more lines.
                    i = tickPositions.length;
                    while (!horiz && i--) {
                        pos = tickPositions[i];
                        label = ticks[pos].label;
                        if (label) {
                            // Reset ellipsis in order to get the correct bounding box (#4070)
                            if (label.styles.textOverflow === 'ellipsis') {
                                label.css({ textOverflow: 'clip' });

                            // Set the correct width in order to read the bounding box height (#4678, #5034)
                            } else if (ticks[pos].labelLength > slotWidth) {
                                label.css({ width: slotWidth + 'px' });
                            }

                            if (label.getBBox().height > this.len / tickPositions.length - (labelMetrics.h - labelMetrics.f)) {
                                label.specCss = { textOverflow: 'ellipsis' };
                            }
                        }
                    }
                }
            }


            // Add ellipsis if the label length is significantly longer than ideal
            if (attr.rotation) {
                css = {
                    width: (labelLength > chart.chartHeight * 0.5 ? chart.chartHeight * 0.33 : chart.chartHeight) + PX
                };
                if (!textOverflowOption) {
                    css.textOverflow = 'ellipsis';
                }
            }

            // Set the explicit or automatic label alignment
            this.labelAlign = labelOptions.align || this.autoLabelAlign(this.labelRotation);
            if (this.labelAlign) {
                attr.align = this.labelAlign;
            }

            // Apply general and specific CSS
            each(tickPositions, function (pos) {
                var tick = ticks[pos],
                    label = tick && tick.label;
                if (label) {
                    label.attr(attr); // This needs to go before the CSS in old IE (#4502)
                    if (css) {
                        label.css(merge(css, label.specCss));
                    }
                    delete label.specCss;
                    tick.rotation = attr.rotation;
                }
            });

            // Note: Why is this not part of getLabelPosition?
            this.tickRotCorr = renderer.rotCorr(labelMetrics.b, this.labelRotation || 0, this.side !== 0);
        },

        /**
         * Return true if the axis has associated data
         */
        hasData: function () {
            return this.hasVisibleSeries || (defined(this.min) && defined(this.max) && !!this.tickPositions);
        },

        /**
         * Render the tick labels to a preliminary position to get their sizes
         */
        getOffset: function () {
            var axis = this,
                chart = axis.chart,
                renderer = chart.renderer,
                options = axis.options,
                tickPositions = axis.tickPositions,
                ticks = axis.ticks,
                horiz = axis.horiz,
                side = axis.side,
                invertedSide = chart.inverted ? [1, 0, 3, 2][side] : side,
                hasData,
                showAxis,
                titleOffset = 0,
                titleOffsetOption,
                titleMargin = 0,
                axisTitleOptions = options.title,
                labelOptions = options.labels,
                labelOffset = 0, // reset
                labelOffsetPadded,
                opposite = axis.opposite,
                axisOffset = chart.axisOffset,
                clipOffset = chart.clipOffset,
                clip,
                directionFactor = [-1, 1, 1, -1][side],
                n,
                textAlign,
                axisParent = axis.axisParent, // Used in color axis
                lineHeightCorrection,
                tickSize = this.tickSize('tick');

            // For reuse in Axis.render
            hasData = axis.hasData();
            axis.showAxis = showAxis = hasData || pick(options.showEmpty, true);

            // Set/reset staggerLines
            axis.staggerLines = axis.horiz && labelOptions.staggerLines;

            // Create the axisGroup and gridGroup elements on first iteration
            if (!axis.axisGroup) {
                axis.gridGroup = renderer.g('grid')
                    .attr({ zIndex: options.gridZIndex || 1 })
                    .add(axisParent);
                axis.axisGroup = renderer.g('axis')
                    .attr({ zIndex: options.zIndex || 2 })
                    .add(axisParent);
                axis.labelGroup = renderer.g('axis-labels')
                    .attr({ zIndex: labelOptions.zIndex || 7 })
                    .addClass(PREFIX + axis.coll.toLowerCase() + '-labels')
                    .add(axisParent);
            }

            if (hasData || axis.isLinked) {

                // Generate ticks
                each(tickPositions, function (pos) {
                    if (!ticks[pos]) {
                        ticks[pos] = new Tick(axis, pos);
                    } else {
                        ticks[pos].addLabel(); // update labels depending on tick interval
                    }
                });

                axis.renderUnsquish();


                // Left side must be align: right and right side must have align: left for labels
                if (labelOptions.reserveSpace !== false && (side === 0 || side === 2 ||
                        { 1: 'left', 3: 'right' }[side] === axis.labelAlign || axis.labelAlign === 'center')) {
                    each(tickPositions, function (pos) {

                        // get the highest offset
                        labelOffset = mathMax(
                            ticks[pos].getLabelSize(),
                            labelOffset
                        );
                    });
                }

                if (axis.staggerLines) {
                    labelOffset *= axis.staggerLines;
                    axis.labelOffset = labelOffset * (axis.opposite ? -1 : 1);
                }


            } else { // doesn't have data
                for (n in ticks) {
                    ticks[n].destroy();
                    delete ticks[n];
                }
            }

            if (axisTitleOptions && axisTitleOptions.text && axisTitleOptions.enabled !== false) {
                if (!axis.axisTitle) {
                    textAlign = axisTitleOptions.textAlign;
                    if (!textAlign) {
                        textAlign = (horiz ? { 
                            low: 'left',
                            middle: 'center',
                            high: 'right'
                        } : { 
                            low: opposite ? 'right' : 'left',
                            middle: 'center',
                            high: opposite ? 'left' : 'right'
                        })[axisTitleOptions.align];
                    }
                    axis.axisTitle = renderer.text(
                        axisTitleOptions.text,
                        0,
                        0,
                        axisTitleOptions.useHTML
                    )
                    .attr({
                        zIndex: 7,
                        rotation: axisTitleOptions.rotation || 0,
                        align: textAlign
                    })
                    .addClass(PREFIX + this.coll.toLowerCase() + '-title')
                    .css(axisTitleOptions.style)
                    .add(axis.axisGroup);
                    axis.axisTitle.isNew = true;
                }

                if (showAxis) {
                    titleOffset = axis.axisTitle.getBBox()[horiz ? 'height' : 'width'];
                    titleOffsetOption = axisTitleOptions.offset;
                    titleMargin = defined(titleOffsetOption) ? 0 : pick(axisTitleOptions.margin, horiz ? 5 : 10);
                }

                // hide or show the title depending on whether showEmpty is set
                axis.axisTitle[showAxis ? 'show' : 'hide'](true);
            }

            // handle automatic or user set offset
            axis.offset = directionFactor * pick(options.offset, axisOffset[side]);

            axis.tickRotCorr = axis.tickRotCorr || { x: 0, y: 0 }; // polar
            if (side === 0) {
                lineHeightCorrection = -axis.labelMetrics().h;
            } else if (side === 2) {
                lineHeightCorrection = axis.tickRotCorr.y;
            } else {
                lineHeightCorrection = 0;
            }

            // Find the padded label offset
            labelOffsetPadded = Math.abs(labelOffset) + titleMargin;
            if (labelOffset) {
                labelOffsetPadded -= lineHeightCorrection;
                labelOffsetPadded += directionFactor * (horiz ? pick(labelOptions.y, axis.tickRotCorr.y + directionFactor * 8) : labelOptions.x);
            }
            axis.axisTitleMargin = pick(titleOffsetOption, labelOffsetPadded);

            axisOffset[side] = mathMax(
                axisOffset[side],
                axis.axisTitleMargin + titleOffset + directionFactor * axis.offset,
                labelOffsetPadded, // #3027
                hasData && tickPositions.length && tickSize ? tickSize[0] : 0 // #4866
            );

            // Decide the clipping needed to keep the graph inside the plot area and axis lines
            clip = options.offset ? 0 : mathFloor(options.lineWidth / 2) * 2; // #4308, #4371
            clipOffset[invertedSide] = mathMax(clipOffset[invertedSide], clip);
        },

        /**
         * Get the path for the axis line
         */
        getLinePath: function (lineWidth) {
            var chart = this.chart,
                opposite = this.opposite,
                offset = this.offset,
                horiz = this.horiz,
                lineLeft = this.left + (opposite ? this.width : 0) + offset,
                lineTop = chart.chartHeight - this.bottom - (opposite ? this.height : 0) + offset;

            if (opposite) {
                lineWidth *= -1; // crispify the other way - #1480, #1687
            }

            return chart.renderer
                .crispLine([
                    M,
                    horiz ?
                        this.left :
                        lineLeft,
                    horiz ?
                        lineTop :
                        this.top,
                    L,
                    horiz ?
                        chart.chartWidth - this.right :
                        lineLeft,
                    horiz ?
                        lineTop :
                        chart.chartHeight - this.bottom
                ], lineWidth);
        },

        /**
         * Position the title
         */
        getTitlePosition: function () {
            // compute anchor points for each of the title align options
            var horiz = this.horiz,
                axisLeft = this.left,
                axisTop = this.top,
                axisLength = this.len,
                axisTitleOptions = this.options.title,
                margin = horiz ? axisLeft : axisTop,
                opposite = this.opposite,
                offset = this.offset,
                xOption = axisTitleOptions.x || 0,
                yOption = axisTitleOptions.y || 0,
                fontSize = pInt(axisTitleOptions.style.fontSize || 12),

                // the position in the length direction of the axis
                alongAxis = {
                    low: margin + (horiz ? 0 : axisLength),
                    middle: margin + axisLength / 2,
                    high: margin + (horiz ? axisLength : 0)
                }[axisTitleOptions.align],

                // the position in the perpendicular direction of the axis
                offAxis = (horiz ? axisTop + this.height : axisLeft) +
                    (horiz ? 1 : -1) * // horizontal axis reverses the margin
                    (opposite ? -1 : 1) * // so does opposite axes
                    this.axisTitleMargin +
                    (this.side === 2 ? fontSize : 0);

            return {
                x: horiz ?
                    alongAxis + xOption :
                    offAxis + (opposite ? this.width : 0) + offset + xOption,
                y: horiz ?
                    offAxis + yOption - (opposite ? this.height : 0) + offset :
                    alongAxis + yOption
            };
        },

        /**
         * Render the axis
         */
        render: function () {
            var axis = this,
                chart = axis.chart,
                renderer = chart.renderer,
                options = axis.options,
                isLog = axis.isLog,
                lin2log = axis.lin2log,
                isLinked = axis.isLinked,
                tickPositions = axis.tickPositions,
                axisTitle = axis.axisTitle,
                ticks = axis.ticks,
                minorTicks = axis.minorTicks,
                alternateBands = axis.alternateBands,
                stackLabelOptions = options.stackLabels,
                alternateGridColor = options.alternateGridColor,
                tickmarkOffset = axis.tickmarkOffset,
                lineWidth = options.lineWidth,
                linePath,
                hasRendered = chart.hasRendered,
                slideInTicks = hasRendered && isNumber(axis.oldMin),
                showAxis = axis.showAxis,
                animation = animObject(renderer.globalAnimation),
                from,
                to;

            // Reset
            axis.labelEdge.length = 0;
            //axis.justifyToPlot = overflow === 'justify';
            axis.overlap = false;

            // Mark all elements inActive before we go over and mark the active ones
            each([ticks, minorTicks, alternateBands], function (coll) {
                var pos;
                for (pos in coll) {
                    coll[pos].isActive = false;
                }
            });

            // If the series has data draw the ticks. Else only the line and title
            if (axis.hasData() || isLinked) {

                // minor ticks
                if (axis.minorTickInterval && !axis.categories) {
                    each(axis.getMinorTickPositions(), function (pos) {
                        if (!minorTicks[pos]) {
                            minorTicks[pos] = new Tick(axis, pos, 'minor');
                        }

                        // render new ticks in old position
                        if (slideInTicks && minorTicks[pos].isNew) {
                            minorTicks[pos].render(null, true);
                        }

                        minorTicks[pos].render(null, false, 1);
                    });
                }

                // Major ticks. Pull out the first item and render it last so that
                // we can get the position of the neighbour label. #808.
                if (tickPositions.length) { // #1300
                    each(tickPositions, function (pos, i) {

                        // linked axes need an extra check to find out if
                        if (!isLinked || (pos >= axis.min && pos <= axis.max)) {

                            if (!ticks[pos]) {
                                ticks[pos] = new Tick(axis, pos);
                            }

                            // render new ticks in old position
                            if (slideInTicks && ticks[pos].isNew) {
                                ticks[pos].render(i, true, 0.1);
                            }

                            ticks[pos].render(i);
                        }

                    });
                    // In a categorized axis, the tick marks are displayed between labels. So
                    // we need to add a tick mark and grid line at the left edge of the X axis.
                    if (tickmarkOffset && (axis.min === 0 || axis.single)) {
                        if (!ticks[-1]) {
                            ticks[-1] = new Tick(axis, -1, null, true);
                        }
                        ticks[-1].render(-1);
                    }

                }

                // alternate grid color
                if (alternateGridColor) {
                    each(tickPositions, function (pos, i) {
                        to = tickPositions[i + 1] !== UNDEFINED ? tickPositions[i + 1] + tickmarkOffset : axis.max - tickmarkOffset; 
                        if (i % 2 === 0 && pos < axis.max && to <= axis.max + (chart.polar ? -tickmarkOffset : tickmarkOffset)) { // #2248, #4660
                            if (!alternateBands[pos]) {
                                alternateBands[pos] = new Highcharts.PlotLineOrBand(axis);
                            }
                            from = pos + tickmarkOffset; // #949
                            alternateBands[pos].options = {
                                from: isLog ? lin2log(from) : from,
                                to: isLog ? lin2log(to) : to,
                                color: alternateGridColor
                            };
                            alternateBands[pos].render();
                            alternateBands[pos].isActive = true;
                        }
                    });
                }

                // custom plot lines and bands
                if (!axis._addedPlotLB) { // only first time
                    each((options.plotLines || []).concat(options.plotBands || []), function (plotLineOptions) {
                        axis.addPlotBandOrLine(plotLineOptions);
                    });
                    axis._addedPlotLB = true;
                }

            } // end if hasData

            // Remove inactive ticks
            each([ticks, minorTicks, alternateBands], function (coll) {
                var pos,
                    i,
                    forDestruction = [],
                    delay = animation.duration,
                    destroyInactiveItems = function () {
                        i = forDestruction.length;
                        while (i--) {
                            // When resizing rapidly, the same items may be destroyed in different timeouts,
                            // or the may be reactivated
                            if (coll[forDestruction[i]] && !coll[forDestruction[i]].isActive) {
                                coll[forDestruction[i]].destroy();
                                delete coll[forDestruction[i]];
                            }
                        }

                    };

                for (pos in coll) {

                    if (!coll[pos].isActive) {
                        // Render to zero opacity
                        coll[pos].render(pos, false, 0);
                        coll[pos].isActive = false;
                        forDestruction.push(pos);
                    }
                }

                // When the objects are finished fading out, destroy them
                syncTimeout(
                    destroyInactiveItems, 
                    coll === alternateBands || !chart.hasRendered || !delay ? 0 : delay
                );
            });

            // Static items. As the axis group is cleared on subsequent calls
            // to render, these items are added outside the group.
            // axis line
            if (lineWidth) {
                linePath = axis.getLinePath(lineWidth);
                if (!axis.axisLine) {
                    axis.axisLine = renderer.path(linePath)
                        .attr({
                            stroke: options.lineColor,
                            'stroke-width': lineWidth,
                            zIndex: 7
                        })
                        .add(axis.axisGroup);
                } else {
                    axis.axisLine.animate({ d: linePath });
                }

                // show or hide the line depending on options.showEmpty
                axis.axisLine[showAxis ? 'show' : 'hide'](true);
            }

            if (axisTitle && showAxis) {

                axisTitle[axisTitle.isNew ? 'attr' : 'animate'](
                    axis.getTitlePosition()
                );
                axisTitle.isNew = false;
            }

            // Stacked totals:
            if (stackLabelOptions && stackLabelOptions.enabled) {
                axis.renderStackTotals();
            }
            // End stacked totals

            axis.isDirty = false;
        },

        /**
         * Redraw the axis to reflect changes in the data or axis extremes
         */
        redraw: function () {

            if (this.visible) {
                // render the axis
                this.render();

                // move plot lines and bands
                each(this.plotLinesAndBands, function (plotLine) {
                    plotLine.render();
                });
            }

            // mark associated series as dirty and ready for redraw
            each(this.series, function (series) {
                series.isDirty = true;
            });

        },

        /**
         * Destroys an Axis instance.
         */
        destroy: function (keepEvents) {
            var axis = this,
                stacks = axis.stacks,
                stackKey,
                plotLinesAndBands = axis.plotLinesAndBands,
                i;

            // Remove the events
            if (!keepEvents) {
                removeEvent(axis);
            }

            // Destroy each stack total
            for (stackKey in stacks) {
                destroyObjectProperties(stacks[stackKey]);

                stacks[stackKey] = null;
            }

            // Destroy collections
            each([axis.ticks, axis.minorTicks, axis.alternateBands], function (coll) {
                destroyObjectProperties(coll);
            });
            i = plotLinesAndBands.length;
            while (i--) { // #1975
                plotLinesAndBands[i].destroy();
            }

            // Destroy local variables
            each(['stackTotalGroup', 'axisLine', 'axisTitle', 'axisGroup', 'cross', 'gridGroup', 'labelGroup'], function (prop) {
                if (axis[prop]) {
                    axis[prop] = axis[prop].destroy();
                }
            });

            // Destroy crosshair
            if (this.cross) {
                this.cross.destroy();
            }
        },

        /**
         * Draw the crosshair
         * 
         * @param  {Object} e The event arguments from the modified pointer event
         * @param  {Object} point The Point object
         */
        drawCrosshair: function (e, point) {

            var path,
                options = this.crosshair,
                pos,
                attribs,
                categorized,
                strokeWidth;

            if (
                // Disabled in options
                !this.crosshair ||
                // Snap
                ((defined(point) || !pick(options.snap, true)) === false)
            ) {
                this.hideCrosshair();

            } else {

                // Get the path
                if (!pick(options.snap, true)) {
                    pos = (this.horiz ? e.chartX - this.pos : this.len - e.chartY + this.pos);
                } else if (defined(point)) {
                    pos = this.isXAxis ? point.plotX : this.len - point.plotY; // #3834
                }

                if (this.isRadial) {
                    path = this.getPlotLinePath(this.isXAxis ? point.x : pick(point.stackY, point.y)) || null; // #3189
                } else {
                    path = this.getPlotLinePath(null, null, null, null, pos) || null; // #3189
                }

                if (path === null) {
                    this.hideCrosshair();
                    return;
                }

                categorized = this.categories && !this.isRadial;
                strokeWidth = pick(options.width, (categorized ? this.transA : 1));

                // Draw the cross
                if (this.cross) {
                    this.cross
                        .attr({
                            d: path,
                            visibility: 'visible',
                            'stroke-width': strokeWidth // #4737
                        });
                } else {
                    attribs = {
                        'pointer-events': 'none', // #5259
                        'stroke-width': strokeWidth,
                        stroke: options.color || (categorized ? 'rgba(155,200,255,0.2)' : '#C0C0C0'),
                        zIndex: pick(options.zIndex, 2)
                    };
                    if (options.dashStyle) {
                        attribs.dashstyle = options.dashStyle;
                    }
                    this.cross = this.chart.renderer.path(path).attr(attribs).add();
                }

            }

        },

        /**
         *    Hide the crosshair.
         */
        hideCrosshair: function () {
            if (this.cross) {
                this.cross.hide();
            }
        }
    }; // end Axis

    extend(Axis.prototype, AxisPlotLineOrBandExtension);

    /**
     * Set the tick positions to a time unit that makes sense, for example
     * on the first of each month or on every Monday. Return an array
     * with the time positions. Used in datetime axes as well as for grouping
     * data on a datetime axis.
     *
     * @param {Object} normalizedInterval The interval in axis values (ms) and the count
     * @param {Number} min The minimum in axis values
     * @param {Number} max The maximum in axis values
     * @param {Number} startOfWeek
     */
    Axis.prototype.getTimeTicks = function (normalizedInterval, min, max, startOfWeek) {
        var tickPositions = [],
            i,
            higherRanks = {},
            useUTC = defaultOptions.global.useUTC,
            minYear, // used in months and years as a basis for Date.UTC()
            minDate = new Date(min - getTZOffset(min)),
            interval = normalizedInterval.unitRange,
            count = normalizedInterval.count;

        if (defined(min)) { // #1300
            minDate[setMilliseconds](interval >= timeUnits.second ? 0 : // #3935
                count * mathFloor(minDate.getMilliseconds() / count)); // #3652, #3654

            if (interval >= timeUnits.second) { // second
                minDate[setSeconds](interval >= timeUnits.minute ? 0 : // #3935
                    count * mathFloor(minDate.getSeconds() / count));
            }

            if (interval >= timeUnits.minute) { // minute
                minDate[setMinutes](interval >= timeUnits.hour ? 0 :
                    count * mathFloor(minDate[getMinutes]() / count));
            }

            if (interval >= timeUnits.hour) { // hour
                minDate[setHours](interval >= timeUnits.day ? 0 :
                    count * mathFloor(minDate[getHours]() / count));
            }

            if (interval >= timeUnits.day) { // day
                minDate[setDate](interval >= timeUnits.month ? 1 :
                    count * mathFloor(minDate[getDate]() / count));
            }

            if (interval >= timeUnits.month) { // month
                minDate[setMonth](interval >= timeUnits.year ? 0 :
                    count * mathFloor(minDate[getMonth]() / count));
                minYear = minDate[getFullYear]();
            }

            if (interval >= timeUnits.year) { // year
                minYear -= minYear % count;
                minDate[setFullYear](minYear);
            }

            // week is a special case that runs outside the hierarchy
            if (interval === timeUnits.week) {
                // get start of current week, independent of count
                minDate[setDate](minDate[getDate]() - minDate[getDay]() +
                    pick(startOfWeek, 1));
            }


            // get tick positions
            i = 1;
            if (timezoneOffset || getTimezoneOffset) {
                minDate = minDate.getTime();
                minDate = new Date(minDate + getTZOffset(minDate));
            }
            minYear = minDate[getFullYear]();
            var time = minDate.getTime(),
                minMonth = minDate[getMonth](),
                minDateDate = minDate[getDate](),
                variableDayLength = !useUTC || !!getTimezoneOffset, // #4951
                localTimezoneOffset = (timeUnits.day +
                        (useUTC ? getTZOffset(minDate) : minDate.getTimezoneOffset() * 60 * 1000)
                    ) % timeUnits.day; // #950, #3359

            // iterate and add tick positions at appropriate values
            while (time < max) {
                tickPositions.push(time);

                // if the interval is years, use Date.UTC to increase years
                if (interval === timeUnits.year) {
                    time = makeTime(minYear + i * count, 0);

                // if the interval is months, use Date.UTC to increase months
                } else if (interval === timeUnits.month) {
                    time = makeTime(minYear, minMonth + i * count);

                // if we're using global time, the interval is not fixed as it jumps
                // one hour at the DST crossover
                } else if (variableDayLength && (interval === timeUnits.day || interval === timeUnits.week)) {
                    time = makeTime(minYear, minMonth, minDateDate +
                        i * count * (interval === timeUnits.day ? 1 : 7));

                // else, the interval is fixed and we use simple addition
                } else {
                    time += interval * count;
                }

                i++;
            }

            // push the last time
            tickPositions.push(time);


            // mark new days if the time is dividible by day (#1649, #1760)
            each(grep(tickPositions, function (time) {
                return interval <= timeUnits.hour && time % timeUnits.day === localTimezoneOffset;
            }), function (time) {
                higherRanks[time] = 'day';
            });
        }


        // record information on the chosen unit - for dynamic label formatter
        tickPositions.info = extend(normalizedInterval, {
            higherRanks: higherRanks,
            totalRange: interval * count
        });

        return tickPositions;
    };

    /**
     * Get a normalized tick interval for dates. Returns a configuration object with
     * unit range (interval), count and name. Used to prepare data for getTimeTicks.
     * Previously this logic was part of getTimeTicks, but as getTimeTicks now runs
     * of segments in stock charts, the normalizing logic was extracted in order to
     * prevent it for running over again for each segment having the same interval.
     * #662, #697.
     */
    Axis.prototype.normalizeTimeTickInterval = function (tickInterval, unitsOption) {
        var units = unitsOption || [[
                'millisecond', // unit name
                [1, 2, 5, 10, 20, 25, 50, 100, 200, 500] // allowed multiples
            ], [
                'second',
                [1, 2, 5, 10, 15, 30]
            ], [
                'minute',
                [1, 2, 5, 10, 15, 30]
            ], [
                'hour',
                [1, 2, 3, 4, 6, 8, 12]
            ], [
                'day',
                [1, 2]
            ], [
                'week',
                [1, 2]
            ], [
                'month',
                [1, 2, 3, 4, 6]
            ], [
                'year',
                null
            ]],
            unit = units[units.length - 1], // default unit is years
            interval = timeUnits[unit[0]],
            multiples = unit[1],
            count,
            i;

        // loop through the units to find the one that best fits the tickInterval
        for (i = 0; i < units.length; i++) {
            unit = units[i];
            interval = timeUnits[unit[0]];
            multiples = unit[1];


            if (units[i + 1]) {
                // lessThan is in the middle between the highest multiple and the next unit.
                var lessThan = (interval * multiples[multiples.length - 1] +
                            timeUnits[units[i + 1][0]]) / 2;

                // break and keep the current unit
                if (tickInterval <= lessThan) {
                    break;
                }
            }
        }

        // prevent 2.5 years intervals, though 25, 250 etc. are allowed
        if (interval === timeUnits.year && tickInterval < 5 * interval) {
            multiples = [1, 2, 5];
        }

        // get the count
        count = normalizeTickInterval(
            tickInterval / interval,
            multiples,
            unit[0] === 'year' ? mathMax(getMagnitude(tickInterval / interval), 1) : 1 // #1913, #2360
        );

        return {
            unitRange: interval,
            count: count,
            unitName: unit[0]
        };
    };
    /**
     * Methods defined on the Axis prototype
     */

    /**
     * Set the tick positions of a logarithmic axis
     */
    Axis.prototype.getLogTickPositions = function (interval, min, max, minor) {
        var axis = this,
            options = axis.options,
            axisLength = axis.len,
            lin2log = axis.lin2log,
            log2lin = axis.log2lin,
            // Since we use this method for both major and minor ticks,
            // use a local variable and return the result
            positions = [];

        // Reset
        if (!minor) {
            axis._minorAutoInterval = null;
        }

        // First case: All ticks fall on whole logarithms: 1, 10, 100 etc.
        if (interval >= 0.5) {
            interval = mathRound(interval);
            positions = axis.getLinearTickPositions(interval, min, max);

        // Second case: We need intermediary ticks. For example
        // 1, 2, 4, 6, 8, 10, 20, 40 etc.
        } else if (interval >= 0.08) {
            var roundedMin = mathFloor(min),
                intermediate,
                i,
                j,
                len,
                pos,
                lastPos,
                break2;

            if (interval > 0.3) {
                intermediate = [1, 2, 4];
            } else if (interval > 0.15) { // 0.2 equals five minor ticks per 1, 10, 100 etc
                intermediate = [1, 2, 4, 6, 8];
            } else { // 0.1 equals ten minor ticks per 1, 10, 100 etc
                intermediate = [1, 2, 3, 4, 5, 6, 7, 8, 9];
            }

            for (i = roundedMin; i < max + 1 && !break2; i++) {
                len = intermediate.length;
                for (j = 0; j < len && !break2; j++) {
                    pos = log2lin(lin2log(i) * intermediate[j]);
                    if (pos > min && (!minor || lastPos <= max) && lastPos !== UNDEFINED) { // #1670, lastPos is #3113
                        positions.push(lastPos);
                    }

                    if (lastPos > max) {
                        break2 = true;
                    }
                    lastPos = pos;
                }
            }

        // Third case: We are so deep in between whole logarithmic values that
        // we might as well handle the tick positions like a linear axis. For
        // example 1.01, 1.02, 1.03, 1.04.
        } else {
            var realMin = lin2log(min),
                realMax = lin2log(max),
                tickIntervalOption = options[minor ? 'minorTickInterval' : 'tickInterval'],
                filteredTickIntervalOption = tickIntervalOption === 'auto' ? null : tickIntervalOption,
                tickPixelIntervalOption = options.tickPixelInterval / (minor ? 5 : 1),
                totalPixelLength = minor ? axisLength / axis.tickPositions.length : axisLength;

            interval = pick(
                filteredTickIntervalOption,
                axis._minorAutoInterval,
                (realMax - realMin) * tickPixelIntervalOption / (totalPixelLength || 1)
            );

            interval = normalizeTickInterval(
                interval,
                null,
                getMagnitude(interval)
            );

            positions = map(axis.getLinearTickPositions(
                interval,
                realMin,
                realMax
            ), log2lin);

            if (!minor) {
                axis._minorAutoInterval = interval / 5;
            }
        }

        // Set the axis-level tickInterval variable
        if (!minor) {
            axis.tickInterval = interval;
        }
        return positions;
    };

    Axis.prototype.log2lin = function (num) {
        return math.log(num) / math.LN10;
    };

    Axis.prototype.lin2log = function (num) {
        return math.pow(10, num);
    };
    /**
     * The tooltip object
     * @param {Object} chart The chart instance
     * @param {Object} options Tooltip options
     */
    var Tooltip = Highcharts.Tooltip = function () {
        this.init.apply(this, arguments);
    };

    Tooltip.prototype = {

        init: function (chart, options) {

            var borderWidth = options.borderWidth,
                style = options.style,
                padding = pInt(style.padding);

            // Save the chart and options
            this.chart = chart;
            this.options = options;

            // Keep track of the current series
            //this.currentSeries = UNDEFINED;

            // List of crosshairs
            this.crosshairs = [];

            // Current values of x and y when animating
            this.now = { x: 0, y: 0 };

            // The tooltip is initially hidden
            this.isHidden = true;


            // create the label
            this.label = chart.renderer.label('', 0, 0, options.shape || 'callout', null, null, options.useHTML, null, 'tooltip')
                .attr({
                    padding: padding,
                    fill: options.backgroundColor,
                    'stroke-width': borderWidth,
                    r: options.borderRadius,
                    zIndex: 8
                })
                .css(style)
                .css({ padding: 0 }) // Remove it from VML, the padding is applied as an attribute instead (#1117)
                .add()
                .attr({ y: -9999 }); // #2301, #2657

            // When using canVG the shadow shows up as a gray circle
            // even if the tooltip is hidden.
            if (!useCanVG) {
                this.label.shadow(options.shadow);
            }

            // Public property for getting the shared state.
            this.shared = options.shared;
        },

        /**
         * Destroy the tooltip and its elements.
         */
        destroy: function () {
            // Destroy and clear local variables
            if (this.label) {
                this.label = this.label.destroy();
            }
            clearTimeout(this.hideTimer);
            clearTimeout(this.tooltipTimeout);
        },

        /**
         * Provide a soft movement for the tooltip
         *
         * @param {Number} x
         * @param {Number} y
         * @private
         */
        move: function (x, y, anchorX, anchorY) {
            var tooltip = this,
                now = tooltip.now,
                animate = tooltip.options.animation !== false && !tooltip.isHidden &&
                    // When we get close to the target position, abort animation and land on the right place (#3056)
                    (mathAbs(x - now.x) > 1 || mathAbs(y - now.y) > 1),
                skipAnchor = tooltip.followPointer || tooltip.len > 1;

            // Get intermediate values for animation
            extend(now, {
                x: animate ? (2 * now.x + x) / 3 : x,
                y: animate ? (now.y + y) / 2 : y,
                anchorX: skipAnchor ? UNDEFINED : animate ? (2 * now.anchorX + anchorX) / 3 : anchorX,
                anchorY: skipAnchor ? UNDEFINED : animate ? (now.anchorY + anchorY) / 2 : anchorY
            });

            // Move to the intermediate value
            tooltip.label.attr(now);


            // Run on next tick of the mouse tracker
            if (animate) {

                // Never allow two timeouts
                clearTimeout(this.tooltipTimeout);

                // Set the fixed interval ticking for the smooth tooltip
                this.tooltipTimeout = setTimeout(function () {
                    // The interval function may still be running during destroy, so check that the chart is really there before calling.
                    if (tooltip) {
                        tooltip.move(x, y, anchorX, anchorY);
                    }
                }, 32);

            }
        },

        /**
         * Hide the tooltip
         */
        hide: function (delay) {
            var tooltip = this;
            clearTimeout(this.hideTimer); // disallow duplicate timers (#1728, #1766)
            delay = pick(delay, this.options.hideDelay, 500);
            if (!this.isHidden) {
                this.hideTimer = syncTimeout(function () {
                    tooltip.label[delay ? 'fadeOut' : 'hide']();
                    tooltip.isHidden = true;
                }, delay);
            }
        },

        /**
         * Extendable method to get the anchor position of the tooltip
         * from a point or set of points
         */
        getAnchor: function (points, mouseEvent) {
            var ret,
                chart = this.chart,
                inverted = chart.inverted,
                plotTop = chart.plotTop,
                plotLeft = chart.plotLeft,
                plotX = 0,
                plotY = 0,
                yAxis,
                xAxis;

            points = splat(points);

            // Pie uses a special tooltipPos
            ret = points[0].tooltipPos;

            // When tooltip follows mouse, relate the position to the mouse
            if (this.followPointer && mouseEvent) {
                if (mouseEvent.chartX === UNDEFINED) {
                    mouseEvent = chart.pointer.normalize(mouseEvent);
                }
                ret = [
                    mouseEvent.chartX - chart.plotLeft,
                    mouseEvent.chartY - plotTop
                ];
            }
            // When shared, use the average position
            if (!ret) {
                each(points, function (point) {
                    yAxis = point.series.yAxis;
                    xAxis = point.series.xAxis;
                    plotX += point.plotX  + (!inverted && xAxis ? xAxis.left - plotLeft : 0);
                    plotY += (point.plotLow ? (point.plotLow + point.plotHigh) / 2 : point.plotY) +
                        (!inverted && yAxis ? yAxis.top - plotTop : 0); // #1151
                });

                plotX /= points.length;
                plotY /= points.length;

                ret = [
                    inverted ? chart.plotWidth - plotY : plotX,
                    this.shared && !inverted && points.length > 1 && mouseEvent ?
                        mouseEvent.chartY - plotTop : // place shared tooltip next to the mouse (#424)
                        inverted ? chart.plotHeight - plotX : plotY
                ];
            }

            return map(ret, mathRound);
        },

        /**
         * Place the tooltip in a chart without spilling over
         * and not covering the point it self.
         */
        getPosition: function (boxWidth, boxHeight, point) {

            var chart = this.chart,
                distance = this.distance,
                ret = {},
                h = point.h || 0, // #4117
                swapped,
                first = ['y', chart.chartHeight, boxHeight, point.plotY + chart.plotTop, chart.plotTop, chart.plotTop + chart.plotHeight],
                second = ['x', chart.chartWidth, boxWidth, point.plotX + chart.plotLeft, chart.plotLeft, chart.plotLeft + chart.plotWidth],
                // The far side is right or bottom
                preferFarSide = !this.followPointer && pick(point.ttBelow, !chart.inverted === !!point.negative), // #4984
                /**
                 * Handle the preferred dimension. When the preferred dimension is tooltip
                 * on top or bottom of the point, it will look for space there.
                 */
                firstDimension = function (dim, outerSize, innerSize, point, min, max) {
                    var roomLeft = innerSize < point - distance,
                        roomRight = point + distance + innerSize < outerSize,
                        alignedLeft = point - distance - innerSize,
                        alignedRight = point + distance;

                    if (preferFarSide && roomRight) {
                        ret[dim] = alignedRight;
                    } else if (!preferFarSide && roomLeft) {
                        ret[dim] = alignedLeft;
                    } else if (roomLeft) {
                        ret[dim] = mathMin(max - innerSize, alignedLeft - h < 0 ? alignedLeft : alignedLeft - h);
                    } else if (roomRight) {
                        ret[dim] = mathMax(min, alignedRight + h + innerSize > outerSize ? alignedRight : alignedRight + h);
                    } else {
                        return false;
                    }
                },
                /**
                 * Handle the secondary dimension. If the preferred dimension is tooltip
                 * on top or bottom of the point, the second dimension is to align the tooltip
                 * above the point, trying to align center but allowing left or right
                 * align within the chart box.
                 */
                secondDimension = function (dim, outerSize, innerSize, point) {
                    var retVal;

                    // Too close to the edge, return false and swap dimensions
                    if (point < distance || point > outerSize - distance) {
                        retVal = false;
                    // Align left/top
                    } else if (point < innerSize / 2) {
                        ret[dim] = 1;
                    // Align right/bottom
                    } else if (point > outerSize - innerSize / 2) {
                        ret[dim] = outerSize - innerSize - 2;
                    // Align center
                    } else {
                        ret[dim] = point - innerSize / 2;
                    }
                    return retVal;
                },
                /**
                 * Swap the dimensions
                 */
                swap = function (count) {
                    var temp = first;
                    first = second;
                    second = temp;
                    swapped = count;
                },
                run = function () {
                    if (firstDimension.apply(0, first) !== false) {
                        if (secondDimension.apply(0, second) === false && !swapped) {
                            swap(true);
                            run();
                        }
                    } else if (!swapped) {
                        swap(true);
                        run();
                    } else {
                        ret.x = ret.y = 0;
                    }
                };

            // Under these conditions, prefer the tooltip on the side of the point
            if (chart.inverted || this.len > 1) {
                swap();
            }
            run();

            return ret;

        },

        /**
         * In case no user defined formatter is given, this will be used. Note that the context
         * here is an object holding point, series, x, y etc.
         */
        defaultFormatter: function (tooltip) {
            var items = this.points || splat(this),
                s;

            // build the header
            s = [tooltip.tooltipFooterHeaderFormatter(items[0])]; //#3397: abstraction to enable formatting of footer and header

            // build the values
            s = s.concat(tooltip.bodyFormatter(items));

            // footer
            s.push(tooltip.tooltipFooterHeaderFormatter(items[0], true)); //#3397: abstraction to enable formatting of footer and header

            return s.join('');
        },

        /**
         * Refresh the tooltip's text and position.
         * @param {Object} point
         */
        refresh: function (point, mouseEvent) {
            var tooltip = this,
                chart = tooltip.chart,
                label = tooltip.label,
                options = tooltip.options,
                x,
                y,
                anchor,
                textConfig = {},
                text,
                pointConfig = [],
                formatter = options.formatter || tooltip.defaultFormatter,
                hoverPoints = chart.hoverPoints,
                borderColor,
                shared = tooltip.shared,
                currentSeries;

            clearTimeout(this.hideTimer);

            // get the reference point coordinates (pie charts use tooltipPos)
            tooltip.followPointer = splat(point)[0].series.tooltipOptions.followPointer;
            anchor = tooltip.getAnchor(point, mouseEvent);
            x = anchor[0];
            y = anchor[1];

            // shared tooltip, array is sent over
            if (shared && !(point.series && point.series.noSharedTooltip)) {

                // hide previous hoverPoints and set new

                chart.hoverPoints = point;
                if (hoverPoints) {
                    each(hoverPoints, function (point) {
                        point.setState();
                    });
                }

                each(point, function (item) {
                    item.setState(HOVER_STATE);

                    pointConfig.push(item.getLabelConfig());
                });

                textConfig = {
                    x: point[0].category,
                    y: point[0].y
                };
                textConfig.points = pointConfig;
                this.len = pointConfig.length;
                point = point[0];

            // single point tooltip
            } else {
                textConfig = point.getLabelConfig();
            }
            text = formatter.call(textConfig, tooltip);

            // register the current series
            currentSeries = point.series;
            this.distance = pick(currentSeries.tooltipOptions.distance, 16);

            // update the inner HTML
            if (text === false) {
                this.hide();
            } else {

                // show it
                if (tooltip.isHidden) {
                    stop(label);
                    label.attr('opacity', 1).show();
                }

                // update text
                label.attr({
                    text: text
                });

                // set the stroke color of the box
                borderColor = options.borderColor || point.color || currentSeries.color || '#606060';
                label.attr({
                    stroke: borderColor
                });
                tooltip.updatePosition({
                    plotX: x,
                    plotY: y,
                    negative: point.negative,
                    ttBelow: point.ttBelow,
                    h: anchor[2] || 0
                });

                this.isHidden = false;
            }
            fireEvent(chart, 'tooltipRefresh', {
                text: text,
                x: x + chart.plotLeft,
                y: y + chart.plotTop,
                borderColor: borderColor
            });
        },

        /**
         * Find the new position and perform the move
         */
        updatePosition: function (point) {
            var chart = this.chart,
                label = this.label,
                pos = (this.options.positioner || this.getPosition).call(
                    this,
                    label.width,
                    label.height,
                    point
                );

            // do the move
            this.move(
                mathRound(pos.x),
                mathRound(pos.y || 0), // can be undefined (#3977)
                point.plotX + chart.plotLeft,
                point.plotY + chart.plotTop
            );
        },

        /**
         * Get the best X date format based on the closest point range on the axis.
         */
        getXDateFormat: function (point, options, xAxis) {
            var xDateFormat,
                dateTimeLabelFormats = options.dateTimeLabelFormats,
                closestPointRange = xAxis && xAxis.closestPointRange,
                n,
                blank = '01-01 00:00:00.000',
                strpos = {
                    millisecond: 15,
                    second: 12,
                    minute: 9,
                    hour: 6,
                    day: 3
                },
                date,
                lastN = 'millisecond'; // for sub-millisecond data, #4223

            if (closestPointRange) {
                date = dateFormat('%m-%d %H:%M:%S.%L', point.x);
                for (n in timeUnits) {

                    // If the range is exactly one week and we're looking at a Sunday/Monday, go for the week format
                    if (closestPointRange === timeUnits.week && +dateFormat('%w', point.x) === xAxis.options.startOfWeek &&
                            date.substr(6) === blank.substr(6)) {
                        n = 'week';
                        break;
                    }

                    // The first format that is too great for the range
                    if (timeUnits[n] > closestPointRange) {
                        n = lastN;
                        break;
                    }

                    // If the point is placed every day at 23:59, we need to show
                    // the minutes as well. #2637.
                    if (strpos[n] && date.substr(strpos[n]) !== blank.substr(strpos[n])) {
                        break;
                    }

                    // Weeks are outside the hierarchy, only apply them on Mondays/Sundays like in the first condition
                    if (n !== 'week') {
                        lastN = n;
                    }
                }

                if (n) {
                    xDateFormat = dateTimeLabelFormats[n];
                }
            } else {
                xDateFormat = dateTimeLabelFormats.day;
            }

            return xDateFormat || dateTimeLabelFormats.year; // #2546, 2581
        },

        /**
         * Format the footer/header of the tooltip
         * #3397: abstraction to enable formatting of footer and header
         */
        tooltipFooterHeaderFormatter: function (point, isFooter) {
            var footOrHead = isFooter ? 'footer' : 'header',
                series = point.series,
                tooltipOptions = series.tooltipOptions,
                xDateFormat = tooltipOptions.xDateFormat,
                xAxis = series.xAxis,
                isDateTime = xAxis && xAxis.options.type === 'datetime' && isNumber(point.key),
                formatString = tooltipOptions[footOrHead + 'Format'];

            // Guess the best date format based on the closest point distance (#568, #3418)
            if (isDateTime && !xDateFormat) {
                xDateFormat = this.getXDateFormat(point, tooltipOptions, xAxis);
            }

            // Insert the footer date format if any
            if (isDateTime && xDateFormat) {
                formatString = formatString.replace('{point.key}', '{point.key:' + xDateFormat + '}');
            }

            return format(formatString, {
                point: point,
                series: series
            });
        },

        /**
         * Build the body (lines) of the tooltip by iterating over the items and returning one entry for each item,
         * abstracting this functionality allows to easily overwrite and extend it.
         */
        bodyFormatter: function (items) {
            return map(items, function (item) {
                var tooltipOptions = item.series.tooltipOptions;
                return (tooltipOptions.pointFormatter || item.point.tooltipFormatter).call(item.point, tooltipOptions.pointFormat);
            });
        }

    };

    var hoverChartIndex;

    // Global flag for touch support
    hasTouch = doc && doc.documentElement.ontouchstart !== UNDEFINED;

    /**
     * The mouse tracker object. All methods starting with "on" are primary DOM event handlers.
     * Subsequent methods should be named differently from what they are doing.
     * @param {Object} chart The Chart instance
     * @param {Object} options The root options object
     */
    var Pointer = Highcharts.Pointer = function (chart, options) {
        this.init(chart, options);
    };

    Pointer.prototype = {
        /**
         * Initialize Pointer
         */
        init: function (chart, options) {

            var chartOptions = options.chart,
                chartEvents = chartOptions.events,
                zoomType = useCanVG ? '' : chartOptions.zoomType,
                inverted = chart.inverted,
                zoomX,
                zoomY;

            // Store references
            this.options = options;
            this.chart = chart;

            // Zoom status
            this.zoomX = zoomX = /x/.test(zoomType);
            this.zoomY = zoomY = /y/.test(zoomType);
            this.zoomHor = (zoomX && !inverted) || (zoomY && inverted);
            this.zoomVert = (zoomY && !inverted) || (zoomX && inverted);
            this.hasZoom = zoomX || zoomY;

            // Do we need to handle click on a touch device?
            this.runChartClick = chartEvents && !!chartEvents.click;

            this.pinchDown = [];
            this.lastValidTouch = {};

            if (Highcharts.Tooltip && options.tooltip.enabled) {
                chart.tooltip = new Tooltip(chart, options.tooltip);
                this.followTouchMove = pick(options.tooltip.followTouchMove, true);
            }

            this.setDOMEvents();
        },

        /**
         * Add crossbrowser support for chartX and chartY
         * @param {Object} e The event object in standard browsers
         */
        normalize: function (e, chartPosition) {
            var chartX,
                chartY,
                ePos;

            // IE normalizing
            e = e || win.event;
            if (!e.target) {
                e.target = e.srcElement;
            }

            // iOS (#2757)
            ePos = e.touches ?  (e.touches.length ? e.touches.item(0) : e.changedTouches[0]) : e;

            // Get mouse position
            if (!chartPosition) {
                this.chartPosition = chartPosition = offset(this.chart.container);
            }

            // chartX and chartY
            if (ePos.pageX === UNDEFINED) { // IE < 9. #886.
                chartX = mathMax(e.x, e.clientX - chartPosition.left); // #2005, #2129: the second case is
                    // for IE10 quirks mode within framesets
                chartY = e.y;
            } else {
                chartX = ePos.pageX - chartPosition.left;
                chartY = ePos.pageY - chartPosition.top;
            }

            return extend(e, {
                chartX: mathRound(chartX),
                chartY: mathRound(chartY)
            });
        },

        /**
         * Get the click position in terms of axis values.
         *
         * @param {Object} e A pointer event
         */
        getCoordinates: function (e) {
            var coordinates = {
                xAxis: [],
                yAxis: []
            };

            each(this.chart.axes, function (axis) {
                coordinates[axis.isXAxis ? 'xAxis' : 'yAxis'].push({
                    axis: axis,
                    value: axis.toValue(e[axis.horiz ? 'chartX' : 'chartY'])
                });
            });
            return coordinates;
        },

        /**
         * With line type charts with a single tracker, get the point closest to the mouse.
         * Run Point.onMouseOver and display tooltip for the point or points.
         */
        runPointActions: function (e) {

            var pointer = this,
                chart = pointer.chart,
                series = chart.series,
                tooltip = chart.tooltip,
                shared = tooltip ? tooltip.shared : false,
                followPointer,
                hoverPoint = chart.hoverPoint,
                hoverSeries = chart.hoverSeries,
                i,
                distance = [Number.MAX_VALUE, Number.MAX_VALUE], // #4511
                anchor,
                noSharedTooltip,
                stickToHoverSeries,
                directTouch,
                kdpoints = [],
                kdpoint = [],
                kdpointT;

            // For hovering over the empty parts of the plot area (hoverSeries is undefined).
            // If there is one series with point tracking (combo chart), don't go to nearest neighbour.
            if (!shared && !hoverSeries) {
                for (i = 0; i < series.length; i++) {
                    if (series[i].directTouch || !series[i].options.stickyTracking) {
                        series = [];
                    }
                }
            }

            // If it has a hoverPoint and that series requires direct touch (like columns, #3899), or we're on
            // a noSharedTooltip series among shared tooltip series (#4546), use the hoverPoint . Otherwise,
            // search the k-d tree.
            stickToHoverSeries = hoverSeries && (shared ? hoverSeries.noSharedTooltip : hoverSeries.directTouch);
            if (stickToHoverSeries && hoverPoint) {
                kdpoint = [hoverPoint];

            // Handle shared tooltip or cases where a series is not yet hovered
            } else {
                // Find nearest points on all series
                each(series, function (s) {
                    // Skip hidden series
                    noSharedTooltip = s.noSharedTooltip && shared;
                    directTouch = !shared && s.directTouch;
                    if (s.visible && !noSharedTooltip && !directTouch && pick(s.options.enableMouseTracking, true)) { // #3821
                        kdpointT = s.searchPoint(e, !noSharedTooltip && s.kdDimensions === 1); // #3828
                        if (kdpointT) {
                            kdpoints.push(kdpointT);
                        }
                    }
                });
                // Find absolute nearest point
                each(kdpoints, function (p) {
                    if (p) {
                        // Store both closest points, using point.dist and point.distX comparisons (#4645):
                        each(['dist', 'distX'], function (dist, k) {
                            if (isNumber(p[dist])) {
                                var
                                    // It is closer than the reference point
                                    isCloser = p[dist] < distance[k],
                                    // It is equally close, but above the reference point (#4679)
                                    isAbove = p[dist] === distance[k] && p.series.group.zIndex >= kdpoint[k].series.group.zIndex;

                                if (isCloser || isAbove) {
                                    distance[k] = p[dist];
                                    kdpoint[k] = p;
                                }
                            }
                        });
                    }
                });
            }

            // Remove points with different x-positions, required for shared tooltip and crosshairs (#4645):
            if (shared) {
                i = kdpoints.length;
                while (i--) {
                    if (kdpoints[i].clientX !== kdpoint[1].clientX || kdpoints[i].series.noSharedTooltip) {
                        kdpoints.splice(i, 1);
                    }
                }
            }

            // Refresh tooltip for kdpoint if new hover point or tooltip was hidden // #3926, #4200
            if (kdpoint[0] && (kdpoint[0] !== this.prevKDPoint || (tooltip && tooltip.isHidden))) {
                // Draw tooltip if necessary
                if (shared && !kdpoint[0].series.noSharedTooltip) {
                    if (kdpoints.length && tooltip) {
                        tooltip.refresh(kdpoints, e);
                    }

                    // Do mouseover on all points (#3919, #3985, #4410)
                    each(kdpoints, function (point) {
                        point.onMouseOver(e, point !== ((hoverSeries && hoverSeries.directTouch && hoverPoint) || kdpoint[0]));
                    });
                    this.prevKDPoint = kdpoint[1];
                } else {
                    if (tooltip) {
                        tooltip.refresh(kdpoint[0], e);
                    }
                    if (!hoverSeries || !hoverSeries.directTouch) { // #4448
                        kdpoint[0].onMouseOver(e);
                    }
                    this.prevKDPoint = kdpoint[0];
                }

            // Update positions (regardless of kdpoint or hoverPoint)
            } else {
                followPointer = hoverSeries && hoverSeries.tooltipOptions.followPointer;
                if (tooltip && followPointer && !tooltip.isHidden) {
                    anchor = tooltip.getAnchor([{}], e);
                    tooltip.updatePosition({ plotX: anchor[0], plotY: anchor[1] });
                }
            }

            // Start the event listener to pick up the tooltip and crosshairs
            if (!pointer._onDocumentMouseMove) {
                pointer._onDocumentMouseMove = function (e) {
                    if (charts[hoverChartIndex]) {
                        charts[hoverChartIndex].pointer.onDocumentMouseMove(e);
                    }
                };
                addEvent(doc, 'mousemove', pointer._onDocumentMouseMove);
            }

            // Crosshair. For each hover point, loop over axes and draw cross if that point
            // belongs to the axis (#4927).
            each(shared ? kdpoints : [pick(hoverPoint, kdpoint[1])], function (point) { // #5269
                each(chart.axes, function (axis) {
                    // In case of snap = false, point is undefined, and we draw the crosshair anyway (#5066)
                    if (!point || point.series[axis.coll] === axis) {
                        axis.drawCrosshair(e, point);
                    }
                });
            });
        },

        /**
         * Reset the tracking by hiding the tooltip, the hover series state and the hover point
         *
         * @param allowMove {Boolean} Instead of destroying the tooltip altogether, allow moving it if possible
         */
        reset: function (allowMove, delay) {
            var pointer = this,
                chart = pointer.chart,
                hoverSeries = chart.hoverSeries,
                hoverPoint = chart.hoverPoint,
                hoverPoints = chart.hoverPoints,
                tooltip = chart.tooltip,
                tooltipPoints = tooltip && tooltip.shared ? hoverPoints : hoverPoint;

            // Check if the points have moved outside the plot area (#1003, #4736, #5101)
            if (allowMove && tooltipPoints) {
                each(splat(tooltipPoints), function (point) {
                    if (point.series.isCartesian && point.plotX === undefined) {
                        allowMove = false;
                    }
                });
            }
        
            // Just move the tooltip, #349
            if (allowMove) {
                if (tooltip && tooltipPoints) {
                    tooltip.refresh(tooltipPoints);
                    if (hoverPoint) { // #2500
                        hoverPoint.setState(hoverPoint.state, true);
                        each(chart.axes, function (axis) {
                            if (pick(axis.crosshair && axis.crosshair.snap, true)) {
                                axis.drawCrosshair(null, hoverPoint);
                            }  else {
                                axis.hideCrosshair();
                            }
                        });

                    }
                }

            // Full reset
            } else {

                if (hoverPoint) {
                    hoverPoint.onMouseOut();
                }

                if (hoverPoints) {
                    each(hoverPoints, function (point) {
                        point.setState();
                    });
                }

                if (hoverSeries) {
                    hoverSeries.onMouseOut();
                }

                if (tooltip) {
                    tooltip.hide(delay);
                }

                if (pointer._onDocumentMouseMove) {
                    removeEvent(doc, 'mousemove', pointer._onDocumentMouseMove);
                    pointer._onDocumentMouseMove = null;
                }

                // Remove crosshairs
                each(chart.axes, function (axis) {
                    axis.hideCrosshair();
                });

                pointer.hoverX = chart.hoverPoints = chart.hoverPoint = null;

            }
        },

        /**
         * Scale series groups to a certain scale and translation
         */
        scaleGroups: function (attribs, clip) {

            var chart = this.chart,
                seriesAttribs;

            // Scale each series
            each(chart.series, function (series) {
                seriesAttribs = attribs || series.getPlotBox(); // #1701
                if (series.xAxis && series.xAxis.zoomEnabled) {
                    series.group.attr(seriesAttribs);
                    if (series.markerGroup) {
                        series.markerGroup.attr(seriesAttribs);
                        series.markerGroup.clip(clip ? chart.clipRect : null);
                    }
                    if (series.dataLabelsGroup) {
                        series.dataLabelsGroup.attr(seriesAttribs);
                    }
                }
            });

            // Clip
            chart.clipRect.attr(clip || chart.clipBox);
        },

        /**
         * Start a drag operation
         */
        dragStart: function (e) {
            var chart = this.chart;

            // Record the start position
            chart.mouseIsDown = e.type;
            chart.cancelClick = false;
            chart.mouseDownX = this.mouseDownX = e.chartX;
            chart.mouseDownY = this.mouseDownY = e.chartY;
        },

        /**
         * Perform a drag operation in response to a mousemove event while the mouse is down
         */
        drag: function (e) {

            var chart = this.chart,
                chartOptions = chart.options.chart,
                chartX = e.chartX,
                chartY = e.chartY,
                zoomHor = this.zoomHor,
                zoomVert = this.zoomVert,
                plotLeft = chart.plotLeft,
                plotTop = chart.plotTop,
                plotWidth = chart.plotWidth,
                plotHeight = chart.plotHeight,
                clickedInside,
                size,
                selectionMarker = this.selectionMarker,
                mouseDownX = this.mouseDownX,
                mouseDownY = this.mouseDownY,
                panKey = chartOptions.panKey && e[chartOptions.panKey + 'Key'];

            // If the device supports both touch and mouse (like IE11), and we are touch-dragging
            // inside the plot area, don't handle the mouse event. #4339.
            if (selectionMarker && selectionMarker.touch) {
                return;
            }

            // If the mouse is outside the plot area, adjust to cooordinates
            // inside to prevent the selection marker from going outside
            if (chartX < plotLeft) {
                chartX = plotLeft;
            } else if (chartX > plotLeft + plotWidth) {
                chartX = plotLeft + plotWidth;
            }

            if (chartY < plotTop) {
                chartY = plotTop;
            } else if (chartY > plotTop + plotHeight) {
                chartY = plotTop + plotHeight;
            }

            // determine if the mouse has moved more than 10px
            this.hasDragged = Math.sqrt(
                Math.pow(mouseDownX - chartX, 2) +
                Math.pow(mouseDownY - chartY, 2)
            );

            if (this.hasDragged > 10) {
                clickedInside = chart.isInsidePlot(mouseDownX - plotLeft, mouseDownY - plotTop);

                // make a selection
                if (chart.hasCartesianSeries && (this.zoomX || this.zoomY) && clickedInside && !panKey) {
                    if (!selectionMarker) {
                        this.selectionMarker = selectionMarker = chart.renderer.rect(
                            plotLeft,
                            plotTop,
                            zoomHor ? 1 : plotWidth,
                            zoomVert ? 1 : plotHeight,
                            0
                        )
                        .attr({
                            fill: chartOptions.selectionMarkerFill || 'rgba(69,114,167,0.25)',
                            zIndex: 7
                        })
                        .add();
                    }
                }

                // adjust the width of the selection marker
                if (selectionMarker && zoomHor) {
                    size = chartX - mouseDownX;
                    selectionMarker.attr({
                        width: mathAbs(size),
                        x: (size > 0 ? 0 : size) + mouseDownX
                    });
                }
                // adjust the height of the selection marker
                if (selectionMarker && zoomVert) {
                    size = chartY - mouseDownY;
                    selectionMarker.attr({
                        height: mathAbs(size),
                        y: (size > 0 ? 0 : size) + mouseDownY
                    });
                }

                // panning
                if (clickedInside && !selectionMarker && chartOptions.panning) {
                    chart.pan(e, chartOptions.panning);
                }
            }
        },

        /**
         * On mouse up or touch end across the entire document, drop the selection.
         */
        drop: function (e) {
            var pointer = this,
                chart = this.chart,
                hasPinched = this.hasPinched;

            if (this.selectionMarker) {
                var selectionData = {
                        originalEvent: e, // #4890
                        xAxis: [],
                        yAxis: []
                    },
                    selectionBox = this.selectionMarker,
                    selectionLeft = selectionBox.attr ? selectionBox.attr('x') : selectionBox.x,
                    selectionTop = selectionBox.attr ? selectionBox.attr('y') : selectionBox.y,
                    selectionWidth = selectionBox.attr ? selectionBox.attr('width') : selectionBox.width,
                    selectionHeight = selectionBox.attr ? selectionBox.attr('height') : selectionBox.height,
                    runZoom;

                // a selection has been made
                if (this.hasDragged || hasPinched) {

                    // record each axis' min and max
                    each(chart.axes, function (axis) {
                        if (axis.zoomEnabled && defined(axis.min) && (hasPinched || pointer[{ xAxis: 'zoomX', yAxis: 'zoomY' }[axis.coll]])) { // #859, #3569
                            var horiz = axis.horiz,
                                minPixelPadding = e.type === 'touchend' ? axis.minPixelPadding : 0, // #1207, #3075
                                selectionMin = axis.toValue((horiz ? selectionLeft : selectionTop) + minPixelPadding),
                                selectionMax = axis.toValue((horiz ? selectionLeft + selectionWidth : selectionTop + selectionHeight) - minPixelPadding);

                            selectionData[axis.coll].push({
                                axis: axis,
                                min: mathMin(selectionMin, selectionMax), // for reversed axes
                                max: mathMax(selectionMin, selectionMax)
                            });
                            runZoom = true;
                        }
                    });
                    if (runZoom) {
                        fireEvent(chart, 'selection', selectionData, function (args) {
                            chart.zoom(extend(args, hasPinched ? { animation: false } : null));
                        });
                    }

                }
                this.selectionMarker = this.selectionMarker.destroy();

                // Reset scaling preview
                if (hasPinched) {
                    this.scaleGroups();
                }
            }

            // Reset all
            if (chart) { // it may be destroyed on mouse up - #877
                css(chart.container, { cursor: chart._cursor });
                chart.cancelClick = this.hasDragged > 10; // #370
                chart.mouseIsDown = this.hasDragged = this.hasPinched = false;
                this.pinchDown = [];
            }
        },

        onContainerMouseDown: function (e) {

            e = this.normalize(e);

            // issue #295, dragging not always working in Firefox
            if (e.preventDefault) {
                e.preventDefault();
            }

            this.dragStart(e);
        },



        onDocumentMouseUp: function (e) {
            if (charts[hoverChartIndex]) {
                charts[hoverChartIndex].pointer.drop(e);
            }
        },

        /**
         * Special handler for mouse move that will hide the tooltip when the mouse leaves the plotarea.
         * Issue #149 workaround. The mouseleave event does not always fire.
         */
        onDocumentMouseMove: function (e) {
            var chart = this.chart,
                chartPosition = this.chartPosition;

            e = this.normalize(e, chartPosition);

            // If we're outside, hide the tooltip
            if (chartPosition && !this.inClass(e.target, 'highcharts-tracker') &&
                    !chart.isInsidePlot(e.chartX - chart.plotLeft, e.chartY - chart.plotTop)) {
                this.reset();
            }
        },

        /**
         * When mouse leaves the container, hide the tooltip.
         */
        onContainerMouseLeave: function (e) {
            var chart = charts[hoverChartIndex];
            if (chart && (e.relatedTarget || e.toElement)) { // #4886, MS Touch end fires mouseleave but with no related target
                chart.pointer.reset();
                chart.pointer.chartPosition = null; // also reset the chart position, used in #149 fix
            }
        },

        // The mousemove, touchmove and touchstart event handler
        onContainerMouseMove: function (e) {

            var chart = this.chart;

            if (!defined(hoverChartIndex) || !charts[hoverChartIndex] || !charts[hoverChartIndex].mouseIsDown) {
                hoverChartIndex = chart.index;
            }

            e = this.normalize(e);
            e.returnValue = false; // #2251, #3224

            if (chart.mouseIsDown === 'mousedown') {
                this.drag(e);
            }

            // Show the tooltip and run mouse over events (#977)
            if ((this.inClass(e.target, 'highcharts-tracker') ||
                    chart.isInsidePlot(e.chartX - chart.plotLeft, e.chartY - chart.plotTop)) && !chart.openMenu) {
                this.runPointActions(e);
            }
        },

        /**
         * Utility to detect whether an element has, or has a parent with, a specific
         * class name. Used on detection of tracker objects and on deciding whether
         * hovering the tooltip should cause the active series to mouse out.
         */
        inClass: function (element, className) {
            var elemClassName;
            while (element) {
                elemClassName = attr(element, 'class');
                if (elemClassName) {
                    if (elemClassName.indexOf(className) !== -1) {
                        return true;
                    }
                    if (elemClassName.indexOf(PREFIX + 'container') !== -1) {
                        return false;
                    }
                }
                element = element.parentNode;
            }
        },

        onTrackerMouseOut: function (e) {
            var series = this.chart.hoverSeries,
                relatedTarget = e.relatedTarget || e.toElement;

            if (series && relatedTarget && !series.options.stickyTracking && // #4886
                    !this.inClass(relatedTarget, PREFIX + 'tooltip') &&
                    !this.inClass(relatedTarget, PREFIX + 'series-' + series.index)) { // #2499, #4465
                series.onMouseOut();
            }
        },

        onContainerClick: function (e) {
            var chart = this.chart,
                hoverPoint = chart.hoverPoint,
                plotLeft = chart.plotLeft,
                plotTop = chart.plotTop;

            e = this.normalize(e);

            if (!chart.cancelClick) {

                // On tracker click, fire the series and point events. #783, #1583
                if (hoverPoint && this.inClass(e.target, PREFIX + 'tracker')) {

                    // the series click event
                    fireEvent(hoverPoint.series, 'click', extend(e, {
                        point: hoverPoint
                    }));

                    // the point click event
                    if (chart.hoverPoint) { // it may be destroyed (#1844)
                        hoverPoint.firePointEvent('click', e);
                    }

                // When clicking outside a tracker, fire a chart event
                } else {
                    extend(e, this.getCoordinates(e));

                    // fire a click event in the chart
                    if (chart.isInsidePlot(e.chartX - plotLeft, e.chartY - plotTop)) {
                        fireEvent(chart, 'click', e);
                    }
                }


            }
        },

        /**
         * Set the JS DOM events on the container and document. This method should contain
         * a one-to-one assignment between methods and their handlers. Any advanced logic should
         * be moved to the handler reflecting the event's name.
         */
        setDOMEvents: function () {

            var pointer = this,
                container = pointer.chart.container;

            container.onmousedown = function (e) {
                pointer.onContainerMouseDown(e);
            };
            container.onmousemove = function (e) {
                pointer.onContainerMouseMove(e);
            };
            container.onclick = function (e) {
                pointer.onContainerClick(e);
            };
            addEvent(container, 'mouseleave', pointer.onContainerMouseLeave);
            if (chartCount === 1) {
                addEvent(doc, 'mouseup', pointer.onDocumentMouseUp);
            }
            if (hasTouch) {
                container.ontouchstart = function (e) {
                    pointer.onContainerTouchStart(e);
                };
                container.ontouchmove = function (e) {
                    pointer.onContainerTouchMove(e);
                };
                if (chartCount === 1) {
                    addEvent(doc, 'touchend', pointer.onDocumentTouchEnd);
                }
            }

        },

        /**
         * Destroys the Pointer object and disconnects DOM events.
         */
        destroy: function () {
            var prop;

            removeEvent(this.chart.container, 'mouseleave', this.onContainerMouseLeave);
            if (!chartCount) {
                removeEvent(doc, 'mouseup', this.onDocumentMouseUp);
                removeEvent(doc, 'touchend', this.onDocumentTouchEnd);
            }

            // memory and CPU leak
            clearInterval(this.tooltipTimeout);

            for (prop in this) {
                this[prop] = null;
            }
        }
    };


    /* Support for touch devices */
    extend(Highcharts.Pointer.prototype, {

        /**
         * Run translation operations
         */
        pinchTranslate: function (pinchDown, touches, transform, selectionMarker, clip, lastValidTouch) {
            if (this.zoomHor || this.pinchHor) {
                this.pinchTranslateDirection(true, pinchDown, touches, transform, selectionMarker, clip, lastValidTouch);
            }
            if (this.zoomVert || this.pinchVert) {
                this.pinchTranslateDirection(false, pinchDown, touches, transform, selectionMarker, clip, lastValidTouch);
            }
        },

        /**
         * Run translation operations for each direction (horizontal and vertical) independently
         */
        pinchTranslateDirection: function (horiz, pinchDown, touches, transform, selectionMarker, clip, lastValidTouch, forcedScale) {
            var chart = this.chart,
                xy = horiz ? 'x' : 'y',
                XY = horiz ? 'X' : 'Y',
                sChartXY = 'chart' + XY,
                wh = horiz ? 'width' : 'height',
                plotLeftTop = chart['plot' + (horiz ? 'Left' : 'Top')],
                selectionWH,
                selectionXY,
                clipXY,
                scale = forcedScale || 1,
                inverted = chart.inverted,
                bounds = chart.bounds[horiz ? 'h' : 'v'],
                singleTouch = pinchDown.length === 1,
                touch0Start = pinchDown[0][sChartXY],
                touch0Now = touches[0][sChartXY],
                touch1Start = !singleTouch && pinchDown[1][sChartXY],
                touch1Now = !singleTouch && touches[1][sChartXY],
                outOfBounds,
                transformScale,
                scaleKey,
                setScale = function () {
                    if (!singleTouch && mathAbs(touch0Start - touch1Start) > 20) { // Don't zoom if fingers are too close on this axis
                        scale = forcedScale || mathAbs(touch0Now - touch1Now) / mathAbs(touch0Start - touch1Start);
                    }

                    clipXY = ((plotLeftTop - touch0Now) / scale) + touch0Start;
                    selectionWH = chart['plot' + (horiz ? 'Width' : 'Height')] / scale;
                };

            // Set the scale, first pass
            setScale();

            selectionXY = clipXY; // the clip position (x or y) is altered if out of bounds, the selection position is not

            // Out of bounds
            if (selectionXY < bounds.min) {
                selectionXY = bounds.min;
                outOfBounds = true;
            } else if (selectionXY + selectionWH > bounds.max) {
                selectionXY = bounds.max - selectionWH;
                outOfBounds = true;
            }

            // Is the chart dragged off its bounds, determined by dataMin and dataMax?
            if (outOfBounds) {

                // Modify the touchNow position in order to create an elastic drag movement. This indicates
                // to the user that the chart is responsive but can't be dragged further.
                touch0Now -= 0.8 * (touch0Now - lastValidTouch[xy][0]);
                if (!singleTouch) {
                    touch1Now -= 0.8 * (touch1Now - lastValidTouch[xy][1]);
                }

                // Set the scale, second pass to adapt to the modified touchNow positions
                setScale();

            } else {
                lastValidTouch[xy] = [touch0Now, touch1Now];
            }

            // Set geometry for clipping, selection and transformation
            if (!inverted) {
                clip[xy] = clipXY - plotLeftTop;
                clip[wh] = selectionWH;
            }
            scaleKey = inverted ? (horiz ? 'scaleY' : 'scaleX') : 'scale' + XY;
            transformScale = inverted ? 1 / scale : scale;

            selectionMarker[wh] = selectionWH;
            selectionMarker[xy] = selectionXY;
            transform[scaleKey] = scale;
            transform['translate' + XY] = (transformScale * plotLeftTop) + (touch0Now - (transformScale * touch0Start));
        },

        /**
         * Handle touch events with two touches
         */
        pinch: function (e) {

            var self = this,
                chart = self.chart,
                pinchDown = self.pinchDown,
                touches = e.touches,
                touchesLength = touches.length,
                lastValidTouch = self.lastValidTouch,
                hasZoom = self.hasZoom,
                selectionMarker = self.selectionMarker,
                transform = {},
                fireClickEvent = touchesLength === 1 && ((self.inClass(e.target, PREFIX + 'tracker') &&
                    chart.runTrackerClick) || self.runChartClick),
                clip = {};

            // Don't initiate panning until the user has pinched. This prevents us from
            // blocking page scrolling as users scroll down a long page (#4210).
            if (touchesLength > 1) {
                self.initiated = true;
            }

            // On touch devices, only proceed to trigger click if a handler is defined
            if (hasZoom && self.initiated && !fireClickEvent) {
                e.preventDefault();
            }

            // Normalize each touch
            map(touches, function (e) {
                return self.normalize(e);
            });

            // Register the touch start position
            if (e.type === 'touchstart') {
                each(touches, function (e, i) {
                    pinchDown[i] = { chartX: e.chartX, chartY: e.chartY };
                });
                lastValidTouch.x = [pinchDown[0].chartX, pinchDown[1] && pinchDown[1].chartX];
                lastValidTouch.y = [pinchDown[0].chartY, pinchDown[1] && pinchDown[1].chartY];

                // Identify the data bounds in pixels
                each(chart.axes, function (axis) {
                    if (axis.zoomEnabled) {
                        var bounds = chart.bounds[axis.horiz ? 'h' : 'v'],
                            minPixelPadding = axis.minPixelPadding,
                            min = axis.toPixels(pick(axis.options.min, axis.dataMin)),
                            max = axis.toPixels(pick(axis.options.max, axis.dataMax)),
                            absMin = mathMin(min, max),
                            absMax = mathMax(min, max);

                        // Store the bounds for use in the touchmove handler
                        bounds.min = mathMin(axis.pos, absMin - minPixelPadding);
                        bounds.max = mathMax(axis.pos + axis.len, absMax + minPixelPadding);
                    }
                });
                self.res = true; // reset on next move

            // Event type is touchmove, handle panning and pinching
            } else if (pinchDown.length) { // can be 0 when releasing, if touchend fires first


                // Set the marker
                if (!selectionMarker) {
                    self.selectionMarker = selectionMarker = extend({
                        destroy: noop,
                        touch: true
                    }, chart.plotBox);
                }

                self.pinchTranslate(pinchDown, touches, transform, selectionMarker, clip, lastValidTouch);

                self.hasPinched = hasZoom;

                // Scale and translate the groups to provide visual feedback during pinching
                self.scaleGroups(transform, clip);

                // Optionally move the tooltip on touchmove
                if (!hasZoom && self.followTouchMove && touchesLength === 1) {
                    this.runPointActions(self.normalize(e));
                } else if (self.res) {
                    self.res = false;
                    this.reset(false, 0);
                }
            }
        },

        /**
         * General touch handler shared by touchstart and touchmove.
         */
        touch: function (e, start) {
            var chart = this.chart,
                hasMoved,
                pinchDown;

            hoverChartIndex = chart.index;

            if (e.touches.length === 1) {

                e = this.normalize(e);

                if (chart.isInsidePlot(e.chartX - chart.plotLeft, e.chartY - chart.plotTop) && !chart.openMenu) {

                    // Run mouse events and display tooltip etc
                    if (start) {
                        this.runPointActions(e);
                    }

                    // Android fires touchmove events after the touchstart even if the
                    // finger hasn't moved, or moved only a pixel or two. In iOS however,
                    // the touchmove doesn't fire unless the finger moves more than ~4px.
                    // So we emulate this behaviour in Android by checking how much it
                    // moved, and cancelling on small distances. #3450.
                    if (e.type === 'touchmove') {
                        pinchDown = this.pinchDown;
                        hasMoved = pinchDown[0] ? Math.sqrt( // #5266
                            Math.pow(pinchDown[0].chartX - e.chartX, 2) +
                            Math.pow(pinchDown[0].chartY - e.chartY, 2)
                        ) >= 4 : false;
                    }

                    if (pick(hasMoved, true)) {
                        this.pinch(e);
                    }

                } else if (start) {
                    // Hide the tooltip on touching outside the plot area (#1203)
                    this.reset();
                }

            } else if (e.touches.length === 2) {
                this.pinch(e);
            }
        },

        onContainerTouchStart: function (e) {
            this.touch(e, true);
        },

        onContainerTouchMove: function (e) {
            this.touch(e);
        },

        onDocumentTouchEnd: function (e) {
            if (charts[hoverChartIndex]) {
                charts[hoverChartIndex].pointer.drop(e);
            }
        }

    });
    if (win.PointerEvent || win.MSPointerEvent) {

        // The touches object keeps track of the points being touched at all times
        var touches = {},
            hasPointerEvent = !!win.PointerEvent,
            getWebkitTouches = function () {
                var key,
                    fake = [];
                fake.item = function (i) {
                    return this[i];
                };
                for (key in touches) {
                    if (touches.hasOwnProperty(key)) {
                        fake.push({
                            pageX: touches[key].pageX,
                            pageY: touches[key].pageY,
                            target: touches[key].target
                        });
                    }
                }
                return fake;
            },
            translateMSPointer = function (e, method, wktype, func) {
                var p;
                if ((e.pointerType === 'touch' || e.pointerType === e.MSPOINTER_TYPE_TOUCH) && charts[hoverChartIndex]) {
                    func(e);
                    p = charts[hoverChartIndex].pointer;
                    p[method]({
                        type: wktype,
                        target: e.currentTarget,
                        preventDefault: noop,
                        touches: getWebkitTouches()
                    });
                }
            };

        /**
         * Extend the Pointer prototype with methods for each event handler and more
         */
        extend(Pointer.prototype, {
            onContainerPointerDown: function (e) {
                translateMSPointer(e, 'onContainerTouchStart', 'touchstart', function (e) {
                    touches[e.pointerId] = { pageX: e.pageX, pageY: e.pageY, target: e.currentTarget };
                });
            },
            onContainerPointerMove: function (e) {
                translateMSPointer(e, 'onContainerTouchMove', 'touchmove', function (e) {
                    touches[e.pointerId] = { pageX: e.pageX, pageY: e.pageY };
                    if (!touches[e.pointerId].target) {
                        touches[e.pointerId].target = e.currentTarget;
                    }
                });
            },
            onDocumentPointerUp: function (e) {
                translateMSPointer(e, 'onDocumentTouchEnd', 'touchend', function (e) {
                    delete touches[e.pointerId];
                });
            },

            /**
             * Add or remove the MS Pointer specific events
             */
            batchMSEvents: function (fn) {
                fn(this.chart.container, hasPointerEvent ? 'pointerdown' : 'MSPointerDown', this.onContainerPointerDown);
                fn(this.chart.container, hasPointerEvent ? 'pointermove' : 'MSPointerMove', this.onContainerPointerMove);
                fn(doc, hasPointerEvent ? 'pointerup' : 'MSPointerUp', this.onDocumentPointerUp);
            }
        });

        // Disable default IE actions for pinch and such on chart element
        wrap(Pointer.prototype, 'init', function (proceed, chart, options) {
            proceed.call(this, chart, options);
            if (this.hasZoom) { // #4014
                css(chart.container, {
                    '-ms-touch-action': NONE,
                    'touch-action': NONE
                });
            }
        });

        // Add IE specific touch events to chart
        wrap(Pointer.prototype, 'setDOMEvents', function (proceed) {
            proceed.apply(this);
            if (this.hasZoom || this.followTouchMove) {
                this.batchMSEvents(addEvent);
            }
        });
        // Destroy MS events also
        wrap(Pointer.prototype, 'destroy', function (proceed) {
            this.batchMSEvents(removeEvent);
            proceed.call(this);
        });
    }
    /**
     * The overview of the chart's series
     */
    var Legend = Highcharts.Legend = function (chart, options) {
        this.init(chart, options);
    };

    Legend.prototype = {

        /**
         * Initialize the legend
         */
        init: function (chart, options) {

            var legend = this,
                itemStyle = options.itemStyle,
                padding,
                itemMarginTop = options.itemMarginTop || 0;

            this.options = options;

            if (!options.enabled) {
                return;
            }

            legend.itemStyle = itemStyle;
            legend.itemHiddenStyle = merge(itemStyle, options.itemHiddenStyle);
            legend.itemMarginTop = itemMarginTop;
            legend.padding = padding = pick(options.padding, 8);
            legend.initialItemX = padding;
            legend.initialItemY = padding - 5; // 5 is the number of pixels above the text
            legend.maxItemWidth = 0;
            legend.chart = chart;
            legend.itemHeight = 0;
            legend.symbolWidth = pick(options.symbolWidth, 16);
            legend.pages = [];


            // Render it
            legend.render();

            // move checkboxes
            addEvent(legend.chart, 'endResize', function () {
                legend.positionCheckboxes();
            });

        },

        /**
         * Set the colors for the legend item
         * @param {Object} item A Series or Point instance
         * @param {Object} visible Dimmed or colored
         */
        colorizeItem: function (item, visible) {
            var legend = this,
                options = legend.options,
                legendItem = item.legendItem,
                legendLine = item.legendLine,
                legendSymbol = item.legendSymbol,
                hiddenColor = legend.itemHiddenStyle.color,
                textColor = visible ? options.itemStyle.color : hiddenColor,
                symbolColor = visible ? (item.legendColor || item.color || '#CCC') : hiddenColor,
                markerOptions = item.options && item.options.marker,
                symbolAttr = { fill: symbolColor },
                key,
                val;

            if (legendItem) {
                legendItem.css({ fill: textColor, color: textColor }); // color for #1553, oldIE
            }
            if (legendLine) {
                legendLine.attr({ stroke: symbolColor });
            }

            if (legendSymbol) {

                // Apply marker options
                if (markerOptions && legendSymbol.isMarker) { // #585
                    symbolAttr.stroke = symbolColor;
                    markerOptions = item.convertAttribs(markerOptions);
                    for (key in markerOptions) {
                        val = markerOptions[key];
                        if (val !== UNDEFINED) {
                            symbolAttr[key] = val;
                        }
                    }
                }

                legendSymbol.attr(symbolAttr);
            }
        },

        /**
         * Position the legend item
         * @param {Object} item A Series or Point instance
         */
        positionItem: function (item) {
            var legend = this,
                options = legend.options,
                symbolPadding = options.symbolPadding,
                ltr = !options.rtl,
                legendItemPos = item._legendItemPos,
                itemX = legendItemPos[0],
                itemY = legendItemPos[1],
                checkbox = item.checkbox,
                legendGroup = item.legendGroup;

            if (legendGroup && legendGroup.element) {
                legendGroup.translate(
                    ltr ? itemX : legend.legendWidth - itemX - 2 * symbolPadding - 4,
                    itemY
                );
            }

            if (checkbox) {
                checkbox.x = itemX;
                checkbox.y = itemY;
            }
        },

        /**
         * Destroy a single legend item
         * @param {Object} item The series or point
         */
        destroyItem: function (item) {
            var checkbox = item.checkbox;

            // destroy SVG elements
            each(['legendItem', 'legendLine', 'legendSymbol', 'legendGroup'], function (key) {
                if (item[key]) {
                    item[key] = item[key].destroy();
                }
            });

            if (checkbox) {
                discardElement(item.checkbox);
            }
        },

        /**
         * Destroys the legend.
         */
        destroy: function () {
            var legend = this,
                legendGroup = legend.group,
                box = legend.box;

            if (box) {
                legend.box = box.destroy();
            }

            if (legendGroup) {
                legend.group = legendGroup.destroy();
            }
        },

        /**
         * Position the checkboxes after the width is determined
         */
        positionCheckboxes: function (scrollOffset) {
            var alignAttr = this.group.alignAttr,
                translateY,
                clipHeight = this.clipHeight || this.legendHeight,
                titleHeight = this.titleHeight;

            if (alignAttr) {
                translateY = alignAttr.translateY;
                each(this.allItems, function (item) {
                    var checkbox = item.checkbox,
                        top;

                    if (checkbox) {
                        top = translateY + titleHeight + checkbox.y + (scrollOffset || 0) + 3;
                        css(checkbox, {
                            left: (alignAttr.translateX + item.checkboxOffset + checkbox.x - 20) + PX,
                            top: top + PX,
                            display: top > translateY - 6 && top < translateY + clipHeight - 6 ? '' : NONE
                        });
                    }
                });
            }
        },

        /**
         * Render the legend title on top of the legend
         */
        renderTitle: function () {
            var options = this.options,
                padding = this.padding,
                titleOptions = options.title,
                titleHeight = 0,
                bBox;

            if (titleOptions.text) {
                if (!this.title) {
                    this.title = this.chart.renderer.label(titleOptions.text, padding - 3, padding - 4, null, null, null, null, null, 'legend-title')
                        .attr({ zIndex: 1 })
                        .css(titleOptions.style)
                        .add(this.group);
                }
                bBox = this.title.getBBox();
                titleHeight = bBox.height;
                this.offsetWidth = bBox.width; // #1717
                this.contentGroup.attr({ translateY: titleHeight });
            }
            this.titleHeight = titleHeight;
        },

        /**
         * Set the legend item text
         */
        setText: function (item) {
            var options = this.options;
            item.legendItem.attr({
                text: options.labelFormat ? format(options.labelFormat, item) : options.labelFormatter.call(item)
            });
        },

        /**
         * Render a single specific legend item
         * @param {Object} item A series or point
         */
        renderItem: function (item) {
            var legend = this,
                chart = legend.chart,
                renderer = chart.renderer,
                options = legend.options,
                horizontal = options.layout === 'horizontal',
                symbolWidth = legend.symbolWidth,
                symbolPadding = options.symbolPadding,
                itemStyle = legend.itemStyle,
                itemHiddenStyle = legend.itemHiddenStyle,
                padding = legend.padding,
                itemDistance = horizontal ? pick(options.itemDistance, 20) : 0,
                ltr = !options.rtl,
                itemHeight,
                widthOption = options.width,
                itemMarginBottom = options.itemMarginBottom || 0,
                itemMarginTop = legend.itemMarginTop,
                initialItemX = legend.initialItemX,
                bBox,
                itemWidth,
                li = item.legendItem,
                series = item.series && item.series.drawLegendSymbol ? item.series : item,
                seriesOptions = series.options,
                showCheckbox = legend.createCheckboxForItem && seriesOptions && seriesOptions.showCheckbox,
                useHTML = options.useHTML;

            if (!li) { // generate it once, later move it

                // Generate the group box
                // A group to hold the symbol and text. Text is to be appended in Legend class.
                item.legendGroup = renderer.g('legend-item')
                    .attr({ zIndex: 1 })
                    .add(legend.scrollGroup);

                // Generate the list item text and add it to the group
                item.legendItem = li = renderer.text(
                        '',
                        ltr ? symbolWidth + symbolPadding : -symbolPadding,
                        legend.baseline || 0,
                        useHTML
                    )
                    .css(merge(item.visible ? itemStyle : itemHiddenStyle)) // merge to prevent modifying original (#1021)
                    .attr({
                        align: ltr ? 'left' : 'right',
                        zIndex: 2
                    })
                    .add(item.legendGroup);

                // Get the baseline for the first item - the font size is equal for all
                if (!legend.baseline) {
                    legend.fontMetrics = renderer.fontMetrics(itemStyle.fontSize, li);
                    legend.baseline = legend.fontMetrics.f + 3 + itemMarginTop;
                    li.attr('y', legend.baseline);
                }

                // Draw the legend symbol inside the group box
                series.drawLegendSymbol(legend, item);

                if (legend.setItemEvents) {
                    legend.setItemEvents(item, li, useHTML, itemStyle, itemHiddenStyle);
                }

                // add the HTML checkbox on top
                if (showCheckbox) {
                    legend.createCheckboxForItem(item);
                }
            }

            // Colorize the items
            legend.colorizeItem(item, item.visible);

            // Always update the text
            legend.setText(item);

            // calculate the positions for the next line
            bBox = li.getBBox();

            itemWidth = item.checkboxOffset =
                options.itemWidth ||
                item.legendItemWidth ||
                symbolWidth + symbolPadding + bBox.width + itemDistance + (showCheckbox ? 20 : 0);
            legend.itemHeight = itemHeight = mathRound(item.legendItemHeight || bBox.height);

            // if the item exceeds the width, start a new line
            if (horizontal && legend.itemX - initialItemX + itemWidth >
                    (widthOption || (chart.chartWidth - 2 * padding - initialItemX - options.x))) {
                legend.itemX = initialItemX;
                legend.itemY += itemMarginTop + legend.lastLineHeight + itemMarginBottom;
                legend.lastLineHeight = 0; // reset for next line (#915, #3976)
            }

            // If the item exceeds the height, start a new column
            /*if (!horizontal && legend.itemY + options.y + itemHeight > chart.chartHeight - spacingTop - spacingBottom) {
                legend.itemY = legend.initialItemY;
                legend.itemX += legend.maxItemWidth;
                legend.maxItemWidth = 0;
            }*/

            // Set the edge positions
            legend.maxItemWidth = mathMax(legend.maxItemWidth, itemWidth);
            legend.lastItemY = itemMarginTop + legend.itemY + itemMarginBottom;
            legend.lastLineHeight = mathMax(itemHeight, legend.lastLineHeight); // #915

            // cache the position of the newly generated or reordered items
            item._legendItemPos = [legend.itemX, legend.itemY];

            // advance
            if (horizontal) {
                legend.itemX += itemWidth;

            } else {
                legend.itemY += itemMarginTop + itemHeight + itemMarginBottom;
                legend.lastLineHeight = itemHeight;
            }

            // the width of the widest item
            legend.offsetWidth = widthOption || mathMax(
                (horizontal ? legend.itemX - initialItemX - itemDistance : itemWidth) + padding,
                legend.offsetWidth
            );
        },

        /**
         * Get all items, which is one item per series for normal series and one item per point
         * for pie series.
         */
        getAllItems: function () {
            var allItems = [];
            each(this.chart.series, function (series) {
                var seriesOptions = series.options;

                // Handle showInLegend. If the series is linked to another series, defaults to false.
                if (!pick(seriesOptions.showInLegend, !defined(seriesOptions.linkedTo) ? UNDEFINED : false, true)) {
                    return;
                }

                // use points or series for the legend item depending on legendType
                allItems = allItems.concat(
                        series.legendItems ||
                        (seriesOptions.legendType === 'point' ?
                                series.data :
                                series)
                );
            });
            return allItems;
        },

        /**
         * Adjust the chart margins by reserving space for the legend on only one side
         * of the chart. If the position is set to a corner, top or bottom is reserved
         * for horizontal legends and left or right for vertical ones.
         */
        adjustMargins: function (margin, spacing) {
            var chart = this.chart,
                options = this.options,
                // Use the first letter of each alignment option in order to detect the side
                alignment = options.align.charAt(0) + options.verticalAlign.charAt(0) + options.layout.charAt(0); // #4189 - use charAt(x) notation instead of [x] for IE7

            if (this.display && !options.floating) {

                each([
                    /(lth|ct|rth)/,
                    /(rtv|rm|rbv)/,
                    /(rbh|cb|lbh)/,
                    /(lbv|lm|ltv)/
                ], function (alignments, side) {
                    if (alignments.test(alignment) && !defined(margin[side])) {
                        // Now we have detected on which side of the chart we should reserve space for the legend
                        chart[marginNames[side]] = mathMax(
                            chart[marginNames[side]],
                            chart.legend[(side + 1) % 2 ? 'legendHeight' : 'legendWidth'] +
                                [1, -1, -1, 1][side] * options[(side % 2) ? 'x' : 'y'] +
                                pick(options.margin, 12) +
                                spacing[side]
                        );
                    }
                });
            }
        },

        /**
         * Render the legend. This method can be called both before and after
         * chart.render. If called after, it will only rearrange items instead
         * of creating new ones.
         */
        render: function () {
            var legend = this,
                chart = legend.chart,
                renderer = chart.renderer,
                legendGroup = legend.group,
                allItems,
                display,
                legendWidth,
                legendHeight,
                box = legend.box,
                options = legend.options,
                padding = legend.padding,
                legendBorderWidth = options.borderWidth,
                legendBackgroundColor = options.backgroundColor;

            legend.itemX = legend.initialItemX;
            legend.itemY = legend.initialItemY;
            legend.offsetWidth = 0;
            legend.lastItemY = 0;

            if (!legendGroup) {
                legend.group = legendGroup = renderer.g('legend')
                    .attr({ zIndex: 7 })
                    .add();
                legend.contentGroup = renderer.g()
                    .attr({ zIndex: 1 }) // above background
                    .add(legendGroup);
                legend.scrollGroup = renderer.g()
                    .add(legend.contentGroup);
            }

            legend.renderTitle();

            // add each series or point
            allItems = legend.getAllItems();

            // sort by legendIndex
            stableSort(allItems, function (a, b) {
                return ((a.options && a.options.legendIndex) || 0) - ((b.options && b.options.legendIndex) || 0);
            });

            // reversed legend
            if (options.reversed) {
                allItems.reverse();
            }

            legend.allItems = allItems;
            legend.display = display = !!allItems.length;

            // render the items
            legend.lastLineHeight = 0;
            each(allItems, function (item) {
                legend.renderItem(item);
            });

            // Get the box
            legendWidth = (options.width || legend.offsetWidth) + padding;
            legendHeight = legend.lastItemY + legend.lastLineHeight + legend.titleHeight;
            legendHeight = legend.handleOverflow(legendHeight);
            legendHeight += padding;

            // Draw the border and/or background
            if (legendBorderWidth || legendBackgroundColor) {

                if (!box) {
                    legend.box = box = renderer.rect(
                        0,
                        0,
                        legendWidth,
                        legendHeight,
                        options.borderRadius,
                        legendBorderWidth || 0
                    ).attr({
                        stroke: options.borderColor,
                        'stroke-width': legendBorderWidth || 0,
                        fill: legendBackgroundColor || NONE
                    })
                    .add(legendGroup)
                    .shadow(options.shadow);
                    box.isNew = true;

                } else if (legendWidth > 0 && legendHeight > 0) {
                    box[box.isNew ? 'attr' : 'animate'](
                        box.crisp({ width: legendWidth, height: legendHeight })
                    );
                    box.isNew = false;
                }

                // hide the border if no items
                box[display ? 'show' : 'hide']();
            }

            legend.legendWidth = legendWidth;
            legend.legendHeight = legendHeight;

            // Now that the legend width and height are established, put the items in the
            // final position
            each(allItems, function (item) {
                legend.positionItem(item);
            });

            // 1.x compatibility: positioning based on style
            /*var props = ['left', 'right', 'top', 'bottom'],
                prop,
                i = 4;
            while (i--) {
                prop = props[i];
                if (options.style[prop] && options.style[prop] !== 'auto') {
                    options[i < 2 ? 'align' : 'verticalAlign'] = prop;
                    options[i < 2 ? 'x' : 'y'] = pInt(options.style[prop]) * (i % 2 ? -1 : 1);
                }
            }*/

            if (display) {
                legendGroup.align(extend({
                    width: legendWidth,
                    height: legendHeight
                }, options), true, 'spacingBox');
            }

            if (!chart.isResizing) {
                this.positionCheckboxes();
            }
        },

        /**
         * Set up the overflow handling by adding navigation with up and down arrows below the
         * legend.
         */
        handleOverflow: function (legendHeight) {
            var legend = this,
                chart = this.chart,
                renderer = chart.renderer,
                options = this.options,
                optionsY = options.y,
                alignTop = options.verticalAlign === 'top',
                spaceHeight = chart.spacingBox.height + (alignTop ? -optionsY : optionsY) - this.padding,
                maxHeight = options.maxHeight,
                clipHeight,
                clipRect = this.clipRect,
                navOptions = options.navigation,
                animation = pick(navOptions.animation, true),
                arrowSize = navOptions.arrowSize || 12,
                nav = this.nav,
                pages = this.pages,
                padding = this.padding,
                lastY,
                allItems = this.allItems,
                clipToHeight = function (height) {
                    clipRect.attr({
                        height: height
                    });

                    // useHTML
                    if (legend.contentGroup.div) {
                        legend.contentGroup.div.style.clip = 'rect(' + padding + 'px,9999px,' + (padding + height) + 'px,0)';
                    }
                };


            // Adjust the height
            if (options.layout === 'horizontal') {
                spaceHeight /= 2;
            }
            if (maxHeight) {
                spaceHeight = mathMin(spaceHeight, maxHeight);
            }

            // Reset the legend height and adjust the clipping rectangle
            pages.length = 0;
            if (legendHeight > spaceHeight && navOptions.enabled !== false) {

                this.clipHeight = clipHeight = mathMax(spaceHeight - 20 - this.titleHeight - padding, 0);
                this.currentPage = pick(this.currentPage, 1);
                this.fullHeight = legendHeight;

                // Fill pages with Y positions so that the top of each a legend item defines
                // the scroll top for each page (#2098)
                each(allItems, function (item, i) {
                    var y = item._legendItemPos[1],
                        h = mathRound(item.legendItem.getBBox().height),
                        len = pages.length;

                    if (!len || (y - pages[len - 1] > clipHeight && (lastY || y) !== pages[len - 1])) {
                        pages.push(lastY || y);
                        len++;
                    }

                    if (i === allItems.length - 1 && y + h - pages[len - 1] > clipHeight) {
                        pages.push(y);
                    }
                    if (y !== lastY) {
                        lastY = y;
                    }
                });

                // Only apply clipping if needed. Clipping causes blurred legend in PDF export (#1787)
                if (!clipRect) {
                    clipRect = legend.clipRect = renderer.clipRect(0, padding, 9999, 0);
                    legend.contentGroup.clip(clipRect);
                }

                clipToHeight(clipHeight);

                // Add navigation elements
                if (!nav) {
                    this.nav = nav = renderer.g().attr({ zIndex: 1 }).add(this.group);
                    this.up = renderer.symbol('triangle', 0, 0, arrowSize, arrowSize)
                        .on('click', function () {
                            legend.scroll(-1, animation);
                        })
                        .add(nav);
                    this.pager = renderer.text('', 15, 10)
                        .css(navOptions.style)
                        .add(nav);
                    this.down = renderer.symbol('triangle-down', 0, 0, arrowSize, arrowSize)
                        .on('click', function () {
                            legend.scroll(1, animation);
                        })
                        .add(nav);
                }

                // Set initial position
                legend.scroll(0);

                legendHeight = spaceHeight;

            } else if (nav) {
                clipToHeight(chart.chartHeight);
                nav.hide();
                this.scrollGroup.attr({
                    translateY: 1
                });
                this.clipHeight = 0; // #1379
            }

            return legendHeight;
        },

        /**
         * Scroll the legend by a number of pages
         * @param {Object} scrollBy
         * @param {Object} animation
         */
        scroll: function (scrollBy, animation) {
            var pages = this.pages,
                pageCount = pages.length,
                currentPage = this.currentPage + scrollBy,
                clipHeight = this.clipHeight,
                navOptions = this.options.navigation,
                activeColor = navOptions.activeColor,
                inactiveColor = navOptions.inactiveColor,
                pager = this.pager,
                padding = this.padding,
                scrollOffset;

            // When resizing while looking at the last page
            if (currentPage > pageCount) {
                currentPage = pageCount;
            }

            if (currentPage > 0) {

                if (animation !== UNDEFINED) {
                    setAnimation(animation, this.chart);
                }

                this.nav.attr({
                    translateX: padding,
                    translateY: clipHeight + this.padding + 7 + this.titleHeight,
                    visibility: VISIBLE
                });
                this.up.attr({
                        fill: currentPage === 1 ? inactiveColor : activeColor
                    })
                    .css({
                        cursor: currentPage === 1 ? 'default' : 'pointer'
                    });
                pager.attr({
                    text: currentPage + '/' + pageCount
                });
                this.down.attr({
                        x: 18 + this.pager.getBBox().width, // adjust to text width
                        fill: currentPage === pageCount ? inactiveColor : activeColor
                    })
                    .css({
                        cursor: currentPage === pageCount ? 'default' : 'pointer'
                    });

                scrollOffset = -pages[currentPage - 1] + this.initialItemY;

                this.scrollGroup.animate({
                    translateY: scrollOffset
                });

                this.currentPage = currentPage;
                this.positionCheckboxes(scrollOffset);
            }

        }

    };

    /*
     * LegendSymbolMixin
     */

    var LegendSymbolMixin = Highcharts.LegendSymbolMixin = {

        /**
         * Get the series' symbol in the legend
         *
         * @param {Object} legend The legend object
         * @param {Object} item The series (this) or point
         */
        drawRectangle: function (legend, item) {
            var symbolHeight = legend.options.symbolHeight || legend.fontMetrics.f;

            item.legendSymbol = this.chart.renderer.rect(
                0,
                legend.baseline - symbolHeight + 1, // #3988
                legend.symbolWidth,
                symbolHeight,
                legend.options.symbolRadius || 0
            ).attr({
                zIndex: 3
            }).add(item.legendGroup);

        },

        /**
         * Get the series' symbol in the legend. This method should be overridable to create custom
         * symbols through Highcharts.seriesTypes[type].prototype.drawLegendSymbols.
         *
         * @param {Object} legend The legend object
         */
        drawLineMarker: function (legend) {

            var options = this.options,
                markerOptions = options.marker,
                radius,
                legendSymbol,
                symbolWidth = legend.symbolWidth,
                renderer = this.chart.renderer,
                legendItemGroup = this.legendGroup,
                verticalCenter = legend.baseline - mathRound(legend.fontMetrics.b * 0.3),
                attr;

            // Draw the line
            if (options.lineWidth) {
                attr = {
                    'stroke-width': options.lineWidth
                };
                if (options.dashStyle) {
                    attr.dashstyle = options.dashStyle;
                }
                this.legendLine = renderer.path([
                    M,
                    0,
                    verticalCenter,
                    L,
                    symbolWidth,
                    verticalCenter
                ])
                .attr(attr)
                .add(legendItemGroup);
            }

            // Draw the marker
            if (markerOptions && markerOptions.enabled !== false) {
                radius = markerOptions.radius;
                this.legendSymbol = legendSymbol = renderer.symbol(
                    this.symbol,
                    (symbolWidth / 2) - radius,
                    verticalCenter - radius,
                    2 * radius,
                    2 * radius,
                    markerOptions
                )
                .add(legendItemGroup);
                legendSymbol.isMarker = true;
            }
        }
    };

    // Workaround for #2030, horizontal legend items not displaying in IE11 Preview,
    // and for #2580, a similar drawing flaw in Firefox 26.
    // Explore if there's a general cause for this. The problem may be related
    // to nested group elements, as the legend item texts are within 4 group elements.
    if (/Trident\/7\.0/.test(userAgent) || isFirefox) {
        wrap(Legend.prototype, 'positionItem', function (proceed, item) {
            var legend = this,
                runPositionItem = function () { // If chart destroyed in sync, this is undefined (#2030)
                    if (item._legendItemPos) {
                        proceed.call(legend, item);
                    }
                };

            // Do it now, for export and to get checkbox placement
            runPositionItem();

            // Do it after to work around the core issue
            setTimeout(runPositionItem);
        });
    }
    /**
     * The Chart class
     * @param {String|Object} renderTo The DOM element to render to, or its id
     * @param {Object} options
     * @param {Function} callback Function to run when the chart has loaded
     */
    var Chart = Highcharts.Chart = function () {
        this.getArgs.apply(this, arguments);
    };

    Highcharts.chart = function (a, b, c) {
        return new Chart(a, b, c);
    };

    Chart.prototype = {

        /**
         * Hook for modules
         */
        callbacks: [],

        /**
         * Handle the arguments passed to the constructor
         * @returns {Array} Arguments without renderTo
         */
        getArgs: function () {
            var args = [].slice.call(arguments);
        
            // Remove the optional first argument, renderTo, and
            // set it on this.
            if (isString(args[0]) || args[0].nodeName) {
                this.renderTo = args.shift();
            }
            this.init(args[0], args[1]);
        },

        /**
         * Initialize the chart
         */
        init: function (userOptions, callback) {

            // Handle regular options
            var options,
                seriesOptions = userOptions.series; // skip merging data points to increase performance

            userOptions.series = null;
            options = merge(defaultOptions, userOptions); // do the merge
            options.series = userOptions.series = seriesOptions; // set back the series data
            this.userOptions = userOptions;

            var optionsChart = options.chart;

            // Create margin & spacing array
            this.margin = this.splashArray('margin', optionsChart);
            this.spacing = this.splashArray('spacing', optionsChart);

            var chartEvents = optionsChart.events;

            //this.runChartClick = chartEvents && !!chartEvents.click;
            this.bounds = { h: {}, v: {} }; // Pixel data bounds for touch zoom

            this.callback = callback;
            this.isResizing = 0;
            this.options = options;
            //chartTitleOptions = UNDEFINED;
            //chartSubtitleOptions = UNDEFINED;

            this.axes = [];
            this.series = [];
            this.hasCartesianSeries = optionsChart.showAxes;
            //this.axisOffset = UNDEFINED;
            //this.maxTicks = UNDEFINED; // handle the greatest amount of ticks on grouped axes
            //this.inverted = UNDEFINED;
            //this.loadingShown = UNDEFINED;
            //this.container = UNDEFINED;
            //this.chartWidth = UNDEFINED;
            //this.chartHeight = UNDEFINED;
            //this.marginRight = UNDEFINED;
            //this.marginBottom = UNDEFINED;
            //this.containerWidth = UNDEFINED;
            //this.containerHeight = UNDEFINED;
            //this.oldChartWidth = UNDEFINED;
            //this.oldChartHeight = UNDEFINED;

            //this.renderTo = UNDEFINED;
            //this.renderToClone = UNDEFINED;

            //this.spacingBox = UNDEFINED

            //this.legend = UNDEFINED;

            // Elements
            //this.chartBackground = UNDEFINED;
            //this.plotBackground = UNDEFINED;
            //this.plotBGImage = UNDEFINED;
            //this.plotBorder = UNDEFINED;
            //this.loadingDiv = UNDEFINED;
            //this.loadingSpan = UNDEFINED;

            var chart = this,
                eventType;

            // Add the chart to the global lookup
            chart.index = charts.length;
            charts.push(chart);
            chartCount++;

            // Set up auto resize
            if (optionsChart.reflow !== false) {
                addEvent(chart, 'load', function () {
                    chart.initReflow();
                });
            }

            // Chart event handlers
            if (chartEvents) {
                for (eventType in chartEvents) {
                    addEvent(chart, eventType, chartEvents[eventType]);
                }
            }

            chart.xAxis = [];
            chart.yAxis = [];

            // Expose methods and variables
            chart.animation = useCanVG ? false : pick(optionsChart.animation, true);
            chart.pointCount = chart.colorCounter = chart.symbolCounter = 0;

            chart.firstRender();
        },

        /**
         * Initialize an individual series, called internally before render time
         */
        initSeries: function (options) {
            var chart = this,
                optionsChart = chart.options.chart,
                type = options.type || optionsChart.type || optionsChart.defaultSeriesType,
                series,
                constr = seriesTypes[type];

            // No such series type
            if (!constr) {
                error(17, true);
            }

            series = new constr();
            series.init(this, options);
            return series;
        },

        /**
         * Check whether a given point is within the plot area
         *
         * @param {Number} plotX Pixel x relative to the plot area
         * @param {Number} plotY Pixel y relative to the plot area
         * @param {Boolean} inverted Whether the chart is inverted
         */
        isInsidePlot: function (plotX, plotY, inverted) {
            var x = inverted ? plotY : plotX,
                y = inverted ? plotX : plotY;

            return x >= 0 &&
                x <= this.plotWidth &&
                y >= 0 &&
                y <= this.plotHeight;
        },

        /**
         * Redraw legend, axes or series based on updated data
         *
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         */
        redraw: function (animation) {
            var chart = this,
                axes = chart.axes,
                series = chart.series,
                pointer = chart.pointer,
                legend = chart.legend,
                redrawLegend = chart.isDirtyLegend,
                hasStackedSeries,
                hasDirtyStacks,
                hasCartesianSeries = chart.hasCartesianSeries,
                isDirtyBox = chart.isDirtyBox,
                seriesLength = series.length,
                i = seriesLength,
                serie,
                renderer = chart.renderer,
                isHiddenChart = renderer.isHidden(),
                afterRedraw = [];

            setAnimation(animation, chart);

            if (isHiddenChart) {
                chart.cloneRenderTo();
            }

            // Adjust title layout (reflow multiline text)
            chart.layOutTitles();

            // link stacked series
            while (i--) {
                serie = series[i];

                if (serie.options.stacking) {
                    hasStackedSeries = true;

                    if (serie.isDirty) {
                        hasDirtyStacks = true;
                        break;
                    }
                }
            }
            if (hasDirtyStacks) { // mark others as dirty
                i = seriesLength;
                while (i--) {
                    serie = series[i];
                    if (serie.options.stacking) {
                        serie.isDirty = true;
                    }
                }
            }

            // Handle updated data in the series
            each(series, function (serie) {
                if (serie.isDirty) {
                    if (serie.options.legendType === 'point') {
                        if (serie.updateTotals) {
                            serie.updateTotals();
                        }
                        redrawLegend = true;
                    }
                }
                if (serie.isDirtyData) {
                    fireEvent(serie, 'updatedData');
                }
            });

            // handle added or removed series
            if (redrawLegend && legend.options.enabled) { // series or pie points are added or removed
                // draw legend graphics
                legend.render();

                chart.isDirtyLegend = false;
            }

            // reset stacks
            if (hasStackedSeries) {
                chart.getStacks();
            }


            if (hasCartesianSeries) {
                if (!chart.isResizing) {

                    // reset maxTicks
                    chart.maxTicks = null;

                    // set axes scales
                    each(axes, function (axis) {
                        axis.setScale();
                    });
                }
            }

            chart.getMargins(); // #3098

            if (hasCartesianSeries) {
                // If one axis is dirty, all axes must be redrawn (#792, #2169)
                each(axes, function (axis) {
                    if (axis.isDirty) {
                        isDirtyBox = true;
                    }
                });

                // redraw axes
                each(axes, function (axis) {

                    // Fire 'afterSetExtremes' only if extremes are set
                    var key = axis.min + ',' + axis.max;
                    if (axis.extKey !== key) { // #821, #4452
                        axis.extKey = key;
                        afterRedraw.push(function () { // prevent a recursive call to chart.redraw() (#1119)
                            fireEvent(axis, 'afterSetExtremes', extend(axis.eventArgs, axis.getExtremes())); // #747, #751
                            delete axis.eventArgs;
                        });
                    }
                    if (isDirtyBox || hasStackedSeries) {
                        axis.redraw();
                    }
                });
            }

            // the plot areas size has changed
            if (isDirtyBox) {
                chart.drawChartBox();
            }


            // redraw affected series
            each(series, function (serie) {
                if (serie.isDirty && serie.visible &&
                        (!serie.isCartesian || serie.xAxis)) { // issue #153
                    serie.redraw();
                }
            });

            // move tooltip or reset
            if (pointer) {
                pointer.reset(true);
            }

            // redraw if canvas
            renderer.draw();

            // fire the event
            fireEvent(chart, 'redraw');

            if (isHiddenChart) {
                chart.cloneRenderTo(true);
            }

            // Fire callbacks that are put on hold until after the redraw
            each(afterRedraw, function (callback) {
                callback.call();
            });
        },

        /**
         * Get an axis, series or point object by id.
         * @param id {String} The id as given in the configuration options
         */
        get: function (id) {
            var chart = this,
                axes = chart.axes,
                series = chart.series;

            var i,
                j,
                points;

            // search axes
            for (i = 0; i < axes.length; i++) {
                if (axes[i].options.id === id) {
                    return axes[i];
                }
            }

            // search series
            for (i = 0; i < series.length; i++) {
                if (series[i].options.id === id) {
                    return series[i];
                }
            }

            // search points
            for (i = 0; i < series.length; i++) {
                points = series[i].points || [];
                for (j = 0; j < points.length; j++) {
                    if (points[j].id === id) {
                        return points[j];
                    }
                }
            }
            return null;
        },

        /**
         * Create the Axis instances based on the config options
         */
        getAxes: function () {
            var chart = this,
                options = this.options,
                xAxisOptions = options.xAxis = splat(options.xAxis || {}),
                yAxisOptions = options.yAxis = splat(options.yAxis || {}),
                optionsArray;

            // make sure the options are arrays and add some members
            each(xAxisOptions, function (axis, i) {
                axis.index = i;
                axis.isX = true;
            });

            each(yAxisOptions, function (axis, i) {
                axis.index = i;
            });

            // concatenate all axis options into one array
            optionsArray = xAxisOptions.concat(yAxisOptions);

            each(optionsArray, function (axisOptions) {
                new Axis(chart, axisOptions); // eslint-disable-line no-new
            });
        },


        /**
         * Get the currently selected points from all series
         */
        getSelectedPoints: function () {
            var points = [];
            each(this.series, function (serie) {
                points = points.concat(grep(serie.points || [], function (point) {
                    return point.selected;
                }));
            });
            return points;
        },

        /**
         * Get the currently selected series
         */
        getSelectedSeries: function () {
            return grep(this.series, function (serie) {
                return serie.selected;
            });
        },

        /**
         * Show the title and subtitle of the chart
         *
         * @param titleOptions {Object} New title options
         * @param subtitleOptions {Object} New subtitle options
         *
         */
        setTitle: function (titleOptions, subtitleOptions, redraw) {
            var chart = this,
                options = chart.options,
                chartTitleOptions,
                chartSubtitleOptions;

            chartTitleOptions = options.title = merge(options.title, titleOptions);
            chartSubtitleOptions = options.subtitle = merge(options.subtitle, subtitleOptions);

            // add title and subtitle
            each([
                ['title', titleOptions, chartTitleOptions],
                ['subtitle', subtitleOptions, chartSubtitleOptions]
            ], function (arr) {
                var name = arr[0],
                    title = chart[name],
                    titleOptions = arr[1],
                    chartTitleOptions = arr[2];

                if (title && titleOptions) {
                    chart[name] = title = title.destroy(); // remove old
                }

                if (chartTitleOptions && chartTitleOptions.text && !title) {
                    chart[name] = chart.renderer.text(
                        chartTitleOptions.text,
                        0,
                        0,
                        chartTitleOptions.useHTML
                    )
                    .attr({
                        align: chartTitleOptions.align,
                        'class': PREFIX + name,
                        zIndex: chartTitleOptions.zIndex || 4
                    })
                    .css(chartTitleOptions.style)
                    .add();
            
                }
            });
            chart.layOutTitles(redraw);
        },

        /**
         * Lay out the chart titles and cache the full offset height for use in getMargins
         */
        layOutTitles: function (redraw) {
            var titleOffset = 0,
                title = this.title,
                subtitle = this.subtitle,
                options = this.options,
                titleOptions = options.title,
                subtitleOptions = options.subtitle,
                requiresDirtyBox,
                renderer = this.renderer,
                spacingBox = this.spacingBox;

            if (title) {
                title
                    .css({ width: (titleOptions.width || spacingBox.width + titleOptions.widthAdjust) + PX })
                    .align(extend({
                        y: renderer.fontMetrics(titleOptions.style.fontSize, title).b - 3
                    }, titleOptions), false, spacingBox);

                if (!titleOptions.floating && !titleOptions.verticalAlign) {
                    titleOffset = title.getBBox().height;
                }
            }
            if (subtitle) {
                subtitle
                    .css({ width: (subtitleOptions.width || spacingBox.width + subtitleOptions.widthAdjust) + PX })
                    .align(extend({
                        y: titleOffset + (titleOptions.margin - 13) + renderer.fontMetrics(subtitleOptions.style.fontSize, title).b
                    }, subtitleOptions), false, spacingBox);

                if (!subtitleOptions.floating && !subtitleOptions.verticalAlign) {
                    titleOffset = mathCeil(titleOffset + subtitle.getBBox().height);
                }
            }

            requiresDirtyBox = this.titleOffset !== titleOffset;
            this.titleOffset = titleOffset; // used in getMargins

            if (!this.isDirtyBox && requiresDirtyBox) {
                this.isDirtyBox = requiresDirtyBox;
                // Redraw if necessary (#2719, #2744)
                if (this.hasRendered && pick(redraw, true) && this.isDirtyBox) {
                    this.redraw();
                }
            }
        },

        /**
         * Get chart width and height according to options and container size
         */
        getChartSize: function () {
            var chart = this,
                optionsChart = chart.options.chart,
                widthOption = optionsChart.width,
                heightOption = optionsChart.height,
                renderTo = chart.renderToClone || chart.renderTo;

            // Get inner width and height
            if (!defined(widthOption)) {
                chart.containerWidth = getStyle(renderTo, 'width');
            }
            if (!defined(heightOption)) {
                chart.containerHeight = getStyle(renderTo, 'height');
            }

            chart.chartWidth = mathMax(0, widthOption || chart.containerWidth || 600); // #1393, 1460
            chart.chartHeight = mathMax(0, pick(heightOption,
                // the offsetHeight of an empty container is 0 in standard browsers, but 19 in IE7:
                chart.containerHeight > 19 ? chart.containerHeight : 400));
        },

        /**
         * Create a clone of the chart's renderTo div and place it outside the viewport to allow
         * size computation on chart.render and chart.redraw
         */
        cloneRenderTo: function (revert) {
            var clone = this.renderToClone,
                container = this.container;

            // Destroy the clone and bring the container back to the real renderTo div
            if (revert) {
                if (clone) {
                    this.renderTo.appendChild(container);
                    discardElement(clone);
                    delete this.renderToClone;
                }

            // Set up the clone
            } else {
                if (container && container.parentNode === this.renderTo) {
                    this.renderTo.removeChild(container); // do not clone this
                }
                this.renderToClone = clone = this.renderTo.cloneNode(0);
                css(clone, {
                    position: ABSOLUTE,
                    top: '-9999px',
                    display: 'block' // #833
                });
                if (clone.style.setProperty) { // #2631
                    clone.style.setProperty('display', 'block', 'important');
                }
                doc.body.appendChild(clone);
                if (container) {
                    clone.appendChild(container);
                }
            }
        },

        /**
         * Get the containing element, determine the size and create the inner container
         * div to hold the chart
         */
        getContainer: function () {
            var chart = this,
                container,
                options = chart.options,
                optionsChart = options.chart,
                chartWidth,
                chartHeight,
                renderTo = chart.renderTo,
                indexAttrName = 'data-highcharts-chart',
                oldChartIndex,
                Ren,
                containerId = 'highcharts-' + idCounter++;

            if (!renderTo) {
                chart.renderTo = renderTo = optionsChart.renderTo;
            }
        
            if (isString(renderTo)) {
                chart.renderTo = renderTo = doc.getElementById(renderTo);
            }

            // Display an error if the renderTo is wrong
            if (!renderTo) {
                error(13, true);
            }

            // If the container already holds a chart, destroy it. The check for hasRendered is there
            // because web pages that are saved to disk from the browser, will preserve the data-highcharts-chart
            // attribute and the SVG contents, but not an interactive chart. So in this case,
            // charts[oldChartIndex] will point to the wrong chart if any (#2609).
            oldChartIndex = pInt(attr(renderTo, indexAttrName));
            if (isNumber(oldChartIndex) && charts[oldChartIndex] && charts[oldChartIndex].hasRendered) {
                charts[oldChartIndex].destroy();
            }

            // Make a reference to the chart from the div
            attr(renderTo, indexAttrName, chart.index);

            // remove previous chart
            renderTo.innerHTML = '';

            // If the container doesn't have an offsetWidth, it has or is a child of a node
            // that has display:none. We need to temporarily move it out to a visible
            // state to determine the size, else the legend and tooltips won't render
            // properly. The allowClone option is used in sparklines as a micro optimization,
            // saving about 1-2 ms each chart.
            if (!optionsChart.skipClone && !renderTo.offsetWidth) {
                chart.cloneRenderTo();
            }

            // get the width and height
            chart.getChartSize();
            chartWidth = chart.chartWidth;
            chartHeight = chart.chartHeight;

            // create the inner container
            chart.container = container = createElement(DIV, {
                    className: PREFIX + 'container' +
                        (optionsChart.className ? ' ' + optionsChart.className : ''),
                    id: containerId
                }, extend({
                    position: RELATIVE,
                    overflow: HIDDEN, // needed for context menu (avoid scrollbars) and
                        // content overflow in IE
                    width: chartWidth + PX,
                    height: chartHeight + PX,
                    textAlign: 'left',
                    lineHeight: 'normal', // #427
                    zIndex: 0, // #1072
                    '-webkit-tap-highlight-color': 'rgba(0,0,0,0)'
                }, optionsChart.style),
                chart.renderToClone || renderTo
            );

            // cache the cursor (#1650)
            chart._cursor = container.style.cursor;

            // Initialize the renderer
            Ren = Highcharts[optionsChart.renderer] || Renderer;
            chart.renderer = new Ren(
                container,
                chartWidth,
                chartHeight,
                optionsChart.style,
                optionsChart.forExport,
                options.exporting && options.exporting.allowHTML
            );

            if (useCanVG) {
                // If we need canvg library, extend and configure the renderer
                // to get the tracker for translating mouse events
                chart.renderer.create(chart, container, chartWidth, chartHeight);
            }
            // Add a reference to the charts index
            chart.renderer.chartIndex = chart.index;
        },

        /**
         * Calculate margins by rendering axis labels in a preliminary position. Title,
         * subtitle and legend have already been rendered at this stage, but will be
         * moved into their final positions
         */
        getMargins: function (skipAxes) {
            var chart = this,
                spacing = chart.spacing,
                margin = chart.margin,
                titleOffset = chart.titleOffset;

            chart.resetMargins();

            // Adjust for title and subtitle
            if (titleOffset && !defined(margin[0])) {
                chart.plotTop = mathMax(chart.plotTop, titleOffset + chart.options.title.margin + spacing[0]);
            }

            // Adjust for legend
            chart.legend.adjustMargins(margin, spacing);

            // adjust for scroller
            if (chart.extraBottomMargin) {
                chart.marginBottom += chart.extraBottomMargin;
            }
            if (chart.extraTopMargin) {
                chart.plotTop += chart.extraTopMargin;
            }
            if (!skipAxes) {
                this.getAxisMargins();
            }
        },

        getAxisMargins: function () {

            var chart = this,
                axisOffset = chart.axisOffset = [0, 0, 0, 0], // top, right, bottom, left
                margin = chart.margin;

            // pre-render axes to get labels offset width
            if (chart.hasCartesianSeries) {
                each(chart.axes, function (axis) {
                    if (axis.visible) {
                        axis.getOffset();
                    }
                });
            }

            // Add the axis offsets
            each(marginNames, function (m, side) {
                if (!defined(margin[side])) {
                    chart[m] += axisOffset[side];
                }
            });

            chart.setChartSize();

        },

        /**
         * Resize the chart to its container if size is not explicitly set
         */
        reflow: function (e) {
            var chart = this,
                optionsChart = chart.options.chart,
                renderTo = chart.renderTo,
                width = optionsChart.width || getStyle(renderTo, 'width'),
                height = optionsChart.height || getStyle(renderTo, 'height'),
                target = e ? e.target : win;

            // Width and height checks for display:none. Target is doc in IE8 and Opera,
            // win in Firefox, Chrome and IE9.
            if (!chart.hasUserSize && !chart.isPrinting && width && height && (target === win || target === doc)) { // #1093
                if (width !== chart.containerWidth || height !== chart.containerHeight) {
                    clearTimeout(chart.reflowTimeout);
                    // When called from window.resize, e is set, else it's called directly (#2224)
                    chart.reflowTimeout = syncTimeout(function () {
                        if (chart.container) { // It may have been destroyed in the meantime (#1257)
                            chart.setSize(width, height, false);
                            chart.hasUserSize = null;
                        }
                    }, e ? 100 : 0);
                }
                chart.containerWidth = width;
                chart.containerHeight = height;
            }
        },

        /**
         * Add the event handlers necessary for auto resizing
         */
        initReflow: function () {
            var chart = this,
                reflow = function (e) {
                    chart.reflow(e);
                };


            addEvent(win, 'resize', reflow);
            addEvent(chart, 'destroy', function () {
                removeEvent(win, 'resize', reflow);
            });
        },

        /**
         * Resize the chart to a given width and height
         * @param {Number} width
         * @param {Number} height
         * @param {Object|Boolean} animation
         */
        setSize: function (width, height, animation) {
            var chart = this,
                chartWidth,
                chartHeight,
                renderer = chart.renderer,
                globalAnimation;

            // Handle the isResizing counter
            chart.isResizing += 1;
        
            // set the animation for the current process
            setAnimation(animation, chart);

            chart.oldChartHeight = chart.chartHeight;
            chart.oldChartWidth = chart.chartWidth;
            if (defined(width)) {
                chart.chartWidth = chartWidth = mathMax(0, mathRound(width));
                chart.hasUserSize = !!chartWidth;
            }
            if (defined(height)) {
                chart.chartHeight = chartHeight = mathMax(0, mathRound(height));
            }

            // Resize the container with the global animation applied if enabled (#2503)
            globalAnimation = renderer.globalAnimation;
            (globalAnimation ? animate : css)(chart.container, {
                width: chartWidth + PX,
                height: chartHeight + PX
            }, globalAnimation);

            chart.setChartSize(true);
            renderer.setSize(chartWidth, chartHeight, animation);

            // handle axes
            chart.maxTicks = null;
            each(chart.axes, function (axis) {
                axis.isDirty = true;
                axis.setScale();
            });

            // make sure non-cartesian series are also handled
            each(chart.series, function (serie) {
                serie.isDirty = true;
            });

            chart.isDirtyLegend = true; // force legend redraw
            chart.isDirtyBox = true; // force redraw of plot and chart border

            chart.layOutTitles(); // #2857
            chart.getMargins();

            chart.redraw(animation);


            chart.oldChartHeight = null;
            fireEvent(chart, 'resize');

            // Fire endResize and set isResizing back. If animation is disabled, fire without delay
            syncTimeout(function () {
                if (chart) {
                    fireEvent(chart, 'endResize', null, function () {
                        chart.isResizing -= 1;
                    });
                }
            }, animObject(globalAnimation).duration);
        },

        /**
         * Set the public chart properties. This is done before and after the pre-render
         * to determine margin sizes
         */
        setChartSize: function (skipAxes) {
            var chart = this,
                inverted = chart.inverted,
                renderer = chart.renderer,
                chartWidth = chart.chartWidth,
                chartHeight = chart.chartHeight,
                optionsChart = chart.options.chart,
                spacing = chart.spacing,
                clipOffset = chart.clipOffset,
                clipX,
                clipY,
                plotLeft,
                plotTop,
                plotWidth,
                plotHeight,
                plotBorderWidth;

            chart.plotLeft = plotLeft = mathRound(chart.plotLeft);
            chart.plotTop = plotTop = mathRound(chart.plotTop);
            chart.plotWidth = plotWidth = mathMax(0, mathRound(chartWidth - plotLeft - chart.marginRight));
            chart.plotHeight = plotHeight = mathMax(0, mathRound(chartHeight - plotTop - chart.marginBottom));

            chart.plotSizeX = inverted ? plotHeight : plotWidth;
            chart.plotSizeY = inverted ? plotWidth : plotHeight;

            chart.plotBorderWidth = optionsChart.plotBorderWidth || 0;

            // Set boxes used for alignment
            chart.spacingBox = renderer.spacingBox = {
                x: spacing[3],
                y: spacing[0],
                width: chartWidth - spacing[3] - spacing[1],
                height: chartHeight - spacing[0] - spacing[2]
            };
            chart.plotBox = renderer.plotBox = {
                x: plotLeft,
                y: plotTop,
                width: plotWidth,
                height: plotHeight
            };

            plotBorderWidth = 2 * mathFloor(chart.plotBorderWidth / 2);
            clipX = mathCeil(mathMax(plotBorderWidth, clipOffset[3]) / 2);
            clipY = mathCeil(mathMax(plotBorderWidth, clipOffset[0]) / 2);
            chart.clipBox = {
                x: clipX,
                y: clipY,
                width: mathFloor(chart.plotSizeX - mathMax(plotBorderWidth, clipOffset[1]) / 2 - clipX),
                height: mathMax(0, mathFloor(chart.plotSizeY - mathMax(plotBorderWidth, clipOffset[2]) / 2 - clipY))
            };

            if (!skipAxes) {
                each(chart.axes, function (axis) {
                    axis.setAxisSize();
                    axis.setAxisTranslation();
                });
            }
        },

        /**
         * Initial margins before auto size margins are applied
         */
        resetMargins: function () {
            var chart = this;

            each(marginNames, function (m, side) {
                chart[m] = pick(chart.margin[side], chart.spacing[side]);
            });
            chart.axisOffset = [0, 0, 0, 0]; // top, right, bottom, left
            chart.clipOffset = [0, 0, 0, 0];
        },

        /**
         * Draw the borders and backgrounds for chart and plot area
         */
        drawChartBox: function () {
            var chart = this,
                optionsChart = chart.options.chart,
                renderer = chart.renderer,
                chartWidth = chart.chartWidth,
                chartHeight = chart.chartHeight,
                chartBackground = chart.chartBackground,
                plotBackground = chart.plotBackground,
                plotBorder = chart.plotBorder,
                plotBGImage = chart.plotBGImage,
                chartBorderWidth = optionsChart.borderWidth || 0,
                chartBackgroundColor = optionsChart.backgroundColor,
                plotBackgroundColor = optionsChart.plotBackgroundColor,
                plotBackgroundImage = optionsChart.plotBackgroundImage,
                plotBorderWidth = optionsChart.plotBorderWidth || 0,
                mgn,
                bgAttr,
                plotLeft = chart.plotLeft,
                plotTop = chart.plotTop,
                plotWidth = chart.plotWidth,
                plotHeight = chart.plotHeight,
                plotBox = chart.plotBox,
                clipRect = chart.clipRect,
                clipBox = chart.clipBox;

            // Chart area
            mgn = chartBorderWidth + (optionsChart.shadow ? 8 : 0);

            if (chartBorderWidth || chartBackgroundColor) {
                if (!chartBackground) {

                    bgAttr = {
                        fill: chartBackgroundColor || NONE
                    };
                    if (chartBorderWidth) { // #980
                        bgAttr.stroke = optionsChart.borderColor;
                        bgAttr['stroke-width'] = chartBorderWidth;
                    }
                    chart.chartBackground = renderer.rect(mgn / 2, mgn / 2, chartWidth - mgn, chartHeight - mgn,
                            optionsChart.borderRadius, chartBorderWidth)
                        .attr(bgAttr)
                        .addClass(PREFIX + 'background')
                        .add()
                        .shadow(optionsChart.shadow);

                } else { // resize
                    chartBackground.animate(
                        chartBackground.crisp({ width: chartWidth - mgn, height: chartHeight - mgn })
                    );
                }
            }


            // Plot background
            if (plotBackgroundColor) {
                if (!plotBackground) {
                    chart.plotBackground = renderer.rect(plotLeft, plotTop, plotWidth, plotHeight, 0)
                        .attr({
                            fill: plotBackgroundColor
                        })
                        .add()
                        .shadow(optionsChart.plotShadow);
                } else {
                    plotBackground.animate(plotBox);
                }
            }
            if (plotBackgroundImage) {
                if (!plotBGImage) {
                    chart.plotBGImage = renderer.image(plotBackgroundImage, plotLeft, plotTop, plotWidth, plotHeight)
                        .add();
                } else {
                    plotBGImage.animate(plotBox);
                }
            }

            // Plot clip
            if (!clipRect) {
                chart.clipRect = renderer.clipRect(clipBox);
            } else {
                clipRect.animate({
                    width: clipBox.width,
                    height: clipBox.height
                });
            }

            // Plot area border
            if (plotBorderWidth) {
                if (!plotBorder) {
                    chart.plotBorder = renderer.rect(plotLeft, plotTop, plotWidth, plotHeight, 0, -plotBorderWidth)
                        .attr({
                            stroke: optionsChart.plotBorderColor,
                            'stroke-width': plotBorderWidth,
                            fill: NONE,
                            zIndex: 1
                        })
                        .add();
                } else {
                    plotBorder.strokeWidth = -plotBorderWidth;
                    plotBorder.animate(
                        plotBorder.crisp({ x: plotLeft, y: plotTop, width: plotWidth, height: plotHeight }) //#3282 plotBorder should be negative
                    );
                }
            }

            // reset
            chart.isDirtyBox = false;
        },

        /**
         * Detect whether a certain chart property is needed based on inspecting its options
         * and series. This mainly applies to the chart.invert property, and in extensions to
         * the chart.angular and chart.polar properties.
         */
        propFromSeries: function () {
            var chart = this,
                optionsChart = chart.options.chart,
                klass,
                seriesOptions = chart.options.series,
                i,
                value;


            each(['inverted', 'angular', 'polar'], function (key) {

                // The default series type's class
                klass = seriesTypes[optionsChart.type || optionsChart.defaultSeriesType];

                // Get the value from available chart-wide properties
                value = (
                    chart[key] || // 1. it is set before
                    optionsChart[key] || // 2. it is set in the options
                    (klass && klass.prototype[key]) // 3. it's default series class requires it
                );

                // 4. Check if any the chart's series require it
                i = seriesOptions && seriesOptions.length;
                while (!value && i--) {
                    klass = seriesTypes[seriesOptions[i].type];
                    if (klass && klass.prototype[key]) {
                        value = true;
                    }
                }

                // Set the chart property
                chart[key] = value;
            });

        },

        /**
         * Link two or more series together. This is done initially from Chart.render,
         * and after Chart.addSeries and Series.remove.
         */
        linkSeries: function () {
            var chart = this,
                chartSeries = chart.series;

            // Reset links
            each(chartSeries, function (series) {
                series.linkedSeries.length = 0;
            });

            // Apply new links
            each(chartSeries, function (series) {
                var linkedTo = series.options.linkedTo;
                if (isString(linkedTo)) {
                    if (linkedTo === ':previous') {
                        linkedTo = chart.series[series.index - 1];
                    } else {
                        linkedTo = chart.get(linkedTo);
                    }
                    if (linkedTo) {
                        linkedTo.linkedSeries.push(series);
                        series.linkedParent = linkedTo;
                        series.visible = pick(series.options.visible, linkedTo.options.visible, series.visible); // #3879
                    }
                }
            });
        },

        /**
         * Render series for the chart
         */
        renderSeries: function () {
            each(this.series, function (serie) {
                serie.translate();
                serie.render();
            });
        },

        /**
         * Render labels for the chart
         */
        renderLabels: function () {
            var chart = this,
                labels = chart.options.labels;
            if (labels.items) {
                each(labels.items, function (label) {
                    var style = extend(labels.style, label.style),
                        x = pInt(style.left) + chart.plotLeft,
                        y = pInt(style.top) + chart.plotTop + 12;

                    // delete to prevent rewriting in IE
                    delete style.left;
                    delete style.top;

                    chart.renderer.text(
                        label.html,
                        x,
                        y
                    )
                    .attr({ zIndex: 2 })
                    .css(style)
                    .add();

                });
            }
        },

        /**
         * Render all graphics for the chart
         */
        render: function () {
            var chart = this,
                axes = chart.axes,
                renderer = chart.renderer,
                options = chart.options,
                tempWidth,
                tempHeight,
                redoHorizontal,
                redoVertical;

            // Title
            chart.setTitle();


            // Legend
            chart.legend = new Legend(chart, options.legend);

            // Get stacks
            if (chart.getStacks) {
                chart.getStacks();
            }

            // Get chart margins
            chart.getMargins(true);
            chart.setChartSize();

            // Record preliminary dimensions for later comparison
            tempWidth = chart.plotWidth;
            tempHeight = chart.plotHeight = chart.plotHeight - 21; // 21 is the most common correction for X axis labels

            // Get margins by pre-rendering axes
            each(axes, function (axis) {
                axis.setScale();
            });
            chart.getAxisMargins();

            // If the plot area size has changed significantly, calculate tick positions again
            redoHorizontal = tempWidth / chart.plotWidth > 1.1;
            redoVertical = tempHeight / chart.plotHeight > 1.05; // Height is more sensitive

            if (redoHorizontal || redoVertical) {

                chart.maxTicks = null; // reset for second pass
                each(axes, function (axis) {
                    if ((axis.horiz && redoHorizontal) || (!axis.horiz && redoVertical)) {
                        axis.setTickInterval(true); // update to reflect the new margins
                    }
                });
                chart.getMargins(); // second pass to check for new labels
            }

            // Draw the borders and backgrounds
            chart.drawChartBox();


            // Axes
            if (chart.hasCartesianSeries) {
                each(axes, function (axis) {
                    if (axis.visible) {
                        axis.render();
                    }
                });
            }

            // The series
            if (!chart.seriesGroup) {
                chart.seriesGroup = renderer.g('series-group')
                    .attr({ zIndex: 3 })
                    .add();
            }
            chart.renderSeries();

            // Labels
            chart.renderLabels();

            // Credits
            chart.showCredits(options.credits);

            // Set flag
            chart.hasRendered = true;

        },

        /**
         * Show chart credits based on config options
         */
        showCredits: function (credits) {
            if (credits.enabled && !this.credits) {
                this.credits = this.renderer.text(
                    credits.text,
                    0,
                    0
                )
                .on('click', function () {
                    if (credits.href) {
                        win.location.href = credits.href;
                    }
                })
                .attr({
                    align: credits.position.align,
                    zIndex: 8
                })
                .css(credits.style)
                .add()
                .align(credits.position);
            }
        },

        /**
         * Clean up memory usage
         */
        destroy: function () {
            var chart = this,
                axes = chart.axes,
                series = chart.series,
                container = chart.container,
                i,
                parentNode = container && container.parentNode;

            // fire the chart.destoy event
            fireEvent(chart, 'destroy');

            // Delete the chart from charts lookup array
            charts[chart.index] = UNDEFINED;
            chartCount--;
            chart.renderTo.removeAttribute('data-highcharts-chart');

            // remove events
            removeEvent(chart);

            // ==== Destroy collections:
            // Destroy axes
            i = axes.length;
            while (i--) {
                axes[i] = axes[i].destroy();
            }

            // Destroy each series
            i = series.length;
            while (i--) {
                series[i] = series[i].destroy();
            }

            // ==== Destroy chart properties:
            each(['title', 'subtitle', 'chartBackground', 'plotBackground', 'plotBGImage',
                    'plotBorder', 'seriesGroup', 'clipRect', 'credits', 'pointer', 'scroller',
                    'rangeSelector', 'legend', 'resetZoomButton', 'tooltip', 'renderer'], function (name) {
                var prop = chart[name];

                if (prop && prop.destroy) {
                    chart[name] = prop.destroy();
                }
            });

            // remove container and all SVG
            if (container) { // can break in IE when destroyed before finished loading
                container.innerHTML = '';
                removeEvent(container);
                if (parentNode) {
                    discardElement(container);
                }

            }

            // clean it all up
            for (i in chart) {
                delete chart[i];
            }

        },


        /**
         * VML namespaces can't be added until after complete. Listening
         * for Perini's doScroll hack is not enough.
         */
        isReadyToRender: function () {
            var chart = this;

            // Note: win == win.top is required
            if ((!hasSVG && (win == win.top && doc.readyState !== 'complete')) || (useCanVG && !win.canvg)) { // eslint-disable-line eqeqeq
                if (useCanVG) {
                    // Delay rendering until canvg library is downloaded and ready
                    CanVGController.push(function () {
                        chart.firstRender();
                    }, chart.options.global.canvasToolsURL);
                } else {
                    doc.attachEvent('onreadystatechange', function () {
                        doc.detachEvent('onreadystatechange', chart.firstRender);
                        if (doc.readyState === 'complete') {
                            chart.firstRender();
                        }
                    });
                }
                return false;
            }
            return true;
        },

        /**
         * Prepare for first rendering after all data are loaded
         */
        firstRender: function () {
            var chart = this,
                options = chart.options;

            // Check whether the chart is ready to render
            if (!chart.isReadyToRender()) {
                return;
            }

            // Create the container
            chart.getContainer();

            // Run an early event after the container and renderer are established
            fireEvent(chart, 'init');


            chart.resetMargins();
            chart.setChartSize();

            // Set the common chart properties (mainly invert) from the given series
            chart.propFromSeries();

            // get axes
            chart.getAxes();

            // Initialize the series
            each(options.series || [], function (serieOptions) {
                chart.initSeries(serieOptions);
            });

            chart.linkSeries();

            // Run an event after axes and series are initialized, but before render. At this stage,
            // the series data is indexed and cached in the xData and yData arrays, so we can access
            // those before rendering. Used in Highstock.
            fireEvent(chart, 'beforeRender');

            // depends on inverted and on margins being set
            if (Highcharts.Pointer) {
                chart.pointer = new Pointer(chart, options);
            }

            chart.render();

            // add canvas
            chart.renderer.draw();
        
            // Fire the load event if there are no external images
            if (!chart.renderer.imgCount && chart.onload) {
                chart.onload();
            }

            // If the chart was rendered outside the top container, put it back in (#3679)
            chart.cloneRenderTo(true);

        },

        /** 
         * On chart load
         */
        onload: function () {
            var chart = this;

            // Run callbacks
            each([this.callback].concat(this.callbacks), function (fn) {
                if (fn && chart.index !== undefined) { // Chart destroyed in its own callback (#3600)
                    fn.apply(chart, [chart]);
                }
            });

            fireEvent(chart, 'load');

            // Don't run again
            this.onload = null;
        },

        /**
        * Creates arrays for spacing and margin from given options.
        */
        splashArray: function (target, options) {
            var oVar = options[target],
                tArray = isObject(oVar) ? oVar : [oVar, oVar, oVar, oVar];

            return [pick(options[target + 'Top'], tArray[0]),
                    pick(options[target + 'Right'], tArray[1]),
                    pick(options[target + 'Bottom'], tArray[2]),
                    pick(options[target + 'Left'], tArray[3])];
        }
    }; // end Chart

    var CenteredSeriesMixin = Highcharts.CenteredSeriesMixin = {
        /**
         * Get the center of the pie based on the size and center options relative to the
         * plot area. Borrowed by the polar and gauge series types.
         */
        getCenter: function () {

            var options = this.options,
                chart = this.chart,
                slicingRoom = 2 * (options.slicedOffset || 0),
                handleSlicingRoom,
                plotWidth = chart.plotWidth - 2 * slicingRoom,
                plotHeight = chart.plotHeight - 2 * slicingRoom,
                centerOption = options.center,
                positions = [pick(centerOption[0], '50%'), pick(centerOption[1], '50%'), options.size || '100%', options.innerSize || 0],
                smallestSize = mathMin(plotWidth, plotHeight),
                i,
                value;

            for (i = 0; i < 4; ++i) {
                value = positions[i];
                handleSlicingRoom = i < 2 || (i === 2 && /%$/.test(value));

                // i == 0: centerX, relative to width
                // i == 1: centerY, relative to height
                // i == 2: size, relative to smallestSize
                // i == 3: innerSize, relative to size
                positions[i] = relativeLength(value, [plotWidth, plotHeight, smallestSize, positions[2]][i]) +
                    (handleSlicingRoom ? slicingRoom : 0);

            }
            // innerSize cannot be larger than size (#3632)
            if (positions[3] > positions[2]) {
                positions[3] = positions[2];
            }
            return positions;
        }
    };

    /**
     * The Point object and prototype. Inheritable and used as base for PiePoint
     */
    var Point = function () {};
    Point.prototype = {

        /**
         * Initialize the point
         * @param {Object} series The series object containing this point
         * @param {Object} options The data in either number, array or object format
         */
        init: function (series, options, x) {

            var point = this,
                colors;
            point.series = series;
            point.color = series.color; // #3445
            point.applyOptions(options, x);
            point.pointAttr = {};

            if (series.options.colorByPoint) {
                colors = series.options.colors || series.chart.options.colors;
                point.color = point.color || colors[series.colorCounter++];
                // loop back to zero
                if (series.colorCounter === colors.length) {
                    series.colorCounter = 0;
                }
            }

            series.chart.pointCount++;
            return point;
        },
        /**
         * Apply the options containing the x and y data and possible some extra properties.
         * This is called on point init or from point.update.
         *
         * @param {Object} options
         */
        applyOptions: function (options, x) {
            var point = this,
                series = point.series,
                pointValKey = series.options.pointValKey || series.pointValKey;

            options = Point.prototype.optionsToObject.call(this, options);

            // copy options directly to point
            extend(point, options);
            point.options = point.options ? extend(point.options, options) : options;

            // For higher dimension series types. For instance, for ranges, point.y is mapped to point.low.
            if (pointValKey) {
                point.y = point[pointValKey];
            }
            point.isNull = point.x === null || point.y === null;

            // If no x is set by now, get auto incremented value. All points must have an
            // x value, however the y value can be null to create a gap in the series
            if (point.x === undefined && series) {
                point.x = x === undefined ? series.autoIncrement() : x;
            }

            return point;
        },

        /**
         * Transform number or array configs into objects
         */
        optionsToObject: function (options) {
            var ret = {},
                series = this.series,
                keys = series.options.keys,
                pointArrayMap = keys || series.pointArrayMap || ['y'],
                valueCount = pointArrayMap.length,
                firstItemType,
                i = 0,
                j = 0;

            if (isNumber(options) || options === null) {
                ret[pointArrayMap[0]] = options;

            } else if (isArray(options)) {
                // with leading x value
                if (!keys && options.length > valueCount) {
                    firstItemType = typeof options[0];
                    if (firstItemType === 'string') {
                        ret.name = options[0];
                    } else if (firstItemType === 'number') {
                        ret.x = options[0];
                    }
                    i++;
                }
                while (j < valueCount) {
                    if (!keys || options[i] !== undefined) { // Skip undefined positions for keys
                        ret[pointArrayMap[j]] = options[i];
                    }
                    i++;
                    j++;
                }
            } else if (typeof options === 'object') {
                ret = options;

                // This is the fastest way to detect if there are individual point dataLabels that need
                // to be considered in drawDataLabels. These can only occur in object configs.
                if (options.dataLabels) {
                    series._hasPointLabels = true;
                }

                // Same approach as above for markers
                if (options.marker) {
                    series._hasPointMarkers = true;
                }
            }
            return ret;
        },

        /**
         * Destroy a point to clear memory. Its reference still stays in series.data.
         */
        destroy: function () {
            var point = this,
                series = point.series,
                chart = series.chart,
                hoverPoints = chart.hoverPoints,
                prop;

            chart.pointCount--;

            if (hoverPoints) {
                point.setState();
                erase(hoverPoints, point);
                if (!hoverPoints.length) {
                    chart.hoverPoints = null;
                }

            }
            if (point === chart.hoverPoint) {
                point.onMouseOut();
            }

            // remove all events
            if (point.graphic || point.dataLabel) { // removeEvent and destroyElements are performance expensive
                removeEvent(point);
                point.destroyElements();
            }

            if (point.legendItem) { // pies have legend items
                chart.legend.destroyItem(point);
            }

            for (prop in point) {
                point[prop] = null;
            }


        },

        /**
         * Destroy SVG elements associated with the point
         */
        destroyElements: function () {
            var point = this,
                props = ['graphic', 'dataLabel', 'dataLabelUpper', 'connector', 'shadowGroup'],
                prop,
                i = 6;
            while (i--) {
                prop = props[i];
                if (point[prop]) {
                    point[prop] = point[prop].destroy();
                }
            }
        },

        /**
         * Return the configuration hash needed for the data label and tooltip formatters
         */
        getLabelConfig: function () {
            return {
                x: this.category,
                y: this.y,
                color: this.color,
                key: this.name || this.category,
                series: this.series,
                point: this,
                percentage: this.percentage,
                total: this.total || this.stackTotal
            };
        },

        /**
         * Extendable method for formatting each point's tooltip line
         *
         * @return {String} A string to be concatenated in to the common tooltip text
         */
        tooltipFormatter: function (pointFormat) {

            // Insert options for valueDecimals, valuePrefix, and valueSuffix
            var series = this.series,
                seriesTooltipOptions = series.tooltipOptions,
                valueDecimals = pick(seriesTooltipOptions.valueDecimals, ''),
                valuePrefix = seriesTooltipOptions.valuePrefix || '',
                valueSuffix = seriesTooltipOptions.valueSuffix || '';

            // Loop over the point array map and replace unformatted values with sprintf formatting markup
            each(series.pointArrayMap || ['y'], function (key) {
                key = '{point.' + key; // without the closing bracket
                if (valuePrefix || valueSuffix) {
                    pointFormat = pointFormat.replace(key + '}', valuePrefix + key + '}' + valueSuffix);
                }
                pointFormat = pointFormat.replace(key + '}', key + ':,.' + valueDecimals + 'f}');
            });

            return format(pointFormat, {
                point: this,
                series: this.series
            });
        },

        /**
         * Fire an event on the Point object.
         * @param {String} eventType
         * @param {Object} eventArgs Additional event arguments
         * @param {Function} defaultFunction Default event handler
         */
        firePointEvent: function (eventType, eventArgs, defaultFunction) {
            var point = this,
                series = this.series,
                seriesOptions = series.options;

            // load event handlers on demand to save time on mouseover/out
            if (seriesOptions.point.events[eventType] || (point.options && point.options.events && point.options.events[eventType])) {
                this.importEvents();
            }

            // add default handler if in selection mode
            if (eventType === 'click' && seriesOptions.allowPointSelect) {
                defaultFunction = function (event) {
                    // Control key is for Windows, meta (= Cmd key) for Mac, Shift for Opera
                    if (point.select) { // Could be destroyed by prior event handlers (#2911)
                        point.select(null, event.ctrlKey || event.metaKey || event.shiftKey);
                    }
                };
            }

            fireEvent(this, eventType, eventArgs, defaultFunction);
        },
        visible: true
    };/**
     * @classDescription The base function which all other series types inherit from. The data in the series is stored
     * in various arrays.
     *
     * - First, series.options.data contains all the original config options for
     * each point whether added by options or methods like series.addPoint.
     * - Next, series.data contains those values converted to points, but in case the series data length
     * exceeds the cropThreshold, or if the data is grouped, series.data doesn't contain all the points. It
     * only contains the points that have been created on demand.
     * - Then there's series.points that contains all currently visible point objects. In case of cropping,
     * the cropped-away points are not part of this array. The series.points array starts at series.cropStart
     * compared to series.data and series.options.data. If however the series data is grouped, these can't
     * be correlated one to one.
     * - series.xData and series.processedXData contain clean x values, equivalent to series.data and series.points.
     * - series.yData and series.processedYData contain clean x values, equivalent to series.data and series.points.
     *
     * @param {Object} chart
     * @param {Object} options
     */
    var Series = Highcharts.Series = function () {};

    Series.prototype = {

        isCartesian: true,
        type: 'line',
        pointClass: Point,
        sorted: true, // requires the data to be sorted
        requireSorting: true,
        pointAttrToOptions: { // mapping between SVG attributes and the corresponding options
            stroke: 'lineColor',
            'stroke-width': 'lineWidth',
            fill: 'fillColor',
            r: 'radius'
        },
        directTouch: false,
        axisTypes: ['xAxis', 'yAxis'],
        colorCounter: 0,
        parallelArrays: ['x', 'y'], // each point's x and y values are stored in this.xData and this.yData
        init: function (chart, options) {
            var series = this,
                eventType,
                events,
                chartSeries = chart.series,
                sortByIndex = function (a, b) {
                    return pick(a.options.index, a._i) - pick(b.options.index, b._i);
                };

            series.chart = chart;
            series.options = options = series.setOptions(options); // merge with plotOptions
            series.linkedSeries = [];

            // bind the axes
            series.bindAxes();

            // set some variables
            extend(series, {
                name: options.name,
                state: NORMAL_STATE,
                pointAttr: {},
                visible: options.visible !== false, // true by default
                selected: options.selected === true // false by default
            });

            // special
            if (useCanVG) {
                options.animation = false;
            }

            // register event listeners
            events = options.events;
            for (eventType in events) {
                addEvent(series, eventType, events[eventType]);
            }
            if (
                (events && events.click) ||
                (options.point && options.point.events && options.point.events.click) ||
                options.allowPointSelect
            ) {
                chart.runTrackerClick = true;
            }

            series.getColor();
            series.getSymbol();

            // Set the data
            each(series.parallelArrays, function (key) {
                series[key + 'Data'] = [];
            });
            series.setData(options.data, false);

            // Mark cartesian
            if (series.isCartesian) {
                chart.hasCartesianSeries = true;
            }

            // Register it in the chart
            chartSeries.push(series);
            series._i = chartSeries.length - 1;

            // Sort series according to index option (#248, #1123, #2456)
            stableSort(chartSeries, sortByIndex);
            if (this.yAxis) {
                stableSort(this.yAxis.series, sortByIndex);
            }

            each(chartSeries, function (series, i) {
                series.index = i;
                series.name = series.name || 'Series ' + (i + 1);
            });

        },

        /**
         * Set the xAxis and yAxis properties of cartesian series, and register the series
         * in the axis.series array
         */
        bindAxes: function () {
            var series = this,
                seriesOptions = series.options,
                chart = series.chart,
                axisOptions;

            each(series.axisTypes || [], function (AXIS) { // repeat for xAxis and yAxis

                each(chart[AXIS], function (axis) { // loop through the chart's axis objects
                    axisOptions = axis.options;

                    // apply if the series xAxis or yAxis option mathches the number of the
                    // axis, or if undefined, use the first axis
                    if ((seriesOptions[AXIS] === axisOptions.index) ||
                            (seriesOptions[AXIS] !== UNDEFINED && seriesOptions[AXIS] === axisOptions.id) ||
                            (seriesOptions[AXIS] === UNDEFINED && axisOptions.index === 0)) {

                        // register this series in the axis.series lookup
                        axis.series.push(series);

                        // set this series.xAxis or series.yAxis reference
                        series[AXIS] = axis;

                        // mark dirty for redraw
                        axis.isDirty = true;
                    }
                });

                // The series needs an X and an Y axis
                if (!series[AXIS] && series.optionalAxis !== AXIS) {
                    error(18, true);
                }

            });
        },

        /**
         * For simple series types like line and column, the data values are held in arrays like
         * xData and yData for quick lookup to find extremes and more. For multidimensional series
         * like bubble and map, this can be extended with arrays like zData and valueData by
         * adding to the series.parallelArrays array.
         */
        updateParallelArrays: function (point, i) {
            var series = point.series,
                args = arguments,
                fn = isNumber(i) ?
                    // Insert the value in the given position
                    function (key) {
                        var val = key === 'y' && series.toYData ? series.toYData(point) : point[key];
                        series[key + 'Data'][i] = val;
                    } :
                    // Apply the method specified in i with the following arguments as arguments
                    function (key) {
                        Array.prototype[i].apply(series[key + 'Data'], Array.prototype.slice.call(args, 2));
                    };

            each(series.parallelArrays, fn);
        },

        /**
         * Return an auto incremented x value based on the pointStart and pointInterval options.
         * This is only used if an x value is not given for the point that calls autoIncrement.
         */
        autoIncrement: function () {

            var options = this.options,
                xIncrement = this.xIncrement,
                date,
                pointInterval,
                pointIntervalUnit = options.pointIntervalUnit;

            xIncrement = pick(xIncrement, options.pointStart, 0);

            this.pointInterval = pointInterval = pick(this.pointInterval, options.pointInterval, 1);

            // Added code for pointInterval strings
            if (pointIntervalUnit) {
                date = new Date(xIncrement);

                if (pointIntervalUnit === 'day') {
                    date = +date[setDate](date[getDate]() + pointInterval);
                } else if (pointIntervalUnit === 'month') {
                    date = +date[setMonth](date[getMonth]() + pointInterval);
                } else if (pointIntervalUnit === 'year') {
                    date = +date[setFullYear](date[getFullYear]() + pointInterval);
                }
                pointInterval = date - xIncrement;
            }

            this.xIncrement = xIncrement + pointInterval;
            return xIncrement;
        },
    
        /**
         * Set the series options by merging from the options tree
         * @param {Object} itemOptions
         */
        setOptions: function (itemOptions) {
            var chart = this.chart,
                chartOptions = chart.options,
                plotOptions = chartOptions.plotOptions,
                userOptions = chart.userOptions || {},
                userPlotOptions = userOptions.plotOptions || {},
                typeOptions = plotOptions[this.type],
                options,
                zones;

            this.userOptions = itemOptions;

            // General series options take precedence over type options because otherwise, default
            // type options like column.animation would be overwritten by the general option.
            // But issues have been raised here (#3881), and the solution may be to distinguish
            // between default option and userOptions like in the tooltip below.
            options = merge(
                typeOptions,
                plotOptions.series,
                itemOptions
            );

            // The tooltip options are merged between global and series specific options
            this.tooltipOptions = merge(
                defaultOptions.tooltip,
                defaultOptions.plotOptions[this.type].tooltip,
                userOptions.tooltip,
                userPlotOptions.series && userPlotOptions.series.tooltip,
                userPlotOptions[this.type] && userPlotOptions[this.type].tooltip,
                itemOptions.tooltip
            );

            // Delete marker object if not allowed (#1125)
            if (typeOptions.marker === null) {
                delete options.marker;
            }

            // Handle color zones
            this.zoneAxis = options.zoneAxis;
            zones = this.zones = (options.zones || []).slice();
            if ((options.negativeColor || options.negativeFillColor) && !options.zones) {
                zones.push({
                    value: options[this.zoneAxis + 'Threshold'] || options.threshold || 0,
                    color: options.negativeColor,
                    fillColor: options.negativeFillColor
                });
            }
            if (zones.length) { // Push one extra zone for the rest
                if (defined(zones[zones.length - 1].value)) {
                    zones.push({
                        color: this.color,
                        fillColor: this.fillColor
                    });
                }
            }
            return options;
        },

        getCyclic: function (prop, value, defaults) {
            var i,
                userOptions = this.userOptions,
                indexName = '_' + prop + 'Index',
                counterName = prop + 'Counter';

            if (!value) {
                if (defined(userOptions[indexName])) { // after Series.update()
                    i = userOptions[indexName];
                } else {
                    userOptions[indexName] = i = this.chart[counterName] % defaults.length;
                    this.chart[counterName] += 1;
                }
                value = defaults[i];
            }
            this[prop] = value;
        },

        /**
         * Get the series' color
         */
        getColor: function () {
            if (this.options.colorByPoint) {
                this.options.color = null; // #4359, selected slice got series.color even when colorByPoint was set.
            } else {
                this.getCyclic('color', this.options.color || defaultPlotOptions[this.type].color, this.chart.options.colors);
            }
        },
        /**
         * Get the series' symbol
         */
        getSymbol: function () {
            var seriesMarkerOption = this.options.marker;

            this.getCyclic('symbol', seriesMarkerOption.symbol, this.chart.options.symbols);

            // don't substract radius in image symbols (#604)
            if (/^url/.test(this.symbol)) {
                seriesMarkerOption.radius = 0;
            }
        },

        drawLegendSymbol: LegendSymbolMixin.drawLineMarker,

        /**
         * Replace the series data with a new set of data
         * @param {Object} data
         * @param {Object} redraw
         */
        setData: function (data, redraw, animation, updatePoints) {
            var series = this,
                oldData = series.points,
                oldDataLength = (oldData && oldData.length) || 0,
                dataLength,
                options = series.options,
                chart = series.chart,
                firstPoint = null,
                xAxis = series.xAxis,
                hasCategories = xAxis && !!xAxis.categories,
                i,
                turboThreshold = options.turboThreshold,
                pt,
                xData = this.xData,
                yData = this.yData,
                pointArrayMap = series.pointArrayMap,
                valueCount = pointArrayMap && pointArrayMap.length;

            data = data || [];
            dataLength = data.length;
            redraw = pick(redraw, true);

            // If the point count is the same as is was, just run Point.update which is
            // cheaper, allows animation, and keeps references to points.
            if (updatePoints !== false && dataLength && oldDataLength === dataLength && !series.cropped && !series.hasGroupedData && series.visible) {
                each(data, function (point, i) {
                    // .update doesn't exist on a linked, hidden series (#3709)
                    if (oldData[i].update && point !== options.data[i]) {
                        oldData[i].update(point, false, null, false);
                    }
                });

            } else {

                // Reset properties
                series.xIncrement = null;

                series.colorCounter = 0; // for series with colorByPoint (#1547)

                // Update parallel arrays
                each(this.parallelArrays, function (key) {
                    series[key + 'Data'].length = 0;
                });

                // In turbo mode, only one- or twodimensional arrays of numbers are allowed. The
                // first value is tested, and we assume that all the rest are defined the same
                // way. Although the 'for' loops are similar, they are repeated inside each
                // if-else conditional for max performance.
                if (turboThreshold && dataLength > turboThreshold) {

                    // find the first non-null point
                    i = 0;
                    while (firstPoint === null && i < dataLength) {
                        firstPoint = data[i];
                        i++;
                    }


                    if (isNumber(firstPoint)) { // assume all points are numbers
                        var x = pick(options.pointStart, 0),
                            pointInterval = pick(options.pointInterval, 1);

                        for (i = 0; i < dataLength; i++) {
                            xData[i] = x;
                            yData[i] = data[i];
                            x += pointInterval;
                        }
                        series.xIncrement = x;
                    } else if (isArray(firstPoint)) { // assume all points are arrays
                        if (valueCount) { // [x, low, high] or [x, o, h, l, c]
                            for (i = 0; i < dataLength; i++) {
                                pt = data[i];
                                xData[i] = pt[0];
                                yData[i] = pt.slice(1, valueCount + 1);
                            }
                        } else { // [x, y]
                            for (i = 0; i < dataLength; i++) {
                                pt = data[i];
                                xData[i] = pt[0];
                                yData[i] = pt[1];
                            }
                        }
                    } else {
                        error(12); // Highcharts expects configs to be numbers or arrays in turbo mode
                    }
                } else {
                    for (i = 0; i < dataLength; i++) {
                        if (data[i] !== UNDEFINED) { // stray commas in oldIE
                            pt = { series: series };
                            series.pointClass.prototype.applyOptions.apply(pt, [data[i]]);
                            series.updateParallelArrays(pt, i);
                            if (hasCategories && defined(pt.name)) { // #4401
                                xAxis.names[pt.x] = pt.name; // #2046
                            }
                        }
                    }
                }

                // Forgetting to cast strings to numbers is a common caveat when handling CSV or JSON
                if (isString(yData[0])) {
                    error(14, true);
                }

                series.data = [];
                series.options.data = series.userOptions.data = data;

                // destroy old points
                i = oldDataLength;
                while (i--) {
                    if (oldData[i] && oldData[i].destroy) {
                        oldData[i].destroy();
                    }
                }

                // reset minRange (#878)
                if (xAxis) {
                    xAxis.minRange = xAxis.userMinRange;
                }

                // redraw
                series.isDirty = series.isDirtyData = chart.isDirtyBox = true;
                animation = false;
            }

            // Typically for pie series, points need to be processed and generated
            // prior to rendering the legend
            if (options.legendType === 'point') {
                this.processData();
                this.generatePoints();
            }

            if (redraw) {
                chart.redraw(animation);
            }
        },

        /**
         * Process the data by cropping away unused data points if the series is longer
         * than the crop threshold. This saves computing time for lage series.
         */
        processData: function (force) {
            var series = this,
                processedXData = series.xData, // copied during slice operation below
                processedYData = series.yData,
                dataLength = processedXData.length,
                croppedData,
                cropStart = 0,
                cropped,
                distance,
                closestPointRange,
                xAxis = series.xAxis,
                i, // loop variable
                options = series.options,
                cropThreshold = options.cropThreshold,
                getExtremesFromAll = series.getExtremesFromAll || options.getExtremesFromAll, // #4599
                isCartesian = series.isCartesian,
                xExtremes,
                val2lin = xAxis && xAxis.val2lin,
                isLog = xAxis && xAxis.isLog,
                min,
                max;

            // If the series data or axes haven't changed, don't go through this. Return false to pass
            // the message on to override methods like in data grouping.
            if (isCartesian && !series.isDirty && !xAxis.isDirty && !series.yAxis.isDirty && !force) {
                return false;
            }

            if (xAxis) {
                xExtremes = xAxis.getExtremes(); // corrected for log axis (#3053)
                min = xExtremes.min;
                max = xExtremes.max;
            }

            // optionally filter out points outside the plot area
            if (isCartesian && series.sorted && !getExtremesFromAll && (!cropThreshold || dataLength > cropThreshold || series.forceCrop)) {

                // it's outside current extremes
                if (processedXData[dataLength - 1] < min || processedXData[0] > max) {
                    processedXData = [];
                    processedYData = [];

                // only crop if it's actually spilling out
                } else if (processedXData[0] < min || processedXData[dataLength - 1] > max) {
                    croppedData = this.cropData(series.xData, series.yData, min, max);
                    processedXData = croppedData.xData;
                    processedYData = croppedData.yData;
                    cropStart = croppedData.start;
                    cropped = true;
                }
            }


            // Find the closest distance between processed points
            i = processedXData.length || 1;
            while (--i) {
                distance = isLog ?
                    val2lin(processedXData[i]) - val2lin(processedXData[i - 1]) :
                    processedXData[i] - processedXData[i - 1];

                if (distance > 0 && (closestPointRange === UNDEFINED || distance < closestPointRange)) {
                    closestPointRange = distance;

                // Unsorted data is not supported by the line tooltip, as well as data grouping and
                // navigation in Stock charts (#725) and width calculation of columns (#1900)
                } else if (distance < 0 && series.requireSorting) {
                    error(15);
                }
            }

            // Record the properties
            series.cropped = cropped; // undefined or true
            series.cropStart = cropStart;
            series.processedXData = processedXData;
            series.processedYData = processedYData;

            series.closestPointRange = closestPointRange;

        },

        /**
         * Iterate over xData and crop values between min and max. Returns object containing crop start/end
         * cropped xData with corresponding part of yData, dataMin and dataMax within the cropped range
         */
        cropData: function (xData, yData, min, max) {
            var dataLength = xData.length,
                cropStart = 0,
                cropEnd = dataLength,
                cropShoulder = pick(this.cropShoulder, 1), // line-type series need one point outside
                i,
                j;

            // iterate up to find slice start
            for (i = 0; i < dataLength; i++) {
                if (xData[i] >= min) {
                    cropStart = mathMax(0, i - cropShoulder);
                    break;
                }
            }

            // proceed to find slice end
            for (j = i; j < dataLength; j++) {
                if (xData[j] > max) {
                    cropEnd = j + cropShoulder;
                    break;
                }
            }

            return {
                xData: xData.slice(cropStart, cropEnd),
                yData: yData.slice(cropStart, cropEnd),
                start: cropStart,
                end: cropEnd
            };
        },


        /**
         * Generate the data point after the data has been processed by cropping away
         * unused points and optionally grouped in Highcharts Stock.
         */
        generatePoints: function () {
            var series = this,
                options = series.options,
                dataOptions = options.data,
                data = series.data,
                dataLength,
                processedXData = series.processedXData,
                processedYData = series.processedYData,
                pointClass = series.pointClass,
                processedDataLength = processedXData.length,
                cropStart = series.cropStart || 0,
                cursor,
                hasGroupedData = series.hasGroupedData,
                point,
                points = [],
                i;

            if (!data && !hasGroupedData) {
                var arr = [];
                arr.length = dataOptions.length;
                data = series.data = arr;
            }

            for (i = 0; i < processedDataLength; i++) {
                cursor = cropStart + i;
                if (!hasGroupedData) {
                    if (data[cursor]) {
                        point = data[cursor];
                    } else if (dataOptions[cursor] !== UNDEFINED) { // #970
                        data[cursor] = point = (new pointClass()).init(series, dataOptions[cursor], processedXData[i]);
                    }
                    points[i] = point;
                } else {
                    // splat the y data in case of ohlc data array
                    points[i] = (new pointClass()).init(series, [processedXData[i]].concat(splat(processedYData[i])));
                    points[i].dataGroup = series.groupMap[i];
                }
                points[i].index = cursor; // For faster access in Point.update
            }

            // Hide cropped-away points - this only runs when the number of points is above cropThreshold, or when
            // swithching view from non-grouped data to grouped data (#637)
            if (data && (processedDataLength !== (dataLength = data.length) || hasGroupedData)) {
                for (i = 0; i < dataLength; i++) {
                    if (i === cropStart && !hasGroupedData) { // when has grouped data, clear all points
                        i += processedDataLength;
                    }
                    if (data[i]) {
                        data[i].destroyElements();
                        data[i].plotX = UNDEFINED; // #1003
                    }
                }
            }

            series.data = data;
            series.points = points;
        },

        /**
         * Calculate Y extremes for visible data
         */
        getExtremes: function (yData) {
            var xAxis = this.xAxis,
                yAxis = this.yAxis,
                xData = this.processedXData,
                yDataLength,
                activeYData = [],
                activeCounter = 0,
                xExtremes = xAxis.getExtremes(), // #2117, need to compensate for log X axis
                xMin = xExtremes.min,
                xMax = xExtremes.max,
                validValue,
                withinRange,
                x,
                y,
                i,
                j;

            yData = yData || this.stackedYData || this.processedYData || [];
            yDataLength = yData.length;

            for (i = 0; i < yDataLength; i++) {

                x = xData[i];
                y = yData[i];

                // For points within the visible range, including the first point outside the
                // visible range, consider y extremes
                validValue = y !== null && y !== UNDEFINED && (!yAxis.isLog || (y.length || y > 0));
                withinRange = this.getExtremesFromAll || this.options.getExtremesFromAll || this.cropped ||
                    ((xData[i + 1] || x) >= xMin &&    (xData[i - 1] || x) <= xMax);

                if (validValue && withinRange) {

                    j = y.length;
                    if (j) { // array, like ohlc or range data
                        while (j--) {
                            if (y[j] !== null) {
                                activeYData[activeCounter++] = y[j];
                            }
                        }
                    } else {
                        activeYData[activeCounter++] = y;
                    }
                }
            }
            this.dataMin = arrayMin(activeYData);
            this.dataMax = arrayMax(activeYData);
        },

        /**
         * Translate data points from raw data values to chart specific positioning data
         * needed later in drawPoints, drawGraph and drawTracker.
         */
        translate: function () {
            if (!this.processedXData) { // hidden series
                this.processData();
            }
            this.generatePoints();
            var series = this,
                options = series.options,
                stacking = options.stacking,
                xAxis = series.xAxis,
                categories = xAxis.categories,
                yAxis = series.yAxis,
                points = series.points,
                dataLength = points.length,
                hasModifyValue = !!series.modifyValue,
                i,
                pointPlacement = options.pointPlacement,
                dynamicallyPlaced = pointPlacement === 'between' || isNumber(pointPlacement),
                threshold = options.threshold,
                stackThreshold = options.startFromThreshold ? threshold : 0,
                plotX,
                plotY,
                lastPlotX,
                stackIndicator,
                closestPointRangePx = Number.MAX_VALUE;

            // Translate each point
            for (i = 0; i < dataLength; i++) {
                var point = points[i],
                    xValue = point.x,
                    yValue = point.y,
                    yBottom = point.low,
                    stack = stacking && yAxis.stacks[(series.negStacks && yValue < (stackThreshold ? 0 : threshold) ? '-' : '') + series.stackKey],
                    pointStack,
                    stackValues;

                // Discard disallowed y values for log axes (#3434)
                if (yAxis.isLog && yValue !== null && yValue <= 0) {
                    point.y = yValue = null;
                    error(10);
                }

                // Get the plotX translation
                point.plotX = plotX = correctFloat( // #5236
                    mathMin(mathMax(-1e5, xAxis.translate(xValue, 0, 0, 0, 1, pointPlacement, this.type === 'flags')), 1e5) // #3923
                );

                // Calculate the bottom y value for stacked series
                if (stacking && series.visible && !point.isNull && stack && stack[xValue]) {
                    stackIndicator = series.getStackIndicator(stackIndicator, xValue, series.index);
                    pointStack = stack[xValue];
                    stackValues = pointStack.points[stackIndicator.key];
                    yBottom = stackValues[0];
                    yValue = stackValues[1];

                    if (yBottom === stackThreshold) {
                        yBottom = pick(threshold, yAxis.min);
                    }
                    if (yAxis.isLog && yBottom <= 0) { // #1200, #1232
                        yBottom = null;
                    }

                    point.total = point.stackTotal = pointStack.total;
                    point.percentage = pointStack.total && (point.y / pointStack.total * 100);
                    point.stackY = yValue;

                    // Place the stack label
                    pointStack.setOffset(series.pointXOffset || 0, series.barW || 0);

                }

                // Set translated yBottom or remove it
                point.yBottom = defined(yBottom) ?
                    yAxis.translate(yBottom, 0, 1, 0, 1) :
                    null;

                // general hook, used for Highstock compare mode
                if (hasModifyValue) {
                    yValue = series.modifyValue(yValue, point);
                }

                // Set the the plotY value, reset it for redraws
                point.plotY = plotY = (typeof yValue === 'number' && yValue !== Infinity) ?
                    mathMin(mathMax(-1e5, yAxis.translate(yValue, 0, 1, 0, 1)), 1e5) : // #3201
                    UNDEFINED;
                point.isInside = plotY !== UNDEFINED && plotY >= 0 && plotY <= yAxis.len && // #3519
                    plotX >= 0 && plotX <= xAxis.len;


                // Set client related positions for mouse tracking
                point.clientX = dynamicallyPlaced ? xAxis.translate(xValue, 0, 0, 0, 1) : plotX; // #1514

                point.negative = point.y < (threshold || 0);

                // some API data
                point.category = categories && categories[point.x] !== UNDEFINED ?
                    categories[point.x] : point.x;

                // Determine auto enabling of markers (#3635, #5099)
                if (!point.isNull) {
                    if (lastPlotX !== undefined) {
                        closestPointRangePx = mathMin(closestPointRangePx, mathAbs(plotX - lastPlotX));
                    }
                    lastPlotX = plotX;
                }

            }
            series.closestPointRangePx = closestPointRangePx;
        },

        /**
         * Return the series points with null points filtered out
         */
        getValidPoints: function (points, insideOnly) {
            var chart = this.chart;
            return grep(points || this.points || [], function isValidPoint(point) { // #3916, #5029
                if (insideOnly && !chart.isInsidePlot(point.plotX, point.plotY, chart.inverted)) { // #5085
                    return false;
                }
                return !point.isNull;
            });
        },

        /**
         * Set the clipping for the series. For animated series it is called twice, first to initiate
         * animating the clip then the second time without the animation to set the final clip.
         */
        setClip: function (animation) {
            var chart = this.chart,
                options = this.options,
                renderer = chart.renderer,
                inverted = chart.inverted,
                seriesClipBox = this.clipBox,
                clipBox = seriesClipBox || chart.clipBox,
                sharedClipKey = this.sharedClipKey || ['_sharedClip', animation && animation.duration, animation && animation.easing, clipBox.height, options.xAxis, options.yAxis].join(','), // #4526
                clipRect = chart[sharedClipKey],
                markerClipRect = chart[sharedClipKey + 'm'];

            // If a clipping rectangle with the same properties is currently present in the chart, use that.
            if (!clipRect) {

                // When animation is set, prepare the initial positions
                if (animation) {
                    clipBox.width = 0;

                    chart[sharedClipKey + 'm'] = markerClipRect = renderer.clipRect(
                        -99, // include the width of the first marker
                        inverted ? -chart.plotLeft : -chart.plotTop,
                        99,
                        inverted ? chart.chartWidth : chart.chartHeight
                    );
                }
                chart[sharedClipKey] = clipRect = renderer.clipRect(clipBox);

            }
            if (animation) {
                clipRect.count += 1;
            }

            if (options.clip !== false) {
                this.group.clip(animation || seriesClipBox ? clipRect : chart.clipRect);
                this.markerGroup.clip(markerClipRect);
                this.sharedClipKey = sharedClipKey;
            }

            // Remove the shared clipping rectangle when all series are shown
            if (!animation) {
                clipRect.count -= 1;
                if (clipRect.count <= 0 && sharedClipKey && chart[sharedClipKey]) {
                    if (!seriesClipBox) {
                        chart[sharedClipKey] = chart[sharedClipKey].destroy();
                    }
                    if (chart[sharedClipKey + 'm']) {
                        chart[sharedClipKey + 'm'] = chart[sharedClipKey + 'm'].destroy();
                    }
                }
            }
        },

        /**
         * Animate in the series
         */
        animate: function (init) {
            var series = this,
                chart = series.chart,
                clipRect,
                animation = series.options.animation,
                sharedClipKey;

            // Animation option is set to true
            if (animation && !isObject(animation)) {
                animation = defaultPlotOptions[series.type].animation;
            }

            // Initialize the animation. Set up the clipping rectangle.
            if (init) {

                series.setClip(animation);

            // Run the animation
            } else {
                sharedClipKey = this.sharedClipKey;
                clipRect = chart[sharedClipKey];
                if (clipRect) {
                    clipRect.animate({
                        width: chart.plotSizeX
                    }, animation);
                }
                if (chart[sharedClipKey + 'm']) {
                    chart[sharedClipKey + 'm'].animate({
                        width: chart.plotSizeX + 99
                    }, animation);
                }

                // Delete this function to allow it only once
                series.animate = null;

            }
        },

        /**
         * This runs after animation to land on the final plot clipping
         */
        afterAnimate: function () {
            this.setClip();
            fireEvent(this, 'afterAnimate');
        },

        /**
         * Draw the markers
         */
        drawPoints: function () {
            var series = this,
                pointAttr,
                points = series.points,
                chart = series.chart,
                plotX,
                plotY,
                i,
                point,
                radius,
                symbol,
                isImage,
                graphic,
                options = series.options,
                seriesMarkerOptions = options.marker,
                seriesPointAttr = series.pointAttr[''],
                pointMarkerOptions,
                hasPointMarker,
                enabled,
                isInside,
                markerGroup = series.markerGroup,
                xAxis = series.xAxis,
                globallyEnabled = pick(
                    seriesMarkerOptions.enabled,
                    xAxis.isRadial,
                    series.closestPointRangePx > 2 * seriesMarkerOptions.radius
                );

            if (seriesMarkerOptions.enabled !== false || series._hasPointMarkers) {

                i = points.length;
                while (i--) {
                    point = points[i];
                    plotX = mathFloor(point.plotX); // #1843
                    plotY = point.plotY;
                    graphic = point.graphic;
                    pointMarkerOptions = point.marker || {};
                    hasPointMarker = !!point.marker;
                    enabled = (globallyEnabled && pointMarkerOptions.enabled === UNDEFINED) || pointMarkerOptions.enabled;
                    isInside = point.isInside;

                    // only draw the point if y is defined
                    if (enabled && isNumber(plotY) && point.y !== null) {

                        // shortcuts
                        pointAttr = point.pointAttr[point.selected ? SELECT_STATE : NORMAL_STATE] || seriesPointAttr;
                        radius = pointAttr.r;
                        symbol = pick(pointMarkerOptions.symbol, series.symbol);
                        isImage = symbol.indexOf('url') === 0;

                        if (graphic) { // update
                            graphic[isInside ? 'show' : 'hide'](true) // Since the marker group isn't clipped, each individual marker must be toggled
                                .attr(pointAttr) // #4759
                                .animate(extend({
                                    x: plotX - radius,
                                    y: plotY - radius
                                }, graphic.symbolName ? { // don't apply to image symbols #507
                                    width: 2 * radius,
                                    height: 2 * radius
                                } : {}));
                        } else if (isInside && (radius > 0 || isImage)) {
                            point.graphic = graphic = chart.renderer.symbol(
                                symbol,
                                plotX - radius,
                                plotY - radius,
                                2 * radius,
                                2 * radius,
                                hasPointMarker ? pointMarkerOptions : seriesMarkerOptions
                            )
                            .attr(pointAttr)
                            .add(markerGroup);
                        }

                    } else if (graphic) {
                        point.graphic = graphic.destroy(); // #1269
                    }
                }
            }

        },

        /**
         * Convert state properties from API naming conventions to SVG attributes
         *
         * @param {Object} options API options object
         * @param {Object} base1 SVG attribute object to inherit from
         * @param {Object} base2 Second level SVG attribute object to inherit from
         */
        convertAttribs: function (options, base1, base2, base3) {
            var conversion = this.pointAttrToOptions,
                attr,
                option,
                obj = {};

            options = options || {};
            base1 = base1 || {};
            base2 = base2 || {};
            base3 = base3 || {};

            for (attr in conversion) {
                option = conversion[attr];
                obj[attr] = pick(options[option], base1[attr], base2[attr], base3[attr]);
            }
            return obj;
        },

        /**
         * Get the state attributes. Each series type has its own set of attributes
         * that are allowed to change on a point's state change. Series wide attributes are stored for
         * all series, and additionally point specific attributes are stored for all
         * points with individual marker options. If such options are not defined for the point,
         * a reference to the series wide attributes is stored in point.pointAttr.
         */
        getAttribs: function () {
            var series = this,
                seriesOptions = series.options,
                normalOptions = defaultPlotOptions[series.type].marker ? seriesOptions.marker : seriesOptions,
                stateOptions = normalOptions.states,
                stateOptionsHover = stateOptions[HOVER_STATE],
                pointStateOptionsHover,
                seriesColor = series.color,
                seriesNegativeColor = series.options.negativeColor,
                normalDefaults = {
                    stroke: seriesColor,
                    fill: seriesColor
                },
                points = series.points || [], // #927
                i,
                j,
                threshold,
                point,
                seriesPointAttr = [],
                pointAttr,
                pointAttrToOptions = series.pointAttrToOptions,
                hasPointSpecificOptions = series.hasPointSpecificOptions,
                defaultLineColor = normalOptions.lineColor,
                defaultFillColor = normalOptions.fillColor,
                turboThreshold = seriesOptions.turboThreshold,
                zones = series.zones,
                zoneAxis = series.zoneAxis || 'y',
                zoneColor, 
                attr,
                key;

            // series type specific modifications
            if (seriesOptions.marker) { // line, spline, area, areaspline, scatter

                // if no hover radius is given, default to normal radius + 2
                stateOptionsHover.radius = stateOptionsHover.radius || normalOptions.radius + stateOptionsHover.radiusPlus;
                stateOptionsHover.lineWidth = stateOptionsHover.lineWidth || normalOptions.lineWidth + stateOptionsHover.lineWidthPlus;

            } else { // column, bar, pie

                // if no hover color is given, brighten the normal color
                stateOptionsHover.color = stateOptionsHover.color ||
                    Color(stateOptionsHover.color || seriesColor)
                        .brighten(stateOptionsHover.brightness).get();

                // if no hover negativeColor is given, brighten the normal negativeColor
                stateOptionsHover.negativeColor = stateOptionsHover.negativeColor ||
                    Color(stateOptionsHover.negativeColor || seriesNegativeColor)
                        .brighten(stateOptionsHover.brightness).get();
            }

            // general point attributes for the series normal state
            seriesPointAttr[NORMAL_STATE] = series.convertAttribs(normalOptions, normalDefaults);

            // HOVER_STATE and SELECT_STATE states inherit from normal state except the default radius
            each([HOVER_STATE, SELECT_STATE], function (state) {
                seriesPointAttr[state] =
                        series.convertAttribs(stateOptions[state], seriesPointAttr[NORMAL_STATE]);
            });

            // set it
            series.pointAttr = seriesPointAttr;


            // Generate the point-specific attribute collections if specific point
            // options are given. If not, create a referance to the series wide point
            // attributes
            i = points.length;
            if (!turboThreshold || i < turboThreshold || hasPointSpecificOptions) {
                while (i--) {
                    point = points[i];
                    normalOptions = (point.options && point.options.marker) || point.options;
                    if (normalOptions && normalOptions.enabled === false) {
                        normalOptions.radius = 0;
                    }

                    zoneColor = null;
                    if (zones.length) {
                        j = 0;
                        threshold = zones[j];
                        while (point[zoneAxis] >= threshold.value) {
                            threshold = zones[++j];
                        }

                        point.color = point.fillColor = zoneColor = pick(threshold.color, series.color); // #3636, #4267, #4430 - inherit color from series, when color is undefined

                    }

                    hasPointSpecificOptions = seriesOptions.colorByPoint || point.color; // #868

                    // check if the point has specific visual options
                    if (point.options) {
                        for (key in pointAttrToOptions) {
                            if (defined(normalOptions[pointAttrToOptions[key]])) {
                                hasPointSpecificOptions = true;
                            }
                        }
                    }

                    // a specific marker config object is defined for the individual point:
                    // create it's own attribute collection
                    if (hasPointSpecificOptions) {
                        normalOptions = normalOptions || {};
                        pointAttr = [];
                        stateOptions = normalOptions.states || {}; // reassign for individual point
                        pointStateOptionsHover = stateOptions[HOVER_STATE] = stateOptions[HOVER_STATE] || {};

                        // Handle colors for column and pies
                        if (!seriesOptions.marker || (point.negative && !pointStateOptionsHover.fillColor && !stateOptionsHover.fillColor)) { // column, bar, point or negative threshold for series with markers (#3636)
                            // If no hover color is given, brighten the normal color. #1619, #2579
                            pointStateOptionsHover[series.pointAttrToOptions.fill] = pointStateOptionsHover.color || (!point.options.color && stateOptionsHover[(point.negative && seriesNegativeColor ? 'negativeColor' : 'color')]) ||
                                Color(point.color)
                                    .brighten(pointStateOptionsHover.brightness || stateOptionsHover.brightness)
                                    .get();
                        }

                        // normal point state inherits series wide normal state
                        attr = { color: point.color }; // #868
                        if (!defaultFillColor) { // Individual point color or negative color markers (#2219)
                            attr.fillColor = point.color;
                        }
                        if (!defaultLineColor) {
                            attr.lineColor = point.color; // Bubbles take point color, line markers use white
                        }
                        // Color is explicitly set to null or undefined (#1288, #4068)
                        if (normalOptions.hasOwnProperty('color') && !normalOptions.color) {
                            delete normalOptions.color;
                        }

                        // When zone is set, but series.states.hover.color is not set, apply zone color on hover, #4670: 
                        if (zoneColor && !stateOptionsHover.fillColor) {
                            pointStateOptionsHover.fillColor = zoneColor;
                        }

                        pointAttr[NORMAL_STATE] = series.convertAttribs(extend(attr, normalOptions), seriesPointAttr[NORMAL_STATE]);

                        // inherit from point normal and series hover
                        pointAttr[HOVER_STATE] = series.convertAttribs(
                            stateOptions[HOVER_STATE],
                            seriesPointAttr[HOVER_STATE],
                            pointAttr[NORMAL_STATE]
                        );

                        // inherit from point normal and series hover
                        pointAttr[SELECT_STATE] = series.convertAttribs(
                            stateOptions[SELECT_STATE],
                            seriesPointAttr[SELECT_STATE],
                            pointAttr[NORMAL_STATE]
                        );


                    // no marker config object is created: copy a reference to the series-wide
                    // attribute collection
                    } else {
                        pointAttr = seriesPointAttr;
                    }

                    point.pointAttr = pointAttr;
                }
            }
        },

        /**
         * Clear DOM objects and free up memory
         */
        destroy: function () {
            var series = this,
                chart = series.chart,
                issue134 = /AppleWebKit\/533/.test(userAgent),
                destroy,
                i,
                data = series.data || [],
                point,
                prop,
                axis;

            // add event hook
            fireEvent(series, 'destroy');

            // remove all events
            removeEvent(series);

            // erase from axes
            each(series.axisTypes || [], function (AXIS) {
                axis = series[AXIS];
                if (axis) {
                    erase(axis.series, series);
                    axis.isDirty = axis.forceRedraw = true;
                }
            });

            // remove legend items
            if (series.legendItem) {
                series.chart.legend.destroyItem(series);
            }

            // destroy all points with their elements
            i = data.length;
            while (i--) {
                point = data[i];
                if (point && point.destroy) {
                    point.destroy();
                }
            }
            series.points = null;

            // Clear the animation timeout if we are destroying the series during initial animation
            clearTimeout(series.animationTimeout);

            // Destroy all SVGElements associated to the series
            for (prop in series) {
                if (series[prop] instanceof SVGElement && !series[prop].survive) { // Survive provides a hook for not destroying

                    // issue 134 workaround
                    destroy = issue134 && prop === 'group' ?
                        'hide' :
                        'destroy';

                    series[prop][destroy]();
                }
            }

            // remove from hoverSeries
            if (chart.hoverSeries === series) {
                chart.hoverSeries = null;
            }
            erase(chart.series, series);

            // clear all members
            for (prop in series) {
                delete series[prop];
            }
        },

        /**
         * Get the graph path
         */
        getGraphPath: function (points, nullsAsZeroes, connectCliffs) {
            var series = this,
                options = series.options,
                step = options.step,
                reversed,
                graphPath = [],
                gap;

            points = points || series.points;

            // Bottom of a stack is reversed
            reversed = points.reversed;
            if (reversed) {
                points.reverse();
            }
            // Reverse the steps (#5004)
            step = { right: 1, center: 2 }[step] || (step && 3);
            if (step && reversed) {
                step = 4 - step;
            }

            // Remove invalid points, especially in spline (#5015)
            if (options.connectNulls && !nullsAsZeroes && !connectCliffs) {
                points = this.getValidPoints(points);
            }

            // Build the line
            each(points, function (point, i) {

                var plotX = point.plotX,
                    plotY = point.plotY,
                    lastPoint = points[i - 1],                
                    pathToPoint; // the path to this point from the previous

                if ((point.leftCliff || (lastPoint && lastPoint.rightCliff)) && !connectCliffs) {
                    gap = true; // ... and continue
                }

                // Line series, nullsAsZeroes is not handled
                if (point.isNull && !defined(nullsAsZeroes) && i > 0) {
                    gap = !options.connectNulls;

                // Area series, nullsAsZeroes is set
                } else if (point.isNull && !nullsAsZeroes) {
                    gap = true;

                } else {

                    if (i === 0 || gap) {
                        pathToPoint = [M, point.plotX, point.plotY];
                
                    } else if (series.getPointSpline) { // generate the spline as defined in the SplineSeries object
                    
                        pathToPoint = series.getPointSpline(points, point, i);

                    } else if (step) {

                        if (step === 1) { // right
                            pathToPoint = [
                                L,
                                lastPoint.plotX,
                                plotY
                            ];
                        
                        } else if (step === 2) { // center
                            pathToPoint = [
                                L,
                                (lastPoint.plotX + plotX) / 2,
                                lastPoint.plotY,
                                L,
                                (lastPoint.plotX + plotX) / 2,
                                plotY
                            ];
                        
                        } else {
                            pathToPoint = [
                                L,
                                plotX,
                                lastPoint.plotY
                            ];
                        }
                        pathToPoint.push(L, plotX, plotY);

                    } else {
                        // normal line to next point
                        pathToPoint = [
                            L,
                            plotX,
                            plotY
                        ];
                    }


                    graphPath.push.apply(graphPath, pathToPoint);
                    gap = false;
                }
            });

            series.graphPath = graphPath;

            return graphPath;

        },

        /**
         * Draw the actual graph
         */
        drawGraph: function () {
            var series = this,
                options = this.options,
                props = [['graph', options.lineColor || this.color, options.dashStyle]],
                lineWidth = options.lineWidth,
                roundCap = options.linecap !== 'square',
                graphPath = (this.gappedPath || this.getGraphPath).call(this),
                fillColor = (this.fillGraph && this.color) || NONE, // polygon series use filled graph
                zones = this.zones;

            each(zones, function (threshold, i) {
                props.push(['zoneGraph' + i, threshold.color || series.color, threshold.dashStyle || options.dashStyle]);
            });

            // Draw the graph
            each(props, function (prop, i) {
                var graphKey = prop[0],
                    graph = series[graphKey],
                    attribs;

                if (graph) {
                    graph.animate({ d: graphPath });

                } else if ((lineWidth || fillColor) && graphPath.length) { // #1487
                    attribs = {
                        stroke: prop[1],
                        'stroke-width': lineWidth,
                        fill: fillColor,
                        zIndex: 1 // #1069
                    };
                    if (prop[2]) {
                        attribs.dashstyle = prop[2];
                    } else if (roundCap) {
                        attribs['stroke-linecap'] = attribs['stroke-linejoin'] = 'round';
                    }

                    series[graphKey] = series.chart.renderer.path(graphPath)
                        .attr(attribs)
                        .add(series.group)
                        .shadow((i < 2) && options.shadow); // add shadow to normal series (0) or to first zone (1) #3932
                }
            });
        },

        /**
         * Clip the graphs into the positive and negative coloured graphs
         */
        applyZones: function () {
            var series = this,
                chart = this.chart,
                renderer = chart.renderer,
                zones = this.zones,
                translatedFrom,
                translatedTo,
                clips = this.clips || [],
                clipAttr,
                graph = this.graph,
                area = this.area,
                chartSizeMax = mathMax(chart.chartWidth, chart.chartHeight),
                axis = this[(this.zoneAxis || 'y') + 'Axis'],
                extremes,
                reversed = axis.reversed,
                inverted = chart.inverted,
                horiz = axis.horiz,
                pxRange,
                pxPosMin,
                pxPosMax,
                ignoreZones = false;

            if (zones.length && (graph || area) && axis.min !== UNDEFINED) {
                // The use of the Color Threshold assumes there are no gaps
                // so it is safe to hide the original graph and area
                if (graph) {
                    graph.hide();
                }
                if (area) {
                    area.hide();
                }

                // Create the clips
                extremes = axis.getExtremes();
                each(zones, function (threshold, i) {

                    translatedFrom = reversed ?
                        (horiz ? chart.plotWidth : 0) :
                        (horiz ? 0 : axis.toPixels(extremes.min));
                    translatedFrom = mathMin(mathMax(pick(translatedTo, translatedFrom), 0), chartSizeMax);
                    translatedTo = mathMin(mathMax(mathRound(axis.toPixels(pick(threshold.value, extremes.max), true)), 0), chartSizeMax);

                    if (ignoreZones) {
                        translatedFrom = translatedTo = axis.toPixels(extremes.max);
                    }

                    pxRange = Math.abs(translatedFrom - translatedTo);
                    pxPosMin = mathMin(translatedFrom, translatedTo);
                    pxPosMax = mathMax(translatedFrom, translatedTo);
                    if (axis.isXAxis) {
                        clipAttr = {
                            x: inverted ? pxPosMax : pxPosMin,
                            y: 0,
                            width: pxRange,
                            height: chartSizeMax
                        };
                        if (!horiz) {
                            clipAttr.x = chart.plotHeight - clipAttr.x;
                        }
                    } else {
                        clipAttr = {
                            x: 0,
                            y: inverted ? pxPosMax : pxPosMin,
                            width: chartSizeMax,
                            height: pxRange
                        };
                        if (horiz) {
                            clipAttr.y = chart.plotWidth - clipAttr.y;
                        }
                    }

                    /// VML SUPPPORT
                    if (chart.inverted && renderer.isVML) {
                        if (axis.isXAxis) {
                            clipAttr = {
                                x: 0,
                                y: reversed ? pxPosMin : pxPosMax,
                                height: clipAttr.width,
                                width: chart.chartWidth
                            };
                        } else {
                            clipAttr = {
                                x: clipAttr.y - chart.plotLeft - chart.spacingBox.x,
                                y: 0,
                                width: clipAttr.height,
                                height: chart.chartHeight
                            };
                        }
                    }
                    /// END OF VML SUPPORT

                    if (clips[i]) {
                        clips[i].animate(clipAttr);
                    } else {
                        clips[i] = renderer.clipRect(clipAttr);

                        if (graph) {
                            series['zoneGraph' + i].clip(clips[i]);
                        }

                        if (area) {
                            series['zoneArea' + i].clip(clips[i]);
                        }
                    }
                    // if this zone extends out of the axis, ignore the others
                    ignoreZones = threshold.value > extremes.max;
                });
                this.clips = clips;
            }
        },

        /**
         * Initialize and perform group inversion on series.group and series.markerGroup
         */
        invertGroups: function () {
            var series = this,
                chart = series.chart;

            // Pie, go away (#1736)
            if (!series.xAxis) {
                return;
            }

            // A fixed size is needed for inversion to work
            function setInvert() {
                var size = {
                    width: series.yAxis.len,
                    height: series.xAxis.len
                };

                each(['group', 'markerGroup'], function (groupName) {
                    if (series[groupName]) {
                        series[groupName].attr(size).invert();
                    }
                });
            }

            addEvent(chart, 'resize', setInvert); // do it on resize
            addEvent(series, 'destroy', function () {
                removeEvent(chart, 'resize', setInvert);
            });

            // Do it now
            setInvert(); // do it now

            // On subsequent render and redraw, just do setInvert without setting up events again
            series.invertGroups = setInvert;
        },

        /**
         * General abstraction for creating plot groups like series.group, series.dataLabelsGroup and
         * series.markerGroup. On subsequent calls, the group will only be adjusted to the updated plot size.
         */
        plotGroup: function (prop, name, visibility, zIndex, parent) {
            var group = this[prop],
                isNew = !group;

            // Generate it on first call
            if (isNew) {
                this[prop] = group = this.chart.renderer.g(name)
                    .attr({
                        zIndex: zIndex || 0.1 // IE8 and pointer logic use this
                    })
                    .add(parent);

                group.addClass('highcharts-series-' + this.index);
            }

            // Place it on first and subsequent (redraw) calls
            group.attr({ visibility: visibility })[isNew ? 'attr' : 'animate'](this.getPlotBox());
            return group;
        },

        /**
         * Get the translation and scale for the plot area of this series
         */
        getPlotBox: function () {
            var chart = this.chart,
                xAxis = this.xAxis,
                yAxis = this.yAxis;

            // Swap axes for inverted (#2339)
            if (chart.inverted) {
                xAxis = yAxis;
                yAxis = this.xAxis;
            }
            return {
                translateX: xAxis ? xAxis.left : chart.plotLeft,
                translateY: yAxis ? yAxis.top : chart.plotTop,
                scaleX: 1, // #1623
                scaleY: 1
            };
        },

        /**
         * Render the graph and markers
         */
        render: function () {
            var series = this,
                chart = series.chart,
                group,
                options = series.options,
                // Animation doesn't work in IE8 quirks when the group div is hidden,
                // and looks bad in other oldIE
                animDuration = !!series.animate && chart.renderer.isSVG && animObject(options.animation).duration,
                visibility = series.visible ? 'inherit' : 'hidden', // #2597
                zIndex = options.zIndex,
                hasRendered = series.hasRendered,
                chartSeriesGroup = chart.seriesGroup;

            // the group
            group = series.plotGroup(
                'group',
                'series',
                visibility,
                zIndex,
                chartSeriesGroup
            );

            series.markerGroup = series.plotGroup(
                'markerGroup',
                'markers',
                visibility,
                zIndex,
                chartSeriesGroup
            );

            // initiate the animation
            if (animDuration) {
                series.animate(true);
            }

            // cache attributes for shapes
            series.getAttribs();

            // SVGRenderer needs to know this before drawing elements (#1089, #1795)
            group.inverted = series.isCartesian ? chart.inverted : false;

            // draw the graph if any
            if (series.drawGraph) {
                series.drawGraph();
                series.applyZones();
            }

            each(series.points, function (point) {
                if (point.redraw) {
                    point.redraw();
                }
            });

            // draw the data labels (inn pies they go before the points)
            if (series.drawDataLabels) {
                series.drawDataLabels();
            }

            // draw the points
            if (series.visible) {
                series.drawPoints();
            }


            // draw the mouse tracking area
            if (series.drawTracker && series.options.enableMouseTracking !== false) {
                series.drawTracker();
            }

            // Handle inverted series and tracker groups
            if (chart.inverted) {
                series.invertGroups();
            }

            // Initial clipping, must be defined after inverting groups for VML. Applies to columns etc. (#3839).
            if (options.clip !== false && !series.sharedClipKey && !hasRendered) {
                group.clip(chart.clipRect);
            }

            // Run the animation
            if (animDuration) {
                series.animate();
            }

            // Call the afterAnimate function on animation complete (but don't overwrite the animation.complete option
            // which should be available to the user).
            if (!hasRendered) {
                series.animationTimeout = syncTimeout(function () {
                    series.afterAnimate();
                }, animDuration);
            }

            series.isDirty = series.isDirtyData = false; // means data is in accordance with what you see
            // (See #322) series.isDirty = series.isDirtyData = false; // means data is in accordance with what you see
            series.hasRendered = true;
        },

        /**
         * Redraw the series after an update in the axes.
         */
        redraw: function () {
            var series = this,
                chart = series.chart,
                wasDirty = series.isDirty || series.isDirtyData, // cache it here as it is set to false in render, but used after
                group = series.group,
                xAxis = series.xAxis,
                yAxis = series.yAxis;

            // reposition on resize
            if (group) {
                if (chart.inverted) {
                    group.attr({
                        width: chart.plotWidth,
                        height: chart.plotHeight
                    });
                }

                group.animate({
                    translateX: pick(xAxis && xAxis.left, chart.plotLeft),
                    translateY: pick(yAxis && yAxis.top, chart.plotTop)
                });
            }

            series.translate();
            series.render();
            if (wasDirty) { // #3868, #3945
                delete this.kdTree;
            }
        },

        /**
         * KD Tree && PointSearching Implementation
         */

        kdDimensions: 1,
        kdAxisArray: ['clientX', 'plotY'],

        searchPoint: function (e, compareX) {
            var series = this,
                xAxis = series.xAxis,
                yAxis = series.yAxis,
                inverted = series.chart.inverted;

            return this.searchKDTree({
                clientX: inverted ? xAxis.len - e.chartY + xAxis.pos : e.chartX - xAxis.pos,
                plotY: inverted ? yAxis.len - e.chartX + yAxis.pos : e.chartY - yAxis.pos
            }, compareX);
        },

        buildKDTree: function () {
            var series = this,
                dimensions = series.kdDimensions;

            // Internal function
            function _kdtree(points, depth, dimensions) {
                var axis,
                    median,
                    length = points && points.length;

                if (length) {

                    // alternate between the axis
                    axis = series.kdAxisArray[depth % dimensions];

                    // sort point array
                    points.sort(function (a, b) {
                        return a[axis] - b[axis];
                    });

                    median = Math.floor(length / 2);

                    // build and return nod
                    return {
                        point: points[median],
                        left: _kdtree(points.slice(0, median), depth + 1, dimensions),
                        right: _kdtree(points.slice(median + 1), depth + 1, dimensions)
                    };

                }
            }

            // Start the recursive build process with a clone of the points array and null points filtered out (#3873)
            function startRecursive() {
                series.kdTree = _kdtree(
                    series.getValidPoints(
                        null,
                        !series.directTouch // For line-type series restrict to plot area, but column-type series not (#3916, #4511)
                    ),
                    dimensions,
                    dimensions
                );
            }
            delete series.kdTree;

            // For testing tooltips, don't build async
            syncTimeout(startRecursive, series.options.kdNow ? 0 : 1);
        },

        searchKDTree: function (point, compareX) {
            var series = this,
                kdX = this.kdAxisArray[0],
                kdY = this.kdAxisArray[1],
                kdComparer = compareX ? 'distX' : 'dist';

            // Set the one and two dimensional distance on the point object
            function setDistance(p1, p2) {
                var x = (defined(p1[kdX]) && defined(p2[kdX])) ? Math.pow(p1[kdX] - p2[kdX], 2) : null,
                    y = (defined(p1[kdY]) && defined(p2[kdY])) ? Math.pow(p1[kdY] - p2[kdY], 2) : null,
                    r = (x || 0) + (y || 0);

                p2.dist = defined(r) ? Math.sqrt(r) : Number.MAX_VALUE;
                p2.distX = defined(x) ? Math.sqrt(x) : Number.MAX_VALUE;
            }
            function _search(search, tree, depth, dimensions) {
                var point = tree.point,
                    axis = series.kdAxisArray[depth % dimensions],
                    tdist,
                    sideA,
                    sideB,
                    ret = point,
                    nPoint1,
                    nPoint2;

                setDistance(search, point);

                // Pick side based on distance to splitting point
                tdist = search[axis] - point[axis];
                sideA = tdist < 0 ? 'left' : 'right';
                sideB = tdist < 0 ? 'right' : 'left';

                // End of tree
                if (tree[sideA]) {
                    nPoint1 = _search(search, tree[sideA], depth + 1, dimensions);

                    ret = (nPoint1[kdComparer] < ret[kdComparer] ? nPoint1 : point);
                }
                if (tree[sideB]) {
                    // compare distance to current best to splitting point to decide wether to check side B or not
                    if (Math.sqrt(tdist * tdist) < ret[kdComparer]) {
                        nPoint2 = _search(search, tree[sideB], depth + 1, dimensions);
                        ret = (nPoint2[kdComparer] < ret[kdComparer] ? nPoint2 : ret);
                    }
                }

                return ret;
            }

            if (!this.kdTree) {
                this.buildKDTree();
            }

            if (this.kdTree) {
                return _search(point,
                    this.kdTree, this.kdDimensions, this.kdDimensions);
            }
        }

    }; // end Series prototype

    /**
     * The class for stack items
     */
    function StackItem(axis, options, isNegative, x, stackOption) {

        var inverted = axis.chart.inverted;

        this.axis = axis;

        // Tells if the stack is negative
        this.isNegative = isNegative;

        // Save the options to be able to style the label
        this.options = options;

        // Save the x value to be able to position the label later
        this.x = x;

        // Initialize total value
        this.total = null;

        // This will keep each points' extremes stored by series.index and point index
        this.points = {};

        // Save the stack option on the series configuration object, and whether to treat it as percent
        this.stack = stackOption;
        this.leftCliff = 0;
        this.rightCliff = 0;

        // The align options and text align varies on whether the stack is negative and
        // if the chart is inverted or not.
        // First test the user supplied value, then use the dynamic.
        this.alignOptions = {
            align: options.align || (inverted ? (isNegative ? 'left' : 'right') : 'center'),
            verticalAlign: options.verticalAlign || (inverted ? 'middle' : (isNegative ? 'bottom' : 'top')),
            y: pick(options.y, inverted ? 4 : (isNegative ? 14 : -6)),
            x: pick(options.x, inverted ? (isNegative ? -6 : 6) : 0)
        };

        this.textAlign = options.textAlign || (inverted ? (isNegative ? 'right' : 'left') : 'center');
    }

    StackItem.prototype = {
        destroy: function () {
            destroyObjectProperties(this, this.axis);
        },

        /**
         * Renders the stack total label and adds it to the stack label group.
         */
        render: function (group) {
            var options = this.options,
                formatOption = options.format,
                str = formatOption ?
                    format(formatOption, this) :
                    options.formatter.call(this);  // format the text in the label

            // Change the text to reflect the new total and set visibility to hidden in case the serie is hidden
            if (this.label) {
                this.label.attr({ text: str, visibility: 'hidden' });
            // Create new label
            } else {
                this.label =
                    this.axis.chart.renderer.text(str, null, null, options.useHTML)        // dummy positions, actual position updated with setOffset method in columnseries
                        .css(options.style)                // apply style
                        .attr({
                            align: this.textAlign,                // fix the text-anchor
                            rotation: options.rotation,    // rotation
                            visibility: HIDDEN                    // hidden until setOffset is called
                        })
                        .add(group);                            // add to the labels-group
            }
        },

        /**
         * Sets the offset that the stack has from the x value and repositions the label.
         */
        setOffset: function (xOffset, xWidth) {
            var stackItem = this,
                axis = stackItem.axis,
                chart = axis.chart,
                inverted = chart.inverted,
                reversed = axis.reversed,
                neg = (this.isNegative && !reversed) || (!this.isNegative && reversed), // #4056
                y = axis.translate(axis.usePercentage ? 100 : this.total, 0, 0, 0, 1), // stack value translated mapped to chart coordinates
                yZero = axis.translate(0),                        // stack origin
                h = mathAbs(y - yZero),                            // stack height
                x = chart.xAxis[0].translate(this.x) + xOffset,    // stack x position
                plotHeight = chart.plotHeight,
                stackBox = {    // this is the box for the complete stack
                    x: inverted ? (neg ? y : y - h) : x,
                    y: inverted ? plotHeight - x - xWidth : (neg ? (plotHeight - y - h) : plotHeight - y),
                    width: inverted ? h : xWidth,
                    height: inverted ? xWidth : h
                },
                label = this.label,
                alignAttr;

            if (label) {
                label.align(this.alignOptions, null, stackBox);    // align the label to the box

                // Set visibility (#678)
                alignAttr = label.alignAttr;
                label[this.options.crop === false || chart.isInsidePlot(alignAttr.x, alignAttr.y) ? 'show' : 'hide'](true);
            }
        }
    };

    /**
     * Generate stacks for each series and calculate stacks total values
     */
    Chart.prototype.getStacks = function () {
        var chart = this;

        // reset stacks for each yAxis
        each(chart.yAxis, function (axis) {
            if (axis.stacks && axis.hasVisibleSeries) {
                axis.oldStacks = axis.stacks;
            }
        });

        each(chart.series, function (series) {
            if (series.options.stacking && (series.visible === true || chart.options.chart.ignoreHiddenSeries === false)) {
                series.stackKey = series.type + pick(series.options.stack, '');
            }
        });
    };


    // Stacking methods defined on the Axis prototype

    /**
     * Build the stacks from top down
     */
    Axis.prototype.buildStacks = function () {
        var axisSeries = this.series,
            series,
            reversedStacks = pick(this.options.reversedStacks, true),
            len = axisSeries.length,
            i;
        if (!this.isXAxis) {
            this.usePercentage = false;
            i = len;
            while (i--) {
                axisSeries[reversedStacks ? i : len - i - 1].setStackedPoints();
            }

            i = len;
            while (i--) {
                series = axisSeries[reversedStacks ? i : len - i - 1];
                if (series.setStackCliffs) {
                    series.setStackCliffs();
                }
            }
            // Loop up again to compute percent stack
            if (this.usePercentage) {
                for (i = 0; i < len; i++) {
                    axisSeries[i].setPercentStacks();
                }
            }
        }
    };

    Axis.prototype.renderStackTotals = function () {
        var axis = this,
            chart = axis.chart,
            renderer = chart.renderer,
            stacks = axis.stacks,
            stackKey,
            oneStack,
            stackCategory,
            stackTotalGroup = axis.stackTotalGroup;

        // Create a separate group for the stack total labels
        if (!stackTotalGroup) {
            axis.stackTotalGroup = stackTotalGroup =
                renderer.g('stack-labels')
                    .attr({
                        visibility: VISIBLE,
                        zIndex: 6
                    })
                    .add();
        }

        // plotLeft/Top will change when y axis gets wider so we need to translate the
        // stackTotalGroup at every render call. See bug #506 and #516
        stackTotalGroup.translate(chart.plotLeft, chart.plotTop);

        // Render each stack total
        for (stackKey in stacks) {
            oneStack = stacks[stackKey];
            for (stackCategory in oneStack) {
                oneStack[stackCategory].render(stackTotalGroup);
            }
        }
    };

    /**
     * Set all the stacks to initial states and destroy unused ones.
     */
    Axis.prototype.resetStacks = function () {
        var stacks = this.stacks,
            type,
            i;
        if (!this.isXAxis) {
            for (type in stacks) {
                for (i in stacks[type]) {

                    // Clean up memory after point deletion (#1044, #4320)
                    if (stacks[type][i].touched < this.stacksTouched) {
                        stacks[type][i].destroy();
                        delete stacks[type][i];

                    // Reset stacks
                    } else {
                        stacks[type][i].total = null;
                        stacks[type][i].cum = 0;
                    }
                }
            }
        }
    };

    Axis.prototype.cleanStacks = function () {
        var stacks, type, i;

        if (!this.isXAxis) {
            if (this.oldStacks) {
                stacks = this.stacks = this.oldStacks;
            }

            // reset stacks
            for (type in stacks) {
                for (i in stacks[type]) {
                    stacks[type][i].cum = stacks[type][i].total;
                }
            }
        }
    };


    // Stacking methods defnied for Series prototype

    /**
     * Adds series' points value to corresponding stack
     */
    Series.prototype.setStackedPoints = function () {
        if (!this.options.stacking || (this.visible !== true && this.chart.options.chart.ignoreHiddenSeries !== false)) {
            return;
        }

        var series = this,
            xData = series.processedXData,
            yData = series.processedYData,
            stackedYData = [],
            yDataLength = yData.length,
            seriesOptions = series.options,
            threshold = seriesOptions.threshold,
            stackThreshold = seriesOptions.startFromThreshold ? threshold : 0,
            stackOption = seriesOptions.stack,
            stacking = seriesOptions.stacking,
            stackKey = series.stackKey,
            negKey = '-' + stackKey,
            negStacks = series.negStacks,
            yAxis = series.yAxis,
            stacks = yAxis.stacks,
            oldStacks = yAxis.oldStacks,
            stackIndicator,
            isNegative,
            stack,
            other,
            key,
            pointKey,
            i,
            x,
            y;


        yAxis.stacksTouched += 1;

        // loop over the non-null y values and read them into a local array
        for (i = 0; i < yDataLength; i++) {
            x = xData[i];
            y = yData[i];
            stackIndicator = series.getStackIndicator(stackIndicator, x, series.index);
            pointKey = stackIndicator.key;
            // Read stacked values into a stack based on the x value,
            // the sign of y and the stack key. Stacking is also handled for null values (#739)
            isNegative = negStacks && y < (stackThreshold ? 0 : threshold);
            key = isNegative ? negKey : stackKey;

            // Create empty object for this stack if it doesn't exist yet
            if (!stacks[key]) {
                stacks[key] = {};
            }

            // Initialize StackItem for this x
            if (!stacks[key][x]) {
                if (oldStacks[key] && oldStacks[key][x]) {
                    stacks[key][x] = oldStacks[key][x];
                    stacks[key][x].total = null;
                } else {
                    stacks[key][x] = new StackItem(yAxis, yAxis.options.stackLabels, isNegative, x, stackOption);
                }
            }

            // If the StackItem doesn't exist, create it first
            stack = stacks[key][x];
            if (y !== null) {
                stack.points[pointKey] = stack.points[series.index] = [pick(stack.cum, stackThreshold)];
                stack.touched = yAxis.stacksTouched;
        

                // In area charts, if there are multiple points on the same X value, let the 
                // area fill the full span of those points
                if (stackIndicator.index > 0 && series.singleStacks === false) {
                    stack.points[pointKey][0] = stack.points[series.index + ',' + x + ',0'][0];
                }
            }

            // Add value to the stack total
            if (stacking === 'percent') {

                // Percent stacked column, totals are the same for the positive and negative stacks
                other = isNegative ? stackKey : negKey;
                if (negStacks && stacks[other] && stacks[other][x]) {
                    other = stacks[other][x];
                    stack.total = other.total = mathMax(other.total, stack.total) + mathAbs(y) || 0;

                // Percent stacked areas
                } else {
                    stack.total = correctFloat(stack.total + (mathAbs(y) || 0));
                }
            } else {
                stack.total = correctFloat(stack.total + (y || 0));
            }

            stack.cum = pick(stack.cum, stackThreshold) + (y || 0);

            if (y !== null) {
                stack.points[pointKey].push(stack.cum);
                stackedYData[i] = stack.cum;
            }

        }

        if (stacking === 'percent') {
            yAxis.usePercentage = true;
        }

        this.stackedYData = stackedYData; // To be used in getExtremes

        // Reset old stacks
        yAxis.oldStacks = {};
    };

    /**
     * Iterate over all stacks and compute the absolute values to percent
     */
    Series.prototype.setPercentStacks = function () {
        var series = this,
            stackKey = series.stackKey,
            stacks = series.yAxis.stacks,
            processedXData = series.processedXData,
            stackIndicator;

        each([stackKey, '-' + stackKey], function (key) {
            var i = processedXData.length,
                x,
                stack,
                pointExtremes,
                totalFactor;

            while (i--) {
                x = processedXData[i];
                stackIndicator = series.getStackIndicator(stackIndicator, x, series.index);
                stack = stacks[key] && stacks[key][x];
                pointExtremes = stack && stack.points[stackIndicator.key];
                if (pointExtremes) {
                    totalFactor = stack.total ? 100 / stack.total : 0;
                    pointExtremes[0] = correctFloat(pointExtremes[0] * totalFactor); // Y bottom value
                    pointExtremes[1] = correctFloat(pointExtremes[1] * totalFactor); // Y value
                    series.stackedYData[i] = pointExtremes[1];
                }
            }
        });
    };

    /**
    * Get stack indicator, according to it's x-value, to determine points with the same x-value
    */
    Series.prototype.getStackIndicator = function (stackIndicator, x, index) {
        if (!defined(stackIndicator) || stackIndicator.x !== x) {
            stackIndicator = {
                x: x,
                index: 0
            };
        } else {
            stackIndicator.index++;
        }

        stackIndicator.key = [index, x, stackIndicator.index].join(',');

        return stackIndicator;
    };

    // Extend the Chart prototype for dynamic methods
    extend(Chart.prototype, {

        /**
         * Add a series dynamically after  time
         *
         * @param {Object} options The config options
         * @param {Boolean} redraw Whether to redraw the chart after adding. Defaults to true.
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         *
         * @return {Object} series The newly created series object
         */
        addSeries: function (options, redraw, animation) {
            var series,
                chart = this;

            if (options) {
                redraw = pick(redraw, true); // defaults to true

                fireEvent(chart, 'addSeries', { options: options }, function () {
                    series = chart.initSeries(options);

                    chart.isDirtyLegend = true; // the series array is out of sync with the display
                    chart.linkSeries();
                    if (redraw) {
                        chart.redraw(animation);
                    }
                });
            }

            return series;
        },

        /**
         * Add an axis to the chart
         * @param {Object} options The axis option
         * @param {Boolean} isX Whether it is an X axis or a value axis
         */
        addAxis: function (options, isX, redraw, animation) {
            var key = isX ? 'xAxis' : 'yAxis',
                chartOptions = this.options,
                userOptions = merge(options, {
                    index: this[key].length,
                    isX: isX
                });

            new Axis(this, userOptions); // eslint-disable-line no-new

            // Push the new axis options to the chart options
            chartOptions[key] = splat(chartOptions[key] || {});
            chartOptions[key].push(userOptions);

            if (pick(redraw, true)) {
                this.redraw(animation);
            }
        },

        /**
         * Dim the chart and show a loading text or symbol
         * @param {String} str An optional text to show in the loading label instead of the default one
         */
        showLoading: function (str) {
            var chart = this,
                options = chart.options,
                loadingDiv = chart.loadingDiv,
                loadingOptions = options.loading,
                setLoadingSize = function () {
                    if (loadingDiv) {
                        css(loadingDiv, {
                            left: chart.plotLeft + PX,
                            top: chart.plotTop + PX,
                            width: chart.plotWidth + PX,
                            height: chart.plotHeight + PX
                        });
                    }
                };

            // create the layer at the first call
            if (!loadingDiv) {
                chart.loadingDiv = loadingDiv = createElement(DIV, {
                    className: PREFIX + 'loading'
                }, extend(loadingOptions.style, {
                    zIndex: 10,
                    display: NONE
                }), chart.container);

                chart.loadingSpan = createElement(
                    'span',
                    null,
                    loadingOptions.labelStyle,
                    loadingDiv
                );
                addEvent(chart, 'redraw', setLoadingSize); // #1080
            }

            // update text
            chart.loadingSpan.innerHTML = str || options.lang.loading;

            // show it
            if (!chart.loadingShown) {
                css(loadingDiv, {
                    opacity: 0,
                    display: ''
                });
                animate(loadingDiv, {
                    opacity: loadingOptions.style.opacity
                }, {
                    duration: loadingOptions.showDuration || 0
                });
                chart.loadingShown = true;
            }
            setLoadingSize();
        },

        /**
         * Hide the loading layer
         */
        hideLoading: function () {
            var options = this.options,
                loadingDiv = this.loadingDiv;

            if (loadingDiv) {
                animate(loadingDiv, {
                    opacity: 0
                }, {
                    duration: options.loading.hideDuration || 100,
                    complete: function () {
                        css(loadingDiv, { display: NONE });
                    }
                });
            }
            this.loadingShown = false;
        }
    });

    // extend the Point prototype for dynamic methods
    extend(Point.prototype, {
        /**
         * Update the point with new options (typically x/y data) and optionally redraw the series.
         *
         * @param {Object} options Point options as defined in the series.data array
         * @param {Boolean} redraw Whether to redraw the chart or wait for an explicit call
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         *
         */
        update: function (options, redraw, animation, runEvent) {
            var point = this,
                series = point.series,
                graphic = point.graphic,
                i,
                chart = series.chart,
                seriesOptions = series.options,
                names = series.xAxis && series.xAxis.names;

            redraw = pick(redraw, true);

            function update() {

                point.applyOptions(options);

                // Update visuals
                if (point.y === null && graphic) { // #4146
                    point.graphic = graphic.destroy();
                }
                if (isObject(options) && !isArray(options)) {
                    // Defer the actual redraw until getAttribs has been called (#3260)
                    point.redraw = function () {
                        if (graphic && graphic.element) {
                            if (options && options.marker && options.marker.symbol) {
                                point.graphic = graphic.destroy();
                            }
                        }
                        if (options && options.dataLabels && point.dataLabel) { // #2468
                            point.dataLabel = point.dataLabel.destroy();
                        }
                        point.redraw = null;
                    };
                }

                // record changes in the parallel arrays
                i = point.index;
                series.updateParallelArrays(point, i);
                if (names && point.name) {
                    names[point.x] = point.name;
                }

                // Record the options to options.data. If there is an object from before,
                // use point options, otherwise use raw options. (#4701)
                seriesOptions.data[i] =  (isObject(seriesOptions.data[i]) && !isArray(seriesOptions.data[i])) ? point.options : options;

                // redraw
                series.isDirty = series.isDirtyData = true;
                if (!series.fixedBox && series.hasCartesianSeries) { // #1906, #2320
                    chart.isDirtyBox = true;
                }

                if (seriesOptions.legendType === 'point') { // #1831, #1885
                    chart.isDirtyLegend = true;
                }
                if (redraw) {
                    chart.redraw(animation);
                }
            }

            // Fire the event with a default handler of doing the update
            if (runEvent === false) { // When called from setData
                update();
            } else {
                point.firePointEvent('update', { options: options }, update);
            }
        },

        /**
         * Remove a point and optionally redraw the series and if necessary the axes
         * @param {Boolean} redraw Whether to redraw the chart or wait for an explicit call
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         */
        remove: function (redraw, animation) {
            this.series.removePoint(inArray(this, this.series.data), redraw, animation);
        }
    });

    // Extend the series prototype for dynamic methods
    extend(Series.prototype, {
        /**
         * Add a point dynamically after chart load time
         * @param {Object} options Point options as given in series.data
         * @param {Boolean} redraw Whether to redraw the chart or wait for an explicit call
         * @param {Boolean} shift If shift is true, a point is shifted off the start
         *    of the series as one is appended to the end.
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         */
        addPoint: function (options, redraw, shift, animation) {
            var series = this,
                seriesOptions = series.options,
                data = series.data,
                graph = series.graph,
                area = series.area,
                chart = series.chart,
                names = series.xAxis && series.xAxis.names,
                currentShift = (graph && graph.shift) || 0,
                shiftShapes = ['graph', 'area'],
                dataOptions = seriesOptions.data,
                point,
                isInTheMiddle,
                xData = series.xData,
                i,
                x;

            setAnimation(animation, chart);

            // Make graph animate sideways
            if (shift) {
                i = series.zones.length;
                while (i--) {
                    shiftShapes.push('zoneGraph' + i, 'zoneArea' + i);
                }
                each(shiftShapes, function (shape) {
                    if (series[shape]) {
                        series[shape].shift = currentShift + (seriesOptions.step ? 2 : 1);
                    }
                });
            }
            if (area) {
                area.isArea = true; // needed in animation, both with and without shift
            }

            // Optional redraw, defaults to true
            redraw = pick(redraw, true);

            // Get options and push the point to xData, yData and series.options. In series.generatePoints
            // the Point instance will be created on demand and pushed to the series.data array.
            point = { series: series };
            series.pointClass.prototype.applyOptions.apply(point, [options]);
            x = point.x;

            // Get the insertion point
            i = xData.length;
            if (series.requireSorting && x < xData[i - 1]) {
                isInTheMiddle = true;
                while (i && xData[i - 1] > x) {
                    i--;
                }
            }

            series.updateParallelArrays(point, 'splice', i, 0, 0); // insert undefined item
            series.updateParallelArrays(point, i); // update it

            if (names && point.name) {
                names[x] = point.name;
            }
            dataOptions.splice(i, 0, options);

            if (isInTheMiddle) {
                series.data.splice(i, 0, null);
                series.processData();
            }

            // Generate points to be added to the legend (#1329)
            if (seriesOptions.legendType === 'point') {
                series.generatePoints();
            }

            // Shift the first point off the parallel arrays
            if (shift) {
                if (data[0] && data[0].remove) {
                    data[0].remove(false);
                } else {
                    data.shift();
                    series.updateParallelArrays(point, 'shift');

                    dataOptions.shift();
                }
            }

            // redraw
            series.isDirty = true;
            series.isDirtyData = true;
            if (redraw) {
                series.getAttribs(); // #1937
                chart.redraw();
            }
        },

        /**
         * Remove a point (rendered or not), by index
         */
        removePoint: function (i, redraw, animation) {

            var series = this,
                data = series.data,
                point = data[i],
                points = series.points,
                chart = series.chart,
                remove = function () {

                    if (points && points.length === data.length) { // #4935
                        points.splice(i, 1);
                    }
                    data.splice(i, 1);
                    series.options.data.splice(i, 1);
                    series.updateParallelArrays(point || { series: series }, 'splice', i, 1);

                    if (point) {
                        point.destroy();
                    }

                    // redraw
                    series.isDirty = true;
                    series.isDirtyData = true;
                    if (redraw) {
                        chart.redraw();
                    }
                };

            setAnimation(animation, chart);
            redraw = pick(redraw, true);

            // Fire the event with a default handler of removing the point
            if (point) {
                point.firePointEvent('remove', null, remove);
            } else {
                remove();
            }
        },

        /**
         * Remove a series and optionally redraw the chart
         *
         * @param {Boolean} redraw Whether to redraw the chart or wait for an explicit call
         * @param {Boolean|Object} animation Whether to apply animation, and optionally animation
         *    configuration
         */
        remove: function (redraw, animation) {
            var series = this,
                chart = series.chart;

            // Fire the event with a default handler of removing the point
            fireEvent(series, 'remove', null, function () {

                // Destroy elements
                series.destroy();

                // Redraw
                chart.isDirtyLegend = chart.isDirtyBox = true;
                chart.linkSeries();

                if (pick(redraw, true)) {
                    chart.redraw(animation);
                }
            });
        },

        /**
         * Update the series with a new set of options
         */
        update: function (newOptions, redraw) {
            var series = this,
                chart = this.chart,
                // must use user options when changing type because this.options is merged
                // in with type specific plotOptions
                oldOptions = this.userOptions,
                oldType = this.type,
                proto = seriesTypes[oldType].prototype,
                preserve = ['group', 'markerGroup', 'dataLabelsGroup'],
                n;

            // If we're changing type or zIndex, create new groups (#3380, #3404)
            if ((newOptions.type && newOptions.type !== oldType) || newOptions.zIndex !== undefined) {
                preserve.length = 0;
            }

            // Make sure groups are not destroyed (#3094)
            each(preserve, function (prop) {
                preserve[prop] = series[prop];
                delete series[prop];
            });

            // Do the merge, with some forced options
            newOptions = merge(oldOptions, {
                animation: false,
                index: this.index,
                pointStart: this.xData[0] // when updating after addPoint
            }, { data: this.options.data }, newOptions);

            // Destroy the series and delete all properties. Reinsert all methods
            // and properties from the new type prototype (#2270, #3719)
            this.remove(false);
            for (n in proto) {
                this[n] = UNDEFINED;
            }
            extend(this, seriesTypes[newOptions.type || oldType].prototype);

            // Re-register groups (#3094)
            each(preserve, function (prop) {
                series[prop] = preserve[prop];
            });

            this.init(chart, newOptions);
            chart.linkSeries(); // Links are lost in this.remove (#3028)
            if (pick(redraw, true)) {
                chart.redraw(false);
            }
        }
    });

    // Extend the Axis.prototype for dynamic methods
    extend(Axis.prototype, {

        /**
         * Update the axis with a new options structure
         */
        update: function (newOptions, redraw) {
            var chart = this.chart;

            newOptions = chart.options[this.coll][this.options.index] = merge(this.userOptions, newOptions);

            this.destroy(true);
            this._addedPlotLB = this.chart._labelPanes = UNDEFINED; // #1611, #2887, #4314

            this.init(chart, extend(newOptions, { events: UNDEFINED }));

            chart.isDirtyBox = true;
            if (pick(redraw, true)) {
                chart.redraw();
            }
        },

        /**
         * Remove the axis from the chart
         */
        remove: function (redraw) {
            var chart = this.chart,
                key = this.coll, // xAxis or yAxis
                axisSeries = this.series,
                i = axisSeries.length;

            // Remove associated series (#2687)
            while (i--) {
                if (axisSeries[i]) {
                    axisSeries[i].remove(false);
                }
            }

            // Remove the axis
            erase(chart.axes, this);
            erase(chart[key], this);
            chart.options[key].splice(this.options.index, 1);
            each(chart[key], function (axis, i) { // Re-index, #1706
                axis.options.index = i;
            });
            this.destroy();
            chart.isDirtyBox = true;

            if (pick(redraw, true)) {
                chart.redraw();
            }
        },

        /**
         * Update the axis title by options
         */
        setTitle: function (newTitleOptions, redraw) {
            this.update({ title: newTitleOptions }, redraw);
        },

        /**
         * Set new axis categories and optionally redraw
         * @param {Array} categories
         * @param {Boolean} redraw
         */
        setCategories: function (categories, redraw) {
            this.update({ categories: categories }, redraw);
        }

    });


    /**
     * LineSeries object
     */
    var LineSeries = extendClass(Series);
    seriesTypes.line = LineSeries;

    /**
     * Set the default options for area
     */
    defaultPlotOptions.area = merge(defaultSeriesOptions, {
        softThreshold: false,
        threshold: 0
        // trackByArea: false,
        // lineColor: null, // overrides color, but lets fillColor be unaltered
        // fillOpacity: 0.75,
        // fillColor: null
    });

    /**
     * AreaSeries object
     */
    var AreaSeries = extendClass(Series, {
        type: 'area',
        singleStacks: false,
        /** 
         * Return an array of stacked points, where null and missing points are replaced by 
         * dummy points in order for gaps to be drawn correctly in stacks.
         */
        getStackPoints: function () {
            var series = this,
                segment = [],
                keys = [],
                xAxis = this.xAxis,
                yAxis = this.yAxis,
                stack = yAxis.stacks[this.stackKey],
                pointMap = {},
                points = this.points,
                seriesIndex = series.index,
                yAxisSeries = yAxis.series,
                seriesLength = yAxisSeries.length,
                visibleSeries,
                upOrDown = pick(yAxis.options.reversedStacks, true) ? 1 : -1,
                i,
                x;

            if (this.options.stacking) {
                // Create a map where we can quickly look up the points by their X value.
                for (i = 0; i < points.length; i++) {
                    pointMap[points[i].x] = points[i];
                }

                // Sort the keys (#1651)
                for (x in stack) {
                    if (stack[x].total !== null) { // nulled after switching between grouping and not (#1651, #2336)
                        keys.push(x);
                    }
                }
                keys.sort(function (a, b) {
                    return a - b;
                });

                visibleSeries = map(yAxisSeries, function () {
                    return this.visible;
                });

                each(keys, function (x, idx) {
                    var y = 0,
                        stackPoint,
                        stackedValues;

                    if (pointMap[x] && !pointMap[x].isNull) {
                        segment.push(pointMap[x]);

                        // Find left and right cliff. -1 goes left, 1 goes right.
                        each([-1, 1], function (direction) {
                            var nullName = direction === 1 ? 'rightNull' : 'leftNull',
                                cliffName = direction === 1 ? 'rightCliff' : 'leftCliff',
                                cliff = 0,
                                otherStack = stack[keys[idx + direction]];

                            // If there is a stack next to this one, to the left or to the right...
                            if (otherStack) {
                                i = seriesIndex;
                                while (i >= 0 && i < seriesLength) { // Can go either up or down, depending on reversedStacks
                                    stackPoint = otherStack.points[i];
                                    if (!stackPoint) {
                                        // If the next point in this series is missing, mark the point
                                        // with point.leftNull or point.rightNull = true.
                                        if (i === seriesIndex) {
                                            pointMap[x][nullName] = true;

                                        // If there are missing points in the next stack in any of the 
                                        // series below this one, we need to substract the missing values
                                        // and add a hiatus to the left or right.
                                        } else if (visibleSeries[i]) {
                                            stackedValues = stack[x].points[i];
                                            if (stackedValues) {
                                                cliff -= stackedValues[1] - stackedValues[0];
                                            }
                                        }
                                    }
                                    // When reversedStacks is true, loop up, else loop down
                                    i += upOrDown; 
                                }                
                            }
                            pointMap[x][cliffName] = cliff;
                        });


                    // There is no point for this X value in this series, so we 
                    // insert a dummy point in order for the areas to be drawn
                    // correctly.
                    } else {

                        // Loop down the stack to find the series below this one that has
                        // a value (#1991)
                        i = seriesIndex;
                        while (i >= 0 && i < seriesLength) {
                            stackPoint = stack[x].points[i];
                            if (stackPoint) {
                                y = stackPoint[1];
                                break;
                            }
                            // When reversedStacks is true, loop up, else loop down
                            i += upOrDown;
                        }

                        y = yAxis.toPixels(y, true);
                        segment.push({ 
                            isNull: true,
                            plotX: xAxis.toPixels(x, true),
                            plotY: y,
                            yBottom: y
                        });
                    }
                });

            } 

            return segment;
        },

        getGraphPath: function (points) {
            var getGraphPath = Series.prototype.getGraphPath,
                graphPath,
                options = this.options,
                stacking = options.stacking,
                yAxis = this.yAxis,
                topPath,
                //topPoints = [],
                bottomPath,
                bottomPoints = [],
                graphPoints = [],
                seriesIndex = this.index,
                i,
                areaPath,
                plotX,
                stacks = yAxis.stacks[this.stackKey],
                threshold = options.threshold,
                translatedThreshold = yAxis.getThreshold(options.threshold),
                isNull,
                yBottom,
                connectNulls = options.connectNulls || stacking === 'percent',
                /**
                 * To display null points in underlying stacked series, this series graph must be 
                 * broken, and the area also fall down to fill the gap left by the null point. #2069
                 */
                addDummyPoints = function (i, otherI, side) {
                    var point = points[i],
                        stackedValues = stacking && stacks[point.x].points[seriesIndex],
                        nullVal = point[side + 'Null'] || 0,
                        cliffVal = point[side + 'Cliff'] || 0,
                        top,
                        bottom,
                        isNull = true;

                    if (cliffVal || nullVal) {

                        top = (nullVal ? stackedValues[0] : stackedValues[1]) + cliffVal;
                        bottom = stackedValues[0] + cliffVal;
                        isNull = !!nullVal;
                
                    } else if (!stacking && points[otherI] && points[otherI].isNull) {
                        top = bottom = threshold;
                    }

                    // Add to the top and bottom line of the area
                    if (top !== undefined) {
                        graphPoints.push({
                            plotX: plotX,
                            plotY: top === null ? translatedThreshold : yAxis.getThreshold(top),
                            isNull: isNull
                        });
                        bottomPoints.push({
                            plotX: plotX,
                            plotY: bottom === null ? translatedThreshold : yAxis.getThreshold(bottom)
                        });
                    }
                };

            // Find what points to use
            points = points || this.points;

        
            // Fill in missing points
            if (stacking) {
                points = this.getStackPoints();
            }

            for (i = 0; i < points.length; i++) {
                isNull = points[i].isNull;
                plotX = pick(points[i].rectPlotX, points[i].plotX);
                yBottom = pick(points[i].yBottom, translatedThreshold);

                if (!isNull || connectNulls) {

                    if (!connectNulls) {
                        addDummyPoints(i, i - 1, 'left');
                    }

                    if (!(isNull && !stacking && connectNulls)) { // Skip null point when stacking is false and connectNulls true
                        graphPoints.push(points[i]);
                        bottomPoints.push({
                            x: i,
                            plotX: plotX,
                            plotY: yBottom
                        });
                    }

                    if (!connectNulls) {
                        addDummyPoints(i, i + 1, 'right');
                    }
                }
            }

            topPath = getGraphPath.call(this, graphPoints, true, true);
        
            bottomPoints.reversed = true;
            bottomPath = getGraphPath.call(this, bottomPoints, true, true);
            if (bottomPath.length) {
                bottomPath[0] = L;
            }

            areaPath = topPath.concat(bottomPath);
            graphPath = getGraphPath.call(this, graphPoints, false, connectNulls); // TODO: don't set leftCliff and rightCliff when connectNulls?

            this.areaPath = areaPath;
            return graphPath;
        },

        /**
         * Draw the graph and the underlying area. This method calls the Series base
         * function and adds the area. The areaPath is calculated in the getSegmentPath
         * method called from Series.prototype.drawGraph.
         */
        drawGraph: function () {

            // Define or reset areaPath
            this.areaPath = [];

            // Call the base method
            Series.prototype.drawGraph.apply(this);

            // Define local variables
            var series = this,
                areaPath = this.areaPath,
                options = this.options,
                zones = this.zones,
                props = [['area', this.color, options.fillColor]]; // area name, main color, fill color

            each(zones, function (threshold, i) {
                props.push(['zoneArea' + i, threshold.color || series.color, threshold.fillColor || options.fillColor]);
            });
            each(props, function (prop) {
                var areaKey = prop[0],
                    area = series[areaKey],
                    attr;

                // Create or update the area
                if (area) { // update
                    area.animate({ d: areaPath });

                } else { // create
                    attr = {
                        fill: prop[2] || prop[1],
                        zIndex: 0 // #1069
                    };
                    if (!prop[2]) {
                        attr['fill-opacity'] = pick(options.fillOpacity, 0.75);
                    }
                    series[areaKey] = series.chart.renderer.path(areaPath)
                        .attr(attr)
                        .add(series.group);
                }
            });
        },

        drawLegendSymbol: LegendSymbolMixin.drawRectangle
    });

    seriesTypes.area = AreaSeries;
    /**
     * Set the default options for spline
     */
    defaultPlotOptions.spline = merge(defaultSeriesOptions);

    /**
     * SplineSeries object
     */
    var SplineSeries = extendClass(Series, {
        type: 'spline',

        /**
         * Get the spline segment from a given point's previous neighbour to the given point
         */
        getPointSpline: function (points, point, i) {
            var smoothing = 1.5, // 1 means control points midway between points, 2 means 1/3 from the point, 3 is 1/4 etc
                denom = smoothing + 1,
                plotX = point.plotX,
                plotY = point.plotY,
                lastPoint = points[i - 1],
                nextPoint = points[i + 1],
                leftContX,
                leftContY,
                rightContX,
                rightContY,
                ret;

            // Find control points
            if (lastPoint && !lastPoint.isNull && nextPoint && !nextPoint.isNull) {
                var lastX = lastPoint.plotX,
                    lastY = lastPoint.plotY,
                    nextX = nextPoint.plotX,
                    nextY = nextPoint.plotY,
                    correction = 0;

                leftContX = (smoothing * plotX + lastX) / denom;
                leftContY = (smoothing * plotY + lastY) / denom;
                rightContX = (smoothing * plotX + nextX) / denom;
                rightContY = (smoothing * plotY + nextY) / denom;

                // Have the two control points make a straight line through main point
                if (rightContX !== leftContX) { // #5016, division by zero
                    correction = ((rightContY - leftContY) * (rightContX - plotX)) /
                        (rightContX - leftContX) + plotY - rightContY;
                }

                leftContY += correction;
                rightContY += correction;

                // to prevent false extremes, check that control points are between
                // neighbouring points' y values
                if (leftContY > lastY && leftContY > plotY) {
                    leftContY = mathMax(lastY, plotY);
                    rightContY = 2 * plotY - leftContY; // mirror of left control point
                } else if (leftContY < lastY && leftContY < plotY) {
                    leftContY = mathMin(lastY, plotY);
                    rightContY = 2 * plotY - leftContY;
                }
                if (rightContY > nextY && rightContY > plotY) {
                    rightContY = mathMax(nextY, plotY);
                    leftContY = 2 * plotY - rightContY;
                } else if (rightContY < nextY && rightContY < plotY) {
                    rightContY = mathMin(nextY, plotY);
                    leftContY = 2 * plotY - rightContY;
                }

                // record for drawing in next point
                point.rightContX = rightContX;
                point.rightContY = rightContY;

            
            }

            // Visualize control points for debugging
            /*
            if (leftContX) {
                this.chart.renderer.circle(leftContX + this.chart.plotLeft, leftContY + this.chart.plotTop, 2)
                    .attr({
                        stroke: 'red',
                        'stroke-width': 1,
                        fill: 'none'
                    })
                    .add();
                this.chart.renderer.path(['M', leftContX + this.chart.plotLeft, leftContY + this.chart.plotTop,
                    'L', plotX + this.chart.plotLeft, plotY + this.chart.plotTop])
                    .attr({
                        stroke: 'red',
                        'stroke-width': 1
                    })
                    .add();
                this.chart.renderer.circle(rightContX + this.chart.plotLeft, rightContY + this.chart.plotTop, 2)
                    .attr({
                        stroke: 'green',
                        'stroke-width': 1,
                        fill: 'none'
                    })
                    .add();
                this.chart.renderer.path(['M', rightContX + this.chart.plotLeft, rightContY + this.chart.plotTop,
                    'L', plotX + this.chart.plotLeft, plotY + this.chart.plotTop])
                    .attr({
                        stroke: 'green',
                        'stroke-width': 1
                    })
                    .add();
            }
            // */
            ret = [
                'C',
                pick(lastPoint.rightContX, lastPoint.plotX),
                pick(lastPoint.rightContY, lastPoint.plotY),
                pick(leftContX, plotX),
                pick(leftContY, plotY),
                plotX,
                plotY
            ];
            lastPoint.rightContX = lastPoint.rightContY = null; // reset for updating series later
            return ret;
        }
    });
    seriesTypes.spline = SplineSeries;

    /**
     * Set the default options for areaspline
     */
    defaultPlotOptions.areaspline = merge(defaultPlotOptions.area);

    /**
     * AreaSplineSeries object
     */
    var areaProto = AreaSeries.prototype,
        AreaSplineSeries = extendClass(SplineSeries, {
            type: 'areaspline',
            getStackPoints: areaProto.getStackPoints,
            getGraphPath: areaProto.getGraphPath,
            setStackCliffs: areaProto.setStackCliffs,
            drawGraph: areaProto.drawGraph,
            drawLegendSymbol: LegendSymbolMixin.drawRectangle
        });

    seriesTypes.areaspline = AreaSplineSeries;

    /**
     * Set the default options for column
     */
    defaultPlotOptions.column = merge(defaultSeriesOptions, {
        borderColor: '#FFFFFF',
        //borderWidth: 1,
        borderRadius: 0,
        //colorByPoint: undefined,
        groupPadding: 0.2,
        //grouping: true,
        marker: null, // point options are specified in the base options
        pointPadding: 0.1,
        //pointWidth: null,
        minPointLength: 0,
        cropThreshold: 50, // when there are more points, they will not animate out of the chart on xAxis.setExtremes
        pointRange: null, // null means auto, meaning 1 in a categorized axis and least distance between points if not categories
        states: {
            hover: {
                brightness: 0.1,
                shadow: false,
                halo: false
            },
            select: {
                color: '#C0C0C0',
                borderColor: '#000000',
                shadow: false
            }
        },
        dataLabels: {
            align: null, // auto
            verticalAlign: null, // auto
            y: null
        },
        softThreshold: false,
        startFromThreshold: true, // false doesn't work well: http://jsfiddle.net/highcharts/hz8fopan/14/
        stickyTracking: false,
        tooltip: {
            distance: 6
        },
        threshold: 0
    });

    /**
     * ColumnSeries object
     */
    var ColumnSeries = extendClass(Series, {
        type: 'column',
        pointAttrToOptions: { // mapping between SVG attributes and the corresponding options
            stroke: 'borderColor',
            fill: 'color',
            r: 'borderRadius'
        },
        cropShoulder: 0,
        directTouch: true, // When tooltip is not shared, this series (and derivatives) requires direct touch/hover. KD-tree does not apply.
        trackerGroups: ['group', 'dataLabelsGroup'],
        negStacks: true, // use separate negative stacks, unlike area stacks where a negative
            // point is substracted from previous (#1910)

        /**
         * Initialize the series
         */
        init: function () {
            Series.prototype.init.apply(this, arguments);

            var series = this,
                chart = series.chart;

            // if the series is added dynamically, force redraw of other
            // series affected by a new column
            if (chart.hasRendered) {
                each(chart.series, function (otherSeries) {
                    if (otherSeries.type === series.type) {
                        otherSeries.isDirty = true;
                    }
                });
            }
        },

        /**
         * Return the width and x offset of the columns adjusted for grouping, groupPadding, pointPadding,
         * pointWidth etc.
         */
        getColumnMetrics: function () {

            var series = this,
                options = series.options,
                xAxis = series.xAxis,
                yAxis = series.yAxis,
                reversedXAxis = xAxis.reversed,
                stackKey,
                stackGroups = {},
                columnCount = 0;

            // Get the total number of column type series.
            // This is called on every series. Consider moving this logic to a
            // chart.orderStacks() function and call it on init, addSeries and removeSeries
            if (options.grouping === false) {
                columnCount = 1;
            } else {
                each(series.chart.series, function (otherSeries) {
                    var otherOptions = otherSeries.options,
                        otherYAxis = otherSeries.yAxis,
                        columnIndex;
                    if (otherSeries.type === series.type && otherSeries.visible &&
                            yAxis.len === otherYAxis.len && yAxis.pos === otherYAxis.pos) {  // #642, #2086
                        if (otherOptions.stacking) {
                            stackKey = otherSeries.stackKey;
                            if (stackGroups[stackKey] === UNDEFINED) {
                                stackGroups[stackKey] = columnCount++;
                            }
                            columnIndex = stackGroups[stackKey];
                        } else if (otherOptions.grouping !== false) { // #1162
                            columnIndex = columnCount++;
                        }
                        otherSeries.columnIndex = columnIndex;
                    }
                });
            }

            var categoryWidth = mathMin(
                    mathAbs(xAxis.transA) * (xAxis.ordinalSlope || options.pointRange || xAxis.closestPointRange || xAxis.tickInterval || 1), // #2610
                    xAxis.len // #1535
                ),
                groupPadding = categoryWidth * options.groupPadding,
                groupWidth = categoryWidth - 2 * groupPadding,
                pointOffsetWidth = groupWidth / columnCount,
                pointWidth = mathMin(
                    options.maxPointWidth || xAxis.len,
                    pick(options.pointWidth, pointOffsetWidth * (1 - 2 * options.pointPadding))
                ),
                pointPadding = (pointOffsetWidth - pointWidth) / 2,
                colIndex = (series.columnIndex || 0) + (reversedXAxis ? 1 : 0), // #1251, #3737
                pointXOffset = pointPadding + (groupPadding + colIndex *
                    pointOffsetWidth - (categoryWidth / 2)) *
                    (reversedXAxis ? -1 : 1);

            // Save it for reading in linked series (Error bars particularly)
            series.columnMetrics = {
                width: pointWidth,
                offset: pointXOffset
            };
            return series.columnMetrics;

        },

        /**
         * Make the columns crisp. The edges are rounded to the nearest full pixel.
         */
        crispCol: function (x, y, w, h) {
            var chart = this.chart,
                borderWidth = this.borderWidth,
                xCrisp = -(borderWidth % 2 ? 0.5 : 0),
                yCrisp = borderWidth % 2 ? 0.5 : 1,
                right,
                bottom,
                fromTop;

            if (chart.inverted && chart.renderer.isVML) {
                yCrisp += 1;
            }

            // Horizontal. We need to first compute the exact right edge, then round it
            // and compute the width from there.
            right = Math.round(x + w) + xCrisp;
            x = Math.round(x) + xCrisp;
            w = right - x;

            // Vertical
            bottom = Math.round(y + h) + yCrisp;
            fromTop = mathAbs(y) <= 0.5 && bottom > 0.5; // #4504, #4656
            y = Math.round(y) + yCrisp;
            h = bottom - y;

            // Top edges are exceptions
            if (fromTop && h) { // #5146
                y -= 1;
                h += 1;
            }

            return {
                x: x,
                y: y,
                width: w,
                height: h
            };
        },

        /**
         * Translate each point to the plot area coordinate system and find shape positions
         */
        translate: function () {
            var series = this,
                chart = series.chart,
                options = series.options,
                borderWidth = series.borderWidth = pick(
                    options.borderWidth,
                    series.closestPointRange * series.xAxis.transA < 2 ? 0 : 1 // #3635
                ),
                yAxis = series.yAxis,
                threshold = options.threshold,
                translatedThreshold = series.translatedThreshold = yAxis.getThreshold(threshold),
                minPointLength = pick(options.minPointLength, 5),
                metrics = series.getColumnMetrics(),
                pointWidth = metrics.width,
                seriesBarW = series.barW = mathMax(pointWidth, 1 + 2 * borderWidth), // postprocessed for border width
                pointXOffset = series.pointXOffset = metrics.offset;

            if (chart.inverted) {
                translatedThreshold -= 0.5; // #3355
            }

            // When the pointPadding is 0, we want the columns to be packed tightly, so we allow individual
            // columns to have individual sizes. When pointPadding is greater, we strive for equal-width
            // columns (#2694).
            if (options.pointPadding) {
                seriesBarW = mathCeil(seriesBarW);
            }

            Series.prototype.translate.apply(series);

            // Record the new values
            each(series.points, function (point) {
                var yBottom = mathMin(pick(point.yBottom, translatedThreshold), 9e4), // #3575
                    safeDistance = 999 + mathAbs(yBottom),
                    plotY = mathMin(mathMax(-safeDistance, point.plotY), yAxis.len + safeDistance), // Don't draw too far outside plot area (#1303, #2241, #4264)
                    barX = point.plotX + pointXOffset,
                    barW = seriesBarW,
                    barY = mathMin(plotY, yBottom),
                    up,
                    barH = mathMax(plotY, yBottom) - barY;

                // Handle options.minPointLength
                if (mathAbs(barH) < minPointLength) {
                    if (minPointLength) {
                        barH = minPointLength;
                        up = (!yAxis.reversed && !point.negative) || (yAxis.reversed && point.negative);
                        barY = mathAbs(barY - translatedThreshold) > minPointLength ? // stacked
                                yBottom - minPointLength : // keep position
                                translatedThreshold - (up ? minPointLength : 0); // #1485, #4051
                    }
                }

                // Cache for access in polar
                point.barX = barX;
                point.pointWidth = pointWidth;

                // Fix the tooltip on center of grouped columns (#1216, #424, #3648)
                point.tooltipPos = chart.inverted ?
                    [yAxis.len + yAxis.pos - chart.plotLeft - plotY, series.xAxis.len - barX - barW / 2, barH] :
                    [barX + barW / 2, plotY + yAxis.pos - chart.plotTop, barH];

                // Register shape type and arguments to be used in drawPoints
                point.shapeType = 'rect';
                point.shapeArgs = series.crispCol(barX, barY, barW, barH);
            });

        },

        getSymbol: noop,

        /**
         * Use a solid rectangle like the area series types
         */
        drawLegendSymbol: LegendSymbolMixin.drawRectangle,


        /**
         * Columns have no graph
         */
        drawGraph: noop,

        /**
         * Draw the columns. For bars, the series.group is rotated, so the same coordinates
         * apply for columns and bars. This method is inherited by scatter series.
         *
         */
        drawPoints: function () {
            var series = this,
                chart = this.chart,
                options = series.options,
                renderer = chart.renderer,
                animationLimit = options.animationLimit || 250,
                shapeArgs,
                pointAttr;

            // draw the columns
            each(series.points, function (point) {
                var plotY = point.plotY,
                    graphic = point.graphic,
                    borderAttr;

                if (isNumber(plotY) && point.y !== null) {
                    shapeArgs = point.shapeArgs;

                    borderAttr = defined(series.borderWidth) ? {
                        'stroke-width': series.borderWidth
                    } : {};

                    pointAttr = point.pointAttr[point.selected ? SELECT_STATE : NORMAL_STATE] || series.pointAttr[NORMAL_STATE];

                    if (graphic) { // update
                        stop(graphic);
                        graphic.attr(borderAttr).attr(pointAttr)[chart.pointCount < animationLimit ? 'animate' : 'attr'](merge(shapeArgs)); // #4267

                    } else {
                        point.graphic = graphic = renderer[point.shapeType](shapeArgs)
                            .attr(borderAttr)
                            .attr(pointAttr)
                            .add(point.group || series.group)
                            .shadow(options.shadow, null, options.stacking && !options.borderRadius);
                    }

                } else if (graphic) {
                    point.graphic = graphic.destroy(); // #1269
                }
            });
        },

        /**
         * Animate the column heights one by one from zero
         * @param {Boolean} init Whether to initialize the animation or run it
         */
        animate: function (init) {
            var series = this,
                yAxis = this.yAxis,
                options = series.options,
                inverted = this.chart.inverted,
                attr = {},
                translatedThreshold;

            if (hasSVG) { // VML is too slow anyway
                if (init) {
                    attr.scaleY = 0.001;
                    translatedThreshold = mathMin(yAxis.pos + yAxis.len, mathMax(yAxis.pos, yAxis.toPixels(options.threshold)));
                    if (inverted) {
                        attr.translateX = translatedThreshold - yAxis.len;
                    } else {
                        attr.translateY = translatedThreshold;
                    }
                    series.group.attr(attr);

                } else { // run the animation

                    attr[inverted ? 'translateX' : 'translateY'] = yAxis.pos;
                    series.group.animate(attr, extend(animObject(series.options.animation), {
                        // Do the scale synchronously to ensure smooth updating (#5030)
                        step: function (val, fx) {
                            series.group.attr({
                                scaleY: mathMax(0.001, fx.pos) // #5250
                            });
                        }
                    }));

                    // delete this function to allow it only once
                    series.animate = null;
                }
            }
        },

        /**
         * Remove this series from the chart
         */
        remove: function () {
            var series = this,
                chart = series.chart;

            // column and bar series affects other series of the same type
            // as they are either stacked or grouped
            if (chart.hasRendered) {
                each(chart.series, function (otherSeries) {
                    if (otherSeries.type === series.type) {
                        otherSeries.isDirty = true;
                    }
                });
            }

            Series.prototype.remove.apply(series, arguments);
        }
    });
    seriesTypes.column = ColumnSeries;
    /**
     * Set the default options for bar
     */
    defaultPlotOptions.bar = merge(defaultPlotOptions.column);
    /**
     * The Bar series class
     */
    var BarSeries = extendClass(ColumnSeries, {
        type: 'bar',
        inverted: true
    });
    seriesTypes.bar = BarSeries;

    /**
     * Set the default options for scatter
     */
    defaultPlotOptions.scatter = merge(defaultSeriesOptions, {
        lineWidth: 0,
        marker: {
            enabled: true // Overrides auto-enabling in line series (#3647)
        },
        tooltip: {
            headerFormat: '<span style="color:{point.color}">\u25CF</span> <span style="font-size: 10px;"> {series.name}</span><br/>',
            pointFormat: 'x: <b>{point.x}</b><br/>y: <b>{point.y}</b><br/>'
        }
    });

    /**
     * The scatter series class
     */
    var ScatterSeries = extendClass(Series, {
        type: 'scatter',
        sorted: false,
        requireSorting: false,
        noSharedTooltip: true,
        trackerGroups: ['group', 'markerGroup', 'dataLabelsGroup'],
        takeOrdinalPosition: false, // #2342
        kdDimensions: 2,
        drawGraph: function () {
            if (this.options.lineWidth) {
                Series.prototype.drawGraph.call(this);
            }
        }
    });

    seriesTypes.scatter = ScatterSeries;

    /**
     * Set the default options for pie
     */
    defaultPlotOptions.pie = merge(defaultSeriesOptions, {
        borderColor: '#FFFFFF',
        borderWidth: 1,
        center: [null, null],
        clip: false,
        colorByPoint: true, // always true for pies
        dataLabels: {
            // align: null,
            // connectorWidth: 1,
            // connectorColor: point.color,
            // connectorPadding: 5,
            distance: 30,
            enabled: true,
            formatter: function () { // #2945
                return this.y === null ? undefined : this.point.name;
            },
            // softConnector: true,
            x: 0
            // y: 0
        },
        ignoreHiddenPoint: true,
        //innerSize: 0,
        legendType: 'point',
        marker: null, // point options are specified in the base options
        size: null,
        showInLegend: false,
        slicedOffset: 10,
        states: {
            hover: {
                brightness: 0.1,
                shadow: false
            }
        },
        stickyTracking: false,
        tooltip: {
            followPointer: true
        }
    });

    /**
     * Extended point object for pies
     */
    var PiePoint = extendClass(Point, {
        /**
         * Initiate the pie slice
         */
        init: function () {

            Point.prototype.init.apply(this, arguments);

            var point = this,
                toggleSlice;

            point.name = pick(point.name, 'Slice');

            // add event listener for select
            toggleSlice = function (e) {
                point.slice(e.type === 'select');
            };
            addEvent(point, 'select', toggleSlice);
            addEvent(point, 'unselect', toggleSlice);

            return point;
        },

        /**
         * Toggle the visibility of the pie slice
         * @param {Boolean} vis Whether to show the slice or not. If undefined, the
         *    visibility is toggled
         */
        setVisible: function (vis, redraw) {
            var point = this,
                series = point.series,
                chart = series.chart,
                ignoreHiddenPoint = series.options.ignoreHiddenPoint;

            redraw = pick(redraw, ignoreHiddenPoint);

            if (vis !== point.visible) {

                // If called without an argument, toggle visibility
                point.visible = point.options.visible = vis = vis === UNDEFINED ? !point.visible : vis;
                series.options.data[inArray(point, series.data)] = point.options; // update userOptions.data

                // Show and hide associated elements. This is performed regardless of redraw or not,
                // because chart.redraw only handles full series.
                each(['graphic', 'dataLabel', 'connector', 'shadowGroup'], function (key) {
                    if (point[key]) {
                        point[key][vis ? 'show' : 'hide'](true);
                    }
                });

                if (point.legendItem) {
                    chart.legend.colorizeItem(point, vis);
                }

                // #4170, hide halo after hiding point
                if (!vis && point.state === 'hover') {
                    point.setState('');
                }

                // Handle ignore hidden slices
                if (ignoreHiddenPoint) {
                    series.isDirty = true;
                }

                if (redraw) {
                    chart.redraw();
                }
            }
        },

        /**
         * Set or toggle whether the slice is cut out from the pie
         * @param {Boolean} sliced When undefined, the slice state is toggled
         * @param {Boolean} redraw Whether to redraw the chart. True by default.
         */
        slice: function (sliced, redraw, animation) {
            var point = this,
                series = point.series,
                chart = series.chart,
                translation;

            setAnimation(animation, chart);

            // redraw is true by default
            redraw = pick(redraw, true);

            // if called without an argument, toggle
            point.sliced = point.options.sliced = sliced = defined(sliced) ? sliced : !point.sliced;
            series.options.data[inArray(point, series.data)] = point.options; // update userOptions.data

            translation = sliced ? point.slicedTranslation : {
                translateX: 0,
                translateY: 0
            };

            point.graphic.animate(translation);

            if (point.shadowGroup) {
                point.shadowGroup.animate(translation);
            }

        },

        haloPath: function (size) {
            var shapeArgs = this.shapeArgs,
                chart = this.series.chart;

            return this.sliced || !this.visible ? [] : this.series.chart.renderer.symbols.arc(chart.plotLeft + shapeArgs.x, chart.plotTop + shapeArgs.y, shapeArgs.r + size, shapeArgs.r + size, {
                innerR: this.shapeArgs.r,
                start: shapeArgs.start,
                end: shapeArgs.end
            });
        }
    });

    /**
     * The Pie series class
     */
    var PieSeries = {
        type: 'pie',
        isCartesian: false,
        pointClass: PiePoint,
        requireSorting: false,
        directTouch: true,
        noSharedTooltip: true,
        trackerGroups: ['group', 'dataLabelsGroup'],
        axisTypes: [],
        pointAttrToOptions: { // mapping between SVG attributes and the corresponding options
            stroke: 'borderColor',
            'stroke-width': 'borderWidth',
            fill: 'color'
        },

        /**
         * Animate the pies in
         */
        animate: function (init) {
            var series = this,
                points = series.points,
                startAngleRad = series.startAngleRad;

            if (!init) {
                each(points, function (point) {
                    var graphic = point.graphic,
                        args = point.shapeArgs;

                    if (graphic) {
                        // start values
                        graphic.attr({
                            r: point.startR || (series.center[3] / 2), // animate from inner radius (#779)
                            start: startAngleRad,
                            end: startAngleRad
                        });

                        // animate
                        graphic.animate({
                            r: args.r,
                            start: args.start,
                            end: args.end
                        }, series.options.animation);
                    }
                });

                // delete this function to allow it only once
                series.animate = null;
            }
        },

        /**
         * Recompute total chart sum and update percentages of points.
         */
        updateTotals: function () {
            var i,
                total = 0,
                points = this.points,
                len = points.length,
                point,
                ignoreHiddenPoint = this.options.ignoreHiddenPoint;

            // Get the total sum
            for (i = 0; i < len; i++) {
                point = points[i];
                total += (ignoreHiddenPoint && !point.visible) ? 0 : point.y;
            }
            this.total = total;

            // Set each point's properties
            for (i = 0; i < len; i++) {
                point = points[i];
                point.percentage = (total > 0 && (point.visible || !ignoreHiddenPoint)) ? point.y / total * 100 : 0;
                point.total = total;
            }
        },

        /**
         * Extend the generatePoints method by adding total and percentage properties to each point
         */
        generatePoints: function () {
            Series.prototype.generatePoints.call(this);
            this.updateTotals();
        },

        /**
         * Do translation for pie slices
         */
        translate: function (positions) {
            this.generatePoints();

            var series = this,
                cumulative = 0,
                precision = 1000, // issue #172
                options = series.options,
                slicedOffset = options.slicedOffset,
                connectorOffset = slicedOffset + options.borderWidth,
                start,
                end,
                angle,
                startAngle = options.startAngle || 0,
                startAngleRad = series.startAngleRad = mathPI / 180 * (startAngle - 90),
                endAngleRad = series.endAngleRad = mathPI / 180 * ((pick(options.endAngle, startAngle + 360)) - 90),
                circ = endAngleRad - startAngleRad, //2 * mathPI,
                points = series.points,
                radiusX, // the x component of the radius vector for a given point
                radiusY,
                labelDistance = options.dataLabels.distance,
                ignoreHiddenPoint = options.ignoreHiddenPoint,
                i,
                len = points.length,
                point;

            // Get positions - either an integer or a percentage string must be given.
            // If positions are passed as a parameter, we're in a recursive loop for adjusting
            // space for data labels.
            if (!positions) {
                series.center = positions = series.getCenter();
            }

            // utility for getting the x value from a given y, used for anticollision logic in data labels
            series.getX = function (y, left) {

                angle = math.asin(mathMin((y - positions[1]) / (positions[2] / 2 + labelDistance), 1));

                return positions[0] +
                    (left ? -1 : 1) *
                    (mathCos(angle) * (positions[2] / 2 + labelDistance));
            };

            // Calculate the geometry for each point
            for (i = 0; i < len; i++) {

                point = points[i];

                // set start and end angle
                start = startAngleRad + (cumulative * circ);
                if (!ignoreHiddenPoint || point.visible) {
                    cumulative += point.percentage / 100;
                }
                end = startAngleRad + (cumulative * circ);

                // set the shape
                point.shapeType = 'arc';
                point.shapeArgs = {
                    x: positions[0],
                    y: positions[1],
                    r: positions[2] / 2,
                    innerR: positions[3] / 2,
                    start: mathRound(start * precision) / precision,
                    end: mathRound(end * precision) / precision
                };

                // The angle must stay within -90 and 270 (#2645)
                angle = (end + start) / 2;
                if (angle > 1.5 * mathPI) {
                    angle -= 2 * mathPI;
                } else if (angle < -mathPI / 2) {
                    angle += 2 * mathPI;
                }

                // Center for the sliced out slice
                point.slicedTranslation = {
                    translateX: mathRound(mathCos(angle) * slicedOffset),
                    translateY: mathRound(mathSin(angle) * slicedOffset)
                };

                // set the anchor point for tooltips
                radiusX = mathCos(angle) * positions[2] / 2;
                radiusY = mathSin(angle) * positions[2] / 2;
                point.tooltipPos = [
                    positions[0] + radiusX * 0.7,
                    positions[1] + radiusY * 0.7
                ];

                point.half = angle < -mathPI / 2 || angle > mathPI / 2 ? 1 : 0;
                point.angle = angle;

                // set the anchor point for data labels
                connectorOffset = mathMin(connectorOffset, labelDistance / 2); // #1678
                point.labelPos = [
                    positions[0] + radiusX + mathCos(angle) * labelDistance, // first break of connector
                    positions[1] + radiusY + mathSin(angle) * labelDistance, // a/a
                    positions[0] + radiusX + mathCos(angle) * connectorOffset, // second break, right outside pie
                    positions[1] + radiusY + mathSin(angle) * connectorOffset, // a/a
                    positions[0] + radiusX, // landing point for connector
                    positions[1] + radiusY, // a/a
                    labelDistance < 0 ? // alignment
                        'center' :
                        point.half ? 'right' : 'left', // alignment
                    angle // center angle
                ];

            }
        },

        drawGraph: null,

        /**
         * Draw the data points
         */
        drawPoints: function () {
            var series = this,
                chart = series.chart,
                renderer = chart.renderer,
                groupTranslation,
                //center,
                graphic,
                //group,
                shadow = series.options.shadow,
                shadowGroup,
                pointAttr,
                shapeArgs,
                attr;

            if (shadow && !series.shadowGroup) {
                series.shadowGroup = renderer.g('shadow')
                    .add(series.group);
            }

            // draw the slices
            each(series.points, function (point) {
                if (point.y !== null) {
                    graphic = point.graphic;
                    shapeArgs = point.shapeArgs;
                    shadowGroup = point.shadowGroup;
                    pointAttr = point.pointAttr[point.selected ? SELECT_STATE : NORMAL_STATE];
                    if (!pointAttr.stroke) {
                        pointAttr.stroke = pointAttr.fill;
                    }

                    // put the shadow behind all points
                    if (shadow && !shadowGroup) {
                        shadowGroup = point.shadowGroup = renderer.g('shadow')
                            .add(series.shadowGroup);
                    }

                    // if the point is sliced, use special translation, else use plot area traslation
                    groupTranslation = point.sliced ? point.slicedTranslation : {
                        translateX: 0,
                        translateY: 0
                    };

                    //group.translate(groupTranslation[0], groupTranslation[1]);
                    if (shadowGroup) {
                        shadowGroup.attr(groupTranslation);
                    }

                    // draw the slice
                    if (graphic) {
                        graphic
                            .setRadialReference(series.center)
                            .attr(pointAttr)
                            .animate(extend(shapeArgs, groupTranslation));
                    } else {
                        attr = { 'stroke-linejoin': 'round' };
                        if (!point.visible) {
                            attr.visibility = 'hidden';
                        }

                        point.graphic = graphic = renderer[point.shapeType](shapeArgs)
                            .setRadialReference(series.center)
                            .attr(pointAttr)
                            .attr(attr)
                            .attr(groupTranslation)
                            .add(series.group)
                            .shadow(shadow, shadowGroup);
                    }
                }
            });

        },


        searchPoint: noop,

        /**
         * Utility for sorting data labels
         */
        sortByAngle: function (points, sign) {
            points.sort(function (a, b) {
                return a.angle !== undefined && (b.angle - a.angle) * sign;
            });
        },

        /**
         * Use a simple symbol from LegendSymbolMixin
         */
        drawLegendSymbol: LegendSymbolMixin.drawRectangle,

        /**
         * Use the getCenter method from drawLegendSymbol
         */
        getCenter: CenteredSeriesMixin.getCenter,

        /**
         * Pies don't have point marker symbols
         */
        getSymbol: noop

    };
    PieSeries = extendClass(Series, PieSeries);
    seriesTypes.pie = PieSeries;

    /**
     * Draw the data labels
     */
    Series.prototype.drawDataLabels = function () {

        var series = this,
            seriesOptions = series.options,
            cursor = seriesOptions.cursor,
            options = seriesOptions.dataLabels,
            points = series.points,
            pointOptions,
            generalOptions,
            hasRendered = series.hasRendered || 0,
            str,
            dataLabelsGroup,
            defer = pick(options.defer, true),
            renderer = series.chart.renderer;

        if (options.enabled || series._hasPointLabels) {

            // Process default alignment of data labels for columns
            if (series.dlProcessOptions) {
                series.dlProcessOptions(options);
            }

            // Create a separate group for the data labels to avoid rotation
            dataLabelsGroup = series.plotGroup(
                'dataLabelsGroup',
                'data-labels',
                defer && !hasRendered ? 'hidden' : 'visible', // #5133
                options.zIndex || 6
            );

            if (defer) {
                dataLabelsGroup.attr({ opacity: +hasRendered }); // #3300
                if (!hasRendered) {
                    addEvent(series, 'afterAnimate', function () {
                        if (series.visible) { // #3023, #3024
                            dataLabelsGroup.show();
                        }
                        dataLabelsGroup[seriesOptions.animation ? 'animate' : 'attr']({ opacity: 1 }, { duration: 200 });
                    });
                }
            }

            // Make the labels for each point
            generalOptions = options;
            each(points, function (point) {

                var enabled,
                    dataLabel = point.dataLabel,
                    labelConfig,
                    attr,
                    name,
                    rotation,
                    connector = point.connector,
                    isNew = true,
                    style,
                    moreStyle = {};

                // Determine if each data label is enabled
                pointOptions = point.dlOptions || (point.options && point.options.dataLabels); // dlOptions is used in treemaps
                enabled = pick(pointOptions && pointOptions.enabled, generalOptions.enabled) && point.y !== null; // #2282, #4641


                // If the point is outside the plot area, destroy it. #678, #820
                if (dataLabel && !enabled) {
                    point.dataLabel = dataLabel.destroy();

                // Individual labels are disabled if the are explicitly disabled
                // in the point options, or if they fall outside the plot area.
                } else if (enabled) {

                    // Create individual options structure that can be extended without
                    // affecting others
                    options = merge(generalOptions, pointOptions);
                    style = options.style;

                    rotation = options.rotation;

                    // Get the string
                    labelConfig = point.getLabelConfig();
                    str = options.format ?
                        format(options.format, labelConfig) :
                        options.formatter.call(labelConfig, options);

                    // Determine the color
                    style.color = pick(options.color, style.color, series.color, 'black');


                    // update existing label
                    if (dataLabel) {

                        if (defined(str)) {
                            dataLabel
                                .attr({
                                    text: str
                                });
                            isNew = false;

                        } else { // #1437 - the label is shown conditionally
                            point.dataLabel = dataLabel = dataLabel.destroy();
                            if (connector) {
                                point.connector = connector.destroy();
                            }
                        }

                    // create new label
                    } else if (defined(str)) {
                        attr = {
                            //align: align,
                            fill: options.backgroundColor,
                            stroke: options.borderColor,
                            'stroke-width': options.borderWidth,
                            r: options.borderRadius || 0,
                            rotation: rotation,
                            padding: options.padding,
                            zIndex: 1
                        };

                        // Get automated contrast color
                        if (style.color === 'contrast') {
                            moreStyle.color = options.inside || options.distance < 0 || !!seriesOptions.stacking ?
                                renderer.getContrast(point.color || series.color) :
                                '#000000';
                        }
                        if (cursor) {
                            moreStyle.cursor = cursor;
                        }


                        // Remove unused attributes (#947)
                        for (name in attr) {
                            if (attr[name] === UNDEFINED) {
                                delete attr[name];
                            }
                        }

                        dataLabel = point.dataLabel = renderer[rotation ? 'text' : 'label']( // labels don't support rotation
                            str,
                            0,
                            -9999,
                            options.shape,
                            null,
                            null,
                            options.useHTML
                        )
                        .attr(attr)
                        .css(extend(style, moreStyle))
                        .add(dataLabelsGroup)
                        .shadow(options.shadow);

                    }

                    if (dataLabel) {
                        // Now the data label is created and placed at 0,0, so we need to align it
                        series.alignDataLabel(point, dataLabel, options, null, isNew);
                    }
                }
            });
        }
    };

    /**
     * Align each individual data label
     */
    Series.prototype.alignDataLabel = function (point, dataLabel, options, alignTo, isNew) {
        var chart = this.chart,
            inverted = chart.inverted,
            plotX = pick(point.plotX, -9999),
            plotY = pick(point.plotY, -9999),
            bBox = dataLabel.getBBox(),
            baseline = chart.renderer.fontMetrics(options.style.fontSize).b,
            rotation = options.rotation,
            normRotation,
            negRotation,
            align = options.align,
            rotCorr, // rotation correction
            // Math.round for rounding errors (#2683), alignTo to allow column labels (#2700)
            visible = this.visible && (point.series.forceDL || chart.isInsidePlot(plotX, mathRound(plotY), inverted) ||
                (alignTo && chart.isInsidePlot(plotX, inverted ? alignTo.x + 1 : alignTo.y + alignTo.height - 1, inverted))),
            alignAttr, // the final position;
            justify = pick(options.overflow, 'justify') === 'justify';

        if (visible) {

            // The alignment box is a singular point
            alignTo = extend({
                x: inverted ? chart.plotWidth - plotY : plotX,
                y: mathRound(inverted ? chart.plotHeight - plotX : plotY),
                width: 0,
                height: 0
            }, alignTo);

            // Add the text size for alignment calculation
            extend(options, {
                width: bBox.width,
                height: bBox.height
            });

            // Allow a hook for changing alignment in the last moment, then do the alignment
            if (rotation) {
                justify = false; // Not supported for rotated text
                rotCorr = chart.renderer.rotCorr(baseline, rotation); // #3723
                alignAttr = {
                    x: alignTo.x + options.x + alignTo.width / 2 + rotCorr.x,
                    y: alignTo.y + options.y + { top: 0, middle: 0.5, bottom: 1 }[options.verticalAlign] * alignTo.height
                };
                dataLabel[isNew ? 'attr' : 'animate'](alignAttr)
                    .attr({ // #3003
                        align: align
                    });

                // Compensate for the rotated label sticking out on the sides
                normRotation = (rotation + 720) % 360;
                negRotation = normRotation > 180 && normRotation < 360;

                if (align === 'left') {
                    alignAttr.y -= negRotation ? bBox.height : 0;
                } else if (align === 'center') {
                    alignAttr.x -= bBox.width / 2;
                    alignAttr.y -= bBox.height / 2;
                } else if (align === 'right') {
                    alignAttr.x -= bBox.width;
                    alignAttr.y -= negRotation ? 0 : bBox.height;
                }
            

            } else {
                dataLabel.align(options, null, alignTo);
                alignAttr = dataLabel.alignAttr;
            }

            // Handle justify or crop
            if (justify) {
                this.justifyDataLabel(dataLabel, options, alignAttr, bBox, alignTo, isNew);
            
            // Now check that the data label is within the plot area
            } else if (pick(options.crop, true)) {
                visible = chart.isInsidePlot(alignAttr.x, alignAttr.y) && chart.isInsidePlot(alignAttr.x + bBox.width, alignAttr.y + bBox.height);
            }

            // When we're using a shape, make it possible with a connector or an arrow pointing to thie point
            if (options.shape && !rotation) {
                dataLabel.attr({
                    anchorX: point.plotX,
                    anchorY: point.plotY
                });
            }
        }

        // Show or hide based on the final aligned position
        if (!visible) {
            stop(dataLabel);
            dataLabel.attr({ y: -9999 });
            dataLabel.placed = false; // don't animate back in
        }

    };

    /**
     * If data labels fall partly outside the plot area, align them back in, in a way that
     * doesn't hide the point.
     */
    Series.prototype.justifyDataLabel = function (dataLabel, options, alignAttr, bBox, alignTo, isNew) {
        var chart = this.chart,
            align = options.align,
            verticalAlign = options.verticalAlign,
            off,
            justified,
            padding = dataLabel.box ? 0 : (dataLabel.padding || 0);

        // Off left
        off = alignAttr.x + padding;
        if (off < 0) {
            if (align === 'right') {
                options.align = 'left';
            } else {
                options.x = -off;
            }
            justified = true;
        }

        // Off right
        off = alignAttr.x + bBox.width - padding;
        if (off > chart.plotWidth) {
            if (align === 'left') {
                options.align = 'right';
            } else {
                options.x = chart.plotWidth - off;
            }
            justified = true;
        }

        // Off top
        off = alignAttr.y + padding;
        if (off < 0) {
            if (verticalAlign === 'bottom') {
                options.verticalAlign = 'top';
            } else {
                options.y = -off;
            }
            justified = true;
        }

        // Off bottom
        off = alignAttr.y + bBox.height - padding;
        if (off > chart.plotHeight) {
            if (verticalAlign === 'top') {
                options.verticalAlign = 'bottom';
            } else {
                options.y = chart.plotHeight - off;
            }
            justified = true;
        }

        if (justified) {
            dataLabel.placed = !isNew;
            dataLabel.align(options, null, alignTo);
        }
    };

    /**
     * Override the base drawDataLabels method by pie specific functionality
     */
    if (seriesTypes.pie) {
        seriesTypes.pie.prototype.drawDataLabels = function () {
            var series = this,
                data = series.data,
                point,
                chart = series.chart,
                options = series.options.dataLabels,
                connectorPadding = pick(options.connectorPadding, 10),
                connectorWidth = pick(options.connectorWidth, 1),
                plotWidth = chart.plotWidth,
                plotHeight = chart.plotHeight,
                connector,
                connectorPath,
                softConnector = pick(options.softConnector, true),
                distanceOption = options.distance,
                seriesCenter = series.center,
                radius = seriesCenter[2] / 2,
                centerY = seriesCenter[1],
                outside = distanceOption > 0,
                dataLabel,
                dataLabelWidth,
                labelPos,
                labelHeight,
                halves = [// divide the points into right and left halves for anti collision
                    [], // right
                    []  // left
                ],
                x,
                y,
                visibility,
                rankArr,
                i,
                j,
                overflow = [0, 0, 0, 0], // top, right, bottom, left
                sort = function (a, b) {
                    return b.y - a.y;
                };

            // get out if not enabled
            if (!series.visible || (!options.enabled && !series._hasPointLabels)) {
                return;
            }

            // run parent method
            Series.prototype.drawDataLabels.apply(series);

            each(data, function (point) {
                if (point.dataLabel && point.visible) { // #407, #2510

                    // Arrange points for detection collision
                    halves[point.half].push(point);

                    // Reset positions (#4905)
                    point.dataLabel._pos = null;
                }
            });

            /* Loop over the points in each half, starting from the top and bottom
             * of the pie to detect overlapping labels.
             */
            i = 2;
            while (i--) {

                var slots = [],
                    slotsLength,
                    usedSlots = [],
                    points = halves[i],
                    pos,
                    bottom,
                    length = points.length,
                    slotIndex;

                if (!length) {
                    continue;
                }

                // Sort by angle
                series.sortByAngle(points, i - 0.5);

                // Assume equal label heights on either hemisphere (#2630)
                j = labelHeight = 0;
                while (!labelHeight && points[j]) { // #1569
                    labelHeight = points[j] && points[j].dataLabel && (points[j].dataLabel.getBBox().height || 21); // 21 is for #968
                    j++;
                }

                // Only do anti-collision when we are outside the pie and have connectors (#856)
                if (distanceOption > 0) {

                    // Build the slots
                    bottom = mathMin(centerY + radius + distanceOption, chart.plotHeight);
                    for (pos = mathMax(0, centerY - radius - distanceOption); pos <= bottom; pos += labelHeight) {
                        slots.push(pos);
                    }
                    slotsLength = slots.length;


                    /* Visualize the slots
                    if (!series.slotElements) {
                        series.slotElements = [];
                    }
                    if (i === 1) {
                        series.slotElements.forEach(function (elem) {
                            elem.destroy();
                        });
                        series.slotElements.length = 0;
                    }

                    slots.forEach(function (pos, no) {
                        var slotX = series.getX(pos, i) + chart.plotLeft - (i ? 100 : 0),
                            slotY = pos + chart.plotTop;

                        if (isNumber(slotX)) {
                            series.slotElements.push(chart.renderer.rect(slotX, slotY - 7, 100, labelHeight, 1)
                                .attr({
                                    'stroke-width': 1,
                                    stroke: 'silver',
                                    fill: 'rgba(0,0,255,0.1)'
                                })
                                .add());
                            series.slotElements.push(chart.renderer.text('Slot '+ no, slotX, slotY + 4)
                                .attr({
                                    fill: 'silver'
                                }).add());
                        }
                    });
                    // */

                    // if there are more values than available slots, remove lowest values
                    if (length > slotsLength) {
                        // create an array for sorting and ranking the points within each quarter
                        rankArr = [].concat(points);
                        rankArr.sort(sort);
                        j = length;
                        while (j--) {
                            rankArr[j].rank = j;
                        }
                        j = length;
                        while (j--) {
                            if (points[j].rank >= slotsLength) {
                                points.splice(j, 1);
                            }
                        }
                        length = points.length;
                    }

                    // The label goes to the nearest open slot, but not closer to the edge than
                    // the label's index.
                    for (j = 0; j < length; j++) {

                        point = points[j];
                        labelPos = point.labelPos;

                        var closest = 9999,
                            distance,
                            slotI;

                        // find the closest slot index
                        for (slotI = 0; slotI < slotsLength; slotI++) {
                            distance = mathAbs(slots[slotI] - labelPos[1]);
                            if (distance < closest) {
                                closest = distance;
                                slotIndex = slotI;
                            }
                        }

                        // if that slot index is closer to the edges of the slots, move it
                        // to the closest appropriate slot
                        if (slotIndex < j && slots[j] !== null) { // cluster at the top
                            slotIndex = j;
                        } else if (slotsLength  < length - j + slotIndex && slots[j] !== null) { // cluster at the bottom
                            slotIndex = slotsLength - length + j;
                            while (slots[slotIndex] === null) { // make sure it is not taken
                                slotIndex++;
                            }
                        } else {
                            // Slot is taken, find next free slot below. In the next run, the next slice will find the
                            // slot above these, because it is the closest one
                            while (slots[slotIndex] === null) { // make sure it is not taken
                                slotIndex++;
                            }
                        }

                        usedSlots.push({ i: slotIndex, y: slots[slotIndex] });
                        slots[slotIndex] = null; // mark as taken
                    }
                    // sort them in order to fill in from the top
                    usedSlots.sort(sort);
                }

                // now the used slots are sorted, fill them up sequentially
                for (j = 0; j < length; j++) {

                    var slot, naturalY;

                    point = points[j];
                    labelPos = point.labelPos;
                    dataLabel = point.dataLabel;
                    visibility = point.visible === false ? HIDDEN : 'inherit';
                    naturalY = labelPos[1];

                    if (distanceOption > 0) {
                        slot = usedSlots.pop();
                        slotIndex = slot.i;

                        // if the slot next to currrent slot is free, the y value is allowed
                        // to fall back to the natural position
                        y = slot.y;
                        if ((naturalY > y && slots[slotIndex + 1] !== null) ||
                                (naturalY < y &&  slots[slotIndex - 1] !== null)) {
                            y = mathMin(mathMax(0, naturalY), chart.plotHeight);
                        }

                    } else {
                        y = naturalY;
                    }

                    // get the x - use the natural x position for first and last slot, to prevent the top
                    // and botton slice connectors from touching each other on either side
                    x = options.justify ?
                        seriesCenter[0] + (i ? -1 : 1) * (radius + distanceOption) :
                        series.getX(y === centerY - radius - distanceOption || y === centerY + radius + distanceOption ? naturalY : y, i);


                    // Record the placement and visibility
                    dataLabel._attr = {
                        visibility: visibility,
                        align: labelPos[6]
                    };
                    dataLabel._pos = {
                        x: x + options.x +
                            ({ left: connectorPadding, right: -connectorPadding }[labelPos[6]] || 0),
                        y: y + options.y - 10 // 10 is for the baseline (label vs text)
                    };
                    dataLabel.connX = x;
                    dataLabel.connY = y;


                    // Detect overflowing data labels
                    if (this.options.size === null) {
                        dataLabelWidth = dataLabel.width;
                        // Overflow left
                        if (x - dataLabelWidth < connectorPadding) {
                            overflow[3] = mathMax(mathRound(dataLabelWidth - x + connectorPadding), overflow[3]);

                        // Overflow right
                        } else if (x + dataLabelWidth > plotWidth - connectorPadding) {
                            overflow[1] = mathMax(mathRound(x + dataLabelWidth - plotWidth + connectorPadding), overflow[1]);
                        }

                        // Overflow top
                        if (y - labelHeight / 2 < 0) {
                            overflow[0] = mathMax(mathRound(-y + labelHeight / 2), overflow[0]);

                        // Overflow left
                        } else if (y + labelHeight / 2 > plotHeight) {
                            overflow[2] = mathMax(mathRound(y + labelHeight / 2 - plotHeight), overflow[2]);
                        }
                    }
                } // for each point
            } // for each half

            // Do not apply the final placement and draw the connectors until we have verified
            // that labels are not spilling over.
            if (arrayMax(overflow) === 0 || this.verifyDataLabelOverflow(overflow)) {

                // Place the labels in the final position
                this.placeDataLabels();

                // Draw the connectors
                if (outside && connectorWidth) {
                    each(this.points, function (point) {
                        connector = point.connector;
                        labelPos = point.labelPos;
                        dataLabel = point.dataLabel;

                        if (dataLabel && dataLabel._pos && point.visible) {
                            visibility = dataLabel._attr.visibility;
                            x = dataLabel.connX;
                            y = dataLabel.connY;
                            connectorPath = softConnector ? [
                                M,
                                x + (labelPos[6] === 'left' ? 5 : -5), y, // end of the string at the label
                                'C',
                                x, y, // first break, next to the label
                                2 * labelPos[2] - labelPos[4], 2 * labelPos[3] - labelPos[5],
                                labelPos[2], labelPos[3], // second break
                                L,
                                labelPos[4], labelPos[5] // base
                            ] : [
                                M,
                                x + (labelPos[6] === 'left' ? 5 : -5), y, // end of the string at the label
                                L,
                                labelPos[2], labelPos[3], // second break
                                L,
                                labelPos[4], labelPos[5] // base
                            ];

                            if (connector) {
                                connector.animate({ d: connectorPath });
                                connector.attr('visibility', visibility);

                            } else {
                                point.connector = connector = series.chart.renderer.path(connectorPath).attr({
                                    'stroke-width': connectorWidth,
                                    stroke: options.connectorColor || point.color || '#606060',
                                    visibility: visibility
                                    //zIndex: 0 // #2722 (reversed)
                                })
                                .add(series.dataLabelsGroup);
                            }
                        } else if (connector) {
                            point.connector = connector.destroy();
                        }
                    });
                }
            }
        };
        /**
         * Perform the final placement of the data labels after we have verified that they
         * fall within the plot area.
         */
        seriesTypes.pie.prototype.placeDataLabels = function () {
            each(this.points, function (point) {
                var dataLabel = point.dataLabel,
                    _pos;

                if (dataLabel && point.visible) {
                    _pos = dataLabel._pos;
                    if (_pos) {
                        dataLabel.attr(dataLabel._attr);
                        dataLabel[dataLabel.moved ? 'animate' : 'attr'](_pos);
                        dataLabel.moved = true;
                    } else if (dataLabel) {
                        dataLabel.attr({ y: -9999 });
                    }
                }
            });
        };

        seriesTypes.pie.prototype.alignDataLabel =  noop;

        /**
         * Verify whether the data labels are allowed to draw, or we should run more translation and data
         * label positioning to keep them inside the plot area. Returns true when data labels are ready
         * to draw.
         */
        seriesTypes.pie.prototype.verifyDataLabelOverflow = function (overflow) {

            var center = this.center,
                options = this.options,
                centerOption = options.center,
                minSize = options.minSize || 80,
                newSize = minSize,
                ret;

            // Handle horizontal size and center
            if (centerOption[0] !== null) { // Fixed center
                newSize = mathMax(center[2] - mathMax(overflow[1], overflow[3]), minSize);

            } else { // Auto center
                newSize = mathMax(
                    center[2] - overflow[1] - overflow[3], // horizontal overflow
                    minSize
                );
                center[0] += (overflow[3] - overflow[1]) / 2; // horizontal center
            }

            // Handle vertical size and center
            if (centerOption[1] !== null) { // Fixed center
                newSize = mathMax(mathMin(newSize, center[2] - mathMax(overflow[0], overflow[2])), minSize);

            } else { // Auto center
                newSize = mathMax(
                    mathMin(
                        newSize,
                        center[2] - overflow[0] - overflow[2] // vertical overflow
                    ),
                    minSize
                );
                center[1] += (overflow[0] - overflow[2]) / 2; // vertical center
            }

            // If the size must be decreased, we need to run translate and drawDataLabels again
            if (newSize < center[2]) {
                center[2] = newSize;
                center[3] = Math.min(relativeLength(options.innerSize || 0, newSize), newSize); // #3632
                this.translate(center);
            
                if (this.drawDataLabels) {
                    this.drawDataLabels();
                }
            // Else, return true to indicate that the pie and its labels is within the plot area
            } else {
                ret = true;
            }
            return ret;
        };
    }

    if (seriesTypes.column) {

        /**
         * Override the basic data label alignment by adjusting for the position of the column
         */
        seriesTypes.column.prototype.alignDataLabel = function (point, dataLabel, options,  alignTo, isNew) {
            var inverted = this.chart.inverted,
                series = point.series,
                dlBox = point.dlBox || point.shapeArgs, // data label box for alignment
                below = pick(point.below, point.plotY > pick(this.translatedThreshold, series.yAxis.len)), // point.below is used in range series
                inside = pick(options.inside, !!this.options.stacking), // draw it inside the box?
                overshoot;

            // Align to the column itself, or the top of it
            if (dlBox) { // Area range uses this method but not alignTo
                alignTo = merge(dlBox);

                if (alignTo.y < 0) {
                    alignTo.height += alignTo.y;
                    alignTo.y = 0;
                }
                overshoot = alignTo.y + alignTo.height - series.yAxis.len;
                if (overshoot > 0) {
                    alignTo.height -= overshoot;
                }

                if (inverted) {
                    alignTo = {
                        x: series.yAxis.len - alignTo.y - alignTo.height,
                        y: series.xAxis.len - alignTo.x - alignTo.width,
                        width: alignTo.height,
                        height: alignTo.width
                    };
                }

                // Compute the alignment box
                if (!inside) {
                    if (inverted) {
                        alignTo.x += below ? 0 : alignTo.width;
                        alignTo.width = 0;
                    } else {
                        alignTo.y += below ? alignTo.height : 0;
                        alignTo.height = 0;
                    }
                }
            }


            // When alignment is undefined (typically columns and bars), display the individual
            // point below or above the point depending on the threshold
            options.align = pick(
                options.align,
                !inverted || inside ? 'center' : below ? 'right' : 'left'
            );
            options.verticalAlign = pick(
                options.verticalAlign,
                inverted || inside ? 'middle' : below ? 'top' : 'bottom'
            );

            // Call the parent method
            Series.prototype.alignDataLabel.call(this, point, dataLabel, options, alignTo, isNew);
        };
    }



    /**
     * Highcharts module to hide overlapping data labels. This module is included in Highcharts.
     */
    (function (H) {
        var Chart = H.Chart,
            each = H.each,
            pick = H.pick,
            addEvent = H.addEvent;

        // Collect potensial overlapping data labels. Stack labels probably don't need to be 
        // considered because they are usually accompanied by data labels that lie inside the columns.
        Chart.prototype.callbacks.push(function (chart) {
            function collectAndHide() {
                var labels = [];

                each(chart.series, function (series) {
                    var dlOptions = series.options.dataLabels,
                        collections = series.dataLabelCollections || ['dataLabel']; // Range series have two collections
                    if ((dlOptions.enabled || series._hasPointLabels) && !dlOptions.allowOverlap && series.visible) { // #3866
                        each(collections, function (coll) {
                            each(series.points, function (point) {
                                if (point[coll]) {
                                    point[coll].labelrank = pick(point.labelrank, point.shapeArgs && point.shapeArgs.height); // #4118
                                    labels.push(point[coll]);
                                }
                            });
                        });
                    }
                });
                chart.hideOverlappingLabels(labels);
            }

            // Do it now ...
            collectAndHide();

            // ... and after each chart redraw
            addEvent(chart, 'redraw', collectAndHide);

        });

        /**
         * Hide overlapping labels. Labels are moved and faded in and out on zoom to provide a smooth 
         * visual imression.
         */    
        Chart.prototype.hideOverlappingLabels = function (labels) {

            var len = labels.length,
                label,
                i,
                j,
                label1,
                label2,
                isIntersecting,
                pos1,
                pos2,
                parent1,
                parent2,
                padding,
                intersectRect = function (x1, y1, w1, h1, x2, y2, w2, h2) {
                    return !(
                        x2 > x1 + w1 ||
                        x2 + w2 < x1 ||
                        y2 > y1 + h1 ||
                        y2 + h2 < y1
                    );
                };
    
            // Mark with initial opacity
            for (i = 0; i < len; i++) {
                label = labels[i];
                if (label) {
                    label.oldOpacity = label.opacity;
                    label.newOpacity = 1;
                }
            }

            // Prevent a situation in a gradually rising slope, that each label
            // will hide the previous one because the previous one always has
            // lower rank.
            labels.sort(function (a, b) {
                return (b.labelrank || 0) - (a.labelrank || 0);
            });

            // Detect overlapping labels
            for (i = 0; i < len; i++) {
                label1 = labels[i];

                for (j = i + 1; j < len; ++j) {
                    label2 = labels[j];
                    if (label1 && label2 && label1.placed && label2.placed && label1.newOpacity !== 0 && label2.newOpacity !== 0) {
                        pos1 = label1.alignAttr;
                        pos2 = label2.alignAttr;
                        parent1 = label1.parentGroup; // Different panes have different positions
                        parent2 = label2.parentGroup;
                        padding = 2 * (label1.box ? 0 : label1.padding); // Substract the padding if no background or border (#4333)
                        isIntersecting = intersectRect(
                            pos1.x + parent1.translateX,
                            pos1.y + parent1.translateY,
                            label1.width - padding,
                            label1.height - padding,
                            pos2.x + parent2.translateX,
                            pos2.y + parent2.translateY,
                            label2.width - padding,
                            label2.height - padding
                        );

                        if (isIntersecting) {
                            (label1.labelrank < label2.labelrank ? label1 : label2).newOpacity = 0;
                        }
                    }
                }
            }

            // Hide or show
            each(labels, function (label) {
                var complete,
                    newOpacity;

                if (label) {
                    newOpacity = label.newOpacity;

                    if (label.oldOpacity !== newOpacity && label.placed) {

                        // Make sure the label is completely hidden to avoid catching clicks (#4362)
                        if (newOpacity) {
                            label.show(true);
                        } else {
                            complete = function () {
                                label.hide();
                            };
                        }

                        // Animate or set the opacity                
                        label.alignAttr.opacity = newOpacity;
                        label[label.isOld ? 'animate' : 'attr'](label.alignAttr, null, complete);
                    
                    }
                    label.isOld = true;
                }
            });
        };
    }(Highcharts));
    /**
     * TrackerMixin for points and graphs
     */

    var TrackerMixin = Highcharts.TrackerMixin = {

        drawTrackerPoint: function () {
            var series = this,
                chart = series.chart,
                pointer = chart.pointer,
                cursor = series.options.cursor,
                css = cursor && { cursor: cursor },
                onMouseOver = function (e) {
                    var target = e.target,
                        point;

                    while (target && !point) {
                        point = target.point;
                        target = target.parentNode;
                    }

                    if (point !== UNDEFINED && point !== chart.hoverPoint) { // undefined on graph in scatterchart
                        point.onMouseOver(e);
                    }
                };

            // Add reference to the point
            each(series.points, function (point) {
                if (point.graphic) {
                    point.graphic.element.point = point;
                }
                if (point.dataLabel) {
                    point.dataLabel.element.point = point;
                }
            });

            // Add the event listeners, we need to do this only once
            if (!series._hasTracking) {
                each(series.trackerGroups, function (key) {
                    if (series[key]) { // we don't always have dataLabelsGroup
                        series[key]
                            .addClass(PREFIX + 'tracker')
                            .on('mouseover', onMouseOver)
                            .on('mouseout', function (e) {
                                pointer.onTrackerMouseOut(e);
                            })
                            .css(css);
                        if (hasTouch) {
                            series[key].on('touchstart', onMouseOver);
                        }
                    }
                });
                series._hasTracking = true;
            }
        },

        /**
         * Draw the tracker object that sits above all data labels and markers to
         * track mouse events on the graph or points. For the line type charts
         * the tracker uses the same graphPath, but with a greater stroke width
         * for better control.
         */
        drawTrackerGraph: function () {
            var series = this,
                options = series.options,
                trackByArea = options.trackByArea,
                trackerPath = [].concat(trackByArea ? series.areaPath : series.graphPath),
                trackerPathLength = trackerPath.length,
                chart = series.chart,
                pointer = chart.pointer,
                renderer = chart.renderer,
                snap = chart.options.tooltip.snap,
                tracker = series.tracker,
                cursor = options.cursor,
                css = cursor && { cursor: cursor },
                i,
                onMouseOver = function () {
                    if (chart.hoverSeries !== series) {
                        series.onMouseOver();
                    }
                },
                /*
                 * Empirical lowest possible opacities for TRACKER_FILL for an element to stay invisible but clickable
                 * IE6: 0.002
                 * IE7: 0.002
                 * IE8: 0.002
                 * IE9: 0.00000000001 (unlimited)
                 * IE10: 0.0001 (exporting only)
                 * FF: 0.00000000001 (unlimited)
                 * Chrome: 0.000001
                 * Safari: 0.000001
                 * Opera: 0.00000000001 (unlimited)
                 */
                TRACKER_FILL = 'rgba(192,192,192,' + (hasSVG ? 0.0001 : 0.002) + ')';

            // Extend end points. A better way would be to use round linecaps,
            // but those are not clickable in VML.
            if (trackerPathLength && !trackByArea) {
                i = trackerPathLength + 1;
                while (i--) {
                    if (trackerPath[i] === M) { // extend left side
                        trackerPath.splice(i + 1, 0, trackerPath[i + 1] - snap, trackerPath[i + 2], L);
                    }
                    if ((i && trackerPath[i] === M) || i === trackerPathLength) { // extend right side
                        trackerPath.splice(i, 0, L, trackerPath[i - 2] + snap, trackerPath[i - 1]);
                    }
                }
            }

            // handle single points
            /*for (i = 0; i < singlePoints.length; i++) {
                singlePoint = singlePoints[i];
                trackerPath.push(M, singlePoint.plotX - snap, singlePoint.plotY,
                L, singlePoint.plotX + snap, singlePoint.plotY);
            }*/

            // draw the tracker
            if (tracker) {
                tracker.attr({ d: trackerPath });
            } else { // create

                series.tracker = renderer.path(trackerPath)
                .attr({
                    'stroke-linejoin': 'round', // #1225
                    visibility: series.visible ? VISIBLE : HIDDEN,
                    stroke: TRACKER_FILL,
                    fill: trackByArea ? TRACKER_FILL : NONE,
                    'stroke-width': options.lineWidth + (trackByArea ? 0 : 2 * snap),
                    zIndex: 2
                })
                .add(series.group);

                // The tracker is added to the series group, which is clipped, but is covered
                // by the marker group. So the marker group also needs to capture events.
                each([series.tracker, series.markerGroup], function (tracker) {
                    tracker.addClass(PREFIX + 'tracker')
                        .on('mouseover', onMouseOver)
                        .on('mouseout', function (e) {
                            pointer.onTrackerMouseOut(e);
                        })
                        .css(css);

                    if (hasTouch) {
                        tracker.on('touchstart', onMouseOver);
                    }
                });
            }
        }
    };
    /* End TrackerMixin */


    /**
     * Add tracking event listener to the series group, so the point graphics
     * themselves act as trackers
     */

    if (seriesTypes.column) {
        ColumnSeries.prototype.drawTracker = TrackerMixin.drawTrackerPoint;
    }

    if (seriesTypes.pie) {
        seriesTypes.pie.prototype.drawTracker = TrackerMixin.drawTrackerPoint;
    }

    if (seriesTypes.scatter) {
        ScatterSeries.prototype.drawTracker = TrackerMixin.drawTrackerPoint;
    }

    /*
     * Extend Legend for item events
     */
    extend(Legend.prototype, {

        setItemEvents: function (item, legendItem, useHTML, itemStyle, itemHiddenStyle) {
            var legend = this;
            // Set the events on the item group, or in case of useHTML, the item itself (#1249)
            (useHTML ? legendItem : item.legendGroup).on('mouseover', function () {
                item.setState(HOVER_STATE);
                legendItem.css(legend.options.itemHoverStyle);
            })
            .on('mouseout', function () {
                legendItem.css(item.visible ? itemStyle : itemHiddenStyle);
                item.setState();
            })
            .on('click', function (event) {
                var strLegendItemClick = 'legendItemClick',
                    fnLegendItemClick = function () {
                        if (item.setVisible) {
                            item.setVisible();
                        }
                    };

                // Pass over the click/touch event. #4.
                event = {
                    browserEvent: event
                };

                // click the name or symbol
                if (item.firePointEvent) { // point
                    item.firePointEvent(strLegendItemClick, event, fnLegendItemClick);
                } else {
                    fireEvent(item, strLegendItemClick, event, fnLegendItemClick);
                }
            });
        },

        createCheckboxForItem: function (item) {
            var legend = this;

            item.checkbox = createElement('input', {
                type: 'checkbox',
                checked: item.selected,
                defaultChecked: item.selected // required by IE7
            }, legend.options.itemCheckboxStyle, legend.chart.container);

            addEvent(item.checkbox, 'click', function (event) {
                var target = event.target;
                fireEvent(
                    item.series || item, 
                    'checkboxClick', 
                    { // #3712
                        checked: target.checked,
                        item: item
                    },
                    function () {
                        item.select();
                    }
                );
            });
        }
    });

    /*
     * Add pointer cursor to legend itemstyle in defaultOptions
     */
    defaultOptions.legend.itemStyle.cursor = 'pointer';


    /*
     * Extend the Chart object with interaction
     */

    extend(Chart.prototype, {
        /**
         * Display the zoom button
         */
        showResetZoom: function () {
            var chart = this,
                lang = defaultOptions.lang,
                btnOptions = chart.options.chart.resetZoomButton,
                theme = btnOptions.theme,
                states = theme.states,
                alignTo = btnOptions.relativeTo === 'chart' ? null : 'plotBox';

            function zoomOut() {
                chart.zoomOut();
            }

            this.resetZoomButton = chart.renderer.button(lang.resetZoom, null, null, zoomOut, theme, states && states.hover)
                .attr({
                    align: btnOptions.position.align,
                    title: lang.resetZoomTitle
                })
                .add()
                .align(btnOptions.position, false, alignTo);

        },

        /**
         * Zoom out to 1:1
         */
        zoomOut: function () {
            var chart = this;
            fireEvent(chart, 'selection', { resetSelection: true }, function () {
                chart.zoom();
            });
        },

        /**
         * Zoom into a given portion of the chart given by axis coordinates
         * @param {Object} event
         */
        zoom: function (event) {
            var chart = this,
                hasZoomed,
                pointer = chart.pointer,
                displayButton = false,
                resetZoomButton;

            // If zoom is called with no arguments, reset the axes
            if (!event || event.resetSelection) {
                each(chart.axes, function (axis) {
                    hasZoomed = axis.zoom();
                });
            } else { // else, zoom in on all axes
                each(event.xAxis.concat(event.yAxis), function (axisData) {
                    var axis = axisData.axis,
                        isXAxis = axis.isXAxis;

                    // don't zoom more than minRange
                    if (pointer[isXAxis ? 'zoomX' : 'zoomY'] || pointer[isXAxis ? 'pinchX' : 'pinchY']) {
                        hasZoomed = axis.zoom(axisData.min, axisData.max);
                        if (axis.displayBtn) {
                            displayButton = true;
                        }
                    }
                });
            }

            // Show or hide the Reset zoom button
            resetZoomButton = chart.resetZoomButton;
            if (displayButton && !resetZoomButton) {
                chart.showResetZoom();
            } else if (!displayButton && isObject(resetZoomButton)) {
                chart.resetZoomButton = resetZoomButton.destroy();
            }


            // Redraw
            if (hasZoomed) {
                chart.redraw(
                    pick(chart.options.chart.animation, event && event.animation, chart.pointCount < 100) // animation
                );
            }
        },

        /**
         * Pan the chart by dragging the mouse across the pane. This function is called
         * on mouse move, and the distance to pan is computed from chartX compared to
         * the first chartX position in the dragging operation.
         */
        pan: function (e, panning) {

            var chart = this,
                hoverPoints = chart.hoverPoints,
                doRedraw;

            // remove active points for shared tooltip
            if (hoverPoints) {
                each(hoverPoints, function (point) {
                    point.setState();
                });
            }

            each(panning === 'xy' ? [1, 0] : [1], function (isX) { // xy is used in maps
                var axis = chart[isX ? 'xAxis' : 'yAxis'][0],
                    horiz = axis.horiz,
                    mousePos = e[horiz ? 'chartX' : 'chartY'],
                    mouseDown = horiz ? 'mouseDownX' : 'mouseDownY',
                    startPos = chart[mouseDown],
                    halfPointRange = (axis.pointRange || 0) / 2,
                    extremes = axis.getExtremes(),
                    newMin = axis.toValue(startPos - mousePos, true) + halfPointRange,
                    newMax = axis.toValue(startPos + axis.len - mousePos, true) - halfPointRange,
                    goingLeft = startPos > mousePos; // #3613
            
                if (axis.series.length &&
                        (goingLeft || newMin > mathMin(extremes.dataMin, extremes.min)) &&    
                        (!goingLeft || newMax < mathMax(extremes.dataMax, extremes.max))) {
                    axis.setExtremes(newMin, newMax, false, false, { trigger: 'pan' });
                    doRedraw = true;
                }

                chart[mouseDown] = mousePos; // set new reference for next run
            });

            if (doRedraw) {
                chart.redraw(false);
            }
            css(chart.container, { cursor: 'move' });
        }
    });

    /*
     * Extend the Point object with interaction
     */
    extend(Point.prototype, {
        /**
         * Toggle the selection status of a point
         * @param {Boolean} selected Whether to select or unselect the point.
         * @param {Boolean} accumulate Whether to add to the previous selection. By default,
         *         this happens if the control key (Cmd on Mac) was pressed during clicking.
         */
        select: function (selected, accumulate) {
            var point = this,
                series = point.series,
                chart = series.chart;

            selected = pick(selected, !point.selected);

            // fire the event with the default handler
            point.firePointEvent(selected ? 'select' : 'unselect', { accumulate: accumulate }, function () {
                point.selected = point.options.selected = selected;
                series.options.data[inArray(point, series.data)] = point.options;

                point.setState(selected && SELECT_STATE);

                // unselect all other points unless Ctrl or Cmd + click
                if (!accumulate) {
                    each(chart.getSelectedPoints(), function (loopPoint) {
                        if (loopPoint.selected && loopPoint !== point) {
                            loopPoint.selected = loopPoint.options.selected = false;
                            series.options.data[inArray(loopPoint, series.data)] = loopPoint.options;
                            loopPoint.setState(NORMAL_STATE);
                            loopPoint.firePointEvent('unselect');
                        }
                    });
                }
            });
        },

        /**
         * Runs on mouse over the point
         *
         * @param {Object} e The event arguments
         * @param {Boolean} byProximity Falsy for kd points that are closest to the mouse, or to
         *        actually hovered points. True for other points in shared tooltip.
         */
        onMouseOver: function (e, byProximity) {
            var point = this,
                series = point.series,
                chart = series.chart,
                tooltip = chart.tooltip,
                hoverPoint = chart.hoverPoint;

            if (chart.hoverSeries !== series) {
                series.onMouseOver();
            }

            // set normal state to previous series
            if (hoverPoint && hoverPoint !== point) {
                hoverPoint.onMouseOut();
            }

            if (point.series) { // It may have been destroyed, #4130

                // trigger the event
                point.firePointEvent('mouseOver');

                // update the tooltip
                if (tooltip && (!tooltip.shared || series.noSharedTooltip)) {
                    tooltip.refresh(point, e);
                }

                // hover this
                point.setState(HOVER_STATE);
                if (!byProximity) {
                    chart.hoverPoint = point;
                }
            }
        },

        /**
         * Runs on mouse out from the point
         */
        onMouseOut: function () {
            var chart = this.series.chart,
                hoverPoints = chart.hoverPoints;

            this.firePointEvent('mouseOut');

            if (!hoverPoints || inArray(this, hoverPoints) === -1) { // #887, #2240
                this.setState();
                chart.hoverPoint = null;
            }
        },

        /**
         * Import events from the series' and point's options. Only do it on
         * demand, to save processing time on hovering.
         */
        importEvents: function () {
            if (!this.hasImportedEvents) {
                var point = this,
                    options = merge(point.series.options.point, point.options),
                    events = options.events,
                    eventType;

                point.events = events;

                for (eventType in events) {
                    addEvent(point, eventType, events[eventType]);
                }
                this.hasImportedEvents = true;

            }
        },

        /**
         * Set the point's state
         * @param {String} state
         */
        setState: function (state, move) {
            var point = this,
                plotX = mathFloor(point.plotX), // #4586
                plotY = point.plotY,
                series = point.series,
                stateOptions = series.options.states,
                markerOptions = defaultPlotOptions[series.type].marker && series.options.marker,
                normalDisabled = markerOptions && !markerOptions.enabled,
                markerStateOptions = markerOptions && markerOptions.states[state],
                stateDisabled = markerStateOptions && markerStateOptions.enabled === false,
                stateMarkerGraphic = series.stateMarkerGraphic,
                pointMarker = point.marker || {},
                chart = series.chart,
                radius,
                halo = series.halo,
                haloOptions,
                newSymbol,
                pointAttr;

            state = state || NORMAL_STATE; // empty string
            pointAttr = point.pointAttr[state] || series.pointAttr[state];

            if (
                    // already has this state
                    (state === point.state && !move) ||
                    // selected points don't respond to hover
                    (point.selected && state !== SELECT_STATE) ||
                    // series' state options is disabled
                    (stateOptions[state] && stateOptions[state].enabled === false) ||
                    // general point marker's state options is disabled
                    (state && (stateDisabled || (normalDisabled && markerStateOptions.enabled === false))) ||
                    // individual point marker's state options is disabled
                    (state && pointMarker.states && pointMarker.states[state] && pointMarker.states[state].enabled === false) // #1610

                ) {
                return;
            }

            // apply hover styles to the existing point
            if (point.graphic) {
                radius = markerOptions && point.graphic.symbolName && pointAttr.r;
                point.graphic.attr(merge(
                    pointAttr,
                    radius ? { // new symbol attributes (#507, #612)
                        x: plotX - radius,
                        y: plotY - radius,
                        width: 2 * radius,
                        height: 2 * radius
                    } : {}
                ));

                // Zooming in from a range with no markers to a range with markers
                if (stateMarkerGraphic) {
                    stateMarkerGraphic.hide();
                }
            } else {
                // if a graphic is not applied to each point in the normal state, create a shared
                // graphic for the hover state
                if (state && markerStateOptions) {
                    radius = markerStateOptions.radius;
                    newSymbol = pointMarker.symbol || series.symbol;

                    // If the point has another symbol than the previous one, throw away the
                    // state marker graphic and force a new one (#1459)
                    if (stateMarkerGraphic && stateMarkerGraphic.currentSymbol !== newSymbol) {
                        stateMarkerGraphic = stateMarkerGraphic.destroy();
                    }

                    // Add a new state marker graphic
                    if (!stateMarkerGraphic) {
                        if (newSymbol) {
                            series.stateMarkerGraphic = stateMarkerGraphic = chart.renderer.symbol(
                                newSymbol,
                                plotX - radius,
                                plotY - radius,
                                2 * radius,
                                2 * radius
                            )
                            .attr(pointAttr)
                            .add(series.markerGroup);
                            stateMarkerGraphic.currentSymbol = newSymbol;
                        }

                    // Move the existing graphic
                    } else {
                        stateMarkerGraphic[move ? 'animate' : 'attr']({ // #1054
                            x: plotX - radius,
                            y: plotY - radius
                        });
                    }
                }

                if (stateMarkerGraphic) {
                    stateMarkerGraphic[state && chart.isInsidePlot(plotX, plotY, chart.inverted) ? 'show' : 'hide'](); // #2450
                    stateMarkerGraphic.element.point = point; // #4310
                }
            }

            // Show me your halo
            haloOptions = stateOptions[state] && stateOptions[state].halo;
            if (haloOptions && haloOptions.size) {
                if (!halo) {
                    series.halo = halo = chart.renderer.path()
                        .add(chart.seriesGroup);
                }
                halo.attr(extend({
                    'fill': point.color || series.color,
                    'fill-opacity': haloOptions.opacity,
                    'zIndex': -1 // #4929, IE8 added halo above everything
                },
                haloOptions.attributes))[move ? 'animate' : 'attr']({
                    d: point.haloPath(haloOptions.size)
                });
            } else if (halo) {
                halo.attr({ d: [] });
            }

            point.state = state;
        },

        /**
         * Get the circular path definition for the halo
         * @param  {Number} size The radius of the circular halo
         * @returns {Array} The path definition
         */
        haloPath: function (size) {
            var series = this.series,
                chart = series.chart,
                plotBox = series.getPlotBox(),
                inverted = chart.inverted,
                plotX = Math.floor(this.plotX);

            return chart.renderer.symbols.circle(
                plotBox.translateX + (inverted ? series.yAxis.len - this.plotY : plotX) - size, 
                plotBox.translateY + (inverted ? series.xAxis.len - plotX : this.plotY) - size, 
                size * 2, 
                size * 2
            );
        }
    });

    /*
     * Extend the Series object with interaction
     */

    extend(Series.prototype, {
        /**
         * Series mouse over handler
         */
        onMouseOver: function () {
            var series = this,
                chart = series.chart,
                hoverSeries = chart.hoverSeries;

            // set normal state to previous series
            if (hoverSeries && hoverSeries !== series) {
                hoverSeries.onMouseOut();
            }

            // trigger the event, but to save processing time,
            // only if defined
            if (series.options.events.mouseOver) {
                fireEvent(series, 'mouseOver');
            }

            // hover this
            series.setState(HOVER_STATE);
            chart.hoverSeries = series;
        },

        /**
         * Series mouse out handler
         */
        onMouseOut: function () {
            // trigger the event only if listeners exist
            var series = this,
                options = series.options,
                chart = series.chart,
                tooltip = chart.tooltip,
                hoverPoint = chart.hoverPoint;

            chart.hoverSeries = null; // #182, set to null before the mouseOut event fires

            // trigger mouse out on the point, which must be in this series
            if (hoverPoint) {
                hoverPoint.onMouseOut();
            }

            // fire the mouse out event
            if (series && options.events.mouseOut) {
                fireEvent(series, 'mouseOut');
            }


            // hide the tooltip
            if (tooltip && !options.stickyTracking && (!tooltip.shared || series.noSharedTooltip)) {
                tooltip.hide();
            }

            // set normal state
            series.setState();
        },

        /**
         * Set the state of the graph
         */
        setState: function (state) {
            var series = this,
                options = series.options,
                graph = series.graph,
                stateOptions = options.states,
                lineWidth = options.lineWidth,
                attribs,
                i = 0;

            state = state || NORMAL_STATE;

            if (series.state !== state) {
                series.state = state;

                if (stateOptions[state] && stateOptions[state].enabled === false) {
                    return;
                }

                if (state) {
                    lineWidth = stateOptions[state].lineWidth || lineWidth + (stateOptions[state].lineWidthPlus || 0); // #4035
                }

                if (graph && !graph.dashstyle) { // hover is turned off for dashed lines in VML
                    attribs = {
                        'stroke-width': lineWidth
                    };
                    // use attr because animate will cause any other animation on the graph to stop
                    graph.attr(attribs);
                    while (series['zoneGraph' + i]) {
                        series['zoneGraph' + i].attr(attribs);
                        i = i + 1;
                    }
                }
            }
        },

        /**
         * Set the visibility of the graph
         *
         * @param vis {Boolean} True to show the series, false to hide. If UNDEFINED,
         *                the visibility is toggled.
         */
        setVisible: function (vis, redraw) {
            var series = this,
                chart = series.chart,
                legendItem = series.legendItem,
                showOrHide,
                ignoreHiddenSeries = chart.options.chart.ignoreHiddenSeries,
                oldVisibility = series.visible;

            // if called without an argument, toggle visibility
            series.visible = vis = series.userOptions.visible = vis === UNDEFINED ? !oldVisibility : vis;
            showOrHide = vis ? 'show' : 'hide';

            // show or hide elements
            each(['group', 'dataLabelsGroup', 'markerGroup', 'tracker'], function (key) {
                if (series[key]) {
                    series[key][showOrHide]();
                }
            });


            // hide tooltip (#1361)
            if (chart.hoverSeries === series || (chart.hoverPoint && chart.hoverPoint.series) === series) {
                series.onMouseOut();
            }


            if (legendItem) {
                chart.legend.colorizeItem(series, vis);
            }


            // rescale or adapt to resized chart
            series.isDirty = true;
            // in a stack, all other series are affected
            if (series.options.stacking) {
                each(chart.series, function (otherSeries) {
                    if (otherSeries.options.stacking && otherSeries.visible) {
                        otherSeries.isDirty = true;
                    }
                });
            }

            // show or hide linked series
            each(series.linkedSeries, function (otherSeries) {
                otherSeries.setVisible(vis, false);
            });

            if (ignoreHiddenSeries) {
                chart.isDirtyBox = true;
            }
            if (redraw !== false) {
                chart.redraw();
            }

            fireEvent(series, showOrHide);
        },

        /**
         * Show the graph
         */
        show: function () {
            this.setVisible(true);
        },

        /**
         * Hide the graph
         */
        hide: function () {
            this.setVisible(false);
        },


        /**
         * Set the selected state of the graph
         *
         * @param selected {Boolean} True to select the series, false to unselect. If
         *                UNDEFINED, the selection state is toggled.
         */
        select: function (selected) {
            var series = this;
            // if called without an argument, toggle
            series.selected = selected = (selected === UNDEFINED) ? !series.selected : selected;

            if (series.checkbox) {
                series.checkbox.checked = selected;
            }

            fireEvent(series, selected ? 'select' : 'unselect');
        },

        drawTracker: TrackerMixin.drawTrackerGraph
    });
    /* ****************************************************************************
     * Start ordinal axis logic                                                   *
     *****************************************************************************/


    wrap(Series.prototype, 'init', function (proceed) {
        var series = this,
            xAxis;

        // call the original function
        proceed.apply(this, Array.prototype.slice.call(arguments, 1));

        xAxis = series.xAxis;

        // Destroy the extended ordinal index on updated data
        if (xAxis && xAxis.options.ordinal) {
            addEvent(series, 'updatedData', function () {
                delete xAxis.ordinalIndex;
            });
        }
    });

    /**
     * In an ordinal axis, there might be areas with dense consentrations of points, then large
     * gaps between some. Creating equally distributed ticks over this entire range
     * may lead to a huge number of ticks that will later be removed. So instead, break the
     * positions up in segments, find the tick positions for each segment then concatenize them.
     * This method is used from both data grouping logic and X axis tick position logic.
     */
    wrap(Axis.prototype, 'getTimeTicks', function (proceed, normalizedInterval, min, max, startOfWeek, positions, closestDistance, findHigherRanks) {

        var start = 0,
            end,
            segmentPositions,
            higherRanks = {},
            hasCrossedHigherRank,
            info,
            posLength,
            outsideMax,
            groupPositions = [],
            lastGroupPosition = -Number.MAX_VALUE,
            tickPixelIntervalOption = this.options.tickPixelInterval;

        // The positions are not always defined, for example for ordinal positions when data
        // has regular interval (#1557, #2090)
        if ((!this.options.ordinal && !this.options.breaks) || !positions || positions.length < 3 || min === UNDEFINED) {
            return proceed.call(this, normalizedInterval, min, max, startOfWeek);
        }

        // Analyze the positions array to split it into segments on gaps larger than 5 times
        // the closest distance. The closest distance is already found at this point, so
        // we reuse that instead of computing it again.
        posLength = positions.length;

        for (end = 0; end < posLength; end++) {

            outsideMax = end && positions[end - 1] > max;

            if (positions[end] < min) { // Set the last position before min
                start = end;
            }

            if (end === posLength - 1 || positions[end + 1] - positions[end] > closestDistance * 5 || outsideMax) {

                // For each segment, calculate the tick positions from the getTimeTicks utility
                // function. The interval will be the same regardless of how long the segment is.
                if (positions[end] > lastGroupPosition) { // #1475

                    segmentPositions = proceed.call(this, normalizedInterval, positions[start], positions[end], startOfWeek);

                    // Prevent duplicate groups, for example for multiple segments within one larger time frame (#1475)
                    while (segmentPositions.length && segmentPositions[0] <= lastGroupPosition) {
                        segmentPositions.shift();
                    }
                    if (segmentPositions.length) {
                        lastGroupPosition = segmentPositions[segmentPositions.length - 1];
                    }

                    groupPositions = groupPositions.concat(segmentPositions);
                }
                // Set start of next segment
                start = end + 1;
            }

            if (outsideMax) {
                break;
            }
        }

        // Get the grouping info from the last of the segments. The info is the same for
        // all segments.
        info = segmentPositions.info;

        // Optionally identify ticks with higher rank, for example when the ticks
        // have crossed midnight.
        if (findHigherRanks && info.unitRange <= timeUnits.hour) {
            end = groupPositions.length - 1;

            // Compare points two by two
            for (start = 1; start < end; start++) {
                if (dateFormat('%d', groupPositions[start]) !== dateFormat('%d', groupPositions[start - 1])) {
                    higherRanks[groupPositions[start]] = 'day';
                    hasCrossedHigherRank = true;
                }
            }

            // If the complete array has crossed midnight, we want to mark the first
            // positions also as higher rank
            if (hasCrossedHigherRank) {
                higherRanks[groupPositions[0]] = 'day';
            }
            info.higherRanks = higherRanks;
        }

        // Save the info
        groupPositions.info = info;



        // Don't show ticks within a gap in the ordinal axis, where the space between
        // two points is greater than a portion of the tick pixel interval
        if (findHigherRanks && defined(tickPixelIntervalOption)) { // check for squashed ticks

            var length = groupPositions.length,
                i = length,
                itemToRemove,
                translated,
                translatedArr = [],
                lastTranslated,
                medianDistance,
                distance,
                distances = [];

            // Find median pixel distance in order to keep a reasonably even distance between
            // ticks (#748)
            while (i--) {
                translated = this.translate(groupPositions[i]);
                if (lastTranslated) {
                    distances[i] = lastTranslated - translated;
                }
                translatedArr[i] = lastTranslated = translated;
            }
            distances.sort();
            medianDistance = distances[mathFloor(distances.length / 2)];
            if (medianDistance < tickPixelIntervalOption * 0.6) {
                medianDistance = null;
            }

            // Now loop over again and remove ticks where needed
            i = groupPositions[length - 1] > max ? length - 1 : length; // #817
            lastTranslated = undefined;
            while (i--) {
                translated = translatedArr[i];
                distance = lastTranslated - translated;

                // Remove ticks that are closer than 0.6 times the pixel interval from the one to the right,
                // but not if it is close to the median distance (#748).
                if (lastTranslated && distance < tickPixelIntervalOption * 0.8 &&
                        (medianDistance === null || distance < medianDistance * 0.8)) {

                    // Is this a higher ranked position with a normal position to the right?
                    if (higherRanks[groupPositions[i]] && !higherRanks[groupPositions[i + 1]]) {

                        // Yes: remove the lower ranked neighbour to the right
                        itemToRemove = i + 1;
                        lastTranslated = translated; // #709

                    } else {

                        // No: remove this one
                        itemToRemove = i;
                    }

                    groupPositions.splice(itemToRemove, 1);

                } else {
                    lastTranslated = translated;
                }
            }
        }
        return groupPositions;
    });

    // Extend the Axis prototype
    extend(Axis.prototype, {

        /**
         * Calculate the ordinal positions before tick positions are calculated.
         */
        beforeSetTickPositions: function () {
            var axis = this,
                len,
                ordinalPositions = [],
                useOrdinal = false,
                dist,
                extremes = axis.getExtremes(),
                min = extremes.min,
                max = extremes.max,
                minIndex,
                maxIndex,
                slope,
                hasBreaks = axis.isXAxis && !!axis.options.breaks,
                isOrdinal = axis.options.ordinal,
                i;

            // apply the ordinal logic
            if (isOrdinal || hasBreaks) { // #4167 YAxis is never ordinal ?

                each(axis.series, function (series, i) {

                    if (series.visible !== false && (series.takeOrdinalPosition !== false || hasBreaks)) {

                        // concatenate the processed X data into the existing positions, or the empty array
                        ordinalPositions = ordinalPositions.concat(series.processedXData);
                        len = ordinalPositions.length;

                        // remove duplicates (#1588)
                        ordinalPositions.sort(function (a, b) {
                            return a - b; // without a custom function it is sorted as strings
                        });

                        if (len) {
                            i = len - 1;
                            while (i--) {
                                if (ordinalPositions[i] === ordinalPositions[i + 1]) {
                                    ordinalPositions.splice(i, 1);
                                }
                            }
                        }
                    }

                });

                // cache the length
                len = ordinalPositions.length;

                // Check if we really need the overhead of mapping axis data against the ordinal positions.
                // If the series consist of evenly spaced data any way, we don't need any ordinal logic.
                if (len > 2) { // two points have equal distance by default
                    dist = ordinalPositions[1] - ordinalPositions[0];
                    i = len - 1;
                    while (i-- && !useOrdinal) {
                        if (ordinalPositions[i + 1] - ordinalPositions[i] !== dist) {
                            useOrdinal = true;
                        }
                    }

                    // When zooming in on a week, prevent axis padding for weekends even though the data within
                    // the week is evenly spaced.
                    if (!axis.options.keepOrdinalPadding && (ordinalPositions[0] - min > dist || max - ordinalPositions[ordinalPositions.length - 1] > dist)) {
                        useOrdinal = true;
                    }
                }

                // Record the slope and offset to compute the linear values from the array index.
                // Since the ordinal positions may exceed the current range, get the start and
                // end positions within it (#719, #665b)
                if (useOrdinal) {

                    // Register
                    axis.ordinalPositions = ordinalPositions;

                    // This relies on the ordinalPositions being set. Use mathMax and mathMin to prevent
                    // padding on either sides of the data.
                    minIndex = axis.val2lin(mathMax(min, ordinalPositions[0]), true);
                    maxIndex = mathMax(axis.val2lin(mathMin(max, ordinalPositions[ordinalPositions.length - 1]), true), 1); // #3339

                    // Set the slope and offset of the values compared to the indices in the ordinal positions
                    axis.ordinalSlope = slope = (max - min) / (maxIndex - minIndex);
                    axis.ordinalOffset = min - (minIndex * slope);

                } else {
                    axis.ordinalPositions = axis.ordinalSlope = axis.ordinalOffset = UNDEFINED;
                }
            }
            axis.isOrdinal = isOrdinal && useOrdinal; // #3818, #4196, #4926
            axis.groupIntervalFactor = null; // reset for next run
        },
        /**
         * Translate from a linear axis value to the corresponding ordinal axis position. If there
         * are no gaps in the ordinal axis this will be the same. The translated value is the value
         * that the point would have if the axis were linear, using the same min and max.
         *
         * @param Number val The axis value
         * @param Boolean toIndex Whether to return the index in the ordinalPositions or the new value
         */
        val2lin: function (val, toIndex) {
            var axis = this,
                ordinalPositions = axis.ordinalPositions,
                ret;

            if (!ordinalPositions) {
                ret = val;

            } else {

                var ordinalLength = ordinalPositions.length,
                    i,
                    distance,
                    ordinalIndex;

                // first look for an exact match in the ordinalpositions array
                i = ordinalLength;
                while (i--) {
                    if (ordinalPositions[i] === val) {
                        ordinalIndex = i;
                        break;
                    }
                }

                // if that failed, find the intermediate position between the two nearest values
                i = ordinalLength - 1;
                while (i--) {
                    if (val > ordinalPositions[i] || i === 0) { // interpolate
                        distance = (val - ordinalPositions[i]) / (ordinalPositions[i + 1] - ordinalPositions[i]); // something between 0 and 1
                        ordinalIndex = i + distance;
                        break;
                    }
                }
                ret = toIndex ?
                    ordinalIndex :
                    axis.ordinalSlope * (ordinalIndex || 0) + axis.ordinalOffset;
            }
            return ret;
        },
        /**
         * Translate from linear (internal) to axis value
         *
         * @param Number val The linear abstracted value
         * @param Boolean fromIndex Translate from an index in the ordinal positions rather than a value
         */
        lin2val: function (val, fromIndex) {
            var axis = this,
                ordinalPositions = axis.ordinalPositions,
                ret;

            if (!ordinalPositions) { // the visible range contains only equally spaced values
                ret = val;

            } else {

                var ordinalSlope = axis.ordinalSlope,
                    ordinalOffset = axis.ordinalOffset,
                    i = ordinalPositions.length - 1,
                    linearEquivalentLeft,
                    linearEquivalentRight,
                    distance;


                // Handle the case where we translate from the index directly, used only
                // when panning an ordinal axis
                if (fromIndex) {

                    if (val < 0) { // out of range, in effect panning to the left
                        val = ordinalPositions[0];
                    } else if (val > i) { // out of range, panning to the right
                        val = ordinalPositions[i];
                    } else { // split it up
                        i = mathFloor(val);
                        distance = val - i; // the decimal
                    }

                // Loop down along the ordinal positions. When the linear equivalent of i matches
                // an ordinal position, interpolate between the left and right values.
                } else {
                    while (i--) {
                        linearEquivalentLeft = (ordinalSlope * i) + ordinalOffset;
                        if (val >= linearEquivalentLeft) {
                            linearEquivalentRight = (ordinalSlope * (i + 1)) + ordinalOffset;
                            distance = (val - linearEquivalentLeft) / (linearEquivalentRight - linearEquivalentLeft); // something between 0 and 1
                            break;
                        }
                    }
                }

                // If the index is within the range of the ordinal positions, return the associated
                // or interpolated value. If not, just return the value
                ret = distance !== UNDEFINED && ordinalPositions[i] !== UNDEFINED ?
                    ordinalPositions[i] + (distance ? distance * (ordinalPositions[i + 1] - ordinalPositions[i]) : 0) :
                    val;
            }
            return ret;
        },
        /**
         * Get the ordinal positions for the entire data set. This is necessary in chart panning
         * because we need to find out what points or data groups are available outside the
         * visible range. When a panning operation starts, if an index for the given grouping
         * does not exists, it is created and cached. This index is deleted on updated data, so
         * it will be regenerated the next time a panning operation starts.
         */
        getExtendedPositions: function () {
            var axis = this,
                chart = axis.chart,
                grouping = axis.series[0].currentDataGrouping,
                ordinalIndex = axis.ordinalIndex,
                key = grouping ? grouping.count + grouping.unitName : 'raw',
                extremes = axis.getExtremes(),
                fakeAxis,
                fakeSeries;

            // If this is the first time, or the ordinal index is deleted by updatedData,
            // create it.
            if (!ordinalIndex) {
                ordinalIndex = axis.ordinalIndex = {};
            }


            if (!ordinalIndex[key]) {

                // Create a fake axis object where the extended ordinal positions are emulated
                fakeAxis = {
                    series: [],
                    getExtremes: function () {
                        return {
                            min: extremes.dataMin,
                            max: extremes.dataMax
                        };
                    },
                    options: {
                        ordinal: true
                    },
                    val2lin: Axis.prototype.val2lin // #2590
                };

                // Add the fake series to hold the full data, then apply processData to it
                each(axis.series, function (series) {
                    fakeSeries = {
                        xAxis: fakeAxis,
                        xData: series.xData,
                        chart: chart,
                        destroyGroupedData: noop
                    };
                    fakeSeries.options = {
                        dataGrouping: grouping ? {
                            enabled: true,
                            forced: true,
                            approximation: 'open', // doesn't matter which, use the fastest
                            units: [[grouping.unitName, [grouping.count]]]
                        } : {
                            enabled: false
                        }
                    };
                    series.processData.apply(fakeSeries);

                    fakeAxis.series.push(fakeSeries);
                });

                // Run beforeSetTickPositions to compute the ordinalPositions
                axis.beforeSetTickPositions.apply(fakeAxis);

                // Cache it
                ordinalIndex[key] = fakeAxis.ordinalPositions;
            }
            return ordinalIndex[key];
        },

        /**
         * Find the factor to estimate how wide the plot area would have been if ordinal
         * gaps were included. This value is used to compute an imagined plot width in order
         * to establish the data grouping interval.
         *
         * A real world case is the intraday-candlestick
         * example. Without this logic, it would show the correct data grouping when viewing
         * a range within each day, but once moving the range to include the gap between two
         * days, the interval would include the cut-away night hours and the data grouping
         * would be wrong. So the below method tries to compensate by identifying the most
         * common point interval, in this case days.
         *
         * An opposite case is presented in issue #718. We have a long array of daily data,
         * then one point is appended one hour after the last point. We expect the data grouping
         * not to change.
         *
         * In the future, if we find cases where this estimation doesn't work optimally, we
         * might need to add a second pass to the data grouping logic, where we do another run
         * with a greater interval if the number of data groups is more than a certain fraction
         * of the desired group count.
         */
        getGroupIntervalFactor: function (xMin, xMax, series) {
            var i,
                processedXData = series.processedXData,
                len = processedXData.length,
                distances = [],
                median,
                groupIntervalFactor = this.groupIntervalFactor;

            // Only do this computation for the first series, let the other inherit it (#2416)
            if (!groupIntervalFactor) {

                // Register all the distances in an array
                for (i = 0; i < len - 1; i++) {
                    distances[i] = processedXData[i + 1] - processedXData[i];
                }

                // Sort them and find the median
                distances.sort(function (a, b) {
                    return a - b;
                });
                median = distances[mathFloor(len / 2)];

                // Compensate for series that don't extend through the entire axis extent. #1675.
                xMin = mathMax(xMin, processedXData[0]);
                xMax = mathMin(xMax, processedXData[len - 1]);

                this.groupIntervalFactor = groupIntervalFactor = (len * median) / (xMax - xMin);
            }

            // Return the factor needed for data grouping
            return groupIntervalFactor;
        },

        /**
         * Make the tick intervals closer because the ordinal gaps make the ticks spread out or cluster
         */
        postProcessTickInterval: function (tickInterval) {
            // Problem: http://jsfiddle.net/highcharts/FQm4E/1/
            // This is a case where this algorithm doesn't work optimally. In this case, the
            // tick labels are spread out per week, but all the gaps reside within weeks. So
            // we have a situation where the labels are courser than the ordinal gaps, and
            // thus the tick interval should not be altered
            var ordinalSlope = this.ordinalSlope,
                ret;


            if (ordinalSlope) {
                if (!this.options.breaks) {
                    ret = tickInterval / (ordinalSlope / this.closestPointRange);
                } else {
                    ret = this.closestPointRange;
                }
            } else {
                ret = tickInterval;
            }
            return ret;
        }
    });

    // Extending the Chart.pan method for ordinal axes
    wrap(Chart.prototype, 'pan', function (proceed, e) {
        var chart = this,
            xAxis = chart.xAxis[0],
            chartX = e.chartX,
            runBase = false;

        if (xAxis.options.ordinal && xAxis.series.length) {

            var mouseDownX = chart.mouseDownX,
                extremes = xAxis.getExtremes(),
                dataMax = extremes.dataMax,
                min = extremes.min,
                max = extremes.max,
                trimmedRange,
                hoverPoints = chart.hoverPoints,
                closestPointRange = xAxis.closestPointRange,
                pointPixelWidth = xAxis.translationSlope * (xAxis.ordinalSlope || closestPointRange),
                movedUnits = (mouseDownX - chartX) / pointPixelWidth, // how many ordinal units did we move?
                extendedAxis = { ordinalPositions: xAxis.getExtendedPositions() }, // get index of all the chart's points
                ordinalPositions,
                searchAxisLeft,
                lin2val = xAxis.lin2val,
                val2lin = xAxis.val2lin,
                searchAxisRight;

            if (!extendedAxis.ordinalPositions) { // we have an ordinal axis, but the data is equally spaced
                runBase = true;

            } else if (mathAbs(movedUnits) > 1) {

                // Remove active points for shared tooltip
                if (hoverPoints) {
                    each(hoverPoints, function (point) {
                        point.setState();
                    });
                }

                if (movedUnits < 0) {
                    searchAxisLeft = extendedAxis;
                    searchAxisRight = xAxis.ordinalPositions ? xAxis : extendedAxis;
                } else {
                    searchAxisLeft = xAxis.ordinalPositions ? xAxis : extendedAxis;
                    searchAxisRight = extendedAxis;
                }

                // In grouped data series, the last ordinal position represents the grouped data, which is
                // to the left of the real data max. If we don't compensate for this, we will be allowed
                // to pan grouped data series passed the right of the plot area.
                ordinalPositions = searchAxisRight.ordinalPositions;
                if (dataMax > ordinalPositions[ordinalPositions.length - 1]) {
                    ordinalPositions.push(dataMax);
                }

                // Get the new min and max values by getting the ordinal index for the current extreme,
                // then add the moved units and translate back to values. This happens on the
                // extended ordinal positions if the new position is out of range, else it happens
                // on the current x axis which is smaller and faster.
                chart.fixedRange = max - min;
                trimmedRange = xAxis.toFixedRange(null, null,
                    lin2val.apply(searchAxisLeft, [
                        val2lin.apply(searchAxisLeft, [min, true]) + movedUnits, // the new index
                        true // translate from index
                    ]),
                    lin2val.apply(searchAxisRight, [
                        val2lin.apply(searchAxisRight, [max, true]) + movedUnits, // the new index
                        true // translate from index
                    ])
                );

                // Apply it if it is within the available data range
                if (trimmedRange.min >= mathMin(extremes.dataMin, min) && trimmedRange.max <= mathMax(dataMax, max)) {
                    xAxis.setExtremes(trimmedRange.min, trimmedRange.max, true, false, { trigger: 'pan' });
                }

                chart.mouseDownX = chartX; // set new reference for next run
                css(chart.container, { cursor: 'move' });
            }

        } else {
            runBase = true;
        }

        // revert to the linear chart.pan version
        if (runBase) {
            // call the original function
            proceed.apply(this, Array.prototype.slice.call(arguments, 1));
        }
    });



    /**
     * Extend getGraphPath by identifying gaps in the ordinal data so that we can draw a gap in the
     * line or area
     */
    Series.prototype.gappedPath = function () {
        var gapSize = this.options.gapSize,
            points = this.points.slice(),
            i = points.length - 1;

        if (gapSize && i > 0) { // #5008

            // extension for ordinal breaks
            while (i--) {
                if (points[i + 1].x - points[i].x > this.closestPointRange * gapSize) {
                    points.splice( // insert after this one
                        i + 1,
                        0,
                        { isNull: true }
                    );
                }
            }
        }

        // Call base method
        //return proceed.call(this, points, a, b);
        return this.getGraphPath(points);
    };

    /* ****************************************************************************
     * End ordinal axis logic                                                   *
     *****************************************************************************/
    /**
     * Highstock JS v4.2.5 (2016-05-06)
     * Highcharts Broken Axis module
     * 
     * License: www.highcharts.com/license
     */

    (function (factory) {
        
        factory(Highcharts);
    
    }(function (H) {

        'use strict';

        var pick = H.pick,
            wrap = H.wrap,
            each = H.each,
            extend = H.extend,
            fireEvent = H.fireEvent,
            Axis = H.Axis,
            Series = H.Series;

        function stripArguments() {
            return Array.prototype.slice.call(arguments, 1);
        }

        extend(Axis.prototype, {
            isInBreak: function (brk, val) {
                var ret,
                    repeat = brk.repeat || Infinity,
                    from = brk.from,
                    length = brk.to - brk.from,
                    test = (val >= from ? (val - from) % repeat :  repeat - ((from - val) % repeat));

                if (!brk.inclusive) {
                    ret = test < length && test !== 0;
                } else {
                    ret = test <= length;
                }
                return ret;
            },

            isInAnyBreak: function (val, testKeep) {

                var breaks = this.options.breaks,
                    i = breaks && breaks.length,
                    inbrk,
                    keep,
                    ret;

            
                if (i) { 

                    while (i--) {
                        if (this.isInBreak(breaks[i], val)) {
                            inbrk = true;
                            if (!keep) {
                                keep = pick(breaks[i].showPoints, this.isXAxis ? false : true);
                            }
                        }
                    }

                    if (inbrk && testKeep) {
                        ret = inbrk && !keep;
                    } else {
                        ret = inbrk;
                    }
                }
                return ret;
            }
        });

        wrap(Axis.prototype, 'setTickPositions', function (proceed) {
            proceed.apply(this, Array.prototype.slice.call(arguments, 1));
        
            if (this.options.breaks) {
                var axis = this,
                    tickPositions = this.tickPositions,
                    info = this.tickPositions.info,
                    newPositions = [],
                    i;

                for (i = 0; i < tickPositions.length; i++) {
                    if (!axis.isInAnyBreak(tickPositions[i])) {
                        newPositions.push(tickPositions[i]);
                    }
                }

                this.tickPositions = newPositions;
                this.tickPositions.info = info;
            }
        });
    
        wrap(Axis.prototype, 'init', function (proceed, chart, userOptions) {
            // Force Axis to be not-ordinal when breaks are defined
            if (userOptions.breaks && userOptions.breaks.length) {
                userOptions.ordinal = false;
            }

            proceed.call(this, chart, userOptions);

            if (this.options.breaks) {

                var axis = this;
            
                axis.isBroken = true;

                this.val2lin = function (val) {
                    var nval = val,
                        brk,
                        i;

                    for (i = 0; i < axis.breakArray.length; i++) {
                        brk = axis.breakArray[i];
                        if (brk.to <= val) {
                            nval -= brk.len;
                        } else if (brk.from >= val) {
                            break;
                        } else if (axis.isInBreak(brk, val)) {
                            nval -= (val - brk.from);
                            break;
                        }
                    }

                    return nval;
                };
            
                this.lin2val = function (val) {
                    var nval = val,
                        brk,
                        i;

                    for (i = 0; i < axis.breakArray.length; i++) {
                        brk = axis.breakArray[i];
                        if (brk.from >= nval) {
                            break;
                        } else if (brk.to < nval) {
                            nval += brk.len;
                        } else if (axis.isInBreak(brk, nval)) {
                            nval += brk.len;
                        }
                    }
                    return nval;
                };

                this.setExtremes = function (newMin, newMax, redraw, animation, eventArguments) {
                    // If trying to set extremes inside a break, extend it to before and after the break ( #3857 )
                    while (this.isInAnyBreak(newMin)) {
                        newMin -= this.closestPointRange;
                    }            
                    while (this.isInAnyBreak(newMax)) {
                        newMax -= this.closestPointRange;
                    }
                    Axis.prototype.setExtremes.call(this, newMin, newMax, redraw, animation, eventArguments);
                };

                this.setAxisTranslation = function (saveOld) {
                    Axis.prototype.setAxisTranslation.call(this, saveOld);

                    var breaks = axis.options.breaks,
                        breakArrayT = [],    // Temporary one
                        breakArray = [],
                        length = 0, 
                        inBrk,
                        repeat,
                        brk,
                        min = axis.userMin || axis.min,
                        max = axis.userMax || axis.max,
                        start,
                        i,
                        j;

                    // Min & max check (#4247)
                    for (i in breaks) {
                        brk = breaks[i];
                        repeat = brk.repeat || Infinity;
                        if (axis.isInBreak(brk, min)) {
                            min += (brk.to % repeat) - (min % repeat);
                        }
                        if (axis.isInBreak(brk, max)) {
                            max -= (max % repeat) - (brk.from % repeat);
                        }
                    }

                    // Construct an array holding all breaks in the axis
                    for (i in breaks) {
                        brk = breaks[i];
                        start = brk.from;
                        repeat = brk.repeat || Infinity;

                        while (start - repeat > min) {
                            start -= repeat;
                        }
                        while (start < min) {
                            start += repeat;
                        }

                        for (j = start; j < max; j += repeat) {
                            breakArrayT.push({
                                value: j,
                                move: 'in'
                            });
                            breakArrayT.push({
                                value: j + (brk.to - brk.from),
                                move: 'out',
                                size: brk.breakSize
                            });
                        }
                    }

                    breakArrayT.sort(function (a, b) {
                        var ret;
                        if (a.value === b.value) {
                            ret = (a.move === 'in' ? 0 : 1) - (b.move === 'in' ? 0 : 1);
                        } else {
                            ret = a.value - b.value;
                        }
                        return ret;
                    });
                
                    // Simplify the breaks
                    inBrk = 0;
                    start = min;

                    for (i in breakArrayT) {
                        brk = breakArrayT[i];
                        inBrk += (brk.move === 'in' ? 1 : -1);

                        if (inBrk === 1 && brk.move === 'in') {
                            start = brk.value;
                        }
                        if (inBrk === 0) {
                            breakArray.push({
                                from: start,
                                to: brk.value,
                                len: brk.value - start - (brk.size || 0)
                            });
                            length += brk.value - start - (brk.size || 0);
                        }
                    }

                    axis.breakArray = breakArray;

                    fireEvent(axis, 'afterBreaks');
                
                    axis.transA *= ((max - axis.min) / (max - min - length));

                    axis.min = min;
                    axis.max = max;
                };
            }
        });

        wrap(Series.prototype, 'generatePoints', function (proceed) {

            proceed.apply(this, stripArguments(arguments));

            var series = this,
                xAxis = series.xAxis,
                yAxis = series.yAxis,
                points = series.points,
                point,
                i = points.length,
                connectNulls = series.options.connectNulls,
                nullGap;


            if (xAxis && yAxis && (xAxis.options.breaks || yAxis.options.breaks)) {
                while (i--) {
                    point = points[i];

                    nullGap = point.y === null && connectNulls === false; // respect nulls inside the break (#4275)
                    if (!nullGap && (xAxis.isInAnyBreak(point.x, true) || yAxis.isInAnyBreak(point.y, true))) {
                        points.splice(i, 1);
                        if (this.data[i]) {
                            this.data[i].destroyElements(); // removes the graphics for this point if they exist
                        }
                    }
                }
            }

        });

        function drawPointsWrapped(proceed) {
            proceed.apply(this);
            this.drawBreaks(this.xAxis, ['x']);
            this.drawBreaks(this.yAxis, pick(this.pointArrayMap, ['y']));
        }

        H.Series.prototype.drawBreaks = function (axis, keys) {
            var series = this,
                points = series.points,
                breaks,
                threshold,
                eventName,
                y;

            each(keys, function (key) {
                breaks = axis.breakArray || [];
                threshold = axis.isXAxis ? axis.min : pick(series.options.threshold, axis.min);
                each(points, function (point) {
                    y = pick(point['stack' + key.toUpperCase()], point[key]);
                    each(breaks, function (brk) {
                        eventName = false;

                        if ((threshold < brk.from && y > brk.to) || (threshold > brk.from && y < brk.from)) { 
                            eventName = 'pointBreak';
                        } else if ((threshold < brk.from && y > brk.from && y < brk.to) || (threshold > brk.from && y > brk.to && y < brk.from)) { // point falls inside the break
                            eventName = 'pointInBreak';
                        } 
                        if (eventName) {
                            fireEvent(axis, eventName, { point: point, brk: brk });
                        }
                    });
                });
            });
        };

        wrap(H.seriesTypes.column.prototype, 'drawPoints', drawPointsWrapped);
        wrap(H.Series.prototype, 'drawPoints', drawPointsWrapped);

    }));
    /* ****************************************************************************
     * Start data grouping module                                                 *
     ******************************************************************************/
    var DATA_GROUPING = 'dataGrouping',
        seriesProto = Series.prototype,
        baseProcessData = seriesProto.processData,
        baseGeneratePoints = seriesProto.generatePoints,
        baseDestroy = seriesProto.destroy,

        commonOptions = {
            approximation: 'average', // average, open, high, low, close, sum
            //enabled: null, // (true for stock charts, false for basic),
            //forced: undefined,
            groupPixelWidth: 2,
            // the first one is the point or start value, the second is the start value if we're dealing with range,
            // the third one is the end value if dealing with a range
            dateTimeLabelFormats: {
                millisecond: ['%A, %b %e, %H:%M:%S.%L', '%A, %b %e, %H:%M:%S.%L', '-%H:%M:%S.%L'],
                second: ['%A, %b %e, %H:%M:%S', '%A, %b %e, %H:%M:%S', '-%H:%M:%S'],
                minute: ['%A, %b %e, %H:%M', '%A, %b %e, %H:%M', '-%H:%M'],
                hour: ['%A, %b %e, %H:%M', '%A, %b %e, %H:%M', '-%H:%M'],
                day: ['%A, %b %e, %Y', '%A, %b %e', '-%A, %b %e, %Y'],
                week: ['Week from %A, %b %e, %Y', '%A, %b %e', '-%A, %b %e, %Y'],
                month: ['%B %Y', '%B', '-%B %Y'],
                year: ['%Y', '%Y', '-%Y']
            }
            // smoothed = false, // enable this for navigator series only
        },

        specificOptions = { // extends common options
            line: {},
            spline: {},
            area: {},
            areaspline: {},
            column: {
                approximation: 'sum',
                groupPixelWidth: 10
            },
            arearange: {
                approximation: 'range'
            },
            areasplinerange: {
                approximation: 'range'
            },
            columnrange: {
                approximation: 'range',
                groupPixelWidth: 10
            },
            candlestick: {
                approximation: 'ohlc',
                groupPixelWidth: 10
            },
            ohlc: {
                approximation: 'ohlc',
                groupPixelWidth: 5
            }
        },

        // units are defined in a separate array to allow complete overriding in case of a user option
        defaultDataGroupingUnits = [
            [
                'millisecond', // unit name
                [1, 2, 5, 10, 20, 25, 50, 100, 200, 500] // allowed multiples
            ], [
                'second',
                [1, 2, 5, 10, 15, 30]
            ], [
                'minute',
                [1, 2, 5, 10, 15, 30]
            ], [
                'hour',
                [1, 2, 3, 4, 6, 8, 12]
            ], [
                'day',
                [1]
            ], [
                'week',
                [1]
            ], [
                'month',
                [1, 3, 6]
            ], [
                'year',
                null
            ]
        ],


        /**
         * Define the available approximation types. The data grouping approximations takes an array
         * or numbers as the first parameter. In case of ohlc, four arrays are sent in as four parameters.
         * Each array consists only of numbers. In case null values belong to the group, the property
         * .hasNulls will be set to true on the array.
         */
        approximations = {
            sum: function (arr) {
                var len = arr.length,
                    ret;

                // 1. it consists of nulls exclusively
                if (!len && arr.hasNulls) {
                    ret = null;
                // 2. it has a length and real values
                } else if (len) {
                    ret = 0;
                    while (len--) {
                        ret += arr[len];
                    }
                }
                // 3. it has zero length, so just return undefined
                // => doNothing()

                return ret;
            },
            average: function (arr) {
                var len = arr.length,
                    ret = approximations.sum(arr);

                // If we have a number, return it divided by the length. If not, return
                // null or undefined based on what the sum method finds.
                if (isNumber(ret) && len) {
                    ret = ret / len;
                }

                return ret;
            },
            open: function (arr) {
                return arr.length ? arr[0] : (arr.hasNulls ? null : UNDEFINED);
            },
            high: function (arr) {
                return arr.length ? arrayMax(arr) : (arr.hasNulls ? null : UNDEFINED);
            },
            low: function (arr) {
                return arr.length ? arrayMin(arr) : (arr.hasNulls ? null : UNDEFINED);
            },
            close: function (arr) {
                return arr.length ? arr[arr.length - 1] : (arr.hasNulls ? null : UNDEFINED);
            },
            // ohlc and range are special cases where a multidimensional array is input and an array is output
            ohlc: function (open, high, low, close) {
                open = approximations.open(open);
                high = approximations.high(high);
                low = approximations.low(low);
                close = approximations.close(close);

                if (isNumber(open) || isNumber(high) || isNumber(low) || isNumber(close)) {
                    return [open, high, low, close];
                }
                // else, return is undefined
            },
            range: function (low, high) {
                low = approximations.low(low);
                high = approximations.high(high);

                if (isNumber(low) || isNumber(high)) {
                    return [low, high];
                }
                // else, return is undefined
            }
        };


    /**
     * Takes parallel arrays of x and y data and groups the data into intervals defined by groupPositions, a collection
     * of starting x values for each group.
     */
    seriesProto.groupData = function (xData, yData, groupPositions, approximation) {
        var series = this,
            data = series.data,
            dataOptions = series.options.data,
            groupedXData = [],
            groupedYData = [],
            groupMap = [],
            dataLength = xData.length,
            pointX,
            pointY,
            groupedY,
            handleYData = !!yData, // when grouping the fake extended axis for panning, we don't need to consider y
            values = [[], [], [], []],
            approximationFn = typeof approximation === 'function' ? approximation : approximations[approximation],
            pointArrayMap = series.pointArrayMap,
            pointArrayMapLength = pointArrayMap && pointArrayMap.length,
            i,
            start = 0;

        // Start with the first point within the X axis range (#2696)
        for (i = 0; i <= dataLength; i++) {
            if (xData[i] >= groupPositions[0]) {
                break;
            }
        }

        for (i; i <= dataLength; i++) {

            // when a new group is entered, summarize and initiate the previous group
            while ((groupPositions[1] !== UNDEFINED && xData[i] >= groupPositions[1]) ||
                    i === dataLength) { // get the last group

                // get group x and y
                pointX = groupPositions.shift();
                groupedY = approximationFn.apply(0, values);

                // push the grouped data
                if (groupedY !== UNDEFINED) {
                    groupedXData.push(pointX);
                    groupedYData.push(groupedY);
                    groupMap.push({ start: start, length: values[0].length });
                }

                // reset the aggregate arrays
                start = i;
                values[0] = [];
                values[1] = [];
                values[2] = [];
                values[3] = [];

                // don't loop beyond the last group
                if (i === dataLength) {
                    break;
                }
            }

            // break out
            if (i === dataLength) {
                break;
            }

            // for each raw data point, push it to an array that contains all values for this specific group
            if (pointArrayMap) {

                var index = series.cropStart + i,
                    point = (data && data[index]) || series.pointClass.prototype.applyOptions.apply({ series: series }, [dataOptions[index]]),
                    j,
                    val;

                for (j = 0; j < pointArrayMapLength; j++) {
                    val = point[pointArrayMap[j]];
                    if (isNumber(val)) {
                        values[j].push(val);
                    } else if (val === null) {
                        values[j].hasNulls = true;
                    }
                }

            } else {
                pointY = handleYData ? yData[i] : null;

                if (isNumber(pointY)) {
                    values[0].push(pointY);
                } else if (pointY === null) {
                    values[0].hasNulls = true;
                }
            }
        }

        return [groupedXData, groupedYData, groupMap];
    };

    /**
     * Extend the basic processData method, that crops the data to the current zoom
     * range, with data grouping logic.
     */
    seriesProto.processData = function () {
        var series = this,
            chart = series.chart,
            options = series.options,
            dataGroupingOptions = options[DATA_GROUPING],
            groupingEnabled = series.allowDG !== false && dataGroupingOptions && pick(dataGroupingOptions.enabled, chart.options._stock),
            hasGroupedData,
            skip;

        // run base method
        series.forceCrop = groupingEnabled; // #334
        series.groupPixelWidth = null; // #2110
        series.hasProcessed = true; // #2692

        // skip if processData returns false or if grouping is disabled (in that order)
        skip = baseProcessData.apply(series, arguments) === false || !groupingEnabled;
        if (!skip) {
            series.destroyGroupedData();

            var i,
                processedXData = series.processedXData,
                processedYData = series.processedYData,
                plotSizeX = chart.plotSizeX,
                xAxis = series.xAxis,
                ordinal = xAxis.options.ordinal,
                groupPixelWidth = series.groupPixelWidth = xAxis.getGroupPixelWidth && xAxis.getGroupPixelWidth();

            // Execute grouping if the amount of points is greater than the limit defined in groupPixelWidth
            if (groupPixelWidth) {
                hasGroupedData = true;

                series.points = null; // force recreation of point instances in series.translate

                var extremes = xAxis.getExtremes(),
                    xMin = extremes.min,
                    xMax = extremes.max,
                    groupIntervalFactor = (ordinal && xAxis.getGroupIntervalFactor(xMin, xMax, series)) || 1,
                    interval = (groupPixelWidth * (xMax - xMin) / plotSizeX) * groupIntervalFactor,
                    groupPositions = xAxis.getTimeTicks(
                        xAxis.normalizeTimeTickInterval(interval, dataGroupingOptions.units || defaultDataGroupingUnits),
                        Math.min(xMin, processedXData[0]), // Processed data may extend beyond axis (#4907)
                        Math.max(xMax, processedXData[processedXData.length - 1]),
                        xAxis.options.startOfWeek,
                        processedXData,
                        series.closestPointRange
                    ),
                    groupedData = seriesProto.groupData.apply(series, [processedXData, processedYData, groupPositions, dataGroupingOptions.approximation]),
                    groupedXData = groupedData[0],
                    groupedYData = groupedData[1];

                // prevent the smoothed data to spill out left and right, and make
                // sure data is not shifted to the left
                if (dataGroupingOptions.smoothed) {
                    i = groupedXData.length - 1;
                    groupedXData[i] = Math.min(groupedXData[i], xMax);
                    while (i-- && i > 0) {
                        groupedXData[i] += interval / 2;
                    }
                    groupedXData[0] = Math.max(groupedXData[0], xMin);
                }

                // record what data grouping values were used
                series.currentDataGrouping = groupPositions.info;
                series.closestPointRange = groupPositions.info.totalRange;
                series.groupMap = groupedData[2];

                // Make sure the X axis extends to show the first group (#2533)
                if (defined(groupedXData[0]) && groupedXData[0] < xAxis.dataMin) {
                    if (xAxis.min === xAxis.dataMin) {
                        xAxis.min = groupedXData[0];
                    }
                    xAxis.dataMin = groupedXData[0];
                }

                // set series props
                series.processedXData = groupedXData;
                series.processedYData = groupedYData;
            } else {
                series.currentDataGrouping = series.groupMap = null;
            }
            series.hasGroupedData = hasGroupedData;
        }
    };

    /**
     * Destroy the grouped data points. #622, #740
     */
    seriesProto.destroyGroupedData = function () {

        var groupedData = this.groupedData;

        // clear previous groups
        each(groupedData || [], function (point, i) {
            if (point) {
                groupedData[i] = point.destroy ? point.destroy() : null;
            }
        });
        this.groupedData = null;
    };

    /**
     * Override the generatePoints method by adding a reference to grouped data
     */
    seriesProto.generatePoints = function () {

        baseGeneratePoints.apply(this);

        // record grouped data in order to let it be destroyed the next time processData runs
        this.destroyGroupedData(); // #622
        this.groupedData = this.hasGroupedData ? this.points : null;
    };

    /**
     * Extend the original method, make the tooltip's header reflect the grouped range
     */
    wrap(Tooltip.prototype, 'tooltipFooterHeaderFormatter', function (proceed, point, isFooter) {
        var tooltip = this,
            series = point.series,
            options = series.options,
            tooltipOptions = series.tooltipOptions,
            dataGroupingOptions = options.dataGrouping,
            xDateFormat = tooltipOptions.xDateFormat,
            xDateFormatEnd,
            xAxis = series.xAxis,
            currentDataGrouping,
            dateTimeLabelFormats,
            labelFormats,
            formattedKey;

        // apply only to grouped series
        if (xAxis && xAxis.options.type === 'datetime' && dataGroupingOptions && isNumber(point.key)) {

            // set variables
            currentDataGrouping = series.currentDataGrouping;
            dateTimeLabelFormats = dataGroupingOptions.dateTimeLabelFormats;

            // if we have grouped data, use the grouping information to get the right format
            if (currentDataGrouping) {
                labelFormats = dateTimeLabelFormats[currentDataGrouping.unitName];
                if (currentDataGrouping.count === 1) {
                    xDateFormat = labelFormats[0];
                } else {
                    xDateFormat = labelFormats[1];
                    xDateFormatEnd = labelFormats[2];
                }
            // if not grouped, and we don't have set the xDateFormat option, get the best fit,
            // so if the least distance between points is one minute, show it, but if the
            // least distance is one day, skip hours and minutes etc.
            } else if (!xDateFormat && dateTimeLabelFormats) {
                xDateFormat = tooltip.getXDateFormat(point, tooltipOptions, xAxis);
            }

            // now format the key
            formattedKey = dateFormat(xDateFormat, point.key);
            if (xDateFormatEnd) {
                formattedKey += dateFormat(xDateFormatEnd, point.key + currentDataGrouping.totalRange - 1);
            }

            // return the replaced format
            return format(tooltipOptions[(isFooter ? 'footer' : 'header') + 'Format'], {
                point: extend(point, { key: formattedKey }),
                series: series
            });
    
        }

        // else, fall back to the regular formatter
        return proceed.call(tooltip, point, isFooter);
    });

    /**
     * Extend the series destroyer
     */
    seriesProto.destroy = function () {
        var series = this,
            groupedData = series.groupedData || [],
            i = groupedData.length;

        while (i--) {
            if (groupedData[i]) {
                groupedData[i].destroy();
            }
        }
        baseDestroy.apply(series);
    };


    // Handle default options for data grouping. This must be set at runtime because some series types are
    // defined after this.
    wrap(seriesProto, 'setOptions', function (proceed, itemOptions) {

        var options = proceed.call(this, itemOptions),
            type = this.type,
            plotOptions = this.chart.options.plotOptions,
            defaultOptions = defaultPlotOptions[type].dataGrouping;

        if (specificOptions[type]) { // #1284
            if (!defaultOptions) {
                defaultOptions = merge(commonOptions, specificOptions[type]);
            }

            options.dataGrouping = merge(
                defaultOptions,
                plotOptions.series && plotOptions.series.dataGrouping, // #1228
                plotOptions[type].dataGrouping, // Set by the StockChart constructor
                itemOptions.dataGrouping
            );
        }

        if (this.chart.options._stock) {
            this.requireSorting = true;
        }

        return options;
    });


    /**
     * When resetting the scale reset the hasProccessed flag to avoid taking previous data grouping
     * of neighbour series into accound when determining group pixel width (#2692).
     */
    wrap(Axis.prototype, 'setScale', function (proceed) {
        proceed.call(this);
        each(this.series, function (series) {
            series.hasProcessed = false;
        });
    });

    /**
     * Get the data grouping pixel width based on the greatest defined individual width
     * of the axis' series, and if whether one of the axes need grouping.
     */
    Axis.prototype.getGroupPixelWidth = function () {

        var series = this.series,
            len = series.length,
            i,
            groupPixelWidth = 0,
            doGrouping = false,
            dataLength,
            dgOptions;

        // If multiple series are compared on the same x axis, give them the same
        // group pixel width (#334)
        i = len;
        while (i--) {
            dgOptions = series[i].options.dataGrouping;
            if (dgOptions) {
                groupPixelWidth = mathMax(groupPixelWidth, dgOptions.groupPixelWidth);

            }
        }

        // If one of the series needs grouping, apply it to all (#1634)
        i = len;
        while (i--) {
            dgOptions = series[i].options.dataGrouping;

            if (dgOptions && series[i].hasProcessed) { // #2692

                dataLength = (series[i].processedXData || series[i].data).length;

                // Execute grouping if the amount of points is greater than the limit defined in groupPixelWidth
                if (series[i].groupPixelWidth || dataLength > (this.chart.plotSizeX / groupPixelWidth) || (dataLength && dgOptions.forced)) {
                    doGrouping = true;
                }
            }
        }

        return doGrouping ? groupPixelWidth : 0;
    };

    /**
     * Force data grouping on all the axis' series.
     */
    Axis.prototype.setDataGrouping = function (dataGrouping, redraw) {
        var i;

        redraw = pick(redraw, true);

        if (!dataGrouping) {
            dataGrouping = {
                forced: false,
                units: null
            };
        }

        // Axis is instantiated, update all series
        if (this instanceof Axis) {
            i = this.series.length;
            while (i--) {
                this.series[i].update({
                    dataGrouping: dataGrouping
                }, false);
            }

        // Axis not yet instanciated, alter series options
        } else {
            each(this.chart.options.series, function (seriesOptions) {
                seriesOptions.dataGrouping = dataGrouping;
            }, false);
        }

        if (redraw) {
            this.chart.redraw();
        }
    };



    /* ****************************************************************************
     * End data grouping module                                                   *
     ******************************************************************************/
    /* ****************************************************************************
     * Start OHLC series code                                                     *
     *****************************************************************************/

    // 1 - Set default options
    defaultPlotOptions.ohlc = merge(defaultPlotOptions.column, {
        lineWidth: 1,
        tooltip: {
            pointFormat: '<span style="color:{point.color}">\u25CF</span> <b> {series.name}</b><br/>' +
                'Open: {point.open}<br/>' +
                'High: {point.high}<br/>' +
                'Low: {point.low}<br/>' +
                'Close: {point.close}<br/>'
        },
        states: {
            hover: {
                lineWidth: 3
            }
        },
        threshold: null
        //upColor: undefined
    });

    // 2 - Create the OHLCSeries object
    var OHLCSeries = extendClass(seriesTypes.column, {
        type: 'ohlc',
        pointArrayMap: ['open', 'high', 'low', 'close'], // array point configs are mapped to this
        toYData: function (point) { // return a plain array for speedy calculation
            return [point.open, point.high, point.low, point.close];
        },
        pointValKey: 'high',

        pointAttrToOptions: { // mapping between SVG attributes and the corresponding options
            stroke: 'color',
            'stroke-width': 'lineWidth'
        },
        upColorProp: 'stroke',

        /**
         * Postprocess mapping between options and SVG attributes
         */
        getAttribs: function () {
            seriesTypes.column.prototype.getAttribs.apply(this, arguments);
            var series = this,
                options = series.options,
                stateOptions = options.states,
                upColor = options.upColor || series.color,
                seriesDownPointAttr = merge(series.pointAttr),
                upColorProp = series.upColorProp;

            seriesDownPointAttr[''][upColorProp] = upColor;
            seriesDownPointAttr.hover[upColorProp] = stateOptions.hover.upColor || upColor;
            seriesDownPointAttr.select[upColorProp] = stateOptions.select.upColor || upColor;

            each(series.points, function (point) {
                if (point.open < point.close && !point.options.color) {
                    point.pointAttr = seriesDownPointAttr;
                }
            });
        },

        /**
         * Translate data points from raw values x and y to plotX and plotY
         */
        translate: function () {
            var series = this,
                yAxis = series.yAxis;

            seriesTypes.column.prototype.translate.apply(series);

            // do the translation
            each(series.points, function (point) {
                // the graphics
                if (point.open !== null) {
                    point.plotOpen = yAxis.translate(point.open, 0, 1, 0, 1);
                }
                if (point.close !== null) {
                    point.plotClose = yAxis.translate(point.close, 0, 1, 0, 1);
                }

            });
        },

        /**
         * Draw the data points
         */
        drawPoints: function () {
            var series = this,
                points = series.points,
                chart = series.chart,
                pointAttr,
                plotOpen,
                plotClose,
                crispCorr,
                halfWidth,
                path,
                graphic,
                crispX;


            each(points, function (point) {
                if (point.plotY !== UNDEFINED) {

                    graphic = point.graphic;
                    pointAttr = point.pointAttr[point.selected ? 'selected' : ''] || series.pointAttr[NORMAL_STATE];

                    // crisp vector coordinates
                    crispCorr = (pointAttr['stroke-width'] % 2) / 2;
                    crispX = mathRound(point.plotX) - crispCorr;  // #2596
                    halfWidth = mathRound(point.shapeArgs.width / 2);

                    // the vertical stem
                    path = [
                        'M',
                        crispX, mathRound(point.yBottom),
                        'L',
                        crispX, mathRound(point.plotY)
                    ];

                    // open
                    if (point.open !== null) {
                        plotOpen = mathRound(point.plotOpen) + crispCorr;
                        path.push(
                            'M',
                            crispX,
                            plotOpen,
                            'L',
                            crispX - halfWidth,
                            plotOpen
                        );
                    }

                    // close
                    if (point.close !== null) {
                        plotClose = mathRound(point.plotClose) + crispCorr;
                        path.push(
                            'M',
                            crispX,
                            plotClose,
                            'L',
                            crispX + halfWidth,
                            plotClose
                        );
                    }

                    // create and/or update the graphic
                    if (graphic) {
                        graphic
                            .attr(pointAttr) // #3897
                            .animate({ d: path });
                    } else {
                        point.graphic = chart.renderer.path(path)
                            .attr(pointAttr)
                            .add(series.group);
                    }

                }


            });

        },

        /**
         * Disable animation
         */
        animate: null


    });
    seriesTypes.ohlc = OHLCSeries;
    /* ****************************************************************************
     * End OHLC series code                                                       *
     *****************************************************************************/
    /* ****************************************************************************
     * Start Candlestick series code                                              *
     *****************************************************************************/

    // 1 - set default options
    defaultPlotOptions.candlestick = merge(defaultPlotOptions.column, {
        lineColor: 'black',
        lineWidth: 1,
        states: {
            hover: {
                lineWidth: 2
            }
        },
        tooltip: defaultPlotOptions.ohlc.tooltip,
        threshold: null,
        upColor: 'white'
        // upLineColor: null
    });

    // 2 - Create the CandlestickSeries object
    var CandlestickSeries = extendClass(OHLCSeries, {
        type: 'candlestick',

        /**
         * One-to-one mapping from options to SVG attributes
         */
        pointAttrToOptions: { // mapping between SVG attributes and the corresponding options
            fill: 'color',
            stroke: 'lineColor',
            'stroke-width': 'lineWidth'
        },
        upColorProp: 'fill',

        /**
         * Postprocess mapping between options and SVG attributes
         */
        getAttribs: function () {
            seriesTypes.ohlc.prototype.getAttribs.apply(this, arguments);
            var series = this,
                options = series.options,
                stateOptions = options.states,
                upLineColor = options.upLineColor || options.lineColor,
                hoverStroke = stateOptions.hover.upLineColor || upLineColor,
                selectStroke = stateOptions.select.upLineColor || upLineColor;

            // Add custom line color for points going up (close > open).
            // Fill is handled by OHLCSeries' getAttribs.
            each(series.points, function (point) {
                if (point.open < point.close) {

                    // If an individual line color is set, we need to merge the
                    // point attributes, because they are shared between all up
                    // points by inheritance from OHCLSeries.
                    if (point.lineColor) {
                        point.pointAttr = merge(point.pointAttr);
                        upLineColor = point.lineColor;
                    }

                    point.pointAttr[''].stroke = upLineColor;
                    point.pointAttr.hover.stroke = hoverStroke;
                    point.pointAttr.select.stroke = selectStroke;
                }
            });
        },

        /**
         * Draw the data points
         */
        drawPoints: function () {
            var series = this,  //state = series.state,
                points = series.points,
                chart = series.chart,
                pointAttr,
                seriesPointAttr = series.pointAttr[''],
                plotOpen,
                plotClose,
                topBox,
                bottomBox,
                hasTopWhisker,
                hasBottomWhisker,
                crispCorr,
                crispX,
                graphic,
                path,
                halfWidth;


            each(points, function (point) {

                graphic = point.graphic;
                if (point.plotY !== UNDEFINED) {

                    pointAttr = point.pointAttr[point.selected ? 'selected' : ''] || seriesPointAttr;

                    // crisp vector coordinates
                    crispCorr = (pointAttr['stroke-width'] % 2) / 2;
                    crispX = mathRound(point.plotX) - crispCorr; // #2596
                    plotOpen = point.plotOpen;
                    plotClose = point.plotClose;
                    topBox = math.min(plotOpen, plotClose);
                    bottomBox = math.max(plotOpen, plotClose);
                    halfWidth = mathRound(point.shapeArgs.width / 2);
                    hasTopWhisker = mathRound(topBox) !== mathRound(point.plotY);
                    hasBottomWhisker = bottomBox !== point.yBottom;
                    topBox = mathRound(topBox) + crispCorr;
                    bottomBox = mathRound(bottomBox) + crispCorr;

                    // Create the path. Due to a bug in Chrome 49, the path is first instanciated
                    // with no values, then the values pushed. For unknown reasons, instanciated
                    // the path array with all the values would lead to a crash when updating
                    // frequently (#5193).
                    path = [];
                    path.push(
                        'M',
                        crispX - halfWidth, bottomBox,
                        'L',
                        crispX - halfWidth, topBox,
                        'L',
                        crispX + halfWidth, topBox,
                        'L',
                        crispX + halfWidth, bottomBox,
                        'Z', // Use a close statement to ensure a nice rectangle #2602
                        'M',
                        crispX, topBox,
                        'L',
                        crispX, hasTopWhisker ? mathRound(point.plotY) : topBox, // #460, #2094
                        'M',
                        crispX, bottomBox,
                        'L',
                        crispX, hasBottomWhisker ? mathRound(point.yBottom) : bottomBox // #460, #2094
                    );

                    if (graphic) {
                        graphic
                            .attr(pointAttr) // #3897
                            .animate({ d: path });
                    } else {
                        point.graphic = chart.renderer.path(path)
                            .attr(pointAttr)
                            .add(series.group)
                            .shadow(series.options.shadow);
                    }

                }
            });

        }


    });

    seriesTypes.candlestick = CandlestickSeries;

    /* ****************************************************************************
     * End Candlestick series code                                                *
     *****************************************************************************/
    /* ****************************************************************************
     * Start Flags series code                                                    *
     *****************************************************************************/

    var symbols = SVGRenderer.prototype.symbols;

    // 1 - set default options
    defaultPlotOptions.flags = merge(defaultPlotOptions.column, {
        fillColor: 'white',
        lineWidth: 1,
        pointRange: 0, // #673
        //radius: 2,
        shape: 'flag',
        stackDistance: 12,
        states: {
            hover: {
                lineColor: 'black',
                fillColor: '#FCFFC5'
            }
        },
        style: {
            fontSize: '11px',
            fontWeight: 'bold',
            textAlign: 'center'
        },
        tooltip: {
            pointFormat: '{point.text}<br/>'
        },
        threshold: null,
        y: -30
    });

    // 2 - Create the CandlestickSeries object
    seriesTypes.flags = extendClass(seriesTypes.column, {
        type: 'flags',
        sorted: false,
        noSharedTooltip: true,
        allowDG: false,
        takeOrdinalPosition: false, // #1074
        trackerGroups: ['markerGroup'],
        forceCrop: true,
        /**
         * Inherit the initialization from base Series
         */
        init: Series.prototype.init,

        /**
         * One-to-one mapping from options to SVG attributes
         */
        pointAttrToOptions: { // mapping between SVG attributes and the corresponding options
            fill: 'fillColor',
            stroke: 'color',
            'stroke-width': 'lineWidth',
            r: 'radius'
        },

        /**
         * Extend the translate method by placing the point on the related series
         */
        translate: function () {

            seriesTypes.column.prototype.translate.apply(this);

            var series = this,
                options = series.options,
                chart = series.chart,
                points = series.points,
                cursor = points.length - 1,
                point,
                lastPoint,
                optionsOnSeries = options.onSeries,
                onSeries = optionsOnSeries && chart.get(optionsOnSeries),
                onKey = options.onKey || 'y',
                step = onSeries && onSeries.options.step,
                onData = onSeries && onSeries.points,
                i = onData && onData.length,
                xAxis = series.xAxis,
                xAxisExt = xAxis.getExtremes(),
                leftPoint,
                lastX,
                rightPoint,
                currentDataGrouping;

            // relate to a master series
            if (onSeries && onSeries.visible && i) {
                currentDataGrouping = onSeries.currentDataGrouping;
                lastX = onData[i - 1].x + (currentDataGrouping ? currentDataGrouping.totalRange : 0); // #2374

                // sort the data points
                points.sort(function (a, b) {
                    return (a.x - b.x);
                });

                onKey = 'plot' + onKey[0].toUpperCase() + onKey.substr(1);
                while (i-- && points[cursor]) {
                    point = points[cursor];
                    leftPoint = onData[i];
                    if (leftPoint.x <= point.x && leftPoint[onKey] !== undefined) {
                        if (point.x <= lastX) { // #803

                            point.plotY = leftPoint[onKey];

                            // interpolate between points, #666
                            if (leftPoint.x < point.x && !step) {
                                rightPoint = onData[i + 1];
                                if (rightPoint && rightPoint[onKey] !== UNDEFINED) {
                                    point.plotY +=
                                        ((point.x - leftPoint.x) / (rightPoint.x - leftPoint.x)) * // the distance ratio, between 0 and 1
                                        (rightPoint[onKey] - leftPoint[onKey]); // the y distance
                                }
                            }
                        }
                        cursor--;
                        i++; // check again for points in the same x position
                        if (cursor < 0) {
                            break;
                        }
                    }
                }
            }

            // Add plotY position and handle stacking
            each(points, function (point, i) {

                var stackIndex;

                // Undefined plotY means the point is either on axis, outside series range or hidden series.
                // If the series is outside the range of the x axis it should fall through with
                // an undefined plotY, but then we must remove the shapeArgs (#847).
                if (point.plotY === UNDEFINED) {
                    if (point.x >= xAxisExt.min && point.x <= xAxisExt.max) { // we're inside xAxis range
                        point.plotY = chart.chartHeight - xAxis.bottom - (xAxis.opposite ? xAxis.height : 0) + xAxis.offset - chart.plotTop;
                    } else {
                        point.shapeArgs = {}; // 847
                    }
                }
                // if multiple flags appear at the same x, order them into a stack
                lastPoint = points[i - 1];
                if (lastPoint && lastPoint.plotX === point.plotX) {
                    if (lastPoint.stackIndex === UNDEFINED) {
                        lastPoint.stackIndex = 0;
                    }
                    stackIndex = lastPoint.stackIndex + 1;
                }
                point.stackIndex = stackIndex; // #3639
            });


        },

        /**
         * Draw the markers
         */
        drawPoints: function () {
            var series = this,
                pointAttr,
                seriesPointAttr = series.pointAttr[''],
                points = series.points,
                chart = series.chart,
                renderer = chart.renderer,
                plotX,
                plotY,
                options = series.options,
                optionsY = options.y,
                shape,
                i,
                point,
                graphic,
                stackIndex,
                anchorX,
                anchorY,
                outsideRight,
                yAxis = series.yAxis;

            i = points.length;
            while (i--) {
                point = points[i];
                outsideRight = point.plotX > series.xAxis.len;
                plotX = point.plotX;
                if (plotX > 0) { // #3119
                    plotX -= pick(point.lineWidth, options.lineWidth) % 2; // #4285
                }
                stackIndex = point.stackIndex;
                shape = point.options.shape || options.shape;
                plotY = point.plotY;
                if (plotY !== UNDEFINED) {
                    plotY = point.plotY + optionsY - (stackIndex !== UNDEFINED && stackIndex * options.stackDistance);
                }
                anchorX = stackIndex ? UNDEFINED : point.plotX; // skip connectors for higher level stacked points
                anchorY = stackIndex ? UNDEFINED : point.plotY;

                graphic = point.graphic;

                // only draw the point if y is defined and the flag is within the visible area
                if (plotY !== UNDEFINED && plotX >= 0 && !outsideRight) {
                    // shortcuts
                    pointAttr = point.pointAttr[point.selected ? 'select' : ''] || seriesPointAttr;
                    if (graphic) { // update
                        graphic.attr({
                            x: plotX,
                            y: plotY,
                            r: pointAttr.r,
                            anchorX: anchorX,
                            anchorY: anchorY
                        });
                    } else {
                        graphic = point.graphic = renderer.label(
                            point.options.title || options.title || 'A',
                            plotX,
                            plotY,
                            shape,
                            anchorX,
                            anchorY,
                            options.useHTML
                        )
                        .css(merge(options.style, point.style))
                        .attr(pointAttr)
                        .attr({
                            align: shape === 'flag' ? 'left' : 'center',
                            width: options.width,
                            height: options.height
                        })
                        .add(series.markerGroup)
                        .shadow(options.shadow);

                    }

                    // Set the tooltip anchor position
                    point.tooltipPos = chart.inverted ? [yAxis.len + yAxis.pos - chart.plotLeft - plotY, series.xAxis.len - plotX] : [plotX, plotY];

                } else if (graphic) {
                    point.graphic = graphic.destroy();
                }

            }

        },

        /**
         * Extend the column trackers with listeners to expand and contract stacks
         */
        drawTracker: function () {
            var series = this,
                points = series.points;

            TrackerMixin.drawTrackerPoint.apply(this);

            // Bring each stacked flag up on mouse over, this allows readability of vertically
            // stacked elements as well as tight points on the x axis. #1924.
            each(points, function (point) {
                var graphic = point.graphic;
                if (graphic) {
                    addEvent(graphic.element, 'mouseover', function () {

                        // Raise this point
                        if (point.stackIndex > 0 && !point.raised) {
                            point._y = graphic.y;
                            graphic.attr({
                                y: point._y - 8
                            });
                            point.raised = true;
                        }

                        // Revert other raised points
                        each(points, function (otherPoint) {
                            if (otherPoint !== point && otherPoint.raised && otherPoint.graphic) {
                                otherPoint.graphic.attr({
                                    y: otherPoint._y
                                });
                                otherPoint.raised = false;
                            }
                        });
                    });
                }
            });
        },

        /**
         * Disable animation
         */
        animate: noop,
        buildKDTree: noop,
        setClip: noop

    });

    // create the flag icon with anchor
    symbols.flag = function (x, y, w, h, options) {
        var anchorX = (options && options.anchorX) || x,
            anchorY = (options &&  options.anchorY) || y;

        return [
            'M', anchorX, anchorY,
            'L', x, y + h,
            x, y,
            x + w, y,
            x + w, y + h,
            x, y + h,
            'Z'
        ];
    };

    // create the circlepin and squarepin icons with anchor
    each(['circle', 'square'], function (shape) {
        symbols[shape + 'pin'] = function (x, y, w, h, options) {

            var anchorX = options && options.anchorX,
                anchorY = options &&  options.anchorY,
                path,
                labelTopOrBottomY;

            // For single-letter flags, make sure circular flags are not taller than their width
            if (shape === 'circle' && h > w) {
                x -= mathRound((h - w) / 2);
                w = h;
            }

            path = symbols[shape](x, y, w, h);

            if (anchorX && anchorY) {
                // if the label is below the anchor, draw the connecting line from the top edge of the label
                // otherwise start drawing from the bottom edge
                labelTopOrBottomY = (y > anchorY) ? y : y + h;
                path.push('M', anchorX, labelTopOrBottomY, 'L', anchorX, anchorY);
            }

            return path;
        };
    });

    // The symbol callbacks are generated on the SVGRenderer object in all browsers. Even
    // VML browsers need this in order to generate shapes in export. Now share
    // them with the VMLRenderer.
    if (Renderer === Highcharts.VMLRenderer) {
        each(['flag', 'circlepin', 'squarepin'], function (shape) {
            VMLRenderer.prototype.symbols[shape] = symbols[shape];
        });
    }

    /* ****************************************************************************
     * End Flags series code                                                      *
     *****************************************************************************/
    /* ****************************************************************************
     * Start Scroller code                                                        *
     *****************************************************************************/
    var units = [].concat(defaultDataGroupingUnits), // copy
        defaultSeriesType,

        // Finding the min or max of a set of variables where we don't know if they are defined,
        // is a pattern that is repeated several places in Highcharts. Consider making this
        // a global utility method.
        numExt = function (extreme) {
            var numbers = grep(arguments, function (n) {
                return isNumber(n);
            });
            if (numbers.length) {
                return Math[extreme].apply(0, numbers);
            }
        };

    // add more resolution to units
    units[4] = ['day', [1, 2, 3, 4]]; // allow more days
    units[5] = ['week', [1, 2, 3]]; // allow more weeks

    defaultSeriesType = seriesTypes.areaspline === UNDEFINED ? 'line' : 'areaspline';

    extend(defaultOptions, {
        navigator: {
            //enabled: true,
            handles: {
                backgroundColor: '#ebe7e8',
                borderColor: '#b2b1b6'
            },
            height: 40,
            margin: 25,
            maskFill: 'rgba(128,179,236,0.3)',
            maskInside: true,
            outlineColor: '#b2b1b6',
            outlineWidth: 1,
            series: {
                type: defaultSeriesType,
                color: '#4572A7',
                compare: null,
                fillOpacity: 0.05,
                dataGrouping: {
                    approximation: 'average',
                    enabled: true,
                    groupPixelWidth: 2,
                    smoothed: true,
                    units: units
                },
                dataLabels: {
                    enabled: false,
                    zIndex: 2 // #1839
                },
                id: PREFIX + 'navigator-series',
                lineColor: null, // Allow color setting while disallowing default candlestick setting (#4602)
                lineWidth: 1,
                marker: {
                    enabled: false
                },
                pointRange: 0,
                shadow: false,
                threshold: null
            },
            //top: undefined,
            xAxis: {
                tickWidth: 0,
                lineWidth: 0,
                gridLineColor: '#EEE',
                gridLineWidth: 1,
                tickPixelInterval: 200,
                labels: {
                    align: 'left',
                    style: {
                        color: '#888'
                    },
                    x: 3,
                    y: -4
                },
                crosshair: false
            },
            yAxis: {
                gridLineWidth: 0,
                startOnTick: false,
                endOnTick: false,
                minPadding: 0.1,
                maxPadding: 0.1,
                labels: {
                    enabled: false
                },
                crosshair: false,
                title: {
                    text: null
                },
                tickWidth: 0
            }
        },
        scrollbar: {
            //enabled: true
            height: isTouchDevice ? 20 : 14,
            barBackgroundColor: '#bfc8d1',
            barBorderRadius: 0,
            barBorderWidth: 1,
            barBorderColor: '#bfc8d1',
            buttonArrowColor: '#666',
            buttonBackgroundColor: '#ebe7e8',
            buttonBorderColor: '#bbb',
            buttonBorderRadius: 0,
            buttonBorderWidth: 1,
            minWidth: 6,
            rifleColor: '#666',
            trackBackgroundColor: '#eeeeee',
            trackBorderColor: '#eeeeee',
            trackBorderWidth: 1,
            // trackBorderRadius: 0
            liveRedraw: hasSVG && !isTouchDevice
        }
    });

    /**
     * The Scroller class
     * @param {Object} chart
     */
    function Scroller(chart) {
        var chartOptions = chart.options,
            navigatorOptions = chartOptions.navigator,
            navigatorEnabled = navigatorOptions.enabled,
            scrollbarOptions = chartOptions.scrollbar,
            scrollbarEnabled = scrollbarOptions.enabled,
            height = navigatorEnabled ? navigatorOptions.height : 0,
            scrollbarHeight = scrollbarEnabled ? scrollbarOptions.height : 0;


        this.handles = [];
        this.scrollbarButtons = [];
        this.elementsToDestroy = []; // Array containing the elements to destroy when Scroller is destroyed

        this.chart = chart;
        this.setBaseSeries();

        this.height = height;
        this.scrollbarHeight = scrollbarHeight;
        this.scrollbarEnabled = scrollbarEnabled;
        this.navigatorEnabled = navigatorEnabled;
        this.navigatorOptions = navigatorOptions;
        this.scrollbarOptions = scrollbarOptions;
        this.outlineHeight = height + scrollbarHeight;

        // Run scroller
        this.init();
    }

    Scroller.prototype = {
        /**
         * Draw one of the handles on the side of the zoomed range in the navigator
         * @param {Number} x The x center for the handle
         * @param {Number} index 0 for left and 1 for right
         */
        drawHandle: function (x, index) {
            var scroller = this,
                chart = scroller.chart,
                renderer = chart.renderer,
                elementsToDestroy = scroller.elementsToDestroy,
                handles = scroller.handles,
                handlesOptions = scroller.navigatorOptions.handles,
                attr = {
                    fill: handlesOptions.backgroundColor,
                    stroke: handlesOptions.borderColor,
                    'stroke-width': 1
                },
                tempElem;

            // create the elements
            if (!scroller.rendered) {
                // the group
                handles[index] = renderer.g('navigator-handle-' + ['left', 'right'][index])
                    .css({ cursor: 'ew-resize' })
                    .attr({ zIndex: 10 - index }) // zIndex = 3 for right handle, 4 for left / 10 - #2908
                    .add();

                // the rectangle
                tempElem = renderer.rect(-4.5, 0, 9, 16, 0, 1)
                    .attr(attr)
                    .add(handles[index]);
                elementsToDestroy.push(tempElem);

                // the rifles
                tempElem = renderer
                    .path([
                        'M',
                        -1.5, 4,
                        'L',
                        -1.5, 12,
                        'M',
                        0.5, 4,
                        'L',
                        0.5, 12
                    ]).attr(attr)
                    .add(handles[index]);
                elementsToDestroy.push(tempElem);
            }

            // Place it
            handles[index][scroller.rendered ? 'animate' : 'attr']({
                translateX: scroller.scrollerLeft + scroller.scrollbarHeight + parseInt(x, 10),
                translateY: scroller.top + scroller.height / 2 - 8
            });
        },

        /**
         * Draw the scrollbar buttons with arrows
         * @param {Number} index 0 is left, 1 is right
         */
        drawScrollbarButton: function (index) {
            var scroller = this,
                chart = scroller.chart,
                renderer = chart.renderer,
                elementsToDestroy = scroller.elementsToDestroy,
                scrollbarButtons = scroller.scrollbarButtons,
                scrollbarHeight = scroller.scrollbarHeight,
                scrollbarOptions = scroller.scrollbarOptions,
                tempElem;

            if (!scroller.rendered) {
                scrollbarButtons[index] = renderer.g().add(scroller.scrollbarGroup);

                tempElem = renderer.rect(
                        -0.5,
                        -0.5,
                        scrollbarHeight + 1, // +1 to compensate for crispifying in rect method
                        scrollbarHeight + 1,
                        scrollbarOptions.buttonBorderRadius,
                        scrollbarOptions.buttonBorderWidth
                    ).attr({
                        stroke: scrollbarOptions.buttonBorderColor,
                        'stroke-width': scrollbarOptions.buttonBorderWidth,
                        fill: scrollbarOptions.buttonBackgroundColor
                    }).add(scrollbarButtons[index]);
                elementsToDestroy.push(tempElem);

                tempElem = renderer
                    .path([
                        'M',
                        scrollbarHeight / 2 + (index ? -1 : 1), scrollbarHeight / 2 - 3,
                        'L',
                        scrollbarHeight / 2 + (index ? -1 : 1), scrollbarHeight / 2 + 3,
                        scrollbarHeight / 2 + (index ? 2 : -2), scrollbarHeight / 2
                    ]).attr({
                        fill: scrollbarOptions.buttonArrowColor
                    }).add(scrollbarButtons[index]);
                elementsToDestroy.push(tempElem);
            }

            // adjust the right side button to the varying length of the scroll track
            if (index) {
                scrollbarButtons[index].attr({
                    translateX: scroller.scrollerWidth - scrollbarHeight
                });
            }
        },

        /**
         * Render the navigator and scroll bar
         * @param {Number} min X axis value minimum
         * @param {Number} max X axis value maximum
         * @param {Number} pxMin Pixel value minimum
         * @param {Number} pxMax Pixel value maximum
         */
        render: function (min, max, pxMin, pxMax) {
            var scroller = this,
                chart = scroller.chart,
                renderer = chart.renderer,
                navigatorLeft,
                navigatorWidth,
                scrollerLeft,
                scrollerWidth,
                scrollbarGroup = scroller.scrollbarGroup,
                navigatorGroup = scroller.navigatorGroup,
                scrollbar = scroller.scrollbar,
                xAxis = scroller.xAxis,
                scrollbarTrack = scroller.scrollbarTrack,
                scrollbarHeight = scroller.scrollbarHeight,
                scrollbarEnabled = scroller.scrollbarEnabled,
                navigatorOptions = scroller.navigatorOptions,
                scrollbarOptions = scroller.scrollbarOptions,
                scrollbarMinWidth = scrollbarOptions.minWidth,
                height = scroller.height,
                top = scroller.top,
                navigatorEnabled = scroller.navigatorEnabled,
                outlineWidth = navigatorOptions.outlineWidth,
                halfOutline = outlineWidth / 2,
                zoomedMin,
                zoomedMax,
                range,
                scrX,
                scrWidth,
                scrollbarPad = 0,
                outlineHeight = scroller.outlineHeight,
                barBorderRadius = scrollbarOptions.barBorderRadius,
                strokeWidth,
                scrollbarStrokeWidth = scrollbarOptions.barBorderWidth,
                centerBarX,
                outlineTop = top + halfOutline,
                rendered = scroller.rendered,
                verb;

            // Don't render the navigator until we have data (#486, #4202, #5172). Don't redraw while moving the handles (#4703).
            if (!isNumber(min) || !isNumber(max) ||    (scroller.hasDragged && !defined(pxMin))) {
                return;
            }

            scroller.navigatorLeft = navigatorLeft = pick(
                xAxis.left,
                chart.plotLeft + scrollbarHeight // in case of scrollbar only, without navigator
            );
            scroller.navigatorWidth = navigatorWidth = pick(xAxis.len, chart.plotWidth - 2 * scrollbarHeight);
            scroller.scrollerLeft = scrollerLeft = navigatorLeft - scrollbarHeight;
            scroller.scrollerWidth = scrollerWidth = scrollerWidth = navigatorWidth + 2 * scrollbarHeight;

            // Get the pixel position of the handles
            pxMin = pick(pxMin, xAxis.translate(min));
            pxMax = pick(pxMax, xAxis.translate(max));
            if (!isNumber(pxMin) || mathAbs(pxMin) === Infinity) { // Verify (#1851, #2238)
                pxMin = 0;
                pxMax = scrollerWidth;
            }

            // Are we below the minRange? (#2618)
            if (xAxis.translate(pxMax, true) - xAxis.translate(pxMin, true) < chart.xAxis[0].minRange) {
                return;
            }


            // handles are allowed to cross, but never exceed the plot area
            scroller.zoomedMax = mathMin(mathMax(pxMin, pxMax, 0), navigatorWidth);
            scroller.zoomedMin = mathMin(mathMax(scroller.fixedWidth ? scroller.zoomedMax - scroller.fixedWidth : mathMin(pxMin, pxMax), 0), navigatorWidth);
            scroller.range = scroller.zoomedMax - scroller.zoomedMin;
            zoomedMax = mathRound(scroller.zoomedMax);
            zoomedMin = mathRound(scroller.zoomedMin);
            range = zoomedMax - zoomedMin;

            if (!rendered) {

                if (navigatorEnabled) {

                    // draw the navigator group
                    scroller.navigatorGroup = navigatorGroup = renderer.g('navigator')
                        .attr({
                            zIndex: 3
                        })
                        .add();

                    scroller.leftShade = renderer.rect()
                        .attr({
                            fill: navigatorOptions.maskFill
                        }).add(navigatorGroup);

                    if (navigatorOptions.maskInside) {
                        scroller.leftShade.css({ cursor: 'ew-resize' });
                    } else {
                        scroller.rightShade = renderer.rect()
                            .attr({
                                fill: navigatorOptions.maskFill
                            }).add(navigatorGroup);
                    }


                    scroller.outline = renderer.path()
                        .attr({
                            'stroke-width': outlineWidth,
                            stroke: navigatorOptions.outlineColor
                        })
                        .add(navigatorGroup);
                }

                if (scrollbarEnabled) {

                    // draw the scrollbar group
                    scroller.scrollbarGroup = scrollbarGroup = renderer.g('scrollbar').add();

                    // the scrollbar track
                    strokeWidth = scrollbarOptions.trackBorderWidth;
                    scroller.scrollbarTrack = scrollbarTrack = renderer.rect().attr({
                        x: 0,
                        y: -strokeWidth % 2 / 2,
                        fill: scrollbarOptions.trackBackgroundColor,
                        stroke: scrollbarOptions.trackBorderColor,
                        'stroke-width': strokeWidth,
                        r: scrollbarOptions.trackBorderRadius || 0,
                        height: scrollbarHeight
                    }).add(scrollbarGroup);

                    // the scrollbar itself
                    scroller.scrollbar = scrollbar = renderer.rect()
                        .attr({
                            y: -scrollbarStrokeWidth % 2 / 2,
                            height: scrollbarHeight,
                            fill: scrollbarOptions.barBackgroundColor,
                            stroke: scrollbarOptions.barBorderColor,
                            'stroke-width': scrollbarStrokeWidth,
                            r: barBorderRadius
                        })
                        .add(scrollbarGroup);

                    scroller.scrollbarRifles = renderer.path()
                        .attr({
                            stroke: scrollbarOptions.rifleColor,
                            'stroke-width': 1
                        })
                        .add(scrollbarGroup);
                }
            }

            // place elements
            verb = rendered ? 'animate' : 'attr';
            if (navigatorEnabled) {
                scroller.leftShade[verb](navigatorOptions.maskInside ? {
                    x: navigatorLeft + zoomedMin,
                    y: top,
                    width: zoomedMax - zoomedMin,
                    height: height
                } : {
                    x: navigatorLeft,
                    y: top,
                    width: zoomedMin,
                    height: height
                });
                if (scroller.rightShade) {
                    scroller.rightShade[verb]({
                        x: navigatorLeft + zoomedMax,
                        y: top,
                        width: navigatorWidth - zoomedMax,
                        height: height
                    });
                }

                scroller.outline[verb]({ d: [
                    M,
                    scrollerLeft, outlineTop, // left
                    L,
                    navigatorLeft + zoomedMin - halfOutline, outlineTop, // upper left of zoomed range
                    navigatorLeft + zoomedMin - halfOutline, outlineTop + outlineHeight, // lower left of z.r.
                    L,
                    navigatorLeft + zoomedMax - halfOutline, outlineTop + outlineHeight, // lower right of z.r.
                    L,
                    navigatorLeft + zoomedMax - halfOutline, outlineTop, // upper right of z.r.
                    scrollerLeft + scrollerWidth, outlineTop // right
                ].concat(navigatorOptions.maskInside ? [
                    M,
                    navigatorLeft + zoomedMin + halfOutline, outlineTop, // upper left of zoomed range
                    L,
                    navigatorLeft + zoomedMax - halfOutline, outlineTop // upper right of z.r.
                ] : []) });
                // draw handles
                scroller.drawHandle(zoomedMin + halfOutline, 0);
                scroller.drawHandle(zoomedMax + halfOutline, 1);
            }

            // draw the scrollbar
            if (scrollbarEnabled && scrollbarGroup) {

                // draw the buttons
                scroller.drawScrollbarButton(0);
                scroller.drawScrollbarButton(1);

                scrollbarGroup[verb]({
                    translateX: scrollerLeft,
                    translateY: mathRound(outlineTop + height)
                });

                scrollbarTrack[verb]({
                    width: scrollerWidth
                });

                // prevent the scrollbar from drawing to small (#1246)
                scrX = scrollbarHeight + zoomedMin;
                scrWidth = range - scrollbarStrokeWidth;
                if (scrWidth < scrollbarMinWidth) {
                    scrollbarPad = (scrollbarMinWidth - scrWidth) / 2;
                    scrWidth = scrollbarMinWidth;
                    scrX -= scrollbarPad;
                }
                scroller.scrollbarPad = scrollbarPad;
                scrollbar[verb]({
                    x: mathFloor(scrX) + (scrollbarStrokeWidth % 2 / 2),
                    width: scrWidth
                });

                centerBarX = scrollbarHeight + zoomedMin + range / 2 - 0.5;

                scroller.scrollbarRifles
                    .attr({
                        visibility: range > 12 ? VISIBLE : HIDDEN
                    })[verb]({
                        d: [
                            M,
                            centerBarX - 3, scrollbarHeight / 4,
                            L,
                            centerBarX - 3, 2 * scrollbarHeight / 3,
                            M,
                            centerBarX, scrollbarHeight / 4,
                            L,
                            centerBarX, 2 * scrollbarHeight / 3,
                            M,
                            centerBarX + 3, scrollbarHeight / 4,
                            L,
                            centerBarX + 3, 2 * scrollbarHeight / 3
                        ]
                    });
            }

            scroller.scrollbarPad = scrollbarPad;
            scroller.rendered = true;
        },

        /**
         * Set up the mouse and touch events for the navigator and scrollbar
         */
        addEvents: function () {
            var chart = this.chart,
                container = chart.container,
                mouseDownHandler = this.mouseDownHandler,
                mouseMoveHandler = this.mouseMoveHandler,
                mouseUpHandler = this.mouseUpHandler,
                _events;

            // Mouse events
            _events = [
                [container, 'mousedown', mouseDownHandler],
                [container, 'mousemove', mouseMoveHandler],
                [doc, 'mouseup', mouseUpHandler]
            ];

            // Touch events
            if (hasTouch) {
                _events.push(
                    [container, 'touchstart', mouseDownHandler],
                    [container, 'touchmove', mouseMoveHandler],
                    [doc, 'touchend', mouseUpHandler]
                );
            }

            // Add them all
            each(_events, function (args) {
                addEvent.apply(null, args);
            });
            this._events = _events;

            // Data events
            if (this.series) {
                addEvent(this.series.xAxis, 'foundExtremes', function () {
                    chart.scroller.modifyNavigatorAxisExtremes();
                });
            }
            addEvent(chart, 'redraw', function () {
                // Move the scrollbar after redraw, like after data updata even if axes don't redraw
                var scroller = this.scroller,
                    xAxis;
                if (scroller) {
                    xAxis = scroller.baseSeries.xAxis;
                    if (xAxis) {
                        scroller.render(xAxis.min, xAxis.max);
                    }
                }
            });
        },

        /**
         * Removes the event handlers attached previously with addEvents.
         */
        removeEvents: function () {

            each(this._events, function (args) {
                removeEvent.apply(null, args);
            });
            this._events = UNDEFINED;
            if (this.navigatorEnabled && this.baseSeries) {
                removeEvent(this.baseSeries, 'updatedData', this.updatedDataHandler);
            }
        },

        /**
         * Initiate the Scroller object
         */
        init: function () {
            var scroller = this,
                chart = scroller.chart,
                xAxis,
                yAxis,
                scrollbarHeight = scroller.scrollbarHeight,
                navigatorOptions = scroller.navigatorOptions,
                height = scroller.height,
                top = scroller.top,
                dragOffset,
                baseSeries = scroller.baseSeries;

            /**
             * Event handler for the mouse down event.
             */
            scroller.mouseDownHandler = function (e) {
                e = chart.pointer.normalize(e);

                var zoomedMin = scroller.zoomedMin,
                    zoomedMax = scroller.zoomedMax,
                    top = scroller.top,
                    scrollbarHeight = scroller.scrollbarHeight,
                    scrollerLeft = scroller.scrollerLeft,
                    scrollerWidth = scroller.scrollerWidth,
                    navigatorLeft = scroller.navigatorLeft,
                    navigatorWidth = scroller.navigatorWidth,
                    scrollbarPad = scroller.scrollbarPad,
                    range = scroller.range,
                    chartX = e.chartX,
                    chartY = e.chartY,
                    baseXAxis = chart.xAxis[0],
                    fixedMax,
                    ext,
                    handleSensitivity = isTouchDevice ? 10 : 7,
                    left,
                    isOnNavigator;

                if (chartY > top && chartY < top + height + scrollbarHeight) { // we're vertically inside the navigator
                    isOnNavigator = !scroller.scrollbarEnabled || chartY < top + height;

                    // grab the left handle
                    if (isOnNavigator && math.abs(chartX - zoomedMin - navigatorLeft) < handleSensitivity) {
                        scroller.grabbedLeft = true;
                        scroller.otherHandlePos = zoomedMax;
                        scroller.fixedExtreme = baseXAxis.max;
                        chart.fixedRange = null;

                    // grab the right handle
                    } else if (isOnNavigator && math.abs(chartX - zoomedMax - navigatorLeft) < handleSensitivity) {
                        scroller.grabbedRight = true;
                        scroller.otherHandlePos = zoomedMin;
                        scroller.fixedExtreme = baseXAxis.min;
                        chart.fixedRange = null;

                    // grab the zoomed range
                    } else if (chartX > navigatorLeft + zoomedMin - scrollbarPad && chartX < navigatorLeft + zoomedMax + scrollbarPad) {
                        scroller.grabbedCenter = chartX;
                        scroller.fixedWidth = range;

                        dragOffset = chartX - zoomedMin;


                    // shift the range by clicking on shaded areas, scrollbar track or scrollbar buttons
                    } else if (chartX > scrollerLeft && chartX < scrollerLeft + scrollerWidth) {

                        // Center around the clicked point
                        if (isOnNavigator) {
                            left = chartX - navigatorLeft - range / 2;

                        // Click on scrollbar
                        } else {

                            // Click left scrollbar button
                            if (chartX < navigatorLeft) {
                                left = zoomedMin - range * 0.2;

                            // Click right scrollbar button
                            } else if (chartX > scrollerLeft + scrollerWidth - scrollbarHeight) {
                                left = zoomedMin + range * 0.2;

                            // Click on scrollbar track, shift the scrollbar by one range
                            } else {
                                left = chartX < navigatorLeft + zoomedMin ? // on the left
                                    zoomedMin - range :
                                    zoomedMax;
                            }
                        }
                        if (left < 0) {
                            left = 0;
                        } else if (left + range >= navigatorWidth) {
                            left = navigatorWidth - range;
                            fixedMax = scroller.getUnionExtremes().dataMax; // #2293, #3543
                        }
                        if (left !== zoomedMin) { // it has actually moved
                            scroller.fixedWidth = range; // #1370

                            ext = xAxis.toFixedRange(left, left + range, null, fixedMax);
                            baseXAxis.setExtremes(
                                ext.min,
                                ext.max,
                                true,
                                false,
                                { trigger: 'navigator' }
                            );
                        }
                    }

                }
            };

            /**
             * Event handler for the mouse move event.
             */
            scroller.mouseMoveHandler = function (e) {
                var scrollbarHeight = scroller.scrollbarHeight,
                    navigatorLeft = scroller.navigatorLeft,
                    navigatorWidth = scroller.navigatorWidth,
                    scrollerLeft = scroller.scrollerLeft,
                    scrollerWidth = scroller.scrollerWidth,
                    range = scroller.range,
                    chartX,
                    hasDragged;

                // In iOS, a mousemove event with e.pageX === 0 is fired when holding the finger
                // down in the center of the scrollbar. This should be ignored.
                if (!e.touches || e.touches[0].pageX !== 0) { // #4696, scrollbar failed on Android

                    e = chart.pointer.normalize(e);
                    chartX = e.chartX;

                    // validation for handle dragging
                    if (chartX < navigatorLeft) {
                        chartX = navigatorLeft;
                    } else if (chartX > scrollerLeft + scrollerWidth - scrollbarHeight) {
                        chartX = scrollerLeft + scrollerWidth - scrollbarHeight;
                    }

                    // drag left handle
                    if (scroller.grabbedLeft) {
                        hasDragged = true;
                        scroller.render(0, 0, chartX - navigatorLeft, scroller.otherHandlePos);

                    // drag right handle
                    } else if (scroller.grabbedRight) {
                        hasDragged = true;
                        scroller.render(0, 0, scroller.otherHandlePos, chartX - navigatorLeft);

                    // drag scrollbar or open area in navigator
                    } else if (scroller.grabbedCenter) {

                        hasDragged = true;
                        if (chartX < dragOffset) { // outside left
                            chartX = dragOffset;
                        } else if (chartX > navigatorWidth + dragOffset - range) { // outside right
                            chartX = navigatorWidth + dragOffset - range;
                        }

                        scroller.render(0, 0, chartX - dragOffset, chartX - dragOffset + range);

                    }
                    if (hasDragged && scroller.scrollbarOptions.liveRedraw) {
                        setTimeout(function () {
                            scroller.mouseUpHandler(e);
                        }, 0);
                    }
                    scroller.hasDragged = hasDragged;
                }
            };

            /**
             * Event handler for the mouse up event.
             */
            scroller.mouseUpHandler = function (e) {
                var ext,
                    fixedMin,
                    fixedMax;

                if (scroller.hasDragged) {
                    // When dragging one handle, make sure the other one doesn't change
                    if (scroller.zoomedMin === scroller.otherHandlePos) {
                        fixedMin = scroller.fixedExtreme;
                    } else if (scroller.zoomedMax === scroller.otherHandlePos) {
                        fixedMax = scroller.fixedExtreme;
                    }

                    // Snap to right edge (#4076)
                    if (scroller.zoomedMax === scroller.navigatorWidth) {
                        fixedMax = scroller.getUnionExtremes().dataMax;
                    }

                    ext = xAxis.toFixedRange(scroller.zoomedMin, scroller.zoomedMax, fixedMin, fixedMax);
                    if (defined(ext.min)) {
                        chart.xAxis[0].setExtremes(
                            ext.min,
                            ext.max,
                            true,
                            false,
                            {
                                trigger: 'navigator',
                                triggerOp: 'navigator-drag',
                                DOMEvent: e // #1838
                            }
                        );
                    }
                }

                if (e.type !== 'mousemove') {
                    scroller.grabbedLeft = scroller.grabbedRight = scroller.grabbedCenter = scroller.fixedWidth =
                        scroller.fixedExtreme = scroller.otherHandlePos = scroller.hasDragged = dragOffset = null;
                }

            };



            var xAxisIndex = chart.xAxis.length,
                yAxisIndex = chart.yAxis.length;

            // make room below the chart
            chart.extraBottomMargin = scroller.outlineHeight + navigatorOptions.margin;

            if (scroller.navigatorEnabled) {
                // an x axis is required for scrollbar also
                scroller.xAxis = xAxis = new Axis(chart, merge({
                    // inherit base xAxis' break and ordinal options
                    breaks: baseSeries && baseSeries.xAxis.options.breaks,
                    ordinal: baseSeries && baseSeries.xAxis.options.ordinal
                }, navigatorOptions.xAxis, {
                    id: 'navigator-x-axis',
                    isX: true,
                    type: 'datetime',
                    index: xAxisIndex,
                    height: height,
                    offset: 0,
                    offsetLeft: scrollbarHeight,
                    offsetRight: -scrollbarHeight,
                    keepOrdinalPadding: true, // #2436
                    startOnTick: false,
                    endOnTick: false,
                    minPadding: 0,
                    maxPadding: 0,
                    zoomEnabled: false
                }));

                scroller.yAxis = yAxis = new Axis(chart, merge(navigatorOptions.yAxis, {
                    id: 'navigator-y-axis',
                    alignTicks: false,
                    height: height,
                    offset: 0,
                    index: yAxisIndex,
                    zoomEnabled: false
                }));

                // If we have a base series, initialize the navigator series
                if (baseSeries || navigatorOptions.series.data) {
                    scroller.addBaseSeries();

                // If not, set up an event to listen for added series
                } else if (chart.series.length === 0) {

                    wrap(chart, 'redraw', function (proceed, animation) {
                        // We've got one, now add it as base and reset chart.redraw
                        if (chart.series.length > 0 && !scroller.series) {
                            scroller.setBaseSeries();
                            chart.redraw = proceed; // reset
                        }
                        proceed.call(chart, animation);
                    });
                }


            // in case of scrollbar only, fake an x axis to get translation
            } else {
                scroller.xAxis = xAxis = {
                    translate: function (value, reverse) {
                        var axis = chart.xAxis[0],
                            ext = axis.getExtremes(),
                            scrollTrackWidth = chart.plotWidth - 2 * scrollbarHeight,
                            min = numExt('min', axis.options.min, ext.dataMin),
                            valueRange = numExt('max', axis.options.max, ext.dataMax) - min;

                        return reverse ?
                            // from pixel to value
                            (value * valueRange / scrollTrackWidth) + min :
                            // from value to pixel
                            scrollTrackWidth * (value - min) / valueRange;
                    },
                    toFixedRange: Axis.prototype.toFixedRange
                };
            }

            // Respond to updated data in the base series.
            // Abort if lazy-loading data from the server.
            if (baseSeries && baseSeries.xAxis && this.navigatorOptions.adaptToUpdatedData !== false) {
                addEvent(baseSeries, 'updatedData', this.updatedDataHandler);

                addEvent(baseSeries.xAxis, 'foundExtremes', function () {
                    if (baseSeries.xAxis) {
                        this.chart.scroller.modifyBaseAxisExtremes();
                    }
                });
        
                // Survive Series.update()
                baseSeries.userOptions.events = extend(baseSeries.userOptions.event, { updatedData: this.updatedDataHandler });

            }


            /**
             * For stock charts, extend the Chart.getMargins method so that we can set the final top position
             * of the navigator once the height of the chart, including the legend, is determined. #367.
             */
            wrap(chart, 'getMargins', function (proceed) {

                var legend = this.legend,
                    legendOptions = legend.options;

                proceed.apply(this, [].slice.call(arguments, 1));

                // Compute the top position
                scroller.top = top = scroller.navigatorOptions.top ||
                    this.chartHeight - scroller.height - scroller.scrollbarHeight - this.spacing[2] -
                            (legendOptions.verticalAlign === 'bottom' && legendOptions.enabled && !legendOptions.floating ?
                                legend.legendHeight + pick(legendOptions.margin, 10) : 0);

                if (xAxis && yAxis) { // false if navigator is disabled (#904)

                    xAxis.options.top = yAxis.options.top = top;

                    xAxis.setAxisSize();
                    yAxis.setAxisSize();
                }
            });


            scroller.addEvents();
        },

        /**
         * Get the union data extremes of the chart - the outer data extremes of the base
         * X axis and the navigator axis.
         */
        getUnionExtremes: function (returnFalseOnNoBaseSeries) {
            var baseAxis = this.chart.xAxis[0],
                navAxis = this.xAxis,
                navAxisOptions = navAxis.options,
                baseAxisOptions = baseAxis.options,
                ret;

            if (!returnFalseOnNoBaseSeries || baseAxis.dataMin !== null) {
                ret = {
                    dataMin: pick( // #4053
                        navAxisOptions && navAxisOptions.min,
                        numExt(
                            'min',
                            baseAxisOptions.min,
                            baseAxis.dataMin,
                            navAxis.dataMin,
                            navAxis.min
                        )
                    ),
                    dataMax: pick(
                        navAxisOptions && navAxisOptions.max,
                        numExt(
                            'max',
                            baseAxisOptions.max,
                            baseAxis.dataMax,
                            navAxis.dataMax,
                            navAxis.max
                        )
                    )
                };
            }
            return ret;
        },

        /**
         * Set the base series. With a bit of modification we should be able to make
         * this an API method to be called from the outside
         */
        setBaseSeries: function (baseSeriesOption) {
            var chart = this.chart;

            baseSeriesOption = baseSeriesOption || chart.options.navigator.baseSeries;

            // If we're resetting, remove the existing series
            if (this.series) {
                this.series.remove();
            }

            // Set the new base series
            this.baseSeries = chart.series[baseSeriesOption] ||
                (typeof baseSeriesOption === 'string' && chart.get(baseSeriesOption)) ||
                chart.series[0];

            // When run after render, this.xAxis already exists
            if (this.xAxis) {
                this.addBaseSeries();
            }
        },

        addBaseSeries: function () {
            var baseSeries = this.baseSeries,
                baseOptions = baseSeries ? baseSeries.options : {},
                baseData = baseOptions.data,
                mergedNavSeriesOptions,
                navigatorSeriesOptions = this.navigatorOptions.series,
                navigatorData;

            // remove it to prevent merging one by one
            navigatorData = navigatorSeriesOptions.data;
            this.hasNavigatorData = !!navigatorData;

            // Merge the series options
            mergedNavSeriesOptions = merge(baseOptions, navigatorSeriesOptions, {
                enableMouseTracking: false,
                group: 'nav', // for columns
                padXAxis: false,
                xAxis: 'navigator-x-axis',
                yAxis: 'navigator-y-axis',
                name: 'Navigator',
                showInLegend: false,
                stacking: false, // We only allow one series anyway (#4823)
                isInternal: true,
                visible: true
            });

            // Set the data. Do a slice to avoid mutating the navigator options from base series (#4923).
            mergedNavSeriesOptions.data = navigatorData || baseData.slice(0);

            // Add the series
            this.series = this.chart.initSeries(mergedNavSeriesOptions);

        },

        /**
         * Set the scroller x axis extremes to reflect the total. The navigator extremes
         * should always be the extremes of the union of all series in the chart as
         * well as the navigator series.
         */
        modifyNavigatorAxisExtremes: function () {
            var xAxis = this.xAxis,
                unionExtremes;

            if (xAxis.getExtremes) {
                unionExtremes = this.getUnionExtremes(true);

                if (unionExtremes && (unionExtremes.dataMin !== xAxis.min || unionExtremes.dataMax !== xAxis.max)) {
                    xAxis.min = unionExtremes.dataMin;
                    xAxis.max = unionExtremes.dataMax;
                }
            }            
        },

        /**
         * Hook to modify the base axis extremes with information from the Navigator
         */
        modifyBaseAxisExtremes: function () {
            var baseSeries = this.baseSeries,
                baseXAxis = baseSeries.xAxis,
                baseExtremes = baseXAxis.getExtremes(),
                baseMin = baseExtremes.min,
                baseMax = baseExtremes.max,
                baseDataMin = baseExtremes.dataMin,
                baseDataMax = baseExtremes.dataMax,
                range = baseMax - baseMin,
                stickToMin = this.stickToMin,
                stickToMax = this.stickToMax,
                newMax,
                newMin,
                navigatorSeries = this.series,
                hasSetExtremes = !!baseXAxis.setExtremes;

            // If the zoomed range is already at the min, move it to the right as new data
            // comes in
            if (stickToMin) {
                newMin = baseDataMin;
                newMax = newMin + range;
            }

            // If the zoomed range is already at the max, move it to the right as new data
            // comes in
            if (stickToMax) {
                newMax = baseDataMax;
                if (!stickToMin) { // if stickToMin is true, the new min value is set above
                    newMin = mathMax(newMax - range, navigatorSeries ? navigatorSeries.xData[0] : -Number.MAX_VALUE);
                }
            }

            // Update the extremes
            if (hasSetExtremes && (stickToMin || stickToMax)) {
                if (isNumber(newMin)) {
                    baseXAxis.min = baseXAxis.userMin = newMin;
                    baseXAxis.max = baseXAxis.userMax = newMax;
                }
            }

            // Reset
            this.stickToMin = this.stickToMax = null;
        },

        /**
         * Handler for updated data on the base series. When data is modified, the navigator series
         * must reflect it. This is called from the Chart.redraw function before axis and series 
         * extremes are computed.
         */
        updatedDataHandler: function () {
            var scroller = this.chart.scroller,
                baseSeries = scroller.baseSeries,
                navigatorSeries = scroller.series;

            // Detect whether the zoomed area should stick to the minimum or maximum. If the current
            // axis minimum falls outside the new updated dataset, we must adjust.
            scroller.stickToMin = baseSeries.xAxis.min <= baseSeries.xData[0];
            // If the scrollbar is scrolled all the way to the right, keep right as new data 
            // comes in.
            scroller.stickToMax = scroller.zoomedMax >= scroller.navigatorWidth;

            // Set the navigator series data to the new data of the base series
            if (navigatorSeries && !scroller.hasNavigatorData) {
                navigatorSeries.options.pointStart = baseSeries.xData[0];
                navigatorSeries.setData(baseSeries.options.data, false);

                // When adding points, shift it. A more fail-safe and lean procedure may be to extend the three
                // cases of updating data (addPoint, update, removePoint) directly so that this operation 
                // on the base series reflects directly on the navigator series.
                if (navigatorSeries.graph && baseSeries.graph) {
                    navigatorSeries.graph.shift = baseSeries.graph.shift;
                }
            }
        },

        /**
         * Destroys allocated elements.
         */
        destroy: function () {
            var scroller = this;

            // Disconnect events added in addEvents
            scroller.removeEvents();

            // Destroy properties
            each([scroller.xAxis, scroller.yAxis, scroller.leftShade, scroller.rightShade, scroller.outline, scroller.scrollbarTrack, scroller.scrollbarRifles, scroller.scrollbarGroup, scroller.scrollbar], function (prop) {
                if (prop && prop.destroy) {
                    prop.destroy();
                }
            });
            scroller.xAxis = scroller.yAxis = scroller.leftShade = scroller.rightShade = scroller.outline = scroller.scrollbarTrack = scroller.scrollbarRifles = scroller.scrollbarGroup = scroller.scrollbar = null;

            // Destroy elements in collection
            each([scroller.scrollbarButtons, scroller.handles, scroller.elementsToDestroy], function (coll) {
                destroyObjectProperties(coll);
            });
        }
    };

    Highcharts.Scroller = Scroller;


    /**
     * For Stock charts, override selection zooming with some special features because
     * X axis zooming is already allowed by the Navigator and Range selector.
     */
    wrap(Axis.prototype, 'zoom', function (proceed, newMin, newMax) {
        var chart = this.chart,
            chartOptions = chart.options,
            zoomType = chartOptions.chart.zoomType,
            previousZoom,
            navigator = chartOptions.navigator,
            rangeSelector = chartOptions.rangeSelector,
            ret;

        if (this.isXAxis && ((navigator && navigator.enabled) ||
                (rangeSelector && rangeSelector.enabled))) {

            // For x only zooming, fool the chart.zoom method not to create the zoom button
            // because the property already exists
            if (zoomType === 'x') {
                chart.resetZoomButton = 'blocked';

            // For y only zooming, ignore the X axis completely
            } else if (zoomType === 'y') {
                ret = false;

            // For xy zooming, record the state of the zoom before zoom selection, then when
            // the reset button is pressed, revert to this state
            } else if (zoomType === 'xy') {
                previousZoom = this.previousZoom;
                if (defined(newMin)) {
                    this.previousZoom = [this.min, this.max];
                } else if (previousZoom) {
                    newMin = previousZoom[0];
                    newMax = previousZoom[1];
                    delete this.previousZoom;
                }
            }

        }
        return ret !== UNDEFINED ? ret : proceed.call(this, newMin, newMax);
    });

    // Initialize scroller for stock charts
    wrap(Chart.prototype, 'init', function (proceed, options, callback) {

        addEvent(this, 'beforeRender', function () {
            var options = this.options;
            if (options.navigator.enabled || options.scrollbar.enabled) {
                this.scroller = new Scroller(this);
            }
        });

        proceed.call(this, options, callback);

    });

    // Pick up badly formatted point options to addPoint
    wrap(Series.prototype, 'addPoint', function (proceed, options, redraw, shift, animation) {
        var turboThreshold = this.options.turboThreshold;
        if (turboThreshold && this.xData.length > turboThreshold && isObject(options) && !isArray(options) && this.chart.scroller) {
            error(20, true);
        }
        proceed.call(this, options, redraw, shift, animation);
    });

    /* ****************************************************************************
     * End Scroller code                                                          *
     *****************************************************************************/
    /* ****************************************************************************
     * Start Range Selector code                                                  *
     *****************************************************************************/
    extend(defaultOptions, {
        rangeSelector: {
            // allButtonsEnabled: false,
            // enabled: true,
            // buttons: {Object}
            // buttonSpacing: 0,
            buttonTheme: {
                width: 28,
                height: 18,
                fill: '#f7f7f7',
                padding: 2,
                r: 0,
                'stroke-width': 0,
                style: {
                    color: '#444',
                    cursor: 'pointer',
                    fontWeight: 'normal'
                },
                zIndex: 7, // #484, #852
                states: {
                    hover: {
                        fill: '#e7e7e7'
                    },
                    select: {
                        fill: '#e7f0f9',
                        style: {
                            color: 'black',
                            fontWeight: 'bold'
                        }
                    }
                }
            },
            height: 35, // reserved space for buttons and input
            inputPosition: {
                align: 'right'
            },
            // inputDateFormat: '%b %e, %Y',
            // inputEditDateFormat: '%Y-%m-%d',
            // inputEnabled: true,
            // inputStyle: {},
            labelStyle: {
                color: '#666'
            }
            // selected: undefined
        }
    });
    defaultOptions.lang = merge(defaultOptions.lang, {
        rangeSelectorZoom: 'Zoom',
        rangeSelectorFrom: 'From',
        rangeSelectorTo: 'To'
    });

    /**
     * The object constructor for the range selector
     * @param {Object} chart
     */
    function RangeSelector(chart) {

        // Run RangeSelector
        this.init(chart);
    }

    RangeSelector.prototype = {
        /**
         * The method to run when one of the buttons in the range selectors is clicked
         * @param {Number} i The index of the button
         * @param {Object} rangeOptions
         * @param {Boolean} redraw
         */
        clickButton: function (i, redraw) {
            var rangeSelector = this,
                selected = rangeSelector.selected,
                chart = rangeSelector.chart,
                buttons = rangeSelector.buttons,
                rangeOptions = rangeSelector.buttonOptions[i],
                baseAxis = chart.xAxis[0],
                unionExtremes = (chart.scroller && chart.scroller.getUnionExtremes()) || baseAxis || {},
                dataMin = unionExtremes.dataMin,
                dataMax = unionExtremes.dataMax,
                newMin,
                newMax = baseAxis && mathRound(mathMin(baseAxis.max, pick(dataMax, baseAxis.max))), // #1568
                now,
                type = rangeOptions.type,
                baseXAxisOptions,
                range = rangeOptions._range,
                rangeMin,
                year,
                minSetting,
                rangeSetting,
                ctx,
                dataGrouping = rangeOptions.dataGrouping;

            if (dataMin === null || dataMax === null || // chart has no data, base series is removed
                    i === rangeSelector.selected) { // same button is clicked twice
                return;
            }

            // Set the fixed range before range is altered
            chart.fixedRange = range;

            // Apply dataGrouping associated to button
            if (dataGrouping) {
                this.forcedDataGrouping = true;
                Axis.prototype.setDataGrouping.call(baseAxis || { chart: this.chart }, dataGrouping, false);
            }

            // Apply range
            if (type === 'month' || type === 'year') {
                if (!baseAxis) {
                    // This is set to the user options and picked up later when the axis is instantiated
                    // so that we know the min and max.
                    range = rangeOptions;
                } else {
                    ctx = {
                        range: rangeOptions,
                        max: newMax,
                        dataMin: dataMin,
                        dataMax: dataMax
                    };
                    newMin = baseAxis.minFromRange.call(ctx);
                    if (isNumber(ctx.newMax)) {
                        newMax = ctx.newMax;
                    }
                }

            // Fixed times like minutes, hours, days
            } else if (range) {
                newMin = mathMax(newMax - range, dataMin);
                newMax = mathMin(newMin + range, dataMax);

            } else if (type === 'ytd') {

                // On user clicks on the buttons, or a delayed action running from the beforeRender
                // event (below), the baseAxis is defined.
                if (baseAxis) {

                    // When "ytd" is the pre-selected button for the initial view, its calculation
                    // is delayed and rerun in the beforeRender event (below). When the series
                    // are initialized, but before the chart is rendered, we have access to the xData
                    // array (#942).
                    if (dataMax === UNDEFINED) {
                        dataMin = Number.MAX_VALUE;
                        dataMax = Number.MIN_VALUE;
                        each(chart.series, function (series) {
                            var xData = series.xData; // reassign it to the last item
                            dataMin = mathMin(xData[0], dataMin);
                            dataMax = mathMax(xData[xData.length - 1], dataMax);
                        });
                        redraw = false;
                    }
                    now = new Date(dataMax);
                    year = now.getFullYear();
                    newMin = rangeMin = mathMax(dataMin || 0, Date.UTC(year, 0, 1));
                    now = now.getTime();
                    newMax = mathMin(dataMax || now, now);

                // "ytd" is pre-selected. We don't yet have access to processed point and extremes data
                // (things like pointStart and pointInterval are missing), so we delay the process (#942)
                } else {
                    addEvent(chart, 'beforeRender', function () {
                        rangeSelector.clickButton(i);
                    });
                    return;
                }
            } else if (type === 'all' && baseAxis) {
                newMin = dataMin;
                newMax = dataMax;
            }

            // Deselect previous button
            if (buttons[selected]) {
                buttons[selected].setState(0);
            }
            // Select this button
            if (buttons[i]) {
                buttons[i].setState(2);
                rangeSelector.lastSelected = i;
            }

            // Update the chart
            if (!baseAxis) {
                // Axis not yet instanciated. Temporarily set min and range
                // options and remove them on chart load (#4317).
                baseXAxisOptions = chart.options.xAxis[0];
                rangeSetting = baseXAxisOptions.range;
                baseXAxisOptions.range = range;
                minSetting = baseXAxisOptions.min;
                baseXAxisOptions.min = rangeMin;
                rangeSelector.setSelected(i);
                addEvent(chart, 'load', function resetMinAndRange() {
                    baseXAxisOptions.range = rangeSetting;
                    baseXAxisOptions.min = minSetting;
                });
            } else {
                // Existing axis object. Set extremes after render time.
                baseAxis.setExtremes(
                    newMin,
                    newMax,
                    pick(redraw, 1),
                    0,
                    {
                        trigger: 'rangeSelectorButton',
                        rangeSelectorButton: rangeOptions
                    }
                );
                rangeSelector.setSelected(i);
            }
        },

        /**
         * Set the selected option. This method only sets the internal flag, it doesn't
         * update the buttons or the actual zoomed range.
         */
        setSelected: function (selected) {
            this.selected = this.options.selected = selected;
        },

        /**
         * The default buttons for pre-selecting time frames
         */
        defaultButtons: [{
            type: 'month',
            count: 1,
            text: '1m'
        }, {
            type: 'month',
            count: 3,
            text: '3m'
        }, {
            type: 'month',
            count: 6,
            text: '6m'
        }, {
            type: 'ytd',
            text: 'YTD'
        }, {
            type: 'year',
            count: 1,
            text: '1y'
        }, {
            type: 'all',
            text: 'All'
        }],

        /**
         * Initialize the range selector
         */
        init: function (chart) {

            var rangeSelector = this,
                options = chart.options.rangeSelector,
                buttonOptions = options.buttons || [].concat(rangeSelector.defaultButtons),
                selectedOption = options.selected,
                blurInputs = rangeSelector.blurInputs = function () {
                    var minInput = rangeSelector.minInput,
                        maxInput = rangeSelector.maxInput;
                    if (minInput && minInput.blur) { //#3274 in some case blur is not defined
                        fireEvent(minInput, 'blur'); //#3274
                    }
                    if (maxInput && maxInput.blur) { //#3274 in some case blur is not defined
                        fireEvent(maxInput, 'blur'); //#3274
                    }
                };

            rangeSelector.chart = chart;
            rangeSelector.options = options;
            rangeSelector.buttons = [];

            chart.extraTopMargin = options.height;
            rangeSelector.buttonOptions = buttonOptions;

            addEvent(chart.container, 'mousedown', blurInputs);
            addEvent(chart, 'resize', blurInputs);

            // Extend the buttonOptions with actual range
            each(buttonOptions, rangeSelector.computeButtonRange);

            // zoomed range based on a pre-selected button index
            if (selectedOption !== UNDEFINED && buttonOptions[selectedOption]) {
                this.clickButton(selectedOption, false);
            }


            addEvent(chart, 'load', function () {
                // If a data grouping is applied to the current button, release it when extremes change
                addEvent(chart.xAxis[0], 'setExtremes', function (e) {
                    if (this.max - this.min !== chart.fixedRange && e.trigger !== 'rangeSelectorButton' &&
                            e.trigger !== 'updatedData' && rangeSelector.forcedDataGrouping) {
                        this.setDataGrouping(false, false);
                    }
                });
                // Normalize the pressed button whenever a new range is selected
                addEvent(chart.xAxis[0], 'afterSetExtremes', function () {
                    rangeSelector.updateButtonStates(true);
                });
            });
        },

        /**
         * Dynamically update the range selector buttons after a new range has been set
         */
        updateButtonStates: function (updating) {
            var rangeSelector = this,
                chart = this.chart,
                baseAxis = chart.xAxis[0],
                unionExtremes = (chart.scroller && chart.scroller.getUnionExtremes()) || baseAxis,
                dataMin = unionExtremes.dataMin,
                dataMax = unionExtremes.dataMax,
                selected = rangeSelector.selected,
                allButtonsEnabled = rangeSelector.options.allButtonsEnabled,
                buttons = rangeSelector.buttons;

            if (updating && chart.fixedRange !== mathRound(baseAxis.max - baseAxis.min)) {
                if (buttons[selected]) {
                    buttons[selected].setState(0);
                }
                rangeSelector.setSelected(null);
            }

            each(rangeSelector.buttonOptions, function (rangeOptions, i) {
                var actualRange = mathRound(baseAxis.max - baseAxis.min),
                    range = rangeOptions._range,
                    type = rangeOptions.type,
                    count = rangeOptions.count || 1,
                    // Disable buttons where the range exceeds what is allowed in the current view
                    isTooGreatRange = range > dataMax - dataMin,
                    // Disable buttons where the range is smaller than the minimum range
                    isTooSmallRange = range < baseAxis.minRange,
                    // Disable the All button if we're already showing all
                    isAllButAlreadyShowingAll = rangeOptions.type === 'all' && baseAxis.max - baseAxis.min >= dataMax - dataMin &&
                        buttons[i].state !== 2,
                    // Disable the YTD button if the complete range is within the same year
                    isYTDButNotAvailable = rangeOptions.type === 'ytd' && dateFormat('%Y', dataMin) === dateFormat('%Y', dataMax),
                    // Set a button on export
                    isSelectedForExport = chart.renderer.forExport && i === selected,

                    isSameRange = range === actualRange,

                    hasNoData = !baseAxis.hasVisibleSeries;

                // Months and years have a variable range so we check the extremes
                if ((type === 'month' || type === 'year') && (actualRange >= { month: 28, year: 365 }[type] * 24 * 36e5 * count) &&
                        (actualRange <= { month: 31, year: 366 }[type] * 24 * 36e5 * count)) {
                    isSameRange = true;
                }
                // The new zoom area happens to match the range for a button - mark it selected.
                // This happens when scrolling across an ordinal gap. It can be seen in the intraday
                // demos when selecting 1h and scroll across the night gap.
                if (isSelectedForExport || (isSameRange && i !== selected) && i === rangeSelector.lastSelected) {
                    rangeSelector.setSelected(i);
                    buttons[i].setState(2);

                } else if (!allButtonsEnabled && (isTooGreatRange || isTooSmallRange || isAllButAlreadyShowingAll || isYTDButNotAvailable || hasNoData)) {
                    buttons[i].setState(3);

                } else if (buttons[i].state === 3) {
                    buttons[i].setState(0);
                }
            });
        },

        /**
         * Compute and cache the range for an individual button
         */
        computeButtonRange: function (rangeOptions) {
            var type = rangeOptions.type,
                count = rangeOptions.count || 1,

                // these time intervals have a fixed number of milliseconds, as opposed
                // to month, ytd and year
                fixedTimes = {
                    millisecond: 1,
                    second: 1000,
                    minute: 60 * 1000,
                    hour: 3600 * 1000,
                    day: 24 * 3600 * 1000,
                    week: 7 * 24 * 3600 * 1000
                };

            // Store the range on the button object
            if (fixedTimes[type]) {
                rangeOptions._range = fixedTimes[type] * count;
            } else if (type === 'month' || type === 'year') {
                rangeOptions._range = { month: 30, year: 365 }[type] * 24 * 36e5 * count;
            }
        },

        /**
         * Set the internal and displayed value of a HTML input for the dates
         * @param {String} name
         * @param {Number} time
         */
        setInputValue: function (name, time) {
            var options = this.chart.options.rangeSelector;

            if (defined(time)) {
                this[name + 'Input'].HCTime = time;
            }

            this[name + 'Input'].value = dateFormat(
                options.inputEditDateFormat || '%Y-%m-%d',
                this[name + 'Input'].HCTime
            );
            this[name + 'DateBox'].attr({
                text: dateFormat(options.inputDateFormat || '%b %e, %Y', this[name + 'Input'].HCTime)
            });
        },

        showInput: function (name) {
            var inputGroup = this.inputGroup,
                dateBox = this[name + 'DateBox'];

            css(this[name + 'Input'], {
                left: (inputGroup.translateX + dateBox.x) + PX,
                top: inputGroup.translateY + PX,
                width: (dateBox.width - 2) + PX,
                height: (dateBox.height - 2) + PX,
                border: '2px solid silver'
            });
        },

        hideInput: function (name) {
            css(this[name + 'Input'], {
                border: 0,
                width: '1px',
                height: '1px'
            });
            this.setInputValue(name);
        },

        /**
         * Draw either the 'from' or the 'to' HTML input box of the range selector
         * @param {Object} name
         */
        drawInput: function (name) {
            var rangeSelector = this,
                chart = rangeSelector.chart,
                chartStyle = chart.renderer.style,
                renderer = chart.renderer,
                options = chart.options.rangeSelector,
                lang = defaultOptions.lang,
                div = rangeSelector.div,
                isMin = name === 'min',
                input,
                label,
                dateBox,
                inputGroup = this.inputGroup;

            function updateExtremes() {
                var inputValue = input.value,
                    value = (options.inputDateParser || Date.parse)(inputValue),
                    xAxis = chart.xAxis[0],
                    dataMin = xAxis.dataMin,
                    dataMax = xAxis.dataMax;
                if (value !== input.previousValue) {
                    input.previousValue = value;
                    // If the value isn't parsed directly to a value by the browser's Date.parse method,
                    // like YYYY-MM-DD in IE, try parsing it a different way
                    if (!isNumber(value)) {
                        value = inputValue.split('-');
                        value = Date.UTC(pInt(value[0]), pInt(value[1]) - 1, pInt(value[2]));
                    }

                    if (isNumber(value)) {

                        // Correct for timezone offset (#433)
                        if (!defaultOptions.global.useUTC) {
                            value = value + new Date().getTimezoneOffset() * 60 * 1000;
                        }

                        // Validate the extremes. If it goes beyound the data min or max, use the
                        // actual data extreme (#2438).
                        if (isMin) {
                            if (value > rangeSelector.maxInput.HCTime) {
                                value = UNDEFINED;
                            } else if (value < dataMin) {
                                value = dataMin;
                            }
                        } else {
                            if (value < rangeSelector.minInput.HCTime) {
                                value = UNDEFINED;
                            } else if (value > dataMax) {
                                value = dataMax;
                            }
                        }

                        // Set the extremes
                        if (value !== UNDEFINED) {
                            chart.xAxis[0].setExtremes(
                                isMin ? value : xAxis.min,
                                isMin ? xAxis.max : value,
                                UNDEFINED,
                                UNDEFINED,
                                { trigger: 'rangeSelectorInput' }
                            );
                        }
                    }
                }
            }

            // Create the text label
            this[name + 'Label'] = label = renderer.label(lang[isMin ? 'rangeSelectorFrom' : 'rangeSelectorTo'], this.inputGroup.offset)
                .attr({
                    padding: 2
                })
                .css(merge(chartStyle, options.labelStyle))
                .add(inputGroup);
            inputGroup.offset += label.width + 5;

            // Create an SVG label that shows updated date ranges and and records click events that
            // bring in the HTML input.
            this[name + 'DateBox'] = dateBox = renderer.label('', inputGroup.offset)
                .attr({
                    padding: 2,
                    width: options.inputBoxWidth || 90,
                    height: options.inputBoxHeight || 17,
                    stroke: options.inputBoxBorderColor || 'silver',
                    'stroke-width': 1
                })
                .css(merge({
                    textAlign: 'center',
                    color: '#444'
                }, chartStyle, options.inputStyle))
                .on('click', function () {
                    rangeSelector.showInput(name); // If it is already focused, the onfocus event doesn't fire (#3713)
                    rangeSelector[name + 'Input'].focus();
                })
                .add(inputGroup);
            inputGroup.offset += dateBox.width + (isMin ? 10 : 0);


            // Create the HTML input element. This is rendered as 1x1 pixel then set to the right size
            // when focused.
            this[name + 'Input'] = input = createElement('input', {
                name: name,
                className: PREFIX + 'range-selector',
                type: 'text'
            }, extend({
                position: ABSOLUTE,
                border: 0,
                width: '1px', // Chrome needs a pixel to see it
                height: '1px',
                padding: 0,
                textAlign: 'center',
                fontSize: chartStyle.fontSize,
                fontFamily: chartStyle.fontFamily,
                left: '-9em', // #4798
                top: chart.plotTop + PX // prevent jump on focus in Firefox
            }, options.inputStyle), div);

            // Blow up the input box
            input.onfocus = function () {
                rangeSelector.showInput(name);
            };
            // Hide away the input box
            input.onblur = function () {
                rangeSelector.hideInput(name);
            };

            // handle changes in the input boxes
            input.onchange = updateExtremes;

            input.onkeypress = function (event) {
                // IE does not fire onchange on enter
                if (event.keyCode === 13) {
                    updateExtremes();
                }
            };
        },

        /**
         * Get the position of the range selector buttons and inputs. This can be overridden from outside for custom positioning.
         */
        getPosition: function () {
            var chart = this.chart,
                options = chart.options.rangeSelector,
                buttonTop = pick((options.buttonPosition || {}).y, chart.plotTop - chart.axisOffset[0] - options.height);

            return {
                buttonTop: buttonTop,
                inputTop: buttonTop - 10
            };
        },

        /**
         * Render the range selector including the buttons and the inputs. The first time render
         * is called, the elements are created and positioned. On subsequent calls, they are
         * moved and updated.
         * @param {Number} min X axis minimum
         * @param {Number} max X axis maximum
         */
        render: function (min, max) {

            var rangeSelector = this,
                chart = rangeSelector.chart,
                renderer = chart.renderer,
                container = chart.container,
                chartOptions = chart.options,
                navButtonOptions = chartOptions.exporting && chartOptions.exporting.enabled !== false &&
                    chartOptions.navigation && chartOptions.navigation.buttonOptions,
                options = chartOptions.rangeSelector,
                buttons = rangeSelector.buttons,
                lang = defaultOptions.lang,
                div = rangeSelector.div,
                inputGroup = rangeSelector.inputGroup,
                buttonTheme = options.buttonTheme,
                buttonPosition = options.buttonPosition || {},
                inputEnabled = options.inputEnabled,
                states = buttonTheme && buttonTheme.states,
                plotLeft = chart.plotLeft,
                buttonLeft,
                pos = this.getPosition(),
                buttonGroup = rangeSelector.group,
                buttonBBox,
                rendered = rangeSelector.rendered;


            // create the elements
            if (!rendered) {

                rangeSelector.group = buttonGroup = renderer.g('range-selector-buttons').add();

                rangeSelector.zoomText = renderer.text(lang.rangeSelectorZoom, pick(buttonPosition.x, plotLeft), 15)
                    .css(options.labelStyle)
                    .add(buttonGroup);

                // button starting position
                buttonLeft = pick(buttonPosition.x, plotLeft) + rangeSelector.zoomText.getBBox().width + 5;

                each(rangeSelector.buttonOptions, function (rangeOptions, i) {
                    buttons[i] = renderer.button(
                            rangeOptions.text,
                            buttonLeft,
                            0,
                            function () {
                                rangeSelector.clickButton(i);
                                rangeSelector.isActive = true;
                            },
                            buttonTheme,
                            states && states.hover,
                            states && states.select,
                            states && states.disabled
                        )
                        .css({
                            textAlign: 'center'
                        })
                        .add(buttonGroup);

                    // increase button position for the next button
                    buttonLeft += buttons[i].width + pick(options.buttonSpacing, 5);

                    if (rangeSelector.selected === i) {
                        buttons[i].setState(2);
                    }
                });

                rangeSelector.updateButtonStates();

                // first create a wrapper outside the container in order to make
                // the inputs work and make export correct
                if (inputEnabled !== false) {
                    rangeSelector.div = div = createElement('div', null, {
                        position: 'relative',
                        height: 0,
                        zIndex: 1 // above container
                    });

                    container.parentNode.insertBefore(div, container);

                    // Create the group to keep the inputs
                    rangeSelector.inputGroup = inputGroup = renderer.g('input-group')
                        .add();
                    inputGroup.offset = 0;

                    rangeSelector.drawInput('min');
                    rangeSelector.drawInput('max');
                }
            }

            // Set or update the group position
            buttonGroup[rendered ? 'animate' : 'attr']({
                translateY: pos.buttonTop
            });

            if (inputEnabled !== false) {

                // Update the alignment to the updated spacing box
                inputGroup.align(extend({
                    y: pos.inputTop,
                    width: inputGroup.offset,
                    // Detect collision with the exporting buttons
                    x: navButtonOptions && (pos.inputTop < (navButtonOptions.y || 0) + navButtonOptions.height - chart.spacing[0]) ?
                        -40 : 0
                }, options.inputPosition), true, chart.spacingBox);

                // Hide if overlapping - inputEnabled is null or undefined
                if (!defined(inputEnabled)) {
                    buttonBBox = buttonGroup.getBBox();
                    inputGroup[inputGroup.translateX < buttonBBox.x + buttonBBox.width + 10 ? 'hide' : 'show']();
                }

                // Set or reset the input values
                rangeSelector.setInputValue('min', min);
                rangeSelector.setInputValue('max', max);
            }

            rangeSelector.rendered = true;
        },

        /**
         * Destroys allocated elements.
         */
        destroy: function () {
            var minInput = this.minInput,
                maxInput = this.maxInput,
                chart = this.chart,
                blurInputs = this.blurInputs,
                key;

            removeEvent(chart.container, 'mousedown', blurInputs);
            removeEvent(chart, 'resize', blurInputs);

            // Destroy elements in collections
            destroyObjectProperties(this.buttons);

            // Clear input element events
            if (minInput) {
                minInput.onfocus = minInput.onblur = minInput.onchange = null;
            }
            if (maxInput) {
                maxInput.onfocus = maxInput.onblur = maxInput.onchange = null;
            }

            // Destroy HTML and SVG elements
            for (key in this) {
                if (this[key] && key !== 'chart') {
                    if (this[key].destroy) { // SVGElement
                        this[key].destroy();
                    } else if (this[key].nodeType) { // HTML element
                        discardElement(this[key]);
                    }
                }
                this[key] = null;
            }
        }
    };

    /**
     * Add logic to normalize the zoomed range in order to preserve the pressed state of range selector buttons
     */
    Axis.prototype.toFixedRange = function (pxMin, pxMax, fixedMin, fixedMax) {
        var fixedRange = this.chart && this.chart.fixedRange,
            newMin = pick(fixedMin, this.translate(pxMin, true)),
            newMax = pick(fixedMax, this.translate(pxMax, true)),
            changeRatio = fixedRange && (newMax - newMin) / fixedRange;

        // If the difference between the fixed range and the actual requested range is
        // too great, the user is dragging across an ordinal gap, and we need to release
        // the range selector button.
        if (changeRatio > 0.7 && changeRatio < 1.3) {
            if (fixedMax) {
                newMin = newMax - fixedRange;
            } else {
                newMax = newMin + fixedRange;
            }
        }
        if (!isNumber(newMin)) { // #1195
            newMin = newMax = undefined;
        }

        return {
            min: newMin,
            max: newMax
        };
    };

    Axis.prototype.minFromRange = function () {
        var rangeOptions = this.range,
            type = rangeOptions.type,
            timeName = { month: 'Month', year: 'FullYear' }[type],
            min,
            max = this.max,
            dataMin,
            range,
            // Get the true range from a start date
            getTrueRange = function (base, count) {
                var date = new Date(base);
                date['set' + timeName](date['get' + timeName]() + count);
                return date.getTime() - base;
            };

        if (isNumber(rangeOptions)) {
            min = this.max - rangeOptions;
            range = rangeOptions;
        } else {
            min = max + getTrueRange(max, -rangeOptions.count);
        }

        dataMin = pick(this.dataMin, Number.MIN_VALUE);
        if (!isNumber(min)) {
            min = dataMin;
        }
        if (min <= dataMin) {
            min = dataMin;
            if (range === undefined) { // #4501
                range = getTrueRange(min, rangeOptions.count);
            }
            this.newMax = mathMin(min + range, this.dataMax);
        }
        if (!isNumber(max)) {
            min = undefined;
        }
        return min;

    };

    // Initialize scroller for stock charts
    wrap(Chart.prototype, 'init', function (proceed, options, callback) {

        addEvent(this, 'init', function () {
            if (this.options.rangeSelector.enabled) {
                this.rangeSelector = new RangeSelector(this);
            }
        });

        proceed.call(this, options, callback);

    });


    Highcharts.RangeSelector = RangeSelector;

    /* ****************************************************************************
     * End Range Selector code                                                    *
     *****************************************************************************/



    Chart.prototype.callbacks.push(function (chart) {
        var extremes,
            scroller = chart.scroller,
            rangeSelector = chart.rangeSelector;

        function renderRangeSelector() {
            extremes = chart.xAxis[0].getExtremes();
            if (isNumber(extremes.min)) {
                rangeSelector.render(extremes.min, extremes.max);
            }
        }

        function afterSetExtremesHandlerRangeSelector(e) {
            rangeSelector.render(e.min, e.max);
        }

        function destroyEvents() {
            if (rangeSelector) {
                removeEvent(chart, 'resize', renderRangeSelector);
                removeEvent(chart.xAxis[0], 'afterSetExtremes', afterSetExtremesHandlerRangeSelector);
            }
        }

        // initiate the scroller
        if (scroller) {
            extremes = chart.xAxis[0].getExtremes();
            scroller.render(extremes.min, extremes.max);
        }
        if (rangeSelector) {
            // redraw the scroller on setExtremes
            addEvent(chart.xAxis[0], 'afterSetExtremes', afterSetExtremesHandlerRangeSelector);

            // redraw the scroller chart resize
            addEvent(chart, 'resize', renderRangeSelector);

            // do it now
            renderRangeSelector();
        }

        // Remove resize/afterSetExtremes at chart destroy
        addEvent(chart, 'destroy', destroyEvents);
    });
    /**
     * A wrapper for Chart with all the default values for a Stock chart
     */
    Highcharts.StockChart = Highcharts.stockChart = function (a, b, c) {
        var hasRenderToArg = isString(a) || a.nodeName,
            options = arguments[hasRenderToArg ? 1 : 0],
            seriesOptions = options.series, // to increase performance, don't merge the data
            opposite,

            // Always disable startOnTick:true on the main axis when the navigator is enabled (#1090)
            navigatorEnabled = pick(options.navigator && options.navigator.enabled, true),
            disableStartOnTick = navigatorEnabled ? {
                startOnTick: false,
                endOnTick: false
            } : null,

            lineOptions = {

                marker: {
                    enabled: false,
                    radius: 2
                }
                // gapSize: 0
            },
            columnOptions = {
                shadow: false,
                borderWidth: 0
            };

        // apply X axis options to both single and multi y axes
        options.xAxis = map(splat(options.xAxis || {}), function (xAxisOptions) {
            return merge(
                { // defaults
                    minPadding: 0,
                    maxPadding: 0,
                    ordinal: true,
                    title: {
                        text: null
                    },
                    labels: {
                        overflow: 'justify'
                    },
                    showLastLabel: true
                }, xAxisOptions, // user options
                { // forced options
                    type: 'datetime',
                    categories: null
                },
                disableStartOnTick
            );
        });

        // apply Y axis options to both single and multi y axes
        options.yAxis = map(splat(options.yAxis || {}), function (yAxisOptions) {
            opposite = pick(yAxisOptions.opposite, true);
            return merge({ // defaults
                labels: {
                    y: -2
                },
                opposite: opposite,
                showLastLabel: false,
                title: {
                    text: null
                }
            }, yAxisOptions // user options
            );
        });

        options.series = null;

        options = merge(
            {
                chart: {
                    panning: true,
                    pinchType: 'x'
                },
                navigator: {
                    enabled: true
                },
                scrollbar: {
                    enabled: true
                },
                rangeSelector: {
                    enabled: true
                },
                title: {
                    text: null,
                    style: {
                        fontSize: '16px'
                    }
                },
                tooltip: {
                    shared: true,
                    crosshairs: true
                },
                legend: {
                    enabled: false
                },

                plotOptions: {
                    line: lineOptions,
                    spline: lineOptions,
                    area: lineOptions,
                    areaspline: lineOptions,
                    arearange: lineOptions,
                    areasplinerange: lineOptions,
                    column: columnOptions,
                    columnrange: columnOptions,
                    candlestick: columnOptions,
                    ohlc: columnOptions
                }

            },
            options, // user's options

            { // forced options
                _stock: true, // internal flag
                chart: {
                    inverted: false
                }
            }
        );

        options.series = seriesOptions;

        return hasRenderToArg ? 
            new Chart(a, options, c) :
            new Chart(options, b);
    };

    // Implement the pinchType option
    wrap(Pointer.prototype, 'init', function (proceed, chart, options) {

        var pinchType = options.chart.pinchType || '';

        proceed.call(this, chart, options);

        // Pinch status
        this.pinchX = this.pinchHor = pinchType.indexOf('x') !== -1;
        this.pinchY = this.pinchVert = pinchType.indexOf('y') !== -1;
        this.hasZoom = this.hasZoom || this.pinchHor || this.pinchVert;
    });

    // Override the automatic label alignment so that the first Y axis' labels
    // are drawn on top of the grid line, and subsequent axes are drawn outside
    wrap(Axis.prototype, 'autoLabelAlign', function (proceed) {
        var chart = this.chart,
            options = this.options,
            panes = chart._labelPanes = chart._labelPanes || {},
            key,
            labelOptions = this.options.labels;
        if (this.chart.options._stock && this.coll === 'yAxis') {
            key = options.top + ',' + options.height;
            if (!panes[key] && labelOptions.enabled) { // do it only for the first Y axis of each pane
                if (labelOptions.x === 15) { // default
                    labelOptions.x = 0;
                }
                if (labelOptions.align === undefined) {
                    labelOptions.align = 'right';
                }
                panes[key] = 1;
                return 'right';
            }
        }
        return proceed.call(this, [].slice.call(arguments, 1));
    });

    // Override getPlotLinePath to allow for multipane charts
    wrap(Axis.prototype, 'getPlotLinePath', function (proceed, value, lineWidth, old, force, translatedValue) {
        var axis = this,
            series = (this.isLinked && !this.series ? this.linkedParent.series : this.series),
            chart = axis.chart,
            renderer = chart.renderer,
            axisLeft = axis.left,
            axisTop = axis.top,
            x1,
            y1,
            x2,
            y2,
            result = [],
            axes = [], //#3416 need a default array
            axes2,
            uniqueAxes,
            transVal;

        // Ignore in case of color Axis. #3360, #3524
        if (axis.coll === 'colorAxis') {
            return proceed.apply(this, [].slice.call(arguments, 1));
        }

        // Get the related axes based on series
        axes = (axis.isXAxis ?
            (defined(axis.options.yAxis) ?
                [chart.yAxis[axis.options.yAxis]] :
                map(series, function (s) {
                    return s.yAxis;
                })
            ) :
            (defined(axis.options.xAxis) ?
                [chart.xAxis[axis.options.xAxis]] :
                map(series, function (s) {
                    return s.xAxis;
                })
            )
        );

        // Get the related axes based options.*Axis setting #2810
        axes2 = (axis.isXAxis ? chart.yAxis : chart.xAxis);
        each(axes2, function (A) {
            if (defined(A.options.id) ? A.options.id.indexOf('navigator') === -1 : true) {
                var a = (A.isXAxis ? 'yAxis' : 'xAxis'),
                    rax = (defined(A.options[a]) ? chart[a][A.options[a]] : chart[a][0]);

                if (axis === rax) {
                    axes.push(A);
                }
            }
        });


        // Remove duplicates in the axes array. If there are no axes in the axes array,
        // we are adding an axis without data, so we need to populate this with grid
        // lines (#2796).
        uniqueAxes = axes.length ? [] : [axis.isXAxis ? chart.yAxis[0] : chart.xAxis[0]]; //#3742
        each(axes, function (axis2) {
            if (inArray(axis2, uniqueAxes) === -1) {
                uniqueAxes.push(axis2);
            }
        });

        transVal = pick(translatedValue, axis.translate(value, null, null, old));
        if (isNumber(transVal)) {
            if (axis.horiz) {
                each(uniqueAxes, function (axis2) {
                    var skip;

                    y1 = axis2.pos;
                    y2 = y1 + axis2.len;
                    x1 = x2 = mathRound(transVal + axis.transB);

                    if (x1 < axisLeft || x1 > axisLeft + axis.width) { // outside plot area
                        if (force) {
                            x1 = x2 = mathMin(mathMax(axisLeft, x1), axisLeft + axis.width);
                        } else {
                            skip = true;
                        }
                    }
                    if (!skip) {
                        result.push('M', x1, y1, 'L', x2, y2);
                    }
                });
            } else {
                each(uniqueAxes, function (axis2) {
                    var skip;

                    x1 = axis2.pos;
                    x2 = x1 + axis2.len;
                    y1 = y2 = mathRound(axisTop + axis.height - transVal);

                    if (y1 < axisTop || y1 > axisTop + axis.height) { // outside plot area
                        if (force) {
                            y1 = y2 = mathMin(mathMax(axisTop, y1), axis.top + axis.height);
                        } else {
                            skip = true;
                        }
                    }
                    if (!skip) {
                        result.push('M', x1, y1, 'L', x2, y2);
                    }
                });
            }
        }
        return result.length > 0 ?
            renderer.crispPolyLine(result, lineWidth || 1) :
            null; //#3557 getPlotLinePath in regular Highcharts also returns null
    });

    // Override getPlotBandPath to allow for multipane charts
    Axis.prototype.getPlotBandPath = function (from, to) {
        var toPath = this.getPlotLinePath(to, null, null, true),
            path = this.getPlotLinePath(from, null, null, true),
            result = [],
            i;

        if (path && toPath && path.toString() !== toPath.toString()) {
            // Go over each subpath
            for (i = 0; i < path.length; i += 6) {
                result.push('M', path[i + 1], path[i + 2], 'L', path[i + 4], path[i + 5], toPath[i + 4], toPath[i + 5], toPath[i + 1], toPath[i + 2]);
            }
        } else { // outside the axis area
            result = null;
        }

        return result;
    };

    // Function to crisp a line with multiple segments
    SVGRenderer.prototype.crispPolyLine = function (points, width) {
        // points format: [M, 0, 0, L, 100, 0]
        // normalize to a crisp line
        var i;
        for (i = 0; i < points.length; i = i + 6) {
            if (points[i + 1] === points[i + 4]) {
                // Substract due to #1129. Now bottom and left axis gridlines behave the same.
                points[i + 1] = points[i + 4] = mathRound(points[i + 1]) - (width % 2 / 2);
            }
            if (points[i + 2] === points[i + 5]) {
                points[i + 2] = points[i + 5] = mathRound(points[i + 2]) + (width % 2 / 2);
            }
        }
        return points;
    };
    if (Renderer === Highcharts.VMLRenderer) {
        VMLRenderer.prototype.crispPolyLine = SVGRenderer.prototype.crispPolyLine;
    }


    // Wrapper to hide the label
    wrap(Axis.prototype, 'hideCrosshair', function (proceed, i) {
        proceed.call(this, i);

        if (this.crossLabel) {
            this.crossLabel = this.crossLabel.hide();
        }
    });

    // Wrapper to draw the label
    wrap(Axis.prototype, 'drawCrosshair', function (proceed, e, point) {
        // Draw the crosshair
        proceed.call(this, e, point);

        // Check if the label has to be drawn
        if (!defined(this.crosshair.label) || !this.crosshair.label.enabled) {
            return;
        }

        var chart = this.chart,
            options = this.options.crosshair.label,        // the label's options
            horiz = this.horiz,                            // axis orientation
            opposite = this.opposite,                    // axis position
            left = this.left,                            // left position
            top = this.top,                                // top position
            crossLabel = this.crossLabel,                // reference to the svgElement
            posx,
            posy,
            crossBox,
            formatOption = options.format,
            formatFormat = '',
            limit,
            align,
            tickInside = this.options.tickPosition === 'inside',
            snap = this.crosshair.snap !== false,
            value;

        align = (horiz ? 'center' : opposite ? (this.labelAlign === 'right' ? 'right' : 'left') : (this.labelAlign === 'left' ? 'left' : 'center'));

        // If the label does not exist yet, create it.
        if (!crossLabel) {
            crossLabel = this.crossLabel = chart.renderer.label(null, null, null, options.shape || 'callout')
            .attr({
                align: options.align || align,
                zIndex: 12,
                fill: options.backgroundColor || (this.series[0] && this.series[0].color) || 'gray',
                padding: pick(options.padding, 8),
                stroke: options.borderColor || '',
                'stroke-width': options.borderWidth || 0,
                r: pick(options.borderRadius, 3)
            })
            .css(extend({
                color: 'white',
                fontWeight: 'normal',
                fontSize: '11px',
                textAlign: 'center'
            }, options.style))
            .add();
        }

        if (horiz) {
            posx = snap ? point.plotX + left : e.chartX;
            posy = top + (opposite ? 0 : this.height);
        } else {
            posx = opposite ? this.width + left : 0;
            posy = snap ? point.plotY + top : e.chartY;
        }

        if (!formatOption && !options.formatter) {
            if (this.isDatetimeAxis) {
                formatFormat = '%b %d, %Y';
            }
            formatOption = '{value' + (formatFormat ? ':' + formatFormat : '') + '}';
        }

        // Show the label
        value = snap ? point[this.isXAxis ? 'x' : 'y'] : this.toValue(horiz ? e.chartX : e.chartY);
        crossLabel.attr({
            text: formatOption ? format(formatOption, { value: value }) : options.formatter.call(this, value),
            anchorX: horiz ? posx : (this.opposite ? 0 : chart.chartWidth),
            anchorY: horiz ? (this.opposite ? chart.chartHeight : 0) : posy,
            x: posx,
            y: posy,
            visibility: VISIBLE
        });
        crossBox = crossLabel.getBBox();

        // now it is placed we can correct its position
        if (horiz) {
            if ((tickInside && !opposite) || (!tickInside && opposite)) {
                posy = crossLabel.y - crossBox.height;
            }
        } else {
            posy = crossLabel.y - (crossBox.height / 2);
        }

        // check the edges
        if (horiz) {
            limit = {
                left: left - crossBox.x,
                right: left + this.width - crossBox.x
            };
        } else {
            limit = {
                left: this.labelAlign === 'left' ? left : 0,
                right: this.labelAlign === 'right' ? left + this.width : chart.chartWidth
            };
        }

        // left edge
        if (crossLabel.translateX < limit.left) {
            posx += limit.left - crossLabel.translateX;
        }
        // right edge
        if (crossLabel.translateX + crossBox.width >= limit.right) {
            posx -= crossLabel.translateX + crossBox.width - limit.right;
        }

        // show the crosslabel
        crossLabel.attr({ x: posx, y: posy, visibility: 'visible' });
    });

    /* ****************************************************************************
     * Start value compare logic                                                  *
     *****************************************************************************/

    var seriesInit = seriesProto.init,
        seriesProcessData = seriesProto.processData,
        pointTooltipFormatter = Point.prototype.tooltipFormatter;

    /**
     * Extend series.init by adding a method to modify the y value used for plotting
     * on the y axis. This method is called both from the axis when finding dataMin
     * and dataMax, and from the series.translate method.
     */
    seriesProto.init = function () {

        // Call base method
        seriesInit.apply(this, arguments);

        // Set comparison mode
        this.setCompare(this.options.compare);
    };

    /**
     * The setCompare method can be called also from the outside after render time
     */
    seriesProto.setCompare = function (compare) {

        // Set or unset the modifyValue method
        this.modifyValue = (compare === 'value' || compare === 'percent') ? function (value, point) {
            var compareValue = this.compareValue;

            if (value !== UNDEFINED) { // #2601

                // get the modified value
                value = compare === 'value' ?
                    value - compareValue : // compare value
                    value = 100 * (value / compareValue) - 100; // compare percent

                // record for tooltip etc.
                if (point) {
                    point.change = value;
                }

            }

            return value;
        } : null;

        // Mark dirty
        if (this.chart.hasRendered) {
            this.isDirty = true;
        }

    };

    /**
     * Extend series.processData by finding the first y value in the plot area,
     * used for comparing the following values
     */
    seriesProto.processData = function () {
        var series = this,
            i,
            keyIndex = -1,
            processedXData,
            processedYData,
            length,
            compareValue;

        // call base method
        seriesProcessData.apply(this, arguments);

        if (series.xAxis && series.processedYData) { // not pies

            // local variables
            processedXData = series.processedXData;
            processedYData = series.processedYData;
            length = processedYData.length;

            // For series with more than one value (range, OHLC etc), compare against
            // the pointValKey (#4922)
            if (series.pointArrayMap) {
                keyIndex = inArray(series.pointValKey || 'y', series.pointArrayMap);
            }

            // find the first value for comparison
            for (i = 0; i < length; i++) {
                compareValue = keyIndex > -1 ? 
                    processedYData[i][keyIndex] :
                    processedYData[i];
                if (isNumber(compareValue) && processedXData[i] >= series.xAxis.min && compareValue !== 0) {
                    series.compareValue = compareValue;
                    break;
                }
            }
        }
    };

    /**
     * Modify series extremes
     */
    wrap(seriesProto, 'getExtremes', function (proceed) {
        var extremes;

        proceed.apply(this, [].slice.call(arguments, 1));

        if (this.modifyValue) {
            extremes = [this.modifyValue(this.dataMin), this.modifyValue(this.dataMax)];
            this.dataMin = arrayMin(extremes);
            this.dataMax = arrayMax(extremes);
        }
    });

    /**
     * Add a utility method, setCompare, to the Y axis
     */
    Axis.prototype.setCompare = function (compare, redraw) {
        if (!this.isXAxis) {
            each(this.series, function (series) {
                series.setCompare(compare);
            });
            if (pick(redraw, true)) {
                this.chart.redraw();
            }
        }
    };

    /**
     * Extend the tooltip formatter by adding support for the point.change variable
     * as well as the changeDecimals option
     */
    Point.prototype.tooltipFormatter = function (pointFormat) {
        var point = this;

        pointFormat = pointFormat.replace(
            '{point.change}',
            (point.change > 0 ? '+' : '') + Highcharts.numberFormat(point.change, pick(point.series.tooltipOptions.changeDecimals, 2))
        );

        return pointTooltipFormatter.apply(this, [pointFormat]);
    };

    /* ****************************************************************************
     * End value compare logic                                                    *
     *****************************************************************************/


    /**
     * Extend the Series prototype to create a separate series clip box. This is related
     * to using multiple panes, and a future pane logic should incorporate this feature (#2754).
     */
    wrap(Series.prototype, 'render', function (proceed) {
        // Only do this on stock charts (#2939), and only if the series type handles clipping
        // in the animate method (#2975).
        if (this.chart.options._stock && this.xAxis) {

            // First render, initial clip box
            if (!this.clipBox && this.animate) {
                this.clipBox = merge(this.chart.clipBox);
                this.clipBox.width = this.xAxis.len;
                this.clipBox.height = this.yAxis.len;

            // On redrawing, resizing etc, update the clip rectangle
            } else if (this.chart[this.sharedClipKey]) {
                stop(this.chart[this.sharedClipKey]); // #2998
                this.chart[this.sharedClipKey].attr({
                    width: this.xAxis.len,
                    height: this.yAxis.len
                });
            }
        }
        proceed.call(this);
    });

    // global variables
    extend(Highcharts, {

        // Constructors
        Color: Color,
        Point: Point,
        Tick: Tick,
        Renderer: Renderer,
        SVGElement: SVGElement,
        SVGRenderer: SVGRenderer,

        // Various
        arrayMin: arrayMin,
        arrayMax: arrayMax,
        charts: charts,
        correctFloat: correctFloat,
        dateFormat: dateFormat,
        error: error,
        format: format,
        pathAnim: pathAnim,
        getOptions: getOptions,
        hasBidiBug: hasBidiBug,
        isTouchDevice: isTouchDevice,
        setOptions: setOptions,
        addEvent: addEvent,
        removeEvent: removeEvent,
        createElement: createElement,
        discardElement: discardElement,
        css: css,
        each: each,
        map: map,
        merge: merge,
        splat: splat,
        stableSort: stableSort,
        extendClass: extendClass,
        pInt: pInt,
        svg: hasSVG,
        canvas: useCanVG,
        vml: !hasSVG && !useCanVG,
        product: PRODUCT,
        version: VERSION
    });
    
    return Highcharts;
}));
