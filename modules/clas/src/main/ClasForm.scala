package lila.clas

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

final class ClasForm(
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.DataForm,
    nameGenerator: NameGenerator
)(implicit ec: scala.concurrent.ExecutionContext) {

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

    val create: Form[NewStudent] =
      Form(
        mapping(
          "create-username" -> securityForms.signup.username,
          "create-realName" -> nonEmptyText(maxLength = 100)
        )(NewStudent.apply)(NewStudent.unapply)
      )

    def generate: Fu[Form[NewStudent]] = nameGenerator() map { username =>
      create fill
        NewStudent(
          username = ~username,
          realName = ""
        )
    }

    def invite(c: Clas) =
      Form(
        mapping(
          "username" -> lila.user.DataForm.historicalUsernameField
            .verifying("Unknown username", { blockingFetchUser(_).isDefined })
            .verifying("This is a teacher", u => !c.teachers.toList.contains(u.toLowerCase)),
          "realName" -> nonEmptyText
        )(NewStudent.apply)(NewStudent.unapply)
      )

    def edit(s: Student) =
      Form(
        mapping(
          "realName" -> nonEmptyText,
          "notes"    -> text(maxLength = 20000)
        )(StudentData.apply)(StudentData.unapply)
      ) fill StudentData(s.realName, s.notes)

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

  case class StudentData(
      realName: String,
      notes: String
  ) {
    def update(c: Student) = c.copy(
      realName = realName,
      notes = notes
    )
  }
}
