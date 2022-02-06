package shogi

import format.forsyth.Sfen

case class StartingPosition(
    japanese: String,
    english: String,
    sfen: Sfen,
    wikiPath: String,
    moves: String,
    featurable: Boolean = true
) {

  val url = s"https://en.wikipedia.org/wiki/$wikiPath"

  val shortName = english takeWhile (':' !=)

  val fullName = s"$japanese ($english)"

  val initial = sfen == variant.Standard.initialSfen
}

object StartingPosition {

  case class Category(name: String, positions: List[StartingPosition])

  val categories: List[Category] = List(
    Category(
      "Handicaps",
      List(
        StartingPosition(
          "香落ち",
          "Lance",
          Sfen("lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "右香落ち",
          "Right Lance",
          Sfen("1nsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "角落ち",
          "Bishop",
          Sfen("lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛車落ち",
          "Rook",
          Sfen("lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛香落ち",
          "Rook-Lance",
          Sfen("lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "二枚落ち",
          "2-piece",
          Sfen("lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "四枚落ち",
          "4-piece",
          Sfen("1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "六枚落ち",
          "6-piece",
          Sfen("2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "八枚落ち",
          "8-piece",
          Sfen("3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "十枚落ち",
          "10-piece",
          Sfen("4k4/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "歩三兵",
          "3 Pawns",
          Sfen("4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w 3p 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "裸玉",
          "Naked King",
          Sfen("4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "トンボ＋桂香",
          "Dragonfly + NL",
          Sfen("ln2k2nl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "トンボ＋香",
          "Dragonfly + L",
          Sfen("l3k3l/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "トンボ",
          "Dragonfly",
          Sfen("4k4/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "香得",
          "Lance Gained",
          Sfen("lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w L 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "角得",
          "Bishop Gained",
          Sfen("lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w B 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛車得",
          "Rook Gained",
          Sfen("lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w R 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛香得",
          "Rook-Lance Gained",
          Sfen("lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RL 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "二枚得",
          "2-piece Gained",
          Sfen("lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "四枚得",
          "4-piece Gained",
          Sfen("1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2L 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "六枚得",
          "6-piece Gained",
          Sfen("2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2N2L 1"),
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "八枚得",
          "8-piece Gained",
          Sfen("3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2S2N2L 1"),
          "Handicap_(shogi)",
          "",
          false
        )
      )
    )
  )

  val all: IndexedSeq[StartingPosition] = categories.flatMap(_.positions).toIndexedSeq

  val initial = StartingPosition("平手", "Initial position", variant.Standard.initialSfen, "Shogi", "")

  def allWithInitial = initial +: all

  lazy val featurable = new scala.util.Random(475591).shuffle(all.filter(_.featurable))

  def randomFeaturable = featurable(scala.util.Random.nextInt(featurable.size))

  def searchByJapaneseName(japanese: String): Option[StartingPosition] =
    all.find(_.japanese == japanese)

  def searchHandicapBySfen(sfen: Option[Sfen]): Option[StartingPosition] = {
    sfen flatMap { sf =>
      categories find { _.name == "Handicaps" } flatMap { hcs =>
        hcs.positions find { p => p.sfen.truncate == sf.truncate }
      }
    }
  }

  def isSfenHandicap(sfen: Option[Sfen]): Boolean = searchHandicapBySfen(sfen).isDefined

}
