import { Prop, prop } from 'common/common';
import { modal } from 'common/modal';
import { bind, onInsert } from 'common/snabbdom';
import { storedSet } from 'common/storage';
import { VNode, h } from 'snabbdom';
import { titleNameToId } from '../util';
import { StudyMemberMap } from './interfaces';

export function makeCtrl(
  send: SocketSend,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
  redraw: () => void,
  trans: Trans
) {
  const open = prop(false),
    previouslyInvited = storedSet<string>('study.previouslyInvited', 10);
  let followings: string[] = [],
    spectators: string[] = [];
  function updateFollowings(f) {
    followings = f(followings);
    if (open()) redraw();
  }
  return {
    open,
    candidates() {
      const existing = members();
      return followings
        .concat(spectators)
        .concat(...previouslyInvited())
        .filter(function (elem, idx, arr) {
          return (
            arr.indexOf(elem) >= idx && // remove duplicates
            !existing.hasOwnProperty(titleNameToId(elem))
          ); // remove existing members
        })
        .sort();
    },
    members,
    setSpectators(usernames: string[]) {
      spectators = usernames;
    },
    setFollowings(usernames: string[]) {
      updateFollowings((_: string[]) => usernames);
    },
    delFollowing(username: string) {
      updateFollowings(function (prevs: string[]) {
        return prevs.filter(function (u: string) {
          return username !== u;
        });
      });
    },
    addFollowing(username: string) {
      updateFollowings(function (prevs: string[]) {
        return prevs.concat([username]);
      });
    },
    toggle() {
      open(!open());
      if (open()) send('following_onlines');
    },
    invite(titleName: string) {
      const userId = titleNameToId(titleName);
      send('invite', userId);
      setTimeout(() => previouslyInvited(userId), 1000);
      setTab();
    },
    previouslyInvited,
    redraw,
    trans,
  };
}

export function view(ctrl: ReturnType<typeof makeCtrl>): VNode {
  const candidates = ctrl.candidates();
  return modal({
    class: 'study__modal.study__invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.trans.noarg('inviteToTheStudy')),
      h('p.info', { attrs: { 'data-icon': '' } }, ctrl.trans.noarg('pleaseOnlyInvitePeopleYouKnow')),
      h('div.input-wrapper', [
        // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: ctrl.trans.noarg('searchByUsername') },
          hook: onInsert<HTMLInputElement>(el => {
            window.lishogi.userAutocomplete($(el), {
              tag: 'span',
              onSelect(v) {
                ctrl.invite(v.name);
                $(el).typeahead('close');
                el.value = '';
                ctrl.redraw();
              },
            });
          }),
        }),
      ]),
      candidates.length
        ? h(
            'div.users',
            candidates.map(function (username: string) {
              return h(
                'span.button.button-metal',
                {
                  key: username,
                  hook: bind('click', _ => ctrl.invite(username)),
                },
                username
              );
            })
          )
        : undefined,
    ],
  });
}
