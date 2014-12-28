var m = require('mithril');
var tds = require('./util').tds;
var util = require('chessground').util;

function renderSeek(ctrl, seek) {
  return m('tr', {
    key: seek.id,
    title: (seek.action === 'joinSeek') ? ctrl.trans('joinTheGame') + ' - ' + seek.perf.name : ctrl.trans('cancel'),
    'data-id': seek.id,
    class: 'seek ' + seek.action
  }, tds([
    m('span', {
      class: 'is is2 color-icon ' + (seek.color || 'random')
    }), (seek.rating ? m('a.ulink', {
      href: '/@/' + seek.username
    }, seek.username) : 'Anonymous'),
    seek.rating ? seek.rating : '',
    seek.days ?  ctrl.trans(seek.days === 1 ? 'oneDay' : 'nbDays', seek.days) : '∞', [m('span', {
      class: 'varicon',
      'data-icon': seek.perf.icon
    }), ctrl.trans(seek.mode === 1 ? 'rated' : 'casual')]
  ]));
};

module.exports = function(ctrl) {
  return m('table.table_wrap', [
    m('thead',
      m('tr', [
        m('th'),
        m('th', ctrl.trans('player')),
        m('th', 'Rating'),
        m('th', ctrl.trans('time')),
        m('th', ctrl.trans('mode'))
      ])
    ),
    m('tbody', {
      onclick: function(e) {
        var el = e.target;
        if (el.classList.contains('ulink')) return;
        do {
          el = el.parentNode;
          if (el.nodeName === 'TR') {
            if (!this.data.me) {
              if (confirm(this.trans('youNeedAnAccountToDoThat'))) location.href = '/signup';
              return;
            }
            return ctrl.clickSeek(el.getAttribute('data-id'));
          }
        }
        while (el.nodeName !== 'TABLE');
      }
    }, ctrl.data.seeks.map(util.partial(renderSeek, ctrl)))
  ]);
};
