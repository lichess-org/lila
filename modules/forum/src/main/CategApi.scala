package lila.forum

import lila.common.paginator._
import lila.db.api._
import lila.db.Implicits._
import lila.db.paginator._
import lila.user.{ User, UserContext }
import tube._

private[forum] final class CategApi(env: Env) {

  def list(teams: List[String], troll: Boolean): Fu[List[CategView]] = for {
    categs ← CategRepo withTeams teams
    views ← (categs map { categ =>
      env.postApi get (categ lastPostId troll) map { topicPost =>
        CategView(categ, topicPost map {
          _ match {
            case (topic, post) => (topic, post, env.postApi lastPageOf topic)
          }
        }, troll)
      }
    }).sequenceFu
  } yield views

  def teamNbPosts(slug: String): Fu[Int] = CategRepo nbPosts teamSlug(slug)

  def makeTeam(slug: String, name: String): Funit =
    CategRepo.nextPosition flatMap { position =>
      val categ = Categ(
        id = teamSlug(slug),
        name = name,
        desc = "Forum of the team " + name,
        pos = position,
        team = slug.some,
        nbTopics = 0,
        nbPosts = 0,
        lastPostId = "",
        nbTopicsTroll = 0,
        nbPostsTroll = 0,
        lastPostIdTroll = "")
      val topic = Topic.make(
        categId = categ.slug,
        slug = slug + "-forum",
        name = name + " forum",
        troll = false)
      val post = Post.make(
        topicId = topic.id,
        author = none,
        userId = "lichess".some,
        ip = none,
        text = "Welcome to the %s forum!\nOnly members of the team can post here, but everybody can read." format name,
        number = 1,
        troll = false,
        hidden = topic.hidden,
        lang = "en".some,
        categId = categ.id)
      $insert(categ) >>
        $insert(post) >>
        $insert(topic withPost post) >>
        $update(categ withTopic post)
    }

  def show(slug: String, page: Int, troll: Boolean): Fu[Option[(Categ, Paginator[TopicView])]] =
    optionT(CategRepo bySlug slug) flatMap { categ =>
      optionT(env.topicApi.paginator(categ, page, troll) map { (categ, _).some })
    }

  def denormalize(categ: Categ): Funit = for {
    topics ← TopicRepo byCateg categ
    topicIds = topics map (_.id)
    nbPosts ← PostRepo countByTopics topicIds
    lastPost ← PostRepo lastByTopics topicIds
    topicsTroll ← TopicRepoTroll byCateg categ
    topicIdsTroll = topicsTroll map (_.id)
    nbPostsTroll ← PostRepoTroll countByTopics topicIdsTroll
    lastPostTroll ← PostRepoTroll lastByTopics topicIdsTroll
    _ ← $update(categ.copy(
      nbTopics = topics.size,
      nbPosts = nbPosts,
      lastPostId = lastPost ?? (_.id),
      nbTopicsTroll = topicsTroll.size,
      nbPostsTroll = nbPostsTroll,
      lastPostIdTroll = lastPostTroll ?? (_.id)
    ))
  } yield ()

  def denormalize: Funit = $find.all[Categ] flatMap { categs =>
    categs.map(denormalize).sequenceFu
  } void
}
