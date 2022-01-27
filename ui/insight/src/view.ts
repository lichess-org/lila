import { h, thunk } from 'snabbdom';
import axis from './axis';
import filters from './filters';
import presets from './presets';
import chart from './chart';
import { vert } from './table';
import help from './help';
import info from './info';
import boards from './boards';
import Ctrl from './ctrl';
import { bind } from 'common/snabbdom';

const renderMain = (ctrl: Ctrl, _cacheKey: string | boolean) => {
  if (ctrl.vm.broken)
    return h('div.broken', [
      h('i', { attrs: { 'data-icon': '' } }),
      'Insights are unavailable.',
      h('br'),
      'Please try again later.',
    ]);
  if (!ctrl.vm.answer) return h('div'); // returning undefined breaks snabbdom's thunks
  return h('div', [chart(ctrl), vert(ctrl), boards(ctrl)]);
};

// Key that determines whether or not renderMain needs to get rerendered
const cacheKey = (ctrl: Ctrl) => {
  const q = ctrl.vm.answer?.question;
  return q ? ctrl.makeUrl(q.dimension, q.metric, q.filters) : ctrl.vm.broken;
};

const tabData = (ctrl: Ctrl, panel: 'filter' | 'preset') => ({
  class: { active: ctrl.vm.panel === panel },
  attrs: { 'data-panel': panel },
  hook: bind('click', () => ctrl.setPanel(panel)),
});

export default function (ctrl: Ctrl) {
  return h(
    'main#insight',
    h(
      'div',
      {
        attrs: {
          class: ctrl.vm.loading ? 'loading' : 'ready',
        },
      },
      [
        h('div.left-side', [
          info(ctrl),
          h('div.panel-tabs', [
            h('a.tab.preset', tabData(ctrl, 'preset'), 'Presets'),
            h('a.tab.filter', tabData(ctrl, 'filter'), 'Filters'),
            Object.keys(ctrl.vm.filters).length
              ? h(
                  'a.clear',
                  {
                    attrs: {
                      title: 'Clear all filters',
                      'data-icon': '',
                    },
                    hook: bind('click', ctrl.clearFilters.bind(ctrl)),
                  },
                  'CLEAR'
                )
              : null,
          ]),
          ctrl.vm.panel === 'filter' ? filters(ctrl) : null,
          ctrl.vm.panel === 'preset' ? presets(ctrl) : null,
          help(ctrl),
        ]),
        h('header', [axis(ctrl), h('h2.text', { attrs: { 'data-icon': '' } }, 'Chess Insights')]),
        thunk('div.insight__main.box', renderMain, [ctrl, cacheKey(ctrl)]),
      ]
    )
  );
}
