package lila.shutup

case class UserRecord(
    _id: String,
    /* pub: Option[List[PublicLine]], intentionally not mapped to DB */
    puf: Option[List[Double]],
    tef: Option[List[Double]],
    prm: Option[List[Double]],
    prc: Option[List[Double]],
    puc: Option[List[Double]]
) {

  def userId = _id

  def reports: List[TextReport] =
    List(
      TextReport(TextType.PublicForumMessage, ~puf),
      TextReport(TextType.TeamForumMessage, ~tef),
      TextReport(TextType.PrivateMessage, ~prm),
      TextReport(TextType.PrivateChat, ~prc),
      TextReport(TextType.PublicChat, ~puc)
    )
}

case class TextAnalysis(
    text: String,
    badWords: List[String]
) {

  lazy val nbWords = text.split("""\s+""").size

  def nbBadWords = badWords.size

  def ratio: Double = if (nbWords == 0) 0 else nbBadWords.toDouble / nbWords

  def dirty = ratio > 0
}

sealed abstract class TextType(
    val key: String,
    val rotation: Int,
    val name: String
)

object TextType {

  case object PublicForumMessage extends TextType("puf", 20, "Public forum message")
  case object TeamForumMessage   extends TextType("tef", 20, "Team forum message")
  case object PrivateMessage     extends TextType("prm", 20, "Private message")
  case object PrivateChat        extends TextType("prc", 40, "Private chat")
  case object PublicChat         extends TextType("puc", 60, "Public chat")
}

case class TextReport(textType: TextType, ratios: List[Double]) {

  def minRatios   = textType.rotation / 15
  def nbBad       = ratios.count(_ > TextReport.unacceptableRatio)
  def tolerableNb = (ratios.size / 10) atLeast 3

  def unacceptable = (ratios.sizeIs >= minRatios) && (nbBad > tolerableNb)
}

object TextReport {

  val unacceptableRatio = 1d / 30
}
