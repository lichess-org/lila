package lila
package wiki

import scalaz.effects._

final class Api(pageRepo: PageRepo) {

  def show(slug: String, lang: String): IO[Option[(Page, List[Page])]] = for {
    page ← pageRepo.bySlugLang(slug, lang)
    pages ← pageRepo.forLang(lang)
    menu = makeMenu(pages)
  } yield page map { _ -> menu }

  private def makeMenu(pages: List[Page]): List[Page] = {
    val (defaultPages, langPages) = pages partition (_.isDefaultLang)
    defaultPages map { dPage ⇒
      langPages find (_.number == dPage.number) getOrElse dPage
    }
  }
}
