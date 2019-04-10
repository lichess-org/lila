import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, titleNameToId, onInsert } from '../util';
import { prop, Prop } from 'common';
import * as dialog from './dialog';
import { StudyMemberMap } from './interfaces';

export function ctrl(send: SocketSend, members: Prop<StudyMemberMap>, setTab: () => void, redraw: () => void) {
  const open = prop(false);
  let followings = [];
  let spectators = [];
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
    members,
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

export function view(ctrl): VNode {
  const candidates = ctrl.candidates();
  return dialog.form({
    class: 'study_invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', 'Invite to the study'),
      h('p.info', { attrs: { 'data-icon': '' } }, [
        'Please only invite people you know,',
        h('br'),
        'and who actively want to join this study.'
      ]),
      candidates.length ? h('div.users', candidates.map(function(username) {
        return h('span.user-link.button', {
          key: username,
          attrs: { 'data-href': '/@/' + username },
          hook: bind('click', _ => ctrl.invite(username))
        }, username);
      })) : undefined,
      h('div.input-wrapper', [ // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: 'Search by username' },
          hook: onInsert<HTMLInputElement>(el => {
            window.lichess.userAutocomplete($(el), {
              tag: 'span',
              onSelect(v) {
                ctrl.invite(v.name);
                $(el).typeahead('close');
                el.value = '';
                ctrl.redraw();
              }
            });
          })
        })
      ])
    ]
  });
}
