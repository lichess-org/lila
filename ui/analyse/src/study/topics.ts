import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import * as modal from '../modal';
import { bind, bindSubmit, onInsert } from '../util';
import { prop, Prop } from 'common';
import { StudyCtrl, Topic } from './interfaces';
import { Redraw } from '../interfaces';

export interface TopicsCtrl {
  open: Prop<boolean>;
  getTopics(): Topic[];
  save(data: string): void;
  trans: Trans;
  redraw: Redraw;
}

export function ctrl(save: (data: string) => void, getTopics: () => Topic[], trans: Trans, redraw: Redraw): TopicsCtrl {

  const open = prop(true);

  return {
    open,
    getTopics,
    save(data: string) {
      save(data);
      open(false);
    },
    trans,
    redraw
  };
}

export function view(ctrl: StudyCtrl): VNode {
  return h('div.study-topics', [
    ...ctrl.topics.getTopics().map(topic =>
    h('a.topic', topic)
  ),
    ctrl.members.canContribute() ? h('a.manage', {
      hook: bind('click', () => ctrl.topics.open(true), ctrl.redraw)
    }, [ 'Manage topics' ]) : null
  ]);
}

let tagify: any | undefined;

export function formView(ctrl: TopicsCtrl): VNode {
  return modal.modal({
    class: 'study-topics',
    onClose() {
      ctrl.open(false);
      ctrl.redraw();
    },
    content: [
      h('h2', 'Study topics'),
      h('form', {
        hook: bindSubmit(_ => {
          const tags = tagify?.value;
          tags && ctrl.save(tags.map(t => t.value));
        }, ctrl.redraw)
      }, [
        h('textarea', {
          hook: onInsert(elm => {
            window.lichess.loadCssPath('tagify');
            window.lichess.loadScript('vendor/tagify/tagify.min.js').then(() => {
              tagify = new window.Tagify(elm);
            })
          })
        }, ctrl.getTopics().join(' ')),
        h('button.button', {
          type: 'submit'
        }, ctrl.trans.noarg('apply'))
      ])
    ]
  });
}
