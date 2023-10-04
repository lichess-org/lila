package controllers

import play.api.data.*
import play.api.data.Forms.{ list as formList, * }
import scala.util.chaining.*

import lila.api.GameApiV2
import lila.app.{ given, * }
import lila.common.config
import lila.common.Form.{ stringIn, given }
import lila.db.dsl.{ *, given }
import lila.rating.{ Perf, PerfType }

final class GameMod(env: Env)(using akka.stream.Materializer) extends LilaController(env):

  import GameMod.*

  def index(username: UserStr) = SecureBody(_.GamesModView) { ctx ?=> _ ?=>
    Found(env.user.repo byId username): user =>
      val form   = filterForm.bindFromRequest()
      val filter = form.fold(_ => emptyFilter, identity)
      for
        arenas  <- env.tournament.leaderboardApi.recentByUser(user, 1)
        swisses <- env.activity.read.recentSwissRanks(user.id)
        povs    <- fetchGames(user, filter)
        games <-
          if isGranted(_.UserEvaluate)
          then env.mod.assessApi.makeAndGetFullOrBasicsFor(povs) map Right.apply
          else fuccess(Left(povs))
        page <- renderPage(views.html.mod.games(user, form, games, arenas.currentPageResults, swisses))
      yield Ok(page)
  }

  private def fetchGames(user: lila.user.User, filter: Filter) =
    val select = toDbSelect(user, filter) ++ lila.game.Query.finished
    import akka.stream.scaladsl.*
    env.game.gameRepo
      .recentGamesFromSecondaryCursor(select)
      .documentSource(10_000)
      .filter: game =>
        filter.perf.forall(game.perfKey ==)
      .take(filter.nbGames)
      .mapConcat { lila.game.Pov(_, user).toList }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)

  def post(username: UserStr) = SecureBody(_.GamesModView) { ctx ?=> me ?=>
    Found(env.user.repo byId username): user =>
      actionForm
        .bindFromRequest()
        .fold(
          err => BadRequest(err.toString),
          {
            case (gameIds, Some("pgn")) => downloadPgn(user, gameIds)
            case (gameIds, Some("analyse") | None) if isGranted(_.UserEvaluate) =>
              multipleAnalysis(me, gameIds)
            case _ => notFound
          }
        )
  }

  private def multipleAnalysis(me: Me, gameIds: Seq[GameId])(using Context) =
    env.game.gameRepo.unanalysedGames(gameIds).flatMap { games =>
      games.map { game =>
        env.fishnet
          .analyser(
            game,
            lila.fishnet.Work.Sender(
              userId = me,
              ip = ctx.ip.some,
              mod = true,
              system = false
            )
          )
          .void
      }.parallel >> env.fishnet.awaiter(games.map(_.id), 2 minutes)
    } inject NoContent

  private def downloadPgn(user: lila.user.User, gameIds: Seq[GameId]) =
    Ok.chunked {
      env.api.gameApiV2.exportByIds(
        GameApiV2.ByIdsConfig(
          ids = gameIds,
          format = GameApiV2.Format.PGN,
          flags = lila.game.PgnDump.WithFlags(),
          perSecond = config.MaxPerSecond(100),
          playerFile = none
        )
      )
    }.pipe(asAttachmentStream(s"lichess_mod_${user.username}_${gameIds.size}_games.pgn"))
      .as(pgnContentType)

object GameMod:

  case class Filter(
      arena: Option[String],
      swiss: Option[String],
      perf: Option[Perf.Key],
      opponents: Option[String],
      nbGamesOpt: Option[Int]
  ):
    def opponentIds: List[UserId] = UserStr
      .from:
        (~opponents)
          .take(800)
          .replace(",", " ")
          .split(' ')
          .map(_.trim)
      .flatMap(lila.user.User.validateId)
      .toList
      .distinct

    def nbGames = nbGamesOpt | 100

  val emptyFilter = Filter(none, none, none, none, none)

  def toDbSelect(user: lila.user.User, filter: Filter): Bdoc =
    import lila.game.Query
    Query.notSimul ++
      filter.perf.so { perf =>
        Query.clock(perf != PerfType.Correspondence.key)
      } ++ filter.arena.so { id =>
        $doc(lila.game.Game.BSONFields.tournamentId -> id)
      } ++ filter.swiss.so { id =>
        $doc(lila.game.Game.BSONFields.swissId -> id)
      } ++ $and(
        Query.user(user),
        filter.opponentIds.match
          case Nil      => Query.noAnon
          case List(id) => Query.user(id)
          case ids      => Query.users(ids)
      )

  val maxGames = 500

  val filterForm = Form:
    mapping(
      "arena"     -> optional(nonEmptyText),
      "swiss"     -> optional(nonEmptyText),
      "perf"      -> optional(of[Perf.Key]),
      "opponents" -> optional(nonEmptyText),
      "nbGamesOpt" -> optional(
        number(min = 1).transform(
          _.atMost(maxGames),
          identity
        )
      )
    )(Filter.apply)(unapply)

  val actionForm = Form:
    tuple(
      "game"   -> formList(of[GameId]),
      "action" -> optional(stringIn(Set("pgn", "analyse")))
    )
