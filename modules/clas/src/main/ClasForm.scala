package lila.clas

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

final class ClasForm(
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.DataForm
) {

  import ClasForm._

  val form = Form(
    mapping(
      "name" -> text(minLength = 3, maxLength = 100),
      "desc" -> text(minLength = 0, maxLength = 2000)
    )(Data.apply)(Data.unapply)
  )

  def create = form

  def edit(c: Clas) = form fill Data(
    name = c.name,
    desc = c.desc
  )

  object student {

    def create = securityForms.signup.managed

    def invite = Form(
      single(
        "invite" -> lila.user.DataForm.historicalUsernameField.verifying("Unknown username", {
          blockingFetchUser(_).isDefined
        })
      )
    )

    private def blockingFetchUser(username: String) =
      lightUserAsync(lila.user.User normalize username).await(1 second, "clasInviteUser")
  }
}

object ClasForm {

  case class Data(
      name: String,
      desc: String
  ) {
    def update(c: Clas) = c.copy(
      name = name,
      desc = desc
    )
  }
}
