// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// @externs_url http://closure-compiler.googlecode.com/svn/trunk/contrib/externs/jquery-1.7.js
// ==/ClosureCompiler==
if (typeof console == "undefined" || typeof console.log == "undefined") console = {
  log: function() {}
};

// declare now, populate later in a distinct script.
var lichess_translations = [];

(function() {

//////////////////
// websocket.js //
//////////////////

var strongSocket = function(url, version, settings) {
  var self = this;
  self.settings = {
    events: {},
    params: {
      sri: Math.random().toString(36).substring(5) // 8 chars
    },
    options: {
      name: "unnamed",
      debug: false,
      offlineDelay: 8000, // time before showing offlineTag
      offlineTag: false, // jQuery object showing connection error
      pingMaxLag: 8000, // time to wait for pong before reseting the connection
      pingDelay: 1500, // time between pong and ping
      lagTag: false, // jQuery object showing ping lag
      ignoreUnknownMessages: false
    }
  };
  $.extend(true, self.settings, settings);
  self.url = url;
  self.version = version;
  self.options = self.settings.options;
  self.ws = null;
  self.fullUrl = null;
  self.pingSchedule = null;
  self.connectSchedule = null;
  self.lastPingTime = self.now();
  self.currentLag = 0;
  self.averageLag = 0;
  self.connect();
  $(window).unload(function() {
    self.destroy();
  });
}
strongSocket.available = window.WebSocket || window.MozWebSocket;
strongSocket.prototype = {
  connect: function() { var self = this;
    self.destroy();
    self.fullUrl = "ws://" + self.url + "?" + $.param($.extend(self.settings.params, { version: self.version }));
    self.debug("connection attempt to " + self.fullUrl);
    if (window.MozWebSocket) self.ws = new MozWebSocket(self.fullUrl);
    else if (window.WebSocket) self.ws = new WebSocket(self.fullUrl);
    else throw "no websockets found on this browser!";

    self.ws.onerror = self.onError;
    self.ws.onopen = function() {
      self.debug("connected to " + self.fullUrl);
      self.onSuccess();
      if (self.options.offlineTag) self.options.offlineTag.hide();
      self.pingNow();
      $('body').trigger('socket.open');
    };
    self.ws.onmessage = function(e) {
      var m = JSON.parse(e.data);
      if (m.t == "n") { self.pong(); }
      else self.debug(m);
      if (m.t == "b") {
        $(m.d || []).each(function() { self.handle(this); });
      } else {
        self.handle(m);
      }
    };
    self.scheduleConnect(self.options.pingMaxLag);
  },
  send: function(t, d) {
    var self = this;
    var data = d || {};
    var message = JSON.stringify({t: t, d: data});
    self.debug(message);
    self.ws.send(message);
  },
  scheduleConnect: function(delay) {
    var self = this;
    clearTimeout(self.connectSchedule);
    //self.debug("schedule connect in " + delay + " ms");
    self.connectSchedule = setTimeout(function() {
      if (self.options.offlineTag) self.options.offlineTag.show();
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
      self.debug(e);
    }
    self.scheduleConnect(self.options.pingMaxLag);
  },
  pong: function() {
    var self = this;
    clearTimeout(self.connectSchedule);
    self.schedulePing(self.options.pingDelay);
    self.currentLag = self.now() - self.lastPingTime;
    if (self.options.lagTag) {
      self.options.lagTag.text(self.currentLag + " ms");
    }
    self.averageLag = self.averageLag * 0.8 + self.currentLag * 0.2;
  },
  pingData: function() {
    return JSON.stringify({t: "p", v: this.version});
  },
  handle: function(m) { var self = this;
    if (m.v) {
      if (m.v <= self.version) {
        self.debug("already has event " + m.v);
        return;
      }
      if (m.v > self.version + 1) {
        self.debug("event gap detected from " + self.version + " to " + m.v);
        return;
      }
      self.version = m.v;
      self.debug("set version " + self.version);
    }
    if (m.t) {
      if (m.t == "resync") {
        location.reload();
        return;
      }
      var h = self.settings.events[m.t];
      if ($.isFunction(h)) h(m.d || null);
      else if(!self.options.ignoreUnknownMessages) {
        self.debug(m.t + " not supported");
      }
    }
  },
  now: function() { return new Date().getTime(); },
  debug: function(msg) { if (this.options.debug) console.debug("[" + this.options.name + "]", msg); },
  destroy: function() {
    var self = this;
    clearTimeout(self.pingSchedule);
    clearTimeout(self.connectSchedule);
    if (self.ws) { self.ws.close(); self.ws = null; }
  },
  onError: function(e) {
    setTimeout(function() {
      if (!$.cookie("wsok") && $("#websocket-fail").length == 0) {
        $.ajax("/assets/websocket-fail.html", {
          success: function(html) {
            $('body').prepend("<div id='websocket-fail'>" + html + "</div>");
          }
        });
      }
    }, 1000);
  },
  onSuccess: function() {
    $.cookie("wsok", 1);
    $("#websocket-fail").remove();
  }
};

/////////////
// ctrl.js //
/////////////

$.cookie.defaults = {
  path: '/',
  domain: document.domain.replace(/^\w+\.(.+)$/, '$1')
};

var lichess = {
  socket: null,
  socketDefaults: {
    events: {
      n: function(e) {
        var $tag = $('#nb_connected_players');
        if ($tag.length && e) {
          $tag.html($tag.html().replace(/\d+/, e)).removeClass('none');
        }
      },
      nbm: function(e) {
        $('#nb_messages').text(e || "0").toggleClass("unread", e > 0);
      },
      tournamentReminder: function(data) {
        if (!$('#tournament_reminder').length && $('body').data("tournament-id") != data.id) {
          $('div.notifications').append(data.html).find("a.withdraw").click(function() {
            $.post($(this).attr("href"));
            $('#tournament_reminder').remove();
            return false;
          });
        }
      },
      analysisAvailable: function() {
        $("div.game_analysis.status").remove();
        $("div.game_analysis").show();
        $.playSound();
        document.title = "/!\\ ANALYSIS READY! " + document.title;
      }
    },
    params: {},
    options: {
      name: "site",
      offlineTag: $('#connection_lost'),
      lagTag: $('#connection_lag')
    }
  },
  onProduction: /.+\.lichess\.org/.test(document.domain),
  socketUrl: document.domain + ":9000"
};
lichess.socketDefaults.options.debug = !lichess.onProduction;

$(function() {

  if (!strongSocket.available) {
    setTimeout(function() {
      var inUrFaceUrl = window.opera ? '/assets/opera-websocket.html' : '/assets/browser.html';
      $.ajax(inUrFaceUrl, { success: function(html) { $('body').prepend(html); } });
    }, 2000);
  }

  // Start game
  var $game = $('div.lichess_game').orNot();
  if ($game) {
    $game.game(_ld_);
    if (!_ld_.player.spectator) {
      $('a.blank_if_play').click(function() {
        if ($game.game('isPlayable')) {
          $(this).attr('target', '_blank');
        }
      });
    }
  }

  setTimeout(function() {
    if (lichess.socket == null && $('div.server_error_box').length == 0) {
      lichess.socket = new strongSocket(lichess.socketUrl + "/socket", 0, lichess.socketDefaults);
    }
  }, 1000);

  $('input.lichess_id_input').select();

  if ($board = $('div.with_marks').orNot()) {
    $.displayBoardMarks($board.parent(), $('#lichess > div.lichess_player_white').length);
  }

  // themepicker
  var $body = $('body');
  var $themes = $('#top div.themepicker div.theme');
  var themes = $.makeArray($themes.map(function() { return $(this).data("theme"); }));
  var theme = $.map(document.body.className.split(/\s+/), function(a){return $.inArray(a, themes) < 0 ? null : a;})[0];
  $themes.hover(function() {
    $body.removeClass(themes.join(' ')).addClass($(this).data("theme"));
  }, function() {
    $body.removeClass(themes.join(' ')).addClass(theme);
  }).click(function() {
    theme = $(this).data("theme");
    $.post($(this).parent().data("href"), {"theme": theme});
    $('#top .themepicker').removeClass("shown");
  });

  // bgpicker
  var bgs = ["light", "dark"];
  var bg = $body.hasClass("dark") ? "dark" : "light";
  function invertBg(bg) { return bg == "dark" ? "light" : "dark"; }
  $('#top a.bgpicker').click(function() {
    bg = invertBg(bg);
    $body.removeClass(bgs.join(' ')).addClass(bg);
    $.post($(this).attr('href'), {bg: bg});
    return false;
  });

  $.centerOverboard = function() {
    if ($overboard = $('div.lichess_overboard.auto_center').orNot()) {
      $overboard.css('top', (238 - $overboard.height() / 2) + 'px').show();
    }
  };
  $.centerOverboard();

  $('.js_email').one('click', function() {
    var email = ['thibault.', 'dupl', 'essis@', 'gmail.com'].join('');
    $(this).replaceWith($('<a/>').text(email).attr('href', 'mailto:'+email));
  });

  function translateTexts() {
    $('.trans_me').each(function() {
      $(this).removeClass('trans_me').text($.trans($(this).text()));
    });
  }
  translateTexts();
  $('body').on('lichess.content_loaded', translateTexts);

  if ($autocomplete = $('input.autocomplete').orNot()) {
    $autocomplete.autocomplete({
      source: $autocomplete.attr('data-provider'),
      minLength: 2,
      delay: 100
    });
  }

  $('.infinitescroll:has(.pager a)').each(function() {
    $(this).infinitescroll({
      navSelector: ".pager",
      nextSelector: ".pager a:last",
      itemSelector: ".infinitescroll .paginated_element",
      loading: {
        msgText: "",
      img: "/assets/images/hloader3.gif",
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
        $p.click(function(e) { e.stopPropagation(); });
        $('html').one('click', function(e) { $p.removeClass('shown').off('click'); });
      }, 10);
    });
  });

  $('#lichess_translation_form_code').change(function() {
    if ("0" != $(this).val()) {
      location.href = $(this).closest('form').attr('data-change-url').replace(/__/, $(this).val());
    }
  });

  $('#incomplete_translation a.close').one('click', function() {
    $(this).parent().remove();
  });

  $('a.translation_call .close').click(function() {
    $.post($(this).data("href"));
    $(this).parent().fadeOut(500);
    return false;
  });

  $('a.delete, input.delete').click(function() {
    return confirm('Delete?');
  });
  $('input.confirm').click(function() {
    return confirm('Confirm this action?');
  });
  $('a.ipban').one("click", function() {
    var $a = $(this);
    if (confirm($a.text() + "?")) {
      $.post($a.attr('href'), function() {
        $a.text('Done').attr('href', '#');
      });
    }
    return false;
  });

  function bookmarks() {
    $('span.bookmark a.icon:not(.jsed)').each(function() {
      var t = $(this).addClass("jsed");
      t.click(function() {
        t.toggleClass("bookmarked");
        $.post(t.attr("href"));
        var count = (parseInt(t.html()) || 0) + (t.hasClass("bookmarked") ? 1 : -1);
        t.html(count > 0 ? count : "");
        return false;
      });
    });
  }
  bookmarks();
  $('body').on('lichess.content_loaded', bookmarks);

  if ($(window).width() < 1060) {
    $("div.lichess_chat").addClass("small_chat");
  }

  $("a.view_pgn_toggle").one("click", function() {
    var $this = $(this).text("...");
    $.ajax({
      url: $this.attr("href"),
      success: function(text) {
        $this.after("<textarea readonly>" + text + "</textarea>").text("Download PGN");
      }
    });
    return false;
  });

  $("form.request_analysis a").click(function() {
    $(this).parent().submit();
  });

  var elem = document.createElement('audio');
  var canPlayAudio = !! elem.canPlayType && elem.canPlayType('audio/ogg; codecs="vorbis"');
  var $soundToggle = $('#sound_state');

  function soundEnabled() {
    return $soundToggle.hasClass("sound_state_on");
  }

  $.playSound = function() {
    if (canPlayAudio && soundEnabled()) {
      var sound = $('#lichess_sound_player').get(0);
      sound.play();
      setTimeout(function() {
        sound.pause();
      },
      1000);
    }
  }

  if (canPlayAudio) {
    $('body').append($('<audio id="lichess_sound_player">').attr('src', $('body').attr('data-sound-file')));
    $soundToggle.click(function() {
      var enabled = !soundEnabled();
      $soundToggle.toggleClass('sound_state_on', enabled);
      $.playSound();
      $.post($soundToggle.attr('href'), {sound: enabled});
      return false;
    });
    $game && $game.trigger('lichess.audio_ready');
  } else {
    $soundToggle.addClass('unavailable');
  }

  if(Boolean(window.chrome)) {
    $("div.addtochrome").show();
  }

});

$.fn.scrollable = function() {
  this.mousewheel(function(e, delta) {
    this.scrollTop -= delta * 30;
    return false;
  });
};

$.fn.orNot = function() {
  return this.length == 0 ? false: this;
};

$.trans = function(text) {
  return lichess_translations[text] ? lichess_translations[text] : text;
}

$.displayBoardMarks = function($board, isWhite) {
  if (isWhite) {
    var factor = 1;
    var base = 0;
  } else {
    var factor = - 1;
    var base = 575;
  }
  $board.find('span.board_mark').remove();
  var letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  var marks = '';
  for (i = 1; i < 9; i++) {
    marks += '<span class="board_mark vert" style="bottom:' + (factor * i * 64 - 38 + base) + 'px;">' + i + '</span>';
    marks += '<span class="board_mark horz" style="left:' + (factor * i * 64 - 35 + base) + 'px;">' + letters[i - 1] + '</span>';
  }
  $board.append(marks);
};

function urlToLink(text) {
  var exp = /\bhttp:\/\/(?:[a-z]{0,3}\.)?(lichess\.org[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
  return text.replace(exp,"<a href='http://$1'>$1</a>");
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
    self.$chat = $("div.lichess_chat").orNot();
    self.$watchers = $("div.watchers");
    self.initialTitle = document.title;
    self.hasMovedOnce = false;
    self.premove = null;
    self.options.tableUrl = self.element.data('table-url');
    self.options.playersUrl = self.element.data('players-url');
    self.options.socketUrl = self.element.data('socket-url');
    self.socketAckTimeout = null;

    $("div.game_tournament .clock").each(function() {
      $(this).clock({time: $(this).data("time")}).clock("start");
    });

    if (self.options.tournament_id) {
      $('body').data('tournament-id', self.options.tournament_id);
    }

    if (self.options.game.started) {
      self.indicateTurn();
      self.initSquaresAndPieces();
      self.initTable();
      self.initClocks();
      if (self.$chat) self.$chat.chat();
      self.$watchers.watchers();
      if (self.isMyTurn() && self.options.game.turns == 0) {
        self.element.one('lichess.audio_ready', function() {
          $.playSound();
        });
      }
      if (!self.options.game.finished && ! self.options.player.spectator) {
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

    if (self.options.player.spectator) {
      self.$board.find("div.lcs").mousedown(function() {
        $("#dont_touch").toggle();
      });
    }

    if (!self.options.opponent.ai && !self.options.player.spectator) {
      // update document title to show playing state
      setTimeout(self.updateTitle = function() {
        document.title = (self.isMyTurn() && ! self.options.game.finished) ? document.title = document.title.indexOf('/\\/') == 0 ? '\\/\\ ' + document.title.replace(/\/\\\/ /, '') : '/\\/ ' + document.title.replace(/\\\/\\ /, '') : document.title;
        setTimeout(self.updateTitle, 400);
      },
      400);
    }

    lichess.socket = new strongSocket(
        lichess.socketUrl + self.options.socketUrl,
        self.options.player.version,
        $.extend(true, lichess.socketDefaults, {
          options: {
            name: "game"
          },
        params: { tk: "--tkph--" },
        events: {
          ack: function() {
            clearTimeout(self.socketAckTimeout);
          },
          message: function(event) {
            self.element.queue(function() {
              if (self.$chat) self.$chat.chat("append", event);
              self.element.dequeue();
            });
          },
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
            $('div.lichess_claim_draw_zone').remove();
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
            $("div#" + event.rook[1], self.$board).append($("div#" + event.rook[0] + " div.lichess_piece.rook", self.$board));
            // if the king is beeing animated, stop it now
            if ($king = $('body > div.king').orNot()) $king.stop(true, true);
            $("div#" + event.king[1], self.$board).append($("div.lichess_piece.king."+event.color, self.$board));
            self.element.dequeue();
          });
        },
        promotion: function(event) {
          self.element.queue(function() {
            $("div#" + event.key + " div.lichess_piece").addClass(event.pieceClass).removeClass("pawn");
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
            self.killPiece($("div#" + event + " div.lichess_piece", self.$board));
            self.element.dequeue();
          });
        },
        redirect: function(event) {
          // stop queue propagation here
          self.element.queue(function() {
            setTimeout(function() {
              location.href = event;
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
        end: function() {
          // Game end must be applied firt: no queue
          self.options.game.finished = true;
          self.$table
            .find("div.lichess_table").addClass("finished").end()
            .find(".moretime").remove().end()
            .find('div.clock').clock('stop');
          self.element.find("div.ui-draggable").draggable("destroy");
          // But enqueue the visible changes
          self.element.queue(function() {
            self.changeTitle($.trans('Game over'));
            self.element.removeClass("my_turn");
            self.reloadTable(function() {
              self.reloadPlayers(function() {
                $.playSound();
                self.element.dequeue();
              });
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
          self.element.queue(function() {
            self.applyPremove();
            self.element.dequeue();
          });
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
        }
        }
        }));
  },
  isMyTurn: function() {
    return this.options.possible_moves != null;
  },
  changeTitle: function(text) {
    if (this.options.player.spectator) return;
    document.title = text + " - " + this.initialTitle;
  },
  indicateTurn: function() {
    var self = this;
    if (self.options.game.finished) {
      self.changeTitle($.trans('Game over'));
    }
    else if (self.isMyTurn()) {
      self.element.addClass("my_turn");
      self.changeTitle($.trans('Your turn'));
    }
    else {
      self.element.removeClass("my_turn");
      self.changeTitle($.trans('Waiting for opponent'));
    }

    if (!self.$table.find('>div').hasClass('finished')) {
      self.$tableInner.find("div.lichess_current_player div.lichess_player." + (self.isMyTurn() ? self.options.opponent.color: self.options.player.color)).hide();
      self.$tableInner.find("div.lichess_current_player div.lichess_player." + (self.isMyTurn() ? self.options.player.color: self.options.opponent.color)).show();
    }
  },
  movePiece: function(from, to, callback, mine) {
    var self = this,
    $piece = self.$board.find("div#" + from + " div.lichess_piece"),
    $from = $("div#" + from, self.$board),
    $to = $("div#" + to, self.$board);

    // already moved
    if (!$piece.length) {
      self.onError(from + " " + to+' empty from square!!', true);
      return;
    }

    self.highlightLastMove(from + " " + to);
    if (!self.isPlayerColor(self.getPieceColor($piece))) {
      $.playSound();
    }

    var afterMove = function() {
      var $killed = $to.find("div.lichess_piece");
      if ($killed.length && self.getPieceColor($piece) != self.getPieceColor($killed)) {
        self.killPiece($killed);
      }
      $piece.css({top: 0, left: 0});
      $to.append($piece);
      $.isFunction(callback || null) && callback();
    };

    var animD = mine ? 0 : self.options.animation_delay;

    $('body > div.lichess_piece').stop(true, true);
    if (animD < 100) {
      afterMove();
    }
    else {
      $("body").append($piece.css({ top: $from.offset().top, left: $from.offset().left }));
      $piece.animate({ top: $to.offset().top, left: $to.offset().left }, animD, afterMove);
    }
  },
  highlightLastMove: function(notation) {
    var self = this;
    var squareIds = notation.split(" ");
    $("div.lcs.moved", self.$board).removeClass("moved");
    $("#" + squareIds[0] + ",#" + squareIds[1], self.$board).addClass("moved");

  },
  killPiece: function($piece) {
    if ($.data($piece, 'draggable')) $piece.draggable("destroy");
    var self = this,
      $deads = self.element.find("div.lichess_cemetery." + self.getPieceColor($piece)),
        $square = $piece.parent();
    $deads.append($("<div>").addClass('lichess_tomb'));
    var $tomb = $("div.lichess_tomb:last", $deads),
        tomb_offset = $tomb.offset();
    $('body').append($piece.css($square.offset()));
    $piece.css("opacity", 0.5).animate({
      top: tomb_offset.top,
      left: tomb_offset.left
    },
    self.options.animation_delay * 2, function() {
      $tomb.append($piece.css({
        position: "relative",
      top: 0,
      left: 0
      }));
    });
  },
  possibleMovesContain: function(from, to) {
    return this.options.possible_moves != null
      && typeof this.options.possible_moves[from] !== 'undefined'
      && this.options.possible_moves[from].indexOf(to) != -1;
  },
  applyPremove: function() {
    var self = this;
    if (self.premove && self.isMyTurn()) {
      var move = self.premove;
      self.unsetPremove();
      if (self.possibleMovesContain(move.from, move.to)) {
        var $fromSquare = $("#"+move.from).orNot();
        var $toSquare = $("#"+move.to).orNot();
        var $piece = $fromSquare.find(".lichess_piece").orNot();
        if ($fromSquare && $toSquare && $piece) {
          self.dropPiece($piece, $fromSquare, $toSquare, true);
        }
      }
    }
  },
  setPremove: function(move) {
    var self = this;
    if (self.isMyTurn()) return;
    self.unsetPremove();
    if (move.from == move.to) return;
    self.premove = move;
    $("#"+move.from+",#"+move.to).addClass("premoved");
    self.unselect();
    $("#premove").show();
  },
  unsetPremove: function() {
    var self = this;
    self.premove = null;
    self.$board.find('div.lcs.premoved').removeClass('premoved');
    $("#premove").hide();
  },
  unselect: function() {
    this.$board.find('> div.selected').removeClass('selected');
  },
  dropPiece: function($piece, $oldSquare, $newSquare, isPremove) {
    var self = this,
    isPremove = isPremove || false;
    squareId = $newSquare.attr('id'),
             moveData = {
               from: $oldSquare.attr("id"),
               to: squareId,
               b: self.blur
             };

    if (!self.isMyTurn()) {
      return self.setPremove({ from: moveData.from, to: moveData.to });
    }

    self.unselect();
    self.hasMovedOnce = true;
    self.blur = 0;
    self.options.possible_moves = null;
    self.movePiece($oldSquare.attr("id"), squareId, null, true);

    function sendMoveRequest(moveData) {
      if (self.canRunClock()) {
        moveData.lag = parseInt(lichess.socket.averageLag);
      }
      lichess.socket.send("move", moveData);
      self.socketAckTimeout = setTimeout(function() {
        location.reload();
      }, lichess.socket.options.pingMaxLag);
    }

    var color = self.options.player.color;
    // promotion
    if ($piece.hasClass('pawn') && ((color == "white" && squareId[1] == 8) || (color == "black" && squareId[1] == 1))) {
      if (isPremove) {
        moveData.promotion = "queen";
        sendMoveRequest(moveData);
      } else {
        var $choices = $('<div class="lichess_promotion_choice">')
          .appendTo(self.$board)
          .html('<div data-piece="queen" class="lichess_piece queen ' + color + '"></div><div data-piece="knight" class="lichess_piece knight ' + color + '"></div><div data-piece="rook" class="lichess_piece rook ' + color + '"></div><div data-piece="bishop" class="lichess_piece bishop ' + color + '"></div>')
          .fadeIn(self.options.animation_delay)
          .find('div.lichess_piece')
          .click(function() { moveData.promotion = $(this).attr('data-piece');
              sendMoveRequest(moveData);
              $choices.fadeOut(self.options.animation_delay, function() {
                $choices.remove();
              });
            }).end();
      }
    }
    else {
      sendMoveRequest(moveData);
    }
  },
  initSquaresAndPieces: function() {
    var self = this;
    if (self.options.game.finished || self.options.player.spectator) {
      return;
    }
    var draggingKey = null;
    var dropped = false;
    // init squares
    self.$board.find("div.lcs").each(function() {
      var squareId = $(this).attr('id');
      $(this).droppable({
        accept: function(draggable) {
          if (!self.isMyTurn()) {
            return draggingKey != squareId;
          } else {
            return draggingKey && self.possibleMovesContain(draggingKey, squareId);
          }
        },
        drop: function(ev, ui) {
          self.dropPiece(ui.draggable, ui.draggable.parent(), $(this));
          dropped = true;
        },
        hoverClass: 'droppable-hover'
      });
    });

    // init pieces
    self.$board.find("div.lichess_piece." + self.options.player.color).each(function() {
      var $this = $(this);
      $this.draggable({
        containment: self.$board,
        helper: function() { return $('<div>').attr('class', $this.attr('class')).appendTo(self.$board); },
        start: function() {
          draggingKey = $this.hide().parent().attr('id');
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

    self.$board.find("div.lichess_piece." + self.options.player.color).each(function() {
      $(this).click(function() {
        self.unsetPremove();
        var $square = $(this).parent();
        if ($square.hasClass('selectable')) return;
        var isSelected = $square.hasClass('selected');
        self.unselect();
        if (isSelected) return;
        $square.addClass('selected');
      });
    });

    self.$board.find("div.lcs").each(function() {
      var $this = $(this);
      $this.hover(function() {
        if($selected = self.$board.find('div.lcs.selected').orNot()) {
          if (!self.isMyTurn() || self.possibleMovesContain($selected.attr('id'), $this.attr('id'))) {
            $this.addClass('selectable');
          }
        }
      },
      function() {
        $this.removeClass('selectable');
      }).click(function() {
        self.unsetPremove();
        var $from = self.$board.find('div.lcs.selected').orNot();
        var $to = $this;
        if (!$from || $from == $to) return;
        var $piece = $from.find('div.lichess_piece');
        if (!self.isMyTurn() && $from) {
          self.dropPiece($piece, $from, $to);
        } else {
          if (!self.possibleMovesContain($from.attr('id'), $this.attr('id'))) return;
          if (!$to.hasClass('selectable')) return;
          $to.removeClass('selectable');
          self.dropPiece($piece, $from, $this);
        }
      });
    });

    /*
     * End of code for touch screens
     */
  },
  reloadTable: function(callback) {
    var self = this;
    self.get(self.options.tableUrl, {
      success: function(html) {
        self.$tableInner.html(html);
        self.initTable();
        $.isFunction(callback) && callback();
        $('body').trigger('lichess.content_loaded');
      }
    }, false);
  },
  reloadPlayers: function(callback) {
    var self = this;
    $.getJSON(self.options.playersUrl, function(data) {
      $(['white', 'black']).each(function() {
        if (data[this]) self.$table.find('div.username.' + this).html(data[this]);
      });
      if (data.me) $('#user_tag span').text(data.me);
      $('body').trigger('lichess.content_loaded');
      $.isFunction(callback) && callback();
    });
  },
  initTable: function() {
    var self = this;
    self.centerTable();
    self.$table.find('a.moretime').unbind("click").click(self.moretime);
  },
  moretime: _.throttle(function() { lichess.socket.send('moretime'); }, 800),
  centerTable: function() {
    var self = this;
    self.$table.find(".lichess_control").each(function() {
      $(this).toggleClass("none", $(this).html().trim() == "");
    });
    self.$table.css('top', (256 - self.$table.height() / 2) + 'px');
  },
  outoftime: _.debounce(function() { lichess.socket.send('outoftime'); }, 200),
  initClocks: function() {
    var self = this;
    if (!self.canRunClock()) return;
    self.$table.find('div.clock').each(function() {
      $(this).clock({
        time: $(this).attr('data-time'),
        buzzer: function() {
          if (!self.options.game.finished && ! self.options.player.spectator) {
            self.outoftime();
          }
        }
      });
    });
    self.updateClocks();
  },
  updateClocks: function(times) {
    var self = this;
    if (!self.canRunClock()) return;
    if (times || false) {
      for (color in times) {
        self.$table.find('div.clock_' + color).clock('setTime', times[color]);
      }
    }
    self.$table.find('div.clock').clock('stop');
    if (self.options.game.turns > 0 || self.options.game.clockRunning) {
      self.$table.find('div.clock_' + self.options.game.player).clock('start');
    }
  },
  canRunClock: function() {
    return this.options.game.clock && this.options.game.started && !this.options.game.finished;
  },
  getPieceColor: function($piece) {
    return $piece.hasClass('white') ? 'white': 'black';
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
      this.onError('status is not success: '+status, reloadIfFail);
    }
    if ((expectation || false) && expectation != xhr.responseText) {
      this.onError('expectation failed: '+xhr.responseText, reloadIfFail);
    }
  },
  onError: function(error, reloadIfFail) {
    var self = this;
    if (reloadIfFail) {
      location.reload();
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
      var html = [], user, i, w;
      for (i in users) {
        w = users[i];
        if (w.indexOf("Anonymous") == 0) {
          user = w;
        } else {
          user = '<a href="/@/' + w + '">' + w + '</a>';
        }
        html.push(user);
      }
      self.list.html(html.join(", "));
      self.element.show();
    } else {
      self.element.hide();
    }
  }
});

$.widget("lichess.chat", {
  _create: function() {
    var self = this;
    self.$msgs = self.element.find('.lichess_messages');
    var headerHeight = self.element.parent().height();
    self.element.css("top", headerHeight + 13);
    self.$msgs.css("height", 454 - headerHeight);
    self.$msgs.find('>li').each(function() { 
      $(this).html(urlToLink($(this).html())); 
    });
    self.$msgs.scrollable();
    var $form = self.element.find('form');
    self.$msgs[0].scrollTop = 9999999;
    var $input = self.element.find('input.lichess_say').one("focus", function() {
      $input.val('').removeClass('lichess_hint');
    });

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
    var $chatToggle = self.element.find('input.toggle_chat');
    $chatToggle.change(function() {
      var enabled = $chatToggle.is(':checked');
      self.element.toggleClass('hidden', !enabled);
      $.post($chatToggle.data('href'), {"chat": enabled});
    });
    if (!$chatToggle.data("enabled")) {
      self.element.addClass('hidden');
    }
    $chatToggle[0].checked = $chatToggle.data("enabled");
  },
  append: function(msg) {
    var self = this;
    self.$msgs.append(urlToLink(msg));
    $('body').trigger('lichess.content_loaded');
    self.$msgs[0].scrollTop = 9999999;
  }
});

$.widget("lichess.clock", {
  _create: function() {
    var self = this;
    this.options.time = parseFloat(this.options.time) * 1000;
    $.extend(this.options, {
      duration: this.options.time,
      state: 'ready'
    });
    this.element.addClass('clock_enabled');
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
        current_time == 0 && $.isFunction(self.options.buzzer) && self.options.buzzer(self.element);
      } else {
        clearInterval(self.options.interval);
      }
    },
    1000);
  },

  setTime: function(time) {
    this.options.time = parseFloat(time) * 1000;
    this._show();
  },

  stop: function() {
    clearInterval(this.options.interval);
    this.options.state = 'stop';
    this.element.removeClass('running');
  },

  _show: function() {
    this.element.text(this._formatDate(new Date(this.options.time)));
  },

  _formatDate: function(date) {
    minutes = this._prefixInteger(date.getUTCMinutes(), 2);
    seconds = this._prefixInteger(date.getSeconds(), 2);
    return minutes + ':' + seconds;
  },

  _prefixInteger: function (num, length) {
    return (num / Math.pow(10, length)).toFixed(length).substr(2);
  }
});

/////////////////
// gamelist.js //
/////////////////

$(function() {

  function parseFen($elem) {
    if (!$elem || !$elem.jquery) {
      $elem = $('.parse_fen');
    }
    $elem.each(function() {
      var $this = $(this);
      var color = $this.data('color') || "white";
      var withKeys = $this.hasClass('with_keys');
      var letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
      var fen = $this.data('fen').replace(/\//g, '');
      var lm = $this.data('lastmove');
      var lastMove = lm ? [lm[0] + lm[1], lm[2] + lm[3]] : [];
      var x, y, html = '', scolor, pcolor, pclass, c, d, increment;
      var pclasses = {'p':'pawn', 'r':'rook', 'n':'knight', 'b':'bishop', 'q':'queen', 'k':'king'};
      var pregex = /(p|r|n|b|q|k)/;

      if ('white' == color) {
        var x = 8, y = 1;
        var increment = function() { y++; if(y > 8) { y = 1; x--; } };
      } else {
        var x = 1, y = 8;
        var increment = function() { y--; if(y < 1) { y = 8; x++; } };
      }
      function openSquare(x, y) {
        var key = 'white' == color ? letters[y - 1] + x : letters[8 - y] + (9 - x);
        var scolor = (x+y)%2 ? 'white' : 'black';
        if ($.inArray(key, lastMove) != -1) scolor += " moved";
        var html = '<div class="lmcs '+scolor+'" style="top:'+(28*(8-x))+'px;left:'+(28*(y-1))+'px;"';
        if (withKeys) {
          html += ' data-key="' + key + '"';
        }
        return html + '>';
      }
      function closeSquare() {
        return '</div>';
      }

      for(var fenIndex in fen) {
        c = fen[fenIndex];
        html += openSquare(x, y);
        if (!isNaN(c)) { // it is numeric
          html += closeSquare();
          increment();
          for (d=1; d<c; d++) {
            html += openSquare(x, y) + closeSquare();
            increment();
          }
        } else {
          pcolor = pregex.test(c) ? 'black' : 'white';
          pclass = pclasses[c.toLowerCase()];
          html += '<div class="lcmp '+pclass+' '+pcolor+'"></div>';
          html += closeSquare();
          increment();
        }
      }

      $this.html(html).removeClass('parse_fen');
      // attempt to free memory
      html = pclasses = increment = pregex = fen = $this = 0;
    });
  }
  parseFen();
  $('body').on('lichess.content_loaded', parseFen);

  var socketOpened = false;

  function registerLiveGames() {
    if (!socketOpened) return;
    var ids = [];
    $('a.mini_board.live').each(function() {
      ids.push($(this).data("live"));
    }).removeClass("live");
    if (ids.length > 0) {
      lichess.socket.send("liveGames", ids.join(" "));
    }
  }
  $('body').on('lichess.content_loaded', registerLiveGames);
  $('body').on('socket.open', function() {
    socketOpened = true;
    registerLiveGames();
  });

  lichess.socketDefaults.events.fen = function(e) {
    $('a.live_' + e.id).each(function() {
      var $this = $(this);
      parseFen($this.data("fen", e.fen).data("lastmove", e.lm));
    });
  };

  $('div.checkmateCaptcha').each(function() {
    var $captcha = $(this);
    var $input = $captcha.find('input');
    var i1, i2;
    $captcha.find('div.lmcs').click(function() {
      var key = $(this).data('key');
      i1 = $input.val();
      i2 = i1.length > 3 ? key : i1 + " " + key;
      $input.val(i2);
    });
  });
});

////////////////
// opening.js //
////////////////

$(function() {

  var $startButtons = $('#start_buttons');

  if (!strongSocket.available) {
    $('#start_buttons a').attr('href', '#');
    $("div.lichess_overboard.joining input.submit").remove();
    return;
  }

  if (!$startButtons.length) {
    return;
  }

  function prepareForm() {
    var $form = $('div.lichess_overboard');
    var $modeChoices = $form.find('.mode_choice input');
    var $casual = $modeChoices.eq(0), $rated = $modeChoices.eq(1);
    var $clockCheckbox = $form.find('.clock_choice input');
    var isHook = $form.hasClass('game_config_hook');
    $form.find('div.buttons').buttonset().disableSelection();
    $form.find('button.submit').button().disableSelection();
    $form.find('.time_choice input, .increment_choice input').each(function() {
      var $input = $(this), $value = $input.parent().find('span');
      var $timeInput = $form.find('.time_choice input');
      var $incrementInput = $form.find('.increment_choice input');
      $input.hide().after($('<div>').slider({
        value: $input.val(),
        min: $input.data('min'),
        max: $input.data('max'),
        range: 'min',
        step: 1,
        slide: function( event, ui ) {
          $value.text(ui.value);
          $input.attr('value', ui.value);
          $form.find('.color_submits button').toggle(
            $timeInput.val() > 0 || $incrementInput.val() > 0
            );
        }
      }));
    });
    $form.find('.elo_range').each(function() {
      var $this = $(this);
      var $input = $this.find("input");
      var $span = $this.parent().find("span.range");
      var min = $input.data("min");
      var max = $input.data("max");
      if ($input.val()) {
        var values = $input.val().split("-");
      } else {
        var values = [min, max];
      }
      $span.text(values.join(' - '));
      $this.slider({
        range: true,
        min: min,
        max: max,
        values: values,
        step: 50,
        slide: function( event, ui ) {
          $input.val(ui.values[0] + "-" + ui.values[1]);
          $span.text(ui.values[0] + " - " + ui.values[1]);
        }
      });
      var $eloRangeConfig = $this.parent();
      $modeChoices.on('change', function() {
        var rated = $rated.attr('checked') == 'checked';
        $eloRangeConfig.toggle(rated);
        if (isHook && rated && $clockCheckbox.attr('checked') != 'checked') {
          $clockCheckbox.click();
        }
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
    var $eloRangeConfig = $form.find('.elo_range_config');
    $form.prepend($('<a class="close"></a>').click(function() {
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
    $startButtons.find('a.config_'+window.location.hash.replace(/#/, '')).click();
  }
});

$.lichessOpeningPreventClicks = function() {
  $('div.lichess_overboard, div.hooks_wrap').hide();
};

// hooks
$(function() {

  var $wrap = $('div.hooks_wrap');
  if (!$wrap.length) return;
  if (!strongSocket.available) return;

  var $chat = $("div.lichess_chat");
  var $chatToggle = $chat.find('input.toggle_chat');
  var chatExists = $chat.length > 0;
  var $bot = $("div.lichess_bot");
  var $newposts = $("div.new_posts");
  var $newpostsinner = $newposts.find('.undertable_inner');
  var $hooks = $wrap.find('div.hooks');
  var $hooksTable = $hooks.find("table");
  var actionUrls = {
    'cancel': $hooks.data('cancel-url'),
  'join': $hooks.data('join-url')
  };
  var $userTag = $('#user_tag');
  var isRegistered = $userTag.length > 0
  var myElo = isRegistered ? parseInt($userTag.data('elo')) : null;
  var hookOwnerId = $hooks.data('my-hook');

  if (chatExists) {
    var $form = $chat.find('form');
    $chat.find('.lichess_messages').scrollable();
    var $input = $chat.find('input.lichess_say').one("focus", function() {
      $input.val('').removeClass('lichess_hint');
    });

    // send a message
    $form.submit(function() {
      if ($input.hasClass("lichess_hint")) return false;
      var text = $.trim($input.val());
      if (!text) return false;
      if (text.length > 140) {
        alert('Max length: 140 chars. ' + text.length + ' chars used.');
        return false;
      }
      $input.val('');
      lichess.socket.send('talk', { txt: text });
      return false;
    });
    $chat.find('a.send').click(function() { $input.trigger('click'); $form.submit(); });

    // toggle the chat
    $chatToggle.change(function() {
      var enabled = $chatToggle.is(':checked');
      $chat.toggleClass('hidden', !enabled);
      $.post($chatToggle.data('href'), {"chat": enabled});
    });
    if (!$chatToggle.data("enabled")) {
      $chat.addClass('hidden');
    }
    $chatToggle[0].checked = $chatToggle.data("enabled");
  }

  function addToChat(html) {
    $chat.find('.lichess_messages').append(html)[0].scrollTop = 9999999;
    $('body').trigger('lichess.content_loaded');
  }
  function buildChatMessage(txt, username) {
    var html = '<li><span>';
    html += '<a class="user_link" href="/@/'+username+'">'+username.substr(0, 12) + '</a>';
    html += '</span>' + urlToLink(txt) + '</li>';
    return html;
  }

  $bot.on("click", "tr", function() { location.href = $(this).find('a.watch').attr("href"); });
  $bot.find('.undertable_inner').scrollable();
  $newpostsinner.scrollable();
  $newpostsinner[0].scrollTop = 9999999;
  $newpostsinner.scrollable();
  setInterval(function() {
    $.ajax($newposts.data('url'), {
      timeout: 10000,
      success: function(data) {
        $newpostsinner.find('ol').html(data);
        $newpostsinner[0].scrollTop = 9999999;
        $('body').trigger('lichess.content_loaded');
      }
    });
  }, 120 * 1000);

  addHooks(lichess_preload.pool);
  renderTimeline(lichess_preload.timeline);
  if (chatExists) {
    var chatHtml = "";
    $.each(lichess_preload.chat, function() {
      if (this.txt) chatHtml += buildChatMessage(this.txt, this.u);
    });
    addToChat(chatHtml);
  }
  lichess.socket = new strongSocket(lichess.socketUrl + "/lobby/socket", lichess_preload.version, $.extend(true, lichess.socketDefaults, {
    params: {
      hook: hookOwnerId
    },
    events: {
      talk: function(e) { if (chatExists && e.txt) addToChat(buildChatMessage(e.txt, e.u)); },
    entry: function(e) { renderTimeline([e]); },
    hook_add: addHook,
    hook_remove: removeHook,
    featured: changeFeatured,
    redirect: function(e) {
      $.lichessOpeningPreventClicks();
      location.href = 'http://'+location.hostname+'/'+e;
    },
    tournaments: reloadTournaments
    },
    options: {
      name: "lobby"
    }
  }));
  $('body').trigger('lichess.content_loaded');

  function reloadTournaments(data) {
    $("table.tournaments").html(data);
  }

  function changeFeatured(html) {
    $('#featured_game').html(html);
    $('body').trigger('lichess.content_loaded');
  }

  function renderTimeline(data) {
    var html = "";
    for (i in data) { html += '<tr>' + data[i] + '</tr>'; }
    $bot.find('.lichess_messages').append(html).parent()[0].scrollTop = 9999999;
    $('body').trigger('lichess.content_loaded');
  }

  function removeHook(id) {
    $("#" + id).find('td.action').addClass('empty').html("").end().fadeOut(500, function() {
      $(this).remove();
      updateHookTable();
    });
  }
  function addHooks(hooks) {
    var html = "";
    for (i in hooks) html += renderHook(hooks[i]);
    $hooksTable.append(html);
    updateHookTable();
  }
  function addHook(hook) {
    $hooksTable.append(renderHook(hook));
    updateHookTable();
  }
  function updateHookTable() {
    if (0 == $hooksTable.find('tr.hook').length) {
      $hooksTable.addClass('empty_table').html('<tr class="create_game"><td colspan="5">'+$.trans("No game available right now, create one!")+'</td></tr>');
    } else {
      $hooksTable.removeClass('empty_table').find('tr.create_game').remove();
    }
    resizeLobby();
    $hooksTable.find('a.join').click($.lichessOpeningPreventClicks);
  }

  function renderHook(hook) {
    if (!isRegistered && hook.mode == "Rated") return "";
    var html = "", isEngine, engineMark, userClass, mode, eloRestriction;
    hook.action = hook.ownerId ? "cancel" : "join";
    html += '<tr id="'+hook.id+'" class="hook'+(hook.action == 'join' ? ' joinable' : '')+'">';
    html += '<td class="color"><span class="'+hook.color+'"></span></td>';
    isEngine = hook.engine && hook.action == 'join';
    engineMark = isEngine ? '<span class="engine_mark"></span>' : '';
    userClass = isEngine ? "user_link engine" : "user_link";
    if (hook.elo) {
      html += '<td><a class="'+userClass+'" href="/@/'+hook.username+'">'+hook.username.substr(0, 12)+'<br />'+'('+hook.elo+')'+engineMark+'</a></td>';
    } else {
      html += '<td>'+hook.username+'</td>';
    }
    html += '</td>';
    eloRestriction = false;
    if (isRegistered) {
      mode = $.trans(hook.mode);
      if (hook.emin) {
        if (hook.action == "join" && (myElo < parseInt(hook.emin) || myElo > parseInt(hook.emax))) {
          eloRestriction = true;
        }
        if (hook.emin > 800 || hook.emax < 2500) {
          mode += "<span class='elorange" + (eloRestriction ? ' nope' : '') + "'>" + hook.emin + ' - ' + hook.emax + '</span>';
        }
      }
    } else {
      mode = "";
    }
    if (hook.variant == 'Chess960') {
      html += '<td><a href="http://en.wikipedia.org/wiki/Chess960"><strong>960</strong></a> ' + mode + '</td>';
    } else {
      html += '<td>'+mode+'</td>';
    }
    html += '<td>'+$.trans(hook.clock)+'</td>';
    if (eloRestriction) {
      html += '<td class="action empty"></td>';
    } else {
      html += '<td class="action">';
      if (hook.action == "cancel") {
        html += '<a href="'+actionUrls.cancel.replace(/\/0{12}/, '/'+hook.ownerId)+'" class="cancel"></a>';
      } else {
        var cancelParam = hookOwnerId ? "?cancel=" + hookOwnerId : ""
          html += '<a href="'+actionUrls.join.replace(/\/0{8}/, '/'+hook.id)+cancelParam+'" class="join"></a>';
      }
    }
    return html;
  }

  function resizeLobby() {
    $wrap.toggleClass("large", $hooks.find("tr").length > 6);
  }

  $hooks.on('click', 'table.empty_table tr', function() {
    $('#start_buttons a.config_hook').click();
  });
});

///////////////////
// tournament.js //
///////////////////

$(function() {

  var $wrap = $('#tournament');
  if (!$wrap.length) return;

  var $userTag = $('#user_tag');

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

  var $chat = $("div.lichess_chat");
  var $chatToggle = $chat.find('input.toggle_chat');
  var chatExists = $chat.length > 0;
  var $userList = $wrap.find("div.user_list");
  var socketUrl = $wrap.data("socket-url");
  var $watchers = $("div.watchers").watchers();

  if (chatExists) {
    var $form = $chat.find('form');
    var $msgs = $chat.find('.lichess_messages');
    $msgs.scrollable();
    $msgs[0].scrollTop = 9999999;
    var $input = $chat.find('input.lichess_say').one("focus", function() {
      $input.val('').removeClass('lichess_hint');
    });

    // send a message
    $form.submit(function() {
      if ($input.hasClass("lichess_hint")) return false;
      var text = $.trim($input.val());
      if (!text) return false;
      if (text.length > 140) {
        alert('Max length: 140 chars. ' + text.length + ' chars used.');
        return false;
      }
      $input.val('');
      lichess.socket.send('talk', { txt: text });
      return false;
    });
    $chat.find('a.send').click(function() { $input.trigger('click'); $form.submit(); });

    // toggle the chat
    $chatToggle.change(function() {
      var enabled = $chatToggle.is(':checked');
      $chat.toggleClass('hidden', !enabled);
      $.post($chatToggle.data('href'), {"chat": enabled});
    });
    if (!$chatToggle.data("enabled")) {
      $chat.addClass('hidden');
    }
    $chatToggle[0].checked = $chatToggle.data("enabled");
  }

  function addToChat(html) {
    $chat.find('.lichess_messages').append(html)[0].scrollTop = 9999999;
    $('body').trigger('lichess.content_loaded');
  }

  function startClock() {
    $("span.tournament_clock").each(function() {
      $(this).clock({time: $(this).data("time")}).clock("start");
    });
  }
  startClock();

  function reload() {
    $wrap.load($wrap.data("href"), function() {
      startClock();
      $('body').trigger('lichess.content_loaded');
    });
  }

  lichess.socket = new strongSocket(lichess.socketUrl + socketUrl, _ld_.version, $.extend(true, lichess.socketDefaults, {
    events: {
      talk: function(e) { if (chatExists) addToChat(e); },
    reload: reload,
    reloadPage: function() {
      location.reload();
    },
    redirect: function(e) {
      location.href = 'http://'+location.hostname+'/'+e;
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

  if(!$("#GameBoard").length) return;

  SetImagePath("/assets/vendor/pgn4web/lichess/64"); // use "" path if images are in the same folder as this javascript file
  SetImageType("png");
  SetShortcutKeysEnabled(false);
  clearShortcutSquares("BCDEFGH", "12345678");
  clearShortcutSquares("A", "1234567");
  var $game = $("#GameBoard");
  var $chat = $("div.lichess_chat").chat();
  var $watchers = $("div.watchers").watchers();

  lichess.socket = new strongSocket(
    lichess.socketUrl + $game.data("socket-url"),
    parseInt($game.data("version")),
    $.extend(true, lichess.socketDefaults, {
      options: {
        name: "analyse",
        ignoreUnknownMessages: true
      },
      events: {
        message: function(event) {
          $chat.chat("append", event);
        },
        crowd: function(event) {
          $watchers.watchers("set", event.watchers);
        }
      }
    }));
});

})();

if (/.+\.lichess\.org/.test(document.domain)) {
  //analytics
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-7935029-3']);
  _gaq.push(['_trackPageview']);
  (function() {
    var ga = document.createElement('script');
    ga.type = 'text/javascript';
    ga.async = true;
    ga.src = 'http://www.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(ga, s);
  })();
}
