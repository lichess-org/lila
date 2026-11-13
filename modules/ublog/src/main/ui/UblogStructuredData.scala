package lila.ublog
package ui

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.config.NetConfig
import lila.ui.{ Helpers, OpenGraph }

final class UblogStructuredData(helpers: Helpers, net: NetConfig):
  import helpers.*

  private given Writes[Instant] = Writes: d =>
    JsString(isoDateTimeFormatter.print(d))

  def apply(user: User, post: UblogPost, image: Option[Url]) =
    post.lived.map: live =>
      lila.ui.bits.structuredData("BlogPosting"):
        Json.obj(
          "headline" -> post.title,
          "description" -> post.intro,
          "inLanguage" -> post.language,
          "image" -> image.toList,
          "keywords" -> post.topics,
          "datePublished" -> live.at,
          "dateModified" -> post.updated.map(_.at).getOrElse(live.at),
          "discussionUrl" -> Option.when(~post.discuss)(routeUrl(routes.Ublog.discuss(post.id))),
          "author" -> user
            .is(UserId.lichess)
            .so(lila.ui.bits.organizationJson(net))
            .getOrElse(authorJson(user)),
          "publisher" -> lila.ui.bits.organizationJson(net)
        )

  private def authorJson(user: User) =
    Json
      .obj(
        "@type" -> "Person",
        "@id" -> routes.User.show(user.username).url,
        "url" -> routeUrl(routes.User.show(user.username)),
        "name" -> user.username
      )
      .add("honorificPrefix" -> user.title)
      .add("alternateName" -> user.profileOrDefault.nonEmptyRealName)

  def openGraph(user: User, post: UblogPost, image: Option[Url]) =
    OpenGraph(
      `type` = "article",
      image = image,
      title = post.title,
      url = routeUrl(routes.Ublog.post(user.username, post.slug, post.id)),
      description = post.intro,
      extra = List(
        post.lived.map(l => ("article:published_time", isoDateTimeFormatter.print(l.at))),
        post.updated.map(u => ("article:modified_time", isoDateTimeFormatter.print(u.at))),
        ("article:author", routeUrl(routes.User.show(user.username)).value).some
      ).flatten ::: post.topics.map(t => ("article:tag", t.value))
    )
