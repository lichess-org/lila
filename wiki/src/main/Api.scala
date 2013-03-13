package lila.wiki

import play.api.libs.concurrent.Execution.Implicits._

final class Api(pageRepo: PageRepo) {

  def show(slug: String): Fu[Option[(Page, List[Page])]] = for {
    page ← pageRepo.find byId slug
    pages ← pageRepo.find.all
  } yield page map { _ -> pages }
}
