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
