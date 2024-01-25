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
      h('span.win', winPercent + '%'),
      winrate[1] ? h('span.draw', drawPercent + '%') : null,
      h('span.loss', lossPercent + '%'),
    ]),
    horizontalBar([winPercent, drawPercent, lossPercent], ['win', 'draw', 'loss']),
  ]);
}

export function horizontalBar(numbers: number[], cls: string[] = []): VNode {
  const noFirst = numbers[0] === 0,
    noLast = numbers[numbers.length - 1] === 0;
  return h(
    'div.simple-horizontal-bar',
    numbers.map((n, i) =>
      h('div' + (cls[i] ? `.${cls[i]}` : ''), {
        style: {
          width: n + '%',
          borderRadius:
            n === 100
              ? '10px'
              : i === 0 || (noFirst && i === 1)
                ? '10px 0 0 10px'
                : i === numbers.length - 1 || (noLast && i === numbers.length - 2)
                  ? '0 10px 10px 0'
                  : '0',
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

export function bigNumberWithDesc(nb: number, desc: string, cls: string = '', affix: string = ''): VNode {
  const node = affix ? h('div', [nb, h('span.tiny', affix)]) : nb;
  return h('div.big-number-with-desc' + (cls ? '.' + cls : ''), [h('div.big-number', node), h('span.desc', desc)]);
}

export function translateStatus(status: Status, trans: Trans): string {
  const noarg = trans.noarg;

  const key = Status[status].toLowerCase();
  switch (key) {
    case 'checkmate':
    case 'resign':
    case 'stalemate':
    case 'draw':
    case 'cheat':
    case 'perpetualCheck':
    case 'royalsLost':
    case 'bareKing':
    case 'repetition':
      return noarg(key);
    case 'timeout':
    case 'outoftime':
      return noarg('timeOut');
    case 'impasse27':
      return noarg('impasse');
    case 'tryRule':
    case 'unknownFinish':
    case 'specialVariantEnd':
      return noarg('variant');
    case 'noStart':
      return trans('xDidntMove', 'x');
    default:
      return trans.noargOrCapitalize(key);
  }
}
