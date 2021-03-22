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
        jsModule("forum"),
        captchaTag
      )
    ) {
      main(cls := "forum forum-topic topic-form page-small box box-pad")(
        h1(
          a(href := routes.ForumCateg.show(categ.slug), dataIcon := "I", cls := "text"),
          categ.name
        ),
        st.section(cls := "warning")(
          h2(dataIcon := "!", cls := "text")(trans.important()),
          p(
            trans.yourQuestionMayHaveBeenAnswered(
              strong(a(href := routes.Main.faq)(trans.inTheFAQ()))
            )
          ),
          p(
            trans.toReportSomeoneForCheatingOrBadBehavior(
              strong(a(href := routes.Report.form)(trans.useTheReportForm()))
            )
          ),
          p(
            trans.toRequestSupport(
              strong(a(href := routes.Main.contact)(trans.tryTheContactPage()))
            )
          ),
          p(
            "Make sure to read ",
            strong(a(href := routes.Page.loneBookmark("forum-etiquette"))("the forum etiquette"))
          )
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
                icon = "".some
              ),
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
  )(implicit ctx: Context) =
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
          description = shorten(posts.currentPageResults.headOption.??(_.text), 152)
        )
        .some
    ) {
      val teamOnly = categ.team.filterNot(myTeam)
      val pager = views.html.base.bits
        .paginationByQuery(routes.ForumTopic.show(categ.slug, topic.slug, 1), posts, showPost = true)

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
        div(cls := "forum-topic__posts expand-text")(
          posts.currentPageResults.map { p =>
            post.show(
              categ,
              topic,
              p,
              s"${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage)}#${p.number}",
              canModCateg = canModCateg,
              canReact = teamOnly.isEmpty
            )
          }
        ),
        div(cls := "forum-topic__actions")(
          if (posts.hasNextPage) emptyFrag
          else if (topic.isOld)
            p(trans.thisTopicIsArchived())
          else if (formWithCaptcha.isDefined)
            h2(id := "reply")(trans.replyToThisTopic())
          else if (topic.closed) p(trans.thisTopicIsNowClosed())
          else
            teamOnly.map { teamId =>
              p(
                trans.joinTheTeamXToPost(
                  a(href := routes.Team.show(teamId))(trans.teamNamedX(teamIdToName(teamId)))
                )
              )
            } orElse {
              if (ctx.me.exists(_.isBot)) p("Bots cannot post in the forum.").some
              else ctx.isAuth option p(trans.youCannotPostYetPlaySomeGames())
            },
          div(
            unsub.map { uns =>
              postForm(
                cls := s"unsub ${if (uns) "on" else "off"}",
                action := routes.Timeline.unsub(s"forum:${topic.id}")
              )(
                button(cls := "button button-empty text on", dataIcon := "v", bits.dataUnsub := "off")(
                  trans.subscribe()
                ),
                button(cls := "button button-empty text off", dataIcon := "v", bits.dataUnsub := "on")(
                  trans.unsubscribe()
                )
              )
            },
            isGranted(_.ModerateForum) option
              postForm(action := routes.ForumTopic.hide(categ.slug, topic.slug))(
                button(cls := "button button-empty button-green")(
                  if (topic.hidden) "Feature" else "Un-feature"
                )
              ),
            canModCateg option
              postForm(action := routes.ForumTopic.close(categ.slug, topic.slug))(
                button(cls := "button button-empty button-red")(
                  if (topic.closed) "Reopen" else "Close"
                )
              ),
            canModCateg option
              postForm(action := routes.ForumTopic.sticky(categ.slug, topic.slug))(
                button(cls := "button button-empty button-brag")(
                  if (topic.isSticky) "Unsticky" else "Sticky"
                )
              )
          )
        ),
        formWithCaptcha.map { case (form, captcha) =>
          postForm(
            cls := "form3 reply",
            action := s"${routes.ForumPost.create(categ.slug, topic.slug, posts.currentPage)}#reply",
            novalidate
          )(
            form3.group(
              form("text"),
              trans.message(),
              help = a(dataIcon := "", cls := "text", href := routes.Page.loneBookmark("forum-etiquette"))(
                "Forum etiquette"
              ).some
            ) { f =>
              form3.textarea(f, klass = "post-text-area")(rows := 10, bits.dataTopic := topic.id)
            },
            views.html.base.captcha(form, captcha),
            form3.actions(
              a(href := routes.ForumCateg.show(categ.slug))(trans.cancel()),
              isGranted(_.PublicMod) option
                form3.submit(
                  frag("Reply as a mod"),
                  nameValue = (form("modIcon").name, "true").some,
                  icon = "".some
                ),
              form3.submit(trans.reply())
            )
          )
        },
        pager
      )
    }
}
