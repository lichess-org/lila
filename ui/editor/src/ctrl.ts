import { EditorConfig, EditorOptions, EditorState, Selected, Redraw, OpeningPosition } from './interfaces';
import { Api as CgApi } from 'shogiground/api';
import { NumberPair, Role } from 'shogiground/types';
import { Rules } from 'shogiops/types';
import { Board } from 'shogiops/board';
import { Setup } from 'shogiops/setup';
import { setupPosition } from 'shogiops/variant';
import { makeSfen, parseSfen, INITIAL_SFEN, INITIAL_EPD, EMPTY_SFEN } from 'shogiops/sfen';

import { defined, prop, Prop } from 'common';
import { eventPosition, opposite } from 'shogiground/util';
import { Hand, Hands } from 'shogiops/hand';
import { handRoles, promotableRoles, promote, unpromote } from 'shogiops/variantUtil';

export default class EditorCtrl {
  cfg: EditorConfig;
  options: EditorOptions;
  trans: Trans;
  extraPositions: OpeningPosition[];
  shogiground: CgApi | undefined;
  redraw: Redraw;

  selected: Prop<Selected>;
  lastTouchMovePos: NumberPair | undefined;

  hands: Hands;
  turn: Color;
  rules: Rules;
  fullmoves: number;

  constructor(cfg: EditorConfig, redraw: Redraw) {
    this.cfg = cfg;
    this.options = cfg.options || {};

    this.trans = window.lishogi.trans(this.cfg.i18n);

    this.selected = prop('pointer');
    this.extraPositions = [
      {
        sfen: INITIAL_SFEN,
        epd: INITIAL_EPD,
        english: this.trans('startPosition'),
      },
      {
        sfen: 'prompt',
        english: this.trans('loadPosition'),
      },
    ];

    if (cfg.positions) {
      cfg.positions.forEach(p => (p.epd = p.sfen.split(' ').splice(0, 4).join(' ')));
    }

    window.Mousetrap.bind('f', (e: Event) => {
      e.preventDefault();
      if (this.shogiground) this.setOrientation(opposite(this.shogiground.state.orientation));
    });
    document.addEventListener('touchmove', e => {
      this.lastTouchMovePos = eventPosition(e as any);
    });
    document.addEventListener(
      'touchend',
      e => {
        e.preventDefault();
        if (!this.lastTouchMovePos) return;
        const target = document.elementFromPoint(this.lastTouchMovePos[0], this.lastTouchMovePos[1]);
        const droppedOnPiece = target?.getElementsByTagName('piece')[0];
        const curDrag = this.shogiground?.state.draggable.current || this.shogiground?.state.draggable.lastDropOff;
        // We add to pocket if we the touchend occurs above it and we are currently draggins something
        if (curDrag && droppedOnPiece && droppedOnPiece.getAttribute('data-nb') !== null) {
          const color = droppedOnPiece.getAttribute('data-color');
          if (color) this.addToPocket(color as Color, curDrag.piece.role, true);
        }
        this.lastTouchMovePos = undefined;
      },
      { passive: false }
    );

    this.rules =
      !this.cfg.embed && window.history.state && window.history.state.rules ? window.history.state.rules : 'shogi';

    this.redraw = () => {};
    this.setSfen(cfg.sfen);
    this.redraw = redraw;
  }

  onChange(): void {
    const sfen = this.getSfen();
    this.cfg.sfen = sfen;
    if (!this.cfg.embed) {
      if (sfen == INITIAL_SFEN) window.history.replaceState('', '', '/editor');
      else window.history.replaceState('', '', this.makeUrl('/editor/', sfen));
    }
    this.options.onChange && this.options.onChange(sfen);
    setTimeout(() => {
      this.shogiground!.state.draggable.lastDropOff = undefined;
    }, 500);
    this.redraw();
  }

  private getSetup(): Setup {
    const boardSfen = this.shogiground ? this.shogiground.getSfen() : this.cfg.sfen;
    const board = parseSfen(boardSfen).unwrap(
      setup => setup.board,
      _ => Board.empty()
    );
    return {
      board,
      hands: this.hands,
      turn: this.turn,
      fullmoves: this.fullmoves,
    };
  }

  getSfen(): string {
    return makeSfen(this.getSetup());
  }

  private getLegalSfen(): string | undefined {
    return setupPosition(this.rules, this.getSetup()).unwrap(
      pos => {
        return makeSfen(pos.toSetup());
      },
      _ => undefined
    );
  }

  private isPlayable(): boolean {
    return setupPosition(this.rules, this.getSetup()).unwrap(
      pos => !pos.isEnd(),
      _ => false
    );
  }

  getState(): EditorState {
    return {
      sfen: this.getSfen(),
      legalSfen: this.getLegalSfen(),
      playable: this.rules == 'shogi' && this.isPlayable(),
    };
  }

  makeAnalysisUrl(legalSfen: string): string {
    return this.makeUrl(`/analysis/`, this.encodeSfen(legalSfen).replace(/%2B/g, '+'));
  }

  encodeSfen(sfen: string): string {
    return encodeURIComponent(sfen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  makeUrl(baseUrl: string, sfen: string): string {
    return baseUrl + this.encodeSfen(sfen).replace(/%2B/g, '+');
  }

  bottomColor(): Color {
    return this.shogiground ? this.shogiground.state.orientation : this.options.orientation || 'sente';
  }

  setTurn(turn: Color): void {
    this.turn = turn;
    this.onChange();
  }

  startPosition(): void {
    this.setSfen(INITIAL_SFEN);
  }

  clearBoard(): void {
    this.setSfen(EMPTY_SFEN);
  }

  loadNewSfen(sfen: string | 'prompt'): void {
    if (sfen === 'prompt') {
      sfen = (prompt('Paste SFEN position') || '').trim();
      if (!sfen) return;
    }
    this.setSfen(sfen);
  }

  setSfen(sfen: string): boolean {
    return parseSfen(sfen).unwrap(
      setup => {
        if (this.shogiground) this.shogiground.set({ sfen });
        this.hands = setup.hands;
        this.turn = setup.turn;
        this.fullmoves = setup.fullmoves;

        this.onChange();
        return true;
      },
      _ => false
    );
  }

  canFillGoteHand(): boolean {
    const setup = this.getSetup();
    return (
      this.countPieces('pawn', setup) <= 18 &&
      this.countPieces('lance', setup) <= 4 &&
      this.countPieces('knight', setup) <= 4 &&
      this.countPieces('silver', setup) <= 4 &&
      this.countPieces('gold', setup) <= 4 &&
      this.countPieces('bishop', setup) <= 2 &&
      this.countPieces('rook', setup) <= 2 &&
      setup.board.occupied.size() + setup.hands.count() < 38 + this.countPieces('king', setup)
    );
  }

  // add support for variants later
  countPieces(role: Role, setup?: Setup): number {
    if (!defined(setup)) setup = this.getSetup();
    role = unpromote('shogi')(role)!;
    return (
      setup.board[role].size() +
      (handRoles('shogi').includes(role) ? setup.hands.sente[role] + setup.hands.gote[role] : 0) +
      (promotableRoles('shogi').includes(role) ? setup.board[promote('shogi')(role)].size() : 0)
    );
  }

  fillGotesHand(): void {
    const setup = this.getSetup();
    const senteHand = this.hands['sente'];

    const pieceCounts: { [index: string]: number } = {
      lance: 4 - setup.board.lance.size() - setup.board.promotedlance.size() - senteHand.lance,
      knight: 4 - setup.board.knight.size() - setup.board.promotedknight.size() - senteHand.knight,
      silver: 4 - setup.board.silver.size() - setup.board.promotedsilver.size() - senteHand.silver,
      gold: 4 - setup.board.gold.size() - senteHand.gold,
      pawn: 18 - setup.board.pawn.size() - setup.board.tokin.size() - senteHand.pawn,
      bishop: 2 - setup.board.bishop.size() - setup.board.horse.size() - senteHand.bishop,
      rook: 2 - setup.board.rook.size() - setup.board.dragon.size() - senteHand.rook,
    };
    this.hands['gote'] = Hand.empty();

    for (const p in pieceCounts) {
      if (pieceCounts[p] > 0) this.hands['gote'][p as Role] = pieceCounts[p];
    }
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.shogiground!.state.orientation !== o) this.shogiground!.toggleOrientation();
    this.redraw();
  }

  addToPocket(c: Color, r: Role, reload: boolean = false): void {
    const unpromotedRole = unpromote('shogi')(r);
    if (handRoles('shogi').includes(unpromotedRole) && this.hands[c][unpromotedRole] < 20)
      this.hands[c][unpromotedRole]++;
    if (reload) this.onChange();
  }
  removeFromPocket(c: Color, r: Role, reload: boolean = false): void {
    const unpromotedRole = unpromote('shogi')(r);
    if (handRoles('shogi').includes(unpromotedRole) && this.hands[c][unpromotedRole] > 0)
      this.hands[c][unpromotedRole]--;
    if (reload) this.onChange();
  }
  clearPocket() {
    this.hands = Hands.empty();
  }
  clearColorPocket(c: Color) {
    this.hands[c] = Hand.empty();
  }
}
