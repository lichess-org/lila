var util = require('../util');
var classSet = require('common').classSet;
var game = require('game').game;
var status = require('game').status;
var router = require('game').router;
var m = require('mithril');

function analysisBoardOrientation(data) {
  if (data.game.variant.key === 'racingKings') {
    return 'white';
  } else {
    return data.player.color;
  }
}

function poolUrl(clock) {
  return '/#pool/' + (clock.initial / 60) + '+' + clock.increment;
}

function analysisButton(ctrl) {
  var d = ctrl.data;
  var url = router.game(d, analysisBoardOrientation(d)) + '#' + ctrl.vm.ply;
  return game.replayable(d) ? m('a.button', {
    href: url,
    // force page load in case the URL is the same
    onclick: function() {
      if (location.pathname === url.split('#')[0]) location.reload();
    }
  }, ctrl.trans.noarg('analysis')) : null;
}

function rematchButton(ctrl) {
  var d = ctrl.data;
  var me = d.player.offeringRematch, them = d.opponent.offeringRematch;
  return m('a', {
    class: classSet({
      'button rematch white': true,
      me: me,
      them: them
    }),
    title: them ? ctrl.trans.noarg('yourOpponentWantsToPlayANewGameWithYou') : (
      me ? ctrl.trans.noarg('rematchOfferSent') : ''),
    config: util.bindOnce('click', function() {
      var d = ctrl.data;
      if (d.game.rematch) location.href = router.game(d.game.rematch, d.opponent.color);
      else if (d.player.offeringRematch) {
        d.player.offeringRematch = false;
        ctrl.socket.send('rematch-no');
      }
      else if (d.opponent.onGame) {
        d.player.offeringRematch = true;
        ctrl.socket.send('rematch-yes');
      }
      else ctrl.challengeRematch();
      m.redraw();
    })
  }, [
    me ? m.trust(lichess.spinnerHtml) : ctrl.trans.noarg('rematch')
  ]);
}

module.exports = {
  standard: function(ctrl, condition, icon, hint, socketMsg, onclick) {
    // disabled if condition callback is provied and is falsy
    var enabled = function() {
      return !condition || condition(ctrl.data);
    };
    return m('button', {
      key: socketMsg || 'click',
      class: 'fbt hint--bottom ' + socketMsg,
      disabled: !enabled(),
      'data-hint': ctrl.trans.noarg(hint),
      config: util.bindOnce('click', function() {
        if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg, null);
      })
    }, m('span', {
      'data-icon': icon
    }));
  },
  forceResign: function(ctrl) {
    if (ctrl.forceResignable())
    return m('div.suggestion', [
      m('p', ctrl.trans('theOtherPlayerHasLeftTheGameYouCanForceResignationOrWaitForHim')),
      m('a.button', {
        onclick: lichess.partial(ctrl.socket.sendLoading, 'resign-force', null),
      }, ctrl.trans('forceResignation')),
      m('a.button', {
        onclick: lichess.partial(ctrl.socket.sendLoading, 'draw-force', null),
      }, ctrl.trans('forceDraw'))
    ]);
  },
  resignConfirm: function(ctrl) {
    return m('div.resign_confirm', [
      m('button.fbt.no.hint--bottom', {
        'data-hint': ctrl.trans('cancel'),
        onclick: lichess.partial(ctrl.resign, false)
      }, m('span', {
        'data-icon': 'L'
      })),
      m('button.fbt.yes.active.hint--bottom', {
        'data-hint': ctrl.trans('resign'),
        onclick: lichess.partial(ctrl.resign, true)
      }, m('span', {
        'data-icon': 'b'
      }))
    ]);
  },
  threefoldClaimDraw: function(ctrl) {
    if (ctrl.data.game.threefold) return m('div.suggestion', [
      m('p', ctrl.trans('threefoldRepetition')),
      m('a.button', {
        onclick: lichess.partial(ctrl.socket.sendLoading, 'draw-claim', null)
      }, ctrl.trans('claimADraw'))
    ]);
  },
  cancelDrawOffer: function(ctrl) {
    if (ctrl.data.player.offeringDraw) return m('div.pending', [
      m('p', ctrl.trans('drawOfferSent'))
    ]);
  },
  answerOpponentDrawOffer: function(ctrl) {
    if (ctrl.data.opponent.offeringDraw) return m('div.negotiation', [
      m('p', ctrl.trans('yourOpponentOffersADraw')),
      m('a.accept[data-icon=E]', {
        config: util.bindOnce('click', lichess.partial(ctrl.socket.sendLoading, 'draw-yes', null)),
        title: ctrl.trans('accept')
      }),
      m('a.decline[data-icon=L]', {
        onclick: lichess.partial(ctrl.socket.sendLoading, 'draw-no', null),
        title: ctrl.trans('decline')
      }),
    ]);
  },
  cancelTakebackProposition: function(ctrl) {
    if (ctrl.data.player.proposingTakeback) return m('div.pending', [
      m('p', ctrl.trans('takebackPropositionSent')),
      m('a.button', {
        onclick: lichess.partial(ctrl.socket.sendLoading, 'takeback-no', null)
      }, ctrl.trans('cancel'))
    ]);
  },
  answerOpponentTakebackProposition: function(ctrl) {
    if (ctrl.data.opponent.proposingTakeback) return m('div.negotiation', [
      m('p', ctrl.trans('yourOpponentProposesATakeback')),
      m('a.accept[data-icon=E]', {
        onclick: lichess.partial(ctrl.takebackYes),
        title: ctrl.trans('accept')
      }),
      m('a.decline[data-icon=L]', {
        onclick: lichess.partial(ctrl.socket.sendLoading, 'takeback-no', null),
        title: ctrl.trans('decline')
      })
    ]);
  },
  submitMove: function(ctrl) {
    if (ctrl.vm.moveToSubmit || ctrl.vm.dropToSubmit) return m('div.negotiation', [
      m('p', ctrl.trans('moveConfirmation')),
      m('a.accept[data-icon=E]', {
        onclick: lichess.partial(ctrl.submitMove, true),
        title: 'Submit move'
      }),
      m('a.decline[data-icon=L]', {
        onclick: lichess.partial(ctrl.submitMove, false),
        title: ctrl.trans('cancel')
      })
    ]);
  },
  backToTournament: function(ctrl) {
    var d = ctrl.data;
    if (d.tournament && d.tournament.running) return m('div.follow_up', [
      m('a', {
        'data-icon': 'G',
        class: 'text fbt strong glowed',
        href: '/tournament/' + d.tournament.id,
        config: util.bindOnce('click', ctrl.setRedirecting)
      }, ctrl.trans('backToTournament')),
     m('form', {
        method: 'post',
        action: '/tournament/' + d.tournament.id + '/withdraw'
      }, m('button.text.button.weak[data-icon=Z]', 'Pause')),
      analysisButton(ctrl)
    ]);
  },
  moretime: function(ctrl) {
    if (game.moretimeable(ctrl.data)) return m('a.moretime.hint--bottom-left', {
      'data-hint': ctrl.trans('giveNbSeconds', ctrl.data.clock.moretime),
      config: util.bindOnce('click', ctrl.socket.moreTime)
    }, m('span[data-icon=O]'));
  },
  followUp: function(ctrl) {
    var d = ctrl.data;
    var rematchable = !d.game.rematch && (status.finished(d) || status.aborted(d)) && !d.tournament && !d.simul && !d.game.boosted && (d.opponent.onGame || (!d.clock && d.player.user && d.opponent.user));
    var newable = (status.finished(d) || status.aborted(d)) && (
      d.game.source === 'lobby' ||
        d.game.source === 'pool');
    return m('div.follow_up', [
      ctrl.vm.challengeRematched ? m('div.suggestion.text[data-icon=j]',
        ctrl.trans('rematchOfferSent')
      ) : (rematchable || d.game.rematch ? rematchButton(ctrl) : null),
      d.tournament ? m('a.button', {
        href: '/tournament/' + d.tournament.id
      }, ctrl.trans('viewTournament')) : null,
      newable ? m('a.button', {
        href: d.game.source === 'pool' ? poolUrl(d.clock) : '/?hook_like=' + d.game.id,
      }, ctrl.trans('newOpponent')) : null,
      analysisButton(ctrl)
    ]);
  },
  watcherFollowUp: function(ctrl) {
    var d = ctrl.data;
    return m('div.follow_up', [
      d.game.rematch ? m('a.button.text[data-icon=v]', {
        href: router.game(d.game.rematch, d.opponent.color)
      }, ctrl.trans('viewRematch')) : null,
      d.tournament ? m('a.button', {
        href: '/tournament/' + d.tournament.id
      }, ctrl.trans('viewTournament')) : null,
      analysisButton(ctrl)
    ]);
  }
};
