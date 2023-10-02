package views.html
package forum

import controllers.report.routes.{ Report as reportRoutes }
import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator

object topic:

  def form(categ: lila.forum.ForumCateg, form: Form[?], captcha: lila.common.Captcha)(using PageContext) =
    views.html.base.layout(
      title = "New forum topic",
      moreCss = cssTag("forum"),
      moreJs = frag(
        jsModule("forum"),
        captchaTag
      )
    ):
      main(cls := "forum forum-topic topic-form page-small box box-pad")(
        boxTop(
          h1(
            a(href := routes.ForumCateg.show(categ.slug), dataIcon := licon.LessThan, cls := "text"),
            categ.name
          )
        ),
        st.section(cls := "warning")(
          h2(dataIcon := licon.CautionTriangle, cls := "text")(trans.important()),
          p:
            trans.yourQuestionMayHaveBeenAnswered:
              strong(a(href := routes.Main.faq)(trans.inTheFAQ()))
          ,
          p:
            trans.toReportSomeoneForCheatingOrBadBehavior:
              strong(a(href := reportRoutes.form)(trans.useTheReportForm()))
          ,
          p:
            trans.toRequestSupport:
              strong(a(href := routes.Main.contact)(trans.tryTheContactPage()))
          ,
          p:
            trans.makeSureToRead:
              strong(a(href := routes.ContentPage.loneBookmark("forum-etiquette"))(trans.theForumEtiquette()))
        ),
        postForm(cls := "form3", action := routes.ForumTopic.create(categ.slug))(
          form3.group(form("name"), trans.subject())(form3.input(_)(autofocus)),
          form3.group(form("post")("text"), trans.message())(
            form3.textarea(_, klass = "post-text-area")(rows := 10)
          ),
          views.html.base.captcha(form("post"), captcha),
          form3.actions(
            a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
            isGranted(_.PublicMod) option
              form3.submit(
                frag("Create as a mod"),
                nameValue = (form("post")("modIcon").name, "true").some,
                icon = licon.Agent.some
              ),
            form3.submit(trans.createTheTopic())
          )
        )
      )

  def show(
      categ: lila.forum.ForumCateg,
      topic: lila.forum.ForumTopic,
      posts: Paginator[lila.forum.ForumPost.WithFrag],
      formWithCaptcha: Option[FormWithCaptcha],
      unsub: Option[Boolean],
      canModCateg: Boolean
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${topic.name} • page ${posts.currentPage}/${posts.nbPages} • ${categ.name}",
      moreJs = frag(
        jsModule("forum"),
        formWithCaptcha.isDefined option captchaTag,
        jsModule("expandText")
      ),
      moreCss = cssTag("forum"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = topic.name,
          url = s"$netBaseUrl${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage).url}",
          description = shorten(posts.currentPageResults.headOption.so(_.post.text), 152)
        )
        .some,
      csp = defaultCsp.withInlineIconFont.withTwitter.some
    ) {
      val teamOnly = categ.team.filterNot(isMyTeamSync)
      val pager = views.html.base.bits
        .paginationByQuery(routes.ForumTopic.show(categ.slug, topic.slug, 1), posts, showPost = true)
      main(cls := "forum forum-topic page-small box box-pad")(
        boxTop(
          h1(
            a(
              href := topic.ublogId.fold(s"${routes.ForumCateg.show(categ.slug)}") { id =>
                routes.Ublog.redirect(id).url
              },
              dataIcon := licon.LessThan,
              cls      := "text"
            ),
            topic.name
          )
        ),
        pager,
        div(cls := "forum-topic__posts")(
          posts.currentPageResults.map { p =>
            post.show(
              categ,
              topic,
              p,
              s"${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage)}#${p.post.number}",
              canReply = formWithCaptcha.isDefined,
              canModCateg = canModCateg,
              canReact = teamOnly.isEmpty
            )
          }
        ),
        pager,
        div(cls := "forum-topic__actions")(
          if topic.isOld then p(trans.thisTopicIsArchived())
          else if formWithCaptcha.isDefined then h2(id := "reply")(trans.replyToThisTopic())
          else if topic.closed then p(trans.thisTopicIsNowClosed())
          else
            teamOnly.map { teamId =>
              p(
                trans.joinTheTeamXToPost(
                  a(href := routes.Team.show(teamId))(trans.teamNamedX(teamIdToName(teamId)))
                )
              )
            } orElse {
              if ctx.me.exists(_.isBot) then p("Bots cannot post in the forum.").some
              else ctx.isAuth option p(trans.youCannotPostYetPlaySomeGames())
            }
          ,
          div(
            unsub.map { uns =>
              postForm(
                cls    := s"unsub ${if uns then "on" else "off"}",
                action := routes.Timeline.unsub(s"forum:${topic.id}")
              )(
                button(cls := "button button-empty text on", dataIcon := licon.Eye, bits.dataUnsub := "off")(
                  trans.subscribe()
                ),
                button(cls := "button button-empty text off", dataIcon := licon.Eye, bits.dataUnsub := "on")(
                  trans.unsubscribe()
                )
              )
            },
            canModCateg || (topic.isUblog && ctx.me.exists(topic.isAuthor)) option
              postForm(action := routes.ForumTopic.close(categ.slug, topic.slug))(
                button(cls := "button button-empty button-red")(
                  if topic.closed then "Reopen" else "Close"
                )
              ),
            canModCateg option
              postForm(action := routes.ForumTopic.sticky(categ.slug, topic.slug))(
                button(cls := "button button-empty button-brag")(
                  if topic.isSticky then "Unsticky" else "Sticky"
                )
              ),
            canModCateg || ctx.me.exists(topic.isAuthor) option deleteModal
          )
        ),
        formWithCaptcha.map { (form, captcha) =>
          postForm(
            cls    := "form3 reply",
            action := s"${routes.ForumPost.create(categ.slug, topic.slug, posts.currentPage)}#reply",
            novalidate
          )(
            form3.group(
              form("text"),
              trans.message(),
              help = a(
                dataIcon := licon.InfoCircle,
                cls      := "text",
                href     := routes.ContentPage.loneBookmark("forum-etiquette")
              )(
                "Forum etiquette"
              ).some
            ) { f =>
              form3.textarea(f, klass = "post-text-area")(rows := 10, bits.dataTopic := topic.id)
            },
            views.html.base.captcha(form, captcha),
            form3.actions(
              a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
              (isGranted(_.PublicMod) || isGranted(_.SeeReport)) option
                form3.submit(
                  frag(s"Reply as a mod ${(!isGranted(_.PublicMod)).so("(anonymously)")}"),
                  nameValue = (form("modIcon").name, "true").some,
                  icon = licon.Agent.some
                ),
              form3.submit(trans.reply())
            )
          )
        }
      )
    }

  private def deleteModal =
    div(cls := "forum-delete-modal none")(
      p("Delete the post"),
      st.form(method := "post", cls := "form3")(
        st.select(
          name := "reason",
          cls  := "form-control"
        )(
          option(value := "")("no message"),
          lila.msg.MsgPreset.forumDeletion.presets.map: reason =>
            option(value := reason)(reason)
        ),
        form3.actions(
          button(cls := "cancel button button-empty", tpe := "button")("Cancel"),
          form3.submit(
            frag("Delete the post")
          )(value := "default", cls := "button-red")
        )
      )
    )
