package lila.relay

import lila.study.{ Study }

case class Relay(
    studyId: Study.Id,
    url: String
) {

  override def toString = s"study:$studyId url:$url"
}
