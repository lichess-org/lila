package lila.friend

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

private[friend] final class DataForm {
}

private[friend] case class RequestSetup(
  friend: String,
  message: String)
