package lila

import memo._
import chess.{ White, Black }

class AliveMemoTest extends SystemTest {

  val memo = SystemEnv().aliveMemo

  sequential

  "non existing key" in {
    memo.get("arst", White) must beNone
  }

  "put key" in {
    memo.put("arst", White) must beIO.success
  }

  "read key" in {
    sequential
    "success put" in {
      memo.put("arst", White) must beIO.success
    }
    "count" in {
      memo.count must_== 1
    }
    "get" in {
      memo.get("arst", White) must beSome
    }
  }
}
