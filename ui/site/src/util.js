var lichess = window.lichess = window.lichess || {};

lichess.engineName = 'Stockfish 8';

lichess.raf = (window.requestAnimationFrame || window.setTimeout).bind(window);
lichess.requestIdleCallback = (window.requestIdleCallback || window.setTimeout).bind(window);
lichess.storage = (function() {
  var withStorage = function(f) {
    // can throw an exception when storage is full
    try {
      return !!window.localStorage ? f(window.localStorage) : null;
    } catch (e) {}
  }
  return {
    get: function(k) {
      return withStorage(function(s) {
        return s.getItem(k);
      });
    },
    set: function(k, v) {
      // removing first may help http://stackoverflow.com/questions/2603682/is-anyone-else-receiving-a-quota-exceeded-err-on-their-ipad-when-accessing-local
      withStorage(function(s) {
        s.removeItem(k);
        s.setItem(k, v);
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
          return lichess.storage.get(k);
        },
        set: function(v) {
          return lichess.storage.set(k, v);
        },
        remove: function() {
          return lichess.storage.remove(k);
        },
        listen: function(f) {
          window.addEventListener('storage', function(e) {
            if (e.key === k && e.newValue !== null) f(e);
          });
        }
      };
    }
  };
})();
lichess.reloadOtherTabs = (function() {
  var storage = lichess.storage.make('reload-other-tabs');
  storage.listen(function() {
    lichess.reload();
  });
  return function() {
    storage.set(1);
  }
})();
lichess.once = function(key, mod) {
  if (mod === 'always') return true;
  if (!lichess.storage.get(key)) {
    lichess.storage.set(key, 1);
    return true;
  }
  return false;
};
lichess.powertip = (function() {

  var elementIdContains = function(id, contained) {
    var el = document.getElementById(id);
    return el && el.contains(contained);
  };

  var onPowertipPreRender = function(id) {
    return function() {
      $.ajax({
        url: ($(this).data('href') || $(this).attr('href')).replace(/\?.+$/, '') + '/mini',
        success: function(html) {
          $('#' + id).html(html);
          lichess.pubsub.emit('content_loaded')();
        }
      });
    };
  };

  var userPowertip = function(el, pos) {
    if (!pos) {
      if (elementIdContains('site_header', el)) pos = 'e';
      else pos = el.getAttribute('data-pt-pos') || 'w';
    }
    $(el).removeClass('ulpt').powerTip({
      intentPollInterval: 200,
      placement: pos,
      mouseOnToPopup: true,
      closeDelay: 200
    }).on({
      powerTipPreRender: onPowertipPreRender('powerTip')
    }).data('powertip', lichess.spinnerHtml);
  };

  var gamePowertip = function(el) {
    $(el).removeClass('glpt').powerTip({
      intentPollInterval: 200,
      placement: 'w',
      smartPlacement: true,
      mouseOnToPopup: true,
      closeDelay: 200,
      popupId: 'miniGame'
    }).on({
      powerTipPreRender: onPowertipPreRender('miniGame')
    }).data('powertip', lichess.spinnerHtml);
  };

  var powerTipWith = function(el, ev, f) {
    f(el);
    $.powerTip.show(el, ev);
  };

  return {
    mouseover: function(e) {
      var t = e.target,
        cl = t.classList;
      if (cl.contains('ulpt')) powerTipWith(t, e, userPowertip);
      else if (cl.contains('glpt')) powerTipWith(t, e, gamePowertip);
    },
    manualGameIn: function(parent) {
      Array.prototype.forEach.call(parent.querySelectorAll('.glpt'), gamePowertip);
    },
    manualUserIn: function(parent) {
      Array.prototype.forEach.call(parent.querySelectorAll('.ulpt'), function(el) {
        userPowertip(el);
      });
    }
  };
})();
lichess.trans = function(i18n) {
  var trans = function(key) {
    var str = i18n[key] || key;
    var args = Array.prototype.slice.call(arguments, 1);
    if (args.length && str.indexOf('$s') > -1) {
      for (var i = 1; i < 4; i++) {
        str = str.replace('%' + i + '$s', args[i - 1]);
      }
    }
    args.forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
  // optimisation for translations without arguments
  trans.noarg = function(key) {
    return i18n[key] || key;
  };
  trans.merge = function(more) {
    Object.keys(more).forEach(function(k) {
      i18n[k] = more[k];
    });
  };
  return trans;
};
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
lichess.isTrident = navigator.userAgent.indexOf('Trident/') > -1;
lichess.isChrome = navigator.userAgent.indexOf('Chrome/') > -1;
lichess.spinnerHtml = '<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>';
lichess.initiatingHtml = '<div class="initiating">' + lichess.spinnerHtml + '</div>';
lichess.assetUrl = function(url, opts) {
  opts = opts || {};
  var baseUrl = opts.sameDomain ? '' : document.body.getAttribute('data-asset-url');
  var version = document.body.getAttribute('data-asset-version');
  return baseUrl + url + (opts.noVersion ? '' : '?v=' + version);
};
lichess.loadedCss = {};
lichess.loadCss = function(url) {
  if (lichess.loadedCss[url]) return;
  lichess.loadedCss[url] = true;
  $('head').append($('<link rel="stylesheet" type="text/css" />').attr('href', lichess.assetUrl(url)));
};
lichess.loadScript = function(url, opts) {
  return $.ajax({
    dataType: "script",
    cache: true,
    url: lichess.assetUrl(url, opts)
  });
};
lichess.hopscotch = function(f) {
  lichess.loadCss('/assets/vendor/hopscotch/dist/css/hopscotch.min.css');
  lichess.loadScript("/assets/vendor/hopscotch/dist/js/hopscotch.min.js", {noVersion:true}).done(f);
}
lichess.slider = function() {
  lichess.loadCss('/assets/stylesheets/jquery-ui.css');
  return lichess.loadScript('/assets/javascripts/vendor/jquery-ui.slider.min.js', {noVersion:true});
};
lichess.shepherd = function(f) {
  var theme = 'shepherd-theme-' + ($('body').hasClass('dark') ? 'default' : 'dark');
  lichess.loadCss('/assets/vendor/shepherd/dist/css/' + theme + '.css');
  lichess.loadCss('/assets/stylesheets/shepherd.css');
  lichess.loadScript("/assets/vendor/shepherd/dist/js/tether.js", {noVersion:true}).done(function() {
    lichess.loadScript("/assets/vendor/shepherd/dist/js/shepherd.min.js", {noVersion:true}).done(function() {
      f(theme);
    });
  });
};
lichess.makeChat = function(id, data, callback) {
  var isDev = document.body.getAttribute('data-dev');
  data.loadCss = lichess.loadCss;
  lichess.loadScript("/assets/compiled/lichess.chat" + (isDev ? '' : '.min') + '.js').done(function() {
    (callback || $.noop)(LichessChat.default(document.getElementById(id), data));
  });
};

lichess.desktopNotification = (function() {
  var notifications = [];
  function closeAll() {
    notifications.forEach(function(n) {
      n.close();
    });
    notifications = [];
  };
  window.addEventListener('focus', closeAll);
  var storage = lichess.storage.make('just-notified');
  function notify(msg) {
    var now = Date.now();
    if (document.hasFocus() || now - storage.get() < 1000) return;
    storage.set(now);
    if ($.isFunction(msg)) msg = msg();
    var notification = new Notification('lichess.org', {
      icon: '//lichess1.org/assets/images/logo.256.png',
      body: msg
    });
    notification.onclick = function() {
      window.focus();
    };
    notifications.push(notification);
  };
  return function(msg) {
    if (document.hasFocus() || !('Notification' in window)) return;
    if (Notification.permission === 'granted') {
      // increase chances that the first tab can put a local storage lock
      setTimeout(notify, 10 + Math.random() * 500, msg);
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission(function(p) {
        if (p === 'granted') notify(msg);
      });
    };
  };
})();

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
// wtf was I thinking
lichess.partial = function() {
  return arguments[0].bind.apply(arguments[0], [null].concat(Array.prototype.slice.call(arguments, 1)));
};
lichess.hasToReload = false;
lichess.redirectInProgress = false;
lichess.reload = function() {
  if (lichess.redirectInProgress) return;
  lichess.hasToReload = true;
  if (window.location.hash) location.reload();
  else location.href = location.href;
};
lichess.escapeHtml = function(html) {
  var div = document.createElement('div');
  div.appendChild(document.createTextNode(html));
  return div.innerHTML;
};
lichess.toYouTubeEmbedUrl = function(url) {
  var m = url.match(/(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?\/ ]{11})(?:\?|&|)(\S*)/i);
  if (!m) return;
  var start = 1;
  m[2].split('&').forEach(function(p) {
    var s = p.split('=');
    if (s[0] === 't' || s[0] === 'start') {
      if (s[1].match(/^\d+$/)) start = parseInt(s[1]);
      else {
        var n = s[1].match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/);
        start = (parseInt(n[1]) || 0) * 3600 + (parseInt(n[2]) || 0) * 60 + (parseInt(n[3]) || 0);
      }
    }
  });
  var params = 'modestbranding=1&rel=0&controls=2&iv_load_policy=3&start=' + start;
  return 'https://www.youtube.com/embed/' + m[1] + '?' + params;
};
$.spreadNumber = function(el, nbSteps, getDuration, previous) {
  var previous = previous,
    displayed;
  var display = function(prev, cur, it) {
    var val = lichess.numberFormat(Math.round(((prev * (nbSteps - 1 - it)) + (cur * (it + 1))) / nbSteps));
    if (val !== displayed) {
      el.textContent = val;
      displayed = val;
    }
  };
  var timeouts = [];
  return function(nb, overrideNbSteps) {
    if (!el || (!nb && nb !== 0)) return;
    if (overrideNbSteps) nbSteps = Math.abs(overrideNbSteps);
    timeouts.forEach(clearTimeout);
    timeouts = [];
    var prev = previous === 0 ? 0 : (previous || nb);
    previous = nb;
    var interv = Math.abs(getDuration() / nbSteps);
    for (var i = 0; i < nbSteps; i++)
      timeouts.push(setTimeout(display.bind(null, prev, nb, i), Math.round(i * interv)));
  };
};
$.fn.scrollTo = function(target, offsetTop) {
  return this.each(function() {
    try {
      var t = (typeof target === "number") ? target : $(target);
      var v = (typeof t === "number") ? t : t.offset().top + this.scrollTop - (offsetTop || 0);
      this.scrollTop = v;
    } catch (e) {}
  });
};
$.modal = function(html) {
  if (!html.clone) html = $('<div>' + html + '</div>');
  var $wrap = $('<div id="modal-wrap">').html(html.clone().removeClass('none').show()).prepend('<span class="close" data-icon="L"></span>');
  var $overlay = $('<div id="modal-overlay">').html($wrap);
  $overlay.add($wrap.find('.close')).one('click', $.modal.close);
  $wrap.click(function(e) {
    e.stopPropagation();
  });
  $('body').prepend($overlay);
  return $wrap;
};
$.modal.close = function() {
  $('#modal-overlay').remove();
};

// polyfills

if (!Array.prototype.find) {
  Object.defineProperty(Array.prototype, 'find', {
    value: function(predicate) {
      for (var i in this) if (predicate(this[i])) return this[i];
    }
  });
}
