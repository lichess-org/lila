package lila.message

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

import lila.security.Granter
import lila.user.User
import lila.common.LightUser

final private[message] class DataForm(
    lightUserAsync: LightUser.Getter,
    security: MessageSecurity
) {

  import DataForm._

  def thread(me: User) =
    Form(
      mapping(
        "username" -> lila.user.DataForm.historicalUsernameField
          .verifying("Unknown username", { blockingFetchUser(_).isDefined })
          .verifying(
            "Sorry, this player doesn't accept new messages", { name =>
              Granter(_.MessageAnyone)(me) || {
                security
                  .canMessage(me.id, User normalize name)
                  .await(2 seconds, "pmAccept") // damn you blocking API
              }
            }
          ),
        "subject" -> text(minLength = 3, maxLength = 100),
        "text"    -> text(minLength = 3, maxLength = 8000),
        "mod"     -> optional(nonEmptyText)
      )({
        case (username, subject, text, mod) =>
          ThreadData(
            user = blockingFetchUser(username) err "Unknown username " + username,
            subject = subject,
            text = text,
            asMod = mod.isDefined
          )
      })(_.export.some)
    )

  def post =
    Form(
      single(
        "text" -> text(minLength = 3)
      )
    )

  private def blockingFetchUser(username: String) =
    lightUserAsync(username).await(1 second, "pmUser")
}

object DataForm {

  case class ThreadData(
      user: LightUser,
      subject: String,
      text: String,
      asMod: Boolean
  ) {

    def export = (user.name, subject, text, asMod option "1")
  }
}
