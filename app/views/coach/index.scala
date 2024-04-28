package views.coach

import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }

import scalalib.paginator.Paginator
import lila.i18n.LangList
import lila.core.user.Flag
import lila.user.Flags

object index:

  import trans.coach.*

  def apply(
      pager: Paginator[lila.coach.Coach.WithUser],
      lang: Option[Lang],
      order: lila.coach.CoachPager.Order,
      langCodes: Set[String],
      countries: lila.coach.CountrySelection,
      country: Option[Flag]
  )(using ctx: PageContext) =
    views.base.layout(
      title = lichessCoaches.txt(),
      moreCss = cssTag("coach"),
      modules = infiniteScrollEsmInit,
      withHrefLangs = lila.ui.LangPath(routes.Coach.all(1)).some
    ):
      val langSelections = ("all", "All languages") :: lila.i18n.LangPicker
        .sortFor(LangList.popularNoRegion.filter(l => langCodes(l.code)), ctx.req)
        .map: l =>
          l.code -> LangList.name(l)

      main(cls := "coach-list coach-full-page")(
        st.aside(cls := "coach-list__side coach-side")(
          p(
            areYouCoach(a(href := "https://lichess.org/help/master")(nmOrFideTitle())),
            br,
            if !ctx.me.exists(_.hasTitle) then a(href := routes.Main.verifyTitle)(confirmTitle())
            else sendApplication(a(href := s"mailto:$contactEmailInClear")(contactEmailInClear))
          )
        ),
        div(cls := "coach-list__main coach-main box")(
          boxTop(
            h1(lichessCoaches()),
            div(cls := "box__top__actions")(
              lila.ui.bits.mselect(
                "coach-lang",
                lang.fold("All languages")(LangList.name),
                langSelections.map: (code, name) =>
                  a(
                    href := routes.Coach.search(code, order.key, country.fold("all")(_.code)),
                    cls  := (code == lang.fold("all")(_.code)).option("current")
                  )(name)
              ),
              lila.ui.bits.mselect(
                "coach-country",
                country.fold("All countries")(Flags.name),
                countries.value.map: (code, name) =>
                  a(
                    href := routes.Coach.search(lang.fold("all")(_.code), order.key, code),
                    cls  := (code == country.fold("all")(_.code)).option("current")
                  )(name)
              ),
              lila.ui.bits.mselect(
                "coach-sort",
                order.name,
                lila.coach.CoachPager.Order.list.map: o =>
                  a(
                    href := routes.Coach.search(lang.fold("all")(_.code), o.key, country.fold("all")(_.code)),
                    cls  := (order == o).option("current")
                  )(o.name)
              )
            )
          ),
          div(cls := "list infinite-scroll")(
            pager.currentPageResults.map: c =>
              st.article(cls := "coach-widget paginated", attr("data-dedup") := c.coach.id.value):
                widget(c, link = true)
            ,
            pagerNext(
              pager,
              np =>
                addQueryParam(
                  routes.Coach.search(lang.fold("all")(_.code), order.key, country.fold("all")(_.code)).url,
                  "page",
                  np.toString
                )
            )
          )
        )
      )
