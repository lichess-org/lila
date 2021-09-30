package shogi
package format
package csa

import variant.Standard

class CsaParserTest extends ShogiTest {

  val parser                 = CsaParser.full _
  def parseMove(str: String) = CsaParser.MoveParser(str, Standard)

  "drop" in {
    parseMove("-0077FU") must beSuccess.like { case d: Drop =>
      d.role must_== Pawn
      d.pos must_== Pos.C3
    }
  }

  "move" in {
    parseMove("5948OU") must beSuccess.like { case a: CsaStd =>
      a.dest === Pos.F2
      a.orig === Pos.E1
      a.role === King
    }
  }

  "basic" should {
    "move" in {
      parser("PI,+5948OU") must beSuccess.like { case p =>
        p.parsedMoves.value.headOption must beSome.like { case a: CsaStd =>
          a.dest === Pos.F2
          a.orig === Pos.E1
          a.role === King
        }
      }
    }
    "drop" in {
      parser("PI,0077KI") must beSuccess.like { case p =>
        p.parsedMoves.value.headOption must beSome.like { case d: Drop =>
          d.role must_== Gold
          d.pos must_== Pos.C3
        }
      }
    }
    "moves with numbers clock info" in {
      parser("""PI,
      +7776FU,T12
      -8384FU,T5
      """) must beSuccess.like { case p =>
        p.parsedMoves.value.lastOption must beSome.like { case a: CsaStd =>
          a.dest === Pos.B6
          a.orig === Pos.B7
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
      -8384FU,T5""") must beSuccess.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Site && tag.value == """KAZUSA ARC"""
        }
      }
    }
    "name tag" in {
      parser("""PI,N-Me
      -8384FU,T5""") must beSuccess.like { case a =>
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
      """) must beSuccess.like { case a =>
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
      -8384FU""") must beSuccess.like { case a =>
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
      """) must beSuccess.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Sente && tag.value == """NOPE"""
        }
      }
    }
  }

  "comments" should {
    "multiple comments" in {
      parser("""PI
      +8483FU
      'such a neat comment
      ' one more
      '
      ' drop P*5e""") must beSuccess.like { case ParsedNotation(_, _, ParsedMoves(List(move))) =>
        move.metas.comments must_== List("such a neat comment", "one more", "drop P*5e")
      }
    }
    "termination comments" in {
      parser("""PI
      +8483FU
      'such a neat comment
      ' one more
      %TORYO,T3
      'comment on termination?""") must beSuccess.like { case ParsedNotation(_, _, ParsedMoves(List(move))) =>
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
      %CHUDAN""") must beSuccess.like { case ParsedNotation(InitialPosition(init), _, _) =>
        init must_== List("HEADER COMMENT", "HEADER2", "H3")
      }
    }
  }

}
