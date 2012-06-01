$.widget("lichess.game", {

  _init: function() {
    var self = this;
    self.$board = self.element.find("div.lichess_board");
    self.$table = self.element.find("div.lichess_table_wrap");
    self.$tableInner = self.$table.find("div.table_inner");
    self.$chat = $("div.lichess_chat");
    self.$chatMsgs = self.$chat.find('.lichess_messages');
    self.$nbViewers = $('.nb_viewers');
    self.initialTitle = document.title;
    self.hasMovedOnce = false;
    self.premove = null;
    self.options.tableUrl = self.element.data('table-url');
    self.options.playersUrl = self.element.data('players-url');
    self.options.socketUrl = self.element.data('socket-url');

    if (self.options.game.started) {
      self.indicateTurn();
      self.initSquaresAndPieces();
      self.initChat();
      self.initTable();
      self.initClocks();
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

    if (!self.options.opponent.ai && !self.options.player.spectator) {
      // update document title to show playing state
      setTimeout(self.updateTitle = function() {
        document.title = (self.isMyTurn() && ! self.options.game.finished) ? document.title = document.title.indexOf('/\\/') == 0 ? '\\/\\ ' + document.title.replace(/\/\\\/ /, '') : '/\\/ ' + document.title.replace(/\\\/\\ /, '') : document.title;
        setTimeout(self.updateTitle, 400);
      },
      400);
    }

    lichess.socket = new $.websocket(
        lichess.socketUrl + self.options.socketUrl,
        self.options.player.version,
        $.extend(true, lichess.socketDefaults, {
          options: {
            name: "game"
          },
        events: {
          message: function(event) {
            self.element.queue(function() {
              self.appendToChat(event);
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
        reload: function(event) {
          // stop queue propagation here
          self.element.queue(function() {
            setTimeout(function() {
              location.reload();
            }, 400);
          });
        },
        threefold_repetition: function(event) {
          self.element.queue(function() {
            self.reloadTable(function() {
              self.element.dequeue();
            });
          });
        },
        gone: function(event) {
          self.$table.find("div.force_resign_zone").toggle(event);
          self.centerTable();
        },
        end: function(event) {
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
                self.element.dequeue();
              });
            });
          });
        },
        reload_table: function(event) {
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
        premove: function(event) {
          self.element.queue(function() {
            self.applyPremove();
            self.element.dequeue();
          });
        },
        crowd: function(event) {
          if (self.$nbViewers.length) {
            self.$nbViewers.html(self.$nbViewers.html().replace(/(\d+|-)/, event.watchers)).toggle(event.watchers > 0);
            $(["white", "black"]).each(function() {
              self.$table.find("div.username." + this).toggleClass("connected", event[this]).toggleClass("offline", !event[this]);
            });
          }
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

    // TODO send moveData here
    function sendMoveRequest(moveData) {
      lichess.socket.send("move", moveData);
    }

    var color = self.options.player.color;
    // promotion
    if ($piece.hasClass('pawn') && ((color == "white" && squareId[1] == 8) || (color == "black" && squareId[1] == 1))) {
      if (isPremove) {
        moveData.promotion = "queen";
        sendMoveRequest(moveData);
      } else {
        var $choices = $('<div class="lichess_promotion_choice">').appendTo(self.$board).html('\
            <div data-piece="queen" class="lichess_piece queen ' + color + '"></div>\
            <div data-piece="knight" class="lichess_piece knight ' + color + '"></div>\
            <div data-piece="rook" class="lichess_piece rook ' + color + '"></div>\
            <div data-piece="bishop" class="lichess_piece bishop ' + color + '"></div>').fadeIn(self.options.animation_delay).find('div.lichess_piece').click(function() {
              moveData.promotion = $(this).attr('data-piece');
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
  initChat: function() {
    var self = this;
    if (!self.$chat.length) {
      return;
    }
    self.$chatMsgs.find('>li').each(function() { $(this).html(urlToLink($(this).html())); });
    self.$chatMsgs.scrollable();
    var $form = self.$chat.find('form');
    self.$chatMsgs[0].scrollTop = 9999999;
    var $input = self.$chat.find('input.lichess_say').one("focus", function() {
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

    self.$chat.find('a.send').click(function() {
      $input.trigger('click');
      $form.submit();
    });

    // toggle the chat
    self.$chat.find('input.toggle_chat').change(function() {
      self.$chat.toggleClass('hidden', ! $(this).attr('checked'));
    }).trigger('change');
  },
  appendToChat: function(msg) {
    if (this.$chat.length) {
      this.$chatMsgs.append(urlToLink(msg))[0];
      $('body').trigger('lichess.content_loaded');
      this.$chatMsgs.scrollTop = 9999999;
    }
  },
  reloadTable: function(callback) {
    var self = this;
    self.get(self.options.tableUrl, {
      success: function(html) {
        $('body > div.tipsy').remove();
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
    self.$table.find('a, input, label').tipsy({
      fade: true
    });
    self.$table.find('a.moretime').unbind("click").click(function() {
      lichess.socket.send("moretime");
      return false;
    });
  },
  centerTable: function() {
    this.$table.css('top', (256 - this.$table.height() / 2) + 'px');
  },
  initClocks: function() {
    var self = this;
    if (!self.canRunClock()) return;
    self.$table.find('div.clock').each(function() {
      $(this).clock({
        time: $(this).attr('data-time'),
        buzzer: function() {
          if (!self.options.game.finished && ! self.options.player.spectator) {
            lichess.socket.send("outoftime");
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
    if (self.options.game.turns > 0) {
      self.$table.find('div.clock_' + self.options.game.player).clock('start');
    }
  },
  canRunClock: function() {
    return this.options.game.clock && this.options.game.started && ! this.options.game.finished;
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

// clock
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
    minutes = this._prefixInteger(date.getMinutes(), 2);
    seconds = this._prefixInteger(date.getSeconds(), 2);
    return minutes + ':' + seconds;
  },

  _prefixInteger: function (num, length) {
    return (num / Math.pow(10, length)).toFixed(length).substr(2);
  }
});
