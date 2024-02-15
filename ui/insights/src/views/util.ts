import { VNode, h } from 'snabbdom';
import { Status, WinRate } from '../types';
import { toPercentage } from '../util';

export function section(title: string, content: VNode | VNode[] | string): VNode {
  return h('section.with-title', [h('h2', title), h('div.section-container', content)]);
}

export function halfSection(title: string, content: (VNode | string)[]): VNode {
  return h('section.half.with-title', [h('h2', title), h('div.section-container', content)]);
}

export function smallWinrateChart(winrate: WinRate): VNode {
  const totalGames = winrate.reduce((a, b) => a + b, 0);

  const winPercent = toPercentage(winrate[0], totalGames),
    drawPercent = toPercentage(winrate[1], totalGames),
    lossPercent = toPercentage(winrate[2], totalGames);
  return h('div.small-winrate-wrap', [
    h('div.small-winrate-info-wrap', [
      winPercent ? h('span.win', winPercent + '%') : null,
      drawPercent ? h('span.draw', drawPercent + '%') : null,
      lossPercent ? h('span.loss', lossPercent + '%') : null,
    ]),
    horizontalBar([winPercent, drawPercent, lossPercent], ['win', 'draw', 'loss']),
  ]);
}

export function horizontalBar(numbers: number[], cls: string[] = []): VNode {
  return h(
    'div.simple-horizontal-bar',
    numbers.map((n, i) =>
      h('div' + (cls[i] ? `.${cls[i]}` : ''), {
        style: {
          width: n + '%',
        },
      })
    )
  );
}

export function winrateTable(
  cls: string,
  headers: [string, string, string],
  records: Record<string, WinRate>,
  fn: (key: string) => VNode
): VNode {
  return h('div.winrate-table.' + cls, [
    h('div.winrate-table-header', [
      h('div.table-col1', headers[0]),
      h('div.table-col2', headers[1]),
      h('div.table-col3', headers[2]),
    ]),
    h('div.winrate-table-content', tableContent(records, fn)),
  ]);
}
function tableContent(records: Record<string, WinRate>, fn: (key: string) => VNode): VNode[] {
  return Object.keys(records).map(key => {
    const cur = records[key];
    const total = cur[0] + cur[1] + cur[2];
    return h('div.winrate-table-row', [
      fn(key),
      h('div.table-col2', total),
      h('div.table-col3', smallWinrateChart(cur)),
    ]);
  });
}

export function bigNumberWithDesc(nb: number | string, desc: string, cls: string = '', affix: string = ''): VNode {
  const node = affix ? h('div', [nb, h('span.tiny', affix)]) : nb;
  return h('div.big-number-with-desc' + (cls ? '.' + cls : ''), [h('div.big-number', node), h('span.desc', desc)]);
}

export function translateStatus(status: Status, trans: Trans): string {
  const key = Status[status];
  return translateStatusName(key, trans);
}

export function translateStatusName(statusName: string, trans: Trans): string {
  const noarg = trans.noarg;

  switch (statusName) {
    case 'checkmate':
    case 'resign':
    case 'stalemate':
    case 'draw':
    case 'cheat':
    case 'perpetualCheck':
    case 'royalsLost':
    case 'bareKing':
    case 'repetition':
      return noarg(statusName);
    case 'timeout':
      return trans('xLeftTheGame', 'X');
    case 'outoftime':
      return noarg('timeOut');
    case 'impasse27':
      return noarg('impasse');
    case 'unknownFinish':
      return noarg('unknown');
    case 'specialVariantEnd':
      return noarg('variant');
    case 'noStart':
      return trans('xDidntMove', 'X');
    default:
      return trans.noargOrCapitalize(statusName);
  }
}
