package lila.coach
package ui

import play.api.i18n.Lang
import scalalib.paginator.Paginator

import lila.core.config.NetDomain
import lila.core.data.RichText
import lila.core.user.{ Flag, Profile }
import lila.rating.UserPerfsExt.{ best6Perfs, hasEstablishedRating }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class CoachUi(helpers: Helpers)(
    picfitUrl: lila.core.misc.PicfitUrl,
    flagInfo: Profile => Option[Flag],
    flagApi: lila.core.user.FlagApi,
    contactEmail: EmailAddress,
    languages: Set[String] => Context ?=> List[Lang]
)(using NetDomain):
  import helpers.{ *, given }
  import trans.coach as trc

  def titleName(c: Coach.WithUser) =
    frag(c.user.title.map(t => s"$t "), c.user.realNameOrUsername)

  def thumbnailUrl(c: Coach) =
    c.picture match
      case Some(image) => picfitUrl.thumbnail(image, Coach.imageSize, Coach.imageSize)
      case _           => assetUrl("images/placeholder.png")

  def thumbnail(c: Coach.WithUser, cssSize: Int = Coach.imageSize) =
    img(
      widthA  := cssSize,
      heightA := cssSize,
      cls     := "picture",
      src     := thumbnailUrl(c.coach),
      alt     := s"${c.user.titleUsername} Lichess coach picture"
    )

  def widget(c: Coach.WithUser, link: Boolean)(using Context) =
    val profile = c.user.profileOrDefault
    frag(
      link.option(a(cls := "overlay", href := routes.Coach.show(c.user.username))),
      thumbnail(c, if link then 300 else 350),
      div(cls := "overview")(
        (if link then h2 else h1) (cls := "coach-name")(titleName(c)),
        c.coach.profile.headline
          .map: h =>
            p(
              cls := s"headline ${
                  if h.length < 60 then "small" else if h.length < 120 then "medium" else "large"
                }"
            )(h),
        table(
          tbody(
            tr(
              th(trc.location()),
              td(
                profile.nonEmptyLocation.map: l =>
                  span(cls := "location")(l),
                flagInfo(profile).map: c =>
                  frag(
                    span(cls := "flag")(
                      img(src := assetUrl(s"images/flags/${c.code}.png")),
                      " ",
                      c.name
                    )
                  )
              )
            ),
            tr(cls := "languages")(
              th(trc.languages()),
              td(c.coach.languages.map(langList.name).mkString(", "))
            ),
            tr(cls := "rating")(
              th(trc.rating()),
              td(
                profile.fideRating.map { r =>
                  frag("FIDE: ", r)
                },
                a(href := routes.User.show(c.user.username))(
                  c.user.perfs.best6Perfs
                    .filter(c.user.perfs.hasEstablishedRating)
                    .map(showPerfRating(c.user.perfs, _))
                )
              )
            ),
            c.coach.profile.hourlyRate.map: r =>
              tr(cls := "rate")(
                th(trc.hourlyRate()),
                td(r)
              ),
            (!link).option(
              tr(cls := "available")(
                th(trc.availability()),
                td:
                  if c.coach.available.yes
                  then span(cls := "text", dataIcon := Icon.Checkmark)(trc.accepting())
                  else span(cls := "text", dataIcon := Icon.X)(trc.notAccepting())
              )
            ),
            c.user.seenAt.map: seen =>
              tr(cls := "seen")(
                th,
                td(trans.site.lastSeenActive(momentFromNow(seen)))
              )
          )
        )
      )
    )

  private def section(title: Frag, text: Option[RichText]) =
    text.map: t =>
      st.section(
        h2(cls := "coach-show__title")(title),
        div(cls := "content")(richText(t.value))
      )

  def show(c: Coach.WithUser, studies: Seq[Frag], posts: Seq[Frag])(using ctx: Context) =
    val profile   = c.coach.profile
    val coachName = s"${c.user.title.so(t => s"$t ")}${c.user.realNameOrUsername}"
    val title     = trc.xCoachesStudents.txt(coachName)
    Page(title)
      .css("bits.coach")
      .graph(
        OpenGraph(
          title = title,
          description = shorten(~(c.coach.profile.headline), 152),
          url = s"$netBaseUrl${routes.Coach.show(c.user.username)}",
          `type` = "profile",
          image = c.coach.picture.isDefined.option(thumbnailUrl(c.coach))
        )
      ):
        main(cls := "coach-show coach-full-page")(
          st.aside(cls := "coach-show__side coach-side")(
            a(cls := "button button-empty", href := routes.User.show(c.user.username))(
              trc.viewXProfile(c.user.username)
            ),
            if ctx.me.exists(_.is(c.coach)) then
              frag(
                if c.coach.listed.value then p("This page is now public.")
                else "This page is not public yet. ",
                a(href := routes.Coach.edit, cls := "text", dataIcon := Icon.Pencil)("Edit my coach profile")
              )
            else
              a(
                cls      := "text button button-empty",
                dataIcon := Icon.BubbleSpeech,
                href     := s"${routes.Msg.convo(c.user.username)}"
              )(trc.sendPM())
          ),
          div(cls := "coach-show__main coach-main box")(
            div(cls := "coach-widget")(widget(c, link = false)),
            div(cls := "coach-show__sections")(
              section(trc.aboutMe(), profile.description),
              section(trc.playingExperience(), profile.playingExperience),
              section(trc.teachingExperience(), profile.teachingExperience),
              section(trc.otherExperiences(), profile.otherExperience),
              section(trc.bestSkills(), profile.skills),
              section(trc.teachingMethod(), profile.methodology)
            ),
            posts.nonEmpty.option:
              st.section(cls := "coach-show__posts")(
                h2(cls := "coach-show__title")(trans.ublog.latestBlogPosts()),
                div(cls := "ublog-post-cards ")(posts)
              )
            ,
            studies.nonEmpty.option:
              st.section(cls := "coach-show__studies")(
                h2(cls := "coach-show__title")(trans.coach.publicStudies()),
                div(cls := "studies")(studies)
              )
            ,
            profile.youtubeUrls.nonEmpty.option(
              st.section(cls := "coach-show__youtube")(
                h2(cls := "coach-show__title")(
                  profile.youtubeChannel
                    .map { url =>
                      a(href := url, targetBlank, noFollow)(trc.youtubeVideos())
                    }
                    .getOrElse(trc.youtubeVideos())
                ),
                div(cls := "list")(
                  profile.youtubeUrls.map { url =>
                    iframe(
                      widthA         := "256",
                      heightA        := "192",
                      src            := url.value,
                      st.frameborder := "0",
                      frame.credentialless,
                      frame.allowfullscreen
                    )
                  }
                )
              )
            )
          )
        )

  def index(
      pager: Paginator[Coach.WithUser],
      lang: Option[Lang],
      order: CoachPager.Order,
      langCodes: Set[String],
      countries: CountrySelection,
      country: Option[Flag]
  )(using ctx: Context) =
    Page(trc.lichessCoaches.txt())
      .css("bits.coach")
      .js(infiniteScrollEsmInit)
      .hrefLangs(lila.ui.LangPath(routes.Coach.all(1))):
        val langSelections = ("all", trans.site.allLanguages.txt()) :: languages(langCodes).map: l =>
          l.code -> langList.name(l)
        main(cls := "coach-list coach-full-page")(
          st.aside(cls := "coach-list__side coach-side")(
            p(
              trc.areYouCoach(trc.nmOrFideTitle()),
              br,
              if !ctx.me.exists(_.hasTitle) then a(href := routes.TitleVerify.index)(trc.confirmTitle())
              else trc.sendApplication(a(href := s"mailto:${contactEmail.value}")(contactEmail.value))
            )
          ),
          div(cls := "coach-list__main coach-main box")(
            boxTop(
              h1(trc.lichessCoaches()),
              div(cls := "box__top__actions")(
                lila.ui.bits.mselect(
                  "coach-lang",
                  lang.fold(trans.site.allLanguages.txt())(langList.name),
                  langSelections.map: (code, name) =>
                    a(
                      href := routes.Coach.search(code, order.key, country.fold("all")(_.code)),
                      cls  := (code == lang.fold("all")(_.code)).option("current")
                    )(name)
                ),
                lila.ui.bits.mselect(
                  "coach-country",
                  country.fold(trans.coach.allCountries.txt())(flagApi.name),
                  (("all", trans.coach.allCountries.txt()) :: countries.value).map: (code, name) =>
                    a(
                      href := routes.Coach.search(lang.fold("all")(_.code), order.key, code),
                      cls  := (code == country.fold("all")(_.code)).option("current")
                    )(name)
                ),
                lila.ui.bits.mselect(
                  "coach-sort",
                  order.i18nKey(),
                  CoachPager.Order.list.map: o =>
                    a(
                      href := routes.Coach
                        .search(lang.fold("all")(_.code), o.key, country.fold("all")(_.code)),
                      cls := (order == o).option("current")
                    )(o.i18nKey())
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
