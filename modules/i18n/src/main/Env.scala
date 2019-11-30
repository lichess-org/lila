package lila.i18n

import play.api.Configuration

import lila.common.config.AppPath

final class Env(
    appConfig: Configuration,
    appPath: AppPath
) {

  lazy val jsDump = new JsDump(
    path = s"${appPath}/${appConfig.get[String]("i18n.web_path.relative")}"
  )

  def cli = new lila.common.Cli {
    def process = {
      case "i18n" :: "js" :: "dump" :: Nil =>
        jsDump.apply inject "Dumped JavaScript translations"
    }
  }
}
