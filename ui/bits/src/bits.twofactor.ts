import QRCode from 'qrcode';

const issuer = window.location.host; // lichess.org
const secret = $('input[name=secret]').val();

QRCode.toCanvas(
  document.getElementById('qrcode'),
  `otpauth://totp/${issuer}:${document.body.dataset.user}?secret=${secret}&issuer=${issuer}`,
  {
    width: 320,
  },
);
