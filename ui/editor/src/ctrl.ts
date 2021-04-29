import { EditorConfig, EditorOptions, EditorState, Selected, Redraw, OpeningPosition } from './interfaces';
import { Api as CgApi } from 'shogiground/api';
import { Role } from 'shogiground/types';
import { Rules, PocketRole, PROMOTABLE_ROLES, POCKET_ROLES, PromotableRole } from 'shogiops/types';
import { Board } from 'shogiops/board';
import { Setup, Material, MaterialSide } from 'shogiops/setup';
import { setupPosition } from 'shogiops/variant';
import { makeFen, parseFen, INITIAL_FEN, INITIAL_EPD, EMPTY_FEN } from 'shogiops/fen';

import { defined, prop, Prop } from 'common';
import { promote, unpromote } from 'shogiops/util';
import { opposite } from 'shogiground/util';

export default class EditorCtrl {
  cfg: EditorConfig;
  options: EditorOptions;
  trans: Trans;
  extraPositions: OpeningPosition[];
  shogiground: CgApi | undefined;
  redraw: Redraw;

  selected: Prop<Selected>;

  pockets: Material;
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
        fen: INITIAL_FEN,
        epd: INITIAL_EPD,
        name: this.trans('startPosition'),
      },
      {
        fen: 'prompt',
        name: this.trans('loadPosition'),
      },
    ];

    if (cfg.positions) {
      cfg.positions.forEach(p => (p.epd = p.fen.split(' ').splice(0, 4).join(' ')));
    }

    window.Mousetrap.bind('f', (e: Event) => {
      e.preventDefault();
      if (this.shogiground) this.setOrientation(opposite(this.shogiground.state.orientation));
    });
    this.rules =
      !this.cfg.embed && window.history.state && window.history.state.rules ? window.history.state.rules : 'shogi';

    this.redraw = () => {};
    this.setFen(cfg.fen);
    this.redraw = redraw;
  }

  onChange(): void {
    const fen = this.getFen();
    this.cfg.fen = fen;
    if (!this.cfg.embed) {
      if (fen == INITIAL_FEN) window.history.replaceState('', '', '/editor');
      else window.history.replaceState('', '', this.makeUrl('/editor/', fen));
    }
    this.options.onChange && this.options.onChange(fen);
    this.redraw();
  }

  private getSetup(): Setup {
    const boardFen = this.shogiground ? this.shogiground.getFen() : this.cfg.fen;
    const board = parseFen(boardFen).unwrap(
      setup => setup.board,
      _ => Board.empty()
    );
    return {
      board,
      pockets: this.pockets,
      turn: this.turn,
      fullmoves: this.fullmoves,
    };
  }

  getFen(): string {
    return makeFen(this.getSetup());
  }

  private getLegalFen(): string | undefined {
    return setupPosition(this.rules, this.getSetup()).unwrap(
      pos => {
        return makeFen(pos.toSetup());
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
      fen: this.getFen(),
      legalFen: this.getLegalFen(),
      playable: this.rules == 'shogi' && this.isPlayable(),
    };
  }

  makeAnalysisUrl(legalFen: string): string {
    return this.makeUrl(`/analysis/`, this.encodeFen(legalFen).replace(/%2B/g, '+'));
  }

  encodeFen(fen: string): string {
    return encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  makeUrl(baseUrl: string, fen: string): string {
    return baseUrl + this.encodeFen(fen).replace(/%2B/g, '+');
  }

  bottomColor(): Color {
    return this.shogiground ? this.shogiground.state.orientation : this.options.orientation || 'sente';
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
      fen = (prompt('Paste SFEN position') || '').trim();
      if (!fen) return;
    }
    this.setFen(fen);
  }

  setFen(fen: string): boolean {
    return parseFen(fen).unwrap(
      setup => {
        if (this.shogiground) this.shogiground.set({ fen });
        this.pockets = setup.pockets;
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
      setup.board.occupied.size() + setup.pockets.count() < 38 + this.countPieces('king', setup)
    );
  }

  countPieces(role: Role, setup?: Setup): number {
    if (!defined(setup)) setup = this.getSetup();
    role = unpromote(role)!;
    return (
      setup.board[role].size() +
      ((POCKET_ROLES as ReadonlyArray<string>).includes(role)
        ? setup.pockets.sente[role as PocketRole] + setup.pockets.gote[role as PocketRole]
        : 0) +
      ((PROMOTABLE_ROLES as ReadonlyArray<string>).includes(role)
        ? setup.board[promote(role as PromotableRole)].size()
        : 0)
    );
  }

  fillGotesHand(): void {
    const setup = this.getSetup();
    const senteHand = this.pockets['sente'];

    const pieceCounts: { [index: string]: number } = {
      lance: 4 - setup.board.lance.size() - setup.board.promotedlance.size() - senteHand.lance,
      knight: 4 - setup.board.knight.size() - setup.board.promotedknight.size() - senteHand.knight,
      silver: 4 - setup.board.silver.size() - setup.board.promotedsilver.size() - senteHand.silver,
      gold: 4 - setup.board.gold.size() - senteHand.gold,
      pawn: 18 - setup.board.pawn.size() - setup.board.tokin.size() - senteHand.pawn,
      bishop: 2 - setup.board.bishop.size() - setup.board.horse.size() - senteHand.bishop,
      rook: 2 - setup.board.rook.size() - setup.board.dragon.size() - senteHand.rook,
    };
    this.pockets['gote'] = MaterialSide.empty();

    for (const p in pieceCounts) {
      if (pieceCounts[p] > 0) this.pockets['gote'][p as PocketRole] = pieceCounts[p];
    }
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.shogiground!.state.orientation !== o) this.shogiground!.toggleOrientation();
    this.redraw();
  }

  addToPocket(c: Color, r: Role, reload: boolean = false): void {
    if (['pawn', 'lance', 'knight', 'silver', 'gold', 'bishop', 'rook'].includes(r)) this.pockets[c][r as PocketRole]++;
    if (reload) this.onChange();
  }
  removeFromPocket(c: Color, r: Role, reload: boolean = false): void {
    if (
      ['pawn', 'lance', 'knight', 'silver', 'gold', 'bishop', 'rook'].includes(r) &&
      this.pockets[c][r as PocketRole] > 0
    )
      this.pockets[c][r as PocketRole]--;
    if (reload) this.onChange();
  }
  clearPocket() {
    this.pockets = Material.empty();
  }
  clearColorPocket(c: Color) {
    this.pockets[c] = MaterialSide.empty();
  }
}
