var m = require('mithril');
var util = require('chessground').util;

function tds(bits) {
  return bits.map(function(bit) {
    return {
      tag: 'td',
      children: [bit]
    };
  });
}

function renderHook(ctrl, hook) {
  var title = (hook.action === 'join') ? ctrl.trans('joinTheGame') + ' - ' + hook.perf.name : ctrl.trans('cancel');
  return m('tr', {
    title: (hook.action === 'join') ? ctrl.trans('joinTheGame') + ' - ' + hook.perf.name : ctrl.trans('cancel'),
    'data-id': hook.id,
    class: hook.action,
    onclick: util.partial(ctrl.clickHook, hook)
  }, tds([
    m('span', {
      class: 'is is2 color-icon ' + (hook.color || 'random')
    }), (hook.rating ? m('a', {
      href: '/@/' + hook.username
    }, hook.username) : 'Anonymous'),
    hook.rating ? hook.rating : '',
    hook.clock, [m('span', {
      class: 'varicon',
      'data-icon': hook.perf.icon
    }), ctrl.trans(hook.mode === 1 ? 'rated' : 'casual')]
  ]));
};

module.exports = function(ctrl) {
  return m('table.table_wrap', [
    m('thead',
      m('tr', [
        m('th', m('span', {
          'data-hint': ctrl.trans('graph'),
          class: 'toggle hint--bottom'
        }, m('span.chart[data-icon=9]'))),
        m('th', ctrl.trans('player')),
        m('th', 'Rating'),
        m('th', ctrl.trans('time')),
        m('th', ctrl.trans('mode'))
      ])
    ),
    m('tbody', ctrl.data.hooks.map(util.partial(renderHook, ctrl)))
  ]);
};
