package lila.wiki

import Page.DefaultLang

import lila.db.dsl._

private[wiki] final class Api(coll: Coll) {

  import Page.PageBSONHandler

  def show(slug: String, lang: String): Fu[Option[(Page, List[Page])]] = for {
    page ← coll.uno[Page]($doc("slug" -> slug, "lang" -> lang)) orElse
      coll.uno[Page]($doc("slug" -> slug, "lang" -> DefaultLang))
    pages ← coll.find($doc(
      "lang" $in Seq(lang, DefaultLang)
    )).sort($sort asc "number").cursor[Page]().gather[List]()
  } yield page map { _ -> makeMenu(pages) }

  private def makeMenu(pages: List[Page]): List[Page] = {
    val (defaultPages, langPages) = pages partition (_.isDefaultLang)
    defaultPages map { dPage =>
      langPages.find(_.number == dPage.number) | dPage
    }
  }
}
