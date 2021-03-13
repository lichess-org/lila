export const chessground = (ctrl: RacerCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode =>
        ctrl.ground(
          Chessground(
            vnode.elm as HTMLElement,
            makeCgConfig(
              ctrl.isRacing()
                ? makeCgOpts(ctrl.run, ctrl.isRacing())
                : {
                    fen: INITIAL_BOARD_FEN,
                    orientation: ctrl.run.pov,
                    movable: { color: ctrl.run.pov },
                  },
              ctrl.pref,
              ctrl.userMove
            )
          )
        ),
      destroy: _ => ctrl.withGround(g => g.destroy()),
    },
  });
