package lila

package object game extends PackageObject {

  type Usis        = Vector[shogi.format.usi.Usi]
  type RatingDiffs = shogi.Color.Map[Int]

  private[game] def logger = lila.log("game")
}
