var m = require('mithril');
var xhr = require('./xhr');

function isToday(timestamp) {
  return moment(timestamp).isSame(new Date(), 'day');
}

module.exports = {
  ctrl: function(opts) {
    var vm = {
      data: m.prop(null),
      loading: m.prop(false)
    };
    var close = function() {
      vm.data(null);
      m.redraw.strategy('all');
    };
    return {
      vm: vm,
      reasons: opts.reasons,
      open: function(username) {
        vm.loading(true);
        xhr.userModInfo(username).then(function(data) {
          vm.data(data);
          vm.loading(false);
          m.redraw();
        });
        m.redraw();
      },
      close: close,
      timeout: function(reason) {
        opts.send('timeout', {
          userId: vm.data().id,
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
      if (ctrl.vm.loading()) return m.trust(lichess.spinnerHtml);
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
        m('div.timeout.block', [
          m('h2', 'Timeout 10 minutes for'),
          ctrl.reasons.map(function(r) {
            return m('a.text[data-icon=p]', {
              onclick: function() {
                ctrl.timeout(r.key)
              }
            }, r.name);
          })
        ]),
        m('div.history.block', [
          m('h2', 'Timeout history'),
          m('table', m('tbody.slist', {
            config: function(el, isUpdate) {
              if (!isUpdate) $('body').trigger('lichess.content_loaded');
            }
          }, data.history.map(function(e) {
            return m('tr', [
              m('td.reason', e.reason),
              m('td.mod', e.mod),
              m('td', m('time', {
                class: "moment",
                'data-format': isToday(e.date) ? 'LT' : 'DD/MM/YY',
                datetime: new Date(e.date).toISOString()
              }))
            ]);
          })))
        ])
      ]);
    }
  }
};
