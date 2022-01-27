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
      new Adapter(
        collection = postRepo.coll,
        selector = postRepo.forUser(me) selectTopic topic.id,
        projection = none,
        sort = postRepo.sortQuery
      ),
      currentPage = page,
      maxPerPage = config.postMaxPerPage
    )

  def categTopics(categ: Categ, page: Int, forUser: Option[User]): Fu[Paginator[TopicView]] =
    Paginator(
      currentPage = page,
      maxPerPage = config.postMaxPerPage,
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
                Skip(offset),
                Limit(length),
                PipelineOperator(
                  $lookup.simple(
                    from = postRepo.coll,
                    as = "post",
                    local = "lastPostId",
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
