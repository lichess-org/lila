package lila.fishnet

import chess.eval.Score
import play.api.libs.json.*

import JsonApi.readers.scoreReads

class JsonApiTest extends munit.FunSuite:

  def read(json: String) = Json.parse(json).validate(using scoreReads).asOpt

  test("read scores"):
    assertEquals(read("""{"cp":-24}"""), Some(Some(Score.cp(-24))))
    assertEquals(read("""{"mate":-3}"""), Some(Some(Score.mate(-3))))
    assertEquals(read("""{"mate":0,"mateGiven":false}"""), Some(Some(Score.mated)))
    assertEquals(read("""{"mate":0,"mateGiven":true}"""), Some(Some(Score.MateGiven)))
    assertEquals(read("""{"mate":0}"""), Some(None)) // older fishnet clients
    assertEquals(read("""{}"""), None)
