package fmcapb3

import spinal.core._
import spinal.lib._
import mybus.fmc.{Fmc, FmcConfig}
import spinal.lib.bus.amba3.apb._

object FmcAsyncToApb3BridgePhase extends SpinalEnum {
  val IDLE, SETUP, ACCESS, ERROR = newElement
}

case class FmcAsyncToApb3Bridge(fmcConfig: FmcConfig, apb3Config: Apb3Config) extends Component {

  val io = new Bundle {
    val fmc = slave(Fmc(fmcConfig))
    val apb = master(Apb3(apb3Config))
  }

  import FmcAsyncToApb3BridgePhase._
  assert(fmcConfig.addressWidth >= apb3Config.addressWidth, "APB size address is bigger than the FMC size address")
  assert(fmcConfig.dataWidth == apb3Config.dataWidth, "APB data width is not equal to FMC data width")
  assert(apb3Config.selWidth == 1, "HSEL width must be equal to 1")

  val phase = RegInit(IDLE)
  val write = Reg(Bool())
  val address = Reg(UInt(apb3Config.addressWidth bits))
  val readData = Reg(Bits(fmcConfig.dataWidth bits)) init 0xE9AAAA

  io.apb.PADDR := address.resized
  io.apb.PWRITE := write
  io.apb.PWDATA := io.fmc.D
  io.fmc.NWAIT := io.apb.PREADY
  io.apb.PENABLE := False
  io.fmc.D := readData

  switch(phase) {
    is(IDLE) {
      io.apb.PSEL := B"0"
      io.apb.PENABLE := False
//      io.apb.PWRITE := False
//      io.apb.PADDR := 0
//      io.apb.PWDATA := 0

      when(!io.fmc.NE) {
        address := io.fmc.A

        // FMC Read
        when(!io.fmc.NOE && io.fmc.NWE) {
          write := False
          phase := SETUP
        }

        // FMC Write
        when(io.fmc.NOE && !io.fmc.NWE) {
          write := True
          phase := SETUP
        }
      }

      is(SETUP) {
        io.apb.PSEL := B"1"
        io.apb.PENABLE := False
        phase := ACCESS
      }

      is(ACCESS) {
        io.apb.PSEL := B"1"
        io.apb.PENABLE := True

        when(io.apb.PREADY) {
          readData := io.apb.PRDATA

          // Wait for signal changes
          when((write && io.fmc.NWE) || (!write && io.fmc.NOE)) {
            phase := io.apb.PSLVERROR ? ERROR | IDLE
          }
        }
      }

      default { // ERROR
        io.apb.PENABLE := False
        io.apb.PSEL := B"0"
        io.fmc.NWAIT := True
        phase := IDLE
      }
    }

  }
}