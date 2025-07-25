package lila.report
package ui

import play.api.data.Form

import lila.core.i18n.{ I18nKey as trans, Translate }
import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.core.perf.UserWithPerfs

case class PendingCounts(streamers: Int, appeals: Int, titles: Int)

object ReportUi:

  def reportScore(score: Report.Score): Frag =
    span(cls := s"score ${score.color}")(score.value.toInt)

final class ReportUi(helpers: Helpers)(menu: Context ?=> Frag):
  import helpers.{ given, * }
  import ReportUi.*

  def filterReason(from: Option[String])(reason: Reason): Boolean = from match
    case Some("forum" | "inbox" | "ublog") => reason.isComm
    case _ => true

  def inbox(form: Form[?], user: User, msgs: List[lila.core.msg.IdText])(using ctx: Context) =
    Page(trans.site.reportAUser.txt())
      .css("mod.report.form")
      .js(esmInitBit("embedReasonToggle")):
        main(cls := "page-small box box-pad report")(
          h1(cls := "box__top")(trans.site.reportAUser()),
          postForm(
            cls := "form3",
            action := routes.Report.inboxCreate(user.username)
          )(
            div(cls := "form-group")(aboutReports),
            form3.globalError(form),
            form3.group(form("username"), trans.site.user()): f =>
              frag(userLink(user), form3.hidden(f, user.id.value.some)),
            reasonFormGroup(form, "inbox".some),
            form3.group(form("msgs"), "Messages to report", klass = "report-inbox-msgs"): f =>
              ul:
                msgs.map: msg =>
                  li(
                    form3
                      .nativeCheckbox(
                        msg.id,
                        s"${f.name}[]",
                        checked = false,
                        value = msg.id
                      ),
                    label(`for` := msg.id)(msg.text)
                  )
            ,
            form3.group(form("text"), trans.site.description()):
              form3.textarea(_)(rows := 8, required)
            ,
            form3.actions(
              a(href := routes.Lobby.home)(trans.site.cancel()),
              form3.submit(trans.site.send())
            )
          )
        )

  def form(form: Form[?], reqUser: Option[User] = None, from: Option[String])(using ctx: Context) =
    Page(trans.site.reportAUser.txt())
      .css("mod.report.form")
      .js(esmInitBit("embedReasonToggle")):
        val defaultReason = form("reason").value.orElse(translatedReasonChoices.headOption.map(_._1.key))
        main(cls := "page-small box box-pad report")(
          h1(cls := "box__top")(trans.site.reportAUser()),
          postForm(
            cls := "form3",
            action := s"${routes.Report.create}${reqUser.so(u => "?username=" + u.username)}"
          )(
            div(cls := "form-group")(
              aboutReports,
              ctx.req.queryString
                .contains("postUrl")
                .option(
                  p(
                    "Here for DMCA or Intellectual Property Take Down Notice? ",
                    a(href := "/dmca")("Complete this form instead"),
                    "."
                  )
                )
            ),
            form3.globalError(form),
            form3.group(form("username"), trans.site.user(), klass = "field_to complete-parent"): f =>
              reqUser
                .map: user =>
                  frag(userLink(user), form3.hidden(f, user.id.value.some))
                .getOrElse:
                  div(form3.input(f, klass = "user-autocomplete")(dataTag := "span", autofocus))
            ,
            reasonFormGroup(form, from),
            form3.group(form("text"), trans.site.description(), help = descriptionHelp(~defaultReason).some):
              form3.textarea(_)(rows := 8)
            ,
            form3.actions(
              a(href := routes.Lobby.home)(trans.site.cancel()),
              form3.submit(trans.site.send())
            )
          )
        )

  private def reasonFormGroup(form: Form[?], from: Option[String])(using Context) =
    form3.group(form("reason"), trans.site.reason()): f =>
      form3.select(
        f,
        translatedReasonChoices.collect:
          case (r, t) if filterReason(from)(r) => (r.key, t),
        trans.site.whatIsIheMatter.txt().some
      )

  private val aboutReports = p(
    a(
      href := routes.Cms.lonePage(lila.core.id.CmsPageKey("report-faq")),
      dataIcon := Icon.InfoCircle,
      cls := "text"
    ):
      "Read more about Lichess reports"
  )

  private def descriptionHelp(current: String)(using ctx: Context) = frag:
    import Reason.*
    val maxLength = "Maximum 3000 characters."
    translatedReasonChoices
      .map(_._1)
      .distinct
      .map: reason =>
        span(
          cls := List(s"report-reason report-reason-${reason.key}" -> true, "none" -> (current != reason.key))
        )(
          if reason == Cheat || reason == Boost then trans.site.reportCheatBoostHelp()
          else if reason == Username then trans.site.reportUsernameHelp()
          else
            "Please provide as much information as possible, including relevant game links, posts, and messages."
          ,
          " ",
          trans.site.reportProcessedFasterInEnglish(),
          " ",
          maxLength
        )

  private def translatedReasonChoices(using Translate) =
    import Reason.*
    List(
      (Cheat, trans.site.cheat.txt()),
      (Stall, "Stalling / Leaving Games"),
      (Boost, "Sandbagging / Boosting / Match fixing"),
      (VerbalAbuse, "Verbal abuse / Cursing / Trolling"),
      (Violence, "Violence / Threats"),
      (Harass, "Harassment / Bullying / Stalking"),
      (SelfHarm, "Suicide / Self-Injury"),
      (Hate, "Hate Speech / Sexism"),
      (Spam, "Spamming"),
      (Username, trans.site.username.txt()),
      (Other, trans.site.other.txt())
    )

  def thanks(userId: UserId, blocked: Boolean)(using ctx: Context) =
    val title = "Thanks for the report"
    Page(title)
      .js(esmInitBit("thanksReport")):
        main(cls := "page-small box box-pad")(
          h1(cls := "box__top")(title),
          p("The moderators will review it very soon, and take appropriate action."),
          br,
          br,
          (!blocked).option(
            p(
              "In the meantime, you can block this user: ",
              submitButton(
                attr("data-action") := routes.Relation.block(userId),
                cls := "report-block button",
                st.title := trans.site.block.txt()
              )(span(cls := "text", dataIcon := Icon.NotAllowed)("Block ", titleNameOrId(userId)))
            )
          ),
          br,
          br,
          p(a(href := routes.Lobby.home)("Return to Lichess homepage"))
        )

  object list:

    private val scoreTag = tag("score")

    def layout(filter: String, scores: Room.Scores, pending: PendingCounts)(using Context, Me) =
      Page("Reports")
        .css("mod.report")
        .wrap: body =>
          main(cls := "page-menu")(
            menu,
            div(id := "report_list", cls := "page-menu__content box")(
              div(cls := "header")(
                i(cls := "icon"),
                span(cls := "tabs")(
                  Granter(_.SeeReport).option:
                    a(
                      href := routes.Report.listWithFilter("all"),
                      cls := List("active" -> (filter == "all"))
                    )(
                      "All",
                      scoreTag(scores.highest)
                    )
                  ,
                  Room.values
                    .filter(Room.isGranted)
                    .map { room =>
                      a(
                        href := routes.Report.listWithFilter(room.key),
                        cls := List(
                          "active" -> (filter == room.key),
                          s"room-${room.key}" -> true
                        )
                      )(
                        room.name,
                        scores.get(room).filter(20 <=).map(scoreTag(_))
                      )
                    }
                    .toList,
                  Granter(_.Appeals).option(
                    a(
                      href := routes.Appeal.queue(),
                      cls := List(
                        "new" -> true,
                        "active" -> (filter == "appeal")
                      )
                    )(
                      countTag(pending.appeals),
                      "Appeals"
                    )
                  ),
                  Granter(_.Streamers).option(
                    a(href := s"${routes.Streamer.index()}?requests=1", cls := "new")(
                      countTag(pending.streamers),
                      "Streamers"
                    )
                  ),
                  Granter(_.TitleRequest).option(
                    a(
                      href := routes.TitleVerify.queue,
                      cls := List(
                        "new" -> true,
                        "active" -> (filter == "title")
                      )
                    )(
                      countTag(pending.titles),
                      "Titles"
                    )
                  )
                )
              ),
              body
            )
          )

    def reportTable(reports: List[Report.WithSuspect])(
        bestPerfs: UserWithPerfs => List[Frag],
        userMarks: User => Frag
    )(using Context, Me) =
      table(cls := "slist slist-pad see")(
        thead(
          tr(
            th("Report"),
            th("By"),
            th
          )
        ),
        tbody(
          reports.map {
            case Report.WithSuspect(r, sus, _) if !r.is(_.Comm) || Granter(_.Shadowban) =>
              tr(cls := List("new" -> r.open))(
                td(
                  reportScore(r.score),
                  strong(r.bestAtom.reason.name.capitalize),
                  br,
                  userLink(sus.user, params = "?mod"),
                  br,
                  p(cls := "perfs")(bestPerfs(sus)),
                  userMarks(sus.user)
                ),
                td(cls := "atoms")(
                  r.bestAtoms(3)
                    .map: a =>
                      div(cls := "atom")(
                        span(cls := "head")(
                          reportScore(a.score),
                          " ",
                          strong(a.reason.name.capitalize),
                          " ",
                          userIdLink(a.by.userId.some),
                          " ",
                          momentFromNowOnce(a.at)
                        ),
                        p(
                          cls := List(
                            "text" -> true,
                            "large" -> (a.text.length > 100 || a.text.linesIterator.size > 3)
                          )
                        )(shorten(a.text, 200))
                      ),
                  (r.atoms.size > 3).option(i(cls := "more")("And ", r.atoms.size - 3, " more"))
                ),
                td(
                  r.inquiry match
                    case None =>
                      if r.done.isDefined then
                        postForm(action := routes.Report.inquiry(r.id.value), cls := "reopen")(
                          submitButton(dataIcon := Icon.PlayTriangle, cls := "text button button-metal")(
                            "Reopen"
                          )
                        )
                      else
                        postForm(action := routes.Report.inquiry(r.id.value), cls := "inquiry")(
                          submitButton(dataIcon := Icon.PlayTriangle, cls := "button button-metal")
                        )
                    case Some(inquiry) =>
                      frag(
                        "Open by ",
                        userIdLink(inquiry.mod.some)
                      )
                )
              )
            case _ => emptyFrag
          }
        )
      )
