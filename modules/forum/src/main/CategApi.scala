package lila.forum

import lila.common.paginator._
import lila.db.dsl._
import lila.user.User

final class CategApi(env: Env, config: ForumConfig)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def list(teams: Iterable[String], forUser: Option[User]): Fu[List[CategView]] =
    for {
      categs <- env.categRepo withTeams teams
      views <- (categs map { categ =>
        env.postApi get (categ lastPostId forUser) map { topicPost =>
          CategView(
            categ,
            topicPost map { case (topic, post) =>
              (topic, post, topic lastPage config.postMaxPerPage)
            },
            forUser
          )
        }
      }).sequenceFu
    } yield views

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
      text =
        "Welcome to the %s forum!\nOnly members of the team can post here, but everybody can read." format name,
      number = 1,
      troll = false,
      hidden = topic.hidden,
      lang = "en".some,
      categId = categ.id,
      modIcon = None
    )
    env.categRepo.coll.insert.one(categ).void >>
      env.postRepo.coll.insert.one(post).void >>
      env.topicRepo.coll.insert.one(topic withPost post).void >>
      env.categRepo.coll.update.one($id(categ.id), categ.withPost(topic, post)).void
  }

  def show(slug: String, page: Int, forUser: Option[User]): Fu[Option[(Categ, Paginator[TopicView])]] =
    env.categRepo bySlug slug flatMap {
      _ ?? { categ =>
        env.paginator.categTopics(categ, page, forUser) dmap { (categ, _).some }
      }
    }

  def denormalize(categ: Categ): Funit =
    for {
      nbTopics      <- env.topicRepo countByCateg categ
      nbPosts       <- env.postRepo countByCateg categ
      lastPost      <- env.postRepo lastByCateg categ
      nbTopicsTroll <- env.topicRepo.unsafe countByCateg categ
      nbPostsTroll  <- env.postRepo.unsafe countByCateg categ
      lastPostTroll <- env.postRepo.unsafe lastByCateg categ
      _ <-
        env.categRepo.coll.update
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
