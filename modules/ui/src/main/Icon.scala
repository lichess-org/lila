/*
 * This source is generated by bin/gen/licon.py. Run licon.py after changing public/font/lichess.sfd
 *
 * Constant names and values are pulled from private use characters defined in the sfd file.
 *
 * Character names can be edited in fontforge's "glyph info" dialog, or by editing the StartChar: line that
 * begins each character chunk in lichess.sfd
 *
 * To make these characters visible in your editor, install the lichess.ttf font (which is also generated by
 * licon.py) and then add it to your editor's font list.
 */

package lila.ui

opaque type Icon = String
object Icon:
  def apply(char: String): Icon = char
  extension (icon: Icon) def value: String = icon
  given scalalib.Iso.StringIso[Icon] = scalalib.Iso.string(apply, value)
  import play.api.libs.json.{ Writes, JsString }
  given Writes[Icon] = JsString(_)
  val CautionTriangle: Icon = "" // e000
  val Link: Icon = "" // e001
  val Rabbit: Icon = "" // e002
  val ShareIos: Icon = "" // e003
  val ShareAndroid: Icon = "" // e004
  val Gear: Icon = "" // e005
  val DieSix: Icon = "" // e006
  val FlagKingHill: Icon = "" // e007
  val FlameBlitz: Icon = "" // e008
  val Feather: Icon = "" // e009
  val Turtle: Icon = "" // e00a
  val FlagChessboard: Icon = "" // e00b
  val ArcheryTarget: Icon = "" // e00c
  val ThreeCheckStack: Icon = "" // e00d
  val UploadCloud: Icon = "" // e00e
  val ExternalArrow: Icon = "" // e00f
  val AnalogTv: Icon = "" // e010
  val RssFeed: Icon = "" // e011
  val StudyBoard: Icon = "" // e012
  val Shield: Icon = "" // e013
  val InkQuill: Icon = "" // e014
  val Target: Icon = "" // e015
  val Crown: Icon = "" // e016
  val LineGraph: Icon = "" // e017
  val GraduateCap: Icon = "" // e018
  val PaperAirplane: Icon = "" // e019
  val ZoomIn: Icon = "" // e01a
  val Expand: Icon = "" // e01b
  val Atom: Icon = "" // e01c
  val List: Icon = "" // e01d
  val Antichess: Icon = "" // e01e
  val Microscope: Icon = "" // e01f
  val ChasingArrows: Icon = "" // e020
  val CrownElite: Icon = "" // e021
  val Funnel: Icon = "" // e022
  val Checkmark: Icon = "" // e023
  val InternalArrow: Icon = "" // e024
  val PlayTriangle: Icon = "" // e025
  val GreaterThan: Icon = "" // e026
  val LessThan: Icon = "" // e027
  val DiscBig: Icon = "" // e028
  val DiscBigOutline: Icon = "" // e029
  val X: Icon = "" // e02a
  val ArrowDownRight: Icon = "" // e02b
  val ArrowUpRight: Icon = "" // e02c
  val PlusButton: Icon = "" // e02d
  val MinusButton: Icon = "" // e02e
  val Fire: Icon = "" // e02f
  val DownTriangle: Icon = "" // e030
  val UpTriangle: Icon = "" // e031
  val Bullet: Icon = "" // e032
  val Swords: Icon = "" // e033
  val JumpLast: Icon = "" // e034
  val JumpFirst: Icon = "" // e035
  val JumpNext: Icon = "" // e036
  val JumpPrev: Icon = "" // e037
  val Pause: Icon = "" // e038
  val Hamburger: Icon = "" // e039
  val Globe: Icon = "" // e03a
  val Book: Icon = "" // e03b
  val BarGraph: Icon = "" // e03c
  val Keypad: Icon = "" // e03d
  val Berserk: Icon = "" // e03e
  val Padlock: Icon = "" // e03f
  val FlagOutline: Icon = "" // e040
  val BubbleSpeech: Icon = "" // e041
  val BubbleConvo: Icon = "" // e042
  val Envelope: Icon = "" // e043
  val Group: Icon = "" // e044
  val Trophy: Icon = "" // e045
  val ThumbsUp: Icon = "" // e046
  val Back: Icon = "" // e047
  val CautionCircle: Icon = "" // e048
  val NotAllowed: Icon = "" // e049
  val RandomColor: Icon = "" // e04a
  val Pencil: Icon = "" // e04b
  val Cogs: Icon = "" // e04c
  val Tag: Icon = "" // e04d
  val Clock: Icon = "" // e04e
  val Trash: Icon = "" // e04f
  val User: Icon = "" // e050
  val StarOutline: Icon = "" // e051
  val Star: Icon = "" // e052
  val MoreTriangle: Icon = "" // e053
  val Eye: Icon = "" // e054
  val Power: Icon = "" // e055
  val Download: Icon = "" // e056
  val Search: Icon = "" // e057
  val Forward: Icon = "" // e058
  val UltraBullet: Icon = "" // e059
  val Storm: Icon = "" // e05a
  val Tools: Icon = "" // e05b
  val Bullseye: Icon = "" // e05c
  val Agent: Icon = "" // e05d
  val Mic: Icon = "" // e05e
  val BarChart: Icon = "" // e05f
  val InfoCircle: Icon = "" // e060
  val ScreenDesktop: Icon = "" // e061
  val PhoneMobile: Icon = "" // e062
  val Multiboard: Icon = "" // e063
  val HeartOutline: Icon = "" // e064
  val FlagRacingKings: Icon = "" // e065
  val Crazyhouse: Icon = "" // e066
  val Tshirt: Icon = "" // e067
  val Heart: Icon = "" // e068
  val RadioTower: Icon = "" // e069
  val BellOutline: Icon = "" // e06a
  val Disc: Icon = "" // e06b
  val Wings: Icon = "" // e06c
  val DiscOutline: Icon = "" // e06d
  val Handset: Icon = "" // e06e
  val ArrowThruApple: Icon = "" // e06f
  val Clipboard: Icon = "" // e070
  val Move: Icon = "" // e071
  val Ibeam: Icon = "" // e072
  val Cancel: Icon = "" // e073
  val Voice: Icon = "" // e074
  val Mask: Icon = "" // e075
  val OneHalf: Icon = "" // e076
  val Mute: Icon = "" // e077
  val Reload: Icon = "" // e078
  val AccountCircle: Icon = "" // e079
  val Logo: Icon = "" // e07a
  val Switch: Icon = "" // e07b
  val Blindfold: Icon = "" // e07c
