var m = require('mithril');
var tds = require('./util').tds;

function renderSeek(ctrl, seek) {
  return m('tr', {
    key: seek.id,
    title: (seek.action === 'joinSeek') ? ctrl.trans('joinTheGame') + ' - ' + seek.perf.name : ctrl.trans('cancel'),
    'data-id': seek.id,
    class: 'seek ' + (seek.action === 'joinSeek' ? 'join' : 'cancel')
  }, tds([
    m('span', {
      class: 'is is2 color-icon ' + (seek.color || 'random')
    }), (seek.rating ? m('span.ulpt', {
      'data-href': '/@/' + seek.username
    }, seek.username) : 'Anonymous'),
    seek.rating + (seek.provisional ? '?' : ''),
    seek.days ? ctrl.trans(seek.days === 1 ? 'oneDay' : 'nbDays', seek.days) : '∞', [m('span', {
      class: 'varicon',
      'data-icon': seek.perf.icon
    }), ctrl.trans(seek.mode === 1 ? 'rated' : 'casual')]
  ]));
}

function createSeek(ctrl) {
  if (ctrl.data.me && ctrl.data.seeks.length < 8)
    return m('div.create',
      m('a.button', {
        href: '/?time=correspondence#hook',
      }, ctrl.trans('createAGame'))
    );
}

module.exports = function(ctrl) {
  return [
    m('table.table_wrap', [
      m('thead',
        m('tr', [
          m('th'),
          m('th', ctrl.trans('player')),
          m('th', ctrl.trans('rating')),
          m('th', ctrl.trans('time')),
          m('th', ctrl.trans('mode'))
        ])
      ),
      m('tbody', {
        onclick: function(e) {
          var el = e.target;
          do {
            el = el.parentNode;
            if (el.nodeName === 'TR') {
              if (!ctrl.data.me) {
                if (confirm(ctrl.trans('youNeedAnAccountToDoThat'))) location.href = '/signup';
                return;
              }
              return ctrl.clickSeek(el.getAttribute('data-id'));
            }
          }
          while (el.nodeName !== 'TABLE');
        }
      }, ctrl.data.seeks.map(lichess.partial(renderSeek, ctrl)))
    ]),
    createSeek(ctrl)
  ];
};
