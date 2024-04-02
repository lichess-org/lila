package views.html
package forum

import controllers.report.routes.Report as reportRoutes
import controllers.routes
import controllers.team.routes.Team as teamRoutes
import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.core.captcha.Captcha
import scalalib.paginator.Paginator

object topic:

  def form(categ: lila.forum.ForumCateg, form: Form[?], captcha: Captcha)(using PageContext) =
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
          h2(dataIcon := licon.CautionTriangle, cls := "text")(trans.site.important()),
          p:
            trans.site.yourQuestionMayHaveBeenAnswered:
              strong(a(href := routes.Main.faq)(trans.site.inTheFAQ()))
          ,
          p:
            trans.site.toReportSomeoneForCheatingOrBadBehavior:
              strong(a(href := reportRoutes.form)(trans.site.useTheReportForm()))
          ,
          p:
            trans.site.toRequestSupport:
              strong(a(href := routes.Main.contact)(trans.site.tryTheContactPage()))
          ,
          p:
            trans.site.makeSureToRead:
              strong(a(href := routes.Cms.lonePage("forum-etiquette"))(trans.site.theForumEtiquette()))
        ),
        postForm(cls := "form3", action := routes.ForumTopic.create(categ.slug))(
          form3.group(form("name"), trans.site.subject())(form3.input(_)(autofocus)),
          form3.group(form("post")("text"), trans.site.message())(
            form3.textarea(_, klass = "post-text-area")(rows := 10)
          ),
          views.html.base.captcha(form("post"), captcha),
          form3.actions(
            a(href := routes.ForumCateg.show(categ.slug))(trans.site.cancel()),
            isGranted(_.PublicMod).option(
              form3.submit(
                frag("Create as a mod"),
                nameValue = (form("post")("modIcon").name, "true").some,
                icon = licon.Agent.some
              )
            ),
            form3.submit(trans.site.createTheTopic())
          )
        )
      )

  def show(
      categ: lila.forum.ForumCateg,
      topic: lila.forum.ForumTopic,
      posts: Paginator[lila.forum.ForumPost.WithFrag],
      formWithCaptcha: Option[(Form[?], Captcha)],
      unsub: Option[Boolean],
      canModCateg: Boolean,
      formText: Option[String] = None,
      replyBlocked: Boolean = false
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${topic.name} • page ${posts.currentPage}/${posts.nbPages} • ${categ.name}",
      moreJs = frag(
        jsModule("forum"),
        formWithCaptcha.isDefined.option(captchaTag),
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
    ):
      val isDiagnostic = categ.isDiagnostic && (canModCateg || ctx.me.exists(topic.isAuthor))
      val headerText   = if isDiagnostic then "Diagnostics" else topic.name
      val backUrl =
        if isDiagnostic && !canModCateg then routes.ForumCateg.index.url
        else
          topic.ublogId.fold(s"${routes.ForumCateg.show(categ.slug)}"): id =>
            routes.Ublog.redirect(id).url

      val teamOnly = categ.team.filterNot(isMyTeamSync)
      val pager = views.html.base.bits
        .paginationByQuery(routes.ForumTopic.show(categ.slug, topic.slug, 1), posts, showPost = true)
      main(cls := "forum forum-topic page-small box box-pad")(
        boxTop(
          h1(a(href := backUrl, dataIcon := licon.LessThan, cls := "text"), headerText),
          isDiagnostic.option(
            postForm(action := routes.ForumTopic.clearDiagnostic(topic.slug))(
              button(cls := "button button-red")("erase diagnostics")
            )
          )
        ),
        pager,
        div(cls := "forum-topic__posts")(
          posts.currentPageResults.map: p =>
            post.show(
              categ,
              topic,
              p,
              s"${routes.ForumTopic.show(categ.slug, topic.slug, posts.currentPage)}#${p.post.number}",
              canReply = formWithCaptcha.isDefined,
              canModCateg = canModCateg,
              canReact = teamOnly.isEmpty
            )
        ),
        pager,
        div(cls := "forum-topic__actions")(
          if topic.isOld then p(trans.site.thisTopicIsArchived())
          else if formWithCaptcha.isDefined then h2(id := "reply")(trans.site.replyToThisTopic())
          else if topic.closed then p(trans.site.thisTopicIsNowClosed())
          else
            teamOnly
              .map: teamId =>
                p:
                  trans.site.joinTheTeamXToPost:
                    a(href := teamRoutes.show(teamId))(trans.site.teamNamedX(teamLink(teamId, true)))
              .orElse:
                if ctx.me.exists(_.isBot) then p("Bots cannot post in the forum.").some
                else if replyBlocked then p(trans.ublog.youBlockedByBlogAuthor()).some
                else ctx.isAuth.option(p(trans.site.youCannotPostYetPlaySomeGames()))
          ,
          div(
            unsub.map: uns =>
              postForm(
                cls    := s"unsub ${if uns then "on" else "off"}",
                action := routes.Timeline.unsub(s"forum:${topic.id}")
              )(
                button(cls := "button button-empty text on", dataIcon := licon.Eye, bits.dataUnsub := "off"):
                  trans.site.subscribe()
                ,
                button(cls := "button button-empty text off", dataIcon := licon.Eye, bits.dataUnsub := "on"):
                  trans.site.unsubscribe()
              ),
            (canModCateg || (topic.isUblog && ctx.me.exists(topic.isAuthor))).option(
              postForm(action := routes.ForumTopic.close(categ.slug, topic.slug))(
                button(cls := "button button-empty button-red")(
                  if topic.closed then "Reopen" else "Close"
                )
              )
            ),
            canModCateg.option(
              postForm(action := routes.ForumTopic.sticky(categ.slug, topic.slug))(
                button(cls := "button button-empty button-brag")(
                  if topic.isSticky then "Unsticky" else "Sticky"
                )
              )
            ),
            (canModCateg || ctx.me.exists(topic.isAuthor)).option(deleteModal)
          )
        ),
        formWithCaptcha.map: (form, captcha) =>
          postForm(
            cls    := "form3 reply",
            action := s"${routes.ForumPost.create(categ.slug, topic.slug, posts.currentPage)}#reply",
            novalidate
          )(
            form3.group(
              form("text"),
              trans.site.message(),
              help = a(
                dataIcon := licon.InfoCircle,
                cls      := "text",
                href     := routes.Cms.lonePage("forum-etiquette")
              )(
                "Forum etiquette"
              ).some
            ): f =>
              form3.textarea(f, klass = "post-text-area")(rows := 10, bits.dataTopic := topic.id)(
                formText
              ),
            views.html.base.captcha(form, captcha),
            form3.actions(
              a(href := routes.ForumCateg.show(categ.slug))(trans.site.cancel()),
              (isGranted(_.PublicMod) || isGranted(_.SeeReport)).option(
                form3.submit(
                  frag(s"Reply as a mod ${(!isGranted(_.PublicMod)).so("(anonymously)")}"),
                  nameValue = (form("modIcon").name, "true").some,
                  icon = licon.Agent.some
                )
              ),
              form3.submit(trans.site.reply())
            )
          )
      )

  def makeDiagnostic(categ: lila.forum.ForumCateg, form: Form[?], captcha: Captcha, text: String)(using
      PageContext
  )(using me: Me) =
    views.html.base.layout(
      title = "Diagnostic report",
      moreCss = cssTag("forum"),
      moreJs = frag(
        jsModule("forum"),
        captchaTag
      )
    ):
      main(cls := "forum forum-topic topic-form page-small box box-pad")(
        boxTop(h1(dataIcon := licon.BubbleConvo, cls := "text")("Diagnostics")),
        st.section(cls := "warning")(
          h2(dataIcon := licon.CautionTriangle, cls := "text")(trans.site.important()),
          p("Unsolicited reports will be ignored."),
          p("Only you and the Lichess moderators can see this forum.")
        ),
        postForm(cls := "form3", action := routes.ForumTopic.create(categ.slug))(
          form3.group(form("post")("text"), trans.site.message())(
            form3.textarea(_, klass = "post-text-area")(rows := 10)(text)
          ),
          form3.hidden("name", me.username.value),
          views.html.base.captcha(form("post"), captcha),
          form3.actions(form3.submit("submit"))
        )
      )

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
