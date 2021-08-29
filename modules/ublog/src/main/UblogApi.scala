package lila.ublog

import lila.db.dsl._
import lila.user.User
import scala.concurrent.ExecutionContext

final class UblogApi(coll: Coll)(implicit ec: ExecutionContext) {

  import UblogBsonHandlers._

  def create(data: UblogForm.UblogPostData, user: User): Fu[UblogPost] = {
    val post = UblogPost.make(user, data.title, data.intro, data.markdown)
    coll.insert.one(post) inject post
  }
}
