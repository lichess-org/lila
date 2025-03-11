package lila.opening
package ui

import chess.opening.Opening

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class WikiUi(helpers: Helpers, bits: OpeningBits):
  import helpers.{ *, given }

  def apply(page: OpeningPage)(using Context) =
    div(cls := List("opening__wiki" -> true, "opening__wiki--editor" -> Granter.opt(_.OpeningWiki)))(
      div(cls := "opening__wiki__markup")(
        page.wiki
          .flatMap(_.markupForMove(page.query.sans.lastOption.so(_.value)))
          .fold(
            div(cls := "opening__wiki__markup__placeholder")(
              "No description of the opening, yet. We're working on it!"
            )
          )(rawHtml)
      ),
      (page.query.openingAndExtraMoves._1.isDefined && Granter.opt(_.OpeningWiki)).option {
        details(cls := "opening__wiki__editor")(
          summary(cls := "opening__wiki__editor__summary")("Edit the description", priorityTag(page)),
          page.query.exactOpening match
            case Some(op) =>
              frag(
                postForm(action := routes.Opening.wikiWrite(op.key.value, page.query.pgnUnderscored))(
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
              page.query.openingAndExtraMoves._1.map { canonical =>
                p(
                  br,
                  a(href := bits.openingKeyUrl(canonical.key))(
                    "This is an unnamed variation. Go to the canonical opening page."
                  )
                )
              }
        )
      }
    )

  private val priorityTexts = Vector("Highest", "High", "Average", "Low", "Lowest")
  def priorityTag(page: OpeningPage) =
    val score = page.exploredOption.fold(priorityTexts.size - 1)(OpeningWiki.priorityOf)
    val text  = priorityTexts.lift(score) | priorityTexts.last
    strong(cls := s"priority priority--$score")(text, " priority")
