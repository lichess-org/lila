package lila.clas

import play.api.data._
import play.api.data.Forms._
import scala.concurrent.duration._

import lila.common.Form.{ cleanNonEmptyText, cleanText }
import lila.user.User

final class ClasForm(
    lightUserAsync: lila.common.LightUser.Getter,
    securityForms: lila.security.SecurityForm,
    nameGenerator: NameGenerator
)(implicit ec: scala.concurrent.ExecutionContext) {

  import ClasForm._

  object clas {

    val form = Form(
      mapping(
        "name" -> cleanText(minLength = 3, maxLength = 100),
        "desc" -> cleanText(minLength = 0, maxLength = 2000),
        "teachers" -> nonEmptyText.verifying(
          "Invalid teacher list",
          str => {
            val ids = readTeacherIds(str)
            ids.nonEmpty && ids.sizeIs <= 10 && ids.forall { id =>
              blockingFetchUser(id).isDefined
            }
          }
        )
      )(ClasData.apply)(ClasData.unapply)
    )

    def create = form

    def edit(c: Clas) =
      form fill ClasData(
        name = c.name,
        desc = c.desc,
        teachers = c.teachers.toList mkString "\n"
      )

    def wall = Form(single("wall" -> text))

    def notifyText = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 300)))
  }

  object student {

    val create: Form[NewStudent] =
      Form(
        mapping(
          "create-username" -> securityForms.signup.username,
          "create-realName" -> cleanNonEmptyText(maxLength = 100)
        )(NewStudent.apply)(NewStudent.unapply)
      )

    def generate: Fu[Form[NewStudent]] =
      nameGenerator() map { username =>
        create fill
          NewStudent(
            username = ~username,
            realName = ""
          )
      }

    def invite(c: Clas) =
      Form(
        mapping(
          "username" -> lila.user.UserForm.historicalUsernameField
            .verifying("Unknown username", { blockingFetchUser(_).isDefined })
            .verifying("This is a teacher", u => !c.teachers.toList.contains(u.toLowerCase)),
          "realName" -> cleanNonEmptyText
        )(NewStudent.apply)(NewStudent.unapply)
      )

    def edit(s: Student) =
      Form(
        mapping(
          "realName" -> cleanNonEmptyText,
          "notes"    -> text(maxLength = 20000)
        )(StudentData.apply)(StudentData.unapply)
      ) fill StudentData(s.realName, s.notes)

    def release =
      Form(
        single(
          "email" -> securityForms.signup.emailField
        )
      )

    def manyCreate(max: Int): Form[ManyNewStudent] =
      Form(
        mapping(
          "realNames" -> cleanNonEmptyText
        )(ManyNewStudent.apply)(ManyNewStudent.unapply).verifying(
          s"There can't be more than ${lila.clas.Clas.maxStudents} per class. Split the students into more classes.",
          _.realNames.lengthIs <= max
        )
      )
  }

  private def blockingFetchUser(username: String) =
    lightUserAsync(User normalize username).await(1 second, "clasInviteUser")
}

object ClasForm {

  private val realNameMaxSize = 100

  case class ClasData(
      name: String,
      desc: String,
      teachers: String
  ) {
    def update(c: Clas) =
      c.copy(
        name = name,
        desc = desc,
        teachers = teacherIds.toNel | c.teachers
      )

    def teacherIds = readTeacherIds(teachers)
  }

  private def readTeacherIds(str: String) =
    str.linesIterator.map(_.trim).filter(_.nonEmpty).map(User.normalize).distinct.toList

  case class NewStudent(
      username: String,
      realName: String
  )

  case class StudentData(
      realName: String,
      notes: String
  ) {
    def update(c: Student) =
      c.copy(
        realName = realName,
        notes = notes
      )
  }

  case class ManyNewStudent(realNamesText: String) {
    def realNames =
      realNamesText.linesIterator.map(_.trim take realNameMaxSize).filter(_.nonEmpty).distinct.toList
  }
}
