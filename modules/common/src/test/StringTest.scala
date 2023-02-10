package lila.common

import scalatags.Text.all._

import org.specs2.mutable.*
import org.specs2.execute.Result

class StringTest extends Specification {

  given config.NetDomain = config.NetDomain("lichess.org")

  val i18nValidStrings = List(
    """éâòöÌÒÒçÇ""",
    """صارف اپنا نام تبدیل کریں۔ یہ صرف ایک دفعہ ہو سکتا ہے اور صرف انگریزی حروف چھوٹے یا بڑے کرنے کی اجازت ہے۔.""",
    """ユーザー名を変更します。これは一回限りで、行なえるのは大文字・小文字の変更だけです。""",
    """ਤੁਹਾਡੇ ਵਿਰੋਧੀ ਨੇ ਖੇਡ ਨੂੰ ਛੱਡ ਦਿੱਤਾ. ਤੁਸੀਂ ਜਿੱਤ ਦਾ ਦਾਅਵਾ ਕਰ ਸਕਦੇ ਹੋ, ਖੇਡ ਨੂੰ ਡਰਾਅ ਕਹਿ ਸਕਦੇ ਹੋ, ਜਾਂ ਇੰਤਜ਼ਾਰ ਕਰ ਸਕਦੇ ਹੋ.""",
    """మీ ప్రత్యర్థి బహుశా ఆట విడిచి వెళ్లిపోయారేమో. మీరు కాసేపు ఆగి చూడవచ్చు, లేదా గెలుపోటములు సమానంగా పంచుకోవచ్చు, లేదా విజయం ప్రకటించుకోవచ్చు.""",
    """ผู้เล่นที่เป็นคอมพิวเตอร์หรือใช้คอมพิวเตอร์ช่วย จะไม่ได้รับอนุญาตให้เล่น  โปรดอย่าใช้การช่วยเหลือจากตัวช่วยเล่นหมากรุก, ฐานข้อมูล หรือบุคคลอื่น ในขณะเล่น""",
    """သင့်ရဲ့ပြိုင်ဘက် ဂိမ်းမှထွက်ခွာသွားပါပြီ. လက်ရှိပွဲကို အနိုင်ယူမည်လား သရေကျပေးမည်လား သို့မဟုတ် စောင့်ဆိုင််းဦးမလား.""",
    """יריבך עזב את המשחק. באפשרותך לכפות פרישה, להכריז על תיקו או להמתין לו."""
  )

  "remove multibyte symbols" >> {
    val rms = String.removeMultibyteSymbols _
    "remove multibyte garbage" >> {
      rms("""🕸Trampas en Aperturas🕸: INTRO👋""") === "Trampas en Aperturas: INTRO"
      rms(
        """🚌🚎🚐🚑🚒🚓🚕🚗🚙🚚🚛🚜🚲🛴🛵🛺🦼🦽 with new and better !pizzes on lichess.org"""
      ) === " with new and better !pizzes on lichess.org"
      rms("🥹") === ""
      rms("🥹🥹🥹 xxx 🥹") === " xxx "
    }
    "preserve languages" >> {
      Result.foreach(i18nValidStrings) { txt =>
        rms(txt) === txt
      }
    }
    "preserve half point" >> {
      rms("½") === "½"
    }
  }

  "remove garbage chars" >> {
    String.removeGarbageChars("""ℱ۩۞۩꧁꧂""") === ""
    String.removeGarbageChars("""ᴀᴛᴏᴍɪᴄ""") === ""
    String.removeGarbageChars("""af éâòöÌÒÒçÇℱ۩۞۩꧁꧂"  صار""") === """af éâòöÌÒÒçÇ"  صار"""
    Result.foreach(i18nValidStrings) { txt =>
      String.removeGarbageChars(txt) === txt
    }
  }

  "normalize" >> {
    "keep º and ª" >> {
      String.normalize("keep normal text") === "keep normal text"
      String.normalize("keep º and ª") === "keep º and ª"
    }
    "preserve half point" >> {
      String.normalize("½") === "½"
    }
  }

  "slugify" >> {
    "be safe >> html" >> {
      String.slugify("hello \" world") must contain("\"").not
      String.slugify("<<<") must contain("<").not
    }
  }

  "richText" >> {
    "handle nl" >> {
      val url = "http://imgur.com/gallery/pMtTE"
      String.html.richText(s"link to $url here\n") === raw {
        s"""link to <a rel="nofollow noopener noreferrer" href="$url" target="_blank">$url</a> here<br>"""
      }

      String.html.richText(s"link\n", false) === raw("link\n")
    }

    "escape chars" >> {
      String.html.richText(s"&") === raw("&amp;")
    }

    "keep trailing dash on url" >> {
      // We use trailing dashes (-) >> our own URL slugs. Always consider them
      // to be part of the URL.
      String.html.richText("a https://example.com/foo--. b") === raw {
        """a <a rel="nofollow noopener noreferrer" href="https://example.com/foo--" target="_blank">example.com/foo--</a>. b"""
      }
    }

    "prize regex" >> {
      "not find btc >> url" >> {
        String.looksLikePrize(s"HqVrbTcy") must beFalse
        String.looksLikePrize(s"10btc") must beTrue
        String.looksLikePrize(s"ten btc") must beTrue
      }
    }

    def extractPosts(s: String) = String.forumPostPathRegex.findAllMatchIn(s).toList.map(_.group(1))

    "forum post path regex" >> {
      "find forum post path" >> {
        extractPosts(
          "[mod](https://lichess.org/@/mod) :gear: Unfeature topic  general-chess-discussion/abc"
        ) === List("general-chess-discussion/abc")
        extractPosts("lichess-feedback/test-2") === List("lichess-feedback/test-2")
        extractPosts("off-topic-discussion/how-come") === List("off-topic-discussion/how-come")
        extractPosts(
          "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes xx team-4-player-chess/chess-getting-boring off-topic-discussion/how-come"
        ) === List(
          "lichess-feedback/bug-unable-to-get-computer-analysis-and-learn-from-my-mistakes",
          "team-4-player-chess/chess-getting-boring",
          "off-topic-discussion/how-come"
        )
      }

      "Not find forum post path" >> {
        extractPosts("yes/no/maybe") === List()
        extractPosts("go/to/some/very/long/path") === List()
        extractPosts("Answer me yes/no?") === List()
      }
    }
  }

  "noShouting" >> {
    String.noShouting("HELLO SIR") === "hello sir"
    String.noShouting("1. Nf3 O-O-O#") === "1. Nf3 O-O-O#"
  }

}
