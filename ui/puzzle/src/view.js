var partial = require('lodash-node/modern/functions/partial');
var chessground = require('chessground');
var m = require('mithril');
var puzzle = require('./puzzle');

// useful in translation arguments
function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function renderUserInfos(ctrl) {
  return m('div.chart_container', [
    m('p', m.trust(ctrl.trans('yourPuzzleRatingX', strong(ctrl.data.user.rating)))),
    ctrl.data.user.history ? m('div.user_chart', {
      config: function(el, isUpdate, context) {
        if (isUpdate) return;
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
      m('a.button.active', {
        href: ctrl.router.Puzzle.home().url
      }, 'Puzzle'),
      m('a.button', {
        href: ctrl.router.Coordinate.home().url
      }, 'Coordinate')
    ]),
    ctrl.data.user ? renderUserInfos(ctrl) : null
  ]);
}

function renderSide(ctrl) {
  return m('div.side', [
    renderTrainingBox(ctrl),
    // ctrl.difficulty ? renderDifficulty(ctrl) : null,
    // renderCommentary(ctrl),
    // renderResult(ctrl)
  ]);
}

// (q/defcomponent Side [{:keys [commentary mode win attempt user difficulty]} router trans ctrl]
//   (d/div {:className "side"}
//          (TrainingBox {:user user
//                        :difficulty difficulty} router trans)
//          (when difficulty (Difficulty difficulty ctrl))
//          (case commentary
//            :retry (CommentRetry nil trans)
//            :great (CommentGreat nil trans)
//            :fail (CommentFail (= "try" mode) trans)
//            commentary)
//          (case win
//            true (CommentWin nil trans)
//            false (CommentLoss nil trans)
//            (case (:win attempt)
//              true (CommentWin attempt trans)
//              false (CommentLoss attempt trans)
//              ""))))



function renderFooter(ctrl) {
  if (ctrl.data.mode != 'view') return null;
  var fen = chessground.fen.write(ctrl.chessground.data.pieces);
  return m('div', [
    // renderViewControls(ctrl, fen),
    // renderContinueLinks(ctrl)
  ]);
}

function renderPlayTable(ctrl) {
  return m('div.lichess_table.onbg',
    m('div.table_inner', [
      m('div.lichess_current_player',
        m('div.lichess_player.' + ctrl.chessground.data.turnColor, [
          m('div.piece.king.' + ctrl.chessground.data.turnColor),
          m('p', ctrl.trans(ctrl.chessground.data.turnColor == ctrl.data.puzzle.color ? 'yourTurn' : 'waiting'))
        ])
      ),
      m('p.findit', ctrl.trans(ctrl.data.puzzle.color == 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack')),
      m('div.lichess_control',
        m('a.button', {
          onclick: ctrl.giveUp
        }, ctrl.trans('giveUp'))
      )
    ])
  );
}

function renderVote(ctrl) {
  return 'vote';
}

function renderViewTable(ctrl) {
  return m('div', [
    (ctrl.data.puzzle.enabled && ctrl.data.voted === false) ? m('div.please_vote', [
      m('p.first', [
        m('strong', ctrl.trans('wasThisPuzzleAnyGood')),
        m('br'),
        m('span', ctrl.trans('pleaseVotePuzzle'))
      ]),
      m('p.then',
        m('strong', ctrl.trans('thankYou'))
      )
    ]) : null,
    m('div.box', [
      (ctrl.data.puzzle.enabled && ctrl.data.user) ? renderVote(ctrl) : null,
      m('h2',
        m('a', {
          href: ctrl.router.Puzzle.show(ctrl.data.puzzle.id)
        }, ctrl.trans('puzzleId', ctrl.data.puzzle.id))
      ),
      m('p', m.trust(ctrl.trans('ratingX', strong(ctrl.data.puzzle.rating)))),
      m('p', m.trust(ctrl.trans('playedXTimes', strong(ctrl.data.puzzle.attempts)))),
      m('p',
        m('input.copyable[readonly][spellCheck=false]', {
          value: ctrl.data.puzzle.url
        })
      )
    ]),
    m('div.continue_wrap', [
      ctrl.data.win === null ? m('button.continue.button[data-icon=G]', {
        onclick: ctrl.newPuzzle
      }, ctrl.trans('continueTraining')) : m('a.continue.button[data-icon=G]', {
        onclick: ctrl.newPuzzle
      }, ctrl.trans('startTraining')), !(ctrl.data.win === null ? ctrl.data.attempt.win : ctrl.data.win) ? m('a.retry[data-icon=P]', {
        onclick: ctrl.retry
      }, ctrl.trans('retryThisPuzzle')) : null
    ])
  ]);
}

function renderRight(ctrl) {
  return m('div.right', {
      config: function(el, isUpdate, context) {
        el.style.top = (256 - el.offsetHeight / 2) + 'px';
      }
    },
    ctrl.data.mode === 'view' ? renderViewTable(ctrl) : renderPlayTable(ctrl)
  );
}

function renderHistory(ctrl) {
  return m('div.history', ctrl.data.historyHtml ? m.trust(ctrl.data.historyHtml) : null);
}

module.exports = function(ctrl) {
  return m('div#puzzle.training', [
    renderSide(ctrl),
    renderRight(ctrl),
    m('div.center', [
      chessground.view(ctrl.chessground),
      // renderFooter(ctrl),
      renderHistory(ctrl)
    ])
  ]);
};
