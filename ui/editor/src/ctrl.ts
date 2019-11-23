import { EditorConfig, EditorOptions, EditorState, Selected, Redraw, OpeningPosition, CastlingToggle, CastlingToggles, CASTLING_TOGGLES } from './interfaces';
import { Api as CgApi } from 'chessground/api';
import { Rules, Square } from 'chessops/types';
import { SquareSet } from 'chessops/squareSet';
import { Board } from 'chessops/board';
import { Setup, Material, RemainingChecks } from 'chessops/setup';
import { Castles, setupPosition } from 'chessops/variant';
import { makeFen, parseFen, parseCastlingFen, INITIAL_FEN, EMPTY_FEN } from 'chessops/fen';
import { defined, prop, Prop } from 'common';

export default class EditorCtrl {
  cfg: EditorConfig;
  options: EditorOptions;
  trans: Trans;
  extraPositions: OpeningPosition[];
  chessground: CgApi | undefined;
  positionIndex: { [boardFen: string]: number };
  redraw: Redraw;

  selected: Prop<Selected>;

  pockets: Material | undefined;
  turn: Color;
  unmovedRooks: SquareSet | undefined;
  castlingToggles: CastlingToggles<boolean>;
  epSquare: Square | undefined;
  remainingChecks: RemainingChecks | undefined;
  rules: Rules;

  constructor(cfg: EditorConfig, redraw: Redraw) {
    this.cfg = cfg;
    this.options = cfg.options || {};

    this.trans = window.lichess.trans(this.cfg.i18n);

    this.selected = prop('pointer');

    this.extraPositions = [{
      fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -',
      name: this.trans('startPosition')
    }, {
      fen: '8/8/8/8/8/8/8/8 w - -',
      name: this.trans('clearBoard')
    }, {
      fen: 'prompt',
      name: this.trans('loadPosition')
    }];

    this.positionIndex = {};
    if (cfg.positions) cfg.positions.forEach((p, i) => {
      this.positionIndex[p.fen.split(' ')[0]] = i;
    });

    window.Mousetrap.bind('f', (e: Event) => {
      e.preventDefault();
      if (this.chessground) this.chessground.toggleOrientation();
      redraw();
    });


    this.castlingToggles = { K: false, Q: false, k: false, q: false };
    this.rules = 'chess';

    this.redraw = () => {};
    this.setFen(cfg.fen);
    this.redraw = redraw;
  }

  onChange(): void {
    this.options.onChange && this.options.onChange(this.getLegalFen());
    this.redraw();
  }

  private castlingToggleFen(): string {
    let fen = '';
    for (const toggle of CASTLING_TOGGLES) {
      if (this.castlingToggles[toggle]) fen += toggle;
    }
    return fen;
  }

  private getSetup(): Setup {
    const boardFen = this.chessground ? this.chessground.getFen() : this.cfg.fen;
    const board = parseFen(boardFen).unwrap(setup => setup.board, _ => Board.empty());
    return {
      board,
      pockets: this.pockets,
      turn: this.turn,
      unmovedRooks: this.unmovedRooks || parseCastlingFen(board, this.castlingToggleFen()).unwrap(),
      epSquare: this.epSquare,
      remainingChecks: this.remainingChecks,
      halfmoves: 0,
      fullmoves: 1,
    };
  }

  getFen(): string {
    return makeFen(this.getSetup(), {promoted: this.rules == 'crazyhouse'});
  }

  getLegalFen(): string | undefined {
    return setupPosition(this.rules, this.getSetup()).unwrap(pos => {
      return makeFen(pos.toSetup(), {promoted: pos.rules == 'crazyhouse'});
    }, _ => undefined);
  }

  isPlayable(): boolean {
    return setupPosition(this.rules, this.getSetup()).unwrap(pos => !pos.isEnd(), _ => false);
  }

  getState(): EditorState {
    return {
      fen: this.getFen(),
      legalFen: this.getLegalFen(),
      playable: this.isPlayable(),
    };
  }

  makeAnalysisUrl(legalFen: string): string {
    return this.makeUrl('', legalFen); // TODO
  }

  makeUrl(baseUrl: string, fen: string): string {
    return baseUrl + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  bottomColor(): Color {
    return this.chessground ?
    this.chessground.state.orientation :
    this.options.orientation || 'white';
  }

  setCastlingToggle(id: CastlingToggle, value: boolean): void {
    if (this.castlingToggles[id] != value) this.unmovedRooks = undefined;
    this.castlingToggles[id] = value;
    this.onChange();
  }

  setTurn(turn: Color): void {
    this.turn = turn;
    this.onChange();
  }

  startPosition(): void {
    this.setFen(INITIAL_FEN);
  }

  clearBoard(): void {
    this.setFen(EMPTY_FEN);
  }

  loadNewFen(fen: string | 'prompt'): void {
    if (fen === 'prompt') {
      fen = (prompt('Paste FEN position') || '').trim();
      if (!fen) return;
    }
    this.setFen(fen);
  }

  setFen(fen: string): boolean {
    return parseFen(fen).unwrap(setup => {
      if (this.chessground) this.chessground.set({fen});
      this.pockets = setup.pockets;
      this.turn = setup.turn;
      this.unmovedRooks = setup.unmovedRooks;
      this.epSquare = setup.epSquare;
      this.remainingChecks = setup.remainingChecks;

      const castles = Castles.fromSetup(setup);
      this.castlingToggles['K'] = defined(castles.rook.white.h);
      this.castlingToggles['Q'] = defined(castles.rook.white.a);
      this.castlingToggles['k'] = defined(castles.rook.white.h);
      this.castlingToggles['q'] = defined(castles.rook.white.a);

      this.onChange();
      return true;
    }, _ => false);
  }

  setRules(rules: Rules): void {
    this.rules = rules;
    if (rules != 'crazyhouse') this.pockets = undefined;
    else if (!this.pockets) this.pockets = Material.empty();
    if (rules != '3check') this.remainingChecks = undefined;
    else if (!this.remainingChecks) this.remainingChecks = RemainingChecks.default();
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.chessground!.state.orientation !== o) this.chessground!.toggleOrientation();
    this.redraw();
  }
}
