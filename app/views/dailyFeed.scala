package views.html

import controllers.routes

import java.time.Instant
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.DailyFeed.Update
import play.api.data.Form
import play.api.i18n.Lang

object dailyFeed:

  private def layout(title: String, edit: Boolean = false)(using PageContext) =
    views.html.site.page.layout(
      title = title,
      active = "updates",
      moreCss = cssTag("dailyFeed"),
      moreJs = edit option jsModule("flatpickr")
    )

  def index(updates: List[Update])(using PageContext) =
    layout("Updates"):
      div(cls := "daily-feed box box-pad")(
        boxTop(
          h1("Updates"),
          div(cls := "box__top__actions")(
            isGranted(_.DailyFeed) option a(
              href     := routes.DailyFeed.createForm,
              cls      := "button button-green",
              dataIcon := licon.PlusButton
            ),
            views.html.site.bits.atomLink(routes.DailyFeed.atom)
          )
        ),
        standardFlash,
        updateList(updates, true)
      )

  def updateList(ups: List[Update], editable: Boolean)(using Context) =
    div(cls := "daily-feed__updates", attr("data-rev") := ups.headOption.map(_.rev))(
      ups.collect:
        case up if (isGranted(_.DailyFeed) || up.public && up.instant.isBefore(Instant.now)) =>
          div(cls := "daily-feed__update", id := up.dayString)(
            iconTag(licon.StarOutline),
            div(cls := "daily-feed__update__content")(
              st.section(cls := "daily-feed__update__day")(
                h2(a(href := s"#${up.dayString}")(semanticDate(up.day))),
                editable && isGranted(_.DailyFeed) option frag(
                  a(
                    href     := routes.DailyFeed.edit(up.day),
                    cls      := "button button-green button-empty button-thin text",
                    dataIcon := licon.Pencil
                  ),
                  !up.public option badTag("Draft")
                )
              ),
              div(cls := "daily-feed__update__markup")(rawHtml(up.rendered))
            )
          )
    )

  def create(form: Form[Update])(using PageContext) =
    layout("Updates: Create", true):
      main(cls := "daily-feed page-small box box-pad")(
        boxTop(
          h1(
            a(href := routes.DailyFeed.index)("Updates"),
            " • ",
            "New update!"
          )
        ),
        postForm(cls := "content_box_content form3", action := routes.DailyFeed.create):
          inForm(form)
      )

  def edit(form: Form[Update], update: Update)(using PageContext) =
    layout(s"Updates ${update.day}", true):
      main(cls := "daily-feed page-small")(
        div(cls := "box box-pad")(
          boxTop(
            h1(
              a(href := routes.DailyFeed.index)("Updates"),
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
          updateList(List(update), false),
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

  def atom(ups: List[Update])(using Lang) =
    import views.html.base.atom.{ atomDate, category }
    views.html.base.atom(
      elems = ups,
      htmlCall = routes.DailyFeed.index,
      atomCall = routes.DailyFeed.atom,
      title = "Lichess Updates",
      updated = ups.headOption.map(_.instant)
    ): up =>
      frag(
        tag("id")(up.dayString),
        tag("published")(atomDate(up.instant)),
        link(
          rel  := "alternate",
          tpe  := "text/html",
          href := s"$netBaseUrl${routes.DailyFeed.index}#${up.dayString}"
        ),
        tag("title")(up.title),
        tag("content")(tpe := "html")(up.rendered)
      )
