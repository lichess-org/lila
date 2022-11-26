package lila.forum

import lila.common.paginator.*
import lila.db.dsl.{ *, given }
import lila.user.User

final class ForumCategApi(
    postRepo: ForumPostRepo,
    topicRepo: ForumTopicRepo,
    categRepo: ForumCategRepo,
    paginator: ForumPaginator,
    config: ForumConfig
)(using scala.concurrent.ExecutionContext):

  import BSONHandlers.given

  def makeTeam(teamId: TeamId, name: String): Funit =
    val categ = ForumCateg(
      _id = teamSlug(teamId),
      name = name,
      desc = "Forum of the team " + name,
      team = teamId.some,
      nbTopics = 0,
      nbPosts = 0,
      lastPostId = ForumPost.Id(""),
      nbTopicsTroll = 0,
      nbPostsTroll = 0,
      lastPostIdTroll = ForumPost.Id("")
    )
    val topic = ForumTopic.make(
      categId = categ.slug,
      slug = s"$teamId-forum",
      name = name + " forum",
      userId = User.lichessId,
      troll = false
    )
    val post = ForumPost.make(
      topicId = topic.id,
      author = none,
      userId = User.lichessId.some,
      text = "Welcome to the %s forum!" format name,
      number = 1,
      troll = false,
      lang = "en".some,
      categId = categ.id,
      modIcon = None
    )
    categRepo.coll.insert.one(categ).void >>
      postRepo.coll.insert.one(post).void >>
      topicRepo.coll.insert.one(topic withPost post).void >>
      categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)).void

  def show(
      slug: String,
      forUser: Option[User],
      page: Int
  ): Fu[Option[(ForumCateg, Paginator[TopicView])]] =
    categRepo bySlug slug flatMap {
      _ ?? { categ =>
        paginator.categTopics(categ, forUser, page) dmap { (categ, _).some }
      }
    }

  def denormalize(categ: ForumCateg): Funit =
    for {
      nbTopics      <- topicRepo countByCateg categ
      nbPosts       <- postRepo countByCateg categ
      lastPost      <- postRepo lastByCateg categ
      nbTopicsTroll <- topicRepo.unsafe countByCateg categ
      nbPostsTroll  <- postRepo.unsafe countByCateg categ
      lastPostTroll <- postRepo.unsafe lastByCateg categ
      _ <-
        categRepo.coll.update
          .one(
            $id(categ.id),
            categ.copy(
              nbTopics = nbTopics,
              nbPosts = nbPosts,
              lastPostId = lastPost.fold(categ.lastPostId)(_.id),
              nbTopicsTroll = nbTopicsTroll,
              nbPostsTroll = nbPostsTroll,
              lastPostIdTroll = lastPostTroll.fold(categ.lastPostIdTroll)(_.id)
            )
          )
          .void
    } yield ()
