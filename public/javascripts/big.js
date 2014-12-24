// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

// declare now, populate later in a distinct script.
var lichess_translations = lichess_translations || [];

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

  var lichess = window.lichess = window.lichess || {};
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
        var $tag = $('#nb_connected_players > strong');
        if ($tag.length && e) {
          var prev = parseInt($tag.text(), 10) || Math.max(0, (e - 10));
          var k = 6;
          var interv = lichess.socket.pingInterval() / k;
          $.fp.range(k).forEach(function(it) {
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
        }
      },
      challengeReminder: function(data) {
        if (!storage.get('challenge-refused-' + data.id)) {
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
              storage.set('challenge-refused-' + data.id, 1);
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
            declineListener($notif.find('a.decline'));
            $('body').trigger('lichess.content_loaded');
            if (!storage.get('challenge-' + data.id)) {
              $('#top .challenge_notifications').addClass('shown');
              $.sound.dong();
              storage.set('challenge-' + data.id, 1);
            }
            refreshButton();
          }
          $('div.lichess_overboard.joining.' + data.id).each(function() {
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

    if (lichess.round) startRound(document.getElementById('lichess'), lichess.round);
    else if (lichess.prelude) startPrelude(document.querySelector('.lichess_game'), lichess.prelude);
    else if (lichess.analyse) startAnalyse(document.getElementById('lichess'), lichess.analyse);
    else if (lichess.user_analysis) startUserAnalysis(document.getElementById('lichess'), lichess.user_analysis);

    setTimeout(function() {
      if (lichess.socket === null) {
        lichess.socket = new lichess.StrongSocket("/socket", 0);
      }
      $.idleTimer(lichess.idleTime, lichess.socket.destroy.bind(lichess.socket), lichess.socket.connect.bind(lichess.socket));
    }, 200);

    // themepicker
    $('#themepicker_toggle').one('click', function() {
      var $body = $('body');
      var $content = $body.children('.content');
      var $themepicker = $('#themepicker');
      var themes = $themepicker.data('themes').split(' ');
      var theme = $.fp.find(document.body.classList, function(a) {
        return $.fp.contains(themes, a);
      });
      var sets = $themepicker.data('sets').split(' ');
      var set = $.fp.find(document.body.classList, function(a) {
        return $.fp.contains(sets, a);
      });
      var theme3ds = $themepicker.data('theme3ds').split(' ');
      var theme3d = $.fp.find(document.body.classList, function(a) {
        return $.fp.contains(theme3ds, a);
      });
      var set3ds = $themepicker.data('set3ds').split(' ');
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
    });

    // Zoom
    var getZoom = function() {
      return storage.get('zoom') || 1;
    };
    var setZoom = function(v) {
      storage.set('zoom', v);

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
    var initialZoom = getZoom();
    if (initialZoom > 1) setZoom(initialZoom); // Instantiate the page's zoom

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
          success: function(list) {
            $links.prepend(list.map(function(lang) {
              var href = location.href.replace(/\/\/\w+\./, '//' + lang[0] + '.');
              var klass = $.fp.contains(langs, lang[0]) ? 'class="accepted"' : '';
              return '<li><a ' + klass + ' lang="' + lang[0] + '" href="' + href + '">' + lang[1] + '</a></li>';
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

  $.sound = (function() {
    var baseUrl = $('body').data('sound-dir') + '/';
    var a = new Audio();
    var hasOgg = !!a.canPlayType && a.canPlayType('audio/ogg; codecs="vorbis"');
    var hasMp3 = !!a.canPlayType && a.canPlayType('audio/mpeg;');
    var ext = hasOgg ? 'ogg' : 'mp3';
    var audio = {
      dong: new Audio(baseUrl + 'dong2.' + ext),
      moveW: new Audio(baseUrl + 'move3.' + ext),
      moveB: new Audio(baseUrl + 'move3.' + ext),
      take: new Audio(baseUrl + 'take2.' + ext),
      lowtime: new Audio(baseUrl + 'lowtime.' + ext)
    };
    var volumes = {
      lowtime: 0.6
    };
    var canPlay = hasOgg || hasMp3;
    var $control = $('#sound_control');
    var $toggle = $('#sound_state');
    $control.add($toggle).toggleClass('sound_state_on', storage.get('sound') == 1);
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
      },
      lowtime: function() {
        if (shouldPlay()) audio.lowtime.play();
      }
    };
    var getVolume = function() {
      return storage.get('sound-volume') || 0.8;
    };
    var setVolume = function(v) {
      storage.set('sound-volume', v);
      Object.keys(audio).forEach(function(k) {
        audio[k].volume = v * (volumes[k] ? volumes[k] : 1);
      });
    };
    var manuallySetVolume = $.fp.debounce(function(v) {
      setVolume(v);
      play.move(true);
    }, 100);
    setVolume(getVolume());
    if (canPlay) {
      $toggle.click(function() {
        $control.add($toggle).toggleClass('sound_state_on', !enabled());
        if (enabled()) storage.set('sound', 1);
        else storage.remove('sound');
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
        time: $(this).data("time"),
        showTenths: false
      }).clock("start");
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
            $.get(url, function(html) {
              $('#site_header div.side').replaceWith(html);
              $('body').trigger('lichess.content_loaded');
              startTournamentClock();
            });
          },
          checkCount: function(e) {
            $('div.check_count')
              .find('.white').text(e.black).end()
              .find('.black').text(e.white);
          },
          opponent_play: function(e) {
            $.ajax({
              url: $nowPlaying.data('reload-url'),
              success: function(html) {
                $nowPlaying.html(html);
                $('body').trigger('lichess.content_loaded');
                loadPlaying();
                var nextId = $nowPlaying.find('input.next_id').val();
                if (nextId) round.moveOn.next(nextId);
              }
            });
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
          storage.set('friends-hide', $(this).is(':visible') ? 0 : 1);
        });
      });
      if (storage.get('friends-hide') == 1) self.$title.click();
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
        if (!enabled) storage.set('nochat', 1);
        else storage.remove('nochat');
      });
      $toggle[0].checked = storage.get('nochat') != 1;
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
      var b = function(x) {
        return '<b>' + x + '</b>';
      };
      if (this.options.showTenths && this.options.time < 10000) {
        tenths = Math.floor(date.getMilliseconds() / 100);
        return b(minutes) + ':' + b(seconds) + '<span>.' + b(tenths) + '</span>';
      } else if (this.options.time >= 3600000) {
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
    parseFen();
    $('body').on('lichess.content_loaded', parseFen);

    var socketOpened = false;

    function startWatching() {
      if (!socketOpened) return;
      var ids = [];
      $('.mini_board.live').removeClass("live").each(function() {
        ids.push(this.getAttribute("data-live"));
      });
      if (ids.length > 0) {
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
                parseFen($board);
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

  ////////////////
  // lobby.js //
  ////////////////

  $(function() {

    var $startButtons = $('#start_buttons');

    if (!lichess.StrongSocket.available) {
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

    function prepareForm() {
      var $form = $('div.lichess_overboard');
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
          case '6':
            key = 'antichess'
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
          if ($timeModeSelect.val() == '1')
            $('#hooks_wrap .tabs .real_time').click();
          else
            $('#hooks_wrap .tabs .seeks').click();
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
          value: $input.val(),
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
          value: $input.val(),
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
      $('div.lichess_overboard').remove();
      $.ajax({
        url: $(this).attr('href'),
        success: function(html) {
          $('div.lichess_overboard').remove();
          $('#hooks_wrap').prepend(html);
          prepareForm();
          $('body').trigger('lichess.content_loaded');
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
    $('#hooks_list, #hooks_chart').hide();
  };

  // hooks
  $(function() {

    var $wrap = $('#hooks_wrap');
    if (!$wrap.length) return;
    if (!lichess.StrongSocket.available) return;

    var socketUrl = $wrap.data('socket-url');
    var $nowPlaying = $('#now_playing');
    var $seeks = $('#seeks');
    var $timeline = $("#timeline");
    var $newposts = $("div.new_posts");
    var $realTime = $('#real_time');
    var $canvas = $wrap.find('.canvas');
    var $hooksList = $wrap.find('#hooks_list');
    var $tableWrap = $hooksList.find('div.table_wrap');
    var $table = $tableWrap.find('table').sortable();
    var $tbody = $table.find('tbody');
    var animation = 500;
    var hookPool = [];
    var nextHooks = [];

    var flushHooksTimeout;
    var flushHooksSchedule = function() {
      flushHooksTimeout = setTimeout(flushHooks, 8000);
    };
    var flushHooks = function() {
      clearTimeout(flushHooksTimeout);
      $tbody.fadeIn(500);
      $tableWrap.clone().attr('id', 'tableclone').appendTo($hooksList).fadeOut(500, function() {
        $(this).remove();
      });
      $tbody.find('tr.disabled').remove();
      $tbody.append(nextHooks);
      nextHooks = [];
      $table.trigger('sortable.sort');
      flushHooksSchedule();
    };
    flushHooksSchedule();
    $('body').on('lichess.hook-flush', flushHooks);

    $wrap.on('click', '>div.tabs>a', function() {
      var tab = $(this).data('tab');
      $(this).siblings().removeClass('active').end().addClass('active');
      $wrap.find('>.tab').hide().filter('.' + tab).show();
      storage.set('lobbytab', tab);
      reloadSeeksIfVisible();
    });
    var active = storage.get('lobbytab');
    if (['real_time', 'seeks', 'now_playing'].indexOf(active) === -1) active = 'real_time';
    if (!$wrap.find('>div.tabs>.' + active).length) active = 'real_time';
    $wrap.find('>div.tabs>.' + active).addClass('active');
    $wrap.find('>.' + active).show();

    $realTime.on('click', '.toggle', function() {
      var mode = $(this).data('mode');
      $realTime.children().hide().filter('#hooks_' + mode).show();
      storage.set('lobbymode', mode);
    });
    var mode = storage.get('lobbymode') || 'list';
    $('#hooks_' + mode).show();

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
              var save = $.fp.debounce(function() {
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

    lichess_preload.hookPool.forEach(addHook);
    drawHooks(true);
    $table.find('th:eq(2)').click().end();

    lichess.socket = new lichess.StrongSocket(socketUrl, lichess_preload.version, {
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
        tournaments: function(data) {
          $("#enterable_tournaments").html(data);
          $('body').trigger('lichess.content_loaded');
        },
        reload_forum: function() {
          setTimeout(function() {
            $.ajax($newposts.data('url'), {
              success: function(data) {
                $newposts.find('ol').html(data).end().scrollTop(0);
                $('body').trigger('lichess.content_loaded');
              }
            });
          }, Math.round(Math.random() * 5000));
        },
        reload_seeks: reloadSeeksIfVisible,
        nbr: function(e) {
          var $tag = $('#site_baseline span');
          if ($tag.length && e) {
            var prev = parseInt($tag.text(), 10);
            var k = 5;
            var interv = 2000 / k;
            $.fp.range(k).forEach(function(it) {
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
        // override fen event to reload playing games list
        fen: function(e) {
          lichess.StrongSocket.defaults.events.fen(e);
          if ($nowPlaying.find('.mini_board_' + e.id).length) $.ajax({
            url: $nowPlaying.data('href'),
            success: function(html) {
              $nowPlaying.html(html);
              $('body').trigger('lichess.content_loaded');
              var nb = $nowPlaying.find('.my_turn').length;
              $wrap.find('.tabs .now_playing').toggleClass('hilight', nb).find('.unread').text(nb);
            }
          });
        }
      },
      options: {
        name: "lobby"
      }
    });

    var variantConfirms = {
      '960': "This is a Chess960 game!\n\nThe starting position of the pieces on the players' home ranks is randomized.\nRead more: http://wikipedia.org/wiki/Chess960\n\nDo you want to play Chess960?",
      'KotH': "This is a King of the Hill game!\n\nThe game can be won by bringing the king to the center.\nRead more: http://lichess.org/king-of-the-hill",
      '3+': "This is a Three-check game!\n\nThe game can be won by checking the opponent 3 times.\nRead more: http://en.wikipedia.org/wiki/Three-check_chess",
      "antichess": "This is a antichess chess game!\n\n If can take a piece, you must. The game can be won by losing all your pieces."
    };

    function confirmVariant(variant) {
      return Object.keys(variantConfirms).every(function(key) {
        var v = variantConfirms[key]
        if (variant == key && !storage.get(key)) {
          var c = confirm(v);
          if (c) storage.set(key, 1);
          return c;
        } else return true;
      })
    }

    $seeks.on('click', 'tr', function() {
      if ($(this).hasClass('must_login')) {
        if (confirm($.trans('You need an account to do that'))) {
          location.href = '/signup';
        }
        return false;
      }
      if ($(this).data('action') != 'joinSeek' || confirmVariant($(this).data('variant'))) {
        lichess.socket.send($(this).data('action'), $(this).data('id'));
      }
    });

    function reloadSeeksIfVisible() {
      if ($seeks.is(':visible')) {
        $.ajax($seeks.data('reload-url'), {
          success: function(html) {
            $seeks.html(html);
          }
        });
      }
    }

    function changeFeatured(o) {
      $('#featured_game').html(o.html);
      $('body').trigger('lichess.content_loaded');
    }

    function removeHook(id) {
      hookPool = hookPool.filter(function(h) {
        return h.id != id;
      });
      drawHooks();
    }

    function syncHookIds(ids) {
      hookPool = hookPool.filter(function(h) {
        return $.fp.contains(ids, h.id);
      });
      drawHooks();
    }

    function addHook(hook) {
      hook.action = hook.uid == lichess.StrongSocket.sri ? "cancel" : "join";
      hookPool.push(hook);
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
      hookPool.forEach(function(hook) {
        var hide = !$.fp.contains(filter.variant, hook.variant) || !$.fp.contains(filter.mode, hook.mode) || !$.fp.contains(filter.speed, hook.speed) ||
          (filter.rating && (!hook.rating || (hook.rating < filter.rating[0] || hook.rating > filter.rating[1])));
        var hash = hook.mode + hook.variant + hook.time + hook.rating;
        if (hide && hook.action != 'cancel') {
          destroyHook(hook.id);
          hidden++;
        } else if ($.fp.contains(seen, hash) && hook.action != 'cancel') {
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
      $.makeArray($canvas.find('>span.plot')).map(function(o) {
        return o.getAttribute('data-id');
      }).concat(
        $.makeArray($tbody.children()).map(function(o) {
          return o.getAttribute('data-id');
        })).forEach(function(id) {
        if (!$.fp.find(hookPool, function(x) {
          return x.id == id;
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
        hook.action == 'cancel' ? 'cancel' : '',
        hook.variant != 'STD' ? 'variant' : ''
      ].join(' ');
      var $plot = $('<span id="' + hook.id + '" class="' + klass + '" style="bottom:' + bottom + 'px;left:' + left + 'px;"></span>');
      return $plot.data('hook', hook).powerTip({
        fadeInTime: 0,
        fadeOutTime: 0,
        placement: hook.rating > 2200 ? 'se' : 'ne',
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
      html += '<span class="clock">' + hook.clock + '</span>';
      html += '<span class="mode">' +
        '<span class="varicon" data-icon="' + hook.perf.icon + '"></span>' + $.trans(hook.mode) + '</span>';
      html += '<span class="is is2 color-icon ' + (hook.color || "random") + '"></span>';
      return html;
    }

    function renderTr(hook) {
      var title = (hook.action == "join") ? $.trans('Join the game') + ' - ' + hook.perf.name : $.trans('cancel');
      return '<tr title="' + title + '"  data-id="' + hook.id + '" class="' + hook.id + ' ' + hook.action + '">' + [
        ['', '<span class="is is2 color-icon ' + (hook.color || "random") + '"></span>'],
        [hook.username, (hook.rating ? '<a href="/@/' + hook.username + '" class="ulink">' + hook.username + '</a>' : 'Anonymous')],
        [hook.rating || 0, hook.rating ? hook.rating : ''],
        [hook.time, hook.clock],
        [hook.mode,
          '<span class="varicon" data-icon="' + hook.perf.icon + '"></span>' +
          $.trans(hook.mode)
        ]
      ].map(function(x) {
        return '<td data-sort-value="' + x[0] + '">' + x[1] + '</td>';
      }).join('') + '</tr>';
    }

    $('#hooks_chart').append(
      [1000, 1200, 1400, 1500, 1600, 1800, 2000, 2200, 2400].map(function(v) {
        var b = ratingY(v);
        return '<span class="y label" style="bottom:' + (b + 5) + 'px">' + v + '</span>' +
          '<div class="grid horiz" style="height:' + (b + 4) + 'px"></div>';
      }).join('') + [1, 2, 3, 5, 7, 10, 15, 20, 30].map(function(v) {
        var l = clockX(v * 60);
        return '<span class="x label" style="left:' + l + 'px">' + v + '</span>' +
          '<div class="grid vert" style="width:' + (l + 7) + 'px"></div>';
      }).join(''));

    $tbody.on('click', 'a.ulink', function(e) {
      e.stopPropagation();
    });
    $tbody.on('click', 'td:not(.disabled)', function() {
      $('#' + $(this).parent().data('id')).click();
    });
    $canvas.on('click', '>span.plot:not(.hiding)', function() {
      var data = $(this).data('hook');
      if (data.action != 'join' || confirmVariant(data.variant)) {
        lichess.socket.send(data.action, data.id);
      }
    });
  });

  ///////////////////
  // tournament.js //
  ///////////////////

  $(function() {

    var $wrap = $('#tournament');
    if (!$wrap.length) return;

    if (!lichess.StrongSocket.available) return;

    if (typeof _ld_ == "undefined") {
      // handle tournament list
      lichess.StrongSocket.defaults.params.flag = "tournament";
      lichess.StrongSocket.defaults.events.reload = function() {
        $wrap.load($wrap.data("href"), function() {
          $('body').trigger('lichess.content_loaded');
        });
      };
      return;
    }

    $('body').data('tournament-id', _ld_.tournament.id);

    var $watchers = $("div.watchers").watchers();

    var $chat = $('#chat');
    if ($chat.length) $chat.chat({
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
        var max = Math.max.apply(Math, $bars.map(function() {
          return parseInt(this.getAttribute('data-value'));
        }));
        $bars.each(function() {
          var width = Math.ceil((parseInt($(this).data('value')) * 100) / max);
          $(this).css('width', width + '%');
        });
      });
    }
    drawBars();

    function reload() {
      $.ajax({
        url: $wrap.data('href'),
        success: function(html) {
          var $tour = $(html);
          if ($wrap.find('table.standing').length) {
            // started
            $wrap.find('table.standing thead').replaceWith($tour.find('table.standing thead'));
            $wrap.find('table.standing tbody').replaceWith($tour.find('table.standing tbody'));
            drawBars();
            $wrap.find('div.pairings').replaceWith($tour.find('div.pairings'));
            $wrap.find('div.game_list').replaceWith($tour.find('div.game_list'));
          } else {
            // created
            $wrap.find('table.user_list').replaceWith($tour.find('table.user_list'));
          }
          $('body').trigger('lichess.content_loaded');
        }
      });
    }

    lichess.socket = new lichess.StrongSocket($wrap.data("socket-url"), _ld_.version, {
      events: {
        start: reload,
        reload: reload,
        reloadPage: function() {
          location.reload();
        },
        crowd: function(data) {
          $watchers.watchers("set", data);
        }
      },
      options: {
        name: "tournament"
      }
    });
  });

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
          ran: "--ranph--"
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
    lichess.analyse.onChange = function(fen, path) {
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
    analyse = LichessAnalyse(element.querySelector('.analyse'), cfg.data, cfg.routes, cfg.i18n, lichess.analyse.onChange);
    lichess.analyse.jump = analyse.jump;

    $('.underboard_content', element).appendTo($('.underboard .center', element)).show();
    $('.advice_summary', element).appendTo($('.underboard .right', element)).show();

    $panels = $('div.analysis_panels > div');
    $('div.analysis_menu').on('click', 'a', function() {
      var panel = $(this).data('panel');
      $(this).siblings('.active').removeClass('active').end().addClass('active');
      $panels.removeClass('active').filter('.' + panel).addClass('active');
      if (panel == 'move_times') try {
        $.renderMoveTimesChart();
      } catch (e) {}
    }).find('a:first').click();

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
})();
