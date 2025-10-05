package peripheral

import spinal.core._
import spinal.lib.bus.amba3.apb._
import spinal.lib.slave

case class IPConfig() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
  }

  /**
   * This IP is designed to test the connection
   * between FMC <-> FPGA is work, detect the FPGA is under operational.
   *
   * Reg 0x00
   *   R -> Return the "SKRP" magic number
   *   W -> Nothing
   * Reg 0x01
   *   R -> Return current version of FMCAPB3 bridge
   *   W -> Nothing
   */
  val regif = new Apb3SlaveFactory(io.apb, 0)
  regif.read(B(0x534B5250, 32 bits), 0x00)
  regif.read(B(0x00000001, 32 bits), 0x04)
}
