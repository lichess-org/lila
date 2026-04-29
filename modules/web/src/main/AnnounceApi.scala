package lila.web

import play.api.libs.json.{ Json, JsObject }

import scala.util.Try

import lila.common.Bus
import lila.core.socket.{ Announce, AnnounceUpdate }
import lila.memo.SettingStore

final class LichobileAnnounceApi(settingStore: SettingStore.Builder):

  val lichobileUpgrade = settingStore[Int](
    "lichobileUpgradeAnnounce",
    default = 0,
    text = "Tell lichobile users about the new mobile app, modulo minutes".some
  )

  private def showNow =
    val setting = lichobileUpgrade.get()
    setting > 0 && (nowSeconds / 60) % setting == 0

  def get: Option[JsObject] =
    showNow.option:
      val msg = "This app is no longer supported! Please upgrade: lichess.org/app"
      val date = nowInstant.plusMinutes(1)
      AnnounceApi.makeJson(msg, date)

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

  private[web] def makeJson(msg: String, date: Instant) =
    Json.obj("msg" -> msg, "date" -> isoDateTimeFormatter.print(date))

  // examples:
  // 5 minutes Lichess will restart
  // 20 seconds Cthulhu will awake
  private def set(str: String): Option[Announce] =
    current = str.split(" ").toList match
      case length :: unit :: rest =>
        Try {
          val msg = rest.mkString(" ")
          val date = nowInstant.plusSeconds(Duration(s"$length $unit").toSeconds.toInt)
          Announce(msg, date, makeJson(msg, date))
        }.toOption
      case _ => none
    get

  def cancel = Announce("", nowInstant, Json.obj())

  private[web] def setupPeriodicUpdate()(using scheduler: Scheduler)(using Executor): Unit =
    import scala.concurrent.duration.*
    scheduler.scheduleAtFixedRate(7.seconds, 3.seconds): () =>
      Bus.pub(AnnounceUpdate(get))
