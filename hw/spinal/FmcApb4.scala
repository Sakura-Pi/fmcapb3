package fmcapb4

import spinal.core._
import spinal.lib._
import mybus.Debugger
import mybus.fmc.FmcBusAsync
import peripheral.Apb4Peripheral
import spinal.lib.bus.amba4.apb.{Apb4, Apb4Config}

import scala.collection.mutable.ArrayBuffer

case class Apb4BindConfig(baseAddr: Long,
                          factory: Apb4BindConfig => Apb4Peripheral,
                          apb4cfg: Apb4Config)

object FmcApb4 {

  class Builder(addressWidth: Int, dataWidth: Int, selWidth: Int) {
    private val slaves = ArrayBuffer[Apb4BindConfig]()
    private val slaves_inst = ArrayBuffer[Apb4Peripheral]()
    private var upstream: Option[FmcBusAsync] = None
    private var debugger: Option[Debugger] = None
    private val apb4cfg = Apb4Config(addressWidth, dataWidth, selWidth, false, false)

    def address(baseAddr: Long, factory: Apb4BindConfig => Apb4Peripheral): Builder = {
      val slave = Apb4BindConfig(baseAddr, factory, apb4cfg)
      slaves += slave
      slaves_inst += factory(slave)
      this
    }

    def link(upstreamBus: FmcBusAsync): Builder = {
      this.upstream = Some(upstreamBus)
      this
    }

    def link(upstreamBus: FmcBusAsync, debugger: Debugger): Builder = {
      this.upstream = Some(upstreamBus)
      this.debugger = Some(debugger)
      this
    }

    def build(): FmcApb4 = {
      val bus = FmcApb4(apb4cfg, slaves.toArray, slaves_inst.toArray, (1 << (addressWidth - selWidth)) - 1)

      // connect slaves to the bus
      for(element <- slaves_inst) {
        bus.io.apb4 >> element.getApb4Interface
//        bus.io.apb4.PREADY := element.getApb4Interface.PREADY
      }

      // connect to upstream
      upstream.foreach { fmcBus => bus.io.fmc <> fmcBus }
      debugger.foreach { debugger => bus.io.debugger <> debugger }

      bus
    }
  }

  def builder(addressWidth: Int, dataWidth: Int, selWidth: Int): Builder
    = new Builder(addressWidth, dataWidth, selWidth)
}

case class FmcApb4(apb4cfg: Apb4Config,
                   slaves: Array[Apb4BindConfig],
                   slaves_inst: Array[Apb4Peripheral],
                   selMask: Int) extends Component {

  val io = new Bundle {
    val fmc = slave(FmcBusAsync())
    val apb4 = master(Apb4(apb4cfg))
    val debugger = slave(Debugger())
  }

  //   Flexible Memory Controller SRAM Read Stat Table
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  //   | is_read | is_write | NE | NOE | NWE | NWAIT | A[25:0] | D[31:0] | PADDR[31:0] | PWDATA[31:0] | PRDATA[31:0] | PSEL | PENABLE | PWRITE | PREADY | PSLVERR |
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  // 0 | False   | False    | 1  | 1   | 1   | 1     |    /    |    /    |      /      |      /       |      /       | 0    | 0       | 0      | 1      |    /    |
  //   +---------+----------+----+-----+-----+-------+---------+---------+--------------+-------------+--------------+------+---------+--------+--------+---------+
  // 1 | False   | False    | 0  | 1   | 1   | 1     |    /    |    /    |      /      |      /       |      /       | 0    | 0       | 0      | 1      |    /    | NE from 1 to 0: Chip Select
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  // 2 | True    | False    | 0  | 0   | 1   |!PREADY| Address |    /    |  Decode A   |      /       |      /       | 1    | 0       | 0      | 1      |    /    | NOE from 1 to 0: Read Operation Start
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  // 3 | True    | False    | 0  | 0   | 1   |!PREADY| Address | PRDATA  |      A      |      /       |    Data?     | 1    | 1       | 0      | 0      |    /    | NOE from 1 to 0: Read Operation Start
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  // 4 |                                                              Wait Until PREADY = 1                                                                       |
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  // 5 | True    | False    | 0  | 0   | 1   |!PREADY| Address | PRDATA  |      /      |      /       |    Data      | 0    | 0       | 0      | 1      |    /    |
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+
  // 6 |                                           Wait Until NE = 1 && NOE = 1 && NWE = 1   Then Jump To Stat 0                                                  |
  //   +---------+----------+----+-----+-----+-------+---------+---------+-------------+--------------+--------------+------+---------+--------+--------+---------+

  val currentState = Reg(FmcApb4State()) init FmcApb4State.IDLE
  val select = Reg(UInt(log2Up(slaves.length) bits)) init 0
  val rdata = Reg(UInt(32 bits)) init 0xE9AAAA
  val localAddr = Reg(UInt(apb4cfg.addressWidth - apb4cfg.selWidth bits)) init 0
  val localSelId = Reg(UInt(apb4cfg.selWidth bits)) init 0

  io.fmc.NWAIT := !False
  io.apb4.PSEL := 0
  io.apb4.PWRITE := False
  io.apb4.PENABLE := False
  io.apb4.PADDR := 0
  io.apb4.PWDATA := 0

  io.debugger.hub(0) := io.fmc.NWE
  io.debugger.hub(1) := io.fmc.NOE
  io.debugger.hub(2) := io.fmc.NE
  io.debugger.hub(3) := currentState.asBits(0)
  io.debugger.hub(4) := currentState.asBits(1)
  io.debugger.hub(5) := (io.fmc.A << 2 === slaves(0).baseAddr)

  // 为剩余的位赋默认值0
  for(i <- 6 until 32) {
    io.debugger.hub(i) := False
  }

  when(currentState === FmcApb4State.IDLE) {
    io.apb4.PSEL := 0
    io.apb4.PWRITE := False
    io.apb4.PENABLE := False
    io.apb4.PADDR := 0
    io.apb4.PWDATA := 0
  }

  when(currentState === FmcApb4State.IDLE && !io.fmc.NE && !io.fmc.NOE && io.fmc.NWE) {

    // 使用地址高位自动解码SEL ID
    val address = UInt(28 bits)

    address := io.fmc.A << 2
    localAddr := (address & U(selMask)).resized
    localSelId := (address >> (apb4cfg.addressWidth - apb4cfg.selWidth)).resized

    val foundMatch = False
    for((element, i) <- slaves.zipWithIndex) {
      when(localSelId === slaves_inst(i).getSelId) {
        io.apb4.PSEL := localSelId.asBits
        io.apb4.PENABLE := False
        io.apb4.PADDR := localAddr.resized
        io.fmc.NWAIT := io.apb4.PREADY
        currentState := FmcApb4State.APB_ACCESS
        foundMatch := True
      }
    }

    when(!foundMatch) {
      currentState := FmcApb4State.IDLE
    }
  }

  when(currentState === FmcApb4State.APB_ACCESS) {
    io.apb4.PSEL := localSelId.asBits
    io.apb4.PENABLE := True
    io.fmc.NWAIT := io.apb4.PREADY
    rdata := io.apb4.PRDATA.asUInt
    when(io.apb4.PREADY === True) {
      currentState := FmcApb4State.WAIT_RELEASE
    }
  }

  when(currentState === FmcApb4State.WAIT_RELEASE) {
    io.fmc.D := rdata
    when(io.fmc.NOE || !io.fmc.NWE) {
      currentState := FmcApb4State.IDLE
    }
  }

}