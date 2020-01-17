package lila.clas

import org.joda.time.DateTime
import reactivemongo.api._
import scala.concurrent.duration._

import lila.db.dsl._
import lila.user.{ Authenticator, User, UserRepo }
import lila.common.EmailAddress
import lila.common.config.{ BaseUrl, Secret }
import lila.message.MessageApi
import lila.security.StringToken

final class ClasApi(
    colls: ClasColls,
    inviteSecret: Secret,
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

    def create(data: ClasForm.Data, teacher: Teacher): Fu[Clas] = {
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas) inject clas
    }

    def update(from: Clas, data: ClasForm.Data): Fu[Clas] = {
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
  }

  object student {

    import User.ClearPassword

    val coll = colls.student

    def allOf(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id))
    def activeOf(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id, "archived" $exists false))

    private def of(selector: Bdoc): Fu[List[Student.WithUser]] =
      coll.ext
        .find(selector)
        .list[Student]() flatMap { students =>
        userRepo.coll.idsMap[User, User.ID](
          students.map(_.userId),
          ReadPreference.secondaryPreferred
        )(_.id) map { users =>
          students.flatMap { s =>
            users.get(s.userId) map { Student.WithUser(s, _) }
          }
        }
      }

    def isManaged(user: User): Fu[Boolean] =
      coll.exists($doc("userId" -> user.id, "managed" -> true))

    def get(clas: Clas, user: User): Fu[Option[Student.WithUser]] =
      coll.ext.one[Student]($id(Student.id(user.id, clas.id))) map2 {
        Student.WithUser(_, user)
      }

    def isIn(clas: Clas, userId: User.ID): Fu[Boolean] =
      coll.exists($id(Student.id(userId, clas.id)))

    def create(clas: Clas, username: String, teacher: Teacher): Fu[(User, ClearPassword)] = {
      val email    = EmailAddress(s"noreply.class.${clas.id}.$username@lichess.org")
      val password = Student.password.generate
      lila.mon.clas.studentCreate(teacher.userId)
      userRepo
        .create(
          username = username,
          passwordHash = authenticator.passEnc(password),
          email = email,
          blind = false,
          mobileApiVersion = none,
          mustConfirmEmail = false
        )
        .orFail(s"No user could be created for $username")
        .flatMap { user =>
          userRepo.setKid(user, true) >>
            coll.insert.one(Student.make(user, clas, teacher.id, managed = true)) inject
            (user -> password)
        }
    }

    def invite(clas: Clas, user: User, teacher: Teacher.WithUser): Funit = {
      lila.mon.clas.studentInvite(teacher.user.id)
      !isIn(clas, user.id) flatMap {
        _ ?? ClasApi.this.invite.create(clas, user, teacher)
      }
    }

    private[ClasApi] def join(clas: Clas, user: User, teacherId: Teacher.Id): Fu[Student] = {
      val student = Student.make(user, clas, teacherId, managed = false)
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
  }

  object invite {

    private case class Invite(studentId: Student.Id, teacherId: Teacher.Id)

    private val tokener = {
      import StringToken._
      implicit val tokenSerializable = new Serializable[Invite] {
        def read(str: String) = str.split(' ') match {
          case Array(s, t) => Invite(Student.Id(s), Teacher.Id(t))
          case _           => sys error s"Invalid invite token $str"
        }
        def write(i: Invite) = s"${i.studentId} ${i.teacherId}"
      }
      val lifetime = 7.days
      new StringToken[Invite](
        secret = inviteSecret,
        getCurrentValue = _ => fuccess(DateStr toStr DateTime.now),
        currentValueHashSize = none,
        valueChecker = StringToken.ValueChecker.Custom(v =>
          fuccess {
            DateStr.toDate(v) exists DateTime.now.minusSeconds(lifetime.toSeconds.toInt).isBefore
          }
        )
      )
    }

    def create(clas: Clas, user: User, teacher: Teacher.WithUser): Funit =
      tokener make Invite(Student.id(user.id, clas.id), teacher.teacher.id) flatMap { token =>
        messageApi.sendOnBehalf(
          sender = teacher.user,
          dest = user,
          subject = s"Invitation to ${clas.name}",
          text = s"""
Please click this link to join the class ${clas.name}:

$baseUrl/class/${clas.id}/student/join/$token

${clas.desc}"""
        )
      }

    def redeem(clasId: Clas.Id, user: User, token: String): Fu[Option[Student]] =
      clas.coll.one[Clas]($id(clasId)) flatMap {
        _ ?? { clas =>
          student.get(clas, user).map2(_.student) orElse {
            tokener read token flatMap {
              _.filter {
                _.studentId == Student.id(user.id, clas.id)
              } ?? { invite =>
                student.join(clas, user, invite.teacherId).dmap(some)
              }
            }
          }
        }
      }
  }
}
