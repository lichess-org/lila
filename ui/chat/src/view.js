var m = require('mithril');
var moderationView = require('./moderation').view;

function renderLine(ctrl) {
  return function(line) {
    return m('li', {
      'data-username': line.u
    }, [
      ctrl.vm.isMod ? moderationView.lineAction : null,
      m.trust($.userLinkLimit(line.u, 14)),
      line.t
    ]);
  };
}

function discussion(ctrl) {
  return m('div.discussion', [
    m('div.top', [
      m('span', ctrl.trans('chatRoom')),
      m('input', {
        type: 'checkbox',
        class: 'toggle_chat',
        title: ctrl.trans('toggleTheChat')
      })
    ]),
    m('ol.messages.content.scroll-shadow-soft', {
        config: function(el, isUpdate, ctx) {
          if (!isUpdate && ctrl.moderation) $(el).on('click', 'i.mod', function(e) {
            ctrl.moderation.open($(e.target).parent().data('username'));
          });
          var autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 150)));
          el.scrollTop = 999999;
          if (autoScroll) setTimeout(function() {
            el.scrollTop = 999999;
          }, 500);
        }
      },
      ctrl.lines.map(renderLine(ctrl))
    ),
    m('input', {
      class: 'lichess_say',
      placeholder: ctrl.trans(ctrl.vm.placeholderKey),
      autocomplete: 'off',
      maxlength: 140,
      config: function(el, isUpdate) {
        if (!isUpdate) el.addEventListener('keypress', function(e) {
          if (e.which == 10 || e.which == 13) {
            ctrl.post(e.target.value);
            e.target.value = '';
          }
        });
      }
    })
  ])
}

module.exports = function(ctrl) {
  return m('div', {
      class: 'mchat' + (ctrl.vm.isMod ? ' mod' : '')
    },
    moderationView.ui(ctrl.moderation) || discussion(ctrl));
};
