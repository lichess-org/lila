function make(data) {

  return new lichess.StrongSocket(data.url.socket, data.player.version, {
    options: {
      name: "game"
    },
    params: {
      ran: "--ranph--",
      userTv: $('.user_tv').data('user-tv')
    },
    events: {}
  });
}

module.exports = {
  make: make
};
