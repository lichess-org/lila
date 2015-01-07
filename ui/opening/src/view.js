var m = require('mithril');
var chessground = require('chessground');
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var xhr = require('./xhr');

function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function renderPlayTable(ctrl) {
  return m('div.table',
    m('div.table_inner', [
      m('div.current_player',
        m('div.player.' + ctrl.chessground.data.turnColor, [
          m('div.no-square', m('div.cg-piece.king.' + ctrl.chessground.data.turnColor)),
          m('p', ctrl.trans('yourTurn'))
        ])
      ),
      m('div.findit', m.trust(ctrl.trans('findNbGoodMoves', strong(ctrl.data.opening.goal)))),
      m('div.control',
        ctrl.data.play ? m('a.button', {
          onclick: partial(xhr.attempt, ctrl)
        }, ctrl.trans('giveUp')) : m('a.button', {
          onclick: partial(xhr.newOpening, ctrl)
        }, ctrl.trans('continueTraining'))
      )
    ])
  );
}

function renderUserInfos(ctrl) {
  return m('div.chart_container', [
    m('p', m.trust(ctrl.trans('yourOpeningScoreX', strong(ctrl.data.user.score)))),
    ctrl.data.user.history ? m('div.user_chart', {
      config: function(el, isUpdate, context) {
        var hash = ctrl.data.user.history.join('');
        if (hash == context.hash) return;
        context.hash = hash;
        var dark = document.body.classList.contains('dark');
        jQuery(el).sparkline(ctrl.data.user.history, {
          type: 'line',
          width: '213px',
          height: '80px',
          lineColor: dark ? '#4444ff' : '#0000ff',
          fillColor: dark ? '#222255' : '#ccccff'
        });
      }
    }) : null
  ]);
}

function renderTrainingBox(ctrl) {
  return m('div.box', [
    m('h1', ctrl.trans('training')),
    m('div.tabs.buttonset', [
      m('a.button', {
        href: '/training'
      }, 'Puzzle'),
      m('a.button', {
        href: '/training/coordinate'
      }, 'Coord'),
      m('a.button.active', {
        href: '/training/opening'
      }, 'Opening'),
    ]),
    ctrl.data.user ? renderUserInfos(ctrl) : m('div.register', [
      m('p', ctrl.trans('toTrackYourProgress')),
      m('p.signup',
        m('a.button', {
          href: '/signup',
        }, ctrl.trans('signUp'))
      ),
      m('p', ctrl.trans('trainingSignupExplanation'))
    ])
  ]);
}

function renderSide(ctrl) {
  return m('div.side', [
    renderTrainingBox(ctrl)
  ]);
}

function progress(ctrl) {
  var steps = [];
  var nbFiguredOut = ctrl.vm.figuredOut.length;
  var lastI = nbFiguredOut - 1;
  var nextI = nbFiguredOut;
  for (var i = 0; i < ctrl.data.opening.goal; i++) {
    steps.push({
      found: ctrl.vm.figuredOut[i],
      last: lastI === i,
      next: nextI === i
    });
  }
  var liWidth = Math.round(100 / ctrl.data.opening.goal) + '%';
  return m('div.meter', [
    m('ul',
      steps.map(function(step) {
        var badSan = (step.next && ctrl.vm.flash.bad) ? ctrl.vm.flash.bad.san : null;
        var dubiousSan = (step.next && ctrl.vm.flash.dubious) ? ctrl.vm.flash.dubious.san : null;
        return m('li', {
          class: classSet({
            found: step.found,
            next: step.next,
            bad: badSan,
            dubious: dubiousSan,
            good: step.last && ctrl.vm.flash.good,
            already: step.found && ctrl.vm.flashFound && ctrl.vm.flashFound.uci === step.found.uci
          }),
          style: {
            width: liWidth
          }
        }, [
          m('span.step', step.found ? step.found.san : (
            badSan || dubiousSan || '?'
          )),
          m('span.stage')
        ]);
      }))
  ]);
}

var loading = m('div.loader.fast');

module.exports = function(ctrl) {
  var percent = Math.ceil(ctrl.vm.figuredOut.length * 100 / ctrl.data.opening.goal) + '%';
  return m('div#opening.training', [
    renderSide(ctrl),
    m('div.board_and_ground', [
      m('div', chessground.view(ctrl.chessground)),
      m('div.right', ctrl.vm.loading ? loading : renderPlayTable(ctrl))
    ]),
    m('div.center', [
      progress(ctrl)
    ])
  ]);
};
