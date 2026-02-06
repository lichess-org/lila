import { h, type VNode } from 'snabbdom';
import { spinnerVdom as spinner, onInsert } from 'lib/view';
import type MsgCtrl from '../ctrl';
import renderConvo from './convo';
import renderContact from './contact';
import * as search from './search';

export default function (ctrl: MsgCtrl): VNode {
  const activeId = ctrl.data.convo?.user.id;
  return h('main.box.msg-app', { class: { [`pane-${ctrl.pane}`]: true } }, [
    h('div.msg-app__side', [
      search.renderInput(ctrl),
      ctrl.search.result
        ? search.renderResults(ctrl, ctrl.search.result)
        : h(
            'div.msg-app__contacts.msg-app__side__content',
            {
              hook: onInsert((el: HTMLElement) => {
                el.addEventListener('scroll', () => {
                  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100) {
                    ctrl.loadMoreContacts();
                  }
                });
              }),
            },
            [
              ...ctrl.data.contacts.map(t => renderContact(ctrl, t, activeId)),
              ctrl.loadingContacts ? h('div.msg-app__contacts__loading', spinner()) : null,
            ],
          ),
    ]),
    ctrl.data.convo
      ? renderConvo(ctrl, ctrl.data.convo)
      : ctrl.loading
        ? h('div.msg-app__convo', { key: ':' }, [h('div.msg-app__convo__head'), spinner()])
        : '',
  ]);
}
