package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object show {

  private def section(title: String, text: Option[lila.coach.CoachProfile.RichText]) = text.map { t =>
    st.section(
      h2(title),
      div(cls := "content")(richText(t.value))
    )
  }

  def apply(
    c: lila.coach.Coach.WithUser,
    coachReviews: lila.coach.CoachReview.Reviews,
    studies: Seq[lila.study.Study.WithChaptersAndLiked],
    myReview: Option[lila.coach.CoachReview]
  )(implicit ctx: Context) = {
    val profile = c.coach.profile
    val coachName = s"${c.user.title.??(t => s"$t ")}${c.user.realNameOrUsername}"
    val title = s"$coachName coaches chess students"
    views.html.base.layout(
      title = title,
      moreJs = frag(
        jsAt("vendor/bar-rating/dist/jquery.barrating.min.js"),
        ctx.isAuth option embedJsUnsafe("""$(function() {
$(".bar-rating").barrating();
$('.coach-review-form .toggle').click(function() {
$(this).remove();
$('.coach-review-form form').show();
});
});""")
      ),
      moreCss = cssTag("coach"),
      openGraph = lila.app.ui.OpenGraph(
        title = title,
        description = shorten(~(c.coach.profile.headline), 152),
        url = s"$netBaseUrl${routes.Coach.show(c.user.username)}",
        `type` = "profile",
        image = c.coach.picturePath.map(p => dbImageUrl(p.value))
      ).some
    ) {
        main(cls := "coach-show coach-full-page")(
          st.aside(cls := "coach-show__side coach-side")(
            a(cls := "button button-empty", href := routes.User.show(c.user.username))("View ", c.user.username, " lichess profile"),
            if (ctx.me.exists(c.coach.is)) frag(
              if (c.coach.isListed) p("This page is now public.")
              else "This page is not public yet. ",
              a(href := routes.Coach.edit, cls := "text", dataIcon := "m")("Edit my coach profile")
            )
            else a(cls := "text button button-empty", dataIcon := "c", href := s"${routes.Message.form}?user=${c.user.username}")(
              "Send a private message"
            ),
            ctx.me.exists(_.id != c.user.id) option review.form(c, myReview),
            review.list(c, coachReviews)
          ),
          div(cls := "coach-show__main coach-main box")(
            div(cls := "coach-widget")(widget(c, link = false)),
            div(cls := "coach-show__sections")(
              section("About me", profile.description),
              section("Playing experience", profile.playingExperience),
              section("Teaching experience", profile.teachingExperience),
              section("Other experiences", profile.otherExperience),
              section("Best skills", profile.skills),
              section("Teaching methodology", profile.methodology)
            ),
            studies.nonEmpty option st.section(cls := "coach-show__studies")(
              h2("Public studies"),
              div(cls := "studies")(
                studies.map { s =>
                  st.article(cls := "study")(study.bits.widget(s, h3))
                }
              )
            ),
            profile.youtubeUrls.nonEmpty option st.section(cls := "coach-show__youtube")(
              h2(
                "Youtube videos",
                profile.youtubeChannel.map { url =>
                  frag(
                    " from my ",
                    a(href := url, target := "_blank", rel := "nofollow")("channel")
                  )
                }
              ),
              div(cls := "list")(
                profile.youtubeUrls.map { url =>
                  iframe(width := "256", height := "192", src := url.value, attr("frameborder") := "0", frame.allowfullscreen)
                }
              )
            )
          )
        )
      }
  }
}
