"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.arena)window.i18n.arena={};let i=window.i18n.arena;i['allowBerserk']="बेभान खेळास परवानगी द्या";i['allowBerserkHelp']="खेळाडूंना एक अधिक गुण मिळण्यासाठी त्यांची घड्याळाची वेळ अर्धी करू द्या";i['allowChatHelp']="खेळाडूंना संवाद खोलीमध्ये चर्चा करू द्या";i['arenaStreaks']="आखाड्यातील विजयी मालिका";i['arenaStreaksHelp']="2 विजयांनंतर, सलग विजय 2 ऐवजी 4 गुण देतात.";i['arenaTournaments']="आखाडे";i['averagePerformance']="सरासरी कामगिरी";i['averageScore']="सरासरी गुण";i['berserk']="बेभान आखाडा";i['berserkAnswer']="जर खेळाडूने खेळाच्या सुरूवातीला बेभान खेळाचे बटन दाबले तर त्यांच्या घडाळ्यातील अर्धा वेळ गायब होईल, पण विजयासाठी ज्यादाचा एक गुण मिळेल. \n\nबेभान खेळल्यास वेळेतील वाढही रद्द होते (१ + २ अपवाद आहे, ते १ + ० देते).\n\nबेभान खेळ शून्य प्रारंभिक वेळ (० + १, ० + २) असलेल्या खेळांसाठी उपलब्ध नाही.\n\nबेभान खेळाचा अतिरिक्त गुण मिळवण्यासाठी आपण खेळामध्ये कमीतकमी 7 चाली करणे गरजेचे आहे.";i['customStartDate']="सानुकूल सुरुवातीची तारीख";i['customStartDateHelp']="तुमच्या स्वतःच्या स्थानिक वेळेत. ह्यामुळे \\\"स्पर्धा सुरू होण्यापूर्वीची वेळ\\\" या वेळेचे सेटिंग अधिलिखित होईल";i['drawingWithinNbMoves']=p({"one":"पहिल्या %s चा हलका खेळ ड्रॉ केल्याने एकतर खेळाडूला कोणताही गुण मिळणार नाही.","other":"पहिल्या %s चालीत खेळ बरोबरीत सोडवल्यास दोन्ही खेळाडूंना गुण मिळणार नाहीत."});i['drawStreakStandard']=s("बरोबरींची मालिका: एखाद्या खेळाडूने सलग बरोबरी केल्यास फक्त पहिल्या बरोबरीचे किंवा %s पेक्षा जास्त चालींनंतर बरोबरीत सुटलेल्या खेळांचे गुण मिळतील. बरोबरींची मालिका केवळ विजयाने तोडता येऊ शकते, पराभव किंवा बरोबरीने नाही.");i['drawStreakVariants']="विविध प्रकारच्या बुद्धिबळामध्ये बरोबरीत सुटलेल्या खेळाचे गुण प्राप्त करण्यासाठी वेगवेगळ्या किमान चाली कराव्या लागतात. प्रत्येक प्रकारासाठी खालील सारणीमध्ये किमान चाली दाखवल्या आहेत.";i['history']="आखाड्यांचा इतिहास";i['howAreScoresCalculated']="गुण कसे मोजले जातात?";i['howAreScoresCalculatedAnswer']="विजयाला मूळ २ गुण मिळतात, बरोबरीसाठी १ गुण आणि पराभवासाठी काहीच गुण मिळत नाहीत.\nतुम्ही सलग दोन खेळ जिंकल्यास तुम्हास दुप्पट गुण मिळू लागतील. हे ज्योत चिन्हाने दर्शवीले जाईल.\nतुम्ही खेळ जिंकण्यात अयशस्वी होईपर्यंत सर्व खेळ दुप्पट गुणांचे असतील.\nम्हणजेच, विजयासाठी ४ गुण, बरोबरीसाठी २ गुण आणि पराभवासाठी ० गुण मिळतील.\n\nउदाहरणार्थ, दोन विजयांनंतर एक बरोबरी झाल्यास एकूण ६ गुण मिळतील: २ + २ + (२ x १)";i['howDoesItEnd']="स्पर्धा कशी संपते?";i['howDoesItEndAnswer']="या स्पर्धेसाठी मर्यादित वेळ आहे. जेव्हा स्पर्धेच्या घड्याळातील वेळ शून्य होईल तेव्हा स्पर्धेचे मानांकन गोठविले जाईल आणि विजेते घोषित केले जातील. यावेळी सुरू असलेले खेळ संपवणे गरजेचे आहे, परंतु ते स्पर्धेसाठी मोजले जाणार नाहीत.";i['howDoesPairingWork']="जोड्या कश्या ठरवल्या जातात?";i['howDoesPairingWorkAnswer']="स्पर्धेच्या सुरूवातीला, खेळाडूंच्या मानांकनाच्या आधारे जोड्या तयार करतात.\nएखादा खेळ संपताच, स्पर्धेच्या लॉबीकडे परत या: त्यानंतर आपल्यास आपल्या क्रमवारीच्या जवळच्या खेळाडूसह जोडले जाईल. यामुळे पुढच्या खेळासाठी जास्त वेळ प्रतीक्षा करावी लागत नाही, तथापि आपण स्पर्धेत इतर सर्व खेळाडूंना सामोरे जाऊ शकत नाही.\nअधिक खेळ खेळण्यासाठी आणि अधिक गुण जिंकण्यासाठी जलद खेळा आणि लॉबीवर परत या.";i['howIsTheWinnerDecided']="विजयी खेळाडू कसा ठरवतात?";i['howIsTheWinnerDecidedAnswer']="स्पर्धेच्या निर्धारित मुदतीच्या समाप्तीच्या वेळी सर्वाधिक गुण असणारा खेळाडू विजेता घोषित केला जाईल.\n\nजेव्हा दोन किंवा अधिक खेळाडूंचे गुण समान असतात, तेव्हा स्पर्धेतील कामगिरी टाय ब्रेक होते.";i['isItRated']="ही स्पर्धा गुणांकित आहे का?";i['isNotRated']="ही स्पर्धा गुणांकित नाही आणि तुमच्या गुणांकनावर परिणाम होणार नाही.";i['isRated']="ही स्पर्धा गुणांकित आहे आणि तुमच्या गुणांकनावर परिणाम करते.";i['minimumGameLength']="खेळातील किमान चाली";i['myTournaments']="माझ्या स्पर्धा";i['newTeamBattle']="नवीन संघ लढाई";i['noArenaStreaks']="Arena Streaks नाहीत";i['noBerserkAllowed']="Berserk ची परवानगी नाही";i['otherRules']="इतर महत्वाचे नियम";i['shareUrl']=s("हे URL लोकांना पाठवा म्हणजे त्यांना भाग घेता येईल: %s");i['someRated']="काही स्पर्धा गुणांकित असतात आणि तुमच्या गुणांकनावर परिणाम करतात.";i['thereIsACountdown']="तुम्हाला पहिल्या चालीसाठी मर्यादित वेळ आहे. आपण या वेळेत चाल करण्यास अयशस्वी झाल्यास आपला विरोधक हा खेळ जिंकेल.";i['thisIsPrivate']="ही खासगी स्पर्धा आहे";i['variant']="प्रकार";i['viewAllXTeams']=p({"one":"संघ पहा","other":"सर्व %s संघ पहा"});i['willBeNotified']="स्पर्धा सुरू झाल्यावर तुम्हाला सुचित करण्यात येईल, तोवर दुसऱ्या टॅबमध्ये तुम्ही काहीही खेळू शकता."})()