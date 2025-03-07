package lila.mod
package ui

import lila.core.config.NetDomain
import lila.report.Report
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.shutup.PublicSource
import lila.core.i18n.Translate

final class ModInquiryUi(helpers: Helpers)(
    sourceOf: PublicSource => Translate ?=> Tag,
    getPmPresets: Me ?=> ModPresets,
    highlightBad: String => Frag
)(using NetDomain):
  import helpers.{ *, given }

  def apply(in: Inquiry)(using Context, Me) =
    div(id := "inquiry", data("username") := in.user.user.username)(
      i(title := "Costello the Inquiry Octopus", cls := "costello"),
      div(cls := "meat")(
        userLink(in.user.user, withPerfRating = in.user.perfs.some, params = "?mod"),
        div(cls := "docs reports")(
          div(cls := "expendable")(in.allReports.map(renderReport(renderAtomText)))
        ),
        modLog(in.history),
        noteZone(in.user.user, in.notes)
      ),
      links(in),
      div(cls := "actions")(
        Granter(_.ModMessage).option:
          div(cls := "dropper warn buttons")(
            iconTag(Icon.Envelope),
            div:
              getPmPresets.value.map: preset =>
                postForm(action := routes.Mod.warn(in.user.username, preset.name))(
                  submitButton(cls := "fbt", title := preset.text)(preset.name),
                  autoNextInput
                )
          )
        ,
        markButtons(in),
        dropperButtons(in)
      ),
      closeInquiry(in)
    )

  def noteZone(u: User, notes: List[lila.user.Note])(using Context, NetDomain) = div(
    cls := List(
      "dropper counter notes" -> true,
      "empty"                 -> notes.isEmpty
    )
  )(
    span(
      countTag(notes.size),
      "Notes"
    ),
    div(
      postForm(cls := "note", action := s"${routes.User.writeNote(u.username)}?inquiry=1")(
        form3.textarea(lila.user.UserForm.note("text"))(
          placeholder := "Write a mod note"
        ),
        div(cls := "submission")(
          submitButton(cls := "button thin", name := "noteType", value := "mod")("SEND"),
          button(
            cls   := "button thin",
            name  := "noteType",
            value := "copy-url",
            title := "copy current URL to note"
          )("ADD URL"),
          Granter
            .opt(_.Admin)
            .option:
              submitButton(cls := "button thin", name := "noteType", value := "dox"):
                "SEND DOX"
        )
      ),
      notes.map: note =>
        (!note.dox || Granter.opt(_.Admin)).option(
          div(cls := "doc note")(
            h3(
              "by ",
              userIdLink(note.from.some, withOnline = false),
              ", ",
              momentFromNow(note.date)
            ),
            p(richText(note.text))
          )
        )
    )
  )

  private def renderReport(renderAtomText: (Report.Atom, Boolean) => Frag)(r: Report)(using Translate) =
    div(cls := "doc report"):
      r.bestAtoms(10)
        .map: atom =>
          div(cls := "atom")(
            h3(
              lila.report.ui.ReportUi.reportScore(atom.score),
              userIdLink(atom.by.userId.some, withOnline = false, params = "?mod"),
              " for ",
              if r.is(_.Comm)
              then a(href := routes.Mod.communicationPublic(r.user))(strong(atom.reason.name))
              else strong(atom.reason.name),
              " ",
              momentFromNow(atom.at)
            ),
            p(renderAtomText(atom, r.is(_.Comm)))
          )

  private def modLog(history: List[Modlog])(using Context) = Granter
    .opt(_.ModLog)
    .option(
      div(
        cls := List(
          "dropper counter history" -> true,
          "empty"                   -> history.isEmpty
        )
      )(
        span(
          countTag(history.size),
          "Mod log"
        ),
        history.nonEmpty.option(
          div(
            ul(
              history.map: e =>
                li(
                  userIdLink(e.mod.userId.some, withOnline = false),
                  " ",
                  b(e.showAction),
                  " ",
                  e.details,
                  " ",
                  momentFromNow(e.date)
                )
            )
          )
        )
      )
    )

  private def links(in: Inquiry)(using Context, Me) = div(cls := "links")(
    Granter(_.MarkBooster).option:
      val searchUrl = routes.User.games(in.user.username, "search")
      div(cls := "dropper view-games")(
        a(href := routes.GameMod.index(in.user.username))("View", br, "Games"),
        div(cls := "view-games-dropdown")(
          a(
            cls  := "fbt",
            href := s"$searchUrl?turnsMax=5&mode=1&players.loser=${in.user.id}&sort.field=d&sort.order=desc"
          )("Quick rated losses"),
          a(
            cls  := "fbt",
            href := s"$searchUrl?turnsMax=5&mode=1&players.winner=${in.user.id}&sort.field=d&sort.order=desc"
          )("Quick rated wins"),
          boostOpponents(in.report, in.allReports, in.user.user).map { opponents =>
            a(
              cls  := "fbt",
              href := s"${routes.GameMod.index(in.user.id)}?opponents=${opponents.toList.mkString(", ")}"
            )("With these opponents")
          }
        )
      )
    ,
    Granter(_.Shadowban).option:
      a(href := routes.Mod.communicationPublic(in.user.id))(
        "View",
        br,
        "Comms"
      )
    ,
    in.report.isAppeal.option(a(href := routes.Appeal.show(in.user.id))("View", br, "Appeal"))
  )

  private def markButtons(in: Inquiry)(using Context, Me) = frag(
    Granter(_.MarkEngine).option:
      val url = routes.Mod.engine(in.user.username, !in.user.marks.engine).url
      div(cls := "dropper engine buttons")(
        postForm(action := url, cls := "main", title := "Mark as cheat")(
          markButton(in.user.marks.engine)(dataIcon := Icon.Cogs),
          autoNextInput
        ),
        thenForms(url, markButton(false))
      )
    ,
    Granter(_.MarkBooster).option:
      val url = routes.Mod.booster(in.user.username, !in.user.marks.boost).url
      div(cls := "dropper booster buttons")(
        postForm(action := url, cls := "main", title := "Mark as booster or sandbagger")(
          markButton(in.user.marks.boost)(dataIcon := Icon.LineGraph),
          autoNextInput
        ),
        thenForms(url, markButton(false))
      )
    ,
    Granter(_.Shadowban).option:
      val url = routes.Mod.troll(in.user.username, !in.user.marks.troll).url
      div(cls := "dropper shadowban buttons")(
        postForm(
          action := url,
          title  := (if in.user.marks.troll then "Un-shadowban" else "Shadowban"),
          cls    := "main"
        )(
          markButton(in.user.marks.troll)(dataIcon := Icon.BubbleSpeech),
          autoNextInput
        ),
        thenForms(url, markButton(false))
      )
    ,
    Granter(_.CloseAccount).option:
      val url = routes.Mod.alt(in.user.username, !in.user.marks.alt).url
      div(cls := "dropper alt buttons")(
        postForm(action := url, cls := "main", title := "Close alt account")(
          markButton(in.user.marks.alt)(i("A")),
          autoNextInput
        ),
        thenForms(url, markButton(false))
      )
  )

  private def dropperButtons(in: Inquiry)(using Context, Me) =
    div(cls := "dropper more buttons")(
      iconTag(Icon.MoreTriangle),
      div(
        Granter(_.SendToZulip).option:
          val url =
            if in.report.isAppeal then routes.Appeal.sendToZulip(in.user.username)
            else routes.Mod.inquiryToZulip
          postForm(action := url):
            submitButton(cls := "fbt")("Send to Zulip")
        ,
        Granter(_.SendToZulip).option:
          postForm(action := routes.Mod.askUsertableCheck(in.user.username)):
            submitButton(cls := "fbt")("Ask for usertable check")
        ,
        Granter(_.SendToZulip).option:
          postForm(action := routes.Mod.createNameCloseVote(in.user.username)):
            submitButton(cls := "fbt")("Create name-close vote")
        ,
        postForm(action := routes.Report.xfiles(in.report.id))(
          submitButton(cls := List("fbt" -> true, "active" -> (in.report.room.key == "xfiles"))):
            "Move to X-Files"
          ,
          autoNextInput
        ),
        div(cls := "separator"),
        lila.memo.Snooze.Duration.values.map: snooze =>
          postForm(action := snoozeUrl(in.report, snooze.toString))(
            submitButton(cls := "fbt")(s"Snooze ${snooze.name}"),
            autoNextInput
          )
      )
    )

  private def closeInquiry(in: Inquiry)(using Context, Me) =
    div(cls := "actions close")(
      span(cls := "switcher", title := "Automatically open next report")(
        span(cls := "switch")(form3.cmnToggle("auto-next", "auto-next", checked = true))
      ),
      postForm(
        action := routes.Report.process(in.report.id),
        title  := "Dismiss this report as processed. (Hotkey: d)",
        cls    := "process"
      )(
        submitButton(dataIcon := Icon.Checkmark, cls := "fbt"),
        autoNextInput
      ),
      postForm(
        action := routes.Report.inquiry(in.report.id.value),
        title  := "Cancel the inquiry, re-instore the report",
        cls    := "cancel"
      ):
        submitButton(dataIcon := Icon.X, cls := "fbt")(in.alreadyMarked.option(disabled))
    )

  private def autoNextInput = input(cls := "auto-next", tpe := "hidden", name := "next", value := "1")

  private def snoozeUrl(report: Report, duration: String): String =
    if report.isAppeal then routes.Appeal.snooze(report.user, duration).url
    else routes.Report.snooze(report.id, duration).url

  private def boostOpponents(
      report: Report,
      allReports: List[Report],
      reportee: User
  ): Option[NonEmptyList[UserId]] =
    (report.is(_.Boost) || reportee.marks.boost).so:
      allReports
        .filter(_.is(_.Boost))
        .flatMap(_.atoms.toList)
        .withFilter(_.byLichess)
        .flatMap(_.text.linesIterator)
        .collect:
          case farmWithRegex(userId)     => List(userId)
          case sandbagWithRegex(userIds) => userIds.split(' ').toList.map(_.replace("@", ""))
        .flatten
        .flatMap(UserStr.read)
        .flatMap(_.validateId)
        .distinct
        .toNel

  private def renderAtomText(atom: Report.Atom, highlight: Boolean)(using Translate) =
    val (link, text) = atom.parseFlag.match
      case Some(flag) => sourceOf(flag.source).some -> flag.quotes.mkString("\n")
      case None       => None                       -> atom.text
    frag(
      link,
      " ",
      if highlight then highlightBad(text) else frag(text),
      " "
    )

  private val farmWithRegex =
    ("^Boosting: farms rating points from @(" + UserName.historicalRegex.pattern + ")").r.unanchored
  private val sandbagWithRegex =
    "^Sandbagging: throws games to (.+)".r.unanchored

  private def markButton(active: Boolean) = submitButton(
    cls := List("fbt icon" -> true, "active" -> active)
  )

  private def thenForms(url: String, button: Tag) =
    div(
      postForm(
        action := url,
        button("And stay on this report"),
        form3.hidden("next", "0")
      ),
      postForm(
        action := url,
        button("Then open profile"),
        form3.hidden("then", "profile")
      )
    )
