package peripheral
import spinal.core._
import spinal.lib._
import mybus.Apb4Bus

case class ReadOnlyReg() extends Apb4Peripheral {

  val io = new Bundle {
    val apb4_slave = slave(Apb4Bus())
  }

  override def getApb4Interface: Apb4Bus = io.apb4_slave

  /**
   * Registers
   */
  val RegValue = Reg(UInt(32 bits)) init 0x12345678 allowUnsetRegToAvoidLatch

  // 默认值
  io.apb4_slave.PREADY := True
  io.apb4_slave.PSLVERR := False
  io.apb4_slave.PRDATA := 0

  // APB4 读操作
  when(io.apb4_slave.PSEL && io.apb4_slave.PENABLE && ~io.apb4_slave.PWRITE) {
    io.apb4_slave.PRDATA := RegValue
    io.apb4_slave.PREADY := True
  }
}
