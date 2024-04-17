import {
  EditorState,
  Selected,
  Redraw,
  CastlingToggle,
  CastlingToggles,
  CASTLING_TOGGLES,
} from './interfaces';
import { Api as CgApi } from 'chessground/api';
import { Rules, Square } from 'chessops/types';
import { SquareSet } from 'chessops/squareSet';
import { Board } from 'chessops/board';
import { Setup, Material, RemainingChecks, defaultSetup } from 'chessops/setup';
import { Castles, defaultPosition, setupPosition } from 'chessops/variant';
import { makeFen, parseFen, parseCastlingFen, INITIAL_FEN, EMPTY_FEN } from 'chessops/fen';
import { lichessVariant, lichessRules } from 'chessops/compat';
import { defined, prop, Prop } from 'common';

export default class EditorCtrl {
  options: Editor.Options;
  trans: Trans;
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

  constructor(
    readonly cfg: Editor.Config,
    readonly redraw: Redraw,
  ) {
    this.options = cfg.options || {};

    this.trans = site.trans(this.cfg.i18n);

    this.selected = prop('pointer');

    if (cfg.positions) cfg.positions.forEach(p => (p.epd = p.fen.split(' ').splice(0, 4).join(' ')));

    if (cfg.endgamePositions)
      cfg.endgamePositions.forEach(p => (p.epd = p.fen.split(' ').splice(0, 4).join(' ')));

    site.mousetrap.bind('f', () => {
      if (this.chessground) this.chessground.toggleOrientation();
      this.onChange();
    });

    this.castlingToggles = { K: false, Q: false, k: false, q: false };
    const params = new URLSearchParams(location.search);
    this.rules = this.cfg.embed ? 'chess' : lichessRules((params.get('variant') || 'standard') as VariantKey);
    this.initialFen = (cfg.fen || params.get('fen') || INITIAL_FEN).replace(/_/g, ' ');

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
      playable: this.rules == 'chess' && this.isPlayable(),
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
    if (this.castlingToggles[id] != value) this.castlingRights = undefined;
    this.castlingToggles[id] = value;
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

  startPosition = () => this.setFen(makeFen(defaultPosition(this.rules).toSetup()));

  clearBoard = () => this.setFen(EMPTY_FEN);

  loadNewFen(fen: string | 'prompt'): void {
    if (fen === 'prompt') {
      fen = (prompt('Paste FEN position') || '').trim();
      if (!fen) return;
    }
    this.setFen(fen);
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
    this.castlingToggles['K'] = defined(castles.rook.white.h);
    this.castlingToggles['Q'] = defined(castles.rook.white.a);
    this.castlingToggles['k'] = defined(castles.rook.black.h);
    this.castlingToggles['q'] = defined(castles.rook.black.a);
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

function urlFen(fen: string): string {
  return encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}
