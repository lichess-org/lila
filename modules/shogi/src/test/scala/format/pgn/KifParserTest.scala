package shogi
package format.pgn
import variant.Standard

class KifParserTest extends ShogiTest {

 // import Fixtures._

  val parser = KifParser.full _
  def parseMove(str: String, lastDest: Option[Pos] = None) = KifParser.MoveParser(str, lastDest, Standard)

  "drop" in {
    parseMove("６四歩打") must beSuccess.like { case d: Drop =>
      d.role must_== Pawn
      d.pos must_== Pos.D6
    }
  }
  
  "move" in {
    parseMove("７七金(78)") must beSuccess.like { case a: Std =>
      a.dest === Pos.C3
      a.file === Some(3)
      a.rank === Some(2)
      a.role === Gold
      a.promotion === false
    }
  }

  "basic" should {
    "move" in {
      parser("☗７七金(78)") must beSuccess.like { case p =>
        p.sans.value.headOption must beSome.like { case a: Std =>
          a.dest === Pos.C3
          a.file === Some(3)
          a.rank === Some(2)
          a.role === Gold
          a.promotion === false
        }
      }
    }
    "drop" in {
      parser("７四歩打") must beSuccess.like { case p =>
        p.sans.value.headOption must beSome.like { case d: Drop =>
          d.role must_== Pawn
          d.pos must_== Pos.C6
        }
      }
    }
    "move with number" in {
      parser("1 ７七G(78)") must beSuccess.like { case p =>
        p.sans.value.headOption must beSome.like { case a: Std =>
          a.dest === Pos.C3
          a.file === Some(3)
          a.rank === Some(2)
          a.role === Gold
          a.promotion === false
        }
      }
    }
    "move with number and a dot" in {
      parser("42. ７7金(78)") must beSuccess.like { case p =>
        p.sans.value.headOption must beSome.like { case a: Std =>
          a.dest === Pos.C3
          a.file === Some(3)
          a.rank === Some(2)
          a.role === Gold
          a.promotion === false
        }
      }
    }
    "moves with numbers clock info" in {
      parser("""
      1 ７六歩(77) (0:12/0:0:12)
      2 ７六飛(77) (0:12/)
      """) must beSuccess.like { case p =>
        p.sans.value.lastOption must beSome.like { case a: Std =>
          a.dest === Pos.C4
          a.file === Some(3)
          a.rank === Some(3)
          a.role === Rook
          a.promotion === false
        }
      }
    }
  }

  "promotion" should {
    "as a true" in {
      parser("3 ２二角成(88) ") must beSuccess.like { case a =>
        a.sans.value.headOption must beSome.like { case san: Std =>
          san.promotion must_== true
        }
      }
    }
    "as a false" in {
      parser("3 ２二角不成(88) ") must beSuccess.like { case a =>
        a.sans.value.headOption must beSome.like { case san: Std =>
          san.promotion must_== false
        }
      }
    }
  }

  "同" should {
    "simple move with 同" in {
      parseMove("同 金(78)", Some(Pos.C3)) must beSuccess.like { case a: Std =>
        a.dest === Pos.C3
        a.file === Some(3)
        a.rank === Some(2)
        a.role === Gold
        a.promotion === false
      }
    }
    "from last move" in {
      parser("""
      1 ７六歩(77) (0:12/0:0:12)
      2 同 飛(77) (0:12/0:0:12)
      """) must beSuccess.like { case p =>
        p.sans.value.lastOption must beSome.like { case a: Std =>
          a.dest === Pos.C4
          a.file === Some(3)
          a.rank === Some(3)
          a.role === Rook
          a.promotion === false
        }
      }
    }
    "consecutive moves" in {
      parser("""
      1 ７六歩(77) (0:12/0:0:12)
      2 同 飛(72) (0:12/0:0:12)
      3 同 角(55) (0:12/0:0:12)
      """) must beSuccess.like { case p =>
        p.sans.value must haveSize(3)
        p.sans.value.lastOption must beSome.like { case a: Std =>
          a.dest === Pos.C4
          a.file === Some(5)
          a.rank === Some(5)
          a.role === Bishop
          a.promotion === false
        }
      }
    }
  }
  "tags" should {
    "one tag" in {
      parser("""後手：  Me""") must beSuccess.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Gote && tag.value == """Me"""
        }
      }
    }
    "multiple tags" in {
      parser("""
        後手：俺
        Sente: Also me
      """) must beSuccess.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Gote && tag.value == """俺"""
        }
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Sente && tag.value == """Also me"""
        }
      }
    }
    "empty tag" in {
      parser("""後手：""") must beSuccess.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Gote && tag.value == """"""
        }
      }
    }
    "empty tag with another nonempty tag" in {
      parser("""
        後手：
        Sente: NOPE
      """) must beSuccess.like { case a =>
        a.tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Sente && tag.value == """NOPE"""
        }
      }
    }
  }

  "comments" in {
    parser("""７7金(78)
    *such a neat comment
    * one more
    * drop P*5e""") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
      san.metas.comments must_== List("such a neat comment", "one more", "drop P*5e")
    }
  }

  "times" should {
    "both times" in {
      parser("""29 ４八玉(59) (2:25/0:8:23)""") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
        san.metas.timeSpent must_== Some(Centis(14500))
        san.metas.timeTotal must_== Some(Centis(50300))
      }
    }
    "fractional time" in {
      parser("""29 ４八玉(59) (2:25.006/0:8:23.006)""") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
        san.metas.timeSpent must_== Some(Centis(14501))
        san.metas.timeTotal must_== Some(Centis(50301))
      }
    }
    "only move time" in {
      parser("""29 ４八玉(59) ( 2:25/)""") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
        san.metas.timeSpent must_== Some(Centis(14500))
        san.metas.timeTotal must_== None
      }
    }
    "no time" in {
      parser("""29 ４八玉(59)""") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
        san.metas.timeSpent must_== None
        san.metas.timeTotal must_== None
      }
    }
    "+ at the end" in {
      parser("""29 ４八玉(59) (2:25/0:8:23)+""") must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
        san.metas.timeSpent must_== Some(Centis(14500))
        san.metas.timeTotal must_== Some(Centis(50300))
      }
    }
  }

  "properly ignore comments, bookmarks and まで..." in {
    parser("""
      後手：
      # Ignore comments
      ## double comments 
      # comments in # comments 
      & and bookmarks # This is not necessary
      先手：先手 # Mid-line
      1 ７六歩(77) (00:00/00:00:00)
      まで122手で中断
    """) must beSuccess.like { case a =>
      a.tags.value.length must_== 2
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Sente && tag.value == "先手"
      }
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == ""
      }
    }
  }

  "from position board" in {
    parser("""
      後手：
      後手の持駒：金四　銀二　香三　歩十三
        ９ ８ ７ ６ ５ ４ ３ ２ １
      +---------------------------+
      | ・ ・ ・v桂 ・ ・ ・ ・ ・|一
      |v玉 角v歩 馬 ・ ・ ・ ・ ・|二
      | ・ ・ ・ ・ ・ ・ ・ ・ ・|三
      | 桂 ・ ・v歩 ・ ・ ・ ・ ・|四
      |vとv桂 ・ ・v歩 ・ ・ ・ ・|五
      | ・ ・ 飛 ・v全 ・ ・ ・ ・|六
      |v歩 桂 ・ ・ ・ ・ ・ ・ ・|七
      | ・ 香 ・ ・ ・ ・ ・ ・ ・|八
      | ・v銀 ・ ・ 龍 ・ ・ ・ ・|九
      +---------------------------+
      先手：先手
      先手の持駒：なし
    """) must beSuccess.like { case a =>
      a.tags.value.length must_== 3
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.FEN && tag.value == "3n5/kBp+B5/9/N2p5/+pn2p4/2R1+s4/pN7/1L7/1s2+R4 b 4g2s3l13p"
      }
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Sente && tag.value == "先手"
      }
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.Gote && tag.value == ""
      }
    }
  }

  "from handicap" in {
    parser("""
      手合割：歩三兵
      先手：
      後手：
      手数----指手---------消費時間--
      1 ７六歩(77) (00:00/00:00:00)
    """) must beSuccess.like { case a =>
      a.tags.value must contain { (tag: Tag) =>
        tag.name == Tag.FEN && tag.value == "4k4/9/9/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w 3p 2"
      }
    }
  }

  "from standard position" in {
    parser("""
      手合割：平手
      先手：
      後手：
      手数----指手---------消費時間--
      1 ７六歩(77) (00:00/00:00:00)
    """) must beSuccess.like { case a =>
      a.tags.value must not contain { (tag: Tag) =>
        tag.name == Tag.FEN
      }
    }
  }

  "variations" should {
    "first move variation" in {
      parser("""
        後手：俺
        Sente: Also me

        手数----指手---------消費時間--
          1 ７六歩(77)        ( 0:00/00:00:00)
        変化：1手
          1 ２六歩(27)        ( 0:00/00:00:00)
      """) must beSuccess.like { case ParsedPgn(_, _, Sans(List(san))) =>
        san.metas.variations.headOption must beSome.like { case variation =>
          variation.value must haveSize(1)
        }
      }
    }
    "nested variations" in {
      parser("""
    手合割：平手
    先手：
    後手：
    手数----指手---------消費時間--
      1 ７六歩(77)        ( 0:00/00:00:00)
      2 ３四歩(33)        ( 0:00/00:00:00)
      3 ２二角成(88)       ( 0:00/00:00:00)
      4 同　銀(31)        ( 0:00/00:00:00)
      5 ２六歩(27)        ( 0:00/00:00:00)
      6 １四歩(13)        ( 0:00/00:00:00)
      7 １六歩(17)        ( 0:00/00:00:00)

    変化：6手
      6 ５四歩(53)        ( 0:00/00:00:00)
      7 ９六歩(97)        ( 0:00/00:00:00)

    変化：5手
      5 １六歩(17)        ( 0:00/00:00:00)

    変化：2手
      2 ８四歩(83)        ( 0:00/00:00:00)
      3 ８六歩(87)        ( 0:00/00:00:00)
      4 ４二玉(51)        ( 0:00/00:00:00)
      5 ４八玉(59)        ( 0:00/00:00:00)
      6 ４四歩(43)        ( 0:00/00:00:00)

    変化：6手
      6 １四歩(13)        ( 0:00/00:00:00)

    変化：4手
      4 ３四歩(33)        ( 0:00/00:00:00)

    変化：1手
      1 ２六歩(27)        ( 0:00/00:00:00)
      """) must beSuccess.like { case ParsedPgn(_, _, Sans(sans)) =>
        sans(5).metas.variations.headOption must beSome.like { case variation =>
          variation.value must haveSize(2)
        }
        sans(4).metas.variations.headOption must beSome.like { case variation =>
          variation.value must haveSize(1)
        }
        sans(1).metas.variations.headOption must beSome.like { case variation =>
          variation.value must haveSize(5)
        }
        sans(0).metas.variations.headOption must beSome.like { case variation =>
          variation.value must haveSize(1)
        }
      }
    }
  }

  "ending" should {
    "find end" in {
      parser("""
        # ---- Kifu for Windows95 V3.53 棋譜ファイル ----
        開始日時：1999/07/15(木) 19:07:12
        終了日時：1999/07/15(木) 19:07:17
        手合割：平手
        先手：先手の対局者名
        後手：後手の対局者名
        手数----指手---------消費時間-- # この行は、なくてもいい
        1 ７六歩(77) ( 0:16/ 00:00:00)
        2 ３四歩(33) ( 0:00/)
        3 投了 ( 0:03/ )
        4 中断 ( 0:03/ )
      """.trim) must beSuccess.like { case ParsedPgn(_, tags, Sans(sans)) =>
        sans must haveSize(2)
        tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Termination && tag.value == "投了"
        }
        tags.value must contain { (tag: Tag) =>
          tag.name == Tag.Result && tag.value == "0-1"
        }
      }
    }
  }
}

