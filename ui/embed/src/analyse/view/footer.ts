import { h } from 'snabbdom';
import { VNode } from 'snabbdom';
import { AnalyseCtrl, StudyData } from '../ctrl';
import { i18n, i18nFormat } from 'i18n';

export function renderFooter(ctrl: AnalyseCtrl): VNode {
  return ctrl.study ? renderStudyFooter(ctrl.study) : renderGameFooter(ctrl);
}

export function renderGameFooter(ctrl: AnalyseCtrl): VNode {
  const url = `//${window.location.host}/${ctrl.data.game.id}/${ctrl.data.orientation || 'sente'}`,
    title = i18nFormat('fromGameLink', ctrl.data.game.id);

  return h('div.analyse__footer', [
    h('div.left', [
      h('a', { attrs: { target: '_blank', href: url } }, h('h1', { attrs: { title } }, title)),
    ]),
  ]);
}

function renderStudyFooter(study: StudyData) {
  const url = `//${window.location.host}/study/${study.id}/${study.chapter.id}`;

  return h('div.analyse__footer', [
    h('div.left', [
      h('a', { attrs: { target: '_blank', href: url } }, [h('h1', study.name)]),
      h(
        'select',
        {
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              el.addEventListener('change', _ => {
                location.href = `//${window.location.host}/study/embed/${study.id}/${el.value}`;
              });
            },
          },
        },
        study.chapters.map(c =>
          h(
            'option',
            {
              props: { value: c.id, selected: c.id === study.chapter.id },
            },
            c.name,
          ),
        ),
      ),
    ]),
    h('a', {
      attrs: {
        target: '_blank',
        class: 'open',
        'data-icon': '=',
        href: url,
        title: i18n('study:open'),
      },
    }),
  ]);
}
