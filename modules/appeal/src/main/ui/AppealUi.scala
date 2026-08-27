package lila.appeal
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.config.NetDomain
import lila.core.userId.ModId

case class ModData(
    mod: Me,
    status: UserStatus,
    presets: List[PairOf[String]],
    relatedAppeals: List[Appeal],
    inquiryBy: Option[ModId],
    markedByMe: Boolean,
    otherUsers: Tag
):
  export status.user
  def userAppeals = relatedAppeals.filter(_.user.is(user))
  def muted = userAppeals.exists(_.muted)

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

  def modAppealMessages(appeal: Appeal)(using Context) =
    appeal.msgs.map: msg =>
      div(
        cls := s"appeal__msg appeal__msg--${if appeal.isByMod(msg) then "mod" else "suspect"}",
        id := appeal.isLast(msg).option("appeal-last-msg")
      )(
        div(cls := "appeal__msg__header")(
          renderUser(appeal, msg.by, asMod = true),
          pastMomentServer(msg.at)
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

  def appealIsClosed(appeal: Appeal)(using Translate) = p(cls := "line-center-text")(
    appeal.closedUntil.fold(frag("This appeal is now closed")): until =>
      frag("Appeal paused until ", showDate(until))
  )

  def renderAccountsDisclosure(accounts: AccountsDisclosure) =
    def row(label: String, value: Frag) =
      div(cls := "appeal__accounts__row")(
        span(cls := "appeal__accounts__label")(label),
        div(cls := "appeal__accounts__value")(value)
      )
    div(cls := "appeal__accounts")(
      h3("Declared accounts"),
      row(
        "Additional accounts",
        if accounts.onlyThisAccount then "None (only this account)"
        else
          accounts.otherUsernames.fold(em("None listed")):
            pre(cls := "appeal__accounts__text")(_)
      ),
      accounts.moreForgotten.option:
        row("", "Has additional accounts but has forgotten their usernames")
      ,
      accounts.household.map: household =>
        row("Household accounts", pre(cls := "appeal__accounts__text")(household))
    )

  def markedByMeWarning =
    div(dataIcon := Icon.CautionTriangle, cls := "marked-by-me text"):
      "You have marked this user. Appeal should be handled by another moderator"

  def modHeader(appeal: Appeal, modData: ModData)(using Context) =
    import modData.*
    h1(cls := "box__top")(
      div(cls := "title")(
        backLink,
        span(cls := "appeal-topic")(appeal.topic.key),
        " appeal by ",
        userIdLink(user.some),
        (userAppeals.sizeIs > 1).option:
          a(href := routes.Appeal.modShowAll(user.username), cls := "appeal__all")(
            small(s" (${userAppeals.size} appeals)")
          )
      ),
      div(cls := "actions")(
        a(
          cls := "button button-empty mod-zone-toggle",
          href := routes.User.mod(user.username),
          titleOrText("Mod zone (Hotkey: m)"),
          dataIcon := Icon.Agent
        )
      )
    )

  def modActions(appeal: Appeal, modData: ModData)(using ctx: Context) =
    import modData.*
    div(cls := "appeal__actions")(
      inquiryBy match
        case None =>
          postForm(action := routes.Appeal.modHandle(appeal.user, appeal.topic))(
            submitButton(cls := "button")("Handle this appeal")
          )
        case Some(mod) if ctx.is(mod) =>
          frag(
            postForm(action := routes.Appeal.toggleClosed(appeal.user, appeal.topic, appeal.isOpen))(
              if appeal.isClosed then
                submitButton("Re-open")(
                  cls := "button button-green button-empty"
                )
              else
                submitButton("Close")(
                  title := "Close this appeal",
                  cls := "button button-red button-empty"
                )
            ),
            postForm(action := routes.Appeal.toggleClosed(appeal.user, appeal.topic, true))(
              form3.selectLowLevel("months", AppealForm.untilMonths, default = "Pause".some)
            ),
            postForm(action := routes.Appeal.toggleMute(appeal.user, appeal.topic, !muted))(
              if muted then
                submitButton("Un-mute")(
                  cls := "button button-green button-empty"
                )
              else
                submitButton("Mute")(
                  title := "Mute all appeals of this user",
                  cls := "button button-red button-empty"
                )
            ),
            if appeal.topic == AppealTopic.blog
            then a(href := routes.Ublog.index(user.username), cls := "button button-empty")("View blog")
            else
              AppealTopicApi.unmark(status, appeal.topic) match
                case None =>
                  button(cls := "button button-green button-empty", disabled)("Nothing to un-mark")
                case Some((text, call)) =>
                  val actionUrl = addQueryParam(call.url, "referrer", appeal.modShowUrl)
                  postForm(action := actionUrl):
                    submitButton(cls := "button button-green button-empty yes-no-confirm")(text)
                ,
            appeal.isOpen.option:
              postForm(action := routes.Appeal.toggleRead(appeal.user, appeal.topic, appeal.isUnread))(
                submitButton(cls := "button button-dim button-empty"):
                  if appeal.isUnread then "Set read" else "Set Unread"
              )
          )
        case Some(mod) =>
          button(userIdLink(mod.some), nbsp, "is handling this.")(
            disabled,
            cls := "button button-empty disabled"
          )
      ,
      postForm(
        action := routes.Appeal.sendToZulip(appeal.user, appeal.topic),
        cls := "appeal__actions__zulip"
      )(submitButton(cls := "button button-empty")("Send to Zulip"))
    )
