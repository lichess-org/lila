package lila.i18n

import play.api.Configuration

final class Env(
    appConfig: Configuration,
    application: play.Application
) {

  lazy val jsDump = new JsDump(
    path = s"${application.path}/${appConfig.get[String]("i18n.web_path.relative")}"
  )

  def cli = new lila.common.Cli {
    def process = {
      case "i18n" :: "js" :: "dump" :: Nil =>
        jsDump.apply inject "Dumped JavaScript translations"
    }
  }
}
