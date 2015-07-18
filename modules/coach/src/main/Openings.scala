package lila.coach

case class Openings(
    white: Map[String, Int],
    black: Map[String, Int]) {

  def apply(c: chess.Color) = c.fold(white, black)

  def aggregate(p: lila.game.Pov) = p.game.opening.map(_.code).fold(this) { code =>
    copy(
      white = if (p.color.white) openingWithCode(white, code) else white,
      black = if (p.color.black) openingWithCode(black, code) else black)
  }

  private def openingWithCode(opening: Map[String, Int], code: String) =
    opening + (code -> opening.get(code).fold(1)(1+))
}

