package views
package html.site

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

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
        h1("Frequently Asked Questions"),
        h2("Lidraughts"),
        question(
          "name",
          "Why is Lidraughts called Lidraughts?",
          p("The short answer is: because ", a(href := "https://lichess.org")("Lichess"), " is called Lichess. Lidraughts is created from the ", a(href := "https://github.com/ornicar/lila")("source code"), " of Lichess, and follows the same philosophy of no advertisements, open source, and everything for free. We chose to follow their name as well, because it contains all these principles in it."),
          p("Lidraughts is a combination of live/light/libre and draughts. It is pronounced ", em("lee-drafts"), ". As taken from on the ", a(href := "https://lichess.org/faq#name")("Lichess FAQ"), ": Live because games are played and watched in real-time, light and libre for the fact that it is open-source and unencumbered by proprietary junk that plagues other websites.")
        ),
        question(
          "contributing",
          "How can I contribute to Lidraughts?",
          p("Lidraughts is powered by donations from patrons and the efforts of a team of volunteers. You can find out more about being a patron ", a(href := routes.Plan.index())("here"), "."),
          p("If you want to help Lidraughts by volunteering your time and skills (translations, piece sets, etc), you can contact us by ", a(href := "mailto:contact@lidraughts.org")("e-mail"), " or in the ", a(href := routes.ForumCateg.index())("forum"), ".")
        ),
        h2("Fair Play"),
        question(
          "marks",
          "Why am I flagged for artificial rating manipulation (sandbagging and boosting) or computer assistance?",
          p("Lidraughts has strong detection methods and a very thorough process for reviewing all the evidence and making a decision. The process often involves multiple moderators and can take a long time. Other than the mark itself, we will not go into details about evidence or the decision making process for individual cases. Doing so would make it easier to avoid detection in the future, and be an invitation to unproductive debates. That time and effort is better spent on other important cases. Users can appeal by emailing ", contactEmailLink, ", but decisions are rarely overturned.")
        ),
        question(
          "rating-refund",
          "When am I eligible for the automatic rating refund from cheaters?",
          p("One minute after a user is marked as engine, their 40 latest rated wins and draws are taken (but only the games played in the latest 3 days). If you are the opponent in one of these games, you get a rating refund if you lost points, and your rating was not provisional. The rating refund will not be the full number of points lost if it would exceed ", em("your rating at the start of cheated game + points lost to cheater + 100"), ". (So, if you earned much rating after the games against the cheater, you might get no or only a partial refund). A refund will never exceed 200 points.")
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
          "Is correspondence different from normal draughts?",
          p("On Lidraughts, the main difference in rules for correspondence draughts is that an opening book is allowed. The use of engines is still prohibited and will result in being flagged for engine assistance.")
        ),
        h2("Gameplay"),
        question(
          "acpl",
          """What is "average centipiece loss"?""",
          p("The centipiece is the unit of measure we use as a representation of the advantage in a draughts position. A centipiece is equal to 1/100th of a man. Therefore 100 centipieces = 1 man. These values play no formal role in the game but are useful to players, and essential in computer draughts, for evaluating positions."),
          p("The top computer move will lose zero centipieces, but lesser moves will result in a deterioration of the position, measured in centipieces."),
          p("This value can be used as an indicator of the quality of play. The fewer centipieces one loses per move, the stronger the play."),
          p("The computer analysis on Lidraughts is powered by Scan.")
        ),
        question(
          "timeout",
          "Drawing and losing on time",
          p("All official FMJD drawing rules apply for endgame positions with only a few pieces left on the board. An overview of these rules can be found on the bottom of our ", a(href := routes.Page.variant("standard"))("Standard rules"), " section."),
          p("Note that the required amount of moves must still be played within the time available, before the game is called a draw. If a player runs out of time the game will be a loss, no matter how close it is to a draw.")
        ),
        h2("Accounts & titles"),
        question(
          "titles",
          "What titles are there on Lidraughts?",
          p(
            "Lidraughts recognises all FMJD titles gained from OTB (over the board) play, as well as many national titles. ",
            "A complete list of the titles:"
          ),
          ul(
            li("International Grandmaster (GMI)"),
            li("Woman International Grandmaster (GMIF))"),
            li("National Grandmaster (GMN))"),
            li("International Master (MI))"),
            li("Woman International Master (MIF))"),
            li("FMJD Master (MF))"),
            li("Woman FMJD Master (MFF))"),
            li("National Master (MN))"),
            li("Woman National Master (MNF))"),
            li("Candidate National Master (cMN))"),
            li("Woman Candidate National Master (cMNF")
          ),
          p("If you have an OTB title, you can apply to have this displayed on your account by completing the ", a(href := routes.Page.master())("verification form"), "."),
          p("Verifying as a titled player on Lidraughts gives access to play in Titled Arena events.")
        ),
        question(
          "usernames",
          "What can my username be?",
          p("In general, usernames should not be: offensive, impersonating someone else, or advertising.")
        ),
        h2("Lidraughts ratings"),
        question(
          "ratings",
          "What rating system does Lidraughts use?",
          p("Ratings are calculated using the Glicko-2 rating method developed by Mark Glickman."),
          p("""Fundamentally, Glicko ratings use "confidence intervals" when calculating and representing your rating. When you first start using the site, your rating starts at 1500 ± 700. The 1500 represents your rating, and the 700 represents the confidence interval."""),
          p("Basically, the system is 90% sure that your rating is somewhere between 800 and 2200. It is incredibly uncertain. Because of this, when a player is just starting out, their rating will change very dramatically, potentially several hundred points at a time. But after some games against established players the confidence interval will narrow, and the amount of points gained/lost after each game will decrease."),
          p("Another point to note is that, as time passes, the confidence interval will increase. This allows you to gain/lose points points more rapidly to match any changes in your skill level over that time.")
        ),
        question(
          "provisional",
          "Why is there a question mark (?) next to a rating?",
          p("The question mark means the rating is provisional. Reasons include:"),
          ul(
            li("The player has not yet finished enough rated games against ", em("opponents of similar strength"), " in the rating category."),
            li("The player hasn't played enough recent games. Depending on the number of games you've played, it might take around a year of inactivity for your rating to become provisional again.")
          ),
          p("Concretely, it means that the Glicko-2 deviation is greater than 110. The deviation is the level of confidence the system has in the rating. The lower the deviation, the more stable is a rating.")
        ),
        question(
          "leaderboards",
          "How do ranks and leaderboards work?",
          p("In order to get on the ", a(href := routes.User.list())("rating leaderboards"), ", you must:"),
          ol(
            li("have played at least 30 rated games in a given rating,"),
            li("have played a rated game within the last month for this rating,"),
            li("have a rating deviation lower than 80,"),
            li("be in the top 10 in this rating.")
          ),
          p("The 2nd requirement is so that players who no longer use their accounts stop populating leaderboards.")
        ),
        question(
          "high-ratings",
          "Why are ratings different compared to other sites and organisations such as FMJD and KNDB?",
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
