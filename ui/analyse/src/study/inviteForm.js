var m = require('mithril');
var partial = require('chessground').util.partial;
var objectValues = require('../util').objectValues;
var dialog = require('./dialog');

function titleNameToId(titleName) {
  var split = titleName.split(' ');
  var name = split.length == 1 ? split[0] : split[1];
  return name.toLowerCase();
}

module.exports = {
  ctrl: function(send, members, setTab) {
    var open = m.prop(false);
    var followings = [];
    var spectators = [];
    var invite = function(titleName) {
      send("invite", titleNameToId(titleName));
      setTab();
    };
    var updateFollowings = function(f) {
      followings = f(followings);
      if (open()) m.redraw();
    };
    return {
      open: open,
      candidates: function() {
        var existing = members();
        return followings.concat(spectators).filter(function(elem, idx, arr) {
          return arr.indexOf(elem) >= idx && // remove duplicates
            !existing.hasOwnProperty(titleNameToId(elem)); // remove existing members
        }).sort();
      },
      members: members,
      setSpectators: function(usernames) {
        spectators = usernames;
        if (open()) m.redraw();
      },
      setFollowings: function(usernames) {
        updateFollowings(function(prevs) {
          return usernames;
        });
      },
      delFollowing: function(username) {
        updateFollowings(function(prevs) {
          return prevs.filter(function(u) {
            return username !== u;
          });
        });
      },
      addFollowing: function(username) {
        updateFollowings(function(prevs) {
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
    var candidates = ctrl.candidates();
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
