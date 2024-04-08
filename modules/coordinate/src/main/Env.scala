package lila.coordinate

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.given
import lila.core.config.CollName

final class Env(
    appConfig: Configuration,
    db: lila.db.Db
)(using Executor):

  private lazy val scoreColl = db(appConfig.get[CollName]("coordinate.collection.score"))

  lazy val api = wire[CoordinateApi]

  lazy val forms = CoordinateForm

enum CoordMode:
  case findSquare, nameSquare
object CoordMode:
  def find(name: String) = values.find(_.toString == name)
