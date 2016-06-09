var m = require('mithril');

function renderLine(ctrl) {
  return function(line) {
    return m('li', [
      m('a', {
        class: 'user_link ulpt',
        href: '/@/' + line.u
      }, line.u),
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
    m('div.chat_pannels',
      m('div.messages_container',
        m('ol.messages.content.scroll-shadow-soft',
          ctrl.lines.map(renderLine(ctrl))
        )
      )
    )
  ]);
};
