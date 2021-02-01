export default function(serverKey: string): Promise<string> {
  const otp = randomAscii(64);
  lichess.socket.send('sk1', `${serverKey}!${otp}`);
  return new Promise(solve =>
    lichess.pubsub.on('socket.in.sk1', encrypted => solve(xor(encrypted, otp)))
  );
}

function xor(a: string, b: string) {
  const result = [];
  for (let i = 0; i < a.length; i++)
    result.push(String.fromCharCode(a.charCodeAt(i) ^ b.charCodeAt(i)));
  return result.join('');
}

function randomAscii(length: number) {
  const result = [];
  // start after '!' which is used as delimiter
  for (let i = 0; i < length; i++) result.push(String.fromCharCode(34 + Math.floor(Math.random() * 92)));
  return result.join('');
}
