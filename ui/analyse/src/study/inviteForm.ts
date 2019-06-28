import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, titleNameToId, onInsert } from '../util';
import { prop, Prop } from 'common';
import { modal } from '../modal';
import { StudyMemberMap } from './interfaces';

export function ctrl(send: SocketSend, members: Prop<StudyMemberMap>, setTab: () => void, redraw: () => void, trans: Trans) {
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
    redraw,
    trans
  };
};

export function view(ctrl): VNode {
  const candidates = ctrl.candidates();
  return modal({
    class: 'study__invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.trans.noarg('inviteToTheStudy')),
      h('p.info', { attrs: { 'data-icon': '' } }, ctrl.trans.noarg('pleaseOnlyInvitePeopleYouKnow')),
      candidates.length ? h('div.users', candidates.map(function(username) {
        return h('span.button.button-metal', {
          key: username,
          hook: bind('click', _ => ctrl.invite(username))
        }, username);
      })) : undefined,
      h('div.input-wrapper', [ // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername') },
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
