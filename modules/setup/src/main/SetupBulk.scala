package lila.setup

import akka.stream.scaladsl._
import chess.format.FEN
import chess.variant.Variant
import chess.{ Clock, Mode, Speed }
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import scala.concurrent.duration._

import lila.game.Game
import lila.game.IdGenerator
import lila.oauth.AccessToken
import lila.oauth.OAuthScope
import lila.oauth.OAuthServer
import lila.user.User
import lila.common.Template

object SetupBulk {

  val maxGames = 500

  case class BulkFormData(
      tokens: String,
      variant: Variant,
      clock: Clock.Config,
      rated: Boolean,
      pairAt: Option[DateTime],
      startClocksAt: Option[DateTime],
      message: Option[Template]
  )

  private def timestampInNearFuture = longNumber(
    min = 0,
    max = DateTime.now.plusDays(1).getMillis
  )

  def form = Form[BulkFormData](
    mapping(
      "players" -> nonEmptyText
        .verifying("Not enough tokens", t => extractTokenPairs(t).nonEmpty)
        .verifying(s"Too many tokens (max: ${maxGames * 2})", t => extractTokenPairs(t).sizeIs < maxGames)
        .verifying(
          "Tokens must be unique",
          t => {
            val tokens = extractTokenPairs(t).view.flatMap { case (w, b) => Vector(w, b) }.toVector
            tokens.size == tokens.distinct.size
          }
        ),
      SetupForm.api.variant,
      "clock"         -> SetupForm.api.clockMapping,
      "rated"         -> boolean,
      "pairAt"        -> optional(timestampInNearFuture),
      "startClocksAt" -> optional(timestampInNearFuture),
      "message"       -> SetupForm.api.message
    ) {
      (
          tokens: String,
          variant: Option[String],
          clock: Clock.Config,
          rated: Boolean,
          pairTs: Option[Long],
          clockTs: Option[Long],
          message: Option[String]
      ) =>
        BulkFormData(
          tokens,
          Variant orDefault ~variant,
          clock,
          rated,
          pairTs.map { new DateTime(_) },
          clockTs.map { new DateTime(_) },
          message map Template
        )
    }(_ => None)
  )

  private[setup] def extractTokenPairs(str: String): List[(AccessToken.Id, AccessToken.Id)] =
    str
      .split(',')
      .view
      .map(_ split ":")
      .collect { case Array(w, b) =>
        w.trim -> b.trim
      }
      .collect {
        case (w, b) if w.nonEmpty && b.nonEmpty => (AccessToken.Id(w), AccessToken.Id(b))
      }
      .toList

  case class BadToken(token: AccessToken.Id, error: OAuthServer.AuthError)

  case class ScheduledGame(id: Game.ID, white: User.ID, black: User.ID)

  case class ScheduledBulk(
      _id: String,
      by: User.ID,
      games: List[ScheduledGame],
      variant: Variant,
      clock: Clock.Config,
      mode: Mode,
      pairAt: DateTime,
      startClocksAt: Option[DateTime],
      scheduledAt: DateTime,
      message: Option[Template],
      pairedAt: Option[DateTime] = None
  ) {
    def userSet = Set(games.flatMap(g => List(g.white, g.black)))
    def collidesWith(other: ScheduledBulk) = {
      pairAt == other.pairAt || startClocksAt == startClocksAt
    } && userSet.exists(other.userSet.contains)
  }

  sealed trait ScheduleError
  case class BadTokens(tokens: List[BadToken])    extends ScheduleError
  case class DuplicateUsers(users: List[User.ID]) extends ScheduleError
  case object RateLimited                         extends ScheduleError

  def toJson(bulk: ScheduledBulk) = {
    import bulk._
    import lila.common.Json.jodaWrites
    Json.obj(
      "id" -> _id,
      "games" -> games.map { g =>
        Json.obj(
          "id"    -> g.id,
          "white" -> g.white,
          "black" -> g.black
        )
      },
      "variant" -> variant.key,
      "clock" -> Json.obj(
        "limit"     -> clock.limitSeconds,
        "increment" -> clock.incrementSeconds
      ),
      "rated"         -> mode.rated,
      "pairAt"        -> pairAt,
      "startClocksAt" -> startClocksAt,
      "scheduledAt"   -> scheduledAt,
      "pairedAt"      -> pairedAt
    )
  }

}

final class SetupBulkApi(oauthServer: OAuthServer, idGenerator: IdGenerator)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import SetupBulk._

  type Result = Either[ScheduleError, ScheduledBulk]

  private val rateLimit = new lila.memo.RateLimit[User.ID](
    credits = maxGames,
    duration = 10.minutes,
    key = "challenge.bulk"
  )

  def apply(data: BulkFormData, me: User): Fu[Result] =
    Source(extractTokenPairs(data.tokens))
      .mapConcat { case (whiteToken, blackToken) =>
        List(whiteToken, blackToken) // flatten now, re-pair later!
      }
      .mapAsync(8) { token =>
        oauthServer.auth(token, List(OAuthScope.Challenge.Write)) map {
          _.left.map { BadToken(token, _) }
        }
      }
      .runFold[Either[List[BadToken], List[User.ID]]](Right(Nil)) {
        case (Left(bads), Left(bad))       => Left(bad :: bads)
        case (Left(bads), _)               => Left(bads)
        case (Right(_), Left(bad))         => Left(bad :: Nil)
        case (Right(users), Right(scoped)) => Right(scoped.user.id :: users)
      }
      .flatMap {
        case Left(errors) => fuccess(Left(BadTokens(errors.reverse)))
        case Right(allPlayers) =>
          val dups = allPlayers
            .groupBy(identity)
            .view
            .mapValues(_.size)
            .collect {
              case (u, nb) if nb > 1 => u
            }
            .toList
          if (dups.nonEmpty) fuccess(Left(DuplicateUsers(dups)))
          else {
            val pairs = allPlayers.reverse
              .grouped(2)
              .collect { case List(w, b) => (w, b) }
              .toList
            val nbGames = pairs.size
            rateLimit[Fu[Result]](me.id, cost = nbGames) {
              lila.mon.api.challenge.bulk.scheduleNb(me.id).increment(nbGames).unit
              idGenerator
                .games(nbGames)
                .map {
                  _.toList zip pairs
                }
                .map {
                  _.map { case (id, (w, b)) =>
                    ScheduledGame(id, w, b)
                  }
                }
                .dmap {
                  ScheduledBulk(
                    _id = lila.common.ThreadLocalRandom nextString 8,
                    by = me.id,
                    _,
                    data.variant,
                    data.clock,
                    Mode(data.rated),
                    pairAt = data.pairAt | DateTime.now,
                    startClocksAt = data.startClocksAt,
                    message = data.message,
                    scheduledAt = DateTime.now
                  )
                }
                .dmap(Right.apply)
            }(fuccess(Left(RateLimited)))
          }
      }
}
