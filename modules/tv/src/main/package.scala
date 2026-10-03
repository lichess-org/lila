package lila.tv

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

case class RenderFeaturedJs(game: Game, promise: Promise[Html])
