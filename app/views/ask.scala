package views.html

import scala.collection.mutable.StringBuilder
import scala.util.Random.shuffle

import scalatags.Text.TypedTag
import controllers.routes
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.ask.Ask
import lila.ask.AskEmbed

object ask:

  def render(fragment: Frag)(using PageContext): Frag =
    val ids = extractIds(fragment, Nil)
    if ids.isEmpty then fragment
    else
      RawFrag:
        AskEmbed.bake(
          fragment.render,
          ids.map: id =>
            env.ask.repo.get(id) match
              case Some(ask) =>
                div(cls := s"ask-container${ask.isStretch so " stretch"}", renderOne(ask)).render
              case _ =>
                p("<not found>").render
        )

  def renderOne(ask: Ask, prevView: Option[Vector[Int]] = None, tallyView: Boolean = false)(using
      Context
  ): Frag =
    RenderAsk(ask, prevView, tallyView).render

  def renderGraph(ask: Ask)(using Context): Frag =
    if ask.isRanked then RenderAsk(ask, None, true).rankGraphBody
    else RenderAsk(ask, None, true).pollGraphBody

  // AskEmbed.bake only has to support embedding in single fragments for all use cases
  // but this recursion (probably) doesn't hurt us so keep it around for later
  private def extractIds(fragment: Modifier, ids: List[Ask.ID]): List[Ask.ID] = fragment match
    case StringFrag(s)  => ids ++ AskEmbed.extractIds(s)
    case RawFrag(f)     => ids ++ AskEmbed.extractIds(f)
    case t: TypedTag[?] => t.modifiers.flatten.foldLeft(ids)((acc, mod) => extractIds(mod, acc))
    case _              => ids

private case class RenderAsk(
    ask: Ask,
    prevView: Option[Vector[Int]],
    tallyView: Boolean
)(using ctx: Context):
  val voterId = ctx.myId.fold(ask.toAnon(ctx.ip))(why => ask.toAnon(why.userId))

  val view = prevView getOrElse:
    if ask.isRandom then shuffle(ask.choices.indices.toList)
    else ask.choices.indices.toList

  def render =
    fieldset(
      cls                                   := s"ask${ask.isAnon so " anon"}",
      id                                    := ask._id,
      ask.hasPickFor(voterId) option (value := "")
    )(
      header,
      ask.isConcluded option label(s"${ask.form.so(_ size) max ask.picks.so(_ size)} responses"),
      ask.choices.nonEmpty option (
        if ask.isRanked then
          if ask.isConcluded || tallyView then rankGraphBody
          else rankBody
        else if ask.isConcluded || tallyView then pollGraphBody
        else pollBody
      ),
      footer
    )

  def header =
    val viewParam = view.mkString("-")
    legend(
      span(cls := "ask__header")(
        label(
          ask.question,
          !tallyView option (
            if ask.isConcluded then span("(Results)")
            else if ask.isRanked then span("(Drag to sort)")
            else if ask.isMulti then span("(Choose all that apply)")
            else span("(Choose one)")
          )
        ),
        maybeDiv(
          "url-actions",
          ask.isTally option button(
            cls        := (if tallyView then "view" else "tally"),
            formmethod := "GET",
            formaction := routes.Ask.view(ask._id, viewParam.some, !tallyView)
          ),
          ctx.myId.contains(ask.creator) || isGranted(_.ModerateForum) option button(
            cls        := "admin",
            formmethod := "GET",
            formaction := routes.Ask.admin(ask._id),
            title      := trans.edit.txt()
          ),
          (ask.hasPickFor(voterId) || ask.hasFormFor(voterId)) && !ask.isConcluded option button(
            cls        := "unset",
            formaction := routes.Ask.unset(ask._id, viewParam.some, ask.isAnon),
            title      := trans.delete.txt()
          )
        ),
        maybeDiv(
          "properties",
          ask.isTraceable option button(
            cls   := "property trace",
            title := "Participants can see who voted for what"
          ),
          ask.isAnon option button(
            cls   := "property anon",
            title := "Your identity is anonymized and secure"
          ),
          ask.isOpen option button(cls := "property open", title := "Anyone can participate")
        )
      )
    )

  def footer =
    div(cls := "ask__footer")(
      ask.footer map (label(_)),
      ask.isForm && !ask.isConcluded && voterId.nonEmpty option frag(
        input(
          cls         := "form-text",
          tpe         := "text",
          maxlength   := 80,
          placeholder := "80 characters max",
          value       := ~ask.formFor(voterId)
        ),
        div(cls := "form-submit")(input(cls := "button", tpe := "button", value := "Submit"))
      ),
      ask.isConcluded && ask.form.exists(_.size > 0) option frag:
        ask.form.map: fmap =>
          div(cls := "form-results")(
            ask.footer map (label(_)),
            fmap.toSeq flatMap:
              case (user, text) => Seq(div(ask.isTraceable so s"$user:"), div(text))
          )
    )

  def pollBody = choiceContainer:
    val picks = ask.picksFor(voterId)
    val sb    = StringBuilder("choice ")
    if ask.isCheckbox then sb ++= "cbx " else sb ++= "btn "
    if ask.isMulti then sb ++= "multiple " else sb ++= "exclusive "
    if ask.isStretch then sb ++= "stretch "
    (view map ask.choices).zipWithIndex map:
      case (choiceText, choice) =>
        val selected = picks.exists(_ contains choice)
        if ask.isCheckbox then
          label(
            cls   := sb.toString + (if selected then "selected" else "enabled"),
            title := tooltip(choice),
            value := choice
          )(input(tpe := "checkbox", selected option checked), choiceText)
        else
          button(
            cls   := sb.toString + (if selected then "selected" else "enabled"),
            title := tooltip(choice),
            value := choice
          )(choiceText)

  def rankBody = choiceContainer:
    validRanking.zipWithIndex map:
      case (choice, index) =>
        val sb = StringBuilder("choice btn rank")
        if ask.isStretch then sb ++= " stretch"
        if ask.hasPickFor(voterId) then sb ++= " submitted"
        div(cls := sb.toString, value := choice, draggable := true)(
          div(s"${index + 1}"),
          label(ask.choices(choice)),
          i
        )

  def pollGraphBody =
    div(cls := "ask__graph")(frag:
      val totals = ask.totals
      val max    = totals.max
      totals.zipWithIndex flatMap:
        case (total, choice) =>
          val pct  = if max == 0 then 0 else total * 100 / max
          val hint = tooltip(choice)
          Seq(
            div(title := hint)(ask.choices(choice)),
            div(cls := "votes-text", title := hint)(pluralize("vote", total)),
            div(cls := "set-width", title := hint, css("width") := s"$pct%")(nbsp)
          )
    )

  def rankGraphBody =
    div(cls := "ask__rank-graph")(frag:
      val tooltipVec = rankedTooltips
      ask.averageRank.zipWithIndex
        .sortWith((i, j) => i._1 < j._1)
        .flatMap:
          case (avgIndex, choice) =>
            val lastIndex = ask.choices.size - 1
            val pct       = (lastIndex - avgIndex) / lastIndex * 100
            val hint      = tooltipVec(choice)
            Seq(
              div(title := hint)(ask.choices(choice)),
              div(cls := "set-width", title := hint, style := s"width: $pct%")(nbsp)
            )
    )

  def maybeDiv(clz: String, tags: Option[Frag]*) =
    if tags.toList.flatten.nonEmpty then div(cls := clz, tags) else emptyFrag

  def choiceContainer =
    val sb = StringBuilder("ask__choices")
    if ask.isVertical then sb ++= " vertical"
    if ask.isStretch then sb ++= " stretch"
    div(cls := sb.toString)

  def tooltip(choice: Int) =
    val sb         = StringBuilder(256)
    val choiceText = ask.choices(choice)
    val hasPick    = ask.hasPickFor(voterId)

    val count    = ask.count(choiceText)
    val isAuthor = ctx.myId.contains(ask.creator)
    val isMod    = isGranted(_.ModerateForum)

    if !ask.isRanked then
      if ask.isConcluded || tallyView then
        sb ++= pluralize("vote", count)
        if ask.isTraceable || isMod then sb ++= s"\n\n${whoPicked(choice)}"
      else
        if isAuthor || ask.isTally then sb ++= pluralize("vote", count)
        if ask.isTraceable && ask.isTally || isMod then sb ++= s"\n\n${whoPicked(choice)}"

    if sb.isEmpty then choiceText else sb.toString

  def rankedTooltips =
    val respondents = ask.picks so (picks => picks.size)
    val rankM       = ask.rankMatrix
    val notables = List(
      0 -> "ranked this first",
      1 -> "chose this in their top two",
      2 -> "chose this in their top three",
      3 -> "chose this in their top four",
      4 -> "chose this in their top five"
    )
    ask.choices.zipWithIndex map:
      case (choiceText, choice) =>
        val sb = StringBuilder(s"$choiceText:\n\n")
        notables filter (_._1 < rankM.length - 1) map:
          case (i, text) =>
            sb ++= s"  ${rankM(choice)(i)} $text\n"
        sb.toString

  def pluralize(item: String, n: Int) =
    s"${if n == 0 then "No" else n} ${item}${if n != 1 then "s" else ""}"

  def whoPicked(choice: Int, max: Int = 100) =
    val who = ask.whoPicked(choice)
    if ask.isAnon then s"${who.size} votes"
    else who.take(max).mkString("", ", ", (who.length > max) so ", and others...")

  def validRanking =
    val initialOrder =
      if ask.isRandom then shuffle((0 until ask.choices.size).toVector)
      else (0 until ask.choices.size).toVector
    ask
      .picksFor(voterId)
      .fold(initialOrder): r =>
        if r == Vector.empty || r.distinct.sorted != initialOrder.sorted then
          voterId so (id => env.ask.repo.setPicks(ask._id, id, Vector.empty[Int].some))
          initialOrder
        else r
