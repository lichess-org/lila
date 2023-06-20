package lila.irwin

import cats.syntax.all.*
import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.analyse.Analysis
import lila.analyse.AnalysisRepo
import lila.common.Bus
import lila.db.dsl.{ *, given }
import lila.game.{ Game, GameRepo, Pov, Query }
import lila.report.{ Mod, ModId, Report, Reporter, Suspect, SuspectId }
import lila.user.{ Me, User, UserRepo }

final class IrwinApi(
    reportColl: Coll,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    analysisRepo: AnalysisRepo,
    modApi: lila.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.notify.NotifyApi,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  lazy val thresholds = IrwinThresholds.makeSetting("irwin", settingStore)

  import BSONHandlers.given

  def dashboard: Fu[IrwinReport.Dashboard] =
    reportColl
      .find($empty)
      .sort($sort desc "date")
      .cursor[IrwinReport]()
      .list(20) dmap IrwinReport.Dashboard.apply

  object reports:

    def insert(data: IrwinReport) = for {
      prev <- get(data.userId)
      report = prev.fold(data)(_ add data)
      _ <- reportColl.update.one($id(report._id), report, upsert = true)
      _ <- markOrReport(report)
    } yield
      notification(report)
      lila.mon.mod.irwin.ownerReport(report.owner).increment().unit

    def get(user: User): Fu[Option[IrwinReport]] =
      get(user.id)

    def get(userId: UserId): Fu[Option[IrwinReport]] =
      reportColl.byId[IrwinReport](userId)

    def withPovs(user: User): Fu[Option[IrwinReport.WithPovs]] =
      get(user) flatMapz { report =>
        gameRepo.gamesTemporarilyFromPrimary(report.games.map(_.gameId)) dmap { games =>
          val povs = games.flatMap { g =>
            Pov(g, user) map { g.id -> _ }
          }.toMap
          IrwinReport.WithPovs(report, povs).some
        }

      }

    private def getSuspect(suspectId: UserId) =
      userRepo byId suspectId orFail s"suspect $suspectId not found" dmap Suspect.apply

    private def markOrReport(report: IrwinReport): Funit =
      userRepo.getTitle(report.suspectId.value) flatMap { title =>
        if (report.activation >= thresholds.get().mark && title.isEmpty)
          modApi.autoMark(report.suspectId, report.note)(using User.irwinId.into(Me.Id)) >>-
            lila.mon.mod.irwin.mark.increment().unit
        else if (report.activation >= thresholds.get().report) for {
          suspect <- getSuspect(report.suspectId.value)
          irwin   <- userRepo.irwin orFail s"Irwin user not found" dmap Mod.apply
          _ <- reportApi.create(
            Report.Candidate(
              reporter = Reporter(irwin.user),
              suspect = suspect,
              reason = lila.report.Reason.Cheat,
              text = s"${report.activation}% over ${report.games.size} games"
            )
          )
        } yield lila.mon.mod.irwin.report.increment().unit
        else funit
      }

  object requests:

    import IrwinRequest.Origin

    def fromMod(suspect: Suspect)(using Me) =
      notification.add(suspect.id)
      insert(suspect, _.Moderator)

    private[irwin] def insert(suspect: Suspect, origin: Origin.type => Origin): Funit =
      for
        analyzed <- getAnalyzedGames(suspect, 15)
        more     <- getMoreGames(suspect, 20 - analyzed.size)
        all = analyzed.map { (game, analysis) =>
          game -> analysis.some
        } ::: more.map(_ -> none)
      yield Bus.publish(
        IrwinRequest(
          suspect = suspect,
          origin = origin(Origin),
          games = all
        ),
        "irwin"
      )

    private[irwin] def fromTournamentLeaders(suspects: List[Suspect]): Funit =
      suspects.traverse_(insert(_, _.Tournament))

    private[irwin] def topOnline(leaders: List[Suspect]): Funit =
      leaders.traverse_(insert(_, _.Leaderboard))

    import lila.game.BSONHandlers.given

    private def baseQuery(suspect: Suspect) =
      Query.finished ++
        Query.variantStandard ++
        Query.rated ++
        Query.user(suspect.id.value) ++
        Query.turnsGt(20) ++
        Query.createdSince(nowInstant minusMonths 6)

    private def getAnalyzedGames(suspect: Suspect, nb: Int): Fu[List[(Game, Analysis)]] =
      gameRepo.coll
        .find(baseQuery(suspect) ++ Query.analysed(true))
        .sort(Query.sortCreated)
        .cursor[Game](ReadPreference.secondaryPreferred)
        .list(nb)
        .flatMap(analysisRepo.associateToGames)

    private def getMoreGames(suspect: Suspect, nb: Int): Fu[List[Game]] =
      (nb > 0) so
        gameRepo.coll
          .find(baseQuery(suspect) ++ Query.analysed(false))
          .sort(Query.sortCreated)
          .cursor[Game](ReadPreference.secondaryPreferred)
          .list(nb)

  object notification:

    private var subs = Map.empty[SuspectId, Set[ModId]]

    def add(suspectId: SuspectId)(using me: Me.Id): Unit =
      subs = subs.updated(suspectId, ~subs.get(suspectId) + me.modId)

    private[IrwinApi] def apply(report: IrwinReport): Funit =
      subs.get(report.suspectId) so { modIds =>
        subs = subs - report.suspectId
        modIds
          .map { modId =>
            notifyApi.notifyOne(modId, lila.notify.IrwinDone(report.suspectId.value))
          }
          .parallel
          .void
      }
