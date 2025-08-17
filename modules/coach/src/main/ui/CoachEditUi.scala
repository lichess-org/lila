package lila.coach
package ui

import play.api.data.Form
import play.api.libs.json.Json

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class CoachEditUi(helpers: Helpers, ui: CoachUi):
  import helpers.{ *, given }

  private val dataTab = attr("data-tab")

  private lazy val jsonLanguages = safeJsonValue:
    Json.toJson(langList.popularNoRegion.map { l =>
      Json.obj(
        "code" -> l.code,
        "value" -> langList.name(l.toTag),
        "searchBy" -> List(
          l.toLocale.getDisplayLanguage,
          l.toLocale.getDisplayCountry
        ).mkString(",")
      )
    })

  def apply(c: lila.coach.Coach.WithUser, form: Form[?], page: Page)(using
      ctx: Context
  ) =
    page
      .css("bits.coach.editor", "bits.tagify")
      .js(Esm("bits.coachForm")):
        div(cls := "coach-edit box")(
          div(cls := "top")(
            span(
              h1(ui.titleName(c)),
              a(
                href := routes.Coach.show(c.user.username),
                cls := "button button-empty text",
                dataIcon := Icon.Eye
              )("Preview coach page")
            ),
            div(cls := "overview")(
              div(cls := "todo", attr("data-profile") := c.user.profileOrDefault.nonEmptyRealName.isDefined)(
                h3("TODO list before publishing your coach profile"),
                ul
              ),
              form3.fieldset("Picture", toggle = true.some)(
                div(cls := "form-group coach-edit-picture")(
                  ui.thumbnail(c, 250)(attr("draggable") := "true", cls := "drop-target"),
                  div(label("Drag file or"), " ", form3.file.selectImage())
                )
              )
            )
          ),
          postForm(cls := "box__pad form3 async", action := routes.Coach.edit)(
            div(cls := "tabs")(
              div(dataTab := "basics", cls := "active")("Basics"),
              div(dataTab := "texts")("Texts"),
              div(dataTab := "contents")("Contents")
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
                form3.group(form("profile.youtubeChannel"), raw("URL of your Youtube channel"))(
                  form3.input(_)
                ),
                form3.group(
                  form("profile.youtubeVideos"),
                  raw("Featured youtube videos"),
                  help = raw("Up to 6 Youtube video URLs, one per line").some
                )(form3.textarea(_)(rows := 6))
              )
            ),
            div(cls := "status text", dataIcon := Icon.Checkmark)("Your changes have been saved.")
          )
        )
