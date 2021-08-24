package lila.clas

import org.joda.time.DateTime
import reactivemongo.api._

import lila.common.config.BaseUrl
import lila.common.EmailAddress
import lila.db.dsl._
import lila.msg.MsgApi
import lila.security.Permission
import lila.user.{ Authenticator, User, UserRepo }
import lila.user.Holder
import lila.hub.actorApi.user.KidId

final class ClasApi(
    colls: ClasColls,
    studentCache: ClasStudentCache,
    nameGenerator: NameGenerator,
    userRepo: UserRepo,
    msgApi: MsgApi,
    authenticator: Authenticator,
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
      userRepo.filterEnabled(clas.teachers.toList) flatMap { filtered =>
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

    def areKidsInSameClass(kid1: KidId, kid2: KidId): Fu[Boolean] =
      fuccess(studentCache.isStudent(kid1.id) && studentCache.isStudent(kid2.id)) >>&
        colls.student.aggregateExists(readPreference = ReadPreference.secondaryPreferred) {
          implicit framework =>
            import framework._
            Match($doc("userId" $in List(kid1.id, kid2.id))) -> List(
              GroupField("clasId")("nb" -> SumAll),
              Match($doc("nb" -> 2)),
              Limit(1)
            )
        }

    def isTeacherOf(teacher: User.ID, student: User.ID): Fu[Boolean] =
      studentCache.isStudent(student) ?? colls.student
        .aggregateExists(readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
          import framework._
          Match($doc("userId" -> student)) -> List(
            Project($doc("clasId" -> true)),
            PipelineOperator(
              $lookup.pipeline(
                from = colls.clas,
                as = "clas",
                local = "clasId",
                foreign = "_id",
                pipeline = List(
                  $doc(
                    "$match" -> $doc(
                      "$expr" -> $doc("$in" -> $arr(teacher, "$teachers"))
                    )
                  ),
                  $doc("$limit"   -> 1),
                  $doc("$project" -> $id(true))
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

    def allWithUsers(clas: Clas, selector: Bdoc = $empty): Fu[List[Student.WithUser]] =
      colls.student
        .aggregateList(Int.MaxValue, ReadPreference.secondaryPreferred) { framework =>
          import framework._
          Match($doc("clasId" -> clas.id) ++ selector) -> List(
            PipelineOperator(
              $doc(
                "$lookup" -> $doc(
                  "from"         -> userRepo.coll.name,
                  "as"           -> "user",
                  "localField"   -> "userId",
                  "foreignField" -> "_id"
                )
              )
            ),
            UnwindField("user")
          )
        }
        .map { docs =>
          for {
            doc     <- docs
            student <- doc.asOpt[Student]
            user    <- doc.getAsOpt[User]("user")
          } yield Student.WithUser(student, user)
        }

    def activeWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      allWithUsers(clas, selectArchived(false))

    private def of(selector: Bdoc): Fu[List[Student]] =
      coll
        .find(selector)
        .sort($sort asc "userId")
        .cursor[Student]()
        .list(500)

    def clasIdsOfUser(userId: User.ID): Fu[List[Clas.Id]] =
      coll.distinctEasy[Clas.Id, List]("clasId", $doc("userId" -> userId) ++ selectArchived(false))

    def count(clasId: Clas.Id): Fu[Int] = coll.countSel($doc("clasId" -> clasId))

    def isManaged(user: User): Fu[Boolean] =
      coll.exists($doc("userId" -> user.id, "managed" -> true))

    def release(user: User): Funit =
      coll.updateField($doc("userId" -> user.id, "managed" -> true), "managed", false).void

    def findManaged(user: User): Fu[Option[Student.ManagedInfo]] =
      coll.find($doc("userId" -> user.id, "managed" -> true)).one[Student] flatMap {
        _ ?? { student =>
          userRepo.byId(student.created.by) zip clas.byId(student.clasId) map {
            case (Some(teacher), Some(clas)) => Student.ManagedInfo(teacher, clas).some
            case _                           => none
          }
        }
      }

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
      lila.mon.clas.student.create(teacher.id).increment()
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
          studentCache addStudent user.id
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

    def archive(sId: Student.Id, by: Holder, v: Boolean): Fu[Option[Student]] =
      coll.ext
        .findAndUpdate[Student](
          selector = $id(sId),
          update =
            if (v) $set("archived" -> Clas.Recorded(by.id, DateTime.now))
            else $unset("archived"),
          fetchNewObject = true
        )

    def closeAccount(s: Student.WithUser): Funit =
      coll.delete.one($id(s.student.id)).void

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

    def create(clas: Clas, user: User, realName: String, teacher: Holder): Fu[ClasInvite.Feedback] =
      student
        .archive(Student.id(user.id, clas.id), teacher, v = false)
        .map2[ClasInvite.Feedback](_ => Already) getOrElse {
        lila.mon.clas.student.invite(teacher.id).increment()
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
              studentCache addStudent user.id
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
        teacher: Holder,
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
