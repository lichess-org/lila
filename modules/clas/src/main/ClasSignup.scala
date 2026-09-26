package lila.clas

import scalalib.ThreadLocalRandom
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.formatter
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

    def form(max: Int): Form[StudentLines] = Form:
      single(
        "realNames" -> of(using formatter.stringTryFormatter[StudentLines](parseLines, _.text))
      ).verifying(
        s"There can't be more than ${lila.clas.Clas.maxStudents} per class. Split the students into more classes.",
        _.lines.lengthIs <= max
      )

    case class StudentLines(lines: List[StudentLine]):
      def text =
        lines
          .map:
            case (Some(username), realName) => s"@$username $realName"
            case (None, realName) => realName.value
          .mkString("\n")

    private def parseLines(text: String): Either[String, StudentLines] =
      text.linesIterator.toList
        .foldLeft(Either.right[String, List[StudentLine]](Nil)):
          case (Left(err), _) => Left(err)
          case (Right(lines), line) =>
            for
              (usr, name) <-
                if line.startsWith("@") then
                  line.drop(1).trim.split(" ", 2) match
                    case Array(username, realName) =>
                      UserStr.read(username).map(_.some -> realName).toRight(s"Invalid username: $username")
                    case _ => Left(s"Invalid line: $line")
                else Right(none -> line)
              name <-
                if name.trim.nonEmpty
                then Right(name.trim.take(realNameMaxSize))
                else Left(s"Empty name in line: $line")
              _ <- usr
                .filter(u => lines.exists(_._1.contains(u)))
                .fold(Right(()))(u => Left(s"Duplicate username: $u"))
            yield (usr, RealName(name)) :: lines
        .flatMap:
          case Nil => Left("Empty list")
          case lines => Right(StudentLines(lines.reverse))

    def create(
        clas: Clas,
        data: StudentLines
    )(using teacher: Me)(using Lang): Fu[List[Student.WithPassword]] =
      for
        nbCurrentStudents <- colls.countStudents(clas.id)
        newStudents <- data.lines
          .take(Clas.maxStudents - nbCurrentStudents)
          .sequentially: (usr, realName) =>
            usr
              .fold(generateName())(u => fuccess(u.into(UserName)))
              .flatMap: username =>
                userRepo
                  .existsSec(username)
                  .flatMap:
                    case true => fuccess(none)
                    case false =>
                      val data = ClasForm.CreateStudent(username, realName)
                      one.create(clas, data).dmap(some)
        _ = clasApi.teamSync(clas)
      yield newStudents.flatten

  private def generateName()(using Lang) =
    nameGenerator().map(_ | UserName(ThreadLocalRandom.nextString(10)))

object ClasSignup:

  private val realNameMaxSize = 100

  private type StudentLine = (Option[UserStr], RealName)

  case class StudentLines(lines: List[StudentLine])
