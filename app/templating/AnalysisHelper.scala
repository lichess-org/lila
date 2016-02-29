package lila.app
package templating

import chess.format.Nag
import play.api.data._
import play.twirl.api.Html

import lila.api.Context

trait AnalysisHelper { self: I18nHelper with SecurityHelper =>

  def nagName(nag: Nag)(implicit ctx: Context) = nag match {
    case Nag.Blunder    => trans.blunders()
    case Nag.Mistake    => trans.mistakes()
    case Nag.Inaccuracy => trans.inaccuracies()
    case nag            => nag.toString
  }

  object isExplorerAvailable {
    import reactivemongo.bson._
    import scala.concurrent.duration._
    private val coll = lila.db.Env.current("flag")
    private val cache = lila.memo.MixedCache.single[Boolean](
      f = coll.find(BSONDocument("_id" -> "explorer")).one[BSONDocument].map {
        _ ?? (~_.getAs[Boolean]("enabled"))
      },
      timeToLive = 10 seconds,
      default = false)
    def apply(implicit ctx: lila.api.Context): Boolean =
      isGranted(_.Beta) || (cache get true)
  }
}
