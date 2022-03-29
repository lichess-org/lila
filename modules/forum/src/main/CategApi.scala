package lila.forum

import lila.common.paginator._
import lila.db.dsl._
import lila.user.User
import lila.pref.PrefApi

final class CategApi(env: Env, prefApi: PrefApi, config: ForumConfig)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  import BSONHandlers._

  def list(teams: Iterable[String], forUser: Option[User]): Fu[List[CategView]] =
    for {
      pref   <- prefApi.getPref(forUser)
      categs <- env.categRepo withTeams teams
      views <- (categs map { categ =>
        env.postApi get (categ lastPostId forUser) map { topicPost =>
          lila.log("auth").warn("here we are")
          CategView(
            categ,
            topicPost map { case (topic, post) =>
              (topic, post, topic lastPage pref.postMaxPerPage)
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
    for {
      categ  <- env.categRepo bySlug slug
      pref   <- prefApi.getPref(forUser)
      topics <- env.paginator.categTopics(categ.get, page, pref.topicMaxPerPage, forUser)
    } yield Option(
      (
        categ.get,
        topics
      )
    )

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
