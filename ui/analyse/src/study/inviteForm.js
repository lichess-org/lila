var m = require('mithril');
var partial = require('chessground').util.partial;

module.exports = {
  ctrl: function(send, members, setTab) {
    var open = m.prop(false);
    var candidates = m.prop([]);
    var invite = function(username) {
      send("invite", username);
      open(false);
      setTab();
    };
    var updateCandidates = function(f) {
      candidates(f(candidates()).sort());
      if (open()) m.redraw();
    };
    return {
      open: open,
      candidates: candidates,
      members: members,
      setCandidates: function(usernames) {
        updateCandidates(function(prevs) {
          return usernames;
        });
      },
      delCandidate: function(username) {
        updateCandidates(function(prevs) {
          return prevs.filter(function(u) {
            return username !== u;
          });
        });
      },
      addCandidate: function(username) {
        updateCandidates(function(prevs) {
          return prevs.concat([username]);
        });
      },
      toggle: function() {
        open(!open());
      },
      invite: invite
    };
  },
  view: function(ctrl) {
    var memberUsernames = ctrl.members();
    return m('div.lichess_overboard.study_overboard.study_invite', [
      m('a.close.icon[data-icon=L]', {
        onclick: partial(ctrl.open, false)
      }),
      m('h2', 'Invite to the study'),
      m('div.users', ctrl.candidates().filter(function(u) {
        return memberUsernames.indexOf(u) === -1;
      }).map(function(username) {
        return m('span.user_link.ulpt', {
          'data-href': '/@/' + username,
          onclick: partial(ctrl.invite, username)
        }, username);
      })),
      m('input', {
        config: function(el, isUpdate) {
          if (isUpdate) return;
          lichess.userAutocomplete($(el), {
            onSelect: function(v) {
              ctrl.invite(v);
              $(el).typeahead('close');
              el.value = '';
              m.redraw();
            }
          });
        },
        placeholder: 'Or search by username'
      })
    ]);
  }
};
