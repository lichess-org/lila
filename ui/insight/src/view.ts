import { h, thunk } from 'snabbdom';
import debounce from 'common/debounce';
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

let forceRenderMain = false;

const renderMain = (ctrl: Ctrl, _cacheKey: string | boolean) => {
  console.log(`${_cacheKey} ${Date.now()}`);
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
  if (forceRenderMain) {
    forceRenderMain = false;
    return Date.now().toString();
  }
  const q = ctrl.vm.answer?.question;
  return q ? ctrl.makeUrl(q.dimension, q.metric, q.filters) : ctrl.vm.broken;
};

const panelTabData = (ctrl: Ctrl, panel: 'filter' | 'preset') => ({
  class: { active: ctrl.vm.panel === panel },
  attrs: { 'data-panel': panel },
  hook: bind('click', () => ctrl.setPanel(panel)),
});

const viewTabData = (ctrl: Ctrl, view: 'questions' | 'filters' | 'answers' | 'combined') => ({
  class: { active: ctrl.vm.view === view },
  //attrs: { 'data-view': view },
  hook: bind('click', () => {
    ctrl.setView(view);
  }),
});

export default function (ctrl: Ctrl) {
  window.onresize = debounce(() => {
    forceRenderMain = true;
    ctrl.redraw();
  }, 200);
  if (ctrl.isWide()) {
    ctrl.vm.view = 'combined';
    return wideView(ctrl);
  } else if (ctrl.vm.view === 'combined') ctrl.vm.view = 'questions';
  return narrowView(ctrl);
}

function wideView(ctrl: Ctrl) {
  return h('main#insight', [
    h('div', { attrs: { class: ctrl.vm.loading ? 'loading' : 'ready' } }, [
      h('div.left-side', [
        info(ctrl),
        h('div.panel-tabs', [
          h('a.tab.preset', panelTabData(ctrl, 'preset'), 'Presets'),
          h('a.tab.filter', panelTabData(ctrl, 'filter'), 'Filters'),
          clearBtn(ctrl, false),
        ]),
        ctrl.vm.panel === 'filter' ? filters(ctrl) : null,
        ctrl.vm.panel === 'preset' ? presets(ctrl) : null,
        help(ctrl),
      ]),
      h('header', [axis(ctrl), h('h2.text', { attrs: { 'data-icon': '' } }, 'Chess Insights')]),
      thunk('div.insight__main.box', renderMain, [ctrl, cacheKey(ctrl)]),
    ]),
  ]);
}

function narrowView(ctrl: Ctrl) {
  return h('main#insight', [
    h('div.view-tabs', [
      h('div.tab', viewTabData(ctrl, 'questions'), 'Questions'),
      h('div.tab', viewTabData(ctrl, 'filters'), 'Filters'),
      h('div.tab', viewTabData(ctrl, 'answers'), 'Answers'),
    ]),
    h(
      'div',
      { attrs: { class: ctrl.vm.loading ? 'loading' : 'ready' } },
      ctrl.vm.view === 'answers'
        ? [h('header', axis(ctrl)), thunk('div.insight__main.box', renderMain, [ctrl, cacheKey(ctrl)])]
        : h('div.left-side', [
            info(ctrl),
            ctrl.vm.view === 'filters' ? clearBtn(ctrl, true) : null,
            ctrl.vm.view === 'questions' ? presets(ctrl) : filters(ctrl),
          ])
    ),
  ]);
}

function clearBtn(ctrl: Ctrl, narrowView: boolean) {
  const btn = () =>
    h(
      'a.clear',
      { attrs: { title: 'Clear all filters', 'data-icon': '' }, hook: bind('click', ctrl.clearFilters.bind(ctrl)) },
      narrowView ? 'CLEAR FILTERS' : 'CLEAR'
    );
  return narrowView ? h('div.mobile-clear-container', btn()) : btn();
}
