package lila.wiki

import lila.db.Types.Coll
import lila.db.api._
import tube._
import play.api.libs.concurrent.Execution.Implicits._

final class Api(implicit coll: Coll) {

  def show(slug: String): Fu[Option[(Page, List[Page])]] = for {
    page ← $find byId slug
    pages ← $find.all
  } yield page map { _ -> pages }
}
