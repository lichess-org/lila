import { Controller } from '../interfaces';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { bind } from '../util';

const staticThemes = new Set([
  "enPassant",
  "long",
  "mateIn1",
  "mateIn2",
  "mateIn3",
  "mateIn4",
  "mateIn5",
  "oneMove",
  "short",
  "veryLong"
]);

export default function theme(ctrl: Controller): VNode {
  return h('div.puzzle__side__theme', [
    h('a', { attrs: { href: '/training/themes' } }, h('h2', ['« ', ctrl.getData().theme.name])),
    h('p', ctrl.getData().theme.desc),
    ctrl.vm.mode != 'view' ? null : editor(ctrl)
  ]);
}

const editor = (ctrl: Controller): VNode => {
  const data = ctrl.getData(),
    user = data.user,
    themes = ctrl.vm.round?.themes || {};
  return h('div.puzzle__themes', [
    h('div.puzzle__themes_list', {
      hook: bind('click', e => {
        const target = e.target as HTMLElement;
        const theme = target.getAttribute('data-theme');
        if (theme) ctrl.voteTheme(theme, target.classList.contains('vote-up'));
      }, ctrl.redraw)
    }, data.puzzle.themes.map(key =>
      h('div.puzzle__themes__list__entry', [
        h('a', {
          attrs: {
            href: `/training/${key}`,
            title: ctrl.trans.noarg(`${key}Description`)
          }
        }, ctrl.trans.noarg(key)),
        !user || staticThemes.has(key) ? null : h('div.puzzle__themes__votes', [
          h('span.puzzle__themes__vote.vote-up', {
            class: { active: themes[key] },
            attrs: { 'data-theme': key }
          }),
          h('span.puzzle__themes__vote.vote-down', {
            class: { active: themes[key] === false },
            attrs: { 'data-theme': key }
          })
        ])
      ])
    ))
  ]);
}
