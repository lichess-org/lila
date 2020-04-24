var m = require('mithril');
var simul = require('../simul');
var util = require('./util');
var xhr = require('../xhr');
var ceval = require('./ceval');
var status = require('game/status');

function pad2(num) {
  return (num < 10 ? '0' : '') + num;
}

function gameDesc(pairing, host) {
  if (pairing.hostColor === 'white')
    return host + ' vs. ' + pairing.player.username;
  else
    return pairing.player.username + ' vs. ' + host;
}

function formatClockTime(seconds) {
  var date = new Date(seconds * 1000),
    millis = date.getUTCMilliseconds(),
    sep = (millis < 500) ? '<sep class="low">:</sep>' : '<sep>:</sep>',
  baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (seconds >= 3600) {
    var hours = pad2(Math.floor(seconds / 3600));
    return hours + '<sep>:</sep>' + baseStr;
  }
  return baseStr;
}

function evalDesc(eval) {
  return eval ? ('Depth ' + eval.depth + ':\n' + eval.moves) : '';
}

function oldEval(ctrl, pairing) {
  return (ctrl.evals && ctrl.evals.length) ? ctrl.evals.find(function (e) { return e.id === pairing.game.id }) : undefined;
}

module.exports = function(ctrl) {
  var sortedPairings = (!ctrl.arbiterData || !ctrl.arbiterSort) ? ctrl.data.pairings : ctrl.data.pairings.slice().sort(function(a, b) {
    var da = ctrl.arbiterData.find(function (d) { return d.id == a.player.id }), db = ctrl.arbiterData.find(function (d) { return d.id == b.player.id });
    if (!da) da = {};
    if (!db) db = {};
    if (ctrl.arbiterSort === 'eval') {
      da = ceval.compareEval(da.ceval ? da.ceval : oldEval(ctrl, a), a);
      db = ceval.compareEval(db.ceval ? db.ceval : oldEval(ctrl, b), b);
    } else if (ctrl.arbiterSort === 'assessment') {
      var eva = Math.min(999, Math.max(-999, -ceval.compareEval(da.ceval ? da.ceval : oldEval(ctrl, a), a))) / 1000,
        evb = Math.min(999, Math.max(-999, -ceval.compareEval(db.ceval ? db.ceval : oldEval(ctrl, b), b))) / 1000;
      da = (da && da.assessment)? (da.assessment.totalSig + eva) : undefined;
      db = (db && db.assessment)? (db.assessment.totalSig + evb) : undefined;
    } else if (ctrl.arbiterSort.startsWith('assessment.')) {
      da = (da && da.assessment)? da.assessment[ctrl.arbiterSort.slice(11)] : undefined;
      db = (db && db.assessment)? db.assessment[ctrl.arbiterSort.slice(11)] : undefined;
    } else {
      da = da[ctrl.arbiterSort];
      db = db[ctrl.arbiterSort];
    }
    if (da === db) 
      return 0;
    else if (da === undefined) 
      return 1;
    else if (db === undefined) 
      return -1;
    else if (da < db) 
      return ctrl.arbiterSortDescending ? 1 : -1;
    else if (da > db) 
      return ctrl.arbiterSortDescending ? -1 : 1;
    else 
      return 0;
  });
  var sortableHeader = function(hint, title, sort) {
    return m('th.sortable', [
      m('span', { 
        title: hint, 
        onclick: function(e) { ctrl.toggleArbiterSort(e.target.nextSibling, sort) } 
      }, title),
      m('span')
    ]);
  }
  var playingToggle = function() {
    return m('label.playing', {
      title: 'Only ongoing games'
    }, [
      m('input', {
        type: 'checkbox',
        checked: ctrl.arbiterPlayingOnly,
        onchange: function(e) {
          ctrl.toggleArbiterPlaying(e.target.checked);
        }
      }),
      'Playing'
    ]);
  }
  return (ctrl.toggleArbiter && ctrl.arbiterData && simul.amArbiter(ctrl)) ? [ m('div.arbiter-panel', [
    playingToggle(),
    m('table.slist.user_list',
      m('thead', m('tr', [
        m('th', { colspan: ctrl.data.variants.length === 1 ? 1 : 2 }, 'Player username'),
        sortableHeader('Simul host clock time remaining.', 'Host clock', 'hostClock'),
        sortableHeader('Simul participant clock time remaining.', 'Player clock', 'clock'),
        sortableHeader('Last move played.', 'Last move', 'lastMove'),
        sortableHeader('The FMJD rating set on the user\'s profile.', 'FMJD', 'officialRating'),
        sortableHeader('Scan 3.1 evaluation (+ is better for host, - is better for participant).', 'Eval', 'eval'),
        sortableHeader('Average centi-piece loss (deviation from the best move as 1/100th of a piece) ± standard deviation.', 'Acpl ± SD', 'assessment.scanSort'),
        sortableHeader('Average move time in seconds ± standard deviation.', 'Move time ± SD', 'assessment.mtSort'),
        sortableHeader('The percentage of moves the player left the game page (on their own turn).', 'Blurs', 'assessment.blurSort'),
        sortableHeader('Aggregate player assessment.', m.trust('&Sigma;'), 'assessment'),
        m('th', m('span', { title: 'Result of the game. Ongoing games can be settled as a win/draw/loss.' }, 'Result'))
      ])),
      m('tbody', sortedPairings.map(function(pairing) {
      var variant = util.playerVariant(ctrl, pairing.player),
        playing = pairing.game.status < status.ids.aborted,
        data = ctrl.arbiterData.find(function (d) { return d.id == pairing.player.id }),
        assessment = data ? data.assessment : undefined,
        oldeval = oldEval(ctrl, pairing),
        eval = data ? data.ceval : undefined,
        drawReason = data ? data.drawReason : undefined, drawText;
      if (!playing && ctrl.arbiterPlayingOnly) return null;
      if (eval) {
        if (ctrl.evals && ctrl.evals.length && ctrl.evals.find(function (e) { return e.id === pairing.game.id })) {
          ctrl.evals = ctrl.evals.map(function(e) {
            return e.id === pairing.game.id ? eval : e;
          });
        } else if (ctrl.evals && ctrl.evals.length) {
          ctrl.evals.push(eval);
        } else {
          ctrl.evals = [eval];
        }
      } else if (oldeval) {
        eval = oldeval;
      }
      var result = !playing ? (
        pairing.winnerColor === 'white' ? (ctrl.pref.draughtsResult ? '2-0' : '1-0')
        : (pairing.winnerColor === 'black' ? (ctrl.pref.draughtsResult ? '0-2' : '0-1')
        : (ctrl.pref.draughtsResult ? '1-1' : '½-½'))
      ) : '*';
      var evalText = ceval.renderEval(eval, pairing, ctrl.pref.draughtsResult);
      switch (drawReason) {
        case 'repetition':
          drawText = 'Threefold repetition';
          break;
        case 'autodraw':
          drawText = 'Autodraw';
          if (result !== '*') result += ' (A)';
          break;
        case 'agreement':
          drawText = 'Draw by agreement';
          break;
      }
      return m('tr', [
        m('td', util.player(pairing.player, pairing.player.rating, pairing.player.provisional, '', '/' + pairing.game.id)),
        ctrl.data.variants.length === 1 ? null : m('td.variant', { 'data-icon': variant.icon }),
        m('td', (data && data.hostClock !== undefined) ? m(
          (playing && pairing.hostColor === data.turnColor) ? 'div.time.running' : 'div.time',
          m.trust(formatClockTime(data.hostClock))
        ) : '-'),
        m('td', (data && data.clock !== undefined) ? m(
          (playing && pairing.hostColor !== data.turnColor) ? 'div.time.running' : 'div.time',
          m.trust(formatClockTime(data.clock))
        ) : '-'),
        m('td', m('span', data.lastMove)),
        m('td', data.officialRating ? data.officialRating : '-'),
        m('td', m('span', { title: evalDesc(eval) }, evalText)),
        m('td.assess', assessment ? [
          m('span.sig.sig_' + assessment.scanSig, {'data-icon': 'J'}),
          assessment.scanAvg + ' ± ' + assessment.scanSd
        ] : '-'),
        m('td.assess', assessment ? [
          m('span.sig.sig_' + assessment.mtSig, {'data-icon': 'J'}),
          Math.round(assessment.mtAvg / 10) + ' ± ' + Math.round(assessment.mtSd / 10)
        ] : '-'),
        m('td.assess', assessment ? [
          m('span.sig.sig_' + assessment.blurSig, {'data-icon': 'J'}),
          assessment.blurPct + '%'
        ] : '-'),
        m('td.assess', assessment ? m('span.sig.sig_' + assessment.totalSig, {
          'data-icon': 'J',
          'title': assessment.totalTxt + ' (eval ' + evalText + ')'
        }) : '-'),
        result !== '*' ? m('td', m('span', drawReason ? { title: drawText } : undefined, result)) :
        m('td.action', !playing ? '-' : m('a.button', {
          'data-icon': '2',
          title: 'Settle ' + gameDesc(pairing, ctrl.data.host.username) + ' as a win/draw/loss',
          onclick: function(e) {
            $('#simul #settle-info').text('Choose one of the options below to settle the game ' + gameDesc(pairing, ctrl.data.host.username) + '. Only continue when you are very sure, because this cannot be undone!');
            $('#simul #settle-hostloss').text('Simul participant ' + pairing.player.username + ' wins')
            $.modal($('#simul .settle_choice'));
            $('#modal-wrap .settle_choice a').click(function() {
              var result = $(this).data('settle'),
                confirmation = 'Please confirm that you want to settle the game ' + gameDesc(pairing, ctrl.data.host.username);
              if (result === 'hostwin') confirmation += ' as a win for simul host ' + ctrl.data.host.username;
              else if (result === 'hostloss') confirmation += ' as a win for simul participant ' + pairing.player.username;
              else confirmation += ' as a draw';
              if (confirm(confirmation + '. This action is irreversible!')) {
                $.modal.close();
                xhr.settle(pairing.player.id, result)(ctrl);
              }
            });
          }
        }))
      ]);
    })))
  ]),
  m('div.settle_choice.block_buttons', [
    m('span', { id: 'settle-info' } ),
    m('a.button', { 'data-settle': 'hostwin' }, 'Simul host ' + ctrl.data.host.username + ' wins'),
    m('a.button', { 'data-settle': 'draw' }, 'Settle as a draw'),
    m('a.button', { 'data-settle': 'hostloss', id: 'settle-hostloss' })
  ])] : null;
}