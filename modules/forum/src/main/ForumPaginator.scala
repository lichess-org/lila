package lila.forum

import lila.common.paginator.*
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.user.{ User, Me }

final class ForumPaginator(
    topicRepo: ForumTopicRepo,
    postRepo: ForumPostRepo,
    config: ForumConfig,
    textExpand: ForumTextExpand
)(using Executor):

  import BSONHandlers.given

  def topicPosts(topic: ForumTopic, page: Int)(using me: Option[Me])(using
      netDomain: lila.common.config.NetDomain
  ): Fu[Paginator[ForumPost.WithFrag]] =
    Paginator(
      Adapter[ForumPost](
        collection = postRepo.coll,
        selector = postRepo.forUser(me) selectTopic topic.id,
        projection = none,
        sort = postRepo.sortQuery
      ),
      currentPage = page,
      maxPerPage = config.postMaxPerPage
    ).flatMap(_ mapFutureList textExpand.manyPosts)

  def categTopics(
      categ: ForumCateg,
      page: Int
  )(using me: Option[Me]): Fu[Paginator[TopicView]] =
    Paginator(
      currentPage = page,
      maxPerPage = config.topicMaxPerPage,
      adapter = new AdapterLike[TopicView]:

        def nbResults: Fu[Int] =
          if categ.isTeam then topicRepo.coll countSel selector
          else fuccess(1000)

        def slice(offset: Int, length: Int): Fu[Seq[TopicView]] =
          topicRepo.coll
            .aggregateList(length, _.sec): framework =>
              import framework.*
              Match(selector) -> List(
                Sort(Descending("updatedAt")),
                Skip(offset),
                Limit(length),
                PipelineOperator:
                  $lookup.simple(
                    from = postRepo.coll,
                    as = "post",
                    local = if me.exists(_.marks.troll) then "lastPostIdTroll" else "lastPostId",
                    foreign = "_id"
                  )
              )
            .map: docs =>
              for
                doc   <- docs
                topic <- doc.asOpt[ForumTopic]
                post = doc.getAsOpt[List[ForumPost]]("post").flatMap(_.headOption)
              yield TopicView(categ, topic, post, topic lastPage config.postMaxPerPage, me)

        private def selector = topicRepo.forUser(me) byCategNotStickyQuery categ
    )
