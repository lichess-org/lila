var m = require('mithril');
var moderationView = require('./moderation').view;

var deletedDom = m('em.deleted', '<deleted>');

function renderLine(ctrl) {
  return function(line) {
    if (line.u === 'lichess') return m('li', m('em.system', line.t));
    return m('li', {
      'data-username': line.u
    }, [
      ctrl.vm.isMod ? moderationView.lineAction : null,
      m.trust($.userLinkLimit(line.u, 14)),
      line.d ? deletedDom : line.t
    ]);
  };
}

function sameLines(l1, l2) {
  return l1.d && l2.d && l1.u === l2.u;
}

function dedupLines(lines) {
  var prev, ls = [];
  lines.forEach(function(l) {
    if (!prev || !sameLines(prev, l)) ls.push(l);
    prev = l;
  });
  return ls;
}

function input(ctrl) {
  var placeholder = ctrl.vm.isTimeout() ? 'You have been timed out.' : ctrl.trans(ctrl.vm.placeholderKey);
  return m('input', {
    class: 'lichess_say',
    placeholder: placeholder,
    autocomplete: 'off',
    maxlength: 140,
    disabled: ctrl.vm.isTimeout(),
    config: function(el, isUpdate) {
      if (!isUpdate) el.addEventListener('keypress', function(e) {
        if (e.which == 10 || e.which == 13) {
          ctrl.post(e.target.value);
          e.target.value = '';
        }
      });
    }
  })
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
      dedupLines(ctrl.lines).map(renderLine(ctrl))
    ),
    input(ctrl)
  ])
}

module.exports = function(ctrl) {
  return m('div', {
      class: 'mchat' + (ctrl.vm.isMod ? ' mod' : '')
    },
    moderationView.ui(ctrl.moderation) || discussion(ctrl));
};
