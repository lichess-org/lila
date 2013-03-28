package lila.wiki

import lila.db.Repo
import lila.db.Types._

private[wiki] final class PageRepo(implicit coll: Coll) extends Repo[String, Page](Pages.json) {

  def clear: Funit = remove(select.all)
}
