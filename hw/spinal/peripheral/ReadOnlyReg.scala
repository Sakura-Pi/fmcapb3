package peripheral

import spinal.core._
import spinal.lib.bus.amba3.apb._
import spinal.lib.slave

case class ReadOnlyReg(apb3Config: Apb3Config, selId: Int) extends Component {

  val io = new Bundle {
    val apb = slave(Apb3(apb3Config))
  }

  val regif = Apb3SlaveFactory(io.apb, selId)
  regif.read(B(0x12345678, 32 bits), 0x4000)
  regif.read(B(0x0A050A05, 32 bits), 0x8000)

}
