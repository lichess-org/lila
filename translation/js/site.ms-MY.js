"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.site)window.i18n.site={};let i=window.i18n.site;i['abortGame']="Batalkan permainan";i['abortTheGame']="Batalkan permainan";i['about']="Perihal";i['aboutSimul']="Simuls melibatkan seorang pemain melawan beberapa pemain serentak.";i['aboutSimulImage']="Dalam 50 lawan, Fisher menang 47 permainan, seri 2 dan kalah 1.";i['aboutSimulRealLife']="Konsep ini diambil daripada hal dunia sebenar. Dalam dunia sebenar, simul host diperlukan untuk bergerak dari satu meja ke meja yang lain untuk bermain satu gerakan.";i['aboutX']=s("Perihal %s");i['accept']="Terima";i['accountCanLogin']=s("Anda boleh log masuk sekarang sebagai %s.");i['accountClosed']=s("Akaun %s telah ditutup.");i['accountConfirmationEmailNotNeeded']="Anda tidak memerlukan e-mel pengesahan.";i['accountConfirmed']=s("Pengguna %s berjaya disahkan sepenuhnya.");i['accountRegisteredWithoutEmail']=s("Akaun %s telah didaftarkan tanpa e-mel.");i['activePlayers']="Pemain aktif";i['addCurrentVariation']="Tambah variasi semasa";i['advancedSettings']="Ketetapan lanjutan";i['advantage']="Kelebihan";i['aiNameLevelAiLevel']=s("%1$s tahap %2$s");i['allInformationIsPublicAndOptional']="Setiap maklumat dapat dilihat umum dan tidak wajib.";i['allSet']="Siap!";i['always']="Selalu";i['analysis']="Analisis papan";i['analysisOptions']="Pilihan analisis";i['andSaveNbPremoveLines']=p({"other":"dan simpan %s urutan pre-gerakan"});i['anonymous']="Nyah-identiti";i['anotherWasX']=s("Yang lain adalah %s");i['apply']="Hantar";i['asBlack']="sebagai hitam";i['asFreeAsLichess']="Percuma seperti Lichess";i['asWhite']="sebagai putih";i['automaticallyProceedToNextGameAfterMoving']="Digerakan ke permainan seterusnya secara automatik selepas gerakan";i['autoSwitch']="Tukar secara automatik";i['availableInNbLanguages']=p({"other":"Terdapat dalam %s bahasa!"});i['averageElo']="Penilaian purata";i['averageOpponent']="Pihak lawan purata";i['averageRatingX']=s("Purata penilaian: %s");i['background']="Latar belakang";i['backgroundImageUrl']="URL Imej latar belakang:";i['backToGame']="Back to game";i['backToTournament']="Back to tournament";i['berserkRate']="Rate berserk";i['bestMoveArrow']="Anak panah gerak terbaik";i['bestWasX']=s("%s adalah yang terbaik");i['biography']="Biografi";i['biographyDescription']="Ceritakan diri anda, minat anda, mengapa anda suka catur, bukaan kegemaran, pemain, ...";i['black']="Hitam";i['blackCastlingKingside']="Hitam O-O";i['blackCheckmatesInOneMove']="Hitam checkmate dalam satu langkah gerakan";i['blackDeclinesDraw']="Pemain hitam menolak seri";i['blackDidntMove']="Hitam tidak bergerak";i['blackIsVictorious']="Hitam menang";i['blackLeftTheGame']="Hitam telah meninggalkan perlawanan";i['blackOffersDraw']="Pemain hitam tawar seri";i['blackPlays']="Hitam untuk main";i['blackResigned']="Hitam telah menyerah kalah";i['blackTimeOut']="Hitam habis masa";i['blackWins']="Menang Hitam";i['blackWinsGame']="Hitam menang";i['blankedPassword']="Anda telah menggunakan kata laluan yang sama di laman sesawang lain, dan laman sesawang tersebut telah dicerobohi. Untuk memastikan keselamatan akaun Lichess anda, kami memerlukan anda menetapkan kata laluan baharu. Terima kasih kerana kerjasama anda.";i['blitz']="Blitz";i['block']="Menghalang";i['blocked']="Dihalang";i['blocks']=p({"other":"%s disekat"});i['blog']="Blog";i['blunder']="Kesilapan";i['boardEditor']="Papan penyunting";i['bookmarkThisGame']="Tanda buku permainan ini";i['builtForTheLoveOfChessNotMoney']="Dibina untuk semangat catur bukan duit";i['bulletBlitzClassical']="Peluru, blitz, klasik";i['by']=s("oleh %s");i['byCPL']="Oleh CPL";i['calculatingMoves']="Mengira pergerakan...";i['cancel']="Batal";i['cancelRematchOffer']="Batal tawaran lawan semula";i['captcha.fail']="Tolong selesaikan captcha catur.";i['capture']="Tangkap";i['castling']="Castling";i['casual']="Casual";i['casualTournament']="Casual";i['changeEmail']="Tukar emel";i['changePassword']="Tukar kata kunci";i['changeUsername']="Tukar nama pengguna";i['changeUsernameDescription']="Tukar nama pengguna. Tindakan ini hanya boleh dibuat sekali dan anda hanya dibenarkan menukar huruf besar/huruf kecil pada nama pengguna anda.";i['changeUsernameNotSame']="Hanya huruf besar atau huruf kecil pada perkataan boleh ditukar. Contohnya, \\\"johndoe\\\" kepada \\\"JohnDoe\\\".";i['chat']="Sembang";i['chatRoom']="Ruang perbualan";i['cheat']="Penipuan";i['cheatDetected']="Penipuan Dikesan";i['checkmate']="Checkmate";i['checkSpamFolder']="Semak juga folder spam anda, ia mungkin telah dihantar di sana. Jika ya, tandakannya sebagai bukan spam.";i['chess960StartPosition']=s("Kedudukan mula Chess960: %s");i['chessBasics']="Asas catur";i['claimADraw']="Tuntut Seri";i['classical']="Classical";i['clearBoard']="Kosongkan papan";i['clickOnTheBoardToMakeYourMove']="Klik pada papan permainan untuk membuat langkah anda, dan buktikan anda adalah manusia.";i['clock']="Jam";i['close']="Tutup";i['closingAccountWithdrawAppeal']="Menutup akaun anda akan menarik balik rayuan anda";i['cloudAnalysis']="Analisis Cloud";i['coaches']="Jurulatih";i['community']="Komuniti";i['composeMessage']="Mengarang mesej";i['computer']="Komputer";i['computerAnalysis']="Analisis komputer";i['computerAnalysisAvailable']="Analisis komputer ada";i['computerAnalysisDisabled']="Analisis komputer ditutup";i['computersAreNotAllowedToPlay']="Computers and computer-assisted players are not allowed to play. Please do not get assistance from chess engines, databases, or from other players while playing. Also note that making multiple accounts is strongly discouraged and excessive multi-accounting will lead to being banned.";i['computerThinking']="Komputer sedang berfikir ...";i['conditionalPremoves']="Pre-gerakan bersyarat";i['conditionOfEntry']="Keperluan kemasukan:";i['confirmMove']="Pastikan gerakan";i['congratsYouWon']="Tahniah, anda menang!";i['continueFromHere']="Teruskan dari sini";i['contribute']="Sumbang";i['copyTextToEmail']=s("Salin (Copy) dan tampal (Paste) teks di atas dan hantar ke %s");i['correspondence']="Jangka panjang";i['correspondenceChess']="Catur jangka panjang";i['cpus']="CPU";i['create']="Cipta";i['createAGame']="Mencipta permainan";i['createANewTopic']="Buat topik baharu";i['createANewTournament']="Buat pertandingan baharu";i['createdBy']="Dibuat oleh";i['createdSimuls']="Simuls yang baharu dibuat";i['createTheTopic']="Buat topik baharu";i['cumulative']="Kumulatif";i['currentGames']="Permainan Sekarang";i['currentPassword']="Kata laluan kini";i['custom']="Tersuai";i['dark']="Gelap";i['database']="Pangkalan data";i['daysPerTurn']="Hari setiap giliran";i['decline']="Tolak";i['defeat']="Kalah";i['delete']="Hapus";i['deleteFromHere']="Hapuskan daripada sini";i['deleteThisImportedGame']="Hapus permainan import ini?";i['depthX']=s("Kedalaman %s");i['description']="Huraian";i['disableKidMode']="Lumpuhkan mod kanak-kanak";i['discussions']="Senarai perbualan";i['doItAgain']="Cuba lagi";i['doneReviewingBlackMistakes']="Selesai menyemak kesilapan hitam";i['doneReviewingWhiteMistakes']="Selesai menyemak kesilapan putih";i['download']="Muat turun";i['draw']="Seri";i['drawByMutualAgreement']="Seri dengan persetujuan bersama";i['drawn']="Seri";i['drawOfferAccepted']="Tawaran seri diterima";i['drawOfferCanceled']="Tawaran seri dibatalkan";i['drawOfferSent']="Tawaran seri dihantar";i['draws']="Seri";i['dtzWithRounding']="DTZ50\\\" yang dibundar, mengikut jumlah pegerakkan separuh sehingga sebuah bidak dimakan atau pegerakkan pion seterusnya";i['duration']="Jangka masa";i['edit']="Sunting";i['editProfile']="Sunting profile";i['email']="Emel";i['emailCanTakeSomeTime']="Ia boleh mengambil masa untuk sampai.";i['emailConfirmHelp']="Bantuan dengan pengesahan e-mel";i['emailConfirmNotReceived']="Tidak menerima e-mel pengesahan anda selepas mendaftar?";i['emailForSignupHelp']="Jika semuanya gagal, hantarkan e-mel ini kepada kami:";i['emailMeALink']="Emel saya link tersebut";i['emailSent']=s("Kami telah menghantar e-mel ke %s.");i['emailSuggestion']="Jangan tetapkan alamat e-mel yang dicadangkan oleh orang lain. Mereka akan menggunakannya untuk mencuri akaun anda.";i['embedInYourWebsite']="Papar dalam laman web anda";i['emptyTournamentName']="Biarkan kosong untuk menamakan pertandingan kepada pemain catur terkenal.";i['enable']="Membolehkan";i['enableKidMode']="Pasang mod kanak-kanak";i['endgame']="Endgame";i['endgamePositions']="Kedudukan Akhir Permainan";i['engineFailed']="Ralat memproseskan enjin";i['engineManager']="Pengurus enjin";i['error.email']="Emel ini tidak sah";i['error.email_acceptable']="Emel ini tidak diterima. Sila semak semula, dan cuba lagi.";i['error.email_different']="Ini merupakan emel anda";i['error.email_unique']="Emel tidak sah atau sudah diguna";i['error.min']=s("Sekurang-kurangnya %s");i['error.namePassword']="Tolong jangan gunakan nama pengguna (Username) anda sebagai kata laluan anda.";i['error.provideOneCheatedGameLink']="Tolong berikan sekurangnya satu link kepada game yang dicurigai ditipu.";i['error.required']="Ruangan ini perlu diisi";i['error.unknown']="Nilai tidak sah";i['error.weakPassword']="Kata laluan ini sangat biasa, dan terlalu mudah untuk diteka.";i['estimatedStart']="Anggaran masa bermula";i['evaluatingYourMove']="Menilai langkah anda ...";i['evaluationGauge']="Tolok penialian";i['eventInProgress']="Sedang bermain sekarang";i['everybodyGetsAllFeaturesForFree']="Semua orang boleh menggunakan semua ciri-ciri percuma";i['exportGames']="Export permainan";i['fast']="Laju";i['favoriteOpponents']="Lawan kegemaran";i['fiftyMovesWithoutProgress']="Lima puluh langkah tanpa kemajuan";i['filterGames']="Tapis permainan";i['findBetterMoveForBlack']="Cari langkah terbaik untuk hitam";i['findBetterMoveForWhite']="Cari langkah terbaik untuk putih";i['finished']="Tamat";i['flipBoard']="Flip board";i['follow']="Ikut";i['followAndChallengeFriends']="Ikut dan cabar rakan-rakan";i['following']="Senarai yang diikuti";i['followsYou']="Mengikuti anda";i['followX']=s("Ikut %s");i['forceDraw']="Paksa seri";i['forceResignation']="Tuntut kemenangan";i['forceVariation']="Force variation";i['forgotPassword']="Lupa kata kunci?";i['forum']="Forum";i['freeOnlineChess']="Catur Percuma Dalam Talian";i['friends']="Senarai rakan";i['fromPosition']="Dari kedudukan";i['fullFeatured']="Ciri-ciri premium";i['gameAborted']="Permainan dibatalkan";i['gameAnalysis']="Analysis permainan";i['gameAsGIF']="Permainan sebagai GIF";i['gameInProgress']=s("Permainan anda dengan %s masih berjalan.");i['gameOver']="Permainan Tamat";i['games']="Games";i['gamesPlayed']="Senarai permainan yang disertai";i['getAHint']="Dapatkan petunjuk";i['giveNbSeconds']=p({"other":"Beri %s saat"});i['goDeeper']="Pergi lebih mendalam";i['goodPractice']="Dengan itu, kami perlu memastikan semua pemain mengikuti amalan yang baik.";i['graph']="Graf";i['hangOn']="Sikit lagi!";i['help']="Tolong:";i['hideBestMove']="Sorok langkah terbaik";i['host']="Host";i['hostANewSimul']="Host simul terbaru";i['hostColorX']=s("Warna host: %s");i['howToAvoidThis']="Bagaimana untuk mengelakkan ini?";i['human']="Manusia";i['ifNoneLeaveEmpty']="Jika tiada, biar kosong";i['ifRatingIsPlusMinusX']=s("Jika rating adalah ± %s");i['ifRegistered']="Jika berdaftar";i['important']="Penting";i['importedByX']=s("Diimport oleh %s");i['importGame']="Import permainan";i['importGameCaveat']="Variasi akan dipadamkan. Untuk menyimpannya, import PGN melalui pembelajaran (Study).";i['importGameExplanation']="Tampalkan (Paste) permainan PGN untuk membolehkan anda semak imbas ulang tayang,\nanalisis komputer, perbualan dalam permainan dan URL kongsi bersama.";i['importPgn']="Import PGN";i['inaccuracy']="Inaccuracy";i['inappropriateNameWarning']="Sebarang perkara yang tidak senonoh boleh menyebabkan akaun anda ditutup.";i['inbox']="Peti mel";i['incorrectPassword']="Kata kunci tidak tepat";i['increment']="Tokokan";i['incrementInSeconds']="Penaikan dalam saat";i['infiniteAnalysis']="Analisis tak terbatas";i['inKidModeTheLichessLogoGetsIconX']=s("Dalam mod kanak-kanak, logo Lichess akan mendapat %s icon, jadi anda tahu kanak-kanak anda selamat.");i['inlineNotation']="Notasi dalaman";i['inLocalBrowser']="dalam browser tempatan";i['insideTheBoard']="Dalam papan";i['insufficientMaterial']="Kekurangan bahan";i['inTheFAQ']="di halaman SOALAN-SOALAN LAZIM";i['invalidAuthenticationCode']="Kod authentikasi tidak sah";i['invalidFen']="FEN tidak sah";i['invalidPgn']="PGN tidak sah";i['invalidUsernameOrPassword']="Nama pengguna atau kata kunci tidak sah";i['inYourLocalTimezone']="Dalam zon waktu anda";i['isPrivate']="Peribadi";i['itsYourTurn']="Giliran anda!";i['join']="Serta";i['joinTheGame']="Menyertai permainan";i['keyboardShortcuts']="Kekunci pintas";i['keyMoveBackwardOrForward']="Gerak ke belakang/hadapan";i['keyShowOrHideComments']="tunjuk/sorok komen";i['kidMode']="Mod kanak-kanak";i['kidModeExplanation']="Hal ini adalah untuk keselamatan. Dalam mod kanak-kanak, semua komunikasi ditutup. Pasang tetapan ini untuk kanak-kanak dan pelajar sekolah untuk melindungi mereka daripada pengguna siber yang lain.";i['kingInTheCenter']="Raja di tengah";i['language']="Bahasa";i['lastPost']="Post terakhir";i['lastSeenActive']=s("Aktif: %s");i['latestForumPosts']="Latest forum posts";i['leaderboard']="Papan pendahulu";i['learnFromThisMistake']="Belajar daripada kesilapan ini";i['learnFromYourMistakes']="Belajar daripada kesilapan anda";i['learnMenu']="Belajar";i['lessThanNbMinutes']=p({"other":"Kurang dari %s minit"});i['level']="Tahap";i['light']="Terang";i['list']="Senarai";i['listBlockedPlayers']="Senarai pemain yang disekat";i['loadingEngine']="Memuatkan enjin ...";i['loadPosition']="Memuat naik posisi";i['lobby']="Lobi";i['location']="Lokasi";i['loginToChat']="Log masuk untuk berbual";i['logOut']="Sign out";i['losing']="Kalah";i['losses']="Kalah";i['lossOr50MovesByPriorMistake']="Kehilangan atau 50 gerakan dengan kesilapan sebelumnya";i['lossSavedBy50MoveRule']="Kekalahan diselamatkan oleh peraturan 50-pergerakan";i['makeAStudy']="Untuk penyimpanan dan perkongsian, pertimbangkan untuk membuat kajian pembelajaran.";i['makeMainLine']="Buat garisan utama";i['makePrivateTournament']="Buat pertandingan privasi peribadi and kurangkan akses dengan kata kunci";i['masterDbExplanation']=s("Daripada dua juta permainan OTB %1$s + FIDE permain yang sudah dinilai daripada %2$s hingga %3$s");i['mateInXHalfMoves']=p({"other":"Mate dalam %s separuh-pergerakan"});i['maximumNbCharacters']=p({"other":"Maksimum: %s aksara."});i['maybeIncludeMoreGamesFromThePreferencesMenu']="Mungki masukkan lebih banyak perlawanan daripada pilihan menu?";i['memberSince']="Ahli sejak";i['memory']="Memori";i['menu']="Menu";i['message']="Mesej";i['middlegame']="Middlegame";i['minutesPerSide']="Minit bagi setiap bahagian";i['mistake']="Kesilapan";i['mobileApp']="Aplikasi Mudah Alih";i['mode']="Mod";i['more']="Papar lebih";i['moreThanNbPerfRatedGames']=p({"other":"≥ %1$s %2$s permainan dinilai"});i['moreThanNbRatedGames']=p({"other":"≥ %s rated games"});i['move']="Gerak";i['movesPlayed']="Gerakan dimain";i['moveTimes']="Masa gerakan";i['multipleLines']="Multiple lines";i['mustBeInTeam']=s("Must be in team %s");i['name']="Nama";i['nbBlunders']=p({"other":"%s kesilapan"});i['nbBookmarks']=p({"other":"%s bookmarks"});i['nbDays']=p({"other":"%s hari"});i['nbDraws']=p({"other":"%s seri"});i['nbFollowers']=p({"other":"%s pengikut"});i['nbFollowing']=p({"other":"%s yang diikut"});i['nbForumPosts']=p({"other":"%s pos forum"});i['nbFriendsOnline']=p({"other":"%s rakan dalam talian"});i['nbGames']=p({"other":"%s Permainan"});i['nbGamesInPlay']=p({"other":"%s permainan sedang berlangsung"});i['nbGamesWithYou']=p({"other":"%s permainan dengan anda"});i['nbHours']=p({"other":"%s jam"});i['nbImportedGames']=p({"other":"%s permainan diimport"});i['nbInaccuracies']=p({"other":"%s ketidaktepatan"});i['nbLosses']=p({"other":"%s kalah"});i['nbMinutes']=p({"other":"%s minit"});i['nbMistakes']=p({"other":"%s kesilapan"});i['nbPerfTypePlayersThisWeek']=p({"other":"%1$s pemain %2$s minggu ini."});i['nbPlayers']=p({"other":"%s pemain-pemain"});i['nbPlaying']=p({"other":"%s bermain"});i['nbPuzzles']=p({"other":"%s teka-teki"});i['nbRated']=p({"other":"%s rated"});i['nbSeconds']=p({"other":"%s saat"});i['nbSecondsToPlayTheFirstMove']=p({"other":"%s saat untuk memulakan langkah pertama"});i['nbStudies']=p({"other":"%s belajar"});i['nbTournamentPoints']=p({"other":"%s tournament points"});i['nbWins']=p({"other":"%s menang"});i['needNbMoreGames']=p({"other":"You need to play %s more rated games"});i['needNbMorePerfGames']=p({"other":"Anda perlu bermain %1$s lagi %2$s permainan dinilai"});i['never']="Tidak pernah";i['neverTypeYourPassword']="Jangan sekali-kali taip kata laluan Lichess anda di laman sesawang lain!";i['newOpponent']="Lawan baru";i['newPassword']="Kata laluan baharu";i['newPasswordAgain']="Kata laluan baharu (lagi sekali)";i['newPasswordsDontMatch']="Kata laluan baharu tidak sepadan";i['newPasswordStrength']="Kekuatan kata laluan";i['newTournament']="Pertandingan baru";i['next']="Seterusnya";i['nextXTournament']=s("Pertandingan %s seterusnya:");i['no']="Tidak";i['noChat']="Tiada ruangan sembang";i['noConditionalPremoves']="Tiada pre-gerakan bersyarat";i['noGameFound']="Tiada perlawanan dijumpai";i['noMistakesFoundForBlack']="Tiada kesilapan untuk hitam";i['noMistakesFoundForWhite']="Tiada kesilapan untuk putih";i['none']="Tiada";i['noNoteYet']="Belum ada catatan";i['normal']="Biasa";i['noSimulExplanation']="Perlawanan simul ini tidak wujud.";i['noSimulFound']="Simul tidak ditemui";i['notACheckmate']="Bukan syahmat";i['notes']="Nota";i['notifications']="Pemberitahuan";i['offerDraw']="Tawarkan Keputusan Seri";i['oneDay']="Satu hari";i['oneUrlPerLine']="Satu URL setiap baris.";i['onlineAndOfflinePlay']="Dalam talian dan luar talian permainan";i['onlyExistingConversations']="Hanya perbualan yang wujud";i['onlyFriends']="Hanya rakan-rakan";i['onlyTeamLeaders']="Ketua pasukan sahaja";i['onlyTeamMembers']="Ahli pasukan sahaja";i['opening']="Opening";i['openingEndgameExplorer']="Penjelajah pembukaan/akhir permainan";i['openingExplorer']="Membuka explorer";i['openingExplorerAndTablebase']="Peneroka bukaan & senarai jadual";i['openings']="Pembukaan";i['openStudy']="Open study";i['openTournaments']="Pertandingan terbuka";i['opponent']="Lawan";i['opponentLeftChoices']="Pemain lain mungkin meninggalkan permainan.Anda boleh menuntut kemenangan, dan tuntut seri, atau tunggu jap.";i['opponentLeftCounter']=p({"other":"Lawan telah meninggalkan permainan. Anda boleh menuntut kemenangan dalam %s saat."});i['orLetYourOpponentScanQrCode']="Atau biarkan lawan anda mengimbas kod QR";i['orUploadPgnFile']="Atau muat naik fail PGN";i['other']="Lain-lain";i['outsideTheBoard']="Luar papan";i['password']="Kata kunci";i['passwordReset']="Reset kata kunci";i['passwordSuggestion']="Jangan tetapkan kata laluan yang dicadangkan oleh orang lain. Mereka akan menggunakannya untuk mencuri akaun anda.";i['pasteTheFenStringHere']="Paste teks FEN di sini";i['pasteThePgnStringHere']="Paste text PGN di sini";i['pause']="Jeda";i['pawnMove']="Bidak gerak";i['performance']="Pencapaian";i['perfRatingX']=s("Rating: %s");i['phoneAndTablet']="Telefon bimbit dan tablet";i['pieceSet']="Set bidak";i['play']="Main";i['playChessEverywhere']="Main catur dimana-mana";i['playChessInStyle']="Main catur dengan bergaya";i['playComputerMove']="Mainkan langkah terbaik komputer";i['player']="Pemain";i['players']="Senarai pemain";i['playEveryGame']="Main setiap permainan yang anda mulakan.";i['playFirstOpeningEndgameExplorerMove']="Main pembukaan pertama/permainantamat-penjelajah gerakan";i['playingRightNow']="Sedang bermain sekarang";i['playVariationToCreateConditionalPremoves']="Tambah variasi untuk membuat pre-gerakan bersyarat";i['playWithAFriend']="Main bersama rakan";i['playWithTheMachine']="Bermain dengan mesin";i['playX']=s("Gerak %s");i['pleasantChessExperience']="Matlamat kami adalah untuk memberikan pengalaman catur yang baik untuk semua.";i['points']="Mata";i['popularOpenings']="Pembukaan terkenal";i['posts']="Pos";i['potentialProblem']="Apabila masalah yang berpotensi dikesan, kami memaparkan mesej ini.";i['practice']="Latihan";i['practiceWithComputer']="Berlatih dengan komputer";i['privacy']="Privasi";i['privacyPolicy']="Polisi privasi";i['proceedToX']=s("Teruskan ke %s");i['profile']="Profil";i['profileCompletion']=s("Kesempurnaan profil: %s");i['promoteVariation']="Naikkan variasi";i['proposeATakeback']="Mencadang ambil semula";i['puzzles']="Teka-teki";i['quickPairing']="Seimbangan pantas";i['raceFinished']="Perlumbaan tamat";i['randomColor']="Pilihan rawak";i['rank']="Rank";i['rankIsUpdatedEveryNbMinutes']=p({"other":"Rank is updated every %s minutes"});i['rankX']=s("Rank: %s");i['rapid']="Pantas";i['rated']="Rated";i['ratedLessThanInPerf']=s("Dinilai ≤ %1$s dalam %2$s untuk minggu lepas");i['ratedMoreThanInPerf']=s("Dinilai ≥ %1$s dalam %2$s");i['ratedTournament']="Rated";i['rating']="Rating";i['ratingRange']="Jurang rating";i['ratingStats']="Stats rating";i['really']="benar";i['realTime']="Masa sebenar";i['realtimeReplay']="Masa sebenar";i['reason']="Sebab";i['recentGames']="Permainan terkini";i['reconnecting']="Penyambungan semula";i['refreshInboxAfterFiveMinutes']="Tunggu 5 minit dan muat ulang semula di peti masuk e-mel anda.";i['rematch']="Lawan semula";i['rematchOfferAccepted']="Tawaran lawan semula diterima";i['rematchOfferCanceled']="Tawaran lawan semula dibatalkan";i['rematchOfferDeclined']="Tawaran lawan semula ditolak";i['rematchOfferSent']="Tawaran lawan semula dihantar";i['rememberMe']="Kekalkan saya di Log masuk";i['removesTheDepthLimit']="Kurangkan had kedalaman dan pastikan komputer anda sihat";i['reopenYourAccount']="Buka akaun anda semula";i['replayMode']="Mode ulang tayang";i['replies']="Balasan";i['reply']="Balas";i['replyToThisTopic']="Balasan kepada topik ini";i['reportAUser']="Laporkan pengguna";i['reportXToModerators']=s("Laporkan %s ke moderator");i['requestAComputerAnalysis']="Minta analisis komputer";i['required']="Diperlukan.";i['reset']="Tetap Semula";i['resign']="Menyerah kalah";i['resignLostGames']="Menyerah setelah kalah (jangan dibiarkan masa habis).";i['resignTheGame']="Menyerah kalah";i['resume']="Sambung";i['resumeLearning']="Sambung belajar";i['resumePractice']="Sambung latihan";i['retry']="Cuba semula";i['returnToSimulHomepage']="Kembali ke halaman utama simul";i['returnToTournamentsHomepage']="Kembali ke laman pertandingan";i['reviewBlackMistakes']="Semak kesilapan hitam";i['reviewWhiteMistakes']="Semak kesilapan putih";i['revokeAllSessions']="batalkan semua sesi";i['safeTournamentName']="Pilih nama yang selamat untuk pertandingan ini.";i['save']="Simpan";i['screenshotCurrentPosition']="Tangkapan skrin (Screenshot) kedudukan semasa";i['security']="Keselamatan";i['seeBestMove']="Lihat langkah terbaik";i['send']="Hantar";i['sentEmailWithLink']="Kami telah menghantar anda emel berserta pautan.";i['setTheBoard']="Perbaiki papan catur";i['showThreat']="Tunjukkan ancaman";i['showVariationArrows']="Tunjuk anak panah variasi";i['side']="Tepi";i['signIn']="Sign in";i['signUp']="Register";i['signupEmailHint']="Kami hanya akan menggunakannya untuk penukaran kata laluan.";i['signupUsernameHint']="Pastikan anda memilih nama yang elok. Anda tidak boleh menukarnya kemudian dan jika sesuatu akaun menggunakan nama yang tidak elok, akaun tersebut akan ditutup!";i['simultaneousExhibitions']="Perlawanan serentak";i['since']="Sejak";i['siteDescription']="Free online chess server. Play chess in a clean interface. No registration, no ads, no plugin required. Play chess with the computer, friends or random opponents.";i['skipThisMove']="Langkau langkah ini";i['slow']="Perlahan";i['socialMediaLinks']="Pautan media sosial";i['solution']="Penyelesaian";i['sorry']="Maaf :(";i['sound']="Bunyi";i['sourceCode']="Source Code";i['spectatorRoom']="Ruang penonton";i['stalemate']="Stalemate";i['standard']="Standard";i['standByX']=s("Berada dengan %s, permain yang diseimbangkan, sila bersedia!");i['standing']="Kedudukan";i['startedStreaming']="mula streaming";i['starting']="Mula:";i['startPosition']="Posisi permulaan";i['streamersMenu']="Senarai streamer";i['strength']="Kekuatan";i['studyMenu']="Selidik";i['subject']="Subjek";i['success']="Berjaya";i['switchSides']="Tukar pihak";i['takeback']="Ambil semula";i['takebackPropositionAccepted']="Tawaran ambil semula diterima";i['takebackPropositionCanceled']="Tawaran ambil semula dibatalkan";i['takebackPropositionDeclined']="Tawaran ambil semula ditolak";i['takebackPropositionSent']="Tawaran ambil semula dihantar";i['talkInChat']="Sila jadi baik di ruangan sembang";i['temporaryInconvenience']="Kami memohon maaf atas kesulitan yang dialami,";i['termsOfService']="Terma servis";i['thankYou']="Terima kasih!";i['thankYouForReading']="Terima kasih kerana membaca!";i['theFirstPersonToComeOnThisUrlWillPlayWithYou']="Orang pertama yang datang ke URL ini akan bermain dengan anda.";i['theGameIsADraw']="Permainan seri.";i['thematic']="Thematic";i['thisAccountViolatedTos']="Akaun ini melanggar Syarat Perkhidmatan Lichess";i['thisGameIsRated']="Permainan ini termasuk dalam rated";i['thisIsAChessCaptcha']="Ini adalah CAPTCHA catur.";i['thisTopicIsNowClosed']="Topik ini sekarang ditutup.";i['threeChecks']="Tiga check";i['threefoldRepetition']="Threefold repetition";i['time']="Masa";i['timeAlmostUp']="Masa hampir habis!";i['timeControl']="Peruntukan masa";i['timeline']="Linimasa";i['today']="Hari ini";i['toggleLocalEvaluation']="Togol penilaian tempatan";i['toggleTheChat']="Togol bual";i['toInviteSomeoneToPlayGiveThisUrl']="Untuk menjemput seseorang bermain, berikan pautan ini";i['tools']="Peralatan";i['topGames']="Permainan teratas";i['topics']="Topik";i['toStudy']="Belajar";i['tournament']="Pertandingan";i['tournamentCalendar']="Kalendar pertandingan";i['tournamentComplete']="Pertandingan selesai";i['tournamentDoesNotExist']="Pertandingan tidak wujud.";i['tournamentEntryCode']="Kod kemasukan pertandingan";i['tournamentIsStarting']="Pertandingan ini akan bermula";i['tournamentNotFound']="Pertandingan tidak ditemui";i['tournamentPairingsAreNowClosed']="Keseimbangan pertandingan sekarang ditutup.";i['tournamentPoints']="Mata pertandingan";i['tournaments']="Senarai pertandingan";i['tournChat']="Ruangan sembang pertandingan";i['tpTimeSpentOnTV']=s("Masa pada TV: %s");i['tpTimeSpentPlaying']=s("Masa diguna bermain: %s");i['transparent']="Lutsinar";i['troll']="Unsur jenaka";i['tryAnotherMoveForBlack']="Cuba langkah lain untuk hitam";i['tryAnotherMoveForWhite']="Cuba langkah lain untuk putih";i['tryToWin']="Cuba menang (atau sekurang-kurangnya seri) setiap permainan yang anda mainkan.";i['typePrivateNotesHere']="Tulis nota peribadi di sini";i['unblock']="Nyah-halang";i['unfollow']="Berhenti mengikuti";i['unfollowX']=s("Nyahikut %s");i['unknown']="Tidak diketahui";i['unknownDueToRounding']="Menang/kalah hanya dijamin jika garis pangkalan jadual yang disyorkan telah diikuti sejak tangkapan terakhir atau perpindahan pawn, disebabkan kemungkinan pembulatan nilai DTZ dalam pangkalan meja Syzygy.";i['unlimited']="Tanpa Had";i['until']="Hingga";i['user']="Pengguna";i['userIsBetterThanPercentOfPerfTypePlayers']=s("%1$s lebih bagus daripada %2$s pemain %3$s.");i['username']="Nama pengguna";i['usernameAlreadyUsed']="Nama pengguna ini sudah digunakan. Sila pilih nama pengguna yang lain dan cuba lagi.";i['usernameCanBeUsedForNewAccount']="Anda boleh menggunakan nama pengguna (Username) ini untuk membuat akaun baharu";i['usernameCharsInvalid']="Nama pengguna mesti hanya mengandungi huruf, nombor, garisan dan sempang.";i['usernameNotFound']=s("Kami tidak dapat mencari mana-mana pengguna dengan nama ini: %s");i['usernameOrEmail']="Nama pengguna atau emel";i['usernamePrefixInvalid']="Nama pengguna anda mesti bermula dengan huruf.";i['usernameSuffixInvalid']="Nama pengguna mesti tamat dengan satu huruf atau nombor.";i['usingServerAnalysis']="Menggunakan server analisis";i['variant']="Variasi";i['variantEnding']="Variasi berakhir";i['variantLoss']="Variasi kalah";i['variants']="Senarai variasi";i['variantWin']="Variasi menang";i['victory']="Menang";i['videoLibrary']="Pusat video";i['viewInFullSize']="View in full size";i['viewRematch']="Paparkan lawan semula";i['views']="Paparkan";i['viewTheSolution']="Papar jawapan";i['viewTournament']="Lihat pertandingan";i['waitForSignupHelp']="Kami akan kembali sebentar lagi untuk membantu anda melengkapkan pendaftaran anda.";i['waiting']="Menunggu";i['waitingForAnalysis']="Menunggu analisis";i['waitingForOpponent']="Menunggu untuk lawan";i['watch']="Tonton";i['watchGames']="Tonton permainan";i['webmasters']="Webmasters";i['weeklyPerfTypeRatingDistribution']=s("Taburan penilaian %s mingguan");i['weHadToTimeYouOutForAWhile']="Kami terpaksa memberikan time out buat sementara.";i['whatIsIheMatter']="Apa masalahnya?";i['whatSignupUsername']="Nama apa yang awak gunakan untuk mendaftar?";i['white']="Putih";i['whiteCastlingKingside']="Putih O-O";i['whiteCheckmatesInOneMove']="Putih checkmate dalam satu langkah gerakan";i['whiteDeclinesDraw']="Pemain putih menolak seri";i['whiteDidntMove']="Putih tidak bergerak";i['whiteDrawBlack']="Putih / Seri / Hitam";i['whiteIsVictorious']="Putih menang";i['whiteLeftTheGame']="Putih telah meninggalkan perlawanan";i['whiteOffersDraw']="Pemain putih tawar seri";i['whitePlays']="Putih untuk main";i['whiteResigned']="Putih telah menyerah kalah";i['whiteTimeOut']="Putih habis masa";i['whiteWins']="Buah putih menang";i['whiteWinsGame']="Putih menang";i['why']="Kenapa?";i['winner']="Pemenang";i['winning']="Menang";i['winOr50MovesByPriorMistake']="Menang atau 50 gerakan dengan kesilapan sebelumnya";i['winPreventedBy50MoveRule']="Kemenangan yang dihalan oleh peraturan 50-pergerakan";i['winRate']="Rate menang";i['wins']="Menang";i['wishYouGreatGames']="kami berharap anda mempunyai permainan yang baik di lichess.org.";i['withdraw']="Tarik diri";i['withEverybody']="Dengan semua";i['withFriends']="Dengan rakan-rakan";i['withNobody']="Dengan tiada siapa-siapa";i['xCreatedTeamY']=s("%1$s mencipta pasukan %2$s");i['xHostsY']=s("%1$s meng\\'host\\' %2$s");i['xIsAFreeYLibreOpenSourceChessServer']=s("%1$s adalah percuma (%2$s), mudah, tiada iklan, server catur daripada open source.");i['xJoinedTeamY']=s("%1$s menyertai pasukan %2$s");i['xJoinsY']=s("%1$s menyertai %2$s");i['xLikesY']=s("%1$s menyukai%2$s");i['xOpeningExplorer']=s("%s membuka explorer");i['xPostedInForumY']=s("%1$s posted in topic %2$s");i['xRating']=s("%s rating");i['xStartedFollowingY']=s("%1$s mula mengikuti %2$s");i['xStartedStreaming']=s("%s mula siaran langsung");i['xWasPlayed']=s("%s dimainkan");i['yes']="Ya";i['yesterday']="Semalam";i['youAreLeavingLichess']="Anda akan meninggalkan Lichess";i['youAreNotInTeam']=s("You are not in the team %s");i['youArePlaying']="Anda sedang bermain!";i['youCanDoBetter']="Anda boleh cuba dengan lebih baik";i['youCantStartNewGame']="Anda tidak boleh memulakan permainan baru sehingga permainan ini selesai.";i['youHaveBeenTimedOut']="Anda telah dilog keluar.";i['youNeedAnAccountToDoThat']="You need an account to do that";i['youPlayTheBlackPieces']="Mula dengan buah hitam";i['youPlayTheWhitePieces']="Mula dengan buah putih";i['yourOpponentOffersADraw']="Lawan anda menawarkan seri";i['yourOpponentProposesATakeback']="Lawan anda mencadangkan untuk ambil semula";i['yourOpponentWantsToPlayANewGameWithYou']="Lawan anda mahu bermain permainan yang baru dengan anda";i['yourPerfRatingIsProvisional']=s("Penilaian %s anda adalah sementara");i['yourPerfRatingIsTooHigh']=s("Penilaian %1$s anda (%2$s) terlalu tinggi");i['yourPerfRatingIsTooLow']=s("Penilaian %1$s anda (%2$s) terlalu rendah");i['yourQuestionMayHaveBeenAnswered']=s("Soalan anda mungkin telah dijawab %1$s");i['yourScore']=s("Markah anda: %s");i['yourTopWeeklyPerfRatingIsTooHigh']=s("Penilaian %1$s teratas mingguan anda (%2$s) terlalu tinggi");i['yourTurn']="Giliran anda";i['zeroAdvertisement']="Sifar iklan"})()