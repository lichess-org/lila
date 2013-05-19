package lila
package friend

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

private[friend] final class DataForm(repo: FriendRepo) {

  import lila.core.Form._
}

private[friend] case class RequestSetup(
  friend: String,
  message: String)
