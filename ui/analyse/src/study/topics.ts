import * as modal from '../modal';
import * as xhr from 'common/xhr';
import { bind, bindSubmit, onInsert } from '../util';
import { h, VNode } from 'snabbdom';
import { prop, Prop } from 'common';
import { Redraw } from '../interfaces';
import { StudyCtrl, Topic } from './interfaces';

export interface TopicsCtrl {
  open: Prop<boolean>;
  getTopics(): Topic[];
  save(data: string): void;
  trans: Trans;
  redraw: Redraw;
}

export function ctrl(save: (data: string) => void, getTopics: () => Topic[], trans: Trans, redraw: Redraw): TopicsCtrl {
  const open = prop(false);

  return {
    open,
    getTopics,
    save(data: string) {
      save(data);
      open(false);
    },
    trans,
    redraw,
  };
}

export function view(ctrl: StudyCtrl): VNode {
  return h('div.study__topics', [
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
          ['Manage topics']
        )
      : null,
  ]);
}

let tagify: any | undefined;

export function formView(ctrl: TopicsCtrl, userId?: string): VNode {
  return modal.modal({
    class: 'study-topics',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', 'Study topics'),
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
              hook: onInsert(elm => setupTagify(elm, userId)),
            },
            ctrl.getTopics().join(', ').replace(/[<>]/g, '')
          ),
          h(
            'button.button',
            {
              type: 'submit',
            },
            ctrl.trans.noarg('apply')
          ),
        ]
      ),
    ],
  });
}

function setupTagify(elm: HTMLElement, userId?: string) {
  lichess.loadCssPath('tagify');
  lichess.loadScript('vendor/tagify/tagify.min.js').then(() => {
    tagify = new window.Tagify(elm, {
      pattern: /.{2,}/,
      maxTags: 30,
    });
    let abortCtrl: AbortController; // for aborting the call
    tagify.on('input', e => {
      const term = e.detail.value.trim();
      if (term.length < 2) return;
      tagify.settings.whitelist.length = 0; // reset the whitelist
      abortCtrl && abortCtrl.abort();
      abortCtrl = new AbortController();
      // show loading animation and hide the suggestions dropdown
      tagify.loading(true).dropdown.hide.call(tagify);
      xhr
        .json(xhr.url('/study/topic/autocomplete', { term, user: userId }), { signal: abortCtrl.signal })
        .then(list => {
          tagify.settings.whitelist.splice(0, list.length, ...list); // update whitelist Array in-place
          tagify.loading(false).dropdown.show.call(tagify, term); // render the suggestions dropdown
        });
    });
    $('.tagify__input').each(function (this: HTMLInputElement) {
      this.focus();
    });
  });
}
