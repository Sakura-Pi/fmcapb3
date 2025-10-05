package mybus.fmc

import spinal.core._
import spinal.lib._

case class FmcConfig(addressWidth: Int, dataWidth: Int)
case class Fmc(config: FmcConfig) extends Bundle with IMasterSlave {

  val A = UInt(config.addressWidth bits)
  val D = inout(Analog(Bits(config.dataWidth bits)))
  val NE = Bool()
  val NWE = Bool()
  val NOE = Bool()
  val NWAIT = Bool()

  override def asMaster(): Unit = {
    out(A, NE, NWE, NOE)
    in(NWAIT)
  }
}
