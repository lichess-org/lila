package lila.irwin

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.{ Pov, GameRepo }
import lila.user.User

final class IrwinApi(
    reportColl: Coll
) {

  import BSONHandlers._

  def insert(report: IrwinReport) = reportColl.update($id(report.id), report, upsert = true)

  def get(user: User): Fu[Option[IrwinReport]] =
    reportColl.find($id(user.id)).uno[IrwinReport]

  def withPovs(user: User): Fu[Option[IrwinReport.WithPovs]] = get(user) flatMap {
    _ ?? { report =>
      GameRepo.gamesFromSecondary(report.games.map(_.gameId)) map { games =>
        val povs = games.flatMap { g =>
          Pov(g, user) map { g.id -> _ }
        }.toMap
        IrwinReport.WithPovs(report, povs).some
      }
    }
  }
}
