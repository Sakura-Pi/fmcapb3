package fmcapb3

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import mybus.fmc.{Fmc, FmcConfig}
import peripheral.{IPConfig, ReadWriteReg, ws2812}

case class FmcApb3TopExample() extends Component {

  /**
   * Configuration
   */
  private val DATA_WIDTH = 32    // FMC Data Width is 32
  private val ADDRESS_WIDTH = 28 // FMC Address Width is 26(addr) + 2 = 28

  private val apb3Config = Apb3Config(ADDRESS_WIDTH, DATA_WIDTH, 1)
  private val fmcConfig = FmcConfig(ADDRESS_WIDTH, DATA_WIDTH)

  val io = new Bundle {
    val fmc = slave(Fmc(fmcConfig))
    val pulse = out Bool()
  }

  /**
   * FmcApb3 Top
   */
  val bridge = FmcToApb3Bridge(fmcConfig, apb3Config)
  io.fmc <> bridge.io.fmc

  val ws2812Controller = ws2812.Controller()
  io.pulse <> ws2812Controller.io.pulse

  /**
   * APB3 Decoder
   */
  Apb3Decoder(
    master = bridge.io.apb,
    slaves = List(
      IPConfig().io.apb          -> (0x0000, 1 KiB),
      ws2812Controller.io.apb    -> (0x10000, 1 KiB),
    )
  )
}

object FmcApb3TopExampleVerilog extends App {
  Config.spinal.generateVerilog(FmcApb3TopExample())
}
