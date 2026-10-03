package lila.clas

import play.api.i18n.Lang

import lila.core.msg.{ MsgApi, SystemMsg }
import lila.core.config.RouteUrl

private final class ClasMsg(msgApi: MsgApi, routeUrl: RouteUrl)(using Executor, lila.core.i18n.Translator):

  def onArchive(clas: Clas) =
    clas.teachers.toList.sequentiallyVoid: userId =>
      msgApi.systemPost(SystemMsg.standard(userId, autoArchiveMsg(clas)))

  def welcomeMessage(teacherId: UserId, student: User, clas: Clas): Funit =
    given Lang = student.realLang | lila.core.i18n.defaultLang
    msgApi
      .post(
        orig = teacherId,
        dest = student.id,
        text = s"""${lila.core.i18n.I18nKey.clas.welcomeToClass.txt(clas.name)}

${routeUrl(routes.Clas.show(clas.id))}

${clas.desc}""",
        multi = true
      )
      .void

  private def autoArchiveMsg(clas: Clas) =
    s"""The class "${clas.name}" has been automatically archived due to inactivity.

You can re-open it at ${routeUrl(routes.Clas.show(clas.id))}"""

  def invitation(
      teacher: Me,
      student: User,
      clas: Clas,
      invite: ClasInvite
  ): Fu[ClasInvite.Feedback] =
    val url = routeUrl(routes.Clas.invitation(invite.id))
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
