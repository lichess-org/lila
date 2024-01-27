package views.html
package coach

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.richText

import controllers.routes

object show:

  import trans.coach.*

  private def section(title: Frag, text: Option[RichText]) =
    text.map: t =>
      st.section(
        h2(cls := "coach-show__title")(title),
        div(cls := "content")(richText(t.value))
      )

  def apply(
      c: lila.coach.Coach.WithUser,
      studies: Seq[lila.study.Study.WithChaptersAndLiked],
      posts: Seq[lila.ublog.UblogPost.PreviewPost]
  )(using ctx: PageContext) =
    val profile   = c.coach.profile
    val coachName = s"${c.user.title.so(t => s"$t ")}${c.user.realNameOrUsername}"
    val title     = xCoachesStudents.txt(coachName)
    views.html.base.layout(
      title = title,
      moreCss = cssTag("coach"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = title,
          description = shorten(~(c.coach.profile.headline), 152),
          url = s"$netBaseUrl${routes.Coach.show(c.user.username)}",
          `type` = "profile",
          image = c.coach.picture.isDefined option picture.thumbnail.url(c.coach)
        )
        .some
    ) {
      main(cls := "coach-show coach-full-page")(
        st.aside(cls := "coach-show__side coach-side")(
          a(cls := "button button-empty", href := routes.User.show(c.user.username))(
            viewXProfile(c.user.username)
          ),
          if ctx.me.exists(_ is c.coach) then
            frag(
              if c.coach.listed.value then p("This page is now public.")
              else "This page is not public yet. ",
              a(href := routes.Coach.edit, cls := "text", dataIcon := licon.Pencil)("Edit my coach profile")
            )
          else
            a(
              cls      := "text button button-empty",
              dataIcon := licon.BubbleSpeech,
              href     := s"${routes.Msg.convo(c.user.username)}"
            )(sendPM()),
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
          posts.nonEmpty option st.section(cls := "coach-show__posts")(
            h2(cls := "coach-show__title")(trans.ublog.latestBlogPosts()),
            div(cls := "ublog-post-cards ")(
              posts map { views.html.ublog.post.card(_) }
            )
          ),
          studies.nonEmpty option st.section(cls := "coach-show__studies")(
            h2(cls := "coach-show__title")(publicStudies()),
            div(cls := "studies")(
              studies.map { s =>
                st.article(cls := "study")(study.bits.widget(s, h3))
              }
            )
          ),
          profile.youtubeUrls.nonEmpty option st.section(cls := "coach-show__youtube")(
            h2(cls := "coach-show__title")(
              profile.youtubeChannel.map { url =>
                a(href := url, targetBlank, noFollow)(youtubeVideos())
              } getOrElse youtubeVideos()
            ),
            div(cls := "list")(
              profile.youtubeUrls.map { url =>
                iframe(
                  widthA              := "256",
                  heightA             := "192",
                  src                 := url.value,
                  attr("frameborder") := "0",
                  frame.credentialless,
                  frame.allowfullscreen
                )
              }
            )
          )
        )
      )
    }
