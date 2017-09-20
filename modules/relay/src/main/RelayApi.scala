package lila.relay

import lila.study.{ Study, StudyApi }

private final class RelayApi(
    studyApi: StudyApi
) {

  val currents = List(
    Relay(
      lila.study.Study.Id("AoUZ6bOS"),
      url = "http://localhost:3000"
    )
  )

  def sync(relay: Relay, pgn: String): Funit = {
    println(s"Sync $relay ${pgn.lines.drop(11).take(6).mkString("\n")}")
    funit
  }
}
