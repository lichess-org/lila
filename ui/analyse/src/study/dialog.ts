import { h } from 'snabbdom'
import { MaybeVNodes } from '../interfaces';
import { bind } from '../util';

interface Dialog {
  class?: string;
  content: MaybeVNodes;
  onClose(): void;
}

export function form(d: Dialog) {
  return h('div.lichess_overboard.study_overboard.' + d.class, {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/material.form.css')
    }
  }, ([
    h('a.close.icon', {
      attrs: { 'data-icon': 'L' },
      hook: bind('click', d.onClose)
    })
  ] as MaybeVNodes).concat(d.content));
}

export function button(name: string) {
  return h('div.button-container',
    h('button.submit.button', {
      attrs: { type: 'submit' },
    }, name)
  );
}
