package lila.irwin

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.analyse.Analysis.Analyzed
import lila.analyse.AnalysisRepo
import lila.common.Bus
import lila.db.dsl._
import lila.game.{ Game, GameRepo, Pov, Query }
import lila.report.{ Mod, ModId, Report, Reporter, Suspect, SuspectId }
import lila.tournament.{ Tournament, TournamentTop }
import lila.user.{ User, UserRepo }

final class IrwinApi(
    reportColl: Coll,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    analysisRepo: AnalysisRepo,
    modApi: lila.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.notify.NotifyApi,
    thresholds: lila.memo.SettingStore[IrwinThresholds]
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def dashboard: Fu[IrwinDashboard] =
    reportColl
      .find($empty)
      .sort($sort desc "date")
      .cursor[IrwinReport]()
      .list(20) dmap IrwinDashboard.apply

  object reports {

    def insert(report: IrwinReport) =
      reportColl.update.one($id(report._id), report, upsert = true) >>
        markOrReport(report) >>
        notification(report) >>-
        lila.mon.mod.irwin.ownerReport(report.owner).increment().unit

    def get(user: User): Fu[Option[IrwinReport]] =
      reportColl.find($id(user.id)).one[IrwinReport]

    def withPovs(user: User): Fu[Option[IrwinReport.WithPovs]] =
      get(user) flatMap {
        _ ?? { report =>
          gameRepo.gamesFromSecondary(report.games.map(_.gameId)) dmap { games =>
            val povs = games.flatMap { g =>
              Pov(g, user) map { g.id -> _ }
            }.toMap
            IrwinReport.WithPovs(report, povs).some
          }
        }
      }

    private def getSuspect(suspectId: User.ID) =
      userRepo byId suspectId orFail s"suspect $suspectId not found" dmap Suspect.apply

    private def markOrReport(report: IrwinReport): Funit =
      if (report.activation >= thresholds.get().mark)
        modApi.autoMark(report.suspectId, ModId.irwin) >>-
          lila.mon.mod.irwin.mark.increment().unit
      else if (report.activation >= thresholds.get().report) for {
        suspect <- getSuspect(report.suspectId.value)
        irwin   <- userRepo byId "irwin" orFail s"Irwin user not found" dmap Mod.apply
        _ <- reportApi.create(
          Report.Candidate(
            reporter = Reporter(irwin.user),
            suspect = suspect,
            reason = lila.report.Reason.Cheat,
            text = s"${report.activation}% over ${report.games.size} games"
          ),
          (x: Report.Score) => Report.Score(60)
        )
      } yield lila.mon.mod.irwin.report.increment().unit
      else funit
  }

  object requests {

    import IrwinRequest.Origin

    def fromMod(suspect: Suspect, mod: Mod) = {
      notification.add(suspect.id, mod.id)
      insert(suspect, _.Moderator)
    }

    private[irwin] def insert(suspect: Suspect, origin: Origin.type => Origin): Funit =
      for {
        analyzed <- getAnalyzedGames(suspect, 15)
        more     <- getMoreGames(suspect, 20 - analyzed.size)
        all = analyzed.map { a =>
          a.game -> a.analysis.some
        } ::: more.map(_ -> none)
      } yield Bus.publish(
        IrwinRequest(
          suspect = suspect,
          origin = origin(Origin),
          games = all
        ),
        "irwin"
      )

    private[irwin] def fromTournamentLeaders(leaders: Map[Tournament, TournamentTop]): Funit =
      lila.common.Future.applySequentially(leaders.toList) { case (tour, top) =>
        userRepo byIds top.value.zipWithIndex
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
      gameRepo.coll
        .find(baseQuery(suspect) ++ Query.analysed(true))
        .sort(Query.sortCreated)
        .cursor[Game](ReadPreference.secondaryPreferred)
        .list(nb)
        .flatMap(analysisRepo.associateToGames)

    private def getMoreGames(suspect: Suspect, nb: Int): Fu[List[Game]] =
      (nb > 0) ??
        gameRepo.coll
          .find(baseQuery(suspect) ++ Query.analysed(false))
          .sort(Query.sortCreated)
          .cursor[Game](ReadPreference.secondaryPreferred)
          .list(nb)
  }

  object notification {

    private var subs = Map.empty[SuspectId, Set[ModId]]

    def add(suspectId: SuspectId, modId: ModId): Unit =
      subs = subs.updated(suspectId, ~subs.get(suspectId) + modId)

    private[IrwinApi] def apply(report: IrwinReport): Funit =
      subs.get(report.suspectId) ?? { modIds =>
        subs = subs - report.suspectId
        import lila.notify.{ IrwinDone, Notification }
        modIds
          .map { modId =>
            notifyApi.addNotification(
              Notification.make(Notification.Notifies(modId.value), IrwinDone(report.suspectId.value))
            )
          }
          .sequenceFu
          .void
      }
  }
}
