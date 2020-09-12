package lila.api

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.util.Try

import lila.hub.actorApi.Announce

object AnnounceStore {

  private var current = none[Announce]

  def get: Option[Announce] = {
    current foreach { c =>
      if (c.date.isBeforeNow) current = none
    }
    current
  }

  def set(announce: Option[Announce]) = {
    current = announce
  }

  // examples:
  // 5 minutes Lichess will restart
  // 20 seconds Cthulhu will awake
  def set(str: String): Option[Announce] = {
    set(str.split(" ").toList match {
      case length :: unit :: rest =>
        Try {
          val msg     = rest mkString " "
          val date    = DateTime.now plusSeconds Duration(s"$length $unit").toSeconds.toInt
          val isoDate = ISODateTimeFormat.dateTime print date
          val json    = Json.obj("msg" -> msg, "date" -> isoDate)
          Announce(msg, date, json)
        }.toOption
      case _ => none
    })
    get
  }

  def cancel = Announce("", DateTime.now, Json.obj())
}
