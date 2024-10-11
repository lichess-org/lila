"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="بەرەوپێشچوونەکانی سەرباز";i['advancedPawnDescription']="یەکێک لە سەربازەکانت قووڵبۆتەوە ناو ناوچەی ڕکابەرەکەت، لەوانەیە هەڕەشەی پلە بەرزکردنەو بکات.";i['advantage']="سوود";i['advantageDescription']="چانسی خۆت بقۆزەوە بۆ ئەوەی سوودێکی یەکلاکەرەوە بەدەستبهێنیت. (200cp ≤ ڕیزبەندی ≤ 600cp)";i['anastasiaMate']="نەخشەکانی کشومات";i['anastasiaMateDescription']="ئەسپ و قەڵا یان شاژن یەکدەگرن بۆ ئەوەی پاشای بەرامبەر لە نێوان لای تەختەکە و پارچەیەکی دۆستانەدا بخەنە تەڵەوە.";i['arabianMate']="کشوماتی عەرەبی";i['arabianMateDescription']="ئەسپێک و قەڵا تیمێک دروست دەکەن بۆ ئەوەی پاشای ڕکابەرەکەیان بخەنە سوچێکی تەختەکە.";i['attackingF2F7']="هێرشبردنە سەر f2 یان f7";i['attackingF2F7Description']="هێرشەکە سەرنج دەخاتەسەر سەربازی f2 یان f7، هەروەک کردنەوەی یاری \\\"fried liver\\\".";i['attraction']="سەرنج ڕاکێشان";i['attractionDescription']="قوربانیدانێکی فریودر یان ئاڵوگۆڕک بە داشەکان، وادەکات ڕکابەرەکەت داشەکانی بە ناچاری بۆ خانەیەک بگوازێتەوە کە ڕێگە بۆ تاکتیکەکەکەت خۆش بکات.";i['backRankMate']="کشومات لە هێڵی دواوە";i['backRankMateDescription']="پاشا لە پلەکانی دواوە مات بکە، لە کاتێک کە لە نێو داشەکانی خۆیدا گیری خواردووە.";i['bishopEndgame']="قۆناغی کۆتایی یاری بە فیل";i['bishopEndgameDescription']="قۆناغی کۆتایی یاری تەنها بە فیلەکان و سەربازەکان.";i['bodenMate']="کشوماتی \\\"بۆدین\\\"";i['bodenMateDescription']="دوو فیل لە سەر هێڵی لاری خۆیان هێرش دەکەنە سەر پاشای ڕکابەر، کاتێک پاشاکە لە نێو دوو داشی خۆی گیری خواردووە.";i['capturingDefender']="بەرگریکارەکان بگرە";i['capturingDefenderDescription']="خواردنی داشێک کە گرینگە بۆ بەرگریکردن لە داشێکی تر، ڕێگەت دەدات ئەو داشەی بەرگری نامێنێت لە جوڵەی دواتردا ئاسانتر بگیرێت.";i['castling']="ماڵکردن";i['castlingDescription']="پاشا بە سەلامەتی بهێڵەرەوە، و قەڵاکە بێنە مەیدان بۆ هێرشکردن.";i['clearance']="پاککردبەوە";i['clearanceDescription']="جوڵەیەکە، زۆرجار وەک پاشەکشە وایە، خانەیەک چۆڵ دەکات بۆ بەرەوپێشبردنی بیرۆکەی تاکتیکەکەت.";i['crushing']="تێکشکاندن";i['crushingDescription']="هەڵە کوشندەکانی رکابەرەکەت ببینەوە بۆ ئەوەی بتوانی لێی سودمەندبیت و تێکی بشکێنی. (هەڵسەنگاندن ≥ 600cp)";i['defensiveMove']="جوڵەی بەرگریکردن";i['defensiveMoveDescription']="جوڵەیەک یان زنجیرە جوڵەیەکی ورد و گرنگە، بە ئەوەی داشەکانت لەدەست نەدەیت و ڕێژەی دۆڕانت کەمکەیتەوە.";i['deflection']="لادان";i['deflectionDescription']="جوڵەیەک کە وا دەکات داشی بەرانبەر ئەرکێک وەرگرێت و سەرقاڵبێت بە بەرگریکردن لە خانەیەکی دیکەی گرنگ، زۆرجار پێی دەگوترێت \\\"زیاد بارکردن\\\".";i['discoveredAttack']="دۆزینەوەی هێرشەکان";i['discoveredAttackDescription']="جوڵاندنی داشێکی (وەک ئەسپ)، کە پێشتر ڕێگری لە کشێک کردبوو لە ڕێگەی داشێکی مەودا دوورەوە (وەک قەڵا).";i['doubleBishopMate']="کشومات بە دوو فیل";i['doubleBishopMateDescription']="بە دوو داشی فیل دەتوانن پاشای رکابەر کشومات بکەن، کاتێک لەسەر هێڵە لارەکانیان لە شوێنی گونجاون و پاشای رکابەریش لە نێوان داشەکانی خۆی گیری خواردووە.";i['doubleCheck']="کشکردن لە دوولاوە";i['doubleCheckDescription']="کشکردن بە دوو داش لە یەک کاتدا، ئەنجامی هێرشێکە کە بە دوو داش دەکرێت لە کاتی جوڵانی داشێک کە کشی پێدەکەیت و داشێک کە نەتجوڵاندووە و ئۆتۆماتیکی دەکەوێتە بەرانبەر پاشای رکابەرەکەت و کش دەکات.";i['dovetailMate']="کشوماتی Dovetail";i['dovetailMateDescription']="شاژن دەتوانێت بە یاوەری پاشا کشومات بکات کاتێک پاشای بەرانبەر شوێنی هەڵاتنی لەلایەن دوو داشی خۆی گیراوە.";i['endgame']="قۆناغی کۆتایی یاری";i['endgameDescription']="تاکتیکی ماوەی قۆناغی کۆتایی یاری.";i['enPassantDescription']="تاکتیکێک کە یاسای ئانپاسان دەگرێتەوە، لە شوێنێک سەربازی رکابەرەکەت دوو خانە جوڵە دەکات تۆ دەتوانی بیگریت \\\"بیخۆیت\\\" کاتێک جوڵەکە لە بەرانبەر سەربازی تۆ دەکرێت.";i['equality']="یەکسانی";i['equalityDescription']="گۆڕینی باری دۆڕان بۆ باری یەکسان بوون یان هەوڵدان بۆ یەکسان بوون";i['exposedKing']="پاشای ئاشکرا";i['exposedKingDescription']="زۆرجار تاکتیکێک کە پاشا تێیدا بەشدارە لە کاتێکدا بە چەند بەرگریکارێکی کەم دەورەدراوە دەبێتە هۆی کشومات.";i['fork']="چەتاڵ";i['forkDescription']="جوڵیەکە کە داشی جوڵاو لە یەککاتدا هێرش دەکاتە سەر دوو داشی ڕکابەرەکەت.";i['hangingPiece']="داشی بێ بەرگری";i['hangingPieceDescription']="تاکتیکێکە داشێکی بەرانبەرەکەتی تێدا بەشدارە کە هیچ بەرگریەکی نییە، یان بەرگری پێویستی نییە و ئاسان دەتوانی بەدەستی بێنی.";i['healthyMix']="تێکەڵکردنێکی تەندروست";i['healthyMixDescription']="کەمێک لە زۆر شت. نازانی چاوەڕێی چی بکەیت، بۆیە ئامادەیی بۆ هەر شتێک و چاوڕێ دەکەیت! هەر وەک لە یارییە ڕاستەقینەکاندا.";i['hookMate']="کشومات بە قەڵغان";i['hookMateDescription']="کشومات بە قەڵا و ئەسپ و سەرباز، لەگەڵ سەربازێکی دوژمن بۆ کەمکردنەوەی سنوری هەڵاتنی پاشا.";i['interference']="تێکەڵبوون";i['interferenceDescription']="جوڵاندنی داشێکە بۆ نێوان دوو داشی بەرگری لێ نەکراوی بەرانبەر، وەک جوڵاندنی فیل بۆ خانەیەک بتوانێ هێرش بکاتە سەر دوو قەڵا.";i['intermezzo']="جوڵەی نێوان";i['intermezzoDescription']="لەبری ئەوەی جوڵەی چاوەڕوانکراو یاری بکەیت، سەرەتا جوڵەیەکی تر بخەرە نێوانیانەوە کە مەترسییەکی دەستبەجێ دروست بکات کە دەبێت بەرامبەرەکە وەڵامی بداتەوە. ئەو شێوازە بە \\\"زویشنزوگ\\\" یان \\\"لە نێوانیاندا\\\" ناسراوە.";i['kingsideAttack']="هێرشبردن لە بەری پاشا";i['kingsideAttackDescription']="هێرشبردن بۆسەر پاشای بەرانبەر، کاتێک ڕکابەرەکەت لە بەرەی پاشاکەی کاستڵینگ دەکات.";i['knightEndgame']="قۆناغی کۆتایی یاری بە فیل";i['knightEndgameDescription']="جۆرێک لە قۆناغی کۆتایی یاری کە تەنها فیلەکان و سەربازەکان بەشدارن.";i['long']="مەتەڵێکی درێژ";i['longDescription']="سێ جوڵە بۆ بوون بە براوە.";i['master']="یاری یاریزانە بەهێزەکان";i['masterDescription']="مەتەڵانێک کە لەو یاریانەوە دەرهێنراون کە لە لایەن یاریزانە هەڵسەنگێنراوەکانەوە کراوە.";i['masterVsMaster']="یاری مامۆستا بەرانبەر مامۆستا";i['masterVsMasterDescription']="مەتەڵی نێو ئەو یاریانەی کە لە نێوان یاریزانانی خاوەن نازناو دروستبوون.";i['mate']="کشومات";i['mateDescription']="بردنەوەی یاری بە شێوازێکی کەشخە.";i['mateIn1']="کشومات بە ١ جوڵە";i['mateIn1Description']="بە یەک جوڵە کشومات بەدەستبێنە.";i['mateIn2']="کشومات بە ٢ جوڵە";i['mateIn2Description']="بە دوو جوڵە کشومات بەدەستبێنە.";i['mateIn3']="کشومات بە ٣ جوڵە";i['mateIn3Description']="بە سێ جوڵە کشومات بەدەستبێنە.";i['mateIn4']="کشومات بە ٤ جوڵە";i['mateIn4Description']="بە چوار جوڵە کشومات بەدەستبێنە.";i['mateIn5']="کشومات بە ٥ جوڵە یان زۆرتر";i['mateIn5Description']="زنجیرەیەک جوڵەی دوا بە دوای یەک بدۆزەوە بۆ گەیشتن بە کشومات.";i['middlegame']="قۆناغی ناوەڕاستی یاری";i['middlegameDescription']="تاکتیکی ماوەی قۆناغی ناوەڕاستی یاری.";i['oneMove']="مەتەڵی یەک جوڵەیی";i['oneMoveDescription']="مەتەڵێک کە تەنها یەک جوڵەی ماوە.";i['opening']="دەستپێک";i['openingDescription']="تاکتیکی ماوەی قۆناغی سەرەتای یاری.";i['pawnEndgame']="کۆتاییەکانی یاری بە سەرباز";i['pawnEndgameDescription']="کۆتاییەکانی یاری تەنها بە سەربازەکان.";i['pin']="جێگیرکردن";i['pinDescription']="تاکتیکی جێگیرکردن، ئەوەیە کە بتوانی بەرانبەرەکەت بوەستێنی لە جوڵاندنی داشێک لەبەر بوونی داشێکی بەهێزتر لە دوای ئەمەوە.";i['playerGames']="یاری یاریزانەکان";i['playerGamesDescription']="لەو مەتەڵانە بگەڕێ کە لە یارییەکانتەوە دروست بوون، یان لە یارییەکانی یاریزانێکی ترەوە.";i['promotion']="پلە بەرزکردنەوە";i['promotionDescription']="پلەی یەکێک لە سەربازەکانت بۆ ئاستی شاژن یان داشێکی لەم بەها کەمتر بەرزبکەوە.";i['puzzleDownloadInformation']=s("ئەو مەتەڵانە بۆ هەموو کەسێکە، دەتوانی لێرەوە دایانبەزێنیت %s.");i['queenEndgame']="کۆتیی یاری بە شاژن";i['queenEndgameDescription']="قۆناغی کۆتایی یاری بە بوونی سەربازەکان و شاژن.";i['queenRookEndgame']="شاژن و قەڵا";i['queenRookEndgameDescription']="قۆناغی کۆتایی یاری بە بوونی تەنها شاژ ەکان و قەڵاکان و سەربازەکان.";i['queensideAttack']="هێرشبردن لە بەرەی شاژنەوە";i['queensideAttackDescription']="هێرشکردنە سەر پاشای ڕکابەر، دوای ماڵکردن لە لای شاژن.";i['quietMove']="جوڵەی بێ دەنگ";i['quietMoveDescription']="جوڵەیەکە نە کشکردنە نە بردنی داشە، تەنانەت راستەوخۆ هەڕەشەش ناکات لەسەر داشی بەرانبەر، بەڵکو خۆ ئامادەکارییە بۆ جوڵەی دواتر کە هەڕەشەیەکە ناتوانرێت بەرگری لێ بکرێت.";i['rookEndgame']="قۆناغی کۆتایی یاری بە قەڵا";i['rookEndgameDescription']="قۆناغی کۆتایی یاری تەنها بە قەڵاکان و سەربازەکان.";i['sacrifice']="قوربانی";i['sacrificeDescription']="تاکتیکێکە کە پێویستە داشەکان بکەیە قوربانی تا بتوانی دەست بەسەر یاریەکە دابگریت دوای چەند جوڵەیەکی ناچاری.";i['short']="مەتەڵی کورت";i['shortDescription']="دوو جوڵە بۆ بوون بە براوە.";i['skewer']="شیش";i['skewerDescription']="بیرۆکەیەکە کە هێرش دەکرێتە سەر داشێکی بەها بەرز لە هێڵەژە دوربخرێتەوە تا داشێکی بەها نزمتر لە دوای ئەمەوە بباتیان هێرشی بکرێتەسەر. ئەمە پێچەوانەی دەرزی \\\"pin\\\" کردنە.";i['smotheredMate']="کشی خنکێنەر";i['smotheredMateDescription']="کشوماتێک کە بە ئەسپ دەکرێت کاتێک پاشای بەرانبەر توانای دەربازبوونی نییە چونکە بە داشەکانی خۆی دەورەدراوە (خنکێنراوە).";i['superGM']="یاری جیهانی مامۆستا گەورەکان";i['superGMDescription']="مەتەڵی ئەو یاریانەی کە لەلایەن باشترین یاریزانانی جیهان ئەنجامدراون.";i['trappedPiece']="داشێک کە کەوتۆتە تەڵەوە";i['trappedPieceDescription']="داشێک بە هۆی سنورداری جوڵەکانییەوە نەتوانێت خۆی لە گرتن دەربازبکات.";i['underPromotion']="لەبەردەم پلە بەرز بوونەوە";i['underPromotionDescription']="بەرزکردنەوەی پلە بۆ ئەسپ، فیل یان قەڵا.";i['veryLong']="مەتەڵی دورو درێژ";i['veryLongDescription']="چوار جوڵە یان زیاتر بۆ بوون بە براوە.";i['xRayAttack']="هێرشی X-Ray";i['xRayAttackDescription']="داشێک هێرش یان بەرگری دەکات لە خانەیەک لە بەرانبەر هێرشی داشی دوژمن.";i['zugzwang']="Zugzwang";i['zugzwangDescription']="یاریزانی بەرامبەر سنووردارە لەو جوڵانەی کە دەتوانێت بیکات و هەموو جوڵەکان پێگەی لە یاریەکە خراپتر دەکەن."})()