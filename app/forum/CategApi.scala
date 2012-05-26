package lila
package forum

import scalaz.effects._

final class CategApi(env: ForumEnv) {

  val list: IO[List[CategView]] = for {
    categs ← env.categRepo.all
    views ← (categs map { categ ⇒
      io(CategView(categ))
    }).sequence
  } yield views
}
