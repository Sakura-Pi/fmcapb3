package mybus

import spinal.core._
import spinal.lib.IMasterSlave

case class Debugger() extends Bundle with IMasterSlave {
  val hub = UInt(32 bits)

  // 提供位级访问的便捷方法
  def apply(index: Int): Bool = hub(index)

  override def asMaster(): Unit = {
    in(hub)
  }
}
