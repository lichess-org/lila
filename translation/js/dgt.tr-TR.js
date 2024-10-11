"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.dgt)window.i18n.dgt={};let i=window.i18n.dgt;i['announceAllMoves']="Bütün Hamleleri Seslendir";i['announceMoveFormat']="Hamle Seslendirme Biçimi";i['asALastResort']=s("Son çare olarak, tahtayı Lichess ile aynı şekilde kurun, ardından %s");i['boardWillAutoConnect']="Tahta, zaten devam eden herhangi bir oyuna veya başlayan herhangi bir yeni oyuna otomatik olarak bağlanacaktır. Hangi oyunu oynayacağınızı seçme yeteneği yakında gelecek.";i['checkYouHaveMadeOpponentsMove']="Önce rakibinizin hamlesini DGT tahtada yaptığınızdan emin olun. Hamlenizi geri alın. Tekrar oynayın.";i['clickToGenerateOne']="Bir tane oluşturmak için tıklayın";i['configurationSection']="Yapılandırma Sekmesinde";i['configure']="Yapılandır";i['configureVoiceNarration']="Hamlelerin seslendirilişini yapılandırın, böylece tahtaya odaklanmaya devam edebilirsiniz.";i['debug']="Hata ayıklama";i['dgtBoard']="DGT tahtası";i['dgtBoardConnectivity']="DGT tahta bağlantısı";i['dgtBoardLimitations']="DGT Tahta Sınırlamaları";i['dgtBoardRequirements']="DGT Tahta Gereksinimleri";i['dgtConfigure']="DGT - Yapılandır";i['dgtPlayMenuEntryAdded']=s("Üst menüde PLAY menünüze %s girişi eklendi.");i['downloadHere']=s("Programı indirmek için tıklayın: %s.");i['enableSpeechSynthesis']="Konuşma Sentezi\\'ni etkinleştir";i['ifLiveChessRunningElsewhere']=s("Eğer %1$s farklı bir bilgisayarda veya portta çalışıyorsa, IP adresini ve portu %2$s ayarlamanız gerekiyor.");i['ifLiveChessRunningOnThisComputer']=s("Eğer %1$s bilgisayarınızda çalışıyorsa, kontrol etmek için %2$s.");i['ifMoveNotDetected']="Eğer bir hamle algılanmadıysa";i['keepPlayPageOpen']="\\\"Oyna\\\" sayfası tarayıcınızda açık olmak zorunda. Görünür olmak zorunda değil, küçültebilir veya Lichess oyun sayfasıyla yanyana koyabilirsiniz, ancak kapatırsanız tahta çalışmayı durduracaktır.";i['keywordFormatDescription']="Anahtar kelimeler JSON formatındadır. Hamlelerinizi ve maçların sonucunu dilinize çevirmek için kullanılırlar. Varsayılan İngilizce\\'dir ancak isteğinize göre değiştirebilirsiniz.";i['keywords']="Anahtar sözcükler";i['lichessAndDgt']="Lichess ve DGT";i['lichessConnectivity']="Lichess bağlantısı";i['moveFormatDescription']="SAN, \\\"Nf6\\\" gibi Lichess\\'teki standart hamle gösterimidir. UCI ise \\\"g8f6\\\" gibi motorlarda yaygındır.";i['noSuitableOauthToken']="Uygun bir OAuth belirteci oluşturulmadı.";i['openingThisLink']="bu bağlantıyı açın";i['playWithDgtBoard']="Bir DGT tahtasıyla oynayın";i['reloadThisPage']="Sayfayı yeniden yükle";i['selectAnnouncePreference']="Hem sizin hem de rakibinizin hamlelerini seslendirmek için EVET\\'i, yalnızca rakibinizin hamlelerini seslendirmek için HAYIR\\'ı seçin.";i['speechSynthesisVoice']="Konuşma sentezi sesi";i['textToSpeech']="Metin okuma";i['thisPageAllowsConnectingDgtBoard']="Bu sayfa, DGT tahtanızı Lichess\\'e bağlamanıza ve oyun oynamak için kullanmanıza olanak tanır.";i['timeControlsForCasualGames']="Sadece Klasik, Yazışma ve Hızlı zaman kontrolleri için rastgele oyunlar için zaman kontrolleri.";i['timeControlsForRatedGames']="Dereceli oyunlar için zaman kontrolleri: Klasik, Yazışma ve 15+10 ve 20+0 gibi bazı Hızlı zaman kontrolleri";i['toConnectTheDgtBoard']=s("DGT Elektronik Tahta\\'ya bağlanmak için %s kurmanız gerekecek.");i['toSeeConsoleMessage']="Konsol mesajını görmek için Control+ Shift + C (Windows, Linux, Chrome OS) veya Command + Option + C (Mac) kısayolunu kullanın";i['useWebSocketUrl']=s("%2$s farklı bir bilgisayarda veya portta çalışmıyorsa \\\"%1$s\\\" sayfasını kullanın.");i['validDgtOauthToken']="DGT oyunu için uygun bir OAuth belirteciniz var.";i['verboseLogging']="Ayrıntılı hata kaydı";i['webSocketUrl']=s("%s WebSocket URL\\'i");i['whenReadySetupBoard']=s("Hazır olduğunuzda tahtanızı kurun ve %s butonuna tıklayın.")})()