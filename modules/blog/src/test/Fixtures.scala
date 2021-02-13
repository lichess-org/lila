package lila.blog

object Fixtures {

  def noYoutube =
    """
<h2>YES! We did it. Crazyhouse is here!</h2>

<p>It's everywhere. <strong>Lobby games, tournaments, simuls, and the analysis board; </strong>Crazyhouse is a first-class variant on lichess.org.</p>

<h2>What is crazyhouse?</h2>

<p>It's a <em>very</em> exciting chess variant. In a crazyhouse game, either player can introduce a captured piece back onto the chessboard as their own. This is called <em>dropping</em> a piece.</p>

<p class="block-img"><img alt="" src="https://d27t3nufpewl0w.cloudfront.net/lichess/de5a66765de83e6c30865a558d067fda66459bd1_tuto1-arrow.png" width="530" height="359" /></p>

<p>In this example, White has two previously captured Black pieces in their <em>pocket</em>, a knight and a bishop. These pieces were captured by White earlier in the game. White can check the Black king by <strong><em>dropping</em></strong><strong> a bishop on c3!</strong></p>

<p class="block-img"><img alt="" src="https://d27t3nufpewl0w.cloudfront.net/lichess/a109d485184efb7600bdb2df9964ab50b2b3358b_tuto2-arrow.png" width="530" height="361" /></p>

<p>In turn Black can block the check by <strong>dropping a knight on e5!</strong></p>

<p><a href="http://lichess.org/variant/crazyhouse"><strong>Read more about crazyhouse rules and strategies here</strong></a></p>

<h2>Have great games!</h2>

<p>To be honest, this was quite tough to implement, but also fun and totally worth it. The main challenge being that crazyhouse changes the very definition of what a <em>move</em> is; since now it can also be a drop. This change to a key concept of lichess required some rewiring of the program foundations.</p>

<p><em>Please don't ask me for </em><a href="https://en.wikipedia.org/wiki/Bughouse_chess"><em>bughouse</em></a>. Lichess is a game for one board and two players. Now, someone should totally build a new bughouse website/app. It's impossible to integrate into lichess because it's radically unlike chess, structurally speaking.</p>

<h2>First crazyhouse tournament</h2>

<p>What could be a better way to get started? <a href="http://lichess.org/tournament/crazy1st"><strong>Join the party!</strong></a></p>

<p><em>[EDIT] The first tournament is over. Second crazyhouse tournament: </em><a href="http://lichess.org/tournament/crazy2nd"><em><strong>Join here!</strong></em></a></p>
"""

  def withYoutube =
    """
<p><em>Note: this article was </em><a href="https://www.reddit.com/r/chess/comments/42zpnv/crazyhouse_an_overview_strategic_concepts/"><em>posted on reddit</em></a><em> first.</em></p>

<h2>Introduction to Crazyhouse</h2>

<h3>What is <a href="http://lichess.org/variant/crazyhouse">crazyhouse</a>, and how does it differ from chess?</h3>

<p><em>According to the wiki:<br>Crazyhouse (also known as Drop chess, Mad chess, Reinforcement chess, Turnabout chess and Schizo-chess) is a chess variant similar to bughouse chess, but with only two players. It effectively incorporates a rule from the game shogi, in which a player can introduce a captured piece back to the chessboard as his own.</em></p>

<p>The fact that pieces can be re-introduced into play makes this an extremely tactical game with a huge emphasis placed on initiative and solid structure. You have to be able to calculate not only the normal chess variations from any given position, but also the resulting implications that could arise with each exchanged piece; and, even more than in chess, you need to actively prevent weaknesses in your own structure.</p>

<p>Pawns and knights increase slightly and the queen decreases slightly in relative importance, and there is no endgame to speak of, since all the pieces can be placed back on the board. Otherwise, crazyhouse takes all the normal chess themes and heightens them by magnitudes. An extra tempo or two can lead to a crushing attack, and even small weaknesses, when exploited correctly, can become glaring. Precision is almost always required, especially when defending, as every position is a sharp one.</p>

<p>For those reasons, I'm willing to propose that becoming a proficient crazyhouse player can only help your chess vision: you'll see tactics more naturally, calculate more efficiently, and identify weaknesses more quickly. A lot of ideas can be carried over. But even if that's not the case, crazyhouse is an extremely fun game that can stand on its own merits! So let's see what it's all about.</p>

<p class="block-img"><img alt="" src="https://d27t3nufpewl0w.cloudfront.net/lichess/c13a4de82869df16e3cf6ae02dbb369fef1feb4c_knightpos744.jpg" width="744" height="391" /></p>

<h2>Basic Strategy and Motifs</h2>

<h3>Theory (or lack thereof)</h3>

<p>I should start by conceding that (as of yet) there's little or no "theory" as there is in chess. It's all pretty much touch-and-go, and the bulk of the action will take place in an often-explosive middlegame. But there are definitely some rules of thumb to live by.</p>

<h3>Opening</h3>

<p>As white, you have an extra tempo which you should use to at best start an attack and at worst gain a positional advantage. As black, your goal is first to neutralize white's initiative and then equalize by imbalancing the position or counterpunching.<br>The standard rules apply: develop your pieces, control the center, and get your king to safety. But there are some opening ideas uniquely emphasized in Crazyhouse:</p>

<ul>

<li>Unproductive pawn moves should never be played until your position is well-established.</li>

<li>Flank pawns should usually be kept where they stand in the opening. Openings like the Bird or Dutch exposing this square are extremely dubious. Even openings like the English and Sicilian aren't to be recommended as they weaken the c2/c7 square respectively. Diagonal pieces (bishops and pawns) can be dropped to exploit the weakened color complex.</li>

<li>Guard the tender f2/f7 square from sacs so that your king isn't drawn out early.</li>

<li>Pawn gambits, especially those in which you delay the recapture, are almost without exception a bad idea; your opponent can accept and then simply drop the extra pawn to stabilize.</li>

<li>Make sure either you have A. open lines for your bishops, or B. your bishop is actively guarding your kingside. So long as the rest of your pieces are active, it can be solid in some scenarios to leave your kingside bishop undeveloped so that it guards tricky pawn drops or sacs on g7/2. That is to say, make sure your bishop is an active piece; it can be an active piece even if it's defending.</li>

<li>Be careful about early pawn tension and trades in the center: only accept exchanges if either your position is already quite solid and well-defended from drops, or you have a concrete idea in mind about what you want to do with that extra piece/pawn.</li>

<li>Note that getting your king to safety does not always mean castling: sometimes it means keeping it in the center and fortifying the tender points of egress. Before castling you need to make sure you have a ready reply to pawn drops and piece sacs on your kingside.</li>

<li>If black doesn't challenge the center at all, it's a good rule of thumb to play both d4 and e4.</li>

</ul>

<h3>Middlegame</h3>

<p>The middlegame is almost always where the game is won or lost. This is where you should look to start exchanging pieces, breaking with your pawns, building up pressure on vulnerable squares, or cracking open your opponent's defenses with careful pawn drops or piece sacs. The midgame is rapidly achieved in crazyhouse, so be ready to join battle straight away! I'll cover strategic midgame motifs below in the 'Strategy Pointers' section.</p>

<p>Lots of exchanges will take place in the middle game. When considering the tactical implications of exchanges, remember that, though they move the same, pieces accomplish slightly different things in crazyhouse than in chess, and that should be accounted for in your mental calculations. As a general rule of thumb, I like to think of piece values thusly:</p>

<h3>Piece	Value</h3>

<ul>

<li>Pawn	= <strong>2</strong></li>

<li>Bishop	= <strong>3</strong></li>

<li>Knight	= <strong>3.5</strong></li>

<li>Rook	= <strong>4</strong></li>

<li>Queen	= <strong>6</strong></li>

</ul>

<h3>Endgame</h3>

<p>lol what endgame</p>

<h2>Strategy Pointers <em>and other midgame motifs</em></h2>

<ul>

<li>On both offense and defense, be aware of positional sacrifices: knights and bishops or even major pieces can often be sac'd to expose the king in a manner which might be considered unsound in chess but which in Crazyhouse is very powerful. Similarly, a few points of material can be sacrificed to gain a foothold in enemy territory.</li>

<li>Guard the weak squares by your king. When your king's in the center, this means f2/7; when your king's castled kingside, this means g2/7.</li>

<li>Scan for weaknesses: holes where pawns can be dropped, pieces vulnerable to a fork, weakly defended kingside squares, overloaded defensive pieces. Identify a weakness and then concentrate all your drops on that weakness.</li>

<li>If you identify a weakness but your pocket is empty, look to force exchanges. If your own position is weak, avoid exchanges until you're more solid.</li>

<li>Avoid weakening squares of a certain color complex. Diags (pawns, bishops) can be dropped deep into your territory on your weak color complex. Try to create weaknesses in your opponent's pawn structure where you can infiltrate. Build pawn lattices into enemy territory (<a href="http://lichess.org/IqfmjHrW">like in this game</a>), preferably near the opponent's king. Expand your space wherever reasonably possible. </li>

</ul>

<p class="block-img"><img alt="" src="https://d27t3nufpewl0w.cloudfront.net/lichess/d4d9512ed572781594bcc3968cbe21364ad21333_crazy3.jpg" width="190" height="192" /></p>

<p>In this example, without the g-pawn, the dark squares, particularly g7, are weak. So white places a pawn on h6 to control that square, and will then look to exchange for more pieces, especially diagonals, to drop on black's dark kingside squares. Notice that a situation like this makes a queen trade highly desirable for white and losing for black, giving white even more control over the game.</p>

<ul>

<li>Defend pawn drops on your kingside: know how to react to p@h3/h6. The knight is an excellent defensive piece in this scenario. For example, if you have a knight on f3 and your king is castled, p@h3 can be countered by gxf3, Bxf3, <strong>Ng5</strong>, Bxf1, Qxf1, with the idea of eventually replacing the g pawn with p@g2. If you have a knight in your pocket, p@h3 can be met gxf3, Bxf3, <strong>N@f4</strong>, simultaneously attacking the bishop and defending g2. Again if the rook is captured by the bishop you recapture with your queen and your king is quite safe.</li>

<li>If they drop a pawn on the h file before you're castled, Rg1/8 is often an adequate response.</li>

<li>Defend a piece sac on your g pawn by protecting it with another piece. It's preferable to recapture kingside pawns with a bishop so that you don't leave diagonals undefended. For example, if your opponent sacs Nxg2 on your castled king, if you recapture with a knight you're susceptible to p@h3 (or p@f3 if your e-pawn has advanced), but if you recapture with a bishop those squares remain defended.</li>

<li>Create batteries on pins. If your bishop already has a pin set up, look to exchange for a bishop elsewhere so you can drop a second bishop behind your first and pile up. Drop pawns and pieces attacking the pinned piece. Distract your opponent's pieces defending the pinned piece. It's especially helpful to capture multiple times on f6 or f3 when the king is castled if the last defender of the pinned piece is the g pawn.</li>

<li>Attack aggressively, but not recklessly. A lot of people fall into the trap of dropping, dropping, dropping to continue the attack, but eventually they won't have a follow-up and the opponent, having gobbled up all of your pieces, can launch a well-provisioned counterattack. The tide can change in an instant.</li>

<li>Similarly, if you're on the defensive, as soon as your opponent gives you a tempo to work with, launch your own attack. If there is no attack to be had, start placing pieces and pawns around your king to defend points your opponent wants to invade.</li>

<li>Pawns should be dropped to build deep lattices into enemy territory, to pry open your opponent's king, to fortify your own king, or to fork two pieces.</li>

<li>Bishops should be used to pin, to block pins, or to fortify your kingside.<br>Your kingside bishop (white's light-squared bishop and black's dark-square bishop) is in many scenarios best kept near your king and used as a defensive piece.</li>

<li>Knights should be kept on hand until you can drop them to attack weak squares around the enemy's king, place extra pressure on a pin, or drop into a fork.</li>

<li>A knight and a queen is the most powerful complementary attacking combination because they each cover squares the other doesn't. Knights can be used to place un-blockable checks, and queens can be used to drop into mates.</li>

<li>Knights can be used to smother mate. Smother mates are more common in z because of the ability to drop a Q or R to force a smothered king combined with the ability to drop the N onto the checking square.</li>

<li>Knights are often strongest when placed on the fifth rank (as white) or the fourth rank (as black), as from there they observe key squares on the seventh (or second) rank. For example, I sometimes like placing a knight N@h5 with the idea of sacking on g7 to pull the king out. <a href="http://lichess.org/THYYswro#29">Example in this game at move 15</a>.</li>

<li>Rooks should for the most part be kept in your pocket until you can exploit the back rank.</li>

<li>Don't bring your Queen out until it can be brought into the attack with tempo or with a clear threat. If overly exposed it can become a focal point for your opponent's attack, gaining him tempo. If kept on d1/8 it covers many important squares, especially the c-pawn and the back rank by your king. The Queen is a fantastic defensive piece; keeping it by your king is rarely a bad idea. At the worst case it can be sac'd for a piece to quell your opponent's attack. Generally speaking, you should be well compensated if you can get two pieces for your queen.</li>

<li>A good attack is almost always worth sacking a piece for, even if it looks speculative or unclear. Once you have the initiative, look to exchange pieces as much as possible so that you can throw them into the attack.</li>

<li>Exchanges favor the attacker.</li>

<li>Start and maintain the initiative. Offense is the best defense.</li>

<li>Preserve tempo wherever possible.</li>

<li>Place pieces where they will serve multiple functions. For example, placing pieces with check which also accomplish other functions is ideal, like placing a bishop on the a or h file with check which also defends key pawns/squares like c2/f2.</li>

<li>Castling without protective minor pieces or pawn stacks on the kingside can be very dangerous, as pieces/pawns can be placed and then sacked to expose your king, like white does <a href="http://ficsgames.org/cgi-bin/show.cgi?ID=382131059">on move 20 in this game</a>. Don't castle if it weakens the protection around your king; sometimes, if you build strong command of the center, it's best to leave your king in the center.</li>

<li>Rooks are more or less of comparable value to minor pieces, unless the back rank is weak, in which case they increase in relative importance.</li>

<li>If the 7th rank is weak and a rook is exposed, place two pawns side-by-side attacking the rook with ideas of Queening. <a href="http://lichess.org/Gpk2WwCD#29">Example on move 15 here</a>. Promoted pieces turn back into pawns when re-captured.</li>

<li>Sometimes it's not worth saving your queen if it weakens your squares or puts you on the defensive: in such cases it can be okay to just protect it with a bishop or continue your attack. For example, on move 10 <a href="http://ficsgames.org/cgi-bin/show.cgi?ID=382131253">in this game</a>, black sacs his Queen (in a safe position) so that he can begin an attack.</li>

<li>Only ever trade queens early if it's advantageous to your position. An early queen trade means you need to be extremely aware of drop attacks moving forward.</li>

<li>Fill the holes in your defense with pawns, as black does on moves 10 and 12 <a href="http://ficsgames.org/cgi-bin/show.cgi?ID=382131217">in this game</a>.</li>

<li>When you're castled on the kingside be wary of your opponent's ideas of capturing on d5 where your Queen (or any undefended piece besides a knight) recaptures, as they can place a knight on e2 winning the Queen.</li>

<li>When you're on the attack, try to place your pieces with check or with another immediate threat so as to save tempo and keep the initiative. When you're defending, try to keep your king on a square where it isn't susceptible to a drop check.</li>

<li>Most play will take place in the center and on the kingside. But if kingside play has ground to a standstill, be willing to look to the queenside to break through.</li>

<li>Emphasize king safety over material gain. Emphasize the initiative over material gain. Only bank material when you can consolidate before getting attacked.</li>

<li>If your king is exposed, either retreat it to safety or drop things around it--preferably pawns--as quickly as possible. It's especially important to cover all the squares knights can be dropped to check you.</li>

<li>Play with a sense of urgency. No lazy moves!</li>

<li>Never go in against a Sicilian when death is on the line.</li>

</ul>

<h2>Common Mating Patterns</h2>

<ul>

<li><a href="http://lichess.org/THYYswro/white#40">Pawn and knight</a></li>

<li><a href="http://lichess.org/PBCIKH6G/black#43">Two knights & pawn</a></li>

<li><a href="http://lichess.org/loeNxFMk/black#52">Diagonal & queen</a></li>

<li><a href="http://lichess.org/zlen3fXC/white#50">Bishop & rook</a></li>

<li><a href="http://lichess.org/0PBzadZe/white#16">Pawn smother</a></li>

<li><a href="http://lichess.org/Ix9npN7B/black#49">Knight & queen</a>. Play continues gxN Q@g2#, Kf1 Q@f2#, or Kh1 Q@g1 Rxg1 Nf2#.</li>

<li><a href="http://lichess.org/G5UBLNT4/white#86">Queen & knight</a></li>

<li><a href="http://lichess.org/xSp1YyCi/black#23">Knight & rook</a></li>

<li><a href="http://lichess.org/vDAmSwoR/white#68">Back rank spanker</a></li>

<li><a href="http://lichess.org/RBYkTfFO/black#25">Pawn, bishop, & knight</a></li>

<li><a href="http://lichess.org/i3oGc2Xy/white#38">Protected pawn & rook</a></li>

<li><a href="http://lichess.org/DOgAUBQv/white#51">Two adjacent diags</a></li>

<li><a href="http://lichess.org/UzMyzMys/black#35">Smother mate</a></li>

<li><a href="http://lichess.org/xFA300uL/white#46">Knight & bishop</a></li>

</ul>

<h2>My Opening Systems</h2>

<p>Here's my Crazyhouse opening repertoire, listed by the frequency with which I play it. I'm not going to claim it's anywhere near perfected, or even that it's what you should play. But I've had success with it. Obviously the move orders will change quite a bit depending on what your opponent does and you'll have to be flexible, but I'm going to list the common move order for each just so you can get a feel for the general set-up. I can't go through each variation because it would take forever, but if there's a certain line you're interested in let me know.</p>

<h3>White Openings</h3>

<h4>Modified Catalan </h4>

<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=4m14s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>

<p><em>Basic setup:</em> 1. d4 ... 2. g3 ... 3. Bg2 ... 4. h3 ... 5. Nf3 ... 6. Bg5 ... 7. 0-0 ... 8. Nbd2</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/THYYswro">lichess.org/THYYswro</a></p>

<p><em>Themes:</em> Solidify and fortify your king before you attack. h3 is an important move so your opponent can't place a pawn there attacking your fianchettoed bishop. You start out a bit passive but extremely solid. Black will have a hard time breaking through while you use the time he's trying to drum something up to form an attack of your own on his likely more exposed kingside. Trade pieces in the center and then drop on the kingside. Often p@e5 to challenge the center or a pinned piece or p@h6 to pry open the king. You can allow the f3 knight to be captured, whereafter you'll often recapture with the e-pawn to build a nice box around your king. If they sac on h3, capture and then simply replace the pieces right back where they were. If no tension develops after the first 8 moves and there is no obvious attacking idea, bring a rook to c1 and break with c4 (or just play c4 immediately) to trade pawns and open things up.</p>

<h4>Modified Chigorin/Trompowsky </h4>

<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=1m01s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>

<p><em>Basic setup:</em> 1. d4 ... 2. Nc3 ... 3. Bg5 ... 4. Nf3 ... 5. e3 (or e4, if allowed) ... 6. Be2/d3 ... 7. 0-0</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/LTdPNcHW">lichess.org/LTdPNcHW</a></p>

<p><em>Themes:</em> Focus on the pin on the h4-d8 diagonal. Sometimes exchange for a second bishop which you can place behind the first on h4. Pile up on the pin. Option of trading on f6 for a pocket knight or on e7 for a pocket bishop. Try to advance e4-e5 if given the chance and claim space. If the black bishop goes to f5, challenge it with Bd3 and re-capture with the c pawn for a strong center; if it goes to g4, block the pin with Be2. If given the chance, exchange pawns in the center and place p@h6. In completely passive or dubious black systems where they're generating little or no counterplay it can be explosive to play e4, d4, and then after the minor pieces are deployed f4!? followed by 0-0 and f5.</p>

<h4>e4 systems</h4>

<p><em>Basic setup:</em> 1. e4 ... 2. Nf3 ... 3. Bc4/b5/e2 ... 4. Bg5/f4 ... 5. Nc3 or Nbd2 followed by eventual c3 ... 6. 0-0</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/UA6ayvXz">lichess.org/UA6ayvXz</a></p>

<p><em>Themes:</em> Develop, control the center, castle, and then immediately attack. I don't play e4 so much, but you can get a good feel for it by reviewing some of FICS's top-rated player's--tantheman's--white games. <a href="http://www.ficsgames.org/cgi-bin/search.cgi?Games=dsearch;statsdays=365;white=tantheman;variant=11;fyear=2000;tyear=2016;colors=1">Find them here</a>.<br></p>

<h4>Offbeat Stuff (1. b3/d3/e3)</h4>

<p><em>Basic setup:</em> 1. b3 ... 2. Bb2 ... 3. e3 ... 4. d3 ... 5. Nf3 ... 6. Be2, stuff like that.</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/dkZPL3rf">lichess.org/dkZPL3rf</a></p>

<p><em>Themes:</em> Throw your opponent off. Build a solid structure and ask your opponent how he's going to break through. Castle queenside occasionally. Break with c4 or throw your kingside pawns at him.</p>

<h3>Black Openings</h3>

<h4>Crosky Gambit</h4>

<div data-oembed="https://www.youtube.com/watch?v=uz-dZ2W4Bf0#t=9m32s" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/uz-dZ2W4Bf0?feature=oembed" frameborder="0" allowfullscreen></iframe></div>

<p>This affectionately self-titled opening is one I developed through many games of trial and error on FICS, and it's my go-to weapon as black. It looks dubious at first glance but it's held up even at very high levels of play. Its themes can actually carry over into several other opening variations in which you allow your f6 knight to be captured.</p>

<p><em>Basic setup:</em> 1. e4 Nf6 2. e5 Nc6!? 3. exf6 gxf6! 4. ... d5 (can transpose with 2. ... d5 instead of Nc6) 5. ... Rg8 6. ...Bg4 7. ...e6</p>

<p class="block-img"><img alt="" src="https://d27t3nufpewl0w.cloudfront.net/lichess/9854ea498c5f49b19101ac5e40510b357d0bdbf2_croskysetup.jpg" width="510" height="319" /></p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/W4lzqvxx">lichess.org/W4lzqvxx</a></p>

<p><em>Themes:</em> Firstly, you build a strong center around your king. The pawn on f6 prevents a knight from coming to the key g5 and e5 squares where it could attack f7. Then your rook is coming to the g file where it's going to dictate play on the kingside and constantly pressure g2. You'll often have a pin on the h5-d1 diagonal where you can win your piece back by placing p@e4; otherwise, that pawn will often be placed p@h3. In return for the piece you get a pawn that you'll use to severely weaken white's position and tremendous pressure on his light square complex in the upper left quadrant of the board (near his king). Capturing the knight on f3 and then dropping N@h4 where it attacks both f3 and g2 is a common motif. Your king will always remain in the center where it's well-guarded. Be careful not to let your h-pawn fall or too much pressure to be dropped around your rook or your king will be come exposed quickly. All in all, you're offering to temporarily go down in material for an open, exciting game where you have immediate counterpunching chances that white will be hard-pressed to deal with without precise play.<br>A second variation deviates on move 3 with 3. ... exf6 instead of gxf6 and continues 4. ... d5, whereafter you develop normally. This is perfectly viable. You offer a knight for a pawn to achieve open lines and accelerated development.</p>

<h4>Modified Modern</h4>

<p class="block-img"><img alt="" src="https://d27t3nufpewl0w.cloudfront.net/lichess/d302e21e46e2aa5d694ac8f442815ae18f760efa_setup2.jpg" width="192" height="192" /></p>

<p><em>Basic setup:</em> 1. ...g6 2. ... Bg7 3. ...Nf6 4. ...h6 5. ...d5 6. ...Bg4 7. ... 0-0 8. ...Nbd7 or Nc6</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/Ix9npN7B">lichess.org/Ix9npN7B</a></p>

<p>Themes: This is going to operate much like the modified Catalan I described above, except for black. You build a strong little box around your king (see image above), whereafter you break with d5. White will often push or place a pawn on e5, and you can either move the knight or use the tempo to start an attack or do something else constructive with the idea of allowing the capture on f6 so you can recapture with the e-pawn and build your box. Exchange in the center and drop on white's kingside. King safety is paramount; keep your box fortress stable with pawn drops as needed (p@e7, p@g5, etc.) and then counterpunch. When attacking pawns can often be placed p@e4 to challenge a pinned piece and gain central influence or p@h3 to open his king.</p>

<h4>Two Knights Defense</h4>

<p><em>Basic setup:</em> 1. ... Nf6 2. ... Nc6 3. ... d6 / 3. ...d5 / 3. ... e5. I prefer 3. ... d6 4. ... Bg4 5. ...e6 6. ... Be7 7. ... 0-0</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/Hjrcklag">lichess.org/Hjrcklag</a></p>

<p><em>Themes:</em> Get your pieces out quickly and build a contained, modest setup staring down white's center without creating tension (d6 e6) or create immediate imbalances by breaking in the center with a quick d5/e5. Often castle kingside, sometimes leave it in the center.</p>

<h4>Modified French</h4>

<p>This should be the go-to opening for new players. It's solid, simple, and avoids most opening traps.</p>

<p><em>Basic setup:</em> 1. e4 e6 2. d4 d5</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/o36HZqS9">lichess.org/o36HZqS9</a></p>

<p><em>Themes:</em> In the exchange varation (3. exd5 exd5), you're happy because your light square bishop gets active and you have open lines. In the advance variation (3. e5), you will not break with c5 or f6 as you might like to in standard chess. Instead you can continue by getting your knight to f5 (often via e7) and your bishop to e7. Because the f6 square is temporarily weak when your knight comes to e7, I actually prefer slightly varying the move order with 2. Ne7 before breaking with d5 so that your knight can immediately move after you break, uncovering the queen's defense of f6. You can also bring your knight to f6 first and move it to e4 after you break with d5 and he advances (or accept the capture on f6 with gxf6 and enter into Crosky Gambit territory!). The main advantage of this opening is that it discourages the Bishop from coming to c4 and peering down on f7.<br></p>

<h4>Modified Scandinavian and Englund Gambits</h4>

<p><em>Basic setup:</em> 1. e4 d5 2. exd5 e6 3. dxe6 Bxe6 4. ... Nf6 5. ... Be7 or 1. d4 e5 2. dxe5 d6 3. exd6 Bxd6 4. ... Nf6 5. ... Bg4</p>

<p><em>Illustrative Game:</em> <a href="http://lichess.org/YydlL3bB">lichess.org/YydlL3bB</a></p>

<p><em>Themes:</em> Gambit a central pawn for compensation by way of accelerated development and open lines. Best played against weaker opponents who can be confidently outplayed, but can be a fun surprise weapon against strong players too if used sparingly.</p>

<h2>Videos and other resources</h2>

<div data-oembed="https://www.youtube.com/watch?v=usf-7UBV5rw" data-oembed-type="video" data-oembed-provider="youtube"><iframe width="480" height="270" src="https://www.youtube.com/embed/usf-7UBV5rw?feature=oembed" frameborder="0" allowfullscreen></iframe></div>

<ul>

<li><a href="https://www.youtube.com/watch?v=wbxgu7M-P1E">IM John Bartholomew plays the lichess Crazyhouse bullet arena</a></li>

<li><a href="https://www.youtube.com/watch?v=Czy1va4lhZg">NM Chess Network plays a lichess blitz tournament</a></li>

<li><a href="https://www.youtube.com/watch?v=rrgrnlNEUOk">Lichess Master Atrophied explains Crazyhouse rules</a></li>

<li><a href="https://www.youtube.com/watch?v=wn87NOBBNl4">Lichess Master Atrophied explains opening principles</a></li>

<li><a href="http://lichess.org/games/search?perf=18&sort.field=d&sort.order=desc">Lichess Crazyhouse Games Database</a></li>

<li><a href="http://ficsgames.org/cgi-bin/search.cgi">FICS Crazyhouse Games Database (search for games by cheesybread or tantheman -- the very best!)</a></li>

<li><a href="https://www.reddit.com/r/crazyhouse">Crazyhouse subreddit</a></li>

</ul>

<h2>tl;dr</h2>

<p>Beginners may be best served by playing conventional e4/d4 systems as white and the French as black. Avoid pawn moves which weaken key squares; fortify weak squares around your king. Maintain the initiative and attack. Sac when it draws the king out and you have a follow-up. Emphasize king safety over material gain. Calculate what your opponent (and you) can do with exchanged pieces before entering into tactical complications. Go crazy!</p>
"""
}
