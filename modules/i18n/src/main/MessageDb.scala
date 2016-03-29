package lila.i18n

import play.api.Application
import play.api.i18n.{ Messages => PlayMessages }

private object MessageDb {

  def apply(app: Application): Messages =
    lila.common.Chronometer.syncEffect(
      lila.i18n.db.Registry.load.+("default.play" -> loadMessages(app)("messages.default"))
    ) { lap =>
        logger.info(s"${lap.millis}ms MessageDb")
      }

  protected def loadMessages(app: Application)(file: String): Map[String, String] = {
    import scala.collection.JavaConverters._
    import play.utils.Resources
    app.classloader.getResources(file).asScala.toList
      .filterNot(url => Resources.isDirectory(app.classloader, url)).reverse
      .map { messageFile =>
        PlayMessages.parse(
          PlayMessages.UrlMessageSource(messageFile),
          messageFile.toString
        ).fold(e => throw e, identity)
      }.foldLeft(Map.empty[String, String]) { _ ++ _ }
  }
}
