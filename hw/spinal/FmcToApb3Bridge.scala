package fmcapb3

import spinal.core._
import spinal.lib._
import mybus.fmc.{Fmc, FmcConfig}
import spinal.lib.bus.amba3.apb._

object FmcToApb3BridgePhase extends SpinalEnum {
  val IDLE, SETUP, ACCESS, ERROR = newElement
}

case class FmcToApb3Bridge(fmcConfig: FmcConfig, apb3Config: Apb3Config) extends Component {

  val io = new Bundle {
    val fmc = slave(Fmc(fmcConfig))
    val apb = master(Apb3(apb3Config))
  }

  import FmcToApb3BridgePhase._
  assert(fmcConfig.addressWidth + 2 >= apb3Config.addressWidth, "APB size address is bigger than the FMC size address")
  assert(fmcConfig.dataWidth == apb3Config.dataWidth, "APB data width is not equal to FMC data width")
  assert(apb3Config.selWidth == 1, "HSEL width must be equal to 1")

  val phase = RegInit(IDLE)
  val write = Reg(Bool())
  val address = Reg(UInt(apb3Config.addressWidth bits))
  val readData = Reg(Bits(fmcConfig.dataWidth bits)) init 0
  val writeData = Reg(Bits(fmcConfig.dataWidth bits)) init 0

  io.apb.PADDR := address.resized
  io.apb.PWRITE := write
  io.fmc.NWAIT := io.apb.PREADY
  io.apb.PENABLE := False
  io.apb.PWDATA := writeData
  readData := io.apb.PRDATA

  switch(phase) {
    is(IDLE) {
      io.apb.PSEL := B"0"
      io.apb.PENABLE := False

      when(!io.fmc.NE) {
        address := io.fmc.A << 2 // Address is word aligned

        // FMC Read
        when(!io.fmc.NOE && io.fmc.NWE) {
          write := False
          phase := SETUP
        }

        // FMC Write
        when(io.fmc.NOE && !io.fmc.NWE) {
          write := True
          writeData := io.fmc.D
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
        when(write) {
          writeData := io.fmc.D
        } otherwise {
          io.fmc.D := readData
        }

        // Jump while FMC signal changes
        when(io.apb.PREADY) {
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