package lila.api

import akka.stream.scaladsl._
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._

import shogi.format.Tag
import lila.analyse.{ Analysis, JsonView => analysisJson }
import lila.common.config.MaxPerSecond
import lila.common.Json.jodaWrites
import lila.common.{ HTTPRequest, LightUser }
import lila.db.dsl._
import lila.team.GameTeams
import lila.game.JsonView._
import lila.game.NotationDump.WithFlags
import lila.game.{ Game, PerfPicker, Query }
import lila.tournament.Tournament
import lila.user.User

final class GameApiV2(
    notationDump: NotationDump,
    gameRepo: lila.game.GameRepo,
    tournamentRepo: lila.tournament.TournamentRepo,
    pairingRepo: lila.tournament.PairingRepo,
    playerRepo: lila.tournament.PlayerRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    getLightUser: LightUser.Getter,
    realPlayerApi: RealPlayerApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import GameApiV2._

  private val keepAliveInterval = 70.seconds // play's idleTimeout = 75s

  def exportOne(game: Game, configInput: OneConfig): Fu[String] = {
    val config = configInput.copy(
      flags = configInput.flags
        .copy(
          evals = configInput.flags.evals && !game.playable,
          delayMoves = !configInput.noDelay
        )
    )
    game.notationImport ifTrue config.imported match {
      case Some(imported) if config.flags.csa == imported.isCsa => fuccess(imported.notation)
      case _ =>
        for {
          realPlayers      <- config.playerFile.??(realPlayerApi.apply)
          (game, analysis) <- enrich(config.flags)(game)
          notationExport <- config.format match {
            case Format.JSON =>
              toJson(game, analysis, config.flags, realPlayers = realPlayers) dmap Json.stringify
            case Format.NOTATION =>
              notationDump(
                game,
                analysis,
                config.flags,
                realPlayers = realPlayers
              ) dmap notationDump.toNotationString
          }
        } yield notationExport
    }
  }

  private def fileType(configInput: Config) = configInput.format match {
    case Format.NOTATION => if (configInput.flags.csa) "csa" else "kif"
    case Format.JSON     => Format.toString.toLowerCase
  }

  private val fileR = """[\s,]""".r
  def filename(game: Game, configInput: Config): Fu[String] =
    gameLightUsers(game) map { case (wu, bu) =>
      java.net.URLEncoder.encode(
        fileR.replaceAllIn(
          "lishogi_game_%s_%s_vs_%s_%s.%s".format(
            Tag.UTCDate.format.print(game.createdAt),
            notationDump.dumper.player(game.sentePlayer, wu),
            notationDump.dumper.player(game.gotePlayer, bu),
            game.id,
            fileType(configInput)
          ),
          "_"
        ),
        "UTF-8"
      )
    }
  def filename(tour: Tournament, configInput: Config): String =
    java.net.URLEncoder.encode(
      fileR.replaceAllIn(
        "lishogi_tournament_%s_%s_%s.%s".format(
          Tag.UTCDate.format.print(tour.startsAt),
          tour.id,
          lila.common.String.slugify(tour.name),
          fileType(configInput)
        ),
        "_"
      ),
      "UTF-8"
    )

  def exportByUser(config: ByUserConfig): Source[String, _] =
    Source futureSource {
      config.playerFile.??(realPlayerApi.apply) map { realPlayers =>
        gameRepo
          .sortedCursor(
            config.vs.fold(Query.user(config.user.id)) { Query.opponents(config.user, _) } ++
              Query.createdBetween(config.since, config.until) ++
              (!config.ongoing).??(Query.finished),
            Query.sortCreated,
            batchSize = config.perSecond.value
          )
          .documentSource()
          .map(g => config.postFilter(g) option g)
          .throttle(config.perSecond.value * 10, 1 second, e => if (e.isDefined) 10 else 2)
          .mapConcat(_.toList)
          .take(config.max | Int.MaxValue)
          .via(preparationFlow(config, realPlayers))
          .keepAlive(keepAliveInterval, () => emptyMsgFor(config))
      }
    }

  def exportByIds(config: ByIdsConfig): Source[String, _] =
    Source futureSource {
      config.playerFile.??(realPlayerApi.apply) map { realPlayers =>
        gameRepo
          .sortedCursor(
            $inIds(config.ids),
            Query.sortCreated,
            batchSize = config.perSecond.value
          )
          .documentSource()
          .throttle(config.perSecond.value, 1 second)
          .via(preparationFlow(config, realPlayers))
      }
    }

  def exportByTournament(config: ByTournamentConfig): Source[String, _] =
    Source futureSource {
      tournamentRepo.isTeamBattle(config.tournamentId) map { isTeamBattle =>
        pairingRepo
          .sortedCursor(
            tournamentId = config.tournamentId,
            batchSize = config.perSecond.value
          )
          .documentSource()
          .grouped(config.perSecond.value)
          .throttle(1, 1 second)
          .mapAsync(1) { pairings =>
            isTeamBattle.?? {
              playerRepo.teamsOfPlayers(config.tournamentId, pairings.flatMap(_.users).distinct).dmap(_.toMap)
            } flatMap { playerTeams =>
              gameRepo.gameOptionsFromSecondary(pairings.map(_.gameId)) map {
                _.zip(pairings) collect { case (Some(game), pairing) =>
                  import cats.implicits._
                  (
                    game,
                    pairing,
                    (
                      playerTeams.get(pairing.user1),
                      playerTeams.get(
                        pairing.user2
                      )
                    ) mapN shogi.Color.Map.apply[String]
                  )
                }
              }
            }
          }
          .mapConcat(identity)
          .mapAsync(4) { case (game, pairing, teams) =>
            enrich(config.flags)(game) dmap { (_, pairing, teams) }
          }
          .mapAsync(4) { case ((game, analysis), pairing, teams) =>
            config.format match {
              case Format.NOTATION => notationDump.formatter(config.flags)(game, analysis, teams, none)
              case Format.JSON =>
                def addBerserk(color: shogi.Color)(json: JsObject) =
                  if (pairing berserkOf color)
                    json deepMerge Json.obj(
                      "players" -> Json.obj(color.name -> Json.obj("berserk" -> true))
                    )
                  else json
                toJson(game, analysis, config.flags, teams) dmap
                  addBerserk(shogi.Sente) dmap
                  addBerserk(shogi.Gote) dmap { json =>
                    s"${Json.stringify(json)}\n"
                  }
            }
          }
      }
    }

  private def preparationFlow(config: Config, realPlayers: Option[RealPlayers]) =
    Flow[Game]
      .mapAsync(4)(enrich(config.flags))
      .mapAsync(4) { case (game, analysis) =>
        formatterFor(config)(game, analysis, None, realPlayers)
      }

  private def enrich(flags: WithFlags)(game: Game) =
    (flags.evals ?? analysisRepo.byGame(game)) dmap {
      (game, _)
    }

  private def formatterFor(config: Config) =
    config.format match {
      case Format.NOTATION => notationDump.formatter(config.flags)
      case Format.JSON     => jsonFormatter(config.flags)
    }

  private def emptyMsgFor(config: Config) =
    config.format match {
      case Format.NOTATION => "\n"
      case Format.JSON     => "{}\n"
    }

  private def jsonFormatter(flags: WithFlags) =
    (
        game: Game,
        analysis: Option[Analysis],
        teams: Option[GameTeams],
        realPlayers: Option[RealPlayers]
    ) =>
      toJson(game, analysis, flags, teams, realPlayers) dmap { json =>
        s"${Json.stringify(json)}\n"
      }

  private def toJson(
      g: Game,
      analysisOption: Option[Analysis],
      withFlags: WithFlags,
      teams: Option[GameTeams] = None,
      realPlayers: Option[RealPlayers] = None
  ): Fu[JsObject] =
    for {
      lightUsers <- gameLightUsers(g) dmap { case (wu, bu) => List(wu, bu) }
      notation <-
        withFlags.notationInJson ?? notationDump
          .apply(g, analysisOption, withFlags, realPlayers = realPlayers)
          .dmap(notationDump.toNotationString)
          .dmap(some)
    } yield Json
      .obj(
        "id"         -> g.id,
        "rated"      -> g.rated,
        "variant"    -> g.variant.key,
        "speed"      -> g.speed.key,
        "perf"       -> PerfPicker.key(g),
        "createdAt"  -> g.createdAt,
        "lastMoveAt" -> g.movedAt,
        "status"     -> g.status.name,
        "players" -> JsObject(g.players zip lightUsers map { case (p, user) =>
          p.color.name -> Json
            .obj()
            .add("user", user)
            .add("rating", p.rating)
            .add("ratingDiff", p.ratingDiff)
            .add("name", p.name)
            .add("provisional" -> p.provisional)
            .add("aiLevel" -> p.aiLevel)
            .add("analysis" -> analysisOption.flatMap(analysisJson.player(g pov p.color)))
            .add("team" -> teams.map(_(p.color)))
        // .add("moveCentis" -> withFlags.moveTimes ?? g.moveTimes(p.color).map(_.map(_.centis)))
        })
      )
      .add("initialSfen" -> g.initialSfen)
      .add("winner" -> g.winnerColor.map(_.name))
      .add("moves" -> withFlags.moves.option {
        withFlags keepDelayIf g.playable applyDelay g.usis.map(_.usi) mkString " "
      })
      .add("notation" -> notation)
      .add("daysPerTurn" -> g.daysPerTurn)
      .add("analysis" -> analysisOption.ifTrue(withFlags.evals).map(analysisJson.moves(_, withGlyph = false)))
      .add("tournament" -> g.tournamentId)
      .add("clock" -> g.clock.map { clock =>
        Json.obj(
          "initial"   -> clock.limitSeconds,
          "increment" -> clock.incrementSeconds,
          "byoyomi"   -> clock.byoyomiSeconds,
          "periods"   -> clock.periodsTotal,
          "totalTime" -> clock.estimateTotalSeconds
        )
      })

  private def gameLightUsers(game: Game): Fu[(Option[LightUser], Option[LightUser])] =
    (game.sentePlayer.userId ?? getLightUser) zip (game.gotePlayer.userId ?? getLightUser)
}

object GameApiV2 {

  sealed trait Format
  object Format {
    case object NOTATION extends Format
    case object JSON     extends Format
    def byRequest(req: play.api.mvc.RequestHeader) = if (HTTPRequest acceptsNdJson req) JSON else NOTATION
  }

  sealed trait Config {
    val format: Format
    val flags: WithFlags
  }

  case class OneConfig(
      format: Format,
      imported: Boolean,
      flags: WithFlags,
      noDelay: Boolean,
      playerFile: Option[String]
  ) extends Config

  case class ByUserConfig(
      user: User,
      vs: Option[User],
      format: Format,
      since: Option[DateTime] = None,
      until: Option[DateTime] = None,
      max: Option[Int] = None,
      rated: Option[Boolean] = None,
      perfType: Set[lila.rating.PerfType],
      analysed: Option[Boolean] = None,
      ongoing: Boolean = false,
      color: Option[shogi.Color],
      flags: WithFlags,
      perSecond: MaxPerSecond,
      playerFile: Option[String]
  ) extends Config {
    def postFilter(g: Game) =
      rated.fold(true)(g.rated ==) && {
        perfType.isEmpty || g.perfType.exists(perfType.contains)
      } && color.fold(true) { c =>
        g.player(c).userId has user.id
      } && analysed.fold(true)(g.metadata.analysed ==)
  }

  case class ByIdsConfig(
      ids: Seq[Game.ID],
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond,
      playerFile: Option[String] = None
  ) extends Config

  case class ByTournamentConfig(
      tournamentId: Tournament.ID,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond
  ) extends Config

}
