package controllers

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._
import scala.util.chaining._

import lila.api.Context
import lila.api.GameApiV2
import lila.app._
import lila.common.config
import lila.common.HTTPRequest
import lila.db.dsl._
import lila.user.Holder

final class GameMod(env: Env)(implicit mat: akka.stream.Materializer) extends LilaController(env) {

  import GameMod._

  def index(username: String) =
    SecureBody(_.Hunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        implicit def req = ctx.body
        val form         = filterForm.bindFromRequest()
        val filter       = form.fold(_ => emptyFilter, identity)
        env.tournament.leaderboardApi.recentByUser(user, 1) zip
          env.activity.read.recentSwissRanks(user.id) zip
          fetchGames(user, filter) flatMap { case ((arenas, swisses), povs) =>
            env.mod.assessApi.makeAndGetFullOrBasicsFor(povs) map { games =>
              Ok(views.html.mod.games(user, form, games, arenas.currentPageResults, swisses))
            }
          }
      }
    }

  private def fetchGames(user: lila.user.User, filter: Filter) = {
    val select = toDbSelect(filter) ++ lila.game.Query.finished
    filter.realSpeed
      .fold(env.game.gameRepo.recentPovsByUserFromSecondary(user, nbGames, select)) { speed =>
        import akka.stream.scaladsl._
        env.game.gameRepo
          .recentGamesByUserFromSecondaryCursor(user, select)
          .documentSource(10_000)
          .filter(_.speed == speed)
          .take(nbGames)
          .mapConcat { g => lila.game.Pov(g, user).toList }
          .toMat(Sink.seq)(Keep.right)
          .run()
          .map(_.toList)
      }
  }

  def post(username: String) =
    SecureBody(_.Hunter) { implicit ctx => me =>
      OptionFuResult(env.user.repo named username) { user =>
        implicit val body = ctx.body
        actionForm
          .bindFromRequest()
          .fold(
            err => BadRequest(err.toString).fuccess,
            {
              case (gameIds, Some("pgn"))            => downloadPgn(user, gameIds).fuccess
              case (gameIds, Some("analyse") | None) => multipleAnalysis(me, gameIds)
              case _                                 => notFound
            }
          )
      }
    }

  private def multipleAnalysis(me: Holder, gameIds: Seq[lila.game.Game.ID])(implicit ctx: Context) =
    env.game.gameRepo.unanalysedGames(gameIds).flatMap { games =>
      games.map { game =>
        env.fishnet.analyser(
          game,
          lila.fishnet.Work.Sender(
            userId = me.id,
            ip = HTTPRequest.ipAddress(ctx.req).some,
            mod = true,
            system = false
          )
        )
      }.sequenceFu >> env.fishnet.awaiter(games.map(_.id), 2 minutes)
    } inject NoContent

  private def downloadPgn(user: lila.user.User, gameIds: Seq[lila.game.Game.ID]) =
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

  private def guessSwisses(user: lila.user.User): Fu[Seq[lila.swiss.Swiss]] = fuccess(Nil)
}

object GameMod {

  val nbGames = 100

  case class Filter(
      arena: Option[String],
      swiss: Option[String],
      speed: Option[String],
      opponents: Option[String]
  ) {
    def opponentIds: List[lila.user.User.ID] =
      (~opponents)
        .take(800)
        .replace(",", " ")
        .split(' ')
        .view
        .flatMap(_.trim.some.filter(_.nonEmpty))
        .filter(lila.user.User.couldBeUsername)
        .map(lila.user.User.normalize)
        .toList
        .distinct
    def realSpeed = speed.flatMap(k => chess.Speed.all.find(_.key == k))
  }

  val emptyFilter = Filter(none, none, none, none)

  def toDbSelect(filter: Filter): Bdoc =
    lila.game.Query.notSimul ++ lila.game.Query.clock(
      filter.realSpeed.exists(chess.Speed.Correspondence !=)
    ) ++ filter.arena.?? { id =>
      $doc(lila.game.Game.BSONFields.tournamentId -> id)
    } ++ filter.swiss.?? { id =>
      $doc(lila.game.Game.BSONFields.swissId -> id)
    } ++ (filter.opponentIds match {
      case Nil      => $empty
      case List(id) => $and(lila.game.Game.BSONFields.playerUids $eq id)
      case ids      => $and(lila.game.Game.BSONFields.playerUids $in ids)
    })

  val filterForm =
    Form(
      mapping(
        "arena"     -> optional(nonEmptyText),
        "swiss"     -> optional(nonEmptyText),
        "speed"     -> optional(nonEmptyText),
        "opponents" -> optional(nonEmptyText)
      )(Filter.apply)(Filter.unapply _)
    )

  val actionForm =
    Form(
      tuple(
        "game"   -> list(nonEmptyText),
        "action" -> optional(lila.common.Form.stringIn(Set("pgn", "analyse")))
      )
    )
}
