package views.html.opening

import chess.opening.Opening
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.opening.{ OpeningPage, OpeningWiki }

object wiki:

  def apply(page: OpeningPage)(using PageContext) =
    div(cls := List("opening__wiki" -> true, "opening__wiki--editor" -> isGranted(_.OpeningWiki)))(
      div(cls := "opening__wiki__markup")(
        page.wiki
          .flatMap(_ markupForMove page.query.sans.lastOption.so(_.value))
          .fold(
            div(cls := "opening__wiki__markup__placeholder")(
              "No description of the opening, yet. We're working on it!"
            )
          )(rawHtml)
      ),
      (page.query.openingAndExtraMoves._1.isDefined && isGranted(_.OpeningWiki)) option {
        details(cls := "opening__wiki__editor")(
          summary(cls := "opening__wiki__editor__summary")("Edit the description", priorityTag(page)),
          page.query.exactOpening match
            case Some(op) =>
              frag(
                postForm(action := routes.Opening.wikiWrite(op.key, page.query.pgnUnderscored))(
                  form3.textarea(
                    OpeningWiki.form
                      .fill(~page.wiki.flatMap(_.revisions.headOption).map(_.text.value))("text")
                  )(),
                  form3.submit("Save and publish")
                ),
                details(cls := "opening__wiki__editor__revisions")(
                  summary("Revision history"),
                  page.wiki.so(_.revisions).map { rev =>
                    div(cls := "opening__wiki__editor__revision")(
                      div(momentFromNowOnce(rev.at), userIdLink(rev.by.some)),
                      textarea(disabled := true)(rev.text)
                    )
                  }
                )
              )
            case None =>
              page.query.openingAndExtraMoves._1 map { canonical =>
                p(
                  br,
                  a(href := bits.keyUrl(canonical.key))(
                    "This is an unnamed variation. Go to the canonical opening page."
                  )
                )
              }
        )
      }
    )

  def showMissing(ops: List[Opening]) = div(cls := "opening__wiki__missing")(
    h2("Openings to explain"),
    p("Sorted by popularity"),
    ul(
      ops map { op =>
        li(a(href := bits.openingUrl(op))(op.name), " ", op.pgn)
      }
    )
  )

  private val priorityTexts = Vector("Highest", "High", "Average", "Low", "Lowest")
  def priorityTag(page: OpeningPage) =
    val score = page.explored.fold(priorityTexts.size - 1)(OpeningWiki.priorityOf)
    val text  = priorityTexts.lift(score) | priorityTexts.last
    strong(cls := s"priority priority--$score")(text, " priority")
