import {
  type EditorState,
  type Selected,
  type Redraw,
  type Options,
  type Config,
  type CastlingToggle,
  type CastlingToggles,
  CASTLING_TOGGLES,
} from './interfaces';
import type { Api as CgApi } from '@lichess-org/chessground/api';
import type { Rules, Square } from 'chessops/types';
import type { SquareSet } from 'chessops/squareSet';
import { Board } from 'chessops/board';
import { type Setup, Material, RemainingChecks, defaultSetup } from 'chessops/setup';
import { Castles, defaultPosition, setupPosition } from 'chessops/variant';
import { makeFen, parseFen, parseCastlingFen, INITIAL_FEN, EMPTY_FEN } from 'chessops/fen';
import { lichessVariant, lichessRules } from 'chessops/compat';
import { defined, prop, type Prop } from 'lib';
import { prompt } from 'lib/view/dialogs';
import { opposite } from '@lichess-org/chessground/util';
import { parseSquare } from 'chessops';

export default class EditorCtrl {
  options: Options;
  chessground: CgApi | undefined;

  selected: Prop<Selected>;

  initialFen: string;
  pockets: Material | undefined;
  turn: Color;
  castlingRights: SquareSet | undefined;
  castlingToggles: CastlingToggles<boolean>;
  epSquare: Square | undefined;
  remainingChecks: RemainingChecks | undefined;
  rules: Rules;
  halfmoves: number;
  fullmoves: number;
  guessCastlingToggles: boolean;

  constructor(
    readonly cfg: Config,
    readonly redraw: Redraw,
  ) {
    this.options = cfg.options || {};

    this.selected = prop('pointer');

    if (cfg.positions) cfg.positions.forEach(p => (p.epd = p.fen.split(' ').splice(0, 4).join(' ')));

    if (cfg.endgamePositions)
      cfg.endgamePositions.forEach(p => (p.epd = p.fen.split(' ').splice(0, 4).join(' ')));

    if (this.options.bindHotkeys !== false)
      site.mousetrap.bind('f', () => {
        if (this.chessground) {
          this.chessground.toggleOrientation();
          if (this.options.orientation) this.setOrientation(opposite(this.options.orientation));
        }
        this.onChange();
      });

    this.castlingToggles = { K: false, Q: false, k: false, q: false };
    const params = new URLSearchParams(location.search);
    this.rules = this.cfg.embed ? 'chess' : lichessRules((params.get('variant') || 'standard') as VariantKey);
    this.initialFen = (cfg.fen || params.get('fen') || INITIAL_FEN).replace(/_/g, ' ');
    this.guessCastlingToggles = false;

    if (!this.cfg.embed) this.options.orientation = params.get('color') === 'black' ? 'black' : 'white';

    parseFen(this.initialFen).unwrap(this.setSetup, _ => {
      this.initialFen = INITIAL_FEN;
      this.setSetup(defaultSetup());
    });
  }

  private nthIndexOf = (haystack: string, needle: string, n: number): number => {
    let index = haystack.indexOf(needle);
    while (n-- > 0) {
      if (index === -1) break;
      index = haystack.indexOf(needle, index + needle.length);
    }
    return index;
  };

  // Ideally to be replaced when something like parseCastlingFen exists in chessops but for epSquare (@getSetup)
  private fenFixedEp(fen: string) {
    let enPassant = fen.split(' ')[3];
    if (enPassant !== '-' && !this.getEnPassantOptions(fen).includes(enPassant)) {
      this.epSquare = undefined;
      enPassant = '-';
    }

    const epIndex = this.nthIndexOf(fen, ' ', 2) + 1;
    const epEndIndex = fen.indexOf(' ', epIndex);
    return `${fen.substring(0, epIndex)}${enPassant}${fen.substring(epEndIndex)}`;
  }

  onChange(): void {
    if (this.guessCastlingToggles) {
      this.castlingToggles = this.computeCastlingToggles();
      this.castlingRights = undefined;
    }
    const fen = this.fenFixedEp(this.getFen());
    if (!this.cfg.embed) {
      window.history.replaceState(null, '', this.makeEditorUrl(fen, this.bottomColor()));
    }
    this.options.onChange?.(fen);
    this.redraw();
  }

  private castlingToggleFen(): string {
    let fen = '';
    for (const toggle of CASTLING_TOGGLES) {
      if (this.castlingToggles[toggle]) fen += toggle;
    }
    return fen;
  }

  private computeCastlingToggles(): CastlingToggles<boolean> {
    const board = this.getSetup().board,
      whiteKingOnE1 = board.king.intersect(board.white).has(parseSquare('e1')),
      blackKingOnE8 = board.king.intersect(board.black).has(parseSquare('e8')),
      whiteRooks = board.rook.intersect(board.white),
      blackRooks = board.rook.intersect(board.black);
    return {
      K: whiteKingOnE1 && whiteRooks.has(parseSquare('h1')),
      Q: whiteKingOnE1 && whiteRooks.has(parseSquare('a1')),
      k: blackKingOnE8 && blackRooks.has(parseSquare('h8')),
      q: blackKingOnE8 && blackRooks.has(parseSquare('a8')),
    };
  }

  private getSetup(): Setup {
    const boardFen = this.chessground?.getFen() || this.initialFen;
    const board = parseFen(boardFen).unwrap(
      setup => setup.board,
      _ => Board.empty(),
    );
    return {
      board,
      pockets: this.pockets,
      turn: this.turn,
      castlingRights: this.castlingRights || parseCastlingFen(board, this.castlingToggleFen()).unwrap(),
      epSquare: this.epSquare,
      remainingChecks: this.remainingChecks,
      halfmoves: this.halfmoves,
      fullmoves: this.fullmoves,
    };
  }

  getFen(): string {
    return makeFen(this.getSetup());
  }

  private getLegalFen(): string | undefined {
    return setupPosition(this.rules, this.getSetup()).unwrap(
      pos => makeFen(pos.toSetup()),
      _ => undefined,
    );
  }

  private isPlayable(): boolean {
    return setupPosition(this.rules, this.getSetup()).unwrap(
      pos => !pos.isEnd(),
      _ => false,
    );
  }

  // hopefully moved to chessops soon
  // https://github.com/niklasf/chessops/issues/154
  private getEnPassantOptions(fen: string): string[] {
    const unpackRank = (packedRank: string) =>
      [...packedRank].reduce((accumulator, current) => {
        const parsedInt = parseInt(current);
        return accumulator + (parsedInt >= 1 ? 'x'.repeat(parsedInt) : current);
      }, '');
    const checkRank = (rank: string, regex: RegExp, offset: number, filesEnPassant: Set<number>) => {
      let match: RegExpExecArray | null;
      while ((match = regex.exec(rank)) != null) {
        filesEnPassant.add(match.index + offset);
      }
    };
    const filesEnPassant: Set<number> = new Set();
    const [positions, turn] = fen.split(' ');
    const ranks = positions.split('/');
    const unpackedRank = unpackRank(ranks[turn === 'w' ? 3 : 4]);
    checkRank(unpackedRank, /pP/g, turn === 'w' ? 0 : 1, filesEnPassant);
    checkRank(unpackedRank, /Pp/g, turn === 'w' ? 1 : 0, filesEnPassant);
    const [rank1, rank2] =
      filesEnPassant.size >= 1
        ? [unpackRank(ranks[turn === 'w' ? 1 : 6]), unpackRank(ranks[turn === 'w' ? 2 : 5])]
        : [null, null];
    return Array.from(filesEnPassant)
      .filter(e => rank1![e] === 'x' && rank2![e] === 'x')
      .map(e => String.fromCharCode('a'.charCodeAt(0) + e) + (turn === 'w' ? '6' : '3'));
  }

  getState(): EditorState {
    const legalFen = this.getLegalFen();
    return {
      fen: this.getFen(),
      legalFen: legalFen,
      playable: this.rules === 'chess' && this.isPlayable(),
      enPassantOptions: legalFen ? this.getEnPassantOptions(legalFen) : [],
    };
  }

  makeAnalysisUrl(legalFen: string, orientation: Color = 'white'): string {
    const variant = this.rules === 'chess' ? '' : lichessVariant(this.rules) + '/';
    return `/analysis/${variant}${urlFen(legalFen)}?color=${orientation}`;
  }

  makeEditorUrl(fen: string, orientation: Color = 'white'): string {
    if (fen === INITIAL_FEN && this.rules === 'chess' && orientation === 'white') return this.cfg.baseUrl;
    const variant = this.rules === 'chess' ? '' : '?variant=' + lichessVariant(this.rules);
    const orientationParam = variant ? `&color=${orientation}` : `?color=${orientation}`;
    return `${this.cfg.baseUrl}/${urlFen(fen)}${variant}${orientationParam}`;
  }

  makeImageUrl = (fen: string): string =>
    `${site.asset.baseUrl()}/export/fen.gif?fen=${urlFen(fen)}&color=${this.bottomColor()}`;

  bottomColor = (): Color =>
    this.chessground ? this.chessground.state.orientation : this.options.orientation || 'white';

  setCastlingToggle(id: CastlingToggle, value: boolean): void {
    if (this.castlingToggles[id] !== value) this.castlingRights = undefined;
    this.castlingToggles[id] = value;
    this.guessCastlingToggles = false;
    this.onChange();
  }

  setTurn(turn: Color): void {
    this.turn = turn;
    this.epSquare = undefined;
    this.onChange();
  }

  setEnPassant(epSquare: Square | undefined): void {
    this.epSquare = epSquare;
    this.onChange();
  }

  startPosition = (): boolean => this.setFen(makeFen(defaultPosition(this.rules).toSetup()));

  clearBoard = (): boolean => {
    this.guessCastlingToggles = this.rules !== 'antichess';
    const parts = EMPTY_FEN.split(' ');
    parts[1] = this.turn[0];
    return this.setFen(parts.join(' '));
  };

  loadNewFen(fen: string | 'prompt'): void {
    if (fen === 'prompt') prompt('Paste FEN position').then(fen => fen && this.setFen(fen.trim()));
    else this.setFen(fen);
  }

  private setSetup = (setup: Setup): void => {
    this.pockets = setup.pockets;
    this.turn = setup.turn;
    this.castlingRights = setup.castlingRights;
    this.epSquare = setup.epSquare;
    this.remainingChecks = setup.remainingChecks;
    this.halfmoves = setup.halfmoves;
    this.fullmoves = setup.fullmoves;

    const castles = Castles.fromSetup(setup);
    this.castlingToggles['Q'] = defined(castles.rook.white.a) || this.castlingRights.has(0);
    this.castlingToggles['K'] = defined(castles.rook.white.h) || this.castlingRights.has(7);
    this.castlingToggles['q'] = defined(castles.rook.black.a) || this.castlingRights.has(56);
    this.castlingToggles['k'] = defined(castles.rook.black.h) || this.castlingRights.has(63);
  };

  setFen = (fen: string): boolean =>
    parseFen(fen).unwrap(
      setup => {
        if (this.chessground) this.chessground.set({ fen });
        this.setSetup(setup);
        this.onChange();
        return true;
      },
      _ => false,
    );

  setRules(rules: Rules): void {
    this.rules = rules;
    if (rules !== 'crazyhouse') this.pockets = undefined;
    else if (!this.pockets) this.pockets = Material.empty();
    if (rules !== '3check') this.remainingChecks = undefined;
    else if (!this.remainingChecks) this.remainingChecks = RemainingChecks.default();
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.chessground!.state.orientation !== o) this.chessground!.toggleOrientation();
    this.redraw();
  }
}

function urlFen(fen: string): string {
  return encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}
