package fmcapb4

import spinal.core._
import spinal.lib._
import mybus.{Apb4Bus, Debugger}
import mybus.fmc.FmcBusAsync
import peripheral.Apb4Peripheral

import scala.collection.mutable.ArrayBuffer

case class SlaveConfig(baseAddr: Long, factory: () => Apb4Peripheral)

object FmcApb4 {

  class Builder {
    private val slaves = ArrayBuffer[SlaveConfig]()
    private var upstream: Option[FmcBusAsync] = None
    private var debugger: Option[Debugger] = None

    def address(baseAddr: Long, factory: () => Apb4Peripheral): Builder = {
      slaves += SlaveConfig(baseAddr, factory)
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
      val bus = FmcApb4(slaves.toArray)

      // connect slaves to the bus
      for((element, i) <- bus.io.apb4.zipWithIndex) {
        val slave = slaves(i).factory()
        element <> slave.getApb4Interface
      }

      // connect to upstream
      upstream.foreach { fmcBus => bus.io.fmc <> fmcBus }
      debugger.foreach { debugger => bus.io.debugger <> debugger }

      bus
    }
  }

  def builder(): Builder = new Builder()
}

case class FmcApb4(slaves: Array[SlaveConfig]) extends Component {

  val io = new Bundle {
    val fmc = slave(FmcBusAsync())
    val apb4 = Vec(master(Apb4Bus()), slaves.length)
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

  io.fmc.NWAIT := True
  for (i <- io.apb4) {
    i.PSEL := False
    i.PENABLE := False
    i.PADDR := 0
    i.PWDATA := 0
    i.PWRITE := False
  }

  val currentState = Reg(FmcApb4State()) init FmcApb4State.IDLE
  val select = Reg(UInt(log2Up(slaves.length) bits)) init 0
  val rdata = Reg(UInt(32 bits)) init 0xE9AAAA

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
    for(i <- io.apb4) {
      i.PSEL := False
      i.PENABLE := False
      i.PADDR := 0
      i.PWDATA := 0
      i.PWRITE := False
    }
  }

  when(currentState === FmcApb4State.IDLE && !io.fmc.NE && !io.fmc.NOE && io.fmc.NWE) {

    val foundMatch = False

    // simple address decoder
    for((element, i) <- slaves.zipWithIndex) {
      val baseAddr26 = U(element.baseAddr & 0xFFFFFFF, 28 bits)  // clamp low 28 bits

      when(baseAddr26 === io.fmc.A << 2) {
        select := U(i, log2Up(slaves.length) bits)
        io.apb4(select).PSEL := True
        io.apb4(select).PENABLE := False
        io.apb4(select).PADDR := io.fmc.A.resized
        io.fmc.NWAIT := io.apb4(select).PREADY
        currentState := FmcApb4State.APB_ACCESS
        foundMatch := True
      }
    }

    when(!foundMatch) {
      currentState := FmcApb4State.IDLE
    }
  }

  when(currentState === FmcApb4State.APB_ACCESS) {
    io.apb4(select).PSEL := True
    io.apb4(select).PENABLE := True
    io.fmc.NWAIT := io.apb4(select).PREADY
    rdata := io.apb4(select).PRDATA
    when(io.apb4(select).PREADY === True) {
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