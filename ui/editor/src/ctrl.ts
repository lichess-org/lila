import {
  EditorConfig,
  EditorOptions,
  EditorState,
  Selected,
  Redraw,
  OpeningPosition,
} from "./interfaces";
import { Api as CgApi } from "shogiground/api";
import {Role} from "shogiground/types";
import { Rules, PocketRole } from 'shogiops/types';
import { Board } from 'shogiops/board';
import { Setup, Material, MaterialSide } from 'shogiops/setup';
import { setupPosition } from 'shogiops/variant';
import { Shogi } from 'shogiops/shogi';
import { makeFen, parseFen, INITIAL_FEN, INITIAL_EPD, EMPTY_FEN } from 'shogiops/fen';
import { lishogiBoardToShogiBoard, makeShogiFen, makeLishogiFen } from "shogiops/compat";

import { prop, Prop } from "common";

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
    this.cfg.fen = makeShogiFen(cfg.fen);
    this.options = cfg.options || {};

    this.trans = window.lishogi.trans(this.cfg.i18n);

    this.selected = prop("pointer");
    this.extraPositions = [
      {
        fen: makeLishogiFen(INITIAL_FEN),
        epd: makeLishogiFen(INITIAL_EPD),
        name: this.trans("startPosition"),
      },
      {
        fen: "prompt",
        name: this.trans("loadPosition"),
      },
    ];

    if (cfg.positions) {
      cfg.positions.forEach(
        (p) => (p.epd = p.fen.split(" ").splice(0, 4).join(" "))
      );
    }

    window.Mousetrap.bind("f", (e: Event) => {
      e.preventDefault();
      if (this.shogiground) this.shogiground.toggleOrientation();
      redraw();
    });
    this.rules = (!this.cfg.embed && window.history.state && window.history.state.rules) ? window.history.state.rules : 'shogi';

    this.redraw = () => {};
    this.setFen(cfg.fen);
    this.redraw = redraw;
  }

  onChange(): void {
    const fen = this.getFen();
    if (!this.cfg.embed) {
      if (fen == INITIAL_FEN) window.history.replaceState("", "", "/editor");
      else window.history.replaceState("", "", this.makeUrl("/editor/", fen));
    }
    this.options.onChange && this.options.onChange(fen);
    this.redraw();
  }

  private getSetup(): Setup {
    const boardFen = this.shogiground ? this.shogiground.getFen() : this.cfg.fen;
    const board = parseFen(lishogiBoardToShogiBoard(boardFen)).unwrap(setup => setup.board, _ => Board.empty());
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
    return setupPosition(this.rules, this.getSetup()).unwrap(pos => {
      return makeFen(pos.toSetup());
    }, _ => undefined);
  }

  private isPlayable(): boolean {
    return setupPosition(this.rules, this.getSetup()).unwrap(pos => !pos.isEnd(), _ => false);
  }

  private tsumeFen(): string | undefined {
    return Shogi.fromSetup(this.getSetup(), false).unwrap(
      pos => {if(pos.board.king.size() > 0) return this.getFen(); else return undefined; },
      _ => undefined
    );
  }

  getState(): EditorState {
    return {
      fen: this.getFen(),
      legalFen: this.getLegalFen(),
      playable: this.rules == 'shogi' && this.isPlayable(),
      tsumeFen: this.tsumeFen(),
      standardPieceNumber: this.standardNumberOfPiecesOrLess()
    };
  }

  makeAnalysisUrl(legalFen: string): string {
    return this.makeUrl(`/analysis/`, makeLishogiFen(legalFen));
  }

  makeUrl(baseUrl: string, fen: string): string {
    return (
      baseUrl +
      encodeURIComponent(fen).replace(/%20/g, "_").replace(/%2F/g, "/")
    );
  }

  bottomColor(): Color {
    return this.shogiground
      ? this.shogiground.state.orientation
      : this.options.orientation || "white";
  }

  setTurn(turn: Color): void {
    this.turn = turn;
    this.onChange();
  }

  startPosition(): void {
    this.setFen(
      INITIAL_FEN
    );
  }

  clearBoard(): void {
    this.setFen(EMPTY_FEN);
  }

  loadNewFen(fen: string | "prompt"): void {
    if (fen === "prompt") {
      fen = (prompt("Paste SFEN position") || "").trim();
      if (!fen) return;
    }
    this.setFen(fen);
  }

  setFen(fen: string): boolean {
    return parseFen(fen).unwrap(setup => {
      if (this.shogiground) this.shogiground.set({fen});
      this.pockets = setup.pockets;
      this.turn = setup.turn;
      this.fullmoves = setup.fullmoves;

      this.onChange();
      return true;
    }, _ => false);
  }

  standardNumberOfPiecesOrLess(): boolean {
    const setup = this.getSetup();
    return setup.board.pawn.size() + setup.board.tokin.size() + this.pockets['black'].pawn <= 18 &&
      setup.board.lance.size() + setup.board.promotedLance.size() + this.pockets['black'].lance <= 4 &&
      setup.board.knight.size() + setup.board.promotedKnight.size() + this.pockets['black'].knight <= 4 &&
      setup.board.silver.size() + setup.board.promotedSilver.size() + this.pockets['black'].silver <= 4 &&
      setup.board.gold.size() + this.pockets['black'].gold <= 4 &&
      setup.board.bishop.size() + setup.board.horse.size() + this.pockets['black'].bishop <= 2 &&
      setup.board.rook.size() + setup.board.dragon.size() + this.pockets['black'].rook <= 2 &&
      setup.board.king.size() <= 2 
  }

  fillGotesHand(): void {
    const setup = this.getSetup();
    const senteHand = this.pockets['black'];
    
    const pieceCounts: {[index: string]:number} = {
      "lance": 4 - setup.board.lance.size() - setup.board.promotedLance.size() - senteHand.lance,
      "knight": 4 - setup.board.knight.size() - setup.board.promotedKnight.size() - senteHand.knight,
      "silver": 4 - setup.board.silver.size() - setup.board.promotedSilver.size() - senteHand.silver,
      "gold": 4 - setup.board.gold.size() - senteHand.gold,
      "pawn": 18 - setup.board.pawn.size() - setup.board.tokin.size() - senteHand.pawn,
      "bishop": 2 - setup.board.bishop.size() - setup.board.horse.size() - senteHand.bishop,
      "rook": 2 - setup.board.rook.size() - setup.board.dragon.size() - senteHand.rook
    };
    this.pockets['white'] = MaterialSide.empty();

    for (const p in pieceCounts) {
      if (pieceCounts[p] > 0)
        this.pockets['white'][p as PocketRole] = pieceCounts[p];
    }
    this.onChange();
  }

  setOrientation(o: Color): void {
    this.options.orientation = o;
    if (this.shogiground!.state.orientation !== o)
      this.shogiground!.toggleOrientation();
    this.redraw();
  }

  addToPocket(c: Color, r: Role, reload: boolean = false): void {
    if(["pawn", "lance", "knight", "silver", "gold", "bishop", "rook"].includes(r))
      this.pockets[c][r as PocketRole]++;
      if(reload) this.onChange();
  }
  removeFromPocket(c: Color, r: Role, reload: boolean = false): void {
    if(["pawn", "lance", "knight", "silver", "gold", "bishop", "rook"].includes(r) &&
      this.pockets[c][r as PocketRole] > 0)
      this.pockets[c][r as PocketRole]--;
    if(reload) this.onChange();
  }
  clearPocket(){
    this.pockets = Material.empty();
  }
  clearColorPocket(c: Color){
    this.pockets[c] = MaterialSide.empty();
  }
}
