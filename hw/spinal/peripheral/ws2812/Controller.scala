package peripheral.ws2812

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb.{Apb3, Apb3SlaveFactory}

// 定义配置寄存器的位域
case class ConfigReg() extends Bundle {
  val EN   = Bool()        // bit 0
  val MODE = UInt(2 bits)  // bit 2:1
}

case class Controller(channels: Int = 1) extends Component {
  clockDomain.frequency = ClockDomain.FixedFrequency(100 MHz)
  
  val io = new Bundle {
    val apb = slave(Apb3(addressWidth = 8, dataWidth = 32))
    val pulse = out Bool()
  }

  val R_CTRL0     = Reg(ConfigReg()) init ConfigReg().getZero
  val R_CHANNEL0  = Reg(Bits(24 bits)) init 0

  // 创建 RGB 数据流和 FIFO
  val rgbStream = Stream(RgbData())
  val rgbFifo = StreamFifo(
    dataType = RgbData(),
    depth = 16
  )
  
  // 连接流到 FIFO
  rgbStream >> rgbFifo.io.push
  
  // 默认情况下流无效
  rgbStream.valid := False
  rgbStream.payload.assignDontCare()

  val regif = new Apb3SlaveFactory(io.apb, 0)
  regif.readAndWrite(R_CTRL0, 0x00, documentation = "Control Register")
  regif.write(R_CHANNEL0, 0x04, documentation = "Channel RGB Register")  // 地址改为 0x04，避免冲突
  
  // 当写入 R_CHANNEL0 时，向 FIFO 推入 RGB 数据
  regif.onWrite(0x04, documentation = "Channel RGB Register") {
    rgbStream.valid := True
    rgbStream.RED   := R_CHANNEL0(7 downto 0).asUInt
    rgbStream.GREEN := R_CHANNEL0(15 downto 8).asUInt
    rgbStream.BLUE  := R_CHANNEL0(23 downto 16).asUInt
  }

  // 可以添加 FIFO 状态寄存器
  val fifoStatus = regif.read(UInt(8 bits), 0x08, documentation = "FIFO Status")
  fifoStatus := rgbFifo.io.occupancy.resized
  
  // 输出示例：当有数据可用且使能时输出脉冲
  io.pulse := R_CTRL0.EN && rgbFifo.io.pop.valid

  new SlowArea(2.5 MHz) {
    val channel = Channel()
    rgbFifo.io.pop >> channel.io.data
  }
}

