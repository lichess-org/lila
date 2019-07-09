package views
package html.site

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object faq {

  private def question(id: String, title: String, answer: Frag*) = div(
    st.id := id,
    cls := "question"
  )(
      h3(a(href := s"#$id")(title)),
      div(cls := "answer")(answer)
    )

  def apply()(implicit ctx: Context) = help.layout(
    title = "Frequently Asked Questions",
    active = "faq",
    moreCss = cssTag("faq")
  ) {
      main(cls := "faq small-page box box-pad")(
        h1(cls := "lichess_title")("Frequently Asked Questions"),
        h2("Lichess"),
        question(
          "name",
          "Why is Lichess called Lichess?",
          p("Lichess is a combination of live/light/libre and chess. It is pronounced ", em("lee-chess"), "."),
          p("Live, because games are played and watched in real-time 24/7; light and libre for the fact that Lichess is open-source and unencumbered by proprietary junk that plagues other websites."),
          p("Similarly, the source code for Lichess, ", a(href := "https://github.com/ornicar/lila")("lila"), ", stands for li[chess in sca]la, seeing as the bulk of Lichess is written in ", a(href := "https://www.scala-lang.org/")("Scala"), ", an intuitive programming language.")
        ),
        question(
          "contributing",
          "How can I contribute to Lichess?",
          p("Lichess is powered by donations from patrons and the efforts of a team of volunteers."),
          p("You can find out more about ", a(href := routes.Plan.index())("being a patron"), " (including a ", a(href := routes.Main.costs())("breakdown of our costs"), "). If you want to help Lichess by volunteering your time and skills, there are many ", a(href := routes.Page.help())("other ways to help"), ".")
        ),
        h2("Fair Play"),
        question(
          "marks",
          "Why am I flagged for artificial rating manipulation (sandbagging and boosting) or computer assistance?",
          p("Lichess has strong detection methods and a very thorough process for reviewing all the evidence and making a decision. The process often involves many moderators and can take a long time. Other than the mark itself, we will not go into details about evidence or the decision making process for individual cases. Doing so would make it easier to avoid detection in the future, and be an invitation to unproductive debates. That time and effort is better spent on other important cases. Users can appeal by emailing contact@lichess.org, but decisions are rarely overturned.")
        ),
        question(
          "rating-refund",
          "When am I eligible for the automatic rating refund from cheaters?",
          p("One minute after a user is marked as engine, their 30 latest rated wins are taken (but only the games played in the latest 3 days). If you are the opponent in one of these games, you get a rating refund if your rating was not provisional. The rating refund will not be the full number of points lost if it would exceed ", em("your rating at the start of cheated game + points lost to cheater + 100"), ". (So, if you earned much rating after the games against the cheater, you might get no or only a partial refund). A refund will never exceed 200 points.")
        ),
        question(
          "leaving",
          "What is done about players leaving games without resigning?",
          p("""If your opponent frequently aborts/leaves games, they get "play banned", which means they're temporarily banned from playing games. This is not publically indicated on their profile. If this behaviour continues, the length of the playban increases - and prolonged behaviour of this nature may lead to account closure.""")
        ),
        question(
          "mod-application",
          "How can I become a moderator?",
          p("It’s not possible to apply to become a moderator. If we see someone who we think would be good as a moderator, we will contact them directly.")
        ),
        question(
          "correspondence",
          "Is correspondence different from normal chess?",
          p("On Lichess, the main difference in rules for correspondence chess is that an opening book is allowed. The use of engines is still prohibited and will result in being flagged for engine assistance. Although ICCF allows engine use in correspondence, Lichess does not.")
        ),
        h2("Gameplay"),
        question(
          "variants",
          "What variants can I play on Lichess?",
          p("Lichess supports standard chess and ", a(href := routes.Page.variantHome())("8 chess variants"), ".")
        ),
        question(
          "acpl",
          """What is "average centipawn loss"?""",
          p("The centipawn is the unit of measure used in chess as representation of the advantage. A centipawn is equal to 1/100th of a pawn. Therefore 100 centipawns = 1 pawn. These values play no formal role in the game but are useful to players, and essential in computer chess, for evaluating positions."),
          p("The top computer move will lose zero centipawns, but lesser moves will result in a deterioration of the position, measured in centipawns."),
          p("This value can be used as an indicator of the quality of play. The fewer centipawns one loses per move, the stronger the play."),
          p("The computer analysis on Lichess is powered by Stockfish.")
        ),
        question(
          "timeout",
          "Losing on time, drawing and insufficient material",
          p("In the event of one player running out of time, that player will usually lose the game. However, the game is drawn if the position is such that the opponent cannot checkmate the player’s king by any possible series of legal moves (", a(href := "https://www.fide.com/fide/handbook.html?id=208&view=article")("FIDE handbook §6.9"), ")."),
          p("Note that it can be possible to mate with a single knight or bishop if the opponent has pieces that could block the king.")
        ),
        question(
          "en-passant",
          "Why can a pawn capture another pawn when it is already passed? (en passant)",
          p("""This is a legal move known as "en passant". The Wikipedia article gives a """, a(href := "https://en.wikipedia.org/wiki/En_passant")("good introduction.")),
          p("It is described in section 3.7 (d) of the ", a(href := "https://www.fide.com/fide/handbook.html?id=171&view=article")("official rules"), ":"),
          p(""""A pawn occupying a square on the same rank as and on an adjacent file to an opponent’s pawn which has just advanced two squares in one move from its original square may capture this opponent’s pawn as though the latter had been moved only one square. This capture is only legal on the move following this advance and is called an ‘en passant’ capture.""""),
          p("See the ", a(href := s"${routes.Learn.index()}#/15")("Lichess training"), " on this move for some practice with it.")
        ),
        h2("Accounts"),
        question(
          "titles",
          "What titles are there on Lichess?",
          p(
            "Lichess recognises all FIDE titles gained from OTB (over the board) play, as well as ",
            a(href := "https://github.com/ornicar/lila/wiki/Handling-title-verification-requests")("many national master titles"), ". ",
            "Here is a list of FIDE titles:"
          ),
          ul(
            li("Grandmaster (GM)"),
            li("International Master (IM)"),
            li("FIDE Master (FM)"),
            li("Candidate Master (CM)"),
            li("Woman Grandmaster (WGM)"),
            li("Woman International Master (WIM)"),
            li("Woman FIDE Master (WFM)"),
            li("Woman Candidate Master (WCM)")
          ),
          p("If you have an OTB title, you can apply to have this displayed on your account by completing the ", a(href := "https://docs.google.com/forms/d/e/1FAIpQLSd64rDqXOihJzPlBsQba75di5ioL-WMFhkInS2_vhVTvDtBag/viewform")("verification form"), ", including a clear image of an identifying document/card and a selfie of you holding the document/card."),
          p("Verifying as a titled player on Lichess gives access to play in the Titled Arena events."),
          p("Finally there is an honorary ", a(href := "#lm")("Lichess Master (LM)"), " title.")
        ),
        question(
          "lm",
          "How do I get the Lichess Master (LM) title?",
          p("This honorific title is unofficial and only exists on Lichess."),
          p("We award it to highly notable players who are good citizens of Lichess, at our discretion. You don't get the LM title, the LM title gets to you. If you qualify, you will get a message from us regarding it and the choice to accept or decline.")
        ),
        question(
          "usernames",
          "What can my username be?",
          p("In general, usernames should not be: offensive, impersonating someone else, or advertising. You can read more about the ", a(href := "https://github.com/ornicar/lila/wiki/Username-policy")("guidelines"), ".")
        ),
        question(
          "trophies",
          "Unique trophies",
          h4("The way of Berserk"),
          p("That trophy is unique in the history of Lichess, nobody other than ", a(href := "https://lichess.org/@/hiimgosu")("hiimgosu"), " will ever have it."),
          p("To get it, hiimgosu challenged himself to berserk and win 100% games of ", a(href := "https://lichess.org/tournament/cDyjj1nL")("a hourly bullet tournament"), "."),
          h4("The Golden Zee"),
          p("That trophy is unique in the history of Lichess, nobody other than ", a(href := "https://lichess.org/@/ZugAddict")("ZugAddict"), " will ever have it."),
          p("ZugAddict was streaming and for the last 2 hours he had been trying to defeat A.I. level 8 in a 1+0 game, without success. Thibault told him that if he successfully did it on stream, he'd get a unique trophy. One hour later, he smashed Stockfish, and the promise was honoured.")
        ),
        h2("Lichess ratings"),
        question(
          "ratings",
          "What rating system does Lichess use?",
          p("Ratings are calculated using the Glicko-2 rating method developed by Mark Glickman. This is a very popular rating method, and is used by a significant number of chess organisations (FIDE being a notable counter-example, as they still use the dated Elo rating system)."),
          p("""Fundamentally, Glicko ratings use "confidence intervals" when calculating and representing your rating. When you first start using the site, your rating starts at 1500 ± 700. The 1500 represents your rating, and the 700 represents the confidence interval."""),
          p("Basically, the system is 90% sure that your rating is somewhere between 800 and 2200. It is incredibly uncertain. Because of this, when a player is just starting out, their rating will change very dramatically, potentially several hundred points at a time. But after some games against established players the confidence interval will narrow, and the amount of points gained/lost after each game will decrease."),
          p("""Another point to note is that the confidence interval changes over time. If you win or lose many games (or rather "points") in a row, the confidence interval will increase allowing you to gain/lose points points more rapidly. This is because a winning/losing streak means that you are incorrectly rated/seeded and the rating system should compensate for that.""")
        ),
        question(
          "provisional",
          "Why is there a question mark (?) next to a rating?",
          p("The question mark means the rating is provisional. Reasons include:"),
          ul(
            li("The player has not yet finished enough rated games against ", em("opponents of similar strength"), " in the rating category."),
            li("The player's strength has recently improved or dropped significantly.")
          ),
          p("Concretely, it means that the Glicko-2 deviation is greater than 110. The deviation is the level of confidence the system has in the rating. The lower the deviation, the more stable is a rating.")
        ),
        question(
          "leaderboards",
          "How do ranks and leaderboards work?",
          p("In order to get on the ", a(href := routes.User.list())("rating leaderboards"), ", you must:"),
          ol(
            li("have played at least 30 rated games in a given rating,"),
            li("have played a rated game within the last week for this rating,"),
            li("be in the top 10 in this rating.")
          ),
          p("The 2nd requirement is so that players who no longer use their accounts stop populating leaderboards.")
        ),
        question(
          "high-ratings",
          "Why are ratings higher compared to other sites and organisations such as FIDE, USCF and the ICC?",
          p("It is best not to think of ratings as absolute numbers, or compare them against other organisations. Different organisations have different levels of players, different rating systems (Elo, Glicko, Glicko-2, or a modified version of the aforementioned). These factors can drastically affect the absolute numbers (ratings)."),
          p("""It's best to think of ratings as "relative" figures (as opposed to "absolute" figures): Within a pool of players, their relative differences in ratings will help you estimate who will win/draw/lose, and how often. Saying "I have X rating" means nothing unless there are other players to compare that rating to.""")
        ),
        question(
          "hide-ratings",
          "How to hide ratings while playing?",
          p(
            "Enable Zen-mode in the ",
            a(href := routes.Pref.form("game-display"))("display preferences"),
            " or by pressing ", em("z"), " during a game."
          )
        )
      )
    }
}
