package peripheral

import spinal.core._
import spinal.lib.bus.amba3.apb._
import spinal.lib.slave

case class ReadWriteReg() extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = 8, dataWidth = 32)))
  }

  val regif = Apb3SlaveFactory(io.apb, 0)
  val REG0 = Reg(UInt(32 bits)) init 0
  val REG1 = Reg(UInt(32 bits)) init 0
  regif.readAndWrite(REG0, 0x0)
  regif.read(REG1, 0x4)

  REG1 := REG0 / 2
}
