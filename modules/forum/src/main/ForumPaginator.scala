package lila.forum

import scalalib.paginator.*
import scalatags.Text.all.raw

import lila.core.config.NetDomain
import lila.db.dsl.*
import lila.db.paginator.Adapter

final class ForumPaginator(
    topicRepo: ForumTopicRepo,
    postRepo: ForumPostRepo,
    feedApi: lila.feed.FeedApi,
    config: ForumConfig,
    textExpand: ForumTextExpand
)(using Executor):

  import BSONHandlers.given

  def recent(categ: ForumCateg, page: Int): Fu[Paginator[ForumPost]] =
    Paginator(
      Adapter[ForumPost](
        collection = postRepo.coll,
        selector = postRepo.selectCateg(categ.id),
        projection = none,
        sort = $sort.createdDesc
      ).withLotsOfResults,
      currentPage = page,
      maxPerPage = MaxPerPage(30)
    )

  def topicPosts(topic: ForumTopic, page: Int)(using
      me: Option[Me],
      netDomain: NetDomain
  ): Fu[Paginator[ForumPost.WithFrag]] =
    topic.feedItemId.fold(
      Paginator(
        Adapter[ForumPost](
          collection = postRepo.coll,
          selector = postRepo.forUser(me).selectTopic(topic.id),
          projection = none,
          sort = $sort.createdAsc
        ).mapFutureList(textExpand.manyPosts),
        currentPage = page,
        maxPerPage = config.postMaxPerPage
      )
    )(feedComments(topic, _, page))

  def categTopics(
      categ: ForumCateg,
      page: Int
  )(using me: Option[Me]): Fu[Paginator[TopicView]] =
    Paginator(
      currentPage = page,
      maxPerPage = config.topicMaxPerPage,
      adapter = new AdapterLike[TopicView]:

        def nbResults: Fu[Int] =
          if categ.isTeam then topicRepo.coll.countSel(selector)
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
                doc <- docs
                topic <- doc.asOpt[ForumTopic]
                post = doc.getAsOpt[List[ForumPost]]("post").flatMap(_.headOption)
              yield TopicView(categ, topic, post, topic.lastPage(config.postMaxPerPage), me)

        private def selector = topicRepo.forUser(me).byCategNotStickyQuery(categ.id)
    )

  private def feedComments(
      topic: ForumTopic,
      feedItemId: lila.feed.Feed.ID,
      page: Int
  )(using
      me: Option[Me],
      netDomain: NetDomain
  ): Fu[Paginator[ForumPost.WithFrag]] =
    Paginator(
      currentPage = page,
      maxPerPage = config.postMaxPerPage,
      adapter = new AdapterLike[ForumPost.WithFrag]:
        def nbResults = postRepo.forUser(me).countByTopic(topic).map(_ + 1)
        def slice(offset: Int, length: Int) =
          val insertFeedItem = offset == 0
          val postOffset = if insertFeedItem then 0 else offset - 1
          val nbToFetch = if insertFeedItem then length - 1 else length
          for
            feedItem <- feedApi.get(feedItemId)
            posts <- postRepo
              .forUser(me)
              .coll
              .find(postRepo.forUser(me).selectTopic(topic.id))
              .sort($sort.createdAsc)
              .skip(postOffset)
              .cursor[ForumPost]()
              .list(nbToFetch)
            postFrags <- textExpand.manyPosts(posts)
          yield feedItem
            .filter(_ => insertFeedItem)
            .map: item =>
              ForumPost.WithFrag(
                ForumPost(
                  id = ForumPostId("feeditem"),
                  topicId = topic.id,
                  categId = topic.categId,
                  author = "lichess".some,
                  userId = none,
                  text = item.content.value,
                  troll = false,
                  lang = none,
                  createdAt = item.at,
                  modIcon = true.some
                ),
                raw(item.rendered.value)
              ) :: postFrags.toList
            .getOrElse(postFrags)
    )
