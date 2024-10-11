"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="हाँ, यहाँ निगमन प्रमाणपत्र है (फ्रेंच में)";i['amount']="राशि";i['bankTransfers']="हम बैंक हस्तांतरण को भी स्वीकार करते हैं";i['becomePatron']="Lichess संरक्षक बनें";i['cancelSupport']="अपना समर्थन रद्द करें";i['celebratedPatrons']="प्रसिद्ध संरक्षक जो लिचेस को संभव बनाते हैं";i['changeCurrency']="मुद्रा बदलें";i['changeMonthlyAmount']=s("मासिक राशि बदलें (%s)");i['changeMonthlySupport']="क्या मैं अपना मासिक समर्थन बदल / रद्द कर सकता हूँ?";i['changeOrContact']=s("हाँ, किसी भी समय, इस पृष्ठ से।\nया आप %s कर सकते हैं।");i['checkOutProfile']="अपना प्रोफ़ाइल पृष्ठ देखें!";i['contactSupport']="लिचेस समर्थन से संपर्क करें";i['costBreakdown']="विस्तृत लागत ब्रेकडाउन देखें";i['currentStatus']="वर्तमान स्टेटस";i['date']="दिनांक";i['decideHowMuch']="तय करें कि Lichess आपके लिए कितना योग्य है:";i['donate']="दान करें";i['donateAsX']=s("%s के रूप में दान करें");i['downgradeNextMonth']="एक महीने बाद, आपसे फिर से शुल्क नहीं लिया जाएगा, और आपका लाइचेस खाता सामान्य खाते में डाउनग्रेड हो जाएगा।";i['featuresComparison']="विस्तृत सुविधा तुलना देखें";i['freeAccount']="मुफ्त खाता";i['freeChess']="मुफ्त शतरंज सभी के लिए, हमेशा के लिए!";i['giftPatronWings']="एक खिलाड़ी को संरक्षक पंख उपहार कीजिये";i['giftPatronWingsShort']="उपहार संरक्षक पंख";i['ifNotRenewedThenAccountWillRevert']="यदि नवीनीकृत नहीं किया जाता है, तो आपका खाता फिर से एक नियमित खाते में वापस आ जाएगा।";i['lichessIsRegisteredWith']=s("लाइकेस %s के साथ पंजीकृत है।");i['lichessPatron']="Lichess संरक्षक";i['lifetime']="लाइफटाइम";i['lifetimePatron']="लाइफटाइम लिचेस पैट्रन";i['logInToDonate']="दान करने के लिए लॉग इन करें";i['makeAdditionalDonation']="अब एक अतिरिक्त दान करें";i['monthly']="मासिक";i['newPatrons']="नए संरक्षक";i['nextPayment']="अगला भुगतान";i['noAdsNoSubs']="कोई विज्ञापन नहीं, कोई सदस्यता नहीं; लेकिन खुला स्रोत और जुनून।";i['noLongerSupport']="अब Lichess का समर्थन नहीं करते";i['noPatronFeatures']="नहीं, क्योंकि Lichess पूरी तरह से मुक्त है, हमेशा के लिए, और सभी के लिए। यह एक वादा है।\nहालाँकि, Patrons को एक नए आकर्षक प्रोफ़ाइल आइकन के साथ डींग मारने का अधिकार मिलता है।";i['nowLifetime']="अब आप जीवन भर के लिए एक Lichess संरक्षक हैं!";i['nowOneMonth']="अब आप एक महीने के लिए Lichess संरक्षक हैं!";i['officialNonProfit']="Lichess एक आधिकारिक गैर-लाभकारी है?";i['onetime']="एक बार";i['onlyDonationFromAbove']="कृपया ध्यान दें कि केवल उपरोक्त दान प्रपत्र ही संरक्षक का दर्जा प्रदान करेगा।";i['otherAmount']="अन्य";i['otherMethods']="दान की अन्य विधियाँ?";i['patronFeatures']="क्या कुछ विशेषताएं संरक्षक के लिए आरक्षित हैं?";i['patronForMonths']=p({"one":"एक महीने के लिए लिचेस संरक्षक","other":"लिचेस संरक्षक %s महीनों के लिए"});i['patronUntil']=s("%s तक आपके पास एक संरक्षक खाता है।");i['payLifetimeOnce']=s("एक बार %s का भुगतान करें। हमेशा के लिए एक लिचेस संरक्षक बनें!");i['paymentDetails']="भुगतान विवरण";i['permanentPatron']="अब आपके पास एक स्थायी संरक्षक खाता है।";i['pleaseEnterAmountInX']=s("कृपया %s में राशि दर्ज करें");i['recurringBilling']="आवर्ती बिलिंग, हर महीने अपने संरक्षक पंख को नवीनीकृत करना।";i['serversAndDeveloper']=s("सबसे पहले, शक्तिशाली सर्वर।\nफिर हम एक पूर्णकालिक डेवलपर का भुगतान करते हैं: लिचेस के संस्थापक,%s।");i['singleDonation']="एक एकल दान जो आपको एक महीने के लिए संरक्षक पंख का अनुदान देता है।";i['stopPayments']="अपना क्रेडिट कार्ड वापस लें और भुगतान रोकें:";i['stopPaymentsPayPal']="पेपैल सदस्यता रद्द करें और भुगतान रोकें:";i['thankYou']="अपने योगदान के लिए धन्यवाद।";i['transactionCompleted']="आपका लेन-देन पूरा हो गया है, और आपके दान की एक रसीद आपको ईमेल की गई है।";i['tyvm']="आपकी मदद के लिए बहुत बहुत शुक्रिया। आपने धमाल मचा दिया!";i['update']="अपडेट करें";i['viewOthers']="अन्य लिचेस संरक्षक देखें";i['weAreNonProfit']="हम एक गैर लाभ संस्था हैं क्योंकि हमारा मानना ​​है कि हर किसी की मुफ्त, विश्व स्तरीय शतरंज मंच तक पहुंच होनी चाहिए।";i['weAreSmallTeam']="हमारी एक छोटी टीम हैं, इसलिए आपका समर्थन हमारे लिए बहुत बड़ी बात है!";i['weRelyOnSupport']="हम आप जैसे लोगों से मिलने वाले समर्थन पर भरोसा करते हैं। यदि आप लिचेस का उपयोग करने का आनंद लेते हैं, तो कृपया दान करके और संरक्षक बनकर हमारा समर्थन करें!";i['whereMoneyGoes']="पैसा कहां जाता है?";i['withCreditCard']="क्रेडिट कार्ड";i['xBecamePatron']=s("%s Lichess Patron बन गया");i['xIsPatronForNbMonths']=p({"one":"%1$s %2$s महीने के लिए Lichess संरक्षक है","other":"%1$s %2$s महीनों के लिए Lichess संरक्षक हैं"});i['xOrY']=s("%1$s या %2$s");i['youHaveLifetime']="आपके पास एक लाइफटाइम संरक्षक खाता है। यह बहुत बढ़िया है!";i['youSupportWith']=s("आप प्रति माह %s के साथ lichess.org का समर्थन करते हैं।");i['youWillBeChargedXOnY']=s("आपसे %2$s पर %1$s लिया जाएगा।")})()