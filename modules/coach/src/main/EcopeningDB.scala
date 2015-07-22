package lila.coach

private[coach] object EcopeningDB {

  import Ecopening._

  lazy val all: Map[ECO, Ecopening] = db.flatMap {
    case (_, openings) => openings
  }

  val db: Map[Family, Map[ECO, Ecopening]] = Map(
    "Uncommon Opening" -> Map(
      "A00" -> Ecopening("A00", "Uncommon Opening", "Uncommon Opening", "g4, a3, h3, etc.", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")
    ),
    "Nimzovich-Larsen Attack" -> Map(
      "A01" -> Ecopening("A01", "Nimzovich-Larsen Attack", "Nimzovich-Larsen Attack", "b3", "rnbqkbnr/pppppppp/8/8/8/1P6/P1PPPPPP/RNBQKBNR")
    ),
    "Bird's Opening" -> Map(
      "A02" -> Ecopening("A02", "Bird's Opening", "Bird's Opening", "f4", "rnbqkbnr/pppppppp/8/8/5P2/8/PPPPP1PP/RNBQKBNR"),
      "A03" -> Ecopening("A03", "Bird's Opening", "Bird's Opening", "f4 d5", "rnbqkbnr/ppp1pppp/8/3p4/5P2/8/PPPPP1PP/RNBQKBNR")
    ),
    "Reti Opening" -> Map(
      "A04" -> Ecopening("A04", "Reti Opening", "Reti Opening", "Nf3", "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R"),
      "A05" -> Ecopening("A05", "Reti Opening", "Reti Opening", "Nf3 Nf6", "rnbqkb1r/pppppppp/5n2/8/8/5N2/PPPPPPPP/RNBQKB1R"),
      "A06" -> Ecopening("A06", "Reti Opening", "Reti Opening", "Nf3 d5", "rnbqkbnr/ppp1pppp/8/3p4/8/5N2/PPPPPPPP/RNBQKB1R"),
      "A09" -> Ecopening("A09", "Reti Opening", "Reti Opening", "Nf3 d5 c4", "rnbqkbnr/ppp1pppp/8/3p4/2P5/5N2/PP1PPPPP/RNBQKB1R")
    ),
    "King's Indian Attack" -> Map(
      "A07" -> Ecopening("A07", "King's Indian Attack", "King's Indian Attack", "Nf3 d5 g3", "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R"),
      "A08" -> Ecopening("A08", "King's Indian Attack", "King's Indian Attack", "Nf3 d5 g3 c5 Bg2", "rnbqkbnr/pp2pppp/8/2pp4/8/5NP1/PPPPPPBP/RNBQK2R")
    ),
    "English" -> Map(
      "A10" -> Ecopening("A10", "English", "English", "c4", "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR"),
      "A11" -> Ecopening("A11", "English", "English, Caro-Kann Defensive System", "c4 c6", "rnbqkbnr/pp1ppppp/2p5/8/2P5/8/PP1PPPPP/RNBQKBNR"),
      "A13" -> Ecopening("A13", "English", "English", "c4 e6", "rnbqkbnr/pppp1ppp/4p3/8/2P5/8/PP1PPPPP/RNBQKBNR"),
      "A14" -> Ecopening("A14", "English", "English", "c4 e6 Nf3 d5 g3 Nf6 Bg2 Be7 O-O", "rnbqk2r/ppp1bppp/4pn2/3p4/2P5/5NP1/PP1PPPBP/RNBQ1RK1"),
      "A15" -> Ecopening("A15", "English", "English", "c4 Nf6", "rnbqkb1r/pppppppp/5n2/8/2P5/8/PP1PPPPP/RNBQKBNR"),
      "A16" -> Ecopening("A16", "English", "English", "c4 Nf6 Nc3", "rnbqkb1r/pppppppp/5n2/8/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A17" -> Ecopening("A17", "English", "English", "c4 Nf6 Nc3 e6", "rnbqkb1r/pppp1ppp/4pn2/8/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A18" -> Ecopening("A18", "English", "English, Mikenas-Carls", "c4 Nf6 Nc3 e6 e4", "rnbqkb1r/pppp1ppp/4pn2/8/2P1P3/2N5/PP1P1PPP/R1BQKBNR"),
      "A19" -> Ecopening("A19", "English", "English, Mikenas-Carls, Sicilian Variation", "c4 Nf6 Nc3 e6 e4 c5", "rnbqkb1r/pp1p1ppp/4pn2/2p5/2P1P3/2N5/PP1P1PPP/R1BQKBNR"),
      "A20" -> Ecopening("A20", "English", "English", "c4 e5", "rnbqkbnr/pppp1ppp/8/4p3/2P5/8/PP1PPPPP/RNBQKBNR"),
      "A21" -> Ecopening("A21", "English", "English", "c4 e5 Nc3", "rnbqkbnr/pppp1ppp/8/4p3/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A22" -> Ecopening("A22", "English", "English", "c4 e5 Nc3 Nf6", "rnbqkb1r/pppp1ppp/5n2/4p3/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A23" -> Ecopening("A23", "English", "English, Bremen System, Keres Variation", "c4 e5 Nc3 Nf6 g3 c6", "rnbqkb1r/pp1p1ppp/2p2n2/4p3/2P5/2N3P1/PP1PPP1P/R1BQKBNR"),
      "A24" -> Ecopening("A24", "English", "English, Bremen System with ...g6", "c4 e5 Nc3 Nf6 g3 g6", "rnbqkb1r/pppp1p1p/5np1/4p3/2P5/2N3P1/PP1PPP1P/R1BQKBNR"),
      "A25" -> Ecopening("A25", "English", "English", "c4 e5 Nc3 Nc6", "r1bqkbnr/pppp1ppp/2n5/4p3/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A26" -> Ecopening("A26", "English", "English", "c4 e5 Nc3 Nc6 g3 g6 Bg2 Bg7 d3 d6", "r1bqk1nr/ppp2pbp/2np2p1/4p3/2P5/2NP2P1/PP2PPBP/R1BQK1NR"),
      "A27" -> Ecopening("A27", "English", "English, Three Knights System", "c4 e5 Nc3 Nc6 Nf3", "r1bqkbnr/pppp1ppp/2n5/4p3/2P5/2N2N2/PP1PPPPP/R1BQKB1R"),
      "A28" -> Ecopening("A28", "English", "English", "c4 e5 Nc3 Nc6 Nf3 Nf6", "r1bqkb1r/pppp1ppp/2n2n2/4p3/2P5/2N2N2/PP1PPPPP/R1BQKB1R"),
      "A29" -> Ecopening("A29", "English", "English, Four Knights, Kingside Fianchetto", "c4 e5 Nc3 Nc6 Nf3 Nf6 g3", "r1bqkb1r/pppp1ppp/2n2n2/4p3/2P5/2N2NP1/PP1PPP1P/R1BQKB1R"),
      "A30" -> Ecopening("A30", "English", "English, Symmetrical", "c4 c5", "rnbqkbnr/pp1ppppp/8/2p5/2P5/8/PP1PPPPP/RNBQKBNR"),
      "A31" -> Ecopening("A31", "English", "English, Symmetrical, Benoni Formation", "c4 c5 Nf3 Nf6 d4", "rnbqkb1r/pp1ppppp/5n2/2p5/2PP4/5N2/PP2PPPP/RNBQKB1R"),
      "A32" -> Ecopening("A32", "English", "English, Symmetrical Variation", "c4 c5 Nf3 Nf6 d4 cxd4 Nxd4 e6", "rnbqkb1r/pp1p1ppp/4pn2/8/2PN4/8/PP2PPPP/RNBQKB1R"),
      "A33" -> Ecopening("A33", "English", "English, Symmetrical", "c4 c5 Nf3 Nf6 d4 cxd4 Nxd4 e6 Nc3 Nc6", "r1bqkb1r/pp1p1ppp/2n1pn2/8/2PN4/2N5/PP2PPPP/R1BQKB1R"),
      "A34" -> Ecopening("A34", "English", "English, Symmetrical", "c4 c5 Nc3", "rnbqkbnr/pp1ppppp/8/2p5/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A35" -> Ecopening("A35", "English", "English, Symmetrical", "c4 c5 Nc3 Nc6", "r1bqkbnr/pp1ppppp/2n5/2p5/2P5/2N5/PP1PPPPP/R1BQKBNR"),
      "A36" -> Ecopening("A36", "English", "English", "c4 c5 Nc3 Nc6 g3", "r1bqkbnr/pp1ppppp/2n5/2p5/2P5/2N3P1/PP1PPP1P/R1BQKBNR"),
      "A37" -> Ecopening("A37", "English", "English, Symmetrical", "c4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 Nf3", "r1bqk1nr/pp1pppbp/2n3p1/2p5/2P5/2N2NP1/PP1PPPBP/R1BQK2R"),
      "A38" -> Ecopening("A38", "English", "English, Symmetrical", "c4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 Nf3 Nf6", "r1bqk2r/pp1pppbp/2n2np1/2p5/2P5/2N2NP1/PP1PPPBP/R1BQK2R"),
      "A39" -> Ecopening("A39", "English", "English, Symmetrical, Main line with d4", "c4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 Nf3 Nf6 O-O O-O7 d4", "r1bqk2r/pp1pppbp/2n2np1/2p5/2P5/2N2NP1/PP1PPPBP/R1BQ1RK1")
    ),
    "English with b3" -> Map(
      "A12" -> Ecopening("A12", "English with b3", "English with b3", "c4 c6 Nf3 d5 b3", "rnbqkbnr/pp2pppp/2p5/3p4/2P5/1P3N2/P2PPPPP/RNBQKB1R")
    ),
    "Queen's Pawn Game" -> Map(
      "A40" -> Ecopening("A40", "Queen's Pawn Game", "Queen's Pawn Game", "d4", "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR"),
      "A45" -> Ecopening("A45", "Queen's Pawn Game", "Queen's Pawn Game", "d4 Nf6", "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR"),
      "A46" -> Ecopening("A46", "Queen's Pawn Game", "Queen's Pawn Game", "d4 Nf6 Nf3", "rnbqkb1r/pppppppp/5n2/8/3P4/5N2/PPP1PPPP/RNBQKB1R"),
      "A50" -> Ecopening("A50", "Queen's Pawn Game", "Queen's Pawn Game", "d4 Nf6 c4", "rnbqkb1r/pppppppp/5n2/8/2PP4/8/PP2PPPP/RNBQKBNR"),
      "D00" -> Ecopening("D00", "Queen's Pawn Game", "Queen's Pawn Game", "d4 d5", "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR"),
      "D02" -> Ecopening("D02", "Queen's Pawn Game", "Queen's Pawn Game", "d4 d5 Nf3", "rnbqkbnr/ppp1pppp/8/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R"),
      "D04" -> Ecopening("D04", "Queen's Pawn Game", "Queen's Pawn Game", "d4 d5 Nf3 Nf6 e3", "rnbqkb1r/ppp1pppp/5n2/3p4/3P4/4PN2/PPP2PPP/RNBQKB1R"),
      "D05" -> Ecopening("D05", "Queen's Pawn Game", "Queen's Pawn Game", "d4 d5 Nf3 Nf6 e3 e6", "rnbqkb1r/ppp2ppp/4pn2/3p4/3P4/4PN2/PPP2PPP/RNBQKB1R"),
      "E00" -> Ecopening("E00", "Queen's Pawn Game", "Queen's Pawn Game", "d4 Nf6 c4 e6", "rnbqkb1r/pppp1ppp/4pn2/8/2PP4/8/PP2PPPP/RNBQKBNR"),
      "E10" -> Ecopening("E10", "Queen's Pawn Game", "Queen's Pawn Game", "d4 Nf6 c4 e6 Nf3", "rnbqkb1r/pppp1ppp/4pn2/8/2PP4/5N2/PP2PPPP/RNBQKB1R")
    ),
    "Queen's Pawn Game " -> Map(
      "A41" -> Ecopening("A41", "Queen's Pawn Game ", "Queen's Pawn Game (with ...d6)", "d4 d6", "rnbqkbnr/ppp1pppp/3p4/8/3P4/8/PPP1PPPP/RNBQKBNR")
    ),
    "Modern Defense" -> Map(
      "A42" -> Ecopening("A42", "Modern Defense", "Modern Defense, Averbakh System", "d4 d6 c4 g6 Nc3 Bg7 e4", "rnbqk1nr/ppp1ppbp/3p2p1/8/2PPP3/2N5/PP3PPP/R1BQKBNR")
    ),
    "Old Benoni" -> Map(
      "A43" -> Ecopening("A43", "Old Benoni", "Old Benoni", "d4 c5", "rnbqkbnr/pp1ppppp/8/2p5/3P4/8/PPP1PPPP/RNBQKBNR")
    ),
    "Old Benoni Defense" -> Map(
      "A44" -> Ecopening("A44", "Old Benoni Defense", "Old Benoni Defense", "d4 c5 d5 e5", "rnbqkbnr/pp1p1ppp/8/2pPp3/8/8/PPP1PPPP/RNBQKBNR")
    ),
    "Queen's Indian" -> Map(
      "A47" -> Ecopening("A47", "Queen's Indian", "Queen's Indian", "d4 Nf6 Nf3 b6", "rnbqkb1r/p1pppppp/1p3n2/8/3P4/5N2/PPP1PPPP/RNBQKB1R"),
      "E12" -> Ecopening("E12", "Queen's Indian", "Queen's Indian", "d4 Nf6 c4 e6 Nf3 b6", "rnbqkb1r/p1pp1ppp/1p2pn2/8/2PP4/5N2/PP2PPPP/RNBQKB1R"),
      "E13" -> Ecopening("E13", "Queen's Indian", "Queen's Indian, 4.Nc3, Main line", "d4 Nf6 c4 e6 Nf3 b6 Nc3 Bb7 Bg5 h6 Bh4 Bb4", "rn1qk2r/pbpp1pp1/1p2pn1p/8/1bPP3B/2N2N2/PP2PPPP/R2QKB1R"),
      "E14" -> Ecopening("E14", "Queen's Indian", "Queen's Indian", "d4 Nf6 c4 e6 Nf3 b6 e3", "rnbqkb1r/p1pp1ppp/1p2pn2/8/2PP4/4PN2/PP3PPP/RNBQKB1R"),
      "E15" -> Ecopening("E15", "Queen's Indian", "Queen's Indian", "d4 Nf6 c4 e6 Nf3 b6 g3", "rnbqkb1r/p1pp1ppp/1p2pn2/8/2PP4/5NP1/PP2PP1P/RNBQKB1R"),
      "E16" -> Ecopening("E16", "Queen's Indian", "Queen's Indian", "d4 Nf6 c4 e6 Nf3 b6 g3 Bb7 Bg2 Bb4+", "rn1qk2r/pbpp1ppp/1p2pn2/8/1bPP4/5NP1/PP2PPBP/RNBQK2R"),
      "E17" -> Ecopening("E17", "Queen's Indian", "Queen's Indian", "d4 Nf6 c4 e6 Nf3 b6 g3 Bb7 Bg2 Be7", "rn1qk2r/pbppbppp/1p2pn2/8/2PP4/5NP1/PP2PPBP/RNBQK2R"),
      "E18" -> Ecopening("E18", "Queen's Indian", "Queen's Indian, Old Main line, 7.Nc3", "d4 Nf6 c4 e6 Nf3 b6 g3 Bb7 Bg2 Be7 O-O O-O7 Nc3", "rn1qk2r/pbppbppp/1p2pn2/8/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "E19" -> Ecopening("E19", "Queen's Indian", "Queen's Indian, Old Main line, 9.Qxc3", "d4 Nf6 c4 e6 Nf3 b6 g3 Bb7 Bg2 Be7 O-O O-O7 Nc3 Ne4 Qc2 Nxc3", "rn1qk2r/pbppbppp/1p2p3/8/2PPn3/5NP1/PPQ1PPBP/RNB2RK1")
    ),
    "King's Indian" -> Map(
      "A48" -> Ecopening("A48", "King's Indian", "King's Indian", "d4 Nf6 Nf3 g6", "rnbqkb1r/pppppp1p/5np1/8/3P4/5N2/PPP1PPPP/RNBQKB1R"),
      "A49" -> Ecopening("A49", "King's Indian", "King's Indian, Fianchetto without c4", "d4 Nf6 Nf3 g6 g3", "rnbqkb1r/pppppp1p/5np1/8/3P4/5NP1/PPP1PP1P/RNBQKB1R"),
      "E61" -> Ecopening("E61", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3", "rnbqkb1r/pppppp1p/5np1/8/2PP4/2N5/PP2PPPP/R1BQKBNR"),
      "E62" -> Ecopening("E62", "King's Indian", "King's Indian, Fianchetto", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3", "rnbqk2r/ppp1ppbp/3p1np1/8/2PP4/2N2NP1/PP2PP1P/R1BQKB1R"),
      "E63" -> Ecopening("E63", "King's Indian", "King's Indian, Fianchetto, Panno Variation", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 Nc67 O-O a6", "rnbq1rk1/1pp1ppbp/p2p1np1/8/2PP4/2N2NP1/PP2PPBP/R1BQK2R"),
      "E64" -> Ecopening("E64", "King's Indian", "King's Indian, Fianchetto, Yugoslav System", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 c5", "rnbq1rk1/pp2ppbp/3p1np1/2p5/2PP4/2N2NP1/PP2PPBP/R1BQK2R"),
      "E65" -> Ecopening("E65", "King's Indian", "King's Indian, Fianchetto, Yugoslav, 7.O-O", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 c57 O-O", "rnbq1rk1/ppp1ppbp/3p1np1/8/2PP4/2N2NP1/PP2PPBP/R1BQK2R"),
      "E66" -> Ecopening("E66", "King's Indian", "King's Indian, Fianchetto, Yugoslav Panno", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 c57 O-O Nc6 d5", "r1bq1rk1/ppp1ppbp/2np1np1/3P4/2P5/2N2NP1/PP2PPBP/R1BQK2R"),
      "E67" -> Ecopening("E67", "King's Indian", "King's Indian, Fianchetto", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 Nbd7", "r1bq1rk1/pppnppbp/3p1np1/8/2PP4/2N2NP1/PP2PPBP/R1BQK2R"),
      "E68" -> Ecopening("E68", "King's Indian", "King's Indian, Fianchetto, Classical Variation, 8.e4", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 Nbd77 O-O e5 e4", "rnbq1rk1/ppp2pbp/3p1np1/4p3/2PPP3/2N2NP1/PP3PBP/R1BQK2R"),
      "E69" -> Ecopening("E69", "King's Indian", "King's Indian, Fianchetto, Classical Main line", "d4 Nf6 c4 g6 Nc3 Bg7 Nf3 d6 g3 O-O Bg2 Nbd77 O-O e5 e4 c6 h3", "rnbq1rk1/pp3pbp/2pp1np1/4p3/2PPP3/2N2NPP/PP3PB1/R1BQK2R"),
      "E70" -> Ecopening("E70", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4", "rnbqk2r/ppppppbp/5np1/8/2PPP3/2N5/PP3PPP/R1BQKBNR"),
      "E71" -> Ecopening("E71", "King's Indian", "King's Indian, Makagonov System (5.h3)", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 h3", "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N4P/PP3PP1/R1BQKBNR"),
      "E72" -> Ecopening("E72", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 g3", "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N3P1/PP3P1P/R1BQKBNR"),
      "E73" -> Ecopening("E73", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Be2", "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N5/PP2BPPP/R1BQK1NR"),
      "E74" -> Ecopening("E74", "King's Indian", "King's Indian, Averbakh, 6...c5", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Be2 O-O Bg5 c5", "rnbq1rk1/pp2ppbp/3p1np1/2p3B1/2PPP3/2N5/PP2BPPP/R2QK1NR"),
      "E75" -> Ecopening("E75", "King's Indian", "King's Indian, Averbakh, Main line", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Be2 O-O Bg5 c57 d5 e6", "rnbq1rk1/ppp1ppbp/5np1/3p2B1/2PPP3/2N5/PP2BPPP/R2QK1NR"),
      "E76" -> Ecopening("E76", "King's Indian", "King's Indian, Four Pawns Attack", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f4", "rnbqk2r/ppp1ppbp/3p1np1/8/2PPPP2/2N5/PP4PP/R1BQKBNR"),
      "E77" -> Ecopening("E77", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f4 O-O Be2", "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPPP2/2N5/PP2B1PP/R1BQK1NR"),
      "E78" -> Ecopening("E78", "King's Indian", "King's Indian, Four Pawns Attack, with Be2 and Nf3", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f4 O-O Be2 c57 Nf3", "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPPP2/2N5/PP2B1PP/R1BQK1NR"),
      "E79" -> Ecopening("E79", "King's Indian", "King's Indian, Four Pawns Attack, Main line", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f4 O-O Be2 c57 Nf3 cxd4 Nxd4 Nc6 Be3", "r1bq1rk1/ppp1ppbp/2np1np1/8/2PPPP2/2N1B3/PP2B1PP/R2QK1NR"),
      "E80" -> Ecopening("E80", "King's Indian", "King's Indian, Samisch Variation", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3", "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N2P2/PP4PP/R1BQKBNR"),
      "E81" -> Ecopening("E81", "King's Indian", "King's Indian, Samisch", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O", "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPP3/2N2P2/PP4PP/R1BQKBNR"),
      "E82" -> Ecopening("E82", "King's Indian", "King's Indian, Samisch, double Fianchetto Variation", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 b6", "rnbq1rk1/p1p1ppbp/1p1p1np1/8/2PPP3/2N1BP2/PP4PP/R2QKBNR"),
      "E83" -> Ecopening("E83", "King's Indian", "King's Indian, Samisch", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 Nc6", "r1bq1rk1/ppp1ppbp/2np1np1/8/2PPP3/2N1BP2/PP4PP/R2QKBNR"),
      "E84" -> Ecopening("E84", "King's Indian", "King's Indian, Samisch, Panno Main line", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 Nc67 Nge2 a6 Qd2 Rb8", "rnbq1rk1/1pp1ppbp/p2p1np1/8/2PPP3/2N1BP2/PP1Q2PP/R3KBNR"),
      "E85" -> Ecopening("E85", "King's Indian", "King's Indian, Samisch, Orthodox Variation", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 e5", "rnbq1rk1/ppp2pbp/3p1np1/4p3/2PPP3/2N1BP2/PP4PP/R2QKBNR"),
      "E86" -> Ecopening("E86", "King's Indian", "King's Indian, Samisch, Orthodox, 7.Nge2 c6", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 e57 Nge2 c6", "rnbq1rk1/pp2ppbp/2pp1np1/8/2PPP3/2N1BP2/PP4PP/R2QKBNR"),
      "E87" -> Ecopening("E87", "King's Indian", "King's Indian, Samisch, Orthodox", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 e57 d5", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PPP3/2N1BP2/PP4PP/R2QKBNR"),
      "E88" -> Ecopening("E88", "King's Indian", "King's Indian, Samisch, Orthodox, 7.d5 c6", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 e57 d5 c6", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PPP3/2N1BP2/PP4PP/R2QKBNR"),
      "E89" -> Ecopening("E89", "King's Indian", "King's Indian, Samisch, Orthodox Main line", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 e57 d5 c6 Nge2 cxd5", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PPP3/2N1BP2/PP2N1PP/R2QKB1R"),
      "E90" -> Ecopening("E90", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3", "rnbqk2r/ppp1ppbp/3p1np1/8/2PPP3/2N2N2/PP3PPP/R1BQKB1R"),
      "E91" -> Ecopening("E91", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2", "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPP3/2N2N2/PP2BPPP/R1BQK2R"),
      "E92" -> Ecopening("E92", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5", "rnbq1rk1/ppp2pbp/3p1np1/4p3/2PPP3/2N2N2/PP2BPPP/R1BQK2R"),
      "E93" -> Ecopening("E93", "King's Indian", "King's Indian, Petrosian System", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 d5 Nbd7", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PPP3/2N2N2/PP2BPPP/R1BQK2R"),
      "E94" -> Ecopening("E94", "King's Indian", "King's Indian, Orthodox", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 O-O", "rnbq1rk1/ppp1ppbp/3p1np1/8/2PPP3/2N2N2/PP2BPPP/R1BQK2R"),
      "E95" -> Ecopening("E95", "King's Indian", "King's Indian, Orthodox, 7...Nbd7, 8.Re1", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 O-O Nbd7 Re1", "r1bq1rk1/pppnppbp/3p1np1/8/2PPP3/2N2N2/PP2BPPP/R1BQK2R"),
      "E96" -> Ecopening("E96", "King's Indian", "King's Indian, Orthodox, 7...Nbd7, Main line", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 O-O Nbd7 Re1 c6 Bf1 a5", "r1bq1rk1/1ppnppbp/3p1np1/p7/2PPP3/2N2N2/PP3PPP/R1BQKB1R"),
      "E97" -> Ecopening("E97", "King's Indian", "King's Indian", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 O-O Nc6", "r1bq1rk1/ppp1ppbp/2np1np1/8/2PPP3/2N2N2/PP2BPPP/R1BQK2R"),
      "E98" -> Ecopening("E98", "King's Indian", "King's Indian, Orthodox, Taimanov, 9.Ne1", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 O-O Nc6 d5 Ne7 Ne1", "r1bq1rk1/ppp1ppbp/2np1np1/3P4/2P1P3/2N2N2/PP2BPPP/R1BQK2R"),
      "E99" -> Ecopening("E99", "King's Indian", "King's Indian, Orthodox, Taimanov", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e57 O-O Nc6 d5 Ne7 Ne1 Nd7 f3 f5", "r1bq1rk1/pppnppbp/2np2p1/3P4/2P1P3/2N2N2/PP2BPPP/R1BQK2R")
    ),
    "Budapest Gambit" -> Map(
      "A51" -> Ecopening("A51", "Budapest Gambit", "Budapest Gambit", "d4 Nf6 c4 e5", "rnbqkb1r/pppp1ppp/5n2/4p3/2PP4/8/PP2PPPP/RNBQKBNR"),
      "A52" -> Ecopening("A52", "Budapest Gambit", "Budapest Gambit", "d4 Nf6 c4 e5 dxe5 Ng4", "rnbqkb1r/pppp1ppp/8/4P3/2P3n1/8/PP2PPPP/RNBQKBNR")
    ),
    "Old Indian" -> Map(
      "A53" -> Ecopening("A53", "Old Indian", "Old Indian", "d4 Nf6 c4 d6", "rnbqkb1r/ppp1pppp/3p1n2/8/2PP4/8/PP2PPPP/RNBQKBNR"),
      "A54" -> Ecopening("A54", "Old Indian", "Old Indian, Ukrainian Variation, 4.Nf3", "d4 Nf6 c4 d6 Nc3 e5 Nf3", "rnbqkb1r/ppp2ppp/3p1n2/4p3/2PP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "A55" -> Ecopening("A55", "Old Indian", "Old Indian, Main line", "d4 Nf6 c4 d6 Nc3 e5 Nf3 Nbd7 e4", "r1bqkb1r/pppn1ppp/3p1n2/4p3/2PPP3/2N2N2/PP3PPP/R1BQKB1R")
    ),
    "Benoni Defense" -> Map(
      "A56" -> Ecopening("A56", "Benoni Defense", "Benoni Defense", "d4 Nf6 c4 c5", "rnbqkb1r/pp1ppppp/5n2/2p5/2PP4/8/PP2PPPP/RNBQKBNR"),
      "A60" -> Ecopening("A60", "Benoni Defense", "Benoni Defense", "d4 Nf6 c4 c5 d5 e6", "rnbqkb1r/pp1p1ppp/4pn2/2pP4/2P5/8/PP2PPPP/RNBQKBNR")
    ),
    "Benko Gambit" -> Map(
      "A57" -> Ecopening("A57", "Benko Gambit", "Benko Gambit", "d4 Nf6 c4 c5 d5 b5", "rnbqkb1r/p2ppppp/5n2/1ppP4/2P5/8/PP2PPPP/RNBQKBNR"),
      "A58" -> Ecopening("A58", "Benko Gambit", "Benko Gambit", "d4 Nf6 c4 c5 d5 b5 cxb5 a6 bxa6", "rnbqkb1r/3ppppp/P4n2/2pP4/8/8/PP2PPPP/RNBQKBNR"),
      "A59" -> Ecopening("A59", "Benko Gambit", "Benko Gambit", "d4 Nf6 c4 c5 d5 b5 cxb5 a6 bxa6 Bxa6 Nc3 d67 e4", "rn1qkb1r/3ppppp/b4n2/2pP4/8/2N5/PP2PPPP/R1BQKBNR")
    ),
    "Benoni" -> Map(
      "A61" -> Ecopening("A61", "Benoni", "Benoni", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 Nf3 g6", "rnbqkb1r/pp3p1p/3p1np1/2pP4/8/2N2N2/PP2PPPP/R1BQKB1R"),
      "A62" -> Ecopening("A62", "Benoni", "Benoni, Fianchetto Variation", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 Nf3 g67 g3 Bg7 Bg2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/8/2N2N2/PP2PPPP/R1BQKB1R"),
      "A63" -> Ecopening("A63", "Benoni", "Benoni, Fianchetto, 9...Nbd7", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 Nf3 g67 g3 Bg7 Bg2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/8/2N2N2/PP2PPPP/R1BQKB1R"),
      "A64" -> Ecopening("A64", "Benoni", "Benoni, Fianchetto, 11...Re8", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 Nf3 g67 g3 Bg7 Bg2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/8/2N2N2/PP2PPPP/R1BQKB1R"),
      "A65" -> Ecopening("A65", "Benoni", "Benoni, 6.e4", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A66" -> Ecopening("A66", "Benoni", "Benoni", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 f4", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A67" -> Ecopening("A67", "Benoni", "Benoni, Taimanov Variation", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 f4 Bg7 Bb5+", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A68" -> Ecopening("A68", "Benoni", "Benoni, Four Pawns Attack", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 f4 Bg7 Nf3 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A69" -> Ecopening("A69", "Benoni", "Benoni, Four Pawns Attack, Main line", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 f4 Bg7 Nf3 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A70" -> Ecopening("A70", "Benoni", "Benoni, Classical with 7.Nf3", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A71" -> Ecopening("A71", "Benoni", "Benoni, Classical, 8.Bg5", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Bg5", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A72" -> Ecopening("A72", "Benoni", "Benoni, Classical without 9.O-O", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A73" -> Ecopening("A73", "Benoni", "Benoni, Classical, 9.O-O", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A74" -> Ecopening("A74", "Benoni", "Benoni, Classical, 9...a6, 10.a4", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A75" -> Ecopening("A75", "Benoni", "Benoni, Classical with ...a6 and 10...Bg4", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A76" -> Ecopening("A76", "Benoni", "Benoni, Classical, 9...Re8", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A77" -> Ecopening("A77", "Benoni", "Benoni, Classical, 9...Re8, 10.Nd2", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A78" -> Ecopening("A78", "Benoni", "Benoni, Classical with ...Re8 and ...Na6", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR"),
      "A79" -> Ecopening("A79", "Benoni", "Benoni, Classical, 11.f3", "d4 Nf6 c4 c5 d5 e6 Nc3 exd5 cxd5 d6 e4 g67 Nf3 Bg7 Be2 O-O", "rnbqkb1r/pp3ppp/3p1n2/2pP4/4P3/2N5/PP3PPP/R1BQKBNR")
    ),
    "Dutch" -> Map(
      "A80" -> Ecopening("A80", "Dutch", "Dutch", "d4 f5", "rnbqkbnr/ppppp1pp/8/5p2/3P4/8/PPP1PPPP/RNBQKBNR"),
      "A81" -> Ecopening("A81", "Dutch", "Dutch", "d4 f5 g3", "rnbqkbnr/ppppp1pp/8/5p2/3P4/6P1/PPP1PP1P/RNBQKBNR"),
      "A82" -> Ecopening("A82", "Dutch", "Dutch, Staunton Gambit", "d4 f5 e4", "rnbqkbnr/ppppp1pp/8/5p2/3PP3/8/PPP2PPP/RNBQKBNR"),
      "A83" -> Ecopening("A83", "Dutch", "Dutch, Staunton Gambit", "d4 f5 e4 fxe4 Nc3 Nf6 Bg5", "rnbqkb1r/ppppp1pp/5n2/6B1/3Pp3/2N5/PPP2PPP/R2QKBNR"),
      "A84" -> Ecopening("A84", "Dutch", "Dutch", "d4 f5 c4", "rnbqkbnr/ppppp1pp/8/5p2/2PP4/8/PP2PPPP/RNBQKBNR"),
      "A85" -> Ecopening("A85", "Dutch", "Dutch, with c4 & Nc3", "d4 f5 c4 Nf6 Nc3", "rnbqkb1r/ppppp1pp/5n2/5p2/2PP4/2N5/PP2PPPP/R1BQKBNR"),
      "A86" -> Ecopening("A86", "Dutch", "Dutch", "d4 f5 c4 Nf6 g3", "rnbqkb1r/ppppp1pp/5n2/5p2/2PP4/6P1/PP2PP1P/RNBQKBNR"),
      "A87" -> Ecopening("A87", "Dutch", "Dutch, Leningrad, Main Variation", "d4 f5 c4 Nf6 g3 g6 Bg2 Bg7 Nf3", "rnbqk2r/ppppp1bp/5np1/5p2/2PP4/5NP1/PP2PPBP/RNBQK2R"),
      "A88" -> Ecopening("A88", "Dutch", "Dutch, Leningrad, Main Variation with c6", "d4 f5 c4 Nf6 g3 g6 Bg2 Bg7 Nf3 O-O O-O d67 Nc3 c6", "rnbq1rk1/pp1pp1bp/2p2np1/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A89" -> Ecopening("A89", "Dutch", "Dutch, Leningrad, Main Variation with Nc6", "d4 f5 c4 Nf6 g3 g6 Bg2 Bg7 Nf3 O-O O-O d67 Nc3 Nc6", "r1bq1rk1/ppppp1bp/2n2np1/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A90" -> Ecopening("A90", "Dutch", "Dutch", "d4 f5 c4 Nf6 g3 e6 Bg2", "rnbqkb1r/pppp2pp/4pn2/5p2/2PP4/6P1/PP2PPBP/RNBQK1NR"),
      "A92" -> Ecopening("A92", "Dutch", "Dutch", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O", "rnbq1rk1/ppppb1pp/4pn2/5p2/2PP4/5NP1/PP2PPBP/RNBQK2R"),
      "A93" -> Ecopening("A93", "Dutch", "Dutch, Stonewall, Botvinnik Variation", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d57 b3", "rnbq1rk1/ppppb1pp/4pn2/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A94" -> Ecopening("A94", "Dutch", "Dutch, Stonewall with Ba3", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d57 b3 c6 Ba3", "rnbq1rk1/pp1pb1pp/2p1pn2/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A95" -> Ecopening("A95", "Dutch", "Dutch, Stonewall", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d57 Nc3 c6", "rnbq1rk1/pp1pb1pp/2p1pn2/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A96" -> Ecopening("A96", "Dutch", "Dutch, Classical Variation", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d6", "rnbq1rk1/ppp1b1pp/3ppn2/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A97" -> Ecopening("A97", "Dutch", "Dutch, Ilyin-Genevsky", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d67 Nc3 Qe8", "rnb1qrk1/ppppb1pp/4pn2/5p2/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "A98" -> Ecopening("A98", "Dutch", "Dutch, Ilyin-Genevsky Variation with Qc2", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d67 Nc3 Qe8 Qc2", "rnb1qrk1/ppppb1pp/4pn2/5p2/2PP4/5NP1/PPQ1PPBP/RNB2RK1"),
      "A99" -> Ecopening("A99", "Dutch", "Dutch, Ilyin-Genevsky Variation with b3", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7 Nf3 O-O O-O d67 Nc3 Qe8 b3", "rnb1qrk1/ppppb1pp/4pn2/5p2/2PP4/1P3NP1/P3PPBP/RNBQ1RK1")
    ),
    "Dutch Defense" -> Map(
      "A91" -> Ecopening("A91", "Dutch Defense", "Dutch Defense", "d4 f5 c4 Nf6 g3 e6 Bg2 Be7", "rnbqk2r/ppppb1pp/4pn2/5p2/2PP4/6P1/PP2PPBP/RNBQK1NR")
    ),
    "Uncommon King's Pawn Opening" -> Map(
      "B00" -> Ecopening("B00", "Uncommon King's Pawn Opening", "Uncommon King's Pawn Opening", "e4", "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR")
    ),
    "Scandinavian" -> Map(
      "B01" -> Ecopening("B01", "Scandinavian", "Scandinavian", "e4 d5", "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR")
    ),
    "Alekhine's Defense" -> Map(
      "B02" -> Ecopening("B02", "Alekhine's Defense", "Alekhine's Defense", "e4 Nf6", "rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR"),
      "B03" -> Ecopening("B03", "Alekhine's Defense", "Alekhine's Defense", "e4 Nf6 e5 Nd5 d4", "rnbqkb1r/pppppppp/8/3nP3/3P4/8/PPP2PPP/RNBQKBNR"),
      "B04" -> Ecopening("B04", "Alekhine's Defense", "Alekhine's Defense, Modern", "e4 Nf6 e5 Nd5 d4 d6 Nf3", "rnbqkb1r/ppp1pppp/3p4/3nP3/3P4/5N2/PPP2PPP/RNBQKB1R"),
      "B05" -> Ecopening("B05", "Alekhine's Defense", "Alekhine's Defense, Modern", "e4 Nf6 e5 Nd5 d4 d6 Nf3 Bg4", "rn1qkb1r/ppp1pppp/3p4/3nP3/3P2b1/5N2/PPP2PPP/RNBQKB1R")
    ),
    "Robatsch" -> Map(
      "B06" -> Ecopening("B06", "Robatsch", "Robatsch", "e4 g6", "rnbqkbnr/pppppp1p/6p1/8/4P3/8/PPPP1PPP/RNBQKBNR")
    ),
    "Pirc" -> Map(
      "B07" -> Ecopening("B07", "Pirc", "Pirc", "e4 d6 d4 Nf6", "rnbqkb1r/ppp1pppp/3p1n2/8/3PP3/8/PPP2PPP/RNBQKBNR"),
      "B08" -> Ecopening("B08", "Pirc", "Pirc, Classical", "e4 d6 d4 Nf6 Nc3 g6 Nf3", "rnbqkb1r/ppp1pp1p/3p1np1/8/3PP3/2N2N2/PPP2PPP/R1BQKB1R"),
      "B09" -> Ecopening("B09", "Pirc", "Pirc, Austrian Attack", "e4 d6 d4 Nf6 Nc3 g6 f4", "rnbqkb1r/ppp1pp1p/3p1np1/8/3PPP2/2N5/PPP3PP/R1BQKBNR")
    ),
    "Caro-Kann" -> Map(
      "B10" -> Ecopening("B10", "Caro-Kann", "Caro-Kann", "e4 c6", "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR"),
      "B11" -> Ecopening("B11", "Caro-Kann", "Caro-Kann, Two Knights, 3...Bg4", "e4 c6 Nc3 d5 Nf3 Bg4", "rn1qkbnr/pp2pppp/2p5/3p4/4P1b1/2N2N2/PPPP1PPP/R1BQKB1R"),
      "B13" -> Ecopening("B13", "Caro-Kann", "Caro-Kann, Exchange", "e4 c6 d4 d5 exd5 cxd5", "rnbqkbnr/pp2pppp/8/3p4/3P4/8/PPP2PPP/RNBQKBNR"),
      "B14" -> Ecopening("B14", "Caro-Kann", "Caro-Kann, Panov-Botvinnik Attack", "e4 c6 d4 d5 exd5 cxd5 c4 Nf6 Nc3 e6", "rnbqkb1r/pp3ppp/4pn2/3p4/2PP4/2N5/PP3PPP/R1BQKBNR"),
      "B15" -> Ecopening("B15", "Caro-Kann", "Caro-Kann", "e4 c6 d4 d5 Nc3", "rnbqkbnr/pp2pppp/2p5/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR"),
      "B16" -> Ecopening("B16", "Caro-Kann", "Caro-Kann, Bronstein-Larsen Variation", "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Nf6 Nxf6+ gxf6", "rnbqkb1r/pp2pp1p/2p2p2/8/3P4/8/PPP2PPP/R1BQKBNR"),
      "B17" -> Ecopening("B17", "Caro-Kann", "Caro-Kann, Steinitz Variation", "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Nd7", "r1bqkbnr/pp1npppp/2p5/8/3PN3/8/PPP2PPP/R1BQKBNR"),
      "B18" -> Ecopening("B18", "Caro-Kann", "Caro-Kann, Classical", "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5", "rn1qkbnr/pp2pppp/2p5/5b2/3PN3/8/PPP2PPP/R1BQKBNR"),
      "B19" -> Ecopening("B19", "Caro-Kann", "Caro-Kann, Classical", "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5 Ng3 Bg6 h4 h67 Nf3 Nd7", "r2qkbnr/pp1npppp/2p3b1/8/3P3P/6N1/PPP2PP1/R1BQKBNR")
    ),
    "Caro-Kann Defense" -> Map(
      "B12" -> Ecopening("B12", "Caro-Kann Defense", "Caro-Kann Defense", "e4 c6 d4", "rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR")
    ),
    "Sicilian" -> Map(
      "B20" -> Ecopening("B20", "Sicilian", "Sicilian", "e4 c5", "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR"),
      "B21" -> Ecopening("B21", "Sicilian", "Sicilian, 2.f4 and 2.d4", "e4 c5 f4", "rnbqkbnr/pp1ppppp/8/2p5/4PP2/8/PPPP2PP/RNBQKBNR"),
      "B22" -> Ecopening("B22", "Sicilian", "Sicilian, Alapin", "e4 c5 c3", "rnbqkbnr/pp1ppppp/8/2p5/4P3/2P5/PP1P1PPP/RNBQKBNR"),
      "B23" -> Ecopening("B23", "Sicilian", "Sicilian, Closed", "e4 c5 Nc3", "rnbqkbnr/pp1ppppp/8/2p5/4P3/2N5/PPPP1PPP/R1BQKBNR"),
      "B24" -> Ecopening("B24", "Sicilian", "Sicilian, Closed", "e4 c5 Nc3 Nc6 g3", "r1bqkbnr/pp1ppppp/2n5/2p5/4P3/2N3P1/PPPP1P1P/R1BQKBNR"),
      "B25" -> Ecopening("B25", "Sicilian", "Sicilian, Closed", "e4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 d3 d6", "r1bqk1nr/pp2ppbp/2np2p1/2p5/4P3/2NP2P1/PPP2PBP/R1BQK1NR"),
      "B26" -> Ecopening("B26", "Sicilian", "Sicilian, Closed, 6.Be3", "e4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 d3 d6 Be3", "r1bqk1nr/pp2ppbp/2np2p1/2p5/4P3/2NPB1P1/PPP2PBP/R2QK1NR"),
      "B27" -> Ecopening("B27", "Sicilian", "Sicilian", "e4 c5 Nf3", "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R"),
      "B28" -> Ecopening("B28", "Sicilian", "Sicilian, O'Kelly Variation", "e4 c5 Nf3 a6", "rnbqkbnr/1p1ppppp/p7/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R"),
      "B29" -> Ecopening("B29", "Sicilian", "Sicilian, Nimzovich-Rubinstein", "e4 c5 Nf3 Nf6", "rnbqkb1r/pp1ppppp/5n2/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R"),
      "B30" -> Ecopening("B30", "Sicilian", "Sicilian", "e4 c5 Nf3 Nc6", "r1bqkbnr/pp1ppppp/2n5/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R"),
      "B31" -> Ecopening("B31", "Sicilian", "Sicilian, Rossolimo Variation", "e4 c5 Nf3 Nc6 Bb5 g6", "r1bqkbnr/pp1ppp1p/2n3p1/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "B32" -> Ecopening("B32", "Sicilian", "Sicilian", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 e5", "r1bqkbnr/pp1p1ppp/2n5/4p3/3NP3/8/PPP2PPP/RNBQKB1R"),
      "B33" -> Ecopening("B33", "Sicilian", "Sicilian", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4", "r1bqkbnr/pp1ppppp/2n5/8/3NP3/8/PPP2PPP/RNBQKB1R"),
      "B34" -> Ecopening("B34", "Sicilian", "Sicilian, Accelerated Fianchetto", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 Nxc6", "r1bqkbnr/pp1ppp1p/2N3p1/8/4P3/8/PPP2PPP/RNBQKB1R"),
      "B35" -> Ecopening("B35", "Sicilian", "Sicilian, Accelerated Fianchetto, Modern Variation with Bc4", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 Nc3 Bg7 Be3 Nf67 Bc4", "r1bqk1nr/pp1pppbp/2n3p1/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B36" -> Ecopening("B36", "Sicilian", "Sicilian, Accelerated Fianchetto", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 c4", "r1bqkbnr/pp1ppp1p/2n3p1/8/2PNP3/8/PP3PPP/RNBQKB1R"),
      "B37" -> Ecopening("B37", "Sicilian", "Sicilian, Accelerated Fianchetto", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 c4 Bg7", "r1bqk1nr/pp1pppbp/2n3p1/8/2PNP3/8/PP3PPP/RNBQKB1R"),
      "B38" -> Ecopening("B38", "Sicilian", "Sicilian, Accelerated Fianchetto, Maroczy Bind, 6.Be3", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 c4 Bg7 Be3", "r1bqk1nr/pp1pppbp/2n3p1/8/2PNP3/4B3/PP3PPP/RN1QKB1R"),
      "B39" -> Ecopening("B39", "Sicilian", "Sicilian, Accelerated Fianchetto, Breyer Variation", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 c4 Bg7 Be3 Nf67 Nc3 Ng4", "r1bqk1nr/pp1pppbp/2n3p1/8/2PNP3/4B3/PP3PPP/RN1QKB1R"),
      "B40" -> Ecopening("B40", "Sicilian", "Sicilian", "e4 c5 Nf3 e6", "rnbqkbnr/pp1p1ppp/4p3/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R"),
      "B41" -> Ecopening("B41", "Sicilian", "Sicilian, Kan", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 a6", "rnbqkbnr/1p1p1ppp/p3p3/8/3NP3/8/PPP2PPP/RNBQKB1R"),
      "B42" -> Ecopening("B42", "Sicilian", "Sicilian, Kan", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 a6 Bd3", "rnbqkbnr/1p1p1ppp/p3p3/8/3NP3/3B4/PPP2PPP/RNBQK2R"),
      "B43" -> Ecopening("B43", "Sicilian", "Sicilian, Kan, 5.Nc3", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 a6 Nc3", "rnbqkbnr/1p1p1ppp/p3p3/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B44" -> Ecopening("B44", "Sicilian", "Sicilian", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6", "r1bqkbnr/pp1p1ppp/2n1p3/8/3NP3/8/PPP2PPP/RNBQKB1R"),
      "B45" -> Ecopening("B45", "Sicilian", "Sicilian, Taimanov", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3", "r1bqkbnr/pp1p1ppp/2n1p3/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B46" -> Ecopening("B46", "Sicilian", "Sicilian, Taimanov Variation", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 a6", "r1bqkbnr/1p1p1ppp/p1n1p3/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B47" -> Ecopening("B47", "Sicilian", "Sicilian, Taimanov (Bastrikov) Variation", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 Qc7", "r1b1kbnr/ppqp1ppp/2n1p3/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B48" -> Ecopening("B48", "Sicilian", "Sicilian, Taimanov Variation", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 Qc7 Be3", "r1b1kbnr/ppqp1ppp/2n1p3/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B49" -> Ecopening("B49", "Sicilian", "Sicilian, Taimanov Variation", "e4 c5 Nf3 e6 d4 cxd4 Nxd4 Nc6 Nc3 Qc7 Be3 a67 Be2", "r1b1kbnr/ppqp1ppp/2n1p3/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B50" -> Ecopening("B50", "Sicilian", "Sicilian", "e4 c5 Nf3 d6", "rnbqkbnr/pp2pppp/3p4/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R"),
      "B51" -> Ecopening("B51", "Sicilian", "Sicilian, Canal-Sokolsky (Rossolimo) Attack", "e4 c5 Nf3 d6 Bb5+", "rnbqkbnr/pp2pppp/3p4/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "B52" -> Ecopening("B52", "Sicilian", "Sicilian, Canal-Sokolsky (Rossolimo) Attack", "e4 c5 Nf3 d6 Bb5+ Bd7", "rn1qkbnr/pp1bpppp/3p4/1Bp5/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "B53" -> Ecopening("B53", "Sicilian", "Sicilian", "e4 c5 Nf3 d6 d4 cxd4 Qxd4", "rnbqkbnr/pp2pppp/3p4/8/3QP3/5N2/PPP2PPP/RNB1KB1R"),
      "B54" -> Ecopening("B54", "Sicilian", "Sicilian", "e4 c5 Nf3 d6 d4 cxd4 Nxd4", "rnbqkbnr/pp2pppp/3p4/8/3NP3/8/PPP2PPP/RNBQKB1R"),
      "B55" -> Ecopening("B55", "Sicilian", "Sicilian, Prins Variation, Venice Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 f3 e5 Bb5+", "rnbqkb1r/pp3ppp/3p1n2/1B2p3/3NP3/5P2/PPP3PP/RNBQK2R"),
      "B56" -> Ecopening("B56", "Sicilian", "Sicilian", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3", "rnbqkb1r/pp2pppp/3p1n2/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B57" -> Ecopening("B57", "Sicilian", "Sicilian", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bc4", "r1bqkb1r/pp2pppp/2np1n2/8/2BNP3/2N5/PPP2PPP/R1BQK2R"),
      "B58" -> Ecopening("B58", "Sicilian", "Sicilian", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 d6 Be2", "r1bqkb1r/pp2pppp/2np1n2/8/3NP3/2N5/PPP1BPPP/R1BQK2R"),
      "B59" -> Ecopening("B59", "Sicilian", "Sicilian, Boleslavsky Variation, 7.Nb3", "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 d6 Be2 e57 Nb3", "r1bqkb1r/pp2pppp/2np1n2/8/3NP3/2N5/PPP1BPPP/R1BQK2R"),
      "B60" -> Ecopening("B60", "Sicilian", "Sicilian, Richter-Rauzer", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5", "r1bqkb1r/pp2pppp/2np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B61" -> Ecopening("B61", "Sicilian", "Sicilian, Richter-Rauzer, Larsen Variation, 7.Qd2", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 Bd77 Qd2", "r1bqkb1r/pp2pppp/2np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B62" -> Ecopening("B62", "Sicilian", "Sicilian, Richter-Rauzer", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e6", "r1bqkb1r/pp3ppp/2nppn2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B63" -> Ecopening("B63", "Sicilian", "Sicilian, Richter-Rauzer Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2", "r1bqkb1r/pp2pppp/2np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B64" -> Ecopening("B64", "Sicilian", "Sicilian, Richter-Rauzer Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2 Be7 O-O-O O-O f4", "r1bqkb1r/pp2pppp/2np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B65" -> Ecopening("B65", "Sicilian", "Sicilian, Richter-Rauzer Attack, 7...Be7 Defense, 9...Nxd4", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2 Be7 O-O-O O-O f4 Nxd4 Qxd4", "r1bqkb1r/pp2pppp/3p1n2/6B1/3QP3/2N5/PPP2PPP/R3KB1R"),
      "B66" -> Ecopening("B66", "Sicilian", "Sicilian, Richter-Rauzer Attack, 7...a6", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2 a6", "r1bqkb1r/1p2pppp/p1np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B67" -> Ecopening("B67", "Sicilian", "Sicilian, Richter-Rauzer Attack, 7...a6 Defense, 8...Bd7", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2 a6 O-O-O Bd7", "r1bqkb1r/1p2pppp/p1np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B68" -> Ecopening("B68", "Sicilian", "Sicilian, Richter-Rauzer Attack, 7...a6 Defense, 9...Be7", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2 a6 O-O-O Bd7 f4 Be7", "r1bqkb1r/1p2pppp/p1np1n2/6B1/3NPP2/2N5/PPP3PP/R2QKB1R"),
      "B69" -> Ecopening("B69", "Sicilian", "Sicilian, Richter-Rauzer Attack, 7...a6 Defense, 11.Bxf6", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5 e67 Qd2 a6 O-O-O Bd7 f4 Be7 Nf3 b5 Bxf6", "r1bqkb1r/4pppp/p1np1B2/1p6/3NPP2/2N5/PPP3PP/R2QKB1R"),
      "B70" -> Ecopening("B70", "Sicilian", "Sicilian, Dragon Variation", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6", "rnbqkb1r/pp2pp1p/3p1np1/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B71" -> Ecopening("B71", "Sicilian", "Sicilian, Dragon, Levenfish Variation", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 f4", "rnbqkb1r/pp2pp1p/3p1np1/8/3NPP2/2N5/PPP3PP/R1BQKB1R"),
      "B72" -> Ecopening("B72", "Sicilian", "Sicilian, Dragon", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3", "rnbqkb1r/pp2pp1p/3p1np1/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B73" -> Ecopening("B73", "Sicilian", "Sicilian, Dragon, Classical", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 Be2 Nc6 O-O", "r1bqkb1r/pp2pp1p/2np1np1/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B74" -> Ecopening("B74", "Sicilian", "Sicilian, Dragon, Classical", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 Be2 Nc6 O-O O-O Nb3", "r1bqkb1r/pp2pp1p/2np1np1/8/4P3/1NN1B3/PPP2PPP/R2QKB1R"),
      "B75" -> Ecopening("B75", "Sicilian", "Sicilian, Dragon, Yugoslav Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 f3", "rnbqkb1r/pp2pp1p/3p1np1/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B76" -> Ecopening("B76", "Sicilian", "Sicilian, Dragon, Yugoslav Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 f3 O-O", "rnbqkb1r/pp2pp1p/3p1np1/8/3NP3/2N1B3/PPP2PPP/R2QKB1R"),
      "B77" -> Ecopening("B77", "Sicilian", "Sicilian, Dragon, Yugoslav Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 f3 O-O Qd2 Nc6 Bc4", "r1bqkb1r/pp2pp1p/2np1np1/8/2BNP3/2N1B3/PPP2PPP/R2QK2R"),
      "B78" -> Ecopening("B78", "Sicilian", "Sicilian, Dragon, Yugoslav Attack, 10.castle long", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 f3 O-O Qd2 Nc6 Bc4 Bd7 O-O-O", "r2qkb1r/pp1bpp1p/2np1np1/8/2BNP3/2N1B3/PPP2PPP/R2QK2R"),
      "B79" -> Ecopening("B79", "Sicilian", "Sicilian, Dragon, Yugoslav Attack, 12.h4", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg77 f3 O-O Qd2 Nc6 Bc4 Bd7 O-O-O Qa5 Bb3 Rfc8 h4", "r2qkb1r/pp1bpp1p/2np1np1/8/3NP3/1BN1B3/PPP2PPP/R2QK2R"),
      "B80" -> Ecopening("B80", "Sicilian", "Sicilian, Scheveningen", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6", "rnbqkb1r/pp3ppp/3ppn2/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B81" -> Ecopening("B81", "Sicilian", "Sicilian, Scheveningen, Keres Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 g4", "rnbqkb1r/pp3ppp/3ppn2/8/3NP1P1/2N5/PPP2P1P/R1BQKB1R"),
      "B82" -> Ecopening("B82", "Sicilian", "Sicilian, Scheveningen", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 f4", "rnbqkb1r/pp3ppp/3ppn2/8/3NPP2/2N5/PPP3PP/R1BQKB1R"),
      "B83" -> Ecopening("B83", "Sicilian", "Sicilian", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Be2", "rnbqkb1r/pp3ppp/3ppn2/8/3NP3/2N5/PPP1BPPP/R1BQK2R"),
      "B84" -> Ecopening("B84", "Sicilian", "Sicilian, Scheveningen", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Be2 a6", "rnbqkb1r/1p3ppp/p2ppn2/8/3NP3/2N5/PPP1BPPP/R1BQK2R"),
      "B85" -> Ecopening("B85", "Sicilian", "Sicilian, Scheveningen, Classical", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Be2 a67 O-O Qc7 f4 Nc6", "r1b1kb1r/ppq2ppp/2nppn2/8/3NPP2/2N5/PPP1B1PP/R1BQK2R"),
      "B86" -> Ecopening("B86", "Sicilian", "Sicilian, Fischer-Sozin Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Bc4", "rnbqkb1r/pp3ppp/3ppn2/8/2BNP3/2N5/PPP2PPP/R1BQK2R"),
      "B87" -> Ecopening("B87", "Sicilian", "Sicilian, Fischer-Sozin with ...a6 and ...b5", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Bc4 a67 Bb3 b5", "rnbqkb1r/p4ppp/3ppn2/1p6/2BNP3/2N5/PPP2PPP/R1BQK2R"),
      "B88" -> Ecopening("B88", "Sicilian", "Sicilian, Fischer-Sozin Attack", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Bc4 Nc6", "r1bqkb1r/pp3ppp/2nppn2/8/2BNP3/2N5/PPP2PPP/R1BQK2R"),
      "B89" -> Ecopening("B89", "Sicilian", "Sicilian", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6 Bc4 Nc67 Be3", "rnbqkb1r/pp3ppp/3ppn2/8/2BNP3/2N5/PPP2PPP/R1BQK2R"),
      "B90" -> Ecopening("B90", "Sicilian", "Sicilian, Najdorf", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6", "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N5/PPP2PPP/R1BQKB1R"),
      "B91" -> Ecopening("B91", "Sicilian", "Sicilian, Najdorf, Zagreb (Fianchetto) Variation", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 g3", "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N3P1/PPP2P1P/R1BQKB1R"),
      "B92" -> Ecopening("B92", "Sicilian", "Sicilian, Najdorf, Opocensky Variation", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be2", "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N5/PPP1BPPP/R1BQK2R"),
      "B93" -> Ecopening("B93", "Sicilian", "Sicilian, Najdorf, 6.f4", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 f4", "rnbqkb1r/1p2pppp/p2p1n2/8/3NPP2/2N5/PPP3PP/R1BQKB1R"),
      "B94" -> Ecopening("B94", "Sicilian", "Sicilian, Najdorf", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5", "rnbqkb1r/1p2pppp/p2p1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B95" -> Ecopening("B95", "Sicilian", "Sicilian, Najdorf, 6...e6", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e6", "rnbqkb1r/1p3ppp/p2ppn2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B96" -> Ecopening("B96", "Sicilian", "Sicilian, Najdorf", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e67 f4", "rnbqkb1r/1p2pppp/p2p1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B97" -> Ecopening("B97", "Sicilian", "Sicilian, Najdorf", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e67 f4 Qb6", "rnb1kb1r/1p2pppp/pq1p1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B98" -> Ecopening("B98", "Sicilian", "Sicilian, Najdorf", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e67 f4 Be7", "rnbqkb1r/1p2pppp/p2p1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R"),
      "B99" -> Ecopening("B99", "Sicilian", "Sicilian, Najdorf, 7...Be7 Main line", "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e67 f4 Be7 Qf3 Qc7 O-O-O Nbd7", "rnb1kb1r/1pq1pppp/p2p1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R")
    ),
    "French Defense" -> Map(
      "C00" -> Ecopening("C00", "French Defense", "French Defense", "e4 e6", "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR")
    ),
    "French" -> Map(
      "C01" -> Ecopening("C01", "French", "French, Exchange", "e4 e6 d4 d5 exd5 exd5 Nc3 Nf6 Bg5", "rnbqkb1r/ppp2ppp/5n2/3p2B1/3P4/2N5/PPP2PPP/R2QKBNR"),
      "C02" -> Ecopening("C02", "French", "French, Advance", "e4 e6 d4 d5 e5", "rnbqkbnr/ppp2ppp/4p3/3pP3/3P4/8/PPP2PPP/RNBQKBNR"),
      "C03" -> Ecopening("C03", "French", "French, Tarrasch", "e4 e6 d4 d5 Nd2", "rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/8/PPPN1PPP/R1BQKBNR"),
      "C04" -> Ecopening("C04", "French", "French, Tarrasch, Guimard Main line", "e4 e6 d4 d5 Nd2 Nc6 Ngf3 Nf6", "r1bqkb1r/ppp2ppp/2n1pn2/3p4/3PP3/5N2/PPPN1PPP/R1BQKB1R"),
      "C05" -> Ecopening("C05", "French", "French, Tarrasch", "e4 e6 d4 d5 Nd2 Nf6", "rnbqkb1r/ppp2ppp/4pn2/3p4/3PP3/8/PPPN1PPP/R1BQKBNR"),
      "C06" -> Ecopening("C06", "French", "French, Tarrasch", "e4 e6 d4 d5 Nd2 Nf6 e5 Nfd7 Bd3 c5 c3 Nc67 Ne2 cxd4 cxd4", "rnbqkb1r/pp1n1ppp/4p3/3pP3/3P4/3B4/PP1N1PPP/R1BQK1NR"),
      "C07" -> Ecopening("C07", "French", "French, Tarrasch", "e4 e6 d4 d5 Nd2 c5", "rnbqkbnr/pp3ppp/4p3/2pp4/3PP3/8/PPPN1PPP/R1BQKBNR"),
      "C08" -> Ecopening("C08", "French", "French, Tarrasch, Open, 4.ed ed", "e4 e6 d4 d5 Nd2 c5 exd5 exd5", "rnbqkbnr/pp3ppp/8/2pp4/3P4/8/PPPN1PPP/R1BQKBNR"),
      "C09" -> Ecopening("C09", "French", "French, Tarrasch, Open Variation, Main line", "e4 e6 d4 d5 Nd2 c5 exd5 exd5 Ngf3 Nc6", "r1bqkbnr/pp3ppp/2n5/2pp4/3P4/5N2/PPPN1PPP/R1BQKB1R"),
      "C10" -> Ecopening("C10", "French", "French", "e4 e6 d4 d5 Nc3", "rnbqkbnr/ppp2ppp/4p3/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR"),
      "C11" -> Ecopening("C11", "French", "French", "e4 e6 d4 d5 Nc3 Nf6", "rnbqkb1r/ppp2ppp/4pn2/3p4/3PP3/2N5/PPP2PPP/R1BQKBNR"),
      "C12" -> Ecopening("C12", "French", "French, McCutcheon", "e4 e6 d4 d5 Nc3 Nf6 Bg5 Bb4", "rnbqk2r/ppp2ppp/4pn2/3p2B1/1b1PP3/2N5/PPP2PPP/R2QKBNR"),
      "C13" -> Ecopening("C13", "French", "French", "e4 e6 d4 d5 Nc3 Nf6 Bg5 Be7", "rnbqk2r/ppp1bppp/4pn2/3p2B1/3PP3/2N5/PPP2PPP/R2QKBNR"),
      "C14" -> Ecopening("C14", "French", "French, Classical", "e4 e6 d4 d5 Nc3 Nf6 Bg5 Be7 e5 Nfd7 Bxe7 Qxe7", "rnb1k2r/pppnqppp/4p3/3pP3/3P4/2N5/PPP2PPP/R2QKBNR"),
      "C15" -> Ecopening("C15", "French", "French, Winawer", "e4 e6 d4 d5 Nc3 Bb4", "rnbqk1nr/ppp2ppp/4p3/3p4/1b1PP3/2N5/PPP2PPP/R1BQKBNR"),
      "C16" -> Ecopening("C16", "French", "French, Winawer", "e4 e6 d4 d5 Nc3 Bb4 e5", "rnbqk1nr/ppp2ppp/4p3/3pP3/1b1P4/2N5/PPP2PPP/R1BQKBNR"),
      "C17" -> Ecopening("C17", "French", "French, Winawer, Advance", "e4 e6 d4 d5 Nc3 Bb4 e5 c5", "rnbqk1nr/pp3ppp/4p3/2ppP3/1b1P4/2N5/PPP2PPP/R1BQKBNR"),
      "C18" -> Ecopening("C18", "French", "French, Winawer", "e4 e6 d4 d5 Nc3 Bb4 e5 c5 a3 Bxc3+ bxc3", "rnbqk1nr/pp3ppp/4p3/2ppP3/3P4/P1P5/2P2PPP/R1BQKBNR"),
      "C19" -> Ecopening("C19", "French", "French, Winawer, Advance", "e4 e6 d4 d5 Nc3 Bb4 e5 c5 a3 Bxc3+ bxc3 Ne7", "rnbqk2r/pp2nppp/4p3/2ppP3/3P4/P1P5/2P2PPP/R1BQKBNR")
    ),
    "King's Pawn Game" -> Map(
      "C20" -> Ecopening("C20", "King's Pawn Game", "King's Pawn Game", "e4 e5", "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR"),
      "C44" -> Ecopening("C44", "King's Pawn Game", "King's Pawn Game", "e4 e5 Nf3 Nc6", "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R")
    ),
    "Center Game" -> Map(
      "C21" -> Ecopening("C21", "Center Game", "Center Game", "e4 e5 d4 exd4", "rnbqkbnr/pppp1ppp/8/8/3pP3/8/PPP2PPP/RNBQKBNR"),
      "C22" -> Ecopening("C22", "Center Game", "Center Game", "e4 e5 d4 exd4 Qxd4 Nc6", "r1bqkbnr/pppp1ppp/2n5/8/3QP3/8/PPP2PPP/RNB1KBNR")
    ),
    "Bishop's Opening" -> Map(
      "C23" -> Ecopening("C23", "Bishop's Opening", "Bishop's Opening", "e4 e5 Bc4", "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR"),
      "C24" -> Ecopening("C24", "Bishop's Opening", "Bishop's Opening", "e4 e5 Bc4 Nf6", "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR")
    ),
    "Vienna" -> Map(
      "C25" -> Ecopening("C25", "Vienna", "Vienna", "e4 e5 Nc3", "rnbqkbnr/pppp1ppp/8/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR"),
      "C26" -> Ecopening("C26", "Vienna", "Vienna", "e4 e5 Nc3 Nf6", "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR")
    ),
    "Vienna Game" -> Map(
      "C27" -> Ecopening("C27", "Vienna Game", "Vienna Game", "e4 e5 Nc3 Nf6 Bc4 Nxe4", "rnbqkb1r/pppp1ppp/8/4p3/2B1n3/2N5/PPPP1PPP/R1BQK1NR"),
      "C28" -> Ecopening("C28", "Vienna Game", "Vienna Game", "e4 e5 Nc3 Nf6 Bc4 Nc6", "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/2N5/PPPP1PPP/R1BQK1NR")
    ),
    "Vienna Gambit" -> Map(
      "C29" -> Ecopening("C29", "Vienna Gambit", "Vienna Gambit", "e4 e5 Nc3 Nf6 f4", "rnbqkb1r/pppp1ppp/5n2/4p3/4PP2/2N5/PPPP2PP/R1BQKBNR")
    ),
    "King's Gambit Declined" -> Map(
      "C30" -> Ecopening("C30", "King's Gambit Declined", "King's Gambit Declined", "e4 e5 f4", "rnbqkbnr/pppp1ppp/8/4p3/4PP2/8/PPPP2PP/RNBQKBNR"),
      "C31" -> Ecopening("C31", "King's Gambit Declined", "King's Gambit Declined, Falkbeer Counter Gambit", "e4 e5 f4 d5", "rnbqkbnr/ppp2ppp/8/3pp3/4PP2/8/PPPP2PP/RNBQKBNR"),
      "C32" -> Ecopening("C32", "King's Gambit Declined", "King's Gambit Declined, Falkbeer Counter Gambit", "e4 e5 f4 d5 exd5 e4 d3 Nf6", "rnbqkb1r/ppp2ppp/5n2/3P4/4pP2/3P4/PPP3PP/RNBQKBNR")
    ),
    "King's Gambit Accepted" -> Map(
      "C33" -> Ecopening("C33", "King's Gambit Accepted", "King's Gambit Accepted", "e4 e5 f4 exf4", "rnbqkbnr/pppp1ppp/8/8/4Pp2/8/PPPP2PP/RNBQKBNR"),
      "C34" -> Ecopening("C34", "King's Gambit Accepted", "King's Gambit Accepted", "e4 e5 f4 exf4 Nf3", "rnbqkbnr/pppp1ppp/8/8/4Pp2/5N2/PPPP2PP/RNBQKB1R"),
      "C35" -> Ecopening("C35", "King's Gambit Accepted", "King's Gambit Accepted, Cunningham", "e4 e5 f4 exf4 Nf3 Be7", "rnbqk1nr/ppppbppp/8/8/4Pp2/5N2/PPPP2PP/RNBQKB1R"),
      "C36" -> Ecopening("C36", "King's Gambit Accepted", "King's Gambit Accepted, Abbazia Defense", "e4 e5 f4 exf4 Nf3 d5", "rnbqkbnr/ppp2ppp/8/3p4/4Pp2/5N2/PPPP2PP/RNBQKB1R"),
      "C37" -> Ecopening("C37", "King's Gambit Accepted", "King's Gambit Accepted", "e4 e5 f4 exf4 Nf3 g5 Nc3", "rnbqkbnr/pppp1p1p/8/6p1/4Pp2/2N2N2/PPPP2PP/R1BQKB1R"),
      "C38" -> Ecopening("C38", "King's Gambit Accepted", "King's Gambit Accepted", "e4 e5 f4 exf4 Nf3 g5 Bc4 Bg7", "rnbqk1nr/pppp1pbp/8/6p1/2B1Pp2/5N2/PPPP2PP/RNBQK2R"),
      "C39" -> Ecopening("C39", "King's Gambit Accepted", "King's Gambit Accepted", "e4 e5 f4 exf4 Nf3 g5 h4", "rnbqkbnr/pppp1p1p/8/6p1/4Pp1P/5N2/PPPP2P1/RNBQKB1R")
    ),
    "King's Knight Opening" -> Map(
      "C40" -> Ecopening("C40", "King's Knight Opening", "King's Knight Opening", "e4 e5 Nf3", "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R")
    ),
    "Philidor Defense" -> Map(
      "C41" -> Ecopening("C41", "Philidor Defense", "Philidor Defense", "e4 e5 Nf3 d6", "rnbqkbnr/ppp2ppp/3p4/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R")
    ),
    "Petrov Defense" -> Map(
      "C42" -> Ecopening("C42", "Petrov Defense", "Petrov Defense", "e4 e5 Nf3 Nf6", "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R")
    ),
    "Petrov" -> Map(
      "C43" -> Ecopening("C43", "Petrov", "Petrov, Modern Attack", "e4 e5 Nf3 Nf6 d4 exd4 e5 Ne4 Qxd4", "rnbqkb1r/pppp1ppp/8/4P3/3Qn3/5N2/PPP2PPP/RNB1KB1R")
    ),
    "Scotch Game" -> Map(
      "C45" -> Ecopening("C45", "Scotch Game", "Scotch Game", "e4 e5 Nf3 Nc6 d4 exd4 Nxd4", "r1bqkbnr/pppp1ppp/2n5/8/3NP3/8/PPP2PPP/RNBQKB1R")
    ),
    "Three Knights" -> Map(
      "C46" -> Ecopening("C46", "Three Knights", "Three Knights", "e4 e5 Nf3 Nc6 Nc3", "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R")
    ),
    "Four Knights" -> Map(
      "C47" -> Ecopening("C47", "Four Knights", "Four Knights", "e4 e5 Nf3 Nc6 Nc3 Nf6", "r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R"),
      "C48" -> Ecopening("C48", "Four Knights", "Four Knights", "e4 e5 Nf3 Nc6 Nc3 Nf6 Bb5", "r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/2N2N2/PPPP1PPP/R1BQK2R"),
      "C49" -> Ecopening("C49", "Four Knights", "Four Knights", "e4 e5 Nf3 Nc6 Nc3 Nf6 Bb5 Bb4", "r1bqk2r/pppp1ppp/2n2n2/1B2p3/1b2P3/2N2N2/PPPP1PPP/R1BQK2R")
    ),
    "Giuoco Piano" -> Map(
      "C50" -> Ecopening("C50", "Giuoco Piano", "Giuoco Piano", "e4 e5 Nf3 Nc6 Bc4 Bc5", "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R"),
      "C53" -> Ecopening("C53", "Giuoco Piano", "Giuoco Piano", "e4 e5 Nf3 Nc6 Bc4 Bc5 c3", "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/2P2N2/PP1P1PPP/RNBQK2R"),
      "C54" -> Ecopening("C54", "Giuoco Piano", "Giuoco Piano", "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d4 exd4 cxd4", "r1bqk2r/pppp1ppp/2n2n2/2b5/2BPP3/5N2/PP3PPP/RNBQK2R")
    ),
    "Evans Gambit" -> Map(
      "C51" -> Ecopening("C51", "Evans Gambit", "Evans Gambit", "e4 e5 Nf3 Nc6 Bc4 Bc5 b4", "r1bqk1nr/pppp1ppp/2n5/2b1p3/1PB1P3/5N2/P1PP1PPP/RNBQK2R"),
      "C52" -> Ecopening("C52", "Evans Gambit", "Evans Gambit", "e4 e5 Nf3 Nc6 Bc4 Bc5 b4 Bxb4 c3 Ba5", "r1bqk1nr/pppp1ppp/2n5/b3p3/2B1P3/2P2N2/P2P1PPP/RNBQK2R")
    ),
    "Two Knights Defense" -> Map(
      "C55" -> Ecopening("C55", "Two Knights Defense", "Two Knights Defense", "e4 e5 Nf3 Nc6 Bc4 Nf6", "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R")
    ),
    "Two Knights" -> Map(
      "C56" -> Ecopening("C56", "Two Knights", "Two Knights", "e4 e5 Nf3 Nc6 Bc4 Nf6 d4 exd4 O-O Nxe4", "r1bqkb1r/pppp1ppp/2n5/8/2Bpn3/5N2/PPP2PPP/RNBQ1RK1"),
      "C57" -> Ecopening("C57", "Two Knights", "Two Knights", "e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5", "r1bqkb1r/pppp1ppp/2n2n2/4p1N1/2B1P3/8/PPPP1PPP/RNBQK2R"),
      "C58" -> Ecopening("C58", "Two Knights", "Two Knights", "e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Na5", "r1bqkb1r/ppp2ppp/5n2/n2Pp1N1/2B5/8/PPPP1PPP/RNBQK2R"),
      "C59" -> Ecopening("C59", "Two Knights", "Two Knights", "e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Na5 Bb5+ c67 dxc6 bxc6 Be2 h6", "r1bqkb1r/ppp2ppp/5n2/nB1Pp1N1/8/8/PPPP1PPP/RNBQK2R")
    ),
    "Ruy Lopez" -> Map(
      "C60" -> Ecopening("C60", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5", "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "C61" -> Ecopening("C61", "Ruy Lopez", "Ruy Lopez, Bird's Defense", "e4 e5 Nf3 Nc6 Bb5 Nd4", "r1bqkbnr/pppp1ppp/8/1B2p3/3nP3/5N2/PPPP1PPP/RNBQK2R"),
      "C62" -> Ecopening("C62", "Ruy Lopez", "Ruy Lopez, Old Steinitz Defense", "e4 e5 Nf3 Nc6 Bb5 d6", "r1bqkbnr/ppp2ppp/2np4/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "C63" -> Ecopening("C63", "Ruy Lopez", "Ruy Lopez, Schliemann Defense", "e4 e5 Nf3 Nc6 Bb5 f5", "r1bqkbnr/pppp2pp/2n5/1B2pp2/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "C64" -> Ecopening("C64", "Ruy Lopez", "Ruy Lopez, Classical", "e4 e5 Nf3 Nc6 Bb5 Bc5", "r1bqk1nr/pppp1ppp/2n5/1Bb1p3/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "C65" -> Ecopening("C65", "Ruy Lopez", "Ruy Lopez, Berlin Defense", "e4 e5 Nf3 Nc6 Bb5 Nf6", "r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "C66" -> Ecopening("C66", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 Nf6 O-O d6", "r1bqkb1r/ppp2ppp/2np1n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C67" -> Ecopening("C67", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 Nf6 O-O Nxe4", "r1bqkb1r/pppp1ppp/2n5/1B2p3/4n3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C68" -> Ecopening("C68", "Ruy Lopez", "Ruy Lopez, Exchange", "e4 e5 Nf3 Nc6 Bb5 a6 Bxc6", "r1bqkbnr/1ppp1ppp/p1B5/4p3/4P3/5N2/PPPP1PPP/RNBQK2R"),
      "C69" -> Ecopening("C69", "Ruy Lopez", "Ruy Lopez, Exchange, Gligoric Variation, 6.d4", "e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 dc O-O f6 d4", "r1bqkbnr/1ppp2pp/p1B2p2/4p3/3PP3/5N2/PPP2PPP/RNBQK2R"),
      "C70" -> Ecopening("C70", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4", "r1bqkbnr/1ppp1ppp/p1n5/4p3/B3P3/5N2/PPPP1PPP/RNBQK2R"),
      "C71" -> Ecopening("C71", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 d6", "r1bqkbnr/1pp2ppp/p1np4/4p3/B3P3/5N2/PPPP1PPP/RNBQK2R"),
      "C72" -> Ecopening("C72", "Ruy Lopez", "Ruy Lopez, Modern Steinitz Defense, 5.O-O", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 d6 O-O", "r1bqkbnr/1pp2ppp/p1np4/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C73" -> Ecopening("C73", "Ruy Lopez", "Ruy Lopez, Modern Steinitz Defense", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 d6 Bxc6+ bxc6 d4", "r1bqkbnr/2p2ppp/p1pp4/4p3/3PP3/5N2/PPP2PPP/RNBQK2R"),
      "C74" -> Ecopening("C74", "Ruy Lopez", "Ruy Lopez, Modern Steinitz Defense", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 d6 c3", "r1bqkbnr/1pp2ppp/p1np4/4p3/B3P3/2P2N2/PP1P1PPP/RNBQK2R"),
      "C75" -> Ecopening("C75", "Ruy Lopez", "Ruy Lopez, Modern Steinitz Defense", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 d6 c3 Bd7", "r2qkbnr/1ppb1ppp/p1np4/4p3/B3P3/2P2N2/PP1P1PPP/RNBQK2R"),
      "C76" -> Ecopening("C76", "Ruy Lopez", "Ruy Lopez, Modern Steinitz Defense, Fianchetto Variation", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 d6 c3 Bd7 d4 g6", "r2qkbnr/1ppb1p1p/p1np2p1/4p3/B2PP3/2P2N2/PP3PPP/RNBQK2R"),
      "C77" -> Ecopening("C77", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6", "r1bqkb1r/1ppp1ppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQK2R"),
      "C78" -> Ecopening("C78", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O", "r1bqkb1r/1ppp1ppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C79" -> Ecopening("C79", "Ruy Lopez", "Ruy Lopez, Steinitz Defense Deferred", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O d6", "r1bqkb1r/1pp2ppp/p1np1n2/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C80" -> Ecopening("C80", "Ruy Lopez", "Ruy Lopez, Open", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Nxe4", "r1bqkb1r/1ppp1ppp/p1n5/4p3/B3n3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C81" -> Ecopening("C81", "Ruy Lopez", "Ruy Lopez, Open, Howell Attack", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Nxe4 d4 b57 Bb3 d5 dxe5 Be6", "r2qkb1r/1pp2ppp/p1n1b3/3pP3/B3n3/5N2/PPP2PPP/RNBQ1RK1"),
      "C82" -> Ecopening("C82", "Ruy Lopez", "Ruy Lopez, Open", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Nxe4 d4 b57 Bb3 d5 dxe5 Be6 c3", "r2qkb1r/1pp2ppp/p1n1b3/3pP3/B3n3/2P2N2/PP3PPP/RNBQ1RK1"),
      "C83" -> Ecopening("C83", "Ruy Lopez", "Ruy Lopez, Open", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Nxe4 d4 b57 Bb3 d5 dxe5 Be6", "r2qkb1r/1pp2ppp/p1n1b3/3pP3/B3n3/5N2/PPP2PPP/RNBQ1RK1"),
      "C84" -> Ecopening("C84", "Ruy Lopez", "Ruy Lopez, Closed", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7", "r1bqk2r/1pppbppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C85" -> Ecopening("C85", "Ruy Lopez", "Ruy Lopez, Exchange Variation Doubly Deferred (DERLD)", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Bxc6 dxc6", "r1bqk2r/1pp1bppp/p1p2n2/4p3/4P3/5N2/PPPP1PPP/RNBQ1RK1"),
      "C86" -> Ecopening("C86", "Ruy Lopez", "Ruy Lopez, Worrall Attack", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Qe2", "r1bqk2r/1pppbppp/p1n2n2/4p3/B3P3/5N2/PPPPQPPP/RNB2RK1"),
      "C87" -> Ecopening("C87", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 d6", "r1bqk2r/1pp1bppp/p1np1n2/4p3/B3P3/5N2/PPPP1PPP/RNBQR1K1"),
      "C88" -> Ecopening("C88", "Ruy Lopez", "Ruy Lopez", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3", "r1bqk2r/1pppbppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQR1K1"),
      "C89" -> Ecopening("C89", "Ruy Lopez", "Ruy Lopez, Marshall", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d5", "r1bq1rk1/1pp1bppp/p1n2n2/3pp3/B3P3/2P2N2/PP1P1PPP/RNBQR1K1"),
      "C90" -> Ecopening("C90", "Ruy Lopez", "Ruy Lopez, Closed", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6", "r1bq1rk1/1pp1bppp/p1np1n2/4p3/B3P3/2P2N2/PP1P1PPP/RNBQR1K1"),
      "C91" -> Ecopening("C91", "Ruy Lopez", "Ruy Lopez, Closed", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 d4", "r1bq1rk1/1pp1bppp/p1np1n2/4p3/B2PP3/2P2N2/PP3PPP/RNBQR1K1"),
      "C92" -> Ecopening("C92", "Ruy Lopez", "Ruy Lopez, Closed", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3", "r1bq1rk1/1pp1bppp/p1np1n2/4p3/B3P3/2P2N1P/PP1P1PP1/RNBQR1K1"),
      "C93" -> Ecopening("C93", "Ruy Lopez", "Ruy Lopez, Closed, Smyslov Defense", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 h6", "r1bq1rk1/1pp1bpp1/p1np1n1p/4p3/B3P3/2P2N1P/PP1P1PP1/RNBQR1K1"),
      "C94" -> Ecopening("C94", "Ruy Lopez", "Ruy Lopez, Closed, Breyer Defense", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 Nb8", "rnbq1rk1/1pp1bppp/p2p1n2/4p3/B3P3/2P2N1P/PP1P1PP1/RNBQR1K1"),
      "C95" -> Ecopening("C95", "Ruy Lopez", "Ruy Lopez, Closed, Breyer", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 Nb8 d4", "rnbq1rk1/1pp1bppp/p2p1n2/4p3/B2PP3/2P2N1P/PP3PP1/RNBQR1K1"),
      "C96" -> Ecopening("C96", "Ruy Lopez", "Ruy Lopez, Closed", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 Na5 Bc2", "r1bq1rk1/1pp1bppp/p2p1n2/n3p3/4P3/2P2N1P/PPBP1PP1/RNBQR1K1"),
      "C97" -> Ecopening("C97", "Ruy Lopez", "Ruy Lopez, Closed, Chigorin", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 Na5 Bc2 c5 d4 Qc7", "r1b2rk1/1pq1bppp/p2p1n2/n1p1p3/3PP3/2P2N1P/PPB2PP1/RNBQR1K1"),
      "C98" -> Ecopening("C98", "Ruy Lopez", "Ruy Lopez, Closed, Chigorin", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 Na5 Bc2 c5 d4 Qc7 Nbd2 Nc6", "r1b2rk1/1pq1bppp/p1np1n2/2p1p3/3PP3/2P2N1P/PPBN1PP1/R1BQR1K1"),
      "C99" -> Ecopening("C99", "Ruy Lopez", "Ruy Lopez, Closed, Chigorin, 12...cd", "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b57 Bb3 O-O c3 d6 h3 Na5 Bc2 c5 d4 Qc7 Nbd2 cxd4 cxd4", "r1b2rk1/1pq1bppp/p2p1n2/n3p3/3PP3/5N1P/PPBN1PP1/R1BQR1K1")
    ),
    "Richter-Veresov Attack" -> Map(
      "D01" -> Ecopening("D01", "Richter-Veresov Attack", "Richter-Veresov Attack", "d4 d5 Nc3 Nf6 Bg5", "rnbqkb1r/ppp1pppp/5n2/3p2B1/3P4/2N5/PPP1PPPP/R2QKBNR")
    ),
    "Torre Attack " -> Map(
      "D03" -> Ecopening("D03", "Torre Attack ", "Torre Attack (Tartakower Variation)", "d4 d5 Nf3 Nf6 Bg5", "rnbqkb1r/ppp1pppp/5n2/3p2B1/3P4/5N2/PPP1PPPP/RN1QKB1R")
    ),
    "Queen's Gambit Declined" -> Map(
      "D06" -> Ecopening("D06", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4", "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR"),
      "D07" -> Ecopening("D07", "Queen's Gambit Declined", "Queen's Gambit Declined, Chigorin Defense", "d4 d5 c4 Nc6", "r1bqkbnr/ppp1pppp/2n5/3p4/2PP4/8/PP2PPPP/RNBQKBNR"),
      "D08" -> Ecopening("D08", "Queen's Gambit Declined", "Queen's Gambit Declined, Albin Counter Gambit", "d4 d5 c4 e5", "rnbqkbnr/ppp2ppp/8/3pp3/2PP4/8/PP2PPPP/RNBQKBNR"),
      "D09" -> Ecopening("D09", "Queen's Gambit Declined", "Queen's Gambit Declined, Albin Counter Gambit, 5.g3", "d4 d5 c4 e5 dxe5 d4 Nf3 Nc6 g3", "r1bqkbnr/ppp2ppp/2n5/4P3/2Pp4/5NP1/PP2PP1P/RNBQKB1R"),
      "D30" -> Ecopening("D30", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6", "rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/8/PP2PPPP/RNBQKBNR"),
      "D31" -> Ecopening("D31", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3", "rnbqkbnr/ppp2ppp/4p3/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR"),
      "D32" -> Ecopening("D32", "Queen's Gambit Declined", "Queen's Gambit Declined, Tarrasch", "d4 d5 c4 e6 Nc3 c5", "rnbqkbnr/pp3ppp/4p3/2pp4/2PP4/2N5/PP2PPPP/R1BQKBNR"),
      "D33" -> Ecopening("D33", "Queen's Gambit Declined", "Queen's Gambit Declined, Tarrasch", "d4 d5 c4 e6 Nc3 c5 cxd5 exd5 Nf3 Nc6 g3", "r1bqkbnr/pp3ppp/2n5/2pp4/3P4/2N2NP1/PP2PP1P/R1BQKB1R"),
      "D34" -> Ecopening("D34", "Queen's Gambit Declined", "Queen's Gambit Declined, Tarrasch", "d4 d5 c4 e6 Nc3 c5 cxd5 exd5 Nf3 Nc6 g3 Nf67 Bg2 Be7", "r1bqk1nr/pp2bppp/2n5/2pp4/3P4/2N2NP1/PP2PP1P/R1BQKB1R"),
      "D35" -> Ecopening("D35", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6", "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR"),
      "D36" -> Ecopening("D36", "Queen's Gambit Declined", "Queen's Gambit Declined, Exchange, Positional line, 6.Qc2", "d4 d5 c4 e6 Nc3 Nf6 cxd5 exd5 Bg5 c6 Qc2", "rnbqkb1r/pp3ppp/2p2n2/3p2B1/3P4/2N5/PPQ1PPPP/R3KBNR"),
      "D37" -> Ecopening("D37", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Nf3", "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D38" -> Ecopening("D38", "Queen's Gambit Declined", "Queen's Gambit Declined, Ragozin Variation", "d4 d5 c4 e6 Nc3 Nf6 Nf3 Bb4", "rnbqk2r/ppp2ppp/4pn2/3p4/1bPP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D39" -> Ecopening("D39", "Queen's Gambit Declined", "Queen's Gambit Declined, Ragozin, Vienna Variation", "d4 d5 c4 e6 Nc3 Nf6 Nf3 Bb4 Bg5 dxc4", "rnbqk2r/ppp2ppp/4pn2/6B1/1bpP4/2N2N2/PP2PPPP/R2QKB1R"),
      "D40" -> Ecopening("D40", "Queen's Gambit Declined", "Queen's Gambit Declined, Semi-Tarrasch", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c5", "rnbqkb1r/pp3ppp/4pn2/2pp4/2PP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D41" -> Ecopening("D41", "Queen's Gambit Declined", "Queen's Gambit Declined, Semi-Tarrasch", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c5 cxd5", "rnbqkb1r/pp3ppp/4pn2/2pP4/3P4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D42" -> Ecopening("D42", "Queen's Gambit Declined", "Queen's Gambit Declined, Semi-Tarrasch, 7.Bd3", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c5 cxd5 Nxd5 e3 Nc67 Bd3", "rnbqkb1r/pp3ppp/4p3/2pn4/3P4/2N1PN2/PP3PPP/R1BQKB1R"),
      "D50" -> Ecopening("D50", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Bg5", "rnbqkb1r/ppp2ppp/4pn2/3p2B1/2PP4/2N5/PP2PPPP/R2QKBNR"),
      "D51" -> Ecopening("D51", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Nbd7", "r1bqkb1r/pppn1ppp/4pn2/3p2B1/2PP4/2N5/PP2PPPP/R2QKBNR"),
      "D52" -> Ecopening("D52", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Nbd7 e3 c6 Nf3", "r1bqkb1r/pp1n1ppp/2p1pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D53" -> Ecopening("D53", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7", "rnbqk2r/ppp1bppp/4pn2/3p2B1/2PP4/2N5/PP2PPPP/R2QKBNR"),
      "D54" -> Ecopening("D54", "Queen's Gambit Declined", "Queen's Gambit Declined, Anti-Neo-Orthodox Variation", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Rc1", "rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1P3/PP3PPP/2RQKBNR"),
      "D55" -> Ecopening("D55", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3", "rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D56" -> Ecopening("D56", "Queen's Gambit Declined", "Queen's Gambit Declined", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h67 Bh4", "rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D57" -> Ecopening("D57", "Queen's Gambit Declined", "Queen's Gambit Declined, Lasker Defense", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h67 Bh4 Ne4 Bxe7 Qxe7", "rnb2rk1/ppp1qppp/4p3/3p4/2PPn3/2N1PN2/PP3PPP/R2QKB1R"),
      "D58" -> Ecopening("D58", "Queen's Gambit Declined", "Queen's Gambit Declined, Tartakower (Makagonov-Bondarevsky) Syst", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h67 Bh4 b6", "rnbq1rk1/p1p1bppp/1p2pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D59" -> Ecopening("D59", "Queen's Gambit Declined", "Queen's Gambit Declined, Tartakower", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h67 Bh4 b6 cxd5 Nxd5", "rnbq1rk1/p1p1bppp/1p2p3/3n2B1/3P4/2N1PN2/PP3PPP/R2QKB1R"),
      "D60" -> Ecopening("D60", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox Defense", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd7", "r1bq1rk1/pppnbppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D61" -> Ecopening("D61", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox, Rubinstein Attack", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Qc2", "rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D62" -> Ecopening("D62", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox, Rubinstein Attack", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Qc2 c5 cxd5", "rnbq1rk1/pp2bppp/4pn2/2pP2B1/3P4/2N1PN2/PP3PPP/R2QKB1R"),
      "D63" -> Ecopening("D63", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox Defense", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1", "rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R"),
      "D64" -> Ecopening("D64", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox, Rubinstein Attack", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1 c6 Qc2", "rnbq1rk1/pp2bppp/2p1pn2/3p2B1/2PP4/2N1PN2/PPQ2PPP/R3KB1R"),
      "D65" -> Ecopening("D65", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox, Rubinstein Attack, Main line", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1 c6 Qc2 a6 cxd5", "rnbq1rk1/1p2bppp/p1p1pn2/3P2B1/3P4/2N1PN2/PPQ2PPP/R3KB1R"),
      "D66" -> Ecopening("D66", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox Defense, Bd3 line", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1 c6 Bd3", "rnbq1rk1/pp2bppp/2p1pn2/3p2B1/2PP4/2NBPN2/PP3PPP/R2QK2R"),
      "D67" -> Ecopening("D67", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox Defense, Bd3 line", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1 c6 Bd3 dxc4 Bxc4 Nd5", "rnbq1rk1/pp2bppp/2p1p3/3n2B1/2BP4/2N1PN2/PP3PPP/R2QK2R"),
      "D68" -> Ecopening("D68", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox Defense, Classical", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1 c6 Bd3 dxc4", "rnbq1rk1/pp2bppp/2p1pn2/6B1/2pP4/2NBPN2/PP3PPP/R2QK2R"),
      "D69" -> Ecopening("D69", "Queen's Gambit Declined", "Queen's Gambit Declined, Orthodox Defense, Classical, 13.de", "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 Nbd77 Rc1 c6 Bd3 dxc4", "rnbq1rk1/pp2bppp/2p1pn2/6B1/2pP4/2NBPN2/PP3PPP/R2QK2R")
    ),
    "Queen's Gambit Declined Slav" -> Map(
      "D10" -> Ecopening("D10", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav", "d4 d5 c4 c6", "rnbqkbnr/pp2pppp/2p5/3p4/2PP4/8/PP2PPPP/RNBQKBNR"),
      "D11" -> Ecopening("D11", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav", "d4 d5 c4 c6 Nf3", "rnbqkbnr/pp2pppp/2p5/3p4/2PP4/5N2/PP2PPPP/RNBQKB1R"),
      "D12" -> Ecopening("D12", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav", "d4 d5 c4 c6 Nf3 Nf6 e3 Bf5", "rn1qkb1r/pp2pppp/2p2n2/3p1b2/2PP4/4PN2/PP3PPP/RNBQKB1R"),
      "D13" -> Ecopening("D13", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav, Exchange Variation", "d4 d5 c4 c6 Nf3 Nf6 cxd5 cxd5", "rnbqkb1r/pp2pppp/5n2/3p4/3P4/5N2/PP2PPPP/RNBQKB1R"),
      "D14" -> Ecopening("D14", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav, Exchange Variation", "d4 d5 c4 c6 Nf3 Nf6 cxd5 cxd5 Nc3 Nc6 Bf4 Bf5", "r2qkb1r/pp2pppp/2n2n2/3p1b2/3P1B2/2N2N2/PP2PPPP/R2QKB1R"),
      "D15" -> Ecopening("D15", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav", "d4 d5 c4 c6 Nf3 Nf6 Nc3", "rnbqkb1r/pp2pppp/2p2n2/3p4/2PP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D16" -> Ecopening("D16", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav", "d4 d5 c4 c6 Nf3 Nf6 Nc3 dxc4 a4", "rnbqkb1r/pp2pppp/2p2n2/8/P1pP4/2N2N2/1P2PPPP/R1BQKB1R"),
      "D17" -> Ecopening("D17", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav", "d4 d5 c4 c6 Nf3 Nf6 Nc3 dxc4 a4 Bf5", "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N2N2/1P2PPPP/R1BQKB1R"),
      "D18" -> Ecopening("D18", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav, Dutch", "d4 d5 c4 c6 Nf3 Nf6 Nc3 dxc4 a4 Bf5 e3", "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N1PN2/1P3PPP/R1BQKB1R"),
      "D19" -> Ecopening("D19", "Queen's Gambit Declined Slav", "Queen's Gambit Declined Slav, Dutch", "d4 d5 c4 c6 Nf3 Nf6 Nc3 dxc4 a4 Bf5 e3 e67 Bxc4 Bb4 O-O O-O Qe2", "rn1qkb1r/pp2pppp/2p2n2/5b2/P1pP4/2N1PN2/1P3PPP/R1BQKB1R")
    ),
    "Queen's Gambit Accepted" -> Map(
      "D20" -> Ecopening("D20", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4", "rnbqkbnr/ppp1pppp/8/8/2pP4/8/PP2PPPP/RNBQKBNR"),
      "D21" -> Ecopening("D21", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4 Nf3", "rnbqkbnr/ppp1pppp/8/8/2pP4/5N2/PP2PPPP/RNBQKB1R"),
      "D22" -> Ecopening("D22", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4 Nf3 a6 e3 Bg4 Bxc4 e6 d5", "rn1qkbnr/1pp2ppp/p3p3/3P4/2B3b1/4PN2/PP3PPP/RNBQK2R"),
      "D23" -> Ecopening("D23", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4 Nf3 Nf6", "rnbqkb1r/ppp1pppp/5n2/8/2pP4/5N2/PP2PPPP/RNBQKB1R"),
      "D24" -> Ecopening("D24", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4 Nf3 Nf6 Nc3", "rnbqkb1r/ppp1pppp/5n2/8/2pP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D25" -> Ecopening("D25", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4 Nf3 Nf6 e3", "rnbqkb1r/ppp1pppp/5n2/8/2pP4/4PN2/PP3PPP/RNBQKB1R"),
      "D26" -> Ecopening("D26", "Queen's Gambit Accepted", "Queen's Gambit Accepted", "d4 d5 c4 dxc4 Nf3 Nf6 e3 e6", "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/4PN2/PP3PPP/RNBQKB1R"),
      "D27" -> Ecopening("D27", "Queen's Gambit Accepted", "Queen's Gambit Accepted, Classical", "d4 d5 c4 dxc4 Nf3 Nf6 e3 e6 Bxc4 c5 O-O a6", "rnbqkb1r/1p3ppp/p3pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1"),
      "D28" -> Ecopening("D28", "Queen's Gambit Accepted", "Queen's Gambit Accepted, Classical", "d4 d5 c4 dxc4 Nf3 Nf6 e3 e6 Bxc4 c5 O-O a67 Qe2", "rnbqkb1r/pp3ppp/4pn2/2p5/2BP4/4PN2/PP3PPP/RNBQ1RK1"),
      "D29" -> Ecopening("D29", "Queen's Gambit Accepted", "Queen's Gambit Accepted, Classical", "d4 d5 c4 dxc4 Nf3 Nf6 e3 e6 Bxc4 c5 O-O a67 Qe2 b5 Bb3 Bb7", "rn1qkb1r/pb3ppp/4pn2/1pp5/3P4/1B2PN2/PP3PPP/RNBQ1RK1")
    ),
    "Queen's Gambit Declined Semi-Slav" -> Map(
      "D43" -> Ecopening("D43", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6", "rnbqkb1r/pp3ppp/2p1pn2/3p4/2PP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D44" -> Ecopening("D44", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6 Bg5 dxc4", "rnbqkb1r/pp3ppp/2p1pn2/6B1/2pP4/2N2N2/PP2PPPP/R2QKB1R"),
      "D45" -> Ecopening("D45", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6 e3", "rnbqkb1r/pp3ppp/2p1pn2/3p4/2PP4/2N1PN2/PP3PPP/R1BQKB1R"),
      "D46" -> Ecopening("D46", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6 e3 Nbd7 Bd3", "r1bqkb1r/pp1n1ppp/2p1pn2/3p4/2PP4/2NBPN2/PP3PPP/R1BQK2R"),
      "D47" -> Ecopening("D47", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6 e3 Nbd7 Bd3 dxc47 Bxc4", "r1bqkb1r/pp1n1ppp/2p1pn2/3p4/2PP4/2NBPN2/PP3PPP/R1BQK2R"),
      "D48" -> Ecopening("D48", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav, Meran", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6 e3 Nbd7 Bd3 dxc47 Bxc4 b5 Bd3 a6", "r1bqkb1r/p2n1ppp/2p1pn2/1p1p4/2PP4/2NBPN2/PP3PPP/R1BQK2R"),
      "D49" -> Ecopening("D49", "Queen's Gambit Declined Semi-Slav", "Queen's Gambit Declined Semi-Slav, Meran", "d4 d5 c4 e6 Nc3 Nf6 Nf3 c6 e3 Nbd7 Bd3 dxc47 Bxc4 b5 Bd3 a6 e4 c5 e5 cxd4 Nxb5", "r1bqkb1r/p2n1ppp/4pn2/1N1pP3/2Pp4/3B1N2/PP3PPP/R1BQK2R")
    ),
    "Neo-Grunfeld Defense" -> Map(
      "D70" -> Ecopening("D70", "Neo-Grunfeld Defense", "Neo-Grunfeld Defense", "d4 Nf6 c4 g6 f3 d5", "rnbqkb1r/ppp1pp1p/5np1/3p4/2PP4/5P2/PP2P1PP/RNBQKBNR")
    ),
    "Neo-Grunfeld" -> Map(
      "D71" -> Ecopening("D71", "Neo-Grunfeld", "Neo-Grunfeld", "d4 Nf6 c4 g6 g3 d5", "rnbqkb1r/ppp1pp1p/5np1/3p4/2PP4/6P1/PP2PP1P/RNBQKBNR"),
      "D72" -> Ecopening("D72", "Neo-Grunfeld", "Neo-Grunfeld, 5.cd, Main line", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 cxd5 Nxd5 e4 Nb67 Ne2", "rnbqk2r/ppp1ppbp/6p1/3n4/3PP3/6P1/PP3PBP/RNBQK1NR"),
      "D73" -> Ecopening("D73", "Neo-Grunfeld", "Neo-Grunfeld, 5.Nf3", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3", "rnbqk2r/ppp1ppbp/5np1/3p4/2PP4/5NP1/PP2PPBP/RNBQK2R"),
      "D74" -> Ecopening("D74", "Neo-Grunfeld", "Neo-Grunfeld, 6.cd Nxd5, 7.O-O", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3 O-O cxd5 Nxd57 O-O", "rnbq1rk1/ppp1ppbp/5np1/3P4/3P4/5NP1/PP2PPBP/RNBQK2R"),
      "D75" -> Ecopening("D75", "Neo-Grunfeld", "Neo-Grunfeld, 6.cd Nxd5, 7.O-O c5, 8.dxc5", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3 O-O cxd5 Nxd57 O-O c5 dxc5", "rnbq1rk1/pp2ppbp/5np1/2PP4/8/5NP1/PP2PPBP/RNBQK2R"),
      "D76" -> Ecopening("D76", "Neo-Grunfeld", "Neo-Grunfeld, 6.cd Nxd5, 7.O-O Nb6", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3 O-O cxd5 Nxd57 O-O Nb6", "rnbq1rk1/ppp1ppbp/5np1/3P4/3P4/5NP1/PP2PPBP/RNBQK2R"),
      "D77" -> Ecopening("D77", "Neo-Grunfeld", "Neo-Grunfeld, 6.O-O", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3 O-O O-O", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "D78" -> Ecopening("D78", "Neo-Grunfeld", "Neo-Grunfeld, 6.O-O c6", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3 O-O O-O c6", "rnbq1rk1/pp2ppbp/2p2np1/3p4/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "D79" -> Ecopening("D79", "Neo-Grunfeld", "Neo-Grunfeld, 6.O-O, Main line", "d4 Nf6 c4 g6 g3 d5 Bg2 Bg7 Nf3 O-O O-O c67 cxd5 cxd5", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PP4/5NP1/PP2PPBP/RNBQ1RK1")
    ),
    "Grunfeld" -> Map(
      "D80" -> Ecopening("D80", "Grunfeld", "Grunfeld", "d4 Nf6 c4 g6 Nc3 d5", "rnbqkb1r/ppp1pp1p/5np1/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR"),
      "D81" -> Ecopening("D81", "Grunfeld", "Grunfeld, Russian Variation", "d4 Nf6 c4 g6 Nc3 d5 Qb3", "rnbqkb1r/ppp1pp1p/5np1/3p4/2PP4/1QN5/PP2PPPP/R1B1KBNR"),
      "D82" -> Ecopening("D82", "Grunfeld", "Grunfeld, 4.Bf4", "d4 Nf6 c4 g6 Nc3 d5 Bf4", "rnbqkb1r/ppp1pp1p/5np1/3p4/2PP1B2/2N5/PP2PPPP/R2QKBNR"),
      "D83" -> Ecopening("D83", "Grunfeld", "Grunfeld, Grunfeld Gambit", "d4 Nf6 c4 g6 Nc3 d5 Bf4 Bg7 e3 O-O", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PP1B2/2N1P3/PP3PPP/R2QKBNR"),
      "D84" -> Ecopening("D84", "Grunfeld", "Grunfeld, Grunfeld Gambit Accepted", "d4 Nf6 c4 g6 Nc3 d5 Bf4 Bg7 e3 O-O cxd5 Nxd57 Nxd5 Qxd5 Bxc7", "rnbq1rk1/ppB1ppbp/6p1/3n4/3P4/2N1P3/PP3PPP/R2QKBNR"),
      "D85" -> Ecopening("D85", "Grunfeld", "Grunfeld", "d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5", "rnbqkb1r/ppp1pp1p/6p1/3n4/3P4/2N5/PP2PPPP/R1BQKBNR"),
      "D86" -> Ecopening("D86", "Grunfeld", "Grunfeld, Exchange", "d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5 e4 Nxc3 bxc3 Bg77 Bc4", "rnbqkb1r/ppp1pp1p/6p1/8/3PP3/2P5/P4PPP/R1BQKBNR"),
      "D87" -> Ecopening("D87", "Grunfeld", "Grunfeld, Exchange", "d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5 e4 Nxc3 bxc3 Bg77 Bc4 O-O Ne2 c5", "rnbqkb1r/pp2pp1p/6p1/2p5/3PP3/2P5/P4PPP/R1BQKBNR"),
      "D88" -> Ecopening("D88", "Grunfeld", "Grunfeld, Spassky Variation, Main line, 10...cd, 11.cd", "d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5 e4 Nxc3 bxc3 Bg77 Bc4 O-O Ne2", "rnbqkb1r/ppp1pp1p/6p1/8/3PP3/2P5/P4PPP/R1BQKBNR"),
      "D89" -> Ecopening("D89", "Grunfeld", "Grunfeld", "d4 Nf6 c4 g6 Nc3 d5 cxd5 Nxd5 e4 Nxc3 bxc3 Bg77 Bc4 O-O Ne2", "rnbqkb1r/ppp1pp1p/6p1/8/3PP3/2P5/P4PPP/R1BQKBNR"),
      "D90" -> Ecopening("D90", "Grunfeld", "Grunfeld", "d4 Nf6 c4 g6 Nc3 d5 Nf3", "rnbqkb1r/ppp1pp1p/5np1/3p4/2PP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "D91" -> Ecopening("D91", "Grunfeld", "Grunfeld, 5.Bg5", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Bg5", "rnbqk2r/ppp1ppbp/5np1/3p2B1/2PP4/2N2N2/PP2PPPP/R2QKB1R"),
      "D92" -> Ecopening("D92", "Grunfeld", "Grunfeld, 5.Bf4", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Bf4", "rnbqk2r/ppp1ppbp/5np1/3p4/2PP1B2/2N2N2/PP2PPPP/R2QKB1R"),
      "D93" -> Ecopening("D93", "Grunfeld", "Grunfeld, with Bf4 & e3", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Bf4 O-O e3", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PP1B2/2N1PN2/PP3PPP/R2QKB1R"),
      "D94" -> Ecopening("D94", "Grunfeld", "Grunfeld", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 e3", "rnbqk2r/ppp1ppbp/5np1/3p4/2PP4/2N1PN2/PP3PPP/R1BQKB1R"),
      "D95" -> Ecopening("D95", "Grunfeld", "Grunfeld", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 e3 O-O Qb3", "rnbq1rk1/ppp1ppbp/5np1/3p4/2PP4/1QN1PN2/PP3PPP/R1B1KB1R"),
      "D96" -> Ecopening("D96", "Grunfeld", "Grunfeld, Russian Variation", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Qb3", "rnbqk2r/ppp1ppbp/5np1/3p4/2PP4/1QN2N2/PP2PPPP/R1B1KB1R"),
      "D97" -> Ecopening("D97", "Grunfeld", "Grunfeld, Russian", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Qb3 dxc4 Qxc4 O-O7 e4", "rnbqk2r/ppp1ppbp/5np1/8/2QP4/2N2N2/PP2PPPP/R1B1KB1R"),
      "D98" -> Ecopening("D98", "Grunfeld", "Grunfeld, Russian", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Qb3 dxc4 Qxc4 O-O7 e4 Bg4", "rn1qk2r/ppp1ppbp/5np1/8/2QP2b1/2N2N2/PP2PPPP/R1B1KB1R")
    ),
    "Grunfeld Defense" -> Map(
      "D99" -> Ecopening("D99", "Grunfeld Defense", "Grunfeld Defense, Smyslov", "d4 Nf6 c4 g6 Nc3 d5 Nf3 Bg7 Qb3 dxc4 Qxc4 O-O7 e4 Bg4 Be3", "rn1qk2r/ppp1ppbp/5np1/8/2QP2b1/2N1BN2/PP2PPPP/R3KB1R")
    ),
    "Catalan" -> Map(
      "E01" -> Ecopening("E01", "Catalan", "Catalan, Closed", "d4 Nf6 c4 e6 g3 d5 Bg2", "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/6P1/PP2PPBP/RNBQK1NR"),
      "E02" -> Ecopening("E02", "Catalan", "Catalan, Open, 5.Qa4", "d4 Nf6 c4 e6 g3 d5 Bg2 dxc4 Qa4+", "rnbqkb1r/ppp2ppp/4pn2/8/Q1pP4/6P1/PP2PPBP/RNB1K1NR"),
      "E03" -> Ecopening("E03", "Catalan", "Catalan, Open", "d4 Nf6 c4 e6 g3 d5 Bg2 dxc4 Qa4+ Nbd7 Qxc4", "r1bqkb1r/pppn1ppp/4pn2/8/2QP4/6P1/PP2PPBP/RNB1K1NR"),
      "E04" -> Ecopening("E04", "Catalan", "Catalan, Open, 5.Nf3", "d4 Nf6 c4 e6 g3 d5 Bg2 dxc4 Nf3", "rnbqkb1r/ppp2ppp/4pn2/8/2pP4/5NP1/PP2PPBP/RNBQK2R"),
      "E05" -> Ecopening("E05", "Catalan", "Catalan, Open, Classical line", "d4 Nf6 c4 e6 g3 d5 Bg2 dxc4 Nf3 Be7", "rnbqk2r/ppp1bppp/4pn2/8/2pP4/5NP1/PP2PPBP/RNBQK2R"),
      "E06" -> Ecopening("E06", "Catalan", "Catalan, Closed, 5.Nf3", "d4 Nf6 c4 e6 g3 d5 Bg2 Be7 Nf3", "rnbqk2r/ppp1bppp/4pn2/3p4/2PP4/5NP1/PP2PPBP/RNBQK2R"),
      "E07" -> Ecopening("E07", "Catalan", "Catalan, Closed", "d4 Nf6 c4 e6 g3 d5 Bg2 Be7 Nf3 O-O O-O Nbd7", "r1bq1rk1/pppnbppp/4pn2/3p4/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "E08" -> Ecopening("E08", "Catalan", "Catalan, Closed", "d4 Nf6 c4 e6 g3 d5 Bg2 Be7 Nf3 O-O O-O Nbd77 Qc2", "rnbq1rk1/ppp1bppp/4pn2/3p4/2PP4/5NP1/PP2PPBP/RNBQ1RK1"),
      "E09" -> Ecopening("E09", "Catalan", "Catalan, Closed", "d4 Nf6 c4 e6 g3 d5 Bg2 Be7 Nf3 O-O O-O Nbd77 Qc2 c6 Nbd2", "rnbq1rk1/pp2bppp/2p1pn2/3p4/2PP4/5NP1/PP1NPPBP/R1BQ1RK1")
    ),
    "Bogo-Indian Defense" -> Map(
      "E11" -> Ecopening("E11", "Bogo-Indian Defense", "Bogo-Indian Defense", "d4 Nf6 c4 e6 Nf3 Bb4+", "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/5N2/PP2PPPP/RNBQKB1R")
    ),
    "Nimzo-Indian" -> Map(
      "E20" -> Ecopening("E20", "Nimzo-Indian", "Nimzo-Indian", "d4 Nf6 c4 e6 Nc3 Bb4", "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PP2PPPP/R1BQKBNR"),
      "E21" -> Ecopening("E21", "Nimzo-Indian", "Nimzo-Indian, Three Knights", "d4 Nf6 c4 e6 Nc3 Bb4 Nf3", "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N2N2/PP2PPPP/R1BQKB1R"),
      "E22" -> Ecopening("E22", "Nimzo-Indian", "Nimzo-Indian, Spielmann Variation", "d4 Nf6 c4 e6 Nc3 Bb4 Qb3", "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/1QN5/PP2PPPP/R1B1KBNR"),
      "E23" -> Ecopening("E23", "Nimzo-Indian", "Nimzo-Indian, Spielmann", "d4 Nf6 c4 e6 Nc3 Bb4 Qb3 c5 dxc5 Nc6", "r1bqk2r/pp1p1ppp/2n1pn2/2P5/1bP5/1QN5/PP2PPPP/R1B1KBNR"),
      "E24" -> Ecopening("E24", "Nimzo-Indian", "Nimzo-Indian, Samisch", "d4 Nf6 c4 e6 Nc3 Bb4 a3 Bxc3+ bxc3", "rnbqk2r/pppp1ppp/4pn2/8/2PP4/P1P5/4PPPP/R1BQKBNR"),
      "E25" -> Ecopening("E25", "Nimzo-Indian", "Nimzo-Indian, Samisch", "d4 Nf6 c4 e6 Nc3 Bb4 a3 Bxc3+ bxc3 c5 f3 d57 cxd5", "rnbqk2r/pp1p1ppp/4pn2/2p5/2PP4/P1P2P2/4P1PP/R1BQKBNR"),
      "E26" -> Ecopening("E26", "Nimzo-Indian", "Nimzo-Indian, Samisch", "d4 Nf6 c4 e6 Nc3 Bb4 a3 Bxc3+ bxc3 c5 e3", "rnbqk2r/pp1p1ppp/4pn2/2p5/2PP4/P1P1P3/5PPP/R1BQKBNR"),
      "E27" -> Ecopening("E27", "Nimzo-Indian", "Nimzo-Indian, Samisch Variation", "d4 Nf6 c4 e6 Nc3 Bb4 a3 Bxc3+ bxc3 O-O", "rnbq1rk1/pppp1ppp/4pn2/8/2PP4/P1P5/4PPPP/R1BQKBNR"),
      "E28" -> Ecopening("E28", "Nimzo-Indian", "Nimzo-Indian, Samisch Variation", "d4 Nf6 c4 e6 Nc3 Bb4 a3 Bxc3+ bxc3 O-O e3", "rnbq1rk1/pppp1ppp/4pn2/8/2PP4/P1P1P3/5PPP/R1BQKBNR"),
      "E29" -> Ecopening("E29", "Nimzo-Indian", "Nimzo-Indian, Samisch", "d4 Nf6 c4 e6 Nc3 Bb4 a3 Bxc3+ bxc3 O-O e3 c57 Bd3 Nc6", "r1bq1rk1/pppp1ppp/2n1pn2/8/2PP4/P1P1P3/5PPP/R1BQKBNR"),
      "E30" -> Ecopening("E30", "Nimzo-Indian", "Nimzo-Indian, Leningrad", "d4 Nf6 c4 e6 Nc3 Bb4 Bg5", "rnbqk2r/pppp1ppp/4pn2/6B1/1bPP4/2N5/PP2PPPP/R2QKBNR"),
      "E31" -> Ecopening("E31", "Nimzo-Indian", "Nimzo-Indian, Leningrad, Main line", "d4 Nf6 c4 e6 Nc3 Bb4 Bg5 h6 Bh4 c5 d5 d6", "rnbqk2r/pp3pp1/3ppn1p/2pP4/1bP4B/2N5/PP2PPPP/R2QKBNR"),
      "E32" -> Ecopening("E32", "Nimzo-Indian", "Nimzo-Indian, Classical", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2", "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N5/PPQ1PPPP/R1B1KBNR"),
      "E33" -> Ecopening("E33", "Nimzo-Indian", "Nimzo-Indian, Classical", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 Nc6", "r1bqk2r/pppp1ppp/2n1pn2/8/1bPP4/2N5/PPQ1PPPP/R1B1KBNR"),
      "E34" -> Ecopening("E34", "Nimzo-Indian", "Nimzo-Indian, Classical, Noa Variation", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 d5", "rnbqk2r/ppp2ppp/4pn2/3p4/1bPP4/2N5/PPQ1PPPP/R1B1KBNR"),
      "E35" -> Ecopening("E35", "Nimzo-Indian", "Nimzo-Indian, Classical, Noa Variation, 5.cd ed", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 d5 cxd5 exd5", "rnbqk2r/ppp2ppp/5n2/3p4/1b1P4/2N5/PPQ1PPPP/R1B1KBNR"),
      "E36" -> Ecopening("E36", "Nimzo-Indian", "Nimzo-Indian, Classical", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 d5 a3", "rnbqk2r/ppp2ppp/4pn2/3p4/1bPP4/P1N5/1PQ1PPPP/R1B1KBNR"),
      "E37" -> Ecopening("E37", "Nimzo-Indian", "Nimzo-Indian, Classical", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 d5 a3 Bxc3+ Qxc3 Ne47 Qc2", "rnbqk2r/ppp2ppp/4pn2/3p4/2PP4/P1Q5/1P2PPPP/R1B1KBNR"),
      "E38" -> Ecopening("E38", "Nimzo-Indian", "Nimzo-Indian, Classical, 4...c5", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 c5", "rnbqk2r/pp1p1ppp/4pn2/2p5/1bPP4/2N5/PPQ1PPPP/R1B1KBNR"),
      "E39" -> Ecopening("E39", "Nimzo-Indian", "Nimzo-Indian, Classical, Pirc Variation", "d4 Nf6 c4 e6 Nc3 Bb4 Qc2 c5 dxc5 O-O", "rnbq1rk1/pp1p1ppp/4pn2/2P5/1bP5/2N5/PPQ1PPPP/R1B1KBNR"),
      "E40" -> Ecopening("E40", "Nimzo-Indian", "Nimzo-Indian, 4.e3", "d4 Nf6 c4 e6 Nc3 Bb4 e3", "rnbqk2r/pppp1ppp/4pn2/8/1bPP4/2N1P3/PP3PPP/R1BQKBNR"),
      "E41" -> Ecopening("E41", "Nimzo-Indian", "Nimzo-Indian", "d4 Nf6 c4 e6 Nc3 Bb4 e3 c5", "rnbqk2r/pp1p1ppp/4pn2/2p5/1bPP4/2N1P3/PP3PPP/R1BQKBNR"),
      "E42" -> Ecopening("E42", "Nimzo-Indian", "Nimzo-Indian, 4.e3 c5, 5.Ne2 (Rubinstein)", "d4 Nf6 c4 e6 Nc3 Bb4 e3 c5 Ne2", "rnbqk2r/pp1p1ppp/4pn2/2p5/1bPP4/2N1P3/PP2NPPP/R1BQKB1R"),
      "E43" -> Ecopening("E43", "Nimzo-Indian", "Nimzo-Indian, Fischer Variation", "d4 Nf6 c4 e6 Nc3 Bb4 e3 b6", "rnbqk2r/p1pp1ppp/1p2pn2/8/1bPP4/2N1P3/PP3PPP/R1BQKBNR"),
      "E44" -> Ecopening("E44", "Nimzo-Indian", "Nimzo-Indian, Fischer Variation, 5.Ne2", "d4 Nf6 c4 e6 Nc3 Bb4 e3 b6 Ne2", "rnbqk2r/p1pp1ppp/1p2pn2/8/1bPP4/2N1P3/PP2NPPP/R1BQKB1R"),
      "E45" -> Ecopening("E45", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Bronstein (Byrne) Variation", "d4 Nf6 c4 e6 Nc3 Bb4 e3 b6 Ne2 Ba6", "rn1qk2r/p1pp1ppp/bp2pn2/8/1bPP4/2N1P3/PP2NPPP/R1BQKB1R"),
      "E46" -> Ecopening("E46", "Nimzo-Indian", "Nimzo-Indian", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O", "rnbq1rk1/pppp1ppp/4pn2/8/1bPP4/2N1P3/PP3PPP/R1BQKBNR"),
      "E47" -> Ecopening("E47", "Nimzo-Indian", "Nimzo-Indian, 4.e3 O-O 5.Bd3", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Bd3", "rnbq1rk1/pppp1ppp/4pn2/8/1bPP4/2NBP3/PP3PPP/R1BQK1NR"),
      "E48" -> Ecopening("E48", "Nimzo-Indian", "Nimzo-Indian, 4.e3 O-O 5.Bd3 d5", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Bd3 d5", "rnbq1rk1/ppp2ppp/4pn2/3p4/1bPP4/2NBP3/PP3PPP/R1BQK1NR"),
      "E49" -> Ecopening("E49", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Botvinnik System", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Bd3 d5 a3 Bxc3+ bxc3", "rnbq1rk1/ppp2ppp/4pn2/3p4/2PP4/P1PBP3/5PPP/R1BQK1NR"),
      "E50" -> Ecopening("E50", "Nimzo-Indian", "Nimzo-Indian, 4.e3 O-O 5.Nf3, without ...d5", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3", "rnbq1rk1/pppp1ppp/4pn2/8/1bPP4/2N1PN2/PP3PPP/R1BQKB1R"),
      "E51" -> Ecopening("E51", "Nimzo-Indian", "Nimzo-Indian, 4.e3", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5", "rnbq1rk1/ppp2ppp/4pn2/3p4/1bPP4/2N1PN2/PP3PPP/R1BQKB1R"),
      "E52" -> Ecopening("E52", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Main line with ...b6", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 b6", "rnbq1rk1/p1p2ppp/1p2pn2/3p4/1bPP4/2NBPN2/PP3PPP/R1BQK2R"),
      "E53" -> Ecopening("E53", "Nimzo-Indian", "Nimzo-Indian, 4.e3", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c5", "rnbq1rk1/pp3ppp/4pn2/2pp4/1bPP4/2NBPN2/PP3PPP/R1BQK2R"),
      "E54" -> Ecopening("E54", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Gligoric System", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c57 O-O dxc4 Bxc4", "rnbq1rk1/ppp2ppp/4pn2/8/1bBP4/2N1PN2/PP3PPP/R1BQK2R"),
      "E55" -> Ecopening("E55", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Gligoric System, Bronstein Variation", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c57 O-O dxc4 Bxc4 Nbd7", "r1bq1rk1/pppn1ppp/4pn2/8/1bBP4/2N1PN2/PP3PPP/R1BQK2R"),
      "E56" -> Ecopening("E56", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Main line with 7...Nc6", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c57 O-O Nc6", "r1bq1rk1/ppp2ppp/2n1pn2/3p4/1bPP4/2NBPN2/PP3PPP/R1BQK2R"),
      "E57" -> Ecopening("E57", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Main line with 8...dc and 9...cd", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c57 O-O Nc6 a3 dxc4 Bxc4", "r1bq1rk1/ppp2ppp/2n1pn2/8/1bBP4/P1N1PN2/1P3PPP/R1BQK2R"),
      "E58" -> Ecopening("E58", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Main line with 8...Bxc3", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c57 O-O Nc6 a3 Bxc3 bxc3", "r1bq1rk1/ppp2ppp/2n1pn2/3p4/2PP4/P1PBPN2/5PPP/R1BQK2R"),
      "E59" -> Ecopening("E59", "Nimzo-Indian", "Nimzo-Indian, 4.e3, Main line", "d4 Nf6 c4 e6 Nc3 Bb4 e3 O-O Nf3 d5 Bd3 c57 O-O Nc6 a3 Bxc3 bxc3 dxc4 Bxc4", "r1bq1rk1/ppp2ppp/2n1pn2/8/2BP4/P1P1PN2/5PPP/R1BQK2R")
    ),
    "King's Indian Defense" -> Map(
      "E60" -> Ecopening("E60", "King's Indian Defense", "King's Indian Defense", "d4 Nf6 c4 g6", "rnbqkb1r/pppppp1p/5np1/8/2PP4/8/PP2PPPP/RNBQKBNR")
    )
  )
}

