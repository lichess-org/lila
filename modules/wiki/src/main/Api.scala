package lila.wiki

import lila.db.api._
import lila.db.Implicits._
import lila.common.PimpedJson._
import tube.pageTube
import Page.DefaultLang

import play.api.libs.json._

private[wiki] final class Api {

  def show(slug: String, lang: String): Fu[Option[(Page, List[Page])]] = for {
    page ← $find.one(Json.obj("slug" -> slug, "lang" -> lang)) zip
      $find.one(Json.obj("slug" -> slug, "lang" -> DefaultLang)) map {
        case (a, b) ⇒ a orElse b
      }
    pages ← $find($query($or(Json.obj(
      "lang" -> lang,
      "lang" -> DefaultLang
    ))).sort($sort asc "number"))
  } yield page map { _ -> makeMenu(pages) }

  private def makeMenu(pages: List[Page]): List[Page] = {
    val (defaultPages, langPages) = pages partition (_.isDefaultLang)
    defaultPages map { dPage ⇒
      langPages find (_.number == dPage.number) getOrElse dPage
    }
  }
}
