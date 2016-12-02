var m = require('mithril');

module.exports = function(ctrl) {
  return [
    ctrl.data.pools.map(function(pool) {
      var active = ctrl.vm.inPool === pool.id;
      var transp = ctrl.vm.inPool && !active;
      return m('div.pool', {
        class: active ? 'active' : (transp ? 'transp' : ''),
        onclick: function() {
          ctrl.clickPool(pool.id);
        }
      }, [
        m('div.clock', pool.lim + '+' + pool.inc),
        m('div.perf', pool.perf),
        active ? m.trust(lichess.spinnerHtml) : null
      ]);
    }),
    m('div.custom', {
      class: ctrl.vm.inPool ? 'transp' : '',
      onclick: function() {
        ctrl.clickPool(null);
        $('#start_buttons .config_hook').mousedown();
      }
    }, 'Custom')
  ];
};
