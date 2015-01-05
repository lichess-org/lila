var m = require('mithril');
var chessground = require('chessground');

function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function renderTable(ctrl) {
  return m('div.table',
    m('div.table_inner', [])
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

function renderResult(ctrl) {
  return [
    m('div.goal', m.trust(ctrl.trans('findNbGoodMoves', strong(ctrl.vm.nbGood)))),
  ];
}

function renderSide(ctrl) {
  return m('div.side', [
    renderTrainingBox(ctrl),
    renderResult(ctrl)
  ]);
}

module.exports = function(ctrl) {
  var percent = Math.ceil(ctrl.vm.figuredOut.length * 100 / ctrl.vm.nbGood) + '%';
  var color;
  switch (ctrl.vm.flash) {
    case 'dubious':
      color = 'yellow';
      break;
    case 'bad':
      color = 'red';
      break;
    default:
      color = 'green';
  }
  return m('div#opening.training', [
    renderSide(ctrl),
    m('div.board_and_ground', [
      m('div', chessground.view(ctrl.chessground)),
      m('div.right', renderTable(ctrl))
    ]),
    m('div.center', [
      m('div.lulzbar', [
        m('div.bar.' + color, {
          style: {
            width: percent
          }
        }, m('span')),
        m('div.label', {
          style: {
            left: percent
          }
        }, [
          m('span'),
          m('div.perc', percent)
        ])
      ])
    ])
  ]);
};
