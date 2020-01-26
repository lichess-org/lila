import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Convo } from '../interfaces'
import { userName } from './util';
import renderMsgs from './msgs';
import throttle from 'common/throttle';
import MsgCtrl from '../ctrl';

export default function renderConvo(ctrl: MsgCtrl, convo: Convo): VNode {
  const user = convo.thread.contact;
  return h('div.msg-app__convo', {
    key: `${user.id}:${convo.msgs[0] && convo.msgs[0].date.getDate()}`,
  }, [
    h('div.msg-app__convo__head', [
      h('a.user-link.ulpt', {
        attrs: { href: `/@/${user.name}` },
        class: {
          online: user.online,
          offline: !user.online
        }
      }, [
        h('i.line' + (user.patron ? '.patron' : '')),
        ...userName(user)
      ]),
      h('div.msg-app__convo__head__actions', renderActions(ctrl, convo))
    ]),
    renderMsgs(ctrl, convo.msgs),
    h('div.msg-app__convo__reply', [
      h('textarea.msg-app__convo__reply__text', {
        attrs: {
          rows: 1,
          autofocus: 1
        },
        hook: {
          insert(vnode) {
            setupTextarea(vnode.elm as HTMLTextAreaElement, user.id, ctrl.post);
          }
        }
      })
    ])
  ]);
}

function renderActions(ctrl: MsgCtrl, convo: Convo): VNode[] {
  const user = convo.thread.contact, nodes = [];
  if (convo.relations.out) nodes.push(
    h('button.msg-app__convo__action.text.hover-text', {
      attrs: {
        'data-icon': 'h',
        href: `/rel/unfollow/${user.id}`,
        title: ctrl.trans.noarg('following'),
        'data-hover-text': ctrl.trans.noarg('unfollow')
      }
    })
  );
  else if (convo.relations.out === false) nodes.push(
    h('button.msg-app__convo__action.text.hover-text', {
      attrs: {
        'data-icon': 'k',
        href: `/rel/unblock/${user.id}`,
        title: ctrl.trans.noarg('blocked'),
        'data-hover-text': ctrl.trans.noarg('unblock')
      }
    })
  );
  else {
    nodes.push(
      h('a.msg-app__convo__action', {
        attrs: {
          'data-icon': 'h',
          href: `/rel/follow/${user.id}`,
          title: ctrl.trans.noarg('follow')
        }
      })
    );
    nodes.push(
      h('a.msg-app__convo__action', {
        attrs: {
          'data-icon': 'k',
          href: `/rel/block/${user.id}`,
          title: ctrl.trans.noarg('block')
        }
      })
    );
  }
  nodes.push(
    h('a.msg-app__convo__action', {
      attrs: {
        'data-icon': 'i',
        href: '/report/flag',
        title: ctrl.trans.noarg('report')
      }
    })
  );
  return nodes;
}

function setupTextarea(area: HTMLTextAreaElement, contact: string, post: (text: string) => void) {

  // save the textarea content until sent
  const storage = window.lichess.storage.make(`msg:area:${contact}`);

  // hack to automatically resize the textarea based on content
  area.value = '';
  let baseScrollHeight = area.scrollHeight;
  area.addEventListener('input', throttle(500, () =>
    setTimeout(() => {
      const text = area.value.trim();
      area.rows = 1;
      // the resize magic
      if (text) area.rows = Math.min(10, 1 + Math.ceil((area.scrollHeight - baseScrollHeight) / 19));
      // and save content
      storage.set(text);
    })
  ));

  // restore previously saved content
  area.value = storage.get() || '';
  if (area.value) area.dispatchEvent(new Event('input'));

  // send the content on <enter.
  area.addEventListener('keypress', (e: KeyboardEvent) => {
    if ((e.which == 10 || e.which == 13) && !e.shiftKey) {
      setTimeout(() => {
        const txt = area.value.trim();
        if (txt) post(txt);
        area.value = '';
        area.dispatchEvent(new Event('input')); // resize the textarea
        storage.remove();
      });
    }
  });
  area.focus();
}
