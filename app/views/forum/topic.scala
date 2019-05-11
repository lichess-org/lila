package views.html
package forum

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object topic {

  def form(categ: lila.forum.Categ, form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    views.html.base.layout(
      title = "New forum topic",
      moreCss = cssTag("forum"),
      moreJs = frag(
        jsTag("forum-post.js"),
        captchaTag
      )
    ) {
        main(cls := "forum forum-topic topic-form page-small box box-pad")(
          h1(
            a(href := routes.ForumCateg.show(categ.slug), dataIcon := "I", cls := "text"),
            categ.name
          ),
          st.section(cls := "warning")(
            h2(dataIcon := "!", cls := "text")("Important"),
            p(
              "Your question may already have an answer ", strong(a(href := routes.Main.faq)("in the F.A.Q."))
            ),
            p(
              "To report a user for cheating or bad behaviour, ", strong(a(href := routes.Report.form)("use the report form"))
            ),
            p(
              "To request support, ", strong(a(href := routes.Main.contact())(raw("try the contact page")))
            )
          ),

          st.form(cls := "form3", action := routes.ForumTopic.create(categ.slug), method := "POST")(
            form3.group(form("name"), trans.subject())(form3.input(_)(autofocus)),
            form3.group(form("post")("text"), trans.message())(form3.textarea(_, klass = "post-text-area")(rows := 10)),
            views.html.base.captcha(form("post"), captcha),
            form3.actions(
              a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
              isGranted(_.PublicMod) option
                form3.submit(frag("Create as mod"), nameValue = (form("post")("modIcon").name, "true").some, icon = "".some),
              form3.submit(trans.createTheTopic())
            )
          )
        )
      }

  def show(
    categ: lila.forum.Categ,
    topic: lila.forum.Topic,
    posts: Paginator[lila.forum.Post],
    formWithCaptcha: Option[FormWithCaptcha],
    unsub: Option[Boolean],
    canModCateg: Boolean
  )(implicit ctx: Context) = views.html.base.layout(
    title = s"${topic.name} • page ${posts.currentPage}/${posts.nbPages} • ${categ.name}",
    moreJs = frag(
      jsTag("forum-post.js"),
      formWithCaptcha.isDefined option captchaTag,
      jsAt("compiled/embed-analyse.js")
    ),
    moreCss = cssTag("forum"),
    openGraph = lila.app.ui.OpenGraph(
      title = topic.name,
      url = s"$netBaseUrl${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage).url}",
      description = shorten(posts.currentPageResults.headOption.??(_.text), 152)
    ).some
  ) {
      val pager = bits.pagination(routes.ForumTopic.show(categ.slug, topic.slug, 1), posts, showPost = true)

      main(cls := "forum forum-topic page-small box box-pad")(
        h1(
          a(
            href := routes.ForumCateg.show(categ.slug),
            dataIcon := "I",
            cls := "text"
          ),
          topic.name
        ),
        pager,
        div(cls := "forum-topic__posts embed_analyse")(
          posts.currentPageResults.map { p =>
            post.show(
              categ,
              topic,
              p,
              s"${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage)}#${p.number}",
              canModCateg = canModCateg
            )
          }
        ),

        div(cls := "forum-topic__actions")(
          if (posts.hasNextPage) emptyFrag
          else if (topic.isOld)
            p("This topic has been archived and can no longer be replied to.")
          else if (formWithCaptcha.isDefined)
            h2(id := "reply")(trans.replyToThisTopic())
          else if (topic.closed) p(trans.thisTopicIsNowClosed())
          else categ.team.filterNot(myTeam).map { teamId =>
            p(
              "Join the ",
              a(href := routes.Team.show(teamId))(teamIdToName(teamId), " team"),
              " to post in this forum"
            )
          } getOrElse p("You can't post in the forums yet. Play some games!"),
          div(
            unsub.map { uns =>
              st.form(cls := s"unsub ${if (uns) "on" else "off"}", method := "post", action := routes.Timeline.unsub(s"forum:${topic.id}"))(
                button(cls := "button button-empty text on", dataIcon := "v", bits.dataUnsub := "off")("Subscribe"),
                button(cls := "button button-empty text off", dataIcon := "v", bits.dataUnsub := "on")("Unsubscribe")
              )
            },

            isGranted(_.ModerateForum) option
              st.form(method := "post", action := routes.ForumTopic.hide(categ.slug, topic.slug))(
                button(cls := "button button-empty button-green")(if (topic.hidden) "Feature" else "Un-feature")
              ),
            canModCateg option
              st.form(method := "post", action := routes.ForumTopic.close(categ.slug, topic.slug))(
                button(cls := "button button-empty button-red")(if (topic.closed) "Reopen" else "Close")
              ),
            canModCateg option
              st.form(method := "post", action := routes.ForumTopic.sticky(categ.slug, topic.slug))(
                button(cls := "button button-empty button-brag")(if (topic.isSticky) "Un-sticky" else "Sticky")
              )
          )
        ),

        formWithCaptcha.map {
          case (form, captcha) => st.form(
            cls := "form3 reply",
            action := s"${routes.ForumPost.create(categ.slug, topic.slug, posts.currentPage)}#reply",
            method := "POST",
            novalidate
          )(
              form3.group(form("text"), trans.message()) { f =>
                form3.textarea(f, klass = "post-text-area")(rows := 10, bits.dataTopic := topic.id)
              },
              views.html.base.captcha(form, captcha),
              form3.actions(
                a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
                isGranted(_.PublicMod) option
                  form3.submit(frag("Reply as mod"), nameValue = (form("modIcon").name, "true").some, icon = "".some),
                form3.submit(trans.reply())
              )
            )
        },

        pager
      )
    }
}
