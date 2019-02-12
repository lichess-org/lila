package views.html
package forum

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.String.html.richText

import controllers.routes

object topic {

  def form(categ: lila.forum.Categ, form: Form[_], captcha: lila.common.Captcha)(implicit ctx: Context) =
    bits.layout(title = "New forum topic", moreJs = jsTag("forum-post.js")) {
      main(cls := "forum forum-topic topic-form box")(
        h1(
          a(
            href := routes.ForumCateg.show(categ.slug),
            dataIcon := "I",
            cls := "text"
          ),
          categ.name
        ),
        div(cls := "warning")(
          h2(dataIcon := "!", cls := "text")("Important"),
          p(
            "To report a user for cheating or bad behaviour,",
            br,
            strong(a(href := routes.Report.form)("use the report form"))
          ),
          p(
            "To ask a question about lichess or chess rules,",
            br,
            strong(a(href := routes.QaQuestion.index())(raw("use the Question &amp; Answers site")))
          )
        ),

        st.form(cls := "form3", action := routes.ForumTopic.create(categ.slug), method := "POST", novalidate := true)(
          form3.group(form("name"), trans.subject.frag())(form3.input(_)(autofocus := true)),
          form3.group(form("post")("text"), trans.message.frag())(form3.textarea(_, klass = "post-text-area")(rows := 10)),
          views.html.base.captcha(form("post"), captcha),
          form3.actions(
            a(href := routes.ForumCateg.show(categ.slug))(trans.cancel.frag()),
            isGranted(_.PublicMod) option
              form3.submit(frag("Create as mod"), nameValue = (form("post")("modIcon").name, "true").some, icon = "".some),
            form3.submit(trans.createTheTopic.frag())
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
  )(implicit ctx: Context) = bits.layout(
    title = s"${topic.name} • page ${posts.currentPage}/${posts.nbPages} • ${categ.name}",
    moreJs = frag(
      jsTag("forum-post.js"),
      jsTag("embed-analyse.js")
    ),
    openGraph = lila.app.ui.OpenGraph(
      title = topic.name,
      url = s"$netBaseUrl${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage).url}",
      description = shorten(posts.currentPageResults.headOption.??(_.text), 152)
    ).some
  ) {

      val bar = div(cls := "bar")(
        bits.pagination(routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage), posts, showPost = false)
      )
      main(cls := "forum forum-topic box box-pad")(
        h1(
          a(
            href := routes.ForumCateg.show(categ.slug),
            dataIcon := "I",
            cls := "text"
          ),
          topic.name
        ),
        bar,
        div(cls := "forum-topic__posts embed_analyse")(
          posts.currentPageResults.map { post =>
            val url = s"${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage)}#${post.number}"
            st.article(cls := List("forum-post" -> true, "erased" -> post.erased), id := post.number)(
              div(cls := "forum-post__metas")(
                div(
                  authorLink(post = post, cssClass = "author".some, modIcon = post.displayModIcon),
                  a(href := url)(
                    post.updatedAt.map { updatedAt =>
                      frag(
                        span(cls := "post-edited")("edited "),
                        momentFromNow(updatedAt)
                      )
                    }.getOrElse {
                      momentFromNow(post.createdAt)
                    }
                  ),
                  isGranted(_.IpBan) option span(cls := "mod postip")(post.ip),
                  ctx.userId.fold(false)(post.shouldShowEditForm(_)) option
                    a(cls := "mod edit button button-empty text", dataIcon := "m")("Edit"),
                  canModCateg option a(
                    cls := "mod delete button button-empty button-red",
                    href := routes.ForumPost.delete(categ.slug, post.id),
                    dataIcon := "q"
                  )
                ),
                a(cls := "anchor", href := url)(s"#${post.number}")
              ),
              p(cls := "forum-post__message")(
                if (post.erased) lila.common.String.erasedHtml
                else richText(post.text)
              ),
              ctx.userId.fold(false)(post.shouldShowEditForm(_)) option
                st.form(cls := "edit-post-form", method := "post", action := routes.ForumPost.edit(post.id))(
                  textarea(
                    dataTopic := topic.id,
                    name := "changes",
                    cls := "post-text-area edit-post-box",
                    minlength := 3,
                    required := true
                  )(post.text),
                  div(cls := "edit-buttons")(
                    a(cls := "edit-post-cancel", href := routes.ForumPost.redirect(post.id), style := "margin-left:20px")(
                      trans.cancel.frag()
                    ),
                    button(`type` := "submit", cls := "button")(trans.apply.frag())
                  )
                )
            )
          }
        ),
        unsub.map { uns =>
          st.form(cls := s"mod unsub ${if (uns) "on" else "off"}", method := "post", action := routes.Timeline.unsub(s"forum:${topic.id}"))(
            button(cls := "button text on", dataIcon := "v", dataUnsub := "off")("Subscribe"),
            button(cls := "button text off", dataIcon := "v", dataUnsub := "on")("Unsubscribe")
          )
        },

        isGranted(_.ModerateForum) option
          st.form(cls := "mod", method := "post", action := routes.ForumTopic.hide(categ.slug, topic.slug))(
            button(cls := "button button-green")(if (topic.hidden) "Feature" else "Un-feature")
          ),
        canModCateg option
          st.form(cls := "mod", method := "post", action := routes.ForumTopic.close(categ.slug, topic.slug))(
            button(cls := "button button-red")(if (topic.closed) "Reopen" else "Close")
          ),
        canModCateg option
          st.form(cls := "mod", method := "post", action := routes.ForumTopic.sticky(categ.slug, topic.slug))(
            button(cls := "button button-brag")(if (topic.closed) "Un-sticky" else "Sticky")
          ),

        div(cls := "topicReply")(
          if (topic.isOld) p("This topic has been archived and can no longer be replied to.")
          else formWithCaptcha.map {
            case (form, captcha) => frag(
              h2(cls := "postNewTitle", id := "reply")(trans.replyToThisTopic.frag()),
              st.form(
                cls := "form3 reply",
                action := s"${routes.ForumPost.create(categ.slug, topic.slug, posts.currentPage)}#reply",
                method := "POST",
                novalidate := true
              )(
                  form3.group(form("text"), trans.message.frag()) { f =>
                    form3.textarea(f, klass = "post-text-area")(rows := 10, dataTopic := topic.id)
                  },
                  views.html.base.captcha(form, captcha),
                  form3.actions(
                    a(href := routes.ForumCateg.show(categ.slug))(trans.cancel.frag()),
                    isGranted(_.PublicMod) option
                      form3.submit(frag("Reply as mod"), nameValue = (form("modIcon").name, "true").some, icon = "".some),
                    form3.submit(trans.reply.frag())
                  )
                )
            )
          }.getOrElse {
            if (topic.closed) p(trans.thisTopicIsNowClosed.frag())
            else categ.team.filterNot(myTeam).map { teamId =>
              p(
                a(href := routes.Team.show(teamId)),
                s"Join the ${teamIdToName(teamId)} team",
                " to post in this forum"
              )
            }
          }
        ),
        bar
      )
    }
  private val dataTopic = attr("data-topic")
  private val dataUnsub = attr("data-unsub")
}
