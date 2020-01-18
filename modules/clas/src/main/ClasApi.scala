package lila.clas

import org.joda.time.DateTime
import reactivemongo.api._

import lila.common.config.BaseUrl
import lila.common.EmailAddress
import lila.db.dsl._
import lila.message.MessageApi
import lila.user.{ Authenticator, User, UserRepo }

final class ClasApi(
    colls: ClasColls,
    userRepo: UserRepo,
    messageApi: MessageApi,
    authenticator: Authenticator,
    baseUrl: BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  object teacher {

    val coll = colls.teacher

    def withOrCreate(user: User): Fu[Teacher.WithUser] =
      coll.byId[Teacher](user.id) flatMap {
        case Some(teacher) => fuccess(Teacher.WithUser(teacher, user))
        case None =>
          val teacher = Teacher make user
          coll.insert.one(teacher) inject Teacher.WithUser(teacher, user)
      }

    // def of(clas: Clas): Fu[List[Teacher]] =
    //   coll.ext.byOrderedIds[Teacher, Teacher.Id](clas.teacherIds)(_.id)
  }

  object clas {

    val coll = colls.clas

    def byId(id: Clas.Id) = coll.byId[Clas](id.value)

    def of(teacher: Teacher): Fu[List[Clas]] =
      coll.ext
        .find($doc("teachers" -> teacher.id))
        .sort($sort desc "viewedAt")
        .list[Clas]()

    def create(data: ClasForm.ClasData, teacher: Teacher): Fu[Clas] = {
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas) inject clas
    }

    def update(from: Clas, data: ClasForm.ClasData): Fu[Clas] = {
      val clas = data update from
      coll.update.one($id(clas.id), clas) inject clas
    }

    def getAndView(id: Clas.Id, teacher: Teacher): Fu[Option[Clas]] =
      coll.ext
        .findAndUpdate[Clas](
          selector = $id(id) ++ $doc("teachers" -> teacher.id),
          update = $set("viewedAt"              -> DateTime.now),
          fetchNewObject = true
        )

    def isTeacherOf(user: User, clasId: Clas.Id): Fu[Boolean] =
      coll.exists($id(clasId) ++ $doc("teachers" -> user.id))
  }

  object student {

    import User.ClearPassword

    val coll = colls.student

    def activeOf(clas: Clas): Fu[List[Student]] =
      of($doc("clasId" -> clas.id, "archived" $exists false))

    def allOfWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id)) flatMap withUsers
    def activeOfWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id, "archived" $exists false)) flatMap withUsers

    private def of(selector: Bdoc): Fu[List[Student]] =
      coll.ext
        .find(selector)
        .sort($sort asc "userId")
        .list[Student]()

    def withUsers(students: List[Student]): Fu[List[Student.WithUser]] =
      userRepo.coll.idsMap[User, User.ID](
        students.map(_.userId),
        ReadPreference.secondaryPreferred
      )(_.id) map { users =>
        students.flatMap { s =>
          users.get(s.userId) map { Student.WithUser(s, _) }
        }
      }

    def isManaged(user: User): Fu[Boolean] =
      coll.exists($doc("userId" -> user.id, "managed" -> true))

    def get(clas: Clas, userId: User.ID): Fu[Option[Student]] =
      coll.ext.one[Student]($id(Student.id(userId, clas.id)))

    def get(clas: Clas, user: User): Fu[Option[Student.WithUser]] =
      get(clas, user.id) map2 { Student.WithUser(_, user) }

//     def isIn(clas: Clas, userId: User.ID): Fu[Boolean] =
//       coll.exists($id(Student.id(userId, clas.id)))

    def update(from: Student, data: ClasForm.StudentData): Fu[Student] = {
      val student = data update from
      coll.update.one($id(student.id), student) inject student
    }

    def create(
        clas: Clas,
        data: ClasForm.NewStudent,
        teacher: Teacher.WithUser
    ): Fu[(User, ClearPassword)] = {
      val email    = EmailAddress(s"noreply.class.${clas.id}.${data.username}@lichess.org")
      val password = Student.password.generate
      lila.mon.clas.studentCreate(teacher.user.id)
      userRepo
        .create(
          username = data.username,
          passwordHash = authenticator.passEnc(password),
          email = email,
          blind = false,
          mobileApiVersion = none,
          mustConfirmEmail = false
        )
        .orFail(s"No user could be created for ${data.username}")
        .flatMap { user =>
          userRepo.setKid(user, true) >>
            coll.insert.one(Student.make(user, clas, teacher.teacher.id, data.realName, managed = true)) >>
            sendWelcomeMessage(teacher, user, clas) inject
            (user -> password)
        }
    }

    def invite(clas: Clas, user: User, realName: String, teacher: Teacher.WithUser): Fu[Option[Student]] = {
      lila.mon.clas.studentInvite(teacher.user.id)
      val student = Student.make(user, clas, teacher.teacher.id, realName, managed = false)
      coll.insert.one(student) >> sendWelcomeMessage(teacher, user, clas) inject student.some
    }.recover(lila.db.recoverDuplicateKey(_ => none))

    private[ClasApi] def join(clas: Clas, user: User, teacherId: Teacher.Id): Fu[Student] = {
      val student = Student.make(user, clas, teacherId, "", managed = false)
      coll.insert.one(student) inject student
    }

    def resetPassword(s: Student): Fu[ClearPassword] = {
      val password = Student.password.generate
      authenticator.setPassword(s.userId, password) inject password
    }

    def archive(s: Student, t: Teacher, v: Boolean): Funit =
      coll.update
        .one(
          $id(s.id),
          if (v) $set("archived" -> Clas.Recorded(t.id, DateTime.now))
          else $unset("archived")
        )
        .void

    private def sendWelcomeMessage(teacher: Teacher.WithUser, student: User, clas: Clas): Funit =
      messageApi
        .sendOnBehalf(
          sender = teacher.user,
          dest = student,
          subject = s"Invitation to ${clas.name}",
          text = s"""
Please click this link to join the class ${clas.name}:

$baseUrl/class/${clas.id}

${clas.desc}"""
        )
  }
}
