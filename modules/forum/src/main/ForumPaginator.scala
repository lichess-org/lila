package lila.forum

import scala.concurrent.ExecutionContext
import reactivemongo.api.ReadPreference
import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class ForumPaginator(
    topicRepo: TopicRepo,
    postRepo: PostRepo,
    config: ForumConfig,
    textExpand: ForumTextExpand
)(implicit ec: ExecutionContext) {

  import BSONHandlers._

  def topicPosts(topic: Topic, page: Int, me: Option[User])(implicit
      netDomain: lila.common.config.NetDomain
  ): Fu[Paginator[Post.WithFrag]] =
    Paginator(
      new Adapter[Post](
        collection = postRepo.coll,
        selector = postRepo.forUser(me) selectTopic topic.id,
        projection = none,
        sort = postRepo.sortQuery
      ),
      currentPage = page,
      maxPerPage = config.postMaxPerPage
    ).flatMap(_ mapFutureList textExpand.manyPosts)

  def categTopics(
      categ: Categ,
      forUser: Option[User],
      page: Int
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
                Skip(offset),
                Limit(length),
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
