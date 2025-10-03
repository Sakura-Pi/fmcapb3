package mybus.fmc

import spinal.core._
import spinal.lib._

case class FmcBusAsync() extends Bundle with IMasterSlave {

  val A = UInt(26 bits)
  val D = inout(Analog(UInt(32 bits)))
  val NE = Bool()
  val NWE = Bool()
  val NOE = Bool()
  val NWAIT = Bool()

  override def asMaster(): Unit = {
    out(A, NE, NWE, NOE)
    in(NWAIT)
  }
}
