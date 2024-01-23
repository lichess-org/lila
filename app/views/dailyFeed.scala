package views.html

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.DailyFeed.Update
import play.api.data.Form
import play.api.i18n.Lang
import lila.common.paginator.Paginator
import scalatags.text.Builder
import scalatags.generic.Frag

object dailyFeed:

  private def layout(title: String, edit: Boolean = false)(using PageContext) =
    views.html.site.page.layout(
      title = title,
      active = "news",
      moreCss = cssTag("dailyFeed"),
      moreJs = frag(infiniteScrollTag, edit option jsModule("flatpickr"), edit option jsModule("dailyFeed"))
    )

  def index(ups: Paginator[Update])(using PageContext) =
    layout("Updates"):
      div(cls := "daily-feed box box-pad")(
        boxTop(
          h1("Lichess updates"),
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
        updates(ups, editor = isGranted(_.DailyFeed))
      )

  def updates(ups: Paginator[Update], editor: Boolean)(using Context) =
    div(cls := "daily-feed__updates infinite-scroll")(
      ups.currentPageResults
        .filter(_.published || editor)
        .map: update =>
          div(cls := "daily-feed__update paginated", id := update.id)(
            marker(update.flair),
            div(cls := "daily-feed__update__content")(
              st.section(cls := "daily-feed__update__day")(
                h2(a(href := s"#${update.id}")(absClientInstant(update.at))),
                editor option frag(
                  a(
                    href     := routes.DailyFeed.edit(update.id),
                    cls      := "button button-green button-empty button-thin text",
                    dataIcon := licon.Pencil
                  ),
                  !update.public option badTag(nbsp, "[Draft]"),
                  update.future option goodTag(nbsp, "[Future]")
                )
              ),
              div(cls := "daily-feed__update__markup")(rawHtml(update.rendered))
            )
          ),
      pagerNext(ups, np => routes.DailyFeed.index(np).url)
    )

  val lobbyUpdates = renderCache[List[Update]](1 minute): ups =>
    div(cls := "daily-feed__updates")(
      ups.map: update =>
        div(cls := "daily-feed__update")(
          marker(update.flair),
          div(
            a(cls := "daily-feed__update__day", href := s"/feed#${update.id}"):
              momentFromNow(update.at)
            ,
            rawHtml(update.rendered)
          )
        ),
      div(cls := "daily-feed__update")(
        marker(),
        div:
          a(cls := "daily-feed__update__day", href := "/feed"):
            "All updates »"
      )
    )

  def create(form: Form[?])(using PageContext) =
    layout("Lichess updates: New", true):
      main(cls := "daily-feed page-small box box-pad")(
        boxTop(
          h1(
            a(href := routes.DailyFeed.index(1))("Daily Feed"),
            " • ",
            "New update!"
          )
        ),
        postForm(cls := "content_box_content form3", action := routes.DailyFeed.create):
          inForm(form)
      )

  def edit(form: Form[?], update: Update)(using PageContext) =
    layout(s"Lichess update ${update.id}", true):
      main(cls := "daily-feed page-small")(
        div(cls := "box box-pad")(
          boxTop(
            h1(
              a(href := routes.DailyFeed.index(1))("Lichess update"),
              " • ",
              semanticDate(update.at)
            )
          ),
          standardFlash,
          postForm(cls := "content_box_content form3", action := routes.DailyFeed.update(update.id)):
            inForm(form)
          ,
          postForm(action := routes.DailyFeed.delete(update.id))(cls := "daily-feed__delete"):
            submitButton(cls := "button button-red button-empty confirm")("Delete")
        )
      )

  private def inForm(form: Form[?])(using Context) =
    frag(
      form3.split(
        form3.group(
          form("at"),
          frag("Date"),
          help = raw("Set in the future to schedule an update.").some,
          half = true
        )(form3.flatpickr(_, minDate = none)(required)),
        form3.checkbox(form("public"), raw("Publish"), half = true)
      ),
      form3.group(
        form("content"),
        "Content",
        help = markdownAvailable.some
      )(form3.textarea(_)(rows := 10)),
      form3.group(form("flair"), "Icon", half = false): field =>
        form3
          .flairPicker(field, Flair from form("flair").value, label = frag("Update icon"), anyFlair = true):
            span(cls := "flair-container"):
              Flair.from(form("flair").value).map(f => marker(f.some, "uflair".some))
      ,
      form3.action(form3.submit("Save"))
    )

  private def marker(flair: Option[Flair] = none, customClass: Option[String] = none) =
    img(
      src := flairSrc(flair getOrElse Flair("symbols.white-star")),
      cls := customClass getOrElse s"daily-feed__update__marker ${flair.nonEmpty so " nobg"}"
    )

  def atom(ups: List[Update])(using Lang) =
    import views.html.base.atom.{ atomDate, category }
    views.html.base.atom(
      elems = ups,
      htmlCall = routes.DailyFeed.index(1),
      atomCall = routes.DailyFeed.atom,
      title = "Lichess updates feed",
      updated = ups.headOption.map(_.at)
    ): up =>
      frag(
        tag("id")(up.id),
        tag("published")(atomDate(up.at)),
        link(
          rel  := "alternate",
          tpe  := "text/html",
          href := s"$netBaseUrl${routes.DailyFeed.index(1)}#${up.id}"
        ),
        tag("title")(up.title),
        tag("content")(tpe := "html")(convertToAbsoluteHrefs(up.rendered))
      )

  private def convertToAbsoluteHrefs(html: Html): Html =
    html.map(_.replaceAll(""" href="/""", s""" href="$netBaseUrl/"""))
