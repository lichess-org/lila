package lila.clas

import play.api.data.Form

object ClasBulk:
  case class PageData(
      c: Clas,
      otherClasses: List[Clas],
      all: List[Student.WithUser],
      form: Form[ClasBulkForm.ActionData]
  )

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
