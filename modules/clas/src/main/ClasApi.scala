package lila.clas

import org.joda.time.DateTime
import scala.concurrent.duration._
import reactivemongo.api._

import lila.common.config.BaseUrl
import lila.common.EmailAddress
import lila.security.Permission
import lila.db.dsl._
import lila.msg.MsgApi
import lila.user.{ Authenticator, User, UserRepo }
import lila.memo.CacheApi._

final class ClasApi(
    colls: ClasColls,
    userRepo: UserRepo,
    msgApi: MsgApi,
    authenticator: Authenticator,
    cacheApi: lila.memo.CacheApi,
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

    def of(clas: Clas): Fu[List[Teacher.WithUser]] =
      coll.ext.byOrderedIds[Teacher, Teacher.Id](clas.teachers.toList)(_.id) flatMap withUsers

    def withUsers(teachers: List[Teacher]): Fu[List[Teacher.WithUser]] =
      userRepo.coll.idsMap[User, User.ID](
        teachers.map(_.userId),
        ReadPreference.secondaryPreferred
      )(_.id) map { users =>
        teachers.flatMap { s =>
          users.get(s.userId) map { Teacher.WithUser(s, _) }
        }
      }
  }

  object clas {

    val coll = colls.clas

    def byId(id: Clas.Id) = coll.byId[Clas](id.value)

    def of(teacher: Teacher): Fu[List[Clas]] =
      coll.ext
        .find($doc("teachers" -> teacher.id))
        .sort($doc("archived" -> 1, "viewedAt" -> -1))
        .list[Clas](100)

    def byIds(clasIds: List[Clas.Id]): Fu[List[Clas]] =
      coll.ext
        .find($inIds(clasIds))
        .sort($sort desc "createdAt")
        .list[Clas]()

    def create(data: ClasForm.ClasData, teacher: Teacher): Fu[Clas] = {
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas) inject clas
    }

    def update(from: Clas, data: ClasForm.ClasData): Fu[Clas] = {
      val clas = data update from
      userRepo.filterByRole(clas.teachers.toList.map(_.value), Permission.Teacher.name) flatMap { filtered =>
        val checked = clas.copy(
          teachers = clas.teachers.toList.filter(t => filtered(t.value)).toNel | from.teachers
        )
        coll.update.one($id(clas.id), checked) inject checked
      }
    }

    def updateWall(clas: Clas, text: String): Funit =
      coll.updateField($id(clas.id), "wall", text).void

    def getAndView(id: Clas.Id, teacher: Teacher): Fu[Option[Clas]] =
      coll.ext
        .findAndUpdate[Clas](
          selector = $id(id) ++ $doc("teachers" -> teacher.id),
          update = $set("viewedAt"              -> DateTime.now),
          fetchNewObject = true
        )

    def isTeacherOf(teacher: User, clasId: Clas.Id): Fu[Boolean] =
      coll.exists($id(clasId) ++ $doc("teachers" -> teacher.id))

    def isTeacherOfStudent(teacherId: User.ID, studentId: Student.Id): Fu[Boolean] =
      student.isStudent(studentId.value) >>&
        student.clasIdsOfUser(studentId.value).flatMap { clasIds =>
          coll.exists($inIds(clasIds) ++ $doc("teachers" -> teacherId))
        }

    def archive(c: Clas, t: Teacher, v: Boolean): Funit =
      coll.update
        .one(
          $id(c.id),
          if (v) $set("archived" -> Clas.Recorded(t.id, DateTime.now))
          else $unset("archived")
        )
        .void
  }

  object student {

    import User.ClearPassword

    val coll = colls.student

    def activeOf(clas: Clas): Fu[List[Student]] =
      of($doc("clasId" -> clas.id, "archived" $exists false))

    def allWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id)) flatMap withUsers
    def activeWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id, "archived" $exists false)) flatMap withUsers

    private def of(selector: Bdoc): Fu[List[Student]] =
      coll.ext
        .find(selector)
        .sort($sort asc "userId")
        .list[Student]()

    def clasIdsOfUser(userId: User.ID): Fu[List[Clas.Id]] =
      coll.distinctEasy[Clas.Id, List]("clasId", $doc("userId" -> userId))

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

    def release(user: User): Funit =
      coll.updateField($doc("userId" -> user.id, "managed" -> true), "managed", false).void

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

    def allIds = idsCache.getUnit

    def isStudent(userId: User.ID) = idsCache.getUnit.dmap(_ contains userId)

    private val idsCache = cacheApi.unit[Set[User.ID]] {
      _.refreshAfterWrite(601 seconds)
        .buildAsyncFuture { _ =>
          coll.distinctEasy[User.ID, Set]("userId", $empty)
        }
    }

    private def sendWelcomeMessage(teacher: Teacher.WithUser, student: User, clas: Clas): Funit =
      msgApi
        .post(
          orig = teacher.user.id,
          dest = student.id,
          text = s"""
Welcome to your class: ${clas.name}.
Here is the link to access the class.

$baseUrl/class/${clas.id}

${clas.desc}""",
          unlimited = true
        )
  }
}
