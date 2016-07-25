var m = require('mithril');
var util = require('chessground').util;
var tds = require('../util').tds;
var hookRepo = require('../../hookRepo');

function renderHook(ctrl, hook) {
  return m('tr', {
    key: hook.id,
    title: hook.disabled ? '' : (
      (hook.action === 'join') ? ctrl.trans('joinTheGame') + ' | ' + hook.perf : ctrl.trans('cancel')
    ),
    'data-id': hook.id,
    class: 'hook ' + hook.action + (hook.disabled ? ' disabled' : '')
  }, tds([
    m('span', {
      class: 'is is2 color-icon ' + (hook.c || 'random')
    }), (hook.rating ? m('span.ulink.ulpt', {
      'data-href': '/@/' + hook.u
    }, hook.u) : 'Anonymous'),
    hook.rating ? hook.rating : '',
    hook.clock, [m('span', {
      class: 'varicon',
      'data-icon': ctrl.perfIcons[hook.perf]
    }), ctrl.trans(hook.ra ? 'rated' : 'casual')]
  ]));
};

function isStandard(value) {
  return function(hook) {
    return (hook.variant === 'standard') === value;
  };
}

function isMine(hook) {
  return hook.action === 'cancel';
}

function isNotMine(hook) {
  return !isMine(hook);
}

module.exports = {
  toggle: function(ctrl) {
    return m('span', {
      'data-hint': ctrl.trans('graph'),
      class: 'mode_toggle hint--bottom',
      onmousedown: util.partial(ctrl.setMode, 'chart')
    }, m('span.chart[data-icon=9]'));
  },
  render: function(ctrl, allHooks) {
    var mine = allHooks.filter(isMine)[0];
    var max = mine ? 13 : 14;
    var hooks = allHooks.slice(0, max);
    var render = util.partial(renderHook, ctrl);
    var standards = hooks.filter(isNotMine).filter(isStandard(true));
    hookRepo.sort(ctrl, standards);
    var variants = hooks.filter(isNotMine).filter(isStandard(false))
      .slice(0, Math.max(0, max - standards.length - 1));
    hookRepo.sort(ctrl, variants);
    var renderedHooks = [
      standards.map(render),
      variants.length ? m('tr.variants', {
          key: 'variants',
        },
        m('td', {
          colspan: 5
        }, '— ' + ctrl.trans('variant') + ' —')
      ) : null,
      variants.map(render)
    ];
    if (mine) renderedHooks.unshift(render(mine));
    return m('table.table_wrap', [
      m('thead',
        m('tr', [
          m('th'),
          m('th', ctrl.trans('player')),
          m('th', {
            class: util.classSet({
              sortable: true,
              sort: ctrl.vm.sort === 'rating'
            }),
            onclick: util.partial(ctrl.setSort, 'rating')
          }, [m('i.is'), ctrl.trans('rating')]),
          m('th', {
            class: util.classSet({
              sortable: true,
              sort: ctrl.vm.sort === 'time'
            }),
            onclick: util.partial(ctrl.setSort, 'time')
          }, [m('i.is'), ctrl.trans('time')]),
          m('th', ctrl.trans('mode'))
        ])
      ),
      m('tbody', {
        class: ctrl.vm.stepping ? 'stepping' : '',
        onclick: function(e) {
          var el = e.target;
          do {
            el = el.parentNode;
            if (el.nodeName === 'TR') return ctrl.clickHook(el.getAttribute('data-id'));
          }
          while (el.nodeName !== 'TABLE');
        }
      }, renderedHooks)
    ]);
  }
};
