import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { User } from '../interfaces'
import MsgCtrl from '../ctrl';
import throttle from 'common/throttle';

export default function renderTextarea(ctrl: MsgCtrl, user: User): VNode {
  return h('textarea.msg-app__convo__reply__text', {
    attrs: {
      rows: 1,
      autofocus: 1
    },
    hook: {
      insert(vnode) {
        setupTextarea(vnode.elm as HTMLTextAreaElement, user.id, ctrl.post);
      }
    }
  });
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
        if (txt.length > 8000) return alert("The message is too long.");
        if (txt) post(txt);
        area.value = '';
        area.dispatchEvent(new Event('input')); // resize the textarea
        storage.remove();
      });
    }
  });
  area.focus();
}
