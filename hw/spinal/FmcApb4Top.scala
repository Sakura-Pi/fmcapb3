package fmcapb4

import spinal.core._
import spinal.lib.slave
import mybus.Debugger
import mybus.fmc.FmcBusAsync
import peripheral.{ReadOnlyReg, ReadOnlyReg2}

case class FmcApb4Top() extends Component {
  val io = new Bundle {
    val fmc = slave(FmcBusAsync())
    val debugger = slave(Debugger())
  }

  /**
   * Configuration
   */
  private val DATA_WIDTH = 32    // FMC Data Width is 32
  private val ADDRESS_WIDTH = 28 // FMC Address Width is 26(addr) + 2(bank) = 28
  private val SEL_MASK = 12      // High 12 bits as SEL bits

  /**
   * FmcApb4 Top
   */
  FmcApb4.builder(ADDRESS_WIDTH, DATA_WIDTH, SEL_MASK)
    .address(0x0010000L, ReadOnlyReg)
    .address(0x0020000L, ReadOnlyReg2)
    .link(io.fmc, io.debugger)
    .build()
}

object FmcApb4Verilog extends App {
  Config.spinal.generateVerilog(FmcApb4Top())
}
