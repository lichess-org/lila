import { clamp } from '@/algo';
import type { CevalHandler, EngineInfo } from '@/ceval';
import { isChrome, isMobile } from '@/device';
import { onClickAway } from '@/index';
import { licon } from '@/licon';
import {
  type VNode,
  onInsert,
  bind,
  dataIcon,
  hl,
  rangeConfig,
  confirm,
  domDialog,
  select,
  label,
  option,
  div,
  button,
  span,
  input,
} from '@/view';

import type { CevalCtrl } from '../ctrl';
import { fewerCores } from '../util';

const allSearchTicks: number[] = [2, 4, 6, 8, 10, 12, 15, 20, 30];
if (!isMobile()) allSearchTicks.push(60, 120, 300, Number.POSITIVE_INFINITY);

export function renderCevalSettings(ctrl: CevalHandler): VNode | null {
  const ceval = ctrl.ceval;

  if (!ceval.showEnginePrefs()) {
    return null;
  }

  const minThreads = ceval.engines.active()?.minThreads ?? 1;
  const maxThreads = ceval.maxThreads;
  const threads = ceval.info()?.threads ?? 1;
  const hashSize = ceval.info()?.hashSize ?? 4;
  const searchTicks = allSearchTicks.filter(
    x => x * 1000 <= (ceval.engines.active()?.maxMovetime ?? Infinity),
  );

  let observer: ResizeObserver;

  function clickThreads(x = ceval.recommendedThreads) {
    ceval.setThreads(x);
    ctrl.startCeval();
    ceval.opts.redraw();
  }

  function threadsTick(dir: 'up' | 'down') {
    return div(`.arrow-${dir}`, { hook: bind('click', () => clickThreads()) });
  }

  function searchTick() {
    return clamp(
      allSearchTicks.findIndex(tickSecs => tickSecs * 1000 >= ceval.storedMovetime()),
      { min: 0, max: searchTicks.length - 1 },
    );
  }

  return div(
    '#ceval-settings-anchor',
    hl(
      'div#ceval-settings',
      {
        hook: onInsert(
          onClickAway(() => {
            ceval.showEnginePrefs(false);
            ceval.opts.redraw();
          }),
        ),
      },
      [
        engineSelection(ctrl),
        (id => {
          return div('.setting', { title: i18n.site.searchTimeDescription }, [
            label({ for: id }, i18n.site.searchTime),
            input('range')(`#${id}`, {
              min: 0,
              max: searchTicks.length - 1,
              step: 1,
              'aria-valuetext': i18n.site.nbSeconds(searchTicks[searchTick()]),
              hook: rangeConfig(searchTick, n => {
                ceval.storedMovetime(searchTicks[n] * 1000);
                ctrl.startCeval();
                ceval.opts.redraw();
              }),
            }),
            div('.range_value', isFinite(searchTicks[searchTick()]) ? `${searchTicks[searchTick()]}s` : '∞'),
          ]);
        })('engine-search-ms'),
        (id => {
          const max = 5;
          return div('.setting', { title: i18n.site.multipleLinesDescription }, [
            label({ for: id }, i18n.site.multipleLines),
            input('range')(`#${id}`, {
              min: 0,
              max,
              step: 1,
              hook: rangeConfig(
                () => ceval.storedPv(),
                (pvs: number) => {
                  ceval.storedPv(pvs);
                  ctrl.clearCeval?.();
                  ceval.opts.redraw();
                },
              ),
            }),
            div('.range_value', `${ceval.storedPv()} / ${max}`),
          ]);
        })('analyse-multipv'),
        maxThreads > minThreads &&
          (id => {
            return div(
              '.setting',
              {
                title:
                  fewerCores() && !ceval.engines.external
                    ? i18n.site.threadsDescriptionMobile
                    : i18n.site.threadsDescription,
              },
              [
                label({ for: id }, i18n.site.threads),
                span([
                  input('range')(`#${id}`, {
                    min: minThreads,
                    max: maxThreads,
                    step: 1,
                    hook: rangeConfig(() => threads, clickThreads),
                  }),
                  hl(
                    'div.tick',
                    {
                      hook: {
                        update: (_, v) => setupTick(v, ceval),
                        insert: v => {
                          setupTick(v, ceval);
                          let animationFrameRequestId: number;
                          observer = new ResizeObserver(() => {
                            cancelAnimationFrame(animationFrameRequestId);
                            animationFrameRequestId = requestAnimationFrame(() => setupTick(v, ceval));
                          });
                          observer.observe(v.elm!.parentElement!);
                        },
                        destroy: () => observer?.disconnect(),
                      },
                    },
                    !ceval.engines.external && [threadsTick('up'), threadsTick('down')],
                  ),
                ]),
                div('.range_value', `${threads} / ${maxThreads}`),
              ],
            );
          })('analyse-threads'),
        (id =>
          div('.setting', { title: i18n.site.memoryDescription }, [
            label({ for: id }, i18n.site.memory),
            input('range')(`#${id}`, {
              min: 4,
              max: Math.floor(Math.log2(ceval.engines.active()?.maxHash ?? 4)),
              step: 1,
              'aria-valuetext': formatHashSize(hashSize),
              hook: rangeConfig(
                () => Math.floor(Math.log2(hashSize)),
                v => {
                  ceval.setHashSize(Math.pow(2, v));
                  ctrl.startCeval();
                  ceval.opts.redraw();
                },
              ),
            }),

            div('.range_value', formatHashSize(hashSize)),
          ]))('analyse-memory'),
      ],
    ),
  );
}

function formatHashSize(v: number) {
  return v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB';
}

function setupTick(v: VNode, ceval: CevalCtrl) {
  const tick = v.elm as HTMLElement;
  const parentSpan = tick.parentElement!;
  const minThreads = ceval.engines.active()?.minThreads ?? 1;
  const thumbWidth = isChrome() ? 17 : 19; // it is what it is
  const trackWidth = parentSpan.querySelector('input')!.offsetWidth - thumbWidth;
  const tickRatio = (ceval.recommendedThreads - minThreads) / (ceval.maxThreads - minThreads);
  const tickLeft = Math.floor(thumbWidth / 2 + trackWidth * tickRatio);

  tick.style.left = `${tickLeft}px`;
  $(tick).toggleClass('recommended', ceval.info()?.threads === ceval.recommendedThreads);
}

function engineSelection({ ceval }: CevalHandler) {
  const active = ceval.engines.active();
  const engines = ceval.engines.supporting({
    rules: ceval.rules,
    nonStandardMaterial: ceval.nonStandardMaterial,
  });
  const external = ceval.engines.external;

  return div('.setting', [
    label({ for: 'select-engine' }, 'Engine:'),
    select(
      '#select-engine',
      {
        hook: bind('change', e => {
          ceval.selectEngine((e.target as HTMLSelectElement).value);
          ceval.opts.redraw();
        }),
      },
      engines.map(({ id, name }) => option({ value: id, selected: active?.id === id }, name)),
    ),
    external &&
      button('.button.button-red.button-empty', {
        ...dataIcon(licon.Trash),
        title: 'Delete external engine',
        hook: bind('click', async e => {
          (e.currentTarget as HTMLElement).blur();
          if (await confirm('Remove external engine?'))
            ceval.engines.deleteExternal(external.id).then(ok => ok && ceval.opts.redraw());
        }),
      }),
    button('.engine-info-button', {
      ...dataIcon(licon.InfoCircle),
      title: i18n.site.enginesFromStrongestToWeakest,
      on: {
        click: () =>
          engineInfo(
            ceval.engines.supporting({
              rules: ceval.rules,
              nonStandardMaterial: ceval.nonStandardMaterial,
              filter: 'browser',
            }),
          ),
      },
    }),
  ]);
}

function engineInfo(engines: EngineInfo[]) {
  if (document.querySelector('.engine-info')) return;
  const engineHtml = (e: EngineInfo) =>
    `<li>${e.name} ${e.url ? `<a href="${e.url}" target="_blank">source</a>` : ''}</li>`;
  domDialog({
    class: 'engine-info-popup',
    easyClose: 'clickOutside',
    htmlText: $html`
      <div>
        <p>${i18n.site.enginesFromStrongestToWeakest}</p>
        <ol>${engines.map(engineHtml).join('')}</ol>
      </div>`,
    show: true,
  });
}
