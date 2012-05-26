package lila
package forum

import scalaz.effects._
import com.github.ornicar.paginator._

final class PostApi(env: ForumEnv, maxPerPage: Int) {

  def paginator(topic: Topic, page: Int): Paginator[Post] =
    Paginator(
      SalatAdapter(
        dao = env.postRepo,
        query = env.postRepo byTopicQuery topic,
        sort = env.postRepo.sortQuery),
      currentPage = page,
      maxPerPage = maxPerPage
    ) | paginator(topic, 1)
}
