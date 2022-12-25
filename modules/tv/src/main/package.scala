package lila.tv

export lila.Lila.{ *, given }

private val logger = lila.log("tv")

case class RenderFeaturedJs(game: lila.game.Game) extends AnyVal
