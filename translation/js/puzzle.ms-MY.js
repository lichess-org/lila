"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzle)window.i18n.puzzle={};let i=window.i18n.puzzle;i['addAnotherTheme']="Tambahkan tema lain";i['advanced']="Lanjutan";i['bestMove']="Gerakan Terbaik!";i['clickToSolve']="Klik untuk menyelesaikan";i['continueTheStreak']="Teruskan coretan";i['continueTraining']="Teruskan Berlatih";i['didYouLikeThisPuzzle']="Adakah kamu menyuka Teka-teki ini?";i['difficultyLevel']="Tahap kesukaran";i['easier']="Lebih Mudah";i['easiest']="Termudah";i['example']="Contoh";i['failed']="gagal";i['findTheBestMoveForBlack']="Cari langkah terbaik untuk hitam.";i['findTheBestMoveForWhite']="Cari langkah terbaik untuk putih.";i['fromGameLink']=s("Dari permainan %s");i['fromMyGames']="Dari permainan saya";i['fromMyGamesNone']="Anda tidak mempunyai teka-teki dalam pangkalan data, tetapi Lichess masih sangat menyayangi anda.\nMain permainan pantas dan klasik untuk meningkatkan peluang anda untuk menambahkan teka-teki anda!";i['fromXGames']=s("Teka-teki dari permainan %s");i['fromXGamesFound']=s("Teka-teki %1$s terdapat dalam permainan %2$s");i['goals']="Sasaran";i['goodMove']="Pergerakan baik";i['harder']="Lebih Susah";i['hardest']="Paling Susah";i['hidden']="tersembunyi";i['history']="Sejarah teka-teki";i['improvementAreas']="Kawasan penambahbaikan";i['improvementAreasDescription']="Latih ini untuk mengoptimumkan kemajuan anda!";i['jumpToNextPuzzleImmediately']="Langsung ke teka-teki seterusnya dengan segera";i['keepGoing']="Terus berusaha…";i['lengths']="Panjang";i['lookupOfPlayer']="Mencari teka-teki dari permainan pemain";i['mates']="Menyekak";i['motifs']="Motif";i['nbPlayed']=p({"other":"%s dimainkan"});i['nbPointsAboveYourPuzzleRating']=p({"other":"%s Satu titik di atas peringkat teka-teki anda"});i['nbPointsBelowYourPuzzleRating']=p({"other":"%s Satu titik di bawah peringkat teka-teki anda"});i['nbToReplay']=p({"other":"%s untuk dimainkan semula"});i['newStreak']="Coretan baru";i['noPuzzlesToShow']="Tiada apa-apa yang ditunjukkan, main dulu teka-teki!";i['normal']="Biasa";i['notTheMove']="Bukan Gerakan Itu!";i['origin']="Asal";i['percentSolved']=s("%s diselesaikan");i['phases']="Fasa";i['playedXTimes']=p({"other":"Bermain selama %s"});i['puzzleComplete']="Teka-teki siap!";i['puzzleDashboard']="Papan Pemuka Teka-teki";i['puzzleDashboardDescription']="Melatih, menganalisis, memperbaiki";i['puzzleId']=s("Teka-teki %s");i['puzzleOfTheDay']="Teka-teki hari ini";i['puzzles']="Teka-teki";i['puzzleSuccess']="Berjaya!";i['puzzleThemes']="Tema Teka-teki";i['ratingX']=s("Penilaian:%s");i['recommended']="Disarankan";i['searchPuzzles']="Cari teka-teki";i['solved']="diselesaikan";i['specialMoves']="Gerakan Khas";i['streakDescription']="Selesaikan teka-teki yang lebih sukar dan membina rentak kemenangan. Tidak ada jam, jadi luangkan masa anda. Satu langkah yang salah, dan ia selesai! Tetapi anda boleh melangkau satu gerakan setiap sesi.";i['streakSkipExplanation']="Langkau langkah ini untuk mengekalkan rentak anda! Hanya berfungsi sekali dalam larian.";i['strengthDescription']="Anda menunjukkan prestasi terbaik dalam tema ini";i['strengths']="Kekuatan";i['toGetPersonalizedPuzzles']="Untuk mendapatkan teka-teki yang diperibadikan:";i['trySomethingElse']="Cuba sesuatu yang lain.";i['voteToLoadNextOne']="Undi untuk memuatkan yang seterusnya!";i['yourStreakX']=s("Coretan anda: %s")})()