package peripheral

import spinal.core._
import spinal.lib.bus.amba3.apb._
import spinal.lib.slave

case class ReadWriteReg(apb3Config: Apb3Config, selId: Int) extends Component {

  val io = new Bundle {
    val apb = slave(Apb3(apb3Config))
  }

  val regif = Apb3SlaveFactory(io.apb, selId)
  val REG0 = Reg(UInt(32 bits)) init 0
  regif.readAndWrite(REG0, 0x4000)

}
