var m = require('mithril');
var moderationView = require('./moderation').view;
var presetView = require('./preset').view;

var delocalizePattern = /(^|[\s\n]|<[A-Za-z]*\/?>)\w{2}\.lichess\.org/gi;

function delocalize(html) {
  return html.replace(delocalizePattern, '$1lichess.org');
}

function escapeHtml(html) {
  return html
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

var isSpammer = lichess.storage.make('spammer');

function isSpam(txt) {
  return /chess-bot\.com/.test(txt);
}

function skipSpam(txt) {
  if (isSpam(txt) && isSpammer.get() != '1') return true;
  return false;
}

var linkPattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?):\/\/|lichess\.org\/)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;

var linkReplace = function(match, before, url) {
  var fullUrl = url.indexOf('http') === 0 ? url : 'https://' + url;
  var minUrl = url.replace(/^(?:https:\/\/)?(.+)$/, '$1');
  return before + '<a target="_blank" rel="nofollow" href="' + fullUrl + '">' + minUrl + '</a>';
};

function autoLink(html) {
  return html.replace(linkPattern, linkReplace);
};

function renderLine(ctrl) {
  return function(line) {
    if (!line.html) line.html = m.trust(autoLink(escapeHtml(delocalize(line.t))));
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
      !skipSpam(line.t)
    ) ls.push(line);
    prev = line;
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
          if (e.target.value === '') {
            var kbm = document.querySelector('.keyboard-move input');
            if (kbm) kbm.focus();
          } else {
            if (isSpam(e.target.value)) isSpammer.set(1);
            ctrl.post(e.target.value);
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
            if (!isUpdate && ctrl.moderation) $(el).on('click', 'i.mod', function(e) {
              ctrl.moderation.open($(e.target).parent().data('username'));
            });
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
