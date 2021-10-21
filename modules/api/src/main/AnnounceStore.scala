package lila.api

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.Json
import scala.concurrent.duration._
import scala.util.Try

import lila.hub.actorApi.Announce
import lila.i18n.I18nKeys

object AnnounceStore {

  private var current = none[Announce]

  private val predefinedMessageI18nMap = Map(
    "restart" -> I18nKeys.lichessWillRestart
  )

  val predefinedMessageKeys = predefinedMessageI18nMap.keys.toList

  val predefinedMessageI18Keys = predefinedMessageI18nMap.values.toList

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
  // msg: "Lichess will restart", durationStr: "5 minutes"
  // msg: "Cthulhu will awake", durationStr: "20 seconds"
  def set(msg: String, durationStr: String): Option[Announce] = {
    set(
      Try {
        val date    = DateTime.now plusSeconds Duration(durationStr).toSeconds.toInt
        val isoDate = ISODateTimeFormat.dateTime print date
        val json    = Json.obj("msg" -> msg, "date" -> isoDate)
        Announce(msg, date, json)
      }.toOption
    )
    get
  }

  // examples:
  // msgKey: "restart", durationStr: "5 minutes"
  def setPredefined(msgKey: String, durationStr: String): Option[Announce] = {
    set(predefinedMessageI18nMap.get(msgKey) match {
      case Some(i18nKey) =>
        Try {
          val date    = DateTime.now plusSeconds Duration(durationStr).toSeconds.toInt
          val isoDate = ISODateTimeFormat.dateTime print date
          val json = Json.obj(
            "msg"     -> i18nKey.txt()(lila.i18n.defaultLang),
            "i18nKey" -> i18nKey.key,
            "date"    -> isoDate
          )
          Announce(msgKey, date, json)
        }.toOption
      case None => none
    })
    get
  }

  def cancel = Announce("", DateTime.now, Json.obj())
}
