package lila.relay

import lila.study.Study

case class Relay(
    studyId: Study.Id,
    url: String
)
