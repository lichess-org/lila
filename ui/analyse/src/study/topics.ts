import { h, type VNode } from 'snabbdom';

import { bind, bindSubmit, onInsert, snabDialog } from 'lib/view';
import { json as xhrJson, url as xhrUrl } from 'lib/xhr';

import type { Topic } from './interfaces';
import type StudyCtrl from './studyCtrl';

export default class TopicsCtrl {
  open = (userId?: string) => formView(this, userId);

  constructor(
    readonly save: (data: string[]) => void,
    readonly getTopics: () => Topic[],
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
          { hook: bind('click', () => ctrl.topics.open(ctrl.members.opts.myId)) },
          i18n.study.manageTopics,
        )
      : null,
  ]);

let tagify: Tagify | undefined;

const formView = (ctrl: TopicsCtrl, userId?: string) =>
  snabDialog({
    closeButton: true,
    class: 'study-topics study-modal',
    show: true,
    modal: true,
    render: (_, dlg) => [
      h('h2', i18n.study.topics),
      h(
        'form',
        {
          hook: bindSubmit(_ => {
            const tags = tagify?.value;
            if (tags) {
              ctrl.save(tags.map(t => t.value));
              dlg.close();
            }
          }),
        },
        [
          h(
            'textarea',
            { hook: onInsert(elm => setupTagify(elm as HTMLTextAreaElement, userId)) },
            ctrl.getTopics().join(', ').replace(/[<>]/g, ''),
          ),
          h('button.button', { type: 'submit' }, i18n.site.save),
        ],
      ),
    ],
  });

function setupTagify(elm: HTMLInputElement | HTMLTextAreaElement, userId?: string) {
  site.asset.loadCss({ key: 'bits.tagify' });
  site.asset.loadIife('npm/tagify.min.js').then(() => {
    const tagi = (tagify = new window.Tagify(elm, { pattern: /.{2,}/, maxTags: 30 }));
    let abortCtrl: AbortController | undefined; // for aborting the call
    tagi.on('input', e => {
      const term = (e.detail as Tagify.TagData).value.trim();
      if (term.length < 2) return;
      tagi.settings.whitelist.length = 0; // reset the whitelist
      abortCtrl?.abort('nevermind ' + term);
      abortCtrl = new AbortController();
      // show loading animation and hide the suggestions dropdown
      tagi.loading(true).dropdown.hide.call(tagi);
      xhrJson(xhrUrl('/study/topic/autocomplete', { term, user: userId }), { signal: abortCtrl.signal }).then(
        list => {
          tagi.settings.whitelist.splice(0, list.length, ...list); // update whitelist Array in-place
          tagi.loading(false).dropdown.show.call(tagi, term); // render the suggestions dropdown
        },
      );
    });
    $('.tagify__input').each(function (this: HTMLInputElement) {
      this.focus();
    });
  });
}
