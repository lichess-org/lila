package lila.tv

export lila.Core.{ *, given }

private val logger = lila.log("tv")

case class RenderFeaturedJs(game: lila.game.Game, promise: Promise[Html])
