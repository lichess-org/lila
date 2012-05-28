package lila
package wiki

import scalaz.effects._

final class Api(pageRepo: PageRepo) {

  def show(slug: String): IO[Option[(Page, List[Page])]] = for {
    page ← pageRepo byId slug
    pages ← pageRepo.all
  } yield page map { _ -> pages }
}
