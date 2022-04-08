package lila.forum

import scala.concurrent.ExecutionContext

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.user.User
import reactivemongo.api.ReadPreference

final class ForumPaginator(
    topicRepo: TopicRepo,
    postRepo: PostRepo,
    config: ForumConfig
)(implicit ec: ExecutionContext) {

  import BSONHandlers._

  def topicPosts(topic: Topic, page: Int, me: Option[User]): Fu[Paginator[Post]] =
    Paginator(
      currentPage = page,
      maxPerPage = config.postMaxPerPage,
      adapter = new AdapterLike[Post] {
        private def selector   = postRepo.forUser(me) selectTopic topic.id
        def nbResults: Fu[Int] = postRepo.coll.secondaryPreferred.countSel(selector)
        def slice(offset: Int, length: Int): Fu[Seq[Post]] = {
          nbResults flatMap { nb =>
            postRepo.coll
              .find(selector, none: Option[Bdoc])
              .sort(postRepo.sortQuery)
              .skip(if (page < 0) 0 else offset)
              .cursor[Post](ReadPreference.primary)
              .list(if (page > 0) length else nb.min((0 - page) * config.postMaxPerPage.value))
          }
        }
      }
    )

  def categTopics(categ: Categ, page: Int, forUser: Option[User]): Fu[Paginator[TopicView]] =
    Paginator(
      currentPage = page,
      maxPerPage = config.topicMaxPerPage,
      adapter = new AdapterLike[TopicView] {

        def nbResults: Fu[Int] =
          if (categ.isTeam) topicRepo.coll countSel selector
          else fuccess(1000)

        def slice(offset: Int, length: Int): Fu[Seq[TopicView]] =
          topicRepo.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(selector) -> List(
                Sort(Descending("updatedAt")),
                Skip(if (page < 0) 0 else offset),
                Limit(if (page > 0) length else (0 - page) * config.topicMaxPerPage.value),
                PipelineOperator(
                  $lookup.simple(
                    from = postRepo.coll,
                    as = "post",
                    local = if (forUser.exists(_.marks.troll)) "lastPostIdTroll" else "lastPostId",
                    foreign = "_id"
                  )
                )
              )
            }
            .map { docs =>
              for {
                doc   <- docs
                topic <- doc.asOpt[Topic]
                post = doc.getAsOpt[List[Post]]("post").flatMap(_.headOption)
              } yield TopicView(categ, topic, post, topic lastPage config.postMaxPerPage, forUser)
            }

        private def selector = topicRepo.forUser(forUser) byCategNotStickyQuery categ
      }
    )
}
