package lila.coach

case class ColorResults(white: Results, black: Results) {

  def apply(c: chess.Color) = c.fold(white, black)
}

object ColorResults {

  val empty = ColorResults(Results.empty, Results.empty)

  case class Computation(white: Results.Computation, black: Results.Computation) {

    def aggregate(p: RichPov) = copy(
      white = if (p.pov.color.white) white aggregate p else white,
      black = if (p.pov.color.black) black aggregate p else black)

    def run = ColorResults(white = white.run, black = black.run)
  }
  val emptyComputation = Computation(Results.emptyComputation, Results.emptyComputation)
}
