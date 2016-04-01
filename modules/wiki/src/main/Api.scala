package lila.wiki

import Page.DefaultLang
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.db.dsl._
import lila.db.Implicits._
import tube.pageTube

private[wiki] final class Api {

  def show(slug: String, lang: String): Fu[Option[(Page, List[Page])]] = for {
    page ← $find.one(Json.obj("slug" -> slug, "lang" -> lang)) zip
      $find.one(Json.obj("slug" -> slug, "lang" -> DefaultLang)) map {
        case (a, b) => a orElse b
      }
    pages ← $find($query(Json.obj(
      "lang" -> $in(Seq(lang, DefaultLang))
    )).sort($sort asc "number"))
  } yield page map { _ -> makeMenu(pages) }

  private def makeMenu(pages: List[Page]): List[Page] = {
    val (defaultPages, langPages) = pages partition (_.isDefaultLang)
    defaultPages map { dPage =>
      langPages.find(_.number == dPage.number) | dPage
    }
  }
}
