var m = require('mithril');

function renderRange(range) {
  return m('div.range', range.replace('-', ' - '));
}

module.exports = function(ctrl) {
  var member = ctrl.vm.poolMember;
  return [
    ctrl.data.pools.map(function(pool) {
      var active = member && member.id === pool.id;
      var transp = member && !active;
      return m('div.pool', {
        class: active ? 'active' : (transp ? 'transp' : ''),
        onclick: function() {
          ctrl.clickPool(pool.id);
        }
      }, [
        m('div.clock', pool.lim + '+' + pool.inc), (active && member.range) ? renderRange(member.range) : m('div.perf', pool.perf),
        active ? m.trust(lichess.spinnerHtml) : null
      ]);
    }),
    m('div.custom', {
      class: member ? 'transp' : '',
      onclick: function() {
        $('#start_buttons .config_hook').mousedown();
      }
    }, ctrl.trans('custom'))
  ];
};
