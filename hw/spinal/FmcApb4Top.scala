package fmcapb4

import spinal.core._
import spinal.lib.slave

import mybus.Debugger
import mybus.fmc.FmcBusAsync
import peripheral.{ReadOnlyReg, ReadOnlyReg2}

case class FmcApb4Top() extends Component {
  val io = new Bundle {
    val fmc_slave = slave(FmcBusAsync())
    val debugger = slave(Debugger())
  }

  FmcApb4.builder()
    .address(0x10000L, () => ReadOnlyReg())
    .address(0x20000L, () => ReadOnlyReg2())
    .link(io.fmc_slave, io.debugger)
    .build()

}

object FmcApb4Verilog extends App {
  Config.spinal.generateVerilog(FmcApb4Top())
}
