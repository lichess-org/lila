import { EditorData, EditorOptions, EditorState, Selected, Redraw, OpeningPosition } from './interfaces';
import { Api as SgApi } from 'shogiground/api';
import { NumberPair } from 'shogiground/types';
import { Rules, Role } from 'shogiops/types';
import { makeSfen, parseSfen, initialSfen } from 'shogiops/sfen';
import { Position } from 'shogiops/shogi';
import { defined, prop, Prop } from 'common/common';
import { eventPosition, opposite, samePiece } from 'shogiground/util';
import { Hand, Hands } from 'shogiops/hand';
import { handRoles, promotableRoles, promote, unpromote } from 'shogiops/variantUtil';
import { toBW } from 'shogiops/util';
import { Shogiground } from 'shogiground';
import { makeConfig } from './shogiground';
import { initializePosition } from 'shogiops/variant';
import { Board } from 'shogiops/board';

export default class EditorCtrl {
  data: EditorData;
  options: EditorOptions;
  trans: Trans;
  extraPositions: OpeningPosition[];
  shogiground: SgApi;
  redraw: Redraw;

  selected: Prop<Selected>;
  initTouchMovePos: NumberPair | undefined;
  lastTouchMovePos: NumberPair | undefined;

  turn: Color;
  rules: Rules;
  fullmoves: number;

  constructor(data: EditorData, redraw: Redraw) {
    this.data = data;
    this.options = data.options || {};

    this.trans = window.lishogi.trans(this.data.i18n);

    this.shogiground = Shogiground(makeConfig(this));

    this.selected = prop('pointer');
    this.extraPositions = [
      {
        sfen: 'start',
        english: this.trans('startPosition'),
      },
      {
        sfen: 'prompt',
        english: this.trans('loadPosition'),
      },
    ];

    window.Mousetrap.bind('f', (e: Event) => {
      e.preventDefault();
      if (this.shogiground) this.setOrientation(opposite(this.shogiground.state.orientation));
    });
    document.addEventListener('touchmove', e => {
      this.lastTouchMovePos = eventPosition(e as any);
      if (!this.initTouchMovePos) this.initTouchMovePos = this.lastTouchMovePos;
    });
    this.rules = data.variant;

    this.redraw = () => {};
    this.setSfen(data.sfen);
    this.redraw = redraw;
  }

  onChange(): void {
    const sfen = this.getSfen();
    this.data.sfen = sfen;
    if (!this.data.embed) window.history.replaceState('', '', this.makeEditorUrl(sfen));
    const cur = this.selected();
    if (typeof cur !== 'string' && this.shogiground)
      this.shogiground.selectPiece({ color: cur[0], role: cur[1] }, true);
    this.options.onChange && this.options.onChange(sfen);
    this.redraw();
  }

  private getPosition(): Position {
    const splitSfen = this.data.sfen.split(' ');
    const boardSfen = this.shogiground ? this.shogiground.getBoardSfen() : splitSfen[0] || '';
    const handsSfen = this.shogiground ? this.shogiground.getHandsSfen() : splitSfen[2] || '';
    return parseSfen(this.rules, `${boardSfen} ${toBW(this.turn)} ${handsSfen} ${this.fullmoves}`, false).unwrap();
  }

  getSfen(): string {
    return makeSfen(this.getPosition());
  }

  private getLegalSfen(): string | undefined {
    return parseSfen(this.rules, this.getSfen(), true).unwrap(
      pos => {
        return makeSfen(pos);
      },
      _ => undefined
    );
  }

  private isPlayable(): boolean {
    return parseSfen(this.rules, this.getSfen()).unwrap(
      pos => !pos.isEnd(),
      _ => false
    );
  }

  getState(): EditorState {
    return {
      sfen: this.getSfen(),
      legalSfen: this.getLegalSfen(),
      playable: this.isPlayable(),
    };
  }

  makeAnalysisUrl(legalSfen: string, orientation: Color = 'sente'): string {
    return `/analysis/${this.rules}/${this.encodeSfen(legalSfen)}?color=${orientation}`;
  }

  makeEditorUrl(sfen: string): string {
    const variant = this.rules === 'standard' ? '' : `/${this.rules}`;
    if (sfen === initialSfen(this.rules)) return `${this.data.baseUrl}${variant}`;
    else return `${this.data.baseUrl}${variant}/${this.encodeSfen(sfen)}`;
  }

  encodeSfen(sfen: string): string {
    return encodeURIComponent(sfen).replace(/%20/g, '_').replace(/%2F/g, '/').replace(/%2B/g, '+');
  }

  bottomColor(): Color {
    return this.shogiground ? this.shogiground.state.orientation : this.options.orientation || 'sente';
  }

  setTurn(turn: Color): void {
    this.turn = turn;
    this.onChange();
  }

  startPosition(): void {
    this.setSfen(initialSfen(this.rules));
  }

  clearBoard(): void {
    const emptyPos = initializePosition(this.rules, Board.empty(), Hands.empty(), this.turn, 1, false).unwrap();
    this.setSfen(makeSfen(emptyPos));
  }

  loadNewSfen(sfen: string | 'prompt'): void {
    if (sfen === 'prompt') {
      sfen = (prompt('Paste SFEN position') || '').trim();
      if (!sfen) return;
    }
    this.setSfen(sfen);
  }

  setSfen(sfen: string): boolean {
    return parseSfen(this.rules, sfen, false).unwrap(
      pos => {
        const splitSfen = sfen.split(' ');
        if (this.shogiground) this.shogiground.set({ sfen: { board: splitSfen[0], hands: splitSfen[2] } });
        this.turn = pos.turn;
        this.fullmoves = pos.fullmoves;

        this.onChange();
        return true;
      },
      _ => false
    );
  }

  canFillGoteHand(): boolean {
    const pos = this.getPosition();
    return (
      this.countPieces('pawn', pos) <= 18 &&
      this.countPieces('lance', pos) <= 4 &&
      this.countPieces('knight', pos) <= 4 &&
      this.countPieces('silver', pos) <= 4 &&
      this.countPieces('gold', pos) <= 4 &&
      this.countPieces('bishop', pos) <= 2 &&
      this.countPieces('rook', pos) <= 2 &&
      pos.board.occupied.size() + pos.hands.count() < 38 + this.countPieces('king', pos)
    );
  }

  countPieces(role: Role, pos?: Position): number {
    if (!defined(pos)) pos = this.getPosition();
    role = unpromote(this.rules)(role) || role;
    return (
      pos.board[role].size() +
      (handRoles(this.rules).includes(role) ? pos.hands.sente[role] + pos.hands.gote[role] : 0) +
      (promotableRoles(this.rules).includes(role) ? pos.board[promote(this.rules)(role)!].size() : 0)
    );
  }

  fillGotesHand(): void {
    const pos = this.getPosition();
    const senteHand = pos.hands['sente'];

    const pieceCounts: { [index: string]: number } = {
      lance: 4 - pos.board.lance.size() - pos.board.promotedlance.size() - senteHand.lance,
      knight: 4 - pos.board.knight.size() - pos.board.promotedknight.size() - senteHand.knight,
      silver: 4 - pos.board.silver.size() - pos.board.promotedsilver.size() - senteHand.silver,
      gold: 4 - pos.board.gold.size() - senteHand.gold,
      pawn: 18 - pos.board.pawn.size() - pos.board.tokin.size() - senteHand.pawn,
      bishop: 2 - pos.board.bishop.size() - pos.board.horse.size() - senteHand.bishop,
      rook: 2 - pos.board.rook.size() - pos.board.dragon.size() - senteHand.rook,
    };
    pos.hands['gote'] = Hand.empty();

    for (const p in pieceCounts) {
      if (pieceCounts[p] > 0) pos.hands['gote'][p as Role] = pieceCounts[p];
    }

    this.setSfen(makeSfen(pos));

    this.onChange();
  }

  setRules(rules: Rules): void {
    this.rules = rules;
    const sfen = initialSfen(rules);
    const splitSfen = sfen.split(' ');
    this.shogiground.set(
      {
        sfen: {
          board: splitSfen[0],
          hands: splitSfen[2],
        },
        hands: {
          roles: handRoles(rules),
        },
      },
      true
    );
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.shogiground.state.orientation !== o) this.shogiground.toggleOrientation();
    this.redraw();
  }

  addToHand(c: Color, r: Role, reload: boolean = false): void {
    const unpromotedRole = unpromote(this.rules)(r);
    this.shogiground.addToHand({ color: c, role: unpromotedRole || r });
    if (reload) this.onChange();
  }
  removeFromHand(c: Color, r: Role, reload: boolean = false): void {
    const unpromotedRole = unpromote(this.rules)(r);
    const piece = { color: c, role: unpromotedRole || r };
    this.shogiground.removeFromHand(piece);
    // unselect if we no loger have piece in hand
    if (
      this.shogiground.state.selectedPiece &&
      samePiece(this.shogiground.state.selectedPiece, piece) &&
      this.shogiground.state.hands.handMap.get(c)?.get(r) === 0
    )
      this.shogiground.selectPiece(null);
    if (reload) this.onChange();
  }
}
