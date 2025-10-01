package fmcapb4

import spinal.core._
import peripheral.{ReadOnlyReg, ReadOnlyReg2}
import spinal.lib.slave
import mybus.FmcBusAsync

case class FmcApb4Top() extends Component {
  val io = new Bundle {
    val fmc_slave = slave(FmcBusAsync())
  }

  FmcApb4.builder()
    .address(0x10000L, () => ReadOnlyReg())
    .address(0x20000L, () => ReadOnlyReg2())
    .link(io.fmc_slave)
    .build()

}

object FmcApb4Verilog extends App {
  Config.spinal.generateVerilog(FmcApb4Top())
}
