var m = require('mithril');
var moderationView = require('./moderation').view;
var presetView = require('./preset').view;

function renderLine(ctrl) {
  return function(line) {
    if (line.u === 'lichess') return m('li', m('em.system', line.t));
    return m('li', {
      'data-username': line.u
    }, [
      ctrl.vm.isMod ? moderationView.lineAction() : null,
      m.trust($.userLinkLimit(line.u, 14)),
      line.d ? m('em.deleted', '<deleted>') : line.t
    ]);
  };
}

function sameLines(l1, l2) {
  return l1.d && l2.d && l1.u === l2.u;
}

function selectLines(lines) {
  var prev, ls = [];
  lines.forEach(function(l) {
    if (!prev || !sameLines(prev, l))
      if (!l.r || ctrl.vm.isTroll) ls.push(l);
    prev = l;
  });
  return ls;
}

function input(ctrl) {
  if (ctrl.data.loginRequired && !ctrl.data.userId) return m('input.lichess_say', {
    placeholder: 'Login to chat',
    disabled: true
  });
  var placeholder;
  if (ctrl.vm.isTimeout()) placeholder = 'You have been timed out.';
  else if (!ctrl.vm.writeable()) placeholder = 'Invited members only.';
  else placeholder = ctrl.trans(ctrl.vm.placeholderKey);
  return m('input', {
    class: 'lichess_say',
    placeholder: placeholder,
    autocomplete: 'off',
    maxlength: 140,
    disabled: ctrl.vm.isTimeout() || !ctrl.vm.writeable(),
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

module.exports = {
  view: function(ctrl) {
    if (!ctrl.vm.enabled()) return null;
    return [
      m('ol.messages.content.scroll-shadow-soft', {
          config: function(el, isUpdate, ctx) {
            if (!isUpdate && ctrl.moderation) $(el).on('click', 'i.mod', function(e) {
              ctrl.moderation.open($(e.target).parent().data('username'));
            });
            var autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 100)));
            el.scrollTop = 999999;
            if (autoScroll) setTimeout(function() {
              el.scrollTop = 999999;
            }, 500);
          }
        },
        selectLines(ctrl.data.lines).map(renderLine(ctrl))
      ),
      input(ctrl),
      presetView(ctrl.preset)
    ];
  }
};
