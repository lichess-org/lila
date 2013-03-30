package lila.forum

final class CategApi(env: Env) {

  // def list(teams: List[String]): Fu[List[CategView]] = for {
  //   categs ← env.categRepo withTeams teams
  //   views ← (categs map { categ ⇒
  //     env.postApi get categ.lastPostId map { topicPost ⇒
  //       CategView(categ, topicPost map {
  //         _ match {
  //           case (topic, post) ⇒ (topic, post, env.postApi lastPageOf topic)
  //         }
  //       })
  //     }
  //   }).sequence
  // } yield views

  // def getTeamNbPosts(slug: String): Fu[Int] =
  //   env.categRepo nbPosts ("team-" + slug)

  // def makeTeam(slug: String, name: String): Fu[Unit] = for {
  //   position ← env.categRepo.nextPosition
  //   categ = Categ(
  //     slug = "team-" + slug,
  //     name = name,
  //     desc = "Forum of the team " + name,
  //     pos = position,
  //     team = slug.some)
  //   topic = Topic(
  //     categId = categ.slug,
  //     slug = slug + "-forum",
  //     name = name + " forum")
  //   post = Post(
  //     topicId = topic.id,
  //     author = none,
  //     userId = "lichess".some,
  //     ip = none,
  //     text = "Welcome to the %s forum!\nOnly members of the team can post here, but everybody can read." format name,
  //     number = 1,
  //     categId = categ.id)
  //   _ ← env.categRepo saveFu categ
  //   _ ← env.postRepo saveFu post
  //   // denormalize topic
  //   _ ← env.topicRepo saveFu topic.copy(
  //     nbPosts = 1,
  //     lastPostId = post.id,
  //     updatedAt = post.createdAt)
  //   // denormalize categ
  //   _ ← env.categRepo saveFu categ.copy(
  //     nbTopics = categ.nbTopics + 1,
  //     nbPosts = categ.nbPosts + 1,
  //     lastPostId = post.id)
  // } yield ()

  // def show(slug: String, page: Int): Fu[Option[(Categ, Paginator[TopicView])]] =
  //   env.categRepo bySlug slug map {
  //     _ map { categ ⇒
  //       categ -> env.topicApi.paginator(categ, page)
  //     }
  //   }

  // def denormalize(categ: Categ): Fu[Unit] = for {
  //   topics ← env.topicRepo byCateg categ
  //   nbPosts ← env.postRepo countByTopics topics
  //   lastPost ← env.postRepo lastByTopics topics
  //   _ ← env.categRepo.saveFu(categ.copy(
  //     nbTopics = topics.size,
  //     nbPosts = nbPosts,
  //     lastPostId = lastPost.id
  //   ))
  // } yield ()

  // val denormalize: Fu[Unit] = for {
  //   categs ← env.categRepo.all
  //   _ ← categs.map(denormalize).sequence
  // } yield ()
}
