package lila

import lila.socket.WithSocket

package object simulation
    extends PackageObject
    with WithPlay
    with WithSocket 
