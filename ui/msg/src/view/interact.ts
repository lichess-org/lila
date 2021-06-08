import { h, VNode } from 'snabbdom';
import { User } from '../interfaces';
import MsgCtrl from '../ctrl';
import { bind } from './util';
import throttle from 'common/throttle';

export default function renderInteract(ctrl: MsgCtrl, user: User): VNode {
  const connected = ctrl.connected();
  return h(
    'form.msg-app__convo__post',
    {
      hook: bind('submit', e => {
        e.preventDefault();
        const area = (e.target as HTMLElement).querySelector('textarea');
        if (area) {
          area.dispatchEvent(new Event('send'));
          area.focus();
        }
      }),
    },
    [
      renderTextarea(ctrl, user),
      h('button.msg-app__convo__post__submit.button', {
        class: { connected },
        attrs: {
          type: 'submit',
          'data-icon': '',
          disabled: !connected,
        },
      }),
    ]
  );
}

function renderTextarea(ctrl: MsgCtrl, user: User): VNode {
  return h('textarea.msg-app__convo__post__text', {
    attrs: {
      rows: 1,
    },
    hook: {
      insert(vnode) {
        setupTextarea(vnode.elm as HTMLTextAreaElement, user.id, ctrl);
      },
    },
  });
}

function setupTextarea(area: HTMLTextAreaElement, contact: string, ctrl: MsgCtrl) {
  const storage = ctrl.textStore!;

  let prev = 0;

  function send() {
    const now = Date.now();
    if (prev > now - 1000 || !ctrl.connected()) return;
    prev = now;
    const txt = area.value.trim();
    if (txt.length > 8000) return alert('The message is too long.');
    if (txt) ctrl.post(txt);
    area.value = '';
    area.dispatchEvent(new Event('input')); // resize the textarea
    storage.remove();
  }

  // hack to automatically resize the textarea based on content
  area.value = '';
  const baseScrollHeight = area.scrollHeight;
  area.addEventListener(
    'input',
    throttle(500, () => {
      const text = area.value.trim();
      area.rows = 1;
      // the resize magic
      if (text) area.rows = Math.min(10, 1 + Math.ceil((area.scrollHeight - baseScrollHeight) / 19));
      // and save content
      storage.set(text);
      ctrl.sendTyping(contact);
    })
  );

  // restore previously saved content
  area.value = storage.get() || '';
  if (area.value) area.dispatchEvent(new Event('input'));

  // send the content on <enter.
  area.addEventListener('keypress', (e: KeyboardEvent) => {
    if ((e.which == 10 || e.which == 13) && !e.shiftKey) {
      e.preventDefault();
      setTimeout(send);
    }
  });
  area.addEventListener('send', send);

  if (!('ontouchstart' in window)) area.focus();
}
