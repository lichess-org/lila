package lila.web

import play.api.libs.json.Json

import scala.util.Try

import lila.common.Bus
import lila.core.socket.Announce

object AnnounceApi:

  private var current = none[Announce]

  def cli(words: List[String]): Fu[String] = words match
    case "cancel" :: Nil =>
      current = none
      Bus.pub(cancel)
      fuccess("Removed announce")
    case sentence =>
      set(sentence.mkString(" ")) match
        case Some(announce) =>
          Bus.pub(announce)
          fuccess(announce.json.toString)
        case None =>
          fuccess:
            "Invalid announce. Format: `announce <length> <unit> <words...>` or just `announce cancel` to cancel it"

  def get: Option[Announce] =
    if current.exists(_.date.isBeforeNow)
    then current = none
    current

  // examples:
  // 5 minutes Lichess will restart
  // 20 seconds Cthulhu will awake
  private def set(str: String): Option[Announce] =
    current = str.split(" ").toList match
      case length :: unit :: rest =>
        Try {
          val msg = rest.mkString(" ")
          val date = nowInstant.plusSeconds(Duration(s"$length $unit").toSeconds.toInt)
          val isoDate = isoDateTimeFormatter.print(date)
          val json = Json.obj("msg" -> msg, "date" -> isoDate)
          Announce(msg, date, json)
        }.toOption
      case _ => none
    get

  def cancel = Announce("", nowInstant, Json.obj())
