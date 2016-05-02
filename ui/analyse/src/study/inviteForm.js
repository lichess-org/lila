var m = require('mithril');
var partial = require('chessground').util.partial;
var objectValues = require('../util').objectValues;

module.exports = {
  ctrl: function(send, members, setTab) {
    var open = m.prop(true);
    var candidates = m.prop([]);
    var invite = function(username) {
      send("invite", username);
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
          return ['veloce', 'lukhas', 'Kingscrusher-Youtube', 'GnarlyGoat', 'Toadofsky', 'Clarkey', 'Unihedron', 'erikelrojo', 'bosspotato'];
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
    var memberIds = Object.keys(ctrl.members());
    return m('div.lichess_overboard.study_overboard.study_invite', [
      m('a.close.icon[data-icon=L]', {
        onclick: partial(ctrl.open, false)
      }),
      m('h2', 'Invite to the study'),
      m('div.users', ctrl.candidates().filter(function(u) {
        return memberIds.indexOf(u.toLowerCase()) === -1;
      }).map(function(username) {
        return m('span.user_link.button', {
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
        placeholder: 'Search by username'
      })
    ]);
  }
};
