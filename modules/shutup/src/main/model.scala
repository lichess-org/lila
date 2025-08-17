package lila.shutup

import lila.core.shutup.PublicSource

case class UserRecord(
    _id: UserId,
    /* pub: Option[List[PublicLine]], intentionally not mapped to DB */
    puf: Option[List[Double]],
    tef: Option[List[Double]],
    prm: Option[List[Double]],
    ubp: Option[List[Double]],
    prc: Option[List[Double]],
    puc: Option[List[Double]]
):

  inline def userId = _id

  def reports: List[TextReport] =
    List(
      TextReport(TextType.PublicForumMessage, ~puf),
      TextReport(TextType.TeamForumMessage, ~tef),
      TextReport(TextType.PrivateMessage, ~prm),
      TextReport(TextType.PrivateChat, ~prc),
      TextReport(TextType.PublicChat, ~puc)
    )

case class TextAnalysis(text: String, badWords: List[String]) extends lila.core.shutup.TextAnalysis:

  lazy val nbWords = text.split("""\s+""").length

  def ratio: Double = {
    if nbWords == 0 then 0 else badWords.size.toDouble / nbWords
  } * {
    if critical then 3 else 1
  }

  def dirty = ratio > 0

  lazy val critical = badWords.nonEmpty && Analyser.isCritical(text)

  def removeEngineIfBot(isBot: => Fu[Boolean]) =
    if badWords.has("engine")
    then
      isBot.dmap:
        if _ then copy(badWords = badWords.filterNot(_ == "engine"))
        else this
    else fuccess(this)

enum TextType(val key: String, val rotation: Int, val name: String):
  case PublicForumMessage extends TextType("puf", 20, "Public forum message")
  case TeamForumMessage extends TextType("tef", 20, "Team forum message")
  case PrivateMessage extends TextType("prm", 20, "Private message")
  case UblogPost extends TextType("ubp", 20, "User blog post")
  case PrivateChat extends TextType("prc", 40, "Private chat")
  case PublicChat extends TextType("puc", 60, "Public chat")

object TextType:
  def of: PublicSource => TextType =
    case PublicSource.Forum(_) => TextType.PublicForumMessage
    case PublicSource.Ublog(_) => TextType.UblogPost
    case _ => TextType.PublicChat

case class TextReport(textType: TextType, ratios: List[Double]):

  def minRatios = textType.rotation / 15
  def nbBad = ratios.count(_ > TextReport.unacceptableRatio)
  def tolerableNb = (ratios.size / 10).atLeast(3)

  def unacceptable = (ratios.sizeIs >= minRatios) && (nbBad > tolerableNb)

object TextReport:

  val unacceptableRatio = 1d / 30
