package lila
package forum

import scalaz.effects._
import com.github.ornicar.paginator.Paginator

final class CategApi(env: ForumEnv) {

  def list(teams: List[String]): IO[List[CategView]] = for {
    categs ← env.categRepo withTeams teams
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

  def getTeamNbPosts(slug: String): IO[Int] =
    env.categRepo nbPosts ("team-" + slug)

  def makeTeam(slug: String, name: String): IO[Unit] = for {
    position ← env.categRepo.nextPosition
    categ = Categ(
      slug = "team-" + slug,
      name = name,
      desc = "Forum of the team " + name,
      pos = position,
      team = slug.some)
    topic = Topic(
      categId = categ.slug,
      slug = slug + "-forum",
      name = name + " forum")
    post = Post(
      topicId = topic.id,
      author = none,
      userId = "lichess".some,
      ip = none,
      text = "Welcome to the %s forum!\nOnly members of the team can post here, but everybody can read." format name,
      number = 1,
      categId = categ.id)
    _ ← env.categRepo saveIO categ
    _ ← env.postRepo saveIO post
    // denormalize topic
    _ ← env.topicRepo saveIO topic.copy(
      nbPosts = 1,
      lastPostId = post.id,
      updatedAt = post.createdAt)
    // denormalize categ
    _ ← env.categRepo saveIO categ.copy(
      nbTopics = categ.nbTopics + 1,
      nbPosts = categ.nbPosts + 1,
      lastPostId = post.id)
  } yield ()

  def show(slug: String, page: Int): IO[Option[(Categ, Paginator[TopicView])]] =
    env.categRepo bySlug slug map {
      _ map { categ ⇒
        categ -> env.topicApi.paginator(categ, page)
      }
    }

  def denormalize(categ: Categ): IO[Unit] = for {
    topics ← env.topicRepo byCateg categ
    nbPosts ← env.postRepo countByTopics topics
    lastPost ← env.postRepo lastByTopics topics
    _ ← env.categRepo.saveIO(categ.copy(
      nbTopics = topics.size,
      nbPosts = nbPosts,
      lastPostId = lastPost.id
    ))
  } yield ()

  val denormalize: IO[Unit] = for {
    categs ← env.categRepo.all
    _ ← categs.map(denormalize).sequence
  } yield ()
}
