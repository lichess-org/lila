// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

var lichess = window.lichess = window.lichess || {};

lichess.StrongSocket = function(url, version, settings) {
  var self = this;
  self.settings = $.extend(true, {}, lichess.StrongSocket.defaults, settings);
  self.url = url;
  self.version = version;
  self.options = self.settings.options;
  self.ws = null;
  self.pingSchedule = null;
  self.connectSchedule = null;
  self.ackableMessages = [];
  self.lastPingTime = self.now();
  self.currentLag = 0;
  self.averageLag = 0;
  self.tryOtherUrl = false;
  self.autoReconnect = true;
  self.debug('Debug is enabled');
  if (self.options.resetUrl || self.options.prodPipe) {
    lichess.storage.remove(self.options.baseUrlKey);
  }
  if (self.options.prodPipe) {
    self.options.baseUrls = ['socket.en.lichess.org:9021'];
  }
  self.connect();
  window.addEventListener('unload', function() {
    self.destroy();
  });
};
lichess.StrongSocket.available = window.WebSocket || window.MozWebSocket;
lichess.StrongSocket.sri = Math.random().toString(36).substring(2);
lichess.StrongSocket.defaults = {
  events: {
    fen: function(e) {
      $('.live_' + e.id).each(function() {
        lichess.parseFen($(this).data("fen", e.fen).data("lastmove", e.lm));
      });
    }
  },
  params: {
    sri: lichess.StrongSocket.sri
  },
  options: {
    name: "unnamed",
    pingMaxLag: 7000, // time to wait for pong before reseting the connection
    pingDelay: 1000, // time between pong and ping
    autoReconnectDelay: 1000,
    lagTag: false, // jQuery object showing ping lag
    ignoreUnknownMessages: false,
    baseUrls: ['socket.' + document.domain].concat(
      ($('body').data('ports') + '').split(',').map(function(port) {
        return 'socket.' + document.domain + ':' + port;
      })),
    baseUrlKey: 'surl3'
  }
};
lichess.StrongSocket.prototype = {
  connect: function() {
    var self = this;
    self.destroy();
    self.autoReconnect = true;
    var fullUrl = "ws://" + self.baseUrl() + self.url + "?" + $.param($.extend(self.settings.params, {
      version: self.version
    }));
    self.debug("connection attempt to " + fullUrl, true);
    try {
      if (window.MozWebSocket) self.ws = new MozWebSocket(fullUrl);
      else if (window.WebSocket) self.ws = new WebSocket(fullUrl);
      else throw "[lila] no websockets found on this browser!";

      self.ws.onerror = function(e) {
        self.onError(e);
      };
      self.ws.onclose = function(e) {
        if (self.autoReconnect) {
          self.debug('Will autoreconnect in ' + self.options.autoReconnectDelay);
          self.scheduleConnect(self.options.autoReconnectDelay);
        }
      };
      self.ws.onopen = function() {
        self.debug("connected to " + fullUrl, true);
        self.onSuccess();
        $('body').removeClass('offline');
        self.pingNow();
        $('body').trigger('socket.open');
        if ($('#user_tag').length) setTimeout(function() {
          self.send("following_onlines");
        }, 500);
        var resend = self.ackableMessages;
        self.ackableMessages = [];
        resend.forEach(function(x) {
          self.send(x.t, x.d);
        });
      };
      self.ws.onmessage = function(e) {
        var m = JSON.parse(e.data);
        if (m.t == "n") {
          self.pong();
        } else self.debug(e.data);
        if (m.t == "b") {
          m.d.forEach(function(mm) {
            self.handle(mm);
          });
        } else self.handle(m);
      };
    } catch (e) {
      self.onError(e);
    }
    self.scheduleConnect(self.options.pingMaxLag);
  },
  send: function(t, d, o) {
    var self = this;
    var data = d || {},
      options = o || {};
    if (options && options.ackable)
      self.ackableMessages.push({
        t: t,
        d: d
      });
    var message = JSON.stringify({
      t: t,
      d: data
    });
    self.debug("send " + message);
    try {
      self.ws.send(message);
    } catch (e) {
      self.debug(e);
    }
  },
  sendAckable: function(t, d) {
    this.send(t, d, {
      ackable: true
    });
  },
  scheduleConnect: function(delay) {
    var self = this;
    // self.debug('schedule connect ' + delay);
    clearTimeout(self.pingSchedule);
    clearTimeout(self.connectSchedule);
    self.connectSchedule = setTimeout(function() {
      $('body').addClass('offline');
      self.tryOtherUrl = true;
      self.connect();
    }, delay);
  },
  schedulePing: function(delay) {
    var self = this;
    clearTimeout(self.pingSchedule);
    self.pingSchedule = setTimeout(function() {
      self.pingNow();
    }, delay);
  },
  pingNow: function() {
    var self = this;
    clearTimeout(self.pingSchedule);
    clearTimeout(self.connectSchedule);
    try {
      self.ws.send(self.pingData());
      self.lastPingTime = self.now();
    } catch (e) {
      self.debug(e, true);
    }
    self.scheduleConnect(self.options.pingMaxLag);
  },
  pong: function() {
    var self = this;
    clearTimeout(self.connectSchedule);
    self.schedulePing(self.options.pingDelay);
    self.currentLag = self.now() - self.lastPingTime;
    if (!self.averageLag) self.averageLag = self.currentLag;
    else self.averageLag = 0.2 * (self.currentLag - self.averageLag) + self.averageLag;
    if (self.options.lagTag) {
      self.options.lagTag.html(Math.round(self.averageLag));
    }
  },
  pingData: function() {
    return JSON.stringify({
      t: "p",
      v: this.version
    });
  },
  handle: function(m) {
    var self = this;
    if (m.v) {
      if (m.v <= self.version) {
        self.debug("already has event " + m.v);
        return;
      }
      if (m.v > self.version + 1) {
        self.debug("event gap detected from " + self.version + " to " + m.v);
        if (!self.options.prodPipe) return;
      }
      self.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        if (!self.options.prodPipe) lichess.reload();
        break;
      case 'ack':
        self.ackableMessages = [];
        break;
      default:
        if (self.settings.receive) self.settings.receive(m.t, m.d);
        var h = self.settings.events[m.t];
        if (h) h(m.d || null);
        else if (!self.options.ignoreUnknownMessages) {
          self.debug('Message not supported ' + JSON.stringify(m));
        }
    }
  },
  now: function() {
    return new Date().getTime();
  },
  debug: function(msg, always) {
    if ((always || this.options.debug) && window.console && console.debug) {
      console.debug("[" + this.options.name + " " + this.settings.params.sri + "]", msg);
    }
  },
  destroy: function() {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = null;
  },
  disconnect: function() {
    if (this.ws) {
      this.debug("Disconnect", true);
      this.autoReconnect = false;
      this.ws.onerror = $.noop();
      this.ws.onclose = $.noop();
      this.ws.onopen = $.noop();
      this.ws.onmessage = $.noop();
      this.ws.close();
    }
  },
  onError: function(e) {
    var self = this;
    self.options.debug = true;
    self.debug('error: ' + JSON.stringify(e));
    self.tryOtherUrl = true;
    setTimeout(function() {
      if (!$('#network_error').length) {
        var msg = "Your browser supports websockets, but cannot get a connection. Maybe you are behind a proxy that does not support websockets. Ask your system administrator to fix it!";
        $('#nb_connected_players').after('<span id="network_error" title="' + msg + '" data-icon="j"> Network error</span>');
      }
    }, 1000);
    clearTimeout(self.pingSchedule);
  },
  onSuccess: function() {
    $('#network_error').remove();
  },
  baseUrl: function() {
    var key = this.options.baseUrlKey;
    var urls = this.options.baseUrls;
    var url = lichess.storage.get(key);
    if (!url) {
      url = urls[0];
      lichess.storage.set(key, url);
    } else if (this.tryOtherUrl) {
      this.tryOtherUrl = false;
      url = urls[(urls.indexOf(url) + 1) % urls.length];
      lichess.storage.set(key, url);
    }
    return url;
  },
  pingInterval: function() {
    return this.options.pingDelay + this.averageLag;
  }
};

// declare now, populate later in a distinct script.
var lichess_translations = lichess_translations || [];

function withStorage(f) {
  // can throw an exception when storage is full
  try {
    return !!window.localStorage ? f(window.localStorage) : null;
  } catch (e) {}
}
lichess.storage = {
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

(function() {

  /////////////
  // ctrl.js //
  /////////////

  $.userLink = function(u) {
    return $.userLinkLimit(u, false);
  };
  $.userLinkLimit = function(u, limit, klass) {
    var split = u.split(' ');
    var id = split.length == 1 ? split[0] : split[1];
    return (u || false) ? '<a class="user_link ulpt ' + (klass || '') + '" href="/@/' + id + '">' + ((limit || false) ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
  };
  $.redirect = function(obj) {
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
    location.href = 'http://' + location.hostname + '/' + url.replace(/^\//, '');
  };
  $.fp = {};
  $.fp.range = function(to) {
    return Array.apply(null, Array(to)).map(function(_, i) {
      return i;
    });
  };
  $.fp.contains = function(list, needle) {
    return list.indexOf(needle) !== -1;
  };
  $.fp.find = function(list, pred) {
    for (var i = 0, len = list.length; i < len; i++) {
      if (pred(list[i])) return list[i];
    }
    return undefined;
  };
  $.fp.debounce = function(func, wait, immediate) {
    var timeout;
    return function() {
      var context = this,
        args = arguments;
      var later = function() {
        timeout = null;
        if (!immediate) func.apply(context, args);
      };
      var callNow = immediate && !timeout;
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
      if (callNow) func.apply(context, args);
    };
  };

  var nbEl = document.querySelector('#nb_connected_players > strong');
  lichess.socket = null;
  lichess.idleTime = 20 * 60 * 1000;
  $.extend(true, lichess.StrongSocket.defaults, {
    events: {
      following_onlines: function(us) {
        $('#friend_box').friends("set", us);
      },
      following_enters: function(name) {
        $('#friend_box').friends('enters', name);
      },
      following_leaves: function(name) {
        $('#friend_box').friends('leaves', name);
      },
      n: function(e) {
        if (nbEl && e) {
          var prev = parseInt(nbEl.textContent, 10) || Math.max(0, (e - 10));
          var k = 5;
          var interv = lichess.socket.pingInterval() / k;
          $.fp.range(k).forEach(function(it) {
            setTimeout(function() {
              var val = Math.round(((prev * (k - 1 - it)) + (e * (it + 1))) / k);
              if (val != prev) {
                nbEl.textContent = val;
                prev = val;
              }
            }, Math.round(it * interv));
          });
        }
      },
      message: function(msg) {
        $('#chat').chat("append", msg);
      },
      nbm: function(e) {
        $('#nb_messages').text(e || "0").toggleClass("unread", e > 0);
      },
      redirect: function(o) {
        setTimeout(function() {
          lichess.hasToReload = true;
          $.redirect(o);
        }, 300);
      },
      tournamentReminder: function(data) {
        if (!$('#tournament_reminder').length && $('body').data("tournament-id") != data.id) {
          $('#notifications').append(data.html).find("a.withdraw").click(function() {
            $.post($(this).attr("href"));
            $('#tournament_reminder').remove();
            return false;
          });
          $('body').trigger('lichess.content_loaded');
        }
      },
      challengeReminder: function(data) {
        if (!lichess.storage.get('challenge-refused-' + data.id)) {
          var refreshButton = function() {
            var nb = $('#challenge_notifications > div').length;
            $('#nb_challenges').text(nb);
            $('#challenge_notifications_tag').toggleClass('none', !nb);
          };
          var htmlId = 'challenge_reminder_' + data.id;
          var $notif = $('#' + htmlId);
          var declineListener = function($a, callback) {
            return $a.click(function() {
              $.post($(this).attr("href"));
              lichess.storage.set('challenge-refused-' + data.id, 1);
              $('#' + htmlId).remove();
              if ($.isFunction(callback)) callback();
              refreshButton();
              return false;
            });
          };
          if ($notif.length) clearTimeout($notif.data('timeout'));
          else {
            $('#challenge_notifications').append(data.html);
            $notif = $('#' + htmlId);
            $notif.find('> a').click(function() {
              lichess.hasToReload = true; // allow quit by accept challenge (simul)
            });
            declineListener($notif.find('a.decline'));
            $('body').trigger('lichess.content_loaded');
            if (!lichess.storage.get('challenge-' + data.id)) {
              if (!lichess.quietMode) {
                $('#top .challenge_notifications').addClass('shown');
                $.sound.dong();
              }
              lichess.storage.set('challenge-' + data.id, 1);
            }
            refreshButton();
          }
          $('.lichess_overboard.joining.' + data.id).each(function() {
            if (!$(this).find('a.decline').length) $(this).find('form').append(
              declineListener($(data.html).find('a.decline').text($.trans('decline')), function() {
                location.href = "/";
              })
            );
          });
          $notif.data('timeout', setTimeout(function() {
            $notif.remove();
            refreshButton();
          }, 3000));
        }
      },
      deployPre: function(html) {
        $('#notifications').append(html);
        setTimeout(function() {
          $('#deploy_pre').fadeOut(1000).remove();
        }, 10000);
      },
      deployPost: function(html) {
        $('#notifications').append(html);
        setTimeout(function() {
          $('#deploy_post').fadeOut(1000).remove();
        }, 10000);
        lichess.socket.disconnect();
      }
    },
    params: {},
    options: {
      name: "site",
      lagTag: $('#top .ping strong'),
      debug: location.search.indexOf('debug-ws') != -1,
      prodPipe: location.search.indexOf('prod-ws') != -1,
      resetUrl: location.search.indexOf('reset-ws') != -1
    }
  });

  lichess.hasToReload = false;
  lichess.reload = function() {
    lichess.hasToReload = true;
    location.reload();
  };

  lichess.parseFen = function($elem) {
    if (!$elem || !$elem.jquery) {
      $elem = $('.parse_fen');
    }
    $elem.each(function() {
      var $this = $(this).removeClass('parse_fen');
      var lm = $this.data('lastmove');
      var lastMove = lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [];
      var color = $this.data('color');
      var ground = $this.data('chessground');
      var playable = $this.data('playable');
      var config = {
        coordinates: false,
        viewOnly: !playable,
        minimalDom: !playable,
        fen: $this.data('fen'),
        lastMove: lm ? [lm[0] + lm[1], lm[2] + lm[3]] : null
      };
      if (color) config.orientation = color;
      if (ground) ground.set(config);
      else $this.data('chessground', Chessground($this[0], config));
    });
  }

  $(function() {

    // small layout
    function onResize() {
      if ($(document.body).width() < 1000) {
        $(document.body).addClass("tight");
        $('#site_header .side_menu').prependTo('div.content_box:first');
      } else {
        $(document.body).removeClass("tight");
        $('#timeline, div.side_box, div.under_chat').each(function() {
          var ol = $(this).offset().left;
          if (ol < 3) {
            var dec = 3 - ol;
            var pad = $(this).outerWidth() - $(this).width();
            $(this).css({
              'margin-left': (dec - 30) + 'px',
              'width': (230 - pad - dec) + 'px'
            });
            $(this).find('input.lichess_say').css('width', (204 - dec) + 'px');
          }
        });
        $('#featured_game').each(function() {
          $(this).children().toggle($(this).width() >= 220);
        });
        $('div.content_box .side_menu').appendTo('#site_header');
      }
    }
    $(window).resize(onResize);
    onResize();

    if (!lichess.StrongSocket.available) {
      $('#lichess').on('mouseover', function() {
        $('#lichess').off('mouseover');
        var inUrFaceUrl = window.opera ? '/assets/opera-websocket.html' : '/assets/browser.html';
        $.ajax(inUrFaceUrl, {
          success: function(html) {
            $('body').prepend(html);
          }
        });
      });
    }

    if (lichess.round) startRound(document.getElementById('lichess'), lichess.round);
    else if (lichess.prelude) startPrelude(document.querySelector('.lichess_game'), lichess.prelude);
    else if (lichess.analyse) startAnalyse(document.getElementById('lichess'), lichess.analyse);
    else if (lichess.user_analysis) startUserAnalysis(document.getElementById('lichess'), lichess.user_analysis);
    else if (lichess.lobby) startLobby(document.getElementById('hooks_wrap'), lichess.lobby);
    else if (lichess.tournament) startTournament(document.getElementById('tournament'), lichess.tournament);

    $('#lichess').on('click', '.socket-link:not(.disabled)', function() {
      lichess.socket.send($(this).data('msg'), $(this).data('data'));
    });

    $('#friend_box').friends();

    $('#lichess').on('click', '.copyable', function() {
      $(this).select();
    });

    $('body').on('click', '.relation_actions a.relation', function() {
      var $a = $(this).addClass('processing');
      $.ajax({
        url: $a.attr('href'),
        type: 'post',
        success: function(html) {
          $a.parent().html(html);
        }
      });
      return false;
    });

    $('body').on('click', '.game_row', function() {
      location.href = $(this).find('a.mini_board').attr('href');
    });

    function userPowertips() {
      var header = document.getElementById('site_header');
      $('.ulpt').removeClass('ulpt').each(function() {
        $(this).powerTip({
          fadeInTime: 100,
          fadeOutTime: 100,
          placement: $(this).data('placement') || ($.contains(header, this) ? 'e' : 'w'),
          mouseOnToPopup: true,
          closeDelay: 200
        }).on({
          powerTipPreRender: function() {
            $.ajax({
              url: ($(this).attr('href') || $(this).data('href')).replace(/\?.+$/, '') + '/mini',
              success: function(html) {
                $('#powerTip').html(html);
                $('body').trigger('lichess.content_loaded');
              }
            });
          }
        }).data('powertip', ' ');
      });
    }
    setTimeout(userPowertips, 600);
    $('body').on('lichess.content_loaded', userPowertips);

    $('#message_notifications_tag').on('click', function() {
      $.ajax({
        url: $(this).data('href'),
        cache: false,
        success: function(html) {
          $('#message_notifications_display').html(html)
            .find('a.mark_as_read').click(function() {
              $.ajax({
                url: $(this).attr('href'),
                method: 'post'
              });
              $(this).parents('.notification').remove();
              if ($('#message_notifications_display').children().length === 0)
                $('#message_notifications_tag').click();
              return false;
            });
          $('body').trigger('lichess.content_loaded');
        }
      });
    });

    function setMoment() {
      $("time.moment").removeClass('moment').each(function() {
        var parsed = moment(this.getAttribute('datetime'));
        var format = this.getAttribute('data-format');
        this.textContent = format == 'calendar' ? parsed.calendar() : parsed.format(format);
      });
    }
    setMoment();
    $('body').on('lichess.content_loaded', setMoment);

    function setMomentFromNow() {
      $("time.moment-from-now").each(function() {
        this.textContent = moment(this.getAttribute('datetime')).fromNow();
      });
    }
    setMomentFromNow();
    $('body').on('lichess.content_loaded', setMomentFromNow);
    setInterval(setMomentFromNow, 2000);

    if ($('body').hasClass('blind_mode')) {
      var setBlindMode = function() {
        $('[data-hint]').each(function() {
          $(this).attr('aria-label', $(this).data('hint'));
        });
      };
      setBlindMode();
      $('body').on('lichess.content_loaded', setBlindMode);
    }

    setTimeout(function() {
      if (lichess.socket === null) {
        lichess.socket = new lichess.StrongSocket("/socket", 0);
      }
      $.idleTimer(lichess.idleTime, lichess.socket.destroy.bind(lichess.socket), lichess.socket.connect.bind(lichess.socket));
    }, 200);

    // themepicker
    $('#themepicker_toggle').one('mouseover', function() {
      var $themepicker = $('#themepicker');
      $.ajax({
        url: $(this).data('url'),
        cache: false,
        success: function(html) {
          $themepicker.append(html);
          var $body = $('body');
          var $content = $body.children('.content');
          var $dropdown = $themepicker.find('.dropdown');
          var themes = $dropdown.data('themes').split(' ');
          var theme = $.fp.find(document.body.classList, function(a) {
            return $.fp.contains(themes, a);
          });
          var sets = $dropdown.data('sets').split(' ');
          var set = $.fp.find(document.body.classList, function(a) {
            return $.fp.contains(sets, a);
          });
          var theme3ds = $dropdown.data('theme3ds').split(' ');
          var theme3d = $.fp.find(document.body.classList, function(a) {
            return $.fp.contains(theme3ds, a);
          });
          var set3ds = $dropdown.data('set3ds').split(' ');
          var set3d = $.fp.find(document.body.classList, function(a) {
            return $.fp.contains(set3ds, a);
          });
          var background = $body.hasClass('dark') ? 'dark' : 'light';
          var is3d = $content.hasClass('is3d');
          $themepicker.find('.is2d div.theme').hover(function() {
            $body.removeClass(themes.join(' ')).addClass($(this).data("theme"));
          }, function() {
            $body.removeClass(themes.join(' ')).addClass(theme);
          }).click(function() {
            theme = $(this).data("theme");
            $.post($(this).parent().data("href"), {
              theme: theme
            });
            $themepicker.removeClass("shown");
          });
          $themepicker.find('.is2d div.no-square').hover(function() {
            $body.removeClass(sets.join(' ')).addClass($(this).data("set"));
          }, function() {
            $body.removeClass(sets.join(' ')).addClass(set);
          }).click(function() {
            set = $(this).data("set");
            $.post($(this).parent().data("href"), {
              set: set
            });
            $themepicker.removeClass("shown");
          });
          $themepicker.find('.is3d div.theme').hover(function() {
            $body.removeClass(theme3ds.join(' ')).addClass($(this).data("theme"));
          }, function() {
            $body.removeClass(theme3ds.join(' ')).addClass(theme3d);
          }).click(function() {
            theme3d = $(this).data("theme");
            $.post($(this).parent().data("href"), {
              theme: theme3d
            });
            $themepicker.removeClass("shown");
          });
          $themepicker.find('.is3d div.no-square').hover(function() {
            $body.removeClass(set3ds.join(' ')).addClass($(this).data("set"));
          }, function() {
            $body.removeClass(set3ds.join(' ')).addClass(set3d);
          }).click(function() {
            set3d = $(this).data("set");
            $.post($(this).parent().data("href"), {
              set: set3d
            });
            $themepicker.removeClass("shown");
          });
          var showBg = function(bg) {
            $body.removeClass('light dark').addClass(bg);
            if (bg == 'dark' && $('link[href*="dark.css"]').length === 0) {
              $('link[href*="common.css"]').clone().each(function() {
                $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'dark.css')).appendTo('head');
              });
            }
          };
          var showDimensions = function(is3d) {
            $content.add('#top').removeClass('is2d is3d').addClass(is3d ? 'is3d' : 'is2d');
            setZoom(getZoom());
          };
          $themepicker.find('.background a').click(function() {
            background = $(this).data('bg');
            $.post($(this).parent().data('href'), {
              bg: background
            });
            $(this).addClass('active').siblings().removeClass('active');
            $themepicker.removeClass("shown");
            return false;
          }).hover(function() {
            showBg($(this).data('bg'));
          }, function() {
            showBg(background);
          }).filter('.' + background).addClass('active');
          $themepicker.find('.dimensions a').click(function() {
            is3d = $(this).data('is3d');
            $.post($(this).parent().data('href'), {
              is3d: is3d
            });
            $(this).addClass('active').siblings().removeClass('active');
            $themepicker.removeClass("shown");
            return false;
          }).hover(function() {
            showDimensions($(this).data('is3d'));
          }, function() {
            showDimensions(is3d);
          }).filter('.' + (is3d ? 'd3' : 'd2')).addClass('active');
          $themepicker.find('.slider').slider({
            orientation: "horizontal",
            min: 1,
            max: 2,
            range: 'min',
            step: 0.01,
            value: getZoom(),
            slide: function(e, ui) {
              manuallySetZoom(ui.value);
            }
          });
        }
      });
    });

    // Zoom
    var getZoom = function() {
      return lichess.storage.get('zoom') || 1;
    };
    var setZoom = function(v) {
      lichess.storage.set('zoom', v);

      var $boardWrap = $(".lichess_game .cg-board-wrap");
      var $lichessGame = $(".lichess_game");
      var px = function(i) {
        return Math.round(i) + 'px';
      };

      $boardWrap.css("width", px(512 * getZoom()));
      $('.underboard').css("margin-left", px((getZoom() - 1) * 250));
      $('.lichess_game > .lichess_overboard').css("left", px(56 + (getZoom() - 1) * 254));

      if ($('body > .content').hasClass('is3d')) {
        $boardWrap.css("height", px(479.08572 * getZoom()));
        $lichessGame.css({
          height: px(479.08572 * getZoom()),
          paddingTop: px(50 * (getZoom() - 1))
        });
        $('#tv_history > .content').css("height", px(250 + 540 * (getZoom() - 1)));
        $('.chat_panels').css("height", px(290 + 529 * (getZoom() - 1)));
      } else {
        $boardWrap.css("height", px(512 * getZoom()));
        $lichessGame.css({
          height: px(512 * getZoom()),
          paddingTop: px(0)
        });
        $('#tv_history > .content').css("height", px(270 + 525 * (getZoom() - 1)));
        $('.chat_panels').css("height", px(325 + 510 * (getZoom() - 1)));
      }

      if ($lichessGame.length) {
        // if on a board with a game
        $('body > .content').css("margin-left", 'calc(50% - ' + px(246.5 + 256 * getZoom()) + ')');
      }
    };

    var manuallySetZoom = $.fp.debounce(setZoom, 10);
    if (getZoom() > 1) setZoom(getZoom()); // Instantiate the page's zoom

    function translateTexts() {
      $('.trans_me').each(function() {
        $(this).removeClass('trans_me');
        if ($(this).val()) $(this).val($.trans($(this).val()));
        else $(this).text($.trans($(this).text()));
      });
    }
    translateTexts();
    $('body').on('lichess.content_loaded', translateTexts);

    $('input.autocomplete').each(function() {
      var $a = $(this);
      $a.autocomplete({
        source: $a.data('provider'),
        minLength: 2,
        delay: 100
      });
    });

    $('.infinitescroll:has(.pager a)').each(function() {
      $(this).infinitescroll({
        navSelector: ".pager",
        nextSelector: ".pager a:last",
        itemSelector: ".infinitescroll .paginated_element",
        errorCallback: function() {
          $("#infscr-loading").remove();
        }
      }, function() {
        $("#infscr-loading").remove();
        $('body').trigger('lichess.content_loaded');
      }).find('div.pager').hide();
    });

    $('#top a.toggle').each(function() {
      var $this = $(this);
      var $p = $this.parent();
      $this.click(function() {
        $p.toggleClass('shown');
        $p.siblings('.shown').removeClass('shown');
        setTimeout(function() {
          $p.click(function(e) {
            e.stopPropagation();
          });
          $('html').one('click', function(e) {
            $p.removeClass('shown').off('click');
          });
        }, 10);
        return false;
      });
    });

    var acceptLanguages = $('body').data('accept-languages');
    if (acceptLanguages) {
      $('#top .lichess_language').one('mouseover', function() {
        var $links = $(this).find('.language_links'),
          langs = acceptLanguages.split(',');
        $.ajax({
          url: $links.data('url'),
          cache: false,
          success: function(list) {
            $links.prepend(list.map(function(lang) {
              var klass = $.fp.contains(langs, lang[0]) ? 'class="accepted"' : '';
              return '<li><button type="submit" ' + klass + '" name="lang" value="' + lang[0] + '">' + lang[1] + '</button></li>';
            }).join(''));
          }
        });
      });
    }

    $('#incomplete_translation a.close').one('click', function() {
      $(this).parent().remove();
    });

    $('#translation_call .close').click(function() {
      $.post($(this).data("href"));
      $(this).parent().fadeOut(500);
      return false;
    });

    $('a.delete, input.delete').click(function() {
      return confirm('Delete?');
    });
    $('input.confirm, button.confirm').click(function() {
      return confirm('Confirm this action?');
    });

    $('div.content').on('click', 'a.bookmark', function() {
      var t = $(this).toggleClass("bookmarked");
      $.post(t.attr("href"));
      var count = (parseInt(t.text(), 10) || 0) + (t.hasClass("bookmarked") ? 1 : -1);
      t.find('span').html(count > 0 ? ' ' + count : "");
      return false;
    });

    $("#import_game form").submit(function() {
      var pgn = $(this).find('textarea').val();
      var nbMoves = parseInt(pgn.replace(/\n/g, ' ').replace(/^.+\s(\d+)\..+$/, '$1'), 10);
      var delay = 50;
      var duration = nbMoves * delay * 2.1 + 1000;
      $(this).find('button').hide().end()
        .find('.error').hide().end()
        .find('.progression').show().animate({
          width: '100%'
        }, duration);
      return true;
    });

    setInterval(function() {
      $('.glowing').toggleClass('glow');
    }, 1500);
  });

  $.lazy = function(factory) {
    var loaded = {};
    return function(key) {
      if (!loaded[key]) loaded[key] = factory(key);
      return loaded[key];
    };
  };

  $.sound = (function() {
    var baseUrl = $('body').data('sound-dir') + '/';
    var a = new Audio();
    var hasOgg = !!a.canPlayType && a.canPlayType('audio/ogg; codecs="vorbis"');
    var hasMp3 = !!a.canPlayType && a.canPlayType('audio/mpeg;');
    var ext = hasOgg ? 'ogg' : 'mp3';
    var names = {
      dong: 'dong2',
      moveW: 'move3',
      moveB: 'move3',
      take: 'take2',
      lowtime: 'lowtime'
    };
    var volumes = {
      lowtime: 0.5
    };
    var computeVolume = function(k, v) {
      return v * (volumes[k] || 1);
    };
    var get = new $.lazy(function(k) {
      var audio = new Audio(baseUrl + names[k] + '.' + ext);
      audio.volume = computeVolume(k, getVolume());
      return audio;
    });
    var canPlay = hasOgg || hasMp3;
    var $control = $('#sound_control');
    var $toggle = $('#sound_state');
    $control.add($toggle).toggleClass('sound_state_on', lichess.storage.get('sound') == 1);
    var enabled = function() {
      return $toggle.hasClass("sound_state_on");
    };
    var shouldPlay = function() {
      return canPlay && enabled();
    };
    var play = {
      move: function(white) {
        if (shouldPlay()) {
          if (white) get('moveW').play();
          else get('moveB').play();
        }
      },
      take: function() {
        if (shouldPlay()) get('take').play();
      },
      dong: function() {
        if (shouldPlay()) get('dong').play();
      },
      lowtime: function() {
        if (shouldPlay()) get('lowtime').play();
      }
    };
    var getVolume = function() {
      return lichess.storage.get('sound-volume') || 0.8;
    };
    var setVolume = function(v) {
      lichess.storage.set('sound-volume', v);
      Object.keys(names).forEach(function(k) {
        get(k).volume = computeVolume(k, v);
      });
    };
    var manuallySetVolume = $.fp.debounce(function(v) {
      setVolume(v);
      play.move(true);
    }, 100);
    if (canPlay) {
      $toggle.click(function() {
        $control.add($toggle).toggleClass('sound_state_on', !enabled());
        if (enabled()) lichess.storage.set('sound', 1);
        else lichess.storage.remove('sound');
        play.dong();
        return false;
      });
      $toggle.one('mouseover', function() {
        $toggle.parent().find('.slider').slider({
          orientation: "vertical",
          min: 0,
          max: 1,
          range: 'min',
          step: 0.01,
          value: getVolume(),
          slide: function(e, ui) {
            manuallySetVolume(ui.value);
          }
        });
      });
    } else $toggle.addClass('unavailable');

    return play;
  })();

  $.fn.orNot = function() {
    return this.length === 0 ? false : this;
  };

  $.trans = function() {
    var str = lichess_translations[arguments[0]];
    if (!str) return arguments[0];
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  function urlToLink(text) {
    var exp = /\bhttp:\/\/(?:[a-z]{0,3}\.)?(lichess\.org[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
    return text.replace(exp, "<a href='http://$1'>$1</a>");
  }

  var startTournamentClock = function() {
    $("div.game_tournament div.clock").each(function() {
      $(this).clock({
        time: parseFloat($(this).data("time"))
      });
    });
  };

  /////////////
  // game.js //
  /////////////

  function startRound(element, cfg) {
    var data = cfg.data;
    if (data.chat) $('#chat').chat({
      messages: data.chat,
      initialNote: data.note,
      gameId: data.game.id
    });
    var $watchers = $('#site_header div.watchers').watchers();
    var $nowPlaying = $('#now_playing');
    var round;
    if (data.tournament) $('body').data('tournament-id', data.tournament.id);
    lichess.socket = new lichess.StrongSocket(
      data.url.socket,
      data.player.version, {
        options: {
          name: "round"
        },
        params: {
          ran: "--ranph--",
          userTv: $('.user_tv').data('user-tv')
        },
        receive: function(t, d) {
          round.socketReceive(t, d);
        },
        events: {
          crowd: function(e) {
            $watchers.watchers("set", e.watchers);
          },
          featured: function(o) {
            if (data.tv) lichess.reload();
          },
          end: function() {
            var url = (data.tv ? cfg.routes.Tv.side : cfg.routes.Round.sideWatcher)(data.game.id, data.player.color).url;
            $.ajax({
              url: url,
              cache: false,
              success: function(html) {
                $('#site_header div.side').replaceWith(html);
                $('body').trigger('lichess.content_loaded');
                startTournamentClock();
              }
            });
          },
          checkCount: function(e) {
            $('div.check_count')
              .find('.white').text(e.black).end()
              .find('.black').text(e.white);
          }
        }
      });
    cfg.element = element.querySelector('.round');
    cfg.socketSend = lichess.socket.send.bind(lichess.socket);
    round = LichessRound(cfg);
    startTournamentClock();
    $('.crosstable', element).prependTo($('.underboard .center', element)).show();
    $('#tv_history').on("click", "tr", function() {
      location.href = $(this).find('a.view').attr('href');
    });
    var loadPlaying = function() {
      var $moveOn = $nowPlaying.find('.move_on').click(function() {
        setMoveOn(round.moveOn.toggle());
      });
      var setMoveOn = function(value) {
        $moveOn.toggleClass('enabled', value);
      };
      setMoveOn(round.moveOn.get());
    };
    loadPlaying();
    $nowPlaying.on('click', '>a', function() {
      lichess.hasToReload = true;
      return true;
    });
  }

  function startPrelude(element, cfg) {
    var data = cfg.data;
    lichess.socket = new lichess.StrongSocket(
      data.url.socket,
      data.player.version, {
        options: {
          name: "prelude"
        },
        params: {
          ran: "--ranph--"
        },
        events: {
          declined: function() {
            $('#challenge_await').remove();
            $('#challenge_declined').show();
          }
        }
      });

    Chessground(element.querySelector('.lichess_board'), {
      viewOnly: true,
      fen: data.game.fen,
      orientation: data.player.color,
      check: data.game.check,
      coordinates: data.pref.coords !== 0,
      highlight: {
        check: data.pref.highlight
      }
    });
    setTimeout(function() {
      $('.lichess_overboard_wrap', element).addClass('visible');
    }, 100);
    $('#challenge_await').each(function() {
      setInterval(function() {
        $('#challenge_await').each(function() {
          lichess.socket.send('challenge', $(this).data('user'));
        });
      }, 1500);
    });
  }

  $.widget("lichess.watchers", {
    _create: function() {
      this.list = this.element.find("span.list");
      this.number = this.element.find("span.number");
    },
    set: function(users) {
      var self = this;
      if (users.length > 0) {
        self.list.html(users.map(function(u) {
          return u.indexOf('(') === -1 ? $.userLink(u) : u.replace(/\s\(1\)/, '');
        }).join(", "));
        var nb = 0;
        users.forEach(function(u) {
          nb += (u.indexOf('(') === -1 ? 1 : parseInt(u.replace(/^.+\((\d+)\)$/, '$1')));
        });
        self.number.html(nb);
        self.element.show();
      } else {
        self.element.hide();
      }
    }
  });

  $.widget("lichess.friends", {
    _create: function() {
      var self = this;
      self.$list = self.element.find("div.list");
      self.$title = self.element.find('.title').click(function() {
        self.element.find('.content_wrap').toggle(100, function() {
          lichess.storage.set('friends-hide', $(this).is(':visible') ? 0 : 1);
        });
      });
      if (lichess.storage.get('friends-hide') == 1) self.$title.click();
      self.$nbOnline = self.$title.find('.online');
      self.$nobody = self.element.find("div.nobody");
      self.set(self.element.data('preload').split(','));
    },
    repaint: function() {
      this.users = $.unique(this.users).filter(function(u) {
        return u !== '';
      });
      this.$nbOnline.text(this.users.length);
      this.$nobody.toggle(this.users.length === 0);
      this.$list.html(this.users.map(this._renderUser).join(""));
      $('body').trigger('lichess.content_loaded');
    },
    set: function(us) {
      this.users = us || [];
      this.repaint();
    },
    enters: function(user) {
      this.users.push(user);
      this.repaint();
    },
    leaves: function(user) {
      this.users = this.users.filter(function(u) {
        return u != user
      });
      this.repaint();
    },
    _renderUser: function(user) {
      var id = $.fp.contains(user, ' ') ? user.split(' ')[1] : user;
      return '<a class="ulpt" data-placement="nw" href="/@/' + id + '">' + user + '</a>';
    }
  });

  $.widget("lichess.chat", {
    _create: function() {
      this.options = $.extend({
        messages: [],
        initialNote: '',
        gameId: null
      }, this.options);
      var self = this;
      self.$msgs = self.element.find('.messages');
      self.$msgs.on('click', 'a', function() {
        $(this).attr('target', '_blank');
      });
      var $form = self.element.find('form');
      var $input = self.element.find('input.lichess_say');
      var $parent = self.element.parent();

      // send a message
      $form.submit(function() {
        var text = $.trim($input.val());
        if (!text) return false;
        if (text.length > 140) {
          alert('Max length: 140 chars. ' + text.length + ' chars used.');
          return false;
        }
        $input.val('');
        lichess.socket.send('talk', text);
        return false;
      });

      self.element.find('a.send').click(function() {
        $input.trigger('click');
        $form.submit();
      });

      // toggle the chat
      var $toggle = $parent.find('input.toggle_chat');
      $toggle.change(function() {
        var enabled = $toggle.is(':checked');
        self.element.toggleClass('hidden', !enabled);
        if (!enabled) lichess.storage.set('nochat', 1);
        else lichess.storage.remove('nochat');
      });
      $toggle[0].checked = lichess.storage.get('nochat') != 1;
      if (!$toggle[0].checked) {
        self.element.addClass('hidden');
      }
      if (self.options.messages.length > 0) self._appendMany(self.options.messages);

      // Toggle Notes/Chat display
      $panels = self.element.find('div.chat_panels > div');
      $parent.find('.chat_menu').on('click', 'a', function() {
        var panel = $(this).data('panel');
        $(this).siblings('.active').removeClass('active').end().addClass('active');
        $panels.removeClass('active').filter('.' + panel).addClass('active');
      }).find('a:first').click();

      $notes = self.element.find('.notes textarea');
      if (self.options.gameId && $notes.length) {
        $notes.on('change keyup paste', $.fp.debounce(function() {
          $.post('/' + self.options.gameId + '/note', {
            text: $notes.val()
          });
        }, 1000));
        $notes.val(self.options.initialNote || '');
      }
    },
    append: function(msg) {
      this._appendHtml(this._render(msg));
    },
    _appendMany: function(objs) {
      var self = this,
        html = "";
      $.each(objs, function() {
        html += self._render(this);
      });
      self._appendHtml(html);
    },
    _render: function(msg) {
      var user, sys = false;
      if (msg.c) {
        user = '<span class="color">[' + msg.c + ']</span>';
      } else if (msg.u == 'lichess') {
        sys = true;
        user = '<span class="system"></span>';
      } else {
        user = '<span class="user">' + $.userLinkLimit(msg.u, 14) + '</span>';
      }
      return '<li class="' + (sys ? 'system trans_me' : '') + (msg.r ? ' troll' : '') + '">' + user + urlToLink(msg.t) + '</li>';
    },
    _appendHtml: function(html) {
      if (!html) return;
      this.$msgs.append(html);
      $('body').trigger('lichess.content_loaded');
      this.$msgs.scrollTop(999999);
    }
  });

  $.widget("lichess.clock", {
    _create: function() {
      var self = this;
      this.options.time = this.options.time * 1000;
      this.$time = this.element.find('>div.time');
      var end_time = new Date().getTime() + self.options.time;
      var tick = function() {
        var current_time = Math.round(end_time - new Date().getTime());
        if (current_time <= 0) {
          clearInterval(self.options.interval);
          current_time = 0;
        }
        self.options.time = current_time;
        self._show();
      };
      self.options.interval = setInterval(tick, 1000);
      tick();
    },
    destroy: function() {
      this.stop();
      $.Widget.prototype.destroy.apply(this);
    },
    _show: function() {
      this.$time.html(this._formatDate(new Date(this.options.time)));
    },
    _formatDate: function(date) {
      var minutes = this._prefixInteger(date.getUTCMinutes(), 2);
      var seconds = this._prefixInteger(date.getSeconds(), 2);
      var b = function(x) {
        return '<b>' + x + '</b>';
      };
      if (this.options.time >= 3600000) {
        var hours = this._prefixInteger(date.getUTCHours(), 2);
        return b(hours) + ':' + b(minutes) + ':' + b(seconds);
      } else {
        return b(minutes) + ':' + b(seconds);
      }
    },
    _prefixInteger: function(num, length) {
      return (num / Math.pow(10, length)).toFixed(length).substr(2);
    }
  });

  /////////////////
  // gamelist.js //
  /////////////////

  $(function() {
    lichess.parseFen();
    $('body').on('lichess.content_loaded', lichess.parseFen);

    var socketOpened = false;

    function startWatching() {
      if (!socketOpened) return;
      var ids = [];
      $('.mini_board.live').removeClass("live").each(function() {
        ids.push(this.getAttribute("data-live"));
      });
      if (ids.length) {
        lichess.socket.send("startWatching", ids.join(" "));
      }
    }
    $('body').on('lichess.content_loaded', startWatching);
    $('body').on('socket.open', function() {
      socketOpened = true;
      startWatching();
    });

    setTimeout(function() {
      $('div.checkmateCaptcha').each(function() {
        var $captcha = $(this);
        var $board = $captcha.find('.mini_board');
        var $input = $captcha.find('input').val('');
        var color = $board.data('color');

        $board.data('chessground').set({
          orientation: color,
          turnColor: color,
          movable: {
            free: false,
            dests: $board.data('moves'),
            color: color,
            coordinates: false,
            events: {
              after: function(orig, dest) {
                $captcha.removeClass("success failure");
                submit(orig + ' ' + dest);
              }
            }
          }
        });

        var submit = function(solution) {
          $input.val(solution);
          $.ajax({
            url: $captcha.data('check-url'),
            data: {
              solution: solution
            },
            success: function(data) {
              $captcha.toggleClass('success', data == 1);
              $captcha.toggleClass('failure', data != 1);
              if (data == 1) $board.data('chessground').stop();
              else setTimeout(function() {
                lichess.parseFen($board);
                $board.data('chessground').set({
                  turnColor: color,
                  movable: {
                    dests: $board.data('moves')
                  }
                });
              }, 300);
            }
          });
        };
      });
    }, 100);
  });

  function startLobby(element, cfg) {
    var $newposts = $("div.new_posts");
    var nbrEl = document.querySelector('#site_baseline span');
    var lobby;

    lichess.socket = new lichess.StrongSocket(
      '/lobby/socket/v1',
      cfg.data.version, {
        receive: function(t, d) {
          lobby.socketReceive(t, d);
        },
        events: {
          reload_timeline: function() {
            $.ajax({
              url: $("#timeline").data('href'),
              cache: false,
              success: function(html) {
                $('#timeline').html(html);
                $('body').trigger('lichess.content_loaded');
              }
            });
          },
          streams: function(html) {
            $('#streams_on_air').html(html);
          },
          featured: function(o) {
            $('#featured_game').html(o.html);
            $('body').trigger('lichess.content_loaded');
          },
          redirect: function(e) {
            lobby.setRedirecting();
            $.redirect(e);
          },
          tournaments: function(data) {
            $("#enterable_tournaments").html(data);
            $('body').trigger('lichess.content_loaded');
          },
          reload_forum: function() {
            setTimeout(function() {
              $.ajax({
                url: $newposts.data('url'),
                cache: false,
                success: function(data) {
                  $newposts.find('ol').html(data).end().scrollTop(0);
                  $('body').trigger('lichess.content_loaded');
                }
              });
            }, Math.round(Math.random() * 5000));
          },
          nbr: function(e) {
            if (nbrEl && e) {
              var prev = parseInt(nbrEl.textContent, 10);
              var k = 4;
              var interv = 2000 / k;
              $.fp.range(k).forEach(function(it) {
                setTimeout(function() {
                  var val = Math.round(((prev * (k - 1 - it)) + (e * (it + 1))) / k);
                  if (val !== prev) {
                    nbrEl.textContent = val;
                    prev = val;
                  }
                }, Math.round(it * interv));
              });
            }
          },
          fen: function(e) {
            lichess.StrongSocket.defaults.events.fen(e);
            lobby.gameActivity(e.id);
          }
        },
        options: {
          name: 'lobby'
        }
      });

    cfg.socketSend = lichess.socket.send.bind(lichess.socket);
    lobby = LichessLobby(document.getElementById('hooks_wrap'), cfg);

    var $startButtons = $('#start_buttons');

    function sliderTime(v) {
      if (v <= 20) return v;
      switch (v) {
        case 21:
          return 25;
        case 22:
          return 30;
        case 23:
          return 35;
        case 24:
          return 40;
        case 25:
          return 45;
        case 26:
          return 60;
        case 27:
          return 90;
        case 28:
          return 120;
        case 29:
          return 150;
        default:
          return 180;
      }
    }

    function sliderDays(v) {
      if (v <= 3) return v;
      switch (v) {
        case 4:
          return 5;
        case 5:
          return 7;
        case 6:
          return 10;
        default:
          return 14;
      }
    }

    function sliderInitVal(v, f, max) {
      for (var i = 0; i < max; i++) {
        if (f(i) === v) return i;
      }
    }

    function prepareForm() {
      var $form = $('.lichess_overboard');
      var $timeModeSelect = $form.find('#timeMode');
      var $modeChoicesWrap = $form.find('.mode_choice');
      var $modeChoices = $modeChoicesWrap.find('input');
      var $casual = $modeChoices.eq(0),
        $rated = $modeChoices.eq(1);
      var $variantSelect = $form.find('#variant');
      var $fenPosition = $form.find(".fen_position");
      var $timeInput = $form.find('.time_choice input');
      var $incrementInput = $form.find('.increment_choice input');
      var $daysInput = $form.find('.days_choice input');
      var isHook = $form.hasClass('game_config_hook');
      var $ratings = $form.find('.ratings > div');
      var toggleButtons = function() {
        var timeMode = $timeModeSelect.val();
        var rated = $rated.prop('checked');
        var timeOk = timeMode != '1' || $timeInput.val() > 0 || $incrementInput.val() > 0;
        var ratedOk = !isHook || !rated || timeMode != '0';
        $form.find('.color_submits button').toggle(timeOk && ratedOk);
      };
      var showRating = function() {
        var timeMode = $timeModeSelect.val();
        var key;
        switch ($variantSelect.val()) {
          case '1':
            if (timeMode == '1') {
              var time = $timeInput.val() * 60 + $incrementInput.val() * 30;
              if (time < 180) key = 'bullet';
              else if (time < 480) key = 'blitz';
              else key = 'classical';
            } else key = 'correspondence';
            break;
          case '2':
            key = 'chess960';
            break;
          case '4':
            key = 'kingOfTheHill';
            break;
          case '5':
            key = 'threeCheck';
            break;
          case '6':
            key = 'antichess'
            break;
          case '7':
            key = 'atomic'
        }
        $ratings.hide().filter('.' + key).show();
      };
      if (isHook) {
        var $formTag = $form.find('form');
        if ($form.data('anon')) {
          $timeModeSelect.val(1)
            .children('.timeMode_2, .timeMode_0')
            .prop('disabled', true)
            .attr('title', $.trans('You need an account to do that'));
        }
        var ajaxSubmit = function(color) {
          $.ajax({
            url: $formTag.attr('action').replace(/uid-placeholder/, lichess.StrongSocket.sri),
            data: $formTag.serialize() + "&color=" + color,
            type: 'post'
          });
          $form.find('a.close').click();
          lobby.setTab($timeModeSelect.val() === '1' ? 'real_time' : 'seeks');
          return false;
        };
        $formTag.find('.color_submits button').click(function() {
          return ajaxSubmit($(this).val());
        });
        $formTag.submit(function() {
          return ajaxSubmit('random');
        });
      }
      $form.find('div.buttons').buttonset().disableSelection();
      $form.find('button.submit').button().disableSelection();
      $timeInput.add($incrementInput).each(function() {
        var $input = $(this),
          $value = $input.siblings('span');
        $input.hide().after($('<div>').slider({
          value: sliderInitVal(parseInt($input.val()), sliderTime, 100),
          min: 0,
          max: 30,
          range: 'min',
          step: 1,
          slide: function(event, ui) {
            var time = sliderTime(ui.value);
            $value.text(time);
            $input.attr('value', time);
            showRating();
            toggleButtons();
          }
        }));
      });
      $daysInput.each(function() {
        var $input = $(this),
          $value = $input.siblings('span');
        $input.hide().after($('<div>').slider({
          value: sliderInitVal(parseInt($input.val()), sliderDays, 20),
          min: 1,
          max: 7,
          range: 'min',
          step: 1,
          slide: function(event, ui) {
            var days = sliderDays(ui.value);
            $value.text(days);
            $input.attr('value', days);
          }
        }));
      });
      $form.find('.rating_range').each(function() {
        var $this = $(this);
        var $input = $this.find("input");
        var $span = $this.siblings("span.range");
        var min = $input.data("min");
        var max = $input.data("max");
        var values = $input.val() ? $input.val().split("-") : [min, max];

        $span.text(values.join(' - '));
        $this.slider({
          range: true,
          min: min,
          max: max,
          values: values,
          step: 50,
          slide: function(event, ui) {
            $input.val(ui.values[0] + "-" + ui.values[1]);
            $span.text(ui.values[0] + " - " + ui.values[1]);
          }
        });
        var $ratingRangeConfig = $this.parent();
        $modeChoices.add($form.find('.members_only input')).on('change', function() {
          var rated = $rated.prop('checked');
          var membersOnly = $form.find('.members_only input').prop('checked');
          $ratingRangeConfig.toggle(rated || membersOnly);
          $form.find('.members_only').toggle(!rated);
          toggleButtons();
        }).trigger('change');
      });
      $timeModeSelect.on('change', function() {
        var timeMode = $(this).val();
        $form.find('.time_choice, .increment_choice').toggle(timeMode == '1');
        $form.find('.days_choice').toggle(timeMode == '2');
        toggleButtons();
        showRating();
      }).trigger('change');
      var $ratingRangeConfig = $form.find('.rating_range_config');
      var $fenInput = $fenPosition.find('input');

      var validateFen = $.fp.debounce(function() {
        $fenInput.removeClass("success failure");
        var fen = $fenInput.val();
        if (fen) {
          $.ajax({
            url: $fenInput.parent().data('validate-url'),
            data: {
              fen: fen
            },
            success: function(data) {
              $fenInput.addClass("success");
              $fenPosition.find('.preview').html(data);
              $fenPosition.find('a.board_editor').each(function() {
                $(this).attr('href', $(this).attr('href').replace(/editor\/.+$/, "editor/" + fen));
              });
              $('body').trigger('lichess.content_loaded');
            },
            error: function() {
              $fenInput.addClass("failure");
              $fenPosition.find('.preview').html("");
            }
          });
        }
      }, 500);
      $fenInput.on('keyup', validateFen);

      $variantSelect.on('change', function() {
        var fen = $(this).val() == '3';
        if (fen && $fenInput.val() !== '') validateFen();
        $fenPosition.toggle(fen);
        $modeChoicesWrap.toggle(!fen);
        if (fen) $casual.click();
        showRating();
      }).trigger('change');

      $form.find('div.level').each(function() {
        var $infos = $(this).find('.ai_info > div');
        $(this).find('label').mouseenter(function() {
          $infos.hide().filter('.' + $(this).attr('for')).show();
        });
        $(this).find('#config_level').mouseleave(function() {
          var level = $(this).find('input:checked').val();
          $infos.hide().filter('.level_' + level).show();
        }).trigger('mouseout');
      });

      $form.find('a.close.icon').click(function() {
        $form.remove();
        $startButtons.find('a.active').removeClass('active');
        return false;
      });
    }

    $startButtons.find('a').click(function() {
      $(this).addClass('active').siblings().removeClass('active');
      $('.lichess_overboard').remove();
      $.ajax({
        url: $(this).attr('href'),
        cache: false,
        success: function(html) {
          $('.lichess_overboard').remove();
          $('#hooks_wrap').prepend(html);
          prepareForm();
          $('body').trigger('lichess.content_loaded');
        }
      });
      return false;
    });

    if (['#ai', '#friend', '#hook'].indexOf(window.location.hash) !== -1) {
      $startButtons
        .find('a.config_' + location.hash.replace(/#/, ''))
        .each(function() {
          $(this).attr("href", $(this).attr("href") + location.search);
        }).click();
    }
  };

  ///////////////////
  // tournament.js //
  ///////////////////

  $(function() {

    var $tournamentList = $('#tournament_list');
    if ($tournamentList.length) {
      // handle tournament list
      lichess.StrongSocket.defaults.params.flag = "tournament";
      lichess.StrongSocket.defaults.events.reload = function() {
        $tournamentList.load($tournamentList.data("href"), function() {
          $('body').trigger('lichess.content_loaded');
        });
      };
      return;
    }
  });

  function startTournament(element, cfg) {
    $('body').data('tournament-id', cfg.data.id);
    var $watchers = $("div.watchers").watchers();
    if (typeof lichess_chat !== 'undefined') $('#chat').chat({
      messages: lichess_chat
    });
    var tournament;
    lichess.socket = new lichess.StrongSocket(
      '/tournament/' + cfg.data.id + '/socket/v1', cfg.socketVersion, {
        receive: function(t, d) {
          tournament.socketReceive(t, d)
        },
        events: {
          crowd: function(data) {
            $watchers.watchers("set", data);
          }
        },
        options: {
          name: "tournament"
        }
      });
    cfg.socketSend = lichess.socket.send.bind(lichess.socket);
    tournament = LichessTournament(element, cfg);
  };

  ////////////////
  // analyse.js //
  ////////////////

  function startAnalyse(element, cfg) {
    var data = cfg.data;
    if (data.chat) $('#chat').chat({
      messages: data.chat,
      initialNote: data.note,
      gameId: data.game.id
    });
    var $watchers = $('#site_header div.watchers').watchers();
    var analyse, $panels;
    lichess.socket = new lichess.StrongSocket(
      data.url.socket,
      data.player.version, {
        options: {
          name: "analyse"
        },
        params: {
          ran: "--ranph--",
          userTv: $('.user_tv').data('user-tv')
        },
        events: {
          analysisAvailable: function() {
            $.sound.dong();
            location.href = location.href.split('#')[0] + '#' + analyse.pathStr();
            location.reload();
          },
          crowd: function(event) {
            $watchers.watchers("set", event.watchers);
          }
        }
      });

    var $advChart = $("#adv_chart");
    var $timeChart = $("#movetimes_chart");
    var $inputFen = $('input.fen', element);
    var unselect = function(chart) {
      chart.getSelectedPoints().forEach(function(point) {
        point.select(false);
      });
    };
    var lastFen, lastPath;
    cfg.onChange = function(fen, path) {
      lastFen = fen = fen || lastFen;
      lastPath = path = path || lastPath;
      var chart, point;
      $inputFen.val(fen);
      if ($advChart.length) {
        chart = $advChart.highcharts();
        if (chart) {
          if (path.length > 1) unselect(chart);
          else {
            point = chart.series[0].data[path[0].ply - 1];
            if (typeof point != "undefined") {
              point.select();
              chart.setTitle({
                text: point.name + ' ' + 'Advantage: <strong>' + point.y + '</strong>'
              });
            } else unselect(chart);
          }
        }
      }
      var timeChartFirstDisplay = true;
      if ($timeChart.length) {
        chart = $timeChart.highcharts();
        if (chart) {
          if (path.length > 1) unselect(chart);
          else {
            var white = path[0].ply % 2 !== 0;
            var serie = white ? 0 : 1;
            var turn = Math.floor((path[0].ply - 1) / 2);
            point = chart.series[serie].data[turn];
            if (typeof point != "undefined") {
              point.select();
              var title = point.name + ' ' + 'Time used: <strong>' + (point.y * (white ? 1 : -1)) + '</strong> s';
              chart.setTitle({
                text: title
              });
              if (timeChartFirstDisplay) {
                chart.setTitle({
                  text: title
                });
                timeChartFirstDisplay = false;
              }
            } else unselect(chart);
          }
        }
      }
    };
    data.path = window.location.hash ? location.hash.replace(/#/, '') : '';
    analyse = LichessAnalyse(element.querySelector('.analyse'), cfg.data, cfg.routes, cfg.i18n, cfg.onChange);
    cfg.jump = analyse.jump;

    $('.underboard_content', element).appendTo($('.underboard .center', element)).show();
    $('.advice_summary', element).appendTo($('.underboard .right', element)).show();

    $panels = $('div.analysis_panels > div');
    var $menu = $('div.analysis_menu');
    var storageKey = 'lichess.analysis.panel';
    var setPanel = function(panel) {
      $menu.children('.active').removeClass('active').end().find('.' + panel).addClass('active');
      $panels.removeClass('active').filter('.' + panel).addClass('active');
      if (panel == 'move_times') try {
        $.renderMoveTimesChart();
      } catch (e) {}
    };
    $menu.on('click', 'a', function() {
      var panel = $(this).data('panel');
      lichess.storage.set(storageKey, panel);
      setPanel(panel);
    });
    if (cfg.data.analysis) setPanel('computer_analysis');
    else {
      var stored = lichess.storage.get(storageKey);
      if (stored && $menu.children('.' + stored).length) setPanel(stored);
      else if ($menu.children('.crosstable').length) $menu.children('.crosstable').click();
      else $menu.children('.move_times').click();
    }

    $panels.find('form.future_game_analysis').submit(function() {
      if ($(this).hasClass('must_login')) {
        if (confirm($.trans('You need an account to do that'))) {
          location.href = '/signup';
        }
        return false;
      }
      $.ajax({
        method: 'post',
        url: $(this).attr('action'),
        success: function(html) {
          $panels.filter('.panel.computer_analysis').html(html);
        }
      });
      return false;
    });
    $panels.find('div.pgn').click(function() {
      var range, selection;
      if (document.body.createTextRange) {
        range = document.body.createTextRange();
        range.moveToElementText($(this)[0]);
        range.select();
      } else if (window.getSelection) {
        selection = window.getSelection();
        range = document.createRange();
        range.selectNodeContents($(this)[0]);
        selection.removeAllRanges();
        selection.addRange(range);
      }
    });
  }

  ////////////////
  // user_analysis.js //
  ////////////////

  function startUserAnalysis(element, cfg) {
    var analyse = LichessAnalyse(element.querySelector('.analyse'), cfg.data, cfg.routes, cfg.i18n, null);
  }

  /////////////// forum.js ////////////////////

  $('#lichess_forum').on('click', 'a.delete', function() {
    $.post($(this).attr("href"));
    $(this).closest(".post").slideUp(100);
    return false;
  });

  $.idleTimer = function(delay, onIdle, onWakeUp) {
    var eventType = 'mousemove';
    var listening = false;
    var active = true;
    var lastSeenActive = new Date();
    var onActivity = function() {
      if (!active) onWakeUp();
      active = true;
      lastSeenActive = new Date();
      stopListening();
    };
    var startListening = function() {
      if (!listening) {
        document.addEventListener(eventType, onActivity);
        listening = true;
      }
    };
    var stopListening = function() {
      if (listening) {
        document.removeEventListener(eventType, onActivity);
        listening = false;
      }
    };
    setInterval(function() {
      if (active && new Date() - lastSeenActive > delay) {
        onIdle();
        active = false;
        startListening();
      } else startListening();
    }, 5000);
  };

  $.modal = function(html) {
    var $wrap = $('<div id="modal-wrap">').html(html.clone().show());
    var $overlay = $('<div id="modal-overlay">').html($wrap);
    $overlay.one('click', function() {
      $('#modal-overlay').remove();
    });
    $wrap.click(function(e) {
      e.stopPropagation();
    });
    $('body').prepend($overlay);
  };
})();
