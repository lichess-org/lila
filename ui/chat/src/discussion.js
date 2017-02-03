var m = require('mithril');
var moderationView = require('./moderation').view;
var presetView = require('./preset').view;
var enhance = require('./enhance');
var spam = require('./spam');

function renderLine(ctrl) {
  return function(line) {
    if (!line.html) line.html = enhance(line.t, {
      parseMoves: ctrl.vm.parseMoves
    });
    if (line.u === 'lichess') return m('li.system', line.html);
    if (line.c) return m('li', [
      m('span', '[' + line.c + ']'),
      line.t
    ]);
    return m('li', {
      'data-username': line.u
    }, [
      ctrl.vm.isMod ? moderationView.lineAction() : null,
      m.trust(
        $.userLinkLimit(line.u, 14) + line.html
      )
    ]);
  };
}

function sameLines(l1, l2) {
  return l1.d && l2.d && l1.u === l2.u;
}

function selectLines(ctrl) {
  var prev, ls = [];
  ctrl.data.lines.forEach(function(line) {
    if (!line.d &&
      (!prev || !sameLines(prev, line)) &&
      (!line.r || ctrl.vm.isTroll) &&
      !spam.skip(line.t)
    ) ls.push(line);
    prev = line;
  });
  return ls;
}

function input(ctrl) {
  if ((ctrl.data.loginRequired && !ctrl.data.userId) || ctrl.data.restricted) return m('input.lichess_say', {
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
          if (e.target.value === '') {
            var kbm = document.querySelector('.keyboard-move input');
            if (kbm) kbm.focus();
          } else {
            var txt = e.target.value;
            spam.report(txt);
            if (ctrl.public && spam.hasTeamUrl(txt)) alert("Please don't advertise teams in the chat.");
            else ctrl.post(txt);
            e.target.value = '';
          }
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
            if (!isUpdate) {
              if (ctrl.moderation) $(el).on('click', 'i.mod', function(e) {
                ctrl.moderation.open($(e.target).parent().data('username'));
              });
              $(el).on('click', 'a.jump', function(e) {
                lichess.pubsub.emit('jump')(e.target.getAttribute('data-ply'));
              });
            }
            if (ctrl.data.lines.length > 5) {
              var autoScroll = (el.scrollTop === 0 || (el.scrollTop > (el.scrollHeight - el.clientHeight - 100)));
              el.scrollTop = 999999;
              if (autoScroll) setTimeout(function() {
                el.scrollTop = 999999;
              }, 500);
            }
          }
        },
        selectLines(ctrl).map(renderLine(ctrl))
      ),
      input(ctrl),
      presetView(ctrl.preset)
    ];
  }
};
