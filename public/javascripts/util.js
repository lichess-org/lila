// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

var lichess = window.lichess = window.lichess || {};

lichess.getParameterByName = function(name) {
  var match = RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
  return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
};

// declare now, populate later in a distinct script.
var lichess_translations = lichess_translations || [];

lichess.raf = (window.requestAnimationFrame || window.setTimeout).bind(window);
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
    remove: function(k) {
      withStorage(function(s) {
        s.removeItem(k);
      });
    },
    set: function(k, v) {
      // removing first may help http://stackoverflow.com/questions/2603682/is-anyone-else-receiving-a-quota-exceeded-err-on-their-ipad-when-accessing-local
      withStorage(function(s) {
        s.removeItem(k);
        s.setItem(k, v);
      });
    }
  };
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

  var userPowertip = function(el) {
    var pos = 'w';
    if (elementIdContains('site_header', el)) pos = 'e';
    if (elementIdContains('friend_box', el)) pos = 'nw';
    $(el).removeClass('ulpt').powerTip({
      intentPollInterval: 200,
      fadeInTime: 100,
      fadeOutTime: 100,
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
      fadeInTime: 100,
      fadeOutTime: 100,
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
    manualGame: function(el) {
      Array.prototype.forEach.call(el.querySelectorAll('.glpt'), gamePowertip);
    }
  };
})();
lichess.trans = function(i18n) {
  var trans = function(key) {
    var str = i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
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
lichess.isSafari = navigator.userAgent.indexOf('Safari/') > -1 && !lichess.isChrome;
lichess.spinnerHtml = '<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>';
lichess.assetUrl = function(url, noVersion) {
  return $('body').data('asset-url') + url + (noVersion ? '' : '?v=' + $('body').data('asset-version'));
};
lichess.loadCss = function(url) {
  $('head').append($('<link rel="stylesheet" type="text/css" />').attr('href', lichess.assetUrl(url)));
}
lichess.loadScript = function(url, noVersion) {
  return $.ajax({
    dataType: "script",
    cache: true,
    url: lichess.assetUrl(url, noVersion)
  });
};
lichess.hopscotch = function(f) {
  lichess.loadCss('/assets/vendor/hopscotch/dist/css/hopscotch.min.css');
  lichess.loadScript("/assets/vendor/hopscotch/dist/js/hopscotch.min.js").done(f);
}
lichess.slider = function() {
  return lichess.loadScript('/assets/javascripts/vendor/jquery-ui.slider.min.js', true);
};
lichess.shepherd = function(f) {
  var theme = 'shepherd-theme-' + ($('body').hasClass('dark') ? 'default' : 'dark');
  lichess.loadCss('/assets/vendor/shepherd/dist/css/' + theme + '.css');
  lichess.loadCss('/assets/stylesheets/shepherd.css');
  lichess.loadScript("/assets/vendor/shepherd/dist/js/tether.js").done(function() {
    lichess.loadScript("/assets/vendor/shepherd/dist/js/shepherd.min.js").done(function() {
      f(theme);
    });
  });
};
lichess.makeChat = function(id, data, callback) {
  var isDev = $('body').data('dev');
  lichess.loadCss('/assets/stylesheets/chat.css');
  if (data.permissions.timeout) lichess.loadCss('/assets/stylesheets/chat.mod.css');
  lichess.loadScript("/assets/compiled/lichess.chat" + (isDev ? '' : '.min') + '.js').done(function() {
    (callback || $.noop)(LichessChat(document.getElementById(id), data));
  });
};

lichess.desktopNotification = (function() {
  var notifications = [];
  var isPageVisible = document.visibilityState !== 'hidden';
  window.addEventListener('blur', function() {
    isPageVisible = false;
  });
  // using document.hidden doesn't entirely work because it may return false if the window is not minimized but covered by other applications
  window.addEventListener('focus', function() {
    isPageVisible = true;
    notifications.forEach(function(n) {
      n.close();
    });
    notifications = [];
  });
  var storageKey = 'just-notified';
  var clearStorageSoon = function() {
    setTimeout(function() {
      lichess.storage.remove(storageKey);
    }, 3000);
  };
  var doNotify = function(msg) {
    if (lichess.storage.get(storageKey)) return;
    lichess.storage.set(storageKey, 1);
    clearStorageSoon();
    var notification = new Notification('lichess.org', {
      icon: '//lichess1.org/assets/images/logo.256.png',
      body: msg
    });
    notification.onclick = function() {
      window.focus();
    }
    notifications.push(notification);
  };
  var notify = function(msg) {
    // increase chances that the first tab can put a local storage lock
    setTimeout(function() {
      doNotify(msg);
    }, Math.round(10 + Math.random() * 500));
  }
  clearStorageSoon(); // in case it wasn't cleared properly before
  return function(msg) {
    if (isPageVisible || !('Notification' in window) || Notification.permission === 'denied') return;
    if (Notification.permission === 'granted') notify(msg);
    else Notification.requestPermission(function(p) {
      if (p === 'granted') notify(msg);
    });
  };
})();
lichess.numberFormat = (function() {
  if (window.Intl && Intl.NumberFormat) {
    var formatter = new Intl.NumberFormat();
    return function(n) {
      return formatter.format(n);
    }
  }
  return function(n) {
    return n;
  };
})();
lichess.idleTimer = function(delay, onIdle, onWakeUp) {
  var events = ['mousemove', 'touchstart'];
  var listening = false;
  var active = true;
  var lastSeenActive = new Date();
  var onActivity = function() {
    if (!active) {
      console.log('Wake up');
      onWakeUp();
    }
    active = true;
    lastSeenActive = new Date();
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
    if (active && new Date() - lastSeenActive > delay) {
      console.log('Idle mode');
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
  var $wrap = $('<div id="modal-wrap">').html(html.clone().show()).prepend('<a class="close" data-icon="L"></a>');
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
