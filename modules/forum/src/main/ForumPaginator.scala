package lila.forum

import scala.concurrent.ExecutionContext
import reactivemongo.api.ReadPreference

import lila.common.paginator._
import lila.db.dsl._
import lila.user.User

final class ForumPaginator(
    topicRepo: TopicRepo,
    postRepo: PostRepo,
    config: ForumConfig
)(implicit ec: ExecutionContext) {

  import BSONHandlers._

  def topicPosts(topic: Topic, me: Option[User], page: Int, inSlice: Boolean): Fu[Paginator[Post]] =
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
              .skip(if (inSlice) offset else 0)
              .cursor[Post](ReadPreference.primary)
              .list(if (inSlice) length else nb.min(page * config.postMaxPerPage.value))
          }
        }
      }
    )

  def categTopics(
      categ: Categ,
      forUser: Option[User],
      page: Int,
      inSlice: Boolean
  ): Fu[Paginator[TopicView]] =
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
                Skip(if (inSlice) offset else 0),
                Limit(if (inSlice) length else page * config.topicMaxPerPage.value),
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
