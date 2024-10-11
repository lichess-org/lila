"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Başka bir tema ekle";i['advanced']="İleri düzey";i['bestMove']="En iyi hamle!";i['byOpenings']="Açılışlardan";i['clickToSolve']="Çözmek için tıklayın";i['continueTheStreak']="Seriye devam et";i['continueTraining']="Pratik yapmaya devam et";i['dailyPuzzle']="Günlük Bulmaca";i['didYouLikeThisPuzzle']="Bulmacayı beğendiniz mi?";i['difficultyLevel']="Zorluk seviyesi";i['downVote']="Bulmacayı beğenme";i['easier']="Kolay";i['easiest']="Çok kolay";i['example']="Örnek";i['failed']="başarısız";i['findTheBestMoveForBlack']="Siyah için en iyi hamleyi bulun.";i['findTheBestMoveForWhite']="Beyaz için en iyi hamleyi bulunuz.";i['fromGameLink']=s("Maçın linki: %s");i['fromMyGames']="Benim oyunlarımdan";i['fromMyGamesNone']="Veri tabanında sizin maçlarınızdan bir bulmaca bulamadık ama dert etmeyin, Lichess pabucunuzu dama atmadı. Size ait bulmacaların eklenme şansını artırmak için rapid ve klasik maçlar oynayın!";i['fromXGames']=s("%s adlı kullanıcının oyunlarından bulmacalar");i['fromXGamesFound']=s("%2$s oyun içinde %1$s bulmaca bulundu");i['goals']="Hedefler";i['goodMove']="İyi hamle";i['harder']="Zor";i['hardest']="Çok zor";i['hidden']="gizli";i['history']="Bulmaca geçmişi";i['improvementAreas']="Gelişim alanları";i['improvementAreasDescription']="Gelişiminizi optimize etmek için bunlar üzerine çalışın!";i['jumpToNextPuzzleImmediately']="Hemen sonraki bulmacaya atla";i['keepGoing']="Devam et...";i['lengths']="Uzunluğa göre";i['lookupOfPlayer']="Bir oyuncunun oyunlarından bulmaca ara";i['mates']="Matlar";i['motifs']="Motifler";i['nbPlayed']=p({"one":"%s bulmaca","other":"%s bulmaca"});i['nbPointsAboveYourPuzzleRating']=p({"one":"Bulmaca puanınızdan bir puan yüksek","other":"Bulmaca puanınızdan %s puan yüksek"});i['nbPointsBelowYourPuzzleRating']=p({"one":"Bulmaca puanınızın bir puan altında","other":"Bulmaca puanınızın %s puan altında"});i['nbToReplay']=p({"one":"%s gözden geçirilecek","other":"%s gözden geçirilecek"});i['newStreak']="Yeni seri";i['nextPuzzle']="Sıradaki bulmaca";i['noPuzzlesToShow']="Gösterilecek bir şey yok. Önce biraz bulmaca çöz!";i['normal']="Normal";i['notTheMove']="Olmadı!";i['openingsYouPlayedTheMost']="Dereceli oyunlarda en çok oynadığınız açılışlar";i['origin']="Kaynak";i['percentSolved']=s("%s çözüldü");i['phases']="Oyun aşamaları";i['playedXTimes']=p({"one":"%s kez çözüldü","other":"%s kez çözüldü"});i['puzzleComplete']="Bulmaca tamamlandı!";i['puzzleDashboard']="Bulmaca kontrol paneli";i['puzzleDashboardDescription']="Egzersiz yapın, analiz edin, becerilerinizi geliştirin";i['puzzleId']=s("Bulmaca %s");i['puzzleOfTheDay']="Günün bulmacası";i['puzzles']="Bulmacalar";i['puzzlesByOpenings']="Açılışlardan bulmacalar";i['puzzleSuccess']="Başarılı!";i['puzzleThemes']="Bulmaca temaları";i['ratingX']=s("Puan: %s");i['recommended']="Önerilen";i['searchPuzzles']="Bulmaca ara";i['solved']="çözüldü";i['specialMoves']="Özel hamleler";i['streakDescription']="Giderek zorlaşan bulmacaları çözün ve bir galibiyet serisi oluşturun. Zaman sınırı yok, bu yüzden acele etmeyin. Bir yanlış hamleye oyun biter! Fakat tur başına bir hamle atlayabilirsiniz.";i['streakSkipExplanation']="Serini korumak için bu hamleyi atla! Her turda sadece bir atlama hakkın var.";i['strengthDescription']="En iyi performansı bu temalarda gösteriyorsunuz";i['strengths']="Güçlü yönler";i['toGetPersonalizedPuzzles']="Kişiselleştirilmiş bulmacalar için:";i['trySomethingElse']="Başka bir şey dene.";i['upVote']="Bulmacayı beğen";i['useCtrlF']="Favori açılışınızı bulmak bulmak için Ctrl+f kısayolunu kullanın!";i['useFindInPage']="Favori açılışlarınızı bulmak için tarayıcı menüsündeki \\\" Sayfa içinde bul\\\" seçeneğini kullanın!";i['voteToLoadNextOne']="Sıradakine geçmek için oy verin!";i['yourPuzzleRatingWillNotChange']="Bulmaca puanınız değişmeyecek. Bulmacaların bir yarış olmadığını unutmayın. Bu puan mevcut becerilerinize en uygun bulmacayı seçmeye yardımcı olur.";i['yourStreakX']=s("Seriniz: %s")})()