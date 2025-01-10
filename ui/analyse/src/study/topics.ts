import { loadCssPath, loadVendorScript } from 'common/assets';
import { type Prop, prop } from 'common/common';
import * as modal from 'common/modal';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type { Redraw } from '../interfaces';
import type { StudyCtrl, Topic } from './interfaces';

export interface TopicsCtrl {
  open: Prop<boolean>;
  getTopics(): Topic[];
  save(data: string): void;
  redraw: Redraw;
}

export function ctrl(
  save: (data: string) => void,
  getTopics: () => Topic[],
  redraw: Redraw,
): TopicsCtrl {
  const open = prop(false);

  return {
    open,
    getTopics,
    save(data: string) {
      save(data);
      open(false);
    },
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
        topic,
      ),
    ),
    ctrl.members.canContribute()
      ? h(
          'a.manage',
          {
            hook: bind('click', () => ctrl.topics.open(true), ctrl.redraw),
          },
          i18n('study:manageTopics'),
        )
      : null,
  ]);
}

let tagify: any | undefined;

export function formView(ctrl: TopicsCtrl, userId?: string): VNode {
  return modal.modal({
    class: 'study__modal.study-topics',
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
              hook: onInsert(elm => setupTagify(elm as HTMLTextAreaElement, userId)),
            },
            ctrl.getTopics().join(', ').replace(/[<>]/g, ''),
          ),
          h(
            'button.button',
            {
              type: 'submit',
            },
            i18n('apply'),
          ),
        ],
      ),
    ],
  });
}

function setupTagify(elm: HTMLTextAreaElement, userId?: string) {
  loadCssPath('misc.tagify');
  loadVendorScript('tagify', 'tagify/tagify.min.js').then(() => {
    const tagi = (tagify = new window.Tagify(elm, { pattern: /.{2,}/, maxTags: 30 }));
    let abortCtrl: AbortController | undefined; // for aborting the call
    tagi.on('input', e => {
      const term = (e.detail as Tagify.TagData).value.trim();
      if (term.length < 2) return;
      tagi.settings.whitelist.length = 0; // reset the whitelist
      abortCtrl?.abort();
      abortCtrl = new AbortController();
      // show loading animation and hide the suggestions dropdown
      tagi.loading(true).dropdown.hide.call(tagi);
      window.lishogi.xhr
        .json(
          'GET',
          '/study/topic/autocomplete',
          { url: { term, user: userId } },
          {
            signal: abortCtrl.signal,
          },
        )
        .then(list => {
          tagi.settings.whitelist.splice(0, list.length, ...list); // update whitelist Array in-place
          tagi.loading(false).dropdown.show.call(tagi, term); // render the suggestions dropdown
        });
    });
    $('.tagify__input').each(function (this: HTMLInputElement) {
      this.focus();
    });
  });
}
