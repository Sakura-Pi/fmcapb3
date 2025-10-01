package mybus

import spinal.core._
import spinal.lib.IMasterSlave

case class Apb4Bus() extends Bundle with IMasterSlave {

  val PADDR = UInt(32 bits)
  val PSEL = Bool()
  val PENABLE = Bool()
  val PWRITE = Bool()
  val PWDATA = UInt(32 bits)
  val PRDATA = UInt(32 bits)
  val PREADY = Bool()
  val PSLVERR = Bool()

  override def asMaster(): Unit = {
    out(PADDR, PSEL, PENABLE, PWRITE, PWDATA)
    in(PRDATA, PREADY, PSLVERR)
  }
}
