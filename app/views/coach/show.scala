package views.html
package coach

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object show {

  import trans.coach._

  private def section(title: Frag, text: Option[lila.coach.CoachProfile.RichText]) =
    text.map { t =>
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
    val profile   = c.coach.profile
    val coachName = s"${c.user.title.??(t => s"$t ")}${c.user.realNameOrUsername}"
    val title     = xCoachesStudents.txt(coachName)
    views.html.base.layout(
      title = title,
      moreJs = jsModule("coach.show"),
      moreCss = cssTag("coach"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = title,
          description = shorten(~(c.coach.profile.headline), 152),
          url = s"$netBaseUrl${routes.Coach.show(c.user.username)}",
          `type` = "profile",
          image = c.coach.picturePath.map(p => dbImageUrl(p.value))
        )
        .some
    ) {
      main(cls := "coach-show coach-full-page")(
        st.aside(cls := "coach-show__side coach-side")(
          a(cls := "button button-empty", href := routes.User.show(c.user.username))(
            viewXProfile(c.user.username)
          ),
          if (ctx.me.exists(c.coach.is))
            frag(
              if (c.coach.isListed) p("This page is now public.")
              else "This page is not public yet. ",
              a(href := routes.Coach.edit, cls := "text", dataIcon := "")("Edit my coach profile")
            )
          else
            a(
              cls := "text button button-empty",
              dataIcon := "",
              href := s"${routes.Msg.convo(c.user.username)}"
            )(sendPM()),
          ctx.me.exists(_.id != c.user.id) option review.form(c, myReview),
          review.list(coachReviews)
        ),
        div(cls := "coach-show__main coach-main box")(
          div(cls := "coach-widget")(widget(c, link = false)),
          div(cls := "coach-show__sections")(
            section(aboutMe(), profile.description),
            section(playingExperience(), profile.playingExperience),
            section(teachingExperience(), profile.teachingExperience),
            section(otherExperiences(), profile.otherExperience),
            section(bestSkills(), profile.skills),
            section(teachingMethod(), profile.methodology)
          ),
          studies.nonEmpty option st.section(cls := "coach-show__studies")(
            h2(publicStudies()),
            div(cls := "studies")(
              studies.map { s =>
                st.article(cls := "study")(study.bits.widget(s, h3))
              }
            )
          ),
          profile.youtubeUrls.nonEmpty option st.section(cls := "coach-show__youtube")(
            h2(
              profile.youtubeChannel.map { url =>
                a(href := url, targetBlank, noFollow)(youtubeVideos())
              } getOrElse youtubeVideos()
            ),
            div(cls := "list")(
              profile.youtubeUrls.map { url =>
                iframe(
                  width := "256",
                  height := "192",
                  src := url.value,
                  attr("frameborder") := "0",
                  frame.allowfullscreen
                )
              }
            )
          )
        )
      )
    }
  }
}
