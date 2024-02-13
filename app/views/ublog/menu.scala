package views.html.ublog

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object menu:

  def apply(active: Either[UserId, String])(using ctx: Context) =
    def isRight(s: String) = active.fold(_ => false, _ == s)
    val lichess            = active.left.toOption.has(lila.user.User.lichessId)
    val community = active == Right("community") || (active.left.toOption.exists(ctx.isnt) && !lichess)
    val mine      = active.left.toOption.exists(ctx.is)
    views.html.site.bits.pageMenuSubnav(
      cls := "force-ltr",
      ctx.kid.no option a(
        cls  := community.option("active"),
        href := langHref(routes.Ublog.communityAll())
      )(trans.ublog.communityBlogs()),
      ctx.kid.no option a(cls := isRight("topics").option("active"), href := routes.Ublog.topics)(
        trans.ublog.blogTopics()
      ),
      (ctx.isAuth && ctx.kid.no) option a(
        cls  := isRight("friends").option("active"),
        href := routes.Ublog.friends()
      )(trans.ublog.friendBlogs()),
      ctx.kid.no option a(cls := isRight("liked").option("active"), href := routes.Ublog.liked())(
        trans.ublog.likedBlogs()
      ),
      ctx.me
        .ifTrue(ctx.kid.no)
        .map: me =>
          a(cls := mine.option("active"), href := routes.Ublog.index(me.username))("My blog"),
      a(cls := lichess.option("active"), href := routes.Ublog.index("Lichess"))("Lichess blog")
    )
