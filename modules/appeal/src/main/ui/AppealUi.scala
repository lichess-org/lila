package lila.appeal
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AppealUi(helpers: Helpers):
  import helpers.{ *, given }

  def page(title: String)(using Context) =
    Page(title)
      .css("bits.form3")
      .css("bits.appeal")
      .css(Granter.opt(_.Appeals).option("mod.user"))
      .js(esmInit("bits.appeal") ++ Granter.opt(_.Appeals).so(Esm("mod.user")))

  def renderUser(appeal: Appeal, userId: UserId, asMod: Boolean)(using Context) =
    if appeal.user.is(userId) then userIdLink(userId.some, params = asMod.so("?mod"))
    else
      span(
        userIdLink(UserId.lichess.some),
        Granter.opt(_.Appeals).option(frag(" (", userIdLink(userId.some), ")"))
      )

  def modSection(section: Tag)(ap: Appeal): Frag =
    section(
      strong(cls := "text inline")("Appeal status"),
      strong(cls := "fat")(a(href := routes.Appeal.modShow(ap.user, ap.topic))(ap.status.toString))
    )

  def list(user: User, appeals: List[Appeal])(using Context) =
    page(s"Appeals by ${user.username}"):
      main(cls := "box box-pad appeal")(
        div(cls := "box__top")(h1("Appeals by ", userIdLink(user.some))),
        table(cls := "appeal-list slist")(
          thead(tr(th("Topic"), th("Status"), th("Messages"), th("Mods"), th("Created"), th("Updated"))),
          tbody:
            appeals.map: ap =>
              tr(
                td(a(href := routes.Appeal.modShow(ap.user, ap.topic))(strong(ap.topic.key))),
                td(ap.status.key),
                td(ap.msgs.size.toString),
                td(fragList(ap.modIds.map(some).map(userIdLink(_)))),
                td(momentFromNowOnce(ap.createdAt)),
                td(momentFromNowOnce(ap.updatedAt))
              )
        )
      )
