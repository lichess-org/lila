lishogi = window.lishogi || {};

lishogi.requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);
lishogi.dispatchEvent = (el, eventName) => el.dispatchEvent(new Event(eventName));

lishogi.hasTouchEvents = 'ontouchstart' in window;

// Unique id for the current document/navigation. Should be different after
// each page load and for each tab. Should be unpredictable and secret while
// in use.
try {
  const data = window.crypto.getRandomValues(new Uint8Array(9));
  lishogi.sri = btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
} catch (_) {
  lishogi.sri = Math.random().toString(36).slice(2, 12);
}

lishogi.isCol1 = (() => {
  let isCol1Cache = 'init'; // 'init' | 'rec' | boolean
  return () => {
    if (typeof isCol1Cache == 'string') {
      if (isCol1Cache == 'init') {
        // only once
        window.addEventListener('resize', () => {
          isCol1Cache = 'rec';
        }); // recompute on resize
        if (navigator.userAgent.indexOf('Edge/') > -1)
          // edge gets false positive on page load, fix later
          requestAnimationFrame(() => {
            isCol1Cache = 'rec';
          });
      }
      isCol1Cache = !!getComputedStyle(document.body).getPropertyValue('--col1');
    }
    return isCol1Cache;
  };
})();

{
  const buildStorage = storage => {
    const api = {
      get: k => storage.getItem(k),
      set: (k, v) => storage.setItem(k, v),
      fire: (k, v) =>
        storage.setItem(
          k,
          JSON.stringify({
            sri: lishogi.sri,
            nonce: Math.random(), // ensure item changes
            value: v,
          })
        ),
      remove: k => storage.removeItem(k),
      make: k => ({
        get: () => api.get(k),
        set: v => api.set(k, v),
        fire: v => api.fire(k, v),
        remove: () => api.remove(k),
        listen: f =>
          window.addEventListener('storage', e => {
            if (e.key !== k || e.storageArea !== storage || e.newValue === null) return;
            let parsed;
            try {
              parsed = JSON.parse(e.newValue);
            } catch (_) {
              return;
            }
            // check sri, because Safari fires events also in the original
            // document when there are multiple tabs
            if (parsed.sri && parsed.sri !== lishogi.sri) f(parsed);
          }),
      }),
      makeBoolean: k => ({
        get: () => api.get(k) == 1,
        set: v => api.set(k, v ? 1 : 0),
        toggle: () => api.set(k, api.get(k) == 1 ? 0 : 1),
      }),
    };
    return api;
  };

  lishogi.storage = buildStorage(window.localStorage);
  lishogi.tempStorage = buildStorage(window.sessionStorage);
}

lishogi.once = (key, mod) => {
  if (mod === 'always') return true;
  if (!lishogi.storage.get(key)) {
    lishogi.storage.set(key, 1);
    return true;
  }
  return false;
};
lishogi.debounce = (func, wait, immediate) => {
  let timeout,
    lastBounce = 0;
  return function () {
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
lishogi.powertip = (() => {
  function containedIn(el, container) {
    return container && container.contains(el);
  }
  function inCrosstable(el) {
    return containedIn(el, document.querySelector('.crosstable'));
  }

  function onPowertipPreRender(id, preload) {
    return function () {
      let url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
      if (preload) preload(url);
      $.ajax({
        url: url + '/mini',
        success: function (html) {
          $('#' + id).html(html);
          lishogi.pubsub.emit('content_loaded');
        },
      });
    };
  }

  let uptA = (url, icon) => '<a class="btn-rack__btn" href="' + url + '" data-icon="' + icon + '"></a>';

  let userPowertip = (el, pos) => {
    pos = pos || el.getAttribute('data-pt-pos') || (inCrosstable(el) ? 'n' : 's');
    $(el)
      .removeClass('ulpt')
      .powerTip({
        intentPollInterval: 200,
        placement: pos,
        smartPlacement: true,
        mouseOnToPopup: true,
        closeDelay: 200,
      })
      .data('powertip', ' ')
      .on({
        powerTipRender: onPowertipPreRender('powerTip', url => {
          const u = url.substr(3);
          const name = $(el).data('name') || $(el).html();
          $('#powerTip').html(
            '<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' +
              name +
              '</span></div></div><div class="upt__actions btn-rack">' +
              uptA('/@/' + u + '/tv', '1') +
              uptA('/inbox/new?user=' + u, 'c') +
              uptA('/?user=' + u + '#friend', 'U') +
              '<a class="btn-rack__btn relation-button" disabled></a></div>'
          );
        }),
      });
  };

  function gamePowertip(el) {
    $(el)
      .removeClass('glpt')
      .powerTip({
        intentPollInterval: 200,
        placement: inCrosstable(el) ? 'n' : 'w',
        smartPlacement: true,
        mouseOnToPopup: true,
        closeDelay: 200,
        popupId: 'miniGame',
      })
      .on({
        powerTipPreRender: onPowertipPreRender('miniGame'),
      })
      .data('powertip', lishogi.spinnerHtml);
  }

  function powerTipWith(el, ev, f) {
    if (lishogi.isHoverable()) {
      f(el);
      $.powerTip.show(el, ev);
    }
  }

  function onIdleForAll(par, sel, fun) {
    lishogi.requestIdleCallback(function () {
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
    manualGame: gamePowertip,
    manualUserIn(parent) {
      onIdleForAll(parent, '.ulpt', el => userPowertip(el));
    },
  };
})();
lishogi.widget = (name, prototype) => {
  var constructor = ($[name] = function (options, element) {
    this.element = $(element);
    $.data(element, name, this);
    this.options = options;
    this._create();
  });
  constructor.prototype = prototype;
  $.fn[name] = function (method) {
    var returnValue = this;
    var args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string')
      this.each(function () {
        var instance = $.data(this, name);
        if (!instance) return;
        if (!$.isFunction(instance[method]) || method.charAt(0) === '_')
          return $.error("no such method '" + method + "' for " + name + ' widget instance');
        returnValue = instance[method].apply(instance, args);
      });
    else
      this.each(function () {
        if (!$.data(this, name)) $.data(this, name, new constructor(method, this));
      });
    return returnValue;
  };
};
lishogi.isHoverable = () => {
  if (typeof lishogi.hoverable === 'undefined')
    lishogi.hoverable =
      !lishogi.hasTouchEvents /* Firefox <= 63 */ || !!getComputedStyle(document.body).getPropertyValue('--hoverable');
  return lishogi.hoverable;
};
lishogi.spinnerHtml = `<div class="spinner"><svg viewBox="-2.5 -2.5 45 55" xmlns="http://www.w3.org/2000/svg">
  <path d="M 20 0 L 33 4 L 40 50 L 0 50 L 7 4 Z"
    style="fill:none;stroke-width:2.5;stroke-opacity:1;" />
</svg></div>`;
lishogi.assetUrl = (path, opts) => {
  opts = opts || {};
  const baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url'),
    version = document.body.getAttribute('data-asset-version');
  return baseUrl + '/assets' + (opts.noVersion ? '' : '/_' + version) + '/' + path;
};
lishogi.loadedCss = {};
lishogi.loadCss = function (url) {
  if (lishogi.loadedCss[url]) return;
  lishogi.loadedCss[url] = true;
  $('head').append($('<link rel="stylesheet" type="text/css" />').attr('href', lishogi.assetUrl(url)));
};
lishogi.loadCssPath = function (key) {
  lishogi.loadCss(
    'css/' + key + '.' + $('body').data('theme') + '.' + ($('body').data('dev') ? 'dev' : 'min') + '.css'
  );
};
lishogi.loadChushogiPieceSprite = function () {
  if (!document.getElementById('chu-piece-sprite')) {
    const cps = document.body.dataset.chuPieceSet || 'Chu_Ryoko_1Kanji';
    $('head').append(
      $('<link id="chu-piece-sprite" rel="stylesheet" type="text/css" />').attr(
        'href',
        lishogi.assetUrl(`piece-css/${cps}.css`)
      )
    );
  }
};
lishogi.loadKyotoshogiPieceSprite = function () {
  if (!document.getElementById('kyo-piece-sprite')) {
    const cps = document.body.dataset.kyoPieceSet || 'Kyo_Ryoko_1Kanji';
    $('head').append(
      $('<link id="kyo-piece-sprite" rel="stylesheet" type="text/css" />').attr(
        'href',
        lishogi.assetUrl(`piece-css/${cps}.css`)
      )
    );
  }
};

lishogi.compiledScript = function (name) {
  return 'compiled/lishogi.' + name + ($('body').data('dev') ? '' : '.min') + '.js';
};
lishogi.loadScript = function (url, opts) {
  return $.ajax({
    dataType: 'script',
    cache: true,
    url: lishogi.assetUrl(url, opts),
  });
};
lishogi.hopscotch = function (f) {
  lishogi.loadCss('vendor/hopscotch/dist/css/hopscotch.min.css');
  lishogi
    .loadScript('vendor/hopscotch/dist/js/hopscotch.min.js', {
      noVersion: true,
    })
    .done(f);
};
lishogi.slider = function () {
  return lishogi.loadScript(
    'javascripts/vendor/jquery-ui.slider' + (lishogi.hasTouchEvents ? '.touch' : '') + '.min.js'
  );
};
lishogi.spectrum = function () {
  lishogi.loadCss('vendor/spectrum/dist/spectrum.min.css');
  return lishogi.loadScript('vendor/spectrum/dist/spectrum.min.js');
};
lishogi.makeChat = function (data, callback) {
  requestAnimationFrame(function () {
    data.loadCss = lishogi.loadCssPath;
    (callback || $.noop)(LishogiChat(document.querySelector('.mchat'), data));
  });
};
lishogi.formAjax = $form => ({
  url: $form.attr('action'),
  method: $form.attr('method') || 'post',
  data: $form.serialize(),
});

lishogi.numberFormat = (function () {
  var formatter = false;
  return function (n) {
    if (formatter === false) formatter = window.Intl && Intl.NumberFormat ? new Intl.NumberFormat() : null;
    if (formatter === null) return n;
    return formatter.format(n);
  };
})();
lishogi.idleTimer = function (delay, onIdle, onWakeUp) {
  var events = ['mousemove', 'touchstart'];
  var listening = false;
  var active = true;
  var lastSeenActive = performance.now();
  var onActivity = function () {
    if (!active) {
      // console.log('Wake up');
      onWakeUp();
    }
    active = true;
    lastSeenActive = performance.now();
    stopListening();
  };
  var startListening = function () {
    if (!listening) {
      events.forEach(function (e) {
        document.addEventListener(e, onActivity);
      });
      listening = true;
    }
  };
  var stopListening = function () {
    if (listening) {
      events.forEach(function (e) {
        document.removeEventListener(e, onActivity);
      });
      listening = false;
    }
  };
  setInterval(function () {
    if (active && performance.now() - lastSeenActive > delay) {
      // console.log('Idle mode');
      onIdle();
      active = false;
    }
    startListening();
  }, 10000);
};
lishogi.pubsub = (function () {
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
    },
  };
})();
lishogi.hasToReload = false;
lishogi.redirectInProgress = false;
lishogi.redirect = function (obj) {
  var url;
  if (typeof obj == 'string') url = obj;
  else {
    url = obj.url;
    if (obj.cookie) {
      var domain = document.domain.replace(/^.+(\.[^.]+\.[^.]+)$/, '$1');
      var cookie = [
        encodeURIComponent(obj.cookie.name) + '=' + obj.cookie.value,
        '; max-age=' + obj.cookie.maxAge,
        '; path=/',
        '; domain=' + domain,
      ].join('');
      document.cookie = cookie;
    }
  }
  var href = '//' + location.host + '/' + url.replace(/^\//, '');
  lishogi.redirectInProgress = href;
  location.href = href;
};
lishogi.reload = function () {
  if (lishogi.redirectInProgress) return;
  lishogi.hasToReload = true;
  lishogi.socket.disconnect();
  if (location.hash) location.reload();
  else location.href = location.href;
};
lishogi.escapeHtml = function (str) {
  return /[&<>"']/.test(str)
    ? str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
    : str;
};
$.modal = function (html, cls, onClose, withDataAndEvents) {
  $.modal.close();
  if (!html.clone) html = $('<div>' + html + '</div>');
  var $wrap = $('<div id="modal-wrap">')
    .html(html.clone(withDataAndEvents).removeClass('none'))
    .prepend('<span class="close" data-icon="L"></span>');
  var $overlay = $('<div id="modal-overlay">').addClass(cls).data('onClose', onClose).html($wrap);
  $wrap.find('.close').on('click', $.modal.close);
  $overlay.on('mousedown', function () {
    $.modal.close();
  });
  $wrap.on('mousedown', function (e) {
    e.stopPropagation();
  });
  $('body').addClass('overlayed').prepend($overlay);
  return $wrap;
};
$.modal.close = function () {
  $('body').removeClass('overlayed');
  $('#modal-overlay').each(function () {
    ($(this).data('onClose') || $.noop)();
    $(this).remove();
  });
};
