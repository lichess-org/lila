lichess = window.lichess || {};

lichess.requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);
lichess.dispatchEvent = (el, eventName) => el.dispatchEvent(new Event(eventName));

lichess.hasTouchEvents = 'ontouchstart' in window;

// Unique id for the current document/navigation. Should be different after
// each page load and for each tab. Should be unpredictable and secret while
// in use.
try {
  const data = window.crypto.getRandomValues(new Uint8Array(9));
  lichess.sri = btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
} catch (_) {
  lichess.sri = Math.random().toString(36).slice(2, 12);
}

lichess.isCol1 = (() => {
  let isCol1Cache = 'init'; // 'init' | 'rec' | boolean
  return () => {
    if (typeof isCol1Cache == 'string') {
      if (isCol1Cache == 'init') { // only once
        window.addEventListener('resize', () => {
          isCol1Cache = 'rec'
        }); // recompute on resize
        if (navigator.userAgent.indexOf('Edge/') > -1) // edge gets false positive on page load, fix later
          requestAnimationFrame(() => {
            isCol1Cache = 'rec'
          });
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
      fire: (k, v) => storage.setItem(k, JSON.stringify({
        sri: lichess.sri,
        nonce: Math.random(), // ensure item changes
        value: v
      })),
      remove: k => storage.removeItem(k),
      make: k => ({
        get: () => api.get(k),
        set: v => api.set(k, v),
        fire: v => api.fire(k, v),
        remove: () => api.remove(k),
        listen: f => window.addEventListener('storage', e => {
          if (e.key !== k || e.storageArea !== storage || e.newValue === null) return;
          let parsed;
          try {
            parsed = JSON.parse(e.newValue);
          } catch (_) {
            return;
          }
          // check sri, because Safari fires events also in the original
          // document when there are multiple tabs
          if (parsed.sri && parsed.sri !== lichess.sri) f(parsed);
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

  const containedIn = (el, container) => container && container.contains(el);

  const inCrosstable = el => containedIn(el, document.querySelector('.crosstable'));

  function onPowertipPreRender(id, preload) {
    return function() {
      const url = ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '');
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

  const uptA = (url, icon) => '<a class="btn-rack__btn" href="' + url + '" data-icon="' + icon + '"></a>';

  const userPowertip = (el, pos) => {
    pos = pos || el.getAttribute('data-pt-pos') || (
      inCrosstable(el) ? 'n' : 's'
    );
    $(el).removeClass('ulpt').powerTip({
      intentPollInterval: 200,
      placement: pos,
      smartPlacement: true,
      closeDelay: 200
    }).data('powertip', ' ').on({
      powerTipRender: onPowertipPreRender('powerTip', (url) => {
        const u = url.substr(3);
        const name = $(el).data('name') || $(el).html();
        $('#powerTip').html('<div class="upt__info"><div class="upt__info__top"><span class="user-link offline">' + name + '</span></div></div><div class="upt__actions btn-rack">' +
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
    lichess.requestIdleCallback(() =>
      Array.prototype.forEach.call(par.querySelectorAll(sel), el => fun(el)) // do not codegolf to `fun`
    )
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
    manualUser: userPowertip,
    manualUserIn(parent) {
      onIdleForAll(parent, '.ulpt', userPowertip);
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
    var args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string') this.each(function() {
      const instance = $.data(this, name);
      if (instance && $.isFunction(instance[method])) instance[method].apply(instance, args);
    });
    else this.each(function() {
      if (!$.data(this, name)) $.data(this, name, new constructor(method, this));
    });
    return this;
  };
};
lichess.isHoverable = () => {
  if (typeof lichess.hoverable === 'undefined')
    lichess.hoverable = !lichess.hasTouchEvents /* Firefox <= 63 */ || !!getComputedStyle(document.body).getPropertyValue('--hoverable');
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
lichess.loadCss = url => {
  if (lichess.loadedCss[url]) return;
  lichess.loadedCss[url] = true;
  $('head').append($('<link rel="stylesheet" type="text/css" />').attr('href', lichess.assetUrl(url)));
};

lichess.loadCssPath = key =>
  lichess.loadCss(`css/${key}.${$('body').data('theme')}.${$('body').data('dev') ? 'dev' : 'min'}.css`);

lichess.jsModule = name =>
  `compiled/lichess.${name}${$('body').data('dev') ? '' : '.min'}.js`;

lichess.loadScript = (url, opts) =>
  $.ajax({
    dataType: "script",
    cache: true,
    url: lichess.assetUrl(url, opts)
  });

lichess.hopscotch = f => {
  lichess.loadCss('vendor/hopscotch/dist/css/hopscotch.min.css');
  lichess.loadScript('vendor/hopscotch/dist/js/hopscotch.min.js', {
    noVersion: true
  }).done(f);
}
lichess.slider = () =>
  lichess.loadScript(
    'javascripts/vendor/jquery-ui.slider' + (lichess.hasTouchEvents ? '.touch' : '') + '.min.js'
  );
lichess.makeChat = (data, callback) =>
  requestAnimationFrame(function() {
    data.loadCss = lichess.loadCssPath;
    (callback || $.noop)(LichessChat(document.querySelector('.mchat'), data));
  });
lichess.formAjax = $form => ({
  url: $form.attr('action'),
  method: $form.attr('method') || 'post',
  data: $form.serialize()
});

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
  let subs = [];
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
    emit(name /*, args... */ ) {
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
  lichess.socket.disconnect();
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

lichess.miniBoard = {
  initAll() {
    Array.from(document.getElementsByClassName('mini-board--init')).forEach(lichess.miniBoard.init);
  },
  init(node) {
    if (!window.Chessground) return setTimeout(() => lichess.miniBoard.init(node), 500);
    const $el = $(node).removeClass('mini-board--init'),
      [fen, orientation, lm] = $el.data('state').split(',');
    $el.data('chessground', Chessground(node, {
      orientation,
      coordinates: false,
      viewOnly: !node.getAttribute('data-playable'),
      resizable: false,
      fen,
      lastMove: lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
      drawable: {
        enabled: false,
        visible: false
      }
    }));
  }
};

lichess.miniGame = (() => {
  const fenColor = fen => fen.indexOf(' b') > 0 ? 'black' : 'white';
  return {
    init(node) {
      if (!window.Chessground) setTimeout(() => lichess.miniGame.init(node), 200);
      const [fen, orientation, lm] = node.getAttribute('data-state').split(','),
        config = {
          coordinates: false,
          viewOnly: true,
          resizable: false,
          fen,
          orientation,
          lastMove: lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
          drawable: {
            enabled: false,
            visible: false
          }
        },
        $el = $(node).removeClass('mini-game--init'),
        $cg = $el.find('.cg-wrap'),
        turnColor = fenColor(fen);
      $cg.data('chessground', Chessground($cg[0], config));
      ['white', 'black'].forEach(color =>
        $el.find('.mini-game__clock--' + color).each(function() {
          $(this).clock({
            time: parseInt(this.getAttribute('data-time')),
            pause: color != turnColor
          });
        })
      );
      return node.getAttribute('data-live');
    },
    initAll() {
      const nodes = Array.from(document.getElementsByClassName('mini-game--init')),
        ids = nodes.map(lichess.miniGame.init).filter(id => id);
      if (ids.length) window.lichess.StrongSocket.firstConnect.then(send =>
        send('startWatching', ids.join(' '))
      );
    },
    update(node, data) {
      const $el = $(node),
        lm = data.lm,
        lastMove = lm && (lm[1] === '@' ? [lm.slice(2)] : [lm[0] + lm[1], lm[2] + lm[3]]),
        cg = $el.find('.cg-wrap').data('chessground');
      if (cg) cg.set({
        fen: data.fen,
        lastMove
      });
      const turnColor = fenColor(data.fen);
      const renderClock = (time, color) => {
        if (!isNaN(time)) $el.find('.mini-game__clock--' + color).clock('set', {
          time,
          pause: color != turnColor
        });
      };
      renderClock(data.wc, 'white');
      renderClock(data.bc, 'black');
    },
    finish(node, win) {
      ['white', 'black'].forEach(color => {
        const $clock = $(node).find('.mini-game__clock--' + color).each(function() {
          $(this).clock('destroy');
        });
        if (!$clock.data('managed')) // snabbdom
          $clock.replaceWith(`<span class="mini-game__result">${win ? (win == color[0] ? 1 : 0) : '½'}</span>`)
      });
    }
  }
})();

lichess.widget("clock", {
  _create: function() {
    this.target = this.options.time * 1000 + Date.now();
    if (!this.options.pause) this.interval = setInterval(this.render.bind(this), 1000);
    this.render();
  },

  set: function(opts) {
    this.options = opts;
    this.target = this.options.time * 1000 + Date.now();
    this.render();
    clearInterval(this.interval);
    if (!opts.pause) this.interval = setInterval(this.render.bind(this), 1000);
  },

  render: function() {
    if (document.body.contains(this.element[0])) {
      this.element.text(this.formatMs(this.target - Date.now()));
      this.element.toggleClass('clock--run', !this.options.pause);
    } else clearInterval(this.interval);
  },

  pad: function(x) {
    return (x < 10 ? '0' : '') + x;
  },

  formatMs: function(msTime) {
    const date = new Date(Math.max(0, msTime + 500)),
      hours = date.getUTCHours(),
      minutes = date.getUTCMinutes(),
      seconds = date.getUTCSeconds();
    return hours > 0 ?
      hours + ':' + this.pad(minutes) + ':' + this.pad(seconds) :
      minutes + ':' + this.pad(seconds);
  }
});
