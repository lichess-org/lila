package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import lila.user.User
import lila.common.Form._

final class SwissForm(implicit ec: scala.concurrent.ExecutionContext) {

  import SwissForm._

  val form = Form(
    mapping(
      "name" -> text.verifying(
        Constraints minLength 2,
        Constraints maxLength 30,
        Constraints.pattern(
          regex = """[\p{L}\p{N}-\s:,;]+""".r,
          error = "error.unknown"
        )
      ),
      "clock" -> mapping(
        "limit"     -> number.verifying(clockLimits.contains _),
        "increment" -> number(min = 0, max = 180)
      )(ClockConfig.apply)(ClockConfig.unapply)
        .verifying("Invalid clock", _.estimateTotalSeconds > 0),
      "startsAt"    -> inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp),
      "variant"     -> nonEmptyText.verifying(v => Variant(v).isDefined),
      "rated"       -> boolean,
      "nbRounds"    -> number(min = 3, max = 50),
      "description" -> optional(nonEmptyText),
      "hasChat"     -> optional(boolean)
    )(SwissData.apply)(SwissData.unapply)
  )

  def create = form
}

object SwissForm {

  val clockLimits: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ {
    (2 to 7 by 1) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  }.map(_.toDouble)

  case class SwissData(
      name: String,
      clock: ClockConfig,
      startsAt: DateTime,
      variant: String,
      rated: Boolean,
      nbRounds: Int,
      description: Option[String],
      hasChat: Option[Boolean]
  ) {
    def realVariant = Variant orDefault variant
  }
}
