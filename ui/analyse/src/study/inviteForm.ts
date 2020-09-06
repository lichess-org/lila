import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, titleNameToId, onInsert } from '../util';
import { prop, Prop } from 'common';
import { modal } from '../modal';
import { StudyMemberMap } from './interfaces';

export function ctrl(send: SocketSend, members: Prop<StudyMemberMap>, setTab: () => void, redraw: () => void, trans: Trans) {
  const open = prop(false);
  let followings: string[] = [];
  let spectators: string[] = [];
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
    setSpectators(usernames: string[]) {
      spectators = usernames;
    },
    setFollowings(usernames: string[]) {
      updateFollowings((_: string[])  => usernames)
    },
    delFollowing(username: string) {
      updateFollowings(function(prevs: string[]) {
        return prevs.filter(function(u: string) {
          return username !== u;
        });
      });
    },
    addFollowing(username: string) {
      updateFollowings(function(prevs: string[]) {
        return prevs.concat([username]);
      });
    },
    toggle() {
      open(!open());
      if (open()) send('following_onlines');
    },
    invite(titleName: string) {
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
      h('div.input-wrapper', [ // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername') },
          hook: onInsert<HTMLInputElement>(el => {
            window.lichess.userAutocomplete($(el), {
              tag: 'span',
              onSelect(v: any) {
                ctrl.invite(v.name || v);
                $(el).typeahead('close');
                el.value = '';
                ctrl.redraw();
              }
            });
          })
        })
      ]),
      candidates.length ? h('div.users', candidates.map(function(username: string) {
        return h('span.button.button-metal', {
          key: username,
          hook: bind('click', _ => ctrl.invite(username))
        }, username);
      })) : undefined
    ]
  });
}
