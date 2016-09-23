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
      vm.loading(false);
    };
    return {
      vm: vm,
      reasons: opts.reasons,
      permissions: opts.permissions,
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
    lineAction: function() {
      return m('i', {
        class: 'mod',
        'data-icon': '',
        title: 'Moderation'
      });
    },
    ui: function(ctrl) {
      if (!ctrl) return;
      if (ctrl.vm.loading()) return m.trust(lichess.spinnerHtml);
      var data = ctrl.vm.data();
      if (!data) return;
      return [
        m('div.top', [
          m('span.text', {
            'data-icon': '',
          }, m.trust($.userLink(data.username))),
          m('span.toggle_chat', {
            'data-icon': 'L',
            onclick: ctrl.close
          })
        ]),
        m('div.content.moderation', [
          m('div.infos.block', [
            [
              data.games + ' games',
              data.troll ? 'TROLL' : null,
              data.engine ? 'ENGINE' : null,
              data.booster ? 'BOOSTER' : null
            ].filter(function(x) {
              return x;
            }).join(' • '),
            ' • ',
            m('a[href=/@/' + data.username + '?mod]', 'profile'),
            ctrl.permissions.shadowban ? [
              ' • ',
              m('a[href=/mod/' + data.username + '/communication]', 'coms')
            ] : null
          ]),
          m('div.timeout.block', [
            m('h2', 'Timeout 10 minutes for'),
            ctrl.reasons.map(function(r) {
              return m('a.text[data-icon=p]', {
                onclick: function() {
                  ctrl.timeout(r.key)
                }
              }, r.name);
            }), (data.troll || !ctrl.permissions.shadowban) ? null : m('div.shadowban', [
              'Or ',
              m('form', {
                action: '/mod/' + data.id + '/troll?set=1',
                method: 'post',
                config: function(el, isUpdate) {
                  if (!isUpdate) $(el).submit(function() {
                    $.post($(this).attr('action'), function() {
                      ctrl.open(data.username);
                    });
                    ctrl.vm.loading(true);
                    m.redraw();
                    return false;
                  });
                }
              }, m('button.button[type=submit]', 'shadowban'))
            ])
          ]),
          m('div.history.block', [
            m('h2', 'Timeout history'),
            m('table', m('tbody.slist', {
              config: function(el, isUpdate) {
                if (!isUpdate) lichess.pubsub.emit('content_loaded')();
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
        ])
      ];
    }
  }
};
