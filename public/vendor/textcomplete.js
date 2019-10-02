/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 5);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _strategy = __webpack_require__(2);

var _strategy2 = _interopRequireDefault(_strategy);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

/**
 * Encapsulate an result of each search results.
 */
var SearchResult = function () {

  /**
   * @param {object} data - An element of array callbacked by search function.
   */
  function SearchResult(data, term, strategy) {
    _classCallCheck(this, SearchResult);

    this.data = data;
    this.term = term;
    this.strategy = strategy;
  }

  _createClass(SearchResult, [{
    key: "replace",
    value: function replace(beforeCursor, afterCursor) {
      var replacement = this.strategy.replace(this.data);
      if (replacement !== null) {
        if (Array.isArray(replacement)) {
          afterCursor = replacement[1] + afterCursor;
          replacement = replacement[0];
        }
        var match = this.strategy.matchText(beforeCursor);
        if (match) {
          replacement = replacement.replace(/\$&/g, match[0]).replace(/\$(\d)/g, function (_, p1) {
            return match[parseInt(p1, 10)];
          });
          return [[beforeCursor.slice(0, match.index), replacement, beforeCursor.slice(match.index + match[0].length)].join(""), afterCursor];
        }
      }
    }
  }, {
    key: "render",
    value: function render() {
      return this.strategy.template(this.data, this.term);
    }
  }]);

  return SearchResult;
}();

exports.default = SearchResult;

/***/ }),
/* 1 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


var has = Object.prototype.hasOwnProperty
  , prefix = '~';

/**
 * Constructor to create a storage for our `EE` objects.
 * An `Events` instance is a plain object whose properties are event names.
 *
 * @constructor
 * @api private
 */
function Events() {}

//
// We try to not inherit from `Object.prototype`. In some engines creating an
// instance in this way is faster than calling `Object.create(null)` directly.
// If `Object.create(null)` is not supported we prefix the event names with a
// character to make sure that the built-in object properties are not
// overridden or used as an attack vector.
//
if (Object.create) {
  Events.prototype = Object.create(null);

  //
  // This hack is needed because the `__proto__` property is still inherited in
  // some old browsers like Android 4, iPhone 5.1, Opera 11 and Safari 5.
  //
  if (!new Events().__proto__) prefix = false;
}

/**
 * Representation of a single event listener.
 *
 * @param {Function} fn The listener function.
 * @param {Mixed} context The context to invoke the listener with.
 * @param {Boolean} [once=false] Specify if the listener is a one-time listener.
 * @constructor
 * @api private
 */
function EE(fn, context, once) {
  this.fn = fn;
  this.context = context;
  this.once = once || false;
}

/**
 * Minimal `EventEmitter` interface that is molded against the Node.js
 * `EventEmitter` interface.
 *
 * @constructor
 * @api public
 */
function EventEmitter() {
  this._events = new Events();
  this._eventsCount = 0;
}

/**
 * Return an array listing the events for which the emitter has registered
 * listeners.
 *
 * @returns {Array}
 * @api public
 */
EventEmitter.prototype.eventNames = function eventNames() {
  var names = []
    , events
    , name;

  if (this._eventsCount === 0) return names;

  for (name in (events = this._events)) {
    if (has.call(events, name)) names.push(prefix ? name.slice(1) : name);
  }

  if (Object.getOwnPropertySymbols) {
    return names.concat(Object.getOwnPropertySymbols(events));
  }

  return names;
};

/**
 * Return the listeners registered for a given event.
 *
 * @param {String|Symbol} event The event name.
 * @param {Boolean} exists Only check if there are listeners.
 * @returns {Array|Boolean}
 * @api public
 */
EventEmitter.prototype.listeners = function listeners(event, exists) {
  var evt = prefix ? prefix + event : event
    , available = this._events[evt];

  if (exists) return !!available;
  if (!available) return [];
  if (available.fn) return [available.fn];

  for (var i = 0, l = available.length, ee = new Array(l); i < l; i++) {
    ee[i] = available[i].fn;
  }

  return ee;
};

/**
 * Calls each of the listeners registered for a given event.
 *
 * @param {String|Symbol} event The event name.
 * @returns {Boolean} `true` if the event had listeners, else `false`.
 * @api public
 */
EventEmitter.prototype.emit = function emit(event, a1, a2, a3, a4, a5) {
  var evt = prefix ? prefix + event : event;

  if (!this._events[evt]) return false;

  var listeners = this._events[evt]
    , len = arguments.length
    , args
    , i;

  if (listeners.fn) {
    if (listeners.once) this.removeListener(event, listeners.fn, undefined, true);

    switch (len) {
      case 1: return listeners.fn.call(listeners.context), true;
      case 2: return listeners.fn.call(listeners.context, a1), true;
      case 3: return listeners.fn.call(listeners.context, a1, a2), true;
      case 4: return listeners.fn.call(listeners.context, a1, a2, a3), true;
      case 5: return listeners.fn.call(listeners.context, a1, a2, a3, a4), true;
      case 6: return listeners.fn.call(listeners.context, a1, a2, a3, a4, a5), true;
    }

    for (i = 1, args = new Array(len -1); i < len; i++) {
      args[i - 1] = arguments[i];
    }

    listeners.fn.apply(listeners.context, args);
  } else {
    var length = listeners.length
      , j;

    for (i = 0; i < length; i++) {
      if (listeners[i].once) this.removeListener(event, listeners[i].fn, undefined, true);

      switch (len) {
        case 1: listeners[i].fn.call(listeners[i].context); break;
        case 2: listeners[i].fn.call(listeners[i].context, a1); break;
        case 3: listeners[i].fn.call(listeners[i].context, a1, a2); break;
        case 4: listeners[i].fn.call(listeners[i].context, a1, a2, a3); break;
        default:
          if (!args) for (j = 1, args = new Array(len -1); j < len; j++) {
            args[j - 1] = arguments[j];
          }

          listeners[i].fn.apply(listeners[i].context, args);
      }
    }
  }

  return true;
};

/**
 * Add a listener for a given event.
 *
 * @param {String|Symbol} event The event name.
 * @param {Function} fn The listener function.
 * @param {Mixed} [context=this] The context to invoke the listener with.
 * @returns {EventEmitter} `this`.
 * @api public
 */
EventEmitter.prototype.on = function on(event, fn, context) {
  var listener = new EE(fn, context || this)
    , evt = prefix ? prefix + event : event;

  if (!this._events[evt]) this._events[evt] = listener, this._eventsCount++;
  else if (!this._events[evt].fn) this._events[evt].push(listener);
  else this._events[evt] = [this._events[evt], listener];

  return this;
};

/**
 * Add a one-time listener for a given event.
 *
 * @param {String|Symbol} event The event name.
 * @param {Function} fn The listener function.
 * @param {Mixed} [context=this] The context to invoke the listener with.
 * @returns {EventEmitter} `this`.
 * @api public
 */
EventEmitter.prototype.once = function once(event, fn, context) {
  var listener = new EE(fn, context || this, true)
    , evt = prefix ? prefix + event : event;

  if (!this._events[evt]) this._events[evt] = listener, this._eventsCount++;
  else if (!this._events[evt].fn) this._events[evt].push(listener);
  else this._events[evt] = [this._events[evt], listener];

  return this;
};

/**
 * Remove the listeners of a given event.
 *
 * @param {String|Symbol} event The event name.
 * @param {Function} fn Only remove the listeners that match this function.
 * @param {Mixed} context Only remove the listeners that have this context.
 * @param {Boolean} once Only remove one-time listeners.
 * @returns {EventEmitter} `this`.
 * @api public
 */
EventEmitter.prototype.removeListener = function removeListener(event, fn, context, once) {
  var evt = prefix ? prefix + event : event;

  if (!this._events[evt]) return this;
  if (!fn) {
    if (--this._eventsCount === 0) this._events = new Events();
    else delete this._events[evt];
    return this;
  }

  var listeners = this._events[evt];

  if (listeners.fn) {
    if (
         listeners.fn === fn
      && (!once || listeners.once)
      && (!context || listeners.context === context)
    ) {
      if (--this._eventsCount === 0) this._events = new Events();
      else delete this._events[evt];
    }
  } else {
    for (var i = 0, events = [], length = listeners.length; i < length; i++) {
      if (
           listeners[i].fn !== fn
        || (once && !listeners[i].once)
        || (context && listeners[i].context !== context)
      ) {
        events.push(listeners[i]);
      }
    }

    //
    // Reset the array, or remove it completely if we have no more listeners.
    //
    if (events.length) this._events[evt] = events.length === 1 ? events[0] : events;
    else if (--this._eventsCount === 0) this._events = new Events();
    else delete this._events[evt];
  }

  return this;
};

/**
 * Remove all listeners, or those of the specified event.
 *
 * @param {String|Symbol} [event] The event name.
 * @returns {EventEmitter} `this`.
 * @api public
 */
EventEmitter.prototype.removeAllListeners = function removeAllListeners(event) {
  var evt;

  if (event) {
    evt = prefix ? prefix + event : event;
    if (this._events[evt]) {
      if (--this._eventsCount === 0) this._events = new Events();
      else delete this._events[evt];
    }
  } else {
    this._events = new Events();
    this._eventsCount = 0;
  }

  return this;
};

//
// Alias methods names because people roll like that.
//
EventEmitter.prototype.off = EventEmitter.prototype.removeListener;
EventEmitter.prototype.addListener = EventEmitter.prototype.on;

//
// This function doesn't apply anymore.
//
EventEmitter.prototype.setMaxListeners = function setMaxListeners() {
  return this;
};

//
// Expose the prefix.
//
EventEmitter.prefixed = prefix;

//
// Allow `EventEmitter` to be imported as module namespace.
//
EventEmitter.EventEmitter = EventEmitter;

//
// Expose the module.
//
if (true) {
  module.exports = EventEmitter;
}


/***/ }),
/* 2 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var DEFAULT_INDEX = 2;

function DEFAULT_TEMPLATE(value) {
  return value;
}

/**
 * Properties for a strategy.
 *
 * @typedef
 */

/**
 * Encapsulate a single strategy.
 */
var Strategy = function () {
  function Strategy(props) {
    _classCallCheck(this, Strategy);

    this.props = props;
    this.cache = props.cache ? {} : null;
  }

  /**
   * @return {this}
   */


  _createClass(Strategy, [{
    key: "destroy",
    value: function destroy() {
      this.cache = null;
      return this;
    }
  }, {
    key: "search",
    value: function search(term, callback, match) {
      if (this.cache) {
        this.searchWithCache(term, callback, match);
      } else {
        this.props.search(term, callback, match);
      }
    }

    /**
     * @param {object} data - An element of array callbacked by search function.
     */

  }, {
    key: "replace",
    value: function replace(data) {
      return this.props.replace(data);
    }

    /** @private */

  }, {
    key: "searchWithCache",
    value: function searchWithCache(term, callback, match) {
      var _this = this;

      if (this.cache && this.cache[term]) {
        callback(this.cache[term]);
      } else {
        this.props.search(term, function (results) {
          if (_this.cache) {
            _this.cache[term] = results;
          }
          callback(results);
        }, match);
      }
    }

    /** @private */

  }, {
    key: "matchText",
    value: function matchText(text) {
      if (typeof this.match === "function") {
        return this.match(text);
      } else {
        return text.match(this.match);
      }
    }

    /** @private */

  }, {
    key: "match",
    get: function get() {
      return this.props.match;
    }

    /** @private */

  }, {
    key: "index",
    get: function get() {
      return typeof this.props.index === "number" ? this.props.index : DEFAULT_INDEX;
    }
  }, {
    key: "template",
    get: function get() {
      return this.props.template || DEFAULT_TEMPLATE;
    }
  }]);

  return Strategy;
}();

exports.default = Strategy;

/***/ }),
/* 3 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.calculateElementOffset = calculateElementOffset;
exports.getLineHeightPx = getLineHeightPx;
exports.calculateLineHeightPx = calculateLineHeightPx;


/**
 * Create a custom event
 *
 * @private
 */
var createCustomEvent = exports.createCustomEvent = function () {
  if (typeof window.CustomEvent === "function") {
    return function (type, options) {
      return new document.defaultView.CustomEvent(type, {
        cancelable: options && options.cancelable || false,
        detail: options && options.detail || undefined
      });
    };
  } else {
    // Custom event polyfill from
    // https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent#polyfill
    return function (type, options) {
      var event = document.createEvent("CustomEvent");
      event.initCustomEvent(type,
      /* bubbles */false, options && options.cancelable || false, options && options.detail || undefined);
      return event;
    };
  }
}

/**
 * Get the current coordinates of the `el` relative to the document.
 *
 * @private
 */
();function calculateElementOffset(el) {
  var rect = el.getBoundingClientRect();
  var _el$ownerDocument = el.ownerDocument,
      defaultView = _el$ownerDocument.defaultView,
      documentElement = _el$ownerDocument.documentElement;

  var offset = {
    top: rect.top + defaultView.pageYOffset,
    left: rect.left + defaultView.pageXOffset
  };
  if (documentElement) {
    offset.top -= documentElement.clientTop;
    offset.left -= documentElement.clientLeft;
  }
  return offset;
}

var CHAR_CODE_ZERO = "0".charCodeAt(0);
var CHAR_CODE_NINE = "9".charCodeAt(0);

function isDigit(charCode) {
  return charCode >= CHAR_CODE_ZERO && charCode <= CHAR_CODE_NINE;
}

/**
 * Returns the line-height of the given node in pixels.
 *
 * @private
 */
function getLineHeightPx(node) {
  var computedStyle = window.getComputedStyle(node

  // If the char code starts with a digit, it is either a value in pixels,
  // or unitless, as per:
  // https://drafts.csswg.org/css2/visudet.html#propdef-line-height
  // https://drafts.csswg.org/css2/cascade.html#computed-value
  );if (isDigit(computedStyle.lineHeight.charCodeAt(0))) {
    // In real browsers the value is *always* in pixels, even for unit-less
    // line-heights. However, we still check as per the spec.
    if (isDigit(computedStyle.lineHeight.charCodeAt(computedStyle.lineHeight.length - 1))) {
      return parseFloat(computedStyle.lineHeight) * parseFloat(computedStyle.fontSize);
    } else {
      return parseFloat(computedStyle.lineHeight);
    }
  }

  // Otherwise, the value is "normal".
  // If the line-height is "normal", calculate by font-size
  return calculateLineHeightPx(node.nodeName, computedStyle);
}

/**
 * Returns calculated line-height of the given node in pixels.
 *
 * @private
 */
function calculateLineHeightPx(nodeName, computedStyle) {
  var body = document.body;
  if (!body) {
    return 0;
  }

  var tempNode = document.createElement(nodeName);
  tempNode.innerHTML = "&nbsp;";
  tempNode.style.fontSize = computedStyle.fontSize;
  tempNode.style.fontFamily = computedStyle.fontFamily;
  tempNode.style.padding = "0";
  body.appendChild(tempNode

  // Make sure textarea has only 1 row
  );if (tempNode instanceof HTMLTextAreaElement) {
    ;tempNode.rows = 1;
  }

  // Assume the height of the element is the line-height
  var height = tempNode.offsetHeight;
  body.removeChild(tempNode);

  return height;
}

/***/ }),
/* 4 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _eventemitter = __webpack_require__(1);

var _eventemitter2 = _interopRequireDefault(_eventemitter);

var _utils = __webpack_require__(3);

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }
/*eslint no-unused-vars: off*/

/**
 * Abstract class representing a editor target.
 *
 * Editor classes must implement `#applySearchResult`, `#getCursorOffset` and
 * `#getBeforeCursor` methods.
 *
 * Editor classes must invoke `#emitMoveEvent`, `#emitEnterEvent`,
 * `#emitChangeEvent` and `#emitEscEvent` at proper timing.
 *
 * @abstract
 */


/** @typedef */
var Editor = function (_EventEmitter) {
  _inherits(Editor, _EventEmitter);

  function Editor() {
    _classCallCheck(this, Editor);

    return _possibleConstructorReturn(this, (Editor.__proto__ || Object.getPrototypeOf(Editor)).apply(this, arguments));
  }

  _createClass(Editor, [{
    key: "destroy",

    /**
     * It is called when associated textcomplete object is destroyed.
     *
     * @return {this}
     */
    value: function destroy() {
      return this;
    }

    /**
     * It is called when a search result is selected by a user.
     */

  }, {
    key: "applySearchResult",
    value: function applySearchResult(_) {
      throw new Error("Not implemented.");
    }

    /**
     * The input cursor's absolute coordinates from the window's left
     * top corner.
     */

  }, {
    key: "getCursorOffset",
    value: function getCursorOffset() {
      throw new Error("Not implemented.");
    }

    /**
     * Editor string value from head to cursor.
     * Returns null if selection type is range not cursor.
     */

  }, {
    key: "getBeforeCursor",
    value: function getBeforeCursor() {
      throw new Error("Not implemented.");
    }

    /**
     * Emit a move event, which moves active dropdown element.
     * Child class must call this method at proper timing with proper parameter.
     *
     * @see {@link Textarea} for live example.
     */

  }, {
    key: "emitMoveEvent",
    value: function emitMoveEvent(code) {
      var moveEvent = (0, _utils.createCustomEvent)("move", {
        cancelable: true,
        detail: {
          code: code
        }
      });
      this.emit("move", moveEvent);
      return moveEvent;
    }

    /**
     * Emit a enter event, which selects current search result.
     * Child class must call this method at proper timing.
     *
     * @see {@link Textarea} for live example.
     */

  }, {
    key: "emitEnterEvent",
    value: function emitEnterEvent() {
      var enterEvent = (0, _utils.createCustomEvent)("enter", { cancelable: true });
      this.emit("enter", enterEvent);
      return enterEvent;
    }

    /**
     * Emit a change event, which triggers auto completion.
     * Child class must call this method at proper timing.
     *
     * @see {@link Textarea} for live example.
     */

  }, {
    key: "emitChangeEvent",
    value: function emitChangeEvent() {
      var changeEvent = (0, _utils.createCustomEvent)("change", {
        detail: {
          beforeCursor: this.getBeforeCursor()
        }
      });
      this.emit("change", changeEvent);
      return changeEvent;
    }

    /**
     * Emit a esc event, which hides dropdown element.
     * Child class must call this method at proper timing.
     *
     * @see {@link Textarea} for live example.
     */

  }, {
    key: "emitEscEvent",
    value: function emitEscEvent() {
      var escEvent = (0, _utils.createCustomEvent)("esc", { cancelable: true });
      this.emit("esc", escEvent);
      return escEvent;
    }

    /**
     * Helper method for parsing KeyboardEvent.
     *
     * @see {@link Textarea} for live example.
     */

  }, {
    key: "getCode",
    value: function getCode(e) {
      return e.keyCode === 9 ? "ENTER" // tab
      : e.keyCode === 13 ? "ENTER" // enter
      : e.keyCode === 27 ? "ESC" // esc
      : e.keyCode === 38 ? "UP" // up
      : e.keyCode === 40 ? "DOWN" // down
      : e.keyCode === 78 && e.ctrlKey ? "DOWN" // ctrl-n
      : e.keyCode === 80 && e.ctrlKey ? "UP" // ctrl-p
      : "OTHER";
    }
  }]);

  return Editor;
}(_eventemitter2.default);

exports.default = Editor;

/***/ }),
/* 5 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
/* WEBPACK VAR INJECTION */(function(global) {

var _textcomplete = __webpack_require__(7);

var _textcomplete2 = _interopRequireDefault(_textcomplete);

var _textarea = __webpack_require__(12);

var _textarea2 = _interopRequireDefault(_textarea);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var editors = void 0;
if (global.Textcomplete && global.Textcomplete.editors) {
  editors = global.Textcomplete.editors;
} else {
  editors = {};
}
editors.Textarea = _textarea2.default;

global.Textcomplete = _textcomplete2.default;
global.Textcomplete.editors = editors;
/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(6)))

/***/ }),
/* 6 */
/***/ (function(module, exports) {

var g;

// This works in non-strict mode
g = (function() {
	return this;
})();

try {
	// This works if eval is allowed (see CSP)
	g = g || Function("return this")() || (1,eval)("this");
} catch(e) {
	// This works if the window reference is available
	if(typeof window === "object")
		g = window;
}

// g can still be undefined, but nothing to do about it...
// We return undefined, instead of nothing here, so it's
// easier to handle this case. if(!global) { ...}

module.exports = g;


/***/ }),
/* 7 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _completer = __webpack_require__(8);

var _completer2 = _interopRequireDefault(_completer);

var _editor = __webpack_require__(4);

var _editor2 = _interopRequireDefault(_editor);

var _dropdown = __webpack_require__(10);

var _dropdown2 = _interopRequireDefault(_dropdown);

var _strategy = __webpack_require__(2);

var _strategy2 = _interopRequireDefault(_strategy);

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

var _eventemitter = __webpack_require__(1);

var _eventemitter2 = _interopRequireDefault(_eventemitter);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var CALLBACK_METHODS = ["handleChange", "handleEnter", "handleEsc", "handleHit", "handleMove", "handleSelect"];

/** @typedef */

/**
 * The core of textcomplete. It acts as a mediator.
 */
var Textcomplete = function (_EventEmitter) {
  _inherits(Textcomplete, _EventEmitter);

  /**
   * @param {Editor} editor - Where the textcomplete works on.
   */
  function Textcomplete(editor) {
    var options = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

    _classCallCheck(this, Textcomplete);

    var _this = _possibleConstructorReturn(this, (Textcomplete.__proto__ || Object.getPrototypeOf(Textcomplete)).call(this));

    _this.completer = new _completer2.default();
    _this.isQueryInFlight = false;
    _this.nextPendingQuery = null;
    _this.dropdown = new _dropdown2.default(options.dropdown || {});
    _this.editor = editor;
    _this.options = options;

    CALLBACK_METHODS.forEach(function (method) {
      ;_this[method] = _this[method].bind(_this);
    });

    _this.startListening();
    return _this;
  }

  /**
   * @return {this}
   */


  _createClass(Textcomplete, [{
    key: "destroy",
    value: function destroy() {
      var destroyEditor = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : true;

      this.completer.destroy();
      this.dropdown.destroy();
      if (destroyEditor) {
        this.editor.destroy();
      }
      this.stopListening();
      return this;
    }

    /**
     * @return {this}
     */

  }, {
    key: "hide",
    value: function hide() {
      this.dropdown.deactivate();
      return this;
    }

    /**
     * @return {this}
     * @example
     * textcomplete.register([{
     *   match: /(^|\s)(\w+)$/,
     *   search: function (term, callback) {
     *     $.ajax({ ... })
     *       .done(callback)
     *       .fail([]);
     *   },
     *   replace: function (value) {
     *     return '$1' + value + ' ';
     *   }
     * }]);
     */

  }, {
    key: "register",
    value: function register(strategyPropsArray) {
      var _this2 = this;

      strategyPropsArray.forEach(function (props) {
        _this2.completer.registerStrategy(new _strategy2.default(props));
      });
      return this;
    }

    /**
     * Start autocompleting.
     *
     * @param {string} text - Head to input cursor.
     * @return {this}
     */

  }, {
    key: "trigger",
    value: function trigger(text) {
      if (this.isQueryInFlight) {
        this.nextPendingQuery = text;
      } else {
        this.isQueryInFlight = true;
        this.nextPendingQuery = null;
        this.completer.run(text);
      }
      return this;
    }

    /** @private */

  }, {
    key: "handleHit",
    value: function handleHit(_ref) {
      var searchResults = _ref.searchResults;

      if (searchResults.length) {
        this.dropdown.render(searchResults, this.editor.getCursorOffset());
      } else {
        this.dropdown.deactivate();
      }
      this.isQueryInFlight = false;
      if (this.nextPendingQuery !== null) {
        this.trigger(this.nextPendingQuery);
      }
    }

    /** @private */

  }, {
    key: "handleMove",
    value: function handleMove(e) {
      e.detail.code === "UP" ? this.dropdown.up(e) : this.dropdown.down(e);
    }

    /** @private */

  }, {
    key: "handleEnter",
    value: function handleEnter(e) {
      var activeItem = this.dropdown.getActiveItem();
      if (activeItem) {
        this.dropdown.select(activeItem);
        e.preventDefault();
      } else {
        this.dropdown.deactivate();
      }
    }

    /** @private */

  }, {
    key: "handleEsc",
    value: function handleEsc(e) {
      if (this.dropdown.shown) {
        this.dropdown.deactivate();
        e.preventDefault();
      }
    }

    /** @private */

  }, {
    key: "handleChange",
    value: function handleChange(e) {
      if (e.detail.beforeCursor != null) {
        this.trigger(e.detail.beforeCursor);
      } else {
        this.dropdown.deactivate();
      }
    }

    /** @private */

  }, {
    key: "handleSelect",
    value: function handleSelect(selectEvent) {
      this.emit("select", selectEvent);
      if (!selectEvent.defaultPrevented) {
        this.editor.applySearchResult(selectEvent.detail.searchResult);
      }
    }

    /** @private */

  }, {
    key: "startListening",
    value: function startListening() {
      var _this3 = this;

      this.editor.on("move", this.handleMove).on("enter", this.handleEnter).on("esc", this.handleEsc).on("change", this.handleChange);
      this.dropdown.on("select", this.handleSelect);["show", "shown", "render", "rendered", "selected", "hidden", "hide"].forEach(function (eventName) {
        _this3.dropdown.on(eventName, function () {
          return _this3.emit(eventName);
        });
      });
      this.completer.on("hit", this.handleHit);
    }

    /** @private */

  }, {
    key: "stopListening",
    value: function stopListening() {
      this.completer.removeAllListeners();
      this.dropdown.removeAllListeners();
      this.editor.removeListener("move", this.handleMove).removeListener("enter", this.handleEnter).removeListener("esc", this.handleEsc).removeListener("change", this.handleChange);
    }
  }]);

  return Textcomplete;
}(_eventemitter2.default);

exports.default = Textcomplete;

/***/ }),
/* 8 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _eventemitter = __webpack_require__(1);

var _eventemitter2 = _interopRequireDefault(_eventemitter);

var _query = __webpack_require__(9);

var _query2 = _interopRequireDefault(_query);

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

var _strategy = __webpack_require__(2);

var _strategy2 = _interopRequireDefault(_strategy);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var CALLBACK_METHODS = ["handleQueryResult"];

/**
 * Complete engine.
 */

var Completer = function (_EventEmitter) {
  _inherits(Completer, _EventEmitter);

  function Completer() {
    _classCallCheck(this, Completer);

    var _this = _possibleConstructorReturn(this, (Completer.__proto__ || Object.getPrototypeOf(Completer)).call(this));

    _this.strategies = [];

    CALLBACK_METHODS.forEach(function (method) {
      ;_this[method] = _this[method].bind(_this);
    });
    return _this;
  }

  /**
   * @return {this}
   */


  _createClass(Completer, [{
    key: "destroy",
    value: function destroy() {
      this.strategies.forEach(function (strategy) {
        return strategy.destroy();
      });
      return this;
    }

    /**
     * Register a strategy to the completer.
     *
     * @return {this}
     */

  }, {
    key: "registerStrategy",
    value: function registerStrategy(strategy) {
      this.strategies.push(strategy);
      return this;
    }

    /**
     * @param {string} text - Head to input cursor.
     */

  }, {
    key: "run",
    value: function run(text) {
      var query = this.extractQuery(text);
      if (query) {
        query.execute(this.handleQueryResult);
      } else {
        this.handleQueryResult([]);
      }
    }

    /**
     * Find a query, which matches to the given text.
     *
     * @private
     */

  }, {
    key: "extractQuery",
    value: function extractQuery(text) {
      for (var i = 0; i < this.strategies.length; i++) {
        var query = _query2.default.build(this.strategies[i], text);
        if (query) {
          return query;
        }
      }
      return null;
    }

    /**
     * Callbacked by {@link Query#execute}.
     *
     * @private
     */

  }, {
    key: "handleQueryResult",
    value: function handleQueryResult(searchResults) {
      this.emit("hit", { searchResults: searchResults });
    }
  }]);

  return Completer;
}(_eventemitter2.default);

exports.default = Completer;

/***/ }),
/* 9 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

var _strategy = __webpack_require__(2);

var _strategy2 = _interopRequireDefault(_strategy);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

/**
 * Encapsulate matching condition between a Strategy and current editor's value.
 */
var Query = function () {
  _createClass(Query, null, [{
    key: "build",


    /**
     * Build a Query object by the given string if this matches to the string.
     *
     * @param {string} text - Head to input cursor.
     */
    value: function build(strategy, text) {
      if (typeof strategy.props.context === "function") {
        var context = strategy.props.context(text);
        if (typeof context === "string") {
          text = context;
        } else if (!context) {
          return null;
        }
      }
      var match = strategy.matchText(text);
      return match ? new Query(strategy, match[strategy.index], match) : null;
    }
  }]);

  function Query(strategy, term, match) {
    _classCallCheck(this, Query);

    this.strategy = strategy;
    this.term = term;
    this.match = match;
  }

  /**
   * Invoke search strategy and callback the given function.
   */


  _createClass(Query, [{
    key: "execute",
    value: function execute(callback) {
      var _this = this;

      this.strategy.search(this.term, function (results) {
        callback(results.map(function (result) {
          return new _search_result2.default(result, _this.term, _this.strategy);
        }));
      }, this.match);
    }
  }]);

  return Query;
}();

exports.default = Query;

/***/ }),
/* 10 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _eventemitter = __webpack_require__(1);

var _eventemitter2 = _interopRequireDefault(_eventemitter);

var _dropdown_item = __webpack_require__(11);

var _dropdown_item2 = _interopRequireDefault(_dropdown_item);

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

var _utils = __webpack_require__(3);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var DEFAULT_CLASS_NAME = "dropdown-menu textcomplete-dropdown";

/** @typedef */

/**
 * Encapsulate a dropdown view.
 *
 * @prop {boolean} shown - Whether the #el is shown or not.
 * @prop {DropdownItem[]} items - The array of rendered dropdown items.
 */
var Dropdown = function (_EventEmitter) {
  _inherits(Dropdown, _EventEmitter);

  _createClass(Dropdown, null, [{
    key: "createElement",
    value: function createElement() {
      var el = document.createElement("ul");
      var style = el.style;
      style.display = "none";
      style.position = "absolute";
      style.zIndex = "10000";
      var body = document.body;
      if (body) {
        body.appendChild(el);
      }
      return el;
    }
  }]);

  function Dropdown(options) {
    _classCallCheck(this, Dropdown);

    var _this = _possibleConstructorReturn(this, (Dropdown.__proto__ || Object.getPrototypeOf(Dropdown)).call(this));

    _this.shown = false;
    _this.items = [];
    _this.activeItem = null;
    _this.footer = options.footer;
    _this.header = options.header;
    _this.maxCount = options.maxCount || 10;
    _this.el.className = options.className || DEFAULT_CLASS_NAME;
    _this.rotate = options.hasOwnProperty("rotate") ? options.rotate : true;
    _this.placement = options.placement;
    _this.itemOptions = options.item || {};
    var style = options.style;
    if (style) {
      Object.keys(style).forEach(function (key) {
        ;_this.el.style[key] = style[key];
      });
    }
    return _this;
  }

  /**
   * @return {this}
   */


  _createClass(Dropdown, [{
    key: "destroy",
    value: function destroy() {
      var parentNode = this.el.parentNode;
      if (parentNode) {
        parentNode.removeChild(this.el);
      }
      this.clear()._el = null;
      return this;
    }
  }, {
    key: "render",


    /**
     * Render the given data as dropdown items.
     *
     * @return {this}
     */
    value: function render(searchResults, cursorOffset) {
      var _this2 = this;

      var renderEvent = (0, _utils.createCustomEvent)("render", { cancelable: true });
      this.emit("render", renderEvent);
      if (renderEvent.defaultPrevented) {
        return this;
      }
      var rawResults = searchResults.map(function (searchResult) {
        return searchResult.data;
      });
      var dropdownItems = searchResults.slice(0, this.maxCount || searchResults.length).map(function (searchResult) {
        return new _dropdown_item2.default(searchResult, _this2.itemOptions);
      });
      this.clear().setStrategyId(searchResults[0]).renderEdge(rawResults, "header").append(dropdownItems).renderEdge(rawResults, "footer").show().setOffset(cursorOffset);
      this.emit("rendered", (0, _utils.createCustomEvent)("rendered"));
      return this;
    }

    /**
     * Hide the dropdown then sweep out items.
     *
     * @return {this}
     */

  }, {
    key: "deactivate",
    value: function deactivate() {
      return this.hide().clear();
    }

    /**
     * @return {this}
     */

  }, {
    key: "select",
    value: function select(dropdownItem) {
      var detail = { searchResult: dropdownItem.searchResult };
      var selectEvent = (0, _utils.createCustomEvent)("select", {
        cancelable: true,
        detail: detail
      });
      this.emit("select", selectEvent);
      if (selectEvent.defaultPrevented) {
        return this;
      }
      this.deactivate();
      this.emit("selected", (0, _utils.createCustomEvent)("selected", { detail: detail }));
      return this;
    }

    /**
     * @return {this}
     */

  }, {
    key: "up",
    value: function up(e) {
      return this.shown ? this.moveActiveItem("prev", e) : this;
    }

    /**
     * @return {this}
     */

  }, {
    key: "down",
    value: function down(e) {
      return this.shown ? this.moveActiveItem("next", e) : this;
    }

    /**
     * Retrieve the active item.
     */

  }, {
    key: "getActiveItem",
    value: function getActiveItem() {
      return this.activeItem;
    }

    /**
     * Add items to dropdown.
     *
     * @private
     */

  }, {
    key: "append",
    value: function append(items) {
      var _this3 = this;

      var fragment = document.createDocumentFragment();
      items.forEach(function (item) {
        _this3.items.push(item);
        item.appended(_this3);
        fragment.appendChild(item.el);
      });
      this.el.appendChild(fragment);
      return this;
    }

    /** @private */

  }, {
    key: "setOffset",
    value: function setOffset(cursorOffset) {
      var doc = document.documentElement;
      if (doc) {
        var elementWidth = this.el.offsetWidth;
        if (cursorOffset.left) {
          var browserWidth = doc.clientWidth;
          if (cursorOffset.left + elementWidth > browserWidth) {
            cursorOffset.left = browserWidth - elementWidth;
          }
          this.el.style.left = cursorOffset.left + "px";
        } else if (cursorOffset.right) {
          if (cursorOffset.right - elementWidth < 0) {
            cursorOffset.right = 0;
          }
          this.el.style.right = cursorOffset.right + "px";
        }
        if (this.isPlacementTop()) {
          this.el.style.bottom = doc.clientHeight - cursorOffset.top + cursorOffset.lineHeight + "px";
        } else {
          this.el.style.top = cursorOffset.top + "px";
        }
      }
      return this;
    }

    /**
     * Show the element.
     *
     * @private
     */

  }, {
    key: "show",
    value: function show() {
      if (!this.shown) {
        var showEvent = (0, _utils.createCustomEvent)("show", { cancelable: true });
        this.emit("show", showEvent);
        if (showEvent.defaultPrevented) {
          return this;
        }
        this.el.style.display = "block";
        this.shown = true;
        this.emit("shown", (0, _utils.createCustomEvent)("shown"));
      }
      return this;
    }

    /**
     * Hide the element.
     *
     * @private
     */

  }, {
    key: "hide",
    value: function hide() {
      if (this.shown) {
        var hideEvent = (0, _utils.createCustomEvent)("hide", { cancelable: true });
        this.emit("hide", hideEvent);
        if (hideEvent.defaultPrevented) {
          return this;
        }
        this.el.style.display = "none";
        this.shown = false;
        this.emit("hidden", (0, _utils.createCustomEvent)("hidden"));
      }
      return this;
    }

    /**
     * Clear search results.
     *
     * @private
     */

  }, {
    key: "clear",
    value: function clear() {
      this.el.innerHTML = "";
      this.items.forEach(function (item) {
        return item.destroy();
      });
      this.items = [];
      return this;
    }

    /** @private */

  }, {
    key: "moveActiveItem",
    value: function moveActiveItem(direction, e) {
      var nextActiveItem = direction === "next" ? this.activeItem ? this.activeItem.next : this.items[0] : this.activeItem ? this.activeItem.prev : this.items[this.items.length - 1];
      if (nextActiveItem) {
        nextActiveItem.activate();
        e.preventDefault();
      }
      return this;
    }

    /** @private */

  }, {
    key: "setStrategyId",
    value: function setStrategyId(searchResult) {
      var strategyId = searchResult && searchResult.strategy.props.id;
      if (strategyId) {
        this.el.setAttribute("data-strategy", strategyId);
      } else {
        this.el.removeAttribute("data-strategy");
      }
      return this;
    }

    /**
     * @private
     * @param {object[]} rawResults - What callbacked by search function.
     */

  }, {
    key: "renderEdge",
    value: function renderEdge(rawResults, type) {
      var source = (type === "header" ? this.header : this.footer) || "";
      var content = typeof source === "function" ? source(rawResults) : source;
      var li = document.createElement("li");
      li.classList.add("textcomplete-" + type);
      li.innerHTML = content;
      this.el.appendChild(li);
      return this;
    }

    /** @private */

  }, {
    key: "isPlacementTop",
    value: function isPlacementTop() {
      return this.placement === "top";
    }
  }, {
    key: "el",
    get: function get() {
      if (!this._el) {
        this._el = Dropdown.createElement();
      }
      return this._el;
    }
  }]);

  return Dropdown;
}(_eventemitter2.default);

exports.default = Dropdown;

/***/ }),
/* 11 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DEFAULT_CLASS_NAME = undefined;

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

var DEFAULT_CLASS_NAME = exports.DEFAULT_CLASS_NAME = "textcomplete-item";
var CALLBACK_METHODS = ["onClick", "onMouseover"];

/** @typedef */


// Declare interface instead of importing Dropdown itself to prevent circular dependency.

/**
 * Encapsulate an item of dropdown.
 */
var DropdownItem = function () {
  function DropdownItem(searchResult, options) {
    var _this = this;

    _classCallCheck(this, DropdownItem);

    this.searchResult = searchResult;
    this.active = false;
    this.className = options.className || DEFAULT_CLASS_NAME;
    this.activeClassName = this.className + " active";

    CALLBACK_METHODS.forEach(function (method) {
      ;_this[method] = _this[method].bind(_this);
    });
  }

  _createClass(DropdownItem, [{
    key: "destroy",


    /**
     * Try to free resources and perform other cleanup operations.
     */
    value: function destroy() {
      this.el.removeEventListener("mousedown", this.onClick, false);
      this.el.removeEventListener("mouseover", this.onMouseover, false);
      this.el.removeEventListener("touchstart", this.onClick, false);
      if (this.active) {
        this.dropdown.activeItem = null;
      }
      // This element has already been removed by {@link Dropdown#clear}.
      this._el = null;
    }

    /**
     * Callbacked when it is appended to a dropdown.
     *
     * @see Dropdown#append
     */

  }, {
    key: "appended",
    value: function appended(dropdown) {
      this.dropdown = dropdown;
      this.siblings = dropdown.items;
      this.index = this.siblings.length - 1;
    }

    /**
     * Deactivate active item then activate itself.
     *
     * @return {this}
     */

  }, {
    key: "activate",
    value: function activate() {
      if (!this.active) {
        var _activeItem = this.dropdown.getActiveItem();
        if (_activeItem) {
          _activeItem.deactivate();
        }
        this.dropdown.activeItem = this;
        this.active = true;
        this.el.className = this.activeClassName;
      }
      return this;
    }

    /**
     * Get the next sibling.
     */

  }, {
    key: "deactivate",


    /** @private */
    value: function deactivate() {
      if (this.active) {
        this.active = false;
        this.el.className = this.className;
        this.dropdown.activeItem = null;
      }
      return this;
    }

    /** @private */

  }, {
    key: "onClick",
    value: function onClick(e) {
      e.preventDefault // Prevent blur event
      ();this.dropdown.select(this);
    }

    /** @private */

  }, {
    key: "onMouseover",
    value: function onMouseover() {
      this.activate();
    }
  }, {
    key: "el",
    get: function get() {
      if (this._el) {
        return this._el;
      }
      var li = document.createElement("li");
      li.className = this.active ? this.activeClassName : this.className;
      var a = document.createElement("a");
      a.innerHTML = this.searchResult.render();
      li.appendChild(a);
      this._el = li;
      li.addEventListener("mousedown", this.onClick);
      li.addEventListener("mouseover", this.onMouseover);
      li.addEventListener("touchstart", this.onClick);
      return li;
    }
  }, {
    key: "next",
    get: function get() {
      var nextIndex = void 0;
      if (this.index === this.siblings.length - 1) {
        if (!this.dropdown.rotate) {
          return null;
        }
        nextIndex = 0;
      } else {
        nextIndex = this.index + 1;
      }
      return this.siblings[nextIndex];
    }

    /**
     * Get the previous sibling.
     */

  }, {
    key: "prev",
    get: function get() {
      var nextIndex = void 0;
      if (this.index === 0) {
        if (!this.dropdown.rotate) {
          return null;
        }
        nextIndex = this.siblings.length - 1;
      } else {
        nextIndex = this.index - 1;
      }
      return this.siblings[nextIndex];
    }
  }]);

  return DropdownItem;
}();

exports.default = DropdownItem;

/***/ }),
/* 12 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _get = function get(object, property, receiver) { if (object === null) object = Function.prototype; var desc = Object.getOwnPropertyDescriptor(object, property); if (desc === undefined) { var parent = Object.getPrototypeOf(object); if (parent === null) { return undefined; } else { return get(parent, property, receiver); } } else if ("value" in desc) { return desc.value; } else { var getter = desc.get; if (getter === undefined) { return undefined; } return getter.call(receiver); } };

var _update = __webpack_require__(13);

var _update2 = _interopRequireDefault(_update);

var _editor = __webpack_require__(4);

var _editor2 = _interopRequireDefault(_editor);

var _utils = __webpack_require__(3);

var _search_result = __webpack_require__(0);

var _search_result2 = _interopRequireDefault(_search_result);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var getCaretCoordinates = __webpack_require__(14);

var CALLBACK_METHODS = ["onInput", "onKeydown"];

/**
 * Encapsulate the target textarea element.
 */

var Textarea = function (_Editor) {
  _inherits(Textarea, _Editor);

  /**
   * @param {HTMLTextAreaElement} el - Where the textcomplete works on.
   */
  function Textarea(el) {
    _classCallCheck(this, Textarea);

    var _this = _possibleConstructorReturn(this, (Textarea.__proto__ || Object.getPrototypeOf(Textarea)).call(this));

    _this.el = el;

    CALLBACK_METHODS.forEach(function (method) {
      ;_this[method] = _this[method].bind(_this);
    });

    _this.startListening();
    return _this;
  }

  /**
   * @return {this}
   */


  _createClass(Textarea, [{
    key: "destroy",
    value: function destroy() {
      _get(Textarea.prototype.__proto__ || Object.getPrototypeOf(Textarea.prototype), "destroy", this).call(this);
      this.stopListening
      // Release the element reference early to help garbage collection.
      ();this.el = null;
      return this;
    }

    /**
     * Implementation for {@link Editor#applySearchResult}
     */

  }, {
    key: "applySearchResult",
    value: function applySearchResult(searchResult) {
      var before = this.getBeforeCursor();
      if (before != null) {
        var replace = searchResult.replace(before, this.getAfterCursor());
        this.el.focus // Clicking a dropdown item removes focus from the element.
        ();if (Array.isArray(replace)) {
          (0, _update2.default)(this.el, replace[0], replace[1]);
          this.el.dispatchEvent(new Event("input"));
        }
      }
    }

    /**
     * Implementation for {@link Editor#getCursorOffset}
     */

  }, {
    key: "getCursorOffset",
    value: function getCursorOffset() {
      var elOffset = (0, _utils.calculateElementOffset)(this.el);
      var elScroll = this.getElScroll();
      var cursorPosition = this.getCursorPosition();
      var lineHeight = (0, _utils.getLineHeightPx)(this.el);
      var top = elOffset.top - elScroll.top + cursorPosition.top + lineHeight;
      var left = elOffset.left - elScroll.left + cursorPosition.left;
      if (this.el.dir !== "rtl") {
        return { top: top, left: left, lineHeight: lineHeight };
      } else {
        var right = document.documentElement ? document.documentElement.clientWidth - left : 0;
        return { top: top, right: right, lineHeight: lineHeight };
      }
    }

    /**
     * Implementation for {@link Editor#getBeforeCursor}
     */

  }, {
    key: "getBeforeCursor",
    value: function getBeforeCursor() {
      return this.el.selectionStart !== this.el.selectionEnd ? null : this.el.value.substring(0, this.el.selectionEnd);
    }

    /** @private */

  }, {
    key: "getAfterCursor",
    value: function getAfterCursor() {
      return this.el.value.substring(this.el.selectionEnd);
    }

    /** @private */

  }, {
    key: "getElScroll",
    value: function getElScroll() {
      return { top: this.el.scrollTop, left: this.el.scrollLeft };
    }

    /**
     * The input cursor's relative coordinates from the textarea's left
     * top corner.
     *
     * @private
     */

  }, {
    key: "getCursorPosition",
    value: function getCursorPosition() {
      return getCaretCoordinates(this.el, this.el.selectionEnd);
    }

    /** @private */

  }, {
    key: "onInput",
    value: function onInput() {
      this.emitChangeEvent();
    }

    /** @private */

  }, {
    key: "onKeydown",
    value: function onKeydown(e) {
      var code = this.getCode(e);
      var event = void 0;
      if (code === "UP" || code === "DOWN") {
        event = this.emitMoveEvent(code);
      } else if (code === "ENTER") {
        event = this.emitEnterEvent();
      } else if (code === "ESC") {
        event = this.emitEscEvent();
      }
      if (event && event.defaultPrevented) {
        e.preventDefault();
      }
    }

    /** @private */

  }, {
    key: "startListening",
    value: function startListening() {
      this.el.addEventListener("input", this.onInput);
      this.el.addEventListener("keydown", this.onKeydown);
    }

    /** @private */

  }, {
    key: "stopListening",
    value: function stopListening() {
      this.el.removeEventListener("input", this.onInput);
      this.el.removeEventListener("keydown", this.onKeydown);
    }
  }]);

  return Textarea;
}(_editor2.default);

exports.default = Textarea;

/***/ }),
/* 13 */
/***/ (function(module, exports, __webpack_require__) {

"use strict";


Object.defineProperty(exports, "__esModule", {
  value: true
});

exports.default = function (el, headToCursor, cursorToTail) {
  var curr = el.value,
      // strA + strB1 + strC
  next = headToCursor + (cursorToTail || ''),
      // strA + strB2 + strC
  activeElement = document.activeElement;

  //  Calculate length of strA and strC
  var aLength = 0,
      cLength = 0;
  while (aLength < curr.length && aLength < next.length && curr[aLength] === next[aLength]) {
    aLength++;
  }
  while (curr.length - cLength - 1 >= 0 && next.length - cLength - 1 >= 0 && curr[curr.length - cLength - 1] === next[next.length - cLength - 1]) {
    cLength++;
  }
  aLength = Math.min(aLength, Math.min(curr.length, next.length) - cLength);

  // Select strB1
  el.setSelectionRange(aLength, curr.length - cLength);

  // Get strB2
  var strB2 = next.substring(aLength, next.length - cLength);

  // Replace strB1 with strB2
  el.focus();
  if (!document.execCommand('insertText', false, strB2)) {
    // Document.execCommand returns false if the command is not supported.
    // Firefox and IE returns false in this case.
    el.value = next;
    el.dispatchEvent(createInputEvent());
  }

  // Move cursor to the end of headToCursor
  el.setSelectionRange(headToCursor.length, headToCursor.length);

  activeElement && activeElement.focus();
  return el;
};

function createInputEvent() {
  if (typeof Event !== "undefined") {
    return new Event("input", { bubbles: true, cancelable: true });
  } else {
    var event = document.createEvent("Event");
    event.initEvent("input", true, true);
    return event;
  }
}

/***/ }),
/* 14 */
/***/ (function(module, exports) {

/* jshint browser: true */

(function () {

// The properties that we copy into a mirrored div.
// Note that some browsers, such as Firefox,
// do not concatenate properties, i.e. padding-top, bottom etc. -> padding,
// so we have to do every single property specifically.
var properties = [
  'direction',  // RTL support
  'boxSizing',
  'width',  // on Chrome and IE, exclude the scrollbar, so the mirror div wraps exactly as the textarea does
  'height',
  'overflowX',
  'overflowY',  // copy the scrollbar for IE

  'borderTopWidth',
  'borderRightWidth',
  'borderBottomWidth',
  'borderLeftWidth',
  'borderStyle',

  'paddingTop',
  'paddingRight',
  'paddingBottom',
  'paddingLeft',

  // https://developer.mozilla.org/en-US/docs/Web/CSS/font
  'fontStyle',
  'fontVariant',
  'fontWeight',
  'fontStretch',
  'fontSize',
  'fontSizeAdjust',
  'lineHeight',
  'fontFamily',

  'textAlign',
  'textTransform',
  'textIndent',
  'textDecoration',  // might not make a difference, but better be safe

  'letterSpacing',
  'wordSpacing',

  'tabSize',
  'MozTabSize'

];

var isBrowser = (typeof window !== 'undefined');
var isFirefox = (isBrowser && window.mozInnerScreenX != null);

function getCaretCoordinates(element, position, options) {
  if(!isBrowser) {
    throw new Error('textarea-caret-position#getCaretCoordinates should only be called in a browser');
  }

  var debug = options && options.debug || false;
  if (debug) {
    var el = document.querySelector('#input-textarea-caret-position-mirror-div');
    if ( el ) { el.parentNode.removeChild(el); }
  }

  // mirrored div
  var div = document.createElement('div');
  div.id = 'input-textarea-caret-position-mirror-div';
  document.body.appendChild(div);

  var style = div.style;
  var computed = window.getComputedStyle? getComputedStyle(element) : element.currentStyle;  // currentStyle for IE < 9

  // default textarea styles
  style.whiteSpace = 'pre-wrap';
  if (element.nodeName !== 'INPUT')
    style.wordWrap = 'break-word';  // only for textarea-s

  // position off-screen
  style.position = 'absolute';  // required to return coordinates properly
  if (!debug)
    style.visibility = 'hidden';  // not 'display: none' because we want rendering

  // transfer the element's properties to the div
  properties.forEach(function (prop) {
    style[prop] = computed[prop];
  });

  if (isFirefox) {
    // Firefox lies about the overflow property for textareas: https://bugzilla.mozilla.org/show_bug.cgi?id=984275
    if (element.scrollHeight > parseInt(computed.height))
      style.overflowY = 'scroll';
  } else {
    style.overflow = 'hidden';  // for Chrome to not render a scrollbar; IE keeps overflowY = 'scroll'
  }

  div.textContent = element.value.substring(0, position);
  // the second special handling for input type="text" vs textarea: spaces need to be replaced with non-breaking spaces - http://stackoverflow.com/a/13402035/1269037
  if (element.nodeName === 'INPUT')
    div.textContent = div.textContent.replace(/\s/g, '\u00a0');

  var span = document.createElement('span');
  // Wrapping must be replicated *exactly*, including when a long word gets
  // onto the next line, with whitespace at the end of the line before (#7).
  // The  *only* reliable way to do that is to copy the *entire* rest of the
  // textarea's content into the <span> created at the caret position.
  // for inputs, just '.' would be enough, but why bother?
  span.textContent = element.value.substring(position) || '.';  // || because a completely empty faux span doesn't render at all
  div.appendChild(span);

  var coordinates = {
    top: span.offsetTop + parseInt(computed['borderTopWidth']),
    left: span.offsetLeft + parseInt(computed['borderLeftWidth'])
  };

  if (debug) {
    span.style.backgroundColor = '#aaa';
  } else {
    document.body.removeChild(div);
  }

  return coordinates;
}

if (typeof module != 'undefined' && typeof module.exports != 'undefined') {
  module.exports = getCaretCoordinates;
} else if(isBrowser){
  window.getCaretCoordinates = getCaretCoordinates;
}

}());


/***/ })
/******/ ]);
//# sourceMappingURL=textcomplete.js.map