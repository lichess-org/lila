package lila.clas

import org.joda.time.DateTime
import reactivemongo.api._
import scala.concurrent.duration._

import lila.common.config.BaseUrl
import lila.common.EmailAddress
import lila.db.dsl._
import lila.memo.CacheApi._
import lila.msg.MsgApi
import lila.security.Permission
import lila.user.{ Authenticator, User, UserRepo }

final class ClasApi(
    colls: ClasColls,
    nameGenerator: NameGenerator,
    userRepo: UserRepo,
    msgApi: MsgApi,
    authenticator: Authenticator,
    cacheApi: lila.memo.CacheApi,
    baseUrl: BaseUrl
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BsonHandlers._

  object clas {

    val coll = colls.clas

    def byId(id: Clas.Id) = coll.byId[Clas](id.value)

    def of(teacher: User): Fu[List[Clas]] =
      coll
        .find($doc("teachers" -> teacher.id))
        .sort($doc("archived" -> 1, "viewedAt" -> -1))
        .cursor[Clas]()
        .list(100)

    def byIds(clasIds: List[Clas.Id]): Fu[List[Clas]] =
      coll
        .find($inIds(clasIds))
        .sort($sort desc "createdAt")
        .cursor[Clas]()
        .list()

    def create(data: ClasForm.ClasData, teacher: User): Fu[Clas] = {
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas) inject clas
    }

    def update(from: Clas, data: ClasForm.ClasData): Fu[Clas] = {
      val clas = data update from
      userRepo.filterByRole(clas.teachers.toList, Permission.Teacher.dbKey) flatMap { filtered =>
        val checked = clas.copy(
          teachers = clas.teachers.toList.filter(filtered.contains).toNel | from.teachers
        )
        coll.update.one($id(clas.id), checked) inject checked
      }
    }

    def updateWall(clas: Clas, text: String): Funit =
      coll.updateField($id(clas.id), "wall", text).void

    def getAndView(id: Clas.Id, teacher: User): Fu[Option[Clas]] =
      coll.ext
        .findAndUpdate[Clas](
          selector = $id(id) ++ $doc("teachers" -> teacher.id),
          update = $set("viewedAt" -> DateTime.now),
          fetchNewObject = true
        )

    def teachers(clas: Clas): Fu[List[User]] =
      userRepo.byOrderedIds(clas.teachers.toList, ReadPreference.secondaryPreferred)

    def isTeacherOf(teacher: User, clasId: Clas.Id): Fu[Boolean] =
      coll.exists($id(clasId) ++ $doc("teachers" -> teacher.id))

    def isTeacherOfStudent(teacherId: User.ID, studentId: Student.Id): Fu[Boolean] =
      student.isStudent(studentId.value) >>&
        colls.student
          .aggregateExists(readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
            import framework._
            Match($doc("userId" -> studentId.value)) -> List(
              Project($doc("clasId" -> true)),
              PipelineOperator(
                $doc(
                  "$lookup" -> $doc(
                    "from" -> colls.clas.name,
                    "let"  -> $doc("c" -> "$clasId"),
                    "pipeline" -> $arr(
                      $doc(
                        "$match" -> $doc(
                          "$expr" -> $doc(
                            "$and" -> $arr(
                              $doc("$eq" -> $arr("$_id", "$$c")),
                              $doc("$in" -> $arr(teacherId, "$teachers"))
                            )
                          )
                        )
                      ),
                      $doc("$limit"   -> 1),
                      $doc("$project" -> $id(true))
                    ),
                    "as" -> "clas"
                  )
                )
              ),
              Match("clas" $ne $arr()),
              Limit(1),
              Project($id(true))
            )
          }

    def archive(c: Clas, t: User, v: Boolean): Funit =
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
      of($doc("clasId" -> clas.id) ++ selectArchived(false))

    def allWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id)) flatMap withUsers

    def activeWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      of($doc("clasId" -> clas.id) ++ selectArchived(false)) flatMap withUsers

    private def of(selector: Bdoc): Fu[List[Student]] =
      coll
        .find(selector)
        .sort($sort asc "userId")
        .cursor[Student]()
        .list(500)

    def clasIdsOfUser(userId: User.ID): Fu[List[Clas.Id]] =
      coll.distinctEasy[Clas.Id, List]("clasId", $doc("userId" -> userId) ++ selectArchived(false))

    def withUsers(students: List[Student]): Fu[List[Student.WithUser]] =
      userRepo.coll.idsMap[User, User.ID](
        students.map(_.userId),
        ReadPreference.secondaryPreferred
      )(_.id) map { users =>
        students.flatMap { s =>
          users.get(s.userId) map { Student.WithUser(s, _) }
        }
      }

    def count(clasId: Clas.Id): Fu[Int] = coll.countSel($doc("clasId" -> clasId))

    def isManaged(user: User): Fu[Boolean] =
      coll.exists($doc("userId" -> user.id, "managed" -> true))

    def release(user: User): Funit =
      coll.updateField($doc("userId" -> user.id, "managed" -> true), "managed", false).void

    def get(clas: Clas, userId: User.ID): Fu[Option[Student]] =
      coll.one[Student]($id(Student.id(userId, clas.id)))

    def get(clas: Clas, user: User): Fu[Option[Student.WithUser]] =
      get(clas, user.id) map2 { Student.WithUser(_, user) }

    def update(from: Student, data: ClasForm.StudentData): Fu[Student] = {
      val student = data update from
      coll.update.one($id(student.id), student) inject student
    }

    def create(
        clas: Clas,
        data: ClasForm.NewStudent,
        teacher: User
    ): Fu[Student.WithPassword] = {
      val email    = EmailAddress(s"noreply.class.${clas.id}.${data.username}@lichess.org")
      val password = Student.password.generate
      lila.mon.clas.studentCreate(teacher.id)
      userRepo
        .create(
          username = data.username,
          passwordHash = authenticator.passEnc(password),
          email = email,
          blind = false,
          mobileApiVersion = none,
          mustConfirmEmail = false,
          lang = teacher.lang
        )
        .orFail(s"No user could be created for ${data.username}")
        .flatMap { user =>
          val student = Student.make(user, clas, teacher.id, data.realName, managed = true)
          userRepo.setKid(user, v = true) >>
            userRepo.setManagedUserInitialPerfs(user.id) >>
            coll.insert.one(student) >>
            sendWelcomeMessage(teacher.id, user, clas) inject
            Student.WithPassword(student, password)
        }
    }

    def manyCreate(
        clas: Clas,
        data: ClasForm.ManyNewStudent,
        teacher: User
    ): Fu[List[Student.WithPassword]] =
      count(clas.id) flatMap { nbCurrentStudents =>
        lila.common.Future.linear(data.realNames.take(Clas.maxStudents - nbCurrentStudents)) { realName =>
          nameGenerator() flatMap { username =>
            val data = ClasForm.NewStudent(
              username = username | lila.common.ThreadLocalRandom.nextString(10),
              realName = realName
            )
            create(clas, data, teacher)
          }
        }
      }

    def resetPassword(s: Student): Fu[ClearPassword] = {
      val password = Student.password.generate
      authenticator.setPassword(s.userId, password) inject password
    }

    def archive(sId: Student.Id, t: User, v: Boolean): Fu[Option[Student]] =
      coll.ext
        .findAndUpdate[Student](
          selector = $id(sId),
          update =
            if (v) $set("archived" -> Clas.Recorded(t.id, DateTime.now))
            else $unset("archived"),
          fetchNewObject = true
        )

    def allIds = idsCache.getUnit

    def isStudent(userId: User.ID) = idsCache.getUnit.dmap(_ contains userId)

    private val idsCache = cacheApi.unit[Set[User.ID]] {
      _.refreshAfterWrite(601 seconds)
        .buildAsyncFuture { _ =>
          coll.distinctEasy[User.ID, Set]("userId", $empty, ReadPreference.secondaryPreferred)
        }
    }

    private[ClasApi] def sendWelcomeMessage(teacherId: User.ID, student: User, clas: Clas): Funit =
      msgApi
        .post(
          orig = teacherId,
          dest = student.id,
          text = s"""${lila.i18n.I18nKeys.clas.welcomeToClass
            .txt(clas.name)(student.realLang | lila.i18n.defaultLang)}

$baseUrl/class/${clas.id}

${clas.desc}""",
          multi = true
        )
        .void
  }

  object invite {

    import ClasInvite.Feedback._

    def create(clas: Clas, user: User, realName: String, teacher: User): Fu[ClasInvite.Feedback] =
      student
        .archive(Student.id(user.id, clas.id), user, v = false)
        .map2[ClasInvite.Feedback](_ => Already) getOrElse {
        lila.mon.clas.studentInvite(teacher.id)
        val invite = ClasInvite.make(clas, user, realName, teacher)
        colls.invite.insert
          .one(invite)
          .void
          .flatMap { _ =>
            sendInviteMessage(teacher, user, clas, invite)
          }
          .recover {
            lila.db.recoverDuplicateKey(_ => Found)
          }
      }

    def get(id: ClasInvite.Id) = colls.invite.one[ClasInvite]($id(id))

    def view(id: ClasInvite.Id, user: User): Fu[Option[(ClasInvite, Clas)]] =
      colls.invite.one[ClasInvite]($id(id) ++ $doc("userId" -> user.id)) flatMap {
        _ ?? { invite =>
          colls.clas.byId[Clas](invite.clasId.value).map2 { invite -> _ }
        }
      }

    def accept(id: ClasInvite.Id, user: User): Fu[Option[Student]] =
      colls.invite.one[ClasInvite]($id(id) ++ $doc("userId" -> user.id)) flatMap {
        _ ?? { invite =>
          colls.clas.one[Clas]($id(invite.clasId)) flatMap {
            _ ?? { clas =>
              val stu = Student.make(user, clas, invite.created.by, invite.realName, managed = false)
              colls.student.insert.one(stu) >>
                colls.invite.updateField($id(id), "accepted", true) >>
                student.sendWelcomeMessage(invite.created.by, user, clas) inject
                stu.some recoverWith lila.db.recoverDuplicateKey { _ =>
                student.get(clas, user.id)
              }
            }
          }
        }
      }

    def decline(id: ClasInvite.Id): Fu[Option[ClasInvite]] =
      colls.invite.ext
        .findAndUpdate[ClasInvite](
          selector = $id(id),
          update = $set("accepted" -> false)
        )

    def listPending(clas: Clas): Fu[List[ClasInvite]] =
      colls.invite
        .find($doc("clasId" -> clas.id, "accepted" $ne true))
        .sort($sort desc "created.at")
        .cursor[ClasInvite]()
        .list(100)

    def delete(id: ClasInvite.Id): Funit =
      colls.invite.delete.one($id(id)).void

    private def sendInviteMessage(
        teacher: User,
        student: User,
        clas: Clas,
        invite: ClasInvite
    ): Fu[ClasInvite.Feedback] = {
      val url = s"$baseUrl/class/invitation/${invite._id}"
      if (student.kid) fuccess(ClasInvite.Feedback.CantMsgKid(url))
      else {
        import lila.i18n.I18nKeys.clas._
        implicit val lang = student.realLang | lila.i18n.defaultLang
        msgApi
          .post(
            orig = teacher.id,
            dest = student.id,
            text = s"""${invitationToClass.txt(clas.name)}

${clickToViewInvitation.txt()}

$url""",
            multi = true
          ) inject ClasInvite.Feedback.Invited
      }
    }
  }

  private def selectArchived(v: Boolean) = $doc("archived" $exists v)
}
