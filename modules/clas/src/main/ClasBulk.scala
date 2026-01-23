package lila.clas

import play.api.data.Form
import lila.clas.ClasBulk.PostResponse
import lila.core.id.ClasId

object ClasBulk:
  case class PageData(
      c: Clas,
      otherClasses: List[Clas],
      all: List[Student.WithUser],
      form: Form[ClasBulkForm.ActionData]
  )
  enum PostResponse:
    case Done
    case Fail
    case CloseAccounts(users: List[User])

object ClasBulkForm:

  import play.api.data.Forms.*

  val form = Form(
    mapping(
      "activeStudents" -> text,
      "archivedStudents" -> text,
      "invites" -> text,
      "action" -> nonEmptyText
    )(ActionData.apply)(unapply)
  )

  def filled(students: List[Student.WithUser], invites: List[ClasInvite]) =
    val (activeStudents, archivedStudents) = students.partition(_.student.isActive)
    form.fill(
      ActionData(
        activeStudents
          .map(s => s"${s.student.userId} ${s.student.realName}")
          .mkString("\n"),
        archivedStudents
          .map(s => s"${s.student.userId} ${s.student.realName}")
          .mkString("\n"),
        invites.map(i => s"${i.userId} ${i.realName}").mkString("\n"),
        ""
      )
    )

  case class ActionData(
      activeStudents: String,
      archivedStudents: String,
      invites: String,
      action: String
  ):
    def activeUserIds = readUserIds(activeStudents)
    def archivedUserIds = readUserIds(archivedStudents)
    def invitesUserIds = readUserIds(invites)

    private def readUserIds(str: String) =
      str.linesIterator
        .map(_.trim.takeWhile(!_.isWhitespace))
        .flatMap(UserStr.read)
        .map(_.id)
        .distinct
        .toList

final class ClasBulkApi(api: ClasApi)(using Executor):

  def load(clas: Clas)(using me: Me): Fu[ClasBulk.PageData] =
    for
      students <- api.student.allWithUsers(clas)
      invites <- api.invite.listPending(clas)
      classes <- api.clas.of(me)
      otherClasses = classes.filter(_.id != clas.id).filter(_.isActive)
    yield ClasBulk.PageData(
      clas,
      otherClasses,
      students,
      ClasBulkForm.filled(students, invites)
    )

  def post(clas: Clas, data: ClasBulkForm.ActionData)(using me: Me): Fu[PostResponse] =
    val moveTo = """move-to-(.+)""".r
    def studentId(id: UserId) = Student.makeId(id, clas.id)
    data.pp.action match
      case "archive" =>
        for _ <- api.student.archiveMany(clas, data.activeUserIds.map(studentId), true)
        yield PostResponse.Done
      case "restore" =>
        for _ <- api.student.archiveMany(clas, data.archivedUserIds.map(studentId), false)
        yield PostResponse.Done
      case moveTo(to) =>
        api.clas
          .getAndView(ClasId(to))
          .flatMap:
            case None => fuccess(PostResponse.Fail)
            case Some(toClas) =>
              val studentIdsSet = data.activeUserIds.toSet
              for
                students <- api.student.allWithUsers(clas)
                selected = students.filter(s => studentIdsSet.contains(s.user.id))
                _ <- selected.traverse(api.student.move(clas, _, toClas))
              yield PostResponse.Done
      case "remove" =>
        val studentIdsSet = data.archivedUserIds.toSet
        for
          students <- api.student.allWithUsers(clas)
          selected = students.filter(s => studentIdsSet.contains(s.user.id))
          closeUsers <- selected.sequentially(closeStudent(clas, _))
        yield PostResponse.CloseAccounts(closeUsers.flatten)
      case "delete-invites" =>
        api.invite.deleteInvites(clas.id, data.invitesUserIds).inject(PostResponse.Done)
      case _ => fuccess(PostResponse.Fail)

  private def closeStudent(clas: Clas, s: Student.WithUser)(using Me): Fu[Option[User]] =
    if s.student.managed then api.student.deleteStudent(clas, s).inject(s.user.some)
    else if s.student.isArchived then api.student.deleteStudent(clas, s).inject(none)
    else fuccess(none)
