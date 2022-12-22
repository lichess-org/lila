import { Prop, defined, prop } from 'common/common';
import { Shogiground } from 'shogiground';
import { Api as SgApi } from 'shogiground/api';
import { NumberPair } from 'shogiground/types';
import { eventPosition, opposite, samePiece } from 'shogiground/util';
import { Board } from 'shogiops/board';
import { Hand, Hands } from 'shogiops/hands';
import { forsythToRole, initialSfen, makeSfen, parseSfen, roleToForsyth } from 'shogiops/sfen';
import { Role, Rules } from 'shogiops/types';
import { toBW } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { handRoles, promotableRoles, promote, unpromote } from 'shogiops/variant/util';
import { initializePosition } from 'shogiops/variant/variant';
import { EditorData, EditorOptions, EditorState, OpeningPosition, Redraw, Selected } from './interfaces';
import { makeConfig } from './shogiground';

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
  moveNumber: number;

  constructor(data: EditorData, redraw: Redraw) {
    this.data = data;
    this.rules = data.variant;
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
    return parseSfen(this.rules, `${boardSfen} ${toBW(this.turn)} ${handsSfen} ${this.moveNumber}`, false).unwrap();
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
    const emptyPos = initializePosition(
      this.rules,
      {
        board: Board.empty(),
        hands: Hands.empty(),
        turn: this.turn,
        moveNumber: 1,
      },
      false
    ).unwrap();
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
        this.moveNumber = pos.moveNumber;

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
      pos.board.role(role).size() +
      (handRoles(this.rules).includes(role)
        ? pos.hands.color('sente').get(role) + pos.hands.color('gote').get(role)
        : 0) +
      (promotableRoles(this.rules).includes(role) ? pos.board.role(promote(this.rules)(role)!).size() : 0)
    );
  }

  fillGotesHand(): void {
    const pos = this.getPosition();
    const senteHand = pos.hands.color('sente');

    const pieceCounts: { [index: string]: number } = {
      lance: 4 - pos.board.role('lance').size() - pos.board.role('promotedlance').size() - senteHand.get('lance'),
      knight: 4 - pos.board.role('knight').size() - pos.board.role('promotedknight').size() - senteHand.get('knight'),
      silver: 4 - pos.board.role('silver').size() - pos.board.role('promotedsilver').size() - senteHand.get('silver'),
      gold: 4 - pos.board.role('gold').size() - senteHand.get('gold'),
      pawn: 18 - pos.board.role('pawn').size() - pos.board.role('tokin').size() - senteHand.get('pawn'),
      bishop: 2 - pos.board.role('bishop').size() - pos.board.role('horse').size() - senteHand.get('bishop'),
      rook: 2 - pos.board.role('rook').size() - pos.board.role('dragon').size() - senteHand.get('rook'),
    };
    const goteHand = Hand.empty();

    for (const p in pieceCounts) {
      if (pieceCounts[p] > 0) goteHand.set(p as Role, pieceCounts[p]);
    }
    pos.hands = Hands.from(senteHand, goteHand);

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
        forsyth: {
          fromForsyth: forsythToRole(rules),
          toForsyth: roleToForsyth(rules),
        },
      },
      true
    );
    if (rules === 'chushogi') window.lishogi.loadChushogiPieceSprite();
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
