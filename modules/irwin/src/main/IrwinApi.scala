package lila.irwin

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.game.{ Pov, GameRepo }
import lila.tournament.{ Tournament, RankedPlayer }
import lila.user.User

final class IrwinApi(
    reportColl: Coll,
    requestColl: Coll,
    modApi: lila.mod.ModApi
) {

  import BSONHandlers._

  def status(user: User): Fu[IrwinStatus] =
    reports.withPovs(user) zip requests.get(user.id) map { (IrwinStatus.apply _).tupled }

  object reports {

    def insert(report: IrwinReport) =
      reportColl.update($id(report.id), report, upsert = true) >>
        requests.drop(report.userId) >>
        actAsMod(report)

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

    private def actAsMod(report: IrwinReport): Funit =
      if (report.totallyCheating) modApi.setEngine("irwin", report.userId, true)
      else if (report.totallyNotCheating) modApi.setEngine("irwin", report.userId, false)
      else funit // keep the report open for mods to review
  }

  object requests {

    import IrwinRequest.Origin

    def getAndStart: Fu[Option[IrwinRequest]] =
      requestColl
        .find($doc("startedAt" $exists false))
        .sort($sort asc "priority")
        .uno[IrwinRequest] flatMap {
          _ ?? { request =>
            requestColl.updateField($id(request.id), "startedAt", DateTime.now) inject request.some
          }
        }

    def get(reportedId: User.ID): Fu[Option[IrwinRequest]] =
      requestColl.byId[IrwinRequest](reportedId)

    def fromMod(reportedId: User.ID) = insert(reportedId, _.Moderator)

    private[irwin] def drop(reportedId: User.ID): Funit = requestColl.remove($id(reportedId)).void

    private[irwin] def insert(reportedId: User.ID, origin: Origin.type => Origin) = {
      val request = IrwinRequest.make(reportedId, origin(Origin))
      get(reportedId) flatMap {
        case Some(prev) if prev.isInProgress => funit
        case Some(prev) if prev.priority isAfter request.priority =>
          requestColl.update($id(request.id), request).void
        case Some(prev) => funit
        case None => requestColl.insert(request).void
      }
    }

    private[irwin] def fromTournamentLeaders(leaders: Map[Tournament, List[RankedPlayer]]): Funit =
      lila.common.Future.applySequentially(leaders.toList) {
        case (tour, rps) =>
          val userIds = rps.filter(_.rank <= tour.nbPlayers / 10).map(_.player.userId)
          lila.common.Future.applySequentially(userIds) { userId =>
            insert(userId, _.Tournament)
          }
      }

    private[irwin] def fromLeaderboard(leaders: List[User.ID]): Funit =
      lila.common.Future.applySequentially(leaders) { userId =>
        insert(userId, _.Leaderboard)
      }
  }
}
