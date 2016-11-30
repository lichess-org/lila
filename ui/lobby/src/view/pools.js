var m = require('mithril');

module.exports = function(ctrl) {
  return [
    ctrl.data.pools.map(function(pool) {
      var active = ctrl.inPool === pool.id;
      var transp = ctrl.inPool && !active;
      return m('div.pool', {
        class: active ? 'active' : (transp ? 'transp' : ''),
        onclick: function() {
          ctrl.clickPool(pool.id);
        }
      }, [
        m('div.clock', [
          m('strong', pool.lim),
          '+',
          m('strong', pool.inc)
        ]),
        m('div.perf', pool.perf),
        active ? m.trust(lichess.spinnerHtml) : null
      ]);
    }),
    m('div.custom', {
      class: ctrl.inPool ? 'transp' : '',
      onclick: function() {
        $('#start_buttons .config_hook').mousedown();
      }
    }, 'Custom')
  ];
}
