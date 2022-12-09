import type Tagify from '@yaireo/tagify';
import { prop, Prop } from 'common';
import { snabModal } from 'common/modal';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { h, VNode } from 'snabbdom';
import { Redraw } from '../interfaces';
import { StudyCtrl, Topic } from './interfaces';

export interface TopicsCtrl {
  open: Prop<boolean>;
  getTopics(): Topic[];
  save(data: string[]): void;
  trans: Trans;
  redraw: Redraw;
}

export function ctrl(
  save: (data: string[]) => void,
  getTopics: () => Topic[],
  trans: Trans,
  redraw: Redraw
): TopicsCtrl {
  const open = prop(false);

  return {
    open,
    getTopics,
    save(data: string[]) {
      save(data);
      open(false);
    },
    trans,
    redraw,
  };
}

export const view = (ctrl: StudyCtrl): VNode =>
  h('div.study__topics', [
    ...ctrl.topics.getTopics().map(topic =>
      h(
        'a.topic',
        {
          attrs: { href: `/study/topic/${encodeURIComponent(topic)}/hot` },
        },
        topic
      )
    ),
    ctrl.members.canContribute()
      ? h(
          'a.manage',
          {
            hook: bind('click', () => ctrl.topics.open(true), ctrl.redraw),
          },
          ctrl.trans.noarg('manageTopics')
        )
      : null,
  ]);

let tagify: Tagify | undefined;

export const formView = (ctrl: TopicsCtrl, userId?: string): VNode =>
  snabModal({
    class: 'study-topics',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', ctrl.trans.noarg('topics')),
      h(
        'form',
        {
          hook: bindSubmit(_ => {
            const tags = tagify?.value;
            tags && ctrl.save(tags.map(t => t.value));
          }, ctrl.redraw),
        },
        [
          h(
            'textarea',
            {
              hook: onInsert(elm => setupTagify(elm as HTMLTextAreaElement, userId)),
            },
            ctrl.getTopics().join(', ').replace(/[<>]/g, '')
          ),
          h(
            'button.button',
            {
              type: 'submit',
            },
            ctrl.trans.noarg('save')
          ),
        ]
      ),
    ],
  });

function setupTagify(elm: HTMLInputElement | HTMLTextAreaElement, userId?: string) {
  lichess.loadCssPath('tagify');
  lichess.loadScript('vendor/tagify/tagify.min.js').then(() => {
    const tagi = (tagify = new (window.Tagify as typeof Tagify)(elm, {
      pattern: /.{2,}/,
      maxTags: 30,
    }));
    let abortCtrl: AbortController | undefined; // for aborting the call
    tagi.on('input', e => {
      const term = e.detail.value.trim();
      if (term.length < 2) return;
      tagi.settings.whitelist!.length = 0; // reset the whitelist
      abortCtrl && abortCtrl.abort();
      abortCtrl = new AbortController();
      // show loading animation and hide the suggestions dropdown
      tagi.loading(true).dropdown.hide.call(tagi);
      xhr
        .json(xhr.url('/study/topic/autocomplete', { term, user: userId }), { signal: abortCtrl.signal })
        .then(list => {
          tagi.settings.whitelist!.splice(0, list.length, ...list); // update whitelist Array in-place
          tagi.loading(false).dropdown.show.call(tagi, term); // render the suggestions dropdown
        });
    });
    $('.tagify__input').each(function (this: HTMLInputElement) {
      this.focus();
    });
  });
}
