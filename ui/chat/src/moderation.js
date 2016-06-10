var m = require('mithril');

module.exports = {
  ctrl: function(opts) {
    var vm = {
      data: m.prop(null),
      // data: m.prop({
      //   username: 'Foobidoo'
      // }),
      loading: m.prop(false)
    };
    var close = function() {
      vm.data(null);
    };
    return {
      vm: vm,
      reasons: opts.reasons,
      open: function(username) {
        vm.data({
          username: username
        });
        m.redraw();
      },
      close: close,
      timeout: function(reason) {
        opts.send('timeout', {
          username: vm.data().username,
          reason: reason
        });
        close();
      }
    }
  },
  view: {
    lineAction: m('i', {
      class: 'mod',
      'data-icon': '',
      title: 'Moderation'
    }),
    ui: function(ctrl) {
      if (!ctrl) return;
      var data = ctrl.vm.data();
      if (!data) return;
      return m('div.moderation', [
        m('div.top', [
          m('span.toggle_chat', {
            'data-icon': 'L',
            onclick: ctrl.close
          }),
          m('span.text', {
            'data-icon': '',
          }, data.username)
        ]),
        m('div.timeout', [
          m('h2', 'Timeout 10 minutes for:'),
          ctrl.reasons.map(function(r) {
            return m('a.text[data-icon=p]', {
              onclick: function() {
                ctrl.timeout(r.key)
              }
            }, r.name);
          })
        ])
      ]);
    }
  }
};
