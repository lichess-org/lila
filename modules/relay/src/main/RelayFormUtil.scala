package lila.relay

import play.api.data.Mapping
import play.api.data.format.Formatter
import chess.FideTC
import lila.common.Form.{ formatter, typeIn }

object RelayFormUtil:

  given Formatter[FideTC] = formatter.stringFormatter(_.toString, FideTC.valueOf)

  val fideTCMapping: Mapping[FideTC] = typeIn(FideTC.values.toSet)
