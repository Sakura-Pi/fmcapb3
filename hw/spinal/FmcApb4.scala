package fmcapb4

import spinal.core._
import spinal.lib._
import mybus.Apb4Bus
import mybus.FmcBusAsync
import peripheral.Apb4Peripheral
import scala.collection.mutable.ArrayBuffer

case class SlaveConfig(baseAddr: Long, factory: () => Apb4Peripheral)

object FmcApb4 {

  class Builder {
    private val slaves = ArrayBuffer[SlaveConfig]()
    private var upstream: Option[FmcBusAsync] = None

    def address(baseAddr: Long, factory: () => Apb4Peripheral): Builder = {
      slaves += SlaveConfig(baseAddr, factory)
      this
    }

    def link(upstreamBus: FmcBusAsync): Builder = {
      this.upstream = Some(upstreamBus)
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
      upstream.foreach { fmcBus =>
        bus.io.fmc <> fmcBus
      }
      
      bus
    }
  }

  def builder(): Builder = new Builder()
}

case class FmcApb4(slaves: Array[SlaveConfig]) extends Component {

  val io = new Bundle {
    val fmc = slave(FmcBusAsync())
    val apb4 = Vec(master(Apb4Bus()), slaves.length)
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
  for(i <- io.apb4) {
    i.PSEL := False
    i.PENABLE := False
    i.PADDR := 0
    i.PWDATA := 0
    i.PWRITE := False
  }

  val currentState = Reg(FmcApb4State()) init FmcApb4State.IDLE
  val select = Reg(UInt(log2Up(slaves.length) bits)) init 0
  val rdata = Reg(UInt(32 bits)) init 0xE9AAAA

  io.fmc.D := rdata

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
    // 等待 FMC 控制信号释放，表示读操作完成
    when(io.fmc.NE || io.fmc.NOE || !io.fmc.NWE) {
      currentState := FmcApb4State.IDLE
    }
  }

  //switch(currentState) {

    // Idle
//    is(FmcApb4State.IDLE) {
//      for(i <- io.apb4) {
//        i.PSEL := False
//        i.PENABLE := False
//        i.PADDR := 0
//        i.PWDATA := 0
//        i.PWRITE := False
//      }
//    }

//    is(FmcApb4State.APB_SETUP) {
//
//    }


  //}


//  for(element <- io.apb4) {
//    // FMC到APB4信号转换
//    element.PADDR := io.fmc.A.resized
//    element.PWDATA := io.fmc.D.resized // 从双向信号读取数据
//    // APB4控制信号
//    element.PSEL := !io.fmc.NE
//    element.PWRITE := isWrite
//    element.PENABLE := isRead || isWrite
//
//    // 读操作时，将APB4的读数据输出到FMC数据线
//    when(isRead && element.PREADY) {
//      io.fmc.D := element.PRDATA
//    }
//  }

//  // FMC到APB4信号转换
//  io.apb4.PADDR := io.fmc.A.resized
//  io.apb4.PWDATA := io.fmc.D.resized // 从双向信号读取数据
//
//  // APB4控制信号
//  io.apb4.PSEL := !io.fmc.NE
//  io.apb4.PWRITE := is_write
//  io.apb4.PENABLE := is_read || is_write
//
//  // 读操作时，将APB4的读数据输出到FMC数据线
//  when(is_read && io.apb4.PREADY) {
//    io.fmc.D := io.apb4.PRDATA
//  } otherwise {
////    io.fmc_data := U(0, 32 bits)
//  }

}
