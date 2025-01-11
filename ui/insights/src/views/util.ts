import { capitalize } from 'common/string';
import { i18n, i18nFormat } from 'i18n';
import type { Role } from 'shogiops/types';
import { type VNode, h } from 'snabbdom';
import { type StatusId, type StatusKey, StatusObject, type WinRate } from '../types';
import { toPercentage } from '../util';

export function section(title: string, content: VNode | VNode[] | string): VNode {
  return h('section.with-title', [h('h2', title), h('div.section-container', content)]);
}

export function halfSection(title: string, content: (VNode | string)[]): VNode {
  return h('section.half.with-title', [h('h2', title), h('div.section-container', content)]);
}

export function smallWinrateChart(winrate: WinRate): VNode {
  const totalGames = winrate.reduce((a, b) => a + b, 0);

  const winPercent = toPercentage(winrate[0], totalGames);
  const drawPercent = toPercentage(winrate[1], totalGames);
  const lossPercent = toPercentage(winrate[2], totalGames);
  return h('div.small-winrate-wrap', [
    h('div.small-winrate-info-wrap', [
      winPercent ? h('span.win', `${winPercent}%`) : null,
      drawPercent ? h('span.draw', `${drawPercent}%`) : null,
      lossPercent ? h('span.loss', `${lossPercent}%`) : null,
    ]),
    horizontalBar([winPercent, drawPercent, lossPercent], ['win', 'draw', 'loss']),
  ]);
}

export function horizontalBar(numbers: number[], cls: string[] = []): VNode {
  return h(
    'div.simple-horizontal-bar',
    numbers.map((n, i) =>
      h(`div${cls[i] ? `.${cls[i]}` : ''}`, {
        style: {
          width: `${n}%`,
        },
      }),
    ),
  );
}

export function winrateTable(
  cls: string,
  headers: [string, string, string],
  records: Record<string, WinRate>,
  fn: (key: string) => VNode,
): VNode {
  return h(`div.winrate-table.${cls}`, [
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

export function bigNumberWithDesc(nb: number | string, desc: string, cls = '', affix = ''): VNode {
  const node = affix ? h('div', [nb, h('span.tiny', affix)]) : nb;
  return h(`div.big-number-with-desc${cls ? `.${cls}` : ''}`, [
    h('div.big-number', node),
    h('span.desc', desc),
  ]);
}

export function translateStatus(statusId: StatusId): string {
  const key = Object.keys(StatusObject).find(key => StatusObject[key as StatusKey] === statusId) as
    | string
    | undefined;
  return translateStatusName(key);
}

export function translateStatusName(statusName: string | undefined): string {
  switch (statusName) {
    case 'checkmate':
      return i18n('checkmate');
    case 'resign':
      return i18n('resign');
    case 'stalemate':
      return i18n('stalemate');
    case 'draw':
      return i18n('draw');
    case 'cheat':
      return i18n('cheat');
    case 'perpetualCheck':
      return i18n('perpetualCheck');
    case 'royalsLost':
      return i18n('royalsLost');
    case 'bareKing':
      return i18n('bareKing');
    case 'repetition':
      return i18n('repetition');
    case 'timeout':
      return i18nFormat('xLeftTheGame', 'X');
    case 'outoftime':
      return i18n('timeOut');
    case 'impasse27':
      return i18n('impasse');
    case 'unknownFinish':
      return i18n('unknown');
    case 'specialVariantEnd':
      return i18n('variant');
    case 'noStart':
      return i18nFormat('xDidntMove', 'X');
    default:
      return statusName ? capitalize(statusName) : i18n('unknown');
  }
}

export function translateRole(role: Role): string {
  switch (role) {
    case 'lance':
      return i18n('pieces:lance');
    case 'knight':
      return i18n('pieces:knight');
    case 'silver':
      return i18n('pieces:silver');
    case 'gold':
      return i18n('pieces:gold');
    case 'king':
      return i18n('pieces:king');
    case 'bishop':
      return i18n('pieces:bishop');
    case 'rook':
      return i18n('pieces:rook');
    case 'pawn':
      return i18n('pieces:pawn');
    case 'tokin':
      return i18n('pieces:tokin');
    case 'promotedlance':
      return i18n('pieces:promotedlance');
    case 'promotedsilver':
      return i18n('pieces:promotedsilver');
    case 'promotedknight':
      return i18n('pieces:promotedknight');
    case 'horse':
      return i18n('pieces:horse');
    case 'dragon':
      return i18n('pieces:dragon');
    case 'promotedpawn':
      return i18n('pieces:promotedpawn');
    case 'leopard':
      return i18n('pieces:leopard');
    case 'copper':
      return i18n('pieces:copper');
    case 'elephant':
      return i18n('pieces:elephant');
    case 'chariot':
      return i18n('pieces:chariot');
    case 'tiger':
      return i18n('pieces:tiger');
    case 'kirin':
      return i18n('pieces:kirin');
    case 'phoenix':
      return i18n('pieces:phoenix');
    case 'sidemover':
      return i18n('pieces:sidemover');
    case 'verticalmover':
      return i18n('pieces:verticalmover');
    case 'lion':
      return i18n('pieces:lion');
    case 'queen':
      return i18n('pieces:queen');
    case 'gobetween':
      return i18n('pieces:gobetween');
    case 'whitehorse':
      return i18n('pieces:whitehorse');
    case 'lionpromoted':
      return i18n('pieces:lionpromoted');
    case 'queenpromoted':
      return i18n('pieces:queenpromoted');
    case 'bishoppromoted':
      return i18n('pieces:bishoppromoted');
    case 'sidemoverpromoted':
      return i18n('pieces:sidemoverpromoted');
    case 'verticalmoverpromoted':
      return i18n('pieces:verticalmoverpromoted');
    case 'rookpromoted':
      return i18n('pieces:rookpromoted');
    case 'prince':
      return i18n('pieces:prince');
    case 'whale':
      return i18n('pieces:whale');
    case 'horsepromoted':
      return i18n('pieces:horsepromoted');
    case 'elephantpromoted':
      return i18n('pieces:elephantpromoted');
    case 'stag':
      return i18n('pieces:stag');
    case 'boar':
      return i18n('pieces:boar');
    case 'ox':
      return i18n('pieces:ox');
    case 'falcon':
      return i18n('pieces:falcon');
    case 'eagle':
      return i18n('pieces:eagle');
    case 'dragonpromoted':
      return i18n('pieces:dragonpromoted');
    default:
      return '?';
  }
}

export function translateSpeed(speed: Speed): string {
  switch (speed) {
    case 'ultraBullet':
      return i18n('ultrabullet');
    case 'bullet':
      return i18n('bullet');
    case 'blitz':
      return i18n('blitz');
    case 'rapid':
      return i18n('rapid');
    case 'classical':
      return i18n('classical');
    case 'correspondence':
      return i18n('correspondence');
    case 'unlimited':
      return i18n('unlimited');
    default:
      return speed || '';
  }
}
