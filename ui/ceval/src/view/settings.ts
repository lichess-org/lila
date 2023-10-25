import { h, VNode } from 'snabbdom';
import { ParentCtrl } from '../types';
import { toggle, ToggleSettings, rangeConfig } from 'common/controls';
import { hasFeature } from 'common/device';
import { onInsert, bind } from 'common/snabbdom';
import { onClickAway } from 'common';

const ctrlToggle = (t: ToggleSettings, ctrl: ParentCtrl) =>
  toggle(t, ctrl.trans, ctrl.getCeval().opts.redraw);

const formatHashSize = (v: number): string => (v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB');

export function renderCevalSettings(ctrl: ParentCtrl): VNode | null {
  const ceval = ctrl.getCeval(),
    noarg = ctrl.trans.noarg,
    engCtrl = ctrl.getCeval().engines;

  return ceval.showEnginePrefs()
    ? h(
        'div#ceval-settings-anchor',
        h(
          'div#ceval-settings',
          { hook: onInsert(onClickAway(() => (ceval.showEnginePrefs(false), ceval.opts.redraw()))) },
          [
            (id => {
              const max = 5;
              return h('div.setting', [
                h('label', { attrs: { for: id } }, noarg('multipleLines')),
                h('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 0,
                    max,
                    step: 1,
                  },
                  hook: rangeConfig(() => ceval!.multiPv(), ctrl.cevalSetMultiPv ?? (() => {})),
                }),
                h('div.range_value', ceval.multiPv() + ' / ' + max),
              ]);
            })('analyse-multipv'),
            hasFeature('sharedMem')
              ? (id => {
                  return h('div.setting', [
                    h('label', { attrs: { for: id } }, noarg('cpus')),
                    h('input#' + id, {
                      attrs: {
                        type: 'range',
                        min: 1,
                        max: ceval.maxThreads(),
                        step: 1,
                        disabled: ceval.maxThreads() <= 1,
                      },
                      hook: rangeConfig(
                        () => ceval.threads(),
                        x => (ceval.setThreads(x), ctrl.cevalReset?.()),
                      ),
                    }),
                    h('div.range_value', `${ceval.threads ? ceval.threads() : 1} / ${ceval.maxThreads()}`),
                  ]);
                })('analyse-threads')
              : null,
            (id =>
              h('div.setting', [
                h('label', { attrs: { for: id } }, noarg('memory')),
                h('input#' + id, {
                  attrs: {
                    type: 'range',
                    min: 4,
                    max: Math.floor(Math.log2(engCtrl.active?.maxHash ?? 4)),
                    step: 1,
                    disabled: ceval.maxHash() <= 16,
                  },
                  hook: rangeConfig(
                    () => Math.floor(Math.log2(ceval.hashSize())),
                    v => (ceval.setHashSize(Math.pow(2, v)), ctrl.cevalReset?.()),
                  ),
                }),
                h('div.range_value', formatHashSize(ceval.hashSize())),
              ]))('analyse-memory'),
            ctrlToggle(
              {
                name: 'infiniteAnalysis',
                title: 'removesTheDepthLimit',
                id: 'infinite',
                checked: ceval.infinite(),
                change: x => (ceval.infinite(x), ctrl.cevalReset?.()),
              },
              ctrl,
            ),
            ...engineSelection(ctrl),
          ],
        ),
      )
    : null;
}

function engineSelection(ctrl: ParentCtrl) {
  const ceval = ctrl.getCeval(),
    active = ceval.engines.active,
    engines = ceval.engines.supporting(ceval.opts.variant.key);
  if (!engines?.length || !ceval.possible || !ceval.allowed()) return [];
  return [
    h('hr'),
    h('div.setting', [
      'Using:',
      h(
        'select.select-engine',
        {
          hook: bind('change', e => ctrl.getCeval().selectEngine((e.target as HTMLSelectElement).value)),
        },
        [
          ...engines.map(engine =>
            h(
              'option',
              {
                attrs: {
                  value: engine.id,
                  selected: active?.id == engine.id,
                },
              },
              engine.name,
            ),
          ),
        ],
      ),
    ]),
  ];
}
