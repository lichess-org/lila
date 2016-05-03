var m = require('mithril');
var partial = require('chessground').util.partial;
var objectValues = require('../util').objectValues;
var dialog = require('./dialog');

module.exports = {
  ctrl: function(send, members, setTab) {
    var open = m.prop(false);
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
    var candidates = ctrl.candidates().filter(function(u) {
      return !ctrl.members().hasOwnProperty(u.toLowerCase());
    });
    return dialog.form({
      class: 'study_invite',
      onClose: partial(ctrl.open, false),
      content: [
        m('h2', 'Invite to the study'),
        candidates.length ? m('div.users', candidates.map(function(username) {
          return m('span.user_link.button', {
            'data-href': '/@/' + username,
            onclick: partial(ctrl.invite, username)
          }, username);
        })) : null,
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
      ]
    });
  }
};
