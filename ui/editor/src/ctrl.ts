import { Prop, defined, prop } from 'common/common';
import { Shogiground } from 'shogiground';
import { Api as SgApi } from 'shogiground/api';
import { NumberPair } from 'shogiground/types';
import { eventPosition, opposite, samePiece } from 'shogiground/util';
import { Board } from 'shogiops/board';
import { Hand, Hands } from 'shogiops/hands';
import {
  forsythToRole,
  initialSfen,
  makeBoardSfen,
  makeHands,
  makeSfen,
  parseBoardSfen,
  parseHands,
  parseSfen,
  roleToForsyth,
} from 'shogiops/sfen';
import { Role, Rules, Setup } from 'shogiops/types';
import { toBW } from 'shogiops/util';
import { handRoles, promotableRoles, promote, unpromote } from 'shogiops/variant/util';
import { defaultPosition } from 'shogiops/variant/variant';
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
    if (!this.setSfen(data.sfen)) {
      alert('Invalid SFEN');
      this.startPosition();
    }
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

  private getSetup(): Setup {
    const splitSfen = this.data.sfen.split(' '),
      boardSfen = this.shogiground ? this.shogiground.getBoardSfen() : splitSfen[0] || '',
      board = parseBoardSfen(this.rules, boardSfen).unwrap(
        b => b,
        _ => Board.empty()
      ),
      handsSfen = this.shogiground ? this.shogiground.getHandsSfen() : splitSfen[2] || '',
      hands = parseHands(this.rules, handsSfen).unwrap(
        b => b,
        _ => Hands.empty()
      );
    return {
      board: board,
      hands: hands,
      turn: this.turn,
      moveNumber: this.moveNumber,
    };
  }

  getSfen(setup?: Setup): string {
    if (!defined(setup)) setup = this.getSetup();
    return [
      makeBoardSfen(this.rules, setup.board),
      toBW(setup.turn),
      makeHands(this.rules, setup.hands),
      Math.max(1, Math.min(this.moveNumber, 9999)),
    ].join(' ');
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
    this.setSfen(
      this.getSfen({
        board: Board.empty(),
        hands: Hands.empty(),
        turn: this.turn,
        moveNumber: 1,
      })
    );
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
      err => {
        console.warn(err);
        return false;
      }
    );
  }

  setHands(hands: Hands): void {
    if (this.shogiground) this.shogiground.set({ sfen: { hands: makeHands(this.rules, hands) } });
    this.onChange();
  }

  canFillGoteHand(): boolean {
    const setup = this.getSetup(),
      startingBoard = defaultPosition(this.rules).board;
    return (
      this.countPieces('pawn', setup) <= startingBoard.role('pawn').size() &&
      this.countPieces('lance', setup) <= startingBoard.role('lance').size() &&
      this.countPieces('knight', setup) <= startingBoard.role('knight').size() &&
      this.countPieces('silver', setup) <= startingBoard.role('silver').size() &&
      this.countPieces('gold', setup) <= startingBoard.role('gold').size() &&
      this.countPieces('bishop', setup) <= startingBoard.role('bishop').size() &&
      this.countPieces('rook', setup) <= startingBoard.role('rook').size() &&
      setup.board.occupied.size() + setup.hands.count() <
        startingBoard.occupied.size() - 2 + this.countPieces('king', setup)
    );
  }

  countPieces(role: Role, setup?: Setup): number {
    if (!defined(setup)) setup = this.getSetup();
    role = unpromote(this.rules)(role) || role;
    return (
      setup.board.role(role).size() +
      (handRoles(this.rules).includes(role)
        ? setup.hands.color('sente').get(role) + setup.hands.color('gote').get(role)
        : 0) +
      (promotableRoles(this.rules).includes(role) ? setup.board.role(promote(this.rules)(role)!).size() : 0)
    );
  }

  fillGotesHand(): void {
    const setup = this.getSetup(),
      board = setup.board,
      senteHand = setup.hands.color('sente'),
      startingBoard = defaultPosition(this.rules).board;

    const pieceCounts: { [index: string]: number } = {
      lance:
        startingBoard.role('lance').size() -
        board.role('lance').size() -
        board.role('promotedlance').size() -
        senteHand.get('lance'),
      knight:
        startingBoard.role('knight').size() -
        board.role('knight').size() -
        board.role('promotedknight').size() -
        senteHand.get('knight'),
      silver:
        startingBoard.role('silver').size() -
        board.role('silver').size() -
        board.role('promotedsilver').size() -
        senteHand.get('silver'),
      gold: startingBoard.role('gold').size() - board.role('gold').size() - senteHand.get('gold'),
      pawn:
        startingBoard.role('pawn').size() -
        board.role('pawn').size() -
        board.role('tokin').size() -
        senteHand.get('pawn'),
      bishop:
        startingBoard.role('bishop').size() -
        board.role('bishop').size() -
        board.role('horse').size() -
        senteHand.get('bishop'),
      rook:
        startingBoard.role('rook').size() -
        board.role('rook').size() -
        board.role('dragon').size() -
        senteHand.get('rook'),
    };
    const goteHand = Hand.empty();

    for (const p in pieceCounts) {
      if (pieceCounts[p] > 0) goteHand.set(p as Role, pieceCounts[p]);
    }

    this.setHands(Hands.from(senteHand, goteHand));
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
