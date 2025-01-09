import { loadChushogiPieceSprite, loadKyotoshogiPieceSprite } from 'common/assets';
import * as data from 'common/data';
import type { Api } from 'shogiground/api';
import type { Config } from 'shogiground/config';
import { usiToSquareNames } from 'shogiops/compat';
import { forsythToRole } from 'shogiops/sfen';
import { handRoles } from 'shogiops/variant/util';
import { resize } from '../util';

function parseSfen(el: HTMLElement): void {
  const lm = el.dataset.lastmove,
    sfen = el.dataset.sfen,
    hands = sfen && sfen.split(' ').length > 2 ? sfen.split(' ')[2] : '',
    color = (el.dataset.color || 'sente') as Color,
    variant = (el.dataset.variant || 'standard') as VariantKey,
    config: Config = {
      coordinates: { enabled: false },
      orientation: color,
      drawable: { enabled: false, visible: false },
      viewOnly: true,
      sfen: { board: sfen, hands: hands },
      hands: { roles: handRoles(variant), inlined: variant !== 'chushogi' },
      lastDests: lm ? usiToSquareNames(lm) : undefined,
      forsyth: {
        fromForsyth: forsythToRole(variant),
      },
    };

  if (variant === 'chushogi') loadChushogiPieceSprite();
  else if (variant === 'kyotoshogi') loadKyotoshogiPieceSprite();

  const sg = data.get<Api>(el, 'shogiground');
  if (sg) sg.set(config);
  else {
    const sgWrap = document.createElement('div');
    sgWrap.classList.add('sg-wrap');
    el.appendChild(sgWrap);
    data.set(el, 'shogiground', window.Shogiground(config, { board: sgWrap }));
  }
}

window.lishogi.ready.then(() => {
  const featuredEl = document.getElementById('featured-game')!;
  const board = (): HTMLAnchorElement => {
    return featuredEl.querySelector<HTMLAnchorElement>('a.mini-board')!;
  };
  parseSfen(board());

  if (!window.EventSource) return;

  const source = new EventSource(document.body.dataset.streamUrl!);
  source.addEventListener(
    'message',
    e => {
      const data = JSON.parse(e.data);
      if (data.t === 'featured') {
        featuredEl.innerHTML = data.d.html;
        featuredEl.querySelectorAll('a').forEach(anchor => {
          anchor.setAttribute('target', '_blank');
        });
        parseSfen(board());
      } else if (data.t == 'sfen') {
        const boardEl = board();
        boardEl.dataset.sfen = data.d.sfen;
        boardEl.dataset.lastmove = data.d.lm;
        parseSfen(boardEl);
      }
    },
    false,
  );

  resize(featuredEl);
  window.addEventListener('resize', () => resize(featuredEl));
});
