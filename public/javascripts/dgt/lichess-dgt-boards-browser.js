(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
  (function (global){
  'use strict';
  
  var objectAssign = require('object-assign');
  
  // compare and isBuffer taken from https://github.com/feross/buffer/blob/680e9e5e488f22aac27599a57dc844a6315928dd/index.js
  // original notice:
  
  /*!
   * The buffer module from node.js, for the browser.
   *
   * @author   Feross Aboukhadijeh <feross@feross.org> <http://feross.org>
   * @license  MIT
   */
  function compare(a, b) {
    if (a === b) {
      return 0;
    }
  
    var x = a.length;
    var y = b.length;
  
    for (var i = 0, len = Math.min(x, y); i < len; ++i) {
      if (a[i] !== b[i]) {
        x = a[i];
        y = b[i];
        break;
      }
    }
  
    if (x < y) {
      return -1;
    }
    if (y < x) {
      return 1;
    }
    return 0;
  }
  function isBuffer(b) {
    if (global.Buffer && typeof global.Buffer.isBuffer === 'function') {
      return global.Buffer.isBuffer(b);
    }
    return !!(b != null && b._isBuffer);
  }
  
  // based on node assert, original notice:
  // NB: The URL to the CommonJS spec is kept just for tradition.
  //     node-assert has evolved a lot since then, both in API and behavior.
  
  // http://wiki.commonjs.org/wiki/Unit_Testing/1.0
  //
  // THIS IS NOT TESTED NOR LIKELY TO WORK OUTSIDE V8!
  //
  // Originally from narwhal.js (http://narwhaljs.org)
  // Copyright (c) 2009 Thomas Robinson <280north.com>
  //
  // Permission is hereby granted, free of charge, to any person obtaining a copy
  // of this software and associated documentation files (the 'Software'), to
  // deal in the Software without restriction, including without limitation the
  // rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
  // sell copies of the Software, and to permit persons to whom the Software is
  // furnished to do so, subject to the following conditions:
  //
  // The above copyright notice and this permission notice shall be included in
  // all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  // AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  // ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
  // WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  var util = require('util/');
  var hasOwn = Object.prototype.hasOwnProperty;
  var pSlice = Array.prototype.slice;
  var functionsHaveNames = (function () {
    return function foo() {}.name === 'foo';
  }());
  function pToString (obj) {
    return Object.prototype.toString.call(obj);
  }
  function isView(arrbuf) {
    if (isBuffer(arrbuf)) {
      return false;
    }
    if (typeof global.ArrayBuffer !== 'function') {
      return false;
    }
    if (typeof ArrayBuffer.isView === 'function') {
      return ArrayBuffer.isView(arrbuf);
    }
    if (!arrbuf) {
      return false;
    }
    if (arrbuf instanceof DataView) {
      return true;
    }
    if (arrbuf.buffer && arrbuf.buffer instanceof ArrayBuffer) {
      return true;
    }
    return false;
  }
  // 1. The assert module provides functions that throw
  // AssertionError's when particular conditions are not met. The
  // assert module must conform to the following interface.
  
  var assert = module.exports = ok;
  
  // 2. The AssertionError is defined in assert.
  // new assert.AssertionError({ message: message,
  //                             actual: actual,
  //                             expected: expected })
  
  var regex = /\s*function\s+([^\(\s]*)\s*/;
  // based on https://github.com/ljharb/function.prototype.name/blob/adeeeec8bfcc6068b187d7d9fb3d5bb1d3a30899/implementation.js
  function getName(func) {
    if (!util.isFunction(func)) {
      return;
    }
    if (functionsHaveNames) {
      return func.name;
    }
    var str = func.toString();
    var match = str.match(regex);
    return match && match[1];
  }
  assert.AssertionError = function AssertionError(options) {
    this.name = 'AssertionError';
    this.actual = options.actual;
    this.expected = options.expected;
    this.operator = options.operator;
    if (options.message) {
      this.message = options.message;
      this.generatedMessage = false;
    } else {
      this.message = getMessage(this);
      this.generatedMessage = true;
    }
    var stackStartFunction = options.stackStartFunction || fail;
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, stackStartFunction);
    } else {
      // non v8 browsers so we can have a stacktrace
      var err = new Error();
      if (err.stack) {
        var out = err.stack;
  
        // try to strip useless frames
        var fn_name = getName(stackStartFunction);
        var idx = out.indexOf('\n' + fn_name);
        if (idx >= 0) {
          // once we have located the function frame
          // we need to strip out everything before it (and its line)
          var next_line = out.indexOf('\n', idx + 1);
          out = out.substring(next_line + 1);
        }
  
        this.stack = out;
      }
    }
  };
  
  // assert.AssertionError instanceof Error
  util.inherits(assert.AssertionError, Error);
  
  function truncate(s, n) {
    if (typeof s === 'string') {
      return s.length < n ? s : s.slice(0, n);
    } else {
      return s;
    }
  }
  function inspect(something) {
    if (functionsHaveNames || !util.isFunction(something)) {
      return util.inspect(something);
    }
    var rawname = getName(something);
    var name = rawname ? ': ' + rawname : '';
    return '[Function' +  name + ']';
  }
  function getMessage(self) {
    return truncate(inspect(self.actual), 128) + ' ' +
           self.operator + ' ' +
           truncate(inspect(self.expected), 128);
  }
  
  // At present only the three keys mentioned above are used and
  // understood by the spec. Implementations or sub modules can pass
  // other keys to the AssertionError's constructor - they will be
  // ignored.
  
  // 3. All of the following functions must throw an AssertionError
  // when a corresponding condition is not met, with a message that
  // may be undefined if not provided.  All assertion methods provide
  // both the actual and expected values to the assertion error for
  // display purposes.
  
  function fail(actual, expected, message, operator, stackStartFunction) {
    throw new assert.AssertionError({
      message: message,
      actual: actual,
      expected: expected,
      operator: operator,
      stackStartFunction: stackStartFunction
    });
  }
  
  // EXTENSION! allows for well behaved errors defined elsewhere.
  assert.fail = fail;
  
  // 4. Pure assertion tests whether a value is truthy, as determined
  // by !!guard.
  // assert.ok(guard, message_opt);
  // This statement is equivalent to assert.equal(true, !!guard,
  // message_opt);. To test strictly for the value true, use
  // assert.strictEqual(true, guard, message_opt);.
  
  function ok(value, message) {
    if (!value) fail(value, true, message, '==', assert.ok);
  }
  assert.ok = ok;
  
  // 5. The equality assertion tests shallow, coercive equality with
  // ==.
  // assert.equal(actual, expected, message_opt);
  
  assert.equal = function equal(actual, expected, message) {
    if (actual != expected) fail(actual, expected, message, '==', assert.equal);
  };
  
  // 6. The non-equality assertion tests for whether two objects are not equal
  // with != assert.notEqual(actual, expected, message_opt);
  
  assert.notEqual = function notEqual(actual, expected, message) {
    if (actual == expected) {
      fail(actual, expected, message, '!=', assert.notEqual);
    }
  };
  
  // 7. The equivalence assertion tests a deep equality relation.
  // assert.deepEqual(actual, expected, message_opt);
  
  assert.deepEqual = function deepEqual(actual, expected, message) {
    if (!_deepEqual(actual, expected, false)) {
      fail(actual, expected, message, 'deepEqual', assert.deepEqual);
    }
  };
  
  assert.deepStrictEqual = function deepStrictEqual(actual, expected, message) {
    if (!_deepEqual(actual, expected, true)) {
      fail(actual, expected, message, 'deepStrictEqual', assert.deepStrictEqual);
    }
  };
  
  function _deepEqual(actual, expected, strict, memos) {
    // 7.1. All identical values are equivalent, as determined by ===.
    if (actual === expected) {
      return true;
    } else if (isBuffer(actual) && isBuffer(expected)) {
      return compare(actual, expected) === 0;
  
    // 7.2. If the expected value is a Date object, the actual value is
    // equivalent if it is also a Date object that refers to the same time.
    } else if (util.isDate(actual) && util.isDate(expected)) {
      return actual.getTime() === expected.getTime();
  
    // 7.3 If the expected value is a RegExp object, the actual value is
    // equivalent if it is also a RegExp object with the same source and
    // properties (`global`, `multiline`, `lastIndex`, `ignoreCase`).
    } else if (util.isRegExp(actual) && util.isRegExp(expected)) {
      return actual.source === expected.source &&
             actual.global === expected.global &&
             actual.multiline === expected.multiline &&
             actual.lastIndex === expected.lastIndex &&
             actual.ignoreCase === expected.ignoreCase;
  
    // 7.4. Other pairs that do not both pass typeof value == 'object',
    // equivalence is determined by ==.
    } else if ((actual === null || typeof actual !== 'object') &&
               (expected === null || typeof expected !== 'object')) {
      return strict ? actual === expected : actual == expected;
  
    // If both values are instances of typed arrays, wrap their underlying
    // ArrayBuffers in a Buffer each to increase performance
    // This optimization requires the arrays to have the same type as checked by
    // Object.prototype.toString (aka pToString). Never perform binary
    // comparisons for Float*Arrays, though, since e.g. +0 === -0 but their
    // bit patterns are not identical.
    } else if (isView(actual) && isView(expected) &&
               pToString(actual) === pToString(expected) &&
               !(actual instanceof Float32Array ||
                 actual instanceof Float64Array)) {
      return compare(new Uint8Array(actual.buffer),
                     new Uint8Array(expected.buffer)) === 0;
  
    // 7.5 For all other Object pairs, including Array objects, equivalence is
    // determined by having the same number of owned properties (as verified
    // with Object.prototype.hasOwnProperty.call), the same set of keys
    // (although not necessarily the same order), equivalent values for every
    // corresponding key, and an identical 'prototype' property. Note: this
    // accounts for both named and indexed properties on Arrays.
    } else if (isBuffer(actual) !== isBuffer(expected)) {
      return false;
    } else {
      memos = memos || {actual: [], expected: []};
  
      var actualIndex = memos.actual.indexOf(actual);
      if (actualIndex !== -1) {
        if (actualIndex === memos.expected.indexOf(expected)) {
          return true;
        }
      }
  
      memos.actual.push(actual);
      memos.expected.push(expected);
  
      return objEquiv(actual, expected, strict, memos);
    }
  }
  
  function isArguments(object) {
    return Object.prototype.toString.call(object) == '[object Arguments]';
  }
  
  function objEquiv(a, b, strict, actualVisitedObjects) {
    if (a === null || a === undefined || b === null || b === undefined)
      return false;
    // if one is a primitive, the other must be same
    if (util.isPrimitive(a) || util.isPrimitive(b))
      return a === b;
    if (strict && Object.getPrototypeOf(a) !== Object.getPrototypeOf(b))
      return false;
    var aIsArgs = isArguments(a);
    var bIsArgs = isArguments(b);
    if ((aIsArgs && !bIsArgs) || (!aIsArgs && bIsArgs))
      return false;
    if (aIsArgs) {
      a = pSlice.call(a);
      b = pSlice.call(b);
      return _deepEqual(a, b, strict);
    }
    var ka = objectKeys(a);
    var kb = objectKeys(b);
    var key, i;
    // having the same number of owned properties (keys incorporates
    // hasOwnProperty)
    if (ka.length !== kb.length)
      return false;
    //the same set of keys (although not necessarily the same order),
    ka.sort();
    kb.sort();
    //~~~cheap key test
    for (i = ka.length - 1; i >= 0; i--) {
      if (ka[i] !== kb[i])
        return false;
    }
    //equivalent values for every corresponding key, and
    //~~~possibly expensive deep test
    for (i = ka.length - 1; i >= 0; i--) {
      key = ka[i];
      if (!_deepEqual(a[key], b[key], strict, actualVisitedObjects))
        return false;
    }
    return true;
  }
  
  // 8. The non-equivalence assertion tests for any deep inequality.
  // assert.notDeepEqual(actual, expected, message_opt);
  
  assert.notDeepEqual = function notDeepEqual(actual, expected, message) {
    if (_deepEqual(actual, expected, false)) {
      fail(actual, expected, message, 'notDeepEqual', assert.notDeepEqual);
    }
  };
  
  assert.notDeepStrictEqual = notDeepStrictEqual;
  function notDeepStrictEqual(actual, expected, message) {
    if (_deepEqual(actual, expected, true)) {
      fail(actual, expected, message, 'notDeepStrictEqual', notDeepStrictEqual);
    }
  }
  
  
  // 9. The strict equality assertion tests strict equality, as determined by ===.
  // assert.strictEqual(actual, expected, message_opt);
  
  assert.strictEqual = function strictEqual(actual, expected, message) {
    if (actual !== expected) {
      fail(actual, expected, message, '===', assert.strictEqual);
    }
  };
  
  // 10. The strict non-equality assertion tests for strict inequality, as
  // determined by !==.  assert.notStrictEqual(actual, expected, message_opt);
  
  assert.notStrictEqual = function notStrictEqual(actual, expected, message) {
    if (actual === expected) {
      fail(actual, expected, message, '!==', assert.notStrictEqual);
    }
  };
  
  function expectedException(actual, expected) {
    if (!actual || !expected) {
      return false;
    }
  
    if (Object.prototype.toString.call(expected) == '[object RegExp]') {
      return expected.test(actual);
    }
  
    try {
      if (actual instanceof expected) {
        return true;
      }
    } catch (e) {
      // Ignore.  The instanceof check doesn't work for arrow functions.
    }
  
    if (Error.isPrototypeOf(expected)) {
      return false;
    }
  
    return expected.call({}, actual) === true;
  }
  
  function _tryBlock(block) {
    var error;
    try {
      block();
    } catch (e) {
      error = e;
    }
    return error;
  }
  
  function _throws(shouldThrow, block, expected, message) {
    var actual;
  
    if (typeof block !== 'function') {
      throw new TypeError('"block" argument must be a function');
    }
  
    if (typeof expected === 'string') {
      message = expected;
      expected = null;
    }
  
    actual = _tryBlock(block);
  
    message = (expected && expected.name ? ' (' + expected.name + ').' : '.') +
              (message ? ' ' + message : '.');
  
    if (shouldThrow && !actual) {
      fail(actual, expected, 'Missing expected exception' + message);
    }
  
    var userProvidedMessage = typeof message === 'string';
    var isUnwantedException = !shouldThrow && util.isError(actual);
    var isUnexpectedException = !shouldThrow && actual && !expected;
  
    if ((isUnwantedException &&
        userProvidedMessage &&
        expectedException(actual, expected)) ||
        isUnexpectedException) {
      fail(actual, expected, 'Got unwanted exception' + message);
    }
  
    if ((shouldThrow && actual && expected &&
        !expectedException(actual, expected)) || (!shouldThrow && actual)) {
      throw actual;
    }
  }
  
  // 11. Expected to throw an error:
  // assert.throws(block, Error_opt, message_opt);
  
  assert.throws = function(block, /*optional*/error, /*optional*/message) {
    _throws(true, block, error, message);
  };
  
  // EXTENSION! This is annoying to write outside this module.
  assert.doesNotThrow = function(block, /*optional*/error, /*optional*/message) {
    _throws(false, block, error, message);
  };
  
  assert.ifError = function(err) { if (err) throw err; };
  
  // Expose a strict only variant of assert
  function strict(value, message) {
    if (!value) fail(value, true, message, '==', strict);
  }
  assert.strict = objectAssign(strict, assert, {
    equal: assert.strictEqual,
    deepEqual: assert.deepStrictEqual,
    notEqual: assert.notStrictEqual,
    notDeepEqual: assert.notDeepStrictEqual
  });
  assert.strict.strict = assert.strict;
  
  var objectKeys = Object.keys || function (obj) {
    var keys = [];
    for (var key in obj) {
      if (hasOwn.call(obj, key)) keys.push(key);
    }
    return keys;
  };
  
  }).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{"object-assign":18,"util/":4}],2:[function(require,module,exports){
  if (typeof Object.create === 'function') {
    // implementation from standard node.js 'util' module
    module.exports = function inherits(ctor, superCtor) {
      ctor.super_ = superCtor
      ctor.prototype = Object.create(superCtor.prototype, {
        constructor: {
          value: ctor,
          enumerable: false,
          writable: true,
          configurable: true
        }
      });
    };
  } else {
    // old school shim for old browsers
    module.exports = function inherits(ctor, superCtor) {
      ctor.super_ = superCtor
      var TempCtor = function () {}
      TempCtor.prototype = superCtor.prototype
      ctor.prototype = new TempCtor()
      ctor.prototype.constructor = ctor
    }
  }
  
  },{}],3:[function(require,module,exports){
  module.exports = function isBuffer(arg) {
    return arg && typeof arg === 'object'
      && typeof arg.copy === 'function'
      && typeof arg.fill === 'function'
      && typeof arg.readUInt8 === 'function';
  }
  },{}],4:[function(require,module,exports){
  (function (process,global){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  var formatRegExp = /%[sdj%]/g;
  exports.format = function(f) {
    if (!isString(f)) {
      var objects = [];
      for (var i = 0; i < arguments.length; i++) {
        objects.push(inspect(arguments[i]));
      }
      return objects.join(' ');
    }
  
    var i = 1;
    var args = arguments;
    var len = args.length;
    var str = String(f).replace(formatRegExp, function(x) {
      if (x === '%%') return '%';
      if (i >= len) return x;
      switch (x) {
        case '%s': return String(args[i++]);
        case '%d': return Number(args[i++]);
        case '%j':
          try {
            return JSON.stringify(args[i++]);
          } catch (_) {
            return '[Circular]';
          }
        default:
          return x;
      }
    });
    for (var x = args[i]; i < len; x = args[++i]) {
      if (isNull(x) || !isObject(x)) {
        str += ' ' + x;
      } else {
        str += ' ' + inspect(x);
      }
    }
    return str;
  };
  
  
  // Mark that a method should not be used.
  // Returns a modified function which warns once by default.
  // If --no-deprecation is set, then it is a no-op.
  exports.deprecate = function(fn, msg) {
    // Allow for deprecating things in the process of starting up.
    if (isUndefined(global.process)) {
      return function() {
        return exports.deprecate(fn, msg).apply(this, arguments);
      };
    }
  
    if (process.noDeprecation === true) {
      return fn;
    }
  
    var warned = false;
    function deprecated() {
      if (!warned) {
        if (process.throwDeprecation) {
          throw new Error(msg);
        } else if (process.traceDeprecation) {
          console.trace(msg);
        } else {
          console.error(msg);
        }
        warned = true;
      }
      return fn.apply(this, arguments);
    }
  
    return deprecated;
  };
  
  
  var debugs = {};
  var debugEnviron;
  exports.debuglog = function(set) {
    if (isUndefined(debugEnviron))
      debugEnviron = process.env.NODE_DEBUG || '';
    set = set.toUpperCase();
    if (!debugs[set]) {
      if (new RegExp('\\b' + set + '\\b', 'i').test(debugEnviron)) {
        var pid = process.pid;
        debugs[set] = function() {
          var msg = exports.format.apply(exports, arguments);
          console.error('%s %d: %s', set, pid, msg);
        };
      } else {
        debugs[set] = function() {};
      }
    }
    return debugs[set];
  };
  
  
  /**
   * Echos the value of a value. Trys to print the value out
   * in the best way possible given the different types.
   *
   * @param {Object} obj The object to print out.
   * @param {Object} opts Optional options object that alters the output.
   */
  /* legacy: obj, showHidden, depth, colors*/
  function inspect(obj, opts) {
    // default options
    var ctx = {
      seen: [],
      stylize: stylizeNoColor
    };
    // legacy...
    if (arguments.length >= 3) ctx.depth = arguments[2];
    if (arguments.length >= 4) ctx.colors = arguments[3];
    if (isBoolean(opts)) {
      // legacy...
      ctx.showHidden = opts;
    } else if (opts) {
      // got an "options" object
      exports._extend(ctx, opts);
    }
    // set default options
    if (isUndefined(ctx.showHidden)) ctx.showHidden = false;
    if (isUndefined(ctx.depth)) ctx.depth = 2;
    if (isUndefined(ctx.colors)) ctx.colors = false;
    if (isUndefined(ctx.customInspect)) ctx.customInspect = true;
    if (ctx.colors) ctx.stylize = stylizeWithColor;
    return formatValue(ctx, obj, ctx.depth);
  }
  exports.inspect = inspect;
  
  
  // http://en.wikipedia.org/wiki/ANSI_escape_code#graphics
  inspect.colors = {
    'bold' : [1, 22],
    'italic' : [3, 23],
    'underline' : [4, 24],
    'inverse' : [7, 27],
    'white' : [37, 39],
    'grey' : [90, 39],
    'black' : [30, 39],
    'blue' : [34, 39],
    'cyan' : [36, 39],
    'green' : [32, 39],
    'magenta' : [35, 39],
    'red' : [31, 39],
    'yellow' : [33, 39]
  };
  
  // Don't use 'blue' not visible on cmd.exe
  inspect.styles = {
    'special': 'cyan',
    'number': 'yellow',
    'boolean': 'yellow',
    'undefined': 'grey',
    'null': 'bold',
    'string': 'green',
    'date': 'magenta',
    // "name": intentionally not styling
    'regexp': 'red'
  };
  
  
  function stylizeWithColor(str, styleType) {
    var style = inspect.styles[styleType];
  
    if (style) {
      return '\u001b[' + inspect.colors[style][0] + 'm' + str +
             '\u001b[' + inspect.colors[style][1] + 'm';
    } else {
      return str;
    }
  }
  
  
  function stylizeNoColor(str, styleType) {
    return str;
  }
  
  
  function arrayToHash(array) {
    var hash = {};
  
    array.forEach(function(val, idx) {
      hash[val] = true;
    });
  
    return hash;
  }
  
  
  function formatValue(ctx, value, recurseTimes) {
    // Provide a hook for user-specified inspect functions.
    // Check that value is an object with an inspect function on it
    if (ctx.customInspect &&
        value &&
        isFunction(value.inspect) &&
        // Filter out the util module, it's inspect function is special
        value.inspect !== exports.inspect &&
        // Also filter out any prototype objects using the circular check.
        !(value.constructor && value.constructor.prototype === value)) {
      var ret = value.inspect(recurseTimes, ctx);
      if (!isString(ret)) {
        ret = formatValue(ctx, ret, recurseTimes);
      }
      return ret;
    }
  
    // Primitive types cannot have properties
    var primitive = formatPrimitive(ctx, value);
    if (primitive) {
      return primitive;
    }
  
    // Look up the keys of the object.
    var keys = Object.keys(value);
    var visibleKeys = arrayToHash(keys);
  
    if (ctx.showHidden) {
      keys = Object.getOwnPropertyNames(value);
    }
  
    // IE doesn't make error fields non-enumerable
    // http://msdn.microsoft.com/en-us/library/ie/dww52sbt(v=vs.94).aspx
    if (isError(value)
        && (keys.indexOf('message') >= 0 || keys.indexOf('description') >= 0)) {
      return formatError(value);
    }
  
    // Some type of object without properties can be shortcutted.
    if (keys.length === 0) {
      if (isFunction(value)) {
        var name = value.name ? ': ' + value.name : '';
        return ctx.stylize('[Function' + name + ']', 'special');
      }
      if (isRegExp(value)) {
        return ctx.stylize(RegExp.prototype.toString.call(value), 'regexp');
      }
      if (isDate(value)) {
        return ctx.stylize(Date.prototype.toString.call(value), 'date');
      }
      if (isError(value)) {
        return formatError(value);
      }
    }
  
    var base = '', array = false, braces = ['{', '}'];
  
    // Make Array say that they are Array
    if (isArray(value)) {
      array = true;
      braces = ['[', ']'];
    }
  
    // Make functions say that they are functions
    if (isFunction(value)) {
      var n = value.name ? ': ' + value.name : '';
      base = ' [Function' + n + ']';
    }
  
    // Make RegExps say that they are RegExps
    if (isRegExp(value)) {
      base = ' ' + RegExp.prototype.toString.call(value);
    }
  
    // Make dates with properties first say the date
    if (isDate(value)) {
      base = ' ' + Date.prototype.toUTCString.call(value);
    }
  
    // Make error with message first say the error
    if (isError(value)) {
      base = ' ' + formatError(value);
    }
  
    if (keys.length === 0 && (!array || value.length == 0)) {
      return braces[0] + base + braces[1];
    }
  
    if (recurseTimes < 0) {
      if (isRegExp(value)) {
        return ctx.stylize(RegExp.prototype.toString.call(value), 'regexp');
      } else {
        return ctx.stylize('[Object]', 'special');
      }
    }
  
    ctx.seen.push(value);
  
    var output;
    if (array) {
      output = formatArray(ctx, value, recurseTimes, visibleKeys, keys);
    } else {
      output = keys.map(function(key) {
        return formatProperty(ctx, value, recurseTimes, visibleKeys, key, array);
      });
    }
  
    ctx.seen.pop();
  
    return reduceToSingleString(output, base, braces);
  }
  
  
  function formatPrimitive(ctx, value) {
    if (isUndefined(value))
      return ctx.stylize('undefined', 'undefined');
    if (isString(value)) {
      var simple = '\'' + JSON.stringify(value).replace(/^"|"$/g, '')
                                               .replace(/'/g, "\\'")
                                               .replace(/\\"/g, '"') + '\'';
      return ctx.stylize(simple, 'string');
    }
    if (isNumber(value))
      return ctx.stylize('' + value, 'number');
    if (isBoolean(value))
      return ctx.stylize('' + value, 'boolean');
    // For some reason typeof null is "object", so special case here.
    if (isNull(value))
      return ctx.stylize('null', 'null');
  }
  
  
  function formatError(value) {
    return '[' + Error.prototype.toString.call(value) + ']';
  }
  
  
  function formatArray(ctx, value, recurseTimes, visibleKeys, keys) {
    var output = [];
    for (var i = 0, l = value.length; i < l; ++i) {
      if (hasOwnProperty(value, String(i))) {
        output.push(formatProperty(ctx, value, recurseTimes, visibleKeys,
            String(i), true));
      } else {
        output.push('');
      }
    }
    keys.forEach(function(key) {
      if (!key.match(/^\d+$/)) {
        output.push(formatProperty(ctx, value, recurseTimes, visibleKeys,
            key, true));
      }
    });
    return output;
  }
  
  
  function formatProperty(ctx, value, recurseTimes, visibleKeys, key, array) {
    var name, str, desc;
    desc = Object.getOwnPropertyDescriptor(value, key) || { value: value[key] };
    if (desc.get) {
      if (desc.set) {
        str = ctx.stylize('[Getter/Setter]', 'special');
      } else {
        str = ctx.stylize('[Getter]', 'special');
      }
    } else {
      if (desc.set) {
        str = ctx.stylize('[Setter]', 'special');
      }
    }
    if (!hasOwnProperty(visibleKeys, key)) {
      name = '[' + key + ']';
    }
    if (!str) {
      if (ctx.seen.indexOf(desc.value) < 0) {
        if (isNull(recurseTimes)) {
          str = formatValue(ctx, desc.value, null);
        } else {
          str = formatValue(ctx, desc.value, recurseTimes - 1);
        }
        if (str.indexOf('\n') > -1) {
          if (array) {
            str = str.split('\n').map(function(line) {
              return '  ' + line;
            }).join('\n').substr(2);
          } else {
            str = '\n' + str.split('\n').map(function(line) {
              return '   ' + line;
            }).join('\n');
          }
        }
      } else {
        str = ctx.stylize('[Circular]', 'special');
      }
    }
    if (isUndefined(name)) {
      if (array && key.match(/^\d+$/)) {
        return str;
      }
      name = JSON.stringify('' + key);
      if (name.match(/^"([a-zA-Z_][a-zA-Z_0-9]*)"$/)) {
        name = name.substr(1, name.length - 2);
        name = ctx.stylize(name, 'name');
      } else {
        name = name.replace(/'/g, "\\'")
                   .replace(/\\"/g, '"')
                   .replace(/(^"|"$)/g, "'");
        name = ctx.stylize(name, 'string');
      }
    }
  
    return name + ': ' + str;
  }
  
  
  function reduceToSingleString(output, base, braces) {
    var numLinesEst = 0;
    var length = output.reduce(function(prev, cur) {
      numLinesEst++;
      if (cur.indexOf('\n') >= 0) numLinesEst++;
      return prev + cur.replace(/\u001b\[\d\d?m/g, '').length + 1;
    }, 0);
  
    if (length > 60) {
      return braces[0] +
             (base === '' ? '' : base + '\n ') +
             ' ' +
             output.join(',\n  ') +
             ' ' +
             braces[1];
    }
  
    return braces[0] + base + ' ' + output.join(', ') + ' ' + braces[1];
  }
  
  
  // NOTE: These type checking functions intentionally don't use `instanceof`
  // because it is fragile and can be easily faked with `Object.create()`.
  function isArray(ar) {
    return Array.isArray(ar);
  }
  exports.isArray = isArray;
  
  function isBoolean(arg) {
    return typeof arg === 'boolean';
  }
  exports.isBoolean = isBoolean;
  
  function isNull(arg) {
    return arg === null;
  }
  exports.isNull = isNull;
  
  function isNullOrUndefined(arg) {
    return arg == null;
  }
  exports.isNullOrUndefined = isNullOrUndefined;
  
  function isNumber(arg) {
    return typeof arg === 'number';
  }
  exports.isNumber = isNumber;
  
  function isString(arg) {
    return typeof arg === 'string';
  }
  exports.isString = isString;
  
  function isSymbol(arg) {
    return typeof arg === 'symbol';
  }
  exports.isSymbol = isSymbol;
  
  function isUndefined(arg) {
    return arg === void 0;
  }
  exports.isUndefined = isUndefined;
  
  function isRegExp(re) {
    return isObject(re) && objectToString(re) === '[object RegExp]';
  }
  exports.isRegExp = isRegExp;
  
  function isObject(arg) {
    return typeof arg === 'object' && arg !== null;
  }
  exports.isObject = isObject;
  
  function isDate(d) {
    return isObject(d) && objectToString(d) === '[object Date]';
  }
  exports.isDate = isDate;
  
  function isError(e) {
    return isObject(e) &&
        (objectToString(e) === '[object Error]' || e instanceof Error);
  }
  exports.isError = isError;
  
  function isFunction(arg) {
    return typeof arg === 'function';
  }
  exports.isFunction = isFunction;
  
  function isPrimitive(arg) {
    return arg === null ||
           typeof arg === 'boolean' ||
           typeof arg === 'number' ||
           typeof arg === 'string' ||
           typeof arg === 'symbol' ||  // ES6 symbol
           typeof arg === 'undefined';
  }
  exports.isPrimitive = isPrimitive;
  
  exports.isBuffer = require('./support/isBuffer');
  
  function objectToString(o) {
    return Object.prototype.toString.call(o);
  }
  
  
  function pad(n) {
    return n < 10 ? '0' + n.toString(10) : n.toString(10);
  }
  
  
  var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep',
                'Oct', 'Nov', 'Dec'];
  
  // 26 Feb 16:19:34
  function timestamp() {
    var d = new Date();
    var time = [pad(d.getHours()),
                pad(d.getMinutes()),
                pad(d.getSeconds())].join(':');
    return [d.getDate(), months[d.getMonth()], time].join(' ');
  }
  
  
  // log is just a thin wrapper to console.log that prepends a timestamp
  exports.log = function() {
    console.log('%s - %s', timestamp(), exports.format.apply(exports, arguments));
  };
  
  
  /**
   * Inherit the prototype methods from one constructor into another.
   *
   * The Function.prototype.inherits from lang.js rewritten as a standalone
   * function (not on Function.prototype). NOTE: If this file is to be loaded
   * during bootstrapping this function needs to be rewritten using some native
   * functions as prototype setup using normal JavaScript does not work as
   * expected during bootstrapping (see mirror.js in r114903).
   *
   * @param {function} ctor Constructor function which needs to inherit the
   *     prototype.
   * @param {function} superCtor Constructor function to inherit prototype from.
   */
  exports.inherits = require('inherits');
  
  exports._extend = function(origin, add) {
    // Don't do anything if add isn't an object
    if (!add || !isObject(add)) return origin;
  
    var keys = Object.keys(add);
    var i = keys.length;
    while (i--) {
      origin[keys[i]] = add[keys[i]];
    }
    return origin;
  };
  
  function hasOwnProperty(obj, prop) {
    return Object.prototype.hasOwnProperty.call(obj, prop);
  }
  
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{"./support/isBuffer":3,"_process":32,"inherits":2}],5:[function(require,module,exports){
  'use strict'
  
  exports.byteLength = byteLength
  exports.toByteArray = toByteArray
  exports.fromByteArray = fromByteArray
  
  var lookup = []
  var revLookup = []
  var Arr = typeof Uint8Array !== 'undefined' ? Uint8Array : Array
  
  var code = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
  for (var i = 0, len = code.length; i < len; ++i) {
    lookup[i] = code[i]
    revLookup[code.charCodeAt(i)] = i
  }
  
  // Support decoding URL-safe base64 strings, as Node.js does.
  // See: https://en.wikipedia.org/wiki/Base64#URL_applications
  revLookup['-'.charCodeAt(0)] = 62
  revLookup['_'.charCodeAt(0)] = 63
  
  function getLens (b64) {
    var len = b64.length
  
    if (len % 4 > 0) {
      throw new Error('Invalid string. Length must be a multiple of 4')
    }
  
    // Trim off extra bytes after placeholder bytes are found
    // See: https://github.com/beatgammit/base64-js/issues/42
    var validLen = b64.indexOf('=')
    if (validLen === -1) validLen = len
  
    var placeHoldersLen = validLen === len
      ? 0
      : 4 - (validLen % 4)
  
    return [validLen, placeHoldersLen]
  }
  
  // base64 is 4/3 + up to two characters of the original data
  function byteLength (b64) {
    var lens = getLens(b64)
    var validLen = lens[0]
    var placeHoldersLen = lens[1]
    return ((validLen + placeHoldersLen) * 3 / 4) - placeHoldersLen
  }
  
  function _byteLength (b64, validLen, placeHoldersLen) {
    return ((validLen + placeHoldersLen) * 3 / 4) - placeHoldersLen
  }
  
  function toByteArray (b64) {
    var tmp
    var lens = getLens(b64)
    var validLen = lens[0]
    var placeHoldersLen = lens[1]
  
    var arr = new Arr(_byteLength(b64, validLen, placeHoldersLen))
  
    var curByte = 0
  
    // if there are placeholders, only get up to the last complete 4 chars
    var len = placeHoldersLen > 0
      ? validLen - 4
      : validLen
  
    var i
    for (i = 0; i < len; i += 4) {
      tmp =
        (revLookup[b64.charCodeAt(i)] << 18) |
        (revLookup[b64.charCodeAt(i + 1)] << 12) |
        (revLookup[b64.charCodeAt(i + 2)] << 6) |
        revLookup[b64.charCodeAt(i + 3)]
      arr[curByte++] = (tmp >> 16) & 0xFF
      arr[curByte++] = (tmp >> 8) & 0xFF
      arr[curByte++] = tmp & 0xFF
    }
  
    if (placeHoldersLen === 2) {
      tmp =
        (revLookup[b64.charCodeAt(i)] << 2) |
        (revLookup[b64.charCodeAt(i + 1)] >> 4)
      arr[curByte++] = tmp & 0xFF
    }
  
    if (placeHoldersLen === 1) {
      tmp =
        (revLookup[b64.charCodeAt(i)] << 10) |
        (revLookup[b64.charCodeAt(i + 1)] << 4) |
        (revLookup[b64.charCodeAt(i + 2)] >> 2)
      arr[curByte++] = (tmp >> 8) & 0xFF
      arr[curByte++] = tmp & 0xFF
    }
  
    return arr
  }
  
  function tripletToBase64 (num) {
    return lookup[num >> 18 & 0x3F] +
      lookup[num >> 12 & 0x3F] +
      lookup[num >> 6 & 0x3F] +
      lookup[num & 0x3F]
  }
  
  function encodeChunk (uint8, start, end) {
    var tmp
    var output = []
    for (var i = start; i < end; i += 3) {
      tmp =
        ((uint8[i] << 16) & 0xFF0000) +
        ((uint8[i + 1] << 8) & 0xFF00) +
        (uint8[i + 2] & 0xFF)
      output.push(tripletToBase64(tmp))
    }
    return output.join('')
  }
  
  function fromByteArray (uint8) {
    var tmp
    var len = uint8.length
    var extraBytes = len % 3 // if we have 1 byte left, pad 2 bytes
    var parts = []
    var maxChunkLength = 16383 // must be multiple of 3
  
    // go through the array every three bytes, we'll deal with trailing stuff later
    for (var i = 0, len2 = len - extraBytes; i < len2; i += maxChunkLength) {
      parts.push(encodeChunk(
        uint8, i, (i + maxChunkLength) > len2 ? len2 : (i + maxChunkLength)
      ))
    }
  
    // pad the end with zeros, but make sure to not forget the extra bytes
    if (extraBytes === 1) {
      tmp = uint8[len - 1]
      parts.push(
        lookup[tmp >> 2] +
        lookup[(tmp << 4) & 0x3F] +
        '=='
      )
    } else if (extraBytes === 2) {
      tmp = (uint8[len - 2] << 8) + uint8[len - 1]
      parts.push(
        lookup[tmp >> 10] +
        lookup[(tmp >> 4) & 0x3F] +
        lookup[(tmp << 2) & 0x3F] +
        '='
      )
    }
  
    return parts.join('')
  }
  
  },{}],6:[function(require,module,exports){
  
  },{}],7:[function(require,module,exports){
  (function (process,Buffer){
  'use strict';
  /* eslint camelcase: "off" */
  
  var assert = require('assert');
  
  var Zstream = require('pako/lib/zlib/zstream');
  var zlib_deflate = require('pako/lib/zlib/deflate.js');
  var zlib_inflate = require('pako/lib/zlib/inflate.js');
  var constants = require('pako/lib/zlib/constants');
  
  for (var key in constants) {
    exports[key] = constants[key];
  }
  
  // zlib modes
  exports.NONE = 0;
  exports.DEFLATE = 1;
  exports.INFLATE = 2;
  exports.GZIP = 3;
  exports.GUNZIP = 4;
  exports.DEFLATERAW = 5;
  exports.INFLATERAW = 6;
  exports.UNZIP = 7;
  
  var GZIP_HEADER_ID1 = 0x1f;
  var GZIP_HEADER_ID2 = 0x8b;
  
  /**
   * Emulate Node's zlib C++ layer for use by the JS layer in index.js
   */
  function Zlib(mode) {
    if (typeof mode !== 'number' || mode < exports.DEFLATE || mode > exports.UNZIP) {
      throw new TypeError('Bad argument');
    }
  
    this.dictionary = null;
    this.err = 0;
    this.flush = 0;
    this.init_done = false;
    this.level = 0;
    this.memLevel = 0;
    this.mode = mode;
    this.strategy = 0;
    this.windowBits = 0;
    this.write_in_progress = false;
    this.pending_close = false;
    this.gzip_id_bytes_read = 0;
  }
  
  Zlib.prototype.close = function () {
    if (this.write_in_progress) {
      this.pending_close = true;
      return;
    }
  
    this.pending_close = false;
  
    assert(this.init_done, 'close before init');
    assert(this.mode <= exports.UNZIP);
  
    if (this.mode === exports.DEFLATE || this.mode === exports.GZIP || this.mode === exports.DEFLATERAW) {
      zlib_deflate.deflateEnd(this.strm);
    } else if (this.mode === exports.INFLATE || this.mode === exports.GUNZIP || this.mode === exports.INFLATERAW || this.mode === exports.UNZIP) {
      zlib_inflate.inflateEnd(this.strm);
    }
  
    this.mode = exports.NONE;
  
    this.dictionary = null;
  };
  
  Zlib.prototype.write = function (flush, input, in_off, in_len, out, out_off, out_len) {
    return this._write(true, flush, input, in_off, in_len, out, out_off, out_len);
  };
  
  Zlib.prototype.writeSync = function (flush, input, in_off, in_len, out, out_off, out_len) {
    return this._write(false, flush, input, in_off, in_len, out, out_off, out_len);
  };
  
  Zlib.prototype._write = function (async, flush, input, in_off, in_len, out, out_off, out_len) {
    assert.equal(arguments.length, 8);
  
    assert(this.init_done, 'write before init');
    assert(this.mode !== exports.NONE, 'already finalized');
    assert.equal(false, this.write_in_progress, 'write already in progress');
    assert.equal(false, this.pending_close, 'close is pending');
  
    this.write_in_progress = true;
  
    assert.equal(false, flush === undefined, 'must provide flush value');
  
    this.write_in_progress = true;
  
    if (flush !== exports.Z_NO_FLUSH && flush !== exports.Z_PARTIAL_FLUSH && flush !== exports.Z_SYNC_FLUSH && flush !== exports.Z_FULL_FLUSH && flush !== exports.Z_FINISH && flush !== exports.Z_BLOCK) {
      throw new Error('Invalid flush value');
    }
  
    if (input == null) {
      input = Buffer.alloc(0);
      in_len = 0;
      in_off = 0;
    }
  
    this.strm.avail_in = in_len;
    this.strm.input = input;
    this.strm.next_in = in_off;
    this.strm.avail_out = out_len;
    this.strm.output = out;
    this.strm.next_out = out_off;
    this.flush = flush;
  
    if (!async) {
      // sync version
      this._process();
  
      if (this._checkError()) {
        return this._afterSync();
      }
      return;
    }
  
    // async version
    var self = this;
    process.nextTick(function () {
      self._process();
      self._after();
    });
  
    return this;
  };
  
  Zlib.prototype._afterSync = function () {
    var avail_out = this.strm.avail_out;
    var avail_in = this.strm.avail_in;
  
    this.write_in_progress = false;
  
    return [avail_in, avail_out];
  };
  
  Zlib.prototype._process = function () {
    var next_expected_header_byte = null;
  
    // If the avail_out is left at 0, then it means that it ran out
    // of room.  If there was avail_out left over, then it means
    // that all of the input was consumed.
    switch (this.mode) {
      case exports.DEFLATE:
      case exports.GZIP:
      case exports.DEFLATERAW:
        this.err = zlib_deflate.deflate(this.strm, this.flush);
        break;
      case exports.UNZIP:
        if (this.strm.avail_in > 0) {
          next_expected_header_byte = this.strm.next_in;
        }
  
        switch (this.gzip_id_bytes_read) {
          case 0:
            if (next_expected_header_byte === null) {
              break;
            }
  
            if (this.strm.input[next_expected_header_byte] === GZIP_HEADER_ID1) {
              this.gzip_id_bytes_read = 1;
              next_expected_header_byte++;
  
              if (this.strm.avail_in === 1) {
                // The only available byte was already read.
                break;
              }
            } else {
              this.mode = exports.INFLATE;
              break;
            }
  
          // fallthrough
          case 1:
            if (next_expected_header_byte === null) {
              break;
            }
  
            if (this.strm.input[next_expected_header_byte] === GZIP_HEADER_ID2) {
              this.gzip_id_bytes_read = 2;
              this.mode = exports.GUNZIP;
            } else {
              // There is no actual difference between INFLATE and INFLATERAW
              // (after initialization).
              this.mode = exports.INFLATE;
            }
  
            break;
          default:
            throw new Error('invalid number of gzip magic number bytes read');
        }
  
      // fallthrough
      case exports.INFLATE:
      case exports.GUNZIP:
      case exports.INFLATERAW:
        this.err = zlib_inflate.inflate(this.strm, this.flush
  
        // If data was encoded with dictionary
        );if (this.err === exports.Z_NEED_DICT && this.dictionary) {
          // Load it
          this.err = zlib_inflate.inflateSetDictionary(this.strm, this.dictionary);
          if (this.err === exports.Z_OK) {
            // And try to decode again
            this.err = zlib_inflate.inflate(this.strm, this.flush);
          } else if (this.err === exports.Z_DATA_ERROR) {
            // Both inflateSetDictionary() and inflate() return Z_DATA_ERROR.
            // Make it possible for After() to tell a bad dictionary from bad
            // input.
            this.err = exports.Z_NEED_DICT;
          }
        }
        while (this.strm.avail_in > 0 && this.mode === exports.GUNZIP && this.err === exports.Z_STREAM_END && this.strm.next_in[0] !== 0x00) {
          // Bytes remain in input buffer. Perhaps this is another compressed
          // member in the same archive, or just trailing garbage.
          // Trailing zero bytes are okay, though, since they are frequently
          // used for padding.
  
          this.reset();
          this.err = zlib_inflate.inflate(this.strm, this.flush);
        }
        break;
      default:
        throw new Error('Unknown mode ' + this.mode);
    }
  };
  
  Zlib.prototype._checkError = function () {
    // Acceptable error states depend on the type of zlib stream.
    switch (this.err) {
      case exports.Z_OK:
      case exports.Z_BUF_ERROR:
        if (this.strm.avail_out !== 0 && this.flush === exports.Z_FINISH) {
          this._error('unexpected end of file');
          return false;
        }
        break;
      case exports.Z_STREAM_END:
        // normal statuses, not fatal
        break;
      case exports.Z_NEED_DICT:
        if (this.dictionary == null) {
          this._error('Missing dictionary');
        } else {
          this._error('Bad dictionary');
        }
        return false;
      default:
        // something else.
        this._error('Zlib error');
        return false;
    }
  
    return true;
  };
  
  Zlib.prototype._after = function () {
    if (!this._checkError()) {
      return;
    }
  
    var avail_out = this.strm.avail_out;
    var avail_in = this.strm.avail_in;
  
    this.write_in_progress = false;
  
    // call the write() cb
    this.callback(avail_in, avail_out);
  
    if (this.pending_close) {
      this.close();
    }
  };
  
  Zlib.prototype._error = function (message) {
    if (this.strm.msg) {
      message = this.strm.msg;
    }
    this.onerror(message, this.err
  
    // no hope of rescue.
    );this.write_in_progress = false;
    if (this.pending_close) {
      this.close();
    }
  };
  
  Zlib.prototype.init = function (windowBits, level, memLevel, strategy, dictionary) {
    assert(arguments.length === 4 || arguments.length === 5, 'init(windowBits, level, memLevel, strategy, [dictionary])');
  
    assert(windowBits >= 8 && windowBits <= 15, 'invalid windowBits');
    assert(level >= -1 && level <= 9, 'invalid compression level');
  
    assert(memLevel >= 1 && memLevel <= 9, 'invalid memlevel');
  
    assert(strategy === exports.Z_FILTERED || strategy === exports.Z_HUFFMAN_ONLY || strategy === exports.Z_RLE || strategy === exports.Z_FIXED || strategy === exports.Z_DEFAULT_STRATEGY, 'invalid strategy');
  
    this._init(level, windowBits, memLevel, strategy, dictionary);
    this._setDictionary();
  };
  
  Zlib.prototype.params = function () {
    throw new Error('deflateParams Not supported');
  };
  
  Zlib.prototype.reset = function () {
    this._reset();
    this._setDictionary();
  };
  
  Zlib.prototype._init = function (level, windowBits, memLevel, strategy, dictionary) {
    this.level = level;
    this.windowBits = windowBits;
    this.memLevel = memLevel;
    this.strategy = strategy;
  
    this.flush = exports.Z_NO_FLUSH;
  
    this.err = exports.Z_OK;
  
    if (this.mode === exports.GZIP || this.mode === exports.GUNZIP) {
      this.windowBits += 16;
    }
  
    if (this.mode === exports.UNZIP) {
      this.windowBits += 32;
    }
  
    if (this.mode === exports.DEFLATERAW || this.mode === exports.INFLATERAW) {
      this.windowBits = -1 * this.windowBits;
    }
  
    this.strm = new Zstream();
  
    switch (this.mode) {
      case exports.DEFLATE:
      case exports.GZIP:
      case exports.DEFLATERAW:
        this.err = zlib_deflate.deflateInit2(this.strm, this.level, exports.Z_DEFLATED, this.windowBits, this.memLevel, this.strategy);
        break;
      case exports.INFLATE:
      case exports.GUNZIP:
      case exports.INFLATERAW:
      case exports.UNZIP:
        this.err = zlib_inflate.inflateInit2(this.strm, this.windowBits);
        break;
      default:
        throw new Error('Unknown mode ' + this.mode);
    }
  
    if (this.err !== exports.Z_OK) {
      this._error('Init error');
    }
  
    this.dictionary = dictionary;
  
    this.write_in_progress = false;
    this.init_done = true;
  };
  
  Zlib.prototype._setDictionary = function () {
    if (this.dictionary == null) {
      return;
    }
  
    this.err = exports.Z_OK;
  
    switch (this.mode) {
      case exports.DEFLATE:
      case exports.DEFLATERAW:
        this.err = zlib_deflate.deflateSetDictionary(this.strm, this.dictionary);
        break;
      default:
        break;
    }
  
    if (this.err !== exports.Z_OK) {
      this._error('Failed to set dictionary');
    }
  };
  
  Zlib.prototype._reset = function () {
    this.err = exports.Z_OK;
  
    switch (this.mode) {
      case exports.DEFLATE:
      case exports.DEFLATERAW:
      case exports.GZIP:
        this.err = zlib_deflate.deflateReset(this.strm);
        break;
      case exports.INFLATE:
      case exports.INFLATERAW:
      case exports.GUNZIP:
        this.err = zlib_inflate.inflateReset(this.strm);
        break;
      default:
        break;
    }
  
    if (this.err !== exports.Z_OK) {
      this._error('Failed to reset stream');
    }
  };
  
  exports.Zlib = Zlib;
  }).call(this,require('_process'),require("buffer").Buffer)
  },{"_process":32,"assert":1,"buffer":9,"pako/lib/zlib/constants":22,"pako/lib/zlib/deflate.js":24,"pako/lib/zlib/inflate.js":26,"pako/lib/zlib/zstream":30}],8:[function(require,module,exports){
  (function (process){
  'use strict';
  
  var Buffer = require('buffer').Buffer;
  var Transform = require('stream').Transform;
  var binding = require('./binding');
  var util = require('util');
  var assert = require('assert').ok;
  var kMaxLength = require('buffer').kMaxLength;
  var kRangeErrorMessage = 'Cannot create final Buffer. It would be larger ' + 'than 0x' + kMaxLength.toString(16) + ' bytes';
  
  // zlib doesn't provide these, so kludge them in following the same
  // const naming scheme zlib uses.
  binding.Z_MIN_WINDOWBITS = 8;
  binding.Z_MAX_WINDOWBITS = 15;
  binding.Z_DEFAULT_WINDOWBITS = 15;
  
  // fewer than 64 bytes per chunk is stupid.
  // technically it could work with as few as 8, but even 64 bytes
  // is absurdly low.  Usually a MB or more is best.
  binding.Z_MIN_CHUNK = 64;
  binding.Z_MAX_CHUNK = Infinity;
  binding.Z_DEFAULT_CHUNK = 16 * 1024;
  
  binding.Z_MIN_MEMLEVEL = 1;
  binding.Z_MAX_MEMLEVEL = 9;
  binding.Z_DEFAULT_MEMLEVEL = 8;
  
  binding.Z_MIN_LEVEL = -1;
  binding.Z_MAX_LEVEL = 9;
  binding.Z_DEFAULT_LEVEL = binding.Z_DEFAULT_COMPRESSION;
  
  // expose all the zlib constants
  var bkeys = Object.keys(binding);
  for (var bk = 0; bk < bkeys.length; bk++) {
    var bkey = bkeys[bk];
    if (bkey.match(/^Z/)) {
      Object.defineProperty(exports, bkey, {
        enumerable: true, value: binding[bkey], writable: false
      });
    }
  }
  
  // translation table for return codes.
  var codes = {
    Z_OK: binding.Z_OK,
    Z_STREAM_END: binding.Z_STREAM_END,
    Z_NEED_DICT: binding.Z_NEED_DICT,
    Z_ERRNO: binding.Z_ERRNO,
    Z_STREAM_ERROR: binding.Z_STREAM_ERROR,
    Z_DATA_ERROR: binding.Z_DATA_ERROR,
    Z_MEM_ERROR: binding.Z_MEM_ERROR,
    Z_BUF_ERROR: binding.Z_BUF_ERROR,
    Z_VERSION_ERROR: binding.Z_VERSION_ERROR
  };
  
  var ckeys = Object.keys(codes);
  for (var ck = 0; ck < ckeys.length; ck++) {
    var ckey = ckeys[ck];
    codes[codes[ckey]] = ckey;
  }
  
  Object.defineProperty(exports, 'codes', {
    enumerable: true, value: Object.freeze(codes), writable: false
  });
  
  exports.Deflate = Deflate;
  exports.Inflate = Inflate;
  exports.Gzip = Gzip;
  exports.Gunzip = Gunzip;
  exports.DeflateRaw = DeflateRaw;
  exports.InflateRaw = InflateRaw;
  exports.Unzip = Unzip;
  
  exports.createDeflate = function (o) {
    return new Deflate(o);
  };
  
  exports.createInflate = function (o) {
    return new Inflate(o);
  };
  
  exports.createDeflateRaw = function (o) {
    return new DeflateRaw(o);
  };
  
  exports.createInflateRaw = function (o) {
    return new InflateRaw(o);
  };
  
  exports.createGzip = function (o) {
    return new Gzip(o);
  };
  
  exports.createGunzip = function (o) {
    return new Gunzip(o);
  };
  
  exports.createUnzip = function (o) {
    return new Unzip(o);
  };
  
  // Convenience methods.
  // compress/decompress a string or buffer in one step.
  exports.deflate = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new Deflate(opts), buffer, callback);
  };
  
  exports.deflateSync = function (buffer, opts) {
    return zlibBufferSync(new Deflate(opts), buffer);
  };
  
  exports.gzip = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new Gzip(opts), buffer, callback);
  };
  
  exports.gzipSync = function (buffer, opts) {
    return zlibBufferSync(new Gzip(opts), buffer);
  };
  
  exports.deflateRaw = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new DeflateRaw(opts), buffer, callback);
  };
  
  exports.deflateRawSync = function (buffer, opts) {
    return zlibBufferSync(new DeflateRaw(opts), buffer);
  };
  
  exports.unzip = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new Unzip(opts), buffer, callback);
  };
  
  exports.unzipSync = function (buffer, opts) {
    return zlibBufferSync(new Unzip(opts), buffer);
  };
  
  exports.inflate = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new Inflate(opts), buffer, callback);
  };
  
  exports.inflateSync = function (buffer, opts) {
    return zlibBufferSync(new Inflate(opts), buffer);
  };
  
  exports.gunzip = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new Gunzip(opts), buffer, callback);
  };
  
  exports.gunzipSync = function (buffer, opts) {
    return zlibBufferSync(new Gunzip(opts), buffer);
  };
  
  exports.inflateRaw = function (buffer, opts, callback) {
    if (typeof opts === 'function') {
      callback = opts;
      opts = {};
    }
    return zlibBuffer(new InflateRaw(opts), buffer, callback);
  };
  
  exports.inflateRawSync = function (buffer, opts) {
    return zlibBufferSync(new InflateRaw(opts), buffer);
  };
  
  function zlibBuffer(engine, buffer, callback) {
    var buffers = [];
    var nread = 0;
  
    engine.on('error', onError);
    engine.on('end', onEnd);
  
    engine.end(buffer);
    flow();
  
    function flow() {
      var chunk;
      while (null !== (chunk = engine.read())) {
        buffers.push(chunk);
        nread += chunk.length;
      }
      engine.once('readable', flow);
    }
  
    function onError(err) {
      engine.removeListener('end', onEnd);
      engine.removeListener('readable', flow);
      callback(err);
    }
  
    function onEnd() {
      var buf;
      var err = null;
  
      if (nread >= kMaxLength) {
        err = new RangeError(kRangeErrorMessage);
      } else {
        buf = Buffer.concat(buffers, nread);
      }
  
      buffers = [];
      engine.close();
      callback(err, buf);
    }
  }
  
  function zlibBufferSync(engine, buffer) {
    if (typeof buffer === 'string') buffer = Buffer.from(buffer);
  
    if (!Buffer.isBuffer(buffer)) throw new TypeError('Not a string or buffer');
  
    var flushFlag = engine._finishFlushFlag;
  
    return engine._processChunk(buffer, flushFlag);
  }
  
  // generic zlib
  // minimal 2-byte header
  function Deflate(opts) {
    if (!(this instanceof Deflate)) return new Deflate(opts);
    Zlib.call(this, opts, binding.DEFLATE);
  }
  
  function Inflate(opts) {
    if (!(this instanceof Inflate)) return new Inflate(opts);
    Zlib.call(this, opts, binding.INFLATE);
  }
  
  // gzip - bigger header, same deflate compression
  function Gzip(opts) {
    if (!(this instanceof Gzip)) return new Gzip(opts);
    Zlib.call(this, opts, binding.GZIP);
  }
  
  function Gunzip(opts) {
    if (!(this instanceof Gunzip)) return new Gunzip(opts);
    Zlib.call(this, opts, binding.GUNZIP);
  }
  
  // raw - no header
  function DeflateRaw(opts) {
    if (!(this instanceof DeflateRaw)) return new DeflateRaw(opts);
    Zlib.call(this, opts, binding.DEFLATERAW);
  }
  
  function InflateRaw(opts) {
    if (!(this instanceof InflateRaw)) return new InflateRaw(opts);
    Zlib.call(this, opts, binding.INFLATERAW);
  }
  
  // auto-detect header.
  function Unzip(opts) {
    if (!(this instanceof Unzip)) return new Unzip(opts);
    Zlib.call(this, opts, binding.UNZIP);
  }
  
  function isValidFlushFlag(flag) {
    return flag === binding.Z_NO_FLUSH || flag === binding.Z_PARTIAL_FLUSH || flag === binding.Z_SYNC_FLUSH || flag === binding.Z_FULL_FLUSH || flag === binding.Z_FINISH || flag === binding.Z_BLOCK;
  }
  
  // the Zlib class they all inherit from
  // This thing manages the queue of requests, and returns
  // true or false if there is anything in the queue when
  // you call the .write() method.
  
  function Zlib(opts, mode) {
    var _this = this;
  
    this._opts = opts = opts || {};
    this._chunkSize = opts.chunkSize || exports.Z_DEFAULT_CHUNK;
  
    Transform.call(this, opts);
  
    if (opts.flush && !isValidFlushFlag(opts.flush)) {
      throw new Error('Invalid flush flag: ' + opts.flush);
    }
    if (opts.finishFlush && !isValidFlushFlag(opts.finishFlush)) {
      throw new Error('Invalid flush flag: ' + opts.finishFlush);
    }
  
    this._flushFlag = opts.flush || binding.Z_NO_FLUSH;
    this._finishFlushFlag = typeof opts.finishFlush !== 'undefined' ? opts.finishFlush : binding.Z_FINISH;
  
    if (opts.chunkSize) {
      if (opts.chunkSize < exports.Z_MIN_CHUNK || opts.chunkSize > exports.Z_MAX_CHUNK) {
        throw new Error('Invalid chunk size: ' + opts.chunkSize);
      }
    }
  
    if (opts.windowBits) {
      if (opts.windowBits < exports.Z_MIN_WINDOWBITS || opts.windowBits > exports.Z_MAX_WINDOWBITS) {
        throw new Error('Invalid windowBits: ' + opts.windowBits);
      }
    }
  
    if (opts.level) {
      if (opts.level < exports.Z_MIN_LEVEL || opts.level > exports.Z_MAX_LEVEL) {
        throw new Error('Invalid compression level: ' + opts.level);
      }
    }
  
    if (opts.memLevel) {
      if (opts.memLevel < exports.Z_MIN_MEMLEVEL || opts.memLevel > exports.Z_MAX_MEMLEVEL) {
        throw new Error('Invalid memLevel: ' + opts.memLevel);
      }
    }
  
    if (opts.strategy) {
      if (opts.strategy != exports.Z_FILTERED && opts.strategy != exports.Z_HUFFMAN_ONLY && opts.strategy != exports.Z_RLE && opts.strategy != exports.Z_FIXED && opts.strategy != exports.Z_DEFAULT_STRATEGY) {
        throw new Error('Invalid strategy: ' + opts.strategy);
      }
    }
  
    if (opts.dictionary) {
      if (!Buffer.isBuffer(opts.dictionary)) {
        throw new Error('Invalid dictionary: it should be a Buffer instance');
      }
    }
  
    this._handle = new binding.Zlib(mode);
  
    var self = this;
    this._hadError = false;
    this._handle.onerror = function (message, errno) {
      // there is no way to cleanly recover.
      // continuing only obscures problems.
      _close(self);
      self._hadError = true;
  
      var error = new Error(message);
      error.errno = errno;
      error.code = exports.codes[errno];
      self.emit('error', error);
    };
  
    var level = exports.Z_DEFAULT_COMPRESSION;
    if (typeof opts.level === 'number') level = opts.level;
  
    var strategy = exports.Z_DEFAULT_STRATEGY;
    if (typeof opts.strategy === 'number') strategy = opts.strategy;
  
    this._handle.init(opts.windowBits || exports.Z_DEFAULT_WINDOWBITS, level, opts.memLevel || exports.Z_DEFAULT_MEMLEVEL, strategy, opts.dictionary);
  
    this._buffer = Buffer.allocUnsafe(this._chunkSize);
    this._offset = 0;
    this._level = level;
    this._strategy = strategy;
  
    this.once('end', this.close);
  
    Object.defineProperty(this, '_closed', {
      get: function () {
        return !_this._handle;
      },
      configurable: true,
      enumerable: true
    });
  }
  
  util.inherits(Zlib, Transform);
  
  Zlib.prototype.params = function (level, strategy, callback) {
    if (level < exports.Z_MIN_LEVEL || level > exports.Z_MAX_LEVEL) {
      throw new RangeError('Invalid compression level: ' + level);
    }
    if (strategy != exports.Z_FILTERED && strategy != exports.Z_HUFFMAN_ONLY && strategy != exports.Z_RLE && strategy != exports.Z_FIXED && strategy != exports.Z_DEFAULT_STRATEGY) {
      throw new TypeError('Invalid strategy: ' + strategy);
    }
  
    if (this._level !== level || this._strategy !== strategy) {
      var self = this;
      this.flush(binding.Z_SYNC_FLUSH, function () {
        assert(self._handle, 'zlib binding closed');
        self._handle.params(level, strategy);
        if (!self._hadError) {
          self._level = level;
          self._strategy = strategy;
          if (callback) callback();
        }
      });
    } else {
      process.nextTick(callback);
    }
  };
  
  Zlib.prototype.reset = function () {
    assert(this._handle, 'zlib binding closed');
    return this._handle.reset();
  };
  
  // This is the _flush function called by the transform class,
  // internally, when the last chunk has been written.
  Zlib.prototype._flush = function (callback) {
    this._transform(Buffer.alloc(0), '', callback);
  };
  
  Zlib.prototype.flush = function (kind, callback) {
    var _this2 = this;
  
    var ws = this._writableState;
  
    if (typeof kind === 'function' || kind === undefined && !callback) {
      callback = kind;
      kind = binding.Z_FULL_FLUSH;
    }
  
    if (ws.ended) {
      if (callback) process.nextTick(callback);
    } else if (ws.ending) {
      if (callback) this.once('end', callback);
    } else if (ws.needDrain) {
      if (callback) {
        this.once('drain', function () {
          return _this2.flush(kind, callback);
        });
      }
    } else {
      this._flushFlag = kind;
      this.write(Buffer.alloc(0), '', callback);
    }
  };
  
  Zlib.prototype.close = function (callback) {
    _close(this, callback);
    process.nextTick(emitCloseNT, this);
  };
  
  function _close(engine, callback) {
    if (callback) process.nextTick(callback);
  
    // Caller may invoke .close after a zlib error (which will null _handle).
    if (!engine._handle) return;
  
    engine._handle.close();
    engine._handle = null;
  }
  
  function emitCloseNT(self) {
    self.emit('close');
  }
  
  Zlib.prototype._transform = function (chunk, encoding, cb) {
    var flushFlag;
    var ws = this._writableState;
    var ending = ws.ending || ws.ended;
    var last = ending && (!chunk || ws.length === chunk.length);
  
    if (chunk !== null && !Buffer.isBuffer(chunk)) return cb(new Error('invalid input'));
  
    if (!this._handle) return cb(new Error('zlib binding closed'));
  
    // If it's the last chunk, or a final flush, we use the Z_FINISH flush flag
    // (or whatever flag was provided using opts.finishFlush).
    // If it's explicitly flushing at some other time, then we use
    // Z_FULL_FLUSH. Otherwise, use Z_NO_FLUSH for maximum compression
    // goodness.
    if (last) flushFlag = this._finishFlushFlag;else {
      flushFlag = this._flushFlag;
      // once we've flushed the last of the queue, stop flushing and
      // go back to the normal behavior.
      if (chunk.length >= ws.length) {
        this._flushFlag = this._opts.flush || binding.Z_NO_FLUSH;
      }
    }
  
    this._processChunk(chunk, flushFlag, cb);
  };
  
  Zlib.prototype._processChunk = function (chunk, flushFlag, cb) {
    var availInBefore = chunk && chunk.length;
    var availOutBefore = this._chunkSize - this._offset;
    var inOff = 0;
  
    var self = this;
  
    var async = typeof cb === 'function';
  
    if (!async) {
      var buffers = [];
      var nread = 0;
  
      var error;
      this.on('error', function (er) {
        error = er;
      });
  
      assert(this._handle, 'zlib binding closed');
      do {
        var res = this._handle.writeSync(flushFlag, chunk, // in
        inOff, // in_off
        availInBefore, // in_len
        this._buffer, // out
        this._offset, //out_off
        availOutBefore); // out_len
      } while (!this._hadError && callback(res[0], res[1]));
  
      if (this._hadError) {
        throw error;
      }
  
      if (nread >= kMaxLength) {
        _close(this);
        throw new RangeError(kRangeErrorMessage);
      }
  
      var buf = Buffer.concat(buffers, nread);
      _close(this);
  
      return buf;
    }
  
    assert(this._handle, 'zlib binding closed');
    var req = this._handle.write(flushFlag, chunk, // in
    inOff, // in_off
    availInBefore, // in_len
    this._buffer, // out
    this._offset, //out_off
    availOutBefore); // out_len
  
    req.buffer = chunk;
    req.callback = callback;
  
    function callback(availInAfter, availOutAfter) {
      // When the callback is used in an async write, the callback's
      // context is the `req` object that was created. The req object
      // is === this._handle, and that's why it's important to null
      // out the values after they are done being used. `this._handle`
      // can stay in memory longer than the callback and buffer are needed.
      if (this) {
        this.buffer = null;
        this.callback = null;
      }
  
      if (self._hadError) return;
  
      var have = availOutBefore - availOutAfter;
      assert(have >= 0, 'have should not go down');
  
      if (have > 0) {
        var out = self._buffer.slice(self._offset, self._offset + have);
        self._offset += have;
        // serve some output to the consumer.
        if (async) {
          self.push(out);
        } else {
          buffers.push(out);
          nread += out.length;
        }
      }
  
      // exhausted the output buffer, or used all the input create a new one.
      if (availOutAfter === 0 || self._offset >= self._chunkSize) {
        availOutBefore = self._chunkSize;
        self._offset = 0;
        self._buffer = Buffer.allocUnsafe(self._chunkSize);
      }
  
      if (availOutAfter === 0) {
        // Not actually done.  Need to reprocess.
        // Also, update the availInBefore to the availInAfter value,
        // so that if we have to hit it a third (fourth, etc.) time,
        // it'll have the correct byte counts.
        inOff += availInBefore - availInAfter;
        availInBefore = availInAfter;
  
        if (!async) return true;
  
        var newReq = self._handle.write(flushFlag, chunk, inOff, availInBefore, self._buffer, self._offset, self._chunkSize);
        newReq.callback = callback; // this same function
        newReq.buffer = chunk;
        return;
      }
  
      if (!async) return false;
  
      // finished with the chunk.
      cb();
    }
  };
  
  util.inherits(Deflate, Zlib);
  util.inherits(Inflate, Zlib);
  util.inherits(Gzip, Zlib);
  util.inherits(Gunzip, Zlib);
  util.inherits(DeflateRaw, Zlib);
  util.inherits(InflateRaw, Zlib);
  util.inherits(Unzip, Zlib);
  }).call(this,require('_process'))
  },{"./binding":7,"_process":32,"assert":1,"buffer":9,"stream":53,"util":80}],9:[function(require,module,exports){
  (function (Buffer){
  /*!
   * The buffer module from node.js, for the browser.
   *
   * @author   Feross Aboukhadijeh <https://feross.org>
   * @license  MIT
   */
  /* eslint-disable no-proto */
  
  'use strict'
  
  var base64 = require('base64-js')
  var ieee754 = require('ieee754')
  
  exports.Buffer = Buffer
  exports.SlowBuffer = SlowBuffer
  exports.INSPECT_MAX_BYTES = 50
  
  var K_MAX_LENGTH = 0x7fffffff
  exports.kMaxLength = K_MAX_LENGTH
  
  /**
   * If `Buffer.TYPED_ARRAY_SUPPORT`:
   *   === true    Use Uint8Array implementation (fastest)
   *   === false   Print warning and recommend using `buffer` v4.x which has an Object
   *               implementation (most compatible, even IE6)
   *
   * Browsers that support typed arrays are IE 10+, Firefox 4+, Chrome 7+, Safari 5.1+,
   * Opera 11.6+, iOS 4.2+.
   *
   * We report that the browser does not support typed arrays if the are not subclassable
   * using __proto__. Firefox 4-29 lacks support for adding new properties to `Uint8Array`
   * (See: https://bugzilla.mozilla.org/show_bug.cgi?id=695438). IE 10 lacks support
   * for __proto__ and has a buggy typed array implementation.
   */
  Buffer.TYPED_ARRAY_SUPPORT = typedArraySupport()
  
  if (!Buffer.TYPED_ARRAY_SUPPORT && typeof console !== 'undefined' &&
      typeof console.error === 'function') {
    console.error(
      'This browser lacks typed array (Uint8Array) support which is required by ' +
      '`buffer` v5.x. Use `buffer` v4.x if you require old browser support.'
    )
  }
  
  function typedArraySupport () {
    // Can typed array instances can be augmented?
    try {
      var arr = new Uint8Array(1)
      arr.__proto__ = { __proto__: Uint8Array.prototype, foo: function () { return 42 } }
      return arr.foo() === 42
    } catch (e) {
      return false
    }
  }
  
  Object.defineProperty(Buffer.prototype, 'parent', {
    enumerable: true,
    get: function () {
      if (!Buffer.isBuffer(this)) return undefined
      return this.buffer
    }
  })
  
  Object.defineProperty(Buffer.prototype, 'offset', {
    enumerable: true,
    get: function () {
      if (!Buffer.isBuffer(this)) return undefined
      return this.byteOffset
    }
  })
  
  function createBuffer (length) {
    if (length > K_MAX_LENGTH) {
      throw new RangeError('The value "' + length + '" is invalid for option "size"')
    }
    // Return an augmented `Uint8Array` instance
    var buf = new Uint8Array(length)
    buf.__proto__ = Buffer.prototype
    return buf
  }
  
  /**
   * The Buffer constructor returns instances of `Uint8Array` that have their
   * prototype changed to `Buffer.prototype`. Furthermore, `Buffer` is a subclass of
   * `Uint8Array`, so the returned instances will have all the node `Buffer` methods
   * and the `Uint8Array` methods. Square bracket notation works as expected -- it
   * returns a single octet.
   *
   * The `Uint8Array` prototype remains unmodified.
   */
  
  function Buffer (arg, encodingOrOffset, length) {
    // Common case.
    if (typeof arg === 'number') {
      if (typeof encodingOrOffset === 'string') {
        throw new TypeError(
          'The "string" argument must be of type string. Received type number'
        )
      }
      return allocUnsafe(arg)
    }
    return from(arg, encodingOrOffset, length)
  }
  
  // Fix subarray() in ES2016. See: https://github.com/feross/buffer/pull/97
  if (typeof Symbol !== 'undefined' && Symbol.species != null &&
      Buffer[Symbol.species] === Buffer) {
    Object.defineProperty(Buffer, Symbol.species, {
      value: null,
      configurable: true,
      enumerable: false,
      writable: false
    })
  }
  
  Buffer.poolSize = 8192 // not used by this implementation
  
  function from (value, encodingOrOffset, length) {
    if (typeof value === 'string') {
      return fromString(value, encodingOrOffset)
    }
  
    if (ArrayBuffer.isView(value)) {
      return fromArrayLike(value)
    }
  
    if (value == null) {
      throw TypeError(
        'The first argument must be one of type string, Buffer, ArrayBuffer, Array, ' +
        'or Array-like Object. Received type ' + (typeof value)
      )
    }
  
    if (isInstance(value, ArrayBuffer) ||
        (value && isInstance(value.buffer, ArrayBuffer))) {
      return fromArrayBuffer(value, encodingOrOffset, length)
    }
  
    if (typeof value === 'number') {
      throw new TypeError(
        'The "value" argument must not be of type number. Received type number'
      )
    }
  
    var valueOf = value.valueOf && value.valueOf()
    if (valueOf != null && valueOf !== value) {
      return Buffer.from(valueOf, encodingOrOffset, length)
    }
  
    var b = fromObject(value)
    if (b) return b
  
    if (typeof Symbol !== 'undefined' && Symbol.toPrimitive != null &&
        typeof value[Symbol.toPrimitive] === 'function') {
      return Buffer.from(
        value[Symbol.toPrimitive]('string'), encodingOrOffset, length
      )
    }
  
    throw new TypeError(
      'The first argument must be one of type string, Buffer, ArrayBuffer, Array, ' +
      'or Array-like Object. Received type ' + (typeof value)
    )
  }
  
  /**
   * Functionally equivalent to Buffer(arg, encoding) but throws a TypeError
   * if value is a number.
   * Buffer.from(str[, encoding])
   * Buffer.from(array)
   * Buffer.from(buffer)
   * Buffer.from(arrayBuffer[, byteOffset[, length]])
   **/
  Buffer.from = function (value, encodingOrOffset, length) {
    return from(value, encodingOrOffset, length)
  }
  
  // Note: Change prototype *after* Buffer.from is defined to workaround Chrome bug:
  // https://github.com/feross/buffer/pull/148
  Buffer.prototype.__proto__ = Uint8Array.prototype
  Buffer.__proto__ = Uint8Array
  
  function assertSize (size) {
    if (typeof size !== 'number') {
      throw new TypeError('"size" argument must be of type number')
    } else if (size < 0) {
      throw new RangeError('The value "' + size + '" is invalid for option "size"')
    }
  }
  
  function alloc (size, fill, encoding) {
    assertSize(size)
    if (size <= 0) {
      return createBuffer(size)
    }
    if (fill !== undefined) {
      // Only pay attention to encoding if it's a string. This
      // prevents accidentally sending in a number that would
      // be interpretted as a start offset.
      return typeof encoding === 'string'
        ? createBuffer(size).fill(fill, encoding)
        : createBuffer(size).fill(fill)
    }
    return createBuffer(size)
  }
  
  /**
   * Creates a new filled Buffer instance.
   * alloc(size[, fill[, encoding]])
   **/
  Buffer.alloc = function (size, fill, encoding) {
    return alloc(size, fill, encoding)
  }
  
  function allocUnsafe (size) {
    assertSize(size)
    return createBuffer(size < 0 ? 0 : checked(size) | 0)
  }
  
  /**
   * Equivalent to Buffer(num), by default creates a non-zero-filled Buffer instance.
   * */
  Buffer.allocUnsafe = function (size) {
    return allocUnsafe(size)
  }
  /**
   * Equivalent to SlowBuffer(num), by default creates a non-zero-filled Buffer instance.
   */
  Buffer.allocUnsafeSlow = function (size) {
    return allocUnsafe(size)
  }
  
  function fromString (string, encoding) {
    if (typeof encoding !== 'string' || encoding === '') {
      encoding = 'utf8'
    }
  
    if (!Buffer.isEncoding(encoding)) {
      throw new TypeError('Unknown encoding: ' + encoding)
    }
  
    var length = byteLength(string, encoding) | 0
    var buf = createBuffer(length)
  
    var actual = buf.write(string, encoding)
  
    if (actual !== length) {
      // Writing a hex string, for example, that contains invalid characters will
      // cause everything after the first invalid character to be ignored. (e.g.
      // 'abxxcd' will be treated as 'ab')
      buf = buf.slice(0, actual)
    }
  
    return buf
  }
  
  function fromArrayLike (array) {
    var length = array.length < 0 ? 0 : checked(array.length) | 0
    var buf = createBuffer(length)
    for (var i = 0; i < length; i += 1) {
      buf[i] = array[i] & 255
    }
    return buf
  }
  
  function fromArrayBuffer (array, byteOffset, length) {
    if (byteOffset < 0 || array.byteLength < byteOffset) {
      throw new RangeError('"offset" is outside of buffer bounds')
    }
  
    if (array.byteLength < byteOffset + (length || 0)) {
      throw new RangeError('"length" is outside of buffer bounds')
    }
  
    var buf
    if (byteOffset === undefined && length === undefined) {
      buf = new Uint8Array(array)
    } else if (length === undefined) {
      buf = new Uint8Array(array, byteOffset)
    } else {
      buf = new Uint8Array(array, byteOffset, length)
    }
  
    // Return an augmented `Uint8Array` instance
    buf.__proto__ = Buffer.prototype
    return buf
  }
  
  function fromObject (obj) {
    if (Buffer.isBuffer(obj)) {
      var len = checked(obj.length) | 0
      var buf = createBuffer(len)
  
      if (buf.length === 0) {
        return buf
      }
  
      obj.copy(buf, 0, 0, len)
      return buf
    }
  
    if (obj.length !== undefined) {
      if (typeof obj.length !== 'number' || numberIsNaN(obj.length)) {
        return createBuffer(0)
      }
      return fromArrayLike(obj)
    }
  
    if (obj.type === 'Buffer' && Array.isArray(obj.data)) {
      return fromArrayLike(obj.data)
    }
  }
  
  function checked (length) {
    // Note: cannot use `length < K_MAX_LENGTH` here because that fails when
    // length is NaN (which is otherwise coerced to zero.)
    if (length >= K_MAX_LENGTH) {
      throw new RangeError('Attempt to allocate Buffer larger than maximum ' +
                           'size: 0x' + K_MAX_LENGTH.toString(16) + ' bytes')
    }
    return length | 0
  }
  
  function SlowBuffer (length) {
    if (+length != length) { // eslint-disable-line eqeqeq
      length = 0
    }
    return Buffer.alloc(+length)
  }
  
  Buffer.isBuffer = function isBuffer (b) {
    return b != null && b._isBuffer === true &&
      b !== Buffer.prototype // so Buffer.isBuffer(Buffer.prototype) will be false
  }
  
  Buffer.compare = function compare (a, b) {
    if (isInstance(a, Uint8Array)) a = Buffer.from(a, a.offset, a.byteLength)
    if (isInstance(b, Uint8Array)) b = Buffer.from(b, b.offset, b.byteLength)
    if (!Buffer.isBuffer(a) || !Buffer.isBuffer(b)) {
      throw new TypeError(
        'The "buf1", "buf2" arguments must be one of type Buffer or Uint8Array'
      )
    }
  
    if (a === b) return 0
  
    var x = a.length
    var y = b.length
  
    for (var i = 0, len = Math.min(x, y); i < len; ++i) {
      if (a[i] !== b[i]) {
        x = a[i]
        y = b[i]
        break
      }
    }
  
    if (x < y) return -1
    if (y < x) return 1
    return 0
  }
  
  Buffer.isEncoding = function isEncoding (encoding) {
    switch (String(encoding).toLowerCase()) {
      case 'hex':
      case 'utf8':
      case 'utf-8':
      case 'ascii':
      case 'latin1':
      case 'binary':
      case 'base64':
      case 'ucs2':
      case 'ucs-2':
      case 'utf16le':
      case 'utf-16le':
        return true
      default:
        return false
    }
  }
  
  Buffer.concat = function concat (list, length) {
    if (!Array.isArray(list)) {
      throw new TypeError('"list" argument must be an Array of Buffers')
    }
  
    if (list.length === 0) {
      return Buffer.alloc(0)
    }
  
    var i
    if (length === undefined) {
      length = 0
      for (i = 0; i < list.length; ++i) {
        length += list[i].length
      }
    }
  
    var buffer = Buffer.allocUnsafe(length)
    var pos = 0
    for (i = 0; i < list.length; ++i) {
      var buf = list[i]
      if (isInstance(buf, Uint8Array)) {
        buf = Buffer.from(buf)
      }
      if (!Buffer.isBuffer(buf)) {
        throw new TypeError('"list" argument must be an Array of Buffers')
      }
      buf.copy(buffer, pos)
      pos += buf.length
    }
    return buffer
  }
  
  function byteLength (string, encoding) {
    if (Buffer.isBuffer(string)) {
      return string.length
    }
    if (ArrayBuffer.isView(string) || isInstance(string, ArrayBuffer)) {
      return string.byteLength
    }
    if (typeof string !== 'string') {
      throw new TypeError(
        'The "string" argument must be one of type string, Buffer, or ArrayBuffer. ' +
        'Received type ' + typeof string
      )
    }
  
    var len = string.length
    var mustMatch = (arguments.length > 2 && arguments[2] === true)
    if (!mustMatch && len === 0) return 0
  
    // Use a for loop to avoid recursion
    var loweredCase = false
    for (;;) {
      switch (encoding) {
        case 'ascii':
        case 'latin1':
        case 'binary':
          return len
        case 'utf8':
        case 'utf-8':
          return utf8ToBytes(string).length
        case 'ucs2':
        case 'ucs-2':
        case 'utf16le':
        case 'utf-16le':
          return len * 2
        case 'hex':
          return len >>> 1
        case 'base64':
          return base64ToBytes(string).length
        default:
          if (loweredCase) {
            return mustMatch ? -1 : utf8ToBytes(string).length // assume utf8
          }
          encoding = ('' + encoding).toLowerCase()
          loweredCase = true
      }
    }
  }
  Buffer.byteLength = byteLength
  
  function slowToString (encoding, start, end) {
    var loweredCase = false
  
    // No need to verify that "this.length <= MAX_UINT32" since it's a read-only
    // property of a typed array.
  
    // This behaves neither like String nor Uint8Array in that we set start/end
    // to their upper/lower bounds if the value passed is out of range.
    // undefined is handled specially as per ECMA-262 6th Edition,
    // Section 13.3.3.7 Runtime Semantics: KeyedBindingInitialization.
    if (start === undefined || start < 0) {
      start = 0
    }
    // Return early if start > this.length. Done here to prevent potential uint32
    // coercion fail below.
    if (start > this.length) {
      return ''
    }
  
    if (end === undefined || end > this.length) {
      end = this.length
    }
  
    if (end <= 0) {
      return ''
    }
  
    // Force coersion to uint32. This will also coerce falsey/NaN values to 0.
    end >>>= 0
    start >>>= 0
  
    if (end <= start) {
      return ''
    }
  
    if (!encoding) encoding = 'utf8'
  
    while (true) {
      switch (encoding) {
        case 'hex':
          return hexSlice(this, start, end)
  
        case 'utf8':
        case 'utf-8':
          return utf8Slice(this, start, end)
  
        case 'ascii':
          return asciiSlice(this, start, end)
  
        case 'latin1':
        case 'binary':
          return latin1Slice(this, start, end)
  
        case 'base64':
          return base64Slice(this, start, end)
  
        case 'ucs2':
        case 'ucs-2':
        case 'utf16le':
        case 'utf-16le':
          return utf16leSlice(this, start, end)
  
        default:
          if (loweredCase) throw new TypeError('Unknown encoding: ' + encoding)
          encoding = (encoding + '').toLowerCase()
          loweredCase = true
      }
    }
  }
  
  // This property is used by `Buffer.isBuffer` (and the `is-buffer` npm package)
  // to detect a Buffer instance. It's not possible to use `instanceof Buffer`
  // reliably in a browserify context because there could be multiple different
  // copies of the 'buffer' package in use. This method works even for Buffer
  // instances that were created from another copy of the `buffer` package.
  // See: https://github.com/feross/buffer/issues/154
  Buffer.prototype._isBuffer = true
  
  function swap (b, n, m) {
    var i = b[n]
    b[n] = b[m]
    b[m] = i
  }
  
  Buffer.prototype.swap16 = function swap16 () {
    var len = this.length
    if (len % 2 !== 0) {
      throw new RangeError('Buffer size must be a multiple of 16-bits')
    }
    for (var i = 0; i < len; i += 2) {
      swap(this, i, i + 1)
    }
    return this
  }
  
  Buffer.prototype.swap32 = function swap32 () {
    var len = this.length
    if (len % 4 !== 0) {
      throw new RangeError('Buffer size must be a multiple of 32-bits')
    }
    for (var i = 0; i < len; i += 4) {
      swap(this, i, i + 3)
      swap(this, i + 1, i + 2)
    }
    return this
  }
  
  Buffer.prototype.swap64 = function swap64 () {
    var len = this.length
    if (len % 8 !== 0) {
      throw new RangeError('Buffer size must be a multiple of 64-bits')
    }
    for (var i = 0; i < len; i += 8) {
      swap(this, i, i + 7)
      swap(this, i + 1, i + 6)
      swap(this, i + 2, i + 5)
      swap(this, i + 3, i + 4)
    }
    return this
  }
  
  Buffer.prototype.toString = function toString () {
    var length = this.length
    if (length === 0) return ''
    if (arguments.length === 0) return utf8Slice(this, 0, length)
    return slowToString.apply(this, arguments)
  }
  
  Buffer.prototype.toLocaleString = Buffer.prototype.toString
  
  Buffer.prototype.equals = function equals (b) {
    if (!Buffer.isBuffer(b)) throw new TypeError('Argument must be a Buffer')
    if (this === b) return true
    return Buffer.compare(this, b) === 0
  }
  
  Buffer.prototype.inspect = function inspect () {
    var str = ''
    var max = exports.INSPECT_MAX_BYTES
    str = this.toString('hex', 0, max).replace(/(.{2})/g, '$1 ').trim()
    if (this.length > max) str += ' ... '
    return '<Buffer ' + str + '>'
  }
  
  Buffer.prototype.compare = function compare (target, start, end, thisStart, thisEnd) {
    if (isInstance(target, Uint8Array)) {
      target = Buffer.from(target, target.offset, target.byteLength)
    }
    if (!Buffer.isBuffer(target)) {
      throw new TypeError(
        'The "target" argument must be one of type Buffer or Uint8Array. ' +
        'Received type ' + (typeof target)
      )
    }
  
    if (start === undefined) {
      start = 0
    }
    if (end === undefined) {
      end = target ? target.length : 0
    }
    if (thisStart === undefined) {
      thisStart = 0
    }
    if (thisEnd === undefined) {
      thisEnd = this.length
    }
  
    if (start < 0 || end > target.length || thisStart < 0 || thisEnd > this.length) {
      throw new RangeError('out of range index')
    }
  
    if (thisStart >= thisEnd && start >= end) {
      return 0
    }
    if (thisStart >= thisEnd) {
      return -1
    }
    if (start >= end) {
      return 1
    }
  
    start >>>= 0
    end >>>= 0
    thisStart >>>= 0
    thisEnd >>>= 0
  
    if (this === target) return 0
  
    var x = thisEnd - thisStart
    var y = end - start
    var len = Math.min(x, y)
  
    var thisCopy = this.slice(thisStart, thisEnd)
    var targetCopy = target.slice(start, end)
  
    for (var i = 0; i < len; ++i) {
      if (thisCopy[i] !== targetCopy[i]) {
        x = thisCopy[i]
        y = targetCopy[i]
        break
      }
    }
  
    if (x < y) return -1
    if (y < x) return 1
    return 0
  }
  
  // Finds either the first index of `val` in `buffer` at offset >= `byteOffset`,
  // OR the last index of `val` in `buffer` at offset <= `byteOffset`.
  //
  // Arguments:
  // - buffer - a Buffer to search
  // - val - a string, Buffer, or number
  // - byteOffset - an index into `buffer`; will be clamped to an int32
  // - encoding - an optional encoding, relevant is val is a string
  // - dir - true for indexOf, false for lastIndexOf
  function bidirectionalIndexOf (buffer, val, byteOffset, encoding, dir) {
    // Empty buffer means no match
    if (buffer.length === 0) return -1
  
    // Normalize byteOffset
    if (typeof byteOffset === 'string') {
      encoding = byteOffset
      byteOffset = 0
    } else if (byteOffset > 0x7fffffff) {
      byteOffset = 0x7fffffff
    } else if (byteOffset < -0x80000000) {
      byteOffset = -0x80000000
    }
    byteOffset = +byteOffset // Coerce to Number.
    if (numberIsNaN(byteOffset)) {
      // byteOffset: it it's undefined, null, NaN, "foo", etc, search whole buffer
      byteOffset = dir ? 0 : (buffer.length - 1)
    }
  
    // Normalize byteOffset: negative offsets start from the end of the buffer
    if (byteOffset < 0) byteOffset = buffer.length + byteOffset
    if (byteOffset >= buffer.length) {
      if (dir) return -1
      else byteOffset = buffer.length - 1
    } else if (byteOffset < 0) {
      if (dir) byteOffset = 0
      else return -1
    }
  
    // Normalize val
    if (typeof val === 'string') {
      val = Buffer.from(val, encoding)
    }
  
    // Finally, search either indexOf (if dir is true) or lastIndexOf
    if (Buffer.isBuffer(val)) {
      // Special case: looking for empty string/buffer always fails
      if (val.length === 0) {
        return -1
      }
      return arrayIndexOf(buffer, val, byteOffset, encoding, dir)
    } else if (typeof val === 'number') {
      val = val & 0xFF // Search for a byte value [0-255]
      if (typeof Uint8Array.prototype.indexOf === 'function') {
        if (dir) {
          return Uint8Array.prototype.indexOf.call(buffer, val, byteOffset)
        } else {
          return Uint8Array.prototype.lastIndexOf.call(buffer, val, byteOffset)
        }
      }
      return arrayIndexOf(buffer, [ val ], byteOffset, encoding, dir)
    }
  
    throw new TypeError('val must be string, number or Buffer')
  }
  
  function arrayIndexOf (arr, val, byteOffset, encoding, dir) {
    var indexSize = 1
    var arrLength = arr.length
    var valLength = val.length
  
    if (encoding !== undefined) {
      encoding = String(encoding).toLowerCase()
      if (encoding === 'ucs2' || encoding === 'ucs-2' ||
          encoding === 'utf16le' || encoding === 'utf-16le') {
        if (arr.length < 2 || val.length < 2) {
          return -1
        }
        indexSize = 2
        arrLength /= 2
        valLength /= 2
        byteOffset /= 2
      }
    }
  
    function read (buf, i) {
      if (indexSize === 1) {
        return buf[i]
      } else {
        return buf.readUInt16BE(i * indexSize)
      }
    }
  
    var i
    if (dir) {
      var foundIndex = -1
      for (i = byteOffset; i < arrLength; i++) {
        if (read(arr, i) === read(val, foundIndex === -1 ? 0 : i - foundIndex)) {
          if (foundIndex === -1) foundIndex = i
          if (i - foundIndex + 1 === valLength) return foundIndex * indexSize
        } else {
          if (foundIndex !== -1) i -= i - foundIndex
          foundIndex = -1
        }
      }
    } else {
      if (byteOffset + valLength > arrLength) byteOffset = arrLength - valLength
      for (i = byteOffset; i >= 0; i--) {
        var found = true
        for (var j = 0; j < valLength; j++) {
          if (read(arr, i + j) !== read(val, j)) {
            found = false
            break
          }
        }
        if (found) return i
      }
    }
  
    return -1
  }
  
  Buffer.prototype.includes = function includes (val, byteOffset, encoding) {
    return this.indexOf(val, byteOffset, encoding) !== -1
  }
  
  Buffer.prototype.indexOf = function indexOf (val, byteOffset, encoding) {
    return bidirectionalIndexOf(this, val, byteOffset, encoding, true)
  }
  
  Buffer.prototype.lastIndexOf = function lastIndexOf (val, byteOffset, encoding) {
    return bidirectionalIndexOf(this, val, byteOffset, encoding, false)
  }
  
  function hexWrite (buf, string, offset, length) {
    offset = Number(offset) || 0
    var remaining = buf.length - offset
    if (!length) {
      length = remaining
    } else {
      length = Number(length)
      if (length > remaining) {
        length = remaining
      }
    }
  
    var strLen = string.length
  
    if (length > strLen / 2) {
      length = strLen / 2
    }
    for (var i = 0; i < length; ++i) {
      var parsed = parseInt(string.substr(i * 2, 2), 16)
      if (numberIsNaN(parsed)) return i
      buf[offset + i] = parsed
    }
    return i
  }
  
  function utf8Write (buf, string, offset, length) {
    return blitBuffer(utf8ToBytes(string, buf.length - offset), buf, offset, length)
  }
  
  function asciiWrite (buf, string, offset, length) {
    return blitBuffer(asciiToBytes(string), buf, offset, length)
  }
  
  function latin1Write (buf, string, offset, length) {
    return asciiWrite(buf, string, offset, length)
  }
  
  function base64Write (buf, string, offset, length) {
    return blitBuffer(base64ToBytes(string), buf, offset, length)
  }
  
  function ucs2Write (buf, string, offset, length) {
    return blitBuffer(utf16leToBytes(string, buf.length - offset), buf, offset, length)
  }
  
  Buffer.prototype.write = function write (string, offset, length, encoding) {
    // Buffer#write(string)
    if (offset === undefined) {
      encoding = 'utf8'
      length = this.length
      offset = 0
    // Buffer#write(string, encoding)
    } else if (length === undefined && typeof offset === 'string') {
      encoding = offset
      length = this.length
      offset = 0
    // Buffer#write(string, offset[, length][, encoding])
    } else if (isFinite(offset)) {
      offset = offset >>> 0
      if (isFinite(length)) {
        length = length >>> 0
        if (encoding === undefined) encoding = 'utf8'
      } else {
        encoding = length
        length = undefined
      }
    } else {
      throw new Error(
        'Buffer.write(string, encoding, offset[, length]) is no longer supported'
      )
    }
  
    var remaining = this.length - offset
    if (length === undefined || length > remaining) length = remaining
  
    if ((string.length > 0 && (length < 0 || offset < 0)) || offset > this.length) {
      throw new RangeError('Attempt to write outside buffer bounds')
    }
  
    if (!encoding) encoding = 'utf8'
  
    var loweredCase = false
    for (;;) {
      switch (encoding) {
        case 'hex':
          return hexWrite(this, string, offset, length)
  
        case 'utf8':
        case 'utf-8':
          return utf8Write(this, string, offset, length)
  
        case 'ascii':
          return asciiWrite(this, string, offset, length)
  
        case 'latin1':
        case 'binary':
          return latin1Write(this, string, offset, length)
  
        case 'base64':
          // Warning: maxLength not taken into account in base64Write
          return base64Write(this, string, offset, length)
  
        case 'ucs2':
        case 'ucs-2':
        case 'utf16le':
        case 'utf-16le':
          return ucs2Write(this, string, offset, length)
  
        default:
          if (loweredCase) throw new TypeError('Unknown encoding: ' + encoding)
          encoding = ('' + encoding).toLowerCase()
          loweredCase = true
      }
    }
  }
  
  Buffer.prototype.toJSON = function toJSON () {
    return {
      type: 'Buffer',
      data: Array.prototype.slice.call(this._arr || this, 0)
    }
  }
  
  function base64Slice (buf, start, end) {
    if (start === 0 && end === buf.length) {
      return base64.fromByteArray(buf)
    } else {
      return base64.fromByteArray(buf.slice(start, end))
    }
  }
  
  function utf8Slice (buf, start, end) {
    end = Math.min(buf.length, end)
    var res = []
  
    var i = start
    while (i < end) {
      var firstByte = buf[i]
      var codePoint = null
      var bytesPerSequence = (firstByte > 0xEF) ? 4
        : (firstByte > 0xDF) ? 3
          : (firstByte > 0xBF) ? 2
            : 1
  
      if (i + bytesPerSequence <= end) {
        var secondByte, thirdByte, fourthByte, tempCodePoint
  
        switch (bytesPerSequence) {
          case 1:
            if (firstByte < 0x80) {
              codePoint = firstByte
            }
            break
          case 2:
            secondByte = buf[i + 1]
            if ((secondByte & 0xC0) === 0x80) {
              tempCodePoint = (firstByte & 0x1F) << 0x6 | (secondByte & 0x3F)
              if (tempCodePoint > 0x7F) {
                codePoint = tempCodePoint
              }
            }
            break
          case 3:
            secondByte = buf[i + 1]
            thirdByte = buf[i + 2]
            if ((secondByte & 0xC0) === 0x80 && (thirdByte & 0xC0) === 0x80) {
              tempCodePoint = (firstByte & 0xF) << 0xC | (secondByte & 0x3F) << 0x6 | (thirdByte & 0x3F)
              if (tempCodePoint > 0x7FF && (tempCodePoint < 0xD800 || tempCodePoint > 0xDFFF)) {
                codePoint = tempCodePoint
              }
            }
            break
          case 4:
            secondByte = buf[i + 1]
            thirdByte = buf[i + 2]
            fourthByte = buf[i + 3]
            if ((secondByte & 0xC0) === 0x80 && (thirdByte & 0xC0) === 0x80 && (fourthByte & 0xC0) === 0x80) {
              tempCodePoint = (firstByte & 0xF) << 0x12 | (secondByte & 0x3F) << 0xC | (thirdByte & 0x3F) << 0x6 | (fourthByte & 0x3F)
              if (tempCodePoint > 0xFFFF && tempCodePoint < 0x110000) {
                codePoint = tempCodePoint
              }
            }
        }
      }
  
      if (codePoint === null) {
        // we did not generate a valid codePoint so insert a
        // replacement char (U+FFFD) and advance only 1 byte
        codePoint = 0xFFFD
        bytesPerSequence = 1
      } else if (codePoint > 0xFFFF) {
        // encode to utf16 (surrogate pair dance)
        codePoint -= 0x10000
        res.push(codePoint >>> 10 & 0x3FF | 0xD800)
        codePoint = 0xDC00 | codePoint & 0x3FF
      }
  
      res.push(codePoint)
      i += bytesPerSequence
    }
  
    return decodeCodePointsArray(res)
  }
  
  // Based on http://stackoverflow.com/a/22747272/680742, the browser with
  // the lowest limit is Chrome, with 0x10000 args.
  // We go 1 magnitude less, for safety
  var MAX_ARGUMENTS_LENGTH = 0x1000
  
  function decodeCodePointsArray (codePoints) {
    var len = codePoints.length
    if (len <= MAX_ARGUMENTS_LENGTH) {
      return String.fromCharCode.apply(String, codePoints) // avoid extra slice()
    }
  
    // Decode in chunks to avoid "call stack size exceeded".
    var res = ''
    var i = 0
    while (i < len) {
      res += String.fromCharCode.apply(
        String,
        codePoints.slice(i, i += MAX_ARGUMENTS_LENGTH)
      )
    }
    return res
  }
  
  function asciiSlice (buf, start, end) {
    var ret = ''
    end = Math.min(buf.length, end)
  
    for (var i = start; i < end; ++i) {
      ret += String.fromCharCode(buf[i] & 0x7F)
    }
    return ret
  }
  
  function latin1Slice (buf, start, end) {
    var ret = ''
    end = Math.min(buf.length, end)
  
    for (var i = start; i < end; ++i) {
      ret += String.fromCharCode(buf[i])
    }
    return ret
  }
  
  function hexSlice (buf, start, end) {
    var len = buf.length
  
    if (!start || start < 0) start = 0
    if (!end || end < 0 || end > len) end = len
  
    var out = ''
    for (var i = start; i < end; ++i) {
      out += toHex(buf[i])
    }
    return out
  }
  
  function utf16leSlice (buf, start, end) {
    var bytes = buf.slice(start, end)
    var res = ''
    for (var i = 0; i < bytes.length; i += 2) {
      res += String.fromCharCode(bytes[i] + (bytes[i + 1] * 256))
    }
    return res
  }
  
  Buffer.prototype.slice = function slice (start, end) {
    var len = this.length
    start = ~~start
    end = end === undefined ? len : ~~end
  
    if (start < 0) {
      start += len
      if (start < 0) start = 0
    } else if (start > len) {
      start = len
    }
  
    if (end < 0) {
      end += len
      if (end < 0) end = 0
    } else if (end > len) {
      end = len
    }
  
    if (end < start) end = start
  
    var newBuf = this.subarray(start, end)
    // Return an augmented `Uint8Array` instance
    newBuf.__proto__ = Buffer.prototype
    return newBuf
  }
  
  /*
   * Need to make sure that buffer isn't trying to write out of bounds.
   */
  function checkOffset (offset, ext, length) {
    if ((offset % 1) !== 0 || offset < 0) throw new RangeError('offset is not uint')
    if (offset + ext > length) throw new RangeError('Trying to access beyond buffer length')
  }
  
  Buffer.prototype.readUIntLE = function readUIntLE (offset, byteLength, noAssert) {
    offset = offset >>> 0
    byteLength = byteLength >>> 0
    if (!noAssert) checkOffset(offset, byteLength, this.length)
  
    var val = this[offset]
    var mul = 1
    var i = 0
    while (++i < byteLength && (mul *= 0x100)) {
      val += this[offset + i] * mul
    }
  
    return val
  }
  
  Buffer.prototype.readUIntBE = function readUIntBE (offset, byteLength, noAssert) {
    offset = offset >>> 0
    byteLength = byteLength >>> 0
    if (!noAssert) {
      checkOffset(offset, byteLength, this.length)
    }
  
    var val = this[offset + --byteLength]
    var mul = 1
    while (byteLength > 0 && (mul *= 0x100)) {
      val += this[offset + --byteLength] * mul
    }
  
    return val
  }
  
  Buffer.prototype.readUInt8 = function readUInt8 (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 1, this.length)
    return this[offset]
  }
  
  Buffer.prototype.readUInt16LE = function readUInt16LE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 2, this.length)
    return this[offset] | (this[offset + 1] << 8)
  }
  
  Buffer.prototype.readUInt16BE = function readUInt16BE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 2, this.length)
    return (this[offset] << 8) | this[offset + 1]
  }
  
  Buffer.prototype.readUInt32LE = function readUInt32LE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 4, this.length)
  
    return ((this[offset]) |
        (this[offset + 1] << 8) |
        (this[offset + 2] << 16)) +
        (this[offset + 3] * 0x1000000)
  }
  
  Buffer.prototype.readUInt32BE = function readUInt32BE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 4, this.length)
  
    return (this[offset] * 0x1000000) +
      ((this[offset + 1] << 16) |
      (this[offset + 2] << 8) |
      this[offset + 3])
  }
  
  Buffer.prototype.readIntLE = function readIntLE (offset, byteLength, noAssert) {
    offset = offset >>> 0
    byteLength = byteLength >>> 0
    if (!noAssert) checkOffset(offset, byteLength, this.length)
  
    var val = this[offset]
    var mul = 1
    var i = 0
    while (++i < byteLength && (mul *= 0x100)) {
      val += this[offset + i] * mul
    }
    mul *= 0x80
  
    if (val >= mul) val -= Math.pow(2, 8 * byteLength)
  
    return val
  }
  
  Buffer.prototype.readIntBE = function readIntBE (offset, byteLength, noAssert) {
    offset = offset >>> 0
    byteLength = byteLength >>> 0
    if (!noAssert) checkOffset(offset, byteLength, this.length)
  
    var i = byteLength
    var mul = 1
    var val = this[offset + --i]
    while (i > 0 && (mul *= 0x100)) {
      val += this[offset + --i] * mul
    }
    mul *= 0x80
  
    if (val >= mul) val -= Math.pow(2, 8 * byteLength)
  
    return val
  }
  
  Buffer.prototype.readInt8 = function readInt8 (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 1, this.length)
    if (!(this[offset] & 0x80)) return (this[offset])
    return ((0xff - this[offset] + 1) * -1)
  }
  
  Buffer.prototype.readInt16LE = function readInt16LE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 2, this.length)
    var val = this[offset] | (this[offset + 1] << 8)
    return (val & 0x8000) ? val | 0xFFFF0000 : val
  }
  
  Buffer.prototype.readInt16BE = function readInt16BE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 2, this.length)
    var val = this[offset + 1] | (this[offset] << 8)
    return (val & 0x8000) ? val | 0xFFFF0000 : val
  }
  
  Buffer.prototype.readInt32LE = function readInt32LE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 4, this.length)
  
    return (this[offset]) |
      (this[offset + 1] << 8) |
      (this[offset + 2] << 16) |
      (this[offset + 3] << 24)
  }
  
  Buffer.prototype.readInt32BE = function readInt32BE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 4, this.length)
  
    return (this[offset] << 24) |
      (this[offset + 1] << 16) |
      (this[offset + 2] << 8) |
      (this[offset + 3])
  }
  
  Buffer.prototype.readFloatLE = function readFloatLE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 4, this.length)
    return ieee754.read(this, offset, true, 23, 4)
  }
  
  Buffer.prototype.readFloatBE = function readFloatBE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 4, this.length)
    return ieee754.read(this, offset, false, 23, 4)
  }
  
  Buffer.prototype.readDoubleLE = function readDoubleLE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 8, this.length)
    return ieee754.read(this, offset, true, 52, 8)
  }
  
  Buffer.prototype.readDoubleBE = function readDoubleBE (offset, noAssert) {
    offset = offset >>> 0
    if (!noAssert) checkOffset(offset, 8, this.length)
    return ieee754.read(this, offset, false, 52, 8)
  }
  
  function checkInt (buf, value, offset, ext, max, min) {
    if (!Buffer.isBuffer(buf)) throw new TypeError('"buffer" argument must be a Buffer instance')
    if (value > max || value < min) throw new RangeError('"value" argument is out of bounds')
    if (offset + ext > buf.length) throw new RangeError('Index out of range')
  }
  
  Buffer.prototype.writeUIntLE = function writeUIntLE (value, offset, byteLength, noAssert) {
    value = +value
    offset = offset >>> 0
    byteLength = byteLength >>> 0
    if (!noAssert) {
      var maxBytes = Math.pow(2, 8 * byteLength) - 1
      checkInt(this, value, offset, byteLength, maxBytes, 0)
    }
  
    var mul = 1
    var i = 0
    this[offset] = value & 0xFF
    while (++i < byteLength && (mul *= 0x100)) {
      this[offset + i] = (value / mul) & 0xFF
    }
  
    return offset + byteLength
  }
  
  Buffer.prototype.writeUIntBE = function writeUIntBE (value, offset, byteLength, noAssert) {
    value = +value
    offset = offset >>> 0
    byteLength = byteLength >>> 0
    if (!noAssert) {
      var maxBytes = Math.pow(2, 8 * byteLength) - 1
      checkInt(this, value, offset, byteLength, maxBytes, 0)
    }
  
    var i = byteLength - 1
    var mul = 1
    this[offset + i] = value & 0xFF
    while (--i >= 0 && (mul *= 0x100)) {
      this[offset + i] = (value / mul) & 0xFF
    }
  
    return offset + byteLength
  }
  
  Buffer.prototype.writeUInt8 = function writeUInt8 (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 1, 0xff, 0)
    this[offset] = (value & 0xff)
    return offset + 1
  }
  
  Buffer.prototype.writeUInt16LE = function writeUInt16LE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 2, 0xffff, 0)
    this[offset] = (value & 0xff)
    this[offset + 1] = (value >>> 8)
    return offset + 2
  }
  
  Buffer.prototype.writeUInt16BE = function writeUInt16BE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 2, 0xffff, 0)
    this[offset] = (value >>> 8)
    this[offset + 1] = (value & 0xff)
    return offset + 2
  }
  
  Buffer.prototype.writeUInt32LE = function writeUInt32LE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 4, 0xffffffff, 0)
    this[offset + 3] = (value >>> 24)
    this[offset + 2] = (value >>> 16)
    this[offset + 1] = (value >>> 8)
    this[offset] = (value & 0xff)
    return offset + 4
  }
  
  Buffer.prototype.writeUInt32BE = function writeUInt32BE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 4, 0xffffffff, 0)
    this[offset] = (value >>> 24)
    this[offset + 1] = (value >>> 16)
    this[offset + 2] = (value >>> 8)
    this[offset + 3] = (value & 0xff)
    return offset + 4
  }
  
  Buffer.prototype.writeIntLE = function writeIntLE (value, offset, byteLength, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) {
      var limit = Math.pow(2, (8 * byteLength) - 1)
  
      checkInt(this, value, offset, byteLength, limit - 1, -limit)
    }
  
    var i = 0
    var mul = 1
    var sub = 0
    this[offset] = value & 0xFF
    while (++i < byteLength && (mul *= 0x100)) {
      if (value < 0 && sub === 0 && this[offset + i - 1] !== 0) {
        sub = 1
      }
      this[offset + i] = ((value / mul) >> 0) - sub & 0xFF
    }
  
    return offset + byteLength
  }
  
  Buffer.prototype.writeIntBE = function writeIntBE (value, offset, byteLength, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) {
      var limit = Math.pow(2, (8 * byteLength) - 1)
  
      checkInt(this, value, offset, byteLength, limit - 1, -limit)
    }
  
    var i = byteLength - 1
    var mul = 1
    var sub = 0
    this[offset + i] = value & 0xFF
    while (--i >= 0 && (mul *= 0x100)) {
      if (value < 0 && sub === 0 && this[offset + i + 1] !== 0) {
        sub = 1
      }
      this[offset + i] = ((value / mul) >> 0) - sub & 0xFF
    }
  
    return offset + byteLength
  }
  
  Buffer.prototype.writeInt8 = function writeInt8 (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 1, 0x7f, -0x80)
    if (value < 0) value = 0xff + value + 1
    this[offset] = (value & 0xff)
    return offset + 1
  }
  
  Buffer.prototype.writeInt16LE = function writeInt16LE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 2, 0x7fff, -0x8000)
    this[offset] = (value & 0xff)
    this[offset + 1] = (value >>> 8)
    return offset + 2
  }
  
  Buffer.prototype.writeInt16BE = function writeInt16BE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 2, 0x7fff, -0x8000)
    this[offset] = (value >>> 8)
    this[offset + 1] = (value & 0xff)
    return offset + 2
  }
  
  Buffer.prototype.writeInt32LE = function writeInt32LE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000)
    this[offset] = (value & 0xff)
    this[offset + 1] = (value >>> 8)
    this[offset + 2] = (value >>> 16)
    this[offset + 3] = (value >>> 24)
    return offset + 4
  }
  
  Buffer.prototype.writeInt32BE = function writeInt32BE (value, offset, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000)
    if (value < 0) value = 0xffffffff + value + 1
    this[offset] = (value >>> 24)
    this[offset + 1] = (value >>> 16)
    this[offset + 2] = (value >>> 8)
    this[offset + 3] = (value & 0xff)
    return offset + 4
  }
  
  function checkIEEE754 (buf, value, offset, ext, max, min) {
    if (offset + ext > buf.length) throw new RangeError('Index out of range')
    if (offset < 0) throw new RangeError('Index out of range')
  }
  
  function writeFloat (buf, value, offset, littleEndian, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) {
      checkIEEE754(buf, value, offset, 4, 3.4028234663852886e+38, -3.4028234663852886e+38)
    }
    ieee754.write(buf, value, offset, littleEndian, 23, 4)
    return offset + 4
  }
  
  Buffer.prototype.writeFloatLE = function writeFloatLE (value, offset, noAssert) {
    return writeFloat(this, value, offset, true, noAssert)
  }
  
  Buffer.prototype.writeFloatBE = function writeFloatBE (value, offset, noAssert) {
    return writeFloat(this, value, offset, false, noAssert)
  }
  
  function writeDouble (buf, value, offset, littleEndian, noAssert) {
    value = +value
    offset = offset >>> 0
    if (!noAssert) {
      checkIEEE754(buf, value, offset, 8, 1.7976931348623157E+308, -1.7976931348623157E+308)
    }
    ieee754.write(buf, value, offset, littleEndian, 52, 8)
    return offset + 8
  }
  
  Buffer.prototype.writeDoubleLE = function writeDoubleLE (value, offset, noAssert) {
    return writeDouble(this, value, offset, true, noAssert)
  }
  
  Buffer.prototype.writeDoubleBE = function writeDoubleBE (value, offset, noAssert) {
    return writeDouble(this, value, offset, false, noAssert)
  }
  
  // copy(targetBuffer, targetStart=0, sourceStart=0, sourceEnd=buffer.length)
  Buffer.prototype.copy = function copy (target, targetStart, start, end) {
    if (!Buffer.isBuffer(target)) throw new TypeError('argument should be a Buffer')
    if (!start) start = 0
    if (!end && end !== 0) end = this.length
    if (targetStart >= target.length) targetStart = target.length
    if (!targetStart) targetStart = 0
    if (end > 0 && end < start) end = start
  
    // Copy 0 bytes; we're done
    if (end === start) return 0
    if (target.length === 0 || this.length === 0) return 0
  
    // Fatal error conditions
    if (targetStart < 0) {
      throw new RangeError('targetStart out of bounds')
    }
    if (start < 0 || start >= this.length) throw new RangeError('Index out of range')
    if (end < 0) throw new RangeError('sourceEnd out of bounds')
  
    // Are we oob?
    if (end > this.length) end = this.length
    if (target.length - targetStart < end - start) {
      end = target.length - targetStart + start
    }
  
    var len = end - start
  
    if (this === target && typeof Uint8Array.prototype.copyWithin === 'function') {
      // Use built-in when available, missing from IE11
      this.copyWithin(targetStart, start, end)
    } else if (this === target && start < targetStart && targetStart < end) {
      // descending copy from end
      for (var i = len - 1; i >= 0; --i) {
        target[i + targetStart] = this[i + start]
      }
    } else {
      Uint8Array.prototype.set.call(
        target,
        this.subarray(start, end),
        targetStart
      )
    }
  
    return len
  }
  
  // Usage:
  //    buffer.fill(number[, offset[, end]])
  //    buffer.fill(buffer[, offset[, end]])
  //    buffer.fill(string[, offset[, end]][, encoding])
  Buffer.prototype.fill = function fill (val, start, end, encoding) {
    // Handle string cases:
    if (typeof val === 'string') {
      if (typeof start === 'string') {
        encoding = start
        start = 0
        end = this.length
      } else if (typeof end === 'string') {
        encoding = end
        end = this.length
      }
      if (encoding !== undefined && typeof encoding !== 'string') {
        throw new TypeError('encoding must be a string')
      }
      if (typeof encoding === 'string' && !Buffer.isEncoding(encoding)) {
        throw new TypeError('Unknown encoding: ' + encoding)
      }
      if (val.length === 1) {
        var code = val.charCodeAt(0)
        if ((encoding === 'utf8' && code < 128) ||
            encoding === 'latin1') {
          // Fast path: If `val` fits into a single byte, use that numeric value.
          val = code
        }
      }
    } else if (typeof val === 'number') {
      val = val & 255
    }
  
    // Invalid ranges are not set to a default, so can range check early.
    if (start < 0 || this.length < start || this.length < end) {
      throw new RangeError('Out of range index')
    }
  
    if (end <= start) {
      return this
    }
  
    start = start >>> 0
    end = end === undefined ? this.length : end >>> 0
  
    if (!val) val = 0
  
    var i
    if (typeof val === 'number') {
      for (i = start; i < end; ++i) {
        this[i] = val
      }
    } else {
      var bytes = Buffer.isBuffer(val)
        ? val
        : Buffer.from(val, encoding)
      var len = bytes.length
      if (len === 0) {
        throw new TypeError('The value "' + val +
          '" is invalid for argument "value"')
      }
      for (i = 0; i < end - start; ++i) {
        this[i + start] = bytes[i % len]
      }
    }
  
    return this
  }
  
  // HELPER FUNCTIONS
  // ================
  
  var INVALID_BASE64_RE = /[^+/0-9A-Za-z-_]/g
  
  function base64clean (str) {
    // Node takes equal signs as end of the Base64 encoding
    str = str.split('=')[0]
    // Node strips out invalid characters like \n and \t from the string, base64-js does not
    str = str.trim().replace(INVALID_BASE64_RE, '')
    // Node converts strings with length < 2 to ''
    if (str.length < 2) return ''
    // Node allows for non-padded base64 strings (missing trailing ===), base64-js does not
    while (str.length % 4 !== 0) {
      str = str + '='
    }
    return str
  }
  
  function toHex (n) {
    if (n < 16) return '0' + n.toString(16)
    return n.toString(16)
  }
  
  function utf8ToBytes (string, units) {
    units = units || Infinity
    var codePoint
    var length = string.length
    var leadSurrogate = null
    var bytes = []
  
    for (var i = 0; i < length; ++i) {
      codePoint = string.charCodeAt(i)
  
      // is surrogate component
      if (codePoint > 0xD7FF && codePoint < 0xE000) {
        // last char was a lead
        if (!leadSurrogate) {
          // no lead yet
          if (codePoint > 0xDBFF) {
            // unexpected trail
            if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
            continue
          } else if (i + 1 === length) {
            // unpaired lead
            if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
            continue
          }
  
          // valid lead
          leadSurrogate = codePoint
  
          continue
        }
  
        // 2 leads in a row
        if (codePoint < 0xDC00) {
          if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
          leadSurrogate = codePoint
          continue
        }
  
        // valid surrogate pair
        codePoint = (leadSurrogate - 0xD800 << 10 | codePoint - 0xDC00) + 0x10000
      } else if (leadSurrogate) {
        // valid bmp char, but last char was a lead
        if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
      }
  
      leadSurrogate = null
  
      // encode utf8
      if (codePoint < 0x80) {
        if ((units -= 1) < 0) break
        bytes.push(codePoint)
      } else if (codePoint < 0x800) {
        if ((units -= 2) < 0) break
        bytes.push(
          codePoint >> 0x6 | 0xC0,
          codePoint & 0x3F | 0x80
        )
      } else if (codePoint < 0x10000) {
        if ((units -= 3) < 0) break
        bytes.push(
          codePoint >> 0xC | 0xE0,
          codePoint >> 0x6 & 0x3F | 0x80,
          codePoint & 0x3F | 0x80
        )
      } else if (codePoint < 0x110000) {
        if ((units -= 4) < 0) break
        bytes.push(
          codePoint >> 0x12 | 0xF0,
          codePoint >> 0xC & 0x3F | 0x80,
          codePoint >> 0x6 & 0x3F | 0x80,
          codePoint & 0x3F | 0x80
        )
      } else {
        throw new Error('Invalid code point')
      }
    }
  
    return bytes
  }
  
  function asciiToBytes (str) {
    var byteArray = []
    for (var i = 0; i < str.length; ++i) {
      // Node's code seems to be doing this and not & 0x7F..
      byteArray.push(str.charCodeAt(i) & 0xFF)
    }
    return byteArray
  }
  
  function utf16leToBytes (str, units) {
    var c, hi, lo
    var byteArray = []
    for (var i = 0; i < str.length; ++i) {
      if ((units -= 2) < 0) break
  
      c = str.charCodeAt(i)
      hi = c >> 8
      lo = c % 256
      byteArray.push(lo)
      byteArray.push(hi)
    }
  
    return byteArray
  }
  
  function base64ToBytes (str) {
    return base64.toByteArray(base64clean(str))
  }
  
  function blitBuffer (src, dst, offset, length) {
    for (var i = 0; i < length; ++i) {
      if ((i + offset >= dst.length) || (i >= src.length)) break
      dst[i + offset] = src[i]
    }
    return i
  }
  
  // ArrayBuffer or Uint8Array objects from other contexts (i.e. iframes) do not pass
  // the `instanceof` check but they should be treated as of that type.
  // See: https://github.com/feross/buffer/issues/166
  function isInstance (obj, type) {
    return obj instanceof type ||
      (obj != null && obj.constructor != null && obj.constructor.name != null &&
        obj.constructor.name === type.name)
  }
  function numberIsNaN (obj) {
    // For IE11 support
    return obj !== obj // eslint-disable-line no-self-compare
  }
  
  }).call(this,require("buffer").Buffer)
  },{"base64-js":5,"buffer":9,"ieee754":14}],10:[function(require,module,exports){
  module.exports = {
    "100": "Continue",
    "101": "Switching Protocols",
    "102": "Processing",
    "200": "OK",
    "201": "Created",
    "202": "Accepted",
    "203": "Non-Authoritative Information",
    "204": "No Content",
    "205": "Reset Content",
    "206": "Partial Content",
    "207": "Multi-Status",
    "208": "Already Reported",
    "226": "IM Used",
    "300": "Multiple Choices",
    "301": "Moved Permanently",
    "302": "Found",
    "303": "See Other",
    "304": "Not Modified",
    "305": "Use Proxy",
    "307": "Temporary Redirect",
    "308": "Permanent Redirect",
    "400": "Bad Request",
    "401": "Unauthorized",
    "402": "Payment Required",
    "403": "Forbidden",
    "404": "Not Found",
    "405": "Method Not Allowed",
    "406": "Not Acceptable",
    "407": "Proxy Authentication Required",
    "408": "Request Timeout",
    "409": "Conflict",
    "410": "Gone",
    "411": "Length Required",
    "412": "Precondition Failed",
    "413": "Payload Too Large",
    "414": "URI Too Long",
    "415": "Unsupported Media Type",
    "416": "Range Not Satisfiable",
    "417": "Expectation Failed",
    "418": "I'm a teapot",
    "421": "Misdirected Request",
    "422": "Unprocessable Entity",
    "423": "Locked",
    "424": "Failed Dependency",
    "425": "Unordered Collection",
    "426": "Upgrade Required",
    "428": "Precondition Required",
    "429": "Too Many Requests",
    "431": "Request Header Fields Too Large",
    "451": "Unavailable For Legal Reasons",
    "500": "Internal Server Error",
    "501": "Not Implemented",
    "502": "Bad Gateway",
    "503": "Service Unavailable",
    "504": "Gateway Timeout",
    "505": "HTTP Version Not Supported",
    "506": "Variant Also Negotiates",
    "507": "Insufficient Storage",
    "508": "Loop Detected",
    "509": "Bandwidth Limit Exceeded",
    "510": "Not Extended",
    "511": "Network Authentication Required"
  }
  
  },{}],11:[function(require,module,exports){
  (function (Buffer){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  // NOTE: These type checking functions intentionally don't use `instanceof`
  // because it is fragile and can be easily faked with `Object.create()`.
  
  function isArray(arg) {
    if (Array.isArray) {
      return Array.isArray(arg);
    }
    return objectToString(arg) === '[object Array]';
  }
  exports.isArray = isArray;
  
  function isBoolean(arg) {
    return typeof arg === 'boolean';
  }
  exports.isBoolean = isBoolean;
  
  function isNull(arg) {
    return arg === null;
  }
  exports.isNull = isNull;
  
  function isNullOrUndefined(arg) {
    return arg == null;
  }
  exports.isNullOrUndefined = isNullOrUndefined;
  
  function isNumber(arg) {
    return typeof arg === 'number';
  }
  exports.isNumber = isNumber;
  
  function isString(arg) {
    return typeof arg === 'string';
  }
  exports.isString = isString;
  
  function isSymbol(arg) {
    return typeof arg === 'symbol';
  }
  exports.isSymbol = isSymbol;
  
  function isUndefined(arg) {
    return arg === void 0;
  }
  exports.isUndefined = isUndefined;
  
  function isRegExp(re) {
    return objectToString(re) === '[object RegExp]';
  }
  exports.isRegExp = isRegExp;
  
  function isObject(arg) {
    return typeof arg === 'object' && arg !== null;
  }
  exports.isObject = isObject;
  
  function isDate(d) {
    return objectToString(d) === '[object Date]';
  }
  exports.isDate = isDate;
  
  function isError(e) {
    return (objectToString(e) === '[object Error]' || e instanceof Error);
  }
  exports.isError = isError;
  
  function isFunction(arg) {
    return typeof arg === 'function';
  }
  exports.isFunction = isFunction;
  
  function isPrimitive(arg) {
    return arg === null ||
           typeof arg === 'boolean' ||
           typeof arg === 'number' ||
           typeof arg === 'string' ||
           typeof arg === 'symbol' ||  // ES6 symbol
           typeof arg === 'undefined';
  }
  exports.isPrimitive = isPrimitive;
  
  exports.isBuffer = Buffer.isBuffer;
  
  function objectToString(o) {
    return Object.prototype.toString.call(o);
  }
  
  }).call(this,{"isBuffer":require("../../is-buffer/index.js")})
  },{"../../is-buffer/index.js":16}],12:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  var objectCreate = Object.create || objectCreatePolyfill
  var objectKeys = Object.keys || objectKeysPolyfill
  var bind = Function.prototype.bind || functionBindPolyfill
  
  function EventEmitter() {
    if (!this._events || !Object.prototype.hasOwnProperty.call(this, '_events')) {
      this._events = objectCreate(null);
      this._eventsCount = 0;
    }
  
    this._maxListeners = this._maxListeners || undefined;
  }
  module.exports = EventEmitter;
  
  // Backwards-compat with node 0.10.x
  EventEmitter.EventEmitter = EventEmitter;
  
  EventEmitter.prototype._events = undefined;
  EventEmitter.prototype._maxListeners = undefined;
  
  // By default EventEmitters will print a warning if more than 10 listeners are
  // added to it. This is a useful default which helps finding memory leaks.
  var defaultMaxListeners = 10;
  
  var hasDefineProperty;
  try {
    var o = {};
    if (Object.defineProperty) Object.defineProperty(o, 'x', { value: 0 });
    hasDefineProperty = o.x === 0;
  } catch (err) { hasDefineProperty = false }
  if (hasDefineProperty) {
    Object.defineProperty(EventEmitter, 'defaultMaxListeners', {
      enumerable: true,
      get: function() {
        return defaultMaxListeners;
      },
      set: function(arg) {
        // check whether the input is a positive number (whose value is zero or
        // greater and not a NaN).
        if (typeof arg !== 'number' || arg < 0 || arg !== arg)
          throw new TypeError('"defaultMaxListeners" must be a positive number');
        defaultMaxListeners = arg;
      }
    });
  } else {
    EventEmitter.defaultMaxListeners = defaultMaxListeners;
  }
  
  // Obviously not all Emitters should be limited to 10. This function allows
  // that to be increased. Set to zero for unlimited.
  EventEmitter.prototype.setMaxListeners = function setMaxListeners(n) {
    if (typeof n !== 'number' || n < 0 || isNaN(n))
      throw new TypeError('"n" argument must be a positive number');
    this._maxListeners = n;
    return this;
  };
  
  function $getMaxListeners(that) {
    if (that._maxListeners === undefined)
      return EventEmitter.defaultMaxListeners;
    return that._maxListeners;
  }
  
  EventEmitter.prototype.getMaxListeners = function getMaxListeners() {
    return $getMaxListeners(this);
  };
  
  // These standalone emit* functions are used to optimize calling of event
  // handlers for fast cases because emit() itself often has a variable number of
  // arguments and can be deoptimized because of that. These functions always have
  // the same number of arguments and thus do not get deoptimized, so the code
  // inside them can execute faster.
  function emitNone(handler, isFn, self) {
    if (isFn)
      handler.call(self);
    else {
      var len = handler.length;
      var listeners = arrayClone(handler, len);
      for (var i = 0; i < len; ++i)
        listeners[i].call(self);
    }
  }
  function emitOne(handler, isFn, self, arg1) {
    if (isFn)
      handler.call(self, arg1);
    else {
      var len = handler.length;
      var listeners = arrayClone(handler, len);
      for (var i = 0; i < len; ++i)
        listeners[i].call(self, arg1);
    }
  }
  function emitTwo(handler, isFn, self, arg1, arg2) {
    if (isFn)
      handler.call(self, arg1, arg2);
    else {
      var len = handler.length;
      var listeners = arrayClone(handler, len);
      for (var i = 0; i < len; ++i)
        listeners[i].call(self, arg1, arg2);
    }
  }
  function emitThree(handler, isFn, self, arg1, arg2, arg3) {
    if (isFn)
      handler.call(self, arg1, arg2, arg3);
    else {
      var len = handler.length;
      var listeners = arrayClone(handler, len);
      for (var i = 0; i < len; ++i)
        listeners[i].call(self, arg1, arg2, arg3);
    }
  }
  
  function emitMany(handler, isFn, self, args) {
    if (isFn)
      handler.apply(self, args);
    else {
      var len = handler.length;
      var listeners = arrayClone(handler, len);
      for (var i = 0; i < len; ++i)
        listeners[i].apply(self, args);
    }
  }
  
  EventEmitter.prototype.emit = function emit(type) {
    var er, handler, len, args, i, events;
    var doError = (type === 'error');
  
    events = this._events;
    if (events)
      doError = (doError && events.error == null);
    else if (!doError)
      return false;
  
    // If there is no 'error' event listener then throw.
    if (doError) {
      if (arguments.length > 1)
        er = arguments[1];
      if (er instanceof Error) {
        throw er; // Unhandled 'error' event
      } else {
        // At least give some kind of context to the user
        var err = new Error('Unhandled "error" event. (' + er + ')');
        err.context = er;
        throw err;
      }
      return false;
    }
  
    handler = events[type];
  
    if (!handler)
      return false;
  
    var isFn = typeof handler === 'function';
    len = arguments.length;
    switch (len) {
        // fast cases
      case 1:
        emitNone(handler, isFn, this);
        break;
      case 2:
        emitOne(handler, isFn, this, arguments[1]);
        break;
      case 3:
        emitTwo(handler, isFn, this, arguments[1], arguments[2]);
        break;
      case 4:
        emitThree(handler, isFn, this, arguments[1], arguments[2], arguments[3]);
        break;
        // slower
      default:
        args = new Array(len - 1);
        for (i = 1; i < len; i++)
          args[i - 1] = arguments[i];
        emitMany(handler, isFn, this, args);
    }
  
    return true;
  };
  
  function _addListener(target, type, listener, prepend) {
    var m;
    var events;
    var existing;
  
    if (typeof listener !== 'function')
      throw new TypeError('"listener" argument must be a function');
  
    events = target._events;
    if (!events) {
      events = target._events = objectCreate(null);
      target._eventsCount = 0;
    } else {
      // To avoid recursion in the case that type === "newListener"! Before
      // adding it to the listeners, first emit "newListener".
      if (events.newListener) {
        target.emit('newListener', type,
            listener.listener ? listener.listener : listener);
  
        // Re-assign `events` because a newListener handler could have caused the
        // this._events to be assigned to a new object
        events = target._events;
      }
      existing = events[type];
    }
  
    if (!existing) {
      // Optimize the case of one listener. Don't need the extra array object.
      existing = events[type] = listener;
      ++target._eventsCount;
    } else {
      if (typeof existing === 'function') {
        // Adding the second element, need to change to array.
        existing = events[type] =
            prepend ? [listener, existing] : [existing, listener];
      } else {
        // If we've already got an array, just append.
        if (prepend) {
          existing.unshift(listener);
        } else {
          existing.push(listener);
        }
      }
  
      // Check for listener leak
      if (!existing.warned) {
        m = $getMaxListeners(target);
        if (m && m > 0 && existing.length > m) {
          existing.warned = true;
          var w = new Error('Possible EventEmitter memory leak detected. ' +
              existing.length + ' "' + String(type) + '" listeners ' +
              'added. Use emitter.setMaxListeners() to ' +
              'increase limit.');
          w.name = 'MaxListenersExceededWarning';
          w.emitter = target;
          w.type = type;
          w.count = existing.length;
          if (typeof console === 'object' && console.warn) {
            console.warn('%s: %s', w.name, w.message);
          }
        }
      }
    }
  
    return target;
  }
  
  EventEmitter.prototype.addListener = function addListener(type, listener) {
    return _addListener(this, type, listener, false);
  };
  
  EventEmitter.prototype.on = EventEmitter.prototype.addListener;
  
  EventEmitter.prototype.prependListener =
      function prependListener(type, listener) {
        return _addListener(this, type, listener, true);
      };
  
  function onceWrapper() {
    if (!this.fired) {
      this.target.removeListener(this.type, this.wrapFn);
      this.fired = true;
      switch (arguments.length) {
        case 0:
          return this.listener.call(this.target);
        case 1:
          return this.listener.call(this.target, arguments[0]);
        case 2:
          return this.listener.call(this.target, arguments[0], arguments[1]);
        case 3:
          return this.listener.call(this.target, arguments[0], arguments[1],
              arguments[2]);
        default:
          var args = new Array(arguments.length);
          for (var i = 0; i < args.length; ++i)
            args[i] = arguments[i];
          this.listener.apply(this.target, args);
      }
    }
  }
  
  function _onceWrap(target, type, listener) {
    var state = { fired: false, wrapFn: undefined, target: target, type: type, listener: listener };
    var wrapped = bind.call(onceWrapper, state);
    wrapped.listener = listener;
    state.wrapFn = wrapped;
    return wrapped;
  }
  
  EventEmitter.prototype.once = function once(type, listener) {
    if (typeof listener !== 'function')
      throw new TypeError('"listener" argument must be a function');
    this.on(type, _onceWrap(this, type, listener));
    return this;
  };
  
  EventEmitter.prototype.prependOnceListener =
      function prependOnceListener(type, listener) {
        if (typeof listener !== 'function')
          throw new TypeError('"listener" argument must be a function');
        this.prependListener(type, _onceWrap(this, type, listener));
        return this;
      };
  
  // Emits a 'removeListener' event if and only if the listener was removed.
  EventEmitter.prototype.removeListener =
      function removeListener(type, listener) {
        var list, events, position, i, originalListener;
  
        if (typeof listener !== 'function')
          throw new TypeError('"listener" argument must be a function');
  
        events = this._events;
        if (!events)
          return this;
  
        list = events[type];
        if (!list)
          return this;
  
        if (list === listener || list.listener === listener) {
          if (--this._eventsCount === 0)
            this._events = objectCreate(null);
          else {
            delete events[type];
            if (events.removeListener)
              this.emit('removeListener', type, list.listener || listener);
          }
        } else if (typeof list !== 'function') {
          position = -1;
  
          for (i = list.length - 1; i >= 0; i--) {
            if (list[i] === listener || list[i].listener === listener) {
              originalListener = list[i].listener;
              position = i;
              break;
            }
          }
  
          if (position < 0)
            return this;
  
          if (position === 0)
            list.shift();
          else
            spliceOne(list, position);
  
          if (list.length === 1)
            events[type] = list[0];
  
          if (events.removeListener)
            this.emit('removeListener', type, originalListener || listener);
        }
  
        return this;
      };
  
  EventEmitter.prototype.removeAllListeners =
      function removeAllListeners(type) {
        var listeners, events, i;
  
        events = this._events;
        if (!events)
          return this;
  
        // not listening for removeListener, no need to emit
        if (!events.removeListener) {
          if (arguments.length === 0) {
            this._events = objectCreate(null);
            this._eventsCount = 0;
          } else if (events[type]) {
            if (--this._eventsCount === 0)
              this._events = objectCreate(null);
            else
              delete events[type];
          }
          return this;
        }
  
        // emit removeListener for all listeners on all events
        if (arguments.length === 0) {
          var keys = objectKeys(events);
          var key;
          for (i = 0; i < keys.length; ++i) {
            key = keys[i];
            if (key === 'removeListener') continue;
            this.removeAllListeners(key);
          }
          this.removeAllListeners('removeListener');
          this._events = objectCreate(null);
          this._eventsCount = 0;
          return this;
        }
  
        listeners = events[type];
  
        if (typeof listeners === 'function') {
          this.removeListener(type, listeners);
        } else if (listeners) {
          // LIFO order
          for (i = listeners.length - 1; i >= 0; i--) {
            this.removeListener(type, listeners[i]);
          }
        }
  
        return this;
      };
  
  function _listeners(target, type, unwrap) {
    var events = target._events;
  
    if (!events)
      return [];
  
    var evlistener = events[type];
    if (!evlistener)
      return [];
  
    if (typeof evlistener === 'function')
      return unwrap ? [evlistener.listener || evlistener] : [evlistener];
  
    return unwrap ? unwrapListeners(evlistener) : arrayClone(evlistener, evlistener.length);
  }
  
  EventEmitter.prototype.listeners = function listeners(type) {
    return _listeners(this, type, true);
  };
  
  EventEmitter.prototype.rawListeners = function rawListeners(type) {
    return _listeners(this, type, false);
  };
  
  EventEmitter.listenerCount = function(emitter, type) {
    if (typeof emitter.listenerCount === 'function') {
      return emitter.listenerCount(type);
    } else {
      return listenerCount.call(emitter, type);
    }
  };
  
  EventEmitter.prototype.listenerCount = listenerCount;
  function listenerCount(type) {
    var events = this._events;
  
    if (events) {
      var evlistener = events[type];
  
      if (typeof evlistener === 'function') {
        return 1;
      } else if (evlistener) {
        return evlistener.length;
      }
    }
  
    return 0;
  }
  
  EventEmitter.prototype.eventNames = function eventNames() {
    return this._eventsCount > 0 ? Reflect.ownKeys(this._events) : [];
  };
  
  // About 1.5x faster than the two-arg version of Array#splice().
  function spliceOne(list, index) {
    for (var i = index, k = i + 1, n = list.length; k < n; i += 1, k += 1)
      list[i] = list[k];
    list.pop();
  }
  
  function arrayClone(arr, n) {
    var copy = new Array(n);
    for (var i = 0; i < n; ++i)
      copy[i] = arr[i];
    return copy;
  }
  
  function unwrapListeners(arr) {
    var ret = new Array(arr.length);
    for (var i = 0; i < ret.length; ++i) {
      ret[i] = arr[i].listener || arr[i];
    }
    return ret;
  }
  
  function objectCreatePolyfill(proto) {
    var F = function() {};
    F.prototype = proto;
    return new F;
  }
  function objectKeysPolyfill(obj) {
    var keys = [];
    for (var k in obj) if (Object.prototype.hasOwnProperty.call(obj, k)) {
      keys.push(k);
    }
    return k;
  }
  function functionBindPolyfill(context) {
    var fn = this;
    return function () {
      return fn.apply(context, arguments);
    };
  }
  
  },{}],13:[function(require,module,exports){
  var http = require('http')
  var url = require('url')
  
  var https = module.exports
  
  for (var key in http) {
    if (http.hasOwnProperty(key)) https[key] = http[key]
  }
  
  https.request = function (params, cb) {
    params = validateParams(params)
    return http.request.call(this, params, cb)
  }
  
  https.get = function (params, cb) {
    params = validateParams(params)
    return http.get.call(this, params, cb)
  }
  
  function validateParams (params) {
    if (typeof params === 'string') {
      params = url.parse(params)
    }
    if (!params.protocol) {
      params.protocol = 'https:'
    }
    if (params.protocol !== 'https:') {
      throw new Error('Protocol "' + params.protocol + '" not supported. Expected "https:"')
    }
    return params
  }
  
  },{"http":54,"url":75}],14:[function(require,module,exports){
  exports.read = function (buffer, offset, isLE, mLen, nBytes) {
    var e, m
    var eLen = (nBytes * 8) - mLen - 1
    var eMax = (1 << eLen) - 1
    var eBias = eMax >> 1
    var nBits = -7
    var i = isLE ? (nBytes - 1) : 0
    var d = isLE ? -1 : 1
    var s = buffer[offset + i]
  
    i += d
  
    e = s & ((1 << (-nBits)) - 1)
    s >>= (-nBits)
    nBits += eLen
    for (; nBits > 0; e = (e * 256) + buffer[offset + i], i += d, nBits -= 8) {}
  
    m = e & ((1 << (-nBits)) - 1)
    e >>= (-nBits)
    nBits += mLen
    for (; nBits > 0; m = (m * 256) + buffer[offset + i], i += d, nBits -= 8) {}
  
    if (e === 0) {
      e = 1 - eBias
    } else if (e === eMax) {
      return m ? NaN : ((s ? -1 : 1) * Infinity)
    } else {
      m = m + Math.pow(2, mLen)
      e = e - eBias
    }
    return (s ? -1 : 1) * m * Math.pow(2, e - mLen)
  }
  
  exports.write = function (buffer, value, offset, isLE, mLen, nBytes) {
    var e, m, c
    var eLen = (nBytes * 8) - mLen - 1
    var eMax = (1 << eLen) - 1
    var eBias = eMax >> 1
    var rt = (mLen === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0)
    var i = isLE ? 0 : (nBytes - 1)
    var d = isLE ? 1 : -1
    var s = value < 0 || (value === 0 && 1 / value < 0) ? 1 : 0
  
    value = Math.abs(value)
  
    if (isNaN(value) || value === Infinity) {
      m = isNaN(value) ? 1 : 0
      e = eMax
    } else {
      e = Math.floor(Math.log(value) / Math.LN2)
      if (value * (c = Math.pow(2, -e)) < 1) {
        e--
        c *= 2
      }
      if (e + eBias >= 1) {
        value += rt / c
      } else {
        value += rt * Math.pow(2, 1 - eBias)
      }
      if (value * c >= 2) {
        e++
        c /= 2
      }
  
      if (e + eBias >= eMax) {
        m = 0
        e = eMax
      } else if (e + eBias >= 1) {
        m = ((value * c) - 1) * Math.pow(2, mLen)
        e = e + eBias
      } else {
        m = value * Math.pow(2, eBias - 1) * Math.pow(2, mLen)
        e = 0
      }
    }
  
    for (; mLen >= 8; buffer[offset + i] = m & 0xff, i += d, m /= 256, mLen -= 8) {}
  
    e = (e << mLen) | m
    eLen += mLen
    for (; eLen > 0; buffer[offset + i] = e & 0xff, i += d, e /= 256, eLen -= 8) {}
  
    buffer[offset + i - d] |= s * 128
  }
  
  },{}],15:[function(require,module,exports){
  if (typeof Object.create === 'function') {
    // implementation from standard node.js 'util' module
    module.exports = function inherits(ctor, superCtor) {
      if (superCtor) {
        ctor.super_ = superCtor
        ctor.prototype = Object.create(superCtor.prototype, {
          constructor: {
            value: ctor,
            enumerable: false,
            writable: true,
            configurable: true
          }
        })
      }
    };
  } else {
    // old school shim for old browsers
    module.exports = function inherits(ctor, superCtor) {
      if (superCtor) {
        ctor.super_ = superCtor
        var TempCtor = function () {}
        TempCtor.prototype = superCtor.prototype
        ctor.prototype = new TempCtor()
        ctor.prototype.constructor = ctor
      }
    }
  }
  
  },{}],16:[function(require,module,exports){
  /*!
   * Determine if an object is a Buffer
   *
   * @author   Feross Aboukhadijeh <https://feross.org>
   * @license  MIT
   */
  
  // The _isBuffer check is for Safari 5-7 support, because it's missing
  // Object.prototype.constructor. Remove this eventually
  module.exports = function (obj) {
    return obj != null && (isBuffer(obj) || isSlowBuffer(obj) || !!obj._isBuffer)
  }
  
  function isBuffer (obj) {
    return !!obj.constructor && typeof obj.constructor.isBuffer === 'function' && obj.constructor.isBuffer(obj)
  }
  
  // For Node v0.10 support. Remove this eventually.
  function isSlowBuffer (obj) {
    return typeof obj.readFloatLE === 'function' && typeof obj.slice === 'function' && isBuffer(obj.slice(0, 0))
  }
  
  },{}],17:[function(require,module,exports){
  var toString = {}.toString;
  
  module.exports = Array.isArray || function (arr) {
    return toString.call(arr) == '[object Array]';
  };
  
  },{}],18:[function(require,module,exports){
  /*
  object-assign
  (c) Sindre Sorhus
  @license MIT
  */
  
  'use strict';
  /* eslint-disable no-unused-vars */
  var getOwnPropertySymbols = Object.getOwnPropertySymbols;
  var hasOwnProperty = Object.prototype.hasOwnProperty;
  var propIsEnumerable = Object.prototype.propertyIsEnumerable;
  
  function toObject(val) {
    if (val === null || val === undefined) {
      throw new TypeError('Object.assign cannot be called with null or undefined');
    }
  
    return Object(val);
  }
  
  function shouldUseNative() {
    try {
      if (!Object.assign) {
        return false;
      }
  
      // Detect buggy property enumeration order in older V8 versions.
  
      // https://bugs.chromium.org/p/v8/issues/detail?id=4118
      var test1 = new String('abc');  // eslint-disable-line no-new-wrappers
      test1[5] = 'de';
      if (Object.getOwnPropertyNames(test1)[0] === '5') {
        return false;
      }
  
      // https://bugs.chromium.org/p/v8/issues/detail?id=3056
      var test2 = {};
      for (var i = 0; i < 10; i++) {
        test2['_' + String.fromCharCode(i)] = i;
      }
      var order2 = Object.getOwnPropertyNames(test2).map(function (n) {
        return test2[n];
      });
      if (order2.join('') !== '0123456789') {
        return false;
      }
  
      // https://bugs.chromium.org/p/v8/issues/detail?id=3056
      var test3 = {};
      'abcdefghijklmnopqrst'.split('').forEach(function (letter) {
        test3[letter] = letter;
      });
      if (Object.keys(Object.assign({}, test3)).join('') !==
          'abcdefghijklmnopqrst') {
        return false;
      }
  
      return true;
    } catch (err) {
      // We don't expect any of the above to throw, but better to be safe.
      return false;
    }
  }
  
  module.exports = shouldUseNative() ? Object.assign : function (target, source) {
    var from;
    var to = toObject(target);
    var symbols;
  
    for (var s = 1; s < arguments.length; s++) {
      from = Object(arguments[s]);
  
      for (var key in from) {
        if (hasOwnProperty.call(from, key)) {
          to[key] = from[key];
        }
      }
  
      if (getOwnPropertySymbols) {
        symbols = getOwnPropertySymbols(from);
        for (var i = 0; i < symbols.length; i++) {
          if (propIsEnumerable.call(from, symbols[i])) {
            to[symbols[i]] = from[symbols[i]];
          }
        }
      }
    }
  
    return to;
  };
  
  },{}],19:[function(require,module,exports){
  exports.endianness = function () { return 'LE' };
  
  exports.hostname = function () {
      if (typeof location !== 'undefined') {
          return location.hostname
      }
      else return '';
  };
  
  exports.loadavg = function () { return [] };
  
  exports.uptime = function () { return 0 };
  
  exports.freemem = function () {
      return Number.MAX_VALUE;
  };
  
  exports.totalmem = function () {
      return Number.MAX_VALUE;
  };
  
  exports.cpus = function () { return [] };
  
  exports.type = function () { return 'Browser' };
  
  exports.release = function () {
      if (typeof navigator !== 'undefined') {
          return navigator.appVersion;
      }
      return '';
  };
  
  exports.networkInterfaces
  = exports.getNetworkInterfaces
  = function () { return {} };
  
  exports.arch = function () { return 'javascript' };
  
  exports.platform = function () { return 'browser' };
  
  exports.tmpdir = exports.tmpDir = function () {
      return '/tmp';
  };
  
  exports.EOL = '\n';
  
  exports.homedir = function () {
    return '/'
  };
  
  },{}],20:[function(require,module,exports){
  'use strict';
  
  
  var TYPED_OK =  (typeof Uint8Array !== 'undefined') &&
                  (typeof Uint16Array !== 'undefined') &&
                  (typeof Int32Array !== 'undefined');
  
  function _has(obj, key) {
    return Object.prototype.hasOwnProperty.call(obj, key);
  }
  
  exports.assign = function (obj /*from1, from2, from3, ...*/) {
    var sources = Array.prototype.slice.call(arguments, 1);
    while (sources.length) {
      var source = sources.shift();
      if (!source) { continue; }
  
      if (typeof source !== 'object') {
        throw new TypeError(source + 'must be non-object');
      }
  
      for (var p in source) {
        if (_has(source, p)) {
          obj[p] = source[p];
        }
      }
    }
  
    return obj;
  };
  
  
  // reduce buffer size, avoiding mem copy
  exports.shrinkBuf = function (buf, size) {
    if (buf.length === size) { return buf; }
    if (buf.subarray) { return buf.subarray(0, size); }
    buf.length = size;
    return buf;
  };
  
  
  var fnTyped = {
    arraySet: function (dest, src, src_offs, len, dest_offs) {
      if (src.subarray && dest.subarray) {
        dest.set(src.subarray(src_offs, src_offs + len), dest_offs);
        return;
      }
      // Fallback to ordinary array
      for (var i = 0; i < len; i++) {
        dest[dest_offs + i] = src[src_offs + i];
      }
    },
    // Join array of chunks to single array.
    flattenChunks: function (chunks) {
      var i, l, len, pos, chunk, result;
  
      // calculate data length
      len = 0;
      for (i = 0, l = chunks.length; i < l; i++) {
        len += chunks[i].length;
      }
  
      // join chunks
      result = new Uint8Array(len);
      pos = 0;
      for (i = 0, l = chunks.length; i < l; i++) {
        chunk = chunks[i];
        result.set(chunk, pos);
        pos += chunk.length;
      }
  
      return result;
    }
  };
  
  var fnUntyped = {
    arraySet: function (dest, src, src_offs, len, dest_offs) {
      for (var i = 0; i < len; i++) {
        dest[dest_offs + i] = src[src_offs + i];
      }
    },
    // Join array of chunks to single array.
    flattenChunks: function (chunks) {
      return [].concat.apply([], chunks);
    }
  };
  
  
  // Enable/Disable typed arrays use, for testing
  //
  exports.setTyped = function (on) {
    if (on) {
      exports.Buf8  = Uint8Array;
      exports.Buf16 = Uint16Array;
      exports.Buf32 = Int32Array;
      exports.assign(exports, fnTyped);
    } else {
      exports.Buf8  = Array;
      exports.Buf16 = Array;
      exports.Buf32 = Array;
      exports.assign(exports, fnUntyped);
    }
  };
  
  exports.setTyped(TYPED_OK);
  
  },{}],21:[function(require,module,exports){
  'use strict';
  
  // Note: adler32 takes 12% for level 0 and 2% for level 6.
  // It isn't worth it to make additional optimizations as in original.
  // Small size is preferable.
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  function adler32(adler, buf, len, pos) {
    var s1 = (adler & 0xffff) |0,
        s2 = ((adler >>> 16) & 0xffff) |0,
        n = 0;
  
    while (len !== 0) {
      // Set limit ~ twice less than 5552, to keep
      // s2 in 31-bits, because we force signed ints.
      // in other case %= will fail.
      n = len > 2000 ? 2000 : len;
      len -= n;
  
      do {
        s1 = (s1 + buf[pos++]) |0;
        s2 = (s2 + s1) |0;
      } while (--n);
  
      s1 %= 65521;
      s2 %= 65521;
    }
  
    return (s1 | (s2 << 16)) |0;
  }
  
  
  module.exports = adler32;
  
  },{}],22:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  module.exports = {
  
    /* Allowed flush values; see deflate() and inflate() below for details */
    Z_NO_FLUSH:         0,
    Z_PARTIAL_FLUSH:    1,
    Z_SYNC_FLUSH:       2,
    Z_FULL_FLUSH:       3,
    Z_FINISH:           4,
    Z_BLOCK:            5,
    Z_TREES:            6,
  
    /* Return codes for the compression/decompression functions. Negative values
    * are errors, positive values are used for special but normal events.
    */
    Z_OK:               0,
    Z_STREAM_END:       1,
    Z_NEED_DICT:        2,
    Z_ERRNO:           -1,
    Z_STREAM_ERROR:    -2,
    Z_DATA_ERROR:      -3,
    //Z_MEM_ERROR:     -4,
    Z_BUF_ERROR:       -5,
    //Z_VERSION_ERROR: -6,
  
    /* compression levels */
    Z_NO_COMPRESSION:         0,
    Z_BEST_SPEED:             1,
    Z_BEST_COMPRESSION:       9,
    Z_DEFAULT_COMPRESSION:   -1,
  
  
    Z_FILTERED:               1,
    Z_HUFFMAN_ONLY:           2,
    Z_RLE:                    3,
    Z_FIXED:                  4,
    Z_DEFAULT_STRATEGY:       0,
  
    /* Possible values of the data_type field (though see inflate()) */
    Z_BINARY:                 0,
    Z_TEXT:                   1,
    //Z_ASCII:                1, // = Z_TEXT (deprecated)
    Z_UNKNOWN:                2,
  
    /* The deflate compression method */
    Z_DEFLATED:               8
    //Z_NULL:                 null // Use -1 or null inline, depending on var type
  };
  
  },{}],23:[function(require,module,exports){
  'use strict';
  
  // Note: we can't get significant speed boost here.
  // So write code to minimize size - no pregenerated tables
  // and array tools dependencies.
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  // Use ordinary array, since untyped makes no boost here
  function makeTable() {
    var c, table = [];
  
    for (var n = 0; n < 256; n++) {
      c = n;
      for (var k = 0; k < 8; k++) {
        c = ((c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1));
      }
      table[n] = c;
    }
  
    return table;
  }
  
  // Create table on load. Just 255 signed longs. Not a problem.
  var crcTable = makeTable();
  
  
  function crc32(crc, buf, len, pos) {
    var t = crcTable,
        end = pos + len;
  
    crc ^= -1;
  
    for (var i = pos; i < end; i++) {
      crc = (crc >>> 8) ^ t[(crc ^ buf[i]) & 0xFF];
    }
  
    return (crc ^ (-1)); // >>> 0;
  }
  
  
  module.exports = crc32;
  
  },{}],24:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  var utils   = require('../utils/common');
  var trees   = require('./trees');
  var adler32 = require('./adler32');
  var crc32   = require('./crc32');
  var msg     = require('./messages');
  
  /* Public constants ==========================================================*/
  /* ===========================================================================*/
  
  
  /* Allowed flush values; see deflate() and inflate() below for details */
  var Z_NO_FLUSH      = 0;
  var Z_PARTIAL_FLUSH = 1;
  //var Z_SYNC_FLUSH    = 2;
  var Z_FULL_FLUSH    = 3;
  var Z_FINISH        = 4;
  var Z_BLOCK         = 5;
  //var Z_TREES         = 6;
  
  
  /* Return codes for the compression/decompression functions. Negative values
   * are errors, positive values are used for special but normal events.
   */
  var Z_OK            = 0;
  var Z_STREAM_END    = 1;
  //var Z_NEED_DICT     = 2;
  //var Z_ERRNO         = -1;
  var Z_STREAM_ERROR  = -2;
  var Z_DATA_ERROR    = -3;
  //var Z_MEM_ERROR     = -4;
  var Z_BUF_ERROR     = -5;
  //var Z_VERSION_ERROR = -6;
  
  
  /* compression levels */
  //var Z_NO_COMPRESSION      = 0;
  //var Z_BEST_SPEED          = 1;
  //var Z_BEST_COMPRESSION    = 9;
  var Z_DEFAULT_COMPRESSION = -1;
  
  
  var Z_FILTERED            = 1;
  var Z_HUFFMAN_ONLY        = 2;
  var Z_RLE                 = 3;
  var Z_FIXED               = 4;
  var Z_DEFAULT_STRATEGY    = 0;
  
  /* Possible values of the data_type field (though see inflate()) */
  //var Z_BINARY              = 0;
  //var Z_TEXT                = 1;
  //var Z_ASCII               = 1; // = Z_TEXT
  var Z_UNKNOWN             = 2;
  
  
  /* The deflate compression method */
  var Z_DEFLATED  = 8;
  
  /*============================================================================*/
  
  
  var MAX_MEM_LEVEL = 9;
  /* Maximum value for memLevel in deflateInit2 */
  var MAX_WBITS = 15;
  /* 32K LZ77 window */
  var DEF_MEM_LEVEL = 8;
  
  
  var LENGTH_CODES  = 29;
  /* number of length codes, not counting the special END_BLOCK code */
  var LITERALS      = 256;
  /* number of literal bytes 0..255 */
  var L_CODES       = LITERALS + 1 + LENGTH_CODES;
  /* number of Literal or Length codes, including the END_BLOCK code */
  var D_CODES       = 30;
  /* number of distance codes */
  var BL_CODES      = 19;
  /* number of codes used to transfer the bit lengths */
  var HEAP_SIZE     = 2 * L_CODES + 1;
  /* maximum heap size */
  var MAX_BITS  = 15;
  /* All codes must not exceed MAX_BITS bits */
  
  var MIN_MATCH = 3;
  var MAX_MATCH = 258;
  var MIN_LOOKAHEAD = (MAX_MATCH + MIN_MATCH + 1);
  
  var PRESET_DICT = 0x20;
  
  var INIT_STATE = 42;
  var EXTRA_STATE = 69;
  var NAME_STATE = 73;
  var COMMENT_STATE = 91;
  var HCRC_STATE = 103;
  var BUSY_STATE = 113;
  var FINISH_STATE = 666;
  
  var BS_NEED_MORE      = 1; /* block not completed, need more input or more output */
  var BS_BLOCK_DONE     = 2; /* block flush performed */
  var BS_FINISH_STARTED = 3; /* finish started, need only more output at next deflate */
  var BS_FINISH_DONE    = 4; /* finish done, accept no more input or output */
  
  var OS_CODE = 0x03; // Unix :) . Don't detect, use this default.
  
  function err(strm, errorCode) {
    strm.msg = msg[errorCode];
    return errorCode;
  }
  
  function rank(f) {
    return ((f) << 1) - ((f) > 4 ? 9 : 0);
  }
  
  function zero(buf) { var len = buf.length; while (--len >= 0) { buf[len] = 0; } }
  
  
  /* =========================================================================
   * Flush as much pending output as possible. All deflate() output goes
   * through this function so some applications may wish to modify it
   * to avoid allocating a large strm->output buffer and copying into it.
   * (See also read_buf()).
   */
  function flush_pending(strm) {
    var s = strm.state;
  
    //_tr_flush_bits(s);
    var len = s.pending;
    if (len > strm.avail_out) {
      len = strm.avail_out;
    }
    if (len === 0) { return; }
  
    utils.arraySet(strm.output, s.pending_buf, s.pending_out, len, strm.next_out);
    strm.next_out += len;
    s.pending_out += len;
    strm.total_out += len;
    strm.avail_out -= len;
    s.pending -= len;
    if (s.pending === 0) {
      s.pending_out = 0;
    }
  }
  
  
  function flush_block_only(s, last) {
    trees._tr_flush_block(s, (s.block_start >= 0 ? s.block_start : -1), s.strstart - s.block_start, last);
    s.block_start = s.strstart;
    flush_pending(s.strm);
  }
  
  
  function put_byte(s, b) {
    s.pending_buf[s.pending++] = b;
  }
  
  
  /* =========================================================================
   * Put a short in the pending buffer. The 16-bit value is put in MSB order.
   * IN assertion: the stream state is correct and there is enough room in
   * pending_buf.
   */
  function putShortMSB(s, b) {
  //  put_byte(s, (Byte)(b >> 8));
  //  put_byte(s, (Byte)(b & 0xff));
    s.pending_buf[s.pending++] = (b >>> 8) & 0xff;
    s.pending_buf[s.pending++] = b & 0xff;
  }
  
  
  /* ===========================================================================
   * Read a new buffer from the current input stream, update the adler32
   * and total number of bytes read.  All deflate() input goes through
   * this function so some applications may wish to modify it to avoid
   * allocating a large strm->input buffer and copying from it.
   * (See also flush_pending()).
   */
  function read_buf(strm, buf, start, size) {
    var len = strm.avail_in;
  
    if (len > size) { len = size; }
    if (len === 0) { return 0; }
  
    strm.avail_in -= len;
  
    // zmemcpy(buf, strm->next_in, len);
    utils.arraySet(buf, strm.input, strm.next_in, len, start);
    if (strm.state.wrap === 1) {
      strm.adler = adler32(strm.adler, buf, len, start);
    }
  
    else if (strm.state.wrap === 2) {
      strm.adler = crc32(strm.adler, buf, len, start);
    }
  
    strm.next_in += len;
    strm.total_in += len;
  
    return len;
  }
  
  
  /* ===========================================================================
   * Set match_start to the longest match starting at the given string and
   * return its length. Matches shorter or equal to prev_length are discarded,
   * in which case the result is equal to prev_length and match_start is
   * garbage.
   * IN assertions: cur_match is the head of the hash chain for the current
   *   string (strstart) and its distance is <= MAX_DIST, and prev_length >= 1
   * OUT assertion: the match length is not greater than s->lookahead.
   */
  function longest_match(s, cur_match) {
    var chain_length = s.max_chain_length;      /* max hash chain length */
    var scan = s.strstart; /* current string */
    var match;                       /* matched string */
    var len;                           /* length of current match */
    var best_len = s.prev_length;              /* best match length so far */
    var nice_match = s.nice_match;             /* stop if match long enough */
    var limit = (s.strstart > (s.w_size - MIN_LOOKAHEAD)) ?
        s.strstart - (s.w_size - MIN_LOOKAHEAD) : 0/*NIL*/;
  
    var _win = s.window; // shortcut
  
    var wmask = s.w_mask;
    var prev  = s.prev;
  
    /* Stop when cur_match becomes <= limit. To simplify the code,
     * we prevent matches with the string of window index 0.
     */
  
    var strend = s.strstart + MAX_MATCH;
    var scan_end1  = _win[scan + best_len - 1];
    var scan_end   = _win[scan + best_len];
  
    /* The code is optimized for HASH_BITS >= 8 and MAX_MATCH-2 multiple of 16.
     * It is easy to get rid of this optimization if necessary.
     */
    // Assert(s->hash_bits >= 8 && MAX_MATCH == 258, "Code too clever");
  
    /* Do not waste too much time if we already have a good match: */
    if (s.prev_length >= s.good_match) {
      chain_length >>= 2;
    }
    /* Do not look for matches beyond the end of the input. This is necessary
     * to make deflate deterministic.
     */
    if (nice_match > s.lookahead) { nice_match = s.lookahead; }
  
    // Assert((ulg)s->strstart <= s->window_size-MIN_LOOKAHEAD, "need lookahead");
  
    do {
      // Assert(cur_match < s->strstart, "no future");
      match = cur_match;
  
      /* Skip to next match if the match length cannot increase
       * or if the match length is less than 2.  Note that the checks below
       * for insufficient lookahead only occur occasionally for performance
       * reasons.  Therefore uninitialized memory will be accessed, and
       * conditional jumps will be made that depend on those values.
       * However the length of the match is limited to the lookahead, so
       * the output of deflate is not affected by the uninitialized values.
       */
  
      if (_win[match + best_len]     !== scan_end  ||
          _win[match + best_len - 1] !== scan_end1 ||
          _win[match]                !== _win[scan] ||
          _win[++match]              !== _win[scan + 1]) {
        continue;
      }
  
      /* The check at best_len-1 can be removed because it will be made
       * again later. (This heuristic is not always a win.)
       * It is not necessary to compare scan[2] and match[2] since they
       * are always equal when the other bytes match, given that
       * the hash keys are equal and that HASH_BITS >= 8.
       */
      scan += 2;
      match++;
      // Assert(*scan == *match, "match[2]?");
  
      /* We check for insufficient lookahead only every 8th comparison;
       * the 256th check will be made at strstart+258.
       */
      do {
        /*jshint noempty:false*/
      } while (_win[++scan] === _win[++match] && _win[++scan] === _win[++match] &&
               _win[++scan] === _win[++match] && _win[++scan] === _win[++match] &&
               _win[++scan] === _win[++match] && _win[++scan] === _win[++match] &&
               _win[++scan] === _win[++match] && _win[++scan] === _win[++match] &&
               scan < strend);
  
      // Assert(scan <= s->window+(unsigned)(s->window_size-1), "wild scan");
  
      len = MAX_MATCH - (strend - scan);
      scan = strend - MAX_MATCH;
  
      if (len > best_len) {
        s.match_start = cur_match;
        best_len = len;
        if (len >= nice_match) {
          break;
        }
        scan_end1  = _win[scan + best_len - 1];
        scan_end   = _win[scan + best_len];
      }
    } while ((cur_match = prev[cur_match & wmask]) > limit && --chain_length !== 0);
  
    if (best_len <= s.lookahead) {
      return best_len;
    }
    return s.lookahead;
  }
  
  
  /* ===========================================================================
   * Fill the window when the lookahead becomes insufficient.
   * Updates strstart and lookahead.
   *
   * IN assertion: lookahead < MIN_LOOKAHEAD
   * OUT assertions: strstart <= window_size-MIN_LOOKAHEAD
   *    At least one byte has been read, or avail_in == 0; reads are
   *    performed for at least two bytes (required for the zip translate_eol
   *    option -- not supported here).
   */
  function fill_window(s) {
    var _w_size = s.w_size;
    var p, n, m, more, str;
  
    //Assert(s->lookahead < MIN_LOOKAHEAD, "already enough lookahead");
  
    do {
      more = s.window_size - s.lookahead - s.strstart;
  
      // JS ints have 32 bit, block below not needed
      /* Deal with !@#$% 64K limit: */
      //if (sizeof(int) <= 2) {
      //    if (more == 0 && s->strstart == 0 && s->lookahead == 0) {
      //        more = wsize;
      //
      //  } else if (more == (unsigned)(-1)) {
      //        /* Very unlikely, but possible on 16 bit machine if
      //         * strstart == 0 && lookahead == 1 (input done a byte at time)
      //         */
      //        more--;
      //    }
      //}
  
  
      /* If the window is almost full and there is insufficient lookahead,
       * move the upper half to the lower one to make room in the upper half.
       */
      if (s.strstart >= _w_size + (_w_size - MIN_LOOKAHEAD)) {
  
        utils.arraySet(s.window, s.window, _w_size, _w_size, 0);
        s.match_start -= _w_size;
        s.strstart -= _w_size;
        /* we now have strstart >= MAX_DIST */
        s.block_start -= _w_size;
  
        /* Slide the hash table (could be avoided with 32 bit values
         at the expense of memory usage). We slide even when level == 0
         to keep the hash table consistent if we switch back to level > 0
         later. (Using level 0 permanently is not an optimal usage of
         zlib, so we don't care about this pathological case.)
         */
  
        n = s.hash_size;
        p = n;
        do {
          m = s.head[--p];
          s.head[p] = (m >= _w_size ? m - _w_size : 0);
        } while (--n);
  
        n = _w_size;
        p = n;
        do {
          m = s.prev[--p];
          s.prev[p] = (m >= _w_size ? m - _w_size : 0);
          /* If n is not on any hash chain, prev[n] is garbage but
           * its value will never be used.
           */
        } while (--n);
  
        more += _w_size;
      }
      if (s.strm.avail_in === 0) {
        break;
      }
  
      /* If there was no sliding:
       *    strstart <= WSIZE+MAX_DIST-1 && lookahead <= MIN_LOOKAHEAD - 1 &&
       *    more == window_size - lookahead - strstart
       * => more >= window_size - (MIN_LOOKAHEAD-1 + WSIZE + MAX_DIST-1)
       * => more >= window_size - 2*WSIZE + 2
       * In the BIG_MEM or MMAP case (not yet supported),
       *   window_size == input_size + MIN_LOOKAHEAD  &&
       *   strstart + s->lookahead <= input_size => more >= MIN_LOOKAHEAD.
       * Otherwise, window_size == 2*WSIZE so more >= 2.
       * If there was sliding, more >= WSIZE. So in all cases, more >= 2.
       */
      //Assert(more >= 2, "more < 2");
      n = read_buf(s.strm, s.window, s.strstart + s.lookahead, more);
      s.lookahead += n;
  
      /* Initialize the hash value now that we have some input: */
      if (s.lookahead + s.insert >= MIN_MATCH) {
        str = s.strstart - s.insert;
        s.ins_h = s.window[str];
  
        /* UPDATE_HASH(s, s->ins_h, s->window[str + 1]); */
        s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[str + 1]) & s.hash_mask;
  //#if MIN_MATCH != 3
  //        Call update_hash() MIN_MATCH-3 more times
  //#endif
        while (s.insert) {
          /* UPDATE_HASH(s, s->ins_h, s->window[str + MIN_MATCH-1]); */
          s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[str + MIN_MATCH - 1]) & s.hash_mask;
  
          s.prev[str & s.w_mask] = s.head[s.ins_h];
          s.head[s.ins_h] = str;
          str++;
          s.insert--;
          if (s.lookahead + s.insert < MIN_MATCH) {
            break;
          }
        }
      }
      /* If the whole input has less than MIN_MATCH bytes, ins_h is garbage,
       * but this is not important since only literal bytes will be emitted.
       */
  
    } while (s.lookahead < MIN_LOOKAHEAD && s.strm.avail_in !== 0);
  
    /* If the WIN_INIT bytes after the end of the current data have never been
     * written, then zero those bytes in order to avoid memory check reports of
     * the use of uninitialized (or uninitialised as Julian writes) bytes by
     * the longest match routines.  Update the high water mark for the next
     * time through here.  WIN_INIT is set to MAX_MATCH since the longest match
     * routines allow scanning to strstart + MAX_MATCH, ignoring lookahead.
     */
  //  if (s.high_water < s.window_size) {
  //    var curr = s.strstart + s.lookahead;
  //    var init = 0;
  //
  //    if (s.high_water < curr) {
  //      /* Previous high water mark below current data -- zero WIN_INIT
  //       * bytes or up to end of window, whichever is less.
  //       */
  //      init = s.window_size - curr;
  //      if (init > WIN_INIT)
  //        init = WIN_INIT;
  //      zmemzero(s->window + curr, (unsigned)init);
  //      s->high_water = curr + init;
  //    }
  //    else if (s->high_water < (ulg)curr + WIN_INIT) {
  //      /* High water mark at or above current data, but below current data
  //       * plus WIN_INIT -- zero out to current data plus WIN_INIT, or up
  //       * to end of window, whichever is less.
  //       */
  //      init = (ulg)curr + WIN_INIT - s->high_water;
  //      if (init > s->window_size - s->high_water)
  //        init = s->window_size - s->high_water;
  //      zmemzero(s->window + s->high_water, (unsigned)init);
  //      s->high_water += init;
  //    }
  //  }
  //
  //  Assert((ulg)s->strstart <= s->window_size - MIN_LOOKAHEAD,
  //    "not enough room for search");
  }
  
  /* ===========================================================================
   * Copy without compression as much as possible from the input stream, return
   * the current block state.
   * This function does not insert new strings in the dictionary since
   * uncompressible data is probably not useful. This function is used
   * only for the level=0 compression option.
   * NOTE: this function should be optimized to avoid extra copying from
   * window to pending_buf.
   */
  function deflate_stored(s, flush) {
    /* Stored blocks are limited to 0xffff bytes, pending_buf is limited
     * to pending_buf_size, and each stored block has a 5 byte header:
     */
    var max_block_size = 0xffff;
  
    if (max_block_size > s.pending_buf_size - 5) {
      max_block_size = s.pending_buf_size - 5;
    }
  
    /* Copy as much as possible from input to output: */
    for (;;) {
      /* Fill the window as much as possible: */
      if (s.lookahead <= 1) {
  
        //Assert(s->strstart < s->w_size+MAX_DIST(s) ||
        //  s->block_start >= (long)s->w_size, "slide too late");
  //      if (!(s.strstart < s.w_size + (s.w_size - MIN_LOOKAHEAD) ||
  //        s.block_start >= s.w_size)) {
  //        throw  new Error("slide too late");
  //      }
  
        fill_window(s);
        if (s.lookahead === 0 && flush === Z_NO_FLUSH) {
          return BS_NEED_MORE;
        }
  
        if (s.lookahead === 0) {
          break;
        }
        /* flush the current block */
      }
      //Assert(s->block_start >= 0L, "block gone");
  //    if (s.block_start < 0) throw new Error("block gone");
  
      s.strstart += s.lookahead;
      s.lookahead = 0;
  
      /* Emit a stored block if pending_buf will be full: */
      var max_start = s.block_start + max_block_size;
  
      if (s.strstart === 0 || s.strstart >= max_start) {
        /* strstart == 0 is possible when wraparound on 16-bit machine */
        s.lookahead = s.strstart - max_start;
        s.strstart = max_start;
        /*** FLUSH_BLOCK(s, 0); ***/
        flush_block_only(s, false);
        if (s.strm.avail_out === 0) {
          return BS_NEED_MORE;
        }
        /***/
  
  
      }
      /* Flush if we may have to slide, otherwise block_start may become
       * negative and the data will be gone:
       */
      if (s.strstart - s.block_start >= (s.w_size - MIN_LOOKAHEAD)) {
        /*** FLUSH_BLOCK(s, 0); ***/
        flush_block_only(s, false);
        if (s.strm.avail_out === 0) {
          return BS_NEED_MORE;
        }
        /***/
      }
    }
  
    s.insert = 0;
  
    if (flush === Z_FINISH) {
      /*** FLUSH_BLOCK(s, 1); ***/
      flush_block_only(s, true);
      if (s.strm.avail_out === 0) {
        return BS_FINISH_STARTED;
      }
      /***/
      return BS_FINISH_DONE;
    }
  
    if (s.strstart > s.block_start) {
      /*** FLUSH_BLOCK(s, 0); ***/
      flush_block_only(s, false);
      if (s.strm.avail_out === 0) {
        return BS_NEED_MORE;
      }
      /***/
    }
  
    return BS_NEED_MORE;
  }
  
  /* ===========================================================================
   * Compress as much as possible from the input stream, return the current
   * block state.
   * This function does not perform lazy evaluation of matches and inserts
   * new strings in the dictionary only for unmatched strings or for short
   * matches. It is used only for the fast compression options.
   */
  function deflate_fast(s, flush) {
    var hash_head;        /* head of the hash chain */
    var bflush;           /* set if current block must be flushed */
  
    for (;;) {
      /* Make sure that we always have enough lookahead, except
       * at the end of the input file. We need MAX_MATCH bytes
       * for the next match, plus MIN_MATCH bytes to insert the
       * string following the next match.
       */
      if (s.lookahead < MIN_LOOKAHEAD) {
        fill_window(s);
        if (s.lookahead < MIN_LOOKAHEAD && flush === Z_NO_FLUSH) {
          return BS_NEED_MORE;
        }
        if (s.lookahead === 0) {
          break; /* flush the current block */
        }
      }
  
      /* Insert the string window[strstart .. strstart+2] in the
       * dictionary, and set hash_head to the head of the hash chain:
       */
      hash_head = 0/*NIL*/;
      if (s.lookahead >= MIN_MATCH) {
        /*** INSERT_STRING(s, s.strstart, hash_head); ***/
        s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[s.strstart + MIN_MATCH - 1]) & s.hash_mask;
        hash_head = s.prev[s.strstart & s.w_mask] = s.head[s.ins_h];
        s.head[s.ins_h] = s.strstart;
        /***/
      }
  
      /* Find the longest match, discarding those <= prev_length.
       * At this point we have always match_length < MIN_MATCH
       */
      if (hash_head !== 0/*NIL*/ && ((s.strstart - hash_head) <= (s.w_size - MIN_LOOKAHEAD))) {
        /* To simplify the code, we prevent matches with the string
         * of window index 0 (in particular we have to avoid a match
         * of the string with itself at the start of the input file).
         */
        s.match_length = longest_match(s, hash_head);
        /* longest_match() sets match_start */
      }
      if (s.match_length >= MIN_MATCH) {
        // check_match(s, s.strstart, s.match_start, s.match_length); // for debug only
  
        /*** _tr_tally_dist(s, s.strstart - s.match_start,
                       s.match_length - MIN_MATCH, bflush); ***/
        bflush = trees._tr_tally(s, s.strstart - s.match_start, s.match_length - MIN_MATCH);
  
        s.lookahead -= s.match_length;
  
        /* Insert new strings in the hash table only if the match length
         * is not too large. This saves time but degrades compression.
         */
        if (s.match_length <= s.max_lazy_match/*max_insert_length*/ && s.lookahead >= MIN_MATCH) {
          s.match_length--; /* string at strstart already in table */
          do {
            s.strstart++;
            /*** INSERT_STRING(s, s.strstart, hash_head); ***/
            s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[s.strstart + MIN_MATCH - 1]) & s.hash_mask;
            hash_head = s.prev[s.strstart & s.w_mask] = s.head[s.ins_h];
            s.head[s.ins_h] = s.strstart;
            /***/
            /* strstart never exceeds WSIZE-MAX_MATCH, so there are
             * always MIN_MATCH bytes ahead.
             */
          } while (--s.match_length !== 0);
          s.strstart++;
        } else
        {
          s.strstart += s.match_length;
          s.match_length = 0;
          s.ins_h = s.window[s.strstart];
          /* UPDATE_HASH(s, s.ins_h, s.window[s.strstart+1]); */
          s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[s.strstart + 1]) & s.hash_mask;
  
  //#if MIN_MATCH != 3
  //                Call UPDATE_HASH() MIN_MATCH-3 more times
  //#endif
          /* If lookahead < MIN_MATCH, ins_h is garbage, but it does not
           * matter since it will be recomputed at next deflate call.
           */
        }
      } else {
        /* No match, output a literal byte */
        //Tracevv((stderr,"%c", s.window[s.strstart]));
        /*** _tr_tally_lit(s, s.window[s.strstart], bflush); ***/
        bflush = trees._tr_tally(s, 0, s.window[s.strstart]);
  
        s.lookahead--;
        s.strstart++;
      }
      if (bflush) {
        /*** FLUSH_BLOCK(s, 0); ***/
        flush_block_only(s, false);
        if (s.strm.avail_out === 0) {
          return BS_NEED_MORE;
        }
        /***/
      }
    }
    s.insert = ((s.strstart < (MIN_MATCH - 1)) ? s.strstart : MIN_MATCH - 1);
    if (flush === Z_FINISH) {
      /*** FLUSH_BLOCK(s, 1); ***/
      flush_block_only(s, true);
      if (s.strm.avail_out === 0) {
        return BS_FINISH_STARTED;
      }
      /***/
      return BS_FINISH_DONE;
    }
    if (s.last_lit) {
      /*** FLUSH_BLOCK(s, 0); ***/
      flush_block_only(s, false);
      if (s.strm.avail_out === 0) {
        return BS_NEED_MORE;
      }
      /***/
    }
    return BS_BLOCK_DONE;
  }
  
  /* ===========================================================================
   * Same as above, but achieves better compression. We use a lazy
   * evaluation for matches: a match is finally adopted only if there is
   * no better match at the next window position.
   */
  function deflate_slow(s, flush) {
    var hash_head;          /* head of hash chain */
    var bflush;              /* set if current block must be flushed */
  
    var max_insert;
  
    /* Process the input block. */
    for (;;) {
      /* Make sure that we always have enough lookahead, except
       * at the end of the input file. We need MAX_MATCH bytes
       * for the next match, plus MIN_MATCH bytes to insert the
       * string following the next match.
       */
      if (s.lookahead < MIN_LOOKAHEAD) {
        fill_window(s);
        if (s.lookahead < MIN_LOOKAHEAD && flush === Z_NO_FLUSH) {
          return BS_NEED_MORE;
        }
        if (s.lookahead === 0) { break; } /* flush the current block */
      }
  
      /* Insert the string window[strstart .. strstart+2] in the
       * dictionary, and set hash_head to the head of the hash chain:
       */
      hash_head = 0/*NIL*/;
      if (s.lookahead >= MIN_MATCH) {
        /*** INSERT_STRING(s, s.strstart, hash_head); ***/
        s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[s.strstart + MIN_MATCH - 1]) & s.hash_mask;
        hash_head = s.prev[s.strstart & s.w_mask] = s.head[s.ins_h];
        s.head[s.ins_h] = s.strstart;
        /***/
      }
  
      /* Find the longest match, discarding those <= prev_length.
       */
      s.prev_length = s.match_length;
      s.prev_match = s.match_start;
      s.match_length = MIN_MATCH - 1;
  
      if (hash_head !== 0/*NIL*/ && s.prev_length < s.max_lazy_match &&
          s.strstart - hash_head <= (s.w_size - MIN_LOOKAHEAD)/*MAX_DIST(s)*/) {
        /* To simplify the code, we prevent matches with the string
         * of window index 0 (in particular we have to avoid a match
         * of the string with itself at the start of the input file).
         */
        s.match_length = longest_match(s, hash_head);
        /* longest_match() sets match_start */
  
        if (s.match_length <= 5 &&
           (s.strategy === Z_FILTERED || (s.match_length === MIN_MATCH && s.strstart - s.match_start > 4096/*TOO_FAR*/))) {
  
          /* If prev_match is also MIN_MATCH, match_start is garbage
           * but we will ignore the current match anyway.
           */
          s.match_length = MIN_MATCH - 1;
        }
      }
      /* If there was a match at the previous step and the current
       * match is not better, output the previous match:
       */
      if (s.prev_length >= MIN_MATCH && s.match_length <= s.prev_length) {
        max_insert = s.strstart + s.lookahead - MIN_MATCH;
        /* Do not insert strings in hash table beyond this. */
  
        //check_match(s, s.strstart-1, s.prev_match, s.prev_length);
  
        /***_tr_tally_dist(s, s.strstart - 1 - s.prev_match,
                       s.prev_length - MIN_MATCH, bflush);***/
        bflush = trees._tr_tally(s, s.strstart - 1 - s.prev_match, s.prev_length - MIN_MATCH);
        /* Insert in hash table all strings up to the end of the match.
         * strstart-1 and strstart are already inserted. If there is not
         * enough lookahead, the last two strings are not inserted in
         * the hash table.
         */
        s.lookahead -= s.prev_length - 1;
        s.prev_length -= 2;
        do {
          if (++s.strstart <= max_insert) {
            /*** INSERT_STRING(s, s.strstart, hash_head); ***/
            s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[s.strstart + MIN_MATCH - 1]) & s.hash_mask;
            hash_head = s.prev[s.strstart & s.w_mask] = s.head[s.ins_h];
            s.head[s.ins_h] = s.strstart;
            /***/
          }
        } while (--s.prev_length !== 0);
        s.match_available = 0;
        s.match_length = MIN_MATCH - 1;
        s.strstart++;
  
        if (bflush) {
          /*** FLUSH_BLOCK(s, 0); ***/
          flush_block_only(s, false);
          if (s.strm.avail_out === 0) {
            return BS_NEED_MORE;
          }
          /***/
        }
  
      } else if (s.match_available) {
        /* If there was no match at the previous position, output a
         * single literal. If there was a match but the current match
         * is longer, truncate the previous match to a single literal.
         */
        //Tracevv((stderr,"%c", s->window[s->strstart-1]));
        /*** _tr_tally_lit(s, s.window[s.strstart-1], bflush); ***/
        bflush = trees._tr_tally(s, 0, s.window[s.strstart - 1]);
  
        if (bflush) {
          /*** FLUSH_BLOCK_ONLY(s, 0) ***/
          flush_block_only(s, false);
          /***/
        }
        s.strstart++;
        s.lookahead--;
        if (s.strm.avail_out === 0) {
          return BS_NEED_MORE;
        }
      } else {
        /* There is no previous match to compare with, wait for
         * the next step to decide.
         */
        s.match_available = 1;
        s.strstart++;
        s.lookahead--;
      }
    }
    //Assert (flush != Z_NO_FLUSH, "no flush?");
    if (s.match_available) {
      //Tracevv((stderr,"%c", s->window[s->strstart-1]));
      /*** _tr_tally_lit(s, s.window[s.strstart-1], bflush); ***/
      bflush = trees._tr_tally(s, 0, s.window[s.strstart - 1]);
  
      s.match_available = 0;
    }
    s.insert = s.strstart < MIN_MATCH - 1 ? s.strstart : MIN_MATCH - 1;
    if (flush === Z_FINISH) {
      /*** FLUSH_BLOCK(s, 1); ***/
      flush_block_only(s, true);
      if (s.strm.avail_out === 0) {
        return BS_FINISH_STARTED;
      }
      /***/
      return BS_FINISH_DONE;
    }
    if (s.last_lit) {
      /*** FLUSH_BLOCK(s, 0); ***/
      flush_block_only(s, false);
      if (s.strm.avail_out === 0) {
        return BS_NEED_MORE;
      }
      /***/
    }
  
    return BS_BLOCK_DONE;
  }
  
  
  /* ===========================================================================
   * For Z_RLE, simply look for runs of bytes, generate matches only of distance
   * one.  Do not maintain a hash table.  (It will be regenerated if this run of
   * deflate switches away from Z_RLE.)
   */
  function deflate_rle(s, flush) {
    var bflush;            /* set if current block must be flushed */
    var prev;              /* byte at distance one to match */
    var scan, strend;      /* scan goes up to strend for length of run */
  
    var _win = s.window;
  
    for (;;) {
      /* Make sure that we always have enough lookahead, except
       * at the end of the input file. We need MAX_MATCH bytes
       * for the longest run, plus one for the unrolled loop.
       */
      if (s.lookahead <= MAX_MATCH) {
        fill_window(s);
        if (s.lookahead <= MAX_MATCH && flush === Z_NO_FLUSH) {
          return BS_NEED_MORE;
        }
        if (s.lookahead === 0) { break; } /* flush the current block */
      }
  
      /* See how many times the previous byte repeats */
      s.match_length = 0;
      if (s.lookahead >= MIN_MATCH && s.strstart > 0) {
        scan = s.strstart - 1;
        prev = _win[scan];
        if (prev === _win[++scan] && prev === _win[++scan] && prev === _win[++scan]) {
          strend = s.strstart + MAX_MATCH;
          do {
            /*jshint noempty:false*/
          } while (prev === _win[++scan] && prev === _win[++scan] &&
                   prev === _win[++scan] && prev === _win[++scan] &&
                   prev === _win[++scan] && prev === _win[++scan] &&
                   prev === _win[++scan] && prev === _win[++scan] &&
                   scan < strend);
          s.match_length = MAX_MATCH - (strend - scan);
          if (s.match_length > s.lookahead) {
            s.match_length = s.lookahead;
          }
        }
        //Assert(scan <= s->window+(uInt)(s->window_size-1), "wild scan");
      }
  
      /* Emit match if have run of MIN_MATCH or longer, else emit literal */
      if (s.match_length >= MIN_MATCH) {
        //check_match(s, s.strstart, s.strstart - 1, s.match_length);
  
        /*** _tr_tally_dist(s, 1, s.match_length - MIN_MATCH, bflush); ***/
        bflush = trees._tr_tally(s, 1, s.match_length - MIN_MATCH);
  
        s.lookahead -= s.match_length;
        s.strstart += s.match_length;
        s.match_length = 0;
      } else {
        /* No match, output a literal byte */
        //Tracevv((stderr,"%c", s->window[s->strstart]));
        /*** _tr_tally_lit(s, s.window[s.strstart], bflush); ***/
        bflush = trees._tr_tally(s, 0, s.window[s.strstart]);
  
        s.lookahead--;
        s.strstart++;
      }
      if (bflush) {
        /*** FLUSH_BLOCK(s, 0); ***/
        flush_block_only(s, false);
        if (s.strm.avail_out === 0) {
          return BS_NEED_MORE;
        }
        /***/
      }
    }
    s.insert = 0;
    if (flush === Z_FINISH) {
      /*** FLUSH_BLOCK(s, 1); ***/
      flush_block_only(s, true);
      if (s.strm.avail_out === 0) {
        return BS_FINISH_STARTED;
      }
      /***/
      return BS_FINISH_DONE;
    }
    if (s.last_lit) {
      /*** FLUSH_BLOCK(s, 0); ***/
      flush_block_only(s, false);
      if (s.strm.avail_out === 0) {
        return BS_NEED_MORE;
      }
      /***/
    }
    return BS_BLOCK_DONE;
  }
  
  /* ===========================================================================
   * For Z_HUFFMAN_ONLY, do not look for matches.  Do not maintain a hash table.
   * (It will be regenerated if this run of deflate switches away from Huffman.)
   */
  function deflate_huff(s, flush) {
    var bflush;             /* set if current block must be flushed */
  
    for (;;) {
      /* Make sure that we have a literal to write. */
      if (s.lookahead === 0) {
        fill_window(s);
        if (s.lookahead === 0) {
          if (flush === Z_NO_FLUSH) {
            return BS_NEED_MORE;
          }
          break;      /* flush the current block */
        }
      }
  
      /* Output a literal byte */
      s.match_length = 0;
      //Tracevv((stderr,"%c", s->window[s->strstart]));
      /*** _tr_tally_lit(s, s.window[s.strstart], bflush); ***/
      bflush = trees._tr_tally(s, 0, s.window[s.strstart]);
      s.lookahead--;
      s.strstart++;
      if (bflush) {
        /*** FLUSH_BLOCK(s, 0); ***/
        flush_block_only(s, false);
        if (s.strm.avail_out === 0) {
          return BS_NEED_MORE;
        }
        /***/
      }
    }
    s.insert = 0;
    if (flush === Z_FINISH) {
      /*** FLUSH_BLOCK(s, 1); ***/
      flush_block_only(s, true);
      if (s.strm.avail_out === 0) {
        return BS_FINISH_STARTED;
      }
      /***/
      return BS_FINISH_DONE;
    }
    if (s.last_lit) {
      /*** FLUSH_BLOCK(s, 0); ***/
      flush_block_only(s, false);
      if (s.strm.avail_out === 0) {
        return BS_NEED_MORE;
      }
      /***/
    }
    return BS_BLOCK_DONE;
  }
  
  /* Values for max_lazy_match, good_match and max_chain_length, depending on
   * the desired pack level (0..9). The values given below have been tuned to
   * exclude worst case performance for pathological files. Better values may be
   * found for specific files.
   */
  function Config(good_length, max_lazy, nice_length, max_chain, func) {
    this.good_length = good_length;
    this.max_lazy = max_lazy;
    this.nice_length = nice_length;
    this.max_chain = max_chain;
    this.func = func;
  }
  
  var configuration_table;
  
  configuration_table = [
    /*      good lazy nice chain */
    new Config(0, 0, 0, 0, deflate_stored),          /* 0 store only */
    new Config(4, 4, 8, 4, deflate_fast),            /* 1 max speed, no lazy matches */
    new Config(4, 5, 16, 8, deflate_fast),           /* 2 */
    new Config(4, 6, 32, 32, deflate_fast),          /* 3 */
  
    new Config(4, 4, 16, 16, deflate_slow),          /* 4 lazy matches */
    new Config(8, 16, 32, 32, deflate_slow),         /* 5 */
    new Config(8, 16, 128, 128, deflate_slow),       /* 6 */
    new Config(8, 32, 128, 256, deflate_slow),       /* 7 */
    new Config(32, 128, 258, 1024, deflate_slow),    /* 8 */
    new Config(32, 258, 258, 4096, deflate_slow)     /* 9 max compression */
  ];
  
  
  /* ===========================================================================
   * Initialize the "longest match" routines for a new zlib stream
   */
  function lm_init(s) {
    s.window_size = 2 * s.w_size;
  
    /*** CLEAR_HASH(s); ***/
    zero(s.head); // Fill with NIL (= 0);
  
    /* Set the default configuration parameters:
     */
    s.max_lazy_match = configuration_table[s.level].max_lazy;
    s.good_match = configuration_table[s.level].good_length;
    s.nice_match = configuration_table[s.level].nice_length;
    s.max_chain_length = configuration_table[s.level].max_chain;
  
    s.strstart = 0;
    s.block_start = 0;
    s.lookahead = 0;
    s.insert = 0;
    s.match_length = s.prev_length = MIN_MATCH - 1;
    s.match_available = 0;
    s.ins_h = 0;
  }
  
  
  function DeflateState() {
    this.strm = null;            /* pointer back to this zlib stream */
    this.status = 0;            /* as the name implies */
    this.pending_buf = null;      /* output still pending */
    this.pending_buf_size = 0;  /* size of pending_buf */
    this.pending_out = 0;       /* next pending byte to output to the stream */
    this.pending = 0;           /* nb of bytes in the pending buffer */
    this.wrap = 0;              /* bit 0 true for zlib, bit 1 true for gzip */
    this.gzhead = null;         /* gzip header information to write */
    this.gzindex = 0;           /* where in extra, name, or comment */
    this.method = Z_DEFLATED; /* can only be DEFLATED */
    this.last_flush = -1;   /* value of flush param for previous deflate call */
  
    this.w_size = 0;  /* LZ77 window size (32K by default) */
    this.w_bits = 0;  /* log2(w_size)  (8..16) */
    this.w_mask = 0;  /* w_size - 1 */
  
    this.window = null;
    /* Sliding window. Input bytes are read into the second half of the window,
     * and move to the first half later to keep a dictionary of at least wSize
     * bytes. With this organization, matches are limited to a distance of
     * wSize-MAX_MATCH bytes, but this ensures that IO is always
     * performed with a length multiple of the block size.
     */
  
    this.window_size = 0;
    /* Actual size of window: 2*wSize, except when the user input buffer
     * is directly used as sliding window.
     */
  
    this.prev = null;
    /* Link to older string with same hash index. To limit the size of this
     * array to 64K, this link is maintained only for the last 32K strings.
     * An index in this array is thus a window index modulo 32K.
     */
  
    this.head = null;   /* Heads of the hash chains or NIL. */
  
    this.ins_h = 0;       /* hash index of string to be inserted */
    this.hash_size = 0;   /* number of elements in hash table */
    this.hash_bits = 0;   /* log2(hash_size) */
    this.hash_mask = 0;   /* hash_size-1 */
  
    this.hash_shift = 0;
    /* Number of bits by which ins_h must be shifted at each input
     * step. It must be such that after MIN_MATCH steps, the oldest
     * byte no longer takes part in the hash key, that is:
     *   hash_shift * MIN_MATCH >= hash_bits
     */
  
    this.block_start = 0;
    /* Window position at the beginning of the current output block. Gets
     * negative when the window is moved backwards.
     */
  
    this.match_length = 0;      /* length of best match */
    this.prev_match = 0;        /* previous match */
    this.match_available = 0;   /* set if previous match exists */
    this.strstart = 0;          /* start of string to insert */
    this.match_start = 0;       /* start of matching string */
    this.lookahead = 0;         /* number of valid bytes ahead in window */
  
    this.prev_length = 0;
    /* Length of the best match at previous step. Matches not greater than this
     * are discarded. This is used in the lazy match evaluation.
     */
  
    this.max_chain_length = 0;
    /* To speed up deflation, hash chains are never searched beyond this
     * length.  A higher limit improves compression ratio but degrades the
     * speed.
     */
  
    this.max_lazy_match = 0;
    /* Attempt to find a better match only when the current match is strictly
     * smaller than this value. This mechanism is used only for compression
     * levels >= 4.
     */
    // That's alias to max_lazy_match, don't use directly
    //this.max_insert_length = 0;
    /* Insert new strings in the hash table only if the match length is not
     * greater than this length. This saves time but degrades compression.
     * max_insert_length is used only for compression levels <= 3.
     */
  
    this.level = 0;     /* compression level (1..9) */
    this.strategy = 0;  /* favor or force Huffman coding*/
  
    this.good_match = 0;
    /* Use a faster search when the previous match is longer than this */
  
    this.nice_match = 0; /* Stop searching when current match exceeds this */
  
                /* used by trees.c: */
  
    /* Didn't use ct_data typedef below to suppress compiler warning */
  
    // struct ct_data_s dyn_ltree[HEAP_SIZE];   /* literal and length tree */
    // struct ct_data_s dyn_dtree[2*D_CODES+1]; /* distance tree */
    // struct ct_data_s bl_tree[2*BL_CODES+1];  /* Huffman tree for bit lengths */
  
    // Use flat array of DOUBLE size, with interleaved fata,
    // because JS does not support effective
    this.dyn_ltree  = new utils.Buf16(HEAP_SIZE * 2);
    this.dyn_dtree  = new utils.Buf16((2 * D_CODES + 1) * 2);
    this.bl_tree    = new utils.Buf16((2 * BL_CODES + 1) * 2);
    zero(this.dyn_ltree);
    zero(this.dyn_dtree);
    zero(this.bl_tree);
  
    this.l_desc   = null;         /* desc. for literal tree */
    this.d_desc   = null;         /* desc. for distance tree */
    this.bl_desc  = null;         /* desc. for bit length tree */
  
    //ush bl_count[MAX_BITS+1];
    this.bl_count = new utils.Buf16(MAX_BITS + 1);
    /* number of codes at each bit length for an optimal tree */
  
    //int heap[2*L_CODES+1];      /* heap used to build the Huffman trees */
    this.heap = new utils.Buf16(2 * L_CODES + 1);  /* heap used to build the Huffman trees */
    zero(this.heap);
  
    this.heap_len = 0;               /* number of elements in the heap */
    this.heap_max = 0;               /* element of largest frequency */
    /* The sons of heap[n] are heap[2*n] and heap[2*n+1]. heap[0] is not used.
     * The same heap array is used to build all trees.
     */
  
    this.depth = new utils.Buf16(2 * L_CODES + 1); //uch depth[2*L_CODES+1];
    zero(this.depth);
    /* Depth of each subtree used as tie breaker for trees of equal frequency
     */
  
    this.l_buf = 0;          /* buffer index for literals or lengths */
  
    this.lit_bufsize = 0;
    /* Size of match buffer for literals/lengths.  There are 4 reasons for
     * limiting lit_bufsize to 64K:
     *   - frequencies can be kept in 16 bit counters
     *   - if compression is not successful for the first block, all input
     *     data is still in the window so we can still emit a stored block even
     *     when input comes from standard input.  (This can also be done for
     *     all blocks if lit_bufsize is not greater than 32K.)
     *   - if compression is not successful for a file smaller than 64K, we can
     *     even emit a stored file instead of a stored block (saving 5 bytes).
     *     This is applicable only for zip (not gzip or zlib).
     *   - creating new Huffman trees less frequently may not provide fast
     *     adaptation to changes in the input data statistics. (Take for
     *     example a binary file with poorly compressible code followed by
     *     a highly compressible string table.) Smaller buffer sizes give
     *     fast adaptation but have of course the overhead of transmitting
     *     trees more frequently.
     *   - I can't count above 4
     */
  
    this.last_lit = 0;      /* running index in l_buf */
  
    this.d_buf = 0;
    /* Buffer index for distances. To simplify the code, d_buf and l_buf have
     * the same number of elements. To use different lengths, an extra flag
     * array would be necessary.
     */
  
    this.opt_len = 0;       /* bit length of current block with optimal trees */
    this.static_len = 0;    /* bit length of current block with static trees */
    this.matches = 0;       /* number of string matches in current block */
    this.insert = 0;        /* bytes at end of window left to insert */
  
  
    this.bi_buf = 0;
    /* Output buffer. bits are inserted starting at the bottom (least
     * significant bits).
     */
    this.bi_valid = 0;
    /* Number of valid bits in bi_buf.  All bits above the last valid bit
     * are always zero.
     */
  
    // Used for window memory init. We safely ignore it for JS. That makes
    // sense only for pointers and memory check tools.
    //this.high_water = 0;
    /* High water mark offset in window for initialized bytes -- bytes above
     * this are set to zero in order to avoid memory check warnings when
     * longest match routines access bytes past the input.  This is then
     * updated to the new high water mark.
     */
  }
  
  
  function deflateResetKeep(strm) {
    var s;
  
    if (!strm || !strm.state) {
      return err(strm, Z_STREAM_ERROR);
    }
  
    strm.total_in = strm.total_out = 0;
    strm.data_type = Z_UNKNOWN;
  
    s = strm.state;
    s.pending = 0;
    s.pending_out = 0;
  
    if (s.wrap < 0) {
      s.wrap = -s.wrap;
      /* was made negative by deflate(..., Z_FINISH); */
    }
    s.status = (s.wrap ? INIT_STATE : BUSY_STATE);
    strm.adler = (s.wrap === 2) ?
      0  // crc32(0, Z_NULL, 0)
    :
      1; // adler32(0, Z_NULL, 0)
    s.last_flush = Z_NO_FLUSH;
    trees._tr_init(s);
    return Z_OK;
  }
  
  
  function deflateReset(strm) {
    var ret = deflateResetKeep(strm);
    if (ret === Z_OK) {
      lm_init(strm.state);
    }
    return ret;
  }
  
  
  function deflateSetHeader(strm, head) {
    if (!strm || !strm.state) { return Z_STREAM_ERROR; }
    if (strm.state.wrap !== 2) { return Z_STREAM_ERROR; }
    strm.state.gzhead = head;
    return Z_OK;
  }
  
  
  function deflateInit2(strm, level, method, windowBits, memLevel, strategy) {
    if (!strm) { // === Z_NULL
      return Z_STREAM_ERROR;
    }
    var wrap = 1;
  
    if (level === Z_DEFAULT_COMPRESSION) {
      level = 6;
    }
  
    if (windowBits < 0) { /* suppress zlib wrapper */
      wrap = 0;
      windowBits = -windowBits;
    }
  
    else if (windowBits > 15) {
      wrap = 2;           /* write gzip wrapper instead */
      windowBits -= 16;
    }
  
  
    if (memLevel < 1 || memLevel > MAX_MEM_LEVEL || method !== Z_DEFLATED ||
      windowBits < 8 || windowBits > 15 || level < 0 || level > 9 ||
      strategy < 0 || strategy > Z_FIXED) {
      return err(strm, Z_STREAM_ERROR);
    }
  
  
    if (windowBits === 8) {
      windowBits = 9;
    }
    /* until 256-byte window bug fixed */
  
    var s = new DeflateState();
  
    strm.state = s;
    s.strm = strm;
  
    s.wrap = wrap;
    s.gzhead = null;
    s.w_bits = windowBits;
    s.w_size = 1 << s.w_bits;
    s.w_mask = s.w_size - 1;
  
    s.hash_bits = memLevel + 7;
    s.hash_size = 1 << s.hash_bits;
    s.hash_mask = s.hash_size - 1;
    s.hash_shift = ~~((s.hash_bits + MIN_MATCH - 1) / MIN_MATCH);
  
    s.window = new utils.Buf8(s.w_size * 2);
    s.head = new utils.Buf16(s.hash_size);
    s.prev = new utils.Buf16(s.w_size);
  
    // Don't need mem init magic for JS.
    //s.high_water = 0;  /* nothing written to s->window yet */
  
    s.lit_bufsize = 1 << (memLevel + 6); /* 16K elements by default */
  
    s.pending_buf_size = s.lit_bufsize * 4;
  
    //overlay = (ushf *) ZALLOC(strm, s->lit_bufsize, sizeof(ush)+2);
    //s->pending_buf = (uchf *) overlay;
    s.pending_buf = new utils.Buf8(s.pending_buf_size);
  
    // It is offset from `s.pending_buf` (size is `s.lit_bufsize * 2`)
    //s->d_buf = overlay + s->lit_bufsize/sizeof(ush);
    s.d_buf = 1 * s.lit_bufsize;
  
    //s->l_buf = s->pending_buf + (1+sizeof(ush))*s->lit_bufsize;
    s.l_buf = (1 + 2) * s.lit_bufsize;
  
    s.level = level;
    s.strategy = strategy;
    s.method = method;
  
    return deflateReset(strm);
  }
  
  function deflateInit(strm, level) {
    return deflateInit2(strm, level, Z_DEFLATED, MAX_WBITS, DEF_MEM_LEVEL, Z_DEFAULT_STRATEGY);
  }
  
  
  function deflate(strm, flush) {
    var old_flush, s;
    var beg, val; // for gzip header write only
  
    if (!strm || !strm.state ||
      flush > Z_BLOCK || flush < 0) {
      return strm ? err(strm, Z_STREAM_ERROR) : Z_STREAM_ERROR;
    }
  
    s = strm.state;
  
    if (!strm.output ||
        (!strm.input && strm.avail_in !== 0) ||
        (s.status === FINISH_STATE && flush !== Z_FINISH)) {
      return err(strm, (strm.avail_out === 0) ? Z_BUF_ERROR : Z_STREAM_ERROR);
    }
  
    s.strm = strm; /* just in case */
    old_flush = s.last_flush;
    s.last_flush = flush;
  
    /* Write the header */
    if (s.status === INIT_STATE) {
  
      if (s.wrap === 2) { // GZIP header
        strm.adler = 0;  //crc32(0L, Z_NULL, 0);
        put_byte(s, 31);
        put_byte(s, 139);
        put_byte(s, 8);
        if (!s.gzhead) { // s->gzhead == Z_NULL
          put_byte(s, 0);
          put_byte(s, 0);
          put_byte(s, 0);
          put_byte(s, 0);
          put_byte(s, 0);
          put_byte(s, s.level === 9 ? 2 :
                      (s.strategy >= Z_HUFFMAN_ONLY || s.level < 2 ?
                       4 : 0));
          put_byte(s, OS_CODE);
          s.status = BUSY_STATE;
        }
        else {
          put_byte(s, (s.gzhead.text ? 1 : 0) +
                      (s.gzhead.hcrc ? 2 : 0) +
                      (!s.gzhead.extra ? 0 : 4) +
                      (!s.gzhead.name ? 0 : 8) +
                      (!s.gzhead.comment ? 0 : 16)
          );
          put_byte(s, s.gzhead.time & 0xff);
          put_byte(s, (s.gzhead.time >> 8) & 0xff);
          put_byte(s, (s.gzhead.time >> 16) & 0xff);
          put_byte(s, (s.gzhead.time >> 24) & 0xff);
          put_byte(s, s.level === 9 ? 2 :
                      (s.strategy >= Z_HUFFMAN_ONLY || s.level < 2 ?
                       4 : 0));
          put_byte(s, s.gzhead.os & 0xff);
          if (s.gzhead.extra && s.gzhead.extra.length) {
            put_byte(s, s.gzhead.extra.length & 0xff);
            put_byte(s, (s.gzhead.extra.length >> 8) & 0xff);
          }
          if (s.gzhead.hcrc) {
            strm.adler = crc32(strm.adler, s.pending_buf, s.pending, 0);
          }
          s.gzindex = 0;
          s.status = EXTRA_STATE;
        }
      }
      else // DEFLATE header
      {
        var header = (Z_DEFLATED + ((s.w_bits - 8) << 4)) << 8;
        var level_flags = -1;
  
        if (s.strategy >= Z_HUFFMAN_ONLY || s.level < 2) {
          level_flags = 0;
        } else if (s.level < 6) {
          level_flags = 1;
        } else if (s.level === 6) {
          level_flags = 2;
        } else {
          level_flags = 3;
        }
        header |= (level_flags << 6);
        if (s.strstart !== 0) { header |= PRESET_DICT; }
        header += 31 - (header % 31);
  
        s.status = BUSY_STATE;
        putShortMSB(s, header);
  
        /* Save the adler32 of the preset dictionary: */
        if (s.strstart !== 0) {
          putShortMSB(s, strm.adler >>> 16);
          putShortMSB(s, strm.adler & 0xffff);
        }
        strm.adler = 1; // adler32(0L, Z_NULL, 0);
      }
    }
  
  //#ifdef GZIP
    if (s.status === EXTRA_STATE) {
      if (s.gzhead.extra/* != Z_NULL*/) {
        beg = s.pending;  /* start of bytes to update crc */
  
        while (s.gzindex < (s.gzhead.extra.length & 0xffff)) {
          if (s.pending === s.pending_buf_size) {
            if (s.gzhead.hcrc && s.pending > beg) {
              strm.adler = crc32(strm.adler, s.pending_buf, s.pending - beg, beg);
            }
            flush_pending(strm);
            beg = s.pending;
            if (s.pending === s.pending_buf_size) {
              break;
            }
          }
          put_byte(s, s.gzhead.extra[s.gzindex] & 0xff);
          s.gzindex++;
        }
        if (s.gzhead.hcrc && s.pending > beg) {
          strm.adler = crc32(strm.adler, s.pending_buf, s.pending - beg, beg);
        }
        if (s.gzindex === s.gzhead.extra.length) {
          s.gzindex = 0;
          s.status = NAME_STATE;
        }
      }
      else {
        s.status = NAME_STATE;
      }
    }
    if (s.status === NAME_STATE) {
      if (s.gzhead.name/* != Z_NULL*/) {
        beg = s.pending;  /* start of bytes to update crc */
        //int val;
  
        do {
          if (s.pending === s.pending_buf_size) {
            if (s.gzhead.hcrc && s.pending > beg) {
              strm.adler = crc32(strm.adler, s.pending_buf, s.pending - beg, beg);
            }
            flush_pending(strm);
            beg = s.pending;
            if (s.pending === s.pending_buf_size) {
              val = 1;
              break;
            }
          }
          // JS specific: little magic to add zero terminator to end of string
          if (s.gzindex < s.gzhead.name.length) {
            val = s.gzhead.name.charCodeAt(s.gzindex++) & 0xff;
          } else {
            val = 0;
          }
          put_byte(s, val);
        } while (val !== 0);
  
        if (s.gzhead.hcrc && s.pending > beg) {
          strm.adler = crc32(strm.adler, s.pending_buf, s.pending - beg, beg);
        }
        if (val === 0) {
          s.gzindex = 0;
          s.status = COMMENT_STATE;
        }
      }
      else {
        s.status = COMMENT_STATE;
      }
    }
    if (s.status === COMMENT_STATE) {
      if (s.gzhead.comment/* != Z_NULL*/) {
        beg = s.pending;  /* start of bytes to update crc */
        //int val;
  
        do {
          if (s.pending === s.pending_buf_size) {
            if (s.gzhead.hcrc && s.pending > beg) {
              strm.adler = crc32(strm.adler, s.pending_buf, s.pending - beg, beg);
            }
            flush_pending(strm);
            beg = s.pending;
            if (s.pending === s.pending_buf_size) {
              val = 1;
              break;
            }
          }
          // JS specific: little magic to add zero terminator to end of string
          if (s.gzindex < s.gzhead.comment.length) {
            val = s.gzhead.comment.charCodeAt(s.gzindex++) & 0xff;
          } else {
            val = 0;
          }
          put_byte(s, val);
        } while (val !== 0);
  
        if (s.gzhead.hcrc && s.pending > beg) {
          strm.adler = crc32(strm.adler, s.pending_buf, s.pending - beg, beg);
        }
        if (val === 0) {
          s.status = HCRC_STATE;
        }
      }
      else {
        s.status = HCRC_STATE;
      }
    }
    if (s.status === HCRC_STATE) {
      if (s.gzhead.hcrc) {
        if (s.pending + 2 > s.pending_buf_size) {
          flush_pending(strm);
        }
        if (s.pending + 2 <= s.pending_buf_size) {
          put_byte(s, strm.adler & 0xff);
          put_byte(s, (strm.adler >> 8) & 0xff);
          strm.adler = 0; //crc32(0L, Z_NULL, 0);
          s.status = BUSY_STATE;
        }
      }
      else {
        s.status = BUSY_STATE;
      }
    }
  //#endif
  
    /* Flush as much pending output as possible */
    if (s.pending !== 0) {
      flush_pending(strm);
      if (strm.avail_out === 0) {
        /* Since avail_out is 0, deflate will be called again with
         * more output space, but possibly with both pending and
         * avail_in equal to zero. There won't be anything to do,
         * but this is not an error situation so make sure we
         * return OK instead of BUF_ERROR at next call of deflate:
         */
        s.last_flush = -1;
        return Z_OK;
      }
  
      /* Make sure there is something to do and avoid duplicate consecutive
       * flushes. For repeated and useless calls with Z_FINISH, we keep
       * returning Z_STREAM_END instead of Z_BUF_ERROR.
       */
    } else if (strm.avail_in === 0 && rank(flush) <= rank(old_flush) &&
      flush !== Z_FINISH) {
      return err(strm, Z_BUF_ERROR);
    }
  
    /* User must not provide more input after the first FINISH: */
    if (s.status === FINISH_STATE && strm.avail_in !== 0) {
      return err(strm, Z_BUF_ERROR);
    }
  
    /* Start a new block or continue the current one.
     */
    if (strm.avail_in !== 0 || s.lookahead !== 0 ||
      (flush !== Z_NO_FLUSH && s.status !== FINISH_STATE)) {
      var bstate = (s.strategy === Z_HUFFMAN_ONLY) ? deflate_huff(s, flush) :
        (s.strategy === Z_RLE ? deflate_rle(s, flush) :
          configuration_table[s.level].func(s, flush));
  
      if (bstate === BS_FINISH_STARTED || bstate === BS_FINISH_DONE) {
        s.status = FINISH_STATE;
      }
      if (bstate === BS_NEED_MORE || bstate === BS_FINISH_STARTED) {
        if (strm.avail_out === 0) {
          s.last_flush = -1;
          /* avoid BUF_ERROR next call, see above */
        }
        return Z_OK;
        /* If flush != Z_NO_FLUSH && avail_out == 0, the next call
         * of deflate should use the same flush parameter to make sure
         * that the flush is complete. So we don't have to output an
         * empty block here, this will be done at next call. This also
         * ensures that for a very small output buffer, we emit at most
         * one empty block.
         */
      }
      if (bstate === BS_BLOCK_DONE) {
        if (flush === Z_PARTIAL_FLUSH) {
          trees._tr_align(s);
        }
        else if (flush !== Z_BLOCK) { /* FULL_FLUSH or SYNC_FLUSH */
  
          trees._tr_stored_block(s, 0, 0, false);
          /* For a full flush, this empty block will be recognized
           * as a special marker by inflate_sync().
           */
          if (flush === Z_FULL_FLUSH) {
            /*** CLEAR_HASH(s); ***/             /* forget history */
            zero(s.head); // Fill with NIL (= 0);
  
            if (s.lookahead === 0) {
              s.strstart = 0;
              s.block_start = 0;
              s.insert = 0;
            }
          }
        }
        flush_pending(strm);
        if (strm.avail_out === 0) {
          s.last_flush = -1; /* avoid BUF_ERROR at next call, see above */
          return Z_OK;
        }
      }
    }
    //Assert(strm->avail_out > 0, "bug2");
    //if (strm.avail_out <= 0) { throw new Error("bug2");}
  
    if (flush !== Z_FINISH) { return Z_OK; }
    if (s.wrap <= 0) { return Z_STREAM_END; }
  
    /* Write the trailer */
    if (s.wrap === 2) {
      put_byte(s, strm.adler & 0xff);
      put_byte(s, (strm.adler >> 8) & 0xff);
      put_byte(s, (strm.adler >> 16) & 0xff);
      put_byte(s, (strm.adler >> 24) & 0xff);
      put_byte(s, strm.total_in & 0xff);
      put_byte(s, (strm.total_in >> 8) & 0xff);
      put_byte(s, (strm.total_in >> 16) & 0xff);
      put_byte(s, (strm.total_in >> 24) & 0xff);
    }
    else
    {
      putShortMSB(s, strm.adler >>> 16);
      putShortMSB(s, strm.adler & 0xffff);
    }
  
    flush_pending(strm);
    /* If avail_out is zero, the application will call deflate again
     * to flush the rest.
     */
    if (s.wrap > 0) { s.wrap = -s.wrap; }
    /* write the trailer only once! */
    return s.pending !== 0 ? Z_OK : Z_STREAM_END;
  }
  
  function deflateEnd(strm) {
    var status;
  
    if (!strm/*== Z_NULL*/ || !strm.state/*== Z_NULL*/) {
      return Z_STREAM_ERROR;
    }
  
    status = strm.state.status;
    if (status !== INIT_STATE &&
      status !== EXTRA_STATE &&
      status !== NAME_STATE &&
      status !== COMMENT_STATE &&
      status !== HCRC_STATE &&
      status !== BUSY_STATE &&
      status !== FINISH_STATE
    ) {
      return err(strm, Z_STREAM_ERROR);
    }
  
    strm.state = null;
  
    return status === BUSY_STATE ? err(strm, Z_DATA_ERROR) : Z_OK;
  }
  
  
  /* =========================================================================
   * Initializes the compression dictionary from the given byte
   * sequence without producing any compressed output.
   */
  function deflateSetDictionary(strm, dictionary) {
    var dictLength = dictionary.length;
  
    var s;
    var str, n;
    var wrap;
    var avail;
    var next;
    var input;
    var tmpDict;
  
    if (!strm/*== Z_NULL*/ || !strm.state/*== Z_NULL*/) {
      return Z_STREAM_ERROR;
    }
  
    s = strm.state;
    wrap = s.wrap;
  
    if (wrap === 2 || (wrap === 1 && s.status !== INIT_STATE) || s.lookahead) {
      return Z_STREAM_ERROR;
    }
  
    /* when using zlib wrappers, compute Adler-32 for provided dictionary */
    if (wrap === 1) {
      /* adler32(strm->adler, dictionary, dictLength); */
      strm.adler = adler32(strm.adler, dictionary, dictLength, 0);
    }
  
    s.wrap = 0;   /* avoid computing Adler-32 in read_buf */
  
    /* if dictionary would fill window, just replace the history */
    if (dictLength >= s.w_size) {
      if (wrap === 0) {            /* already empty otherwise */
        /*** CLEAR_HASH(s); ***/
        zero(s.head); // Fill with NIL (= 0);
        s.strstart = 0;
        s.block_start = 0;
        s.insert = 0;
      }
      /* use the tail */
      // dictionary = dictionary.slice(dictLength - s.w_size);
      tmpDict = new utils.Buf8(s.w_size);
      utils.arraySet(tmpDict, dictionary, dictLength - s.w_size, s.w_size, 0);
      dictionary = tmpDict;
      dictLength = s.w_size;
    }
    /* insert dictionary into window and hash */
    avail = strm.avail_in;
    next = strm.next_in;
    input = strm.input;
    strm.avail_in = dictLength;
    strm.next_in = 0;
    strm.input = dictionary;
    fill_window(s);
    while (s.lookahead >= MIN_MATCH) {
      str = s.strstart;
      n = s.lookahead - (MIN_MATCH - 1);
      do {
        /* UPDATE_HASH(s, s->ins_h, s->window[str + MIN_MATCH-1]); */
        s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[str + MIN_MATCH - 1]) & s.hash_mask;
  
        s.prev[str & s.w_mask] = s.head[s.ins_h];
  
        s.head[s.ins_h] = str;
        str++;
      } while (--n);
      s.strstart = str;
      s.lookahead = MIN_MATCH - 1;
      fill_window(s);
    }
    s.strstart += s.lookahead;
    s.block_start = s.strstart;
    s.insert = s.lookahead;
    s.lookahead = 0;
    s.match_length = s.prev_length = MIN_MATCH - 1;
    s.match_available = 0;
    strm.next_in = next;
    strm.input = input;
    strm.avail_in = avail;
    s.wrap = wrap;
    return Z_OK;
  }
  
  
  exports.deflateInit = deflateInit;
  exports.deflateInit2 = deflateInit2;
  exports.deflateReset = deflateReset;
  exports.deflateResetKeep = deflateResetKeep;
  exports.deflateSetHeader = deflateSetHeader;
  exports.deflate = deflate;
  exports.deflateEnd = deflateEnd;
  exports.deflateSetDictionary = deflateSetDictionary;
  exports.deflateInfo = 'pako deflate (from Nodeca project)';
  
  /* Not implemented
  exports.deflateBound = deflateBound;
  exports.deflateCopy = deflateCopy;
  exports.deflateParams = deflateParams;
  exports.deflatePending = deflatePending;
  exports.deflatePrime = deflatePrime;
  exports.deflateTune = deflateTune;
  */
  
  },{"../utils/common":20,"./adler32":21,"./crc32":23,"./messages":28,"./trees":29}],25:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  // See state defs from inflate.js
  var BAD = 30;       /* got a data error -- remain here until reset */
  var TYPE = 12;      /* i: waiting for type bits, including last-flag bit */
  
  /*
     Decode literal, length, and distance codes and write out the resulting
     literal and match bytes until either not enough input or output is
     available, an end-of-block is encountered, or a data error is encountered.
     When large enough input and output buffers are supplied to inflate(), for
     example, a 16K input buffer and a 64K output buffer, more than 95% of the
     inflate execution time is spent in this routine.
  
     Entry assumptions:
  
          state.mode === LEN
          strm.avail_in >= 6
          strm.avail_out >= 258
          start >= strm.avail_out
          state.bits < 8
  
     On return, state.mode is one of:
  
          LEN -- ran out of enough output space or enough available input
          TYPE -- reached end of block code, inflate() to interpret next block
          BAD -- error in block data
  
     Notes:
  
      - The maximum input bits used by a length/distance pair is 15 bits for the
        length code, 5 bits for the length extra, 15 bits for the distance code,
        and 13 bits for the distance extra.  This totals 48 bits, or six bytes.
        Therefore if strm.avail_in >= 6, then there is enough input to avoid
        checking for available input while decoding.
  
      - The maximum bytes that a single length/distance pair can output is 258
        bytes, which is the maximum length that can be coded.  inflate_fast()
        requires strm.avail_out >= 258 for each loop to avoid checking for
        output space.
   */
  module.exports = function inflate_fast(strm, start) {
    var state;
    var _in;                    /* local strm.input */
    var last;                   /* have enough input while in < last */
    var _out;                   /* local strm.output */
    var beg;                    /* inflate()'s initial strm.output */
    var end;                    /* while out < end, enough space available */
  //#ifdef INFLATE_STRICT
    var dmax;                   /* maximum distance from zlib header */
  //#endif
    var wsize;                  /* window size or zero if not using window */
    var whave;                  /* valid bytes in the window */
    var wnext;                  /* window write index */
    // Use `s_window` instead `window`, avoid conflict with instrumentation tools
    var s_window;               /* allocated sliding window, if wsize != 0 */
    var hold;                   /* local strm.hold */
    var bits;                   /* local strm.bits */
    var lcode;                  /* local strm.lencode */
    var dcode;                  /* local strm.distcode */
    var lmask;                  /* mask for first level of length codes */
    var dmask;                  /* mask for first level of distance codes */
    var here;                   /* retrieved table entry */
    var op;                     /* code bits, operation, extra bits, or */
                                /*  window position, window bytes to copy */
    var len;                    /* match length, unused bytes */
    var dist;                   /* match distance */
    var from;                   /* where to copy match from */
    var from_source;
  
  
    var input, output; // JS specific, because we have no pointers
  
    /* copy state to local variables */
    state = strm.state;
    //here = state.here;
    _in = strm.next_in;
    input = strm.input;
    last = _in + (strm.avail_in - 5);
    _out = strm.next_out;
    output = strm.output;
    beg = _out - (start - strm.avail_out);
    end = _out + (strm.avail_out - 257);
  //#ifdef INFLATE_STRICT
    dmax = state.dmax;
  //#endif
    wsize = state.wsize;
    whave = state.whave;
    wnext = state.wnext;
    s_window = state.window;
    hold = state.hold;
    bits = state.bits;
    lcode = state.lencode;
    dcode = state.distcode;
    lmask = (1 << state.lenbits) - 1;
    dmask = (1 << state.distbits) - 1;
  
  
    /* decode literals and length/distances until end-of-block or not enough
       input data or output space */
  
    top:
    do {
      if (bits < 15) {
        hold += input[_in++] << bits;
        bits += 8;
        hold += input[_in++] << bits;
        bits += 8;
      }
  
      here = lcode[hold & lmask];
  
      dolen:
      for (;;) { // Goto emulation
        op = here >>> 24/*here.bits*/;
        hold >>>= op;
        bits -= op;
        op = (here >>> 16) & 0xff/*here.op*/;
        if (op === 0) {                          /* literal */
          //Tracevv((stderr, here.val >= 0x20 && here.val < 0x7f ?
          //        "inflate:         literal '%c'\n" :
          //        "inflate:         literal 0x%02x\n", here.val));
          output[_out++] = here & 0xffff/*here.val*/;
        }
        else if (op & 16) {                     /* length base */
          len = here & 0xffff/*here.val*/;
          op &= 15;                           /* number of extra bits */
          if (op) {
            if (bits < op) {
              hold += input[_in++] << bits;
              bits += 8;
            }
            len += hold & ((1 << op) - 1);
            hold >>>= op;
            bits -= op;
          }
          //Tracevv((stderr, "inflate:         length %u\n", len));
          if (bits < 15) {
            hold += input[_in++] << bits;
            bits += 8;
            hold += input[_in++] << bits;
            bits += 8;
          }
          here = dcode[hold & dmask];
  
          dodist:
          for (;;) { // goto emulation
            op = here >>> 24/*here.bits*/;
            hold >>>= op;
            bits -= op;
            op = (here >>> 16) & 0xff/*here.op*/;
  
            if (op & 16) {                      /* distance base */
              dist = here & 0xffff/*here.val*/;
              op &= 15;                       /* number of extra bits */
              if (bits < op) {
                hold += input[_in++] << bits;
                bits += 8;
                if (bits < op) {
                  hold += input[_in++] << bits;
                  bits += 8;
                }
              }
              dist += hold & ((1 << op) - 1);
  //#ifdef INFLATE_STRICT
              if (dist > dmax) {
                strm.msg = 'invalid distance too far back';
                state.mode = BAD;
                break top;
              }
  //#endif
              hold >>>= op;
              bits -= op;
              //Tracevv((stderr, "inflate:         distance %u\n", dist));
              op = _out - beg;                /* max distance in output */
              if (dist > op) {                /* see if copy from window */
                op = dist - op;               /* distance back in window */
                if (op > whave) {
                  if (state.sane) {
                    strm.msg = 'invalid distance too far back';
                    state.mode = BAD;
                    break top;
                  }
  
  // (!) This block is disabled in zlib defaults,
  // don't enable it for binary compatibility
  //#ifdef INFLATE_ALLOW_INVALID_DISTANCE_TOOFAR_ARRR
  //                if (len <= op - whave) {
  //                  do {
  //                    output[_out++] = 0;
  //                  } while (--len);
  //                  continue top;
  //                }
  //                len -= op - whave;
  //                do {
  //                  output[_out++] = 0;
  //                } while (--op > whave);
  //                if (op === 0) {
  //                  from = _out - dist;
  //                  do {
  //                    output[_out++] = output[from++];
  //                  } while (--len);
  //                  continue top;
  //                }
  //#endif
                }
                from = 0; // window index
                from_source = s_window;
                if (wnext === 0) {           /* very common case */
                  from += wsize - op;
                  if (op < len) {         /* some from window */
                    len -= op;
                    do {
                      output[_out++] = s_window[from++];
                    } while (--op);
                    from = _out - dist;  /* rest from output */
                    from_source = output;
                  }
                }
                else if (wnext < op) {      /* wrap around window */
                  from += wsize + wnext - op;
                  op -= wnext;
                  if (op < len) {         /* some from end of window */
                    len -= op;
                    do {
                      output[_out++] = s_window[from++];
                    } while (--op);
                    from = 0;
                    if (wnext < len) {  /* some from start of window */
                      op = wnext;
                      len -= op;
                      do {
                        output[_out++] = s_window[from++];
                      } while (--op);
                      from = _out - dist;      /* rest from output */
                      from_source = output;
                    }
                  }
                }
                else {                      /* contiguous in window */
                  from += wnext - op;
                  if (op < len) {         /* some from window */
                    len -= op;
                    do {
                      output[_out++] = s_window[from++];
                    } while (--op);
                    from = _out - dist;  /* rest from output */
                    from_source = output;
                  }
                }
                while (len > 2) {
                  output[_out++] = from_source[from++];
                  output[_out++] = from_source[from++];
                  output[_out++] = from_source[from++];
                  len -= 3;
                }
                if (len) {
                  output[_out++] = from_source[from++];
                  if (len > 1) {
                    output[_out++] = from_source[from++];
                  }
                }
              }
              else {
                from = _out - dist;          /* copy direct from output */
                do {                        /* minimum length is three */
                  output[_out++] = output[from++];
                  output[_out++] = output[from++];
                  output[_out++] = output[from++];
                  len -= 3;
                } while (len > 2);
                if (len) {
                  output[_out++] = output[from++];
                  if (len > 1) {
                    output[_out++] = output[from++];
                  }
                }
              }
            }
            else if ((op & 64) === 0) {          /* 2nd level distance code */
              here = dcode[(here & 0xffff)/*here.val*/ + (hold & ((1 << op) - 1))];
              continue dodist;
            }
            else {
              strm.msg = 'invalid distance code';
              state.mode = BAD;
              break top;
            }
  
            break; // need to emulate goto via "continue"
          }
        }
        else if ((op & 64) === 0) {              /* 2nd level length code */
          here = lcode[(here & 0xffff)/*here.val*/ + (hold & ((1 << op) - 1))];
          continue dolen;
        }
        else if (op & 32) {                     /* end-of-block */
          //Tracevv((stderr, "inflate:         end of block\n"));
          state.mode = TYPE;
          break top;
        }
        else {
          strm.msg = 'invalid literal/length code';
          state.mode = BAD;
          break top;
        }
  
        break; // need to emulate goto via "continue"
      }
    } while (_in < last && _out < end);
  
    /* return unused bytes (on entry, bits < 8, so in won't go too far back) */
    len = bits >> 3;
    _in -= len;
    bits -= len << 3;
    hold &= (1 << bits) - 1;
  
    /* update state and return */
    strm.next_in = _in;
    strm.next_out = _out;
    strm.avail_in = (_in < last ? 5 + (last - _in) : 5 - (_in - last));
    strm.avail_out = (_out < end ? 257 + (end - _out) : 257 - (_out - end));
    state.hold = hold;
    state.bits = bits;
    return;
  };
  
  },{}],26:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  var utils         = require('../utils/common');
  var adler32       = require('./adler32');
  var crc32         = require('./crc32');
  var inflate_fast  = require('./inffast');
  var inflate_table = require('./inftrees');
  
  var CODES = 0;
  var LENS = 1;
  var DISTS = 2;
  
  /* Public constants ==========================================================*/
  /* ===========================================================================*/
  
  
  /* Allowed flush values; see deflate() and inflate() below for details */
  //var Z_NO_FLUSH      = 0;
  //var Z_PARTIAL_FLUSH = 1;
  //var Z_SYNC_FLUSH    = 2;
  //var Z_FULL_FLUSH    = 3;
  var Z_FINISH        = 4;
  var Z_BLOCK         = 5;
  var Z_TREES         = 6;
  
  
  /* Return codes for the compression/decompression functions. Negative values
   * are errors, positive values are used for special but normal events.
   */
  var Z_OK            = 0;
  var Z_STREAM_END    = 1;
  var Z_NEED_DICT     = 2;
  //var Z_ERRNO         = -1;
  var Z_STREAM_ERROR  = -2;
  var Z_DATA_ERROR    = -3;
  var Z_MEM_ERROR     = -4;
  var Z_BUF_ERROR     = -5;
  //var Z_VERSION_ERROR = -6;
  
  /* The deflate compression method */
  var Z_DEFLATED  = 8;
  
  
  /* STATES ====================================================================*/
  /* ===========================================================================*/
  
  
  var    HEAD = 1;       /* i: waiting for magic header */
  var    FLAGS = 2;      /* i: waiting for method and flags (gzip) */
  var    TIME = 3;       /* i: waiting for modification time (gzip) */
  var    OS = 4;         /* i: waiting for extra flags and operating system (gzip) */
  var    EXLEN = 5;      /* i: waiting for extra length (gzip) */
  var    EXTRA = 6;      /* i: waiting for extra bytes (gzip) */
  var    NAME = 7;       /* i: waiting for end of file name (gzip) */
  var    COMMENT = 8;    /* i: waiting for end of comment (gzip) */
  var    HCRC = 9;       /* i: waiting for header crc (gzip) */
  var    DICTID = 10;    /* i: waiting for dictionary check value */
  var    DICT = 11;      /* waiting for inflateSetDictionary() call */
  var        TYPE = 12;      /* i: waiting for type bits, including last-flag bit */
  var        TYPEDO = 13;    /* i: same, but skip check to exit inflate on new block */
  var        STORED = 14;    /* i: waiting for stored size (length and complement) */
  var        COPY_ = 15;     /* i/o: same as COPY below, but only first time in */
  var        COPY = 16;      /* i/o: waiting for input or output to copy stored block */
  var        TABLE = 17;     /* i: waiting for dynamic block table lengths */
  var        LENLENS = 18;   /* i: waiting for code length code lengths */
  var        CODELENS = 19;  /* i: waiting for length/lit and distance code lengths */
  var            LEN_ = 20;      /* i: same as LEN below, but only first time in */
  var            LEN = 21;       /* i: waiting for length/lit/eob code */
  var            LENEXT = 22;    /* i: waiting for length extra bits */
  var            DIST = 23;      /* i: waiting for distance code */
  var            DISTEXT = 24;   /* i: waiting for distance extra bits */
  var            MATCH = 25;     /* o: waiting for output space to copy string */
  var            LIT = 26;       /* o: waiting for output space to write literal */
  var    CHECK = 27;     /* i: waiting for 32-bit check value */
  var    LENGTH = 28;    /* i: waiting for 32-bit length (gzip) */
  var    DONE = 29;      /* finished check, done -- remain here until reset */
  var    BAD = 30;       /* got a data error -- remain here until reset */
  var    MEM = 31;       /* got an inflate() memory error -- remain here until reset */
  var    SYNC = 32;      /* looking for synchronization bytes to restart inflate() */
  
  /* ===========================================================================*/
  
  
  
  var ENOUGH_LENS = 852;
  var ENOUGH_DISTS = 592;
  //var ENOUGH =  (ENOUGH_LENS+ENOUGH_DISTS);
  
  var MAX_WBITS = 15;
  /* 32K LZ77 window */
  var DEF_WBITS = MAX_WBITS;
  
  
  function zswap32(q) {
    return  (((q >>> 24) & 0xff) +
            ((q >>> 8) & 0xff00) +
            ((q & 0xff00) << 8) +
            ((q & 0xff) << 24));
  }
  
  
  function InflateState() {
    this.mode = 0;             /* current inflate mode */
    this.last = false;          /* true if processing last block */
    this.wrap = 0;              /* bit 0 true for zlib, bit 1 true for gzip */
    this.havedict = false;      /* true if dictionary provided */
    this.flags = 0;             /* gzip header method and flags (0 if zlib) */
    this.dmax = 0;              /* zlib header max distance (INFLATE_STRICT) */
    this.check = 0;             /* protected copy of check value */
    this.total = 0;             /* protected copy of output count */
    // TODO: may be {}
    this.head = null;           /* where to save gzip header information */
  
    /* sliding window */
    this.wbits = 0;             /* log base 2 of requested window size */
    this.wsize = 0;             /* window size or zero if not using window */
    this.whave = 0;             /* valid bytes in the window */
    this.wnext = 0;             /* window write index */
    this.window = null;         /* allocated sliding window, if needed */
  
    /* bit accumulator */
    this.hold = 0;              /* input bit accumulator */
    this.bits = 0;              /* number of bits in "in" */
  
    /* for string and stored block copying */
    this.length = 0;            /* literal or length of data to copy */
    this.offset = 0;            /* distance back to copy string from */
  
    /* for table and code decoding */
    this.extra = 0;             /* extra bits needed */
  
    /* fixed and dynamic code tables */
    this.lencode = null;          /* starting table for length/literal codes */
    this.distcode = null;         /* starting table for distance codes */
    this.lenbits = 0;           /* index bits for lencode */
    this.distbits = 0;          /* index bits for distcode */
  
    /* dynamic table building */
    this.ncode = 0;             /* number of code length code lengths */
    this.nlen = 0;              /* number of length code lengths */
    this.ndist = 0;             /* number of distance code lengths */
    this.have = 0;              /* number of code lengths in lens[] */
    this.next = null;              /* next available space in codes[] */
  
    this.lens = new utils.Buf16(320); /* temporary storage for code lengths */
    this.work = new utils.Buf16(288); /* work area for code table building */
  
    /*
     because we don't have pointers in js, we use lencode and distcode directly
     as buffers so we don't need codes
    */
    //this.codes = new utils.Buf32(ENOUGH);       /* space for code tables */
    this.lendyn = null;              /* dynamic table for length/literal codes (JS specific) */
    this.distdyn = null;             /* dynamic table for distance codes (JS specific) */
    this.sane = 0;                   /* if false, allow invalid distance too far */
    this.back = 0;                   /* bits back of last unprocessed length/lit */
    this.was = 0;                    /* initial length of match */
  }
  
  function inflateResetKeep(strm) {
    var state;
  
    if (!strm || !strm.state) { return Z_STREAM_ERROR; }
    state = strm.state;
    strm.total_in = strm.total_out = state.total = 0;
    strm.msg = ''; /*Z_NULL*/
    if (state.wrap) {       /* to support ill-conceived Java test suite */
      strm.adler = state.wrap & 1;
    }
    state.mode = HEAD;
    state.last = 0;
    state.havedict = 0;
    state.dmax = 32768;
    state.head = null/*Z_NULL*/;
    state.hold = 0;
    state.bits = 0;
    //state.lencode = state.distcode = state.next = state.codes;
    state.lencode = state.lendyn = new utils.Buf32(ENOUGH_LENS);
    state.distcode = state.distdyn = new utils.Buf32(ENOUGH_DISTS);
  
    state.sane = 1;
    state.back = -1;
    //Tracev((stderr, "inflate: reset\n"));
    return Z_OK;
  }
  
  function inflateReset(strm) {
    var state;
  
    if (!strm || !strm.state) { return Z_STREAM_ERROR; }
    state = strm.state;
    state.wsize = 0;
    state.whave = 0;
    state.wnext = 0;
    return inflateResetKeep(strm);
  
  }
  
  function inflateReset2(strm, windowBits) {
    var wrap;
    var state;
  
    /* get the state */
    if (!strm || !strm.state) { return Z_STREAM_ERROR; }
    state = strm.state;
  
    /* extract wrap request from windowBits parameter */
    if (windowBits < 0) {
      wrap = 0;
      windowBits = -windowBits;
    }
    else {
      wrap = (windowBits >> 4) + 1;
      if (windowBits < 48) {
        windowBits &= 15;
      }
    }
  
    /* set number of window bits, free window if different */
    if (windowBits && (windowBits < 8 || windowBits > 15)) {
      return Z_STREAM_ERROR;
    }
    if (state.window !== null && state.wbits !== windowBits) {
      state.window = null;
    }
  
    /* update state and reset the rest of it */
    state.wrap = wrap;
    state.wbits = windowBits;
    return inflateReset(strm);
  }
  
  function inflateInit2(strm, windowBits) {
    var ret;
    var state;
  
    if (!strm) { return Z_STREAM_ERROR; }
    //strm.msg = Z_NULL;                 /* in case we return an error */
  
    state = new InflateState();
  
    //if (state === Z_NULL) return Z_MEM_ERROR;
    //Tracev((stderr, "inflate: allocated\n"));
    strm.state = state;
    state.window = null/*Z_NULL*/;
    ret = inflateReset2(strm, windowBits);
    if (ret !== Z_OK) {
      strm.state = null/*Z_NULL*/;
    }
    return ret;
  }
  
  function inflateInit(strm) {
    return inflateInit2(strm, DEF_WBITS);
  }
  
  
  /*
   Return state with length and distance decoding tables and index sizes set to
   fixed code decoding.  Normally this returns fixed tables from inffixed.h.
   If BUILDFIXED is defined, then instead this routine builds the tables the
   first time it's called, and returns those tables the first time and
   thereafter.  This reduces the size of the code by about 2K bytes, in
   exchange for a little execution time.  However, BUILDFIXED should not be
   used for threaded applications, since the rewriting of the tables and virgin
   may not be thread-safe.
   */
  var virgin = true;
  
  var lenfix, distfix; // We have no pointers in JS, so keep tables separate
  
  function fixedtables(state) {
    /* build fixed huffman tables if first call (may not be thread safe) */
    if (virgin) {
      var sym;
  
      lenfix = new utils.Buf32(512);
      distfix = new utils.Buf32(32);
  
      /* literal/length table */
      sym = 0;
      while (sym < 144) { state.lens[sym++] = 8; }
      while (sym < 256) { state.lens[sym++] = 9; }
      while (sym < 280) { state.lens[sym++] = 7; }
      while (sym < 288) { state.lens[sym++] = 8; }
  
      inflate_table(LENS,  state.lens, 0, 288, lenfix,   0, state.work, { bits: 9 });
  
      /* distance table */
      sym = 0;
      while (sym < 32) { state.lens[sym++] = 5; }
  
      inflate_table(DISTS, state.lens, 0, 32,   distfix, 0, state.work, { bits: 5 });
  
      /* do this just once */
      virgin = false;
    }
  
    state.lencode = lenfix;
    state.lenbits = 9;
    state.distcode = distfix;
    state.distbits = 5;
  }
  
  
  /*
   Update the window with the last wsize (normally 32K) bytes written before
   returning.  If window does not exist yet, create it.  This is only called
   when a window is already in use, or when output has been written during this
   inflate call, but the end of the deflate stream has not been reached yet.
   It is also called to create a window for dictionary data when a dictionary
   is loaded.
  
   Providing output buffers larger than 32K to inflate() should provide a speed
   advantage, since only the last 32K of output is copied to the sliding window
   upon return from inflate(), and since all distances after the first 32K of
   output will fall in the output data, making match copies simpler and faster.
   The advantage may be dependent on the size of the processor's data caches.
   */
  function updatewindow(strm, src, end, copy) {
    var dist;
    var state = strm.state;
  
    /* if it hasn't been done already, allocate space for the window */
    if (state.window === null) {
      state.wsize = 1 << state.wbits;
      state.wnext = 0;
      state.whave = 0;
  
      state.window = new utils.Buf8(state.wsize);
    }
  
    /* copy state->wsize or less output bytes into the circular window */
    if (copy >= state.wsize) {
      utils.arraySet(state.window, src, end - state.wsize, state.wsize, 0);
      state.wnext = 0;
      state.whave = state.wsize;
    }
    else {
      dist = state.wsize - state.wnext;
      if (dist > copy) {
        dist = copy;
      }
      //zmemcpy(state->window + state->wnext, end - copy, dist);
      utils.arraySet(state.window, src, end - copy, dist, state.wnext);
      copy -= dist;
      if (copy) {
        //zmemcpy(state->window, end - copy, copy);
        utils.arraySet(state.window, src, end - copy, copy, 0);
        state.wnext = copy;
        state.whave = state.wsize;
      }
      else {
        state.wnext += dist;
        if (state.wnext === state.wsize) { state.wnext = 0; }
        if (state.whave < state.wsize) { state.whave += dist; }
      }
    }
    return 0;
  }
  
  function inflate(strm, flush) {
    var state;
    var input, output;          // input/output buffers
    var next;                   /* next input INDEX */
    var put;                    /* next output INDEX */
    var have, left;             /* available input and output */
    var hold;                   /* bit buffer */
    var bits;                   /* bits in bit buffer */
    var _in, _out;              /* save starting available input and output */
    var copy;                   /* number of stored or match bytes to copy */
    var from;                   /* where to copy match bytes from */
    var from_source;
    var here = 0;               /* current decoding table entry */
    var here_bits, here_op, here_val; // paked "here" denormalized (JS specific)
    //var last;                   /* parent table entry */
    var last_bits, last_op, last_val; // paked "last" denormalized (JS specific)
    var len;                    /* length to copy for repeats, bits to drop */
    var ret;                    /* return code */
    var hbuf = new utils.Buf8(4);    /* buffer for gzip header crc calculation */
    var opts;
  
    var n; // temporary var for NEED_BITS
  
    var order = /* permutation of code lengths */
      [ 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 ];
  
  
    if (!strm || !strm.state || !strm.output ||
        (!strm.input && strm.avail_in !== 0)) {
      return Z_STREAM_ERROR;
    }
  
    state = strm.state;
    if (state.mode === TYPE) { state.mode = TYPEDO; }    /* skip check */
  
  
    //--- LOAD() ---
    put = strm.next_out;
    output = strm.output;
    left = strm.avail_out;
    next = strm.next_in;
    input = strm.input;
    have = strm.avail_in;
    hold = state.hold;
    bits = state.bits;
    //---
  
    _in = have;
    _out = left;
    ret = Z_OK;
  
    inf_leave: // goto emulation
    for (;;) {
      switch (state.mode) {
        case HEAD:
          if (state.wrap === 0) {
            state.mode = TYPEDO;
            break;
          }
          //=== NEEDBITS(16);
          while (bits < 16) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          if ((state.wrap & 2) && hold === 0x8b1f) {  /* gzip header */
            state.check = 0/*crc32(0L, Z_NULL, 0)*/;
            //=== CRC2(state.check, hold);
            hbuf[0] = hold & 0xff;
            hbuf[1] = (hold >>> 8) & 0xff;
            state.check = crc32(state.check, hbuf, 2, 0);
            //===//
  
            //=== INITBITS();
            hold = 0;
            bits = 0;
            //===//
            state.mode = FLAGS;
            break;
          }
          state.flags = 0;           /* expect zlib header */
          if (state.head) {
            state.head.done = false;
          }
          if (!(state.wrap & 1) ||   /* check if zlib header allowed */
            (((hold & 0xff)/*BITS(8)*/ << 8) + (hold >> 8)) % 31) {
            strm.msg = 'incorrect header check';
            state.mode = BAD;
            break;
          }
          if ((hold & 0x0f)/*BITS(4)*/ !== Z_DEFLATED) {
            strm.msg = 'unknown compression method';
            state.mode = BAD;
            break;
          }
          //--- DROPBITS(4) ---//
          hold >>>= 4;
          bits -= 4;
          //---//
          len = (hold & 0x0f)/*BITS(4)*/ + 8;
          if (state.wbits === 0) {
            state.wbits = len;
          }
          else if (len > state.wbits) {
            strm.msg = 'invalid window size';
            state.mode = BAD;
            break;
          }
          state.dmax = 1 << len;
          //Tracev((stderr, "inflate:   zlib header ok\n"));
          strm.adler = state.check = 1/*adler32(0L, Z_NULL, 0)*/;
          state.mode = hold & 0x200 ? DICTID : TYPE;
          //=== INITBITS();
          hold = 0;
          bits = 0;
          //===//
          break;
        case FLAGS:
          //=== NEEDBITS(16); */
          while (bits < 16) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          state.flags = hold;
          if ((state.flags & 0xff) !== Z_DEFLATED) {
            strm.msg = 'unknown compression method';
            state.mode = BAD;
            break;
          }
          if (state.flags & 0xe000) {
            strm.msg = 'unknown header flags set';
            state.mode = BAD;
            break;
          }
          if (state.head) {
            state.head.text = ((hold >> 8) & 1);
          }
          if (state.flags & 0x0200) {
            //=== CRC2(state.check, hold);
            hbuf[0] = hold & 0xff;
            hbuf[1] = (hold >>> 8) & 0xff;
            state.check = crc32(state.check, hbuf, 2, 0);
            //===//
          }
          //=== INITBITS();
          hold = 0;
          bits = 0;
          //===//
          state.mode = TIME;
          /* falls through */
        case TIME:
          //=== NEEDBITS(32); */
          while (bits < 32) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          if (state.head) {
            state.head.time = hold;
          }
          if (state.flags & 0x0200) {
            //=== CRC4(state.check, hold)
            hbuf[0] = hold & 0xff;
            hbuf[1] = (hold >>> 8) & 0xff;
            hbuf[2] = (hold >>> 16) & 0xff;
            hbuf[3] = (hold >>> 24) & 0xff;
            state.check = crc32(state.check, hbuf, 4, 0);
            //===
          }
          //=== INITBITS();
          hold = 0;
          bits = 0;
          //===//
          state.mode = OS;
          /* falls through */
        case OS:
          //=== NEEDBITS(16); */
          while (bits < 16) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          if (state.head) {
            state.head.xflags = (hold & 0xff);
            state.head.os = (hold >> 8);
          }
          if (state.flags & 0x0200) {
            //=== CRC2(state.check, hold);
            hbuf[0] = hold & 0xff;
            hbuf[1] = (hold >>> 8) & 0xff;
            state.check = crc32(state.check, hbuf, 2, 0);
            //===//
          }
          //=== INITBITS();
          hold = 0;
          bits = 0;
          //===//
          state.mode = EXLEN;
          /* falls through */
        case EXLEN:
          if (state.flags & 0x0400) {
            //=== NEEDBITS(16); */
            while (bits < 16) {
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
            }
            //===//
            state.length = hold;
            if (state.head) {
              state.head.extra_len = hold;
            }
            if (state.flags & 0x0200) {
              //=== CRC2(state.check, hold);
              hbuf[0] = hold & 0xff;
              hbuf[1] = (hold >>> 8) & 0xff;
              state.check = crc32(state.check, hbuf, 2, 0);
              //===//
            }
            //=== INITBITS();
            hold = 0;
            bits = 0;
            //===//
          }
          else if (state.head) {
            state.head.extra = null/*Z_NULL*/;
          }
          state.mode = EXTRA;
          /* falls through */
        case EXTRA:
          if (state.flags & 0x0400) {
            copy = state.length;
            if (copy > have) { copy = have; }
            if (copy) {
              if (state.head) {
                len = state.head.extra_len - state.length;
                if (!state.head.extra) {
                  // Use untyped array for more convenient processing later
                  state.head.extra = new Array(state.head.extra_len);
                }
                utils.arraySet(
                  state.head.extra,
                  input,
                  next,
                  // extra field is limited to 65536 bytes
                  // - no need for additional size check
                  copy,
                  /*len + copy > state.head.extra_max - len ? state.head.extra_max : copy,*/
                  len
                );
                //zmemcpy(state.head.extra + len, next,
                //        len + copy > state.head.extra_max ?
                //        state.head.extra_max - len : copy);
              }
              if (state.flags & 0x0200) {
                state.check = crc32(state.check, input, copy, next);
              }
              have -= copy;
              next += copy;
              state.length -= copy;
            }
            if (state.length) { break inf_leave; }
          }
          state.length = 0;
          state.mode = NAME;
          /* falls through */
        case NAME:
          if (state.flags & 0x0800) {
            if (have === 0) { break inf_leave; }
            copy = 0;
            do {
              // TODO: 2 or 1 bytes?
              len = input[next + copy++];
              /* use constant limit because in js we should not preallocate memory */
              if (state.head && len &&
                  (state.length < 65536 /*state.head.name_max*/)) {
                state.head.name += String.fromCharCode(len);
              }
            } while (len && copy < have);
  
            if (state.flags & 0x0200) {
              state.check = crc32(state.check, input, copy, next);
            }
            have -= copy;
            next += copy;
            if (len) { break inf_leave; }
          }
          else if (state.head) {
            state.head.name = null;
          }
          state.length = 0;
          state.mode = COMMENT;
          /* falls through */
        case COMMENT:
          if (state.flags & 0x1000) {
            if (have === 0) { break inf_leave; }
            copy = 0;
            do {
              len = input[next + copy++];
              /* use constant limit because in js we should not preallocate memory */
              if (state.head && len &&
                  (state.length < 65536 /*state.head.comm_max*/)) {
                state.head.comment += String.fromCharCode(len);
              }
            } while (len && copy < have);
            if (state.flags & 0x0200) {
              state.check = crc32(state.check, input, copy, next);
            }
            have -= copy;
            next += copy;
            if (len) { break inf_leave; }
          }
          else if (state.head) {
            state.head.comment = null;
          }
          state.mode = HCRC;
          /* falls through */
        case HCRC:
          if (state.flags & 0x0200) {
            //=== NEEDBITS(16); */
            while (bits < 16) {
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
            }
            //===//
            if (hold !== (state.check & 0xffff)) {
              strm.msg = 'header crc mismatch';
              state.mode = BAD;
              break;
            }
            //=== INITBITS();
            hold = 0;
            bits = 0;
            //===//
          }
          if (state.head) {
            state.head.hcrc = ((state.flags >> 9) & 1);
            state.head.done = true;
          }
          strm.adler = state.check = 0;
          state.mode = TYPE;
          break;
        case DICTID:
          //=== NEEDBITS(32); */
          while (bits < 32) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          strm.adler = state.check = zswap32(hold);
          //=== INITBITS();
          hold = 0;
          bits = 0;
          //===//
          state.mode = DICT;
          /* falls through */
        case DICT:
          if (state.havedict === 0) {
            //--- RESTORE() ---
            strm.next_out = put;
            strm.avail_out = left;
            strm.next_in = next;
            strm.avail_in = have;
            state.hold = hold;
            state.bits = bits;
            //---
            return Z_NEED_DICT;
          }
          strm.adler = state.check = 1/*adler32(0L, Z_NULL, 0)*/;
          state.mode = TYPE;
          /* falls through */
        case TYPE:
          if (flush === Z_BLOCK || flush === Z_TREES) { break inf_leave; }
          /* falls through */
        case TYPEDO:
          if (state.last) {
            //--- BYTEBITS() ---//
            hold >>>= bits & 7;
            bits -= bits & 7;
            //---//
            state.mode = CHECK;
            break;
          }
          //=== NEEDBITS(3); */
          while (bits < 3) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          state.last = (hold & 0x01)/*BITS(1)*/;
          //--- DROPBITS(1) ---//
          hold >>>= 1;
          bits -= 1;
          //---//
  
          switch ((hold & 0x03)/*BITS(2)*/) {
            case 0:                             /* stored block */
              //Tracev((stderr, "inflate:     stored block%s\n",
              //        state.last ? " (last)" : ""));
              state.mode = STORED;
              break;
            case 1:                             /* fixed block */
              fixedtables(state);
              //Tracev((stderr, "inflate:     fixed codes block%s\n",
              //        state.last ? " (last)" : ""));
              state.mode = LEN_;             /* decode codes */
              if (flush === Z_TREES) {
                //--- DROPBITS(2) ---//
                hold >>>= 2;
                bits -= 2;
                //---//
                break inf_leave;
              }
              break;
            case 2:                             /* dynamic block */
              //Tracev((stderr, "inflate:     dynamic codes block%s\n",
              //        state.last ? " (last)" : ""));
              state.mode = TABLE;
              break;
            case 3:
              strm.msg = 'invalid block type';
              state.mode = BAD;
          }
          //--- DROPBITS(2) ---//
          hold >>>= 2;
          bits -= 2;
          //---//
          break;
        case STORED:
          //--- BYTEBITS() ---// /* go to byte boundary */
          hold >>>= bits & 7;
          bits -= bits & 7;
          //---//
          //=== NEEDBITS(32); */
          while (bits < 32) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          if ((hold & 0xffff) !== ((hold >>> 16) ^ 0xffff)) {
            strm.msg = 'invalid stored block lengths';
            state.mode = BAD;
            break;
          }
          state.length = hold & 0xffff;
          //Tracev((stderr, "inflate:       stored length %u\n",
          //        state.length));
          //=== INITBITS();
          hold = 0;
          bits = 0;
          //===//
          state.mode = COPY_;
          if (flush === Z_TREES) { break inf_leave; }
          /* falls through */
        case COPY_:
          state.mode = COPY;
          /* falls through */
        case COPY:
          copy = state.length;
          if (copy) {
            if (copy > have) { copy = have; }
            if (copy > left) { copy = left; }
            if (copy === 0) { break inf_leave; }
            //--- zmemcpy(put, next, copy); ---
            utils.arraySet(output, input, next, copy, put);
            //---//
            have -= copy;
            next += copy;
            left -= copy;
            put += copy;
            state.length -= copy;
            break;
          }
          //Tracev((stderr, "inflate:       stored end\n"));
          state.mode = TYPE;
          break;
        case TABLE:
          //=== NEEDBITS(14); */
          while (bits < 14) {
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
          }
          //===//
          state.nlen = (hold & 0x1f)/*BITS(5)*/ + 257;
          //--- DROPBITS(5) ---//
          hold >>>= 5;
          bits -= 5;
          //---//
          state.ndist = (hold & 0x1f)/*BITS(5)*/ + 1;
          //--- DROPBITS(5) ---//
          hold >>>= 5;
          bits -= 5;
          //---//
          state.ncode = (hold & 0x0f)/*BITS(4)*/ + 4;
          //--- DROPBITS(4) ---//
          hold >>>= 4;
          bits -= 4;
          //---//
  //#ifndef PKZIP_BUG_WORKAROUND
          if (state.nlen > 286 || state.ndist > 30) {
            strm.msg = 'too many length or distance symbols';
            state.mode = BAD;
            break;
          }
  //#endif
          //Tracev((stderr, "inflate:       table sizes ok\n"));
          state.have = 0;
          state.mode = LENLENS;
          /* falls through */
        case LENLENS:
          while (state.have < state.ncode) {
            //=== NEEDBITS(3);
            while (bits < 3) {
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
            }
            //===//
            state.lens[order[state.have++]] = (hold & 0x07);//BITS(3);
            //--- DROPBITS(3) ---//
            hold >>>= 3;
            bits -= 3;
            //---//
          }
          while (state.have < 19) {
            state.lens[order[state.have++]] = 0;
          }
          // We have separate tables & no pointers. 2 commented lines below not needed.
          //state.next = state.codes;
          //state.lencode = state.next;
          // Switch to use dynamic table
          state.lencode = state.lendyn;
          state.lenbits = 7;
  
          opts = { bits: state.lenbits };
          ret = inflate_table(CODES, state.lens, 0, 19, state.lencode, 0, state.work, opts);
          state.lenbits = opts.bits;
  
          if (ret) {
            strm.msg = 'invalid code lengths set';
            state.mode = BAD;
            break;
          }
          //Tracev((stderr, "inflate:       code lengths ok\n"));
          state.have = 0;
          state.mode = CODELENS;
          /* falls through */
        case CODELENS:
          while (state.have < state.nlen + state.ndist) {
            for (;;) {
              here = state.lencode[hold & ((1 << state.lenbits) - 1)];/*BITS(state.lenbits)*/
              here_bits = here >>> 24;
              here_op = (here >>> 16) & 0xff;
              here_val = here & 0xffff;
  
              if ((here_bits) <= bits) { break; }
              //--- PULLBYTE() ---//
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
              //---//
            }
            if (here_val < 16) {
              //--- DROPBITS(here.bits) ---//
              hold >>>= here_bits;
              bits -= here_bits;
              //---//
              state.lens[state.have++] = here_val;
            }
            else {
              if (here_val === 16) {
                //=== NEEDBITS(here.bits + 2);
                n = here_bits + 2;
                while (bits < n) {
                  if (have === 0) { break inf_leave; }
                  have--;
                  hold += input[next++] << bits;
                  bits += 8;
                }
                //===//
                //--- DROPBITS(here.bits) ---//
                hold >>>= here_bits;
                bits -= here_bits;
                //---//
                if (state.have === 0) {
                  strm.msg = 'invalid bit length repeat';
                  state.mode = BAD;
                  break;
                }
                len = state.lens[state.have - 1];
                copy = 3 + (hold & 0x03);//BITS(2);
                //--- DROPBITS(2) ---//
                hold >>>= 2;
                bits -= 2;
                //---//
              }
              else if (here_val === 17) {
                //=== NEEDBITS(here.bits + 3);
                n = here_bits + 3;
                while (bits < n) {
                  if (have === 0) { break inf_leave; }
                  have--;
                  hold += input[next++] << bits;
                  bits += 8;
                }
                //===//
                //--- DROPBITS(here.bits) ---//
                hold >>>= here_bits;
                bits -= here_bits;
                //---//
                len = 0;
                copy = 3 + (hold & 0x07);//BITS(3);
                //--- DROPBITS(3) ---//
                hold >>>= 3;
                bits -= 3;
                //---//
              }
              else {
                //=== NEEDBITS(here.bits + 7);
                n = here_bits + 7;
                while (bits < n) {
                  if (have === 0) { break inf_leave; }
                  have--;
                  hold += input[next++] << bits;
                  bits += 8;
                }
                //===//
                //--- DROPBITS(here.bits) ---//
                hold >>>= here_bits;
                bits -= here_bits;
                //---//
                len = 0;
                copy = 11 + (hold & 0x7f);//BITS(7);
                //--- DROPBITS(7) ---//
                hold >>>= 7;
                bits -= 7;
                //---//
              }
              if (state.have + copy > state.nlen + state.ndist) {
                strm.msg = 'invalid bit length repeat';
                state.mode = BAD;
                break;
              }
              while (copy--) {
                state.lens[state.have++] = len;
              }
            }
          }
  
          /* handle error breaks in while */
          if (state.mode === BAD) { break; }
  
          /* check for end-of-block code (better have one) */
          if (state.lens[256] === 0) {
            strm.msg = 'invalid code -- missing end-of-block';
            state.mode = BAD;
            break;
          }
  
          /* build code tables -- note: do not change the lenbits or distbits
             values here (9 and 6) without reading the comments in inftrees.h
             concerning the ENOUGH constants, which depend on those values */
          state.lenbits = 9;
  
          opts = { bits: state.lenbits };
          ret = inflate_table(LENS, state.lens, 0, state.nlen, state.lencode, 0, state.work, opts);
          // We have separate tables & no pointers. 2 commented lines below not needed.
          // state.next_index = opts.table_index;
          state.lenbits = opts.bits;
          // state.lencode = state.next;
  
          if (ret) {
            strm.msg = 'invalid literal/lengths set';
            state.mode = BAD;
            break;
          }
  
          state.distbits = 6;
          //state.distcode.copy(state.codes);
          // Switch to use dynamic table
          state.distcode = state.distdyn;
          opts = { bits: state.distbits };
          ret = inflate_table(DISTS, state.lens, state.nlen, state.ndist, state.distcode, 0, state.work, opts);
          // We have separate tables & no pointers. 2 commented lines below not needed.
          // state.next_index = opts.table_index;
          state.distbits = opts.bits;
          // state.distcode = state.next;
  
          if (ret) {
            strm.msg = 'invalid distances set';
            state.mode = BAD;
            break;
          }
          //Tracev((stderr, 'inflate:       codes ok\n'));
          state.mode = LEN_;
          if (flush === Z_TREES) { break inf_leave; }
          /* falls through */
        case LEN_:
          state.mode = LEN;
          /* falls through */
        case LEN:
          if (have >= 6 && left >= 258) {
            //--- RESTORE() ---
            strm.next_out = put;
            strm.avail_out = left;
            strm.next_in = next;
            strm.avail_in = have;
            state.hold = hold;
            state.bits = bits;
            //---
            inflate_fast(strm, _out);
            //--- LOAD() ---
            put = strm.next_out;
            output = strm.output;
            left = strm.avail_out;
            next = strm.next_in;
            input = strm.input;
            have = strm.avail_in;
            hold = state.hold;
            bits = state.bits;
            //---
  
            if (state.mode === TYPE) {
              state.back = -1;
            }
            break;
          }
          state.back = 0;
          for (;;) {
            here = state.lencode[hold & ((1 << state.lenbits) - 1)];  /*BITS(state.lenbits)*/
            here_bits = here >>> 24;
            here_op = (here >>> 16) & 0xff;
            here_val = here & 0xffff;
  
            if (here_bits <= bits) { break; }
            //--- PULLBYTE() ---//
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
            //---//
          }
          if (here_op && (here_op & 0xf0) === 0) {
            last_bits = here_bits;
            last_op = here_op;
            last_val = here_val;
            for (;;) {
              here = state.lencode[last_val +
                      ((hold & ((1 << (last_bits + last_op)) - 1))/*BITS(last.bits + last.op)*/ >> last_bits)];
              here_bits = here >>> 24;
              here_op = (here >>> 16) & 0xff;
              here_val = here & 0xffff;
  
              if ((last_bits + here_bits) <= bits) { break; }
              //--- PULLBYTE() ---//
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
              //---//
            }
            //--- DROPBITS(last.bits) ---//
            hold >>>= last_bits;
            bits -= last_bits;
            //---//
            state.back += last_bits;
          }
          //--- DROPBITS(here.bits) ---//
          hold >>>= here_bits;
          bits -= here_bits;
          //---//
          state.back += here_bits;
          state.length = here_val;
          if (here_op === 0) {
            //Tracevv((stderr, here.val >= 0x20 && here.val < 0x7f ?
            //        "inflate:         literal '%c'\n" :
            //        "inflate:         literal 0x%02x\n", here.val));
            state.mode = LIT;
            break;
          }
          if (here_op & 32) {
            //Tracevv((stderr, "inflate:         end of block\n"));
            state.back = -1;
            state.mode = TYPE;
            break;
          }
          if (here_op & 64) {
            strm.msg = 'invalid literal/length code';
            state.mode = BAD;
            break;
          }
          state.extra = here_op & 15;
          state.mode = LENEXT;
          /* falls through */
        case LENEXT:
          if (state.extra) {
            //=== NEEDBITS(state.extra);
            n = state.extra;
            while (bits < n) {
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
            }
            //===//
            state.length += hold & ((1 << state.extra) - 1)/*BITS(state.extra)*/;
            //--- DROPBITS(state.extra) ---//
            hold >>>= state.extra;
            bits -= state.extra;
            //---//
            state.back += state.extra;
          }
          //Tracevv((stderr, "inflate:         length %u\n", state.length));
          state.was = state.length;
          state.mode = DIST;
          /* falls through */
        case DIST:
          for (;;) {
            here = state.distcode[hold & ((1 << state.distbits) - 1)];/*BITS(state.distbits)*/
            here_bits = here >>> 24;
            here_op = (here >>> 16) & 0xff;
            here_val = here & 0xffff;
  
            if ((here_bits) <= bits) { break; }
            //--- PULLBYTE() ---//
            if (have === 0) { break inf_leave; }
            have--;
            hold += input[next++] << bits;
            bits += 8;
            //---//
          }
          if ((here_op & 0xf0) === 0) {
            last_bits = here_bits;
            last_op = here_op;
            last_val = here_val;
            for (;;) {
              here = state.distcode[last_val +
                      ((hold & ((1 << (last_bits + last_op)) - 1))/*BITS(last.bits + last.op)*/ >> last_bits)];
              here_bits = here >>> 24;
              here_op = (here >>> 16) & 0xff;
              here_val = here & 0xffff;
  
              if ((last_bits + here_bits) <= bits) { break; }
              //--- PULLBYTE() ---//
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
              //---//
            }
            //--- DROPBITS(last.bits) ---//
            hold >>>= last_bits;
            bits -= last_bits;
            //---//
            state.back += last_bits;
          }
          //--- DROPBITS(here.bits) ---//
          hold >>>= here_bits;
          bits -= here_bits;
          //---//
          state.back += here_bits;
          if (here_op & 64) {
            strm.msg = 'invalid distance code';
            state.mode = BAD;
            break;
          }
          state.offset = here_val;
          state.extra = (here_op) & 15;
          state.mode = DISTEXT;
          /* falls through */
        case DISTEXT:
          if (state.extra) {
            //=== NEEDBITS(state.extra);
            n = state.extra;
            while (bits < n) {
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
            }
            //===//
            state.offset += hold & ((1 << state.extra) - 1)/*BITS(state.extra)*/;
            //--- DROPBITS(state.extra) ---//
            hold >>>= state.extra;
            bits -= state.extra;
            //---//
            state.back += state.extra;
          }
  //#ifdef INFLATE_STRICT
          if (state.offset > state.dmax) {
            strm.msg = 'invalid distance too far back';
            state.mode = BAD;
            break;
          }
  //#endif
          //Tracevv((stderr, "inflate:         distance %u\n", state.offset));
          state.mode = MATCH;
          /* falls through */
        case MATCH:
          if (left === 0) { break inf_leave; }
          copy = _out - left;
          if (state.offset > copy) {         /* copy from window */
            copy = state.offset - copy;
            if (copy > state.whave) {
              if (state.sane) {
                strm.msg = 'invalid distance too far back';
                state.mode = BAD;
                break;
              }
  // (!) This block is disabled in zlib defaults,
  // don't enable it for binary compatibility
  //#ifdef INFLATE_ALLOW_INVALID_DISTANCE_TOOFAR_ARRR
  //          Trace((stderr, "inflate.c too far\n"));
  //          copy -= state.whave;
  //          if (copy > state.length) { copy = state.length; }
  //          if (copy > left) { copy = left; }
  //          left -= copy;
  //          state.length -= copy;
  //          do {
  //            output[put++] = 0;
  //          } while (--copy);
  //          if (state.length === 0) { state.mode = LEN; }
  //          break;
  //#endif
            }
            if (copy > state.wnext) {
              copy -= state.wnext;
              from = state.wsize - copy;
            }
            else {
              from = state.wnext - copy;
            }
            if (copy > state.length) { copy = state.length; }
            from_source = state.window;
          }
          else {                              /* copy from output */
            from_source = output;
            from = put - state.offset;
            copy = state.length;
          }
          if (copy > left) { copy = left; }
          left -= copy;
          state.length -= copy;
          do {
            output[put++] = from_source[from++];
          } while (--copy);
          if (state.length === 0) { state.mode = LEN; }
          break;
        case LIT:
          if (left === 0) { break inf_leave; }
          output[put++] = state.length;
          left--;
          state.mode = LEN;
          break;
        case CHECK:
          if (state.wrap) {
            //=== NEEDBITS(32);
            while (bits < 32) {
              if (have === 0) { break inf_leave; }
              have--;
              // Use '|' instead of '+' to make sure that result is signed
              hold |= input[next++] << bits;
              bits += 8;
            }
            //===//
            _out -= left;
            strm.total_out += _out;
            state.total += _out;
            if (_out) {
              strm.adler = state.check =
                  /*UPDATE(state.check, put - _out, _out);*/
                  (state.flags ? crc32(state.check, output, _out, put - _out) : adler32(state.check, output, _out, put - _out));
  
            }
            _out = left;
            // NB: crc32 stored as signed 32-bit int, zswap32 returns signed too
            if ((state.flags ? hold : zswap32(hold)) !== state.check) {
              strm.msg = 'incorrect data check';
              state.mode = BAD;
              break;
            }
            //=== INITBITS();
            hold = 0;
            bits = 0;
            //===//
            //Tracev((stderr, "inflate:   check matches trailer\n"));
          }
          state.mode = LENGTH;
          /* falls through */
        case LENGTH:
          if (state.wrap && state.flags) {
            //=== NEEDBITS(32);
            while (bits < 32) {
              if (have === 0) { break inf_leave; }
              have--;
              hold += input[next++] << bits;
              bits += 8;
            }
            //===//
            if (hold !== (state.total & 0xffffffff)) {
              strm.msg = 'incorrect length check';
              state.mode = BAD;
              break;
            }
            //=== INITBITS();
            hold = 0;
            bits = 0;
            //===//
            //Tracev((stderr, "inflate:   length matches trailer\n"));
          }
          state.mode = DONE;
          /* falls through */
        case DONE:
          ret = Z_STREAM_END;
          break inf_leave;
        case BAD:
          ret = Z_DATA_ERROR;
          break inf_leave;
        case MEM:
          return Z_MEM_ERROR;
        case SYNC:
          /* falls through */
        default:
          return Z_STREAM_ERROR;
      }
    }
  
    // inf_leave <- here is real place for "goto inf_leave", emulated via "break inf_leave"
  
    /*
       Return from inflate(), updating the total counts and the check value.
       If there was no progress during the inflate() call, return a buffer
       error.  Call updatewindow() to create and/or update the window state.
       Note: a memory error from inflate() is non-recoverable.
     */
  
    //--- RESTORE() ---
    strm.next_out = put;
    strm.avail_out = left;
    strm.next_in = next;
    strm.avail_in = have;
    state.hold = hold;
    state.bits = bits;
    //---
  
    if (state.wsize || (_out !== strm.avail_out && state.mode < BAD &&
                        (state.mode < CHECK || flush !== Z_FINISH))) {
      if (updatewindow(strm, strm.output, strm.next_out, _out - strm.avail_out)) {
        state.mode = MEM;
        return Z_MEM_ERROR;
      }
    }
    _in -= strm.avail_in;
    _out -= strm.avail_out;
    strm.total_in += _in;
    strm.total_out += _out;
    state.total += _out;
    if (state.wrap && _out) {
      strm.adler = state.check = /*UPDATE(state.check, strm.next_out - _out, _out);*/
        (state.flags ? crc32(state.check, output, _out, strm.next_out - _out) : adler32(state.check, output, _out, strm.next_out - _out));
    }
    strm.data_type = state.bits + (state.last ? 64 : 0) +
                      (state.mode === TYPE ? 128 : 0) +
                      (state.mode === LEN_ || state.mode === COPY_ ? 256 : 0);
    if (((_in === 0 && _out === 0) || flush === Z_FINISH) && ret === Z_OK) {
      ret = Z_BUF_ERROR;
    }
    return ret;
  }
  
  function inflateEnd(strm) {
  
    if (!strm || !strm.state /*|| strm->zfree == (free_func)0*/) {
      return Z_STREAM_ERROR;
    }
  
    var state = strm.state;
    if (state.window) {
      state.window = null;
    }
    strm.state = null;
    return Z_OK;
  }
  
  function inflateGetHeader(strm, head) {
    var state;
  
    /* check state */
    if (!strm || !strm.state) { return Z_STREAM_ERROR; }
    state = strm.state;
    if ((state.wrap & 2) === 0) { return Z_STREAM_ERROR; }
  
    /* save header structure */
    state.head = head;
    head.done = false;
    return Z_OK;
  }
  
  function inflateSetDictionary(strm, dictionary) {
    var dictLength = dictionary.length;
  
    var state;
    var dictid;
    var ret;
  
    /* check state */
    if (!strm /* == Z_NULL */ || !strm.state /* == Z_NULL */) { return Z_STREAM_ERROR; }
    state = strm.state;
  
    if (state.wrap !== 0 && state.mode !== DICT) {
      return Z_STREAM_ERROR;
    }
  
    /* check for correct dictionary identifier */
    if (state.mode === DICT) {
      dictid = 1; /* adler32(0, null, 0)*/
      /* dictid = adler32(dictid, dictionary, dictLength); */
      dictid = adler32(dictid, dictionary, dictLength, 0);
      if (dictid !== state.check) {
        return Z_DATA_ERROR;
      }
    }
    /* copy dictionary to window using updatewindow(), which will amend the
     existing dictionary if appropriate */
    ret = updatewindow(strm, dictionary, dictLength, dictLength);
    if (ret) {
      state.mode = MEM;
      return Z_MEM_ERROR;
    }
    state.havedict = 1;
    // Tracev((stderr, "inflate:   dictionary set\n"));
    return Z_OK;
  }
  
  exports.inflateReset = inflateReset;
  exports.inflateReset2 = inflateReset2;
  exports.inflateResetKeep = inflateResetKeep;
  exports.inflateInit = inflateInit;
  exports.inflateInit2 = inflateInit2;
  exports.inflate = inflate;
  exports.inflateEnd = inflateEnd;
  exports.inflateGetHeader = inflateGetHeader;
  exports.inflateSetDictionary = inflateSetDictionary;
  exports.inflateInfo = 'pako inflate (from Nodeca project)';
  
  /* Not implemented
  exports.inflateCopy = inflateCopy;
  exports.inflateGetDictionary = inflateGetDictionary;
  exports.inflateMark = inflateMark;
  exports.inflatePrime = inflatePrime;
  exports.inflateSync = inflateSync;
  exports.inflateSyncPoint = inflateSyncPoint;
  exports.inflateUndermine = inflateUndermine;
  */
  
  },{"../utils/common":20,"./adler32":21,"./crc32":23,"./inffast":25,"./inftrees":27}],27:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  var utils = require('../utils/common');
  
  var MAXBITS = 15;
  var ENOUGH_LENS = 852;
  var ENOUGH_DISTS = 592;
  //var ENOUGH = (ENOUGH_LENS+ENOUGH_DISTS);
  
  var CODES = 0;
  var LENS = 1;
  var DISTS = 2;
  
  var lbase = [ /* Length codes 257..285 base */
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
    35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
  ];
  
  var lext = [ /* Length codes 257..285 extra */
    16, 16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18,
    19, 19, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 16, 72, 78
  ];
  
  var dbase = [ /* Distance codes 0..29 base */
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
    257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
    8193, 12289, 16385, 24577, 0, 0
  ];
  
  var dext = [ /* Distance codes 0..29 extra */
    16, 16, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22,
    23, 23, 24, 24, 25, 25, 26, 26, 27, 27,
    28, 28, 29, 29, 64, 64
  ];
  
  module.exports = function inflate_table(type, lens, lens_index, codes, table, table_index, work, opts)
  {
    var bits = opts.bits;
        //here = opts.here; /* table entry for duplication */
  
    var len = 0;               /* a code's length in bits */
    var sym = 0;               /* index of code symbols */
    var min = 0, max = 0;          /* minimum and maximum code lengths */
    var root = 0;              /* number of index bits for root table */
    var curr = 0;              /* number of index bits for current table */
    var drop = 0;              /* code bits to drop for sub-table */
    var left = 0;                   /* number of prefix codes available */
    var used = 0;              /* code entries in table used */
    var huff = 0;              /* Huffman code */
    var incr;              /* for incrementing code, index */
    var fill;              /* index for replicating entries */
    var low;               /* low bits for current root entry */
    var mask;              /* mask for low root bits */
    var next;             /* next available space in table */
    var base = null;     /* base value table to use */
    var base_index = 0;
  //  var shoextra;    /* extra bits table to use */
    var end;                    /* use base and extra for symbol > end */
    var count = new utils.Buf16(MAXBITS + 1); //[MAXBITS+1];    /* number of codes of each length */
    var offs = new utils.Buf16(MAXBITS + 1); //[MAXBITS+1];     /* offsets in table for each length */
    var extra = null;
    var extra_index = 0;
  
    var here_bits, here_op, here_val;
  
    /*
     Process a set of code lengths to create a canonical Huffman code.  The
     code lengths are lens[0..codes-1].  Each length corresponds to the
     symbols 0..codes-1.  The Huffman code is generated by first sorting the
     symbols by length from short to long, and retaining the symbol order
     for codes with equal lengths.  Then the code starts with all zero bits
     for the first code of the shortest length, and the codes are integer
     increments for the same length, and zeros are appended as the length
     increases.  For the deflate format, these bits are stored backwards
     from their more natural integer increment ordering, and so when the
     decoding tables are built in the large loop below, the integer codes
     are incremented backwards.
  
     This routine assumes, but does not check, that all of the entries in
     lens[] are in the range 0..MAXBITS.  The caller must assure this.
     1..MAXBITS is interpreted as that code length.  zero means that that
     symbol does not occur in this code.
  
     The codes are sorted by computing a count of codes for each length,
     creating from that a table of starting indices for each length in the
     sorted table, and then entering the symbols in order in the sorted
     table.  The sorted table is work[], with that space being provided by
     the caller.
  
     The length counts are used for other purposes as well, i.e. finding
     the minimum and maximum length codes, determining if there are any
     codes at all, checking for a valid set of lengths, and looking ahead
     at length counts to determine sub-table sizes when building the
     decoding tables.
     */
  
    /* accumulate lengths for codes (assumes lens[] all in 0..MAXBITS) */
    for (len = 0; len <= MAXBITS; len++) {
      count[len] = 0;
    }
    for (sym = 0; sym < codes; sym++) {
      count[lens[lens_index + sym]]++;
    }
  
    /* bound code lengths, force root to be within code lengths */
    root = bits;
    for (max = MAXBITS; max >= 1; max--) {
      if (count[max] !== 0) { break; }
    }
    if (root > max) {
      root = max;
    }
    if (max === 0) {                     /* no symbols to code at all */
      //table.op[opts.table_index] = 64;  //here.op = (var char)64;    /* invalid code marker */
      //table.bits[opts.table_index] = 1;   //here.bits = (var char)1;
      //table.val[opts.table_index++] = 0;   //here.val = (var short)0;
      table[table_index++] = (1 << 24) | (64 << 16) | 0;
  
  
      //table.op[opts.table_index] = 64;
      //table.bits[opts.table_index] = 1;
      //table.val[opts.table_index++] = 0;
      table[table_index++] = (1 << 24) | (64 << 16) | 0;
  
      opts.bits = 1;
      return 0;     /* no symbols, but wait for decoding to report error */
    }
    for (min = 1; min < max; min++) {
      if (count[min] !== 0) { break; }
    }
    if (root < min) {
      root = min;
    }
  
    /* check for an over-subscribed or incomplete set of lengths */
    left = 1;
    for (len = 1; len <= MAXBITS; len++) {
      left <<= 1;
      left -= count[len];
      if (left < 0) {
        return -1;
      }        /* over-subscribed */
    }
    if (left > 0 && (type === CODES || max !== 1)) {
      return -1;                      /* incomplete set */
    }
  
    /* generate offsets into symbol table for each length for sorting */
    offs[1] = 0;
    for (len = 1; len < MAXBITS; len++) {
      offs[len + 1] = offs[len] + count[len];
    }
  
    /* sort symbols by length, by symbol order within each length */
    for (sym = 0; sym < codes; sym++) {
      if (lens[lens_index + sym] !== 0) {
        work[offs[lens[lens_index + sym]]++] = sym;
      }
    }
  
    /*
     Create and fill in decoding tables.  In this loop, the table being
     filled is at next and has curr index bits.  The code being used is huff
     with length len.  That code is converted to an index by dropping drop
     bits off of the bottom.  For codes where len is less than drop + curr,
     those top drop + curr - len bits are incremented through all values to
     fill the table with replicated entries.
  
     root is the number of index bits for the root table.  When len exceeds
     root, sub-tables are created pointed to by the root entry with an index
     of the low root bits of huff.  This is saved in low to check for when a
     new sub-table should be started.  drop is zero when the root table is
     being filled, and drop is root when sub-tables are being filled.
  
     When a new sub-table is needed, it is necessary to look ahead in the
     code lengths to determine what size sub-table is needed.  The length
     counts are used for this, and so count[] is decremented as codes are
     entered in the tables.
  
     used keeps track of how many table entries have been allocated from the
     provided *table space.  It is checked for LENS and DIST tables against
     the constants ENOUGH_LENS and ENOUGH_DISTS to guard against changes in
     the initial root table size constants.  See the comments in inftrees.h
     for more information.
  
     sym increments through all symbols, and the loop terminates when
     all codes of length max, i.e. all codes, have been processed.  This
     routine permits incomplete codes, so another loop after this one fills
     in the rest of the decoding tables with invalid code markers.
     */
  
    /* set up for code type */
    // poor man optimization - use if-else instead of switch,
    // to avoid deopts in old v8
    if (type === CODES) {
      base = extra = work;    /* dummy value--not used */
      end = 19;
  
    } else if (type === LENS) {
      base = lbase;
      base_index -= 257;
      extra = lext;
      extra_index -= 257;
      end = 256;
  
    } else {                    /* DISTS */
      base = dbase;
      extra = dext;
      end = -1;
    }
  
    /* initialize opts for loop */
    huff = 0;                   /* starting code */
    sym = 0;                    /* starting code symbol */
    len = min;                  /* starting code length */
    next = table_index;              /* current table to fill in */
    curr = root;                /* current table index bits */
    drop = 0;                   /* current bits to drop from code for index */
    low = -1;                   /* trigger new sub-table when len > root */
    used = 1 << root;          /* use root table entries */
    mask = used - 1;            /* mask for comparing low */
  
    /* check available table space */
    if ((type === LENS && used > ENOUGH_LENS) ||
      (type === DISTS && used > ENOUGH_DISTS)) {
      return 1;
    }
  
    /* process all codes and make table entries */
    for (;;) {
      /* create table entry */
      here_bits = len - drop;
      if (work[sym] < end) {
        here_op = 0;
        here_val = work[sym];
      }
      else if (work[sym] > end) {
        here_op = extra[extra_index + work[sym]];
        here_val = base[base_index + work[sym]];
      }
      else {
        here_op = 32 + 64;         /* end of block */
        here_val = 0;
      }
  
      /* replicate for those indices with low len bits equal to huff */
      incr = 1 << (len - drop);
      fill = 1 << curr;
      min = fill;                 /* save offset to next table */
      do {
        fill -= incr;
        table[next + (huff >> drop) + fill] = (here_bits << 24) | (here_op << 16) | here_val |0;
      } while (fill !== 0);
  
      /* backwards increment the len-bit code huff */
      incr = 1 << (len - 1);
      while (huff & incr) {
        incr >>= 1;
      }
      if (incr !== 0) {
        huff &= incr - 1;
        huff += incr;
      } else {
        huff = 0;
      }
  
      /* go to next symbol, update count, len */
      sym++;
      if (--count[len] === 0) {
        if (len === max) { break; }
        len = lens[lens_index + work[sym]];
      }
  
      /* create new sub-table if needed */
      if (len > root && (huff & mask) !== low) {
        /* if first time, transition to sub-tables */
        if (drop === 0) {
          drop = root;
        }
  
        /* increment past last table */
        next += min;            /* here min is 1 << curr */
  
        /* determine length of next table */
        curr = len - drop;
        left = 1 << curr;
        while (curr + drop < max) {
          left -= count[curr + drop];
          if (left <= 0) { break; }
          curr++;
          left <<= 1;
        }
  
        /* check for enough space */
        used += 1 << curr;
        if ((type === LENS && used > ENOUGH_LENS) ||
          (type === DISTS && used > ENOUGH_DISTS)) {
          return 1;
        }
  
        /* point entry in root table to sub-table */
        low = huff & mask;
        /*table.op[low] = curr;
        table.bits[low] = root;
        table.val[low] = next - opts.table_index;*/
        table[low] = (root << 24) | (curr << 16) | (next - table_index) |0;
      }
    }
  
    /* fill in remaining table entry if code is incomplete (guaranteed to have
     at most one remaining entry, since if the code is incomplete, the
     maximum code length that was allowed to get this far is one bit) */
    if (huff !== 0) {
      //table.op[next + huff] = 64;            /* invalid code marker */
      //table.bits[next + huff] = len - drop;
      //table.val[next + huff] = 0;
      table[next + huff] = ((len - drop) << 24) | (64 << 16) |0;
    }
  
    /* set return parameters */
    //opts.table_index += used;
    opts.bits = root;
    return 0;
  };
  
  },{"../utils/common":20}],28:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  module.exports = {
    2:      'need dictionary',     /* Z_NEED_DICT       2  */
    1:      'stream end',          /* Z_STREAM_END      1  */
    0:      '',                    /* Z_OK              0  */
    '-1':   'file error',          /* Z_ERRNO         (-1) */
    '-2':   'stream error',        /* Z_STREAM_ERROR  (-2) */
    '-3':   'data error',          /* Z_DATA_ERROR    (-3) */
    '-4':   'insufficient memory', /* Z_MEM_ERROR     (-4) */
    '-5':   'buffer error',        /* Z_BUF_ERROR     (-5) */
    '-6':   'incompatible version' /* Z_VERSION_ERROR (-6) */
  };
  
  },{}],29:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  /* eslint-disable space-unary-ops */
  
  var utils = require('../utils/common');
  
  /* Public constants ==========================================================*/
  /* ===========================================================================*/
  
  
  //var Z_FILTERED          = 1;
  //var Z_HUFFMAN_ONLY      = 2;
  //var Z_RLE               = 3;
  var Z_FIXED               = 4;
  //var Z_DEFAULT_STRATEGY  = 0;
  
  /* Possible values of the data_type field (though see inflate()) */
  var Z_BINARY              = 0;
  var Z_TEXT                = 1;
  //var Z_ASCII             = 1; // = Z_TEXT
  var Z_UNKNOWN             = 2;
  
  /*============================================================================*/
  
  
  function zero(buf) { var len = buf.length; while (--len >= 0) { buf[len] = 0; } }
  
  // From zutil.h
  
  var STORED_BLOCK = 0;
  var STATIC_TREES = 1;
  var DYN_TREES    = 2;
  /* The three kinds of block type */
  
  var MIN_MATCH    = 3;
  var MAX_MATCH    = 258;
  /* The minimum and maximum match lengths */
  
  // From deflate.h
  /* ===========================================================================
   * Internal compression state.
   */
  
  var LENGTH_CODES  = 29;
  /* number of length codes, not counting the special END_BLOCK code */
  
  var LITERALS      = 256;
  /* number of literal bytes 0..255 */
  
  var L_CODES       = LITERALS + 1 + LENGTH_CODES;
  /* number of Literal or Length codes, including the END_BLOCK code */
  
  var D_CODES       = 30;
  /* number of distance codes */
  
  var BL_CODES      = 19;
  /* number of codes used to transfer the bit lengths */
  
  var HEAP_SIZE     = 2 * L_CODES + 1;
  /* maximum heap size */
  
  var MAX_BITS      = 15;
  /* All codes must not exceed MAX_BITS bits */
  
  var Buf_size      = 16;
  /* size of bit buffer in bi_buf */
  
  
  /* ===========================================================================
   * Constants
   */
  
  var MAX_BL_BITS = 7;
  /* Bit length codes must not exceed MAX_BL_BITS bits */
  
  var END_BLOCK   = 256;
  /* end of block literal code */
  
  var REP_3_6     = 16;
  /* repeat previous bit length 3-6 times (2 bits of repeat count) */
  
  var REPZ_3_10   = 17;
  /* repeat a zero length 3-10 times  (3 bits of repeat count) */
  
  var REPZ_11_138 = 18;
  /* repeat a zero length 11-138 times  (7 bits of repeat count) */
  
  /* eslint-disable comma-spacing,array-bracket-spacing */
  var extra_lbits =   /* extra bits for each length code */
    [0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0];
  
  var extra_dbits =   /* extra bits for each distance code */
    [0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13];
  
  var extra_blbits =  /* extra bits for each bit length code */
    [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,7];
  
  var bl_order =
    [16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15];
  /* eslint-enable comma-spacing,array-bracket-spacing */
  
  /* The lengths of the bit length codes are sent in order of decreasing
   * probability, to avoid transmitting the lengths for unused bit length codes.
   */
  
  /* ===========================================================================
   * Local data. These are initialized only once.
   */
  
  // We pre-fill arrays with 0 to avoid uninitialized gaps
  
  var DIST_CODE_LEN = 512; /* see definition of array dist_code below */
  
  // !!!! Use flat array instead of structure, Freq = i*2, Len = i*2+1
  var static_ltree  = new Array((L_CODES + 2) * 2);
  zero(static_ltree);
  /* The static literal tree. Since the bit lengths are imposed, there is no
   * need for the L_CODES extra codes used during heap construction. However
   * The codes 286 and 287 are needed to build a canonical tree (see _tr_init
   * below).
   */
  
  var static_dtree  = new Array(D_CODES * 2);
  zero(static_dtree);
  /* The static distance tree. (Actually a trivial tree since all codes use
   * 5 bits.)
   */
  
  var _dist_code    = new Array(DIST_CODE_LEN);
  zero(_dist_code);
  /* Distance codes. The first 256 values correspond to the distances
   * 3 .. 258, the last 256 values correspond to the top 8 bits of
   * the 15 bit distances.
   */
  
  var _length_code  = new Array(MAX_MATCH - MIN_MATCH + 1);
  zero(_length_code);
  /* length code for each normalized match length (0 == MIN_MATCH) */
  
  var base_length   = new Array(LENGTH_CODES);
  zero(base_length);
  /* First normalized length for each code (0 = MIN_MATCH) */
  
  var base_dist     = new Array(D_CODES);
  zero(base_dist);
  /* First normalized distance for each code (0 = distance of 1) */
  
  
  function StaticTreeDesc(static_tree, extra_bits, extra_base, elems, max_length) {
  
    this.static_tree  = static_tree;  /* static tree or NULL */
    this.extra_bits   = extra_bits;   /* extra bits for each code or NULL */
    this.extra_base   = extra_base;   /* base index for extra_bits */
    this.elems        = elems;        /* max number of elements in the tree */
    this.max_length   = max_length;   /* max bit length for the codes */
  
    // show if `static_tree` has data or dummy - needed for monomorphic objects
    this.has_stree    = static_tree && static_tree.length;
  }
  
  
  var static_l_desc;
  var static_d_desc;
  var static_bl_desc;
  
  
  function TreeDesc(dyn_tree, stat_desc) {
    this.dyn_tree = dyn_tree;     /* the dynamic tree */
    this.max_code = 0;            /* largest code with non zero frequency */
    this.stat_desc = stat_desc;   /* the corresponding static tree */
  }
  
  
  
  function d_code(dist) {
    return dist < 256 ? _dist_code[dist] : _dist_code[256 + (dist >>> 7)];
  }
  
  
  /* ===========================================================================
   * Output a short LSB first on the stream.
   * IN assertion: there is enough room in pendingBuf.
   */
  function put_short(s, w) {
  //    put_byte(s, (uch)((w) & 0xff));
  //    put_byte(s, (uch)((ush)(w) >> 8));
    s.pending_buf[s.pending++] = (w) & 0xff;
    s.pending_buf[s.pending++] = (w >>> 8) & 0xff;
  }
  
  
  /* ===========================================================================
   * Send a value on a given number of bits.
   * IN assertion: length <= 16 and value fits in length bits.
   */
  function send_bits(s, value, length) {
    if (s.bi_valid > (Buf_size - length)) {
      s.bi_buf |= (value << s.bi_valid) & 0xffff;
      put_short(s, s.bi_buf);
      s.bi_buf = value >> (Buf_size - s.bi_valid);
      s.bi_valid += length - Buf_size;
    } else {
      s.bi_buf |= (value << s.bi_valid) & 0xffff;
      s.bi_valid += length;
    }
  }
  
  
  function send_code(s, c, tree) {
    send_bits(s, tree[c * 2]/*.Code*/, tree[c * 2 + 1]/*.Len*/);
  }
  
  
  /* ===========================================================================
   * Reverse the first len bits of a code, using straightforward code (a faster
   * method would use a table)
   * IN assertion: 1 <= len <= 15
   */
  function bi_reverse(code, len) {
    var res = 0;
    do {
      res |= code & 1;
      code >>>= 1;
      res <<= 1;
    } while (--len > 0);
    return res >>> 1;
  }
  
  
  /* ===========================================================================
   * Flush the bit buffer, keeping at most 7 bits in it.
   */
  function bi_flush(s) {
    if (s.bi_valid === 16) {
      put_short(s, s.bi_buf);
      s.bi_buf = 0;
      s.bi_valid = 0;
  
    } else if (s.bi_valid >= 8) {
      s.pending_buf[s.pending++] = s.bi_buf & 0xff;
      s.bi_buf >>= 8;
      s.bi_valid -= 8;
    }
  }
  
  
  /* ===========================================================================
   * Compute the optimal bit lengths for a tree and update the total bit length
   * for the current block.
   * IN assertion: the fields freq and dad are set, heap[heap_max] and
   *    above are the tree nodes sorted by increasing frequency.
   * OUT assertions: the field len is set to the optimal bit length, the
   *     array bl_count contains the frequencies for each bit length.
   *     The length opt_len is updated; static_len is also updated if stree is
   *     not null.
   */
  function gen_bitlen(s, desc)
  //    deflate_state *s;
  //    tree_desc *desc;    /* the tree descriptor */
  {
    var tree            = desc.dyn_tree;
    var max_code        = desc.max_code;
    var stree           = desc.stat_desc.static_tree;
    var has_stree       = desc.stat_desc.has_stree;
    var extra           = desc.stat_desc.extra_bits;
    var base            = desc.stat_desc.extra_base;
    var max_length      = desc.stat_desc.max_length;
    var h;              /* heap index */
    var n, m;           /* iterate over the tree elements */
    var bits;           /* bit length */
    var xbits;          /* extra bits */
    var f;              /* frequency */
    var overflow = 0;   /* number of elements with bit length too large */
  
    for (bits = 0; bits <= MAX_BITS; bits++) {
      s.bl_count[bits] = 0;
    }
  
    /* In a first pass, compute the optimal bit lengths (which may
     * overflow in the case of the bit length tree).
     */
    tree[s.heap[s.heap_max] * 2 + 1]/*.Len*/ = 0; /* root of the heap */
  
    for (h = s.heap_max + 1; h < HEAP_SIZE; h++) {
      n = s.heap[h];
      bits = tree[tree[n * 2 + 1]/*.Dad*/ * 2 + 1]/*.Len*/ + 1;
      if (bits > max_length) {
        bits = max_length;
        overflow++;
      }
      tree[n * 2 + 1]/*.Len*/ = bits;
      /* We overwrite tree[n].Dad which is no longer needed */
  
      if (n > max_code) { continue; } /* not a leaf node */
  
      s.bl_count[bits]++;
      xbits = 0;
      if (n >= base) {
        xbits = extra[n - base];
      }
      f = tree[n * 2]/*.Freq*/;
      s.opt_len += f * (bits + xbits);
      if (has_stree) {
        s.static_len += f * (stree[n * 2 + 1]/*.Len*/ + xbits);
      }
    }
    if (overflow === 0) { return; }
  
    // Trace((stderr,"\nbit length overflow\n"));
    /* This happens for example on obj2 and pic of the Calgary corpus */
  
    /* Find the first bit length which could increase: */
    do {
      bits = max_length - 1;
      while (s.bl_count[bits] === 0) { bits--; }
      s.bl_count[bits]--;      /* move one leaf down the tree */
      s.bl_count[bits + 1] += 2; /* move one overflow item as its brother */
      s.bl_count[max_length]--;
      /* The brother of the overflow item also moves one step up,
       * but this does not affect bl_count[max_length]
       */
      overflow -= 2;
    } while (overflow > 0);
  
    /* Now recompute all bit lengths, scanning in increasing frequency.
     * h is still equal to HEAP_SIZE. (It is simpler to reconstruct all
     * lengths instead of fixing only the wrong ones. This idea is taken
     * from 'ar' written by Haruhiko Okumura.)
     */
    for (bits = max_length; bits !== 0; bits--) {
      n = s.bl_count[bits];
      while (n !== 0) {
        m = s.heap[--h];
        if (m > max_code) { continue; }
        if (tree[m * 2 + 1]/*.Len*/ !== bits) {
          // Trace((stderr,"code %d bits %d->%d\n", m, tree[m].Len, bits));
          s.opt_len += (bits - tree[m * 2 + 1]/*.Len*/) * tree[m * 2]/*.Freq*/;
          tree[m * 2 + 1]/*.Len*/ = bits;
        }
        n--;
      }
    }
  }
  
  
  /* ===========================================================================
   * Generate the codes for a given tree and bit counts (which need not be
   * optimal).
   * IN assertion: the array bl_count contains the bit length statistics for
   * the given tree and the field len is set for all tree elements.
   * OUT assertion: the field code is set for all tree elements of non
   *     zero code length.
   */
  function gen_codes(tree, max_code, bl_count)
  //    ct_data *tree;             /* the tree to decorate */
  //    int max_code;              /* largest code with non zero frequency */
  //    ushf *bl_count;            /* number of codes at each bit length */
  {
    var next_code = new Array(MAX_BITS + 1); /* next code value for each bit length */
    var code = 0;              /* running code value */
    var bits;                  /* bit index */
    var n;                     /* code index */
  
    /* The distribution counts are first used to generate the code values
     * without bit reversal.
     */
    for (bits = 1; bits <= MAX_BITS; bits++) {
      next_code[bits] = code = (code + bl_count[bits - 1]) << 1;
    }
    /* Check that the bit counts in bl_count are consistent. The last code
     * must be all ones.
     */
    //Assert (code + bl_count[MAX_BITS]-1 == (1<<MAX_BITS)-1,
    //        "inconsistent bit counts");
    //Tracev((stderr,"\ngen_codes: max_code %d ", max_code));
  
    for (n = 0;  n <= max_code; n++) {
      var len = tree[n * 2 + 1]/*.Len*/;
      if (len === 0) { continue; }
      /* Now reverse the bits */
      tree[n * 2]/*.Code*/ = bi_reverse(next_code[len]++, len);
  
      //Tracecv(tree != static_ltree, (stderr,"\nn %3d %c l %2d c %4x (%x) ",
      //     n, (isgraph(n) ? n : ' '), len, tree[n].Code, next_code[len]-1));
    }
  }
  
  
  /* ===========================================================================
   * Initialize the various 'constant' tables.
   */
  function tr_static_init() {
    var n;        /* iterates over tree elements */
    var bits;     /* bit counter */
    var length;   /* length value */
    var code;     /* code value */
    var dist;     /* distance index */
    var bl_count = new Array(MAX_BITS + 1);
    /* number of codes at each bit length for an optimal tree */
  
    // do check in _tr_init()
    //if (static_init_done) return;
  
    /* For some embedded targets, global variables are not initialized: */
  /*#ifdef NO_INIT_GLOBAL_POINTERS
    static_l_desc.static_tree = static_ltree;
    static_l_desc.extra_bits = extra_lbits;
    static_d_desc.static_tree = static_dtree;
    static_d_desc.extra_bits = extra_dbits;
    static_bl_desc.extra_bits = extra_blbits;
  #endif*/
  
    /* Initialize the mapping length (0..255) -> length code (0..28) */
    length = 0;
    for (code = 0; code < LENGTH_CODES - 1; code++) {
      base_length[code] = length;
      for (n = 0; n < (1 << extra_lbits[code]); n++) {
        _length_code[length++] = code;
      }
    }
    //Assert (length == 256, "tr_static_init: length != 256");
    /* Note that the length 255 (match length 258) can be represented
     * in two different ways: code 284 + 5 bits or code 285, so we
     * overwrite length_code[255] to use the best encoding:
     */
    _length_code[length - 1] = code;
  
    /* Initialize the mapping dist (0..32K) -> dist code (0..29) */
    dist = 0;
    for (code = 0; code < 16; code++) {
      base_dist[code] = dist;
      for (n = 0; n < (1 << extra_dbits[code]); n++) {
        _dist_code[dist++] = code;
      }
    }
    //Assert (dist == 256, "tr_static_init: dist != 256");
    dist >>= 7; /* from now on, all distances are divided by 128 */
    for (; code < D_CODES; code++) {
      base_dist[code] = dist << 7;
      for (n = 0; n < (1 << (extra_dbits[code] - 7)); n++) {
        _dist_code[256 + dist++] = code;
      }
    }
    //Assert (dist == 256, "tr_static_init: 256+dist != 512");
  
    /* Construct the codes of the static literal tree */
    for (bits = 0; bits <= MAX_BITS; bits++) {
      bl_count[bits] = 0;
    }
  
    n = 0;
    while (n <= 143) {
      static_ltree[n * 2 + 1]/*.Len*/ = 8;
      n++;
      bl_count[8]++;
    }
    while (n <= 255) {
      static_ltree[n * 2 + 1]/*.Len*/ = 9;
      n++;
      bl_count[9]++;
    }
    while (n <= 279) {
      static_ltree[n * 2 + 1]/*.Len*/ = 7;
      n++;
      bl_count[7]++;
    }
    while (n <= 287) {
      static_ltree[n * 2 + 1]/*.Len*/ = 8;
      n++;
      bl_count[8]++;
    }
    /* Codes 286 and 287 do not exist, but we must include them in the
     * tree construction to get a canonical Huffman tree (longest code
     * all ones)
     */
    gen_codes(static_ltree, L_CODES + 1, bl_count);
  
    /* The static distance tree is trivial: */
    for (n = 0; n < D_CODES; n++) {
      static_dtree[n * 2 + 1]/*.Len*/ = 5;
      static_dtree[n * 2]/*.Code*/ = bi_reverse(n, 5);
    }
  
    // Now data ready and we can init static trees
    static_l_desc = new StaticTreeDesc(static_ltree, extra_lbits, LITERALS + 1, L_CODES, MAX_BITS);
    static_d_desc = new StaticTreeDesc(static_dtree, extra_dbits, 0,          D_CODES, MAX_BITS);
    static_bl_desc = new StaticTreeDesc(new Array(0), extra_blbits, 0,         BL_CODES, MAX_BL_BITS);
  
    //static_init_done = true;
  }
  
  
  /* ===========================================================================
   * Initialize a new block.
   */
  function init_block(s) {
    var n; /* iterates over tree elements */
  
    /* Initialize the trees. */
    for (n = 0; n < L_CODES;  n++) { s.dyn_ltree[n * 2]/*.Freq*/ = 0; }
    for (n = 0; n < D_CODES;  n++) { s.dyn_dtree[n * 2]/*.Freq*/ = 0; }
    for (n = 0; n < BL_CODES; n++) { s.bl_tree[n * 2]/*.Freq*/ = 0; }
  
    s.dyn_ltree[END_BLOCK * 2]/*.Freq*/ = 1;
    s.opt_len = s.static_len = 0;
    s.last_lit = s.matches = 0;
  }
  
  
  /* ===========================================================================
   * Flush the bit buffer and align the output on a byte boundary
   */
  function bi_windup(s)
  {
    if (s.bi_valid > 8) {
      put_short(s, s.bi_buf);
    } else if (s.bi_valid > 0) {
      //put_byte(s, (Byte)s->bi_buf);
      s.pending_buf[s.pending++] = s.bi_buf;
    }
    s.bi_buf = 0;
    s.bi_valid = 0;
  }
  
  /* ===========================================================================
   * Copy a stored block, storing first the length and its
   * one's complement if requested.
   */
  function copy_block(s, buf, len, header)
  //DeflateState *s;
  //charf    *buf;    /* the input data */
  //unsigned len;     /* its length */
  //int      header;  /* true if block header must be written */
  {
    bi_windup(s);        /* align on byte boundary */
  
    if (header) {
      put_short(s, len);
      put_short(s, ~len);
    }
  //  while (len--) {
  //    put_byte(s, *buf++);
  //  }
    utils.arraySet(s.pending_buf, s.window, buf, len, s.pending);
    s.pending += len;
  }
  
  /* ===========================================================================
   * Compares to subtrees, using the tree depth as tie breaker when
   * the subtrees have equal frequency. This minimizes the worst case length.
   */
  function smaller(tree, n, m, depth) {
    var _n2 = n * 2;
    var _m2 = m * 2;
    return (tree[_n2]/*.Freq*/ < tree[_m2]/*.Freq*/ ||
           (tree[_n2]/*.Freq*/ === tree[_m2]/*.Freq*/ && depth[n] <= depth[m]));
  }
  
  /* ===========================================================================
   * Restore the heap property by moving down the tree starting at node k,
   * exchanging a node with the smallest of its two sons if necessary, stopping
   * when the heap property is re-established (each father smaller than its
   * two sons).
   */
  function pqdownheap(s, tree, k)
  //    deflate_state *s;
  //    ct_data *tree;  /* the tree to restore */
  //    int k;               /* node to move down */
  {
    var v = s.heap[k];
    var j = k << 1;  /* left son of k */
    while (j <= s.heap_len) {
      /* Set j to the smallest of the two sons: */
      if (j < s.heap_len &&
        smaller(tree, s.heap[j + 1], s.heap[j], s.depth)) {
        j++;
      }
      /* Exit if v is smaller than both sons */
      if (smaller(tree, v, s.heap[j], s.depth)) { break; }
  
      /* Exchange v with the smallest son */
      s.heap[k] = s.heap[j];
      k = j;
  
      /* And continue down the tree, setting j to the left son of k */
      j <<= 1;
    }
    s.heap[k] = v;
  }
  
  
  // inlined manually
  // var SMALLEST = 1;
  
  /* ===========================================================================
   * Send the block data compressed using the given Huffman trees
   */
  function compress_block(s, ltree, dtree)
  //    deflate_state *s;
  //    const ct_data *ltree; /* literal tree */
  //    const ct_data *dtree; /* distance tree */
  {
    var dist;           /* distance of matched string */
    var lc;             /* match length or unmatched char (if dist == 0) */
    var lx = 0;         /* running index in l_buf */
    var code;           /* the code to send */
    var extra;          /* number of extra bits to send */
  
    if (s.last_lit !== 0) {
      do {
        dist = (s.pending_buf[s.d_buf + lx * 2] << 8) | (s.pending_buf[s.d_buf + lx * 2 + 1]);
        lc = s.pending_buf[s.l_buf + lx];
        lx++;
  
        if (dist === 0) {
          send_code(s, lc, ltree); /* send a literal byte */
          //Tracecv(isgraph(lc), (stderr," '%c' ", lc));
        } else {
          /* Here, lc is the match length - MIN_MATCH */
          code = _length_code[lc];
          send_code(s, code + LITERALS + 1, ltree); /* send the length code */
          extra = extra_lbits[code];
          if (extra !== 0) {
            lc -= base_length[code];
            send_bits(s, lc, extra);       /* send the extra length bits */
          }
          dist--; /* dist is now the match distance - 1 */
          code = d_code(dist);
          //Assert (code < D_CODES, "bad d_code");
  
          send_code(s, code, dtree);       /* send the distance code */
          extra = extra_dbits[code];
          if (extra !== 0) {
            dist -= base_dist[code];
            send_bits(s, dist, extra);   /* send the extra distance bits */
          }
        } /* literal or match pair ? */
  
        /* Check that the overlay between pending_buf and d_buf+l_buf is ok: */
        //Assert((uInt)(s->pending) < s->lit_bufsize + 2*lx,
        //       "pendingBuf overflow");
  
      } while (lx < s.last_lit);
    }
  
    send_code(s, END_BLOCK, ltree);
  }
  
  
  /* ===========================================================================
   * Construct one Huffman tree and assigns the code bit strings and lengths.
   * Update the total bit length for the current block.
   * IN assertion: the field freq is set for all tree elements.
   * OUT assertions: the fields len and code are set to the optimal bit length
   *     and corresponding code. The length opt_len is updated; static_len is
   *     also updated if stree is not null. The field max_code is set.
   */
  function build_tree(s, desc)
  //    deflate_state *s;
  //    tree_desc *desc; /* the tree descriptor */
  {
    var tree     = desc.dyn_tree;
    var stree    = desc.stat_desc.static_tree;
    var has_stree = desc.stat_desc.has_stree;
    var elems    = desc.stat_desc.elems;
    var n, m;          /* iterate over heap elements */
    var max_code = -1; /* largest code with non zero frequency */
    var node;          /* new node being created */
  
    /* Construct the initial heap, with least frequent element in
     * heap[SMALLEST]. The sons of heap[n] are heap[2*n] and heap[2*n+1].
     * heap[0] is not used.
     */
    s.heap_len = 0;
    s.heap_max = HEAP_SIZE;
  
    for (n = 0; n < elems; n++) {
      if (tree[n * 2]/*.Freq*/ !== 0) {
        s.heap[++s.heap_len] = max_code = n;
        s.depth[n] = 0;
  
      } else {
        tree[n * 2 + 1]/*.Len*/ = 0;
      }
    }
  
    /* The pkzip format requires that at least one distance code exists,
     * and that at least one bit should be sent even if there is only one
     * possible code. So to avoid special checks later on we force at least
     * two codes of non zero frequency.
     */
    while (s.heap_len < 2) {
      node = s.heap[++s.heap_len] = (max_code < 2 ? ++max_code : 0);
      tree[node * 2]/*.Freq*/ = 1;
      s.depth[node] = 0;
      s.opt_len--;
  
      if (has_stree) {
        s.static_len -= stree[node * 2 + 1]/*.Len*/;
      }
      /* node is 0 or 1 so it does not have extra bits */
    }
    desc.max_code = max_code;
  
    /* The elements heap[heap_len/2+1 .. heap_len] are leaves of the tree,
     * establish sub-heaps of increasing lengths:
     */
    for (n = (s.heap_len >> 1/*int /2*/); n >= 1; n--) { pqdownheap(s, tree, n); }
  
    /* Construct the Huffman tree by repeatedly combining the least two
     * frequent nodes.
     */
    node = elems;              /* next internal node of the tree */
    do {
      //pqremove(s, tree, n);  /* n = node of least frequency */
      /*** pqremove ***/
      n = s.heap[1/*SMALLEST*/];
      s.heap[1/*SMALLEST*/] = s.heap[s.heap_len--];
      pqdownheap(s, tree, 1/*SMALLEST*/);
      /***/
  
      m = s.heap[1/*SMALLEST*/]; /* m = node of next least frequency */
  
      s.heap[--s.heap_max] = n; /* keep the nodes sorted by frequency */
      s.heap[--s.heap_max] = m;
  
      /* Create a new node father of n and m */
      tree[node * 2]/*.Freq*/ = tree[n * 2]/*.Freq*/ + tree[m * 2]/*.Freq*/;
      s.depth[node] = (s.depth[n] >= s.depth[m] ? s.depth[n] : s.depth[m]) + 1;
      tree[n * 2 + 1]/*.Dad*/ = tree[m * 2 + 1]/*.Dad*/ = node;
  
      /* and insert the new node in the heap */
      s.heap[1/*SMALLEST*/] = node++;
      pqdownheap(s, tree, 1/*SMALLEST*/);
  
    } while (s.heap_len >= 2);
  
    s.heap[--s.heap_max] = s.heap[1/*SMALLEST*/];
  
    /* At this point, the fields freq and dad are set. We can now
     * generate the bit lengths.
     */
    gen_bitlen(s, desc);
  
    /* The field len is now set, we can generate the bit codes */
    gen_codes(tree, max_code, s.bl_count);
  }
  
  
  /* ===========================================================================
   * Scan a literal or distance tree to determine the frequencies of the codes
   * in the bit length tree.
   */
  function scan_tree(s, tree, max_code)
  //    deflate_state *s;
  //    ct_data *tree;   /* the tree to be scanned */
  //    int max_code;    /* and its largest code of non zero frequency */
  {
    var n;                     /* iterates over all tree elements */
    var prevlen = -1;          /* last emitted length */
    var curlen;                /* length of current code */
  
    var nextlen = tree[0 * 2 + 1]/*.Len*/; /* length of next code */
  
    var count = 0;             /* repeat count of the current code */
    var max_count = 7;         /* max repeat count */
    var min_count = 4;         /* min repeat count */
  
    if (nextlen === 0) {
      max_count = 138;
      min_count = 3;
    }
    tree[(max_code + 1) * 2 + 1]/*.Len*/ = 0xffff; /* guard */
  
    for (n = 0; n <= max_code; n++) {
      curlen = nextlen;
      nextlen = tree[(n + 1) * 2 + 1]/*.Len*/;
  
      if (++count < max_count && curlen === nextlen) {
        continue;
  
      } else if (count < min_count) {
        s.bl_tree[curlen * 2]/*.Freq*/ += count;
  
      } else if (curlen !== 0) {
  
        if (curlen !== prevlen) { s.bl_tree[curlen * 2]/*.Freq*/++; }
        s.bl_tree[REP_3_6 * 2]/*.Freq*/++;
  
      } else if (count <= 10) {
        s.bl_tree[REPZ_3_10 * 2]/*.Freq*/++;
  
      } else {
        s.bl_tree[REPZ_11_138 * 2]/*.Freq*/++;
      }
  
      count = 0;
      prevlen = curlen;
  
      if (nextlen === 0) {
        max_count = 138;
        min_count = 3;
  
      } else if (curlen === nextlen) {
        max_count = 6;
        min_count = 3;
  
      } else {
        max_count = 7;
        min_count = 4;
      }
    }
  }
  
  
  /* ===========================================================================
   * Send a literal or distance tree in compressed form, using the codes in
   * bl_tree.
   */
  function send_tree(s, tree, max_code)
  //    deflate_state *s;
  //    ct_data *tree; /* the tree to be scanned */
  //    int max_code;       /* and its largest code of non zero frequency */
  {
    var n;                     /* iterates over all tree elements */
    var prevlen = -1;          /* last emitted length */
    var curlen;                /* length of current code */
  
    var nextlen = tree[0 * 2 + 1]/*.Len*/; /* length of next code */
  
    var count = 0;             /* repeat count of the current code */
    var max_count = 7;         /* max repeat count */
    var min_count = 4;         /* min repeat count */
  
    /* tree[max_code+1].Len = -1; */  /* guard already set */
    if (nextlen === 0) {
      max_count = 138;
      min_count = 3;
    }
  
    for (n = 0; n <= max_code; n++) {
      curlen = nextlen;
      nextlen = tree[(n + 1) * 2 + 1]/*.Len*/;
  
      if (++count < max_count && curlen === nextlen) {
        continue;
  
      } else if (count < min_count) {
        do { send_code(s, curlen, s.bl_tree); } while (--count !== 0);
  
      } else if (curlen !== 0) {
        if (curlen !== prevlen) {
          send_code(s, curlen, s.bl_tree);
          count--;
        }
        //Assert(count >= 3 && count <= 6, " 3_6?");
        send_code(s, REP_3_6, s.bl_tree);
        send_bits(s, count - 3, 2);
  
      } else if (count <= 10) {
        send_code(s, REPZ_3_10, s.bl_tree);
        send_bits(s, count - 3, 3);
  
      } else {
        send_code(s, REPZ_11_138, s.bl_tree);
        send_bits(s, count - 11, 7);
      }
  
      count = 0;
      prevlen = curlen;
      if (nextlen === 0) {
        max_count = 138;
        min_count = 3;
  
      } else if (curlen === nextlen) {
        max_count = 6;
        min_count = 3;
  
      } else {
        max_count = 7;
        min_count = 4;
      }
    }
  }
  
  
  /* ===========================================================================
   * Construct the Huffman tree for the bit lengths and return the index in
   * bl_order of the last bit length code to send.
   */
  function build_bl_tree(s) {
    var max_blindex;  /* index of last bit length code of non zero freq */
  
    /* Determine the bit length frequencies for literal and distance trees */
    scan_tree(s, s.dyn_ltree, s.l_desc.max_code);
    scan_tree(s, s.dyn_dtree, s.d_desc.max_code);
  
    /* Build the bit length tree: */
    build_tree(s, s.bl_desc);
    /* opt_len now includes the length of the tree representations, except
     * the lengths of the bit lengths codes and the 5+5+4 bits for the counts.
     */
  
    /* Determine the number of bit length codes to send. The pkzip format
     * requires that at least 4 bit length codes be sent. (appnote.txt says
     * 3 but the actual value used is 4.)
     */
    for (max_blindex = BL_CODES - 1; max_blindex >= 3; max_blindex--) {
      if (s.bl_tree[bl_order[max_blindex] * 2 + 1]/*.Len*/ !== 0) {
        break;
      }
    }
    /* Update opt_len to include the bit length tree and counts */
    s.opt_len += 3 * (max_blindex + 1) + 5 + 5 + 4;
    //Tracev((stderr, "\ndyn trees: dyn %ld, stat %ld",
    //        s->opt_len, s->static_len));
  
    return max_blindex;
  }
  
  
  /* ===========================================================================
   * Send the header for a block using dynamic Huffman trees: the counts, the
   * lengths of the bit length codes, the literal tree and the distance tree.
   * IN assertion: lcodes >= 257, dcodes >= 1, blcodes >= 4.
   */
  function send_all_trees(s, lcodes, dcodes, blcodes)
  //    deflate_state *s;
  //    int lcodes, dcodes, blcodes; /* number of codes for each tree */
  {
    var rank;                    /* index in bl_order */
  
    //Assert (lcodes >= 257 && dcodes >= 1 && blcodes >= 4, "not enough codes");
    //Assert (lcodes <= L_CODES && dcodes <= D_CODES && blcodes <= BL_CODES,
    //        "too many codes");
    //Tracev((stderr, "\nbl counts: "));
    send_bits(s, lcodes - 257, 5); /* not +255 as stated in appnote.txt */
    send_bits(s, dcodes - 1,   5);
    send_bits(s, blcodes - 4,  4); /* not -3 as stated in appnote.txt */
    for (rank = 0; rank < blcodes; rank++) {
      //Tracev((stderr, "\nbl code %2d ", bl_order[rank]));
      send_bits(s, s.bl_tree[bl_order[rank] * 2 + 1]/*.Len*/, 3);
    }
    //Tracev((stderr, "\nbl tree: sent %ld", s->bits_sent));
  
    send_tree(s, s.dyn_ltree, lcodes - 1); /* literal tree */
    //Tracev((stderr, "\nlit tree: sent %ld", s->bits_sent));
  
    send_tree(s, s.dyn_dtree, dcodes - 1); /* distance tree */
    //Tracev((stderr, "\ndist tree: sent %ld", s->bits_sent));
  }
  
  
  /* ===========================================================================
   * Check if the data type is TEXT or BINARY, using the following algorithm:
   * - TEXT if the two conditions below are satisfied:
   *    a) There are no non-portable control characters belonging to the
   *       "black list" (0..6, 14..25, 28..31).
   *    b) There is at least one printable character belonging to the
   *       "white list" (9 {TAB}, 10 {LF}, 13 {CR}, 32..255).
   * - BINARY otherwise.
   * - The following partially-portable control characters form a
   *   "gray list" that is ignored in this detection algorithm:
   *   (7 {BEL}, 8 {BS}, 11 {VT}, 12 {FF}, 26 {SUB}, 27 {ESC}).
   * IN assertion: the fields Freq of dyn_ltree are set.
   */
  function detect_data_type(s) {
    /* black_mask is the bit mask of black-listed bytes
     * set bits 0..6, 14..25, and 28..31
     * 0xf3ffc07f = binary 11110011111111111100000001111111
     */
    var black_mask = 0xf3ffc07f;
    var n;
  
    /* Check for non-textual ("black-listed") bytes. */
    for (n = 0; n <= 31; n++, black_mask >>>= 1) {
      if ((black_mask & 1) && (s.dyn_ltree[n * 2]/*.Freq*/ !== 0)) {
        return Z_BINARY;
      }
    }
  
    /* Check for textual ("white-listed") bytes. */
    if (s.dyn_ltree[9 * 2]/*.Freq*/ !== 0 || s.dyn_ltree[10 * 2]/*.Freq*/ !== 0 ||
        s.dyn_ltree[13 * 2]/*.Freq*/ !== 0) {
      return Z_TEXT;
    }
    for (n = 32; n < LITERALS; n++) {
      if (s.dyn_ltree[n * 2]/*.Freq*/ !== 0) {
        return Z_TEXT;
      }
    }
  
    /* There are no "black-listed" or "white-listed" bytes:
     * this stream either is empty or has tolerated ("gray-listed") bytes only.
     */
    return Z_BINARY;
  }
  
  
  var static_init_done = false;
  
  /* ===========================================================================
   * Initialize the tree data structures for a new zlib stream.
   */
  function _tr_init(s)
  {
  
    if (!static_init_done) {
      tr_static_init();
      static_init_done = true;
    }
  
    s.l_desc  = new TreeDesc(s.dyn_ltree, static_l_desc);
    s.d_desc  = new TreeDesc(s.dyn_dtree, static_d_desc);
    s.bl_desc = new TreeDesc(s.bl_tree, static_bl_desc);
  
    s.bi_buf = 0;
    s.bi_valid = 0;
  
    /* Initialize the first block of the first file: */
    init_block(s);
  }
  
  
  /* ===========================================================================
   * Send a stored block
   */
  function _tr_stored_block(s, buf, stored_len, last)
  //DeflateState *s;
  //charf *buf;       /* input block */
  //ulg stored_len;   /* length of input block */
  //int last;         /* one if this is the last block for a file */
  {
    send_bits(s, (STORED_BLOCK << 1) + (last ? 1 : 0), 3);    /* send block type */
    copy_block(s, buf, stored_len, true); /* with header */
  }
  
  
  /* ===========================================================================
   * Send one empty static block to give enough lookahead for inflate.
   * This takes 10 bits, of which 7 may remain in the bit buffer.
   */
  function _tr_align(s) {
    send_bits(s, STATIC_TREES << 1, 3);
    send_code(s, END_BLOCK, static_ltree);
    bi_flush(s);
  }
  
  
  /* ===========================================================================
   * Determine the best encoding for the current block: dynamic trees, static
   * trees or store, and output the encoded block to the zip file.
   */
  function _tr_flush_block(s, buf, stored_len, last)
  //DeflateState *s;
  //charf *buf;       /* input block, or NULL if too old */
  //ulg stored_len;   /* length of input block */
  //int last;         /* one if this is the last block for a file */
  {
    var opt_lenb, static_lenb;  /* opt_len and static_len in bytes */
    var max_blindex = 0;        /* index of last bit length code of non zero freq */
  
    /* Build the Huffman trees unless a stored block is forced */
    if (s.level > 0) {
  
      /* Check if the file is binary or text */
      if (s.strm.data_type === Z_UNKNOWN) {
        s.strm.data_type = detect_data_type(s);
      }
  
      /* Construct the literal and distance trees */
      build_tree(s, s.l_desc);
      // Tracev((stderr, "\nlit data: dyn %ld, stat %ld", s->opt_len,
      //        s->static_len));
  
      build_tree(s, s.d_desc);
      // Tracev((stderr, "\ndist data: dyn %ld, stat %ld", s->opt_len,
      //        s->static_len));
      /* At this point, opt_len and static_len are the total bit lengths of
       * the compressed block data, excluding the tree representations.
       */
  
      /* Build the bit length tree for the above two trees, and get the index
       * in bl_order of the last bit length code to send.
       */
      max_blindex = build_bl_tree(s);
  
      /* Determine the best encoding. Compute the block lengths in bytes. */
      opt_lenb = (s.opt_len + 3 + 7) >>> 3;
      static_lenb = (s.static_len + 3 + 7) >>> 3;
  
      // Tracev((stderr, "\nopt %lu(%lu) stat %lu(%lu) stored %lu lit %u ",
      //        opt_lenb, s->opt_len, static_lenb, s->static_len, stored_len,
      //        s->last_lit));
  
      if (static_lenb <= opt_lenb) { opt_lenb = static_lenb; }
  
    } else {
      // Assert(buf != (char*)0, "lost buf");
      opt_lenb = static_lenb = stored_len + 5; /* force a stored block */
    }
  
    if ((stored_len + 4 <= opt_lenb) && (buf !== -1)) {
      /* 4: two words for the lengths */
  
      /* The test buf != NULL is only necessary if LIT_BUFSIZE > WSIZE.
       * Otherwise we can't have processed more than WSIZE input bytes since
       * the last block flush, because compression would have been
       * successful. If LIT_BUFSIZE <= WSIZE, it is never too late to
       * transform a block into a stored block.
       */
      _tr_stored_block(s, buf, stored_len, last);
  
    } else if (s.strategy === Z_FIXED || static_lenb === opt_lenb) {
  
      send_bits(s, (STATIC_TREES << 1) + (last ? 1 : 0), 3);
      compress_block(s, static_ltree, static_dtree);
  
    } else {
      send_bits(s, (DYN_TREES << 1) + (last ? 1 : 0), 3);
      send_all_trees(s, s.l_desc.max_code + 1, s.d_desc.max_code + 1, max_blindex + 1);
      compress_block(s, s.dyn_ltree, s.dyn_dtree);
    }
    // Assert (s->compressed_len == s->bits_sent, "bad compressed size");
    /* The above check is made mod 2^32, for files larger than 512 MB
     * and uLong implemented on 32 bits.
     */
    init_block(s);
  
    if (last) {
      bi_windup(s);
    }
    // Tracev((stderr,"\ncomprlen %lu(%lu) ", s->compressed_len>>3,
    //       s->compressed_len-7*last));
  }
  
  /* ===========================================================================
   * Save the match info and tally the frequency counts. Return true if
   * the current block must be flushed.
   */
  function _tr_tally(s, dist, lc)
  //    deflate_state *s;
  //    unsigned dist;  /* distance of matched string */
  //    unsigned lc;    /* match length-MIN_MATCH or unmatched char (if dist==0) */
  {
    //var out_length, in_length, dcode;
  
    s.pending_buf[s.d_buf + s.last_lit * 2]     = (dist >>> 8) & 0xff;
    s.pending_buf[s.d_buf + s.last_lit * 2 + 1] = dist & 0xff;
  
    s.pending_buf[s.l_buf + s.last_lit] = lc & 0xff;
    s.last_lit++;
  
    if (dist === 0) {
      /* lc is the unmatched char */
      s.dyn_ltree[lc * 2]/*.Freq*/++;
    } else {
      s.matches++;
      /* Here, lc is the match length - MIN_MATCH */
      dist--;             /* dist = match distance - 1 */
      //Assert((ush)dist < (ush)MAX_DIST(s) &&
      //       (ush)lc <= (ush)(MAX_MATCH-MIN_MATCH) &&
      //       (ush)d_code(dist) < (ush)D_CODES,  "_tr_tally: bad match");
  
      s.dyn_ltree[(_length_code[lc] + LITERALS + 1) * 2]/*.Freq*/++;
      s.dyn_dtree[d_code(dist) * 2]/*.Freq*/++;
    }
  
  // (!) This block is disabled in zlib defaults,
  // don't enable it for binary compatibility
  
  //#ifdef TRUNCATE_BLOCK
  //  /* Try to guess if it is profitable to stop the current block here */
  //  if ((s.last_lit & 0x1fff) === 0 && s.level > 2) {
  //    /* Compute an upper bound for the compressed length */
  //    out_length = s.last_lit*8;
  //    in_length = s.strstart - s.block_start;
  //
  //    for (dcode = 0; dcode < D_CODES; dcode++) {
  //      out_length += s.dyn_dtree[dcode*2]/*.Freq*/ * (5 + extra_dbits[dcode]);
  //    }
  //    out_length >>>= 3;
  //    //Tracev((stderr,"\nlast_lit %u, in %ld, out ~%ld(%ld%%) ",
  //    //       s->last_lit, in_length, out_length,
  //    //       100L - out_length*100L/in_length));
  //    if (s.matches < (s.last_lit>>1)/*int /2*/ && out_length < (in_length>>1)/*int /2*/) {
  //      return true;
  //    }
  //  }
  //#endif
  
    return (s.last_lit === s.lit_bufsize - 1);
    /* We avoid equality with lit_bufsize because of wraparound at 64K
     * on 16 bit machines and because stored blocks are restricted to
     * 64K-1 bytes.
     */
  }
  
  exports._tr_init  = _tr_init;
  exports._tr_stored_block = _tr_stored_block;
  exports._tr_flush_block  = _tr_flush_block;
  exports._tr_tally = _tr_tally;
  exports._tr_align = _tr_align;
  
  },{"../utils/common":20}],30:[function(require,module,exports){
  'use strict';
  
  // (C) 1995-2013 Jean-loup Gailly and Mark Adler
  // (C) 2014-2017 Vitaly Puzrin and Andrey Tupitsin
  //
  // This software is provided 'as-is', without any express or implied
  // warranty. In no event will the authors be held liable for any damages
  // arising from the use of this software.
  //
  // Permission is granted to anyone to use this software for any purpose,
  // including commercial applications, and to alter it and redistribute it
  // freely, subject to the following restrictions:
  //
  // 1. The origin of this software must not be misrepresented; you must not
  //   claim that you wrote the original software. If you use this software
  //   in a product, an acknowledgment in the product documentation would be
  //   appreciated but is not required.
  // 2. Altered source versions must be plainly marked as such, and must not be
  //   misrepresented as being the original software.
  // 3. This notice may not be removed or altered from any source distribution.
  
  function ZStream() {
    /* next input byte */
    this.input = null; // JS specific, because we have no pointers
    this.next_in = 0;
    /* number of bytes available at input */
    this.avail_in = 0;
    /* total number of input bytes read so far */
    this.total_in = 0;
    /* next output byte should be put there */
    this.output = null; // JS specific, because we have no pointers
    this.next_out = 0;
    /* remaining free space at output */
    this.avail_out = 0;
    /* total number of bytes output so far */
    this.total_out = 0;
    /* last error message, NULL if no error */
    this.msg = ''/*Z_NULL*/;
    /* not visible by applications */
    this.state = null;
    /* best guess about the data type: binary or text */
    this.data_type = 2/*Z_UNKNOWN*/;
    /* adler32 value of the uncompressed data */
    this.adler = 0;
  }
  
  module.exports = ZStream;
  
  },{}],31:[function(require,module,exports){
  (function (process){
  'use strict';
  
  if (typeof process === 'undefined' ||
      !process.version ||
      process.version.indexOf('v0.') === 0 ||
      process.version.indexOf('v1.') === 0 && process.version.indexOf('v1.8.') !== 0) {
    module.exports = { nextTick: nextTick };
  } else {
    module.exports = process
  }
  
  function nextTick(fn, arg1, arg2, arg3) {
    if (typeof fn !== 'function') {
      throw new TypeError('"callback" argument must be a function');
    }
    var len = arguments.length;
    var args, i;
    switch (len) {
    case 0:
    case 1:
      return process.nextTick(fn);
    case 2:
      return process.nextTick(function afterTickOne() {
        fn.call(null, arg1);
      });
    case 3:
      return process.nextTick(function afterTickTwo() {
        fn.call(null, arg1, arg2);
      });
    case 4:
      return process.nextTick(function afterTickThree() {
        fn.call(null, arg1, arg2, arg3);
      });
    default:
      args = new Array(len - 1);
      i = 0;
      while (i < args.length) {
        args[i++] = arguments[i];
      }
      return process.nextTick(function afterTick() {
        fn.apply(null, args);
      });
    }
  }
  
  
  }).call(this,require('_process'))
  },{"_process":32}],32:[function(require,module,exports){
  // shim for using process in browser
  var process = module.exports = {};
  
  // cached from whatever global is present so that test runners that stub it
  // don't break things.  But we need to wrap it in a try catch in case it is
  // wrapped in strict mode code which doesn't define any globals.  It's inside a
  // function because try/catches deoptimize in certain engines.
  
  var cachedSetTimeout;
  var cachedClearTimeout;
  
  function defaultSetTimout() {
      throw new Error('setTimeout has not been defined');
  }
  function defaultClearTimeout () {
      throw new Error('clearTimeout has not been defined');
  }
  (function () {
      try {
          if (typeof setTimeout === 'function') {
              cachedSetTimeout = setTimeout;
          } else {
              cachedSetTimeout = defaultSetTimout;
          }
      } catch (e) {
          cachedSetTimeout = defaultSetTimout;
      }
      try {
          if (typeof clearTimeout === 'function') {
              cachedClearTimeout = clearTimeout;
          } else {
              cachedClearTimeout = defaultClearTimeout;
          }
      } catch (e) {
          cachedClearTimeout = defaultClearTimeout;
      }
  } ())
  function runTimeout(fun) {
      if (cachedSetTimeout === setTimeout) {
          //normal enviroments in sane situations
          return setTimeout(fun, 0);
      }
      // if setTimeout wasn't available but was latter defined
      if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
          cachedSetTimeout = setTimeout;
          return setTimeout(fun, 0);
      }
      try {
          // when when somebody has screwed with setTimeout but no I.E. maddness
          return cachedSetTimeout(fun, 0);
      } catch(e){
          try {
              // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
              return cachedSetTimeout.call(null, fun, 0);
          } catch(e){
              // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
              return cachedSetTimeout.call(this, fun, 0);
          }
      }
  
  
  }
  function runClearTimeout(marker) {
      if (cachedClearTimeout === clearTimeout) {
          //normal enviroments in sane situations
          return clearTimeout(marker);
      }
      // if clearTimeout wasn't available but was latter defined
      if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
          cachedClearTimeout = clearTimeout;
          return clearTimeout(marker);
      }
      try {
          // when when somebody has screwed with setTimeout but no I.E. maddness
          return cachedClearTimeout(marker);
      } catch (e){
          try {
              // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
              return cachedClearTimeout.call(null, marker);
          } catch (e){
              // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
              // Some versions of I.E. have different rules for clearTimeout vs setTimeout
              return cachedClearTimeout.call(this, marker);
          }
      }
  
  
  
  }
  var queue = [];
  var draining = false;
  var currentQueue;
  var queueIndex = -1;
  
  function cleanUpNextTick() {
      if (!draining || !currentQueue) {
          return;
      }
      draining = false;
      if (currentQueue.length) {
          queue = currentQueue.concat(queue);
      } else {
          queueIndex = -1;
      }
      if (queue.length) {
          drainQueue();
      }
  }
  
  function drainQueue() {
      if (draining) {
          return;
      }
      var timeout = runTimeout(cleanUpNextTick);
      draining = true;
  
      var len = queue.length;
      while(len) {
          currentQueue = queue;
          queue = [];
          while (++queueIndex < len) {
              if (currentQueue) {
                  currentQueue[queueIndex].run();
              }
          }
          queueIndex = -1;
          len = queue.length;
      }
      currentQueue = null;
      draining = false;
      runClearTimeout(timeout);
  }
  
  process.nextTick = function (fun) {
      var args = new Array(arguments.length - 1);
      if (arguments.length > 1) {
          for (var i = 1; i < arguments.length; i++) {
              args[i - 1] = arguments[i];
          }
      }
      queue.push(new Item(fun, args));
      if (queue.length === 1 && !draining) {
          runTimeout(drainQueue);
      }
  };
  
  // v8 likes predictible objects
  function Item(fun, array) {
      this.fun = fun;
      this.array = array;
  }
  Item.prototype.run = function () {
      this.fun.apply(null, this.array);
  };
  process.title = 'browser';
  process.browser = true;
  process.env = {};
  process.argv = [];
  process.version = ''; // empty string to avoid regexp issues
  process.versions = {};
  
  function noop() {}
  
  process.on = noop;
  process.addListener = noop;
  process.once = noop;
  process.off = noop;
  process.removeListener = noop;
  process.removeAllListeners = noop;
  process.emit = noop;
  process.prependListener = noop;
  process.prependOnceListener = noop;
  
  process.listeners = function (name) { return [] }
  
  process.binding = function (name) {
      throw new Error('process.binding is not supported');
  };
  
  process.cwd = function () { return '/' };
  process.chdir = function (dir) {
      throw new Error('process.chdir is not supported');
  };
  process.umask = function() { return 0; };
  
  },{}],33:[function(require,module,exports){
  (function (global){
  /*! https://mths.be/punycode v1.4.1 by @mathias */
  ;(function(root) {
  
    /** Detect free variables */
    var freeExports = typeof exports == 'object' && exports &&
      !exports.nodeType && exports;
    var freeModule = typeof module == 'object' && module &&
      !module.nodeType && module;
    var freeGlobal = typeof global == 'object' && global;
    if (
      freeGlobal.global === freeGlobal ||
      freeGlobal.window === freeGlobal ||
      freeGlobal.self === freeGlobal
    ) {
      root = freeGlobal;
    }
  
    /**
     * The `punycode` object.
     * @name punycode
     * @type Object
     */
    var punycode,
  
    /** Highest positive signed 32-bit float value */
    maxInt = 2147483647, // aka. 0x7FFFFFFF or 2^31-1
  
    /** Bootstring parameters */
    base = 36,
    tMin = 1,
    tMax = 26,
    skew = 38,
    damp = 700,
    initialBias = 72,
    initialN = 128, // 0x80
    delimiter = '-', // '\x2D'
  
    /** Regular expressions */
    regexPunycode = /^xn--/,
    regexNonASCII = /[^\x20-\x7E]/, // unprintable ASCII chars + non-ASCII chars
    regexSeparators = /[\x2E\u3002\uFF0E\uFF61]/g, // RFC 3490 separators
  
    /** Error messages */
    errors = {
      'overflow': 'Overflow: input needs wider integers to process',
      'not-basic': 'Illegal input >= 0x80 (not a basic code point)',
      'invalid-input': 'Invalid input'
    },
  
    /** Convenience shortcuts */
    baseMinusTMin = base - tMin,
    floor = Math.floor,
    stringFromCharCode = String.fromCharCode,
  
    /** Temporary variable */
    key;
  
    /*--------------------------------------------------------------------------*/
  
    /**
     * A generic error utility function.
     * @private
     * @param {String} type The error type.
     * @returns {Error} Throws a `RangeError` with the applicable error message.
     */
    function error(type) {
      throw new RangeError(errors[type]);
    }
  
    /**
     * A generic `Array#map` utility function.
     * @private
     * @param {Array} array The array to iterate over.
     * @param {Function} callback The function that gets called for every array
     * item.
     * @returns {Array} A new array of values returned by the callback function.
     */
    function map(array, fn) {
      var length = array.length;
      var result = [];
      while (length--) {
        result[length] = fn(array[length]);
      }
      return result;
    }
  
    /**
     * A simple `Array#map`-like wrapper to work with domain name strings or email
     * addresses.
     * @private
     * @param {String} domain The domain name or email address.
     * @param {Function} callback The function that gets called for every
     * character.
     * @returns {Array} A new string of characters returned by the callback
     * function.
     */
    function mapDomain(string, fn) {
      var parts = string.split('@');
      var result = '';
      if (parts.length > 1) {
        // In email addresses, only the domain name should be punycoded. Leave
        // the local part (i.e. everything up to `@`) intact.
        result = parts[0] + '@';
        string = parts[1];
      }
      // Avoid `split(regex)` for IE8 compatibility. See #17.
      string = string.replace(regexSeparators, '\x2E');
      var labels = string.split('.');
      var encoded = map(labels, fn).join('.');
      return result + encoded;
    }
  
    /**
     * Creates an array containing the numeric code points of each Unicode
     * character in the string. While JavaScript uses UCS-2 internally,
     * this function will convert a pair of surrogate halves (each of which
     * UCS-2 exposes as separate characters) into a single code point,
     * matching UTF-16.
     * @see `punycode.ucs2.encode`
     * @see <https://mathiasbynens.be/notes/javascript-encoding>
     * @memberOf punycode.ucs2
     * @name decode
     * @param {String} string The Unicode input string (UCS-2).
     * @returns {Array} The new array of code points.
     */
    function ucs2decode(string) {
      var output = [],
          counter = 0,
          length = string.length,
          value,
          extra;
      while (counter < length) {
        value = string.charCodeAt(counter++);
        if (value >= 0xD800 && value <= 0xDBFF && counter < length) {
          // high surrogate, and there is a next character
          extra = string.charCodeAt(counter++);
          if ((extra & 0xFC00) == 0xDC00) { // low surrogate
            output.push(((value & 0x3FF) << 10) + (extra & 0x3FF) + 0x10000);
          } else {
            // unmatched surrogate; only append this code unit, in case the next
            // code unit is the high surrogate of a surrogate pair
            output.push(value);
            counter--;
          }
        } else {
          output.push(value);
        }
      }
      return output;
    }
  
    /**
     * Creates a string based on an array of numeric code points.
     * @see `punycode.ucs2.decode`
     * @memberOf punycode.ucs2
     * @name encode
     * @param {Array} codePoints The array of numeric code points.
     * @returns {String} The new Unicode string (UCS-2).
     */
    function ucs2encode(array) {
      return map(array, function(value) {
        var output = '';
        if (value > 0xFFFF) {
          value -= 0x10000;
          output += stringFromCharCode(value >>> 10 & 0x3FF | 0xD800);
          value = 0xDC00 | value & 0x3FF;
        }
        output += stringFromCharCode(value);
        return output;
      }).join('');
    }
  
    /**
     * Converts a basic code point into a digit/integer.
     * @see `digitToBasic()`
     * @private
     * @param {Number} codePoint The basic numeric code point value.
     * @returns {Number} The numeric value of a basic code point (for use in
     * representing integers) in the range `0` to `base - 1`, or `base` if
     * the code point does not represent a value.
     */
    function basicToDigit(codePoint) {
      if (codePoint - 48 < 10) {
        return codePoint - 22;
      }
      if (codePoint - 65 < 26) {
        return codePoint - 65;
      }
      if (codePoint - 97 < 26) {
        return codePoint - 97;
      }
      return base;
    }
  
    /**
     * Converts a digit/integer into a basic code point.
     * @see `basicToDigit()`
     * @private
     * @param {Number} digit The numeric value of a basic code point.
     * @returns {Number} The basic code point whose value (when used for
     * representing integers) is `digit`, which needs to be in the range
     * `0` to `base - 1`. If `flag` is non-zero, the uppercase form is
     * used; else, the lowercase form is used. The behavior is undefined
     * if `flag` is non-zero and `digit` has no uppercase form.
     */
    function digitToBasic(digit, flag) {
      //  0..25 map to ASCII a..z or A..Z
      // 26..35 map to ASCII 0..9
      return digit + 22 + 75 * (digit < 26) - ((flag != 0) << 5);
    }
  
    /**
     * Bias adaptation function as per section 3.4 of RFC 3492.
     * https://tools.ietf.org/html/rfc3492#section-3.4
     * @private
     */
    function adapt(delta, numPoints, firstTime) {
      var k = 0;
      delta = firstTime ? floor(delta / damp) : delta >> 1;
      delta += floor(delta / numPoints);
      for (/* no initialization */; delta > baseMinusTMin * tMax >> 1; k += base) {
        delta = floor(delta / baseMinusTMin);
      }
      return floor(k + (baseMinusTMin + 1) * delta / (delta + skew));
    }
  
    /**
     * Converts a Punycode string of ASCII-only symbols to a string of Unicode
     * symbols.
     * @memberOf punycode
     * @param {String} input The Punycode string of ASCII-only symbols.
     * @returns {String} The resulting string of Unicode symbols.
     */
    function decode(input) {
      // Don't use UCS-2
      var output = [],
          inputLength = input.length,
          out,
          i = 0,
          n = initialN,
          bias = initialBias,
          basic,
          j,
          index,
          oldi,
          w,
          k,
          digit,
          t,
          /** Cached calculation results */
          baseMinusT;
  
      // Handle the basic code points: let `basic` be the number of input code
      // points before the last delimiter, or `0` if there is none, then copy
      // the first basic code points to the output.
  
      basic = input.lastIndexOf(delimiter);
      if (basic < 0) {
        basic = 0;
      }
  
      for (j = 0; j < basic; ++j) {
        // if it's not a basic code point
        if (input.charCodeAt(j) >= 0x80) {
          error('not-basic');
        }
        output.push(input.charCodeAt(j));
      }
  
      // Main decoding loop: start just after the last delimiter if any basic code
      // points were copied; start at the beginning otherwise.
  
      for (index = basic > 0 ? basic + 1 : 0; index < inputLength; /* no final expression */) {
  
        // `index` is the index of the next character to be consumed.
        // Decode a generalized variable-length integer into `delta`,
        // which gets added to `i`. The overflow checking is easier
        // if we increase `i` as we go, then subtract off its starting
        // value at the end to obtain `delta`.
        for (oldi = i, w = 1, k = base; /* no condition */; k += base) {
  
          if (index >= inputLength) {
            error('invalid-input');
          }
  
          digit = basicToDigit(input.charCodeAt(index++));
  
          if (digit >= base || digit > floor((maxInt - i) / w)) {
            error('overflow');
          }
  
          i += digit * w;
          t = k <= bias ? tMin : (k >= bias + tMax ? tMax : k - bias);
  
          if (digit < t) {
            break;
          }
  
          baseMinusT = base - t;
          if (w > floor(maxInt / baseMinusT)) {
            error('overflow');
          }
  
          w *= baseMinusT;
  
        }
  
        out = output.length + 1;
        bias = adapt(i - oldi, out, oldi == 0);
  
        // `i` was supposed to wrap around from `out` to `0`,
        // incrementing `n` each time, so we'll fix that now:
        if (floor(i / out) > maxInt - n) {
          error('overflow');
        }
  
        n += floor(i / out);
        i %= out;
  
        // Insert `n` at position `i` of the output
        output.splice(i++, 0, n);
  
      }
  
      return ucs2encode(output);
    }
  
    /**
     * Converts a string of Unicode symbols (e.g. a domain name label) to a
     * Punycode string of ASCII-only symbols.
     * @memberOf punycode
     * @param {String} input The string of Unicode symbols.
     * @returns {String} The resulting Punycode string of ASCII-only symbols.
     */
    function encode(input) {
      var n,
          delta,
          handledCPCount,
          basicLength,
          bias,
          j,
          m,
          q,
          k,
          t,
          currentValue,
          output = [],
          /** `inputLength` will hold the number of code points in `input`. */
          inputLength,
          /** Cached calculation results */
          handledCPCountPlusOne,
          baseMinusT,
          qMinusT;
  
      // Convert the input in UCS-2 to Unicode
      input = ucs2decode(input);
  
      // Cache the length
      inputLength = input.length;
  
      // Initialize the state
      n = initialN;
      delta = 0;
      bias = initialBias;
  
      // Handle the basic code points
      for (j = 0; j < inputLength; ++j) {
        currentValue = input[j];
        if (currentValue < 0x80) {
          output.push(stringFromCharCode(currentValue));
        }
      }
  
      handledCPCount = basicLength = output.length;
  
      // `handledCPCount` is the number of code points that have been handled;
      // `basicLength` is the number of basic code points.
  
      // Finish the basic string - if it is not empty - with a delimiter
      if (basicLength) {
        output.push(delimiter);
      }
  
      // Main encoding loop:
      while (handledCPCount < inputLength) {
  
        // All non-basic code points < n have been handled already. Find the next
        // larger one:
        for (m = maxInt, j = 0; j < inputLength; ++j) {
          currentValue = input[j];
          if (currentValue >= n && currentValue < m) {
            m = currentValue;
          }
        }
  
        // Increase `delta` enough to advance the decoder's <n,i> state to <m,0>,
        // but guard against overflow
        handledCPCountPlusOne = handledCPCount + 1;
        if (m - n > floor((maxInt - delta) / handledCPCountPlusOne)) {
          error('overflow');
        }
  
        delta += (m - n) * handledCPCountPlusOne;
        n = m;
  
        for (j = 0; j < inputLength; ++j) {
          currentValue = input[j];
  
          if (currentValue < n && ++delta > maxInt) {
            error('overflow');
          }
  
          if (currentValue == n) {
            // Represent delta as a generalized variable-length integer
            for (q = delta, k = base; /* no condition */; k += base) {
              t = k <= bias ? tMin : (k >= bias + tMax ? tMax : k - bias);
              if (q < t) {
                break;
              }
              qMinusT = q - t;
              baseMinusT = base - t;
              output.push(
                stringFromCharCode(digitToBasic(t + qMinusT % baseMinusT, 0))
              );
              q = floor(qMinusT / baseMinusT);
            }
  
            output.push(stringFromCharCode(digitToBasic(q, 0)));
            bias = adapt(delta, handledCPCountPlusOne, handledCPCount == basicLength);
            delta = 0;
            ++handledCPCount;
          }
        }
  
        ++delta;
        ++n;
  
      }
      return output.join('');
    }
  
    /**
     * Converts a Punycode string representing a domain name or an email address
     * to Unicode. Only the Punycoded parts of the input will be converted, i.e.
     * it doesn't matter if you call it on a string that has already been
     * converted to Unicode.
     * @memberOf punycode
     * @param {String} input The Punycoded domain name or email address to
     * convert to Unicode.
     * @returns {String} The Unicode representation of the given Punycode
     * string.
     */
    function toUnicode(input) {
      return mapDomain(input, function(string) {
        return regexPunycode.test(string)
          ? decode(string.slice(4).toLowerCase())
          : string;
      });
    }
  
    /**
     * Converts a Unicode string representing a domain name or an email address to
     * Punycode. Only the non-ASCII parts of the domain name will be converted,
     * i.e. it doesn't matter if you call it with a domain that's already in
     * ASCII.
     * @memberOf punycode
     * @param {String} input The domain name or email address to convert, as a
     * Unicode string.
     * @returns {String} The Punycode representation of the given domain name or
     * email address.
     */
    function toASCII(input) {
      return mapDomain(input, function(string) {
        return regexNonASCII.test(string)
          ? 'xn--' + encode(string)
          : string;
      });
    }
  
    /*--------------------------------------------------------------------------*/
  
    /** Define the public API */
    punycode = {
      /**
       * A string representing the current Punycode.js version number.
       * @memberOf punycode
       * @type String
       */
      'version': '1.4.1',
      /**
       * An object of methods to convert from JavaScript's internal character
       * representation (UCS-2) to Unicode code points, and back.
       * @see <https://mathiasbynens.be/notes/javascript-encoding>
       * @memberOf punycode
       * @type Object
       */
      'ucs2': {
        'decode': ucs2decode,
        'encode': ucs2encode
      },
      'decode': decode,
      'encode': encode,
      'toASCII': toASCII,
      'toUnicode': toUnicode
    };
  
    /** Expose `punycode` */
    // Some AMD build optimizers, like r.js, check for specific condition patterns
    // like the following:
    if (
      typeof define == 'function' &&
      typeof define.amd == 'object' &&
      define.amd
    ) {
      define('punycode', function() {
        return punycode;
      });
    } else if (freeExports && freeModule) {
      if (module.exports == freeExports) {
        // in Node.js, io.js, or RingoJS v0.8.0+
        freeModule.exports = punycode;
      } else {
        // in Narwhal or RingoJS v0.7.0-
        for (key in punycode) {
          punycode.hasOwnProperty(key) && (freeExports[key] = punycode[key]);
        }
      }
    } else {
      // in Rhino or a web browser
      root.punycode = punycode;
    }
  
  }(this));
  
  }).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{}],34:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  'use strict';
  
  // If obj.hasOwnProperty has been overridden, then calling
  // obj.hasOwnProperty(prop) will break.
  // See: https://github.com/joyent/node/issues/1707
  function hasOwnProperty(obj, prop) {
    return Object.prototype.hasOwnProperty.call(obj, prop);
  }
  
  module.exports = function(qs, sep, eq, options) {
    sep = sep || '&';
    eq = eq || '=';
    var obj = {};
  
    if (typeof qs !== 'string' || qs.length === 0) {
      return obj;
    }
  
    var regexp = /\+/g;
    qs = qs.split(sep);
  
    var maxKeys = 1000;
    if (options && typeof options.maxKeys === 'number') {
      maxKeys = options.maxKeys;
    }
  
    var len = qs.length;
    // maxKeys <= 0 means that we should not limit keys count
    if (maxKeys > 0 && len > maxKeys) {
      len = maxKeys;
    }
  
    for (var i = 0; i < len; ++i) {
      var x = qs[i].replace(regexp, '%20'),
          idx = x.indexOf(eq),
          kstr, vstr, k, v;
  
      if (idx >= 0) {
        kstr = x.substr(0, idx);
        vstr = x.substr(idx + 1);
      } else {
        kstr = x;
        vstr = '';
      }
  
      k = decodeURIComponent(kstr);
      v = decodeURIComponent(vstr);
  
      if (!hasOwnProperty(obj, k)) {
        obj[k] = v;
      } else if (isArray(obj[k])) {
        obj[k].push(v);
      } else {
        obj[k] = [obj[k], v];
      }
    }
  
    return obj;
  };
  
  var isArray = Array.isArray || function (xs) {
    return Object.prototype.toString.call(xs) === '[object Array]';
  };
  
  },{}],35:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  'use strict';
  
  var stringifyPrimitive = function(v) {
    switch (typeof v) {
      case 'string':
        return v;
  
      case 'boolean':
        return v ? 'true' : 'false';
  
      case 'number':
        return isFinite(v) ? v : '';
  
      default:
        return '';
    }
  };
  
  module.exports = function(obj, sep, eq, name) {
    sep = sep || '&';
    eq = eq || '=';
    if (obj === null) {
      obj = undefined;
    }
  
    if (typeof obj === 'object') {
      return map(objectKeys(obj), function(k) {
        var ks = encodeURIComponent(stringifyPrimitive(k)) + eq;
        if (isArray(obj[k])) {
          return map(obj[k], function(v) {
            return ks + encodeURIComponent(stringifyPrimitive(v));
          }).join(sep);
        } else {
          return ks + encodeURIComponent(stringifyPrimitive(obj[k]));
        }
      }).join(sep);
  
    }
  
    if (!name) return '';
    return encodeURIComponent(stringifyPrimitive(name)) + eq +
           encodeURIComponent(stringifyPrimitive(obj));
  };
  
  var isArray = Array.isArray || function (xs) {
    return Object.prototype.toString.call(xs) === '[object Array]';
  };
  
  function map (xs, f) {
    if (xs.map) return xs.map(f);
    var res = [];
    for (var i = 0; i < xs.length; i++) {
      res.push(f(xs[i], i));
    }
    return res;
  }
  
  var objectKeys = Object.keys || function (obj) {
    var res = [];
    for (var key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) res.push(key);
    }
    return res;
  };
  
  },{}],36:[function(require,module,exports){
  'use strict';
  
  exports.decode = exports.parse = require('./decode');
  exports.encode = exports.stringify = require('./encode');
  
  },{"./decode":34,"./encode":35}],37:[function(require,module,exports){
  module.exports = require('./lib/_stream_duplex.js');
  
  },{"./lib/_stream_duplex.js":38}],38:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  // a duplex stream is just a stream that is both readable and writable.
  // Since JS doesn't have multiple prototypal inheritance, this class
  // prototypally inherits from Readable, and then parasitically from
  // Writable.
  
  'use strict';
  
  /*<replacement>*/
  
  var pna = require('process-nextick-args');
  /*</replacement>*/
  
  /*<replacement>*/
  var objectKeys = Object.keys || function (obj) {
    var keys = [];
    for (var key in obj) {
      keys.push(key);
    }return keys;
  };
  /*</replacement>*/
  
  module.exports = Duplex;
  
  /*<replacement>*/
  var util = Object.create(require('core-util-is'));
  util.inherits = require('inherits');
  /*</replacement>*/
  
  var Readable = require('./_stream_readable');
  var Writable = require('./_stream_writable');
  
  util.inherits(Duplex, Readable);
  
  {
    // avoid scope creep, the keys array can then be collected
    var keys = objectKeys(Writable.prototype);
    for (var v = 0; v < keys.length; v++) {
      var method = keys[v];
      if (!Duplex.prototype[method]) Duplex.prototype[method] = Writable.prototype[method];
    }
  }
  
  function Duplex(options) {
    if (!(this instanceof Duplex)) return new Duplex(options);
  
    Readable.call(this, options);
    Writable.call(this, options);
  
    if (options && options.readable === false) this.readable = false;
  
    if (options && options.writable === false) this.writable = false;
  
    this.allowHalfOpen = true;
    if (options && options.allowHalfOpen === false) this.allowHalfOpen = false;
  
    this.once('end', onend);
  }
  
  Object.defineProperty(Duplex.prototype, 'writableHighWaterMark', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function () {
      return this._writableState.highWaterMark;
    }
  });
  
  // the no-half-open enforcer
  function onend() {
    // if we allow half-open state, or if the writable side ended,
    // then we're ok.
    if (this.allowHalfOpen || this._writableState.ended) return;
  
    // no more data can be written.
    // But allow more writes to happen in this tick.
    pna.nextTick(onEndNT, this);
  }
  
  function onEndNT(self) {
    self.end();
  }
  
  Object.defineProperty(Duplex.prototype, 'destroyed', {
    get: function () {
      if (this._readableState === undefined || this._writableState === undefined) {
        return false;
      }
      return this._readableState.destroyed && this._writableState.destroyed;
    },
    set: function (value) {
      // we ignore the value if the stream
      // has not been initialized yet
      if (this._readableState === undefined || this._writableState === undefined) {
        return;
      }
  
      // backward compatibility, the user is explicitly
      // managing destroyed
      this._readableState.destroyed = value;
      this._writableState.destroyed = value;
    }
  });
  
  Duplex.prototype._destroy = function (err, cb) {
    this.push(null);
    this.end();
  
    pna.nextTick(cb, err);
  };
  },{"./_stream_readable":40,"./_stream_writable":42,"core-util-is":11,"inherits":15,"process-nextick-args":31}],39:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  // a passthrough stream.
  // basically just the most minimal sort of Transform stream.
  // Every written chunk gets output as-is.
  
  'use strict';
  
  module.exports = PassThrough;
  
  var Transform = require('./_stream_transform');
  
  /*<replacement>*/
  var util = Object.create(require('core-util-is'));
  util.inherits = require('inherits');
  /*</replacement>*/
  
  util.inherits(PassThrough, Transform);
  
  function PassThrough(options) {
    if (!(this instanceof PassThrough)) return new PassThrough(options);
  
    Transform.call(this, options);
  }
  
  PassThrough.prototype._transform = function (chunk, encoding, cb) {
    cb(null, chunk);
  };
  },{"./_stream_transform":41,"core-util-is":11,"inherits":15}],40:[function(require,module,exports){
  (function (process,global){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  'use strict';
  
  /*<replacement>*/
  
  var pna = require('process-nextick-args');
  /*</replacement>*/
  
  module.exports = Readable;
  
  /*<replacement>*/
  var isArray = require('isarray');
  /*</replacement>*/
  
  /*<replacement>*/
  var Duplex;
  /*</replacement>*/
  
  Readable.ReadableState = ReadableState;
  
  /*<replacement>*/
  var EE = require('events').EventEmitter;
  
  var EElistenerCount = function (emitter, type) {
    return emitter.listeners(type).length;
  };
  /*</replacement>*/
  
  /*<replacement>*/
  var Stream = require('./internal/streams/stream');
  /*</replacement>*/
  
  /*<replacement>*/
  
  var Buffer = require('safe-buffer').Buffer;
  var OurUint8Array = global.Uint8Array || function () {};
  function _uint8ArrayToBuffer(chunk) {
    return Buffer.from(chunk);
  }
  function _isUint8Array(obj) {
    return Buffer.isBuffer(obj) || obj instanceof OurUint8Array;
  }
  
  /*</replacement>*/
  
  /*<replacement>*/
  var util = Object.create(require('core-util-is'));
  util.inherits = require('inherits');
  /*</replacement>*/
  
  /*<replacement>*/
  var debugUtil = require('util');
  var debug = void 0;
  if (debugUtil && debugUtil.debuglog) {
    debug = debugUtil.debuglog('stream');
  } else {
    debug = function () {};
  }
  /*</replacement>*/
  
  var BufferList = require('./internal/streams/BufferList');
  var destroyImpl = require('./internal/streams/destroy');
  var StringDecoder;
  
  util.inherits(Readable, Stream);
  
  var kProxyEvents = ['error', 'close', 'destroy', 'pause', 'resume'];
  
  function prependListener(emitter, event, fn) {
    // Sadly this is not cacheable as some libraries bundle their own
    // event emitter implementation with them.
    if (typeof emitter.prependListener === 'function') return emitter.prependListener(event, fn);
  
    // This is a hack to make sure that our error handler is attached before any
    // userland ones.  NEVER DO THIS. This is here only because this code needs
    // to continue to work with older versions of Node.js that do not include
    // the prependListener() method. The goal is to eventually remove this hack.
    if (!emitter._events || !emitter._events[event]) emitter.on(event, fn);else if (isArray(emitter._events[event])) emitter._events[event].unshift(fn);else emitter._events[event] = [fn, emitter._events[event]];
  }
  
  function ReadableState(options, stream) {
    Duplex = Duplex || require('./_stream_duplex');
  
    options = options || {};
  
    // Duplex streams are both readable and writable, but share
    // the same options object.
    // However, some cases require setting options to different
    // values for the readable and the writable sides of the duplex stream.
    // These options can be provided separately as readableXXX and writableXXX.
    var isDuplex = stream instanceof Duplex;
  
    // object stream flag. Used to make read(n) ignore n and to
    // make all the buffer merging and length checks go away
    this.objectMode = !!options.objectMode;
  
    if (isDuplex) this.objectMode = this.objectMode || !!options.readableObjectMode;
  
    // the point at which it stops calling _read() to fill the buffer
    // Note: 0 is a valid value, means "don't call _read preemptively ever"
    var hwm = options.highWaterMark;
    var readableHwm = options.readableHighWaterMark;
    var defaultHwm = this.objectMode ? 16 : 16 * 1024;
  
    if (hwm || hwm === 0) this.highWaterMark = hwm;else if (isDuplex && (readableHwm || readableHwm === 0)) this.highWaterMark = readableHwm;else this.highWaterMark = defaultHwm;
  
    // cast to ints.
    this.highWaterMark = Math.floor(this.highWaterMark);
  
    // A linked list is used to store data chunks instead of an array because the
    // linked list can remove elements from the beginning faster than
    // array.shift()
    this.buffer = new BufferList();
    this.length = 0;
    this.pipes = null;
    this.pipesCount = 0;
    this.flowing = null;
    this.ended = false;
    this.endEmitted = false;
    this.reading = false;
  
    // a flag to be able to tell if the event 'readable'/'data' is emitted
    // immediately, or on a later tick.  We set this to true at first, because
    // any actions that shouldn't happen until "later" should generally also
    // not happen before the first read call.
    this.sync = true;
  
    // whenever we return null, then we set a flag to say
    // that we're awaiting a 'readable' event emission.
    this.needReadable = false;
    this.emittedReadable = false;
    this.readableListening = false;
    this.resumeScheduled = false;
  
    // has it been destroyed
    this.destroyed = false;
  
    // Crypto is kind of old and crusty.  Historically, its default string
    // encoding is 'binary' so we have to make this configurable.
    // Everything else in the universe uses 'utf8', though.
    this.defaultEncoding = options.defaultEncoding || 'utf8';
  
    // the number of writers that are awaiting a drain event in .pipe()s
    this.awaitDrain = 0;
  
    // if true, a maybeReadMore has been scheduled
    this.readingMore = false;
  
    this.decoder = null;
    this.encoding = null;
    if (options.encoding) {
      if (!StringDecoder) StringDecoder = require('string_decoder/').StringDecoder;
      this.decoder = new StringDecoder(options.encoding);
      this.encoding = options.encoding;
    }
  }
  
  function Readable(options) {
    Duplex = Duplex || require('./_stream_duplex');
  
    if (!(this instanceof Readable)) return new Readable(options);
  
    this._readableState = new ReadableState(options, this);
  
    // legacy
    this.readable = true;
  
    if (options) {
      if (typeof options.read === 'function') this._read = options.read;
  
      if (typeof options.destroy === 'function') this._destroy = options.destroy;
    }
  
    Stream.call(this);
  }
  
  Object.defineProperty(Readable.prototype, 'destroyed', {
    get: function () {
      if (this._readableState === undefined) {
        return false;
      }
      return this._readableState.destroyed;
    },
    set: function (value) {
      // we ignore the value if the stream
      // has not been initialized yet
      if (!this._readableState) {
        return;
      }
  
      // backward compatibility, the user is explicitly
      // managing destroyed
      this._readableState.destroyed = value;
    }
  });
  
  Readable.prototype.destroy = destroyImpl.destroy;
  Readable.prototype._undestroy = destroyImpl.undestroy;
  Readable.prototype._destroy = function (err, cb) {
    this.push(null);
    cb(err);
  };
  
  // Manually shove something into the read() buffer.
  // This returns true if the highWaterMark has not been hit yet,
  // similar to how Writable.write() returns true if you should
  // write() some more.
  Readable.prototype.push = function (chunk, encoding) {
    var state = this._readableState;
    var skipChunkCheck;
  
    if (!state.objectMode) {
      if (typeof chunk === 'string') {
        encoding = encoding || state.defaultEncoding;
        if (encoding !== state.encoding) {
          chunk = Buffer.from(chunk, encoding);
          encoding = '';
        }
        skipChunkCheck = true;
      }
    } else {
      skipChunkCheck = true;
    }
  
    return readableAddChunk(this, chunk, encoding, false, skipChunkCheck);
  };
  
  // Unshift should *always* be something directly out of read()
  Readable.prototype.unshift = function (chunk) {
    return readableAddChunk(this, chunk, null, true, false);
  };
  
  function readableAddChunk(stream, chunk, encoding, addToFront, skipChunkCheck) {
    var state = stream._readableState;
    if (chunk === null) {
      state.reading = false;
      onEofChunk(stream, state);
    } else {
      var er;
      if (!skipChunkCheck) er = chunkInvalid(state, chunk);
      if (er) {
        stream.emit('error', er);
      } else if (state.objectMode || chunk && chunk.length > 0) {
        if (typeof chunk !== 'string' && !state.objectMode && Object.getPrototypeOf(chunk) !== Buffer.prototype) {
          chunk = _uint8ArrayToBuffer(chunk);
        }
  
        if (addToFront) {
          if (state.endEmitted) stream.emit('error', new Error('stream.unshift() after end event'));else addChunk(stream, state, chunk, true);
        } else if (state.ended) {
          stream.emit('error', new Error('stream.push() after EOF'));
        } else {
          state.reading = false;
          if (state.decoder && !encoding) {
            chunk = state.decoder.write(chunk);
            if (state.objectMode || chunk.length !== 0) addChunk(stream, state, chunk, false);else maybeReadMore(stream, state);
          } else {
            addChunk(stream, state, chunk, false);
          }
        }
      } else if (!addToFront) {
        state.reading = false;
      }
    }
  
    return needMoreData(state);
  }
  
  function addChunk(stream, state, chunk, addToFront) {
    if (state.flowing && state.length === 0 && !state.sync) {
      stream.emit('data', chunk);
      stream.read(0);
    } else {
      // update the buffer info.
      state.length += state.objectMode ? 1 : chunk.length;
      if (addToFront) state.buffer.unshift(chunk);else state.buffer.push(chunk);
  
      if (state.needReadable) emitReadable(stream);
    }
    maybeReadMore(stream, state);
  }
  
  function chunkInvalid(state, chunk) {
    var er;
    if (!_isUint8Array(chunk) && typeof chunk !== 'string' && chunk !== undefined && !state.objectMode) {
      er = new TypeError('Invalid non-string/buffer chunk');
    }
    return er;
  }
  
  // if it's past the high water mark, we can push in some more.
  // Also, if we have no data yet, we can stand some
  // more bytes.  This is to work around cases where hwm=0,
  // such as the repl.  Also, if the push() triggered a
  // readable event, and the user called read(largeNumber) such that
  // needReadable was set, then we ought to push more, so that another
  // 'readable' event will be triggered.
  function needMoreData(state) {
    return !state.ended && (state.needReadable || state.length < state.highWaterMark || state.length === 0);
  }
  
  Readable.prototype.isPaused = function () {
    return this._readableState.flowing === false;
  };
  
  // backwards compatibility.
  Readable.prototype.setEncoding = function (enc) {
    if (!StringDecoder) StringDecoder = require('string_decoder/').StringDecoder;
    this._readableState.decoder = new StringDecoder(enc);
    this._readableState.encoding = enc;
    return this;
  };
  
  // Don't raise the hwm > 8MB
  var MAX_HWM = 0x800000;
  function computeNewHighWaterMark(n) {
    if (n >= MAX_HWM) {
      n = MAX_HWM;
    } else {
      // Get the next highest power of 2 to prevent increasing hwm excessively in
      // tiny amounts
      n--;
      n |= n >>> 1;
      n |= n >>> 2;
      n |= n >>> 4;
      n |= n >>> 8;
      n |= n >>> 16;
      n++;
    }
    return n;
  }
  
  // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  function howMuchToRead(n, state) {
    if (n <= 0 || state.length === 0 && state.ended) return 0;
    if (state.objectMode) return 1;
    if (n !== n) {
      // Only flow one buffer at a time
      if (state.flowing && state.length) return state.buffer.head.data.length;else return state.length;
    }
    // If we're asking for more than the current hwm, then raise the hwm.
    if (n > state.highWaterMark) state.highWaterMark = computeNewHighWaterMark(n);
    if (n <= state.length) return n;
    // Don't have enough
    if (!state.ended) {
      state.needReadable = true;
      return 0;
    }
    return state.length;
  }
  
  // you can override either this method, or the async _read(n) below.
  Readable.prototype.read = function (n) {
    debug('read', n);
    n = parseInt(n, 10);
    var state = this._readableState;
    var nOrig = n;
  
    if (n !== 0) state.emittedReadable = false;
  
    // if we're doing read(0) to trigger a readable event, but we
    // already have a bunch of data in the buffer, then just trigger
    // the 'readable' event and move on.
    if (n === 0 && state.needReadable && (state.length >= state.highWaterMark || state.ended)) {
      debug('read: emitReadable', state.length, state.ended);
      if (state.length === 0 && state.ended) endReadable(this);else emitReadable(this);
      return null;
    }
  
    n = howMuchToRead(n, state);
  
    // if we've ended, and we're now clear, then finish it up.
    if (n === 0 && state.ended) {
      if (state.length === 0) endReadable(this);
      return null;
    }
  
    // All the actual chunk generation logic needs to be
    // *below* the call to _read.  The reason is that in certain
    // synthetic stream cases, such as passthrough streams, _read
    // may be a completely synchronous operation which may change
    // the state of the read buffer, providing enough data when
    // before there was *not* enough.
    //
    // So, the steps are:
    // 1. Figure out what the state of things will be after we do
    // a read from the buffer.
    //
    // 2. If that resulting state will trigger a _read, then call _read.
    // Note that this may be asynchronous, or synchronous.  Yes, it is
    // deeply ugly to write APIs this way, but that still doesn't mean
    // that the Readable class should behave improperly, as streams are
    // designed to be sync/async agnostic.
    // Take note if the _read call is sync or async (ie, if the read call
    // has returned yet), so that we know whether or not it's safe to emit
    // 'readable' etc.
    //
    // 3. Actually pull the requested chunks out of the buffer and return.
  
    // if we need a readable event, then we need to do some reading.
    var doRead = state.needReadable;
    debug('need readable', doRead);
  
    // if we currently have less than the highWaterMark, then also read some
    if (state.length === 0 || state.length - n < state.highWaterMark) {
      doRead = true;
      debug('length less than watermark', doRead);
    }
  
    // however, if we've ended, then there's no point, and if we're already
    // reading, then it's unnecessary.
    if (state.ended || state.reading) {
      doRead = false;
      debug('reading or ended', doRead);
    } else if (doRead) {
      debug('do read');
      state.reading = true;
      state.sync = true;
      // if the length is currently zero, then we *need* a readable event.
      if (state.length === 0) state.needReadable = true;
      // call internal read method
      this._read(state.highWaterMark);
      state.sync = false;
      // If _read pushed data synchronously, then `reading` will be false,
      // and we need to re-evaluate how much data we can return to the user.
      if (!state.reading) n = howMuchToRead(nOrig, state);
    }
  
    var ret;
    if (n > 0) ret = fromList(n, state);else ret = null;
  
    if (ret === null) {
      state.needReadable = true;
      n = 0;
    } else {
      state.length -= n;
    }
  
    if (state.length === 0) {
      // If we have nothing in the buffer, then we want to know
      // as soon as we *do* get something into the buffer.
      if (!state.ended) state.needReadable = true;
  
      // If we tried to read() past the EOF, then emit end on the next tick.
      if (nOrig !== n && state.ended) endReadable(this);
    }
  
    if (ret !== null) this.emit('data', ret);
  
    return ret;
  };
  
  function onEofChunk(stream, state) {
    if (state.ended) return;
    if (state.decoder) {
      var chunk = state.decoder.end();
      if (chunk && chunk.length) {
        state.buffer.push(chunk);
        state.length += state.objectMode ? 1 : chunk.length;
      }
    }
    state.ended = true;
  
    // emit 'readable' now to make sure it gets picked up.
    emitReadable(stream);
  }
  
  // Don't emit readable right away in sync mode, because this can trigger
  // another read() call => stack overflow.  This way, it might trigger
  // a nextTick recursion warning, but that's not so bad.
  function emitReadable(stream) {
    var state = stream._readableState;
    state.needReadable = false;
    if (!state.emittedReadable) {
      debug('emitReadable', state.flowing);
      state.emittedReadable = true;
      if (state.sync) pna.nextTick(emitReadable_, stream);else emitReadable_(stream);
    }
  }
  
  function emitReadable_(stream) {
    debug('emit readable');
    stream.emit('readable');
    flow(stream);
  }
  
  // at this point, the user has presumably seen the 'readable' event,
  // and called read() to consume some data.  that may have triggered
  // in turn another _read(n) call, in which case reading = true if
  // it's in progress.
  // However, if we're not ended, or reading, and the length < hwm,
  // then go ahead and try to read some more preemptively.
  function maybeReadMore(stream, state) {
    if (!state.readingMore) {
      state.readingMore = true;
      pna.nextTick(maybeReadMore_, stream, state);
    }
  }
  
  function maybeReadMore_(stream, state) {
    var len = state.length;
    while (!state.reading && !state.flowing && !state.ended && state.length < state.highWaterMark) {
      debug('maybeReadMore read 0');
      stream.read(0);
      if (len === state.length)
        // didn't get any data, stop spinning.
        break;else len = state.length;
    }
    state.readingMore = false;
  }
  
  // abstract method.  to be overridden in specific implementation classes.
  // call cb(er, data) where data is <= n in length.
  // for virtual (non-string, non-buffer) streams, "length" is somewhat
  // arbitrary, and perhaps not very meaningful.
  Readable.prototype._read = function (n) {
    this.emit('error', new Error('_read() is not implemented'));
  };
  
  Readable.prototype.pipe = function (dest, pipeOpts) {
    var src = this;
    var state = this._readableState;
  
    switch (state.pipesCount) {
      case 0:
        state.pipes = dest;
        break;
      case 1:
        state.pipes = [state.pipes, dest];
        break;
      default:
        state.pipes.push(dest);
        break;
    }
    state.pipesCount += 1;
    debug('pipe count=%d opts=%j', state.pipesCount, pipeOpts);
  
    var doEnd = (!pipeOpts || pipeOpts.end !== false) && dest !== process.stdout && dest !== process.stderr;
  
    var endFn = doEnd ? onend : unpipe;
    if (state.endEmitted) pna.nextTick(endFn);else src.once('end', endFn);
  
    dest.on('unpipe', onunpipe);
    function onunpipe(readable, unpipeInfo) {
      debug('onunpipe');
      if (readable === src) {
        if (unpipeInfo && unpipeInfo.hasUnpiped === false) {
          unpipeInfo.hasUnpiped = true;
          cleanup();
        }
      }
    }
  
    function onend() {
      debug('onend');
      dest.end();
    }
  
    // when the dest drains, it reduces the awaitDrain counter
    // on the source.  This would be more elegant with a .once()
    // handler in flow(), but adding and removing repeatedly is
    // too slow.
    var ondrain = pipeOnDrain(src);
    dest.on('drain', ondrain);
  
    var cleanedUp = false;
    function cleanup() {
      debug('cleanup');
      // cleanup event handlers once the pipe is broken
      dest.removeListener('close', onclose);
      dest.removeListener('finish', onfinish);
      dest.removeListener('drain', ondrain);
      dest.removeListener('error', onerror);
      dest.removeListener('unpipe', onunpipe);
      src.removeListener('end', onend);
      src.removeListener('end', unpipe);
      src.removeListener('data', ondata);
  
      cleanedUp = true;
  
      // if the reader is waiting for a drain event from this
      // specific writer, then it would cause it to never start
      // flowing again.
      // So, if this is awaiting a drain, then we just call it now.
      // If we don't know, then assume that we are waiting for one.
      if (state.awaitDrain && (!dest._writableState || dest._writableState.needDrain)) ondrain();
    }
  
    // If the user pushes more data while we're writing to dest then we'll end up
    // in ondata again. However, we only want to increase awaitDrain once because
    // dest will only emit one 'drain' event for the multiple writes.
    // => Introduce a guard on increasing awaitDrain.
    var increasedAwaitDrain = false;
    src.on('data', ondata);
    function ondata(chunk) {
      debug('ondata');
      increasedAwaitDrain = false;
      var ret = dest.write(chunk);
      if (false === ret && !increasedAwaitDrain) {
        // If the user unpiped during `dest.write()`, it is possible
        // to get stuck in a permanently paused state if that write
        // also returned false.
        // => Check whether `dest` is still a piping destination.
        if ((state.pipesCount === 1 && state.pipes === dest || state.pipesCount > 1 && indexOf(state.pipes, dest) !== -1) && !cleanedUp) {
          debug('false write response, pause', src._readableState.awaitDrain);
          src._readableState.awaitDrain++;
          increasedAwaitDrain = true;
        }
        src.pause();
      }
    }
  
    // if the dest has an error, then stop piping into it.
    // however, don't suppress the throwing behavior for this.
    function onerror(er) {
      debug('onerror', er);
      unpipe();
      dest.removeListener('error', onerror);
      if (EElistenerCount(dest, 'error') === 0) dest.emit('error', er);
    }
  
    // Make sure our error handler is attached before userland ones.
    prependListener(dest, 'error', onerror);
  
    // Both close and finish should trigger unpipe, but only once.
    function onclose() {
      dest.removeListener('finish', onfinish);
      unpipe();
    }
    dest.once('close', onclose);
    function onfinish() {
      debug('onfinish');
      dest.removeListener('close', onclose);
      unpipe();
    }
    dest.once('finish', onfinish);
  
    function unpipe() {
      debug('unpipe');
      src.unpipe(dest);
    }
  
    // tell the dest that it's being piped to
    dest.emit('pipe', src);
  
    // start the flow if it hasn't been started already.
    if (!state.flowing) {
      debug('pipe resume');
      src.resume();
    }
  
    return dest;
  };
  
  function pipeOnDrain(src) {
    return function () {
      var state = src._readableState;
      debug('pipeOnDrain', state.awaitDrain);
      if (state.awaitDrain) state.awaitDrain--;
      if (state.awaitDrain === 0 && EElistenerCount(src, 'data')) {
        state.flowing = true;
        flow(src);
      }
    };
  }
  
  Readable.prototype.unpipe = function (dest) {
    var state = this._readableState;
    var unpipeInfo = { hasUnpiped: false };
  
    // if we're not piping anywhere, then do nothing.
    if (state.pipesCount === 0) return this;
  
    // just one destination.  most common case.
    if (state.pipesCount === 1) {
      // passed in one, but it's not the right one.
      if (dest && dest !== state.pipes) return this;
  
      if (!dest) dest = state.pipes;
  
      // got a match.
      state.pipes = null;
      state.pipesCount = 0;
      state.flowing = false;
      if (dest) dest.emit('unpipe', this, unpipeInfo);
      return this;
    }
  
    // slow case. multiple pipe destinations.
  
    if (!dest) {
      // remove all.
      var dests = state.pipes;
      var len = state.pipesCount;
      state.pipes = null;
      state.pipesCount = 0;
      state.flowing = false;
  
      for (var i = 0; i < len; i++) {
        dests[i].emit('unpipe', this, unpipeInfo);
      }return this;
    }
  
    // try to find the right one.
    var index = indexOf(state.pipes, dest);
    if (index === -1) return this;
  
    state.pipes.splice(index, 1);
    state.pipesCount -= 1;
    if (state.pipesCount === 1) state.pipes = state.pipes[0];
  
    dest.emit('unpipe', this, unpipeInfo);
  
    return this;
  };
  
  // set up data events if they are asked for
  // Ensure readable listeners eventually get something
  Readable.prototype.on = function (ev, fn) {
    var res = Stream.prototype.on.call(this, ev, fn);
  
    if (ev === 'data') {
      // Start flowing on next tick if stream isn't explicitly paused
      if (this._readableState.flowing !== false) this.resume();
    } else if (ev === 'readable') {
      var state = this._readableState;
      if (!state.endEmitted && !state.readableListening) {
        state.readableListening = state.needReadable = true;
        state.emittedReadable = false;
        if (!state.reading) {
          pna.nextTick(nReadingNextTick, this);
        } else if (state.length) {
          emitReadable(this);
        }
      }
    }
  
    return res;
  };
  Readable.prototype.addListener = Readable.prototype.on;
  
  function nReadingNextTick(self) {
    debug('readable nexttick read 0');
    self.read(0);
  }
  
  // pause() and resume() are remnants of the legacy readable stream API
  // If the user uses them, then switch into old mode.
  Readable.prototype.resume = function () {
    var state = this._readableState;
    if (!state.flowing) {
      debug('resume');
      state.flowing = true;
      resume(this, state);
    }
    return this;
  };
  
  function resume(stream, state) {
    if (!state.resumeScheduled) {
      state.resumeScheduled = true;
      pna.nextTick(resume_, stream, state);
    }
  }
  
  function resume_(stream, state) {
    if (!state.reading) {
      debug('resume read 0');
      stream.read(0);
    }
  
    state.resumeScheduled = false;
    state.awaitDrain = 0;
    stream.emit('resume');
    flow(stream);
    if (state.flowing && !state.reading) stream.read(0);
  }
  
  Readable.prototype.pause = function () {
    debug('call pause flowing=%j', this._readableState.flowing);
    if (false !== this._readableState.flowing) {
      debug('pause');
      this._readableState.flowing = false;
      this.emit('pause');
    }
    return this;
  };
  
  function flow(stream) {
    var state = stream._readableState;
    debug('flow', state.flowing);
    while (state.flowing && stream.read() !== null) {}
  }
  
  // wrap an old-style stream as the async data source.
  // This is *not* part of the readable stream interface.
  // It is an ugly unfortunate mess of history.
  Readable.prototype.wrap = function (stream) {
    var _this = this;
  
    var state = this._readableState;
    var paused = false;
  
    stream.on('end', function () {
      debug('wrapped end');
      if (state.decoder && !state.ended) {
        var chunk = state.decoder.end();
        if (chunk && chunk.length) _this.push(chunk);
      }
  
      _this.push(null);
    });
  
    stream.on('data', function (chunk) {
      debug('wrapped data');
      if (state.decoder) chunk = state.decoder.write(chunk);
  
      // don't skip over falsy values in objectMode
      if (state.objectMode && (chunk === null || chunk === undefined)) return;else if (!state.objectMode && (!chunk || !chunk.length)) return;
  
      var ret = _this.push(chunk);
      if (!ret) {
        paused = true;
        stream.pause();
      }
    });
  
    // proxy all the other methods.
    // important when wrapping filters and duplexes.
    for (var i in stream) {
      if (this[i] === undefined && typeof stream[i] === 'function') {
        this[i] = function (method) {
          return function () {
            return stream[method].apply(stream, arguments);
          };
        }(i);
      }
    }
  
    // proxy certain important events.
    for (var n = 0; n < kProxyEvents.length; n++) {
      stream.on(kProxyEvents[n], this.emit.bind(this, kProxyEvents[n]));
    }
  
    // when we try to consume some more bytes, simply unpause the
    // underlying stream.
    this._read = function (n) {
      debug('wrapped _read', n);
      if (paused) {
        paused = false;
        stream.resume();
      }
    };
  
    return this;
  };
  
  Object.defineProperty(Readable.prototype, 'readableHighWaterMark', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function () {
      return this._readableState.highWaterMark;
    }
  });
  
  // exposed for testing purposes only.
  Readable._fromList = fromList;
  
  // Pluck off n bytes from an array of buffers.
  // Length is the combined lengths of all the buffers in the list.
  // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  function fromList(n, state) {
    // nothing buffered
    if (state.length === 0) return null;
  
    var ret;
    if (state.objectMode) ret = state.buffer.shift();else if (!n || n >= state.length) {
      // read it all, truncate the list
      if (state.decoder) ret = state.buffer.join('');else if (state.buffer.length === 1) ret = state.buffer.head.data;else ret = state.buffer.concat(state.length);
      state.buffer.clear();
    } else {
      // read part of list
      ret = fromListPartial(n, state.buffer, state.decoder);
    }
  
    return ret;
  }
  
  // Extracts only enough buffered data to satisfy the amount requested.
  // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  function fromListPartial(n, list, hasStrings) {
    var ret;
    if (n < list.head.data.length) {
      // slice is the same for buffers and strings
      ret = list.head.data.slice(0, n);
      list.head.data = list.head.data.slice(n);
    } else if (n === list.head.data.length) {
      // first chunk is a perfect match
      ret = list.shift();
    } else {
      // result spans more than one buffer
      ret = hasStrings ? copyFromBufferString(n, list) : copyFromBuffer(n, list);
    }
    return ret;
  }
  
  // Copies a specified amount of characters from the list of buffered data
  // chunks.
  // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  function copyFromBufferString(n, list) {
    var p = list.head;
    var c = 1;
    var ret = p.data;
    n -= ret.length;
    while (p = p.next) {
      var str = p.data;
      var nb = n > str.length ? str.length : n;
      if (nb === str.length) ret += str;else ret += str.slice(0, n);
      n -= nb;
      if (n === 0) {
        if (nb === str.length) {
          ++c;
          if (p.next) list.head = p.next;else list.head = list.tail = null;
        } else {
          list.head = p;
          p.data = str.slice(nb);
        }
        break;
      }
      ++c;
    }
    list.length -= c;
    return ret;
  }
  
  // Copies a specified amount of bytes from the list of buffered data chunks.
  // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  function copyFromBuffer(n, list) {
    var ret = Buffer.allocUnsafe(n);
    var p = list.head;
    var c = 1;
    p.data.copy(ret);
    n -= p.data.length;
    while (p = p.next) {
      var buf = p.data;
      var nb = n > buf.length ? buf.length : n;
      buf.copy(ret, ret.length - n, 0, nb);
      n -= nb;
      if (n === 0) {
        if (nb === buf.length) {
          ++c;
          if (p.next) list.head = p.next;else list.head = list.tail = null;
        } else {
          list.head = p;
          p.data = buf.slice(nb);
        }
        break;
      }
      ++c;
    }
    list.length -= c;
    return ret;
  }
  
  function endReadable(stream) {
    var state = stream._readableState;
  
    // If we get here before consuming all the bytes, then that is a
    // bug in node.  Should never happen.
    if (state.length > 0) throw new Error('"endReadable()" called on non-empty stream');
  
    if (!state.endEmitted) {
      state.ended = true;
      pna.nextTick(endReadableNT, state, stream);
    }
  }
  
  function endReadableNT(state, stream) {
    // Check that we didn't get one last unshift.
    if (!state.endEmitted && state.length === 0) {
      state.endEmitted = true;
      stream.readable = false;
      stream.emit('end');
    }
  }
  
  function indexOf(xs, x) {
    for (var i = 0, l = xs.length; i < l; i++) {
      if (xs[i] === x) return i;
    }
    return -1;
  }
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{"./_stream_duplex":38,"./internal/streams/BufferList":43,"./internal/streams/destroy":44,"./internal/streams/stream":45,"_process":32,"core-util-is":11,"events":12,"inherits":15,"isarray":17,"process-nextick-args":31,"safe-buffer":46,"string_decoder/":47,"util":6}],41:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  // a transform stream is a readable/writable stream where you do
  // something with the data.  Sometimes it's called a "filter",
  // but that's not a great name for it, since that implies a thing where
  // some bits pass through, and others are simply ignored.  (That would
  // be a valid example of a transform, of course.)
  //
  // While the output is causally related to the input, it's not a
  // necessarily symmetric or synchronous transformation.  For example,
  // a zlib stream might take multiple plain-text writes(), and then
  // emit a single compressed chunk some time in the future.
  //
  // Here's how this works:
  //
  // The Transform stream has all the aspects of the readable and writable
  // stream classes.  When you write(chunk), that calls _write(chunk,cb)
  // internally, and returns false if there's a lot of pending writes
  // buffered up.  When you call read(), that calls _read(n) until
  // there's enough pending readable data buffered up.
  //
  // In a transform stream, the written data is placed in a buffer.  When
  // _read(n) is called, it transforms the queued up data, calling the
  // buffered _write cb's as it consumes chunks.  If consuming a single
  // written chunk would result in multiple output chunks, then the first
  // outputted bit calls the readcb, and subsequent chunks just go into
  // the read buffer, and will cause it to emit 'readable' if necessary.
  //
  // This way, back-pressure is actually determined by the reading side,
  // since _read has to be called to start processing a new chunk.  However,
  // a pathological inflate type of transform can cause excessive buffering
  // here.  For example, imagine a stream where every byte of input is
  // interpreted as an integer from 0-255, and then results in that many
  // bytes of output.  Writing the 4 bytes {ff,ff,ff,ff} would result in
  // 1kb of data being output.  In this case, you could write a very small
  // amount of input, and end up with a very large amount of output.  In
  // such a pathological inflating mechanism, there'd be no way to tell
  // the system to stop doing the transform.  A single 4MB write could
  // cause the system to run out of memory.
  //
  // However, even in such a pathological case, only a single written chunk
  // would be consumed, and then the rest would wait (un-transformed) until
  // the results of the previous transformed chunk were consumed.
  
  'use strict';
  
  module.exports = Transform;
  
  var Duplex = require('./_stream_duplex');
  
  /*<replacement>*/
  var util = Object.create(require('core-util-is'));
  util.inherits = require('inherits');
  /*</replacement>*/
  
  util.inherits(Transform, Duplex);
  
  function afterTransform(er, data) {
    var ts = this._transformState;
    ts.transforming = false;
  
    var cb = ts.writecb;
  
    if (!cb) {
      return this.emit('error', new Error('write callback called multiple times'));
    }
  
    ts.writechunk = null;
    ts.writecb = null;
  
    if (data != null) // single equals check for both `null` and `undefined`
      this.push(data);
  
    cb(er);
  
    var rs = this._readableState;
    rs.reading = false;
    if (rs.needReadable || rs.length < rs.highWaterMark) {
      this._read(rs.highWaterMark);
    }
  }
  
  function Transform(options) {
    if (!(this instanceof Transform)) return new Transform(options);
  
    Duplex.call(this, options);
  
    this._transformState = {
      afterTransform: afterTransform.bind(this),
      needTransform: false,
      transforming: false,
      writecb: null,
      writechunk: null,
      writeencoding: null
    };
  
    // start out asking for a readable event once data is transformed.
    this._readableState.needReadable = true;
  
    // we have implemented the _read method, and done the other things
    // that Readable wants before the first _read call, so unset the
    // sync guard flag.
    this._readableState.sync = false;
  
    if (options) {
      if (typeof options.transform === 'function') this._transform = options.transform;
  
      if (typeof options.flush === 'function') this._flush = options.flush;
    }
  
    // When the writable side finishes, then flush out anything remaining.
    this.on('prefinish', prefinish);
  }
  
  function prefinish() {
    var _this = this;
  
    if (typeof this._flush === 'function') {
      this._flush(function (er, data) {
        done(_this, er, data);
      });
    } else {
      done(this, null, null);
    }
  }
  
  Transform.prototype.push = function (chunk, encoding) {
    this._transformState.needTransform = false;
    return Duplex.prototype.push.call(this, chunk, encoding);
  };
  
  // This is the part where you do stuff!
  // override this function in implementation classes.
  // 'chunk' is an input chunk.
  //
  // Call `push(newChunk)` to pass along transformed output
  // to the readable side.  You may call 'push' zero or more times.
  //
  // Call `cb(err)` when you are done with this chunk.  If you pass
  // an error, then that'll put the hurt on the whole operation.  If you
  // never call cb(), then you'll never get another chunk.
  Transform.prototype._transform = function (chunk, encoding, cb) {
    throw new Error('_transform() is not implemented');
  };
  
  Transform.prototype._write = function (chunk, encoding, cb) {
    var ts = this._transformState;
    ts.writecb = cb;
    ts.writechunk = chunk;
    ts.writeencoding = encoding;
    if (!ts.transforming) {
      var rs = this._readableState;
      if (ts.needTransform || rs.needReadable || rs.length < rs.highWaterMark) this._read(rs.highWaterMark);
    }
  };
  
  // Doesn't matter what the args are here.
  // _transform does all the work.
  // That we got here means that the readable side wants more data.
  Transform.prototype._read = function (n) {
    var ts = this._transformState;
  
    if (ts.writechunk !== null && ts.writecb && !ts.transforming) {
      ts.transforming = true;
      this._transform(ts.writechunk, ts.writeencoding, ts.afterTransform);
    } else {
      // mark that we need a transform, so that any data that comes in
      // will get processed, now that we've asked for it.
      ts.needTransform = true;
    }
  };
  
  Transform.prototype._destroy = function (err, cb) {
    var _this2 = this;
  
    Duplex.prototype._destroy.call(this, err, function (err2) {
      cb(err2);
      _this2.emit('close');
    });
  };
  
  function done(stream, er, data) {
    if (er) return stream.emit('error', er);
  
    if (data != null) // single equals check for both `null` and `undefined`
      stream.push(data);
  
    // if there's nothing in the write buffer, then that means
    // that nothing more will ever be provided
    if (stream._writableState.length) throw new Error('Calling transform done when ws.length != 0');
  
    if (stream._transformState.transforming) throw new Error('Calling transform done when still transforming');
  
    return stream.push(null);
  }
  },{"./_stream_duplex":38,"core-util-is":11,"inherits":15}],42:[function(require,module,exports){
  (function (process,global,setImmediate){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  // A bit simpler than readable streams.
  // Implement an async ._write(chunk, encoding, cb), and it'll handle all
  // the drain event emission and buffering.
  
  'use strict';
  
  /*<replacement>*/
  
  var pna = require('process-nextick-args');
  /*</replacement>*/
  
  module.exports = Writable;
  
  /* <replacement> */
  function WriteReq(chunk, encoding, cb) {
    this.chunk = chunk;
    this.encoding = encoding;
    this.callback = cb;
    this.next = null;
  }
  
  // It seems a linked list but it is not
  // there will be only 2 of these for each stream
  function CorkedRequest(state) {
    var _this = this;
  
    this.next = null;
    this.entry = null;
    this.finish = function () {
      onCorkedFinish(_this, state);
    };
  }
  /* </replacement> */
  
  /*<replacement>*/
  var asyncWrite = !process.browser && ['v0.10', 'v0.9.'].indexOf(process.version.slice(0, 5)) > -1 ? setImmediate : pna.nextTick;
  /*</replacement>*/
  
  /*<replacement>*/
  var Duplex;
  /*</replacement>*/
  
  Writable.WritableState = WritableState;
  
  /*<replacement>*/
  var util = Object.create(require('core-util-is'));
  util.inherits = require('inherits');
  /*</replacement>*/
  
  /*<replacement>*/
  var internalUtil = {
    deprecate: require('util-deprecate')
  };
  /*</replacement>*/
  
  /*<replacement>*/
  var Stream = require('./internal/streams/stream');
  /*</replacement>*/
  
  /*<replacement>*/
  
  var Buffer = require('safe-buffer').Buffer;
  var OurUint8Array = global.Uint8Array || function () {};
  function _uint8ArrayToBuffer(chunk) {
    return Buffer.from(chunk);
  }
  function _isUint8Array(obj) {
    return Buffer.isBuffer(obj) || obj instanceof OurUint8Array;
  }
  
  /*</replacement>*/
  
  var destroyImpl = require('./internal/streams/destroy');
  
  util.inherits(Writable, Stream);
  
  function nop() {}
  
  function WritableState(options, stream) {
    Duplex = Duplex || require('./_stream_duplex');
  
    options = options || {};
  
    // Duplex streams are both readable and writable, but share
    // the same options object.
    // However, some cases require setting options to different
    // values for the readable and the writable sides of the duplex stream.
    // These options can be provided separately as readableXXX and writableXXX.
    var isDuplex = stream instanceof Duplex;
  
    // object stream flag to indicate whether or not this stream
    // contains buffers or objects.
    this.objectMode = !!options.objectMode;
  
    if (isDuplex) this.objectMode = this.objectMode || !!options.writableObjectMode;
  
    // the point at which write() starts returning false
    // Note: 0 is a valid value, means that we always return false if
    // the entire buffer is not flushed immediately on write()
    var hwm = options.highWaterMark;
    var writableHwm = options.writableHighWaterMark;
    var defaultHwm = this.objectMode ? 16 : 16 * 1024;
  
    if (hwm || hwm === 0) this.highWaterMark = hwm;else if (isDuplex && (writableHwm || writableHwm === 0)) this.highWaterMark = writableHwm;else this.highWaterMark = defaultHwm;
  
    // cast to ints.
    this.highWaterMark = Math.floor(this.highWaterMark);
  
    // if _final has been called
    this.finalCalled = false;
  
    // drain event flag.
    this.needDrain = false;
    // at the start of calling end()
    this.ending = false;
    // when end() has been called, and returned
    this.ended = false;
    // when 'finish' is emitted
    this.finished = false;
  
    // has it been destroyed
    this.destroyed = false;
  
    // should we decode strings into buffers before passing to _write?
    // this is here so that some node-core streams can optimize string
    // handling at a lower level.
    var noDecode = options.decodeStrings === false;
    this.decodeStrings = !noDecode;
  
    // Crypto is kind of old and crusty.  Historically, its default string
    // encoding is 'binary' so we have to make this configurable.
    // Everything else in the universe uses 'utf8', though.
    this.defaultEncoding = options.defaultEncoding || 'utf8';
  
    // not an actual buffer we keep track of, but a measurement
    // of how much we're waiting to get pushed to some underlying
    // socket or file.
    this.length = 0;
  
    // a flag to see when we're in the middle of a write.
    this.writing = false;
  
    // when true all writes will be buffered until .uncork() call
    this.corked = 0;
  
    // a flag to be able to tell if the onwrite cb is called immediately,
    // or on a later tick.  We set this to true at first, because any
    // actions that shouldn't happen until "later" should generally also
    // not happen before the first write call.
    this.sync = true;
  
    // a flag to know if we're processing previously buffered items, which
    // may call the _write() callback in the same tick, so that we don't
    // end up in an overlapped onwrite situation.
    this.bufferProcessing = false;
  
    // the callback that's passed to _write(chunk,cb)
    this.onwrite = function (er) {
      onwrite(stream, er);
    };
  
    // the callback that the user supplies to write(chunk,encoding,cb)
    this.writecb = null;
  
    // the amount that is being written when _write is called.
    this.writelen = 0;
  
    this.bufferedRequest = null;
    this.lastBufferedRequest = null;
  
    // number of pending user-supplied write callbacks
    // this must be 0 before 'finish' can be emitted
    this.pendingcb = 0;
  
    // emit prefinish if the only thing we're waiting for is _write cbs
    // This is relevant for synchronous Transform streams
    this.prefinished = false;
  
    // True if the error was already emitted and should not be thrown again
    this.errorEmitted = false;
  
    // count buffered requests
    this.bufferedRequestCount = 0;
  
    // allocate the first CorkedRequest, there is always
    // one allocated and free to use, and we maintain at most two
    this.corkedRequestsFree = new CorkedRequest(this);
  }
  
  WritableState.prototype.getBuffer = function getBuffer() {
    var current = this.bufferedRequest;
    var out = [];
    while (current) {
      out.push(current);
      current = current.next;
    }
    return out;
  };
  
  (function () {
    try {
      Object.defineProperty(WritableState.prototype, 'buffer', {
        get: internalUtil.deprecate(function () {
          return this.getBuffer();
        }, '_writableState.buffer is deprecated. Use _writableState.getBuffer ' + 'instead.', 'DEP0003')
      });
    } catch (_) {}
  })();
  
  // Test _writableState for inheritance to account for Duplex streams,
  // whose prototype chain only points to Readable.
  var realHasInstance;
  if (typeof Symbol === 'function' && Symbol.hasInstance && typeof Function.prototype[Symbol.hasInstance] === 'function') {
    realHasInstance = Function.prototype[Symbol.hasInstance];
    Object.defineProperty(Writable, Symbol.hasInstance, {
      value: function (object) {
        if (realHasInstance.call(this, object)) return true;
        if (this !== Writable) return false;
  
        return object && object._writableState instanceof WritableState;
      }
    });
  } else {
    realHasInstance = function (object) {
      return object instanceof this;
    };
  }
  
  function Writable(options) {
    Duplex = Duplex || require('./_stream_duplex');
  
    // Writable ctor is applied to Duplexes, too.
    // `realHasInstance` is necessary because using plain `instanceof`
    // would return false, as no `_writableState` property is attached.
  
    // Trying to use the custom `instanceof` for Writable here will also break the
    // Node.js LazyTransform implementation, which has a non-trivial getter for
    // `_writableState` that would lead to infinite recursion.
    if (!realHasInstance.call(Writable, this) && !(this instanceof Duplex)) {
      return new Writable(options);
    }
  
    this._writableState = new WritableState(options, this);
  
    // legacy.
    this.writable = true;
  
    if (options) {
      if (typeof options.write === 'function') this._write = options.write;
  
      if (typeof options.writev === 'function') this._writev = options.writev;
  
      if (typeof options.destroy === 'function') this._destroy = options.destroy;
  
      if (typeof options.final === 'function') this._final = options.final;
    }
  
    Stream.call(this);
  }
  
  // Otherwise people can pipe Writable streams, which is just wrong.
  Writable.prototype.pipe = function () {
    this.emit('error', new Error('Cannot pipe, not readable'));
  };
  
  function writeAfterEnd(stream, cb) {
    var er = new Error('write after end');
    // TODO: defer error events consistently everywhere, not just the cb
    stream.emit('error', er);
    pna.nextTick(cb, er);
  }
  
  // Checks that a user-supplied chunk is valid, especially for the particular
  // mode the stream is in. Currently this means that `null` is never accepted
  // and undefined/non-string values are only allowed in object mode.
  function validChunk(stream, state, chunk, cb) {
    var valid = true;
    var er = false;
  
    if (chunk === null) {
      er = new TypeError('May not write null values to stream');
    } else if (typeof chunk !== 'string' && chunk !== undefined && !state.objectMode) {
      er = new TypeError('Invalid non-string/buffer chunk');
    }
    if (er) {
      stream.emit('error', er);
      pna.nextTick(cb, er);
      valid = false;
    }
    return valid;
  }
  
  Writable.prototype.write = function (chunk, encoding, cb) {
    var state = this._writableState;
    var ret = false;
    var isBuf = !state.objectMode && _isUint8Array(chunk);
  
    if (isBuf && !Buffer.isBuffer(chunk)) {
      chunk = _uint8ArrayToBuffer(chunk);
    }
  
    if (typeof encoding === 'function') {
      cb = encoding;
      encoding = null;
    }
  
    if (isBuf) encoding = 'buffer';else if (!encoding) encoding = state.defaultEncoding;
  
    if (typeof cb !== 'function') cb = nop;
  
    if (state.ended) writeAfterEnd(this, cb);else if (isBuf || validChunk(this, state, chunk, cb)) {
      state.pendingcb++;
      ret = writeOrBuffer(this, state, isBuf, chunk, encoding, cb);
    }
  
    return ret;
  };
  
  Writable.prototype.cork = function () {
    var state = this._writableState;
  
    state.corked++;
  };
  
  Writable.prototype.uncork = function () {
    var state = this._writableState;
  
    if (state.corked) {
      state.corked--;
  
      if (!state.writing && !state.corked && !state.finished && !state.bufferProcessing && state.bufferedRequest) clearBuffer(this, state);
    }
  };
  
  Writable.prototype.setDefaultEncoding = function setDefaultEncoding(encoding) {
    // node::ParseEncoding() requires lower case.
    if (typeof encoding === 'string') encoding = encoding.toLowerCase();
    if (!(['hex', 'utf8', 'utf-8', 'ascii', 'binary', 'base64', 'ucs2', 'ucs-2', 'utf16le', 'utf-16le', 'raw'].indexOf((encoding + '').toLowerCase()) > -1)) throw new TypeError('Unknown encoding: ' + encoding);
    this._writableState.defaultEncoding = encoding;
    return this;
  };
  
  function decodeChunk(state, chunk, encoding) {
    if (!state.objectMode && state.decodeStrings !== false && typeof chunk === 'string') {
      chunk = Buffer.from(chunk, encoding);
    }
    return chunk;
  }
  
  Object.defineProperty(Writable.prototype, 'writableHighWaterMark', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function () {
      return this._writableState.highWaterMark;
    }
  });
  
  // if we're already writing something, then just put this
  // in the queue, and wait our turn.  Otherwise, call _write
  // If we return false, then we need a drain event, so set that flag.
  function writeOrBuffer(stream, state, isBuf, chunk, encoding, cb) {
    if (!isBuf) {
      var newChunk = decodeChunk(state, chunk, encoding);
      if (chunk !== newChunk) {
        isBuf = true;
        encoding = 'buffer';
        chunk = newChunk;
      }
    }
    var len = state.objectMode ? 1 : chunk.length;
  
    state.length += len;
  
    var ret = state.length < state.highWaterMark;
    // we must ensure that previous needDrain will not be reset to false.
    if (!ret) state.needDrain = true;
  
    if (state.writing || state.corked) {
      var last = state.lastBufferedRequest;
      state.lastBufferedRequest = {
        chunk: chunk,
        encoding: encoding,
        isBuf: isBuf,
        callback: cb,
        next: null
      };
      if (last) {
        last.next = state.lastBufferedRequest;
      } else {
        state.bufferedRequest = state.lastBufferedRequest;
      }
      state.bufferedRequestCount += 1;
    } else {
      doWrite(stream, state, false, len, chunk, encoding, cb);
    }
  
    return ret;
  }
  
  function doWrite(stream, state, writev, len, chunk, encoding, cb) {
    state.writelen = len;
    state.writecb = cb;
    state.writing = true;
    state.sync = true;
    if (writev) stream._writev(chunk, state.onwrite);else stream._write(chunk, encoding, state.onwrite);
    state.sync = false;
  }
  
  function onwriteError(stream, state, sync, er, cb) {
    --state.pendingcb;
  
    if (sync) {
      // defer the callback if we are being called synchronously
      // to avoid piling up things on the stack
      pna.nextTick(cb, er);
      // this can emit finish, and it will always happen
      // after error
      pna.nextTick(finishMaybe, stream, state);
      stream._writableState.errorEmitted = true;
      stream.emit('error', er);
    } else {
      // the caller expect this to happen before if
      // it is async
      cb(er);
      stream._writableState.errorEmitted = true;
      stream.emit('error', er);
      // this can emit finish, but finish must
      // always follow error
      finishMaybe(stream, state);
    }
  }
  
  function onwriteStateUpdate(state) {
    state.writing = false;
    state.writecb = null;
    state.length -= state.writelen;
    state.writelen = 0;
  }
  
  function onwrite(stream, er) {
    var state = stream._writableState;
    var sync = state.sync;
    var cb = state.writecb;
  
    onwriteStateUpdate(state);
  
    if (er) onwriteError(stream, state, sync, er, cb);else {
      // Check if we're actually ready to finish, but don't emit yet
      var finished = needFinish(state);
  
      if (!finished && !state.corked && !state.bufferProcessing && state.bufferedRequest) {
        clearBuffer(stream, state);
      }
  
      if (sync) {
        /*<replacement>*/
        asyncWrite(afterWrite, stream, state, finished, cb);
        /*</replacement>*/
      } else {
        afterWrite(stream, state, finished, cb);
      }
    }
  }
  
  function afterWrite(stream, state, finished, cb) {
    if (!finished) onwriteDrain(stream, state);
    state.pendingcb--;
    cb();
    finishMaybe(stream, state);
  }
  
  // Must force callback to be called on nextTick, so that we don't
  // emit 'drain' before the write() consumer gets the 'false' return
  // value, and has a chance to attach a 'drain' listener.
  function onwriteDrain(stream, state) {
    if (state.length === 0 && state.needDrain) {
      state.needDrain = false;
      stream.emit('drain');
    }
  }
  
  // if there's something in the buffer waiting, then process it
  function clearBuffer(stream, state) {
    state.bufferProcessing = true;
    var entry = state.bufferedRequest;
  
    if (stream._writev && entry && entry.next) {
      // Fast case, write everything using _writev()
      var l = state.bufferedRequestCount;
      var buffer = new Array(l);
      var holder = state.corkedRequestsFree;
      holder.entry = entry;
  
      var count = 0;
      var allBuffers = true;
      while (entry) {
        buffer[count] = entry;
        if (!entry.isBuf) allBuffers = false;
        entry = entry.next;
        count += 1;
      }
      buffer.allBuffers = allBuffers;
  
      doWrite(stream, state, true, state.length, buffer, '', holder.finish);
  
      // doWrite is almost always async, defer these to save a bit of time
      // as the hot path ends with doWrite
      state.pendingcb++;
      state.lastBufferedRequest = null;
      if (holder.next) {
        state.corkedRequestsFree = holder.next;
        holder.next = null;
      } else {
        state.corkedRequestsFree = new CorkedRequest(state);
      }
      state.bufferedRequestCount = 0;
    } else {
      // Slow case, write chunks one-by-one
      while (entry) {
        var chunk = entry.chunk;
        var encoding = entry.encoding;
        var cb = entry.callback;
        var len = state.objectMode ? 1 : chunk.length;
  
        doWrite(stream, state, false, len, chunk, encoding, cb);
        entry = entry.next;
        state.bufferedRequestCount--;
        // if we didn't call the onwrite immediately, then
        // it means that we need to wait until it does.
        // also, that means that the chunk and cb are currently
        // being processed, so move the buffer counter past them.
        if (state.writing) {
          break;
        }
      }
  
      if (entry === null) state.lastBufferedRequest = null;
    }
  
    state.bufferedRequest = entry;
    state.bufferProcessing = false;
  }
  
  Writable.prototype._write = function (chunk, encoding, cb) {
    cb(new Error('_write() is not implemented'));
  };
  
  Writable.prototype._writev = null;
  
  Writable.prototype.end = function (chunk, encoding, cb) {
    var state = this._writableState;
  
    if (typeof chunk === 'function') {
      cb = chunk;
      chunk = null;
      encoding = null;
    } else if (typeof encoding === 'function') {
      cb = encoding;
      encoding = null;
    }
  
    if (chunk !== null && chunk !== undefined) this.write(chunk, encoding);
  
    // .end() fully uncorks
    if (state.corked) {
      state.corked = 1;
      this.uncork();
    }
  
    // ignore unnecessary end() calls.
    if (!state.ending && !state.finished) endWritable(this, state, cb);
  };
  
  function needFinish(state) {
    return state.ending && state.length === 0 && state.bufferedRequest === null && !state.finished && !state.writing;
  }
  function callFinal(stream, state) {
    stream._final(function (err) {
      state.pendingcb--;
      if (err) {
        stream.emit('error', err);
      }
      state.prefinished = true;
      stream.emit('prefinish');
      finishMaybe(stream, state);
    });
  }
  function prefinish(stream, state) {
    if (!state.prefinished && !state.finalCalled) {
      if (typeof stream._final === 'function') {
        state.pendingcb++;
        state.finalCalled = true;
        pna.nextTick(callFinal, stream, state);
      } else {
        state.prefinished = true;
        stream.emit('prefinish');
      }
    }
  }
  
  function finishMaybe(stream, state) {
    var need = needFinish(state);
    if (need) {
      prefinish(stream, state);
      if (state.pendingcb === 0) {
        state.finished = true;
        stream.emit('finish');
      }
    }
    return need;
  }
  
  function endWritable(stream, state, cb) {
    state.ending = true;
    finishMaybe(stream, state);
    if (cb) {
      if (state.finished) pna.nextTick(cb);else stream.once('finish', cb);
    }
    state.ended = true;
    stream.writable = false;
  }
  
  function onCorkedFinish(corkReq, state, err) {
    var entry = corkReq.entry;
    corkReq.entry = null;
    while (entry) {
      var cb = entry.callback;
      state.pendingcb--;
      cb(err);
      entry = entry.next;
    }
    if (state.corkedRequestsFree) {
      state.corkedRequestsFree.next = corkReq;
    } else {
      state.corkedRequestsFree = corkReq;
    }
  }
  
  Object.defineProperty(Writable.prototype, 'destroyed', {
    get: function () {
      if (this._writableState === undefined) {
        return false;
      }
      return this._writableState.destroyed;
    },
    set: function (value) {
      // we ignore the value if the stream
      // has not been initialized yet
      if (!this._writableState) {
        return;
      }
  
      // backward compatibility, the user is explicitly
      // managing destroyed
      this._writableState.destroyed = value;
    }
  });
  
  Writable.prototype.destroy = destroyImpl.destroy;
  Writable.prototype._undestroy = destroyImpl.undestroy;
  Writable.prototype._destroy = function (err, cb) {
    this.end();
    cb(err);
  };
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {},require("timers").setImmediate)
  },{"./_stream_duplex":38,"./internal/streams/destroy":44,"./internal/streams/stream":45,"_process":32,"core-util-is":11,"inherits":15,"process-nextick-args":31,"safe-buffer":46,"timers":74,"util-deprecate":77}],43:[function(require,module,exports){
  'use strict';
  
  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }
  
  var Buffer = require('safe-buffer').Buffer;
  var util = require('util');
  
  function copyBuffer(src, target, offset) {
    src.copy(target, offset);
  }
  
  module.exports = function () {
    function BufferList() {
      _classCallCheck(this, BufferList);
  
      this.head = null;
      this.tail = null;
      this.length = 0;
    }
  
    BufferList.prototype.push = function push(v) {
      var entry = { data: v, next: null };
      if (this.length > 0) this.tail.next = entry;else this.head = entry;
      this.tail = entry;
      ++this.length;
    };
  
    BufferList.prototype.unshift = function unshift(v) {
      var entry = { data: v, next: this.head };
      if (this.length === 0) this.tail = entry;
      this.head = entry;
      ++this.length;
    };
  
    BufferList.prototype.shift = function shift() {
      if (this.length === 0) return;
      var ret = this.head.data;
      if (this.length === 1) this.head = this.tail = null;else this.head = this.head.next;
      --this.length;
      return ret;
    };
  
    BufferList.prototype.clear = function clear() {
      this.head = this.tail = null;
      this.length = 0;
    };
  
    BufferList.prototype.join = function join(s) {
      if (this.length === 0) return '';
      var p = this.head;
      var ret = '' + p.data;
      while (p = p.next) {
        ret += s + p.data;
      }return ret;
    };
  
    BufferList.prototype.concat = function concat(n) {
      if (this.length === 0) return Buffer.alloc(0);
      if (this.length === 1) return this.head.data;
      var ret = Buffer.allocUnsafe(n >>> 0);
      var p = this.head;
      var i = 0;
      while (p) {
        copyBuffer(p.data, ret, i);
        i += p.data.length;
        p = p.next;
      }
      return ret;
    };
  
    return BufferList;
  }();
  
  if (util && util.inspect && util.inspect.custom) {
    module.exports.prototype[util.inspect.custom] = function () {
      var obj = util.inspect({ length: this.length });
      return this.constructor.name + ' ' + obj;
    };
  }
  },{"safe-buffer":46,"util":6}],44:[function(require,module,exports){
  'use strict';
  
  /*<replacement>*/
  
  var pna = require('process-nextick-args');
  /*</replacement>*/
  
  // undocumented cb() API, needed for core, not for public API
  function destroy(err, cb) {
    var _this = this;
  
    var readableDestroyed = this._readableState && this._readableState.destroyed;
    var writableDestroyed = this._writableState && this._writableState.destroyed;
  
    if (readableDestroyed || writableDestroyed) {
      if (cb) {
        cb(err);
      } else if (err && (!this._writableState || !this._writableState.errorEmitted)) {
        pna.nextTick(emitErrorNT, this, err);
      }
      return this;
    }
  
    // we set destroyed to true before firing error callbacks in order
    // to make it re-entrance safe in case destroy() is called within callbacks
  
    if (this._readableState) {
      this._readableState.destroyed = true;
    }
  
    // if this is a duplex stream mark the writable part as destroyed as well
    if (this._writableState) {
      this._writableState.destroyed = true;
    }
  
    this._destroy(err || null, function (err) {
      if (!cb && err) {
        pna.nextTick(emitErrorNT, _this, err);
        if (_this._writableState) {
          _this._writableState.errorEmitted = true;
        }
      } else if (cb) {
        cb(err);
      }
    });
  
    return this;
  }
  
  function undestroy() {
    if (this._readableState) {
      this._readableState.destroyed = false;
      this._readableState.reading = false;
      this._readableState.ended = false;
      this._readableState.endEmitted = false;
    }
  
    if (this._writableState) {
      this._writableState.destroyed = false;
      this._writableState.ended = false;
      this._writableState.ending = false;
      this._writableState.finished = false;
      this._writableState.errorEmitted = false;
    }
  }
  
  function emitErrorNT(self, err) {
    self.emit('error', err);
  }
  
  module.exports = {
    destroy: destroy,
    undestroy: undestroy
  };
  },{"process-nextick-args":31}],45:[function(require,module,exports){
  module.exports = require('events').EventEmitter;
  
  },{"events":12}],46:[function(require,module,exports){
  /* eslint-disable node/no-deprecated-api */
  var buffer = require('buffer')
  var Buffer = buffer.Buffer
  
  // alternative to using Object.keys for old browsers
  function copyProps (src, dst) {
    for (var key in src) {
      dst[key] = src[key]
    }
  }
  if (Buffer.from && Buffer.alloc && Buffer.allocUnsafe && Buffer.allocUnsafeSlow) {
    module.exports = buffer
  } else {
    // Copy properties from require('buffer')
    copyProps(buffer, exports)
    exports.Buffer = SafeBuffer
  }
  
  function SafeBuffer (arg, encodingOrOffset, length) {
    return Buffer(arg, encodingOrOffset, length)
  }
  
  // Copy static methods from Buffer
  copyProps(Buffer, SafeBuffer)
  
  SafeBuffer.from = function (arg, encodingOrOffset, length) {
    if (typeof arg === 'number') {
      throw new TypeError('Argument must not be a number')
    }
    return Buffer(arg, encodingOrOffset, length)
  }
  
  SafeBuffer.alloc = function (size, fill, encoding) {
    if (typeof size !== 'number') {
      throw new TypeError('Argument must be a number')
    }
    var buf = Buffer(size)
    if (fill !== undefined) {
      if (typeof encoding === 'string') {
        buf.fill(fill, encoding)
      } else {
        buf.fill(fill)
      }
    } else {
      buf.fill(0)
    }
    return buf
  }
  
  SafeBuffer.allocUnsafe = function (size) {
    if (typeof size !== 'number') {
      throw new TypeError('Argument must be a number')
    }
    return Buffer(size)
  }
  
  SafeBuffer.allocUnsafeSlow = function (size) {
    if (typeof size !== 'number') {
      throw new TypeError('Argument must be a number')
    }
    return buffer.SlowBuffer(size)
  }
  
  },{"buffer":9}],47:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  'use strict';
  
  /*<replacement>*/
  
  var Buffer = require('safe-buffer').Buffer;
  /*</replacement>*/
  
  var isEncoding = Buffer.isEncoding || function (encoding) {
    encoding = '' + encoding;
    switch (encoding && encoding.toLowerCase()) {
      case 'hex':case 'utf8':case 'utf-8':case 'ascii':case 'binary':case 'base64':case 'ucs2':case 'ucs-2':case 'utf16le':case 'utf-16le':case 'raw':
        return true;
      default:
        return false;
    }
  };
  
  function _normalizeEncoding(enc) {
    if (!enc) return 'utf8';
    var retried;
    while (true) {
      switch (enc) {
        case 'utf8':
        case 'utf-8':
          return 'utf8';
        case 'ucs2':
        case 'ucs-2':
        case 'utf16le':
        case 'utf-16le':
          return 'utf16le';
        case 'latin1':
        case 'binary':
          return 'latin1';
        case 'base64':
        case 'ascii':
        case 'hex':
          return enc;
        default:
          if (retried) return; // undefined
          enc = ('' + enc).toLowerCase();
          retried = true;
      }
    }
  };
  
  // Do not cache `Buffer.isEncoding` when checking encoding names as some
  // modules monkey-patch it to support additional encodings
  function normalizeEncoding(enc) {
    var nenc = _normalizeEncoding(enc);
    if (typeof nenc !== 'string' && (Buffer.isEncoding === isEncoding || !isEncoding(enc))) throw new Error('Unknown encoding: ' + enc);
    return nenc || enc;
  }
  
  // StringDecoder provides an interface for efficiently splitting a series of
  // buffers into a series of JS strings without breaking apart multi-byte
  // characters.
  exports.StringDecoder = StringDecoder;
  function StringDecoder(encoding) {
    this.encoding = normalizeEncoding(encoding);
    var nb;
    switch (this.encoding) {
      case 'utf16le':
        this.text = utf16Text;
        this.end = utf16End;
        nb = 4;
        break;
      case 'utf8':
        this.fillLast = utf8FillLast;
        nb = 4;
        break;
      case 'base64':
        this.text = base64Text;
        this.end = base64End;
        nb = 3;
        break;
      default:
        this.write = simpleWrite;
        this.end = simpleEnd;
        return;
    }
    this.lastNeed = 0;
    this.lastTotal = 0;
    this.lastChar = Buffer.allocUnsafe(nb);
  }
  
  StringDecoder.prototype.write = function (buf) {
    if (buf.length === 0) return '';
    var r;
    var i;
    if (this.lastNeed) {
      r = this.fillLast(buf);
      if (r === undefined) return '';
      i = this.lastNeed;
      this.lastNeed = 0;
    } else {
      i = 0;
    }
    if (i < buf.length) return r ? r + this.text(buf, i) : this.text(buf, i);
    return r || '';
  };
  
  StringDecoder.prototype.end = utf8End;
  
  // Returns only complete characters in a Buffer
  StringDecoder.prototype.text = utf8Text;
  
  // Attempts to complete a partial non-UTF-8 character using bytes from a Buffer
  StringDecoder.prototype.fillLast = function (buf) {
    if (this.lastNeed <= buf.length) {
      buf.copy(this.lastChar, this.lastTotal - this.lastNeed, 0, this.lastNeed);
      return this.lastChar.toString(this.encoding, 0, this.lastTotal);
    }
    buf.copy(this.lastChar, this.lastTotal - this.lastNeed, 0, buf.length);
    this.lastNeed -= buf.length;
  };
  
  // Checks the type of a UTF-8 byte, whether it's ASCII, a leading byte, or a
  // continuation byte. If an invalid byte is detected, -2 is returned.
  function utf8CheckByte(byte) {
    if (byte <= 0x7F) return 0;else if (byte >> 5 === 0x06) return 2;else if (byte >> 4 === 0x0E) return 3;else if (byte >> 3 === 0x1E) return 4;
    return byte >> 6 === 0x02 ? -1 : -2;
  }
  
  // Checks at most 3 bytes at the end of a Buffer in order to detect an
  // incomplete multi-byte UTF-8 character. The total number of bytes (2, 3, or 4)
  // needed to complete the UTF-8 character (if applicable) are returned.
  function utf8CheckIncomplete(self, buf, i) {
    var j = buf.length - 1;
    if (j < i) return 0;
    var nb = utf8CheckByte(buf[j]);
    if (nb >= 0) {
      if (nb > 0) self.lastNeed = nb - 1;
      return nb;
    }
    if (--j < i || nb === -2) return 0;
    nb = utf8CheckByte(buf[j]);
    if (nb >= 0) {
      if (nb > 0) self.lastNeed = nb - 2;
      return nb;
    }
    if (--j < i || nb === -2) return 0;
    nb = utf8CheckByte(buf[j]);
    if (nb >= 0) {
      if (nb > 0) {
        if (nb === 2) nb = 0;else self.lastNeed = nb - 3;
      }
      return nb;
    }
    return 0;
  }
  
  // Validates as many continuation bytes for a multi-byte UTF-8 character as
  // needed or are available. If we see a non-continuation byte where we expect
  // one, we "replace" the validated continuation bytes we've seen so far with
  // a single UTF-8 replacement character ('\ufffd'), to match v8's UTF-8 decoding
  // behavior. The continuation byte check is included three times in the case
  // where all of the continuation bytes for a character exist in the same buffer.
  // It is also done this way as a slight performance increase instead of using a
  // loop.
  function utf8CheckExtraBytes(self, buf, p) {
    if ((buf[0] & 0xC0) !== 0x80) {
      self.lastNeed = 0;
      return '\ufffd';
    }
    if (self.lastNeed > 1 && buf.length > 1) {
      if ((buf[1] & 0xC0) !== 0x80) {
        self.lastNeed = 1;
        return '\ufffd';
      }
      if (self.lastNeed > 2 && buf.length > 2) {
        if ((buf[2] & 0xC0) !== 0x80) {
          self.lastNeed = 2;
          return '\ufffd';
        }
      }
    }
  }
  
  // Attempts to complete a multi-byte UTF-8 character using bytes from a Buffer.
  function utf8FillLast(buf) {
    var p = this.lastTotal - this.lastNeed;
    var r = utf8CheckExtraBytes(this, buf, p);
    if (r !== undefined) return r;
    if (this.lastNeed <= buf.length) {
      buf.copy(this.lastChar, p, 0, this.lastNeed);
      return this.lastChar.toString(this.encoding, 0, this.lastTotal);
    }
    buf.copy(this.lastChar, p, 0, buf.length);
    this.lastNeed -= buf.length;
  }
  
  // Returns all complete UTF-8 characters in a Buffer. If the Buffer ended on a
  // partial character, the character's bytes are buffered until the required
  // number of bytes are available.
  function utf8Text(buf, i) {
    var total = utf8CheckIncomplete(this, buf, i);
    if (!this.lastNeed) return buf.toString('utf8', i);
    this.lastTotal = total;
    var end = buf.length - (total - this.lastNeed);
    buf.copy(this.lastChar, 0, end);
    return buf.toString('utf8', i, end);
  }
  
  // For UTF-8, a replacement character is added when ending on a partial
  // character.
  function utf8End(buf) {
    var r = buf && buf.length ? this.write(buf) : '';
    if (this.lastNeed) return r + '\ufffd';
    return r;
  }
  
  // UTF-16LE typically needs two bytes per character, but even if we have an even
  // number of bytes available, we need to check if we end on a leading/high
  // surrogate. In that case, we need to wait for the next two bytes in order to
  // decode the last character properly.
  function utf16Text(buf, i) {
    if ((buf.length - i) % 2 === 0) {
      var r = buf.toString('utf16le', i);
      if (r) {
        var c = r.charCodeAt(r.length - 1);
        if (c >= 0xD800 && c <= 0xDBFF) {
          this.lastNeed = 2;
          this.lastTotal = 4;
          this.lastChar[0] = buf[buf.length - 2];
          this.lastChar[1] = buf[buf.length - 1];
          return r.slice(0, -1);
        }
      }
      return r;
    }
    this.lastNeed = 1;
    this.lastTotal = 2;
    this.lastChar[0] = buf[buf.length - 1];
    return buf.toString('utf16le', i, buf.length - 1);
  }
  
  // For UTF-16LE we do not explicitly append special replacement characters if we
  // end on a partial character, we simply let v8 handle that.
  function utf16End(buf) {
    var r = buf && buf.length ? this.write(buf) : '';
    if (this.lastNeed) {
      var end = this.lastTotal - this.lastNeed;
      return r + this.lastChar.toString('utf16le', 0, end);
    }
    return r;
  }
  
  function base64Text(buf, i) {
    var n = (buf.length - i) % 3;
    if (n === 0) return buf.toString('base64', i);
    this.lastNeed = 3 - n;
    this.lastTotal = 3;
    if (n === 1) {
      this.lastChar[0] = buf[buf.length - 1];
    } else {
      this.lastChar[0] = buf[buf.length - 2];
      this.lastChar[1] = buf[buf.length - 1];
    }
    return buf.toString('base64', i, buf.length - n);
  }
  
  function base64End(buf) {
    var r = buf && buf.length ? this.write(buf) : '';
    if (this.lastNeed) return r + this.lastChar.toString('base64', 0, 3 - this.lastNeed);
    return r;
  }
  
  // Pass bytes on through for single-byte encodings (e.g. ascii, latin1, hex)
  function simpleWrite(buf) {
    return buf.toString(this.encoding);
  }
  
  function simpleEnd(buf) {
    return buf && buf.length ? this.write(buf) : '';
  }
  },{"safe-buffer":46}],48:[function(require,module,exports){
  module.exports = require('./readable').PassThrough
  
  },{"./readable":49}],49:[function(require,module,exports){
  exports = module.exports = require('./lib/_stream_readable.js');
  exports.Stream = exports;
  exports.Readable = exports;
  exports.Writable = require('./lib/_stream_writable.js');
  exports.Duplex = require('./lib/_stream_duplex.js');
  exports.Transform = require('./lib/_stream_transform.js');
  exports.PassThrough = require('./lib/_stream_passthrough.js');
  
  },{"./lib/_stream_duplex.js":38,"./lib/_stream_passthrough.js":39,"./lib/_stream_readable.js":40,"./lib/_stream_transform.js":41,"./lib/_stream_writable.js":42}],50:[function(require,module,exports){
  module.exports = require('./readable').Transform
  
  },{"./readable":49}],51:[function(require,module,exports){
  module.exports = require('./lib/_stream_writable.js');
  
  },{"./lib/_stream_writable.js":42}],52:[function(require,module,exports){
  /*! safe-buffer. MIT License. Feross Aboukhadijeh <https://feross.org/opensource> */
  /* eslint-disable node/no-deprecated-api */
  var buffer = require('buffer')
  var Buffer = buffer.Buffer
  
  // alternative to using Object.keys for old browsers
  function copyProps (src, dst) {
    for (var key in src) {
      dst[key] = src[key]
    }
  }
  if (Buffer.from && Buffer.alloc && Buffer.allocUnsafe && Buffer.allocUnsafeSlow) {
    module.exports = buffer
  } else {
    // Copy properties from require('buffer')
    copyProps(buffer, exports)
    exports.Buffer = SafeBuffer
  }
  
  function SafeBuffer (arg, encodingOrOffset, length) {
    return Buffer(arg, encodingOrOffset, length)
  }
  
  SafeBuffer.prototype = Object.create(Buffer.prototype)
  
  // Copy static methods from Buffer
  copyProps(Buffer, SafeBuffer)
  
  SafeBuffer.from = function (arg, encodingOrOffset, length) {
    if (typeof arg === 'number') {
      throw new TypeError('Argument must not be a number')
    }
    return Buffer(arg, encodingOrOffset, length)
  }
  
  SafeBuffer.alloc = function (size, fill, encoding) {
    if (typeof size !== 'number') {
      throw new TypeError('Argument must be a number')
    }
    var buf = Buffer(size)
    if (fill !== undefined) {
      if (typeof encoding === 'string') {
        buf.fill(fill, encoding)
      } else {
        buf.fill(fill)
      }
    } else {
      buf.fill(0)
    }
    return buf
  }
  
  SafeBuffer.allocUnsafe = function (size) {
    if (typeof size !== 'number') {
      throw new TypeError('Argument must be a number')
    }
    return Buffer(size)
  }
  
  SafeBuffer.allocUnsafeSlow = function (size) {
    if (typeof size !== 'number') {
      throw new TypeError('Argument must be a number')
    }
    return buffer.SlowBuffer(size)
  }
  
  },{"buffer":9}],53:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  module.exports = Stream;
  
  var EE = require('events').EventEmitter;
  var inherits = require('inherits');
  
  inherits(Stream, EE);
  Stream.Readable = require('readable-stream/readable.js');
  Stream.Writable = require('readable-stream/writable.js');
  Stream.Duplex = require('readable-stream/duplex.js');
  Stream.Transform = require('readable-stream/transform.js');
  Stream.PassThrough = require('readable-stream/passthrough.js');
  
  // Backwards-compat with node 0.4.x
  Stream.Stream = Stream;
  
  
  
  // old-style streams.  Note that the pipe method (the only relevant
  // part of this class) is overridden in the Readable class.
  
  function Stream() {
    EE.call(this);
  }
  
  Stream.prototype.pipe = function(dest, options) {
    var source = this;
  
    function ondata(chunk) {
      if (dest.writable) {
        if (false === dest.write(chunk) && source.pause) {
          source.pause();
        }
      }
    }
  
    source.on('data', ondata);
  
    function ondrain() {
      if (source.readable && source.resume) {
        source.resume();
      }
    }
  
    dest.on('drain', ondrain);
  
    // If the 'end' option is not supplied, dest.end() will be called when
    // source gets the 'end' or 'close' events.  Only dest.end() once.
    if (!dest._isStdio && (!options || options.end !== false)) {
      source.on('end', onend);
      source.on('close', onclose);
    }
  
    var didOnEnd = false;
    function onend() {
      if (didOnEnd) return;
      didOnEnd = true;
  
      dest.end();
    }
  
  
    function onclose() {
      if (didOnEnd) return;
      didOnEnd = true;
  
      if (typeof dest.destroy === 'function') dest.destroy();
    }
  
    // don't leave dangling pipes when there are errors.
    function onerror(er) {
      cleanup();
      if (EE.listenerCount(this, 'error') === 0) {
        throw er; // Unhandled stream error in pipe.
      }
    }
  
    source.on('error', onerror);
    dest.on('error', onerror);
  
    // remove all the event listeners that were added.
    function cleanup() {
      source.removeListener('data', ondata);
      dest.removeListener('drain', ondrain);
  
      source.removeListener('end', onend);
      source.removeListener('close', onclose);
  
      source.removeListener('error', onerror);
      dest.removeListener('error', onerror);
  
      source.removeListener('end', cleanup);
      source.removeListener('close', cleanup);
  
      dest.removeListener('close', cleanup);
    }
  
    source.on('end', cleanup);
    source.on('close', cleanup);
  
    dest.on('close', cleanup);
  
    dest.emit('pipe', source);
  
    // Allow for unix-like usage: A.pipe(B).pipe(C)
    return dest;
  };
  
  },{"events":12,"inherits":15,"readable-stream/duplex.js":37,"readable-stream/passthrough.js":48,"readable-stream/readable.js":49,"readable-stream/transform.js":50,"readable-stream/writable.js":51}],54:[function(require,module,exports){
  (function (global){
  var ClientRequest = require('./lib/request')
  var response = require('./lib/response')
  var extend = require('xtend')
  var statusCodes = require('builtin-status-codes')
  var url = require('url')
  
  var http = exports
  
  http.request = function (opts, cb) {
    if (typeof opts === 'string')
      opts = url.parse(opts)
    else
      opts = extend(opts)
  
    // Normally, the page is loaded from http or https, so not specifying a protocol
    // will result in a (valid) protocol-relative url. However, this won't work if
    // the protocol is something else, like 'file:'
    var defaultProtocol = global.location.protocol.search(/^https?:$/) === -1 ? 'http:' : ''
  
    var protocol = opts.protocol || defaultProtocol
    var host = opts.hostname || opts.host
    var port = opts.port
    var path = opts.path || '/'
  
    // Necessary for IPv6 addresses
    if (host && host.indexOf(':') !== -1)
      host = '[' + host + ']'
  
    // This may be a relative url. The browser should always be able to interpret it correctly.
    opts.url = (host ? (protocol + '//' + host) : '') + (port ? ':' + port : '') + path
    opts.method = (opts.method || 'GET').toUpperCase()
    opts.headers = opts.headers || {}
  
    // Also valid opts.auth, opts.mode
  
    var req = new ClientRequest(opts)
    if (cb)
      req.on('response', cb)
    return req
  }
  
  http.get = function get (opts, cb) {
    var req = http.request(opts, cb)
    req.end()
    return req
  }
  
  http.ClientRequest = ClientRequest
  http.IncomingMessage = response.IncomingMessage
  
  http.Agent = function () {}
  http.Agent.defaultMaxSockets = 4
  
  http.globalAgent = new http.Agent()
  
  http.STATUS_CODES = statusCodes
  
  http.METHODS = [
    'CHECKOUT',
    'CONNECT',
    'COPY',
    'DELETE',
    'GET',
    'HEAD',
    'LOCK',
    'M-SEARCH',
    'MERGE',
    'MKACTIVITY',
    'MKCOL',
    'MOVE',
    'NOTIFY',
    'OPTIONS',
    'PATCH',
    'POST',
    'PROPFIND',
    'PROPPATCH',
    'PURGE',
    'PUT',
    'REPORT',
    'SEARCH',
    'SUBSCRIBE',
    'TRACE',
    'UNLOCK',
    'UNSUBSCRIBE'
  ]
  }).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{"./lib/request":56,"./lib/response":57,"builtin-status-codes":10,"url":75,"xtend":81}],55:[function(require,module,exports){
  (function (global){
  exports.fetch = isFunction(global.fetch) && isFunction(global.ReadableStream)
  
  exports.writableStream = isFunction(global.WritableStream)
  
  exports.abortController = isFunction(global.AbortController)
  
  // The xhr request to example.com may violate some restrictive CSP configurations,
  // so if we're running in a browser that supports `fetch`, avoid calling getXHR()
  // and assume support for certain features below.
  var xhr
  function getXHR () {
    // Cache the xhr value
    if (xhr !== undefined) return xhr
  
    if (global.XMLHttpRequest) {
      xhr = new global.XMLHttpRequest()
      // If XDomainRequest is available (ie only, where xhr might not work
      // cross domain), use the page location. Otherwise use example.com
      // Note: this doesn't actually make an http request.
      try {
        xhr.open('GET', global.XDomainRequest ? '/' : 'https://example.com')
      } catch(e) {
        xhr = null
      }
    } else {
      // Service workers don't have XHR
      xhr = null
    }
    return xhr
  }
  
  function checkTypeSupport (type) {
    var xhr = getXHR()
    if (!xhr) return false
    try {
      xhr.responseType = type
      return xhr.responseType === type
    } catch (e) {}
    return false
  }
  
  // If fetch is supported, then arraybuffer will be supported too. Skip calling
  // checkTypeSupport(), since that calls getXHR().
  exports.arraybuffer = exports.fetch || checkTypeSupport('arraybuffer')
  
  // These next two tests unavoidably show warnings in Chrome. Since fetch will always
  // be used if it's available, just return false for these to avoid the warnings.
  exports.msstream = !exports.fetch && checkTypeSupport('ms-stream')
  exports.mozchunkedarraybuffer = !exports.fetch && checkTypeSupport('moz-chunked-arraybuffer')
  
  // If fetch is supported, then overrideMimeType will be supported too. Skip calling
  // getXHR().
  exports.overrideMimeType = exports.fetch || (getXHR() ? isFunction(getXHR().overrideMimeType) : false)
  
  function isFunction (value) {
    return typeof value === 'function'
  }
  
  xhr = null // Help gc
  
  }).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{}],56:[function(require,module,exports){
  (function (process,global,Buffer){
  var capability = require('./capability')
  var inherits = require('inherits')
  var response = require('./response')
  var stream = require('readable-stream')
  
  var IncomingMessage = response.IncomingMessage
  var rStates = response.readyStates
  
  function decideMode (preferBinary, useFetch) {
    if (capability.fetch && useFetch) {
      return 'fetch'
    } else if (capability.mozchunkedarraybuffer) {
      return 'moz-chunked-arraybuffer'
    } else if (capability.msstream) {
      return 'ms-stream'
    } else if (capability.arraybuffer && preferBinary) {
      return 'arraybuffer'
    } else {
      return 'text'
    }
  }
  
  var ClientRequest = module.exports = function (opts) {
    var self = this
    stream.Writable.call(self)
  
    self._opts = opts
    self._body = []
    self._headers = {}
    if (opts.auth)
      self.setHeader('Authorization', 'Basic ' + Buffer.from(opts.auth).toString('base64'))
    Object.keys(opts.headers).forEach(function (name) {
      self.setHeader(name, opts.headers[name])
    })
  
    var preferBinary
    var useFetch = true
    if (opts.mode === 'disable-fetch' || ('requestTimeout' in opts && !capability.abortController)) {
      // If the use of XHR should be preferred. Not typically needed.
      useFetch = false
      preferBinary = true
    } else if (opts.mode === 'prefer-streaming') {
      // If streaming is a high priority but binary compatibility and
      // the accuracy of the 'content-type' header aren't
      preferBinary = false
    } else if (opts.mode === 'allow-wrong-content-type') {
      // If streaming is more important than preserving the 'content-type' header
      preferBinary = !capability.overrideMimeType
    } else if (!opts.mode || opts.mode === 'default' || opts.mode === 'prefer-fast') {
      // Use binary if text streaming may corrupt data or the content-type header, or for speed
      preferBinary = true
    } else {
      throw new Error('Invalid value for opts.mode')
    }
    self._mode = decideMode(preferBinary, useFetch)
    self._fetchTimer = null
  
    self.on('finish', function () {
      self._onFinish()
    })
  }
  
  inherits(ClientRequest, stream.Writable)
  
  ClientRequest.prototype.setHeader = function (name, value) {
    var self = this
    var lowerName = name.toLowerCase()
    // This check is not necessary, but it prevents warnings from browsers about setting unsafe
    // headers. To be honest I'm not entirely sure hiding these warnings is a good thing, but
    // http-browserify did it, so I will too.
    if (unsafeHeaders.indexOf(lowerName) !== -1)
      return
  
    self._headers[lowerName] = {
      name: name,
      value: value
    }
  }
  
  ClientRequest.prototype.getHeader = function (name) {
    var header = this._headers[name.toLowerCase()]
    if (header)
      return header.value
    return null
  }
  
  ClientRequest.prototype.removeHeader = function (name) {
    var self = this
    delete self._headers[name.toLowerCase()]
  }
  
  ClientRequest.prototype._onFinish = function () {
    var self = this
  
    if (self._destroyed)
      return
    var opts = self._opts
  
    var headersObj = self._headers
    var body = null
    if (opts.method !== 'GET' && opts.method !== 'HEAD') {
          body = new Blob(self._body, {
              type: (headersObj['content-type'] || {}).value || ''
          });
      }
  
    // create flattened list of headers
    var headersList = []
    Object.keys(headersObj).forEach(function (keyName) {
      var name = headersObj[keyName].name
      var value = headersObj[keyName].value
      if (Array.isArray(value)) {
        value.forEach(function (v) {
          headersList.push([name, v])
        })
      } else {
        headersList.push([name, value])
      }
    })
  
    if (self._mode === 'fetch') {
      var signal = null
      if (capability.abortController) {
        var controller = new AbortController()
        signal = controller.signal
        self._fetchAbortController = controller
  
        if ('requestTimeout' in opts && opts.requestTimeout !== 0) {
          self._fetchTimer = global.setTimeout(function () {
            self.emit('requestTimeout')
            if (self._fetchAbortController)
              self._fetchAbortController.abort()
          }, opts.requestTimeout)
        }
      }
  
      global.fetch(self._opts.url, {
        method: self._opts.method,
        headers: headersList,
        body: body || undefined,
        mode: 'cors',
        credentials: opts.withCredentials ? 'include' : 'same-origin',
        signal: signal
      }).then(function (response) {
        self._fetchResponse = response
        self._connect()
      }, function (reason) {
        global.clearTimeout(self._fetchTimer)
        if (!self._destroyed)
          self.emit('error', reason)
      })
    } else {
      var xhr = self._xhr = new global.XMLHttpRequest()
      try {
        xhr.open(self._opts.method, self._opts.url, true)
      } catch (err) {
        process.nextTick(function () {
          self.emit('error', err)
        })
        return
      }
  
      // Can't set responseType on really old browsers
      if ('responseType' in xhr)
        xhr.responseType = self._mode
  
      if ('withCredentials' in xhr)
        xhr.withCredentials = !!opts.withCredentials
  
      if (self._mode === 'text' && 'overrideMimeType' in xhr)
        xhr.overrideMimeType('text/plain; charset=x-user-defined')
  
      if ('requestTimeout' in opts) {
        xhr.timeout = opts.requestTimeout
        xhr.ontimeout = function () {
          self.emit('requestTimeout')
        }
      }
  
      headersList.forEach(function (header) {
        xhr.setRequestHeader(header[0], header[1])
      })
  
      self._response = null
      xhr.onreadystatechange = function () {
        switch (xhr.readyState) {
          case rStates.LOADING:
          case rStates.DONE:
            self._onXHRProgress()
            break
        }
      }
      // Necessary for streaming in Firefox, since xhr.response is ONLY defined
      // in onprogress, not in onreadystatechange with xhr.readyState = 3
      if (self._mode === 'moz-chunked-arraybuffer') {
        xhr.onprogress = function () {
          self._onXHRProgress()
        }
      }
  
      xhr.onerror = function () {
        if (self._destroyed)
          return
        self.emit('error', new Error('XHR error'))
      }
  
      try {
        xhr.send(body)
      } catch (err) {
        process.nextTick(function () {
          self.emit('error', err)
        })
        return
      }
    }
  }
  
  /**
   * Checks if xhr.status is readable and non-zero, indicating no error.
   * Even though the spec says it should be available in readyState 3,
   * accessing it throws an exception in IE8
   */
  function statusValid (xhr) {
    try {
      var status = xhr.status
      return (status !== null && status !== 0)
    } catch (e) {
      return false
    }
  }
  
  ClientRequest.prototype._onXHRProgress = function () {
    var self = this
  
    if (!statusValid(self._xhr) || self._destroyed)
      return
  
    if (!self._response)
      self._connect()
  
    self._response._onXHRProgress()
  }
  
  ClientRequest.prototype._connect = function () {
    var self = this
  
    if (self._destroyed)
      return
  
    self._response = new IncomingMessage(self._xhr, self._fetchResponse, self._mode, self._fetchTimer)
    self._response.on('error', function(err) {
      self.emit('error', err)
    })
  
    self.emit('response', self._response)
  }
  
  ClientRequest.prototype._write = function (chunk, encoding, cb) {
    var self = this
  
    self._body.push(chunk)
    cb()
  }
  
  ClientRequest.prototype.abort = ClientRequest.prototype.destroy = function () {
    var self = this
    self._destroyed = true
    global.clearTimeout(self._fetchTimer)
    if (self._response)
      self._response._destroyed = true
    if (self._xhr)
      self._xhr.abort()
    else if (self._fetchAbortController)
      self._fetchAbortController.abort()
  }
  
  ClientRequest.prototype.end = function (data, encoding, cb) {
    var self = this
    if (typeof data === 'function') {
      cb = data
      data = undefined
    }
  
    stream.Writable.prototype.end.call(self, data, encoding, cb)
  }
  
  ClientRequest.prototype.flushHeaders = function () {}
  ClientRequest.prototype.setTimeout = function () {}
  ClientRequest.prototype.setNoDelay = function () {}
  ClientRequest.prototype.setSocketKeepAlive = function () {}
  
  // Taken from http://www.w3.org/TR/XMLHttpRequest/#the-setrequestheader%28%29-method
  var unsafeHeaders = [
    'accept-charset',
    'accept-encoding',
    'access-control-request-headers',
    'access-control-request-method',
    'connection',
    'content-length',
    'cookie',
    'cookie2',
    'date',
    'dnt',
    'expect',
    'host',
    'keep-alive',
    'origin',
    'referer',
    'te',
    'trailer',
    'transfer-encoding',
    'upgrade',
    'via'
  ]
  
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {},require("buffer").Buffer)
  },{"./capability":55,"./response":57,"_process":32,"buffer":9,"inherits":15,"readable-stream":72}],57:[function(require,module,exports){
  (function (process,global,Buffer){
  var capability = require('./capability')
  var inherits = require('inherits')
  var stream = require('readable-stream')
  
  var rStates = exports.readyStates = {
    UNSENT: 0,
    OPENED: 1,
    HEADERS_RECEIVED: 2,
    LOADING: 3,
    DONE: 4
  }
  
  var IncomingMessage = exports.IncomingMessage = function (xhr, response, mode, fetchTimer) {
    var self = this
    stream.Readable.call(self)
  
    self._mode = mode
    self.headers = {}
    self.rawHeaders = []
    self.trailers = {}
    self.rawTrailers = []
  
    // Fake the 'close' event, but only once 'end' fires
    self.on('end', function () {
      // The nextTick is necessary to prevent the 'request' module from causing an infinite loop
      process.nextTick(function () {
        self.emit('close')
      })
    })
  
    if (mode === 'fetch') {
      self._fetchResponse = response
  
      self.url = response.url
      self.statusCode = response.status
      self.statusMessage = response.statusText
      
      response.headers.forEach(function (header, key){
        self.headers[key.toLowerCase()] = header
        self.rawHeaders.push(key, header)
      })
  
      if (capability.writableStream) {
        var writable = new WritableStream({
          write: function (chunk) {
            return new Promise(function (resolve, reject) {
              if (self._destroyed) {
                reject()
              } else if(self.push(Buffer.from(chunk))) {
                resolve()
              } else {
                self._resumeFetch = resolve
              }
            })
          },
          close: function () {
            global.clearTimeout(fetchTimer)
            if (!self._destroyed)
              self.push(null)
          },
          abort: function (err) {
            if (!self._destroyed)
              self.emit('error', err)
          }
        })
  
        try {
          response.body.pipeTo(writable).catch(function (err) {
            global.clearTimeout(fetchTimer)
            if (!self._destroyed)
              self.emit('error', err)
          })
          return
        } catch (e) {} // pipeTo method isn't defined. Can't find a better way to feature test this
      }
      // fallback for when writableStream or pipeTo aren't available
      var reader = response.body.getReader()
      function read () {
        reader.read().then(function (result) {
          if (self._destroyed)
            return
          if (result.done) {
            global.clearTimeout(fetchTimer)
            self.push(null)
            return
          }
          self.push(Buffer.from(result.value))
          read()
        }).catch(function (err) {
          global.clearTimeout(fetchTimer)
          if (!self._destroyed)
            self.emit('error', err)
        })
      }
      read()
    } else {
      self._xhr = xhr
      self._pos = 0
  
      self.url = xhr.responseURL
      self.statusCode = xhr.status
      self.statusMessage = xhr.statusText
      var headers = xhr.getAllResponseHeaders().split(/\r?\n/)
      headers.forEach(function (header) {
        var matches = header.match(/^([^:]+):\s*(.*)/)
        if (matches) {
          var key = matches[1].toLowerCase()
          if (key === 'set-cookie') {
            if (self.headers[key] === undefined) {
              self.headers[key] = []
            }
            self.headers[key].push(matches[2])
          } else if (self.headers[key] !== undefined) {
            self.headers[key] += ', ' + matches[2]
          } else {
            self.headers[key] = matches[2]
          }
          self.rawHeaders.push(matches[1], matches[2])
        }
      })
  
      self._charset = 'x-user-defined'
      if (!capability.overrideMimeType) {
        var mimeType = self.rawHeaders['mime-type']
        if (mimeType) {
          var charsetMatch = mimeType.match(/;\s*charset=([^;])(;|$)/)
          if (charsetMatch) {
            self._charset = charsetMatch[1].toLowerCase()
          }
        }
        if (!self._charset)
          self._charset = 'utf-8' // best guess
      }
    }
  }
  
  inherits(IncomingMessage, stream.Readable)
  
  IncomingMessage.prototype._read = function () {
    var self = this
  
    var resolve = self._resumeFetch
    if (resolve) {
      self._resumeFetch = null
      resolve()
    }
  }
  
  IncomingMessage.prototype._onXHRProgress = function () {
    var self = this
  
    var xhr = self._xhr
  
    var response = null
    switch (self._mode) {
      case 'text':
        response = xhr.responseText
        if (response.length > self._pos) {
          var newData = response.substr(self._pos)
          if (self._charset === 'x-user-defined') {
            var buffer = Buffer.alloc(newData.length)
            for (var i = 0; i < newData.length; i++)
              buffer[i] = newData.charCodeAt(i) & 0xff
  
            self.push(buffer)
          } else {
            self.push(newData, self._charset)
          }
          self._pos = response.length
        }
        break
      case 'arraybuffer':
        if (xhr.readyState !== rStates.DONE || !xhr.response)
          break
        response = xhr.response
        self.push(Buffer.from(new Uint8Array(response)))
        break
      case 'moz-chunked-arraybuffer': // take whole
        response = xhr.response
        if (xhr.readyState !== rStates.LOADING || !response)
          break
        self.push(Buffer.from(new Uint8Array(response)))
        break
      case 'ms-stream':
        response = xhr.response
        if (xhr.readyState !== rStates.LOADING)
          break
        var reader = new global.MSStreamReader()
        reader.onprogress = function () {
          if (reader.result.byteLength > self._pos) {
            self.push(Buffer.from(new Uint8Array(reader.result.slice(self._pos))))
            self._pos = reader.result.byteLength
          }
        }
        reader.onload = function () {
          self.push(null)
        }
        // reader.onerror = ??? // TODO: this
        reader.readAsArrayBuffer(response)
        break
    }
  
    // The ms-stream case handles end separately in reader.onload()
    if (self._xhr.readyState === rStates.DONE && self._mode !== 'ms-stream') {
      self.push(null)
    }
  }
  
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {},require("buffer").Buffer)
  },{"./capability":55,"_process":32,"buffer":9,"inherits":15,"readable-stream":72}],58:[function(require,module,exports){
  'use strict';
  
  function _inheritsLoose(subClass, superClass) { subClass.prototype = Object.create(superClass.prototype); subClass.prototype.constructor = subClass; subClass.__proto__ = superClass; }
  
  var codes = {};
  
  function createErrorType(code, message, Base) {
    if (!Base) {
      Base = Error;
    }
  
    function getMessage(arg1, arg2, arg3) {
      if (typeof message === 'string') {
        return message;
      } else {
        return message(arg1, arg2, arg3);
      }
    }
  
    var NodeError =
    /*#__PURE__*/
    function (_Base) {
      _inheritsLoose(NodeError, _Base);
  
      function NodeError(arg1, arg2, arg3) {
        return _Base.call(this, getMessage(arg1, arg2, arg3)) || this;
      }
  
      return NodeError;
    }(Base);
  
    NodeError.prototype.name = Base.name;
    NodeError.prototype.code = code;
    codes[code] = NodeError;
  } // https://github.com/nodejs/node/blob/v10.8.0/lib/internal/errors.js
  
  
  function oneOf(expected, thing) {
    if (Array.isArray(expected)) {
      var len = expected.length;
      expected = expected.map(function (i) {
        return String(i);
      });
  
      if (len > 2) {
        return "one of ".concat(thing, " ").concat(expected.slice(0, len - 1).join(', '), ", or ") + expected[len - 1];
      } else if (len === 2) {
        return "one of ".concat(thing, " ").concat(expected[0], " or ").concat(expected[1]);
      } else {
        return "of ".concat(thing, " ").concat(expected[0]);
      }
    } else {
      return "of ".concat(thing, " ").concat(String(expected));
    }
  } // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/startsWith
  
  
  function startsWith(str, search, pos) {
    return str.substr(!pos || pos < 0 ? 0 : +pos, search.length) === search;
  } // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/endsWith
  
  
  function endsWith(str, search, this_len) {
    if (this_len === undefined || this_len > str.length) {
      this_len = str.length;
    }
  
    return str.substring(this_len - search.length, this_len) === search;
  } // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/includes
  
  
  function includes(str, search, start) {
    if (typeof start !== 'number') {
      start = 0;
    }
  
    if (start + search.length > str.length) {
      return false;
    } else {
      return str.indexOf(search, start) !== -1;
    }
  }
  
  createErrorType('ERR_INVALID_OPT_VALUE', function (name, value) {
    return 'The value "' + value + '" is invalid for option "' + name + '"';
  }, TypeError);
  createErrorType('ERR_INVALID_ARG_TYPE', function (name, expected, actual) {
    // determiner: 'must be' or 'must not be'
    var determiner;
  
    if (typeof expected === 'string' && startsWith(expected, 'not ')) {
      determiner = 'must not be';
      expected = expected.replace(/^not /, '');
    } else {
      determiner = 'must be';
    }
  
    var msg;
  
    if (endsWith(name, ' argument')) {
      // For cases like 'first argument'
      msg = "The ".concat(name, " ").concat(determiner, " ").concat(oneOf(expected, 'type'));
    } else {
      var type = includes(name, '.') ? 'property' : 'argument';
      msg = "The \"".concat(name, "\" ").concat(type, " ").concat(determiner, " ").concat(oneOf(expected, 'type'));
    }
  
    msg += ". Received type ".concat(typeof actual);
    return msg;
  }, TypeError);
  createErrorType('ERR_STREAM_PUSH_AFTER_EOF', 'stream.push() after EOF');
  createErrorType('ERR_METHOD_NOT_IMPLEMENTED', function (name) {
    return 'The ' + name + ' method is not implemented';
  });
  createErrorType('ERR_STREAM_PREMATURE_CLOSE', 'Premature close');
  createErrorType('ERR_STREAM_DESTROYED', function (name) {
    return 'Cannot call ' + name + ' after a stream was destroyed';
  });
  createErrorType('ERR_MULTIPLE_CALLBACK', 'Callback called multiple times');
  createErrorType('ERR_STREAM_CANNOT_PIPE', 'Cannot pipe, not readable');
  createErrorType('ERR_STREAM_WRITE_AFTER_END', 'write after end');
  createErrorType('ERR_STREAM_NULL_VALUES', 'May not write null values to stream', TypeError);
  createErrorType('ERR_UNKNOWN_ENCODING', function (arg) {
    return 'Unknown encoding: ' + arg;
  }, TypeError);
  createErrorType('ERR_STREAM_UNSHIFT_AFTER_END_EVENT', 'stream.unshift() after end event');
  module.exports.codes = codes;
  
  },{}],59:[function(require,module,exports){
  (function (process){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  // a duplex stream is just a stream that is both readable and writable.
  // Since JS doesn't have multiple prototypal inheritance, this class
  // prototypally inherits from Readable, and then parasitically from
  // Writable.
  'use strict';
  /*<replacement>*/
  
  var objectKeys = Object.keys || function (obj) {
    var keys = [];
  
    for (var key in obj) {
      keys.push(key);
    }
  
    return keys;
  };
  /*</replacement>*/
  
  
  module.exports = Duplex;
  
  var Readable = require('./_stream_readable');
  
  var Writable = require('./_stream_writable');
  
  require('inherits')(Duplex, Readable);
  
  {
    // Allow the keys array to be GC'ed.
    var keys = objectKeys(Writable.prototype);
  
    for (var v = 0; v < keys.length; v++) {
      var method = keys[v];
      if (!Duplex.prototype[method]) Duplex.prototype[method] = Writable.prototype[method];
    }
  }
  
  function Duplex(options) {
    if (!(this instanceof Duplex)) return new Duplex(options);
    Readable.call(this, options);
    Writable.call(this, options);
    this.allowHalfOpen = true;
  
    if (options) {
      if (options.readable === false) this.readable = false;
      if (options.writable === false) this.writable = false;
  
      if (options.allowHalfOpen === false) {
        this.allowHalfOpen = false;
        this.once('end', onend);
      }
    }
  }
  
  Object.defineProperty(Duplex.prototype, 'writableHighWaterMark', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._writableState.highWaterMark;
    }
  });
  Object.defineProperty(Duplex.prototype, 'writableBuffer', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._writableState && this._writableState.getBuffer();
    }
  });
  Object.defineProperty(Duplex.prototype, 'writableLength', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._writableState.length;
    }
  }); // the no-half-open enforcer
  
  function onend() {
    // If the writable side ended, then we're ok.
    if (this._writableState.ended) return; // no more data can be written.
    // But allow more writes to happen in this tick.
  
    process.nextTick(onEndNT, this);
  }
  
  function onEndNT(self) {
    self.end();
  }
  
  Object.defineProperty(Duplex.prototype, 'destroyed', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      if (this._readableState === undefined || this._writableState === undefined) {
        return false;
      }
  
      return this._readableState.destroyed && this._writableState.destroyed;
    },
    set: function set(value) {
      // we ignore the value if the stream
      // has not been initialized yet
      if (this._readableState === undefined || this._writableState === undefined) {
        return;
      } // backward compatibility, the user is explicitly
      // managing destroyed
  
  
      this._readableState.destroyed = value;
      this._writableState.destroyed = value;
    }
  });
  }).call(this,require('_process'))
  },{"./_stream_readable":61,"./_stream_writable":63,"_process":32,"inherits":15}],60:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  // a passthrough stream.
  // basically just the most minimal sort of Transform stream.
  // Every written chunk gets output as-is.
  'use strict';
  
  module.exports = PassThrough;
  
  var Transform = require('./_stream_transform');
  
  require('inherits')(PassThrough, Transform);
  
  function PassThrough(options) {
    if (!(this instanceof PassThrough)) return new PassThrough(options);
    Transform.call(this, options);
  }
  
  PassThrough.prototype._transform = function (chunk, encoding, cb) {
    cb(null, chunk);
  };
  },{"./_stream_transform":62,"inherits":15}],61:[function(require,module,exports){
  (function (process,global){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  'use strict';
  
  module.exports = Readable;
  /*<replacement>*/
  
  var Duplex;
  /*</replacement>*/
  
  Readable.ReadableState = ReadableState;
  /*<replacement>*/
  
  var EE = require('events').EventEmitter;
  
  var EElistenerCount = function EElistenerCount(emitter, type) {
    return emitter.listeners(type).length;
  };
  /*</replacement>*/
  
  /*<replacement>*/
  
  
  var Stream = require('./internal/streams/stream');
  /*</replacement>*/
  
  
  var Buffer = require('buffer').Buffer;
  
  var OurUint8Array = global.Uint8Array || function () {};
  
  function _uint8ArrayToBuffer(chunk) {
    return Buffer.from(chunk);
  }
  
  function _isUint8Array(obj) {
    return Buffer.isBuffer(obj) || obj instanceof OurUint8Array;
  }
  /*<replacement>*/
  
  
  var debugUtil = require('util');
  
  var debug;
  
  if (debugUtil && debugUtil.debuglog) {
    debug = debugUtil.debuglog('stream');
  } else {
    debug = function debug() {};
  }
  /*</replacement>*/
  
  
  var BufferList = require('./internal/streams/buffer_list');
  
  var destroyImpl = require('./internal/streams/destroy');
  
  var _require = require('./internal/streams/state'),
      getHighWaterMark = _require.getHighWaterMark;
  
  var _require$codes = require('../errors').codes,
      ERR_INVALID_ARG_TYPE = _require$codes.ERR_INVALID_ARG_TYPE,
      ERR_STREAM_PUSH_AFTER_EOF = _require$codes.ERR_STREAM_PUSH_AFTER_EOF,
      ERR_METHOD_NOT_IMPLEMENTED = _require$codes.ERR_METHOD_NOT_IMPLEMENTED,
      ERR_STREAM_UNSHIFT_AFTER_END_EVENT = _require$codes.ERR_STREAM_UNSHIFT_AFTER_END_EVENT; // Lazy loaded to improve the startup performance.
  
  
  var StringDecoder;
  var createReadableStreamAsyncIterator;
  var from;
  
  require('inherits')(Readable, Stream);
  
  var errorOrDestroy = destroyImpl.errorOrDestroy;
  var kProxyEvents = ['error', 'close', 'destroy', 'pause', 'resume'];
  
  function prependListener(emitter, event, fn) {
    // Sadly this is not cacheable as some libraries bundle their own
    // event emitter implementation with them.
    if (typeof emitter.prependListener === 'function') return emitter.prependListener(event, fn); // This is a hack to make sure that our error handler is attached before any
    // userland ones.  NEVER DO THIS. This is here only because this code needs
    // to continue to work with older versions of Node.js that do not include
    // the prependListener() method. The goal is to eventually remove this hack.
  
    if (!emitter._events || !emitter._events[event]) emitter.on(event, fn);else if (Array.isArray(emitter._events[event])) emitter._events[event].unshift(fn);else emitter._events[event] = [fn, emitter._events[event]];
  }
  
  function ReadableState(options, stream, isDuplex) {
    Duplex = Duplex || require('./_stream_duplex');
    options = options || {}; // Duplex streams are both readable and writable, but share
    // the same options object.
    // However, some cases require setting options to different
    // values for the readable and the writable sides of the duplex stream.
    // These options can be provided separately as readableXXX and writableXXX.
  
    if (typeof isDuplex !== 'boolean') isDuplex = stream instanceof Duplex; // object stream flag. Used to make read(n) ignore n and to
    // make all the buffer merging and length checks go away
  
    this.objectMode = !!options.objectMode;
    if (isDuplex) this.objectMode = this.objectMode || !!options.readableObjectMode; // the point at which it stops calling _read() to fill the buffer
    // Note: 0 is a valid value, means "don't call _read preemptively ever"
  
    this.highWaterMark = getHighWaterMark(this, options, 'readableHighWaterMark', isDuplex); // A linked list is used to store data chunks instead of an array because the
    // linked list can remove elements from the beginning faster than
    // array.shift()
  
    this.buffer = new BufferList();
    this.length = 0;
    this.pipes = null;
    this.pipesCount = 0;
    this.flowing = null;
    this.ended = false;
    this.endEmitted = false;
    this.reading = false; // a flag to be able to tell if the event 'readable'/'data' is emitted
    // immediately, or on a later tick.  We set this to true at first, because
    // any actions that shouldn't happen until "later" should generally also
    // not happen before the first read call.
  
    this.sync = true; // whenever we return null, then we set a flag to say
    // that we're awaiting a 'readable' event emission.
  
    this.needReadable = false;
    this.emittedReadable = false;
    this.readableListening = false;
    this.resumeScheduled = false;
    this.paused = true; // Should close be emitted on destroy. Defaults to true.
  
    this.emitClose = options.emitClose !== false; // Should .destroy() be called after 'end' (and potentially 'finish')
  
    this.autoDestroy = !!options.autoDestroy; // has it been destroyed
  
    this.destroyed = false; // Crypto is kind of old and crusty.  Historically, its default string
    // encoding is 'binary' so we have to make this configurable.
    // Everything else in the universe uses 'utf8', though.
  
    this.defaultEncoding = options.defaultEncoding || 'utf8'; // the number of writers that are awaiting a drain event in .pipe()s
  
    this.awaitDrain = 0; // if true, a maybeReadMore has been scheduled
  
    this.readingMore = false;
    this.decoder = null;
    this.encoding = null;
  
    if (options.encoding) {
      if (!StringDecoder) StringDecoder = require('string_decoder/').StringDecoder;
      this.decoder = new StringDecoder(options.encoding);
      this.encoding = options.encoding;
    }
  }
  
  function Readable(options) {
    Duplex = Duplex || require('./_stream_duplex');
    if (!(this instanceof Readable)) return new Readable(options); // Checking for a Stream.Duplex instance is faster here instead of inside
    // the ReadableState constructor, at least with V8 6.5
  
    var isDuplex = this instanceof Duplex;
    this._readableState = new ReadableState(options, this, isDuplex); // legacy
  
    this.readable = true;
  
    if (options) {
      if (typeof options.read === 'function') this._read = options.read;
      if (typeof options.destroy === 'function') this._destroy = options.destroy;
    }
  
    Stream.call(this);
  }
  
  Object.defineProperty(Readable.prototype, 'destroyed', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      if (this._readableState === undefined) {
        return false;
      }
  
      return this._readableState.destroyed;
    },
    set: function set(value) {
      // we ignore the value if the stream
      // has not been initialized yet
      if (!this._readableState) {
        return;
      } // backward compatibility, the user is explicitly
      // managing destroyed
  
  
      this._readableState.destroyed = value;
    }
  });
  Readable.prototype.destroy = destroyImpl.destroy;
  Readable.prototype._undestroy = destroyImpl.undestroy;
  
  Readable.prototype._destroy = function (err, cb) {
    cb(err);
  }; // Manually shove something into the read() buffer.
  // This returns true if the highWaterMark has not been hit yet,
  // similar to how Writable.write() returns true if you should
  // write() some more.
  
  
  Readable.prototype.push = function (chunk, encoding) {
    var state = this._readableState;
    var skipChunkCheck;
  
    if (!state.objectMode) {
      if (typeof chunk === 'string') {
        encoding = encoding || state.defaultEncoding;
  
        if (encoding !== state.encoding) {
          chunk = Buffer.from(chunk, encoding);
          encoding = '';
        }
  
        skipChunkCheck = true;
      }
    } else {
      skipChunkCheck = true;
    }
  
    return readableAddChunk(this, chunk, encoding, false, skipChunkCheck);
  }; // Unshift should *always* be something directly out of read()
  
  
  Readable.prototype.unshift = function (chunk) {
    return readableAddChunk(this, chunk, null, true, false);
  };
  
  function readableAddChunk(stream, chunk, encoding, addToFront, skipChunkCheck) {
    debug('readableAddChunk', chunk);
    var state = stream._readableState;
  
    if (chunk === null) {
      state.reading = false;
      onEofChunk(stream, state);
    } else {
      var er;
      if (!skipChunkCheck) er = chunkInvalid(state, chunk);
  
      if (er) {
        errorOrDestroy(stream, er);
      } else if (state.objectMode || chunk && chunk.length > 0) {
        if (typeof chunk !== 'string' && !state.objectMode && Object.getPrototypeOf(chunk) !== Buffer.prototype) {
          chunk = _uint8ArrayToBuffer(chunk);
        }
  
        if (addToFront) {
          if (state.endEmitted) errorOrDestroy(stream, new ERR_STREAM_UNSHIFT_AFTER_END_EVENT());else addChunk(stream, state, chunk, true);
        } else if (state.ended) {
          errorOrDestroy(stream, new ERR_STREAM_PUSH_AFTER_EOF());
        } else if (state.destroyed) {
          return false;
        } else {
          state.reading = false;
  
          if (state.decoder && !encoding) {
            chunk = state.decoder.write(chunk);
            if (state.objectMode || chunk.length !== 0) addChunk(stream, state, chunk, false);else maybeReadMore(stream, state);
          } else {
            addChunk(stream, state, chunk, false);
          }
        }
      } else if (!addToFront) {
        state.reading = false;
        maybeReadMore(stream, state);
      }
    } // We can push more data if we are below the highWaterMark.
    // Also, if we have no data yet, we can stand some more bytes.
    // This is to work around cases where hwm=0, such as the repl.
  
  
    return !state.ended && (state.length < state.highWaterMark || state.length === 0);
  }
  
  function addChunk(stream, state, chunk, addToFront) {
    if (state.flowing && state.length === 0 && !state.sync) {
      state.awaitDrain = 0;
      stream.emit('data', chunk);
    } else {
      // update the buffer info.
      state.length += state.objectMode ? 1 : chunk.length;
      if (addToFront) state.buffer.unshift(chunk);else state.buffer.push(chunk);
      if (state.needReadable) emitReadable(stream);
    }
  
    maybeReadMore(stream, state);
  }
  
  function chunkInvalid(state, chunk) {
    var er;
  
    if (!_isUint8Array(chunk) && typeof chunk !== 'string' && chunk !== undefined && !state.objectMode) {
      er = new ERR_INVALID_ARG_TYPE('chunk', ['string', 'Buffer', 'Uint8Array'], chunk);
    }
  
    return er;
  }
  
  Readable.prototype.isPaused = function () {
    return this._readableState.flowing === false;
  }; // backwards compatibility.
  
  
  Readable.prototype.setEncoding = function (enc) {
    if (!StringDecoder) StringDecoder = require('string_decoder/').StringDecoder;
    var decoder = new StringDecoder(enc);
    this._readableState.decoder = decoder; // If setEncoding(null), decoder.encoding equals utf8
  
    this._readableState.encoding = this._readableState.decoder.encoding; // Iterate over current buffer to convert already stored Buffers:
  
    var p = this._readableState.buffer.head;
    var content = '';
  
    while (p !== null) {
      content += decoder.write(p.data);
      p = p.next;
    }
  
    this._readableState.buffer.clear();
  
    if (content !== '') this._readableState.buffer.push(content);
    this._readableState.length = content.length;
    return this;
  }; // Don't raise the hwm > 1GB
  
  
  var MAX_HWM = 0x40000000;
  
  function computeNewHighWaterMark(n) {
    if (n >= MAX_HWM) {
      // TODO(ronag): Throw ERR_VALUE_OUT_OF_RANGE.
      n = MAX_HWM;
    } else {
      // Get the next highest power of 2 to prevent increasing hwm excessively in
      // tiny amounts
      n--;
      n |= n >>> 1;
      n |= n >>> 2;
      n |= n >>> 4;
      n |= n >>> 8;
      n |= n >>> 16;
      n++;
    }
  
    return n;
  } // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  
  
  function howMuchToRead(n, state) {
    if (n <= 0 || state.length === 0 && state.ended) return 0;
    if (state.objectMode) return 1;
  
    if (n !== n) {
      // Only flow one buffer at a time
      if (state.flowing && state.length) return state.buffer.head.data.length;else return state.length;
    } // If we're asking for more than the current hwm, then raise the hwm.
  
  
    if (n > state.highWaterMark) state.highWaterMark = computeNewHighWaterMark(n);
    if (n <= state.length) return n; // Don't have enough
  
    if (!state.ended) {
      state.needReadable = true;
      return 0;
    }
  
    return state.length;
  } // you can override either this method, or the async _read(n) below.
  
  
  Readable.prototype.read = function (n) {
    debug('read', n);
    n = parseInt(n, 10);
    var state = this._readableState;
    var nOrig = n;
    if (n !== 0) state.emittedReadable = false; // if we're doing read(0) to trigger a readable event, but we
    // already have a bunch of data in the buffer, then just trigger
    // the 'readable' event and move on.
  
    if (n === 0 && state.needReadable && ((state.highWaterMark !== 0 ? state.length >= state.highWaterMark : state.length > 0) || state.ended)) {
      debug('read: emitReadable', state.length, state.ended);
      if (state.length === 0 && state.ended) endReadable(this);else emitReadable(this);
      return null;
    }
  
    n = howMuchToRead(n, state); // if we've ended, and we're now clear, then finish it up.
  
    if (n === 0 && state.ended) {
      if (state.length === 0) endReadable(this);
      return null;
    } // All the actual chunk generation logic needs to be
    // *below* the call to _read.  The reason is that in certain
    // synthetic stream cases, such as passthrough streams, _read
    // may be a completely synchronous operation which may change
    // the state of the read buffer, providing enough data when
    // before there was *not* enough.
    //
    // So, the steps are:
    // 1. Figure out what the state of things will be after we do
    // a read from the buffer.
    //
    // 2. If that resulting state will trigger a _read, then call _read.
    // Note that this may be asynchronous, or synchronous.  Yes, it is
    // deeply ugly to write APIs this way, but that still doesn't mean
    // that the Readable class should behave improperly, as streams are
    // designed to be sync/async agnostic.
    // Take note if the _read call is sync or async (ie, if the read call
    // has returned yet), so that we know whether or not it's safe to emit
    // 'readable' etc.
    //
    // 3. Actually pull the requested chunks out of the buffer and return.
    // if we need a readable event, then we need to do some reading.
  
  
    var doRead = state.needReadable;
    debug('need readable', doRead); // if we currently have less than the highWaterMark, then also read some
  
    if (state.length === 0 || state.length - n < state.highWaterMark) {
      doRead = true;
      debug('length less than watermark', doRead);
    } // however, if we've ended, then there's no point, and if we're already
    // reading, then it's unnecessary.
  
  
    if (state.ended || state.reading) {
      doRead = false;
      debug('reading or ended', doRead);
    } else if (doRead) {
      debug('do read');
      state.reading = true;
      state.sync = true; // if the length is currently zero, then we *need* a readable event.
  
      if (state.length === 0) state.needReadable = true; // call internal read method
  
      this._read(state.highWaterMark);
  
      state.sync = false; // If _read pushed data synchronously, then `reading` will be false,
      // and we need to re-evaluate how much data we can return to the user.
  
      if (!state.reading) n = howMuchToRead(nOrig, state);
    }
  
    var ret;
    if (n > 0) ret = fromList(n, state);else ret = null;
  
    if (ret === null) {
      state.needReadable = state.length <= state.highWaterMark;
      n = 0;
    } else {
      state.length -= n;
      state.awaitDrain = 0;
    }
  
    if (state.length === 0) {
      // If we have nothing in the buffer, then we want to know
      // as soon as we *do* get something into the buffer.
      if (!state.ended) state.needReadable = true; // If we tried to read() past the EOF, then emit end on the next tick.
  
      if (nOrig !== n && state.ended) endReadable(this);
    }
  
    if (ret !== null) this.emit('data', ret);
    return ret;
  };
  
  function onEofChunk(stream, state) {
    debug('onEofChunk');
    if (state.ended) return;
  
    if (state.decoder) {
      var chunk = state.decoder.end();
  
      if (chunk && chunk.length) {
        state.buffer.push(chunk);
        state.length += state.objectMode ? 1 : chunk.length;
      }
    }
  
    state.ended = true;
  
    if (state.sync) {
      // if we are sync, wait until next tick to emit the data.
      // Otherwise we risk emitting data in the flow()
      // the readable code triggers during a read() call
      emitReadable(stream);
    } else {
      // emit 'readable' now to make sure it gets picked up.
      state.needReadable = false;
  
      if (!state.emittedReadable) {
        state.emittedReadable = true;
        emitReadable_(stream);
      }
    }
  } // Don't emit readable right away in sync mode, because this can trigger
  // another read() call => stack overflow.  This way, it might trigger
  // a nextTick recursion warning, but that's not so bad.
  
  
  function emitReadable(stream) {
    var state = stream._readableState;
    debug('emitReadable', state.needReadable, state.emittedReadable);
    state.needReadable = false;
  
    if (!state.emittedReadable) {
      debug('emitReadable', state.flowing);
      state.emittedReadable = true;
      process.nextTick(emitReadable_, stream);
    }
  }
  
  function emitReadable_(stream) {
    var state = stream._readableState;
    debug('emitReadable_', state.destroyed, state.length, state.ended);
  
    if (!state.destroyed && (state.length || state.ended)) {
      stream.emit('readable');
      state.emittedReadable = false;
    } // The stream needs another readable event if
    // 1. It is not flowing, as the flow mechanism will take
    //    care of it.
    // 2. It is not ended.
    // 3. It is below the highWaterMark, so we can schedule
    //    another readable later.
  
  
    state.needReadable = !state.flowing && !state.ended && state.length <= state.highWaterMark;
    flow(stream);
  } // at this point, the user has presumably seen the 'readable' event,
  // and called read() to consume some data.  that may have triggered
  // in turn another _read(n) call, in which case reading = true if
  // it's in progress.
  // However, if we're not ended, or reading, and the length < hwm,
  // then go ahead and try to read some more preemptively.
  
  
  function maybeReadMore(stream, state) {
    if (!state.readingMore) {
      state.readingMore = true;
      process.nextTick(maybeReadMore_, stream, state);
    }
  }
  
  function maybeReadMore_(stream, state) {
    // Attempt to read more data if we should.
    //
    // The conditions for reading more data are (one of):
    // - Not enough data buffered (state.length < state.highWaterMark). The loop
    //   is responsible for filling the buffer with enough data if such data
    //   is available. If highWaterMark is 0 and we are not in the flowing mode
    //   we should _not_ attempt to buffer any extra data. We'll get more data
    //   when the stream consumer calls read() instead.
    // - No data in the buffer, and the stream is in flowing mode. In this mode
    //   the loop below is responsible for ensuring read() is called. Failing to
    //   call read here would abort the flow and there's no other mechanism for
    //   continuing the flow if the stream consumer has just subscribed to the
    //   'data' event.
    //
    // In addition to the above conditions to keep reading data, the following
    // conditions prevent the data from being read:
    // - The stream has ended (state.ended).
    // - There is already a pending 'read' operation (state.reading). This is a
    //   case where the the stream has called the implementation defined _read()
    //   method, but they are processing the call asynchronously and have _not_
    //   called push() with new data. In this case we skip performing more
    //   read()s. The execution ends in this method again after the _read() ends
    //   up calling push() with more data.
    while (!state.reading && !state.ended && (state.length < state.highWaterMark || state.flowing && state.length === 0)) {
      var len = state.length;
      debug('maybeReadMore read 0');
      stream.read(0);
      if (len === state.length) // didn't get any data, stop spinning.
        break;
    }
  
    state.readingMore = false;
  } // abstract method.  to be overridden in specific implementation classes.
  // call cb(er, data) where data is <= n in length.
  // for virtual (non-string, non-buffer) streams, "length" is somewhat
  // arbitrary, and perhaps not very meaningful.
  
  
  Readable.prototype._read = function (n) {
    errorOrDestroy(this, new ERR_METHOD_NOT_IMPLEMENTED('_read()'));
  };
  
  Readable.prototype.pipe = function (dest, pipeOpts) {
    var src = this;
    var state = this._readableState;
  
    switch (state.pipesCount) {
      case 0:
        state.pipes = dest;
        break;
  
      case 1:
        state.pipes = [state.pipes, dest];
        break;
  
      default:
        state.pipes.push(dest);
        break;
    }
  
    state.pipesCount += 1;
    debug('pipe count=%d opts=%j', state.pipesCount, pipeOpts);
    var doEnd = (!pipeOpts || pipeOpts.end !== false) && dest !== process.stdout && dest !== process.stderr;
    var endFn = doEnd ? onend : unpipe;
    if (state.endEmitted) process.nextTick(endFn);else src.once('end', endFn);
    dest.on('unpipe', onunpipe);
  
    function onunpipe(readable, unpipeInfo) {
      debug('onunpipe');
  
      if (readable === src) {
        if (unpipeInfo && unpipeInfo.hasUnpiped === false) {
          unpipeInfo.hasUnpiped = true;
          cleanup();
        }
      }
    }
  
    function onend() {
      debug('onend');
      dest.end();
    } // when the dest drains, it reduces the awaitDrain counter
    // on the source.  This would be more elegant with a .once()
    // handler in flow(), but adding and removing repeatedly is
    // too slow.
  
  
    var ondrain = pipeOnDrain(src);
    dest.on('drain', ondrain);
    var cleanedUp = false;
  
    function cleanup() {
      debug('cleanup'); // cleanup event handlers once the pipe is broken
  
      dest.removeListener('close', onclose);
      dest.removeListener('finish', onfinish);
      dest.removeListener('drain', ondrain);
      dest.removeListener('error', onerror);
      dest.removeListener('unpipe', onunpipe);
      src.removeListener('end', onend);
      src.removeListener('end', unpipe);
      src.removeListener('data', ondata);
      cleanedUp = true; // if the reader is waiting for a drain event from this
      // specific writer, then it would cause it to never start
      // flowing again.
      // So, if this is awaiting a drain, then we just call it now.
      // If we don't know, then assume that we are waiting for one.
  
      if (state.awaitDrain && (!dest._writableState || dest._writableState.needDrain)) ondrain();
    }
  
    src.on('data', ondata);
  
    function ondata(chunk) {
      debug('ondata');
      var ret = dest.write(chunk);
      debug('dest.write', ret);
  
      if (ret === false) {
        // If the user unpiped during `dest.write()`, it is possible
        // to get stuck in a permanently paused state if that write
        // also returned false.
        // => Check whether `dest` is still a piping destination.
        if ((state.pipesCount === 1 && state.pipes === dest || state.pipesCount > 1 && indexOf(state.pipes, dest) !== -1) && !cleanedUp) {
          debug('false write response, pause', state.awaitDrain);
          state.awaitDrain++;
        }
  
        src.pause();
      }
    } // if the dest has an error, then stop piping into it.
    // however, don't suppress the throwing behavior for this.
  
  
    function onerror(er) {
      debug('onerror', er);
      unpipe();
      dest.removeListener('error', onerror);
      if (EElistenerCount(dest, 'error') === 0) errorOrDestroy(dest, er);
    } // Make sure our error handler is attached before userland ones.
  
  
    prependListener(dest, 'error', onerror); // Both close and finish should trigger unpipe, but only once.
  
    function onclose() {
      dest.removeListener('finish', onfinish);
      unpipe();
    }
  
    dest.once('close', onclose);
  
    function onfinish() {
      debug('onfinish');
      dest.removeListener('close', onclose);
      unpipe();
    }
  
    dest.once('finish', onfinish);
  
    function unpipe() {
      debug('unpipe');
      src.unpipe(dest);
    } // tell the dest that it's being piped to
  
  
    dest.emit('pipe', src); // start the flow if it hasn't been started already.
  
    if (!state.flowing) {
      debug('pipe resume');
      src.resume();
    }
  
    return dest;
  };
  
  function pipeOnDrain(src) {
    return function pipeOnDrainFunctionResult() {
      var state = src._readableState;
      debug('pipeOnDrain', state.awaitDrain);
      if (state.awaitDrain) state.awaitDrain--;
  
      if (state.awaitDrain === 0 && EElistenerCount(src, 'data')) {
        state.flowing = true;
        flow(src);
      }
    };
  }
  
  Readable.prototype.unpipe = function (dest) {
    var state = this._readableState;
    var unpipeInfo = {
      hasUnpiped: false
    }; // if we're not piping anywhere, then do nothing.
  
    if (state.pipesCount === 0) return this; // just one destination.  most common case.
  
    if (state.pipesCount === 1) {
      // passed in one, but it's not the right one.
      if (dest && dest !== state.pipes) return this;
      if (!dest) dest = state.pipes; // got a match.
  
      state.pipes = null;
      state.pipesCount = 0;
      state.flowing = false;
      if (dest) dest.emit('unpipe', this, unpipeInfo);
      return this;
    } // slow case. multiple pipe destinations.
  
  
    if (!dest) {
      // remove all.
      var dests = state.pipes;
      var len = state.pipesCount;
      state.pipes = null;
      state.pipesCount = 0;
      state.flowing = false;
  
      for (var i = 0; i < len; i++) {
        dests[i].emit('unpipe', this, {
          hasUnpiped: false
        });
      }
  
      return this;
    } // try to find the right one.
  
  
    var index = indexOf(state.pipes, dest);
    if (index === -1) return this;
    state.pipes.splice(index, 1);
    state.pipesCount -= 1;
    if (state.pipesCount === 1) state.pipes = state.pipes[0];
    dest.emit('unpipe', this, unpipeInfo);
    return this;
  }; // set up data events if they are asked for
  // Ensure readable listeners eventually get something
  
  
  Readable.prototype.on = function (ev, fn) {
    var res = Stream.prototype.on.call(this, ev, fn);
    var state = this._readableState;
  
    if (ev === 'data') {
      // update readableListening so that resume() may be a no-op
      // a few lines down. This is needed to support once('readable').
      state.readableListening = this.listenerCount('readable') > 0; // Try start flowing on next tick if stream isn't explicitly paused
  
      if (state.flowing !== false) this.resume();
    } else if (ev === 'readable') {
      if (!state.endEmitted && !state.readableListening) {
        state.readableListening = state.needReadable = true;
        state.flowing = false;
        state.emittedReadable = false;
        debug('on readable', state.length, state.reading);
  
        if (state.length) {
          emitReadable(this);
        } else if (!state.reading) {
          process.nextTick(nReadingNextTick, this);
        }
      }
    }
  
    return res;
  };
  
  Readable.prototype.addListener = Readable.prototype.on;
  
  Readable.prototype.removeListener = function (ev, fn) {
    var res = Stream.prototype.removeListener.call(this, ev, fn);
  
    if (ev === 'readable') {
      // We need to check if there is someone still listening to
      // readable and reset the state. However this needs to happen
      // after readable has been emitted but before I/O (nextTick) to
      // support once('readable', fn) cycles. This means that calling
      // resume within the same tick will have no
      // effect.
      process.nextTick(updateReadableListening, this);
    }
  
    return res;
  };
  
  Readable.prototype.removeAllListeners = function (ev) {
    var res = Stream.prototype.removeAllListeners.apply(this, arguments);
  
    if (ev === 'readable' || ev === undefined) {
      // We need to check if there is someone still listening to
      // readable and reset the state. However this needs to happen
      // after readable has been emitted but before I/O (nextTick) to
      // support once('readable', fn) cycles. This means that calling
      // resume within the same tick will have no
      // effect.
      process.nextTick(updateReadableListening, this);
    }
  
    return res;
  };
  
  function updateReadableListening(self) {
    var state = self._readableState;
    state.readableListening = self.listenerCount('readable') > 0;
  
    if (state.resumeScheduled && !state.paused) {
      // flowing needs to be set to true now, otherwise
      // the upcoming resume will not flow.
      state.flowing = true; // crude way to check if we should resume
    } else if (self.listenerCount('data') > 0) {
      self.resume();
    }
  }
  
  function nReadingNextTick(self) {
    debug('readable nexttick read 0');
    self.read(0);
  } // pause() and resume() are remnants of the legacy readable stream API
  // If the user uses them, then switch into old mode.
  
  
  Readable.prototype.resume = function () {
    var state = this._readableState;
  
    if (!state.flowing) {
      debug('resume'); // we flow only if there is no one listening
      // for readable, but we still have to call
      // resume()
  
      state.flowing = !state.readableListening;
      resume(this, state);
    }
  
    state.paused = false;
    return this;
  };
  
  function resume(stream, state) {
    if (!state.resumeScheduled) {
      state.resumeScheduled = true;
      process.nextTick(resume_, stream, state);
    }
  }
  
  function resume_(stream, state) {
    debug('resume', state.reading);
  
    if (!state.reading) {
      stream.read(0);
    }
  
    state.resumeScheduled = false;
    stream.emit('resume');
    flow(stream);
    if (state.flowing && !state.reading) stream.read(0);
  }
  
  Readable.prototype.pause = function () {
    debug('call pause flowing=%j', this._readableState.flowing);
  
    if (this._readableState.flowing !== false) {
      debug('pause');
      this._readableState.flowing = false;
      this.emit('pause');
    }
  
    this._readableState.paused = true;
    return this;
  };
  
  function flow(stream) {
    var state = stream._readableState;
    debug('flow', state.flowing);
  
    while (state.flowing && stream.read() !== null) {
      ;
    }
  } // wrap an old-style stream as the async data source.
  // This is *not* part of the readable stream interface.
  // It is an ugly unfortunate mess of history.
  
  
  Readable.prototype.wrap = function (stream) {
    var _this = this;
  
    var state = this._readableState;
    var paused = false;
    stream.on('end', function () {
      debug('wrapped end');
  
      if (state.decoder && !state.ended) {
        var chunk = state.decoder.end();
        if (chunk && chunk.length) _this.push(chunk);
      }
  
      _this.push(null);
    });
    stream.on('data', function (chunk) {
      debug('wrapped data');
      if (state.decoder) chunk = state.decoder.write(chunk); // don't skip over falsy values in objectMode
  
      if (state.objectMode && (chunk === null || chunk === undefined)) return;else if (!state.objectMode && (!chunk || !chunk.length)) return;
  
      var ret = _this.push(chunk);
  
      if (!ret) {
        paused = true;
        stream.pause();
      }
    }); // proxy all the other methods.
    // important when wrapping filters and duplexes.
  
    for (var i in stream) {
      if (this[i] === undefined && typeof stream[i] === 'function') {
        this[i] = function methodWrap(method) {
          return function methodWrapReturnFunction() {
            return stream[method].apply(stream, arguments);
          };
        }(i);
      }
    } // proxy certain important events.
  
  
    for (var n = 0; n < kProxyEvents.length; n++) {
      stream.on(kProxyEvents[n], this.emit.bind(this, kProxyEvents[n]));
    } // when we try to consume some more bytes, simply unpause the
    // underlying stream.
  
  
    this._read = function (n) {
      debug('wrapped _read', n);
  
      if (paused) {
        paused = false;
        stream.resume();
      }
    };
  
    return this;
  };
  
  if (typeof Symbol === 'function') {
    Readable.prototype[Symbol.asyncIterator] = function () {
      if (createReadableStreamAsyncIterator === undefined) {
        createReadableStreamAsyncIterator = require('./internal/streams/async_iterator');
      }
  
      return createReadableStreamAsyncIterator(this);
    };
  }
  
  Object.defineProperty(Readable.prototype, 'readableHighWaterMark', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._readableState.highWaterMark;
    }
  });
  Object.defineProperty(Readable.prototype, 'readableBuffer', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._readableState && this._readableState.buffer;
    }
  });
  Object.defineProperty(Readable.prototype, 'readableFlowing', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._readableState.flowing;
    },
    set: function set(state) {
      if (this._readableState) {
        this._readableState.flowing = state;
      }
    }
  }); // exposed for testing purposes only.
  
  Readable._fromList = fromList;
  Object.defineProperty(Readable.prototype, 'readableLength', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._readableState.length;
    }
  }); // Pluck off n bytes from an array of buffers.
  // Length is the combined lengths of all the buffers in the list.
  // This function is designed to be inlinable, so please take care when making
  // changes to the function body.
  
  function fromList(n, state) {
    // nothing buffered
    if (state.length === 0) return null;
    var ret;
    if (state.objectMode) ret = state.buffer.shift();else if (!n || n >= state.length) {
      // read it all, truncate the list
      if (state.decoder) ret = state.buffer.join('');else if (state.buffer.length === 1) ret = state.buffer.first();else ret = state.buffer.concat(state.length);
      state.buffer.clear();
    } else {
      // read part of list
      ret = state.buffer.consume(n, state.decoder);
    }
    return ret;
  }
  
  function endReadable(stream) {
    var state = stream._readableState;
    debug('endReadable', state.endEmitted);
  
    if (!state.endEmitted) {
      state.ended = true;
      process.nextTick(endReadableNT, state, stream);
    }
  }
  
  function endReadableNT(state, stream) {
    debug('endReadableNT', state.endEmitted, state.length); // Check that we didn't get one last unshift.
  
    if (!state.endEmitted && state.length === 0) {
      state.endEmitted = true;
      stream.readable = false;
      stream.emit('end');
  
      if (state.autoDestroy) {
        // In case of duplex streams we need a way to detect
        // if the writable side is ready for autoDestroy as well
        var wState = stream._writableState;
  
        if (!wState || wState.autoDestroy && wState.finished) {
          stream.destroy();
        }
      }
    }
  }
  
  if (typeof Symbol === 'function') {
    Readable.from = function (iterable, opts) {
      if (from === undefined) {
        from = require('./internal/streams/from');
      }
  
      return from(Readable, iterable, opts);
    };
  }
  
  function indexOf(xs, x) {
    for (var i = 0, l = xs.length; i < l; i++) {
      if (xs[i] === x) return i;
    }
  
    return -1;
  }
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{"../errors":58,"./_stream_duplex":59,"./internal/streams/async_iterator":64,"./internal/streams/buffer_list":65,"./internal/streams/destroy":66,"./internal/streams/from":68,"./internal/streams/state":70,"./internal/streams/stream":71,"_process":32,"buffer":9,"events":12,"inherits":15,"string_decoder/":73,"util":6}],62:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  // a transform stream is a readable/writable stream where you do
  // something with the data.  Sometimes it's called a "filter",
  // but that's not a great name for it, since that implies a thing where
  // some bits pass through, and others are simply ignored.  (That would
  // be a valid example of a transform, of course.)
  //
  // While the output is causally related to the input, it's not a
  // necessarily symmetric or synchronous transformation.  For example,
  // a zlib stream might take multiple plain-text writes(), and then
  // emit a single compressed chunk some time in the future.
  //
  // Here's how this works:
  //
  // The Transform stream has all the aspects of the readable and writable
  // stream classes.  When you write(chunk), that calls _write(chunk,cb)
  // internally, and returns false if there's a lot of pending writes
  // buffered up.  When you call read(), that calls _read(n) until
  // there's enough pending readable data buffered up.
  //
  // In a transform stream, the written data is placed in a buffer.  When
  // _read(n) is called, it transforms the queued up data, calling the
  // buffered _write cb's as it consumes chunks.  If consuming a single
  // written chunk would result in multiple output chunks, then the first
  // outputted bit calls the readcb, and subsequent chunks just go into
  // the read buffer, and will cause it to emit 'readable' if necessary.
  //
  // This way, back-pressure is actually determined by the reading side,
  // since _read has to be called to start processing a new chunk.  However,
  // a pathological inflate type of transform can cause excessive buffering
  // here.  For example, imagine a stream where every byte of input is
  // interpreted as an integer from 0-255, and then results in that many
  // bytes of output.  Writing the 4 bytes {ff,ff,ff,ff} would result in
  // 1kb of data being output.  In this case, you could write a very small
  // amount of input, and end up with a very large amount of output.  In
  // such a pathological inflating mechanism, there'd be no way to tell
  // the system to stop doing the transform.  A single 4MB write could
  // cause the system to run out of memory.
  //
  // However, even in such a pathological case, only a single written chunk
  // would be consumed, and then the rest would wait (un-transformed) until
  // the results of the previous transformed chunk were consumed.
  'use strict';
  
  module.exports = Transform;
  
  var _require$codes = require('../errors').codes,
      ERR_METHOD_NOT_IMPLEMENTED = _require$codes.ERR_METHOD_NOT_IMPLEMENTED,
      ERR_MULTIPLE_CALLBACK = _require$codes.ERR_MULTIPLE_CALLBACK,
      ERR_TRANSFORM_ALREADY_TRANSFORMING = _require$codes.ERR_TRANSFORM_ALREADY_TRANSFORMING,
      ERR_TRANSFORM_WITH_LENGTH_0 = _require$codes.ERR_TRANSFORM_WITH_LENGTH_0;
  
  var Duplex = require('./_stream_duplex');
  
  require('inherits')(Transform, Duplex);
  
  function afterTransform(er, data) {
    var ts = this._transformState;
    ts.transforming = false;
    var cb = ts.writecb;
  
    if (cb === null) {
      return this.emit('error', new ERR_MULTIPLE_CALLBACK());
    }
  
    ts.writechunk = null;
    ts.writecb = null;
    if (data != null) // single equals check for both `null` and `undefined`
      this.push(data);
    cb(er);
    var rs = this._readableState;
    rs.reading = false;
  
    if (rs.needReadable || rs.length < rs.highWaterMark) {
      this._read(rs.highWaterMark);
    }
  }
  
  function Transform(options) {
    if (!(this instanceof Transform)) return new Transform(options);
    Duplex.call(this, options);
    this._transformState = {
      afterTransform: afterTransform.bind(this),
      needTransform: false,
      transforming: false,
      writecb: null,
      writechunk: null,
      writeencoding: null
    }; // start out asking for a readable event once data is transformed.
  
    this._readableState.needReadable = true; // we have implemented the _read method, and done the other things
    // that Readable wants before the first _read call, so unset the
    // sync guard flag.
  
    this._readableState.sync = false;
  
    if (options) {
      if (typeof options.transform === 'function') this._transform = options.transform;
      if (typeof options.flush === 'function') this._flush = options.flush;
    } // When the writable side finishes, then flush out anything remaining.
  
  
    this.on('prefinish', prefinish);
  }
  
  function prefinish() {
    var _this = this;
  
    if (typeof this._flush === 'function' && !this._readableState.destroyed) {
      this._flush(function (er, data) {
        done(_this, er, data);
      });
    } else {
      done(this, null, null);
    }
  }
  
  Transform.prototype.push = function (chunk, encoding) {
    this._transformState.needTransform = false;
    return Duplex.prototype.push.call(this, chunk, encoding);
  }; // This is the part where you do stuff!
  // override this function in implementation classes.
  // 'chunk' is an input chunk.
  //
  // Call `push(newChunk)` to pass along transformed output
  // to the readable side.  You may call 'push' zero or more times.
  //
  // Call `cb(err)` when you are done with this chunk.  If you pass
  // an error, then that'll put the hurt on the whole operation.  If you
  // never call cb(), then you'll never get another chunk.
  
  
  Transform.prototype._transform = function (chunk, encoding, cb) {
    cb(new ERR_METHOD_NOT_IMPLEMENTED('_transform()'));
  };
  
  Transform.prototype._write = function (chunk, encoding, cb) {
    var ts = this._transformState;
    ts.writecb = cb;
    ts.writechunk = chunk;
    ts.writeencoding = encoding;
  
    if (!ts.transforming) {
      var rs = this._readableState;
      if (ts.needTransform || rs.needReadable || rs.length < rs.highWaterMark) this._read(rs.highWaterMark);
    }
  }; // Doesn't matter what the args are here.
  // _transform does all the work.
  // That we got here means that the readable side wants more data.
  
  
  Transform.prototype._read = function (n) {
    var ts = this._transformState;
  
    if (ts.writechunk !== null && !ts.transforming) {
      ts.transforming = true;
  
      this._transform(ts.writechunk, ts.writeencoding, ts.afterTransform);
    } else {
      // mark that we need a transform, so that any data that comes in
      // will get processed, now that we've asked for it.
      ts.needTransform = true;
    }
  };
  
  Transform.prototype._destroy = function (err, cb) {
    Duplex.prototype._destroy.call(this, err, function (err2) {
      cb(err2);
    });
  };
  
  function done(stream, er, data) {
    if (er) return stream.emit('error', er);
    if (data != null) // single equals check for both `null` and `undefined`
      stream.push(data); // TODO(BridgeAR): Write a test for these two error cases
    // if there's nothing in the write buffer, then that means
    // that nothing more will ever be provided
  
    if (stream._writableState.length) throw new ERR_TRANSFORM_WITH_LENGTH_0();
    if (stream._transformState.transforming) throw new ERR_TRANSFORM_ALREADY_TRANSFORMING();
    return stream.push(null);
  }
  },{"../errors":58,"./_stream_duplex":59,"inherits":15}],63:[function(require,module,exports){
  (function (process,global){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  // A bit simpler than readable streams.
  // Implement an async ._write(chunk, encoding, cb), and it'll handle all
  // the drain event emission and buffering.
  'use strict';
  
  module.exports = Writable;
  /* <replacement> */
  
  function WriteReq(chunk, encoding, cb) {
    this.chunk = chunk;
    this.encoding = encoding;
    this.callback = cb;
    this.next = null;
  } // It seems a linked list but it is not
  // there will be only 2 of these for each stream
  
  
  function CorkedRequest(state) {
    var _this = this;
  
    this.next = null;
    this.entry = null;
  
    this.finish = function () {
      onCorkedFinish(_this, state);
    };
  }
  /* </replacement> */
  
  /*<replacement>*/
  
  
  var Duplex;
  /*</replacement>*/
  
  Writable.WritableState = WritableState;
  /*<replacement>*/
  
  var internalUtil = {
    deprecate: require('util-deprecate')
  };
  /*</replacement>*/
  
  /*<replacement>*/
  
  var Stream = require('./internal/streams/stream');
  /*</replacement>*/
  
  
  var Buffer = require('buffer').Buffer;
  
  var OurUint8Array = global.Uint8Array || function () {};
  
  function _uint8ArrayToBuffer(chunk) {
    return Buffer.from(chunk);
  }
  
  function _isUint8Array(obj) {
    return Buffer.isBuffer(obj) || obj instanceof OurUint8Array;
  }
  
  var destroyImpl = require('./internal/streams/destroy');
  
  var _require = require('./internal/streams/state'),
      getHighWaterMark = _require.getHighWaterMark;
  
  var _require$codes = require('../errors').codes,
      ERR_INVALID_ARG_TYPE = _require$codes.ERR_INVALID_ARG_TYPE,
      ERR_METHOD_NOT_IMPLEMENTED = _require$codes.ERR_METHOD_NOT_IMPLEMENTED,
      ERR_MULTIPLE_CALLBACK = _require$codes.ERR_MULTIPLE_CALLBACK,
      ERR_STREAM_CANNOT_PIPE = _require$codes.ERR_STREAM_CANNOT_PIPE,
      ERR_STREAM_DESTROYED = _require$codes.ERR_STREAM_DESTROYED,
      ERR_STREAM_NULL_VALUES = _require$codes.ERR_STREAM_NULL_VALUES,
      ERR_STREAM_WRITE_AFTER_END = _require$codes.ERR_STREAM_WRITE_AFTER_END,
      ERR_UNKNOWN_ENCODING = _require$codes.ERR_UNKNOWN_ENCODING;
  
  var errorOrDestroy = destroyImpl.errorOrDestroy;
  
  require('inherits')(Writable, Stream);
  
  function nop() {}
  
  function WritableState(options, stream, isDuplex) {
    Duplex = Duplex || require('./_stream_duplex');
    options = options || {}; // Duplex streams are both readable and writable, but share
    // the same options object.
    // However, some cases require setting options to different
    // values for the readable and the writable sides of the duplex stream,
    // e.g. options.readableObjectMode vs. options.writableObjectMode, etc.
  
    if (typeof isDuplex !== 'boolean') isDuplex = stream instanceof Duplex; // object stream flag to indicate whether or not this stream
    // contains buffers or objects.
  
    this.objectMode = !!options.objectMode;
    if (isDuplex) this.objectMode = this.objectMode || !!options.writableObjectMode; // the point at which write() starts returning false
    // Note: 0 is a valid value, means that we always return false if
    // the entire buffer is not flushed immediately on write()
  
    this.highWaterMark = getHighWaterMark(this, options, 'writableHighWaterMark', isDuplex); // if _final has been called
  
    this.finalCalled = false; // drain event flag.
  
    this.needDrain = false; // at the start of calling end()
  
    this.ending = false; // when end() has been called, and returned
  
    this.ended = false; // when 'finish' is emitted
  
    this.finished = false; // has it been destroyed
  
    this.destroyed = false; // should we decode strings into buffers before passing to _write?
    // this is here so that some node-core streams can optimize string
    // handling at a lower level.
  
    var noDecode = options.decodeStrings === false;
    this.decodeStrings = !noDecode; // Crypto is kind of old and crusty.  Historically, its default string
    // encoding is 'binary' so we have to make this configurable.
    // Everything else in the universe uses 'utf8', though.
  
    this.defaultEncoding = options.defaultEncoding || 'utf8'; // not an actual buffer we keep track of, but a measurement
    // of how much we're waiting to get pushed to some underlying
    // socket or file.
  
    this.length = 0; // a flag to see when we're in the middle of a write.
  
    this.writing = false; // when true all writes will be buffered until .uncork() call
  
    this.corked = 0; // a flag to be able to tell if the onwrite cb is called immediately,
    // or on a later tick.  We set this to true at first, because any
    // actions that shouldn't happen until "later" should generally also
    // not happen before the first write call.
  
    this.sync = true; // a flag to know if we're processing previously buffered items, which
    // may call the _write() callback in the same tick, so that we don't
    // end up in an overlapped onwrite situation.
  
    this.bufferProcessing = false; // the callback that's passed to _write(chunk,cb)
  
    this.onwrite = function (er) {
      onwrite(stream, er);
    }; // the callback that the user supplies to write(chunk,encoding,cb)
  
  
    this.writecb = null; // the amount that is being written when _write is called.
  
    this.writelen = 0;
    this.bufferedRequest = null;
    this.lastBufferedRequest = null; // number of pending user-supplied write callbacks
    // this must be 0 before 'finish' can be emitted
  
    this.pendingcb = 0; // emit prefinish if the only thing we're waiting for is _write cbs
    // This is relevant for synchronous Transform streams
  
    this.prefinished = false; // True if the error was already emitted and should not be thrown again
  
    this.errorEmitted = false; // Should close be emitted on destroy. Defaults to true.
  
    this.emitClose = options.emitClose !== false; // Should .destroy() be called after 'finish' (and potentially 'end')
  
    this.autoDestroy = !!options.autoDestroy; // count buffered requests
  
    this.bufferedRequestCount = 0; // allocate the first CorkedRequest, there is always
    // one allocated and free to use, and we maintain at most two
  
    this.corkedRequestsFree = new CorkedRequest(this);
  }
  
  WritableState.prototype.getBuffer = function getBuffer() {
    var current = this.bufferedRequest;
    var out = [];
  
    while (current) {
      out.push(current);
      current = current.next;
    }
  
    return out;
  };
  
  (function () {
    try {
      Object.defineProperty(WritableState.prototype, 'buffer', {
        get: internalUtil.deprecate(function writableStateBufferGetter() {
          return this.getBuffer();
        }, '_writableState.buffer is deprecated. Use _writableState.getBuffer ' + 'instead.', 'DEP0003')
      });
    } catch (_) {}
  })(); // Test _writableState for inheritance to account for Duplex streams,
  // whose prototype chain only points to Readable.
  
  
  var realHasInstance;
  
  if (typeof Symbol === 'function' && Symbol.hasInstance && typeof Function.prototype[Symbol.hasInstance] === 'function') {
    realHasInstance = Function.prototype[Symbol.hasInstance];
    Object.defineProperty(Writable, Symbol.hasInstance, {
      value: function value(object) {
        if (realHasInstance.call(this, object)) return true;
        if (this !== Writable) return false;
        return object && object._writableState instanceof WritableState;
      }
    });
  } else {
    realHasInstance = function realHasInstance(object) {
      return object instanceof this;
    };
  }
  
  function Writable(options) {
    Duplex = Duplex || require('./_stream_duplex'); // Writable ctor is applied to Duplexes, too.
    // `realHasInstance` is necessary because using plain `instanceof`
    // would return false, as no `_writableState` property is attached.
    // Trying to use the custom `instanceof` for Writable here will also break the
    // Node.js LazyTransform implementation, which has a non-trivial getter for
    // `_writableState` that would lead to infinite recursion.
    // Checking for a Stream.Duplex instance is faster here instead of inside
    // the WritableState constructor, at least with V8 6.5
  
    var isDuplex = this instanceof Duplex;
    if (!isDuplex && !realHasInstance.call(Writable, this)) return new Writable(options);
    this._writableState = new WritableState(options, this, isDuplex); // legacy.
  
    this.writable = true;
  
    if (options) {
      if (typeof options.write === 'function') this._write = options.write;
      if (typeof options.writev === 'function') this._writev = options.writev;
      if (typeof options.destroy === 'function') this._destroy = options.destroy;
      if (typeof options.final === 'function') this._final = options.final;
    }
  
    Stream.call(this);
  } // Otherwise people can pipe Writable streams, which is just wrong.
  
  
  Writable.prototype.pipe = function () {
    errorOrDestroy(this, new ERR_STREAM_CANNOT_PIPE());
  };
  
  function writeAfterEnd(stream, cb) {
    var er = new ERR_STREAM_WRITE_AFTER_END(); // TODO: defer error events consistently everywhere, not just the cb
  
    errorOrDestroy(stream, er);
    process.nextTick(cb, er);
  } // Checks that a user-supplied chunk is valid, especially for the particular
  // mode the stream is in. Currently this means that `null` is never accepted
  // and undefined/non-string values are only allowed in object mode.
  
  
  function validChunk(stream, state, chunk, cb) {
    var er;
  
    if (chunk === null) {
      er = new ERR_STREAM_NULL_VALUES();
    } else if (typeof chunk !== 'string' && !state.objectMode) {
      er = new ERR_INVALID_ARG_TYPE('chunk', ['string', 'Buffer'], chunk);
    }
  
    if (er) {
      errorOrDestroy(stream, er);
      process.nextTick(cb, er);
      return false;
    }
  
    return true;
  }
  
  Writable.prototype.write = function (chunk, encoding, cb) {
    var state = this._writableState;
    var ret = false;
  
    var isBuf = !state.objectMode && _isUint8Array(chunk);
  
    if (isBuf && !Buffer.isBuffer(chunk)) {
      chunk = _uint8ArrayToBuffer(chunk);
    }
  
    if (typeof encoding === 'function') {
      cb = encoding;
      encoding = null;
    }
  
    if (isBuf) encoding = 'buffer';else if (!encoding) encoding = state.defaultEncoding;
    if (typeof cb !== 'function') cb = nop;
    if (state.ending) writeAfterEnd(this, cb);else if (isBuf || validChunk(this, state, chunk, cb)) {
      state.pendingcb++;
      ret = writeOrBuffer(this, state, isBuf, chunk, encoding, cb);
    }
    return ret;
  };
  
  Writable.prototype.cork = function () {
    this._writableState.corked++;
  };
  
  Writable.prototype.uncork = function () {
    var state = this._writableState;
  
    if (state.corked) {
      state.corked--;
      if (!state.writing && !state.corked && !state.bufferProcessing && state.bufferedRequest) clearBuffer(this, state);
    }
  };
  
  Writable.prototype.setDefaultEncoding = function setDefaultEncoding(encoding) {
    // node::ParseEncoding() requires lower case.
    if (typeof encoding === 'string') encoding = encoding.toLowerCase();
    if (!(['hex', 'utf8', 'utf-8', 'ascii', 'binary', 'base64', 'ucs2', 'ucs-2', 'utf16le', 'utf-16le', 'raw'].indexOf((encoding + '').toLowerCase()) > -1)) throw new ERR_UNKNOWN_ENCODING(encoding);
    this._writableState.defaultEncoding = encoding;
    return this;
  };
  
  Object.defineProperty(Writable.prototype, 'writableBuffer', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._writableState && this._writableState.getBuffer();
    }
  });
  
  function decodeChunk(state, chunk, encoding) {
    if (!state.objectMode && state.decodeStrings !== false && typeof chunk === 'string') {
      chunk = Buffer.from(chunk, encoding);
    }
  
    return chunk;
  }
  
  Object.defineProperty(Writable.prototype, 'writableHighWaterMark', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._writableState.highWaterMark;
    }
  }); // if we're already writing something, then just put this
  // in the queue, and wait our turn.  Otherwise, call _write
  // If we return false, then we need a drain event, so set that flag.
  
  function writeOrBuffer(stream, state, isBuf, chunk, encoding, cb) {
    if (!isBuf) {
      var newChunk = decodeChunk(state, chunk, encoding);
  
      if (chunk !== newChunk) {
        isBuf = true;
        encoding = 'buffer';
        chunk = newChunk;
      }
    }
  
    var len = state.objectMode ? 1 : chunk.length;
    state.length += len;
    var ret = state.length < state.highWaterMark; // we must ensure that previous needDrain will not be reset to false.
  
    if (!ret) state.needDrain = true;
  
    if (state.writing || state.corked) {
      var last = state.lastBufferedRequest;
      state.lastBufferedRequest = {
        chunk: chunk,
        encoding: encoding,
        isBuf: isBuf,
        callback: cb,
        next: null
      };
  
      if (last) {
        last.next = state.lastBufferedRequest;
      } else {
        state.bufferedRequest = state.lastBufferedRequest;
      }
  
      state.bufferedRequestCount += 1;
    } else {
      doWrite(stream, state, false, len, chunk, encoding, cb);
    }
  
    return ret;
  }
  
  function doWrite(stream, state, writev, len, chunk, encoding, cb) {
    state.writelen = len;
    state.writecb = cb;
    state.writing = true;
    state.sync = true;
    if (state.destroyed) state.onwrite(new ERR_STREAM_DESTROYED('write'));else if (writev) stream._writev(chunk, state.onwrite);else stream._write(chunk, encoding, state.onwrite);
    state.sync = false;
  }
  
  function onwriteError(stream, state, sync, er, cb) {
    --state.pendingcb;
  
    if (sync) {
      // defer the callback if we are being called synchronously
      // to avoid piling up things on the stack
      process.nextTick(cb, er); // this can emit finish, and it will always happen
      // after error
  
      process.nextTick(finishMaybe, stream, state);
      stream._writableState.errorEmitted = true;
      errorOrDestroy(stream, er);
    } else {
      // the caller expect this to happen before if
      // it is async
      cb(er);
      stream._writableState.errorEmitted = true;
      errorOrDestroy(stream, er); // this can emit finish, but finish must
      // always follow error
  
      finishMaybe(stream, state);
    }
  }
  
  function onwriteStateUpdate(state) {
    state.writing = false;
    state.writecb = null;
    state.length -= state.writelen;
    state.writelen = 0;
  }
  
  function onwrite(stream, er) {
    var state = stream._writableState;
    var sync = state.sync;
    var cb = state.writecb;
    if (typeof cb !== 'function') throw new ERR_MULTIPLE_CALLBACK();
    onwriteStateUpdate(state);
    if (er) onwriteError(stream, state, sync, er, cb);else {
      // Check if we're actually ready to finish, but don't emit yet
      var finished = needFinish(state) || stream.destroyed;
  
      if (!finished && !state.corked && !state.bufferProcessing && state.bufferedRequest) {
        clearBuffer(stream, state);
      }
  
      if (sync) {
        process.nextTick(afterWrite, stream, state, finished, cb);
      } else {
        afterWrite(stream, state, finished, cb);
      }
    }
  }
  
  function afterWrite(stream, state, finished, cb) {
    if (!finished) onwriteDrain(stream, state);
    state.pendingcb--;
    cb();
    finishMaybe(stream, state);
  } // Must force callback to be called on nextTick, so that we don't
  // emit 'drain' before the write() consumer gets the 'false' return
  // value, and has a chance to attach a 'drain' listener.
  
  
  function onwriteDrain(stream, state) {
    if (state.length === 0 && state.needDrain) {
      state.needDrain = false;
      stream.emit('drain');
    }
  } // if there's something in the buffer waiting, then process it
  
  
  function clearBuffer(stream, state) {
    state.bufferProcessing = true;
    var entry = state.bufferedRequest;
  
    if (stream._writev && entry && entry.next) {
      // Fast case, write everything using _writev()
      var l = state.bufferedRequestCount;
      var buffer = new Array(l);
      var holder = state.corkedRequestsFree;
      holder.entry = entry;
      var count = 0;
      var allBuffers = true;
  
      while (entry) {
        buffer[count] = entry;
        if (!entry.isBuf) allBuffers = false;
        entry = entry.next;
        count += 1;
      }
  
      buffer.allBuffers = allBuffers;
      doWrite(stream, state, true, state.length, buffer, '', holder.finish); // doWrite is almost always async, defer these to save a bit of time
      // as the hot path ends with doWrite
  
      state.pendingcb++;
      state.lastBufferedRequest = null;
  
      if (holder.next) {
        state.corkedRequestsFree = holder.next;
        holder.next = null;
      } else {
        state.corkedRequestsFree = new CorkedRequest(state);
      }
  
      state.bufferedRequestCount = 0;
    } else {
      // Slow case, write chunks one-by-one
      while (entry) {
        var chunk = entry.chunk;
        var encoding = entry.encoding;
        var cb = entry.callback;
        var len = state.objectMode ? 1 : chunk.length;
        doWrite(stream, state, false, len, chunk, encoding, cb);
        entry = entry.next;
        state.bufferedRequestCount--; // if we didn't call the onwrite immediately, then
        // it means that we need to wait until it does.
        // also, that means that the chunk and cb are currently
        // being processed, so move the buffer counter past them.
  
        if (state.writing) {
          break;
        }
      }
  
      if (entry === null) state.lastBufferedRequest = null;
    }
  
    state.bufferedRequest = entry;
    state.bufferProcessing = false;
  }
  
  Writable.prototype._write = function (chunk, encoding, cb) {
    cb(new ERR_METHOD_NOT_IMPLEMENTED('_write()'));
  };
  
  Writable.prototype._writev = null;
  
  Writable.prototype.end = function (chunk, encoding, cb) {
    var state = this._writableState;
  
    if (typeof chunk === 'function') {
      cb = chunk;
      chunk = null;
      encoding = null;
    } else if (typeof encoding === 'function') {
      cb = encoding;
      encoding = null;
    }
  
    if (chunk !== null && chunk !== undefined) this.write(chunk, encoding); // .end() fully uncorks
  
    if (state.corked) {
      state.corked = 1;
      this.uncork();
    } // ignore unnecessary end() calls.
  
  
    if (!state.ending) endWritable(this, state, cb);
    return this;
  };
  
  Object.defineProperty(Writable.prototype, 'writableLength', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      return this._writableState.length;
    }
  });
  
  function needFinish(state) {
    return state.ending && state.length === 0 && state.bufferedRequest === null && !state.finished && !state.writing;
  }
  
  function callFinal(stream, state) {
    stream._final(function (err) {
      state.pendingcb--;
  
      if (err) {
        errorOrDestroy(stream, err);
      }
  
      state.prefinished = true;
      stream.emit('prefinish');
      finishMaybe(stream, state);
    });
  }
  
  function prefinish(stream, state) {
    if (!state.prefinished && !state.finalCalled) {
      if (typeof stream._final === 'function' && !state.destroyed) {
        state.pendingcb++;
        state.finalCalled = true;
        process.nextTick(callFinal, stream, state);
      } else {
        state.prefinished = true;
        stream.emit('prefinish');
      }
    }
  }
  
  function finishMaybe(stream, state) {
    var need = needFinish(state);
  
    if (need) {
      prefinish(stream, state);
  
      if (state.pendingcb === 0) {
        state.finished = true;
        stream.emit('finish');
  
        if (state.autoDestroy) {
          // In case of duplex streams we need a way to detect
          // if the readable side is ready for autoDestroy as well
          var rState = stream._readableState;
  
          if (!rState || rState.autoDestroy && rState.endEmitted) {
            stream.destroy();
          }
        }
      }
    }
  
    return need;
  }
  
  function endWritable(stream, state, cb) {
    state.ending = true;
    finishMaybe(stream, state);
  
    if (cb) {
      if (state.finished) process.nextTick(cb);else stream.once('finish', cb);
    }
  
    state.ended = true;
    stream.writable = false;
  }
  
  function onCorkedFinish(corkReq, state, err) {
    var entry = corkReq.entry;
    corkReq.entry = null;
  
    while (entry) {
      var cb = entry.callback;
      state.pendingcb--;
      cb(err);
      entry = entry.next;
    } // reuse the free corkReq.
  
  
    state.corkedRequestsFree.next = corkReq;
  }
  
  Object.defineProperty(Writable.prototype, 'destroyed', {
    // making it explicit this property is not enumerable
    // because otherwise some prototype manipulation in
    // userland will fail
    enumerable: false,
    get: function get() {
      if (this._writableState === undefined) {
        return false;
      }
  
      return this._writableState.destroyed;
    },
    set: function set(value) {
      // we ignore the value if the stream
      // has not been initialized yet
      if (!this._writableState) {
        return;
      } // backward compatibility, the user is explicitly
      // managing destroyed
  
  
      this._writableState.destroyed = value;
    }
  });
  Writable.prototype.destroy = destroyImpl.destroy;
  Writable.prototype._undestroy = destroyImpl.undestroy;
  
  Writable.prototype._destroy = function (err, cb) {
    cb(err);
  };
  }).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{"../errors":58,"./_stream_duplex":59,"./internal/streams/destroy":66,"./internal/streams/state":70,"./internal/streams/stream":71,"_process":32,"buffer":9,"inherits":15,"util-deprecate":77}],64:[function(require,module,exports){
  (function (process){
  'use strict';
  
  var _Object$setPrototypeO;
  
  function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }
  
  var finished = require('./end-of-stream');
  
  var kLastResolve = Symbol('lastResolve');
  var kLastReject = Symbol('lastReject');
  var kError = Symbol('error');
  var kEnded = Symbol('ended');
  var kLastPromise = Symbol('lastPromise');
  var kHandlePromise = Symbol('handlePromise');
  var kStream = Symbol('stream');
  
  function createIterResult(value, done) {
    return {
      value: value,
      done: done
    };
  }
  
  function readAndResolve(iter) {
    var resolve = iter[kLastResolve];
  
    if (resolve !== null) {
      var data = iter[kStream].read(); // we defer if data is null
      // we can be expecting either 'end' or
      // 'error'
  
      if (data !== null) {
        iter[kLastPromise] = null;
        iter[kLastResolve] = null;
        iter[kLastReject] = null;
        resolve(createIterResult(data, false));
      }
    }
  }
  
  function onReadable(iter) {
    // we wait for the next tick, because it might
    // emit an error with process.nextTick
    process.nextTick(readAndResolve, iter);
  }
  
  function wrapForNext(lastPromise, iter) {
    return function (resolve, reject) {
      lastPromise.then(function () {
        if (iter[kEnded]) {
          resolve(createIterResult(undefined, true));
          return;
        }
  
        iter[kHandlePromise](resolve, reject);
      }, reject);
    };
  }
  
  var AsyncIteratorPrototype = Object.getPrototypeOf(function () {});
  var ReadableStreamAsyncIteratorPrototype = Object.setPrototypeOf((_Object$setPrototypeO = {
    get stream() {
      return this[kStream];
    },
  
    next: function next() {
      var _this = this;
  
      // if we have detected an error in the meanwhile
      // reject straight away
      var error = this[kError];
  
      if (error !== null) {
        return Promise.reject(error);
      }
  
      if (this[kEnded]) {
        return Promise.resolve(createIterResult(undefined, true));
      }
  
      if (this[kStream].destroyed) {
        // We need to defer via nextTick because if .destroy(err) is
        // called, the error will be emitted via nextTick, and
        // we cannot guarantee that there is no error lingering around
        // waiting to be emitted.
        return new Promise(function (resolve, reject) {
          process.nextTick(function () {
            if (_this[kError]) {
              reject(_this[kError]);
            } else {
              resolve(createIterResult(undefined, true));
            }
          });
        });
      } // if we have multiple next() calls
      // we will wait for the previous Promise to finish
      // this logic is optimized to support for await loops,
      // where next() is only called once at a time
  
  
      var lastPromise = this[kLastPromise];
      var promise;
  
      if (lastPromise) {
        promise = new Promise(wrapForNext(lastPromise, this));
      } else {
        // fast path needed to support multiple this.push()
        // without triggering the next() queue
        var data = this[kStream].read();
  
        if (data !== null) {
          return Promise.resolve(createIterResult(data, false));
        }
  
        promise = new Promise(this[kHandlePromise]);
      }
  
      this[kLastPromise] = promise;
      return promise;
    }
  }, _defineProperty(_Object$setPrototypeO, Symbol.asyncIterator, function () {
    return this;
  }), _defineProperty(_Object$setPrototypeO, "return", function _return() {
    var _this2 = this;
  
    // destroy(err, cb) is a private API
    // we can guarantee we have that here, because we control the
    // Readable class this is attached to
    return new Promise(function (resolve, reject) {
      _this2[kStream].destroy(null, function (err) {
        if (err) {
          reject(err);
          return;
        }
  
        resolve(createIterResult(undefined, true));
      });
    });
  }), _Object$setPrototypeO), AsyncIteratorPrototype);
  
  var createReadableStreamAsyncIterator = function createReadableStreamAsyncIterator(stream) {
    var _Object$create;
  
    var iterator = Object.create(ReadableStreamAsyncIteratorPrototype, (_Object$create = {}, _defineProperty(_Object$create, kStream, {
      value: stream,
      writable: true
    }), _defineProperty(_Object$create, kLastResolve, {
      value: null,
      writable: true
    }), _defineProperty(_Object$create, kLastReject, {
      value: null,
      writable: true
    }), _defineProperty(_Object$create, kError, {
      value: null,
      writable: true
    }), _defineProperty(_Object$create, kEnded, {
      value: stream._readableState.endEmitted,
      writable: true
    }), _defineProperty(_Object$create, kHandlePromise, {
      value: function value(resolve, reject) {
        var data = iterator[kStream].read();
  
        if (data) {
          iterator[kLastPromise] = null;
          iterator[kLastResolve] = null;
          iterator[kLastReject] = null;
          resolve(createIterResult(data, false));
        } else {
          iterator[kLastResolve] = resolve;
          iterator[kLastReject] = reject;
        }
      },
      writable: true
    }), _Object$create));
    iterator[kLastPromise] = null;
    finished(stream, function (err) {
      if (err && err.code !== 'ERR_STREAM_PREMATURE_CLOSE') {
        var reject = iterator[kLastReject]; // reject if we are waiting for data in the Promise
        // returned by next() and store the error
  
        if (reject !== null) {
          iterator[kLastPromise] = null;
          iterator[kLastResolve] = null;
          iterator[kLastReject] = null;
          reject(err);
        }
  
        iterator[kError] = err;
        return;
      }
  
      var resolve = iterator[kLastResolve];
  
      if (resolve !== null) {
        iterator[kLastPromise] = null;
        iterator[kLastResolve] = null;
        iterator[kLastReject] = null;
        resolve(createIterResult(undefined, true));
      }
  
      iterator[kEnded] = true;
    });
    stream.on('readable', onReadable.bind(null, iterator));
    return iterator;
  };
  
  module.exports = createReadableStreamAsyncIterator;
  }).call(this,require('_process'))
  },{"./end-of-stream":67,"_process":32}],65:[function(require,module,exports){
  'use strict';
  
  function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }
  
  function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(Object(source), true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(Object(source)).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }
  
  function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }
  
  function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }
  
  function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }
  
  function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }
  
  var _require = require('buffer'),
      Buffer = _require.Buffer;
  
  var _require2 = require('util'),
      inspect = _require2.inspect;
  
  var custom = inspect && inspect.custom || 'inspect';
  
  function copyBuffer(src, target, offset) {
    Buffer.prototype.copy.call(src, target, offset);
  }
  
  module.exports =
  /*#__PURE__*/
  function () {
    function BufferList() {
      _classCallCheck(this, BufferList);
  
      this.head = null;
      this.tail = null;
      this.length = 0;
    }
  
    _createClass(BufferList, [{
      key: "push",
      value: function push(v) {
        var entry = {
          data: v,
          next: null
        };
        if (this.length > 0) this.tail.next = entry;else this.head = entry;
        this.tail = entry;
        ++this.length;
      }
    }, {
      key: "unshift",
      value: function unshift(v) {
        var entry = {
          data: v,
          next: this.head
        };
        if (this.length === 0) this.tail = entry;
        this.head = entry;
        ++this.length;
      }
    }, {
      key: "shift",
      value: function shift() {
        if (this.length === 0) return;
        var ret = this.head.data;
        if (this.length === 1) this.head = this.tail = null;else this.head = this.head.next;
        --this.length;
        return ret;
      }
    }, {
      key: "clear",
      value: function clear() {
        this.head = this.tail = null;
        this.length = 0;
      }
    }, {
      key: "join",
      value: function join(s) {
        if (this.length === 0) return '';
        var p = this.head;
        var ret = '' + p.data;
  
        while (p = p.next) {
          ret += s + p.data;
        }
  
        return ret;
      }
    }, {
      key: "concat",
      value: function concat(n) {
        if (this.length === 0) return Buffer.alloc(0);
        var ret = Buffer.allocUnsafe(n >>> 0);
        var p = this.head;
        var i = 0;
  
        while (p) {
          copyBuffer(p.data, ret, i);
          i += p.data.length;
          p = p.next;
        }
  
        return ret;
      } // Consumes a specified amount of bytes or characters from the buffered data.
  
    }, {
      key: "consume",
      value: function consume(n, hasStrings) {
        var ret;
  
        if (n < this.head.data.length) {
          // `slice` is the same for buffers and strings.
          ret = this.head.data.slice(0, n);
          this.head.data = this.head.data.slice(n);
        } else if (n === this.head.data.length) {
          // First chunk is a perfect match.
          ret = this.shift();
        } else {
          // Result spans more than one buffer.
          ret = hasStrings ? this._getString(n) : this._getBuffer(n);
        }
  
        return ret;
      }
    }, {
      key: "first",
      value: function first() {
        return this.head.data;
      } // Consumes a specified amount of characters from the buffered data.
  
    }, {
      key: "_getString",
      value: function _getString(n) {
        var p = this.head;
        var c = 1;
        var ret = p.data;
        n -= ret.length;
  
        while (p = p.next) {
          var str = p.data;
          var nb = n > str.length ? str.length : n;
          if (nb === str.length) ret += str;else ret += str.slice(0, n);
          n -= nb;
  
          if (n === 0) {
            if (nb === str.length) {
              ++c;
              if (p.next) this.head = p.next;else this.head = this.tail = null;
            } else {
              this.head = p;
              p.data = str.slice(nb);
            }
  
            break;
          }
  
          ++c;
        }
  
        this.length -= c;
        return ret;
      } // Consumes a specified amount of bytes from the buffered data.
  
    }, {
      key: "_getBuffer",
      value: function _getBuffer(n) {
        var ret = Buffer.allocUnsafe(n);
        var p = this.head;
        var c = 1;
        p.data.copy(ret);
        n -= p.data.length;
  
        while (p = p.next) {
          var buf = p.data;
          var nb = n > buf.length ? buf.length : n;
          buf.copy(ret, ret.length - n, 0, nb);
          n -= nb;
  
          if (n === 0) {
            if (nb === buf.length) {
              ++c;
              if (p.next) this.head = p.next;else this.head = this.tail = null;
            } else {
              this.head = p;
              p.data = buf.slice(nb);
            }
  
            break;
          }
  
          ++c;
        }
  
        this.length -= c;
        return ret;
      } // Make sure the linked list only shows the minimal necessary information.
  
    }, {
      key: custom,
      value: function value(_, options) {
        return inspect(this, _objectSpread({}, options, {
          // Only inspect one level.
          depth: 0,
          // It should not recurse.
          customInspect: false
        }));
      }
    }]);
  
    return BufferList;
  }();
  },{"buffer":9,"util":6}],66:[function(require,module,exports){
  (function (process){
  'use strict'; // undocumented cb() API, needed for core, not for public API
  
  function destroy(err, cb) {
    var _this = this;
  
    var readableDestroyed = this._readableState && this._readableState.destroyed;
    var writableDestroyed = this._writableState && this._writableState.destroyed;
  
    if (readableDestroyed || writableDestroyed) {
      if (cb) {
        cb(err);
      } else if (err) {
        if (!this._writableState) {
          process.nextTick(emitErrorNT, this, err);
        } else if (!this._writableState.errorEmitted) {
          this._writableState.errorEmitted = true;
          process.nextTick(emitErrorNT, this, err);
        }
      }
  
      return this;
    } // we set destroyed to true before firing error callbacks in order
    // to make it re-entrance safe in case destroy() is called within callbacks
  
  
    if (this._readableState) {
      this._readableState.destroyed = true;
    } // if this is a duplex stream mark the writable part as destroyed as well
  
  
    if (this._writableState) {
      this._writableState.destroyed = true;
    }
  
    this._destroy(err || null, function (err) {
      if (!cb && err) {
        if (!_this._writableState) {
          process.nextTick(emitErrorAndCloseNT, _this, err);
        } else if (!_this._writableState.errorEmitted) {
          _this._writableState.errorEmitted = true;
          process.nextTick(emitErrorAndCloseNT, _this, err);
        } else {
          process.nextTick(emitCloseNT, _this);
        }
      } else if (cb) {
        process.nextTick(emitCloseNT, _this);
        cb(err);
      } else {
        process.nextTick(emitCloseNT, _this);
      }
    });
  
    return this;
  }
  
  function emitErrorAndCloseNT(self, err) {
    emitErrorNT(self, err);
    emitCloseNT(self);
  }
  
  function emitCloseNT(self) {
    if (self._writableState && !self._writableState.emitClose) return;
    if (self._readableState && !self._readableState.emitClose) return;
    self.emit('close');
  }
  
  function undestroy() {
    if (this._readableState) {
      this._readableState.destroyed = false;
      this._readableState.reading = false;
      this._readableState.ended = false;
      this._readableState.endEmitted = false;
    }
  
    if (this._writableState) {
      this._writableState.destroyed = false;
      this._writableState.ended = false;
      this._writableState.ending = false;
      this._writableState.finalCalled = false;
      this._writableState.prefinished = false;
      this._writableState.finished = false;
      this._writableState.errorEmitted = false;
    }
  }
  
  function emitErrorNT(self, err) {
    self.emit('error', err);
  }
  
  function errorOrDestroy(stream, err) {
    // We have tests that rely on errors being emitted
    // in the same tick, so changing this is semver major.
    // For now when you opt-in to autoDestroy we allow
    // the error to be emitted nextTick. In a future
    // semver major update we should change the default to this.
    var rState = stream._readableState;
    var wState = stream._writableState;
    if (rState && rState.autoDestroy || wState && wState.autoDestroy) stream.destroy(err);else stream.emit('error', err);
  }
  
  module.exports = {
    destroy: destroy,
    undestroy: undestroy,
    errorOrDestroy: errorOrDestroy
  };
  }).call(this,require('_process'))
  },{"_process":32}],67:[function(require,module,exports){
  // Ported from https://github.com/mafintosh/end-of-stream with
  // permission from the author, Mathias Buus (@mafintosh).
  'use strict';
  
  var ERR_STREAM_PREMATURE_CLOSE = require('../../../errors').codes.ERR_STREAM_PREMATURE_CLOSE;
  
  function once(callback) {
    var called = false;
    return function () {
      if (called) return;
      called = true;
  
      for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
        args[_key] = arguments[_key];
      }
  
      callback.apply(this, args);
    };
  }
  
  function noop() {}
  
  function isRequest(stream) {
    return stream.setHeader && typeof stream.abort === 'function';
  }
  
  function eos(stream, opts, callback) {
    if (typeof opts === 'function') return eos(stream, null, opts);
    if (!opts) opts = {};
    callback = once(callback || noop);
    var readable = opts.readable || opts.readable !== false && stream.readable;
    var writable = opts.writable || opts.writable !== false && stream.writable;
  
    var onlegacyfinish = function onlegacyfinish() {
      if (!stream.writable) onfinish();
    };
  
    var writableEnded = stream._writableState && stream._writableState.finished;
  
    var onfinish = function onfinish() {
      writable = false;
      writableEnded = true;
      if (!readable) callback.call(stream);
    };
  
    var readableEnded = stream._readableState && stream._readableState.endEmitted;
  
    var onend = function onend() {
      readable = false;
      readableEnded = true;
      if (!writable) callback.call(stream);
    };
  
    var onerror = function onerror(err) {
      callback.call(stream, err);
    };
  
    var onclose = function onclose() {
      var err;
  
      if (readable && !readableEnded) {
        if (!stream._readableState || !stream._readableState.ended) err = new ERR_STREAM_PREMATURE_CLOSE();
        return callback.call(stream, err);
      }
  
      if (writable && !writableEnded) {
        if (!stream._writableState || !stream._writableState.ended) err = new ERR_STREAM_PREMATURE_CLOSE();
        return callback.call(stream, err);
      }
    };
  
    var onrequest = function onrequest() {
      stream.req.on('finish', onfinish);
    };
  
    if (isRequest(stream)) {
      stream.on('complete', onfinish);
      stream.on('abort', onclose);
      if (stream.req) onrequest();else stream.on('request', onrequest);
    } else if (writable && !stream._writableState) {
      // legacy streams
      stream.on('end', onlegacyfinish);
      stream.on('close', onlegacyfinish);
    }
  
    stream.on('end', onend);
    stream.on('finish', onfinish);
    if (opts.error !== false) stream.on('error', onerror);
    stream.on('close', onclose);
    return function () {
      stream.removeListener('complete', onfinish);
      stream.removeListener('abort', onclose);
      stream.removeListener('request', onrequest);
      if (stream.req) stream.req.removeListener('finish', onfinish);
      stream.removeListener('end', onlegacyfinish);
      stream.removeListener('close', onlegacyfinish);
      stream.removeListener('finish', onfinish);
      stream.removeListener('end', onend);
      stream.removeListener('error', onerror);
      stream.removeListener('close', onclose);
    };
  }
  
  module.exports = eos;
  },{"../../../errors":58}],68:[function(require,module,exports){
  module.exports = function () {
    throw new Error('Readable.from is not available in the browser')
  };
  
  },{}],69:[function(require,module,exports){
  // Ported from https://github.com/mafintosh/pump with
  // permission from the author, Mathias Buus (@mafintosh).
  'use strict';
  
  var eos;
  
  function once(callback) {
    var called = false;
    return function () {
      if (called) return;
      called = true;
      callback.apply(void 0, arguments);
    };
  }
  
  var _require$codes = require('../../../errors').codes,
      ERR_MISSING_ARGS = _require$codes.ERR_MISSING_ARGS,
      ERR_STREAM_DESTROYED = _require$codes.ERR_STREAM_DESTROYED;
  
  function noop(err) {
    // Rethrow the error if it exists to avoid swallowing it
    if (err) throw err;
  }
  
  function isRequest(stream) {
    return stream.setHeader && typeof stream.abort === 'function';
  }
  
  function destroyer(stream, reading, writing, callback) {
    callback = once(callback);
    var closed = false;
    stream.on('close', function () {
      closed = true;
    });
    if (eos === undefined) eos = require('./end-of-stream');
    eos(stream, {
      readable: reading,
      writable: writing
    }, function (err) {
      if (err) return callback(err);
      closed = true;
      callback();
    });
    var destroyed = false;
    return function (err) {
      if (closed) return;
      if (destroyed) return;
      destroyed = true; // request.destroy just do .end - .abort is what we want
  
      if (isRequest(stream)) return stream.abort();
      if (typeof stream.destroy === 'function') return stream.destroy();
      callback(err || new ERR_STREAM_DESTROYED('pipe'));
    };
  }
  
  function call(fn) {
    fn();
  }
  
  function pipe(from, to) {
    return from.pipe(to);
  }
  
  function popCallback(streams) {
    if (!streams.length) return noop;
    if (typeof streams[streams.length - 1] !== 'function') return noop;
    return streams.pop();
  }
  
  function pipeline() {
    for (var _len = arguments.length, streams = new Array(_len), _key = 0; _key < _len; _key++) {
      streams[_key] = arguments[_key];
    }
  
    var callback = popCallback(streams);
    if (Array.isArray(streams[0])) streams = streams[0];
  
    if (streams.length < 2) {
      throw new ERR_MISSING_ARGS('streams');
    }
  
    var error;
    var destroys = streams.map(function (stream, i) {
      var reading = i < streams.length - 1;
      var writing = i > 0;
      return destroyer(stream, reading, writing, function (err) {
        if (!error) error = err;
        if (err) destroys.forEach(call);
        if (reading) return;
        destroys.forEach(call);
        callback(error);
      });
    });
    return streams.reduce(pipe);
  }
  
  module.exports = pipeline;
  },{"../../../errors":58,"./end-of-stream":67}],70:[function(require,module,exports){
  'use strict';
  
  var ERR_INVALID_OPT_VALUE = require('../../../errors').codes.ERR_INVALID_OPT_VALUE;
  
  function highWaterMarkFrom(options, isDuplex, duplexKey) {
    return options.highWaterMark != null ? options.highWaterMark : isDuplex ? options[duplexKey] : null;
  }
  
  function getHighWaterMark(state, options, duplexKey, isDuplex) {
    var hwm = highWaterMarkFrom(options, isDuplex, duplexKey);
  
    if (hwm != null) {
      if (!(isFinite(hwm) && Math.floor(hwm) === hwm) || hwm < 0) {
        var name = isDuplex ? duplexKey : 'highWaterMark';
        throw new ERR_INVALID_OPT_VALUE(name, hwm);
      }
  
      return Math.floor(hwm);
    } // Default value
  
  
    return state.objectMode ? 16 : 16 * 1024;
  }
  
  module.exports = {
    getHighWaterMark: getHighWaterMark
  };
  },{"../../../errors":58}],71:[function(require,module,exports){
  arguments[4][45][0].apply(exports,arguments)
  },{"dup":45,"events":12}],72:[function(require,module,exports){
  exports = module.exports = require('./lib/_stream_readable.js');
  exports.Stream = exports;
  exports.Readable = exports;
  exports.Writable = require('./lib/_stream_writable.js');
  exports.Duplex = require('./lib/_stream_duplex.js');
  exports.Transform = require('./lib/_stream_transform.js');
  exports.PassThrough = require('./lib/_stream_passthrough.js');
  exports.finished = require('./lib/internal/streams/end-of-stream.js');
  exports.pipeline = require('./lib/internal/streams/pipeline.js');
  
  },{"./lib/_stream_duplex.js":59,"./lib/_stream_passthrough.js":60,"./lib/_stream_readable.js":61,"./lib/_stream_transform.js":62,"./lib/_stream_writable.js":63,"./lib/internal/streams/end-of-stream.js":67,"./lib/internal/streams/pipeline.js":69}],73:[function(require,module,exports){
  arguments[4][47][0].apply(exports,arguments)
  },{"dup":47,"safe-buffer":52}],74:[function(require,module,exports){
  (function (setImmediate,clearImmediate){
  var nextTick = require('process/browser.js').nextTick;
  var apply = Function.prototype.apply;
  var slice = Array.prototype.slice;
  var immediateIds = {};
  var nextImmediateId = 0;
  
  // DOM APIs, for completeness
  
  exports.setTimeout = function() {
    return new Timeout(apply.call(setTimeout, window, arguments), clearTimeout);
  };
  exports.setInterval = function() {
    return new Timeout(apply.call(setInterval, window, arguments), clearInterval);
  };
  exports.clearTimeout =
  exports.clearInterval = function(timeout) { timeout.close(); };
  
  function Timeout(id, clearFn) {
    this._id = id;
    this._clearFn = clearFn;
  }
  Timeout.prototype.unref = Timeout.prototype.ref = function() {};
  Timeout.prototype.close = function() {
    this._clearFn.call(window, this._id);
  };
  
  // Does not start the time, just sets up the members needed.
  exports.enroll = function(item, msecs) {
    clearTimeout(item._idleTimeoutId);
    item._idleTimeout = msecs;
  };
  
  exports.unenroll = function(item) {
    clearTimeout(item._idleTimeoutId);
    item._idleTimeout = -1;
  };
  
  exports._unrefActive = exports.active = function(item) {
    clearTimeout(item._idleTimeoutId);
  
    var msecs = item._idleTimeout;
    if (msecs >= 0) {
      item._idleTimeoutId = setTimeout(function onTimeout() {
        if (item._onTimeout)
          item._onTimeout();
      }, msecs);
    }
  };
  
  // That's not how node.js implements it but the exposed api is the same.
  exports.setImmediate = typeof setImmediate === "function" ? setImmediate : function(fn) {
    var id = nextImmediateId++;
    var args = arguments.length < 2 ? false : slice.call(arguments, 1);
  
    immediateIds[id] = true;
  
    nextTick(function onNextTick() {
      if (immediateIds[id]) {
        // fn.call() is faster so we optimize for the common use-case
        // @see http://jsperf.com/call-apply-segu
        if (args) {
          fn.apply(null, args);
        } else {
          fn.call(null);
        }
        // Prevent ids from leaking
        exports.clearImmediate(id);
      }
    });
  
    return id;
  };
  
  exports.clearImmediate = typeof clearImmediate === "function" ? clearImmediate : function(id) {
    delete immediateIds[id];
  };
  }).call(this,require("timers").setImmediate,require("timers").clearImmediate)
  },{"process/browser.js":32,"timers":74}],75:[function(require,module,exports){
  // Copyright Joyent, Inc. and other Node contributors.
  //
  // Permission is hereby granted, free of charge, to any person obtaining a
  // copy of this software and associated documentation files (the
  // "Software"), to deal in the Software without restriction, including
  // without limitation the rights to use, copy, modify, merge, publish,
  // distribute, sublicense, and/or sell copies of the Software, and to permit
  // persons to whom the Software is furnished to do so, subject to the
  // following conditions:
  //
  // The above copyright notice and this permission notice shall be included
  // in all copies or substantial portions of the Software.
  //
  // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  // OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
  // NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
  // DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
  // OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
  // USE OR OTHER DEALINGS IN THE SOFTWARE.
  
  'use strict';
  
  var punycode = require('punycode');
  var util = require('./util');
  
  exports.parse = urlParse;
  exports.resolve = urlResolve;
  exports.resolveObject = urlResolveObject;
  exports.format = urlFormat;
  
  exports.Url = Url;
  
  function Url() {
    this.protocol = null;
    this.slashes = null;
    this.auth = null;
    this.host = null;
    this.port = null;
    this.hostname = null;
    this.hash = null;
    this.search = null;
    this.query = null;
    this.pathname = null;
    this.path = null;
    this.href = null;
  }
  
  // Reference: RFC 3986, RFC 1808, RFC 2396
  
  // define these here so at least they only have to be
  // compiled once on the first module load.
  var protocolPattern = /^([a-z0-9.+-]+:)/i,
      portPattern = /:[0-9]*$/,
  
      // Special case for a simple path URL
      simplePathPattern = /^(\/\/?(?!\/)[^\?\s]*)(\?[^\s]*)?$/,
  
      // RFC 2396: characters reserved for delimiting URLs.
      // We actually just auto-escape these.
      delims = ['<', '>', '"', '`', ' ', '\r', '\n', '\t'],
  
      // RFC 2396: characters not allowed for various reasons.
      unwise = ['{', '}', '|', '\\', '^', '`'].concat(delims),
  
      // Allowed by RFCs, but cause of XSS attacks.  Always escape these.
      autoEscape = ['\''].concat(unwise),
      // Characters that are never ever allowed in a hostname.
      // Note that any invalid chars are also handled, but these
      // are the ones that are *expected* to be seen, so we fast-path
      // them.
      nonHostChars = ['%', '/', '?', ';', '#'].concat(autoEscape),
      hostEndingChars = ['/', '?', '#'],
      hostnameMaxLen = 255,
      hostnamePartPattern = /^[+a-z0-9A-Z_-]{0,63}$/,
      hostnamePartStart = /^([+a-z0-9A-Z_-]{0,63})(.*)$/,
      // protocols that can allow "unsafe" and "unwise" chars.
      unsafeProtocol = {
        'javascript': true,
        'javascript:': true
      },
      // protocols that never have a hostname.
      hostlessProtocol = {
        'javascript': true,
        'javascript:': true
      },
      // protocols that always contain a // bit.
      slashedProtocol = {
        'http': true,
        'https': true,
        'ftp': true,
        'gopher': true,
        'file': true,
        'http:': true,
        'https:': true,
        'ftp:': true,
        'gopher:': true,
        'file:': true
      },
      querystring = require('querystring');
  
  function urlParse(url, parseQueryString, slashesDenoteHost) {
    if (url && util.isObject(url) && url instanceof Url) return url;
  
    var u = new Url;
    u.parse(url, parseQueryString, slashesDenoteHost);
    return u;
  }
  
  Url.prototype.parse = function(url, parseQueryString, slashesDenoteHost) {
    if (!util.isString(url)) {
      throw new TypeError("Parameter 'url' must be a string, not " + typeof url);
    }
  
    // Copy chrome, IE, opera backslash-handling behavior.
    // Back slashes before the query string get converted to forward slashes
    // See: https://code.google.com/p/chromium/issues/detail?id=25916
    var queryIndex = url.indexOf('?'),
        splitter =
            (queryIndex !== -1 && queryIndex < url.indexOf('#')) ? '?' : '#',
        uSplit = url.split(splitter),
        slashRegex = /\\/g;
    uSplit[0] = uSplit[0].replace(slashRegex, '/');
    url = uSplit.join(splitter);
  
    var rest = url;
  
    // trim before proceeding.
    // This is to support parse stuff like "  http://foo.com  \n"
    rest = rest.trim();
  
    if (!slashesDenoteHost && url.split('#').length === 1) {
      // Try fast path regexp
      var simplePath = simplePathPattern.exec(rest);
      if (simplePath) {
        this.path = rest;
        this.href = rest;
        this.pathname = simplePath[1];
        if (simplePath[2]) {
          this.search = simplePath[2];
          if (parseQueryString) {
            this.query = querystring.parse(this.search.substr(1));
          } else {
            this.query = this.search.substr(1);
          }
        } else if (parseQueryString) {
          this.search = '';
          this.query = {};
        }
        return this;
      }
    }
  
    var proto = protocolPattern.exec(rest);
    if (proto) {
      proto = proto[0];
      var lowerProto = proto.toLowerCase();
      this.protocol = lowerProto;
      rest = rest.substr(proto.length);
    }
  
    // figure out if it's got a host
    // user@server is *always* interpreted as a hostname, and url
    // resolution will treat //foo/bar as host=foo,path=bar because that's
    // how the browser resolves relative URLs.
    if (slashesDenoteHost || proto || rest.match(/^\/\/[^@\/]+@[^@\/]+/)) {
      var slashes = rest.substr(0, 2) === '//';
      if (slashes && !(proto && hostlessProtocol[proto])) {
        rest = rest.substr(2);
        this.slashes = true;
      }
    }
  
    if (!hostlessProtocol[proto] &&
        (slashes || (proto && !slashedProtocol[proto]))) {
  
      // there's a hostname.
      // the first instance of /, ?, ;, or # ends the host.
      //
      // If there is an @ in the hostname, then non-host chars *are* allowed
      // to the left of the last @ sign, unless some host-ending character
      // comes *before* the @-sign.
      // URLs are obnoxious.
      //
      // ex:
      // http://a@b@c/ => user:a@b host:c
      // http://a@b?@c => user:a host:c path:/?@c
  
      // v0.12 TODO(isaacs): This is not quite how Chrome does things.
      // Review our test case against browsers more comprehensively.
  
      // find the first instance of any hostEndingChars
      var hostEnd = -1;
      for (var i = 0; i < hostEndingChars.length; i++) {
        var hec = rest.indexOf(hostEndingChars[i]);
        if (hec !== -1 && (hostEnd === -1 || hec < hostEnd))
          hostEnd = hec;
      }
  
      // at this point, either we have an explicit point where the
      // auth portion cannot go past, or the last @ char is the decider.
      var auth, atSign;
      if (hostEnd === -1) {
        // atSign can be anywhere.
        atSign = rest.lastIndexOf('@');
      } else {
        // atSign must be in auth portion.
        // http://a@b/c@d => host:b auth:a path:/c@d
        atSign = rest.lastIndexOf('@', hostEnd);
      }
  
      // Now we have a portion which is definitely the auth.
      // Pull that off.
      if (atSign !== -1) {
        auth = rest.slice(0, atSign);
        rest = rest.slice(atSign + 1);
        this.auth = decodeURIComponent(auth);
      }
  
      // the host is the remaining to the left of the first non-host char
      hostEnd = -1;
      for (var i = 0; i < nonHostChars.length; i++) {
        var hec = rest.indexOf(nonHostChars[i]);
        if (hec !== -1 && (hostEnd === -1 || hec < hostEnd))
          hostEnd = hec;
      }
      // if we still have not hit it, then the entire thing is a host.
      if (hostEnd === -1)
        hostEnd = rest.length;
  
      this.host = rest.slice(0, hostEnd);
      rest = rest.slice(hostEnd);
  
      // pull out port.
      this.parseHost();
  
      // we've indicated that there is a hostname,
      // so even if it's empty, it has to be present.
      this.hostname = this.hostname || '';
  
      // if hostname begins with [ and ends with ]
      // assume that it's an IPv6 address.
      var ipv6Hostname = this.hostname[0] === '[' &&
          this.hostname[this.hostname.length - 1] === ']';
  
      // validate a little.
      if (!ipv6Hostname) {
        var hostparts = this.hostname.split(/\./);
        for (var i = 0, l = hostparts.length; i < l; i++) {
          var part = hostparts[i];
          if (!part) continue;
          if (!part.match(hostnamePartPattern)) {
            var newpart = '';
            for (var j = 0, k = part.length; j < k; j++) {
              if (part.charCodeAt(j) > 127) {
                // we replace non-ASCII char with a temporary placeholder
                // we need this to make sure size of hostname is not
                // broken by replacing non-ASCII by nothing
                newpart += 'x';
              } else {
                newpart += part[j];
              }
            }
            // we test again with ASCII char only
            if (!newpart.match(hostnamePartPattern)) {
              var validParts = hostparts.slice(0, i);
              var notHost = hostparts.slice(i + 1);
              var bit = part.match(hostnamePartStart);
              if (bit) {
                validParts.push(bit[1]);
                notHost.unshift(bit[2]);
              }
              if (notHost.length) {
                rest = '/' + notHost.join('.') + rest;
              }
              this.hostname = validParts.join('.');
              break;
            }
          }
        }
      }
  
      if (this.hostname.length > hostnameMaxLen) {
        this.hostname = '';
      } else {
        // hostnames are always lower case.
        this.hostname = this.hostname.toLowerCase();
      }
  
      if (!ipv6Hostname) {
        // IDNA Support: Returns a punycoded representation of "domain".
        // It only converts parts of the domain name that
        // have non-ASCII characters, i.e. it doesn't matter if
        // you call it with a domain that already is ASCII-only.
        this.hostname = punycode.toASCII(this.hostname);
      }
  
      var p = this.port ? ':' + this.port : '';
      var h = this.hostname || '';
      this.host = h + p;
      this.href += this.host;
  
      // strip [ and ] from the hostname
      // the host field still retains them, though
      if (ipv6Hostname) {
        this.hostname = this.hostname.substr(1, this.hostname.length - 2);
        if (rest[0] !== '/') {
          rest = '/' + rest;
        }
      }
    }
  
    // now rest is set to the post-host stuff.
    // chop off any delim chars.
    if (!unsafeProtocol[lowerProto]) {
  
      // First, make 100% sure that any "autoEscape" chars get
      // escaped, even if encodeURIComponent doesn't think they
      // need to be.
      for (var i = 0, l = autoEscape.length; i < l; i++) {
        var ae = autoEscape[i];
        if (rest.indexOf(ae) === -1)
          continue;
        var esc = encodeURIComponent(ae);
        if (esc === ae) {
          esc = escape(ae);
        }
        rest = rest.split(ae).join(esc);
      }
    }
  
  
    // chop off from the tail first.
    var hash = rest.indexOf('#');
    if (hash !== -1) {
      // got a fragment string.
      this.hash = rest.substr(hash);
      rest = rest.slice(0, hash);
    }
    var qm = rest.indexOf('?');
    if (qm !== -1) {
      this.search = rest.substr(qm);
      this.query = rest.substr(qm + 1);
      if (parseQueryString) {
        this.query = querystring.parse(this.query);
      }
      rest = rest.slice(0, qm);
    } else if (parseQueryString) {
      // no query string, but parseQueryString still requested
      this.search = '';
      this.query = {};
    }
    if (rest) this.pathname = rest;
    if (slashedProtocol[lowerProto] &&
        this.hostname && !this.pathname) {
      this.pathname = '/';
    }
  
    //to support http.request
    if (this.pathname || this.search) {
      var p = this.pathname || '';
      var s = this.search || '';
      this.path = p + s;
    }
  
    // finally, reconstruct the href based on what has been validated.
    this.href = this.format();
    return this;
  };
  
  // format a parsed object into a url string
  function urlFormat(obj) {
    // ensure it's an object, and not a string url.
    // If it's an obj, this is a no-op.
    // this way, you can call url_format() on strings
    // to clean up potentially wonky urls.
    if (util.isString(obj)) obj = urlParse(obj);
    if (!(obj instanceof Url)) return Url.prototype.format.call(obj);
    return obj.format();
  }
  
  Url.prototype.format = function() {
    var auth = this.auth || '';
    if (auth) {
      auth = encodeURIComponent(auth);
      auth = auth.replace(/%3A/i, ':');
      auth += '@';
    }
  
    var protocol = this.protocol || '',
        pathname = this.pathname || '',
        hash = this.hash || '',
        host = false,
        query = '';
  
    if (this.host) {
      host = auth + this.host;
    } else if (this.hostname) {
      host = auth + (this.hostname.indexOf(':') === -1 ?
          this.hostname :
          '[' + this.hostname + ']');
      if (this.port) {
        host += ':' + this.port;
      }
    }
  
    if (this.query &&
        util.isObject(this.query) &&
        Object.keys(this.query).length) {
      query = querystring.stringify(this.query);
    }
  
    var search = this.search || (query && ('?' + query)) || '';
  
    if (protocol && protocol.substr(-1) !== ':') protocol += ':';
  
    // only the slashedProtocols get the //.  Not mailto:, xmpp:, etc.
    // unless they had them to begin with.
    if (this.slashes ||
        (!protocol || slashedProtocol[protocol]) && host !== false) {
      host = '//' + (host || '');
      if (pathname && pathname.charAt(0) !== '/') pathname = '/' + pathname;
    } else if (!host) {
      host = '';
    }
  
    if (hash && hash.charAt(0) !== '#') hash = '#' + hash;
    if (search && search.charAt(0) !== '?') search = '?' + search;
  
    pathname = pathname.replace(/[?#]/g, function(match) {
      return encodeURIComponent(match);
    });
    search = search.replace('#', '%23');
  
    return protocol + host + pathname + search + hash;
  };
  
  function urlResolve(source, relative) {
    return urlParse(source, false, true).resolve(relative);
  }
  
  Url.prototype.resolve = function(relative) {
    return this.resolveObject(urlParse(relative, false, true)).format();
  };
  
  function urlResolveObject(source, relative) {
    if (!source) return relative;
    return urlParse(source, false, true).resolveObject(relative);
  }
  
  Url.prototype.resolveObject = function(relative) {
    if (util.isString(relative)) {
      var rel = new Url();
      rel.parse(relative, false, true);
      relative = rel;
    }
  
    var result = new Url();
    var tkeys = Object.keys(this);
    for (var tk = 0; tk < tkeys.length; tk++) {
      var tkey = tkeys[tk];
      result[tkey] = this[tkey];
    }
  
    // hash is always overridden, no matter what.
    // even href="" will remove it.
    result.hash = relative.hash;
  
    // if the relative url is empty, then there's nothing left to do here.
    if (relative.href === '') {
      result.href = result.format();
      return result;
    }
  
    // hrefs like //foo/bar always cut to the protocol.
    if (relative.slashes && !relative.protocol) {
      // take everything except the protocol from relative
      var rkeys = Object.keys(relative);
      for (var rk = 0; rk < rkeys.length; rk++) {
        var rkey = rkeys[rk];
        if (rkey !== 'protocol')
          result[rkey] = relative[rkey];
      }
  
      //urlParse appends trailing / to urls like http://www.example.com
      if (slashedProtocol[result.protocol] &&
          result.hostname && !result.pathname) {
        result.path = result.pathname = '/';
      }
  
      result.href = result.format();
      return result;
    }
  
    if (relative.protocol && relative.protocol !== result.protocol) {
      // if it's a known url protocol, then changing
      // the protocol does weird things
      // first, if it's not file:, then we MUST have a host,
      // and if there was a path
      // to begin with, then we MUST have a path.
      // if it is file:, then the host is dropped,
      // because that's known to be hostless.
      // anything else is assumed to be absolute.
      if (!slashedProtocol[relative.protocol]) {
        var keys = Object.keys(relative);
        for (var v = 0; v < keys.length; v++) {
          var k = keys[v];
          result[k] = relative[k];
        }
        result.href = result.format();
        return result;
      }
  
      result.protocol = relative.protocol;
      if (!relative.host && !hostlessProtocol[relative.protocol]) {
        var relPath = (relative.pathname || '').split('/');
        while (relPath.length && !(relative.host = relPath.shift()));
        if (!relative.host) relative.host = '';
        if (!relative.hostname) relative.hostname = '';
        if (relPath[0] !== '') relPath.unshift('');
        if (relPath.length < 2) relPath.unshift('');
        result.pathname = relPath.join('/');
      } else {
        result.pathname = relative.pathname;
      }
      result.search = relative.search;
      result.query = relative.query;
      result.host = relative.host || '';
      result.auth = relative.auth;
      result.hostname = relative.hostname || relative.host;
      result.port = relative.port;
      // to support http.request
      if (result.pathname || result.search) {
        var p = result.pathname || '';
        var s = result.search || '';
        result.path = p + s;
      }
      result.slashes = result.slashes || relative.slashes;
      result.href = result.format();
      return result;
    }
  
    var isSourceAbs = (result.pathname && result.pathname.charAt(0) === '/'),
        isRelAbs = (
            relative.host ||
            relative.pathname && relative.pathname.charAt(0) === '/'
        ),
        mustEndAbs = (isRelAbs || isSourceAbs ||
                      (result.host && relative.pathname)),
        removeAllDots = mustEndAbs,
        srcPath = result.pathname && result.pathname.split('/') || [],
        relPath = relative.pathname && relative.pathname.split('/') || [],
        psychotic = result.protocol && !slashedProtocol[result.protocol];
  
    // if the url is a non-slashed url, then relative
    // links like ../.. should be able
    // to crawl up to the hostname, as well.  This is strange.
    // result.protocol has already been set by now.
    // Later on, put the first path part into the host field.
    if (psychotic) {
      result.hostname = '';
      result.port = null;
      if (result.host) {
        if (srcPath[0] === '') srcPath[0] = result.host;
        else srcPath.unshift(result.host);
      }
      result.host = '';
      if (relative.protocol) {
        relative.hostname = null;
        relative.port = null;
        if (relative.host) {
          if (relPath[0] === '') relPath[0] = relative.host;
          else relPath.unshift(relative.host);
        }
        relative.host = null;
      }
      mustEndAbs = mustEndAbs && (relPath[0] === '' || srcPath[0] === '');
    }
  
    if (isRelAbs) {
      // it's absolute.
      result.host = (relative.host || relative.host === '') ?
                    relative.host : result.host;
      result.hostname = (relative.hostname || relative.hostname === '') ?
                        relative.hostname : result.hostname;
      result.search = relative.search;
      result.query = relative.query;
      srcPath = relPath;
      // fall through to the dot-handling below.
    } else if (relPath.length) {
      // it's relative
      // throw away the existing file, and take the new path instead.
      if (!srcPath) srcPath = [];
      srcPath.pop();
      srcPath = srcPath.concat(relPath);
      result.search = relative.search;
      result.query = relative.query;
    } else if (!util.isNullOrUndefined(relative.search)) {
      // just pull out the search.
      // like href='?foo'.
      // Put this after the other two cases because it simplifies the booleans
      if (psychotic) {
        result.hostname = result.host = srcPath.shift();
        //occationaly the auth can get stuck only in host
        //this especially happens in cases like
        //url.resolveObject('mailto:local1@domain1', 'local2@domain2')
        var authInHost = result.host && result.host.indexOf('@') > 0 ?
                         result.host.split('@') : false;
        if (authInHost) {
          result.auth = authInHost.shift();
          result.host = result.hostname = authInHost.shift();
        }
      }
      result.search = relative.search;
      result.query = relative.query;
      //to support http.request
      if (!util.isNull(result.pathname) || !util.isNull(result.search)) {
        result.path = (result.pathname ? result.pathname : '') +
                      (result.search ? result.search : '');
      }
      result.href = result.format();
      return result;
    }
  
    if (!srcPath.length) {
      // no path at all.  easy.
      // we've already handled the other stuff above.
      result.pathname = null;
      //to support http.request
      if (result.search) {
        result.path = '/' + result.search;
      } else {
        result.path = null;
      }
      result.href = result.format();
      return result;
    }
  
    // if a url ENDs in . or .., then it must get a trailing slash.
    // however, if it ends in anything else non-slashy,
    // then it must NOT get a trailing slash.
    var last = srcPath.slice(-1)[0];
    var hasTrailingSlash = (
        (result.host || relative.host || srcPath.length > 1) &&
        (last === '.' || last === '..') || last === '');
  
    // strip single dots, resolve double dots to parent dir
    // if the path tries to go above the root, `up` ends up > 0
    var up = 0;
    for (var i = srcPath.length; i >= 0; i--) {
      last = srcPath[i];
      if (last === '.') {
        srcPath.splice(i, 1);
      } else if (last === '..') {
        srcPath.splice(i, 1);
        up++;
      } else if (up) {
        srcPath.splice(i, 1);
        up--;
      }
    }
  
    // if the path is allowed to go above the root, restore leading ..s
    if (!mustEndAbs && !removeAllDots) {
      for (; up--; up) {
        srcPath.unshift('..');
      }
    }
  
    if (mustEndAbs && srcPath[0] !== '' &&
        (!srcPath[0] || srcPath[0].charAt(0) !== '/')) {
      srcPath.unshift('');
    }
  
    if (hasTrailingSlash && (srcPath.join('/').substr(-1) !== '/')) {
      srcPath.push('');
    }
  
    var isAbsolute = srcPath[0] === '' ||
        (srcPath[0] && srcPath[0].charAt(0) === '/');
  
    // put the host back
    if (psychotic) {
      result.hostname = result.host = isAbsolute ? '' :
                                      srcPath.length ? srcPath.shift() : '';
      //occationaly the auth can get stuck only in host
      //this especially happens in cases like
      //url.resolveObject('mailto:local1@domain1', 'local2@domain2')
      var authInHost = result.host && result.host.indexOf('@') > 0 ?
                       result.host.split('@') : false;
      if (authInHost) {
        result.auth = authInHost.shift();
        result.host = result.hostname = authInHost.shift();
      }
    }
  
    mustEndAbs = mustEndAbs || (result.host && srcPath.length);
  
    if (mustEndAbs && !isAbsolute) {
      srcPath.unshift('');
    }
  
    if (!srcPath.length) {
      result.pathname = null;
      result.path = null;
    } else {
      result.pathname = srcPath.join('/');
    }
  
    //to support request.http
    if (!util.isNull(result.pathname) || !util.isNull(result.search)) {
      result.path = (result.pathname ? result.pathname : '') +
                    (result.search ? result.search : '');
    }
    result.auth = relative.auth || result.auth;
    result.slashes = result.slashes || relative.slashes;
    result.href = result.format();
    return result;
  };
  
  Url.prototype.parseHost = function() {
    var host = this.host;
    var port = portPattern.exec(host);
    if (port) {
      port = port[0];
      if (port !== ':') {
        this.port = port.substr(1);
      }
      host = host.substr(0, host.length - port.length);
    }
    if (host) this.hostname = host;
  };
  
  },{"./util":76,"punycode":33,"querystring":36}],76:[function(require,module,exports){
  'use strict';
  
  module.exports = {
    isString: function(arg) {
      return typeof(arg) === 'string';
    },
    isObject: function(arg) {
      return typeof(arg) === 'object' && arg !== null;
    },
    isNull: function(arg) {
      return arg === null;
    },
    isNullOrUndefined: function(arg) {
      return arg == null;
    }
  };
  
  },{}],77:[function(require,module,exports){
  (function (global){
  
  /**
   * Module exports.
   */
  
  module.exports = deprecate;
  
  /**
   * Mark that a method should not be used.
   * Returns a modified function which warns once by default.
   *
   * If `localStorage.noDeprecation = true` is set, then it is a no-op.
   *
   * If `localStorage.throwDeprecation = true` is set, then deprecated functions
   * will throw an Error when invoked.
   *
   * If `localStorage.traceDeprecation = true` is set, then deprecated functions
   * will invoke `console.trace()` instead of `console.error()`.
   *
   * @param {Function} fn - the function to deprecate
   * @param {String} msg - the string to print to the console when `fn` is invoked
   * @returns {Function} a new "deprecated" version of `fn`
   * @api public
   */
  
  function deprecate (fn, msg) {
    if (config('noDeprecation')) {
      return fn;
    }
  
    var warned = false;
    function deprecated() {
      if (!warned) {
        if (config('throwDeprecation')) {
          throw new Error(msg);
        } else if (config('traceDeprecation')) {
          console.trace(msg);
        } else {
          console.warn(msg);
        }
        warned = true;
      }
      return fn.apply(this, arguments);
    }
  
    return deprecated;
  }
  
  /**
   * Checks `localStorage` for boolean values for the given `name`.
   *
   * @param {String} name
   * @returns {Boolean}
   * @api private
   */
  
  function config (name) {
    // accessing global.localStorage can trigger a DOMException in sandboxed iframes
    try {
      if (!global.localStorage) return false;
    } catch (_) {
      return false;
    }
    var val = global.localStorage[name];
    if (null == val) return false;
    return String(val).toLowerCase() === 'true';
  }
  
  }).call(this,typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
  },{}],78:[function(require,module,exports){
  arguments[4][2][0].apply(exports,arguments)
  },{"dup":2}],79:[function(require,module,exports){
  arguments[4][3][0].apply(exports,arguments)
  },{"dup":3}],80:[function(require,module,exports){
  arguments[4][4][0].apply(exports,arguments)
  },{"./support/isBuffer":79,"_process":32,"dup":4,"inherits":78}],81:[function(require,module,exports){
  module.exports = extend
  
  var hasOwnProperty = Object.prototype.hasOwnProperty;
  
  function extend() {
      var target = {}
  
      for (var i = 0; i < arguments.length; i++) {
          var source = arguments[i]
  
          for (var key in source) {
              if (hasOwnProperty.call(source, key)) {
                  target[key] = source[key]
              }
          }
      }
  
      return target
  }
  
  },{}],82:[function(require,module,exports){
  (function (process,Buffer){
  /**
   * REQUIRED MODULES
   */
  const axios = require('axios'); //Promise based HTTP client
  const httpAdapter = require('axios/lib/adapters/http');
  const colors = require('colors'); //This is to do colors on the console
  const figlet = require('figlet'); /* ASCII Art for BIG Texts */
  figlet.defaults({fontPath: window.lichess.assetUrl('font')});
  //window.lichess.assetUrl('images/mascot/octopus.svg')
  const tts = require('./text-to-speech-browser'); /* Browser version of text to speech */
  const { Chess } = require('chess.js'); /* An open source chessboard management from https://github.com/jhlywa/chess.js/ */
  const BoardManager = require('./board-manager'); /* This will manage input from the DGT Board */
  //This is to get input from the console, ideally wont be necessary since input will be received from the board
  
  //Commented since it broke the browser version
  //const readline = require('readline').createInterface({
  //    input: process.stdin,
  //    output: process.stdout
  //})
  
  /**
   * CONFIGURATION VALUES
   */
  var nconf = require('./bconf');
  const baseURL = nconf.get('baseURL');
  //axios.defaults.proxy = { host: "127.0.0.1", port:8888}
  const personalToken = nconf.get('personalToken');
  var verbose = Boolean(nconf.get('dgt-verbose')); //Verbose on or off
  var announceAllMoves = Boolean(nconf.get('dgt-speech-announce-all-moves')); //Announce moves for both players or only the opponents
  const announceMoveFormat = nconf.get('dgt-speech-announce-move-format');
  const keywords = nconf.get('dgt-speech-keywords');
  const keywordsBase = ["K", "Q", "R", "B", "N", "P", "x", "+", "#", "(=)", "O-O-O", "O-O", "white", "black", "wins by", "timeout", "resignation"]
  if (verbose) console.log("Runnig in verbose mode");
  /**
   * GLOBAL VATIABLES
   */
  var time = new Date(); //A Global time object
  var currentGameId = ''; //Track which is the current Game, in case there are several open games
  var currentGameColor = ''; //Track which color is being currently played by the player
  var me; //Track my information
  var gameInfoMap = new Map(); //A collection of key values to store game inmutable information of all open games
  var gameStateMap = new Map(); //A collection of key values to store the changing state of all open games
  var gameConnectionMap = new Map(); //A collection of key values to store the network status of a game
  var gameChessBoardMap = new Map(); //A collection of Chess Boads representing the current board of the games
  var eventSteamStatus = { connected: false, lastEvent: time.getTime() }; //An object to store network status of the main eventStream
  const dgtBoard = new BoardManager(); //Store the board manager object
  var chatArray = [{
      "type": "chatLine",
      "username": "andrescavallin",
      "text": "Please be nice in the chat!",
      "room": "spectator"
  }]
  
  /**
   * This make axios treat all http reponses as valid and don't throw an error so its easier to see error body
   * This was added because some useful 4xx error messages were not shown
   */
  axios.defaults.validateStatus = function () {
      return true;
  };
  
  
  /**
   * GET /api/account
   * 
   * Get my profile
   * 
   * Shows Public informations about the logged in user.
   * 
   * Example:
  {
    id: 'andrescavallin',
    username: 'andrescavallin',
    online: true,
    perfs: {
      blitz: { games: 0, rating: 1500, rd: 350, prog: 0, prov: true },
      bullet: { games: 0, rating: 1500, rd: 350, prog: 0, prov: true },
      correspondence: { games: 0, rating: 1500, rd: 350, prog: 0, prov: true },
      classical: { games: 0, rating: 1500, rd: 350, prog: 0, prov: true },
      rapid: { games: 3, rating: 1362, rd: 178, prog: 0, prov: true }
    },
    createdAt: 1581950312761,
    seenAt: 1587182372646,
    playTime: { total: 3737, tv: 0 },
    language: 'en-US',
    url: 'https://lichess.org/@/andrescavallin',
    playing: 'https://lichess.org/JgauZ9M2/white',
    nbFollowing: 1,
    nbFollowers: 1,
    count: {
      all: 10,
      rated: 3,
      ai: 1,
      draw: 1,
      drawH: 1,
      loss: 6,
      lossH: 5,
      win: 3,
      winH: 3,
      bookmark: 0,
      playing: 1,
      import: 0,
      me: 0
    },
    followable: true,
    following: false,
    blocking: false,
    followsYou: false
  }
   */
  function getProfile() {
      //Log intention
      if (verbose) console.log(colors.dim.grey('getProfile - About to call /api/account'));
      axios.get('/api/account', {
          baseURL: baseURL,
          headers: { 'Authorization': 'Bearer ' + personalToken }
      }).then(
          function (response) {
              //Log raw data received
              if (verbose) console.log(colors.dim.grey('/api/account Response:' + JSON.stringify(response.data)));
              //Diplay Title + UserName . Title may be undefined
              console.log("\n");
              console.log("┌─────────────────────────────────────────────────────┐");
              console.log("│ " + colors.bold.white((typeof response.data.title !== "undefined") ? response.data.title : '') + colors.white(' ' + response.data.username));
              //Display performance ratings
              console.table(response.data.perfs);
              //Store my profile
              me = response.data;
              //Update on browser - Only works on browser
              /*
              try {
                  document.getElementById("user_tag").innerText = response.data.username;
              }
              catch (error) {
                  console.error(error.message);
                  //Do nothing
              }
              */
          },
          err => console.error('getProfile - Error. '.red + err.message));
  }
  
  /** 
  GET /api/stream/event
  Stream incoming events
  
  Stream the events reaching a lichess user in real time as ndjson.
  
  Each line is a JSON object containing a type field. Possible values are:
  
  challenge Incoming challenge
  gameStart Start of a game
  When the stream opens, all current challenges and games are sent.
  
  Examples:
  {"type":"gameStart","game":{"id":"kjKzl2MO"}}
  {"type":"challenge","challenge":{"id":"WTr3JNcm","status":"created","challenger":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1362,"provisional":true,"online":true,"lag":3},"destUser":{"id":"godking666","name":"Godking666","title":null,"rating":1910,"online":true,"lag":3},"variant":{"key":"standard","name":"Standard","short":"Std"},"rated":false,"speed":"rapid","timeControl":{"type":"clock","limit":900,"increment":10,"show":"15+10"},"color":"white","perf":{"icon":"#","name":"Rapid"}}}
  
  * @param {string} personalToken - The secret token used for authentication on lichess.org
   */
  function connectToEventStream(personalToken) {
      //Log intention
      if (verbose) console.log(colors.dim.grey('connectToEventStream - About to call /api/stream/event'));
      axios.get('/api/stream/event', {
          baseURL: baseURL,
          headers: { 'Authorization': 'Bearer ' + personalToken },
          responseType: 'stream',
          adapter: httpAdapter
      }).then((response) => {
          const stream = response.data;
          stream.on('data', (chunk /* chunk is an ArrayBuffer */) => {
              //Log raw data received
              if (verbose) console.log(colors.dim.grey('connectToEventStream - stream event recevied:' + Buffer.from(chunk)));
              //Update connection status
              eventSteamStatus = { connected: true, lastEvent: time.getTime() };
              //Response may contain several JSON objects on the same chunk separated by \n . This may create an empty element at the end.
              var JSONArray = Decodeuint8arr(chunk).split('\n');
              for (i = 0; i < JSONArray.length; i++) {
                  //Skip empty elements that may have happened witht the .split('\n')
                  if (JSONArray[i].length > 2) {
                      try {
                          var data = JSON.parse(JSONArray[i]);
                          //JSON data found, let's check if this is a game that started. field type is mandatory except on http 4xx
                          if (data.type == "gameStart") {
                              if (verbose) console.log(colors.dim.gray('connectToEventStream - gameStart event arrived. GameId: ' + data.game.id));
                              try {
                                  //Connect to that game's stream
                                  connectToGameStream(data.game.id);
                              }
                              catch (error) {
                                  //This will trigger if connectToGameStream fails
                                  console.log('connectToEventStream - Failed to connect to game stream.'.red + Error(error).message);
                              }
                          }
                          else if (data.type == "challenge") {
                              //Challenge received
                              //TODO
                          }
                          else if (response.status >= 400) {
                              console.log(colors.yellow('connectToEventStream - ' + data.error));
                          }
                      }
                      catch (error) {
                          console.log('connectToEventStream - Unable to parse JSON or Unexpected error. '.red + Error(error).message);
                      }
                  }
                  else {
                      //Signal that some empty message arrived. This is normal to keep the connection alive.
                      if (verbose) console.log("*"); //process.stdout.write("*"); Replace to support browser
                  }
              }
          });
          stream.on('end', () => {
              //End Stream output.end();
              console.log('connectToEventStream - Event Stream ended.'.yellow);
              //Update connection status
              eventSteamStatus = { connected: false, lastEvent: time.getTime() };
  
          });
      });
  }
  
  
  /**
  Stream Board game state
   
  GET /api/board/game/stream/{gameId}
   
  Stream the state of a game being played with the Board API, as ndjson.
  Use this endpoint to get updates about the game in real-time, with a single request.
  Each line is a JSON object containing a type field. Possible values are:
   
  gameFull Full game data. All values are immutable, except for the state field.
  gameState Current state of the game. Immutable values not included. Sent when a move is played, a draw is offered, or when the game ends.
  chatLine Chat message sent by a user in the room "player" or "spectator".
  The first line is always of type gameFull.
   
  Examples:
   
  New Game
  {"id":"972RKuuq","variant":{"key":"standard","name":"Standard","short":"Std"},"clock":{"initial":900000,"increment":10000},"speed":"rapid","perf":{"name":"Rapid"},"rated":false,"createdAt":1586647003562,"white":{"id":"godking666","name":"Godking666","title":null,"rating":1761},"black":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1362,"provisional":true},"initialFen":"startpos","type":"gameFull","state":{"type":"gameState","moves":"e2e4","wtime":900000,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}}
  First Move
  {"type":"gameState","moves":"e2e4","wtime":900000,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}
  Middle Game
  {"type":"gameState","moves":"e2e4 c7c6 g1f3 d7d5 e4e5 c8f5 d2d4 e7e6 h2h3 f5e4 b1d2 f8b4 c2c3 b4a5 d2e4 d5e4 f3d2 d8h4 g2g3 h4e7 d2e4 e7d7 e4d6 e8f8 d1f3 g8h6 c1h6 h8g8 h6g5 a5c7 e1c1 c7d6 e5d6 d7d6 g5f4 d6d5 f3d5 c6d5 f4d6 f8e8 d6b8 a8b8 f1b5 e8f8 h1e1 f8e7 d1d3 a7a6 b5a4 g8c8 a4b3 b7b5 b3d5 e7f8","wtime":903960,"btime":847860,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"started"}
  After reconnect
  {"id":"ZQDjy4sa","variant":{"key":"standard","name":"Standard","short":"Std"},"clock":{"initial":900000,"increment":10000},"speed":"rapid","perf":{"name":"Rapid"},"rated":true,"createdAt":1586643869056,"white":{"id":"gg60","name":"gg60","title":null,"rating":1509},"black":{"id":"andrescavallin","name":"andrescavallin","title":null,"rating":1433,"provisional":true},"initialFen":"startpos","type":"gameFull","state":{"type":"gameState","moves":"e2e4 c7c6 g1f3 d7d5 e4e5 c8f5 d2d4 e7e6 h2h3 f5e4 b1d2 f8b4 c2c3 b4a5 d2e4 d5e4 f3d2 d8h4 g2g3 h4e7 d2e4 e7d7 e4d6 e8f8 d1f3 g8h6 c1h6 h8g8 h6g5 a5c7 e1c1 c7d6 e5d6 d7d6 g5f4 d6d5 f3d5 c6d5 f4d6 f8e8 d6b8 a8b8 f1b5 e8f8 h1e1 f8e7 d1d3 a7a6 b5a4 g8c8 a4b3 b7b5 b3d5 e7f8 d5b3 a6a5 a2a3 a5a4 b3a2 f7f6 e1e6 f8f7 e6b6","wtime":912940,"btime":821720,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"resign","winner":"white"}}
  Draw Offered
  {"type":"gameState","moves":"e2e4 c7c6","wtime":880580,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":true,"status":"started"}
  After draw accepted
  {"type":"gameState","moves":"e2e4 c7c6","wtime":865460,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"draw"}
  Out of Time
  {"type":"gameState","moves":"e2e3 e7e5","wtime":0,"btime":900000,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"outoftime","winner":"black"}
  Mate
  {"type":"gameState","moves":"e2e4 e7e5 f1c4 d7d6 d1f3 b8c6 f3f7","wtime":900480,"btime":907720,"winc":10000,"binc":10000,"wdraw":false,"bdraw":false,"status":"mate"}
  Promotion
  {"type":"gameState","moves":"e2e4 b8c6 g1f3 c6d4 f1c4 e7e5 d2d3 d7d5 f3d4 f7f6 c4d5 f6f5 f2f3 g7g6 e1g1 c7c6 d5b3 d8d5 e4d5 a8b8 d4e6 f8b4 e6c7 e8e7 d5d6 e7f6 d6d7 b4f8 d7d8q","wtime":2147483647,"btime":2147483647,"winc":0,"binc":0,"wdraw":false,"bdraw":false,"status":"started"}
  @param {string} gameId - The alphanumeric identifier of the game to be tracked
   */
  
  function connectToGameStream(gameId) {
      //Log intention
      if (verbose) console.log(colors.dim.grey('connectToGameStream - About to call /api/board/game/stream/' + gameId));
      //gameId = 'ZQDjy4sa';
      //Now hook the on data event to the stream
      axios.get('/api/board/game/stream/' + gameId, {
          baseURL: baseURL,
          headers: { 'Authorization': 'Bearer ' + personalToken },
          responseType: 'stream',
          adapter: httpAdapter
      })
          .then((response) => {
              const stream = response.data;
              stream.on('data', (chunk /* chunk is an ArrayBuffer */) => {
                  //Log raw data received
                  if (verbose) console.log(colors.dim.grey('connectToGameStream - board game stream recevied:' + Buffer.from(chunk)));
                  //Update connection status
                  gameConnectionMap.set(gameId, { connected: true, lastEvent: time.getTime() });
                  //Response may contain several JSON objects on the same chunk separated by \n . This may create an empty element at the end.
                  var JSONArray = Decodeuint8arr(chunk).split('\n');
                  for (i = 0; i < JSONArray.length; i++) {
                      //Skip empty elements that may have happened witht the .split('\n')
                      if (JSONArray[i].length > 2) {
                          try {
                              var data = JSON.parse(JSONArray[i]);
                              //The first line is always of type gameFull.
                              if (data.type == "gameFull") {
                                  if (!verbose) console.clear();
                                  //Log game Summary 
                                  //logGameSummary(data);
                                  //Store game inmutable information on the gameInfoMap dictionary collection 
                                  gameInfoMap.set(gameId, data);
                                  //Store game state on the gameStateMap dictionary collection
                                  gameStateMap.set(gameId, data.state);
                                  //Update the ChessBoard to the ChessBoard Map
                                  initializeChessBoard(gameId, data);
                                  //Log the state. Note that we are doing this after storing the state and initializing the chess.js board
                                  logGameState(gameId);
                                  //Show prompt
                                  console.log('Enter command and press enter >');
                                  //Call chooseCurrentGame to determine if this stream will be the new current game
                                  chooseCurrentGame();
                              }
                              else if (data.type == "gameState") {
                                  if (!verbose) console.clear();
                                  //Update the ChessBoard Map
                                  updateChessBoard(gameId, gameStateMap.get(gameId), data);
                                  //Update game state with most recent state
                                  gameStateMap.set(gameId, data);
                                  //Log the state. Note that we are doing this after storing the state and updating the chess.js board
                                  logGameState(gameId);
                                  //Show prompt
                                  console.log('Enter command and press enter >');
                              }
                              else if (data.type == "chatLine") {
                                  //Received chat line
                                  //TODO
                              }
                              else if (response.status >= 400) {
                                  console.log(colors.yellow('connectToGameStream - ' + data.error));
                              }
                          }
                          catch (error) {
                              console.log('connectToGameStream - No valid game data or Unexpected error. '.red + Error(error).message);
                          }
                      }
                      else {
                          //Signal that some empty message arrived
                          if (verbose) console.log(":"); //process.stdout.write(":"); Changed to support browser
                      }
                  }
              });
              stream.on('end', () => {
                  //End Stream output.end();
                  console.log('connectToGameStream - Game ' + gameId + ' Stream ended.'.yellow);
                  //Update connection state
                  gameConnectionMap.set(gameId, { connected: false, lastEvent: time.getTime() });
                  //Now if the connection was closed but the game is not over, reconnect
                  /*
                  if (gameStateMap.get(gameId).status == 'started')
                  {
                      connectToGameStream(gameId);
                  }
                  */
              });
          })
          .catch(function (error) {
              console.log(error);
              if (error.response)
                  console.log(error.response.data);
          });
  }
  
  /**
   * Make a Board move
   * 
   * /api/board/game/{gameId}/move/{move}
   * 
   * Make a move in a game being played with the Board API.
   * The move can also contain a draw offer/agreement.
   * 
   * @param {string} gameId 
   * @param {string} move 
   */
  function sendMove(gameId, move) {
      if (move.length > 1) {
          //Automatically decline draws when making a move
          var url = url = `${baseURL}/api/board/game/${gameId}/move/${move}?offeringDraw=false`
          //Log intention
          if (verbose) console.log(colors.dim.grey('sendMove - About to call ' + url));
          axios({
              method: "post",
              url: url,
              headers: { 'Authorization': 'Bearer ' + personalToken }
          })
              .then(function (response) {
                  try {
                      if (response.status == 200 || response.status == 201) {
                          //Move sucessfully sent
                          if (verbose) console.log(colors.dim.grey('sendMove - Move sucessfully sent.'));
                      }
                      else {
                          console.error('sendMove - Failed to send move. '.red + response.data.error);
                      }
  
                  }
                  catch (error) {
                      console.error('sendMove - Unexpected error. '.red + error);
                  }
              })
              .catch(function (error) {
                  console.error('sendMove - Error. '.red + error.message);
              });
      }
      else {
          if (verbose) console.log(colors.dim.grey(`sendMove - Received move: "${move}" will not be sent to lichess `));
      }
  }
  
  /**
   * Display the summary of the Game excluding the state
   * 
   * @param {string} gameInfo - The alphanumeric identifier of the game to be shown
   */
  function logGameSummary(gameInfo) {
      console.log(""); //process.stdout.write("\n"); Changed to support browser
      console.table([
          { white: ((gameInfo.white.title !== null) ? gameInfo.white.title : '@'), black: ((gameInfo.black.title !== null) ? gameInfo.black.title : '@'), game: 'Id: ' + gameInfo.id },
          { white: gameInfo.white.name, black: gameInfo.black.name, game: gameInfo.variant.short + ' ' + (gameInfo.rated ? 'rated' : 'unrated') },
          { white: gameInfo.white.rating, black: gameInfo.black.rating, game: gameInfo.speed + ' ' + ((gameInfo.clock !== null) ? (String(gameInfo.clock.initial / 60000) + "'+" + String(gameInfo.clock.increment / 1000) + "''") : '∞') }]);
  }
  
  /**
   * Display the state as stored in the Dictionary collection
   *
   * @param {string} gameId - The alphanumeric identifier of the game for which state is going to be shown
   */
  function logGameState(gameId) {
      if (gameStateMap.has(gameId) && gameInfoMap.has(gameId)) {
          var gameInfo = gameInfoMap.get(gameId);
          var gameState = gameStateMap.get(gameId);
          var lastMove = getLastMove(gameId);
          console.log(""); //process.stdout.write("\n"); Changed to support browser
          console.table({
              'Title': { white: ((gameInfo.white.title !== null) ? gameInfo.white.title : '@'), black: ((gameInfo.black.title !== null) ? gameInfo.black.title : '@'), game: 'Id: ' + gameInfo.id },
              'Username': { white: gameInfo.white.name, black: gameInfo.black.name, game: 'Status: ' + gameState.status },
              'Rating': { white: gameInfo.white.rating, black: gameInfo.black.rating, game: gameInfo.variant.short + ' ' + (gameInfo.rated ? 'rated' : 'unrated') },
              'Timer': { white: formattedTimer(gameState.wtime), black: formattedTimer(gameState.btime), game: gameInfo.speed + ' ' + ((gameInfo.clock !== null) ? (String(gameInfo.clock.initial / 60000) + "'+" + String(gameInfo.clock.increment / 1000) + "''") : '∞') },
              'Last Move': { white: (lastMove.player == 'white' ? lastMove.move : '?'), black: (lastMove.player == 'black' ? lastMove.move : '?'), game: lastMove.player },
          });
          switch (gameState.status) {
              case "started":
                  //Announce the last move
                  if (me.id !== lastMove.by || announceAllMoves) {
                      announcePlay(lastMove, gameState.wtime, gameState.btime);
                  }
                  break;
              case "outoftime":
                  //announceWinner(gameState.winner, 'flag', gameState.winner + ' wins by timeout');
                  announceWinner(keywords[gameState.winner], 'flag', keywords[gameState.winner] + ' ' + keywords['wins by'] + ' ' + keywords['timeout']);
                  break;
              case "resign":
                  var winner = gameState.winner;
                  //announceWinner(gameState.winner, 'resign', gameState.winner + ' wins by resignation');
                  announceWinner(keywords[gameState.winner], 'resign', keywords[gameState.winner] + ' ' + keywords['wins by'] + ' ' + keywords['resignation']);
                  break;
              case "mate":
                  //announceWinner(lastMove.player, 'mate', lastMove.player + ' wins by checkmate');
                  announceWinner(keywords[lastMove.player], 'mate', keywords[lastMove.player] + ' ' + keywords['wins by'] + ' ' + keywords['#']);
                  break;
              case "draw":
                  //announceWinner('draw', 'draw', 'game ends in draw');
                  announceWinner('draw', 'draw', keywords['(=)']);
                  break;
              default:
                  console.log(`Unknown status received: ${gameState.status}`.red);
              // code block
          }
      }
  }
  
  
  /**
   * Peeks a game state and calculates who played the last move and what move it was
   * 
   * @param {string} gameId - The alphanumeric identifier of the game where the last move is going to be calculated
   */
  function getLastMove(gameId) {
      if (gameStateMap.has(gameId) && gameInfoMap.has(gameId)) {
          var gameInfo = gameInfoMap.get(gameId);
          var gameState = gameStateMap.get(gameId);
  
          if (announceMoveFormat.toLowerCase() == "san" && gameChessBoardMap.get(gameId).history().length > 0) {
              //Last move came from Lichess use the last game state . Even if move is played on board, we will wait until it comes from lichess
              var lastSAN = gameChessBoardMap.get(gameId).history({ verbose: true })[gameChessBoardMap.get(gameId).history().length - 1];
              return { player: (lastSAN.color == "w" ? "white" : "black"), move: lastSAN.san, by: gameInfo[(lastSAN.color == "w" ? "white" : "black")].id };
          }
          else {
              //This is the original code that does not used chess.js objects and can be used to get the UCI move but not SAN.
              if (String(gameState.moves).length > 1) {
                  moves = gameState.moves.split(' ');
                  if (moves.length % 2 == 0)
                      return { player: 'black', move: moves[moves.length - 1], by: gameInfo.black.id };
                  else
                      return { player: 'white', move: moves[moves.length - 1], by: gameInfo.white.id };
              }
              else {
                  return { player: 'none', move: 'none' };
              }
          }
      }
  }
  
  /**
   * Convert an Uint8Array into a string.
   * Useful to parse upcoming chunks of data
   *
   * @returns {String}
   */
  function Decodeuint8arr(uint8array) {
      return new TextDecoder("utf-8").decode(uint8array);
  }
  
  /**
   * Wait some time without blocking other code
   *
   * @param {number} ms - The number of milliseconds to sleep
   */
  function sleep(ms = 0) {
      return new Promise(r => setTimeout(r, ms));
  }
  
  /**
   * Return a string representation of the remaining time on the clock
   * 
   * @param {number} timer - Numeric representation of remaining time
   * 
   * @returns {String} - String representation of numeric time
   */
  function formattedTimer(timer) {
      // Pad function to pad with 0 to 2 or 3 digits, default is 2
      var pad = (n, z = 2) => ('00' + n).slice(-z);
      return pad(timer / 3.6e6 | 0) + ':' + pad((timer % 3.6e6) / 6e4 | 0) + ':' + pad((timer % 6e4) / 1000 | 0) //+ '.' + pad(timer % 1000, 3);
  }
  
  
  /**
   * mainLoop() is a function that tries to keep the streams connected at all times, up to a maximum of 20 retries
   */
  async function mainLoop() {
      //Program ends after 20 re-connection attempts
      for (let attempts = 0; attempts < 20; attempts++) {
          //Connect to main event stream
          connectToEventStream(personalToken);
          //On the first time, if there are no games, it may take several seconds to receive data so lets wait a bit. Also give some time to connect to started games
          await sleep(5000);
          //Now enter a loop to monitor the connection
          do {
              //sleep 5 seconds and just listen to events
              await sleep(5000);
              //Check if any started games are disconnected
              for (let [gameId, networkState] of gameConnectionMap) {
                  if (!networkState.connected && gameStateMap.get(gameId).status == "started") {
                      //Game is not conencted and has not finished, reconnect
                      if (verbose) console.log(colors.dim.grey(`Started game is disconnected. Attempting reconnection for gameId: ${gameId}`));
                      connectToGameStream(gameId);
                  }
              }
          }
          while (eventSteamStatus.connected)
          //This means event stream is not connected
          console.warn("No connection to event stream. Attempting re-connection. Attempt: " + attempts);
      }
      console.error("No connection to event stream after maximum number of attempts 20. Exiting application");
      process.exit(0); //Success
  }
  
  /**
   * Iterate the gameConnectionMap dictionary and return an arrays containing only the games that can be played with the board
   * @returns {Array} - Array containing a summary of playable games
   */
  function playableGamesArray() {
      var playableGames = [];
      var keys = Array.from(gameConnectionMap.keys());
      //The for each iterator is not used since we don't want to continue execution. We want a syncrhonous result
      //for (let [gameId, networkState] of gameConnectionMap) {
      //    if (gameConnectionMap.get(gameId).connected && gameStateMap.get(gameId).status == "started") {    
      for (var i = 0; i < keys.length; i++) {
          if (gameConnectionMap.get(keys[i]).connected && gameStateMap.get(keys[i]).status == "started") {
              //Game is good for commands
              var gameInfo = gameInfoMap.get(keys[i]);
              var gameState = gameStateMap.get(keys[i]);
              var lastMove = getLastMove(keys[i]);
              var versus = (gameInfo.black.id == me.id) ? ((gameInfo.white.title !== null) ? gameInfo.white.title : '@') + ' ' + gameInfo.white.name : ((gameInfo.black.title !== null) ? gameInfo.black.title : '@') + ' ' + gameInfo.black.name;
              //Since console width may vary, here are 3 versions of the Game Info Summary presented to the user <=100    <=130    >130
              var columns = 140; //Changed to support browsers process.stdout.columns
              if (columns <= 110) {
                  playableGames.push({
                      'gameId': gameInfo.id,
                      'versus': versus,
                      'variant': gameInfo.variant.short,
                      'Timer': gameInfo.speed,
                      'Last Move': lastMove.player + ' ' + lastMove.move
                  })
              }
              else if (columns <= 130) {
                  playableGames.push({
                      'gameId': gameInfo.id,
                      'versus': versus,
                      'game rating': gameInfo.variant.short + ' ' + (gameInfo.rated ? 'rated' : 'unrated'),
                      'Timer': gameInfo.speed,
                      'Last Move': lastMove.player + ' ' + lastMove.move + ' by ' + lastMove.by
                  })
              }
              else {
                  playableGames.push({
                      'gameId': gameInfo.id,
                      'versus': versus,
                      'vs rating': (gameInfo.black.id == me.id) ? gameInfo.white.rating : gameInfo.black.rating,
                      'game rating': gameInfo.variant.short + ' ' + (gameInfo.rated ? 'rated' : 'unrated'),
                      'Timer': gameInfo.speed + ' ' + ((gameInfo.clock !== null) ? (String(gameInfo.clock.initial / 60000) + "'+" + String(gameInfo.clock.increment / 1000) + "''") : '∞'),
                      'Last Move': lastMove.player + ' ' + lastMove.move + ' by ' + lastMove.by
                  })
              }
          }
      }
      return playableGames;
  }
  
  function readLineAsync(prompt) {
      return new Promise((resolve, reject) => {
          /*        readline.question(prompt, (answer) => {
                      resolve(answer);
                  });*/
      });
  }
  
  async function keyboardInputHandler() {
      do {
          //Register event for text input from console and wait for input without blocking execution
          var command = await readLineAsync("Enter command and press enter >");
          if (verbose) console.log(colors.dim.grey(`keyboardInputHandler - Keyboard input: ${command}`));
          //Find and active game, don't keep trying to find one since keyboard commands can be sent again . Also the command may be a quit
          if (!(gameStateMap.has(currentGameId) && gameConnectionMap.get(currentGameId).connected && gameStateMap.get(currentGameId).status == "started")) {
              await chooseCurrentGame();
          }
          //Now send the move as written, no validation
          sendMove(currentGameId, command);
      } while (String(command).toLowerCase() != 'end' && String(command).toLowerCase() != 'exit' && String(command).toLowerCase() != 'quit')
      readline.close();
      if (verbose) console.log(colors.dim.grey('Exiting...'));
      process.exit(0); //Success
  }
  
  function announcePlay(lastMove, wtime, btime) {
      if (lastMove.player == 'white') {
          figlet.text('  ' + lastMove.move + '  ', { font: 'univers', horizontalLayout: 'full' }, function (err, data) {
              if (err) {
                  console.log('Something went wrong...' + err.message);
                  return;
              }
              console.log(colors.bgWhite.black(data));
          });
          console.log(colors.bgWhite.black('                             W H I T E                             '));
      }
      else {
          figlet.text('  ' + lastMove.move + '  ', { font: 'univers', horizontalLayout: 'full' }, function (err, data) {
              if (err) {
                  console.log('Something went wrong...' + err.message);
                  return;
              }
              console.log(colors.bgGray.brightWhite(data));
          });
          console.log(colors.bgGray.brightWhite('                             B L A C K                             '));
      }
      //tts.say(lastMove.player);
      //Now play it using text to speech library
      if (announceMoveFormat.toLowerCase() == "san") {
  
          tts.say(raplaceKeywords(lastMove.move));
      }
      else {
          tts.say(lastMove.move);
      }
  }
  
  function announceWinner(winner, status, message) {
      if (winner == 'white') {
          figlet.text('  ' + status + '  ', { font: 'univers', horizontalLayout: 'full' }, function (err, data) {
              if (err) {
                  console.log('Something went wrong...' + err.message);
                  return;
              }
              console.log(colors.bgWhite.black(data));
          });
          console.log(colors.bgWhite.black('                             W H I T E    W I N S                         '));
      }
      else if (winner == 'black') {
          figlet.text('  ' + status + '  ', { font: 'univers', horizontalLayout: 'full' }, function (err, data) {
              if (err) {
                  console.log('Something went wrong...' + err.message);
                  return;
              }
              console.log(colors.bgGray.brightWhite(data));
          });
          console.log(colors.bgGray.brightWhite('                             B L A C K    W I N S                         '));
      }
      else {
          figlet.text('  ' + status + '  ', { font: 'univers', horizontalLayout: 'full' }, function (err, data) {
              if (err) {
                  console.log('Something went wrong...' + err.message);
                  return;
              }
              console.log(data);
          });
          console.log('                             * * * * *                             ');
      }
      //Now play message using text to speech library
      tts.say(message);
  }
  
  function announceInvalidMove() {
      if (currentGameColor == 'white') {
          console.log(colors.bgWhite.black(
              figlet.text('  [ X X ]  ', { font: 'univers', horizontalLayout: 'full' }, function (err, text) {
                  if (err) {
                      console.log('Figlet error...' + err);
                      return;
                  }
              })
          ));
          console.log(colors.bgWhite.black('                             W H I T E                             '));
      }
      else {
          console.log(colors.bgGray.brightWhite(
              figlet.text('  [ X X ]  ', { font: 'univers', horizontalLayout: 'full' }, function (err, text) {
                  if (err) {
                      console.log('Figlet error...' + err);
                      return;
                  }
              })
          ));
          console.log(colors.bgGray.brightWhite('                             B L A C K                             '));
      }
      //Now play it using text to speech library
      tts.say('Illegal Move');
  }
  
  function start() {
      console.clear();
      console.log("      ,....,            ".blue.bold + "          ▄████▄   ██░ ██ ▓█████   ██████   ██████     ".red);
      console.log("     ,::::::<           ".blue.bold + "         ▒██▀ ▀█  ▓██░ ██▒▓█   ▀ ▒██    ▒ ▒██    ▒     ".red);
      console.log("    ,::/^\\\"``.        ".blue.bold + "           ▒▓█    ▄ ▒██▀▀██░▒███   ░ ▓██▄   ░ ▓██▄       ".red);
      console.log("   ,::/, `   e`.        ".blue.bold + "         ▒▓▓▄ ▄██▒░▓█ ░██ ▒▓█  ▄   ▒   ██▒  ▒   ██▒    ".red);
      console.log("  ,::; |        '.      ".blue.bold + "         ▒ ▓███▀ ░░▓█▒░██▓░▒████▒▒██████▒▒▒██████▒▒    ".red);
      console.log("  ,::|  \___,-.  c)     ".blue.bold + "          ░ ░▒ ▒  ░ ▒ ░░▒░▒░░ ▒░ ░▒ ▒▓▒ ▒ ░▒ ▒▓▒ ▒ ░    ".red);
      console.log("  ;::|     \\   '-'     ".blue.bold + "          ░  ▒    ▒ ░▒░ ░ ░ ░  ░░ ░▒  ░ ░░ ░▒  ░ ░      ".red);
      console.log("  ;::|      \\          ".blue.bold + "          ░         ░  ░░ ░   ░   ░  ░  ░  ░  ░  ░      ".red);
      console.log("  ;::|   _.=`\\         ".blue.bold + "          ░ ░       ░  ░  ░   ░  ░      ░        ░      ".red);
      console.log("  `;:|.=` _.=`\\        ".blue.bold + "          ░                                             ".red);
      console.log("    '|_.=`   __\\       ".blue.bold + "                                                        ".red);
      console.log("    `\\_..==`` /        ".blue.bold + "         Lichess.org - DGT Electronic Board Connector   ".yellow.bold);
      console.log("     .'.___.-'.         ".blue.bold + "       Developed by Andres Cavallin and Juan Cavallin  ".yellow.bold);
      console.log("    /          \\       ".blue.bold + "                                                        ".red);
      console.log("jgs('--......--')       ".blue.bold + "                                                      ".red);
      console.log("   /'--......--'\\      ".blue.bold + "                                                        ".red);
      console.log("   `\"--......--\"`     ".blue.bold + "                                                        ".red);
  }
  
  /* New Code to Connect to Board */
  
  /**
   * This function will update the currentGameId with a valid active game
   * and then will attach this game to the DGT Board
   * It requires that all maps are up to date: gameInfoMap, gameStateMap, gameConnectionMap and gameChessBoardMap
   */
  async function chooseCurrentGame() {
      //Determine new value for currentGameId. First create an array with only the started games
      //So then there is none or more than one started game
      var playableGames = playableGamesArray();
      //If there is only one started game, then its easy
      if (playableGames.length == 1) {
          currentGameId = playableGames[0].gameId;
          attachCurrentGameIdToDGTBoard(); //Let the board know which color the player is actually playing and setup the position
          console.log('Active game updated. currentGameId: ' + currentGameId);
      }
      else if (playableGames.length == 0) {
          console.log('No started playable games, challenges or games are disconnected. Please start a new game or fix connection.');
          //TODO What happens if the games reconnect and this move is not sent?
      }
      else {
          console.table(playableGames);
          var index = await readLineAsync(`Please enter the index number of the game you want to play with the Board. [0..${playableGames.length - 1}] >\n`);
          if (verbose) console.log(colors.dim.grey(`validateAndSendBoardMove - Game Index: ${index}`));
          if (!isNaN(index) && index.length > 0 && Number(index) >= 0 && Number(index) < playableGames.length) {
              currentGameId = playableGames[Number(index)].gameId;
              //Command looks good for currentGameId 
              attachCurrentGameIdToDGTBoard(); //Let the board know which color the player is actually playing and setup the position
              console.log('Active game updated. currentGameId: ' + currentGameId);
          }
          else {
              console.log('Invalid Index Number. Will not connect to any game at this time.');
              //Retries may be required or not, so retries should be handled on calling function and not here.
          }
      }
  }
  
  
  /**
   * Initialize a ChessBoard when connecting or re-connecting to a game
   * 
   * @param {string} gameId - The gameId of the game to store on the board
   * @param {Object} data - The gameFull event from lichess.org
   */
  function initializeChessBoard(gameId, data) {
      try {
          var chess = new Chess();
          if (data.initialFen != "startpos")
              chess.load(data.initialFen);
          var moves = [];
          var moves = data.state.moves.split(' ');
          for (i = 0; i < moves.length; i++) {
              if (moves[i] != '') {
                  //Make any move that may have been already played on the ChessBoard. Useful when reconnecting
                  chess.move(moves[i], { sloppy: true });
              }
          }
          //Store the ChessBoard on the ChessBoardMap
          gameChessBoardMap.set(gameId, chess);
          if (verbose) console.log(colors.dim.grey(`initializeChessBoard - New Board for gameId: ${gameId}`));
          if (verbose) console.log(colors.dim.grey(chess.ascii()));
      }
      catch (error) {
          console.error(`initializeChessBoard - Error: ${error.message}`.red);
      }
  }
  
  /**
   * Update the ChessBoard for the specified gameId with the new moves on newState since the last stored state
   * 
   * @param {string} gameId - The gameId of the game to store on the board
   * @param {Object} currentState - The state stored on the gameStateMap
   * @param {Object} newState - The new state not yet stored
   */
  function updateChessBoard(gameId, currentState, newState) {
      try {
          var chess = gameChessBoardMap.get(gameId);
          var pendingMoves
          if (!currentState.moves) {
              //No prior moves. Use the new moves
              pendingMoves = newState.moves;
          }
          else {
              //Get all the moves on the newState that are not present on the currentState
              pendingMoves = newState.moves.substring(currentState.moves.length, newState.moves.length);
          }
          var moves = [];
          var moves = pendingMoves.split(' ');
          for (i = 0; i < moves.length; i++) {
              if (moves[i].length > 1) {
                  //Make the new move
                  chess.move(moves[i], { sloppy: true });
              }
          }
          //Store the ChessBoard on the ChessBoardMap
          //gameChessBoardMap.set(gameId, chess);
          if (verbose) console.log(colors.dim.grey(`updateChessBoard - Updated Board for gameId: ${gameId}`));
          if (verbose) console.log(colors.dim.grey(chess.ascii()));
      }
      catch (error) {
          console.error(`updateChessBoard - Error: ${error.message}`.red);
      }
  }
  
  /**
   * Utility function to update which color is being played with the board
   */
  function attachCurrentGameIdToDGTBoard() {
      if (me.id == gameInfoMap.get(currentGameId).white.id)
          currentGameColor = 'white';
      else
          currentGameColor = 'black';
      //Update the board witht the color
      //TODO 
  
      //TODO Send to setUp a reference to gameChessBoardMap and the currentGameId instead of the object since it may change
      //So the values can be asigned inside the setup function.
  
  
      //Setup DGT Board
      dgtBoard.currentGameColor = currentGameColor.substring(0, 1);
      dgtBoard.board_lichess = gameChessBoardMap.get(currentGameId);
      dgtBoard.setUp(gameChessBoardMap.get(currentGameId));
  }
  
  function lichessFormattedMove(boardMove) {
      try {
          if ((dgtBoard.currentGameColor == 'w' && boardMove.color == 'w') ||
              (dgtBoard.currentGameColor == 'b' && boardMove.color == 'b')) {
              return boardMove.from + boardMove.to;
          }
          else {
              //TODO: Veify if opponent's piece is adjusted properly COMPLETED. Verification process is in boardManager
  
              console.error('lichessFormattedMove - Error. Received move is for the wrong color. Expected color is '.red + currentGameColor);
              return '';
          }
      }
      catch (error) {
          console.error(`lichessFormattedMove - Error: ${error.message}`.red);
          return '';
      }
  }
  
  /**
   * Listen to events coming from DGT Board
   * Possible events are connect, disconnect and move
   * 
   * This function is used if the way the boards is sending the events and moves
   * is through event emmiters . Event won't lock the application
   */
  function connectToBoardEvents() {
      dgtBoard.on('move', async (move) => {
          // Outputs : Received validated move
          if (verbose) console.log(colors.dim.grey(`connectToBoardEvents - Received move event from Board: ${JSON.stringify(move)}`));
          await validateAndSendBoardMove(move);
      });
  
      dgtBoard.on('invalidMove', async () => {
          // Outputs : Received an invalid move. Notify user.
          if (verbose) console.log(colors.dim.grey(`connectToBoardEvents - Received invalidMove event from Board`));
          // Notify about invalid move.
          announceInvalidMove();
      });
  
      dgtBoard.on('adjust', async () => {
          // Todo: Send a message to make sure the right adjustment was made
          if (verbose) console.log(colors.dim.grey(`connectToBoardEvents - Received adjust event from Board`));
      })
  
      dgtBoard.on('invalidAdjust', async (move) => {
          // Todo: Send a message showing the wrong adjustment made
          if (verbose) console.log(colors.dim.grey(`connectToBoardEvents - Received invalidAdjust event from Board: ${JSON.stringify(move)}`));
          //Inform user
          tts.say('Incorrect the move was');
          await sleep(1000);
          //Repeat last game state announcement
          var gameState = gameStateMap.get(currentGameId);
          var lastMove = getLastMove(currentGameId);
          announcePlay(lastMove, gameState.wtime, gameState.btime);
      })
  }
  
  /**
   * This function is used if the board implementation uses the approach of
   * waiting for move to be maid on the board.
   * This is similar to awaiting input from a keyboard readling
   * The function is async to prevent locking;
   */
  async function boardInputHandler() {
      var command = '';
      do {
          //Register event for text input from console and wait for input without blocking execution
          var boardMove = await dgtBoard.nextMove(getLastMove(currentGameId));
          if (verbose) console.log(colors.dim.grey(`boardInputHandler - Board move: ${boardMove}`));
          await validateAndSendBoardMove(boardMove);
      } while (true) //TODO Put a proper ending condition like game_over()
  
      if (verbose) console.log(colors.dim.grey('Game Over Exiting...'));
      process.exit(0); //Success
  }
  
  /**
   * This functions hanldes the sending the move to the right lichess game. 
   * If more than one game is being played, it will ask which game to connect to,
   * thus waiting for user input and so, it becomes async
   * 
   * @param {Object} boardMove - The move in chess.js format or string if in lichess format
   */
  async function validateAndSendBoardMove(boardMove) {
      //While there is not and active game, keep trying to find one so the move is not lost
      while (!(gameStateMap.has(currentGameId) && gameConnectionMap.get(currentGameId).connected && gameStateMap.get(currentGameId).status == "started")) {
          //Wait a few seconds to see if the games reconnects or starts and give some space to other code to run
          await sleep(2000);
          //Now attempt to select for which game is this command intented
          await chooseCurrentGame();
      }
      //Now send the move
      command = lichessFormattedMove(boardMove); //The command needs to be verified so it runs after the color is set
      sendMove(currentGameId, command);
  }
  
  function raplaceKeywords(sanMove) {
      var extendedSanMove = sanMove;
      for (let i = 0; i < keywordsBase.length; i++) {
          try {
              extendedSanMove = extendedSanMove.replace(keywordsBase[i], ' ' + keywords[keywordsBase[i]] + ' ');
          } catch (error) {
              console.log(`raplaceKeywords - Error replacing keyword. ${keywordsBase[i]} . ${Error(error).message}`.red);
          }
      }
      return extendedSanMove;
  }
  
  
  /**
   * Show the profile and then
   * Start the Main Loop
   */
  start();
  connectToBoardEvents();  //Connect to events from DGT Board
  getProfile();
  mainLoop();
  keyboardInputHandler(); //This is to allow moves to be entered by keyboard
  //boardInputHandler(); //start monitoring moves one at a time instead of by events
  //dgtBoard.playRandom(); //Emulates a board by playing randomly
  }).call(this,require('_process'),require("buffer").Buffer)
  },{"./bconf":83,"./board-manager":84,"./text-to-speech-browser":131,"_process":32,"axios":85,"axios/lib/adapters/http":86,"buffer":9,"chess.js":113,"colors":118,"figlet":128}],83:[function(require,module,exports){
  /**
   * CONFIGURATION VALUES
   */
  
  const defaultValues =
  {
      "baseURL": "https://lichess.org",
      "personalToken": "",
      "liveChessURL": "ws://localhost:1982/api/v1.0",
      "verbose": true,
      "announceAllMoves": false,
      "announceMoveFormat": "san",
      "splitWords": false,
      "keywords": {
          "K": "King",
          "Q": "Queen",
          "R": "Rook",
          "B": "Bishop",
          "N": "Knight",
          "P": "Pawn",
          "x": "Takes",
          "+": "Check",
          "#": "Checkmate",
          "(=)": "Game ends in draw",
          "O-O-O": "Castles queenside",
          "O-O": "Castles kingside",
          "white": "White",
          "black": "Black",
          "wins by": "wins by",
          "timeout": "timeout",
          "resignation": "resignation",
          "illegal": "illegal",
          "move": "move"
      },
      "keywords_spanish_sample": { "K": "Rey", "Q": "Dama", "R": "Torre", "B": "Alfil", "N": "Caballo", "P": "Peón", "x": "Por", "+": "Jaque", "#": "Jaquemate", "(=)": "Juego termina en Tablas", "O-O": "Enroque corto", "O-O-O": "Enroque largo", "white": "Blancas", "black": "Negras", "wins by": "ganan por", "timeout": "timeout", "resignation": "dimisión", "illegal": "incorrecta", "move": "jugada" }
  }
  
  
  module.exports = {
      /**
       * Utilitary function to retrieve and store values from browser storage and manage default values
       * 
       * @param {string} key The key for the configuration value
       * 
       * @returns {var} The configuration value for the provided key stored on the browser, of the default if not found
       */
      get: (key) => {
          var tempConfigValue
          if (key == "personalToken") {
              tempConfigValue = window.personalToken;
          } else if (key == "baseURL") {
              tempConfigValue = location.origin;
          }
          else if (localStorage.getItem(key) != null) {
              //return stored value
              if (key=='dgt-speech-keywords') {
                  try {
                      tempConfigValue = JSON.parse(localStorage.getItem(key));
                  }
                  catch (error) {
                      tempConfigValue = defaultValues[key];
                  }
              }
              else {
                  tempConfigValue = localStorage.getItem(key);
                  if (tempConfigValue == "true") tempConfigValue = true;
                  if (tempConfigValue == "false") tempConfigValue = false;
              }
          }
          else {
              //return default value
              tempConfigValue = defaultValues[key];
          }
          //console.log(key + ": " + tempConfigValue);
          return tempConfigValue;
      }
  };
  
  
  
  
  
  
  
  },{}],84:[function(require,module,exports){
  (function (process){
  /**
   * REQUIRED MODULES
   */
  const { Chess } = require('chess.js');
  //const board_lichess = new Chess(); //This board contains the position shown on Lichess.org
  const EventEmitter = require('events'); //This is to send events to subscribed applications like move and connect
  const colors = require('colors'); //This is to do colors on the console
  //const WebSocket = require('ws'); //Websockets to communicate with LiveChes from DGT - Commented to support browser
  
  
  /**
   * CONFIGURATION VALUES
   */
  var nconf = require('./bconf');
  /* Removes nconf and replaced by bcfong to support browser
  nconf.argv()
      .env()
      .file({ file: './config.json' });
  nconf.defaults({
      "verbose": false,
      "liveChessURL": "ws://localhost:1982/api/v1.0"
  });
  */
  var verbose = Boolean(nconf.get('dgt-verbose'));; //Verbose on or off
  const liveChessURL = nconf.get('dgt-livechess-url');
  
  
  /**
   * Global Variables for DGT Board Connection (JACM)
   */
  var boards = []; //An array to store all the board recognized by DGT LiveChess 
  //subscription stores the information about the board being connected, most importantly the serialnr
  var subscription = { "id": 2, "call": "subscribe", "param": { "feed": "eboardevent", "id": 1, "param": { "serialnr": "" } } };
  const letterNotation = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']; //Array to easy get the letter for a square
  
  var moveObject;
  var SANMove;
  var localBoard = new Chess()
  var connection; // = new WebSocket(liveChessURL)
  //var hasMadeInvalidAdjustment
  class BoardManager extends EventEmitter {
      constructor() {
          super();
          //Try to connect to LiveChess as soon as the object is created.
          connection
          //Attach to WebSocket events
          this.connectToLiveChess();
          //And keep monitoring connection
          this.connectionMonitorLoop();
      }
  
      currentGameColor = ''; //Public instance field to store the color being played with the board
      board_lichess = new Chess(); //Public reference to the lichess board representation
      currentSerialnr = 0; //Public property to store the current serial number of the DGT Board in case there is more than one
      isLiveChessConnected = false; //Used to track if a board there is a connection to DGT Live Chess
  
      connectToLiveChess() {
          //Open the WebSocket
          connection = new WebSocket(liveChessURL)
  
          //Attach Events
          connection.onopen = () => {
              this.isLiveChessConnected = true;
              if (verbose) console.log(colors.dim.magenta("Websocket onopen: Connection to LiveChess was sucessful"))
              connection.send('{"id":1,"call":"eboards"}');
          }
  
          connection.onerror = error => {
              console.log(colors.dim.red("Websocket ERROR: " + error.message));
          }
  
          connection.onclose = () => {
              console.error(colors.dim.red('Websocket to LiveChess disconnected'));
              //Clear the value of current serial number this serves as a diconnected status
              this.currentSerialnr = 0;
              //Set connection state to false
              this.isLiveChessConnected = false;
          };
  
          connection.onmessage = e => {
              if (verbose) console.log(colors.dim.magenta('Websocket onmessage with data:' + e.data));
              var message = JSON.parse(e.data);
              if (message.response == 'call' && message.id == '1') {
                  //Get the list of availble boards on LiveChess
                  boards = message.param;
                  console.table(boards)
                  console.log(boards[0].serialnr)
                  //TODO we need to be able to handle more than one board
                  //Update the base subscription message with the serial number
                  this.currentSerialnr = boards[0].serialnr;
                  subscription.param.param.serialnr = this.currentSerialnr;
                  if (verbose) console.log(colors.dim.magenta('Websocket onmessage[call]: board serial number updated to: ' + this.currentSerialnr));
                  if (verbose) console.log(colors.dim.magenta('Webscoket - about to send the following message \n' + JSON.stringify(subscription)));
                  connection.send(JSON.stringify(subscription))
                  //Check if the board is properly connected
                  if (boards[0].state != "ACTIVE" && boards[0].state != "INACTIVE") // "NOTRESPONDING" || "DELAYED"
                      console.error(`Board with serial ${this.currentSerialnr} is not properly connected. Please fix`);
                  //Send setup with stating position
                  //const newChess = new Chess()
                  //this.setUp(newChess);
                  if (this.currentGameColor != '') {
                      //There is a game in progress, setup the board as per lichess board
                      if (verbose) console.log(colors.dim.magenta('There is a game in progress, calling setup...'));
                      this.setUp(this.board_lichess);
                  }
              }
              else if (message.response == 'feed' && !(!message.param.san)) {
                  //Received move from board
                  //Adjust local chess.js board with received move
                  if (verbose) console.log(colors.dim.magenta('onmessage - san: ' + message.param.san))
                  SANMove = message.param.san[message.param.san.length - 1]
                  moveObject = localBoard.move(SANMove)
                  //if valid move on local chess.js
                  if (!(!moveObject) && (message.param.san.length > 0)) {
                      //if received move.color == this.currentGameColor
                      if (moveObject.color == this.currentGameColor) {
                          //This is a valid new move send to lichess
                          if (verbose) console.log(colors.dim.green('Valid Move played: ' + SANMove))
                          this.emit('move', moveObject) //moveObject must be in chess.js format
                      }
                      else if (this.board_lichess.history()[this.board_lichess.history().length - 1] == SANMove) {
                          //This is a valid adjustment
                          if (verbose) console.log(colors.dim.green('Valid Adjustment: ' + SANMove))
                          this.emit('adjust') //no moveObject required
                      }
  
                      else {
                          //Invalid Adjustment
                          //Setup DGT board and also initialize localBoard (or undo)
                          localBoard.undo();
                          //TODO Instead of undo we need to bring all the moves from lichess except the last one.
                          //bad adjustment event
                          if (verbose) console.error(colors.red('Invalid Adjustment was made'))
                          this.emit('invalidAdjust', moveObject) //moveObject is optional and must be in chess.js format       
                      }
  
                  }
                  else {
                      //Troubleshooting in case of unknown error
                      if (message.param.san.length == 0) {
                          //This is fine, this was just the setup
                          if (verbose) console.log(colors.green('No real move. This was just the setup.'))
                      }
                      else if (localBoard.history()[localBoard.history().length - 1] == SANMove) {
                          //This is fine, the same last move was received
                          if (verbose) console.log(colors.green('Move received is the same as the last moved played on localboard and DGT Board' + SANMove))
                      }
                      else {
                          //The only way to have an invalid move is that move was legal on LiveChess DGT Board but it was not legal on localchess chess.js board
                          if (verbose) console.error(colors.red('invalidMove - Position Mismatch between DGT Board and internal in memory Board . SAN: ' + SANMove));
                          this.emit('invalidMove');
                          console.log(localBoard.ascii());
                      }
                  }
              }
          }
      }
  
      async setUp(chess) {
          var fen = await chess.fen()
          var setupMessage = {
              "id": 3,
              "call": "call",
              "param": {
                  "id": 1,
                  "method": "setup",
                  "param": {
                      "fen": fen
                  }
              }
          }
          if (verbose) console.log(colors.dim.magenta("setUp -: " + JSON.stringify(setupMessage)))
          if (this.isLiveChessConnected) {
              connection.send(JSON.stringify(setupMessage))
          }
          else {
              console.error(colors.red("WebSocket is not open - cannot send setup command."));
          }
          //Initialize chess.js localboard
          localBoard.load(chess.fen())
      }
  
      async connectionMonitorLoop() {
          //Program ends after 20 re-connection attempts
          for (let attempts = 0; attempts < 20; attempts++) {
              do {
                  //Just sleep five seconds while there is a valid currentSerialnr
                  await this.sleep(5000);
              }
              while (this.currentSerialnr != 0 && this.isLiveChessConnected)
              //currentSerialnr is 0 so still no connection to board. Retry
              if (!this.isLiveChessConnected) {
                  console.warn("No connection to DGT Live Chess. Attempting re-connection. Attempt: " + attempts);
                  this.connectToLiveChess();
              }
              else {
                  //Websocket is fine but still no board detected
                  console.warn("Connection to DGT Live Chess is Fine but no board is detected. Attempting re-connection. Attempt: " + attempts);
                  connection.send('{"id":1,"call":"eboards"}');
              }
          }
          console.error("No connection to DGT Live Chess after maximum number of attempts 20. Exiting application");
          process.exit(0); //Success
      }
  
      async boardMove(start, current) {
          var piece;
          var color;
          var originalSquare = ''
          var moveSquare = '';
          var captures = '';
          var captured = '';
  
          current.move('a6') //TODO Remove this line
          //Get the 2D Array from each chess.js object
          var startBoard = start.board(); var currentBoard = current.board();
  
          //Iterate the 8x8 2D Arrays of startBoard and currentBoard
          for (var i = 0; i < 8; i++) {
              for (var j = 0; j < 8; j++) {
                  //Check if the square on startBoard is different to square on currentBoard
                  if (JSON.stringify(startBoard[i][j]) != JSON.stringify(currentBoard[i][j])) {
                      //If they are different, check if the current board has that square as empty
                      if (JSON.stringify(currentBoard[i][j]) == "null") {
                          //If this square is now empty is because this is the origin (from) square
                          piece = startBoard[i][j].type;
                          color = startBoard[i][j].color;
                          originalSquare = `${letterNotation[j] + (i + 1)}`;
                          if (verbose) console.log(colors.dim.magenta("boardMove - Calcuated origin square: " + originalSquare));
                      }
                      else {
                          //If the square is not empty on the current board is because this is the end (to) square
                          moveSquare = `${letterNotation[j] + (i + 1)}`;
                          if (verbose) console.log(colors.dim.magenta("boardMove - Found end square: " + moveSquare))
                          //end square
  
                          //Check if it was a capture
                          // TODO: doesnt not work for en pasante
                          if (JSON.stringify(startBoard[i][j]) != "null" && startBoard[i][j].color != currentBoard[i][j].color) {
                              captures = 'x';
                              captured = startBoard[i][j].type;
                          }
                      }
                      //Check if we already found the move, then no need to continue iterating
                      if (originalSquare != '' && moveSquare != '') { break; }
                  }
              }
          }
          //Check if both originalSquare (from) and moveSquare (To) were found
          if (originalSquare != '' && moveSquare != '') {
              //Create a move object similar to the chess.js move object but with a move property with the lichess move format
              var verboseMove = { move: `${originalSquare + moveSquare}`, color: color, piece: piece, from: originalSquare, to: moveSquare }
              //Calculate an add the san property
              SANMove = `${piece + captures + moveSquare}`;
              verboseMove.san = SANMove;
              //Add the captured piece if available
              if (captures == 'x') verboseMove.captured = captured;
              if (verbose) console.log(colors.dim.magenta(`boardMove - Lichess: ${originalSquare + moveSquare} SAN: ${piece + captures + moveSquare}`));
              if (verbose) console.log(colors.dim.magenta(JSON.stringify(verboseMove)));
              return verboseMove;
          }
          else {
              //No move was found
              if (verbose) console.log(colors.dim.magenta("boardMove - No move found."));
              return null;
          }
      }
  
      /**
       * Wait some time without blocking other code
       *
       * @param {number} ms - The number of milliseconds to sleep
       */
      sleep(ms = 0) {
          return new Promise(r => setTimeout(r, ms));
      }
  
      async playRandom() {
          var move;
          var moves = [];
          while (!this.board_lichess.game_over()) {
              if (!this.currentGameColor) {
                  //Don't have a color yet, attempt playing as white
                  //First move, go for e4
                  move = { "color": "w", "from": "e2", "to": "e4", "flags": "b", "piece": "p", "san": "e4" };
              }
              else {
                  //Wait for my turn
                  while (this.currentGameColor != this.board_lichess.turn())
                      await this.sleep(1000);
                  //Get the list of possible moves. Use verbose to get the from and to values
                  moves = this.board_lichess.moves({ verbose: true });
                  //Get a random valid move
                  move = moves[Math.floor(Math.random() * moves.length)];
  
              }
              //resolve the export promise
              this.emit('move', move);
              await this.sleep(5000);
          }
          if (verbose) console.log(colors.dim.blue(`playRandom - Game is Over.`));
      }
  
      /**
       * @typedef {Object} move
       * @property {string} color - color of the piece being moved
       * @property {string} from - square where the piece was before the move
       * @property {string} to - sqare where the piece is moved to
       * @property {string} flags - if there is a flag event
       * @property {string} piece - type of piece moved
       * @property {string} san - move in Standard Notation
       * @property {string} captured - key is included when the move is a capture
       * @property {string} promotion - key is included when the move is a promotion
       */
  
      /**
       * 
       * @param {Object} lastMove - The lastMove object as received from lichess
       * @param {string} lastMove.player - The color of the player who played it. Used only for validations
       * @param {string} lastMove.move - The move in lichess format which from concatenated with to
       * @param {string} lastMove.by - The name of the player. This is not used
       * 
       * @returns {move} - The mov in chess.js format
       */
      async nextMove(lastMove) {
          var move;
          var moves = [];
          try {
              if (!(!lastMove) && lastMove.player != 'none') {
                  // lastMove is a valid variable
                  //Sample last move: { player: 'white', move: moves[moves.length - 1], by: gameInfo.white.id }
                  //board_lichess.move(lastMove.move, { sloppy: true })
                  //Get the list of possible moves. Use verbose to get the from and to values
                  moves = this.board_lichess.moves({ verbose: true });
                  //Get a random valid move
                  move = moves[Math.floor(Math.random() * moves.length)];
              }
              else {
                  //First move, go for e4
                  move = { "color": "w", "from": "e2", "to": "e4", "flags": "b", "piece": "p", "san": "e4" };
              }
          }
          catch (error) {
              console.error(`nextMove - Error: ${error.message}`.red);
          }
          await this.sleep(5000);
          //resolve the export promise
          return move;
      }
  
      /**
       * Check if the board thinks its game over.
       */
      game_over() {
          return board_lichess.game_over();
      }
  
  }
  
  module.exports = BoardManager
  
  
  
  
  
  
  
  }).call(this,require('_process'))
  },{"./bconf":83,"_process":32,"chess.js":113,"colors":118,"events":12}],85:[function(require,module,exports){
  module.exports = require('./lib/axios');
  },{"./lib/axios":88}],86:[function(require,module,exports){
  (function (process,Buffer){
  'use strict';
  
  var utils = require('./../utils');
  var settle = require('./../core/settle');
  var buildFullPath = require('../core/buildFullPath');
  var buildURL = require('./../helpers/buildURL');
  var http = require('http');
  var https = require('https');
  var httpFollow = require('follow-redirects').http;
  var httpsFollow = require('follow-redirects').https;
  var url = require('url');
  var zlib = require('zlib');
  var pkg = require('./../../package.json');
  var createError = require('../core/createError');
  var enhanceError = require('../core/enhanceError');
  
  var isHttps = /https:?/;
  
  /*eslint consistent-return:0*/
  module.exports = function httpAdapter(config) {
    return new Promise(function dispatchHttpRequest(resolvePromise, rejectPromise) {
      var resolve = function resolve(value) {
        resolvePromise(value);
      };
      var reject = function reject(value) {
        rejectPromise(value);
      };
      var data = config.data;
      var headers = config.headers;
  
      // Set User-Agent (required by some servers)
      // Only set header if it hasn't been set in config
      // See https://github.com/axios/axios/issues/69
      if (!headers['User-Agent'] && !headers['user-agent']) {
        headers['User-Agent'] = 'axios/' + pkg.version;
      }
  
      if (data && !utils.isStream(data)) {
        if (Buffer.isBuffer(data)) {
          // Nothing to do...
        } else if (utils.isArrayBuffer(data)) {
          data = Buffer.from(new Uint8Array(data));
        } else if (utils.isString(data)) {
          data = Buffer.from(data, 'utf-8');
        } else {
          return reject(createError(
            'Data after transformation must be a string, an ArrayBuffer, a Buffer, or a Stream',
            config
          ));
        }
  
        // Add Content-Length header if data exists
        headers['Content-Length'] = data.length;
      }
  
      // HTTP basic authentication
      var auth = undefined;
      if (config.auth) {
        var username = config.auth.username || '';
        var password = config.auth.password || '';
        auth = username + ':' + password;
      }
  
      // Parse url
      var fullPath = buildFullPath(config.baseURL, config.url);
      var parsed = url.parse(fullPath);
      var protocol = parsed.protocol || 'http:';
  
      if (!auth && parsed.auth) {
        var urlAuth = parsed.auth.split(':');
        var urlUsername = urlAuth[0] || '';
        var urlPassword = urlAuth[1] || '';
        auth = urlUsername + ':' + urlPassword;
      }
  
      if (auth) {
        delete headers.Authorization;
      }
  
      var isHttpsRequest = isHttps.test(protocol);
      var agent = isHttpsRequest ? config.httpsAgent : config.httpAgent;
  
      var options = {
        path: buildURL(parsed.path, config.params, config.paramsSerializer).replace(/^\?/, ''),
        method: config.method.toUpperCase(),
        headers: headers,
        agent: agent,
        agents: { http: config.httpAgent, https: config.httpsAgent },
        auth: auth
      };
  
      if (config.socketPath) {
        options.socketPath = config.socketPath;
      } else {
        options.hostname = parsed.hostname;
        options.port = parsed.port;
      }
  
      var proxy = config.proxy;
      if (!proxy && proxy !== false) {
        var proxyEnv = protocol.slice(0, -1) + '_proxy';
        var proxyUrl = process.env[proxyEnv] || process.env[proxyEnv.toUpperCase()];
        if (proxyUrl) {
          var parsedProxyUrl = url.parse(proxyUrl);
          var noProxyEnv = process.env.no_proxy || process.env.NO_PROXY;
          var shouldProxy = true;
  
          if (noProxyEnv) {
            var noProxy = noProxyEnv.split(',').map(function trim(s) {
              return s.trim();
            });
  
            shouldProxy = !noProxy.some(function proxyMatch(proxyElement) {
              if (!proxyElement) {
                return false;
              }
              if (proxyElement === '*') {
                return true;
              }
              if (proxyElement[0] === '.' &&
                  parsed.hostname.substr(parsed.hostname.length - proxyElement.length) === proxyElement) {
                return true;
              }
  
              return parsed.hostname === proxyElement;
            });
          }
  
  
          if (shouldProxy) {
            proxy = {
              host: parsedProxyUrl.hostname,
              port: parsedProxyUrl.port
            };
  
            if (parsedProxyUrl.auth) {
              var proxyUrlAuth = parsedProxyUrl.auth.split(':');
              proxy.auth = {
                username: proxyUrlAuth[0],
                password: proxyUrlAuth[1]
              };
            }
          }
        }
      }
  
      if (proxy) {
        options.hostname = proxy.host;
        options.host = proxy.host;
        options.headers.host = parsed.hostname + (parsed.port ? ':' + parsed.port : '');
        options.port = proxy.port;
        options.path = protocol + '//' + parsed.hostname + (parsed.port ? ':' + parsed.port : '') + options.path;
  
        // Basic proxy authorization
        if (proxy.auth) {
          var base64 = Buffer.from(proxy.auth.username + ':' + proxy.auth.password, 'utf8').toString('base64');
          options.headers['Proxy-Authorization'] = 'Basic ' + base64;
        }
      }
  
      var transport;
      var isHttpsProxy = isHttpsRequest && (proxy ? isHttps.test(proxy.protocol) : true);
      if (config.transport) {
        transport = config.transport;
      } else if (config.maxRedirects === 0) {
        transport = isHttpsProxy ? https : http;
      } else {
        if (config.maxRedirects) {
          options.maxRedirects = config.maxRedirects;
        }
        transport = isHttpsProxy ? httpsFollow : httpFollow;
      }
  
      if (config.maxContentLength && config.maxContentLength > -1) {
        options.maxBodyLength = config.maxContentLength;
      }
  
      // Create the request
      var req = transport.request(options, function handleResponse(res) {
        if (req.aborted) return;
  
        // uncompress the response body transparently if required
        var stream = res;
        switch (res.headers['content-encoding']) {
        /*eslint default-case:0*/
        case 'gzip':
        case 'compress':
        case 'deflate':
          // add the unzipper to the body stream processing pipeline
          stream = (res.statusCode === 204) ? stream : stream.pipe(zlib.createUnzip());
  
          // remove the content-encoding in order to not confuse downstream operations
          delete res.headers['content-encoding'];
          break;
        }
  
        // return the last request in case of redirects
        var lastRequest = res.req || req;
  
        var response = {
          status: res.statusCode,
          statusText: res.statusMessage,
          headers: res.headers,
          config: config,
          request: lastRequest
        };
  
        if (config.responseType === 'stream') {
          response.data = stream;
          settle(resolve, reject, response);
        } else {
          var responseBuffer = [];
          stream.on('data', function handleStreamData(chunk) {
            responseBuffer.push(chunk);
  
            // make sure the content length is not over the maxContentLength if specified
            if (config.maxContentLength > -1 && Buffer.concat(responseBuffer).length > config.maxContentLength) {
              stream.destroy();
              reject(createError('maxContentLength size of ' + config.maxContentLength + ' exceeded',
                config, null, lastRequest));
            }
          });
  
          stream.on('error', function handleStreamError(err) {
            if (req.aborted) return;
            reject(enhanceError(err, config, null, lastRequest));
          });
  
          stream.on('end', function handleStreamEnd() {
            var responseData = Buffer.concat(responseBuffer);
            if (config.responseType !== 'arraybuffer') {
              responseData = responseData.toString(config.responseEncoding);
            }
  
            response.data = responseData;
            settle(resolve, reject, response);
          });
        }
      });
  
      // Handle errors
      req.on('error', function handleRequestError(err) {
        if (req.aborted) return;
        reject(enhanceError(err, config, null, req));
      });
  
      // Handle request timeout
      if (config.timeout) {
        // Sometime, the response will be very slow, and does not respond, the connect event will be block by event loop system.
        // And timer callback will be fired, and abort() will be invoked before connection, then get "socket hang up" and code ECONNRESET.
        // At this time, if we have a large number of request, nodejs will hang up some socket on background. and the number will up and up.
        // And then these socket which be hang up will devoring CPU little by little.
        // ClientRequest.setTimeout will be fired on the specify milliseconds, and can make sure that abort() will be fired after connect.
        req.setTimeout(config.timeout, function handleRequestTimeout() {
          req.abort();
          reject(createError('timeout of ' + config.timeout + 'ms exceeded', config, 'ECONNABORTED', req));
        });
      }
  
      if (config.cancelToken) {
        // Handle cancellation
        config.cancelToken.promise.then(function onCanceled(cancel) {
          if (req.aborted) return;
  
          req.abort();
          reject(cancel);
        });
      }
  
      // Send the request
      if (utils.isStream(data)) {
        data.on('error', function handleStreamError(err) {
          reject(enhanceError(err, config, null, req));
        }).pipe(req);
      } else {
        req.end(data);
      }
    });
  };
  
  }).call(this,require('_process'),require("buffer").Buffer)
  },{"../core/buildFullPath":94,"../core/createError":95,"../core/enhanceError":97,"./../../package.json":112,"./../core/settle":99,"./../helpers/buildURL":103,"./../utils":111,"_process":32,"buffer":9,"follow-redirects":129,"http":54,"https":13,"url":75,"zlib":8}],87:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  var settle = require('./../core/settle');
  var buildURL = require('./../helpers/buildURL');
  var buildFullPath = require('../core/buildFullPath');
  var parseHeaders = require('./../helpers/parseHeaders');
  var isURLSameOrigin = require('./../helpers/isURLSameOrigin');
  var createError = require('../core/createError');
  
  module.exports = function xhrAdapter(config) {
    return new Promise(function dispatchXhrRequest(resolve, reject) {
      var requestData = config.data;
      var requestHeaders = config.headers;
  
      if (utils.isFormData(requestData)) {
        delete requestHeaders['Content-Type']; // Let the browser set it
      }
  
      var request = new XMLHttpRequest();
  
      // HTTP basic authentication
      if (config.auth) {
        var username = config.auth.username || '';
        var password = config.auth.password || '';
        requestHeaders.Authorization = 'Basic ' + btoa(username + ':' + password);
      }
  
      var fullPath = buildFullPath(config.baseURL, config.url);
      request.open(config.method.toUpperCase(), buildURL(fullPath, config.params, config.paramsSerializer), true);
  
      // Set the request timeout in MS
      request.timeout = config.timeout;
  
      // Listen for ready state
      request.onreadystatechange = function handleLoad() {
        if (!request || request.readyState !== 4) {
          return;
        }
  
        // The request errored out and we didn't get a response, this will be
        // handled by onerror instead
        // With one exception: request that using file: protocol, most browsers
        // will return status as 0 even though it's a successful request
        if (request.status === 0 && !(request.responseURL && request.responseURL.indexOf('file:') === 0)) {
          return;
        }
  
        // Prepare the response
        var responseHeaders = 'getAllResponseHeaders' in request ? parseHeaders(request.getAllResponseHeaders()) : null;
        var responseData = !config.responseType || config.responseType === 'text' ? request.responseText : request.response;
        var response = {
          data: responseData,
          status: request.status,
          statusText: request.statusText,
          headers: responseHeaders,
          config: config,
          request: request
        };
  
        settle(resolve, reject, response);
  
        // Clean up request
        request = null;
      };
  
      // Handle browser request cancellation (as opposed to a manual cancellation)
      request.onabort = function handleAbort() {
        if (!request) {
          return;
        }
  
        reject(createError('Request aborted', config, 'ECONNABORTED', request));
  
        // Clean up request
        request = null;
      };
  
      // Handle low level network errors
      request.onerror = function handleError() {
        // Real errors are hidden from us by the browser
        // onerror should only fire if it's a network error
        reject(createError('Network Error', config, null, request));
  
        // Clean up request
        request = null;
      };
  
      // Handle timeout
      request.ontimeout = function handleTimeout() {
        var timeoutErrorMessage = 'timeout of ' + config.timeout + 'ms exceeded';
        if (config.timeoutErrorMessage) {
          timeoutErrorMessage = config.timeoutErrorMessage;
        }
        reject(createError(timeoutErrorMessage, config, 'ECONNABORTED',
          request));
  
        // Clean up request
        request = null;
      };
  
      // Add xsrf header
      // This is only done if running in a standard browser environment.
      // Specifically not if we're in a web worker, or react-native.
      if (utils.isStandardBrowserEnv()) {
        var cookies = require('./../helpers/cookies');
  
        // Add xsrf header
        var xsrfValue = (config.withCredentials || isURLSameOrigin(fullPath)) && config.xsrfCookieName ?
          cookies.read(config.xsrfCookieName) :
          undefined;
  
        if (xsrfValue) {
          requestHeaders[config.xsrfHeaderName] = xsrfValue;
        }
      }
  
      // Add headers to the request
      if ('setRequestHeader' in request) {
        utils.forEach(requestHeaders, function setRequestHeader(val, key) {
          if (typeof requestData === 'undefined' && key.toLowerCase() === 'content-type') {
            // Remove Content-Type if data is undefined
            delete requestHeaders[key];
          } else {
            // Otherwise add header to the request
            request.setRequestHeader(key, val);
          }
        });
      }
  
      // Add withCredentials to request if needed
      if (!utils.isUndefined(config.withCredentials)) {
        request.withCredentials = !!config.withCredentials;
      }
  
      // Add responseType to request if needed
      if (config.responseType) {
        try {
          request.responseType = config.responseType;
        } catch (e) {
          // Expected DOMException thrown by browsers not compatible XMLHttpRequest Level 2.
          // But, this can be suppressed for 'json' type as it can be parsed by default 'transformResponse' function.
          if (config.responseType !== 'json') {
            throw e;
          }
        }
      }
  
      // Handle progress if needed
      if (typeof config.onDownloadProgress === 'function') {
        request.addEventListener('progress', config.onDownloadProgress);
      }
  
      // Not all browsers support upload events
      if (typeof config.onUploadProgress === 'function' && request.upload) {
        request.upload.addEventListener('progress', config.onUploadProgress);
      }
  
      if (config.cancelToken) {
        // Handle cancellation
        config.cancelToken.promise.then(function onCanceled(cancel) {
          if (!request) {
            return;
          }
  
          request.abort();
          reject(cancel);
          // Clean up request
          request = null;
        });
      }
  
      if (requestData === undefined) {
        requestData = null;
      }
  
      // Send the request
      request.send(requestData);
    });
  };
  
  },{"../core/buildFullPath":94,"../core/createError":95,"./../core/settle":99,"./../helpers/buildURL":103,"./../helpers/cookies":105,"./../helpers/isURLSameOrigin":107,"./../helpers/parseHeaders":109,"./../utils":111}],88:[function(require,module,exports){
  'use strict';
  
  var utils = require('./utils');
  var bind = require('./helpers/bind');
  var Axios = require('./core/Axios');
  var mergeConfig = require('./core/mergeConfig');
  var defaults = require('./defaults');
  
  /**
   * Create an instance of Axios
   *
   * @param {Object} defaultConfig The default config for the instance
   * @return {Axios} A new instance of Axios
   */
  function createInstance(defaultConfig) {
    var context = new Axios(defaultConfig);
    var instance = bind(Axios.prototype.request, context);
  
    // Copy axios.prototype to instance
    utils.extend(instance, Axios.prototype, context);
  
    // Copy context to instance
    utils.extend(instance, context);
  
    return instance;
  }
  
  // Create the default instance to be exported
  var axios = createInstance(defaults);
  
  // Expose Axios class to allow class inheritance
  axios.Axios = Axios;
  
  // Factory for creating new instances
  axios.create = function create(instanceConfig) {
    return createInstance(mergeConfig(axios.defaults, instanceConfig));
  };
  
  // Expose Cancel & CancelToken
  axios.Cancel = require('./cancel/Cancel');
  axios.CancelToken = require('./cancel/CancelToken');
  axios.isCancel = require('./cancel/isCancel');
  
  // Expose all/spread
  axios.all = function all(promises) {
    return Promise.all(promises);
  };
  axios.spread = require('./helpers/spread');
  
  module.exports = axios;
  
  // Allow use of default import syntax in TypeScript
  module.exports.default = axios;
  
  },{"./cancel/Cancel":89,"./cancel/CancelToken":90,"./cancel/isCancel":91,"./core/Axios":92,"./core/mergeConfig":98,"./defaults":101,"./helpers/bind":102,"./helpers/spread":110,"./utils":111}],89:[function(require,module,exports){
  'use strict';
  
  /**
   * A `Cancel` is an object that is thrown when an operation is canceled.
   *
   * @class
   * @param {string=} message The message.
   */
  function Cancel(message) {
    this.message = message;
  }
  
  Cancel.prototype.toString = function toString() {
    return 'Cancel' + (this.message ? ': ' + this.message : '');
  };
  
  Cancel.prototype.__CANCEL__ = true;
  
  module.exports = Cancel;
  
  },{}],90:[function(require,module,exports){
  'use strict';
  
  var Cancel = require('./Cancel');
  
  /**
   * A `CancelToken` is an object that can be used to request cancellation of an operation.
   *
   * @class
   * @param {Function} executor The executor function.
   */
  function CancelToken(executor) {
    if (typeof executor !== 'function') {
      throw new TypeError('executor must be a function.');
    }
  
    var resolvePromise;
    this.promise = new Promise(function promiseExecutor(resolve) {
      resolvePromise = resolve;
    });
  
    var token = this;
    executor(function cancel(message) {
      if (token.reason) {
        // Cancellation has already been requested
        return;
      }
  
      token.reason = new Cancel(message);
      resolvePromise(token.reason);
    });
  }
  
  /**
   * Throws a `Cancel` if cancellation has been requested.
   */
  CancelToken.prototype.throwIfRequested = function throwIfRequested() {
    if (this.reason) {
      throw this.reason;
    }
  };
  
  /**
   * Returns an object that contains a new `CancelToken` and a function that, when called,
   * cancels the `CancelToken`.
   */
  CancelToken.source = function source() {
    var cancel;
    var token = new CancelToken(function executor(c) {
      cancel = c;
    });
    return {
      token: token,
      cancel: cancel
    };
  };
  
  module.exports = CancelToken;
  
  },{"./Cancel":89}],91:[function(require,module,exports){
  'use strict';
  
  module.exports = function isCancel(value) {
    return !!(value && value.__CANCEL__);
  };
  
  },{}],92:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  var buildURL = require('../helpers/buildURL');
  var InterceptorManager = require('./InterceptorManager');
  var dispatchRequest = require('./dispatchRequest');
  var mergeConfig = require('./mergeConfig');
  
  /**
   * Create a new instance of Axios
   *
   * @param {Object} instanceConfig The default config for the instance
   */
  function Axios(instanceConfig) {
    this.defaults = instanceConfig;
    this.interceptors = {
      request: new InterceptorManager(),
      response: new InterceptorManager()
    };
  }
  
  /**
   * Dispatch a request
   *
   * @param {Object} config The config specific for this request (merged with this.defaults)
   */
  Axios.prototype.request = function request(config) {
    /*eslint no-param-reassign:0*/
    // Allow for axios('example/url'[, config]) a la fetch API
    if (typeof config === 'string') {
      config = arguments[1] || {};
      config.url = arguments[0];
    } else {
      config = config || {};
    }
  
    config = mergeConfig(this.defaults, config);
  
    // Set config.method
    if (config.method) {
      config.method = config.method.toLowerCase();
    } else if (this.defaults.method) {
      config.method = this.defaults.method.toLowerCase();
    } else {
      config.method = 'get';
    }
  
    // Hook up interceptors middleware
    var chain = [dispatchRequest, undefined];
    var promise = Promise.resolve(config);
  
    this.interceptors.request.forEach(function unshiftRequestInterceptors(interceptor) {
      chain.unshift(interceptor.fulfilled, interceptor.rejected);
    });
  
    this.interceptors.response.forEach(function pushResponseInterceptors(interceptor) {
      chain.push(interceptor.fulfilled, interceptor.rejected);
    });
  
    while (chain.length) {
      promise = promise.then(chain.shift(), chain.shift());
    }
  
    return promise;
  };
  
  Axios.prototype.getUri = function getUri(config) {
    config = mergeConfig(this.defaults, config);
    return buildURL(config.url, config.params, config.paramsSerializer).replace(/^\?/, '');
  };
  
  // Provide aliases for supported request methods
  utils.forEach(['delete', 'get', 'head', 'options'], function forEachMethodNoData(method) {
    /*eslint func-names:0*/
    Axios.prototype[method] = function(url, config) {
      return this.request(utils.merge(config || {}, {
        method: method,
        url: url
      }));
    };
  });
  
  utils.forEach(['post', 'put', 'patch'], function forEachMethodWithData(method) {
    /*eslint func-names:0*/
    Axios.prototype[method] = function(url, data, config) {
      return this.request(utils.merge(config || {}, {
        method: method,
        url: url,
        data: data
      }));
    };
  });
  
  module.exports = Axios;
  
  },{"../helpers/buildURL":103,"./../utils":111,"./InterceptorManager":93,"./dispatchRequest":96,"./mergeConfig":98}],93:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  
  function InterceptorManager() {
    this.handlers = [];
  }
  
  /**
   * Add a new interceptor to the stack
   *
   * @param {Function} fulfilled The function to handle `then` for a `Promise`
   * @param {Function} rejected The function to handle `reject` for a `Promise`
   *
   * @return {Number} An ID used to remove interceptor later
   */
  InterceptorManager.prototype.use = function use(fulfilled, rejected) {
    this.handlers.push({
      fulfilled: fulfilled,
      rejected: rejected
    });
    return this.handlers.length - 1;
  };
  
  /**
   * Remove an interceptor from the stack
   *
   * @param {Number} id The ID that was returned by `use`
   */
  InterceptorManager.prototype.eject = function eject(id) {
    if (this.handlers[id]) {
      this.handlers[id] = null;
    }
  };
  
  /**
   * Iterate over all the registered interceptors
   *
   * This method is particularly useful for skipping over any
   * interceptors that may have become `null` calling `eject`.
   *
   * @param {Function} fn The function to call for each interceptor
   */
  InterceptorManager.prototype.forEach = function forEach(fn) {
    utils.forEach(this.handlers, function forEachHandler(h) {
      if (h !== null) {
        fn(h);
      }
    });
  };
  
  module.exports = InterceptorManager;
  
  },{"./../utils":111}],94:[function(require,module,exports){
  'use strict';
  
  var isAbsoluteURL = require('../helpers/isAbsoluteURL');
  var combineURLs = require('../helpers/combineURLs');
  
  /**
   * Creates a new URL by combining the baseURL with the requestedURL,
   * only when the requestedURL is not already an absolute URL.
   * If the requestURL is absolute, this function returns the requestedURL untouched.
   *
   * @param {string} baseURL The base URL
   * @param {string} requestedURL Absolute or relative URL to combine
   * @returns {string} The combined full path
   */
  module.exports = function buildFullPath(baseURL, requestedURL) {
    if (baseURL && !isAbsoluteURL(requestedURL)) {
      return combineURLs(baseURL, requestedURL);
    }
    return requestedURL;
  };
  
  },{"../helpers/combineURLs":104,"../helpers/isAbsoluteURL":106}],95:[function(require,module,exports){
  'use strict';
  
  var enhanceError = require('./enhanceError');
  
  /**
   * Create an Error with the specified message, config, error code, request and response.
   *
   * @param {string} message The error message.
   * @param {Object} config The config.
   * @param {string} [code] The error code (for example, 'ECONNABORTED').
   * @param {Object} [request] The request.
   * @param {Object} [response] The response.
   * @returns {Error} The created error.
   */
  module.exports = function createError(message, config, code, request, response) {
    var error = new Error(message);
    return enhanceError(error, config, code, request, response);
  };
  
  },{"./enhanceError":97}],96:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  var transformData = require('./transformData');
  var isCancel = require('../cancel/isCancel');
  var defaults = require('../defaults');
  
  /**
   * Throws a `Cancel` if cancellation has been requested.
   */
  function throwIfCancellationRequested(config) {
    if (config.cancelToken) {
      config.cancelToken.throwIfRequested();
    }
  }
  
  /**
   * Dispatch a request to the server using the configured adapter.
   *
   * @param {object} config The config that is to be used for the request
   * @returns {Promise} The Promise to be fulfilled
   */
  module.exports = function dispatchRequest(config) {
    throwIfCancellationRequested(config);
  
    // Ensure headers exist
    config.headers = config.headers || {};
  
    // Transform request data
    config.data = transformData(
      config.data,
      config.headers,
      config.transformRequest
    );
  
    // Flatten headers
    config.headers = utils.merge(
      config.headers.common || {},
      config.headers[config.method] || {},
      config.headers
    );
  
    utils.forEach(
      ['delete', 'get', 'head', 'post', 'put', 'patch', 'common'],
      function cleanHeaderConfig(method) {
        delete config.headers[method];
      }
    );
  
    var adapter = config.adapter || defaults.adapter;
  
    return adapter(config).then(function onAdapterResolution(response) {
      throwIfCancellationRequested(config);
  
      // Transform response data
      response.data = transformData(
        response.data,
        response.headers,
        config.transformResponse
      );
  
      return response;
    }, function onAdapterRejection(reason) {
      if (!isCancel(reason)) {
        throwIfCancellationRequested(config);
  
        // Transform response data
        if (reason && reason.response) {
          reason.response.data = transformData(
            reason.response.data,
            reason.response.headers,
            config.transformResponse
          );
        }
      }
  
      return Promise.reject(reason);
    });
  };
  
  },{"../cancel/isCancel":91,"../defaults":101,"./../utils":111,"./transformData":100}],97:[function(require,module,exports){
  'use strict';
  
  /**
   * Update an Error with the specified config, error code, and response.
   *
   * @param {Error} error The error to update.
   * @param {Object} config The config.
   * @param {string} [code] The error code (for example, 'ECONNABORTED').
   * @param {Object} [request] The request.
   * @param {Object} [response] The response.
   * @returns {Error} The error.
   */
  module.exports = function enhanceError(error, config, code, request, response) {
    error.config = config;
    if (code) {
      error.code = code;
    }
  
    error.request = request;
    error.response = response;
    error.isAxiosError = true;
  
    error.toJSON = function() {
      return {
        // Standard
        message: this.message,
        name: this.name,
        // Microsoft
        description: this.description,
        number: this.number,
        // Mozilla
        fileName: this.fileName,
        lineNumber: this.lineNumber,
        columnNumber: this.columnNumber,
        stack: this.stack,
        // Axios
        config: this.config,
        code: this.code
      };
    };
    return error;
  };
  
  },{}],98:[function(require,module,exports){
  'use strict';
  
  var utils = require('../utils');
  
  /**
   * Config-specific merge-function which creates a new config-object
   * by merging two configuration objects together.
   *
   * @param {Object} config1
   * @param {Object} config2
   * @returns {Object} New object resulting from merging config2 to config1
   */
  module.exports = function mergeConfig(config1, config2) {
    // eslint-disable-next-line no-param-reassign
    config2 = config2 || {};
    var config = {};
  
    var valueFromConfig2Keys = ['url', 'method', 'params', 'data'];
    var mergeDeepPropertiesKeys = ['headers', 'auth', 'proxy'];
    var defaultToConfig2Keys = [
      'baseURL', 'url', 'transformRequest', 'transformResponse', 'paramsSerializer',
      'timeout', 'withCredentials', 'adapter', 'responseType', 'xsrfCookieName',
      'xsrfHeaderName', 'onUploadProgress', 'onDownloadProgress',
      'maxContentLength', 'validateStatus', 'maxRedirects', 'httpAgent',
      'httpsAgent', 'cancelToken', 'socketPath'
    ];
  
    utils.forEach(valueFromConfig2Keys, function valueFromConfig2(prop) {
      if (typeof config2[prop] !== 'undefined') {
        config[prop] = config2[prop];
      }
    });
  
    utils.forEach(mergeDeepPropertiesKeys, function mergeDeepProperties(prop) {
      if (utils.isObject(config2[prop])) {
        config[prop] = utils.deepMerge(config1[prop], config2[prop]);
      } else if (typeof config2[prop] !== 'undefined') {
        config[prop] = config2[prop];
      } else if (utils.isObject(config1[prop])) {
        config[prop] = utils.deepMerge(config1[prop]);
      } else if (typeof config1[prop] !== 'undefined') {
        config[prop] = config1[prop];
      }
    });
  
    utils.forEach(defaultToConfig2Keys, function defaultToConfig2(prop) {
      if (typeof config2[prop] !== 'undefined') {
        config[prop] = config2[prop];
      } else if (typeof config1[prop] !== 'undefined') {
        config[prop] = config1[prop];
      }
    });
  
    var axiosKeys = valueFromConfig2Keys
      .concat(mergeDeepPropertiesKeys)
      .concat(defaultToConfig2Keys);
  
    var otherKeys = Object
      .keys(config2)
      .filter(function filterAxiosKeys(key) {
        return axiosKeys.indexOf(key) === -1;
      });
  
    utils.forEach(otherKeys, function otherKeysDefaultToConfig2(prop) {
      if (typeof config2[prop] !== 'undefined') {
        config[prop] = config2[prop];
      } else if (typeof config1[prop] !== 'undefined') {
        config[prop] = config1[prop];
      }
    });
  
    return config;
  };
  
  },{"../utils":111}],99:[function(require,module,exports){
  'use strict';
  
  var createError = require('./createError');
  
  /**
   * Resolve or reject a Promise based on response status.
   *
   * @param {Function} resolve A function that resolves the promise.
   * @param {Function} reject A function that rejects the promise.
   * @param {object} response The response.
   */
  module.exports = function settle(resolve, reject, response) {
    var validateStatus = response.config.validateStatus;
    if (!validateStatus || validateStatus(response.status)) {
      resolve(response);
    } else {
      reject(createError(
        'Request failed with status code ' + response.status,
        response.config,
        null,
        response.request,
        response
      ));
    }
  };
  
  },{"./createError":95}],100:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  
  /**
   * Transform the data for a request or a response
   *
   * @param {Object|String} data The data to be transformed
   * @param {Array} headers The headers for the request or response
   * @param {Array|Function} fns A single function or Array of functions
   * @returns {*} The resulting transformed data
   */
  module.exports = function transformData(data, headers, fns) {
    /*eslint no-param-reassign:0*/
    utils.forEach(fns, function transform(fn) {
      data = fn(data, headers);
    });
  
    return data;
  };
  
  },{"./../utils":111}],101:[function(require,module,exports){
  (function (process){
  'use strict';
  
  var utils = require('./utils');
  var normalizeHeaderName = require('./helpers/normalizeHeaderName');
  
  var DEFAULT_CONTENT_TYPE = {
    'Content-Type': 'application/x-www-form-urlencoded'
  };
  
  function setContentTypeIfUnset(headers, value) {
    if (!utils.isUndefined(headers) && utils.isUndefined(headers['Content-Type'])) {
      headers['Content-Type'] = value;
    }
  }
  
  function getDefaultAdapter() {
    var adapter;
    if (typeof XMLHttpRequest !== 'undefined') {
      // For browsers use XHR adapter
      adapter = require('./adapters/xhr');
    } else if (typeof process !== 'undefined' && Object.prototype.toString.call(process) === '[object process]') {
      // For node use HTTP adapter
      adapter = require('./adapters/http');
    }
    return adapter;
  }
  
  var defaults = {
    adapter: getDefaultAdapter(),
  
    transformRequest: [function transformRequest(data, headers) {
      normalizeHeaderName(headers, 'Accept');
      normalizeHeaderName(headers, 'Content-Type');
      if (utils.isFormData(data) ||
        utils.isArrayBuffer(data) ||
        utils.isBuffer(data) ||
        utils.isStream(data) ||
        utils.isFile(data) ||
        utils.isBlob(data)
      ) {
        return data;
      }
      if (utils.isArrayBufferView(data)) {
        return data.buffer;
      }
      if (utils.isURLSearchParams(data)) {
        setContentTypeIfUnset(headers, 'application/x-www-form-urlencoded;charset=utf-8');
        return data.toString();
      }
      if (utils.isObject(data)) {
        setContentTypeIfUnset(headers, 'application/json;charset=utf-8');
        return JSON.stringify(data);
      }
      return data;
    }],
  
    transformResponse: [function transformResponse(data) {
      /*eslint no-param-reassign:0*/
      if (typeof data === 'string') {
        try {
          data = JSON.parse(data);
        } catch (e) { /* Ignore */ }
      }
      return data;
    }],
  
    /**
     * A timeout in milliseconds to abort a request. If set to 0 (default) a
     * timeout is not created.
     */
    timeout: 0,
  
    xsrfCookieName: 'XSRF-TOKEN',
    xsrfHeaderName: 'X-XSRF-TOKEN',
  
    maxContentLength: -1,
  
    validateStatus: function validateStatus(status) {
      return status >= 200 && status < 300;
    }
  };
  
  defaults.headers = {
    common: {
      'Accept': 'application/json, text/plain, */*'
    }
  };
  
  utils.forEach(['delete', 'get', 'head'], function forEachMethodNoData(method) {
    defaults.headers[method] = {};
  });
  
  utils.forEach(['post', 'put', 'patch'], function forEachMethodWithData(method) {
    defaults.headers[method] = utils.merge(DEFAULT_CONTENT_TYPE);
  });
  
  module.exports = defaults;
  
  }).call(this,require('_process'))
  },{"./adapters/http":87,"./adapters/xhr":87,"./helpers/normalizeHeaderName":108,"./utils":111,"_process":32}],102:[function(require,module,exports){
  'use strict';
  
  module.exports = function bind(fn, thisArg) {
    return function wrap() {
      var args = new Array(arguments.length);
      for (var i = 0; i < args.length; i++) {
        args[i] = arguments[i];
      }
      return fn.apply(thisArg, args);
    };
  };
  
  },{}],103:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  
  function encode(val) {
    return encodeURIComponent(val).
      replace(/%40/gi, '@').
      replace(/%3A/gi, ':').
      replace(/%24/g, '$').
      replace(/%2C/gi, ',').
      replace(/%20/g, '+').
      replace(/%5B/gi, '[').
      replace(/%5D/gi, ']');
  }
  
  /**
   * Build a URL by appending params to the end
   *
   * @param {string} url The base of the url (e.g., http://www.google.com)
   * @param {object} [params] The params to be appended
   * @returns {string} The formatted url
   */
  module.exports = function buildURL(url, params, paramsSerializer) {
    /*eslint no-param-reassign:0*/
    if (!params) {
      return url;
    }
  
    var serializedParams;
    if (paramsSerializer) {
      serializedParams = paramsSerializer(params);
    } else if (utils.isURLSearchParams(params)) {
      serializedParams = params.toString();
    } else {
      var parts = [];
  
      utils.forEach(params, function serialize(val, key) {
        if (val === null || typeof val === 'undefined') {
          return;
        }
  
        if (utils.isArray(val)) {
          key = key + '[]';
        } else {
          val = [val];
        }
  
        utils.forEach(val, function parseValue(v) {
          if (utils.isDate(v)) {
            v = v.toISOString();
          } else if (utils.isObject(v)) {
            v = JSON.stringify(v);
          }
          parts.push(encode(key) + '=' + encode(v));
        });
      });
  
      serializedParams = parts.join('&');
    }
  
    if (serializedParams) {
      var hashmarkIndex = url.indexOf('#');
      if (hashmarkIndex !== -1) {
        url = url.slice(0, hashmarkIndex);
      }
  
      url += (url.indexOf('?') === -1 ? '?' : '&') + serializedParams;
    }
  
    return url;
  };
  
  },{"./../utils":111}],104:[function(require,module,exports){
  'use strict';
  
  /**
   * Creates a new URL by combining the specified URLs
   *
   * @param {string} baseURL The base URL
   * @param {string} relativeURL The relative URL
   * @returns {string} The combined URL
   */
  module.exports = function combineURLs(baseURL, relativeURL) {
    return relativeURL
      ? baseURL.replace(/\/+$/, '') + '/' + relativeURL.replace(/^\/+/, '')
      : baseURL;
  };
  
  },{}],105:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  
  module.exports = (
    utils.isStandardBrowserEnv() ?
  
    // Standard browser envs support document.cookie
      (function standardBrowserEnv() {
        return {
          write: function write(name, value, expires, path, domain, secure) {
            var cookie = [];
            cookie.push(name + '=' + encodeURIComponent(value));
  
            if (utils.isNumber(expires)) {
              cookie.push('expires=' + new Date(expires).toGMTString());
            }
  
            if (utils.isString(path)) {
              cookie.push('path=' + path);
            }
  
            if (utils.isString(domain)) {
              cookie.push('domain=' + domain);
            }
  
            if (secure === true) {
              cookie.push('secure');
            }
  
            document.cookie = cookie.join('; ');
          },
  
          read: function read(name) {
            var match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
            return (match ? decodeURIComponent(match[3]) : null);
          },
  
          remove: function remove(name) {
            this.write(name, '', Date.now() - 86400000);
          }
        };
      })() :
  
    // Non standard browser env (web workers, react-native) lack needed support.
      (function nonStandardBrowserEnv() {
        return {
          write: function write() {},
          read: function read() { return null; },
          remove: function remove() {}
        };
      })()
  );
  
  },{"./../utils":111}],106:[function(require,module,exports){
  'use strict';
  
  /**
   * Determines whether the specified URL is absolute
   *
   * @param {string} url The URL to test
   * @returns {boolean} True if the specified URL is absolute, otherwise false
   */
  module.exports = function isAbsoluteURL(url) {
    // A URL is considered absolute if it begins with "<scheme>://" or "//" (protocol-relative URL).
    // RFC 3986 defines scheme name as a sequence of characters beginning with a letter and followed
    // by any combination of letters, digits, plus, period, or hyphen.
    return /^([a-z][a-z\d\+\-\.]*:)?\/\//i.test(url);
  };
  
  },{}],107:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  
  module.exports = (
    utils.isStandardBrowserEnv() ?
  
    // Standard browser envs have full support of the APIs needed to test
    // whether the request URL is of the same origin as current location.
      (function standardBrowserEnv() {
        var msie = /(msie|trident)/i.test(navigator.userAgent);
        var urlParsingNode = document.createElement('a');
        var originURL;
  
        /**
      * Parse a URL to discover it's components
      *
      * @param {String} url The URL to be parsed
      * @returns {Object}
      */
        function resolveURL(url) {
          var href = url;
  
          if (msie) {
          // IE needs attribute set twice to normalize properties
            urlParsingNode.setAttribute('href', href);
            href = urlParsingNode.href;
          }
  
          urlParsingNode.setAttribute('href', href);
  
          // urlParsingNode provides the UrlUtils interface - http://url.spec.whatwg.org/#urlutils
          return {
            href: urlParsingNode.href,
            protocol: urlParsingNode.protocol ? urlParsingNode.protocol.replace(/:$/, '') : '',
            host: urlParsingNode.host,
            search: urlParsingNode.search ? urlParsingNode.search.replace(/^\?/, '') : '',
            hash: urlParsingNode.hash ? urlParsingNode.hash.replace(/^#/, '') : '',
            hostname: urlParsingNode.hostname,
            port: urlParsingNode.port,
            pathname: (urlParsingNode.pathname.charAt(0) === '/') ?
              urlParsingNode.pathname :
              '/' + urlParsingNode.pathname
          };
        }
  
        originURL = resolveURL(window.location.href);
  
        /**
      * Determine if a URL shares the same origin as the current location
      *
      * @param {String} requestURL The URL to test
      * @returns {boolean} True if URL shares the same origin, otherwise false
      */
        return function isURLSameOrigin(requestURL) {
          var parsed = (utils.isString(requestURL)) ? resolveURL(requestURL) : requestURL;
          return (parsed.protocol === originURL.protocol &&
              parsed.host === originURL.host);
        };
      })() :
  
    // Non standard browser envs (web workers, react-native) lack needed support.
      (function nonStandardBrowserEnv() {
        return function isURLSameOrigin() {
          return true;
        };
      })()
  );
  
  },{"./../utils":111}],108:[function(require,module,exports){
  'use strict';
  
  var utils = require('../utils');
  
  module.exports = function normalizeHeaderName(headers, normalizedName) {
    utils.forEach(headers, function processHeader(value, name) {
      if (name !== normalizedName && name.toUpperCase() === normalizedName.toUpperCase()) {
        headers[normalizedName] = value;
        delete headers[name];
      }
    });
  };
  
  },{"../utils":111}],109:[function(require,module,exports){
  'use strict';
  
  var utils = require('./../utils');
  
  // Headers whose duplicates are ignored by node
  // c.f. https://nodejs.org/api/http.html#http_message_headers
  var ignoreDuplicateOf = [
    'age', 'authorization', 'content-length', 'content-type', 'etag',
    'expires', 'from', 'host', 'if-modified-since', 'if-unmodified-since',
    'last-modified', 'location', 'max-forwards', 'proxy-authorization',
    'referer', 'retry-after', 'user-agent'
  ];
  
  /**
   * Parse headers into an object
   *
   * ```
   * Date: Wed, 27 Aug 2014 08:58:49 GMT
   * Content-Type: application/json
   * Connection: keep-alive
   * Transfer-Encoding: chunked
   * ```
   *
   * @param {String} headers Headers needing to be parsed
   * @returns {Object} Headers parsed into an object
   */
  module.exports = function parseHeaders(headers) {
    var parsed = {};
    var key;
    var val;
    var i;
  
    if (!headers) { return parsed; }
  
    utils.forEach(headers.split('\n'), function parser(line) {
      i = line.indexOf(':');
      key = utils.trim(line.substr(0, i)).toLowerCase();
      val = utils.trim(line.substr(i + 1));
  
      if (key) {
        if (parsed[key] && ignoreDuplicateOf.indexOf(key) >= 0) {
          return;
        }
        if (key === 'set-cookie') {
          parsed[key] = (parsed[key] ? parsed[key] : []).concat([val]);
        } else {
          parsed[key] = parsed[key] ? parsed[key] + ', ' + val : val;
        }
      }
    });
  
    return parsed;
  };
  
  },{"./../utils":111}],110:[function(require,module,exports){
  'use strict';
  
  /**
   * Syntactic sugar for invoking a function and expanding an array for arguments.
   *
   * Common use case would be to use `Function.prototype.apply`.
   *
   *  ```js
   *  function f(x, y, z) {}
   *  var args = [1, 2, 3];
   *  f.apply(null, args);
   *  ```
   *
   * With `spread` this example can be re-written.
   *
   *  ```js
   *  spread(function(x, y, z) {})([1, 2, 3]);
   *  ```
   *
   * @param {Function} callback
   * @returns {Function}
   */
  module.exports = function spread(callback) {
    return function wrap(arr) {
      return callback.apply(null, arr);
    };
  };
  
  },{}],111:[function(require,module,exports){
  'use strict';
  
  var bind = require('./helpers/bind');
  
  /*global toString:true*/
  
  // utils is a library of generic helper functions non-specific to axios
  
  var toString = Object.prototype.toString;
  
  /**
   * Determine if a value is an Array
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is an Array, otherwise false
   */
  function isArray(val) {
    return toString.call(val) === '[object Array]';
  }
  
  /**
   * Determine if a value is undefined
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if the value is undefined, otherwise false
   */
  function isUndefined(val) {
    return typeof val === 'undefined';
  }
  
  /**
   * Determine if a value is a Buffer
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a Buffer, otherwise false
   */
  function isBuffer(val) {
    return val !== null && !isUndefined(val) && val.constructor !== null && !isUndefined(val.constructor)
      && typeof val.constructor.isBuffer === 'function' && val.constructor.isBuffer(val);
  }
  
  /**
   * Determine if a value is an ArrayBuffer
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is an ArrayBuffer, otherwise false
   */
  function isArrayBuffer(val) {
    return toString.call(val) === '[object ArrayBuffer]';
  }
  
  /**
   * Determine if a value is a FormData
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is an FormData, otherwise false
   */
  function isFormData(val) {
    return (typeof FormData !== 'undefined') && (val instanceof FormData);
  }
  
  /**
   * Determine if a value is a view on an ArrayBuffer
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a view on an ArrayBuffer, otherwise false
   */
  function isArrayBufferView(val) {
    var result;
    if ((typeof ArrayBuffer !== 'undefined') && (ArrayBuffer.isView)) {
      result = ArrayBuffer.isView(val);
    } else {
      result = (val) && (val.buffer) && (val.buffer instanceof ArrayBuffer);
    }
    return result;
  }
  
  /**
   * Determine if a value is a String
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a String, otherwise false
   */
  function isString(val) {
    return typeof val === 'string';
  }
  
  /**
   * Determine if a value is a Number
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a Number, otherwise false
   */
  function isNumber(val) {
    return typeof val === 'number';
  }
  
  /**
   * Determine if a value is an Object
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is an Object, otherwise false
   */
  function isObject(val) {
    return val !== null && typeof val === 'object';
  }
  
  /**
   * Determine if a value is a Date
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a Date, otherwise false
   */
  function isDate(val) {
    return toString.call(val) === '[object Date]';
  }
  
  /**
   * Determine if a value is a File
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a File, otherwise false
   */
  function isFile(val) {
    return toString.call(val) === '[object File]';
  }
  
  /**
   * Determine if a value is a Blob
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a Blob, otherwise false
   */
  function isBlob(val) {
    return toString.call(val) === '[object Blob]';
  }
  
  /**
   * Determine if a value is a Function
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a Function, otherwise false
   */
  function isFunction(val) {
    return toString.call(val) === '[object Function]';
  }
  
  /**
   * Determine if a value is a Stream
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a Stream, otherwise false
   */
  function isStream(val) {
    return isObject(val) && isFunction(val.pipe);
  }
  
  /**
   * Determine if a value is a URLSearchParams object
   *
   * @param {Object} val The value to test
   * @returns {boolean} True if value is a URLSearchParams object, otherwise false
   */
  function isURLSearchParams(val) {
    return typeof URLSearchParams !== 'undefined' && val instanceof URLSearchParams;
  }
  
  /**
   * Trim excess whitespace off the beginning and end of a string
   *
   * @param {String} str The String to trim
   * @returns {String} The String freed of excess whitespace
   */
  function trim(str) {
    return str.replace(/^\s*/, '').replace(/\s*$/, '');
  }
  
  /**
   * Determine if we're running in a standard browser environment
   *
   * This allows axios to run in a web worker, and react-native.
   * Both environments support XMLHttpRequest, but not fully standard globals.
   *
   * web workers:
   *  typeof window -> undefined
   *  typeof document -> undefined
   *
   * react-native:
   *  navigator.product -> 'ReactNative'
   * nativescript
   *  navigator.product -> 'NativeScript' or 'NS'
   */
  function isStandardBrowserEnv() {
    if (typeof navigator !== 'undefined' && (navigator.product === 'ReactNative' ||
                                             navigator.product === 'NativeScript' ||
                                             navigator.product === 'NS')) {
      return false;
    }
    return (
      typeof window !== 'undefined' &&
      typeof document !== 'undefined'
    );
  }
  
  /**
   * Iterate over an Array or an Object invoking a function for each item.
   *
   * If `obj` is an Array callback will be called passing
   * the value, index, and complete array for each item.
   *
   * If 'obj' is an Object callback will be called passing
   * the value, key, and complete object for each property.
   *
   * @param {Object|Array} obj The object to iterate
   * @param {Function} fn The callback to invoke for each item
   */
  function forEach(obj, fn) {
    // Don't bother if no value provided
    if (obj === null || typeof obj === 'undefined') {
      return;
    }
  
    // Force an array if not already something iterable
    if (typeof obj !== 'object') {
      /*eslint no-param-reassign:0*/
      obj = [obj];
    }
  
    if (isArray(obj)) {
      // Iterate over array values
      for (var i = 0, l = obj.length; i < l; i++) {
        fn.call(null, obj[i], i, obj);
      }
    } else {
      // Iterate over object keys
      for (var key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          fn.call(null, obj[key], key, obj);
        }
      }
    }
  }
  
  /**
   * Accepts varargs expecting each argument to be an object, then
   * immutably merges the properties of each object and returns result.
   *
   * When multiple objects contain the same key the later object in
   * the arguments list will take precedence.
   *
   * Example:
   *
   * ```js
   * var result = merge({foo: 123}, {foo: 456});
   * console.log(result.foo); // outputs 456
   * ```
   *
   * @param {Object} obj1 Object to merge
   * @returns {Object} Result of all merge properties
   */
  function merge(/* obj1, obj2, obj3, ... */) {
    var result = {};
    function assignValue(val, key) {
      if (typeof result[key] === 'object' && typeof val === 'object') {
        result[key] = merge(result[key], val);
      } else {
        result[key] = val;
      }
    }
  
    for (var i = 0, l = arguments.length; i < l; i++) {
      forEach(arguments[i], assignValue);
    }
    return result;
  }
  
  /**
   * Function equal to merge with the difference being that no reference
   * to original objects is kept.
   *
   * @see merge
   * @param {Object} obj1 Object to merge
   * @returns {Object} Result of all merge properties
   */
  function deepMerge(/* obj1, obj2, obj3, ... */) {
    var result = {};
    function assignValue(val, key) {
      if (typeof result[key] === 'object' && typeof val === 'object') {
        result[key] = deepMerge(result[key], val);
      } else if (typeof val === 'object') {
        result[key] = deepMerge({}, val);
      } else {
        result[key] = val;
      }
    }
  
    for (var i = 0, l = arguments.length; i < l; i++) {
      forEach(arguments[i], assignValue);
    }
    return result;
  }
  
  /**
   * Extends object a by mutably adding to it the properties of object b.
   *
   * @param {Object} a The object to be extended
   * @param {Object} b The object to copy properties from
   * @param {Object} thisArg The object to bind function to
   * @return {Object} The resulting value of object a
   */
  function extend(a, b, thisArg) {
    forEach(b, function assignValue(val, key) {
      if (thisArg && typeof val === 'function') {
        a[key] = bind(val, thisArg);
      } else {
        a[key] = val;
      }
    });
    return a;
  }
  
  module.exports = {
    isArray: isArray,
    isArrayBuffer: isArrayBuffer,
    isBuffer: isBuffer,
    isFormData: isFormData,
    isArrayBufferView: isArrayBufferView,
    isString: isString,
    isNumber: isNumber,
    isObject: isObject,
    isUndefined: isUndefined,
    isDate: isDate,
    isFile: isFile,
    isBlob: isBlob,
    isFunction: isFunction,
    isStream: isStream,
    isURLSearchParams: isURLSearchParams,
    isStandardBrowserEnv: isStandardBrowserEnv,
    forEach: forEach,
    merge: merge,
    deepMerge: deepMerge,
    extend: extend,
    trim: trim
  };
  
  },{"./helpers/bind":102}],112:[function(require,module,exports){
  module.exports={
    "_args": [
      [
        "axios@0.19.2",
        "C:\\Users\\andre\\Documents\\github\\lichess-dgt-boards-browser"
      ]
    ],
    "_from": "axios@0.19.2",
    "_id": "axios@0.19.2",
    "_inBundle": false,
    "_integrity": "sha512-fjgm5MvRHLhx+osE2xoekY70AhARk3a6hkN+3Io1jc00jtquGvxYlKlsFUhmUET0V5te6CcZI7lcv2Ym61mjHA==",
    "_location": "/axios",
    "_phantomChildren": {},
    "_requested": {
      "type": "version",
      "registry": true,
      "raw": "axios@0.19.2",
      "name": "axios",
      "escapedName": "axios",
      "rawSpec": "0.19.2",
      "saveSpec": null,
      "fetchSpec": "0.19.2"
    },
    "_requiredBy": [
      "/"
    ],
    "_resolved": "https://registry.npmjs.org/axios/-/axios-0.19.2.tgz",
    "_spec": "0.19.2",
    "_where": "C:\\Users\\andre\\Documents\\github\\lichess-dgt-boards-browser",
    "author": {
      "name": "Matt Zabriskie"
    },
    "browser": {
      "./lib/adapters/http.js": "./lib/adapters/xhr.js"
    },
    "bugs": {
      "url": "https://github.com/axios/axios/issues"
    },
    "bundlesize": [
      {
        "path": "./dist/axios.min.js",
        "threshold": "5kB"
      }
    ],
    "dependencies": {
      "follow-redirects": "1.5.10"
    },
    "description": "Promise based HTTP client for the browser and node.js",
    "devDependencies": {
      "bundlesize": "^0.17.0",
      "coveralls": "^3.0.0",
      "es6-promise": "^4.2.4",
      "grunt": "^1.0.2",
      "grunt-banner": "^0.6.0",
      "grunt-cli": "^1.2.0",
      "grunt-contrib-clean": "^1.1.0",
      "grunt-contrib-watch": "^1.0.0",
      "grunt-eslint": "^20.1.0",
      "grunt-karma": "^2.0.0",
      "grunt-mocha-test": "^0.13.3",
      "grunt-ts": "^6.0.0-beta.19",
      "grunt-webpack": "^1.0.18",
      "istanbul-instrumenter-loader": "^1.0.0",
      "jasmine-core": "^2.4.1",
      "karma": "^1.3.0",
      "karma-chrome-launcher": "^2.2.0",
      "karma-coverage": "^1.1.1",
      "karma-firefox-launcher": "^1.1.0",
      "karma-jasmine": "^1.1.1",
      "karma-jasmine-ajax": "^0.1.13",
      "karma-opera-launcher": "^1.0.0",
      "karma-safari-launcher": "^1.0.0",
      "karma-sauce-launcher": "^1.2.0",
      "karma-sinon": "^1.0.5",
      "karma-sourcemap-loader": "^0.3.7",
      "karma-webpack": "^1.7.0",
      "load-grunt-tasks": "^3.5.2",
      "minimist": "^1.2.0",
      "mocha": "^5.2.0",
      "sinon": "^4.5.0",
      "typescript": "^2.8.1",
      "url-search-params": "^0.10.0",
      "webpack": "^1.13.1",
      "webpack-dev-server": "^1.14.1"
    },
    "homepage": "https://github.com/axios/axios",
    "keywords": [
      "xhr",
      "http",
      "ajax",
      "promise",
      "node"
    ],
    "license": "MIT",
    "main": "index.js",
    "name": "axios",
    "repository": {
      "type": "git",
      "url": "git+https://github.com/axios/axios.git"
    },
    "scripts": {
      "build": "NODE_ENV=production grunt build",
      "coveralls": "cat coverage/lcov.info | ./node_modules/coveralls/bin/coveralls.js",
      "examples": "node ./examples/server.js",
      "fix": "eslint --fix lib/**/*.js",
      "postversion": "git push && git push --tags",
      "preversion": "npm test",
      "start": "node ./sandbox/server.js",
      "test": "grunt test && bundlesize",
      "version": "npm run build && grunt version && git add -A dist && git add CHANGELOG.md bower.json package.json"
    },
    "typings": "./index.d.ts",
    "version": "0.19.2"
  }
  
  },{}],113:[function(require,module,exports){
  /*
   * Copyright (c) 2020, Jeff Hlywa (jhlywa@gmail.com)
   * All rights reserved.
   *
   * Redistribution and use in source and binary forms, with or without
   * modification, are permitted provided that the following conditions are met:
   *
   * 1. Redistributions of source code must retain the above copyright notice,
   *    this list of conditions and the following disclaimer.
   * 2. Redistributions in binary form must reproduce the above copyright notice,
   *    this list of conditions and the following disclaimer in the documentation
   *    and/or other materials provided with the distribution.
   *
   * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
   * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
   * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
   * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
   * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
   * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
   * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
   * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
   * POSSIBILITY OF SUCH DAMAGE.
   *
   *----------------------------------------------------------------------------*/
  
  /* minified license below  */
  
  /* @license
   * Copyright (c) 2018, Jeff Hlywa (jhlywa@gmail.com)
   * Released under the BSD license
   * https://github.com/jhlywa/chess.js/blob/master/LICENSE
   */
  
  var Chess = function(fen) {
    var BLACK = 'b'
    var WHITE = 'w'
  
    var EMPTY = -1
  
    var PAWN = 'p'
    var KNIGHT = 'n'
    var BISHOP = 'b'
    var ROOK = 'r'
    var QUEEN = 'q'
    var KING = 'k'
  
    var SYMBOLS = 'pnbrqkPNBRQK'
  
    var DEFAULT_POSITION =
      'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
  
    var POSSIBLE_RESULTS = ['1-0', '0-1', '1/2-1/2', '*']
  
    var PAWN_OFFSETS = {
      b: [16, 32, 17, 15],
      w: [-16, -32, -17, -15]
    }
  
    var PIECE_OFFSETS = {
      n: [-18, -33, -31, -14, 18, 33, 31, 14],
      b: [-17, -15, 17, 15],
      r: [-16, 1, 16, -1],
      q: [-17, -16, -15, 1, 17, 16, 15, -1],
      k: [-17, -16, -15, 1, 17, 16, 15, -1]
    }
  
    // prettier-ignore
    var ATTACKS = [
      20, 0, 0, 0, 0, 0, 0, 24,  0, 0, 0, 0, 0, 0,20, 0,
       0,20, 0, 0, 0, 0, 0, 24,  0, 0, 0, 0, 0,20, 0, 0,
       0, 0,20, 0, 0, 0, 0, 24,  0, 0, 0, 0,20, 0, 0, 0,
       0, 0, 0,20, 0, 0, 0, 24,  0, 0, 0,20, 0, 0, 0, 0,
       0, 0, 0, 0,20, 0, 0, 24,  0, 0,20, 0, 0, 0, 0, 0,
       0, 0, 0, 0, 0,20, 2, 24,  2,20, 0, 0, 0, 0, 0, 0,
       0, 0, 0, 0, 0, 2,53, 56, 53, 2, 0, 0, 0, 0, 0, 0,
      24,24,24,24,24,24,56,  0, 56,24,24,24,24,24,24, 0,
       0, 0, 0, 0, 0, 2,53, 56, 53, 2, 0, 0, 0, 0, 0, 0,
       0, 0, 0, 0, 0,20, 2, 24,  2,20, 0, 0, 0, 0, 0, 0,
       0, 0, 0, 0,20, 0, 0, 24,  0, 0,20, 0, 0, 0, 0, 0,
       0, 0, 0,20, 0, 0, 0, 24,  0, 0, 0,20, 0, 0, 0, 0,
       0, 0,20, 0, 0, 0, 0, 24,  0, 0, 0, 0,20, 0, 0, 0,
       0,20, 0, 0, 0, 0, 0, 24,  0, 0, 0, 0, 0,20, 0, 0,
      20, 0, 0, 0, 0, 0, 0, 24,  0, 0, 0, 0, 0, 0,20
    ];
  
    // prettier-ignore
    var RAYS = [
       17,  0,  0,  0,  0,  0,  0, 16,  0,  0,  0,  0,  0,  0, 15, 0,
        0, 17,  0,  0,  0,  0,  0, 16,  0,  0,  0,  0,  0, 15,  0, 0,
        0,  0, 17,  0,  0,  0,  0, 16,  0,  0,  0,  0, 15,  0,  0, 0,
        0,  0,  0, 17,  0,  0,  0, 16,  0,  0,  0, 15,  0,  0,  0, 0,
        0,  0,  0,  0, 17,  0,  0, 16,  0,  0, 15,  0,  0,  0,  0, 0,
        0,  0,  0,  0,  0, 17,  0, 16,  0, 15,  0,  0,  0,  0,  0, 0,
        0,  0,  0,  0,  0,  0, 17, 16, 15,  0,  0,  0,  0,  0,  0, 0,
        1,  1,  1,  1,  1,  1,  1,  0, -1, -1,  -1,-1, -1, -1, -1, 0,
        0,  0,  0,  0,  0,  0,-15,-16,-17,  0,  0,  0,  0,  0,  0, 0,
        0,  0,  0,  0,  0,-15,  0,-16,  0,-17,  0,  0,  0,  0,  0, 0,
        0,  0,  0,  0,-15,  0,  0,-16,  0,  0,-17,  0,  0,  0,  0, 0,
        0,  0,  0,-15,  0,  0,  0,-16,  0,  0,  0,-17,  0,  0,  0, 0,
        0,  0,-15,  0,  0,  0,  0,-16,  0,  0,  0,  0,-17,  0,  0, 0,
        0,-15,  0,  0,  0,  0,  0,-16,  0,  0,  0,  0,  0,-17,  0, 0,
      -15,  0,  0,  0,  0,  0,  0,-16,  0,  0,  0,  0,  0,  0,-17
    ];
  
    var SHIFTS = { p: 0, n: 1, b: 2, r: 3, q: 4, k: 5 }
  
    var FLAGS = {
      NORMAL: 'n',
      CAPTURE: 'c',
      BIG_PAWN: 'b',
      EP_CAPTURE: 'e',
      PROMOTION: 'p',
      KSIDE_CASTLE: 'k',
      QSIDE_CASTLE: 'q'
    }
  
    var BITS = {
      NORMAL: 1,
      CAPTURE: 2,
      BIG_PAWN: 4,
      EP_CAPTURE: 8,
      PROMOTION: 16,
      KSIDE_CASTLE: 32,
      QSIDE_CASTLE: 64
    }
  
    var RANK_1 = 7
    var RANK_2 = 6
    var RANK_3 = 5
    var RANK_4 = 4
    var RANK_5 = 3
    var RANK_6 = 2
    var RANK_7 = 1
    var RANK_8 = 0
  
    // prettier-ignore
    var SQUARES = {
      a8:   0, b8:   1, c8:   2, d8:   3, e8:   4, f8:   5, g8:   6, h8:   7,
      a7:  16, b7:  17, c7:  18, d7:  19, e7:  20, f7:  21, g7:  22, h7:  23,
      a6:  32, b6:  33, c6:  34, d6:  35, e6:  36, f6:  37, g6:  38, h6:  39,
      a5:  48, b5:  49, c5:  50, d5:  51, e5:  52, f5:  53, g5:  54, h5:  55,
      a4:  64, b4:  65, c4:  66, d4:  67, e4:  68, f4:  69, g4:  70, h4:  71,
      a3:  80, b3:  81, c3:  82, d3:  83, e3:  84, f3:  85, g3:  86, h3:  87,
      a2:  96, b2:  97, c2:  98, d2:  99, e2: 100, f2: 101, g2: 102, h2: 103,
      a1: 112, b1: 113, c1: 114, d1: 115, e1: 116, f1: 117, g1: 118, h1: 119
    };
  
    var ROOKS = {
      w: [
        { square: SQUARES.a1, flag: BITS.QSIDE_CASTLE },
        { square: SQUARES.h1, flag: BITS.KSIDE_CASTLE }
      ],
      b: [
        { square: SQUARES.a8, flag: BITS.QSIDE_CASTLE },
        { square: SQUARES.h8, flag: BITS.KSIDE_CASTLE }
      ]
    }
  
    var board = new Array(128)
    var kings = { w: EMPTY, b: EMPTY }
    var turn = WHITE
    var castling = { w: 0, b: 0 }
    var ep_square = EMPTY
    var half_moves = 0
    var move_number = 1
    var history = []
    var header = {}
  
    /* if the user passes in a fen string, load it, else default to
     * starting position
     */
    if (typeof fen === 'undefined') {
      load(DEFAULT_POSITION)
    } else {
      load(fen)
    }
  
    function clear(keep_headers) {
      if (typeof keep_headers === 'undefined') {
        keep_headers = false
      }
  
      board = new Array(128)
      kings = { w: EMPTY, b: EMPTY }
      turn = WHITE
      castling = { w: 0, b: 0 }
      ep_square = EMPTY
      half_moves = 0
      move_number = 1
      history = []
      if (!keep_headers) header = {}
      update_setup(generate_fen())
    }
  
    function reset() {
      load(DEFAULT_POSITION)
    }
  
    function load(fen, keep_headers) {
      if (typeof keep_headers === 'undefined') {
        keep_headers = false
      }
  
      var tokens = fen.split(/\s+/)
      var position = tokens[0]
      var square = 0
  
      if (!validate_fen(fen).valid) {
        return false
      }
  
      clear(keep_headers)
  
      for (var i = 0; i < position.length; i++) {
        var piece = position.charAt(i)
  
        if (piece === '/') {
          square += 8
        } else if (is_digit(piece)) {
          square += parseInt(piece, 10)
        } else {
          var color = piece < 'a' ? WHITE : BLACK
          put({ type: piece.toLowerCase(), color: color }, algebraic(square))
          square++
        }
      }
  
      turn = tokens[1]
  
      if (tokens[2].indexOf('K') > -1) {
        castling.w |= BITS.KSIDE_CASTLE
      }
      if (tokens[2].indexOf('Q') > -1) {
        castling.w |= BITS.QSIDE_CASTLE
      }
      if (tokens[2].indexOf('k') > -1) {
        castling.b |= BITS.KSIDE_CASTLE
      }
      if (tokens[2].indexOf('q') > -1) {
        castling.b |= BITS.QSIDE_CASTLE
      }
  
      ep_square = tokens[3] === '-' ? EMPTY : SQUARES[tokens[3]]
      half_moves = parseInt(tokens[4], 10)
      move_number = parseInt(tokens[5], 10)
  
      update_setup(generate_fen())
  
      return true
    }
  
    /* TODO: this function is pretty much crap - it validates structure but
     * completely ignores content (e.g. doesn't verify that each side has a king)
     * ... we should rewrite this, and ditch the silly error_number field while
     * we're at it
     */
    function validate_fen(fen) {
      var errors = {
        0: 'No errors.',
        1: 'FEN string must contain six space-delimited fields.',
        2: '6th field (move number) must be a positive integer.',
        3: '5th field (half move counter) must be a non-negative integer.',
        4: '4th field (en-passant square) is invalid.',
        5: '3rd field (castling availability) is invalid.',
        6: '2nd field (side to move) is invalid.',
        7: "1st field (piece positions) does not contain 8 '/'-delimited rows.",
        8: '1st field (piece positions) is invalid [consecutive numbers].',
        9: '1st field (piece positions) is invalid [invalid piece].',
        10: '1st field (piece positions) is invalid [row too large].',
        11: 'Illegal en-passant square'
      }
  
      /* 1st criterion: 6 space-seperated fields? */
      var tokens = fen.split(/\s+/)
      if (tokens.length !== 6) {
        return { valid: false, error_number: 1, error: errors[1] }
      }
  
      /* 2nd criterion: move number field is a integer value > 0? */
      if (isNaN(tokens[5]) || parseInt(tokens[5], 10) <= 0) {
        return { valid: false, error_number: 2, error: errors[2] }
      }
  
      /* 3rd criterion: half move counter is an integer >= 0? */
      if (isNaN(tokens[4]) || parseInt(tokens[4], 10) < 0) {
        return { valid: false, error_number: 3, error: errors[3] }
      }
  
      /* 4th criterion: 4th field is a valid e.p.-string? */
      if (!/^(-|[abcdefgh][36])$/.test(tokens[3])) {
        return { valid: false, error_number: 4, error: errors[4] }
      }
  
      /* 5th criterion: 3th field is a valid castle-string? */
      if (!/^(KQ?k?q?|Qk?q?|kq?|q|-)$/.test(tokens[2])) {
        return { valid: false, error_number: 5, error: errors[5] }
      }
  
      /* 6th criterion: 2nd field is "w" (white) or "b" (black)? */
      if (!/^(w|b)$/.test(tokens[1])) {
        return { valid: false, error_number: 6, error: errors[6] }
      }
  
      /* 7th criterion: 1st field contains 8 rows? */
      var rows = tokens[0].split('/')
      if (rows.length !== 8) {
        return { valid: false, error_number: 7, error: errors[7] }
      }
  
      /* 8th criterion: every row is valid? */
      for (var i = 0; i < rows.length; i++) {
        /* check for right sum of fields AND not two numbers in succession */
        var sum_fields = 0
        var previous_was_number = false
  
        for (var k = 0; k < rows[i].length; k++) {
          if (!isNaN(rows[i][k])) {
            if (previous_was_number) {
              return { valid: false, error_number: 8, error: errors[8] }
            }
            sum_fields += parseInt(rows[i][k], 10)
            previous_was_number = true
          } else {
            if (!/^[prnbqkPRNBQK]$/.test(rows[i][k])) {
              return { valid: false, error_number: 9, error: errors[9] }
            }
            sum_fields += 1
            previous_was_number = false
          }
        }
        if (sum_fields !== 8) {
          return { valid: false, error_number: 10, error: errors[10] }
        }
      }
  
      if (
        (tokens[3][1] == '3' && tokens[1] == 'w') ||
        (tokens[3][1] == '6' && tokens[1] == 'b')
      ) {
        return { valid: false, error_number: 11, error: errors[11] }
      }
  
      /* everything's okay! */
      return { valid: true, error_number: 0, error: errors[0] }
    }
  
    function generate_fen() {
      var empty = 0
      var fen = ''
  
      for (var i = SQUARES.a8; i <= SQUARES.h1; i++) {
        if (board[i] == null) {
          empty++
        } else {
          if (empty > 0) {
            fen += empty
            empty = 0
          }
          var color = board[i].color
          var piece = board[i].type
  
          fen += color === WHITE ? piece.toUpperCase() : piece.toLowerCase()
        }
  
        if ((i + 1) & 0x88) {
          if (empty > 0) {
            fen += empty
          }
  
          if (i !== SQUARES.h1) {
            fen += '/'
          }
  
          empty = 0
          i += 8
        }
      }
  
      var cflags = ''
      if (castling[WHITE] & BITS.KSIDE_CASTLE) {
        cflags += 'K'
      }
      if (castling[WHITE] & BITS.QSIDE_CASTLE) {
        cflags += 'Q'
      }
      if (castling[BLACK] & BITS.KSIDE_CASTLE) {
        cflags += 'k'
      }
      if (castling[BLACK] & BITS.QSIDE_CASTLE) {
        cflags += 'q'
      }
  
      /* do we have an empty castling flag? */
      cflags = cflags || '-'
      var epflags = ep_square === EMPTY ? '-' : algebraic(ep_square)
  
      return [fen, turn, cflags, epflags, half_moves, move_number].join(' ')
    }
  
    function set_header(args) {
      for (var i = 0; i < args.length; i += 2) {
        if (typeof args[i] === 'string' && typeof args[i + 1] === 'string') {
          header[args[i]] = args[i + 1]
        }
      }
      return header
    }
  
    /* called when the initial board setup is changed with put() or remove().
     * modifies the SetUp and FEN properties of the header object.  if the FEN is
     * equal to the default position, the SetUp and FEN are deleted
     * the setup is only updated if history.length is zero, ie moves haven't been
     * made.
     */
    function update_setup(fen) {
      if (history.length > 0) return
  
      if (fen !== DEFAULT_POSITION) {
        header['SetUp'] = '1'
        header['FEN'] = fen
      } else {
        delete header['SetUp']
        delete header['FEN']
      }
    }
  
    function get(square) {
      var piece = board[SQUARES[square]]
      return piece ? { type: piece.type, color: piece.color } : null
    }
  
    function put(piece, square) {
      /* check for valid piece object */
      if (!('type' in piece && 'color' in piece)) {
        return false
      }
  
      /* check for piece */
      if (SYMBOLS.indexOf(piece.type.toLowerCase()) === -1) {
        return false
      }
  
      /* check for valid square */
      if (!(square in SQUARES)) {
        return false
      }
  
      var sq = SQUARES[square]
  
      /* don't let the user place more than one king */
      if (
        piece.type == KING &&
        !(kings[piece.color] == EMPTY || kings[piece.color] == sq)
      ) {
        return false
      }
  
      board[sq] = { type: piece.type, color: piece.color }
      if (piece.type === KING) {
        kings[piece.color] = sq
      }
  
      update_setup(generate_fen())
  
      return true
    }
  
    function remove(square) {
      var piece = get(square)
      board[SQUARES[square]] = null
      if (piece && piece.type === KING) {
        kings[piece.color] = EMPTY
      }
  
      update_setup(generate_fen())
  
      return piece
    }
  
    function build_move(board, from, to, flags, promotion) {
      var move = {
        color: turn,
        from: from,
        to: to,
        flags: flags,
        piece: board[from].type
      }
  
      if (promotion) {
        move.flags |= BITS.PROMOTION
        move.promotion = promotion
      }
  
      if (board[to]) {
        move.captured = board[to].type
      } else if (flags & BITS.EP_CAPTURE) {
        move.captured = PAWN
      }
      return move
    }
  
    function generate_moves(options) {
      function add_move(board, moves, from, to, flags) {
        /* if pawn promotion */
        if (
          board[from].type === PAWN &&
          (rank(to) === RANK_8 || rank(to) === RANK_1)
        ) {
          var pieces = [QUEEN, ROOK, BISHOP, KNIGHT]
          for (var i = 0, len = pieces.length; i < len; i++) {
            moves.push(build_move(board, from, to, flags, pieces[i]))
          }
        } else {
          moves.push(build_move(board, from, to, flags))
        }
      }
  
      var moves = []
      var us = turn
      var them = swap_color(us)
      var second_rank = { b: RANK_7, w: RANK_2 }
  
      var first_sq = SQUARES.a8
      var last_sq = SQUARES.h1
      var single_square = false
  
      /* do we want legal moves? */
      var legal =
        typeof options !== 'undefined' && 'legal' in options
          ? options.legal
          : true
  
      /* are we generating moves for a single square? */
      if (typeof options !== 'undefined' && 'square' in options) {
        if (options.square in SQUARES) {
          first_sq = last_sq = SQUARES[options.square]
          single_square = true
        } else {
          /* invalid square */
          return []
        }
      }
  
      for (var i = first_sq; i <= last_sq; i++) {
        /* did we run off the end of the board */
        if (i & 0x88) {
          i += 7
          continue
        }
  
        var piece = board[i]
        if (piece == null || piece.color !== us) {
          continue
        }
  
        if (piece.type === PAWN) {
          /* single square, non-capturing */
          var square = i + PAWN_OFFSETS[us][0]
          if (board[square] == null) {
            add_move(board, moves, i, square, BITS.NORMAL)
  
            /* double square */
            var square = i + PAWN_OFFSETS[us][1]
            if (second_rank[us] === rank(i) && board[square] == null) {
              add_move(board, moves, i, square, BITS.BIG_PAWN)
            }
          }
  
          /* pawn captures */
          for (j = 2; j < 4; j++) {
            var square = i + PAWN_OFFSETS[us][j]
            if (square & 0x88) continue
  
            if (board[square] != null && board[square].color === them) {
              add_move(board, moves, i, square, BITS.CAPTURE)
            } else if (square === ep_square) {
              add_move(board, moves, i, ep_square, BITS.EP_CAPTURE)
            }
          }
        } else {
          for (var j = 0, len = PIECE_OFFSETS[piece.type].length; j < len; j++) {
            var offset = PIECE_OFFSETS[piece.type][j]
            var square = i
  
            while (true) {
              square += offset
              if (square & 0x88) break
  
              if (board[square] == null) {
                add_move(board, moves, i, square, BITS.NORMAL)
              } else {
                if (board[square].color === us) break
                add_move(board, moves, i, square, BITS.CAPTURE)
                break
              }
  
              /* break, if knight or king */
              if (piece.type === 'n' || piece.type === 'k') break
            }
          }
        }
      }
  
      /* check for castling if: a) we're generating all moves, or b) we're doing
       * single square move generation on the king's square
       */
      if (!single_square || last_sq === kings[us]) {
        /* king-side castling */
        if (castling[us] & BITS.KSIDE_CASTLE) {
          var castling_from = kings[us]
          var castling_to = castling_from + 2
  
          if (
            board[castling_from + 1] == null &&
            board[castling_to] == null &&
            !attacked(them, kings[us]) &&
            !attacked(them, castling_from + 1) &&
            !attacked(them, castling_to)
          ) {
            add_move(board, moves, kings[us], castling_to, BITS.KSIDE_CASTLE)
          }
        }
  
        /* queen-side castling */
        if (castling[us] & BITS.QSIDE_CASTLE) {
          var castling_from = kings[us]
          var castling_to = castling_from - 2
  
          if (
            board[castling_from - 1] == null &&
            board[castling_from - 2] == null &&
            board[castling_from - 3] == null &&
            !attacked(them, kings[us]) &&
            !attacked(them, castling_from - 1) &&
            !attacked(them, castling_to)
          ) {
            add_move(board, moves, kings[us], castling_to, BITS.QSIDE_CASTLE)
          }
        }
      }
  
      /* return all pseudo-legal moves (this includes moves that allow the king
       * to be captured)
       */
      if (!legal) {
        return moves
      }
  
      /* filter out illegal moves */
      var legal_moves = []
      for (var i = 0, len = moves.length; i < len; i++) {
        make_move(moves[i])
        if (!king_attacked(us)) {
          legal_moves.push(moves[i])
        }
        undo_move()
      }
  
      return legal_moves
    }
  
    /* convert a move from 0x88 coordinates to Standard Algebraic Notation
     * (SAN)
     *
     * @param {boolean} sloppy Use the sloppy SAN generator to work around over
     * disambiguation bugs in Fritz and Chessbase.  See below:
     *
     * r1bqkbnr/ppp2ppp/2n5/1B1pP3/4P3/8/PPPP2PP/RNBQK1NR b KQkq - 2 4
     * 4. ... Nge7 is overly disambiguated because the knight on c6 is pinned
     * 4. ... Ne7 is technically the valid SAN
     */
    function move_to_san(move, sloppy) {
      var output = ''
  
      if (move.flags & BITS.KSIDE_CASTLE) {
        output = 'O-O'
      } else if (move.flags & BITS.QSIDE_CASTLE) {
        output = 'O-O-O'
      } else {
        var disambiguator = get_disambiguator(move, sloppy)
  
        if (move.piece !== PAWN) {
          output += move.piece.toUpperCase() + disambiguator
        }
  
        if (move.flags & (BITS.CAPTURE | BITS.EP_CAPTURE)) {
          if (move.piece === PAWN) {
            output += algebraic(move.from)[0]
          }
          output += 'x'
        }
  
        output += algebraic(move.to)
  
        if (move.flags & BITS.PROMOTION) {
          output += '=' + move.promotion.toUpperCase()
        }
      }
  
      make_move(move)
      if (in_check()) {
        if (in_checkmate()) {
          output += '#'
        } else {
          output += '+'
        }
      }
      undo_move()
  
      return output
    }
  
    // parses all of the decorators out of a SAN string
    function stripped_san(move) {
      return move.replace(/=/, '').replace(/[+#]?[?!]*$/, '')
    }
  
    function attacked(color, square) {
      for (var i = SQUARES.a8; i <= SQUARES.h1; i++) {
        /* did we run off the end of the board */
        if (i & 0x88) {
          i += 7
          continue
        }
  
        /* if empty square or wrong color */
        if (board[i] == null || board[i].color !== color) continue
  
        var piece = board[i]
        var difference = i - square
        var index = difference + 119
  
        if (ATTACKS[index] & (1 << SHIFTS[piece.type])) {
          if (piece.type === PAWN) {
            if (difference > 0) {
              if (piece.color === WHITE) return true
            } else {
              if (piece.color === BLACK) return true
            }
            continue
          }
  
          /* if the piece is a knight or a king */
          if (piece.type === 'n' || piece.type === 'k') return true
  
          var offset = RAYS[index]
          var j = i + offset
  
          var blocked = false
          while (j !== square) {
            if (board[j] != null) {
              blocked = true
              break
            }
            j += offset
          }
  
          if (!blocked) return true
        }
      }
  
      return false
    }
  
    function king_attacked(color) {
      return attacked(swap_color(color), kings[color])
    }
  
    function in_check() {
      return king_attacked(turn)
    }
  
    function in_checkmate() {
      return in_check() && generate_moves().length === 0
    }
  
    function in_stalemate() {
      return !in_check() && generate_moves().length === 0
    }
  
    function insufficient_material() {
      var pieces = {}
      var bishops = []
      var num_pieces = 0
      var sq_color = 0
  
      for (var i = SQUARES.a8; i <= SQUARES.h1; i++) {
        sq_color = (sq_color + 1) % 2
        if (i & 0x88) {
          i += 7
          continue
        }
  
        var piece = board[i]
        if (piece) {
          pieces[piece.type] = piece.type in pieces ? pieces[piece.type] + 1 : 1
          if (piece.type === BISHOP) {
            bishops.push(sq_color)
          }
          num_pieces++
        }
      }
  
      /* k vs. k */
      if (num_pieces === 2) {
        return true
      } else if (
        /* k vs. kn .... or .... k vs. kb */
        num_pieces === 3 &&
        (pieces[BISHOP] === 1 || pieces[KNIGHT] === 1)
      ) {
        return true
      } else if (num_pieces === pieces[BISHOP] + 2) {
        /* kb vs. kb where any number of bishops are all on the same color */
        var sum = 0
        var len = bishops.length
        for (var i = 0; i < len; i++) {
          sum += bishops[i]
        }
        if (sum === 0 || sum === len) {
          return true
        }
      }
  
      return false
    }
  
    function in_threefold_repetition() {
      /* TODO: while this function is fine for casual use, a better
       * implementation would use a Zobrist key (instead of FEN). the
       * Zobrist key would be maintained in the make_move/undo_move functions,
       * avoiding the costly that we do below.
       */
      var moves = []
      var positions = {}
      var repetition = false
  
      while (true) {
        var move = undo_move()
        if (!move) break
        moves.push(move)
      }
  
      while (true) {
        /* remove the last two fields in the FEN string, they're not needed
         * when checking for draw by rep */
        var fen = generate_fen()
          .split(' ')
          .slice(0, 4)
          .join(' ')
  
        /* has the position occurred three or move times */
        positions[fen] = fen in positions ? positions[fen] + 1 : 1
        if (positions[fen] >= 3) {
          repetition = true
        }
  
        if (!moves.length) {
          break
        }
        make_move(moves.pop())
      }
  
      return repetition
    }
  
    function push(move) {
      history.push({
        move: move,
        kings: { b: kings.b, w: kings.w },
        turn: turn,
        castling: { b: castling.b, w: castling.w },
        ep_square: ep_square,
        half_moves: half_moves,
        move_number: move_number
      })
    }
  
    function make_move(move) {
      var us = turn
      var them = swap_color(us)
      push(move)
  
      board[move.to] = board[move.from]
      board[move.from] = null
  
      /* if ep capture, remove the captured pawn */
      if (move.flags & BITS.EP_CAPTURE) {
        if (turn === BLACK) {
          board[move.to - 16] = null
        } else {
          board[move.to + 16] = null
        }
      }
  
      /* if pawn promotion, replace with new piece */
      if (move.flags & BITS.PROMOTION) {
        board[move.to] = { type: move.promotion, color: us }
      }
  
      /* if we moved the king */
      if (board[move.to].type === KING) {
        kings[board[move.to].color] = move.to
  
        /* if we castled, move the rook next to the king */
        if (move.flags & BITS.KSIDE_CASTLE) {
          var castling_to = move.to - 1
          var castling_from = move.to + 1
          board[castling_to] = board[castling_from]
          board[castling_from] = null
        } else if (move.flags & BITS.QSIDE_CASTLE) {
          var castling_to = move.to + 1
          var castling_from = move.to - 2
          board[castling_to] = board[castling_from]
          board[castling_from] = null
        }
  
        /* turn off castling */
        castling[us] = ''
      }
  
      /* turn off castling if we move a rook */
      if (castling[us]) {
        for (var i = 0, len = ROOKS[us].length; i < len; i++) {
          if (
            move.from === ROOKS[us][i].square &&
            castling[us] & ROOKS[us][i].flag
          ) {
            castling[us] ^= ROOKS[us][i].flag
            break
          }
        }
      }
  
      /* turn off castling if we capture a rook */
      if (castling[them]) {
        for (var i = 0, len = ROOKS[them].length; i < len; i++) {
          if (
            move.to === ROOKS[them][i].square &&
            castling[them] & ROOKS[them][i].flag
          ) {
            castling[them] ^= ROOKS[them][i].flag
            break
          }
        }
      }
  
      /* if big pawn move, update the en passant square */
      if (move.flags & BITS.BIG_PAWN) {
        if (turn === 'b') {
          ep_square = move.to - 16
        } else {
          ep_square = move.to + 16
        }
      } else {
        ep_square = EMPTY
      }
  
      /* reset the 50 move counter if a pawn is moved or a piece is captured */
      if (move.piece === PAWN) {
        half_moves = 0
      } else if (move.flags & (BITS.CAPTURE | BITS.EP_CAPTURE)) {
        half_moves = 0
      } else {
        half_moves++
      }
  
      if (turn === BLACK) {
        move_number++
      }
      turn = swap_color(turn)
    }
  
    function undo_move() {
      var old = history.pop()
      if (old == null) {
        return null
      }
  
      var move = old.move
      kings = old.kings
      turn = old.turn
      castling = old.castling
      ep_square = old.ep_square
      half_moves = old.half_moves
      move_number = old.move_number
  
      var us = turn
      var them = swap_color(turn)
  
      board[move.from] = board[move.to]
      board[move.from].type = move.piece // to undo any promotions
      board[move.to] = null
  
      if (move.flags & BITS.CAPTURE) {
        board[move.to] = { type: move.captured, color: them }
      } else if (move.flags & BITS.EP_CAPTURE) {
        var index
        if (us === BLACK) {
          index = move.to - 16
        } else {
          index = move.to + 16
        }
        board[index] = { type: PAWN, color: them }
      }
  
      if (move.flags & (BITS.KSIDE_CASTLE | BITS.QSIDE_CASTLE)) {
        var castling_to, castling_from
        if (move.flags & BITS.KSIDE_CASTLE) {
          castling_to = move.to + 1
          castling_from = move.to - 1
        } else if (move.flags & BITS.QSIDE_CASTLE) {
          castling_to = move.to - 2
          castling_from = move.to + 1
        }
  
        board[castling_to] = board[castling_from]
        board[castling_from] = null
      }
  
      return move
    }
  
    /* this function is used to uniquely identify ambiguous moves */
    function get_disambiguator(move, sloppy) {
      var moves = generate_moves({ legal: !sloppy })
  
      var from = move.from
      var to = move.to
      var piece = move.piece
  
      var ambiguities = 0
      var same_rank = 0
      var same_file = 0
  
      for (var i = 0, len = moves.length; i < len; i++) {
        var ambig_from = moves[i].from
        var ambig_to = moves[i].to
        var ambig_piece = moves[i].piece
  
        /* if a move of the same piece type ends on the same to square, we'll
         * need to add a disambiguator to the algebraic notation
         */
        if (piece === ambig_piece && from !== ambig_from && to === ambig_to) {
          ambiguities++
  
          if (rank(from) === rank(ambig_from)) {
            same_rank++
          }
  
          if (file(from) === file(ambig_from)) {
            same_file++
          }
        }
      }
  
      if (ambiguities > 0) {
        /* if there exists a similar moving piece on the same rank and file as
         * the move in question, use the square as the disambiguator
         */
        if (same_rank > 0 && same_file > 0) {
          return algebraic(from)
        } else if (same_file > 0) {
          /* if the moving piece rests on the same file, use the rank symbol as the
           * disambiguator
           */
          return algebraic(from).charAt(1)
        } else {
          /* else use the file symbol */
          return algebraic(from).charAt(0)
        }
      }
  
      return ''
    }
  
    function ascii() {
      var s = '   +------------------------+\n'
      for (var i = SQUARES.a8; i <= SQUARES.h1; i++) {
        /* display the rank */
        if (file(i) === 0) {
          s += ' ' + '87654321'[rank(i)] + ' |'
        }
  
        /* empty piece */
        if (board[i] == null) {
          s += ' . '
        } else {
          var piece = board[i].type
          var color = board[i].color
          var symbol = color === WHITE ? piece.toUpperCase() : piece.toLowerCase()
          s += ' ' + symbol + ' '
        }
  
        if ((i + 1) & 0x88) {
          s += '|\n'
          i += 8
        }
      }
      s += '   +------------------------+\n'
      s += '     a  b  c  d  e  f  g  h\n'
  
      return s
    }
  
    // convert a move from Standard Algebraic Notation (SAN) to 0x88 coordinates
    function move_from_san(move, sloppy) {
      // strip off any move decorations: e.g Nf3+?!
      var clean_move = stripped_san(move)
  
      // if we're using the sloppy parser run a regex to grab piece, to, and from
      // this should parse invalid SAN like: Pe2-e4, Rc1c4, Qf3xf7
      if (sloppy) {
        var matches = clean_move.match(
          /([pnbrqkPNBRQK])?([a-h][1-8])x?-?([a-h][1-8])([qrbnQRBN])?/
        )
        if (matches) {
          var piece = matches[1]
          var from = matches[2]
          var to = matches[3]
          var promotion = matches[4]
        }
      }
  
      var moves = generate_moves()
      for (var i = 0, len = moves.length; i < len; i++) {
        // try the strict parser first, then the sloppy parser if requested
        // by the user
        if (
          clean_move === stripped_san(move_to_san(moves[i])) ||
          (sloppy && clean_move === stripped_san(move_to_san(moves[i], true)))
        ) {
          return moves[i]
        } else {
          if (
            matches &&
            (!piece || piece.toLowerCase() == moves[i].piece) &&
            SQUARES[from] == moves[i].from &&
            SQUARES[to] == moves[i].to &&
            (!promotion || promotion.toLowerCase() == moves[i].promotion)
          ) {
            return moves[i]
          }
        }
      }
  
      return null
    }
  
    /*****************************************************************************
     * UTILITY FUNCTIONS
     ****************************************************************************/
    function rank(i) {
      return i >> 4
    }
  
    function file(i) {
      return i & 15
    }
  
    function algebraic(i) {
      var f = file(i),
        r = rank(i)
      return 'abcdefgh'.substring(f, f + 1) + '87654321'.substring(r, r + 1)
    }
  
    function swap_color(c) {
      return c === WHITE ? BLACK : WHITE
    }
  
    function is_digit(c) {
      return '0123456789'.indexOf(c) !== -1
    }
  
    /* pretty = external move object */
    function make_pretty(ugly_move) {
      var move = clone(ugly_move)
      move.san = move_to_san(move, false)
      move.to = algebraic(move.to)
      move.from = algebraic(move.from)
  
      var flags = ''
  
      for (var flag in BITS) {
        if (BITS[flag] & move.flags) {
          flags += FLAGS[flag]
        }
      }
      move.flags = flags
  
      return move
    }
  
    function clone(obj) {
      var dupe = obj instanceof Array ? [] : {}
  
      for (var property in obj) {
        if (typeof property === 'object') {
          dupe[property] = clone(obj[property])
        } else {
          dupe[property] = obj[property]
        }
      }
  
      return dupe
    }
  
    function trim(str) {
      return str.replace(/^\s+|\s+$/g, '')
    }
  
    /*****************************************************************************
     * DEBUGGING UTILITIES
     ****************************************************************************/
    function perft(depth) {
      var moves = generate_moves({ legal: false })
      var nodes = 0
      var color = turn
  
      for (var i = 0, len = moves.length; i < len; i++) {
        make_move(moves[i])
        if (!king_attacked(color)) {
          if (depth - 1 > 0) {
            var child_nodes = perft(depth - 1)
            nodes += child_nodes
          } else {
            nodes++
          }
        }
        undo_move()
      }
  
      return nodes
    }
  
    return {
      /***************************************************************************
       * PUBLIC CONSTANTS (is there a better way to do this?)
       **************************************************************************/
      WHITE: WHITE,
      BLACK: BLACK,
      PAWN: PAWN,
      KNIGHT: KNIGHT,
      BISHOP: BISHOP,
      ROOK: ROOK,
      QUEEN: QUEEN,
      KING: KING,
      SQUARES: (function() {
        /* from the ECMA-262 spec (section 12.6.4):
         * "The mechanics of enumerating the properties ... is
         * implementation dependent"
         * so: for (var sq in SQUARES) { keys.push(sq); } might not be
         * ordered correctly
         */
        var keys = []
        for (var i = SQUARES.a8; i <= SQUARES.h1; i++) {
          if (i & 0x88) {
            i += 7
            continue
          }
          keys.push(algebraic(i))
        }
        return keys
      })(),
      FLAGS: FLAGS,
  
      /***************************************************************************
       * PUBLIC API
       **************************************************************************/
      load: function(fen) {
        return load(fen)
      },
  
      reset: function() {
        return reset()
      },
  
      moves: function(options) {
        /* The internal representation of a chess move is in 0x88 format, and
         * not meant to be human-readable.  The code below converts the 0x88
         * square coordinates to algebraic coordinates.  It also prunes an
         * unnecessary move keys resulting from a verbose call.
         */
  
        var ugly_moves = generate_moves(options)
        var moves = []
  
        for (var i = 0, len = ugly_moves.length; i < len; i++) {
          /* does the user want a full move object (most likely not), or just
           * SAN
           */
          if (
            typeof options !== 'undefined' &&
            'verbose' in options &&
            options.verbose
          ) {
            moves.push(make_pretty(ugly_moves[i]))
          } else {
            moves.push(move_to_san(ugly_moves[i], false))
          }
        }
  
        return moves
      },
  
      in_check: function() {
        return in_check()
      },
  
      in_checkmate: function() {
        return in_checkmate()
      },
  
      in_stalemate: function() {
        return in_stalemate()
      },
  
      in_draw: function() {
        return (
          half_moves >= 100 ||
          in_stalemate() ||
          insufficient_material() ||
          in_threefold_repetition()
        )
      },
  
      insufficient_material: function() {
        return insufficient_material()
      },
  
      in_threefold_repetition: function() {
        return in_threefold_repetition()
      },
  
      game_over: function() {
        return (
          half_moves >= 100 ||
          in_checkmate() ||
          in_stalemate() ||
          insufficient_material() ||
          in_threefold_repetition()
        )
      },
  
      validate_fen: function(fen) {
        return validate_fen(fen)
      },
  
      fen: function() {
        return generate_fen()
      },
  
      board: function() {
        var output = [],
          row = []
  
        for (var i = SQUARES.a8; i <= SQUARES.h1; i++) {
          if (board[i] == null) {
            row.push(null)
          } else {
            row.push({ type: board[i].type, color: board[i].color })
          }
          if ((i + 1) & 0x88) {
            output.push(row)
            row = []
            i += 8
          }
        }
  
        return output
      },
  
      pgn: function(options) {
        /* using the specification from http://www.chessclub.com/help/PGN-spec
         * example for html usage: .pgn({ max_width: 72, newline_char: "<br />" })
         */
        var newline =
          typeof options === 'object' && typeof options.newline_char === 'string'
            ? options.newline_char
            : '\n'
        var max_width =
          typeof options === 'object' && typeof options.max_width === 'number'
            ? options.max_width
            : 0
        var result = []
        var header_exists = false
  
        /* add the PGN header headerrmation */
        for (var i in header) {
          /* TODO: order of enumerated properties in header object is not
           * guaranteed, see ECMA-262 spec (section 12.6.4)
           */
          result.push('[' + i + ' "' + header[i] + '"]' + newline)
          header_exists = true
        }
  
        if (header_exists && history.length) {
          result.push(newline)
        }
  
        /* pop all of history onto reversed_history */
        var reversed_history = []
        while (history.length > 0) {
          reversed_history.push(undo_move())
        }
  
        var moves = []
        var move_string = ''
  
        /* build the list of moves.  a move_string looks like: "3. e3 e6" */
        while (reversed_history.length > 0) {
          var move = reversed_history.pop()
  
          /* if the position started with black to move, start PGN with 1. ... */
          if (!history.length && move.color === 'b') {
            move_string = move_number + '. ...'
          } else if (move.color === 'w') {
            /* store the previous generated move_string if we have one */
            if (move_string.length) {
              moves.push(move_string)
            }
            move_string = move_number + '.'
          }
  
          move_string = move_string + ' ' + move_to_san(move, false)
          make_move(move)
        }
  
        /* are there any other leftover moves? */
        if (move_string.length) {
          moves.push(move_string)
        }
  
        /* is there a result? */
        if (typeof header.Result !== 'undefined') {
          moves.push(header.Result)
        }
  
        /* history should be back to what is was before we started generating PGN,
         * so join together moves
         */
        if (max_width === 0) {
          return result.join('') + moves.join(' ')
        }
  
        /* wrap the PGN output at max_width */
        var current_width = 0
        for (var i = 0; i < moves.length; i++) {
          /* if the current move will push past max_width */
          if (current_width + moves[i].length > max_width && i !== 0) {
            /* don't end the line with whitespace */
            if (result[result.length - 1] === ' ') {
              result.pop()
            }
  
            result.push(newline)
            current_width = 0
          } else if (i !== 0) {
            result.push(' ')
            current_width++
          }
          result.push(moves[i])
          current_width += moves[i].length
        }
  
        return result.join('')
      },
  
      load_pgn: function(pgn, options) {
        // allow the user to specify the sloppy move parser to work around over
        // disambiguation bugs in Fritz and Chessbase
        var sloppy =
          typeof options !== 'undefined' && 'sloppy' in options
            ? options.sloppy
            : false
  
        function mask(str) {
          return str.replace(/\\/g, '\\')
        }
  
        function has_keys(object) {
          for (var key in object) {
            return true
          }
          return false
        }
  
        function parse_pgn_header(header, options) {
          var newline_char =
            typeof options === 'object' &&
            typeof options.newline_char === 'string'
              ? options.newline_char
              : '\r?\n'
          var header_obj = {}
          var headers = header.split(new RegExp(mask(newline_char)))
          var key = ''
          var value = ''
  
          for (var i = 0; i < headers.length; i++) {
            key = headers[i].replace(/^\[([A-Z][A-Za-z]*)\s.*\]$/, '$1')
            value = headers[i].replace(/^\[[A-Za-z]+\s"(.*)"\]$/, '$1')
            if (trim(key).length > 0) {
              header_obj[key] = value
            }
          }
  
          return header_obj
        }
  
        var newline_char =
          typeof options === 'object' && typeof options.newline_char === 'string'
            ? options.newline_char
            : '\r?\n'
  
        // RegExp to split header. Takes advantage of the fact that header and movetext
        // will always have a blank line between them (ie, two newline_char's).
        // With default newline_char, will equal: /^(\[((?:\r?\n)|.)*\])(?:\r?\n){2}/
        var header_regex = new RegExp(
          '^(\\[((?:' +
            mask(newline_char) +
            ')|.)*\\])' +
            '(?:' +
            mask(newline_char) +
            '){2}'
        )
  
        // If no header given, begin with moves.
        var header_string = header_regex.test(pgn)
          ? header_regex.exec(pgn)[1]
          : ''
  
        // Put the board in the starting position
        reset()
  
        /* parse PGN header */
        var headers = parse_pgn_header(header_string, options)
        for (var key in headers) {
          set_header([key, headers[key]])
        }
  
        /* load the starting position indicated by [Setup '1'] and
         * [FEN position] */
        if (headers['SetUp'] === '1') {
          if (!('FEN' in headers && load(headers['FEN'], true))) {
            // second argument to load: don't clear the headers
            return false
          }
        }
  
        /* delete header to get the moves */
        var ms = pgn
          .replace(header_string, '')
          .replace(new RegExp(mask(newline_char), 'g'), ' ')
  
        /* delete comments */
        ms = ms.replace(/(\{[^}]+\})+?/g, '')
  
        /* delete recursive annotation variations */
        var rav_regex = /(\([^\(\)]+\))+?/g
        while (rav_regex.test(ms)) {
          ms = ms.replace(rav_regex, '')
        }
  
        /* delete move numbers */
        ms = ms.replace(/\d+\.(\.\.)?/g, '')
  
        /* delete ... indicating black to move */
        ms = ms.replace(/\.\.\./g, '')
  
        /* delete numeric annotation glyphs */
        ms = ms.replace(/\$\d+/g, '')
  
        /* trim and get array of moves */
        var moves = trim(ms).split(new RegExp(/\s+/))
  
        /* delete empty entries */
        moves = moves
          .join(',')
          .replace(/,,+/g, ',')
          .split(',')
        var move = ''
  
        for (var half_move = 0; half_move < moves.length - 1; half_move++) {
          move = move_from_san(moves[half_move], sloppy)
  
          /* move not possible! (don't clear the board to examine to show the
           * latest valid position)
           */
          if (move == null) {
            return false
          } else {
            make_move(move)
          }
        }
  
        /* examine last move */
        move = moves[moves.length - 1]
        if (POSSIBLE_RESULTS.indexOf(move) > -1) {
          if (has_keys(header) && typeof header.Result === 'undefined') {
            set_header(['Result', move])
          }
        } else {
          move = move_from_san(move, sloppy)
          if (move == null) {
            return false
          } else {
            make_move(move)
          }
        }
        return true
      },
  
      header: function() {
        return set_header(arguments)
      },
  
      ascii: function() {
        return ascii()
      },
  
      turn: function() {
        return turn
      },
  
      move: function(move, options) {
        /* The move function can be called with in the following parameters:
         *
         * .move('Nxb7')      <- where 'move' is a case-sensitive SAN string
         *
         * .move({ from: 'h7', <- where the 'move' is a move object (additional
         *         to :'h8',      fields are ignored)
         *         promotion: 'q',
         *      })
         */
  
        // allow the user to specify the sloppy move parser to work around over
        // disambiguation bugs in Fritz and Chessbase
        var sloppy =
          typeof options !== 'undefined' && 'sloppy' in options
            ? options.sloppy
            : false
  
        var move_obj = null
  
        if (typeof move === 'string') {
          move_obj = move_from_san(move, sloppy)
        } else if (typeof move === 'object') {
          var moves = generate_moves()
  
          /* convert the pretty move object to an ugly move object */
          for (var i = 0, len = moves.length; i < len; i++) {
            if (
              move.from === algebraic(moves[i].from) &&
              move.to === algebraic(moves[i].to) &&
              (!('promotion' in moves[i]) ||
                move.promotion === moves[i].promotion)
            ) {
              move_obj = moves[i]
              break
            }
          }
        }
  
        /* failed to find move */
        if (!move_obj) {
          return null
        }
  
        /* need to make a copy of move because we can't generate SAN after the
         * move is made
         */
        var pretty_move = make_pretty(move_obj)
  
        make_move(move_obj)
  
        return pretty_move
      },
  
      undo: function() {
        var move = undo_move()
        return move ? make_pretty(move) : null
      },
  
      clear: function() {
        return clear()
      },
  
      put: function(piece, square) {
        return put(piece, square)
      },
  
      get: function(square) {
        return get(square)
      },
  
      remove: function(square) {
        return remove(square)
      },
  
      perft: function(depth) {
        return perft(depth)
      },
  
      square_color: function(square) {
        if (square in SQUARES) {
          var sq_0x88 = SQUARES[square]
          return (rank(sq_0x88) + file(sq_0x88)) % 2 === 0 ? 'light' : 'dark'
        }
  
        return null
      },
  
      history: function(options) {
        var reversed_history = []
        var move_history = []
        var verbose =
          typeof options !== 'undefined' &&
          'verbose' in options &&
          options.verbose
  
        while (history.length > 0) {
          reversed_history.push(undo_move())
        }
  
        while (reversed_history.length > 0) {
          var move = reversed_history.pop()
          if (verbose) {
            move_history.push(make_pretty(move))
          } else {
            move_history.push(move_to_san(move))
          }
          make_move(move)
        }
  
        return move_history
      }
    }
  }
  
  /* export Chess object if using node or any other CommonJS compatible
   * environment */
  if (typeof exports !== 'undefined') exports.Chess = Chess
  /* export Chess object for any RequireJS compatible environment */
  if (typeof define !== 'undefined')
    define(function() {
      return Chess
    })
  
  },{}],114:[function(require,module,exports){
  /*
  
  The MIT License (MIT)
  
  Original Library
    - Copyright (c) Marak Squires
  
  Additional functionality
   - Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
  
  */
  
  var colors = {};
  module['exports'] = colors;
  
  colors.themes = {};
  
  var util = require('util');
  var ansiStyles = colors.styles = require('./styles');
  var defineProps = Object.defineProperties;
  var newLineRegex = new RegExp(/[\r\n]+/g);
  
  colors.supportsColor = require('./system/supports-colors').supportsColor;
  
  if (typeof colors.enabled === 'undefined') {
    colors.enabled = colors.supportsColor() !== false;
  }
  
  colors.enable = function() {
    colors.enabled = true;
  };
  
  colors.disable = function() {
    colors.enabled = false;
  };
  
  colors.stripColors = colors.strip = function(str) {
    return ('' + str).replace(/\x1B\[\d+m/g, '');
  };
  
  // eslint-disable-next-line no-unused-vars
  var stylize = colors.stylize = function stylize(str, style) {
    if (!colors.enabled) {
      return str+'';
    }
  
    var styleMap = ansiStyles[style];
  
    // Stylize should work for non-ANSI styles, too
    if(!styleMap && style in colors){
      // Style maps like trap operate as functions on strings;
      // they don't have properties like open or close.
      return colors[style](str);
    }
  
    return styleMap.open + str + styleMap.close;
  };
  
  var matchOperatorsRe = /[|\\{}()[\]^$+*?.]/g;
  var escapeStringRegexp = function(str) {
    if (typeof str !== 'string') {
      throw new TypeError('Expected a string');
    }
    return str.replace(matchOperatorsRe, '\\$&');
  };
  
  function build(_styles) {
    var builder = function builder() {
      return applyStyle.apply(builder, arguments);
    };
    builder._styles = _styles;
    // __proto__ is used because we must return a function, but there is
    // no way to create a function with a different prototype.
    builder.__proto__ = proto;
    return builder;
  }
  
  var styles = (function() {
    var ret = {};
    ansiStyles.grey = ansiStyles.gray;
    Object.keys(ansiStyles).forEach(function(key) {
      ansiStyles[key].closeRe =
        new RegExp(escapeStringRegexp(ansiStyles[key].close), 'g');
      ret[key] = {
        get: function() {
          return build(this._styles.concat(key));
        },
      };
    });
    return ret;
  })();
  
  var proto = defineProps(function colors() {}, styles);
  
  function applyStyle() {
    var args = Array.prototype.slice.call(arguments);
  
    var str = args.map(function(arg) {
      // Use weak equality check so we can colorize null/undefined in safe mode
      if (arg != null && arg.constructor === String) {
        return arg;
      } else {
        return util.inspect(arg);
      }
    }).join(' ');
  
    if (!colors.enabled || !str) {
      return str;
    }
  
    var newLinesPresent = str.indexOf('\n') != -1;
  
    var nestedStyles = this._styles;
  
    var i = nestedStyles.length;
    while (i--) {
      var code = ansiStyles[nestedStyles[i]];
      str = code.open + str.replace(code.closeRe, code.open) + code.close;
      if (newLinesPresent) {
        str = str.replace(newLineRegex, function(match) {
          return code.close + match + code.open;
        });
      }
    }
  
    return str;
  }
  
  colors.setTheme = function(theme) {
    if (typeof theme === 'string') {
      console.log('colors.setTheme now only accepts an object, not a string.  ' +
        'If you are trying to set a theme from a file, it is now your (the ' +
        'caller\'s) responsibility to require the file.  The old syntax ' +
        'looked like colors.setTheme(__dirname + ' +
        '\'/../themes/generic-logging.js\'); The new syntax looks like '+
        'colors.setTheme(require(__dirname + ' +
        '\'/../themes/generic-logging.js\'));');
      return;
    }
    for (var style in theme) {
      (function(style) {
        colors[style] = function(str) {
          if (typeof theme[style] === 'object') {
            var out = str;
            for (var i in theme[style]) {
              out = colors[theme[style][i]](out);
            }
            return out;
          }
          return colors[theme[style]](str);
        };
      })(style);
    }
  };
  
  function init() {
    var ret = {};
    Object.keys(styles).forEach(function(name) {
      ret[name] = {
        get: function() {
          return build([name]);
        },
      };
    });
    return ret;
  }
  
  var sequencer = function sequencer(map, str) {
    var exploded = str.split('');
    exploded = exploded.map(map);
    return exploded.join('');
  };
  
  // custom formatter methods
  colors.trap = require('./custom/trap');
  colors.zalgo = require('./custom/zalgo');
  
  // maps
  colors.maps = {};
  colors.maps.america = require('./maps/america')(colors);
  colors.maps.zebra = require('./maps/zebra')(colors);
  colors.maps.rainbow = require('./maps/rainbow')(colors);
  colors.maps.random = require('./maps/random')(colors);
  
  for (var map in colors.maps) {
    (function(map) {
      colors[map] = function(str) {
        return sequencer(colors.maps[map], str);
      };
    })(map);
  }
  
  defineProps(colors, init());
  
  },{"./custom/trap":115,"./custom/zalgo":116,"./maps/america":119,"./maps/rainbow":120,"./maps/random":121,"./maps/zebra":122,"./styles":123,"./system/supports-colors":125,"util":80}],115:[function(require,module,exports){
  module['exports'] = function runTheTrap(text, options) {
    var result = '';
    text = text || 'Run the trap, drop the bass';
    text = text.split('');
    var trap = {
      a: ['\u0040', '\u0104', '\u023a', '\u0245', '\u0394', '\u039b', '\u0414'],
      b: ['\u00df', '\u0181', '\u0243', '\u026e', '\u03b2', '\u0e3f'],
      c: ['\u00a9', '\u023b', '\u03fe'],
      d: ['\u00d0', '\u018a', '\u0500', '\u0501', '\u0502', '\u0503'],
      e: ['\u00cb', '\u0115', '\u018e', '\u0258', '\u03a3', '\u03be', '\u04bc',
        '\u0a6c'],
      f: ['\u04fa'],
      g: ['\u0262'],
      h: ['\u0126', '\u0195', '\u04a2', '\u04ba', '\u04c7', '\u050a'],
      i: ['\u0f0f'],
      j: ['\u0134'],
      k: ['\u0138', '\u04a0', '\u04c3', '\u051e'],
      l: ['\u0139'],
      m: ['\u028d', '\u04cd', '\u04ce', '\u0520', '\u0521', '\u0d69'],
      n: ['\u00d1', '\u014b', '\u019d', '\u0376', '\u03a0', '\u048a'],
      o: ['\u00d8', '\u00f5', '\u00f8', '\u01fe', '\u0298', '\u047a', '\u05dd',
        '\u06dd', '\u0e4f'],
      p: ['\u01f7', '\u048e'],
      q: ['\u09cd'],
      r: ['\u00ae', '\u01a6', '\u0210', '\u024c', '\u0280', '\u042f'],
      s: ['\u00a7', '\u03de', '\u03df', '\u03e8'],
      t: ['\u0141', '\u0166', '\u0373'],
      u: ['\u01b1', '\u054d'],
      v: ['\u05d8'],
      w: ['\u0428', '\u0460', '\u047c', '\u0d70'],
      x: ['\u04b2', '\u04fe', '\u04fc', '\u04fd'],
      y: ['\u00a5', '\u04b0', '\u04cb'],
      z: ['\u01b5', '\u0240'],
    };
    text.forEach(function(c) {
      c = c.toLowerCase();
      var chars = trap[c] || [' '];
      var rand = Math.floor(Math.random() * chars.length);
      if (typeof trap[c] !== 'undefined') {
        result += trap[c][rand];
      } else {
        result += c;
      }
    });
    return result;
  };
  
  },{}],116:[function(require,module,exports){
  // please no
  module['exports'] = function zalgo(text, options) {
    text = text || '   he is here   ';
    var soul = {
      'up': [
        '̍', '̎', '̄', '̅',
        '̿', '̑', '̆', '̐',
        '͒', '͗', '͑', '̇',
        '̈', '̊', '͂', '̓',
        '̈', '͊', '͋', '͌',
        '̃', '̂', '̌', '͐',
        '̀', '́', '̋', '̏',
        '̒', '̓', '̔', '̽',
        '̉', 'ͣ', 'ͤ', 'ͥ',
        'ͦ', 'ͧ', 'ͨ', 'ͩ',
        'ͪ', 'ͫ', 'ͬ', 'ͭ',
        'ͮ', 'ͯ', '̾', '͛',
        '͆', '̚',
      ],
      'down': [
        '̖', '̗', '̘', '̙',
        '̜', '̝', '̞', '̟',
        '̠', '̤', '̥', '̦',
        '̩', '̪', '̫', '̬',
        '̭', '̮', '̯', '̰',
        '̱', '̲', '̳', '̹',
        '̺', '̻', '̼', 'ͅ',
        '͇', '͈', '͉', '͍',
        '͎', '͓', '͔', '͕',
        '͖', '͙', '͚', '̣',
      ],
      'mid': [
        '̕', '̛', '̀', '́',
        '͘', '̡', '̢', '̧',
        '̨', '̴', '̵', '̶',
        '͜', '͝', '͞',
        '͟', '͠', '͢', '̸',
        '̷', '͡', ' ҉',
      ],
    };
    var all = [].concat(soul.up, soul.down, soul.mid);
  
    function randomNumber(range) {
      var r = Math.floor(Math.random() * range);
      return r;
    }
  
    function isChar(character) {
      var bool = false;
      all.filter(function(i) {
        bool = (i === character);
      });
      return bool;
    }
  
  
    function heComes(text, options) {
      var result = '';
      var counts;
      var l;
      options = options || {};
      options['up'] =
        typeof options['up'] !== 'undefined' ? options['up'] : true;
      options['mid'] =
        typeof options['mid'] !== 'undefined' ? options['mid'] : true;
      options['down'] =
        typeof options['down'] !== 'undefined' ? options['down'] : true;
      options['size'] =
        typeof options['size'] !== 'undefined' ? options['size'] : 'maxi';
      text = text.split('');
      for (l in text) {
        if (isChar(l)) {
          continue;
        }
        result = result + text[l];
        counts = {'up': 0, 'down': 0, 'mid': 0};
        switch (options.size) {
          case 'mini':
            counts.up = randomNumber(8);
            counts.mid = randomNumber(2);
            counts.down = randomNumber(8);
            break;
          case 'maxi':
            counts.up = randomNumber(16) + 3;
            counts.mid = randomNumber(4) + 1;
            counts.down = randomNumber(64) + 3;
            break;
          default:
            counts.up = randomNumber(8) + 1;
            counts.mid = randomNumber(6) / 2;
            counts.down = randomNumber(8) + 1;
            break;
        }
  
        var arr = ['up', 'mid', 'down'];
        for (var d in arr) {
          var index = arr[d];
          for (var i = 0; i <= counts[index]; i++) {
            if (options[index]) {
              result = result + soul[index][randomNumber(soul[index].length)];
            }
          }
        }
      }
      return result;
    }
    // don't summon him
    return heComes(text, options);
  };
  
  
  },{}],117:[function(require,module,exports){
  var colors = require('./colors');
  
  module['exports'] = function() {
    //
    // Extends prototype of native string object to allow for "foo".red syntax
    //
    var addProperty = function(color, func) {
      String.prototype.__defineGetter__(color, func);
    };
  
    addProperty('strip', function() {
      return colors.strip(this);
    });
  
    addProperty('stripColors', function() {
      return colors.strip(this);
    });
  
    addProperty('trap', function() {
      return colors.trap(this);
    });
  
    addProperty('zalgo', function() {
      return colors.zalgo(this);
    });
  
    addProperty('zebra', function() {
      return colors.zebra(this);
    });
  
    addProperty('rainbow', function() {
      return colors.rainbow(this);
    });
  
    addProperty('random', function() {
      return colors.random(this);
    });
  
    addProperty('america', function() {
      return colors.america(this);
    });
  
    //
    // Iterate through all default styles and colors
    //
    var x = Object.keys(colors.styles);
    x.forEach(function(style) {
      addProperty(style, function() {
        return colors.stylize(this, style);
      });
    });
  
    function applyTheme(theme) {
      //
      // Remark: This is a list of methods that exist
      // on String that you should not overwrite.
      //
      var stringPrototypeBlacklist = [
        '__defineGetter__', '__defineSetter__', '__lookupGetter__',
        '__lookupSetter__', 'charAt', 'constructor', 'hasOwnProperty',
        'isPrototypeOf', 'propertyIsEnumerable', 'toLocaleString', 'toString',
        'valueOf', 'charCodeAt', 'indexOf', 'lastIndexOf', 'length',
        'localeCompare', 'match', 'repeat', 'replace', 'search', 'slice',
        'split', 'substring', 'toLocaleLowerCase', 'toLocaleUpperCase',
        'toLowerCase', 'toUpperCase', 'trim', 'trimLeft', 'trimRight',
      ];
  
      Object.keys(theme).forEach(function(prop) {
        if (stringPrototypeBlacklist.indexOf(prop) !== -1) {
          console.log('warn: '.red + ('String.prototype' + prop).magenta +
            ' is probably something you don\'t want to override.  ' +
            'Ignoring style name');
        } else {
          if (typeof(theme[prop]) === 'string') {
            colors[prop] = colors[theme[prop]];
            addProperty(prop, function() {
              return colors[prop](this);
            });
          } else {
            var themePropApplicator = function(str) {
              var ret = str || this;
              for (var t = 0; t < theme[prop].length; t++) {
                ret = colors[theme[prop][t]](ret);
              }
              return ret;
            };
            addProperty(prop, themePropApplicator);
            colors[prop] = function(str) {
              return themePropApplicator(str);
            };
          }
        }
      });
    }
  
    colors.setTheme = function(theme) {
      if (typeof theme === 'string') {
        console.log('colors.setTheme now only accepts an object, not a string. ' +
          'If you are trying to set a theme from a file, it is now your (the ' +
          'caller\'s) responsibility to require the file.  The old syntax ' +
          'looked like colors.setTheme(__dirname + ' +
          '\'/../themes/generic-logging.js\'); The new syntax looks like '+
          'colors.setTheme(require(__dirname + ' +
          '\'/../themes/generic-logging.js\'));');
        return;
      } else {
        applyTheme(theme);
      }
    };
  };
  
  },{"./colors":114}],118:[function(require,module,exports){
  var colors = require('./colors');
  module['exports'] = colors;
  
  // Remark: By default, colors will add style properties to String.prototype.
  //
  // If you don't wish to extend String.prototype, you can do this instead and
  // native String will not be touched:
  //
  //   var colors = require('colors/safe);
  //   colors.red("foo")
  //
  //
  require('./extendStringPrototype')();
  
  },{"./colors":114,"./extendStringPrototype":117}],119:[function(require,module,exports){
  module['exports'] = function(colors) {
    return function(letter, i, exploded) {
      if (letter === ' ') return letter;
      switch (i%3) {
        case 0: return colors.red(letter);
        case 1: return colors.white(letter);
        case 2: return colors.blue(letter);
      }
    };
  };
  
  },{}],120:[function(require,module,exports){
  module['exports'] = function(colors) {
    // RoY G BiV
    var rainbowColors = ['red', 'yellow', 'green', 'blue', 'magenta'];
    return function(letter, i, exploded) {
      if (letter === ' ') {
        return letter;
      } else {
        return colors[rainbowColors[i++ % rainbowColors.length]](letter);
      }
    };
  };
  
  
  },{}],121:[function(require,module,exports){
  module['exports'] = function(colors) {
    var available = ['underline', 'inverse', 'grey', 'yellow', 'red', 'green',
      'blue', 'white', 'cyan', 'magenta', 'brightYellow', 'brightRed',
      'brightGreen', 'brightBlue', 'brightWhite', 'brightCyan', 'brightMagenta'];
    return function(letter, i, exploded) {
      return letter === ' ' ? letter :
        colors[
            available[Math.round(Math.random() * (available.length - 2))]
        ](letter);
    };
  };
  
  },{}],122:[function(require,module,exports){
  module['exports'] = function(colors) {
    return function(letter, i, exploded) {
      return i % 2 === 0 ? letter : colors.inverse(letter);
    };
  };
  
  },{}],123:[function(require,module,exports){
  /*
  The MIT License (MIT)
  
  Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
  
  */
  
  var styles = {};
  module['exports'] = styles;
  
  var codes = {
    reset: [0, 0],
  
    bold: [1, 22],
    dim: [2, 22],
    italic: [3, 23],
    underline: [4, 24],
    inverse: [7, 27],
    hidden: [8, 28],
    strikethrough: [9, 29],
  
    black: [30, 39],
    red: [31, 39],
    green: [32, 39],
    yellow: [33, 39],
    blue: [34, 39],
    magenta: [35, 39],
    cyan: [36, 39],
    white: [37, 39],
    gray: [90, 39],
    grey: [90, 39],
  
    brightRed: [91, 39],
    brightGreen: [92, 39],
    brightYellow: [93, 39],
    brightBlue: [94, 39],
    brightMagenta: [95, 39],
    brightCyan: [96, 39],
    brightWhite: [97, 39],
  
    bgBlack: [40, 49],
    bgRed: [41, 49],
    bgGreen: [42, 49],
    bgYellow: [43, 49],
    bgBlue: [44, 49],
    bgMagenta: [45, 49],
    bgCyan: [46, 49],
    bgWhite: [47, 49],
    bgGray: [100, 49],
    bgGrey: [100, 49],
  
    bgBrightRed: [101, 49],
    bgBrightGreen: [102, 49],
    bgBrightYellow: [103, 49],
    bgBrightBlue: [104, 49],
    bgBrightMagenta: [105, 49],
    bgBrightCyan: [106, 49],
    bgBrightWhite: [107, 49],
  
    // legacy styles for colors pre v1.0.0
    blackBG: [40, 49],
    redBG: [41, 49],
    greenBG: [42, 49],
    yellowBG: [43, 49],
    blueBG: [44, 49],
    magentaBG: [45, 49],
    cyanBG: [46, 49],
    whiteBG: [47, 49],
  
  };
  
  Object.keys(codes).forEach(function(key) {
    var val = codes[key];
    var style = styles[key] = [];
    style.open = '\u001b[' + val[0] + 'm';
    style.close = '\u001b[' + val[1] + 'm';
  });
  
  },{}],124:[function(require,module,exports){
  (function (process){
  /*
  MIT License
  
  Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)
  
  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal in
  the Software without restriction, including without limitation the rights to
  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
  of the Software, and to permit persons to whom the Software is furnished to do
  so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
  */
  
  'use strict';
  
  module.exports = function(flag, argv) {
    argv = argv || process.argv;
  
    var terminatorPos = argv.indexOf('--');
    var prefix = /^-{1,2}/.test(flag) ? '' : '--';
    var pos = argv.indexOf(prefix + flag);
  
    return pos !== -1 && (terminatorPos === -1 ? true : pos < terminatorPos);
  };
  
  }).call(this,require('_process'))
  },{"_process":32}],125:[function(require,module,exports){
  (function (process){
  /*
  The MIT License (MIT)
  
  Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)
  
  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  THE SOFTWARE.
  
  */
  
  'use strict';
  
  var os = require('os');
  var hasFlag = require('./has-flag.js');
  
  var env = process.env;
  
  var forceColor = void 0;
  if (hasFlag('no-color') || hasFlag('no-colors') || hasFlag('color=false')) {
    forceColor = false;
  } else if (hasFlag('color') || hasFlag('colors') || hasFlag('color=true')
             || hasFlag('color=always')) {
    forceColor = true;
  }
  if ('FORCE_COLOR' in env) {
    forceColor = env.FORCE_COLOR.length === 0
      || parseInt(env.FORCE_COLOR, 10) !== 0;
  }
  
  function translateLevel(level) {
    if (level === 0) {
      return false;
    }
  
    return {
      level: level,
      hasBasic: true,
      has256: level >= 2,
      has16m: level >= 3,
    };
  }
  
  function supportsColor(stream) {
    if (forceColor === false) {
      return 0;
    }
  
    if (hasFlag('color=16m') || hasFlag('color=full')
        || hasFlag('color=truecolor')) {
      return 3;
    }
  
    if (hasFlag('color=256')) {
      return 2;
    }
  
    if (stream && !stream.isTTY && forceColor !== true) {
      return 0;
    }
  
    var min = forceColor ? 1 : 0;
  
    if (process.platform === 'win32') {
      // Node.js 7.5.0 is the first version of Node.js to include a patch to
      // libuv that enables 256 color output on Windows. Anything earlier and it
      // won't work. However, here we target Node.js 8 at minimum as it is an LTS
      // release, and Node.js 7 is not. Windows 10 build 10586 is the first
      // Windows release that supports 256 colors. Windows 10 build 14931 is the
      // first release that supports 16m/TrueColor.
      var osRelease = os.release().split('.');
      if (Number(process.versions.node.split('.')[0]) >= 8
          && Number(osRelease[0]) >= 10 && Number(osRelease[2]) >= 10586) {
        return Number(osRelease[2]) >= 14931 ? 3 : 2;
      }
  
      return 1;
    }
  
    if ('CI' in env) {
      if (['TRAVIS', 'CIRCLECI', 'APPVEYOR', 'GITLAB_CI'].some(function(sign) {
        return sign in env;
      }) || env.CI_NAME === 'codeship') {
        return 1;
      }
  
      return min;
    }
  
    if ('TEAMCITY_VERSION' in env) {
      return (/^(9\.(0*[1-9]\d*)\.|\d{2,}\.)/.test(env.TEAMCITY_VERSION) ? 1 : 0
      );
    }
  
    if ('TERM_PROGRAM' in env) {
      var version = parseInt((env.TERM_PROGRAM_VERSION || '').split('.')[0], 10);
  
      switch (env.TERM_PROGRAM) {
        case 'iTerm.app':
          return version >= 3 ? 3 : 2;
        case 'Hyper':
          return 3;
        case 'Apple_Terminal':
          return 2;
        // No default
      }
    }
  
    if (/-256(color)?$/i.test(env.TERM)) {
      return 2;
    }
  
    if (/^screen|^xterm|^vt100|^rxvt|color|ansi|cygwin|linux/i.test(env.TERM)) {
      return 1;
    }
  
    if ('COLORTERM' in env) {
      return 1;
    }
  
    if (env.TERM === 'dumb') {
      return min;
    }
  
    return min;
  }
  
  function getSupportLevel(stream) {
    var level = supportsColor(stream);
    return translateLevel(level);
  }
  
  module.exports = {
    supportsColor: getSupportLevel,
    stdout: getSupportLevel(process.stdout),
    stderr: getSupportLevel(process.stderr),
  };
  
  }).call(this,require('_process'))
  },{"./has-flag.js":124,"_process":32,"os":19}],126:[function(require,module,exports){
  (function (process){
  /**
   * This is the web browser implementation of `debug()`.
   *
   * Expose `debug()` as the module.
   */
  
  exports = module.exports = require('./debug');
  exports.speechSynthesis = speechSynthesis;
  exports.log = log;
  exports.formatArgs = formatArgs;
  exports.save = save;
  exports.load = load;
  exports.useColors = useColors;
  exports.storage = 'undefined' != typeof chrome
                 && 'undefined' != typeof chrome.storage
                    ? chrome.storage.local
                    : localstorage();
  
  /**
   * Colors.
   */
  
  exports.colors = [
    '#0000CC', '#0000FF', '#0033CC', '#0033FF', '#0066CC', '#0066FF', '#0099CC',
    '#0099FF', '#00CC00', '#00CC33', '#00CC66', '#00CC99', '#00CCCC', '#00CCFF',
    '#3300CC', '#3300FF', '#3333CC', '#3333FF', '#3366CC', '#3366FF', '#3399CC',
    '#3399FF', '#33CC00', '#33CC33', '#33CC66', '#33CC99', '#33CCCC', '#33CCFF',
    '#6600CC', '#6600FF', '#6633CC', '#6633FF', '#66CC00', '#66CC33', '#9900CC',
    '#9900FF', '#9933CC', '#9933FF', '#99CC00', '#99CC33', '#CC0000', '#CC0033',
    '#CC0066', '#CC0099', '#CC00CC', '#CC00FF', '#CC3300', '#CC3333', '#CC3366',
    '#CC3399', '#CC33CC', '#CC33FF', '#CC6600', '#CC6633', '#CC9900', '#CC9933',
    '#CCCC00', '#CCCC33', '#FF0000', '#FF0033', '#FF0066', '#FF0099', '#FF00CC',
    '#FF00FF', '#FF3300', '#FF3333', '#FF3366', '#FF3399', '#FF33CC', '#FF33FF',
    '#FF6600', '#FF6633', '#FF9900', '#FF9933', '#FFCC00', '#FFCC33'
  ];
  
  /**
   * Currently only WebKit-based Web Inspectors, Firefox >= v31,
   * and the Firebug extension (any Firefox version) are known
   * to support "%c" CSS customizations.
   *
   * TODO: add a `localStorage` variable to explicitly enable/disable colors
   */
  
  function useColors() {
    // NB: In an Electron preload script, document will be defined but not fully
    // initialized. Since we know we're in Chrome, we'll just detect this case
    // explicitly
    if (typeof window !== 'undefined' && window.process && window.process.type === 'renderer') {
      return true;
    }
  
    // Internet Explorer and Edge do not support colors.
    if (typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/(edge|trident)\/(\d+)/)) {
      return false;
    }
  
    // is webkit? http://stackoverflow.com/a/16459606/376773
    // document is undefined in react-native: https://github.com/facebook/react-native/pull/1632
    return (typeof document !== 'undefined' && document.documentElement && document.documentElement.style && document.documentElement.style.WebkitAppearance) ||
      // is firebug? http://stackoverflow.com/a/398120/376773
      (typeof window !== 'undefined' && window.console && (window.console.firebug || (window.console.exception && window.console.table))) ||
      // is firefox >= v31?
      // https://developer.mozilla.org/en-US/docs/Tools/Web_Console#Styling_messages
      (typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/firefox\/(\d+)/) && parseInt(RegExp.$1, 10) >= 31) ||
      // double check webkit in userAgent just in case we are in a worker
      (typeof navigator !== 'undefined' && navigator.userAgent && navigator.userAgent.toLowerCase().match(/applewebkit\/(\d+)/));
  }
  
  /**
   * Map %j to `JSON.stringify()`, since no Web Inspectors do that by default.
   */
  
  exports.formatters.j = function(v) {
    try {
      return JSON.stringify(v);
    } catch (err) {
      return '[UnexpectedJSONParseError]: ' + err.message;
    }
  };
  
  
  /**
   * Colorize log arguments if enabled.
   *
   * @api public
   */
  
  function formatArgs(args) {
    var useColors = this.useColors;
  
    args[0] = (useColors ? '%c' : '')
      + this.namespace
      + (useColors ? ' %c' : ' ')
      + args[0]
      + (useColors ? '%c ' : ' ')
      + '+' + exports.humanize(this.diff);
  
    if (!useColors) return;
  
    var c = 'color: ' + this.color;
    args.splice(1, 0, c, 'color: inherit')
  
    // the final "%c" is somewhat tricky, because there could be other
    // arguments passed either before or after the %c, so we need to
    // figure out the correct index to insert the CSS into
    var index = 0;
    var lastC = 0;
    args[0].replace(/%[a-zA-Z%]/g, function(match) {
      if ('%%' === match) return;
      index++;
      if ('%c' === match) {
        // we only are interested in the *last* %c
        // (the user may have provided their own)
        lastC = index;
      }
    });
  
    args.splice(lastC, 0, c);
  }
  
  /**
   * Invokes `console.log()` when available.
   * No-op when `console.log` is not a "function".
   *
   * @api public
   */
  
  function log() {
    // this hackery is required for IE8/9, where
    // the `console.log` function doesn't have 'apply'
    return 'object' === typeof console
      && console.log
      && Function.prototype.apply.call(console.log, console, arguments);
  }
  
  /**
   * Save `namespaces`.
   *
   * @param {String} namespaces
   * @api private
   */
  
  function save(namespaces) {
    try {
      if (null == namespaces) {
        exports.storage.removeItem('debug');
      } else {
        exports.storage.debug = namespaces;
      }
    } catch(e) {}
  }
  
  /**
   * Load `namespaces`.
   *
   * @return {String} returns the previously persisted debug modes
   * @api private
   */
  
  function load() {
    var r;
    try {
      r = exports.storage.debug;
    } catch(e) {}
  
    // If debug isn't set in LS, and we're in Electron, try to load $DEBUG
    if (!r && typeof process !== 'undefined' && 'env' in process) {
      r = process.env.DEBUG;
    }
  
    return r;
  }
  
  /**
   * Enable namespaces listed in `localStorage.debug` initially.
   */
  
  exports.enable(load());
  
  /**
   * Localstorage attempts to return the localstorage.
   *
   * This is necessary because safari throws
   * when a user disables cookies/localstorage
   * and you attempt to access it.
   *
   * @return {LocalStorage}
   * @api private
   */
  
  function localstorage() {
    try {
      return window.localStorage;
    } catch (e) {}
  }
  
  }).call(this,require('_process'))
  },{"./debug":127,"_process":32}],127:[function(require,module,exports){
  
  /**
   * This is the common logic for both the Node.js and web browser
   * implementations of `debug()`.
   *
   * Expose `debug()` as the module.
   */
  
  exports = module.exports = createDebug.debug = createDebug['default'] = createDebug;
  exports.coerce = coerce;
  exports.disable = disable;
  exports.enable = enable;
  exports.enabled = enabled;
  exports.humanize = require('ms');
  
  /**
   * Active `debug` instances.
   */
  exports.instances = [];
  
  /**
   * The currently active debug mode names, and names to skip.
   */
  
  exports.names = [];
  exports.skips = [];
  
  /**
   * Map of special "%n" handling functions, for the debug "format" argument.
   *
   * Valid key names are a single, lower or upper-case letter, i.e. "n" and "N".
   */
  
  exports.formatters = {};
  
  /**
   * Select a color.
   * @param {String} namespace
   * @return {Number}
   * @api private
   */
  
  function selectColor(namespace) {
    var hash = 0, i;
  
    for (i in namespace) {
      hash  = ((hash << 5) - hash) + namespace.charCodeAt(i);
      hash |= 0; // Convert to 32bit integer
    }
  
    return exports.colors[Math.abs(hash) % exports.colors.length];
  }
  
  /**
   * Create a debugger with the given `namespace`.
   *
   * @param {String} namespace
   * @return {Function}
   * @api public
   */
  
  function createDebug(namespace) {
  
    var prevTime;
  
    function debug() {
      // disabled?
      if (!debug.enabled) return;
  
      var self = debug;
  
      // set `diff` timestamp
      var curr = +new Date();
      var ms = curr - (prevTime || curr);
      self.diff = ms;
      self.prev = prevTime;
      self.curr = curr;
      prevTime = curr;
  
      // turn the `arguments` into a proper Array
      var args = new Array(arguments.length);
      for (var i = 0; i < args.length; i++) {
        args[i] = arguments[i];
      }
  
      args[0] = exports.coerce(args[0]);
  
      if ('string' !== typeof args[0]) {
        // anything else let's inspect with %O
        args.unshift('%O');
      }
  
      // apply any `formatters` transformations
      var index = 0;
      args[0] = args[0].replace(/%([a-zA-Z%])/g, function(match, format) {
        // if we encounter an escaped % then don't increase the array index
        if (match === '%%') return match;
        index++;
        var formatter = exports.formatters[format];
        if ('function' === typeof formatter) {
          var val = args[index];
          match = formatter.call(self, val);
  
          // now we need to remove `args[index]` since it's inlined in the `format`
          args.splice(index, 1);
          index--;
        }
        return match;
      });
  
      // apply env-specific formatting (colors, etc.)
      exports.formatArgs.call(self, args);
  
      var logFn = debug.log || exports.log || console.log.bind(console);
      logFn.apply(self, args);
    }
  
    debug.namespace = namespace;
    debug.enabled = exports.enabled(namespace);
    debug.useColors = exports.useColors();
    debug.color = selectColor(namespace);
    debug.destroy = destroy;
  
    // env-specific initialization logic for debug instances
    if ('function' === typeof exports.init) {
      exports.init(debug);
    }
  
    exports.instances.push(debug);
  
    return debug;
  }
  
  function destroy () {
    var index = exports.instances.indexOf(this);
    if (index !== -1) {
      exports.instances.splice(index, 1);
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Enables a debug mode by namespaces. This can include modes
   * separated by a colon and wildcards.
   *
   * @param {String} namespaces
   * @api public
   */
  
  function enable(namespaces) {
    exports.save(namespaces);
  
    exports.names = [];
    exports.skips = [];
  
    var i;
    var split = (typeof namespaces === 'string' ? namespaces : '').split(/[\s,]+/);
    var len = split.length;
  
    for (i = 0; i < len; i++) {
      if (!split[i]) continue; // ignore empty strings
      namespaces = split[i].replace(/\*/g, '.*?');
      if (namespaces[0] === '-') {
        exports.skips.push(new RegExp('^' + namespaces.substr(1) + '$'));
      } else {
        exports.names.push(new RegExp('^' + namespaces + '$'));
      }
    }
  
    for (i = 0; i < exports.instances.length; i++) {
      var instance = exports.instances[i];
      instance.enabled = exports.enabled(instance.namespace);
    }
  }
  
  /**
   * Disable debug output.
   *
   * @api public
   */
  
  function disable() {
    exports.enable('');
  }
  
  /**
   * Returns true if the given mode name is enabled, false otherwise.
   *
   * @param {String} name
   * @return {Boolean}
   * @api public
   */
  
  function enabled(name) {
    if (name[name.length - 1] === '*') {
      return true;
    }
    var i, len;
    for (i = 0, len = exports.skips.length; i < len; i++) {
      if (exports.skips[i].test(name)) {
        return false;
      }
    }
    for (i = 0, len = exports.names.length; i < len; i++) {
      if (exports.names[i].test(name)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Coerce `val`.
   *
   * @param {Mixed} val
   * @return {Mixed}
   * @api private
   */
  
  function coerce(val) {
    if (val instanceof Error) return val.stack || val.message;
    return val;
  }
  
  },{"ms":130}],128:[function(require,module,exports){
  /*
      FIGlet.js (a FIGDriver for FIGlet fonts)
      By Patrick Gillespie (patorjk@gmail.com)
      Originally Written For: http://patorjk.com/software/taag/
      License: MIT (with this header staying intact)
  
      This JavaScript code aims to fully implement the FIGlet spec.
      Full FIGlet spec: http://patorjk.com/software/taag/docs/figfont.txt
  
      FIGlet fonts are actually kind of complex, which is why you will see
      a lot of code about parsing and interpreting rules. The actual generation
      code is pretty simple and is done near the bottom of the code.
  */
  
  "use strict";
  
  var figlet = figlet || (function() {
  
      // ---------------------------------------------------------------------
      // Private static variables
  
      var FULL_WIDTH = 0,
          FITTING = 1,
          SMUSHING = 2,
          CONTROLLED_SMUSHING = 3;
  
      // ---------------------------------------------------------------------
      // Variable that will hold information about the fonts
  
      var figFonts = {}; // What stores all of the FIGlet font data
      var figDefaults = {
          font: 'Standard',
          fontPath: './fonts'
      };
  
      // ---------------------------------------------------------------------
      // Private static methods
  
      /*
          This method takes in the oldLayout and newLayout data from the FIGfont header file and returns
          the layout information.
      */
      function getSmushingRules(oldLayout, newLayout) {
          var rules = {};
          var val, index, len, code;
          var codes = [[16384,"vLayout",SMUSHING], [8192,"vLayout",FITTING], [4096, "vRule5", true], [2048, "vRule4", true],
                       [1024, "vRule3", true], [512, "vRule2", true], [256, "vRule1", true], [128, "hLayout", SMUSHING],
                       [64, "hLayout", FITTING], [32, "hRule6", true], [16, "hRule5", true], [8, "hRule4", true], [4, "hRule3", true],
                       [2, "hRule2", true], [1, "hRule1", true]];
  
          val = (newLayout !== null) ? newLayout : oldLayout;
          index = 0;
          len = codes.length;
          while ( index < len ) {
              code = codes[index];
              if (val >= code[0]) {
                  val = val - code[0];
                  rules[code[1]] = (typeof rules[code[1]] === "undefined") ? code[2] : rules[code[1]];
              } else if (code[1] !== "vLayout" && code[1] !== "hLayout") {
                  rules[code[1]] = false;
              }
              index++;
          }
  
          if (typeof rules["hLayout"] === "undefined") {
              if (oldLayout === 0) {
                  rules["hLayout"] = FITTING;
              } else if (oldLayout === -1) {
                  rules["hLayout"] = FULL_WIDTH;
              } else {
                  if (rules["hRule1"] || rules["hRule2"] || rules["hRule3"] || rules["hRule4"] ||rules["hRule5"] || rules["hRule6"] ) {
                      rules["hLayout"] = CONTROLLED_SMUSHING;
                  } else {
                      rules["hLayout"] = SMUSHING;
                  }
              }
          } else if (rules["hLayout"] === SMUSHING) {
              if (rules["hRule1"] || rules["hRule2"] || rules["hRule3"] || rules["hRule4"] ||rules["hRule5"] || rules["hRule6"] ) {
                  rules["hLayout"] = CONTROLLED_SMUSHING;
              }
          }
  
          if (typeof rules["vLayout"] === "undefined") {
              if (rules["vRule1"] || rules["vRule2"] || rules["vRule3"] || rules["vRule4"] ||rules["vRule5"]  ) {
                  rules["vLayout"] = CONTROLLED_SMUSHING;
              } else {
                  rules["vLayout"] = FULL_WIDTH;
              }
          } else if (rules["vLayout"] === SMUSHING) {
              if (rules["vRule1"] || rules["vRule2"] || rules["vRule3"] || rules["vRule4"] ||rules["vRule5"]  ) {
                  rules["vLayout"] = CONTROLLED_SMUSHING;
              }
          }
  
          return rules;
      }
  
      /* The [vh]Rule[1-6]_Smush functions return the smushed character OR false if the two characters can't be smushed */
  
      /*
          Rule 1: EQUAL CHARACTER SMUSHING (code value 1)
  
              Two sub-characters are smushed into a single sub-character
              if they are the same.  This rule does not smush
              hardblanks.  (See rule 6 on hardblanks below)
      */
      function hRule1_Smush(ch1, ch2, hardBlank) {
          if (ch1 === ch2 && ch1 !== hardBlank) {return ch1;}
          return false;
      }
  
      /*
          Rule 2: UNDERSCORE SMUSHING (code value 2)
  
              An underscore ("_") will be replaced by any of: "|", "/",
              "\", "[", "]", "{", "}", "(", ")", "<" or ">".
      */
      function hRule2_Smush(ch1, ch2) {
          var rule2Str = "|/\\[]{}()<>";
          if (ch1 === "_") {
              if (rule2Str.indexOf(ch2) !== -1) {return ch2;}
          } else if (ch2 === "_") {
              if (rule2Str.indexOf(ch1) !== -1) {return ch1;}
          }
          return false;
      }
  
      /*
          Rule 3: HIERARCHY SMUSHING (code value 4)
  
              A hierarchy of six classes is used: "|", "/\", "[]", "{}",
              "()", and "<>".  When two smushing sub-characters are
              from different classes, the one from the latter class
              will be used.
      */
      function hRule3_Smush(ch1, ch2) {
          var rule3Classes = "| /\\ [] {} () <>";
          var r3_pos1 = rule3Classes.indexOf(ch1);
          var r3_pos2 = rule3Classes.indexOf(ch2);
          if (r3_pos1 !== -1 && r3_pos2 !== -1) {
              if (r3_pos1 !== r3_pos2 && Math.abs(r3_pos1-r3_pos2) !== 1) {
                  return rule3Classes.substr(Math.max(r3_pos1,r3_pos2), 1);
              }
          }
          return false;
      }
  
      /*
          Rule 4: OPPOSITE PAIR SMUSHING (code value 8)
  
              Smushes opposing brackets ("[]" or "]["), braces ("{}" or
              "}{") and parentheses ("()" or ")(") together, replacing
              any such pair with a vertical bar ("|").
      */
      function hRule4_Smush(ch1, ch2) {
          var rule4Str = "[] {} ()";
          var r4_pos1 = rule4Str.indexOf(ch1);
          var r4_pos2 = rule4Str.indexOf(ch2);
          if (r4_pos1 !== -1 && r4_pos2 !== -1) {
              if (Math.abs(r4_pos1-r4_pos2) <= 1) {
                  return "|";
              }
          }
          return false;
      }
  
      /*
          Rule 5: BIG X SMUSHING (code value 16)
  
              Smushes "/\" into "|", "\/" into "Y", and "><" into "X".
              Note that "<>" is not smushed in any way by this rule.
              The name "BIG X" is historical; originally all three pairs
              were smushed into "X".
      */
      function hRule5_Smush(ch1, ch2) {
          var rule5Str = "/\\ \\/ ><";
          var rule5Hash = {"0": "|", "3": "Y", "6": "X"};
          var r5_pos1 = rule5Str.indexOf(ch1);
          var r5_pos2 = rule5Str.indexOf(ch2);
          if (r5_pos1 !== -1 && r5_pos2 !== -1) {
              if ((r5_pos2-r5_pos1) === 1) {
                  return rule5Hash[r5_pos1];
              }
          }
          return false;
      }
  
      /*
          Rule 6: HARDBLANK SMUSHING (code value 32)
  
              Smushes two hardblanks together, replacing them with a
              single hardblank.  (See "Hardblanks" below.)
      */
      function hRule6_Smush(ch1, ch2, hardBlank) {
          if (ch1 === hardBlank && ch2 === hardBlank) {
              return hardBlank;
          }
          return false;
      }
  
      /*
          Rule 1: EQUAL CHARACTER SMUSHING (code value 256)
  
              Same as horizontal smushing rule 1.
      */
      function vRule1_Smush(ch1, ch2) {
          if (ch1 === ch2) {return ch1;}
          return false;
      }
  
      /*
          Rule 2: UNDERSCORE SMUSHING (code value 512)
  
              Same as horizontal smushing rule 2.
      */
      function vRule2_Smush(ch1, ch2) {
          var rule2Str = "|/\\[]{}()<>";
          if (ch1 === "_") {
              if (rule2Str.indexOf(ch2) !== -1) {return ch2;}
          } else if (ch2 === "_") {
              if (rule2Str.indexOf(ch1) !== -1) {return ch1;}
          }
          return false;
      }
  
      /*
          Rule 3: HIERARCHY SMUSHING (code value 1024)
  
              Same as horizontal smushing rule 3.
      */
      function vRule3_Smush(ch1, ch2) {
          var rule3Classes = "| /\\ [] {} () <>";
          var r3_pos1 = rule3Classes.indexOf(ch1);
          var r3_pos2 = rule3Classes.indexOf(ch2);
          if (r3_pos1 !== -1 && r3_pos2 !== -1) {
              if (r3_pos1 !== r3_pos2 && Math.abs(r3_pos1-r3_pos2) !== 1) {
                  return rule3Classes.substr(Math.max(r3_pos1,r3_pos2), 1);
              }
          }
          return false;
      }
  
      /*
          Rule 4: HORIZONTAL LINE SMUSHING (code value 2048)
  
              Smushes stacked pairs of "-" and "_", replacing them with
              a single "=" sub-character.  It does not matter which is
              found above the other.  Note that vertical smushing rule 1
              will smush IDENTICAL pairs of horizontal lines, while this
              rule smushes horizontal lines consisting of DIFFERENT
              sub-characters.
      */
      function vRule4_Smush(ch1, ch2) {
          if ( (ch1 === "-" && ch2 === "_") || (ch1 === "_" && ch2 === "-") ) {
              return "=";
          }
          return false;
      }
  
      /*
          Rule 5: VERTICAL LINE SUPERSMUSHING (code value 4096)
  
              This one rule is different from all others, in that it
              "supersmushes" vertical lines consisting of several
              vertical bars ("|").  This creates the illusion that
              FIGcharacters have slid vertically against each other.
              Supersmushing continues until any sub-characters other
              than "|" would have to be smushed.  Supersmushing can
              produce impressive results, but it is seldom possible,
              since other sub-characters would usually have to be
              considered for smushing as soon as any such stacked
              vertical lines are encountered.
      */
      function vRule5_Smush(ch1, ch2) {
          if ( ch1 === "|" && ch2 === "|" ) {
              return "|";
          }
          return false;
      }
  
      /*
          Universal smushing simply overrides the sub-character from the
          earlier FIGcharacter with the sub-character from the later
          FIGcharacter.  This produces an "overlapping" effect with some
          FIGfonts, wherin the latter FIGcharacter may appear to be "in
          front".
      */
      function uni_Smush(ch1, ch2, hardBlank) {
          if (ch2 === " " || ch2 === "") {
              return ch1;
          } else if (ch2 === hardBlank && ch1 !== " ") {
              return ch1;
          } else {
              return ch2;
          }
      }
  
      // --------------------------------------------------------------------------
      // main vertical smush routines (excluding rules)
  
      /*
          txt1 - A line of text
          txt2 - A line of text
          opts - FIGlet options array
  
          About: Takes in two lines of text and returns one of the following:
          "valid" - These lines can be smushed together given the current smushing rules
          "end" - The lines can be smushed, but we're at a stopping point
          "invalid" - The two lines cannot be smushed together
      */
      function canVerticalSmush(txt1, txt2, opts) {
          if (opts.fittingRules.vLayout === FULL_WIDTH) {return "invalid";}
          var ii, len = Math.min(txt1.length, txt2.length);
          var ch1, ch2, endSmush = false, validSmush;
          if (len===0) {return "invalid";}
  
          for (ii = 0; ii < len; ii++) {
              ch1 = txt1.substr(ii,1);
              ch2 = txt2.substr(ii,1);
              if (ch1 !== " " && ch2 !== " ") {
                  if (opts.fittingRules.vLayout === FITTING) {
                      return "invalid";
                  } else if (opts.fittingRules.vLayout === SMUSHING) {
                      return "end";
                  } else {
                      if (vRule5_Smush(ch1,ch2)) {endSmush = endSmush || false; continue;} // rule 5 allow for "super" smushing, but only if we're not already ending this smush
                      validSmush = false;
                      validSmush = (opts.fittingRules.vRule1) ? vRule1_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule2) ? vRule2_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule3) ? vRule3_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule4) ? vRule4_Smush(ch1,ch2) : validSmush;
                      endSmush = true;
                      if (!validSmush) {return "invalid";}
                  }
              }
          }
          if (endSmush) {
              return "end";
          } else {
              return "valid";
          }
      }
  
      function getVerticalSmushDist(lines1, lines2, opts) {
          var maxDist = lines1.length;
          var len1 = lines1.length;
          var len2 = lines2.length;
          var subLines1, subLines2, slen;
          var curDist = 1;
          var ii, ret, result;
          while (curDist <= maxDist) {
  
              subLines1 = lines1.slice(Math.max(0,len1-curDist), len1);
              subLines2 = lines2.slice(0, Math.min(maxDist, curDist));
  
              slen = subLines2.length;//TODO:check this
              result = "";
              for (ii = 0; ii < slen; ii++) {
                  ret = canVerticalSmush(subLines1[ii], subLines2[ii], opts);
                  if (ret === "end") {
                      result = ret;
                  } else if (ret === "invalid") {
                      result = ret;
                      break;
                  } else {
                      if (result === "") {
                          result = "valid";
                      }
                  }
              }
  
              if (result === "invalid") {curDist--;break;}
              if (result === "end") {break;}
              if (result === "valid") {curDist++;}
          }
  
          return Math.min(maxDist,curDist);
      }
  
      function verticallySmushLines(line1, line2, opts) {
          var ii, len = Math.min(line1.length, line2.length);
          var ch1, ch2, result = "", validSmush;
  
          for (ii = 0; ii < len; ii++) {
              ch1 = line1.substr(ii,1);
              ch2 = line2.substr(ii,1);
              if (ch1 !== " " && ch2 !== " ") {
                  if (opts.fittingRules.vLayout === FITTING) {
                      result += uni_Smush(ch1,ch2);
                  } else if (opts.fittingRules.vLayout === SMUSHING) {
                      result += uni_Smush(ch1,ch2);
                  } else {
                      validSmush = (opts.fittingRules.vRule5) ? vRule5_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule1) ? vRule1_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule2) ? vRule2_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule3) ? vRule3_Smush(ch1,ch2) : validSmush;
                      validSmush = (!validSmush && opts.fittingRules.vRule4) ? vRule4_Smush(ch1,ch2) : validSmush;
                      result += validSmush;
                  }
              } else {
                  result += uni_Smush(ch1,ch2);
              }
          }
          return result;
      }
  
      function verticalSmush(lines1, lines2, overlap, opts) {
          var len1 = lines1.length;
          var len2 = lines2.length;
          var piece1 = lines1.slice(0, Math.max(0,len1-overlap));
          var piece2_1 = lines1.slice(Math.max(0,len1-overlap), len1);
          var piece2_2 = lines2.slice(0, Math.min(overlap, len2));
          var ii, len, line, piece2 = [], piece3, result = [];
  
          len = piece2_1.length;
          for (ii = 0; ii < len; ii++) {
              if (ii >= len2) {
                  line = piece2_1[ii];
              } else {
                  line = verticallySmushLines(piece2_1[ii], piece2_2[ii], opts);
              }
              piece2.push(line);
          }
  
          piece3 = lines2.slice(Math.min(overlap,len2), len2);
  
          return result.concat(piece1,piece2,piece3);
      }
  
      function padLines(lines, numSpaces) {
          var ii, len = lines.length, padding = "";
          for (ii = 0; ii < numSpaces; ii++) {
              padding += " ";
          }
          for (ii = 0; ii < len; ii++) {
              lines[ii] += padding;
          }
      }
  
      function smushVerticalFigLines(output, lines, opts) {
          var len1 = output[0].length;
          var len2 = lines[0].length;
          var overlap;
          if (len1 > len2) {
              padLines(lines, len1-len2);
          } else if (len2 > len1) {
              padLines(output, len2-len1);
          }
          overlap = getVerticalSmushDist(output, lines, opts);
          return verticalSmush(output, lines, overlap,opts);
      }
  
      // -------------------------------------------------------------------------
      // Main horizontal smush routines (excluding rules)
  
      function getHorizontalSmushLength(txt1, txt2, opts) {
          if (opts.fittingRules.hLayout === FULL_WIDTH) {return 0;}
          var ii, len1 = txt1.length, len2 = txt2.length;
          var maxDist = len1;
          var curDist = 1;
          var breakAfter = false;
          var validSmush = false;
          var seg1, seg2, ch1, ch2;
          if (len1 === 0) {return 0;}
  
          distCal: while (curDist <= maxDist) {
              seg1 = txt1.substr(len1-curDist,curDist);
              seg2 = txt2.substr(0,Math.min(curDist,len2));
              for (ii = 0; ii < Math.min(curDist,len2); ii++) {
                  ch1 = seg1.substr(ii,1);
                  ch2 = seg2.substr(ii,1);
                  if (ch1 !== " " && ch2 !== " " ) {
                      if (opts.fittingRules.hLayout === FITTING) {
                          curDist = curDist - 1;
                          break distCal;
                      } else if (opts.fittingRules.hLayout === SMUSHING) {
                          if (ch1 === opts.hardBlank || ch2 === opts.hardBlank) {
                              curDist = curDist - 1; // universal smushing does not smush hardblanks
                          }
                          break distCal;
                      } else {
                          breakAfter = true; // we know we need to break, but we need to check if our smushing rules will allow us to smush the overlapped characters
                          validSmush = false; // the below checks will let us know if we can smush these characters
  
                          validSmush = (opts.fittingRules.hRule1) ? hRule1_Smush(ch1,ch2,opts.hardBlank) : validSmush;
                          validSmush = (!validSmush && opts.fittingRules.hRule2) ? hRule2_Smush(ch1,ch2,opts.hardBlank) : validSmush;
                          validSmush = (!validSmush && opts.fittingRules.hRule3) ? hRule3_Smush(ch1,ch2,opts.hardBlank) : validSmush;
                          validSmush = (!validSmush && opts.fittingRules.hRule4) ? hRule4_Smush(ch1,ch2,opts.hardBlank) : validSmush;
                          validSmush = (!validSmush && opts.fittingRules.hRule5) ? hRule5_Smush(ch1,ch2,opts.hardBlank) : validSmush;
                          validSmush = (!validSmush && opts.fittingRules.hRule6) ? hRule6_Smush(ch1,ch2,opts.hardBlank) : validSmush;
  
                          if (!validSmush) {
                              curDist = curDist - 1;
                              break distCal;
                          }
                      }
                  }
              }
              if (breakAfter) {break;}
              curDist++;
          }
          return Math.min(maxDist,curDist);
      }
  
      function horizontalSmush(textBlock1, textBlock2, overlap, opts) {
          var ii, jj, ch, outputFig = [],
              overlapStart,piece1,piece2,piece3,len1,len2,txt1,txt2;
  
          for (ii = 0; ii < opts.height; ii++) {
              txt1 = textBlock1[ii];
              txt2 = textBlock2[ii];
              len1 = txt1.length;
              len2 = txt2.length;
              overlapStart = len1-overlap;
              piece1 = txt1.substr(0,Math.max(0,overlapStart));
              piece2 = "";
  
              // determine overlap piece
              var seg1 = txt1.substr(Math.max(0,len1-overlap),overlap);
              var seg2 = txt2.substr(0,Math.min(overlap,len2));
  
              for (jj = 0; jj < overlap; jj++) {
                  var ch1 = (jj < len1) ? seg1.substr(jj,1) : " ";
                  var ch2 = (jj < len2) ? seg2.substr(jj,1) : " ";
  
                  if (ch1 !== " " && ch2 !== " ") {
                      if (opts.fittingRules.hLayout === FITTING) {
                          piece2 += uni_Smush(ch1, ch2, opts.hardBlank);
                      } else if (opts.fittingRules.hLayout === SMUSHING) {
                          piece2 += uni_Smush(ch1, ch2, opts.hardBlank);
                      } else {
                          // Controlled Smushing
                          var nextCh = "";
                          nextCh = (!nextCh && opts.fittingRules.hRule1) ? hRule1_Smush(ch1,ch2,opts.hardBlank) : nextCh;
                          nextCh = (!nextCh && opts.fittingRules.hRule2) ? hRule2_Smush(ch1,ch2,opts.hardBlank) : nextCh;
                          nextCh = (!nextCh && opts.fittingRules.hRule3) ? hRule3_Smush(ch1,ch2,opts.hardBlank) : nextCh;
                          nextCh = (!nextCh && opts.fittingRules.hRule4) ? hRule4_Smush(ch1,ch2,opts.hardBlank) : nextCh;
                          nextCh = (!nextCh && opts.fittingRules.hRule5) ? hRule5_Smush(ch1,ch2,opts.hardBlank) : nextCh;
                          nextCh = (!nextCh && opts.fittingRules.hRule6) ? hRule6_Smush(ch1,ch2,opts.hardBlank) : nextCh;
                          nextCh = nextCh || uni_Smush(ch1, ch2, opts.hardBlank);
                          piece2 += nextCh;
                      }
                  } else {
                      piece2 += uni_Smush(ch1, ch2, opts.hardBlank);
                  }
              }
  
              if (overlap >= len2) {
                  piece3 = "";
              } else {
                  piece3 = txt2.substr(overlap,Math.max(0,len2-overlap));
              }
              outputFig[ii] = piece1 + piece2 + piece3;
          }
          return outputFig;
      }
  
      /*
          Creates new empty ASCII placeholder of give len
          - len - number
      */
      function newFigChar(len) {
          var outputFigText = [], row;
          for (row = 0; row < len; row++) {
              outputFigText[row] = "";
          }
          return outputFigText;
      }
  
      /*
         join words or single characaters into single Fig line
         - array - array of ASCII words or single characters: {fig: array, overlap: number}
         - len - height of the Characters (number of rows)
         - opt - options object
      */
      function joinFigArray(array, len, opts) {
          return array.reduce(function(acc, data) {
              return horizontalSmush(acc, data.fig, data.overlap, opts);
          }, newFigChar(len));
      }
  
      /*
         break long word return leftover characters and line before the break
         - figChars - list of single ASCII characters in form {fig, overlap}
         - len - number of rows
         - opt - options object
      */
      function breakWord(figChars, len, opts) {
          var result = {};
          for (var i = figChars.length; --i;) {
              var w = joinFigArray(figChars.slice(0, i), len, opts);
              if (figLinesWidth(w) <= opts.width) {
                  result.outputFigText = w;
                  if (i < figChars.length) {
                      result.chars = figChars.slice(i);
                  } else {
                      result.chars = [];
                  }
                  break;
              }
          }
          return result;
      }
  
      function generateFigTextLines(txt, figChars, opts) {
          var charIndex, figChar, spaceIndex, overlap = 0, row, outputFigText, len, height=opts.height, outputFigLines = [], maxWidth, prevWidth, nextFigChars, figWords = [], char, isSpace, textFigWord, textFigLine, tmpBreak;
  
          outputFigText = newFigChar(height);
          if (opts.width > 0 && opts.whitespaceBreak) {
              // list of characters is used to break in the middle of the word when word is logner
              // chars is array of characters with {fig, overlap} and overlap is for whole word
              nextFigChars = {
                  chars: [],
                  overlap: overlap
              };
          }
          if (opts.printDirection === 1) {
              txt = txt.split('').reverse().join('');
          }
          len = txt.length;
          for (charIndex = 0; charIndex < len; charIndex++) {
              char = txt.substr(charIndex, 1);
              isSpace = char.match(/\s/);
              figChar = figChars[char.charCodeAt(0)];
              textFigLine = null;
              if (figChar) {
                  if (opts.fittingRules.hLayout !== FULL_WIDTH) {
                      overlap = 10000;// a value too high to be the overlap
                      for (row = 0; row < opts.height; row++) {
                          overlap = Math.min(overlap, getHorizontalSmushLength(outputFigText[row], figChar[row], opts));
                      }
                      overlap = (overlap === 10000) ? 0 : overlap;
                  }
                  if (opts.width > 0) {
                      if (opts.whitespaceBreak) {
                          // next character in last word (figChars have same data as words)
                          textFigWord = joinFigArray(nextFigChars.chars.concat([{
                              fig: figChar, overlap: overlap
                          }]), height, opts);
                          textFigLine = joinFigArray(figWords.concat([{
                              fig: textFigWord, overlap: nextFigChars.overlap
                          }]), height, opts);
                          maxWidth = figLinesWidth(textFigLine);
                      } else {
                          textFigLine = horizontalSmush(outputFigText, figChar, overlap, opts);
                          maxWidth = figLinesWidth(textFigLine);
                      }
                      if (maxWidth >= opts.width && charIndex > 0) {
                          if (opts.whitespaceBreak) {
                              outputFigText = joinFigArray(figWords.slice(0, -1), height, opts);
                              if (figWords.length > 1) {
                                  outputFigLines.push(outputFigText);
                                  outputFigText = newFigChar(height);
                              }
                              figWords = [];
                          } else {
                              outputFigLines.push(outputFigText);
                              outputFigText = newFigChar(height);
                          }
                      }
                  }
                  if (opts.width > 0 && opts.whitespaceBreak) {
                      if (!isSpace || charIndex === len - 1) {
                          nextFigChars.chars.push({fig: figChar, overlap: overlap});
                      }
                      if (isSpace || charIndex === len - 1) {
                          // break long words
                          tmpBreak = null;
                          while (true) {
                              textFigLine = joinFigArray(nextFigChars.chars, height, opts);
                              maxWidth = figLinesWidth(textFigLine);
                              if (maxWidth >= opts.width) {
                                  tmpBreak = breakWord(nextFigChars.chars, height, opts);
                                  nextFigChars = { chars: tmpBreak.chars };
                                  outputFigLines.push(tmpBreak.outputFigText);
                              } else {
                                  break;
                              }
                          }
                          // any leftovers
                          if (maxWidth > 0) {
                              if (tmpBreak) {
                                  figWords.push({fig: textFigLine, overlap: 1});
                              } else {
                                  figWords.push({
                                      fig: textFigLine,
                                      overlap: nextFigChars.overlap
                                  });
                              }
                          }
                          // save space character and current overlap for smush in joinFigWords
                          if (isSpace) {
                              figWords.push({fig: figChar, overlap: overlap});
                              outputFigText = newFigChar(height);
                          }
                          if (charIndex === len - 1) {
                              // last line
                              outputFigText = joinFigArray(figWords, height, opts);
                          }
                          nextFigChars = {
                              chars: [],
                              overlap: overlap
                          };
                          continue;
                      }
                  }
                  outputFigText = horizontalSmush(outputFigText, figChar, overlap, opts);
              }
          }
          // special case when last line would be empty
          // this may happen if text fit exactly opt.width
          if (figLinesWidth(outputFigText) > 0) {
              outputFigLines.push(outputFigText);
          }
          // remove hardblanks
          if (opts.showHardBlanks !== true) {
              outputFigLines.forEach(function(outputFigText) {
                  len = outputFigText.length;
                  for (row = 0; row < len; row++) {
                      outputFigText[row] = outputFigText[row].replace(new RegExp("\\"+opts.hardBlank,"g")," ");
                  }
              });
          }
          return outputFigLines;
      }
  
      // -------------------------------------------------------------------------
      // Parsing and Generation methods
  
      var getHorizontalFittingRules = function(layout, options) {
          var props = ["hLayout", "hRule1","hRule2","hRule3","hRule4","hRule5","hRule6"],
              params = {}, prop, ii;
          if (layout === "default") {
              for (ii = 0; ii < props.length; ii++) {
                  params[props[ii]] = options.fittingRules[props[ii]];
              }
          } else if (layout === "full") {
              params = {"hLayout": FULL_WIDTH,"hRule1":false,"hRule2":false,"hRule3":false,"hRule4":false,"hRule5":false,"hRule6":false};
          } else if (layout === "fitted") {
              params = {"hLayout": FITTING,"hRule1":false,"hRule2":false,"hRule3":false,"hRule4":false,"hRule5":false,"hRule6":false};
          } else if (layout === "controlled smushing") {
              params = {"hLayout": CONTROLLED_SMUSHING,"hRule1":true,"hRule2":true,"hRule3":true,"hRule4":true,"hRule5":true,"hRule6":true};
          } else if (layout === "universal smushing") {
              params = {"hLayout": SMUSHING,"hRule1":false,"hRule2":false,"hRule3":false,"hRule4":false,"hRule5":false,"hRule6":false};
          } else {
              return;
          }
          return params;
      };
  
      var getVerticalFittingRules = function(layout, options) {
          var props = ["vLayout", "vRule1","vRule2","vRule3","vRule4","vRule5"],
              params = {}, prop, ii;
          if (layout === "default") {
              for (ii = 0; ii < props.length; ii++) {
                  params[props[ii]] = options.fittingRules[props[ii]];
              }
          } else if (layout === "full") {
              params = {"vLayout": FULL_WIDTH,"vRule1":false,"vRule2":false,"vRule3":false,"vRule4":false,"vRule5":false};
          } else if (layout === "fitted") {
              params = {"vLayout": FITTING,"vRule1":false,"vRule2":false,"vRule3":false,"vRule4":false,"vRule5":false};
          } else if (layout === "controlled smushing") {
              params = {"vLayout": CONTROLLED_SMUSHING,"vRule1":true,"vRule2":true,"vRule3":true,"vRule4":true,"vRule5":true};
          } else if (layout === "universal smushing") {
              params = {"vLayout": SMUSHING,"vRule1":false,"vRule2":false,"vRule3":false,"vRule4":false,"vRule5":false};
          } else {
              return;
          }
          return params;
      };
      /*
          Return max line of the ASCII Art
          - text is array of lines for text
          - char is next character
       */
      var figLinesWidth = function(textLines) {
          return Math.max.apply(Math, textLines.map(function(line, i) {
              return line.length;
          }));
      };
  
      /*
          Generates the ASCII Art
          - fontName: Font to use
          - option: Options to override the defaults
          - txt: The text to make into ASCII Art
      */
      var generateText = function(fontName, options, txt) {
          txt = txt.replace(/\r\n/g,"\n").replace(/\r/g,"\n");
          var lines = txt.split("\n");
          var figLines = [];
          var ii, len, output;
          len = lines.length;
          for (ii = 0; ii < len; ii++) {
              figLines = figLines.concat( generateFigTextLines(lines[ii], figFonts[fontName], options) );
          }
          len = figLines.length;
          output = figLines[0];
          for (ii = 1; ii < len; ii++) {
              output = smushVerticalFigLines(output, figLines[ii], options);
          }
  
          return output ? output.join("\n") : '';
      };
  
      // -------------------------------------------------------------------------
      // Public methods
  
      /*
          A short-cut for the figlet.text method
  
          Parameters:
          - txt (string): The text to make into ASCII Art
          - options (object/string - optional): Options that will override the current font's default options.
            If a string is provided instead of an object, it is assumed to be the font name.
  
              * font
              * horizontalLayout
              * verticalLayout
              * showHardBlanks - Wont remove hardblank characters
  
          - next (function): A callback function, it will contained the outputted ASCII Art.
      */
      var me = function(txt, options, next) {
          me.text(txt, options, next);
      };
      me.text = function(txt, options, next) {
          var fontName = '';
  
          // Validate inputs
          txt = txt + '';
  
          if (typeof arguments[1] === 'function') {
              next = options;
              options = {};
              options.font = figDefaults.font; // default font
          }
  
          if (typeof options === 'string') {
              fontName = options;
              options = {};
          } else {
              options = options || {};
              fontName = options.font || figDefaults.font;
          }
  
          /*
              Load the font. If it loads, it's data will be contained in the figFonts object.
              The callback will recieve a fontsOpts object, which contains the default
              options of the font (its fitting rules, etc etc).
          */
          me.loadFont(fontName, function(err, fontOpts) {
              if (err) {
                  return next(err);
              }
  
              next(null, generateText(fontName, _reworkFontOpts(fontOpts, options), txt));
          });
      };
  
      /*
          Synchronous version of figlet.text.
          Accepts the same parameters.
       */
      me.textSync = function(txt, options) {
          var fontName = '';
  
          // Validate inputs
          txt = txt + '';
  
          if (typeof options === 'string') {
              fontName = options;
              options = {};
          } else {
              options = options || {};
              fontName = options.font || figDefaults.font;
          }
  
          var fontOpts = _reworkFontOpts(me.loadFontSync(fontName), options);
          return generateText(fontName, fontOpts, txt);
      };
  
      /*
        takes assigned options and merges them with the default options from the choosen font
       */
      function _reworkFontOpts(fontOpts, options) {
          var myOpts = JSON.parse(JSON.stringify(fontOpts)), // make a copy because we may edit this (see below)
              params,
              prop;
  
          /*
           If the user is chosing to use a specific type of layout (e.g., 'full', 'fitted', etc etc)
           Then we need to override the default font options.
           */
          if (typeof options.horizontalLayout !== 'undefined') {
              params = getHorizontalFittingRules(options.horizontalLayout, fontOpts);
              for (prop in params) {
                  myOpts.fittingRules[prop] = params[prop];
              }
          }
          if (typeof options.verticalLayout !== 'undefined') {
              params = getVerticalFittingRules(options.verticalLayout, fontOpts);
              for (prop in params) {
                  myOpts.fittingRules[prop] = params[prop];
              }
          }
          myOpts.printDirection = (typeof options.printDirection !== 'undefined') ? options.printDirection : fontOpts.printDirection;
          myOpts.showHardBlanks = options.showHardBlanks || false;
          myOpts.width = options.width || -1;
          myOpts.whitespaceBreak = options.whitespaceBreak || false;
  
          return myOpts;
      }
  
      /*
          Returns metadata about a specfic FIGlet font.
  
          Returns:
              next(err, options, headerComment)
              - err: The error if an error occurred, otherwise null/falsey.
              - options (object): The options defined for the font.
              - headerComment (string): The font's header comment.
      */
      me.metadata = function(fontName, next) {
          fontName = fontName + '';
  
          /*
              Load the font. If it loads, it's data will be contained in the figFonts object.
              The callback will recieve a fontsOpts object, which contains the default
              options of the font (its fitting rules, etc etc).
          */
          me.loadFont(fontName, function(err, fontOpts) {
              if (err) {
                  next(err);
                  return;
              }
  
              next(null, fontOpts, figFonts[fontName].comment);
          });
      };
  
      /*
          Allows you to override defaults. See the definition of the figDefaults object up above
          to see what properties can be overridden.
          Returns the options for the font.
      */
      me.defaults = function(opts) {
          if (typeof opts === 'object' && opts !== null) {
              for (var prop in opts) {
                  if (opts.hasOwnProperty(prop)) {
                      figDefaults[prop] = opts[prop];
                  }
              }
          }
          return JSON.parse(JSON.stringify(figDefaults));
      };
  
      /*
          Parses data from a FIGlet font file and places it into the figFonts object.
      */
      me.parseFont = function(fontName, data) {
          data = data.replace(/\r\n/g,"\n").replace(/\r/g,"\n");
          figFonts[fontName] = {};
  
          var lines = data.split("\n");
          var headerData = lines.splice(0,1)[0].split(" ");
          var figFont = figFonts[fontName];
          var opts = {};
  
          opts.hardBlank = headerData[0].substr(5,1);
          opts.height = parseInt(headerData[1], 10);
          opts.baseline = parseInt(headerData[2], 10);
          opts.maxLength = parseInt(headerData[3], 10);
          opts.oldLayout = parseInt(headerData[4], 10);
          opts.numCommentLines = parseInt(headerData[5], 10);
          opts.printDirection = (headerData.length >= 6) ? parseInt(headerData[6], 10) : 0;
          opts.fullLayout = (headerData.length >= 7) ? parseInt(headerData[7], 10) : null;
          opts.codeTagCount = (headerData.length >= 8) ? parseInt(headerData[8], 10) : null;
          opts.fittingRules = getSmushingRules(opts.oldLayout, opts.fullLayout);
  
          figFont.options = opts;
  
          // error check
          if (opts.hardBlank.length !== 1 ||
              isNaN(opts.height) ||
              isNaN(opts.baseline) ||
              isNaN(opts.maxLength) ||
              isNaN(opts.oldLayout) ||
              isNaN(opts.numCommentLines) )
          {
              throw new Error('FIGlet header contains invalid values.');
          }
  
          /*
              All FIGlet fonts must contain chars 32-126, 196, 214, 220, 228, 246, 252, 223
          */
  
          var charNums = [], ii;
          for (ii = 32; ii <= 126; ii++) {
              charNums.push(ii);
          }
          charNums = charNums.concat(196, 214, 220, 228, 246, 252, 223);
  
          // error check - validate that there are enough lines in the file
          if (lines.length < (opts.numCommentLines + (opts.height * charNums.length)) ) {
              throw new Error('FIGlet file is missing data.');
          }
  
          /*
              Parse out the context of the file and put it into our figFont object
          */
  
          var cNum, endCharRegEx, parseError = false;
  
          figFont.comment = lines.splice(0,opts.numCommentLines).join("\n");
          figFont.numChars = 0;
  
          while (lines.length > 0 && figFont.numChars < charNums.length) {
              cNum = charNums[figFont.numChars];
              figFont[cNum] = lines.splice(0,opts.height);
              // remove end sub-chars
              for (ii = 0; ii < opts.height; ii++) {
                  if (typeof figFont[cNum][ii] === "undefined") {
                      figFont[cNum][ii] = "";
                  } else {
                      endCharRegEx = new RegExp("\\"+figFont[cNum][ii].substr(figFont[cNum][ii].length-1,1)+"+$");
                      figFont[cNum][ii] = figFont[cNum][ii].replace(endCharRegEx,"");
                  }
              }
              figFont.numChars++;
          }
  
          /*
              Now we check to see if any additional characters are present
          */
  
          while (lines.length > 0) {
              cNum = lines.splice(0,1)[0].split(" ")[0];
              if ( /^0[xX][0-9a-fA-F]+$/.test(cNum) ) {
                  cNum = parseInt(cNum, 16);
              } else if ( /^0[0-7]+$/.test(cNum) ) {
                  cNum = parseInt(cNum, 8);
              } else if ( /^[0-9]+$/.test(cNum) ) {
                  cNum = parseInt(cNum, 10);
              } else if ( /^-0[xX][0-9a-fA-F]+$/.test(cNum) ) {
                  cNum = parseInt(cNum, 16);
              } else {
                  if (cNum === "") {break;}
                  // something's wrong
                  console.log("Invalid data:"+cNum);
                  parseError = true;
                  break;
              }
  
              figFont[cNum] = lines.splice(0,opts.height);
              // remove end sub-chars
              for (ii = 0; ii < opts.height; ii++) {
                  if (typeof figFont[cNum][ii] === "undefined") {
                      figFont[cNum][ii] = "";
                  } else {
                      endCharRegEx = new RegExp("\\"+figFont[cNum][ii].substr(figFont[cNum][ii].length-1,1)+"+$");
                      figFont[cNum][ii] = figFont[cNum][ii].replace(endCharRegEx,"");
                  }
              }
              figFont.numChars++;
          }
  
          // error check
          if (parseError === true) {
              throw new Error('Error parsing data.');
          }
  
          return opts;
      };
  
      /*
          Loads a font.
      */
      me.loadFont = function(fontName, next) {
          if (figFonts[fontName]) {
              next(null, figFonts[fontName].options);
              return;
          }
  
          if (typeof fetch !== 'function') {
            console.error('figlet.js requires the fetch API or a fetch polyfill such as https://cdnjs.com/libraries/fetch');
            throw new Error('fetch is required for figlet.js to work.')
          }
  
          fetch(figDefaults.fontPath + '/' + fontName + '.flf')
              .then(function(response) {
                  if(response.ok) {
                      return response.text();
                  }
  
                  console.log('Unexpected response', response);
                  throw new Error('Network response was not ok.');
              })
              .then(function(text) {
                  next(null, me.parseFont(fontName, text));
              })
              .catch(next);
      };
  
      /*
          loads a font synchronously, not implemented for the browser
       */
      me.loadFontSync = function(name) {
          if (figFonts[name]) {
            return figFonts[name].options;
          }
          throw new Error('synchronous font loading is not implemented for the browser');
      };
  
      /*
          preloads a list of fonts prior to using textSync
          - fonts: an array of font names (i.e. ["Standard","Soft"])
          - next: callback function
       */
      me.preloadFonts = function(fonts, next) {
          var fontData = [];
  
          fonts.reduce(function(promise, name){
              return promise.then(function() {
                  return fetch(figDefaults.fontPath + '/' + name + '.flf').then((response) => {
                      return response.text();
                  }).then(function(data) {
                      fontData.push(data);
                  });
              });
          }, Promise.resolve()).then(function(res){
              for(var i in fonts){
                  me.parseFont(fonts[i], fontData[i]);
              }
  
              if(next)next();
          });
      };
  
      me.figFonts = figFonts;
  
      return me;
  })();
  
  // for node.js
  if (typeof module !== 'undefined') {
      if (typeof module.exports !== 'undefined') {
          module.exports = figlet;
      }
  }
  
  },{}],129:[function(require,module,exports){
  var url = require("url");
  var http = require("http");
  var https = require("https");
  var assert = require("assert");
  var Writable = require("stream").Writable;
  var debug = require("debug")("follow-redirects");
  
  // RFC7231§4.2.1: Of the request methods defined by this specification,
  // the GET, HEAD, OPTIONS, and TRACE methods are defined to be safe.
  var SAFE_METHODS = { GET: true, HEAD: true, OPTIONS: true, TRACE: true };
  
  // Create handlers that pass events from native requests
  var eventHandlers = Object.create(null);
  ["abort", "aborted", "error", "socket", "timeout"].forEach(function (event) {
    eventHandlers[event] = function (arg) {
      this._redirectable.emit(event, arg);
    };
  });
  
  // An HTTP(S) request that can be redirected
  function RedirectableRequest(options, responseCallback) {
    // Initialize the request
    Writable.call(this);
    options.headers = options.headers || {};
    this._options = options;
    this._redirectCount = 0;
    this._redirects = [];
    this._requestBodyLength = 0;
    this._requestBodyBuffers = [];
  
    // Since http.request treats host as an alias of hostname,
    // but the url module interprets host as hostname plus port,
    // eliminate the host property to avoid confusion.
    if (options.host) {
      // Use hostname if set, because it has precedence
      if (!options.hostname) {
        options.hostname = options.host;
      }
      delete options.host;
    }
  
    // Attach a callback if passed
    if (responseCallback) {
      this.on("response", responseCallback);
    }
  
    // React to responses of native requests
    var self = this;
    this._onNativeResponse = function (response) {
      self._processResponse(response);
    };
  
    // Complete the URL object when necessary
    if (!options.pathname && options.path) {
      var searchPos = options.path.indexOf("?");
      if (searchPos < 0) {
        options.pathname = options.path;
      }
      else {
        options.pathname = options.path.substring(0, searchPos);
        options.search = options.path.substring(searchPos);
      }
    }
  
    // Perform the first request
    this._performRequest();
  }
  RedirectableRequest.prototype = Object.create(Writable.prototype);
  
  // Writes buffered data to the current native request
  RedirectableRequest.prototype.write = function (data, encoding, callback) {
    // Validate input and shift parameters if necessary
    if (!(typeof data === "string" || typeof data === "object" && ("length" in data))) {
      throw new Error("data should be a string, Buffer or Uint8Array");
    }
    if (typeof encoding === "function") {
      callback = encoding;
      encoding = null;
    }
  
    // Ignore empty buffers, since writing them doesn't invoke the callback
    // https://github.com/nodejs/node/issues/22066
    if (data.length === 0) {
      if (callback) {
        callback();
      }
      return;
    }
    // Only write when we don't exceed the maximum body length
    if (this._requestBodyLength + data.length <= this._options.maxBodyLength) {
      this._requestBodyLength += data.length;
      this._requestBodyBuffers.push({ data: data, encoding: encoding });
      this._currentRequest.write(data, encoding, callback);
    }
    // Error when we exceed the maximum body length
    else {
      this.emit("error", new Error("Request body larger than maxBodyLength limit"));
      this.abort();
    }
  };
  
  // Ends the current native request
  RedirectableRequest.prototype.end = function (data, encoding, callback) {
    // Shift parameters if necessary
    if (typeof data === "function") {
      callback = data;
      data = encoding = null;
    }
    else if (typeof encoding === "function") {
      callback = encoding;
      encoding = null;
    }
  
    // Write data and end
    var currentRequest = this._currentRequest;
    this.write(data || "", encoding, function () {
      currentRequest.end(null, null, callback);
    });
  };
  
  // Sets a header value on the current native request
  RedirectableRequest.prototype.setHeader = function (name, value) {
    this._options.headers[name] = value;
    this._currentRequest.setHeader(name, value);
  };
  
  // Clears a header value on the current native request
  RedirectableRequest.prototype.removeHeader = function (name) {
    delete this._options.headers[name];
    this._currentRequest.removeHeader(name);
  };
  
  // Proxy all other public ClientRequest methods
  [
    "abort", "flushHeaders", "getHeader",
    "setNoDelay", "setSocketKeepAlive", "setTimeout",
  ].forEach(function (method) {
    RedirectableRequest.prototype[method] = function (a, b) {
      return this._currentRequest[method](a, b);
    };
  });
  
  // Proxy all public ClientRequest properties
  ["aborted", "connection", "socket"].forEach(function (property) {
    Object.defineProperty(RedirectableRequest.prototype, property, {
      get: function () { return this._currentRequest[property]; },
    });
  });
  
  // Executes the next native request (initial or redirect)
  RedirectableRequest.prototype._performRequest = function () {
    // Load the native protocol
    var protocol = this._options.protocol;
    var nativeProtocol = this._options.nativeProtocols[protocol];
    if (!nativeProtocol) {
      this.emit("error", new Error("Unsupported protocol " + protocol));
      return;
    }
  
    // If specified, use the agent corresponding to the protocol
    // (HTTP and HTTPS use different types of agents)
    if (this._options.agents) {
      var scheme = protocol.substr(0, protocol.length - 1);
      this._options.agent = this._options.agents[scheme];
    }
  
    // Create the native request
    var request = this._currentRequest =
          nativeProtocol.request(this._options, this._onNativeResponse);
    this._currentUrl = url.format(this._options);
  
    // Set up event handlers
    request._redirectable = this;
    for (var event in eventHandlers) {
      /* istanbul ignore else */
      if (event) {
        request.on(event, eventHandlers[event]);
      }
    }
  
    // End a redirected request
    // (The first request must be ended explicitly with RedirectableRequest#end)
    if (this._isRedirect) {
      // Write the request entity and end.
      var i = 0;
      var buffers = this._requestBodyBuffers;
      (function writeNext() {
        if (i < buffers.length) {
          var buffer = buffers[i++];
          request.write(buffer.data, buffer.encoding, writeNext);
        }
        else {
          request.end();
        }
      }());
    }
  };
  
  // Processes a response from the current native request
  RedirectableRequest.prototype._processResponse = function (response) {
    // Store the redirected response
    if (this._options.trackRedirects) {
      this._redirects.push({
        url: this._currentUrl,
        headers: response.headers,
        statusCode: response.statusCode,
      });
    }
  
    // RFC7231§6.4: The 3xx (Redirection) class of status code indicates
    // that further action needs to be taken by the user agent in order to
    // fulfill the request. If a Location header field is provided,
    // the user agent MAY automatically redirect its request to the URI
    // referenced by the Location field value,
    // even if the specific status code is not understood.
    var location = response.headers.location;
    if (location && this._options.followRedirects !== false &&
        response.statusCode >= 300 && response.statusCode < 400) {
      // RFC7231§6.4: A client SHOULD detect and intervene
      // in cyclical redirections (i.e., "infinite" redirection loops).
      if (++this._redirectCount > this._options.maxRedirects) {
        this.emit("error", new Error("Max redirects exceeded."));
        return;
      }
  
      // RFC7231§6.4: Automatic redirection needs to done with
      // care for methods not known to be safe […],
      // since the user might not wish to redirect an unsafe request.
      // RFC7231§6.4.7: The 307 (Temporary Redirect) status code indicates
      // that the target resource resides temporarily under a different URI
      // and the user agent MUST NOT change the request method
      // if it performs an automatic redirection to that URI.
      var header;
      var headers = this._options.headers;
      if (response.statusCode !== 307 && !(this._options.method in SAFE_METHODS)) {
        this._options.method = "GET";
        // Drop a possible entity and headers related to it
        this._requestBodyBuffers = [];
        for (header in headers) {
          if (/^content-/i.test(header)) {
            delete headers[header];
          }
        }
      }
  
      // Drop the Host header, as the redirect might lead to a different host
      if (!this._isRedirect) {
        for (header in headers) {
          if (/^host$/i.test(header)) {
            delete headers[header];
          }
        }
      }
  
      // Perform the redirected request
      var redirectUrl = url.resolve(this._currentUrl, location);
      debug("redirecting to", redirectUrl);
      Object.assign(this._options, url.parse(redirectUrl));
      this._isRedirect = true;
      this._performRequest();
  
      // Discard the remainder of the response to avoid waiting for data
      response.destroy();
    }
    else {
      // The response is not a redirect; return it as-is
      response.responseUrl = this._currentUrl;
      response.redirects = this._redirects;
      this.emit("response", response);
  
      // Clean up
      this._requestBodyBuffers = [];
    }
  };
  
  // Wraps the key/value object of protocols with redirect functionality
  function wrap(protocols) {
    // Default settings
    var exports = {
      maxRedirects: 21,
      maxBodyLength: 10 * 1024 * 1024,
    };
  
    // Wrap each protocol
    var nativeProtocols = {};
    Object.keys(protocols).forEach(function (scheme) {
      var protocol = scheme + ":";
      var nativeProtocol = nativeProtocols[protocol] = protocols[scheme];
      var wrappedProtocol = exports[scheme] = Object.create(nativeProtocol);
  
      // Executes a request, following redirects
      wrappedProtocol.request = function (options, callback) {
        if (typeof options === "string") {
          options = url.parse(options);
          options.maxRedirects = exports.maxRedirects;
        }
        else {
          options = Object.assign({
            protocol: protocol,
            maxRedirects: exports.maxRedirects,
            maxBodyLength: exports.maxBodyLength,
          }, options);
        }
        options.nativeProtocols = nativeProtocols;
        assert.equal(options.protocol, protocol, "protocol mismatch");
        debug("options", options);
        return new RedirectableRequest(options, callback);
      };
  
      // Executes a GET request, following redirects
      wrappedProtocol.get = function (options, callback) {
        var request = wrappedProtocol.request(options, callback);
        request.end();
        return request;
      };
    });
    return exports;
  }
  
  // Exports
  module.exports = wrap({ http: http, https: https });
  module.exports.wrap = wrap;
  
  },{"assert":1,"debug":126,"http":54,"https":13,"stream":53,"url":75}],130:[function(require,module,exports){
  /**
   * Helpers.
   */
  
  var s = 1000;
  var m = s * 60;
  var h = m * 60;
  var d = h * 24;
  var y = d * 365.25;
  
  /**
   * Parse or format the given `val`.
   *
   * Options:
   *
   *  - `long` verbose formatting [false]
   *
   * @param {String|Number} val
   * @param {Object} [options]
   * @throws {Error} throw an error if val is not a non-empty string or a number
   * @return {String|Number}
   * @api public
   */
  
  module.exports = function(val, options) {
    options = options || {};
    var type = typeof val;
    if (type === 'string' && val.length > 0) {
      return parse(val);
    } else if (type === 'number' && isNaN(val) === false) {
      return options.long ? fmtLong(val) : fmtShort(val);
    }
    throw new Error(
      'val is not a non-empty string or a valid number. val=' +
        JSON.stringify(val)
    );
  };
  
  /**
   * Parse the given `str` and return milliseconds.
   *
   * @param {String} str
   * @return {Number}
   * @api private
   */
  
  function parse(str) {
    str = String(str);
    if (str.length > 100) {
      return;
    }
    var match = /^((?:\d+)?\.?\d+) *(milliseconds?|msecs?|ms|seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h|days?|d|years?|yrs?|y)?$/i.exec(
      str
    );
    if (!match) {
      return;
    }
    var n = parseFloat(match[1]);
    var type = (match[2] || 'ms').toLowerCase();
    switch (type) {
      case 'years':
      case 'year':
      case 'yrs':
      case 'yr':
      case 'y':
        return n * y;
      case 'days':
      case 'day':
      case 'd':
        return n * d;
      case 'hours':
      case 'hour':
      case 'hrs':
      case 'hr':
      case 'h':
        return n * h;
      case 'minutes':
      case 'minute':
      case 'mins':
      case 'min':
      case 'm':
        return n * m;
      case 'seconds':
      case 'second':
      case 'secs':
      case 'sec':
      case 's':
        return n * s;
      case 'milliseconds':
      case 'millisecond':
      case 'msecs':
      case 'msec':
      case 'ms':
        return n;
      default:
        return undefined;
    }
  }
  
  /**
   * Short format for `ms`.
   *
   * @param {Number} ms
   * @return {String}
   * @api private
   */
  
  function fmtShort(ms) {
    if (ms >= d) {
      return Math.round(ms / d) + 'd';
    }
    if (ms >= h) {
      return Math.round(ms / h) + 'h';
    }
    if (ms >= m) {
      return Math.round(ms / m) + 'm';
    }
    if (ms >= s) {
      return Math.round(ms / s) + 's';
    }
    return ms + 'ms';
  }
  
  /**
   * Long format for `ms`.
   *
   * @param {Number} ms
   * @return {String}
   * @api private
   */
  
  function fmtLong(ms) {
    return plural(ms, d, 'day') ||
      plural(ms, h, 'hour') ||
      plural(ms, m, 'minute') ||
      plural(ms, s, 'second') ||
      ms + ' ms';
  }
  
  /**
   * Pluralization helper.
   */
  
  function plural(ms, n, name) {
    if (ms < n) {
      return;
    }
    if (ms < n * 1.5) {
      return Math.floor(ms / n) + ' ' + name;
    }
    return Math.ceil(ms / n) + ' ' + name + 's';
  }
  
  },{}],131:[function(require,module,exports){
  /**
   * REQUIRED MODULES
   */
  
  /**
   * CONFIGURATION VALUES
   */
  /**
   * CONFIGURATION VALUES
   */
  var nconf = require('./bconf');
  var verbose = Boolean(nconf.get('dgt-verbose'));; //Verbose on or off
  const splitWords = Boolean(nconf.get('splitWords'));
  speechSynthesisOn = Boolean(nconf.get('dgt-speech-synthesis'));; //Text To Speech on or off
  /**
   * GLOBAL VATIABLES
   */
  var lastText = '';
  
  module.exports = {
      say: (text) => {
          //Check Voice is disables
          if (!speechSynthesisOn) return;
          //Check if the text is the same as the last one, if so, skip this play
          if (text == lastText) {
              if (verbose) console.log(colors.dim.cyan(`Text: '${text}' is the same as the last Text received.`));
              //return;
              //Don't return since with SAN this can be very common. Wee need to know the color of the player to prevent real duplicates
          }
          else
              lastText = text;
  
          //Split phrase to have fewer audio files
          //text.split(' ').forEach(word => {
          if (verbose) console.log('TTS - for text: ' + text);
          wordArray(text).forEach(word => {
              if (word.length > 0 && word != " ") {
                  //window.speak(word);
                  var utterThis = new SpeechSynthesisUtterance(word);
                  var selectedOption = localStorage.getItem("dgt-speech-voice"); //voiceSelect.selectedOptions[0].getAttribute('data-name');
                  var availableVoices = speechSynthesis.getVoices();
                  for (i = 0; i < availableVoices.length; i++) {
                      if (availableVoices[i].name === selectedOption) {
                          utterThis.voice = availableVoices[i];
                          break;
                      }
                  }
                  //utterThis.pitch = pitch.value;
                  utterThis.rate = .60;
                  speechSynthesis.speak(utterThis);                
              }
          });
      }
  };
  
  /**
   * Return true if the string contains at least one numeric character
   * @param {string} myString 
   */
  function hasNumber(myString) {
      return /\d/.test(myString);
  }
  
  function wordArray(text) {
      var myArray = new Array;
      if (!splitWords) {
          //This will use more disk space but will sound much better
          //Just return a single item array with the whole text.
          myArray.push(text.trim());
          return myArray;
      }
      var wordArray = text.split(' ');
      var phrase = '';
      for (let i = 0; i < wordArray.length; i++) {
          //Check if has a number. If it has it must be its own word
          if (hasNumber(wordArray[i])) {
              //Push previos phrase into Array
              if (phrase != '') {
                  myArray.push(phrase.trim());
              }
              //Push this move as separate prhase into Array
              myArray.push(wordArray[i]);
              //Clear prhase
              phrase = '';
          }
          else if (wordArray[i].length > 0 && wordArray[i] != ' ') {
              //Since this is not a square keep adding this to the prhase
              phrase += ' ' + wordArray[i];
          }
      }
      //Push last phrase into Array if any
      if (phrase != '') {
          myArray.push(phrase.trim());
      }
      return myArray;
  }
  },{"./bconf":83}]},{},[82]);