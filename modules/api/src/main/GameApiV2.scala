package lila.api

import akka.stream.scaladsl.*
import chess.format.Fen
import chess.format.pgn.{ Tag, PgnStr }
import play.api.libs.json.*

import lila.analyse.{ Analysis, AccuracyPercent, JsonView as analysisJson }
import lila.common.config.MaxPerSecond
import lila.common.Json.given
import lila.common.{ HTTPRequest, LightUser }
import lila.db.dsl.{ *, given }
import lila.game.JsonView.given
import lila.game.PgnDump.WithFlags
import lila.game.{ Game, Query }
import lila.team.GameTeams
import lila.tournament.Tournament
import lila.user.User
import lila.round.GameProxyRepo
import chess.ByColor

final class GameApiV2(
    pgnDump: PgnDump,
    gameRepo: lila.game.GameRepo,
    gameJsonView: lila.game.JsonView,
    pairingRepo: lila.tournament.PairingRepo,
    playerRepo: lila.tournament.PlayerRepo,
    swissApi: lila.swiss.SwissApi,
    analysisRepo: lila.analyse.AnalysisRepo,
    annotator: lila.analyse.Annotator,
    getLightUser: LightUser.Getter,
    realPlayerApi: RealPlayerApi,
    gameProxy: GameProxyRepo
)(using Executor, akka.actor.ActorSystem):

  import GameApiV2.*

  private val keepAliveInterval = 70.seconds // play's idleTimeout = 75s

  def exportOne(game: Game, config: OneConfig): Fu[String] =
    game.pgnImport ifTrue config.imported match
      case Some(imported) => fuccess(imported.pgn.value)
      case None =>
        for
          realPlayers                  <- config.playerFile.so(realPlayerApi.apply)
          (game, initialFen, analysis) <- enrich(config.flags)(game)
          formatted <- config.format match
            case Format.JSON =>
              toJson(game, initialFen, analysis, config.flags, realPlayers = realPlayers) dmap Json.stringify
            case Format.PGN =>
              PgnStr raw pgnDump(
                game,
                initialFen,
                analysis,
                config.flags,
                realPlayers = realPlayers
              ).dmap(annotator.toPgnString)
        yield formatted

  private val fileR = """[\s,]""".r

  def filename(game: Game, format: Format): Fu[String] =
    gameLightUsers(game).map: users =>
      fileR.replaceAllIn(
        "lichess_pgn_%s_%s_vs_%s.%s.%s".format(
          Tag.UTCDate.format.print(game.createdAt),
          pgnDump.dumper.player.tupled(users.white),
          pgnDump.dumper.player.tupled(users.black),
          game.id,
          format.toString.toLowerCase
        ),
        "_"
      )

  def filename(tour: Tournament, format: Format): String =
    filename(tour, format.toString.toLowerCase)

  def filename(tour: Tournament, format: String): String =
    fileR.replaceAllIn(
      "lichess_tournament_%s_%s_%s.%s".format(
        Tag.UTCDate.format.print(tour.startsAt),
        tour.id,
        lila.common.String.slugify(tour.name),
        format
      ),
      "_"
    )

  def filename(swiss: lila.swiss.Swiss, format: Format): String =
    filename(swiss, format.toString.toLowerCase)

  def filename(swiss: lila.swiss.Swiss, format: String): String =
    fileR.replaceAllIn(
      "lichess_swiss_%s_%s_%s.%s".format(
        Tag.UTCDate.format.print(swiss.startsAt),
        swiss.id,
        lila.common.String.slugify(swiss.name),
        format
      ),
      "_"
    )

  def exportByUser(config: ByUserConfig): Source[String, ?] =
    Source.futureSource:
      config.playerFile.so(realPlayerApi.apply) map { realPlayers =>
        val playerSelect =
          if config.finished then
            config.vs.fold(Query.user(config.user.id)) { Query.opponents(config.user, _) }
          else
            config.vs.map(_.id).fold(Query.nowPlaying(config.user.id)) {
              Query.nowPlayingVs(config.user.id, _)
            }
        gameRepo
          .sortedCursor(
            playerSelect ++
              Query.createdBetween(config.since, config.until) ++ (!config.ongoing so Query.finished),
            config.sort.bson,
            batchSize = config.perSecond.value
          )
          .documentSource()
          .map(g => config.postFilter(g) option g)
          .throttle(config.perSecond.value * 10, 1 second, e => if e.isDefined then 10 else 2)
          .mapConcat(_.toList)
          .take(config.max | Int.MaxValue)
          .via(upgradeOngoingGame)
          .via(preparationFlow(config, realPlayers))
          .keepAlive(keepAliveInterval, () => emptyMsgFor(config))
      }

  def exportByIds(config: ByIdsConfig): Source[String, ?] =
    Source.futureSource:
      config.playerFile.so(realPlayerApi.apply) map { realPlayers =>
        gameRepo
          .sortedCursor(
            $inIds(config.ids),
            Query.sortCreated,
            batchSize = config.perSecond.value
          )
          .documentSource()
          .throttle(config.perSecond.value, 1 second)
          .via(upgradeOngoingGame)
          .via(preparationFlow(config, realPlayers))
      }

  def exportByTournament(config: ByTournamentConfig, onlyUserId: Option[UserId]): Source[String, ?] =
    pairingRepo
      .sortedCursor(
        tournamentId = config.tour.id,
        userId = onlyUserId,
        batchSize = config.perSecond.value
      )
      .documentSource()
      .grouped(30)
      .mapAsync(1): pairings =>
        config.tour.isTeamBattle.so {
          playerRepo.teamsOfPlayers(config.tour.id, pairings.flatMap(_.users).distinct).dmap(_.toMap)
        } flatMap { playerTeams =>
          gameRepo.gameOptionsFromSecondary(pairings.map(_.gameId)) map {
            _.zip(pairings) collect { case (Some(game), pairing) =>
              (
                game,
                pairing,
                ByColor(pairing.user1, pairing.user2).traverse(playerTeams.get)
              )
            }
          }
        }
      .mapConcat(identity)
      .throttle(config.perSecond.value, 1 second)
      .mapAsync(4): (game, pairing, teams) =>
        enrich(config.flags)(game) dmap { (_, pairing, teams) }
      .mapAsync(4) { case ((game, fen, analysis), pairing, teams) =>
        config.format match
          case Format.PGN => pgnDump.formatter(config.flags)(game, fen, analysis, teams, none)
          case Format.JSON =>
            def addBerserk(color: chess.Color)(json: JsObject) =
              if pairing berserkOf color then
                json deepMerge Json.obj(
                  "players" -> Json.obj(color.name -> Json.obj("berserk" -> true))
                )
              else json
            toJson(game, fen, analysis, config.flags, teams) dmap
              addBerserk(chess.White) dmap
              addBerserk(chess.Black) dmap { json =>
                s"${Json.stringify(json)}\n"
              }
      }

  def exportBySwiss(config: BySwissConfig): Source[String, ?] =
    swissApi
      .gameIdSource(
        swissId = config.swissId,
        player = config.player,
        batchSize = config.perSecond.value
      )
      .grouped(30)
      .mapAsync(1)(gameRepo.gamesTemporarilyFromPrimary)
      .mapConcat(identity)
      .throttle(config.perSecond.value, 1 second)
      .mapAsync(4)(enrich(config.flags))
      .mapAsync(4): (game, fen, analysis) =>
        config.format match
          case Format.PGN => pgnDump.formatter(config.flags)(game, fen, analysis, none, none)
          case Format.JSON =>
            toJson(game, fen, analysis, config.flags, None).dmap: json =>
              s"${Json.stringify(json)}\n"

  def exportUserImportedGames(user: User): Source[PgnStr, ?] =
    gameRepo
      .sortedCursor(Query imported user.id, Query.importedSort, batchSize = 20)
      .documentSource()
      .throttle(20, 1 second)
      .mapConcat(_.pgnImport.map(_.pgn.map(_ + "\n\n\n")).toList)

  private val upgradeOngoingGame =
    Flow[Game].mapAsync(4)(gameProxy.upgradeIfPresent)

  private def preparationFlow(config: Config, realPlayers: Option[RealPlayers]) =
    Flow[Game]
      .mapAsync(4)(enrich(config.flags))
      .mapAsync(4): (game, fen, analysis) =>
        formatterFor(config)(game, fen, analysis, None, realPlayers)

  private def enrich(flags: WithFlags)(game: Game) =
    gameRepo initialFen game flatMap { initialFen =>
      (flags.requiresAnalysis so analysisRepo.byGame(game)) dmap {
        (game, initialFen, _)
      }
    }

  private def formatterFor(config: Config) =
    config.format match
      case Format.PGN  => pgnDump.formatter(config.flags)
      case Format.JSON => jsonFormatter(config.flags)

  private def emptyMsgFor(config: Config) =
    config.format match
      case Format.PGN  => "\n"
      case Format.JSON => "{}\n"

  private def jsonFormatter(flags: WithFlags) =
    (
        game: Game,
        initialFen: Option[Fen.Epd],
        analysis: Option[Analysis],
        teams: Option[GameTeams],
        realPlayers: Option[RealPlayers]
    ) =>
      toJson(game, initialFen, analysis, flags, teams, realPlayers).dmap: json =>
        s"${Json.stringify(json)}\n"

  private def toJson(
      g: Game,
      initialFen: Option[Fen.Epd],
      analysisOption: Option[Analysis],
      withFlags: WithFlags,
      teams: Option[GameTeams] = None,
      realPlayers: Option[RealPlayers] = None
  ): Fu[JsObject] = for
    lightUsers <- gameLightUsers(g)
    pgn <-
      withFlags.pgnInJson soFu pgnDump
        .apply(g, initialFen, analysisOption, withFlags, realPlayers = realPlayers)
        .dmap(annotator.toPgnString)
    accuracy = analysisOption.ifTrue(withFlags.accuracy).flatMap {
      AccuracyPercent.gameAccuracy(g.startedAtPly.turn, _)
    }
  yield Json
    .obj(
      "id"         -> g.id,
      "rated"      -> g.rated,
      "variant"    -> g.variant.key,
      "speed"      -> g.speed.key,
      "perf"       -> g.perfKey,
      "createdAt"  -> g.createdAt,
      "lastMoveAt" -> g.movedAt,
      "status"     -> g.status.name,
      "players" -> JsObject(lightUsers.mapList: (p, user) =>
        p.color.name -> gameJsonView
          .player(p, user)
          .add(
            "analysis" -> analysisOption.flatMap(
              analysisJson.player(g.pov(p.color).sideAndStart)(_, accuracy)
            )
          )
          .add("team" -> teams.map(_(p.color))))
    )
    .add("initialFen" -> initialFen)
    .add("winner" -> g.winnerColor.map(_.name))
    .add("opening" -> g.opening.ifTrue(withFlags.opening))
    .add("moves" -> withFlags.moves.option {
      withFlags keepDelayIf g.playable applyDelay g.sans mkString " "
    })
    .add("clocks" -> withFlags.clocks.so(g.bothClockStates).map { clocks =>
      withFlags keepDelayIf g.playable applyDelay clocks
    })
    .add("pgn" -> pgn)
    .add("daysPerTurn" -> g.daysPerTurn)
    .add("analysis" -> analysisOption.ifTrue(withFlags.evals).map(analysisJson.moves(_, withGlyph = false)))
    .add("tournament" -> g.tournamentId)
    .add("swiss" -> g.swissId)
    .add("clock" -> g.clock.map: clock =>
      Json.obj(
        "initial"   -> clock.limitSeconds,
        "increment" -> clock.incrementSeconds,
        "totalTime" -> clock.estimateTotalSeconds
      ))
    .add("lastFen" -> withFlags.lastFen.option(Fen.write(g.chess.situation)))

  private def gameLightUsers(game: Game): Future[ByColor[(lila.game.Player, Option[LightUser])]] =
    game.players.traverse(_.userId so getLightUser).dmap(game.players.zip(_))

object GameApiV2:

  enum Format:
    case PGN, JSON
  object Format:
    def byRequest(req: play.api.mvc.RequestHeader) = if HTTPRequest acceptsNdJson req then JSON else PGN

  sealed trait Config:
    val format: Format
    val flags: WithFlags

  enum GameSort(val bson: Bdoc):
    case DateAsc  extends GameSort(Query.sortChronological)
    case DateDesc extends GameSort(Query.sortAntiChronological)

  case class OneConfig(
      format: Format,
      imported: Boolean,
      flags: WithFlags,
      playerFile: Option[String]
  ) extends Config

  case class ByUserConfig(
      user: User,
      vs: Option[User],
      format: Format,
      since: Option[Instant] = None,
      until: Option[Instant] = None,
      max: Option[Int] = None,
      rated: Option[Boolean] = None,
      perfType: Set[lila.rating.PerfType],
      analysed: Option[Boolean] = None,
      color: Option[chess.Color],
      flags: WithFlags,
      sort: GameSort,
      perSecond: MaxPerSecond,
      playerFile: Option[String],
      ongoing: Boolean = false,
      finished: Boolean = true
  ) extends Config:
    def postFilter(g: Game) =
      rated.forall(g.rated ==) && {
        perfType.isEmpty || perfType.contains(g.perfType)
      } && color.forall { c =>
        g.player(c).userId has user.id
      } && analysed.forall(g.metadata.analysed ==)

  case class ByIdsConfig(
      ids: Seq[GameId],
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond,
      playerFile: Option[String] = None
  ) extends Config

  case class ByTournamentConfig(
      tour: Tournament,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond
  ) extends Config

  case class BySwissConfig(
      swissId: SwissId,
      format: Format,
      flags: WithFlags,
      perSecond: MaxPerSecond,
      player: Option[UserId]
  ) extends Config
