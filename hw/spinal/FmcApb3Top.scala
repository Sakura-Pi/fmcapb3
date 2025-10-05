package fmcapb3

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import mybus.fmc.{Fmc, FmcConfig}
import peripheral.{IPConfig}

case class FmcApb3Top() extends Component {

  /**
   * Configuration
   */
  private val DATA_WIDTH = 32    // FMC Data Width is 32
  private val ADDRESS_WIDTH = 28 // FMC Address Width is 26(addr) + 2 = 28

  private val apb3Config = Apb3Config(ADDRESS_WIDTH, DATA_WIDTH, 1)
  private val fmcConfig = FmcConfig(ADDRESS_WIDTH - 2, DATA_WIDTH)

  val io = new Bundle {
    val fmc = slave(Fmc(fmcConfig))
  }

  /**
   * FmcApb3 Top
   */
  val bridge = FmcToApb3Bridge(fmcConfig, apb3Config)
  io.fmc <> bridge.io.fmc

  /**
   * APB3 Decoder
   */
  Apb3Decoder(
    master = bridge.io.apb,
    slaves = List(
      IPConfig().io.apb -> (0x0000, 1 KiB),
      /* Here To Bind Your Amazing IP Cores ♪~ (🌸◡‿◡) */
    )
  )

}

object FmcApb3TopVerilog extends App {
  Config.spinal.generateVerilog(FmcApb3Top())
}
