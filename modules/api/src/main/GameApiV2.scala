package lila.api

import akka.stream.scaladsl._
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._

import chess.format.FEN
import chess.format.pgn.Tag
import lila.analyse.{ JsonView => analysisJson, Analysis }
import lila.common.config.MaxPerSecond
import lila.common.Json.jodaWrites
import lila.common.{ HTTPRequest, LightUser }
import lila.db.dsl._
import lila.game.JsonView._
import lila.game.PgnDump.WithFlags
import lila.game.{ Game, PerfPicker, Query }
import lila.tournament.Tournament
import lila.user.User

final class GameApiV2(
    pgnDump: PgnDump,
    gameRepo: lila.game.GameRepo,
    pairingRepo: lila.tournament.PairingRepo,
    swissApi: lila.swiss.SwissApi,
    analysisRepo: lila.analyse.AnalysisRepo,
    getLightUser: LightUser.Getter
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import GameApiV2._

  private val keepAliveInterval = 70.seconds // play's idleTimeout = 75s

  def exportOne(game: Game, configInput: OneConfig): Fu[String] = {
    val config = configInput.copy(
      flags = configInput.flags.copy(
        delayMoves = (game.playable && !configInput.noDelay) ?? 3,
        evals = configInput.flags.evals && !game.playable
      )
    )
    game.pgnImport ifTrue config.imported match {
      case Some(imported) => fuccess(imported.pgn)
      case None =>
        enrich(config.flags)(game) flatMap {
          case (game, initialFen, analysis) =>
            config.format match {
              case Format.JSON => toJson(game, initialFen, analysis, config.flags) dmap Json.stringify
              case Format.PGN  => pgnDump.toPgnString(game, initialFen, analysis, config.flags)
            }
        }
    }
  }

  private val fileR = """[\s,]""".r
  def filename(game: Game, format: Format): Fu[String] =
    gameLightUsers(game) map {
      case List(wu, bu) =>
        fileR.replaceAllIn(
          "lichess_pgn_%s_%s_vs_%s.%s.%s".format(
            Tag.UTCDate.format.print(game.createdAt),
            pgnDump.dumper.player(game.whitePlayer, wu),
            pgnDump.dumper.player(game.blackPlayer, bu),
            game.id,
            format.toString.toLowerCase
          ),
          "_"
        )
    }
  def filename(tour: Tournament, format: Format): String =
    fileR.replaceAllIn(
      "lichess_tournament_%s_%s_%s.%s".format(
        Tag.UTCDate.format.print(tour.startsAt),
        tour.id,
        lila.common.String.slugify(tour.name),
        format.toString.toLowerCase
      ),
      "_"
    )
  def filename(swiss: lila.swiss.Swiss, format: Format): String =
    fileR.replaceAllIn(
      "lichess_swiss_%s_%s_%s.%s".format(
        Tag.UTCDate.format.print(swiss.startsAt),
        swiss.id,
        lila.common.String.slugify(swiss.name),
        format.toString.toLowerCase
      ),
      "_"
    )

  def exportByUser(config: ByUserConfig): Source[String, _] =
    gameRepo
      .sortedCursor(
        config.vs.fold(Query.user(config.user.id)) { Query.opponents(config.user, _) } ++
          Query.createdBetween(config.since, config.until) ++
          (!config.ongoing).??(Query.finished),
        Query.sortCreated,
        batchSize = config.perSecond.value
      )
      .documentSource()
      .grouped(config.perSecond.value)
      .throttle(1, 1 second)
      .mapConcat(_ filter config.postFilter)
      .take(config.max | Int.MaxValue)
      .via(preparationFlow(config))
      .keepAlive(keepAliveInterval, () => emptyMsgFor(config))

  def exportByIds(config: ByIdsConfig): Source[String, _] =
    gameRepo
      .sortedCursor(
        $inIds(config.ids) ++ Query.finished,
        Query.sortCreated,
        batchSize = config.perSecond.value
      )
      .documentSource()
      .throttle(config.perSecond.value, 1 second)
      .via(preparationFlow(config))

  def exportByTournament(config: ByTournamentConfig): Source[String, _] =
    pairingRepo
      .sortedCursor(
        tournamentId = config.tournamentId,
        batchSize = config.perSecond.value
      )
      .documentSource()
      .grouped(config.perSecond.value)
      .throttle(1, 1 second)
      .mapAsync(1) { pairings =>
        gameRepo.gameOptionsFromSecondary(pairings.map(_.gameId)) map {
          _.zip(pairings) collect {
            case (Some(game), pairing) => game -> pairing
          }
        }
      }
      .mapConcat(identity)
      .mapAsync(4) {
        case (game, pairing) => enrich(config.flags)(game) dmap { _ -> pairing }
      }
      .mapAsync(4) {
        case ((game, fen, analysis), pairing) =>
          config.format match {
            case Format.PGN => pgnDump.formatter(config.flags)(game, fen, analysis)
            case Format.JSON =>
              def addBerserk(color: chess.Color)(json: JsObject) =
                if (pairing berserkOf color)
                  json deepMerge Json.obj("players" -> Json.obj(color.name -> Json.obj("berserk" -> true)))
                else json
              toJson(game, fen, analysis, config.flags) dmap
                addBerserk(chess.White) dmap
                addBerserk(chess.Black) dmap { json =>
                s"${Json.stringify(json)}\n"
              }
          }
      }

  def exportBySwiss(config: BySwissConfig): Source[String, _] =
    swissApi
      .gameIdSource(
        swissId = config.swissId,
        batchSize = config.perSecond.value
      )
      .grouped(config.perSecond.value)
      .throttle(1, 1 second)
      .mapAsync(1)(gameRepo.gamesFromSecondary)
      .mapConcat(identity)
      .mapAsync(4)(enrich(config.flags))
      .mapAsync(4) {
        case (game, fen, analysis) =>
          config.format match {
            case Format.PGN => pgnDump.formatter(config.flags)(game, fen, analysis)
            case Format.JSON =>
              toJson(game, fen, analysis, config.flags) dmap { json =>
                s"${Json.stringify(json)}\n"
              }
          }
      }

  private def preparationFlow(config: Config) =
    Flow[Game]
      .mapAsync(4)(enrich(config.flags))
      .mapAsync(4)(formatterFor(config).tupled)

  private def enrich(flags: WithFlags)(game: Game) =
    gameRepo initialFen game flatMap { initialFen =>
      (flags.evals ?? analysisRepo.byGame(game)) dmap {
        (game, initialFen, _)
      }
    }

  private def formatterFor(config: Config) =
    config.format match {
      case Format.PGN  => pgnDump.formatter(config.flags)
      case Format.JSON => jsonFormatter(config.flags)
    }

  private def emptyMsgFor(config: Config) =
    config.format match {
      case Format.PGN  => "\n"
      case Format.JSON => "{}\n"
    }

  private def jsonFormatter(flags: WithFlags) =
    (game: Game, initialFen: Option[FEN], analysis: Option[Analysis]) =>
      toJson(game, initialFen, analysis, flags) dmap { json =>
        s"${Json.stringify(json)}\n"
      }

  private def toJson(
      g: Game,
      initialFen: Option[FEN],
      analysisOption: Option[Analysis],
      withFlags: WithFlags
  ): Fu[JsObject] =
    for {
      lightUsers <- gameLightUsers(g)
      pgn <-
        withFlags.pgnInJson ?? pgnDump
          .toPgnString(g, initialFen, analysisOption, withFlags)
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
        "players" -> JsObject(g.players zip lightUsers map {
          case (p, user) =>
            p.color.name -> Json
              .obj()
              .add("user", user)
              .add("rating", p.rating)
              .add("ratingDiff", p.ratingDiff)
              .add("name", p.name)
              .add("provisional" -> p.provisional)
              .add("aiLevel" -> p.aiLevel)
              .add("analysis" -> analysisOption.flatMap(analysisJson.player(g pov p.color)))
          // .add("moveCentis" -> withFlags.moveTimes ?? g.moveTimes(p.color).map(_.map(_.centis)))
        })
      )
      .add("initialFen" -> initialFen.map(_.value))
      .add("winner" -> g.winnerColor.map(_.name))
      .add("opening" -> g.opening.ifTrue(withFlags.opening))
      .add("moves" -> withFlags.moves.option(g.pgnMoves mkString " "))
      .add("pgn" -> pgn)
      .add("daysPerTurn" -> g.daysPerTurn)
      .add("analysis" -> analysisOption.ifTrue(withFlags.evals).map(analysisJson.moves(_, withGlyph = false)))
      .add("tournament" -> g.tournamentId)
      .add("clock" -> g.clock.map { clock =>
        Json.obj(
          "initial"   -> clock.limitSeconds,
          "increment" -> clock.incrementSeconds,
          "totalTime" -> clock.estimateTotalSeconds
        )
      })

  private def gameLightUsers(game: Game): Fu[List[Option[LightUser]]] =
    (game.whitePlayer.userId ?? getLightUser) zip (game.blackPlayer.userId ?? getLightUser) map {
      case (wu, bu) => List(wu, bu)
    }
}

object GameApiV2 {

  sealed trait Format
  object Format {
    case object PGN  extends Format
    case object JSON extends Format
    def byRequest(req: play.api.mvc.RequestHeader) = if (HTTPRequest acceptsNdJson req) JSON else PGN
  }

  sealed trait Config {
    val format: Format
    val flags: WithFlags
  }

  case class OneConfig(
      format: Format,
      imported: Boolean,
      flags: WithFlags,
      noDelay: Boolean
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
      color: Option[chess.Color],
      flags: WithFlags,
      perSecond: MaxPerSecond
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
      perSecond: MaxPerSecond
  ) extends Config

  case class ByTournamentConfig(
      tournamentId: Tournament.ID,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond
  ) extends Config

  case class BySwissConfig(
      swissId: lila.swiss.Swiss.Id,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond
  ) extends Config
}
