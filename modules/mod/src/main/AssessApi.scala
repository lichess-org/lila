package lila.mod

import chess.{ Black, Color, White }
import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.common.ThreadLocalRandom
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.evaluation.Statistics
import lila.evaluation.{ AccountAction, PlayerAggregateAssessment, PlayerAssessment }
import lila.game.{ Game, Player, Pov, Source }
import lila.report.{ ModId, SuspectId }
import lila.user.User

final class AssessApi(
    assessRepo: AssessmentRepo,
    modApi: ModApi,
    userRepo: lila.user.UserRepo,
    reporter: lila.hub.actors.Report,
    fishnet: lila.hub.actors.Fishnet,
    gameRepo: lila.game.GameRepo,
    analysisRepo: AnalysisRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private def bottomDate = DateTime.now.minusSeconds(3600 * 24 * 30 * 6) // matches a mongo expire index

  import lila.evaluation.EvaluationBsonHandlers._
  import Analysis.analysisBSONHandler

  private def createPlayerAssessment(assessed: PlayerAssessment) =
    assessRepo.coll.update.one($id(assessed._id), assessed, upsert = true).void

  def getPlayerAssessmentById(id: User.ID) =
    assessRepo.coll.byId[PlayerAssessment](id)

  private def getPlayerAssessmentsByUserId(userId: User.ID, nb: Int) =
    assessRepo.coll
      .find($doc("userId" -> userId))
      .sort($sort desc "date")
      .cursor[PlayerAssessment](ReadPreference.secondaryPreferred)
      .list(nb)

  private def getPlayerAggregateAssessment(
      userId: User.ID,
      nb: Int = 100
  ): Fu[Option[PlayerAggregateAssessment]] =
    userRepo byId userId flatMap {
      _.filter(_.noBot) ?? { user =>
        getPlayerAssessmentsByUserId(userId, nb) map { games =>
          games.nonEmpty option PlayerAggregateAssessment(user, games)
        }
      }
    }

  def withGames(pag: PlayerAggregateAssessment): Fu[PlayerAggregateAssessment.WithGames] =
    gameRepo gamesFromSecondary pag.playerAssessments.map(_.gameId) map {
      PlayerAggregateAssessment.WithGames(pag, _)
    }

  private def buildMissing(povs: List[Pov]): Funit =
    assessRepo.coll
      .distinctEasy[Game.ID, Set]("gameId", $inIds(povs.map(p => s"${p.gameId}/${p.color.name}"))) flatMap {
      existingIds =>
        val missing = povs collect {
          case pov if pov.game.metadata.analysed && !existingIds.contains(pov.gameId) => pov.gameId
        }
        missing.nonEmpty ??
          analysisRepo.coll
            .idsMap[Analysis, Game.ID](missing)(_.id)
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
                .sequenceFu
                .void
            }
    }

  def makeAndGetFullOrBasicsFor(
      povs: List[Pov]
  ): Fu[List[(Pov, Either[PlayerAssessment, PlayerAssessment.Basics])]] =
    buildMissing(povs) >>
      assessRepo.coll
        .idsMap[PlayerAssessment, Game.ID](
          ids = povs.map(p => s"${p.gameId}/${p.color.name}"),
          readPreference = ReadPreference.secondaryPreferred
        )(_.gameId)
        .flatMap { fulls =>
          val basicsPovs = povs.filterNot(p => fulls.exists(_._1 == p.gameId))
          gameRepo.holdAlert.povs(basicsPovs) map { holds =>
            povs map { pov =>
              pov -> {
                fulls.get(pov.gameId) match {
                  case Some(full) => Left(full)
                  case None       => Right(PlayerAssessment.makeBasics(pov, holds get pov.gameId))
                }
              }
            }
          }
        }

  def getPlayerAggregateAssessmentWithGames(
      userId: User.ID,
      nb: Int = 100
  ): Fu[Option[PlayerAggregateAssessment.WithGames]] =
    getPlayerAggregateAssessment(userId, nb) flatMap {
      _ ?? { pag =>
        withGames(pag) dmap some
      }
    }

  def refreshAssessOf(user: User): Funit =
    !user.isBot ??
      (gameRepo.gamesForAssessment(user.id, 100) flatMap { gs =>
        (gs map { g =>
          analysisRepo.byGame(g) flatMap {
            _ ?? { onAnalysisReady(g, _, thenAssessUser = false) }
          }
        }).sequenceFu.void
      }) >> assessUser(user.id)

  def onAnalysisReady(game: Game, analysis: Analysis, thenAssessUser: Boolean = true): Funit =
    gameRepo.holdAlert game game flatMap { holdAlerts =>
      def consistentMoveTimes(game: Game)(player: Player) =
        Statistics.moderatelyConsistentMoveTimes(Pov(game, player))
      val shouldAssess =
        if (!game.source.exists(assessableSources.contains)) false
        else if (game.mode.casual) false
        else if (Player.HoldAlert suspicious holdAlerts) true
        else if (game.isCorrespondence) false
        else if (game.playedTurns < 40) false
        else if (game.players exists consistentMoveTimes(game)) true
        else if (game.createdAt isBefore bottomDate) false
        else true
      shouldAssess.?? {
        createPlayerAssessment(PlayerAssessment.make(game pov White, analysis, holdAlerts.white)) >>
          createPlayerAssessment(PlayerAssessment.make(game pov Black, analysis, holdAlerts.black))
      } >> {
        (shouldAssess && thenAssessUser) ?? {
          game.whitePlayer.userId.??(assessUser) >> game.blackPlayer.userId.??(assessUser)
        }
      }
    }

  def assessUser(userId: User.ID): Funit =
    getPlayerAggregateAssessment(userId) flatMap {
      case Some(playerAggregateAssessment) =>
        playerAggregateAssessment.action match {
          case AccountAction.Engine | AccountAction.EngineAndBan =>
            userRepo.getTitle(userId).flatMap {
              case None => modApi.autoMark(SuspectId(userId), ModId.lichess)
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
      case _ => funit
    }

  private val assessableSources: Set[Source] =
    Set(Source.Lobby, Source.Pool, Source.Tournament, Source.Swiss, Source.Simul)

  private def randomPercent(percent: Int): Boolean =
    ThreadLocalRandom.nextInt(100) < percent

  def onGameReady(game: Game, white: User, black: User): Funit = {

    import AutoAnalysis.Reason._

    def manyBlurs(player: Player) =
      game.playerBlurPercent(player.color) >= 70

    def winnerGreatProgress(player: Player): Boolean = {
      game.winner ?? (player ==)
    } && game.perfType ?? { perfType =>
      player.color.fold(white, black).perfs(perfType).progress >= 100
    }

    def noFastCoefVariation(player: Player): Option[Float] =
      Statistics.noFastMoves(Pov(game, player)) ?? Statistics.moveTimeCoefVariation(Pov(game, player))

    def winnerUserOption = game.winnerColor.map(_.fold(white, black))
    def winnerNbGames =
      for {
        user     <- winnerUserOption
        perfType <- game.perfType
      } yield user.perfs(perfType).nb

    def suspCoefVariation(c: Color) = {
      val x = noFastCoefVariation(game player c)
      x.filter(_ < 0.45f) orElse x.filter(_ < 0.5f).ifTrue(ThreadLocalRandom.nextBoolean())
    }
    lazy val whiteSuspCoefVariation = suspCoefVariation(chess.White)
    lazy val blackSuspCoefVariation = suspCoefVariation(chess.Black)

    val shouldAnalyse: Fu[Option[AutoAnalysis.Reason]] =
      if (!game.analysable) fuccess(none)
      else if (!game.source.exists(assessableSources.contains)) fuccess(none)
      // give up on correspondence games
      else if (game.isCorrespondence) fuccess(none)
      // stop here for short games
      else if (game.playedTurns < 36) fuccess(none)
      // stop here for long games
      else if (game.playedTurns > 90) fuccess(none)
      // stop here for casual games
      else if (!game.mode.rated) fuccess(none)
      // discard old games
      else if (game.createdAt isBefore bottomDate) fuccess(none)
      // someone is using a bot
      else
        gameRepo.holdAlert game game map { holdAlerts =>
          if (Player.HoldAlert suspicious holdAlerts) HoldAlert.some
          // white has consistent move times
          else if (whiteSuspCoefVariation.isDefined && randomPercent(70))
            whiteSuspCoefVariation.map(_ => WhiteMoveTime)
          // black has consistent move times
          else if (blackSuspCoefVariation.isDefined && randomPercent(70))
            blackSuspCoefVariation.map(_ => BlackMoveTime)
          // don't analyse half of other bullet games
          else if (game.speed == chess.Speed.Bullet && randomPercent(50)) none
          // someone blurs a lot
          else if (game.players exists manyBlurs) Blurs.some
          // the winner shows a great rating progress
          else if (game.players exists winnerGreatProgress) WinnerRatingProgress.some
          // analyse some tourney games
          // else if (game.isTournament) randomPercent(20) option "Tourney random"
          /// analyse new player games
          else if (winnerNbGames.??(30 >) && randomPercent(75)) NewPlayerWin.some
          else none
        }

    shouldAnalyse map {
      _ ?? { reason =>
        lila.mon.cheat.autoAnalysis(reason.toString).increment()
        fishnet ! lila.hub.actorApi.fishnet.AutoAnalyse(game.id)
      }
    }
  }
}
