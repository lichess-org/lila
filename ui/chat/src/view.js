var m = require('mithril');

function renderLine(ctrl) {
  return function(line) {
    return m('li', [
      m.trust($.userLinkLimit(line.u, 14)),
      line.t
    ]);
  };
}

module.exports = function(ctrl) {
  return m('div.mchat', [
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
      onkeypress: function(e) {
        if (e.which == 10 || e.which == 13) {
          ctrl.post(e.target.value);
          e.target.value = '';
        }
      }
    })
  ]);
};
