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
    )(ClasData.apply)(ClasData.unapply)
  )

  def create = form

  def edit(c: Clas) = form fill ClasData(
    name = c.name,
    desc = c.desc
  )

  object student {

    def create = Form(
      mapping(
        "username" -> securityForms.signup.username,
        "realName" -> nonEmptyText
      )(NewStudent.apply)(NewStudent.unapply)
    )

    def invite = Form(
      mapping(
        "username" -> lila.user.DataForm.historicalUsernameField.verifying("Unknown username", {
          blockingFetchUser(_).isDefined
        }),
        "realName" -> nonEmptyText
      )(NewStudent.apply)(NewStudent.unapply)
    )

    private def blockingFetchUser(username: String) =
      lightUserAsync(lila.user.User normalize username).await(1 second, "clasInviteUser")
  }
}

object ClasForm {

  case class ClasData(
      name: String,
      desc: String
  ) {
    def update(c: Clas) = c.copy(
      name = name,
      desc = desc
    )
  }

  case class NewStudent(
      username: String,
      realName: String
  )
}
