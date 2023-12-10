package lila.mod

import chess.{ Black, Color, White }
import reactivemongo.api.bson.*
import ornicar.scalalib.ThreadLocalRandom

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.db.dsl.{ *, given }
import lila.evaluation.Statistics
import lila.evaluation.{ AccountAction, PlayerAggregateAssessment, PlayerAssessment }
import lila.game.{ Game, Player, Pov, Source }
import lila.report.{ ModId, SuspectId }
import lila.user.User

final class AssessApi(
    assessRepo: AssessmentRepo,
    modApi: ModApi,
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    reporter: lila.hub.actors.Report,
    fishnet: lila.hub.actors.Fishnet,
    gameRepo: lila.game.GameRepo,
    analysisRepo: AnalysisRepo
)(using Executor):

  private def bottomDate = nowInstant.minusSeconds(3600 * 24 * 30 * 6) // matches a mongo expire index

  import lila.evaluation.EvaluationBsonHandlers.given
  import lila.analyse.AnalyseBsonHandlers.given

  private def createPlayerAssessment(assessed: PlayerAssessment) =
    assessRepo.coll.update.one($id(assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: UserId) =
    assessRepo.coll.byId[PlayerAssessment](id)

  private def getPlayerAssessmentsByUserId(userId: UserId, nb: Int) =
    assessRepo.coll
      .find($doc("userId" -> userId))
      .sort($sort desc "date")
      .cursor[PlayerAssessment](ReadPref.priTemp)
      .list(nb)

  private def getPlayerAggregateAssessment(
      userId: UserId,
      nb: Int = 100
  ): Fu[Option[PlayerAggregateAssessment]] =
    userApi withPerfs userId flatMap {
      _.filter(_.noBot) so { user =>
        getPlayerAssessmentsByUserId(userId, nb) map { games =>
          games.nonEmpty option PlayerAggregateAssessment(user, games)
        }
      }
    }

  def withGames(pag: PlayerAggregateAssessment): Fu[PlayerAggregateAssessment.WithGames] =
    gameRepo gamesTemporarilyFromPrimary pag.playerAssessments.map(_.gameId) map {
      PlayerAggregateAssessment.WithGames(pag, _)
    }

  private def buildMissing(povs: List[Pov]): Funit =
    assessRepo.coll
      .distinctEasy[GameId, Set]("gameId", $inIds(povs.map(p => s"${p.gameId}/${p.color.name}"))) flatMap {
      existingIds =>
        val missing = povs collect {
          case pov if pov.game.metadata.analysed && !existingIds.contains(pov.gameId) => pov.gameId
        }
        missing.nonEmpty so
          analysisRepo.coll
            .idsMap[Analysis, GameId](missing)(x => GameId(x.id.value))
            .flatMap { ans =>
              povs
                .flatMap { pov =>
                  ans get pov.gameId map { pov -> _ }
                }
                .map { case (pov, analysis) =>
                  gameRepo.holdAlert game pov.game flatMap { holdAlerts =>
                    createPlayerAssessment(PlayerAssessment.make(pov, analysis, holdAlerts(pov.color)))
                  }
                }
                .parallel
                .void
            }
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
          gameRepo.holdAlert.povs(basicsPovs) map { holds =>
            povs.map: pov =>
              pov -> {
                fulls.get(pov.gameId.value) match
                  case Some(full) => Left(full)
                  case None       => Right(PlayerAssessment.makeBasics(pov, holds get pov.gameId))
              }
          }

  def getPlayerAggregateAssessmentWithGames(
      userId: UserId,
      nb: Int = 100
  ): Fu[Option[PlayerAggregateAssessment.WithGames]] =
    getPlayerAggregateAssessment(userId, nb).flatMap(_ soFu withGames)

  def refreshAssessOf(user: User): Funit =
    !user.isBot so
      gameRepo.gamesForAssessment(user.id, 100).flatMap { gs =>
        gs.map: g =>
          analysisRepo.byGame(g) flatMapz {
            onAnalysisReady(g, _, thenAssessUser = false)
          }
        .parallel
          .void
      } >> assessUser(user.id)

  def onAnalysisReady(game: Game, analysis: Analysis, thenAssessUser: Boolean = true): Funit =
    gameRepo.holdAlert game game flatMap { holdAlerts =>
      def consistentMoveTimes(game: Game)(player: Player) =
        Statistics.moderatelyConsistentMoveTimes(Pov(game, player))
      val shouldAssess =
        if !game.source.exists(assessableSources.contains) then false
        else if game.mode.casual then false
        else if Player.HoldAlert suspicious holdAlerts then true
        else if game.isCorrespondence then false
        else if game.playedTurns < PlayerAssessment.minPlies then false
        else if game.players exists consistentMoveTimes(game) then true
        else if game.createdAt isBefore bottomDate then false
        else true
      shouldAssess.so {
        createPlayerAssessment(PlayerAssessment.make(game pov White, analysis, holdAlerts.white)) >>
          createPlayerAssessment(PlayerAssessment.make(game pov Black, analysis, holdAlerts.black))
      } >> {
        (shouldAssess && thenAssessUser) so {
          game.whitePlayer.userId.so(assessUser) >> game.blackPlayer.userId.so(assessUser)
        }
      }
    }

  def assessUser(userId: UserId): Funit =
    getPlayerAggregateAssessment(userId) flatMapz { playerAggregateAssessment =>
      playerAggregateAssessment.action match
        case AccountAction.Engine | AccountAction.EngineAndBan =>
          userRepo.getTitle(userId).flatMap {
            case None =>
              modApi
                .autoMark(
                  SuspectId(userId),
                  playerAggregateAssessment.reportText(3)
                )(using User.lichessIdAsMe)
            case Some(_) =>
              fuccess {
                reporter ! lila.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(3))
              }
          }
        case AccountAction.Report(_) =>
          fuccess {
            reporter ! lila.hub.actorApi.report.Cheater(userId, playerAggregateAssessment.reportText(3))
          }
        case AccountAction.Nothing =>
          // reporter ! lila.hub.actorApi.report.Clean(userId)
          funit
    }

  private val assessableSources: Set[Source] =
    Set(Source.Lobby, Source.Pool, Source.Arena, Source.Swiss, Source.Simul)

  private def randomPercent(percent: Int): Boolean =
    ThreadLocalRandom.nextInt(100) < percent

  def onGameReady(game: Game, white: User.WithPerf, black: User.WithPerf): Funit =

    import AutoAnalysis.Reason.*

    def manyBlurs(player: Player) =
      game.playerBlurPercent(player.color) >= 70

    def winnerGreatProgress(player: Player): Boolean =
      game.winner.has(player) && player.color.fold(white, black).perf.progress >= 90

    def noFastCoefVariation(player: Player): Option[Float] =
      Statistics.noFastMoves(Pov(game, player)) so Statistics.moveTimeCoefVariation(Pov(game, player))

    def winnerUserOption = game.winnerColor.map(_.fold(white, black))
    def winnerNbGames    = winnerUserOption.map(_.perf.nb)

    def suspCoefVariation(c: Color) =
      val x = noFastCoefVariation(game player c)
      x.filter(_ < 0.45f) orElse x.filter(_ < 0.5f).ifTrue(ThreadLocalRandom.nextBoolean())
    lazy val whiteSuspCoefVariation = suspCoefVariation(chess.White)
    lazy val blackSuspCoefVariation = suspCoefVariation(chess.Black)

    def isUpset = ~(for
      winner <- game.winner
      loser  <- game.loser
      wR     <- winner.stableRating
      lR     <- loser.stableRating
    yield wR <= lR - 300)

    val shouldAnalyse: Fu[Option[AutoAnalysis.Reason]] =
      if !game.analysable then fuccess(none)
      else if game.speed >= chess.Speed.Blitz && (white.hasTitle || black.hasTitle) then
        fuccess(TitledPlayer.some)
      else if !game.source.exists(assessableSources.contains) then fuccess(none)
      // give up on correspondence games
      else if game.isCorrespondence then fuccess(none)
      // stop here for short games
      else if game.playedTurns < PlayerAssessment.minPlies then fuccess(none)
      // stop here for long games
      else if game.playedTurns > 95 then fuccess(none)
      // stop here for casual games
      else if !game.mode.rated then fuccess(none)
      // discard old games
      else if game.createdAt isBefore bottomDate then fuccess(none)
      else if isUpset then fuccess(Upset.some)
      // white has consistent move times
      else if whiteSuspCoefVariation.isDefined then fuccess(WhiteMoveTime.some)
      // black has consistent move times
      else if blackSuspCoefVariation.isDefined then fuccess(BlackMoveTime.some)
      else
        // someone is using a bot
        gameRepo.holdAlert game game map { holdAlerts =>
          if Player.HoldAlert suspicious holdAlerts then HoldAlert.some
          // don't analyse most of other bullet games
          else if game.speed == chess.Speed.Bullet && randomPercent(70) then none
          // someone blurs a lot
          else if game.players exists manyBlurs then Blurs.some
          // the winner shows a great rating progress
          else if game.players exists winnerGreatProgress then WinnerRatingProgress.some
          // analyse some tourney games
          // else if (game.isTournament) randomPercent(20) option "Tourney random"
          /// analyse new player games
          else if winnerNbGames.so(40 >) && randomPercent(75) then NewPlayerWin.some
          else none
        }

    shouldAnalyse.mapz: reason =>
      lila.mon.cheat.autoAnalysis(reason.toString).increment()
      fishnet ! lila.hub.actorApi.fishnet.AutoAnalyse(game.id)
