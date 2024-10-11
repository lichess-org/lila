"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="帳戶";i['acplExplanation']="釐兵是國際象棋計算優勢所用的計量單位。1釐兵等於兵價值的1/100。也就是說，100釐兵=1兵。這些價值在游戲中不起正式作用，但對玩家有用，對電腦來評估局面也很重要。\n\n最佳的電腦著法將會損失0釐兵，但較差的著法將導致釐兵計量下局面的惡化。\n\n該值可以用作著法質量的一個指標。每次失去的釐兵越少，著法就越強。\n\nLichess上的電腦分析是基於Stockfish的。";i['adviceOnMitigatingAddiction']=s("我們時常收到使用者希望能夠暫時讓他們放下西洋棋作更重要的事情。\n\n雖然 Lichess 不會在使用者不違反服務條款的情況下以任何理由踢出或封禁玩家，我們建議以其他工具限制過量的下棋行為。有些人建議使用網站阻擋器例如 %1$s、%2$s、以及%3$s。如果你想在不被時間控制器所干擾的情況下繼續使用此網站，你可以參考 %4$s，或是擁有更少人的%5$s。\n\n有些玩家擔心他們過量的遊玩西洋棋已經惡化成為一種上癮。事實上，世界衛生組織將遊戲成癮分類為一種 %6$s，並且有以下特點\n\n1）無法自制遊戲成癮\n2）將遊戲列為優先\n3）即使有負面影響仍然增加遊戲時間\n\n如果你認為你下西洋棋的行為有以下症狀，我們鼓勵你與一位朋友、家庭成員或專業談談。");i['aHourlyBulletTournament']="小時制Bullet模式錦標賽";i['areThereWebsitesBasedOnLichess']="有基於Lichess的網頁嗎？";i['asWellAsManyNMtitles']="眾多國家大師頭銜";i['basedOnGameDuration']=s("Lichess上的時限基於預期的對弈時間 %1$s\n例如，5+3的預期對弈時間為 5 × 60 + 40 × 3 = 420秒。");i['beingAPatron']="成為贊助者";i['beInTopTen']="在此模式下得到前10的積分";i['breakdownOfOurCosts']="經費收支";i['canIbecomeLM']="我可以獲得 Lichess Master (LM) 頭銜嗎？";i['canIChangeMyUsername']="我可以更改我的使用者名稱嗎？";i['configure']="配置";i['connexionLostCanIGetMyRatingBack']="我因為斷線或lag而導致我輸了。我可以加回我的分數嗎?";i['desktop']="電腦";i['discoveringEnPassant']="假設兵已經過了對方兵控制的格子，為什麼還能夠被吃掉？(吃過路兵)";i['displayPreferences']="隱藏積分顯示";i['durationFormula']="(初始時間) + 40 x (加時)";i['eightVariants']="8種變體";i['enableAutoplayForSoundsA']="大多瀏覽器都會禁止網站在新載入分頁中播放影音以保護使用者。想像所有網站都可以以各種影音廣告轟炸您的耳朵。\n\n紅色靜音按鈕會在音效被阻擋時顯示。大多在點擊棋子時才會停用此種限制。在某些行動版瀏覽器上，拖曳一個棋子不會被認為為一種點擊方式。在那種情況下您必須在每個棋局開始後點擊棋盤以啟用音效。\n\n在此種限制發生時我們會以紅色圖示警告你。大多時候可以允許 lichess.org 播放音效。以下是如何在大多瀏覽器上啟用音效：";i['enableAutoplayForSoundsChrome']="1. 到 lichess.org\n2. 在搜尋欄中點擊開關圖示\n3. 點擊網站設定\n4. 允許音效";i['enableAutoplayForSoundsFirefox']="1. 到 lichess.org\n2. 在 Linux/Windows 上按下 Ctrl-i  或是在 macOS 按下 cmd-i\n3. 點擊「權限」分頁\n4. 在 lichess.org 允許自動播放影音內容";i['enableAutoplayForSoundsMicrosoftEdge']="1. 點擊右上角的選單\n2. 點擊設定\n3. 點擊「Cookie 和網站權限」\n4. 捲動到底下並點擊媒體自動播放\n5. 增加 lichess.org 以允許";i['enableAutoplayForSoundsQ']="如何啟用自動播放聲音？";i['enableAutoplayForSoundsSafari']="1. 到 lichess.org\n2. 點擊 Safari 的選單\n3. 點擊「lichess.org 的設定...」\n4. 允許自動播放";i['enableDisableNotificationPopUps']="啟用或禁用通知彈出窗口？";i['enableZenMode']=s("在 %1$s 中啟用 Zen 模式，或在遊戲期間按 %2$s。");i['explainingEnPassant']=s("這是一個叫做“吃過路兵”的合法著法。維基百科文章給出了一個%1$s。\n\n在3.7 (d)節中作了在%2$s裡描述。\n\n“對方一隻兵剛剛從初始位置起步走兩格，與本方的兵處在相鄰橫線時，本方的兵可以吃掉它，就如同它只走了一格一樣。這種吃法稱為‘吃過路兵’，只能在對方兵走兩格之後立即進行。“\n\n請查看 %3$s 練習這個著法。");i['fairPlay']="公平的下棋";i['fairPlayPage']="公平對奕頁面";i['faqAbbreviation']="常見問題";i['fewerLobbyPools']="這個";i['fideHandbook']="FIDE 手冊";i['fideHandbookX']=s("國際棋聯手冊 %s");i['findMoreAndSeeHowHelp']=s("你可以在這找到更多關於 %1$s和%2$s的細節。如果你願意貢獻你的時間及技術在Lichess，這裡有更多的 %3$s。");i['frequentlyAskedQuestions']="常見問題";i['gameplay']="下棋";i['goldenZeeExplanation']="當時，ZugAddict己經直播了兩個小時，但他仍未能在1+0的棋局中擊敗第八級人工智能。Thibault吿訴他，如果他能在直播上成功擊敗人工智能，便會獲得一個獨特的奬盃。一個小時後，他粉碎了Stockfish， Thibault也兌現了承諾。";i['goodIntroduction']="好的自我介紹";i['guidelines']="指南";i['havePlayedARatedGameAtLeastOneWeekAgo']="在上週內玩過一個有評分的遊戲，";i['havePlayedMoreThanThirtyGamesInThatRating']="至少玩過 30 場有評分的遊戲，";i['hearItPronouncedBySpecialist']="聽聽看標準發音";i['howBulletBlitzEtcDecided']="Bullet、Blitz和其他模式的時限是怎麼決定的？";i['howCanIBecomeModerator']="我要如何成為一個仲裁者？";i['howCanIContributeToLichess']="我可以怎樣來幫助Lichess?";i['howDoLeaderoardsWork']="排名和排行榜是如何運作的?";i['howToHideRatingWhilePlaying']="如何在奕棋時隱藏評分?";i['howToThreeDots']="如何...";i['inferiorThanXsEqualYtimeControl']=s("≤ %1$s秒屬於 %2$s 比賽");i['inOrderToAppearsYouMust']=s("要登上 %1$s，您必須：");i['insufficientMaterial']="超時判負、和棋、以及子力不足";i['isCorrespondenceDifferent']="通訊賽和常規賽有差異嗎？";i['keyboardShortcuts']="有哪些快捷鍵？";i['keyboardShortcutsExplanation']="某些 Lichess 頁面有快捷鍵。嘗試在研究、分析、謎題、或下棋頁面中按下「？」以查看可用快捷鍵。";i['leavingGameWithoutResigningExplanation']="如果你的對手經常放棄／中離棋局，他們會受到＂禁止對弈＂的懲罰。代表他們暫時無法對弈。這不會顯示在他們的個人資料上。如果情況未改善，每次懲罰的時間將會延長，並有可能導致帳號被封鎖。";i['leechess']="lee-chess";i['lichessCanOptionnalySendPopUps']="Lichess 可以選擇發送彈出通知，例如當輪到您進行下一步或當您收到私人消息時。\n單擊瀏覽器 URL 欄中 lichess.org 網址旁邊的鎖定圖標。\n然後選擇是否允許還是阻止來自 Lichess 的通知。";i['lichessCombinationLiveLightLibrePronounced']=s("Lichess來自\\'直播/光明/自由\\'(live/light/libre) 加上\\'國際象棋\\'(chess) 的組合，讀做%1$s。");i['lichessFollowFIDErules']=s("若玩家消盡了時間，該玩家通常會被判負。另外，如果對手無法在允許的最大限著 (%1$s回合) 內將殺玩家的王，對局就是和棋。\n\n在極少數情況下，這可能很難自動決定（強制線路、堡壘）。 默認情況下，沒有耗時的玩家總會贏。\n\n註意：如果對方一隻棋子阻擋自己的王，那麼單馬或者單象也有可能實現將殺。");i['lichessPoweredByDonationsAndVolunteers']="Lichess能夠順利運作是因為有許多人的贊助以及一群自願者的努力。";i['lichessRatings']="Lichess 評分";i['lichessRecognizeAllOTBtitles']=s("Lichess接受所有OTB (在棋盤上) 對局得到的國際象棋聯稱號，以及%1$s，以下列出：");i['lichessSupportChessAnd']=s("Lichess上有標準國際象棋和%1$s");i['lichessTraining']="Lichess 訓練";i['lichessUserstyles']="Lichess userstyles（與 stylus 一併使用）";i['lMtitleComesToYouDoNotRequestIt']="這個尊稱是非官方的，只存在於 Lichess 上。\n我們很少根據自己的判斷將其授予作為 Lichess 好公民的知名玩家。你不能自行得到 LM 頭銜，是 LM 頭銜會過來給你。如果您符合條件，您將會收到我們關於它以及接受或拒絕選擇的消息。\n\n請問要求取得 LM 頭銜。";i['mentalHealthCondition']="獨立的心理健康狀況";i['notPlayedEnoughRatedGamesAgainstX']=s("該玩家尚未完成與評級類別中的 %1$s 的足夠多的評級遊戲。");i['notPlayedRecently']="一個玩家最近玩的遊戲不夠多。根據您玩過的遊戲數量，您的評分可能需要大約一年不活動才能再次變為臨時評分。";i['notRepeatedMoves']="我們沒有重復著法。為什麼對局仍然被判和？";i['noUpperCaseDot']="不行";i['otherWaysToHelp']="其他尋求幫助的方法";i['ownerUniqueTrophies']=s("這個獎杯在 Lichess 的歷史上是獨一無二的，除了 %1$s 之外沒有人曾經擁有它。");i['pleaseReadFairPlayPage']=s("請查閱%s以取得更多資訊");i['positions']="位置";i['preventLeavingGameWithoutResigning']="對那些沒有認輸就離開棋局的棋手會怎麼處理？";i['provisionalRatingExplanation']="那個問號表示評級是臨時的。原因包括：";i['ratingDeviationLowerThanXinChessYinVariants']=s("在標準國際象棋中評級偏差低於 %1$s，在變體中低於 %2$s，");i['ratingDeviationMorethanOneHundredTen']="具體而言，這意味著 Glicko-2 偏差大於 110。偏差是系統對評級的可信程度。偏差越小，評級越穩定。";i['ratingLeaderboards']="評分排行表";i['ratingRefundExplanation']="在一個棋手被標記的一分鐘後，他們最近的40局積分賽將在3天內無效。如過你是他們這幾局的對手並且積分不是浮動的狀態，你輸掉的積分(輸局或和局) 將會退回，退回的積分上限基於你的最高積分及在那些棋局之後的積分變化。\n(例如：你如果在那些棋局之後積分大幅提升，你可能就不會拿回那些積分或是只拿回一部分)。且退回的積分永遠不會超過150分。";i['ratingSystemUsedByLichess']="評分是使用 Mark Glickman 開發的 Glicko-2 評分方法計算的。這是一種非常流行的評分方法，並且被大量國際象棋組織使用(FIDE 是一個值得注意的反面例子，因為他們仍然使用過時的 Elo 評分系統)。\n從根本上說，Glicko 評分在計算和表示您的評分時使用“信心度區間”。當您第一次開始使用該網站時，您的評分將會從 1500 ± 1000 開始。1500 代表您的評分，1000 代表可信度區間。\n基本上，系統 95% 確定您的評分在 500 到 2500 之間。這是非常不確定的。正因為如此，當一個玩家剛開始時，他們的評分會發生很大的變化，一次可能會上升幾百個評分。但是在與老玩家進行一些棋局之後，信心度區間會變窄，每場比賽後獲得/失去的積分數量會減少。\n還有一點需要注意的是，隨著時間的推移，信心度區間會增加。這使您可以更快地獲得/失去積分，以匹配您現在的技能水平。";i['repeatedPositionsThatMatters']=s("三次重復局面是關於局面的%1$s重復，而不是著法。重復局面不一定會連續發生。");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="第二個要求是讓不再使用其帳戶的玩家停止佔據排行榜。";i['showYourTitle']=s("如果你有 OTB 頭銜（GM、IM、FM、CM）你可以完成%1$s以顯示在帳號中，包括上傳清晰的你與可辨識的身分卡、文件的照片。\n\n\n認證成為頭銜玩家有助於躋身於頭銜競技場並且取得 %2$s 頭銜。");i['similarOpponents']="實力相近的對手";i['stopMyselfFromPlaying']="如何克制西洋棋成癮？";i['superiorThanXsEqualYtimeControl']=s("≥ %1$ss = %2$s");i['threeFoldHasToBeClaimed']=s("三次重復局面和棋需要由其中一個選手提出。您可以通過按下已顯示的按鈕，或者在您最後一次重復移動之前提出和棋。無論對方接不接受，根據三次重複規範，遊戲會以和棋結束。您也可以%1$sLichess為您自動提和重復局面。此外，五次重復局面總是立即結束游戲。");i['threefoldRepetition']="三次重複局面";i['threefoldRepetitionExplanation']=s("如果同一個局面出現了三次，玩家可以根據%1$s提和。Lichess執行FIDE官方規則，這些規則在 %2$s第9.2條中已有規定。");i['threefoldRepetitionLowerCase']="三次重複局面";i['titlesAvailableOnLichess']="Lichess 上有什麼頭銜?";i['uniqueTrophies']="獨一無二的獎盃";i['usernamesCannotBeChanged']="因為某些技術性原因無法更改使用者名稱。使用者名稱已太被廣泛使用：資料庫、導出、日誌、與其他人的腦中。你可以更改大小寫一次。";i['usernamesNotOffensive']=s("一般來說，用戶名不應該是：冒犯性的、冒充他人的或含有廣告的。您可以閱讀有關 %1$s 的更多信息。");i['verificationForm']="驗證表格";i['viewSiteInformationPopUp']="查看網站的信息彈出窗口";i['watchIMRosenCheckmate']=s("觀看國際大師 Eric Rosen 將死 %s");i['wayOfBerserkExplanation']=s("hiimgosu 為了贏得獎盃而以 berserk 比賽鍛鍊自己並且最終贏得每場%s");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="不幸的是，我們無法退還因延遲或斷開連接而丟失的遊戲評分，無論問題出在您方還是我們方。不過後者非常少見。另外請注意，當 Lichess 重新啟動並且您因此而導致棋局過時，我們會中止遊戲以防止不公平的損失。";i['weRepeatedthreeTimesPosButNoDraw']="同一個局面三次已經被重復了。為什麼對局沒有判和？";i['whatIsACPL']="平均釐兵損失 (ACPL) 是什麼？";i['whatIsProvisionalRating']="為什麼評分旁邊有問號 (?) 在旁邊？";i['whatUsernameCanIchoose']="我可以取甚麼樣的使用者名稱？";i['whatVariantsCanIplay']="有哪些變體是在Lichess上玩得到的？";i['whenAmIEligibleRatinRefund']="我什麼時候可以有資格自動取回與作弊者對弈的積分？";i['whichRatingSystemUsedByLichess']="Lichess 使用什麼評分機制?";i['whyAreRatingHigher']="為什麼 FIDE、USCF 和 ICC 等其他網站和組織相比 Lichess 的評級更高？";i['whyAreRatingHigherExplanation']="最好不要將評級視為絕對的數字，或將其與其他組織進行比較。不同的組織有不同級別的玩家，不同的評級系統(Elo、Glicko、Glicko-2 或上述的修改版本)。這些因素會極大地影響絕對數字(評級)。\n 最好將評級視為“相對”數字(而不是“絕對”數字)：在一群玩家中，他們在評級上的相對差異將幫助您估計誰會贏/平手/輸，以及多久。說“我有 X 等級”並沒有任何意義，除非有其他玩家可以比較這個等級。";i['whyIsLichessCalledLichess']="為什麼要叫Lichess?";i['whyIsLilaCalledLila']=s("相同的，Lichess的原始碼 %1$s 代表Li[chess in sca]la，整個Lichess是用 %2$s 此直觀式語言所寫成");i['whyLiveLightLibre']="直播(live)，無時無刻都有人在下棋，而每一場棋局也都被觀賞。光明及自由(light and libre) 代表Lichess是開源軟體及不被版權蟑螂影響。";i['yesLichessInspiredOtherOpenSourceWebsites']=s("有的，Lichess的確有啟發其他使用我們%1$s、%2$s或是%3$s的開源網站。");i['youCannotApply']="你無法主動申請成為一個仲裁者。如果我們看到我們認為有能力擔任仲裁者的人，我們會主動聯繫他們。";i['youCanUseOpeningBookNoEngine']="在Lichess，兩者的最大的規則差異在通信賽可以使用開局庫。但電腦輔助一樣是禁止的，並一樣會被標記為電腦輔助的使用者。\n即便在ICCF允許在通信賽中使用電腦輔助，但Lichess不行。"})()