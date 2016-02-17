var m = require('mithril');
var chessground = require('chessground');
var classSet = chessground.util.classSet;
var partial = chessground.util.partial;
var xhr = require('./xhr');

function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function renderAnalysisButton(ctrl) {
  return m('a.button.hint--bottom', {
    'data-hint': ctrl.trans('analysis'),
    href: '/analysis/' + encodeURIComponent(ctrl.data.opening.fen).replace(/%20/g, '_').replace(/%2F/g, '/'),
    target: ctrl.data.play ? '_blank' : '_self',
    rel: 'nofollow'
  }, m('span', {
    'data-icon': 'A'
  }));
}

function renderPlayTable(ctrl) {
  return m('div.table_wrap',
    m('div.table', [
      m('div.table_inner', [
        m('div.current_player',
          m('div.player.' + ctrl.data.opening.color, [
            m('div.no-square', m('piece.king.' + ctrl.data.opening.color)),
            m('p', ctrl.trans('yourTurn'))
          ])
        ),
        m('div.findit', m.trust(ctrl.trans('findNbStrongMoves', strong(ctrl.data.opening.goal)))),
        m('div.control', [
          m('a.button', {
            onclick: partial(xhr.attempt, ctrl)
          }, ctrl.trans('giveUp')),
          ' ',
          renderAnalysisButton(ctrl)
        ])
      ])
    ])
  );
}

function renderViewTable(ctrl) {
  return [
    m('div.box', [
      m('h2.text', {
        'data-icon': ']'
      }, m('a', {
        href: '/training/opening/' + ctrl.data.opening.id
      }, ctrl.trans('openingId', ctrl.data.opening.id))),
      m('p', m.trust(ctrl.trans('ratingX', strong(ctrl.data.opening.rating)))),
      m('p', m.trust(ctrl.trans('playedXTimes', strong(ctrl.data.opening.attempts)))),
      m('div.control', [
        renderAnalysisButton(ctrl),
        ' ',
        m('a.button.hint--bottom', {
          'data-hint': ctrl.trans('boardEditor'),
          href: '/editor/' + ctrl.data.opening.fen
        }, m('span[data-icon=m]')),
        ' ',
        m('a.button.hint--bottom', {
          'data-hint': ctrl.trans('continueFromHere'),
          onclick: function() {
            $.modal($('.continue_with'));
          }
        }, m('span[data-icon=U]'))
      ]),
      m('p',
        m('input.copyable[readonly][spellCheck=false]', {
          value: ctrl.data.opening.url
        })
      )
    ]),
    m('div.continue_wrap',
      m('button.continue.button.text[data-icon=G]', {
        onclick: partial(xhr.newOpening, ctrl)
      }, ctrl.trans('continueTraining'))
    )
  ];
}

function renderContinueLinks(ctrl, fen) {
  return m('div.continue_with', [
    m('a.button', {
      href: '/?fen=' + ctrl.data.opening.fen + '#ai',
      rel: 'nofollow'
    }, ctrl.trans('playWithTheMachine')),
    m('br'),
    m('a.button', {
      href: '/?fen=' + ctrl.data.opening.fen + '#friend',
      rel: 'nofollow'
    }, ctrl.trans('playWithAFriend'))
  ]);
}

function renderUserInfos(ctrl) {
  return m('div.chart_container', [
    m('p', m.trust(ctrl.trans('yourOpeningRatingX', strong(ctrl.data.user.rating)))),
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

function renderCommentary(ctrl) {
  switch (ctrl.vm.comment) {
    case 'dubious':
      return m('div.comment.retry', [
        m('h3', m('strong', ctrl.trans('goodMove'))),
        m('span', ctrl.trans('butYouCanDoBetter'))
      ]);
    case 'good':
      return m('div.comment.great', [
        m('h3.text[data-icon=E]', m('strong', ctrl.trans('bestMove'))),
        ctrl.vm.figuredOut.length < ctrl.data.goal ? m('span', ctrl.trans('keepGoing')) : null
      ]);
    case 'bad':
      return m('div.comment.fail', [
        m('h3.text[data-icon=k]', m('strong', ctrl.trans('thisMoveGivesYourOpponentTheAdvantage')))
      ]);
    default:
      return ctrl.vm.comment
  }
}

function renderTrainingBox(ctrl) {
  return m('div.box', [
    m('h1', ctrl.trans('openings')),
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

function renderRatingDiff(diff) {
  return m('strong.rating', diff > 0 ? '+' + diff : diff);
}

function renderWin(ctrl, attempt) {
  return m('div.comment.win', [
    m('h3.text[data-icon=E]', [
      m('strong', ctrl.trans('victory')),
      attempt ? renderRatingDiff(attempt.userRatingDiff) : null
    ]),
    attempt ? m('span', ctrl.trans('openingSolved')) : null
  ]);
}

function renderLoss(ctrl, attempt) {
  return m('div.comment.loss',
    m('h3.text[data-icon=k]', [
      m('strong', ctrl.trans('openingFailed')),
      attempt ? renderRatingDiff(attempt.userRatingDiff) : null
    ])
  );
}

function renderResult(ctrl) {
  switch (ctrl.data.win) {
    case true:
      return renderWin(ctrl, null);
    case false:
      return renderLoss(ctrl, null);
    default:
      switch (ctrl.data.attempt && ctrl.data.attempt.win) {
        case true:
          return renderWin(ctrl, ctrl.data.attempt);
        case false:
          return renderLoss(ctrl, ctrl.data.attempt);
      }
  }
}

function renderSide(ctrl) {
  return m('div.side', [
    renderTrainingBox(ctrl),
    ctrl.data.play ? renderCommentary(ctrl) : null,
    renderResult(ctrl)
  ]);
}

function renderHistory(ctrl) {
  return m('div.history', {
    config: function(el, isUpdate, context) {
      var hash = ctrl.data.user.history.join('');
      if (hash == context.hash) return;
      context.hash = hash;
      $.ajax({
        url: '/training/opening/history',
        success: function(html) {
          el.innerHTML = html;
        }
      });
    }
  });
}

function progress(ctrl) {
  var steps = [];
  var figuredOut = ctrl.vm.figuredOut.slice(0);
  var nbFiguredOut = figuredOut.length;
  var lastI = nbFiguredOut - 1;
  var nextI = nbFiguredOut;
  if (!ctrl.data.play) figuredOut = figuredOut.concat(
    ctrl.notFiguredOut().slice(0, ctrl.data.opening.goal - figuredOut.length)
  );
  for (var i = 0; i < ctrl.data.opening.goal; i++) {
    steps.push({
      found: i < nbFiguredOut,
      move: figuredOut[i],
      last: lastI === i,
      next: nextI === i
    });
  }
  var liWidth = Math.round(100 / ctrl.data.opening.goal) + '%';
  return m('div.meter', [
    m('ul',
      steps.map(function(step, i) {
        var badSan = (step.next && ctrl.vm.flash.bad) ? ctrl.vm.flash.bad.san : null;
        var dubiousSan = (step.next && ctrl.vm.flash.dubious) ? ctrl.vm.flash.dubious.san : null;
        return m('li', {
          key: i,
          class: classSet({
            found: step.found,
            next: step.next,
            bad: badSan,
            dubious: dubiousSan,
            good: step.last && ctrl.vm.flash.good,
            already: step.move && ctrl.vm.flashFound && ctrl.vm.flashFound.uci === step.move.uci
          }),
          style: {
            width: liWidth
          }
        }, [
          m('span.step', step.move ? step.move.san : (
            badSan || dubiousSan || '?'
          )),
          m('span.stage')
        ]);
      }))
  ]);
}

module.exports = function(ctrl) {
  var percent = Math.ceil(ctrl.vm.figuredOut.length * 100 / ctrl.data.opening.goal) + '%';
  return m('div#opening.training', [
    renderSide(ctrl),
    m('div.board_and_ground', [
      m('div', chessground.view(ctrl.chessground)),
      m('div.right', ctrl.vm.loading ? m.trust(lichess.spinnerHtml) : (
        ctrl.data.play ? renderPlayTable(ctrl) : renderViewTable(ctrl)
      ))
    ]),
    m('div.underboard',
      m('div.center', [
        progress(ctrl),
        m('table.identified', ctrl.data.opening.identified.map(function(ident) {
          return m('tr', [
            m('td', ident.name),
            m('td', ident.moves)
          ]);
        })), (ctrl.data.user && ctrl.data.user.history) ? renderHistory(ctrl) : null,
        ctrl.data.play ? null : renderContinueLinks(ctrl)
      ])
    )
  ]);
};
