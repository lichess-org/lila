lichess = window.lichess || {};

lichess.engineName = 'Stockfish 10+';

lichess.raf = window.requestAnimationFrame.bind(window);
lichess.requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);
lichess.dispatchEvent = (el, eventName) => el.dispatchEvent(new Event(eventName));

lichess.hasTouchEvents = 'ontouchstart' in window;
lichess.mousedownEvent = lichess.hasTouchEvents ? 'touchstart' : 'mousedown';

lichess.isCol1 = (() => {
  let isCol1Cache = 'init'; // 'init' | 'rec' | boolean
  return () => {
    if (typeof isCol1Cache == 'string') {
      if (isCol1Cache == 'init') { // only once
        window.addEventListener('resize', () => { isCol1Cache = 'rec' }); // recompute on resize
        if (navigator.userAgent.indexOf('Edge/') > -1) // edge gets false positive on page load, fix later
          window.lichess.raf(() => { isCol1Cache = 'rec' });
      }
      isCol1Cache = !!getComputedStyle(document.body).getPropertyValue('--col1');
    }
    return isCol1Cache;
  };
})();

{
  const buildStorage = (storage) => {
    const api = {
      get: k => storage.getItem(k),
      set: (k, v) => storage.setItem(k, v),
      remove: k => storage.removeItem(k),
      make: k => ({
        get: () => api.get(k),
        set: v => api.set(k, v),
        remove: () => api.remove(k),
        listen: f => window.addEventListener('storage', e => {
          if (e.key === k &&
            e.storageArea === storage &&
            e.newValue !== null) f(e);
        })
      }),
      makeBoolean: k => ({
        get: () => api.get(k) == 1,
        set: v => api.set(k, v ? 1 : 0),
        toggle: () => api.set(k, api.get(k) == 1 ? 0 : 1)
      })
    };
    return api;
  };


  lichess.storage = buildStorage(window.localStorage);
  lichess.tempStorage = buildStorage(window.sessionStorage);
}

lichess.once = (key, mod) => {
  if (mod === 'always') return true;
  if (!lichess.storage.get(key)) {
    lichess.storage.set(key, 1);
    return true;
  }
  return false;
};
lichess.debounce = (func, wait, immediate) => {
  let timeout, lastBounce = 0;
  return function() {
    let context = this,
      args = arguments,
      elapsed = performance.now() - lastBounce;
    lastBounce = performance.now();
    let later = () => {
      timeout = null;
      func.apply(context, args);
    };
    clearTimeout(timeout);
    if (immediate && elapsed > wait) func.apply(context, args);
    else timeout = setTimeout(later, wait);
  };
};
lichess.powertip = (() => {

  function containedIn(el, container) {
    return container && container.contains(el);
  }
  function inCrosstable(el) {
    return containedIn(el, document.querySelector('.crosstable'));
  }

  function onPowertipPreRender(id, preload) {
    return function() {
      let url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
      if (preload) preload(url);
      $.ajax({
        url: url + '/mini',
        success: function(html) {
          $('#' + id).html(html);
          lichess.pubsub.emit('content_loaded');
        }
      });
    };
  };

  let uptA = (url, icon) => '<a class="btn-rack__btn" href="' + url + '" data-icon="' + icon + '"></a>';

  let userPowertip = (el, pos) => {
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
      powerTipRender: onPowertipPreRender('powerTip', (url) => {
        const u = url.substr(3);
        $('#powerTip').html('<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' + $(el).html() + '</span></div></div><div class="upt__actions btn-rack">' +
          uptA('/@/' + u + '/tv', '1') +
          uptA('/inbox/new?user=' + u, 'c') +
          uptA('/?user=' + u + '#friend', 'U') +
          '<a class="btn-rack__btn relation-button" disabled></a></div>');
      })
    });
  };

  function gamePowertip(el) {
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

  function powerTipWith(el, ev, f) {
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
    mouseover(e) {
      var t = e.target,
        cl = t.classList;
      if (cl.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (cl.contains('glpt')) powerTipWith(t, e, gamePowertip);
    },
    manualGameIn(parent) {
      onIdleForAll(parent, '.glpt', gamePowertip);
    },
    manualUserIn(parent) {
      onIdleForAll(parent, '.ulpt', (el) => userPowertip(el));
    }
  };
})();
lichess.widget = (name, prototype) => {
  var constructor = $[name] = function(options, element) {
    this.element = $(element);
    $.data(element, name, this);
    this.options = options;
    this._create();
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
      if (!$.data(this, name)) $.data(this, name, new constructor(method, this));
    });
    return returnValue;
  };
};
lichess.isHoverable = () => {
  if (typeof lichess.hoverable === 'undefined')
    lichess.hoverable = !lichess.hasTouchEvents || !!getComputedStyle(document.body).getPropertyValue('--hoverable');
  return lichess.hoverable;
};
lichess.spinnerHtml = '<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>';
lichess.assetUrl = (path, opts) => {
  opts = opts || {};
  const baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url'),
    version = document.body.getAttribute('data-asset-version');
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
    'javascripts/vendor/jquery-ui.slider' + (lichess.hasTouchEvents ? '.touch' : '') + '.min.js'
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
  var lastSeenActive = performance.now();
  var onActivity = function() {
    if (!active) {
      // console.log('Wake up');
      onWakeUp();
    }
    active = true;
    lastSeenActive = performance.now();
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
    if (active && performance.now() - lastSeenActive > delay) {
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
    on(name, cb) {
      subs[name] = subs[name] || [];
      subs[name].push(cb);
    },
    off(name, cb) {
      if (!subs[name]) return;
      for (var i in subs[name]) {
        if (subs[name][i] === cb) {
          subs[name].splice(i);
          break;
        }
      }
    },
    emit(name /*, args... */) {
      if (!subs[name]) return;
      const args = Array.prototype.slice.call(arguments, 1);
      for (let i in subs[name]) subs[name][i].apply(null, args);
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
      var domain = document.domain.replace(/^.+(\.[^.]+\.[^.]+)$/, '$1');
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
  return /[&<>"']/.test(str) ?
    str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, '&#39;')
    .replace(/"/g, '&quot;') :
    str;
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
  $wrap.find('.close').on('click', $.modal.close);
  $overlay.on('click', function() {
    // disgusting hack
    // dragging slider out of a modal closes the modal
    if (!$('.ui-slider-handle.ui-state-focus').length) $.modal.close();
  });
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
