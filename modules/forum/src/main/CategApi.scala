package lila.forum

import lila.common.paginator._
import lila.db.dsl._
import lila.user.User

final class CategApi(
    postRepo: PostRepo,
    topicRepo: TopicRepo,
    categRepo: CategRepo,
    paginator: ForumPaginator,
    config: ForumConfig
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BSONHandlers._

  def makeTeam(slug: String, name: String): Funit = {
    val categ = Categ(
      _id = teamSlug(slug),
      name = name,
      desc = "Forum of the team " + name,
      team = slug.some,
      nbTopics = 0,
      nbPosts = 0,
      lastPostId = "",
      nbTopicsTroll = 0,
      nbPostsTroll = 0,
      lastPostIdTroll = ""
    )
    val topic = Topic.make(
      categId = categ.slug,
      slug = slug + "-forum",
      name = name + " forum",
      userId = User.lichessId,
      troll = false,
      hidden = false
    )
    val post = Post.make(
      topicId = topic.id,
      author = none,
      userId = User.lichessId.some,
      text = "Welcome to the %s forum!" format name,
      number = 1,
      troll = false,
      hidden = topic.hidden,
      lang = "en".some,
      categId = categ.id,
      modIcon = None
    )
    categRepo.coll.insert.one(categ).void >>
      postRepo.coll.insert.one(post).void >>
      topicRepo.coll.insert.one(topic withPost post).void >>
      categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)).void
  }

  def show(
      slug: String,
      forUser: Option[User],
      page: Int
  ): Fu[Option[(Categ, Paginator[TopicView])]] =
    categRepo bySlug slug flatMap {
      _ ?? { categ =>
        paginator.categTopics(categ, forUser, page) dmap { (categ, _).some }
      }
    }

  def denormalize(categ: Categ): Funit =
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
              lastPostId = lastPost ?? (_.id),
              nbTopicsTroll = nbTopicsTroll,
              nbPostsTroll = nbPostsTroll,
              lastPostIdTroll = lastPostTroll ?? (_.id)
            )
          )
          .void
    } yield ()
}
