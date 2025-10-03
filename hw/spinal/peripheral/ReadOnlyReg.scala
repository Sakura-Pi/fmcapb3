package peripheral
import fmcapb4.Apb4BindConfig
import spinal.core._

case class ReadOnlyReg(bind: Apb4BindConfig) extends Apb4Peripheral(bind) {
  reg.read(B(0x5555AAAA, 32 bits), address = 0x00)
  reg.read(B(0x12345678, 32 bits), address = 0x04)
}
