package lila.clas

import scalalib.ThreadLocalRandom
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.cleanNonEmptyText
import lila.clas.Student.RealName
import play.api.i18n.Lang
import lila.core.user.KidMode

final class ClasSignup(
    colls: ClasColls,
    filters: ClasUserFilters,
    clasMsg: ClasMsg,
    clasApi: ClasApi, // only for teamSync
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    authenticator: lila.core.security.Authenticator,
    nameGenerator: NameGenerator
)(using Executor):

  import ClasSignup.*
  import BsonHandlers.given

  object one:

    def create(
        clas: Clas,
        data: ClasForm.CreateStudent
    )(using teacher: Me): Fu[Student.WithPassword] =
      val email = EmailAddress(s"noreply.class.${clas.id}.${data.username}@lichess.org")
      val password = Student.password.generate()
      lila.mon.clas.student.create(teacher.userId).increment()
      for
        user <- userRepo
          .create(
            name = data.username,
            passwordHash = authenticator.passEnc(password),
            email = email,
            blind = false,
            mustConfirmEmail = false,
            lang = teacher.lang,
            kid = KidMode.Yes
          )
          .orFail(s"No user could be created for ${data.username}")
        _ = filters.student.add(user.id)
        student = Student.make(user, clas, teacher.userId, data.realName, managed = true)
        _ <- perfsRepo.setManagedUserInitialPerfs(user.id)
        _ <- colls.student.insert.one(student)
        _ <- clasMsg.welcomeMessage(teacher.userId, user, clas)
        _ = clasApi.teamSync(clas)
      yield Student.WithPassword(student, password)

  object multi:

    def form(max: Int): Form[ManyNewStudent] = Form:
      mapping(
        "realNames" -> cleanNonEmptyText
      )(ManyNewStudent.apply)(_.realNamesText.some).verifying(
        s"There can't be more than ${lila.clas.Clas.maxStudents} per class. Split the students into more classes.",
        _.realNames.lengthIs <= max
      )

    def create(
        clas: Clas,
        data: ManyNewStudent
    )(using teacher: Me)(using Lang): Fu[List[Student.WithPassword]] =
      for
        nbCurrentStudents <- colls.countStudents(clas.id)
        newStudents <- data.realNames
          .take(Clas.maxStudents - nbCurrentStudents)
          .sequentially: realName =>
            nameGenerator().flatMap: username =>
              val data = ClasForm.CreateStudent(
                username = username | UserName(ThreadLocalRandom.nextString(10)),
                realName = realName
              )
              one.create(clas, data)
        _ = clasApi.teamSync(clas)
      yield newStudents

object ClasSignup:

  private val realNameMaxSize = 100

  case class ManyNewStudent(realNamesText: String):
    def realNames = RealName.from:
      realNamesText.linesIterator.map(_.trim.take(realNameMaxSize)).filter(_.nonEmpty).distinct.toList
