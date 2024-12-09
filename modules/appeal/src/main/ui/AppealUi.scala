package lila.appeal
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AppealUi(helpers: Helpers):
  import helpers.{ *, given }

  def page(title: String)(using Context) =
    Page(title)
      .css("bits.form3")
      .css("bits.appeal")
      .css(Granter.opt(_.UserModView).option("mod.user"))
      .js(esmInitBit("appeal") ++ Granter.opt(_.UserModView).so(Esm("mod.user")))

  def renderMark(suspect: User)(using ctx: Context) =
    val query = Granter.opt(_.Appeals).so(ctx.req.queryString.toMap)
    if suspect.enabled.no || query.contains("alt") then trans.appeal.closedByModerators()
    else if suspect.marks.engine || query.contains("engine") then trans.appeal.engineMarked()
    else if suspect.marks.boost || query.contains("boost") then trans.appeal.boosterMarked()
    else if suspect.marks.troll || query.contains("shadowban") then trans.appeal.accountMuted()
    else if suspect.marks.rankban || query.contains("rankban") then trans.appeal.excludedFromLeaderboards()
    else if suspect.marks.arenaBan || query.contains("arenaban") then trans.appeal.arenaBanned()
    else if suspect.marks.prizeban || query.contains("prizeban") then trans.appeal.prizeBanned()
    else trans.appeal.cleanAllGood()

  def renderUser(appeal: Appeal, userId: UserId, asMod: Boolean)(using Context) =
    if appeal.isAbout(userId) then userIdLink(userId.some, params = asMod.so("?mod"))
    else
      span(
        userIdLink(UserId.lichess.some),
        Granter.opt(_.Appeals).option(frag(" (", userIdLink(userId.some), ")"))
      )

  def modSection(section: Tag)(ap: Appeal): Frag =
    section(
      strong(cls := "text inline")("Appeal status"),
      strong(cls := "fat")(a(href := routes.Appeal.show(ap.userId))(ap.status.toString))
    )
