package lila.swiss

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.i18n.Lang
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.{ GreatPlayer, LightUser, Uptime }
import lila.hub.LightTeam.TeamID
import lila.quote.Quote.quoteWriter
import lila.rating.PerfType
import lila.socket.Socket.SocketVersion
import lila.user.User

final class SwissJson(
    // lightUserApi: lila.user.LightUserApi
)(implicit ec: ExecutionContext) {

  def apply(
      swiss: Swiss,
      // rounds: List[SwissRound],
      me: Option[User],
      socketVersion: Option[SocketVersion]
  )(implicit lang: Lang): Fu[JsObject] = fuccess {
    Json
      .obj(
        "id"        -> swiss.id.value,
        "createdBy" -> swiss.createdBy,
        "startsAt"  -> formatDate(swiss.startsAt),
        "name"      -> swiss.name,
        "perf"      -> swiss.perfType,
        "clock"     -> swiss.clock,
        "variant"   -> swiss.variant.key,
        "nbRounds"  -> swiss.nbRounds,
        "nbPlayers" -> swiss.nbPlayers
      )
      .add("isStarted" -> swiss.isStarted)
      .add("isFinished" -> swiss.isFinished)
      .add("socketVersion" -> socketVersion.map(_.value))
      .add("quote" -> swiss.isCreated.option(lila.quote.Quote.one(swiss.id.value)))
      .add("description" -> swiss.description)
  }

  private def formatDate(date: DateTime) = ISODateTimeFormat.dateTime print date

  implicit private val clockWrites: OWrites[chess.Clock.Config] = OWrites { clock =>
    Json.obj(
      "limit"     -> clock.limitSeconds,
      "increment" -> clock.incrementSeconds
    )
  }

  implicit private def perfTypeWrites(implicit lang: Lang): OWrites[PerfType] = OWrites { pt =>
    Json.obj(
      "icon" -> pt.iconChar.toString,
      "name" -> pt.trans
    )
  }
}
