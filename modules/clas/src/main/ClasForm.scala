package lila.clas

import play.api.data.*
import play.api.data.Forms.*
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.common.Form.{ cleanNonEmptyText, cleanText, into }
import lila.core.security.{ Hcaptcha, HcaptchaForm }

final class ClasForm(
    lightUserAsync: lila.core.LightUser.Getter,
    signupForm: lila.core.security.SignupForm,
    nameGenerator: NameGenerator,
    hcaptcha: Hcaptcha
)(using Executor):

  import ClasForm.*

  object clas:

    val form: Form[ClasData] = Form:
      mapping(
        "name" -> cleanText(minLength = 3, maxLength = 100),
        "desc" -> cleanText(minLength = 0, maxLength = 2000),
        "teachers" -> nonEmptyText.verifying(
          "Invalid teacher list",
          str =>
            val ids = readTeacherIds(str)
            ids.nonEmpty && ids.sizeIs <= 10 && ids.forall: id =>
              blockingFetchUser(id.into(UserStr)).isDefined
        )
      )(ClasData.apply)(unapply)

    def create(using RequestHeader): Fu[HcaptchaForm[ClasData]] = hcaptcha.form(form)

    def edit(c: Clas): Form[ClasData] = form.fill:
      ClasData(
        name = c.name,
        desc = c.desc,
        teachers = c.teachers.toList.mkString("\n")
      )

    def wall = Form(single("wall" -> text(maxLength = 100_000).into[Markdown]))

    def notifyText = Form(single("text" -> nonEmptyText(minLength = 10, maxLength = 300)))

  object student:

    val create: Form[CreateStudent] = Form:
      mapping(
        "create-username" -> signupForm.username,
        "create-realName" -> cleanNonEmptyText(maxLength = 100)
      )(CreateStudent.apply)(unapply)

    def generate(using Lang): Fu[Form[CreateStudent]] =
      nameGenerator().map: username =>
        create.fill:
          CreateStudent(
            username = username | UserName(""),
            realName = ""
          )

    def invite(c: Clas) = Form:
      mapping(
        "username" -> lila.common.Form.username.historicalField
          .verifying("Unknown username", { blockingFetchUser(_).exists(!_.isBot) })
          .verifying("This is a teacher", u => !c.teachers.toList.contains(u.id)),
        "realName" -> cleanNonEmptyText
      )(InviteStudent.apply)(unapply)

    val inviteAccept = Form(single("v" -> Forms.boolean))

    def edit(s: Student) = Form(
      mapping(
        "realName" -> cleanNonEmptyText,
        "notes"    -> text(maxLength = 20000)
      )(StudentData.apply)(unapply)
    ).fill(StudentData(s.realName, s.notes))

    def release = Form(single("email" -> signupForm.emailField))

    def manyCreate(max: Int): Form[ManyNewStudent] = Form:
      mapping(
        "realNames" -> cleanNonEmptyText
      )(ManyNewStudent.apply)(_.realNamesText.some).verifying(
        s"There can't be more than ${lila.clas.Clas.maxStudents} per class. Split the students into more classes.",
        _.realNames.lengthIs <= max
      )

  private def blockingFetchUser(username: UserStr) =
    lightUserAsync(username.id).await(1.second, "clasInviteUser")

object ClasForm:

  private val realNameMaxSize = 100

  case class ClasData(
      name: String,
      desc: String,
      teachers: String
  ):
    def update(c: Clas) =
      c.copy(
        name = name,
        desc = desc,
        teachers = teacherIds.toNel | c.teachers
      )

    def teacherIds = readTeacherIds(teachers)

  private def readTeacherIds(str: String) =
    UserStr.from(str.linesIterator.map(_.trim).filter(_.nonEmpty)).map(_.id).distinct.toList

  case class InviteStudent(username: UserStr, realName: String)
  case class CreateStudent(username: UserName, realName: String)

  case class StudentData(
      realName: String,
      notes: String
  ):
    def update(c: Student) =
      c.copy(
        realName = realName,
        notes = notes
      )

  case class ManyNewStudent(realNamesText: String):
    def realNames =
      realNamesText.linesIterator.map(_.trim.take(realNameMaxSize)).filter(_.nonEmpty).distinct.toList
