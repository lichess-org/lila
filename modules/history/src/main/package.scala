package lila

package object history extends PackageObject with WithPlay {

  private[history] type ID = String

  private[history] type Date = Int
  private[history] type Rating = Int
  private[history] type RatingsMap = List[(Date, Rating)]
}
