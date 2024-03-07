import { Prop, defined, prop } from 'common/common';
import { analysis, editor } from 'common/links';
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
  makeHandsSfen,
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
import { EditorData, EditorOptions, EditorState, Redraw, Selected } from './interfaces';
import { makeConfig } from './shogiground';

export default class EditorCtrl {
  data: EditorData;
  options: EditorOptions;
  trans: Trans;
  shogiground: SgApi;
  redraw: Redraw;

  selected: Prop<Selected>;
  initTouchMovePos: NumberPair | undefined;
  lastTouchMovePos: NumberPair | undefined;

  turn: Color;
  rules: Rules;
  moveNumber: number;

  backStack: string[] = [];
  currentBeforeStack: string;
  forwardStack: string[] = [];

  constructor(data: EditorData, redraw: Redraw) {
    this.data = data;
    this.rules = data.variant;
    this.options = data.options;

    this.trans = window.lishogi.trans(this.data.i18n);

    this.shogiground = Shogiground(makeConfig(this));

    this.selected = prop('pointer');

    this.bind();

    this.redraw = () => {};
    if (!this.setSfen(data.sfen)) {
      alert(this.trans.noarg('invalidSfen'));
      this.startPosition();
    }
    this.redraw = redraw;
  }

  bind(): void {
    if (!window.Mousetrap) return;
    const preventing = (f: () => void) => (e: MouseEvent) => {
      e.preventDefault();
      f();
    };

    const kbd = window.Mousetrap;
    kbd.bind(
      ['f'],
      preventing(() => {
        if (this.shogiground) this.setOrientation(opposite(this.shogiground.state.orientation));
      })
    );
    kbd.bind(
      ['left', 'k'],
      preventing(() => {
        this.backward();
        this.redraw();
      })
    );
    kbd.bind(
      ['right', 'j'],
      preventing(() => {
        this.forward();
        this.redraw();
      })
    );
    kbd.bind(
      ['up', '0'],
      preventing(() => {
        this.first();
        this.redraw();
      })
    );
    kbd.bind(
      ['down', '$'],
      preventing(() => {
        this.last();
        this.redraw();
      })
    );

    document.addEventListener('touchmove', e => {
      this.lastTouchMovePos = eventPosition(e as any);
      if (!this.initTouchMovePos) this.initTouchMovePos = this.lastTouchMovePos;
    });
  }

  onChange(history = false, pushState = false): void {
    const sfen = this.getSfen();
    this.data.sfen = sfen;

    if (!history) {
      if (this.currentBeforeStack) {
        this.forwardStack = [];
        this.backStack.push(this.currentBeforeStack);
      }
      this.currentBeforeStack = sfen;
    }

    if (!this.data.embed) {
      if (pushState) window.history.pushState('', '', this.makeEditorUrl(sfen));
      else window.history.replaceState('', '', this.makeEditorUrl(sfen));
    }
    const cur = this.selected();
    if (typeof cur !== 'string' && this.shogiground)
      this.shogiground.selectPiece({ color: cur[0], role: cur[1] }, true, true);
    this.options.onChange?.(sfen, this.rules, this.bottomColor());
    this.redraw();
  }

  forward(): void {
    if (this.forwardStack.length) {
      const sfen = this.forwardStack.pop()!;
      this.backStack.push(this.currentBeforeStack);
      this.currentBeforeStack = sfen;
      this.setSfen(sfen, true);
    }
  }

  backward(): void {
    if (this.backStack.length) {
      const sfen = this.backStack.pop()!;
      this.forwardStack.push(this.currentBeforeStack);
      this.currentBeforeStack = sfen;
      this.setSfen(sfen, true);
    }
  }

  last(): void {
    if (this.forwardStack.length) {
      this.forwardStack.reverse();
      const sfen = this.forwardStack.pop()!;
      this.backStack.push(this.currentBeforeStack);
      this.backStack = this.backStack.concat(this.forwardStack);
      this.forwardStack = [];
      this.currentBeforeStack = sfen;
      this.setSfen(sfen, true);
    }
  }

  first(): void {
    if (this.backStack.length) {
      this.backStack.reverse();
      const sfen = this.backStack.pop()!;
      this.forwardStack.push(this.currentBeforeStack);
      this.forwardStack = this.forwardStack.concat(this.backStack);
      this.backStack = [];
      this.currentBeforeStack = sfen;
      this.setSfen(sfen, true);
    }
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
      makeHandsSfen(this.rules, setup.hands),
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
    return analysis(this.rules, legalSfen, orientation);
  }

  makeEditorUrl(sfen: string): string {
    return this.data.baseUrl + editor(this.rules, sfen, this.bottomColor());
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

  setSfen(sfen: string, history: boolean = false): boolean {
    return parseSfen(this.rules, sfen, false).unwrap(
      pos => {
        const splitSfen = sfen.split(' ');
        if (this.shogiground) this.shogiground.set({ sfen: { board: splitSfen[0], hands: splitSfen[2] } });
        this.turn = pos.turn;
        this.moveNumber = pos.moveNumber;

        this.onChange(history);
        return true;
      },
      err => {
        console.warn(err);
        return false;
      }
    );
  }

  setHands(hands: Hands): void {
    if (this.shogiground) this.shogiground.set({ sfen: { hands: makeHandsSfen(this.rules, hands) } });
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
    this.turn = 'sente';
    const sfen = initialSfen(rules),
      splitSfen = sfen.split(' ');
    this.shogiground.set(
      {
        sfen: {
          board: splitSfen[0],
          hands: splitSfen[2],
        },
        hands: {
          roles: handRoles(rules),
          inlined: rules !== 'chushogi',
        },
        forsyth: {
          fromForsyth: forsythToRole(rules),
          toForsyth: roleToForsyth(rules),
        },
      },
      true
    );
    if (rules === 'chushogi') window.lishogi.loadChushogiPieceSprite();
    else if (rules === 'kyotoshogi') window.lishogi.loadKyotoshogiPieceSprite();
    this.onChange(false, true);
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.shogiground.state.orientation !== o) this.shogiground.toggleOrientation();
    this.onChange();
  }

  addToHand(c: Color, r: Role, reload: boolean = false): void {
    const unpromotedRole = handRoles(this.rules).includes(r) ? r : unpromote(this.rules)(r);
    this.shogiground.addToHand({ color: c, role: unpromotedRole || r });
    if (reload) this.onChange();
  }
  removeFromHand(c: Color, r: Role, reload: boolean = false): void {
    const unpromotedRole = handRoles(this.rules).includes(r) ? r : unpromote(this.rules)(r);
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
