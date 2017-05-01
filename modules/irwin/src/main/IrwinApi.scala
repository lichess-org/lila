package lila.irwin

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.{ Pov, GameRepo }
import lila.user.User

final class IrwinApi(
    reportColl: Coll,
    requestColl: Coll
) {

  import BSONHandlers._

  object reports {

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

  object requests {

    import IrwinRequest.Origin

    def getAndStart: Fu[Option[IrwinRequest]] =
      requestColl
        .find($doc("startedAt" $exists false))
        .sort($sort desc "priority")
        .uno[IrwinRequest] flatMap {
          _ ?? { request =>
            requestColl.updateField($id(request.id), "startedAt", DateTime.now) inject request.some
          }
        }

    def get(reportedId: User.ID): Fu[Option[IrwinRequest]] =
      requestColl.byId[IrwinRequest]($id(reportedId))

    def insert(reportedId: User.ID, origin: Origin.type => Origin) = {
      val request = IrwinRequest.make(reportedId, origin(Origin))
      get(reportedId) flatMap {
        case Some(prev) if prev.isInProgress => funit
        case Some(prev) if prev.priority isAfter request.priority =>
          requestColl.update($id(request.id), request).void
        case Some(prev) => funit
        case None => requestColl.insert(request).void
      }
    }
  }
}
