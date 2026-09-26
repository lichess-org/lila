import { debounce } from '@/async';
import { blurOnEscape } from '@/common';
import { type VNode, onInsert, div, textarea } from '@/view';

import type { NoteCtrl, NoteOpts } from './interfaces';
import * as xhr from './xhr';

export function noteCtrl(opts: NoteOpts): NoteCtrl {
  let text: string | undefined = opts.text;
  const doPost = debounce(() => {
    xhr.setNote(opts.id, text || '');
  }, 1000);
  return {
    id: opts.id,
    text: () => text,
    fetch() {
      xhr.getNote(opts.id).then(t => {
        text = t || '';
        opts.redraw();
      });
    },
    post(t) {
      text = t;
      doPost();
    },
  };
}

export function noteView(ctrl: NoteCtrl, autofocus: boolean): VNode {
  const text = ctrl.text();
  if (text === undefined) return div('.loading', { hook: { insert: ctrl.fetch } });
  return textarea()('.mchat__note', {
    attrs: { placeholder: i18n.site.typePrivateNotesHere, spellcheck: 'false' },
    hook: onInsert<HTMLTextAreaElement>(el => {
      el.value = text;
      if (autofocus) el.focus();
      blurOnEscape(el);
      $(el).on('change keyup paste', () => ctrl.post(el.value));
    }),
  });
}
