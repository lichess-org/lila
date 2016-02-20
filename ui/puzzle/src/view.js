var map = require('lodash/collection/map');
var chessground = require('chessground');
var partial = chessground.util.partial;
var m = require('mithril');
var puzzle = require('./puzzle');
var xhr = require('./xhr');

// useful in translation arguments
function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function renderUserInfos(ctrl) {
  return m('div.chart_container', [
    m('p', m.trust(ctrl.trans('yourPuzzleRatingX', strong(ctrl.data.user.rating)))),
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

function renderDifficulty(ctrl) {
  return m('div.difficulty', map(ctrl.data.difficulty.choices, function(dif) {
    var id = dif[0],
      name = dif[1];
    return m('a.button' + (id == ctrl.data.difficulty.current ? '.active' : ''), {
      disabled: id == ctrl.data.difficulty.current,
      onclick: partial(xhr.setDifficulty, ctrl, id)
    }, name);
  }));
}

function renderCommentary(ctrl) {
  switch (ctrl.data.comment) {
    case 'retry':
      return m('div.comment.retry', [
        m('h3', m('strong', ctrl.trans('goodMove'))),
        m('span', ctrl.trans('butYouCanDoBetter'))
      ]);
    case 'great':
      return m('div.comment.great', [
        m('h3.text[data-icon=E]', m('strong', ctrl.trans('bestMove'))),
        m('span', ctrl.trans('keepGoing'))
      ]);
    case 'fail':
      return m('div.comment.fail', [
        m('h3.text[data-icon=k]', m('strong', ctrl.trans('puzzleFailed'))),
        ctrl.data.mode == 'try' ? m('span', ctrl.trans('butYouCanKeepTrying')) : null
      ]);
    default:
      return ctrl.data.comment
  }
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
    attempt ? m('span', ctrl.trans('puzzleSolvedInXSeconds', attempt.seconds)) : null
  ]);
}

function renderLoss(ctrl, attempt) {
  return m('div.comment.loss',
    m('h3.text[data-icon=k]', [
      m('strong', ctrl.trans('puzzleFailed')),
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
    ctrl.data.difficulty ? renderDifficulty(ctrl) : null,
    renderCommentary(ctrl),
    renderResult(ctrl)
  ]);
}

function renderPlayTable(ctrl) {
  return m('div.table_wrap',
    m('div.table',
      m('div.table_inner', [
        m('div.current_player',
          m('div.player.' + ctrl.chessground.data.turnColor, [
            m('div.no-square', m('piece.king.' + ctrl.chessground.data.turnColor)),
            m('p', ctrl.trans(ctrl.chessground.data.turnColor == ctrl.data.puzzle.color ? 'yourTurn' : 'waiting'))
          ])
        ),
        m('p.findit', ctrl.trans(ctrl.data.puzzle.color == 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack')),
        m('div.control',
          m('a.button.giveup', {
            config: function(el, isUpdate) {
              setTimeout(function() {
                el.classList.add('revealed');
              }, 1000);
            },
            onclick: partial(xhr.attempt, ctrl, 0)
          }, ctrl.trans('giveUp'))
        )
      ])
    )
  );
}

function renderVote(ctrl) {
  return m('div.upvote' + (ctrl.data.attempt ? '.enabled' : ''), [
    m('a[data-icon=S]', {
      title: ctrl.trans('thisPuzzleIsCorrect'),
      class: ctrl.data.attempt.vote ? ' active' : '',
      onclick: partial(xhr.vote, ctrl, 1)
    }),
    m('span.count.hint--bottom[data-hint=Popularity]', ctrl.data.puzzle.vote),
    m('a[data-icon=R]', {
      title: ctrl.trans('thisPuzzleIsWrong'),
      class: ctrl.data.attempt.vote === false ? ' active' : '',
      onclick: partial(xhr.vote, ctrl, 0)
    })
  ]);
}

function renderViewTable(ctrl) {
  return [
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
      m('h2.text[data-icon="-"]',
        m('a', {
          href: ctrl.router.Puzzle.show(ctrl.data.puzzle.id).url
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
      ctrl.data.win === null ? m('button.continue.button.text[data-icon=G]', {
        onclick: partial(xhr.newPuzzle, ctrl)
      }, ctrl.trans('continueTraining')) : m('a.continue.button.text[data-icon=G]', {
        onclick: partial(xhr.newPuzzle, ctrl)
      }, ctrl.trans('continueTraining')), !(ctrl.data.win === null ? ctrl.data.attempt.win : ctrl.data.win) ? m('a.retry.text[data-icon=P]', {
        onclick: partial(xhr.retry, ctrl)
      }, ctrl.trans('retryThisPuzzle')) : null
    ])
  ];
}

function renderViewControls(ctrl, fen) {
  var history = ctrl.data.replay.history;
  var step = ctrl.data.replay.step;
  return m('div.game_control', [
    ctrl.data.puzzle.gameId ? m('a.button.hint--bottom', {
      'data-hint': ctrl.trans('fromGameLink', ctrl.data.puzzle.gameId),
      href: ctrl.router.Round.watcher(ctrl.data.puzzle.gameId, ctrl.data.puzzle.color).url + '#' + ctrl.data.puzzle.initialPly
    }, m('span[data-icon=v]')) : null,
    m('a.button.hint--bottom', {
      'data-hint': ctrl.trans('boardEditor'),
      href: ctrl.router.Editor.load(fen).url
    }, m('span[data-icon=m]')),
    m('a.button.hint--bottom', {
      'data-hint': ctrl.trans('analysis'),
      href: puzzle.makeUrl('/analysis/', fen) + '?color=' + ctrl.chessground.data.orientation,
    }, m('span[data-icon=A]')),
    m('a.button.hint--bottom', {
      'data-hint': ctrl.trans('continueFromHere'),
      onclick: function() {
        $.modal($('.continue_with'));
      }
    }, m('span[data-icon=U]')),
    m('div#GameButtons.hint--bottom', {
      'data-hint': 'Review puzzle solution'
    }, [
      ['first', 'W', 0],
      ['prev', 'Y', step - 1],
      ['next', 'X', step + 1],
      ['last', 'V', history.length - 1]
    ].map(function(b) {
      var enabled = step != b[2] && b[2] >= 0 && b[2] < history.length;
      return m('a.button.' + b[0] + (enabled ? '' : '.disabled'), {
        'data-icon': b[1],
        onclick: enabled ? partial(ctrl.jump, b[2]) : null
      });
    }))
  ]);
}

function renderContinueLinks(ctrl, fen) {
  return m('div.continue_with', [
    m('a.button', {
      href: '/?fen=' + fen + '#ai',
      rel: 'nofollow'
    }, ctrl.trans('playWithTheMachine')),
    m('br'),
    m('a.button', {
      href: '/?fen=' + fen + '#friend',
      rel: 'nofollow'
    }, ctrl.trans('playWithAFriend'))
  ]);
}

function renderFooter(ctrl) {
  if (ctrl.data.mode != 'view') return null;
  var fen = ctrl.data.replay.history[ctrl.data.replay.step].fen;
  return m('div', [
    renderViewControls(ctrl, fen),
    renderContinueLinks(ctrl, fen)
  ]);
}

function renderHistory(ctrl) {
  return m('div.history', {
    config: function(el, isUpdate, context) {
      var hash = ctrl.data.user.history.join('');
      if (hash == context.hash) return;
      context.hash = hash;
      $.ajax({
        url: '/training/history',
        success: function(html) {
          el.innerHTML = html;
        }
      });
    }
  });
}

function wheel(ctrl, e) {
  if (ctrl.data.mode != 'view') return true;
  if (e.deltaY > 0) ctrl.jump(ctrl.data.replay.step + 1);
  else if (e.deltaY < 0) ctrl.jump(ctrl.data.replay.step - 1);
  m.redraw();
  e.preventDefault();
  return false;
}

module.exports = function(ctrl) {
  return m('div#puzzle.training', [
    renderSide(ctrl),
    m('div.board_and_ground', [
      m('div', {
          config: function(el, isUpdate) {
            if (!isUpdate) el.addEventListener('wheel', function(e) {
              return wheel(ctrl, e);
            });
          }
        },
        chessground.view(ctrl.chessground)),
      m('div.right', ctrl.vm.loading ? m.trust(lichess.spinnerHtml) : (ctrl.data.mode == 'view' ? renderViewTable(ctrl) : renderPlayTable(ctrl)))
    ]),
    m('div.underboard',
      m('div.center', [
        renderFooter(ctrl),
        ctrl.data.user ? renderHistory(ctrl) : null
      ])
    )
  ]);
};
