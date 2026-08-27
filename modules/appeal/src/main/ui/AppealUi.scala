package lila.appeal
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain

final class AppealUi(helpers: Helpers)(using NetDomain):
  import helpers.{ *, given }

  def page(title: String)(using Context) =
    Page(title)
      .css("bits.form3")
      .css("bits.appeal")
      .css(Granter.opt(_.Appeals).option("mod.user"))
      .js(esmInit("bits.appeal") ++ Granter.opt(_.Appeals).so(Esm("mod.user")))

  def renderUser(appeal: Appeal, userId: UserId, asMod: Boolean)(using Context) =
    if appeal.user.is(userId) then userIdLink(userId.some, params = asMod.so("?mod"))
    else if userId.is(UserId.lichess) then userIdLink(UserId.lichess.some)
    else
      span(
        userIdLink(UserId.lichess.some),
        Granter.opt(_.Appeals).option(frag(" (", userIdLink(userId.some), ")"))
      )

  def modSection(section: Tag)(ap: Appeal): Frag =
    section(
      strong(cls := "text inline")("Appeal status"),
      strong(cls := "fat")(a(href := ap.modShowUrl)(ap.status.key))
    )

  def backLink =
    a(href := routes.Appeal.modQueue, dataIcon := Icon.LessThan, cls := "text")

  def list(user: User, appeals: List[Appeal])(using Context) =
    val muted = appeals.exists(_.muted)
    page(s"Appeals by ${user.username}"):
      main(cls := "box box-pad appeal")(
        div(cls := "box__top"):
          h1(backLink, "Appeals by ", userIdLink(user.some), muted.option(frag(nbsp, "(muted)")))
        ,
        table(cls := "appeal-list slist")(
          thead(tr(th("Topic"), th("Status"), th("Messages"), th("Mods"), th("Created"), th("Updated"))),
          tbody:
            appeals.map: ap =>
              tr(
                td(a(href := ap.modShowUrl)(strong(ap.topic.key))),
                td:
                  ap.closedUntil.fold[Frag](ap.status.key): until =>
                    frag("paused until ", showDate(until))
                ,
                td(ap.msgs.size.toString),
                td(fragList(ap.modIds.map(some).map(userIdLink(_)))),
                td(momentFromNowOnce(ap.createdAt)),
                td(momentFromNowOnce(ap.updatedAt))
              )
        )
      )

  def userAppealMessages(appeal: Appeal)(using Context) =
    appeal.msgs.map: msg =>
      div(cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}")(
        div(cls := "appeal__msg__header")(
          renderUser(appeal, msg.by, asMod = false),
          momentFromNowOnce(msg.at)
        ),
        div(cls := "appeal__msg__text")(richText(msg.text, expandImg = false))
      )

  def userInactiveAppeals(appeals: List[Appeal])(using Context, Me) =
    appeals
      .sortBy(_.updatedAt)
      .reverse
      .map: appeal =>
        val titleTag = if Granter(_.Appeals) then a(href := appeal.modShowUrl) else span
        div(cls := "box box-pad appeal-closed")(
          div(cls := "box__top")(
            h1(
              span(cls := "appeal-topic")(appeal.topic.key),
              nbsp,
              titleTag:
                if appeal.isClosed then
                  appeal.closedUntil.fold[Frag]("Appeal closed"): until =>
                    frag("Appeal paused until ", showDate(until))
                else "Appeal on hold"
            )
          ),
          userAppealMessages(appeal)
        )
