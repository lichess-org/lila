package lila.setup

import akka.stream.scaladsl.*
import chess.variant.{ FromPosition, Variant }
import chess.format.Fen
import chess.{ Clock, Mode, ByColor }
import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.Json
import ornicar.scalalib.ThreadLocalRandom

import lila.common.{ Bearer, Days, Template }
import lila.game.{ GameRule, IdGenerator }
import lila.oauth.{ OAuthScope, OAuthServer, EndpointScopes }
import lila.user.User
import lila.rating.PerfType

object SetupBulk:

  val maxGames = 500
  val maxBulks = 20

  case class BulkFormData(
      tokens: String,
      variant: Variant,
      clock: Option[Clock.Config],
      days: Option[Days],
      rated: Boolean,
      pairAt: Option[Instant],
      startClocksAt: Option[Instant],
      message: Option[Template],
      rules: Set[GameRule],
      fen: Option[Fen.Epd] = None
  ):
    def clockOrDays = clock.toLeft(days | Days(3))

    def allowMultiplePairingsPerUser = clock.isEmpty

    def validFen = ApiConfig.validFen(variant, fen)

    def autoVariant =
      if variant.standard && fen.exists(!_.isInitial) then copy(variant = FromPosition)
      else this

  private def timestampInNearFuture = longNumber(
    min = 0,
    max = nowInstant.plusDays(7).toMillis
  )

  def form = Form[BulkFormData](
    mapping(
      "players" -> nonEmptyText
        .verifying("Not enough tokens", t => extractTokenPairs(t).nonEmpty)
        .verifying(s"Too many tokens (max: ${maxGames * 2})", t => extractTokenPairs(t).sizeIs < maxGames),
      SetupForm.api.variant,
      SetupForm.api.clock,
      SetupForm.api.optionalDays,
      "fen"           -> Mappings.fenField,
      "rated"         -> boolean,
      "pairAt"        -> optional(timestampInNearFuture),
      "startClocksAt" -> optional(timestampInNearFuture),
      "message"       -> SetupForm.api.message,
      "rules"         -> optional(Mappings.gameRules)
    ) {
      (
          tokens: String,
          variant: Option[Variant.LilaKey],
          clock: Option[Clock.Config],
          days: Option[Days],
          fen: Option[Fen.Epd],
          rated: Boolean,
          pairTs: Option[Long],
          clockTs: Option[Long],
          message: Option[String],
          rules: Option[Set[GameRule]]
      ) =>
        BulkFormData(
          tokens,
          Variant.orDefault(variant),
          clock,
          days,
          rated,
          pairTs map millisToInstant,
          clockTs map millisToInstant,
          message map Template.apply,
          ~rules,
          fen
        ).autoVariant
    }(_ => None)
      .verifying(
        "clock or correspondence days required",
        c => c.clock.isDefined || c.days.isDefined
      )
      .verifying("invalidFen", _.validFen)
      .verifying(
        "Tokens must be unique for real-time games (not correspondence)",
        data =>
          data.allowMultiplePairingsPerUser || {
            val tokens = extractTokenPairs(data.tokens).view.flatMap { case (w, b) => Vector(w, b) }.toVector
            tokens.size == tokens.distinct.size
          }
      )
  )

  private[setup] def extractTokenPairs(str: String): List[(Bearer, Bearer)] =
    str
      .split(',')
      .view
      .map(_ split ":")
      .collect:
        case Array(w, b) =>
          w.trim -> b.trim
      .collect:
        case (w, b) if w.nonEmpty && b.nonEmpty => (Bearer(w), Bearer(b))
      .toList

  case class BadToken(token: Bearer, error: OAuthServer.AuthError)

  case class ScheduledGame(id: GameId, white: UserId, black: UserId):
    def userIds = ByColor(white, black)

  case class ScheduledBulk(
      _id: String,
      by: UserId,
      games: List[ScheduledGame],
      variant: Variant,
      clock: Either[Clock.Config, Days],
      mode: Mode,
      pairAt: Instant,
      startClocksAt: Option[Instant],
      scheduledAt: Instant,
      message: Option[Template],
      rules: Set[GameRule] = Set.empty,
      pairedAt: Option[Instant] = None,
      fen: Option[Fen.Epd] = None
  ):
    def userSet = Set(games.flatMap(g => List(g.white, g.black)))
    def collidesWith(other: ScheduledBulk) = {
      pairAt == other.pairAt || startClocksAt.exists(other.startClocksAt.contains)
    } && userSet.exists(other.userSet.contains)
    def nonEmptyRules = rules.nonEmpty option rules
    def perfType      = PerfType(variant, chess.Speed(clock.left.toOption))

  enum ScheduleError:
    case BadTokens(tokens: List[BadToken])
    case DuplicateUsers(users: List[UserId])
    case RateLimited

  def toJson(bulk: ScheduledBulk) =
    import bulk.*
    import lila.common.Json.given
    import lila.game.JsonView.given
    Json
      .obj(
        "id" -> _id,
        "games" -> games.map: g =>
          Json.obj(
            "id"    -> g.id,
            "white" -> g.white,
            "black" -> g.black
          ),
        "variant"       -> variant.key,
        "rated"         -> mode.rated,
        "pairAt"        -> pairAt,
        "startClocksAt" -> startClocksAt,
        "scheduledAt"   -> scheduledAt,
        "pairedAt"      -> pairedAt
      )
      .add("clock" -> bulk.clock.left.toOption.map: c =>
        Json.obj(
          "limit"     -> c.limitSeconds,
          "increment" -> c.incrementSeconds
        ))
      .add("correspondence" -> bulk.clock.toOption.map: days =>
        Json.obj("daysPerTurn" -> days))
      .add("message" -> message.map(_.value))
      .add("rules" -> nonEmptyRules)
      .add("fen" -> fen)

final class SetupBulkApi(oauthServer: OAuthServer, idGenerator: IdGenerator)(using
    Executor,
    akka.stream.Materializer
):

  import SetupBulk.*

  type Result = Either[ScheduleError, ScheduledBulk]

  private val rateLimit = lila.memo.RateLimit[UserId](
    credits = maxGames * 3,
    duration = 10.minutes,
    key = "challenge.bulk"
  )

  def apply(data: BulkFormData, me: User): Fu[Result] =
    Source(extractTokenPairs(data.tokens))
      .mapConcat: (whiteToken, blackToken) =>
        List(whiteToken, blackToken) // flatten now, re-pair later!
      .mapAsync(8): token =>
        oauthServer.auth(token, OAuthScope.select(_.Challenge.Write) into EndpointScopes, none) map {
          _.left.map { BadToken(token, _) }
        }
      .runFold[Either[List[BadToken], List[UserId]]](Right(Nil)):
        case (Left(bads), Left(bad))       => Left(bad :: bads)
        case (Left(bads), _)               => Left(bads)
        case (Right(_), Left(bad))         => Left(bad :: Nil)
        case (Right(users), Right(scoped)) => Right(scoped.me :: users)
      .flatMap:
        case Left(errors) => fuccess(Left(ScheduleError.BadTokens(errors.reverse)))
        case Right(allPlayers) =>
          lazy val dups = allPlayers
            .groupBy(identity)
            .view
            .mapValues(_.size)
            .collect:
              case (u, nb) if nb > 1 => u
            .toList
          if !data.allowMultiplePairingsPerUser && dups.nonEmpty
          then fuccess(Left(ScheduleError.DuplicateUsers(dups)))
          else
            val pairs = allPlayers.reverse
              .grouped(2)
              .collect { case List(w, b) => (w, b) }
              .toList
            val nbGames = pairs.size
            val cost    = nbGames * (if me.isVerified || me.isApiHog then 1 else 3)
            rateLimit(me.id, fuccess(Left(ScheduleError.RateLimited)), cost = cost):
              lila.mon.api.challenge.bulk.scheduleNb(me.id.value).increment(nbGames)
              idGenerator
                .games(nbGames)
                .map:
                  _.toList zip pairs
                .map:
                  _.map:
                    case (id, (w, b)) => ScheduledGame(id, w, b)
                .dmap:
                  ScheduledBulk(
                    _id = ThreadLocalRandom nextString 8,
                    by = me.id,
                    _,
                    data.variant,
                    data.clockOrDays,
                    Mode(data.rated),
                    pairAt = data.pairAt | nowInstant,
                    startClocksAt = data.startClocksAt,
                    message = data.message,
                    rules = data.rules,
                    scheduledAt = nowInstant,
                    fen = data.fen
                  )
                .dmap(Right.apply)
