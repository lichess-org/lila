package shogi
package format
package csa

class CsaParserTest extends ShogiTest {

  import CsaFixtures._

  val parser                 = CsaParser.full _
  def parseMove(str: String) = CsaParser.MoveParser(str)

  "drop" in {
    parseMove("-0077FU") must beValid.like { case d: Drop =>
      d.role must_== Pawn
      d.pos must_== Pos.SQ7G
    }
  }

  "move" in {
    parseMove("5948OU") must beValid.like { case a: CsaMove =>
      a.dest === Pos.SQ4H
      a.orig === Pos.SQ5I
      a.role === King
    }
  }

  "basic" should {
    "move" in {
      parser("PI,+5948OU") must beValid.like { case p =>
        p.parsedMoves.value.headOption must beSome.like { case a: CsaMove =>
          a.dest === Pos.SQ4H
          a.orig === Pos.SQ5I
          a.role === King
        }
      }
    }
    "drop" in {
      parser("PI,0077KI") must beValid.like { case p =>
        p.parsedMoves.value.headOption must beSome.like { case d: Drop =>
          d.role must_== Gold
          d.pos must_== Pos.SQ7G
        }
      }
    }
    "moves with numbers clock info" in {
      parser("""PI,
      +7776FU,T12
      -8384FU,T5
      """) must beValid.like { case p =>
        p.parsedMoves.value.lastOption must beSome.like { case a: CsaMove =>
          a.dest === Pos.SQ8D
          a.orig === Pos.SQ8C
          a.role === Pawn
          a.metas.timeSpent must beSome.like { case c: Centis =>
            c === Centis(500)
          }
        }
      }
    }
  }

  "tags" should {
    "one tag" in {
      parser("""PI,$SITE:KAZUSA ARC
      -8384FU,T5""") must beValid.like { case a =>
        a.tags.value.size must_== 1
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Site && tag.value == """KAZUSA ARC"""
        }
      }
    }
    "name tag" in {
      parser("""PI,N-Me
      -8384FU,T5""") must beValid.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Gote && tag.value == """Me"""
        }
      }
    }
    "multiple tags" in {
      parser("""
        PI
        N-Me
        $SITE:lishogi
        N+Also me
        -8384FU
      """) must beValid.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Gote && tag.value == """Me"""
        }
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Sente && tag.value == """Also me"""
        }
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Site && tag.value == """lishogi"""
        }
      }
    }
    "empty tag is ignored" in {
      parser("""PI
      N-
      -8384FU""") must beValid.like { case a =>
        a.tags.value must not contain { (tag: Tag) =>
          tag.name == Tag.Gote
        }
      }
    }
    "empty tag with another nonempty tag" in {
      parser("""
        PI,
        N-
        N+NOPE
        -8384FU
      """) must beValid.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Sente && tag.value == """NOPE"""
        }
      }
    }
  }

  "comments" should {
    "multiple comments" in {
      parser("""PI,+8483FU
      'such a neat comment
      ' one more, keep com,ma
      '
      ' drop P*5e""") must beValid.like { case ParsedNotation(_, _, ParsedMoves(List(move))) =>
        move.metas.comments must_== List("such a neat comment", "one more, keep com,ma", "drop P*5e")
      }
    }
    "termination comments" in {
      parser("""PI
      +8483FU
      'such a neat comment
      ' one more
      %TORYO,T3
      'comment on termination?""") must beValid.like { case ParsedNotation(_, _, ParsedMoves(List(move))) =>
        move.metas.comments must_== List("such a neat comment", "one more", "comment on termination?")
      }
    }
    "comments in header" in {
      parser("""'HEADER COMMENT
      'HEADER2
      $SITE:lishogi
      PI33FU
      ' H3
      +8483FU
      'Something comments
      +8483FU
      %CHUDAN""") must beValid.like { case ParsedNotation(InitialPosition(init), Tags(tags), _) =>
        init must_== List("HEADER COMMENT", "HEADER2", "H3")
        tags.size must_== 3
      }
    }
  }

  "from initial board" in {
    parser("""V2.2
N+鈴木大介 九段
N-深浦康市 九段
$EVENT:王座戦
$SITE:東京・将棋会館
$START:2017-03-22T01:00:00.000Z
$OPENING:中飛車
P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
P2 * -HI *  *  *  *  * -KA * 
P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
P4 *  *  *  *  *  *  *  *  * 
P5 *  *  *  *  *  *  *  *  * 
P6 *  *  *  *  *  *  *  *  * 
P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
P8 * +KA *  *  *  *  * +HI * 
P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
+
+7776FU
-8384FU
    """) must beValid.like { case ParsedNotation(_, Tags(tags), _) =>
      tags.size must_== 6
      tags must not contain { (tag: Tag) =>
        tag.name == Tag.Sfen
      }
      tags must contain { (tag: Tag) =>
        tag.name == Tag.Sente && tag.value == "鈴木大介 九段"
      }
      tags must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == "深浦康市 九段"
      }
      tags must contain { (tag: Tag) =>
        tag.name == Tag.Site && tag.value == "東京・将棋会館"
      }
    }
  }

  "csa fixture 1" in {
    parser(csa1) must beValid.like { case ParsedNotation(_, Tags(tags), ParsedMoves(pm)) =>
      pm.size must_== 111
      tags.size must_== 8
    }
  }

  "csa fixture 2" in {
    parser(csa2) must beValid.like { case ParsedNotation(_, Tags(tags), ParsedMoves(pm)) =>
      pm.size must_== 258
      tags.size must_== 4
    }
  }

}
