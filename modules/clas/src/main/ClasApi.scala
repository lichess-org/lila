package lila.clas

import play.api.i18n.Lang
import reactivemongo.api.*
import scalalib.ThreadLocalRandom

import lila.common.Markdown
import lila.core.config.BaseUrl
import lila.core.id.{ ClasId, ClasInviteId, StudentId }
import lila.core.msg.MsgApi
import lila.db.dsl.{ *, given }
import lila.rating.{ Perf, PerfType, UserPerfs }
import lila.core.user.KidMode

final class ClasApi(
    colls: ClasColls,
    studentCache: ClasStudentCache,
    nameGenerator: NameGenerator,
    userRepo: lila.user.UserRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    msgApi: MsgApi,
    authenticator: lila.core.security.Authenticator,
    baseUrl: BaseUrl
)(using Executor, lila.core.i18n.Translator):

  import BsonHandlers.given

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    colls.clas.update.one($doc("created.by" -> del.id), $set("created.by" -> UserId.ghost), multi = true)
    colls.clas.update.one($doc("teachers" -> del.id), $pull("teachers" -> del.id), multi = true)
    colls.student.delete.one($doc("userId" -> del.id))

  object clas:

    val coll = colls.clas

    def byId(id: ClasId) = coll.byId[Clas](id.value)

    def of(teacher: User): Fu[List[Clas]] =
      coll
        .find($doc("teachers" -> teacher.id))
        .sort($doc("archived" -> 1, "viewedAt" -> -1))
        .cursor[Clas]()
        .list(100)

    def countOf(teacher: User): Fu[Int] =
      coll.countSel($doc("teachers" -> teacher.id))

    def byIds(clasIds: List[ClasId]): Fu[List[Clas]] =
      coll
        .find($inIds(clasIds))
        .sort($sort.desc("createdAt"))
        .cursor[Clas]()
        .listAll()

    def create(data: ClasForm.ClasData, teacher: User): Fu[Clas] =
      val clas = Clas.make(teacher, data.name, data.desc)
      coll.insert.one(clas).inject(clas)

    def update(from: Clas, data: ClasForm.ClasData): Fu[Clas] =
      val clas = data.update(from)
      userRepo.filterEnabled(clas.teachers.toList).flatMap { filtered =>
        val checked = clas.copy(
          teachers = clas.teachers.toList.filter(filtered.contains).toNel | from.teachers
        )
        coll.update.one($id(clas.id), checked).inject(checked)
      }

    def updateWall(clas: Clas, text: Markdown): Funit =
      coll.updateField($id(clas.id), "wall", text).void

    def getAndView(id: ClasId)(using teacher: Me): Fu[Option[Clas]] =
      coll
        .findAndUpdateSimplified[Clas](
          selector = $id(id) ++ $doc("teachers" -> teacher.userId),
          update = $set("viewedAt" -> nowInstant),
          fetchNewObject = true
        )

    def teachers(clas: Clas): Fu[List[User]] =
      userRepo.byOrderedIds(clas.teachers.toList, readPref = _.sec)

    def isTeacherOf(teacher: User, clasId: ClasId): Fu[Boolean] =
      coll.exists($id(clasId) ++ $doc("teachers" -> teacher.id))

    def areKidsInSameClass(kid1: UserId, kid2: UserId): Fu[Boolean] =
      fuccess(studentCache.isStudent(kid1) && studentCache.isStudent(kid2)) >>&
        colls.student.aggregateExists(_.sec): framework =>
          import framework.*
          Match($doc("userId".$in(List(kid1.id, kid2.id)))) -> List(
            GroupField("clasId")("nb" -> SumAll),
            Match($doc("nb" -> 2)),
            Limit(1)
          )

    def isTeacherOf(teacher: UserId, student: UserId): Fu[Boolean] =
      studentCache
        .isStudent(student)
        .so(
          colls.student
            .aggregateExists(_.sec): framework =>
              import framework.*
              Match($doc("userId" -> student)) -> List(
                Project($doc("clasId" -> true)),
                PipelineOperator(
                  $lookup.pipeline(
                    from = colls.clas,
                    as = "clas",
                    local = "clasId",
                    foreign = "_id",
                    pipe = List(
                      $doc("$match" -> $doc("teachers" -> teacher)),
                      $doc("$limit" -> 1),
                      $doc("$project" -> $id(true))
                    )
                  )
                ),
                Match("clas".$ne($arr())),
                Limit(1),
                Project($id(true))
              )
        )

    def archive(c: Clas, t: User, v: Boolean): Funit =
      coll.update
        .one(
          $id(c.id),
          if v then $set("archived" -> Clas.Recorded(t.id, nowInstant))
          else $unset("archived")
        )
        .void

  object student:

    import lila.core.security.ClearPassword

    val coll = colls.student

    def activeOf(clas: Clas): Fu[List[Student]] =
      of($doc("clasId" -> clas.id) ++ selectArchived(false))

    def allWithUsers(clas: Clas, selector: Bdoc = $empty): Fu[List[Student.WithUser]] =
      colls.student
        .aggregateList(Int.MaxValue, _.sec): framework =>
          import framework.*
          Match($doc("clasId" -> clas.id) ++ selector) -> List(
            PipelineOperator(
              $lookup.simple(
                from = userRepo.coll,
                as = "user",
                local = "userId",
                foreign = "_id"
              )
            ),
            UnwindField("user")
          )
        .map: docs =>
          import lila.user.BSONHandlers.userHandler
          for
            doc <- docs
            student <- doc.asOpt[Student]
            user <- doc.getAsOpt[User]("user")
          yield Student.WithUser(student, user)

    def activeWithUsers(clas: Clas): Fu[List[Student.WithUser]] =
      allWithUsers(clas, selectArchived(false))

    def withPerfs(student: Student.WithUser): Fu[Student.WithUserPerfs] =
      perfsRepo.perfsOf(student.user).map(student.withPerfs)

    def withPerfs(students: List[Student.WithUser]): Fu[List[Student.WithUserPerfs]] =
      perfsRepo.idsMap(students.map(_.user.id), _.sec).map { perfs =>
        students.map(s => s.withPerfs(perfs.getOrElse(s.user.id, UserPerfs.default(s.user.id))))
      }

    def withPerf(students: List[Student.WithUser], perfType: PerfType): Fu[List[Student.WithUserPerf]] =
      perfsRepo.idsMap(students.map(_.user.id), perfType, _.sec).map { perfs =>
        students.map: s =>
          Student.WithUserPerf(s.student, s.user, perfs.getOrElse(s.user.id, Perf.default))
      }

    private def of(selector: Bdoc): Fu[List[Student]] =
      coll
        .find(selector)
        .sort($sort.asc("userId"))
        .cursor[Student]()
        .list(500)

    def clasIdsOfUser(userId: UserId): Fu[List[ClasId]] =
      coll.distinctEasy[ClasId, List]("clasId", $doc("userId" -> userId) ++ selectArchived(false))

    def count(clasId: ClasId): Fu[Int] = coll.countSel($doc("clasId" -> clasId))

    def isManaged(user: User): Fu[Boolean] =
      coll.exists($doc("userId" -> user.id, "managed" -> true))

    def release(user: User): Funit =
      coll.updateField($doc("userId" -> user.id, "managed" -> true), "managed", false).void

    def findManaged(user: User): Fu[Option[Student.ManagedInfo]] =
      coll.find($doc("userId" -> user.id, "managed" -> true)).one[Student].flatMapz { student =>
        userRepo
          .byId(student.created.by)
          .zip(clas.byId(student.clasId))
          .map:
            case (Some(teacher), Some(clas)) => Student.ManagedInfo(teacher, clas).some
            case _ => none
      }

    def get(clas: Clas, userId: UserId): Fu[Option[Student]] =
      coll.one[Student]($id(Student.makeId(userId, clas.id)))

    def get(clas: Clas, user: User): Fu[Option[Student.WithUser]] =
      get(clas, user.id).map2 { Student.WithUser(_, user) }

    def withManagingClas(s: Student.WithUserPerfs, clas: Clas): Fu[Student.WithUserAndManagingClas] = {
      if s.student.managed then fuccess(clas.some)
      else
        colls.student
          .aggregateOne(_.sec): framework =>
            import framework.*
            Match($doc("userId" -> s.user.id, "managed" -> true)) -> List(
              PipelineOperator(
                $lookup.simple(
                  from = colls.clas,
                  as = "clas",
                  local = "clasId",
                  foreign = "_id"
                )
              ),
              UnwindField("clas")
            )
          .map:
            _.flatMap(_.getAsOpt[Clas]("clas"))
    }.map { Student.WithUserAndManagingClas(s, _) }

    def update(from: Student, data: ClasForm.StudentData): Fu[Student] =
      val student = data.update(from)
      coll.update.one($id(student.id), student).inject(student)

    def create(
        clas: Clas,
        data: ClasForm.CreateStudent,
        teacher: User
    ): Fu[Student.WithPassword] =
      val email = EmailAddress(s"noreply.class.${clas.id}.${data.username}@lichess.org")
      val password = Student.password.generate
      lila.mon.clas.student.create(teacher.id.value).increment()
      for
        user <- userRepo
          .create(
            name = data.username,
            passwordHash = authenticator.passEnc(password),
            email = email,
            blind = false,
            mobileApiVersion = none,
            mustConfirmEmail = false,
            lang = teacher.lang
          )
          .orFail(s"No user could be created for ${data.username}")
        _ = studentCache.addStudent(user.id)
        student = Student.make(user, clas, teacher.id, data.realName, managed = true)
        _ <- userRepo.setKid(user, KidMode.Yes)
        _ <- perfsRepo.setManagedUserInitialPerfs(user.id)
        _ <- coll.insert.one(student)
        _ <- sendWelcomeMessage(teacher.id, user, clas)
      yield Student.WithPassword(student, password)

    def move(s: Student.WithUser, toClas: Clas)(using teacher: Me): Fu[Option[Student]] = for
      _ <- closeAccount(s)
      stu = s.student.copy(
        id = Student.makeId(s.user.id, toClas.id),
        clasId = toClas.id,
        created = Clas.Recorded(by = teacher.userId, at = nowInstant)
      )
      moved <- colls.student.insert
        .one(stu)
        .inject(stu.some)
        .recoverWith(lila.db.recoverDuplicateKey { _ =>
          student.get(toClas, s.user.id)
        })
    yield moved

    def manyCreate(
        clas: Clas,
        data: ClasForm.ManyNewStudent,
        teacher: User
    )(using Lang): Fu[List[Student.WithPassword]] =
      count(clas.id).flatMap: nbCurrentStudents =>
        data.realNames
          .take(Clas.maxStudents - nbCurrentStudents)
          .sequentially: realName =>
            nameGenerator().flatMap: username =>
              val data = ClasForm.CreateStudent(
                username = username | UserName(ThreadLocalRandom.nextString(10)),
                realName = realName
              )
              create(clas, data, teacher)

    def resetPassword(s: Student): Fu[ClearPassword] =
      val password = Student.password.generate
      authenticator.setPassword(s.userId, password).inject(password)

    def archive(sId: StudentId, v: Boolean)(using me: Me): Fu[Option[Student]] =
      coll
        .findAndUpdateSimplified[Student](
          selector = $id(sId),
          update =
            if v then $set("archived" -> Clas.Recorded(me, nowInstant))
            else $unset("archived"),
          fetchNewObject = true
        )

    def closeAccount(s: Student.WithUser): Funit =
      coll.delete.one($id(s.student.id)).void

    private[ClasApi] def sendWelcomeMessage(teacherId: UserId, student: User, clas: Clas): Funit =
      given Lang = student.realLang | lila.core.i18n.defaultLang
      msgApi
        .post(
          orig = teacherId,
          dest = student.id,
          text = s"""${lila.core.i18n.I18nKey.clas.welcomeToClass.txt(clas.name)}

$baseUrl/class/${clas.id}

${clas.desc}""",
          multi = true
        )
        .void

  object invite:

    import ClasInvite.Feedback.*

    def create(clas: Clas, user: User, realName: String)(using teacher: Me): Fu[ClasInvite.Feedback] =
      student
        .archive(Student.makeId(user.id, clas.id), v = false)
        .map2[ClasInvite.Feedback](_ => Already)
        .getOrElse:
          lila.mon.clas.student.invite(teacher.userId.value).increment()
          val invite = ClasInvite.make(clas, user, realName)
          colls.invite.insert
            .one(invite)
            .void
            .flatMap: _ =>
              sendInviteMessage(teacher, user, clas, invite)
            .recover:
              lila.db.recoverDuplicateKey(_ => Found)

    def get(id: ClasInviteId) = colls.invite.one[ClasInvite]($id(id))

    def view(id: ClasInviteId, user: User): Fu[Option[(ClasInvite, Clas)]] =
      colls.invite.one[ClasInvite]($id(id) ++ $doc("userId" -> user.id)).flatMapz { invite =>
        colls.clas.byId[Clas](invite.clasId.value).map2 { invite -> _ }
      }

    def accept(id: ClasInviteId, user: User): Fu[Option[Student]] =
      colls.invite.one[ClasInvite]($id(id) ++ $doc("userId" -> user.id)).flatMapz { invite =>
        colls.clas.one[Clas]($id(invite.clasId)).flatMapz { clas =>
          studentCache.addStudent(user.id)
          val stu = Student.make(user, clas, invite.created.by, invite.realName, managed = false)
          (colls.student.insert.one(stu) >>
            colls.invite.updateField($id(id), "accepted", true) >>
            student.sendWelcomeMessage(invite.created.by, user, clas))
            .inject(stu.some)
            .recoverWith(lila.db.recoverDuplicateKey { _ =>
              student.get(clas, user.id)
            })
        }
      }

    def decline(id: ClasInviteId): Fu[Option[ClasInvite]] =
      colls.invite
        .findAndUpdateSimplified[ClasInvite](
          selector = $id(id),
          update = $set("accepted" -> false)
        )

    def listPending(clas: Clas): Fu[List[ClasInvite]] =
      colls.invite
        .find($doc("clasId" -> clas.id, "accepted".$ne(true)))
        .sort($sort.desc("created.at"))
        .cursor[ClasInvite]()
        .list(100)

    def delete(id: ClasInviteId): Funit =
      colls.invite.delete.one($id(id)).void

    private def sendInviteMessage(
        teacher: Me,
        student: User,
        clas: Clas,
        invite: ClasInvite
    ): Fu[ClasInvite.Feedback] =
      val url = s"$baseUrl/class/invitation/${invite.id}"
      if student.kid.yes then fuccess(ClasInvite.Feedback.CantMsgKid(url))
      else
        import lila.core.i18n.I18nKey.clas.*
        given play.api.i18n.Lang = student.realLang | lila.core.i18n.defaultLang
        msgApi
          .post(
            orig = teacher.userId,
            dest = student.id,
            text = s"""${invitationToClass.txt(clas.name)}

${clickToViewInvitation.txt()}

$url""",
            multi = true
          )
          .inject(ClasInvite.Feedback.Invited)

  private def selectArchived(v: Boolean) = $doc("archived".$exists(v))
