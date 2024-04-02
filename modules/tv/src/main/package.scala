package lila.tv

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("tv")

case class RenderFeaturedJs(game: lila.game.Game, promise: Promise[Html])
