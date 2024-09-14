package lila.common

import play.api.libs.json._
import scala.util.Random

final class Proverb(val english: String, val japanese: String)

object Proverb {

  def one = all(Random.nextInt(all.size))

  def one(seed: String) = all(new Random(seed.hashCode).nextInt(all.size))

  val all = Vector(
    new Proverb("A pawn is worth a thousand generals", "一歩千金"),
    new Proverb("A pawn-anchored gold is more solid than a rock.", "金底の歩、岩よりも堅し"),
    new Proverb("A lance on the back rank has power.", "下段の香に力あり"),
    new Proverb("Drop the knight well back.", "桂は控えて打て"),
    new Proverb("The knight that jumps high falls prey to a pawn.", "桂の高跳び歩の餌食"),
    new Proverb("Use the silver like a plover (zigzag).", "銀は千鳥に使え"),
    new Proverb("Silver at the head of the opponent's knight is the standard move.", "桂先の銀定跡なり"),
    new Proverb("Save the gold till the end.", "金はとどめに残せ"),
    new Proverb("Entice the gold diagonally forwards.", "金は斜めに誘え"),
    new Proverb("The gold pulled back is bound to be a good move.", "金は引く手に好手あり"),
    new Proverb("With gold and silver reversed, you are bound to get trouble.", "金銀の逆形は悪形"),
    new Proverb("In the opening the bishop is stronger than the rook.", "序盤は飛車より角"),
    new Proverb("Oppose bishop with bishop.", "角には角"),
    new Proverb("The dragon in the enemy camp, the horse in your own camp.", "竜は敵陣に馬は自陣に"),
    new Proverb("A dragon inside is superior to a dragon outside.", "内竜は外竜に勝る"),
    new Proverb("A tokin on 5c cannot lose.", "５三のと金に負けなし"),
    new Proverb("A tokin is faster than you think.", "と金の遅早"),
    new Proverb("Shun an immobile king.", "居玉は避けよ"),
    new Proverb("Don't put king and rook close together.", "玉飛接近すべからず"),
    new Proverb("In the Yagura opening, don't push the edge pawn.", "矢倉囲いに端歩を突くな"),
    new Proverb("Push up the edge pawn when playing the Mino castle.", "美濃囲いは端歩を突け"),
    new Proverb("Don't put the king on the bishop's diagonal.", "玉は敵の角筋を避けよ"),
    new Proverb("In the opening push up pawns in the odd-number files.", "序盤は奇数の歩を突け"),
    new Proverb("Make a vanguard pawn, then make it stick.", "位を取ったら位の確保"),
    new Proverb("The 5e vanguard pawn is the Tennozan mountain.", "５五の位は天王山"),
    new Proverb("There are three advantages to pawn-exchange in the rook file.", "飛車先の歩交換三つの得あり"),
    new Proverb("Don't push the 5th file-pawn in bishop-exchange.", "角換わり将棋に5筋を突くな"),
    new Proverb("Against a silver in front of its pawn, defend with a pawn.", "歩越し銀には歩で対抗"),
    new Proverb("With three pawns in hand, a joining pawn and a dangling pawn.", "三歩あったら継ぎ歩に垂れ歩"),
    new Proverb("Recapture the vanguard with a joining pawn.", "位の奪回あわせの手筋"),
    new Proverb("Against ranging rook, aim at a bishop's exchange.", "振り飛車には角交換"),
    new Proverb("Range your rook onto the strategic foothold.", "拠点に飛車を振れ"),
    new Proverb("If there is an unprotected piece, you won't be stuck for a move.", "浮き駒に手あり"),
    new Proverb("Begin your attack with a sacrifice pawn push.", "開戦は歩の突き捨てから"),
    new Proverb("Approach the king by surrounding him.", "玉は包むように寄せよ"),
    new Proverb("Drive the king to the back rank.", "玉は下段に落とせ"),
    new Proverb("Against a king on the edge, push the edge pawn.", "端玉には端歩で"),
    new Proverb("A knight at the head of the king is hard to approach.", "桂頭の玉寄せにくし"),
    new Proverb("Drop a silver at the king's belly.", "玉の腹から銀を打て"),
    new Proverb("An attack with four pieces won't fail.", "４枚の攻めは切れない"),
    new Proverb("In the endgame, speed is more important than material profit.", "終盤は駒の損得より速度"),
    new Proverb("Drop where your opponent wants to drop.", "敵の打ちたいところに打て"),
    new Proverb("Defend against major pieces by drawing them closer.", "大駒は近づけて受けよ"),
    new Proverb("Don't run from a fork.", "両取り逃げるべからず"),
    new Proverb("Take two for one, even if there is a pawn in it.", "二枚換えは歩ともせよ"),
    new Proverb("Early escape is worth eight moves.", "玉の早逃げ八手の得あり"),
    // lishogi
    new Proverb("A thousand games on Lishogi, one step closer to mastery.", "Lishogiで千局、名人へ一歩")
  )

  implicit def proverbWriter: OWrites[Proverb] =
    OWrites { q =>
      Json.obj(
        "english"  -> q.english,
        "japanese" -> q.japanese
      )
    }
}
