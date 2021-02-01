package lila.setup

import akka.stream.scaladsl._
import chess.format.FEN
import chess.variant.Variant
import chess.{ Clock, Speed }
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import lila.game.Game
import lila.oauth.AccessToken
import lila.oauth.OAuthScope
import lila.oauth.OAuthServer
import lila.user.User

object BulkChallenge {

  val maxGames = 500

  case class BulkFormData(tokens: String, variant: Variant, clock: Clock.Config, rated: Boolean)

  val form = Form[BulkFormData](
    mapping(
      "tokens" -> nonEmptyText
        .verifying("Not enough tokens", t => extractTokenPairs(t).isEmpty)
        .verifying(s"Too many tokens (max: ${maxGames * 2})", t => extractTokenPairs(t).sizeIs > maxGames),
      SetupForm.api.variant,
      "clock" -> SetupForm.api.clockMapping,
      "rated" -> boolean
    ) { (tokens: String, variant: Option[String], clock: Clock.Config, rated: Boolean) =>
      BulkFormData(tokens, Variant orDefault ~variant, clock, rated)
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

  case class ScheduledBulkPairing(
      players: List[(User.ID, User.ID)],
      variant: Variant,
      clock: Clock.Config,
      rated: Boolean,
      pairAt: DateTime,
      startClocksAt: DateTime
  )
}

final class BulkChallengeApi(oauthServer: OAuthServer)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import BulkChallenge._

  def apply(data: BulkFormData): Fu[Either[List[BadToken], ScheduledBulkPairing]] =
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
      .map {
        _.map {
          _.reverse
            .grouped(2)
            .collect { case List(w, b) => (w, b) }
            .toList
        }.left.map(_.reverse)
      }
      .map {
        _.map { players =>
          ScheduledBulkPairing(
            players,
            data.variant,
            data.clock,
            data.rated,
            DateTime.now,
            DateTime.now
          )
        }
      }
}
