(function (factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define(['jquery'], factory);
  } else if (typeof module === "object" && module.exports) {
    var $ = require('jquery');
    module.exports = factory($);
  } else {
    // Browser globals
    factory(jQuery);
  }
}(function (jQuery) {

/*!
 * jQuery.textcomplete
 *
 * Repository: https://github.com/yuku-t/jquery-textcomplete
 * License:    MIT (https://github.com/yuku-t/jquery-textcomplete/blob/master/LICENSE)
 * Author:     Yuku Takahashi
 */

if (typeof jQuery === 'undefined') {
  throw new Error('jQuery.textcomplete requires jQuery');
}

+function ($) {
  'use strict';

  var warn = function (message) {
    if (console.warn) { console.warn(message); }
  };

  var id = 1;

  $.fn.textcomplete = function (strategies, option) {
    var args = Array.prototype.slice.call(arguments);
    return this.each(function () {
      var self = this;
      var $this = $(this);
      var completer = $this.data('textComplete');
      if (!completer) {
        option || (option = {});
        option._oid = id++;  // unique object id
        completer = new $.fn.textcomplete.Completer(this, option);
        $this.data('textComplete', completer);
      }
      if (typeof strategies === 'string') {
        if (!completer) return;
        args.shift()
        completer[strategies].apply(completer, args);
        if (strategies === 'destroy') {
          $this.removeData('textComplete');
        }
      } else {
        // For backward compatibility.
        // TODO: Remove at v0.4
        $.each(strategies, function (obj) {
          $.each(['header', 'footer', 'placement', 'maxCount'], function (name) {
            if (obj[name]) {
              completer.option[name] = obj[name];
              warn(name + 'as a strategy param is deprecated. Use option.');
              delete obj[name];
            }
          });
        });
        completer.register($.fn.textcomplete.Strategy.parse(strategies, {
          el: self,
          $el: $this
        }));
      }
    });
  };

}(jQuery);

+function ($) {
  'use strict';

  // Exclusive execution control utility.
  //
  // func - The function to be locked. It is executed with a function named
  //        `free` as the first argument. Once it is called, additional
  //        execution are ignored until the free is invoked. Then the last
  //        ignored execution will be replayed immediately.
  //
  // Examples
  //
  //   var lockedFunc = lock(function (free) {
  //     setTimeout(function { free(); }, 1000); // It will be free in 1 sec.
  //     console.log('Hello, world');
  //   });
  //   lockedFunc();  // => 'Hello, world'
  //   lockedFunc();  // none
  //   lockedFunc();  // none
  //   // 1 sec past then
  //   // => 'Hello, world'
  //   lockedFunc();  // => 'Hello, world'
  //   lockedFunc();  // none
  //
  // Returns a wrapped function.
  var lock = function (func) {
    var locked, queuedArgsToReplay;

    return function () {
      // Convert arguments into a real array.
      var args = Array.prototype.slice.call(arguments);
      if (locked) {
        // Keep a copy of this argument list to replay later.
        // OK to overwrite a previous value because we only replay
        // the last one.
        queuedArgsToReplay = args;
        return;
      }
      locked = true;
      var self = this;
      args.unshift(function replayOrFree() {
        if (queuedArgsToReplay) {
          // Other request(s) arrived while we were locked.
          // Now that the lock is becoming available, replay
          // the latest such request, then call back here to
          // unlock (or replay another request that arrived
          // while this one was in flight).
          var replayArgs = queuedArgsToReplay;
          queuedArgsToReplay = undefined;
          replayArgs.unshift(replayOrFree);
          func.apply(self, replayArgs);
        } else {
          locked = false;
        }
      });
      func.apply(this, args);
    };
  };

  var isString = function (obj) {
    return Object.prototype.toString.call(obj) === '[object String]';
  };

  var uniqueId = 0;

  function Completer(element, option) {
    this.$el        = $(element);
    this.id         = 'textcomplete' + uniqueId++;
    this.strategies = [];
    this.views      = [];
    this.option     = $.extend({}, Completer.defaults, option);

    if (!this.$el.is('input[type=text]') && !this.$el.is('input[type=search]') && !this.$el.is('textarea') && !element.isContentEditable && element.contentEditable != 'true') {
      throw new Error('textcomplete must be called on a Textarea or a ContentEditable.');
    }

    // use ownerDocument to fix iframe / IE issues
    if (element === element.ownerDocument.activeElement) {
      // element has already been focused. Initialize view objects immediately.
      this.initialize()
    } else {
      // Initialize view objects lazily.
      var self = this;
      this.$el.one('focus.' + this.id, function () { self.initialize(); });

      // Special handling for CKEditor: lazy init on instance load
      if ((!this.option.adapter || this.option.adapter == 'CKEditor') && typeof CKEDITOR != 'undefined' && (this.$el.is('textarea'))) {
        CKEDITOR.on("instanceReady", function(event) {
          event.editor.once("focus", function(event2) {
            // replace the element with the Iframe element and flag it as CKEditor
            self.$el = $(event.editor.editable().$);
            if (!self.option.adapter) {
              self.option.adapter = $.fn.textcomplete['CKEditor'];
              self.option.ckeditor_instance = event.editor;
            }
            self.initialize();
          });
        });
      }
    }
  }

  Completer.defaults = {
    appendTo: 'body',
    className: '',  // deprecated option
    dropdownClassName: 'dropdown-menu textcomplete-dropdown',
    maxCount: 10,
    zIndex: '100',
    rightEdgeOffset: 30
  };

  $.extend(Completer.prototype, {
    // Public properties
    // -----------------

    id:         null,
    option:     null,
    strategies: null,
    adapter:    null,
    dropdown:   null,
    $el:        null,
    $iframe:    null,

    // Public methods
    // --------------

    initialize: function () {
      var element = this.$el.get(0);
      
      // check if we are in an iframe
      // we need to alter positioning logic if using an iframe
      if (this.$el.prop('ownerDocument') !== document && window.frames.length) {
        for (var iframeIndex = 0; iframeIndex < window.frames.length; iframeIndex++) {
          if (this.$el.prop('ownerDocument') === window.frames[iframeIndex].document) {
            this.$iframe = $(window.frames[iframeIndex].frameElement);
            break;
          }
        }
      }
      
      
      // Initialize view objects.
      this.dropdown = new $.fn.textcomplete.Dropdown(element, this, this.option);
      var Adapter, viewName;
      if (this.option.adapter) {
        Adapter = this.option.adapter;
      } else {
        if (this.$el.is('textarea') || this.$el.is('input[type=text]') || this.$el.is('input[type=search]')) {
          viewName = typeof element.selectionEnd === 'number' ? 'Textarea' : 'IETextarea';
        } else {
          viewName = 'ContentEditable';
        }
        Adapter = $.fn.textcomplete[viewName];
      }
      this.adapter = new Adapter(element, this, this.option);
    },

    destroy: function () {
      this.$el.off('.' + this.id);
      if (this.adapter) {
        this.adapter.destroy();
      }
      if (this.dropdown) {
        this.dropdown.destroy();
      }
      this.$el = this.adapter = this.dropdown = null;
    },

    deactivate: function () {
      if (this.dropdown) {
        this.dropdown.deactivate();
      }
    },

    // Invoke textcomplete.
    trigger: function (text, skipUnchangedTerm) {
      if (!this.dropdown) { this.initialize(); }
      text != null || (text = this.adapter.getTextFromHeadToCaret());
      var searchQuery = this._extractSearchQuery(text);
      if (searchQuery.length) {
        var term = searchQuery[1];
        // Ignore shift-key, ctrl-key and so on.
        if (skipUnchangedTerm && this._term === term && term !== "") { return; }
        this._term = term;
        this._search.apply(this, searchQuery);
      } else {
        this._term = null;
        this.dropdown.deactivate();
      }
    },

    fire: function (eventName) {
      var args = Array.prototype.slice.call(arguments, 1);
      this.$el.trigger(eventName, args);
      return this;
    },

    register: function (strategies) {
      Array.prototype.push.apply(this.strategies, strategies);
    },

    // Insert the value into adapter view. It is called when the dropdown is clicked
    // or selected.
    //
    // value    - The selected element of the array callbacked from search func.
    // strategy - The Strategy object.
    // e        - Click or keydown event object.
    select: function (value, strategy, e) {
      this._term = null;
      this.adapter.select(value, strategy, e);
      this.fire('change').fire('textComplete:select', value, strategy);
      this.adapter.focus();
    },

    // Private properties
    // ------------------

    _clearAtNext: true,
    _term:        null,

    // Private methods
    // ---------------

    // Parse the given text and extract the first matching strategy.
    //
    // Returns an array including the strategy, the query term and the match
    // object if the text matches an strategy; otherwise returns an empty array.
    _extractSearchQuery: function (text) {
      for (var i = 0; i < this.strategies.length; i++) {
        var strategy = this.strategies[i];
        var context = strategy.context(text);
        if (context || context === '') {
          var matchRegexp = $.isFunction(strategy.match) ? strategy.match(text) : strategy.match;
          if (isString(context)) { text = context; }
          var match = text.match(matchRegexp);
          if (match) { return [strategy, match[strategy.index], match]; }
        }
      }
      return []
    },

    // Call the search method of selected strategy..
    _search: lock(function (free, strategy, term, match) {
      var self = this;
      strategy.search(term, function (data, stillSearching) {
        if (!self.dropdown.shown) {
          self.dropdown.activate();
        }
        if (self._clearAtNext) {
          // The first callback in the current lock.
          self.dropdown.clear();
          self._clearAtNext = false;
        }
        self.dropdown.setPosition(self.adapter.getCaretPosition());
        self.dropdown.render(self._zip(data, strategy, term));
        if (!stillSearching) {
          // The last callback in the current lock.
          free();
          self._clearAtNext = true; // Call dropdown.clear at the next time.
        }
      }, match);
    }),

    // Build a parameter for Dropdown#render.
    //
    // Examples
    //
    //  this._zip(['a', 'b'], 's');
    //  //=> [{ value: 'a', strategy: 's' }, { value: 'b', strategy: 's' }]
    _zip: function (data, strategy, term) {
      return $.map(data, function (value) {
        return { value: value, strategy: strategy, term: term };
      });
    }
  });

  $.fn.textcomplete.Completer = Completer;
}(jQuery);

+function ($) {
  'use strict';

  var $window = $(window);

  var include = function (zippedData, datum) {
    var i, elem;
    var idProperty = datum.strategy.idProperty
    for (i = 0; i < zippedData.length; i++) {
      elem = zippedData[i];
      if (elem.strategy !== datum.strategy) continue;
      if (idProperty) {
        if (elem.value[idProperty] === datum.value[idProperty]) return true;
      } else {
        if (elem.value === datum.value) return true;
      }
    }
    return false;
  };

  var dropdownViews = {};
  $(document).on('click', function (e) {
    var id = e.originalEvent && e.originalEvent.keepTextCompleteDropdown;
    $.each(dropdownViews, function (key, view) {
      if (key !== id) { view.deactivate(); }
    });
  });

  var commands = {
    SKIP_DEFAULT: 0,
    KEY_UP: 1,
    KEY_DOWN: 2,
    KEY_ENTER: 3,
    KEY_PAGEUP: 4,
    KEY_PAGEDOWN: 5,
    KEY_ESCAPE: 6
  };

  // Dropdown view
  // =============

  // Construct Dropdown object.
  //
  // element - Textarea or contenteditable element.
  function Dropdown(element, completer, option) {
    this.$el       = Dropdown.createElement(option);
    this.completer = completer;
    this.id        = completer.id + 'dropdown';
    this._data     = []; // zipped data.
    this.$inputEl  = $(element);
    this.option    = option;

    // Override setPosition method.
    if (option.listPosition) { this.setPosition = option.listPosition; }
    if (option.height) { this.$el.height(option.height); }
    var self = this;
    $.each(['maxCount', 'placement', 'footer', 'header', 'noResultsMessage', 'className'], function (_i, name) {
      if (option[name] != null) { self[name] = option[name]; }
    });
    this._bindEvents(element);
    dropdownViews[this.id] = this;
  }

  $.extend(Dropdown, {
    // Class methods
    // -------------

    createElement: function (option) {
      var $parent = option.appendTo;
      if (!($parent instanceof $)) { $parent = $($parent); }
      var $el = $('<ul></ul>')
        .addClass(option.dropdownClassName)
        .attr('id', 'textcomplete-dropdown-' + option._oid)
        .css({
          display: 'none',
          left: 0,
          position: 'absolute',
          zIndex: option.zIndex
        })
        .appendTo($parent);
      return $el;
    }
  });

  $.extend(Dropdown.prototype, {
    // Public properties
    // -----------------

    $el:       null,  // jQuery object of ul.dropdown-menu element.
    $inputEl:  null,  // jQuery object of target textarea.
    completer: null,
    footer:    null,
    header:    null,
    id:        null,
    maxCount:  null,
    placement: '',
    shown:     false,
    data:      [],     // Shown zipped data.
    className: '',

    // Public methods
    // --------------

    destroy: function () {
      // Don't remove $el because it may be shared by several textcompletes.
      this.deactivate();

      this.$el.off('.' + this.id);
      this.$inputEl.off('.' + this.id);
      this.clear();
      this.$el.remove();
      this.$el = this.$inputEl = this.completer = null;
      delete dropdownViews[this.id]
    },

    render: function (zippedData) {
      var contentsHtml = this._buildContents(zippedData);
      var unzippedData = $.map(zippedData, function (d) { return d.value; });
      if (zippedData.length) {
        var strategy = zippedData[0].strategy;
        if (strategy.id) {
          this.$el.attr('data-strategy', strategy.id);
        } else {
          this.$el.removeAttr('data-strategy');
        }
        this._renderHeader(unzippedData);
        this._renderFooter(unzippedData);
        if (contentsHtml) {
          this._renderContents(contentsHtml);
          this._fitToBottom();
          this._fitToRight();
          this._activateIndexedItem();
        }
        this._setScroll();
      } else if (this.noResultsMessage) {
        this._renderNoResultsMessage(unzippedData);
      } else if (this.shown) {
        this.deactivate();
      }
    },

    setPosition: function (pos) {
      // Make the dropdown fixed if the input is also fixed
      // This can't be done during init, as textcomplete may be used on multiple elements on the same page
      // Because the same dropdown is reused behind the scenes, we need to recheck every time the dropdown is showed
      var position = 'absolute';
      // Check if input or one of its parents has positioning we need to care about
      this.$inputEl.add(this.$inputEl.parents()).each(function() {
        if($(this).css('position') === 'absolute') // The element has absolute positioning, so it's all OK
          return false;
        if($(this).css('position') === 'fixed') {
          pos.top -= $window.scrollTop();
          pos.left -= $window.scrollLeft();
          position = 'fixed';
          return false;
        }
      });
      this.$el.css(this._applyPlacement(pos));
      this.$el.css({ position: position }); // Update positioning

      return this;
    },

    clear: function () {
      this.$el.html('');
      this.data = [];
      this._index = 0;
      this._$header = this._$footer = this._$noResultsMessage = null;
    },

    activate: function () {
      if (!this.shown) {
        this.clear();
        this.$el.show();
        if (this.className) { this.$el.addClass(this.className); }
        this.completer.fire('textComplete:show');
        this.shown = true;
      }
      return this;
    },

    deactivate: function () {
      if (this.shown) {
        this.$el.hide();
        if (this.className) { this.$el.removeClass(this.className); }
        this.completer.fire('textComplete:hide');
        this.shown = false;
      }
      return this;
    },

    isUp: function (e) {
      return e.keyCode === 38 || (e.ctrlKey && e.keyCode === 80);  // UP, Ctrl-P
    },

    isDown: function (e) {
      return e.keyCode === 40 || (e.ctrlKey && e.keyCode === 78);  // DOWN, Ctrl-N
    },

    isEnter: function (e) {
      var modifiers = e.ctrlKey || e.altKey || e.metaKey || e.shiftKey;
      return !modifiers && (e.keyCode === 13 || e.keyCode === 9 || (this.option.completeOnSpace === true && e.keyCode === 32))  // ENTER, TAB
    },

    isPageup: function (e) {
      return e.keyCode === 33;  // PAGEUP
    },

    isPagedown: function (e) {
      return e.keyCode === 34;  // PAGEDOWN
    },

    isEscape: function (e) {
      return e.keyCode === 27;  // ESCAPE
    },

    // Private properties
    // ------------------

    _data:    null,  // Currently shown zipped data.
    _index:   null,
    _$header: null,
    _$noResultsMessage: null,
    _$footer: null,

    // Private methods
    // ---------------

    _bindEvents: function () {
      this.$el.on('mousedown.' + this.id, '.textcomplete-item', $.proxy(this._onClick, this));
      this.$el.on('touchstart.' + this.id, '.textcomplete-item', $.proxy(this._onClick, this));
      this.$el.on('mouseover.' + this.id, '.textcomplete-item', $.proxy(this._onMouseover, this));
      this.$inputEl.on('keydown.' + this.id, $.proxy(this._onKeydown, this));
    },

    _onClick: function (e) {
      var $el = $(e.target);
      e.preventDefault();
      e.originalEvent.keepTextCompleteDropdown = this.id;
      if (!$el.hasClass('textcomplete-item')) {
        $el = $el.closest('.textcomplete-item');
      }
      var datum = this.data[parseInt($el.data('index'), 10)];
      this.completer.select(datum.value, datum.strategy, e);
      var self = this;
      // Deactive at next tick to allow other event handlers to know whether
      // the dropdown has been shown or not.
      setTimeout(function () {
        self.deactivate();
        if (e.type === 'touchstart') {
          self.$inputEl.focus();
        }
      }, 0);
    },

    // Activate hovered item.
    _onMouseover: function (e) {
      var $el = $(e.target);
      e.preventDefault();
      if (!$el.hasClass('textcomplete-item')) {
        $el = $el.closest('.textcomplete-item');
      }
      this._index = parseInt($el.data('index'), 10);
      this._activateIndexedItem();
    },

    _onKeydown: function (e) {
      if (!this.shown) { return; }

      var command;

      if ($.isFunction(this.option.onKeydown)) {
        command = this.option.onKeydown(e, commands);
      }

      if (command == null) {
        command = this._defaultKeydown(e);
      }

      switch (command) {
        case commands.KEY_UP:
          e.preventDefault();
          this._up();
          break;
        case commands.KEY_DOWN:
          e.preventDefault();
          this._down();
          break;
        case commands.KEY_ENTER:
          e.preventDefault();
          this._enter(e);
          break;
        case commands.KEY_PAGEUP:
          e.preventDefault();
          this._pageup();
          break;
        case commands.KEY_PAGEDOWN:
          e.preventDefault();
          this._pagedown();
          break;
        case commands.KEY_ESCAPE:
          e.preventDefault();
          this.deactivate();
          break;
      }
    },

    _defaultKeydown: function (e) {
      if (this.isUp(e)) {
        return commands.KEY_UP;
      } else if (this.isDown(e)) {
        return commands.KEY_DOWN;
      } else if (this.isEnter(e)) {
        return commands.KEY_ENTER;
      } else if (this.isPageup(e)) {
        return commands.KEY_PAGEUP;
      } else if (this.isPagedown(e)) {
        return commands.KEY_PAGEDOWN;
      } else if (this.isEscape(e)) {
        return commands.KEY_ESCAPE;
      }
    },

    _up: function () {
      if (this._index === 0) {
        this._index = this.data.length - 1;
      } else {
        this._index -= 1;
      }
      this._activateIndexedItem();
      this._setScroll();
    },

    _down: function () {
      if (this._index === this.data.length - 1) {
        this._index = 0;
      } else {
        this._index += 1;
      }
      this._activateIndexedItem();
      this._setScroll();
    },

    _enter: function (e) {
      var datum = this.data[parseInt(this._getActiveElement().data('index'), 10)];
      this.completer.select(datum.value, datum.strategy, e);
      this.deactivate();
    },

    _pageup: function () {
      var target = 0;
      var threshold = this._getActiveElement().position().top - this.$el.innerHeight();
      this.$el.children().each(function (i) {
        if ($(this).position().top + $(this).outerHeight() > threshold) {
          target = i;
          return false;
        }
      });
      this._index = target;
      this._activateIndexedItem();
      this._setScroll();
    },

    _pagedown: function () {
      var target = this.data.length - 1;
      var threshold = this._getActiveElement().position().top + this.$el.innerHeight();
      this.$el.children().each(function (i) {
        if ($(this).position().top > threshold) {
          target = i;
          return false
        }
      });
      this._index = target;
      this._activateIndexedItem();
      this._setScroll();
    },

    _activateIndexedItem: function () {
      this.$el.find('.textcomplete-item.active').removeClass('active');
      this._getActiveElement().addClass('active');
    },

    _getActiveElement: function () {
      return this.$el.children('.textcomplete-item:nth(' + this._index + ')');
    },

    _setScroll: function () {
      var $activeEl = this._getActiveElement();
      var itemTop = $activeEl.position().top;
      var itemHeight = $activeEl.outerHeight();
      var visibleHeight = this.$el.innerHeight();
      var visibleTop = this.$el.scrollTop();
      if (this._index === 0 || this._index == this.data.length - 1 || itemTop < 0) {
        this.$el.scrollTop(itemTop + visibleTop);
      } else if (itemTop + itemHeight > visibleHeight) {
        this.$el.scrollTop(itemTop + itemHeight + visibleTop - visibleHeight);
      }
    },

    _buildContents: function (zippedData) {
      var datum, i, index;
      var html = '';
      for (i = 0; i < zippedData.length; i++) {
        if (this.data.length === this.maxCount) break;
        datum = zippedData[i];
        if (include(this.data, datum)) { continue; }
        index = this.data.length;
        this.data.push(datum);
        html += '<li class="textcomplete-item" data-index="' + index + '"><a>';
        html +=   datum.strategy.template(datum.value, datum.term);
        html += '</a></li>';
      }
      return html;
    },

    _renderHeader: function (unzippedData) {
      if (this.header) {
        if (!this._$header) {
          this._$header = $('<li class="textcomplete-header"></li>').prependTo(this.$el);
        }
        var html = $.isFunction(this.header) ? this.header(unzippedData) : this.header;
        this._$header.html(html);
      }
    },

    _renderFooter: function (unzippedData) {
      if (this.footer) {
        if (!this._$footer) {
          this._$footer = $('<li class="textcomplete-footer"></li>').appendTo(this.$el);
        }
        var html = $.isFunction(this.footer) ? this.footer(unzippedData) : this.footer;
        this._$footer.html(html);
      }
    },

    _renderNoResultsMessage: function (unzippedData) {
      if (this.noResultsMessage) {
        if (!this._$noResultsMessage) {
          this._$noResultsMessage = $('<li class="textcomplete-no-results-message"></li>').appendTo(this.$el);
        }
        var html = $.isFunction(this.noResultsMessage) ? this.noResultsMessage(unzippedData) : this.noResultsMessage;
        this._$noResultsMessage.html(html);
      }
    },

    _renderContents: function (html) {
      if (this._$footer) {
        this._$footer.before(html);
      } else {
        this.$el.append(html);
      }
    },

    _fitToBottom: function() {
      var windowScrollBottom = $window.scrollTop() + $window.height();
      var height = this.$el.height();
      if ((this.$el.position().top + height) > windowScrollBottom) {
        // only do this if we are not in an iframe
        if (!this.completer.$iframe) {
          this.$el.offset({top: windowScrollBottom - height});
        }
      }
    },

    _fitToRight: function() {
      // We don't know how wide our content is until the browser positions us, and at that point it clips us
      // to the document width so we don't know if we would have overrun it. As a heuristic to avoid that clipping
      // (which makes our elements wrap onto the next line and corrupt the next item), if we're close to the right
      // edge, move left. We don't know how far to move left, so just keep nudging a bit.
      var tolerance = this.option.rightEdgeOffset; // pixels. Make wider than vertical scrollbar because we might not be able to use that space.
      var lastOffset = this.$el.offset().left, offset;
      var width = this.$el.width();
      var maxLeft = $window.width() - tolerance;
      while (lastOffset + width > maxLeft) {
        this.$el.offset({left: lastOffset - tolerance});
        offset = this.$el.offset().left;
        if (offset >= lastOffset) { break; }
        lastOffset = offset;
      }
    },

    _applyPlacement: function (position) {
      // If the 'placement' option set to 'top', move the position above the element.
      if (this.placement.indexOf('top') !== -1) {
        // Overwrite the position object to set the 'bottom' property instead of the top.
        position = {
          top: 'auto',
          bottom: this.$el.parent().height() - position.top + position.lineHeight,
          left: position.left
        };
      } else {
        position.bottom = 'auto';
        delete position.lineHeight;
      }
      if (this.placement.indexOf('absleft') !== -1) {
        position.left = 0;
      } else if (this.placement.indexOf('absright') !== -1) {
        position.right = 0;
        position.left = 'auto';
      }
      return position;
    }
  });

  $.fn.textcomplete.Dropdown = Dropdown;
  $.extend($.fn.textcomplete, commands);
}(jQuery);

+function ($) {
  'use strict';

  // Memoize a search function.
  var memoize = function (func) {
    var memo = {};
    return function (term, callback) {
      if (memo[term]) {
        callback(memo[term]);
      } else {
        func.call(this, term, function (data) {
          memo[term] = (memo[term] || []).concat(data);
          callback.apply(null, arguments);
        });
      }
    };
  };

  function Strategy(options) {
    $.extend(this, options);
    if (this.cache) { this.search = memoize(this.search); }
  }

  Strategy.parse = function (strategiesArray, params) {
    return $.map(strategiesArray, function (strategy) {
      var strategyObj = new Strategy(strategy);
      strategyObj.el = params.el;
      strategyObj.$el = params.$el;
      return strategyObj;
    });
  };

  $.extend(Strategy.prototype, {
    // Public properties
    // -----------------

    // Required
    match:      null,
    replace:    null,
    search:     null,

    // Optional
    id:         null,
    cache:      false,
    context:    function () { return true; },
    index:      2,
    template:   function (obj) { return obj; },
    idProperty: null
  });

  $.fn.textcomplete.Strategy = Strategy;

}(jQuery);

+function ($) {
  'use strict';

  var now = Date.now || function () { return new Date().getTime(); };

  // Returns a function, that, as long as it continues to be invoked, will not
  // be triggered. The function will be called after it stops being called for
  // `wait` msec.
  //
  // This utility function was originally implemented at Underscore.js.
  var debounce = function (func, wait) {
    var timeout, args, context, timestamp, result;
    var later = function () {
      var last = now() - timestamp;
      if (last < wait) {
        timeout = setTimeout(later, wait - last);
      } else {
        timeout = null;
        result = func.apply(context, args);
        context = args = null;
      }
    };

    return function () {
      context = this;
      args = arguments;
      timestamp = now();
      if (!timeout) {
        timeout = setTimeout(later, wait);
      }
      return result;
    };
  };

  function Adapter () {}

  $.extend(Adapter.prototype, {
    // Public properties
    // -----------------

    id:        null, // Identity.
    completer: null, // Completer object which creates it.
    el:        null, // Textarea element.
    $el:       null, // jQuery object of the textarea.
    option:    null,

    // Public methods
    // --------------

    initialize: function (element, completer, option) {
      this.el        = element;
      this.$el       = $(element);
      this.id        = completer.id + this.constructor.name;
      this.completer = completer;
      this.option    = option;

      if (this.option.debounce) {
        this._onKeyup = debounce(this._onKeyup, this.option.debounce);
      }

      this._bindEvents();
    },

    destroy: function () {
      this.$el.off('.' + this.id); // Remove all event handlers.
      this.$el = this.el = this.completer = null;
    },

    // Update the element with the given value and strategy.
    //
    // value    - The selected object. It is one of the item of the array
    //            which was callbacked from the search function.
    // strategy - The Strategy associated with the selected value.
    select: function (/* value, strategy */) {
      throw new Error('Not implemented');
    },

    // Returns the caret's relative coordinates from body's left top corner.
    getCaretPosition: function () {
      var position = this._getCaretRelativePosition();
      var offset = this.$el.offset();

      // Calculate the left top corner of `this.option.appendTo` element.
      var $parent = this.option.appendTo;
      if ($parent) {
         if (!($parent instanceof $)) { $parent = $($parent); }
         var parentOffset = $parent.offsetParent().offset();
         offset.top -= parentOffset.top;
         offset.left -= parentOffset.left;
      }

      position.top += offset.top;
      position.left += offset.left;
      return position;
    },

    // Focus on the element.
    focus: function () {
      this.$el.focus();
    },

    // Private methods
    // ---------------

    _bindEvents: function () {
      this.$el.on('keyup.' + this.id, $.proxy(this._onKeyup, this));
    },

    _onKeyup: function (e) {
      if (this._skipSearch(e)) { return; }
      this.completer.trigger(this.getTextFromHeadToCaret(), true);
    },

    // Suppress searching if it returns true.
    _skipSearch: function (clickEvent) {
      switch (clickEvent.keyCode) {
        case 9:  // TAB
        case 13: // ENTER
        case 40: // DOWN
        case 38: // UP
        case 27: // ESC
          return true;
      }
      if (clickEvent.ctrlKey) switch (clickEvent.keyCode) {
        case 78: // Ctrl-N
        case 80: // Ctrl-P
          return true;
      }
    }
  });

  $.fn.textcomplete.Adapter = Adapter;
}(jQuery);

+function ($) {
  'use strict';

  // Textarea adapter
  // ================
  //
  // Managing a textarea. It doesn't know a Dropdown.
  function Textarea(element, completer, option) {
    this.initialize(element, completer, option);
  }

  $.extend(Textarea.prototype, $.fn.textcomplete.Adapter.prototype, {
    // Public methods
    // --------------

    // Update the textarea with the given value and strategy.
    select: function (value, strategy, e) {
      var pre = this.getTextFromHeadToCaret();
      var post = this.el.value.substring(this.el.selectionEnd);
      var newSubstr = strategy.replace(value, e);
      var regExp;
      if (typeof newSubstr !== 'undefined') {
        if ($.isArray(newSubstr)) {
          post = newSubstr[1] + post;
          newSubstr = newSubstr[0];
        }
        regExp = $.isFunction(strategy.match) ? strategy.match(pre) : strategy.match;
        pre = pre.replace(regExp, newSubstr);
        this.$el.val(pre + post);
        this.el.selectionStart = this.el.selectionEnd = pre.length;
      }
    },

    getTextFromHeadToCaret: function () {
      return this.el.value.substring(0, this.el.selectionEnd);
    },

    // Private methods
    // ---------------

    _getCaretRelativePosition: function () {
      var p = $.fn.textcomplete.getCaretCoordinates(this.el, this.el.selectionStart);
      return {
        top: p.top + this._calculateLineHeight() - this.$el.scrollTop(),
        left: p.left - this.$el.scrollLeft(),
        lineHeight: this._calculateLineHeight()
      };
    },

    _calculateLineHeight: function () {
      var lineHeight = parseInt(this.$el.css('line-height'), 10);
      if (isNaN(lineHeight)) {
        // http://stackoverflow.com/a/4515470/1297336
        var parentNode = this.el.parentNode;
        var temp = document.createElement(this.el.nodeName);
        var style = this.el.style;
        temp.setAttribute(
          'style',
          'margin:0px;padding:0px;font-family:' + style.fontFamily + ';font-size:' + style.fontSize
        );
        temp.innerHTML = 'test';
        parentNode.appendChild(temp);
        lineHeight = temp.clientHeight;
        parentNode.removeChild(temp);
      }
      return lineHeight;
    }
  });

  $.fn.textcomplete.Textarea = Textarea;
}(jQuery);

+function ($) {
  'use strict';

  var sentinelChar = '吶';

  function IETextarea(element, completer, option) {
    this.initialize(element, completer, option);
    $('<span>' + sentinelChar + '</span>').css({
      position: 'absolute',
      top: -9999,
      left: -9999
    }).insertBefore(element);
  }

  $.extend(IETextarea.prototype, $.fn.textcomplete.Textarea.prototype, {
    // Public methods
    // --------------

    select: function (value, strategy, e) {
      var pre = this.getTextFromHeadToCaret();
      var post = this.el.value.substring(pre.length);
      var newSubstr = strategy.replace(value, e);
      var regExp;
      if (typeof newSubstr !== 'undefined') {
        if ($.isArray(newSubstr)) {
          post = newSubstr[1] + post;
          newSubstr = newSubstr[0];
        }
        regExp = $.isFunction(strategy.match) ? strategy.match(pre) : strategy.match;
        pre = pre.replace(regExp, newSubstr);
        this.$el.val(pre + post);
        this.el.focus();
        var range = this.el.createTextRange();
        range.collapse(true);
        range.moveEnd('character', pre.length);
        range.moveStart('character', pre.length);
        range.select();
      }
    },

    getTextFromHeadToCaret: function () {
      this.el.focus();
      var range = document.selection.createRange();
      range.moveStart('character', -this.el.value.length);
      var arr = range.text.split(sentinelChar)
      return arr.length === 1 ? arr[0] : arr[1];
    }
  });

  $.fn.textcomplete.IETextarea = IETextarea;
}(jQuery);

// NOTE: TextComplete plugin has contenteditable support but it does not work
//       fine especially on old IEs.
//       Any pull requests are REALLY welcome.

+function ($) {
  'use strict';

  // ContentEditable adapter
  // =======================
  //
  // Adapter for contenteditable elements.
  function ContentEditable (element, completer, option) {
    this.initialize(element, completer, option);
  }

  $.extend(ContentEditable.prototype, $.fn.textcomplete.Adapter.prototype, {
    // Public methods
    // --------------

    // Update the content with the given value and strategy.
    // When an dropdown item is selected, it is executed.
    select: function (value, strategy, e) {
      var pre = this.getTextFromHeadToCaret();
      // use ownerDocument instead of window to support iframes
      var sel = this.el.ownerDocument.getSelection();
      
      var range = sel.getRangeAt(0);
      var selection = range.cloneRange();
      selection.selectNodeContents(range.startContainer);
      var content = selection.toString();
      var post = content.substring(range.startOffset);
      var newSubstr = strategy.replace(value, e);
      var regExp;
      if (typeof newSubstr !== 'undefined') {
        if ($.isArray(newSubstr)) {
          post = newSubstr[1] + post;
          newSubstr = newSubstr[0];
        }
        regExp = $.isFunction(strategy.match) ? strategy.match(pre) : strategy.match;
        pre = pre.replace(regExp, newSubstr)
            .replace(/ $/, "&nbsp"); // &nbsp necessary at least for CKeditor to not eat spaces
        range.selectNodeContents(range.startContainer);
        range.deleteContents();
        
        // create temporary elements
        var preWrapper = this.el.ownerDocument.createElement("div");
        preWrapper.innerHTML = pre;
        var postWrapper = this.el.ownerDocument.createElement("div");
        postWrapper.innerHTML = post;
        
        // create the fragment thats inserted
        var fragment = this.el.ownerDocument.createDocumentFragment();
        var childNode;
        var lastOfPre;
        while (childNode = preWrapper.firstChild) {
        	lastOfPre = fragment.appendChild(childNode);
        }
        while (childNode = postWrapper.firstChild) {
        	fragment.appendChild(childNode);
        }
        
        // insert the fragment & jump behind the last node in "pre"
        range.insertNode(fragment);
        range.setStartAfter(lastOfPre);
        
        range.collapse(true);
        sel.removeAllRanges();
        sel.addRange(range);
      }
    },

    // Private methods
    // ---------------

    // Returns the caret's relative position from the contenteditable's
    // left top corner.
    //
    // Examples
    //
    //   this._getCaretRelativePosition()
    //   //=> { top: 18, left: 200, lineHeight: 16 }
    //
    // Dropdown's position will be decided using the result.
    _getCaretRelativePosition: function () {
      var range = this.el.ownerDocument.getSelection().getRangeAt(0).cloneRange();
      var node = this.el.ownerDocument.createElement('span');
      range.insertNode(node);
      range.selectNodeContents(node);
      range.deleteContents();
      var $node = $(node);
      var position = $node.offset();
      position.left -= this.$el.offset().left;
      position.top += $node.height() - this.$el.offset().top;
      position.lineHeight = $node.height();
      
      // special positioning logic for iframes
      // this is typically used for contenteditables such as tinymce or ckeditor
      if (this.completer.$iframe) {
        var iframePosition = this.completer.$iframe.offset();
        position.top += iframePosition.top;
        position.left += iframePosition.left;
        //subtract scrollTop from element in iframe
        position.top -= this.$el.scrollTop(); 
      }
      
      $node.remove();
      return position;
    },

    // Returns the string between the first character and the caret.
    // Completer will be triggered with the result for start autocompleting.
    //
    // Example
    //
    //   // Suppose the html is '<b>hello</b> wor|ld' and | is the caret.
    //   this.getTextFromHeadToCaret()
    //   // => ' wor'  // not '<b>hello</b> wor'
    getTextFromHeadToCaret: function () {
      var range = this.el.ownerDocument.getSelection().getRangeAt(0);
      var selection = range.cloneRange();
      selection.selectNodeContents(range.startContainer);
      return selection.toString().substring(0, range.startOffset);
    }
  });

  $.fn.textcomplete.ContentEditable = ContentEditable;
}(jQuery);

// NOTE: TextComplete plugin has contenteditable support but it does not work
//       fine especially on old IEs.
//       Any pull requests are REALLY welcome.

+function ($) {
  'use strict';

  // CKEditor adapter
  // =======================
  //
  // Adapter for CKEditor, based on contenteditable elements.
  function CKEditor (element, completer, option) {
    this.initialize(element, completer, option);
  }

  $.extend(CKEditor.prototype, $.fn.textcomplete.ContentEditable.prototype, {
    _bindEvents: function () {
      var $this = this;
      this.option.ckeditor_instance.on('key', function(event) {
        var domEvent = event.data;
        $this._onKeyup(domEvent);
        if ($this.completer.dropdown.shown && $this._skipSearch(domEvent)) {
          return false;
        }
      }, null, null, 1); // 1 = Priority = Important!
      // we actually also need the native event, as the CKEditor one is happening to late
      this.$el.on('keyup.' + this.id, $.proxy(this._onKeyup, this));
    },
});

  $.fn.textcomplete.CKEditor = CKEditor;
}(jQuery);

// The MIT License (MIT)
// 
// Copyright (c) 2015 Jonathan Ong me@jongleberry.com
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// https://github.com/component/textarea-caret-position

(function ($) {

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

$.fn.textcomplete.getCaretCoordinates = getCaretCoordinates;

}(jQuery));

return jQuery;
}));
