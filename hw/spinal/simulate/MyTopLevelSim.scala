import fmcapb4.{Config, FmcApb4Top}
import mybus.fmc.sim.FmcAsyncDriver
import spinal.core._
import spinal.core.sim._

object MyTopLevelSim extends App {
  Config.sim.compile(FmcApb4Top()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    val driver = FmcAsyncDriver(dut.io.fmc, dut.clockDomain)
    driver.reset()

    println("=== Start ===")
    val reg0 = driver.read(0x10000L, true)
    val reg1 = driver.read(0x20000L, true)
    println(reg0)
    println(reg1)

//    driver.write(0x10000L, 0xA5A5A5, true)
    dut.clockDomain.waitSampling(10)

    driver.reset()
    println("=== End ===")
  }
}
