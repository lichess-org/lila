// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

// declare now, populate later in a distinct script.
var lichess_translations = lichess_translations || [];
var lichess_sri = Math.random().toString(36).substring(5); // 8 chars

function withStorage(f) {
  // can throw an exception when storage is full
  try {
    return !!window.localStorage ? f(window.localStorage) : null;
  } catch (e) {}
}
var storage = {
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

  //////////////////
  // websocket.js //
  //////////////////

  var strongSocketDefaults = {
    events: {
      fen: function(e) {
        $('a.live_' + e.id).each(function() {
          parseFen($(this).data("fen", e.fen).data("lastmove", e.lm));
        });
      }
    },
    params: {
      sri: lichess_sri
    },
    options: {
      name: "unnamed",
      pingMaxLag: 7000, // time to wait for pong before reseting the connection
      pingDelay: 1000, // time between pong and ping
      autoReconnectDelay: 1000,
      lagTag: false, // jQuery object showing ping lag
      ignoreUnknownMessages: false
    }
  };

  var strongSocket = function(url, version, settings) {
    var self = this;
    self.settings = strongSocketDefaults;
    $.extend(true, self.settings, settings);
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
      storage.remove(self.options.baseUrlKey);
    }
    if (self.options.prodPipe) {
      self.options.baseUrls = ['socket.en.lichess.org:9021'];
    }
    self.connect();
    $(window).on('unload', function() {
      self.destroy();
    });
  };
  strongSocket.available = window.WebSocket || window.MozWebSocket;
  strongSocket.prototype = {
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

        // if (self.options.debug) window.liws = self.ws;
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
          _.each(resend, function(x) {
            self.send(x.t, x.d);
          });
        };
        self.ws.onmessage = function(e) {
          var m = JSON.parse(e.data);
          if (m.t == "n") {
            self.pong();
          } else self.debug(e.data);
          if (m.t == "b") {
            $(m.d || []).each(function() {
              self.handle(this);
            });
          } else {
            self.handle(m);
          }
        };
      } catch (e) {
        self.onError(e);
      }
      self.scheduleConnect(self.options.pingMaxLag);
    },
    send: function(t, d) {
      var self = this;
      var data = d || {};
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
      this.ackableMessages.push({
        t: t,
        d: d
      });
      this.send(t, d);
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
        self.options.lagTag.html('<strong>' + self.currentLag + "</strong> ms");
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
          var h = self.settings.events[m.t];
          if ($.isFunction(h)) h(m.d || null);
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
        console.debug("[" + this.options.name + " " + lichess_sri + "]", msg);
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
        if (!storage.get("wsok") && $("#websocket-fail").length === 0) {
          $.ajax("/assets/websocket-fail.html", {
            success: function(html) {
              $('body').prepend("<div id='websocket-fail'>" + html + "</div>");
            }
          });
        }
      }, 1000);
      clearTimeout(self.pingSchedule);
    },
    onSuccess: function() {
      storage.set("wsok", 1);
      $("#websocket-fail").remove();
    },
    baseUrl: function() {
      var key = this.options.baseUrlKey;
      var urls = this.options.baseUrls;
      var url = storage.get(key);
      if (!url) {
        url = urls[0];
        storage.set(key, url);
      } else if (this.tryOtherUrl) {
        this.tryOtherUrl = false;
        url = urls[(urls.indexOf(url) + 1) % urls.length];
        storage.set(key, url);
      }
      return url;
    },
    pingInterval: function() {
      return this.options.pingDelay + this.averageLag;
    }
  };

  /////////////
  // ctrl.js //
  /////////////

  $.userLink = function(u) {
    return $.userLinkLimit(u, false);
  };
  $.userLinkLimit = function(u, limit, klass) {
    return (u || false) ? '<a class="user_link ulpt ' + (klass || '') + '" href="/@/' + u + '">' + ((limit || false) ? u.substring(0, limit) : u) + '</a>' : 'Anonymous';
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

  var lichess = {
    socket: null,
    socketDefaults: {
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
          var $tag = $('#nb_connected_players > strong');
          if ($tag.length && e) {
            var prev = parseInt($tag.text(), 10) || Math.max(0, (e - 10));
            var k = 6;
            var interv = lichess.socket.pingInterval() / k;
            _.each(_.range(k), function(it) {
              setTimeout(function() {
                var val = Math.round(((prev * (k - 1 - it)) + (e * (it + 1))) / k);
                if (val != prev) {
                  $tag.text(val);
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
        tournamentReminder: function(data) {
          if (!$('#tournament_reminder').length && $('body').data("tournament-id") != data.id) {
            $('#notifications').append(data.html).find("a.withdraw").click(function() {
              $.post($(this).attr("href"));
              $('#tournament_reminder').remove();
              return false;
            });
          }
        },
        challengeReminder: function(data) {
          if (!storage.get('challenge-refused-' + data.id)) {
            var autoDecline = $('#lichess a.lichess_resign').length;
            var $overboard = $('div.lichess_overboard.joining');
            var declineListener = function($a, callback) {
              return $a.click(function() {
                $.post($(this).attr("href"));
                storage.set('challenge-refused-' + data.id, 1);
                $('#challenge_reminder').remove();
                if ($.isFunction(callback)) callback();
                return false;
              });
            };
            if ($overboard.length) {
              if (!$overboard.find('a.decline').length)
                $overboard.find('form').append(
                  declineListener($(data.html).find('a.decline'), function() {
                    location.href = "/";
                  })
                );
              return;
            }
            $('#challenge_reminder').each(function() {
              clearTimeout($(this).data('timeout'));
              $(this).remove();
            });
            $('#notifications').append($(data.html).toggle(!autoDecline));
            declineListener($('#notifications a.decline'));
            $('#challenge_reminder').data('timeout', setTimeout(function() {
              $('#challenge_reminder').remove();
            }, 3000));
            // automatically decline when playing
            if (autoDecline) {
              $('#notifications a.decline').click();
            } else {
              $('body').trigger('lichess.content_loaded');
              if (!storage.get('challenge-' + data.id)) {
                $.sound.dong();
                storage.set('challenge-' + data.id, 1);
              }
            }
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
        baseUrls: _.union(
          'socket.' + document.domain,
          _.map(($('body').data('ports') + '').split(','), function(port) {
            return 'socket.' + document.domain + ':' + port;
          })),
        baseUrlKey: 'surl3',
        name: "site",
        lagTag: $('#connection_lag'),
        debug: location.search.indexOf('debug-ws') != -1,
        prodPipe: location.search.indexOf('prod-ws') != -1,
        resetUrl: location.search.indexOf('reset-ws') != -1
      }
    },
    idleTime: 20 * 60 * 1000
  };

  lichess.hasToReload = false;
  lichess.reload = function() {
    lichess.hasToReload = true;
    location.reload();
  };

  $(function() {

    // small layout
    function onResize() {
      if ($(document.body).width() < 1000) {
        $(document.body).addClass("tight");
        $('#site_header .side_menu').prependTo('div.content_box:first');
      } else {
        $(document.body).removeClass("tight");
        $('#timeline, div.lichess_goodies div.box, div.under_chat').each(function() {
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
          $(this).children().toggle($(this).width() == 224);
        });
        $('div.content_box .side_menu').appendTo('#site_header');
      }
    }
    $(window).resize(onResize);
    onResize();

    var bodyHeight = $('body').height();
    var winHeight = $(window).height();
    if (bodyHeight < winHeight) {
      var $footer = $('div.footer_wrap');
      $footer.css('marginTop', winHeight - bodyHeight + 30 + 'px');
    }

    if (!strongSocket.available) {
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

    $('#lichess').on('click', 'a.socket-link', function() {
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

    function userPowertips() {
      var header = document.getElementById('site_header');
      $('a.ulpt').removeClass('ulpt').each(function() {
        $(this).powerTip({
          fadeInTime: 100,
          fadeOutTime: 100,
          placement: $(this).data('placement') || ($.contains(header, this) ? 'e' : 'w'),
          mouseOnToPopup: true,
          closeDelay: 200
        }).on({
          powerTipPreRender: function() {
            $.ajax({
              url: $(this).attr('href').replace(/\?.+$/, '') + '/mini',
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

    $('#top a.message').powerTip({
      placement: 'sw',
      mouseOnToPopup: true,
      closeDelay: 200
    }).on({
      powerTipPreRender: function() {
        $.ajax({
          url: $(this).data('href'),
          success: function(html) {
            $('#powerTip').html(html).addClass('messages');
          }
        });
      }
    }).data('powertip', ' ');

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

    // Start game
    var $game = $('div.lichess_game').orNot();
    if ($game) $game.game(_ld_);

    setTimeout(function() {
      if (lichess.socket === null) {
        lichess.socket = new strongSocket("/socket", 0, lichess.socketDefaults);
      }
      $(document).idleTimer(lichess.idleTime)
        .on('idle.idleTimer', function() {
          lichess.socket.destroy();
        })
        .on('active.idleTimer', function() {
          lichess.socket.connect();
        });
    }, 200);

    var $board = $('div.with_marks');
    if ($board.length > 0) {
      $.displayBoardMarks($board.parent(), $('#lichess > div.lichess_player_white').length);
    }

    // themepicker
    var $body = $('body');
    var $themes = $('#top div.themepicker div.theme');
    var themes = $.makeArray($themes.map(function() {
      return $(this).data("theme");
    }));
    var theme = $.map(document.body.className.split(/\s+/), function(a) {
      return $.inArray(a, themes) < 0 ? null : a;
    })[0];
    $themes.hover(function() {
      $body.removeClass(themes.join(' ')).addClass($(this).data("theme"));
    }, function() {
      $body.removeClass(themes.join(' ')).addClass(theme);
    }).click(function() {
      theme = $(this).data("theme");
      $.post($(this).parent().data("href"), {
        "theme": theme
      });
      $('#top .themepicker').removeClass("shown");
    });

    $('#top a.bgpicker').click(function() {
      var bg = $body.hasClass("dark") ? "light" : "dark";
      $body.removeClass('light dark').addClass(bg);
      if (bg == 'dark' && $('link[href*="dark.css"]').length === 0) {
        $('link[href*="common.css"]').clone().each(function() {
          $(this).attr('href', $(this).attr('href').replace(/common\.css/, 'dark.css')).appendTo('head');
        });
      }
      $.post($(this).attr('href'), {
        bg: bg
      });
      return false;
    });

    $.centerOverboard = function() {
      var $o = $('div.lichess_overboard.auto_center');
      if ($o.length > 0) {
        $o.css('top', Math.max(-30, 238 - $o.height() / 2) + 'px').show();
      }
    };
    $.centerOverboard();

    $('.js_email').one('click', function() {
      var email = 'thibault.duplessis@gmail.com';
      $(this).replaceWith($('<a/>').text(email).attr('href', 'mailto:' + email));
    });

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
        loading: {
          msgText: "",
          finishedMsg: "---"
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
        var $t = $(this);
        _.each(acceptLanguages.split(','), function(lang) {
          $t.find('a[lang="' + lang + '"]').addClass('accepted');
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
  });

  $.sound = (function() {
    var baseUrl = $('body').data('sound-dir');
    var makeAudio = function(file, volume) {
      var a = new Audio(baseUrl + '/' + file);
      a.volume = volume;
      return a;
    };
    var audio = {
      dong: makeAudio('dong.ogg', 1),
      moveW: makeAudio('move.ogg', 0.6),
      moveB: makeAudio('move.ogg', 0.6),
      take: makeAudio('take.ogg', 0.6)
    };
    var canPlay = !! audio.moveW.canPlayType && audio.moveW.canPlayType('audio/ogg; codecs="vorbis"');
    var $toggle = $('#sound_state').toggleClass('sound_state_on', storage.get('sound') == 1);
    var enabled = function() {
      return $toggle.hasClass("sound_state_on");
    };
    var shouldPlay = function() {
      return canPlay && enabled();
    };
    var play = {
      move: function(white) {
        if (shouldPlay()) {
          if (white) audio.moveW.play();
          else audio.moveB.play();
        }
      },
      take: function() {
        if (shouldPlay()) audio.take.play();
      },
      dong: function() {
        if (shouldPlay()) audio.dong.play();
      }
    };
    if (canPlay) $toggle.click(function() {
      $toggle.toggleClass('sound_state_on', !enabled());
      if (enabled()) storage.set('sound', 1);
      else storage.remove('sound');
      play.dong();
      return false;
    });
    else $toggle.addClass('unavailable');

    return play;
  })();

  $.fn.orNot = function() {
    return this.length === 0 ? false : this;
  };

  $.trans = function(text) {
    return lichess_translations[text] ? lichess_translations[text] : text;
  };

  $.displayBoardMarks = function($board, isWhite) {
    var factor = 1,
      base = 0;
    if (!isWhite) {
      factor = -1;
      base = 575;
    }
    var letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'],
      marks = '';
    for (i = 1; i < 9; i++) {
      marks += '<span class="board_mark vert" style="bottom:' + (factor * i * 64 - 38 + base) + 'px;">' + i + '</span>';
      marks += '<span class="board_mark horz" style="left:' + (factor * i * 64 - 35 + base) + 'px;">' + letters[i - 1] + '</span>';
    }
    $board.remove('span.board_mark').append(marks);
  };

  function urlToLink(text) {
    var exp = /\bhttp:\/\/(?:[a-z]{0,3}\.)?(lichess\.org[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
    return text.replace(exp, "<a href='http://$1'>$1</a>");
  }

  /////////////
  // game.js //
  /////////////

  $.widget("lichess.game", {

    _init: function() {
      var self = this;
      self.$board = self.element.find("div.lichess_board");
      self.$table = self.element.find("div.lichess_table_wrap");
      self.$tableInner = self.$table.find("div.table_inner");
      self.$chat = $('#chat').orNot();
      self.$watchers = $("div.watchers");
      self.initialTitle = document.title;
      self.hasMovedOnce = false;
      self.premove = null;
      self.holdStart = null;
      self.holds = [];
      self.options.tableUrl = self.element.data('table-url');
      self.options.endUrl = self.element.data('end-url');
      self.options.socketUrl = self.element.data('socket-url');

      startTournamentClock();

      if (self.options.tournament_id) {
        $('body').data('tournament-id', self.options.tournament_id);
      }

      if (self.options.game.started) {
        self.indicateTurn();
        self.initSquaresAndPieces();
        self.initTable();
        self.initClocks();
        if (self.$chat) self.$chat.chat({
          resize: true,
          messages: lichess_chat
        });
        self.$watchers.watchers();
        if (self.isMyTurn() && self.options.game.turns === 0) {
          $.sound.dong();
        }
        if (!self.options.game.finished && !self.options.player.spectator) {
          self.blur = 0;
          $(window).blur(function() {
            self.blur = 1;
          });
        }
        self.unloaded = false;
        $(window).unload(function() {
          self.unloaded = true;
        });
        if (self.options.game.last_move) {
          self.highlightLastMove(self.options.game.last_move);
        }
      }

      if (self.options.player.spectator && !self.options.game.finished) {
        self.$board.find("div.lcs").mousedown(function() {
          $("#dont_touch").toggle();
        });
      }

      if (!self.options.opponent.ai && !self.options.player.spectator) {
        setTimeout(self.updateTitle = function() {
          document.title = (self.isMyTurn() && self.options.game.started && !self.options.game.finished) ? document.title = document.title.indexOf('/\\/') === 0 ? '\\/\\ ' + document.title.replace(/\/\\\/ /, '') : '/\\/ ' + document.title.replace(/\\\/\\ /, '') : document.title;
          setTimeout(self.updateTitle, 400);
        }, 400);
      }

      if (!self.options.player.spectator) {
        var okToLeave = function() {
          return lichess.hasToReload || !self.options.game.started || self.options.game.finished || !self.options.game.clock;
        };
        $(window).on('beforeunload', function() {
          if (!okToLeave()) return 'There is a game in progress!';
        }).on('unload', function() {
          if (!okToLeave()) lichess.socket.send('bye');
        });
      }

      $('#tv_history').on("click", "tr", function() {
        location.href = $(this).find('a.view').attr('href');
      });

      lichess.socket = new strongSocket(
        self.options.socketUrl,
        self.options.player.version,
        $.extend(true, lichess.socketDefaults, {
          options: {
            name: "game"
          },
          params: {
            ran: "--ranph--"
          },
          events: {
            possible_moves: function(event) {
              self.element.queue(function() {
                self.options.possible_moves = event;
                self.indicateTurn();
                self.element.dequeue();
              });
            },
            move: function(event) {
              self.element.queue(function() {
                // if a draw was claimable, remove the zone
                $('#claim_draw_zone').remove();
                self.$board.find("div.lcs.check").removeClass("check");
                self.$board.find("div.droppable-hover").removeClass("droppable-hover");
                // If I made the move, the piece is already moved on the board
                if (self.hasMovedOnce && event.color == self.options.player.color) {
                  self.element.dequeue();
                } else {
                  self.movePiece(event.from, event.to, function() {
                    self.element.dequeue();
                  }, false);
                }
              });
            },
            castling: function(event) {
              self.element.queue(function() {
                $("div#" + event.rook[1], self.$board).append($("div#" + event.rook[0] + " div.piece.rook", self.$board));
                // if the king is beeing animated, stop it now
                $('body > div.king').each(function($k) {
                  $.stop(true, true);
                });
                $("div#" + event.king[1], self.$board).append($("div.piece.king." + event.color, self.$board));
                self.element.dequeue();
              });
            },
            promotion: function(event) {
              self.element.queue(function() {
                $("div#" + event.key + " div.piece").addClass(event.pieceClass).removeClass("pawn");
                self.element.dequeue();
              });
            },
            check: function(event) {
              self.element.queue(function() {
                $("div#" + event, self.$board).addClass("check");
                self.element.dequeue();
              });
            },
            enpassant: function(event) {
              self.element.queue(function() {
                self.killPiece($("div#" + event + " div.piece", self.$board));
                self.element.dequeue();
              });
            },
            redirect: function(event) {
              // stop queue propagation here
              self.element.queue(function() {
                setTimeout(function() {
                  lichess.hasToReload = true;
                  $.redirect(event);
                }, 400);
              });
            },
            threefold_repetition: function() {
              self.element.queue(function() {
                self.reloadTable(function() {
                  self.element.dequeue();
                });
              });
            },
            gone: function(event) {
              if (!self.options.opponent.ai) {
                self.$table.find("div.force_resign_zone").toggle(event);
                self.centerTable();
              }
            },
            featured: function(o) {
              if (self.options.player.spectator && self.options.tv) {
                // stop queue propagation here
                self.element.queue(function() {
                  lichess.reload();
                });
              }
            },
            end: function() {
              // Game end must be applied firt: no queue
              self.options.game.finished = true;
              self.$table
                .find("div.lichess_table").addClass("finished").end()
                .find(".moretime").remove().end()
                .find('div.clock').clock('stop');
              try {
                self.element.find("div.ui-draggable").draggable("destroy");
              } catch (e) {}
              setTimeout(function() {
                self.element.find('.ui-draggable-dragging').remove();
                self.$board.find('.piece:not(:visible)').show();
              }, 300);
              $('div.underboard').hide().filter('.replay_and_analyse').show();
              // But enqueue the visible changes
              self.element.queue(function() {
                self.changeTitle($.trans("Game Over"));
                self.element.removeClass("my_turn");
                $.sound.dong();
                self.loadEnd(function() {
                  self.element.dequeue();
                });
              });
            },
            reload_table: function() {
              self.element.queue(function() {
                self.reloadTable(function() {
                  self.element.dequeue();
                });
              });
            },
            clock: function(event) {
              self.element.queue(function() {
                self.updateClocks(event);
                self.element.dequeue();
              });
            },
            premove: function() {
              if (self.options.enablePremove) {
                self.element.queue(function() {
                  self.applyPremove();
                  self.element.dequeue();
                });
              }
            },
            crowd: function(event) {
              $(["white", "black"]).each(function() {
                self.$table.find("div.username." + this).toggleClass("connected", event[this]).toggleClass("offline", !event[this]);
              });
              self.$watchers.watchers("set", event.watchers);
            },
            state: function(event) {
              self.element.queue(function() {
                self.options.game.player = event.color;
                self.options.game.turns = event.turns;
                self.element.dequeue();
              });
            },
            declined: function() {
              $('#challenge_await').remove();
              $('#challenge_declined').show();
            }
          }
        }));

      $('#challenge_await').each(function() {
        var userId = $(this).data('user');
        setInterval(function() {
          if ($('#challenge_await').length) lichess.socket.send('challenge', userId);
        }, 1500);
      });
    },
    isMyTurn: function() {
      return this.options.possible_moves !== null;
    },
    changeTitle: function(text) {
      if (this.options.player.spectator) return;
      document.title = text + " - " + this.initialTitle;
    },
    indicateTurn: function() {
      var self = this;
      if (self.options.game.finished) {
        self.changeTitle($.trans("Game Over"));
      } else if (self.isMyTurn()) {
        self.element.addClass("my_turn");
        self.changeTitle($.trans('Your turn'));
      } else {
        self.element.removeClass("my_turn");
        self.changeTitle($.trans('Waiting for opponent'));
      }

      if (!self.$table.find('>div').hasClass('finished')) {
        self.$tableInner.find("div.lichess_current_player div.lichess_player." + (self.isMyTurn() ? self.options.opponent.color : self.options.player.color)).hide();
        self.$tableInner.find("div.lichess_current_player div.lichess_player." + (self.isMyTurn() ? self.options.player.color : self.options.opponent.color)).show();
      }
    },
    movePiece: function(from, to, callback, mine) {

      // must stop any animation first, so the piece can get killed
      $('body > div.piece').stop(true, true);

      var self = this,
        $piece = self.$board.find("div#" + from + " div.piece"),
        $from = $("div#" + from, self.$board),
        $to = $("div#" + to, self.$board),
        $killed = $to.find("div.piece");

      // already moved
      if (!$piece.length) {
        self.onError(from + " " + to + ' empty from square!!', true);
        return;
      }

      var color = self.getPieceColor($piece);

      if ($killed.length) $.sound.take();
      else $.sound.move(color == 'white');

      self.highlightLastMove(from + " " + to);

      var afterMove = function() {
        if ($killed.length && color != self.getPieceColor($killed)) {
          self.killPiece($killed);
        }
        $piece.css({
          top: 0,
          left: 0
        });
        $to.append($piece);
        if ($.isFunction(callback || null)) callback();
      };

      var animD = mine ? 0 : self.animationDelay();

      if (animD < 50) {
        afterMove();
      } else {
        $("body").append($piece.css({
          top: $from.offset().top,
          left: $from.offset().left
        }));
        $piece.animate({
          top: $to.offset().top,
          left: $to.offset().left
        }, animD, afterMove);
      }
    },
    animationDelay: function() {
      var self = this;
      if (self.hasClock()) {
        var times = self.$table.find('div.clock').map(function() {
          return $(this).clock('getSeconds');
        }).get();
        times.sort();
        return this.options.animation_delay * Math.min(1, times[0] / 120);
      } else return this.options.animation_delay;
    },
    highlightLastMove: function(notation) {
      var self = this;
      var squareIds = notation.split(" ");
      $("div.lcs.moved", self.$board).removeClass("moved");
      $("#" + squareIds[0] + ",#" + squareIds[1], self.$board).addClass("moved");

    },
    killPiece: function($piece) {
      if ($.data($piece, 'draggable')) $piece.draggable("destroy");
      this.element.find("div.lichess_cemetery." + this.getPieceColor($piece))
        .append($("<div>").addClass('lichess_tomb').append($piece.css('position', 'relative')));
    },
    possibleMovesContain: function(from, to) {
      return this.options.possible_moves !== null && typeof this.options.possible_moves[from] !== 'undefined' && this.options.possible_moves[from].indexOf(to) != -1;
    },
    validMove: function(from, to, piece) {
      if (from == to) return false;
      var self = this,
        f = self.getSquareCoords(from),
        t = self.getSquareCoords(to),
        color = self.getPieceColor(piece),
        role = self.getPieceRole(piece);
      switch (role) {
        case 'pawn':
          if (Math.abs(t.x - f.x) > 1) return false;
          if (color == 'white') return (t.y == f.y + 1) || (f.y == 2 && t.y == 4 && f.x == t.x);
          else return (t.y == f.y - 1) || (f.y == 7 && t.y == 5 && f.x == t.x);
          break;
        case 'knight':
          var xd = Math.abs(t.x - f.x);
          var yd = Math.abs(t.y - f.y);
          return (xd == 1 && yd == 2) || (xd == 2 && yd == 1);
        case 'bishop':
          return Math.abs(t.x - f.x) == Math.abs(t.y - f.y);
        case 'rook':
          return t.x == f.x || t.y == f.y;
        case 'king':
          return (Math.abs(t.x - f.x) <= 1 && Math.abs(t.y - f.y) <= 1) ||
            (f.y == t.y && (f.y == (color == 'white' ? 1 : 8)) && (
            (f.x == 5 && (t.x == 3 || t.x == 7)) ||
            $('#' + to + '>.rook.' + color).length == 1));
        case 'queen':
          return Math.abs(t.x - f.x) == Math.abs(t.y - f.y) || t.x == f.x || t.y == f.y;
      }
    },
    applyPremove: function() {
      var self = this;
      if (self.options.enablePremove && self.premove && self.isMyTurn()) {
        var move = self.premove;
        self.unsetPremove();
        self.apiMove(move.from, move.to, true);
      }
    },
    setPremove: function(move) {
      var self = this;
      if (!self.options.enablePremove || self.isMyTurn()) return;
      self.unsetPremove();
      if (!self.validMove(move.from, move.to, move.piece)) return;
      self.premove = move;
      $("#" + move.from + ",#" + move.to).addClass("premoved");
      self.unselect();
      $("#premove_alert").show();
    },
    unsetPremove: function() {
      var self = this;
      self.premove = null;
      self.$board.find('div.lcs.premoved').removeClass('premoved');
      $("#premove_alert").hide();
    },
    unselect: function() {
      this.$board.find('> div.selected').removeClass('selected');
    },
    apiMove: function(orig, dest, isPremove) {
      if (!this.possibleMovesContain(orig, dest)) return;
      var $fromSquare = $("#" + orig).orNot();
      var $toSquare = $("#" + dest).orNot();
      var $piece = $fromSquare.find(".piece").orNot();
      if ($fromSquare && $toSquare && $piece) {
        this.dropPiece($piece, $fromSquare, $toSquare, isPremove | false);
      }
    },
    dropPiece: function($piece, $oldSquare, $newSquare, isPremove) {
      var self = this,
        squareId = $newSquare.attr('id'),
        moveData = {
          from: $oldSquare.attr("id"),
          to: squareId,
          b: self.blur
        };
      if (moveData.from == moveData.to) return;

      if (!self.isMyTurn()) {
        return self.setPremove({
          piece: $piece,
          from: moveData.from,
          to: moveData.to
        });
      }

      self.unselect();
      self.hasMovedOnce = true;
      self.blur = 0;
      self.options.possible_moves = null;
      self.movePiece($oldSquare.attr("id"), squareId, null, true);

      function sendMoveRequest(moveData) {
        if (self.hasClock()) {
          moveData.lag = Math.round(lichess.socket.averageLag);
        }
        lichess.socket.sendAckable("move", moveData);
      }

      var color = self.options.player.color;
      // promotion
      if ($piece.hasClass('pawn') && ((color == "white" && squareId[1] == 8) || (color == "black" && squareId[1] == 1))) {
        var aq = self.options.autoQueen;
        if (aq == 3 || (isPremove && aq == 2)) {
          moveData.promotion = "queen";
          sendMoveRequest(moveData);
        } else {
          var html = _.map(['queen', 'knight', 'rook', 'bishop'], function(p) {
            return '<div data-piece="' + p + '" class="piece ' + p + ' ' + color + '"></div>';
          }).join('');
          var $choices = $('<div class="promotion_choice onbg">')
            .appendTo(self.$board)
            .html(html)
            .fadeIn(self.animationDelay())
            .find('div.piece')
            .click(function() {
              moveData.promotion = $(this).attr('data-piece');
              sendMoveRequest(moveData);
              $choices.fadeOut(self.animationDelay(), function() {
                $choices.remove();
              });
            }).end();
        }
      } else {
        sendMoveRequest(moveData);
      }
    },
    initSquaresAndPieces: function() {
      var self = this;
      if (self.options.game.finished || self.options.player.spectator) {
        return;
      }
      var draggingKey;
      var dropped = false;
      // init squares
      self.$board.find("div.lcs").each(function() {
        var squareId = $(this).attr('id');
        $(this).droppable({
          accept: function(draggable) {
            if (!self.isMyTurn()) {
              var $piece = $('#' + draggingKey).find('>.piece');
              if ($piece.length) return self.validMove(draggingKey, squareId, $piece);
            } else {
              return draggingKey && self.possibleMovesContain(draggingKey, squareId);
            }
          },
          drop: function(ev, ui) {
            self.dropPiece(ui.draggable, ui.draggable.parent(), $(this));
            self.addHold();
            dropped = true;
          },
          hoverClass: 'droppable-hover'
        });
      });

      // init pieces
      self.$board.find("div.piece." + self.options.player.color).each(function() {
        var $this = $(this);
        $this.draggable({
          helper: function() {
            return $('<div>').attr('class', $this.attr('class')).appendTo(self.$board);
          },
          start: function() {
            draggingKey = $this.hide().parent().attr('id');
            self.holdStart = Date.now();
            dropped = false;
            self.unselect();
          },
          stop: function(e, ui) {
            draggingKey = null;
            var dist = Math.sqrt(Math.pow(ui.originalPosition.top - ui.position.top, 2) + Math.pow(ui.originalPosition.left - ui.position.left, 2));
            if (!dropped && dist <= 32) $this.trigger('click');
            $this.show();
          },
          scroll: false
        });
      });

      /*
       * Code for touch screens like android or iphone
       */

      self.$board.find("div.piece." + self.options.player.color).each(function() {
        $(this).click(function() {
          self.unsetPremove();
          var $square = $(this).parent();
          if ($square.hasClass('selectable')) return;
          var isSelected = $square.hasClass('selected');
          self.unselect();
          if (isSelected) return;
          $square.addClass('selected');
          self.holdStart = Date.now();
        });
      });

      self.$board.find("div.lcs").each(function() {
        var $this = $(this);
        $this.hover(function() {
          var $selected = self.$board.find('div.lcs.selected');
          if ($selected.length) {
            var $piece = $selected.find('>.piece');
            var validPremove = !self.isMyTurn() && $piece.length && self.validMove($selected.attr('id'), $this.attr('id'), $piece);
            if (validPremove || self.possibleMovesContain($selected.attr('id'), $this.attr('id'))) {
              $this.addClass('selectable');
            }
          }
        }, function() {
          $this.removeClass('selectable');
        }).click(function() {
          self.unsetPremove();
          var $from = self.$board.find('div.lcs.selected').orNot();
          var $to = $this;
          if (!$from || $from == $to) return;
          var $piece = $from.find('div.piece');
          if (!self.isMyTurn() && $from) {
            self.dropPiece($piece, $from, $to);
          } else {
            if (!self.possibleMovesContain($from.attr('id'), $this.attr('id'))) return;
            if (!$to.hasClass('selectable')) return;
            $to.removeClass('selectable');
            self.dropPiece($piece, $from, $this);
          }
          self.addHold();
        });
      });

      /*
       * End of code for touch screens
       */
    },
    addHold: function() {
      if (this.holdStart) {
        var nb = 10;
        this.holds.push(Date.now() - this.holdStart);
        this.holdStart = null;
        if (this.holds.length > nb) {
          this.holds.shift();
          var mean = this.holds.reduce(function(a, b) {
            return a + b;
          }) / nb;
          if (mean > 3 && mean < 80) {
            var diffs = this.holds.map(function(a) {
              return Math.pow(a - mean, 2);
            });
            var sd = Math.sqrt(diffs.reduce(function(a, b) {
              return a + b;
            }) / nb);
            if (sd < 10) lichess.socket.send('hold', {
              mean: Math.round(mean),
              sd: Math.round(sd)
            });
          }
        }
      }
    },
    reloadTable: function(callback) {
      var self = this;
      self.get(self.options.tableUrl, {
          success: function(html) {
            self.$tableInner.html(html);
            self.initTable();
            if ($.isFunction(callback)) callback();
            $('body').trigger('lichess.content_loaded');
            self.$tableInner.find('.lichess_claim_draw').each(function() {
              var $link = $(this);
              if (self.options.autoThreefold == 3) $link.click();
              if (self.options.autoThreefold == 2) {
                self.$table.find('.clock_bottom').each(function() {
                  if ($(this).clock('getSeconds') < 30) $link.click();
                });
              }
            });
          }
        },
        false);
    },
    loadEnd: function(callback) {
      var self = this;
      $.getJSON(self.options.endUrl, function(data) {
        $(['white', 'black']).each(function() {
          if (data.players[this]) self.$table.find('div.username.' + this).html(data.players[this]);
        });
        if (data.players.me) $('#user_tag span').text(data.players.me);
        self.$tableInner.html(data.table);
        self.initTable();
        if (!(self.options.player.spectator && self.options.tv)) {
          $('div.lichess_goodies').replaceWith(data.infobox);
          startTournamentClock();
        }
        if (self.$chat) self.$chat.chat('resize');
        if ($.isFunction(callback)) callback();
        $('body').trigger('lichess.content_loaded');
      });
    },
    initTable: function() {
      var self = this;
      self.centerTable();
      self.$table.find('a.moretime').unbind("click").click(self.moretime);
    },
    moretime: _.throttle(function() {
      lichess.socket.send('moretime');
    }, 800),
    centerTable: function() {
      var self = this;
      self.$table.find(".lichess_control").each(function() {
        $(this).toggleClass("none", $(this).html().trim() === "");
        $(this).find('a.button:empty').each(function() {
          $(this).hover(function() {
            $(this).text(' ' + ($(this).data('title') || $(this).attr('title')));
          }, function() {
            $(this).text('');
          });
        });
      });
      self.$table.css('top', (256 - self.$table.height() / 2) + 'px');
    },
    outoftime: _.debounce(function() {
      lichess.socket.send('outoftime');
    }, 200),
    initClocks: function() {
      if (!this.hasClock()) return;
      var self = this;
      self.$table.find('div.clock').each(function() {
        var $c = $(this);
        $c.clock({
          showTenths: self.options.clockTenths,
          showBar: self.options.clockBar,
          time: $c.data('time'),
          barTime: $c.data('bar-time'),
          emerg: $c.data('emerg'),
          buzzer: function() {
            if (!self.options.game.finished && !self.options.player.spectator) {
              self.outoftime();
            }
          }
        });
      });
      self.updateClocks();
    },
    updateClocks: function(times) {
      var self = this;
      if (times || false) {
        for (var color in times) {
          self.$table.find('div.clock_' + color).clock('setTime', times[color]);
        }
      }
      self.$table.find('div.clock').clock('stop');
      if (self.hasClock() && !self.options.game.finished && ((self.options.game.turns - self.options.game.startedAtTurn) > 1 || self.options.game.clockRunning)) {
        self.$table.find('div.clock_' + self.options.game.player).clock('start');
      }
    },
    hasClock: function() {
      return this.options.game.clock && this.options.game.started;
    },
    getPieceColor: function($piece) {
      return $piece.hasClass('white') ? 'white' : 'black';
    },
    getPieceRole: function($piece) {
      var klass = $piece[0].className;
      return _.find(['pawn', 'knight', 'bishop', 'rook', 'queen', 'king'], function(r) {
        return klass.indexOf(r) != -1;
      });
    },
    getSquareCoords: function(square) {
      return {
        x: 'abcdefgh'.indexOf(square[0]) + 1,
        y: parseInt(square[1], 10)
      };
    },
    isPlayerColor: function(color) {
      return !this.options.player.spectator && this.options.player.color == color;
    },
    get: function(url, options, reloadIfFail) {
      var self = this;
      options = $.extend({
          type: 'GET',
          timeout: 8000,
          cache: false
        },
        options || {});
      $.ajax(url, options).complete(function(x, s) {
        self.onXhrComplete(x, s, null, reloadIfFail);
      });
    },
    post: function(url, options, reloadIfFail) {
      var self = this;
      options = $.extend({
          type: 'POST',
          timeout: 8000
        },
        options || {});
      $.ajax(url, options).complete(function(x, s) {
        self.onXhrComplete(x, s, 'ok', reloadIfFail);
      });
    },
    onXhrComplete: function(xhr, status, expectation, reloadIfFail) {
      if (status != 'success') {
        this.onError('status is not success: ' + status, reloadIfFail);
      }
      if ((expectation || false) && expectation != xhr.responseText) {
        this.onError('expectation failed: ' + xhr.responseText, reloadIfFail);
      }
    },
    onError: function(error, reloadIfFail) {
      var self = this;
      if (reloadIfFail) {
        lichess.reload();
      }
    }
  });

  $.widget("lichess.watchers", {
    _create: function() {
      this.list = this.element.find("span.list");
    },
    set: function(users) {
      var self = this;
      if (users.length > 0) {
        self.list.html(_.map(users, function(u) {
          return u.indexOf('(') === -1 ? $.userLink(u) : u.replace(/\s\(1\)/, '');
        }).join(", "));
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
          storage.set('friends-hide', $(this).is(':visible') ? 0 : 1);
        });
      });
      if (storage.get('friends-hide') == 1) self.$title.click();
      self.$nbOnline = self.$title.find('.online');
      self.$nobody = self.element.find("div.nobody");
      self.set(_.compact(self.element.data('preload').split(',')));
    },
    repaint: function() {
      this.users = _.uniq(this.users);
      this.$nbOnline.text(this.users.length);
      this.$nobody.toggle(this.users.length === 0);
      this.$list.html(_.map(this.users, this._renderUser).join(""));
      $('body').trigger('lichess.content_loaded');
    },
    set: function(us) {
      this.users = us;
      this.repaint();
    },
    enters: function(user) {
      this.users.push(user);
      this.repaint();
    },
    leaves: function(user) {
      this.users = _.without(this.users, user);
      this.repaint();
    },
    _renderUser: function(user) {
      var id = _.last(user.split(' '));
      return '<a class="ulpt" data-placement="nw" href="/@/' + id + '">' + user + '</a>';
    }
  });

  $.widget("lichess.chat", {
    _create: function() {
      this.options = $.extend({
        messages: [],
        resize: false
      }, this.options);
      var self = this;
      self.$msgs = self.element.find('.messages');
      self.$msgs.on('click', 'a', function() {
        $(this).attr('target', '_blank');
      });
      if (self.options.resize) self.resize();
      var $form = self.element.find('form');
      var $input = self.element.find('input.lichess_say');

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
      var $toggle = self.element.find('input.toggle_chat');
      $toggle.change(function() {
        var enabled = $toggle.is(':checked');
        self.element.toggleClass('hidden', !enabled);
        if (!enabled) storage.set('nochat', 1);
        else storage.remove('nochat');
      });
      $toggle[0].checked = storage.get('nochat') != 1;
      if (!$toggle[0].checked) {
        self.element.addClass('hidden');
      }
      if (self.options.messages.length > 0) self._appendMany(self.options.messages);
    },
    resize: function() {
      var self = this;
      setTimeout(function() {
        var headHeight = self.element.parent().height();
        self.element.css("top", headHeight + 13);
        self.$msgs.css('height', 459 - headHeight).scrollTop(999999);
      }, 10);
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

  var startTournamentClock = function() {
    $("div.game_tournament div.clock").each(function() {
      $(this).clock({
        time: $(this).data("time"),
        showTenths: false
      }).clock("start");
    });
  };

  $.widget("lichess.clock", {
    _create: function() {
      var o = this.options;
      this.options.time = parseFloat(this.options.time) * 1000;
      this.options.barTime = parseFloat(this.options.barTime) * 1000;
      this.options.emerg = parseFloat(this.options.emerg) * 1000;
      $.extend(this.options, {
        state: 'ready'
      });
      this.$time = this.element.find('>div.time');
      this.$bar = this.element.find('>div.bar>span');
      this._show();
    },
    destroy: function() {
      this.stop();
      $.Widget.prototype.destroy.apply(this);
    },
    start: function() {
      var self = this;
      self.options.state = 'running';
      self.element.addClass('running');
      var end_time = new Date().getTime() + self.options.time;
      self.options.interval = setInterval(function() {
          if (self.options.state == 'running') {
            var current_time = Math.round(end_time - new Date().getTime());
            if (current_time <= 0) {
              clearInterval(self.options.interval);
              current_time = 0;
            }

            self.options.time = current_time;
            self._show();

            //If the timer completed, fire the buzzer callback
            if (current_time === 0 && $.isFunction(self.options.buzzer)) self.options.buzzer(self.element);
          } else {
            clearInterval(self.options.interval);
          }
        },
        100);
    },

    setTime: function(time) {
      this.options.time = parseFloat(time) * 1000;
      this._show();
    },

    getSeconds: function() {
      return Math.round(this.options.time / 1000);
    },

    stop: function() {
      clearInterval(this.options.interval);
      this.options.state = 'stop';
      this.element.removeClass('running');
      this.element.toggleClass('outoftime', this.options.time <= 0);
    },

    _show: function() {
      var html = this._formatDate(new Date(this.options.time));
      if (html != this.$time.html()) {
        this.$time.html(html);
        this.element.toggleClass('emerg', this.options.time < this.options.emerg);
      }
      if (this.options.showBar) {
        var barWidth = Math.max(0, Math.min(100, (this.options.time / this.options.barTime) * 100));
        this.$bar.css('width', barWidth + '%');
      }
    },

    _formatDate: function(date) {
      var minutes = this._prefixInteger(date.getUTCMinutes(), 2);
      var seconds = this._prefixInteger(date.getSeconds(), 2);
      if (this.options.showTenths && this.options.time < 10000) {
        tenths = Math.floor(date.getMilliseconds() / 100);
        return minutes + ':' + seconds + '<span>.' + tenths + '</span>';
      } else if (this.options.time >= 3600000) {
        var hours = this._prefixInteger(date.getUTCHours(), 2);
        return hours + ':' + minutes + ':' + seconds;
      } else {
        return minutes + ':' + seconds;
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
    parseFen();
    $('body').on('lichess.content_loaded', parseFen);

    var socketOpened = false;

    function registerLiveGames() {
      if (!socketOpened) return;
      var ids = [];
      $('a.mini_board.live').removeClass("live").each(function() {
        ids.push($(this).data("live"));
      });
      if (ids.length > 0) {
        lichess.socket.send("liveGames", ids.join(" "));
      }
    }
    $('body').on('lichess.content_loaded', registerLiveGames);
    $('body').on('socket.open', function() {
      socketOpened = true;
      registerLiveGames();
    });

    $('div.checkmateCaptcha').each(function() {
      var $captcha = $(this);
      var $board = $captcha.find('.mini_board');
      var color = $board.data('color');
      var $squares = $captcha.find('.mini_board > div');
      var $input = $captcha.find('input').val('');
      var submit = function(s) {
        $input.val(s);
        $.ajax({
          url: $captcha.data('check-url'),
          data: {
            solution: $input.val()
          },
          success: function(data) {
            $captcha.toggleClass('success', data == 1);
            $captcha.toggleClass('failure', data != 1);
            if (data == 1) {
              $board.find('.ui-draggable').draggable('destroy');
            } else {
              parseFen($board);
              boardInteractions();
            }
          }
        });
      };
      $captcha.find('button.retry').click(function() {
        $input.val("");
        $squares.removeClass('moved');
        $captcha.removeClass("success failure");
        return false;
      });
      var boardInteractions = function() {
        $board.find('.lcmp.' + color).draggable({
          revert: 'invalid',
          scroll: false,
          distance: 10
        });
        $board.find('> div').droppable({
          drop: function(ev, ui) {
            submit(ui.draggable.parent().data('key') + ' ' + $(this).data('key'));
            $(this).empty().append(ui.draggable.css({
              top: 0,
              left: 0
            }));
          },
          hoverClass: 'moved'
        });
      };
      boardInteractions();
      $board.on('click', '> div', function() {
        var key = $(this).data('key');
        $captcha.removeClass("success failure");
        if ($input.val().length == 2) {
          submit($.trim($input.val() + " " + key));
        } else {
          $squares.removeClass('moved');
          // first click must be on an own piece
          if (!$(this).find('.' + color).length) return;
          $input.val(key);
        }
        $(this).addClass('moved');
      });
    });
  });

  ////////////////
  // lobby.js //
  ////////////////

  $(function() {

    var $startButtons = $('#start_buttons');

    if (!strongSocket.available) {
      $startButtons.find('a').attr('href', '#');
      $("div.lichess_overboard.joining input.submit").remove();
      return;
    }

    if (!$startButtons.length) {
      return;
    }

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

    function prepareForm() {
      var $form = $('div.lichess_overboard');
      var $modeChoicesWrap = $form.find('.mode_choice');
      var $modeChoices = $modeChoicesWrap.find('input');
      var $variantChoices = $form.find('.variants input');
      var $casual = $modeChoices.eq(0),
        $rated = $modeChoices.eq(1);
      var $fenVariant = $variantChoices.eq(2);
      var $fenPosition = $form.find(".fen_position");
      var $clockCheckbox = $form.find('.clock_choice input');
      var $timeInput = $form.find('.time_choice input');
      var $incrementInput = $form.find('.increment_choice input');
      var isHook = $form.hasClass('game_config_hook');
      var myRating = parseInt($('#user_tag').data('rating'), 10);
      if (isHook) {
        var $formTag = $form.find('form');

        var ajaxSubmit = function(color) {
          $.ajax({
            url: $formTag.attr('action').replace(/uid-placeholder/, lichess_sri),
            data: $formTag.serialize() + "&color=" + color,
            type: 'post'
          });
          $form.find('a.close').click();
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
          $value = $input.siblings('span'),
          time;
        $input.hide().after($('<div>').slider({
          value: $input.val(),
          min: 0,
          max: 30,
          range: 'min',
          step: 1,
          slide: function(event, ui) {
            time = sliderTime(ui.value);
            $value.text(time);
            $input.attr('value', time);
            $form.find('.color_submits button').toggle(
              $timeInput.val() > 0 || $incrementInput.val() > 0);
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
          if (isHook && rated && !$clockCheckbox.prop('checked')) {
            $clockCheckbox.click();
          }
          $form.find('.members_only').toggle(!rated);
          $.centerOverboard();
        }).trigger('change');
      });
      $clockCheckbox.on('change', function() {
        var checked = $(this).is(':checked');
        $form.find('.time_choice, .increment_choice').toggle(checked);
        if (isHook && !checked) {
          $casual.click();
        }
        $.centerOverboard();
      }).trigger('change');
      var $ratingRangeConfig = $form.find('.rating_range_config');
      var $fenInput = $fenPosition.find('input');

      var validateFen = _.debounce(function() {
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
              $.centerOverboard();
            },
            error: function() {
              $fenInput.addClass("failure");
              $fenPosition.find('.preview').html("");
              $.centerOverboard();
            }
          });
        }
      }, 500);
      $fenInput.on('keyup', validateFen);

      $variantChoices.on('change', function() {
        var fen = $fenVariant.prop('checked');
        if (fen && $fenInput.val() !== '') validateFen();
        $fenPosition.toggle(fen);
        $modeChoicesWrap.toggle(!fen);
        if (fen) $casual.click();
        $.centerOverboard();
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

      $form.prepend($('<a class="close" data-icon="L"></a>').click(function() {
        $form.remove();
        $startButtons.find('a.active').removeClass('active');
      }));
    }

    $startButtons.find('a').click(function() {
      $startButtons.find('a.active').removeClass('active');
      $(this).addClass('active');
      $('div.lichess_overboard').remove();
      $.ajax({
        url: $(this).attr('href'),
        success: function(html) {
          $('div.lichess_overboard').remove();
          $('div.lichess_board_wrap').prepend(html);
          prepareForm();
          $.centerOverboard();
        }
      });
      return false;
    });
    $('#lichess').on('submit', 'form', $.lichessOpeningPreventClicks);

    if (window.location.hash) {
      $startButtons
        .find('a.config_' + location.hash.replace(/#/, ''))
        .each(function() {
          $(this).attr("href", $(this).attr("href") + location.search);
        }).click();
    }
  });

  $.lichessOpeningPreventClicks = function() {
    $('#hooks_table, #hooks_chart').hide();
  };

  // hooks
  $(function() {

    var $wrap = $('#hooks_wrap');
    if (!$wrap.length) return;
    if (!strongSocket.available) return;

    var $timeline = $("#timeline");
    var $newposts = $("div.new_posts");
    var $canvas = $wrap.find('.canvas');
    var $table = $wrap.find('#hooks_table').sortable();
    var $tablereload = $table.find('th.reload');
    var $tbody = $table.find('tbody');
    var $userTag = $('#user_tag');
    var isRegistered = $userTag.length > 0;
    var myRating = isRegistered ? parseInt($userTag.data('rating'), 10) : null;
    var animation = 500;
    var pool = [];
    var nextHooks = [];

    var flushHooksTimeout;
    var flushHooksSchedule = function() {
      flushHooksTimeout = setTimeout(flushHooks, 8000);
    };
    var flushHooks = function() {
      clearTimeout(flushHooksTimeout);
      $tbody.fadeIn(500);
      $table.clone().attr('id', 'tableclone').appendTo($wrap).fadeOut(500, function() {
        $(this).remove();
      });
      $tbody.find('tr.disabled').remove();
      $tbody.append(nextHooks);
      nextHooks = [];
      $table.trigger('sortable.sort');
      flushHooksSchedule();
    };
    flushHooksSchedule();
    $tablereload.click(function(e) {
      e.stopPropagation();
      flushHooks();
    });
    $('body').on('lichess.hook-flush', flushHooks);

    $wrap.on('click', '>div.tabs>a', function() {
      var tab = $(this).data('tab');
      $(this).siblings().removeClass('active').end().addClass('active');
      $wrap.find('>.tab').hide().filter('.' + tab).show();
      storage.set('lobbytab', tab);
    });
    var active = storage.get('lobbytab') || 'list';
    $wrap.find('>div.tabs>.' + active).addClass('active');
    $wrap.find('>.' + active).show();
    $wrap.find('a.filter').click(function() {
      var $a = $(this);
      var $div = $wrap.find('#hook_filter');
      setTimeout(function() {
        $div.click(function(e) {
          e.stopPropagation();
        });
        $('html').one('click', function(e) {
          $div.off('click').removeClass('active');
          $a.removeClass('active');
        });
      }, 10);
      if ($(this).toggleClass('active').hasClass('active')) {
        $div.addClass('active');
        if ($div.is(':empty')) {
          $.ajax({
            url: $(this).attr('href'),
            success: function(html) {
              var save = _.throttle(function() {
                var $form = $div.find('form');
                $.ajax({
                  url: $form.attr('action'),
                  data: $form.serialize(),
                  type: 'post',
                  success: function(filter) {
                    lichess_preload.filter = filter;
                    drawHooks();
                    $('body').trigger('lichess.hook-flush');
                  }
                });
              }, 500);
              $div.html(html).find('input').change(save);
              $div.find('button.reset').click(function() {
                $div.find('label input').prop('checked', true).trigger('change');
                $div.find('.rating_range').each(function() {
                  var s = $(this);
                  s.slider('values', [s.slider('option', 'min'), s.slider('option', 'max')]).trigger('change');
                });
              });
              $div.find('button').click(function() {
                $wrap.find('a.filter').click();
                return false;
              });
              $div.find('.rating_range').each(function() {
                var $this = $(this);
                var $input = $this.find("input");
                var $span = $this.siblings(".range");
                var min = $input.data("min");
                var max = $input.data("max");
                var values = $input.val() ? $input.val().split("-") : [min, max];
                $span.text(values.join(' - '));

                function change() {
                  setTimeout(function() {
                    var values = $this.slider('values');
                    $input.val(values[0] + "-" + values[1]);
                    $span.text(values[0] + " - " + values[1]);
                    save();
                  }, 50);
                }
                $this.slider({
                  range: true,
                  min: min,
                  max: max,
                  values: values,
                  step: 50,
                  slide: change
                }).change(change);
              });
            }
          });
        }
      } else {
        $div.removeClass('active');
      }
      return false;
    });

    function resizeTimeline() {
      if ($timeline.length) {
        var pos = $timeline.offset().top,
          max = $('#lichess').offset().top + 536;
        while (pos + $timeline.outerHeight() > max) {
          $timeline.find('div.entry:last').remove();
        }
      }
    }
    resizeTimeline();

    _.each(lichess_preload.pool, addHook);
    drawHooks(true);
    $table.find('th:eq(2)').click().end();

    lichess.socket = new strongSocket("/lobby/socket", lichess_preload.version, $.extend(true, lichess.socketDefaults, {
      events: {
        reload_timeline: function() {
          $.ajax({
            url: $timeline.data('href'),
            success: function(html) {
              $timeline.html(html);
              resizeTimeline();
              $('body').trigger('lichess.content_loaded');
            }
          });
        },
        streams: function(html) {
          $('#streams_on_air').html(html);
        },
        hook_add: function(hook) {
          addHook(hook);
          drawHooks();
          if (hook.action == 'cancel') $('body').trigger('lichess.hook-flush');
        },
        hook_remove: removeHook,
        hook_list: syncHookIds,
        featured: changeFeatured,
        redirect: function(e) {
          $.lichessOpeningPreventClicks();
          $.redirect(e);
        },
        tournaments: reloadTournaments,
        reload_forum: reloadForum
      },
      options: {
        name: "lobby"
      }
    }));

    function reloadTournaments(data) {
      $("#enterable_tournaments").html(data);
      $('body').trigger('lichess.content_loaded');
    }

    function reloadForum() {
      setTimeout(function() {
        $.ajax($newposts.data('url'), {
          success: function(data) {
            $newposts.find('ol').html(data).end().scrollTop(0);
            $('body').trigger('lichess.content_loaded');
          }
        });
      }, Math.round(Math.random() * 5000));
    }

    function changeFeatured(o) {
      $('#featured_game').html(o.html);
      $('body').trigger('lichess.content_loaded');
    }

    function removeHook(id) {
      pool = _.reject(pool, function(h) {
        return h.id == id;
      });
      drawHooks();
    }

    function syncHookIds(ids) {
      pool = _.filter(pool, function(h) {
        return _.contains(ids, h.id);
      });
      drawHooks();
    }

    function addHook(hook) {
      if (!isRegistered && hook.mode == "Casual" && !hook.allowAnon) return;
      if (_.contains(lichess_preload.blocks, hook.username.toLowerCase())) return;
      if (!isRegistered && hook.mode == "Rated") hook.action = 'register';
      else hook.action = hook.uid == lichess_sri ? "cancel" : "join";
      if (hook.action == 'join' && hook.emin && myRating && (myRating < parseInt(hook.emin, 10) || myRating > parseInt(hook.emax, 10))) return;
      pool.push(hook);
    }

    function disableHook(id) {
      $tbody.children('.' + id).addClass('disabled').attr('title', '');
      destroyChartHook(id);
    }

    function destroyHook(id) {
      $tbody.children('.' + id).remove();
      destroyChartHook(id);
    }

    function destroyChartHook(id) {
      $('#' + id).not('.hiding').addClass('hiding').fadeOut(animation, function() {
        $.powerTip.destroy($(this));
        $(this).remove();
      });
    }

    function drawHooks(initial) {
      var filter = lichess_preload.filter;
      var seen = [];
      var hidden = 0;
      var visible = 0;
      _.each(pool, function(hook) {
        var hide = !_.contains(filter.variant, hook.variant) || !_.contains(filter.mode, hook.mode) || !_.contains(filter.speed, hook.speed) ||
          (filter.rating && (!hook.rating || (hook.rating && (hook.rating < filter.rating[0] || hook.rating > filter.rating[1]))));
        var hash = hook.mode + hook.variant + hook.time + hook.rating;
        if (hide && hook.action != 'cancel') {
          destroyHook(hook.id);
          hidden++;
        } else if (_.contains(seen, hash) && hook.action != 'cancel') {
          $('#' + hook.id).filter(':visible').hide();
          $tbody.children('.' + hook.id).hide();
        } else {
          visible++;
          if (!$('#' + hook.id).length) {
            $canvas.append($(renderPlot(hook)).fadeIn(animation));
            if (initial) $tbody.append(renderTr(hook));
            else nextHooks.push(renderTr(hook));
          } else {
            $('#' + hook.id).not(':visible').fadeIn(animation);
            $tbody.children('.' + hook.id).show();
          }
        }
        if (hook.action != 'cancel') seen.push(hash);
      });
      _.each(_.union(
        _.map($canvas.find('>span.plot'), function(o) {
          return $(o).attr('id');
        }),
        _.map($tbody.children(), function(o) {
          return $(o).data('id');
        })), function(id) {
        if (!_.findWhere(pool, {
          id: id
        })) disableHook(id);
      });

      $wrap
        .find('a.filter')
        .toggleClass('on', hidden > 0)
        .find('span.number').text('(' + hidden + ')');

      $('body').trigger('lichess.content_loaded');
    }

    function renderPlot(hook) {
      var bottom = Math.max(0, ratingY(hook.rating) - 7);
      var left = Math.max(0, clockX(hook.time) - 4);
      var klass = [
        'plot',
        hook.mode == "Rated" ? 'rated' : 'casual',
        hook.variant == "Chess960" ? 'chess960' : '',
        hook.action == 'cancel' ? 'cancel' : ''
      ].join(' ');
      var $plot = $('<span id="' + hook.id + '" class="' + klass + '" style="bottom:' + bottom + 'px;left:' + left + 'px;"></span>');
      return $plot.data('hook', hook).powerTip({
        fadeInTime: 0,
        fadeOutTime: 0,
        placement: 'ne',
        mouseOnToPopup: true,
        closeDelay: 200,
        intentPollInterval: 50,
        popupId: 'hook'
      }).data('powertipjq', $(renderHook(hook)));
    }

    function ratingY(e) {
      function ratingLog(a) {
        return Math.log(a / 150 + 1);
      }
      var rating = Math.max(800, Math.min(2800, e || 1500));
      var ratio;
      if (rating == 1500) {
        ratio = 0.25;
      } else if (rating > 1500) {
        ratio = 0.25 + (ratingLog(rating - 1500) / ratingLog(1300)) * 3 / 4;
      } else {
        ratio = 0.25 - (ratingLog(1500 - rating) / ratingLog(500)) / 4;
      }
      return Math.round(ratio * 489);
    }

    function clockX(dur) {
      function durLog(a) {
        return Math.log((a - 30) / 200 + 1);
      }
      var max = 2000;
      return Math.round(durLog(Math.min(max, dur || max)) / durLog(max) * 489);
    }

    function renderHook(hook) {
      var html = '';
      if (hook.rating) {
        html += '<a class="opponent" href="/@/' + hook.username + '">' + hook.username.substr(0, 14) + '</a>';
        html += '<span class="rating">' + hook.rating + '</span>';
      } else {
        html += '<span class="opponent anon">Anonymous</span>';
      }
      if (hook.clock) {
        var clock = hook.clock.replace(/\s/g, '').replace(/\+/, '<span>+</span>');
        html += '<span class="clock">' + clock + '</span>';
      } else {
        html += '<span class="clock nope">∞</span>';
      }
      html += '<span class="mode">';
      html += $.trans(hook.mode);
      if (hook.variant == 'Chess960') html += ', 960';
      html += '</span>';
      var k = hook.color ? (hook.color == "black" ? "J" : "K") : "l";
      html += '<span class="is2" data-icon="' + k + '"></span>';
      if (hook.engine && hook.action == 'join') {
        html += '<span class="is2" data-icon="j"></span>';
      }
      return html;
    }

    function renderTr(hook) {
      var title = (hook.action == "join") ? $.trans('Join the game') : $.trans('cancel');
      var k = hook.color ? (hook.color == "black" ? "J" : "K") : "l";
      return '<tr title="' + title + '"  data-id="' + hook.id + '" class="' + hook.id + ' ' + hook.action + '">' + _.map([
        ['', '<span class="is2" data-icon="' + k + '"></span>'],
        [hook.username, (hook.rating ? '<a href="/@/' + hook.username + '" class="ulink">' + hook.username + '</a>' : 'Anonymous') +
          ((hook.engine && hook.action == 'join') ? ' <span data-icon="j"></span>' : '')
        ],
        [hook.rating || 0, hook.rating || ''],
        [hook.time || 9999, hook.clock ? hook.clock : '∞'],
        [hook.mode, $.trans(hook.mode) + (hook.variant == 'Chess960' ? '<span class="chess960">960</span>' : '')]
      ], function(x) {
        return '<td data-sort-value="' + x[0] + '">' + x[1] + '</td>';
      }).join('') + '</tr>';
    }

    $('#hooks_chart').append(
      _.map([1000, 1200, 1400, 1500, 1600, 1800, 2000, 2200, 2400, 2600, 2800], function(v) {
        var b = ratingY(v);
        return '<span class="y label" style="bottom:' + (b + 5) + 'px">' + v + '</span>' +
          '<div class="grid horiz" style="height:' + (b + 4) + 'px"></div>';
      }).join('') +
      _.map([1, 2, 3, 5, 7, 10, 15, 20, 30], function(v) {
        var l = clockX(v * 60);
        return '<span class="x label" style="left:' + l + 'px">' + v + '</span>' +
          '<div class="grid vert" style="width:' + (l + 7) + 'px"></div>';
      }).join(''));

    function confirm960(hook) {
      if (hook.variant == "Chess960" && hook.action == "join" && !storage.get('c960')) {
        var c = confirm("This is a Chess960 game!\n\nThe starting position of the pieces on the players' home ranks is randomized.\nRead more: http://wikipedia.org/wiki/Chess960\n\nDo you want to play Chess960?");
        if (c) storage.set('c960', 1);
        return c;
      } else return true;
    }

    $tbody.on('click', 'a.ulink', function(e) {
      e.stopPropagation();
    });
    $tbody.on('click', 'td:not(.disabled)', function() {
      $('#' + $(this).parent().data('id')).click();
    });
    $canvas.on('click', '>span.plot:not(.hiding)', function() {
      var hook = $(this).data('hook');
      if (lichess_preload.engine && hook.mode == 'Rated') {
        disableHook(hook.id);
        return;
      }
      if (hook.action == 'register') {
        if (confirm($.trans('This game is rated') + '.\n' + $.trans('You need an account to do that') + '.')) location.href = '/signup';
        return;
      }
      if (confirm960(hook)) {
        lichess.socket.send(hook.action, hook.id);
      }
    });
  });

  ///////////////////
  // tournament.js //
  ///////////////////

  $(function() {

    var $wrap = $('#tournament');
    if (!$wrap.length) return;

    if (!strongSocket.available) return;

    if (typeof _ld_ == "undefined") {
      // handle tournament list
      lichess.socketDefaults.params.flag = "tournament";
      lichess.socketDefaults.events.reload = function() {
        $wrap.load($wrap.data("href"), function() {
          $('body').trigger('lichess.content_loaded');
        });
      };
      return;
    }

    $('body').data('tournament-id', _ld_.tournament.id);

    var $userList = $wrap.find("div.user_list");
    var socketUrl = $wrap.data("socket-url");
    var $watchers = $("div.watchers").watchers();

    var $chat = $('#chat');
    if ($chat.length) $chat.chat({
      resize: true,
      messages: lichess_chat
    });

    function startClock() {
      $("div.tournament_clock").each(function() {
        $(this).clock({
          time: $(this).data("time")
        }).clock("start");
      });
    }
    startClock();

    function drawBars() {
      $wrap.find('table.standing').each(function() {
        var $bars = $(this).find('.bar');
        var max = _.max($bars.map(function() {
          return parseInt($(this).data('value'));
        }));
        $bars.each(function() {
          var width = Math.ceil((parseInt($(this).data('value')) * 100) / max);
          $(this).css('width', width + '%');
        });
      });
    }
    drawBars();

    function reload() {
      $wrap.load($wrap.data("href"), function() {
        startClock();
        $('body').trigger('lichess.content_loaded');
        drawBars();
      });
    }

    function start() {
      alert($.trans("Tournament is starting"));
      reload();
    }

    lichess.socket = new strongSocket(socketUrl, _ld_.version, $.extend(true, lichess.socketDefaults, {
      events: {
        start: start,
        reload: reload,
        reloadPage: function() {
          location.reload();
        },
        redirect: function(e) {
          $.redirect(e);
        },
        crowd: function(data) {
          $watchers.watchers("set", data);
        }
      },
      options: {
        name: "tournament"
      }
    }));
  });

  ////////////////
  // analyse.js //
  ////////////////

  $(function() {

    if (!$("#GameBoard").length) return;

    $('a.continue').click(function() {
      $('div.continue').toggle();
      return false;
    });

    // override to remove word boundaries (\b)
    // required to match e2e4 and highlight the moves on the board
    chessMovesRegExp = new RegExp("((\\d+(\\.{1,3}|\\s)\\s*)?((([KQRBN][a-h1-8]?)|[a-h])?x?[a-h][1-8](=[QRNB])?|O-O-O|O-O)[!?+#]*)", "g");

    SetImagePath('http://' + document.domain.replace(/^\w+/, 'static') + "/assets/images/piece");
    SetImageType("svg");
    SetShortcutKeysEnabled(true);
    $('input').on('focus', function() {
      SetShortcutKeysEnabled(false);
    }).on('blur', function() {
      SetShortcutKeysEnabled(true);
    });
    var $game = $("#GameBoard");
    $game.mousewheel(function(event) {
      if (event.deltaY == -1) {
        $('#forwardButton').click();
      } else if (event.deltaY == 1) {
        $('#backButton').click();
      }
      event.stopPropagation();
      return false;
    });
    var $chat = $('#chat');
    if ($chat.length) $chat.chat({
      resize: true,
      messages: lichess_chat
    });
    var $watchers = $("div.watchers").watchers();

    var $panels = $('div.analysis_panels > div');
    $('div.analysis_menu').on('click', 'a', function() {
      var panel = $(this).data('panel');
      $(this).siblings('.active').removeClass('active').end().addClass('active');
      $panels.removeClass('active').filter('.' + panel).addClass('active');
      if (panel == 'move_times') $.renderMoveTimesChart();
    });

    $panels.find('form.future_game_analysis').submit(function() {
      if ($(this).hasClass('must_login')) {
        if (confirm($.trans('You need an account to do that') + '.')) {
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

    lichess.socket = new strongSocket(
      $game.data("socket-url"),
      parseInt($game.data("version"), 10),
      $.extend(true, lichess.socketDefaults, {
        options: {
          name: "analyse",
          ignoreUnknownMessages: true
        },
        events: {
          analysisAvailable: function() {
            $.sound.dong();
            location.href = location.href.split('#')[0] + '#' + CurrentPly;
            location.reload();
          },
          crowd: function(event) {
            $watchers.watchers("set", event.watchers);
          }
        }
      }));
  });

  $.fn.sortable = function(sortFns) {
    return this.each(function() {
      var $table = $(this);
      var $ths = $table.find('th');

      // Enum containing sorting directions
      var dir = {
        ASC: "asc",
        DESC: "desc"
      };

      // Merge sort functions with some default sort functions.
      sortFns = $.extend({}, {
        "int": function(a, b) {
          return parseInt(a, 10) - parseInt(b, 10);
        },
        "float": function(a, b) {
          return parseFloat(a) - parseFloat(b);
        },
        "string": function(a, b) {
          if (a < b) return -1;
          if (a > b) return +1;
          return 0;
        }
      }, sortFns || {});

      // Return the resulting indexes of a sort so we can apply
      // this result elsewhere. This returns an array of index numbers.
      // return[0] = x means "arr's 0th element is now at x"

      function sort_map(arr, sort_function, sort_dir) {
        var map = [];
        var index = 0;
        var sorted = arr.slice(0).sort(sort_function);
        if (sort_dir == dir.DESC) sorted = sorted.reverse();
        for (var i = 0; i < arr.length; i++) {
          index = $.inArray(arr[i], sorted);

          // If this index is already in the map, look for the next index.
          // This handles the case of duplicate entries.
          while ($.inArray(index, map) != -1) {
            index++;
          }
          map.push(index);
        }
        return map;
      }

      // Apply a sort map to the array.

      function apply_sort_map(arr, map) {
        var clone = arr.slice(0),
          newIndex = 0;
        for (var i = 0; i < map.length; i++) {
          newIndex = map[i];
          clone[newIndex] = arr[i];
        }
        return clone;
      }

      $table.on("click", "th", function() {
        var sort_dir = $(this).data("sort-dir") === dir.DESC ? dir.ASC : dir.DESC;
        $ths.data("sort-dir", null).removeClass("sorting-desc sorting-asc");
        $(this).data("sort-dir", sort_dir).addClass("sorting-" + sort_dir);
        $table.trigger('sortable.sort');
      });

      $table.on("sortable.sort", function() {

        var $th = $ths.filter('.sorting-desc,.sorting-asc').first();
        if (!$th.length) return;
        var th_index = $th.index();

        var type = $th.data("sort");
        if (!type) return;
        var sort_dir = $th.data("sort-dir") || dir.DESC;

        var trs = $table.children("tbody").children("tr");

        setTimeout(function() {
          // Gather the elements for this column
          var column = [];
          var sortMethod = sortFns[type];

          // Push either the value of the `data-order-by` attribute if specified
          // or just the text() value in this column to column[] for comparison.
          trs.each(function(index, tr) {
            var $e = $(tr).children().eq(th_index);
            var sort_val = $e.data("sort-value");
            var order_by = typeof(sort_val) !== "undefined" ? sort_val : $e.text();
            column.push(order_by);
          });

          var theMap = sort_map(column, sortMethod, sort_dir);

          $table.children("tbody").html($(apply_sort_map(trs, theMap)));
        }, 10);
      });
    });
  };

})();
