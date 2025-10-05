import fmcapb3.{Config, FmcApb3Top}
import mybus.fmc.sim.FmcDriver
import spinal.core._
import spinal.core.sim._

object MyTopLevelSim extends App {
  Config.sim.compile(FmcApb3Top()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut
    dut.clockDomain.forkStimulus(period = 10)

    val driver = FmcDriver(dut.io.fmc, dut.clockDomain)
    driver.reset()

    println("=== Start ===")
    var reg0 = driver.read(0x10000L, true)
    println(reg0)

    driver.write(0x10000L, 0x233333, true)

    reg0 = driver.read(0x10000L, true)
    dut.clockDomain.waitSampling(10)

    driver.reset()
    println("=== End ===")
  }
}
