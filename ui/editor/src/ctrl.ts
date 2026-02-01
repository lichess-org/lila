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
import { type Result } from '@badrap/result';
import type { Api as CgApi } from '@lichess-org/chessground/api';
import type { Rules, Square } from 'chessops/types';
import { Board } from 'chessops/board';
import { type Setup, Material, RemainingChecks, defaultSetup } from 'chessops/setup';
import { Castles, defaultPosition, Position, PositionError, setupPosition } from 'chessops/variant';
import { makeFen, parseFen, parseCastlingFen, INITIAL_FEN, EMPTY_FEN } from 'chessops/fen';
import { lichessRules } from 'chessops/compat';
import { defined, prop, type Prop } from 'lib';
import { prompt } from 'lib/view';
import { opposite } from '@lichess-org/chessground/util';
import { parseSquare } from 'chessops';
import { chess960CastlingSquares, chess960IdToFEN, fenToChess960Id, randomPositionId } from './chess960';

export default class EditorCtrl {
  options: Options;
  chessground: CgApi | undefined;

  selected: Prop<Selected>;

  initialFen: FEN;
  pockets: Material | undefined;
  turn: Color;
  castlingToggles: CastlingToggles<boolean>;
  enabledCastlingToggles: CastlingToggles<boolean>;
  epSquare: Square | undefined;
  remainingChecks: RemainingChecks | undefined;
  variant: VariantKey = 'standard';
  halfmoves: number;
  fullmoves: number;
  guessCastlingToggles: boolean;
  chess960PositionId: number | undefined;

  constructor(
    readonly cfg: Config,
    readonly redraw: Redraw,
  ) {
    this.options = cfg.options || {};

    this.selected = prop('pointer');

    [...(cfg.positions || []), ...(cfg.endgamePositions || [])].forEach(
      p => (p.epd = p.fen.split(' ').slice(0, 4).join(' ')),
    );

    if (this.options.bindHotkeys !== false)
      site.mousetrap.bind('f', () => {
        if (this.chessground) {
          this.chessground.toggleOrientation();
          if (this.options.orientation) this.setOrientation(opposite(this.options.orientation));
        }
        this.onChange();
      });

    this.castlingToggles = { K: false, Q: false, k: false, q: false };
    this.enabledCastlingToggles = { K: false, Q: false, k: false, q: false };
    const params = new URLSearchParams(location.search);
    this.variant = this.cfg.embed ? 'standard' : ((params.get('variant') || 'standard') as VariantKey);
    const fenPassedIn: FEN | null = cfg.fen || params.get('fen');
    this.initialFen = (fenPassedIn || INITIAL_FEN).replace(/_/g, ' ');
    this.guessCastlingToggles = false;
    this.chess960PositionId = fenPassedIn
      ? fenToChess960Id(fenPassedIn)
      : params.get('position') !== null
        ? parseInt(params.get('position')!, 10)
        : randomPositionId();

    if (!this.cfg.embed) this.options.orientation = params.get('color') === 'black' ? 'black' : 'white';

    parseFen(this.initialFen).unwrap(this.setSetup, _ => {
      this.initialFen = INITIAL_FEN;
      this.setSetup(defaultSetup());
    });
  }

  private indexOfNthOccurrence = (haystack: string, needle: string, n: number): number => {
    let index = haystack.indexOf(needle);
    for (; n > 1 && index !== -1; n--) index = haystack.indexOf(needle, index + needle.length);
    return index;
  };

  // Ideally to be replaced when something like parseCastlingFen exists in chessops but for epSquare (@getSetup)
  private fenFixedEp(fen: FEN) {
    let enPassant = fen.split(' ')[3];
    if (enPassant !== '-' && !this.getEnPassantOptions(fen).includes(enPassant)) {
      this.epSquare = undefined;
      enPassant = '-';
    }
    const epIndex = this.indexOfNthOccurrence(fen, ' ', 3) + 1;
    const epEndIndex = fen.indexOf(' ', epIndex);
    return `${fen.substring(0, epIndex)}${enPassant}${fen.substring(epEndIndex)}`;
  }

  onChange(): void {
    this.enabledCastlingToggles = this.computeCastlingToggles();
    if (this.guessCastlingToggles) {
      this.castlingToggles = this.enabledCastlingToggles;
    }

    const fen = this.fenFixedEp(this.getFen());
    if (!this.cfg.embed) {
      window.history.replaceState(null, '', this.makeEditorUrl(fen, this.bottomColor()));
    }
    this.options.onChange?.(fen);
    this.chess960PositionId = fenToChess960Id(fen) ?? this.chess960PositionId;
    this.redraw();
  }

  private castlingToggleFen(): string {
    return CASTLING_TOGGLES.filter(toggle => this.enabledCastlingToggles[toggle] && this.castlingToggles[toggle]).join('');
  }

  private computeCastlingToggles(): CastlingToggles<boolean> {
    const chess960Castling = chess960CastlingSquares(this.chess960PositionId);
    const board = this.getSetup().board,
      whiteKingOnE1 = board.king.intersect(board.white).has(parseSquare(chess960Castling.white.king)!),
      blackKingOnE8 = board.king.intersect(board.black).has(parseSquare(chess960Castling.black.king)!),
      whiteRooks = board.rook.intersect(board.white),
      blackRooks = board.rook.intersect(board.black);
    return {
      K: whiteKingOnE1 && whiteRooks.has(parseSquare(chess960Castling.white.rookK)!),
      Q: whiteKingOnE1 && whiteRooks.has(parseSquare(chess960Castling.white.rookQ)!),
      k: blackKingOnE8 && blackRooks.has(parseSquare(chess960Castling.black.rookK)!),
      q: blackKingOnE8 && blackRooks.has(parseSquare(chess960Castling.black.rookQ)!),
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
      castlingRights: parseCastlingFen(board, this.castlingToggleFen()).unwrap(),
      epSquare: this.epSquare,
      remainingChecks: this.remainingChecks,
      halfmoves: this.halfmoves,
      fullmoves: this.fullmoves,
    };
  }

  private getRules(): Rules {
    return lichessRules(this.variant);
  }

  getFen(): FEN {
    return makeFen(this.getSetup());
  }

  getPosition(): Result<Position, PositionError> {
    return setupPosition(this.getRules(), this.getSetup());
  }

  private getLegalFen(): FEN | undefined {
    return this.getPosition().unwrap(
      pos => makeFen(pos.toSetup()),
      _ => undefined,
    );
  }

  private isPlayable(): boolean {
    return this.getPosition().unwrap(
      pos => !pos.isEnd(),
      _ => false,
    );
  }

  // hopefully moved to chessops soon
  // https://github.com/niklasf/chessops/issues/154
  private getEnPassantOptions(fen: FEN): string[] {
    const unpackRank = (packedRank: string) =>
      [...packedRank].reduce((accumulator, current) => {
        const parsedInt = parseInt(current);
        return accumulator + (parsedInt >= 1 ? 'x'.repeat(parsedInt) : current);
      }, '');
    const checkRank = (rank: string, regex: RegExp, offset: number, filesEnPassant: Set<number>) => {
      let match: RegExpExecArray | null;
      while ((match = regex.exec(rank)) !== null) {
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
      playable: ['standard', 'chess960', 'fromPosition'].includes(this.variant) && this.isPlayable(),
      enPassantOptions: legalFen ? this.getEnPassantOptions(legalFen) : [],
    };
  }

  makeAnalysisUrl(legalFen: FEN, orientation: Color = 'white'): string {
    const variant = this.variant === 'standard' ? '' : this.variant + '/';
    const chess960PositionId =
      this.chess960PositionId === undefined ? '' : `&position=${this.chess960PositionId}`;
    return `/analysis/${variant}${urlFen(legalFen)}?color=${orientation}${chess960PositionId}`;
  }

  makeEditorUrl(fen: FEN, orientation: Color = 'white'): string {
    if (fen === INITIAL_FEN && this.variant === 'standard' && orientation === 'white')
      return this.cfg.baseUrl;
    const variant = this.variant === 'standard' ? '' : '?variant=' + this.variant;
    const chess960PositionId =
      this.chess960PositionId === undefined ? '' : `&position=${this.chess960PositionId}`;
    const orientationParam = variant ? `&color=${orientation}` : `?color=${orientation}`;
    return `${this.cfg.baseUrl}/${urlFen(fen)}${variant}${orientationParam}${chess960PositionId}`;
  }

  makeImageUrl = (fen: FEN): string =>
    `${site.asset.baseUrl()}/export/fen.gif?fen=${urlFen(fen)}&color=${this.bottomColor()}`;

  bottomColor = (): Color =>
    this.chessground ? this.chessground.state.orientation : this.options.orientation || 'white';

  setCastlingToggle(id: CastlingToggle, value: boolean): void {
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

  startPosition = (): boolean =>
    this.setFen(
      this.variant === 'chess960' && this.chess960PositionId !== undefined
        ? chess960IdToFEN(this.chess960PositionId)
        : makeFen(defaultPosition(this.getRules()).toSetup()),
    );

  clearBoard = (): boolean => {
    this.guessCastlingToggles = this.variant !== 'antichess';
    const parts = EMPTY_FEN.split(' ');
    parts[1] = this.turn[0];
    return this.setFen(parts.join(' '));
  };

  loadNewFen(fen: FEN | 'prompt'): void {
    if (fen === 'prompt') prompt('Paste FEN position').then(fen => fen && this.setFen(fen.trim()));
    else this.setFen(fen);
  }

  private setSetup = (setup: Setup): void => {
    this.pockets = setup.pockets;
    this.turn = setup.turn;
    this.epSquare = setup.epSquare;
    this.remainingChecks = setup.remainingChecks;
    this.halfmoves = setup.halfmoves;
    this.fullmoves = setup.fullmoves;

    const castles = Castles.fromSetup(setup);
    this.castlingToggles['Q'] = defined(castles.rook.white.a) || setup.castlingRights.has(0);
    this.castlingToggles['K'] = defined(castles.rook.white.h) || setup.castlingRights.has(7);
    this.castlingToggles['q'] = defined(castles.rook.black.a) || setup.castlingRights.has(56);
    this.castlingToggles['k'] = defined(castles.rook.black.h) || setup.castlingRights.has(63);

    this.enabledCastlingToggles = this.computeCastlingToggles();
  };

  setFen = (fen: FEN): boolean =>
    parseFen(fen).unwrap(
      setup => {
        if (this.chessground) this.chessground.set({ fen });
        this.setSetup(setup);
        this.onChange();
        return true;
      },
      _ => false,
    );

  setVariant(variant: VariantKey): void {
    this.variant = variant;
    if (variant === 'crazyhouse') this.pockets ||= Material.empty();
    else this.pockets = undefined;
    if (variant === 'threeCheck') this.remainingChecks ||= RemainingChecks.default();
    else this.remainingChecks = undefined;
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.chessground!.state.orientation !== o) this.chessground!.toggleOrientation();
    this.redraw();
  }

  set960Position(positionId: number): void {
    this.chess960PositionId = positionId;
    this.setFen(chess960IdToFEN(positionId));
  }

  setRandom960Position(): void {
    const id = randomPositionId();
    id !== this.chess960PositionId ? this.set960Position(id) : this.setRandom960Position();
  }
}

function urlFen(fen: FEN): string {
  return encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}
