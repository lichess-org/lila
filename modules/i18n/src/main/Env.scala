package lila.i18n

import com.typesafe.config.Config

final class Env(
    config: Config,
    appPath: String
) {

  private val WebPathRelative = config getString "web_path.relative"
  private val NetDomain = config getString "net.domain"

  lazy val jsDump = new JsDump(path = appPath + "/" + WebPathRelative)

  def cli = new lila.common.Cli {
    def process = {
      case "i18n" :: "js" :: "dump" :: Nil =>
        jsDump.apply inject "Dumped JavaScript translations"
    }
  }
}

object Env {

  import lila.common.PlayApp

  lazy val current = "i18n" boot new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    appPath = PlayApp withApp (_.path.getCanonicalPath)
  )
}
