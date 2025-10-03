package peripheral

import fmcapb4.Apb4BindConfig
import spinal.core.{Bundle, Component}
import spinal.lib.bus.amba4.apb.{Apb4, Apb4SlaveFactory}
import spinal.lib.slave

abstract class Apb4Peripheral(config: Apb4BindConfig) extends Component {

  protected val io = new Bundle {
    val apb4 = slave(Apb4(config.apb4cfg))
  }

  /**
   * For automatically FMC4APB4 bridge connection
   * @return Apb4
   */
  def getApb4Interface: Apb4 = io.apb4

  /**
   * 从baseAddr提取SEL ID
   * 从地址的高位提取selWidth位作为SEL ID
   */
  def getSelId: Int = {
    val selStartBit = config.apb4cfg.addressWidth - config.apb4cfg.selWidth
    val selMask = (1 << config.apb4cfg.selWidth) - 1
    ((config.baseAddr >> selStartBit) & selMask).toInt
  }

  /**
   * 获取本地地址（去除SEL位后的地址）
   */
  def getLocalAddrMask: Long = {
    val localAddrBits = config.apb4cfg.addressWidth - config.apb4cfg.selWidth
    (1L << localAddrBits) - 1
  }

  /**
   * Register Interface
   */
  protected val reg = new Apb4SlaveFactory(io.apb4, getSelId)
}
