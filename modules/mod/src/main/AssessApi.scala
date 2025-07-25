package lila.mod

import chess.{ Black, ByColor, Color, White }
import chess.rating.IntRatingDiff
import reactivemongo.api.bson.*
import scalalib.ThreadLocalRandom

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.core.game.{ Player, Source }
import lila.core.report.SuspectId
import lila.db.dsl.{ *, given }
import lila.evaluation.{ AccountAction, PlayerAggregateAssessment, PlayerAssessment, Statistics }
import lila.game.GameExt.playerBlurPercent

final class AssessApi(
    assessRepo: AssessmentRepo,
    modApi: ModApi,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    gameRepo: lila.game.GameRepo,
    gameApi: lila.core.game.GameApi,
    analysisRepo: AnalysisRepo,
    reportApi: lila.core.report.ReportApi
)(using Executor):

  private def bottomDate = nowInstant.minusSeconds(3600 * 24 * 30 * 6) // matches a mongo expire index

  import lila.evaluation.EvaluationBsonHandlers.given
  import lila.analyse.AnalyseBsonHandlers.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    assessRepo.coll.delete.one($id(del.id))

  private def createPlayerAssessment(assessed: PlayerAssessment) =
    assessRepo.coll.update.one($id(assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: UserId) =
    assessRepo.coll.byId[PlayerAssessment](id)

  private def getPlayerAssessmentsByUserId(userId: UserId, nb: Int) =
    assessRepo.coll
      .find($doc("userId" -> userId))
      .sort($sort.desc("date"))
      .cursor[PlayerAssessment](ReadPref.sec)
      .list(nb)

  private def getPlayerAggregateAssessment(
      userId: UserId,
      nb: Int = 100
  ): Fu[Option[PlayerAggregateAssessment]] =
    userApi
      .withPerfs(userId)
      .flatMap:
        _.filter(_.noBot).so { user =>
          getPlayerAssessmentsByUserId(userId, nb).map { games =>
            games.nonEmpty.option(PlayerAggregateAssessment(user, games))
          }
        }

  def withGames(pag: PlayerAggregateAssessment): Fu[PlayerAggregateAssessment.WithGames] =
    gameRepo
      .gamesFromSecondary(pag.playerAssessments.map(_.gameId))
      .map:
        PlayerAggregateAssessment.WithGames(pag, _)

  private def buildMissing(povs: List[Pov]): Funit =
    assessRepo.coll
      .distinctEasy[GameId, Set]("gameId", $inIds(povs.map(p => s"${p.gameId}/${p.color.name}")))
      .flatMap { existingIds =>
        val missing = povs.collect:
          case pov if pov.game.metadata.analysed && !existingIds.contains(pov.gameId) => pov.gameId
        missing.nonEmpty.so(
          analysisRepo.coll
            .idsMap[Analysis, GameId](missing)(x => GameId(x.id.value))
            .flatMap { ans =>
              povs
                .flatMap: pov =>
                  ans.get(pov.gameId).map { pov -> _ }
                .parallelVoid: (pov, analysis) =>
                  gameRepo.holdAlert.game(pov.game).flatMap { holdAlerts =>
                    createPlayerAssessment(PlayerAssessment.make(pov, analysis, holdAlerts(pov.color)))
                  }
            }
        )
      }

  def makeAndGetFullOrBasicsFor(
      povs: List[Pov]
  ): Fu[List[(Pov, Either[PlayerAssessment, PlayerAssessment.Basics])]] =
    buildMissing(povs) >>
      assessRepo.coll
        .idsMap[PlayerAssessment, String](
          ids = povs.map(p => s"${p.gameId}/${p.color.name}"),
          readPref = _.pri
        )(_.gameId.value)
        .flatMap: fulls =>
          val basicsPovs = povs.filterNot(p => fulls.exists(_._1 == p.gameId.value))
          gameRepo.holdAlert.povs(basicsPovs).map { holds =>
            povs.map: pov =>
              pov -> {
                fulls.get(pov.gameId.value) match
                  case Some(full) => Left(full)
                  case None => Right(PlayerAssessment.makeBasics(pov, holds.get(pov.gameId)))
              }
          }

  def getPlayerAggregateAssessmentWithGames(
      userId: UserId,
      nb: Int = 100
  ): Fu[Option[PlayerAggregateAssessment.WithGames]] =
    getPlayerAggregateAssessment(userId, nb).flatMap(_.soFu(withGames))

  def refreshAssessOf(user: User): Funit =
    (!user.isBot).so:
      gameRepo.gamesForAssessment(user.id, 100).flatMap { gs =>
        gs.parallelVoid: g =>
          analysisRepo
            .byGame(g)
            .flatMapz:
              onAnalysisReady(g, _, thenAssessUser = false)
      } >> assessUser(user.id)

  def onAnalysisReady(game: Game, analysis: Analysis, thenAssessUser: Boolean = true): Funit =
    gameRepo.holdAlert.game(game).flatMap { holdAlerts =>
      def consistentMoveTimes(game: Game)(player: Player) =
        Statistics.moderatelyConsistentMoveTimes(Pov(game, player))
      val shouldAssess =
        if !game.source.exists(assessableSources.contains) then false
        else if game.rated.no then false
        else if lila.game.Player.HoldAlert.suspicious(holdAlerts) then true
        else if game.isCorrespondence then false
        else if game.playedPlies < PlayerAssessment.minPlies then false
        else if game.players.exists(consistentMoveTimes(game)) then true
        else if game.createdAt.isBefore(bottomDate) then false
        else true
      shouldAssess.so {
        createPlayerAssessment(PlayerAssessment.make(game.pov(White), analysis, holdAlerts.white)) >>
          createPlayerAssessment(PlayerAssessment.make(game.pov(Black), analysis, holdAlerts.black))
      } >> {
        (shouldAssess && thenAssessUser).so:
          game.whitePlayer.userId.so(assessUser) >> game.blackPlayer.userId.so(assessUser)
      }
    }

  def assessUser(userId: UserId): Funit =
    getPlayerAggregateAssessment(userId).flatMapz { playerAggregateAssessment =>
      playerAggregateAssessment.action match
        case AccountAction.Engine | AccountAction.EngineAndBan =>
          userRepo
            .getTitle(userId)
            .flatMap:
              case None =>
                modApi
                  .autoMark(
                    SuspectId(userId),
                    playerAggregateAssessment.reportText(3)
                  )(using UserId.lichessAsMe)
              case Some(_) =>
                reportApi.autoCheatReport(userId, playerAggregateAssessment.reportText(3))
        case AccountAction.Report(_) =>
          reportApi.autoCheatReport(userId, playerAggregateAssessment.reportText(3))
        case AccountAction.Nothing =>
          funit
    }

  private val assessableSources: Set[Source] =
    Set(Source.Lobby, Source.Pool, Source.Arena, Source.Swiss, Source.Simul)

  private def randomPercent(percent: Int): Boolean =
    ThreadLocalRandom.nextInt(100) < percent

  def onGameReady(game: Game, players: ByColor[lila.core.user.WithPerf]): Funit =

    import AutoAnalysis.Reason.*

    def manyBlurs(player: Player) =
      game.playerBlurPercent(player.color) >= 70

    def winnerGreatProgress(player: Player): Boolean =
      game.winner.has(player) && players(player.color).perf.progress >= IntRatingDiff(80)

    def noFastCoefVariation(player: Player): Option[Float] =
      Statistics.noFastMoves(Pov(game, player)).so(Statistics.moveTimeCoefVariation(Pov(game, player)))

    def winnerUserOption = game.winnerColor.map(players(_))
    def winnerNbGames = winnerUserOption.map(_.perf.nb)

    def suspCoefVariation(c: Color): Boolean =
      val x = noFastCoefVariation(game.player(c))
      x.exists(_ < 0.47f) || (x.exists(_ < 0.53f) && ThreadLocalRandom.nextBoolean())

    def isUpset = ~(for
      winner <- game.winner
      loser <- game.loser
      wR <- winner.rating
      lR <- loser.stableRating
    yield wR <= lR.map(_ - 250))

    val shouldAnalyse: Fu[Option[AutoAnalysis.Reason]] =
      if !gameApi.analysable(game) then fuccess(none)
      else if game.speed >= chess.Speed.Blitz && players.exists(_.user.hasTitle) then
        fuccess(TitledPlayer.some)
      else if !game.source.exists(assessableSources.contains) then fuccess(none)
      // give up on correspondence games
      else if game.isCorrespondence then fuccess(none)
      // stop here for short games
      else if game.playedPlies < PlayerAssessment.minPlies then fuccess(none)
      // stop here for long games
      else if game.playedPlies > 95 then fuccess(none)
      // stop here for casual games
      else if game.rated.no then fuccess(none)
      // discard old games
      else if game.createdAt.isBefore(bottomDate) then fuccess(none)
      else if isUpset then fuccess(Upset.some)
      // white has consistent move times
      else if suspCoefVariation(White) then fuccess(WhiteMoveTime.some)
      // black has consistent move times
      else if suspCoefVariation(Black) then fuccess(BlackMoveTime.some)
      else
        // someone is using a bot
        gameRepo.holdAlert.game(game).map { holdAlerts =>
          if lila.game.Player.HoldAlert.suspicious(holdAlerts) then HoldAlert.some
          // don't analyse most of other bullet games
          else if game.speed == chess.Speed.Bullet && randomPercent(70) then none
          // someone blurs a lot
          else if game.players.exists(manyBlurs) then Blurs.some
          // the winner shows a great rating progress
          else if game.players.exists(winnerGreatProgress) then WinnerRatingProgress.some
          // analyse some tourney games
          // else if (game.isTournament) randomPercent(20) option "Tourney random"
          /// analyse new player games
          else if winnerNbGames.so(_ < 40) && randomPercent(75) then NewPlayerWin.some
          else none
        }

    shouldAnalyse.mapz: reason =>
      lila.mon.cheat.autoAnalysis(reason.toString).increment()
      lila.common.Bus.pub(lila.core.fishnet.Bus.GameRequest(game.id))
