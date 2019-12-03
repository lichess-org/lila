package lila.forum

import lila.common.paginator._
import lila.db.dsl._

final class CategApi(
    postApi: => PostApi,
    topicApi: => TopicApi,
    categRepo: CategRepo,
    topicRepo: TopicRepo,
    postRepo: PostRepo
) {

  import BSONHandlers._

  def list(teams: Iterable[String], troll: Boolean): Fu[List[CategView]] = for {
    categs <- categRepo withTeams teams
    views <- (categs map { categ =>
      postApi get (categ lastPostId troll) map { topicPost =>
        CategView(categ, topicPost map {
          _ match {
            case (topic, post) => (topic, post, postApi lastPageOf topic)
          }
        }, troll)
      }
    }).sequenceFu
  } yield views

  def teamNbPosts(slug: String): Fu[Int] = categRepo nbPosts teamSlug(slug)

  def makeTeam(slug: String, name: String): Funit =
    categRepo.nextPosition flatMap { position =>
      val categ = Categ(
        _id = teamSlug(slug),
        name = name,
        desc = "Forum of the team " + name,
        pos = position,
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
        troll = false,
        hidden = false
      )
      val post = Post.make(
        topicId = topic.id,
        author = none,
        userId = lila.user.User.lichessId.some,
        ip = none,
        text = "Welcome to the %s forum!\nOnly members of the team can post here, but everybody can read." format name,
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
        categRepo.coll.update.one($id(categ.id), categ withTopic post).void
    }

  def show(slug: String, page: Int, troll: Boolean): Fu[Option[(Categ, Paginator[TopicView])]] =
    optionT(categRepo bySlug slug) flatMap { categ =>
      optionT(topicApi.paginator(categ, page, troll) map { (categ, _).some })
    } run

  def denormalize(categ: Categ): Funit = for {
    nbTopics <- topicRepo countByCateg categ
    nbPosts <- postRepo countByCateg categ
    lastPost <- postRepo lastByCateg categ
    nbTopicsTroll <- topicRepo withTroll true countByCateg categ
    nbPostsTroll <- postRepo withTroll true countByCateg categ
    lastPostTroll <- postRepo withTroll true lastByCateg categ
    _ <- categRepo.coll.update.one($id(categ.id), categ.copy(
      nbTopics = nbTopics,
      nbPosts = nbPosts,
      lastPostId = lastPost ?? (_.id),
      nbTopicsTroll = nbTopicsTroll,
      nbPostsTroll = nbPostsTroll,
      lastPostIdTroll = lastPostTroll ?? (_.id)
    )).void
  } yield ()
}
