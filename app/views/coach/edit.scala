package views.html.coach

import play.api.data.Form
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.LangList
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.{ richText, safeJsonValue }

import controllers.routes

object edit {

  private val dataTab   = attr("data-tab")
  private val dataValue = attr("data-value")

  private lazy val jsonLanguages = safeJsonValue {
    Json toJson LangList.popularNoRegion.map { l =>
      Json.obj(
        "code"  -> l.code,
        "value" -> LangList.name(l),
        "searchBy" -> List(
          l.toLocale.getDisplayLanguage,
          l.toLocale.getDisplayCountry
        ).mkString(",")
      )
    }
  }

  def apply(c: lila.coach.Coach.WithUser, form: Form[_], reviews: lila.coach.CoachReview.Reviews)(implicit
      ctx: Context
  ) = {
    views.html.account.layout(
      title = s"${c.user.titleUsername} coach page",
      evenMoreCss = frag(cssTag("coach.editor"), cssTag("tagify")),
      evenMoreJs = jsModule("coach.form"),
      active = "coach"
    )(
      div(cls := "account coach-edit box")(
        div(cls := "top")(
          div(cls := "picture_wrap")(
            if (c.coach.hasPicture)
              a(
                cls := "upload_picture",
                href := routes.Coach.picture,
                title := "Change/delete your profile picture"
              )(
                widget.pic(c, 250)
              )
            else
              div(cls := "upload_picture")(
                a(cls := "button", href := routes.Coach.picture)("Upload a profile picture")
              )
          ),
          div(cls := "overview")(
            h1(widget.titleName(c)),
            div(cls := "todo", attr("data-profile") := c.user.profileOrDefault.nonEmptyRealName.isDefined)(
              h3("TODO list before publishing your coach profile"),
              ul
            ),
            div(
              a(
                href := routes.Coach.show(c.user.username),
                cls := "button button-empty text",
                dataIcon := "v"
              )("Preview coach page")
            )
          )
        ),
        postForm(cls := "box__pad form3 async", action := routes.Coach.edit)(
          div(cls := "tabs")(
            div(dataTab := "basics", cls := "active")("Basics"),
            div(dataTab := "texts")("Texts"),
            div(dataTab := "contents")("Contents"),
            div(dataTab := "reviews", dataCount := reviews.list.size, cls := "data-count")(
              "Pending reviews"
            )
          ),
          div(cls := "panels")(
            div(cls := "panel basics active")(
              form3.split(
                form3.checkbox(
                  form("listed"),
                  raw("Publish on the coaches list"),
                  help = raw("Enable when your profile is ready").some,
                  half = true
                ),
                form3.checkbox(
                  form("available"),
                  raw("Currently available for lessons"),
                  help = raw("Enable to get more students").some,
                  half = true
                )
              ),
              form3.group(
                form("profile.headline"),
                raw("Short and inspiring headline"),
                help = raw("Just one sentence to make students want to choose you (3 to 170 chars)").some
              )(form3.input(_)),
              form3.split(
                form3.group(
                  form("languages"),
                  raw("Languages spoken"),
                  help = raw("Which languages can you give lessons in?").some,
                  half = true
                )(
                  form3.input(_)(
                    data("all") := jsonLanguages,
                    data("value") := c.coach.languages.mkString(",")
                  )
                ),
                form3.group(
                  form("profile.hourlyRate"),
                  raw("Hourly rate"),
                  help = raw("Indicative, non-contractual (3 to 140 chars)").some,
                  half = true
                )(form3.input(_))
              )
            ),
            div(cls := "panel texts")(
              form3.group(
                form("profile.description"),
                raw("Who are you?"),
                help = raw("Age, profession, country... let your students know you").some
              )(form3.textarea(_)(rows := 8)),
              form3.group(
                form("profile.playingExperience"),
                raw("Playing experience"),
                help = raw("Tournaments played, best wins, other achievements").some
              )(form3.textarea(_)(rows := 8)),
              form3.group(
                form("profile.teachingExperience"),
                raw("Teaching experience"),
                help = raw("Diplomas, years of practice, best student results").some
              )(form3.textarea(_)(rows := 8)),
              form3.group(
                form("profile.otherExperience"),
                raw("Other experiences"),
                help = raw("E.g. as chess commentator, or teaching other domains").some
              )(form3.textarea(_)(rows := 8)),
              form3.group(form("profile.skills"), raw("Best skills in chess and teaching"))(
                form3.textarea(_)(rows := 8)
              ),
              form3.group(
                form("profile.methodology"),
                raw("Teaching methodology"),
                help = raw("How you prepare and run lessons. How you follow up with students.").some
              )(form3.textarea(_)(rows := 8))
            ),
            div(cls := "panel contents")(
              form3.group(
                form("profile.publicStudies"),
                raw("Featured public Lichess studies"),
                help = raw("Up to 6 Lichess study URLs, one per line").some
              )(form3.textarea(_)()),
              form3.group(form("profile.youtubeChannel"), raw("URL of your Youtube channel"))(form3.input(_)),
              form3.group(
                form("profile.youtubeVideos"),
                raw("Featured youtube videos"),
                help = raw("Up to 6 Youtube video URLs, one per line").some
              )(form3.textarea(_)(rows := 6))
            ),
            div(cls := "panel reviews")(
              p(cls := "help text", dataIcon := "")("Reviews are visible only after you approve them."),
              reviews.list.map { r =>
                div(cls := "review", attr("data-action") := routes.Coach.approveReview(r.id))(
                  div(cls := "user")(
                    userIdLink(r.userId.some),
                    review.barRating(selected = r.score.some, enabled = false),
                    momentFromNow(r.updatedAt)
                  ),
                  div(cls := "content")(
                    r.moddedAt.isDefined option div(cls := "modded")(
                      "Moderators have disapproved this review. Please only accept reviews from ",
                      "actual students, based on actual lessons. Reviews must be about your coaching services.",
                      br,
                      "You may delete this review, or ask the author to rephrase it, then approve it."
                    ),
                    richText(r.text)
                  ),
                  div(cls := "actions btn-rack")(
                    r.moddedAt.fold(true)(_.isBefore(r.updatedAt)) option
                      a(dataValue := "1", cls := "btn-rack__btn yes", dataIcon := "E"),
                    a(dataValue := "0", cls := "btn-rack__btn no", dataIcon := "L")
                  )
                )
              }
            )
          ),
          div(cls := "status text", dataIcon := "E")("Your changes have been saved.")
        )
      )
    )
  }
}
