import { clamp } from '@/algo';
import type { CevalHandler, EngineInfo } from '@/ceval';
import { isChrome, isMac, isMobile, isSafari } from '@/device';
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
import { info } from '@/view/dialogs';

import type { CevalCtrl } from '../ctrl';

export const searchTicks: number[] = [2, 4, 6, 8, 10, 12, 15, 20, 30];
if (!isMobile()) searchTicks.push(60, 120, 300, Number.POSITIVE_INFINITY);

export function renderCevalSettings(ctrl: CevalHandler): VNode | null {
  const ceval = ctrl.ceval;
  const info = ceval.info();
  const engine = ceval.engines.active();
  if (!ceval.showEnginePrefs() || !engine || !info) {
    return null;
  }

  const minThreads = engine.minThreads;
  const maxThreads = engine.maxThreads;
  const threads = info.threads;
  const hashSize = info.hashSize;

  let observer: ResizeObserver;

  function clickThreads(x = ceval.recommendedThreads) {
    ceval.setThreads(x);
    ctrl.startCevalIfEnabled();
    ceval.opts.redraw();
  }

  function threadsTick(dir: 'up' | 'down') {
    return div(`.arrow-${dir}`, { hook: bind('click', () => clickThreads()) });
  }

  function searchTick() {
    return clamp(
      searchTicks.findIndex(tickSecs => tickSecs * 1000 >= ceval.storedMovetime()),
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
                ctrl.startCevalIfEnabled();
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
                  isMobile() && !ceval.engines.native()
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
                    !ceval.engines.native() && [threadsTick('up'), threadsTick('down')],
                  ),
                ]),
                div('.range_value', `${threads} / ${maxThreads}`),
              ],
            );
          })('analyse-threads'),
        hashSetting(engine, hashSize, hash => {
          ceval.setHashSize(hash);
          ctrl.startCevalIfEnabled();
          ceval.opts.redraw();
        }),
      ],
    ),
  );
}

export function hashSetting(
  engine: EngineInfo,
  hashSize: number,
  onChange: (hashSize: number) => void,
  id = 'analyse-memory',
  name: string = i18n.site.memory,
): VNode {
  return div('.setting', { title: i18n.site.memoryDescription }, [
    label({ for: id }, name),
    input('range')(`#${id}`, {
      min: 4,
      max: Math.floor(Math.log2(engine.maxHash ?? 32)),
      step: 1,
      'aria-valuetext': formatHashSize(hashSize),
      hook: rangeConfig(
        () => Math.floor(Math.log2(hashSize)),
        value => onChange(Math.pow(2, value)),
      ),
    }),
    div('.range_value', formatHashSize(hashSize)),
  ]);
}

function formatHashSize(v: number) {
  return v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB';
}

function setupTick(v: VNode, ceval: CevalCtrl) {
  const engine = ceval.engines.active();
  if (!engine) return;

  const tick = v.elm as HTMLElement;
  const parentSpan = tick.parentElement!;
  const thumbWidth = isChrome() ? 17 : 19; // it is what it is
  const trackWidth = parentSpan.querySelector('input')!.offsetWidth - thumbWidth;
  const tickRatio = (ceval.recommendedThreads - engine.minThreads) / (engine.maxThreads - engine.minThreads);
  const tickLeft = Math.floor(thumbWidth / 2 + trackWidth * tickRatio);

  tick.style.left = `${tickLeft}px`;
  $(tick).toggleClass('recommended', ceval.info()?.threads === ceval.recommendedThreads);
}

function engineSelection(ctrl: CevalHandler) {
  const ceval = ctrl.ceval;
  const active = ceval.engines.active();
  const engines = ceval.engines.supporting({
    rules: ceval.rules,
    nonStandardMaterial: ceval.nonStandardMaterial,
  });
  const relayed = ceval.engines.relayed();

  return div('.setting', [
    label({ for: 'select-engine' }, i18n.site.engine),
    engineSelect(
      engines,
      active?.id,
      id => {
        ceval.selectEngine(id);
        ctrl.startCevalIfEnabled();
      },
      'select-engine',
      () => setupDirectEngineDialog(ctrl),
    ),
    relayed &&
      button('.button.button-red.button-empty', {
        ...dataIcon(licon.Trash),
        title: 'Delete external engine',
        hook: bind('click', async e => {
          (e.currentTarget as HTMLElement).blur();
          if (await confirm('Remove external engine?'))
            ceval.engines.deleteExternal(relayed.id).then(ok => ok && ceval.opts.redraw());
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
              withTech: t => t !== 'EXTERNAL' && t !== 'DIRECT',
            }),
          ),
      },
    }),
  ]);
}

export function engineSelect(
  engines: EngineInfo[],
  activeId: string | undefined,
  onChange: (id: string) => void,
  id = 'select-engine',
  onSetupNative?: () => void,
): VNode {
  return select(
    `#${id}`,
    {
      hook: bind('change', event => {
        const select = event.target as HTMLSelectElement;
        if (select.value === 'setup-direct') {
          select.value = activeId ?? engines[0]?.id ?? '';
          onSetupNative?.();
        } else onChange(select.value);
      }),
    },
    [
      ...engines.map(engine => option({ value: engine.id, selected: activeId === engine.id }, engine.name)),
      !isSafari() && onSetupNative
        ? [hl('hr'), option({ value: 'setup-direct' }, 'Setup direct engine...')]
        : [],
    ],
  );
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

async function setupDirectEngineDialog(ctrl: CevalHandler) {
  const ceval = ctrl.ceval;
  const direct = await ceval.engines.discoverDirect(true);
  if (direct) {
    info(`Connected to ${direct.name}. The engine is ready.`);
    ceval.selectEngine(direct.id);
    ctrl.startCevalIfEnabled();
    return;
  }

  const suggestion = await suggestDownload();
  const releases = 'https://github.com/jonbgamble/engine-host/releases';
  const downloads = [
    { os: 'linux', arch: 'amd64', label: 'Linux (x86-64)', extension: 'tar.gz' },
    { os: 'linux', arch: 'arm64', label: 'Linux (ARM64)', extension: 'tar.gz' },
    { os: 'windows', arch: 'amd64', label: 'Windows (x86-64)', extension: 'zip' },
    { os: 'windows', arch: 'arm64', label: 'Windows (ARM64)', extension: 'zip' },
    { os: 'macos', arch: 'arm64', label: 'macOS (Apple Silicon)', extension: 'zip' },
    { os: 'macos', arch: 'amd64', label: 'macOS (Intel)', extension: 'zip' },
  ] as const;
  const downloadLink = (download: (typeof downloads)[number]) =>
    `<li><a href="${releases}/latest/download/direct-engine-host-${download.os}-${download.arch}.${download.extension}">${download.label}</a></li>`;
  const suggested = downloads.filter(
    download => download.os === suggestion.os && (!suggestion.arch || download.arch === suggestion.arch),
  );

  await domDialog({
    modal: true,
    show: true,
    htmlText: $html`
      <h2>Direct engine setup</h2><br>
      <p>No Direct Engine Host found. Need to download it?</p>
      <ul>${(suggested.length ? suggested : downloads).map(downloadLink).join('')}</ul>
      <p><a href="${releases}/latest" target="_blank" rel="noopener">All platforms</a></p>
      <p>
        Once you've got it running, click the "Choose engine" button and locate your
        chess engine executable. Make sure to allow local network access for lichess.org in your browser
        settings. When this is done, choose "Setup direct engine" from the Engine popup again.
      </p>
      <p>You only need to do this once.</p>`,
  });
}

async function suggestDownload(): Promise<{
  os?: 'linux' | 'windows' | 'macos';
  arch?: 'amd64' | 'arm64';
}> {
  const ua = (
    navigator as Navigator & {
      userAgentData?: {
        mobile: boolean;
        platform: string;
        getHighEntropyValues?: (hints: string[]) => Promise<{ architecture?: string; bitness?: string }>;
      };
    }
  ).userAgentData;
  if (isMobile() || ua?.mobile) return {};

  const platform = ua?.platform ?? navigator.userAgent;
  const os =
    isMac() || /macOS|Macintosh/i.test(platform)
      ? 'macos'
      : /Windows/i.test(platform)
        ? 'windows'
        : /Linux/i.test(platform)
          ? 'linux'
          : undefined;
  try {
    const hints = await ua?.getHighEntropyValues?.(['architecture', 'bitness']);
    const arch =
      hints?.bitness !== '64'
        ? undefined
        : hints.architecture === 'arm'
          ? 'arm64'
          : hints.architecture === 'x86'
            ? 'amd64'
            : undefined;
    return { os, arch };
  } catch {
    return { os };
  }
}
