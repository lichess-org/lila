$(function() {
	$('div.messages div.message').each(function() {
		if (!$(this).find('input').val()) {
			$(this).addClass('missing');
		}
	});
	$('div.locale_menu a').click(function() {
		$(this).parent().find('a').removeClass('active');
		$(this).addClass('active');
		$('div.messages div.message').show();
		if ($(this).hasClass('missing')) {
			$('div.messages div.message').not('.missing').hide();
		}
	});
});

