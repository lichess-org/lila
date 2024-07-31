import { prop } from 'common';
import { bind, bindSubmit, onInsert } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { snabDialog } from 'common/dialog';
import { h, VNode } from 'snabbdom';
import { Redraw } from '../interfaces';
import { Topic } from './interfaces';
import StudyCtrl from './studyCtrl';

export default class TopicsCtrl {
  open = prop(false);

  constructor(
    readonly save: (data: string[]) => void,
    readonly getTopics: () => Topic[],
    readonly trans: Trans,
    readonly redraw: Redraw,
  ) {}
}

export const view = (ctrl: StudyCtrl): VNode =>
  h('div.study__topics', [
    ...ctrl.topics
      .getTopics()
      .map(topic =>
        h('a.topic', { attrs: { href: `/study/topic/${encodeURIComponent(topic)}/hot` } }, topic),
      ),
    ctrl.members.canContribute()
      ? h(
          'a.manage',
          { hook: bind('click', () => ctrl.topics.open(true), ctrl.redraw) },
          ctrl.trans.noarg('manageTopics'),
        )
      : null,
  ]);

let tagify: Tagify | undefined;

export const formView = (ctrl: TopicsCtrl, userId?: string): VNode =>
  snabDialog({
    class: 'study-topics',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    vnodes: [
      h('h2', ctrl.trans.noarg('topics')),
      h(
        'form',
        {
          hook: bindSubmit(_ => {
            const tags = tagify?.value;
            if (tags) {
              ctrl.save(tags.map(t => t.value));
              ctrl.open(false);
            }
          }, ctrl.redraw),
        },
        [
          h(
            'textarea',
            { hook: onInsert(elm => setupTagify(elm as HTMLTextAreaElement, userId)) },
            ctrl.getTopics().join(', ').replace(/[<>]/g, ''),
          ),
          h('button.button', { type: 'submit' }, ctrl.trans.noarg('save')),
        ],
      ),
    ],
    onInsert: dlg => {
      dlg.show();
      (dlg.view.querySelector('.tagify__input') as HTMLElement)?.focus();
    },
  });

function setupTagify(elm: HTMLInputElement | HTMLTextAreaElement, userId?: string) {
  site.asset.loadCssPath('bits.tagify');
  site.asset.loadIife('npm/tagify.min.js').then(() => {
    const tagi = (tagify = new window.Tagify(elm, { pattern: /.{2,}/, maxTags: 30 }));
    let abortCtrl: AbortController | undefined; // for aborting the call
    tagi.on('input', e => {
      const term = (e.detail as Tagify.TagData).value.trim();
      if (term.length < 2) return;
      tagi.settings.whitelist.length = 0; // reset the whitelist
      abortCtrl && abortCtrl.abort();
      abortCtrl = new AbortController();
      // show loading animation and hide the suggestions dropdown
      tagi.loading(true).dropdown.hide.call(tagi);
      xhr
        .json(xhr.url('/study/topic/autocomplete', { term, user: userId }), { signal: abortCtrl.signal })
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
