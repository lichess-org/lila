[lidraughts.org](https://lidraughts.org)
==================================

Lidraughts is a rewrite of [lichess/lila](https://github.com/ornicar/lila/) for international draughts (on a 10X10 board), including the variants [Frisian draughts](https://lidraughts.org/variant/frisian) and [antidraughts](https://lidraughts.org/variant/antidraughts).

It features [live games](https://lidraughts.org/?any#hook),
[computer opponent](https://lidraughts.org/setup/ai),
[tournaments](https://lidraughts.org/tournament),
[simuls](https://lidraughts.org/simul),
[tactics trainer](https://lidraughts.org/training),
[coordinates training](https://lidraughts.org/training/coordinate),
[studies / shared analysis](https://lidraughts.org/study),
[forums](https://lidraughts.org/forum) and
[teams](https://lidraughts.org/team).

Computer opposition and analysis is made possible by Fabien Letouzey's great engine [Scan 3.0](https://github.com/rhalbersma/scan).

The UI is currently available in 11 languages, translated with a varying degree of completeness: English, Dutch, German, French, Spanish, Italian, Greek, Polish, Russian, Portuguese; and out of respect for Frisian draughts, also in Frisian!

Plans for the future include adding an opening explorer and tablebases, and possibly more variants.

The source includes a draughts implementation of [scalachess](https://github.com/ornicar/scalachess/) in modules/draughts. The UI component [chessground](https://github.com/ornicar/chessground) is implemented for draughts as ui/draughtsground.
