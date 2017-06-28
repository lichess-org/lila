import { h } from 'snabbdom'
import { bind, titleNameToId } from '../util';
import { prop } from 'common';
import * as dialog from './dialog';

export function ctrl(send, members, setTab, redraw: () => void) {
  const open = prop(false);
  var followings = [];
  var spectators = [];
  function updateFollowings(f) {
    followings = f(followings);
    if (open()) redraw();
  };
  return {
    open,
    candidates() {
      const existing = members();
      return followings.concat(spectators).filter(function(elem, idx, arr) {
        return arr.indexOf(elem) >= idx && // remove duplicates
        !existing.hasOwnProperty(titleNameToId(elem)); // remove existing members
      }).sort();
    },
    members: members,
    setSpectators(usernames) {
      spectators = usernames;
    },
    setFollowings(usernames) {
      updateFollowings(_ => usernames)
    },
    delFollowing(username) {
      updateFollowings(function(prevs) {
        return prevs.filter(function(u) {
          return username !== u;
        });
      });
    },
    addFollowing(username) {
      updateFollowings(function(prevs) {
        return prevs.concat([username]);
      });
    },
    toggle() {
      open(!open());
      if (open()) send('following_onlines');
    },
    invite(titleName) {
      send("invite", titleNameToId(titleName));
      setTab();
    },
    redraw
  };
};

export function view(ctrl) {
  var candidates = ctrl.candidates();
  return dialog.form({
    class: 'study_invite',
    onClose: () => ctrl.open(false),
    content: [
      h('h2', 'Invite to the study'),
      h('p.info', {
        attrs: { 'data-icon': '' }
      }, [
        'Please only invite people you know,',
        h('br'),
        'and who actively want to join this study.'
      ]),
      candidates.length ? h('div.users', candidates.map(function(username) {
        return h('span.user_link.button', {
          attrs: { 'data-href': '/@/' + username },
          hook: bind('click', _ => ctrl.invite(username))
        }, username);
      })) : null,
      h('input', {
        attrs: { placeholder: 'Search by username' },
        hook: {
          insert: vnode => {
            const el = vnode.elm as HTMLInputElement;
            window.lichess.userAutocomplete($(el), {
              onSelect: function(v) {
                ctrl.invite(v);
                $(el).typeahead('close');
                el.value = '';
                ctrl.redraw();
              }
            });
          }
        }
      })
    ]
  });
}
