package lila

package object learn extends PackageObject {

  private[learn] val logger = lila.log("learn")
}

package learn {
  case class ScoreEntry(stage: String, level: Int, score: Int)
}
