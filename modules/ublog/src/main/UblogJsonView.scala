package lila.ublog

import play.api.libs.json.*
import lila.core.LightUser
import lila.common.Json.given

final class UblogJsonView(picfitUrl: lila.core.misc.PicfitUrl, lightUser: LightUser.GetterSync):

  given OWrites[UblogPost.PreviewPost] = OWrites: p =>
    Json
      .obj(
        "id" -> p.id,
        "title" -> p.title,
        "slug" -> p.slug,
        "createdAt" -> p.created.at,
        "url" -> urlOfPost(p).url
      )
      .add("author" -> lightUser(p.created.by))
      .add("image" -> p.image.map(i => UblogPost.thumbnail(picfitUrl, i.id, _.Size.Small)))

  def urlOfPost(post: UblogPost.BasePost) = post.blog match
    case UblogBlog.Id.User(userId) => routes.Ublog.post(userId, post.slug, post.id)
