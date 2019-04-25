lichess = window.lichess || {};

lichess.engineName = 'Stockfish 10+';

lichess.raf = window.requestAnimationFrame.bind(window);
lichess.requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);
lichess.dispatchEvent = function(el, eventName) {
  el.dispatchEvent(new Event(eventName));
};


lichess.hasTouchEvents = 'ontouchstart' in window;
lichess.mousedownEvent = lichess.hasTouchEvents ? 'touchstart' : 'mousedown';

function buildStorage(storageKey) {
  try {
    // just accessing localStorage can throw an exception...
    var storage = window[storageKey];
  } catch (e) {}
  var withStorage = storage ? function(f) {
    // this can throw exceptions as well.
    try { return f(storage); }
    catch (e) {}
  } : function() {};
  var storageObj = {
    get: function(k) {
      return withStorage(function(s) {
        return s.getItem(k);
      });
    },
    set: function(k, v) {
      withStorage(function(s) {
        try {
          s.setItem(k, v);
        } catch (e) {
          // removing first may help http://stackoverflow.com/questions/2603682/is-anyone-else-receiving-a-quota-exceeded-err-on-their-ipad-when-accessing-local
          s.removeItem(k);
          s.setItem(k, v);
        }
      });
    },
    remove: function(k) {
      withStorage(function(s) {
        s.removeItem(k);
      });
    },
    make: function(k) {
      return {
        get: function() {
          return storageObj.get(k);
        },
        set: function(v) {
          return storageObj.set(k, v);
        },
        remove: function() {
          return storageObj.remove(k);
        },
        listen: function(f) {
          window.addEventListener('storage', function(e) {
            if (e.key === k &&
              e.storageArea === storage &&
              e.newValue !== null) f(e);
          });
        }
      };
    }
  };
  return storageObj;
};

lichess.storage = buildStorage('localStorage');

lichess.once = function(key, mod) {
  if (mod === 'always') return true;
  if (!lichess.storage.get(key)) {
    lichess.storage.set(key, 1);
    return true;
  }
  return false;
};
lichess.debounce = function(func, wait, immediate) {
  var timeout;
  var lastBounce = 0;
  return function() {
    var context = this,
      args = arguments,
      elapsed = Date.now() - lastBounce;
    lastBounce = Date.now();
    var later = function() {
      timeout = null;
      func.apply(context, args);
    };
    clearTimeout(timeout);
    if (immediate && elapsed > wait) func.apply(context, args);
    else timeout = setTimeout(later, wait);
  };
};
lichess.powertip = (function() {

  function containedIn(el, container) {
    return container && container.contains(el);
  }
  function inCrosstable(el) {
    return containedIn(el, document.querySelector('.crosstable'));
  }

  var onPowertipPreRender = function(id, preload) {
    return function() {
      var url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
      if (preload) preload(url);
      $.ajax({
        url: url + '/mini',
        success: function(html) {
          $('#' + id).html(html);
          lichess.pubsub.emit('content_loaded')();
        }
      });
    };
  };

  var uptA = function(url, icon) {
    return '<a class="btn-rack__btn" href="' + url + '" data-icon="' + icon + '"></a>';
  }
  var userPowertip = function(el, pos) {
    pos = pos || el.getAttribute('data-pt-pos') || (
      inCrosstable(el) ? 'n' : 's'
    );
    $(el).removeClass('ulpt').powerTip({
      intentPollInterval: 200,
      placement: pos,
      smartPlacement: true,
      mouseOnToPopup: true,
      closeDelay: 200
    }).data('powertip', ' ').on({
      powerTipRender: onPowertipPreRender('powerTip', function(url) {
        var u = url.substr(3);
        $('#powerTip').html('<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' + $(el).html() + '</span></div></div><div class="upt__actions btn-rack">' +
          uptA('/@/' + u + '/tv', '1') +
          uptA('/inbox/new?user=' + u, 'c') +
          uptA('/?user=' + u + '#friend', 'U') +
          '<a class="btn-rack__btn relation-button" disabled></a></div>');
      })
    });
  };

  var gamePowertip = function(el) {
    $(el).removeClass('glpt').powerTip({
      intentPollInterval: 200,
      placement: inCrosstable(el) ? 'n' : 'w',
      smartPlacement: true,
      mouseOnToPopup: true,
      closeDelay: 200,
      popupId: 'miniGame'
    }).on({
      powerTipPreRender: onPowertipPreRender('miniGame')
    }).data('powertip', lichess.spinnerHtml);
  };

  var powerTipWith = function(el, ev, f) {
    if (lichess.isHoverable()) {
      f(el);
      $.powerTip.show(el, ev);
    }
  };

  function onIdleForAll(par, sel, fun) {
    lichess.requestIdleCallback(function() {
      Array.prototype.forEach.call(par.querySelectorAll(sel), fun);
    });
  }

  return {
    mouseover: function(e) {
      var t = e.target,
        cl = t.classList;
      if (cl.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (cl.contains('glpt')) powerTipWith(t, e, gamePowertip);
    },
    manualGameIn: function(parent) {
      onIdleForAll(parent, '.glpt', gamePowertip);
    },
    manualUserIn: function(parent) {
      onIdleForAll(parent, '.ulpt', function(el) { userPowertip(el) });
    }
  };
})();
lichess.widget = function(name, prototype) {
  var constructor = $[name] = function(options, element) {
    var self = this;
    self.element = $(element);
    $.data(element, name, self);
    self.options = options;
    self._create();
  };
  constructor.prototype = prototype;
  $.fn[name] = function(method) {
    var returnValue = this;
    var args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string') this.each(function() {
      var instance = $.data(this, name);
      if (!instance) return;
      if (!$.isFunction(instance[method]) || method.charAt(0) === "_")
        return $.error("no such method '" + method + "' for " + name + " widget instance");
      returnValue = instance[method].apply(instance, args);
    });
    else this.each(function() {
      if ($.data(this, name)) return $.error("widget " + name + " already bound to " + this);
      $.data(this, name, new constructor(method, this));
    });
    return returnValue;
  };
};
lichess.isHoverable = function () {
  if (typeof lichess.hoverable === 'undefined')
    lichess.hoverable = !!getComputedStyle(document.body).getPropertyValue('--hoverable');
  return lichess.hoverable;
};
lichess.spinnerHtml = '<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>';
lichess.assetUrl = function(path, opts) {
  opts = opts || {};
  var baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url');
  var version = document.body.getAttribute('data-asset-version');
  return baseUrl + '/assets' + (opts.noVersion ? '' : '/_' + version) + '/' + path;
};
lichess.loadedCss = {};
lichess.loadCss = function(url) {
  if (lichess.loadedCss[url]) return;
  lichess.loadedCss[url] = true;
  $('head').append($('<link rel="stylesheet" type="text/css" />').attr('href', lichess.assetUrl(url)));
};
lichess.loadCssPath = function(key) {
  lichess.loadCss('css/' + key + '.' + $('body').data('theme') + '.' + ($('body').data('dev') ? 'dev' : 'min') + '.css');
}
lichess.compiledScript = function(name) {
  return 'compiled/lichess.' + name + ($('body').data('dev') ? '' : '.min') + '.js';
}
lichess.loadScript = function(url, opts) {
  return $.ajax({
    dataType: "script",
    cache: true,
    url: lichess.assetUrl(url, opts)
  });
};
lichess.hopscotch = function(f) {
  lichess.loadCss('vendor/hopscotch/dist/css/hopscotch.min.css');
  lichess.loadScript('vendor/hopscotch/dist/js/hopscotch.min.js', {noVersion:true}).done(f);
}
lichess.slider = function() {
  return lichess.loadScript(
    'javascripts/vendor/jquery-ui.slider' + (hasTouchEvents ? '.touch' : '') + '.min.js',
    {noVersion:true}
  );
};
lichess.makeChat = function(data, callback) {
  lichess.raf(function() {
    data.loadCss = lichess.loadCssPath;
    (callback || $.noop)(LichessChat.default(document.querySelector('.mchat'), data));
  });
};

lichess.numberFormat = (function() {
  var formatter = false;
  return function(n) {
    if (formatter === false) formatter = (window.Intl && Intl.NumberFormat) ? new Intl.NumberFormat() : null;
    if (formatter === null) return n;
    return formatter.format(n);
  };
})();
lichess.idleTimer = function(delay, onIdle, onWakeUp) {
  var events = ['mousemove', 'touchstart'];
  var listening = false;
  var active = true;
  var lastSeenActive = Date.now();
  var onActivity = function() {
    if (!active) {
      // console.log('Wake up');
      onWakeUp();
    }
    active = true;
    lastSeenActive = Date.now();
    stopListening();
  };
  var startListening = function() {
    if (!listening) {
      events.forEach(function(e) {
        document.addEventListener(e, onActivity);
      });
      listening = true;
    }
  };
  var stopListening = function() {
    if (listening) {
      events.forEach(function(e) {
        document.removeEventListener(e, onActivity);
      });
      listening = false;
    }
  };
  setInterval(function() {
    if (active && Date.now() - lastSeenActive > delay) {
      // console.log('Idle mode');
      onIdle();
      active = false;
    }
    startListening();
  }, 10000);
};
lichess.pubsub = (function() {
  var subs = [];
  return {
    on: function(name, cb) {
      subs[name] = subs[name] || [];
      subs[name].push(cb);
    },
    off: function(name, cb) {
      if (!subs[name]) return;
      for (var i in subs[name]) {
        if (subs[name][i] === cb) {
          subs[name].splice(i);
          break;
        }
      }
    },
    emit: function(name) {
      return function() {
        if (!subs[name]) return;
        var args = Array.prototype.slice.call(arguments, 0);
        for (var i in subs[name]) subs[name][i].apply(null, args);
      }
    }
  };
})();
lichess.hasToReload = false;
lichess.redirectInProgress = false;
lichess.redirect = function(obj) {
  var url;
  if (typeof obj == "string") url = obj;
  else {
    url = obj.url;
    if (obj.cookie) {
      var domain = document.domain.replace(/^.+(\.[^\.]+\.[^\.]+)$/, '$1');
      var cookie = [
        encodeURIComponent(obj.cookie.name) + '=' + obj.cookie.value,
        '; max-age=' + obj.cookie.maxAge,
        '; path=/',
        '; domain=' + domain
      ].join('');
      document.cookie = cookie;
    }
  }
  var href = '//' + location.host + '/' + url.replace(/^\//, '');
  lichess.redirectInProgress = href;
  location.href = href;
};
lichess.reload = function() {
  if (lichess.redirectInProgress) return;
  lichess.hasToReload = true;
  if (location.hash) location.reload();
  else location.href = location.href;
};
lichess.escapeHtml = function(str) {
  return /[&<>\"\']/.test(str) ?
    str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, '&#39;')
    .replace(/"/g, '&quot;') :
    str;
};
$.fn.scrollTo = function(target, offsetTop) {
  return this.each(function() {
    if (typeof target === "number") this.scrollTop  = target;
    else {
      var $t = $(target);
      if (!$t.length) return;
      this.scrollTop = $t.offset().top + this.scrollTop - (offsetTop || 0);
    }
  });
};
$.modal = function(html, cls, onClose) {
  $.modal.close();
  if (!html.clone) html = $('<div>' + html + '</div>');
  var $wrap = $('<div id="modal-wrap">')
    .html(html.clone().removeClass('none'))
    .prepend('<span class="close" data-icon="L"></span>');
  var $overlay = $('<div id="modal-overlay">')
    .addClass(cls)
    .data('onClose', onClose)
    .html($wrap);
  $overlay.add($wrap.find('.close')).one('click', $.modal.close);
  $wrap.on('click', function(e) {
    e.stopPropagation();
  });
  $('body').addClass('overlayed').prepend($overlay);
  return $wrap;
};
$.modal.close = function() {
  $('body').removeClass('overlayed');
  $('#modal-overlay').each(function() {
    ($(this).data('onClose') || $.noop)();
    $(this).remove();
  });
};
