import { h } from 'snabbdom';
import { AutoQueen } from 'common/prefs';
import { type MaybeVNode, bind, onInsert } from 'common/snabbdom';
import type { DrawShape } from 'chessground/draw';
import { opposite, key2pos } from 'chessground/util';
import type { MoveMetadata } from 'chessground/types';

export type Hooks = {
  submit: (orig: Key, dest: Key, role: Role) => void;
  show?: (ctrl: PromotionCtrl, roles: Role[] | false) => void;
};

interface Promoting {
  orig: Key;
  dest: Key;
  pre: boolean;
  hooks: Hooks;
}

const PROMOTABLE_ROLES: Role[] = ['queen', 'knight', 'rook', 'bishop'];

export function promote(g: CgApi, key: Key, role: Role): void {
  const piece = g.state.pieces.get(key);
  if (piece && piece.role === 'pawn') {
    g.setPieces(new Map([[key, { color: piece.color, role, promoted: true }]]));
  }
}

export class PromotionCtrl {
  private promoting?: Promoting;
  private prePromotionRole?: Role;

  constructor(
    private withGround: <A>(f: (cg: CgApi) => A) => A | false | undefined,
    private onCancel: () => void,
    private redraw: () => void,
    private autoQueenPref: AutoQueen = AutoQueen.Never,
  ) {}

  start = (orig: Key, dest: Key, hooks: Hooks, meta?: MoveMetadata, forceAutoQueen = false): boolean =>
    this.withGround(g => {
      const premovePiece = g.state.pieces.get(orig);
      const piece = premovePiece || g.state.pieces.get(dest);
      if (
        piece?.role === 'pawn' &&
        ((dest[1] === '8' && g.state.turnColor === 'black') ||
          (dest[1] === '1' && g.state.turnColor === 'white'))
      ) {
        if (this.prePromotionRole && meta?.premove) {
          this.doPromote({ orig, dest, hooks }, this.prePromotionRole);
          return true;
        }
        if (
          !meta?.ctrlKey &&
          !this.promoting &&
          (this.autoQueenPref === AutoQueen.Always ||
            (this.autoQueenPref === AutoQueen.OnPremove && premovePiece) ||
            forceAutoQueen)
        ) {
          if (premovePiece) this.setPrePromotion(dest, 'queen');
          else this.doPromote({ orig, dest, hooks }, 'queen');
          return true;
        }
        this.promoting = { orig, dest, pre: !!premovePiece, hooks };
        this.redraw();
        return true;
      }
      return false;
    }) || false;

  cancel = (): void => {
    this.cancelPrePromotion();
    if (this.promoting) {
      this.promoting = undefined;
      this.onCancel();
      this.redraw();
    }
  };

  cancelPrePromotion = (): void => {
    this.promoting?.hooks.show?.(this, false);
    if (this.prePromotionRole) {
      this.withGround(g => g.setAutoShapes([]));
      this.prePromotionRole = undefined;
      this.redraw();
    }
  };

  view = (antichess?: boolean): MaybeVNode => {
    const promoting = this.promoting;
    if (!promoting) return;
    promoting.hooks.show?.(this, antichess ? [...PROMOTABLE_ROLES, 'king'] : PROMOTABLE_ROLES);

    return (
      this.withGround(g =>
        this.renderPromotion(
          promoting.dest,
          antichess ? PROMOTABLE_ROLES.concat('king') : PROMOTABLE_ROLES,
          opposite(g.state.turnColor),
          g.state.orientation,
        ),
      ) || null
    );
  };

  finish(role: Role): void {
    const promoting = this.promoting;
    if (promoting) {
      this.promoting = undefined;
      if (promoting.pre) this.setPrePromotion(promoting.dest, role);
      else this.doPromote(promoting, role);
      promoting.hooks.show?.(this, false);
      this.redraw();
    }
  }

  private doPromote(promoting: Omit<Promoting, 'pre'>, role: Role): void {
    this.withGround(g => promote(g, promoting.dest, role));
    promoting.hooks.submit(promoting.orig, promoting.dest, role);
  }

  private setPrePromotion(dest: Key, role: Role): void {
    this.prePromotionRole = role;
    this.withGround(g =>
      g.setAutoShapes([
        {
          orig: dest,
          piece: { color: opposite(g.state.turnColor), role, opacity: 0.8 },
          brush: '',
        } as DrawShape,
      ]),
    );
  }

  private renderPromotion(dest: Key, pieces: Role[], color: Color, orientation: Color): MaybeVNode {
    let left = (7 - key2pos(dest)[0]) * 12.5;
    if (orientation === 'white') left = 87.5 - left;

    const vertical = color === orientation ? 'top' : 'bottom';

    return h(
      'div#promotion-choice.' + vertical,
      {
        hook: onInsert(el => {
          el.addEventListener('click', this.cancel);
          el.oncontextmenu = () => false;
        }),
      },
      pieces.map((serverRole, i) => {
        const top = (color === orientation ? i : 7 - i) * 12.5;
        return h(
          'square',
          {
            attrs: { style: 'top: ' + top + '%;left: ' + left + '%' },
            hook: bind('click', e => {
              e.stopPropagation();
              this.finish(serverRole);
            }),
          },
          [h('piece.' + serverRole + '.' + color)],
        );
      }),
    );
  }
}
