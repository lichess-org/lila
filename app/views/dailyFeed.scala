package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.DailyFeed.Update
import play.api.data.Form

object dailyFeed:

  private def layout(title: String, edit: Boolean = false)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("dailyFeed"),
      moreJs = edit option jsModule("flatpickr")
    )

  def index(updates: List[Update])(using PageContext) =
    layout("Daily News"):
      main(cls := "daily-feed page-small box box-pad")(
        boxTop(
          h1("Daily News"),
          isGranted(_.DailyFeed) option div(cls := "box__top__actions"):
            a(
              href     := routes.DailyFeed.createForm,
              cls      := "button button-green",
              dataIcon := licon.PlusButton
            )
        ),
        standardFlash,
        updateList(updates)
      )

  private def updateList(ups: List[Update])(using Context) =
    div(cls := "daily-feed__updates")(
      ups.map: update =>
        div(cls := "daily-feed__update")(
          iconTag(licon.StarOutline),
          div(cls := "daily-feed__update__content")(
            div(cls := "daily-feed__update__day")(
              semanticDate(update.day),
              isGranted(_.DailyFeed) option frag(
                a(
                  href     := routes.DailyFeed.edit(update.day),
                  cls      := "button button-green button-empty button-thin text",
                  dataIcon := licon.Pencil
                ),
                !update.public option badTag("Draft")
              )
            ),
            div(cls := "daily-feed__update__markup")(rawHtml(update.rendered))
          )
        )
    )

  def create(form: Form[Update])(using PageContext) =
    layout("Daily News: New", true):
      main(cls := "daily-feed page-small box box-pad")(
        boxTop(
          h1(
            a(href := routes.DailyFeed.index)("Daily Feed"),
            " • ",
            "New update!"
          )
        ),
        postForm(cls := "content_box_content form3", action := routes.DailyFeed.create):
          inForm(form)
      )

  def edit(form: Form[Update], update: Update)(using PageContext) =
    layout(s"Daily News ${update.day}", true):
      main(cls := "daily-feed page-small")(
        div(cls := "box box-pad")(
          boxTop(
            h1(
              a(href := routes.DailyFeed.index)("Daily News"),
              " • ",
              semanticDate(update.day)
            )
          ),
          standardFlash,
          postForm(cls := "content_box_content form3", action := routes.DailyFeed.update(update.day)):
            inForm(form)
        ),
        br,
        div(cls := "box box-pad")(
          updateList(List(update)),
          postForm(action := routes.DailyFeed.delete(update.day))(cls := "daily-feed__delete"):
            submitButton(cls := "button button-red button-empty confirm")("Delete")
        )
      )

  private def inForm(form: Form[Update])(using Context) =
    frag(
      form3.split(
        form3.group(form("day"), frag("Day"), half = true)(
          form3.flatpickr(_, withTime = false, utc = true, minDate = none, dateFormat = "Y-m-d".some)(
            required
          )
        ),
        form3.checkbox(form("public"), raw("Publish"), half = true)
      ),
      form3.group(
        form("content"),
        "Content",
        help = markdownAvailable.some
      )(form3.textarea(_)(rows := 10)),
      form3.action(form3.submit("Save"))
    )
