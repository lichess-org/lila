package chess

case class StartingPosition(
    eco: String,
    name: String,
    fen: String,
    wikiPath: String,
    moves: String,
    featurable: Boolean = true
) {

  def url = s"https://en.wikipedia.org/wiki/$wikiPath"

  val shortName = name takeWhile (':' !=)

  def fullName = s"$eco $name"

  def initial = fen == format.Forsyth.initial
}

object StartingPosition {

  case class Category(name: String, positions: List[StartingPosition])

  val categories: List[Category] = List(
    Category(
      "Handicaps",
      List(
        StartingPosition(
          "1",
          "left lance",
          "lnsgkgsn1/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w - 1",
          "Handicap_(shogi)",
          "",
          false
        )
      )
    )
  )

  val all: IndexedSeq[StartingPosition] = categories.flatMap(_.positions).toIndexedSeq

  val initial = StartingPosition("---", "Initial position", format.Forsyth.initial, "Chess", "")

  def allWithInitial = initial +: all

  lazy val featurable = new scala.util.Random(475591).shuffle(all.filter(_.featurable)).toIndexedSeq

  def randomFeaturable = featurable(scala.util.Random.nextInt(featurable.size))

  object presets {
    val halloween = StartingPosition(
      "C47",
      "Halloween Gambit",
      "r1bqkb1r/pppp1ppp/2n2n2/4N3/4P3/2N5/PPPP1PPP/R1BQKB1R b KQkq - 1 4",
      "Halloween_Gambit",
      "1. e4 e5 2. Nf3 Nc6 3. Nc3 Nf6 4. Nxe5"
    )
    val frankenstein = StartingPosition(
      "C27",
      "Frankenstein-Dracula Variation",
      "rnbqkb1r/pppp1ppp/8/4p3/2B1n3/2N5/PPPP1PPP/R1BQK1NR w KQkq - 0 4",
      "Frankenstein-Dracula_Variation",
      "1. e4 e5 2. Nc3 Nf6 3. Bc4 Nxe4"
    )
  }
}
