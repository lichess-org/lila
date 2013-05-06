package lila.forum

import lila.user.{ User, Context }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Implicits._
import lila.db.api._
import tube._

import scalaz.{ OptionT, OptionTs }

private[forum] final class CategApi(env: Env) extends OptionTs {

  def list(teams: List[String]): Fu[List[CategView]] = for {
    categs ← CategRepo withTeams teams
    views ← (categs map { categ ⇒
      env.postApi get categ.lastPostId map { topicPost ⇒
        CategView(categ, topicPost map {
          _ match {
            case (topic, post) ⇒ (topic, post, env.postApi lastPageOf topic)
          }
        })
      }
    }).sequence
  } yield views

  def teamNbPosts(slug: String): Fu[Int] = CategRepo nbPosts teamSlug(slug)

  def makeTeam(slug: String, name: String): Funit =
    CategRepo.nextPosition flatMap { position ⇒
      val categ = Categ(
        id = teamSlug(slug),
        name = name,
        desc = "Forum of the team " + name,
        pos = position,
        team = slug.some)
      val topic = Topic.make(
        categId = categ.slug,
        slug = slug + "-forum",
        name = name + " forum")
      val post = Post.make(
        topicId = topic.id,
        author = none,
        userId = "lichess".some,
        ip = none,
        text = "Welcome to the %s forum!\nOnly members of the team can post here, but everybody can read." format name,
        number = 1,
        categId = categ.id)
      $insert(categ) >>
        $insert(post) >>
        $insert(topic.copy(
          nbPosts = 1,
          lastPostId = post.id,
          updatedAt = post.createdAt)) >>
        $update(categ.copy(
          nbTopics = categ.nbTopics + 1,
          nbPosts = categ.nbPosts + 1,
          lastPostId = post.id))
    }

  def show(slug: String, page: Int): Fu[Option[(Categ, Paginator[TopicView])]] =
    optionT(CategRepo bySlug slug) flatMap { categ ⇒
      optionT(env.topicApi.paginator(categ, page) map { (categ, _).some })
    }

  def denormalize(categ: Categ): Funit = for {
    topics ← TopicRepo byCateg categ
    topicIds = topics map (_.id)
    nbPosts ← PostRepo countByTopics topicIds
    lastPost ← PostRepo lastByTopics topicIds
    _ ← $update(categ.copy(
      nbTopics = topics.size,
      nbPosts = nbPosts,
      lastPostId = lastPost zmap (_.id)
    ))
  } yield ()

  val denormalize: Funit = $find.all[Categ] flatMap { categs ⇒
    categs.map(denormalize).sequence
  } void
}
