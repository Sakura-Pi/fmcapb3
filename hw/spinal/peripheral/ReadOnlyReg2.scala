package peripheral

import fmcapb4.Apb4BindConfig
import spinal.core._

case class ReadOnlyReg2(bind: Apb4BindConfig) extends Apb4Peripheral(bind) {
  reg.read(B(0x00E9E9E9, 32 bits), address = 0x00)
  reg.read(B(0x56781234, 32 bits), address = 0x04)
}

