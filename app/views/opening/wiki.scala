package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ OpeningPage, OpeningWiki }

object wiki {

  def apply(page: OpeningPage)(implicit ctx: Context) = div(cls := "opening__wiki")(
    page.wiki
      .flatMap(_.markup)
      .fold(frag("No description of the opening, yet.")) { markup =>
        div(cls := "opening__wiki__markup")(raw(markup))
      },
    (page.query.openingAndExtraMoves._1.isDefined && isGranted(_.OpeningWiki)) option {
      details(cls := "opening__wiki__editor")(
        summary(cls := "opening__wiki__editor__summary")("Edit the description"),
        page.query.opening match {
          case Some(op) =>
            frag(
              postForm(action := routes.Opening.wikiWrite(op.key))(
                form3.textarea(
                  OpeningWiki.form
                    .fill(~page.wiki.flatMap(_.revisions.headOption).map(_.text.value))("text")
                )(),
                form3.submit("Save and publish")
              ),
              details(cls := "opening__wiki__editor__revisions")(
                summary("Revision history"),
                page.wiki.??(_.revisions).map { rev =>
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
                a(href := routes.Opening.query(canonical.key))(
                  "This is an unnamed variation. Go to the canonical opening page."
                )
              )
            }
        }
      )
    }
  )
}
