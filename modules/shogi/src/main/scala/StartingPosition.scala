package shogi

case class StartingPosition(
    eco: String,
    name: String,
    fen: String,
    wikiPath: String,
    moves: String,
    featurable: Boolean = true
) {

  val url = s"https://en.wikipedia.org/wiki/$wikiPath"

  val shortName = name takeWhile (':' !=)

  val fullName = s"$eco - $name"

  val initial = fen == format.Forsyth.initial
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
          "lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "右香落ち",
          "Right Lance",
          "1nsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "角落ち",
          "Bishop",
          "lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛車落ち",
          "Rook",
          "lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛香落ち",
          "Rook-Lance",
          "lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "二枚落ち",
          "2-piece",
          "lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "四枚落ち",
          "4-piece",
          "1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "六枚落ち",
          "6-piece",
          "2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "八枚落ち",
          "8-piece",
          "3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "十枚落ち",
          "10-piece",
          "4k4/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "歩三兵",
          "3 Pawns",
          "4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w 3p 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "裸玉",
          "Naked King",
          "4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "トンボ＋桂香",
          "Dragonfly + NL",
          "ln2k2nl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "トンボ＋香",
          "Dragonfly + L",
          "l3k3l/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "トンボ",
          "Dragonfly",
          "4k4/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "香得",
          "Lance Gained",
          "lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w L 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "角得",
          "Bishop Gained",
          "lnsgkgsnl/1r7/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w B 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛車得",
          "Rook Gained",
          "lnsgkgsnl/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w R 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "飛香得",
          "Rook-Lance Gained",
          "lnsgkgsn1/7b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RL 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "二枚得",
          "2-piece Gained",
          "lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "四枚得",
          "4-piece Gained",
          "1nsgkgsn1/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2L 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "六枚得",
          "6-piece Gained",
          "2sgkgs2/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2N2L 2",
          "Handicap_(shogi)",
          "",
          false
        ),
        StartingPosition(
          "八枚得",
          "8-piece Gained",
          "3gkg3/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w RB2S2N2L 2",
          "Handicap_(shogi)",
          "",
          false
        )
      )
    )
  )

  val all: IndexedSeq[StartingPosition] = categories.flatMap(_.positions).toIndexedSeq

  val initial = StartingPosition("---", "Initial position", format.Forsyth.initial, "Shogi", "")

  def allWithInitial = initial +: all

  lazy val featurable = new scala.util.Random(475591).shuffle(all.filter(_.featurable)).toIndexedSeq

  def randomFeaturable = featurable(scala.util.Random.nextInt(featurable.size))

  def searchByEco(eco: String): Option[StartingPosition] =
    all.find(_.eco == eco)

  def searchHandicapByFen(fen: Option[format.FEN]): Option[StartingPosition] = {
    fen flatMap { fe =>
      this.categories find { _.name == "Handicaps" } flatMap { hcs =>
        hcs.positions find { _.fen == fe.value }
      }
    }
  }

  def isFENHandicap(fen: Option[format.FEN]): Boolean = searchHandicapByFen(fen).isDefined

}
