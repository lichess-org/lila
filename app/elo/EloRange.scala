package lila
package elo

trait EloRange {

  // ^\d{3,4}\-\d{3,4}$
  val eloRange: Option[String]

  def eloMin: Option[Int] = eloRange flatMap { e ⇒
    parseIntOption(e takeWhile ('-' !=))
  }

  def eloMax: Option[Int] = eloRange flatMap { e ⇒
    parseIntOption(e dropWhile ('-' !=) tail)
  }
}
