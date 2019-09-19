package lila.irwin

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference
import reactivemongo.bson._

import lila.analyse.Analysis.Analyzed
import lila.analyse.AnalysisRepo
import lila.db.dsl._
import lila.game.{ Game, Pov, GameRepo, Query }
import lila.report.{ Report, Mod, Suspect, Reporter, SuspectId, ModId }
import lila.tournament.{ Tournament, TournamentTop }
import lila.user.{ User, UserRepo }

final class IrwinApi(
    reportColl: Coll,
    modApi: lila.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.notify.NotifyApi,
    bus: lila.common.Bus,
    mode: () => String
) {

  val reportThreshold = 85
  val markThreshold = 93

  import BSONHandlers._

  def dashboard: Fu[IrwinDashboard] =
    reportColl.find($empty).sort($sort desc "date").list[IrwinReport](20) map IrwinDashboard.apply

  object reports {

    def insert(report: IrwinReport) = (mode() != "none") ?? {
      for {
        _ <- reportColl.update($id(report._id), report, upsert = true)
        _ <- markOrReport(report)
        _ <- notification(report)
      } yield {
        lila.mon.mod.irwin.ownerReport(report.owner)()
      }
    }

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

    private def getSuspect(suspectId: User.ID) =
      UserRepo byId suspectId flatten s"suspect $suspectId not found" map Suspect.apply

    private def markOrReport(report: IrwinReport): Funit =
      if (report.activation >= markThreshold && mode() == "mark")
        modApi.autoMark(report.suspectId, ModId.irwin) >>-
          lila.mon.mod.irwin.mark()
      else if (report.activation >= reportThreshold && mode() != "none") for {
        suspect <- getSuspect(report.suspectId.value)
        irwin <- UserRepo byId "irwin" flatten s"Irwin user not found" map Mod.apply
        _ <- reportApi.create(Report.Candidate(
          reporter = Reporter(irwin.user),
          suspect = suspect,
          reason = lila.report.Reason.Cheat,
          text = s"${report.activation}% over ${report.games.size} games"
        ))
      } yield lila.mon.mod.irwin.report()
      else funit
  }

  object requests {

    import IrwinRequest.Origin

    def fromMod(suspect: Suspect, mod: Mod) = {
      notification.add(suspect.id, mod.id)
      insert(suspect, _.Moderator)
    }

    private[irwin] def insert(suspect: Suspect, origin: Origin.type => Origin): Funit = for {
      analyzed <- getAnalyzedGames(suspect, 15)
      more <- getMoreGames(suspect, 20 - analyzed.size)
      all = analyzed.map { a => a.game -> a.analysis.some } ::: more.map(_ -> none)
    } yield bus.publish(IrwinRequest(
      suspect = suspect,
      origin = origin(Origin),
      games = all
    ), 'irwin)

    private[irwin] def fromTournamentLeaders(leaders: Map[Tournament, TournamentTop]): Funit =
      lila.common.Future.applySequentially(leaders.toList) {
        case (tour, top) =>
          UserRepo byIds top.value.zipWithIndex
            .filter(_._2 <= tour.nbPlayers * 2 / 100)
            .map(_._1.userId)
            .take(20) flatMap { users =>
              lila.common.Future.applySequentially(users) { user =>
                insert(Suspect(user), _.Tournament)
              }
            }
      }

    private[irwin] def fromLeaderboard(leaders: List[User]): Funit =
      lila.common.Future.applySequentially(leaders) { user =>
        insert(Suspect(user), _.Leaderboard)
      }

    import lila.game.BSONHandlers._

    private def baseQuery(suspect: Suspect) =
      Query.finished ++
        Query.variantStandard ++
        Query.rated ++
        Query.user(suspect.id.value) ++
        Query.turnsGt(20) ++
        Query.createdSince(DateTime.now minusMonths 6)

    private def getAnalyzedGames(suspect: Suspect, nb: Int): Fu[List[Analyzed]] =
      GameRepo.coll.find(baseQuery(suspect) ++ Query.analysed(true))
        .sort(Query.sortCreated)
        .cursor[Game](ReadPreference.secondaryPreferred)
        .list(nb)
        .flatMap(AnalysisRepo.associateToGames)

    private def getMoreGames(suspect: Suspect, nb: Int): Fu[List[Game]] = (nb > 0) ??
      GameRepo.coll.find(baseQuery(suspect) ++ Query.analysed(false))
      .sort(Query.sortCreated).cursor[Game](ReadPreference.secondaryPreferred)
      .list(nb)
  }

  object notification {

    private var subs = Map.empty[SuspectId, ModId]

    def add(suspectId: SuspectId, modId: ModId): Unit = subs += (suspectId -> modId)

    private[IrwinApi] def apply(report: IrwinReport): Funit =
      subs.get(report.suspectId) ?? { modId =>
        subs = subs - report.suspectId
        import lila.notify.{ Notification, IrwinDone }
        notifyApi.addNotification(
          Notification.make(Notification.Notifies(modId.value), IrwinDone(report.suspectId.value))
        )
      }
  }
}
