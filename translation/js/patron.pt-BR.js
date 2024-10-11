"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.patron)window.i18n.patron={};let i=window.i18n.patron;i['actOfCreation']="Sim, aqui está o ato de criação (em francês)";i['amount']="Quantia";i['bankTransfers']="Também aceitamos transferências bancárias";i['becomePatron']="Seja um Patrono Lichess";i['cancelSupport']="cancelar seu apoio";i['celebratedPatrons']="Os famosos Patronos que tornam o Lichess possível";i['changeCurrency']="Trocar de moeda";i['changeMonthlyAmount']=s("Alterar o valor mensal (%s)");i['changeMonthlySupport']="Posso mudar/cancelar meu suporte mensal?";i['changeOrContact']=s("Sim, a qualquer momento, a partir desta página.\nOu você pode %s.");i['checkOutProfile']="Confira sua página de perfil!";i['contactSupport']="entrar em contato com o suporte do Lichess";i['costBreakdown']="Veja a análise detalhada do custo";i['currentStatus']="Situação atual";i['date']="Data";i['decideHowMuch']="Decida quanto Lichess vale para você:";i['donate']="Doação";i['donateAsX']=s("Doar como %s");i['downgradeNextMonth']="Em um mês, você NÃO será cobrado novamente, e sua conta Lichess será convertida para a versão gratuita.";i['featuresComparison']="Veja a comparação detalhada de recursos";i['freeAccount']="Conta gratuita";i['freeChess']="Xadrez de graça, para todos, para sempre!";i['giftPatronWings']="Presenteie um jogador com asas de Patrono";i['giftPatronWingsShort']="Presentear com asas de Patrono";i['ifNotRenewedThenAccountWillRevert']="Se não for renovada, sua conta voltará a ser uma conta normal.";i['lichessIsRegisteredWith']=s("Lichess está registrado em %s.");i['lichessPatron']="Apoie o Lichess";i['lifetime']="Vitalício";i['lifetimePatron']="Patrono vitalício do Lichess";i['logInToDonate']="Faça login para doar";i['makeAdditionalDonation']="Faça uma doação adicional agora";i['monthly']="Mensalmente";i['newPatrons']="Novos Patronos";i['nextPayment']="Próximo pagamento";i['noAdsNoSubs']="Sem anúncios, sem assinaturas; mas com código aberto e paixão.";i['noLongerSupport']="Deixar de apoiar o Lichess";i['noPatronFeatures']="Não, porque o Lichess é totalmente gratuito, para sempre e para todos. É uma promessa.\nNo entanto, Patronos têm direito de se gabar com um novo ícone legal de perfil.";i['nowLifetime']="Você agora é um Patrono vitalício do Lichess!";i['nowOneMonth']="Você agora é Patrono do Lichess por um mês!";i['officialNonProfit']="O Lichess é uma organização sem fins lucrativos?";i['onetime']="Uma vez";i['onlyDonationFromAbove']="Somente doações pelo formulário acima lhe concedem o status de Patrono.";i['otherAmount']="Outro";i['otherMethods']="Outros métodos de doação?";i['patronFeatures']="Alguns recursos são exclusivos para Patronos?";i['patronForMonths']=p({"one":"Patrono Lichess por um mês","other":"Apoiador Lichess por %s meses"});i['patronUntil']=s("Você tem uma conta de Patrono até %s.");i['payLifetimeOnce']=s("Pague %s uma vez. Seja um Patrono Lichess para sempre!");i['paymentDetails']="Detalhes de pagamento";i['permanentPatron']="Agora você tem uma conta de Patrono permanente.";i['pleaseEnterAmountInX']=s("Por favor, insira um valor em %s");i['recurringBilling']="Faturamento recorrente, renovando suas asas de Patrono todo mês.";i['serversAndDeveloper']=s("Primeiro de tudo, servidores poderosos.\nEm seguida, pagamos um desenvolvedor em tempo integral: %s, o fundador do Lichess.");i['singleDonation']="Uma única doação que lhe concede as asas de Patrono por um mês.";i['stopPayments']="Retire seu cartão de crédito e interrompa os pagamentos:";i['stopPaymentsPayPal']="Cancelar assinatura PayPal e interromper pagamentos:";i['stripeManageSub']="Gerencie sua assinatura e faça o download de suas faturas e recibos";i['thankYou']="Obrigado pela sua doação!";i['transactionCompleted']="Sua transação foi concluída e um recibo de sua doação foi enviado por e-mail para você.";i['tyvm']="Muito obrigado pelo seu apoio. Você é demais!";i['update']="Atualização";i['updatePaymentMethod']="Atualizar forma de pagamento";i['viewOthers']="Ver outros Patronos do Lichess";i['weAreNonProfit']="Somos uma associação sem fins lucrativos porque acreditamos que todos devem ter acesso a uma plataforma de xadrez gratuita e de nível mundial.";i['weAreSmallTeam']="Somos uma equipe pequena, então o seu apoio faz uma grande diferença!";i['weRelyOnSupport']="Contamos com o apoio de pessoas como você para tornar isso possível. Se você gosta de usar o Lichess, considere apoiar-nos doando e se tornando um Patrono!";i['whereMoneyGoes']="Para onde vai o dinheiro?";i['withCreditCard']="Cartão de Crédito";i['xBecamePatron']=s("%s se tornou um Patrono Lichess");i['xIsPatronForNbMonths']=p({"one":"%1$s é um Patrono Lichess por %2$s mês","other":"%1$s é um Patrono Lichess por %2$s meses"});i['xOrY']=s("%1$s ou %2$s");i['youHaveLifetime']="Você tem uma conta de Patrono vitalícia. Isso é incrível!";i['youSupportWith']=s("Você apoia o lichess.org com %s por mês.");i['youWillBeChargedXOnY']=s("Você será cobrado em %1$s no dia %2$s.")})()