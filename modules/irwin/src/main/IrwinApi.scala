package lila.irwin

import reactivemongo.api.bson.*

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.common.Bus
import lila.core.report.SuspectId
import lila.core.userId.ModId
import lila.db.dsl.{ *, given }
import lila.game.{ GameRepo, Query }
import lila.report.{ Mod, Report, Reporter, Suspect }

final class IrwinApi(
    reportColl: Coll,
    gameRepo: GameRepo,
    userApi: lila.core.user.UserApi,
    analysisRepo: AnalysisRepo,
    modApi: lila.core.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.core.notify.NotifyApi,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  lazy val thresholds = IrwinThresholds.makeSetting("irwin", settingStore)

  import BSONHandlers.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    reportColl.delete.one($id(del.id))

  def dashboard: Fu[IrwinReport.Dashboard] =
    reportColl
      .find($empty)
      .sort($sort.desc("date"))
      .cursor[IrwinReport]()
      .list(20)
      .dmap(IrwinReport.Dashboard.apply)

  object reports:

    def insert(data: IrwinReport) = for
      prev <- get(data.userId)
      report = prev.fold(data)(_.add(data))
      _ <- reportColl.update.one($id(report._id), report, upsert = true)
      _ <- markOrReport(report)
    yield
      notification(report)
      lila.mon.mod.irwin.ownerReport(report.owner).increment()

    def get(user: User): Fu[Option[IrwinReport]] =
      get(user.id)

    def get(userId: UserId): Fu[Option[IrwinReport]] =
      reportColl.byId[IrwinReport](userId)

    def withPovs(user: User): Fu[Option[IrwinReport.WithPovs]] =
      get(user).flatMapz { report =>
        gameRepo.gamesFromSecondary(report.games.map(_.gameId)).map { games =>
          val povs = games.flatMap { g =>
            Pov(g, user).map { g.id -> _ }
          }.toMap
          IrwinReport.WithPovs(report, povs).some
        }
      }

    private def getSuspect(suspectId: UserId) =
      userApi.byId(suspectId).orFail(s"suspect $suspectId not found").dmap(Suspect.apply)

    private def markOrReport(report: IrwinReport): Funit =
      userApi.getTitle(report.suspectId.value).flatMap { title =>
        if report.activation >= thresholds.get().mark && title.isEmpty then
          for _ <- modApi.autoMark(report.suspectId, report.note)(using UserId.irwin.into(MyId))
          yield lila.mon.mod.irwin.mark.increment()
        else if report.activation >= thresholds.get().report then
          for
            suspect <- getSuspect(report.suspectId.value)
            irwin <- userApi.byId(UserId.irwin).orFail("Irwin user not found").dmap(Mod.apply)
            _ <- reportApi.create(
              Report.Candidate(
                reporter = Reporter(irwin.user),
                suspect = suspect,
                reason = lila.report.Reason.Cheat,
                text = s"${report.activation}% over ${report.games.size} games"
              )
            )
          yield lila.mon.mod.irwin.report.increment()
        else funit
      }

  object requests:

    import IrwinRequest.Origin

    def fromMod(suspect: Suspect)(using Me) =
      notification.add(suspect.id)
      insert(suspect, _.Moderator)

    private[irwin] def insert(suspect: Suspect, origin: Origin.type => Origin): Funit =
      suspect.user.noBot.so:
        for
          analyzed <- getAnalyzedGames(suspect, 15)
          more <- getMoreGames(suspect, 20 - analyzed.size)
          all = analyzed.map { (game, analysis) =>
            game -> analysis.some
          } ::: more.map(_ -> none)
        yield Bus.pub:
          IrwinRequest(suspect = suspect, origin = origin(Origin), games = all)

    private[irwin] def fromTournamentLeaders(suspects: List[Suspect]): Funit =
      suspects.sequentiallyVoid(insert(_, _.Tournament))

    private[irwin] def topOnline(leaders: List[Suspect]): Funit =
      leaders.sequentiallyVoid(insert(_, _.Leaderboard))

    import lila.game.BSONHandlers.given

    private def baseQuery(suspect: Suspect) =
      Query.finished ++
        Query.variantStandard ++
        Query.rated ++
        Query.user(suspect.id.value) ++
        Query.turnsGt(20) ++
        Query.createdSince(nowInstant.minusMonths(6))

    private def getAnalyzedGames(suspect: Suspect, nb: Int): Fu[List[(Game, Analysis)]] =
      gameRepo.coll
        .find(baseQuery(suspect) ++ Query.analysed(true))
        .sort(Query.sortCreated)
        .cursor[Game](ReadPref.sec)
        .list(nb)
        .flatMap(analysisRepo.associateToGames)

    private def getMoreGames(suspect: Suspect, nb: Int): Fu[List[Game]] =
      (nb > 0).so(
        gameRepo.coll
          .find(baseQuery(suspect) ++ Query.analysed(false))
          .sort(Query.sortCreated)
          .cursor[Game](ReadPref.sec)
          .list(nb)
      )

  object notification:

    private var subs = Map.empty[SuspectId, Set[ModId]]

    def add(suspectId: SuspectId)(using me: MyId): Unit =
      subs = subs.updated(suspectId, ~subs.get(suspectId) + me.modId)

    private[IrwinApi] def apply(report: IrwinReport): Funit =
      subs.get(report.suspectId).so { modIds =>
        subs = subs - report.suspectId
        modIds.toList.sequentiallyVoid: modId =>
          val notif = lila.core.notify.NotificationContent.IrwinDone(report.suspectId.value)
          notifyApi.notifyOne(modId, notif)
      }
