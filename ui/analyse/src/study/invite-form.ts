import { type Prop, prop } from 'common/common';
import { modal } from 'common/modal';
import { bind, onInsert } from 'common/snabbdom';
import { storedSet } from 'common/storage';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import { titleNameToId } from '../util';
import type { StudyInviteFormCtrl, StudyMemberMap } from './interfaces';

export function makeCtrl(
  send: Socket.Send,
  members: Prop<StudyMemberMap>,
  setTab: () => void,
  redraw: () => void,
): StudyInviteFormCtrl {
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
        .filter((elem, idx, arr) => {
          return (
            arr.indexOf(elem) >= idx && // remove duplicates
            !Object.prototype.hasOwnProperty.call(existing, titleNameToId(elem))
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
      updateFollowings((prevs: string[]) => prevs.filter((u: string) => username !== u));
    },
    addFollowing(username: string) {
      updateFollowings((prevs: string[]) => prevs.concat([username]));
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
  };
}

export function view(ctrl: StudyInviteFormCtrl): VNode {
  const candidates = ctrl.candidates();
  return modal({
    class: 'study__modal.study__invite',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', i18n('study:inviteToTheStudy')),
      h('p.info', { attrs: { 'data-icon': '' } }, i18n('study:pleaseOnlyInvitePeopleYouKnow')),
      h('div.input-wrapper', [
        // because typeahead messes up with snabbdom
        h('input', {
          attrs: { placeholder: i18n('study:searchByUsername') },
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
            candidates.map((username: string) =>
              h(
                'span.button.button-metal',
                {
                  key: username,
                  hook: bind('click', _ => ctrl.invite(username)),
                },
                username,
              ),
            ),
          )
        : undefined,
    ],
  });
}
