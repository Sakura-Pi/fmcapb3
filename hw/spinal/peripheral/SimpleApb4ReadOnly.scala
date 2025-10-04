package peripheral

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.apb._

/**
 * 最简单的SpinalHDL APB4只读寄存器实现
 * 使用Apb4SlaveFactory让代码极其简洁
 */
case class SimpleApb4ReadOnlyReg() extends Component {
//
//  val io = new Bundle {
//    val apb4 = slave(Apb4(Apb4Config(addressWidth = 28, dataWidth = 32)))
//  }
//
//  // 一行代码创建APB4从设备工厂
//  val factory = new Apb4SlaveFactory(io.apb4, 1)
//
//  // 创建只读寄存器 - 每个只需一行代码
//  factory.read(B(0x12345678, 32 bits), address = 0x00)  // 设备ID
//  factory.read(B(0x00010000, 32 bits), address = 0x04)  // 版本号
//
//  // 动态只读寄存器
//  val counter = Reg(UInt(32 bits)) init 0
//  counter := counter + 1
//  factory.read(counter.asBits, address = 0x08)  // 计数器
//
//  // 状态寄存器
//  val status = new Bundle {
//    val ready = Bool()
//    val busy = Bool()
//    val reserved = Bits(30 bits)
//  }
//  status.ready := True
//  status.busy := False
//  status.reserved := 0
//  factory.read(status.asBits, address = 0x0C)
}
