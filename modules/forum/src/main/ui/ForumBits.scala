package lila.forum
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ForumBits(helpers: Helpers):
  import helpers.{ *, given }

  def searchForm(search: String = "")(using Context) =
    div(cls := "box__top__actions")(
      form(cls := "search", action := routes.ForumPost.search())(
        input(
          name := "text",
          value := search,
          placeholder := trans.search.search.txt(),
          enterkeyhint := "search"
        )
      )
    )

  def authorLink(post: ForumPost, cssClass: Option[String] = None, withOnline: Boolean = true)(using
      Context
  ): Frag =
    if !Granter.opt(_.ModerateForum) && post.erased
    then span(cls := "author")("<erased>")
    else
      userIdLink(
        post.userId,
        cssClass = cssClass,
        withOnline = withOnline,
        modIcon = ~post.modIcon
      )

  val dataTopic = attr("data-topic")
  val dataUnsub = attr("data-unsub")

  def postTextarea(field: play.api.data.Field)(modifiers: Modifier*)(using Context) =
    bits.markdownEditor(MarkdownRealm.forum):
      form3.textarea(field, "post-text-area")(
        rows := 10,
        autocomplete := "off",
        placeholder := trans.site.pleaseBeNiceInTheForum.txt()
      )(modifiers)

  def usermodTimeout(negative: Usermod.NegativeReports)(using Context) =
    st.section(cls := "forum-usermod-timeout")(
      h2(trans.site.forumUsageTemporarilyDisabled()),
      p(trans.site.youMayPostAgainX(momentFromNow(negative.until))),
      negative.posts.nonEmpty.option:
        frag(
          p(trans.site.complaintsThatCausedThis()),
          ul:
            negative.posts.toList
              .sortBy(_._1.value)
              .map: (postId, post) =>
                li(
                  a(href := routes.ForumPost.redirect(postId))(post.topic),
                  " — ",
                  Usermod.Reason.values.toList
                    .filter: reason =>
                      post.complaints.values.exists(_ == reason)
                    .zipWithIndex
                    .map: (reason, index) =>
                      val count = post.complaints.values.count(_ == reason)
                      span(
                        if index > 0 then ", " else "",
                        reason match
                          case Usermod.Reason.Disagree => trans.site.disagree()
                          case Usermod.Reason.Troll => trans.site.troll()
                          case Usermod.Reason.OffTopic => trans.site.offTopic()
                          case Usermod.Reason.Spam => trans.site.spam()
                          case Usermod.Reason.Offensive => trans.site.offensive()
                          case Usermod.Reason.Abusive => trans.site.abusive(),
                        s" × $count"
                      )
                )
        )
    )
