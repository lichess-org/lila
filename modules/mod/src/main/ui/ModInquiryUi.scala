package lila.mod
package ui

import lila.core.config.NetDomain
import lila.report.{ Reason, Report }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ModInquiryUi(helpers: Helpers):
  import helpers.{ *, given }

  def autoNextInput = input(cls := "auto-next", tpe := "hidden", name := "next", value := "1")

  def markButton(active: Boolean) = submitButton(
    cls := List("fbt icon" -> true, "active" -> active)
  )

  def renderReport(renderAtomText: (String, Boolean) => Frag)(r: Report)(using Translate) =
    div(cls := "doc report")(
      r.bestAtoms(10).map { atom =>
        div(cls := "atom")(
          h3(
            lila.report.ui.ReportUi.reportScore(atom.score),
            userIdLink(atom.by.userId.some, withOnline = false),
            " for ",
            if r.isComm
            then a(href := routes.Mod.communicationPublic(r.user))(strong(r.reason.name))
            else strong(r.reason.name),
            " ",
            momentFromNow(atom.at)
          ),
          p(renderAtomText(atom.simplifiedText, r.isComm))
        )
      }
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
          Granter
            .opt(_.Admin)
            .option(
              submitButton(cls := "button thin", name := "noteType", value := "dox")(
                "SEND DOX"
              )
            )
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

  def modLog(history: List[Modlog])(using Context) = Granter
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

  def boostOpponents(
      report: Report,
      allReports: List[Report],
      reportee: User
  ): Option[NonEmptyList[UserId]] =
    (report.reason == Reason.Boost || reportee.marks.boost).so {
      allReports
        .filter(_.reason == Reason.Boost)
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
    }

  private val farmWithRegex =
    ("^Boosting: farms rating points from @(" + UserName.historicalRegex.pattern + ")").r.unanchored
  private val sandbagWithRegex =
    "^Sandbagging: throws games to (.+)".r.unanchored

  def thenForms(url: String, button: Tag) =
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
