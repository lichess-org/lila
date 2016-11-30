var m = require('mithril');

module.exports = {
  render: function(ctrl) {
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
          m('div.clock', pool.lim + '+' + pool.inc),
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
  },
  anonOverlay: function() {
    return m('div.pool_overlay', m('div.text', [
      m('h2', 'Rated games only!'),
      m('p', [
        'Therefore it requires ',
        m('a[href=/signup]', 'making a user account'),
        '.'
      ]),
      m('p', 'Fortunately, it\'s easy and free forever!')
    ]));
  }
};
