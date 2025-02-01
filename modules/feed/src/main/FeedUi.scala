package lila.feed
package ui

import play.api.data.Form
import scalalib.paginator.Paginator

import lila.ui.{ *, given }

import ScalatagsTemplate.{ *, given }

final class FeedUi(helpers: Helpers, atomUi: AtomUi)(
    sitePage: String => Context ?=> Page
)(using Executor):
  import helpers.{ *, given }

  private def renderCache[A](ttl: FiniteDuration)(toFrag: A => Frag): A => Frag =
    val cache = lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterWrite(ttl)
      .build[A, String]()
    from => raw(cache.get(from, from => toFrag(from).render))

  private def page(title: String, edit: Boolean = false)(using Context): Page =
    sitePage(title)
      .css("bits.dailyFeed")
      .js(infiniteScrollEsmInit)
      .js(edit.option(Esm("bits.flatpickr")))
      .js(edit.option(esmInitBit("dailyFeed")))

  def index(ups: Paginator[Feed.Update])(using Context) =
    page("Updates"):
      div(cls := "daily-feed box box-pad")(
        boxTop(
          h1("Lichess updates"),
          div(cls := "box__top__actions")(
            Granter
              .opt(_.Feed)
              .option(
                a(
                  href     := routes.Feed.createForm,
                  cls      := "button button-green",
                  dataIcon := Icon.PlusButton
                )
              ),
            atomUi.atomLink(routes.Feed.atom)
          )
        ),
        standardFlash,
        updates(ups, editor = Granter.opt(_.Feed))
      )

  val lobbyUpdates = renderCache[List[Feed.Update]](1.minute): ups =>
    div(cls := "daily-feed__updates")(
      ups.map: update =>
        div(cls := "daily-feed__update")(
          marker(update.flair),
          div(
            a(cls := "daily-feed__update__day", href := s"${routes.Feed.index(1)}#${update.id}"):
              momentFromNow(update.at)
            ,
            rawHtml(update.rendered)
          )
        ),
      div(cls := "daily-feed__update")(
        marker(),
        div:
          a(cls := "daily-feed__update__day", href := routes.Feed.index(1)):
            "All updates »"
      )
    )

  def create(form: Form[?])(using Context) =
    page("Lichess updates: New", true):
      main(cls := "daily-feed page-small box box-pad")(
        boxTop(
          h1(
            a(href := routes.Feed.index(1))("Daily Feed"),
            " • ",
            "New update!"
          )
        ),
        postForm(cls := "form3", action := routes.Feed.create):
          inForm(form)
      )

  def edit(form: Form[?], update: Feed.Update)(using Context) =
    page(s"Lichess update ${update.id}", true):
      main(cls := "daily-feed page-small")(
        div(cls := "box box-pad")(
          boxTop(
            h1(
              a(href := routes.Feed.index(1))("Lichess update"),
              " • ",
              semanticDate(update.at)
            )
          ),
          standardFlash,
          postForm(cls := "form3", action := routes.Feed.update(update.id)):
            inForm(form)
          ,
          postForm(action := routes.Feed.delete(update.id))(cls := "daily-feed__delete"):
            submitButton(cls := "button button-red button-empty yes-no-confirm")("Delete")
        )
      )

  def atom(ups: List[Feed.Update])(using Translate) =
    atomUi.feed(
      elems = ups,
      htmlCall = routes.Feed.index(1),
      atomCall = routes.Feed.atom,
      title = "Lichess updates feed",
      updated = ups.headOption.map(_.at)
    ): up =>
      val url = s"$netBaseUrl${routes.Feed.index(1)}#${up.id}"
      frag(
        tag("id")(url),
        tag("author")(tag("name")("Lichess")),
        tag("published")(atomUi.atomDate(up.at)),
        tag("updated")(atomUi.atomDate(up.at)),
        link(
          rel  := "alternate",
          tpe  := "text/html",
          href := url
        ),
        tag("title")(up.title),
        tag("content")(tpe := "html")(convertToAbsoluteHrefs(up.rendered))
      )

  private def convertToAbsoluteHrefs(html: Html): Html =
    html.map(_.replaceAll(""" href="/""", s""" href="$netBaseUrl/"""))

  private def updates(ups: Paginator[Feed.Update], editor: Boolean)(using Context) =
    div(cls := "daily-feed__updates infinite-scroll")(
      ups.currentPageResults
        .filter(_.published || editor)
        .map: update =>
          div(cls := "daily-feed__update paginated", id := update.id)(
            marker(update.flair),
            div(cls := "daily-feed__update__content")(
              st.section(cls := "daily-feed__update__day")(
                h2(a(href := s"#${update.id}")(absClientInstant(update.at))),
                editor.option(
                  frag(
                    a(
                      href     := routes.Feed.edit(update.id),
                      cls      := "button button-green button-empty button-thin text",
                      dataIcon := Icon.Pencil
                    ),
                    (!update.public).option(badTag(nbsp, "[Draft]")),
                    update.future.option(goodTag(nbsp, "[Future]"))
                  )
                )
              ),
              div(cls := "daily-feed__update__markup")(rawHtml(update.rendered))
            )
          ),
      pagerNext(ups, np => routes.Feed.index(np).url)
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
          .flairPicker(field, Flair.from(form("flair").value), anyFlair = true),
      form3.action(form3.submit("Save"))
    )

  private def marker(flair: Option[Flair] = none, customClass: Option[String] = none) =
    img(
      src := flairSrc(flair.getOrElse(Flair("symbols.white-star"))),
      cls := customClass.getOrElse(s"daily-feed__update__marker ${flair.nonEmpty.so(" nobg")}")
    )
